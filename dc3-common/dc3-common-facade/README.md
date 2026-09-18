# DC3 Common Facade

The facade modules define and implement cross-service business boundaries without binding callers directly to gRPC or
in-process service classes.

## Modules

| Module                            | Role                                                                 |
|-----------------------------------|----------------------------------------------------------------------|
| `dc3-common-facade-api`           | transport-independent facade interfaces and request/result contracts |
| `dc3-common-facade-grpc`          | distributed implementations backed by shared gRPC stubs              |
| `dc3-common-facade-local-auth`    | in-process auth facade implementation                                |
| `dc3-common-facade-local-data`    | in-process data facade implementation                                |
| `dc3-common-facade-local-manager` | in-process manager facade implementation                             |
| `dc3-common-facade-local`         | convenience POM aggregating all local implementations                |

## Selection

- Distributed center applications normally use `dc3-common-facade-grpc`.
- Single-process applications use the required domain-specific local modules.
- Depend on `dc3-common-facade-local` only when the application intentionally needs every local facade.
- Business code imports interfaces from `dc3-common-facade-api`; transport selection belongs in assembly/configuration.

## Verification

```bash
mvn -s .mvn/settings.xml -q -f dc3-common/dc3-common-facade/pom.xml -DskipTests compile
mvn -s .mvn/settings.xml -f dc3-common/dc3-common-facade/pom.xml test
```
