# DC3 Common API

## Overview

`dc3-common-api` provides shared gRPC utility classes used across all services in the IoT DC3 platform. It contains
builder utilities for constructing gRPC request/response objects
from domain model entities.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-api
- **Version**: 2026.5.22

## Key Components

- **`GrpcBuilderUtil`** — Utility class for building common gRPC DTOs from BO/DO entities (e.g., setting pagination,
  building result wrappers)

## Dependencies

This module is included in any service that imports a `dc3-api-*` module. It bridges domain model objects with generated
protobuf classes.

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-api-auth` / `dc3-api-data` / `dc3-api-driver` / `dc3-api-manager` — gRPC API contracts that use these utilities

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

