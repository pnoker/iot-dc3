---
title: Testing
---

<script setup>
import TestingDiagram from '../../.vitepress/theme/components/TestingDiagram.vue'
</script>


# Testing

This page covers how the IoT DC3 backend layers its tests, when a test is required, and which command runs each layer.
By the end you'll know how to pick the right test type, run unit, integration, and E2E tests locally, and tell what kind
of test a change needs before it counts as done.

> You are here: you've read the [development overview and conventions](./), and you want to validate a change to
> mergeable quality. Testing tips specific to writing drivers are in [driver authoring](./driver-authoring).

## Why layer the tests

Heavier is not better. An assertion can catch a logic error in milliseconds — that's no reason to spin up a PostgreSQL
container to confirm it. But pure mocks can't exercise cross-service message contracts or time-series persistence, so
IoT DC3 splits testing into three layers. Lower layers are faster, more numerous, and more isolated; higher layers are
slower, fewer, and closer to the real chain:

- **Unit tests** are the most numerous and the fastest. They check isolated business logic without starting the Spring
  context.
- **Integration tests** use Testcontainers to spin up real PostgreSQL/TimescaleDB, RabbitMQ, and MQTT, exercising
  cross-component collaboration — DAL, gRPC, and messaging.
- **E2E tests** are the fewest. They cover end-to-end business chains (command dispatch, event routing, time-series
  table operations). They're off by default and require an environment variable to run.

The rule for picking a layer: use a unit test if you can avoid external dependencies; reach for integration only when a
real container is needed to reproduce the behavior (SQL, messaging, gRPC contracts); reserve E2E for validating the
entire chain.

## The three-layer pyramid and its commands

The diagram maps the three layers, each layer's scope and startup mode, and the command that triggers it. The
executors (Surefire/Failsafe) and the gate (`DC3_E2E`) decide which layers a single `mvn` run reaches.

<TestingDiagram lang="en" />

::: warning Integration and E2E require a container runtime
Both `make test-it` and `make test-e2e` use Testcontainers to start PostgreSQL/TimescaleDB, RabbitMQ, and other
containers at runtime, so a working container runtime (podman or Docker) must be present locally. E2E additionally boots
a full set of real dependencies on a shared Docker network. Without a container runtime these two commands fail outright
rather than skip — `make test` (pure unit) is unaffected.
:::

The table below compares each layer's goal, executor, and tech stack (reference only; the prose above is authoritative
on detail):

| Layer             | Goal                                                      | Executor / Gate                                    | Technical approach                                                            |
|-------------------|-----------------------------------------------------------|----------------------------------------------------|-------------------------------------------------------------------------------|
| Unit tests        | Quickly verify isolated business logic                    | Surefire                                           | JUnit 5, Mockito 5, AssertJ; Reactor `StepVerifier` for reactive verification |
| Integration tests | Verify real infrastructure and cross-module collaboration | Failsafe; `*IT.java`                               | Testcontainers, gRPC InProcess, RabbitMQ harness                              |
| E2E tests         | Verify end-to-end business chains                         | `@EnabledIfEnvironmentVariable(named = "DC3_E2E")` | `dc3-e2e`, Testcontainers (shared Docker network)                             |

## How to run it locally

Backend commands go through the Makefile under `iot-dc3/`. The four most common:

```bash
make test                 # Unit tests (Surefire)
make test-it              # Integration tests (Failsafe + Testcontainers; needs a container runtime)
make test-e2e             # E2E: equivalent to DC3_E2E=true mvn -s .mvn/settings.xml -pl dc3-e2e -am -Pe2e verify
make coverage             # Aggregated JaCoCo report (dc3-coverage -am verify)
```

`make test-e2e` already sets `DC3_E2E=true` and runs `-Pe2e` only on the `dc3-e2e` module, so you don't need to export
the environment variable by hand. To run a single module or a single case, call Maven directly:

```bash
# Unit tests for a specific module
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager

# A single test class / a single test method
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager -Dtest=DeviceControllerTest
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-public -Dtest="RTest#testOkWithData"
```

After `make coverage` finishes, the aggregated report lands at:

```text
dc3-coverage/target/site/jacoco-aggregate/index.html
```

::: info Why Failsafe points to outputDirectory
The parent POM configures Failsafe with `classesDirectory=${project.build.outputDirectory}`. Spring Boot repackages a
module's artifact into an executable fat jar, which can't be loaded directly on the Failsafe classpath; pointing the
test run at the unrepackaged, ordinary compiled classes is what lets the classes under test load correctly. Keep this in
mind when writing integration tests for executable modules like drivers.
:::

## Frontend test commands

The frontend (`dc3-web/`) uses pnpm + Vitest (unit/api/component/view) and Playwright (E2E), independent of the
backend:

::: code-group

```bash [Vitest suites]
pnpm test                 # All Vitest suites
pnpm test:unit            # tests/unit
pnpm test:api             # tests/api
pnpm test:component       # tests/component
pnpm test:views           # tests/views
pnpm test:guard           # tests/guardrails (AI coding guardrails)
pnpm test:ci              # vitest run --coverage (CI gate)
```

```bash [Playwright E2E]
pnpm test:e2e             # headless chromium
pnpm test:e2e:headed      # Visible browser (E2E_HEADLESS=false)
```

:::

## When a test is mandatory

The core convention: **for a bug fix, first write a failing test that reproduces it, then fix.** Turn "this bug no
longer occurs" into an executable test that's red right now; the loop only closes when the fix turns it green. Other
changes get tests in proportion to risk:

| Change type                   | Requirement                                                                     |
|-------------------------------|---------------------------------------------------------------------------------|
| Bug fix                       | First add a regression test that reproduces the problem, then implement the fix |
| New feature / behavior change | Add unit tests, and integration tests as risk warrants                          |
| Refactoring                   | Preserve existing coverage; add contract tests for implicit contracts           |
| DAL / SQL change              | Add Testcontainers tests and run `make test-it`                                 |
| gRPC proto change             | Update the server, client, and contract tests in sync                           |
| Docs-only / formatting change | No Java tests needed; a docs build or format check suffices                     |

## Reusable test infrastructure

The `dc3-common-test` module gathers the containers and base classes shared across modules, so each module doesn't build
its own. Integration tests reuse these singleton containers and harnesses directly:

| Tool                     | Purpose                                                                                       |
|--------------------------|-----------------------------------------------------------------------------------------------|
| `PgTimescaleContainer`   | Singleton `timescale/timescaledb-ha:pg18` container, for database and time-series table tests |
| `RabbitContainer`        | RabbitMQ container, for message publish, confirm, and consume tests                           |
| `MqttContainer`          | MQTT container, for MQTT driver tests                                                         |
| `GrpcInProcessExtension` | JUnit 5 extension: one in-process gRPC server + managed channel per test                      |
| `RabbitTestHarness`      | Send and receive RabbitMQ within tests, with `awaitTrue()` backed by Awaitility               |
| `FixedClockConfig`       | `@TestConfiguration` that pins the `Clock` bean to a deterministic instant                    |

Two contract-test base classes guard cross-cutting conventions:

- `EnumContractTest<E>`: via `@TestFactory`, checks that enum `getIndex()` is unique, `ofIndex()` round-trips, and
  constant names stay stable.
- `SecretFieldContractTest`: checks that sensitive fields such as `apiKey`, `password`, `secret`, and `token` don't leak
  through `@ToString` or serialization.

::: tip Time, randomness, and waiting
Inject `java.time.Clock` instead of calling `LocalDateTime.now()` directly; use `FixedClockConfig` when you need fixed
time. Use Awaitility for async waits — never bare `Thread.sleep`. Use `WebTestClient` for WebFlux, not `MockMvc`; use an
in-process channel for gRPC, and don't open a real socket.
:::

## Coverage gate

`make coverage` aggregates JaCoCo data from every module, gated by the thresholds in `dc3-coverage/pom.xml`:

| Metric          | Current threshold                      |
|-----------------|----------------------------------------|
| Line coverage   | `coverage.line.minimum = 0.20` (20%)   |
| Branch coverage | `coverage.branch.minimum = 0.15` (15%) |

The thresholds are deliberately modest for now, to keep progress moving while the test suite is still expanding. The
check looks only at the static minimum — any metric below the table above blocks the change, with no comparison against
a historical baseline. When raising a threshold, submit tests that support the new bar rather than just bumping the
number.

## CI workflows

PRs and pushes run tests across three GitHub Actions workflows, one-to-one with the local commands:

| Workflow   | Trigger                                          | Main task                   |
|------------|--------------------------------------------------|-----------------------------|
| `ci.yml`   | push / PR to develop, release, main              | Fast compile                |
| `test.yml` | push / PR to develop, release, main              | Unit, integration, coverage |
| `e2e.yml`  | push to develop, release, main or manual trigger | E2E                         |

Before merging, confirm: the unit and integration jobs pass; coverage isn't below the `dc3-coverage/pom.xml`
thresholds (below blocks); and for behavior changes, the description states what's been verified and what risks remain.

## Further reading

- [Development overview and conventions](./) — overall conventions and command entry points for secondary development
- [Driver authoring](./driver-authoring) — how to add the protocol layer and integration tests when deriving a new
  driver
- [Environment variables](../quickstart/environment) — dependency hosts and ports needed to run Java locally
