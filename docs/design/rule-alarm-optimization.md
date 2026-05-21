---
title: 规则告警链路优化与缺陷修复方案
---

# 规则告警链路优化与缺陷修复方案

本文用于梳理当前 `rule / notify / channel / message` 告警链路的性能问题和功能缺陷，并给出后续优化方案。目标是在保留现有领域模型的前提下，让规则判断链路适合高频位号数据、设备状态告警和驱动状态告警。

> 现状核对：2026-05-21。本文初版描述的链路在命名上已部分演进：`DeviceEventService.heartbeatEvent / alarmEvent` 已合并为 `DeviceAlarmService.alarm`，`processDeviceEvent / processDriverEvent` 已重命名为 `processDeviceAlarm / processDriverAlarm`，并且 `dc3_entity_alarm` 表与 `AlarmEventRecordService` 雏形已经就位。下文已按当前代码事实做修订；架构方向不变。

## 核心结论

- 当前规则链路是通的，但属于功能优先版本。
- 规则判断本身不重，性能瓶颈主要来自每条事实数据都查询 `dc3_rule`。
- 位号数据已经批量写入，但规则判断仍逐条执行，批量优势没有传递到规则链路。
- 设备、驱动告警存在 `tenantId` 缺失后规则静默跳过的风险（DTO 落库会写 `tenant_id = 0`，但 `AlarmRuleTriggerService` 又要求 `tenantId > 0`）。
- 通知发送当前在规则处理链路中同步执行，外部网络耗时会反向拖慢数据消费。
- `RuleExt.window` 已建模但当前 `RuleEvaluatorImpl` 完全不看 window 字段，只对单条事实求值。
- 恢复判断需要依赖已有 firing 状态，避免冷启动或普通数据触发无意义恢复。
- 后续应将各表的 `event_id` 字段统一改为 `alarm_id`，指向 `dc3_entity_alarm.id`（DO 层目前仍为 `event_id`，但 `AlarmEventRecordService` 已经在写 `dc3_entity_alarm` 并把生成的 alarmId 反写到 `RuleFact.eventId`）。

## 当前链路

### 位号数据

```text
PointValueReceiver
  -> PointValueServiceImpl.save
  -> AlarmRuleTriggerService.processPointValue
  -> AlarmRulePipelineServiceImpl.process
  -> RuleEngineImpl.evaluate
  -> AlarmEventRecordService.ensureEvent  // firing 时落 dc3_entity_alarm
  -> RuleNotificationServiceImpl.notify   // 同步发送
```

低速场景下，`PointValueReceiver` 直接调用 `PointValueService.save(pointValue)`。高速场景下，点值先进入 `PointValueJob` 缓冲，定时批量调用 `PointValueService.save(list)`，分流阈值为 `dc3.point.batch.speed`。

问题在于批量保存后仍然逐条触发规则判断（`PointValueServiceImpl.save(List)`）：

```text
pointValueBOList.forEach(alarmRuleTriggerService::processPointValue)
```

因此当前性能模型仍是：

```text
N 条点值
  -> N 次构建 RuleFact
  -> N 次查询 dc3_rule
  -> N 次遍历候选规则
```

### 设备告警

```text
DeviceAlarmReceiver
  -> DeviceAlarmServiceImpl.alarm   // 写 dc3_entity_alarm
  -> AlarmRuleTriggerService.processDeviceAlarm
  -> AlarmRulePipelineServiceImpl.process
```

`DeviceAlarmServiceImpl.alarm` 在落库 `dc3_entity_alarm` 时使用 `AlarmSourceFlagEnum.DEVICE_REPORT`，并将生成的 `alarmId` 注入到下游 `DeviceAlarmDTO` 中再投递给规则触发器。Data-center 侧目前已经不区分 heartbeat / alarm 两种事件类型，所有进入规则判断的设备事件都以 alarm 形态出现。

进入规则判断的来源：

- 上游显式上报的 `DeviceAlarmDTO`。
- `OfflineExpiryListener` 监听到设备状态 key 在 `LocalCacheService` 过期后合成的 OFFLINE 告警（`AlarmSourceFlagEnum.STATE_TIMEOUT`）。

### 驱动告警

