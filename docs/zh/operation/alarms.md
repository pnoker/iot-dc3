---
title: 告警与通知
---

<script setup>
import AlarmSourceFlowDiagram from '../../.vitepress/theme/components/AlarmSourceFlowDiagram.vue'
import AlarmErDiagram from '../../.vitepress/theme/components/AlarmErDiagram.vue'
import AlarmNotifyFlowDiagram from '../../.vitepress/theme/components/AlarmNotifyFlowDiagram.vue'
</script>


# 告警与通知

平台把"什么时候出问题、谁该被告知"统一收敛到一张运行告警表和一条通知链路。读完这页，你能看懂五类告警来源如何汇入
`dc3_entity_alarm`、前端三个告警视图是怎么从同一张表过滤出来的，以及规则触发后通知是怎么经由邮件/短信/Webhook 发出去的。

> 你在这里：已[接入一个设备](./device-onboarding)
> 并有数据流转，想为异常配置告警与通知。要理解数据从哪来，可回看[数据平面](../architecture/data-plane)。

## 为什么是"一张表 + 一条链路"

现场会从很多角度出问题：规则命中阈值、设备/驱动心跳超时、设备主动上报故障、驱动上报异常、设备上报的事件触发了规则。如果每类来源各建一套表、各走一套通知，运维就要在多个页面之间来回对账。IoT
DC3 的选择是：**所有运行告警，无论来源，统一落在 `dc3_entity_alarm`**；用 `alarm_source_flag`（来自哪里）和
`alarm_target_type_flag`（针对什么实体）两个标志位区分，再靠索引把"驱动告警/设备告警/位号告警"这类视图快速过滤出来。

这条统一表与一条"规则 → 状态机 → 通知"的链路配合，构成告警子系统的两个支柱：前者是**事实记录**，后者是**触发与送达**。

## 五类来源如何汇入一张表

五类告警来源最终都写入 `dc3_entity_alarm`，区别只在标志位。`alarm_source_flag` 的取值来自 `AlarmSourceTypeEnum`，注意
`EVENT_REPORT=5`、`SYSTEM=4`（5 号为持久化兼容保留，枚举里排在 4 之后）：

<AlarmSourceFlowDiagram lang="zh" />

`alarm_source_flag`（来自哪里）和 `alarm_type_flag`（发生了什么）是两个独立维度，不要混淆：

- **来源 `alarm_source_flag`**：`0=RULE`、`1=STATE_TIMEOUT`、`2=DEVICE_REPORT`、`3=DRIVER_REPORT`、`4=SYSTEM`、
  `5=EVENT_REPORT`。
- **类型 `alarm_type_flag`**：`0=RULE`（规则命中）、`1=OFFLINE`（心跳超时）、`2=FAULT`（设备内部故障）、`3=STATE_FLIP`（实体状态翻转）、
  `4=REPORT`（外部事件上报）。

### 三个前端告警视图，同一张表

前端 Settings 下的三个告警页面查询的都是 `dc3_entity_alarm`（经 `POST /api/v3/data/dashboard/alert/page`），靠请求体里的
`source` 字符串过滤出不同实体维度：

| 视图   | 路由路径                     | 过滤参数            |
|------|--------------------------|-----------------|
| 驱动告警 | `/settings/alarm/driver` | `source=driver` |
| 设备告警 | `/settings/alarm/device` | `source=device` |
| 位号告警 | `/settings/alarm/point`  | `source=point`  |

这套过滤之所以快，靠的是建在表上的复合索引：
`idx_entity_alarm_source_time (tenant_id, alarm_source_flag, create_time DESC)` 服务"按来源 + 时间"翻页，
`idx_entity_alarm_target (tenant_id, alarm_target_type_flag, entity_id, create_time DESC)` 服务"按实体维度"翻页。两个索引都以
`tenant_id` 打头——告警数据严格按租户隔离。

## 规则、状态机与通知的实体关系

写入 `dc3_entity_alarm` 只是"记一笔"。要让告警"反复触发不轰炸、恢复能感知、能送达到人"，靠的是 `dc3_rule`（规则定义）→
`dc3_rule_state`（运行状态机）→ `dc3_notify`（通知配置）→ `dc3_notify_channel`（渠道）→ `dc3_notify_history`（送达审计）这条链路。下图是这些表与
`dc3_event_history` 的关系（这些是逻辑关联，库内通过 id 列关联，未建外键约束）：

<AlarmErDiagram lang="zh" />

### 触发状态机：pending → firing → recovered → closed

`dc3_rule_state` 是每条规则对每个实体（由 `fingerprint` 唯一标识）的运行态，`entity_state_flag` 取值受 SQL 约束
`CHECK (entity_state_flag BETWEEN 0 AND 3)`：

- `0=pending` 待触发，`1=firing` 触发中，`2=recovered` 已恢复，`3=closed` 已关闭。
- 状态翻转时记录 `first_trigger_time` / `last_trigger_time` / `last_recover_time` / `last_notify_time` 与
  `trigger_count`，并把当次告警的 `alarm_id` 回填——这让"同一异常持续触发"只累加计数、不重复刷屏，恢复也能被感知。

### 从触发到送达

规则命中后，告警写入与通知发送的次序如下（`dc3_notify_history` 的 pending 记录在事务内同步落库，随后才经 RabbitMQ
异步投递渠道并回写状态）：

