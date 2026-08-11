# DC3 Common RabbitMQ

## Overview

`dc3-common-rabbitmq` is the shared RabbitMQ infrastructure module of the IoT DC3 platform. It defines the durable
platform exchanges, connection factory, message conversion, and profile activation used by services and drivers.
Domain modules own their queues and bindings.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-rabbitmq

## Key Components

| Component                   | Purpose                                                              |
|-----------------------------|----------------------------------------------------------------------|
| `ExchangeConfig`            | Declares the shared durable topic exchanges                           |
| `RabbitConfig`              | Connection factory and Jackson-based message converter configuration |
| `RabbitmqEnvironmentConfig` | Loads RabbitMQ environment defaults                                   |
| `ActiveRabbitProfileConfig` | Activates the `rabbit` profile unless explicitly disabled             |

## Topic Exchanges

| Exchange bean(s) | Base exchange name(s) | Purpose |
|---|---|---|
| `stateExchange`, `alarmExchange` | `dc3.e.state`, `dc3.e.alarm` | Driver/device state and alarm processing |
| `metadataExchange` | `dc3.e.metadata` | Metadata change broadcast to drivers |
| `pointCommandExchange` | `dc3.e.point_command` | Point read/write commands |
| `valueExchange` | `dc3.e.value` | Point values from drivers to Data Center |
| `mqttExchange` | `dc3.e.mqtt` | MQTT-to-platform message bridging |
| `stateTimeoutDelayExchange`, `stateTimeoutCheckExchange` | `dc3.e.state_timeout_delay`, `dc3.e.state_timeout_check` | Delayed state-timeout checks |
| `commandExchange`, `commandResultExchange`, `commandDeadExchange` | `dc3.e.command`, `dc3.e.command_result`, `dc3.e.command_dead` | Custom commands, results, and dead letters |
| `eventExchange` | `dc3.e.event` | Reported domain events |

`ExchangeConfig` currently declares 12 shared exchanges. Data and driver modules add queues, bindings, and specialized
dead-letter/result exchanges. The optional `dc3.rabbit.tag` system property prefixes runtime names, so use
`RabbitConstant` rather than duplicating literal names in code.

## Configuration Properties

```yaml
spring:
  rabbitmq:
    virtual-host: ${RABBITMQ_VIRTUAL_HOST:dc3}
    host: ${RABBITMQ_HOST:dc3-rabbitmq}
    port: ${RABBITMQ_PORT:35672}
    username: ${RABBITMQ_USERNAME:dc3}
    password: ${RABBITMQ_PASSWORD:dc3dc3dc3}
    ssl:
      enabled: ${RABBITMQ_SSL_ENABLED:false}
      algorithm: ${RABBITMQ_SSL_ALGORITHM:TLS}
      validate-server-certificate: ${RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE:false}
      verify-hostname: ${RABBITMQ_SSL_VERIFY_HOSTNAME:false}
```

To use TLS, set `RABBITMQ_SSL_ENABLED=true` and switch the RabbitMQ port to `5671` in Compose or `35671` for local
source runs. Server certificate validation is intentionally left to deployment configuration: provide a Java truststore
and set Spring Boot's native `spring.rabbitmq.ssl.trust-store`, `trust-store-type`, and
`trust-store-password` properties when `RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE=true`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-rabbitmq -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-rabbitmq -am test
```

## Related Modules

- `dc3-common-constant` / `RabbitConstant` — All exchange/queue/routing key names
- `dc3-common-data` — Consumes values/state/events and publishes point/custom commands
- `dc3-common-manager` — Publishes to `dc3.e.metadata`
- `dc3-common-driver` — Consumes metadata and point/custom commands; publishes values and command results
