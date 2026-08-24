# DC3 Center Manager

## Overview

`dc3-center-manager` is the Device Management Center of the IoT DC3 platform. It provides comprehensive management for
all device collections including device/driver registration, profile management, point configuration, permission
management, and command interfaces.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-manager
- **Package**: `io.github.pnoker.center.manager`

## Service Ports

| Protocol  | Port   | Configuration variable |
|-----------|--------|------------------------|
| HTTP REST | `8400` | `DC3_MANAGER_PORT`      |
| gRPC      | `9400` | `DC3_MANAGER_GRPC_PORT` |

## Key Responsibilities

- **Driver Management**: Driver registration, attribute management, metadata sync
- **Device Management**: Device CRUD, group management, device-profile binding
- **Profile Management**: Profile templates and point attribute configuration
- **Point Management**: Point definitions, type flags, scale/precision settings
- **gRPC Server**: Implements `DriverApi`, `DeviceApi`, `PointApi` for driver registration and data query
- **Metadata Events**: Publishes metadata change events over RabbitMQ to notify relevant drivers
- **Scheduled Jobs**: Quartz-based hourly maintenance job (`HourlyJobForManager`)

## REST Endpoints (via Gateway)

Accessible through the gateway at `/api/v3/manager/**` (authentication required).

Key endpoint prefixes (defined in `ManagerConstant`):

| Prefix              | Resource                |
|---------------------|-------------------------|
| `/driver`           | Driver management       |
| `/device`           | Device management       |
| `/profile`          | Profile templates       |
| `/point`            | Point definitions       |
| `/driver_attribute` | Driver-level attributes |
| `/point_attribute`  | Point-level attributes  |
| `/command`          | Device commands         |
| `/event`            | Device events           |
| `/group`            | Device groups           |
| `/topic`            | MQTT/data topics        |

The complete prefix set (labels, dictionaries, attribute configs, dashboards, batch operations) lives in
`ManagerConstant`.

## gRPC Services (consumed by drivers and data service)

| Service                    | Used by                                         |
|----------------------------|-------------------------------------------------|
| `DriverApi.DriverRegister` | Drivers registering on startup                  |
| `DeviceApi.GetById`        | Drivers fetching device configuration           |
| `PointApi.GetById`         | Drivers fetching point configuration            |
| `DriverApi.GetByDeviceId`  | Distributed facades resolving command routing   |

## Dependencies

This service wires `dc3-common-manager` which contains all business logic.

```xml

<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-manager</artifactId>
</dependency>
```

## Configuration

- `application.yml` — base port and profile config
- `application-dev.yml` — dev env: Postgres, RabbitMQ, gRPC client addresses
- `application-pre.yml` — pre-release datasource, messaging, and static gRPC client addresses
- `application-pro.yml` — production datasource, messaging, and static gRPC client addresses

## Running Locally

### 1. Start Infrastructure

```bash
make up-db
```

### 2. Build

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-manager -am package
```

### 3. Run (after auth is up)

```bash
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-manager -am test
```

## Related Modules

- `dc3-api-driver` - Driver-side gRPC API implemented by this service
- `dc3-api-manager` - Manager-side gRPC API implemented by this service
- `dc3-common-manager` - Business logic implementation
