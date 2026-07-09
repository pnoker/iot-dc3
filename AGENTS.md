# AGENTS.md

Shared engineering instructions for AI coding agents working in this repository.

This is the canonical project guidance file. Keep tool-specific files such as `CLAUDE.md` as thin compatibility
pointers to this file so various AI coding assistants follow the same rules.

## Project Snapshot

IoT DC3 is a distributed industrial IoT platform for device connectivity, data collection, metadata management, and
gateway access. The project is built around Spring Boot services, gRPC contracts, RabbitMQ messaging, PostgreSQL
persistence, and Docker/Podman Compose deployment.

Primary runtime layers:

- **Gateway**: HTTP entrypoint through Spring Cloud Gateway.
- **Auth Center**: tenant, token, user, role, resource, and API authorization.
- **Manager Center**: driver, device, point, profile, and metadata coordination.
- **Data Center**: point value ingestion, query, command dispatch, and data dashboards.
- **Agentic Center**: AI-assisted operations backed by OpenAI-compatible APIs.
- **Drivers**: protocol adapters for Modbus TCP, MQTT, OPC DA/UA, PLC S7, CoAP, listening-virtual, and virtual drivers.

Core stack:

- Java 21
- Maven 3.9+
- Spring Boot / Spring Cloud (versions managed by the `dc3-parent` POM — check `pom.xml`)
- Spring Framework 7
- PostgreSQL, RabbitMQ, optional EMQX, optional Grafana/Elasticsearch stacks
- Spring gRPC with generated protobuf APIs
- Compose runtime, wrapped by the root `Makefile` (`COMPOSE=podman compose` by default)

## Repository Map

```
iot-dc3/
├── dc3-api/                          # Protobuf & gRPC contracts
│   ├── dc3-api-auth                  #   auth service proto
│   ├── dc3-api-data                  #   data service proto
│   ├── dc3-api-driver                #   driver service proto
│   └── dc3-api-manager               #   manager service proto
├── dc3-common/                       # Shared libraries
│   ├── dc3-common-model              #   Base BO/VO/DTO/Builder/Ext classes
│   ├── dc3-common-public             #   BaseService<B,Q>, R<T> envelope, TenantOwned
│   ├── dc3-common-web                #   BaseController (reactive), WebFlux config
│   ├── dc3-common-auth               #   Token gRPC server, auth controllers
│   ├── dc3-common-manager            #   Device/Driver/Point/Profile services + DAL
│   ├── dc3-common-data               #   Point value ingestion, command dispatch
│   ├── dc3-common-driver             #   Driver SDK (SPI interfaces + runtime)
│   ├── dc3-common-facade/            #   Cross-service facade
│   │   ├── dc3-common-facade-api             #     Interface contracts
│   │   ├── dc3-common-facade-grpc            #     gRPC implementations
│   │   ├── dc3-common-facade-local-auth      #     In-process auth facade
│   │   ├── dc3-common-facade-local-data      #     In-process data facade
│   │   └── dc3-common-facade-local-manager   #     In-process manager facade
│   ├── dc3-common-dal                #   Shared label/group DAL
│   ├── dc3-common-postgres           #   MyBatis-Plus configuration
│   ├── dc3-common-rabbitmq           #   RabbitMQ config and constants
│   ├── dc3-common-mqtt               #   MQTT config
│   ├── dc3-common-repository         #   Point value repository abstraction
│   ├── dc3-common-quartz             #   Scheduling infrastructure
│   ├── dc3-common-test               #   Testcontainers, harnesses, contract test bases
│   ├── dc3-common-agentic            #   AI-assisted operations
│   ├── dc3-common-gateway            #   Gateway utilities
│   ├── dc3-common-log                #   Logging configuration
│   ├── dc3-common-exception          #   Exception hierarchy
│   ├── dc3-common-constant           #   Shared constants
│   ├── dc3-common-sql                #   SQL utilities
│   ├── dc3-common-thread             #   Thread pool config
│   ├── dc3-common-api                #   API utilities
│   └── dc3-common-resource-registrar #   Resource registration
├── dc3-center/                       # Deployable service applications
│   ├── dc3-center-auth               #   Auth service
│   ├── dc3-center-manager            #   Manager service
│   ├── dc3-center-data               #   Data service
│   ├── dc3-center-agentic            #   AI-assisted operations service
│   └── dc3-center-single             #   All-in-one single process
├── dc3-driver/                       # Protocol driver implementations
│   ├── dc3-driver-modbus-tcp         #   Modbus TCP
│   ├── dc3-driver-modbus-rtu         #   Modbus RTU
│   ├── dc3-driver-mqtt               #   MQTT
│   ├── dc3-driver-opc-ua             #   OPC UA
│   ├── dc3-driver-opc-da             #   OPC DA
│   ├── dc3-driver-plcs7              #   S7 PLC
│   ├── dc3-driver-coap               #   CoAP
│   ├── dc3-driver-virtual            #   Virtual (simulator)
│   ├── dc3-driver-listening-virtual  #   Listening virtual
│   └── ... (20+ more protocol drivers)
├── dc3-gateway/                      # Spring Cloud Gateway (HTTP entrypoint)
├── dc3-coverage/                     # JaCoCo aggregate coverage report
├── dc3-e2e/                          # Testcontainers-backed E2E tests
├── dc3/                              # Compose files, env, scripts, docs
│   ├── docker-compose.yml            #   Main app stack
│   ├── docker-compose-db.yml         #   Database services
│   ├── docker-compose-dev.yml        #   Development overrides
│   ├── docker-compose-optional.yml   #   Optional monitoring/messaging
│   ├── env/dev.env                   #   IDE-friendly local env vars
│   └── bin/                          #   changelog.py, tag.sh
├── docs/                             # VitePress documentation site
├── Makefile                          # Preferred command entrypoint
├── .husky/                           # Git hooks (pre-commit runs lint-staged)
└── .mvn/settings.xml                 # Local Maven mirror (mainland China)
```

