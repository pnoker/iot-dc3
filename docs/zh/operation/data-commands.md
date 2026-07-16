---
title: 数据与命令
---

<script setup>
import DataCommandsFlowDiagram from '../../.vitepress/theme/components/DataCommandsFlowDiagram.vue'
import DataCommandsStateDiagram from '../../.vitepress/theme/components/DataCommandsStateDiagram.vue'
</script>


# 数据与命令

设备接入后，验证只剩两件事：值能不能查到、命令能不能下到。这页把"看数据"和"下命令"连成一条用户故事——先用真实 `curl`
读到位号(Point)的最新值与历史，再下一条写命令并轮询它的执行结果，最后讲清设备离线、只读位号、写失败这些边界下系统的真实行为。

> 你在这里：已[接入第一个设备](../quickstart/first-device)、值开始进库。读完这页你能独立地查值、下命令、判断命令到底成没成。

## 两条相反的链路

数据与命令是方向相反的两条链路，但在你这一侧都收敛成 HTTP 调用，统一从网关(Gateway / `dc3-gateway`，`8000`)进。

**数据流是设备 → 你**：驱动(Driver)把一次采集封装成位号值(PointValue)，经 RabbitMQ 的 `dc3.e.value` 交换机发往数据中心(
Data Center / `dc3-center-data`)，落进 TimescaleDB 的 `dc3_point_value` 超表；你通过 `/api/v3/data/point_value/latest`
读最新值、`/api/v3/data/point_value/list` 读历史区间。这是**已发生事实的查询**，无副作用、可随意重试。

**命令流是你 → 设备**：你 `POST` 一条读/写命令到数据中心，它先落 `dc3_point_command_history`（状态 `PENDING`），再经
`dc3.e.point_command` 交换机路由到目标驱动；驱动对设备执行后，结果经 `dc3.e.point_command_result` 回传。命令接口*
*立即返回一个 `commandId`**，真正的成败要靠这个 ID 去轮询。这是**异步、有副作用**的写路径。

<DataCommandsFlowDiagram lang="zh" />

两条链路的字段、模型变换与 RabbitMQ 拓扑细节，分别在 [数据平面](../architecture/data-plane)
与 [命令平面](../architecture/command-plane) 里展开；这页只讲怎么用。

## 看数据：查最新值与历史

数据中心暴露两个读接口，都是 `POST`（请求体携带分页与过滤条件），返回 `Page<PointValueVO>`，每条值含 `deviceId`、`pointId`、
`rawValue`（原始值）、`calValue`（工程值）、`numValue`（数值投影，可空），以及 `createTime`、`operateTime`。两者都按租户(`tenantId`)
隔离，权限码 `point_value:list`。受保护接口需带鉴权头 `X-Auth-Tenant`、`X-Auth-Login`、`X-Auth-Token`
（如何取见 [API 文档](../development/api-documentation)）。

`/api/v3/data/point_value/latest` 拿每个位号的当前值，`deviceId`、`pointId` 可选——只给 `deviceId` 就是这台设备所有位号的最新值。分页字段嵌在
`page` 对象里：

::: code-group

```bash [latest 最新值]
# 示例 deviceId / pointId，替换为你自己的
curl -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <租户>' -H 'X-Auth-Login: <账号>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "page": {"current": 1, "size": 20}}'
```

```bash [list 历史区间]
# 用 rangeKey (today/24h/7d/30d) 或 createTimeFrom / rangeHours 圈定时间窗
curl -X POST http://localhost:8000/api/v3/data/point_value/list \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <租户>' -H 'X-Auth-Login: <账号>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "pointId": 2001,
       "rangeKey": "24h",
       "page": {"current": 1, "size": 100}}'
```

:::

`/api/v3/data/point_value/list` 比 `latest` 多 `createTimeFrom`、`rangeHours`、`rangeKey`（`today`/`24h`/`7d`/`30d`
）等时间过滤字段，用于翻历史。响应大致是这个形态（示例值）：

```json
{
  "data": {
    "current": 1, "size": 20, "total": 1,
    "records": [
      { "deviceId": 1001, "pointId": 2001, "rawValue": "26.5", "calValue": "26.5",
        "numValue": 26.5, "createTime": "2026-06-22T11:59:58" }
    ]
  }
}
```

::: warning 聚合时 num_value 可空
底层 `dc3_point_value.num_value` 对非数值/JSON 载荷为 `NULL`。如果你绕过 API 直接对超表做 `AVG`/`SUM` 等聚合，必须加
`num_value IS NOT NULL`，否则结果偏差。位号的原始值与计算值分别在 `raw_value`、`cal_value`（均为文本）。
:::

## 下命令：读命令、写命令与轮询结果

命令接口在数据中心，权限码 `point_command:list`。它们**不返回执行结果**，只返回一个命令 ID——执行是异步的，你拿这个 ID 去轮询历史。

`POST /api/v3/data/point_command/read` 主动触发一次读（绕过采集周期，立刻向设备要值）；
`POST /api/v3/data/point_command/write` 把一个值写到可写位号。两者都接受可选的 `commandId` 用于幂等去重。

::: code-group

```bash [写命令 write]
curl -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <租户>' -H 'X-Auth-Login: <账号>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "pointId": 2001, "value": "100"}'
# 响应体 data 即为 commandId（示例）: "a1b2c3d4-...."
```

```bash [读命令 read]
curl -X POST http://localhost:8000/api/v3/data/point_command/read \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <租户>' -H 'X-Auth-Login: <账号>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "pointId": 2001}'
```

