# DC3 Driver PLC S7

## Overview

`dc3-driver-plcs7` is the Siemens S7 PLC driver of the IoT DC3 platform. It communicates with Siemens S7 series PLCs
(S7-200/300/400/1200/1500) using the S7 TCP protocol to read and write data block registers.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-plcs7
- **Driver Name**: PLC S7 Driver

## Driver Attributes (Device-level)

| Attribute | Description                                          |
|-----------|------------------------------------------------------|
| Host      | PLC IP address                                       |
| Port      | S7 TCP port (typically 102)                          |
| PLC Type  | Siemens PLC model (selects the S7 addressing scheme) |

## Point Attributes

| Attribute   | Description                                     |
|-------------|-------------------------------------------------|
| DB Number   | Siemens data block number                       |
| Byte Offset | Byte offset within the data block               |
| Bit Offset  | Bit offset within the byte (for boolean points) |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

- Siemens S7 PLC (S7-200 Smart, S7-1200, S7-1500 or compatible)
- PLC PUT/GET access enabled
- Compatible PLC programming: Siemens TIA Portal or STEP 7

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-plcs7 -am package
java -jar dc3-driver/dc3-driver-plcs7/target/dc3-driver-plcs7.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-plcs7 -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
