# DC3 Common Exception

## Overview

`dc3-common-exception` defines the standard exception types and exception utility class for the IoT DC3 platform. All
services and modules use these exceptions to signal business
errors consistently.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-exception
- **Version**: 2026.5.22

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
| `AuthException`      | Authentication/authorization failure     |
| `DuplicateException` | Duplicate entity conflict                |

### Utilities

- **`ExceptionUtil`** — Helper methods for wrapping and rethrowing exceptions with context
- **`ExceptionConstant`** — Standard exception message strings (e.g., `UTILITY_CLASS`)

## Usage

```java
throw new NotFoundException("Driver not found for device: " + deviceId);
throw new AddException("Failed to add driver: " + entityBO.getServiceName());
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

Used by all `dc3-common-*` service modules and `dc3-center-*` services.

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

