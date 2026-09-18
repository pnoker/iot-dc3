# DC3 Common Thread

## Overview

`dc3-common-thread` is the shared thread pool module of the IoT DC3 platform. It exposes a set of JDK executor beans
(configured from the `dc3.thread` properties) that other modules inject explicitly for background work such as
concurrent point reads and batch message handling.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-thread

## Key Components

| Component          | Purpose                                                                                                                                                                                                                                          |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ThreadPoolConfig` | Provides three executor beans — `threadPoolExecutor` (`ThreadPoolExecutor`), `virtualThreadExecutor` (virtual-thread `ExecutorService`), `scheduledThreadPoolExecutor` (`ScheduledThreadPoolExecutor`) — sharing a caller-runs rejection handler |
| `ThreadProperties` | Binds thread pool properties from YAML (prefix `dc3.thread`: `prefix`, `corePoolSize`, `maximumPoolSize`, `keepAliveTime`)                                                                                                                       |
| `ThreadInitRunner` | Startup log for confirming thread pool initialization                                                                                                                                                                                            |

## Configuration Properties

```yaml
dc3:
  thread:
    prefix: dc3-thread-
    core-pool-size: 4
    maximum-pool-size: 32
    keep-alive-time: 15
```

## Usage

Inject the executor you need, for example `virtualThreadExecutor` in `MqttScheduleJob`, or `threadPoolExecutor` in
`DriverReadScheduleJob`. Data services also inject the shared executors for concurrent persistence and message work.

> Note: Spring's `@Async` does **not** use these beans — it runs on Spring Boot's default `applicationTaskExecutor`.
> These executors are obtained by explicit constructor injection, not via `@Async`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-thread -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-thread -am test
```

## Related Modules

- `dc3-common-data` / `dc3-common-mqtt` — inject `virtualThreadExecutor` for batch persistence / message handling
- `dc3-common-driver` — injects `threadPoolExecutor` for concurrent device reads
- `dc3-driver-opc-da` — injects `scheduledThreadPoolExecutor`
