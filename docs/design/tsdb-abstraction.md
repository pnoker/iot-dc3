# Design: Pluggable Time-Series Store Abstraction (TSDB Port)

|                |                                                                                   |
|----------------|-----------------------------------------------------------------------------------|
| **Status**     | Proposed — not yet implemented                                                     |
| **Date**       | 2026-08-20                                                                        |
| **Scope**      | point-value time-series storage (device telemetry history + analytics reads)       |
| **Target**     | TimescaleDB (default), TDengine, InfluxDB, IoTDB — plus a TCK for community stores |
| **Predecessor**| [`storage-abstraction.md`](./storage-abstraction.md) §4 sketched this track; this document supersedes that section |
| **Sibling**    | [`mq-abstraction.md`](./mq-abstraction.md) — same port/adapter/TCK playbook, executed 2026-08-19..20 |
| **Discussion** | open for review before Phase 1 starts                                             |

## 1. Summary

Device telemetry history lives in a TimescaleDB hypertable embedded in the main
PostgreSQL instance, written and read through paths that leak the store everywhere:
idempotent `ON CONFLICT` inserts, a fencing-token-guarded latest-value upsert, a
MyBatis-Plus `Page` in the port signature, a single-window-only aggregate, and **nine
dashboard statements that query the hypertable directly** (one with
`dc3_history.` cross-schema, another via `time_bucket`). The store is not swappable
today — not because a port is missing (`RepositoryService` exists), but because the
port is SQL-shaped, incomplete, and bypassed.

The proposal mirrors the executed MQ playbook: a thin **TSDB port**
(`dc3-tsdb-core`) carrying the semantics the platform actually uses — series
append with natural upsert, last-N history, cursor-paginated range history,
single-window and **bucketed** aggregation, count-in-range — plus one adapter per
store behind `dc3.tsdb.type` and a broker-neutral TCK as the acceptance bar.
TimescaleDB comes first as a behavior-identical extraction; TDengine (CN demand),
InfluxDB 3 and IoTDB follow.

Two architectural decisions do most of the work:

1. **The latest-value projection stays relational.** `dc3_point_latest` (the
   fencing-token-guarded "current value per point") is OLTP state, not time-series —
   it feeds dashboards, streams and alarms with names joined in, and its semantics
   (tuple-comparison guard) cannot be expressed uniformly across TSDBs. Every adapter
   updates it through the same relational path; TSDB adapters store **history only**.
   This removes the hardest cross-store semantic instead of negotiating it.
2. **Idempotency moves to the ingest layer; the store does natural upsert.** The
   hypertable's `message_id` unique index is a relational luxury. TSDBs dedupe on
   (series, timestamp) with store-specific duplicate policies; the port declares the
   policy per adapter and the ingest layer keeps a short idempotency window for
   MQ-level redeliveries (the at-least-once contract stays).

## 2. What exists today — verified inventory

### 2.1 Write path (lease-fenced, transactional)

`PointValueReceiver` (MQ batch) → schema-v1 validation → `PostgresRepositoryServiceImpl.
savePointValues` — one PostgreSQL transaction:

1. `insertHistoryBatch` — `INSERT INTO dc3_point_value … ON CONFLICT (message_id,
   create_time, device_id) DO NOTHING RETURNING message_id` (idempotent append;
   accepted set = returned ids).
2. `upsertLatestBatch` — `INSERT INTO dc3_point_latest … ON CONFLICT (tenant_id,
   device_id, point_id) DO UPDATE … WHERE (EXCLUDED.fencing_token, EXCLUDED.create_time,
   EXCLUDED.sequence, EXCLUDED.message_id) > (old tuple)` — fencing-guarded latest.
3. Batch pre-sorted by `INGEST_ORDER` to avoid deadlocks between concurrently
   consuming replicas.

### 2.2 Physical schema (`dc3/dependencies/postgres/initdb/05-iot-dc3-history.sql`)

- `dc3_point_value` hypertable: `message_id, schema_version, driver_node, sequence,
  fencing_token, device_id, point_id, raw_value TEXT, cal_value TEXT,
  num_value DOUBLE NULL (numeric projection of cal_value), driver_id, tenant_id,
  create_time TIMESTAMPTZ (device acquisition time), operate_time`. Chunked by day
  + 16-way hash on `device_id`; unique `(message_id, create_time, device_id)`;
  compression after 7 days (segmentby tenant/device/point); **retention 180 days**.
