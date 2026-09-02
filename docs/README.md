# IoT DC3 文档导航(docs/)

> 本文件是 `docs/` 目录的中文导读与分类索引，按类别给出摘要、状态与阅读建议。
> 仓库工程规范见根目录 [AGENTS.md](../AGENTS.md)。目录内全部文档均为简体中文。

## 总览

| 分类 | 文档 | 状态 |
|------|------|------|
| 选型指南 | [db-dialects.md](./db-dialects.md) 关系库选型 | 有效 |
| 选型指南 | [tsdb-stores.md](./tsdb-stores.md) 时序存储选型 | 有效 |
| 选型指南 | [mq-brokers.md](./mq-brokers.md) 消息中间件选型 | 有效 |
| 开发工具 | [development.md](./development.md) Make 开发、启动与质量命令 | 有效 |
| 设计/架构 | [design/relational-r2dbc.md](./design/relational-r2dbc.md) R2DBC 关系访问层 | 已落地，待全量实库/拓扑验收 |
| 质量/测试 | [r2dbc-migration-test-handoff.md](./r2dbc-migration-test-handoff.md) R2DBC 全平台测试交接 | 待执行，发布硬门禁 |
| 设计/架构 | [design/tsdb-abstraction.md](./design/tsdb-abstraction.md) 时序存储 Port | 已落地 |
| 设计/架构 | [design/mq-abstraction.md](./design/mq-abstraction.md) 消息中间件 Port | 已落地 |
| 设计/架构 | [design/storage-abstraction.md](./design/storage-abstraction.md) 存储抽象总纲 | 部分被取代(§3/§4) |
| 设计/架构 | [design/mcp-runtime-overhaul.md](./design/mcp-runtime-overhaul.md) MCP 运行时重构 | 提案,未实施 |
| 设计/架构 | [design/dc3-client-sdk.md](./design/dc3-client-sdk.md) 客户端 SDK | 提案(phase 0 已落地) |
| 设计/架构 | [design/frontend-three-terminal-ux.md](./design/frontend-three-terminal-ux.md) 前端三终端 UX ADR | ADR |
| 分析对比 | [comparison-dgiot.md](./comparison-dgiot.md) DG-IoT 对比借鉴 | 开放评审,未排期 |
| 分析对比 | [comparison-supos-free.md](./comparison-supos-free.md) supOS-Free 对比借鉴 | 开放评审,未排期 |
| 分析对比 | [dc3-thing-model-review.md](./dc3-thing-model-review.md) 物模型代码级核实 | 事实判定(非路线图) |

---

## 一、选型指南(部署 / 运维决策用)

三篇对应三个可插拔维度:关系库、时序库、消息中间件。**部署换什么、门槛是什么、能力差异是什么,先看这里。**

### 1. [关系库选型指南](./db-dialects.md)

- **回答的问题**:PostgreSQL / MySQL / MariaDB 三方言怎么选,差异到底在哪。
- **关键结论**:PostgreSQL 默认;MySQL 硬门槛 ≥8.0(窗口函数、CTE、SKIP LOCKED);MariaDB ≥10.6。
- **最有用的一张表**:"现状矩阵"——upsert 三种方言形态(`ON CONFLICT` / `AS new` 行别名 / `VALUES(col)`)、RETURNING 已弃用、
  咨询锁(`pg_advisory_xact_lock` 事务级 vs `GET_LOCK` 会话级须 try/finally)、`operate_time` 触发器 vs 列属性、三方言契约套件 8/8。

### 2. [时序存储选型指南](./tsdb-stores.md)

- **回答的问题**:TimescaleDB / TDengine / InfluxDB / IoTDB 四库,哪些能力是真支持、哪些是如实拒绝。
- **关键结论**:矩阵按适配器**实际声明**发布而非预估——如 IoTDB 无法按 driver 分组计数(driver 是 measurement 非路径层)、
  InfluxDB 无精确 PERCENTILE(门面精算)、直方图能力两家声明 false(面板零桶降级)。
- **默认**:TimescaleDB(内嵌 PG,部署最简)。

### 3. [消息 broker 选型指南](./mq-brokers.md)

- **回答的问题**:内部异步面选哪个 broker;南向设备面 MQTT 怎么与之共处("两平面一原则")。
- **关键结论**:RabbitMQ 默认;Kafka / Pulsar / MQTT 5 与 RabbitMQ 均已通过同一套真实 broker TCK。
- **注意**:能力矩阵里"延迟消息"多数是本地回退(fallback)而非原生;Kafka 适配器不配 SASL/TLS。

---

## 二、设计与架构文档(design/)

### 存储演进主线(取代链,先看这张图再读文档)

```text
storage-abstraction.md(2026-08-17 总纲)
 ├─ §1-2 三层存储模型、§6 统一配置面 .......... 仍有效(总纲骨架)
 ├─ §3 关系方言机制(MyBatis databaseId 双方言)──已落地 R1/R2,后继──→ relational-r2dbc.md(R2DBC 重写,已批准)
 └─ §4 时序存储 Port .......................... 已被取代──→ tsdb-abstraction.md(已落地)
```

### 4. [基于 Spring Data R2DBC 的关系访问层设计](./design/relational-r2dbc.md)

- **一句话**:用统一的 PostgreSQL R2DBC + Reactor 替换 MyBatis-Plus/JDBC，四个业务域和 single center 一次性切换。
- **读什么**:数据格式、租户/事务不变量、offset/cursor 分页、Flux 取消和发布门禁。
- **注意**:分页和响应信封是一次性硬切换，不保留旧 `R<T>`、页码字段或兼容别名。

