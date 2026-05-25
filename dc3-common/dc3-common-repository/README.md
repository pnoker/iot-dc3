# DC3 Common Repository

## Overview

`dc3-common-repository` is the pluggable data storage interface module of the IoT DC3 platform. It abstracts point value
persistence through a `RepositoryService` interface,
allowing the Data Center to store telemetry data in different backends without coupling business logic to a specific
storage implementation.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-repository
- **Version**: 2026.5.22

## Key Components

| Component                          | Purpose                                                                                       |
|------------------------------------|-----------------------------------------------------------------------------------------------|
| `RepositoryService`                | Storage interface: save point values and query latest/history/page data                       |
| `RepositoryStrategyFactory`        | Runtime registry for available `RepositoryService` implementations                            |
| `PointValueBO`                     | Business object representing a point value with timestamp, device ID, point ID, and raw value |
| `PointQueryBO` / `PointValueQuery` | Query objects for paginated and filter-based retrieval                                        |
| `ActiveRepositoryProfileConfig`    | Activates the `repository` profile unless `dc3.repository.auto-profile=false` is set          |

## Usage Pattern

The Data Center discovers storage implementations through `RepositoryStrategyFactory`. The active implementation is
selected by the application profile. Add a concrete storage implementation (e.g. TimescaleDB, InfluxDB, MongoDB) by
implementing `RepositoryService`, registering it as a Spring bean, and adding it to the factory during initialization.

```java
@Service
public class TimescaleRepositoryServiceImpl implements RepositoryService, InitializingBean {

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.POSTGRES;
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(this);
    }

    // Implement savePointValue, selectLatestPointValue, selectPagePointValue, etc.
}
```

Set `dc3.repository.auto-profile=false` to opt out of automatic `repository` profile activation.

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-data` — Uses `RepositoryStrategyFactory` to route point-value persistence operations
- `dc3-common-data` — Caches latest point values with in-process `LocalCacheService` alongside repository storage

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