- `dc3_point_latest`: one row per (tenant, device, point), the transactional latest
  projection.

### 2.3 Read surface

| Caller | Operation | Today |
|--------|-----------|-------|
| Alarm rule windows (`RepositoryWindowDataSource`) | `aggregateInWindow` (AVG/MIN/MAX/SUM/COUNT + sample_count), `samplesInWindow` | via port |
| History pages | `listPagePointValue` — MyBatis-Plus `Page` + relative windows (`rangeHours`, `rangeKey`, `createTimeFrom`), device/point name + enable filters | via port |
| Last-N | `listHistoryPointValue(tenant, device, point, count)` | via port |
| Latest | `selectLatestPointValue` / `listLatestPointValues` | via port (reads `dc3_point_latest`) |
| **Dashboard (data)** | `countInRange`, `countTotal`, `timeseries` (`time_bucket` + `generate_series` gap fill), `top`, `latestStream`, `latencyHistogram` (`operate_time − create_time` ms bins), `hourlyActivity`, `silentSources`, `coverageGapItems` — **9 statements, direct SQL on the hypertable** | **bypasses port** |
| **Dashboard (manager)** | 1 more statement reading `dc3_history.dc3_point_value` cross-schema | **bypasses port** |

### 2.4 What is unreasonable about it (the motivation, plainly)

1. **Embedded store**: TimescaleDB shares the OLTP instance — telemetry I/O competes
   with transactions; no independent scaling, backup or lifecycle.
2. **The port is SQL-shaped**: MyBatis-Plus `Page` and SQL aggregate semantics leak
   through `RepositoryService`; an external store cannot implement it faithfully.
3. **The port is incomplete**: no bucketed aggregation (dashboards had to bypass it
   with `time_bucket`), no cursor pagination, no count — so the bypass grew.
4. **The port is bypassed**: 10 dashboard statements read the hypertable (one
   cross-schema) — any store swap breaks the dashboards silently.
5. **Single-window aggregate only** — charting/downsampling has no port primitive.
6. **Relational luxuries assumed**: `message_id` unique index, tuple-comparison
   upsert guards, `RETURNING`-based idempotency — none portable.
7. **Value model does double duty**: every sample carries `raw_value`/`cal_value`
   strings plus a `num_value` projection — necessary (aggregates need numerics,
   history needs originals), but the projection policy is implicit.

## 3. Goals / Non-Goals

**Goals**

- `dc3-common-data` (and the dashboard services) compile against a store-neutral
  TSDB API with zero store classes on the compile classpath.
- One adapter per store, selected by `dc3.tsdb.type` (default `timescale`); exactly
  one active, mirroring `dc3.mq.type`.
- Timescale deployments keep behavior: same physical schema when embedded, same
  read results — Phase 1 is an extraction, not a rewrite.
- The dashboards stop bypassing the port — every point-value read goes through it.
- Community adapters have a mechanical acceptance bar (TCK), like `dc3-mq-tck`.
- External-store deployments: the store gets its own compose service and profile;
  the main PG drops the Timescale extension entirely (it keeps `dc3_point_latest`).

**Non-Goals**

- Relational dialect pluggability (MySQL etc.) — that is storage-abstraction.md
  Layer 1, unchanged.
- Vector storage (Layer 3 placeholder).
- Cross-store query federation, store-side JOINs with relational metadata
  (enrichment stays at the application layer).
- Exactly-once. At-least-once + idempotent writes, as everywhere else.
- Streaming analytics / continuous aggregates as a port primitive (capability-gated
  future; dashboards use bucketed pull queries).

## 4. Semantics the port must carry

Verified against the callers above; this is the whole list — no more, no less.

