# DC3 Driver TCP/UDP

## Overview

`dc3-driver-tcp-udp` is the generic raw TCP/UDP socket driver of the IoT DC3 platform. It communicates with devices over
plain TCP or UDP sockets, selectable per device via the `protocol` attribute. For each point it sends a configured HEX
command and reads back the raw response, which is then parsed by frame header/footer, data offset/length, delimiter,
data format (HEX/ASCII/INT16/UINT16/INT32/FLOAT), and byte order. TCP connections are cached per device (with a
consecutive-failure backoff), while UDP is connectionless. Write commands send a HEX command template with the
`${value}` placeholder substituted. This module has no third-party protocol dependency — it uses the JDK socket APIs
directly.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-tcp-udp
- **Driver Name**: TCP/UDP Raw Driver

## Driver Attributes (Device-level)

| Attribute       | Code           | Type   | Default   | Description   |
|-----------------|----------------|--------|-----------|---------------|
| Protocol        | protocol       | STRING | TCP       | TCP or UDP    |
| Host            | host           | STRING | localhost |               |
| Port            | port           | INT    | 502       |               |
| Connect Timeout | connectTimeout | INT    | 5000      |               |
| Read Timeout    | readTimeout    | INT    | 3000      |               |
| Delimiter       | delimiter      | STRING |           | Hex delimiter |

## Point Attributes

| Attribute      | Code          | Type   | Default | Description                        |
|----------------|---------------|--------|---------|------------------------------------|
| Send Command   | sendCommand   | STRING |         |                                    |
| Receive Length | receiveLength | INT    | 0       | 0 means use delimiter              |
| Frame Header   | frameHeader   | STRING |         |                                    |
| Frame Footer   | frameFooter   | STRING |         |                                    |
| Data Offset    | dataOffset    | INT    | 0       |                                    |
| Data Length    | dataLength    | INT    | 0       |                                    |
| Data Format    | dataFormat    | STRING | HEX     | HEX/ASCII/INT16/UINT16/INT32/FLOAT |
| Byte Order     | byteOrder     | STRING | BIG     |                                    |

## Command Attributes (write)

| Attribute    | Code        | Type   | Default  | Description |
|--------------|-------------|--------|----------|-------------|
| Send Command | sendCommand | STRING | ${value} |             |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable TCP or UDP endpoint. The protocol selection, host/port, timeouts, and per-point HEX commands are supplied
through the driver and point attributes above.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-tcp-udp -am package
java -jar dc3-driver/dc3-driver-tcp-udp/target/dc3-driver-tcp-udp.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-tcp-udp -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
