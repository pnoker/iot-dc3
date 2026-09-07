# Backend conventions

Read this when changing Java modules, REST/gRPC contracts, or drivers. Entry point and cross-cutting rules:
[`AGENTS.md`](../../AGENTS.md).

## Layering

Business modules follow this flow:

```text
Controller (WebFlux) -> Service (BO) -> Reactive repository (R2DBC)
```

- Controllers implement the `BaseController` interface and return typed `Mono<T>`/`Flux<T>` payloads.
- Keep the request path non-blocking; use Reactor-native R2DBC publishers end to end.
- Services extend `BaseService<B, Q>`, work in business objects, and own business rules.
- Reactive repositories use `DatabaseClient`/`R2dbcEntityTemplate` and explicit tenant predicates.
- Do not expose persistence objects from controllers or facade contracts.

Common types:

| Type                          | Module              | Role                                                          |
|-------------------------------|---------------------|---------------------------------------------------------------|
| `BaseService<B,Q>`            | `dc3-common-public` | base CRUD service contract                                    |
| `BaseController`              | `dc3-common-web`    | reactive controller helpers and user/tenant context           |
| RFC 9457 problem details      | `dc3-common-web`   | standard HTTP error body (`application/problem+json`)      |
| `BaseBO`, `BaseVO`, `BaseDTO` | `dc3-common-model`  | shared business, web, and transfer fields                     |
| `BaseBuilder`                 | `dc3-common-model`  | MapStruct conversion base                                     |
| `TenantOwned`                 | `dc3-common-public` | marker for tenant-scoped entities                             |

## Shared code placement

Place code according to ownership and dependency direction, not Java visibility. A `public` method is not automatically
shared infrastructure.

- Put framework-neutral contracts, request/pagination/tree entities, tenant markers, and broadly reusable utilities in
  `dc3-common-public`.
- Put platform-wide constants and top-level shared domain/wire/persistence enums in `dc3-common-constant`.
- Put BO/VO/DTO bases, builders, validation groups, extensions, and shared transport models in `dc3-common-model`; those
  models may reference enums owned by `dc3-common-constant`.
- Keep framework- or capability-specific public helpers in the narrowest owning module, such as gRPC conversion in
  `dc3-common-api`, WebFlux helpers in `dc3-common-web`, and broker adapters in the `dc3-mq-*` family.
- Keep constants and nested enums used by only one module, protocol, configuration object, or implementation beside that
  owner. Reserve top-level `*Constant` classes and top-level public enums for `dc3-common-constant`; use a
  concern-specific local name such as `*Limits`/`*Defaults`, a nested enum, or a private field until the concept becomes
  a stable cross-module contract.
- Do not duplicate cross-module wire names, header names, routing identifiers, cache-key fragments, or persistence
  codes. Define one canonical symbol in `dc3-common-constant` and migrate callers together.
- Preserve the dependency floor: `dc3-common-constant` must not depend on other DC3 modules, and `dc3-common-public`
  must not depend on capability modules.

## Tenant safety

Tenant isolation is a hard requirement.

- Preserve tenant scope in every new query and mutation.
- Carry tenant IDs through gRPC requests whenever the contract supports them.
- Include tenant context in cache keys for tenant-owned data.
- Validate ownership before returning or mutating data across service boundaries.
- Do not add `tenantId IS NULL` shortcuts unless the data model explicitly defines global records.
- Treat missing tenant validation as a correctness and security defect, not a convenience trade-off.

## Facade boundaries

Business code must use facade interfaces for cross-service calls.

- Contracts belong in `dc3-common-facade-api`.
- Transport-backed implementations belong in `dc3-common-facade-grpc`.
- In-process implementations belong in the matching module:
  `dc3-common-facade-local-auth`, `dc3-common-facade-local-data`, or
  `dc3-common-facade-local-manager`.
- `dc3-common-facade-local` is a dependency aggregator and does not contain implementation sources.
- Keep controllers and services independent from transport details unless they are explicit transport adapters.

## gRPC contracts

Proto files live under `dc3-api/*/src/main/protobuf`.

When changing a contract:

1. Update the `.proto` file.
2. Compile the affected API module to regenerate sources.
3. Update server implementations and client builders/stubs together.
4. Preserve backward compatibility where practical.
5. Verify tenant propagation and standard gRPC status errors.

