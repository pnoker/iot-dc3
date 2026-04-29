# CLAUDE.md

This file provides guidance to AI coding assistants when working with code in this repository.

## Project Overview

IoT DC3 is a distributed IoT platform built on Spring Cloud for industrial device connectivity, data collection, and management. It uses a microservices architecture with gRPC +
RabbitMQ for inter-service communication and supports multiple industrial protocols (Modbus TCP, OPC DA/UA, MQTT, Siemens S7, virtual listening).

**Architecture Layers:**

1. **Driver Layer** — Protocol drivers that connect to physical devices (southbound I/O)
2. **Data Layer** — Time-series point value collection and storage
3. **Management Layer** — Core coordination hub (device/driver/point metadata, tenant, auth)
4. **Gateway Layer** — Spring Cloud Gateway entrypoint routing `/api/v3/*` to center services

**Technology Stack:**

- Java 21, Spring Boot 4.0.6, Spring Framework 7.0.7 (project version `2026.4.29`, parent `dc3-parent:2026.4.29`)
- PostgreSQL (primary DB), RabbitMQ (messaging), EMQX/MQTT (IoT protocol); in-process `LocalCacheService` replaces Redis
- gRPC/Protobuf for inter-service APIs via `org.springframework.grpc:spring-grpc-spring-boot-starter` — server classes use `@Service` + extend generated `*ImplBase`; clients are
  registered as beans in a central `GrpcStubConfig` using `GrpcChannelFactory`
- Docker / Podman Compose for deployment; a top-level `Makefile` wraps the common flows

## Development Workflow

### Infrastructure Stacks (docker-compose files under `dc3/`)

The `dc3/` directory ships multiple compose files. Each has an `-aliyun` variant that pulls images from the Aliyun registry (for users in mainland China):

| Stack           | File                                   | Contents                                          |
|-----------------|----------------------------------------|---------------------------------------------------|
| `db`            | `dc3/docker-compose-db.yml`            | Postgres (`35432`), RabbitMQ (`35672`/`15672`)    |
| `optional`      | `dc3/docker-compose-optional.yml`      | EMQX broker (MQTT `31883`, dashboard `18083`)     |
| `dev`           | `dc3/docker-compose-dev.yml`           | All center services + drivers (built from source) |
| `app`           | `dc3/docker-compose.yml`               | Packaged application stack (released images)      |
| `grafana`       | `dc3/docker-compose-grafana.yml`       | Grafana observability stack                       |
| `elasticsearch` | `dc3/docker-compose-elasticsearch.yml` | Elasticsearch stack                               |

Note: The project no longer depends on Redis — caching is handled by the in-process `LocalCacheService` (see `dc3-common-public`).

### Starting Dependencies

**Preferred: use the Makefile** (wraps `podman compose`; override with `COMPOSE=...` to use `docker compose`):

```bash
# Bring up DB dependencies (Postgres + RabbitMQ)
make dev-db

# Bring up optional MQTT (EMQX)
make dev-optional

# Bring up full local dev stack (db + optional + dev services)
make dev-all

# Use Aliyun registry instead of Docker Hub
make dev-all REGISTRY=domestic      # aliases: aliyun, cn
make dev-all REGISTRY=global        # aliases: overseas, international (default)

# Packaged application stack (from released images)
make app-all REGISTRY=aliyun

# Generic compose stack helpers
make compose-up   STACK=grafana REGISTRY=cn
make compose-down STACK=dev
make compose-logs STACK=dev
make compose-ps   STACK=db
```

**Direct compose invocation (if needed):**

```bash
podman compose -f dc3/docker-compose-db.yml up -d
podman compose -f dc3/docker-compose-optional.yml up -d
```

### Building

Maven settings are checked in at `.mvn/settings.xml` — always pass `-s .mvn/settings.xml` so local builds match CI / Dockerfile behavior:

```bash
# Load local env vars used by application YAMLs
source dc3/env/dev.env.sh

# Preferred (uses Makefile)
make package           # == mvn -s .mvn/settings.xml clean package
make clean
make build             # package + build dev-stack images
make deploy            # mvn clean deploy with profile 'deploy' in dc3-api / dc3-common

# Or directly
mvn -s .mvn/settings.xml clean package
mvn -s .mvn/settings.xml clean package -pl dc3-center/dc3-center-auth -am
```

