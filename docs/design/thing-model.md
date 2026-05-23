---
title: 物模型设计方案
---

# 物模型设计方案

本文是物模型改造初版设计，用于先统一概念和边界，再进入接口、表结构和代码实现。这里的"物模型"用于说明行业语义，不要求把 DC3
的领域名词改成通用物模型名词。

> **实施状态**：Phase 1 开工在即；Phase 2-4 为后续迭代。各章节 `[DONE]` / `[TODO]` 标记实际进度。

---

## 0. 实施进度总览

| 阶段 | 内容 | 状态 | 说明 |
|------|------|------|------|
| Phase 1 | Device 加 `profile_id`，移除 ProfileBind | [DONE] | 28 文件改动，核心重构 |
| Phase 1 | 数据迁移（多 Profile → 单 Profile） | 不做 | 已决策：新设备走新逻辑，老设备兼容过渡 |
| Phase 2 | `dc3_command` / `dc3_command_param` 表 | [TODO] | 纯增量，无风险 |
| Phase 2 | `dc3_event` / `dc3_event_param` 表 | [TODO] | 纯增量，无风险 |
| Phase 3 | 指令调用链路 + 事件上报链路 | [TODO] | 依赖 Phase 2 完成 |
| Phase 4 | Web UI + 导入导出 + 文档 | [TODO] | 产品化收尾 |

---

## 设计结论

基于现有 `Profile` 扩展指令和事件是合理的。当前 `Profile` 已经承担"设备模板 / 产品模型"的角色：`Point` 通过 `profile_id`
归属于 `Profile`，运行态数据再按 `device_id + point_id` 落入数据中心。这与物模型中的"产品定义属性，设备实例上报属性值"基本一致。

改造第一步应先把 `Device` 与 `Profile` 的关系从"设备可绑定多个 Profile"收敛为"设备只能归属一个 Profile"。也就是说，一个
`Profile` 可以被多个设备复用，但一个 `Device` 只能选择一个 `Profile` 作为自己的能力模型。完成这一步后，`Profile` 承载物模型能力、
`Point` 承载属性能力的定位就能在数据结构上成立，剩下的缺口只是继续补齐指令和事件。

需要注意的是，指令和事件不应塞进 `profile_ext` 成为大 JSON。`profile_ext`
更适合放兼容性较强的扩展信息；指令、事件具有独立查询、鉴权、版本、参数校验、驱动适配和运行记录需求，应作为 `Profile`
下的结构化子资源建模。

## 设计哲学

DC3 保留自己的领域语言：`Profile` 就是 DC3 对一类设备能力模型的表达，`Point` 就是 DC3
对设备数据点和控制点的表达。它们的能力可以完整覆盖通用物模型中的产品、属性等概念，但不需要把表名、接口名、页面名统一改成
`ThingModel`、`Property` 这类通用名词。

这样做有三个好处：

- 保持当前系统认知连续性，用户、驱动开发者和历史代码仍围绕 `Profile`、`Point` 理解系统。
- 保留 DC3 的工业接入风格，`Point` 既能表达采集属性，也能表达可写控制点，比纯 `Property` 更贴近现有驱动模型。
- 避免为了贴合行业术语重命名大面积模块，把改造重点放在关系收敛、指令定义、事件定义和运行链路闭环上。

## 能力映射

| 物模型概念    | DC3 概念              | 说明                                                               |
|----------|---------------------|------------------------------------------------------------------|
| 产品 / 物模型 | `Profile`           | DC3 的设备能力模型，定义一类设备可复用的能力集合                                       |
| 设备实例     | `Device`            | 实际接入的设备，归属于租户和驱动                                                 |
| 属性       | `Point`             | DC3 的点位表达，已包含读写、类型、单位、精度、倍率等信息                                   |
| 属性值      | `PointValue`        | 运行态采集数据，归属 `device_id + point_id`                                |
| 设备归属能力模型 | `Device.profile_id` | 设备只能归属一个 `Profile`，不再维护多对多绑定                                     |
| 服务       | `Command`           | DC3 的动作型指令能力；Point 读写由 `rw_flag` 和现有读写链路表达，不进入 `dc3_command`     |
| 事件       | `Event` + 运行态事件记录   | 新增 `Profile` 级事件定义；已有设备/驱动告警由 `dc3_entity_alarm` 承载，是运行态记录而非模型定义 |

