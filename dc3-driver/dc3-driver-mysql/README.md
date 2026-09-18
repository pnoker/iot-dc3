# DC3 Driver MySQL

## Overview

`dc3-driver-mysql` is the MySQL database driver of the IoT DC3 platform. It connects to a MySQL database via JDBC
(`mysql-connector-j`), reading point values through configured `SELECT` queries and writing through configured
`UPDATE`/`INSERT` queries. JDBC connection handling is provided by the shared `dc3-common-sql` abstract base service.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-mysql
- **Driver Name**: MySQL Driver

## Driver Attributes (Device-level)

| Attribute     | Description                  |
|---------------|------------------------------|
| Host          | MySQL host                   |
| Port          | MySQL port                   |
| Database      | MySQL database name          |
| Username      | MySQL username               |
| Password      | MySQL password               |
| Query Timeout | SQL query timeout in seconds |

## Point Attributes

| Attribute   | Description                                     |
|-------------|-------------------------------------------------|
| Read Query  | SQL SELECT query for reading point value        |
| Write Query | SQL UPDATE/INSERT query for writing point value |

## Command Attributes (write)

| Attribute     | Description                      |
|---------------|----------------------------------|
| Execute Query | SQL query to execute for command |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable MySQL database addressable by the configured host, port, and credentials.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-mysql -am package
java -jar dc3-driver/dc3-driver-mysql/target/dc3-driver-mysql.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-mysql -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
- `dc3-common-sql` — Abstract JDBC driver service (connection pooling, read/write query execution)
