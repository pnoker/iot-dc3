# DC3 Common Exception

## Overview

`dc3-common-exception` defines the standard exception types and exception utility class for the IoT DC3 platform. All
services and modules use these exceptions to signal business errors consistently.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-exception

## Key Components

### Exception Types

| Exception            | Usage                                    |
|----------------------|------------------------------------------|
| `ServiceException`   | General service-level business error     |
| `UpdateException`    | Entity update failure                    |
| `AddException`       | Entity add failure                       |
| `DeleteException`    | Entity delete failure                    |
| `NotFoundException`  | Entity or resource not found             |
| `JsonException`      | JSON serialization/deserialization error |
| `SecurityException`  | General security policy violation        |
| `UnAuthorizedException` | Missing or invalid authentication     |
| `AccessDeniedException` | Authenticated principal lacks access  |
| `DuplicateException` | Duplicate entity conflict                |

### Utilities

- **`ExceptionUtil`** — Builds shared service-unavailable messages
- **`ExceptionMessageFormatter`** — Internal `{}` placeholder and trailing-cause formatter used by business exceptions

Shared message constants such as `ExceptionConstant` belong to `dc3-common-constant`.

## Usage

```java
throw new NotFoundException("Driver not found for device: " + deviceId);
throw new AddException("Failed to add driver: " + entityBO.getServiceName());
```

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-exception -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-exception -am test
```

## Related Modules

Used by all `dc3-common-*` service modules and `dc3-center-*` services.
