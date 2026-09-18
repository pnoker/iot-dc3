# DC3 Driver CAN

## Overview

`dc3-driver-can` is the CAN bus driver of the IoT DC3 platform. It is intended to read and write CAN frames on a
SocketCAN interface, parsing frame payloads into device point values and sending command frames to the bus.

> ⚠️ **Work in progress.** This driver is a skeleton — its class documentation explicitly states "Protocol-level
> I/O is not yet fully implemented" and there are TODO markers in the method bodies (e.g. native SocketCAN JNI
> integration is not done). The current `read()`/`write()` path shells out to Linux `can-utils`
> (`candump`/`cansend`) and `health()` checks the interface via `ip link show`. Treat it as a starting template,
> not a production-ready driver.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-can
- **Driver Name**: CAN Bus Driver

## Driver Attributes (Device-level)

| Attribute    | Description                            |
|--------------|----------------------------------------|
| Interface    | CAN interface name (e.g. can0)         |
| Bitrate      | CAN bus bitrate                        |
| Frame Format | STANDARD (11-bit) or EXTENDED (29-bit) |

## Point Attributes

| Attribute      | Description                           |
|----------------|---------------------------------------|
| CAN ID         | CAN identifier to match               |
| Data Offset    | Byte offset within the frame payload  |
| Data Length    | Number of bytes to read               |
| Data Format    | INT / UINT / HEX                      |
| Byte Order     | Byte order (e.g. LITTLE)              |
| Request CAN ID | CAN ID for an optional request frame  |
| Request Data   | Payload of the optional request frame |

## Command Attributes (write)

| Attribute | Description                      |
|-----------|----------------------------------|
| CAN ID    | CAN identifier to write to       |
| Data      | Frame data (supports `${value}`) |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A Linux host with an available SocketCAN interface (e.g. `can0`) and the `can-utils` package installed (`candump`,
`cansend`), reachable from the driver process.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-can -am package
java -jar dc3-driver/dc3-driver-can/target/dc3-driver-can.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-can -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