| # | Semantic | Current shape |
|---|----------|---------------|
| S1 | Series identity | `(tenantId, deviceId, pointId)`; numeric IDs only — names enriched at the app layer |
| S2 | Append (batch, at-least-once) | one row per sample: full envelope incl. `message_id`, `schema_version`, `driver_node`, `sequence`, `fencing_token`, `raw/cal/num`, `driver_id`, `create_time` (device time), `operate_time` (server receive time) |
| S3 | Duplicate policy | store-level upsert on (series, timestamp); MQ-level redelivery dedupe at ingest (short idempotency window) |
| S4 | Last-N history | newest `count` samples for a series, newest first |
| S5 | Range history, paginated | cursor pagination (descending `(create_time, message_id)`); relative windows (`rangeHours`) resolved by the caller before the port |
| S6 | Single-window aggregate | AVG/MIN/MAX/SUM/COUNT over `num_value` (NULL-skipping) + sample_count |
| S7 | **Bucketed aggregate** (new port op) | same functions per fixed-width bucket over a window; empty buckets either omitted (capability `gapFill=false`) or zero-filled; bucket width from the caller |
| S8 | Count in range | per series or tenant-wide, bounded window |
| S9 | Latency source | `operate_time − create_time` per sample — the port must store **both timestamps** for the latency histogram to remain computable |
| S10 | Retention | time-based expiry per store config (today 180 d), capability-declared |
| S11 | Tenant isolation | every read carries tenant scope; negative tests in the TCK |
| S12 | Timestamp precision | microsecond `Instant` at the port; adapters may round down to the store's native precision (documented) |

**Deliberately out of the port** (with rationale):

- **Latest value** — stays in `dc3_point_latest` via the relational path (§1).
  `selectLatestPointValue` / `listLatestPointValues` move out of the TSDB port into
  the data-center's latest-value service reading the projection.
- **`message_id` uniqueness** — the ingest layer's idempotency window replaces the
  unique index; the column still rides along as a field for traceability.
- **Delete-by-range** — capability-gated (`deleteRange`): tenant offboarding needs it,
  but policies differ wildly (Timescale `DELETE`, TDengine per-child-table, IoTDB
  range delete, Influx bucket drop only). Adapters that cannot implement it declare
  `false` and offboarding falls back to store-native tooling.

## 5. Module layout

Follows the executed `dc3-mq` family shape:

```
dc3-tsdb/                     # top-level aggregator: the store-selection family
├── dc3-tsdb-core/            # Port: sample model, TsdbStore SPI, capabilities,
│                             #      cursor page, aggregate model. Zero store deps.
├── dc3-tsdb-timescale/       # Adapter: today's SQL, extracted verbatim
├── dc3-tsdb-tdengine/        # Adapter: TDengine 3.x (super tables, JDBC)
├── dc3-tsdb-influxdb/        # Adapter: InfluxDB 3.x (SQL/Flight)
├── dc3-tsdb-iotdb/           # Adapter: Apache IoTDB (session API)
└── dc3-tsdb-tck/             # Contract suite (Testcontainers per store)
```

`dc3-common-repository` shrinks to (or is absorbed by) the port: `RepositoryService`
retires; its callers (alarm windows, history pages, last-N) migrate to
`TsdbStore` + the latest-value service in Phase 1. Nothing else in the platform
touches point-value storage.

## 6. Core API

```java
package io.github.pnoker.common.tsdb;

/** Series identity — numeric platform IDs; names enriched at the app layer. */
public record SeriesKey(long tenantId, long deviceId, long pointId) {}

/** One stored sample; timestamps are epoch-micro Instants. */
public record PointValueSample(
        SeriesKey series,
        Instant deviceTime,            // create_time: device acquisition time
        Instant receiveTime,           // operate_time: server receive time (S9)
        String rawValue, String calValue,
        Double numericValue,           // projection of calValue; null for non-numeric
        String messageId, int schemaVersion,
        String driverNode, long sequence, long fencingToken, long driverId) {}

public enum AggregateFunction { AVG, MIN, MAX, SUM, COUNT }

public record TimeWindow(Instant from, Instant toExclusive) {}

public record Cursor(Instant deviceTime, String messageId) {}   // descending page anchor

public interface TsdbStore {
    String type();
    TsdbCapabilities capabilities();

    /** Batch append; store-level upsert on (series, deviceTime). Idempotent for the
        same batch. Returns accepted sample count (best-effort per store). */
    int append(List<PointValueSample> samples);

    /** S4: newest `limit` samples for one series, newest first. */
    List<PointValueSample> last(SeriesKey series, int limit);

    /** S5: one page of history, descending, starting strictly after `cursor`
        (null = newest). Filters: series optional (tenant-wide scan when absent,
        capability-gated), window required. */
    CursorPage<PointValueSample> history(HistoryQuery query, Cursor cursor, int pageSize);

    /** S6: single-window aggregate over numericValue (NULL-skipping) + sample count. */
    WindowAggregate aggregate(SeriesKey series, AggregateFunction fn, TimeWindow window);

    /** S7: per-bucket aggregates, ascending buckets; empty buckets zero-filled when
        capabilities.gapFill() else omitted. Bucket width from the caller. */
    List<BucketAggregate> bucketedAggregate(SeriesKey series, AggregateFunction fn,
                                            TimeWindow window, Duration bucketWidth);

    /** S8: sample count within the window; series optional for tenant-wide counts. */
    long count(Long tenantId, SeriesKey seriesOrNull, TimeWindow window);

    /** S10/S-delete: capability-gated time-range delete. */
    void deleteRange(SeriesKey series, TimeWindow window);
}

public record TsdbCapabilities(
        boolean gapFill,                 // zero-fill empty buckets
        boolean tenantWideScan,          // history/count without a series
        boolean deleteRange,
        OrderingGuarantee ordering,      // NONE | PER_SERIES (append order per series)
        Precision precision,             // MICRO / MILLI / NANO
        boolean backfill                 // out-of-order / late writes accepted
) {}
```

