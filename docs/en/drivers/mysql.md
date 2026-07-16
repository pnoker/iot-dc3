---
title: MySQL Driver
---

<script setup>
import MysqlDiagram from '../../.vitepress/theme/components/MysqlDiagram.vue'
</script>


# MySQL Driver

`dc3-driver-mysql` onboards a MySQL database into IoT DC3 as a data source: acting as a database client, it runs a
`SELECT` on each polling cycle and uses the queried value as the reading, and supports writing values into the database
via the `UPDATE`/`INSERT` write query configured on the point. After reading this you can set the connection parameters
on a [Device](../introduction/concepts/device), the read/write SQL on each [Point](../introduction/concepts/point), and
pinpoint common "can't connect / no value / write fails" problems.

> You are here: a driver that onboards an existing database as a data source. Not all data comes from a fieldbus
> device—much business data, historical data, and third-party results simply live in a MySQL table.

## Protocol background

MySQL is the world's most widely used open-source relational database, born in 1995, using SQL as its query language and
organizing data into tables/rows/columns. In IoT scenarios it is often not a "field device" but a hub where data
converges: business systems such as MES/ERP, third-party platforms, and historical archives all tend to drop their
results into a MySQL table for downstream consumption. Onboard such a table as a data source, and the platform can poll
its columns into [PointValues](../introduction/concepts/point-value) just like it polls a real device.

This driver acts as a database client ([Driver](../introduction/concepts/driver) type `DRIVER_CLIENT`), connecting to a
MySQL database over JDBC (`mysql-connector-j`, driver class `com.mysql.cj.jdbc.Driver`) and reading/writing values by
the SQL configured on each [Point](../introduction/concepts/point). Its communication model is classic *
*request-response**—the driver, as a client, actively issues queries; the database never pushes, so collection is driven
by cron polling. The shared logic for JDBC connections, connection pooling, and SQL execution lives in the abstract base
class `AbstractJdbcDriverCustomService` (`dc3-common-sql` module), reused by all four database drivers (MySQL,
PostgreSQL, Oracle, SQL Server), each of which only supplies JDBC URL construction and the driver class name.

Seen through the [IoT data pipeline](../foundations/data-pipeline), database drivers sit at the entry point of "moving
externally-structured data into the platform": the sensing layer and fieldbus protocols digitize physical quantities,
while the MySQL driver onboards results already accumulated in a database into the same pipeline—ultimately stored,
queried, and consumed by alarms and AI just like values collected from real devices.

Two driver-specific concepts that the configuration tables below rely on:

<MysqlDiagram lang="en" />

- **Read Query**: a `SELECT` configured on the point; the driver runs it on each polling cycle and takes the **first
  column of the first row** of the result as the point's value.
- **Write Query**: an `UPDATE`/`INSERT` configured on the point, using a single `?` placeholder for the value to
  write—when a write command fires, the command parameter is bound via prepared-statement parameter binding.

## Attribute configuration

Onboarding a MySQL database requires filling in [attributes](../introduction/concepts/attribute-config) at three levels:
device-level connection parameters (`driver-attribute`), each polled point's read/write SQL (`point-attribute`), and one
reserved attribute on the write command (`command-attribute`). The attributes, types, and defaults below are taken from
the driver's `application.yml` (`dc3-driver-mysql` module).

### Driver attributes (device-level `driver-attribute`)

Driver attributes answer "which database to connect to, which account to use, and the query timeout". Fill in one set
per MySQL database on the [Device](../introduction/concepts/device):

| Attribute     | code           | Type   | Default     | Remark                       |
|---------------|----------------|--------|-------------|------------------------------|
| Host          | `host`         | STRING | `localhost` | MySQL host IP or hostname    |
| Port          | `port`         | INT    | `3306`      | MySQL port (standard 3306)   |
| Database      | `database`     | STRING | (empty)     | MySQL database name          |
| Username      | `username`     | STRING | `root`      | MySQL username               |
| Password      | `password`     | STRING | (empty)     | MySQL password               |
| Query Timeout | `queryTimeout` | INT    | `30`        | SQL query timeout in seconds |

