# DC3 Common MQTT

## Overview

`dc3-common-mqtt` is the shared MQTT module of the IoT DC3 platform. It provides auto-configuration (via Spring
Integration and the Eclipse Paho client) for MQTT connection, inbound message handling, and topic subscription, used by
the MQTT driver and any service requiring MQTT connectivity.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-mqtt

## Key Components

| Component               | Purpose                                                                                 |
|-------------------------|-----------------------------------------------------------------------------------------|
| `MqttConfig`            | `@AutoConfiguration` wiring Spring Integration MQTT channels and a Paho inbound adapter |
| `MqttProperties`        | Binds MQTT properties from YAML (prefix `dc3.driver.mqtt`)                              |
| `MqttEnvironmentConfig` | `EnvironmentPostProcessor` that loads MQTT environment defaults                         |
| `MqttUtil`              | Builds Paho `MqttConnectOptions` from `MqttProperties`                                  |
| `MqttInitRunner`        | Startup runner for MQTT initialization                                                  |
| `MqttScheduleService`   | Batches received MQTT messages on a Quartz schedule                                     |

## Configuration Properties

Configure in `application*.yml` under the `dc3.driver.mqtt` prefix. The shared `application-mqtt.yml` ships
these defaults:

```yaml
dc3:
  driver:
    mqtt:
      ca-crt: classpath:/certs/ca.crt
      client-crt: classpath:/certs/client.crt
      client-key: classpath:/certs/client.key
      client-key-pass: dc3-client
      topic-prefix: dc3/${dc3.driver.tenant}/${spring.application.name}/
      default-send-topic:
        qos: 1
        name: command
      keep-alive: 15
      completion-timeout: 3000
      batch:
        speed: ${MQTT_BATCH_SPEED:100}
        interval: ${MQTT_BATCH_INTERVAL:5}
```

Connection settings such as `url` (dev default `tcp://${MQTT_BROKER_HOST:dc3-rabbitmq}:${MQTT_BROKER_PORT:2883}`),
`auth-type` (`NONE` | `USERNAME` | `CLIENT`), `username`, `password`, and `receive-topics` are supplied by the
consumer application (e.g. `dc3-driver-mqtt`).

## Usage

This module is activated when the `mqtt` profile is included or MQTT-related auto-configuration is on the classpath. The
MQTT driver (`dc3-driver-mqtt`) depends on this module as its primary integration layer.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-mqtt -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-mqtt -am test
```

## Related Modules

- `dc3-driver-mqtt` — Primary consumer of this module
- MQTT broker: the dev profile points at the RabbitMQ MQTT plugin (`dc3-rabbitmq:2883`); EMQX is also available via the
  optional stack (`make up-optional`, port `31883`)
