---
title: DLMS/COSEM Driver
---

<script setup>
import DlmsDiagram from '../../.vitepress/theme/components/DlmsDiagram.vue'
</script>


# DLMS/COSEM Driver

`dc3-driver-dlms` connects DLMS/COSEM metering devices (electricity, water, gas, and heat meters) to IoT DC3: it targets
**OBIS codes**, acts as a DLMS client to the meter, and periodically reads COSEM object attribute values. By the end of
this page you will understand how DLMS/COSEM addresses data, how to fill the protocol attributes on a device and its
points correctly, and exactly where this driver's implementation currently stops.

> You are here: the "meter" onboarding side of field devices. To understand why metering protocols form their own world
> and where OBIS codes sit in the network layer, start with [Industrial Buses & Protocols](../foundations/fieldbus).

## Protocol Background

DLMS/COSEM (Device Language Message Specification / Companion Specification for Energy Metering) is the international
standard protocol for utility metering of electricity, water, gas, and heat, corresponding to the IEC 62056 / EN 13757-1
standard series and maintained by the DLMS User Association. It is the de facto common language between meter-reading
systems, energy-management platforms, and smart meters: a large share of smart meters, concentrators, and data terminals
in Europe and China speak it.

The biggest difference between DLMS/COSEM and protocols like Modbus or CIP is its **object-oriented addressing model**:

- **Modbus addresses by register** — you must know which holding register a value lives in (e.g. `40001`); the address
  is a raw number.
- **DLMS/COSEM addresses by object + OBIS code** — each readable quantity in the meter (active energy, voltage,
  clock, …) is modeled as a **COSEM object**, uniquely identified by a 6-field **OBIS code** (such as `1.0.1.8.0.255`);
  each object in turn has several numbered **attributes**, where attribute `2` is usually the "present value." Reading a
  quantity means "locate the object by OBIS code, fetch the value by attribute number."

This "object + code" model makes metering semantics highly standardized — `1.0.1.8.0.255` means "total active energy" in
any compliant electricity meter, portable across vendors; the price is that before onboarding you must look up the OBIS
code and object type of each quantity.

In the four-layer IoT architecture, DLMS/COSEM belongs to the metering side of the **network layer**: it solves "how a
meter sends metering data out with standard semantics," sitting above the sensing layer (metering chips/transducers) and
below the platform layer (IoT DC3 aggregation). The diagram below places OBIS code-based addressing within a single
collection:

<DlmsDiagram lang="en" />

The driver uses the **Gurux DLMS library** (`GXDLMSClient`) to build and decode DLMS frames, acting as a client over TCP
or serial to connect to the meter, reading the matching attribute value by the OBIS code configured on each point and
unifying it as a [PointValue](../introduction/concepts/point-value) sent upstream.

## Attribute Configuration

DLMS/COSEM onboarding parameters come in two layers: **driver attributes (driver-attribute)** describe "which meter,
which transport and authentication" and are filled on the [device](../introduction/concepts/device); **point
attributes (point-attribute)** describe "which object, which attribute" and are filled on
each [Point](../introduction/concepts/point). The defaults of all of these come from the driver's `application.yml`; for
the three-layer origin see [Attributes and Config](../introduction/concepts/attribute-config). DLMS/COSEM is read-only
metering semantics, so this driver provides no write commands and there are no command attributes (`command-attribute`
is empty).

### Driver Attributes (device-level `driver-attribute`)

When onboarding a meter, first state its transport and network/serial location on the device, then fill the
client/server addresses and authentication of the DLMS session. `transportType` decides TCP versus serial and selects
one of two mutually exclusive connection groups; `clientAddress` / `serverAddress` identify the two ends of the DLMS
session; `authentication` / `password` decide the privilege at which the association is established.

| Attribute      | code             | Type   | Default        | Description                                       |
|----------------|------------------|--------|----------------|---------------------------------------------------|
| Transport Type | `transportType`  | STRING | `TCP`          | Transport type (TCP, SERIAL)                      |
| Host           | `host`           | STRING | `localhost`    | Remote device address (TCP mode)                  |
| Port           | `port`           | INT    | `4059`         | Remote device port (TCP mode, DLMS standard 4059) |
| Serial Port    | `serialPort`     | STRING | `/dev/ttyUSB0` | Serial port path (SERIAL mode)                    |
| Baud Rate      | `baudRate`       | INT    | `9600`         | Baud rate (SERIAL mode)                           |
| Client Address | `clientAddress`  | INT    | `16`           | DLMS client address (public client=16)            |
| Server Address | `serverAddress`  | INT    | `1`            | DLMS server address                               |
| Authentication | `authentication` | STRING | `NONE`         | Authentication method (NONE, LOW, HIGH)           |
| Password       | `password`       | STRING | (empty)        | Authentication password (used with LOW/HIGH)      |

