# DC3 MQ TCK

`dc3-mq-tck` is the broker-neutral contract suite of the `dc3-mq` family: an adapter that passes these tests is
compliant with the messaging port. `AbstractMqContractTest` defines the shared contract (publish/confirm, subscribe,
batch consumption, retry, and poison-message handling); one concrete test per adapter boots the broker in a disposable
Testcontainers container.

## Contract tests

| Test | Broker |
|---|---|
| `RabbitMqContractTest` | RabbitMQ (`rabbitmq:3.13-management-alpine`) |
| `KafkaContractTest` | Apache Kafka (`apache/kafka:3.9.0`) |
| `RocketMqContractTest` | RocketMQ |
| `PulsarContractTest` | Pulsar |
| `ActiveMqContractTest` | ActiveMQ |
| `MqttContractTest` | HiveMQ CE |

## Running

Requires a container runtime — the tests are annotated `@Testcontainers(disabledWithoutDocker = true)` and are skipped
without Docker:

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-tck test
```

## Related Modules

- `dc3-mq-core` — the port being verified
- `dc3-mq-*` — the adapters under test
