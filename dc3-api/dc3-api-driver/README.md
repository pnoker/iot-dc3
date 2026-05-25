# DC3 API Driver

## Overview

`dc3-api-driver` provides gRPC service definitions for driver services in the IoT DC3 platform. It defines the
communication interface between device drivers and the platform's
manager service, enabling device registration, metadata synchronization, and point value collection.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-api-driver
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.api.common.driver`

## Proto Definitions

### driver_driver.proto

Defines driver registration RPC calls and data structures.

**Service**: `DriverApi`

- `DriverRegister` - Register a driver instance with the platform (used by drivers on startup)

**Key Messages**:

- `GrpcDriverRegisterDTO` - Driver registration request information
- `GrpcRDriverRegisterDTO` - Driver registration response with configuration data

### driver_device.proto

Defines device-related RPC calls for driver services.

**Service**: `DeviceApi`

- `SelectByPage` - Query devices with pagination support
- `SelectById` - Query device by device ID

**Key Messages**:

- `GrpcPageDeviceQuery` - Paginated device query request
- `GrpcDeviceQuery` - Single device query request
- `GrpcRPageDeviceDTO` - Paginated device list response
- `GrpcRDeviceDTO` - Single device response
- `GrpcRDeviceAttachDTO` - Device with full configuration (device + points + attributes)

### driver_point.proto

Defines point-related RPC calls for driver services.

**Service**: `PointApi`

- `SelectByPage` - Query points with pagination support
- `SelectById` - Query point by point ID

**Key Messages**:

- `GrpcPagePointQuery` - Paginated point query request
- `GrpcPointQuery` - Single point query request
- `GrpcRPagePointDTO` - Paginated point list response
- `GrpcRPointDTO` - Single point response

### driver_entity.proto

Defines driver entity structures used during registration.

**Key Messages**:

- `GrpcDriverRegisterDTO` - Complete driver registration information including:
    - Tenant identification
    - Driver client name (service instance identifier)
    - Driver metadata (GrpcDriverDTO)
    - Driver attribute definitions
    - Point attribute definitions

### driver_query.proto

Defines query structures for driver services.

**Key Messages**:

- `GrpcDriverQuery` - Driver query by driver_id
- `GrpcDeviceQuery` - Device query by driver_id and device_id
- `GrpcPointQuery` - Point query by driver_id and point_id

### driver_query_page.proto

Defines paginated query structures for driver services.

**Key Messages**:

- `GrpcPageDeviceQuery` - Paginated device query with filters (tenant, driver, device)
- `GrpcPagePointQuery` - Paginated point query with filters (tenant, driver, device, profile)

## Dependencies

This module depends on common proto definitions:

- `api/common/entity.proto` - Common entity definitions (Driver, Device, Point, Attributes)
- `api/common/page.proto` - Pagination support
- `api/common/r.proto` - Common response wrapper

## Usage

### 1. Add Dependency

```xml

<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-api-driver</artifactId>
  <version>2026.5.22</version>
</dependency>
```

### 2. Driver Registration Flow

```protobuf
// 1. Driver builds registration request
    GrpcDriverRegisterDTO registerRequest = GrpcDriverRegisterDTO.newBuilder()
    .setTenant("default")
    .setClient("modbus-tcp-driver-001")
    .setDriver(driverInfo)
    .addAllDriverAttributes(driverAttributes)
    .addAllPointAttributes(pointAttributes)
    .build();

// 2. Call registration service
    GrpcRDriverRegisterDTO response = driverApi.driverRegister(registerRequest);

// 3. Extract configuration from response
    GrpcDriverDTO driver = response.getDriver();
    List<Long> deviceIds = response.getDeviceIdsList();
    List<GrpcDriverAttributeDTO> driverAttrs = response.getDriverAttributesList();
    List<GrpcPointAttributeDTO> pointAttrs = response.getPointAttributesList();

// 4. Load device configurations
    for (Long deviceId : deviceIds) {
GrpcDeviceQuery query = GrpcDeviceQuery.newBuilder()
    .setDriverId(driver.getId())
    .setDeviceId(deviceId)
    .build();
    GrpcRDeviceDTO deviceResponse = deviceApi.selectById(query);
// Process device configuration
    }
```

### 3. Query Device Configuration

```java
// Query device with full configuration
GrpcRDeviceAttachDTO deviceAttach = deviceApi.selectById(deviceQuery);

// Extract device information
GrpcDeviceDTO device = deviceAttach.getDevice();
List<Long> pointIds = deviceAttach.getPointIdsList();
List<GrpcDriverAttributeConfigDTO> driverConfigs = deviceAttach.getDriverConfigsList();
List<GrpcPointAttributeConfigDTO> pointConfigs = deviceAttach.getPointConfigsList();

