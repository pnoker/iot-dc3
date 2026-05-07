# DC3 Driver OPC UA

## Overview

`dc3-driver-opc-ua` is the OPC UA (Unified Architecture) protocol driver of the IoT DC3 platform. It connects to OPC UA
servers to read and write node values from industrial
automation systems using the OPC UA binary protocol.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-opc-ua
- **Version**: 2026.5.5
- **Driver Name**: OpcUa驱动

## Driver Attributes (Device-level)

| Attribute | Description                          |
|-----------|--------------------------------------|
| 主机 (Host) | OPC UA server hostname or IP         |
| 端口 (Port) | OPC UA server port (typically 4840)  |
| 路径 (Path) | OPC UA endpoint path (e.g., `/milo`) |

## Point Attributes

| Attribute        | Description                 |
|------------------|-----------------------------|
| 命名空间 (Namespace) | OPC UA node namespace index |
| 位号 (Tag)         | OPC UA node identifier      |

## Prerequisites

An OPC UA server (e.g., Milo server, Prosys OPC UA Simulation Server) accessible on the network.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-opc-ua/target/dc3-driver-opc-ua.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

