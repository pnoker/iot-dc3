# AGENTS.md

Canonical engineering instructions for AI coding agents working in IoT DC3.

## Scope and precedence

- This file applies to the whole repository.
- Follow the user's request first, then this repository-level guidance.
- Keep changes focused. Do not revert unrelated work in a dirty worktree.
- Do not create a Git commit without the explicit, commit-specific confirmation described below.

## Sources of truth

Avoid copying volatile versions or generated state into documentation. Verify them at the source:

| Concern | Source of truth |
|---|---|
| Java, Spring, Maven plugins and reactor modules | root `pom.xml` and affected module POMs |
| Backend build commands | root `Makefile` |
| Frontend dependencies and scripts | `dc3-web/package.json` and `dc3-web/pnpm-lock.yaml` |
| Frontend build image/tool pins | `dc3-web/Dockerfile` |
| Containers and registries | root `Makefile`, `.env.example`, and `dc3/docker-compose*.yml` |
| CI behaviour | `.github/workflows/` |
| Release notes | `dc3/bin/changelog.py` and generated `dc3/doc/CHANGE.md` |

If this file disagrees with executable configuration, treat the executable configuration as current and update this
file as part of the same change when appropriate.

## Project overview

IoT DC3 is a multi-protocol, cloud-native, open-source industrial IoT platform evolving toward AI agents. Its main runtime areas are:

- Gateway: HTTP entrypoint through Spring Cloud Gateway.
- Auth Center: tenant, token, user, role, resource, and API authorization.
- Manager Center: drivers, devices, points, profiles, and metadata.
- Data Center: point-value ingestion, queries, commands, and dashboards.
- Agentic Center: AI-assisted operations through OpenAI-compatible APIs.
- Drivers: protocol adapters built on the shared driver SDK.
- Web: Vue-based management UI in `dc3-web/`.

The backend targets Java 21 and is a Maven multi-module Spring Boot/Spring Cloud project. Check the root POM for current
framework versions.

## Repository map

```text
iot-dc3/
├── dc3-api/              protobuf and gRPC contracts
├── dc3-common/           shared models, services, DAL, facades, driver SDK, and infrastructure
├── dc3-center/           deployable auth, manager, data, agentic, and single-process applications
├── dc3-driver/           protocol driver implementations
├── dc3-gateway/          HTTP gateway
├── dc3-web/              Vue/TypeScript frontend
├── dc3-coverage/         aggregate JaCoCo report and absolute coverage gate
├── dc3-e2e/              Testcontainers-backed end-to-end tests
├── dc3/                  compose files, environment files, scripts, and generated release notes
├── docs/design/          design documents (e.g. `mq-abstraction.md` — proposed pluggable broker port)
├── Makefile              preferred backend/container command entrypoint
└── .mvn/settings.xml     local Maven mirror configuration
```

## Backend architecture

### Layering

Business modules follow this flow:

```text
Controller (WebFlux) -> Service (BO) -> Manager (DO) -> Mapper (SQL)
```

- Controllers implement the `BaseController` interface and return `Mono<R<T>>`.
- Use `BaseController.async(...)` to move blocking JDBC work to the bounded-elastic scheduler.
- Services extend `BaseService<B, Q>`, work in business objects, and own business rules.
- Managers extend MyBatis-Plus `IService<DO>`; implementations extend `ServiceImpl<Mapper, DO>`.
- Mappers extend MyBatis-Plus `BaseMapper<DO>` and contain persistence-level SQL operations.
- Do not expose persistence objects from controllers or facade contracts.

Common types:

| Type | Module | Role |
|---|---|---|
| `BaseService<B,Q>` | `dc3-common-public` | base CRUD service contract |
| `BaseController` | `dc3-common-web` | reactive controller helpers and user/tenant context |
| `R<T>` | `dc3-common-public` | standard response envelope; use `R.ok(...)` and `R.fail(...)` |
| `BaseBO`, `BaseVO`, `BaseDTO` | `dc3-common-model` | shared business, web, and transfer fields |
| `BaseBuilder` | `dc3-common-model` | MapStruct conversion base |
| `TenantOwned` | `dc3-common-public` | marker for tenant-scoped entities |

### Tenant safety

Tenant isolation is a hard requirement.

- Preserve tenant scope in every new query and mutation.
- Carry tenant IDs through gRPC requests whenever the contract supports them.
- Include tenant context in cache keys for tenant-owned data.
- Validate ownership before returning or mutating data across service boundaries.
- Do not add `tenantId IS NULL` shortcuts unless the data model explicitly defines global records.
- Treat missing tenant validation as a correctness and security defect, not a convenience trade-off.

