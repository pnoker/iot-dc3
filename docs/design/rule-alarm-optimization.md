---
title: 规则告警链路优化与缺陷修复方案
---

# 规则告警链路优化与缺陷修复方案

本文用于梳理当前 `rule / notify / channel / message` 告警链路的性能问题和功能缺陷，并给出后续优化方案。目标是在保留现有领域模型的前提下，让规则判断链路适合高频位号数据、设备状态告警和驱动状态告警。

## 核心结论

- 当前规则链路是通的，但属于功能优先版本。
- 规则判断本身不重，性能瓶颈主要来自每条事实数据都查询 `dc3_rule`。
- 位号数据已经批量写入，但规则判断仍逐条执行，批量优势没有传递到规则链路。
- 设备、驱动事件存在 `tenantId` 缺失后规则静默跳过的风险。
- 通知发送当前在规则处理链路中同步执行，外部网络耗时会反向拖慢数据消费。
- `RuleExt.window` 已建模但当前没有真正实现窗口计算。
- 恢复判断需要依赖已有 firing 状态，避免冷启动或普通数据触发无意义恢复。
- 后续应将 `event_id` 统一调整为 `alarm_id`，指向 `dc3_entity_alarm`。

## 当前链路

### 位号数据

```text
PointValueReceiver
  -> PointValueServiceImpl.save
  -> AlarmRuleTriggerService.processPointValue
  -> AlarmRulePipelineService.process
  -> RuleEngine.evaluate
  -> RuleNotificationService.notify
```

低速场景下，`PointValueReceiver` 直接调用 `PointValueService.save(pointValue)`。高速场景下，点值先进入 `PointValueJob` 缓冲，定时批量调用 `PointValueService.save(list)`。

问题在于批量保存后仍然逐条触发规则判断：

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

### 设备事件

```text
DeviceEventReceiver
  -> DeviceEventService.heartbeatEvent / alarmEvent
  -> AlarmRuleTriggerService.processDeviceEvent
  -> AlarmRulePipelineService.process
```

普通心跳只刷新状态缓存。只有以下情况会进入规则判断：

- 显式 `ALARM` 事件。
- 心跳导致在线族和离线族之间状态翻转。
- 当前 `OfflineExpiryListener` 判断设备状态 key 过期后生成离线事件。

### 驱动事件

```text
DriverEventReceiver
  -> DriverEventService.heartbeatEvent / alarmEvent
  -> AlarmRuleTriggerService.processDriverEvent
  -> AlarmRulePipelineService.process
```

驱动和设备类似，普通心跳不触发规则。显式告警、状态翻转、超时离线才触发规则。

后续设备与驱动超时方案落地后，入口应从 `LocalCacheService` 过期监听调整为：

```text
RabbitMQ timeout check
  -> 二次检查 dc3_entity_state
  -> 状态翻转
  -> 构建 DEVICE / DRIVER RuleFact
  -> 规则判断
```

## 当前缺陷

### 规则查询成本过高

当前 `RuleEngineImpl` 每处理一个 `RuleFact` 都查询一次 `dc3_rule`。位号数据是最高频入口，这会导致数据库查询量和点值上报量线性绑定。

现有查询条件为：

```text
tenant_id
alarm_target_type_flag
enable_flag
entity_id = 当前实体 or entity_id = 0
```

但当前表结构只提供了 `entity_id`、`notify_id`、`message_id` 等单列索引，缺少面向规则评估的组合索引。

### 批量写入没有批量判断

`PointValueJob` 已经把点值批量交给 `PointValueService.save(list)`，但规则判断仍然在保存后逐条执行。高吞吐时，规则判断会重新把批量数据拆散。

### 配置对象重复查询

规则命中后会加载：

- `dc3_notify`
- `dc3_message`
- `dc3_notify_channel_bind`
- `dc3_notify_channel`

这些对象变化频率很低，但当前链路按命中结果实时查询。告警量上来后，数据库会同时承受规则查询和通知配置查询。

### 通知同步发送

当前通知发送在 `RuleNotificationService.notify` 中同步执行。飞书、Webhook、Email 等外部系统慢或抖动时，会拖慢规则处理线程。

更关键的是，规则状态更新、通知历史写入和外部发送耦合在同一条调用链中，不利于失败重试、削峰和排查。

### 事件缺少 tenantId 时规则静默跳过

`AlarmRuleTriggerService` 会校验 `tenantId` 和实体 ID。缺失时直接返回。

设备、驱动事件服务当前在落库时会把缺失 `tenantId` 的事件写成 `0`，但规则判断需要真实租户 ID。这样会出现：

```text
事件入库成功
  -> 规则没有触发
  -> 用户看不到通知
```

这个问题对设备、驱动主动上报告警尤其敏感。

### 恢复判断可能脱离 firing 状态

