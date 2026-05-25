# DC3 Common Driver

## Overview

`dc3-common-driver` is the shared driver dependency module of the IoT DC3 platform. It provides the driver SDK shared by
all protocol drivers, including auto-registration with the
Manager Center, metadata sync, RabbitMQ command handling, and scheduled data collection.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-driver
- **Version**: 2026.5.22

## Key Components

| Component                                    | Purpose                                                       |
|----------------------------------------------|---------------------------------------------------------------|
| `DriverInitRunner`                           | Registers the driver with Manager Center via gRPC on startup  |
| `DriverEnvironmentConfig`                    | Binds driver YAML config (name, attributes, point attributes) |
| gRPC Clients (`PointClient`, `DeviceClient`) | Fetches point/device config from Manager Center               |
| RabbitMQ Consumers                           | Receives metadata update events and device commands           |
| Scheduled Jobs                               | Periodic read jobs triggering driver's data collection loop   |
| `DriverTopicConfig`                          | Configures driver-specific RabbitMQ queues/bindings           |

## Driver Registration Flow

```
Driver startup
  → DriverInitRunner
    → gRPC: dc3-center-manager / DriverApi.driverRegister()
      ← Returns: driver ID, driver attributes, point attributes, device IDs
    → Subscribe to metadata queue: dc3.q.metadata.driver.{serviceName}
    → Subscribe to command queue: dc3.q.command.driver.{serviceName}
```

## RabbitMQ Integration

| Exchange         | Queue                             | Purpose                             |
|------------------|-----------------------------------|-------------------------------------|
| `dc3.e.metadata` | `dc3.q.metadata.driver.{service}` | Receive config change events        |
| `dc3.e.command`  | `dc3.q.command.driver.{service}`  | Receive READ/WRITE commands         |
| `dc3.e.value`    | —                                 | Publish point values to data center |

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- All `dc3-driver-*` modules — Consume this SDK
- `dc3-api-driver` — gRPC contracts consumed by this SDK
- `dc3-common-rabbitmq` — RabbitMQ exchange configuration
- `dc3-common-constant` — `RabbitConstant` routing key prefixes

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

