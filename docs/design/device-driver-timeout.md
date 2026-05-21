---
title: 设备与驱动状态超时管理说明
---

# 设备与驱动状态超时管理说明

本文按当前代码实现梳理 DC3 驱动状态、设备状态的续租、过期、查询和告警链路。

## 当前结论

- 状态事实源是 `dc3_entity_state` 持久化状态租约表，不再是进程内 Caffeine 缓存。
- 心跳到达后双写 `dc3_entity_state` 和本地缓存：数据库为权威数据源，本地缓存作为可选加速层。
- `EntityStateExpiryScanner` 每 15 秒扫描 `expire_time` 已过期的行，通过 `lease_version` 二次检查避免竞态。
- 查询链路（Controller、gRPC、Local Facade、SystemHealth）均从 `dc3_entity_state` 读取状态。
- 过期时扫描器写入 `dc3_entity_alarm` 的离线超时告警，并触发规则链路。
- 重启后数据库中状态不丢失，查询结果一致；多实例共享同一数据库，解决状态分裂问题。

## 状态租约表

`dc3_entity_state` 存储每个驱动和设备的当前状态租约：

| 字段 | 说明 |
| --- | --- |
| `entity_type_flag` | 实体类型（DRIVER=3, DEVICE=6），沿用 `EntityTypeFlagEnum` |
| `entity_id` | 驱动 ID 或设备 ID |
| `driver_id` | 冗余字段：驱动条目等于 entity_id，设备条目为所属驱动 |
| `state_flag` | 当前状态码索引（DriverStatusEnum / DeviceStatusEnum） |
| `lease_version` | 单调递增版本号，每次心跳 +1，扫描器用于二次检查 |
| `expire_time` | 租约绝对到期时间 |
| `ttl_seconds` | 本次心跳使用的 TTL 秒数 |
| `tenant_id` | 租户 ID |

每个实体（`entity_type_flag + entity_id`）在表中最多一行（唯一索引保证）。

DDL 详见 [entity-state.md](entity-state.md)。

## 消息资源

状态事件使用 `dc3.e.state` topic exchange 和两个 Data Center 侧队列：

| 消息 | Exchange | Routing key | Queue | 消费者 |
| --- | --- | --- | --- | --- |
| 驱动状态 | `dc3.e.state` | `dc3.r.state.driver.<service>` | `dc3.q.state.driver` | `DriverStateReceiver` |
| 设备状态 | `dc3.e.state` | `dc3.r.state.device.<service>` | `dc3.q.state.device` | `DeviceStateReceiver` |

`DataTopicConfig` 为两个状态队列配置了 30 秒 queue TTL（仅控制消息积压，不是在线状态 TTL）。

## 驱动状态流程

驱动 SDK 每 15 秒上报一次驱动状态：

1. `DriverStatusScheduleJob` 从 `DriverMetadata` 读取当前驱动信息，构造 `DriverStateDTO`。
2. `DriverSenderServiceImpl` 发布消息到 `dc3.e.state`。
3. `DriverStateReceiver` 调用 `DriverStateService.heartbeat`。
4. `DriverStateServiceImpl` 双写：
   - 查询 `dc3_entity_state` 现有行，不存在则新建，存在则 `lease_version + 1`。
   - 更新 `state_flag`、`expire_time`（now + 45 秒）、`ttl_seconds`、`tenant_id`。
   - `saveOrUpdate` 写入数据库。
   - 写入本地 Caffeine 缓存。
5. 如果新旧状态跨越在线族边界，写入驱动上报告警并触发规则链路。

## 设备状态流程

设备状态由具体驱动主动上报：

| 方法 | TTL 语义 |
| --- | --- |
| `deviceStatusSender(deviceId, status)` | SDK 默认 15 分钟 |
| `deviceStatusSender(deviceId, status, timeOut, timeUnit)` | 调用方传入 TTL |

Data Center 侧流程与驱动类似：

1. `DeviceStateReceiver` 调用 `DeviceStateService.heartbeat`。
2. `DeviceStateServiceImpl` 双写数据库和本地缓存，TTL 来自 `DeviceStateDTO.timeOut/timeUnit`。

## 过期与离线告警

`EntityStateExpiryScanner` 每 15 秒执行一次扫描：

1. 查询 `dc3_entity_state` 中 `expire_time < now()` 的行。
2. 对每行重新读取并检查 `lease_version`：如果心跳已续租（版本号变化），跳过。
3. 如果最后状态已经是 `offline`，更新 `expire_time` 为未来 5 分钟并跳过。
4. 写入 `dc3_entity_alarm`：
   - `alarm_type_flag = OFFLINE`
   - `alarm_source_flag = STATE_TIMEOUT`
   - `alarm_level_flag = P1`
5. 更新 `state_flag` 为 `offline`，`expire_time` 推进 5 分钟避免重复扫描。
6. 调用 `AlarmRuleTriggerService` 进入规则匹配和通知链路。

`OfflineExpiryListener` 保留作为 Caffeine 缓存级别的补充：当缓存 TTL 自然到期时，它检查数据库中状态是否已被扫描器标记为 offline，如果是则跳过，避免重复告警。

## 状态查询

所有状态查询从 `dc3_entity_state` 读取：

| 查询入口 | 实现 |
| --- | --- |
| `DeviceStatusController` | 按 Manager facade 查设备列表，按 `entity_id` 读 `dc3_entity_state` |
| `DriverStatusController` | 同上，按驱动 ID 读 |
| `StatusHealthServer` | gRPC 查询中直接读 `dc3_entity_state` |
| `StatusHealthLocalFacade` | 本地 facade 读 `dc3_entity_state` |
| `SystemHealthServiceImpl` | 遍历实体列表，从 `dc3_entity_state` 读取并统计在线数量 |

查询规则：
- 行不存在或 `expire_time < now()`：返回 `offline`。
- 行存在且未过期：根据 `state_flag` 映射为枚举 code。
- 在线统计只计 `online`，`maintain` 不计入在线数。

缓存 key 仍使用 `driver_status:<driverId>` / `device_status:<deviceId>` 格式，但本地缓存不再是查询的权威数据源。

## 告警来源语义

| 场景 | 写入入口 | `alarm_type_flag` | `alarm_source_flag` |
| --- | --- | --- | --- |
| 心跳状态翻转 | `DriverStateServiceImpl` / `DeviceStateServiceImpl` | `REPORT` | `DRIVER_REPORT` / `DEVICE_REPORT` |
| TTL 到期离线 | `EntityStateExpiryScanner` | `OFFLINE` | `STATE_TIMEOUT` |

## 已解决的问题

- **重启丢状态**：`dc3_entity_state` 持久化在数据库中，Data Center 重启后状态不丢失。
- **多实例不一致**：所有实例读取同一数据库，状态查询结果一致。
- **二次检查**：`lease_version` 单调递增，扫描器通过版本号比对避免旧扫描覆盖新心跳。
- **过期精度**：数据库 `expire_time` 是精确的绝对时间，扫描器每 15 秒检查一次。

## 剩余限制

- **扫描延迟**：扫描器 15 秒间隔，状态过期最多延迟约 15 秒才会被检测到。对于需要亚秒级离线检测的场景，可考虑缩短间隔或引入 RabbitMQ DLX 延迟检查。
- **非 TTL 淘汰不告警**：显式 invalidate 或容量淘汰不产生告警（与之前一致）。
- **心跳双写开销**：每次心跳写一次数据库。对于高频率心跳场景（如 15 秒一次驱动心跳），需要评估数据库写入压力。