```text
DriverAlarmReceiver
  -> DriverAlarmServiceImpl.alarm   // 写 dc3_entity_alarm
  -> AlarmRuleTriggerService.processDriverAlarm
  -> AlarmRulePipelineServiceImpl.process
```

驱动链路与设备同构。`OfflineExpiryListener` 同样负责从 `DRIVER_STATUS_KEY_PREFIX` 过期事件合成 driver OFFLINE 告警。

后续设备与驱动超时方案落地后，入口应从 `LocalCacheService` 过期监听调整为：

```text
RabbitMQ timeout check
  -> 二次检查 dc3_entity_state
  -> 状态翻转
  -> 构建 DEVICE / DRIVER RuleFact
  -> 规则判断
```

## 当前缺陷

> 每条缺陷后附 **状态** 标签：仍存在 / 已部分修复 / 已修复。

### 规则查询成本过高 — 仍存在

`RuleEngineImpl.loadCandidateRules` 每处理一个 `RuleFact` 都用 MyBatis-Plus `lambdaQuery().list()` 查询一次 `dc3_rule`。位号数据是最高频入口，这会导致数据库查询量和点值上报量线性绑定。

现有查询条件为：

```text
tenant_id
alarm_target_type_flag
enable_flag
entity_id = 当前实体 or entity_id = 0
```

仓库内不维护建表 DDL（`dc3-common-test/src/main/resources/db/init-timescale.sql` 仅包含扩展声明），索引结构需要由 DBA 侧确认。本文按"仅有单列默认索引、缺少 `(tenant_id, alarm_target_type_flag, enable_flag, entity_id)` 组合索引"的常见情况推进设计。

### 批量写入没有批量判断 — 仍存在

`PointValueJob` 已经把点值批量交给 `PointValueServiceImpl.save(List)`，但内部最后一步仍是 `pointValueBOList.forEach(alarmRuleTriggerService::processPointValue)`，规则判断把批量数据重新拆散。

### 配置对象重复查询 — 仍存在

规则命中后 `RuleNotificationServiceImpl` 会按 id 实时加载：

- `dc3_notify`（`loadNotify`）
- `dc3_message`（`loadMessage`）
- `dc3_notify_channel_bind`（`loadEnabledBinds`）
- `dc3_notify_channel`（`loadChannel`）

这些对象变化频率很低，但当前链路按命中结果实时查询。告警量上来后，数据库会同时承受规则查询和通知配置查询。

### 通知同步发送 — 仍存在

`RuleNotificationServiceImpl.notify` 在 `@Transactional` 方法内串行 `notifyChannelAdapterRegistry.find(...).send(channel, payload)`，`dc3_notify_history` 直接以最终状态写入。飞书、Webhook、Email 等外部系统慢或抖动时，会拖慢规则处理线程，整条规则事务也会跟着拉长。

更关键的是，规则状态更新、通知历史写入和外部发送耦合在同一条调用链 + 同一事务中，不利于失败重试、削峰和排查。

注：`NotifyHistoryStatusEnum` 已经预留了 `PENDING / SUCCESS / FAILED / RETRYING / SKIPPED` 五个状态，下面的异步化方案可以直接复用。

### 告警缺少 tenantId 时规则静默跳过 — 仍存在

`AlarmRuleTriggerServiceImpl.processDeviceAlarm` / `processDriverAlarm` 通过 `isValidId(tenantId)` 校验，要求 `tenantId > 0`。

但 `DeviceAlarmServiceImpl.alarm` / `DriverAlarmServiceImpl.alarm` 在落库 `dc3_entity_alarm` 时使用 `Objects.nonNull(entityDTO.getTenantId()) ? entityDTO.getTenantId() : 0L`，缺失时直接写 `0`；DTO 中的 `tenantId` 也保持原值（null/0）传给规则触发器。结果就是：

```text
事件入库成功（tenant_id = 0）
  -> 规则触发器看到 tenantId 不合法
  -> 规则没有触发
  -> 用户看不到通知
```

`OfflineExpiryListener` 在合成超时告警时已经会通过 `DriverFacade` / `DeviceFacade` 反查 tenantId，所以超时路径相对稳。问题主要集中在 driver/device 主动上报告警的入口。