## 目标

- 保留 `Profile` 作为 DC3 的设备能力模型根，避免重新引入一套平行的 `Product / ThingModel` 主表。
- 将设备与物模型关系调整为 `Device.profile_id -> Profile.id`，移除 `dc3_profile_bind` 带来的多模型绑定语义。
- 保留 `Point` 作为 DC3 的点位概念，不引入 `Property` 替换它。
- 在 `Profile` 下新增自定义指令定义和事件定义，补齐 `Point`、`Command`、事件三元能力模型。
- 运行态数据仍归属设备实例：点位值写入 `PointValue`，指令调用和事件上报进入数据中心。
- 保持多租户隔离，所有新增定义和运行记录都必须带 `tenant_id`。

## 非目标

- 初版不重命名数据库、接口和页面中的 `Profile`、`Point` 概念。
- 初版不一次性覆盖复杂 TSL 标准，只实现 DC3 当前驱动和数据流能闭环的最小物模型。
- 不做历史数据迁移。新建设备直接走 `profile_id`；已有设备通过兼容路径（bind 和 `profile_id` 并存过渡期）逐步切换。

## 领域模型

```text
Profile
  +-- Point
  +-- Command
  +-- Event

Device
  +-- profile_id -> Profile.id
  +-- connect Driver

Runtime
  +-- PointValue(device_id, point_id, value)
  +-- CommandInvoke(device_id, command_code, input, output, status)
  +-- DeviceEvent(device_id, event_id, payload, level, status)
```

### Profile

`Profile` 继续作为 DC3 的设备能力模型根对象。业务层、接口和前端仍使用 `Profile` 作为主概念；文档可以说明它覆盖物模型能力，但不把它改名为物模型。

当前字段已具备：`profile_code`、`profile_name`、`profile_type_flag`、`profile_share_flag`、`version`、`profile_ext`、`enable_flag`、`tenant_id`。

建议约束：

- `profile_code` 在同租户下保持唯一，并作为物模型编码。
- `version` 表示模型版本，Point、指令、事件变更时同步更新。
- `profile_ext` 可补充 `category`、`modelVersion`、`source`、`tags` 等弱结构化信息。
- `profile_share_flag` 继续控制租户、驱动、用户范围内的复用边界。

### Point

`Point` 作为 DC3 的点位概念，不建议新建 `Property` 表替代它。当前已有 `profile_id` 归属。字段已经覆盖属性的核心定义：

| 字段                        | 能力含义                |
|---------------------------|---------------------|
| `point_name`              | 点位名称                |
| `point_code`              | 点位标识符               |
| `point_type_flag`         | 数据类型                |
| `rw_flag`                 | 读写能力                |
| `unit`                    | 单位                  |
| `value_decimal`           | 精度                  |
| `base_value` / `multiple` | 采集值换算规则             |
| `point_ext`               | 协议映射、约束、枚举值、采集策略等扩展 |

建议补充的约束：

- 同一 `profile_id` 下 `point_code` 唯一。
- 设备的 Point 集合只来自自身 `profile_id` 指向的 `Profile`。
- `point_ext` 中可放点位约束，如 `min`、`max`、`enum`、`step`、`reportMode`。

### Command [TODO]

物模型语义里的服务在 DC3 中统一表达为 `Command`。这里的 `Command` 只描述读写 Point 之外的动作型能力，例如重启、校准、切换模式、下发配置。指令定义属于
`Profile`，指令执行属于 `Device`。

