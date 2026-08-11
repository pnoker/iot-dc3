# DC3 Driver LwM2M

## Overview

`dc3-driver-lwm2m` is the LwM2M (Lightweight M2M) protocol driver of the IoT DC3 platform. It is intended to run an
embedded Eclipse Leshan LwM2M server, accept device registrations, and read/write LwM2M resources by Object / Object
Instance / Resource ID.

> ⚠️ **Work in progress.** This driver is currently a skeleton — protocol-level I/O is not yet fully implemented. The
> class documentation of `Lwm2mDriverCustomServiceImpl` explicitly states it is a "work-in-progress skeleton". Treat it
> as a starting template, not a production-ready driver.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-lwm2m
- **Driver Name**: LwM2M Driver

## Driver Attributes (Device-level)

| Attribute     | Description                            |
|---------------|----------------------------------------|
| Endpoint      | LwM2M device endpoint name             |
| Server Host   | LwM2M server bind address              |
| Server Port   | CoAP port                              |
| Secure Port   | CoAPS/DTLS port                        |
| Security Mode | Security mode: NOSEC, PSK              |
| PSK Identity  | PSK identity (when securityMode=PSK)   |
| PSK Key       | PSK key in HEX (when securityMode=PSK) |

## Point Attributes

| Attribute          | Description                                |
|--------------------|--------------------------------------------|
| Object ID          | LwM2M Object ID (e.g. 3303=Temperature)    |
| Object Instance ID | LwM2M Object Instance ID                   |
| Resource ID        | LwM2M Resource ID (e.g. 5700=Sensor Value) |
| Observe            | Enable LwM2M Observe: true, false          |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and
local buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

LwM2M client devices that register with the embedded Leshan server. The server binds to the configured `Server Host` /
`Server Port` (CoAP, default `5683`) and `Secure Port` (CoAPS/DTLS, default `5684`).

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-lwm2m -am package
java -jar dc3-driver/dc3-driver-lwm2m/target/dc3-driver-lwm2m.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-lwm2m -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
