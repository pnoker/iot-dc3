# `dc3/bin` tooling

This directory contains repository automation and local development helpers.
The implementation language follows the tool's operating-system and parsing
needs; the root `Makefile` provides the stable project-level entry points.

## Tool index

| Tool                              | Runtime                              | Purpose                                                                                 | Safe default entry                 |
| --------------------------------- | ------------------------------------ | --------------------------------------------------------------------------------------- | ---------------------------------- |
| `audit_controller_permissions.py` | Python standard library              | Audit controller mappings, authorization annotations, and resource-registration closure | `make validate-permissions`        |
| `check_documentation.py`          | Python standard library              | Validate module metadata, links, protobuf documentation, and public Javadocs            | `make validate-documentation`      |
| `check_postgres_init.py`          | Python standard library              | Validate PostgreSQL initialization SQL comments, tables, columns, and entity mappings   | `make validate-postgres-init`      |
| `check_r2dbc_migration.py`        | Python standard library              | Detect legacy MyBatis/JDBC, pagination, response envelopes, and blocking bridges        | `make validate-r2dbc-migration`    |
| `schema_fingerprint.py`           | Python standard library              | Check or synchronize the canonical PostgreSQL schema fingerprint                        | `make validate-schema-fingerprint` |
| `changelog.py`                    | Python standard library + Git        | Generate the categorized release changelog from commit history                          | `make changelog`                   |
| `release_backfill.py`             | Python standard library + GitHub CLI | Dry-run or apply missing GitHub Releases recorded in `CHANGE.md`                        | `make release-backfill`            |
| `dev.mjs`                         | Node.js built-ins + Maven/Java       | Build and run one or all Spring Boot services with shared signal handling               | `make dev`                         |
| `export_openapi.sh`               | Bash + curl                          | Export OpenAPI documents from a running gateway                                         | `make openapi`                     |
| `tag.sh`                          | Bash + Git/GitHub remote             | Validate and push the release tag derived from `pom.xml`                                | `make tag`                         |

## Why Python remains supported

The Python tools use only the standard library (plus external `git`/`gh`
commands where their workflows already require them). Keeping them in Python
avoids adding Node package dependencies to backend tooling, keeps backend CI
independent from Node installation, and preserves the behavior of release and
documentation gates. A Node rewrite should be considered only for a new tool
whose primary responsibility is already part of the Web or CLI package.

## Validation

Run the syntax gate for all Python, Node, and shell tools with:

```bash
make validate-scripts
```

Run the complete repository quality gate with:

```bash
make check
```

The complete check includes `dc3/bin` syntax validation, Java Spotless and
Checkstyle checks, strict public-API Javadoc validation, CLI Prettier, and
Web/CLI ESLint. The Java, Web, and CLI gates are also available through
`make validate-java-quality`, `make validate-web-quality`, and
`make validate-cli-quality`. The Java formatter and header source are configured
in the root Maven build; the canonical copyright text remains `COPYRIGHT`.
