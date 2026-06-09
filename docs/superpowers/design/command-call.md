---
title: 自定义指令调用方案
---

# 自定义指令调用方案

本文描述 DC3 自定义指令从 API 下发到 Driver 执行再到结果回执的完整链路设计。

> **背景**：Phase 2 完成了 `dc3_command` / `dc3_command_param` 表结构定义，Phase 4-5 补齐了运行时调用链路。

---

## 与 PointCommand 的区别

| 概念   | PointCommand                        | Command (自定义指令)                           |
|------|-------------------------------------|-------------------------------------------|
| 触发方式 | 点位读写 (READ/WRITE)                   | 自定义服务调用 (RESTART, CALIBRATE 等)            |
| 定义归属 | `dc3_point` 的 `rw_flag`             | `dc3_command` 独立表                         |
| 驱动接口 | `DriverProtocol.read()` / `write()` | `DriverCommand.execute()`                 |
| 参数模型 | 单值读写                                | 结构化输入/输出参数 Map                            |
| DTO  | `PointCommandDTO`                   | `CommandCallDTO` / `CommandCallResultDTO` |

---

## CommandCall 生命周期

```
PENDING → SENT → SUCCESS / FAILED / TIMEOUT / EXPIRED / DUPLICATE
```

| 状态          | 说明                         |
|-------------|----------------------------|
| `PENDING`   | 已创建调用记录，等待投递               |
| `SENT`      | 已投递到 RabbitMQ，等待 Driver 执行 |
| `SUCCESS`   | Driver 执行成功，结果已回写          |
| `FAILED`    | Driver 执行失败，错误信息已回写        |
| `TIMEOUT`   | 超时未收到回执                    |
| `EXPIRED`   | 消息 TTL 过期，经由 DLX 回收        |
| `DUPLICATE` | 重复的消息被去重拦截                 |

---

## RabbitMQ 拓扑

```
dc3.e.command (topic exchange)
  ├── dc3.q.command.{service} (TTL 30s, DLX → dc3.e.command_dead)
  └── dc3.e.command_dead (topic exchange)
        └── dc3.q.command_dead

dc3.e.command_result (topic exchange)
  └── dc3.q.command_result (TTL 60s)
```

**路由键**：

- 指令下发：`dc3.r.command.{service}` (service = driver service name)
- 死信路由：`#` (所有死信消息)
- 结果回执：`dc3.r.command_result.{service}`

**消息流**：

1. Data Center 通过 `CommandRecordService.call()` 持久化 `CommandRecordDO` (PENDING)
2. 投递 `CommandCallDTO` 到 `dc3.e.command`，routing key = `dc3.r.command.{driverService}`
3. 更新状态为 SENT
4. Driver 侧 `CommandReceiver` 消费 → 去重校验 → 过期检查 → 设备锁 → 执行
5. 执行完成后 Driver 发布 `CommandCallResultDTO` 到 `dc3.e.command_result`
6. Data Center 的 `CommandResultReceiver` 消费 → 更新 `CommandRecordDO` 状态
7. 若消息 TTL 过期未被消费，自动路由到 `dc3.q.command_dead` → Data Center 更新状态为 EXPIRED

---

## API 路径

| 方法   | 路径                                | 说明         |
|------|-----------------------------------|------------|
| POST | `/data/command_record/call`       | 下发自定义指令    |
| GET  | `/data/command_record/{recordId}` | 查询调用记录详情   |
| POST | `/data/command_record/list`       | 分页查询调用记录列表 |

gRPC API 路径（Data Center）：

- `DataService.CommandCall` — 下发自定义指令
- `DataService.CommandRecord` — 查询调用记录
- `DataService.CommandRecordList` — 分页查询

---

## Driver SDK 扩展点

### DriverCommand 接口

```java
public interface DriverCommand {
    default Map<String, String> execute(
            Map<String, AttributeBO> driverConfig,
            DeviceBO device,
            FacadeCommandBO command,
            Map<String, String> paramValues) {
        return Collections.emptyMap();
    }
}
```

- `DriverCustomService` 扩展 `DriverCommand`，所有驱动自动继承默认实现（返回空 map）
- 各协议驱动可按需覆盖 `execute()` 方法，实现协议特定的指令逻辑

### DriverSenderService

- `commandResultSender(CommandCallResultDTO)` — 发送指令执行回执
- 自动从 `DriverMetadata` 填充 `tenantId`

---

## 去重与可靠性

- **去重**：`CommandDedupCache` (Caffeine) 基于 `recordId` 去重，防止重复消费
- **设备锁**：`DeviceLockManager.runExclusive(deviceId)` 保证同一设备的指令串行执行
- **过期保护**：队列 TTL 30s + DLX，过期消息自动进入死信队列
- **重试**：非 redelivered 异常 → nack + requeue；redelivered 异常 → FAILED 结果 + ack（避免死循环）