<AlarmNotifyFlowDiagram lang="zh" />

`dc3_notify_channel.channel_type_flag` 受约束 `CHECK (channel_type_flag BETWEEN 0 AND 2)`，即 `0=email`、`1=SMS`、
`2=webhook`；`dc3_notify_history.status_flag` 受约束 `CHECK (status_flag BETWEEN 0 AND 4)`，覆盖
pending/sent/success/failed/retry 五态，失败可重试并累加 `retry_count`。

## 事件历史 vs 运行告警：别混为一谈

`dc3_event_history` 和 `dc3_entity_alarm` 经常被一起提及，但它们是两类不同的东西。事件历史是**设备主动上报的事件原始日志**
——设备通过 `EventReportDTO` 上报，经 `EventReportReceiver` 落入 `dc3_event_history`；运行告警是**任意来源触发后产生的统一告警记录
**。两者的关系是：事件上报**可能**触发规则评估，进而产生一条 `alarm_source_flag=5` 的运行告警，但事件历史本身仍是独立的日志。

| 维度   | `dc3_event_history`（事件历史）         | `dc3_entity_alarm`（运行告警）                      |
|------|-----------------------------------|-----------------------------------------------|
| 定义来源 | 模板里的事件定义（`dc3_event` 表）           | 任意规则/状态触发产生的告警                                |
| 发起方  | 设备通过 `EventReportDTO` 主动上报        | 规则引擎、状态超时、设备/驱动/事件上报                          |
| 落库表  | `dc3_event_history`（原始日志）         | `dc3_entity_alarm`（统一记录）                      |
| 状态跟踪 | `acknowledge_flag`（0 未确认 / 1 已确认） | `confirm_flag`（0/1）+ `dc3_rule_state` 生命周期    |
| 生命周期 | 单次创建，事后确认                         | 关联 `dc3_rule_state`，跟踪 trigger_count、首末次时间、恢复 |

`dc3_event_history` 自己也有分级字段：`event_type_flag`（`0=info`/`1=alert`/`2=fault`/`3=lifecycle`）和 `event_level_flag`（
`0=LOW`/`1=MEDIUM`/`2=HIGH`/`3=CRITICAL`），并以 `record_id`（UUID）唯一标识一次上报。

## 实操：查询与确认

查询某租户的告警走数据中心的看板接口；下面给出真实路径与请求/响应形态，示例值标注为示例。

::: code-group

```bash [curl 查询告警]
# 经网关 (8000) 查询运行告警，按 source/类型/确认态过滤
# X-Auth-* 三件套由 POST /api/v3/auth/token/generate 登录后取得
curl -X POST http://localhost:8000/api/v3/data/dashboard/alert/page \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <示例: 你的租户>' \
  -H 'X-Auth-Login: <示例: 你的登录名>' \
  -H 'X-Auth-Token: <示例: 登录返回的 token>' \
  -d '{
        "current": 1,
        "size": 20,
        "source": "device",
        "alarmTypeFlag": 0,
        "confirmFlag": 0
      }'
```

```json [响应形态（示例）]
{
  "code": "R200",
  "data": {
    "current": 1,
    "size": 20,
    "total": 3,
    "records": [
      {
        "id": "示例-雪花ID",
        "source": "device",
        "sourceId": "示例-来源实体ID",
        "pointId": "示例-位号ID",
        "alarmTypeFlag": 0,
        "confirmFlag": 0,
        "message": "示例-告警描述",
        "createTime": "2026-06-22T10:00:00"
      }
    ]
  }
}
```

:::

::: tip 三视图就是一个 source 参数
前端"驱动/设备/位号告警"三页本质是对同一接口传不同 `source`（`driver`/`device`/`point`）。你用 curl 复刻它们，只需切换这一个字段。
:::

## 约束与边界

::: warning 告警级别是 P0–P3
`alarm_level_flag` 取 `0=P0 … 3=P3`，0 为最高优先级，排序/筛选时注意方向。
:::

::: danger 告警严格按租户隔离
`dc3_entity_alarm`、`dc3_rule`、`dc3_rule_state`、`dc3_notify*` 全部带 `tenant_id`，所有索引以 `tenant_id`
打头。新增查询/缓存键时必须保留租户范围，不得跨租户读取或省略 `tenant_id`。
:::

::: info 表关联为逻辑关联，无外键约束
`rule_id`、`rule_state_id`、`alarm_id`、`notify_id`、`channel_id` 等是 id 列逻辑关联，库内未建 FK 约束。删除/清理时需在业务层保证一致性，DB
不会级联。
:::

::: info 以源码为准
本页标志位取值与约束依据 `AlarmSourceTypeEnum` / `AlarmTargetTypeEnum` 与 `03-iot-dc3-data.sql` 的 `CHECK` 约束（仓库路径
`dc3/dependencies/postgres/initdb/03-iot-dc3-data.sql`）。字段名以 DO 模型与 SQL 为准；通知触发与异步投递的具体实现以
`AlarmRuleTriggerService` 等服务类为准。
:::

## 延伸阅读

- [设备接入](./device-onboarding) — 告警的前提：先把设备接进来、有值在流转
- [数据平面](../architecture/data-plane) — 设备值如何落库，告警规则评估的数据来源
