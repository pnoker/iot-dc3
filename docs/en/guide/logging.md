---
title: Logging Conventions
---

<script setup>
import LoggingDiagram from '../../.vitepress/theme/components/LoggingDiagram.vue'
</script>


# Logging Conventions

IoT DC3 logs are read by two audiences: you, during local development, and machines, during production troubleshooting.
This page covers structured messages, MDC context, level conventions, the redaction hard line, and how container logs
rotate — so your logs are searchable and never leak secrets.

> You are here: writing business code or chasing a production issue, and want to know what to log and where to find it.
> Read alongside [Observability](./observability) and [Troubleshooting](./troubleshooting).

## Why it is designed this way

A log line earns its keep not when it is written, but three days later when someone greps for it. In a microservice
deployment a single device command crosses the gateway, the data center, and a driver across several processes, with
logs scattered across containers. If every message phrases things differently, omits key IDs, and cannot be correlated,
troubleshooting turns into a needle-in-a-haystack search.

That is why IoT DC3 splits logging into two layers of responsibility:

- **Application code** emits **stable event names + structured parameters**. The same event uses the same phrasing and
  parameter order across every module, so a cross-process chain can be reassembled.
- **The Appender in `dc3-common-log`** owns the final format — colored console output locally for humans, JSON file
  output for machines to parse and collect.

Business code never hardcodes content for a particular output format. So when you change the collection stack (ELK,
Loki, ...), you change the Appender, not hundreds of `log.info` calls.

## How a log line flows

The diagram below traces the full path from code to a persisted or collected log: the application emits an event, it
passes through MDC context slots, and is then formatted by two separate Appenders.

<LoggingDiagram lang="en" />

Both Appenders attach to `root` (default level `INFO`) and are configured by the `logback.xml` in `dc3-common-log`. The
JSON Appender uses `net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder`, emitting `timestamp`, `version`,
`message`, `loggerName`, `threadName`, `logLevel`, `logLevelValue`, **`mdc`**, `contextName`, `stackTrace` field by
field. The `mdc` entry is the reserved slot for the trace correlation described in the next section.

## MDC: the reserved trace-context slot

MDC (Mapped Diagnostic Context) is SLF4J's thread-local context: key-value pairs placed in MDC are rendered into every
subsequent log line on that thread. The `logback.xml` in `dc3-common-log` already mounts the `<mdc/>` provider in the
JSON encoder, reserving an output slot for this — any field placed in MDC will appear per line in the JSON `mdc` entry.

The intended use of MDC is to populate `traceId`, `tenantId`, and `userId` at request entry, which gives you two things:

- **Cross-service correlation**: a single `traceId` threads through gateway → data center → driver. Search by `traceId`
  and the entire call chain comes back together.
- **Tenant attribution**: every log line carries `tenantId`, so you can answer "which tenant triggered this" directly —
  consistent with the platform's [tenant isolation](../architecture/auth-rbac) boundary.

::: info MDC auto-injection is not yet wired
The encoder's `<mdc/>` provider is in place, but the codebase currently has **no** filter, interceptor, or AOP aspect
writing `traceId`/`tenantId`/`userId` into MDC (no `MDC.put` anywhere in the repo), so the `mdc` entry in today's JSON
output is effectively empty. The trace correlation described above is a **planned, not-yet-implemented** capability.
Until it is wired up, pass any field you need to correlate on explicitly as a message parameter per the next section (
e.g. `tenantId`, `deviceId`).
:::

## Writing structured messages

Log bodies use English, stable event names, and SLF4J parameterized placeholders `{}`. Placeholders keep the message
template constant (so `grep` and log aggregation can group by template), with variables passed as arguments:

```java
log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId);
log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId, e);
```

Where applicable, order parameters as below so events of the same kind look identical across modules:

```text
module/action, tenantId, userId, resource IDs, filters, status/result, durationMs
```

Examples:

```java
log.info("Device registered, tenantId={}, deviceId={}, driverId={}", tenantId, deviceId, driverId);
log.debug("Agentic chat request received, mode={}, model={}, messageCount={}, conversationIdPresent={}, skill={}, tenantId={}, userId={}",
        mode, model, messageCount, conversationIdPresent, skill, tenantId, userId);
```

::: warning Do not concatenate strings, do not drop the stack trace
Avoid `+` concatenation and `String.format` — they break the message template and are evaluated whether or not the level
is enabled. After catching an exception, unless you mean to hide the stack trace, pass the exception object as the *
*last argument** (not `e.getMessage()`); SLF4J renders the full stack trace automatically:

```java
// ✅ Stable template + full stack trace
log.warn("Point read command failed, tenantId={}, deviceId={}, pointId={}", tenantId, deviceId, pointId, e);

// ❌ Concatenation + lost stack trace
log.info("Device registered: " + deviceId);
log.error("Failed to register device: {}", e.getMessage());
```

:::

### Declarative method logging: `@Logs`

`dc3-common-log` provides the `@Logs` annotation (intercepted by the `LogsAspect` Spring AOP aspect) for declarative
method-level logging without the boilerplate. The annotation members are `value` (the log message), `type` (
`LogsTypeEnum`, one of `INFO`/`WARN`/`DEBUG`/`ERROR`, default `INFO`), `tag` (a classification tag), and `save` (whether
to persist, default `false`):

```java
@Logs(value = "warn-resource", type = LogsTypeEnum.WARN, tag = "resource", save = true)
public void someMethod() {
    // method body
}
```

