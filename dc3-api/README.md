# DC3 API

`dc3-api` owns the protobuf contracts shared between IoT DC3 processes. It contains generated-contract modules only;
business logic belongs in `dc3-common-*` implementations.

## Modules

| Module            | Contract surface                                                           | Primary consumers                  |
|-------------------|----------------------------------------------------------------------------|------------------------------------|
| `dc3-api-auth`    | tenant, user, token, permission, resource registry, MCP runtime            | gateway and center services        |
| `dc3-api-data`    | point values, command history, event history                               | manager, drivers, and data clients |
| `dc3-api-driver`  | driver registration and driver-scoped device/point metadata                | protocol drivers                   |
| `dc3-api-manager` | manager-scoped driver, device, profile, point, command, and event metadata | data and other centers             |

Proto sources live under each module's `src/main/protobuf/` directory. Generated Java sources are build artifacts and
must not be edited directly.

## Contract changes

1. Update the affected `.proto` files.
2. Treat contract changes as a coordinated hard cutover; compatibility shims are not supported.
3. Compile the affected module to regenerate Java sources.
4. Update server implementations, builders, and clients together.
5. Verify tenant propagation, standard gRPC status handling, and single/list cardinality naming.

```bash
mvn -s .mvn/settings.xml -q -f dc3-api/pom.xml compile
```

Use `GetXxx` for single results and `ListXxx` for collections, pages, or maps. Do not reintroduce legacy `SelectXxx`
RPC names.

## Documentation ownership

Each child README documents its current services and messages. The `.proto` files remain the authoritative source for
method names, request/response types, and field definitions.
