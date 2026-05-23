---
title: 位号命令链路重构方案
---

# 位号命令链路重构方案

本文是 DC3 位号读写下行链路（HTTP/gRPC → 数据中心 → RabbitMQ → 驱动 → 设备）的重构与优化设计。

> **实施状态**：方案已分阶段落地中。命名收敛 (Phase 1) 已全部完成；链路质量改造 (Phase 2-4) 部分完成，剩余项见各章节
`[DONE]` / `[PARTIAL]` / `[TODO]` 标记。

---

## 0. 实施进度总览

| 章节   | 模块                            | 状态        | 关键缺失                                                                   |
|------|-------------------------------|-----------|------------------------------------------------------------------------|
| §2   | 命名收敛                          | [DONE]    | —                                                                      |
| §2.3 | 死代码清理                         | [DONE]    | —                                                                      |
| §4.1 | DTO 序列化改造                     | [DONE]    | sealed interface + polymorphic payload + `Instant` 时间字段 + record DTO   |
| §4.2 | 路由拓扑 (DLX + result)           | [DONE]    | —                                                                      |
| §4.3 | `dc3_point_command` 持久化       | [DONE]    | —                                                                      |
| §4.4 | 同步等待 API                      | [PARTIAL] | POST 返回 `commandId`, GET `/{commandId}` 可轮询; CompletableFuture 同步等待未实现 |
| §4.5 | Driver: write() 返回值           | [DONE]    | `Boolean` 返回值被正确接收, 失败不 echo 假值                                        |
| §4.5 | Driver: 设备级串行锁                | [DONE]    | `DeviceLockManager.runExclusive(deviceId)` 串行化同设备命令                    |
| §4.6 | 幂等去重                          | [DONE]    | —                                                                      |
| §4.7 | 中心侧 enableFlag / rwFlag 校验    | [DONE]    | —                                                                      |
| §4.7 | Driver 在线检查                   | [DONE]    | `EntityStateMapper` 查询 DRIVER 状态, 离线抛 `ServiceException`               |
| §4.7 | 值范围校验 (PointCommandValidator) | [PARTIAL] | 基础非空校验已实现; min/max/enum/step 校验待 `point_ext.constraints` 落地            |
| §4.8 | publisher confirm → SENT      | [DONE]    | —                                                                      |
| §4.5 | Driver: expireAt 预判           | [DONE]    | `PointCommandReceiver` 入口处检查, 过期直接 EXPIRED                             |
| §4.4 | 查询/列表 API                     | [DONE]    | `GET /{commandId}` + `POST /list` 分页过滤已实现                              |
| §4.7 | commandId 幂等                  | [DONE]    | 调用方可选传 commandId, 重复提交返回已有状态                                           |

---

## 1. 背景

### 1.1 命名收敛（已完成）

原代码中同时存在三组类，经 Phase 1 已统一为 `PointCommand*`：

| 组别                                                              | 模块                                     | 处置                    |
|-----------------------------------------------------------------|----------------------------------------|-----------------------|
| `PointValueCommand*`                                            | dc3-common-data / dc3-common-facade    | 已重命名为 `PointCommand*` |
| `DeviceCommand*`                                                | dc3-common-model / dc3-common-driver   | 已合并到 `PointCommand*`  |
| `DriverCommand*` (`DriverCommandDTO` / `DriverCommandTypeEnum`) | dc3-common-model / dc3-common-constant | 已删除（死代码）              |

RabbitMQ 资源已统一为 `dc3.e.point_command` / `dc3.q.point_command.<svc>` / `dc3.r.point_command.<svc>`。

### 1.2 与物模型 `Command`（设备服务）的语义边界

参见 [物模型设计方案](thing-model.md)。物模型规划中 `Command` 表示**读写 Point 之外的动作型服务**
（重启、校准、切换模式等），定义在 `Profile` 下、由 `Device` 调用。

本方案的位号读写不是物模型层概念，而是属性维度的运行态访问能力。两者通过命名前缀分层：

| 概念          | 命名前缀           | 队列 / 表前缀                                    |
|-------------|----------------|---------------------------------------------|
| 位号读写命令（本方案） | `PointCommand` | `dc3.e.point_command` / `dc3_point_command` |
| 物模型设备服务（未来） | `Command`      | `dc3.e.command` / `dc3_command`             |

### 1.3 当前下行链路问题（及已解决项）