::: info Currently used only by tests
The `@Logs` aspect is implemented, but production code does not yet use it on any Controller or Service (only
`LogsAspectTest` covers it). It exists as an optional declarative logging capability; for business logging the
convention remains the SLF4J parameterized style described earlier on this page.
:::

## Log level conventions

Levels are not chosen at random — they set default production output volume and alert noise. The `root` default is
`INFO`, which means `trace`/`debug` are not persisted by default. Put information at the right level so that during
troubleshooting you have what you need and silence the rest. The table below gives the conventions; learn the criteria
for each tier first, then apply them:

| Level   | Criteria (when to use)                                                                                                                         |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `trace` | High-frequency diagnostic detail, off by default, opened temporarily only when drilling into a single issue                                    |
| `debug` | Request details, tool invocations, query conditions, branch decisions useful during troubleshooting; not emitted by default                    |
| `info`  | Lifecycle events, startup summaries, long-task success summaries, significant state transitions — should be visible in steady-state production |
| `warn`  | Recoverable failures, illegal client input, retries, degradation, external-dependency anomalies that already have a fallback                   |
| `error` | Unrecoverable errors, errors requiring operator attention, or errors that cause the current operation to fail                                  |

The key question: **did the current operation fail, and does it need human intervention?** Failure without a fallback is
`error`; failure that is degraded or retried is `warn`; key checkpoints in the normal flow are `info`; the rest of the
diagnostic detail goes to `debug`. Third-party framework noise is already throttled to `WARN` per package in
`logback.xml` (e.g. `org.springframework.*`, `com.zaxxer.hikari`, MyBatis); do not turn them back up in business code.

## Redaction: keys and secrets are never written in the clear

::: danger Keys, tokens, and passwords are never logged in the clear
Never log keys, Bearer tokens, passwords, the full `Authorization` header, raw private payloads, or arbitrary request
bodies at any level. Once it is written to a log, it is in the file, in the collection system, in the backups — and you
cannot take it back.

When you need evidence, log only **derived information**: presence, length, a few leading characters + length, or a
resource ID. On a validation failure, for example, log `tokenPrefix=eyJ0..., tokenLen=212`, not the entire token.
:::

This hard line is most critical for two high-risk fields — leaking their plaintext is equivalent to compromising the
entire authentication chain (see [the env directory](../quickstart/environment)):

- `DC3_SECURITY_KEY` — the Token signing key for Auth Center.
- `AUTH_HMAC_SECRET` — the HMAC-SHA256 secret the gateway uses to sign `X-Auth-Principal` for the backend.

For command values that may be sensitive (such as the `value` written to a point), log only derived information —
length, presence, or a resource ID — never the raw value:

```java
// ✅ Keep only derived information
log.info("Point write accepted, tenantId={}, deviceId={}, pointId={}, valueLen={}", tenantId, deviceId, pointId, value.length());

// ❌ Logging the raw command value or credentials
log.info("Point write, value={}, token={}", value, token);
```

## Container log rotation

The in-app `logback.xml` already provides file rolling (`SizeAndTimeBasedRollingPolicy`: default 200MB per file, 20GB
total cap, 30 historical files retained, archived daily as `.gz`). Under container deployment, though, the process's
`stdout`/`stderr` are owned by the container runtime, so disk usage must be bounded separately at the Compose layer —
otherwise a long-running container's logs can fill the host disk.

The `dc3` Compose files use a shared `x-logging` anchor to attach a uniform Docker `json-file` driver rotation policy to
every application service:

```yaml
# dc3/docker-compose-dev.yml
x-logging: &default-logging
  driver: json-file
  options:
    max-size: ${DC3_LOG_MAX_SIZE:-10M}   # rotate a container log file once it reaches this size
    max-file: "${DC3_LOG_MAX_FILE:-20}"  # number of rotated files to retain
```

The two knobs are tuned in the root `.env` (Compose-only, not injected into the local Java process):

| Variable           | Default | Purpose                                            |
|--------------------|---------|----------------------------------------------------|
| `DC3_LOG_MAX_SIZE` | `10M`   | Rotation threshold for a single container log file |
| `DC3_LOG_MAX_FILE` | `20`    | Number of rotated log files to retain              |

At the defaults, each container uses at most about `10M × 20 = 200M` of disk. To view and follow container logs:

::: code-group

```bash [podman]
podman logs -f --tail 200 dc3-center-data
```

```bash [make]
# Run from iot-dc3/, follows the last 200 lines of the current stack
make logs
```

:::

::: info In-app rotation vs container rotation
The two rotation mechanisms are independent. `logback.xml` governs the rolling files written to `LOG_FILE` inside the
container (by default in a temp directory, and larger); `DC3_LOG_MAX_SIZE`/`DC3_LOG_MAX_FILE` governs the `stdout`/
`stderr` captured by the container runtime. Production collection usually sources from the latter (`json-file`), with
the former serving as secondary in-container retention.
:::

## Further reading

- [Observability](./observability) — how logs, metrics, and traces work together, and how to bring up the ELK/Grafana
  stack
- [Troubleshooting](./troubleshooting) — once you have a `traceId`/`tenantId`, how to localize a failure
- [The env directory](../quickstart/environment) — the source and boundary of variables like `DC3_LOG_*`,
  `DC3_SECURITY_KEY`, and `AUTH_HMAC_SECRET`
