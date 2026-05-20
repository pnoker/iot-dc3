---
title: 实体告警统一表设计方案
---

# 实体告警统一表设计方案

本文用于评估将 `dc3_driver_event`、`dc3_device_event` 合并为统一实体告警表 `dc3_entity_alarm` 的设计。目标是把驱动、设备、位号的告警记录收敛到一张表，避免后续 Dashboard、规则告警、通知确认和查询接口长期维护两套事件表逻辑。

## 核心结论

- 新表命名建议为 `dc3_entity_alarm`。
- `dc3_entity_alarm` 放在 `dc3_data` schema，属于运行态告警数据。
- `dc3_driver_event`、`dc3_device_event` 一次性替换为 `dc3_entity_alarm`。
- `dc3_entity_state` 保存当前状态事实，`dc3_entity_alarm` 保存告警记录。
- 不使用 `dc3_event` 命名，避免和 Profile 下的物模型事件定义混淆。
- 告警触发仍复用现有 rule 链路：`POINT` 数据触发、`DEVICE` 事件触发、`DRIVER` 事件触发。
- 不做双写、双读、旧接口转换等兼容逻辑；旧表只作为一次性数据迁移来源。

## 命名调整

现有命名：

| 当前表 | 问题 |
| --- | --- |
| `dc3_driver_event` | 只能表达 driver，无法统一查询 point/device/driver 告警 |
| `dc3_device_event` | 同时承载 device 告警和 point 规则告警，语义已经不纯 |

建议命名：

| 新表 | 定位 |
| --- | --- |
| `dc3_entity_state` | 实体当前状态事实表 |
| `dc3_entity_alarm` | 实体告警记录表 |

命名口径：

- 数据库对象统一使用 `state` 表达运行态状态，如 `dc3_entity_state`、`dc3_rule_state`、`rule_state_id`。
- Java DTO、已有枚举和规则 fact 字段可继续使用 `status`，如 `DeviceStatus`、`DriverStatus`、`status == offline`。
- 不再新增 `dc3_entity_status`、`rule_status_id` 这类混用命名。

这里选择 `alarm` 而不是 `event`，是因为这张表只保存需要关注、确认、通知、追踪的告警记录；普通心跳、普通状态刷新不进入该表。

## 概念边界

```text
dc3_entity_state
  当前事实：现在在线/离线/故障/维护，最近心跳，租约到期时间

dc3_entity_alarm
  告警记录：离线告警、故障告警、规则告警、主动上报告警、恢复记录

dc3_rule / dc3_rule_state
  告警决策和持续态：什么条件算告警，当前规则是否 firing/recovered
```

## 触发关系

位号、设备、驱动的规则触发方式保持当前逻辑：

| 目标 | 触发源 | 规则入口 | 告警目标 |
| --- | --- | --- | --- |
| 位号 | 点位值上报 | `processPointValue` | `POINT` |
| 设备 | 设备离线、设备故障、设备主动告警、状态翻转 | `processDeviceAlarm` | `DEVICE` |
| 驱动 | 驱动离线、驱动故障、驱动主动告警、状态翻转 | `processDriverAlarm` | `DRIVER` |

告警表不负责判断是否超时，也不负责判断规则是否命中。推荐链路：

```text
状态/数据/告警进入 Data Center
  -> 构建 RuleFact
  -> 匹配 dc3_rule
  -> 更新 dc3_rule_state
  -> 写入 dc3_entity_alarm
  -> 执行通知
```

设备、驱动超时方案中的链路调整为：

```text
MQ 超时检查
  -> 二次检查 dc3_entity_state
  -> 确认离线后更新状态
  -> 构建 DEVICE / DRIVER RuleFact
  -> 命中规则后写 dc3_entity_alarm
```

如果希望所有离线都落告警记录，应提供默认全局规则：

```text
alarm_target_type_flag = DEVICE
entity_id = 0
condition.field = source
condition.operator = ==
condition.expected = device-offline
```

驱动同理配置 `DRIVER + source == driver-offline`。

## 表结构设计

字段风格参考现有 PostgreSQL 脚本：

- 枚举类字段使用 `*_flag SMALLINT`。
- 扩展字段使用 `JSON DEFAULT '{}'::JSON`。
- 索引命名使用 `idx_*`。
- 使用 `operate_time` 触发器维护更新时间。

