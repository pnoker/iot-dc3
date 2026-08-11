# DC3 API Auth

`dc3-api-auth` defines the authentication and authorization gRPC contracts. Generated Java types use the
`io.github.pnoker.api.center.auth` package; proto sources live under `src/main/protobuf/api/common/auth/`.

## Services

| Service | RPCs | Purpose |
|---|---|---|
| `TenantApi` | `GetByCode` | resolve tenant metadata |
| `UserApi` | `GetById`, `GetByPrincipalId` | resolve user identity |
| `TokenApi` | `CheckValid` | validate login/token material |
| `LocalCredentialApi` | `GetByLoginName` | resolve local credentials |
| `PermissionApi` | `ListPermissionCodes` | resolve effective permission codes |
| `ResourceRegistryApi` | `Sync` | synchronize annotated API/menu resources |
| `McpRuntimeApi` | `Introspect`, `ListTools`, `ResolveTool`, `AuthorizeToolCall`, `Audit` | authorize and audit MCP tools |

Every response uses a contract-specific wrapper containing `GrpcR`. Callers must inspect the result envelope before
reading response data.

## Consumers and implementation

- `dc3-common-auth` implements the servers as Spring `@Service` beans extending generated `*ImplBase` classes.
- `dc3-common-facade-grpc` creates shared blocking stubs and exposes transport-independent auth facades.
- `dc3-gateway` uses auth facades for ingress authentication and authorization.

Business code should depend on facade interfaces instead of generated stubs unless it is itself a transport adapter.

## Build and Verification

Run from the repository root:

```bash
mvn -s .mvn/settings.xml -q -pl dc3-api/dc3-api-auth -am compile
```

This module has no handwritten runtime code or module-specific tests. A successful compile verifies proto syntax and
generated Java sources; server/facade behaviour is tested in the implementing modules.

When changing the contract, preserve field numbers, update implementations and clients together, and verify that
tenant and authorization context remain explicit.
