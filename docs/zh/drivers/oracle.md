---
title: Oracle 驱动
---

# Oracle 驱动

> **`dc3-driver-oracle` 把一个 Oracle 数据库当作数据源接入 IoT DC3**——周期性执行 `SELECT` 查询把查到的值当采集值，并支持用 `UPDATE`/`INSERT` 向库里写值的命令。

不是所有数据都来自现场协议设备：很多业务数据、历史数据、第三方系统沉淀的结果，本身就躺在一张 Oracle 表里。本驱动作为数据库客户端（[驱动](../introduction/concepts/driver) 类型 `DRIVER_CLIENT`），用 JDBC（`ojdbc11`）连到一个 Oracle 库，按[位号](../introduction/concepts/point)上配置的 SQL 去查值、写值。JDBC 连接的建立与查询执行由共享的 `dc3-common-sql` 抽象基类负责。适用于：把已有业务库里的字段当位号采集、对接只提供数据库视图的上游系统、把外部系统结果定时拉进平台。

下面先解释几个本驱动特有的概念，后面配置表会反复用到：

- **连接方式（Connection Type）**：Oracle 的连接标识有两种——按 **SID** 连，或按 **Service Name** 连。驱动据此拼出不同形态的 JDBC URL。
- **读查询（Read Query）**：位号上配的一条 `SELECT` 语句，驱动按采集周期执行它，取结果作为[位号值](../introduction/concepts/point-value)。
- **写查询（Write Query）**：位号上配的一条 `UPDATE`/`INSERT` 语句，里面用一个 `?` 占位符代表要写入的值——写命令触发时，命令参数会作为该参数绑定进去。

## 驱动名 / code / 类型

- **驱动名 / code**：`Oracle Driver` / `OracleDriver`
- **类型**：`DRIVER_CLIENT`（驱动主动连库并发起查询）

## 驱动配置（设备级 `driver-attribute`）

把一个 Oracle 库接进来时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。它们决定连到哪个库、用什么账号、用哪种连接方式：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | Oracle host |
| Port | `port` | INT | `1521` | Oracle port |
| Database | `database` | STRING | （空）| Oracle database name |
| Username | `username` | STRING | `root` | Oracle username |
| Password | `password` | STRING | （空）| Oracle password |
| Query Timeout | `queryTimeout` | INT | `30` | SQL query timeout in seconds |
| Connection Type | `connectionType` | STRING | `SID` | Oracle connection type [SID, ServiceName] |
| SID | `sid` | STRING | `ORCL` | Oracle SID |
| Service Name | `serviceName` | STRING | （空）| Oracle service name |

设备配置校验要求 `host`、`port`、`database`、`username`、`password`、`connectionType` 必填，缺任一项都不通过。`sid` / `serviceName` 哪个生效取决于 `connectionType`——见下方易错点。

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

写命令上可填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Execute Query | `executeQuery` | STRING | （空）| SQL query to execute for command |

写值实际走的是位号上的 `writeQuery`：写命令触发时，驱动取位号的 `writeQuery`，把命令参数绑定到其 `?` 占位符后执行（见上一节）。`executeQuery` 这个属性当前在驱动代码里没有被消费——没有任何"按命令直接执行一段 SQL"的路径会读取它，配了也不会生效。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义周期**：驱动还有一个 custom 调度，默认 cron `0/5 * * * * ?`（每 5 秒），供驱动自定义逻辑使用。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把一张 `sensor` 表里 `id=1` 那行的 `temperature` 字段当温度位号采进来（用 SID 连一个 `ORCL` 实例）：

1. 选 `Oracle Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`port=1521`、`database=iot`、`username=root`、`password=******`、`connectionType=SID`、`sid=ORCL`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `readQuery=SELECT temperature FROM sensor WHERE id = 1`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到查到的温度值。

## 易错点

::: warning Connection Type 决定填 SID 还是 Service Name
`connectionType=SID` 时驱动用 `sid` 拼出 `jdbc:oracle:thin:@host:port:sid`（默认 `sid=ORCL`）；`connectionType=ServiceName` 时改用 `serviceName` 拼出 `jdbc:oracle:thin:@//host:port/serviceName`，此时 `serviceName` 必填、缺了连接会失败。两者各对应 Oracle 不同的命名方式，按你的实例实际暴露的那种来填，别两个都填指望驱动自动挑。
:::

::: warning Write Query 用 `?` 占位符，不是 `${value}`
写值时 `writeQuery` 里用一个 `?` 占位符代表写入值（如 `UPDATE sensor SET temperature = ? WHERE id = 1`），驱动会把命令参数作为 JDBC 参数绑定进去——这是预编译参数绑定，不是字符串拼接。不要在 SQL 里手动拼接值，也不要用 `${value}` 这类模板语法，那样既不会被替换、也失去防注入的好处。
:::

::: tip 查询超时按 `queryTimeout` 秒生效
`queryTimeout`（默认 30 秒）作用在每次 SQL 执行上。位号对应的查询若涉及大表或慢 SQL，可能在采集周期内还没返回就被超时打断——这时应优化 SQL 或加索引，而不是一味调大超时。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `readQuery` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [MySQL 驱动](./mysql) — 同属 `dc3-common-sql` 家族的另一种数据库接入
