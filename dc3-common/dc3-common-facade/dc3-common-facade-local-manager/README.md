# DC3 Common Facade Local (Manager)

## Overview

`dc3-common-facade-local-manager` provides the **in-process implementation** of the manager-domain facade contracts from
`dc3-common-facade-api`. Each facade delegates directly to the manager service beans on the local classpath instead of
issuing gRPC calls — used inside the `dc3-center-single` monolith.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-facade-local-manager

## Implementations

In-process manager facades: `DriverLocalFacade`, `DeviceLocalFacade`, `PointLocalFacade`, `ProfileLocalFacade`,
`CommandLocalFacade`, `EventLocalFacade`. MapStruct `Facade*Builder` classes convert between manager domain objects and
facade BOs. Beans are registered by `LocalFacadeManagerAutoConfiguration`.

## Activation

Active when the manager-domain in-process mode is selected (`dc3.facade.manager.mode=local`) and `dc3-common-manager` is on the
classpath.

## Dependencies

- `dc3-common-facade-api` — the contracts implemented here
- `dc3-common-manager` — manager service beans invoked directly

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-facade/dc3-common-facade-local-manager -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-facade/dc3-common-facade-local-manager -am test
```

## Related Modules

- `dc3-common-facade-grpc` — gRPC alternative for distributed deployments
- `dc3-common-facade-local-auth` / `-data` — sibling in-process facades
