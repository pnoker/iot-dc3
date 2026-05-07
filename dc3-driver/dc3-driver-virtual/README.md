# DC3 Driver Virtual

## Overview

`dc3-driver-virtual` is the Virtual Driver of the IoT DC3 platform, designed for testing and development purposes. It
simulates device communication by generating random point
values on a configurable schedule without connecting to real hardware.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-virtual
- **Version**: 2026.5.5
- **Driver Name**: 虚拟驱动

## Driver Attributes

| Attribute | Description               |
|-----------|---------------------------|
| 主机 (Host) | Simulated target hostname |
| 端口 (Port) | Simulated target port     |
| 位号 (Tag)  | Point tag identifier      |

## Data Collection Schedule

Configured in `application-dev.yml`:

```yaml
driver:
  schedule:
    read:
      cron: '0/1 * * * * ?'   # Every second
```

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar
```

### 3. Verify Registration

The driver logs show gRPC registration with Manager Center on startup:

```
Driver register success, service name: dc3-driver-virtual
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

