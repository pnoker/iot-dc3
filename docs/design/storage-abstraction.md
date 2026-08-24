# Design: Pluggable Storage Abstraction (Relational Dialect + Time-Series Port)

|                |                                                                                   |
|----------------|-----------------------------------------------------------------------------------|
| **Status**     | Implemented — R1/R2 landed (dual-dialect seed + forks + contract suite; selection guide at [db-dialects.md](../db-dialects.md)) |
| **Date**       | 2026-08-17                                                                        |
| **Revised**    | 2026-08-19 — inventory refreshed after the driver-lease commit (956de3dd3); §6.1 added |
| **Scope**      | persistence layer: relational core + point-value time-series store                 |
| **Target**     | Relational: PostgreSQL (default), MySQL 8 — Time-series: TimescaleDB (default), TDengine, InfluxDB, IoTDB |
| **Related**    | [`mq-abstraction.md`](./mq-abstraction.md) — the third pluggable dimension          |
| **Discussion** | open for review before implementation starts                                       |

## 1. Summary

IoT DC3 should let a deployer pick the relational database, the time-series store, and the
message broker **independently** — any combination must be a supported deployment, e.g.
`MySQL + TDengine + RocketMQ` for one company, `PostgreSQL + TimescaleDB + Kafka` for
another. This document designs the two storage dimensions; the broker dimension is
covered by the companion MQ design.

The key structural insight: today TimescaleDB runs as an **extension inside the main
PostgreSQL instance** (single `dc3` database, single datasource, `dc3_point_value`
hypertable in the `dc3_history` schema), so "replace the time-series store" currently has
no deployment boundary to swap at. But the code is closer to ready than the deployment:
a `RepositoryService` port already exists (`dc3-common-repository`) and carries almost
  the entire point-value surface — but the dashboard read surface still bypasses it.
  The work is therefore:

- **Relational layer** — not a new port: MyBatis *is* the port. What is needed is a
  **dialect mechanism**: a dialect-neutral infra module, portable-first SQL policy, a
  `databaseId` fork for the genuinely dialect-bound statements (9 today, across 4
  mappers — concentrated in the driver-lease subsystem), and per-dialect seed SQL.
  Only 7 of 51 mapper files contain PostgreSQL-specific syntax today.
- **Time-series layer** — promote the existing `RepositoryService` port to a real
  swappable boundary: extract the implementation into per-store modules, fold the
  dashboard read surface (≈10 bypass statements) into the port, neutralize the
  SQL-flavored parts of the interface (pagination, aggregate semantics), and add
  per-store adapters.
- Both layers share the same mechanism family as the MQ design: **profiles +
  capability matrix + TCK**, giving the project one consistent pluggability story:
  `dc3.facade.mode` (today) → `dc3.db.type`, `dc3.repository.type`, `dc3.mq.type`.

## 2. The three-layer storage model

```
┌─────────────────────────────────────────────────────────────────┐
│ Relational core            dc3.db.type: postgres | mysql        │
│ auth, manager, alarms, command/event history, observability     │
│ → MyBatis dialect profiles (§3)                                 │
├─────────────────────────────────────────────────────────────────┤
│ Time-series store          dc3.repository.type:                 │
│ dc3_point_value domain          timescale | tdengine |          │
│ → RepositoryService port        influxdb | iotdb          (§4)  │
├─────────────────────────────────────────────────────────────────┤
│ Vector (future)            capability-negotiated (§5)           │
│ agentic embeddings — pgvector or external store                 │
└─────────────────────────────────────────────────────────────────┘
```

Orthogonality matrix — every cell is a valid deployment:

|                     | TimescaleDB (in-PG) | TDengine        | InfluxDB        | IoTDB           |
|---------------------|---------------------|-----------------|-----------------|-----------------|
| **PostgreSQL core** | ✅ today's default   | ✅              | ✅              | ✅              |
| **MySQL core**      | n/a (PG-only)       | ✅              | ✅              | ✅              |

Note the bottom-left cell: with an external time-series store, the relational database
loses its only heavy PostgreSQL-only feature (the hypertable), and `timescale` collapses
into `postgres`. The two migration tracks are independent and mutually simplifying.

## 3. Layer 1 — Relational dialect

### 3.1 Current coupling (verified inventory)