// Initialize device connection
initializeDevice(device, driverConfigs, pointConfigs);
```

### 4. Query Point Configuration

```java
// Query point by ID
GrpcPointQuery pointQuery = GrpcPointQuery.newBuilder()
                .setDriverId(driverId)
                .setPointId(pointId)
                .build();

GrpcRPointDTO pointResponse = pointApi.selectById(pointQuery);
GrpcPointDTO point = pointResponse.getData();

// Configure point reading
configurePoint(point);
```

## Driver Service Lifecycle

### 1. Startup Phase

```
Driver Service Start
    ↓
Build GrpcDriverRegisterDTO
    ↓
Call DriverApi.driverRegister()
    ↓
Receive Driver Configuration
    ↓
Load Device Configurations
    ↓
Load Point Configurations
    ↓
Initialize Device Connections
    ↓
Ready for Data Collection
```

### 2. Runtime Phase

```
Periodic Data Collection
    ↓
Read Point Values from Device
    ↓
Convert and Format Values
    ↓
Send to Data Center
    ↓
Repeat
```

### 3. Shutdown Phase

```
Driver Service Stop
    ↓
Close Device Connections
    ↓
Unregister from Manager
    ↓
Cleanup Resources
```

## Data Models

### Driver Metadata (GrpcDriverDTO)

- **driver_name**: Display name
- **driver_code**: Unique identifier
- **service_name**: Driver service type
- **service_host**: Deployment location
- **driver_type_flag**: Driver type (e.g., virtual, modbus, opc-ua)
- **driver_ext**: Extended configuration (JSON)
- **enable_flag**: Active status
- **signature**: Driver signature for validation
- **version**: Configuration version

### Device Metadata (GrpcDeviceDTO)

- **device_name**: Display name
- **device_code**: Unique identifier
- **driver_id**: Associated driver
- **device_ext**: Extended configuration (JSON)
- **enable_flag**: Active status
- **signature**: Device signature
- **version**: Configuration version

### Point Metadata (GrpcPointDTO)

- **point_name**: Display name
- **point_code**: Unique identifier
- **point_type_flag**: Data type (digital, analog, string)
- **rw_flag**: Read/write permission
- **base_value**: Base value for conversion
- **multiple**: Scale factor
- **value_decimal**: Precision
- **unit**: Measurement unit
- **profile_id**: Associated profile template

### Attribute Configuration

Drivers and points can have configurable attributes:

- **Driver Attributes**: Driver-level configuration (e.g., connection settings)
- **Point Attributes**: Point-level configuration (e.g., register address)

Attributes support:

- Type specification (string, number, boolean)
- Default values
- Per-device configuration overrides
- Dynamic configuration updates

## API Features

### Multi-Tenancy Support

- Tenant-scoped driver registration
- Tenant-isolated device and point queries
- Cross-tenant operation prevention

### Configuration Synchronization

- Automatic configuration distribution on registration
- Version-based configuration updates
- Signature-based change detection

### Extensibility

- Custom driver types via driver_type_flag
- Extended configuration via *_ext fields
- Custom attribute definitions

### Query Optimization

- Pagination support for large device/point lists
- Driver-scoped queries for performance
- Profile-based filtering for template queries

## Integration Points

### Driver Implementation

```java
// Driver service implements DriverApi for registration
public class ModbusTcpDriverService {
    private final DriverApiGrpc.DriverApiBlockingStub driverApi;
    private final DeviceApiGrpc.DeviceApiBlockingStub deviceApi;
    private final PointApiGrpc.PointApiBlockingStub pointApi;

    public void start() {
        // Register driver
        registerDriver();

        // Load configurations
        loadDeviceConfigurations();

        // Start data collection
        startDataCollection();
    }
}
```

### Manager Service

Manager service implements these APIs to provide:

- Driver registration handling
- Device metadata management
- Point metadata management
- Configuration distribution

### Data Collection

```
Driver Device SDK → Read Device → Convert Data → Send to Data Center
```

## Communication Pattern

### Synchronous Queries

- Configuration queries (device, point)
- Registration operations

### Asynchronous Events

- Device connection status updates
- Point value changes (via message queue)
- Configuration change notifications

## Error Handling

### Registration Failures

- Invalid tenant
- Duplicate driver instance
- Missing required attributes

### Query Failures

- Device not found
- Point not found
- Permission denied
- Driver not registered

## Performance Considerations

- **Connection Pooling**: Reuse gRPC channels for manager communication
- **Configuration Caching**: Cache device/point configurations locally
- **Batch Queries**: Use pagination queries for loading multiple devices/points
- **Lazy Loading**: Load device configurations on-demand

## Build Instructions

```bash
# Build the module
mvn -s ../../.mvn/settings.xml clean package

# Install to local repository
mvn -s ../../.mvn/settings.xml clean install
```

## Related Modules

- `dc3-api-manager` - Manager service API for device/driver/point management
- `dc3-api-data` - Data service API for point value storage
- `dc3-common-driver` - Driver framework SDK

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
