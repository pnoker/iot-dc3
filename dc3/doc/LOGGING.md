# Logging Guidelines

This project uses human-readable console logs and structured JSON file logs. Application log messages should follow the
same wording and argument style across modules so operators can search, filter, and correlate events reliably.

## Message Style

Use English, parameterized SLF4J messages, and stable event names:

```java
log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId);
log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}", toolName, tenantId, deviceId, e);
```

Avoid string concatenation and `String.format` in log statements:

```java
log.info("Device registered, tenantId={}, deviceId={}, driverId={}", tenantId, deviceId, driverId);
```

Do not write logs like this:

```java
log.info("Device registered: " + deviceId);
log.error("Failed to register device: {}", e.getMessage());
```

## Levels

- `trace`: very high-frequency diagnostics, disabled by default.
- `debug`: request details, tool invocation, query filters, and branch decisions useful during troubleshooting.
- `info`: lifecycle events, startup summaries, successful long-running job summaries, and important state transitions.
- `warn`: recoverable failures, invalid client input, retries, degraded behavior, or external dependency failures handled by fallback behavior.
- `error`: unrecoverable failures that require operator attention or break the current operation.

When logging a caught exception, pass the exception object unless the stack trace is intentionally suppressed:

```java
log.warn("Point read command failed, tenantId={}, deviceId={}, pointId={}", tenantId, deviceId, pointId, e);
```

## Context Fields

Prefer key-value arguments in this order when they apply:

```text
module/action, tenantId, userId, resource IDs, filters, status/result, durationMs
```

Examples:

```java
log.info("Agentic skills loaded, count={}, skills={}", loaded.size(), loaded);
log.debug("Agentic chat request received, mode={}, model={}, messageCount={}, conversationIdPresent={}, skill={}, tenantId={}, userId={}",
        mode, model, messageCount, conversationIdPresent, skill, tenantId, userId);
```

Do not log secrets, bearer tokens, passwords, full authorization headers, raw private payloads, or arbitrary request
bodies at `info`. For potentially sensitive command values, log derived metadata such as length or presence instead of
the raw value.

## Format

`dc3-common-log` configures:

- console logs with Spring Boot's colored pattern for local debugging;
- rolling file logs with JSON fields for machine parsing, including timestamp, logger, thread, level, MDC, message, and
  stack trace.

Keep this split intentional: code should standardize message wording and structured placeholders, while appenders decide
the physical output format.
