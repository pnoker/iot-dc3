# DC3 Common Public

## Overview

`dc3-common-public` is the foundational common utilities module of the IoT DC3 platform. It provides the universal
response wrapper (`R<T>`), base entity classes, JWT key
management, HTTP client configuration, and shared utility functions used across all platform modules.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-public
- **Version**: 2026.5.5

## Key Components

### Response Wrapper

`R<T>` is the standard REST response envelope used by all controllers:

```java
// Success with data
return Mono.just(R.ok(entityVO));

// Success with message
return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));

// Failure with message
return Mono.just(R.fail(e.getMessage()));
```

Fields: `ok` (boolean), `code` (String), `message` (String), `data` (T)

### Common Entities

| Class                      | Purpose                                              |
|----------------------------|------------------------------------------------------|
| `Pages`                    | Pagination request params (`current`, `size`)        |
| `RequestHeader.UserHeader` | Tenant/user identity propagated from gateway headers |
| `Keys`                     | JWT signing key holder                               |
| `TreeNode`                 | Generic hierarchical data structure                  |

### Utilities

| Utility          | Purpose                                             |
|------------------|-----------------------------------------------------|
| `JsonUtil`       | Jackson JSON serialization/deserialization helpers  |
| `UserHeaderUtil` | Extracts `UserHeader` from reactive WebFlux context |
| `HostUtil`       | Resolves host/IP information                        |
| `ResponseUtil`   | Writes HTTP responses in WebFlux context            |

### HTTP Client

`OkHttpConfig` — Pre-configured `OkHttpClient` bean with timeout and retry settings.

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

Foundation for all `dc3-common-*`, `dc3-center-*`, and `dc3-driver-*` modules.

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

