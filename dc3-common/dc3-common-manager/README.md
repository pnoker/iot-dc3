# DC3 Common Manager

## Overview

`dc3-common-manager` is the shared Device Management business module of the IoT DC3 platform. It owns the Manager
domain's controllers, services, repository ports and adapters, gRPC servers, and event handling logic, including
label/group persistence. It is wired into `dc3-center-manager`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-manager

## Key Components

| Layer                            | Contents                                                                                            |
|----------------------------------|-----------------------------------------------------------------------------------------------------|
| Controllers                      | REST controllers for driver, device, profile, point, group, label, topic, etc.                      |
| Services                         | Reactive business services returning `Mono`/`Flux` without blocking bridges                        |
| Repositories                     | R2DBC repository ports and dialect-aware adapters for Manager metadata                              |
| gRPC Servers (Spring `@Service`) | `DriverDriverServer`, `DriverDeviceServer`, `DriverPointServer`, `ManagerPointServer`               |
| Metadata Events                  | `MetadataEventPublisher`, `MetadataEventListener` — async metadata change notification via RabbitMQ |
| Scheduled Jobs                   | `ScheduleForManagerServiceImpl` — Quartz-based hourly maintenance job (`HourlyJobForManager`)       |

## gRPC Services Exposed

| Server Class         | Usage                                                 |
|----------------------|-------------------------------------------------------|
| `DriverDriverServer` | Driver registration, called by all drivers on startup |
| `DriverDeviceServer` | Device query by device ID, called by drivers          |
| `DriverPointServer`  | Point query by point ID, called by drivers            |
| `ManagerPointServer` | Point query called by Data service                    |

## Metadata Event Flow

```
REST: update device/point
  → Service layer change
    → MetadataEventPublisher.publishEvent(MetadataEvent)
      → MetadataEventListener (async)
        → Find affected driver(s)
        → RabbitMQ: dc3.e.metadata / dc3.r.metadata.driver.{serviceName}
          → Driver receives and refreshes local config
```

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-manager -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-manager -am test
```

## Related Modules

- `dc3-center-manager` — Bootstraps this module as a Spring Boot service
- `dc3-api-driver` — gRPC contracts implemented by this module
- `dc3-api-manager` — Manager-side gRPC contracts implemented by this module
- `dc3-common-model` — BO/VO/DTO/DO entities
