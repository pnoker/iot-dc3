# 设计：可插拔存储抽象（关系库方言 + 时序 Port）

|            |                                                                                                                                 |
|------------|---------------------------------------------------------------------------------------------------------------------------------|
| **状态**   | 已实现 —— R1/R2 已落地（双方言种子 + 方言分支 + 契约套件；选型指南见 [db-dialects.md](../db-dialects.md)）                      |
| **日期**   | 2026-08-17                                                                                                                      |
| **修订**   | 2026-08-19 —— 驱动租约提交（956de3dd3）之后刷新盘点；新增 §6.1                                                                  |
| **范围**   | 持久层：关系库核心 + 位值时序存储                                                                                               |
| **目标**   | 关系库：PostgreSQL（默认）、MySQL 8 —— 时序：TimescaleDB（默认）、TDengine、InfluxDB、IoTDB                                     |
| **相关**   | [`mq-abstraction.md`](./mq-abstraction.md) —— 第三个可插拔维度                                                                  |
| **讨论**   | 实施启动前开放评审                                                                                                              |

## 1. 摘要

IoT DC3 应当允许部署者**独立地**选择关系数据库、时序存储与消息中间件——任意组合都必须是一种受支持的部署形态，例如
一家公司用 `MySQL + TDengine + RocketMQ`，另一家用 `PostgreSQL + TimescaleDB + Kafka`。本文设计其中两个存储维度；
消息中间件维度由姊妹篇 MQ 设计覆盖。

关键的结构性判断：如今 TimescaleDB 作为**扩展运行在主 PostgreSQL 实例内部**（单一 `dc3` 数据库、单一数据源，
`dc3_history` schema 中的 `dc3_point_value`
超表），因此"替换时序存储"目前在部署上没有可供切换的边界。但代码比部署更接近就绪：
一个 `RepositoryService` port（`dc3-common-repository`）已经存在，承载了几乎整个位值操作面——
但看板读取面仍在绕开它。因此要做的工作是：

- **关系库层** —— 不需要新的 port：MyBatis *就是* port。需要的是一套**方言机制**：一个方言中立的基建模块、可移植优先的
  SQL 策略、针对真正绑定方言语句的 `databaseId` 方言分支（当前 9 条，分布在 4 个 mapper——集中于驱动租约子系统），
  以及按方言划分的种子 SQL。当前 51 个 mapper 文件中只有 7 个包含 PostgreSQL 特有语法。
- **时序层** —— 把既有的 `RepositoryService` port 提升为真正可切换的边界：把实现抽取到按库划分的模块、把看板读取面
  （约 10 条旁路语句）收编进 port、中和接口中 SQL 风味的部分（分页、聚合语义），并增加按库的适配器。
- 两个层与 MQ 设计共享同一机制家族：**profile + 能力矩阵 + TCK**，让项目拥有一个一致的可插拔叙事：
  `dc3.facade.mode`（现状）→ `dc3.db.type`、`dc3.repository.type`、`dc3.mq.type`。

## 2. 三层存储模型

```
┌─────────────────────────────────────────────────────────────────┐
│ Relational core            dc3.db.type: postgres | mysql | mariadb        │
│ auth, manager, alarms, command/event history, observability     │
│ → MyBatis dialect profiles (§3)                                 │
├─────────────────────────────────────────────────────────────────┤
│ Time-series store          dc3.repository.type:                 │
│ dc3_point_value domain          timescale | tdengine |          │
│ → RepositoryService port        influxdb | iotdb          (§4)  │
├─────────────────────────────────────────────────────────────────┤
│ Vector (future)            capability-negotiated (§5)           │
│ agentic embeddings — pgvector or external store                 │
└─────────────────────────────────────────────────────────────────┘
```

正交性矩阵——每个单元格都是一种有效部署：

|                     | TimescaleDB (in-PG) | TDengine | InfluxDB | IoTDB |
|---------------------|---------------------|----------|----------|-------|
| **PostgreSQL 核心** | ✅ 当前默认          | ✅       | ✅       | ✅    |
| **MySQL 核心**      | 不适用（仅 PG）      | ✅       | ✅       | ✅    |

