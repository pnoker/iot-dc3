---
title: 事件 Event
---

<script setup>
import EventRelationDiagram from '../../../.vitepress/theme/components/EventRelationDiagram.vue'
import EventFlowDiagram from '../../../.vitepress/theme/components/EventFlowDiagram.vue'
</script>

# 事件 Event

> **事件是设备主动上报的一次业务发生**——故障、告警、模式切换、生命周期变化……定义归属[模板](./profile)
> ，实例归属[设备](./device)，上报后既留原始流水，又可触发告警。

事件回答的是"设备身上发生了什么"，而不是"某个量现在是多少"。后者是[位号值](./point-value)
（周期采集的数值快照），前者是离散的、带语义的一次发生：一台门禁设备的"温度 = 25.3℃"是位号值，"门被强行打开"是一个事件。

事件分两层：**定义**（这类设备会上报哪些事件、每个事件带哪些参数）沉淀在模板里，由 `dc3_event` / `dc3_event_param` 承载；*
*实例**（某台设备某时刻真的报了一次）由驱动上报、落到 `dc3_event_history`。

## 关键字段

事件定义 `EventBO`（表 `dc3_event`）：

| 字段               | 类型                | 含义                                 |
|------------------|-------------------|------------------------------------|
| `eventName`      | String            | 事件名称（展示用）                          |
| `eventCode`      | String            | 事件标识符，上报与告警规则按它匹配（如 `DOOR_FORCED`） |
| `eventTypeFlag`  | EventTypeFlagEnum | 事件类型，见下                            |
| `eventLevelFlag` | EventLevelEnum    | 事件级别，见下                            |
| `profileId`      | Long              | 归属的[模板](./profile)                |
| `eventExt`       | JSON              | 扩展配置                               |

事件参数 `EventParamBO`（表 `dc3_event_param`，声明事件携带哪些输出参数）：

| 字段                        | 类型            | 含义        |
|---------------------------|---------------|-----------|
| `paramName` / `paramCode` | String        | 参数名 / 标识符 |
| `paramTypeFlag`           | PointTypeEnum | 参数数据类型    |
| `eventId`                 | Long          | 归属的事件定义   |

::: tip 事件参数复用位号的类型系统
`paramTypeFlag` 用的是和位号一样的 `PointTypeEnum`（`STRING` / `INT` / `FLOAT` / `BOOLEAN`
…），事件参数的取值范围与[位号](./point)数据类型完全一致。
:::

## 事件类型与级别

| 类型 `eventTypeFlag` | 说明     |
|--------------------|--------|
| `info`             | 信息事件   |
| `alert`            | 告警事件   |
| `fault`            | 故障事件   |
| `lifecycle`        | 生命周期事件 |

| 级别 `eventLevelFlag` | `0` LOW | `1` MEDIUM | `2` HIGH | `3` CRITICAL |
|---------------------|---------|------------|----------|--------------|

## 与其它概念的关系

<EventRelationDiagram lang="zh" />

- 事件**定义**挂在模板下，与[位号](./point)、[指令](./command)并列，共同描述"这类设备有什么能力"。
- 事件**实例**由[设备](./device)通过[驱动](./driver)上报，带上 `eventCode`、级别和一组参数值。

## 上报链路与生命周期

<EventFlowDiagram lang="zh" />

一次上报（`EventReportDTO`）携带：`recordId`（UUID）、`deviceId`、`eventId`、`eventCode`、`eventTypeFlag`、`eventLevelFlag`、
`paramValues`、`message`、`occurTime`。数据中心先把它落成**原始流水** `dc3_event_history`，再提交给告警规则引擎；命中规则才会在
`dc3_entity_alarm` 生成/更新一条**运行态告警**。

::: warning EventHistory 不等于告警
`dc3_event_history` 是"设备说发生了什么"的原始流水，**每次上报都记**；`dc3_entity_alarm` 是"告警引擎判定需要关注"的结果，*
*只有命中规则才有**。查"设备报过哪些事件"看前者，查"当前有哪些告警"看后者，别混为一谈。
:::

## 示例

门禁设备的模板里定义一个事件：`eventCode = DOOR_FORCED`、`eventTypeFlag = alert`、`eventLevelFlag = 3`，带参数
`openMethod`（String）。现场设备被撬开时，驱动上报
`EventReportDTO{ eventCode: "DOOR_FORCED", paramValues: { openMethod: "pry" }, occurTime: ... }`；数据中心记入
`dc3_event_history`，并因级别为 CRITICAL 命中告警规则，在 `dc3_entity_alarm` 生成告警。

## 上报 API

| 方法   | 路径                                               | 说明                    |
|------|--------------------------------------------------|-----------------------|
| POST | `/data/event_history/report`                     | 上报事件                  |
| GET  | `/data/event_history/get_by_record_id?recordId=` | 按 `recordId` 查询事件记录详情 |
| POST | `/data/event_history/list`                       | 分页查询事件记录              |

## 延伸阅读

- [模板 Profile](./profile) — 事件定义挂在模板下
- [指令 Command](./command) — 下行的对偶：事件上行、指令下行
- [位号值 PointValue](./point-value) — 互补：连续数值 vs 离散发生
- [告警与通知](../../operation/alarms) — 事件如何变成告警
- [数据平面](../../architecture/data-plane) — 上行链路的交换机 / 队列 / 可靠性细节
