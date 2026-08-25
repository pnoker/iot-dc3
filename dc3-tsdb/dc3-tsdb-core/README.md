# DC3 TSDB Core

`dc3-tsdb-core` defines the store-neutral time-series port of IoT DC3: the `TsdbStore` SPI plus the `TsdbModel`
sample model and capability set. The Data Center persists point values through this port and never through
store-specific classes. The module has zero store dependencies.

## Key types

| Type | Role |
|---|---|
| `TsdbStore` | SPI implemented by every time-series adapter (write, query, schema, capabilities) |
| `TsdbModel` | sample model shared across adapters |

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-core -am package
```

## Testing

This module has no tests of its own; the port contract is verified against every store in `dc3-tsdb-tck`:

```bash
mvn -s .mvn/settings.xml -q -pl dc3-tsdb/dc3-tsdb-core -am -DskipTests compile
```

## Related Modules

- `dc3-tsdb-*` — store adapters implementing `TsdbStore`
- `dc3-tsdb-tck` — store-neutral contract suite
- `dc3-common-data` — primary consumer of the port