### 恢复判断可能脱离 firing 状态 — 仍存在

`RuleEngineImpl.evaluate` 内部对每条候选规则同时尝试 `matches` 和 `recovers`，没有读取 `dc3_rule_state` 也没有判断当前是不是 FIRING。`RuleNotificationServiceImpl.persistRuleState` 的 recovery 分支也直接写 `RECOVERED`，不要求前一状态是 `FIRING`。

这会带来两个问题：

- 系统冷启动时，正常数据可能生成 `RECOVERED` 状态。
- 没有触发过的规则也可能进入恢复通知判断。

恢复应当是一个状态转换，只能从 `FIRING` 转到 `RECOVERED`。

### 窗口规则尚未真正实现 — 仍存在

`RuleExt.Window` 已经定义了 `mode (LAST / ALL / ANY / AVG / MIN / MAX / SUM / COUNT)` / `duration` / `minSamples`，但 `RuleEvaluatorImpl.matches` / `recovers` 完全不读 window 字段，只对当前一条事实求值。

如果用户配置了窗口规则，系统现在没有按窗口聚合执行，容易造成配置语义和运行效果不一致。

### 告警级别口径不统一 — 仍存在

`NotifyPolicyEngineImpl.levelAllowed` 仅基于 `RuleMatch.severity` 与 `NotifyChannelBindExt.Content.levels` 做匹配；`dc3_message.message_level` 在发送策略里没有起决定作用。

这容易让人误解：

```text
到底是 rule_ext.severity 决定告警级别
还是 message.message_level 决定告警级别
```

建议统一为：规则决定告警级别，消息模板只决定内容表达。

### rule_state 并发更新不够稳 — 仍存在

`RuleNotificationServiceImpl.persistRuleState` 是先 `loadState` 再 `save` 或 `updateById`，不是数据库原子 upsert。多实例或同一时间高并发命中同一规则时，可能出现：

- 并发插入冲突（依赖 `tenant_id + rule_id + alarm_target_type_flag + entity_id + fingerprint` 上的唯一索引兜底）。
- `trigger_count` 丢增量。
- `last_notify_time` 被旧请求覆盖。

规则状态应改为数据库原子 upsert。

## 目标架构

优化后的链路建议拆成三段：

```text
事实进入
  -> RuleFact 标准化
  -> 规则匹配与状态转换
  -> 告警落库
  -> 通知任务投递
  -> 异步通知发送
```

### 运行原则

- `dc3_rule` 是规则定义，不承载运行状态。
- `dc3_rule_state` 是规则运行态，只记录 firing/recovered、触发次数、最近通知时间等。
- `dc3_entity_alarm` 是告警事实记录，承载 point/device/driver 的统一告警 — 表已存在，`AlarmEventRecordServiceImpl.ensureEvent` 已经在 firing 时写入，并通过 `RuleStateDO.eventId` 索引现有 firing 行让 recovery 复用 alarmId。
- `dc3_notify_history` 是通知投递历史，`NotifyHistoryStatusEnum` 已具备 pending/success/failed/retrying/skipped 五状态，可直接扩展为全生命周期。
- 规则判断链路不直接依赖外部通知系统成功与否。

## 优化方案

### 规则缓存

新增规则缓存服务，建议命名为 `RuleRegistry` 或 `RuleCacheService`。

缓存维度：

```text
tenantId + alarmTargetTypeFlag + entityId
tenantId + alarmTargetTypeFlag + 0
```

评估某个实体时，候选规则为：

```text
实体专属规则 + 同类型全局规则
```

缓存内容为已经转换好的 `RuleBO`，避免每次查询后重复做 DO 到 BO 转换。

失效策略：

- 管理端新增、更新、删除、启停规则时主动失效对应租户缓存。
- Data Center 本地缓存设置较短兜底 TTL，例如 30 到 60 秒。
- 多实例场景可通过 RabbitMQ 发布规则变更事件，其他实例收到后清理本地缓存。

### 规则评估索引

`dc3_rule` 增加面向评估链路的组合索引：

```sql
CREATE INDEX idx_rule_eval
    ON dc3_rule (tenant_id, alarm_target_type_flag, enable_flag, entity_id)
    WHERE deleted = 0;
```