## Layering Architecture

All business modules follow a strict four-layer pattern:

```
Controller (WebFlux, Mono<R<T>>)  →  Service (BO)  →  Manager (DO, MyBatis-Plus)  →  Mapper (SQL)
```

### Key Base Classes

| Class                           | Module              | Purpose                                                                               |
|---------------------------------|---------------------|---------------------------------------------------------------------------------------|
| `BaseService<B,Q>`              | `dc3-common-public` | CRUD interface: `add`, `delete`, `update`, `getById`, `list(Q)`                       |
| `BaseController`                | `dc3-common-web`    | Reactive controller interface with `getUserHeader`, `requireTenant`, `async` defaults |
| `R<T>`                          | `dc3-common-public` | Response envelope: `ok`, `code`, `message`, `data` — use `R.ok(data)` / `R.fail(msg)` |
| `BaseBO` / `BaseVO` / `BaseDTO` | `dc3-common-model`  | Shared fields: `id`, `remark`, `creatorId/Name/Time`, `operatorId/Name/Time`          |
| `BaseBuilder`                   | `dc3-common-model`  | MapStruct `@Mapper(componentModel = "spring")` for VO↔BO↔DTO conversion               |
| `BaseExt`                       | `dc3-common-model`  | JSON extension column: `type`, `version`, `remark`                                    |
| `TenantOwned`                   | `dc3-common-public` | Marker interface for tenant-scoped entities; used by `BaseController.requireTenant()` |

### Controller Layer

Controllers implement the `BaseController` interface (Java interface with default methods, not an abstract class). They
return `Mono<R<T>>`. The `async()` helper offloads blocking JDBC calls to the `boundedElastic` scheduler. Example:

```java
public Mono<R<DeviceBO>> getById(@PathVariable Long id) {
    return async(() -> R.ok(deviceService.getById(id)));
}
```

### Service Layer

Service interfaces extend `BaseService<B, Q>` and work exclusively in BO types. Cross-service calls go through facade
interfaces. Concrete services inject their Manager (DAL layer) and handle BO↔DO conversion via builders.

