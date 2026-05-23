---
title: 事件上报方案
---

# 事件上报方案

本文描述 DC3 从设备事件上报到数据持久化再到告警规则触发的完整链路设计。

> **背景**：Phase 2 完成了 `dc3_event` / `dc3_event_param` 表结构定义，Phase 4-5 补齐了事件上报运行时链路，Phase 6
> 补齐了告警规则触发。

---

## Event 模型

事件是设备主动上报的业务事件（故障、告警、模式切换等），定义归属 `Profile`，实例归属 `Device`。

### 事件定义表

| 表                 | 作用       |
|-------------------|----------|
| `dc3_event`       | 事件主定义    |
| `dc3_event_param` | 事件输出参数定义 |

### 事件类型

| 类型          | 说明     |
|-------------|--------|
| `info`      | 信息事件   |
| `alert`     | 告警事件   |
| `fault`     | 故障事件   |
| `lifecycle` | 生命周期事件 |

### 事件级别

| 级别 | 说明 |
|----|----|
| 0  | 普通 |
| 1  | 警告 |
| 2  | 严重 |
| 3  | 紧急 |

---

## EventRecord vs EntityAlarm

| 概念   | EventRecord        | EntityAlarm                     |
|------|--------------------|---------------------------------|
| 定位   | 事件上报原始日志           | 告警引擎评估后的运行态告警记录                 |
| 触发方式 | 设备主动上报             | 规则引擎匹配后创建                       |
| 记录表  | `dc3_event_record` | `dc3_entity_alarm`              |
| 关联链路 | 设备 → 事件定义          | 多个来源（DEVICE/DRIVER/POINT/EVENT） |

设备上报事件后：

1. 持久化到 `dc3_event_record`（原始日志）
2. 提交到告警规则引擎评估
3. 匹配告警规则时，由 `AlarmEventRecordService.ensureEvent()` 在 `dc3_entity_alarm` 中创建/更新告警记录

---

## RabbitMQ 拓扑

```
dc3.e.event (topic exchange)
  └── dc3.q.event.{service} (TTL 60s)
```

**路由键**：`dc3.r.event.{service}`

**消息流**：

1. Driver 通过 `DriverSenderService.eventReportSender()` 发布 `EventReportDTO`
2. Data Center 的 `EventReportReceiver` 消费
3. `EventReportServiceImpl.report(EventReportDTO)` 持久化 `EventRecordDO`
4. `AlarmRuleTriggerService.processEventReport()` 提交到告警规则引擎
5. 规则引擎匹配后触发告警通知

---

## API 路径

| 方法   | 路径                              | 说明         |
|------|---------------------------------|------------|
| POST | `/data/event_report/report`     | 上报事件（REST） |
| GET  | `/data/event_report/{recordId}` | 查询事件记录详情   |
| POST | `/data/event_report/list`       | 分页查询事件记录列表 |

gRPC API 路径（Data Center）：

- `DataService.EventReport` — 上报事件
- `DataService.EventRecord` — 查询事件记录
- `DataService.EventRecordList` — 分页查询

---

## Driver SDK 扩展点

### DriverSenderService

- `eventReportSender(EventReportDTO entityDTO)` — 上报事件
- 自动从 `DriverMetadata` 填充 `driverId` 和 `tenantId`

### EventReportDTO

```java
public record EventReportDTO(
    String recordId,      // 事件记录唯一 ID (UUID)
    Long tenantId,        // 租户 ID
    Long deviceId,        // 设备 ID
    Long eventId,         // 事件定义 ID
    String eventCode,     // 事件标识符
    Byte eventTypeFlag,   // 事件类型
    Byte eventLevelFlag,  // 事件级别
    Map<String, String> paramValues, // 事件输出参数
    String message,       // 事件描述
    Instant occurTime,    // 发生时间
    int schemaVersion     // 协议版本
)
```

---

## 告警规则触发

新增 `AlarmTargetTypeFlagEnum.EVENT` 目标类型，事件上报后自动串入告警规则引擎：

1. `EventReportServiceImpl.report(EventReportDTO)` 持久化后调用 `alarmRuleTriggerService.processEventReport(dto)`
2. 构建 `RuleFact(tenantId, EVENT, deviceId, alarmId=null, ts, RuleFactValues.eventReport(dto))`
3. 规则引擎评估 `EventReportSnapshot` 中的字段（`eventCode`, `eventTypeFlag`, `eventLevelFlag`, `paramValues`, `message`）
4. 规则条件示例：`eventCode == 'DOOR_OPEN' && eventLevelFlag == 3`

---

## 可靠性

- 消息持久化（durable exchange + queue）
- 消费者手动 ack，异常 nack + requeue
- 必填字段校验（recordId / deviceId / eventId 为 null → reject）
- 事件记录与告警通知解耦：事件记录成功后立即返回，告警通知异步处理
