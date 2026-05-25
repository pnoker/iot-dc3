# DC3 Driver PLC S7

## Overview

`dc3-driver-plcs7` is the Siemens S7 PLC driver of the IoT DC3 platform. It communicates with Siemens S7 series PLCs (
S7-200/300/400/1200/1500) using the S7 TCP protocol to read
and write data block registers.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-plcs7
- **Version**: 2026.5.22
- **Driver Name**: PLC S7 Driver

## Driver Attributes (Device-level)

| Attribute | Description                 |
|-----------|-----------------------------|
| Host      | PLC IP address              |
| Port      | S7 TCP port (typically 102) |

## Point Attributes

| Attribute   | Description                                     |
|-------------|-------------------------------------------------|
| DB Number   | Siemens data block number                       |
| Byte Offset | Byte offset within the data block               |
| Bit Offset  | Bit offset within the byte (for boolean points) |
| Data Length | Length of data to read in bytes                 |

## Prerequisites

- Siemens S7 PLC (S7-200 Smart, S7-1200, S7-1500 or compatible)
- PLC PUT/GET access enabled
- Compatible PLC programming: Siemens TIA Portal or STEP 7

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-plcs7/target/dc3-driver-plcs7.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
