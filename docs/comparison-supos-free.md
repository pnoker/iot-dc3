# supOS-Free 与 IoT DC3 对比及借鉴分析

|                |                                                                 |
|----------------|-----------------------------------------------------------------|
| **Status**     | Analysis / proposal — for reference, not a committed roadmap      |
| **Date**       | 2026-08-18                                                       |
| **Scope**      | 跨项目对比（supOS-Free ↔ IoT DC3），提炼可借鉴点并落地建议          |
| **Related**    | [storage-abstraction.md](./design/storage-abstraction.md)、[mq-abstraction.md](./design/mq-abstraction.md) |
| **Discussion** | 开放评审，未进入实施排期                                           |

> 说明：本文中 “superos-free” 即 **supOS-Free**（蓝卓数字科技/中控体系的“工厂操作系统”免费版）。
> 对比结论基于 supOS-Free 公开资料与本仓库（IoT DC3）当前代码/文档，日期见上表。

---

## 1. 一句话定位

| | **supOS-Free** | **IoT DC3** |
|---|---|---|
| 定位 | 面向中小微企业/软件服务商的**工厂操作系统**（“工业安卓”） | 面向开发者的**开源工业物联网平台**，AI 赋能 |
| 厂商背景 | 蓝卓数字科技（中控体系，30 年工业沉淀、4000+ 智能工厂） | 社区开源项目（pnoker），Gitee GVP |
| 核心诉求 | 零代码/低代码 + 应用生态，让业务人员也能搭应用 | 多协议接入 + 云原生微服务 + AI 智能体，让开发者二次开发 |
| 商业模式 | **永久免费、可商用**，可平滑升级到商业版 | AGPL 3.0，个人/学习/内部免费，商用需商业授权 |
| 协议接入 | 内置 **50+ 工业协议驱动**（PLC/CNC/仪器仪表，Modbus/OPC UA 等） | **36 个驱动模块**（Modbus/OPC UA/S7/BACnet/IEC104/MQTT/CoAP/LwM2M/数据桥接等）+ 驱动 SDK |
| 数据建模 | **UNS 统一命名空间、面向对象建模**（对标传统面向 Tag） | DO/BO/VO 三层建模 + 点位/设备/驱动/Profile 模板模型 |
| 存储 | 多模态：关系 + 时序 + 非结构化 + 文件 | PostgreSQL（TimescaleDB + pgvector + AGE）+ RabbitMQ |
| 应用开发 | **低代码/零代码**：1500+ 组件、智表、报表、ETL、大屏、应用商店 | 前后端分离二次开发（Vue3 + TS），无内置低代码 |
| AI | 弱（社区版集成 CopilotKit/MCP，非核心卖点） | **强**：Agentic Center，Spring AI，Tool Calling，多模型，对话记忆 |
| 架构 | 多开源组件拼装（Kong/Keycloak/EMQX/Node-RED/FUXA/Grafana/TDengine 等） | 自研 Spring Boot 4 + Spring Cloud 2025 微服务 + gRPC |

---

## 2. 分维度对比

### 2.1 目标用户
- **supOS-Free**：业务人员/集成商/中小企业，强调“不写代码也能用”，App 商店 + 模板复用。
- **IoT DC3**：开发团队/技术集成商，需要二次开发、定制协议、嵌入自有系统。

### 2.2 接入能力
- 两者都覆盖主流工业协议。supOS-Free 数量上宣称更多（50+）、集成 Node-RED 边缘计算 + 打通 ERP/WMS/MES；DC3 靠 **Driver SDK** 快速自研驱动，并带数据桥接（MySQL/PG/Oracle/SQLServer/Redis/Kafka）与仿真/调试驱动（Virtual/Listening Virtual），更偏“可编程扩展”。

### 2.3 建模与数据
- supOS-Free 的 **UNS 面向对象建模** 是其差异化亮点，适合大规模设备标准化、跨系统复用。
- DC3 用经典三层模型 + 点位/Profile 模板体系，配合多租户隔离，更贴近传统 IT 系统集成。

