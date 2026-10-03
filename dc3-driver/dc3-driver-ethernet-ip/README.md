# DC3 Driver EtherNet/IP

## Overview

`dc3-driver-ethernet-ip` is the EtherNet/IP (CIP) protocol driver of the IoT DC3 platform, intended for communication
with Rockwell Allen-Bradley PLCs. It implements the protocol over a raw TCP socket with CIP (Common Industrial Protocol)
message framing — no external protocol library is used — reading and writing tags via CIP Data Table Read/Write
services.

## Status

| Area                  | Status  | Notes                                                                                |
|-----------------------|---------|--------------------------------------------------------------------------------------|
| Connection management | preview | TCP connect plus CIP RegisterSession; backplane slot carried in the connection path  |
| Point read            | preview | CIP Data Table Read via UCMM sendRRData; golden-frame unit tested, PLC round-trip pending |
| Point write           | preview | CIP Data Table Write via UCMM sendRRData                                             |
| Device health         | preview | Cached session/socket state inspection                                               |
| Known gaps            | —       | ForwardOpen (connection-oriented CIP transport) not implemented; UCMM read/write only |

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-ethernet-ip
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

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable Rockwell Allen-Bradley (or compatible) EtherNet/IP PLC, typically on TCP port 44818.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-ethernet-ip -am package
java -jar dc3-driver/dc3-driver-ethernet-ip/target/dc3-driver-ethernet-ip.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-ethernet-ip -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
