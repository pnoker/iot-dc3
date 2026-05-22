---
title: 位号命令链路重构方案
---

# 位号命令链路重构方案

本文是 DC3 位号读写下行链路（HTTP/gRPC → 数据中心 → RabbitMQ → 驱动 → 设备）的重构与优化设计。
目标是同时解决两件事：

- **命名收敛**：把现有 `PointValueCommand` / `DeviceCommand` 两套并行词以及 `DriverCommand`
  这组死代码统一为 `PointCommand`，与物模型规划中的设备服务 `Command` 通过命名前缀清晰分层。
- **链路质量**：当前下行链路在 *唯一性 / 准确性 / 结果反馈 / 报文序列化* 四个维度都存在缺陷，
  无法支撑写控制类位号和工艺设定值这类对正确性敏感的场景。

本方案不保留历史命名与历史报文兼容，**一步切换**。

---

## 1. 背景

### 1.1 现有 `Command` 类族的混乱

代码里同时存在三组类，表达力割裂、且其中一组完全没有调用方：

| 组别                                                              | 模块                                     | 角色                                              |
|-----------------------------------------------------------------|----------------------------------------|-------------------------------------------------|
| `PointValueCommand*`                                            | dc3-common-data / dc3-common-facade    | 中心侧 HTTP / gRPC / facade 入口；强调"操作位号值"           |
| `DeviceCommand*`                                                | dc3-common-model / dc3-common-driver   | RabbitMQ 报文 + driver 端消费；强调"投递设备"               |
| `DriverCommand*` (`DriverCommandDTO` / `DriverCommandTypeEnum`) | dc3-common-model / dc3-common-constant | **死代码**，无任何调用方；曾计划做"驱动配置下发"，被 metadata 通道替代后未清理 |

中间 RabbitMQ 资源（`dc3.e.command` / `dc3.q.command.device.<svc>` / `dc3.r.command.device.<svc>`）
又用了纯中性的 `command` 词，使得同一条链路在三种命名间来回跳，新读者必须建一张映射表才能跟踪。

### 1.2 与物模型 `Command`（设备服务）的语义边界

参见 [物模型设计方案](thing-model.md)。物模型规划中 `Command` 表示**读写 Point 之外的动作型服务**
（重启、校准、切换模式等），定义在 `Profile` 下、由 `Device` 调用，是物模型三元
（Property / Service / Event）中"服务"维度的承载。

本方案要重构的位号读写**不是物模型层概念**，而是属性维度的运行态访问能力。两者通过命名前缀分层：

| 概念          | 命名前缀           | 队列 / 表 前缀                                   |
|-------------|----------------|---------------------------------------------|
| 位号读写命令（本方案） | `PointCommand` | `dc3.e.point_command` / `dc3_point_command` |
| 物模型设备服务（未来） | `Command`      | `dc3.e.command` / `dc3_command`             |

> 之前曾考虑过 `PointInstruction` 命名以彻底避开 `Command` 词，但与现有口径距离过远；
> 加 `Point` 前缀后语义已能与物模型 `Command` 充分区隔，并保留了"指令 = command"的直觉，最终选定 `PointCommand`。

### 1.3 当前下行链路四大问题

下面是对现有链路（实现见
`dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/biz/impl/PointValueCommandServiceImpl.java:64-96`、
`dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/receiver/rabbit/DeviceCommandReceiver.java:62-108`、
`dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/service/impl/DriverWriteServiceImpl.java:69-107`
）的归纳：

| 维度   | 现状                                                                                           | 风险                              |
|------|----------------------------------------------------------------------------------------------|---------------------------------|
| 唯一性  | 报文无 ID；driver 端无幂等去重；nack/requeue 重投会让设备被写第二次；同 device 多线程消费                                 | 设定点被复制 / PLC 协议交错 / 重复事件        |
| 准确性  | 不校验 `enableFlag`、`rwFlag`、driver 在线、值范围；离线 driver 命令 30s 后被 broker 静默丢弃                      | 越权写、误操作、调用方收到假成功                |
| 结果反馈 | HTTP 立即 `R.ok()`；driver SDK 忽略 `write()` 返回值并回流"假位号值"；命令第二次失败被 `reject(requeue=false)` 静默丢弃  | 平台显示成功但设备未生效；故障无审计              |
| 序列化  | DTO 字段 `content` 是被二次 JSON 序列化的 `String`；缺 tenantId / commandId / 来源 / 超时；时间用裸 LocalDateTime | 报文体积膨胀、跨时区漂移、协议演进不兼容、调试与审计无法做下去 |

