# DC3 Common Data

## Overview

`dc3-common-data` is the shared Data Center business module of the IoT DC3 platform. It provides all service
implementations for point value ingestion, command dispatch, driver/device status tracking, and data query. It is wired
into `dc3-center-data`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-data

## Key Components

| Layer        | Contents                                                                                          |
|--------------|---------------------------------------------------------------------------------------------------|
| Controllers  | REST controllers for point values, point/custom commands, events, status, and health            |
| Services     | Point value, point command, command history, event history, driver status, and device status      |
| Facades      | `DriverFacade`, `PointFacade`, and related transport-independent cross-service APIs               |
| RabbitMQ     | Point/custom-command producers plus value, state, alarm, event, and result consumers              |
| Init         | `DataInitRunner` for startup preparation                                                          |

## Command Dispatch Flow

```
REST /api/v3/data/point_value/read
  → PointCommandServiceImpl
    → DriverFacade.getByDeviceId(tenantId, deviceId)
    → RabbitMQ: dc3.e.point_command / dc3.r.point_command.{serviceName}
      → Driver receives and acts
```

Custom device commands follow the parallel `dc3.e.command` / `dc3.r.command.{serviceName}` route through
`CommandHistoryServiceImpl`.

## MQ Topics

| Exchange              | Queue or routing key                | Direction                   |
|-----------------------|-------------------------------------|-----------------------------|
| `dc3.e.value`         | `dc3.q.value.point`                 | Inbound point values        |
| `dc3.e.point_command` | `dc3.r.point_command.{service}`     | Outbound point read/write   |
| `dc3.e.command`       | `dc3.r.command.{service}`           | Outbound custom commands    |
| `dc3.e.state`         | `dc3.q.state.driver` / `dc3.q.state.device` | Inbound driver/device state |
| `dc3.e.event`         | `dc3.q.event.report`                | Inbound reported events     |

The optional `dc3.rabbit.tag` system property prefixes runtime names; `RabbitConstant` remains authoritative.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-data -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-data -am test
```

## Related Modules

- `dc3-center-data` — Bootstraps this module as a Spring Boot service
- `dc3-api-manager` — gRPC API consumed by this module for driver/point resolution
- `dc3-common-repository` — Storage adapter for persisting point values
- `dc3-common-rabbitmq` — RabbitMQ exchange/queue configuration
