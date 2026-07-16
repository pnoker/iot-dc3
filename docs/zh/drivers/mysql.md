---
title: MySQL 驱动
---

<script setup>
import MysqlDiagram from '../../.vitepress/theme/components/MysqlDiagram.vue'
</script>


# MySQL 驱动

`dc3-driver-mysql` 把一个 MySQL 数据库当作数据源接入 IoT DC3：它作为数据库客户端，按采集周期对库里执行 `SELECT`
把查到的值当采集值，并支持用位号上配的 `UPDATE`/`INSERT`
写查询向库里写值。读完你能在[设备](../introduction/concepts/device)
上配好连库参数、在[位号](../introduction/concepts/point)上配好读/写 SQL，并定位常见的"连不上库/查不到值/写不下去"问题。

> 你在这里：把一个已有数据库当数据源接进来的落地驱动。不是所有数据都来自现场协议设备——很多业务数据、历史数据、第三方系统的结果，本身就躺在一张
> MySQL 表里。

## 协议背景

MySQL 是世界上使用最广的开源关系型数据库，诞生于 1995 年，以 SQL 为查询语言、以表/行/列组织数据。在物联网场景里，它常常不是"
现场设备"，而是数据汇聚的中转站：MES/ERP 等业务系统、第三方平台、历史归档库，都习惯把结果落在一张 MySQL
表里对外提供。把这张表当数据源接进来，就能让平台像采集真实设备一样，周期性地把表里的字段拉成[位号值](../introduction/concepts/point-value)。

本驱动作为数据库客户端（[驱动](../introduction/concepts/driver)类型 `DRIVER_CLIENT`），通过 JDBC（`mysql-connector-j`，驱动类
`com.mysql.cj.jdbc.Driver`）连到一个 MySQL 库，按[位号](../introduction/concepts/point)上配置的 SQL 去查值、写值。它的通信模型是典型的
**请求-响应**——驱动作为客户端主动发起查询，库不会主动推送，所以采集由 cron 周期轮询驱动。JDBC 连接、连接池、SQL
执行的通用逻辑由共享的抽象基类 `AbstractJdbcDriverCustomService`（`dc3-common-sql` 模块）负责，MySQL、PostgreSQL、Oracle、SQL
Server 四个数据库驱动都复用它，各自只提供 JDBC URL 拼装与驱动类名。

放进[物联网数据管线](../foundations/data-pipeline)看，这类数据库驱动处在"把外部已结构化的数据搬进平台"
的入口位置：感知层与现场协议负责把物理量数字化，而 MySQL 驱动负责把已经沉淀在库里的结构化结果接入同一条管线，最终和真实设备采到的位号值一样落库、可查、可被告警与
AI 使用。

下面两个本驱动特有的概念，配置表会反复用到：

<MysqlDiagram lang="zh" />

- **读查询（Read Query）**：位号上配的一条 `SELECT`，驱动按采集周期执行它，取结果**第一行第一列**作为该位号的值。
- **写查询（Write Query）**：位号上配的一条 `UPDATE`/`INSERT`，里面用一个 `?` 占位符代表要写入的值——写命令触发时，命令参数以预编译参数绑定的方式填进去。

## 属性配置

接入一个 MySQL 库，需要在三个层面填[属性](../introduction/concepts/attribute-config)：设备级的连库参数（`driver-attribute`
）、每个采集位号的读/写 SQL（`point-attribute`）、以及写命令上的一个保留属性（`command-attribute`）。下面各属性、类型、默认值均取自驱动的
`application.yml`（`dc3-driver-mysql` 模块）。

### 驱动属性（设备级 `driver-attribute`）

驱动属性回答"连到哪个库、用什么账号、查询超时多久"。在[设备](../introduction/concepts/device)上为每个 MySQL 库填一组：

| 属性            | code           | 类型     | 默认值         | 说明                |
|---------------|----------------|--------|-------------|-------------------|
| Host          | `host`         | STRING | `localhost` | MySQL 主机 IP 或主机名  |
| Port          | `port`         | INT    | `3306`      | MySQL 端口（标准 3306） |
| Database      | `database`     | STRING | （空）         | MySQL 库名          |
| Username      | `username`     | STRING | `root`      | MySQL 用户名         |
| Password      | `password`     | STRING | （空）         | MySQL 密码          |
| Query Timeout | `queryTimeout` | INT    | `30`        | SQL 查询超时（秒）       |

