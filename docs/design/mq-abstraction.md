# Design: Pluggable Message Broker Abstraction (MQ Port)

|                |                                                                                   |
|----------------|-----------------------------------------------------------------------------------|
| **Status**     | Phases 1–3 delivered: port + rabbitmq / kafka / activemq / mqtt / rocketmq adapters TCK-certified; pulsar pending |
| **Date**       | 2026-08-17, revised 2026-08-19 (re-verified after commit `956de3dd3`; MQTT decoupling) |
| **Scope**      | `dc3-common-*` messaging layer (center ↔ driver async plane)                       |
| **Target**     | RabbitMQ (default), Kafka, RocketMQ, Pulsar, ActiveMQ (Artemis / Classic), MQTT 5 (EMQX / HiveMQ / NanoMQ / …) |
| **Related**    | [`storage-abstraction.md`](./storage-abstraction.md) — relational + time-series pluggability, same profile/TCK mechanism |
| **Discussion** | open for review before Phase 1 starts                                             |

## 1. Summary

IoT DC3 currently talks to RabbitMQ through its native Spring AMQP API (`RabbitTemplate`,
`@RabbitListener`, `Channel`, `CorrelationData`) from three business modules. As a globally
distributed open-source project we want deployers — and the community — to be able to run the
platform on whichever broker their environment already standardizes on.

The proposal: introduce a thin **port** (`dc3-common-mq`) that abstracts the *semantics* the
platform actually uses — logical topics, load-balanced vs broadcast subscription, delayed
delivery, dead-lettering, batch consumption with transactional acknowledgement, bounded
redelivery, publisher confirmation — plus one adapter module per broker. RabbitMQ comes first
as a byte-for-byte wire-compatible migration of the existing code; a broker-neutral
contract-test suite (TCK) makes community adapters for other brokers possible with a clear
acceptance bar. One of the adapters is **MQTT 5**, so the internal plane can also run over
whichever MQTT broker the deployment already operates.

The design deliberately does **not** try to model broker topology (exchanges, bindings,
partitions). It models the two subscription modes every mainstream broker can express, and
negotiates everything else through declared capabilities with documented fallbacks.

MQTT plays two distinct roles in the platform and both must treat the broker as a free
selection: the **device-access plane** (vendor-neutral by protocol already) and — via the
new adapter — the **internal async plane** itself. §7.1 draws the boundary.

Two recent platform changes (2026-08-18, commit `956de3dd3` — lease-fenced durable telemetry)
strengthen rather than invalidate this design: the driver now owns durability through a
mandatory SQLite outbox in front of the broker, and point-value ingestion is transactional
and idempotent (schema-versioned payloads with `messageId` dedupe and fencing tokens). Both
reduce how much the port has to demand from the broker.

## 2. Background — how RabbitMQ is used today

Accurate as of 2026-08-19, re-verified after commit `956de3dd3` (2026-08-18). All numbers
verified against the tree.

**Topology** (names centralized in
`dc3-common/dc3-common-constant/.../driver/RabbitConstant.java`):

- **15 declared topic exchanges** (`dc3.e.<domain>`): state, alarm, metadata, point_command,
  value, mqtt, state_timeout_delay, state_timeout_check, command, command_result, command_dead,
  event (declared in `dc3-common-rabbitmq/.../ExchangeConfig.java`) plus point_value_dead,
  point_command_dead, point_command_result (declared in
  `dc3-common-data/.../DataTopicConfig.java`). Two of them are dead weight: `dc3.e.mqtt` is
  declared but has no binding, producer, or consumer, and the `register` exchange/queue
  constants in `RabbitConstant` are declared and referenced nowhere — both are Phase 1
  cleanup candidates (§12; routing through an MQTT broker belongs to the §7.1 adapter,
  not a bespoke exchange).
- **19 declared queue definitions** — 16 center-side (load-balanced and dead-letter) in
  `DataTopicConfig` and 3 driver-side patterns in `dc3-common-driver/.../DriverTopicConfig.java`,
  the latter instantiated per driver client. The driver-side command queues carry a
  lease-coupled `x-expires` (§8.8); the metadata queue is auto-delete with 30 s TTL.
- Routing keys `dc3.r.<domain>.<service>`; environment prefix via the `dc3.rabbit.tag`
  system property.

**Producers** (14 raw `rabbitTemplate.convertAndSend` call sites in main code; the driver's
8 business send methods funnel through 6 raw sites):

| Side | Wrapped behind interface? | Sites |
|------|---------------------------|-------|
| Driver | Yes — `DriverSenderService` → `DriverSenderServiceImpl` | 8 sends (6 raw call sites) + `BufferServiceImpl` outbox republish |
| Data center | No — direct `RabbitTemplate` | `CommandHistoryServiceImpl`, `PointCommandServiceImpl`, `DriverStateServiceImpl`, `NotifyTaskSender`, `EntityStateExpiryScanner` |
| Manager center | No — direct `RabbitTemplate` | `MetadataEventListener` |

**Consumers** — 16 `@RabbitListener` methods, all manual-ack via `Channel` +
`RabbitAckUtil`, split across three container factories (latency-tuned default,
high-throughput, and — since `956de3dd3` — the batch-enabled
`pointValueRabbitListenerContainerFactory` for the point-value path): 13 in `dc3-common-data`
(`data/rabbit/*` receivers, `NotifyWorker`, `EntityStateExpiryScanner`) and 3 in
`dc3-common-driver` (`receiver/rabbit/*`). The point-value **dead** queue intentionally has
no listener: since `956de3dd3` it is a consumer-less quarantine (§8.2).

