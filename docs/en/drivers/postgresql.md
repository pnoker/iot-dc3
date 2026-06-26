---
title: PostgreSQL Driver
---

# PostgreSQL Driver

`dc3-driver-postgresql` onboards a PostgreSQL database into IoT DC3 as a data source: it runs a `SELECT` per polling cycle and uses the queried value as the reading, and supports writing values via the `UPDATE`/`INSERT` write query configured on a point. By the end you can treat columns of an existing table as polled points, and understand the boundaries of the read and write SQL paths.

## Protocol background

Not all data comes from a fieldbus device. A lot of business data, historical data, and results accumulated by third-party systems simply live in a PostgreSQL table—an MES work-order status, the settled cumulative quantity from a metering system, a database view that an upstream platform exposes and nothing else. This kind of data has no fieldbus protocol like Modbus or OPC UA to ride on, yet it is a genuine extension of the device world and needs to be brought into the unified point model to be managed and consumed.

PostgreSQL is an open-source object-relational database (ORDBMS) known for strict SQL-standard compliance, strong transactions (MVCC), and rich data types (JSON/JSONB, arrays, ranges, geometry). It listens on port `5432` by default, and clients connect over standard JDBC (`org.postgresql.Driver`). Onboarding it into an IoT platform essentially means "treating a database as a class of device": one row, one column of a table becomes the source of a point's value.

In the four-layer IoT reference architecture, a database-bridge driver straddles the boundary between the network and platform layers—it parses no fieldbus frames, but re-admits "data already landed in a table" into the polling pipeline as points. For how this path connects to time-series storage and stream processing, see [Time-Series Data & Stream Processing](../foundations/data-pipeline).

::: info How it differs from fieldbus drivers
Drivers like Modbus and OPC UA face "live physical quantities"—registers change in real time, and what you read is the current field value. A database driver faces "data already persisted"—what you read is a snapshot written by some upstream system, possibly already stale. When treating a column as a point, align the polling cadence with the upstream write cadence, or you will repeatedly read the same old value.
:::

## Attribute configuration

Onboarding uses three kinds of attributes: **driver attributes** (device level—which database, which account), **point attributes** (the read/write SQL per point), and **command attributes** (a reserved item, currently inert). Every default comes from the driver's `application.yml`; you fill in concrete values on the [Device](../introduction/concepts/device)/[Point](../introduction/concepts/point). For where attributes come from across the three layers, see [Attributes & Config](../introduction/concepts/attribute-config).

### Driver attributes (device-level `driver-attribute`)

When onboarding a PostgreSQL database, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). They decide which database to connect to, which account to use, and the connection timeout:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | PostgreSQL host |
| Port | `port` | INT | `5432` | PostgreSQL port |
| Database | `database` | STRING | (empty) | Database name to connect to |
| Username | `username` | STRING | `root` | Connection account |
| Password | `password` | STRING | (empty) | Account password |
| Query Timeout | `queryTimeout` | INT | `30` | Connection-acquire timeout (seconds); used as the Hikari pool `connectionTimeout` |

The driver builds the JDBC URL from `host`, `port`, and `database` (in the form `jdbc:postgresql://host:port/database`). Device validation (`validate()`) checks that all five of `host`, `port`, `database`, `username`, `password` are present—any empty one fails; `queryTimeout` is optional and defaults to `30` seconds. Each device maps to its own HikariCP connection pool inside the driver (`maximumPoolSize=5`, `minimumIdle=1`), cached and reused by device ID.

### Point attributes (`point-attribute`)

On each polled [Point](../introduction/concepts/point), fill in its read/write SQL:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Read Query | `readQuery` | STRING | (empty) | `SELECT` statement for reading the point value |
| Write Query | `writeQuery` | STRING | (empty) | `UPDATE`/`INSERT` for writing, using a single `?` placeholder for the written value (bound as a parameter) |

::: tip Read Query takes the first column of the first row
`readQuery` is a plain `SELECT`; after running it the driver takes the **first column of the first row** (`rs.getObject(1)`) as the point's value, then `toString()`s it for the point to parse by its data type ([Point](../introduction/concepts/point) `pointTypeFlag`). So a single-row, single-column query like `SELECT temperature FROM sensor WHERE id = 1` is the safest form. `readQuery` is required on a point; point validation (`validatePoint()`) fails without it.
:::

### Command attributes (`command-attribute`)

The write command keeps one attribute, but it is not consumed by the implementation:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Execute Query | `executeQuery` | STRING | (empty) | SQL to execute for the command (reserved) |

::: warning `executeQuery` is currently not consumed by the implementation
Writing a value actually goes through the point's `writeQuery`: the driver's `write()` reads the `point-attribute` `writeQuery` and runs the `UPDATE`/`INSERT` with prepared-statement parameter binding. The `executeQuery` on `command-attribute` is kept only as a configuration item—no code in the current driver reads or executes it; there is no separate "execute a SQL string per command" path. To write values, put the SQL in the point's `writeQuery`; configuring `executeQuery` has no effect.
:::

### Polling & health

