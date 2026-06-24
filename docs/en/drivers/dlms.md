---
title: DLMS/COSEM Driver
---

# DLMS/COSEM Driver

> **`dc3-driver-dlms` brings DLMS/COSEM metering devices into IoT DC3**—targeting meters such as energy meters, it periodically reads COSEM object attributes addressed by OBIS codes.

DLMS/COSEM (Device Language Message Specification / Companion Specification for Energy Metering) is the international standard protocol (IEC 62056) for utility metering of electricity, water, gas, and heat. Each readable quantity in the device (e.g. active energy, voltage, clock) is modeled as a **COSEM object**, uniquely identified by an **OBIS code** (such as `1.0.1.8.0.255`), and each object has several numbered **attributes**, where attribute `2` is usually the "present value". This driver uses the Gurux DLMS library to build and decode DLMS frames, acting as a client over TCP or serial to connect to the meter and read attribute values by the OBIS code configured on each [Point](../introduction/concepts/point).

Use cases: connecting meter-reading systems to electricity/water/gas meters, or energy-management platforms collecting metering data.

- **Driver name / code**: `DLMS/COSEM Driver` / `DlmsDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the meter)

::: warning Skeleton implementation for now
This driver is currently a template skeleton: the Gurux client can generate DLMS frames, but the transport send/receive is not yet implemented—in the current implementation `read()` / `write()` throw directly (a read failure throws `ReadPointException`, a write failure throws `WritePointException`), so the SDK records the failure and applies connection backoff rather than returning a fabricated value or write success; `health()` only checks whether a client object is cached (no real connectivity probe). Treat it as a starting point for onboarding a new meter, not a production-ready driver. The attribute tables and schedules below are taken from the real config and are safe to fill in, but the actual read behavior is still to be completed.
:::

## Driver configuration (device-level `driver-attribute`)

When onboarding a DLMS/COSEM device, fill these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Transport Type | `transportType` | STRING | `TCP` | Transport type (TCP, SERIAL) |
| Host | `host` | STRING | `localhost` | Remote device address (TCP mode) |
| Port | `port` | INT | `4059` | Remote device port (TCP mode) |
| Serial Port | `serialPort` | STRING | `/dev/ttyUSB0` | Serial port path (SERIAL mode) |
| Baud Rate | `baudRate` | INT | `9600` | Baud rate (SERIAL mode) |
| Client Address | `clientAddress` | INT | `16` | DLMS client address (public client=16) |
| Server Address | `serverAddress` | INT | `1` | DLMS server address |
| Authentication | `authentication` | STRING | `NONE` | Authentication method (NONE, LOW, HIGH) |
| Password | `password` | STRING | `(empty)` | Authentication password |

::: tip TCP or SERIAL, pick one
With `transportType=TCP`, only `host` / `port` are used; with `transportType=SERIAL`, only `serialPort` / `baudRate` are used. The other group is ignored under the current transport and need not be removed. `clientAddress` / `serverAddress` / `authentication` / `password` apply to both modes.
:::

## Point configuration (`point-attribute`)

Fill these on each [Point](../introduction/concepts/point) to locate the COSEM object to read by OBIS code:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Object Type | `objectType` | STRING | `REGISTER` | DLMS object type (REGISTER, CLOCK, DATA, etc.) |
| Logical Name | `logicalName` | STRING | `(empty)` | Object logical name / OBIS code (e.g. `1.0.1.8.0.255`) |
| Attribute ID | `attributeId` | INT | `2` | Attribute ID (2=Present Value) |

::: tip The OBIS code locates "which quantity to read"
`logicalName` is a 6-part OBIS code uniquely identifying one metering quantity in the meter—for example `1.0.1.8.0.255` is "total active energy". `attributeId=2` reads the object's "present value" attribute. The Point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) must match the actual type of the object attribute.
:::

DLMS/COSEM is read-only metering semantics, and this driver provides no write commands—`command-attribute` is empty in `application.yml`, so there is no write-command configuration table.

## Acquisition and health

- **Acquisition cycle**: default read cron `0/30 * * * * ?` (one read round every 30 seconds); plus a custom scheduled task cron `0/5 * * * * ?` (every 5 seconds)—the current `schedule()` is a no-op, so this task runs no logic for now.
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`—see [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

Onboard one electricity meter at IP `192.168.1.20:4059` and read total active energy:

1. Create a [Device](../introduction/concepts/device) with `DLMS/COSEM Driver`, filling driver attributes `transportType=TCP`, `host=192.168.1.20`, `port=4059`, `clientAddress=16`, `serverAddress=1`, `authentication=NONE`.
2. Add an energy [Point](../introduction/concepts/point) to the [Profile](../introduction/concepts/profile) bound to the device (`pointTypeFlag=DOUBLE`, `READ_ONLY`), filling point attributes `objectType=REGISTER`, `logicalName=1.0.1.8.0.255`, `attributeId=2`.
3. Start the driver; a read round triggers within 30 seconds—but the current `read()` throws `ReadPointException` directly, so the SDK records a read failure and backs off, and no value appears in [PointValue](../introduction/concepts/point-value) for now; a real value is only collected once the transport layer is completed.

## Common pitfalls

::: warning Host/Port must match the transport type
`host` / `port` only take effect under `transportType=TCP`. If you set `transportType` to `SERIAL` but only filled in `host`, the driver follows the serial branch and looks for `serialPort`, failing to reach the meter. When changing the transport type, remember to fill in the corresponding group of attributes.
:::

::: tip clientAddress defaults to the public client
`clientAddress=16` is the DLMS "public client", which most meters allow to read basic metering quantities without authentication. To read protected objects, switch to a higher-privilege client address and raise `authentication` to `LOW` / `HIGH` with a `password`.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes and config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `logicalName`
- [Device onboarding](../operation/device-onboarding) — a complete onboarding flow
- [Modbus TCP Driver](./modbus-tcp) — another TCP industrial-protocol driver
