# DC3 Driver Serial

## Overview

`dc3-driver-serial` is the generic serial port driver of the IoT DC3 platform. It communicates with RS232/RS485/RS422
devices over a serial port using the jSerialComm library, caching one serial connection per device. For each point it
sends a configured HEX command, reads back the raw response, and parses it according to frame header/footer, checksum,
data offset/length, data format (HEX/ASCII/BINARY/FLOAT), and byte order. Write commands send a HEX command template
with the `${value}` placeholder substituted.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-serial
- **Version**: 2026.5.22
- **Driver Name**: Serial Port Driver

## Driver Attributes (Device-level)

| Attribute   | Code     | Type   | Default      | Description                                                  |
|-------------|----------|--------|--------------|--------------------------------------------------------------|
| Serial Port | port     | STRING | /dev/ttyUSB0 | Serial port device path                                      |
| Baud Rate   | baudRate | INT    | 9600         | Baud rate (1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200) |
| Data Bits   | dataBits | INT    | 8            | Data bits (5, 6, 7, 8)                                       |
| Stop Bits   | stopBits | INT    | 1            | Stop bits (1, 2)                                             |
| Parity      | parity   | INT    | 0            | Parity (0=None, 1=Odd, 2=Even)                               |
| Timeout     | timeout  | INT    | 1000         | Read timeout in milliseconds                                 |

## Point Attributes

| Attribute      | Code          | Type   | Default | Description                                          |
|----------------|---------------|--------|---------|------------------------------------------------------|
| Send Command   | sendCommand   | STRING |         | HEX command to send (e.g. 01 03 00 00 00 0A C5 CD)   |
| Receive Length | receiveLength | INT    | 0       | Expected response length in bytes (0=auto detect)    |
| Frame Header   | frameHeader   | STRING |         | Frame header in HEX (e.g. 01 03)                     |
| Frame Footer   | frameFooter   | STRING |         | Frame footer in HEX (e.g. 0D 0A)                     |
| Data Offset    | dataOffset    | INT    | 0       | Data region offset from frame start                  |
| Data Length    | dataLength    | INT    | 0       | Data region length in bytes (0=until frame footer)   |
| Checksum Type  | checksumType  | STRING | NONE    | Checksum type: NONE, CRC16, XOR                      |
| Data Format    | dataFormat    | STRING | HEX     | Data format: HEX, ASCII, BINARY, FLOAT               |
| Byte Order     | byteOrder     | STRING | BIG     | Byte order: BIG, LITTLE                              |

## Command Attributes (write)

| Attribute    | Code        | Type   | Default  | Description                                  |
|--------------|-------------|--------|----------|----------------------------------------------|
| Send Command | sendCommand | STRING | ${value} | HEX command template with ${value} placeholder |
| Byte Order   | byteOrder   | STRING | BIG      | Byte order for encoding value: BIG, LITTLE   |

## Prerequisites

A serial device connected to a serial port reachable from the host running the driver (e.g. `/dev/ttyUSB0`). The
port path and line parameters (baud rate, data/stop bits, parity, timeout) are supplied through the driver
attributes above.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-serial/target/dc3-driver-serial.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
