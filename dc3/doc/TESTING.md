# IoT DC3 Testing Guide

This document is the canonical testing reference for the iot-dc3 backend
repository. It complements `AGENTS.md` (engineering rules), `LOGGING.md`
(runtime logging conventions) and `ENVIRONMENT.md` (runtime configuration).

## 1. Test Pyramid

| Layer | Goal | Mechanics | Target share |
|---|---|---|---|
| Unit | Fast, isolated business logic checks | JUnit 5 + Mockito + AssertJ | ~70% |
| Slice | Spring slice tests for controllers, JSON, persistence | `@WebFluxTest`, `@JsonTest`, `@MybatisPlusTest` | ~25% |
| Integration | Real infrastructure via Testcontainers, gRPC in-process | PG18+TimescaleDB, RabbitMQ, MQTT, gRPC InProcess | included in slice/integration share |
| End-to-end | Full reactor against a docker-compose stack | `dc3-e2e` with rest-assured | ~5% |

Aggregate coverage gates (read from `dc3-coverage`):

- Repository line coverage ≥ 80%
- Critical-domain line coverage ≥ 90% (auth, manager, data, driver SDK,
  public utilities, vendored modbus codecs)
- Repository branch coverage ≥ 65%

Coverage is intentionally relaxed in the early stages and tightened as
each test stage lands. The current threshold is configured in
`dc3-coverage/pom.xml`.

## 2. Directory & Naming Conventions

Each module that owns test code follows the same layout:

```
<module>/src/test/
├── java/<package>/
│   ├── unit/           # *Test.java; Surefire executes
│   ├── slice/          # *SliceTest.java; @WebFluxTest / @JsonTest etc.
│   ├── integration/    # *IT.java; Failsafe executes; @Tag("integration")
│   ├── contract/       # *ContractTest.java; extends a dc3-common-test base
│   └── support/        # Module-specific fixtures
└── resources/
    ├── application-test.yml
    ├── fixtures/       # JSON / CSV / protocol byte streams
    └── logback-test.xml
```

Naming rules:

- Unit tests: `*Test.java`. Surefire matches by default.
- Integration tests: `*IT.java`. Failsafe matches by default.
- Test method names: `should_<expected>_when_<condition>` or
  `given_<state>__when_<action>__then_<outcome>`.
- All assertions use AssertJ. Do not introduce JUnit 4 assertions.
- Mocks use Mockito 5 (including `mockStatic` for static helpers).
  PowerMock is forbidden.
- Fixture resources go under `src/test/resources/fixtures/` and use the
  `<feature>-<scenario>.json` naming pattern.

## 3. When Tests Are Required

| Change type | Required action |
|---|---|
| Bug fix | Add a regression test that fails before the fix |
| New feature / behaviour change | Add unit + slice/integration coverage |
| Refactor | Preserve coverage; add a contract test if behaviour was previously implicit |
| DAL / SQL change | Run `make test-it`; add a Testcontainers slice test |
| gRPC proto change | Update server + stub tests in the same PR |
| Pure docs / formatting | No test required |

The `Validation Checklist` in `AGENTS.md` enumerates the full matrix.

## 4. Time, Randomness, IO

- Inject a `java.time.Clock` instead of calling `LocalDateTime.now()`.
  The shared `FixedClockConfig` test configuration produces a
  deterministic instant; import it via `@Import(FixedClockConfig.class)`.
- Inject a `RandomGenerator` (or seed it) — never rely on real entropy.
- Static utilities are mocked with `Mockito.mockStatic(...)` inside a
  `try-with-resources` block.
- WebFlux paths are exercised with `WebTestClient`; `MockMvc` is not used
  in this repository.
- gRPC is exercised with `grpc-testing` and `InProcessChannelBuilder`;
  no real sockets in tests.
- Async waits go through Awaitility (`org.awaitility.Awaitility.await()`)
  with a polling interval. `Thread.sleep` in tests is forbidden.