注意左下角单元格：时序存储一旦外置，关系数据库就失去了它唯一重度依赖 PostgreSQL 的特性（超表），
`timescale` 也随之坍缩为 `postgres`。两条迁移轨道相互独立，且互为简化。

## 3. 第 1 层 —— 关系库方言

### 3.1 现状耦合（已核实的盘点）

**51 个 mapper XML 文件**，分布于 `dc3-common-auth`（18）、`dc3-common-dal`（4）、
`dc3-common-data`（11）、`dc3-common-manager`（18）。其中 **7 个包含 PostgreSQL 特有语法**——2026-08-18 的
驱动租约提交（`956de3dd3`）新增了一个文件（`DriverLeaseMapper`），并改造了另一个（`PointValueMapper`：其
`DISTINCT ON`
批量最新值语句已被 `ON CONFLICT` upsert 取代）：

| 文件                                                 | 语法                                                                                                                                       | 语义                                                                                       |
|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `dc3-common-data/.../EntityStateMapper.xml`          | 2 条语句：`ON CONFLICT ... RETURNING`（`upsertEntityState`，含 `json_build_object`）、`UPDATE ... FROM ... RETURNING`（`claimExpiredDevices`） | 实体状态 upsert + 过期租约认领，均返回行（驱动心跳热路径）                                   |
| `dc3-common-data/.../PointValueMapper.xml`           | 2 条语句：`ON CONFLICT DO NOTHING ... RETURNING`、`ON CONFLICT DO UPDATE`                                                                   | 幂等批量插入 + 最新值 upsert（持久遥测路径）                                                |
| `dc3-common-data/.../DashboardMapper.xml`            | `time_bucket(INTERVAL ...)`                                                                                                                | 看板时序聚合（TimescaleDB）——属于第 2 层，见 §4.2                                           |
| `dc3-common-data/.../AlertMapper.xml`                | `COUNT(*) FILTER (WHERE)` ×3、`generate_series` + `date_trunc`                                                                             | 告警统计 + 日历日骨架                                                                       |
| `dc3-common-auth/.../ResourceRegistryLockMapper.xml` | `pg_advisory_xact_lock(hashtext())`                                                                                                        | 注册表分布式锁                                                                              |
| `dc3-common-auth/.../OAuthMcpMapper.xml`             | `::text` 类型转换                                                                                                                          | 参数判空惯用法                                                                              |
| `dc3-common-manager/.../DriverLeaseMapper.xml`       | 4 条语句：`pg_advisory_xact_lock`、`ON CONFLICT ... DO UPDATE ... RETURNING` ×3                                                             | 驱动租约获取 / 续期 / 设备认领（热路径，`956de3dd3` 新增）                                   |

**基建模块** `dc3-common-postgres` —— 硬编码了 `org.postgresql.Driver`、
`PaginationInnerInterceptor(DbType.POSTGRE_SQL)`、`TimestamptzLocalDateTimeTypeHandler`，以及 `postgres` profile
的激活。每个业务模块都依赖它。

**DDL 细节**（种子位于 `dc3/dependencies/postgres/initdb/`）：全库使用 `TIMESTAMPTZ` 列；5 个 schema（`dc3_auth`、
`dc3_data`、`dc3_manager`、`dc3_history`、
`dc3_agentic`）通过 `search_path` 解析——mapper 表名因此得以**不带限定**；`JSON` 列
（可移植类型，而非 `jsonb`），默认值为
`'{}'::JSON` —— MySQL 不支持字面量 JSON 默认值，其种子改用表达式默认值（`DEFAULT ('{}')`，要求
8.0.13+）。**plpgsql 触发器函数**是另一部分与 MySQL 不兼容的种子逻辑：每个 schema 一个简单的 `update_operate_time()`
（MySQL 对应：
`ON UPDATE CURRENT_TIMESTAMP`），以及——来自驱动租约提交——三个
使用过渡表（transition table）和 `INSERT ... ON CONFLICT` 的 `track_driver_device_revision_*` 语句级触发器
（MySQL 既没有语句级触发器，也没有过渡表；这套修订计数逻辑必须上移到应用层或重新表达）。已安装的扩展：`timescaledb`
（在用——第 2 层）、`vector`
（已安装，**没有任何列使用它**）、`age`（已安装；任何 mapper 中都不存在 `cypher()` 用法——见开放问题）。

