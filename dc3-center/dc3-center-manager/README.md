# DC3 Center Manager

## Overview

`dc3-center-manager` is the Device Management Center of the IoT DC3 platform. It provides comprehensive management for
all device collections including device/driver registration,
profile management, point configuration, permission management, and command interfaces.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-manager
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.center.manager`

## Service Ports

| Protocol  | Port                                                 |
|-----------|------------------------------------------------------|
| HTTP REST | `8400` (default, overridable via `SERVER_PORT`)      |
| gRPC      | `9400` (default, overridable via `GRPC_SERVER_PORT`) |

## Key Responsibilities

- **Driver Management**: Driver registration, attribute management, metadata sync
- **Device Management**: Device CRUD, group management, device-profile binding
- **Profile Management**: Profile templates and point attribute configuration
- **Point Management**: Point definitions, type flags, scale/precision settings
- **gRPC Server**: Implements `DriverApi`, `DeviceApi`, `PointApi` for driver registration and data query
- **Metadata Events**: Publishes metadata change events over RabbitMQ to notify relevant drivers
- **Scheduled Jobs**: Hourly jobs for platform statistics (e.g., point data volume)

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
| `/group`            | Device groups           |
| `/topic`            | MQTT/data topics        |

## gRPC Services (consumed by drivers and data service)

| Service                      | Used By                          |
|------------------------------|----------------------------------|
| `DriverApi.driverRegister`   | Drivers on startup               |
| `DeviceApi.selectById`       | Drivers fetching device config   |
| `PointApi.selectById`        | Drivers and data service         |
| `DriverApi.selectByDeviceId` | Data service for command routing |

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
- `application-pre.yml` — pre-release: Nacos-based service discovery
- `application-pro.yml` — production: Nacos-based service discovery

## Running Locally

### 1. Start Infrastructure

```bash
podman compose -f dc3/docker-compose-db.yml up -d
```

### 2. Build

```bash
mvn -s .mvn/settings.xml clean package
```

### 3. Run (after auth is up)

```bash
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

## Related Modules

- `dc3-api-driver` - Driver-side gRPC API implemented by this service
- `dc3-api-manager` - Manager-side gRPC API implemented by this service
- `dc3-common-manager` - Business logic implementation

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