驱动用 `host`、`port`、`database` 拼出 JDBC URL，形如
`jdbc:mysql://host:port/database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`。`host`、`port`、`database`、
`username`、`password` 五项均为必填——配置校验（`validate()`）逐项检查，缺任一项都不通过。驱动按设备 ID 缓存一个 HikariCP
连接池（一台设备一个池，最大 5 连接），连接超时取 `queryTimeout × 1000` 毫秒。

::: tip queryTimeout 同时作用于建连与查询节奏
`queryTimeout`（默认 30 秒）被设为连接池的 `connectionTimeout`：拿不到连接、或建连卡住超过这个时长就会失败。它与采集周期是两回事——慢
SQL 若长期逼近或超过它，应优化 SQL 或加索引，而不是一味调大超时。
:::

### 位号属性（`point-attribute`）

位号属性回答"查这个库里的哪一个值、往哪写"。每个采集[位号](../introduction/concepts/point)填它对应的读/写 SQL：

| 属性          | code         | 类型     | 默认值 | 说明                                              |
|-------------|--------------|--------|-----|-------------------------------------------------|
| Read Query  | `readQuery`  | STRING | （空） | 读取位号值的 `SELECT` 查询                              |
| Write Query | `writeQuery` | STRING | （空） | 写值用的 `UPDATE`/`INSERT`，用单个 `?` 占位符代表写入值（作为参数绑定） |

::: tip Read Query 取结果的第一行第一列
`readQuery` 是一条普通 `SELECT`，驱动取其结果**第一行的第一列**（`rs.getObject(1)`）作为该位号的值——所以写成
`SELECT temperature FROM sensor WHERE id = 1` 这种只返回单行单列的查询最稳妥。结果集为空时取到 `null`
。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）决定这个值如何被解析。`readQuery` 是位号的必填项（
`validatePoint()` 强制），缺了位号校验不通过；`writeQuery` 仅在该位号要被写时才必填。
:::

### 写命令属性（`command-attribute`）

写命令上可以配这个属性，但当前并未被实现消费：

| 属性            | code           | 类型     | 默认值 | 说明            |
|---------------|----------------|--------|-----|---------------|
| Execute Query | `executeQuery` | STRING | （空） | 命令要执行的 SQL 查询 |

::: warning executeQuery 当前未被实现消费
写值走的是**位号上的 `writeQuery`**：`write()` 取 `point-attribute` 的 `writeQuery`，把命令参数 `setString(1, value)`
绑进唯一的 `?` 占位符后执行 `UPDATE`/`INSERT`。`command-attribute` 上的 `executeQuery`
仅作为配置项保留，当前驱动代码中没有任何地方读取或执行它——不存在"按命令直接执行一段 SQL"的独立路径，写值一律走
`writeQuery`。以代码为准。
:::

## 故障排查

MySQL 接入失败大多集中在连接、账号权限、查询定位、字段类型几类。按下面顺序排查：

1. **连不上库（设备一直 offline）**。先确认 `host:port` 可达：`telnet <host> 3306` 或 `nc -vz <host> 3306`。健康检查通过
   `conn.isValid(5)`（5 秒内能否拿到有效连接）判定在线，建连或心跳失败即报 offline。常见根因：库未监听公网/容器网络、防火墙拦截、
   `bind-address` 限制。

2. **能连但被拒（账号/权限/SSL）**。确认 `username`/`password` 正确，且该账号被授权从驱动所在主机连接（MySQL 的账号是
   `user@host` 维度，`root@localhost` 连不上远程）。驱动拼的 URL 带 `useSSL=false&allowPublicKeyRetrieval=true`——若目标库强制
   SSL 或禁用 public key retrieval，会在建连阶段失败。建连失败抛 `ConnectorException`，并使该设备的连接池失效、下个周期重建。

3. **查不到值 / 取到了错行**。驱动只取结果的第一行第一列，所以 `readQuery` 要能稳定定位到目标行。结果集为空会得到 `null`
   ；返回多行时只取到第一行，可能不是你要的那条。把 `WHERE` 主键条件写全，避免随表数据增长取到错行。