如果启用规则缓存，这个索引主要用于缓存重建和冷启动加载。

### 位号批量判断

新增批量入口：

```text
AlarmRuleTriggerService.processPointValues(List<PointValueBO>)
```

批量处理步骤：

1. 过滤无效点值。
2. 按 `tenantId + pointId` 分组。
3. 为每组加载一次候选规则。
4. 组内逐条执行规则判断。
5. 批量写入或更新 `dc3_rule_state`。
6. 批量写入 `dc3_entity_alarm`。
7. 批量创建 `dc3_notify_history` pending 任务。

这样可以把规则查询成本从：

```text
N 条点值 -> N 次规则查询
```

降低为：

```text
N 条点值 -> 按 pointId 分组后的 M 次缓存读取
```

在规则缓存命中时，数据库查询可进一步降为 0。

### 设备和驱动补齐租户上下文

设备、驱动告警进入规则判断前，必须确保 `tenantId` 有值。

建议规则：

- DTO 携带 `tenantId` 时直接使用。
- DTO 缺失 `tenantId` 时，通过 `DeviceFacade` / `DriverFacade` 查询并补齐（`OfflineExpiryListener` 已在用这个模式，可以推广到 `DeviceAlarmServiceImpl` / `DriverAlarmServiceImpl`）。
- 补齐失败时不入库 `dc3_entity_alarm`，并记录 warn 日志，避免出现"alarm 入库 + 规则不触发"的隐性丢失。
- 落库事件或告警时不再默默写 `tenant_id = 0`。

这可以避免事件已入库但规则没有触发的隐性缺陷。

### 恢复状态约束

恢复判断必须依赖已有 firing 状态。

推荐流程：

```text
当前事实进入
  -> 加载候选规则
  -> 加载对应 rule_state
  -> 如果 rule_state 是 FIRING，才允许判断 recovery
  -> recovery 命中后更新为 RECOVERED
```

如果没有 firing 状态，即使当前事实满足 recovery 条件，也不创建 recovered 状态，不发送恢复通知。

### 窗口规则处理

窗口规则分两阶段落地。

第一阶段：

- 明确只支持 `LAST` 模式。
- 当 `window.mode` 为空或 `LAST` 时，按当前事实判断。
- 当配置了 `AVG / MIN / MAX / SUM / COUNT / ALL / ANY` 等模式时，规则保存阶段拒绝，或运行时标记为不支持并跳过。

第二阶段：

- 新增窗口聚合服务。
- 位号窗口可以基于最近值缓存或时序库查询。
- 设备、驱动窗口可以基于 `dc3_entity_state` 和 `dc3_entity_alarm` 聚合。
- 聚合结果仍转换为标准 `RuleFact` 后进入相同规则链路。

不要让规则配置表面支持窗口，但运行时实际忽略窗口。

### 告警级别统一

建议统一口径：

| 字段 | 定位 |
| --- | --- |
| `dc3_rule.rule_ext.severity` | 告警级别事实来源 |
| `dc3_entity_alarm.alarm_level_flag` | 告警记录中的级别 |
| `dc3_notify_channel_bind.bind_ext.levels` | 通道允许发送的告警级别 |
| `dc3_message.message_level` | 可删除或降级为模板默认展示级别 |

如果保留 `message_level`，也不建议它参与通知路由判断，避免同一条告警在规则和模板之间出现两个级别。

### rule_state 原子 upsert

`dc3_rule_state` 更新应改成数据库原子写。

建议规则：

- 唯一键仍使用 `tenant_id + rule_id + alarm_target_type_flag + entity_id + fingerprint`。
- firing 时使用 `INSERT ... ON CONFLICT DO UPDATE`。
- `trigger_count = trigger_count + 1` 在数据库内完成。
- `last_trigger_time`、`last_recover_time`、`last_notify_time` 按状态转换更新。

这样可以避免多实例并发下状态重复和计数丢失。

### 通知异步化

建议把通知发送从规则判断主链路中拆出去。

推荐链路：

```text
规则命中
  -> 写 dc3_rule_state
  -> 写 dc3_entity_alarm（AlarmEventRecordService.ensureEvent 已在做）
  -> 创建 dc3_notify_history，status_flag = pending
  -> 提交事务
  -> 发布 notify task MQ
  -> NotifyWorker 发送
  -> 更新 dc3_notify_history 为 success / failed / retrying / skipped
```