### 3.2 设计

**模块拆分** —— 与 facade/MQ profile 同一家族：

```
dc3-common-postgres  ──►  dc3-common-jdbc    # dialect-neutral: TenantLineHandler,
                                        #      MyBatis-Plus wiring, MybatisUtil
                         dc3-db-postgres     # driver, DbType, timestamptz handler, profile
                         dc3-db-mysql        # driver, DbType, DATETIME(6) mapping
```

由 `dc3.db.type` 通过 `@ConditionalOnProperty` 选择，沿用既有的
`dc3.facade.mode` 模式。

**可移植优先的 SQL 策略。** 对 PG 特有语句的默认处理，是改写为一种在 PostgreSQL 上表现相当的等价可移植形式——
方言分支仅保留给那些可移植化会损失语义或性能的语句：

| PG 特有语法                     | 可移植改写（两库皆可运行）            | 是否需要方言分支？                                                     |
|---------------------------------|--------------------------------------|----------------------------------------------------------------------|
| `COUNT(*) FILTER (WHERE x)`     | `SUM(CASE WHEN x THEN 1 ELSE 0 END)` | 否                                                                   |
| `::text` 类型转换               | 去掉 / 改写参数判空惯用法            | 否                                                                   |
| `generate_series` 日期骨架      | 递归 CTE（MySQL 8 ✔、PG ✔）        | 否                                                                   |
| `ON CONFLICT ... RETURNING`     | —                                    | **方言分支**：`INSERT ... ON DUPLICATE KEY UPDATE` + 同事务 re-select   |
| `UPDATE ... FROM ... RETURNING` | —                                    | **方言分支**：MySQL 没有 `RETURNING`；多表 `UPDATE` + re-select |
| `pg_advisory_xact_lock`         | —                                    | **方言分支**：`GET_LOCK()` / 锁表唯一约束                               |
| `time_bucket(...)`              | —                                    | **移入第 2 层**（TSDB 适配器关注点）                                    |

（`DISTINCT ON` → `ROW_NUMBER()` 曾是计划中的可移植改写，直到 2026-08-18；它所作用的语句已被
`ON CONFLICT` upsert 取代，如今成为一条方言分支——见 §3.1。）

方言分支使用 MyBatis 原生的 `databaseIdProvider`：`<select id="x" databaseId="mysql">`。可移植改写完成后的预期分支面：
**9 条语句、分布在 4 个 mapper**——
`EntityStateMapper` ×2、`PointValueMapper` ×2、`DriverLeaseMapper` ×4、
`ResourceRegistryLockMapper` ×1。驱动租约子系统（租约获取/续期、持久遥测、修订触发器）
如今是最大的单一 PG 惯用语法簇；其语义可移植，只是表达方式不可移植。

**schema → 数据库。** 5 个 PG schema 映射为 5 个 MySQL 数据库；由于 mapper 表名不带限定，
连接所选定的数据库就扮演了如今 `search_path`
的角色。mapper 无需任何改动。

**时间语义。** `TIMESTAMPTZ` → `DATETIME(6)`，外加一条显式的**以 UTC 存储与传输**的约定（在
`TimestamptzLocalDateTimeTypeHandler` 中已是隐式做法）；该约定写入文档并在 TCK 中以断言锁定。

**种子 SQL。** 按方言划分目录，与今天一样由后端仓库维护：
`dc3/dependencies/postgres/initdb/`（既有）与 `dc3/dependencies/mysql/initdb/`。`dc3_point_value` 的 DDL 与全部
TimescaleDB 语句移入 `timescale`
仓储适配器（§4）——时序库外置后，关系种子中完全不包含任何扩展 DDL。列类型之外的种子侧差异：plpgsql 触发器函数
（§3.1）——`update_operate_time()` 映射为
`ON UPDATE CURRENT_TIMESTAMP`，驱动设备修订触发器需要重新实现（应用层或 MySQL
行级触发器）——以及 `'{}'::JSON` 默认值映射为表达式默认值。

**硬性要求：** MySQL 8.0+（窗口函数、CTE、递归 CTE）。MariaDB 10.5+ 顺带即可支持；更老的 MySQL 不在范围内。

