# DC3 Common Driver

## Overview

`dc3-common-driver` is the shared driver dependency module of the IoT DC3 platform. It provides the driver SDK shared by
all protocol drivers, including auto-registration with the Manager Center, metadata sync, RabbitMQ command handling, and
scheduled data collection.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-driver

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
    → gRPC: dc3-center-manager / DriverApi.DriverRegister
      ← Returns: driver ID, driver attributes, point attributes, device IDs
    → Subscribe to metadata queue: dc3.q.metadata.driver.{serviceName}
    → Subscribe to point-command queue: dc3.q.point_command.{serviceName}
    → Subscribe to custom-command queue: dc3.q.command.{serviceName}
```

## RabbitMQ Integration

| Exchange              | Queue                               | Purpose                              |
|-----------------------|-------------------------------------|--------------------------------------|
| `dc3.e.metadata`      | `dc3.q.metadata.driver.{service}`   | Receive configuration changes        |
| `dc3.e.point_command` | `dc3.q.point_command.{service}`     | Receive point read/write commands    |
| `dc3.e.command`       | `dc3.q.command.{service}`           | Receive custom device commands       |
| `dc3.e.value`         | —                                   | Publish point values to Data Center  |

The optional `dc3.rabbit.tag` system property prefixes runtime names; use `RabbitConstant` and `DriverTopicConfig` as
the authoritative definitions.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-driver -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-driver -am test
```

## Related Modules

- All `dc3-driver-*` modules — Consume this SDK
- `dc3-api-driver` — gRPC contracts consumed by this SDK
- `dc3-common-rabbitmq` — RabbitMQ exchange configuration
- `dc3-common-constant` — `RabbitConstant` routing key prefixes
