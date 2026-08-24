# DG-IoT 与 IoT DC3 对比及借鉴分析

|                |                                                                 |
|----------------|-----------------------------------------------------------------|
| **Status**     | Analysis / proposal — for reference, not a committed roadmap      |
| **Date**       | 2026-08-18                                                       |
| **Scope**      | 跨项目对比（DG-IoT ↔ IoT DC3），提炼可借鉴点，并澄清双方物模型差异      |
| **Related**    | [comparison-supos-free.md](./comparison-supos-free.md)、[storage-abstraction.md](./design/storage-abstraction.md)、[mq-abstraction.md](./design/mq-abstraction.md) |
| **Discussion** | 开放评审，未进入实施排期                                           |

> 说明：本文中 **DG-IoT**（`dgiot/dgiot`，gitee: `dgiiot/dgiot`）为国内首款轻量级开源工业物联网平台。
> 结论基于 DG-IoT 公开资料与本仓库（IoT DC3）当前代码/文档，日期见上表。
> 相较早期口径，本文**修正**两处：DC3 物模型的「定义层」严谨、且「属性/服务」已真实落地运行时；但缺「设备影子、物模型实例化、物模型贯通规则/组态/通道」——准确说是「半套活的物模型」（详见 §3 代码级核实）。

---

## 1. 一句话定位