The driver builds the JDBC URL from `host`, `port`, and `database`, in the form
`jdbc:mysql://host:port/database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`. All five of `host`,
`port`, `database`, `username`, and `password` are required—configuration validation (`validate()`) checks each one, and
any missing field fails. The driver caches one HikariCP connection pool per device ID (one pool per device, max 5
connections), with the connection timeout set to `queryTimeout × 1000` milliseconds.

::: tip queryTimeout applies to both connecting and the query pace
`queryTimeout` (default 30 seconds) is used as the pool's `connectionTimeout`: failing to acquire a connection, or a
connection that stalls beyond this duration, fails. It is separate from the polling interval—if a slow SQL consistently
approaches or exceeds it, optimize the SQL or add an index rather than just raising the timeout.
:::

### Point attributes (`point-attribute`)

Point attributes answer "which value to query from this database, and where to write". Fill in the read/write SQL on
each polled [Point](../introduction/concepts/point):

| Attribute   | code         | Type   | Default | Remark                                                                                        |
|-------------|--------------|--------|---------|-----------------------------------------------------------------------------------------------|
| Read Query  | `readQuery`  | STRING | (empty) | `SELECT` query for reading the point value                                                    |
| Write Query | `writeQuery` | STRING | (empty) | `UPDATE`/`INSERT` using a single `?` placeholder for the written value (bound as a parameter) |

::: tip Read Query takes the first column of the first row
`readQuery` is a plain `SELECT`, and the driver takes the **first column of the first row** of its result (
`rs.getObject(1)`) as the point's value—so a single-row, single-column query like
`SELECT temperature FROM sensor WHERE id = 1` is the safest form. An empty result set yields `null`. The point's data
type ([Point](../introduction/concepts/point) `pointTypeFlag`) decides how that value is parsed. `readQuery` is required
on a point (enforced by `validatePoint()`); without it, point validation fails. `writeQuery` is required only when that
point is to be written.
:::

### Write command attributes (`command-attribute`)

This attribute can be configured on the write command, but is not consumed by the implementation:

| Attribute     | code           | Type   | Default | Remark                               |
|---------------|----------------|--------|---------|--------------------------------------|
| Execute Query | `executeQuery` | STRING | (empty) | SQL query to execute for the command |

::: warning executeQuery is currently not consumed by the implementation
Writing a value goes through the **point's `writeQuery`**: `write()` reads the `point-attribute` `writeQuery`, binds the
command parameter with `setString(1, value)` into the single `?` placeholder, and executes the `UPDATE`/`INSERT`. The
`command-attribute` `executeQuery` is kept only as a configuration item—nothing in the current driver code reads or
executes it. There is no separate "run a SQL statement directly by command" path; writing always goes through
`writeQuery`. Code is the source of truth.
:::

## Troubleshooting

MySQL onboarding failures mostly cluster around connection, account permissions, query targeting, and field types. Work
through them in order:

1. **Can't connect (device stays offline)**. First confirm `host:port` is reachable: `telnet <host> 3306` or
   `nc -vz <host> 3306`. The health check decides online via `conn.isValid(5)` (whether a valid connection can be
   obtained within 5 seconds); a failed connect or heartbeat reports offline. Common root causes: the database not
   listening on the public/container network, a firewall, or a `bind-address` restriction.

2. **Connects but is rejected (account / permission / SSL)**. Confirm `username`/`password` are correct and the account
   is authorized to connect from the driver's host (MySQL accounts are scoped as `user@host`; `root@localhost` cannot
   connect remotely). The URL the driver builds carries `useSSL=false&allowPublicKeyRetrieval=true`—if the target
   database enforces SSL or disables public key retrieval, the connect phase fails. A failed connect throws
   `ConnectorException` and invalidates that device's pool, which is rebuilt on the next cycle.

3. **No value / wrong row returned**. The driver only takes the first column of the first row, so `readQuery` must
   reliably pinpoint the target row. An empty result set yields `null`; when multiple rows return, only the first is
   used and may not be the row you meant. Write the `WHERE` primary-key condition fully so you don't pick the wrong row
   as the table grows.

4. **Value / type mismatch**. The point's `pointTypeFlag` decides how the returned string is parsed. Configuring a text
   column as a `FLOAT` point, or treating a date/enum column as numeric, can fail parsing. Use `CAST`/`CONVERT` in
   `readQuery`, or select only the target numeric column, so the returned value matches the point's type.

