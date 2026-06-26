# DC3 Driver EtherNet/IP

## Overview

`dc3-driver-ethernet-ip` is the EtherNet/IP (CIP) protocol driver of the IoT DC3 platform, intended for
communication with Rockwell Allen-Bradley PLCs. It implements the protocol over a raw TCP socket with CIP
(Common Industrial Protocol) message framing — no external protocol library is used — reading and writing tags
via CIP Data Table Read/Write services.

> ⚠️ **Work in progress.** This driver is a skeleton — its class documentation explicitly states "Protocol-level
> I/O is not yet fully implemented" and the method bodies carry TODO markers. The CIP session setup is not done:
> `getConnector()` does not send the RegisterSession / ForwardOpen commands, the EtherNet/IP encapsulation header
> is a simplified placeholder, and `health()` only inspects the cached socket state rather than performing a real
> protocol probe. Treat it as a starting template, not a production-ready driver.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-ethernet-ip
- **Version**: 2026.5.22
- **Driver Name**: EtherNet/IP Driver

## Driver Attributes (Device-level)

| Attribute | Description                          |
|-----------|--------------------------------------|
| Host      | PLC host address                     |
| Port      | EtherNet/IP TCP port (default 44818) |
| Slot      | PLC backplane slot                   |
| Timeout   | Request timeout in milliseconds      |

## Point Attributes

| Attribute     | Description                |
|---------------|----------------------------|
| Tag Name      | CIP tag name               |
| Tag Type      | Tag data type (e.g. DINT)  |
| Element Count | Number of elements to read |

## Command Attributes (write)

| Attribute    | Description                          |
|--------------|--------------------------------------|
| Send Command | Value to write (supports `${value}`) |

## Prerequisites

A reachable Rockwell Allen-Bradley (or compatible) EtherNet/IP PLC, typically on TCP port 44818.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-ethernet-ip/target/dc3-driver-ethernet-ip.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