### 3.3 关系库 TCK

`make test-it`（DAL 集成测试）针对两个 Testcontainers 镜像（`postgres` 与 `mysql:8`）以完全相同的夹具执行；
结果一致即视为合规。CI 两个都跑——这也是确保新增 mapper 保持可移植、或显式走分支的纪律机制。

## 4. 第 2 层 —— 时序存储（`RepositoryService` port）

> **已于 2026-08-20 被取代**：时序轨道现在拥有自己的完整设计，见
> [`tsdb-abstraction.md`](./tsdb-abstraction.md)（独立存储目标、TCK 门槛、
> 最新值留在关系库的决策）。下面的草稿仅作历史记录保留。

### 4.0（历史草稿）

### 4.1 今天的现状

- **Port**：`dc3-common-repository` 定义了 `RepositoryService`——覆盖位值全域的 8 个操作：
  `savePointValue(s)`、
  `listHistoryPointValue`、`selectLatestPointValue`、`listLatestPointValues`、
  `listPagePointValue`、`aggregateInWindow`、`samplesInWindow`——外加
  `RepositoryStrategyFactory`（命名策略注册表）与 `ActiveRepositoryProfileConfig`。
- **单一实现**：`PostgresRepositoryServiceImpl`（位于
  `dc3-common-data` 内），以 `StrategyConstant.Storage.POSTGRES` 注册；通过 `PointValueManager`/`PointValueMapper`
  把 TimescaleDB 超表当作普通 PG 表处理。
- **数据流**：`PointValueReceiver`（MQ）→ `PointValueIngestBuffer` →
  `PointValueServiceImpl` → `PointValueLocalCacheService`（最新值缓存）+
  `RepositoryService`（持久化）。告警长窗口评估通过
  `RepositoryWindowDataSource` 读取——同样走这个 port。
- **部署形态**：TimescaleDB 是 PG 内的扩展——单实例、单数据源，没有可切换的边界。

### 4.2 存储真正可换之前需要补齐的缺口

1. **看板读取面绕开了 port。** 数据侧
   `DashboardMapper.xml` 的 **10 条语句中有 9 条**直接读 `dc3_point_value`
   （`countInRange`、`countTotal`、`timeseries`、`top`、`latestStream`、
   `latencyHistogram`、`hourlyActivity`、`silentSources`、`coverageGapItems`）；manager 侧的 `DashboardMapper.xml`
   又加了第 10 条（`FROM dc3_history.dc3_point_value`）。只有 `timeseries` 用到 `time_bucket`——其余都是可移植 SQL，
   但无论可移植与否，位值存储一旦外置它们全会失效。而且其中几条比分桶聚合更复杂（延迟直方图分箱、静默源检测、
   覆盖缺口、活跃度网格）：设计中立的读取 port——或基于 port 原语重新表达这些看板——是 T1 中最大的一块单独工作量，
   不是挪一条语句了事（开放问题 §8）。
2. **把实现抽取**为按库划分的模块；由
   `dc3.repository.type` 选择：

   ```
   dc3-repository-timescale    # today's PostgresRepositoryServiceImpl, moved
   dc3-repository-tdengine     # TDengine 3.x super-table adapter
   dc3-repository-influxdb     # InfluxDB 3.x adapter
   dc3-repository-iotdb        # Apache IoTDB adapter
   ```

3. **中和 port 的 SQL 风味表面**：`Page`（一个 MyBatis-Plus 类型）
   与 SQL 聚合语义从 port 泄漏出去。设计目标：一个对游标友好的分页抽象（按时间降序的游标；offset 作为可选能力）
   与一个库中立的聚合枚举——TDengine `INTERVAL`、InfluxDB 3 SQL 与
   IoTDB 都能表达 AVG/MIN/MAX/SUM/COUNT 窗口，因此该枚举可以干净映射。