5. **Write command returns failure**. Writing requires exactly one `?` placeholder in `writeQuery` and a statement
   targeting a writable table and row. `write()` treats "affected rows > 0" as success—if the `WHERE` condition matches
   no rows, `executeUpdate()` returns 0 and the write is judged failed. Run the same `UPDATE` by hand in the database
   first to confirm it hits a row. A failed write throws `WritePointException` and invalidates the pool.

6. **Slow SQL drags down collection**. A `readQuery` that full-scans a large table or lacks an index may not return
   within the polling cycle, or may approach `queryTimeout`. Indexing the predicate columns and selecting only the
   necessary columns is a better fix than simply raising the timeout.

::: warning Write Query uses a `?` placeholder, not `${value}`
When writing, `writeQuery` uses a **single** `?` placeholder for the value (e.g.
`UPDATE sensor SET temperature = ? WHERE id = 1`), bound by the driver via `PreparedStatement.setString(1, value)`—this
is prepared-statement parameter binding, not string concatenation, so a malicious value cannot alter the statement
structure (no SQL injection). Do not concatenate the value into the SQL by hand, and do not use template syntax like
`${value}`: it would neither be substituted nor give you injection protection.
:::

## How it lands in IoT DC3

- **`dc3.driver.code`**: `MysqlDriver` (type `DRIVER_CLIENT`, actively connects to the database and issues queries).
  This is a stable routing identifier—do not change it casually.
- **Read capability**: ✓ implemented. `read()` executes the point's `readQuery` and takes the first column of the first
  row as the point value.
- **Write capability**: ✓ implemented. `write()` executes the point's `writeQuery`, binding the written value as a `?`
  prepared-statement parameter; affected rows > 0 means success.
- **Subscribe/report**: — not supported. MySQL is request-response; the driver only actively queries/writes and never
  passively receives pushes. This matches the `✓ / ✓ / —` for MySQL in the [driver capability matrix](./matrix).
- **Polling interval**: default cron `0/30 * * * * ?` (read once every 30 seconds), configured under `schedule.read` in
  the driver's `application.yml`; there is also a `custom` schedule with default cron `0/5 * * * * ?` (every 5 seconds),
  but the base class `schedule()` is an empty implementation and database drivers do not use it.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`; the
  verdict relies on `conn.isValid(5)`. See [Device](../introduction/concepts/device) for the online-state mechanism.

::: info Implementation status: available
This driver is a **complete implementation** (not a skeleton). Reading, writing, the health check, the per-device cached
HikariCP pool, and pool invalidation-and-rebuild on failure are all in place, reusing the tested
`AbstractJdbcDriverCustomService` base class. The only thing to note is that the `command-attribute` `executeQuery` is
reserved but not consumed by the code—writing always goes through the point's `writeQuery` (see the warning above).
:::

### Minimal onboarding example

Onboard the `temperature` column of the `id=1` row in a `sensor` table as a temperature point:

1. Create a [Device](../introduction/concepts/device) with `MySQL Driver`, and set the driver attributes
   `host=192.168.1.10`, `port=3306`, `database=iot`, `username=root`, `password=******`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attribute
   `readQuery=SELECT temperature FROM sensor WHERE id = 1`.
3. Start the driver, and within 30 seconds the queried temperature shows up in
   the [PointValue](../introduction/concepts/point-value).
4. If the point should be writable, add `writeQuery=UPDATE sensor SET temperature = ? WHERE id = 1` to its point
   attributes and configure a write [Command](../introduction/concepts/command) for it.

::: tip One driver instance can serve multiple databases
A single MySQL driver process can serve multiple devices: each device connects to its own database per its driver
attributes and holds its own connection pool (cached by device ID). When device metadata is deleted/updated, the
corresponding pool is closed and rebuilt on demand.
:::

## Further reading

- [Drivers overview](./index) — entry point and categories for all drivers
- [Driver capability matrix](./matrix) — read/write/subscribe at a glance, including the MySQL row
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Time-Series Data & Stream Processing](../foundations/data-pipeline) — how PointValues are stored, computed, and
  queried after entering the platform
- [PostgreSQL Driver](./postgresql) — another database data source on the same JDBC base class
