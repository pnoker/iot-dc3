# DC3 Common DAL

## Overview

`dc3-common-dal` is the shared Data Access Layer module of the IoT DC3 platform. It provides common VO/BO entities, DAL
manager implementations, and shared data structures used
across multiple service modules.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-dal
- **Version**: 2026.5.22

## Key Components

- **VO Entities** — View objects for REST response payloads (e.g., `LabelVO`, `GroupVO`, `DictionaryVO`)
- **DAL Config** — `DalConfig` shared MyBatis-Plus data access configuration
- **Common Managers** — Shared DAL manager interfaces and implementations for label, group, and label-bind operations

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-common-postgres` — PostgreSQL and MyBatis-Plus base configuration
- `dc3-common-model` — Base BO/VO/DTO model definitions
- `dc3-common-manager` / `dc3-common-auth` / `dc3-common-data` — Consumers of this DAL layer

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

