# DC3 Common SQL

## Overview

`dc3-common-sql` is the shared base for the database (JDBC) drivers of the IoT DC3 platform. It provides
`AbstractJdbcDriverCustomService`, a common driver-service base that handles JDBC connection management (via HikariCP)
and query execution, so each database driver only supplies its dialect/connection specifics.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-sql

## Key Components

| Component                         | Purpose                                                             |
|-----------------------------------|---------------------------------------------------------------------|
| `AbstractJdbcDriverCustomService` | Base driver service for JDBC sources — pooled connections + queries |

## Dependencies

- `dc3-common-driver` — Driver SDK that the base service plugs into
- `HikariCP` — JDBC connection pool

## Connection Management

One HikariCP pool is cached per device (`ConcurrentHashMap<Long, HikariDataSource>`), created lazily on first access and
configured with a maximum of 5 connections and a minimum idle of 1. Pools are evicted when:

- the device is deleted or updated (device metadata event),
- the driver configuration is updated or the driver is deleted (driver metadata event — every pool is closed),
- a read/write fails at the connection level, so the next access rebuilds the pool from current configuration.

## Driver Attributes

| Attribute    | Default          | Semantics                                                                                                     |
|--------------|------------------|---------------------------------------------------------------------------------------------------------------|
| host         | `localhost`      | Database host                                                                                                 |
| port         | dialect-specific | Database port                                                                                                 |
| database     | —                | Database name (MySQL, PostgreSQL, SQL Server; Oracle uses SID/ServiceName instead)                            |
| username     | `root`           | Login name                                                                                                    |
| password     | —                | Login password                                                                                                |
| queryTimeout | `30`             | SQL statement timeout in **seconds**, applied via `PreparedStatement.setQueryTimeout` to every read and write |

## Point Attributes

| Attribute    | Semantics                                                                                                                                                 |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `readQuery`  | SQL SELECT executed on read; must not contain `?` placeholders because reads bind no parameters                                                           |
| `writeQuery` | SQL UPDATE/INSERT/DELETE executed on write; must contain exactly one `?` placeholder — the point value is bound via `setString`, preventing SQL injection |

`validatePoint` derives which queries are required from the point's read/write flag: read-only points require
`readQuery`, write-only points require `writeQuery`, and read-write points require both. A point with an unknown flag is
treated as read-only.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-sql -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-sql -am test
```

## Related Modules

- `dc3-driver-mysql`, `dc3-driver-oracle`, `dc3-driver-postgresql`, `dc3-driver-sqlserver` — JDBC drivers that extend
  `AbstractJdbcDriverCustomService`