### 2.4 应用层
- supOS-Free 赢在**低代码/零代码**：可视化 ETL、智表、报表引擎、大屏组态、应用商店，开箱即用。
- DC3 无低代码平台，靠前端工程开发；但提供 REST + gRPC 完整 API，适合做定制化产品底座。

### 2.5 AI 能力
- **DC3 明显领先**：Agentic Center 支持自然语言查设备/读写点位/告警根因分析/多模型/对话记忆。
- supOS-Free 目前 AI 不是主打（社区版有 CopilotKit/MCP 组件，但定位弱）。

### 2.6 架构与技术栈
- supOS-Free：**拼装型**，用 Kong、Keycloak、EMQX、Node-RED、FUXA、Grafana、TDengine/TimescaleDB、Hasura、MinIO 等开源件组合，部署重量级、组件多。
- IoT DC3：**自研型**，Java 21 + Spring Boot 4 + Spring Cloud 2025 + gRPC，六层微服务，治理、可观测、水平扩展更内聚，代码可控性高。

### 2.7 开源与授权
- supOS-Free 宣称永久免费可商用（另有 Apache 2.0 的 Open supOS 社区版）。
- DC3 是 **AGPL 3.0**，商用有传染性约束，对外提供 SaaS/商业服务需商业授权——这是企业选型的关键差异。

---

## 3. 可借鉴点

> 原则：借鉴的是 supOS-Free 的**能力维度**（存储/对象存储/大屏/ETL/模板生态），
> 不是它的**技术选型**（组件拼装架构）。DC3 的自研微服务架构不必对齐。

### P0 —— 已有铺垫，直接落地（投入产出比最高）

#### 3.1 多模态 / 可插拔存储
- **supOS 做法**：关系 + 时序（TDengine/TimescaleDB）+ 非结构化 + 文件多模态底座。
- **DC3 现状**：[storage-abstraction.md](./design/storage-abstraction.md) 已设计 `dc3.db.type` 关系方言 + `dc3.repository.type` 时序端口（目标 PostgreSQL/MySQL + TimescaleDB/TDengine/InfluxDB/IoTDB），状态为 Proposed — not yet implemented。
- **落地建议**：优先实现 **TDengine 时序适配器**（supOS 主打，国内工业客户常点名），并把 `RepositoryService` 端口真正抽成可插拔。几乎无需重新设计。

#### 3.2 对象存储（文件/非结构化）
- **supOS 做法**：MinIO 做 S3 兼容对象存储。
- **DC3 现状**：仓库内无 MinIO/S3，缺文件/图片/模型文件存储能力。
- **落地建议**：引入 MinIO 作为可选基础设施，服务驱动上传包、截图/大屏图片、报表导出、Agentic 的模型/文档文件。低耦合、高价值，配合 `dc3/` 下 compose 增加 optional service。

#### 3.3 可插拔消息总线
- **supOS 做法**：EMQX（MQTT）+ Kafka（数据总线）+ Node-RED（边缘计算）。
- **DC3 现状**：[mq-abstraction.md](./design/mq-abstraction.md) 已设计 `dc3-common-mq` 端口 + 每 broker 一个适配器（RabbitMQ 默认，目标 Kafka/RocketMQ/Pulsar），同样 Proposed。
- **落地建议**：先落地 **Kafka 适配器**——工业客户常要求 Kafka 对接大数据平台，是 DC3 在“数据总线”上对标 supOS 的关键一环。

### P1 —— 值得借鉴，投入中等偏大

#### 3.4 UNS 统一命名空间 + 面向对象建模
- **supOS 做法**：UNS 理念，把设备属性/事件/命令封装为标准对象模型，“一次建模、批量复用”。
- **DC3 现状**：已有 **Profile 模板 + Point 属性** 机制（`dc3-center-manager` 的 Profile Management），是“设备类型模板复用”雏形，但命名空间是「驱动/设备/点位」三层，无 ISA-95/UNS 式跨系统标准命名与共享模型。
- **落地建议**：在 Profile 之上增加“对象模型/资产模型”层，支持模型导入导出与共享（JSON Schema），点位命名挂到 UNS 规范下。

