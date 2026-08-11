# DC3 Center Single

## Overview

`dc3-center-single` is the Integrated All-in-One Center of the IoT DC3 platform. It combines authorization, data, and
management services into a single deployable module for simplified single-node or lightweight deployment scenarios.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-single
- **Package**: `io.github.pnoker.center.single`

## Service Ports

| Protocol  | Port   | Configuration variable |
|-----------|--------|------------------------|
| HTTP REST | `8100` | `DC3_SINGLE_PORT`       |
| gRPC      | `9100` | `DC3_SINGLE_GRPC_PORT`  |

## Key Responsibilities

Combines all capabilities of the three center services into one process:

- All capabilities of `dc3-center-auth` (token, tenant, user management)
- All capabilities of `dc3-center-data` (point value ingestion, query)
- All capabilities of `dc3-center-manager` (driver, device, profile, point management)

## Use Case

Designed for:

- Development and testing environments where running three separate service processes is unnecessary
- Lightweight edge/small-scale deployments where resource constraints favor a single JVM process

## Dependencies

Wires all three common service modules:

```xml
<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-auth</artifactId>
</dependency>
<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-data</artifactId>
</dependency>
<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-manager</artifactId>
</dependency>
```

## Configuration

- `application.yml` — base port and profile config
- `application-dev.yml` — dev env: single Postgres/RabbitMQ config
- `application-pre.yml` — pre-release runtime overrides for the all-in-one process
- `application-pro.yml` — production target config

## Running Locally

### 1. Start Infrastructure

```bash
make up-db
```

### 2. Build

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-single -am package
```

### 3. Run

```bash
java -jar dc3-center/dc3-center-single/target/dc3-center-single.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-single -am test
```

## Related Modules

- `dc3-center-auth` — Standalone auth service
- `dc3-center-data` — Standalone data service
- `dc3-center-manager` — Standalone manager service
- `dc3-common-auth` / `dc3-common-data` / `dc3-common-manager` — Shared business logic
