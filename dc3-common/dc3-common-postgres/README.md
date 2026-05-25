# DC3 Common Postgres

## Overview

`dc3-common-postgres` is the shared PostgreSQL and MyBatis-Plus configuration module of the IoT DC3 platform. It
auto-configures datasource, paging plugin, and common MyBatis
utility support for all services that use PostgreSQL as their primary storage.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-postgres
- **Version**: 2026.5.22

## Key Components

| Component                     | Purpose                                                                                      |
|-------------------------------|----------------------------------------------------------------------------------------------|
| `MybatisPlusConfig`           | Configures `MybatisPlusInterceptor` with pagination plugin; sets default enum type handler   |
| `MybatisUtil`                 | Utility methods for building MyBatis-Plus `LambdaQueryWrapper` conditions from query objects |
| `ActivePostgresProfileConfig` | Activates the `postgres` profile unless `dc3.postgres.auto-profile=false` is set             |

## Configuration Properties

Uses `dynamic-datasource-spring-boot-starter` for multi-datasource support:

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:postgresql://${POSTGRES_HOST:dc3-postgres}:${POSTGRES_PORT:35432}/${POSTGRES_DB:dc3}?currentSchema=${POSTGRES_SCHEMA:dc3_manager}&...
          username: ${POSTGRES_USERNAME:dc3}
          password: ${POSTGRES_PASSWORD:dc3dc3dc3}
```

Set `dc3.postgres.auto-profile=false` to opt out of automatic `postgres` profile activation.
Applications can override the shared MyBatis-Plus pagination interceptor by declaring their own
`MybatisPlusInterceptor` bean.

## Schema Isolation

Each center service uses a separate Postgres schema via `currentSchema` in the JDBC URL:

| Service              | Schema        |
|----------------------|---------------|
| `dc3-center-auth`    | `dc3_auth`    |
| `dc3-center-manager` | `dc3_manager` |
| `dc3-center-data`    | `dc3_data`    |

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-manager`, `dc3-common-auth`, `dc3-common-data` — Include this as a dependency for Postgres access
- `dc3-common-dal` — Builds on top of this for shared DAL entities

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
