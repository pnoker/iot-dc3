# DC3 DB TCK

`dc3-db-tck` is the relational contract suite for the canonical PostgreSQL runtime.
It executes the production R2DBC repository ports directly; JDBC, MyBatis and
alternate relational engines are deliberately absent from this module.

## Contract tests

`PostgresDialectContractIT` boots a disposable Testcontainers PostgreSQL instance and runs the
same `DatabaseClient`/repository contract used by production:

| Test                          | Engine     |
|-------------------------------|------------|
| `PostgresDialectContractIT` | PostgreSQL |

## Running

Requires a container runtime — the tests are annotated `@Testcontainers(disabledWithoutDocker = true)` and are skipped
without Docker:

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-tck -am verify
```

## Related Modules

- `dc3-db-r2dbc-core` / `dc3-db-r2dbc-runtime` — reactive contracts and runtime
- `dc3-db-r2dbc-postgres` — the only supported relational adapter
