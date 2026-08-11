# DC3 Driver PostgreSQL

## Overview

`dc3-driver-postgresql` is the PostgreSQL database driver of the IoT DC3 platform. It connects to a PostgreSQL database
via JDBC, reads point values by executing configured SQL `SELECT` queries on a schedule, and writes / executes SQL for
write commands. It builds on the shared `AbstractJdbcDriverCustomService` from `dc3-common-sql`, which constructs the
JDBC URL `jdbc:postgresql://<host>:<port>/<database>` using the `org.postgresql.Driver`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-postgresql
- **Driver Name**: PostgreSQL Driver

## Driver Attributes (Device-level)

| Attribute     | Code         | Type   | Default   | Description                  |
|---------------|--------------|--------|-----------|------------------------------|
| Host          | host         | STRING | localhost | PostgreSQL host              |
| Port          | port         | INT    | 5432      | PostgreSQL port              |
| Database      | database     | STRING |           | PostgreSQL database name     |
| Username      | username     | STRING | root      | PostgreSQL username          |
| Password      | password     | STRING |           | PostgreSQL password          |
| Query Timeout | queryTimeout | INT    | 30        | SQL query timeout in seconds |

## Point Attributes

| Attribute   | Code       | Type   | Description                                                                           |
|-------------|------------|--------|---------------------------------------------------------------------------------------|
| Read Query  | readQuery  | STRING | SQL SELECT query for reading point value                                              |
| Write Query | writeQuery | STRING | SQL UPDATE/INSERT using a single `?` placeholder for the value (bound as a parameter) |

## Command Attributes (write)

| Attribute     | Code         | Type   | Description                      |
|---------------|--------------|--------|----------------------------------|
| Execute Query | executeQuery | STRING | SQL query to execute for command |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and
local buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable PostgreSQL database. The connection URL, credentials, and the SQL queries to run are all supplied through
the driver and point attributes above — nothing is hardcoded.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-postgresql -am package
java -jar dc3-driver/dc3-driver-postgresql/target/dc3-driver-postgresql.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-postgresql -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
- `dc3-common-sql` — `AbstractJdbcDriverCustomService` base class providing JDBC read/write logic
