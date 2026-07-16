---
title: 日志规范
---

<script setup>
import LoggingDiagram from '../../.vitepress/theme/components/LoggingDiagram.vue'
</script>


# 日志规范

IoT DC3 的日志要同时服务两个读者：本地开发时的人，和线上排障时的机器。这页讲清两者怎么兼顾——结构化消息、MDC
上下文、级别约定、脱敏红线，以及容器日志怎么轮转，让你写出"能被搜索、不会泄密"的日志。

> 你在这里：写业务代码或排查线上问题，想知道日志该怎么打、去哪儿看。配套阅读 [可观测性](./observability)
> 与 [故障排查](./troubleshooting)。

## 为什么这样设计

一条日志的价值不在它被打出来的那一刻，而在三天后有人 `grep` 它的时候。微服务下，一次设备命令会横跨网关、数据中心、驱动多个进程，日志散落在不同容器里——如果每条消息措辞各异、关键
ID 缺失、上下文不可关联，排障就退化成大海捞针。

所以 IoT DC3 把日志拆成两层职责：

- **应用代码**只负责写出**稳定的事件名 + 结构化参数**——同一件事在所有模块用同样的措辞和参数顺序，跨进程才能拼成一条链路。
- **`dc3-common-log` 的 Appender**负责最终格式——本地输出彩色控制台便于人读，文件输出 JSON 便于机器解析与采集。

业务代码绝不为某种输出格式硬编码内容。这样换采集方案（ELK、Loki…）时，改的是 Appender，不是几百处 `log.info`。

## 日志怎么流动

下图是一条日志从代码到落盘/采集的完整路径：应用写出事件，经 MDC 上下文槽位，再由两个 Appender 分别格式化。

<LoggingDiagram lang="zh" />

两个 Appender 都挂在 `root`（默认级别 `INFO`），由 `dc3-common-log` 的 `logback.xml` 配置。JSON Appender 用
`net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder`，逐字段输出 `timestamp`、`version`、`message`、`loggerName`、
`threadName`、`logLevel`、`logLevelValue`、**`mdc`**、`contextName`、`stackTrace`——其中 `mdc` 这一项，是下一节要讲的链路关联的预留槽位。

## MDC：预留的链路上下文槽位

MDC（Mapped Diagnostic Context）是 SLF4J 的线程级上下文：往 MDC 里放的键值，会随本线程后续每一条日志自动渲染进输出。
`dc3-common-log` 的 `logback.xml` 已在 JSON encoder 里挂上 `<mdc/>` provider，为这套能力预留了输出槽位——任何被放进 MDC
的字段都会逐条出现在 JSON 的 `mdc` 项里。

MDC 的目标用法是在请求入口放入 `traceId`、`tenantId`、`userId`，从而支撑两个能力：

- **跨服务关联**：同一个 `traceId` 贯穿网关 → 数据中心 → 驱动，按 `traceId` 一搜即可聚齐整条调用链。
- **租户归因**：每条日志带 `tenantId`，直接回答"是哪个租户触发的"——与平台的[租户隔离](../architecture/auth-rbac)边界一致。

::: info MDC 自动注入尚未接线
encoder 的 `<mdc/>` provider 已就位，但当前代码库**没有**任何过滤器/拦截器/AOP 把 `traceId`/`tenantId`/`userId` 写入
MDC（全仓无 `MDC.put`），所以现阶段 JSON 输出里的 `mdc` 项实际为空。上面描述的链路关联是**已规划、尚未实现**
的能力。在它接线之前，需要关联的字段请按下一节作为消息参数显式传入（如 `tenantId`、`deviceId`）。
:::

## 写出结构化的消息

日志正文用英文、稳定事件名和 SLF4J 参数化占位符 `{}`。占位符让消息模板保持恒定（便于 `grep` 与日志聚合按模板归类），变量作为参数传入：

```java
log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId);
log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId, e);
```

参数适用时按下面的顺序组织，保证同类事件在不同模块长得一样：

```text
module/action, tenantId, userId, resource IDs, filters, status/result, durationMs
```

示例：

```java
log.info("Device registered, tenantId={}, deviceId={}, driverId={}", tenantId, deviceId, driverId);
log.debug("Agentic chat request received, mode={}, model={}, messageCount={}, conversationIdPresent={}, skill={}, tenantId={}, userId={}",
        mode, model, messageCount, conversationIdPresent, skill, tenantId, userId);
```

::: warning 不要字符串拼接，不要丢堆栈
避免 `+` 拼接和 `String.format`——它们破坏消息模板、且无论级别是否输出都会先求值。捕获异常后，除非明确要隐藏堆栈，否则把异常对象作为
**最后一个参数**传入（而非 `e.getMessage()`），SLF4J 会自动渲染完整堆栈：

```java
// ✅ 模板稳定 + 完整堆栈
log.warn("Point read command failed, tenantId={}, deviceId={}, pointId={}", tenantId, deviceId, pointId, e);

// ❌ 拼接 + 丢失堆栈
log.info("Device registered: " + deviceId);
log.error("Failed to register device: {}", e.getMessage());
```

:::

### 声明式方法日志：`@Logs`

`dc3-common-log` 提供 `@Logs` 注解（由 `LogsAspect` 这个 Spring AOP 切面拦截），可在方法上声明式地记录一条日志，免去手写。注解成员为
`value`（日志消息）、`type`（`LogsTypeEnum`，取值 `INFO`/`WARN`/`DEBUG`/`ERROR`，默认 `INFO`）、`tag`（分类标签）、`save`（是否持久化，默认
`false`）：

