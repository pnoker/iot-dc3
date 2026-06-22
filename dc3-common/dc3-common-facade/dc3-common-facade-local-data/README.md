# DC3 Common Facade Local (Data)

## Overview

`dc3-common-facade-local-data` provides the **in-process implementation** of the data-domain facade contracts from
`dc3-common-facade-api`. Each facade delegates directly to the data service beans on the local classpath instead of
issuing gRPC calls — used inside the `dc3-center-single` monolith.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-facade-local-data
- **Version**: 2026.5.22

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
mvn -s ../../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-facade-grpc` — gRPC alternative for distributed deployments
- `dc3-common-facade-local-auth` / `-manager` — sibling in-process facades

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
