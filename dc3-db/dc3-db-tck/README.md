# DC3 DB TCK

`dc3-db-tck` is the relational contract suite of the `dc3-db` family: identical mapper-level assertions executed
against every supported dialect, so a dialect adapter is certified by the same behaviour matrix.

## Contract tests

`AbstractDbDialectContractTest` defines the shared assertions; one concrete test per dialect boots the engine in a
disposable Testcontainers container and runs the real mappers from `dc3-common-auth` / `dc3-common-data` /
`dc3-common-manager`:

| Test | Dialect |
|---|---|
| `PostgresDialectContractTest` | PostgreSQL |
| `MysqlDialectContractTest` | MySQL |
| `MariadbDialectContractTest` | MariaDB |

## Running

Requires a container runtime — the tests are annotated `@Testcontainers(disabledWithoutDocker = true)` and are skipped
without Docker:

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-tck test
```

## Related Modules

- `dc3-db-core` — dialect-neutral base under test
- `dc3-db-postgres` / `dc3-db-mysql` / `dc3-db-mariadb` — the adapters under test
