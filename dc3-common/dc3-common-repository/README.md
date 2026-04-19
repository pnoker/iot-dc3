# DC3 Common Repository

## Overview

`dc3-common-repository` is the pluggable data storage interface module of the IoT DC3 platform. It abstracts point value persistence through a `RepositoryService` interface,
allowing the Data Center to store telemetry data in different backends without coupling business logic to a specific storage implementation.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-repository
- **Version**: 2026.4.19

## Key Components

| Component                          | Purpose                                                                                                 |
|------------------------------------|---------------------------------------------------------------------------------------------------------|
| `RepositoryService`                | Storage interface: `save(PointValueBO)`, `saveList(List<PointValueBO>)`, `latest(query)`, `page(query)` |
| `PointValueBO`                     | Business object representing a point value with timestamp, device ID, point ID, and raw value           |
| `PointQueryBO` / `PointValueQuery` | Query objects for paginated and filter-based retrieval                                                  |

## Usage Pattern

The Data Center depends on `RepositoryService` via Spring injection. The active implementation is selected by the application profile. Add a concrete storage implementation (e.g.,
TimescaleDB, InfluxDB, MongoDB) by implementing `RepositoryService` and registering it as a Spring bean.

```java
@Resource
private RepositoryService repositoryService;

// Persist ingested point value
repositoryService.save(pointValueBO);

// Query latest value for a point
PointValueBO latest = repositoryService.latest(queryBO);
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-data` — Injects and uses `RepositoryService` for all persistence operations
- `dc3-common-redis` — Caches latest point values alongside repository storage

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

