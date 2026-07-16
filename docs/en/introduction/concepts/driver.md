---
title: Driver
---

<script setup>
import DriverRelationDiagram from '../../../.vitepress/theme/components/DriverRelationDiagram.vue'
import DriverLifecycleDiagram from '../../../.vitepress/theme/components/DriverLifecycleDiagram.vue'
</script>

# Driver

> **A Driver is a standalone protocol-adapter service instance (`dc3-driver-*`)**—it translates an industrial protocol (
> Modbus, OPC UA, MQTT…) into DC3's unified [Point](./point) read/write and [PointValue](./point-value) reporting. One
> protocol maps to one driver module, and on startup the driver registers itself plus the config items it can accept
> with
> the management center.

A Driver answers "how does DC3 actually talk to this [Device](./device)?". A device only describes "what is connected";
the thing that holds the protocol session, polls on a schedule, and translates register values into point values is the
driver—a **running service process**. In other words: a device is a row of metadata, a driver is a program that runs.

The easy confusion is "Driver" vs. "Device": one Modbus TCP driver instance (`dc3-driver-modbus-tcp`) can connect to
hundreds or thousands of Modbus devices at once; each device tells the driver "my IP, port, slave address" through
its [attribute config](./attribute-config). **A driver is a one-to-many protocol gateway; a device is an access point
hanging under it.**

## What it is / why it exists

Industrial field protocols are wildly diverse, and the DC3 core cannot bundle every protocol stack. So DC3 pushes "how
to speak the protocol" down into independent driver services, and the core agrees with drivers on only one unified point
read/write contract. Adding a protocol = writing a new `dc3-driver-*` service; the core and the Web app stay untouched.

Each driver does one crucial thing on startup: **self-registration**. It registers with the management center carrying
its identity (`DriverBO`) and "which config items I can accept" (a set of `DriverAttribute`). From this the management
center knows: what this driver is called, where it runs, which [Tenant](./tenant) it belongs to, and which fields to
fill in when configuring devices for it.

## Key fields

Driver `DriverBO` (the identity metadata a driver service registers with the management center):

| Field                   | Type             | Meaning                                                                               |
|-------------------------|------------------|---------------------------------------------------------------------------------------|
| `driverName`            | String           | Driver display name (e.g. `Modbus Tcp Driver`)                                        |
| `driverCode`            | String           | Driver code, the unique identifier defined in configuration                           |
| `serviceName`           | String           | Driver service name, used for registration and routing (e.g. `dc3-driver-modbus-tcp`) |
| `serviceHost`           | String           | Driver service host address                                                           |
| `driverTypeFlag`        | DriverTypeEnum   | Driver runtime type, see below                                                        |
| `driverExt`             | DriverExt        | Extended metadata (JSON)                                                              |
| `enableFlag`            | EnableFlagEnum   | Enable flag                                                                           |
| `tenantId`              | Long             | Owning [Tenant](./tenant)                                                             |
| `signature` / `version` | String / Integer | Data signature and version                                                            |

Driver config item `DriverAttributeBO` (declares which [attribute config](./attribute-config) fields the driver can
accept; reported together with the driver at registration):

| Field               | Type               | Meaning                                                               |
|---------------------|--------------------|-----------------------------------------------------------------------|
| `attributeName`     | String             | Config item name (e.g. `Host`, `Port`)                                |
| `attributeCode`     | String             | Config item identifier, matched against when a device supplies values |
| `attributeTypeFlag` | AttributeTypeEnum  | Config item data type (`string` / `int` / `long` / `float`…)          |
| `defaultValue`      | String             | Default value                                                         |
| `driverId`          | Long               | Owning driver                                                         |
| `attributeExt`      | DriverAttributeExt | Extended config (JSON)                                                |
| `enableFlag`        | EnableFlagEnum     | Enable flag                                                           |
| `tenantId`          | Long               | Owning tenant                                                         |

::: tip DriverAttribute is a "declaration of config items", not a "config value"
`DriverAttribute` describes "this driver needs you to fill in `Host`, `Port`"—it is a **template**; the `192.168.1.10`,
`502` a specific device actually fills in are [attribute config](./attribute-config) (`DriverAttributeConfig`). The
former is produced by driver registration, the latter by you when configuring a device.
:::

