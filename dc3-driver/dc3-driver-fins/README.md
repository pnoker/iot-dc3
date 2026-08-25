# DC3 Driver FINS

## Overview

`dc3-driver-fins` is the Omron FINS protocol driver of the IoT DC3 platform. It communicates with Omron PLCs via the
FINS protocol over TCP sockets, building FINS frames manually with proper headers and memory read/write commands. It
supports the D, W, H, and C memory areas. No external protocol library is used.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-fins
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

| Attribute    | Description                                            |
|--------------|--------------------------------------------------------|
| Memory Area  | Memory area (D / W / H / C)                            |
| Address      | Word address within the memory area                    |
| Data Type    | INT16 / UINT16 / INT32 / UINT32 / FLOAT / STRING / BCD |
| Bit Position | Bit position within the word                           |

## Command Attributes (write)

| Attribute   | Description                  |
|-------------|------------------------------|
| Memory Area | Memory area (D / W / H / C)  |
| Address     | Word address within the area |
| Data Type   | Value data type              |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable Omron PLC speaking FINS over TCP, typically on port 9600.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-fins -am package
java -jar dc3-driver/dc3-driver-fins/target/dc3-driver-fins.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-fins -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