::: tip TCP or SERIAL, pick one
With `transportType=TCP`, only `host` / `port` are used; with `transportType=SERIAL`, only `serialPort` / `baudRate` are
used. The other group is ignored under the current transport and need not be removed. `clientAddress` /
`serverAddress` / `authentication` / `password` apply to both modes.
:::

::: tip `clientAddress=16` is the public client
`clientAddress=16` is the DLMS "public client," which most meters allow to read basic metering quantities with `NONE`
authentication. To read protected objects (such as load profiles or parameter configuration), switch to a
higher-privilege client address and raise `authentication` to `LOW` / `HIGH` with a `password`.
:::

::: info `validate()` checks only five required fields
The driver's `validate()` lists `transportType` / `host` / `port` / `clientAddress` / `serverAddress` as required;
`serialPort` / `baudRate` / `authentication` / `password` are not in the required check (fill as needed). Validation
only checks "whether a value is present," not whether the transport type and the filled parameters are consistent — see
Troubleshooting below.
:::

### Point Attributes (`point-attribute`)

Each collected point must state which COSEM object to read and which attribute of that object to fetch. The OBIS code
locates "which quantity to read," and the attribute number locates "which facet of that quantity to take."

| Attribute    | code          | Type   | Default    | Description                                            |
|--------------|---------------|--------|------------|--------------------------------------------------------|
| Object Type  | `objectType`  | STRING | `REGISTER` | DLMS object type (REGISTER, CLOCK, DATA, etc.)         |
| Logical Name | `logicalName` | STRING | (empty)    | Object logical name / OBIS code (e.g. `1.0.1.8.0.255`) |
| Attribute ID | `attributeId` | INT    | `2`        | Attribute ID (2=Present Value)                         |

::: tip The OBIS code locates "which quantity to read"
`logicalName` is a 6-part OBIS code uniquely identifying one metering quantity in the meter — for example
`1.0.1.8.0.255` is "total active energy" and `1.0.32.7.0.255` is "phase A voltage." `objectType` tells the driver which
COSEM interface class the object is (`REGISTER` metering register, `CLOCK` clock, `DATA` generic data, etc.), and
attributes mean different things across interface classes. `attributeId=2` reads the object's "present value" attribute.
The Point's own data type ([Point](../introduction/concepts/point) `pointTypeFlag`) must match the actual type of the
object attribute.
:::

::: info `validatePoint()` checks only `objectType`
Point validation lists only `objectType` as required; `logicalName` defaults to empty, but leaving it empty means no
object can be located. Be sure to fill the actual OBIS code on every collected point.
:::

### Collection and Health