**51 mapper XML files** across `dc3-common-auth` (18), `dc3-common-dal` (4),
`dc3-common-data` (11), `dc3-common-manager` (18). **7 contain PostgreSQL-specific
syntax** — the 2026-08-18 driver-lease commit (`956de3dd3`) added one file
(`DriverLeaseMapper`) and converted another (`PointValueMapper`: its `DISTINCT ON`
batch-latest statement was replaced by `ON CONFLICT` upserts):

| File | Syntax | Semantics |
|------|--------|-----------|
| `dc3-common-data/.../EntityStateMapper.xml` | 2 stmts: `ON CONFLICT ... RETURNING` (`upsertEntityState`, + `json_build_object`), `UPDATE ... FROM ... RETURNING` (`claimExpiredDevices`) | entity-state upsert + expired-lease claim, both returning rows (driver heartbeat hot path) |
| `dc3-common-data/.../PointValueMapper.xml` | 2 stmts: `ON CONFLICT DO NOTHING ... RETURNING`, `ON CONFLICT DO UPDATE` | idempotent batch insert + latest-value upsert (durable telemetry path) |
| `dc3-common-data/.../DashboardMapper.xml` | `time_bucket(INTERVAL ...)` | dashboard time-series aggregation (TimescaleDB) — belongs to Layer 2, see §4.2 |
| `dc3-common-data/.../AlertMapper.xml` | `COUNT(*) FILTER (WHERE)` ×3, `generate_series` + `date_trunc` | alert stats + calendar-day spine |
| `dc3-common-auth/.../ResourceRegistryLockMapper.xml` | `pg_advisory_xact_lock(hashtext())` | registry distributed lock |
| `dc3-common-auth/.../OAuthMcpMapper.xml` | `::text` casts | parameter null-check idiom |
| `dc3-common-manager/.../DriverLeaseMapper.xml` | 4 stmts: `pg_advisory_xact_lock`, `ON CONFLICT ... DO UPDATE ... RETURNING` ×3 | driver lease acquire / renew / device-claim (hot path, new in `956de3dd3`) |

**Infrastructure module** `dc3-common-postgres` — hardcodes `org.postgresql.Driver`,
`PaginationInnerInterceptor(DbType.POSTGRE_SQL)`, `TimestamptzLocalDateTimeTypeHandler`,
and the `postgres` profile activation. Every business module depends on it.

**DDL specifics** (seed `dc3/dependencies/postgres/initdb/`): `TIMESTAMPTZ` columns
throughout; 5 schemas (`dc3_auth`, `dc3_data`, `dc3_manager`, `dc3_history`,
`dc3_agentic`) resolved via `search_path` — mapper table names are conveniently
**unqualified**; `JSON` columns (portable type, not `jsonb`), defaulted as
`'{}'::JSON` — MySQL has no literal JSON defaults, its seed uses expression defaults
(`DEFAULT ('{}')`, requires 8.0.13+). **plpgsql trigger functions** are additional
MySQL-incompatible seed logic: a trivial `update_operate_time()` per schema (MySQL:
`ON UPDATE CURRENT_TIMESTAMP`), and — from the driver-lease commit — three
`track_driver_device_revision_*` statement-level triggers using transition tables and
`INSERT ... ON CONFLICT` (MySQL has neither statement-level triggers nor transition
tables; this revision-counter logic must move into the application layer or be
re-expressed). Extensions installed: `timescaledb` (used — Layer 2), `vector`
(installed, **no columns use it**), `age` (installed; no `cypher()` usage exists in
any mapper — see open questions).

### 3.2 Design

**Module split** — same family as facade/MQ profiles:

```
dc3-common-postgres  ──►  dc3-common-jdbc    # dialect-neutral: TenantLineHandler,
                                        #      MyBatis-Plus wiring, MybatisUtil
                         dc3-db-postgres     # driver, DbType, timestamptz handler, profile
                         dc3-db-mysql        # driver, DbType, DATETIME(6) mapping
```

Selected by `dc3.db.type` via `@ConditionalOnProperty`, the established
`dc3.facade.mode` pattern.

**Portable-first SQL policy.** The default action for a PG-specific statement is a
portable rewrite that performs equivalently on PostgreSQL — the fork is reserved for
statements where portability costs semantics or performance:

