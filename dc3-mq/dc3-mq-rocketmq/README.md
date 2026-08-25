# DC3 MQ RocketMQ

`dc3-mq-rocketmq` adapts the broker-neutral port to Apache RocketMQ (`rocketmq-client`). Logical topics map to
RocketMQ topics; publish and subscription use the standard producer/consumer APIs with the port's confirmation model.

## Activation

Active when `dc3.mq.type=rocketmq`.

## Configuration

| Key | Default | Meaning |
|---|---|---|
| `dc3.mq.rocketmq.name-server-address` | `localhost:9876` | NameServer address |

## Dependencies

`dc3-mq-core`, `rocketmq-client`, `dc3-common-constant`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-rocketmq -am package
```

## Testing

No module-specific tests; behaviour is verified by `RocketMqContractTest` in `dc3-mq-tck`.

## Related Modules

- `dc3-mq` — broker-neutral port family
- `dc3-mq-tck` — contract suite
- `docs/mq-brokers.md` — broker selection guide
