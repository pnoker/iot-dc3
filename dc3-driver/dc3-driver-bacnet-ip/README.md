# DC3 Driver BACnet IP

## Overview

`dc3-driver-bacnet-ip` is the BACnet/IP protocol driver of the IoT DC3 platform. It uses the BACnet4J library to
communicate with BACnet/IP devices over UDP, reading and writing object properties addressed by remote device
instance number, object type, object instance, and property identifier.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-bacnet-ip
- **Version**: 2026.5.22
- **Driver Name**: BACnet IP Driver

## Driver Attributes (Device-level)

| Attribute         | Description                                       |
|-------------------|---------------------------------------------------|
| Local Device ID   | Local BACnet device instance number               |
| Bind Address      | Local bind address                                |
| Port              | BACnet UDP port (default 47808 = 0xBAC0)          |
| Broadcast Address | Broadcast address for device discovery            |
| Timeout           | Request timeout in milliseconds                   |

## Point Attributes

| Attribute       | Description                                                          |
|-----------------|----------------------------------------------------------------------|
| Remote Device ID | Remote BACnet device instance number                                |
| Object Type     | BACnet object type (ANALOG_INPUT, ANALOG_OUTPUT, BINARY_INPUT, etc.) |
| Object Instance | Object instance number                                               |
| Property ID     | Property identifier (PRESENT_VALUE, DESCRIPTION, STATUS_FLAGS, etc.) |

## Command Attributes (write)

| Attribute        | Description                            |
|------------------|----------------------------------------|
| Remote Device ID | Remote BACnet device instance number   |
| Object Type      | BACnet object type for writing         |
| Object Instance  | Object instance number                 |
| Property ID      | Property identifier                    |

## Prerequisites

A reachable BACnet/IP device (or simulator) on the network. Communication uses UDP, typically on port 47808.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-bacnet-ip/target/dc3-driver-bacnet-ip.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
