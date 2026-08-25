# DC3 TSDB TCK

`dc3-tsdb-tck` is the store-neutral contract suite of the `dc3-tsdb` family: an adapter that passes these tests is
compliant with the time-series port. `AbstractTsdbContractTest` defines the shared contract (write, latest-value
read, history read, and schema behaviour); one concrete test per store boots the engine in a disposable Testcontainers
container.

## Contract tests

| Test | Store |
|---|---|
| `TimescaleContractTest` | TimescaleDB |
| `TdengineContractTest` | TDengine |
| `InfluxdbContractTest` | InfluxDB 3 |
| `IotdbContractTest` | Apache IoTDB |

## Running

Requires a container runtime — the tests are skipped without Docker:

```bash
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-tck test
```

## Related Modules

- `dc3-tsdb-core` — the port being verified
- `dc3-tsdb-*` — the adapters under test