| 维度   | 问题                                                                                  | 状态                                                           |
|------|-------------------------------------------------------------------------------------|--------------------------------------------------------------|
| 唯一性  | 报文无 ID；同 device 多线程消费                                                               | [DONE] — `commandId` + 幂等去重 + `DeviceLockManager` 设备级串行锁     |
| 准确性  | 不校验 enableFlag / rwFlag / driver 在线 / 值范围; 离线 driver 命令 30s 后静默丢弃                   | [PARTIAL] — enableFlag / rwFlag / driver 在线校验已实现; 值范围仅基础非空   |
| 结果反馈 | HTTP 立即 `R.ok()`; driver SDK 忽略 `write()` 返回值并 echo 假位号值                            | [DONE] — `write()` 返回值被正确接收, POST 返回 commandId 可轮询           |
| 序列化  | DTO 字段 `content` 是二次 JSON 序列化的 `String`; 缺 tenantId / commandId / source / expireAt | [DONE] — sealed interface + polymorphic payload + record DTO |

---

## 2. 命名收敛 [DONE]

Phase 1 已全部完成。以下映射表保留作为历史参照。

### 2.1 重命名映射表

| 类别              | 旧名                                                             | 新名                                                   |
|-----------------|----------------------------------------------------------------|------------------------------------------------------|
| HTTP 路径前缀       | `/point_value_command`                                         | `/point_command`                                     |
| HTTP Controller | `PointValueCommandController`                                  | `PointCommandController`                             |
| Service 接口      | `PointValueCommandService`                                     | `PointCommandService`                                |
| Service 实现      | `PointValueCommandServiceImpl`                                 | `PointCommandServiceImpl`                            |
| 顶层 DTO          | `DeviceCommandDTO`                                             | `PointCommandDTO`                                    |
| 子结构（读）          | `DeviceCommandDTO.DeviceRead`                                  | `PointCommandDTO.PointRead`                          |
| 子结构（写）          | `DeviceCommandDTO.DeviceWrite`                                 | `PointCommandDTO.PointWrite`                         |
| 类型枚举            | `DeviceCommandTypeEnum`                                        | `PointCommandTypeEnum`                               |
| Exchange        | `dc3.e.command`                                                | `dc3.e.point_command`                                |
| 命令 routing      | `dc3.r.command.device.<svc>`                                   | `dc3.r.point_command.<svc>`                          |
| 命令队列            | `dc3.q.command.device.<svc>`                                   | `dc3.q.point_command.<svc>`                          |
| 死信 Exchange     | —                                                              | `dc3.e.point_command_dead`                           |
| 死信队列            | —                                                              | `dc3.q.point_command_dead`                           |
| 结果 Exchange     | —                                                              | `dc3.e.point_command_result`                         |
| 结果队列            | —                                                              | `dc3.q.point_command_result`                         |
| 常量类             | `RabbitConstant.*COMMAND*`                                     | `RabbitConstant.*POINT_COMMAND*`                     |
| Driver 监听器      | `DeviceCommandReceiver`                                        | `PointCommandReceiver`                               |
| Facade 接口       | `PointValueCommandFacade`                                      | `PointCommandFacade`                                 |
| Facade 实现       | `PointValueCommandGrpcFacade` / `PointValueCommandLocalFacade` | `PointCommandGrpcFacade` / `PointCommandLocalFacade` |
| 数据库表            | —                                                              | `dc3_point_command`                                  |

### 2.2 死代码清理 [DONE]

已物理删除：

- `dc3-common-model/.../dto/DriverCommandDTO.java`
- `dc3-common-constant/.../enums/DriverCommandTypeEnum.java`

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

四个核心改动及实施状态：

1. 报文层 [DONE]：引入 `commandId`、`tenantId`、`source`、`occurredAt`、`expireAt`、`schemaVersion`；
   去掉 `content` 双重 JSON → sealed interface + polymorphic payload。
2. 路由层 [DONE]：结果回执通道 + DLX 均已配置并运行。
3. 存储层 [DONE]：`dc3_point_command` 表 + `PointCommandDO` + 生命周期状态机已落地。
4. 行为层 [DONE]：幂等去重 + `write()` 返回值 + 设备级串行锁 + `expireAt` 预判均已实现。

---

## 4. 详细设计

### 4.1 命令报文（DTO）[DONE]

