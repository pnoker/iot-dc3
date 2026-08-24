# DC3 Driver Redis

## Overview

`dc3-driver-redis` treats Redis as a key-value data source. It reads and writes string keys (GET/SET) and hash fields
(HGET/HSET) through the Spring Boot auto-configured `StringRedisTemplate`. A device-level key prefix isolates multiple
logical devices within one Redis instance.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-redis
- **Driver Name**: Redis Driver

## Driver Attributes (Device-level)

| Attribute  | Code      | Type   | Default | Description                          |
|------------|-----------|--------|---------|--------------------------------------|
| Key Prefix | keyPrefix | STRING |         | Prefix prepended to every point key  |

## Point Attributes

| Attribute | Code     | Type   | Default | Description                               |
|-----------|----------|--------|---------|-------------------------------------------|
| Key       | key      | STRING |         | Redis key (relative to the key prefix)    |
| Data Type | dataType | STRING | STRING  | Redis data type: STRING or HASH           |
| Field     | field    | STRING |         | Hash field, required when dataType=HASH   |

## Command Attributes (write)

| Attribute | Code     | Type   | Default | Description                                    |
|-----------|----------|--------|---------|------------------------------------------------|
| Data Type | dataType | STRING | STRING  | Redis data type of the written key: STRING or HASH |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable Redis instance. Point the `spring.data.redis.*` properties (or `REDIS_HOST`, `REDIS_PORT`,
`REDIS_PASSWORD`, `REDIS_DATABASE`) at the target server.

## Connection

The Redis connection is configured through Spring Boot `spring.data.redis.*` properties in `application.yml`, driven
by the `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, and `REDIS_DATABASE` environment variables.

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-redis -am package
java -jar dc3-driver/dc3-driver-redis/target/dc3-driver-redis.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-redis -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
