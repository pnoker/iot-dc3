---
title: PostgreSQL 驱动
---

# PostgreSQL 驱动

`dc3-driver-postgresql` 把一个 PostgreSQL 数据库当作数据源接入 IoT DC3：按采集周期执行 `SELECT` 把查到的值当采集值，并支持用位号上配置的
`UPDATE`/`INSERT` 写查询向库里写值。读完你能把已有库表里的字段当位号采进平台，并理解读/写两条 SQL 路径各自的边界。

## 协议背景

不是所有数据都来自现场协议设备。很多业务数据、历史数据、第三方系统沉淀的结果，本身就躺在一张 PostgreSQL 表里——MES
的工单状态、计量系统结算后的累计量、上游平台只对外开放的一个数据库视图。这类数据没有 Modbus、OPC UA
这样的现场总线协议可走，但它们确实是设备世界的延伸，需要被纳入统一的位号体系来管理和消费。

PostgreSQL 是一套开源的对象-关系型数据库（ORDBMS），以严格的 SQL 标准遵循度、强事务（MVCC）、丰富的数据类型（JSON/JSONB、数组、范围、几何）著称，默认监听
`5432` 端口，客户端通过标准 JDBC（`org.postgresql.Driver`）连接。把它接入物联网平台，本质上是把"数据库当成一类设备"
：一张表的一行一列，就是一个位号的取值来源。

在物联网四层参考架构里，数据库桥接驱动横跨网络层与平台层的边界——它不解析任何现场总线报文，而是把"已经落到库里的数据"
以位号的形式重新接入采集管线。理解这条路径如何与时序存储、流处理衔接，见[时序数据与流处理](../foundations/data-pipeline)。

::: info 与现场协议驱动的本质差异
Modbus、OPC UA 这类驱动面对的是"物理量在线"——寄存器实时变化、读到的就是此刻的现场值。数据库驱动面对的是"数据已落库"
——你读到的是某个上游系统写入的、可能已经过期的快照。把数据库字段当位号采集时，采集周期与上游写库节奏要对得上，否则会反复采到同一个旧值。
:::

## 属性配置

接入分三类属性：**驱动属性**（设备级，定位到哪个库、用什么账号）、**位号属性**（每个位号的读/写 SQL）、**写命令属性**
（保留项，当前未生效）。所有属性的默认值都来自驱动的 `application.yml`
，在[设备](../introduction/concepts/device)/[位号](../introduction/concepts/point)
上为它们填具体值即可，三层来历见[属性与配置](../introduction/concepts/attribute-config)。

### 驱动属性（设备级 `driver-attribute`）

把一个 PostgreSQL 库接进来时，在[设备](../introduction/concepts/device)
上填这些[属性](../introduction/concepts/attribute-config)。它们决定连到哪个库、用什么账号、连接超时多久：

| 属性            | code           | 类型     | 默认值         | 说明                                             |
|---------------|----------------|--------|-------------|------------------------------------------------|
| Host          | `host`         | STRING | `localhost` | PostgreSQL 主机地址                                |
| Port          | `port`         | INT    | `5432`      | PostgreSQL 端口                                  |
| Database      | `database`     | STRING | （空）         | 要连接的数据库名                                       |
| Username      | `username`     | STRING | `root`      | 连接账号                                           |
| Password      | `password`     | STRING | （空）         | 账号密码                                           |
| Query Timeout | `queryTimeout` | INT    | `30`        | 连接获取超时（秒），同时作为 Hikari 连接池的 `connectionTimeout` |

驱动用 `host`、`port`、`database` 拼出 JDBC URL（形如 `jdbc:postgresql://host:port/database`）。设备配置校验（`validate()`
）会逐项检查 `host`、`port`、`database`、`username`、`password` 五项是否填写——任一为空都不通过；`queryTimeout` 非必填，缺省按
`30` 秒。每个设备在驱动内对应一个独立的 HikariCP 连接池（`maximumPoolSize=5`、`minimumIdle=1`），按设备 ID 缓存复用。

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填它对应的读/写 SQL：

| 属性          | code         | 类型     | 默认值 | 说明                                           |
|-------------|--------------|--------|-----|----------------------------------------------|
| Read Query  | `readQuery`  | STRING | （空） | 读取位号值的 `SELECT` 语句                           |
| Write Query | `writeQuery` | STRING | （空） | 写值用的 `UPDATE`/`INSERT`，用单个 `?` 占位待写入值（按参数绑定） |

::: tip Read Query 取结果的第一行第一列
`readQuery` 是一条普通 `SELECT`，驱动执行后取结果集**第一行的第一列**（`rs.getObject(1)`）作为该位号的值，再 `toString()`
交给位号按其数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）解析。所以写成
`SELECT temperature FROM sensor WHERE id = 1` 这种只返回单行单列的查询最稳妥。`readQuery` 是位号的必填项，位号校验（
`validatePoint()`）缺它不通过。
:::

### 写命令属性（`command-attribute`）

写命令上保留了一个属性，但当前不被实现消费：

| 属性            | code           | 类型     | 默认值 | 说明              |
|---------------|----------------|--------|-----|-----------------|
| Execute Query | `executeQuery` | STRING | （空） | 命令执行用的 SQL（保留项） |