## 5. Testcontainers Conventions

Containers expose a single shared instance per JVM, started lazily on
class-load and reused across modules via `withReuse(true)`. Pin images
to the production-aligned tag:

| Container | Image | Notes |
|---|---|---|
| Postgres + TimescaleDB | `timescale/timescaledb-ha:pg18` | Mirrors the production Dockerfile under `dc3-docker/dc3/dependencies/postgres` |
| RabbitMQ | `rabbitmq:3.13-management` | Publisher confirms + mandatory delivery enabled, mirroring `RabbitConfig` |
| MQTT | `eclipse-mosquitto:2.0` | Anonymous access on port 1883 for driver tests |

The shared wrappers live in
`dc3-common/dc3-common-test/src/main/java/io/github/pnoker/test/containers/`
and inject themselves into Spring environments via
`@DynamicPropertySource` callbacks.

For local reuse, contributors can opt-in by exporting:

```bash
export TESTCONTAINERS_REUSE_ENABLE=true
```

CI sets this automatically inside the integration job.

## 6. Test Data Strategy

- Small fixed payloads belong in `src/test/resources/fixtures/` so they
  can be diffed in PR review.
- Exhaustive object generation uses Instancio. Seeds are pinned with
  `Settings.create().set(Keys.SEED, ...)` for reproducibility.
- Database state is bootstrapped per test class with Testcontainers
  init scripts placed under each module's
  `src/test/resources/db/`. The repository-wide TimescaleDB extension is
  declared once in `dc3-common-test`.
- Sensitive data (api keys, tokens, passwords) is replaced with the
  literal string `should-not-leak` in fixture VOs to make accidental
  exposure obvious.

## 7. Contract Test Suites

`dc3-common-test` ships reusable JUnit 5 base classes:

- `EnumContractTest<E>`: subclass it with the enum class to assert
  `getIndex()` round-trips through `ofIndex(index)` for every constant.
- `SecretFieldContractTest`: subclass it with the VO classes you want to
  validate; the suite asserts secret-bearing fields never leak through
  `toString()`.

Additional base classes (BuilderRoundTrip, TenantPropagation, ...) are
introduced in their owning modules' `src/test/` trees during later
stages so they can refer to business BO/VO types without reverse
dependencies.

## 8. Local Commands

```bash
make test                 # Unit phase
make test-it              # Integration phase (requires Docker)
make coverage             # Aggregate jacoco report (target/site/jacoco-aggregate)
make test-e2e             # Full docker-compose end-to-end suite
mvn -B -pl <module> test  # Targeted unit run for a single module
```

All commands respect `-Dskip.unit.tests=true` and
`-Dskip.integration.tests=true` if a test phase needs to be bypassed
temporarily.

## 9. CI

| Workflow | Triggers | Jobs |
|---|---|---|
| `ci.yml` | push + PR to develop/release/main | `compile` (fast-fail) |
| `test.yml` | push + PR to develop/release/main | `unit`, `integration`, `coverage` |
| `e2e.yml` | push to develop/release/main + workflow_dispatch | `e2e` |

PR review expectations:

- The `unit` and `integration` jobs must be green before merge.
- The `coverage` job uploads the aggregate jacoco report as a workflow
  artifact named `jacoco-aggregate` and enforces the configured
  coverage thresholds.
- Coverage regressions of more than 1% in the aggregate report block
  the PR.

## 10. AI Test PR Discipline

Aligned with the `Commit Rules` in `AGENTS.md`:

- Tests for a stage land in a dedicated PR (e.g. PR-S2 for the auth
  domain), with commits split by intent within that PR.
- AI-authored commits use the documented `Claude <claude[bot]>` identity
  via command-scoped environment variables; repository-level git config
  is never modified.
- Before pushing a commit, the AI presents the proposed subject, the
  files included and the verification steps already executed.
