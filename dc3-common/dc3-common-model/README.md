# DC3 Common Model

## Overview

`dc3-common-model` is the shared domain model module of the IoT DC3 platform. It defines the base BO/VO/DTO classes,
MapStruct builder contracts, JSON extension models, validation groups, and shared transport DTOs used across services
and drivers. Shared domain/wire/persistence enums belong to `dc3-common-constant`; persistence DO classes remain in the
modules that own their database tables.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-model

## Key Components

### Base Classes

| Class         | Purpose                                                                 |
|---------------|-------------------------------------------------------------------------|
| `BaseBO`      | Base business object with ID, remark, and creator/operator audit fields |
| `BaseVO`      | Base web view object with ID, remark, and creator/operator audit fields |
| `BaseDTO`     | Base cross-process DTO with ID and audit timestamps                     |
| `BaseBuilder` | MapStruct conversion contract for `VO <-> BO <-> DTO`                   |
| `BaseExt`     | Base JSON extension model used by domain-specific `*Ext` classes        |

### Validation Groups

Used with `@Validated(...)` in controllers:

| Interface                     | Usage                                                            |
|-------------------------------|------------------------------------------------------------------|
| `Add`                         | Marks fields required only on creation (`@PostMapping("/add")`)  |
| `Update`                      | Marks fields required only on update (`@PostMapping("/update")`) |
| `Select`                      | Marks fields for query operations                                |
| `Read`                        | Read-path validation (e.g. `read` requests)                      |
| `Write`                       | Write-path validation (e.g. `write` requests)                    |
| `Auth`                        | Authentication-scoped validation                                 |
| `Check` / `Parent` / `Upload` | Specialized validation groups                                    |

### Shared DTOs and Extensions

- `MetadataEventDTO` — Metadata-change event transferred to driver listeners
- `PointCommandDTO` / `PointCommandResultDTO` — Point command request/result payloads
- `CommandCallDTO` / `CommandCallResultDTO` — Custom command request/result payloads
- Domain-specific `*Ext` classes — JSON extension-column shapes

`RequestHeader`, `PageRequest`, `OffsetPage`, `CursorPage`, `TreeNode`, and `TenantOwned` belong to `dc3-common-public` or
`dc3-db-r2dbc-core`, not this module. Shared
top-level enums belong to `dc3-common-constant` and are referenced by model fields where appropriate.

## Usage Example

```java
// Validation groups in controller
@PostMapping("/add")
public Mono<ResponseEntity<DriverVO>> add(@Validated(Add.class) @RequestBody DriverVO entityVO) { ...}

@PostMapping("/update")
public Mono<ResponseEntity<DriverVO>> update(@Validated(Update.class) @RequestBody DriverVO entityVO) { ...}
```

## Build Instructions

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-model -am package
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-common/dc3-common-model -am test
```

## Related Modules

Foundation for all `dc3-common-*`, `dc3-center-*`, and `dc3-driver-*` modules.
