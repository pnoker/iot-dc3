# Message Broker Selection

IoT DC3's internal async plane (point values, commands, states, alarms, events,
notify tasks between the center services and the drivers) runs behind a
broker-neutral messaging port: the application code is identical on every broker,
and the deployment picks one with a single setting plus one dependency.

> Design background: [`docs/design/mq-abstraction.md`](./design/mq-abstraction.md)

## How to pick a broker

1. Set `DC3_MQ_TYPE` (maps to `dc3.mq.type`) — see the table below. Default is
   `rabbitmq`, so existing deployments change nothing.
2. Provide the connection settings for that broker (table below).
3. That is all — the `dc3` images ship every certified adapter; nothing else in
   the stack is broker-specific.

The internal plane and the device-access plane are independent: switching the
internal broker never affects the MQTT drivers or the EMQX-based device access.

## Certified brokers

Every adapter below passes the same broker-neutral contract suite
(`dc3-common-mq-tck`) against a live broker.

| Broker | `DC3_MQ_TYPE` | Connection settings | Notes |
|--------|--------------|---------------------|-------|
| RabbitMQ (default) | `rabbitmq` | `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` (+ SSL) | Wire-compatible with all pre-port deployments; publisher confirms + returns |
| Kafka | `kafka` | `spring.kafka.bootstrap-servers` | `acks=all`; new consumer groups start at latest; per-key ordering |
| RocketMQ | `rocketmq` | `dc3.mq.rocketmq.name-server-address` | CLUSTERING/BROADCASTING modes; sync-send confirmation |
| Pulsar | `pulsar` | `dc3.mq.pulsar.service-url` | Shared/exclusive subscriptions; native batch receive |
| ActiveMQ (Artemis / Classic) | `activemq` | `dc3.mq.activemq.url` (+ user/password) | JMS 2.0; shared durable subscriptions; scheduled delays; no publisher confirm — the driver outbox covers durability |
| MQTT 5 (EMQX / HiveMQ / NanoMQ / VerneMQ) | `mqtt` | `dc3.mq.mqtt.host` / `dc3.mq.mqtt.port` | Shared subscriptions (MQTT 5); QoS 1; lets one broker serve both the device plane and the internal plane |

## Capability matrix

| Capability | RabbitMQ | Kafka | RocketMQ | Pulsar | ActiveMQ | MQTT 5 |
|------------|----------|-------|----------|--------|----------|--------|
| Delayed message | fallback* | ❌ → local fallback | fallback (levels quantize) | fallback (native available) | ✅ JMS scheduled | ❌ → local fallback |
| Native DLQ | DLX + quarantine | adapter `.dlq` topic | adapter `-dlq` topic | adapter `-dlq` topic | adapter `.dlq` queue | adapter `/dlq` topic |
| Broadcast | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Per-message ack | ✅ | offset (approx) | ✅ | ✅ | ✅ | ✅ (QoS 1) |
| Publisher confirm | ✅ | ✅ (acks=all) | ✅ sync send | ✅ (message id) | ❌ best-effort (driver outbox covers it) | ✅ (PUBACK) |
| Batch delivery | ✅ native | ✅ native | ✅ native | ✅ native | ⚠️ synthesized | ⚠️ synthesized |
| Per-key ordering | ❌ | ✅ | ✅ (per queue) | ❌ (shared sub) | ❌ | ❌ |
| Subscription expiry | ✅ x-expires | ❌ documented | ❌ documented | ❌ documented | ❌ documented | ⚠️ session expiry |
| Group durability offline | ✅ durable queue | ✅ log retention | ✅ offsets | ✅ durable subscription | ✅ durable subscription | ⚠️ broker-dependent |
| Retention | queue TTL | retention config | retention | retention | subscription retention | broker-dependent |

\* RabbitMQ's intrinsic TTL+DLX delays (driver lease timeout, device scan tick) work
server-side exactly as before; arbitrary per-message delays use the port's local
scheduler fallback on every broker for uniform behavior.

The full reasoning (semantics per broker, fallbacks, the TCK as the acceptance bar
for community adapters) lives in the design document linked above.

## Notes

- **Driver outbox**: point-value durability is owned by the driver's SQLite outbox
  on every broker, so brokers without publisher confirmation remain fully safe.
- **At-least-once**: the delivery contract is at-least-once with idempotent
  consumers on every broker; consumers must not assume ordering (except where the
  matrix notes per-key ordering).
- **Community adapters**: pass the contract suite in `dc3-common-mq-tck` against
  your broker to certify a new adapter (Redis Streams, SQS, ...).
