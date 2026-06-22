# DC3 Driver Mitsubishi Melsec

## Overview

`dc3-driver-melsec` is the Mitsubishi Melsec MC protocol driver of the IoT DC3 platform. It connects to Melsec PLCs over
the MC protocol using the `iot-communication` library, reading and writing device memory addresses (e.g. `D100`, `M0`,
`X10`, `W200`) by point value type.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-melsec
- **Version**: 2026.5.22
- **Driver Name**: Mitsubishi Melsec Driver

## Driver Attributes (Device-level)

| Attribute  | Description                       |
|------------|-----------------------------------|
| Host       | Ip                                |
| Port       | Port                              |
| PLC Series | PLC series (A/QnA/Q_L/IQ_R)       |

## Point Attributes

| Attribute      | Description                                         |
|----------------|-----------------------------------------------------|
| Device Address | Device memory address (D100, M0, X10, W200 etc.)    |
| String Length  | String read length (0 for non-string types)         |

## Prerequisites

A reachable Mitsubishi Melsec PLC (or simulator) exposing the MC protocol over TCP, addressable by the configured host
and port.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-melsec/target/dc3-driver-melsec.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
