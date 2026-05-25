# DC3 Common MQTT

## Overview

`dc3-common-mqtt` is the shared MQTT client configuration module of the IoT DC3 platform. It provides auto-configuration
for MQTT connection, message handling, and topic
subscription used by the MQTT driver and any service requiring MQTT connectivity.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-mqtt
- **Version**: 2026.5.22

## Key Components

| Component               | Purpose                                                      |
|-------------------------|--------------------------------------------------------------|
| `MqttConfig`            | Auto-configures `MqttClient` based on environment properties |
| `MqttEnvironmentConfig` | Binds MQTT connection properties from YAML                   |
| `MqttUtil`              | Utility methods for publishing/subscribing MQTT messages     |
| `MqttInitRunner`        | Startup runner for MQTT connection initialization            |

## Configuration Properties

Configure in `application*.yml`:

```yaml
mqtt:
  host: ${MQTT_HOST:dc3-emqx}
  port: ${MQTT_PORT:1883}
  username: ${MQTT_USERNAME:dc3}
  password: ${MQTT_PASSWORD:dc3dc3dc3}
  qos: 1
  client-id: ${spring.application.name}
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
- For MQTT broker: use EMQX via `podman compose -f dc3/docker-compose-optional.yml up -d` (port `31883`)

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

