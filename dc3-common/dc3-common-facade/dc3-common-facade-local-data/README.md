# DC3 Common Facade Local (Data)

## Overview

`dc3-common-facade-local-data` provides the **in-process implementation** of the data-domain facade contracts from
`dc3-common-facade-api`. Each facade delegates directly to the data service beans on the local classpath instead of
issuing gRPC calls — used inside the `dc3-center-single` monolith.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-facade-local-data

## Implementations

In-process data facades: `PointValueLocalFacade`, `PointCommandLocalFacade`, `StatusHealthLocalFacade`. MapStruct
`Facade*Builder` classes convert between data domain objects and facade BOs. Beans are registered by
`LocalFacadeDataAutoConfiguration`.

## Activation

Active when the in-process facade mode is selected (`dc3.facade.mode=local`) and `dc3-common-data` is on the classpath.

## Dependencies

- `dc3-common-facade-api` — the contracts implemented here
- `dc3-common-data` — data service beans invoked directly

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-facade/dc3-common-facade-local-data -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-facade/dc3-common-facade-local-data -am test
```

## Related Modules

- `dc3-common-facade-grpc` — gRPC alternative for distributed deployments
- `dc3-common-facade-local-auth` / `-manager` — sibling in-process facades
