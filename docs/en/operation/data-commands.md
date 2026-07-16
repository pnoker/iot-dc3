---
title: Data and Commands
---

<script setup>
import DataCommandsFlowDiagram from '../../.vitepress/theme/components/DataCommandsFlowDiagram.vue'
import DataCommandsStateDiagram from '../../.vitepress/theme/components/DataCommandsStateDiagram.vue'
</script>


# Data and Commands

Once a device is connected, there are two things to verify: can you read its values, and can you push commands to it?
This page covers both. First you'll use real `curl` calls to read a point's latest value and history, then issue a write
command and poll for its result. The last section covers what happens at the edges: offline devices, read-only points,
and failed writes.

> You are here: you've already [connected your first device](../quickstart/first-device) and values are flowing into
> storage. By the end of this page you'll be able to query values, issue commands, and tell whether a command truly
> succeeded — on your own.

## Two opposite pipelines

Data and commands run in opposite directions, but on your side both are just HTTP calls through the Gateway (
`dc3-gateway`, `8000`).

**The data flow goes device → you.** The driver packages each acquisition into a PointValue, sends it through RabbitMQ's
`dc3.e.value` exchange to the Data Center (`dc3-center-data`), and stores it in TimescaleDB's `dc3_point_value`
hypertable. You read the latest value through `/api/v3/data/point_value/latest` and the historical range through
`/api/v3/data/point_value/list`. This is a **query over facts that have already happened** — no side effects, safe to
retry.

**The command flow goes you → device.** You `POST` a read or write command to the Data Center, which records it in
`dc3_point_command_history` (status `PENDING`), then routes it through the `dc3.e.point_command` exchange to the target
driver. After the driver runs it against the device, the result comes back through `dc3.e.point_command_result`. The
command interface **returns a `commandId` immediately** — actual success or failure is determined by polling with that
ID. This is an **asynchronous write path with side effects**.

<DataCommandsFlowDiagram lang="en" />

For the field-level details, model transformations, and RabbitMQ topology of each pipeline,
see [Data Plane](../architecture/data-plane) and [Command Plane](../architecture/command-plane). This page only covers
how to use them.

## Reading data: latest values and history

The Data Center exposes two read interfaces. Both are `POST` (the body carries pagination and filter conditions) and
each returns `Page<PointValueVO>`. Every value carries `deviceId`, `pointId`, `rawValue` (raw value), `calValue` (
engineering value), `numValue` (numeric projection, nullable), plus `createTime` and `operateTime`. Both are
tenant-isolated (`tenantId`) and sit under the permission code `point_value:list`. Protected interfaces require the auth
headers `X-Auth-Tenant`, `X-Auth-Login`, and `X-Auth-Token` — see
the [API documentation](../development/api-documentation) for how to obtain them.

`/api/v3/data/point_value/latest` returns the current value of each point. `deviceId` and `pointId` are optional —
supplying only `deviceId` returns the latest values of all points on that device. Pagination fields nest inside the
`page` object:

::: code-group

```bash [latest value]
# Example deviceId / pointId — replace with your own
curl -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <tenant>' -H 'X-Auth-Login: <account>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "page": {"current": 1, "size": 20}}'
```

```bash [list historical range]
# Use rangeKey (today/24h/7d/30d) or createTimeFrom / rangeHours to bound the time window
curl -X POST http://localhost:8000/api/v3/data/point_value/list \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <tenant>' -H 'X-Auth-Login: <account>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "pointId": 2001,
       "rangeKey": "24h",
       "page": {"current": 1, "size": 100}}'
```

:::

Compared to `latest`, `/api/v3/data/point_value/list` adds time-filter fields such as `createTimeFrom`, `rangeHours`,
and `rangeKey` (`today`/`24h`/`7d`/`30d`) for paging through history. The response looks roughly like this (sample
values):

```json
{
  "data": {
    "current": 1, "size": 20, "total": 1,
    "records": [
      { "deviceId": 1001, "pointId": 2001, "rawValue": "26.5", "calValue": "26.5",
        "numValue": 26.5, "createTime": "2026-06-22T11:59:58" }
    ]
  }
}
```

::: warning num_value may be null in aggregations
The underlying `dc3_point_value.num_value` is `NULL` for non-numeric or JSON payloads. If you bypass the API and run
aggregations like `AVG`/`SUM` directly on the hypertable, you must add `num_value IS NOT NULL`, or the results will be
skewed. A point's raw and calculated values live in `raw_value` and `cal_value` respectively (both are text).
:::

## Issuing commands: read, write, and polling for results

The command interfaces live in the Data Center, under the permission code `point_command:list`. They **don't return the
execution result** — only a command ID. Execution is asynchronous; you take that ID and poll the history.

`POST /api/v3/data/point_command/read` triggers a single read on demand, bypassing the acquisition cycle to request the
value from the device right now. `POST /api/v3/data/point_command/write` writes a value to a writable point. Both accept
an optional `commandId` for idempotent deduplication.

::: code-group

```bash [write command]
curl -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <tenant>' -H 'X-Auth-Login: <account>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "pointId": 2001, "value": "100"}'
# The data field of the response body is the commandId (example): "a1b2c3d4-...."
```

```bash [read command]
curl -X POST http://localhost:8000/api/v3/data/point_command/read \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <tenant>' -H 'X-Auth-Login: <account>' -H 'X-Auth-Token: <token>' \
  -d '{"deviceId": 1001, "pointId": 2001}'
```

