# DC3 TSDB

dc3-tsdb contains the store-neutral reactive time-series contract used by the Data Center. Relational point history is
implemented by `dc3-common-data`'s native R2DBC store; no synchronous JDBC or pseudo-reactive adapter is shipped.

## Modules

| Module             | Role                                                                                             |
|--------------------|--------------------------------------------------------------------------------------------------|
| dc3-tsdb-core      | store-neutral reactive `TsdbStore` port, `TsdbModel` sample model and capability set             |

## Selection

The active relational store is the R2DBC connection configured by the Data Center. There is no runtime adapter
selection and no JDBC fallback.

## Build and verify

```bash
mvn -s .mvn/settings.xml -q -f dc3-tsdb/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-tsdb/pom.xml test
```

The reactive boundary and migration gates are documented in `docs/design/relational-r2dbc.md`.