### Manager Layer (DAL)

Manager interfaces extend MyBatis-Plus `IService<DO>`. Implementations extend `ServiceImpl<Mapper, DO>`. They provide
`checkDuplicate()` and `innerSave()`. The `select*` verb is reserved for this layer only. The Mapper layer is standard
MyBatis-Plus `BaseMapper<DO>`.

DO classes use snowflake IDs (`@TableId(type = IdType.ASSIGN_ID)`), soft delete (`@TableLogic` on `deleted` field), and
`JacksonTypeHandler` for JSON columns.

## Command Rules

Prefer the root `Makefile` when a target exists. It keeps commands consistent across local machines and CI.

Common commands:

```bash
make up-db
make up-optional
make up-db && make up-optional && make up-dev
make package
make changelog
```

Direct Maven commands should use the checked-in settings file for local development:

```bash
mvn -s .mvn/settings.xml clean package
mvn -s .mvn/settings.xml -q -DskipTests compile

# Run tests for a single module
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager

# Run a single test class
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager -Dtest=DeviceControllerTest

# Run a single test method
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-public -Dtest="RTest#testOkWithData"
```

GitHub Actions should not need `.mvn/settings.xml` unless a workflow is intentionally testing the mainland-China mirror
path. CI should prefer public Maven Central defaults.

## Environment Files

- Root `.env.example`: template for Docker/Podman Compose interpolation.
- Root `.env`: local, untracked Compose interpolation file created from `.env.example`.
- `dc3/env/dev.env`: IDE-friendly local Java process variables without `export`.
- `dc3/env/dev.env.sh`: shell-friendly local Java process variables with `export`.

Do not treat these files as interchangeable. See `docs/quickstart/environment.md` before changing environment variables.

## Compose Rules

The compose files under `dc3/` are the canonical container definitions:

- `docker-compose.yml`
- `docker-compose-dev.yml`
- `docker-compose-db.yml`
- `docker-compose-optional.yml` (EMQX + Elasticsearch/Logstash/Kibana/APM + Prometheus/exporters/Grafana)

Registry selection is controlled by environment variables and Make arguments, not by duplicated registry-specific
compose files:

```bash
make up STACK=dev REGISTRY=cn
make up-db-global && make up-optional-global && make up-app-global
make compose-config STACK=optional REGISTRY=cn
```

For container changes, run `make config` or the corresponding `podman compose config` path for every
touched compose file.

## Architecture Rules

### Tenant Safety

Tenant isolation is a hard requirement.

- New queries must preserve tenant scope.
- New gRPC requests must carry tenant IDs when the contract supports them.
- Cache keys for tenant-scoped data must include tenant context.
- Cross-service lookups must validate ownership before returning or mutating data.
- Do not add `tenantId IS NULL` shortcuts unless the data model explicitly defines global records.

### Facade Boundaries

Use facade interfaces for cross-service calls from business code.

- Put contracts in `dc3-common-facade-api`.
- Put gRPC-backed implementations in `dc3-common-facade-grpc`.
- Put in-process implementations in `dc3-common-facade-local`.
- Keep controllers and service classes from binding directly to transport details unless they are transport adapters.

### gRPC

Proto files live under `dc3-api/*/src/main/protobuf`.

When changing a gRPC contract:

1. Update the `.proto` file.
2. Regenerate by compiling the affected module or the full project.
3. Update server implementations and client builders/stubs together.
4. Preserve backward compatibility where practical.
5. Verify tenant propagation and error envelope behavior.

Server classes should be Spring beans extending generated `*ImplBase` classes. Client stubs should come from shared
stub configuration instead of ad hoc channel construction.

Server implementation pattern:

```java
@Service
public class ManagerDriverServer extends DriverApiGrpc.DriverApiImplBase {
    // Inject Grpc*Builder for domain ↔ gRPC DTO conversion
    // Each method builds a GrpcR result envelope (mirroring REST R<T>)
    // Uses StreamObserver for async response:
    //   onNext(builder.build()) + onCompleted()
}
```

### Driver SDK

