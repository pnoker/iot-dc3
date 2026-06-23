---
title: Contributing Guide
---

# Contributing Guide

This page is for anyone getting ready to submit code, docs, or feedback to IoT DC3. By the end you'll know how to set up a local environment, move a change from branch to PR, write commit messages that hold up in release notes, and run the right checks before merging.

> You are here: ready to get involved. Before writing backend code, read the [Development Overview & Conventions](../development/) (the repository-root `AGENTS.md` is the authoritative source for engineering conventions); to get verification running, see [Testing](../development/testing).

## Ways to Contribute

Contributing is more than writing code. All four kinds below are welcome, and all of them matter:

- **Report reproducible bugs** — attach logs, version, configuration, and reproduction steps so maintainers don't have to guess.
- **Propose new features** — state the target scenario, the expected behavior, and the impact on existing compatibility.
- **Improve documentation** — add examples, translations, or troubleshooting notes. Even a one-character fix is worth a PR.
- **Submit code** — focused commits plus tests or verification notes, with every line of change traceable to a requirement.

::: tip Open an Issue Before You Start
For larger features or anything that changes behavior, open an Issue first to align on the approach before writing code. Small fixes can go straight to a PR.
:::

## Set Up Your Local Development Environment

The platform is a distributed service built on Java 21 / Spring Boot 4. Locally you need at least the dependency stack (PostgreSQL + RabbitMQ) running. Get the toolchain in place first, start the dependencies, then make sure the Java processes pick up the right runtime variables.

Supported toolchain:

- JDK 21
- Maven 3.9+
- Podman or Docker
- Make (optional, but recommended)

Start the local dependency stack from the repository root:

::: code-group

```bash [Start the dependency stack]
make up-db          # PostgreSQL + RabbitMQ
make up-optional    # Optional stack: EMQX / ELK / Prometheus / Grafana
```

```bash [Validate compose]
podman compose -f dc3/docker-compose-db.yml config --quiet
```

:::

When you run Java processes from source, you have to inject the runtime variables into the process yourself. The root `.env` only serves Docker Compose and will **not** be picked up by local Java processes automatically:

::: code-group

```bash [Run Java from a shell]
source dc3/env/dev.env.sh
```

```bash [Prepare Compose interpolation]
cp .env.example .env
```

:::

::: warning `.env` and `dev.env` Are Not the Same Thing
The root `.env` (copied from `.env.example`) is only for Docker Compose variable interpolation. Running Java locally from an IDE or CLI needs `dc3/env/dev.env` (read by the IDE EnvFile plugin) or `dc3/env/dev.env.sh` (sourced into the shell). For the differences between the four files and how to use them with JetBrains IDEA, see [Environment Variables Explained](../quickstart/environment).
:::

## Branches and Pull Requests

A contribution starts with a focused branch and ends with a focused PR. Keep unrelated refactors, formatting cleanups, and behavior changes separate so review can move quickly.

- Unless a maintainer says otherwise, branch your feature or fix off the latest `main`.
- Give branches semantic names, like `feature/<name>/<topic>` or `fix/<name>/<topic>`.
- Submit PRs against the `develop` branch.
- Keep the PR focused: don't mix refactors, formatting churn, and behavior changes into one PR unless they're all required for the same fix.
- Reference the relevant Issue in the PR description.

## Commit Messages: Conventional Commits

Commit messages are generated straight into release notes (`dc3/doc/CHANGE.md` is built from git history), so the subject has to be specific and readable. The format is fixed:

```text
<type>(<scope>): <english imperative summary>
```

- Write the subject in **English, lowercase, imperative mood**, specific enough to belong in release notes.
- Allowed types: `feat`, `fix`, `perf`, `refactor`, `docs`, `build`, `ci`, `test`, `chore`, `style`, `security`, `revert`.
- Add a scope for any change that isn't a tiny root-level one.
- Skip weak subjects like `update`, `fix`, `misc`, `wip`, or `.` — they make release notes unreadable.

Real examples:

```text
fix(manager): validate tenant scope for device queries
docs(env): explain JetBrains IDEA environment variables
refactor(container): deduplicate compose registry overrides
```

::: warning The Subject Goes Straight Into Release Notes
`dc3/doc/CHANGE.md` is generated from commit messages, and a weak subject makes the notes unreadable. Before you commit, check your subject against the format and the examples above to confirm it's specific and readable.
:::

## Verification Before Merging

Before opening a PR, run the checks that match what you changed. Verification scales with the change — you don't need to run everything every time.

::: code-group

```bash [Java / shared behavior]
mvn -s .mvn/settings.xml clean package
```

```bash [Container / compose]
podman compose -f dc3/docker-compose-db.yml config
make config STACK=db    # or app/dev/optional, depending on the stack touched
```

:::

- **Docs-only changes**: at minimum, verify by hand that links, commands, and formatting are correct.
- **Container changes**: for every compose file you touched, run `make config STACK=<app|dev|db|optional>` or `podman compose config`.
- **More testing conventions** (unit, integration, E2E, coverage gates) are covered in [Testing](../development/testing).

## Coding Conventions (Key Points)

The full spec in `AGENTS.md` is authoritative. This section lists only the rules contributors trip over most. None of them are style preferences — they're hard constraints on platform correctness.

- Follow the existing package structure, naming, validation, exception, logging, and facade patterns. Don't introduce new ones.
- **Tenant isolation is a hard requirement**: every new query, gRPC call, cache key, and data change must preserve the `tenantId` scope.
- For grouped configuration, prefer typed configuration properties with validation over scattered `@Value` annotations.
- Behavior changes must come with tests or focused verification notes, especially for shared common modules and cross-service contracts.
- Don't commit secrets, locally generated files, IDE metadata, or machine-specific configuration.

::: tip CRUD Verbs Follow Result Cardinality
The platform has no free naming space — the verb in a method name, HTTP path, or gRPC RPC has to reflect the result cardinality (`get` for a single record, `list` for a collection). See the [Development Overview & Conventions](../development/) for details.
:::

## Documentation and Translation

When you change the root README content, keep `README.md`, `README.zh.md`, `README.ja.md`, and `README.vi.md` structurally aligned. If you can't finish the translation sync in the same PR, say so in the PR description.

## Release Notes

Before tagging a release, generate a categorized changelog from git history:

```bash
make changelog
```

By default it reads the current version from `pom.xml`, compares `HEAD` against the nearest reachable `dc3.release.*` tag, and updates `dc3/doc/CHANGE.md`. You can override the range or version when needed:

```bash
make changelog FROM=dc3.release.20251005.00 TO=HEAD VERSION=2026.5.22
```

::: info Changelog-Only Commits
By default, release commits like "generate changelog" are skipped, so re-running after you commit `CHANGE.md` stays stable. Set `INCLUDE_CHANGELOG_COMMITS=true` only when those commits need to appear in the release notes.
:::

## License

The IoT DC3 Community Edition is licensed under the GNU Affero General Public License v3.0 or later. The license statements live in the repository-root `LICENSE-AGPL.txt` and `LICENSE.txt`.

## Further Reading

- [Development Overview & Conventions](../development/) — authoritative engineering conventions: CRUD verbs, layered calls, facade boundaries
- [Testing](../development/testing) — unit, integration, E2E, and coverage conventions
- [Environment Variables Explained](../quickstart/environment) — the differences between `.env` / `dev.env` / `dev.env.sh` and IDE usage
- [Code of Conduct](./code-of-conduct) — please read before participating in the community
- [Security Policy](./security) — how to report security vulnerabilities responsibly
