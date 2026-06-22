# DC3 Driver Zigbee

## Overview

`dc3-driver-zigbee` is the Zigbee driver of the IoT DC3 platform. It is intended to manage a Zigbee network through a
serial coordinator dongle (using the ZSmartSystems Zigbee library), read point values from Zigbee devices via ZCL
attributes addressed by node IEEE address / endpoint / cluster / attribute, and write values to ZCL attributes.

> ⚠️ **Work in progress.** This driver is currently a skeleton — protocol-level I/O is not yet fully implemented.
> The implementation (`ZigbeeDriverCustomServiceImpl`) carries an explicit "work-in-progress skeleton" warning and
> multiple `TODO` markers: `initial()` hardcodes the serial port (`/dev/ttyUSB0`) and baud rate (`115200`) instead of
> reading them from the driver configuration; the device-level `health()` returns `online` unconditionally; and the
> write path (`writeAttribute`) only logs and never actually writes the ZCL attribute. Treat it as a starting
> template, not a production-ready driver.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-zigbee
- **Version**: 2026.5.22
- **Driver Name**: Zigbee Driver

## Driver Attributes (Device-level)

| Attribute   | Code       | Type   | Default      | Description                                |
|-------------|------------|--------|--------------|--------------------------------------------|
| Serial Port | serialPort | STRING | /dev/ttyUSB0 | Zigbee coordinator serial port             |
| Baud Rate   | baudRate   | INT    | 115200       | Serial port baud rate                      |
| Dongle Type | dongleType | STRING | TELEGESIS    | Coordinator dongle type (TELEGESIS, EMBER, CONBEE) |
| PAN ID      | panId      | INT    | 0            | PAN ID (0=auto)                            |
| Channel     | channel    | INT    | 0            | Channel (0=auto, 11-26)                    |

## Point Attributes

| Attribute         | Code            | Type   | Default | Description                                          |
|-------------------|-----------------|--------|---------|------------------------------------------------------|
| Node IEEE Address | nodeIeeeAddress | STRING |         | Zigbee node IEEE address (e.g. 00158D0001234567)     |
| Endpoint ID       | endpointId      | INT    | 1       | Endpoint ID                                          |
| Cluster ID        | clusterId       | INT    | 0       | Cluster ID (e.g. 1026=Temperature Measurement)       |
| Attribute ID      | attributeId     | INT    | 0       | Attribute ID (e.g. 0=Measured Value)                 |

## Command Attributes (write)

| Attribute         | Code            | Type   | Default | Description               |
|-------------------|-----------------|--------|---------|---------------------------|
| Node IEEE Address | nodeIeeeAddress | STRING |         | Zigbee node IEEE address  |
| Endpoint ID       | endpointId      | INT    | 1       | Endpoint ID               |
| Cluster ID        | clusterId       | INT    | 0       | Cluster ID for writing    |
| Attribute ID      | attributeId     | INT    | 0       | Attribute ID for writing  |

## Prerequisites

A Zigbee coordinator dongle connected to a serial port on the host running the driver. The dependencies bundle the
Telegesis dongle adapter; note that `initial()` currently hardcodes the serial port and baud rate rather than reading
the driver attributes above (see the work-in-progress warning).

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-zigbee/target/dc3-driver-zigbee.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
