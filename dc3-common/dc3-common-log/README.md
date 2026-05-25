# DC3 Common Log

## Overview

`dc3-common-log` is the shared logging aspect module of the IoT DC3 platform. It provides a custom AOP-based `@Logs`
annotation for declarative method-level logging across all
services.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-log
- **Version**: 2026.5.22

## Key Components

| Component    | Purpose                                                                       |
|--------------|-------------------------------------------------------------------------------|
| `@Logs`      | Method annotation to enable structured logging for controller/service methods |
| `LogsType`   | Enum defining log types (e.g., `SysLog`, `OpLog`)                             |
| `LogsAspect` | Spring AOP aspect that intercepts annotated methods and records log entries   |

## Usage

```java
@Logs(title = "Add Driver", type = LogsType.OpLog)
@PostMapping("/add")
public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverVO entityVO) {
    // method body
}
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

Used by controller layers in `dc3-common-auth`, `dc3-common-data`, `dc3-common-manager`.

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

