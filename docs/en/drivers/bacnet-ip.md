---
title: BACnet/IP Driver
---

<script setup>
import BacnetIpDiagram from '../../.vitepress/theme/components/BacnetIpDiagram.vue'
</script>


# BACnet/IP Driver

`dc3-driver-bacnet-ip` connects BACnet/IP devices to IoT DC3. It joins the network as a local BACnet device, discovers
remote devices via broadcast, periodically reads object property values, and supports writing values to writable object
properties. After reading this you can configure local networking parameters on
a [Device](../introduction/concepts/device), locate a remote data point on a [Point](../introduction/concepts/point)
by "object type + instance number + property", and diagnose the common "can't discover the device / reading the wrong
object / can't write" problems.

> You are here: a concrete driver on the building-automation side of the network layer. For BACnet's object-property
> addressing model and its place in the four-layer architecture,
> see [Industrial Buses & Protocols](../foundations/fieldbus).

## Protocol Background

BACnet (Building Automation and Control network) is an international standard protocol for building automation (ASHRAE
135 / ISO 16484-5), in wide use since 1995 for HVAC units, fresh-air systems, lighting, elevators, chiller/boiler
plants, thermostats, and other mechanical/electrical equipment. Its goal is interoperability across building equipment
from different vendors, so it abstracts "device capability" into a unified object model rather than binding to specific
hardware.

**BACnet/IP** is the variant of BACnet that runs over Ethernet: it wraps BACnet application-layer messages in UDP,
listening on port **47808** by default (i.e. `0xBAC0`). Compared with the earlier MS/TP serial or ISO 8802-3 Ethernet
forms, BACnet/IP reuses the existing IP network directly and discovers devices within a subnet via UDP broadcast — which
also imposes one hard constraint: broadcast does not cross layer-3 routing by default (see Troubleshooting below).

In the [four-layer IoT architecture](../foundations/fieldbus), BACnet/IP belongs to the building-automation side of the
**network layer**: it defines how mechanical/electrical equipment is addressed and read/written on the network, carrying
the temperatures, states, and metering values gathered by the sensing layer up to the platform. Its communication model
is **master/slave, request-response** — this driver acts as the initiator (client), actively discovering and accessing
remote devices and polling on a cron cycle; remote devices do not report on their own.

BACnet addressing is a three-level structure, and understanding it is understanding how to configure a Point:

<BacnetIpDiagram lang="en" />

A physical device is identified by a unique **device instance number**; it contains a number of **objects** (such as
`ANALOG_INPUT`, `BINARY_OUTPUT`), each located by "object type + object instance number"; and each object exposes
several **properties**, the most common being `PRESENT_VALUE` (the current value). Reading or writing a point is
essentially locating the path "remote device → object → property".

