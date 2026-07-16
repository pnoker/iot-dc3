---
title: "Data Plane: How a Point Value Lands in Storage"
---

<script setup>
import DataPlaneClassDiagram from '../../.vitepress/theme/components/DataPlaneClassDiagram.vue'
import DataPlaneSequenceDiagram from '../../.vitepress/theme/components/DataPlaneSequenceDiagram.vue'
</script>


# Data Plane: How a Point Value Lands in Storage

A raw register read on a device has a long way to go before it becomes a queryable value. It passes through driver
normalization, the message bus, and the data center, and only then is it written to the time-series store. This page
follows that journey end to end: the exchanges and queues on the path, how the consumer persists the message, the layers
the value moves through, and how the latest-value cache is hit on reads. By the end you'll know every hop from device to
API, and the hard constraints that apply to aggregate queries.

> You are here: you already know Point and PointValue from [Core Concepts](../introduction/concepts) and want to see the
> data flow. For the reverse read/write commands, see the [Command Plane](./command-plane).

## The Journey of a Value

The data flow is a one-way **south-to-north** link. Drivers poll their devices periodically, wrap each read of a point
into a `PointValue`, and publish it to RabbitMQ's value exchange via `DriverSenderService.pointValueSender()`. The data
center `dc3-center-data` consumes the queue, writes each value to TimescaleDB, and at the same time pushes it into a
local Caffeine cache for hot reads. The whole link is **asynchronous** — once the driver publishes, it does not wait for
the data center to confirm the write. Durable delivery on the bus plus manual ack make sure nothing is lost.

<DataPlaneDiagram lang="en" />

The driver's routing key is `dc3.r.value.point.` plus its own service name (`driverProperties.getService()`, e.g. the
service name you configured for a `dc3-driver-virtual` instance). The data center's queue binds with the wildcard
`dc3.r.value.point.*`, so a single queue receives values from every driver. When publishing, `pointValueSender()` fills
in `driverId` and `tenantId` from `DriverMetadata` if the message doesn't already carry them. It uses
`PointValueCorrelation` (a random UUID plus deviceId and pointId) as correlation data, paired with publisher confirms to
track delivery.

## RabbitMQ Topology: Value Exchange, Point Queue, Dead Letters

The value channel is a single **topic exchange** `dc3.e.value`, with one **durable queue** `dc3.q.value.point` bound
under it. The queue is declared in the data center's `DataTopicConfig`:

```java
QueueBuilder.durable(RabbitConstant.QUEUE_POINT_VALUE)   // dc3.q.value.point
    .ttl(604800000)                                       // 7 days = 604800000 ms
    .deadLetterExchange(RabbitConstant.TOPIC_EXCHANGE_POINT_VALUE_DEAD) // dc3.e.point_value_dead
    .deadLetterRoutingKey("#")
    .build();
```

Three things to know:

- **Durable + 7-day TTL**: the queue is `durable`, and messages are stamped `PERSISTENT` on publish (`RabbitConfig`'s
  `BeforePublishPostProcessor` sets `MessageDeliveryMode.PERSISTENT` on every message). A message stays in the queue for
  at most 7 days (`604800000` ms); if it isn't consumed before the timeout, it dead-letters.
- **Dead-letter fallback**: messages that time out or are `reject`ed flow to the dead-letter exchange
  `dc3.e.point_value_dead` (dead-letter queue `dc3.q.point_value_dead`). They aren't dropped silently — you can inspect
  them there.
- **Wildcard binding**: the queue binds to `dc3.e.value` with `dc3.r.value.point.*`, so one queue collects point values
  from all driver instances.

::: info Exchange/queue names carry an environment prefix
Constants in `RabbitConstant` like `dc3.e.value` and `dc3.q.value.point` are prefixed with an environment `tag` at
assembly time. This page uses the stable suffix names with the prefix stripped, so you can find them by name in the
RabbitMQ management console.
:::

## Consumer: How PointValueReceiver Persists

The data center's `PointValueReceiver` listens on the point queue with
`@RabbitListener(queues = "#{pointValueQueue.name}")` and deserializes the payload into `PointValueBO` (JSON via
`JacksonJsonMessageConverter`). It uses **manual ack**:

