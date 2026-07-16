---
title: Time-Series Data & Stream Processing
---

<script setup>
import DataPipelineFlowDiagram from '../../.vitepress/theme/components/DataPipelineFlowDiagram.vue'
import DataPipelineIngestDiagram from '../../.vitepress/theme/components/DataPipelineIngestDiagram.vue'
</script>


# Time-Series Data & Stream Processing

The real test of the IoT platform layer is **how to store, compute over, and query an endless stream of point values**.
This layer is neither the device nor the business application — it is the "data backbone" wedged in between: thousands
of timestamped readings pour in every second, they have to be written and retained, and at the same time pulled out on
demand by dashboards, alarms, and AI. By the end of this chapter you will understand why time-series data needs storage
and pipelines built for it, know what batch and stream processing are each good for, and be able to map this general
paradigm onto IoT DC3's [data plane](../architecture/data-plane) — the link where a point value is delivered
asynchronously through RabbitMQ, lands in a TimescaleDB hypertable, and then enters the latest-value cache.

## What This Layer Is / Why It Exists

In the four-layer reference architecture, the platform layer's job is to "store, manage, and compute." Once the
perception layer produces physical quantities and the network layer delivers them, the platform layer faces a workload
utterly unlike a traditional business system: **time-series data**.

Time-series data has a few shared traits, and understanding them is understanding why you can't just force-fit it into
an ordinary relational table:

- **High write, low update**: data is almost purely append-only — once a reading lands, it is essentially never
  modified. Write throughput is the dominant pressure, and the strengths of a traditional database — transactions,
  row-level updates, foreign keys — are barely used here.
- **Naturally time-indexed**: every record carries a timestamp, and the overwhelming majority of queries are "the values
  of some point on some device over some time range." Time is the primary query dimension, not an optional column.
- **Recent-hot, old-cold**: freshly acquired values are read constantly (live dashboards, current alarm evaluation),
  while values from months ago are scanned only in the occasional trend analysis. Access heat decays fast over time.
- **Value decays with precision**: nobody cares about millisecond detail from a year ago — an hourly or daily
  mean/extreme is enough. This is exactly where **downsampling** and **retention policies** earn their keep.

Store this data in an ordinary relational table and the problems surface quickly: once a single table swells past
hundreds of millions of rows, time-range scans get slower and slower; B-tree indexes keep splitting and bloating under
constant appends; and with no built-in expiry, old data can only be cleaned by hand-written scripts. What the platform
layer needs is a storage and processing paradigm **tailored for "time + append + hot/cold tiering"** — and that is
precisely why time-series databases and stream-processing pipelines exist.

## Key Technologies & Trade-offs

Getting time-series data right relies on a pipeline, not a single component. A typical IoT data pipeline is a chain of
four stages: the acquisition side publishes readings to a **message bus** for decoupling, the consumer side **persists**
the messages into time-series storage, and on top of that storage a **query** layer serves applications and algorithms.

<DataPipelineFlowDiagram lang="en" />

A few key technologies and trade-offs sit along this pipeline:

**Time-series databases: the hypertable and partitioning idea.** The common approach across solutions (TimescaleDB,
InfluxDB, TDengine) is to **automatically slice one big logical table into many small pieces**. Take TimescaleDB's *
*hypertable**: to the user it's just a normal table, read and written with standard SQL; underneath it auto-slices data
by a time dimension (plus a device/tag dimension) into individual **chunks**. The payoff is obvious — a query with a
time range only scans the relevant chunks (chunk pruning) instead of the whole table; writes always land on the "newest"
chunk, so the index stays local rather than inserting all over a billion-row table; and expired data can be dropped
whole-chunk, orders of magnitude faster than row-by-row `DELETE`. Partitioning is the foundation of every bit of
time-series performance.

**Downsampling and retention: trade precision for cost.** Since data value decays over time, there's no need to pay a
high-precision storage bill for old data. Two complementary strategies: a **retention policy** automatically drops raw
data past a certain age; **downsampling / continuous aggregates** pre-roll high-frequency raw values into low-frequency
summaries (e.g. per-minute raw → hourly mean/extreme), keeping the long-term trend while shrinking the footprint
dramatically. Layer **columnar compression** on top and cold chunks usually compress to a fraction of their original
size. Together, these three make "keep three years of data" economically viable.

