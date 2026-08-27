# Design: Relational Access Layer on Spring Data R2DBC (per-dialect, per-service modules)

|                |                                                                                                                              |
|----------------|------------------------------------------------------------------------------------------------------------------------------|
| **Status**     | Approved 2026-08-28 — §5 decisions confirmed (D14 deferred to the P1 TCK spike; D18/D19 added at first review)                  |
| **Date**       | 2026-08-28                                                                                                                    |
| **Scope**      | relational persistence layer: `dc3-common-auth` + `dc3-db` family first; `dc3-common-manager` / `dc3-common-data` follow        |
| **Target**     | replace MyBatis-Plus + JDBC with Spring Data R2DBC; end-to-end reactive; GraalVM native-image ready                            |
| **Supersedes** | `storage-abstraction.md` §3 (relational dialect mechanism — "MyBatis is the port"); its TSDB half already moved to `tsdb-abstraction.md` |
| **Related**    | `mq-abstraction.md`, `tsdb-abstraction.md` — the family pattern this design extends to the relational layer                    |

## 1. Motivation

Three drivers, in priority order:

1. **GraalVM native images.** `native-maven-plugin` 1.1.10 is already managed in the root POM, but MyBatis-Plus blocks the
   path: runtime mapper proxies, reflection-driven bean wiring, and jsqlparser-based SQL rewriting are all hostile to
   closed-world compilation. Spring Data R2DBC ships native support (hints built in since Boot 3; drivers are
   Netty-based). The dependency swap is the enabling step for native builds of every center service.
2. **True end-to-end reactive.** Controllers are WebFlux today, but Service → Manager → Mapper is synchronous blocking,
   bridged by `BaseController.async(...)` onto `Schedulers.boundedElastic()` with manual `TenantContextHolder`
   set/clear. A reactive persistence layer removes the bridge, the thread-pool hop, and the ThreadLocal lifecycle
   hazard in one move.
3. **Dialect freedom.** The current dialect mechanism is MyBatis `databaseId` forks inside shared XML (42 statements
   across 6 files), gated by a "portable-first" discipline. R2DBC has no `databaseId`; the natural shape is one
   implementation module per dialect holding native SQL — the same contract/adapter/TCK structure the MQ and TSDB
   families already use (`RepositoryService` port in `dc3-common-repository`, adapters in `dc3-tsdb-*`, TCK in
   `dc3-tsdb-tck`).

Weighting, stated plainly: driver 1 is the hard requirement. Driver 2, for auth itself, is an architectural-consistency
investment — auth is a management plane (login/CRUD, human-driven QPS) and no latency or throughput gain is claimed from
removing the boundedElastic hop; its payoffs (native image, no ThreadLocal lifecycle hazard, readiness for the
high-concurrency data/manager wave) arrive later. The cost — rewriting 12 services plus the OAuth/MCP runtime as
reactive chains, with harder debugging — is accepted and bounded by the P2/P3 go/no-go gate, the TCK, and e2e.

What does **not** change: deploy-time selection via `dc3.db.type` (postgres | mysql | mariadb), the initdb seed SQL
(executed by container entrypoints, independent of the access layer), table schemas, and the HTTP/gRPC surface.

## 2. Goals / Non-goals

**Goals**

- auth center is the pilot: fully R2DBC, zero MyBatis on its classpath, native image compiles and boots.
- One module per (dialect × service), holding that dialect's native SQL — no shared XML, no `databaseId`.
- Repository behavior TCK: same suite green on PostgreSQL, MySQL, MariaDB via Testcontainers.
- Tenant isolation preserved with **stronger** guarantees than today (see §6).
- gRPC contracts unchanged; HTTP contracts unchanged **except** the deliberately redesigned pagination
  envelope (D8 — nested `page` object; one-step cutover, no compatibility window).

**Non-goals**

- Runtime hot-swap of the database engine (unchanged: deploy-time selection, restart to switch).
- Rewriting manager/data in this track — they follow the same pattern later; their MyBatis stack is untouched.
- MyBatis-Plus feature parity. Only the features actually used by auth are rebuilt (inventory in §3).
- ORM-level schema management. initdb stays authoritative; no Flyway/Liquibase introduced.

## 3. Current-state facts that shape the design

Verified inventory (2026-08-28), each fact constrains a decision:

