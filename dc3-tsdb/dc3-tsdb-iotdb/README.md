# DC3 TSDB IoTDB

`dc3-tsdb-iotdb` adapts the store-neutral port to Apache IoTDB over the session API (`iotdb-session`). Series are
stored as tree paths under `root.dc3.*`.

## Activation

Active when `dc3.tsdb.type=iotdb`.

## Configuration

`IotdbTsdbProperties` binds the `dc3.tsdb.iotdb` prefix:

| Key | Default | Meaning |
|---|---|---|
| `dc3.tsdb.iotdb.host` | `localhost` | IoTDB host |
| `dc3.tsdb.iotdb.port` | `6667` | session port |
| `dc3.tsdb.iotdb.username` | `root` | login name |
| `dc3.tsdb.iotdb.password` | `root` | login password |

## Dependencies

`dc3-tsdb-core`, `iotdb-session`, `spring-boot-autoconfigure`.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-tsdb/dc3-tsdb-iotdb -am package
```

## Testing

No module-specific tests; behaviour is verified by `IotdbContractTest` in `dc3-tsdb-tck` (Testcontainers).

## Related Modules

- `dc3-tsdb` — store-neutral port family
- `dc3-tsdb-tck` — contract suite
- `docs/tsdb-stores.md` — store selection guide
