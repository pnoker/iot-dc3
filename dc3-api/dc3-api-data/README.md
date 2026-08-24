# DC3 API Data

`dc3-api-data` defines gRPC contracts for values, commands, events, status, and system-health queries. Generated Java
types use `io.github.pnoker.api.center.data`; proto sources live under `src/main/protobuf/api/common/data/`.

## Services

| Service | RPCs | Purpose |
|---|---|---|
| `PointValueApi` | `GetLastValue`, `ListHistoryValues`, `ListSeriesVolumes` | query point values and value-volume series |
| `PointValueApi` | `ReadCommand`, `WriteCommand` | submit point read/write commands |
| `CommandHistoryApi` | `CallCommand`, `GetByRecordId`, `ListByPage` | dispatch and query command history |
| `EventHistoryApi` | `ReportEvent`, `GetByRecordId`, `ListByPage` | report and query event history |
| `StatusHealthApi` | `DeviceStatusesByIds`, `DeviceStatusesByProfileId` | query device status snapshots |
| `StatusHealthApi` | `DriverStatusesByIds`, `DriverDeviceStatusSummary` | query driver status snapshots and device summaries |
| `StatusHealthApi` | `SystemHealth` | query the platform health snapshot |

Single-result RPCs use `GetXxx`; collection/page results use `ListXxx` or an explicitly named status aggregation. Do
not reintroduce legacy `SelectXxx` names.

## Consumers and implementation

- `dc3-common-data` implements the data-side servers.
- `dc3-common-facade-grpc` wraps generated stubs in transport-independent facades.
- Manager and other business modules should call the facade API rather than construct channels directly.

All write/report requests must carry tenant context through the transport and into the business layer.

## Build and Verification

```bash
mvn -s .mvn/settings.xml -q -pl dc3-api/dc3-api-data -am compile
```

This contract module has no module-specific tests. Compile it after proto changes, then run the matching server and
facade tests in `dc3-common-data` and `dc3-common-facade-grpc`.
