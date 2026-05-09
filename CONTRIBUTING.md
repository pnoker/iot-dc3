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
make dev-db
make dev-optional
```

For source-run Java processes, load runtime variables from:

```bash
source dc3/env/dev.env.sh
```

For Docker Compose interpolation, copy the root template first:

```bash
cp .env.example .env
```

See `dc3/doc/ENVIRONMENT.md` for the difference between `.env.example`, `.env`, `dc3/env/dev.env`, and
`dc3/env/dev.env.sh`, including JetBrains IDEA usage.

## Branches and Pull Requests

- Create feature and fix branches from the latest `main` branch unless a maintainer asks otherwise.
- Use descriptive branch names such as `feature/<name>/<topic>` or `fix/<name>/<topic>`.
- Open pull requests against `develop`.
- Keep pull requests focused. Avoid mixing refactors, formatting churn, and behavior changes unless they are necessary
  for the same fix.
- Reference related issues in the pull request description.

## Commit Messages

Use concise Conventional Commit-style subjects:

```text
fix(manager): validate tenant scope for device queries
docs(env): explain JetBrains IDEA environment variables
refactor(container): deduplicate compose registry overrides
```

## Build and Verification

Before opening a pull request, run the checks that match the touched area:

```bash
mvn -s .mvn/settings.xml clean package
docker compose -f dc3/docker-compose-db.yml config
```

For documentation-only changes, at least check links, commands, and formatting manually. For container changes, run
`docker compose config` for every touched compose file.

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