**Module coupling** — `dc3-common-data`, `dc3-common-driver`, `dc3-common-manager` and
`dc3-common-facade-local-manager` depend directly on `dc3-common-rabbitmq`, so the Spring
AMQP API leaks into every business module. A parallel module, `dc3-common-mqtt`
(Spring Integration MQTT + Eclipse Paho v3 client), carries the driver-side MQTT
device-access plane; only `dc3-driver-mqtt` depends on it, and it contains no
broker-vendor code — EMQX is a deployment default, not a code dependency (§7.1).

**Eight RabbitMQ-specific semantics** the abstraction must carry (the hard part — plain
send/receive is easy):

| # | Semantic | Current implementation |
|---|----------|------------------------|
| 1 | Delayed messages | TTL + DLX: 45 s driver-timeout check, 10 s device-scan tick (`DriverStateServiceImpl`, `EntityStateExpiryScanner`, delay/check exchanges in `DataTopicConfig`) |
| 2 | Per-instance broadcast | Driver metadata sync via auto-delete queue (TTL 30 s) named per driver client (`DriverTopicConfig`) |
| 3 | Dead-letter queues | point_value (live-queue TTL 7 d → DLX; dead queue is a consumer-less quarantine since `956de3dd3`), point_command, command DLX + dedicated dead receivers (`PointCommandDeadReceiver`, `CommandDeadReceiver`) |
| 4 | Manual ack + bounded back-pressure | manual ack everywhere; the point-value path acks a whole broker batch only after the PostgreSQL transaction commits (`PointValueReceiver` + `PointValueRabbitConfig`); back-pressure is bounded by prefetch ≥ batchSize with the broker as durable buffer (the old data-side ingest buffer and its buffer-full `nack(requeue=true)` were removed in `956de3dd3`) |
| 5 | Durable outbox + publisher confirm | driver persists every point value to a **mandatory SQLite outbox** (WAL, `synchronous=FULL`) before publishing; the confirm future and a retry scheduler delete or republish (`BufferServiceImpl`, `PointValueBuffer`) |
| 6 | Queue TTL as retention guard | state/alarm 30 s, event/command_result 60 s, notify_task 24 h, point_value 7 d, driver-side command queues 30 s (`DataTopicConfig`, `DriverTopicConfig`) |
| 7 | Batch consumption | consumer-side batching on the point-value path: batch listener + consumer batch, batch-level bounded retry with backoff, exhaustion rejects the whole batch to DLX (`PointValueRabbitConfig`) |
| 8 | Lease-coupled per-instance queue lifecycle | driver command queues expire (`x-expires` from `driver.lease.queue-expires`) with the driver lease; metadata broadcast queues are auto-delete with 30 s TTL (`DriverTopicConfig`) |

Two load-bearing facts make the whole abstraction feasible:

- **The delayed-timeout paths already treat the database lease as the source of truth**
  (`leaseVersion` idempotency check in `DriverTimeoutCheckReceiver`). The MQ delay is only a
  *trigger*, so a broker without native delayed delivery can fall back to a local scheduler
  without breaking correctness.
- **At-least-once + idempotent consumers is now implemented, not assumed**: point-value
  payloads carry a schema-v1 envelope (`schemaVersion`, `messageId`, `sequence`,
  `fencingToken`, `driverNode`) validated on ingest, and Data-Center ingestion is
  transactional and idempotent (`956de3dd3`).

Cross-cutting concerns that already exist and must survive the abstraction unchanged:
JSON payload convention (Jackson) — formalized on the point-value path by the schema-v1
envelope, `X-Request-Id` MDC propagation across the broker hop
(`MdcRequestIdMessagePostProcessor` / `MdcRequestIdListenerAdvice`), persistent delivery,
tenant-scoped payloads.

## 3. Goals / Non-Goals

**Goals**

- Business modules (`dc3-common-data`, `dc3-common-driver`, `dc3-common-manager`) compile
  against a broker-neutral API with **zero** broker classes on the compile classpath.
- One adapter module per broker; exactly one adapter active at runtime, selected by
  `dc3.mq.type` (default `rabbitmq`).
- The MQTT broker is a free deployment selection in both of its roles: the device-access
  plane stays vendor-neutral by protocol (EMQX is a compose default, not a code
  dependency), and the internal plane may optionally run over any MQTT 5 broker via the
  `dc3-common-mq-mqtt` adapter (§7.1).
- Existing RabbitMQ deployments keep working through the migration: physical
  exchange/queue/routing-key names byte-for-byte identical, no forced redeploy of the
  whole fleet at once.
- The point-value path's throughput semantics survive the abstraction: batch delivery with
  post-commit batch acknowledgement and bounded redelivery.
- Third-party driver authors remain unaffected: `DriverSenderService` signature does not
  change.
- Community-contributed adapters have a mechanical acceptance bar (the TCK).

**Non-Goals**

- Exactly-once delivery or cross-broker distributed transactions. The contract is
  **at-least-once + idempotent consumers** — formally implemented on the point-value path
  (messageId dedupe, fencing tokens) and required by convention elsewhere.
- Multi-broker topologies (bridging RabbitMQ to Kafka etc.).
- Rewriting the device-access MQTT plane (`dc3-common-mqtt`, `dc3-driver-mqtt`) — those
  are protocol drivers, not the internal async plane. §7.1 records the boundary and the
  vendor-neutrality guardrails instead.
- Hiding performance/ordering differences between brokers (documented, not erased).

## 4. Design principles

1. **Abstract semantics, not broker features.** The unifying level is *logical topic +
   subscription mode*. Exchange/binding/partition/tag models are adapter internals. The
   two subscription modes (`LOAD_BALANCE`, `BROADCAST`) are the lowest common semantics all
   five target brokers can express.