> 当前实现仍使用旧模型：`PointCommandDTO` 为 `@Builder` 类，`content` 字段存放二次 JSON 序列化的 `PointRead`/`PointWrite`
> 字符串，时间字段为 `LocalDateTime`，且 `implements Serializable`。以下为设计目标。

完整 IDL 在落地 PR 中给出。

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

### 4.2 路由拓扑 [DONE]

路由已全部配置并运行。`DriverTopicConfig` 声明 driver 侧命令队列（30s TTL + DLX），`DataTopicConfig` 声明中心侧死信队列与结果消费队列。

| Exchange                             | Queue                        | Routing Key                  | TTL / DLX                                  |
|--------------------------------------|------------------------------|------------------------------|--------------------------------------------|
| `dc3.e.point_command` (topic)        | `dc3.q.point_command.<svc>`  | `dc3.r.point_command.<svc>`  | TTL=30s，**DLX=`dc3.e.point_command_dead`** |
| `dc3.e.point_command_result` (topic) | `dc3.q.point_command_result` | `dc3.r.point_command_result` | TTL=60s，DLX=`dc3.e.point_command_dead`     |
| `dc3.e.point_command_dead` (topic)   | `dc3.q.point_command_dead`   | `#`                          | 无 TTL，需人工/定时任务清理                           |

要点：

- 命令队列引入 **DLX**，覆盖现状（reject 后静默丢失）的最大风险；DLX 队列的消息携带 `x-death` 头，便于审计与重放。
- Driver 端继续以 `serviceName` 为路由键，不变；多副本 driver 共享同一队列，competing consumers 语义保持。
- 结果回执通道是新加的，driver SDK 必须发；中心侧消费者更新命令状态 + 触发 WebSocket 推送。

### 4.3 持久化：`dc3_point_command` [DONE]

表已通过 MyBatis Plus 落地（`PointCommandDO`，`PointCommandManager`）。中心侧写入 PENDING → SENT，
`PointCommandResultReceiver` 推进到终态，`PointCommandDeadReceiver` 处理死信。

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

### 4.4 中心侧 API [DONE]

| 方法                                 | 状态     | 说明                                        |
|------------------------------------|--------|-------------------------------------------|
| `POST /point_command/read`         | [DONE] | 返回 `R<String>` (commandId), 调用方可轮询结果      |
| `POST /point_command/write`        | [DONE] | 同上                                        |
| `POST /point_command/submit-async` | [TODO] | 未实现（低优先级，当前 fire-and-forget 可通过 list 查状态） |
| `GET /point_command/{commandId}`   | [DONE] | RESTful 路径, 返回 `PointCommandDO` 含当前状态     |
| `POST /point_command/list`         | [DONE] | 分页查询，支持 deviceId/pointId/status/type 过滤   |

同步等待策略（待实现）：

- 中心侧维护内存级 `Map<commandId, CompletableFuture<Result>>`；
- 收到 result 消息时 complete future；
- HTTP 等待窗口 = `min(expireAt - now, 5s)`；
- 超时返回 `408 + 当前状态`；
- 超时后 future 不再阻塞调用方，但状态机依然受 result 通道驱动。

> 同步等待仅在 dc3-center-data 单实例内有效。多实例部署需选型：
>
> - **简易方案（推荐先行）**：HTTP 短轮询 / SSE，前端 1s 一次查 `GET /point_command/{id}`，最长 10s。
> - **进阶方案**：Redis pub/sub 广播 result，由持有 future 的实例 complete。

### 4.5 Driver SDK 改动 [DONE]

已实现项：`PointCommandReceiver` 替代了 `DeviceCommandReceiver`；幂等去重 + 结果回执通道可用；`write()` 返回值被正确接收；
`expireAt` 预判已实现。

待实现项（按优先级）：

| 项                     | 设计                                                    | 当前缺陷                                                        |
|-----------------------|-------------------------------------------------------|-------------------------------------------------------------|
| write() 返回值           | 必须接收 `driverCustomService.write(...)` 的 `boolean` 返回值 | [DONE]                                                      |
| 真实读值进 result          | `read()` 的真实值放入 result，不单靠 PointValue 流               | `responseValue` 始终为 null（read 是异步的）                         |
| 设备级串行锁                | `deviceLockManager.runExclusive(deviceId, ...)`       | [DONE] `DeviceLockManager` 已集成到 `PointCommandReceiver` 分发链路 |
| expireAt 预判           | `Instant.now().isAfter(dto.expireAt())` → 直接 EXPIRED  | [DONE]                                                      |
| PointCommandValidator | 从 `point_ext` 读取 min/max/enum/step 校验                 | 基础非空已实现, 扩展校验待 `point_ext.constraints` 落地                   |