4. 每个适配器的**数据模型映射**——租户隔离依托各存储的原生维度（租户安全规则得以保留）：

   | 概念 | Timescale | TDengine 3 | InfluxDB 3 | IoTDB |
         |---------|-----------|------------|------------|-------|
   | 序列标识 | (tenant_id, device_id, point_id) 主键 | 超级表 + 标签 `tenant/device/point` | measurement + 标签 | 路径 `tenant.device.point` |
   | 租户作用域 | WHERE tenant_id | 标签过滤（有索引） | 标签过滤 | 路径前缀过滤 |
   | 最新值 | 索引 / `DISTINCT ON` | `LAST()` | `last()` / SQL | `last` 查询 |
   | 窗口聚合 | `time_bucket` | `INTERVAL(...)` | SQL `date_bin` | 按时间分组的窗口 |
   | 保留策略 | drop-chunks 策略 / 压缩 | 每数据库 `KEEP` | retention policy | TTL |
   | 写入路径 | SQL 批量 | schemaless / STABLE 批量插入 | line protocol / SQL | session 批量插入 |

5. **保留与生命周期**作为显式声明的能力（timescale 压缩、TDengine `KEEP`、Influx retention、IoTDB
   TTL），而不是隐藏的 DDL。
6. **部署拓扑**：外置 TSDB 是第二个数据源，拥有自己的 compose 服务与 profile
   （`dc3/dependencies/<store>/`，每个库一个镜像，与每个适配器配一个 broker 的模式一致）。有了外置存储，
   主 PG 完全不再需要 TimescaleDB。

### 4.3 时序 TCK

一套套件，N 个容器：写入固定的位值夹具，然后断言
`listHistoryPointValue` / `listLatestPointValues` / `aggregateInWindow` /
`samplesInWindow` 返回等价结果；租户隔离反向测试（跨租户读取不返回任何数据）；
保留测试（过期数据消失）。通过 TCK 是社区存储的验收门槛（例如日后有人带来 Cassandra 或 ClickHouse 适配器）。

## 5. 第 3 层 —— 向量（占位，按能力协商）

`pgvector` 已安装但未使用——不存在任何 `vector` 列。当 Agentic Center 引入 embedding 时，决策点就会到来：
向量检索是作为 **PostgreSQL 专属能力**（通过能力协商加特性开关，MySQL profile 退化为外置存储或无向量检索），
还是作为第四个可插拔维度。现在不做设计；记录在此，是为了届时这是一个有意识的选择。

## 6. 配置面（跨所有可插拔维度统一）

```yaml
dc3:
  facade:
    mode: grpc          # exists today
  db:
    type: postgres      # postgres | mysql | mariadb                     (§3)
  repository:
    type: timescale     # timescale | tdengine | influxdb | iotdb  (§4)
  mq:
    type: rabbitmq      # see mq-abstraction.md
```

所有维度共用一个机制：`@ConditionalOnProperty` 的 profile 选择、按维度发布能力矩阵、启动日志汇总协商出的组合。
Compose 栈在 profile 背后声明按库划分的服务，`make up` 即可组装出所配置的任意组合。

### 6.1 "可插拔"意味着什么——以及它刻意不意味着什么

**部署期选择就是目标。** 三个维度的任意组合都在组装栈时确定；
`dc3.*.type` 在启动时读取一次。切换既有部署意味着修改配置项并重启受影响的服务——对全新部署而言这就是全部，无需迁移任何数据。

**运行时热替换明确不是目标**，理由有三个结构性原因：

1. **数据引力** —— 数据在引擎手里。在真实数据之下把 PostgreSQL 换成 MySQL 而不搬数据，得到的只是一个空库；
   搬数据是一次*迁移*，不是一次插拔动作。任何抽象层都改变不了这一点。
2. **启动期绑定** —— 连接池、`SqlSessionFactory` 与 `databaseId`
   解析（关系库），TSDB 客户端与摄入缓冲刷写点（时序）
   都在启动时建立。
3. **方言正确性** —— 每条分支语句都是为将执行它的那个引擎解析和路由的；运行时翻转方言会绕开这一保证。

**port 架构真正能带来的是带切换（cutover）的在线迁移**（"温替换"）。对时序维度，
`RepositoryStrategyFactory` 可以同时注册两个适配器：双写窗口（写入扇出）→ 从旧库回填历史 → 用 TCK 夹具做等价校验
→ 切换读取 → 排空 → 退役旧库。MQ 缓冲的摄入路径（`PointValueReceiver` →
`PointValueIngestBuffer`）意味着存储短暂不可用时不会有在途数据丢失。对关系维度，对应物是标准的在线迁移工具
（逻辑复制 / CDC / pgloader）——得到本设计的支持，但在本设计范围之外。

