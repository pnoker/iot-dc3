# DC3 MQ Kafka

`dc3-mq-kafka` adapts the broker-neutral port to Apache Kafka (`spring-kafka`). Logical topics map to Kafka topics,
`Dc3Listener` subscriptions become Kafka listener registrations, and publishing uses producer records with
confirmations.

## Activation

Active when `dc3.mq.type=kafka`.

## Configuration

| Key                              | Default                                                                                | Meaning     |
|----------------------------------|----------------------------------------------------------------------------------------|-------------|
| `dc3.mq.kafka.bootstrap-servers` | `DC3_MQ_KAFKA_BOOTSTRAP`, then `spring.kafka.bootstrap-servers`, then `localhost:9092` | broker list |

## Dependencies

`dc3-mq-core`, `spring-kafka`, `dc3-common-constant`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-kafka -am package
```

## Testing

No module-specific tests; behaviour is verified by `KafkaContractTest` in `dc3-mq-tck` (disposable
`apache/kafka:3.9.0` container).

## Related Modules

- `dc3-mq` — broker-neutral port family
- `dc3-mq-tck` — contract suite
- `docs/mq-brokers.md` — broker selection guide