| # | Fact | Consequence |
|---|------|-------------|
| F1 | Of 16 Manager interfaces in `dc3-common-auth`, **15 are empty** (`extends IService<XxxDO>`, zero methods). Only `IdentityAuditLogManager` has a custom method. | The contract is a blank page — define it from what Services actually need, not from `IService`. |
| F2 | Service layer leans on `IService` + `LambdaQueryWrapper` (fuzzy queries, `page(...)`, `getOne(wrapper)`, `wrapper.apply("1 = 0")`, two-step membership→`IN principalIds` tenant filtering for tenant-less tables). | Dynamic-query logic moves into named repository methods taking the existing Query BOs (D7). |
| F3 | `OAuthMcpMapper` is a plain hand-written mapper (30 methods, no `BaseMapper`), injected **directly by biz layer** (`OAuthMcpRuntimeServiceImpl`), writing 7 tables; all `databaseId` forks for auth live in its XML. `ResourceRegistryLockMapper` (advisory lock) is also hand-written. | Already repository-shaped; migration is SQL relocation + reactive signatures. It is the largest single aggregate (D13). |
| F4 | `TenantContextHolder` is ThreadLocal (in `dc3-common-constant`); written in exactly 3 entry kinds: `BaseController.async`, local facades, gRPC servers (manual set/clear). Consumer is `TenantLineHandlerImpl` (fail-closed: no tenant → `TenantNotScopedException`). Hand-written XML SQL already carries explicit `tenant_id = #{tenantId}` — the interceptor only covers wrapper paths. | Explicit tenant parameters in the new contract are a continuation of the existing pattern, not new discipline (D5). ThreadLocal stays for MyBatis-era modules only. |
| F5 | 11 tables have no `tenant_id` column (whitelist in `TenantLineHandlerImpl`): `dc3_tenant`, `dc3_principal`, `dc3_user`, `dc3_local_credential`, `dc3_external_identity`, `dc3_resource`, `dc3_role_resource_bind`, `dc3_api`, `dc3_mcp_tool_catalog`, `dc3_mcp_connection_tool`, `dc3_menu`. | Tenant-less repositories are named as such in the contract; no runtime whitelist needed (D6). |
| F6 | IDs are application-side snowflake everywhere: `@TableId(type = ASSIGN_ID)` (60+ DOs) plus explicit `IdWorker.getId()` in the OAuth/MCP path. No DB sequences. | Swap in a small first-party snowflake generator; no schema change (D10). |
| F7 | Logical delete via `@TableLogic` (47 DOs, `deleted` flag; partial unique indexes like `... WHERE deleted = 0`). | Explicit `deleted = 0` predicates in implementations; contract documents the semantics (D9). |
| F8 | `create_time` / `operate_time` are purely DB-side (PG trigger functions / MySQL `ON UPDATE CURRENT_TIMESTAMP`); updates deliberately null out `operate_time` and re-read. | Inserts exclude time columns; keep re-read pattern; zero schema change (D11). |
| F9 | 19 `@Transactional(rollbackFor = Exception.class)` sites in auth (9 in `OAuthMcpRuntimeServiceImpl`, 3 in `ResourceRegistrySyncServiceImpl`, 3 in `MenuServiceImpl`, 3 in `ServiceAccountServiceImpl`). Plus one unmanaged 3-table write in `UserController.add/delete` (principal → user → membership) — a pre-existing defect. | Reactive transactions with `R2dbcTransactionManager`; fix the UserController gap during migration (D12). |
| F10 | Pagination is one uniform pattern: `Page<DO>` + `PageUtil.page(Pages)` in 12 ServiceImpls; HTTP returns MyBatis-Plus `Page<VO>` (a leaked framework type — internal fields serialize into the JSON); gRPC uses proto `GrpcPage` (data/driver contracts only — auth's gRPC surface has no pagination); OAuth/MCP does manual `limit/offset` + `count`. | Redesign the envelope (D8): frontend consumption is funneled through `PageResult` in `types/common.ts` + `usePagedList`/`useEntityListPage` + `mock/response.ts`, reading only `records/total/size/current` — one coordinated frontend switch. |
| F11 | SQL exceptions have no dedicated translation; business uniqueness is check-then-insert (`getOne` → `DuplicateException`); fallback advice returns 500. | Keep check-then-insert as primary; add a thin `R2dbcException` mapping helper (D17). |
| F12 | `dc3-common-dal` (label/group tables) is only used by manager; auth merely imports `DictionaryBO` from it, and `DictionaryForAuthService` reads `dc3_tenant` via `TenantManager`. data/agentic depend on dal in POM but reference it zero times in code. | auth pilot does **not** touch `dc3-common-dal`. Scope shrinks. |
| F13 | Datasource per center service is a single `master` route (dynamic-datasource), URL `currentSchema=dc3_auth` (PG) / per-service database (MySQL); Hikari max 64. | R2DBC needs one connection factory + `r2dbc-pool`; dynamic-datasource drops out of auth entirely. |
| F14 | Selection mechanism today: `dc3.db.type` → `@ConditionalOnProperty` per dialect jar + EnvironmentPostProcessor profile activation; adapter jars are opt-in Maven deps (auth ships postgres by default; mysql/mariadb only in TCK). | Same mechanism reused; no new selection concept (D3). |
| F15 | Existing `dc3-db-{postgres,mysql,mariadb}` jars carry MyBatis-specific config (pagination `DbType`, timestamptz TypeHandler, driver-class-name). manager/data still need them during transition. | Leave the existing dialect modules untouched; R2DBC config lives in the new per-service dialect modules (D3). |

## 4. Target architecture

```text
dc3-common/dc3-common-auth
│   contract: io.github.pnoker.common.auth.repository.*   (plain reactive interfaces, Mono/Flux)
│   entities: domain DOs (no ORM annotations required by the contract)
│   services/biz: rewritten as reactive chains on the contract
│
dc3-db/dc3-db-r2dbc-core            ← new, pure types, ZERO Spring/R2DBC deps: snowflake IDs, PageResult/PageInfo,
│                                      R2dbcException mapping — safe for the contract module to depend on (D2/D4)
├── dc3-db-r2dbc-boot               ← new: shared R2DBC auto-config helpers (ConnectionFactory/pool config,
│                                      D19 codec, observability); spring-data-r2dbc compiles only here
├── dc3-db-auth-postgres            ← Pg auth repositories: Spring Data + @Query native SQL,
│                                      application-auth-postgres.yml (r2dbc URL, pool), auto-config
├── dc3-db-auth-mysql               ← ditto, MySQL dialect
├── dc3-db-auth-mariadb             ← ditto, MariaDB dialect
└── dc3-db-tck                      ← extended: repository behavior contracts × 3 engines
        (existing dc3-db-core / dc3-db-{postgres,mysql,mariadb} untouched — they keep serving
         manager/data's MyBatis stack until their own migration)
```

Dependency direction: `dc3-db-auth-{dialect} → dc3-common-auth (contract) + dc3-db-r2dbc-core (pure types) + dc3-db-r2dbc-boot + spring-data-r2dbc + dialect r2dbc driver`. The contract module depends only on `dc3-db-r2dbc-core` and never sees Spring Data, driver types, or MyBatis — the core/boot split is what keeps D2 true while D4 still lets contracts use `PageResult`.

Selection reuses F14 exactly: each `dc3-db-auth-{dialect}` auto-configuration is `@ConditionalOnProperty(prefix = "dc3.db", name = "type", havingValue = "{dialect}")` (postgres `matchIfMissing = true`), contributes its `ConnectionFactory`-based config via its own profile yml, and a missing-adapter mismatch fails fast at boot — same as `MybatisPlusConfig` does today.

## 5. Decision checklist

Each entry: context → options → **recommendation** → consequence. Status column is the review tracker.

| # | Decision | Recommendation | Status |
|---|----------|----------------|--------|
| D1 | **Contract home** — where repository interfaces live. Options: (a) `dc3-common-auth` `repository` package; (b) `dc3-db-core` as a contract hub. | **(a)** — follows the TSDB precedent (`RepositoryService` lives in `dc3-common-repository`, adapters depend on it). Keeps `dc3-db-core` from becoming a business omnibus; implementation → contract dependency is clean. | **Confirmed 2026-08-28** |
| D2 | **Contract style** — plain `Mono`/`Flux` interfaces vs extending Spring Data repository types. | **Plain interfaces, zero framework types.** Dialect modules may internally extend `ReactiveCrudRepository` and adapt. TCK tests the contract, not Spring Data. | **Confirmed 2026-08-28** |
| D3 | **Module layout** — nested under existing dialect modules vs flat under `dc3-db`; if flat, which naming axis. | **Flat, service-first**: `dc3-db/dc3-db-auth-{postgres,mysql,mariadb}` (later `dc3-db-manager-*`, `dc3-db-data-*`). Service-first because day-to-day work opens the three dialect implementations of one contract side by side, `dc3-center-auth`'s dependency list shows a single prefix, and TCK contracts are organized per business module. Existing `dc3-db-*` modules untouched (F15). Nesting rejected: `dc3-db-postgres` is a jar serving a retiring MyBatis transition and would need aggregator/inheritance contortions. | **Confirmed 2026-08-28** |
| D4 | **Shared R2DBC infrastructure** — new `dc3-db-r2dbc-core` vs scattering helpers. | **Two thin modules, split by dependency weight**: `dc3-db-r2dbc-core` holds pure types only — first-party snowflake (MP-compatible shape: timestamp+worker+sequence, so IDs stay trend-ordered across the transition), `PageResult<T>`/`PageInfo`, `R2dbcException` → business exception mapping — with zero Spring/R2DBC deps, so the contract module may depend on it without violating D2; `dc3-db-r2dbc-boot` holds the Spring-dependent shared helpers (ConnectionFactory/pool auto-config conventions, custom conversions, D19 codec, observability) where spring-data-r2dbc is a compile dep. No MyBatis anywhere. | **Confirmed 2026-08-28** (amended: core/boot split so D2 and D4 stop contradicting) |
| D5 | **Tenant passing** — explicit `tenantId` parameters vs Reactor Context propagation vs keeping ThreadLocal. | **Explicit parameters** (first argument, `Long tenantId`). Compile-time fail-closed — stronger than today's runtime exception; continues what hand-written SQL already does (F4); kills the ThreadLocal lifecycle hazard on the R2DBC path. All three entry kinds already hold `tenantId` explicitly (Controller via security context, gRPC via request field, local facade via parameter) — the change deletes the ThreadLocal detour rather than adding parameter threading. `TenantContextHolder` remains untouched for manager/data's MyBatis stack. | **Confirmed 2026-08-28** |
| D6 | **Tenant-less surfaces** — how `runIgnore`/whitelist semantics carry over. | **Named method variants** on tenant-less repositories (F5 tables): no `runIgnore`, no global whitelist. System/login paths call methods whose names say they are unscoped (e.g. `findByLoginName`), and every tenant-scoped method simply does not exist without a tenant argument. | **Confirmed 2026-08-28** |
| D7 | **Dynamic queries** — port `LambdaQueryWrapper` semantics how? | **Named repository methods per Query BO** (`Mono<PageResult<UserDO>> listByQuery(Long tenantId, UserQuery q)`); each dialect renders its own SQL. No criteria-builder port — the 12 fuzzyQuery methods are finite, reviewable SQL. | **Confirmed 2026-08-28** |
| D8 | **Pagination** — neutral type and envelope shape. | **Redesigned, one-step cutover, no compatibility window (not MP-compatible): nested page object.** `PageResult<T> = { records: T[], page: PageInfo }`, `PageInfo = { current, size, total, pages }`, both in `dc3-db-r2dbc-core` (D4). Why nested instead of a flat MP-shaped neutral type (flat would be zero-cost today but pays forever): a paged response keeps exactly two top-level keys — payload and metadata — so future pagination metadata (sort echo, approximate-total flags, cursors) extends `PageInfo` without ever touching the payload or accreting new top-level fields, which is precisely the accretion mechanism that leaked MP's internal fields in the first place (F10); `PageInfo` is reusable wherever only counts matter; frontend gets one generic `PageResult<T>` with typed metadata. `orders` are not echoed back (the caller knows what it sent). `PageUtil`'s clamping/default-ordering moves into the repository layer. Why no compatibility layer: the break is paid exactly once — a dual-format window would keep the leaked shape alive through the manager/data migrations and force a second break later; the shape changes here, at the moment the persistence layer is rewritten anyway. Bounded cutover: dc3-web (four funnel touchpoints — `types/common.ts`, `usePagedList`, `useEntityListPage`, `mock/response.ts` — now reading `records` + `page.*`), openapi snapshots, and e2e switch in the same change set; external REST integrators get a documented breaking change via release notes and regenerated OpenAPI. auth's gRPC contracts carry no pagination, so protos are untouched. `PageResult`/`PageInfo` become the project-wide envelope as manager/data migrate. | **Confirmed 2026-08-28** (re-review: one-step cutover, compatibility explicitly rejected) |
| D9 | **Logical delete** — how `@TableLogic` semantics survive. | **Explicit SQL predicates** (`deleted = 0` in every read, `SET deleted = 1` in deletes) + Javadoc on each contract method stating whether deleted rows are filtered. Partial unique indexes (`WHERE deleted = 0`) continue to enforce uniqueness. | **Confirmed 2026-08-28** |
| D10 | **ID generation** — MP `IdWorker` is unavailable post-migration. | **First-party snowflake in `dc3-db-r2dbc-core`**, same bit layout as MP `ASSIGN_ID`; called explicitly before insert (the `OAuthMcpRuntimeServiceImpl` pattern already does exactly this — generalize it). **WorkerId allocation is a correctness invariant, not a detail**: explicit env assignment (`DC3_DB_WORKER_ID`-style; StatefulSet ordinal / fixed per replica), deterministic IP-hash fallback for single-node dev, and startup detection that refuses a duplicate workerId+datacenter pair within one deployment; the TCK adds a two-instance uniqueness contract. The assigned space must also not collide with the MP `IdWorker` instances still writing the same tables from `dc3-center-single` during the dual-stack transition. | **Confirmed 2026-08-28** (amended: workerId allocation rule) |
| D11 | **Timestamps** — who writes `create_time`/`operate_time`. | **DB stays authoritative** (F8): inserts exclude both columns, updates never set `operate_time`, business code re-reads after update where it needs the trigger value. Zero schema change, zero behavior change. | **Confirmed 2026-08-28** |
| D12 | **Transactions** — reactive transaction strategy. | **`@Transactional(rollbackFor = Exception.class)` on reactive methods + `R2dbcTransactionManager`** auto-configured per dialect module. All 19 existing sites carry over (F9); the unmanaged `UserController.add/delete` 3-table write is wrapped in a service-level transaction as part of the migration (defect fix, noted in §11). | **Confirmed 2026-08-28** |
| D13 | **OAuth/MCP surface split** — one `OAuthMcpRepository` mirroring the 30-method mapper vs per-aggregate repositories. | **Per-aggregate split**: `OAuthClientRepository`, `OAuthAuthorizationRepository`, `McpConnectionRepository` (+ `dc3_mcp_connection_tool` — `replaceConnectionTools` is a connection aggregate operation), `McpToolCatalogRepository`, `McpAuditLogRepository` (+ `dc3_mcp_tool_confirmation`; exact home decided at implementation by the aggregate-root rule). The 7 tables (F3) map to 5 focused contracts; dialect-forked statements (JSON casts, upsert idioms) land in each dialect module. Reactive transactions bind to the `ConnectionFactory`, not the repository, so biz-level `@Transactional` composes across repositories freely. | **Confirmed 2026-08-28** |
| D14 | **MySQL driver** — `r2dbc-mysql` (community) vs `r2dbc-mariadb` (official) against MySQL servers. | **Decided by TCK spike, both candidates wired**: MySQL has no official R2DBC driver; the community driver is well-regarded but volunteer-maintained, while the official MariaDB driver also speaks the MySQL protocol (caveats: `caching_sha2_password`, JSON codec). Gate: whichever passes the full TCK on `mysql:8.4` stays; record the rationale here. MariaDB dialect uses `r2dbc-mariadb` (no contest). | Pending — P1 spike |
| D15 | **Query style inside dialect modules.** | **Spring Data interfaces + `@Query` native SQL as the default; `DatabaseClient` for batch upserts and connection-bound operations** (advisory locks — see §8). Free choice per statement, invisible above the contract (D2). | **Confirmed 2026-08-28** |
| D16 | **Coexistence & cutover** — MyBatis removal timing for auth. | **Hard cutover per service**: auth ships R2DBC-only (drops `dc3-db-core`, dynamic-datasource, `mapping/*.xml`, `@MapperScan`); `dc3-center-single` runs both stacks against the same database until manager/data migrate (two pools during transition — accepted, bounded). | **Confirmed 2026-08-28** |
| D17 | **Error translation** — SQL exception mapping. | **Thin helper in `dc3-db-r2dbc-core`**: `R2dbcDataIntegrityViolationException` → existing `DuplicateException`/`BusinessException` mapping, applied in dialect implementations at natural points. Check-then-insert stays the primary UX path (F11); the mapping is a backstop so constraint violations no longer surface as 500s. | **Confirmed 2026-08-28** |
| D18 | **Facade & gRPC server boundary** — auth's non-HTTP seams, absent from earlier drafts. Auth services are also exposed via 7 blocking facade contracts (`TokenFacade.checkValid` returns `boolean`, plus Permission/User/Tenant/LocalCredential/ResourceRegistry/McpRuntime), 7 gRPC servers (`grpc/*Server.java` — `StreamObserver` callbacks wrapping services in `TenantContextHolder.runIgnore`), and the local facade impls; consumers are the gateway (blocking gRPC stub) and every WebFlux service's security chain via `FacadePermissionProvider`. Options: (a) make facade contracts reactive now; (b) keep blocking contracts and bridge explicitly. | **(b) — blocking edge, explicit and temporary.** Facade contracts stay synchronous in this track; the reactive→blocking bridge lives only in auth's gRPC servers and local facade impls, executed on gRPC executor / boundedElastic threads (never an event loop — guard against event-loop `block()` errors), and `runIgnore` wrappers become unscoped method variants (§6). This keeps the pilot's blast radius bounded — option (a) would drag the gateway `FilterServiceImpl`, `dc3-common-facade-grpc`, and shared `dc3-common-web` into the auth track. "True end-to-end reactive" holds controller→repository inside auth; the edge stays synchronous by decision until the facade wave lands with manager/data, at which point `TokenGrpcFacade`, `FilterServiceImpl`, and `FacadePermissionProvider` simplify. In-process gRPC tests keep the blocking `GrpcInProcessExtension` harness. | **Confirmed 2026-08-28** |
| D19 | **Timestamp codec** — who converts `timestamptz` ↔ `LocalDateTime` (successor to `TimestamptzLocalDateTimeTypeHandler`). The R2DBC PostgreSQL driver maps `timestamptz` natively to `OffsetDateTime`, not `LocalDateTime`; MySQL/MariaDB `DATETIME` needs no conversion. | **First-party `R2dbcCustomConversions` in `dc3-db-r2dbc-boot`** (D4 split): registers UTC converters (OffsetDateTime ↔ LocalDateTime) for the PG dialect, preserving F8/D11 semantics and the §9 round-trip contract; MySQL/MariaDB modules use default mappings. Decided explicitly because it is the top R2DBC-PG migration pitfall and would otherwise surface as a red TCK item with no owner. | **Confirmed 2026-08-28** |

## 6. Tenant isolation on R2DBC

The contract makes tenant scope a **type-level property**:

- Every repository for a tenant-owned table exposes only `tenantId`-carrying methods — code that lacks a tenant cannot
  compile a query, which upgrades today's runtime `TenantNotScopedException` to a compile-time guarantee.
- Repositories over the 11 tenant-less tables (F5) have no tenant parameters at all; cross-tenant reads on those tables
  (e.g. user → memberships) go through explicit two-step service logic exactly as today.
- System paths (startup sync, expiry scanners, login-before-context, MCP runtime) use unscoped method variants with
  explicit names — replacing `TenantContextHolder.runIgnore(...)` threading on the auth path.
- TCK ships **negative tests**: cross-tenant reads must return empty/absent on every list/get; tenant-less repositories
  must not gain tenant filters; constraint violations on cross-tenant writes stay mapped (D17).

`TenantContextHolder`, `TenantLineHandlerImpl` and the tenant interceptor are **not modified** — they keep serving
manager/data until their migration removes them.

## 7. Repository contract conventions

- Package `io.github.pnoker.common.auth.repository`; one interface per aggregate; parameter objects are existing
  BOs/Query objects and domain DOs.
- Method naming follows the project's CRUD verb policy (`add/delete/update/getById/list...`; `select*` reserved for
  persistence-flavored reads — same rule as AGENTS.md, applied to the new layer).
