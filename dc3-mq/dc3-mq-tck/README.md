# DC3 MQ TCK

`dc3-mq-tck` is the release gate for the four supported `dc3-mq` adapters. An adapter that passes these tests is compliant
with the messaging port. `AbstractMqContractTest` defines the shared contract (publish/confirm, Publisher completion,
subscribe, batch consumption, retry, lifecycle re-delivery, and poison-message handling); each concrete test boots the
broker in a disposable Testcontainers container.

## Contract tests

| Test                   | Broker                                       |
|------------------------|----------------------------------------------|
| `RabbitMqContractIT` | RabbitMQ (`rabbitmq:3.13-management-alpine`) |
| `KafkaContractIT`    | Apache Kafka (`apache/kafka:3.9.0`)          |
| `PulsarContractIT`   | Apache Pulsar                                |
| `MqttContractIT`     | HiveMQ CE (MQTT 5)                           |

## Running

Requires a container runtime or the documented external-broker environment variables. Container-backed cases are
skipped when Docker is unavailable:

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-tck -am verify
```

## Related Modules

- `dc3-mq-core` — the port being verified
- `dc3-mq-*` — the adapters under test
