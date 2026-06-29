# IoT DC3 Contributor Guide

Thank you for contributing to IoT DC3. This guide keeps contributions predictable for maintainers and safe for users.

## Ways to Contribute

- Report reproducible bugs with logs, versions, configuration, and steps to reproduce.
- Propose features with the target scenario, expected behavior, and compatibility impact.
- Improve documentation, examples, translations, and troubleshooting notes.
- Submit code changes with focused commits and tests or verification notes.

## Development Environment

Use the supported toolchain:

- JDK 21
- Maven 3.9+
- Podman or Docker
- Make, optional but recommended

Start the local dependency stack from the repository root:

```bash
make up-db
make up-optional
```

For source-run Java processes, load runtime variables from:

```bash
source dc3/env/dev.env.sh
```

For Compose interpolation, copy the root template first:

```bash
cp .env.example .env
```

See `dc3/doc/ENVIRONMENT.md` for the difference between `.env.example`, `.env`, `dc3/env/dev.env`, and
`dc3/env/dev.env.sh`, including JetBrains IDEA usage.

## Branches and Pull Requests

IoT DC3 follows a simplified Git Flow:

- `develop` — integration branch. Cut `feature/<scope>` branches from `develop` and open pull requests back against
  `develop`. Full CI (lint / test / build / e2e) runs here.
- `main` — production trunk. Verified work is promoted from `develop` to `main` via pull request. Each merge to `main`
  is a release (a tag is cut and artifacts are published).
- `hotfix/<scope>` — cut from `main` for production fixes; open the PR back against `main` (then tag), and back-merge to
  `develop`.
- `release` — archived (read-only). It is kept for history only; do not open pull requests against it.

Use descriptive branch names such as `feature/<name>/<topic>` or `fix/<name>/<topic>`. Keep pull requests focused —
avoid mixing refactors, formatting churn, and behavior changes unless they are necessary for the same fix. Reference
related issues in the pull request description.

## Commit Messages

Use concise Conventional Commit-style subjects:

```text
fix(manager): validate tenant scope for device queries
docs(env): explain JetBrains IDEA environment variables
refactor(container): deduplicate compose registry overrides
```

Allowed types are `feat`, `fix`, `perf`, `refactor`, `docs`, `build`, `ci`, `test`, `chore`, `style`, `security`, and
`revert`. Use English, keep the subject specific, and avoid vague descriptions such as `update`, `fix`, `misc`, `wip`,
or `.` because release notes are generated from commit history.

Husky Git hooks are pre-installed in the repository (`.husky/`). The `pre-commit` hook automatically runs lint-staged
(eslint + prettier) on staged files before each commit. No manual setup is needed.

## Build and Verification

Before opening a pull request, run the checks that match the touched area:

```bash
mvn -s .mvn/settings.xml clean package
podman compose -f dc3/docker-compose-db.yml config
```

For documentation-only changes, at least check links, commands, and formatting manually. For container changes, run
`make config STACK=<app|dev|db|optional>` or `podman compose config` for every touched compose file.

## Release Notes

Before tagging a release, generate the categorized changelog from git:

```bash
make changelog
```

By default this reads the current version from `pom.xml`, compares `HEAD` with the latest reachable `v*`
(semver) tag, and updates `dc3/doc/CHANGE.md`. You can override the range or version when needed:

```bash
make changelog FROM=v2025.9.3 TO=HEAD VERSION=2026.5.22
```

To cut a release, switch to `main` and create the next semver tag (this also opens a GitHub Release):

```bash
make tag            # patch: v2025.9.3 -> v2025.9.4
make tag minor      # minor: v2025.9.4 -> v2025.10.0
make tag major      # major: v2025.10.0 -> v2026.0.0
```

`bash dc3/bin/tag.sh --dry-run` previews the next tag without pushing. Tagging only runs on `main`.

Generated changelog-only release commits are skipped by default so rerunning the command after committing
`CHANGE.md` remains stable. Set `INCLUDE_CHANGELOG_COMMITS=true` only when those commits should appear in
release notes.

## Coding Guidelines

- Follow the existing package structure, naming, validation, exception, logging, and facade patterns.
- Keep tenant-aware behavior explicit. New queries, gRPC calls, cache keys, and data mutations must preserve tenant
  scope.
- Prefer typed configuration properties with validation over scattered `@Value` usage for grouped settings.
- Add tests or focused verification for behavior changes, especially shared common modules and cross-service contracts.
- Do not commit secrets, generated local files, IDE metadata, or machine-specific configuration.

## Documentation and Translation

When changing root README content, keep `README.md`, `README.zh.md`, `README.ja.md`, and `README.vi.md` structurally
aligned. If a translated update is not possible in the same pull request, call it out clearly in the pull request
description.

## License

IoT DC3 Community Edition is licensed under the GNU Affero General Public License v3.0 or later. See
`LICENSE-AGPL.txt` and `LICENSE.txt` for the project license notice.