- **Polling interval**: default cron `0/30 * * * * ?` (read once every 30 seconds).
- **Custom interval**: the driver also has a custom schedule, default cron `0/5 * * * * ?` (every 5 seconds); the JDBC database driver's `schedule()` is an empty implementation, so this schedule currently does nothing.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`. The health check borrows a connection from the pool and uses `conn.isValid(5)` to decide reachability—if it can't connect, the device goes offline. See [Device](../introduction/concepts/device) for the online-state mechanism.

## Troubleshooting

When a database-bridge onboarding fails, the cause is rarely the protocol itself—it is the connection parameters, the SQL shape, or the upstream database's state. Ordered by frequency:

::: warning Device stays offline / pool won't come up
The health check decides with `conn.isValid(5)`. If a device is always offline, first confirm `host`/`port` is reachable (don't use `localhost` from inside a container to reach the host), `database` name case is correct, and the `username`/`password` account can log into that database. PostgreSQL is also gated by `pg_hba.conf`—if the source IP or auth method isn't allowed it rejects the connection outright; that error is on the database side, so open it up there.
:::

::: warning Read Query must be read-only and resolve to a single value
The driver takes only the first column of the first row of the `readQuery` result, so the query should return a single row and column and pinpoint the target row with a `WHERE` primary-key condition. With multiple rows/columns only the first is used and may not be the row you meant; with no result the point value is `null`. Never put `UPDATE`/`DELETE` in `readQuery`—reading is a read-only path, and writing there corrupts data. Write the filter fully so you don't pick the wrong row as the table's data changes.
:::

::: warning Write Query uses a `?` placeholder, not `${value}`
When writing, `writeQuery` uses a single `?` for the value (e.g. `UPDATE sensor SET temperature = ? WHERE id = 1`), and the driver binds the command parameter as a JDBC parameter via `ps.setString(1, value)`—prepared-statement binding, not string concatenation, inherently safe from SQL injection. Do not concatenate the value by hand, and do not use template syntax like `${value}`: it would neither be substituted nor give you injection protection. A write is judged successful by "affected rows > 0", so an `UPDATE` that hits 0 rows (the `WHERE` matched nothing) counts as a write failure.
:::

::: tip Identifier case follows PostgreSQL rules
PostgreSQL folds unquoted identifiers to lowercase and matches double-quoted ones literally. A wrong-case `database` won't connect; table/column names in `readQuery`/`writeQuery` that don't match the case used at creation will report "does not exist". Fill them in with the real case, and double-quote identifiers in the SQL when needed.
:::

::: tip Query timeout / slow SQL
`queryTimeout` (default 30 seconds) is used as the Hikari pool `connectionTimeout`—the upper bound on waiting to acquire a connection. For large tables or slow SQL, optimize the SQL, add indexes, and narrow the `WHERE` rather than simply raising the timeout—a slow query holds a pool connection (only 5 exist) and drags down polling of the whole batch.
:::

## How it lands in IoT DC3

- **Driver name / code**: `PostgreSQL Driver` / `PostgresqlDriver` (`dc3.driver.code` is a stable routing identifier the platform routes messages by—don't change it casually).
- **Type**: `DRIVER_CLIENT`—the driver actively connects and issues queries; it does not listen for pushes.
- **Read / write / subscribe**: consistent with the [driver capability matrix](./matrix)—read ✓, write ✓, subscribe —. Reads take the first value of a `SELECT`; writes use the prepared binding of `writeQuery`; a database has no change subscription, so values are pulled by periodic polling.

::: info Implementation status: available (not a skeleton)
The PostgreSQL driver is an **available implementation**, not a skeleton. Connection, read, write, and health check are all provided by the shared `dc3-common-sql` abstract base service `AbstractJdbcDriverCustomService` (the same logic shared with MySQL, Oracle, and SQL Server); the PostgreSQL subclass only supplies JDBC URL construction, the driver class name `org.postgresql.Driver`, and the default port `5432`. The one caveat is the `command-attribute` `executeQuery`, which is reserved but unwired (see Attribute configuration above).
:::

### Minimal onboarding example

Onboard the `temperature` column of the `id=1` row in a `sensor` table as a temperature point:

1. Create a [Device](../introduction/concepts/device) with `PostgreSQL Driver`, and set the driver attributes `host=192.168.1.10`, `port=5432`, `database=iot`, `username=root`, `password=******`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attribute `readQuery=SELECT temperature FROM sensor WHERE id = 1`.
3. Start the driver, and within 30 seconds the queried temperature shows up in the [PointValue](../introduction/concepts/point-value).

For a writable point, also set `writeQuery=UPDATE sensor SET temperature = ? WHERE id = 1` on the same point and make its `rwFlag` writable. For a complete walkthrough, see [Device Onboarding](../operation/device-onboarding).

## Further reading

- [Drivers Overview](./index) — categorization and selection map of all drivers
- [Driver Capability Matrix](./matrix) — read/write/subscribe capabilities at a glance
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Time-Series Data & Stream Processing](../foundations/data-pipeline) — how polled point values land in the time-series store and feed stream processing
- [MySQL Driver](./mysql) — another JDBC database data source with an identical configuration structure