- Return shapes: `Mono<X>` for 0..1, `Flux<X>` for many, `Mono<PageResult<X>>` for paged; writes return `Mono<Void>`
  or the re-read entity where the business needs DB-computed values (F8 pattern).
- Transactions are declared in services/biz (as today, F9); repositories stay transaction-unaware.
- No ORM annotations are required on DOs by the contract; dialect modules may keep private `@Table` row classes and
  map to domain DOs, or annotate shared DOs directly if clean — decided per aggregate during implementation, TCK is
  indifferent (D2 keeps this an implementation detail).

## 8. Dialect implementation conventions

Per dialect module (`dc3-db-auth-postgres` as the reference):

- **Config**: `application-auth-{dialect}.yml` providing `spring.r2dbc.*` (URL template from the same
  `DC3_DB_*` env family — PG `options=search_path=dc3_auth`, URL-encoded, same search-path semantics as today's
  JDBC URL; MySQL/MariaDB per-service database) and `r2dbc-pool` sizing equivalent to today's Hikari settings (F13).
  Profile activated by the module's EnvironmentPostProcessor, mirroring F14. Observability parity is part of the
  config contract: r2dbc-pool Micrometer metrics and a connection health indicator, registered by
  `dc3-db-r2dbc-boot`, replacing Hikari's metrics.
