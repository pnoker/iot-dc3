# DC3 Common Resource

## Overview

`dc3-common-resource` discovers a service's HTTP API endpoints at startup and registers them as authorization
resources with the Auth Center. This keeps the RBAC resource catalog in sync with the controllers that are actually
deployed, so permissions can be granted against real endpoints.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-resource

## Key Components

| Component                            | Purpose                                                           |
|--------------------------------------|-------------------------------------------------------------------|
| `ApiEndpointScanner`                 | Scans annotated controllers and collects their API endpoints      |
| `ControllerAnnotationGate`           | Decides which controllers/endpoints are eligible for registration |
| `ApiAnnotationValidator`             | Validates the API annotations on scanned endpoints                |
| `ResourceRegistrar`                  | Submits the collected endpoints to the Auth Center on startup     |
| `ResourceRegistrarProperties`        | Binds registrar configuration from YAML                           |
| `ResourceRegistrarAutoConfiguration` | Wires the registrar into any service that includes this module    |

Registration is performed through `ResourceRegistryFacade` (from `dc3-common-facade-api`), so it works over either the
gRPC or in-process facade transport.

## Dependencies

- `dc3-common-web` — controller/annotation infrastructure
- `dc3-common-facade-api` — `ResourceRegistryFacade` used to submit resources

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-resource -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-resource -am test
```

## Related Modules

- `dc3-center-*` — include this module to register their API resources
- `dc3-common-auth` — owns the resource registry on the receiving side