| PG-only syntax | Portable rewrite (runs on both) | Fork needed? |
|---------------|--------------------------------|--------------|
| `COUNT(*) FILTER (WHERE x)` | `SUM(CASE WHEN x THEN 1 ELSE 0 END)` | no |
| `::text` casts | drop / rewrite null-check idiom | no |
| `generate_series` date spine | recursive CTE (MySQL 8 ✔, PG ✔) | no |
| `ON CONFLICT ... RETURNING` | — | **fork**: `INSERT ... ON DUPLICATE KEY UPDATE` + same-tx re-select |
| `UPDATE ... FROM ... RETURNING` | — | **fork**: MySQL has no `RETURNING`; multi-table `UPDATE` + re-select |
| `pg_advisory_xact_lock` | — | **fork**: `GET_LOCK()` / lock-table unique constraint |
| `time_bucket(...)` | — | **moves to Layer 2** (TSDB adapter concern) |

(`DISTINCT ON` → `ROW_NUMBER()` was a planned portable rewrite until 2026-08-18; the
statement it applied to was replaced by `ON CONFLICT` upserts and is now a fork — see
§3.1.)

Forks use MyBatis' native `databaseIdProvider`: `<select id="x" databaseId="mysql">`.
Expected fork surface after the portable rewrites: **9 statements across 4 mappers** —
`EntityStateMapper` ×2, `PointValueMapper` ×2, `DriverLeaseMapper` ×4,
`ResourceRegistryLockMapper` ×1. The driver-lease subsystem (lease acquire/renew,
durable telemetry, revision triggers) is now the single largest PG-idiomatic cluster;
its semantics are portable, only their expression is not.

**Schema → database.** The 5 PG schemas map to 5 MySQL databases; because mapper table
names are unqualified, the connection's selected database plays the role `search_path`
plays today. No mapper changes.

**Time semantics.** `TIMESTAMPTZ` → `DATETIME(6)` plus an explicit
**store-and-transfer-in-UTC** convention (already implicit in
`TimestamptzLocalDateTimeTypeHandler`); documented and asserted in the TCK.

**Seed SQL.** Per-dialect directories, backend-owned as today:
`dc3/dependencies/postgres/initdb/` (existing) and `dc3/dependencies/mysql/initdb/`.
The `dc3_point_value` DDL and all TimescaleDB statements move to the `timescale`
repository adapter (§4) — with an external TSDB the relational seed contains no
extension DDL at all. Seed-side divergences beyond column types: the plpgsql trigger
functions (§3.1) — `update_operate_time()` maps to `ON UPDATE CURRENT_TIMESTAMP`, the
driver-device revision triggers need re-implementation (application layer or MySQL
row-level triggers) — and `'{}'::JSON` defaults map to expression defaults.

**Hard requirement:** MySQL 8.0+ (window functions, CTEs, recursive CTEs). MariaDB 10.5+
follows for free; older MySQL is out of scope.

### 3.3 Relational TCK

`make test-it` (DAL integration tests) executed against two Testcontainers images
(`postgres` and `mysql:8`) with identical fixtures; identical results = compliant.
CI runs both — this is also the discipline mechanism that keeps new mappers portable
or explicitly forked.

## 4. Layer 2 — Time-series store (`RepositoryService` port)

> **Superseded 2026-08-20**: the time-series track now has its own full design at
> [`tsdb-abstraction.md`](./tsdb-abstraction.md) (standalone-store targets, TCK gate,
> latest-value-stays-relational decision). The sketch below is kept for history.

### 4.0 (historical sketch)

### 4.1 What exists today

- **Port**: `dc3-common-repository` defines `RepositoryService` — 8 operations that
  cover the entire point-value domain: `savePointValue(s)`,
  `listHistoryPointValue`, `selectLatestPointValue`, `listLatestPointValues`,
  `listPagePointValue`, `aggregateInWindow`, `samplesInWindow` — plus
  `RepositoryStrategyFactory` (named-strategy registry) and `ActiveRepositoryProfileConfig`.
- **Single implementation**: `PostgresRepositoryServiceImpl` (inside
  `dc3-common-data`), registered under `StrategyConstant.Storage.POSTGRES`; treats the
  TimescaleDB hypertable as a plain PG table via `PointValueManager`/`PointValueMapper`.
- **Flow**: `PointValueReceiver` (MQ) → `PointValueIngestBuffer` →
  `PointValueServiceImpl` → `PointValueLocalCacheService` (latest-value cache) +
  `RepositoryService` (persistence). Alarm long-window evaluation reads via
  `RepositoryWindowDataSource` → the same port.
- **Deployment**: TimescaleDB is an in-PG extension — single instance, single
  datasource, no swap boundary.

### 4.2 Gaps to close before the store is truly swappable

