# DC3 Driver LoRaWAN

## Overview

`dc3-driver-lorawan` ingests LoRaWAN uplinks by subscribing to ChirpStack MQTT topics
(`application/+/device/+/event/up`). It decodes the JSON payload, caching the latest FRMPayload (base64) and Cayenne
LPP object fields per DevEUI. Reads resolve a point against the cached DevEUI; writes publish a downlink command to
the ChirpStack `command/down` topic.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-lorawan
- **Driver Name**: LoRaWAN Driver

## Driver Attributes (Device-level)

| Attribute       | Code          | Type   | Default                        | Description                            |
|-----------------|---------------|--------|--------------------------------|----------------------------------------|
| Application ID  | applicationId | STRING |                                | ChirpStack application ID for downlink |
| Broker URI      | brokerUri     | STRING | tcp://dc3-mqtt:1883            | MQTT broker URI for ChirpStack events  |
| Subscribe Topic | topic         | STRING | application/+/device/+/event/up| MQTT uplink topic filter               |
| Username        | username      | STRING |                                | MQTT broker username (optional)        |
| Password        | password      | STRING |                                | MQTT broker password (optional)        |

## Point Attributes

| Attribute | Code   | Type   | Default | Description                                            |
|-----------|--------|--------|---------|--------------------------------------------------------|
| DevEUI    | devEui | STRING |         | LoRaWAN device EUI (16 hex characters)                 |
| Field     | field  | STRING |         | Cayenne LPP object field; empty returns raw base64     |

## Command Attributes (write)

| Attribute | Code   | Type   | Default | Description                          |
|-----------|--------|--------|---------|--------------------------------------|
| DevEUI    | devEui | STRING |         | Target device EUI for the downlink   |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A ChirpStack instance publishing uplinks to the configured MQTT broker topic
(`application/+/device/+/event/up`).

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-lorawan -am package
java -jar dc3-driver/dc3-driver-lorawan/target/dc3-driver-lorawan.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-lorawan -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