The **write orchestration stays in `dc3-common-data`** (schema validation, ingest
idempotency window, `dc3_point_latest` relational upsert) — the port is the storage
boundary, exactly like the MQ port is the broker boundary.

## 7. Per-store mapping

| Concept | TimescaleDB | TDengine 3.x | InfluxDB 3 | IoTDB |
|---------|-------------|--------------|------------|-------|
| series identity | row columns (tenant, device, point) | super table `point_value`; **tags** tenant/device/point; one child table per point | measurement `point_value`; **tags** tenant/device/point/driver | path `root.dc3.{tenant}.{device}.{point}` |
| tenant scoping | WHERE tenant_id | tag filter (indexed) | tag filter | path prefix filter |
| sample fields | columns incl. metadata | columns: raw/cal NCHAR, num DOUBLE, message metadata as columns | **fields**: raw(string), cal(string), num(float); message metadata as fields (never tags — unbounded cardinality) | measurements: raw, cal, num + metadata as separate measurements |
| write | SQL batch (today's, minus `ON CONFLICT … RETURNING` — natural upsert instead) | STABLE batch insert (schemaless or SQL) | SQL INSERT / line protocol | session batch insert |
| duplicate (series, time) policy | update-in-place (PK semantics) | duplicate timestamp: last-write-wins (update mode) | same timestamp+tags: field overwrite | same timestamp: overwrite |
| last-N | `ORDER BY create_time DESC LIMIT n` | `ORDER BY ts DESC LIMIT n` | SQL `ORDER BY time DESC LIMIT n` | SQL `ORDER BY time DESC LIMIT n` |
| single aggregate | SQL | SQL | SQL | SQL |
| bucketed aggregate | `time_bucket` | `INTERVAL(width)` | `date_bin(width, time)` | `GROUP BY ([width], time)` |
| gap fill | `generate_series` join | `INTERVAL(…) FILL(0/NULL)` | app-side fill | `FILL` variants / app-side |
| retention | drop-chunks policy (180 d) | `KEEP` per database | partition/bucket lifecycle | TTL per storage group |
| precision | micro | micro (`precision us`) | nano (port rounds to micro) | nano/ms per config |
| backfill | ✅ | ✅ | ✅ (v3) | ✅ |
| client | today's PG datasource | JDBC | Flight/HTTP (Java v3 client) | session SDK |

**InfluxDB version strategy** (open question §10.4, leaning): target **InfluxDB 3**
(Core/Enterprise SQL interface). OSS 2.x is in maintenance with Flux deprecated; a
2.x adapter would bind to a dead query language. The capability matrix marks the
adapter `influxdb (3.x)`.

**TDengine note**: child-table-per-point means one child table per (device, point)
pair — creation is lazy via schemaless insert or pre-provisioned on point enable.
Numeric IDs are stringified into table/tag names (stable mapping documented).

**IoTDB note**: numeric IDs in paths (`root.dc3.1001.2001.31`); metadata rides as
measurements; TTL per tenant subtree approximated by storage-group layout.

## 8. Capability matrix (published, per adapter)

| Capability | Timescale | TDengine | InfluxDB 3 | IoTDB |
|------------|-----------|----------|------------|-------|
| Bucketed aggregate | ✅ `time_bucket` | ✅ `INTERVAL` | ✅ `date_bin` | ✅ group-by-time |
| Gap fill | ✅ generate_series | ✅ `FILL` | ❌ app-side | ⚠️ partial |
| Tenant-wide scan | ✅ | ⚠️ super-table scan | ✅ | ⚠️ path template |
| Delete range | ✅ (DELETE, slow) | ✅ per child table | ❌ partition drop | ✅ |
| Per-series append order | ✅ | ✅ | ⚠️ (no strict order) | ✅ |
| Backfill / late writes | ✅ | ✅ | ✅ | ✅ |
| Retention | ✅ chunks | ✅ `KEEP` | ✅ partitions | ✅ TTL |
| Precision | micro | micro | nano | ms/ns |
| String values | ✅ TEXT | ✅ NCHAR | ✅ fields | ✅ TEXT |
| Embedded-in-PG mode | ✅ (default) | ❌ | ❌ | ❌ |

The startup negotiation log summarizes the active store's row, like the MQ port.

## 9. The hard parts, argued

### 9.1 Latest value stays relational (the big simplification)

`dc3_point_latest` is joined into dashboards by name, streamed to the web, and
guarded by a fencing-token tuple comparison — all OLTP concerns. Every TSDB adapter
would have to fake these (TDengine `LAST_ROW`, Influx `last()`, IoTDB `last`) with
**different** tie-breaking and no transactional link to ingestion. Keeping the
projection in PG:

- every adapter updates it identically through the existing relational path;
- the fencing guard stays exactly as strong as today;
- TSDB adapters become pure history stores — dramatically thinner and testable.

Cost: with an external store the history append and the latest upsert are no longer
one transaction. Failure modes: latest updated but history lost (impossible order —
history is written first), or history written and latest lost (batch re-delivered;
history upserts naturally, latest upsert idempotently re-runs; the ingest idempotency
window absorbs it). Acceptable under at-least-once, and precisely why S3 moves the
dedupe responsibility to ingest.

### 9.2 Idempotency: unique index → ingest window

The `message_id` unique index made writes exactly-once *in the store*. TSDBs give
(series, timestamp) upsert instead. The port therefore: (a) keeps `messageId` as a
stored field for traceability, (b) declares the duplicate policy per adapter, and
(c) the ingest layer keeps a bounded idempotency window (Redis or in-memory LRU of
recent message ids per consumer) sized to the MQ redelivery horizon — the same
pattern the driver outbox already uses on the producer side.

### 9.3 Bucketed aggregation is a first-class port op (new)

The dashboards' `time_bucket` bypass exists because the port lacks bucketing. S7
closes the gap; the timescale adapter implements it with today's SQL verbatim, the
others with their native interval functions. Gap-fill differences are a capability.

### 9.4 The dashboard reads get re-expressed (the largest single workload)

All ten bypassing statements map onto port primitives plus app-side assembly:

| Statement | Port ops |
|-----------|----------|
| countInRange / countTotal | `count` |
| timeseries | `bucketedAggregate` (+ app gap-fill if `gapFill=false`) |
| top | `count` per series, ranked in the app (tenant-wide scan capability) |
| latestStream | latest-value service (PG projection) — no TSDB |
| latencyHistogram | `history` page(s) or a dedicated `count`-per-bucket on `receiveTime−deviceTime` — see §10.2 |
| hourlyActivity | `bucketedAggregate` COUNT per hour, assembled in the app |
| silentSources | latest projection (`dc3_point_latest`) timestamps — no TSDB |
| coverageGapItems | `bucketedAggregate` COUNT + app-side gap detection |
| manager cross-schema statement | calls the data-center facade instead of reading `dc3_history` |

### 9.5 Timestamps and the value model

`Instant` at microsecond precision at the port; adapters round to native precision
(nano stores keep micros; ms stores round — documented, TCK-tolerant). The
raw/cal/numeric triple stays exactly as today — the numeric projection is computed
at ingest (unchanged code), never by the store.

## 10. TCK — the acceptance bar

`dc3-tsdb-tck`, one neutral suite per store container (timescale, tdengine, iotdb;
influx where licensable):

1. append → readback fidelity: every field of `PointValueSample` survives the round
   trip (including null `numericValue` and both timestamps)
2. last-N: newest-first ordering, exact limit
3. history cursor: stable descending pagination, cursor resumes exactly (no skips or
   duplicates across pages)
4. single-window aggregate: known fixture (incl. non-numeric samples that must be
   NULL-skipped for AVG/MIN/MAX/SUM but counted by COUNT)
5. bucketed aggregate: bucket boundaries align to epoch-anchored windows; empty
   buckets zero-filled or omitted per `gapFill`
6. count: series-scoped and (capability-gated) tenant-wide
7. duplicate (series, timestamp) re-append: exactly one sample, last-write-wins
8. backfill: older-than-newest sample is accepted and readable
9. tenant isolation: cross-tenant series reads return nothing (negative test)
10. retention: expired window disappears (timing-tolerant; capability-gated)
11. precision: micro timestamps round-trip or round predictably per declared precision
12. burst: a 5k-sample batch lands complete

## 11. Migration plan

- **Phase 1 — extract the port, zero behavior change.**
  Create `dc3-tsdb` (core + timescale adapter): move the hypertable SQL verbatim,
  adapt to natural upsert (drop `ON CONFLICT … RETURNING` idempotency in favor of
  the ingest window), retire `RepositoryService`, migrate its three callers, add
  S7/S8/cursor to the port, and **re-express all ten dashboard statements on the
  port** (§9.4). The latest-value service (`dc3_point_latest`) moves out of the
  store interface into the data-center.
  *Gate: existing unit suites green; E2E (`PostgresHypertableIT`, dashboards)
  unmodified and green.*
- **Phase 2 — TCK + TDengine adapter.** Highest CN demand; super-table mapping
  exercises the trickiest series model, so it hardens the port early.
- **Phase 3 — InfluxDB 3 + IoTDB adapters**; publish the capability matrix;
  `dc3.tsdb.type` compose profiles under `dc3/dependencies/<store>/`.
- **Throughout**: `dc3_point_latest` never moves; PG keeps it even when the
  Timescale extension is dropped.

## 12. Open questions

1. **Latency histogram home** — keep per-sample `receiveTime` storage (S9, all
   stores pay two timestamps per sample) or move receive-latency metrics to the
   observability stack (Prometheus ingest histograms) and drop S9 from the port?
   Leaning: keep S9 (it is one field, and store-native beats a second system).
2. **Idempotency window store** — in-memory LRU per data-center replica (simple,
   lost on restart — acceptable under at-least-once) or Redis-backed (survives
   restarts, one more dependency)? Leaning LRU, sized by MQ redelivery horizon.
3. **Tenant-wide scan on TDengine/IoTDB** — super-table scan and path-template
   queries exist but cost; should `top`/`hourlyActivity` be capability-degraded to
   "per-series loop from the app" when `tenantWideScan=false`?
4. **InfluxDB edition** — 3.x Core (free, SQL) vs Enterprise; confirm the v3 Java
   client's batch and SQL coverage before committing the adapter (Phase 3 spike).
5. **Point disablement** — when a point is disabled/deleted, should the adapter
   drop its child table / series data? (Today nothing deletes history.) Leaning:
   no automatic deletion; `deleteRange` exists for explicit offboarding.
6. **Store-to-store migration tooling** — a one-shot dual-read/dual-write mode, or
   an offline copy CLI (`dc3-tsdb-copy`)? Needed before real deployments switch
   stores with history in place. Leaning offline CLI, Phase 3.
7. **`dc3-common-repository` fate** — absorbed into `dc3-tsdb-core` and deleted, or
  kept as the app-facing facade delegating to the port? Leaning: absorb and delete
  (no compatibility aliases, per house style).

## 13. Relationship to storage-abstraction.md

This document supersedes §4 (Layer 2) of
[`storage-abstraction.md`](./storage-abstraction.md): the sketch there assumed the
store stayed PG-adjacent; the executed MQ experience and this analysis widen the
target to standalone stores with a TCK gate. Layers 1 (relational dialect) and 3
(vector placeholder) are unaffected.
