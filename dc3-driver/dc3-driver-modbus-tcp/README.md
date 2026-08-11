# DC3 Driver Modbus TCP

## Overview

`dc3-driver-modbus-tcp` is the Modbus TCP protocol driver of the IoT DC3 platform. It connects to Modbus TCP slave
devices, reads coil/register values periodically, and supports write commands for register control.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-modbus-tcp
- **Driver Name**: Modbus TCP Driver

## Driver Attributes (Device-level)

| Attribute | Description                     |
|-----------|---------------------------------|
| Host      | Modbus slave IP address         |
| Port      | Modbus TCP port (typically 502) |

## Point Attributes

| Attribute     | Description                        |
|---------------|------------------------------------|
| Slave ID      | Modbus slave unit ID               |
| Function Code | Modbus function code (01/02/03/04) |
| Offset        | Register/coil address offset       |

## Command Attributes (write)

| Attribute      | Description                     |
|----------------|---------------------------------|
| Slave ID       | Modbus slave unit ID            |
| Function Code  | Modbus function code            |
| Offset         | Register/coil address offset    |
| Value Template | Template for the value to write |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and
local buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A running Modbus TCP slave device or simulator accessible on the network.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-modbus-tcp -am package
java -jar dc3-driver/dc3-driver-modbus-tcp/target/dc3-driver-modbus-tcp.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-modbus-tcp -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