```sql
CREATE TABLE dc3_entity_alarm
(
    id                       BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    alarm_target_type_flag   SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm target type flag, 0: point, 1: device, 2: driver
    entity_id                BIGINT   DEFAULT 0 NOT NULL,                 -- Alarm target entity ID
    driver_id                BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    device_id                BIGINT   DEFAULT 0 NOT NULL,                 -- Device ID
    point_id                 BIGINT   DEFAULT 0 NOT NULL,                 -- Point ID
    rule_id                  BIGINT   DEFAULT 0 NOT NULL,                 -- Rule ID
    rule_state_id            BIGINT   DEFAULT 0 NOT NULL,                 -- Rule state ID
    alarm_type_flag          SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm type flag, 0: rule, 1: offline, 2: fault, 3: state flip, 4: report
    alarm_source_flag        SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm source flag, 0: rule, 1: state timeout, 2: device report, 3: driver report, 4: system
    alarm_level_flag         SMALLINT DEFAULT 2 NOT NULL,                 -- Alarm level flag, 0: P0, 1: P1, 2: P2, 3: P3
    alarm_ext                JSON     DEFAULT '{}'::JSON        NOT NULL, -- Alarm extension information
    expired_time             BIGINT   DEFAULT 0 NOT NULL,                 -- Expiration duration, seconds
    confirm_flag             SMALLINT DEFAULT 0 NOT NULL,                 -- Confirmation flag, 0: unconfirmed, 1: confirmed
    tenant_id                BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,           -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,          -- Operation time
    deleted                  SMALLINT DEFAULT 0 NOT NULL                  -- Logical delete flag, 0: not deleted, 1: deleted
);

CREATE INDEX idx_entity_alarm_alert
    ON dc3_entity_alarm (tenant_id, alarm_level_flag, confirm_flag, create_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_entity_alarm_target
    ON dc3_entity_alarm (tenant_id, alarm_target_type_flag, entity_id, create_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_entity_alarm_driver
    ON dc3_entity_alarm (tenant_id, driver_id, create_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_entity_alarm_device
    ON dc3_entity_alarm (tenant_id, device_id, create_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_entity_alarm_point
    ON dc3_entity_alarm (tenant_id, point_id, create_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_entity_alarm_rule
    ON dc3_entity_alarm (tenant_id, rule_id, create_time DESC)
    WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_entity_alarm
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();
```

实际初始化 SQL 还应补充 `COMMENT ON TABLE` / `COMMENT ON COLUMN`，与现有脚本保持一致。

## 字段说明

| 字段 | 说明 |
| --- | --- |
| `alarm_target_type_flag` | 告警目标类型，沿用 `AlarmTargetTypeFlagEnum`：`0` point，`1` device，`2` driver |
| `entity_id` | 告警目标 ID；point 告警为 point_id，device 告警为 device_id，driver 告警为 driver_id |
| `driver_id` | 冗余字段，便于按驱动聚合查询 |
| `device_id` | 冗余字段，便于按设备聚合查询 |
| `point_id` | 冗余字段，便于按位号聚合查询 |
| `rule_id` | 命中的规则 ID；非规则直写告警为 `0` |
| `rule_state_id` | 对应规则状态 ID；初版可先置 `0` |
| `alarm_type_flag` | 告警类型，建议新增枚举：rule/offline/fault/state flip/report |
| `alarm_source_flag` | 告警来源，建议新增枚举：rule/state timeout/device report/driver report/system |
| `alarm_level_flag` | 告警级别，沿用 `AlarmMessageLevelFlagEnum` |
| `alarm_ext` | 告警上下文，如 source、status、message、rule 信息、原始 fact |

## 一次性迁移映射

旧表只作为一次性数据迁移来源。迁移完成后，初始化 SQL 和业务代码中不再保留 `dc3_driver_event`、`dc3_device_event`。

迁移时按以下规则转换：

| 旧表 | 旧字段 | 新字段 |
| --- | --- | --- |
| `dc3_driver_event` | `driver_id` | `alarm_target_type_flag = DRIVER`，`entity_id = driver_id`，`driver_id = driver_id` |
| `dc3_device_event` | `device_id` 且 `point_id = 0` | `alarm_target_type_flag = DEVICE`，`entity_id = device_id`，`device_id = device_id` |
| `dc3_device_event` | `point_id > 0` | `alarm_target_type_flag = POINT`，`entity_id = point_id`，`device_id = device_id`，`point_id = point_id` |
| 两张旧表 | `event_ext` | `alarm_ext` |
| 两张旧表 | `expired_time` | `expired_time` |
| 两张旧表 | `confirm_flag` | `confirm_flag` |

`event_type_flag` 不建议原样保留。旧表只有 `heartbeat/alarm`，新表只保存告警记录，可以用 `alarm_type_flag` 和 `alarm_source_flag` 表达更准确的语义。

## 代码命名调整

代码命名一步到位，不保留旧 Event 命名的适配层：

| 当前命名 | 新命名 |
| --- | --- |
| `DriverEventDO` / `DeviceEventDO` | `EntityAlarmDO` |
| `DriverEventManager` / `DeviceEventManager` | `EntityAlarmManager` |
| `DriverEventMapper` / `DeviceEventMapper` | `EntityAlarmMapper` |
| `DriverEventTypeEnum` / `DeviceEventTypeEnum` | `EntityAlarmTypeFlagEnum` 或 `AlarmTypeFlagEnum` |
| `event_ext` | `alarm_ext` |
| `event_type_flag` | `alarm_type_flag` / `alarm_source_flag` |

驱动/设备上报协议也需要同步清理 Event 命名：