**The message bus: decoupling and backpressure.** If the acquisition side and the persistence side are wired directly, a
jitter at either end drags the other down — a burst from devices instantly overwhelms the database, and one slow query
in the database blocks the acquisition threads. Inserting a **message bus** in between (RabbitMQ, Kafka, an MQTT broker,
etc.) decouples the two ends: producers just publish, consumers drain at their own pace. This brings in **backpressure
**: when downstream can't keep up, messages accumulate in the queue instead of being dropped, and the risk is controlled
with **prefetch limits**, **consumer concurrency**, and **dead letters plus TTL** — accumulation is bounded, timeouts
dead-letter, and consumption scales horizontally. The bus lets the pipeline "elastically absorb" traffic spikes instead
of meeting them head-on.

**Batch vs. stream: two computing postures.** The same time-series data admits two algorithmic styles:

- **Batch**: accumulate a batch, then compute/write it all at once. High throughput and low per-unit overhead, but
  latency — suited to "hourly reports" or "daily trends" where latency doesn't matter, and to batched persistence on the
  write side to amortize I/O cost.
- **Stream**: compute as soon as data arrives. Low latency, able to finish alarm evaluation and sliding-window
  aggregation as a value is persisted (or even before) — suited to "alert the instant temperature exceeds a limit" or "
  live dashboard," anything that demands sub-second response.

These aren't an either/or; they're **two forks of the same pipeline**: the hot path goes through stream processing for
real-time, the cold path through batch for throughput. In practice a common combination is "batch the writes to
amortize, evaluate alarms in stream for immediacy."

## Engineering Notes

A few lessons worth keeping when you take the paradigm above into production:

- **Write throughput is always the first constraint**. A time-series system's capacity starts with "how many points per
  second can it write." Batched writes, sensible chunk sizes, and avoiding heavy index maintenance during writes all
  yield to write throughput. Read optimization comes second.
- **Distinguish "acquisition moment" from "persistence moment"**. When a device read a value and when it was written to
  the store are two different things, and their difference is the pipeline latency. Store both timestamps so you can
  order correctly and monitor link delay.
- **Beware nullable values in aggregations**. Time-series tables often mix in non-numeric payloads — strings, JSON —
  whose numeric column is null. Run `AVG`/`SUM`/`MAX` without explicitly filtering nulls and the result is silently
  skewed — a pit people fall into repeatedly.
- **Plan hot/cold tiering early**. Compression and retention policies are best defined at table-creation time; bolting
  them on after data has grown into the billions is a painful retrofit. Compressed chunks are usually read-only, so
  confirm your query path still works.
- **Tune backpressure parameters to the load**. Prefetch count, consumer concurrency, queue TTL — these aren't constants
  you guess: prefetch too large wastes memory, too small can't saturate throughput; concurrency too high contends for DB
  connections, too low can't keep up with consumption. Start with conservative defaults, then tune against measured
  traffic.
- **Dead letters aren't a trash can**. Messages that time out, fail to parse, or repeatedly fail processing land in the
  dead-letter queue, and someone needs to watch them and have a record — not let them pile up or vanish silently.

## How It Lands in IoT DC3

IoT DC3's [data plane](../architecture/data-plane) is one concrete implementation of the general pipeline above. A point
value goes from device to queryable through exactly those four stages — "acquisition → message bus → consume & persist →
cache/query":

<DataPipelineIngestDiagram lang="en" />

**Message bus: asynchronous RabbitMQ delivery + 7-day TTL + dead letters.** A point value a driver acquires is not
written straight to the database; it is published to RabbitMQ's topic exchange `dc3.e.value`, and the data center
`dc3-center-data`'s durable queue `dc3.q.value.point` collects every driver's values via the wildcard
`dc3.r.value.point.*`. This queue declares a **7-day TTL** (`604800000` ms) and a **dead-letter exchange**
`dc3.e.point_value_dead`: a message stays in the queue for at most 7 days, and on timeout or reject it dead-letters
rather than being silently dropped. This is exactly "message-bus decoupling + backpressure + dead-letter fallback"
realized in DC3.

