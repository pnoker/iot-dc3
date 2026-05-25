# DC3 Driver OPC DA

## Overview

`dc3-driver-opc-da` is the OPC DA (Data Access) protocol driver of the IoT DC3 platform. It connects to OPC DA servers
using DCOM/J-Interop to read real-time process data from
OPC-compliant industrial devices and SCADA systems.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-opc-da
- **Version**: 2026.5.22
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

## Prerequisites

- An OPC DA server running on a Windows host accessible via DCOM
- DCOM permissions configured to allow remote access from the driver host
- OPC DA specification: OPC DA 2.0 / 3.0

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-opc-da/target/dc3-driver-opc-da.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
