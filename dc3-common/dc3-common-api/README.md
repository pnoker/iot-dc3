# DC3 Common API

## Overview

`dc3-common-api` provides shared gRPC utility classes used across all services in the IoT DC3 platform. It contains
builder utilities for constructing gRPC request/response objects from domain model entities.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-api

## Key Components

- **`GrpcBuilderUtil`** — Utility class for building common gRPC DTOs from BO/DO entities (e.g., setting pagination,
  building result wrappers)

## Dependencies

This module is included in any service that imports a `dc3-api-*` module. It bridges domain model objects with generated
protobuf classes.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-api -am package
```

## Testing

This module currently has no module-specific automated tests. Verify generated or production sources by compiling the
affected reactor from the repository root:

```bash
mvn -s .mvn/settings.xml -q -pl dc3-common/dc3-common-api -am -DskipTests compile
```

## Related Modules

- `dc3-api-auth` / `dc3-api-data` / `dc3-api-driver` / `dc3-api-manager` — gRPC API contracts that use these utilities