Drivers implement a composable set of SPI interfaces from `dc3-common-driver`. The SDK handles registration,
scheduling, and value dispatch — drivers only implement protocol logic.

| SPI Interface            | Methods                                              | Purpose                                               |
|--------------------------|------------------------------------------------------|-------------------------------------------------------|
| `DriverProtocol`         | `read(...)`, `write(...)`                            | Core protocol I/O (the primary contract)              |
| `DriverLifecycle`        | `initial()`, `schedule()`                            | Startup initialization + periodic Quartz task         |
| `DriverMetadataListener` | `event(MetadataEventDTO)`                            | React to CRUD changes on driver/device/point metadata |
| `DriverHealth`           | `health()` → `DriverHealthState`                     | Driver-level health (defaults to ONLINE)              |
| `DeviceHealth`           | `health(driverConfig, device)` → `DeviceHealthState` | Per-device health check                               |
| `DriverCommand`          | `execute(...)` → `Map<String,String>`                | Custom device commands                                |

`DriverCustomService` aggregates all SPI interfaces for convenience. Protocol-agnostic plumbing (registration via
`DriverRegisterService`, scheduling via `DriverScheduleService`, value sending via `DriverSenderService`) is handled by
the SDK runtime.

### Web API

- Controllers return the standard response envelope.
- Do not expose persistence entities directly.
- Use BO/VO/DTO separation and existing builder classes.
- Use grouped validation annotations consistently.
- Keep validation and exception messages in English.

### OpenAPI / Swagger

REST endpoints are documented with springdoc-openapi (annotations only, no
hand-maintained spec). See `docs/development/api-documentation.md` for the full
guide.

- Annotate controllers (`@Tag`, `@Operation`, `@Parameter`) and DTOs
  (`@Schema` with `example` / `requiredMode` where useful). Keep all doc text
  English.
- Each business module owns a `GroupedOpenApi` bean (e.g. `AuthApiGroupConfig`);
  add one when introducing a new center with its own controller package, plus a
  gateway aggregation route and `swagger-ui.urls` entry.
- Shared config (`SpringDocConfig`, `WebFluxSecurityConfig`) lives in
  `dc3-common-web` and is registered in `AutoConfiguration.imports` — center
  apps do not scan `io.github.pnoker.common.config`, so a plain `@Configuration`
  there will not load.
- Docs are exposed in dev/test/pre and disabled in production (`pro` profile).
  View aggregated docs at the gateway `:8000/swagger-ui.html`; export with
  `make openapi` against a running stack.

### CRUD Verb Convention

The verb on every CRUD-shaped method and HTTP path must reflect the
cardinality of the result, applied consistently across Service interfaces,
ServiceImpl, Controller, Local Facade, gRPC Facade, gRPC server, and gRPC
RPC names in `.proto` files.

| Action                 | Java method    | HTTP path   | gRPC RPC  |
|------------------------|----------------|-------------|-----------|
| Create one record      | `add(BO)`      | `/add`      | n/a       |
| Delete by id           | `delete(Long)` | `/delete`   | n/a       |
| Update one record      | `update(BO)`   | `/update`   | n/a       |
| Query single record    | `getXxx(...)`  | `/get_xxx`  | `GetXxx`  |
| Query multiple records | `listXxx(...)` | `/list_xxx` | `ListXxx` |

- The base CRUD methods are inherited from `BaseService<B, Q>`:
  `add`, `delete`, `update`, `getById`, `list(Q)`. Subinterfaces add
  `getByXxx`/`listByXxx` only with extra cardinality-matching verbs.
- `select*` is reserved for raw MyBatis Mapper calls inside `*ManagerImpl`
  classes, never on Service/Controller/Facade APIs.
- `remove*` is reserved for MyBatis-Plus inherited Manager methods
  (`removeById`, `remove(wrapper)`); business deletion uses `delete*`.
- `find*`, `query*`, `fetch*` are not used as primary CRUD verbs.
- HTTP paths use lowercase snake_case and mirror the Java method name.
- gRPC RPC names use PascalCase and mirror the Java method name.

