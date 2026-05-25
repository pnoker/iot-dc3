# DC3 Common Data

## Overview

`dc3-common-data` is the shared Data Center business module of the IoT DC3 platform. It provides all service
implementations for point value ingestion, command dispatch,
driver/device status tracking, and data query. It is wired into `dc3-center-data`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-data
- **Version**: 2026.5.22

## Key Components

| Layer        | Contents                                                                                 |
|--------------|------------------------------------------------------------------------------------------|
| Controllers  | REST controllers for point value read/write, query, status                               |
| Services     | `PointCommandService`, `PointValueService`, `DriverStatusService`, `DeviceStatusService` |
| gRPC Clients | `@GrpcClient(ManagerConstant.SERVICE_NAME)` stubs for `DriverApi`, `PointApi`            |
| RabbitMQ     | Producer for `dc3.e.command`; Consumer for `dc3.e.value`, `dc3.e.event`                  |
| Init         | `DataInitRunner` for startup preparation                                                 |

## Command Dispatch Flow

```
REST /api/v3/data/point_value/read
  → PointCommandServiceImpl
    → gRPC: driverApiBlockingStub.selectByDeviceId()
    → RabbitMQ: dc3.e.command / dc3.r.command.device.{serviceName}
      → Driver receives and acts
```

## MQ Topics

| Exchange        | Queue                                       | Direction               |
|-----------------|---------------------------------------------|-------------------------|
| `dc3.e.value`   | `dc3.q.value.point`                         | Inbound (from drivers)  |
| `dc3.e.command` | `dc3.q.command.device.{service}`            | Outbound (to drivers)   |
| `dc3.e.event`   | `dc3.q.event.driver` / `dc3.q.event.device` | Inbound (status events) |

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-center-data` — Bootstraps this module as a Spring Boot service
- `dc3-api-manager` — gRPC API consumed by this module for driver/point resolution
- `dc3-common-repository` — Storage adapter for persisting point values
- `dc3-common-rabbitmq` — RabbitMQ exchange/queue configuration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