1. **The dashboard read surface bypasses the port.** The data-side
   `DashboardMapper.xml` reads `dc3_point_value` directly in **9 of its 10 statements**
   (`countInRange`, `countTotal`, `timeseries`, `top`, `latestStream`,
   `latencyHistogram`, `hourlyActivity`, `silentSources`, `coverageGapItems`); the
   manager-side `DashboardMapper.xml` adds a 10th (`FROM dc3_history.dc3_point_value`).
   Only `timeseries` uses `time_bucket` — the others are portable SQL, but portable or
   not they all break the moment the point-value store is external. And several are
   richer than a bucketed aggregation (latency histogram bins, silent-source
   detection, coverage gaps, activity grid): designing the neutral read port — or
   re-expressing these dashboards on top of port primitives — is the largest single
   T1 workload, not a one-statement move (open question §8).
2. **Extract the implementation** into per-store modules; selection by
   `dc3.repository.type`:

   ```
   dc3-repository-timescale    # today's PostgresRepositoryServiceImpl, moved
   dc3-repository-tdengine     # TDengine 3.x super-table adapter
   dc3-repository-influxdb     # InfluxDB 3.x adapter
   dc3-repository-iotdb        # Apache IoTDB adapter
   ```

3. **Neutralize the SQL-flavored surface** of the port: `Page` (a MyBatis-Plus type)
   and SQL aggregate semantics leak through. Design target: a cursor-friendly
   pagination abstraction (time-descending cursor; offset optional capability) and a
   store-neutral aggregate enum — TDengine `INTERVAL`, InfluxDB 3 SQL and IoTDB all
   express AVG/MIN/MAX/SUM/COUNT windows, so the enum maps cleanly.
4. **Data-model mapping** per adapter — tenant isolation rides the store's native
   dimension (tenant safety rule preserved):

   | Concept | Timescale | TDengine 3 | InfluxDB 3 | IoTDB |
   |---------|-----------|------------|------------|-------|
   | series identity | (tenant_id, device_id, point_id) PK | super table + tags `tenant/device/point` | measurement + tags | path `tenant.device.point` |
   | tenant scoping | WHERE tenant_id | tag filter (indexed) | tag filter | path prefix filter |
   | latest value | index / `DISTINCT ON` | `LAST()` | `last()` / SQL | `last` query |
   | window aggregate | `time_bucket` | `INTERVAL(...)` | SQL `date_bin` | group-by-time window |
   | retention | drop-chunks policy / compression | `KEEP` per database | retention policy | TTL |
   | write path | SQL batch | schemaless / STABLE batch insert | line protocol / SQL | session batch insert |

5. **Retention & lifecycle** as declared capabilities (timescale compression,
   TDengine `KEEP`, Influx retention, IoTDB TTL) rather than hidden DDL.
6. **Deployment topology**: an external TSDB is a second datasource with its own
   compose service and profile (`dc3/dependencies/<store>/`, image per store,
   mirroring the broker-per-adapter pattern). With an external store the main PG no
   longer needs TimescaleDB at all.

### 4.3 Time-series TCK

One suite, N containers: write a fixed fixture of point values, then assert
`listHistoryPointValue` / `listLatestPointValues` / `aggregateInWindow` /
`samplesInWindow` return equivalent results; tenant-isolation negative tests
(cross-tenant reads return nothing); retention test (expired data disappears).
Passing the TCK is the acceptance bar for community stores (e.g. someone bringing
Cassandra or ClickHouse adapters later).

## 5. Layer 3 — Vector (placeholder, capability-negotiated)

`pgvector` is installed but unused — no `vector` columns exist. When the Agentic Center
adds embeddings, the decision point arrives: vector search as a **PostgreSQL-only
capability** (feature-flagged via capability negotiation, MySQL profile degrades to
external store or no vector search) or as a fourth pluggable dimension. Not designed
now; recorded so the choice is conscious when it happens.

## 6. Configuration surface (unified across all pluggable dimensions)

```yaml
dc3:
  facade:
    mode: grpc          # exists today
  db:
    type: postgres      # postgres | mysql                     (§3)
  repository:
    type: timescale     # timescale | tdengine | influxdb | iotdb  (§4)
  mq:
    type: rabbitmq      # see mq-abstraction.md
```

One mechanism for all: `@ConditionalOnProperty` profile selection, capability matrix
published per dimension, startup log summarizing the negotiated combination. Compose
stacks declare per-store services behind profiles so `make up` assembles whatever
combination is configured.

### 6.1 What "pluggable" means — and what it deliberately does not

