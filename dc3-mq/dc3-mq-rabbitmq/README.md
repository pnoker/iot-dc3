# DC3 MQ RabbitMQ

`dc3-mq-rabbitmq` adapts the broker-neutral port to RabbitMQ. Its physical topology is byte-for-byte identical to the
legacy names (`dc3.e.value`, `dc3.q.value.point`, ...), so existing deployments and the `dc3.rabbit.tag` prefixing
behaviour keep working unchanged.

## Activation

Active when `dc3.mq.type=rabbitmq` — the default (`matchIfMissing = true`).

## Key types

| Type | Role |
|---|---|
| `RabbitMqAdapter` | `BrokerAdapter` implementation (exchanges, queues, listeners, confirms) |
| `RabbitNames` / `RabbitTopology` | canonical exchange/queue/routing names and bindings |
| `RabbitAcknowledgment` | publisher-confirm handling |
| `ActiveRabbitProfileConfig` / `RabbitEnvironmentConfig` | profile wiring and environment defaults |

## Configuration

Shared connection defaults live in `application-rabbitmq.yml`; runtime addresses use the standard
`spring.rabbitmq.*` properties. The optional `dc3.rabbit.tag` system property prefixes exchange, queue, and routing
names for isolated environments.

## Dependencies

`dc3-mq-core`, `spring-boot-starter-amqp`, `dc3-common-constant`, `dc3-common-public`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-rabbitmq -am package
```

## Testing

No module-specific tests; behaviour is verified by `RabbitMqContractTest` in `dc3-mq-tck`.

## Related Modules

- `dc3-mq` — broker-neutral port family
- `dc3-mq-tck` — contract suite
- `docs/mq-brokers.md` — broker selection guide
