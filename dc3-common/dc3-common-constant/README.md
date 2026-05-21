# DC3 Common Constant

## Overview

`dc3-common-constant` is the shared constants and enumerations module of the IoT DC3 platform. It defines all
platform-wide constants, routing keys, service names, URL prefixes,
and enumeration types used across services, drivers, and common modules.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-constant
- **Version**: 2026.5.18

## Key Components

### Service Constants

| Class             | Purpose                                                    |
|-------------------|------------------------------------------------------------|
| `AuthConstant`    | Auth service name, URL prefixes                            |
| `ManagerConstant` | Manager service name (`dc3-center-manager`), URL prefixes  |
| `DataConstant`    | Data service name, URL prefixes, point value API constants |
| `DriverConstant`  | Driver service name helpers                                |

### RabbitMQ Constants

`RabbitConstant` defines all exchange names, queue name prefixes, and routing key prefixes:

| Constant                         | Value                    |
|----------------------------------|--------------------------|
| `TOPIC_EXCHANGE_COMMAND`         | `dc3.e.command`          |
| `TOPIC_EXCHANGE_METADATA`        | `dc3.e.metadata`         |
| `TOPIC_EXCHANGE_VALUE`           | `dc3.e.value`            |
| `TOPIC_EXCHANGE_EVENT`           | `dc3.e.event`            |
| `ROUTING_DEVICE_COMMAND_PREFIX`  | `dc3.r.command.device.`  |
| `ROUTING_DRIVER_METADATA_PREFIX` | `dc3.r.metadata.driver.` |
| `ROUTING_POINT_VALUE_PREFIX`     | `dc3.r.value.point.`     |

> **Important**: RabbitMQ routing keys are suffixed with the driver's service name. These constants must not be renamed
> without updating all consumers.

### Common Constants

| Class                               | Purpose                                       |
|-------------------------------------|-----------------------------------------------|
| `DefaultConstant`                   | Platform defaults (page size, etc.)           |
| `TimeConstant`                      | Date/time format strings                      |
| `RequestConstant`                   | HTTP header key names (tenant/user injection) |
| `PrefixConstant` / `SuffixConstant` | Common cache/key prefixes and suffixes        |

### Enumerations

Located in `io.github.pnoker.common.enums`:

- `EnableFlagEnum` — Entity enable/disable status
- `DriverStatusEnum` — Driver online/offline/fault states
- `DriverTypeFlagEnum` — Driver protocol type classification
- `PointTypeFlagEnum` — Point value type (int, float, bool, string, etc.)
- `MetadataOperateTypeEnum` — Metadata operation (add/update/delete)
- `DeviceCommandTypeEnum` — Device command type (READ/WRITE)
- And many others (`AttributeTypeFlagEnum`, `ProfileShareFlagEnum`, etc.)

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

Used as a dependency by virtually all other `dc3-common-*` and `dc3-center-*` modules.

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
