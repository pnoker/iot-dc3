# DC3 Driver Listening Virtual

## Overview

`dc3-driver-listening-virtual` is the Listening Virtual Driver of the IoT DC3 platform. It operates in passive listening
mode, accepting inbound TCP and UDP connections from devices that push data. It demonstrates listening-type driver
patterns supporting both TCP and UDP transports.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-listening-virtual
- **Driver Name**: Listening Virtual TCP/UDP Driver

## Service Ports

| Protocol | Port                                         |
|----------|----------------------------------------------|
| TCP      | `6270` (default, overridable via `TCP_PORT`) |
| UDP      | `6271` (default, overridable via `UDP_PORT`) |

## Point Attributes

| Attribute  | Description                   |
|------------|-------------------------------|
| Keyword    | Packet identification keyword |
| Start Byte | Data start byte offset        |
| End Byte   | Data end byte offset          |
| Type       | Data type interpretation      |

This driver declares no device-level driver attributes; all configuration is per-point.

The module `application.yml` and its profile-specific variants are authoritative for attribute codes, types, default
values, scheduling, health, and local buffering. Keep this README aligned when that metadata changes.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-listening-virtual -am package
java -jar dc3-driver/dc3-driver-listening-virtual/target/dc3-driver-listening-virtual.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-listening-virtual -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
