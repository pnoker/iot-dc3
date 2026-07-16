---
title: "Command Plane: Dispatching Read/Write Commands and Their Receipts"
---

<script setup>
import CommandStateDiagram from '../../.vitepress/theme/components/CommandStateDiagram.vue'
import CommandFlowDiagram from '../../.vitepress/theme/components/CommandFlowDiagram.vue'
</script>


# Command Plane: Dispatching Read/Write Commands and Their Receipts

The data plane pulls values up from devices. The command plane runs the other way: it takes a "read this point" or "
write this value" request and drives it from the HTTP entry point through the data center, over RabbitMQ, into the
driver, out to the device, and back as a receipt. This page traces that full path and the state machine behind it —
submission, validation, persistence, dispatch, driver execution, and receipt — so you understand why submitting a
command is "take a ticket, poll for the result," and what each status means when something fails.

> You are here: you already know the collection flow from the [data plane](./data-plane). Now look at the reverse
> direction — command dispatch. A command can originate from the Web UI, the CLI, or AI (
> see [Data and Commands](../operation/data-commands)).

## Why "Asynchronous Ticket + Polling"

Dispatching a command crosses processes, crosses the network, and finally lands on a physical device. Any hop along the
way can be slow or can fail. If the HTTP request blocked until the device finished executing, gateway threads would be
tied up for a long time — and an offline device or a protocol timeout could drag the whole call chain down with it.

So the command plane splits "submission" from "result." After `POST /api/v3/data/point_command/read` and
`POST /api/v3/data/point_command/write` validate the request at the data center, persist the command as `PENDING`, and
publish it to RabbitMQ, they **return a `commandId` immediately** (a 36-character UUID). The caller takes that ticket
and polls the history endpoint to see where the command stands, whether it succeeded or failed, and what value the
device returned.

The chain starts at `PointCommandController` (`dc3-common-data`). Both endpoints require the `point_command:list`
permission. The request body supplies `deviceId` / `pointId` (write commands also need `value`), and may carry a
`commandId` to make submission idempotent — resubmitting the same `commandId` returns the existing record and never
dispatches a duplicate.

## The Journey of a Write Command

The sequence diagram below shows the happy path: the caller submits, the driver writes the value into the device and
acknowledges success, and the caller polls and gets the result.

<CommandPlaneDiagram lang="en" />

### Submission Side: Validate, Persist, Publish

Before dispatching, `PointCommandServiceImpl` runs a sequence of checks. A failure at any step throws immediately, and
nothing is enqueued:

- **Tenant scope**: `deviceId` / `pointId` must belong to the current tenant, and the device's bound `profileId` must
  match the point's `profileId`. A mismatch is rejected as an authorization violation.
- **Enabled status**: both the device and the point must have `enableFlag` enabled. A disabled device or point takes no
  commands.
- **Writability (write commands only)**: the point's `rwFlag` must be `WRITE_ONLY` or `READ_WRITE`. Writing a
  `READ_ONLY` point is rejected ("Point is not writable"). This matches the [Core Concepts](../introduction/concepts)
  rule that "the Point itself decides read/write."
- **Driver online**: the owning driver's status is looked up in `dc3_entity_state`. Anything other than `ONLINE` is
  rejected ("Driver is offline").

When validation passes, the command is written to `dc3_point_command_history` as `PENDING`, then published with
`rabbitTemplate.convertAndSend(...)` and `CorrelationData` set to `commandId` — so RabbitMQ's publisher-confirm maps
exactly to this one command. After the publish call returns, the record moves to `SENT` and `sendTime` is written.

### The Delivered Payload: PointCommandDTO

What crosses RabbitMQ is not a loose JSON string but a strongly typed record, `PointCommandDTO`. Its `payload` field is
a `sealed` interface, and every time field uses `Instant` (UTC):

```java
public record PointCommandDTO(
        String commandId,          // one-to-one with history record and receipt
        Long tenantId,             // tenant isolation
        PointCommandTypeEnum type, // READ / WRITE / ...
        PointCommandPayload payload, // ReadPayload | WritePayload (polymorphic)
        PointCommandSourceEnum source,
        Long sourceUserId,
        Instant occurredAt,
        Instant expireAt,          // defaults to occurredAt + 10s
        int schemaVersion
) { }
```

