---
title: Oracle Driver
---

# Oracle Driver

> **`dc3-driver-oracle` onboards an Oracle database into IoT DC3 as a data source**—it periodically runs `SELECT` queries and treats the returned values as collected values, and supports commands that write values back with `UPDATE`/`INSERT`.

Not all data comes from on-site protocol devices: a lot of business data, historical data, and results accumulated by third-party systems already live in an Oracle table. This driver acts as a database client (a [Driver](../introduction/concepts/driver) of type `DRIVER_CLIENT`), connecting to an Oracle database over JDBC (`ojdbc11`) and reading/writing values with the SQL configured on each [Point](../introduction/concepts/point). Establishing the JDBC connection and executing queries are handled by the shared `dc3-common-sql` abstract base service. Use it to: collect a field from an existing business database as a point, integrate an upstream system that only exposes a database view, or periodically pull external-system results into the platform.

A few driver-specific concepts come up repeatedly in the config tables below:

- **Connection Type**: Oracle has two ways to identify a connection—connect by **SID**, or connect by **Service Name**. The driver builds a differently-shaped JDBC URL accordingly.
- **Read Query**: a `SELECT` statement configured on a point; the driver runs it on the collection schedule and takes the result as the [PointValue](../introduction/concepts/point-value).
- **Write Query**: an `UPDATE`/`INSERT` statement configured on a point, using a single `?` placeholder for the value to write—when a write command fires, the command argument is bound to that parameter.

## Driver Name / code / Type

- **Driver Name / code**: `Oracle Driver` / `OracleDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively connects to the database and issues queries)

## Driver Config (device-level `driver-attribute`)

When onboarding an Oracle database, fill in these [Attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). They determine which database to connect to, which account to use, and which connection method to use:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | Oracle host |
| Port | `port` | INT | `1521` | Oracle port |
| Database | `database` | STRING | (empty) | Oracle database name |
| Username | `username` | STRING | `root` | Oracle username |
| Password | `password` | STRING | (empty) | Oracle password |
| Query Timeout | `queryTimeout` | INT | `30` | SQL query timeout in seconds |
| Connection Type | `connectionType` | STRING | `SID` | Oracle connection type [SID, ServiceName] |
| SID | `sid` | STRING | `ORCL` | Oracle SID |
| Service Name | `serviceName` | STRING | (empty) | Oracle service name |

Device config validation requires `host`, `port`, `database`, `username`, `password`, and `connectionType`; it fails if any is missing. Whether `sid` or `serviceName` takes effect depends on `connectionType`—see the pitfalls below.

## Point Config (`point-attribute`)

On each collected [Point](../introduction/concepts/point), fill in its read/write SQL:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Read Query | `readQuery` | STRING | (empty) | SQL SELECT query for reading point value |
| Write Query | `writeQuery` | STRING | (empty) | SQL UPDATE/INSERT using a single ? placeholder for the written value (bound as a parameter) |

::: tip Read Query takes the first value of the result
`readQuery` is an ordinary `SELECT`; the driver takes the first value of its result as the point's value—so a query that returns a single row and single column, like `SELECT temperature FROM sensor WHERE id = 1`, is the safest form. The point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) determines how this value is parsed. `readQuery` is required on a point; point validation fails without it.
:::

## Write Command Config (`command-attribute`)

The write command can carry:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Execute Query | `executeQuery` | STRING | (empty) | SQL query to execute for command |

Writing a value actually goes through the point's `writeQuery`: when a write command fires, the driver takes the point's `writeQuery`, binds the command argument to its `?` placeholder, and executes it (see the previous section). The `executeQuery` attribute is currently not consumed by the driver code—there is no "execute a piece of SQL by command" path that reads it, so configuring it has no effect.

## Collection & Health

- **Collection schedule**: default cron `0/30 * * * * ?` (reads once every 30 seconds).
- **Custom schedule**: the driver also has a custom schedule, default cron `0/5 * * * * ?` (every 5 seconds), for the driver's custom logic.
- **Health / online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`—see [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal Onboarding Example

Collect the `temperature` field of the `id=1` row in a `sensor` table as a temperature point (connecting to an `ORCL` instance by SID):

1. Create a [Device](../introduction/concepts/device) with `Oracle Driver`, and set driver attributes to `host=192.168.1.10`, `port=1521`, `database=iot`, `username=root`, `password=******`, `connectionType=SID`, `sid=ORCL`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attribute `readQuery=SELECT temperature FROM sensor WHERE id = 1`.
3. Start the driver; within 30 seconds the queried temperature value shows up in [PointValue](../introduction/concepts/point-value).

## Pitfalls

::: warning Connection Type decides whether to fill SID or Service Name
When `connectionType=SID`, the driver builds `jdbc:oracle:thin:@host:port:sid` using `sid` (default `sid=ORCL`); when `connectionType=ServiceName`, it instead uses `serviceName` to build `jdbc:oracle:thin:@//host:port/serviceName`, in which case `serviceName` is required and the connection fails without it. The two correspond to Oracle's different naming methods—fill in whichever your instance actually exposes; don't fill both expecting the driver to pick automatically.
:::

::: warning Write Query uses a `?` placeholder, not `${value}`
When writing, `writeQuery` uses a single `?` placeholder for the value to write (e.g. `UPDATE sensor SET temperature = ? WHERE id = 1`), and the driver binds the command argument as a JDBC parameter—this is prepared-statement parameter binding, not string concatenation. Don't manually concatenate the value into the SQL, and don't use template syntax like `${value}`; that won't be substituted and loses the injection protection.
:::

::: tip Query timeout takes effect in `queryTimeout` seconds
`queryTimeout` (default 30 seconds) applies to each SQL execution. If a point's query touches a large table or is a slow SQL, it may be cut off by the timeout before returning within the collection cycle—optimize the SQL or add an index rather than simply raising the timeout.
:::

## Further Reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes & Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `readQuery`
- [Device Onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [MySQL Driver](./mysql) — another database onboarding in the same `dc3-common-sql` family
