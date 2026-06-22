# DC3 Common MQTT

## Overview

`dc3-common-mqtt` is the shared MQTT module of the IoT DC3 platform. It provides auto-configuration (via Spring
Integration and the Eclipse Paho client) for MQTT connection, inbound message handling, and topic subscription,
used by the MQTT driver and any service requiring MQTT connectivity.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-mqtt
- **Version**: 2026.5.22

## Key Components

| Component               | Purpose                                                                                |
|-------------------------|----------------------------------------------------------------------------------------|
| `MqttConfig`            | `@AutoConfiguration` wiring Spring Integration MQTT channels and a Paho inbound adapter |
| `MqttProperties`        | Binds MQTT properties from YAML (prefix `dc3.driver.mqtt`)                              |
| `MqttEnvironmentConfig` | `EnvironmentPostProcessor` that loads MQTT environment defaults                         |
| `MqttUtil`              | Builds Paho `MqttConnectOptions` from `MqttProperties`                                  |
| `MqttInitRunner`        | Startup runner for MQTT initialization                                                  |
| `MqttScheduleService`   | Batches received MQTT messages on a Quartz schedule                                     |

## Configuration Properties

Configure in `application*.yml` under the `dc3.driver.mqtt` prefix:

```yaml
dc3:
  driver:
    mqtt:
      url: tcp://${MQTT_BROKER_HOST:dc3-rabbitmq}:${MQTT_BROKER_PORT:2883}
      auth-type: USERNAME            # NONE | USERNAME | CLIENT
      username: ${MQTT_USERNAME:dc3}
      password: ${MQTT_PASSWORD:}
      topic-prefix: dc3/${dc3.driver.tenant}/${spring.application.name}/
      receive-topics:
        - name: data
          qos: 1
```

## Usage

This module is activated when the `mqtt` profile is included or MQTT-related auto-configuration is on the classpath. The
MQTT driver (`dc3-driver-mqtt`) depends on this module as
its primary integration layer.

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-driver-mqtt` — Primary consumer of this module
- MQTT broker: the dev profile points at the RabbitMQ MQTT plugin (`dc3-rabbitmq:2883`); EMQX is also available via the optional stack (`podman compose -f dc3/docker-compose-optional.yml up -d`, port `31883`)

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