- **Validation**: if `pointValueBO` is null or missing `deviceId` → `RabbitAckUtil.reject` (`basicReject` without
  requeue) → dead letters.
- **Persistence**: one of two paths, picked by the inbound rate. Below `POINT_BATCH_SPEED` (default 100), it calls
  `pointValueService.save(pointValueBO)` for **immediate persistence**. Above the threshold, it hands the value off to
  `PointValueJob` for **batch processing**. The rate is `speed = count / interval`, where `POINT_BATCH_INTERVAL` (
  default `5`, in **seconds**, Quartz `IntervalUnit.SECOND`) is the divisor — not the flush interval. `PointValueJob`
  runs on a Quartz schedule and flushes the entire accumulated buffer each time it fires, regardless of buffer size.
  There is no batch-size trigger and no first-in-first-flush.
- **Acknowledgment**: success → `RabbitAckUtil.ack`. A thrown exception → `RabbitAckUtil.nack(requeue=true)`, which
  requeues for retry.

::: info Consumer concurrency is the default tier, not the high-throughput tier
`PointValueReceiver` doesn't specify a `containerFactory`, so it uses the default listener container factory:
`concurrentConsumers=2`, `maxConcurrentConsumers=8`, `prefetchCount=10`, `AcknowledgeMode.MANUAL`. `RabbitConfig` also
exposes a high-throughput factory, `highThroughputRabbitListenerContainerFactory` (`concurrent=4`, `max=32`,
`prefetch=100`), but no listener currently opts in. If you need higher throughput, add
`containerFactory="highThroughputRabbitListenerContainerFactory"` to the `@RabbitListener` explicitly. The code is
authoritative: `dc3-common-rabbitmq/.../RabbitConfig.java`.
:::

`pointValueService.save()` does two things. First it writes the value into the local Caffeine latest-value cache (
`PointValueLocalCache`, key = `REAL_TIME_VALUE_KEY_PREFIX + tenantId + "." + deviceId + "." + pointId`, dot-separated
and prefixed) and into the time-series store. Then it hands the value straight to the alarm engine for evaluation.

## Model Transformation: Six Faces Across the Layers

The same "point value" changes form six times along the link, and each layer owns its own shape and serialization. The
part that trips people up: **`PointValue` and `PointValueBO` are not the same class.** The first is the sending bean on
the driver side; the second is the message/business object on the consumer side. They belong to different layers.

<DataPlaneClassDiagram lang="en" />

Layer by layer:

- **`ReadPointValue`** (driver): the raw reading returned by the driver protocol layer's `read()`, carrying device and
  point context.
- **`CalculatedPointValue`** (driver): the result of applying linear scaling/projection (`baseValue`/`multiple`, etc.)
  to the raw value. It computes `finalValue` (the engineering-value string) and `numericValue` (the numeric projection,
  which may be empty).
- **`PointValue`** (driver sending bean): `new PointValue(readPointValue)` runs `calculate()` internally, filling in
  `rawValue`/`calValue`/`numValue` and `createTime` (the moment of acquisition). This is the payload published to
  RabbitMQ.
- **`PointValueBO`** (message/business): the object the data center deserializes from the queue. It carries `tenantId`,
  `createTime`, and `operateTime`, and is the input to persistence and alarm evaluation.
- **`PointValueDO`** (persistent): the database shape written to `dc3_point_value`, with `num_value` as a nullable
  `DOUBLE`.
- **`PointValueVO`** (API): the shape returned to clients by read endpoints. It exposes `deviceId`/`pointId`/`rawValue`/
  `calValue`/`numValue`/`createTime`/`operateTime`/`hasLatestValue`/`driverId`/`tenantId`.

## Storage: dc3_point_value Is a TimescaleDB Hypertable

Values land in a TimescaleDB **hypertable**, `dc3_history.dc3_point_value` (it lives in the `dc3_history` schema;
`search_path` includes `dc3_history, public`, so this page and queries often shorten it to `dc3_point_value`). It is
partitioned along two dimensions: the time dimension `create_time` with one chunk per **1 day**, and the device
dimension `device_id` with **16** hash buckets.

