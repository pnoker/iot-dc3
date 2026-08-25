# DC3 DB Core

`dc3-db-core` is the dialect-neutral JDBC infrastructure of the `dc3-db` family: dynamic-datasource wiring, MyBatis-Plus
configuration, the automatic tenant-line handler, and code-generator utilities. Dialect adapters layer their driver and
conventions on top.

## Key types

| Type                      | Role                                                |
|---------------------------|-----------------------------------------------------|
| `MybatisPlusConfig`       | dynamic-datasource and MyBatis-Plus wiring          |
| `TenantLineHandlerImpl`   | automatic tenant-scope line for tenant-owned tables |
| `MybatisUtil`             | code-generator utilities                            |
| `ActiveJdbcProfileConfig` | activates the `jdbc` profile                        |

Shared datasource defaults live in `application-jdbc.yml`.

## Dependencies

`dynamic-datasource-spring-boot4-starter`, `mybatis-plus-spring-boot4-starter`, `mybatis-plus-jsqlparser`,
`mybatis-plus-generator`, `spring-boot-starter-jdbc`, `dc3-common-constant`, `dc3-common-exception`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-core -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-core -am test
```

## Related Modules

- `dc3-db-postgres` / `dc3-db-mysql` / `dc3-db-mariadb` — dialect adapters layered on this module
- `dc3-db-tck` — dual/tri-dialect relational contract suite
- `docs/db-dialects.md` — dialect conventions
