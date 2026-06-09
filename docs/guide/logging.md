# 日志规范

IoT DC3 使用便于人工阅读的控制台日志，以及便于机器解析的 JSON 文件日志。应用代码中的日志消息应在各模块保持一致的措辞、参数顺序和事件命名，方便运维人员搜索、过滤和关联问题。

## 消息风格

日志正文使用英文、稳定事件名和 SLF4J 参数化占位符：

```java
log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId);
log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId, e);
```

避免字符串拼接和 `String.format`：

```java
log.info("Device registered, tenantId={}, deviceId={}, driverId={}", tenantId, deviceId, driverId);
```

不要写成：

```java
log.info("Device registered: " + deviceId);
log.error("Failed to register device: {}", e.getMessage());
```

## 日志级别

| 级别 | 使用场景 |
|------|----------|
| `trace` | 高频诊断信息，默认关闭 |
| `debug` | 请求细节、工具调用、查询条件、排障时有用的分支判断 |
| `info` | 生命周期事件、启动摘要、长任务成功摘要、重要状态转换 |
| `warn` | 可恢复失败、非法客户端输入、重试、降级行为、外部依赖异常且已有兜底 |
| `error` | 需要运维关注或导致当前操作失败的不可恢复错误 |

捕获异常后记录日志时，除非明确要隐藏堆栈，否则应把异常对象作为最后一个参数传入：

```java
log.warn("Point read command failed, tenantId={}, deviceId={}, pointId={}", tenantId, deviceId, pointId, e);
```

## 上下文字段

适用时优先按以下顺序组织参数：

```text
module/action, tenantId, userId, resource IDs, filters, status/result, durationMs
```

示例：

```java
log.info("Agentic skills loaded, count={}, skills={}", loaded.size(), loaded);
log.debug("Agentic chat request received, mode={}, model={}, messageCount={}, conversationIdPresent={}, skill={}, tenantId={}, userId={}",
        mode, model, messageCount, conversationIdPresent, skill, tenantId, userId);
```

不要在 `info` 级别记录密钥、Bearer token、密码、完整 Authorization 头、原始私有载荷或任意请求体。对可能敏感的命令值，只记录长度、是否存在、资源 ID 等派生信息。

## 输出格式

`dc3-common-log` 配置了两类输出：

| 输出 | 用途 |
|------|------|
| 控制台彩色日志 | 本地开发和人工调试 |
| 滚动 JSON 文件日志 | 机器解析，包含 timestamp、logger、thread、level、MDC、message 和 stack trace |

代码负责统一消息措辞和结构化占位符，Appender 负责最终输出格式。不要在业务代码里为某一种输出格式硬编码日志内容。