There are no Java test sources under `**/src/test/**/*.java` — rely on compile + targeted runtime smoke checks when refactoring.

### Running Services (manual, JAR mode)

Services must be started in this order:

1. **Gateway** — HTTP `8000`
   ```bash
   java -jar dc3-gateway/target/dc3-gateway.jar
   ```
2. **Auth Center** — HTTP `8300`, gRPC `9300`
3. **Data Center** — HTTP `8500`, gRPC `9500`
4. **Manager Center** — HTTP `8400`, gRPC `9400`
5. **Drivers** — e.g. `dc3-driver-virtual`, `dc3-driver-listening-virtual` (HTTP `6270`, gRPC `6271`), `dc3-driver-modbus-tcp`, `dc3-driver-mqtt`, `dc3-driver-opc-da`,
   `dc3-driver-opc-ua`, `dc3-driver-plcs7`

`dc3/env/dev.env.sh` defines host/port/credentials for local infra and `CENTER_AUTH_HOST` / `CENTER_DATA_HOST` / `CENTER_MANAGER_HOST` gRPC targets. `source dc3/env/dev.env.sh`
before running services from the shell or IDE.

## Module Structure

```
iot-dc3/                            # root POM (parent: dc3-parent:2026.4.29)
├── dc3-api/                        # Protobuf/gRPC API contracts
│   ├── dc3-api-auth                # tenant, token, user, user_login
│   ├── dc3-api-data                # point_value
│   ├── dc3-api-driver              # driver_{device,driver,entity,point,query,query_page}
│   └── dc3-api-manager             # manager_{device,driver,point,query,query_page}
│
├── dc3-common/                     # Shared components & reusable business code
│   ├── dc3-common-api              # Common gRPC definitions
│   ├── dc3-common-auth             # Auth helpers (token parsing, tenant context)
│   ├── dc3-common-constant         # Constants/enums (e.g. RabbitConstant, ManagerConstant)
│   ├── dc3-common-dal              # Data access layer (MyBatis-Plus)
│   ├── dc3-common-data             # Data service business logic
│   ├── dc3-common-driver           # Driver framework SDK (base classes for protocol drivers)
│   ├── dc3-common-exception        # Standard exception types
│   ├── dc3-common-facade-api       # Facade interface contracts (center-to-center service abstraction)
│   ├── dc3-common-facade-grpc      # gRPC-backed facade implementations (client stubs + GrpcStubConfig)
│   ├── dc3-common-facade-local     # In-process facade implementations (for single-node deployments)
│   ├── dc3-common-gateway          # Gateway filters/routing helpers
│   ├── dc3-common-log              # Logging (operation log, audit)
│   ├── dc3-common-manager          # Manager service business logic (hosts server-side gRPC implementations)
│   ├── dc3-common-model            # Domain models (BO/VO/DTO) and Builders
│   ├── dc3-common-mqtt             # MQTT client helpers
│   ├── dc3-common-postgres         # Postgres-specific configuration
│   ├── dc3-common-public           # Cross-cutting utilities; hosts `R<T>` response envelope, `LocalCacheService` in-process cache
│   ├── dc3-common-quartz           # Quartz scheduling
│   ├── dc3-common-rabbitmq         # RabbitMQ ExchangeConfig / routing
│   ├── dc3-common-repository       # Repository-pattern abstractions
│   ├── dc3-common-thread           # Thread pool / executor helpers
│   └── dc3-common-web              # Reactive web base (BaseController, validation groups)
│
├── dc3-center/                     # Deployable center services
│   ├── dc3-center-auth             # Authentication service (HTTP 8300 / gRPC 9300)
│   ├── dc3-center-data             # Data service (HTTP 8500 / gRPC 9500)
│   ├── dc3-center-manager          # Manager service (HTTP 8400 / gRPC 9400)
│   └── dc3-center-single           # Single-node all-in-one deployment option
│
├── dc3-driver/                     # Device drivers
│   ├── dc3-driver-virtual          # Virtual (polling) driver, for testing
│   ├── dc3-driver-listening-virtual # Virtual listening/push-style driver (HTTP 6270 / gRPC 6271)
│   ├── dc3-driver-modbus-tcp       # Modbus TCP
│   ├── dc3-driver-mqtt             # MQTT subscriber driver
│   ├── dc3-driver-opc-da           # OPC DA
│   ├── dc3-driver-opc-ua           # OPC UA
│   └── dc3-driver-plcs7            # Siemens S7 PLC
│
├── dc3-gateway/                    # Spring Cloud Gateway entrypoint (port 8000, StripPrefix=2)
├── dc3/                            # Compose files, env scripts (env/dev.env.sh), bin/, doc/
├── .mvn/settings.xml               # Checked-in Maven settings (always pass via -s)
├── Makefile                        # Wraps mvn + podman compose workflows
└── Dockerfile                      # Base image build (FROM pnoker/dc3-jdk:21)
```

