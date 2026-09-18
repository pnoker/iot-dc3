# DC3 Common Facade API

## Overview

`dc3-common-facade-api` defines the **facade contracts** of the IoT DC3 platform — the interfaces that business code
uses for cross-service calls instead of binding directly to a transport. Two interchangeable implementations satisfy
these contracts at runtime:

- **gRPC** (`dc3-common-facade-grpc`) — cross-process calls between the standalone center services.
- **In-process** (`dc3-common-facade-local-auth` / `-data` / `-manager`) — direct local calls inside the
  `dc3-center-single` monolith.

The implementation is selected independently per domain by `dc3.facade.auth.mode`, `dc3.facade.manager.mode`, and
`dc3.facade.data.mode`. A center sets its own domain to `disabled`, because facades are cross-service boundaries;
the single-process center uses `local`, and remote consumers use `grpc`. Controllers and
services never depend on transport details.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-facade-api

## Facade Contracts

| Domain  | Facade interfaces                                                                                                                      |
|---------|----------------------------------------------------------------------------------------------------------------------------------------|
| Auth    | `TokenFacade`, `ReactiveTokenFacade`, `UserFacade`, `ReactiveUserFacade`, `TenantFacade`, `ReactiveTenantFacade`, `PermissionFacade`, `ResourceRegistryFacade`, `McpRuntimeFacade`, `LocalCredentialFacade`, `ReactiveLocalCredentialFacade` |
| Manager | `DriverFacade`, `DeviceFacade`, `PointFacade`, `ProfileFacade`, `CommandFacade`, `EventFacade`                                         |
| Data    | `PointValueFacade`, `PointCommandFacade`, `StatusHealthFacade`                                                                         |

Facades exchange business objects (BO); request/response shaping is the caller's responsibility. New manager read
paths expose Reactor types directly: `Mono<T>` for a single record, `Flux<T>` for a bounded collection, and
`Mono<OffsetPage<T>>` for offset pagination. `FacadePointOffsetQuery` is the canonical point-list request; it carries
an explicit tenant, `offset`, `limit` (1..200), filters, and a whitelisted sort specification. New gRPC/local point
facade calls use these methods and never block a WebFlux or Agentic event loop.

## Dependencies

- `dc3-common-constant`, `dc3-common-model` — shared constants and domain models

## Usage

Business modules depend on this API module and inject the facade interfaces. The active implementation
(`dc3-common-facade-grpc` or one of the `dc3-common-facade-local-*` modules) is added per deployment and wired by its
auto-configuration.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-facade/dc3-common-facade-api -am package
```

## Testing

This module currently has no module-specific automated tests. Verify generated or production sources by compiling the
affected reactor from the repository root:

```bash
mvn -s .mvn/settings.xml -q -pl dc3-common/dc3-common-facade/dc3-common-facade-api -am -DskipTests compile
```

## Related Modules

- `dc3-common-facade-grpc` — gRPC-backed implementation
- `dc3-common-facade-local-auth` / `-data` / `-manager` — in-process implementations
