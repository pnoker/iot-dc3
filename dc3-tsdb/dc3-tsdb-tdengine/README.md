# DC3 TSDB TDengine

`dc3-tsdb-tdengine` adapts the store-neutral port to TDengine over its REST/WS JDBC driver (`taos-jdbcdriver`). Points
are written to a supertable with per-series subtables.

## Activation

Active when `dc3.tsdb.type=tdengine`.

## Configuration

`TdengineTsdbProperties` binds the `dc3.tsdb.tdengine` prefix:

| Key                                   | Default                          | Meaning              |
|---------------------------------------|----------------------------------|----------------------|
| `dc3.tsdb.tdengine.url`               | `jdbc:TAOS-RS://localhost:6041/` | JDBC URL             |
| `dc3.tsdb.tdengine.username`          | `root`                           | login name           |
| `dc3.tsdb.tdengine.password`          | `taosdata`                       | login password       |
| `dc3.tsdb.tdengine.database`          | `dc3`                            | target database      |
| `dc3.tsdb.tdengine.maximum-pool-size` | `8`                              | connection pool size |

## Dependencies

`dc3-tsdb-core`, `taos-jdbcdriver`, `spring-jdbc`, HikariCP, `spring-boot-autoconfigure`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-tdengine -am package
```

## Testing

No module-specific tests; behaviour is verified by `TdengineContractTest` in `dc3-tsdb-tck` (Testcontainers).

## Related Modules

- `dc3-tsdb` — store-neutral port family
- `dc3-tsdb-tck` — contract suite
- `docs/tsdb-stores.md` — store selection guide
