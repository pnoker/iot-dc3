# DC3 MQ

dc3-mq is the pluggable message-broker layer of IoT DC3. It defines a broker-neutral messaging port — logical topics,
subscriptions, delivery envelopes, and capability negotiation — plus one certified adapter per supported broker.
Business code (centers, drivers, common modules) depends on the port and never on broker-specific classes.

## Modules

| Module | Role |
|---|---|
| dc3-mq-core | broker-neutral port: BrokerAdapter SPI, Dc3Listener/MqListener annotations, MessageSender, envelopes, retry policy, and batch-consumer properties (dc3.data.point.batch) |
| dc3-mq-rabbitmq | RabbitMQ adapter (default); physical topology is byte-for-byte identical to the legacy RabbitConstant names |
| dc3-mq-kafka | Kafka adapter |
| dc3-mq-rocketmq | RocketMQ adapter |
| dc3-mq-pulsar | Pulsar adapter |
| dc3-mq-activemq | ActiveMQ (Artemis / Classic, JMS 2.0) adapter |
| dc3-mq-mqtt | MQTT 5 adapter (EMQX / HiveMQ / NanoMQ / ...) |
| dc3-mq-tck | broker-neutral contract suite: an adapter that passes these tests is compliant |

## Selection

The active broker is chosen by the dc3.mq.type property (default rabbitmq). Only the selected adapter's
auto-configuration is active; the others remain dormant on the classpath.

```yaml
dc3:
  mq:
    type: rabbitmq
```

Logical topics (MqTopic) are mapped to broker-specific exchange/queue/topic names by each adapter; the RabbitMQ adapter
keeps the legacy names such as dc3.e.value and dc3.q.value.point, so existing deployments and the dc3.rabbit.tag
prefixing behaviour keep working unchanged.

## Build and verify

```bash
mvn -s .mvn/settings.xml -q -f dc3-mq/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-mq/pom.xml test
```

Adapter selection guides and trade-offs live in docs/mq-brokers.md.