#### 4.1 [R2DBC flag-day 全平台测试交接](./r2dbc-migration-test-handoff.md)

- **一句话**:用 Podman 对 Auth、Manager、Data、Agentic、single/distributed、Gateway、Web、CLI 和 PostgreSQL/TimescaleDB 做连续白盒、黑盒与故障交叉验收。
- **读什么**:当前已证实与未证实边界、已知阻断问题、全 Store 实库矩阵、数据库组合、分页/流式协议和发布硬门禁。
- **注意**:当前单元/TCK 基线不能替代本文要求的全链路证据；任一必测项失败、跳过或无证据都阻断发布。

### 5. [时序存储抽象(TSDB Port)](./design/tsdb-abstraction.md)

- **一句话**:关系与时序统一为 PostgreSQL/TimescaleDB + R2DBC，`dc3-tsdb-core` 定义历史 Port，单一实现由 Data 域承载并接受 PostgreSQL TCK。
- **读什么**:port 必须承载的语义(§4)、核心 API(§6);能力差异的落地细节对应 [tsdb-stores.md](./tsdb-stores.md)。

### 6. [消息中间件抽象(MQ Port)](./design/mq-abstraction.md)

- **一句话**: center ↔ driver 异步平面的 broker 抽象,`dc3-mq-core` + 四适配器(RabbitMQ/Kafka/Pulsar/MQTT 5)+ TCK,已交付。
- **读什么**:核心 API(§6)、订阅模式与目的地映射(§7)、逐 broker 的硬语义(§8:延迟消息、死信、MQTT 两平面分离)。

### 7. [统一 PostgreSQL R2DBC 存储设计](./design/storage-abstraction.md)

- **一句话**:统一 PostgreSQL、TimescaleDB、R2DBC、JSONB、UTC 时间、租户隔离和事务边界的现行总纲。
- **读什么**:运行时拓扑、数据格式、分页、并发、取消和发布门禁。

### 8. [MCP 运行时全面重构:内聚的授权契约](./design/mcp-runtime-overhaul.md) *提案*

- **一句话**:现状一次 `tools/call` 要三次网关→auth 冗余往返且有正确性缺口;目标一次内聚往返、响应式非阻塞、真实 input schema、异步审计。
- **读什么**:§2.3 的三冗余一缺口(问题陈述最精彩)、§5 目标契约、§9 备选方案。
- **关联**:实施依赖 relational-r2dbc 的 auth 响应式改造节奏。

### 9. [dc3-sdk:框架无关客户端 SDK](./design/dc3-client-sdk.md) *提案(phase 0 已落地)*

- **一句话**:网关 HTTP 契约现在被 dc3-web(30 个 api 模块)和 dc3-cli(手写 fetch)重复实现,未来 native app 会是第三份;
  目标一个 TypeScript SDK 实现一次,三端消费。
- **读什么**:边界规则(可 lint 强制)、迁移计划;依赖前端 ADR 的边界纪律。

### 10. [前端三终端 UX 架构(ADR)](./design/frontend-three-terminal-ux.md)

- **一句话**:dc3-web 从桌面优先(1280px 硬底线)转向桌面/平板/手机三终端架构,用公理化推导替代零散 `@media` 补丁。
- **读什么**:公理、分层模型、断点契约、边界复用纪律——前端任何新页面开工前都该过一遍。

---

## 三、分析与对比(参考性,未进实施排期)

### 11. [DG-IoT 与 IoT DC3 对比借鉴](./comparison-dgiot.md)

- **一句话**:跨项目对比提炼可借鉴点,并澄清双方物模型差异;开放评审中,**不是承诺路线图**。

### 12. [supOS-Free 与 IoT DC3 对比借鉴](./comparison-supos-free.md)

- **一句话**:同上,对标 supOS-Free;结论与 storage/mq 抽象设计互相印证。

### 13. [DC3 物模型(Profile)代码级核实与最终判定](./dc3-thing-model-review.md)

- **一句话**:用 grep 全仓 + 逐文件读源码的方式回答"DC3 的 Profile 是否等价于物模型",证据落到类/方法/表名,给出最终判定。
- **价值**:回答"DC3 有没有物模型"这一常见问题的权威出处。

---

## 按任务找文档(推荐阅读路径)

| 你想做什么 | 按顺序读 |
|------------|----------|
| 部署 / 换库 / 换 broker | [db-dialects](./db-dialects.md) → [tsdb-stores](./tsdb-stores.md) → [mq-brokers](./mq-brokers.md) |
| 理解存储层现状与演进 | [storage-abstraction §1–2](./design/storage-abstraction.md) → [tsdb-abstraction](./design/tsdb-abstraction.md) → [db-dialects](./db-dialects.md) → [relational-r2dbc](./design/relational-r2dbc.md) |
| 验收全平台 R2DBC 迁移 | [relational-r2dbc 全文](./design/relational-r2dbc.md) → [r2dbc-migration-test-handoff](./r2dbc-migration-test-handoff.md) → 根目录 AGENTS.md |
| 前端 / 客户端开发 | [frontend-three-terminal-ux](./design/frontend-three-terminal-ux.md) → [dc3-client-sdk](./design/dc3-client-sdk.md) |
| MCP / AI 集成 | [mcp-runtime-overhaul](./design/mcp-runtime-overhaul.md) → [relational-r2dbc §5 D13](./design/relational-r2dbc.md)(OAuth/MCP 聚合拆分) |
| 了解项目定位 / 竞品差异 | [comparison-dgiot](./comparison-dgiot.md) → [comparison-supos-free](./comparison-supos-free.md) → [dc3-thing-model-review](./dc3-thing-model-review.md) |
