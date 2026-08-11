# DC3 Driver OPC DA

## Overview

`dc3-driver-opc-da` is the OPC DA (Data Access) protocol driver of the IoT DC3 platform. It connects to OPC DA servers
using DCOM/J-Interop to read real-time process data from OPC-compliant industrial devices and SCADA systems.

> **Work in progress.** Protocol-level I/O is not fully implemented in `OpcDaDriverCustomServiceImpl`. Treat this
> module as an integration skeleton, not a production-ready OPC DA driver, until its read/write TODOs are completed and
> verified against a real server.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-opc-da
- **Driver Name**: OPC DA Driver

## Driver Attributes (Device-level)

| Attribute | Description                             |
|-----------|-----------------------------------------|
| Host      | OPC DA server hostname or IP            |
| CLSID     | OPC server CLSID (COM class identifier) |
| Username  | Windows DCOM authentication username    |
| Password  | Windows DCOM authentication password    |

## Point Attributes

| Attribute | Description            |
|-----------|------------------------|
| Group     | OPC DA item group name |
| Tag       | OPC DA item tag name   |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and
local buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

- An OPC DA server running on a Windows host accessible via DCOM
- DCOM permissions configured to allow remote access from the driver host
- OPC DA specification: OPC DA 2.0 / 3.0

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-opc-da -am package
java -jar dc3-driver/dc3-driver-opc-da/target/dc3-driver-opc-da.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-opc-da -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
