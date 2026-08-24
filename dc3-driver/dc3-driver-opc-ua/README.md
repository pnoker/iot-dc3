# DC3 Driver OPC UA

## Overview

`dc3-driver-opc-ua` is the OPC UA (Unified Architecture) protocol driver of the IoT DC3 platform. It connects to OPC UA
servers to read and write node values from industrial automation systems using the OPC UA binary protocol.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-opc-ua
- **Driver Name**: OPC UA Driver

## Driver Attributes (Device-level)

| Attribute | Description                          |
|-----------|--------------------------------------|
| Host      | OPC UA server hostname or IP         |
| Port      | OPC UA server port (default 18600)   |
| Path      | OPC UA endpoint path (default `/`)    |

## Point Attributes

| Attribute | Description                 |
|-----------|-----------------------------|
| Namespace | OPC UA node namespace index |
| Tag       | OPC UA node identifier      |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and
local buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

An OPC UA server (e.g., Milo server, Prosys OPC UA Simulation Server) accessible on the network.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-opc-ua -am package
java -jar dc3-driver/dc3-driver-opc-ua/target/dc3-driver-opc-ua.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-opc-ua -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
