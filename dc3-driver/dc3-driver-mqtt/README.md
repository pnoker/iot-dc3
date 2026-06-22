# DC3 Driver MQTT

## Overview

`dc3-driver-mqtt` is the MQTT protocol driver of the IoT DC3 platform. It is intended to subscribe to configured MQTT
topics, parse incoming payloads as device point values, and forward commands to devices via MQTT publish.

> ⚠️ **Work in progress.** This driver is currently a skeleton — protocol-level I/O is not yet fully implemented
> (`health()` always reports online and `read()` is a reference stub; see the TODO markers in
> `MqttDriverCustomServiceImpl`). Treat it as a starting template, not a production-ready driver.

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

## Command Attributes (write)

| Attribute        | Description                                 |
|------------------|---------------------------------------------|
| Command Topic    | MQTT topic for sending write commands       |
| Command QoS      | MQTT QoS level for command messages (0/1/2) |
| Payload Template | Template for the command payload            |

## Event Attributes

| Attribute       | Description                       |
|-----------------|-----------------------------------|
| Source Topic    | MQTT topic to receive events from |
| Event Code Path | Path to the event code in payload |
| Payload Path    | Path to the event payload         |

## Prerequisites

An MQTT broker must be running. The dev profile connects to the RabbitMQ MQTT plugin (`dc3-rabbitmq:2883`),
which ships with the base stack:

```bash
podman compose -f dc3/docker-compose-db.yml up -d
```

EMQX is available as an alternative via the optional stack (`docker-compose-optional.yml`, port `31883`); point
`MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` at whichever broker you use.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
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
