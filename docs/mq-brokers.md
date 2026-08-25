# Message Broker Selection

IoT DC3's internal async plane (point values, commands, states, alarms, events, notify tasks between the center services
and the drivers) runs behind a broker-neutral messaging port: the application code is identical on every broker, and the
deployment picks one with a single setting plus one dependency.

> Design background: [`docs/design/mq-abstraction.md`](./design/mq-abstraction.md)

## How to pick a broker

1. Set `DC3_MQ_TYPE` (maps to `dc3.mq.type`) — see the table below. Default is
   `rabbitmq`, so existing deployments change nothing.
2. Provide the connection settings for that broker (table below).
3. That is all in terms of configuration — nothing else in the stack is broker-specific.
   Note that only the `rabbitmq` adapter ships in the standard `dc3` images; running
   another broker additionally requires adding the corresponding `dc3-mq-*` adapter
   dependency to the deployment (step 1 of the selection is still just the type +
   connection settings).

The internal plane and the device-access plane are independent: switching the internal broker never affects the MQTT
drivers or the EMQX-based device access.

## Device-access plane (southbound MQTT)

The device-access MQTT stack needs no adapter layer at all — MQTT is an open wire protocol and the driver uses a
standard Paho client, so the broker is selected by one URL (`dc3.driver.mqtt.url`): EMQX, Mosquitto, HiveMQ, NanoMQ,
VerneMQ ... are drop-in replacements with zero code change. The compose default (`dc3-emqx`) is a deployment choice, not
a dependency; vendor-specific features (management APIs, rule engines) must not leak into driver code, and
`MqttVendorNeutralityIT` keeps that property verified mechanically — the same client code round-trips against two
different broker vendors, differing only in the URL.

One dialect note: the device-access client speaks MQTT 3.1.1, which every mainstream broker supports and which covers
device access (publish/subscribe, QoS, retained, TLS, X.509). MQTT 5-only features such as shared subscriptions are used
exclusively by the internal-plane adapter above.

## Certified brokers

Every adapter below except RocketMQ passes the same broker-neutral contract suite (`dc3-mq-tck`) against a live
broker. The RocketMQ adapter is **experimental / not yet certified**: its contract suite is opt-in
(`TCK_ROCKETMQ_NAMESRV`) and self-describes as not-yet-certified — run it against your own broker before relying on
it.

| Broker                                    | `DC3_MQ_TYPE` | Connection settings                                                                   | Notes                                                                                                               |
|-------------------------------------------|---------------|---------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| RabbitMQ (default)                        | `rabbitmq`    | `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` (+ SSL) | Wire-compatible with all pre-port deployments; publisher confirms + returns                                         |
| Kafka                                     | `kafka`       | `spring.kafka.bootstrap-servers`                                                      | `acks=all`; new consumer groups start at latest; per-key ordering; plain connections only — the adapter configures no SASL/TLS |
| RocketMQ (experimental)                   | `rocketmq`    | `dc3.mq.rocketmq.name-server-address`                                                 | CLUSTERING/BROADCASTING modes; sync-send confirmation; uncertified — see above                                      |
| Pulsar                                    | `pulsar`      | `dc3.mq.pulsar.service-url`                                                           | Shared/exclusive subscriptions; native batch receive                                                                |
| ActiveMQ (Artemis / Classic)              | `activemq`    | `dc3.mq.activemq.url` (+ user/password)                                               | JMS 2.0; shared durable subscriptions; scheduled delays; no publisher confirm — the driver outbox covers durability |
| MQTT 5 (EMQX / HiveMQ / NanoMQ / VerneMQ) | `mqtt`        | `dc3.mq.mqtt.host` / `dc3.mq.mqtt.port`                                               | Shared subscriptions (MQTT 5); QoS 1; lets one broker serve both the device plane and the internal plane            |

## Capability matrix

| Capability               | RabbitMQ         | Kafka                | RocketMQ                   | Pulsar                      | ActiveMQ                                 | MQTT 5               |
|--------------------------|------------------|----------------------|----------------------------|-----------------------------|------------------------------------------|----------------------|
| Delayed message          | fallback*        | ❌ → local fallback  | fallback (levels quantize) | fallback (native available) | ✅ JMS scheduled                         | ❌ → local fallback  |
| Native DLQ               | DLX + quarantine | adapter `.dlq` topic | adapter `-dlq` topic       | adapter `.dlq` topic        | adapter `.dlq` queue                     | adapter `/dlq` topic |
| Broadcast                | ✅               | ✅                   | ✅                         | ✅                          | ✅                                       | ✅                   |
| Per-message ack          | ✅               | offset (approx)      | ✅                         | ✅                          | ✅                                       | ✅ (QoS 1)           |
| Publisher confirm        | ✅               | ✅ (acks=all)        | ✅ sync send               | ✅ (message id)             | ❌ best-effort (driver outbox covers it) | ✅ (PUBACK)          |
| Batch delivery           | ✅ native        | ✅ native            | ✅ native                  | ✅ native                   | ⚠️ synthesized                           | ⚠️ synthesized       |
| Per-key ordering         | ❌               | ✅                   | ❌ (keys carried for routing only) | ❌ (shared sub)     | ❌                                       | ❌                   |
| Key-pattern routing      | ✅ broker bindings | ✅ client-side router | ✅ client-side router     | ✅ client-side router        | ✅ client-side router                    | ✅ client-side router |
| Subscription expiry      | ✅ x-expires     | ❌ documented        | ❌ documented              | ❌ documented               | ❌ documented                            | ⚠️ session expiry    |
| Group durability offline | ✅ durable queue | ✅ log retention     | ✅ offsets                 | ✅ durable subscription     | ✅ durable subscription                  | ⚠️ broker-dependent  |
| Retention                | queue TTL        | retention config     | retention                  | retention                   | subscription retention                   | broker-dependent     |

\* RabbitMQ's intrinsic TTL+DLX delays (driver lease timeout, device scan tick) work server-side exactly as before;
arbitrary per-message delays use the port's local scheduler fallback on every broker for uniform behavior.

The full reasoning (semantics per broker, fallbacks, the TCK as the acceptance bar for community adapters) lives in the
design document linked above.

## Notes

- **Key-pattern routing**: only RabbitMQ evaluates `SubscriptionSpec.keyPattern` broker-side (topic-exchange
  bindings). Every other adapter runs a client-side topic router in front of one consumer per (topic, group): the
  message's partition key is matched against each listener's pattern with Rabbit topic-binding wildcard semantics
  (`*` = one word, `#` = zero or more words, blank pattern = everything; see `KeyMatcher` in `dc3-mq-core`), several
  matching listeners in one JVM round-robin, and a key matching no listener in that JVM is acknowledged and skipped —
  Rabbit's unroutable-drop semantics. Cross-JVM load balancing stays the broker's job via the shared consumer group.
- **Kafka security**: the adapter builds its producer/consumer with `acks=all` but no SASL/TLS settings — plain
  connections only. Secured Kafka clusters need a user-provided `KafkaTemplate`/consumer config until the adapter
  grows a security-properties bridge.
- **Driver outbox**: point-value durability is owned by the driver's SQLite outbox on every broker, so brokers without
  publisher confirmation remain fully safe.
- **At-least-once**: the delivery contract is at-least-once with idempotent consumers on every broker; consumers must
  not assume ordering (except where the matrix notes per-key ordering).
- **Community adapters**: pass the contract suite in `dc3-mq-tck` against your broker to certify a new adapter (Redis
  Streams, SQS, ...).
