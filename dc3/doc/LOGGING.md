# Logging Standard

IoT DC3 uses one logging contract across gateway, center services, shared modules and protocol drivers. Backend
applications inherit `dc3-common-log`; the web application uses `src/utils/log.ts`.

## Runtime format

- Console logs include the application name, process ID, thread, source, `requestId`, `traceId` and `spanId`.
- Rolling files use JSON and include the same MDC context, service name, process ID, exception stack and structured
  key-value pairs.
- Console and file writes use lossless asynchronous queues. When a queue is saturated, the producer applies
  backpressure instead of silently discarding events.
- `DC3_LOG_LEVEL` controls the root level. File rotation remains configurable through the standard Logback rolling
  policy environment properties.

## Message contract

Every application log must:

1. Use an English, stable event description as a string literal.
2. Use SLF4J placeholders instead of concatenation or `String.format`.
3. Put searchable context after the description as lower-camel-case `key={}` fields.
4. Pass a caught exception as the final argument when its stack is operationally useful.
5. Prefer identifiers, counts, lengths, states and reason codes over serialized domain objects.

Example:

```java
log.warn("Point command rejected, reason=expired, commandId={}, expireAt={}", commandId, expireAt);
log.error("Metadata event handling failed, id={}, type={}, operation={}", id, type, operation, exception);
```

## Levels

- `ERROR`: an operation failed and needs intervention, retry or durable failure handling.
- `WARN`: a recoverable degradation, rejected input or suspicious state.
- `INFO`: a low-frequency lifecycle or business state transition. Never use it for per-point telemetry.
- `DEBUG`: diagnostic metadata such as identifiers and counts.
- `TRACE`: exceptional deep diagnostics only; raw payloads and credentials remain forbidden.

## Security and volume

Never log passwords, secrets, tokens, authorization or principal headers, raw commands, message bodies, telemetry
payloads, notification content or complete request/response objects. Log safe metadata such as `payloadLength`,
`messageId`, `deviceId`, `pointId`, `topic`, `routingKey` and item counts instead.

High-frequency paths must aggregate or sample at `DEBUG`; normal point ingestion must not produce one `INFO` line per
value. Broker failures should include exchange/routing metadata and body length, never the body itself.

## Enforcement

Run the complete logging gates with:

```bash
make validate-logging
```

The backend gate scans every production Java module and rejects console printing, dynamic/non-English messages,
message concatenation, serialized domain objects and raw sensitive fields. The frontend guard rejects direct
`console.*` calls outside the unified logger. Both gates also run as part of the normal backend/frontend test suites.