具体证据与行号清单见 §11 引用。

---

## 2. 命名收敛

### 2.1 设计原则

- **位号语义优先**：所有"读写位号"相关产物统一以 `PointCommand` 作为词根；
- **彻底切换**：不保留 `PointValueCommand` / `DeviceCommand` 任何旧命名、旧 exchange、旧 queue、旧 URL，
  发版即旧物全删；
- **一刀切清理**：`DriverCommand*` 死代码连带删除，避免再误导新读者；
- **物模型边界**：物模型 `Command`（设备服务）以后落地时使用无前缀的 `Command` 词族，
  与 `PointCommand` 通过前缀天然分层。

### 2.2 重命名映射表

| 类别              | 旧名                                                             | 新名                                                   |
|-----------------|----------------------------------------------------------------|------------------------------------------------------|
| HTTP 路径前缀       | `/point_value_command`                                         | `/point_command`                                     |
| HTTP Controller | `PointValueCommandController`                                  | `PointCommandController`                             |
| Service 接口      | `PointValueCommandService`                                     | `PointCommandService`                                |
| Service 实现      | `PointValueCommandServiceImpl`                                 | `PointCommandServiceImpl`                            |
| 顶层 DTO          | `DeviceCommandDTO`                                             | `PointCommandDTO`                                    |
| 子结构（读）          | `DeviceCommandDTO.DeviceRead`                                  | `PointCommandDTO.ReadPayload`                        |
| 子结构（写）          | `DeviceCommandDTO.DeviceWrite`                                 | `PointCommandDTO.WritePayload`                       |
| 类型枚举            | `DeviceCommandTypeEnum`                                        | `PointCommandTypeEnum`                               |
| Exchange        | `dc3.e.command`                                                | `dc3.e.point_command`                                |
| 命令 routing      | `dc3.r.command.device.<svc>`                                   | `dc3.r.point_command.<svc>`                          |
| 命令队列            | `dc3.q.command.device.<svc>`                                   | `dc3.q.point_command.<svc>`                          |
| 死信 Exchange     | —                                                              | `dc3.e.point_command_dead`                           |
| 死信队列            | —                                                              | `dc3.q.point_command_dead`                           |
| 结果 Exchange     | —                                                              | `dc3.e.point_command_result`                         |
| 结果队列            | —                                                              | `dc3.q.point_command_result`                         |
| 常量类             | `RabbitConstant.*COMMAND*`（设备命令相关）                             | `RabbitConstant.*POINT_COMMAND*`                     |
| Driver 监听器      | `DeviceCommandReceiver`                                        | `PointCommandReceiver`                               |
| gRPC 方法         | `readCommand` / `writeCommand`                                 | `submitReadCommand` / `submitWriteCommand`           |
| Facade 接口       | `PointValueCommandFacade`                                      | `PointCommandFacade`                                 |
| Facade 方法       | `dispatchRead` / `dispatchWrite`                               | `submitRead` / `submitWrite`                         |
| Facade 实现       | `PointValueCommandGrpcFacade` / `PointValueCommandLocalFacade` | `PointCommandGrpcFacade` / `PointCommandLocalFacade` |
| 数据库表（新增）        | —                                                              | `dc3_point_command`                                  |

### 2.3 死代码清理

直接物理删除（删类 + 删导入 + 删常量）：

- `dc3-common/dc3-common-model/src/main/java/io/github/pnoker/common/entity/dto/DriverCommandDTO.java`
- `dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/DriverCommandTypeEnum.java`

确认零调用：`grep -r "DriverCommandDTO\|DriverCommandTypeEnum" --include="*.java"` 在生产源码里无非自身的引用。

### 2.4 不做兼容

旧 HTTP 路径、旧 exchange / queue、旧 DTO 类、旧 facade 接口、旧 gRPC 方法、旧常量在重构 PR 中
**一并删除**。具体含义：

- 不保留 `/point_value_command/*` forward；
- 不在 broker 上保留 `dc3.e.command` / `dc3.q.command.device.*`，**重构发版必须先停服**；
- 不保留 `@Deprecated` 包装；
- 前端、agentic、driver 必须在同一发版窗口内全部升级。