```java
@Logs(value = "warn-resource", type = LogsTypeEnum.WARN, tag = "resource", save = true)
public void someMethod() {
    // 方法体
}
```

::: info 当前仅测试用例使用
`@Logs` 切面已实现，但平台生产代码暂未在任何 Controller/Service 上使用它（仅 `LogsAspectTest`
覆盖）。它作为可选的声明式日志能力存在，业务日志现行约定仍以本页前述的 SLF4J 参数化写法为主。
:::

## 日志级别约定

级别不是随手选的，它决定了线上默认输出量和告警噪声。`root` 默认 `INFO`，意味着 `trace`/`debug`
默认不落盘——把信息放对级别，排障时才能"该有的有、该静的静"。下表给出约定，先理解每一档的判断标准，再对照使用：

| 级别      | 判断标准（什么时候用）                            |
|---------|----------------------------------------|
| `trace` | 高频诊断细节，默认关闭，只在深挖单点问题时临时打开              |
| `debug` | 请求细节、工具调用、查询条件、排障时有用的分支判断；默认不输出        |
| `info`  | 生命周期事件、启动摘要、长任务成功摘要、重要状态转换——线上稳态下应当能看到 |
| `warn`  | 可恢复失败、非法客户端输入、重试、降级、外部依赖异常但已有兜底        |
| `error` | 不可恢复、需要运维关注、或导致当前操作失败的错误               |

判断要点：**当前操作是否失败、是否需要人介入**——失败且无兜底用 `error`，失败但已降级/重试用 `warn`，正常流程的关键节点用
`info`，其余诊断信息压到 `debug`。第三方框架噪声已在 `logback.xml` 里逐包压到 `WARN`（如 `org.springframework.*`、
`com.zaxxer.hikari`、MyBatis 等），不要在业务里把它们重新放大。

## 脱敏：密钥与隐私绝不明文

::: danger 密钥、token、密码绝不明文落日志
不要在任何级别记录密钥、Bearer token、密码、完整 `Authorization` 头、原始私有载荷或任意请求体。一旦写进日志，它就进了文件、进了采集系统、进了备份——撤不回来。

需要佐证时只记录**派生信息**：是否存在、长度、前几位 + 长度、或资源 ID。例如校验失败时记 `tokenPrefix=eyJ0..., tokenLen=212`
，而不是整段 token。
:::

这条红线对两个高风险字段尤其关键，它们的明文一旦泄露等于整个鉴权链失守（参见 [env 目录](../quickstart/environment)）：

- `DC3_SECURITY_KEY` — Auth Center 的 Token 签名密钥。
- `AUTH_HMAC_SECRET` — 网关到后端签发 `X-Auth-Principal` 的 HMAC-SHA256 密钥。

对"可能敏感"的命令值（如写位号下发的 `value`），同样只记录长度/是否存在/资源 ID 等派生信息，不记原值：

```java
// ✅ 只留派生信息
log.info("Point write accepted, tenantId={}, deviceId={}, pointId={}, valueLen={}", tenantId, deviceId, pointId, value.length());

// ❌ 把原始命令值/凭证打进日志
log.info("Point write, value={}, token={}", value, token);
```

## 容器日志轮转

应用内的 `logback.xml` 已自带文件滚动（`SizeAndTimeBasedRollingPolicy`，应用默认单文件 200MB、总量上限 20GB、保留 30
个历史文件、按天 `.gz` 归档）。但在容器部署下，进程的 `stdout`/`stderr` 由容器运行时接管，需要在 Compose
层另行限制磁盘占用——否则一个长跑容器的日志能把宿主机磁盘写满。

`dc3` 的 Compose 文件用一个共享的 `x-logging` 锚点，给每个应用服务统一挂上 Docker `json-file` 驱动的轮转策略：

```yaml
# dc3/docker-compose-dev.yml
x-logging: &default-logging
  driver: json-file
  options:
    max-size: ${DC3_LOG_MAX_SIZE:-10M}   # 单个容器日志文件达到此大小即轮转
    max-file: "${DC3_LOG_MAX_FILE:-20}"  # 保留的轮转文件数
```

两个开关在根 `.env`（Compose-only，不注入本地 Java 进程）里调整：

| 变量                 | 默认值   | 作用           |
|--------------------|-------|--------------|
| `DC3_LOG_MAX_SIZE` | `10M` | 单个容器日志文件轮转阈值 |
| `DC3_LOG_MAX_FILE` | `20`  | 保留的轮转日志文件数   |

按默认值，每个容器最多占用约 `10M × 20 = 200M` 磁盘。查看与跟随容器日志：

::: code-group

```bash [podman]
podman logs -f --tail 200 dc3-center-data
```

```bash [make]
# 从 iot-dc3/ 执行，跟随当前栈最近 200 行
make logs
```

:::

::: info 应用内轮转 vs 容器轮转
两套轮转独立生效：`logback.xml` 管的是容器内 `LOG_FILE` 写出的滚动文件（默认在临时目录，体量较大）；`DC3_LOG_MAX_SIZE`/
`DC3_LOG_MAX_FILE` 管的是容器运行时捕获的 `stdout`/`stderr`。生产采集通常以后者（`json-file`）为采集源，前者作为容器内的二级保留。
:::

## 延伸阅读

- [可观测性](./observability) — 日志、指标、追踪如何协同，以及 ELK/Grafana 栈怎么起
- [故障排查](./troubleshooting) — 拿到 `traceId`/`tenantId` 后如何定位一次失败
- [env 目录](../quickstart/environment) — `DC3_LOG_*` 与 `DC3_SECURITY_KEY`/`AUTH_HMAC_SECRET` 等变量的来源与边界
