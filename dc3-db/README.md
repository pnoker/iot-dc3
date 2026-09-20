# DC3 DB

dc3-db is the relational persistence layer of IoT DC3. The runtime is PostgreSQL-only and
R2DBC-only: Spring Boot owns one pooled `ConnectionFactory`, repositories use explicit reactive
ports, and schema fingerprints fail startup when the database contract is wrong. JDBC/MyBatis are
not supported runtime paths.

## Modules

| Module          | Role                                                                                 |
|-----------------|--------------------------------------------------------------------------------------|
| dc3-db-core     | Framework-neutral `PageRequest`/`OffsetPage`/`CursorPage`, tenant and operation contracts |
| dc3-db-runtime  | Spring Boot R2DBC pool, reactive transactions, the single PostgreSQL dialect adapter and the schema-fingerprint startup gate |
| dc3-db-auth     | PostgreSQL R2DBC stores for the auth domain ports                                    |
| dc3-db-manager  | PostgreSQL R2DBC stores for the manager domain ports                                 |
| dc3-db-data     | PostgreSQL R2DBC stores for the data domain ports                                    |
| dc3-db-agentic  | PostgreSQL R2DBC stores for the agentic domain ports                                 |
| dc3-db-tck      | PostgreSQL Testcontainers contract suite                                             |

## Selection

The only supported relational engine is PostgreSQL. The postgres dialect registers
unconditionally from `dc3-db-runtime`; the runtime rejects startup unless exactly one
`R2dbcDialect` bean is present.
Changing the database requires a deliberate architecture change, not a runtime compatibility
switch.

## Build and verify

```bash
mvn -s .mvn/settings.xml -q -f dc3-db/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-db/pom.xml test
python3 dc3/bin/schema_fingerprint.py --check
```

Dialect conventions and migration notes live in docs/db-dialects.md.
