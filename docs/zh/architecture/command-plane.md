---
title: 命令平面：读写命令的下发与回执
---

<script setup>
import CommandStateDiagram from '../../.vitepress/theme/components/CommandStateDiagram.vue'
import CommandFlowDiagram from '../../.vitepress/theme/components/CommandFlowDiagram.vue'
</script>


# 命令平面：读写命令的下发与回执

数据平面把设备的值采上来，命令平面做相反的事：把一次"读这个位号"或"给这个位号写值"的请求，从 HTTP
入口一路下发到驱动、到设备，再把执行结果回写。这页追踪一条命令从提交、校验、入库、经 RabbitMQ
下发、驱动执行到回执落库的完整链路与状态机，让你能看懂为什么提交命令是"立即拿号、轮询取结果"，以及失败时各状态分别意味着什么。

> 你在这里：已理解 [数据平面](./data-plane) 的采集方向，现在看反向的命令下发。命令的产生方可以是 Web、CLI，也可以是
> AI（见 [数据与命令](../operation/data-commands)）。

## 为什么是"异步拿号 + 轮询"

下发一条命令要跨进程、跨网络、最终落到现场设备上——这中间任何一跳都可能慢、可能失败。如果让 HTTP
请求一直阻塞等设备执行完，网关线程会被长时间占用，设备离线或协议超时还会拖垮整条调用链。

所以命令平面把"提交"和"结果"解耦：`POST /api/v3/data/point_command/read` 和 `POST /api/v3/data/point_command/write`
在数据中心做完校验、把命令以 `PENDING` 落库、发往 RabbitMQ 之后，**立即返回一个 `commandId`**（一个 36 字符的
UUID）。调用方拿这个号去轮询历史接口，就能看到命令此刻走到了哪一步、成功还是失败、设备回了什么值。

这条链路的入口在 `PointCommandController`（`dc3-common-data`），两个端点权限都是 `point_command:list`，请求体里给
`deviceId` / `pointId`（写命令再加 `value`），可选地带一个 `commandId` 让提交变成幂等——同一个 `commandId`
重复提交会直接返回已存在的那条记录，不会重复下发。

## 一条写命令的旅程

下面这张时序图是写命令的 happy path：从调用方提交，到驱动把值真正写进设备并回执成功，再到调用方轮询拿到结果。

<CommandPlaneDiagram lang="zh" />

### 提交侧：校验、落库、发布

数据中心 `PointCommandServiceImpl` 在下发前依次校验，任一步失败都直接抛异常、不入队：

- **租户范围**：`deviceId` / `pointId` 必须属于当前租户，且设备绑定的 `profileId` 与位号的 `profileId` 一致，否则按越权拒绝。
- **启用状态**：设备与位号的 `enableFlag` 必须为启用，禁用的设备或位号不接受命令。
- **可写性（仅写命令）**：位号的 `rwFlag` 必须是 `WRITE_ONLY` 或 `READ_WRITE`；写一个 `READ_ONLY` 位号会被拒绝（"Point is not
  writable"）。这呼应了[核心概念](../introduction/concepts)里"读写由 Point 自己决定"的约定。
- **驱动在线**：从 `dc3_entity_state` 查这台设备所属驱动的状态，不是 `ONLINE` 就拒绝（"Driver is offline"）。

校验通过后，命令以 `PENDING` 状态写入 `dc3_point_command_history`，随即用 `rabbitTemplate.convertAndSend(...)` 发布，并把
`CorrelationData` 设为 `commandId`——这样 RabbitMQ 的 publisher-confirm 回执就能精确对应到这一条命令。发布调用返回后，记录被更新为
`SENT`、写入 `sendTime`。

### 投递的载荷：PointCommandDTO

跨 RabbitMQ 传输的不是宽泛的 JSON 字符串，而是一个强类型的 record `PointCommandDTO`，其 `payload` 字段是 `sealed`
接口；时间字段统一用 `Instant`（UTC）：

```java
public record PointCommandDTO(
        String commandId,          // 与历史记录、回执一一对应
        Long tenantId,             // 租户隔离
        PointCommandTypeEnum type, // READ / WRITE / ...
        PointCommandPayload payload, // ReadPayload | WritePayload（多态）
        PointCommandSourceEnum source,
        Long sourceUserId,
        Instant occurredAt,
        Instant expireAt,          // 默认 occurredAt + 10s
        int schemaVersion
) { }
```