> 这是有意识的取舍：DC3 当前还在演进期，命名不一致带来的长期成本远大于一次切换的短期成本。
> 落地节奏依靠版本号 + CHANGELOG 的 BREAKING 通告对外承诺。

---

## 3. 链路总体目标

```
┌──────────────┐ HTTP/JSON  ┌────────────────────────┐ 持久化
│ 前端 / 第三方 │──────────▶│ dc3-center-data         │──────▶ dc3_point_command
└──────────────┘            │  PointCommandController │       (PENDING)
                            │  PointCommandService    │
┌──────────────┐  gRPC      │                         │
│ dc3-agentic  │──────────▶ │                         │
└──────────────┘            └────────────────────────┘
                                      │
                              发布到 dc3.e.point_command (publisher confirm + mandatory)
                                      │
                                      ▼
                  dc3.q.point_command.<svc> (durable, x-dead-letter-exchange)
                                      │
                                      ▼
                     ┌─────────────────────────────────┐
                     │ Driver: PointCommandReceiver        │
                     │  - 幂等去重 (commandId)              │
                     │  - 设备级串行 (deviceId 锁)            │
                     │  - 调用 DriverCustomService.read/write │
                     └─────────────────────────────────┘
                                      │
                       发布到 dc3.e.point_command_result
                                      │
                                      ▼
                  dc3.q.point_command_result (中心侧消费)
                                      │
                                      ▼
                    更新 dc3_point_command 状态
                    + 推送 WebSocket / 同步释放挂起的 HTTP 请求
```

四个核心改动：

1. 报文层：引入 `commandId`、`tenantId`、`source`、`occurredAt`、`expireAt`、`schemaVersion`；
   去掉 `content` 双重 JSON。
2. 路由层：新增 **结果回执** 通道 `dc3.e.point_command_result` + `dc3.q.point_command_result`；
   所有命令队列配置 DLX（`dc3.q.point_command_dead`）。
3. 存储层：新增 `dc3_point_command` 持久化每条命令的生命周期
   （PENDING → SENT → SUCCESS / FAILED / TIMEOUT / EXPIRED / DEAD）。
4. 行为层：driver SDK 强制使用 `write()` 返回值；幂等去重（commandId）；按 deviceId 串行化。

---

## 4. 详细设计

### 4.1 命令报文（DTO）

仅给字段，类型标注按 Java 写法。完整 IDL 在落地 PR 中给出。

```java
public sealed interface PointCommandPayload
        permits PointCommandPayload.Read, PointCommandPayload.Write {

    record Read(Long deviceId, Long pointId) implements PointCommandPayload {
    }

    record Write(Long deviceId, Long pointId, String value) implements PointCommandPayload {
    }
}

public record PointCommandDTO(
        String commandId,        // UUID v4，调用方生成或中心侧补
        Long tenantId,         // 必填:跨租户隔离
        PointCommandTypeEnum type,            // READ / WRITE
        PointCommandPayload payload,          // 直接强类型,不再嵌套 JSON 字符串
        CommandSource source,           // HTTP / GRPC / AGENTIC / SCHEDULED
        Long sourceUserId,     // 发起人(HTTP 来源),可空
        Instant occurredAt,       // 调用方时间戳(UTC)
        Instant expireAt,         // 软超时,broker TTL 之外的应用层超时
        int schemaVersion     // 协议版本,初版 = 1
) {
}
```

要点：

- `payload` 用 sealed interface + Jackson polymorphic（`@JsonTypeInfo(use = NAME, property = "kind")`），
  反序列化时按 `kind` 字段直接产出 `Read` / `Write`，**取消 `content` 字段的二次 JSON 序列化**。
- 所有时间字段使用 `Instant`（UTC），统一通过项目级 `ObjectMapper` + `JavaTimeModule` 配置；不接受裸 `LocalDateTime`。
- `commandId` 强制通过 Jakarta Validation 校验为 36 位 UUID 字符串；调用方未填写则中心侧补 UUID v4。
- `expireAt` 默认 = `occurredAt + 10s`，前端可在 VO 里覆盖；driver 收到时若已超期直接丢弃并发 TIMEOUT 结果。
- 不再 `implements Serializable`，改为纯 record；DTO 仅经由 JSON 在 RabbitMQ 流转。

结果回执 DTO：

