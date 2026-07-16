---
title: Oracle Driver
---

<script setup>
import OracleDiagram from '../../.vitepress/theme/components/OracleDiagram.vue'
</script>


# Oracle Driver

`dc3-driver-oracle` onboards an Oracle database into IoT DC3 as a data source: acting as a database client, it runs a
`SELECT` on each polling cycle and uses the queried value as the reading, and supports writing values into the database
via the `UPDATE`/`INSERT` write query configured on the point. After reading this you can set the connection parameters
on a [Device](../introduction/concepts/device) (including Oracle's distinctive SID / Service Name connection methods),
the read/write SQL on each [Point](../introduction/concepts/point), and pinpoint common "can't connect / no value /
write fails" problems.

> You are here: a driver that onboards an existing database as a data source. Not all data comes from a fieldbus
> device—much business data, historical data, and third-party results simply live in an Oracle table.

## Protocol background

Oracle Database is the archetypal enterprise-grade relational database, first released in 1979, using SQL as its query
language and organizing data into tables/rows/columns; it carries a great deal of core business systems and historical
archives in finance, power, and manufacturing. In IoT scenarios it is often not a "field device" but a hub where data
converges: business systems such as MES/ERP, third-party platforms, and historical databases all tend to drop their
results into an Oracle table for downstream consumption. Onboard such a table as a data source, and the platform can
poll its columns into [PointValues](../introduction/concepts/point-value) just like it polls a real device.

This driver acts as a database client ([Driver](../introduction/concepts/driver) type `DRIVER_CLIENT`), connecting to an
Oracle database over JDBC (`ojdbc11`, driver class `oracle.jdbc.OracleDriver`) and reading/writing values by the SQL
configured on each [Point](../introduction/concepts/point). Its communication model is classic **request-response**—the
driver, as a client, actively issues queries; the database never pushes, so collection is driven by cron polling. The
shared logic for JDBC connections, connection pooling, and SQL execution lives in the abstract base class
`AbstractJdbcDriverCustomService` (`dc3-common-sql` module), reused by all four database drivers (MySQL, PostgreSQL,
Oracle, SQL Server), each of which only supplies JDBC URL construction and the driver class name.

Seen through the [IoT data pipeline](../foundations/data-pipeline), database drivers sit beyond the network layer, at
the entry point of "moving externally-structured data into the platform": the sensing layer and fieldbus protocols
digitize physical quantities and the network layer delivers them, while the Oracle driver onboards results already
accumulated in a database into the same pipeline—ultimately stored, queried, and consumed by alarms and AI just like
values collected from real devices.

Oracle's biggest difference from the other databases is **how it identifies a database instance**: Oracle locates an
instance by either SID (System Identifier) or Service Name, and the driver builds a differently-shaped JDBC URL
accordingly. Three driver-specific concepts that the configuration tables below rely on:

<OracleDiagram lang="en" />

- **Connection Type**: `SID` or `ServiceName`. The driver builds a differently-shaped JDBC URL accordingly (see the
  diagram above); the two correspond to Oracle's different naming methods.
- **Read Query**: a `SELECT` configured on the point; the driver runs it on each polling cycle and takes the **first
  column of the first row** of the result as the point's value.
- **Write Query**: an `UPDATE`/`INSERT` configured on the point, using a single `?` placeholder for the value to
  write—when a write command fires, the command parameter is bound via prepared-statement parameter binding.

## Attribute configuration

Onboarding an Oracle database requires filling in [attributes](../introduction/concepts/attribute-config) at three
levels: device-level connection parameters (`driver-attribute`), each polled point's read/write SQL (`point-attribute`),
and one reserved attribute on the write command (`command-attribute`). The attributes, types, and defaults below are
taken from the driver's `application.yml` (`dc3-driver-oracle` module).

### Driver attributes (device-level `driver-attribute`)

Driver attributes answer "which database to connect to, which account to use, which connection method, and the query
timeout". Fill in one set per Oracle database on the [Device](../introduction/concepts/device):

| Attribute       | code             | Type   | Default     | Remark                                                       |
|-----------------|------------------|--------|-------------|--------------------------------------------------------------|
| Host            | `host`           | STRING | `localhost` | Oracle host IP or hostname                                   |
| Port            | `port`           | INT    | `1521`      | Oracle port (standard 1521)                                  |
| Database        | `database`       | STRING | (empty)     | Oracle database name                                         |
| Username        | `username`       | STRING | `root`      | Oracle username                                              |
| Password        | `password`       | STRING | (empty)     | Oracle password                                              |
| Query Timeout   | `queryTimeout`   | INT    | `30`        | SQL query timeout in seconds                                 |
| Connection Type | `connectionType` | STRING | `SID`       | Connection method `[SID, ServiceName]`                       |
| SID             | `sid`            | STRING | `ORCL`      | Oracle SID (used when `connectionType=SID`)                  |
| Service Name    | `serviceName`    | STRING | (empty)     | Oracle service name (used when `connectionType=ServiceName`) |

The driver builds the JDBC URL by `connectionType`: with `SID` it uses `sid` to form `jdbc:oracle:thin:@host:port:sid` (
default `sid=ORCL`), and with `ServiceName` it uses `serviceName` to form `jdbc:oracle:thin:@//host:port/serviceName`.
Configuration validation (`validate()`) requires all six of `host`, `port`, `database`, `username`, `password`, and
`connectionType`, and any missing field fails. Whether `sid` or `serviceName` takes effect depends on `connectionType`
—see Troubleshooting below. The driver caches one HikariCP connection pool per device ID (one pool per device, max 5
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

Oracle onboarding failures mostly cluster around the connection method (picking the wrong SID/Service Name), network,
account permissions, and query targeting. Work through them in order:

1. **Wrong connection method (mixing SID and Service Name)**. With `connectionType=SID` the driver uses only `sid` to
   build the URL and ignores `serviceName`; with `connectionType=ServiceName` it uses only `serviceName` and ignores
   `sid`, and an empty `serviceName` throws immediately (`getRequiredConfig`). First confirm whether your instance
   exposes a SID or a service name (`lsnrctl status` shows the services registered with the listener), fill in that one,
   and don't fill both expecting the driver to pick automatically.

2. **Can't connect (device stays offline)**. First confirm `host:port` is reachable: `telnet <host> 1521` or
   `nc -vz <host> 1521`. The health check decides online via `conn.isValid(5)` (whether a valid connection can be
   obtained within 5 seconds); a failed connect or heartbeat reports offline. Common root causes: the listener not
   started or not registering that instance, a firewall blocking 1521, or a broken container network.

3. **Connects but is rejected (account / permission / instance name mismatch)**. Confirm `username`/`password` are
   correct, the account is not locked, and it has `CREATE SESSION`. `ORA-12505`/`ORA-12514` usually means the SID or
   Service Name is wrong (the listener received the request but found no matching instance/service); `ORA-01017` is a
   bad account or password. A failed connect throws `ConnectorException` and invalidates that device's pool, which is
   rebuilt on the next cycle.

4. **No value / wrong row returned**. The driver only takes the first column of the first row, so `readQuery` must
   reliably pinpoint the target row. An empty result set yields `null`; when multiple rows return, only the first is
   used and may not be the row you meant. Write the `WHERE` primary-key condition fully so you don't pick the wrong row
   as the table grows; to force a single row, use `WHERE ... AND ROWNUM = 1` or `FETCH FIRST 1 ROW ONLY`.

5. **Value / type mismatch**. The point's `pointTypeFlag` decides how the returned string is parsed. Configuring a text
   column as a `FLOAT` point, or treating a `NUMBER`/`DATE` column as another type, can fail parsing. Use `TO_CHAR`/
   `CAST` in `readQuery`, or select only the target numeric column, so the returned value matches the point's type.

6. **Write command returns failure**. Writing requires exactly one `?` placeholder in `writeQuery` and a statement
   targeting a writable table and row. `write()` treats "affected rows > 0" as success—if the `WHERE` condition matches
   no rows, `executeUpdate()` returns 0 and the write is judged failed. Run the same `UPDATE` by hand in the database
   first to confirm it hits a row. A failed write throws `WritePointException` and invalidates the pool.

::: warning Connection Type decides whether to fill SID or Service Name
With `connectionType=SID` the driver builds `jdbc:oracle:thin:@host:port:sid` using `sid` (default `sid=ORCL`); with
`connectionType=ServiceName` it instead uses `serviceName` to build `jdbc:oracle:thin:@//host:port/serviceName`, in
which case `serviceName` is required and throws before connecting if missing. The two correspond to Oracle's different
naming methods—fill in whichever your instance actually exposes.
:::

::: warning Write Query uses a `?` placeholder, not `${value}`
When writing, `writeQuery` uses a **single** `?` placeholder for the value (e.g.
`UPDATE sensor SET temperature = ? WHERE id = 1`), bound by the driver via `PreparedStatement.setString(1, value)`—this
is prepared-statement parameter binding, not string concatenation, so a malicious value cannot alter the statement
structure (no SQL injection). Do not concatenate the value into the SQL by hand, and do not use template syntax like
`${value}`: it would neither be substituted nor give you injection protection.
:::

## How it lands in IoT DC3

- **`dc3.driver.code`**: `OracleDriver` (type `DRIVER_CLIENT`, actively connects to the database and issues queries).
  This is a stable routing identifier—do not change it casually.
- **Read capability**: ✓ implemented. `read()` executes the point's `readQuery` and takes the first column of the first
  row as the point value.
- **Write capability**: ✓ implemented. `write()` executes the point's `writeQuery`, binding the written value as a `?`
  prepared-statement parameter; affected rows > 0 means success.
- **Subscribe/report**: — not supported. Oracle is request-response; the driver only actively queries/writes and never
  passively receives pushes. This matches the `✓ / ✓ / —` for Oracle in the [driver capability matrix](./matrix).
- **Polling interval**: default cron `0/30 * * * * ?` (read once every 30 seconds), configured under `schedule.read` in
  the driver's `application.yml`; there is also a `custom` schedule with default cron `0/5 * * * * ?` (every 5 seconds),
  but the base class `schedule()` is an empty implementation and database drivers do not use it.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`; the
  verdict relies on `conn.isValid(5)`. See [Device](../introduction/concepts/device) for the online-state mechanism.

::: info Implementation status: available
This driver is a **complete implementation** (not a skeleton). Oracle's two SID / Service Name JDBC URL forms, reading,
writing, the health check, the per-device cached HikariCP pool, and pool invalidation-and-rebuild on failure are all in
place, reusing the tested `AbstractJdbcDriverCustomService` base class. The only thing to note is that the
`command-attribute` `executeQuery` is reserved but not consumed by the code—writing always goes through the point's
`writeQuery` (see the warning above).
:::

### Minimal onboarding example

Onboard the `temperature` column of the `id=1` row in a `sensor` table as a temperature point (connecting to an `ORCL`
instance by SID):

1. Create a [Device](../introduction/concepts/device) with `Oracle Driver`, and set the driver attributes
   `host=192.168.1.10`, `port=1521`, `database=iot`, `username=root`, `password=******`, `connectionType=SID`,
   `sid=ORCL`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attribute
   `readQuery=SELECT temperature FROM sensor WHERE id = 1`.
3. Start the driver, and within 30 seconds the queried temperature shows up in
   the [PointValue](../introduction/concepts/point-value).
4. If the point should be writable, add `writeQuery=UPDATE sensor SET temperature = ? WHERE id = 1` to its point
   attributes and configure a write [Command](../introduction/concepts/command) for it.
5. If your instance uses a service name, switch `connectionType` to `ServiceName` and fill in `serviceName` (leave `sid`
   empty).

::: tip One driver instance can serve multiple databases
A single Oracle driver process can serve multiple devices: each device connects to its own database per its driver
attributes and holds its own connection pool (cached by device ID). When device metadata is deleted/updated, the
corresponding pool is closed and rebuilt on demand.
:::

## Further reading

- [Drivers overview](./index) — entry point and categories for all drivers
- [Driver capability matrix](./matrix) — read/write/subscribe at a glance, including the Oracle row
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Time-Series Data & Stream Processing](../foundations/data-pipeline) — how PointValues are stored, computed, and
  queried after entering the platform
- [MySQL Driver](./mysql) — another database data source on the same JDBC base class
