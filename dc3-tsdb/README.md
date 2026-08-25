# DC3 TSDB

dc3-tsdb is the pluggable time-series storage layer of IoT DC3. It defines a store-neutral port — the TsdbStore SPI with
a sample model and capability set — plus one adapter per supported time-series database. The Data Center writes point
values through the port and never through store-specific classes.

## Modules

| Module             | Role                                                                                             |
|--------------------|--------------------------------------------------------------------------------------------------|
| dc3-tsdb-core      | store-neutral port: TsdbStore SPI, TsdbModel sample model, capabilities; zero store dependencies |
| dc3-tsdb-timescale | TimescaleDB adapter (default; embedded or standalone PostgreSQL)                                 |
| dc3-tsdb-tdengine  | TDengine adapter — supertable + per-series subtables over the REST/WS JDBC driver                |
| dc3-tsdb-influxdb  | InfluxDB 3 adapter — tags/fields over the documented v3 HTTP SQL and line-protocol APIs          |
| dc3-tsdb-iotdb     | Apache IoTDB adapter — tree paths root.dc3.* over the session API                                |
| dc3-tsdb-tck       | store-neutral contract suite: an adapter that passes these tests is compliant                    |

## Selection

The active store is chosen by the dc3.tsdb.type property (default timescale). Only the selected adapter's
auto-configuration is active.

```yaml
dc3:
  tsdb:
    type: timescale
```

Store-specific settings use the dc3.tsdb.<store>.* prefix (e.g. dc3.tsdb.timescale.rollup.minute-keep-days).

## Build and verify

```bash
mvn -s .mvn/settings.xml -q -f dc3-tsdb/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-tsdb/pom.xml test
```

Store selection guides and trade-offs live in docs/tsdb-stores.md.
