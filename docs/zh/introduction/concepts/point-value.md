---
title: 位号值 PointValue
---

<script setup>
import PointValueRelationDiagram from '../../../.vitepress/theme/components/PointValueRelationDiagram.vue'
import PointValueFlowDiagram from '../../../.vitepress/theme/components/PointValueFlowDiagram.vue'
</script>

# 位号值 PointValue

> **位号值是某个[位号](./point)在某一时刻的一次取值快照**——"3 号水泵的出水温度在 14:05:03 这一瞬间是 25.3℃"。它归属
`device + point` 并带时间戳，由[驱动](./driver)采集上行，落入 TimescaleDB 时序库。

位号值回答的是"某个量**现在/那一刻**是多少"。[位号](./point)是模板里的"列定义"（这类设备有"出水温度"
这个测点），位号值则是这一列在一行行时间上的"取数"——同一个位号会随时间产生成千上万条位号值。

它和[事件](./event)是一对互补的上行数据：位号值是**连续数值**的周期采样（温度每秒一条），事件是**离散发生**的一次业务动作（"
温度过高告警"触发了一次）。门禁设备的"温度 = 25.3℃"是位号值，"门被强行打开"是事件。

不要把位号值和"位号当前值"混为一谈：位号值是一条条**历史流水**，每次采集都新增一条、只追加不更新；"当前值"只是按
`device_id + point_id` 取最新一条位号值的查询结果。

## rawValue 与 calValue

一条位号值同时保留两个值：

- **`rawValue` 原始值**——驱动从设备原样读到的数据，未经任何换算。例如 4-20mA 变送器回传的寄存器原码 `6400`。
- **`calValue` 换算后工程值**——按位号上配置的换算规则（`baseValue` / `multiple` 等）算出的、人能直接读的工程量。例如 `6400`
  经换算得到 `25.3`（℃）。

保留原始值的意义在于可回溯、可重算：换算规则改了，历史 `rawValue` 还在，能重新算出新的工程值。

## 关键字段

位号值 `PointValueBO`（表 `dc3_point_value`）：

| 字段               | 类型            | 含义                                                       |
|------------------|---------------|----------------------------------------------------------|
| `deviceId`       | Long          | 归属[设备](./device)                                         |
| `pointId`        | Long          | 归属[位号](./point)                                          |
| `rawValue`       | String        | 原始值，设备原样回传，未换算                                           |
| `calValue`       | String        | 换算后的工程值，人可读                                              |
| `numValue`       | Double        | `calValue` 的数值投影；能干净解析为 double 时填充，布尔 / JSON / 文本则为 NULL |
| `hasLatestValue` | Boolean       | 取最新值查询时，是否真取到了采样值                                        |
| `driverId`       | Long          | 采集该数据的[驱动](./driver)                                     |
| `tenantId`       | Long          | 归属[租户](./tenant)                                         |
| `createTime`     | LocalDateTime | 采集 / 写入时间，即该快照的时间戳                                       |
| `operateTime`    | LocalDateTime | 最近操作时间                                                   |

::: tip 为什么有 numValue
`rawValue` 和 `calValue` 都是 `String`（要同时容纳数值、布尔、JSON、文本）。`numValue` 是 `calValue` 能解析成数字时的副本，专供
AVG / MIN / MAX / SUM、时序聚合等查询走数值索引，省掉每次现场 cast。非数值位号（开关量、字符串、JSON）的 `numValue` 为
NULL，聚合查询用 `num_value IS NOT NULL` 直接跳过它们。
:::

## 与其它概念的关系

<PointValueRelationDiagram lang="zh" />

- 位号值由 `deviceId + pointId` 共同定位：**哪台设备**的**哪个测点**。
- [位号](./point)给出列定义（类型、单位、换算规则），位号值是这列的一行行运行态取数。
- [事件](./event)与位号值并列于上行链路，一个连续、一个离散，互为补充。

## 采集与上行链路

<PointValueFlowDiagram lang="zh" />

驱动从设备读到 `rawValue`，按位号换算规则算出 `calValue`，能解析为数字时再填 `numValue`，连同 `deviceId` / `pointId` /
`driverId` / `tenantId` / `createTime` 打包上行；数据中心把它**追加**写入 `dc3_point_value` 超表（hypertable，按
`create_time` 1 天分片 + 按 `device_id` 哈希分片，压缩与 180 天保留策略由 TimescaleDB 自动维护）。

::: warning 位号值只增不改，注意保留策略
`dc3_point_value` 是append-only 的历史流水：每次采集都新增一行，不做 UPDATE。查"当前值"是取最新一条，不是某个会被覆盖的字段。另外它配置了
180 天保留策略——超期数据会被自动清理，需要长期留存请提前归档。
:::

## 示例

3 号水泵（`deviceId=1024`）的出水温度位号（`pointId=2048`），位号上配了换算规则把 4-20mA 原码映射到 0-100℃。14:05:03 驱动（
`driverId=8`）读到寄存器原码 `6400`，换算得 `25.3`：

```text
PointValueBO{
  deviceId: 1024, pointId: 2048, driverId: 8, tenantId: 1,
  rawValue: "6400",      // 设备原样回传
  calValue: "25.3",      // 换算后工程值（℃）
  numValue: 25.3,        // 可数值聚合
  createTime: 2026-06-24T14:05:03
}
```

一秒后又采到一条 `25.4`……如此持续累积成时序流水。查"当前出水温度"= 取 `device_id=1024, point_id=2048` 的最新一条；查"
今天均温"= 对 `num_value` 在时间窗内做 AVG。

## 查询 API

| 方法   | 路径                                                    | 说明                                            |
|------|-------------------------------------------------------|-----------------------------------------------|
| POST | `/point_value/latest`                                 | 分页查询各位号的最新值                                   |
| POST | `/point_value/list`                                   | 分页查询位号值历史                                     |
| GET  | `/point_value/list_history_by_device_id_and_point_id` | 按 `device_id + point_id` 查历史值（`count` 默认 100） |

## 延伸阅读

- [位号 Point](./point) — 位号值是位号的运行态取数
- [事件 Event](./event) — 互补：连续数值 vs 离散发生
- [核心概念总览](../concepts) — 回到概念地图
- [数据平面](../../architecture/data-plane) — 上行采集链路的交换机 / 队列 / TimescaleDB 细节