Servers are Spring beans extending generated `*ImplBase` classes. Reuse shared stub configuration; do not construct ad
hoc channels in business code.

## Driver SDK

Drivers implement protocol behaviour through the SPI types in `dc3-common-driver`; shared runtime services handle
registration, scheduling, and value dispatch.

Primary extension points are `DriverProtocol`, `DriverLifecycle`, `DriverMetadataListener`, `DriverHealth`,
`DeviceHealth`, and `DriverCommand`. Prefer existing SDK plumbing over driver-specific infrastructure.

Driver `application.yml` metadata is user-facing:

- Keep `name`, `attribute-name`, and `remark` in English.
- Treat driver `code` values as routing-stable identifiers. Changing one requires a metadata and RabbitMQ migration
  plan.

## CRUD verbs

CRUD-shaped names reflect result cardinality across Service, Controller, Facade, gRPC server, and proto RPCs:

| Action       | Java           | HTTP        | gRPC      |
|--------------|----------------|-------------|-----------|
| create one   | `add(BO)`      | `/add`      | n/a       |
| delete by ID | `delete(Long)` | `/delete`   | n/a       |
| update one   | `update(BO)`   | `/update`   | n/a       |
| return one   | `getXxx(...)`  | `/get_xxx`  | `GetXxx`  |
| return many  | `listXxx(...)` | `/list_xxx` | `ListXxx` |

- Base CRUD comes from `BaseService<B,Q>`: `add`, `delete`, `update`, `getById`, and `list(Q)`.
- Reserve `select*` for raw store persistence operations.
- Do not introduce `find*`, `query*`, or `fetch*` as primary CRUD verbs.
- HTTP paths are lowercase snake_case and mirror Java names.
- Use `getStatusByPage(Q)` for status maps and `dispatchRead`/`dispatchWrite` for command dispatch, following existing
  contracts.

## Models and enums

- DOs model database storage; BOs model business semantics; VOs/DTOs model web or transport input/output.
- Persistent write paths accept BOs in services. Controllers and transport adapters convert VO/DTO input to BO.
- Read-only projections may return VOs directly when a duplicate BO would add no business meaning.
- Use MapStruct builders for VO/BO/DO conversion, including enum/index conversion.
- Do not leak database-coded `Byte`, `Integer`, or `String` flags when a domain enum exists or should exist.
- `*FlagEnum` is for boolean-like toggles, `*StatusEnum` for state machines, and `*TypeEnum` for classifications.
- Enum constants use descriptive `UPPER_SNAKE_CASE`; enum `code` values use lowercase tokens.
- Do not introduce magic flag constants such as `private static final Byte DEFAULT = 1`.
- Do not expose secrets in VOs. Exclude `apiKey`, `password`, `secret`, `token`, and credential fields from
  serialization and Lombok `@ToString`.

## Web API and OpenAPI

- Controllers return typed payloads and never expose DOs; errors use RFC 9457 problem details.
- Apply grouped validation consistently and keep validation/exception messages in English.
- Document REST endpoints with springdoc annotations; do not maintain a parallel handwritten OpenAPI spec.
- Each business controller package needs the appropriate `GroupedOpenApi` bean, gateway aggregation route, and Swagger
  UI entry.
- Shared WebFlux/springdoc configuration belongs in `dc3-common-web` and must be registered through
  `AutoConfiguration.imports` when component scanning will not discover it.
- Docs are enabled in development-style profiles and disabled in production. Export a running stack with `make openapi`.

## Configuration and logging

- Custom configuration-property prefixes use `dc3.*`.
- Prefer validated, typed `@ConfigurationProperties` over scattered `@Value` fields.
- YAML deployment values use `${ENV:default}` placeholders.
- WebFlux base paths use `spring.webflux.base-path`, not `server.servlet.context-path`.
- Use English, stable event names and parameterized SLF4J messages.
- Prefer structured fields such as `tenantId={}, userId={}, deviceId={}`.
- Never log tokens, passwords, credentials, full request bodies, or raw private payloads at info level.
- Pass caught exceptions to warn/error logs unless stack-trace suppression is intentional.