### Facade boundaries

Business code must use facade interfaces for cross-service calls.

- Contracts belong in `dc3-common-facade-api`.
- Transport-backed implementations belong in `dc3-common-facade-grpc`.
- In-process implementations belong in the matching module:
  `dc3-common-facade-local-auth`, `dc3-common-facade-local-data`, or
  `dc3-common-facade-local-manager`.
- `dc3-common-facade-local` is a dependency aggregator and does not contain implementation sources.
- Keep controllers and services independent from transport details unless they are explicit transport adapters.

### gRPC contracts

Proto files live under `dc3-api/*/src/main/protobuf`.

When changing a contract:

1. Update the `.proto` file.
2. Compile the affected API module to regenerate sources.
3. Update server implementations and client builders/stubs together.
4. Preserve backward compatibility where practical.
5. Verify tenant propagation and the `GrpcR` error envelope.

Servers are Spring beans extending generated `*ImplBase` classes. Reuse shared stub configuration; do not construct ad
hoc channels in business code.

### Driver SDK

Drivers implement protocol behaviour through the SPI types in `dc3-common-driver`; shared runtime services handle
registration, scheduling, and value dispatch.

Primary extension points are `DriverProtocol`, `DriverLifecycle`, `DriverMetadataListener`, `DriverHealth`,
`DeviceHealth`, and `DriverCommand`. Prefer existing SDK plumbing over driver-specific infrastructure.

Driver `application.yml` metadata is user-facing:

- Keep `name`, `attribute-name`, and `remark` in English.
- Treat driver `code` values as routing-stable identifiers. Changing one requires a metadata and RabbitMQ migration
  plan.

## API and domain conventions

### CRUD verbs

CRUD-shaped names reflect result cardinality across Service, Controller, Facade, gRPC server, and proto RPCs:

| Action | Java | HTTP | gRPC |
|---|---|---|---|
| create one | `add(BO)` | `/add` | n/a |
| delete by ID | `delete(Long)` | `/delete` | n/a |
| update one | `update(BO)` | `/update` | n/a |
| return one | `getXxx(...)` | `/get_xxx` | `GetXxx` |
| return many | `listXxx(...)` | `/list_xxx` | `ListXxx` |

- Base CRUD comes from `BaseService<B,Q>`: `add`, `delete`, `update`, `getById`, and `list(Q)`.
- Reserve `select*` for raw Mapper/Manager persistence operations.
- Reserve MyBatis-Plus `remove*` for the Manager layer; business deletion uses `delete*`.
- Do not introduce `find*`, `query*`, or `fetch*` as primary CRUD verbs.
- HTTP paths are lowercase snake_case and mirror Java names.
- Use `getStatusByPage(Q)` for status maps and `dispatchRead`/`dispatchWrite` for command dispatch, following existing
  contracts.

### Models and enums

- DOs model database storage; BOs model business semantics; VOs/DTOs model web or transport input/output.
- Persistent write paths accept BOs in services. Controllers and transport adapters convert VO/DTO input to BO.
- Read-only projections may return VOs directly when a duplicate BO would add no business meaning.
- Use MapStruct builders for VO/BO/DO conversion, including enum/index conversion.
- Do not leak database-coded `Byte`, `Integer`, or `String` flags when a domain enum exists or should exist.
- `*FlagEnum` is for boolean-like toggles, `*StatusEnum` for state machines, and `*TypeEnum` for classifications.
- Enum constants use descriptive `UPPER_SNAKE_CASE`; enum `code` values use lowercase tokens.
- Do not introduce magic flag constants such as `private static final Byte DEFAULT = 1`.
- Do not expose secrets in VOs. Exclude `apiKey`, `password`, `secret`, `token`, and credential fields from serialization
  and Lombok `@ToString`.

### Web API and OpenAPI

- Controllers return the standard `R<T>` envelope and never expose DOs.
- Apply grouped validation consistently and keep validation/exception messages in English.
- Document REST endpoints with springdoc annotations; do not maintain a parallel handwritten OpenAPI spec.
- Each business controller package needs the appropriate `GroupedOpenApi` bean, gateway aggregation route, and Swagger
  UI entry.
- Shared WebFlux/springdoc configuration belongs in `dc3-common-web` and must be registered through
  `AutoConfiguration.imports` when component scanning will not discover it.