```java
public record PointCommandResultDTO(
        String commandId,
        Long tenantId,
        PointCommandStatusEnum status,        // SUCCESS / FAILED / TIMEOUT / EXPIRED / DUPLICATE
        String responseValue,    // 成功时回写值,失败时为空
        String errorCode,        // 失败码,例如 PROTOCOL_REJECT / DEVICE_OFFLINE / VALIDATION
        String errorMessage,
        Instant finishedAt,
        int schemaVersion
) {
}
```

### 4.2 路由拓扑

| Exchange                             | Queue                        | Routing Key                  | TTL / DLX                                  |
|--------------------------------------|------------------------------|------------------------------|--------------------------------------------|
| `dc3.e.point_command` (topic)        | `dc3.q.point_command.<svc>`  | `dc3.r.point_command.<svc>`  | TTL=30s，**DLX=`dc3.e.point_command_dead`** |
| `dc3.e.point_command_result` (topic) | `dc3.q.point_command_result` | `dc3.r.point_command_result` | TTL=60s，DLX=`dc3.e.point_command_dead`     |
| `dc3.e.point_command_dead` (topic)   | `dc3.q.point_command_dead`   | `#`                          | 无 TTL，需人工/定时任务清理                           |

要点：

- 命令队列引入 **DLX**，覆盖现状（reject 后静默丢失）的最大风险；DLX 队列的消息携带 `x-death` 头，便于审计与重放。
- Driver 端继续以 `serviceName` 为路由键，不变；多副本 driver 共享同一队列，competing consumers 语义保持。
- 结果回执通道是新加的，driver SDK 必须发；中心侧消费者更新命令状态 + 触发 WebSocket 推送。

### 4.3 持久化：`dc3_point_command`

```sql
CREATE TABLE dc3_point_command
(
    id BIGSERIAL PRIMARY KEY,
    command_id     CHAR(36)    NOT NULL,
    tenant_id      BIGINT      NOT NULL,
    type           VARCHAR(8)  NOT NULL, -- READ / WRITE
    device_id      BIGINT      NOT NULL,
    point_id       BIGINT      NOT NULL,
    request_value  VARCHAR(256),         -- 仅 WRITE
    response_value VARCHAR(256),         -- 反馈值
    status         VARCHAR(16) NOT NULL, -- PENDING / SENT / SUCCESS / FAILED / TIMEOUT / EXPIRED / DEAD
    error_code     VARCHAR(64),
    error_message  VARCHAR(1024),
    source         VARCHAR(16) NOT NULL,
    source_user_id BIGINT,
    occurred_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    expire_at TIMESTAMPTZ NOT NULL,
    schema_version SMALLINT    NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, command_id)
);

CREATE INDEX idx_point_command_lookup
    ON dc3_point_command (tenant_id, device_id, point_id, occurred_at DESC);
CREATE INDEX idx_point_command_pending
    ON dc3_point_command (status, expire_at) WHERE status IN ('PENDING','SENT');
```

- 持久化只在中心侧做；driver 端依赖回执 + 自身日志，不引入额外存储。
- 表保留按 `tenant_id` 分区或定期归档（默认 7 天，与点值历史的留存策略一致）。

### 4.4 中心侧 API

新增 / 调整：

| 方法                                 | 说明                                  |
|------------------------------------|-------------------------------------|
| `POST /point_command/read`         | 提交读命令（同步等回执）                        |
| `POST /point_command/write`        | 提交写命令（同步等回执）                        |
| `POST /point_command/submit-async` | 提交并立即返回 `commandId`，由调用方轮询/订阅       |
| `GET  /point_command/{commandId}`  | 查询单条命令最终状态                          |
| `GET  /point_command/list`         | 按 tenant/device/point/status/时间窗 检索 |

同步等待策略：

- 中心侧维护内存级 `Map<commandId, CompletableFuture<Result>>`；
- 收到 result 消息时 complete future；
- HTTP 等待窗口 = `min(expireAt - now, 5s)`；
- 超时返回 `408 + 当前状态`，状态可能是 `SENT`（仍在执行）或 `EXPIRED`（已知超时）；
- 超时后 future 不再阻塞调用方，但状态机依然受 result 通道驱动。

> 同步等回执对前端"写入并立刻看到结果"的体验最直接，但只在 dc3-center-data 单实例内有效。多实例部署时，
> 外部接入实例 A 但 result 落到实例 B → A 的内存 future 不会被 complete。解决方案有两个，落地时选其一：
>
> - **简易方案（推荐先行）**：HTTP 短轮询 / SSE，前端 1s 一次查 `GET /point_command/{id}`，最长 10s。
> - **进阶方案**：所有 dc3-center-data 实例都订阅 `dc3.q.point_command_result`，按 `commandId` 哈希分片，
    > 或者用 Redis pub/sub 把 result 广播给所有实例，由持有 future 的实例 complete。

