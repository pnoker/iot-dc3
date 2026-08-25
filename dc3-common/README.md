# DC3 Common

`dc3-common` contains reusable backend libraries and the shared business implementations assembled by the deployable
center and driver applications.

## Domain modules

| Module               | Responsibility                                                             |
|----------------------|----------------------------------------------------------------------------|
| `dc3-common-auth`    | authentication, authorization, identity, OAuth2, and auth gRPC servers     |
| `dc3-common-manager` | driver/device/profile/point metadata and manager APIs                      |
| `dc3-common-data`    | values, commands, events, status, and data APIs                            |
| `dc3-common-agentic` | AI models, sessions, tools, and assisted operations                        |
| `dc3-common-driver`  | driver SDK, registration, scheduling, metadata, command, and value runtime |
| `dc3-common-gateway` | gateway routes, authentication filter, and ingress support                 |

## Contracts and models

| Module                 | Responsibility                                                                              |
|------------------------|---------------------------------------------------------------------------------------------|
| `dc3-common-model`     | shared BO/VO/DTO bases, builders, extension models, validation groups, and transport models |
| `dc3-common-public`    | response envelope, `BaseService`, shared entities/utilities, and tenant markers             |
| `dc3-common-api`       | shared gRPC conversion helpers                                                              |
| `dc3-common-facade`    | cross-service facade contracts and implementations                                          |
| `dc3-common-constant`  | stable platform-wide constants and shared domain/wire/persistence enums                     |
| `dc3-common-exception` | shared exception hierarchy                                                                  |

## Infrastructure modules

| Module                          | Responsibility                                        |
|---------------------------------|-------------------------------------------------------|
| `dc3-common-dal`                | shared label/group persistence                        |
| `dc3-common-mqtt`               | MQTT client configuration                             |
| `dc3-common-quartz`             | scheduling infrastructure                             |
| `dc3-common-thread`             | managed executors                                     |
| `dc3-common-web`                | WebFlux, springdoc, security, and controller support  |
| `dc3-common-log`                | logging defaults                                      |
| `dc3-common-sql`                | SQL utilities                                         |
| `dc3-common-resource-registrar` | API/resource annotation discovery and synchronization |
| `dc3-common-test`               | shared tests, harnesses, and Testcontainers           |

Datasource, messaging, and time-series storage were split into dedicated top-level families; business modules depend on
them directly:

- `dc3-db` — relational dialect adapters (PostgreSQL/MySQL/MariaDB) behind `dc3.db.type`
- `dc3-mq` — broker-neutral messaging port with per-broker adapters (RabbitMQ, Kafka, RocketMQ, Pulsar, ActiveMQ, MQTT)
- `dc3-tsdb` — store-neutral time-series port with per-store adapters (TimescaleDB, TDengine, InfluxDB, IoTDB)

## Architecture rules

- Preserve `Controller -> Service -> Manager -> Mapper` layering.
- Keep tenant scope in queries, cache keys, gRPC calls, and cross-service lookups.
- Use BOs in persistent business services and builders for VO/BO/DO conversion.
- Use facade interfaces for cross-service business calls.
- Add dependencies to the narrowest module that owns the required capability.
- Treat Java visibility and module ownership separately: framework-neutral shared code belongs in `dc3-common-public`,
  while capability-specific public APIs remain with their owning module.
- Keep platform-wide constants and top-level shared enums in `dc3-common-constant`; keep module-local constants and
  nested implementation/configuration enums beside their owner. Top-level `*Constant` names and top-level public enums
  are reserved for `dc3-common-constant`; use concern-specific local names such as `*Limits` or `*Defaults`.

## Verification

```bash
mvn -s .mvn/settings.xml -q -f dc3-common/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-common/pom.xml test
```

Use the child README for module-specific configuration and tests.