```sql
SELECT create_hypertable('dc3_point_value', by_range('create_time', INTERVAL '1 day'));
SELECT add_dimension('dc3_point_value', by_hash('device_id', 16));
```

To control storage and query cost, this hypertable also has two data-lifecycle policies:

```sql
-- chunks older than 7 days are compressed automatically (segmented by tenant/device/point, ordered by create_time)
ALTER TABLE dc3_point_value SET (timescaledb.compress,
    compress_segmentby='tenant_id,device_id,point_id', compress_orderby='create_time DESC');
SELECT add_compression_policy('dc3_point_value', INTERVAL '7 days');
-- data older than 180 days is dropped automatically
SELECT add_retention_policy('dc3_point_value', INTERVAL '180 days');
```

::: tip Compression and retention are on by default
Compressing after 7 days cuts disk usage significantly (compressed chunks stay queryable, just write-restricted); the
180-day retention policy drops expired chunks automatically. If your workload needs a longer horizon, tune both
intervals at deployment.
:::

Key columns and indexes:

| Column         | Type                            | Description                                                             |
|----------------|---------------------------------|-------------------------------------------------------------------------|
| `raw_value`    | `TEXT NOT NULL`                 | Raw value read from the device                                          |
| `cal_value`    | `TEXT NOT NULL`                 | Value after scaling/projection                                          |
| `num_value`    | `DOUBLE PRECISION` **nullable** | Numeric projection of `cal_value`; `NULL` for non-numeric/JSON payloads |
| `create_time`  | `TIMESTAMPTZ NOT NULL`          | **Acquisition moment** (driver-side read)                               |
| `operate_time` | `TIMESTAMPTZ NOT NULL`          | **Persistence moment** (data-center write)                              |

The primary time-series index `idx_point_value_ts_lookup` is `(tenant_id, device_id, point_id, create_time DESC)`. Both
tenant-isolated latest-value lookups and time-window scans use it. There's also a **partial index**
`idx_point_value_num_time ... WHERE num_value IS NOT NULL`, dedicated to numeric aggregation.

::: warning create_time and operate_time are two distinct moments
`create_time` is when the driver read the value; `operate_time` is when the data center wrote it to the store. The two
are kept apart on purpose — their difference is the acquisition-to-persistence pipeline latency, which dashboards use to
measure link delay. On persistence, `save()` always rewrites `operate_time` to the current time, and fills in the
current time for `create_time` only when it's missing.
:::

::: danger num_value is nullable: aggregate queries must use num_value IS NOT NULL
`dc3_point_value.num_value` is `NULL` for non-numeric or JSON payloads. Any aggregation such as `AVG`/`SUM`/`MAX`/`MIN`
**must** add `WHERE num_value IS NOT NULL`. Without it, null values from string-typed points get mixed in and skew the
result. The partial index `idx_point_value_num_time` also covers only rows where `num_value IS NOT NULL` — skip the
predicate and you miss the index too.
:::

## Message Reliability and Post-Persistence Alarms

The data plane's no-loss guarantee rests on three overlapping mechanisms, all wired up in the shared `RabbitConfig`:

- **Durable delivery**: the publish pre-processor stamps every message `PERSISTENT`, which — together with the `durable`
  queue — survives a broker restart.
- **Manual ack**: the consumer only `ack`s after a successful write. On exception it `nack(requeue=true)` to retry; on
  validation failure it `reject`s to dead letters. A message is never silently swallowed.
- **Publisher confirms**: `rabbitTemplate` registers a confirm callback that logs an error on NACK. The correlation data
  `PointValueCorrelation` lets you map a confirmation back to the specific device and point.

Once a value is persisted, `PointValueServiceImpl.save()` calls
`alarmRuleTriggerService.processPointValue(pointValueBO)` right away, evaluating alarm rules against that value *
*synchronously** — not on a separate delayed link. For the rules, the state machine, and notification channels,
see [Alarms and Notifications](../operation/alarms).

## Reading the Latest Value: Cache First, Time-Series Store on Miss

