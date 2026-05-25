# DC3 API Manager

## Overview

`dc3-api-manager` provides gRPC service definitions for the Manager Center in the IoT DC3 platform. It defines the
interfaces used by the Data service and other consumers to query
driver, device, and point metadata from the Manager Center.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-api-manager
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.api.center.manager`

## Proto Definitions

### manager_driver.proto

Defines driver-related RPC calls from the manager perspective.

**Service**: `DriverApi`

- `SelectByDeviceId` - Query driver information by device ID
- `SelectByServiceName` - Query driver information by service name

**Key Messages**:

- `GrpcDriverQuery` - Driver query by device ID
- `GrpcServiceNameQuery` - Driver query by service name
- `GrpcRDriverDTO` - Response wrapper containing driver information
- `GrpcDriverDTO` - Driver data structure (service name, driver type, host, etc.)

### manager_device.proto

Defines device-related RPC calls from the manager perspective.

**Service**: `DeviceApi`

- `SelectById` - Query device information by device ID

**Key Messages**:

- `GrpcDeviceQuery` - Device query by device ID
- `GrpcRDeviceDTO` - Response wrapper containing device information

### manager_point.proto

Defines point-related RPC calls from the manager perspective.

**Service**: `PointApi`

- `SelectById` - Query point information by point ID

**Key Messages**:

- `GrpcPointQuery` - Point query by point ID
- `GrpcRPointDTO` - Response wrapper containing point information

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
GrpcRDriverDTO response = driverApiBlockingStub.selectByDeviceId(query);
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

