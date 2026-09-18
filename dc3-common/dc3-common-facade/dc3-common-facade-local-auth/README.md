# DC3 Common Facade Local (Auth)

## Overview

`dc3-common-facade-local-auth` provides the **in-process implementation** of the auth-domain facade contracts from
`dc3-common-facade-api`. Instead of issuing gRPC calls, each facade delegates directly to the auth service beans on the
local classpath — used inside the `dc3-center-single` monolith where auth, manager, and data run in one process.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-facade-local-auth

## Implementations

In-process auth facades: `TokenLocalFacade`, `UserLocalFacade`, `TenantLocalFacade`, `PermissionLocalFacade`,
`ResourceRegistryLocalFacade`, `McpRuntimeLocalFacade`, `LocalCredentialLocalFacade`. MapStruct `Facade*Builder`
classes convert between auth domain objects and facade BOs. Beans are registered by `LocalFacadeAuthAutoConfiguration`.

## Activation

Active when the auth-domain in-process mode is selected (`dc3.facade.auth.mode=local`) and `dc3-common-auth` is on the classpath.

## Dependencies

- `dc3-common-facade-api` — the contracts implemented here
- `dc3-common-auth` — auth service beans invoked directly

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-facade/dc3-common-facade-local-auth -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-facade/dc3-common-facade-local-auth -am test
```

## Related Modules

- `dc3-common-facade-grpc` — gRPC alternative for distributed deployments
- `dc3-common-facade-local-data` / `-manager` — sibling in-process facades
