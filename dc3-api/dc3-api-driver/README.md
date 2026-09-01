# DC3 API Driver

`dc3-api-driver` defines the gRPC contract used by protocol drivers to register with the Manager Center and retrieve
driver-scoped device and point configuration. Generated Java types use `io.github.pnoker.api.common.driver`.

## Services

| Service     | RPC              | Response                        | Purpose                                                      |
|-------------|------------------|---------------------------------|--------------------------------------------------------------|
| `DriverApi` | `DriverRegister` | `GrpcDriverRegistrationDTO`     | register metadata and receive assigned configuration         |
| `DriverApi` | `GetById`        | `GrpcDriverRegistrationDTO`     | reload registered driver metadata                            |
| `DriverApi` | `RenewLease`     | stream of `GrpcDriverLeaseDTO`  | renew one runtime lease and stream its owned-device snapshot |
| `DeviceApi` | `List`           | `GrpcOffsetPageDeviceDTO`       | offset page through driver-owned devices                     |
| `DeviceApi` | `GetById`        | `GrpcDeviceAttachDTO`           | get one device with attached attribute configuration         |
| `PointApi`  | `List`           | `GrpcOffsetPagePointDTO`        | offset page through driver-visible points                    |
| `PointApi`  | `GetById`        | `GrpcPointDTO`                  | get one point with attached configuration                    |

Proto sources live under `src/main/protobuf/api/common/driver/`. The `.proto` files are authoritative for fields and
wrapper shapes.

## Registration flow

1. `dc3-common-driver` builds `GrpcDriverRegisterDTO` from the driver's `application.yml` metadata.
2. The driver calls `DriverApi.DriverRegister` on the Manager Center.
3. The response supplies driver metadata, device IDs, and supported driver/point/command/event attributes.
4. Each runtime instance calls `DriverApi.RenewLease` to renew its bounded lease and receive the devices it currently
   owns. Command routing uses the active owner and assignment version to fence stale instances.
5. `DeviceClient` and `PointClient` load attached configuration with `GetById` or `List`.
6. The SDK initializes protocol scheduling, metadata subscriptions, commands, and value dispatch.

## Response handling example

```java
GrpcDeviceAttachDTO attachment = deviceApiBlockingStub.getById(deviceQuery);
GrpcDeviceDTO device = attachment.getDevice();
```

Errors are returned through standard gRPC status codes; a successful response is the typed attachment payload.

## Implementation

The Manager Center implements these services. Drivers are clients; they do not implement `DriverApi` themselves.
`dc3-common-driver` creates shared stubs through `DriverClientStubConfig` and exposes higher-level clients to driver
implementations.

## Build and Verification

```bash
mvn -s .mvn/settings.xml -q -pl dc3-api/dc3-api-driver -am compile
```

After a contract change, run the matching gRPC server/client tests in `dc3-common-manager` and `dc3-common-driver`.
Keep tenant context explicit when evolving registration or query messages.