当前 `RuleEvaluator.recovers` 只根据当前事实和 recovery 条件判断恢复，没有先确认该规则是否已有 firing 状态。

这会带来两个问题：

- 系统冷启动时，正常数据可能生成 recovered 状态。
- 没有触发过的规则也可能进入恢复通知判断。

恢复应当是一个状态转换，只能从 firing 转到 recovered。

### 窗口规则尚未真正实现

`RuleExt.Window` 已经定义了 `LAST / ALL / ANY / AVG / MIN / MAX / SUM / COUNT` 等模式，但当前 `RuleEvaluatorImpl` 实际只判断当前这条事实数据。

如果用户配置了窗口规则，系统现在没有按窗口聚合执行，容易造成配置语义和运行效果不一致。

### 告警级别口径不统一

当前通知策略按 `RuleMatch.severity` 判断通道级别过滤，而 `dc3_message.message_level` 在发送策略里没有起决定作用。

这容易让人误解：

```text
到底是 rule_ext.severity 决定告警级别
还是 message.message_level 决定告警级别
```

建议统一为：规则决定告警级别，消息模板只决定内容表达。

### rule_state 并发更新不够稳

`dc3_rule_state` 有唯一索引，但当前逻辑是先查再新增或更新。多实例或同一时间高并发命中同一规则时，可能出现：

- 并发插入冲突。
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
- `dc3_entity_alarm` 是告警事实记录，承载 point/device/driver 的统一告警。
- `dc3_notify_history` 是通知投递历史，可扩展为 pending/success/failed/retrying/skipped 全生命周期。
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

设备、驱动事件进入规则判断前，必须确保 `tenantId` 有值。

建议规则：

- DTO 携带 `tenantId` 时直接使用。
- DTO 缺失 `tenantId` 时，通过 `DeviceFacade` / `DriverFacade` 查询并补齐。
- 补齐失败时不触发规则，并记录 warn 日志。
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
  -> 写 dc3_entity_alarm
  -> 创建 dc3_notify_history，status_flag = pending
  -> 提交事务
  -> 发布 notify task MQ
  -> NotifyWorker 发送
  -> 更新 dc3_notify_history 为 success / failed / retrying / skipped
```

`NotifyHistoryStatusEnum` 已经有 `PENDING / SUCCESS / FAILED / RETRYING / SKIPPED`，可以复用 `dc3_notify_history` 表作为通知任务和投递结果表。

发送失败处理：

- 可重试错误更新为 `RETRYING`，递增 `retry_count`。
- 超过最大次数更新为 `FAILED`。
- 策略过滤、通道禁用、模板缺失更新为 `SKIPPED`。
- 外部响应内容继续写入 `response_ext`。

这样规则判断线程只负责产生告警和通知任务，不再等待外部系统响应。

## 数据库调整

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

### 规则入口

```text
AlarmRuleTriggerService
  processPointValue
  processPointValues
  processDeviceAlarm
  processDriverAlarm
```

### 规则核心

```text
RuleRegistry
  缓存规则定义
  按 tenant/type/entity 返回候选规则

RuleFactNormalizer
  将 point/device/driver 输入转换为 RuleFact

RuleEvaluator
  只负责条件判断，不查库，不发通知

RuleStateService
  负责 rule_state 原子状态转换

EntityAlarmService
  负责写 dc3_entity_alarm
```

### 通知链路

```text
NotifyTaskService
  创建 pending notify_history
  发布通知任务

NotifyWorker
  加载 notify/message/channel
  执行策略判断
  渲染模板
  调用通道适配器
  更新 notify_history
```

## 分阶段落地

### 第一阶段：缺陷修复

- 设备、驱动事件补齐 `tenantId`，不再默默用 `0` 触发规则。
- 恢复判断必须依赖已有 firing 状态。
- 明确窗口规则初版只支持 `LAST`，其他模式保存时拒绝或运行时跳过。
- 统一告警级别来源，优先使用 `rule_ext.severity`。
- 增加 `idx_rule_eval` 索引。

### 第二阶段：性能优化

- 增加规则缓存。
- 增加 `processPointValues` 批量规则入口。
- `PointValueService.save(list)` 改为批量触发规则判断。
- 通知配置对象增加缓存。

### 第三阶段：链路解耦

- `dc3_rule_state.event_id` 改为 `alarm_id`。
- `dc3_notify_history.event_id` 改为 `alarm_id`。
- 告警统一写入 `dc3_entity_alarm`。
- 通知发送异步化，`dc3_notify_history` 先落 pending，再由 worker 投递。

### 第四阶段：窗口能力

- 实现窗口聚合服务。
- 支持 `AVG / MIN / MAX / SUM / COUNT / ALL / ANY`。
- 为窗口规则增加单元测试、集成测试和压测场景。

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
