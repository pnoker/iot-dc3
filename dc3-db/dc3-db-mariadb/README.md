# DC3 DB MariaDB

`dc3-db-mariadb` is the MariaDB 10.6+ dialect adapter of the `dc3-db` family: the `org.mariadb.jdbc.Driver`
driver with a MySQL-compatible surface — the only known difference is ODKU row aliases, which MariaDB writes as
`VALUES()` instead.

## Activation

Active when `dc3.db.type=mariadb`.

## Key types

| Type                         | Role                                     |
|------------------------------|------------------------------------------|
| `MariadbDbAutoConfiguration` | adapter wiring selected by `dc3.db.type` |
| `ActiveMariadbProfileConfig` | activates the MariaDB profile            |

`application-mariadb.yml` registers the master datasource and driver.

## Dependencies

`dc3-db-core`, `mariadb-java-client`, `spring-boot-autoconfigure`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-mariadb -am package
```

## Testing

No module-specific tests; dialect behaviour is verified by `MariadbDialectContractTest` in `dc3-db-tck`.

## Related Modules

- `dc3-db-core` — dialect-neutral base
- `dc3-db-tck` — dialect contract suite
- `docs/db-dialects.md` — dialect conventions