Special cases follow the same cardinality rule:

- `getStatusByPage(Q)` for status-snapshot lookups whose return type is a
  `Map<Long,String>`, not a `Page` (DeviceStatusService, DriverStatusService).
- Boolean-returning action methods stay on a try-pattern verb when the
  failure outcome is a normal result, e.g. `tryCancelToken(...)`.
- Single-record dispatch facades use `dispatch*` to avoid noun-verb
  ambiguity (`PointValueCommandFacade.dispatchRead/dispatchWrite`).

### Domain Modeling

Keep persistence, business, and web representations deliberately separated.

- Persistence objects (`*DO`) model the database shape. Database-coded flags and type columns may stay as `Byte` on
  `*DO` classes, but they must not leak into business or response models when a domain enum exists or should exist.
- Business objects (`*BO`) carry business semantics. Use enums such as `EnableFlagEnum`, `DefaultFlagEnum`, or
  domain-specific enums instead of naked `Byte`, `Integer`, or `String` flags.
- View objects (`*VO`) carry API response/request semantics. Prefer the same domain enums used by the corresponding
  `*BO`, unless a public API compatibility requirement explicitly requires a primitive or string.
- Use MapStruct `*Builder` classes for `VO <-> BO <-> DO` conversion. Put enum/index conversion in the builder
  (`EnumValue.getIndex()` for `BO -> DO`, `Enum.ofIndex(...)` for `DO -> BO`) instead of scattering it through services.
- Controllers should translate web input/output (`VO`, request DTOs, path variables) to and from `BO`; services should
  expose and accept `BO` rather than `VO` for persistent business entities.
- Read-only projection/aggregation responses (dashboard statistics, health snapshots, topic listings, model-option
  lists, history query results, etc.) may be returned directly as `VO` from read-only query services without a parallel
  `BO` — these carry no business behaviour and forcing a duplicate `BO` is over-modeling. The "services expose `BO`"
  rule applies to persistent business entities and to write-path business inputs (command submission, event reports),
  which must accept a `BO`; the controller (or transport adapter) converts the inbound `VO`/request into that `BO`.
- MyBatis-Plus query conditions may use enum values directly when the enum field has `@EnumValue`, for example
  `.eq(EntityDO::getEnableFlag, EnableFlagEnum.ENABLE)`. Plain Java comparisons against `Byte` fields must compare
  with the enum index, preferably centralized in a builder or helper.
- Do not introduce magic flag constants such as `private static final Byte DEFAULT = 1`. Add or reuse a domain enum
  instead, with `@EnumValue`, `ofIndex(...)`, and clear names.
- Domain enum suffixes follow strict semantics:
    - `*FlagEnum` for boolean-like 0/1 toggles (`EnableFlagEnum`, `DefaultFlagEnum`, `ConfirmFlagEnum`).
    - `*StatusEnum` for state-machine values with multiple states (`EntityStatusEnum`, `RuleStatusEnum`,
      `NotifyHistoryStatusEnum`). Do not append `Flag` to a state-machine enum name.
    - `*TypeEnum` for closed classification sets, including multi-valued classifications and levels
      (`MetadataTypeEnum`, `PointTypeEnum`, `EventLevelEnum`, `ExpireTypeEnum`). Multi-valued sets must not
      use the `*FlagEnum` suffix.
- Enum constant names use `UPPER_SNAKE_CASE` and stay descriptive — single-letter names like `R`/`W` are not allowed.
  The internal `code` string field on enums uses lowercase tokens (e.g. `"enable"`, `"online"`, `"pending"`) so that
  values are consistent across `*FlagEnum`, `*StatusEnum`, and `*TypeEnum` definitions.
- Do not expose secrets in `VO` classes, and exclude secret-bearing fields such as `apiKey`, `password`, `secret`,
  `token`, and credentials from Lombok `@ToString`.

### Configuration

