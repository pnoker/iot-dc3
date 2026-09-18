# DC3 Center Data

## Overview

`dc3-center-data` is the Data Center of the IoT DC3 platform. It consumes device point values from drivers over RabbitMQ
(AMQP), stores them through the pluggable time-series port (`dc3-tsdb`, TimescaleDB by default), and exposes data query
and command APIs.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-data
- **Package**: `io.github.pnoker.center.data`

## Service Ports

| Protocol  | Port   | Configuration variable |
|-----------|--------|------------------------|
| HTTP REST | `8500` | `DC3_DATA_PORT`        |
| gRPC      | `9500` | `DC3_DATA_GRPC_PORT`   |

## Key Responsibilities

- **Point Value Ingestion**: Receives point values from drivers via RabbitMQ (`dc3.e.value` exchange,
  `dc3.q.value.point` queue) and persists them to the time-series storage
- **Point Value Query**: Exposes REST and gRPC APIs to query the latest and historical point values
- **Point Command Dispatch**: Resolves the target driver through `DriverFacade` and publishes point read/write commands
  to `dc3.e.point_command`
- **Custom Command Dispatch**: Publishes custom device commands to `dc3.e.command`
- **Driver and Device Status**: Tracks state, timeout, and alarm events
- **Data Query**: Supports pagination query, real-time telemetry, and historical data retrieval

## REST Endpoints (via Gateway)

Accessible through the gateway at `/api/v3/data/**` (authentication required).

## Messaging Topics

| Exchange              | Direction | Purpose                            |
|-----------------------|-----------|------------------------------------|
| `dc3.e.value`         | Inbound   | Receive point values from drivers  |
| `dc3.e.point_command` | Outbound  | Dispatch point read/write commands |
| `dc3.e.command`       | Outbound  | Dispatch custom device commands    |
| `dc3.e.state`         | Inbound   | Receive driver/device state events |
| `dc3.e.event`         | Inbound   | Receive reported domain events     |

## Dependencies

This service wires `dc3-common-data` which contains all business logic.

```xml

<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-data</artifactId>
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
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-data -am package
```

### 3. Run (after auth and manager are up)

```bash
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-data -am test
```

## Related Modules

- `dc3-api-data` - gRPC API contracts for point value queries
- `dc3-api-manager` - gRPC API for resolving driver/point metadata
- `dc3-common-data` - Business logic implementation
- `dc3-tsdb-core` - Pluggable time-series storage adapter (TimescaleDB adapter selected by default)
