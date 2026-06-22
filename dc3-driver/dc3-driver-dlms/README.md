# DC3 Driver DLMS/COSEM

## Overview

`dc3-driver-dlms` is the DLMS/COSEM protocol driver of the IoT DC3 platform. It is intended to read COSEM object
attributes from energy meters and similar devices over TCP or serial transports, using the Gurux DLMS library to
build and decode DLMS frames.

> ⚠️ **Work in progress.** This driver is a skeleton — its class documentation explicitly states "Protocol-level
> I/O is not yet fully implemented" and the method bodies carry TODO markers. The Gurux client generates DLMS
> frames, but the transport send/receive is not implemented: `read()` returns a `null` value, `write()` returns
> `true` unconditionally, `health()` only checks whether a client object is cached (no real connectivity probe),
> and `getConnector()` does not establish a TCP/serial connection or HDLC handshake. Treat it as a starting
> template, not a production-ready driver.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-dlms
- **Version**: 2026.5.22
- **Driver Name**: DLMS/COSEM Driver

## Driver Attributes (Device-level)

| Attribute      | Description                              |
|----------------|------------------------------------------|
| Transport Type | Transport type (TCP, SERIAL)             |
| Host           | Remote device address (TCP mode)         |
| Port           | Remote device port (TCP mode)            |
| Serial Port    | Serial port path (SERIAL mode)           |
| Baud Rate      | Baud rate (SERIAL mode)                  |
| Client Address | DLMS client address (public client = 16) |
| Server Address | DLMS server address                      |
| Authentication | Authentication method (NONE, LOW, HIGH)  |
| Password       | Authentication password                  |

## Point Attributes

| Attribute    | Description                                       |
|--------------|---------------------------------------------------|
| Object Type  | DLMS object type (REGISTER, CLOCK, DATA, etc.)    |
| Logical Name | Object logical name / OBIS code (e.g. 1.0.1.8.0.255) |
| Attribute ID | Attribute ID (2 = Present Value)                  |

## Prerequisites

A reachable DLMS/COSEM device (or simulator) over TCP (default port 4059) or serial, plus the matching client/
server addresses and authentication settings.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-dlms/target/dc3-driver-dlms.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
