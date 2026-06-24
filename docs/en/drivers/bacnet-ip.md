---
title: BACnet/IP Driver
---

# BACnet/IP Driver

> **`dc3-driver-bacnet-ip` connects BACnet/IP devices to IoT DC3** — it targets the object properties of remote devices, periodically reading property values, and supports commands that write values to writable object properties.

BACnet (Building Automation and Control network) is an international standard protocol for building automation, widely used for HVAC units, fresh-air systems, lighting, elevators, chiller/boiler plants, thermostats, and other mechanical/electrical equipment. BACnet/IP is the variant that runs over UDP (standard port `47808 = 0xBAC0`). Built on the BACnet4J library, this driver joins the network as a local BACnet device (LocalDevice), discovers remote devices via broadcast, then reads and writes the properties of remote objects according to the object type, object instance number, and property identifier configured on each [Point](../introduction/concepts/point).

In BACnet, a physical device is identified by a unique **device instance number**, contains a number of **objects** (such as `ANALOG_INPUT`, `BINARY_OUTPUT`) — each object located by "object type + object instance number" — and each object exposes several **properties** (the most common being `PRESENT_VALUE`, the current value). Reading or writing a point is essentially locating "remote device → object → property".

- **Driver name / code**: `BACnet IP Driver` / `BacnetIpDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively discovers and accesses remote devices)

## Driver Configuration (device-level `driver-attribute`)

When onboarding a BACnet/IP device, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). Note that these configure how the **local driver** joins the network, not the remote device address — the remote device is located by the device instance number on the Points below:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Local Device ID | `localDeviceId` | INT | `1001` | Local BACnet device instance number |
| Bind Address | `bindAddress` | STRING | `0.0.0.0` | Local bind address |
| Port | `port` | INT | `47808` | BACnet UDP port (default 47808 = 0xBAC0) |
| Broadcast Address | `broadcastAddress` | STRING | `255.255.255.255` | Broadcast address for device discovery |
| Timeout | `timeout` | INT | `6000` | Request timeout in milliseconds |

## Point Configuration (`point-attribute`)

Fill these on each collected [Point](../introduction/concepts/point) to locate "which property of which object on which remote device":

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Remote Device ID | `remoteDeviceId` | INT | `0` | Remote BACnet device instance number |
| Object Type | `objectType` | STRING | `ANALOG_INPUT` | BACnet object type (ANALOG_INPUT, ANALOG_OUTPUT, ANALOG_VALUE, BINARY_INPUT, BINARY_OUTPUT, BINARY_VALUE, MULTI_STATE_INPUT, MULTI_STATE_OUTPUT, MULTI_STATE_VALUE, DEVICE) |
| Object Instance | `objectInstance` | INT | `0` | Object instance number |
| Property ID | `propertyId` | STRING | `PRESENT_VALUE` | Property identifier (PRESENT_VALUE, DESCRIPTION, STATUS_FLAGS, etc.) |

## Write Command Configuration (`command-attribute`)

Writable points also need these on the write command (same structure as the point configuration, but defaulting to a writable output object):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Remote Device ID | `remoteDeviceId` | INT | `0` | Remote BACnet device instance number |
| Object Type | `objectType` | STRING | `ANALOG_OUTPUT` | BACnet object type to write |
| Object Instance | `objectInstance` | INT | `0` | Object instance number |
| Property ID | `propertyId` | STRING | `PRESENT_VALUE` | Property identifier |

## Collection and Health

- **Collection interval**: default cron `0/30 * * * * ?` (one read round every 30 seconds).
- **Health / online status**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds` — the driver judges online status by whether the local and remote device connection is ready; see [Device](../introduction/concepts/device).

## Minimal Onboarding Example

Onboard the temperature object of a BACnet/IP device whose device instance number is `9001` on the network:

1. Create a [Device](../introduction/concepts/device) with `BACnet IP Driver`. The driver attributes can all keep their defaults (`localDeviceId=1001`, `bindAddress=0.0.0.0`, `port=47808`, `broadcastAddress=255.255.255.255`, `timeout=6000`); adjust `bindAddress` / `broadcastAddress` only when the host has multiple NICs or the broadcast cannot reach the target subnet.
2. Add a temperature [Point](../introduction/concepts/point) (`READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, with point attributes `remoteDeviceId=9001`, `objectType=ANALOG_INPUT`, `objectInstance=1`, `propertyId=PRESENT_VALUE`.
3. Start the driver, and within 30 seconds the object's `PRESENT_VALUE` appears in the [PointValue](../introduction/concepts/point-value).

## Pitfalls

::: warning Object Type / Property ID must use the exact uppercase enum name
`objectType` and `propertyId` are matched by their exact uppercase names (e.g. `ANALOG_INPUT`, `PRESENT_VALUE`). A wrong or misspelled value raises no error — the driver **silently falls back** to `ANALOG_INPUT` / `PRESENT_VALUE`, so you may read the value of a different object without noticing. `objectType` supports only the 10 types listed above; `propertyId` supports `PRESENT_VALUE`, `DESCRIPTION`, `STATUS_FLAGS`, `EVENT_STATE`, `RELIABILITY`, `UNITS`, `OUT_OF_SERVICE`.
:::

::: tip The write value's encoding is determined by the object type
The driver decides how to encode the written value by the object type prefix: `ANALOG_*` is written as a float; `BINARY_*` treats `true` / `1` / `active` (case-insensitive) as "active" and everything else as "inactive"; `MULTI_STATE_*` is written as an integer. So to toggle a `BINARY_OUTPUT`, send `1` or `true` rather than `ON`.
:::

::: warning The remote device must be discoverable via broadcast
The driver can only read and write after discovering the remote device on the network via broadcast (an unfound `remoteDeviceId` blocks until timeout). BACnet/IP uses UDP broadcast, which normally cannot cross layer-3 routing — make sure the driver and the target device are in the same broadcast domain. Across subnets, deploy a BBMD on the network and adjust `broadcastAddress` accordingly.
:::

## Further Reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes and Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `localDeviceId` / `objectType`
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — another common industrial field protocol