## gRPC Service API Definitions

Proto files live under `dc3-api/*/src/main/protobuf/api/<layer>/<service>/`:

- **dc3-api-auth** (`api/center/auth/`): `tenant.proto`, `token.proto`, `user.proto`, `user_login.proto`
- **dc3-api-data** (`api/center/data/`): `point_value.proto`
- **dc3-api-driver** (`api/common/driver/`): `driver_device.proto`, `driver_driver.proto`, `driver_entity.proto`, `driver_point.proto`, `driver_query.proto`,
  `driver_query_page.proto`
- **dc3-api-manager** (`api/center/manager/`): `manager_device.proto`, `manager_driver.proto`, `manager_point.proto`, `manager_query.proto`, `manager_query_page.proto`

When modifying service APIs:

1. Edit the `.proto` file in the appropriate `dc3-api-*` module
2. Regenerate Java classes: `mvn -s .mvn/settings.xml clean package` (protobuf-maven-plugin runs at compile phase)
3. Implement the service interface in the corresponding `dc3-center-*` or driver module — the class extends the generated `*ImplBase` and is annotated with `@Service`. Spring gRPC
   auto-registers all `BindableService` beans on the server.
4. Inter-service calls use gRPC stubs produced by a central `GrpcStubConfig` (`@Bean` methods calling `GrpcChannelFactory.createChannel(<service-name>)`) and injected via
   `@Resource` — **not REST**

## Service Boundaries & Runtime Data Flow

- HTTP enters via `dc3-gateway` (port 8000). Routes defined in `dc3-gateway/src/main/resources/application-pre.yml` strip the `/api/v3` prefix (`StripPrefix=2`) and optionally
  apply the `Authentic` filter.
- Manager / Data / Auth each expose both **REST** (for the gateway) and **gRPC** (for inter-service + drivers).
- Drivers register and fetch device/point config via Manager gRPC:
    - Server: `@Service`-annotated `*ImplBase` subclasses under `dc3-common-manager/.../grpc/server/driver/` (e.g. `DriverDriverServer`); Spring gRPC registers them automatically
    - Client stubs: drivers and Data service obtain stubs from `GrpcStubConfig` (channel named `ManagerConstant.SERVICE_NAME`) and inject them with `@Resource`
- Commands and metadata changes flow asynchronously over **RabbitMQ topic exchanges**:
    - Exchange/routing key constants: `dc3-common-constant/.../RabbitConstant.java`
    - Exchange declarations: `dc3-common-rabbitmq/.../ExchangeConfig.java`
    - Example command path — Data service resolves the driver by gRPC, then publishes to `dc3.e.command` keyed by driver service name (
      `dc3-common-data/.../PointValueCommandServiceImpl.java`).

## Code Architecture Patterns

**Reactive Web Layer (center services, gateway):**

- Controllers return `Mono<R<T>>` and typically `extends BaseController` to pull tenant/user headers from the reactive context.
- Response envelope is always `R<T>` (`dc3-common-public/.../R.java`) for REST, `GrpcR` for gRPC.
- Validation uses grouped marker interfaces (`Add`, `Update`, etc.) with `@Validated(...)` on controller methods.
- URL prefixes/constants go in `*Constant` classes (e.g. `ManagerConstant.DRIVER_URL_PREFIX`), never hardcoded in controllers.

