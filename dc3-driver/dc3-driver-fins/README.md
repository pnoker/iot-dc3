# DC3 Driver FINS

## Overview

`dc3-driver-fins` is the Omron FINS protocol driver of the IoT DC3 platform. It communicates with Omron PLCs via
the FINS protocol over TCP sockets, building FINS frames manually with proper headers and memory read/write
commands. It supports the D, W, H, and C memory areas. No external protocol library is used.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-fins
- **Version**: 2026.5.22
- **Driver Name**: Omron FINS Driver

## Driver Attributes (Device-level)

| Attribute   | Description                     |
|-------------|---------------------------------|
| Host        | PLC host address                |
| Port        | FINS port (default 9600)        |
| Protocol    | Transport protocol (e.g. TCP)   |
| Source Node | FINS source node number         |
| Dest Node   | FINS destination node number    |
| Source Unit | FINS source unit number         |
| Dest Unit   | FINS destination unit number    |
| Timeout     | Request timeout in milliseconds |

## Point Attributes

| Attribute    | Description                                          |
|--------------|------------------------------------------------------|
| Memory Area  | Memory area (D / W / H / C)                          |
| Address      | Word address within the memory area                  |
| Data Type    | INT16 / UINT16 / INT32 / UINT32 / FLOAT / STRING / BCD |
| Bit Position | Bit position within the word                         |

## Command Attributes (write)

| Attribute   | Description                  |
|-------------|------------------------------|
| Memory Area | Memory area (D / W / H / C)  |
| Address     | Word address within the area |
| Data Type   | Value data type              |

## Prerequisites

A reachable Omron PLC speaking FINS over TCP, typically on port 9600.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-fins/target/dc3-driver-fins.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
