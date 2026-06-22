# DC3 Gateway

## Overview

`dc3-gateway` is the API Gateway of the IoT DC3 platform, built on Spring Cloud Gateway (WebFlux). It serves as the
single ingress point for all external HTTP traffic, providing
rate limiting, authentication verification, service routing, and reverse proxying.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-gateway
- **Version**: 2026.5.22
- **Package**: `io.github.pnoker.gateway`

## Service Ports

| Protocol | Port   |
|----------|--------|
| HTTP     | `8000` |

## Key Responsibilities

- **Request Routing**: Routes `/api/v3/{service}/**` requests to the corresponding center service via `StripPrefix=2`
- **Authentication Filter**: `Authentic` filter validates Bearer tokens with the Auth Center before forwarding protected
  routes
- **Service Addressing**: Routes to backend centers via static addresses, overridable through `GATEWAY_ROUTE_*` / `CENTER_*_HOST` environment variables (no service registry)
- **gRPC Client**: Connects to `dc3-center-auth` (port `9300`) for token validation
- **OAuth2 / MCP Ingress**: Exposes the Auth Center's OAuth2 authorization-server and MCP discovery endpoints

## Routing Rules

Routes are matched in definition order (first match wins); the public token and OAuth
metadata routes are deliberately defined before the `/api/v3/auth/**` wildcard.

| Path Pattern                                                                                                    | Backend Service      | Auth Required |
|-----------------------------------------------------------------------------------------------------------------|----------------------|---------------|
| `/.well-known/oauth-authorization-server`, `/oauth2/jwks`, `/oauth2/token`, `/oauth2/revoke`, `/oauth2/register` | `dc3-center-auth`    | No            |
| `/oauth2/authorize`                                                                                             | `dc3-center-auth`    | Yes           |
| `/api/v3/auth/token/**`                                                                                         | `dc3-center-auth`    | No            |
| `/api/v3/auth/**`                                                                                               | `dc3-center-auth`    | Yes           |
| `/api/v3/manager/**`                                                                                            | `dc3-center-manager` | Yes           |
| `/api/v3/data/**`                                                                                               | `dc3-center-data`    | Yes           |
| `/api/v3/agentic/**`                                                                                            | `dc3-center-agentic` | Yes           |

The `/oauth2/**` and `/.well-known/**` routes expose the Auth Center's OAuth2
authorization-server and MCP discovery endpoints for MCP clients.

## Dependencies

Business logic is shared via `dc3-common-gateway`:

```xml
<dependency>
    <groupId>io.github.pnoker</groupId>
    <artifactId>dc3-common-gateway</artifactId>
</dependency>
```

## Configuration

- `application.yml` — base port, active profile, facade mode, SpringDoc aggregation
- `application-dev.yml` — dev env: profile group and debug logging
- `application-pre.yml` — pre-release overrides
- `application-pro.yml` — production: disables SpringDoc / Swagger UI

Route definitions and the auth gRPC channel are shared in `dc3-common-gateway`'s
`application-gateway.yml` (overridable via `GATEWAY_ROUTE_*` / `CENTER_*_HOST` env vars).

## Running Locally

### 1. Start Infrastructure

```bash
podman compose -f dc3/docker-compose-db.yml up -d
```

### 2. Build

```bash
mvn -s .mvn/settings.xml clean package
```

### 3. Run (start first before any center service)

```bash
java -jar dc3-gateway/target/dc3-gateway.jar
```

## Related Modules

- `dc3-common-gateway` — `Authentic` filter implementation and gateway utilities
- `dc3-api-auth` — gRPC API contracts for token validation
- `dc3-center-auth` — Token validation backend

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

