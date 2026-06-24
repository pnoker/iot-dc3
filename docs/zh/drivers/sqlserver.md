---
title: SQL Server 驱动
---

# SQL Server 驱动

> **`dc3-driver-sqlserver` 把一个 Microsoft SQL Server 数据库当作数据源接入 IoT DC3**——周期性执行 `SELECT` 查询把查到的值当采集值，并支持用 `UPDATE`/`INSERT` 向库里写值的命令。

不是所有数据都来自现场协议设备：很多业务数据、历史数据、第三方系统沉淀的结果，本身就躺在一张 SQL Server 表里。本驱动作为数据库客户端（[驱动](../introduction/concepts/driver) 类型 `DRIVER_CLIENT`），用 JDBC（`com.microsoft.sqlserver.jdbc.SQLServerDriver`）连到一个 SQL Server 实例，按[位号](../introduction/concepts/point)上配置的 SQL 去查值、写值。JDBC 连接的建立与查询执行由共享的 `dc3-common-sql` 抽象基类负责。适用于：把已有业务库里的字段当位号采集、对接只提供数据库视图的上游系统、把外部系统结果定时拉进平台。

下面先解释两个本驱动特有的概念，后面配置表会反复用到：

- **读查询（Read Query）**：位号上配的一条 `SELECT` 语句，驱动按采集周期执行它，取结果作为[位号值](../introduction/concepts/point-value)。
- **写查询（Write Query）**：位号上配的一条 `UPDATE`/`INSERT` 语句，里面用一个 `?` 占位符代表要写入的值——写命令触发时，命令参数会作为该参数绑定进去。

## 驱动名 / code / 类型

- **驱动名 / code**：`SQL Server Driver` / `SqlserverDriver`
- **类型**：`DRIVER_CLIENT`（驱动主动连库并发起查询）

## 驱动配置（设备级 `driver-attribute`）

把一个 SQL Server 库接进来时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。它们决定连到哪个库、用什么账号、查询超时多久、以及连接是否加密：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | SQL Server host |
| Port | `port` | INT | `1433` | SQL Server port |
| Database | `database` | STRING | （空）| SQL Server database name |
| Username | `username` | STRING | `root` | SQL Server username |
| Password | `password` | STRING | （空）| SQL Server password |
| Query Timeout | `queryTimeout` | INT | `30` | SQL query timeout in seconds |
| Encrypt | `encrypt` | STRING | `false` | SQL Server encrypt connection |
| Trust Server Certificate | `trustServerCertificate` | STRING | `true` | SQL Server trust server certificate |

驱动用这些属性拼出 JDBC URL，形如 `jdbc:sqlserver://host:port;databaseName=...;encrypt=...;trustServerCertificate=...;`。其中 `database`、`username`、`password` 为必填——缺任一项，设备配置校验都不会通过。

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填它对应的读/写 SQL：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Read Query | `readQuery` | STRING | （空）| SQL SELECT query for reading point value |
| Write Query | `writeQuery` | STRING | （空）| SQL UPDATE/INSERT using a single ? placeholder for the written value (bound as a parameter) |

::: tip Read Query 取结果的第一个值
`readQuery` 是一条普通 `SELECT`，驱动取其结果中的第一个值作为该位号的值——所以写成 `SELECT temperature FROM sensor WHERE id = 1` 这种只返回单行单列的查询最稳妥。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）决定这个值如何被解析。`readQuery` 是位号的必填项，缺了位号校验不通过。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Execute Query | `executeQuery` | STRING | （空）| SQL query to execute for command |

::: warning 写值实际走 `writeQuery`，`executeQuery` 当前未被消费
驱动的写值路径（`write()`）只读取位号上的 `writeQuery`：把命令参数绑定进它的 `?` 占位符后执行 `UPDATE`/`INSERT`。这里的 `command-attribute` `executeQuery` 虽然作为属性注册在驱动里，但当前没有任何驱动代码读取或执行它——配置了也不会生效。要让写命令落库，请在位号的 `writeQuery` 上配置写 SQL。
:::

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义周期**：驱动还有一个 custom 调度，默认 cron `0/5 * * * * ?`（每 5 秒），供驱动自定义逻辑使用。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把一张 `sensor` 表里 `id=1` 那行的 `temperature` 字段当温度位号采进来：

1. 选 `SQL Server Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`port=1433`、`database=iot`、`username=sa`、`password=******`（先连内网测试时保留默认 `encrypt=false`）。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `readQuery=SELECT temperature FROM sensor WHERE id = 1`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到查到的温度值。

## 易错点

::: warning encrypt 与 trustServerCertificate 要配套填
SQL Server 的 JDBC 驱动在 `encrypt=true` 时会做 TLS 握手并校验服务端证书；若服务端用的是自签名证书，校验会失败、连接报错。开启加密（`encrypt=true`）连自签名证书的实例时，必须把 `trustServerCertificate=true` 一起设上，跳过证书链校验。这两个属性都是 STRING 类型，填字符串 `"true"`/`"false"`，不是布尔。内网明文测试时保持默认 `encrypt=false` 即可。
:::

::: warning Read Query 必须是只读且能定位到单值
驱动取 `readQuery` 结果的第一个值，所以查询要返回单行单列、且能稳定定位到目标行（带 `WHERE` 主键条件）。返回多行/多列时只会取到第一个，结果可能不是你想要的那条；别在 `readQuery` 里写 `UPDATE`/`DELETE`，采集是只读路径。把过滤条件写全，避免随表数据变化取到错行。
:::

::: warning Write Query 用 `?` 占位符，不是 `${value}`
写值时 `writeQuery` 里用一个 `?` 占位符代表写入值（如 `UPDATE sensor SET temperature = ? WHERE id = 1`），驱动会把命令参数作为 JDBC 参数绑定进去——这是预编译参数绑定，不是字符串拼接。不要在 SQL 里手动拼接值，也不要用 `${value}` 这类模板语法，那样既不会被替换、也失去防注入的好处。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `readQuery` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [MySQL 驱动](./mysql) — 另一种关系型数据库数据源，配置结构相同
