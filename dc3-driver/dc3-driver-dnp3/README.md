# DC3 Driver DNP3

## Overview

`dc3-driver-dnp3` targets DNP3 (IEEE 1815) masters for utility automation. The protocol stack is provided by the
`io.stepfunc:dnp3` native FFI binding (Rust `dnp3` runtime with per-platform native libraries bundled in the jar).

## Status

**Implemented.** The module registers, validates its attributes, and implements the full DNP3 master read/write path:
one native `Runtime` + TCP `MasterChannel` + association per outstation, class 0/1/2/3 integrity polling through a
`ReadHandler` that caches point values by index, and `DIRECT_OPERATE` commands for binary and analog outputs.

> The `io.stepfunc:dnp3` stack is a native FFI binding (Rust `dnp3` runtime with per-platform native libraries bundled
> in the jar). The code compiles and the API surface is confirmed against the binding, but commissioning against a
> real outstation is required to verify native-library loading and on-wire behaviour in a target environment.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-dnp3
- **Driver Name**: DNP3 Driver

## Driver Attributes (Device-level)

| Attribute          | Code              | Type   | Default | Description                   |
|--------------------|-------------------|--------|---------|-------------------------------|
| Host               | host              | STRING |         | DNP3 outstation address       |
| Port               | port              | INT    | 20000   | DNP3 TCP service port         |
| Master Address     | masterAddress     | INT    | 1       | Master link-layer address     |
| Outstation Address | outstationAddress | INT    | 1       | Outstation link-layer address |

## Point Attributes

| Attribute   | Code       | Type   | Default      | Description                                                                                   |
|-------------|------------|--------|--------------|-----------------------------------------------------------------------------------------------|
| Point Index | pointIndex | INT    | 0            | DNP3 point index within the selected point type                                               |
| Point Type  | pointType  | STRING | BINARY_INPUT | BINARY_INPUT, ANALOG_INPUT, COUNTER, DOUBLE_BIT_BINARY_INPUT, BINARY_OUTPUT, or ANALOG_OUTPUT |

## Command Attributes (write)

| Attribute   | Code       | Type   | Default       | Description                                            |
|-------------|------------|--------|---------------|--------------------------------------------------------|
| Point Index | pointIndex | INT    | 0             | DNP3 point index within the selected output point type |
| Point Type  | pointType  | STRING | BINARY_OUTPUT | BINARY_OUTPUT or ANALOG_OUTPUT                         |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable DNP3 outstation over TCP (default port 20000) with matching master/outstation link-layer addresses. Native
library loading must be verified on the target platform before commissioning.

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-dnp3 -am package
java -jar dc3-driver/dc3-driver-dnp3/target/dc3-driver-dnp3.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-dnp3 -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