```bash [轮询结果]
# commandId 用上一步拿到的命令 ID
curl 'http://localhost:8000/api/v3/data/point_command_history/get_by_command_id?commandId=a1b2c3d4-....' \
  -H 'X-Auth-Tenant: <租户>' -H 'X-Auth-Login: <账号>' -H 'X-Auth-Token: <token>'
```

:::

轮询返回 `PointCommandHistoryVO`，关键字段 `status`（执行状态，`PointCommandStatusEnum`）、`responseValue`（结果/读回值）、
`requestValue`、`finishTime`、`expireTime`。状态从 `PENDING` 开始流转，看到 `SUCCESS` 才算成：

```json
{
  "data": {
    "commandId": "a1b2c3d4-....",
    "deviceId": 1001,
    "status": "SUCCESS",
    "responseValue": "100",
    "finishTime": "2026-06-22T12:00:01"
  }
}
```

::: tip 命令有 10 秒默认时效
底层命令 DTO 的 `expireAt` 默认是 `下发时刻 + 10s`。设备若在这之前没被触达，驱动在消费时发现 `now > expireAt` 会把命令置为
`EXPIRED`，而不是无限等待。轮询时若长时间停在 `PENDING`/`SENT`，优先怀疑驱动或设备不在线。
:::

## 命令状态机：PENDING 之后会去哪

命令一生在 `dc3_point_command_history.status` 里逐格推进。`PENDING` 是刚落库待发布；RabbitMQ 发布确认(publisher-confirm)
回来后转 `SENT`（已进队列、等驱动）；之后由驱动的执行回执决定终态。理解这张图，你就能从一个状态反推卡在哪一跳。

<DataCommandsStateDiagram lang="zh" />

终态共六种，对应 `PointCommandStatusEnum` 的 `2`–`7`：`SUCCESS(2)` 成功；`FAILED(3)` 驱动明确失败；`TIMEOUT(4)` 应用层等不到回执；
`EXPIRED(5)` 超过 `expireAt` 才被消费；`DUPLICATE(7)` 被驱动去重缓存挡掉；`DEAD(6)` 被拒入死信、不再处理。`PENDING(0)` 与
`SENT(1)` 是过程态。

::: info TIMEOUT 当前无生产者
`TIMEOUT(4)` 在 `PointCommandStatusEnum` 中已预留，但当前链路尚无代码把命令置为该状态；
`SUCCESS/FAILED/EXPIRED/DUPLICATE/DEAD` 才是实际会产生的终态。状态机图中的 `TIMEOUT` 转移按此理解。
:::

## 边界：离线、只读位号与写失败

下命令前，系统会按租户一致 → 设备/位号启用 → 写命令校验 `rwFlag` →
驱动在线的顺序校验，任一不过都不会真正派发。三种最常见的"命令没成"原因要分清：

设备/驱动**离线**时，命令仍能提交并拿到 `commandId`，但因无人消费，最终多半停在 `SENT` 直至 `EXPIRED`，或在超时后变
`TIMEOUT`。设备在线与否由 `dc3_entity_state` 的租约(lease)＋ RabbitMQ 心跳判定，不是你手填的。

位号的读写能力由其 `rwFlag` 决定，取值 `READ_ONLY`、`WRITE_ONLY`、`READ_WRITE`。对只读位号下写命令，会在校验阶段被直接拒绝。

::: danger 只读位号写会被拒，写失败不回显值
对 `rwFlag=READ_ONLY` 的位号调用 `/point_command/write` 会被拒绝——这是设计约束，不是临时校验。
此外，写命令**只有**在驱动的 `write()` 明确返回成功时才记 `SUCCESS`；一旦失败，结果状态为 `FAILED` 且
`responseValue=null`——**失败的写不会回显任何值**，以杜绝"看起来成功了"的假象。轮询到 `FAILED` 时不要用 `responseValue`
当作已写入的值。
:::

::: info 自定义命令是另一套
本页讲的是位号级读写命令（`dc3.e.point_command` / `PointCommandDTO`）。设备级"自定义命令"走的是独立的 `dc3.e.command` /
`CommandCallDTO` 命名空间，不要把两者的交换机或 DTO 混用。
:::

## 排查清单

按现象快速定位。表格只作速查，根因解释见上文与两条平面文档。

| 现象                             | 优先排查                                                                 |
|--------------------------------|----------------------------------------------------------------------|
| 有设备但 `/point_value/latest` 返回空 | 租户/`deviceId`/`pointId` 是否对、采集周期、驱动协议日志、RabbitMQ 是否积压                |
| 历史值有缺口或延迟                      | 批处理阈值(`POINT_BATCH_SPEED`/`POINT_BATCH_INTERVAL`)、RabbitMQ 积压、数据中心日志 |
| 命令长期停在 `PENDING`/`SENT`        | 目标驱动是否在线、是否监听了 `dc3.q.point_command.{serviceName}`                   |
| 命令变 `EXPIRED`                  | 设备/驱动离线或响应慢，10 秒 `expireAt` 已过                                       |
| 写命令返回 `FAILED`                 | 位号 `rwFlag` 是否允许写、写入值类型/范围、协议返回码                                     |

## 延伸阅读

- [数据平面](../architecture/data-plane) — 一条值从设备到超表的每一跳、RabbitMQ 拓扑与模型变换
- [命令平面](../architecture/command-plane) — 命令生命周期状态机、队列 TTL/DLX 与结果回执通道
- [第一个设备](../quickstart/first-device) — 还没有可查的设备？先走完这条黄金路径
- [API 文档](../development/api-documentation) — 鉴权头怎么取、OpenAPI/Swagger 在哪
