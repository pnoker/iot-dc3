# DC3 TSDB TimescaleDB

`dc3-tsdb-timescale` adapts the store-neutral port to TimescaleDB (embedded or standalone PostgreSQL) over
`spring-jdbc`.

## Activation

Active when `dc3.tsdb.type=timescale` — the default (`matchIfMissing = true`).

## Key types

| Type                             | Role                                                                                  |
|----------------------------------|---------------------------------------------------------------------------------------|
| `TimescaleTsdbStore`             | `TsdbStore` implementation (hypertable writes, latest-value upserts, history queries) |
| `TsdbTimescaleAutoConfiguration` | adapter wiring and rollup retention                                                   |

## Configuration

| Key                                          | Default | Meaning                                      |
|----------------------------------------------|---------|----------------------------------------------|
| `dc3.tsdb.timescale.rollup.minute-keep-days` | `365`   | minute-tier retention used by the rollup job |

## Dependencies

`dc3-tsdb-core`, `spring-jdbc`, `spring-boot-autoconfigure`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-timescale -am package
```

## Testing

No module-specific tests; behaviour is verified by `TimescaleContractTest` in `dc3-tsdb-tck` (Testcontainers).

## Related Modules

- `dc3-tsdb` — store-neutral port family
- `dc3-tsdb-tck` — contract suite
- `docs/tsdb-stores.md` — store selection guide
