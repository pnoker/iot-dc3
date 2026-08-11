# DC3 Driver Mitsubishi Melsec

## Overview

`dc3-driver-melsec` is the Mitsubishi Melsec MC protocol driver of the IoT DC3 platform. It connects to Melsec PLCs over
the MC protocol using the `iot-communication` library, reading and writing device memory addresses (e.g. `D100`, `M0`,
`X10`, `W200`) by point value type.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-melsec
- **Driver Name**: Mitsubishi Melsec Driver

## Driver Attributes (Device-level)

| Attribute  | Description                 |
|------------|-----------------------------|
| Host       | Ip                          |
| Port       | Port                        |
| PLC Series | PLC series (A/QnA/Q_L/IQ_R) |

## Point Attributes

| Attribute      | Description                                      |
|----------------|--------------------------------------------------|
| Device Address | Device memory address (D100, M0, X10, W200 etc.) |
| String Length  | String read length (0 for non-string types)      |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and
local buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable Mitsubishi Melsec PLC (or simulator) exposing the MC protocol over TCP, addressable by the configured host
and port.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-melsec -am package
java -jar dc3-driver/dc3-driver-melsec/target/dc3-driver-melsec.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-melsec -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