**Models & Mapping:**

- Explicit BO / VO / DTO separation — **never expose entities directly**.
- Conversions go through `*Builder` classes (e.g. `DriverBuilder`, `GrpcDriverBuilder`) rather than reflection-based mappers.

**Configuration:**

- All YAML uses `${ENV:default}` placeholders — **never hardcode `localhost` in code paths**.
- Profile selection via `${NODE_ENV:dev}`; Maven profiles: `dev` (default), `test`, `pre`, `pro`.
- `pre` / `pro` profiles resolve service discovery/config through Nacos (`spring.cloud.nacos.*` in gateway + center YAMLs).
- gRPC targets overridable via env (`CENTER_AUTH_HOST`, `CENTER_DATA_HOST`, `CENTER_MANAGER_HOST`).

**Driver Services:**

- Main class: `@SpringBootApplication` with `SpringApplication.run()`.
- Extend base classes from `dc3-common-driver`.
- Implement gRPC service interfaces from `dc3-api-driver`.
- Register with Manager Center on startup via `DriverApi.DriverRegister()`.
- **Driver service names are routing-critical** — they become suffixes in RabbitMQ routing keys and gRPC target names. Preserve existing naming constants when refactoring.

**Center Services:**

- Implement gRPC service interfaces from the matching `dc3-api-*` module.
- Use `dc3-common-dal` for data access, `LocalCacheService` from `dc3-common-public` for caching, `dc3-common-rabbitmq` for messaging.

**Multi-tenancy:**

- Tenant/namespace isolation is managed through Auth Center.
- Tenant context is propagated via request headers and available through `BaseController`.

**Error Handling:**

- Use standard exceptions from `dc3-common-exception`.
- Responses always wrapped (`R<T>` / `GrpcR`) — never leak stack traces.

## Developing a New Driver

1. Create a new module under `dc3-driver/` following `dc3-driver-virtual` as a template.
2. Add it to `dc3-driver/pom.xml` `<modules>`.
3. Depend on `dc3-common-driver`; implement the required protocol adapter.
4. Keep the driver service name consistent across `application.yml`, RabbitMQ routing, and gRPC registration.
5. Handle the standard driver responsibilities: connect to devices, acquire data, execute commands, register with Manager, report status/events.

## Git Commit Identity (for Claude's commits)

**Project-only rule — only applies to commits Claude makes in this repository.**

When committing, Claude must use the `claude[bot]` GitHub App identity so commits show Claude's real avatar on GitHub, consistent with how Copilot's commits already appear in this
repo's history. Run every commit as:

```bash
git -c user.name='Claude' \
    -c user.email='209825114+claude[bot]@users.noreply.github.com' \
    commit -m "$(cat <<'EOF'
<commit message here>

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

- `209825114` is the GitHub user ID of the official `claude[bot]` account; the `<id>+<login>@users.noreply.github.com` form is what GitHub uses to link a commit to a bot avatar.
- Do **NOT** run `git config --local user.name` / `user.email` — the repo's default git identity must stay as-is so the human author's own commits keep their own identity.
- The `Co-Authored-By` trailer may remain; it is harmless alongside the bot-identity author.

## Branching and Contribution

- **Main branch:** `main` (production-ready)
- **Development branch:** `develop` (PR integration target)
- **Release branch:** `release` (used as the PR base for this repository per local config)
- **Feature branches:** `feature/<your_name>/<feature_description>` (e.g. `feature/pnoker/mqtt_driver`)
- **License:** AGPL 3.0 — all changes must preserve this license header.

## Notes on Existing State

- No Java unit tests (`**/src/test/**/*.java` is empty). Validate changes by compile + targeted runtime smoke (spin up a compose stack, hit gateway, check driver registration /
  gRPC calls).
- READMEs with additional context: root `README.md` (and `.ja.md`, `.vi.md`, `.zh.md`), plus per-API READMEs (`dc3-api/dc3-api-auth/README.md`, `.../dc3-api-data/README.md`,
  `.../dc3-api-driver/README.md`).
