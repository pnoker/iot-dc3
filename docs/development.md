# Development and quality commands

The repository uses the root `Makefile` as its project-level command entry point.
There is no root Node package or pnpm workspace. The Web application and TypeScript
CLI keep independent `package.json` and `pnpm-lock.yaml` files.

## Install Node dependencies

Use Node.js 22 or newer and install both independent packages from the repository
root:

```bash
make install-node
```

When changing dependencies, update only the affected package and lockfile:

```bash
(cd dc3-web && corepack pnpm add <package>)
(cd dc3-cli && corepack pnpm add <package>)
```

## Development services

Start PostgreSQL and RabbitMQ before running backend services from source:

```bash
make up-db
```

The backend development commands build the selected Spring Boot modules and load
variables from `dc3/env/dev.env`:

| Command            | Service                                                          |
| ------------------ | ---------------------------------------------------------------- |
| `make dev-auth`    | Auth center (`8300`)                                             |
| `make dev-gateway` | Gateway (`8000`)                                                 |
| `make dev-data`    | Data center (`8500`)                                             |
| `make dev-manager` | Manager center (`8400`)                                          |
| `make dev-agentic` | Agentic center (`8600`)                                          |
| `make dev`         | All five backend services with prefixed logs and shared shutdown |
| `make dev-web`     | Vue development server                                           |
| `make dev-cli`     | TypeScript CLI build watcher                                     |

Set `DC3_ENV_FILE`, `MAVEN_CMD`, or `JAVA_CMD` when an alternative environment or
executable is required:

```bash
DC3_ENV_FILE=dc3/env/dev.env MAVEN_CMD=/path/to/mvn make dev-auth
```

## Repository quality

The Make targets preserve each toolchain's native configuration while providing a
single repository-level interface:

```bash
make format  # apply Java Spotless, Web ESLint fixes, and CLI Prettier/ESLint fixes
make lint    # check Java Checkstyle plus Web and CLI ESLint
make check   # run all non-mutating Java, Web, CLI, and Python quality gates
```

`make check` includes:

- Java formatting and canonical `COPYRIGHT` headers through Spotless;
- Java naming and Javadoc structure through Checkstyle;
- strict public API doclint and reference validation;
- Web TypeScript and ESLint checks;
- CLI Prettier, ESLint, and build checks;
- Python, Node, and shell syntax validation for `dc3/bin` tools.

Component-level targets remain available for focused work:

```bash
make validate-java-quality
make validate-web-quality
make validate-cli-quality
make validate-scripts
```

Tests remain separate from formatting and static quality checks. Use `make test`,
`make test-it`, `make test-e2e`, or the affected package's native pnpm test command.
