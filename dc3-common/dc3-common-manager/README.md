# DC3 Common Manager

## Overview

`dc3-common-manager` is the shared Device Management business module of the IoT DC3 platform. It contains all
controllers, service implementations, gRPC servers, DAL managers,
mappers, and event handling logic for the Manager Center. It is wired into `dc3-center-manager`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-manager
- **Version**: 2026.5.22

## Key Components

| Layer                         | Contents                                                                                            |
|-------------------------------|-----------------------------------------------------------------------------------------------------|
| Controllers                   | REST controllers for driver, device, profile, point, group, label, topic, etc.                      |
| Services                      | `DriverService`, `DeviceService`, `ProfileService`, `PointService`, `DriverRegisterService`         |
| gRPC Servers (`@GrpcService`) | `DriverDriverServer`, `DriverDeviceServer`, `DriverPointServer`, `ManagerPointServer`               |
| DAL Managers                  | `DriverManager`, `DeviceManager`, `ProfileManager`, `PointManager` (MyBatis-Plus `IService`)        |
| Metadata Events               | `MetadataEventPublisher`, `MetadataEventListener` — async metadata change notification via RabbitMQ |
| Scheduled Jobs                | `ScheduleForManagerServiceImpl` — Quartz-based hourly statistics                                    |

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
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-center-manager` — Bootstraps this module as a Spring Boot service
- `dc3-api-driver` — gRPC contracts implemented by this module
- `dc3-api-manager` — Manager-side gRPC contracts implemented by this module
- `dc3-common-model` — BO/VO/DTO/DO entities

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