- Custom `@ConfigurationProperties` prefixes must use `dc3.*`.
- Prefer typed properties with validation over scattered `@Value` usage.
- Keep legacy aliases only when they protect existing deployments during migration.
- YAML should use `${ENV:default}` placeholders for deploy-time values.
- Services built on `dc3-common-web` use WebFlux; configure request base paths with `spring.webflux.base-path`, not
  `server.servlet.context-path`.

All custom prefixes use `dc3.*`. Check `@ConfigurationProperties` annotations in the codebase for the current set — do
not hardcode a prefix list in documentation.

### Logging

Use the repository logging convention in `docs/guide/logging.md`.

- Use English, stable event names, and parameterized SLF4J placeholders.
- Prefer key-value fields such as `tenantId={}, userId={}, deviceId={}` over prose-only messages.
- Do not log secrets, tokens, passwords, full request bodies, or raw private payloads at `info`.
- Pass the exception object to warn/error logs for caught exceptions unless the stack trace is intentionally suppressed.

### Driver Metadata

Driver `application.yml` files are user-facing metadata. Keep `name`, `attribute-name`, and `remark` values in English.

Driver `code` values are routing-critical and must remain stable after a driver is used in real deployments. Changing a
driver code requires a migration plan for manager metadata and RabbitMQ routing.

## Release Notes

`dc3/doc/CHANGE.md` is generated from git history. Do not hand-edit the current release block unless fixing generator
output.

Use:

```bash
make changelog
make changelog FROM=<previous-tag-or-ref> TO=HEAD VERSION=<version>
```

Commit message quality directly affects release notes. Vague subjects such as `update`, `fix`, `.`, `add comment`, or
non-English subjects make the generated changelog poor and should be rejected.

Generated changelog-only commits with subject `docs(release): update generated changelog` are skipped by default by the
generator so rerunning `make changelog` after committing `CHANGE.md` remains stable.

## Commit Rules

### AI Commit Confirmation

AI coding agents must not create commits without explicit user confirmation.

Before each commit, show:

- the proposed commit message;
- the files included in that commit;
- the reason this group of files belongs together;
- the verification already run for the change.

Wait for the user to approve that specific commit before running `git commit`.

Do not batch unrelated changes into one commit. Split commits by intent, for example:

- feature or behavior changes;
- bug fixes;
- configuration changes;
- documentation changes;
- generated release notes.

`dc3/doc/CHANGE.md` must be committed separately with exactly:

```text
docs(release): update generated changelog
```

If the user asks to commit multiple changes, first present the proposed commit sequence and wait for approval.

Use Conventional Commit subjects:

```text
feat(agentic): add session cleanup policy
fix(manager): validate tenant scope for device queries
docs(env): explain JetBrains IDEA environment variables
refactor(container): deduplicate compose registry overrides
```

Required subject format:

```text
<type>(optional-scope): <english imperative summary>
```

Allowed types:

- `feat`
- `fix`
- `perf`
- `refactor`
- `docs`
- `build`
- `ci`
- `test`
- `chore`
- `style`
- `security`
- `revert`

Rules:

- Use English in the subject.
- Keep the subject specific enough to be useful in `CHANGE.md`.
- Prefer a scope for anything outside a tiny root-only change.
- Do not use weak subjects such as `update`, `fix bug`, `change code`, `misc`, `wip`, or `.`.
- Use `!` for breaking changes and explain the impact in the body.
- For release-note-only commits, use exactly `docs(release): update generated changelog`.

Husky manages Git hooks automatically — no manual install needed. The `pre-commit` hook runs lint-staged (eslint)
on staged files before each commit. A `commit-msg` hook stub exists (`.husky/_/commit-msg`) but has no
validation script attached yet; to enforce conventional-commit format, add a script at `.husky/commit-msg`.

## Testing

### Test Pyramid

- **Unit tests** (`*Test.java`, `*Tests.java`): run by Surefire. JUnit 5 + Mockito (
  `@ExtendWith(MockitoExtension.class)`),
  AssertJ assertions, Reactor `StepVerifier` for reactive code. No Spring context spin-up — controllers are tested with
  manual dependency injection and context wiring.
