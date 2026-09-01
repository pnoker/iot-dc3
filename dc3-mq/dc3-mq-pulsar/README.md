# DC3 MQ Pulsar

`dc3-mq-pulsar` adapts the broker-neutral port to Apache Pulsar (`pulsar-client`). Logical topics map to Pulsar topics;
publish and subscription use the standard client APIs with the port's confirmation model.

## Activation

Active when `dc3.mq.type=pulsar`.

## Configuration

| Key                         | Default                   | Meaning            |
|-----------------------------|---------------------------|--------------------|
| `dc3.mq.pulsar.service-url` | `pulsar://localhost:6650` | broker service URL |

## Dependencies

`dc3-mq-core`, `pulsar-client`, `dc3-common-constant`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-pulsar -am package
```

## Testing

No module-specific tests; behaviour is verified by `PulsarContractIT` in `dc3-mq-tck`.

## Related Modules

- `dc3-mq` — broker-neutral port family
- `dc3-mq-tck` — contract suite
- `docs/mq-brokers.md` — broker selection guide