### 4.5 Driver SDK 改动

`PointCommandReceiver`（替代 `DeviceCommandReceiver`）：

```java

@RabbitHandler
@RabbitListener(queues = "#{pointCommandQueue.name}",
        containerFactory = "highThroughputRabbitListenerContainerFactory")
public void onCommand(Channel channel, Message message, PointCommandDTO dto) {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();

    // 1) expireAt 提前判断,过期直接丢回执
    if (Instant.now().isAfter(dto.expireAt())) {
        resultSender.send(Result.expired(dto));
        RabbitAckUtil.ack(channel, deliveryTag);
        return;
    }

    // 2) 幂等去重(Caffeine 5min TTL)
    if (!dedupCache.tryAcquire(dto.commandId())) {
        resultSender.send(Result.duplicate(dto));   // 仍然回执,调用方可见
        RabbitAckUtil.ack(channel, deliveryTag);
        return;
    }

    // 3) deviceId 串行化(每 device 一把锁)
    deviceLockManager.runExclusive(dto.payload().deviceId(), () -> {
        try {
            Result r = dispatcher.dispatch(dto);    // 内部分流到 read / write
            resultSender.send(r);
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (TransientException te) {
            // 设备瞬时故障:第一次 nack 重投,第二次 reject 进 DLX,并立即发 FAILED 回执
            handleFailure(channel, deliveryTag, message, dto, te);
        } catch (Exception e) {
            // 协议/参数错误:直接 reject 进 DLX,发 FAILED 回执
            resultSender.send(Result.failed(dto, e));
            RabbitAckUtil.reject(channel, deliveryTag);
        }
    });
}
```

`DriverWriteServiceImpl` 改造点：

- 必须接收 `driverCustomService.write(...)` 的 `boolean` 返回值；
- **不再 echo 假 `PointValue`**；只有 `write()` 真正返回 `true` 才在 result 中携带 `responseValue`；
- 失败时填充 `errorCode + errorMessage`，并发回 FAILED result；
- 可写性校验前置：`device.enableFlag`、`point.enableFlag`、`point.rwFlag.contains(WRITE)`；
- 类型与值范围校验前置：`PointCommandValidator` 读取 `point_ext` 的 min/max/enum/step。

`DriverReadServiceImpl` 同样调整：把读到的真实值放进 result 而不是仅触发 PointValue 流；
PointValue 上行链路保持不变，driver 在读成功后仍走 `pointValueSender`。

### 4.6 幂等去重缓存

```java
class CommandDedupCache {
    Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(50_000)
            .build();

    boolean tryAcquire(String commandId) {
        return cache.asMap().putIfAbsent(commandId, Boolean.TRUE) == null;
    }
}
```

设计要点：

- 仅在 driver 进程内做去重，不引入 Redis；多副本 driver 一份命令只投递给一个副本，副本内幂等已足够。
- TTL = 5 分钟，覆盖典型重投窗口（broker reconnect、driver short outage）。
- 重复命令也会发回回执（status = `DUPLICATE`），保证调用方一定能看到结果。

### 4.7 验证规则前置

中心侧 `validateCommandScope` 必须新增：

| 校验                                        | 失败处理                                      |
|-------------------------------------------|-------------------------------------------|
| `device.enableFlag = ENABLE`              | 400 / 状态 = REJECTED                       |
| `point.enableFlag = ENABLE`               | 400 / 状态 = REJECTED                       |
| WRITE 时 `point.rwFlag` 包含 WRITE           | 400 / 状态 = REJECTED                       |
| `driver` 已注册（注册中心心跳新于 30s）                | 503 / 状态 = REJECTED（避免 30s 后被 broker 静默丢） |
| WRITE 值落入 `point_ext` 的 min/max/enum/step | 400 / 状态 = REJECTED                       |
| `commandId` 在表中已存在                        | 200 + 当前状态（天然幂等到中心侧）                      |

中心侧的中心化校验保证 driver 端不会再收到根本不该执行的命令；driver 端只对协议层做最终校验。

### 4.8 publisher 侧确认

publisher confirm 已在
`dc3-common/dc3-common-rabbitmq/src/main/java/io/github/pnoker/common/config/RabbitConfig.java:86-100` 启用，但目前只是日志。
改造为：