| 当前命名 | 新命名 |
| --- | --- |
| `DriverEventDTO` | `DriverStateDTO` + `DriverAlarmDTO` |
| `DeviceEventDTO` | `DeviceStateDTO` + `DeviceAlarmDTO` |
| `DriverEventReceiver` | `DriverStateReceiver` + `DriverAlarmReceiver` |
| `DeviceEventReceiver` | `DeviceStateReceiver` + `DeviceAlarmReceiver` |
| `DriverEventService` | `DriverStateService` + `DriverAlarmService` |
| `DeviceEventService` | `DeviceStateService` + `DeviceAlarmService` |

本次改造直接拆分状态和告警：

| 职责 | 新命名 |
| --- | --- |
| 驱动心跳/状态上报 | `DriverStateDTO` / `DriverStateReceiver` / `DriverStateService` |
| 设备心跳/状态上报 | `DeviceStateDTO` / `DeviceStateReceiver` / `DeviceStateService` |
| 驱动主动告警上报 | `DriverAlarmDTO` / `DriverAlarmReceiver` / `DriverAlarmService` |
| 设备主动告警上报 | `DeviceAlarmDTO` / `DeviceAlarmReceiver` / `DeviceAlarmService` |

这样可以避免新设计中继续出现"状态走 EventDTO、告警走 EventDTO"的语义混用。

## 与规则链路的关系

规则链路建议调整为统一写 `dc3_entity_alarm`：

```text
RuleFact
  -> RuleEngine evaluate
  -> RuleNotification / RuleState
  -> AlarmEventRecordService 写 dc3_entity_alarm
```

现有 `AlarmEventRecordServiceImpl` 中的 `persistDeviceEvent` / `persistDriverEvent` 可合并为：

```text
persistEntityAlarm(match)
```

目标 ID 取值：

| 目标类型 | `entity_id` | 冗余字段 |
| --- | --- | --- |
| POINT | `pointId` | `point_id`、`device_id`、`driver_id` |
| DEVICE | `deviceId` | `device_id`、`driver_id` |
| DRIVER | `driverId` | `driver_id` |

## 与状态超时方案的关系

`dc3_entity_state` 和 `dc3_entity_alarm` 分工如下：

| 表 | 职责 |
| --- | --- |
| `dc3_entity_state` | 当前状态事实，供查询和超时判断使用 |
| `dc3_entity_alarm` | 告警历史，供告警列表、通知确认、规则追踪使用 |

设备或驱动离线时：

```text
MQ 超时检查确认离线
  -> 更新 dc3_entity_state.state_flag = offline
  -> 构建 DEVICE / DRIVER RuleFact
  -> 命中规则后写 dc3_entity_alarm
```

不建议在状态查询接口中直接写 `dc3_entity_alarm`，避免读接口产生副作用。

## 一次性落地步骤

### 1. 数据库

- 新增 `dc3_entity_alarm`。
- 从初始化 SQL 中移除 `dc3_driver_event`、`dc3_device_event`。
- 如需保留历史数据，提供一次性迁移 SQL，把旧表数据导入 `dc3_entity_alarm` 后删除旧表。

### 2. 数据模型

- 新增 `EntityAlarmDO/BO/VO/Mapper/Manager/Service`。
- 删除 `DriverEventDO`、`DeviceEventDO` 及对应 Manager/Mapper。
- 删除 `DriverEventTypeEnum`、`DeviceEventTypeEnum`，新增 `AlarmTypeFlagEnum`、`AlarmSourceFlagEnum`。

### 3. 规则告警

- `AlarmEventRecordService` 改为只写 `dc3_entity_alarm`。
- `persistDeviceEvent` / `persistDriverEvent` 合并为 `persistEntityAlarm`。
- `dc3_rule_state.event_id` 建议同步改名为 `alarm_id`，引用 `dc3_entity_alarm.id`。

### 4. 状态与告警入口

- 状态链路使用 `dc3_entity_state`。
- 告警链路使用 `dc3_entity_alarm`。
- 将 `DeviceEventDTO` / `DriverEventDTO` 拆分为 State DTO 和 Alarm DTO。

### 5. API 与前端

- 删除旧的 `/driver/event`、`/device/event` 语义接口。
- 新增或调整为实体告警查询接口，例如 `/entity/alarm`。
- Dashboard、告警列表、确认操作统一读取和更新 `dc3_entity_alarm`。
- Demo 数据统一写入 `dc3_entity_alarm`。

## 待确认

- 是否需要记录非告警类状态变化。如果需要，应另设 `dc3_entity_event` 或 `dc3_entity_state_history`，不要塞进 `dc3_entity_alarm`。
- 主动上报告警是否必须无规则也落库；如果必须，可以提供默认全局规则或保留直写入口。
- `alarm_type_flag`、`alarm_source_flag` 的枚举值是否放入 `dc3-common-constant`。
- 是否同步把 `dc3_rule_state.event_id` 改为 `alarm_id`。
