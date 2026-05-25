# DC3 Center Auth

## Overview

`dc3-center-auth` is the Authorization Center of the IoT DC3 platform. It provides authentication and authorization
management for the entire platform including tenant management,
user login, token validation, and permission control.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-auth
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.center.auth`

## Service Ports

| Protocol  | Port                                                 |
|-----------|------------------------------------------------------|
| HTTP REST | `8300` (default, overridable via `SERVER_PORT`)      |
| gRPC      | `9300` (default, overridable via `GRPC_SERVER_PORT`) |

## Key Responsibilities

- **Token Management**: Issue, validate, and revoke JWT tokens for authenticated users
- **Tenant Management**: Multi-tenant registration, lookup by tenant code
- **User Authentication**: User login validation with salt-based encrypted password
- **Dictionary Services**: Provide lookup dictionaries for auth-scoped data
- **gRPC Server**: Exposes `TenantApi`, `UserApi`, `UserLoginApi`, `TokenApi` for inter-service consumption (e.g.,
  Gateway)

## REST Endpoints (via Gateway)

Accessible through the gateway at `/api/v3/auth/**`:

| Path                         | Auth Required |
|------------------------------|---------------|
| `/api/v3/auth/token/**`      | No            |
| `/api/v3/auth/user/**`       | Yes           |
| `/api/v3/auth/tenant/**`     | Yes           |
| `/api/v3/auth/dictionary/**` | Yes           |

## Dependencies

This service wires `dc3-common-auth` which contains all business logic controllers, services, gRPC servers, and mappers.

```xml

<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-auth</artifactId>
</dependency>
```

## Configuration

- `application.yml` — base port and profile config
- `application-dev.yml` — dev env: Postgres connection via `${ENV:default}` vars
- `application-pre.yml` — pre-release: Nacos-based service discovery
- `application-pro.yml` — production: Nacos-based service discovery

## Running Locally

### 1. Start Infrastructure

```bash
podman compose -f dc3/docker-compose-db.yml up -d
```

### 2. Build

```bash
mvn -s .mvn/settings.xml clean package
```

### 3. Run

```bash
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar
```

## Related Modules

- `dc3-api-auth` - gRPC API contracts for auth service
- `dc3-common-auth` - Business logic implementation
- `dc3-gateway` - Consumes token validation via gRPC

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