::: warning `executeQuery` 当前未被实现消费
写值实际走的是位号上的 `writeQuery`：驱动的 `write()` 取 `point-attribute` 的 `writeQuery`，用预编译参数绑定执行 `UPDATE`/
`INSERT`。`command-attribute` 上的 `executeQuery` 仅作为配置项保留，当前驱动代码中没有任何地方读取或执行它——不存在"
按命令直接执行一段 SQL"的独立路径。需要写值时，请把 SQL 配在位号的 `writeQuery` 上，配 `executeQuery` 不会生效。
:::

### 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义周期**：驱动还有一个 custom 调度，默认 cron `0/5 * * * * ?`（每 5 秒）；JDBC 数据库驱动的 `schedule()`
  为空实现，该调度当前不做任何事。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。健康检查通过从连接池取一条连接、`conn.isValid(5)`
  判断库是否可达——连不上则设备置为离线。在线状态机制见[设备](../introduction/concepts/device)。

## 故障排查

数据库桥接接入失败，多半不在协议本身，而在连接参数、SQL 形态或上游库的状态。以下按出现频率排列：

::: warning 设备一直离线 / 连接池建不起来
健康检查靠 `conn.isValid(5)` 判定。若设备始终离线，先确认 `host`/`port` 可达（容器内连宿主机别用 `localhost`）、`database`
名大小写正确、账号 `username`/`password` 有该库的登录权限。PostgreSQL 默认还受 `pg_hba.conf` 约束——来源 IP
或认证方式不被允许时会直接拒连，这类错误不在驱动侧，要去库端放行。
:::

::: warning Read Query 必须只读且能定位到单值
驱动只取 `readQuery` 结果的第一行第一列，所以查询要返回单行单列、且带 `WHERE`
主键条件稳定定位目标行。返回多行/多列时只会取到第一个，可能不是你想要的那条；查询无结果时位号值为 `null`。别在 `readQuery`
里写 `UPDATE`/`DELETE`——采集是只读路径，写进去会污染数据。把过滤条件写全，避免随表数据变化取到错行。
:::

::: warning Write Query 用 `?` 占位符，不是 `${value}`
写值时 `writeQuery` 用单个 `?` 代表写入值（如 `UPDATE sensor SET temperature = ? WHERE id = 1`），驱动用
`ps.setString(1, value)` 把命令参数作为 JDBC 参数绑定——这是预编译参数绑定，不是字符串拼接，天然防 SQL 注入。不要手动拼接值，也不要用
`${value}` 这类模板语法：那样既不会被替换、也失去防注入的好处。写操作以"受影响行数 > 0"判成功，`UPDATE` 命中 0 行（`WHERE`
没匹配到）会被当作写失败。
:::

::: tip 库名 / 表名大小写按 PostgreSQL 规则
PostgreSQL 对不加引号的标识符会折叠成小写，加双引号则按字面大小写匹配。`database` 填错大小写连不上库；`readQuery`/
`writeQuery` 里表名、列名大小写与实际建库时不符也会报"不存在"。按真实大小写填，必要时在 SQL 里用双引号包住标识符。
:::

::: tip 查询超时 / 慢 SQL
`queryTimeout`（默认 30 秒）被用作 Hikari 连接池的 `connectionTimeout`，即"取不到连接时的等待上限"。涉及大表或慢 SQL 时，应优化
SQL、加索引、缩小 `WHERE` 范围，而不是一味调大超时——慢查询会占住连接池（仅 5 条连接），拖累整批位号的采集。
:::

## 在 IoT DC3 中如何落地

- **驱动名 / code**：`PostgreSQL Driver` / `PostgresqlDriver`（`dc3.driver.code` 是稳定路由标识，平台内据它路由消息，不可随意改）。
- **类型**：`DRIVER_CLIENT`——驱动主动连库并发起查询，不监听外部推送。
- **读 / 写 / 订阅能力**：与[驱动能力矩阵](./matrix)一致——读 ✓、写 ✓、订阅 —。读走 `SELECT` 取首值，写走 `writeQuery`
  的预编译绑定；数据库没有变更订阅，靠周期轮询拉取。

::: info 实现状态：可用（非骨架）
PostgreSQL 驱动是**可用实现**，不是骨架。连接、读、写、健康检查均由共享的 `dc3-common-sql` 抽象基类
`AbstractJdbcDriverCustomService` 落地（与 MySQL、Oracle、SQL Server 共用同一套逻辑），PostgreSQL 子类只提供 JDBC URL 拼装、驱动类名
`org.postgresql.Driver` 和默认端口 `5432`。唯一需要留意的是 `command-attribute` 的 `executeQuery` 属性保留但未接线（见上文属性配置）。
:::

### 最小接入示例

把一张 `sensor` 表里 `id=1` 那行的 `temperature` 字段当温度位号采进来：

1. 选 `PostgreSQL Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`port=5432`、
   `database=iot`、`username=root`、`password=******`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `readQuery=SELECT temperature FROM sensor WHERE id = 1`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到查到的温度值。

需要可写位号时，再在同一位号的 `writeQuery` 上配 `UPDATE sensor SET temperature = ? WHERE id = 1`，并把位号 `rwFlag`
设为可写。一次完整流程见[设备接入](../operation/device-onboarding)。

## 延伸阅读

- [驱动总览](./index) — 全部驱动的分类与选型地图
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [时序数据与流处理](../foundations/data-pipeline) — 采到的位号值如何落到时序库、被流处理消费
- [MySQL 驱动](./mysql) — 另一种 JDBC 数据库数据源，配置结构完全一致