**Deploy-time selection is the goal.** Any combination of the three dimensions is
chosen when the stack is assembled; `dc3.*.type` is read once at startup. Switching an
existing deployment means changing the property and restarting the affected services —
for a fresh deployment that is the whole story, no data moves.

**Runtime hot-swap is explicitly not a goal**, for three structural reasons:

1. **Data gravity** — the engine holds the data. Swapping PostgreSQL for MySQL under
   live data without moving it yields an empty store; moving the data is a
   *migration*, not a plug event. No abstraction layer can change that.
2. **Startup binding** — connection pools, `SqlSessionFactory` and `databaseId`
   resolution (relational), TSDB clients and ingest-buffer flush points (time-series)
   are all established at boot.
3. **Dialect correctness** — every forked statement is parsed and routed for the
   engine that will execute it; a runtime dialect flip would bypass that guarantee.

**What the port architecture does enable is online migration with cutover** ("warm
swap"). For the time-series dimension the `RepositoryStrategyFactory` can register two
adapters simultaneously: dual-write window (fan out writes) → backfill history from
the old store → equivalence-check against the TCK fixtures → flip reads → drain →
retire the old store. The MQ-buffered ingest path (`PointValueReceiver` →
`PointValueIngestBuffer`) means no in-flight data is lost while a store is briefly
unavailable. For the relational dimension the equivalent is standard online-migration
tooling (logical replication / CDC / pgloader) — supported by, but outside, this
design.

**Adding a store is "hot" in the only sense that matters**: adapter modules are
additive — new module + one config property, zero core changes; the TCK is the bar.

## 7. Migration plan

The two tracks are independent and can interleave:

- **R1 — relational hygiene (zero behavior change).** Portable SQL rewrites
  (`FILTER`→`SUM(CASE)` ×3, `generate_series`→recursive CTE, drop `::text`), split
  `dc3-common-postgres` → `dc3-common-jdbc` + `dc3-db-postgres`.
  *Gate: existing E2E and `make test-it` green, unchanged.*
- **R2 — MySQL dialect.** Fork the 9 dialect-bound statements across 4 mappers via
  `databaseId`; re-implement the seed trigger functions (`update_operate_time` →
  `ON UPDATE CURRENT_TIMESTAMP`, driver-device revision triggers → application layer);
  write `initdb/mysql/` (expression JSON defaults), add `dc3-db-mysql`, stand up the
  dual-dialect DAL TCK in CI.
- **T1 — consolidate the TSDB boundary.** Move `PostgresRepositoryServiceImpl` →
  `dc3-repository-timescale`; fold the 10-statement dashboard read surface into the
  port (spike the read-primitives shape first — open question §8.2); move
  `dc3_point_value` DDL + `time_bucket` out of the relational seed.
  *Gate: no query outside the TSDB adapter references `dc3_point_value`.*
- **T2 — port surface cleanup + TDengine adapter.** Cursor pagination, neutral
  aggregate enum; TDengine 3 adapter as the first external-store proof, plus the
  time-series TCK.
- **T3 — community stores.** InfluxDB / IoTDB adapters, explicitly framed as
  community-sized tasks with the TCK as the bar (same framing as MQ adapters).

## 8. Open questions

1. **AGE** — installed and loaded at bootstrap, but no `cypher()` query exists in the
   codebase, and AGENTS.md describes it as a pillar extension. Keep (documented as
   reserved for future graph features) or drop from the base image? Either way the
   docs and the image should agree.
2. **Dashboard read-port home and shape** — the bypass surface is 10 statements
   (§4.2), several richer than bucketed aggregation (latency histogram, silent
   sources, coverage gaps). Options: (a) extend `RepositoryService` with composable
   read primitives (count-in-range, bucketed aggregate, latest-stream) and
   re-express the analytic dashboards on top; (b) a separate read-only dashboard
   port carrying the full statement semantics. Leaning: (a) — a port that encodes
   every dashboard shape would leak the current UI into the contract, and the
   primitives compose; but the re-expression cost for `latencyHistogram` /
   `silentSources` / `coverageGapItems` needs a spike before T1 commits to it.
3. **Pagination semantics** — pure time cursor, or cursor + optional offset for
   small-result UI pages? Affects the frontend history view contract.
4. **Latest-value read path** — `PointValueLocalCacheService` already caches latest
   values; confirm whether port-level `selectLatest*` is only a cold-start fallback
   (if so, adapters can implement it simply and optimize for write throughput).
5. **InfluxDB version** — 3.x (SQL) is the natural target; whether a 1.8/2.x
   contributor adapter is worth accepting is a community question.
6. **Phase ordering** — if T1/T2 land before R2, the MySQL fork shrinks (hypertable
   already gone); if R2 lands first, MySQL ships with plain-table `dc3_point_value`
   and T2 migrates it. Decide by community demand signals.

## 9. Appendix — inventory (migration checklists)

PG-specific mapper statements (R1/R2 checklist; refreshed 2026-08-19):

| Mapper | Statement | Action |
|--------|-----------|--------|
| `AlertMapper` ×3 | `COUNT(*) FILTER` | portable rewrite (`SUM(CASE)`) |
| `AlertMapper` | `generate_series` + `date_trunc` | recursive CTE (unify) |
| `OAuthMcpMapper` | `::text` casts | drop / rewrite |
| `EntityStateMapper` | `upsertEntityState` — `ON CONFLICT ... RETURNING` | **fork** (pg / mysql) |
| `EntityStateMapper` | `claimExpiredDevices` — `UPDATE ... FROM ... RETURNING` | **fork** (pg / mysql) |
| `PointValueMapper` | idempotent batch insert — `ON CONFLICT DO NOTHING ... RETURNING` | **fork** (pg / mysql); statement moves into TSDB adapter at T1 |
| `PointValueMapper` | latest-value upsert — `ON CONFLICT DO UPDATE` | **fork** (pg / mysql); moves into TSDB adapter at T1 |
| `DriverLeaseMapper` | `pg_advisory_xact_lock` acquire | **fork** (pg / mysql) |
| `DriverLeaseMapper` ×3 | lease renew / device-claim — `ON CONFLICT ... DO UPDATE ... RETURNING` | **fork** (pg / mysql) |
| `ResourceRegistryLockMapper` | `pg_advisory_xact_lock(hashtext())` | **fork** (pg / mysql) |
| `DashboardMapper` (data) | `time_bucket` + 8 further direct `dc3_point_value` reads | **T1**: fold into TSDB port |
| `DashboardMapper` (manager) | direct `dc3_history.dc3_point_value` read | **T1**: fold into TSDB port |

Seed-side R2 items: plpgsql `update_operate_time()` triggers (→ `ON UPDATE
CURRENT_TIMESTAMP`), 3× `track_driver_device_revision_*` statement-level triggers
(→ application layer), `'{}'::JSON` defaults (→ expression defaults).

TSDB boundary checklist (T1):

- `RepositoryService` / `RepositoryStrategyFactory` / `ActiveRepositoryProfileConfig` —
  keep, promote to shared contract.
- `PostgresRepositoryServiceImpl` + `PointValueManager`/`PointValueMapper`
  point-value statements → `dc3-repository-timescale`.
- `dc3_point_value` DDL, hypertable/compression DDL, seed data → timescale adapter.
- Verify zero remaining `dc3_point_value` references outside TSDB adapters
  (`grep -r dc3_point_value` gate).

**R1/R2 实施记录（2026-08-24）**：关系轨道全部落地——R1 可移植改写 + 模块拆分
（中立基建 + 顶层 `dc3-db` 家族——后按家族一致性迁为 `dc3-db/dc3-db-core`，与 dc3-mq-core/dc3-tsdb-core 同构）；R2 MySQL 方言（databaseId
fork、RETURNING 解耦为 upsert+re-select、序列退役为行内 +1、咨询锁/触发器/
JSON 簇各有等价实现）+ 双引擎种子（`pg2mysql_seed.py` 派生）+ `dc3-db-tck`
双方言契约套件（PG/MySQL 各 8/8：latest 围栏 upsert 三级决胜、state
upsert+reselect、三步 claim、租约行内递增、修订触发器、目录 JSON 双跳）。
实施中的关键发现（已全部写进 [db-dialects.md](../db-dialects.md)）：MySQL 8.4
移除了 ODKU 的 VALUES()（行别名 `AS new` 是正道，VALUES() 会静默自比较）；
MySQL 的 SET 按顺序生效而 PG 读快照——守卫列必须前置；内嵌 JSON 文本进
MySQL 需 NO_BACKSLASH_ESCAPES 或参数绑定。§3.1 清单与最终交付的差异：时序
迁移（T1-T3）先行落地后，PointValueMapper 只剩 latest 投影一条 fork、数据侧
DashboardMapper 十条旁路已整体消失。