**新增一个存储在唯一重要的意义上是"热"的**：适配器模块是纯增量的——新模块 + 一个配置项，核心零改动；
TCK 就是门槛。

## 7. 迁移计划

两条轨道相互独立，可以交错进行：

- **R1 —— 关系库卫生（零行为变化）。** 可移植 SQL 改写（`FILTER`→`SUM(CASE)` ×3、`generate_series`
  →递归 CTE、去掉 `::text`），把
  `dc3-common-postgres` 拆分为 `dc3-common-jdbc` + `dc3-db-postgres`。*门槛：既有 E2E 与 `make test-it` 保持绿色、结果不变。*
- **R2 —— MySQL 方言。** 通过 `databaseId` 对 4 个 mapper 中 9 条绑定方言的语句做分支；重新实现种子触发器函数
  （`update_operate_time` →
  `ON UPDATE CURRENT_TIMESTAMP`，驱动设备修订触发器 → 应用层）；编写 `initdb/mysql/`（表达式
  JSON 默认值），新增 `dc3-db-mysql`，在 CI 中建立双方言 DAL TCK。
- **T1 —— 收拢 TSDB 边界。** 把 `PostgresRepositoryServiceImpl` 迁至
  `dc3-repository-timescale`；把 10 条语句的看板读取面收编进 port（先做一次 spike 摸清读取原语的形状——开放问题 §8.2）；把
  `dc3_point_value` DDL + `time_bucket` 移出关系种子。*门槛：TSDB 适配器之外没有任何查询再引用
  `dc3_point_value`。*
- **T2 —— port 表面清理 + TDengine 适配器。** 游标分页、中立聚合枚举；TDengine 3 适配器作为首个外置存储的验证，
  外加时序 TCK。
- **T3 —— 社区存储。** InfluxDB / IoTDB 适配器，明确定位为社区体量的任务，以 TCK 为门槛
  （与 MQ 适配器采用相同的定位方式）。

## 8. 开放问题

1. **AGE** —— 启动时已安装并加载，但代码库中不存在任何 `cypher()` 查询，而 AGENTS.md 将其描述为支柱性扩展。
   保留（记录为预留给未来图功能）还是从基础镜像中移除？无论哪种，文档与镜像应当一致。
2. **看板读取 port 的归属与形状** —— 旁路面共 10 条语句（§4.2），其中几条比分桶聚合更复杂
   （延迟直方图、静默源、覆盖缺口）。选项：(a) 用可组合的读取原语（范围计数、分桶聚合、最新流）扩展
   `RepositoryService`，并在此基础上重新表达分析型看板；(b) 另设一个只读的看板 port，承载完整的语句语义。
   倾向：(a) —— 一个把每种看板形状都编码进去的 port 会把当前 UI 泄漏进契约，而原语是可组合的；
   但在 T1 承诺之前，`latencyHistogram` /
   `silentSources` / `coverageGapItems` 的重新表达成本需要先做一次 spike。
3. **分页语义** —— 纯时间游标，还是游标 + 可选 offset（用于小结果集的 UI 分页）？影响前端历史视图契约。
4. **最新值读取路径** —— `PointValueLocalCacheService` 已经缓存最新值；需确认 port 层的
   `selectLatest*` 是否只是冷启动回退（如果是，适配器可以简单实现它，并针对写吞吐做优化）。
5. **InfluxDB 版本** —— 3.x（SQL）是自然的目标；1.8/2.x 的贡献者适配器是否值得接受，是社区问题。
6. **阶段顺序** —— 若 T1/T2 先于 R2 落地，MySQL 分支会缩小（超表已消失）；若 R2 先落地，
   MySQL 将随普通表形态的 `dc3_point_value`
   一起发布，由 T2 完成迁移。根据社区需求信号决定。

## 9. 附录 —— 盘点（迁移清单）

PG 特有的 mapper 语句（R1/R2 清单；2026-08-19 刷新）：

