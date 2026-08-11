# DC3 Driver Modbus RTU

## Overview

`dc3-driver-modbus-rtu` is the Modbus RTU protocol driver of the IoT DC3 platform. It connects to Modbus slave devices
over a serial port (via `modbus4j` and `jSerialComm`), reads coil/register values using function codes 1-4, and writes
values to coils and holding registers.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-modbus-rtu
- **Driver Name**: Modbus RTU Driver

## Driver Attributes (Device-level)

| Attribute | Description                                     |
|-----------|-------------------------------------------------|
| Port      | Serial port name (e.g. /dev/ttyUSB0, COM3)      |
| Baud Rate | Serial baud rate (e.g. 9600, 19200, 115200)     |
| Data Bits | Data bits (7 or 8)                              |
| Stop Bits | Stop bits (1 or 2)                              |
| Parity    | Parity (0=None, 1=Odd, 2=Even, 3=Mark, 4=Space) |

## Point Attributes

| Attribute     | Description                       |
|---------------|-----------------------------------|
| Slave ID      | Modbus slave unit ID              |
| Function Code | Modbus function code [1, 2, 3, 4] |
| Offset        | Register or coil address offset   |

## Command Attributes (write)

| Attribute      | Description                                 |
|----------------|---------------------------------------------|
| Slave ID       | Modbus slave unit ID                        |
| Function Code  | Modbus write function code [5, 6, 15, 16]   |
| Offset         | Register or coil address offset             |
| Value Template | Value template rendered with command params |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and
local buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A Modbus RTU slave device connected to an accessible serial port (e.g. `/dev/ttyUSB0`, `COM3`) on the host running the
driver.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-modbus-rtu -am package
java -jar dc3-driver/dc3-driver-modbus-rtu/target/dc3-driver-modbus-rtu.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-modbus-rtu -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
