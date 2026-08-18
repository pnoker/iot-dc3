# DC3 API Driver

`dc3-api-driver` defines the gRPC contract used by protocol drivers to register with the Manager Center and retrieve
driver-scoped device and point configuration. Generated Java types use `io.github.pnoker.api.common.driver`.

## Services

| Service | RPC | Response | Purpose |
|---|---|---|---|
| `DriverApi` | `DriverRegister` | `GrpcRDriverRegisterDTO` | register metadata and receive assigned configuration |
| `DriverApi` | `GetById` | `GrpcRDriverRegisterDTO` | reload registered driver metadata |
| `DriverApi` | `RenewLease` | stream of `GrpcRDriverLeaseDTO` | renew one runtime lease and stream its owned-device snapshot |
| `DeviceApi` | `ListByPage` | `GrpcRPageDeviceDTO` | page through driver-owned devices |
| `DeviceApi` | `GetById` | `GrpcRDeviceDTO` | get one device with attached attribute configuration |
| `PointApi` | `ListByPage` | `GrpcRPagePointDTO` | page through driver-visible points |
| `PointApi` | `GetById` | `GrpcRPointDTO` | get one point with attached configuration |

Proto sources live under `src/main/protobuf/api/common/driver/`. The `.proto` files are authoritative for fields and
wrapper shapes.

## Registration flow

1. `dc3-common-driver` builds `GrpcDriverRegisterDTO` from the driver's `application.yml` metadata.
2. The driver calls `DriverApi.DriverRegister` on the Manager Center.
3. The response supplies driver metadata, device IDs, and supported driver/point/command/event attributes.
4. Each runtime instance calls `DriverApi.RenewLease` to renew its bounded lease and receive the devices it currently
   owns. Command routing uses the active owner and assignment version to fence stale instances.
5. `DeviceClient` and `PointClient` load attached configuration with `GetById` or `ListByPage`.
6. The SDK initializes protocol scheduling, metadata subscriptions, commands, and value dispatch.

## Response handling example

```java
GrpcRDeviceDTO response = deviceApiBlockingStub.getById(deviceQuery);
if (!response.getResult().getOk()) {
    throw new IllegalStateException(response.getResult().getMessage());
}
GrpcRDeviceAttachDTO attachment = response.getData();
GrpcDeviceDTO device = attachment.getDevice();
```

`DeviceApi.GetById` returns `GrpcRDeviceDTO`; `GrpcRDeviceAttachDTO` is its `data` field, not the RPC's top-level return
type.

## Implementation

The Manager Center implements these services. Drivers are clients; they do not implement `DriverApi` themselves.
`dc3-common-driver` creates shared stubs through `DriverClientStubConfig` and exposes higher-level clients to driver
implementations.

## Build and Verification

```bash
mvn -s .mvn/settings.xml -q -pl dc3-api/dc3-api-driver -am compile
```

After a contract change, run the matching gRPC server/client tests in `dc3-common-manager` and `dc3-common-driver`.
Preserve field numbers and tenant context when evolving registration or query messages.
