# DC3 Driver Virtual

## Overview

`dc3-driver-virtual` is the Virtual Driver of the IoT DC3 platform, designed for testing and development purposes. It
simulates device communication by generating random point values on a configurable schedule without connecting to real
hardware.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-virtual
- **Driver Name**: Virtual Driver

## Driver Attributes (Device-level)

| Attribute | Description               |
|-----------|---------------------------|
| Host      | Simulated target hostname |
| Port      | Simulated target port     |

## Point Attributes

| Attribute | Description          |
|-----------|----------------------|
| Tag       | Point tag identifier |

## Command Attributes (write)

| Attribute         | Description                       |
|-------------------|-----------------------------------|
| Payload Template  | Template for the command payload  |
| Response Template | Template for parsing the response |

## Event Attributes

| Attribute       | Description                       |
|-----------------|-----------------------------------|
| Event Code Path | Path to the event code in payload |
| Payload Path    | Path to the event payload         |

## Data Collection Schedule

The module `application.yml` and its profile-specific variants are authoritative for attribute codes, types, default
values, scheduling, health, and local buffering. Keep this README aligned when that metadata changes.

Configured in `application-dev.yml`:

```yaml
dc3:
  driver:
    schedule:
      read:
        cron: '0/1 * * * * ?'   # Every second
```

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-virtual -am package
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar
```

### 3. Verify Registration

The driver logs show gRPC registration with Manager Center on startup:

```
Driver register success, service name: dc3-driver-virtual
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-virtual -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
