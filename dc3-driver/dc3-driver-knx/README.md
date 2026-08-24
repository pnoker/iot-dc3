# DC3 Driver KNX

## Overview

`dc3-driver-knx` connects to KNX (ISO/IEC 14543-3) installations through a KNX IP gateway using Calimero. It
maintains one tunneling link per gateway device and reads/writes group addresses as boolean, unsigned, float, or
control values.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-knx
- **Driver Name**: KNX Driver

## Driver Attributes (Device-level)

| Attribute      | Code          | Type    | Default | Description                              |
|----------------|---------------|---------|---------|------------------------------------------|
| Remote Host    | remoteHost    | STRING  |         | KNX IP gateway address (tunneling server)|
| Remote Port    | remotePort    | INT     | 3671    | KNX IP gateway port                      |
| Local Host     | localHost     | STRING  |         | Local bind address (optional)            |
| Use NAT        | useNat        | BOOLEAN | false   | Enable NAT mode for tunneling            |
| Device Address | deviceAddress | STRING  | 0.0.0   | Local KNX individual address             |

## Point Attributes

| Attribute     | Code         | Type   | Default | Description                                      |
|---------------|--------------|--------|---------|--------------------------------------------------|
| Group Address | groupAddress | STRING |         | KNX group address, e.g. 1/2/3                    |
| Data Type     | dataType     | STRING | BOOL    | BOOL, UINT, FLOAT, or CONTROL                    |
| DPT           | dpt          | STRING |         | Datapoint type for UINT reads/writes, e.g. 5.001 |

## Command Attributes (write)

| Attribute     | Code         | Type   | Default | Description                          |
|---------------|--------------|--------|---------|--------------------------------------|
| Group Address | groupAddress | STRING |         | KNX group address to write to        |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A KNX installation reachable through a KNX IP gateway (default port 3671) with tunneling enabled.

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-knx -am package
java -jar dc3-driver/dc3-driver-knx/target/dc3-driver-knx.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-knx -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
