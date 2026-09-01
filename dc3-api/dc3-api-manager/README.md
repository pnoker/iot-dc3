# DC3 API Manager

`dc3-api-manager` defines Manager Center gRPC contracts for metadata lookups used by distributed facades and other
services. Generated Java types use `io.github.pnoker.api.center.manager`.

## Services

| Service      | Single-result RPCs                | Collection/page RPCs                                                 |
|--------------|-----------------------------------|----------------------------------------------------------------------|
| `DriverApi`  | `GetByDriverId`, `GetByDeviceId`  | `List` (offset), `ListByDriverIds`             |
| `DeviceApi`  | `GetByDeviceId`, `GetActiveOwner` | `List` (offset), `ListByProfileId`, `ListByDriverId`, `ListByDeviceIds` |
| `PointApi`   | `GetById`                         | `List` (offset), `ListByIds`             |
| `ProfileApi` | `GetByProfileId`                  | `List` (offset), `ListByProfileIds`, `ListByDeviceId` |
| `CommandApi` | `GetById`                         | `List` (offset), `ListByIds`                  |
| `EventApi`   | `GetById`                         | `List` (offset), `ListByIds`                   |

Proto sources live under `src/main/protobuf/api/common/manager/`. Shared query and offset-page messages are defined in
`manager_query.proto` and `api/common/page.proto`.

## Usage boundary

Business services should inject facade interfaces such as `DriverFacade`, `DeviceFacade`, or `PointFacade` from
`dc3-common-facade-api`. Canonical `List` contracts return an `OffsetPage` directly and are
consumed through asynchronous gRPC stubs.

A transport adapter that needs the generated API calls the current cardinality-matching methods, for example:

```java
GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder()
        .setDeviceId(deviceId)
        .setTenantId(tenantId)
        .build();
GrpcDriverDTO response = driverApiBlockingStub.getByDeviceId(request);
```

Do not use removed `SelectXxx` RPC names or ad hoc channel construction in business code.

## Implementation

`dc3-common-manager` implements the services as Spring `@Service` beans extending generated `*ImplBase` classes.
`dc3-common-facade-grpc` consumes them through stubs created by `GrpcStubConfig`.

## Build and Verification

```bash
mvn -s .mvn/settings.xml -q -pl dc3-api/dc3-api-manager -am compile
```

After proto changes, run the Manager gRPC server tests and facade contract tests. Verify tenant propagation and update
the corresponding server, builder, facade, and caller in the same change.
