# DC3 Common Constant

## Overview

`dc3-common-constant` is the shared constants and enumerations module of the IoT DC3 platform. It defines platform-wide
constants, routing keys, service names, URL prefixes, and top-level domain/wire/persistence enums used across services,
drivers, and common modules. Constants and nested enums used by only one module, protocol, configuration object, or
implementation remain beside their owner until they become a stable cross-module contract. Top-level `*Constant`
classes and top-level public enums are reserved for this module; local holders use concern-specific names such as
`*Limits` or `*Defaults`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-constant

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

| Constant                         | Base value               |
|----------------------------------|--------------------------|
| `TOPIC_EXCHANGE_POINT_COMMAND`   | `dc3.e.point_command`    |
| `TOPIC_EXCHANGE_COMMAND`         | `dc3.e.command`          |
| `TOPIC_EXCHANGE_METADATA`        | `dc3.e.metadata`         |
| `TOPIC_EXCHANGE_VALUE`           | `dc3.e.value`            |
| `TOPIC_EXCHANGE_EVENT`           | `dc3.e.event`            |
| `ROUTING_POINT_COMMAND_PREFIX`   | `dc3.r.point_command.`   |
| `ROUTING_COMMAND_PREFIX`         | `dc3.r.command.`         |
| `ROUTING_DRIVER_METADATA_PREFIX` | `dc3.r.metadata.driver.` |
| `ROUTING_POINT_VALUE_PREFIX`     | `dc3.r.value.point.`     |

The runtime may prepend the `dc3.rabbit.tag` system-property value to exchange, queue, and selected routing constants.
Point read/write operations use the point-command constants; `TOPIC_EXCHANGE_COMMAND` is reserved for custom commands.
Routing keys are suffixed with the driver's service name. Do not rename them without updating every producer, binding,
consumer, and deployed queue migration.

### Common Constants

| Class                               | Purpose                                       |
|-------------------------------------|-----------------------------------------------|
| `DefaultConstant`                   | Platform defaults (page size, etc.)           |
| `TimeConstant`                      | Date/time format strings                      |
| `RequestConstant`                   | HTTP header key names (tenant/user injection) |
| `RequestIdConstant`                 | Cross-transport request correlation keys      |
| `PrefixConstant` / `SuffixConstant` | Common cache/key prefixes and suffixes        |

### Enumerations

Located in `io.github.pnoker.common.enums`. The list below is a selection of the most commonly referenced enums; see the
package for the complete set (alarm, MCP, OAuth, notify, and other domain enums live there too):

- `EnableFlagEnum` — Boolean-like enable/disable state
- `EntityStatusEnum` — Online, offline, maintenance, and fault states
- `DriverTypeEnum` — Driver client/server, gateway, and connection classifications
- `PointTypeEnum` — Point value types such as string, numeric, and boolean
- `MetadataOperateTypeEnum` — Metadata add/delete/update operations
- `PointCommandTypeEnum` — Point read, batch-read, write, batch-write, and configuration commands
- `AttributeTypeEnum` — Attribute value types
- `ProfileShareTypeEnum` — Tenant-, driver-, and user-scoped profile sharing

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-constant -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-constant -am test
```

## Related Modules

Used as a dependency by virtually all other `dc3-common-*` and `dc3-center-*` modules.
