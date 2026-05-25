# DC3 Driver MQTT

## Overview

`dc3-driver-mqtt` is the MQTT protocol driver of the IoT DC3 platform. It subscribes to configured MQTT topics, parses
incoming payloads as device point values, and forwards
commands to devices via MQTT publish.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-mqtt
- **Version**: 2026.5.22
- **Driver Name**: MQTT Driver

## Point Attributes

| Attribute     | Description                                 |
|---------------|---------------------------------------------|
| Command Topic | MQTT topic for sending write commands       |
| Command QoS   | MQTT QoS level for command messages (0/1/2) |

## Prerequisites

An MQTT broker (EMQX recommended) must be running:

```bash
podman compose -f dc3/docker-compose-optional.yml up -d
# EMQX MQTT broker on port 31883
```

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
podman compose -f dc3/docker-compose-optional.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-mqtt/target/dc3-driver-mqtt.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration and RabbitMQ integration
- `dc3-common-mqtt` — MQTT client configuration and utilities

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
