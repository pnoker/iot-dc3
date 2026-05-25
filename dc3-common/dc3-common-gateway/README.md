# DC3 Common Gateway

## Overview

`dc3-common-gateway` is the shared gateway module of the IoT DC3 platform. It provides the `Authentic` gateway filter
factory and supporting services that validate tokens with the
Auth Center before forwarding requests in the `dc3-gateway`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-gateway
- **Version**: 2026.5.22

## Key Components

| Component                       | Purpose                                                                                   |
|---------------------------------|-------------------------------------------------------------------------------------------|
| `AuthenticGatewayFilterFactory` | Spring Cloud Gateway `GatewayFilterFactory` that intercepts requests and validates tokens |
| `AuthenticGatewayFilter`        | Applies token validation logic; injects tenant/user headers downstream                    |
| `FilterServiceImpl`             | Calls Auth Center via gRPC to validate the Bearer token                                   |
| `GatewayInitRunner`             | Startup runner for gateway-specific initialization                                        |

## Filter Flow

```
Incoming HTTP request with Authorization: Bearer {token}
  → AuthenticGatewayFilter
    → gRPC: dc3-center-auth / TokenApi.checkTokenValid()
      ← token valid: inject X-Auth-Tenant-Id, X-Auth-User-Id headers
      ← token invalid: return 401 Unauthorized
  → Forward to backend service
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-gateway` — Bootstraps this module
- `dc3-api-auth` — gRPC contract for token validation
- `dc3-center-auth` — Token validation backend

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

