# DC3 Driver MySQL

## Overview

`dc3-driver-mysql` is the MySQL database driver of the IoT DC3 platform. It connects to a MySQL database via JDBC
(`mysql-connector-j`), reading point values through configured `SELECT` queries and writing through configured
`UPDATE`/`INSERT` queries. JDBC connection handling is provided by the shared `dc3-common-sql` abstract base service.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-mysql
- **Version**: 2026.5.22
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

## Prerequisites

A reachable MySQL database addressable by the configured host, port, and credentials.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-mysql/target/dc3-driver-mysql.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
- `dc3-common-sql` — Abstract JDBC driver service (connection pooling, read/write query execution)

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
