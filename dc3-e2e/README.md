# DC3 E2E

## Overview

`dc3-e2e` is the backend end-to-end test suite of the IoT DC3 platform. It drives a running stack through real HTTP
(via REST Assured) and message-queue paths to verify cross-service behavior — command call, event report, point-value
storage, RabbitMQ delivery, and TimescaleDB hypertables.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-e2e
- **Version**: 2026.5.22
- **Packaging**: jar (test sources only)

## Test Suites

| IT                        | Verifies                                              |
|---------------------------|-------------------------------------------------------|
| `HarnessSmokeIT`          | The E2E harness itself comes up                       |
| `RestAssuredHarnessIT`    | REST Assured wiring against the gateway               |
| `CommandCallE2eIT`        | Command call end-to-end                               |
| `EventReportE2eIT`        | Event report end-to-end                               |
| `RabbitDeliveryIT`        | RabbitMQ message delivery                             |
| `PostgresHypertableIT`    | TimescaleDB hypertable behavior                       |
| `E2eEnvironmentGuardIT`   | Guards that the target environment is configured      |

The harness (`E2eStack`, `BaseE2eIT`) lives under `src/test/java/io/github/pnoker/e2e/harness`.

## Running

```bash
make test-e2e
# = DC3_E2E=true mvn -B -Dmaven.test.skip=false -Dskip.unit.tests=true -pl dc3-e2e -am -Pe2e verify
```

Requires a container runtime (`podman`) for the Testcontainers-backed dependencies provided by `dc3-common-test`.

## Dependencies

- `dc3-common-test` — Testcontainers and test harnesses
- `dc3-common-model` — domain models
- `rest-assured` — HTTP assertions

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
