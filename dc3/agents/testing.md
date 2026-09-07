# Testing and verification conventions

Read this when writing or running tests. Entry point, commands, and the validation matrix: [`AGENTS.md`](../../AGENTS.md).

## Test types

- Unit tests (`*Test.java`, `*Tests.java`) run with Surefire, JUnit 5, Mockito, AssertJ, and Reactor `StepVerifier`
  where appropriate. Do not start a Spring context for a test that can use direct construction.
- Integration tests (`*IT.java`) run with Failsafe and may use `dc3-common-test` Testcontainers and harnesses.
- E2E tests live in `dc3-e2e/` and are gated by the `DC3_E2E` environment variable.

Reusable test infrastructure lives in `dc3-common-test` (verify names against its sources): `GrpcInProcessExtension`,
`RabbitTestHarness`, `PgTimescaleContainer`, `RabbitContainer`, `MqttContainer`, `FixedClockConfig`, `JsonAssertions`,
`EnumContractTest`, and `SecretFieldContractTest`.

## Coverage

`make coverage` generates the aggregate report under `dc3-coverage/target/site/jacoco-aggregate/`. The current gate is
an absolute minimum configured in `dc3-coverage/pom.xml`; `dc3-coverage/scripts/check_coverage.py` validates the
aggregate XML. Do not claim a relative regression gate unless the build implements one.