::: info Consumer concurrency is the default tier, not the high-throughput tier
The point queue's consumer `PointValueReceiver` doesn't specify a `containerFactory`, so it runs on the **default
listener container factory**: `concurrentConsumers=2`, `maxConcurrentConsumers=8`, `prefetchCount=10`, manual ack.
`RabbitConfig` also exposes a high-throughput factory `highThroughputRabbitListenerContainerFactory` (`concurrent=4`,
`max=32`, `prefetch=100`), but **no listener currently opts in** — the high-throughput factory exists and is off by
default. When you need it, add `containerFactory` to the `@RabbitListener` explicitly.
:::

**Batched persistence: one of two paths by inbound rate.** The data center doesn't blindly write row by row. It splits
by inbound rate — below `POINT_BATCH_SPEED` (default `100`) it persists immediately; above the threshold it hands off to
a Quartz scheduled batch job, with `POINT_BATCH_INTERVAL` (default `5`, in **seconds**) participating in the rate
calculation. This is exactly the engineering trade-off of "batch the writes to amortize I/O, write immediately at low
volume for real-time."

**Time-series storage: the TimescaleDB hypertable `dc3_point_value`.** Point values land in the TimescaleDB **hypertable
** `dc3_point_value` (in the `dc3_history` schema). It puts the partitioning idea into practice — two-dimensional
partitioning by the time dimension `create_time` with **one chunk per 1 day** and the device dimension `device_id` with
**16 hash buckets** — and carries two data-lifecycle policies: **chunks older than 7 days are compressed columnar
automatically**, and **data older than 180 days is dropped automatically**. Hot/cold tiering, compression, and retention
are all out-of-the-box hard facts here.

::: danger num_value is nullable: aggregate queries must use num_value IS NOT NULL
`dc3_point_value.num_value` (`DOUBLE PRECISION`) is `NULL` for non-numeric or JSON payloads. Any `AVG`/`SUM`/`MAX`/`MIN`
aggregation **must** add `WHERE num_value IS NOT NULL`, or nulls from string-typed points get mixed in and skew the
result; the hypertable also has a partial index covering only `num_value IS NOT NULL`, so skip the predicate and you
miss the index too. This is precisely "beware nullable values in aggregations" from the Engineering Notes, made concrete
in DC3.
:::

**Latest-value cache: the Caffeine hot path.** As the write path persists, it simultaneously pushes the latest value
into a local **Caffeine latest-value cache**; a latest-value read (`point_value/latest`) hits that cache first and falls
back to TimescaleDB only on a miss. The historical-range query (`point_value/list`) skips the cache and scans the
hypertable directly. This is "recent-hot, old-cold" realized on the read path: the hot latest value goes through an
in-memory cache, and cold data goes through a time-range scan of the time-series store.

The link's reliability rests on three overlapping mechanisms: every message is stamped `PERSISTENT` before publish (
paired with the `durable` queue, it survives a broker restart), the consumer uses manual ack (ack only on success,
requeue on exception, dead-letter on validation failure), and publisher confirms (a confirm callback tracks delivery).
Once a value is persisted, it is also handed **synchronously** to the alarm engine for evaluation — DC3's embodiment of
stream processing's "compute as soon as data arrives." For the full link, model transformation, and read-endpoint
examples, see the [data plane](../architecture/data-plane).

## Further Reading

- [Edge & Cloud Architecture](./edge-cloud) — the upstream of this pipeline: whether acquisition runs at the edge or in
  the cloud, and how drivers push down
- [Data Intelligence & AIoT](./aiot) — the downstream of this pipeline: how persisted data is analyzed and consumed by
  large models
- [IoT Technology Overview](./) — the four-layer reference architecture and DC3's layer-by-layer mapping
- [Data Plane](../architecture/data-plane) — the full link of a point value from device to storage in DC3, and its hard
  constraints
- [Services & Topology](../architecture/services) — where the data center, message bus, and time-series store sit in
  DC3's service map