- **Integration tests** (`*IT.java`): run by Failsafe. Use `dc3-common-test` infrastructure: Testcontainers
  (`PgTimescaleContainer`, `RabbitContainer`, `MqttContainer`), gRPC in-process extension (`GrpcInProcessExtension`),
  and RabbitMQ test harness (`RabbitTestHarness`).
- **E2E tests** (`dc3-e2e/`): gated behind `@EnabledIfEnvironmentVariable(named = "DC3_E2E")`. Boots real
  PostgreSQL/TimescaleDB and RabbitMQ via Testcontainers on a shared Docker network. Tests messaging contracts (command
  dispatch, event routing, hypertable operations). Run with `make test-e2e`.

### Test Infrastructure (`dc3-common-test`)

| Utility                  | Purpose                                                              |
|--------------------------|----------------------------------------------------------------------|
| `GrpcInProcessExtension` | JUnit 5 extension: in-process gRPC server + managed channel per test |
| `RabbitTestHarness`      | Send/receive to RabbitMQ in tests; `awaitTrue()` via Awaitility      |
| `PgTimescaleContainer`   | Singleton `timescale/timescaledb-ha:pg18` container                  |
| `RabbitContainer`        | RabbitMQ testcontainer                                               |
| `MqttContainer`          | MQTT testcontainer                                                   |
| `FixedClockConfig`       | `@TestConfiguration` overriding the `Clock` bean to a fixed instant  |
| `JsonAssertions`         | `assertJsonEquals()` and `assertJsonContains()` over JSONAssert      |

### Contract Tests

- `EnumContractTest<E>`: abstract test verifying `getIndex()` uniqueness, `ofIndex()` round-trip, and name stability
  for all enum constants via `@TestFactory`.
- `SecretFieldContractTest`: verifies sensitive fields (apiKey, password, secret, token) are excluded from
  `@ToString` and serialization.

### Coverage Gate

`make coverage` generates the aggregate JaCoCo report at `dc3-coverage/target/site/jacoco-aggregate/`. The
`check_coverage.py` script enforces minimum thresholds (default: ≥20% line, ≥15% branch). Coverage regressions greater
than 1% block the change.

## Validation Checklist

Run checks proportional to the change:

- Java/shared behavior: `mvn -s .mvn/settings.xml -q -DskipTests compile`
- Full package: `mvn -s .mvn/settings.xml clean package`
- Behaviour change in tested code: `make test`
- DAL or SQL change: `make test-it` (requires a Docker-compatible container runtime, runs Testcontainers)
- gRPC proto change: regenerate stubs and run the matching contract tests
- Aggregate coverage check: `make coverage` and inspect
  `dc3-coverage/target/site/jacoco-aggregate/index.html`. Coverage
  regressions greater than 1% block the change.
- Changelog script: `python3 -m py_compile dc3/bin/changelog.py`
- Compose files: `podman compose -f dc3/<file>.yml config --quiet`
- YAML syntax: parse changed YAML after normalizing Maven placeholders such as `@project.artifactId@`
- Agent or docs changes: check links, command examples, stale filenames, and current workflow names

See `docs/development/testing.md` for the full test pyramid, naming conventions,
Testcontainers strategy and CI workflow expectations.

Before committing code that changes public behavior, mention what was verified and what was not verified.

## Editing Rules

- Keep changes focused on the request.
- Do not revert unrelated user changes.
- Prefer existing patterns and helper APIs over new abstractions.
- Keep public/user-facing text in English unless a specific localized document is being edited.
- Preserve AGPL headers where they already exist.
- Avoid generated metadata churn unless the task requires it.
- Use structured parsers or existing toolchains for structured files when practical.

## Documentation Rules

- Root README files in multiple languages should stay structurally aligned.
- Runtime and environment changes should update `docs/quickstart/environment.md`.
- Driver authoring changes should update `docs/development/driver-authoring.md`.
- Container changes should update compose examples and `.env.example` if variables change.
- Test strategy, harness or coverage gate changes should update `docs/development/testing.md`.
- Release workflow changes should update `CONTRIBUTING.md` and this file.