载荷 `PointCommandPayload` 是一个密封接口，只有 `ReadPayload(deviceId, pointId)` 与
`WritePayload(deviceId, pointId, value)` 两种实现，驱动侧用 `switch` 模式匹配分发，编译期即可穷尽所有分支。

::: warning expireAt 默认只有 10 秒
`PointCommandDTO` 的工厂方法 `ofRead()` / `ofWrite()` 把 `expireAt` 设为 `Instant.now().plusSeconds(10)`
。命令在队列里积压、或驱动消费时已经 `now > expireAt`，会被判定为 `EXPIRED` 而不执行——这是为采集类命令设计的短时效语义，不要把它当成可以慢慢排队的长任务。
:::

### 驱动侧：预检、去重、加锁、执行

驱动用 `PointCommandReceiver` 消费命令队列。拿到一条命令后，处理顺序是固定的：

1. **基本校验**：`commandId` / `tenantId` / `type` / `payload` 任一为空，或读/写载荷缺字段，直接 `reject`（进死信，不重投）。
2. **expireAt 预检**：`now > expireAt` → 回执 `EXPIRED`，不碰设备。
3. **去重**：用 Caffeine 去重缓存（5 分钟过期、上限 5 万条）`tryAcquire(commandId)`；命中说明这条命令已处理过，回执
   `DUPLICATE`。
4. **每设备串行锁**：通过 `DeviceLockManager` 拿这台设备的 `ReentrantLock`（引用计数管理锁的创建与回收），保证同一设备上的多条命令不会交错执行、打乱协议时序。
5. **读/写分发**：`ReadPayload` 调 `driverReadService.read(...)`；`WritePayload` 调 `driverWriteService.write(...)`。

::: danger 写失败不回显值
写命令**只有当 `driverWriteService.write()` 返回 `Boolean.TRUE` 时才算成功**，回执 `SUCCESS` 并带上刚写入的值。一旦返回
`false`，回执是 `FAILED` 且 `responseValue=null`——**绝不回显任何值**
。这是有意为之：写失败却回显一个值，会让上层误以为命令成功、设备状态已变更，造成"假成功"。看到 `FAILED`，就要当成"
这次写入没有生效"。
:::

## 命令的生命周期

一条命令的状态由 `PointCommandStatusEnum` 定义，从提交到终态的流转如下图。提交侧负责 `PENDING → SENT`
；其余终态都由驱动消费时产生的回执、经结果队列写回。

<CommandStateDiagram lang="zh" />

各状态对应的枚举索引与含义（`PointCommandStatusEnum`，括号内是落库的 `status` 值）：

| 状态          | index | 含义                    |
|-------------|-------|-----------------------|
| `PENDING`   | 0     | 已提交、待发布               |
| `SENT`      | 1     | 已发布到 broker、待驱动处理     |
| `SUCCESS`   | 2     | 驱动确认成功                |
| `FAILED`    | 3     | 驱动报告失败（写失败 / 重投后异常）   |
| `TIMEOUT`   | 4     | 应用层超时（枚举已预留，当前链路尚不产生） |
| `EXPIRED`   | 5     | 执行前 `expireAt` 已过     |
| `DEAD`      | 6     | 被 reject 进入死信，不再处理    |
| `DUPLICATE` | 7     | 被驱动去重缓存判定为重复          |

`EXPIRED` 由驱动在消费时 `now > expireAt` 判定；`DUPLICATE` 由去重缓存命中产生。

::: info TIMEOUT 当前无生产者
`PointCommandStatusEnum` 预留了 `TIMEOUT(4)`，但当前链路里没有任何代码把命令置为此状态——`SUCCESS` / `FAILED` /
`EXPIRED` / `DUPLICATE` / `DEAD` 各有明确的产生路径，唯独 `TIMEOUT` 是为将来的应用层超时语义预留的枚举位，状态机里用注记标注（而非活跃边）即此意。
:::

