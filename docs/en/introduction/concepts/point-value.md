---
title: PointValue
---

<script setup>
import PointValueRelationDiagram from '../../../.vitepress/theme/components/PointValueRelationDiagram.vue'
import PointValueFlowDiagram from '../../../.vitepress/theme/components/PointValueFlowDiagram.vue'
</script>

# PointValue

> **A point value is a single snapshot of one [point](./point) at one instant in time**—"the outlet temperature of pump
> #3 was 25.3℃ at exactly 14:05:03." It belongs to `device + point`, carries a timestamp, is collected upstream by
> a [driver](./driver), and lands in the TimescaleDB time-series store.

A point value answers "what is this quantity **now / at that moment**." A [point](./point) is the "column definition" in
the template (this kind of device has an "outlet temperature" measurement); a point value is the "reading" of that
column across rows over time—one point produces thousands upon thousands of point values as time passes.

It is the complementary upstream counterpart to an [event](./event): a point value is a periodic sample of a *
*continuous value** (a temperature reading every second), an event is a discrete **business occurrence** ("
over-temperature alarm" fired once). For a door-access device, "temperature = 25.3℃" is a point value; "door was forced
open" is an event.

Don't conflate a point value with "the point's current value": point values are individual **history records**—each
sample appends a new row, append-only, never updated; "current value" is simply the result of querying the latest point
value by `device_id + point_id`.

## rawValue vs calValue

A single point value retains two values at once:

- **`rawValue`**—the data the driver reads from the device verbatim, with no conversion. For example, the raw register
  code `6400` returned by a 4-20mA transmitter.
- **`calValue`**—the human-readable engineering value computed from the conversion rule configured on the point (
  `baseValue` / `multiple`, etc.). For example, `6400` converts to `25.3` (℃).

Keeping the raw value matters for traceability and recomputation: if the conversion rule changes, the historical
`rawValue` is still there, so the new engineering value can be recomputed.

## Key Fields

Point value `PointValueBO` (table `dc3_point_value`):

| Field            | Type          | Meaning                                                                                                         |
|------------------|---------------|-----------------------------------------------------------------------------------------------------------------|
| `deviceId`       | Long          | Owning [device](./device)                                                                                       |
| `pointId`        | Long          | Owning [point](./point)                                                                                         |
| `rawValue`       | String        | Raw value, returned by the device verbatim, not converted                                                       |
| `calValue`       | String        | Converted engineering value, human-readable                                                                     |
| `numValue`       | Double        | Numeric projection of `calValue`; populated when it parses cleanly as a double, NULL for booleans / JSON / text |
| `hasLatestValue` | Boolean       | Whether the latest-value query returned a real sampled value                                                    |
| `driverId`       | Long          | The [driver](./driver) that collected the data                                                                  |
| `tenantId`       | Long          | Owning [tenant](./tenant)                                                                                       |
| `createTime`     | LocalDateTime | Collection / write time, i.e. the timestamp of this snapshot                                                    |
| `operateTime`    | LocalDateTime | Last operation time                                                                                             |

::: tip Why numValue exists
Both `rawValue` and `calValue` are `String` (they must hold numbers, booleans, JSON, and text alike). `numValue` is a
copy of `calValue` when it parses as a number, dedicated to AVG / MIN / MAX / SUM and time-series aggregation queries so
they can use a numeric index and skip the per-row cast. For non-numeric points (digital, string, JSON), `numValue` is
NULL, and aggregate queries use `num_value IS NOT NULL` to skip them outright.
:::

## Relationship to Other Concepts

<PointValueRelationDiagram lang="en" />

- A point value is located jointly by `deviceId + pointId`: **which device's** **which measurement**.
- A [point](./point) gives the column definition (type, unit, conversion rule); a point value is the runtime reading of
  that column, row by row.
- An [event](./event) sits alongside point values on the upstream link—one continuous, one discrete, complementary.

## Collection and Upstream Flow

<PointValueFlowDiagram lang="en" />

The driver reads `rawValue` from the device, computes `calValue` via the point's conversion rule, fills `numValue` when
it parses as a number, and ships it upstream together with `deviceId` / `pointId` / `driverId` / `tenantId` /
`createTime`; the Data Center **appends** it to the `dc3_point_value` hypertable (chunked by `create_time` at 1-day
intervals plus hash-partitioned by `device_id`, with compression and a 180-day retention policy maintained automatically
by TimescaleDB).

::: warning Point values are append-only—mind the retention policy
`dc3_point_value` is an append-only history stream: each sample inserts a new row, with no UPDATE. Querying "current
value" means taking the latest row, not reading some field that gets overwritten. It also carries a 180-day retention
policy—data past that age is cleaned up automatically, so archive ahead of time if you need long-term retention.
:::

## Example

The outlet-temperature point (`pointId=2048`) of pump #3 (`deviceId=1024`) has a conversion rule mapping the 4-20mA raw
code to 0-100℃. At 14:05:03 the driver (`driverId=8`) reads register raw code `6400` and converts it to `25.3`:

```text
PointValueBO{
  deviceId: 1024, pointId: 2048, driverId: 8, tenantId: 1,
  rawValue: "6400",      // returned by the device verbatim
  calValue: "25.3",      // converted engineering value (℃)
  numValue: 25.3,        // aggregatable as a number
  createTime: 2026-06-24T14:05:03
}
```

A second later another reading of `25.4` arrives… and so on, accumulating into a time-series stream. Querying "current
outlet temperature" = take the latest row for `device_id=1024, point_id=2048`; querying "today's average temperature" =
run AVG over `num_value` within the time window.

## Query API

| Method | Path                                                  | Description                                                       |
|--------|-------------------------------------------------------|-------------------------------------------------------------------|
| POST   | `/point_value/latest`                                 | Paged query of the latest value per point                         |
| POST   | `/point_value/list`                                   | Paged query of point value history                                |
| GET    | `/point_value/list_history_by_device_id_and_point_id` | Query history by `device_id + point_id` (`count` defaults to 100) |

## Further Reading

- [Point](./point) — a point value is the runtime reading of a point
- [Event](./event) — complementary: continuous value vs discrete occurrence
- [Core Concepts Overview](../concepts) — back to the concept map
- [Data Plane](../../architecture/data-plane) — exchange / queue / TimescaleDB details of the upstream collection link
