# DC3 Common Facade gRPC

## Overview

`dc3-common-facade-grpc` is the **gRPC-backed implementation** of the facade contracts defined in
`dc3-common-facade-api`. Each facade translates a business call into a gRPC request against the corresponding center
service (auth / manager / data), so business code makes cross-process calls without touching gRPC stubs directly.
This is the default implementation for the distributed (multi-service) deployment.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-facade-grpc
- **Version**: 2026.5.22

## Implementations

gRPC facades include `TokenGrpcFacade`, `UserGrpcFacade`, `TenantGrpcFacade`, `PermissionGrpcFacade`,
`ResourceRegistryGrpcFacade`, `McpRuntimeGrpcFacade`, `LocalCredentialGrpcFacade`, `DriverGrpcFacade`,
`DeviceGrpcFacade`, `CommandGrpcFacade`, `PointValueGrpcFacade`, `PointCommandGrpcFacade`, `StatusHealthGrpcFacade`,
etc. `GrpcFacadeSupport` holds shared response-unwrapping / error-mapping helpers. Beans are registered by
`GrpcFacadeAutoConfiguration`.

## Activation

Active when `dc3.facade.mode=grpc` (`DC3_FACADE_MODE`, the default). gRPC channels are configured per service
(e.g. the auth channel points at `dc3-center-auth:9300`).

## Dependencies

- `dc3-common-facade-api` — the contracts implemented here
- `dc3-api-auth`, `dc3-api-data`, `dc3-api-manager` — generated gRPC stubs
- `spring-grpc-spring-boot-starter` — gRPC client support

## Build Instructions

```bash
mvn -s ../../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-facade-api` — facade contracts
- `dc3-common-facade-local-*` — in-process alternative used by `dc3-center-single`

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