| Mapper                       | 语句                                                                   | 处理动作                                                       |
|------------------------------|------------------------------------------------------------------------|----------------------------------------------------------------|
| `AlertMapper` ×3             | `COUNT(*) FILTER`                                                      | 可移植改写（`SUM(CASE)`）                                       |
| `AlertMapper`                | `generate_series` + `date_trunc`                                       | 递归 CTE（统一）                                                |
| `OAuthMcpMapper`             | `::text` 类型转换                                                      | 去掉 / 改写                                                     |
| `EntityStateMapper`          | `upsertEntityState` —— `ON CONFLICT ... RETURNING`                     | **方言分支**（pg / mysql）                                       |
| `EntityStateMapper`          | `claimExpiredDevices` —— `UPDATE ... FROM ... RETURNING`               | **方言分支**（pg / mysql）                                       |
| `PointValueMapper`           | 幂等批量插入 —— `ON CONFLICT DO NOTHING ... RETURNING`                 | **方言分支**（pg / mysql）；语句在 T1 移入 TSDB 适配器           |
| `PointValueMapper`           | 最新值 upsert —— `ON CONFLICT DO UPDATE`                               | **方言分支**（pg / mysql）；在 T1 移入 TSDB 适配器               |
| `DriverLeaseMapper`          | `pg_advisory_xact_lock` 获取                                           | **方言分支**（pg / mysql）                                       |
| `DriverLeaseMapper` ×3       | 租约续期 / 设备认领 —— `ON CONFLICT ... DO UPDATE ... RETURNING`       | **方言分支**（pg / mysql）                                       |
| `ResourceRegistryLockMapper` | `pg_advisory_xact_lock(hashtext())`                                    | **方言分支**（pg / mysql）                                       |
| `DashboardMapper`（数据侧）  | `time_bucket` + 另有 8 条直读 `dc3_point_value`                        | **T1**：收编进 TSDB port                                         |
| `DashboardMapper`（manager 侧） | 直读 `dc3_history.dc3_point_value`                                  | **T1**：收编进 TSDB port                                         |

种子侧 R2 事项：plpgsql `update_operate_time()` 触发器（→ `ON UPDATE
CURRENT_TIMESTAMP`）、3 个 `track_driver_device_revision_*` 语句级触发器（→ 应用层）、`'{}'::JSON`
默认值（→ 表达式默认值）。

TSDB 边界清单（T1）：

- `RepositoryService` / `RepositoryStrategyFactory` / `ActiveRepositoryProfileConfig` —— 保留，提升为共享
  契约。
- `PostgresRepositoryServiceImpl` + `PointValueManager`/`PointValueMapper`
  的位值语句 → `dc3-repository-timescale`。
- `dc3_point_value` DDL、超表/压缩 DDL、种子数据 → timescale 适配器。
- 验证 TSDB 适配器之外不再残留任何 `dc3_point_value` 引用（`grep -r dc3_point_value` 门槛）。

**R1/R2 实施记录（2026-08-24）**：关系轨道全部落地——R1 可移植改写 + 模块拆分 （中立基建 + 顶层 `dc3-db` 家族——后按家族一致性迁为
`dc3-db/dc3-db-core`，与 dc3-mq-core/dc3-tsdb-core 同构）；R2 MySQL 方言（databaseId fork、RETURNING 解耦为
upsert+re-select、序列退役为行内 +1、咨询锁/触发器/ JSON 簇各有等价实现）+ 双引擎种子（`pg2mysql_seed.py` 派生）+
`dc3-db-tck`
双方言契约套件（PG/MySQL 各 8/8：latest 围栏 upsert 三级决胜、state upsert+reselect、三步 claim、租约行内递增、修订触发器、目录
JSON 双跳）。 实施中的关键发现（已全部写进 [db-dialects.md](../db-dialects.md)）：MySQL 8.4 移除了 ODKU 的 VALUES ()（行别名
`AS new` 是正道，VALUES () 会静默自比较）； MySQL 的 SET 按顺序生效而 PG 读快照——守卫列必须前置；内嵌 JSON 文本进 MySQL 需
NO_BACKSLASH_ESCAPES 或参数绑定。§3.1 清单与最终交付的差异：时序 迁移（T1-T3）先行落地后，PointValueMapper 只剩 latest 投影一条
fork、数据侧 DashboardMapper 十条旁路已整体消失。