The write path pushes the latest value into Caffeine at the same time it writes the store; the read path hits that cache
first. `POST /api/v3/data/point_value/latest` enters `PointValueServiceImpl.latest()`, which batch-queries the cache via
`pointValueLocalCacheService.selectLatestPointValue(tenantId, deviceId, pointIds)`, collects the pointIds that missed,
and then falls back to TimescaleDB in a single pass (`repositoryService.listLatestPointValues`) to fill in the rest.

<DataPlaneSequenceDiagram lang="en" />

The historical-range query `POST /api/v3/data/point_value/list` skips the cache. It scans the time-series store directly
via `repositoryService.listPagePointValue(query)`, filtered by `startTime`/`endTime`. Both read endpoints are guarded by
`@PreAuthorize("@perm.can('point_value', 'list')")`, return a paginated `PointValueVO`, and force tenant context through
`PointValueQuery` — you can't pull another tenant's data.

## How To: Read Latest Values and History

Both read endpoints are forwarded through the gateway `dc3-gateway` (:8000). Protected endpoints must carry the three
auth headers `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token` (for the salt-fetch + token-issue flow,
see [Quick Start](../quickstart/)).

::: code-group

```bash [latest value curl]
curl -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H "X-Auth-Tenant: default" \
  -H "X-Auth-Login: <login>" \
  -H "X-Auth-Token: <token>" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": 1024, "pointId": 2048, "current": 1, "size": 10}'
```

```bash [historical range curl]
curl -X POST http://localhost:8000/api/v3/data/point_value/list \
  -H "X-Auth-Tenant: default" \
  -H "X-Auth-Login: <login>" \
  -H "X-Auth-Token: <token>" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": 1024, "pointId": 2048, "current": 1, "size": 50,
       "startTime": "2026-06-22T00:00:00", "endTime": "2026-06-22T23:59:59"}'
```

:::

The response is a paginated `PointValueVO`. Each record holds one latest (or in-range) value for a point:

```json
{
  "code": "200",
  "data": {
    "current": 1,
    "size": 10,
    "total": 1,
    "records": [
      {
        "deviceId": 1024,
        "pointId": 2048,
        "rawValue": "23.5",
        "calValue": "23.5",
        "numValue": 23.5,
        "hasLatestValue": true,
        "createTime": "2026-06-22T08:30:00",
        "operateTime": "2026-06-22T08:30:01"
      }
    ]
  }
}
```

::: tip Field names follow the actual response
The values above are examples. `PointValueVO`'s exposed fields (`rawValue`/`calValue`/`numValue`/`createTime`/
`operateTime`, and so on) are mapped from `PointValueDO` by a MapStruct builder. For integration, defer to the actual
JSON the gateway returns.
:::

## Constraints and Boundaries

- **Aggregations must carry `num_value IS NOT NULL`**: see the danger callout above. It's a hard prerequisite for
  correct numeric statistics.
- **`PointValue` ≠ `PointValueBO`**: don't swap the driver sending bean for the business object across layers — their
  field sets and serialization contexts differ.
- **Consumer concurrency is the default tier**: the point queue runs on the default listener factory (prefetch=10,
  concurrency 2–8). The high-throughput factory exists but isn't enabled; opt in explicitly when you need it.
- **Tenant isolation is enforced at the controller layer**: read endpoints carry tenant context via `PointValueQuery`,
  and after fetching, the controller layer's `requireTenant` / `filterTenant` checks the tenant; cross-tenant access
  returns no data.
- **Dead letters aren't loss**: values that time out or get rejected land in `dc3.e.point_value_dead`. When
  troubleshooting, look in the dead-letter queue.

## Further Reading

- [Command Plane](./command-plane) — how the reverse read/write commands are dispatched, acknowledged, and queried for
  status
- [Domain Model](./domain-model) — the DO/BO/VO layering and field details of Point / PointValue
- [Alarms and Notifications](../operation/alarms) — how alarm rules are evaluated after persistence and how
  notifications are delivered
- [Time-Series & Streaming](../foundations/data-pipeline) — the general principles of time-series databases and stream
  processing
