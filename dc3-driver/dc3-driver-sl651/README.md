# DC3 Driver SL651

## Overview

`dc3-driver-sl651` is the SL651-2014 hydrological telemetry driver of the IoT DC3 platform. SL651 is a server-side
protocol: remote hydrological monitoring stations push telemetry reports to a central server. This driver starts an
SL651 TCP server (via the `iot-communication` library) on the configured listen port, receives station reports, extracts
the telemetry body elements, matches the station address against device code/name, and forwards the value at the
configured element index as a point value to the DC3 platform.

Because data arrives asynchronously and unsolicited from remote stations, this is a listener driver rather than a
polling one — the SDK `read` and `write` methods are intentionally not used (`read` returns `null` and `write`
returns `false` by design, as documented in the implementation). Scheduled reads are disabled (`schedule.read.enable:
false`); only the custom schedule and device health check are enabled.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-sl651
- **Driver Name**: SL651 Hydrological Telemetry Driver

## Service Ports

| Port | Code | Default | Description                                |
|------|------|---------|--------------------------------------------|
| 5001 | port | 5001    | TCP port for the SL651 server to listen on |

The listen port is exposed as a driver attribute (see below) and read at startup.

## Driver Attributes (Device-level)

| Attribute     | Code | Type   | Default | Description                            |
|---------------|------|--------|---------|----------------------------------------|
| Listen Port   | port | INT    | 5001    | TCP port for SL651 server to listen on |
| Auth Password | pwd  | STRING | 0000    | Remote station authentication password |

## Point Attributes

| Attribute     | Code  | Type | Default | Description                                           |
|---------------|-------|------|---------|-------------------------------------------------------|
| Element Index | index | INT  | 0       | Zero-based index into the telemetry body element list |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

SL651-2014 remote stations configured to report to this driver's listen port (default 5001). The `iot-communication`
library provides the SL651 server; the driver loads it reflectively at startup, so it logs a warning and remains idle if
the SL651 API is unavailable.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-sl651 -am package
java -jar dc3-driver/dc3-driver-sl651/target/dc3-driver-sl651.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-sl651 -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
