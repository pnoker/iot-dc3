# DC3 MQ Core

`dc3-mq-core` defines the broker-neutral messaging port of IoT DC3. Business code depends on this module only —
never on a broker-specific adapter. It owns the logical topics, subscription contracts, delivery envelopes, capability
negotiation, retry classification, and the batch-consumer configuration shared by the data path.

## Key types

| Type | Role |
|---|---|
| `BrokerAdapter` | SPI implemented by every broker adapter |
| `BrokerCapabilities` | negotiated broker feature set |
| `Dc3Listener` / `MqListener` | listener annotations processed by `Dc3ListenerProcessor` |
| `MessageSender` / `MessageSenderImpl` | publish path with per-message confirmations |
| `MqMessage` / `MqReceived` | logical wire envelopes (`EnvelopeCodec`) |
| `RetryPolicy` / `MqPoisonException` / `MqPublishException` | retry and failure classification |
| `MqBatchListener` / `RawBatchListener` / `RawDeliveryListener` | batch-consumer contracts |
| `BatchConsumerProperties` | binds `dc3.data.point.batch` (batch size, timeouts, concurrency, retries) |
| `MqAutoConfiguration` | wires the shared messaging beans |

## Dependencies

`dc3-common-constant`, `dc3-common-public`, Spring Boot auto-configuration, OpenTelemetry API. No broker libraries.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-core -am package
```

## Testing

This module has no tests of its own; the port contract is verified against every adapter in `dc3-mq-tck`:

```bash
mvn -s .mvn/settings.xml -q -pl dc3-mq/dc3-mq-core -am -DskipTests compile
```

## Related Modules

- `dc3-mq-*` — broker adapters implementing `BrokerAdapter`
- `dc3-mq-tck` — broker-neutral contract suite
- `dc3-common-data` / `dc3-common-driver` — primary consumers of the port
