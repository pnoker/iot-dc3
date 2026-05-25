# DC3 Driver Modbus TCP

## Overview

`dc3-driver-modbus-tcp` is the Modbus TCP protocol driver of the IoT DC3 platform. It connects to Modbus TCP slave
devices, reads coil/register values periodically, and supports
write commands for register control.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-modbus-tcp
- **Version**: 2026.5.22
- **Driver Name**: Modbus TCP Driver

## Driver Attributes (Device-level)

| Attribute | Description                     |
|-----------|---------------------------------|
| Host      | Modbus slave IP address         |
| Port      | Modbus TCP port (typically 502) |
| Slave ID  | Modbus slave unit ID            |

## Point Attributes

| Attribute     | Description                        |
|---------------|------------------------------------|
| Function Code | Modbus function code (01/02/03/04) |
| Offset        | Register/coil address offset       |

## Prerequisites

A running Modbus TCP slave device or simulator accessible on the network.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-modbus-tcp/target/dc3-driver-modbus-tcp.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
