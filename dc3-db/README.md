# DC3 DB

dc3-db is the relational dialect layer of IoT DC3. It provides dialect-neutral JDBC infrastructure — MyBatis-Plus
wiring, the tenant-line handler, and generator utilities — plus one adapter per supported database engine behind the
dc3.db.type property.

## Modules

| Module | Role |
|---|---|
| dc3-db-core | dialect-neutral JDBC infrastructure: MybatisPlusConfig, TenantLineHandlerImpl, MybatisUtil, profile wiring |
| dc3-db-postgres | PostgreSQL dialect adapter (default): driver, timestamptz type handler, pagination DbType |
| dc3-db-mysql | MySQL 8 dialect adapter: driver, DATETIME(6) UTC conventions |
| dc3-db-mariadb | MariaDB 10.6+ dialect adapter — MySQL-compatible surface except ODKU row aliases (uses VALUES()) |
| dc3-db-tck | dual-dialect relational contract suite — identical mapper-level assertions against PostgreSQL and MySQL |

## Selection

The active dialect is chosen by the dc3.db.type property (default postgres). Only the selected adapter's
auto-configuration is active.

```yaml
dc3:
  db:
    type: postgres
```

## Build and verify

```bash
mvn -s .mvn/settings.xml -q -f dc3-db/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-db/pom.xml test
```

Dialect conventions and migration notes live in docs/db-dialects.md.