## Driver types

| Type `driverTypeFlag` | code            | Description                                                                           |
|-----------------------|-----------------|---------------------------------------------------------------------------------------|
| `DRIVER_CLIENT`       | `driver-client` | Client-mode protocol driver, actively connects to devices (e.g. Modbus TCP polling)   |
| `DRIVER_SERVER`       | `driver-server` | Server-mode protocol driver, waits for devices to connect (e.g. MQTT, listening-type) |
| `GATEWAY`             | `gateway`       | Gateway driver                                                                        |
| `CONNECT`             | `connect`       | Connection driver                                                                     |

## Relationship to other concepts

<DriverRelationDiagram lang="en" />

- A driver **registers its identity once** and can carry collection for **many** [Devices](./device).
- The `DriverAttribute` a driver registers is a template; each device fills values against it
  via [attribute config](./attribute-config).
- The driver collects the [Points](./point) defined by the [Profile](./profile), translates the results
  into [PointValues](./point-value), and reports them.

## Startup registration and online status

<DriverLifecycleDiagram lang="en" />

On startup, registration is triggered by `DriverInitRunner` (an `ApplicationRunner`): it builds a `RegisterBO` (carrying
`tenant`, `driver`=`DriverBO`, `driverAttributes`, etc.) and calls `DriverRegisterService.initial()` to report to the
management center, retrying with exponential backoff until it succeeds. After registration, a driver is not "online
forever the moment it registers"—its **online status is a lease**: the SDK periodically triggers `DriverHealth.health()`
to report a heartbeat, renewing a 45-second lease in `dc3_entity_state` (`entity_type_flag = 3` denotes a driver); when
the lease expires unrenewed it is judged `offline`. State values are `online` / `offline` / `maintain` / `fault`.

::: warning Online status does not live in the metadata table
`dc3_driver` stores the driver's **config metadata** (name, service name, tenant); changing it does not mean the driver
is running. Whether a driver is currently online is read from the runtime state table `dc3_entity_state`, maintained by
heartbeat lease renewal—after a process crash or network drop the lease naturally expires and flips to offline. To see "
which drivers exist" look at the former; to see "is the driver reachable now" look at the latter.
:::

## Example

You want to onboard a batch of Modbus TCP meters in a workshop:

1. Deploy and start a `dc3-driver-modbus-tcp` service instance; it registers
   `DriverBO{ serviceName: "dc3-driver-modbus-tcp", driverTypeFlag: DRIVER_CLIENT }` and declares config items
   `DriverAttribute{ attributeCode: "host", type: string }`, `{ attributeCode: "port", type: int }`.
2. In the Web app create a [Device](./device) attached to that driver, and fill in
   its [attribute config](./attribute-config) per the declaration: `host=192.168.1.10`, `port=502`.
3. Using these, the driver opens a Modbus session, periodically reads registers for the [Points](./point) defined in
   the [Profile](./profile), translates the raw values into [PointValues](./point-value), and reports them to the data
   center.
4. The driver reports a heartbeat every 15 seconds to renew its lease; one day the service process is killed, 45 seconds
   later the lease expires, the platform marks the driver `offline`, and its devices follow into the offline scan.

## Built-in drivers

DC3 ships with **28** ready-to-use protocol drivers, covering industrial field protocols (Modbus RTU/TCP, OPC UA/DA, PLC
S7, Melsec, BACnet/IP, IEC104, DLMS, SNMP, CAN…), IoT protocols (MQTT, CoAP, LwM2M, HTTP, ZigBee, BLE…), serial/network
pass-through (Serial, TCP/UDP), and database access (MySQL, PostgreSQL, Oracle, SQLServer). For the full list and each
driver's responsibility see the [Module Map](../../architecture/modules).

## Further reading

- [Device](./device) — the access point under a driver; one driver, many devices
- [DriverAttributeConfig](./attribute-config) — the connection values a device fills per the driver's declared
  DriverAttribute
- [Point](./point) — the target data points a driver collects
- [Core Concepts Overview](../concepts) — the object model and three-layer configuration at a glance
- [Module Map](../../architecture/modules) — the list of 28 built-in drivers and the service topology
- [Driver Authoring Guide](../../development/driver-authoring) — how to write your own `dc3-driver-*`
