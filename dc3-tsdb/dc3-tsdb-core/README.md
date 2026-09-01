# DC3 TSDB Core

`dc3-tsdb-core` defines the reactive, store-neutral time-series port of IoT DC3: the `TsdbStore` SPI plus the
`TsdbModel` sample model and capability set. Every operation returns `Mono` or `Flux`; synchronous adapters are not
part of the platform build.

## Key types

| Type        | Role                                                                              |
|-------------|-----------------------------------------------------------------------------------|
| `TsdbStore` | reactive SPI implemented by the R2DBC data store (write, query and capabilities) |
| `TsdbModel` | sample model shared across adapters                                               |

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-core -am package
```

## Testing

The contract is exercised by `dc3-common-data` unit tests and R2DBC integration tests.

## Related Modules

- `dc3-common-data` — native R2DBC implementation and primary consumer of the port
