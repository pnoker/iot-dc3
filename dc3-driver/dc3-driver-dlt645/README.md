# DC3 Driver DLT645

## Overview

`dc3-driver-dlt645` is the DL/T 645-2007 smart electricity meter driver of the IoT DC3 platform. It communicates with
meters over a serial port (RS485) using the jSerialComm library, caching one serial connection per meter. Each point
reads a data identifier (DI0-DI3) from the meter and decodes the response data field (offset by `+0x33` per the
standard) according to a configurable data format.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-dlt645
- **Driver Name**: DL/T 645 Driver

## Driver Attributes (Device-level)

| Attribute      | Code         | Type   | Default        | Description                                     |
|----------------|--------------|--------|----------------|-------------------------------------------------|
| Serial Port    | port         | STRING | /dev/ttyUSB0   | Serial port device path                         |
| Baud Rate      | baudRate     | INT    | 2400           | Baud rate (1200, 2400, 4800, 9600)              |
| Data Bits      | dataBits     | INT    | 8              | Data bits (7, 8)                                |
| Stop Bits      | stopBits     | INT    | 1              | Stop bits (1, 2)                                |
| Parity         | parity       | INT    | 2              | Parity (0=None, 1=Odd, 2=Even)                  |
| Timeout        | timeout      | INT    | 1000           | Read timeout in milliseconds                    |
| Meter Address  | meterAddress | STRING | 000000000000   | 12-digit meter address in BCD                   |
| Write Password | password     | STRING | 00000000       | 8-char hexadecimal write password (4 bytes)     |
| Operator Code  | operatorCode | STRING | 00000000       | 8-char hexadecimal operator code (4 bytes)      |

## Point Attributes

| Attribute       | Code       | Type   | Default  | Description                                                     |
|-----------------|------------|--------|----------|-----------------------------------------------------------------|
| Data Identifier | di         | STRING | 00010000 | 8-char hexadecimal DI0-DI3 (00010000 = total active energy)     |
| Data Format     | dataFormat | STRING | HEX      | Data format: HEX, BCD, INT, FLOAT, ASCII                        |

## Command Attributes (write)

| Attribute       | Code       | Type   | Default  | Description                                        |
|-----------------|------------|--------|----------|----------------------------------------------------|
| Data Identifier | di         | STRING | 00010000 | 8-char hexadecimal DI0-DI3                         |
| Data Format     | dataFormat | STRING | HEX      | Format used to encode the written value            |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A DL/T 645-2007 electricity meter connected over RS485 to a serial port reachable from the host running the driver
(e.g. `/dev/ttyUSB0`), with baud rate, parity (typically even), and the 12-digit meter address configured.

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-dlt645 -am package
java -jar dc3-driver/dc3-driver-dlt645/target/dc3-driver-dlt645.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-dlt645 -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
