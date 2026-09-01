# DC3 Common Web

## Overview

`dc3-common-web` is the shared WebFlux configuration module of the IoT DC3 platform. It provides global exception
handling, WebFlux request processing configuration, global web filters, and reactive response utilities for all
REST-based center services.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-web

## Key Components

| Component               | Purpose                                                                              |
|-------------------------|--------------------------------------------------------------------------------------|
| `WebFluxConfig`         | Global WebFlux configuration (codecs, CORS, message converters)                      |
| `WebFluxSecurityConfig` | Security chain, public-path rules, and facade-backed authorization                   |
| `RequestIdWebFilter`    | Adds and propagates request IDs for tracing                                          |
| `ExceptionConfig`       | `@ControllerAdvice` global exception handler mapping failures to RFC 9457 Problem Details |
| `BaseController`        | Reactive controller helpers plus user/tenant context resolution                      |
| `PrincipalHeaderUtil`   | Reads the signed principal headers injected by the gateway                           |
| `SpringDocConfig`       | Shared springdoc/OpenAPI group configuration                                         |
| `ResponseUtil`          | Utilities for writing non-controller `ServerHttpResponse` bodies in reactive context |

## Exception Handling

All exceptions thrown by controllers are caught by `ExceptionConfig` and mapped to an
`application/problem+json` response:

```json
{
  "type": "https://iot-dc3.github.io/problems/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "limit must be between 1 and 200",
  "instance": "/api/v3/manager/device/list"
}
```

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-web -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-web -am test
```

## Related Modules

- `dc3-common-auth`, `dc3-common-data`, `dc3-common-manager` — All include this for reactive web support
- `dc3-common-public` — Provides shared request, enum and Problem Details models
