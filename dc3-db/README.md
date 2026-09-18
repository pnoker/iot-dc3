# DC3 DB

dc3-db is the relational persistence layer of IoT DC3. The runtime is PostgreSQL-only and
R2DBC-only: Spring Boot owns one pooled `ConnectionFactory`, repositories use explicit reactive
ports, and schema fingerprints fail startup when the database contract is wrong. JDBC/MyBatis are
not supported runtime paths.

## Modules

| Module                  | Role                                                                                 |
|-------------------------|--------------------------------------------------------------------------------------|
| dc3-db-r2dbc-core       | Framework-neutral `PageRequest`/`OffsetPage`/`CursorPage`, tenant and operation contracts |
| dc3-db-r2dbc-runtime    | Spring Boot R2DBC pool, reactive transactions and schema-fingerprint startup gate   |
| dc3-db-r2dbc-postgres   | PostgreSQL R2DBC dialect and JSONB/identifier conventions                           |
| dc3-db-postgres-auth    | PostgreSQL R2DBC stores for the auth domain ports                                   |
| dc3-db-postgres-manager | PostgreSQL R2DBC stores for the manager domain ports                                |
| dc3-db-postgres-data    | PostgreSQL R2DBC stores for the data domain ports                                   |
| dc3-db-postgres-agentic | PostgreSQL R2DBC stores for the agentic domain ports                                |
| dc3-db-tck              | PostgreSQL Testcontainers contract suite                                             |

## Selection

The only supported relational engine is PostgreSQL. The dialect is fixed to `postgres` in the
runtime configuration; changing the database requires a deliberate architecture change, not a
runtime compatibility switch.

```yaml
dc3:
  db:
    type: postgres
```

## Build and verify

```bash
mvn -s .mvn/settings.xml -q -f dc3-db/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-db/pom.xml test
python3 dc3/bin/schema_fingerprint.py --check
```

Dialect conventions and migration notes live in docs/db-dialects.md.