- **Standard CRUD**: Spring Data repository interfaces extending `ReactiveCrudRepository` where the shape fits.
- **Dialect-locked statements** — the statements that motivated per-dialect modules. Each dialect module writes its
  native form; the table below is the migration inventory (auth-relevant):

  | Concern | PostgreSQL | MySQL 8 | MariaDB | Note |
  |---------|------------|---------|---------|------|
  | Upsert | `INSERT ... ON CONFLICT ... DO UPDATE` | `ON DUPLICATE KEY UPDATE ... AS new` (alias form; `VALUES()` removed in 8.4) | `ON DUPLICATE KEY UPDATE ... VALUES(col)` | returning rows differs: PG `RETURNING` vs re-select (established TCK pattern) |
  | Advisory lock | `pg_advisory_xact_lock(hashtext(?))` in-tx | `GET_LOCK(?, 10)` **session-level — must run on one pooled connection and pair with `RELEASE_LOCK`** | same as MySQL | R2DBC hazard: use connection-bound execution (`Mono.usingWhen` / `Connection` API) for the MySQL/MariaDB form |
  | JSON columns | native JSON codec | `CAST(? AS JSON)` binding (no backslash escapes / use bind params) | no `CAST AS JSON` — plain parameter text | contract type is `String`; conversion is dialect-internal |
  | String ops in catalog queries | `||`, `regexp_replace(..., 'g')` | `CONCAT`, global-by-default replace | as MySQL | from current `OAuthMcpMapper` forks |
  | Pagination | `LIMIT ? OFFSET ?` | `LIMIT ?, ?` | as MySQL | count query per repository method |

