# AGENTS.md

## Existing agent conventions discovered

- Searched `**/{.github/copilot-instructions.md,AGENT.md,AGENTS.md,CLAUDE.md,.cursorrules,.windsurfrules,.clinerules,.cursor/rules/**,.windsurf/rules/**,.clinerules/**,README.md}`.
- Found only README files: `README.md`, `dc3-api/dc3-api-auth/README.md`, `dc3-api/dc3-api-data/README.md`, `dc3-api/dc3-api-driver/README.md`.
- No pre-existing repo-local AI instruction files were found.

## Big picture (what lives where)

- This is a Maven multi-module Spring Cloud IoT platform (`pom.xml` root modules: `dc3-api`, `dc3-common`, `dc3-center`, `dc3-driver`, `dc3-gateway`).
- `dc3-api/*`: protobuf/gRPC API contracts shared across services (for auth, data, driver/manager flows).
- `dc3-center/*`: deployable center services (`auth`, `manager`, `data`, plus `single`); each mostly wires one `dc3-common-*` business module.
- `dc3-common/*`: reusable business code (controllers/services/dal, grpc servers/clients, messaging, constants, model mapping).
- `dc3-driver/*`: protocol drivers (virtual, modbus, mqtt, opc, plcs7) using gRPC + RabbitMQ to integrate with manager/data.
- `dc3-gateway`: Spring Cloud Gateway entrypoint routing `/api/v3/*` to center services.

## Service boundaries and runtime data flow

- HTTP enters via gateway routes (`dc3-gateway/src/main/resources/application-pre.yml`) with `StripPrefix=2` and optional `Authentic` filter.
- Manager/Data/Auth expose REST + gRPC; example manager ports are HTTP `8400`, gRPC `9400` (`dc3-center/dc3-center-manager/src/main/resources/application.yml`).
- Drivers register and fetch config through manager gRPC (`@GrpcService` server in `dc3-common-manager/.../grpc/server/driver/DriverDriverServer.java`; client stubs in drivers/data
  via `@GrpcClient(ManagerConstant.SERVICE_NAME)`).
- Commands and metadata changes are asynchronous over RabbitMQ topic exchanges/routing keys (`dc3-common-constant/.../RabbitConstant.java`,
  `dc3-common-rabbitmq/.../ExchangeConfig.java`).
- Example command path: Data service resolves driver by gRPC then publishes `dc3.e.command` message keyed by driver service (
  `dc3-common-data/.../PointValueCommandServiceImpl.java`).

## Developer workflows that actually work here

- Start infra dependencies first: `podman compose -f dc3/docker-compose-db.yml up -d` (Postgres/Redis/RabbitMQ).
- Optional MQTT broker for drivers: `podman compose -f dc3/docker-compose-optional.yml up -d` (EMQX on `31883`).
- Local env variables come from `dc3/env/dev.env.sh`; `source` the file before running services from the shell or IDE.
- Build all modules from repo root: `mvn -s .mvn/settings.xml clean package`.
- Recommended manual startup order from `README.md`: gateway -> auth -> data -> manager -> driver(s).
- Full containerized dev stack can be started with `make dev` (`dc3/` compose-based).

## Code patterns to follow in changes

- Keep URL prefixes/constants in `*Constant` classes (example: `ManagerConstant.DRIVER_URL_PREFIX`), then reference in controllers.
- Web controllers are reactive (`Mono<R<T>>`) and typically implement `BaseController` to pull tenant/user headers from context.
- Response envelope is always `R<T>` (`dc3-common-public/.../R.java`) for REST and `GrpcR` in gRPC handlers.
- Mapping style is explicit BO/VO/DTO + Builder classes (example: `DriverBuilder`, `GrpcDriverBuilder`) rather than direct entity exposure.
- Validation relies on grouped marker interfaces (`Add`, `Update`, etc.) used in `@Validated(...)` controller methods.

## Integration assumptions to preserve

- Service discovery/config is Nacos-based in pre/pro profiles (`spring.cloud.nacos.*` in gateway and center profile YAMLs).
- Storage/messaging defaults are Postgres + Redis + RabbitMQ; do not hardcode localhost in code paths, keep `${ENV:default}` style in YAML.
- Driver/service names are routing-critical (used in RabbitMQ routing suffixes and gRPC target names); preserve existing naming constants.
- There are currently no Java test sources under `**/src/test/**/*.java`; when refactoring, rely on compile + targeted runtime smoke checks.