- **Driver name / code**: `BACnet IP Driver` / `BacnetIpDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively discovers and accesses remote devices)

## Attribute Configuration

Onboarding a BACnet/IP device requires filling in [attributes](../introduction/concepts/attribute-config) at three
levels: device-level local networking parameters (`driver-attribute`), the addressing parameters of each collected
point (`point-attribute`), and the write-command parameters of each writable point (`command-attribute`). The
attributes, types, and defaults below all come from the driver's `application.yml` (the `dc3-driver-bacnet-ip` module).

### Driver Attributes (device-level `driver-attribute`)

Driver attributes answer "what identity the driver uses, which NIC it binds, and how it broadcasts" — note that these
configure how the **local driver** joins the network, **not** the remote device address; the remote device is located by
`remoteDeviceId` on the Points below. Fill in one set per device on the [Device](../introduction/concepts/device):

| Attribute         | code               | Type   | Default           | Description                              |
|-------------------|--------------------|--------|-------------------|------------------------------------------|
| Local Device ID   | `localDeviceId`    | INT    | `1001`            | Local BACnet device instance number      |
| Bind Address      | `bindAddress`      | STRING | `0.0.0.0`         | Local bind address                       |
| Port              | `port`             | INT    | `47808`           | BACnet UDP port (default 47808 = 0xBAC0) |
| Broadcast Address | `broadcastAddress` | STRING | `255.255.255.255` | Broadcast address for device discovery   |
| Timeout           | `timeout`          | INT    | `6000`            | Request timeout in milliseconds          |

`localDeviceId` is the instance number the driver occupies when it joins the network as a `LocalDevice`; it must not
collide with the instance number of any existing BACnet device on the network. `bindAddress` defaults to `0.0.0.0`,
letting the system pick the NIC; specify a concrete NIC IP only when the host has multiple NICs or the broadcast cannot
reach the target subnet. `broadcastAddress` is the address used to broadcast when discovering remote devices; the
default `255.255.255.255` is a limited broadcast, and for cross-subnet onboarding you should change it to the directed
broadcast address of the target subnet. The driver caches connections by device ID (one `LocalDevice` per device), and
the underlying transport timeout takes `timeout`. Config validation (`validate`) requires `localDeviceId`,
`bindAddress`, and `port` to be non-empty.

### Point Attributes (`point-attribute`)

Point attributes answer "which property of which object on which remote device to read". Fill in one set per
collected [Point](../introduction/concepts/point):

| Attribute        | code             | Type   | Default         | Description                          |
|------------------|------------------|--------|-----------------|--------------------------------------|
| Remote Device ID | `remoteDeviceId` | INT    | `0`             | Remote BACnet device instance number |
| Object Type      | `objectType`     | STRING | `ANALOG_INPUT`  | BACnet object type (see enum below)  |
| Object Instance  | `objectInstance` | INT    | `0`             | Object instance number               |
| Property ID      | `propertyId`     | STRING | `PRESENT_VALUE` | Property identifier                  |

`remoteDeviceId` is the instance number of the remote device to read; the driver first discovers the device by broadcast
using it, then locates the object by `objectType` + `objectInstance` and fetches the property by `propertyId`. Both
`objectType` and `propertyId` are matched by their **exact uppercase enum name**, drawn from a fixed mapping table in
the code (see the pitfall below and "How it lands in IoT DC3"). Config validation (`validatePoint`) requires all four to
be non-empty.

### Write Command Attributes (`command-attribute`)

Writable points also need one set on the write command, with the same structure as the point attributes but defaulting
to a writable output object:

| Attribute        | code             | Type   | Default         | Description                          |
|------------------|------------------|--------|-----------------|--------------------------------------|
| Remote Device ID | `remoteDeviceId` | INT    | `0`             | Remote BACnet device instance number |
| Object Type      | `objectType`     | STRING | `ANALOG_OUTPUT` | BACnet object type to write          |
| Object Instance  | `objectInstance` | INT    | `0`             | Object instance number               |
| Property ID      | `propertyId`     | STRING | `PRESENT_VALUE` | Property identifier                  |

The written value is auto-encoded by the target object type (see the "Write value encoding" tip below).

::: warning Object Type / Property ID must use the exact uppercase enum name, or it silently falls back
`objectType` and `propertyId` are matched by their exact uppercase names. A wrong or misspelled value **raises no error
** — the driver's `resolveObjectType()` / `resolvePropertyIdentifier()` **silently falls back** to `ANALOG_INPUT` /
`PRESENT_VALUE`, so you may read the value of a different object without noticing.

- `objectType` is supported in code for **10** types: `ANALOG_INPUT`, `ANALOG_OUTPUT`, `ANALOG_VALUE`, `BINARY_INPUT`,
  `BINARY_OUTPUT`, `BINARY_VALUE`, `MULTI_STATE_INPUT`, `MULTI_STATE_OUTPUT`, `MULTI_STATE_VALUE`, `DEVICE` (the yml
  remark lists only the first 9; `DEVICE` also works — defer to the code).
- `propertyId` supports 7: `PRESENT_VALUE`, `DESCRIPTION`, `STATUS_FLAGS`, `EVENT_STATE`, `RELIABILITY`, `UNITS`,
  `OUT_OF_SERVICE`.
  :::

## Troubleshooting

BACnet/IP onboarding failures cluster into three kinds: broadcast discovery, object addressing, and write-value
encoding. Work through them in order:

1. **Device stays offline / connection creation fails**. When the driver joins the network with `localDeviceId`, if that
   instance number collides with an existing device, or binding the port/NIC fails, `LocalDevice.initialize()` throws
   `ConnectorException` and the device stays offline. First confirm `localDeviceId` is unique on the network, port
   `47808` is not occupied by another BACnet program on the same host, and `bindAddress` points to a real, usable NIC.

2. **Connected but reads keep timing out (stuck until timeout)**. The driver relies on
   `getRemoteDeviceBlocking(remoteDeviceId)` to block while broadcasting for the remote device — if `remoteDeviceId` is
   not found on the network it **blocks until timeout** and then throws `ReadPointException`. First check whether
   `remoteDeviceId` is exactly the target device's instance number; then confirm the driver and the target device are in
   the same broadcast domain (see the pitfall below).

3. **Read value is wrong / looks like a different point**. Most likely `objectType` or `propertyId` is misspelled and
   triggered the silent fallback (back to `ANALOG_INPUT` / `PRESENT_VALUE`). Check the uppercase enum name character by
   character and confirm it is in the supported list above.

4. **Write command fails**. First confirm the target object type is writable (`*_INPUT` objects are usually physically
   read-only and cannot be written), then confirm the input value matches the encoding rules (see "Write value encoding"
   below). A failed write throws `WritePointException`.

5. **Can't reach devices across subnets / on a multi-NIC host**. BACnet/IP uses UDP broadcast, which does not cross
   layer-3 routing by default. See the pitfall container below.

6. **Online status flapping**. The health check defaults to once every 15 seconds (cron `0/15 * * * * ?`) with a
   45-second lease timeout; the driver judges online status by `LocalDevice.isInitialized()`. Frequent online/offline
   flapping usually means packet loss or unstable local-device initialization —
   see [Device](../introduction/concepts/device) for the online-status mechanism.

::: tip Write value encoding is determined by the object type
The driver's `createEncodable()` decides how to encode the written value by the object type prefix: `ANALOG_*` is
written as a float (`Real`); `BINARY_*` treats `true` / `1` / `active` (case-insensitive) as "active" and everything
else as "inactive"; `MULTI_STATE_*` and `DEVICE` are written as an integer (`UnsignedInteger`); a non-numeric value that
lands on the analog branch degrades to a string (`CharacterString`). So to toggle a `BINARY_OUTPUT`, send `1` or `true`
rather than `ON`.
:::

::: warning The remote device must be discoverable via broadcast
The driver can only read and write after discovering the remote device on the network via broadcast. BACnet/IP uses UDP
broadcast, which normally cannot cross layer-3 routing — make sure the driver and the target device are in the same
broadcast domain. Across subnets, deploy a **BBMD** (BACnet/IP Broadcast Management Device) on the network and change
`broadcastAddress` to the directed broadcast address of the target subnet. An unfound `remoteDeviceId` blocks until
timeout (see Troubleshooting item 2 above).
:::

## How it lands in IoT DC3

- **`dc3.driver.code`**: `BacnetIpDriver` (type `DRIVER_CLIENT`, actively discovers and accesses remote devices). This
  is a stable routing identifier — do not change it casually.
- **Read**: ✓ implemented. Discovers the device by broadcast using `remoteDeviceId`, reads the object property by
  `objectType` + `objectInstance` + `propertyId`, and returns the result as a string.
- **Write**: ✓ implemented. Auto-encodes the written value by object type (see the "Write value encoding" tip above).
- **Subscribe/report**: — not supported. BACnet is a master/slave polling model; this driver only actively reads/writes
  and never passively receives pushes (COV subscription is not implemented). This matches the "✓ / ✓ / —" for BACnet/IP
  in the [driver capability matrix](./matrix).
- **Collection cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds), configured under
  `schedule.read` in the driver's `application.yml`.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds`, judged
  by `LocalDevice.isInitialized()`.

