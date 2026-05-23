# AGENTS.md

Shared engineering instructions for AI coding agents working in this repository.

This is the canonical project guidance file. Keep tool-specific files such as `CLAUDE.md` as thin compatibility
pointers to this file so Codex, Claude Code, and similar coding tools follow the same rules.

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
- **Drivers**: protocol adapters for Modbus TCP, MQTT, OPC DA/UA, PLC S7, and virtual drivers.

Core stack:

- Java 21
- Maven 3.9+
- Spring Boot 4.0.6 / Spring Framework 7
- PostgreSQL, RabbitMQ, optional EMQX, optional Grafana/Elasticsearch stacks
- Spring gRPC with generated protobuf APIs
- Compose runtime, wrapped by the root `Makefile` (`COMPOSE=podman compose` by default)

## Repository Map

- `dc3-api/`: protobuf and gRPC API contracts.
- `dc3-common/`: shared domain, business, facade, transport, persistence, configuration, and utility modules.
- `dc3-center/`: deployable center services.
- `dc3-driver/`: deployable device drivers.
- `dc3-gateway/`: HTTP gateway service.
- `dc3/`: compose files, runtime env files, scripts, and project docs.
- `.mvn/settings.xml`: local Maven mirror configuration used for mainland China development.
- `Makefile`: preferred command entrypoint for build, compose, hooks, and release helpers.

## Command Rules

Prefer the root `Makefile` when a target exists. It keeps commands consistent across local machines and CI.

Common commands:

```bash
make dev-db
make dev-optional
make dev-all
make package
make changelog
make install-hooks
```

Direct Maven commands should use the checked-in settings file for local development:

```bash
mvn -s .mvn/settings.xml clean package
mvn -s .mvn/settings.xml -q -DskipTests compile
```

GitHub Actions should not need `.mvn/settings.xml` unless a workflow is intentionally testing the mainland-China mirror
path. CI should prefer public Maven Central defaults.

## Environment Files

- Root `.env.example`: template for Docker/Podman Compose interpolation.
- Root `.env`: local, untracked Compose interpolation file created from `.env.example`.
- `dc3/env/dev.env`: IDE-friendly local Java process variables without `export`.
- `dc3/env/dev.env.sh`: shell-friendly local Java process variables with `export`.

Do not treat these files as interchangeable. See `dc3/doc/ENVIRONMENT.md` before changing environment variables.

## Compose Rules

The compose files under `dc3/` are the canonical container definitions:

- `docker-compose.yml`
- `docker-compose-dev.yml`
- `docker-compose-db.yml`
- `docker-compose-optional.yml` (EMQX + Elasticsearch/Logstash/Kibana/APM + Prometheus/exporters/Grafana)

Registry selection is controlled by environment variables and Make arguments, not by duplicated registry-specific
compose files:

```bash
make dev REGISTRY=cn
make app-all REGISTRY=global
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

### Web API

- Controllers return the standard response envelope.
- Do not expose persistence entities directly.
- Use BO/VO/DTO separation and existing builder classes.
- Use grouped validation annotations consistently.
- Keep validation and exception messages in English.

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
- MyBatis-Plus query conditions may use enum values directly when the enum field has `@EnumValue`, for example
  `.eq(EntityDO::getEnableFlag, EnableFlagEnum.ENABLE)`. Plain Java comparisons against `Byte` fields must compare
  with the enum index, preferably centralized in a builder or helper.
- Do not introduce magic flag constants such as `private static final Byte DEFAULT = 1`. Add or reuse a domain enum
  instead, with `@EnumValue`, `ofIndex(...)`, and clear names.
- Domain enum suffixes follow strict semantics:
    - `*FlagEnum` for boolean-like 0/1 toggles (`EnableFlagEnum`, `DefaultFlagEnum`, `ExpireFlagEnum`).
    - `*StatusEnum` for state-machine values with multiple states (`DeviceStatusEnum`, `DriverStatusEnum`,
      `NotifyHistoryStatusEnum`). Do not append `Flag` to a state-machine enum name.
    - `*TypeEnum` for closed classification sets (`MetadataTypeEnum`, `ResponseEnum`-style code groups).
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

Current custom prefixes:

- `dc3.agentic`
- `dc3.driver`
- `dc3.driver.mqtt`
- `dc3.resource-registrar`
- `dc3.thread`

### Logging

Use the repository logging convention in `dc3/doc/LOGGING.md`.

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

Install local commit validation:

```bash
make install-hooks
```

The hook checks commit subjects before `git commit` completes. It is a local Git mechanism; CI or branch protection
should run the same script for server-side enforcement if needed.

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

See `dc3/doc/TESTING.md` for the full test pyramid, naming conventions,
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
- Runtime and environment changes should update `dc3/doc/ENVIRONMENT.md`.
- Driver authoring changes should update `dc3/doc/DRIVER-AUTHORING.md`.
- Container changes should update compose examples and `.env.example` if variables change.
- Test strategy, harness or coverage gate changes should update `dc3/doc/TESTING.md`.
- Release workflow changes should update `CONTRIBUTING.md` and this file.
