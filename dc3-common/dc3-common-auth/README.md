# DC3 Common Auth

## Overview

`dc3-common-auth` is the shared authentication business module of the IoT DC3 platform. It contains all controllers,
service implementations, gRPC servers, mappers, and DAL classes that implement the authentication center's
functionality. It is wired directly into `dc3-center-auth`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-auth

## Key Components

| Layer        | Contents                                                                   |
|--------------|----------------------------------------------------------------------------|
| Controllers  | REST controllers for user, tenant, token, dictionary endpoints             |
| Services     | `TokenService`, `UserService`, `TenantService`, `DictionaryForAuthService` |
| gRPC Servers | Spring `@Service` beans extending generated `*ImplBase` server classes     |
| DAL          | MyBatis-Plus mappers and DAL managers for auth tables                      |
| Init         | `AuthInitRunner` for startup checks                                        |

## gRPC Services Exposed

| Service               | Purpose                                                      |
|-----------------------|--------------------------------------------------------------|
| `TokenApi`            | Validate login and token material for gateway authentication |
| `TenantApi`           | Resolve tenant metadata by code                              |
| `UserApi`             | Resolve users by ID or principal ID                          |
| `LocalCredentialApi`  | Resolve local credentials by login name                      |
| `PermissionApi`       | List effective permission codes                              |
| `ResourceRegistryApi` | Synchronize discovered API and menu resources                |
| `McpRuntimeApi`       | Introspect, authorize, resolve, and audit MCP tool calls     |

Distributed callers use the corresponding facade interfaces; they should not construct gRPC channels in business code.

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-auth -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-auth -am test
```

## Related Modules

- `dc3-center-auth` — Bootstraps this module as a Spring Boot service
- `dc3-api-auth` — gRPC contract implemented by this module
- `dc3-common-model` — Entity, BO, VO, DTO definitions
