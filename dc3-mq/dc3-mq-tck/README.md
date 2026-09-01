# DC3 MQ TCK

`dc3-mq-tck` is the release gate for the four supported `dc3-mq` adapters. An adapter that passes these tests is compliant
with the messaging port. `AbstractMqContractTest` defines the shared contract (publish/confirm, Publisher completion,
subscribe, batch consumption, retry, lifecycle re-delivery, and poison-message handling); each concrete test boots the
broker in a disposable Testcontainers container.

## Contract tests

| Test                   | Broker                                       |
|------------------------|----------------------------------------------|
| `RabbitMqContractTest` | RabbitMQ (`rabbitmq:3.13-management-alpine`) |
| `KafkaContractTest`    | Apache Kafka (`apache/kafka:3.9.0`)          |
| `PulsarContractTest`   | Apache Pulsar                                |
| `MqttContractTest`     | HiveMQ CE (MQTT 5)                           |

## Running

Requires a container runtime — the tests are annotated `@Testcontainers(disabledWithoutDocker = true)` and are skipped
without Docker:

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-tck test
```

## Related Modules

- `dc3-mq-core` — the port being verified
- `dc3-mq-*` — the adapters under test