### 4.6 幂等去重缓存 [DONE]

已实现 `CommandDedupCache`（Caffeine, 5min TTL, 50k max, `putIfAbsent`），位置：
`dc3-common-driver/.../cache/CommandDedupCache.java`。Duplicate 命令发 `DUPLICATE` 回执。

### 4.7 验证规则前置 [DONE]

| 校验                                        | 状态     | 失败处理                                                    |
|-------------------------------------------|--------|---------------------------------------------------------|
| `device.enableFlag = ENABLE`              | [DONE] | 已在 `PointCommandServiceImpl.validateScope()`            |
| `point.enableFlag = ENABLE`               | [DONE] | 同上                                                      |
| WRITE 时 `point.rwFlag` 包含 WRITE           | [DONE] | 已在 `validateWriteScope()`                               |
| `driver` 已注册且在线                           | [DONE] | `EntityStateMapper` 查 DRIVER 状态, 离线抛 `ServiceException` |
| WRITE 值基础非空                               | [DONE] | `PointCommandValidator.validateWriteValue()`            |
| WRITE 值落入 `point_ext` 的 min/max/enum/step | [TODO] | 待 `point_ext.constraints` 字段落地                          |
| `commandId` 在表中已存在                        | [DONE] | 调用方可选传 commandId, 重复提交返回已有 commandId                    |

### 4.8 publisher 侧确认 [DONE]

已实现：`RabbitTemplate#correlationData = commandId`，ConfirmCallback ACK 推进 PENDING → SENT。

---

## 5. 时序示例

> 当前实际路径中 `complete future` 和 `200 OK + value` 两步未实现，HTTP 在 publish 后立即返回 `R.ok()`。以下为设计目标。

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

## 6. 升级说明

Phase 1（命名一刀切）已完成并发布。旧 HTTP 路径、旧 exchange/queue、旧 DTO 已删除。新 driver 与旧中心不能混跑。

### 6.1 剩余改造的升级策略

剩余 Phase 2-4 改造对以下模块的要求：

| 模块                 | 影响                          | 升级动作                   |
|--------------------|-----------------------------|------------------------|
| iot-dc3-web        | HTTP API 语义变化（同步返回真实结果）     | 切换到新 API 语义；新增"命令历史"页面 |
| dc3-center-agentic | gRPC stub 命名变化              | 重新生成 stub              |
| dc3-driver-*       | SDK 行为变化（write 返回值、设备锁、值校验） | 升级 SDK 依赖              |

### 6.2 数据库变更

- `dc3_point_command` 表已创建（DDL 见 §4.3）；
- 预留：`dc3_point.point_ext` 增加 `constraints` 字段（min / max / enum / step / readonly），
  缺失时跳过值校验，不阻塞发版。

---

## 7. 落地阶段（修订）

原 Phase 1 已全部完成。Phase 2-4 按实际进度重新划分优先级：

| 优先级 | 步骤                | 范围                                                                                                          | 出口标准                                                 |
|-----|-------------------|-------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| P0  | DTO 序列化改造         | sealed interface + polymorphic payload；`Instant` 时间字段；`commandId` / `expireAt` / `source` / `schemaVersion` | [DONE] `content` JSON 字符串字段删除；DTO 为 record 类型        |
| P0  | write() 返回值       | `DriverWriteServiceImpl` 接收 `Boolean` 返回值；失败不 echo 假值                                                       | [DONE] 写失败时 result.status = FAILED, responseValue 为空 |
| P1  | 同步等待 / 轮询 API     | `POST /point_command/*` 返回 commandId；`GET /point_command/{id}` 轮询                                           | [PARTIAL] 轮询已可用; CompletableFuture 同步等待未实现           |
| P1  | Driver 在线 + 值范围校验 | 中心侧在线检查；`PointCommandValidator`                                                                             | [PARTIAL] 在线检查已实现; 值范围仅基础非空                          |
| P2  | 设备级串行锁            | `deviceLockManager.runExclusive(deviceId)`                                                                  | [DONE] 同 device 并发写无协议交错                             |
| P2  | expireAt 预判       | `PointCommandReceiver` 入口处检查 `expireAt`                                                                     | [DONE] 过期命令直接 EXPIRED，不进业务逻辑                         |
| P3  | 查询/列表 API 完善      | `GET /point_command/{commandId}` 规范 URL；`GET /point_command/list` 检索                                        | [DONE] RESTful 查询 + 分页列表已实现                          |
| P3  | DLX 重放工具          | CLI 或后台页面消费 `dc3.q.point_command_dead`                                                                      | [TODO] 运维可从死信队列恢复命令                                  |

