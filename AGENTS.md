# AGENTS.md

Canonical engineering instructions for AI coding agents working in IoT DC3.

This file is kept lean on purpose: it holds always-relevant rules and a task routing table. Deep, task-specific
conventions live in `dc3/agents/*.md` and are read on demand.

## Scope and precedence

- This file applies to the whole repository; the `dc3/agents/*.md` guides refine it per task area.
- Follow the user's request first, then this repository-level guidance.
- Keep changes focused. Do not revert unrelated work in a dirty worktree.

## Project in one paragraph

IoT DC3 is a multi-protocol, cloud-native, open-source industrial IoT platform evolving toward AI agents: a Spring Cloud
Gateway entrypoint, Auth/Manager/Data/Agentic centers, reactive R2DBC persistence, gRPC facades, protocol drivers on a
shared SDK, a Vue web UI in `dc3-web/`, and a standalone TypeScript CLI in `dc3-cli/`. The backend is a Java 21 Maven
multi-module Spring Boot/Spring Cloud project (verify versions in the root `pom.xml`). For the full project story and
capability overview, see `README.ai.md`.

## First principles

When no explicit rule covers a situation, derive the answer from these:

- Fix root causes, not symptoms: a patch that silences a failure without removing its cause is technical debt.
- Understand the mechanism before changing it: read the implementation first, and derive the fix from the lowest
  broken layer instead of stacking workarounds.
- Executable configuration is the only source of truth; documentation drifts.
- Tenant isolation is a correctness and security requirement, never a trade-off.
- Wire contracts evolve backward-compatibly; breaking changes are explicit and `!`-marked.
- Code lives with its owner; dependency direction never reverses.
- Reuse before abstracting; add complexity only when the task demands it.
- Every change is verifiable: run the proportionate checks and report honestly what passed.
- Secrets never appear in contracts, logs, or serialization.

## Sources of truth

Avoid copying volatile versions or generated state into documentation. Verify them at the source:

| Concern                                   | Source of truth                                                |
|-------------------------------------------|----------------------------------------------------------------|
| Java, Spring, Maven plugins, reactor modules | root `pom.xml` and affected module POMs                     |
| Backend build commands                     | root `Makefile`                                                |
| Frontend dependencies, scripts, tool pins  | `dc3-web/package.json`, `pnpm-lock.yaml`, `dc3-web/Dockerfile` |
| Containers and registries                  | root `Makefile`, `.env.example`, and `dc3/docker-compose*.yml` |
| CI behaviour                               | `.github/workflows/`                                           |
| Release notes                              | `dc3/bin/changelog.py` and generated `dc3/doc/CHANGE.md`       |

If this file disagrees with executable configuration, treat the executable configuration as current and update this file
as part of the same change when appropriate.

## Validation matrix

Run checks proportionate to the change:

- Java/shared behaviour: `mvn -s .mvn/settings.xml -q -DskipTests compile`.
- Tested behaviour: affected unit tests, then `make test` when warranted.
- DAL/SQL or infrastructure integration: affected ITs or `make test-it` with a container runtime.
- gRPC: compile generated sources and run matching client/server contract tests.
- Coverage-sensitive changes: `make coverage` and inspect the aggregate report.
- Changelog tooling: `python3 -m py_compile dc3/bin/changelog.py`.
- Compose: render/validate every touched configuration.
- YAML: parse after accounting for Maven placeholders such as `@project.artifactId@`.
- Agent/docs changes: validate referenced paths, targets, scripts, test selectors, and links; `make validate-documentation`.
- Documentation or public Javadoc changes: run `make validate-documentation` and `make validate-javadoc`.

Report what was verified and what was not verified before handing off public-behaviour changes.

## Commands

Prefer a root `Makefile` target when one exists. `make help` lists every target, including generated Compose shortcuts
such as `make up-db` and `make config-dev`; a full command table also lives in `docs/development.md`. Frequently used:

```bash
make check        # complete non-mutating quality gate
make test         # unit tests
make test-it      # integration tests
make test-e2e     # E2E harness
make coverage     # aggregate JaCoCo report
make openapi      # export OpenAPI from a running stack
```

