# DC3 Common Web

## Overview

`dc3-common-web` is the shared WebFlux configuration module of the IoT DC3 platform. It provides global exception
handling, WebFlux request processing configuration, global web
filters, and reactive response utilities for all REST-based center services.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-web
- **Version**: 2026.5.22

## Key Components

| Component         | Purpose                                                                              |
|-------------------|--------------------------------------------------------------------------------------|
| `WebFluxConfig`   | Global WebFlux configuration (codecs, CORS, message converters)                      |
| `ExceptionConfig` | `@ControllerAdvice` global exception handler mapping exceptions to `R<T>` responses  |
| `WebFilterConfig` | Registers global web filters (e.g., request logging, context enrichment)             |
| `ResponseUtil`    | Utilities for writing non-controller `ServerHttpResponse` bodies in reactive context |

## Exception Handling

All exceptions thrown by controllers are caught by `ExceptionConfig` and mapped to a structured `R<T>` response:

```json
{
  "ok": false,
  "code": "FAILURE",
  "message": "Resource not found"
}
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-auth`, `dc3-common-data`, `dc3-common-manager` — All include this for reactive web support
- `dc3-common-public` — Provides `R<T>` response wrapper

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