#### 3.5 模板生态 / 应用商店（轻量版）
- **supOS 做法**：应用商店 + 工业 APP 全生命周期管理，模板/组件可复用下载。
- **DC3 现状**：Profile 模板存在但无市场/共享/版本化；驱动是代码级模块（`dc3-driver/*`），非可分发资产。
- **落地建议**：先做轻量 **Profile 模板市场**（模板导入/导出/版本/公开分享），再做“驱动商店”。

#### 3.6 低代码大屏 / SCADA
- **supOS 做法**：FUXA SCADA + 1500+ 组件低代码大屏、车间看板、组态拖拽。
- **DC3 现状**：`home` 是写死仪表盘（背后 API 较全：告警统计、健康、拓扑 Sankey、延迟直方图、活动热力图等），但无可视化组态/大屏编排器。
- **落地建议**（两档）：
  - **轻量档**：把 home 升级为“可配置 widget 看板”（拖拽/增删卡片，数据源复用现有 dashboard API）。
  - **重档**：像 supOS 集成 FUXA 那样，把 FUXA（或类似开源 SCADA）作为可选 optional service 接入，与点位数据打通。不建议自研完整组态引擎。

#### 3.7 可视化 ETL / 数据集成编排
- **supOS 做法**：画布拖拽的可视化 ETL（数据同步/加工/清洗，SQL、转置、聚合、自定义算子）。
- **DC3 现状**：有数据桥接驱动（mysql/oracle/postgresql/sqlserver/redis/kafka），但只是“接入”，无编排/流转/清洗。
- **落地建议**：短期在数据桥接驱动上加“数据流转规则/定时任务”；中期把 **Node-RED** 作为 optional 组件引入（supOS 同款思路），专做边缘/集成编排。

### 不建议照搬

- **架构拼装方式**：supOS 是 Kong/Konga/Keycloak/Node-RED/EMQX/FUXA/Grafana/Hasura 组合，组件多、部署重、可控性弱。DC3 自研微服务更内聚，不建议为了“对齐”而换架构。要借鉴能力维度，不换技术选型。
- **Keycloak/Kong**：DC3 已有 JWT + Spring Security RBAC 和 Spring Cloud Gateway，重叠，不必引入。
- **应用商店全套**：完整工业 APP 生命周期 + 商店是重资产，做轻量模板市场即可。

---

## 4. 优先级路线图

| 优先级 | 借鉴点 | 工作量 | 备注 |
|---|---|---|---|
| P0 | 落地 storage/mq 抽象（TDengine + Kafka 适配器） | 中 | 设计已就绪，直接施工 |
| P0 | MinIO 对象存储 | 低 | 补齐文件/非结构化短板 |
| P1 | Profile 对象模型 + 模板市场（导入/导出/共享） | 中 | 放大已有模板价值 |
| P1 | home 可配置 widget 看板 | 中 | 复用现有 dashboard API |
| P2 | Node-RED / FUXA 作为 optional 服务 | 中 | 借力开源，不自研 |
| P3 | 报表引擎、可视化 ETL、完整应用商店 | 高 | 投入大，视战略决定 |

---

## 5. 总结

DC3 真正该从 supOS-Free 学的不是它的“组件拼装”，而是“把能力做全、做可插拔、做可复用”。其中存储/MQ/对象存储三点 DC3 已经写好蓝图，是最该先落地、投入产出比最高的部分；大屏组态、ETL、模板生态属于进阶，优先“借开源”而非自研。

## 6. 参考来源

- [supOS-Free 产品页](https://www.supos.com/product/free)
- [supOS-Free 发布介绍（掘金）](https://juejin.cn/post/7604694326518988851)
- [Open supOS GitHub](https://github.com/supOS-Project/supOS)
- [IoT DC3 仓库](https://github.com/pnoker/iot-dc3)
