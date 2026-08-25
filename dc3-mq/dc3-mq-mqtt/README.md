# DC3 MQ MQTT

`dc3-mq-mqtt` adapts the broker-neutral port to MQTT 5 (`hivemq-mqtt-client`), compatible with EMQX, HiveMQ,
NanoMQ, and other MQTT 5 brokers.

## Activation

Active when `dc3.mq.type=mqtt`.

## Configuration

| Key | Default | Meaning |
|---|---|---|
| `dc3.mq.mqtt.host` | `localhost` | broker host |
| `dc3.mq.mqtt.port` | `1883` | broker port |

## Dependencies

`dc3-mq-core`, `hivemq-mqtt-client`, `dc3-common-constant`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-mqtt -am package
```

## Testing

No module-specific tests; behaviour is verified by `MqttContractTest` in `dc3-mq-tck` (disposable HiveMQ CE
container).

## Related Modules

- `dc3-mq` — broker-neutral port family
- `dc3-mq-tck` — contract suite
- `docs/mq-brokers.md` — broker selection guide
