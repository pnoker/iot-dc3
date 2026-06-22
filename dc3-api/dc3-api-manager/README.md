# DC3 API Manager

## Overview

`dc3-api-manager` provides gRPC service definitions for the Manager Center in the IoT DC3 platform. It defines the
interfaces used by the Data service and other consumers to query
driver, device, point, profile, command, and event metadata from the Manager Center.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-api-manager
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.api.center.manager`

## Proto Definitions

### manager_driver.proto

Defines driver-related RPC calls from the manager perspective.

**Service**: `DriverApi`

- `ListByPage` - Query drivers with pagination support
- `GetByDriverId` - Query driver information by driver ID
- `ListByDriverIds` - Query drivers by a list of driver IDs
- `GetByDeviceId` - Query driver information by device ID

**Key Messages**:

- `GrpcPageDriverQuery` - Paginated driver query request
- `GrpcDriverQuery` - Driver query by driver ID / device ID
- `GrpcDriverIdsQuery` - Driver query by a list of driver IDs
- `GrpcRDriverDTO` - Response wrapper containing driver information
- `GrpcRPageDriverDTO` / `GrpcRDriverListDTO` - Paginated / list responses
- `GrpcDriverDTO` - Driver data structure (service name, driver type, host, etc.)

### manager_device.proto

Defines device-related RPC calls from the manager perspective.

**Service**: `DeviceApi`

- `ListByPage` - Query devices with pagination support
- `ListByProfileId` - Query devices by profile ID
- `ListByDriverId` - Query devices by driver ID
- `GetByDeviceId` - Query device information by device ID
- `ListByDeviceIds` - Query devices by a list of device IDs

**Key Messages**:

- `GrpcPageDeviceQuery` - Paginated device query request
- `GrpcDeviceQuery` - Single device query
- `GrpcDeviceIdsQuery` - Device query by a list of device IDs
- `GrpcRDeviceDTO` / `GrpcRPageDeviceDTO` / `GrpcRDeviceListDTO` - Single / paginated / list responses

### manager_point.proto

Defines point-related RPC calls from the manager perspective.

**Service**: `PointApi`

- `ListByPage` - Query points with pagination support
- `GetById` - Query point information by point ID
- `ListByIds` - Query points by a list of point IDs

**Key Messages**:

- `GrpcPagePointQuery` - Paginated point query request
- `GrpcPointQuery` - Single point query
- `GrpcPointIdsQuery` - Point query by a list of point IDs
- `GrpcRPointDTO` / `GrpcRPagePointDTO` / `GrpcRPointListDTO` - Single / paginated / list responses

### manager_command.proto

Defines command-related RPC calls from the manager perspective.

**Service**: `CommandApi`

- `ListByPage` - Query commands with pagination support
- `GetById` - Query command information by command ID
- `ListByIds` - Query commands by a list of command IDs

### manager_event.proto

Defines event-related RPC calls from the manager perspective.

**Service**: `EventApi`

- `ListByPage` - Query events with pagination support
- `GetById` - Query event information by event ID
- `ListByIds` - Query events by a list of event IDs

### manager_profile.proto

Defines profile-related RPC calls from the manager perspective.

**Service**: `ProfileApi`

- `ListByPage` - Query profiles with pagination support
- `GetByProfileId` - Query profile information by profile ID
- `ListByProfileIds` - Query profiles by a list of profile IDs
- `ListByDeviceId` - Query profiles bound to a device

### manager_query.proto / manager_query_page.proto

Shared query and paginated query structures for manager RPC calls.

## Dependencies

This module depends on common proto definitions:

- `api/common/entity.proto` - Common entity definitions
- `api/common/page.proto` - Pagination support
- `api/common/r.proto` - Common response wrapper

## Usage

### 1. Add Dependency

```xml

<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-api-manager</artifactId>
    <version>2026.5.22</version>
</dependency>
```

### 2. Inject gRPC Client

```java

@GrpcClient(ManagerConstant.SERVICE_NAME)
private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

@GrpcClient(ManagerConstant.SERVICE_NAME)
private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;
```

### 3. Query Driver by Device

```java
GrpcDeviceQuery query = GrpcDeviceQuery.newBuilder()
        .setDeviceId(deviceId)
        .build();
GrpcRDriverDTO response = driverApiBlockingStub.getByDeviceId(query);
if(response.

getResult().

getOk()){
String serviceName = response.getData().getServiceName();
}
```

## Integration Points

- Used by `dc3-common-data` to resolve the target driver service name before publishing RabbitMQ commands
- Used by `dc3-common-data` to look up point metadata for data queries

## Build Instructions

```bash
# Build the module (run from repo root)
mvn -s .mvn/settings.xml clean package

# Install to local repository
mvn -s .mvn/settings.xml clean install

# Build this module only
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-api-driver` - Driver-side gRPC API for driver registration and config sync
- `dc3-common-data` - Consumes this API to resolve driver/point metadata
- `dc3-common-manager` - Implements this API as `@GrpcService` server

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

