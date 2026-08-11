# DC3 Common

`dc3-common` contains reusable backend libraries and the shared business implementations assembled by the deployable
center and driver applications.

## Domain modules

| Module | Responsibility |
|---|---|
| `dc3-common-auth` | authentication, authorization, identity, OAuth2, and auth gRPC servers |
| `dc3-common-manager` | driver/device/profile/point metadata and manager APIs |
| `dc3-common-data` | values, commands, events, status, and data APIs |
| `dc3-common-agentic` | AI models, sessions, tools, and assisted operations |
| `dc3-common-driver` | driver SDK, registration, scheduling, metadata, command, and value runtime |
| `dc3-common-gateway` | gateway routes, authentication filter, and ingress support |

## Contracts and models

| Module | Responsibility |
|---|---|
| `dc3-common-model` | shared BO/VO/DTO bases, builders, extension models, validation groups, and domain enums |
| `dc3-common-public` | response envelope, `BaseService`, shared entities/utilities, and tenant markers |
| `dc3-common-api` | shared gRPC conversion helpers |
| `dc3-common-facade` | cross-service facade contracts and implementations |
| `dc3-common-constant` | stable shared constants |
| `dc3-common-exception` | shared exception hierarchy |

## Infrastructure modules

| Module | Responsibility |
|---|---|
| `dc3-common-dal` | shared label/group persistence |
| `dc3-common-postgres` | datasource and MyBatis-Plus configuration |
| `dc3-common-repository` | point-value storage abstraction |
| `dc3-common-rabbitmq` | shared exchanges, connection configuration, and message conversion |
| `dc3-common-mqtt` | MQTT client configuration |
| `dc3-common-quartz` | scheduling infrastructure |
| `dc3-common-thread` | managed executors |
| `dc3-common-web` | WebFlux, springdoc, security, and controller support |
| `dc3-common-log` | logging defaults |
| `dc3-common-sql` | SQL utilities |
| `dc3-common-resource-registrar` | API/resource annotation discovery and synchronization |
| `dc3-common-test` | shared tests, harnesses, and Testcontainers |

## Architecture rules

- Preserve `Controller -> Service -> Manager -> Mapper` layering.
- Keep tenant scope in queries, cache keys, gRPC calls, and cross-service lookups.
- Use BOs in persistent business services and builders for VO/BO/DO conversion.
- Use facade interfaces for cross-service business calls.
- Add dependencies to the narrowest module that owns the required capability.

## Verification

```bash
mvn -s .mvn/settings.xml -q -f dc3-common/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-common/pom.xml test
```

Use the child README for module-specific configuration and tests.
