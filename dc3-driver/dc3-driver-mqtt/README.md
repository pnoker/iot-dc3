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

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

An MQTT broker must be running. The dev profile connects to the RabbitMQ MQTT plugin (`dc3-rabbitmq:2883`), which ships
with the base stack:

```bash
make up-db
```

EMQX is available as an alternative via the optional stack (`docker-compose-optional.yml`, port `31883`); point
`MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` at whichever broker you use.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-mqtt -am package
java -jar dc3-driver/dc3-driver-mqtt/target/dc3-driver-mqtt.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-mqtt -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
- `dc3-common-mqtt` — MQTT client configuration and utilities
