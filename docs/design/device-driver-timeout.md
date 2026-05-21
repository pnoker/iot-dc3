---
title: 设备与驱动状态超时管理说明
---

# 设备与驱动状态超时管理说明

本文按当前代码实现梳理 DC3 驱动状态、设备状态的续租、过期、查询和告警链路。旧版文档曾把
`dc3_entity_state` 状态租约表、RabbitMQ TTL + DLX 延迟检查写成目标方案；这些对象当前没有落地到代码和
初始化 SQL 中，因此不能再作为现状描述。

## 当前结论

- 当前状态事实源是 Data Center 进程内的 `LocalCacheService`，底层是 Caffeine 可变 TTL 缓存。
- RabbitMQ 只负责投递驱动/设备状态事件，不负责状态过期检查。
- 当前没有 `dc3_entity_state`、`DriverTimeoutCheckDTO`、状态超时延迟队列、DLX 检查队列、`lease_version`
  或数据库扫描器。
- 驱动状态由 SDK 定时上报，Data Center 固定写入 45 秒 TTL。
- 设备状态由驱动上报，TTL 来自 `DeviceStateDTO.timeOut/timeUnit`；SDK 默认值是 15 分钟，示例驱动常用
  25 秒。
- TTL 自然过期后，`OfflineExpiryListener` 写入 `dc3_entity_alarm` 的离线超时告警，并触发规则链路。
- 状态查询仍然读取本地缓存；缓存缺失时返回 `offline`。
- Data Center 重启丢状态、多实例状态分裂的问题仍然存在，尚未由持久化租约机制解决。

## 当前消息资源

状态事件使用现有 `dc3.e.state` topic exchange 和两个 Data Center 侧队列：

| 消息 | Exchange | Routing key | Queue | 消费者 |
| --- | --- | --- | --- | --- |
| 驱动状态 | `dc3.e.state` | `dc3.r.state.driver.<service>` | `dc3.q.state.driver` | `DriverStateReceiver` |
| 设备状态 | `dc3.e.state` | `dc3.r.state.device.<service>` | `dc3.q.state.device` | `DeviceStateReceiver` |

`DataTopicConfig` 为两个状态队列配置了 30 秒 queue TTL。这个 TTL 只表示状态消息在队列积压超过 30 秒后会被
丢弃，避免陈旧心跳长时间滞留；它不是在线状态 TTL，也不是离线判定机制。当前状态队列没有配置状态超时
DLX。

告警事件使用 `dc3.e.alarm`，其中驱动/设备主动上报告警分别进入 `dc3.q.alarm.driver` 和
`dc3.q.alarm.device`。状态过期生成的离线告警不走 RabbitMQ 告警入口，而是在 Data Center 的
`OfflineExpiryListener` 内直接写 `dc3_entity_alarm`。

## 驱动状态流程

驱动 SDK 启动调度时，`DriverScheduleServiceImpl` 固定注册 `DriverStatusScheduleJob`，cron 为：

```text
0/15 * * * * ?
```

也就是每 15 秒上报一次驱动状态。流程如下：

1. `DriverStatusScheduleJob` 从 `DriverMetadata` 读取当前驱动 ID、租户 ID 和状态。
2. 构造 `DriverStateDTO`，其中只包含 `tenantId`、`driverId`、`status` 和 `createTime`，不携带超时配置。
3. `DriverSenderServiceImpl.driverStateSender` 发布消息到 `dc3.e.state`。
4. `DriverStateReceiver` 校验 `driverId` 和 `status` 后调用 `DriverStateService.heartbeat`。
5. `DriverStateServiceImpl` 把 `driver_status:<driverId>` 写入 `LocalCacheService`，TTL 固定为 45 秒。
6. 如果缓存里已有上一状态，且新旧状态跨越在线族边界，则写入驱动上报告警并触发规则链路。

驱动在线族当前定义为 `online` 和 `maintain`。从 `online/maintain` 切到 `offline/fault`，或反向切换，都会被
视为状态翻转。第一次心跳没有上一状态，不生成翻转告警。

## 设备状态流程

设备状态由具体驱动根据协议行为主动上报。SDK 暴露两个入口：

| 方法 | TTL 语义 |
| --- | --- |
| `deviceStatusSender(deviceId, status)` | 使用 SDK 默认 TTL：15 分钟 |
| `deviceStatusSender(deviceId, status, timeOut, timeUnit)` | 使用调用方传入 TTL |

多数示例驱动在自定义调度中调用第二个方法，定期把设备置为 `online`，TTL 为 25 秒。实际生产驱动应该按协议
语义决定何时上报：例如轮询成功、收到设备心跳、连接状态恢复、协议会话变化等。

Data Center 侧流程如下：

1. `DriverSenderServiceImpl` 构造 `DeviceStateDTO`，补齐当前驱动的 `driverId` 和 `tenantId`。
2. `DeviceStateReceiver` 校验 `deviceId` 和 `status` 后调用 `DeviceStateService.heartbeat`。
3. `DeviceStateServiceImpl` 把 `device_status:<deviceId>` 写入 `LocalCacheService`，TTL 使用 DTO 中的
   `timeOut/timeUnit`。
4. 如果缓存里已有上一状态，且新旧状态跨越在线族边界，则写入设备上报告警并触发规则链路。