::: info Implementation status: available
This driver is a **complete implementation** (not a skeleton), built on BACnet4J. `read()` / `write()` issue real BACnet
read/write requests, `health()` judges online status by the local device's initialization state, `validate()` /
`validatePoint()` perform required-field checks, and it caches the `LocalDevice` connection by device ID. Note two
behaviors that differ from intuition: (1) a misspelled `objectType` / `propertyId` **silently falls back** rather than
erroring; (2) an unfound `remoteDeviceId` **blocks until timeout** — both covered above.
:::

::: info schedule.custom is enabled but is a no-op
The driver's `application.yml` declares `schedule.custom` (cron `0/5 * * * * ?`, `enable: true`), but the `schedule()`
method is an empty implementation (no custom periodic logic). That is, this custom schedule currently produces no
collection behavior; actual collection is driven only by `schedule.read` — defer to the code.
:::

### Minimal Onboarding Example

Onboard the temperature object of a BACnet/IP device whose device instance number is `9001` on the network:

1. Create a [Device](../introduction/concepts/device) using `BACnet IP Driver`. The driver attributes can all keep their
   defaults (`localDeviceId=1001`, `bindAddress=0.0.0.0`, `port=47808`, `broadcastAddress=255.255.255.255`,
   `timeout=6000`); adjust `bindAddress` / `broadcastAddress` only when the host has multiple NICs or the broadcast
   cannot reach the target subnet.
2. Add a temperature [Point](../introduction/concepts/point) (`READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, with point attributes `remoteDeviceId=9001`,
   `objectType=ANALOG_INPUT`, `objectInstance=1`, `propertyId=PRESENT_VALUE`.
3. Start the driver, and within 30 seconds the object's `PRESENT_VALUE` appears in
   the [PointValue](../introduction/concepts/point-value).
4. If the Point must be writable, configure a write [Command](../introduction/concepts/command) for it, set `objectType`
   explicitly to a writable output object (e.g. `ANALOG_OUTPUT`), and send the value per the object-type rules (a number
   for analog, `1`/`true` for binary).

::: tip One driver instance can serve multiple devices
A single BACnet/IP driver process can serve multiple devices, each holding its own cached `LocalDevice` by device ID,
with the target remote device distinguished by `remoteDeviceId` on the Point.
:::

## Further Reading

- [Driver overview](./index) — entry point and taxonomy for all drivers
- [Driver capability matrix](./matrix) — read/write/subscribe at a glance, including the BACnet/IP row
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Industrial Buses & Protocols](../foundations/fieldbus) — BACnet's object-property addressing model and its place on
  the building-automation side
- [SNMP Driver](./snmp) — another active read/write network-management protocol