`NotifyHistoryStatusEnum.PENDING / SUCCESS / FAILED / RETRYING / SKIPPED` 已经定义，可以直接复用 `dc3_notify_history` 表作为通知任务和投递结果表。当前实现中 `RuleNotificationServiceImpl.historySkipped` / `persistHistory` 已经会按结果写入 `SKIPPED` / `SUCCESS` / `FAILED`，缺的主要是"先 PENDING 落库 + 后续异步更新"这一步。

发送失败处理：

- 可重试错误更新为 `RETRYING`，递增 `retry_count`。
- 超过最大次数更新为 `FAILED`。
- 策略过滤、通道禁用、模板缺失更新为 `SKIPPED`。
- 外部响应内容继续写入 `response_ext`。

这样规则判断线程只负责产生告警和通知任务，不再等待外部系统响应。

## 数据库调整

> 仓库内不维护 dc3 业务表的 DDL（`dc3-common-test/src/main/resources/db/init-timescale.sql` 仅声明扩展），下面的索引和字段重命名需要由 DBA 侧或独立迁移脚本执行。

### dc3_rule

新增评估索引：

```sql
CREATE INDEX idx_rule_eval
    ON dc3_rule (tenant_id, alarm_target_type_flag, enable_flag, entity_id)
    WHERE deleted = 0;
```

### dc3_rule_state

字段调整：

```text
event_id -> alarm_id
```

`alarm_id` 指向 `dc3_entity_alarm.id`。

> 当前 `RuleStateDO.eventId` 已经在 `AlarmEventRecordServiceImpl.findFiringEventId` 内被当作 `alarmId` 使用。重命名时需要同步更新 `RuleStateDO` / `NotifyHistoryDO` / `RuleStateBO` / `NotifyHistoryBO` / `RuleFact.eventId` 等所有引用，以及 `RuleNotificationServiceImpl.persistRuleState` / `persistHistory` 中的字段名。

建议补充索引：

```sql
CREATE INDEX idx_rule_state_alarm
    ON dc3_rule_state (tenant_id, alarm_id)
    WHERE deleted = 0 AND alarm_id > 0;
```

### dc3_notify_history

字段调整：

```text
event_id -> alarm_id
```

`alarm_id` 指向 `dc3_entity_alarm.id`。

建议补充 pending 任务索引：

```sql
CREATE INDEX idx_notify_history_pending
    ON dc3_notify_history (tenant_id, status_flag, create_time)
    WHERE deleted = 0;
```

原 `idx_notify_history_event` 同步调整为 `idx_notify_history_alarm`。

## 推荐代码结构

> 现状提示：`AlarmRuleTriggerService` 的 `processPointValue` / `processDeviceAlarm` / `processDriverAlarm` 已经存在，缺的是 `processPointValues` 批量入口；`RuleEvaluator` 已经只做条件判断；`AlarmEventRecordServiceImpl` 是下面 `EntityAlarmService` 的雏形；`RuleStateService` 仓库里已有但只承担 `dc3_rule_state` 的 CRUD（manager 侧使用），下面所说的"规则状态机服务"指的是把 `RuleNotificationServiceImpl.persistRuleState` 里的状态转换逻辑抽出去并改成原子 upsert，可以新建一个独立服务（例如 `RuleStateMachine`）以避免和现有 BaseService CRUD 混在一起。

### 规则入口

```text
AlarmRuleTriggerService
  processPointValue
  processPointValues   // 待新增
  processDeviceAlarm
  processDriverAlarm
```

### 规则核心

```text
RuleRegistry            // 待新增
  缓存规则定义
  按 tenant/type/entity 返回候选规则

RuleFactNormalizer      // 待新增（当前转换逻辑分散在 AlarmRuleTriggerServiceImpl / RuleFactValues）
  将 point/device/driver 输入转换为 RuleFact

RuleEvaluator           // 已存在，仅条件判断
  只负责条件判断，不查库，不发通知

RuleStateMachine        // 待新增（与 RuleStateService CRUD 区分开）
  负责 rule_state 原子状态转换

EntityAlarmService      // AlarmEventRecordServiceImpl 已是雏形
  负责写 dc3_entity_alarm
```