命令的 `type` 由 `PointCommandTypeEnum` 区分：`READ(0)` / `READ_BATCH(1)` / `WRITE(2)` / `WRITE_BATCH(3)` / `CONFIG(4)`
——当前读写端点下发的是 `READ` 与 `WRITE`。

### 错误路径：重投一次，再失败就落账

驱动执行抛异常时的处理，专门避免"毒消息"在队列里死循环：

- **首次失败（非重投）**：释放该命令的去重占用，`nack(requeue=true)` 让消息重回队列再试一次。
- **重投后仍失败**：不再重投，直接回执 `FAILED`（`errorCode=DRIVER_ERROR`）并 `ack` 掉这条消息，让它出队。

这样每条命令最多被驱动尝试两次，既给了瞬时故障一次自愈机会，又不会让一条始终失败的命令无限循环。

## 命令的 RabbitMQ 拓扑

命令链路用两组交换机/队列：一组把命令从数据中心送到对应驱动，一组把回执从驱动送回数据中心。命令队列按驱动 `serviceName`
分队，带 30 秒 TTL 与死信交换机；结果队列带 60 秒 TTL。

<CommandFlowDiagram lang="zh" />

命令队列 `dc3.q.point_command.{serviceName}` 是 durable、`ttl(30000)`，死信指向 `dc3.e.point_command_dead`
——两条路径会进死信：一是命令在驱动侧 30 秒内没被消费（TTL 到期），二是基本校验失败时驱动直接 `reject`（不重投）把消息打入
DLX；两者都不在原队列无限滞留。回执走 `dc3.e.point_command_result`（topic），结果队列 `dc3.q.point_command_result` 为
`ttl(60000)`，由数据中心的 `PointCommandResultReceiver` 消费：按 `commandId` 查到历史记录，写入终态 `status`、
`responseValue`、`errorCode` / `errorMessage` 与 `finishTime`。

## 提交与轮询：真实路由

下发一条写命令、再轮询结果，是两次独立的 HTTP 调用。所有路径都经网关（`http://localhost:8000`）转发，受保护端点需带
`X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token` 三个鉴权头。

::: code-group

```bash [提交写命令]
# 给设备 1024 的位号 2048 写值 25.5；返回 commandId（示例 UUID）
curl -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H "X-Auth-Tenant: <tenant>" \
  -H "X-Auth-Login: <login>" \
  -H "X-Auth-Token: <token>" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": 1024, "pointId": 2048, "value": "25.5"}'
# → {"code":"...","data":"9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", ...}
```

```bash [轮询结果]
# 用上一步拿到的 commandId 查历史，看 status 与 responseValue
curl "http://localhost:8000/api/v3/data/point_command_history/get_by_command_id?commandId=9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" \
  -H "X-Auth-Tenant: <tenant>" \
  -H "X-Auth-Login: <login>" \
  -H "X-Auth-Token: <token>"
```

:::

轮询返回的是 `PointCommandHistoryVO`——查询按租户隔离（`getByCommandId(tenantId, commandId)`），所以一个租户拿不到另一个租户的命令记录。
`status` 走到上面任一终态即代表本次命令结束；写命令尤其要看 `responseValue`：`SUCCESS` 时它是回显的写入值，`FAILED` 时它一定是
`null`（见上文"写失败不回显值"）。

::: info 与"自定义指令"是两套命名空间
本页讲的是**位号读写**（`point_command`）：交换机 `dc3.e.point_command`、DTO `PointCommandDTO`、表
`dc3_point_command_history`。平台另有一套**自定义指令**（Custom Command），走 `dc3.e.command`、DTO `CommandCallDTO`，用于在
Profile 上定义的设备级动作。两者结构相似但互不相通，不要把路由键、DTO 或历史表混用。
:::

## 延伸阅读

- [数据平面](./data-plane) — 反向的位号值采集链路：交换机、队列、TimescaleDB 落库
- [核心概念与心智模型](../introduction/concepts) — 位号的 `rwFlag` 为何决定可写性
- [驱动开发](../development/driver-authoring) — `DriverProtocol.write()` 返回 `Boolean` 的契约与命令处理管线
- [数据与命令](../operation/data-commands) — 从使用者视角下发命令、处理离线与只读位号
