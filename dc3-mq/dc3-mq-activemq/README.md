# DC3 MQ ActiveMQ

`dc3-mq-activemq` adapts the broker-neutral port to ActiveMQ over JMS 2.0 (`artemis-jakarta-client`), covering both
ActiveMQ Artemis and ActiveMQ Classic brokers.

## Activation

Active when `dc3.mq.type=activemq`.

## Configuration

| Key | Default | Meaning |
|---|---|---|
| `dc3.mq.activemq.url` | `tcp://localhost:61616` | broker connection URL |
| `dc3.mq.activemq.user` | *(empty)* | login name |
| `dc3.mq.activemq.password` | *(empty)* | login password |

## Dependencies

`dc3-mq-core`, `artemis-jakarta-client`, `dc3-common-constant`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-activemq -am package
```

## Testing

No module-specific tests; behaviour is verified by `ActiveMqContractTest` in `dc3-mq-tck`.

## Related Modules

- `dc3-mq` — broker-neutral port family
- `dc3-mq-tck` — contract suite
- `docs/mq-brokers.md` — broker selection guide
