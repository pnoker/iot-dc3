# DC3 Common Thread

## Overview

`dc3-common-thread` is the shared thread pool configuration module of the IoT DC3 platform. It configures the
platform-wide async task executor used for `@Async` annotated methods
across all services.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-thread
- **Version**: 2026.5.22

## Key Components

| Component          | Purpose                                                                                            |
|--------------------|----------------------------------------------------------------------------------------------------|
| `ThreadPoolConfig` | Creates and configures the `TaskExecutor` bean used by Spring's `@Async`                           |
| `ThreadProperties` | Binds thread pool properties from YAML (`core-pool-size`, `max-pool-size`, `queue-capacity`, etc.) |
| `ThreadInitRunner` | Startup log for confirming thread pool initialization                                              |

## Configuration Properties

```yaml
thread:
  pool:
    core-pool-size: ${THREAD_POOL_CORE_SIZE:10}
    max-pool-size: ${THREAD_POOL_MAX_SIZE:200}
    queue-capacity: ${THREAD_POOL_QUEUE_CAPACITY:1000}
    keep-alive-seconds: ${THREAD_POOL_KEEP_ALIVE:30}
    thread-name-prefix: dc3-async-
```

## Usage

Any `@Async` annotated method (e.g., `MetadataEventListener.onApplicationEvent`) executes on this thread pool
automatically when `dc3-common-thread` is on the classpath.

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-manager` — Uses async thread pool for `MetadataEventListener`
- All `dc3-center-*` — Pull in this module for async support

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