```bash [poll for result]
# commandId is the command ID obtained in the previous step
curl 'http://localhost:8000/api/v3/data/point_command_history/get_by_command_id?commandId=a1b2c3d4-....' \
  -H 'X-Auth-Tenant: <tenant>' -H 'X-Auth-Login: <account>' -H 'X-Auth-Token: <token>'
```

:::

Polling returns a `PointCommandHistoryVO`. Its key fields are `status` (execution status, `PointCommandStatusEnum`),
`responseValue` (result / read-back value), `requestValue`, `finishTime`, and `expireTime`. Status starts at `PENDING`
and moves forward from there — it's only truly done when you see `SUCCESS`:

```json
{
  "data": {
    "commandId": "a1b2c3d4-....",
    "deviceId": 1001,
    "status": "SUCCESS",
    "responseValue": "100",
    "finishTime": "2026-06-22T12:00:01"
  }
}
```

::: tip Commands have a default 10-second time-to-live
The command DTO's `expireAt` defaults to `time of dispatch + 10s`. If the device isn't reached before then, the driver —
upon consuming the command and finding `now > expireAt` — marks it `EXPIRED` instead of waiting indefinitely. If polling
stays at `PENDING`/`SENT` for a long time, suspect the driver or an offline device first.
:::

## Command state machine: where does it go after PENDING

A command's lifetime advances one cell at a time through `dc3_point_command_history.status`. `PENDING` means it's just
been persisted and is waiting to be published; once the RabbitMQ publisher-confirm returns, it moves to `SENT` (queued,
waiting for the driver). From there the driver's execution receipt decides the terminal state. Understand this diagram
and you can work backwards from any status to see which hop it's stuck on.

<DataCommandsStateDiagram lang="en" />

There are six terminal states, matching values `2`–`7` of `PointCommandStatusEnum`: `SUCCESS(2)` succeeded; `FAILED(3)`
the driver explicitly failed; `TIMEOUT(4)` the application layer never received a receipt; `EXPIRED(5)` consumed only
after `expireAt` had passed; `DUPLICATE(7)` blocked by the driver's dedup cache; `DEAD(6)` rejected into the dead-letter
queue and no longer processed. `PENDING(0)` and `SENT(1)` are transitional.

::: info TIMEOUT currently has no producer
`TIMEOUT(4)` is reserved in `PointCommandStatusEnum`, but no code in the current pipeline sets a command to this status;
`SUCCESS/FAILED/EXPIRED/DUPLICATE/DEAD` are the terminal states actually produced. Read the `TIMEOUT` transition in the
state-machine diagram with that in mind.
:::

## Edge cases: offline, read-only points, and failed writes

Before dispatching a command, the system validates in this order: tenant consistency → device/point enabled → `rwFlag`
check (for write commands) → driver online. If any check fails, the command is never dispatched. Three common "the
command didn't go through" causes are worth keeping distinct:

When the device or driver is **offline**, the command can still be submitted and you'll get a `commandId`, but nothing
consumes it — so it usually sits at `SENT` until `EXPIRED`, or becomes `TIMEOUT` after a timeout. Whether a device is
online is determined by the lease in `dc3_entity_state` plus the RabbitMQ heartbeat; you don't set it by hand.

A point's read/write capability is set by its `rwFlag`, which takes the values `READ_ONLY`, `WRITE_ONLY`, and
`READ_WRITE`. A write command to a read-only point is rejected outright at the validation stage.

::: danger Writes to read-only points are rejected; failed writes echo no value
Calling `/point_command/write` on a point with `rwFlag=READ_ONLY` is rejected — this is a design constraint, not a
temporary check.
A write command is recorded as `SUCCESS` **only** when the driver's `write()` explicitly returns success. On failure the
status is `FAILED` and `responseValue=null` — **a failed write echoes no value at all**, so nothing makes it look like
it succeeded. When you poll and see `FAILED`, don't treat `responseValue` as the value that was written.
:::

::: info Custom commands are a separate mechanism
This page covers point-level read/write commands (`dc3.e.point_command` / `PointCommandDTO`). Device-level "custom
commands" use the separate `dc3.e.command` / `CommandCallDTO` namespace — don't mix the exchanges or DTOs of the two.
:::

## Troubleshooting checklist

Find issues quickly by symptom. The table is a quick reference; root-cause explanations are in the text above and in the
two plane documents.

| Symptom                                               | Check first                                                                                                                 |
|-------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Devices exist but `/point_value/latest` returns empty | Whether tenant/`deviceId`/`pointId` are correct, the acquisition cycle, driver protocol logs, whether RabbitMQ is backed up |
| Historical values have gaps or lag                    | Batch thresholds (`POINT_BATCH_SPEED`/`POINT_BATCH_INTERVAL`), RabbitMQ backlog, Data Center logs                           |
| Command stuck at `PENDING`/`SENT` for a long time     | Whether the target driver is online, whether it listens on `dc3.q.point_command.{serviceName}`                              |
| Command becomes `EXPIRED`                             | Device/driver offline or slow to respond; the 10-second `expireAt` has elapsed                                              |
| Write command returns `FAILED`                        | Whether the point's `rwFlag` allows writes, the written value's type/range, the protocol return code                        |

## Further reading

- [Data Plane](../architecture/data-plane) — every hop a value takes from device to hypertable, the RabbitMQ topology,
  and model transformations
- [Command Plane](../architecture/command-plane) — the command lifecycle state machine, queue TTL/DLX, and the
  result-receipt channel
- [First Device](../quickstart/first-device) — no device to query yet? Walk this golden path first
- [API Documentation](../development/api-documentation) — how to obtain the auth headers, where to find OpenAPI/Swagger
