# DC3 Center Data

## Overview

`dc3-center-data` is the Data Center of the IoT DC3 platform. It integrates common messaging middleware including AMQP,
WebSocket, and MQTT for collecting device point values from
drivers, storing them in the time-series repository, and exposing data query APIs.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-data
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.center.data`

## Service Ports

| Protocol  | Port                                                 |
|-----------|------------------------------------------------------|
| HTTP REST | `8500` (default, overridable via `SERVER_PORT`)      |
| gRPC      | `9500` (default, overridable via `GRPC_SERVER_PORT`) |

## Key Responsibilities

- **Point Value Ingestion**: Receives point values from drivers via RabbitMQ (`dc3.e.value` exchange,
  `dc3.q.value.point` queue) and persists them to the time-series storage
- **Point Value Query**: Exposes REST and gRPC APIs to query the latest and historical point values
- **Device Command Dispatch**: Receives read/write commands, resolves the target driver via Manager gRPC (
  `ManagerConstant.SERVICE_NAME`), and publishes to `dc3.e.command`
- **Driver Status**: Tracks driver online/offline status events
- **Data Query**: Supports pagination query, real-time telemetry, and historical data retrieval

## REST Endpoints (via Gateway)

Accessible through the gateway at `/api/v3/data/**` (authentication required).

## Messaging Topics

| Exchange        | Direction | Purpose                                 |
|-----------------|-----------|-----------------------------------------|
| `dc3.e.value`   | Inbound   | Receive point values from drivers       |
| `dc3.e.command` | Outbound  | Dispatch read/write commands to drivers |
| `dc3.e.event`   | Inbound   | Receive driver/device status events     |

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

### 3. Run (after auth and manager are up)

```bash
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar
```

## Related Modules

- `dc3-api-data` - gRPC API contracts for point value queries
- `dc3-api-manager` - gRPC API for resolving driver/point metadata
- `dc3-common-data` - Business logic implementation
- `dc3-common-repository` - Pluggable time-series storage adapter

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
