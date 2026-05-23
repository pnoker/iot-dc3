---
title: 物模型改造设计
---

# 物模型改造设计

关联设计：[物模型设计方案](../design/thing-model.md)

## 状态

- 当前阶段：初版方案
- 目标范围：基于现有 `Profile`、`Point`、读写指令补齐自定义指令、事件能力
- 核心判断：先把设备与 `Profile` 收敛为单归属，确保 `Profile` 承载设备能力模型、`Point` 承载点位能力；之后把物模型语义里的服务统一落到
  DC3 的 `Command / 指令` 体系

## 待办清单

### 方案确认

- [ ] 确认 `Profile` 作为 DC3 设备能力模型根对象，不新增平行的 `Product / ThingModel` 主表。
- [ ] 确认继续使用 `Profile`、`Point` 作为 DC3 领域名词，不改名为 `ThingModel`、`Property`。
- [ ] 确认一个 `Device` 只能归属一个 `Profile`，一个 `Profile` 可以复用到多个 `Device`。
- [ ] 确认 `Point` 覆盖属性能力，数据库、接口和页面命名保持不变。
- [ ] 确认物模型语义里的服务统一命名为 `Command / 指令`，不新增 `Service` 领域概念。
- [ ] 确认 Point 读写由 `Point.rw_flag` 和现有读写链路表达，不纳入 `dc3_command` 自定义指令定义范围。
- [ ] 确认自定义指令定义表命名为 `dc3_command`、`dc3_command_param`。
- [ ] 确认事件定义表命名为 `dc3_event`、`dc3_event_param`。
- [ ] 确认设备/驱动在线状态由 `LocalCacheService`（Caffeine）和运行态事件派生，不纳入 `dc3_event` 模型事件定义范围。
- [ ] 确认 `dc3_driver_event` 保持平台运行态事件定位，`dc3_device_event` 只有在补充 `event_id` 或 `event_code`
  后才可承载模型事件实例。
- [ ] 确认历史多 `Profile` 绑定设备的迁移规则和人工确认流程。

### 设备与 Profile 单归属

- [x] 在 `dc3_device` 增加 `profile_id` 字段，并补充 DO/BO/VO/Builder 映射。
- [x] 将 `dc3_profile_bind` 有效绑定迁移到 `dc3_device.profile_id`（不做数据迁移，新设备走新逻辑）。
- [x] 对绑定了多个 `Profile` 的设备输出迁移清单，确认唯一目标 `Profile` 后再迁移（已决策：老设备兼容过渡）。
- [x] 移除 `ProfileBind` 的 DO/BO/VO、Mapper、Service、Controller、Facade、gRPC 契约和测试。
- [x] 调整按 `profile_id` 查询设备、按 `device_id` 查询点位、设备状态和 Dashboard 的 SQL / Service 逻辑。
- [x] 调整设备导入导出模板，设备直接携带 `profile_id` 或 `profile_code`。
- [ ] 移除前端和文档中的多 Profile 绑定入口与说明。

### Manager Center

- [ ] 新增自定义指令定义 DO/BO/VO、Builder、Mapper、Service、Controller。
- [ ] 新增事件定义 DO/BO/VO、Builder、Mapper、Service、Controller。
- [ ] 增加按 `profile_id` 查询 Point、指令、事件的聚合接口。
- [ ] 增加 `point_code`、`command_code`、`event_code` 在同一 `profile_id` 下的唯一校验。

### API 与 Facade

- [ ] 在 `dc3-api-manager` 中补充自定义指令、事件定义 gRPC 契约。
- [ ] 在 `dc3-common-facade-api` 中补充自定义指令、事件查询 facade。
- [ ] 在 `dc3-common-facade-grpc` 和 `dc3-common-facade-local` 中补齐实现。
- [ ] 更新 Agentic 工具能力，使其能查询 Profile、Point、指令、事件。

### Data Center

- [ ] 保留现有点位读写 API，作为 Point 内置读写链路，不写入 `dc3_command`。
- [ ] 扩展自定义指令调用 API，支持按 `device_id + command_code` 调用。
- [ ] 新增指令调用记录，保存入参、出参、状态、错误信息、耗时和关联驱动。
- [ ] 扩展事件上报 API，支持按 `device_id + event_code` 关联模型事件定义。
- [ ] 评估复用 `dc3_device_event` 还是新增事件实例表，并补齐模型事件与平台运行态事件的边界和迁移方案。

### Driver SDK

- [ ] 扩展 `DeviceCommandDTO`，增加自定义指令类型和参数结构。
- [ ] 增加指令执行结果回传协议，支持同步与异步调用。
- [ ] 增加事件上报辅助接口，驱动可以按 `event_code` 上报模型事件。
- [ ] 先在虚拟驱动中实现示例自定义指令和示例事件，作为其他协议驱动参考。

### Web 与文档

- [ ] Web 的 Profile 详情页增加 Point、指令、事件三个页签。
- [ ] 操作手册补充 Profile 能力配置说明。
- [ ] 开发文档补充驱动如何适配自定义指令和事件。
- [ ] 增加完整 Profile 能力模型导入导出格式说明。

## 验收标准

- 一个 `Profile` 可以完整定义 Point、指令、事件。
- 一个 `Device` 只归属一个 `Profile`，并可采集该模型 Point、调用该模型指令、上报该模型事件。
- 点位值、指令调用记录、事件记录都能按租户和设备隔离查询。
- 老的点位采集、点位读写命令保持兼容；旧的多 Profile 绑定流程被移除。
- 虚拟驱动提供可运行的端到端示例。