For direct Maven work, use the checked-in settings file locally:

```bash
mvn -s .mvn/settings.xml -q -DskipTests compile
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager -Dtest=DriverControllerTest
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-public \
  -Dtest="RTest#okWithDataExposesDataAndDefaultEnvelope"
```

When using `-am` together with `-Dtest`, add `-Dsurefire.failIfNoSpecifiedTests=false` so dependency modules without the
selected test do not fail spuriously.

GitHub Actions should normally use public Maven repositories rather than the local mirror settings unless a workflow is
intentionally testing that mirror.

## Hard rules

- Tenant isolation is a hard requirement. Preserve tenant scope in every new query and mutation, carry tenant IDs
  through gRPC where the contract supports them, and include tenant context in cache keys for tenant-owned data. Treat
  missing tenant validation as a correctness and security defect, not a convenience trade-off.
- Never add `tenantId IS NULL` shortcuts unless the data model explicitly defines global records.
- Never expose secrets in VOs or logs. Exclude `apiKey`, `password`, `secret`, `token`, and credential fields from
  serialization, `@ToString`, and info-level logging.
- `dc3/doc/CHANGE.md` is generated from Git history. Do not hand-edit the current release block unless fixing generator
  output. Release, tag, and backfill workflows live in `CONTRIBUTING.md`.

## Commit rules

Use Conventional Commit subjects:

```text
<type>(optional-scope): <english imperative summary>
```

Allowed types are `feat`, `fix`, `perf`, `refactor`, `docs`, `build`, `ci`, `test`, `chore`, `style`, `security`,
`revert`. Use `!` for breaking changes and explain the impact in the body. Keep subjects specific enough for release
notes; a changelog-only commit must use exactly `docs(release): update generated changelog`.

The tracked `.husky/pre-commit` hook runs frontend `lint-staged`. Do not claim commit-message enforcement unless a
tracked commit-msg validation hook is added.

Release-workflow changes should update `CONTRIBUTING.md` and this file.

## Task routing

Read the matching guide before changing that area:

| When the task touches                          | Read first                              |
|------------------------------------------------|-----------------------------------------|
| Backend Java, REST/gRPC contracts, drivers     | `dc3/agents/backend.md`                 |
| `dc3-web/`                                     | `dc3/agents/frontend.md`                |
| `dc3-cli/` or gateway CLI subcommands          | `dc3/agents/cli.md`                     |
| Writing/running tests, coverage                | `dc3/agents/testing.md`                 |
| Releases, tags, backfill                       | `CONTRIBUTING.md` (Release Notes)       |
| Modules, Compose files, CI workflows           | root `pom.xml`, `Makefile`, `.github/`  |

## Environment and Compose

- `.env.example` is the Compose interpolation template; `.env` is local and untracked, created through `make init-env`.
- `dc3/env/dev.env` is for IDE/local Java processes without `export`; `dc3/env/dev.env.sh` is shell-sourceable with
  `export`. Do not treat these files as interchangeable.
- Canonical Compose files are under `dc3/`. Registry choice is controlled through Make arguments and environment
  variables, not duplicated Compose files.
- After a Compose change, validate every touched stack with its corresponding `make config-*` target or an equivalent
  `docker compose ... config` / `podman compose ... config` command.

## Editing and documentation

- Preserve AGPL headers where they already exist.
- Prefer existing patterns and helpers over new abstractions.
- Keep public/user-facing project text in English unless editing a localized document.
- Document public types and non-override methods with their contract, constraints, nullability, tenant scope,
  concurrency semantics, or other non-obvious invariants. An inherited contract is sufficient for an `@Override`
  method. Do not restate the signature or implementation line by line.
- Keep code comments focused on intent and design constraints. Remove comments that merely narrate the next statement,
  and update comments in the same change as the behavior they describe.
- Do not use volatile `@version` Javadoc tags. Use `@since` only when it records a stable public API milestone.
- Avoid generated metadata churn unless required by the task.
- Use structured parsers or project toolchains for structured files when practical.
- Keep multilingual root READMEs structurally aligned.
