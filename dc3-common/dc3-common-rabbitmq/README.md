# DC3 Common RabbitMQ

## Overview

`dc3-common-rabbitmq` is the shared RabbitMQ configuration module of the IoT DC3 platform. It defines all topic
exchanges, queue bindings, and connection configuration used for
asynchronous communication between services and drivers.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-rabbitmq
- **Version**: 2026.5.22

## Key Components

| Component                   | Purpose                                                              |
|-----------------------------|----------------------------------------------------------------------|
| `ExchangeConfig`            | Declares all 5 topic exchanges as persistent Spring beans            |
| `RabbitConfig`              | Connection factory and Jackson-based message converter configuration |
| `RabbitmqEnvironmentConfig` | Binds RabbitMQ connection properties from environment variables      |
| `ActiveRabbitProfileConfig` | Profile-conditional activation                                       |

## Topic Exchanges

| Exchange Bean      | Exchange Name    | Purpose                                     |
|--------------------|------------------|---------------------------------------------|
| `eventExchange`    | `dc3.e.event`    | Driver/device status events (load-balanced) |
| `metadataExchange` | `dc3.e.metadata` | Metadata change broadcast to drivers        |
| `commandExchange`  | `dc3.e.command`  | Commands from data service to drivers       |
| `valueExchange`    | `dc3.e.value`    | Point values from drivers to data center    |
| `mqttExchange`     | `dc3.e.mqtt`     | MQTT-to-platform message bridging           |

## Configuration Properties

```yaml
spring:
  rabbitmq:
    virtual-host: ${RABBITMQ_VIRTUAL_HOST:dc3}
    host: ${RABBITMQ_HOST:dc3-rabbitmq}
    port: ${RABBITMQ_PORT:35672}
    username: ${RABBITMQ_USERNAME:dc3}
    password: ${RABBITMQ_PASSWORD:dc3dc3dc3}
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-constant` / `RabbitConstant` — All exchange/queue/routing key names
- `dc3-common-data` — Consumes `dc3.e.value`, publishes to `dc3.e.command`
- `dc3-common-manager` — Publishes to `dc3.e.metadata`
- `dc3-common-driver` — Consumes from `dc3.e.metadata`, `dc3.e.command`; publishes to `dc3.e.value`

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