2. **Capability negotiation with graceful degradation, not lowest common denominator.**
   The adapter declares what it supports; the core applies documented fallbacks (e.g.
   delayed message → local scheduler) and logs the negotiated result at startup. We never
   dumb the API down to what JMS can do.
3. **One envelope format.** Payload is always JSON bytes; type info and trace context ride
   in standardized headers (`dc3-type`, `X-Request-Id`, `tenant-id`), and payload-level
   schema versioning is already in production on the point-value path (schema-v1 envelope).
   Serialization lives in the API layer — no reliance on broker-side type converters
   (today's `__TypeId__` header is a Spring AMQP internal). This keeps the wire format
   identical across brokers.

## 5. Module layout

Mirrors the existing facade-module organization (contract module + per-transport
implementations):

```
dc3-common/
├── dc3-common-mq/               # Port: interfaces, message model, envelope, capability
│                                #      negotiation, @Dc3Listener processing, fallback
│                                #      scheduler. Zero broker dependencies.
├── dc3-common-mq-rabbitmq/      # Adapter: RabbitConfig/ExchangeConfig/DataTopicConfig/
│                                #         DriverTopicConfig/PointValueRabbitConfig move
│                                #         here unchanged
├── dc3-common-mq-kafka/         # Adapter: spring-kafka
├── dc3-common-mq-rocketmq/      # Adapter: rocketmq-spring
├── dc3-common-mq-pulsar/        # Adapter: pulsar client
├── dc3-common-mq-activemq/      # Adapter: JMS 2.0 (covers Artemis and Classic)
├── dc3-common-mq-mqtt/          # Adapter: MQTT 5 client (EMQX / HiveMQ / NanoMQ / …; §7.1)
└── dc3-common-mq-tck/           # Broker-neutral contract test suite (Testcontainers)
```

Dependency direction after migration:

```
dc3-common-data ─┐
dc3-common-driver ─┼─► dc3-common-mq          (compile-time)
dc3-common-manager ─┘         ▲
                              │ (runtime, exactly one)
        dc3-common-mq-rabbitmq / -kafka / -rocketmq / -pulsar / -activemq
```

Adapters are selected by `dc3.mq.type` and activated via
`@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "...")`, the same
pattern `dc3.facade.mode` uses for facade transports.

## 6. Core API

```java
package io.github.pnoker.common.mq;

/** Logical destinations. Business code references these only. */
public enum MqTopic {
    STATE, ALARM, METADATA, POINT_COMMAND, POINT_VALUE, EVENT,
    COMMAND, COMMAND_RESULT, POINT_COMMAND_RESULT, NOTIFY_TASK
    // + logical dead-letter variants declared internally (see §8.2)
}

/** Outbound message. Envelope + JSON body, broker-agnostic. */
public final class MqMessage {
    private final MqTopic topic;
    private final String partitionKey;          // was routing-key suffix: driver service / deviceId
    private final byte[] payload;               // JSON, serialized by the API layer
    private final Map<String, String> headers;  // dc3-type, X-Request-Id, tenant-id, ...
    private final Duration delay;               // Duration.ZERO = immediate
    // builder; delay respected only if capabilities.delayedMessage()
}

public interface MessageSender {
    /** Sync send; on confirming brokers returns after the broker accepted. */
    void send(MqMessage message);

    /** Async send with unified publisher-confirm callback. */
    void sendAsync(MqMessage message, SendConfirmation confirmation);
}

public interface SendConfirmation {
    /** confirmed=false carries a retryable flag; the driver outbox keys off this. */
    void onConfirm(MqMessage message, boolean confirmed, Throwable cause);
}

public interface MqListener<T> {
    void onMessage(MqReceived<T> message, Acknowledgment ack);
}

/** Batch variant — the highest-throughput path (point values). */
public interface MqBatchListener<T> {
    /**
     * The batch is the unit of delivery AND of acknowledgement: ack() commits every
     * message in the batch; reject() requeues or dead-letters the batch as a whole.
     */
    void onBatch(List<MqReceived<T>> messages, Acknowledgment ack);
}

public final class MqReceived<T> {
    private final T payload;                    // deserialized via dc3-type header
    private final Map<String, String> headers;  // X-Request-Id restored into MDC by core
    private final int redeliveryCount;          // best-effort per broker
}

public interface Acknowledgment {
    void ack();
    /** requeue=true → retry later; requeue=false → route to the topic's dead-letter. */
    void reject(boolean requeue);
}

/** Bounded, exponential-backoff redelivery; exhaustion routes to the dead-letter. */
public record RetryPolicy(
    int maxAttempts, Duration initialBackoff, double multiplier, Duration maxBackoff
) {}

/** Subscription declaration — replaces @RabbitListener + container-factory choice. */
public record SubscriptionSpec(
    MqTopic topic,
    SubscriptionMode mode,            // LOAD_BALANCE | BROADCAST
    ConsumptionProfile profile,       // LATENCY | THROUGHPUT (concurrency/prefetch defaults)
    DeliveryMode delivery,            // SINGLE | BATCH
    int batchSize,                    // upper bound per batch; ignored when SINGLE
    RetryPolicy retry,                // bounded redelivery before dead-lettering
    Duration instanceTtl,             // per-instance queue/subscription expiry (drivers; §8.8)
    String group,                     // consumer group; default derived from service name
    Class<?> payloadType,
    boolean deadLetterEnabled
) {}

/** SPI implemented by each adapter module. */
public interface BrokerAdapter {
    String type();                                    // "rabbitmq" | "kafka" | ...
    MessageSender sender();
    void subscribe(SubscriptionSpec spec, MqListener<?> listener);
    void subscribe(SubscriptionSpec spec, MqBatchListener<?> listener);
    BrokerCapabilities capabilities();
}

public record BrokerCapabilities(
    boolean delayedMessage,         // native delayed/timed delivery
    boolean deadLetterQueue,        // broker-level DLQ (vs. explicit dead-letter topic)
    boolean broadcastSubscription,  // per-instance subscription
    boolean perMessageAck,
    boolean publisherConfirm,
    boolean batchDelivery,          // native batch fetch (vs. adapter-synthesized)
    boolean subscriptionExpiry,     // idle per-instance subscription auto-expiry
    OrderingGuarantee ordering      // NONE | PER_KEY
) {}
```

Business-side shape after migration — a single-message receiver becomes:

```java
@Dc3Listener(topic = MqTopic.COMMAND, mode = LOAD_BALANCE, profile = LATENCY)
public void commandReceive(MqReceived<CommandBO> message, Acknowledgment ack) { ... }
```

and the batch point-value receiver keeps its throughput semantics:

```java
@Dc3Listener(topic = MqTopic.POINT_VALUE, mode = LOAD_BALANCE, profile = THROUGHPUT, delivery = BATCH)
public void pointValueReceive(List<MqReceived<PointValueBO>> messages, Acknowledgment ack) { ... }
```

batchSize, prefetch and the retry policy bind from configuration (`PointBatchProperties`
today) rather than annotation literals, so ops can tune them per deployment.

`Channel`, `Message`, delivery tags and `RabbitAckUtil` disappear from business code. The
`@Dc3Listener` annotation is processed by `dc3-common-mq` core, which registers the
subscription with the active adapter — mirroring how `@RabbitListener` is processed today,
minus the broker API.

Poison handling within a batch is batch-granular — matching today's behavior, where one
invalid message causes the whole batch to retry and then dead-letter (§8.7). Whether the
port should eventually offer per-message extraction is open question §13.7.

## 7. Subscription modes and destination mapping

The two subscription modes are the central modeling decision:

- **LOAD_BALANCE** — competing consumers, each message handled once platform-wide. Used by
  point values, states, alarms, commands, events, notify tasks.
- **BROADCAST** — every running instance gets its own copy. Used by driver metadata sync
  (each driver process must refresh its local cache) and the device-scan tick fan-out.

Physical mapping per broker (namespace = today's `dc3.rabbit.tag` environment prefix):

| Semantic | RabbitMQ (adapter keeps current names) | Kafka | RocketMQ | Pulsar | ActiveMQ (JMS) | MQTT 5 |
|----------|----------------------------------------|-------|----------|--------|----------------|--------|
| LOAD_BALANCE | shared durable queue bound `rk.*` | consumer group on topic `dc3.<topic>` | CLUSTERING consume mode | Shared subscription `dc3-<topic>` | JMS Queue | shared subscription `$share/<group>/dc3/<topic>` |
| BROADCAST | per-instance auto-delete queue (current design) | per-instance `groupId = group + instanceId` | BROADCASTING consume mode | Exclusive subscription per instance | JMS Topic, unshared durable subscriber | normal subscription, persistent session per instance |
| Partition key | routing-key suffix `.<service>` | record key → partition | message key (ordered within queue) | key (key_shared if needed) | JMS selector on `key` property | — (group round-robin, no ordering) |
| Batch | consumer batch (`setBatchListener`, prefetch ≥ batchSize) | poll loop (`max.poll.records`) | batch consumption (`consumeMessageBatchSize`) | batch receive API | adapter drains within a short window (synthesized) | adapter drains within a short window (synthesized) |
| Instance expiry | `x-expires` / auto-delete + TTL (§8.8) | ❌ offsets persist — documented cleanup policy | subscription group config | subscription expiry policy | no direct equivalent — documented cleanup policy | session expiry interval ⚠️ (§13.8) |
| Namespace | name prefix (today) | topic prefix | namespace | Pulsar tenant/namespace (native fit) | destination prefix | topic prefix (`dc3/<topic>`, slash-separated) |

Ordering note (documented, not hidden): RabbitMQ today guarantees nothing; Kafka with
`partitionKey = driver service` gives per-driver ordering — strictly stronger than the
current semantics, and it matters for point-value ingestion order.

### 7.1 The MQTT boundary — two planes, one principle

MQTT appears in two distinct roles in the platform. Both must treat the MQTT broker as a
free, swappable selection — same principle as the rest of this design:

1. **Device-access plane (southbound)** — `dc3-common-mqtt` + `dc3-driver-mqtt`. This is
   protocol access, not the internal async plane, and it is already vendor-neutral by
   construction: a standard Eclipse Paho v3 client (MQTT 3.1.1) configured entirely
   through `dc3.driver.mqtt.*` (URL, auth, TLS material, topic prefix). EMQX is the
   compose-bundled default (`docker-compose-optional.yml`), nothing more — Mosquitto,
   HiveMQ, NanoMQ or VerneMQ drop in via configuration alone. Guardrails that keep it
   that way: no broker-vendor management API, rule engine, or plugin-specific code in
   Java modules; vendor specifics live in deploy configuration only. The client library
   pins the *protocol version* (3.1.1 today), not the vendor — upgrading to an MQTT 5
   client is an independent, optional library change.
2. **Internal async plane (optional broker)** — the `dc3-common-mq-mqtt` adapter. MQTT 5
   shared subscriptions express LOAD_BALANCE; a normal subscription per instance expresses
   BROADCAST; QoS 1 gives per-message ack and PUBACK confirmation (§8.4). This lets a
   deployment consolidate: one MQTT broker serving both planes, or a smaller stack without
   RabbitMQ at all. The gaps fall back through the standard mechanisms (delay → local
   scheduler, DLQ → explicit topic, batch → synthesized windowing), and the TCK —
   including group durability with all instances down (§11 case 13) — is the acceptance
   bar, because shared-subscription retention semantics vary between brokers (§13.8).

The two planes stay independent: running the internal plane over MQTT changes nothing for
the device-access drivers, and swapping the device-access broker changes nothing for the
internal plane.

## 8. Hard semantics, broker by broker

### 8.1 Delayed messages

| Broker | Mechanism |
|--------|-----------|
| RabbitMQ | current TTL + DLX pattern (unchanged); delayed-message plugin optional |
| RocketMQ | delay levels / arbitrary timing (5.x) |
| Pulsar | delayed delivery API |
| ActiveMQ | JMS scheduled delivery (`AMQ_SCHEDULED_DELAY`) |
| Kafka | **none** → API-layer fallback: local `ScheduledExecutor` re-send |
| MQTT | **none** → same local scheduler fallback |

Fallback safety: consumers of delayed messages (`DriverTimeoutCheckReceiver`,
device scan) are already idempotent against the DB lease (`leaseVersion`). Multiple or
late triggers are harmless. The fallback runs in whichever instance sent the message;
startup logs state `delayedMessage=false → local scheduler fallback active`.

### 8.2 Dead-letter queues

`reject(false)` is the single entry point. The adapter maps it to:

| Broker | Mechanism |
|--------|-----------|
| RabbitMQ | current DLX wiring (point_value / point_command / command) |
| Kafka | explicit `dc3.<topic>.dlq` topic written by the adapter on exhaustion |
| RocketMQ | built-in `%RETRY%group` / `%DLQ%group`, surfaced as logical dead-letter |
| Pulsar | DLQ policy + `maxRedeliverCount` |
| ActiveMQ | JMS redelivery policy + DLQ |
| MQTT | explicit `dc3/<topic>.dlq` topic written by the adapter on exhaustion |

The dedicated dead receivers (`CommandDeadReceiver`, `PointCommandDeadReceiver`) re-declare
their subscriptions against the logical dead-letter topics. The point-value dead queue has
**no consumer by design** since `956de3dd3` — it is a quarantine for poison batches and
7-day-unconsumed values; the port models it as a dead-letter topic without a subscription.
Its retention is an open question (§13.6). The 7-day TTL on the **live** point-value queue
becomes a topic retention policy attribute — see §8.6.

### 8.3 Acknowledgement, retry and back-pressure

| Operation | RabbitMQ | Kafka | RocketMQ | Pulsar | ActiveMQ | MQTT 5 |
|-----------|----------|-------|----------|--------|----------|--------|
| `ack()` | basicAck (single or batch-multiple) | offset commit (batched) | CONSUME_SUCCESS | ack | acknowledge | PUBACK (QoS 1) |
| `reject(true)` | basicNack requeue | seek back, no commit | RECONSUME_LATER | negative ack / redelivery | rollback | adapter-level retry loop (broker has no nack) |
| `reject(false)` | basicReject → DLX | write to `.dlq` topic | built-in retry→DLQ | DLQ policy | redelivery-policy DLQ | publish to `.dlq` topic |
| bounded retry (RetryPolicy) | stateless retry advice + recoverer (today's batch factory) | in-memory retry loop, then `.dlq` | `%RETRY%group` (native fit) | redelivery backoff + DLQ | redelivery policy | adapter in-memory loop |
| back-pressure | prefetch ≥ batchSize, broker as durable buffer | consumer `pause()`/`resume()` | suspend current queue | flow control / queue size | session recover | receive maximum (MQTT 5 flow control) |

The bounded-retry row matters: since `956de3dd3` the point-value factory wraps delivery in
a stateless retry advice with exponential backoff, and exhaustion rejects the whole batch
to the DLX. `RetryPolicy` on `SubscriptionSpec` makes that a first-class port semantic
instead of per-adapter improvisation — Kafka needs an in-memory loop, RocketMQ gets it
natively from `%RETRY%group`.

The old buffer-full `nack(requeue=true)` pattern is gone from the codebase (the data-side
ingest buffer was removed; `NotifyWorker` deliberately acks failures as FAILED instead of
requeueing). Back-pressure is now "broker as durable buffer + bounded prefetch", which
maps cleanly to every target broker.

Contract documented for consumers: **at-least-once**, redelivery possible, idempotency
required. `redeliveryCount` on `MqReceived` is best-effort (exact on Kafka via retry topic
count, RocketMQ reconsume times, Pulsar redelivery count; approximate elsewhere).

### 8.4 Publisher confirmation and the driver outbox

Since `956de3dd3` the driver already owns durability: every point value is persisted to a
**mandatory SQLite outbox** (WAL, `synchronous=FULL`, startup validation, missing
configuration fails fast) *before* `convertAndSend`, and `BufferServiceImpl` +
`PointValueBuffer` run a claim-before-publish retry scheduler over pending rows.

After migration, the outbox itself stays driver-side and broker-neutral — only the final
`convertAndSend` + `CorrelationData`-future plumbing swaps for `sendAsync` +
`SendConfirmation`. Broker mapping: RabbitMQ publisher confirms; Kafka `acks=all` future;
RocketMQ send result; Pulsar send future; MQTT QoS 1 PUBACK (per-message confirm — the
strongest of the non-RabbitMQ set); **JMS has no confirmation** → best-effort
(fire-and-forget with failure-driven republish from the outbox), marked in the capability
matrix.

Consequence worth stating explicitly: **publisher confirm is no longer load-bearing for
durability**. Because the outbox owns persistence, `publisherConfirm=false` brokers are
fully acceptable — confirm becomes a latency/republish-frequency optimization, not a
correctness requirement. The ActiveMQ adapter is therefore a first-class citizen, not a
degraded mode.

### 8.5 Envelope, tracing, tenant context

- `dc3-type` header carries the payload class name; the API layer deserializes — replaces
  Spring AMQP's `__TypeId__`.
- Payload-level schema versioning is already in production on the point-value path
  (schema-v1: `schemaVersion`, `messageId`, `sequence`, `fencingToken`, `driverNode`,
  validated in `PointValueReceiver`); the port adopts the same convention — schema fields
  live in the payload, transport hints in headers.
- `X-Request-Id` propagation (currently `MdcRequestIdMessagePostProcessor` /
  `MdcRequestIdListenerAdvice`) moves into `dc3-common-mq` core; all brokers support
  string headers.
- Tenant id remains inside the payload (and mirrored as a header for operational
  filtering); tenant scoping rules are unaffected by the broker choice.

### 8.6 Queue-level message TTL as retention policy

A RabbitMQ-specific usage the port must model: queues carry per-queue message TTL
as a retention/expiry guard — state/alarm 30 s (stale lifecycle events are worthless),
event/command_result 60 s, notify_task 24 h (runaway-backlog guard when outbound channels
are stuck), point_value 7 d (then dead-lettered, §8.2), driver-side command queues 30 s.

This becomes a `retention` attribute on the topic registry, with per-broker semantics
documented rather than identical: RabbitMQ TTL drops per message from the head of the
queue; Kafka/RocketMQ/Pulsar retention is time-based log retention (drops whole old
segments — effectively the same "don't keep forever" guarantee); JMS TTL and MQTT 5
message expiry are per message.
The distinction only matters for exact expiry timing, which no consumer in the codebase
relies on (expired state events are simply dropped, and the DB remains the source of
truth).

### 8.7 Batch consumption and transactional ack

The point-value path — the platform's highest-volume stream — runs on consumer-side
batching since `956de3dd3`:

- `PointValueRabbitConfig` declares a third container factory: batch listener + consumer
  batch, `prefetch ≥ batchSize`, bounded retry with backoff, exhaustion → whole batch to
  the point-value DLX.
- `PointValueReceiver` receives `List<Message>`, validates the schema-v1 envelope per
  message, persists history + latest projections in **one PostgreSQL transaction**, and
  only then `basicAck(lastTag, multiple=true)` — ack and commit are atomic from the
  consumer's perspective.

| Broker | Batch mechanism |
|--------|-----------------|
| RabbitMQ | consumer batch (`setBatchListener` + `setConsumerBatchEnabled`), current design |
| Kafka | poll loop with `max.poll.records`; offsets committed after the handler returns |
| RocketMQ | batch consumption (`consumeMessageBatchSize`) |
| Pulsar | batch receive API |
| ActiveMQ | no native consumer batch → adapter drains available messages within a short window (synthesized, capability `batchDelivery=false`) |
| MQTT | no native consumer batch → same adapter-side windowing (synthesized, `batchDelivery=false`) |

Port model: `DeliveryMode.BATCH` + `MqBatchListener` + `RetryPolicy`. The TCK verifies
that `ack()` after batch processing commits every message in the batch and that retry
exhaustion dead-letters rather than drops (§11).

Known trade-off (documented, not hidden): one poison message retries and dead-letters the
**whole batch** today. Batch-level granularity is the Spring default and keeps the
cross-broker story simple; per-message extraction is open question §13.7.

### 8.8 Per-instance queue lifecycle (lease-coupled expiry)

Driver-side subscriptions are per-instance and must not outlive their driver:

- command/point-command queues carry `x-expires = driver.lease.queue-expires` — a dead
  driver's queue disappears with its lease instead of silently accumulating commands that
  would be delivered to a stale instance on restart;
- the metadata broadcast queue is auto-delete with 30 s message TTL, bounding staleness
  from dead instances.

Port model: `instanceTtl` on `SubscriptionSpec` + capability `subscriptionExpiry`.
RabbitMQ maps to `x-expires`/auto-delete natively; RocketMQ/Pulsar have subscription-level
equivalents; Kafka offsets simply persist (stale groups are cosmetic — documented cleanup
policy via admin tooling); JMS has no direct equivalent (documented cleanup policy);
MQTT 5 maps to the session expiry interval of the persistent session.
Neither fallback affects correctness, because command senders already validate driver
lease/ownership before dispatch.

## 9. Capability matrix (published, per adapter)

Implementation status (2026-08-19): rabbitmq, kafka, activemq (Artemis), mqtt 5 and
rocketmq adapters are implemented and certified by the TCK against live brokers;
pulsar is pending. Certified columns below reflect the implemented behavior (e.g.
rabbit delays arbitrary messages through the port fallback, rocketmq native delay
levels would quantize requested durations; the rocketmq classic client replays topic
backlog for brand-new consumer groups regardless of consumeFromWhere, so the adapter
seeds fresh groups to the latest offset and warms up not-yet-created topics on
subscribe).

| Capability | RabbitMQ ✅ | Kafka ✅ | ActiveMQ ✅ | MQTT 5 ✅ | RocketMQ ✅ | Pulsar (pending) |
|------------|----------|-------|----------|--------|----------|--------|
| Delayed message | fallback* | ❌ → local fallback | ✅ JMS scheduled | ❌ → local fallback | fallback (levels quantize) | ✅ native |
| Native DLQ | DLX + quarantine | adapter `.dlq` topic | adapter `.dlq` queue | adapter `/dlq` topic | adapter `-dlq` topic | ✅ policy |
| Broadcast | ✅ per-instance queue | ✅ (instance groups) | ✅ topic consumer | ✅ plain filter | ✅ BROADCASTING | ✅ |
| Per-message ack | ✅ | offset (approx) | ✅ client ack | ✅ QoS 1 | ✅ | ✅ |
| Publisher confirm | ✅ confirms | ✅ (acks=all) | ❌ best-effort (outbox covers it, §8.4) | ✅ (PUBACK) | ✅ sync send | ✅ |
| Batch delivery | ✅ native | ✅ native | ⚠️ synthesized | ⚠️ synthesized | ✅ consumer batch | ✅ batch receive |
| Per-key ordering | ❌ | ✅ | ❌ | ❌ | ✅ (per queue) | ✅ (key_shared) |
| Subscription expiry | ✅ x-expires | ❌ documented | ❌ documented | ❌ documented | ❌ documented | ⚠️ session expiry |
| Group durability offline | ✅ durable queue | ✅ log retention | ✅ durable subscription | ⚠️ broker-dependent (§13.8) | ✅ offsets | ✅ |
| Retention | queue TTL | retention config | subscription retention | broker-dependent | retention | retention/TTL |

\* rabbit intrinsic TTL+DLX delays (STATE_TIMEOUT / DEVICE_SCAN) work server-side as
before; arbitrary per-message delays use the port fallback (capability false).

This table is user-facing documentation ("which broker should I pick?") and the startup
negotiation log summarizes it per deployment.

## 10. Alternatives considered

**Spring Cloud Stream** — gives official RabbitMQ/Kafka binders for free, but: the
RocketMQ binder is Alibaba-maintained, the Pulsar binder has thin community coverage,
ActiveMQ has no modern binder; and its functional programming model fights our per-driver
dynamic subscriptions, TTL+DLX delays, manual-ack back-pressure, batch-ack and
confirm-outbox patterns. Rejected as the port layer — though an individual adapter may
internally build on SCStream later; the port is the contract we control.

**JMS 2.0 as the core API** — rejected. Lowest common denominator: no delayed delivery
standard, weak DLQ semantics, no publisher confirmation, broadcast/group semantics map
poorly, no batch consumption. JMS remains the implementation technology of the ActiveMQ
adapter only.

**Apache Camel as the port** — rejected. An integration framework as the contract layer
inverts the dependency (the platform's core messaging on a routing DSL), drags a large
dependency graph into every service, and still leaves the DC3-specific semantics (batch
post-commit ack, lease-coupled subscription expiry) to be modeled somewhere. Adapters stay
plain client libraries.

**Do nothing / RabbitMQ-only** — rejected for a globally distributed project: Kafka and
RocketMQ are the default choices in many enterprise environments (especially CN ecosystem
for RocketMQ), and "bring your own broker" is a recurring community ask for on-prem
integration.

## 11. TCK — the community extension mechanism

`dc3-common-mq-tck` contains one broker-neutral contract suite executed against each
adapter via Testcontainers (rabbitmq, kafka, rocketmq, pulsar, artemis, an MQTT 5 broker —
EMQX or NanoMQ):

1. send → receive (round-trip, envelope fidelity, headers, `dc3-type` deserialization)
2. LOAD_BALANCE: exactly one consumer receives each message across 2 instances
3. BROADCAST: both instances receive each message
4. delay: message not delivered before the deadline, delivered after
5. `reject(false)` → dead-letter topic receives the message
6. `reject(true)` → redelivery observed (at-least-once)
7. `sendAsync` confirmation fires with the correct outcome
8. requestId header survives the hop (MDC restored)
9. back-pressure: rejected/full-path message is not lost
10. BATCH delivery: batch callback receives ≥ 1 messages; `ack()` commits the whole batch
    (no redelivery after restart)
11. RetryPolicy: observed attempts ≤ maxAttempts, then dead-letter — never silent drop
12. instanceTtl: where `subscriptionExpiry=true`, an idle per-instance subscription is
    removed after the TTL (timing-tolerant assertion)
13. LOAD_BALANCE with no live instance: messages published while the entire group is down
    are retained and delivered when an instance starts (queue-level durability; MQTT
    shared-subscription brokers vary here — §13.8)

**An adapter passes the TCK ⇒ it is compliant.** This is the acceptance bar for community
adapters (EMQX-bridged transports, Redis Streams, SQS…). The existing
`RabbitDeliveryIT` / `RabbitTestHarness` assertions migrate into this suite; the E2E
suite keeps running unchanged against the rabbitmq adapter as the migration gate.

## 12. Migration plan

Wire compatibility is the invariant: no existing RabbitMQ deployment should notice the
refactor.

- **Phase 1 — extract the port, RabbitMQ adapter moves unchanged.**
  Create `dc3-common-mq` + `dc3-common-mq-rabbitmq`. Move `RabbitConfig`,
  `ExchangeConfig`, `DataTopicConfig`, `DriverTopicConfig`, `PointValueRabbitConfig`, MDC
  propagation, ack helpers into the adapter with **identical physical names**. Convert the
  14 raw producer call sites (8 driver send methods, outbox republish, 6 center-side
  sends) and 16 listeners to the new API (`@Dc3Listener`), including the batch point-value
  receiver. Business-module poms swap `dc3-common-rabbitmq` → `dc3-common-mq` +
  `dc3-common-mq-rabbitmq`.
  *Gate: existing E2E (`RabbitDeliveryIT` etc.) runs unmodified and green.*
- **Phase 2 — TCK + Kafka adapter.** Highest global demand. Partition-key ordering per
  driver is a documented upgrade over the RabbitMQ baseline.
- **Phase 3 — RocketMQ (CN ecosystem demand), Pulsar, ActiveMQ/Artemis, MQTT 5.** The
  MQTT adapter lets a deployment run both planes (device access + internal async) on one
  broker — or drop RabbitMQ entirely where an MQTT broker is already operated. Publish the
  capability matrix, `dc3.mq.type` compose profiles per broker.
- **Throughout** — `DriverSenderService` interface is untouched; third-party driver
  JARs compiled against it keep working.

Suggested cleanups riding along Phase 1:

- `RabbitConstant` moves out of `dc3-common-constant`'s `constant/driver` package: logical
  names become `MqTopic` in the port; physical names become adapter-private
  (`RabbitNames`).
- Delete dead topology: the `register` exchange/queue/routing constants (declared and
  referenced nowhere) and the unused `dc3.e.mqtt` exchange + `QUEUE_MQTT` (no binding, no
  producer, no consumer). Re-introduce only when a real consumer exists.
- Packages `data/rabbit`, `receiver/rabbit` → `data/mq/listener`, `driver/mq/listener`.
- `dc3.rabbit.tag` → `dc3.mq.namespace` (rabbitmq adapter maps it to the legacy property
  for compatibility).

## 13. Open questions

1. **Pulsar tenancy** — map `dc3.mq.namespace` to Pulsar tenant/namespace natively, or
   flatten to topic prefix? (Native is cleaner but requires tenant provisioning docs.)
2. **Envelope evolution** — align `dc3-type`/headers with CloudEvents 1.0 attribute names
   for third-party interoperability? Cheap now, breaking later; leaning yes for `type` /
   `traceparent`. The schema-v1 payload envelope strengthens the case: transport headers
   and payload schema should evolve on one roadmap.
3. **Kafka back-pressure semantics** — `pause()` needs a resume signal the current
   `Acknowledgment` API doesn't express; may need `Acknowledgment.defer()` or a
   listener-container-level hook. Decide during Phase 2 TCK work.
4. **BROADCAST queue TTLs** — the 30 s auto-delete TTL on metadata queues bounds staleness
   on dead instances; non-RabbitMQ brokers express this via subscription expiry or
   heartbeat — confirm per-adapter strategy in the TCK.
5. **Single-process mode (`dc3-center-single`)** — confirm whether it should get an
   in-process/no-broker adapter (`dc3-common-mq-local`) for the smallest deployments,
   reusing the facade `local` precedent.
6. **Point-value dead-queue retention** — the quarantine queue has no consumer and no TTL;
   a stuck deployment grows it without bound. Decide: queue TTL, size-capped alerting, or
   a minimal auditing consumer.
7. **Batch poison granularity** — today one poison message dead-letters the whole batch.
   Per-message extraction (dead-letter only the offender, ack the rest) is friendlier but
   cross-broker messier (Kafka needs a reprocess loop). Decide what the TCK mandates;
   batch-granular is the safe default.
8. **MQTT shared-subscription group semantics** — MQTT 5 standardizes `$share` delivery
   to one *online* member but is silent on retention while no member is online
   (broker-specific). TCK case 13 decides compliance per broker; the capability matrix
   must call the behavior out for deployers weighing the MQTT adapter.

## 14. Appendix — current call-site inventory (Phase 1 checklist)

Producers (14 raw call sites, main code):

| Module | Class | Sends to |
|--------|-------|----------|
| driver | `DriverSenderServiceImpl` (8 methods, 6 raw sites) | STATE, ALARM ×2, POINT_VALUE, POINT_COMMAND_RESULT, COMMAND_RESULT, EVENT |
| driver | `BufferServiceImpl` | POINT_VALUE (outbox republish + retry scheduler) |
| data | `DriverStateServiceImpl` | STATE_TIMEOUT_DELAY (45 s) |
| data | `EntityStateExpiryScanner` | STATE_TIMEOUT_DELAY (scan tick), DEVICE_SCAN |
| data | `CommandHistoryServiceImpl` | COMMAND |
| data | `PointCommandServiceImpl` | POINT_COMMAND |
| data | `NotifyTaskSender` | NOTIFY_TASK (via alarm exchange) |
| manager | `MetadataEventListener` | METADATA |

Consumers (16):

| Module | Listener | Topic / role | Mode |
|--------|----------|--------------|------|
| data | `DriverStateReceiver`, `DeviceStateReceiver` | STATE | LOAD_BALANCE |
| data | `DriverAlarmReceiver`, `DeviceAlarmReceiver` | ALARM | LOAD_BALANCE |
| data | `PointValueReceiver` (BATCH, post-commit ack) | POINT_VALUE | LOAD_BALANCE |
| data | `PointCommandResultReceiver`, `CommandResultReceiver` | result topics | LOAD_BALANCE |
| data | `PointCommandDeadReceiver`, `CommandDeadReceiver` | dead letters | LOAD_BALANCE |
| data | `EventReportReceiver` | EVENT | LOAD_BALANCE |
| data | `DriverTimeoutCheckReceiver` | delay check | LOAD_BALANCE |
| data | `NotifyWorker` | NOTIFY_TASK | LOAD_BALANCE |
| data | `EntityStateExpiryScanner` (listener) | device scan | LOAD_BALANCE |
| driver | `MetadataReceiver` | METADATA | **BROADCAST** |
| driver | `CommandReceiver`, `PointCommandReceiver` | COMMAND / POINT_COMMAND | LOAD_BALANCE (per-service queue, lease-coupled expiry) |

The point-value dead queue deliberately has no listener (quarantine, §8.2).
