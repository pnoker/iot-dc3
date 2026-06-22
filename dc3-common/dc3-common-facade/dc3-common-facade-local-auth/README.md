# DC3 Common Facade Local (Auth)

## Overview

`dc3-common-facade-local-auth` provides the **in-process implementation** of the auth-domain facade contracts from
`dc3-common-facade-api`. Instead of issuing gRPC calls, each facade delegates directly to the auth service beans on
the local classpath — used inside the `dc3-center-single` monolith where auth, manager, and data run in one process.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-facade-local-auth
- **Version**: 2026.5.22

## Implementations

In-process auth facades: `TokenLocalFacade`, `UserLocalFacade`, `TenantLocalFacade`, `PermissionLocalFacade`,
`ResourceRegistryLocalFacade`, `McpRuntimeLocalFacade`, `LocalCredentialLocalFacade`. MapStruct `Facade*Builder`
classes convert between auth domain objects and facade BOs. Beans are registered by `LocalFacadeAuthAutoConfiguration`.

## Activation

Active when the in-process facade mode is selected (`dc3.facade.mode=local`) and `dc3-common-auth` is on the classpath.

## Dependencies

- `dc3-common-facade-api` — the contracts implemented here
- `dc3-common-auth` — auth service beans invoked directly

## Build Instructions

```bash
mvn -s ../../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-facade-grpc` — gRPC alternative for distributed deployments
- `dc3-common-facade-local-data` / `-manager` — sibling in-process facades

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