现有位号读写不属于自定义 `Command` 范畴，而是 `Point` 自身的访问能力和现有读写链路：

| 读写能力    | 来源                    | 说明                               |
|---------|-----------------------|----------------------------------|
| `READ`  | `Point.rw_flag` 包含读能力 | 读取指定 `device_id + point_id` 的点位值 |
| `WRITE` | `Point.rw_flag` 包含写能力 | 向指定 `device_id + point_id` 写入目标值 |

Point 读写不在 `dc3_command` 中重复建模，由 `Point.rw_flag` 和现有 `PointCommand` 链路表达。新增 `Command` 只用于描述读写
Point 之外的自定义指令能力，并通过 `profile_id` 归属到 `Profile`。

建议新增定义表：

| 表                   | 作用           |
|---------------------|--------------|
| `dc3_command`       | 自定义指令主定义     |
| `dc3_command_param` | 自定义指令入参、出参定义 |

`dc3_command` 建议字段：

| 字段             | 说明                                |
|----------------|-----------------------------------|
| `id`           | 主键                                |
| `profile_id`   | 归属 Profile                        |
| `command_name` | 指令名称                              |
| `command_code` | 指令标识符，同一 `profile_id` 下唯一         |
| `command_type` | 指令类型，如 `custom`、`config`、`action` |
| `call_type`    | 调用方式，如 `sync`、`async`             |
| `timeout`      | 调用超时时间                            |
| `command_ext`  | 协议映射、驱动指令模板、幂等配置等扩展               |
| `enable_flag`  | 启停状态                              |
| `tenant_id`    | 租户                                |

`dc3_command_param` 建议字段：

| 字段                          | 说明                       |
|-----------------------------|--------------------------|
| `command_id`                | 归属指令                     |
| `param_name` / `param_code` | 参数名称与标识                  |
| `param_direction`           | `input` 或 `output`       |
| `param_type_flag`           | 数据类型，复用或扩展点位类型枚举         |
| `required_flag`             | 是否必填                     |
| `default_value`             | 默认值                      |
| `param_ext`                 | 枚举、范围、单位、JSON Schema 片段等 |

运行链路建议复用现有命令通道进行演进：

1. 应用侧调用 `Data Center` 的指令 API；读写指令传入 `device_id + point_id`，自定义指令传入 `device_id + command_code`
   和参数。
2. `Data Center` 读取设备的 `profile_id`；读写指令校验目标 `Point` 是否属于该 `Profile`，自定义指令校验目标 `Command`
   是否属于该 `Profile`。
3. 通过 `DriverFacade` 找到设备驱动，向现有命令交换机投递 `DeviceCommandDTO`。
4. 驱动执行后返回结果；同步指令等待结果，异步指令只记录受理状态。
5. 指令调用记录写入数据中心，便于追踪、重试和审计。

### Event [TODO]

事件表示设备主动上报或平台根据数据规则识别出的业务事件，例如故障、告警、模式切换、工况变化。事件定义属于 `Profile`，事件实例属于
`Device`。

建议新增定义表：

| 表                 | 作用       |
|-------------------|----------|
| `dc3_event`       | 事件主定义    |
| `dc3_event_param` | 事件输出参数定义 |

`dc3_event` 建议字段：

| 字段            | 说明                                   |
|---------------|--------------------------------------|
| `id`          | 主键                                   |
| `profile_id`  | 归属 Profile                           |
| `event_name`  | 事件名称                                 |
| `event_code`  | 事件标识符，同一 `profile_id` 下唯一            |
| `event_type`  | `info`、`alert`、`fault`、`lifecycle` 等 |
| `level`       | 事件级别                                 |
| `event_ext`   | 确认策略、过期策略、协议映射等扩展                    |
| `enable_flag` | 启停状态                                 |
| `tenant_id`   | 租户                                   |

### Event 与现有运行态事件的边界