### 通知链路

```text
NotifyTaskService       // 待新增
  创建 pending notify_history
  发布通知任务

NotifyWorker            // 待新增
  加载 notify/message/channel
  执行策略判断
  渲染模板
  调用通道适配器
  更新 notify_history
```

## 分阶段落地

### 第一阶段：缺陷修复 — 全部待做

- [ ] 设备、驱动告警补齐 `tenantId`，不再默默用 `0` 入库 + 静默跳过规则。
- [ ] 恢复判断必须依赖已有 firing 状态（`RuleEngineImpl.evaluate` + `RuleNotificationServiceImpl.persistRuleState` 两处都要约束）。
- [ ] 明确窗口规则初版只支持 `LAST`，其他模式保存时拒绝或运行时跳过。
- [ ] 统一告警级别来源，优先使用 `rule_ext.severity`；`message_level` 退化成模板默认。
- [ ] 增加 `idx_rule_eval` 索引（DDL 由 DBA 落地）。

### 第二阶段：性能优化 — 全部待做

- [ ] 增加规则缓存（`RuleRegistry`）。
- [ ] 增加 `processPointValues` 批量规则入口。
- [ ] `PointValueServiceImpl.save(List)` 改为批量触发规则判断（去掉当前的 `forEach(processPointValue)`）。
- [ ] 通知配置对象增加缓存（notify / message / channel / bind）。

### 第三阶段：链路解耦 — 部分已做

- [x] 告警统一写入 `dc3_entity_alarm`（`AlarmEventRecordServiceImpl`、`OfflineExpiryListener`、`DeviceAlarmServiceImpl`、`DriverAlarmServiceImpl` 均已写入）。
- [ ] `dc3_rule_state.event_id` 改为 `alarm_id`（含 DO/BO/Query/Builder/RuleFact 全链路重命名）。
- [ ] `dc3_notify_history.event_id` 改为 `alarm_id`。
- [ ] 通知发送异步化，`dc3_notify_history` 先落 pending，再由 worker 投递（状态枚举已就绪，缺 worker 与 PENDING 分支）。

### 第四阶段：窗口能力 — 全部待做

- [ ] 实现窗口聚合服务。
- [ ] 支持 `AVG / MIN / MAX / SUM / COUNT / ALL / ANY`。
- [ ] 为窗口规则增加单元测试、集成测试和压测场景。

## 验证重点

### 功能测试

- 位号阈值规则 firing。
- 位号恢复规则只在已有 firing 状态后 recovered。
- 设备 offline 规则命中。
- 驱动 offline 规则命中。
- `entity_id = 0` 全局规则命中。
- 通道级别过滤生效。
- 静默期和限流生效。
- 模板变量缺失时通知历史可追踪。

### 并发测试

- 同一位号高频触发同一规则，`dc3_rule_state` 不重复。
- 多实例同时处理同一实体告警，`trigger_count` 不丢增量。
- 通知 worker 重试不会重复发送已成功历史。

### 性能测试

需要压测以下指标：

| 指标 | 目标 |
| --- | --- |
| 点值消费吞吐 | 不因规则查询线性下降 |
| 规则缓存命中率 | 稳定后接近 100% |
| 单条点值规则判断耗时 | 避免数据库查询后保持毫秒级 |
| 通知发送耗时 | 不影响点值消费主链路 |
| 数据库 QPS | 规则缓存开启后明显下降 |

## 最终形态

优化完成后的核心链路：

```text
PointValue / EntityState / EntityAlarmFact
  -> RuleFactNormalizer
  -> RuleRegistry cached rules
  -> RuleEvaluator
  -> RuleStateService upsert
  -> EntityAlarmService
  -> NotifyTaskService pending history
  -> NotifyWorker async send
```

这样可以保持当前模型的清晰分工：

```text
dc3_rule          定义什么情况下告警
dc3_rule_state    记录规则当前运行状态
dc3_entity_alarm  记录已经发生的告警事实
dc3_notify        定义通知策略
dc3_message       定义消息模板
dc3_notify_channel 定义投递通道
dc3_notify_history 保存通知任务和投递历史
```
