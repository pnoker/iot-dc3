# DC3 Center Auth

## Overview

`dc3-center-auth` is the Authorization Center of the IoT DC3 platform. It provides authentication and authorization
management for the entire platform including tenant management, local credential login, token validation, and permission
control.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-auth
- **Package**: `io.github.pnoker.center.auth`

## Service Ports

| Protocol  | Port   | Configuration variable |
|-----------|--------|------------------------|
| HTTP REST | `8300` | `DC3_AUTH_PORT`        |
| gRPC      | `9300` | `DC3_AUTH_GRPC_PORT`   |

## Key Responsibilities

- **Token Management**: Issue, validate, and revoke JWT tokens for authenticated users
- **Tenant Management**: Multi-tenant registration, lookup by tenant code
- **User Authentication**: Local credential validation with server-side password hashing
- **Dictionary Services**: Provide lookup dictionaries for auth-scoped data
- **gRPC Server**: Exposes tenant, user, credential, token, permission, resource-registry, and MCP-runtime APIs for
  facade-backed inter-service consumption

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
- `application-pre.yml` — pre-release datasource and runtime overrides
- `application-pro.yml` — production datasource and runtime overrides

## Running Locally

### 1. Start Infrastructure

```bash
make up-db
```

### 2. Build

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-auth -am package
```

### 3. Run

```bash
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-auth -am test
```

## Related Modules

- `dc3-api-auth` - gRPC API contracts for auth service
- `dc3-common-auth` - Business logic implementation
- `dc3-gateway` - Consumes token validation via gRPC
