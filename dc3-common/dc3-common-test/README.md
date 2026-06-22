# DC3 Common Test

## Overview

`dc3-common-test` is the shared test-support module of the IoT DC3 platform. It provides reusable contract tests,
test harnesses, and Testcontainers definitions so individual modules can write integration tests without re-deriving
the same fixtures.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-test
- **Version**: 2026.5.22

## Key Components

| Area              | Components                                                                         |
|-------------------|------------------------------------------------------------------------------------|
| Contract tests    | `SecretFieldContractTest`, `EnumContractTest`                                      |
| Test support      | `RabbitTestHarness`, `JsonAssertions`, `FixedClockConfig`, `GrpcInProcessExtension`|
| Testcontainers    | `PgTimescaleContainer`, `RabbitContainer`, `MqttContainer`                         |

## Dependencies

- `spring-boot-starter-test`, `reactor-test`, `spring-rabbit-test`, `grpc-testing`

## Usage

Add as a `test`-scoped dependency. Integration tests requiring real infrastructure use the Testcontainers helpers
(PostgreSQL + TimescaleDB, RabbitMQ, MQTT), which need a running container runtime (`podman`).

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-e2e` — backend end-to-end suite built on these helpers
- Consumed test-scoped by `dc3-common-*` and `dc3-center-*` modules

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
