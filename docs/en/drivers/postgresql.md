---
title: PostgreSQL Driver
---

# PostgreSQL Driver

> **`dc3-driver-postgresql` onboards a PostgreSQL database into IoT DC3 as a data source**—it periodically runs `SELECT` queries and uses the queried value as the reading, and supports writing values into the database via the `UPDATE`/`INSERT` write query configured on a point.

Not all data comes from a fieldbus device: a lot of business data, historical data, and results accumulated by third-party systems simply live in a PostgreSQL table. This driver acts as a database client ([Driver](../introduction/concepts/driver) type `DRIVER_CLIENT`), connecting to a PostgreSQL database over JDBC (`org.postgresql.Driver`) and reading/writing values by the SQL configured on each [Point](../introduction/concepts/point). JDBC connection handling and query execution are provided by the shared `dc3-common-sql` abstract base service. It fits: treating columns in an existing business database as polled points, integrating upstream systems that only expose a database view, and pulling external system results into the platform on a schedule.

Two driver-specific concepts that the configuration tables below rely on:

- **Read Query**: a `SELECT` statement configured on the point; the driver runs it on each polling cycle and takes the result as the [PointValue](../introduction/concepts/point-value).
- **Write Query**: an `UPDATE`/`INSERT` statement configured on the point, using a single `?` placeholder for the value to write—when a value is written to the point, the written value is bound as that parameter.

## Driver name / code / type

- **Driver name / code**: `PostgreSQL Driver` / `PostgresqlDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively connects to the database and issues queries)

## Driver configuration (device-level `driver-attribute`)

When onboarding a PostgreSQL database, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). They decide which database to connect to, which account to use, and the query timeout:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | PostgreSQL host |
| Port | `port` | INT | `5432` | PostgreSQL port |
| Database | `database` | STRING | (empty) | PostgreSQL database name |
| Username | `username` | STRING | `root` | PostgreSQL username |
| Password | `password` | STRING | (empty) | PostgreSQL password |
| Query Timeout | `queryTimeout` | INT | `30` | SQL query timeout in seconds |

The driver builds the JDBC URL from `host`, `port`, and `database` (in the form `jdbc:postgresql://host:port/database`); `database`, `username`, and `password` are required—if any is missing, device configuration validation fails.

## Point configuration (`point-attribute`)

On each polled [Point](../introduction/concepts/point), fill in its read/write SQL:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Read Query | `readQuery` | STRING | (empty) | SQL SELECT query for reading point value |
| Write Query | `writeQuery` | STRING | (empty) | SQL UPDATE/INSERT using a single ? placeholder for the written value (bound as a parameter) |

::: tip Read Query takes the first value from the result
`readQuery` is a plain `SELECT`, and the driver takes the first value of its result as the point's value—so a single-row, single-column query like `SELECT temperature FROM sensor WHERE id = 1` is the safest form. The point's data type ([Point](../introduction/concepts/point) `pointTypeFlag`) decides how that value is parsed. `readQuery` is required on a point; without it, point validation fails.
:::

## Write command configuration (`command-attribute`)

A writable point also needs this on its write command:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Execute Query | `executeQuery` | STRING | (empty) | SQL query to execute for command |

::: warning `executeQuery` is currently not consumed by the implementation
Writing a value to a point actually goes through the point's `writeQuery` (see Point configuration above): the driver's `write()` only reads and executes `writeQuery`. The `executeQuery` attribute is kept in the configuration, but no code in the current driver implementation reads or executes it—configuring it has no effect. To write values, put the SQL in the point's `writeQuery`.
:::

## Polling & health

- **Polling interval**: default cron `0/30 * * * * ?` (read once every 30 seconds).
- **Custom interval**: the driver also has a custom schedule, default cron `0/5 * * * * ?` (every 5 seconds), used by the driver's custom logic.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`—see [Device](../introduction/concepts/device) for the online-state mechanism.

## Minimal onboarding example

Onboard the `temperature` column of the `id=1` row in a `sensor` table as a temperature point:

1. Create a [Device](../introduction/concepts/device) with `PostgreSQL Driver`, and set the driver attributes `host=192.168.1.10`, `port=5432`, `database=iot`, `username=root`, `password=******`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attribute `readQuery=SELECT temperature FROM sensor WHERE id = 1`.
3. Start the driver, and within 30 seconds the queried temperature shows up in the [PointValue](../introduction/concepts/point-value).

## Pitfalls

::: warning Read Query must be read-only and resolve to a single value
The driver takes the first value of the `readQuery` result, so the query should return a single row and column and reliably pinpoint the target row (with a `WHERE` primary-key condition). When it returns multiple rows/columns, only the first is used and may not be the row you meant; never put `UPDATE`/`DELETE` in `readQuery`—reading is a read-only path. Write the filter fully so you don't pick the wrong row as the table's data changes.
:::

::: warning Write Query uses a `?` placeholder, not `${value}`
When writing, `writeQuery` uses a single `?` placeholder for the value (e.g. `UPDATE sensor SET temperature = ? WHERE id = 1`), and the driver binds the command parameter as a JDBC parameter—this is prepared-statement parameter binding, not string concatenation. Do not concatenate the value into the SQL by hand, and do not use template syntax like `${value}`: it would neither be substituted nor give you injection protection.
:::

::: tip Identifier case follows PostgreSQL rules
PostgreSQL is case-sensitive for double-quoted identifiers: an unquoted identifier is folded to lowercase, while a double-quoted one matches literally. A wrong-case `database` won't connect, and table/column names in `readQuery` that don't match the actual case won't be found—fill them in with the real case used at creation time, and double-quote identifiers in the SQL when needed.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes & Config](../introduction/concepts/attribute-config) — where attributes like `host` / `readQuery` come from across the three layers
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [MySQL Driver](./mysql) — another JDBC database data source with an identical configuration structure