每个步骤可独立 PR，不要求同一 release。

---

## 8. 性能与容量预估

- `dc3_point_command` 写入 QPS ≈ HTTP 写入 QPS。在 1k QPS 下，按 7 天保留 ≈ 6 亿行，
  需要在 `tenant_id, occurred_at` 上做范围分区或冷归档，不在初版要求范围内但要预留。
- 同步等待用的 `CompletableFuture` 数量 ≈ 并发 HTTP 请求数；超时强制释放，无溢出风险。
- driver 端 `Caffeine` dedup 缓存：50_000 条 × 36B ≈ 2MB，可忽略。
- result 通道双倍 RabbitMQ 流量；按现有点值流量 1/100 估算，broker 压力增量极小。

---

## 9. 风险与未决项

| 项                             | 状态   | 风险                                            | 应对                                                        |
|-------------------------------|------|-----------------------------------------------|-----------------------------------------------------------|
| 同步等待 vs 异步轮询                  | 未决   | 多实例部署时同步等待需要广播 result                         | 简易方案：202 + 轮询；进阶方案：Redis pub/sub                          |
| `commandId` 是否对前端必填           | 已决   | 前端忘填会丢失天然幂等                                   | 中心侧补 UUID，同时推荐前端预生成                                       |
| 物模型 `Command` 何时落地            | 未决   | 需要 `dc3.e.command` / `dc3_command` 命名空间       | 物模型方案 PR 中显式约束命名前缀                                        |
| DLX 重放工具                      | 未决   | 无运维工具消费死信队列                                   | P3 单独立项                                                   |
| Driver 进程级 dedup              | 已知约束 | driver 重启丢去重窗口                                | 属于设计取舍，生产可接受                                              |
| 命名切换对生产环境的冲击                  | 已解决  | Phase 1 已发布，旧资源已删除                            | —                                                         |
| sealed interface + record 兼容性 | 新风险  | Jackson 2.x 对 Java record 的 parameter name 支持 | 落地前验证 `ParameterNamesModule`；不通过则降级为普通类 + `@JsonTypeInfo` |

---

## 10. 决策项

1. **命名** `PointCommand` — 已确认，Phase 1 已完成。
2. **同步等回执范围** — 待决：P1 优先采用简易方案（202 + 轮询），多实例广播延后。
3. **DLX 重放工具** — 待决：P3 单独立项。
4. **WRITE 值范围校验位置** — 已确认：中心侧（统一）+ driver 侧（贴协议）两层兜底。
5. **`schema_version`** — 已确认：进库，`dc3_point_command` 表已包含 `schema_version SMALLINT` 列。

---

## 11. 引用

- 物模型设计：[docs/design/thing-model.md](thing-model.md)
- 当前代码定位（Phase 1 后的命名）：
    - `dc3-common-data/.../controller/PointCommandController.java`
    - `dc3-common-data/.../biz/impl/PointCommandServiceImpl.java`
    - `dc3-common-data/.../receiver/rabbit/PointCommandResultReceiver.java`
    - `dc3-common-data/.../receiver/rabbit/PointCommandDeadReceiver.java`
    - `dc3-common-model/.../dto/PointCommandDTO.java`
    - `dc3-common-model/.../dto/PointCommandResultDTO.java`
    - `dc3-common-driver/.../receiver/rabbit/PointCommandReceiver.java`
    - `dc3-common-driver/.../service/impl/DriverWriteServiceImpl.java`
    - `dc3-common-driver/.../service/impl/DriverReadServiceImpl.java`
    - `dc3-common-driver/.../cache/CommandDedupCache.java`
    - `dc3-common-driver/.../config/DriverTopicConfig.java`
    - `dc3-common-rabbitmq/.../config/RabbitConfig.java`
    - `dc3-common-constant/.../enums/PointCommandTypeEnum.java`
    - `dc3-common-constant/.../enums/PointCommandStatusEnum.java`
    - `dc3-common-constant/.../enums/PointCommandSourceEnum.java`
    - `dc3-common-data/.../entity/model/PointCommandDO.java`