4. **数值/类型对不上**。位号的 `pointTypeFlag` 决定取回的字符串如何被解析。把一个文本列配成 `FLOAT`
   位号、或日期/枚举列直接当数值，都可能解析异常。`readQuery` 里用 `CAST`/`CONVERT` 或只选目标数值列，保证取回的值与位号类型相符。

5. **写命令返回失败**。写值要求 `writeQuery` 里恰有一个 `?` 占位符、且语句指向可写的表与行。`write()` 返回"受影响行数 > 0"
   为成功——若 `WHERE` 条件没命中任何行，`executeUpdate()` 返回 0、写被判为失败。先用同样的 `UPDATE` 在库里手工跑一遍确认能命中行。写失败抛
   `WritePointException` 并使连接池失效。

6. **慢 SQL 拖垮采集**。大表全扫、缺索引的 `readQuery` 可能在采集周期内迟迟不返回，或逼近 `queryTimeout`
   。给查询条件列加索引、只选必要列，比单纯调大超时更治本。

::: warning Write Query 用 `?` 占位符，不是 `${value}`
写值时 `writeQuery` 里用**一个** `?` 占位符代表写入值（如 `UPDATE sensor SET temperature = ? WHERE id = 1`），驱动通过
`PreparedStatement.setString(1, value)` 绑定——这是预编译参数绑定，不是字符串拼接，恶意值无法改写语句结构（无 SQL 注入）。不要在
SQL 里手动拼接值，也不要用 `${value}` 这类模板语法，那样既不会被替换、也失去防注入的好处。
:::

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`MysqlDriver`（类型 `DRIVER_CLIENT`，主动连库并发起查询）。这是稳定的路由标识，不要随意改。
- **读能力**：✓ 已实现。`read()` 执行位号的 `readQuery`，取结果第一行第一列作为位号值。
- **写能力**：✓ 已实现。`write()` 执行位号的 `writeQuery`，用 `?` 预编译参数绑定写入值，受影响行数 > 0 即成功。
- **订阅/上报**：— 不支持。MySQL 是请求-响应模型，驱动只主动查/写、不被动接收推送。这与[驱动能力矩阵](./matrix)中 MySQL
  的「✓ / ✓ / —」一致。
- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮），在驱动 `application.yml` 的 `schedule.read` 配置；另有一个
  `custom` 自定义调度默认 cron `0/5 * * * * ?`（每 5 秒），但基类的 `schedule()` 为空实现、数据库驱动不使用它。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`；判定靠 `conn.isValid(5)`
  。在线状态机制见[设备](../introduction/concepts/device)。

::: info 实现状态：可用
本驱动是**完整实现**（非骨架）。读、写、健康检查、按设备缓存的 HikariCP 连接池、失败时连接池失效重建均已落地，复用经测试的
`AbstractJdbcDriverCustomService` 基类。唯一需注意的差异是 `command-attribute` 上的 `executeQuery` 属性保留但未被代码消费——写值一律走位号的
`writeQuery`（见上方警告）。
:::

### 最小接入示例

把一张 `sensor` 表里 `id=1` 那行的 `temperature` 字段当温度位号采进来：

1. 选 `MySQL Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`port=3306`、
   `database=iot`、`username=root`、`password=******`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `readQuery=SELECT temperature FROM sensor WHERE id = 1`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到查到的温度值。
4. 若该位号需可写，给它的 point 属性补 `writeQuery=UPDATE sensor SET temperature = ? WHERE id = 1`
   ，再给它配写[命令](../introduction/concepts/command)。

::: tip 一个驱动实例可接多个库
同一个 MySQL 驱动进程可服务多台设备：每个设备按其 driver 属性各自连一个库、各占一个连接池（按设备 ID
缓存）。设备元数据被删除/更新时，对应连接池会被关闭并按需重建。
:::

## 延伸阅读

- [驱动总览](./index) — 全部驱动入口与分类
- [驱动能力矩阵](./matrix) — 读/写/订阅能力一览，含 MySQL 行
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [时序数据与流处理](../foundations/data-pipeline) — 位号值进入平台后如何存储、计算与查询
- [PostgreSQL 驱动](./postgresql) — 同一 JDBC 基类的另一种数据库数据源