当前系统已通过 `dc3_entity_alarm` 统一承载设备/驱动的运行态告警记录（`EntityAlarmDO`，含 `alarm_type_flag`、`alarm_source_flag`、`alarm_level_flag`）。
原 `dc3_device_event` / `dc3_driver_event` 已合并到 `dc3_entity_alarm` 中。

| 现有事件                         | 当前定位               | 是否属于 `dc3_event` 定义范围                                        |
|------------------------------|--------------------|--------------------------------------------------------------|
| `dc3_entity_alarm` 状态超时告警    | 驱动/设备在线、离线、故障等平台运行态记录 | 否，属于平台状态链路，不归属 `Profile`                                    |
| `dc3_entity_alarm` 规则告警       | 规则引擎触发的告警记录         | 否，属于规则引擎链路                                                  |
| `dc3_entity_alarm` 设备上报告警     | 设备侧业务告警运行记录         | 可作为模型事件实例基础，但需要补充 `event_id` 或 `event_code` 与 `dc3_event` 关联 |

设备状态和驱动状态由 `dc3_entity_state` 维护持久化状态租约；心跳续租，状态翻转或租约过期时再派生告警事件记录到 `dc3_entity_alarm`。
因此，在线/离线状态不建议建成 `Profile` 下的通用 `dc3_event` 定义，避免把平台生命周期和设备业务能力混在一起。

## API 与模块落点

| 模块                      | 改造点                                                                                                            |
|-------------------------|----------------------------------------------------------------------------------------------------------------|
| `dc3-common-manager`    | 先将设备与 `Profile` 关系收敛为单归属，移除 `ProfileBind` 相关模型、服务和接口；再新增自定义指令、事件定义的 BO/VO/DO、Builder、Service、Controller、Mapper |
| `dc3-api-manager`       | 补充自定义指令、事件定义的 gRPC 查询契约                                                                                        |
| `dc3-common-facade-api` | 新增 `CommandFacade`、`EventFacade` 或聚合查询接口                                                                       |
| `dc3-common-data`       | 扩展指令调用 API、事件上报/查询 API，补齐指令调用记录                                                                                |
| `dc3-common-driver`     | 扩展驱动命令 DTO，支持自定义指令参数和执行结果回传                                                                                    |
| `dc3-driver-*`          | 按协议实现自定义指令和事件上报映射                                                                                              |
| `dc3-web`               | Profile 详情页增加 Point、指令、事件三个页签                                                                                  |

## 兼容策略

- 现有 Profile、Point API 保持命名不变，不为了贴合物模型术语新增同义别名接口。
- 新建设备直接写 `profile_id`；已有设备可留空，通过兼容路径（bind 查询）过渡。
- 自定义指令、事件新增表独立演进，不影响 `PointValue` 采集链路和现有 `PointCommand` 读写链路。
- 租户、启停、逻辑删除、版本字段遵循现有 `Profile / Point` 模式。

## 待确认问题

- 自定义指令结果是否需要强同步回传；若需要，命令通道要补充关联 ID 和响应队列。
- 事件运行记录是扩展现有 `dc3_entity_alarm`，还是新建更通用的事件实例表。
- 参数类型是否复用 `PointTypeFlagEnum`，还是抽象为通用 `ValueTypeEnum`。
- ~~已存在多 `Profile` 绑定数据时的迁移策略~~ — 已决策：不做数据迁移，新设备走新逻辑，老设备兼容过渡。

## 分阶段落地

### 第一阶段：设备与物模型单归属 [DONE]

**范围**：28 个文件，12 个类删除，16 个类修改。

**DDL**：
```sql
ALTER TABLE dc3_device ADD COLUMN profile_id BIGINT;
CREATE INDEX idx_device_profile ON dc3_device (tenant_id, profile_id) WHERE deleted = 0;
```

**代码改动清单**：

