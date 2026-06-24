---
title: SQL Server Driver
---

# SQL Server Driver

> **`dc3-driver-sqlserver` onboards a Microsoft SQL Server database into IoT DC3 as a data source**—it periodically runs `SELECT` queries and uses the queried value as the reading, and supports write commands that push values into the database via `UPDATE`/`INSERT`.

Not all data comes from a fieldbus device: a lot of business data, historical data, and results accumulated by third-party systems simply live in a SQL Server table. This driver acts as a database client ([Driver](../introduction/concepts/driver) type `DRIVER_CLIENT`), connecting to a SQL Server instance over JDBC (`com.microsoft.sqlserver.jdbc.SQLServerDriver`) and reading/writing values by the SQL configured on each [Point](../introduction/concepts/point). JDBC connection handling and query execution are provided by the shared `dc3-common-sql` abstract base service. It fits: treating columns in an existing business database as polled points, integrating upstream systems that only expose a database view, and pulling external system results into the platform on a schedule.

Two driver-specific concepts that the configuration tables below rely on:

- **Read Query**: a `SELECT` statement configured on the point; the driver runs it on each polling cycle and takes the result as the [PointValue](../introduction/concepts/point-value).
- **Write Query**: an `UPDATE`/`INSERT` statement configured on the point, using a single `?` placeholder for the value to write—when a write command fires, the command parameter is bound as that parameter.

## Driver name / code / type

- **Driver name / code**: `SQL Server Driver` / `SqlserverDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively connects to the database and issues queries)

## Driver configuration (device-level `driver-attribute`)

When onboarding a SQL Server database, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). They decide which database to connect to, which account to use, the query timeout, and whether the connection is encrypted:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | SQL Server host |
| Port | `port` | INT | `1433` | SQL Server port |
| Database | `database` | STRING | (empty) | SQL Server database name |
| Username | `username` | STRING | `root` | SQL Server username |
| Password | `password` | STRING | (empty) | SQL Server password |
| Query Timeout | `queryTimeout` | INT | `30` | SQL query timeout in seconds |
| Encrypt | `encrypt` | STRING | `false` | SQL Server encrypt connection |
| Trust Server Certificate | `trustServerCertificate` | STRING | `true` | SQL Server trust server certificate |

The driver builds the JDBC URL from these attributes, in the form `jdbc:sqlserver://host:port;databaseName=...;encrypt=...;trustServerCertificate=...;`. Of these, `database`, `username`, and `password` are required—if any is missing, device configuration validation fails.

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

::: warning Writes actually go through `writeQuery`; `executeQuery` is currently not consumed
The driver's write path (`write()`) only reads the point's `writeQuery`: it binds the command parameter into that query's `?` placeholder and runs the `UPDATE`/`INSERT`. The `command-attribute` `executeQuery` here is registered as a driver attribute, but no driver code currently reads or executes it—configuring it has no effect. To make a write command land in the database, configure the write SQL on the point's `writeQuery`.
:::

## Polling & health

- **Polling interval**: default cron `0/30 * * * * ?` (read once every 30 seconds).
- **Custom interval**: the driver also has a custom schedule, default cron `0/5 * * * * ?` (every 5 seconds), used by the driver's custom logic.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`—see [Device](../introduction/concepts/device) for the online-state mechanism.

## Minimal onboarding example

Onboard the `temperature` column of the `id=1` row in a `sensor` table as a temperature point:

1. Create a [Device](../introduction/concepts/device) with `SQL Server Driver`, and set the driver attributes `host=192.168.1.10`, `port=1433`, `database=iot`, `username=sa`, `password=******` (keep the default `encrypt=false` while testing over a trusted network).
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attribute `readQuery=SELECT temperature FROM sensor WHERE id = 1`.
3. Start the driver, and within 30 seconds the queried temperature shows up in the [PointValue](../introduction/concepts/point-value).

## Pitfalls

::: warning encrypt and trustServerCertificate must be set together
The SQL Server JDBC driver performs a TLS handshake and validates the server certificate when `encrypt=true`; if the server uses a self-signed certificate, validation fails and the connection errors out. When enabling encryption (`encrypt=true`) against an instance with a self-signed certificate, you must also set `trustServerCertificate=true` to skip certificate-chain validation. Both attributes are STRING type—fill the string `"true"`/`"false"`, not a boolean. For plaintext testing on a trusted network, just keep the default `encrypt=false`.
:::

::: warning Read Query must be read-only and resolve to a single value
The driver takes the first value of the `readQuery` result, so the query should return a single row and column and reliably pinpoint the target row (with a `WHERE` primary-key condition). When it returns multiple rows/columns, only the first is used and may not be the row you meant; never put `UPDATE`/`DELETE` in `readQuery`—reading is a read-only path. Write the filter fully so you don't pick the wrong row as the table's data changes.
:::

::: warning Write Query uses a `?` placeholder, not `${value}`
When writing, `writeQuery` uses a single `?` placeholder for the value (e.g. `UPDATE sensor SET temperature = ? WHERE id = 1`), and the driver binds the command parameter as a JDBC parameter—this is prepared-statement parameter binding, not string concatenation. Do not concatenate the value into the SQL by hand, and do not use template syntax like `${value}`: it would neither be substituted nor give you injection protection.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes & Config](../introduction/concepts/attribute-config) — where attributes like `host` / `readQuery` come from across the three layers
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [MySQL Driver](./mysql) — another relational-database data source with the same configuration structure
