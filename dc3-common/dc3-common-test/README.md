# DC3 Common Test

## Overview

`dc3-common-test` is the shared test-support module of the IoT DC3 platform. It provides reusable contract tests, test
harnesses, and Testcontainers definitions so individual modules can write integration tests without re-deriving the same
fixtures.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-test

## Key Components

| Area           | Components                                                                          |
|----------------|-------------------------------------------------------------------------------------|
| Contract tests | `SecretFieldContractTest`, `EnumContractTest`                                       |
| Test support   | `RabbitTestHarness`, `JsonAssertions`, `FixedClockConfig`, `GrpcInProcessExtension` |
| Testcontainers | `PgTimescaleContainer`, `RabbitContainer`, `MqttContainer`                          |

## Dependencies

- `spring-boot-starter-test`, `reactor-test`, `spring-rabbit-test`, `grpc-testing`

## Usage

Add as a `test`-scoped dependency. Integration tests requiring real infrastructure use the Testcontainers helpers
(PostgreSQL + TimescaleDB, RabbitMQ, MQTT), which need a Docker-compatible container runtime.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-test -am package
```

## Testing

This module currently has no module-specific automated tests. Verify generated or production sources by compiling the
affected reactor from the repository root:

```bash
mvn -s .mvn/settings.xml -q -pl dc3-common/dc3-common-test -am -DskipTests compile
```

## Related Modules

- `dc3-e2e` — backend end-to-end suite built on these helpers
- Consumed test-scoped by `dc3-common-*` and `dc3-center-*` modules
