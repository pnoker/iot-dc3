# DC3 Common Model

## Overview

`dc3-common-model` is the shared domain model module of the IoT DC3 platform. It defines all base classes, domain
entities (BO/VO/DTO/DO), validation group interfaces, and query
objects used across all services and drivers.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-model
- **Version**: 2026.5.22

## Key Components

### Base Classes

| Class    | Purpose                                                                                    |
|----------|--------------------------------------------------------------------------------------------|
| `BaseBO` | Base business object — `id`, audit fields (creatorId, createTime, operatorId, operateTime) |
| `BaseDO` | Base database object (MyBatis-Plus entity with `@TableId`)                                 |
| `BaseVO` | Base view object for REST request/response                                                 |

### Validation Groups

Used with `@Validated(...)` in controllers:

| Interface       | Usage                                                            |
|-----------------|------------------------------------------------------------------|
| `Add`           | Marks fields required only on creation (`@PostMapping("/add")`)  |
| `Update`        | Marks fields required only on update (`@PostMapping("/update")`) |
| `Select`        | Marks fields for query operations                                |
| `Read` / `Auth` | Specialized validation groups                                    |

### Common Entities

- `RequestHeader.UserHeader` — Carries tenant/user ID extracted from gateway-injected headers
- `Pages` — Pagination parameters (current page, page size)
- `TreeNode` — Generic tree structure for hierarchical data

## Usage Example

```java
// Validation groups in controller
@PostMapping("/add")
public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverVO entityVO) { ...}

@PostMapping("/update")
public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverVO entityVO) { ...}
```

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

Foundation for all `dc3-common-*`, `dc3-center-*`, and `dc3-driver-*` modules.

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

