# DC3 Common Gateway

## Overview

`dc3-common-gateway` is the shared gateway module of the IoT DC3 platform. It provides the `Authentic` gateway filter
factory and supporting services that validate tokens with the Auth Center before forwarding requests in the
`dc3-gateway`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-gateway

## Key Components

| Component                       | Purpose                                                                                   |
|---------------------------------|-------------------------------------------------------------------------------------------|
| `AuthenticGatewayFilterFactory` | Spring Cloud Gateway `GatewayFilterFactory` that intercepts requests and validates tokens |
| `AuthenticGatewayFilter`        | Applies token validation logic; injects principal headers downstream                      |
| `FilterServiceImpl`             | Calls Auth Center via gRPC to validate the Bearer token                                   |
| `GatewayInitRunner`             | Startup runner for gateway-specific initialization                                        |
| `McpGatewayController`          | OAuth2 authorization-server and MCP discovery endpoints exposed at the gateway            |
| `McpGatewayProperties`          | Binds MCP gateway settings from YAML                                                      |

## Filter Flow

```
Incoming HTTP request with Authorization: Bearer {token}
  → AuthenticGatewayFilter
    → FilterServiceImpl
      → TokenFacade.checkValid()
        → distributed mode: gRPC TokenApi.CheckValid
      ← token valid: inject signed X-Auth-Principal header
      ← token invalid: return 401 Unauthorized
  → Forward to backend service
```

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-gateway -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-gateway -am test
```

## Related Modules

- `dc3-gateway` — Bootstraps this module
- `dc3-api-auth` — gRPC contract for token validation
- `dc3-center-auth` — Token validation backend