| 操作 | 文件 | 说明 |
|------|------|------|
| DELETE | `ProfileBindDO.java` | 模型类 |
| DELETE | `ProfileBindBO.java` | 业务对象 |
| DELETE | `ProfileBindVO.java` | 视图对象 |
| DELETE | `ProfileBindQuery.java` | 查询对象 |
| DELETE | `ProfileBindBuilder.java` | 构建器 |
| DELETE | `ProfileBindMapper.java` | Mapper 接口 |
| DELETE | `ProfileBindMapper.xml` | Mapper XML |
| DELETE | `ProfileBindManager.java` | Manager 接口 |
| DELETE | `ProfileBindManagerImpl.java` | Manager 实现 |
| DELETE | `ProfileBindService.java` | Service 接口 |
| DELETE | `ProfileBindServiceImpl.java` | Service 实现 |
| MODIFY | `ProfileBindingRow.java` | Dashboard DTO，精简为只有 profileId + deviceId |
| MODIFY | `DeviceDO.java` | 加 `profile_id` 字段 |
| MODIFY | `DeviceBO.java` | `Set<Long> profileIds` → `Long profileId` |
| MODIFY | `DeviceVO.java` | 同上 |
| MODIFY | `DeviceBuilder.java` | 适配新字段 |
| MODIFY | `DeviceServiceImpl.java` | 移除所有 bind 调用，直接读写 `profile_id` |
| MODIFY | `DeviceMapper.xml` | `selectPageWithProfile` 去掉 INNER JOIN |
| MODIFY | `ProfileServiceImpl.java` | `listByDeviceId()` 改用 `device.profile_id` |
| MODIFY | `PointServiceImpl.java` | device↔profile 解析改用 `device.profile_id` |
| MODIFY | `DriverServiceImpl.java` | `listDeviceIdsByProfileId` 查 `dc3_device.profile_id` |
| MODIFY | `TopicServiceImpl.java` | 点位解析链路 |
| MODIFY | `PointAttributeConfigServiceImpl.java` | bind 校验 → profile_id 校验 |
| MODIFY | `DashboardServiceImpl.java` | 拓扑图数据源 |
| MODIFY | `DashboardMapper.xml` | 拓扑 SQL |
| MODIFY | `ImportDeviceServiceImpl.java` | `importProfileBind` → 设 `profile_id` |
| MODIFY | `driver/entity/bo/DeviceBO.java` | `profileIds` → `profileId` |
| MODIFY | `driver/entity/builder/DeviceBuilder.java` | 适配 |
| MODIFY | `driver/job/DriverReadScheduleJob.java` | `getProfileIds()` → `getProfileId()` |

**出口标准**：
- `dc3_device` 有 `profile_id` 列
- `DeviceDO` / `DeviceBO` / `DeviceVO` 使用 `profileId` 单值
- ProfileBind 全链路物理删除
- Device/Point/Driver/Topic/Dashboard 服务不再引用 `dc3_profile_bind`
- 现有测试通过

### 第二阶段：模型定义闭环 [TODO]

- 保留 `Profile` 和 `Point`，新增 `dc3_command`、`dc3_command_param`、`dc3_event`、`dc3_event_param` 四张表。
- Manager 提供自定义指令、事件 CRUD、分页、按 `profile_id` 查询。
- Profile 详情聚合返回 Point、指令、事件。
- 增加同一 `Profile` 下 `point_code`、`command_code`、`event_code` 唯一校验。

### 第三阶段：运行链路闭环 [TODO]

- Data Center 扩展指令调用 API 和调用记录。
- Driver SDK 扩展自定义指令 DTO。
- Data Center 事件上报 API 支持按 `event_code` 关联模型定义。
- 虚拟驱动先实现一条示例自定义指令和一条示例事件，作为协议驱动参考。

### 第四阶段：产品化体验 [TODO]

- Web UI 保留 Profile 管理入口，在 Profile 详情中补齐 Point、指令、事件能力视图。
- Point、指令、事件统一展示为设备能力视图。
- 导入导出支持完整 Profile 能力模型 JSON。
- 文档补充操作手册和驱动适配指南。
