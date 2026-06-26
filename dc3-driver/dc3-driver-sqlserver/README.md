# DC3 Driver SQL Server

## Overview

`dc3-driver-sqlserver` is the Microsoft SQL Server database driver of the IoT DC3 platform. It connects to a SQL
Server instance via JDBC, reads point values by executing configured SQL `SELECT` queries on a schedule, and
writes / executes SQL for write commands. It builds on the shared `AbstractJdbcDriverCustomService` from
`dc3-common-sql`, which constructs the JDBC URL
`jdbc:sqlserver://<host>:<port>;databaseName=<database>;encrypt=<encrypt>;trustServerCertificate=<...>;`
using the `com.microsoft.sqlserver.jdbc.SQLServerDriver`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-sqlserver
- **Version**: 2026.5.22
- **Driver Name**: SQL Server Driver

## Driver Attributes (Device-level)

| Attribute                | Code                   | Type   | Default   | Description                         |
|--------------------------|------------------------|--------|-----------|-------------------------------------|
| Host                     | host                   | STRING | localhost | SQL Server host                     |
| Port                     | port                   | INT    | 1433      | SQL Server port                     |
| Database                 | database               | STRING |           | SQL Server database name            |
| Username                 | username               | STRING | root      | SQL Server username                 |
| Password                 | password               | STRING |           | SQL Server password                 |
| Query Timeout            | queryTimeout           | INT    | 30        | SQL query timeout in seconds        |
| Encrypt                  | encrypt                | STRING | false     | SQL Server encrypt connection       |
| Trust Server Certificate | trustServerCertificate | STRING | true      | SQL Server trust server certificate |

## Point Attributes

| Attribute   | Code       | Type   | Description                                                                           |
|-------------|------------|--------|---------------------------------------------------------------------------------------|
| Read Query  | readQuery  | STRING | SQL SELECT query for reading point value                                              |
| Write Query | writeQuery | STRING | SQL UPDATE/INSERT using a single `?` placeholder for the value (bound as a parameter) |

## Command Attributes (write)

| Attribute     | Code         | Type   | Description                      |
|---------------|--------------|--------|----------------------------------|
| Execute Query | executeQuery | STRING | SQL query to execute for command |

## Prerequisites

A reachable Microsoft SQL Server instance. The connection URL, credentials, encryption flags, and the SQL queries
to run are all supplied through the driver and point attributes above — nothing is hardcoded.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-sqlserver/target/dc3-driver-sqlserver.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
- `dc3-common-sql` — `AbstractJdbcDriverCustomService` base class providing JDBC read/write logic

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