| | **DG-IoT** | **IoT DC3** |
|---|---|---|
| 定位 | 国内首款**轻量级**开源工业物联网平台，一站式快速交付 | 多协议、云原生、AI 赋能的**开发者型**工业物联网平台 |
| 技术栈 | **Erlang/OTP 24.3 + EMQX 4.9 + TDengine + PostgreSQL(Parse) + Mnesia/ETS**；前端 Vue，边缘 iotStudio(Python+Vue) | **Java 21 + Spring Boot 4 + Spring Cloud 2025 + Spring AI 2**；PostgreSQL(TimescaleDB+pgvector+AGE) + RabbitMQ + gRPC；前端 Vue3+TS |
| 架构 | Erlang 单应用 + 内置 EMQX，**单体-ish**（app 模块化） | **六层微服务**（网关 + 4 center + 消息总线 + 驱动） |
| 核心卖点 | **6~30 分钟快速部署、千万级承载、电信级稳定性**（Erlang/EMQX 长连接） | 多协议接入 + 云原生微服务 + **AI Agentic Center** |
| 开发范式 | **物模型-规则引擎-数据通道-组态页面**全流程可视化低代码 | 前后端分离二次开发（Vue3+TS），无内置低代码 |
| 协议接入 | 插件化：Modbus/OPC UA/DA/DLT645/BACnet/Siemens PLC/SL651/**GB28181(视频)** 等 + 定制协议 | **36 个驱动模块** + Driver SDK（无视频协议） |
| 数据通道 | **数据通道**转发 MQTT/Kafka/HTTP/WebSocket + 多源接入(MES/WMS/ERP/ESB/文件/视频) | RabbitMQ 消息总线 + 数据桥接驱动(MySQL/PG/Oracle/SQLServer/Redis/Kafka) |
| 规则引擎 | **强规则引擎**：内置 160+ 函数、过滤/清洗/标准化、实时流处理 | 告警规则引擎 + 事件/命令历史（较简单） |
| 组态/大屏 | **组态页面低代码**（数据通道到组态全流程） | home 写死仪表盘，无组态编排 |
| AI | 边缘 AI/ML 预处理（点到为止） | **强**：Agentic Center，Spring AI，Tool Calling，多模型 |
| 授权 | **Apache 2.0**，免费商用 | **AGPL 3.0**，商用需商业授权 |
| 部署 | `make run` 一键，支持 Win/Linux/Mac | Maven + Podman/Docker Compose |

---

## 2. 分维度对比

### 2.1 目标用户
- **DG-IoT**：集成商/快速交付项目，强调“6 分钟部署、轻量、低代码、免费商用”，适合中小项目快速起量。
- **IoT DC3**：开发团队/平台化产品，需要深度定制、多租户、AI 能力、云原生治理。

### 2.2 技术栈与承载
- DG-IoT 靠 **Erlang/OTP + 内置 EMQX** 拿到“千万级长连接、电信级稳定”的高并发优势，这是 JVM 微服务较难直接对标的点。
- DC3 是 **JVM + Spring Cloud + gRPC**，水平扩展靠无状态微服务 + RabbitMQ 削峰，承载上限取决于集群规模，单机并发不如 Erlang。

### 2.3 协议接入
- DG-IoT：插件化协议，含 **GB28181 视频流**（DC3 没有），支持定制协议。
- DC3：36 个驱动模块覆盖工业/物联网/数据桥接/仿真，配 **Driver SDK** 自研协议，广度上更系统、更可编程。

### 2.4 物模型（详见 §3 对照表）
- DG-IoT 的**物模型是核心**，一次建模批量复用，并贯通规则引擎/数据通道/组态。
- DC3 有**定义严谨、属性/服务已落地的物模型**（Profile + Point/Command/Event），但缺设备影子/物模型实例化，且尚未与规则/通道/组态贯通（详见 §3）。

### 2.5 规则引擎 / 数据处理
- DG-IoT **明显更强**：内置 160+ 函数，覆盖抽取/转换/过滤/排序/分组/聚合/连接，支持数据标准化、清洗、实时流、边缘计算。
- DC3 目前只有告警规则 + 事件/命令历史，**数据清洗/流处理是明显短板**。

### 2.6 组态 / 大屏
- DG-IoT 有**组态页面低代码**（数据通道→组态全流程），是核心卖点之一。
- DC3 的 home 是写死仪表盘（API 较全但无可视化编排器），**低代码组态是最大差距之一**。

### 2.7 数据通道 / 转发
- DG-IoT 的“数据通道”抽象很实用：MQTT/Kafka/HTTP/WebSocket 多通道转发 + MES/WMS/ERP/ESB 多源接入。
- DC3 有数据桥接驱动 + `mq-abstraction.md`（Proposed），方向一致但**缺“通道”这一层编排抽象**。

### 2.8 部署与交付
- DG-IoT：`make run` 一键、6 分钟部署、跨平台，交付体验极轻。
- DC3：需 Maven 打包 + Compose 起基础设施，**交付门槛明显更高**。

### 2.9 AI
- DC3 **领先**：Agentic Center 自然语言运维、告警根因分析、多模型、对话记忆。
- DG-IoT 仅在边缘提到 AI/ML 预处理，无平台级智能体能力。

### 2.10 授权
- DG-IoT 是 **Apache 2.0**，免费商用无传染约束；DC3 是 **AGPL 3.0**，对外 SaaS 需商业授权。这是企业选型最硬的一条差异。

---

## 3. 物模型对照（修正结论）

**结论先行（三层判定）**：DC3 的 Profile 是「**半套活的物模型**」——定义层严谨完整，属性/服务两条线真的落到运行时；但缺设备影子、物模型实例化、以及物模型贯通规则/组态/通道。

### 3.1 三要素映射

核心实体位于 `dc3-common/dc3-common-manager/.../entity/model/`：

| 物模型要素 | DG-IoT 叫法 | DC3 对应实体（DO） | 关键字段 |
|---|---|---|---|
| **模板** | 物模型 Object Model | `ProfileDO`（`dc3_profile`） | `profileName`/`profileCode`、**`profileShareFlag`（共享标志）**、`profileTypeFlag`、`profileExt` |
| **属性** Properties | 属性 | `PointDO`（`dc3_point`）+ `PointAttributeDO` | `pointTypeFlag`、`rwFlag`（读写）、**`baseValue`/`multiple`/`valueDecimal`/`unit`（量程缩放+精度+单位）**、`profileId`、`pointExt` |
| **服务/命令** Services | 服务 | `CommandDO`（`dc3_command`）+ `CommandAttributeDO` + `CommandParamDO` | `commandTypeFlag`、`callTypeFlag`（调用方式）、`timeout`、`profileId` |
| **事件** Events | 事件 | `EventDO`（`dc3_event`）+ `EventAttributeDO` + `EventParamDO` | `eventTypeFlag`、**`eventLevelFlag`（事件分级）**、`profileId` |
| **设备实例** | 设备 | `DeviceDO`（`dc3_device`） | **`driverId` + `profileId`（协议 + 物模型双绑定）** |

### 3.2 层级关系

```
Profile(物模型模板)
 ├── Point      属性（可带量程缩放 baseValue×multiple、单位、精度）
 ├── Command    服务（含 CommandAttribute 参数、CommandParam 下发参数）
 └── Event      事件（含 EventAttribute 属性、EventParam 参数、事件分级）
Device = Driver(协议) + Profile(物模型)   ← 设备是「协议接入」与「物模型」的绑定体
```

### 3.3 运行时落地情况（代码级核实）

| 物模型要素 | 运行时落地 | 证据（类 / 方法） |
|---|---|---|
| 属性（采集 + 量程缩放） | ✅ 真落地 | `TypedValueConverter.calculatePointValue` 应用 `multiple × value + base` 线性变换 + `valueDecimal` 精度舍入，经 `ReadPointValue.calculate()` 进入采集读路径 |
| 属性（读写命令） | ✅ 真落地 | `PointCommandServiceImpl.read()/write()` + `PointCommandValidator` + `PointCommandHistory` |
| 服务（自定义命令） | ✅ 真落地，语义完整 | `CommandReceiver.commandReceive` → `commandFacade.getById` 解析 Command + `deviceMetadata.getCommandConfig` 取命令属性配置 + 入参 `paramValues`/出参 `resultValues`，带去重、设备锁、过期、重试、审计快照 |
| 事件 | ⚠️ 半落地 | `EventHistoryServiceImpl` 用 `FacadeEventBO` 做历史 scope 校验，属性/参数未深度消费 |

> 含义：量程缩放、服务调用不是「只存不用的字段」，而是真实一等公民——这是 DC3 强于多数「只有 schema 的物模型」的地方。

### 3.4 真正缺失的部分（代码级核实）

| 物模型语义 | 状态 | 证据 |
|---|---|---|
| 设备影子（reported/desired 属性集合） | ❌ 缺失 | 全仓库 grep `shadow` = 0；仅 `EntityStateDO`（`dc3_entity_state`）维护心跳/在线状态租约（`stateFlag`/`lastHeartbeatTime`/`expireTime`/`timeoutSeconds`），非属性影子 |
| 物模型实例化 / ontology / 状态机 | ❌ 缺失 | grep `instance`/`thingModel` = 0；`DeviceDO` 只是 `driverId` + `profileId` 外键，非运行时模型实例 |
| 物模型 → 规则引擎 | ❌ 未贯通 | Profile 仅出现在 manager 域 CRUD；告警规则在 data 域独立运行（`RuleStateService`/`RuleExt`），不读 Profile |
| 物模型 → 数据通道 / 组态 | ❌ 未贯通 | 无「按物模型」的通道编排；无组态/大屏绑定 |
| 物模型共享 | ⚠️ 有底座 | `profileShareFlag` 存在，缺市场/导入导出/版本化 |

> 一句话（修正后）：DC3 的 Profile 在「定义严谨度」和「属性/服务两条线的运行时落地」上确实强于多数物模型框架（量程缩放真应用、服务调用带参数带返回还带锁带审计）；但在「运行时物模型」的核心语义上不成立——缺**设备影子、物模型实例化、物模型贯通规则/组态/通道**。所以准确说是「半套活的物模型」，而非「完整物模型」。

---

## 4. 可借鉴点

> 原则：借鉴 DG-IoT 的**能力维度**（通道/规则/组态/轻量交付），不换**技术选型**（Erlang）。

| 优先级 | 借鉴点 | DG-IoT 做法 | DC3 现状 → 落地建议 |
|---|---|---|---|
| P0 | **数据通道抽象** | MQTT/Kafka/HTTP/WS 多通道转发 + MES/WMS/ERP/ESB 多源接入 | 数据桥接驱动 + `mq-abstraction.md`(Proposed) → 在 MQ 端口之上加一层「通道」编排，落地 Kafka 适配器 |
| P0 | **规则引擎/数据处理增强** | 160+ 函数、清洗/标准化/实时流 | 仅告警规则 → 扩展规则引擎支持数据清洗/转换/转发，复用 `dc3-common-data` |
| P1 | **组态页面低代码** | 数据通道→组态全流程低代码 | home 写死 → 可配置 widget 看板 + 组态/大屏 |
| P1 | **物模型贯通** | 物模型一次建模，贯通规则/通道/组态 | Profile 独立 → 打通「模型→规则→通道→看板」链路，落成 `profileShareFlag` 模板市场 |
| P1 | **设备影子 / 物模型实例化** | 每设备一个影子进程（gen_statem），reported/desired 状态机 | 仅 `EntityState` 心跳状态 → 引入设备影子层，维护属性 reported/desired 与版本 |
| P1 | **一键部署/轻量化交付** | `make run`、6 分钟部署、跨平台 | Maven+Compose 较重 → 提供单机一键启动脚本/二进制化交付 |
| P2 | **视频流接入（GB28181）** | 内置 GB28181 视频协议 | 无视频协议 → 视场景加 GB28181 驱动/网关 |
| P2 | **Apache 2.0 授权策略** | 免费商用 | AGPL → 视商业模式考虑社区版+商业授权双轨 |

---

## 5. 不建议照搬

- **Erlang 技术栈**：DG-IoT 的高并发来自 Erlang/OTP + EMQX，但团队与生态门槛高。DC3 的 Java 生态更适合平台化，不为并发换语言；要借鉴的是通道/规则/组态能力。
- **单体-ish 架构**：DG-IoT 是 Erlang 单应用 + 模块，部署轻但跨服务治理弱。DC3 的微服务 + gRPC 更适合多租户、可观测、团队协作，保留即可。

---

## 6. 总结

DG-IoT 与 DC3 定位互补：DG-IoT 偏「轻量快速、开箱即用、低代码、免费商用」，DC3 偏「平台底座、AI 赋能、可控可扩展」。

对 DC3 而言，最该从 DG-IoT 学的三点依次是：**数据通道编排、规则引擎/数据处理增强、组态低代码**；而物模型方面，DC3 的「定义层 + 属性/服务运行时」已经强，要补的是三样运行时语义——**设备影子、物模型实例化、以及物模型贯通规则/通道/组态**。

## 7. 参考来源

- [DG-IoT 官方文档·架构](https://docs.dgiotcloud.cn/docs/product_overview/)
- [DG-IoT GitHub](https://github.com/dgiot/dgiot)
- [DG-IoT README（经典版）](https://github.com/SpaceWatcher/dgiot)
- [IoT DC3 仓库](https://github.com/pnoker/iot-dc3)