- **Lock × transaction composition rule** (binds the advisory-lock row above): `pg_advisory_xact_lock` participates in
  the surrounding `@Transactional` and auto-releases at commit; the MySQL/MariaDB `GET_LOCK`/`RELEASE_LOCK` pair runs
  on one pooled connection via `Mono.usingWhen` (release wired to cancellation as well) and **must not execute inside
  a Spring-managed transaction** — the transaction binds its own connection, and lock-connection + tx-connection under
  pool pressure can starve into deadlock. Lock repository methods are transaction-exempt by contract; the TCK ships a
  negative test for the pairing.
- **Snowflake IDs** are generated app-side before insert (D10) — no `RETURNING id` dependency, all dialects identical.
- **Native hints**: dialect modules register `RuntimeHints` for their row classes (`@RegisterReflectionForBinding`)
  and contribute driver-specific hints; verified by the native smoke test (§10).

## 9. TCK 2.0 — repository behavior contracts

`dc3-db-tck` gains a second suite alongside the existing mapper contracts (which remain until manager/data migrate):

- One abstract contract test per repository interface; three concrete subclasses (Postgres / MySQL / MariaDB) on
  Testcontainers, fixtures identical to today's images (`postgres`, `mysql:8.4`, `mariadb:10.11`, same initdb seeds).
