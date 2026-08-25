# DC3 DB MySQL

`dc3-db-mysql` is the MySQL 8 dialect adapter of the `dc3-db` family: the `com.mysql.cj.jdbc.Driver` driver,
`DATETIME(6)` UTC conventions, and MySQL pagination `DbType`.

## Activation

Active when `dc3.db.type=mysql`.

## Key types

| Type                       | Role                                                       |
|----------------------------|------------------------------------------------------------|
| `MysqlDbAutoConfiguration` | adapter wiring selected by `dc3.db.type`                   |
| `ActiveMysqlProfileConfig` | activates the MySQL profile (PostgreSQL stays the default) |

`application-mysql.yml` registers the master datasource, driver, and the `jdbc-type-for-null` convention.

## Dependencies

`dc3-db-core`, `mysql-connector-j`, `spring-boot-autoconfigure`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-mysql -am package
```

## Testing

No module-specific tests; dialect behaviour is verified by `MysqlDialectContractTest` in `dc3-db-tck`.

## Related Modules

- `dc3-db-core` — dialect-neutral base
- `dc3-db-tck` — dialect contract suite
- `docs/db-dialects.md` — dialect conventions
