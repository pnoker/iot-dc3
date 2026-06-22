# DC3 Common SQL

## Overview

`dc3-common-sql` is the shared base for the database (JDBC) drivers of the IoT DC3 platform. It provides
`AbstractJdbcDriverCustomService`, a common driver-service base that handles JDBC connection management (via HikariCP)
and query execution, so each database driver only supplies its dialect/connection specifics.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-sql
- **Version**: 2026.5.22

## Key Components

| Component                        | Purpose                                                              |
|----------------------------------|----------------------------------------------------------------------|
| `AbstractJdbcDriverCustomService`| Base driver service for JDBC sources — pooled connections + queries  |

## Dependencies

- `dc3-common-driver` — Driver SDK that the base service plugs into
- `HikariCP` — JDBC connection pool

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-driver-mysql`, `dc3-driver-oracle`, `dc3-driver-postgresql`, `dc3-driver-sqlserver` — JDBC drivers that extend `AbstractJdbcDriverCustomService`

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