- Coverage classes: CRUD + logical-delete semantics; fuzzy/paged listing equivalence (incl. default `create_time DESC`
  ordering and `PageUtil` clamping); upsert idempotency under the three dialect idioms; advisory-lock acquire/release
  (including the MySQL/MariaDB same-connection requirement); tenant negative tests (§6); JSON round-trip; UTC
  timestamp round-trip (TIMESTAMPTZ ↔ `LocalDateTime`, the current `TimestamptzLocalDateTimeTypeHandler` contract).
- Gate: new mapper/repository work must extend the contract suite — this is the discipline mechanism replacing
  `databaseId` routing.

## 10. Native verification

The pilot's exit criterion, in order:

1. `dc3-center-auth` builds with `mvn -Pnative` (plugin already managed in the root POM) against the R2DBC stack.
2. The native binary boots against a Testcontainers PostgreSQL, serves a representative endpoint slice
   (login → token → one tenant-scoped list → one MCP tool-catalog query), and passes the TCK contract suite run
   natively (allowed to be a subset — record which).
3. Image size / RSS / startup time are recorded in this document as the baseline for manager/data to beat.

If any blocking native issue surfaces (driver hints, reflection misses), it is a design-level finding: fix in
`dc3-db-r2dbc-core`/dialect modules, never by re-adding MyBatis to auth.