- `RabbitTemplate#correlationData = commandId`；
- ConfirmCallback ACK → 把 `dc3_point_command.status` 由 PENDING 推进到 SENT；
- ConfirmCallback NACK 或 ReturnCallback → 立即推进到 FAILED 并写入错误原因。

---

## 5. 时序示例

写命令同步等回执的成功路径：

```
前端                Center-Data                  Broker                 Driver                设备
 │  POST /write       │                              │                      │                    │
 │ ─────────────────▶ │ INSERT command (PENDING)     │                      │                    │
 │                    │ publish (correlationId=id)   │                      │                    │
 │                    │ ───────────────────────────▶ │                      │                    │
 │                    │  ◀── confirm ACK ──── status=SENT                   │                    │
 │                    │                              │ ─── deliver ───────▶ │                    │
 │                    │                              │                      │ dedup OK           │
 │                    │                              │                      │ deviceLock acquire │
 │                    │                              │                      │ writeRegister ────▶│
 │                    │                              │                      │ ◀── ok ────────────│
 │                    │                              │                      │ ack command        │
 │                    │                              │ ◀── publish result ──│                    │
 │                    │ ◀── deliver result ──        │                      │                    │
 │                    │ status=SUCCESS               │                      │                    │
 │                    │ complete future              │                      │                    │
 │ ◀── 200 OK + value │                              │                      │                    │
```

异常路径下的关键差异：

- driver 抛异常 → `nack(requeue=true)` + 同步发一条 FAILED result（status 会被覆盖回滚 `SENT`）；
- 二次失败 → `reject(requeue=false)` 进 DLX；中心侧 result 消费器收到 `x-death` 后把状态推进到 `DEAD`。

---

## 6. 切换策略（一步到位）

### 6.1 版本切割

- 所在版本号下个 minor +1（参考 CHANGELOG 既有 BREAKING 通告风格），并在发版说明中明确：
    1. HTTP 路径 `/point_value_command/*` 永久下线，调用方必须切到 `/point_command/*`；
    2. 旧 RabbitMQ 资源 `dc3.e.command` 与 `dc3.q.command.device.*` 永久删除；
    3. 旧 DTO `DeviceCommandDTO` / `DeviceCommandTypeEnum` / `PointValueCommandFacade` 等永久删除；
    4. 旧 driver 与新中心**不能混跑**：升级 center 前必须停掉所有旧 driver；升级完成后再启动新 driver。
- 升级清单作为 release 必备项写入 [docs/operation](../operation/)；不在 CHANGELOG 里隐瞒 BREAKING。

### 6.2 数据库变更

- 新增 `dc3_point_command`（DDL 见 §4.3）；
- 可选：在 `dc3_point.point_ext` 中预留 `constraints`（min / max / enum / step / readonly）字段，
  缺失时跳过值校验，不阻塞重构发版。

### 6.3 前端 / agentic / driver 同步升级

| 模块                 | 升级动作                                                                          |
|--------------------|-------------------------------------------------------------------------------|
| iot-dc3-web        | 切换到 `/api/v3/data/point_command/*` 路径，新增"命令历史"页面                              |
| dc3-center-agentic | 重新生成 gRPC stub，`PointValueCommandFacade` 改 `PointCommandFacade`               |
| dc3-driver-*       | 升级 SDK：监听 `dc3.q.point_command.<svc>`，使用 `PointCommandDTO`，强制接收 `write()` 返回值 |

> 部署窗口：建议夜间维护窗口，center 与所有 driver 同窗口替换；不留并行运行的灰度区。

---

## 7. 落地阶段

| 阶段                       | 范围                                                                                                           | 出口标准                                                                                              |
|--------------------------|--------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Phase 1：命名一刀切 + 死代码清理    | 删除 `PointValueCommand*` / `DeviceCommand*` / `DriverCommand*`；重构为 `PointCommand*`；旧 exchange/queue/HTTP 路径删除 | `grep -rn "PointValueCommand\|DeviceCommand\|DriverCommand"` 在生产源码里返回 0 行；旧 broker 资源不再被声明；测试套件全绿 |
| Phase 2：报文模型 + 持久化 + DLX | 引入 `commandId`、sealed payload、`Instant` 时间字段、`dc3_point_command` 表、DLX 配置                                    | 集成测试覆盖：重复投递（落到中心侧幂等）、过期投递（直接 EXPIRED）、二次失败入 DLX                                                   |
| Phase 3：幂等 + 串行 + 校验     | driver 端 `CommandDedupCache`、`deviceLockManager`；中心侧 `validateCommandScope` 扩展校验                             | 压测：同 device 并发写无协议交错；禁用点位被 400 拒绝；离线 driver 命令立刻 503                                              |
| Phase 4：结果回执 + 同步 API    | result exchange / queue；`POST /point_command/{read,write}` 同步等回执；`GET /point_command/{id}`；DLX 中状态推进         | 端到端测试：HTTP 写命令收到的不是假 `R.ok`，而是真实 SUCCESS/FAILED 与最终值                                              |