`PointCommandPayload` is a sealed interface with exactly two implementations, `ReadPayload(deviceId, pointId)` and
`WritePayload(deviceId, pointId, value)`. The driver dispatches them with a `switch` pattern match, and all branches are
exhaustively checked at compile time.

::: warning expireAt defaults to only 10 seconds
`PointCommandDTO.ofRead()` / `ofWrite()` set `expireAt` to `Instant.now().plusSeconds(10)`. If a command backs up in the
queue, or `now > expireAt` already holds when the driver picks it up, it is judged `EXPIRED` and not executed. This is
short-lived semantics designed for collection-style commands — don't treat it as a long-running task that can sit in a
queue.
:::

### Driver Side: Precheck, Dedup, Lock, Execute

The driver consumes the command queue through `PointCommandReceiver`. Once it picks up a command, the order is fixed:

1. **Basic validation**: if any of `commandId` / `tenantId` / `type` / `payload` is null, or the read/write payload is
   missing a field, the message is `reject`ed directly (sent to the dead-letter queue, not requeued).
2. **expireAt precheck**: `now > expireAt` -> receipt `EXPIRED`; the device is never touched.
3. **Dedup**: `tryAcquire(commandId)` against a Caffeine dedup cache (5-minute expiry, capped at 50,000 entries). A hit
   means this command already ran, so the receipt is `DUPLICATE`.
4. **Per-device serial lock**: acquire the device's `ReentrantLock` through `DeviceLockManager` (reference-counted for
   creation and reclamation). This keeps multiple commands on the same device from interleaving and scrambling the
   protocol timing.
5. **Read/write dispatch**: `ReadPayload` calls `driverReadService.read(...)`; `WritePayload` calls
   `driverWriteService.write(...)`.

::: danger A failed write returns no value
A write command **counts as successful only when `driverWriteService.write()` returns `Boolean.TRUE`**. Then the receipt
is `SUCCESS` and carries the value just written. The moment it returns `false`, the receipt is `FAILED` with
`responseValue=null` — **no value is echoed**. That's deliberate: echoing a value on a failed write would mislead upper
layers into thinking the command landed and the device state changed — a false success. When you see `FAILED`, read it
as "this write did not take effect."
:::

## The Command Lifecycle

A command's status is defined by `PointCommandStatusEnum`. The flow from submission to terminal state is below. The
submission side owns `PENDING -> SENT`; every terminal state after that is produced by the receipt the driver emits on
consumption and written back through the result queue.

<CommandStateDiagram lang="en" />

The index and meaning of each status (`PointCommandStatusEnum`, with the persisted `status` value in parentheses):

| Status      | index | Meaning                                                                             |
|-------------|-------|-------------------------------------------------------------------------------------|
| `PENDING`   | 0     | Submitted, awaiting publish                                                         |
| `SENT`      | 1     | Published to broker, awaiting driver processing                                     |
| `SUCCESS`   | 2     | Driver confirmed success                                                            |
| `FAILED`    | 3     | Driver reported failure (write failure / exception after requeue)                   |
| `TIMEOUT`   | 4     | Application-layer timeout (reserved in enum, not yet produced by the current chain) |
| `EXPIRED`   | 5     | `expireAt` had already passed before execution                                      |
| `DEAD`      | 6     | Rejected into the dead-letter queue, no longer processed                            |
| `DUPLICATE` | 7     | Judged a duplicate by the driver's dedup cache                                      |

`EXPIRED` is set by the driver when `now > expireAt` at consumption; `DUPLICATE` comes from a dedup-cache hit.

::: info TIMEOUT currently has no producer
`PointCommandStatusEnum` reserves `TIMEOUT(4)`, but no code in the current chain ever sets a command to this status.
`SUCCESS` / `FAILED` / `EXPIRED` / `DUPLICATE` / `DEAD` each have a clear production path; `TIMEOUT` alone is an enum
slot held for future application-layer timeout semantics, which is why the state machine annotates it with a note rather
than an active edge.
:::

