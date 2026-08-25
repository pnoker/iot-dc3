# DC3 TSDB InfluxDB

`dc3-tsdb-influxdb` adapts the store-neutral port to InfluxDB 3 over the documented v3 HTTP SQL and line-protocol APIs.
Points map to tag/field columns; no InfluxDB-specific JDBC or client SDK is required.

## Activation

Active when `dc3.tsdb.type=influxdb`.

## Configuration

`InfluxdbTsdbProperties` binds the `dc3.tsdb.influxdb` prefix:

| Key                          | Default                 | Meaning                  |
|------------------------------|-------------------------|--------------------------|
| `dc3.tsdb.influxdb.url`      | `http://localhost:8181` | InfluxDB 3 HTTP endpoint |
| `dc3.tsdb.influxdb.token`    | *(empty)*               | auth token               |
| `dc3.tsdb.influxdb.database` | `dc3`                   | target database          |

## Dependencies

`dc3-tsdb-core`, `jackson-databind`, `spring-boot-autoconfigure`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-influxdb -am package
```

## Testing

No module-specific tests; behaviour is verified by `InfluxdbContractTest` in `dc3-tsdb-tck` (Testcontainers).

## Related Modules

- `dc3-tsdb` — store-neutral port family
- `dc3-tsdb-tck` — contract suite
- `docs/tsdb-stores.md` — store selection guide