## 11. Transition and coexistence

- **auth cutover is hard** (D16): one commit removes `dc3-db-core`, `dynamic-datasource`, `@MapperScan`,
  `mapping/*.xml`, the 16 Manager shells, and `TenantContextHolder` usage from the auth path. No long-lived
  dual-persistence inside auth.
- **`dc3-center-single`** temporarily carries both stacks (auth on R2DBC, manager/data on MyBatis) against the same
  database — two pools, accepted and bounded by the transition.
- **Facade/gRPC edge stays synchronous through the transition (D18)**: the gateway and manager/data keep consuming
  blocking facade contracts; the reactive→blocking bridge is confined to auth's gRPC servers and local facade impls
  on non-event-loop threads, and is retired when the facade wave lands with manager/data.
- **Uncommitted MariaDB XML forks in the worktree** (`OAuthMcpMapper.xml` etc.): land or drop them independently of
  this design — their *semantic* findings (CAST-AS-JSON absence, `AS new` vs `VALUES()`, GET_LOCK pairing) are already
  folded into §8 and survive the XML's deletion.
- **Defect fix riding along** (D12): `UserController.add/delete`'s unmanaged principal→user→membership write gets a
  service-level reactive transaction during rewrite.
- **Common reuse across services**: when manager/data migrate, they copy the module pattern
  (`dc3-db-manager-{dialect}`, ...); `dc3-db-r2dbc-core` and TCK infrastructure are shared from day one.

## 12. Phased plan and gates

