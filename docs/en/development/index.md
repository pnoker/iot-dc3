---
title: Development Overview and Conventions
---

<script setup>
import DevIndexDiagram from '../../.vitepress/theme/components/DevIndexDiagram.vue'
</script>


# Development Overview and Conventions

This page is for developers about to write backend code for IoT DC3. By the end you'll know where the authoritative
conventions live, which naming and layering rules are non-negotiable, and the path from an edit to a commit.

> You are here: ready to extend the platform. Next step depends on your goal — writing a new protocol driver,
> see [Driver Authoring](./driver-authoring); wiring up an API, see [API Documentation](./api-documentation); running
> tests, see [Testing](./testing).

## The authoritative conventions live in `AGENTS.md`

This page is an entry point and quick overview. The real conventions are in **`iot-dc3/AGENTS.md` at the repository root
** — a single source of truth shared across AI tools that covers module layering, Maven commands, the validation
workflow, and commit and changelog rules. `iot-dc3/.claude/CLAUDE.md` just delegates to it and adds nothing. Read
`AGENTS.md` end to end before you start. Where this page and `AGENTS.md` disagree, `AGENTS.md` wins.

The platform is a distributed set of services built on Java 21 / Spring Boot 4 / Spring Cloud 2025. Services coordinate
over gRPC, metadata is persisted in PostgreSQL, and asynchronous messaging runs over RabbitMQ. This stack drives three
conventions you can't work around: CRUD verbs follow the cardinality of the result, cross-service calls go through a
facade, and domain objects are layered as DO/BO/VO.

## CRUD verbs follow the "cardinality of the result"

There's no free naming space here. For every CRUD-shaped method, HTTP path, gRPC RPC, and frontend API function, **the
verb must reflect the cardinality of the returned result** — `get` for a single record, `list` for a collection. This
applies across Service interfaces, ServiceImpl, Controller, Local/gRPC Facade, the gRPC server, and the RPC names in
`.proto`, and it's enforced the same way in both the frontend and backend repos. The payoff: from a method name or a
path alone, you know whether it returns one record or a batch — no need to read the implementation.

| Action        | Java method    | HTTP path   | gRPC RPC  | Frontend function |
|---------------|----------------|-------------|-----------|-------------------|
| Single record | `getXxx(...)`  | `/get_xxx`  | `GetXxx`  | `getXxx(...)`     |
| Collection    | `listXxx(...)` | `/list_xxx` | `ListXxx` | `listXxx(...)`    |
| Create        | `add(BO)`      | `/add`      | n/a       | `addXxx(...)`     |
| Update        | `update(BO)`   | `/update`   | n/a       | `updateXxx(...)`  |
| Delete        | `delete(Long)` | `/delete`   | n/a       | `deleteXxx(...)`  |

The five base methods `add`/`delete`/`update`/`getById`/`list(Q)` come from `BaseService<B, Q>`. Sub-interfaces only add
`getByXxx`/`listByXxx` when they need to query by some dimension, and the verb still has to match the cardinality.
`DeviceController` is a ready-made template — its endpoints are exactly `/add`, `/delete`, `/update`, `/get_by_id` (
single record), `/list_by_ids`, `/list_by_profile_id`, `/list` (collection), with verbs strictly aligned to cardinality.

::: warning Do not misuse these three reserved verbs

- `select*` is only for raw MyBatis Mapper calls inside `*ManagerImpl`, and **never appears** on
  Service/Controller/Facade.
- `remove*` is only for the Manager methods inherited from MyBatis-Plus (`removeById`, `remove(wrapper)`); business
  deletion always uses `delete*`.
- `find*`, `query*`, and `fetch*` are not used as primary CRUD verbs.
  :::

## Layered calls: Controller(VO) → Service(BO) → Manager(DO) / Facade (cross-service)

An incoming request passes through three layers, and each layer knows one data representation. The Controller receives
and returns **VO** (the API shape). The Service interface extends `BaseService<B, Q>` and **works only on BO types** —
business semantics, using domain enums like `EnableFlagEnum`. The Manager/Mapper operates on **DO** — the database
shape, with flags as `Byte`. MapStruct's `*Builder` classes handle conversion between the three representations, and a
DO flag never leaks into a business or response model.

