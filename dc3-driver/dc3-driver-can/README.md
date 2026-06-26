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
- **Version**: 2026.5.22
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

## Prerequisites

A Linux host with an available SocketCAN interface (e.g. `can0`) and the `can-utils` package installed
(`candump`, `cansend`), reachable from the driver process.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-can/target/dc3-driver-can.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
