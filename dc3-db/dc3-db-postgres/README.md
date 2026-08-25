# DC3 DB PostgreSQL

`dc3-db-postgres` is the PostgreSQL dialect adapter of the `dc3-db` family: the `org.postgresql.Driver` driver,
`TIMESTAMPTZ` → `LocalDateTime` type handling, and PostgreSQL pagination `DbType`.

## Activation

Active when `dc3.db.type=postgres` — the default (`matchIfMissing = true`).

## Key types

| Type | Role |
|---|---|
| `PostgresDbAutoConfiguration` | adapter wiring selected by `dc3.db.type` |
| `TimestamptzLocalDateTimeTypeHandler` | `TIMESTAMPTZ` bridge into `LocalDateTime` |
| `ActivePostgresProfileConfig` | activates the PostgreSQL profile |

`application-postgres.yml` registers the master datasource, driver, and type-handler scanning.

## Dependencies

`dc3-db-core`, `postgresql`, `spring-boot-autoconfigure`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-postgres -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-postgres -am test
```

## Related Modules

- `dc3-db-core` — dialect-neutral base
- `dc3-db-tck` — dialect contract suite
- `docs/db-dialects.md` — dialect conventions
