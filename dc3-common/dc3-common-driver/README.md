# DC3 Common Driver

## Overview

`dc3-common-driver` is the shared driver dependency module of the IoT DC3 platform. It provides the driver SDK shared by
all protocol drivers, including auto-registration with Manager Center, PostgreSQL-backed runtime ownership, metadata
sync, RabbitMQ command handling, durable telemetry publication, and scheduled data collection. Redis is not part of the
driver coordination path.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-driver

## Key Components

| Component                                    | Purpose                                                       |
|----------------------------------------------|---------------------------------------------------------------|
| `DriverInitRunner`                           | Registers logical metadata and starts the runtime lease        |
| `DriverLeaseRenewScheduleJob`                | Renews membership and installs streamed ownership snapshots    |
| `DriverEnvironmentConfig`                    | Derives immutable node, service, host, and client identities   |
| `BufferServiceImpl`                          | SQLite outbox deleted only after broker ACK and routability     |
| RabbitMQ Consumers                           | Validates node/fencing before executing directed commands      |
| Scheduled Jobs                               | Reads only devices currently owned by the runtime node         |
| `DriverTopicConfig`                          | Creates expiring per-node command queues and bindings           |

## Driver Registration Flow

```
Driver startup
  → DriverInitRunner
    → gRPC: dc3-center-manager / DriverApi.DriverRegister
      ← Returns: logical driver and protocol metadata
    → gRPC stream: DriverApi.RenewLease
      ← Returns: bounded device-lease pages, assignment version, fencing tokens
    → Atomically install the ownership snapshot after stream completion
    → Subscribe to metadata queue: dc3.q.metadata.driver.{serviceName}
    → Subscribe to point-command queue: dc3.q.point_command.{serviceName}.{node}
    → Subscribe to custom-command queue: dc3.q.command.{serviceName}.{node}
```

Manager Center stores runtime membership, device assignments, assignment revisions, and fencing tokens in PostgreSQL.
Rendezvous hashing assigns each active device to exactly one live node. Stable heartbeats read only membership and one
device-revision row; full device scans happen only after membership or device-set changes. Both reconciliation and gRPC
delivery use keyset pages, so no manager request materializes every device in memory.

## RabbitMQ Integration

| Exchange              | Queue                               | Purpose                              |
|-----------------------|-------------------------------------|--------------------------------------|
| `dc3.e.metadata`      | `dc3.q.metadata.driver.{service}`   | Receive configuration changes        |
| `dc3.e.point_command` | `dc3.q.point_command.{service}.{node}` | Receive owner-directed point commands |
| `dc3.e.command`       | `dc3.q.command.{service}.{node}`       | Receive owner-directed custom commands |
| `dc3.e.value`         | —                                   | Publish point values to Data Center  |

The optional `dc3.rabbit.tag` system property prefixes runtime names; use `RabbitConstant` and `DriverTopicConfig` as
the authoritative definitions.

## Delivery and Failure Semantics

- A point value receives an immutable message ID, schema version, node sequence, owner node, and fencing token.
- Every point value is written to the mandatory SQLite outbox using WAL with full synchronous durability before publish. A row is removed only after RabbitMQ publisher
  confirm ACK and no mandatory-return signal. NACK, unroutable, timeout, and synchronous failures remain durable and use
  capped exponential backoff; there is no retry-count or size-based data eviction.
- List-based reports are inserted in one SQLite transaction before the first RabbitMQ publish, preserving durability
  while amortizing FULL-synchronous fsync cost for high-volume protocol frames.
- Every driver runtime must own an exclusive persistent volume for its outbox directory. Do not mount the same SQLite
  file into multiple driver processes or replicas. The supplied Compose stacks use a separate named volume per driver
  service; an orchestrator must provide equivalent per-pod persistent storage.
- A driver stops reads, writes, and telemetry immediately when its local lease expires. Manager and Data Center also
  reject stale owners by node and fencing token, so a paused or partitioned process cannot resume as an owner.
- Command execution is at-least-once across process crashes. Protocol implementations should use the command ID when the
  physical device supports idempotency; no platform can guarantee exactly-once physical I/O after a crash without
  device-side idempotency.

## Runtime Settings

| Property | Default | Meaning |
|---|---:|---|
| `dc3.driver.lease.seconds` | `30` | Manager-issued runtime lease; valid range is 10–120 seconds |
| `dc3.driver.lease.renew-cron` | `0/10 * * * * ?` | Lease renewal schedule; keep comfortably below the lease |
| `dc3.driver.lease.queue-expires-millis` | `300000` | Removes unused per-node command queues after pod churn |
| `dc3.driver.buffer.db-path` | `dc3/data/driver/buffer.db` | Mandatory SQLite outbox path on runtime-exclusive persistent storage |
| `dc3.driver.buffer.batch-size` | `200` | Maximum outbox rows attempted per republish tick |
| `dc3.driver.buffer.max-backoff-seconds` | `600` | Maximum per-message republish backoff |

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-driver -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-driver -am test
```

## Related Modules

- All `dc3-driver-*` modules — Consume this SDK
- `dc3-api-driver` — gRPC contracts consumed by this SDK
- `dc3-common-rabbitmq` — RabbitMQ exchange configuration
- `dc3-common-constant` — `RabbitConstant` routing key prefixes