- **Collection cycle**: default read cron `0/30 * * * * ?` (reads once every 30 seconds).
- **Custom schedule**: `schedule.custom` is enabled in the yml (cron `0/5 * * * * ?`), but the current `schedule()`
  method body is empty and performs no custom logic.
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds` —
  see [Device](../introduction/concepts/device) for the online-state mechanism.

## Troubleshooting

DLMS/COSEM onboarding failures mostly fall into two buckets: "transport not matched" and "object not located." Work
through them from the outside in.

::: warning Transport type and connection parameters do not match
`host` / `port` only take effect under `transportType=TCP`, and `serialPort` / `baudRate` only under `SERIAL`.
`validate()` does not catch a "transport vs parameters" mismatch — if you set `transportType` to `SERIAL` but only
filled in `host`, the driver follows the serial branch and looks for `serialPort`, failing to reach the meter. When
changing the transport type, remember to fill the corresponding group of attributes.
:::

::: warning TCP port or firewall: 4059 unreachable
The standard port for DLMS over TCP is `4059` — different from Modbus's `502` and IEC 104's `2404`, so do not carry
those over. First confirm `host:4059` is reachable from the driver host (`telnet <host> 4059` or `nc -vz <host> 4059`).
Common causes: the meter/concentrator does not expose the DLMS port, the network is not routed, or a firewall blocks

4059.

:::

::: warning Serial path or baud rate mismatch
In serial mode, `serialPort` (e.g. `/dev/ttyUSB0`) must be a device node that really exists on the driver host, and the
current user must have read/write permission (on Linux you often need to add the user to the `dialout` group).
`baudRate` must match the meter's setting (common metering rates are `300` / `9600` / `19200`); a mismatch means frames
cannot be parsed correctly.
:::

::: warning OBIS code or object type mismatch, quantity not located
`logicalName` must match an object that actually exists in the meter verbatim, and `objectType` must match that object's
real COSEM interface class. Configuring a `CLOCK` (clock) object as `REGISTER`, or entering an OBIS code that the meter
does not have, both fail the read. Before onboarding, check each quantity's code and object type against the vendor's
object list (OBIS table).
:::

::: warning Insufficient authentication, protected objects refused
The public client at `clientAddress=16` can usually read only basic metering quantities. When reading protected objects
such as load profiles, event logs, or parameters, still using the public client or `authentication=NONE` makes the meter
refuse the association or the read. Switch to a higher-privilege client address and raise `authentication` to `LOW` /
`HIGH` with the correct `password`.
:::

::: info Online state is not evidence that data was collected
`health()` only checks whether the driver has cached a `GXDLMSClient` object for the device — it does **no real
connectivity probe** — and in the current implementation the connection cache is never populated (the transport layer is
pending, see below). To judge whether data is really being collected, rely on whether
the [PointValue](../introduction/concepts/point-value) updates, not on the device online state.
:::

## How It Lands in IoT DC3

- **dc3.driver.code**: `DlmsDriver` (a stable routing identifier — registration and message routing both rely on it, do
  not change it casually). Driver name `DLMS/COSEM Driver`, type `DRIVER_CLIENT` (the driver actively connects to the
  meter).
- **Read**: metering semantics — it is meant to read COSEM attributes by OBIS code on the collection cycle — but the
  current `read()` throws `ReadPointException` directly; the collection trunk is not yet wired (see the skeleton note
  below).
- **Write**: not provided. DLMS/COSEM is read-only metering in this driver; `command-attribute` is empty and `write()`
  throws `WritePointException` directly.
- **Subscribe / push**: not supported. This driver actively polls on the collection cycle and does not listen for
  meter-initiated pushes.

Aligned with the [Driver Capability Matrix](./matrix): in the matrix DLMS is marked `—` for read/write/subscribe, with
the note "smart meter, transport pending."

::: warning Work in progress (skeleton)
This driver is a protocol skeleton, earlier-stage than drivers whose "upper flow is wired and only framing is missing":
the Gurux client (`GXDLMSClient`) can generate DLMS frames, but the **transport send/receive and HDLC handshake are not
yet implemented**, so neither the read nor the write trunk is wired:

- `read()` / `write()` throw "not implemented" exceptions (`ReadPointException` / `WritePointException`) to fail fast,
  so the SDK records the failure and applies connection backoff rather than returning a cached or fabricated value;
- `health()` only checks whether the internal connection cache `clientMap` contains the device, not a real protocol
  probe; and since that cache is never populated in the current implementation, devices will not actually show online;
- the `schedule()` method body is empty, so the `custom` scheduled task enabled in the yml runs no logic for now.

Treat it as a starting template for onboarding a new meter, not a production-ready driver. The attribute tables and
schedules below are taken verbatim from the real `application.yml` and are safe to fill in; but for the actual read
behavior, consult the `read()` / `write()` / `initial()` source in `DlmsDriverCustomServiceImpl`.
:::

The minimal path to onboard a DLMS/COSEM electricity meter (to validate the configuration flow, not for production
collection):

1. Create a [device](../introduction/concepts/device) with `DLMS/COSEM Driver`, and set the driver attributes
   `transportType=TCP`, `host=192.168.1.20`, `port=4059`, `clientAddress=16`, `serverAddress=1`, `authentication=NONE`.
2. Add an energy [Point](../introduction/concepts/point) (`pointTypeFlag=DOUBLE`, `READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes
   `objectType=REGISTER`, `logicalName=1.0.1.8.0.255`, `attributeId=2`.
3. Start the driver and watch the logs; the current `read()` throws `ReadPointException` directly, so the SDK records a
   read failure and backs off, and no value appears in [PointValue](../introduction/concepts/point-value) for now — a
   real value is only collected once the transport layer is completed.

For the complete onboarding procedure, see [Device Onboarding](../operation/device-onboarding).

## Further Reading

- [Drivers Overview](./index) — entry point for driver categories and selection
- [Driver Capability Matrix](./matrix) — read/write/subscribe capabilities and implementation status at a glance
- [Device Onboarding](../operation/device-onboarding) — a complete device onboarding flow
- [Industrial Buses & Protocols](../foundations/fieldbus) — the metering side of the network layer; OBIS object
  addressing and how it compares to other protocols