There's a hard boundary here: **when business code needs data from another service, it doesn't touch transport details
directly — it goes through a facade interface.** Controller and Service classes don't bind to any gRPC or REST detail.
They only call the contract interfaces in `dc3-common-facade-api`, and the deployment topology decides whether the
implementation behind them is gRPC (`dc3-common-facade-grpc`) or in-process (`dc3-common-facade-local-*`). Distributed
deployments default to `grpc` (`DC3_FACADE_MODE=grpc`).

<DevIndexDiagram lang="en" />

The dashed line labeled "must go through facade" is the boundary itself. The left half is the in-service VO→BO→DO
drop-through; the right half is any cross-service read or write, which must first be abstracted into a facade contract
and then have its transport picked by configuration. That makes `grpc` (distributed) and `local` (monolith) a pure
deployment-topology choice — no business code changes. For the fields of each layer's objects, enum conversion, and
`*Builder` details, see [Domain Model](../architecture/domain-model).

## The first path: from changing one endpoint to committing

Putting the three conventions together, a typical backend change looks like this. Say you want to add a "count devices
by driverId" API to device management:

::: code-group

```text [layer placement]
1. VO/BO/DO     add fields in dc3-common-model or the corresponding module (if needed), keep MapStruct *Builder in sync
2. Manager      call the Mapper via select* in *ManagerImpl (select* allowed only here)
3. Service      add getCountByDriverId(...) to the Service interface, implement it in ServiceImpl, touch BO only
4. cross-service?  if you need data from another center, go through the *Facade interface, never connect to gRPC directly
5. Controller   GET /get_count_by_driver_id —— use the get verb to query a single value
```

```bash [validation]
# Fast compile check (always run after changing Java/shared behavior)
mvn -s .mvn/settings.xml -q -DskipTests compile

# Full package (choose proportional to the change before committing)
mvn -s .mvn/settings.xml clean package

# Changed DAL/SQL: integration tests that require a container runtime
make test-it
```

:::

Write the code, run validation, then commit.

## Commit conventions: Conventional Commits

Commit messages become release notes directly (`CHANGE.md` is generated from git history), so the subject has to be
specific and readable. The format is fixed:

```text
<type>(optional-scope): <english imperative summary>
```

- Write the subject in **English, lowercase, imperative mood**, specific enough to read well in `CHANGE.md`.
- Allowed types: `feat`, `fix`, `perf`, `refactor`, `docs`, `build`, `ci`, `test`, `chore`, `style`, `security`,
  `revert`.
- Prefer a scope for any non-trivial change outside the root. For breaking changes, use `!` and explain the impact in
  the body.
- Skip weak subjects like `update`, `fix bug`, `change code`, `misc`, `wip`, or `.`.

Real examples:

```text
feat(agentic): add session cleanup policy
fix(manager): validate tenant scope for device queries
docs(env): explain JetBrains IDEA environment variables
```

::: warning Hard rules before committing

- AI collaboration agents **must not create a commit without explicit confirmation**. Before committing, show the
  proposed commit message and the files to be included, and wait for approval.
- Don't stuff unrelated changes into one commit — split by intent (feature, fix, and refactor each get their own
  commit).
- The release-notes commit is fixed as `docs(release): update generated changelog`, and `CHANGE.md` is committed
  separately.
- Before committing, check your commit message against the format and examples above; non-conforming formats are
  rejected by CI before merge.
  :::

## Further reading

- [Domain Model](../architecture/domain-model) — fields of each DO/BO/VO layer, enum conversion, and MapStruct
  `*Builder` details
- [Driver Authoring](./driver-authoring) — copy the `dc3-driver-virtual` template to extend a new protocol driver,
  implementing `DriverCustomService`
- [API Documentation](./api-documentation) — how OpenAPI/Swagger is exposed, the auth header, and the export workflow
- [Testing](./testing) — conventions for unit, integration, E2E, and coverage