A command's `type` comes from `PointCommandTypeEnum`: `READ(0)` / `READ_BATCH(1)` / `WRITE(2)` / `WRITE_BATCH(3)` /
`CONFIG(4)`. The current read/write endpoints dispatch `READ` and `WRITE`.

### The Error Path: Requeue Once, Then Record the Failure

When driver execution throws, the handling is built to stop a "poison message" from looping in the queue forever:

- **First failure (not a requeue)**: release the command's dedup hold and `nack(requeue=true)` to put the message back
  on the queue for one more attempt.
- **Still failing after requeue**: don't requeue again. Emit a `FAILED` receipt directly (`errorCode=DRIVER_ERROR`) and
  `ack` the message so it leaves the queue.

Each command is attempted by the driver at most twice — one chance for a transient fault to clear on its own, and a hard
stop before a perpetually failing command loops forever.

## The Command RabbitMQ Topology

The command chain uses two sets of exchanges and queues. One set carries commands from the data center to the driver;
the other carries receipts back. Command queues are partitioned by the driver's `serviceName`, with a 30-second TTL and
a dead-letter exchange. The result queue has a 60-second TTL.

<CommandFlowDiagram lang="en" />

The command queue `dc3.q.point_command.{serviceName}` is durable with `ttl(30000)` and dead-letters to
`dc3.e.point_command_dead`. Two paths reach the dead-letter queue: a command the driver never consumes within 30
seconds (TTL expiry), or the driver `reject`ing it (no requeue) when basic validation fails. Either way, nothing lingers
in the original queue.

Receipts travel over `dc3.e.point_command_result` (topic). The result queue `dc3.q.point_command_result` has
`ttl(60000)` and is consumed by the data center's `PointCommandResultReceiver`: it looks up the history record by
`commandId` and writes the terminal `status`, `responseValue`, `errorCode` / `errorMessage`, and `finishTime`.

## Submission and Polling: The Real Routes

Dispatching a write command and polling for its result are two independent HTTP calls. All paths forward through the
gateway (`http://localhost:8000`), and protected endpoints require the three auth headers `X-Auth-Tenant` /
`X-Auth-Login` / `X-Auth-Token`.

::: code-group

```bash [Submit write command]
# Write value 25.5 to device 1024, point 2048; returns commandId (example UUID)
curl -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H "X-Auth-Tenant: <tenant>" \
  -H "X-Auth-Login: <login>" \
  -H "X-Auth-Token: <token>" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": 1024, "pointId": 2048, "value": "25.5"}'
# → {"code":"...","data":"9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", ...}
```

```bash [Poll result]
# Use the commandId from previous step to query history; check status and responseValue
curl "http://localhost:8000/api/v3/data/point_command_history/get_by_command_id?commandId=9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" \
  -H "X-Auth-Tenant: <tenant>" \
  -H "X-Auth-Login: <login>" \
  -H "X-Auth-Token: <token>"
```

:::

Polling returns a `PointCommandHistoryVO`. The query is tenant-isolated (`getByCommandId(tenantId, commandId)`), so one
tenant cannot read another tenant's command records. The command is done as soon as `status` reaches any terminal state
above. For write commands, watch `responseValue`: on `SUCCESS` it holds the echoed written value, and on `FAILED` it is
always `null` (see "A failed write returns no value" above).

::: info A separate namespace from "Custom Commands"
This page covers **point read/write** (`point_command`): exchange `dc3.e.point_command`, DTO `PointCommandDTO`, table
`dc3_point_command_history`. The platform also has a separate set of **Custom Commands** that travel over
`dc3.e.command` with DTO `CommandCallDTO`, used for device-level actions defined on a Profile. The two are structurally
similar but isolated — don't mix their routing keys, DTOs, or history tables.
:::

## Further Reading

- [Data Plane](./data-plane) — the reverse point-value collection chain: exchanges, queues, TimescaleDB persistence
- [Core Concepts and Mental Model](../introduction/concepts) — why a point's `rwFlag` decides its writability
- [Driver Development](../development/driver-authoring) — the `DriverProtocol.write()` contract returning `Boolean`, and
  the command processing pipeline
- [Data and Commands](../operation/data-commands) — dispatching commands as a user, handling offline and read-only
  points
