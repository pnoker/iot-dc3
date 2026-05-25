# DC3 Common Auth

## Overview

`dc3-common-auth` is the shared authentication business module of the IoT DC3 platform. It contains all controllers,
service implementations, gRPC servers, mappers, and DAL classes
that implement the authentication center's functionality. It is wired directly into `dc3-center-auth`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-auth
- **Version**: 2026.5.22

## Key Components

| Layer        | Contents                                                                   |
|--------------|----------------------------------------------------------------------------|
| Controllers  | REST controllers for user, tenant, token, dictionary endpoints             |
| Services     | `TokenService`, `UserService`, `TenantService`, `DictionaryForAuthService` |
| gRPC Servers | `TenantServer`, `UserServer`, `TokenServer` (`@GrpcService`)               |
| DAL          | MyBatis-Plus mappers and DAL managers for auth tables                      |
| Init         | `AuthInitRunner` for startup checks                                        |

## gRPC Services Exposed

| Service     | Consumed By                  |
|-------------|------------------------------|
| `TokenApi`  | Gateway (`Authentic` filter) |
| `TenantApi` | Gateway, other services      |
| `UserApi`   | Gateway, other services      |

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-center-auth` — Bootstraps this module as a Spring Boot service
- `dc3-api-auth` — gRPC contract implemented by this module
- `dc3-common-model` — Entity, BO, VO, DTO definitions

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