| Phase | Content | Gate |
|-------|---------|------|
| P0 | Design sign-off (this document; all §5 decisions Confirmed). | Approved doc committed. **Done 2026-08-28** (D14 deferred to its spike). |
| P1 | Skeletons: `dc3-db-r2dbc-core` (snowflake + workerId rule (D10), `PageResult`, exception mapping), `dc3-db-r2dbc-boot` (auto-config, D19 codec), module shells ×3, TCK harness extension, auto-config + fail-fast selection wiring. | `make test` green; empty contract wired end-to-end on PG. |
| P2 | **Vertical slice**: `TenantRepository` (pure CRUD + paging) and `ResourceRegistryLockRepository` (advisory lock, most dialect-sensitive) — contract → 3 dialects → TCK → reactive service → controller. | TCK 3/3 green; PG dev stack boots and serves tenant CRUD. |
| P3 | Native spike on the slice: `dc3-center-auth` `-Pnative` compile + boot + endpoint smoke. | §10 criteria on the slice. |
| P4 | Rollout by aggregate: users/principals/credentials → roles/binds/memberships → menus/APIs/resources + registry sync → service accounts/audit → OAuth/MCP (largest, last). Each aggregate lands with its TCK contracts and its migrated StepVerifier tests (Appendix A). | Full TCK green ×3; auth E2E (`dc3-e2e`) green. |
| P5 | Auth MyBatis removal (D16), `dc3-center-auth` native image as the deliverable, docs updated (`db-dialects.md`, this file's status), MySQL driver decision (D14) recorded. | Native boot; zero `mybatis` strings on auth classpath. |

P2–P3 are the go/no-go point: if the slice proves the pattern, P4 is mechanical; if it doesn't, only the slice is
thrown away.

## 13. Open questions

1. **D14 driver data** — actual TCK results for `r2dbc-mysql` vs `r2dbc-mariadb`-against-MySQL (P1 spike output).
2. **Row-class strategy** (§7): annotated shared DOs vs private row classes per dialect — pick one convention during
   P2 and record it here.
3. **Connection pool sizing under native/R2DBC** — whether 64 max (F13) is still right for reactive demand patterns;
   measure during P3.
4. **`dc3-e2e` coverage** — which auth E2E flows must exist before P4 rollout is allowed to start.
5. **History of `storage-abstraction.md`** — mark §3 superseded by this document (one-line status edit) or retire the
   whole file once manager/data land (its TSDB half is already superseded).
6. **Observability parity** — exact r2dbc-pool metric set, health-indicator wiring, and the slow-query logging
   convention to be implemented in `dc3-db-r2dbc-boot` (P2); measure against the Hikari baseline from F13.

## Appendix A — auth migration inventory

- **Mappers (18)**: 16 `BaseMapper` shells (map 1:1 to 15 empty Managers + `IdentityAuditLogManager`) → standard
  repository contracts; `OAuthMcpMapper` (30 methods, 7 tables) → 5 aggregate repositories (D13);
  `ResourceRegistryLockMapper` (advisory lock) → `ResourceRegistryLockRepository`.
- **`@Transactional` sites (19)**: enumerated in F9; all carry to D12.
- **Dialect-forked statements in auth**: `ResourceRegistryLockMapper.xml` (lock, 3 forms), `OAuthMcpMapper.xml`
  (JSON casts ×4, catalog string ops ×2, upsert idioms ×2) — semantics table in §8.
- **Services to rewrite reactive**: 12 paged/fuzzy Services + `OAuthMcpRuntimeServiceImpl` +
  `ResourceRegistrySyncServiceImpl` + `DictionaryForAuthService` (reads `dc3_tenant`, F12).
- **Facade & gRPC servers (the edge, D18)**: the 7 facade contracts stay blocking; the 7 gRPC servers
  (`TokenServer`, `PermissionServer`, `UserServer`, `TenantServer`, `LocalCredentialServer`,
  `ResourceRegistryServer`, `McpRuntimeServer`) and the 7 local facade impls gain the explicit reactive→blocking
  bridge (non-event-loop threads) and swap `runIgnore` wrappers for unscoped method variants (§6).
- **Tests (17 files)**: blocking service/biz, controller, and gRPC tests (e.g. `TokenServiceImplTest`,
  `OAuthMcpRuntimeServiceImplTest`, `TokenServerTest`, `McpRuntimeServerTest`) rewritten with `StepVerifier`;
  in-process gRPC tests keep `GrpcInProcessExtension` against the D18 blocking edge; ThreadLocal-based context
  tests migrate to explicit-parameter assertions.
- **Not in scope**: `dc3-common-dal` (F12), all manager/data/agentic DAL, DDL/initdb, `dc3-db-core` and existing
  dialect modules (F15).
