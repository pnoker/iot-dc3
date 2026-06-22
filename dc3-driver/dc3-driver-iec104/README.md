# DC3 Driver IEC 104

## Overview

`dc3-driver-iec104` is the IEC 60870-5-104 protocol driver of the IoT DC3 platform. It is intended to connect to
substation automation and telecontrol equipment using the `j60870` (OpenMUC) library, handling ASDU events and data
polling for IEC 104 client connections.

> ⚠️ **Work in progress.** This driver is currently a skeleton — protocol-level I/O is not yet fully implemented. The
> class documentation of `Iec104DriverCustomServiceImpl` explicitly states it is a "work-in-progress skeleton", and
> `read()`/`write()`/`connectToIec104Server()` contain TODO markers noting the j60870 read/write/connection-builder
> APIs still need to be verified (`read()` returns a cached or empty value). Treat it as a starting template, not a
> production-ready driver.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-iec104
- **Version**: 2026.5.22
- **Driver Name**: IEC 104 Driver

## Driver Attributes (Device-level)

| Attribute       | Description |
|-----------------|-------------|
| Host            |             |
| Port            |             |
| ASDU Address    |             |
| COT Length      |             |
| CA Length       |             |
| IOA Length      |             |
| Connect Timeout |             |

## Point Attributes

| Attribute | Description |
|-----------|-------------|
| IOA       |             |
| ASDU Type |             |

## Command Attributes (write)

| Attribute    | Description |
|--------------|-------------|
| Send Command |             |

## Prerequisites

A reachable IEC 60870-5-104 server (substation/telecontrol device or simulator) addressable by the configured host and
port.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-iec104/target/dc3-driver-iec104.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
