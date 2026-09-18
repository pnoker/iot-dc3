# DC3 MQ

dc3-mq is the pluggable message-broker layer of IoT DC3. It defines a broker-neutral messaging port — logical topics,
subscriptions, delivery envelopes, and capability negotiation — plus one certified adapter per supported broker.
Business code (centers, drivers, common modules) depends on the port and never on broker-specific classes.

## Modules

| Module          | Role                                                                                                                                                                     |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| dc3-mq-core     | broker-neutral port: BrokerAdapter SPI, Dc3Listener/MqListener annotations, MessageSender, envelopes, retry policy, and batch-consumer properties (dc3.data.point.batch) |
| dc3-mq-rabbitmq | RabbitMQ adapter (default)                                                                                                                                    |
| dc3-mq-kafka    | Kafka adapter                                                                                                                                                            |
| dc3-mq-pulsar   | Pulsar adapter                                                                                                                                                           |
| dc3-mq-mqtt     | MQTT 5 adapter (EMQX / HiveMQ / NanoMQ / ...)                                                                                                                            |
| dc3-mq-tck      | broker-neutral contract suite: an adapter that passes these tests is compliant                                                                                           |

## Selection

The active broker is chosen by the dc3.mq.type property (default rabbitmq). Only the selected adapter's
auto-configuration is active; the others remain dormant on the classpath.

```yaml
dc3:
  mq:
    type: rabbitmq
```

Logical topics (`MqTopic`) are mapped to broker-specific exchange/queue/topic names by each adapter. Physical names are
implementation details and must not be referenced by business modules.

## Build and verify

```bash
mvn -s .mvn/settings.xml -q -f dc3-mq/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-mq/pom.xml test
```

Adapter selection guides and trade-offs live in docs/mq-brokers.md.
