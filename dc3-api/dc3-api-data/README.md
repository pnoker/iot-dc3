# DC3 API Data

## Overview

`dc3-api-data` provides gRPC service definitions for device data collection and management in the IoT DC3 platform. It
defines interfaces for querying real-time device point values
and historical data retrieval.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-api-data
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.api.center.data`

## Proto Definitions

### point_value.proto

Defines point value query RPC calls and data structures for device data collection.

**Service**: `PointValueApi`

- `LastValue` - Query the latest collected value of a device point

**Key Messages**:

- `GrpcPointValueQuery` - Request wrapper for point value queries (device_id, point_id, tenant_id)
- `GrpcRPointValueDTO` - Response wrapper containing point value information
- `GrpcPointValueDTO` - Point value data structure

## Data Models

### Point Value Model

The `GrpcPointValueDTO` represents collected data from device points:

```protobuf
message GrpcPointValueDTO {
  int64 id = 1;           // Point value record ID
  int64 device_id = 2;    // Source device identifier
  int64 point_id = 3;     // Point identifier
  string value = 4;       // Processed/converted value
  string raw_value = 5;   // Raw value from device
  int64 create_time = 6;  // Storage timestamp
}
```

### Field Descriptions

| Field         | Type   | Description                                          |
|---------------|--------|------------------------------------------------------|
| `id`          | int64  | Unique identifier for the point value record         |
| `device_id`   | int64  | ID of the device that generated the data             |
| `point_id`    | int64  | ID of the point (tag) within the device              |
| `value`       | string | Processed value after data conversion and formatting |
| `raw_value`   | string | Original raw value collected from the device         |
| `create_time` | int64  | Unix timestamp when data was stored (milliseconds)   |

## Dependencies

This module depends on common proto definitions:

- `api/common/r.proto` - Common response wrapper (GrpcR)

## Usage

### 1. Add Dependency

```xml

<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-api-data</artifactId>
    <version>2026.5.22</version>
</dependency>
```

### 2. Import Proto Files

```protobuf
import "api/common/data/point_value.proto";
```

### 3. Implement Service

```java
public class PointValueServiceImpl extends PointValueApiGrpc.PointValueApiImplBase {
    @Override
    public void lastValue(GrpcPointValueQuery request,
                          StreamObserver<GrpcRPointValueDTO> responseObserver) {
        // Query latest point value from database
        // Return GrpcRPointValueDTO with point value data
    }
}
```

### 4. Query Example

```java
// Build query request
GrpcPointValueQuery query = GrpcPointValueQuery.newBuilder()
                .setDeviceId(12345L)
                .setPointId(67890L)
                .setTenantId(1L)
                .build();

// Call service
GrpcRPointValueDTO response = pointValueApi.lastValue(query);

// Extract data
if(response.

getResult().

getOk()){
GrpcPointValueDTO pointValue = response.getData();
String value = pointValue.getValue();
String rawValue = pointValue.getRawValue();
long timestamp = pointValue.getCreateTime();
}
```

## API Features

### Real-Time Data Access

- Query latest point values by device and point IDs
- Support for both raw and processed values
- Tenant-scoped data access

### Data Processing

- **Raw Value**: Original data read from the device without processing
- **Processed Value**: Converted and formatted value after applying:
    - Unit conversion
    - Scale factors
    - Precision formatting
    - Data type conversion

### Data Flow

```
Device → Driver → Data Collection → Time Series DB → PointValueApi
```

## Use Cases

### 1. Real-Time Monitoring

```java
// Monitor device point value in real-time
GrpcPointValueDTO latestValue = getLastValue(deviceId, pointId);

displayValue(latestValue.getValue());
```

### 2. Data Analysis

```java
// Compare raw vs processed values
String raw = pointValue.getRawValue();
String processed = pointValue.getValue();

analyzeConversion(raw, processed);
```

### 3. Time Series Operations

```java
// Use timestamp for time-based queries
long timestamp = pointValue.getCreateTime();
Date collectionTime = new Date(timestamp);
```

## Performance Considerations

- **Query Optimization**: Always include tenant_id for proper data isolation
- **Caching**: Consider caching recent point values for high-frequency queries
- **Time Series Database**: This API typically queries time-series databases (e.g., InfluxDB, TDengine)
- **Data Volume**: Point value queries can generate high traffic in large-scale deployments

## Integration Points

### Driver Services

Drivers write point values to the data layer:

```
Driver Service → Device SDK → Point Collection → Data Store
```

### Manager Service

Manager service provides device/point metadata:

```
PointValueApi → ManagerApi (get point info) → Return value with metadata
```

### Application Layer

Upper applications consume point value data for:

- Real-time dashboards
- Data analytics
- Alarm monitoring
- Historical data queries

## Data Retention

Point values are typically stored in time-series databases with:

- **High-frequency data**: Raw values for immediate processing
- **Aggregated data**: Processed values for long-term storage
- **Retention policies**: Configurable by tenant or device type

## Build Instructions

```bash
# Build the module
mvn -s ../../.mvn/settings.xml clean package

# Install to local repository
mvn -s ../../.mvn/settings.xml clean install
```

## Related Modules

- `dc3-api-driver` - Driver interface for point data collection
- `dc3-api-manager` - Device and point metadata management
- `dc3-common-data` - Data models and DTOs

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