- Docs are enabled in development-style profiles and disabled in production. Export a running stack with `make openapi`.

### Configuration and logging

- Custom configuration-property prefixes use `dc3.*`.
- Prefer validated, typed `@ConfigurationProperties` over scattered `@Value` fields.
- YAML deployment values use `${ENV:default}` placeholders.
- WebFlux base paths use `spring.webflux.base-path`, not `server.servlet.context-path`.
- Use English, stable event names and parameterized SLF4J messages.
- Prefer structured fields such as `tenantId={}, userId={}, deviceId={}`.
- Never log tokens, passwords, credentials, full request bodies, or raw private payloads at info level.
- Pass caught exceptions to warn/error logs unless stack-trace suppression is intentional.

## Frontend conventions

Frontend code lives in `dc3-web/`. Its executable configuration is the source of truth:

- dependencies, package-manager version, and scripts: `dc3-web/package.json` and `pnpm-lock.yaml`;
- TypeScript behaviour: `dc3-web/tsconfig.json`;
- Vite, proxy, environment, and SCSS behaviour: `dc3-web/vite.config.ts`;
- test configuration: `dc3-web/vitest.config.ts` and `playwright.config.ts`;
- container toolchain pins: `dc3-web/Dockerfile`.

Use pnpm only; do not create npm or Yarn lockfiles. Keep package-manager pins aligned between `package.json` and the
Dockerfile.

Key rules:

- `verbatimModuleSyntax` is enabled. Use `import type` for every type-only import; Vue components, functions, and icons
  remain normal value imports.
- Use `<Entity>Form` for create/update payloads and `<Entity>Record` for read responses.
- Represent Java 64-bit IDs as strings. The backend emits identifiers as JSON strings on the HTTP contract, so standard JSON parsing (no JSONBigInt) is sufficient.
- API wrappers mirror backend cardinality: `getXxx` for one value, `listXxx` for collections/maps/pages, and
  `addXxx`/`updateXxx`/`deleteXxx` for mutations.
- Reuse CRUD helpers from `src/api/common.ts` and API bases from `src/config/constant/api.ts`; keep API wrappers thin.
- Prefer `<script setup>`, Composition API, setup-style Pinia stores, and existing composables.
- Every router-guard branch must settle navigation. Prefer return-style guards for new code and cover guard changes with
  tests.
- Axios interceptors own authentication headers and 401 handling; do not duplicate that logic in feature APIs.
- Vite dotenv files live under `src/config/env/` and use the `APP_` prefix.
- Global Element Plus variables are injected by Vite; do not duplicate their `@use` directives in components.
- Menu changes may require synchronized backend seed data, `settingsNav.ts`, router definitions, i18n locales,
  `Layout.vue`, and `Settings.vue` changes.

Common frontend checks, run from `dc3-web/`:

```bash
pnpm check
pnpm lint:check
pnpm test:guard
pnpm test:ci
pnpm build
make ci
```

Use affected Vitest suites for focused changes and Playwright for browser-level workflows. Coverage thresholds belong in
`vitest.config.ts`; do not duplicate their numbers here.

## Commands

Prefer a root `Makefile` target when one exists. It selects Docker Compose or Podman Compose from the local environment
and centralizes Maven settings.

```bash
make up-db
make up-optional
make up-dev
make package
make test
make test-it
make test-e2e
make coverage
make changelog
```

For direct Maven work, use the checked-in settings file locally:

```bash
mvn -s .mvn/settings.xml -q -DskipTests compile
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager -Dtest=DriverControllerTest
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-public \
  -Dtest="RTest#okWithDataExposesDataAndDefaultEnvelope"
```

When using `-am` together with `-Dtest`, add `-Dsurefire.failIfNoSpecifiedTests=false` so dependency modules without the
selected test do not fail spuriously.

GitHub Actions should normally use public Maven repositories rather than the local mirror settings unless a workflow
is intentionally testing that mirror.

## Environment and Compose

- `.env.example` is the Compose interpolation template.
- `.env` is local and untracked; create it through `make init-env`.
- `dc3/env/dev.env` is for IDE/local Java process variables without `export`.
- `dc3/env/dev.env.sh` is shell-sourceable and uses `export`.
- Do not treat these files as interchangeable.

Canonical Compose files are under `dc3/`:

- `docker-compose.yml`
- `docker-compose-dev.yml`
- `docker-compose-db.yml`
- `docker-compose-optional.yml`

