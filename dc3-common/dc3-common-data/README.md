# DC3 Common Data

## Overview

`dc3-common-data` is the shared Data Center business module of the IoT DC3 platform. It provides all service
implementations for point-value ingestion, owner-directed command dispatch, driver/device status tracking, and data
query. It is wired into `dc3-center-data`. PostgreSQL/TimescaleDB and RabbitMQ provide the shared state and buffering;
Redis is not required.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-data

## Key Components

| Layer        | Contents                                                                                          |
|--------------|---------------------------------------------------------------------------------------------------|
| Controllers  | REST controllers for point values, point/custom commands, events, status, and health            |
| Services     | Point value, point command, command history, event history, driver status, and device status      |
| Facades      | `DriverFacade`, `PointFacade`, and related transport-independent cross-service APIs               |
| RabbitMQ     | Point/custom-command producers plus value, state, alarm, event, and result consumers              |
| Init         | `DataInitRunner` for startup preparation                                                          |

## Command Dispatch Flow

```
REST /api/v3/data/point_value/read
  → PointCommandServiceImpl
    → DeviceFacade.getActiveOwner(tenantId, deviceId)
    → RabbitMQ publisher confirm: dc3.e.point_command / dc3.r.point_command.{serviceName}.{ownerNode}
      → Only the leased owner accepts the matching fencing token
```

Custom device commands follow the parallel `dc3.e.command` / `dc3.r.command.{serviceName}` route through
`CommandHistoryServiceImpl`.

## Point-Value Ingestion Flow

```
driver SQLite outbox
  → RabbitMQ dc3.e.value / dc3.q.value.point
    → bounded broker-side consumer batch
      → validate the complete wire schema
        → one PostgreSQL transaction
          → reject expired node or stale fencing token
          → insert history with replay conflict ignored
          → upsert shared latest value using fence/time/sequence ordering
        → manual RabbitMQ ACK only after transaction commit
```

Every Data Center replica consumes from the same queue and uses the same PostgreSQL history/latest tables. There is no
process-local latest-value source of truth. Concurrent batches sort keys before persistence to keep lock ordering stable.
Malformed messages and batches that exhaust transient retries are rejected to the configured dead-letter path.

## MQ Topics

| Exchange              | Queue or routing key                | Direction                   |
|-----------------------|-------------------------------------|-----------------------------|
| `dc3.e.value`         | `dc3.q.value.point`                 | Inbound point values        |
| `dc3.e.point_command` | `dc3.r.point_command.{service}.{node}` | Outbound point read/write |
| `dc3.e.command`       | `dc3.r.command.{service}.{node}`       | Outbound custom commands  |
| `dc3.e.state`         | `dc3.q.state.driver` / `dc3.q.state.device` | Inbound driver/device state |
| `dc3.e.event`         | `dc3.q.event.report`                | Inbound reported events     |

The optional `dc3.rabbit.tag` system property prefixes runtime names; `RabbitConstant` remains authoritative.

## Ingestion Settings

| Property | Default | Meaning |
|---|---:|---|
| `dc3.data.point.batch.batch-size` | `500` | Maximum database transaction batch |
| `dc3.data.point.batch.receive-timeout-millis` | `100` | Maximum wait to fill a consumer batch |
| `dc3.data.point.batch.concurrent-consumers` | `4` | Initial Data Center consumer count |
| `dc3.data.point.batch.max-concurrent-consumers` | `16` | Elastic consumer ceiling |
| `dc3.data.point.batch.prefetch-count` | `1000` | Per-consumer broker prefetch |
| `dc3.data.point.batch.max-retries` | `3` | Transient batch attempts before dead-letter rejection |

Scale the consumer count only after PostgreSQL commit latency, connection-pool capacity, RabbitMQ unacked count, and
dead-letter rate are observable. The effective in-flight upper bound is approximately `consumers × prefetch`; it should
remain below the memory and recovery budget of a Data Center replica.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-data -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-data -am test
```

## Related Modules

- `dc3-center-data` — Bootstraps this module as a Spring Boot service
- `dc3-api-manager` — gRPC API consumed by this module for driver/point resolution
- `dc3-tsdb-core` / `dc3-tsdb-timescale` — store-neutral time-series port and TimescaleDB adapter
- `dc3-mq-core` / `dc3-mq-rabbitmq` — broker-neutral messaging port and RabbitMQ adapter
