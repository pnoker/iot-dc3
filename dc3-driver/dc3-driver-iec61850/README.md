# DC3 Driver IEC 61850

## Overview

`dc3-driver-iec61850` acts as an IEC 61850 MMS client using OpenMUC `openiec61850`. It maintains one MMS association
per IED device and reads/writes data attributes addressed by an object reference and functional constraint
(e.g. `S1MMXU1.TotW.actVal` / `MX`).

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-iec61850
- **Driver Name**: IEC 61850 Driver

## Driver Attributes (Device-level)

| Attribute | Code | Type   | Default | Description                    |
|-----------|------|--------|---------|--------------------------------|
| Host      | host | STRING |         | IEC 61850 server (IED) address |
| Port      | port | INT    | 102     | MMS service port               |

## Point Attributes

| Attribute             | Code                | Type   | Default | Description                                        |
|-----------------------|---------------------|--------|---------|----------------------------------------------------|
| Object Reference      | objectReference      | STRING |         | Data object reference, e.g. S1MMXU1.TotW.actVal    |
| Functional Constraint | functionalConstraint | STRING | MX      | Functional constraint, e.g. MX, ST, CO, SP, SE     |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-iec61850 -am package
java -jar dc3-driver/dc3-driver-iec61850/target/dc3-driver-iec61850.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-iec61850 -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