设备在线族同样是 `online` 和 `maintain`。需要注意：`fault` 不是在线族，但缓存仍会按 DTO 的 TTL 续租；如果
后续不再上报，TTL 到期后仍会进入离线超时处理。

## 过期与离线告警

`LocalCacheService` 只在 Caffeine 的 `RemovalCause.EXPIRED` 上触发过期监听。显式失效、容量淘汰等原因不会
触发离线告警。

`OfflineExpiryListener` 在启动时注册监听器。状态 key 过期时，它会异步处理：

1. 根据 key 前缀区分 `driver_status:` 或 `device_status:`。
2. 如果最后状态已经是 `offline`，直接忽略。
3. 通过 Manager facade 回查驱动或设备，补齐租户 ID；设备还会补齐驱动 ID。
4. 无法解析实体或有效租户时丢弃本次告警，并记录日志。
5. 写入 `dc3_entity_alarm`：
   - `alarm_type_flag = OFFLINE`
   - `alarm_source_flag = STATE_TIMEOUT`
   - `alarm_level_flag = P1`
   - `alarm_ext.type = driver-offline` 或 `device-offline`
   - `confirm_flag = 0`
   - `expired_time = 0`
6. 调用 `AlarmRuleTriggerService` 继续进入规则匹配和通知链路。

过期处理不会把本地缓存重新写成 `offline`。缓存项已经不存在，后续查询按"缓存缺失即 offline"处理。

## 状态查询

当前所有状态查询都以本地缓存为准：

| 查询入口 | 当前实现 |
| --- | --- |
| `DeviceStatusController` | 先按租户从 Manager 查询设备，再按设备 ID 读 `device_status:<id>` |
| `DriverStatusController` | 先按租户从 Manager 查询驱动，再按驱动 ID 读 `driver_status:<id>` |
| `StatusHealthServer` | gRPC 查询中使用 Manager facade 做租户范围过滤，再读本地状态缓存 |
| `StatusHealthLocalFacade` | 单体/本地 facade 中同样读本地状态缓存 |
| `SystemHealthServiceImpl` | 驱动、设备总览按本地缓存统计在线数量 |

缓存缺失统一返回 `offline`。当前没有 `unknown` 状态。

在线数量统计只把状态码等于 `online` 的实体计为在线；`maintain` 可以避免在线族翻转告警，但不会被在线数量
统计为 online。

状态缓存 key 只有实体 ID，没有租户 ID：

```text
driver_status:<driverId>
device_status:<deviceId>
```

因此查询入口必须先按租户查出实体列表，再按实体 ID 读取状态。不要新增绕过租户实体过滤、直接按任意 ID
读缓存的接口。

## 告警来源语义

当前有两类与状态有关的告警：

| 场景 | 写入入口 | `alarm_type_flag` | `alarm_source_flag` |
| --- | --- | --- | --- |
| 心跳状态翻转 | `DriverStateServiceImpl` / `DeviceStateServiceImpl` 调用告警服务 | `REPORT` | `DRIVER_REPORT` / `DEVICE_REPORT` |
| TTL 到期离线 | `OfflineExpiryListener` 直接写实体告警 | `OFFLINE` | `STATE_TIMEOUT` |

也就是说，"驱动主动上报 fault/offline"和"心跳停止导致 TTL 到期"在实体告警表中是两条不同语义。规则配置和
看板聚合时应按 `alarm_type_flag`、`alarm_source_flag`、`alarm_ext.type` 区分。

## 当前限制

- **重启丢状态**：Data Center 重启后本地缓存为空，所有状态查询都会临时返回 `offline`，直到新的心跳到达。
- **多实例不一致**：状态队列是竞争消费，一个 Data Center 实例收到心跳不会同步到其他实例；不同实例查询结果
  可能不同。
- **没有二次检查**：Caffeine TTL 到期即触发离线告警，没有 `lease_version`、`expire_time` 或数据库行锁来过滤旧
  检查消息。
- **过期时间不精确**：Caffeine 过期监听不承诺毫秒级准时触发，通常由缓存读写和维护周期推动。
- **非 TTL 淘汰不告警**：容量淘汰、显式 invalidate 不产生离线超时告警。
- **告警依赖元数据回查**：过期时如果 Manager 无法返回实体或租户，离线告警会被丢弃。

## 后续演进建议

如果要真正解决重启恢复、多实例一致性和状态审计问题，才需要引入持久化状态租约。建议分阶段落地，避免把
未实现设计误写成当前事实：

1. 新增 `dc3_entity_state` 表、DO/Mapper/Manager，并明确租户维度唯一约束。
2. 在 `DriverStateServiceImpl` 和 `DeviceStateServiceImpl` 中先双写状态表与本地缓存，验证查询语义。
3. 将 Controller、gRPC server、local facade、`SystemHealthServiceImpl` 切到状态表读取。
4. 用数据库 `expire_time` 扫描或 RabbitMQ 到期检查替代 `OfflineExpiryListener`。
5. 引入 `lease_version` 或 `last_heartbeat_time` 二次检查，避免旧检查覆盖新心跳。
6. 确认多实例并发策略，例如 `FOR UPDATE SKIP LOCKED`、幂等更新和唯一约束冲突处理。
7. 删除状态链路对 `LocalCacheService` 的依赖，只保留点位最新值等适合本地缓存的场景。

在这些步骤完成前，文档和代码评审都应把当前实现称为"本地 TTL 缓存方案"，而不是"状态租约表方案"。