四个阶段在同一个 release 内完成，但代码评审与合并按阶段拆 PR；不在 minor 中间释放半成品。

---

## 8. 性能与容量预估

- `dc3_point_command` 写入 QPS ≈ HTTP 写入 QPS。在 1k QPS 下，按 7 天保留 ≈ 6 亿行，
  需要在 `tenant_id, occurred_at` 上做范围分区或冷归档，不在初版要求范围内但要预留。
- 同步等待用的 `CompletableFuture` 数量 ≈ 并发 HTTP 请求数；超时强制释放，无溢出风险。
- driver 端 `Caffeine` dedup 缓存：50_000 条 × 36B ≈ 2MB，可忽略。
- result 通道双倍 RabbitMQ 流量；按现有点值流量 1/100 估算，broker 压力增量极小。

---

## 9. 风险与未决项

| 项                   | 风险                                              | 备选                                  |
|---------------------|-------------------------------------------------|-------------------------------------|
| 同步等待 vs 异步轮询        | 多实例部署时同步等待需要广播 result                           | Phase 4 仅暴露异步接口 + 轮询，进阶方案延后         |
| `commandId` 是否对前端必填 | 前端忘填会丢失天然幂等                                     | 中心侧补 UUID 但同时强烈推荐前端预生成              |
| 物模型 `Command` 何时落地  | 一旦落地需要 `dc3.e.command` / `dc3_command` 与本方案命名分离 | 物模型方案 PR 中显式约束新命名前缀                 |
| DLX 中消息的回放工具        | 当前没有运维工具消费 `dc3.q.point_command_dead`           | Phase 4 之后单独立项补 CLI 或后台页面           |
| Driver 进程级 dedup    | driver 重启会丢去重窗口                                 | 已在 §4.6 列为已知约束；如需跨重启幂等，需上 Redis     |
| 一刀切对生产环境的冲击         | 需要全量停服窗口；老 driver 不能混跑                          | release notes 提前两周公告；提供升级 checklist |

---

## 10. 决策项（请评审确认）

1. **命名**：`PointCommand` 已确认；命名空间 `io.github.pnoker.common.data.point.command` /
   `io.github.pnoker.common.driver.point.command` 是否接受？
2. **同步等回执范围**：Phase 4 是否仅在单实例中心可用，多实例广播作为后续单独立项？
3. **DLX 重放工具**：是否在 Phase 4 之后单独立项？
4. **WRITE 值范围校验位置**：建议中心侧（统一）+ driver 侧（贴协议）两层兜底，是否同意？
5. **`schema_version`**：是否进库？建议进，便于以后协议升级时筛选历史命令。

---

## 11. 引用

- 物模型设计：[docs/design/thing-model.md](thing-model.md)
- 现状代码定位：
  -
  `dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/controller/PointValueCommandController.java:42-78`
  -
  `dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/biz/impl/PointValueCommandServiceImpl.java:64-111`
    - `dc3-common/dc3-common-model/src/main/java/io/github/pnoker/common/entity/dto/DeviceCommandDTO.java:44-148`
    -
  `dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/receiver/rabbit/DeviceCommandReceiver.java:62-108`
  -
  `dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/service/impl/DriverWriteServiceImpl.java:69-124`
    - `dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/config/DriverTopicConfig.java:60-112`
    - `dc3-common/dc3-common-rabbitmq/src/main/java/io/github/pnoker/common/config/RabbitConfig.java:51-152`
    - `dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/grpc/server/PointValueServer.java:154-217`
    - 死代码：`dc3-common/dc3-common-model/src/main/java/io/github/pnoker/common/entity/dto/DriverCommandDTO.java`、
      `dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/DriverCommandTypeEnum.java`