Registry choice is controlled through Make arguments and environment variables, not duplicated Compose files. After a
Compose change, validate every touched stack with its corresponding `make config-*` target or an equivalent
`docker compose ... config` / `podman compose ... config` command.

## Testing and verification

### Test types

- Unit tests (`*Test.java`, `*Tests.java`) run with Surefire, JUnit 5, Mockito, AssertJ, and Reactor `StepVerifier` where
  appropriate. Do not start a Spring context for a test that can use direct construction.
- Integration tests (`*IT.java`) run with Failsafe and may use `dc3-common-test` Testcontainers and harnesses.
- E2E tests live in `dc3-e2e/` and are gated by the `DC3_E2E` environment variable.

Reusable test infrastructure includes `GrpcInProcessExtension`, `RabbitTestHarness`, `PgTimescaleContainer`,
`RabbitContainer`, `MqttContainer`, `FixedClockConfig`, `JsonAssertions`, `EnumContractTest`, and
`SecretFieldContractTest`.

### Coverage

`make coverage` generates the aggregate report under `dc3-coverage/target/site/jacoco-aggregate/`. The current gate is
an absolute minimum configured in `dc3-coverage/pom.xml`; `dc3-coverage/scripts/check_coverage.py` validates the
aggregate XML. Do not claim a relative regression gate unless the build implements one.

### Proportional validation

Run checks proportionate to the change:

- Java/shared behaviour: `mvn -s .mvn/settings.xml -q -DskipTests compile`.
- Tested behaviour: affected unit tests, then `make test` when warranted.
- DAL/SQL or infrastructure integration: affected ITs or `make test-it` with a container runtime.
- gRPC: compile generated sources and run matching client/server contract tests.
- Coverage-sensitive changes: `make coverage` and inspect the aggregate report.
- Changelog tooling: `python3 -m py_compile dc3/bin/changelog.py`.
- Compose: render/validate every touched configuration.
- YAML: parse after accounting for Maven placeholders such as `@project.artifactId@`.
- Agent/docs changes: validate referenced paths, targets, scripts, test selectors, and links.

Report what was verified and what was not verified before handing off public-behaviour changes.

## Release notes

`dc3/doc/CHANGE.md` is generated from Git history. Do not hand-edit the current release block unless fixing generator
output.

```bash
make changelog
make changelog FROM=<previous-tag-or-ref> TO=HEAD VERSION=<version>
```

Commit subjects feed the generated changelog. Reject vague or non-English subjects. A changelog-only commit must use
exactly:

```text
docs(release): update generated changelog
```

Commit that file separately from behaviour, configuration, or tooling changes.

The root `pom.xml` version is the release identity. After committing the version and generated changelog on `main`, run
`make tag`; the script creates only the matching annotated `v<project.version>` tag. It must not calculate a new version
or create a GitHub Release directly. The `Docker Images` workflow owns release verification, image publishing, and
GitHub Release creation. Keep its `release` environment protected with required reviewers and tag restrictions.

## Commit rules

AI coding agents must not commit without explicit confirmation for that specific commit.

Before each commit, present:

- proposed commit message;
- exact files included;
- why those files form one coherent change;
- verification already completed.

Wait for approval before running `git commit`. For multiple commits, present the sequence first and obtain approval for
each commit before creating it.

Use Conventional Commit subjects:

```text
<type>(optional-scope): <english imperative summary>
```

Allowed types are `feat`, `fix`, `perf`, `refactor`, `docs`, `build`, `ci`, `test`, `chore`, `style`, `security`,
`revert`. Use `!` for breaking changes and explain the impact in the body. Keep subjects specific enough for release
notes.

The tracked `.husky/pre-commit` hook runs frontend `lint-staged`. Do not claim commit-message enforcement unless a
tracked commit-msg validation hook is added.

## Editing and documentation

- Preserve AGPL headers where they already exist.
- Prefer existing patterns and helpers over new abstractions.
- Keep public/user-facing project text in English unless editing a localized document.
- Avoid generated metadata churn unless required by the task.
- Use structured parsers or project toolchains for structured files when practical.
- Keep multilingual root READMEs structurally aligned.
- Runtime/environment changes should update the environment guide in `pnoker/iot-dc3-docs`.
- Driver-authoring changes should update the driver-authoring guide in `pnoker/iot-dc3-docs`.
- Test strategy, harness, or coverage changes should update the testing guide in `pnoker/iot-dc3-docs`.
- Release-workflow changes should update `CONTRIBUTING.md` and this file.
