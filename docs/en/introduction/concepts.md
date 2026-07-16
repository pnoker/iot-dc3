---
title: Core Concepts and Mental Model
---

<script setup>
import ConceptsDomainDiagram from '../../.vitepress/theme/components/ConceptsDomainDiagram.vue'
import ConceptsFlowDiagram from '../../.vitepress/theme/components/ConceptsFlowDiagram.vue'
</script>


# Core Concepts and Mental Model

To use IoT DC3 well, you need a simple object model in your head. This page gives you that: a one-sentence summary, an
entity-relationship diagram, a walk through each object, the often-confused "three layers of configuration," and the
tenant boundary that runs through everything. After this, the terminology in the rest of the docs will make sense.

> You are here: you've read the [platform positioning](./), and want the concepts straight before getting hands-on.
> Next, see [choose a path by role](./paths) or jump to [Quick Start](../quickstart/).

## One-Sentence Mental Model

> **Drivers connect devices, profiles describe capabilities, devices bind profiles, points carry data; the data center
stores values and dispatches commands.**

Breaking that down: a protocol **Driver** talks to devices. A **Profile** describes what a class of similar devices can
do (which points, commands, and events they have). A **Device** is a concrete instance bound to a profile and a driver.
A **Point** is a data item to collect or write. The collected value is a **PointValue**.

## Objects and Relationships

These relationships are fixed: a profile contains multiple points, commands, and events. A device **binds to exactly one
** profile (since Phase-1, `Device.profileId` is a single foreign key, no longer many-to-many) and one driver. A point
produces many point values.

<ConceptsDomainDiagram lang="en" />

## Object by Object

- **Driver (`dc3-driver-*`)**: a protocol-adapter service that talks to devices or data sources. On startup it registers
  itself, plus the configuration items (attributes) it accepts, with the management center. The platform ships 28
  built-in drivers covering Modbus, OPC UA, S7, MQTT, and more — see the [module map](../architecture/modules).
- **Profile**: a capability template for similar devices. It records which points this kind of device has, which custom
  commands it supports, and which events it reports, so devices can reuse it.
- **Device**: the platform's mirror of a real physical device in the field. It binds to one profile (which sets its
  points) and one driver (which sets how it communicates).
- **Point**: a data item. Its key fields are `pointTypeFlag` (data type) and `rwFlag` (read/write direction).

::: tip Read/write is decided by the Point itself
Whether a point can be written depends on its `rwFlag` (`READ_ONLY` / `WRITE_ONLY` / `READ_WRITE`), **not** on the
command table. Writes to a `READ_ONLY` point are rejected. A point can also carry a `unit` and a linear conversion (
`baseValue` / `multiple`) that turns the raw value into an engineering value.
:::

## Three Layers of Configuration: Param, Attribute, Config

This is the part people trip over most. IoT DC3 splits "configuration" into three layers, each answering a different
question:

| Layer                    | Object                                                                       | Question it answers                                              | Source                                                   |
|--------------------------|------------------------------------------------------------------------------|------------------------------------------------------------------|----------------------------------------------------------|
| Business layer Param     | `CommandParam` / `EventParam`                                                | Which input/output parameters does this command/event have       | Defined in the profile model                             |
| Protocol layer Attribute | `DriverAttribute` / `PointAttribute` / `CommandAttribute` / `EventAttribute` | **Which** configuration items does this driver have              | Registered from `application.yml` when the driver starts |
| Instance layer Config    | `PointAttributeConfigDO`, etc.                                               | The **concrete values** **this device** fills in for those items | Set by the user for the device/point                     |

For example, the Modbus driver declares that "a point needs a register address" (the Attribute, registered by the
driver), while "device #3's temperature point lives at address 40001" (the Config, the value the device instance
supplies). Once this distinction clicks, the "configure point attributes" step
in [Device Onboarding](../operation/device-onboarding) makes sense.

## Data Flow and Command Flow

Around these objects, the platform runs two opposing pipelines. The **data flow** pulls device values up, stores them,
and exposes them for querying. The **command flow** sends read/write requests back down to devices for execution.

<ConceptsFlowDiagram lang="en" />

For the full implementation of both pipelines (exchanges, queues, lifecycle, acknowledgements), see
the [Data Plane](../architecture/data-plane) and [Command Plane](../architecture/command-plane).

## Tenant Boundary

Business data is isolated by **tenant (`tenantId`)**. When you call APIs, create devices, query data, or dispatch
commands, keep the tenant context consistent. The platform checks the tenant context at the controller layer (
`requireTenant` / `filterTenant`) — accessing another tenant's records by id or in bulk is treated as not-found (a 404
rather than the data itself). In development the default tenant is usually `default`; in production it follows your
organization and permission model. For how isolation is enforced layer by layer,
see [Auth · Tenant · RBAC](../architecture/auth-rbac).

## Concept Reference

Each core concept has its own entry covering its definition, key fields, relationships, lifecycle, and common pitfalls:

- [Profile (Thing Model)](./concepts/profile) — capability template for a class of devices, aggregating points /
  commands / events
- [Device](./concepts/device) — a platform mirror of one field device
- [Driver](./concepts/driver) — a protocol adapter service that talks to devices
- [Point](./concepts/point) — a single data item (a value to read or write)
- [Point Value](./concepts/point-value) — a snapshot of a point's value at a moment
- [Command](./concepts/command) — trigger a device action (vs. writing a point)
- [Event](./concepts/event) — a business occurrence a device reports
- [Attribute & Config](./concepts/attribute-config) — the Param / Attribute / Config layers
- [Tenant](./concepts/tenant) — the business-data isolation boundary

## Further Reading

- [Choose a Path by Role](./paths) — pick a reading order based on your goal
- [Device Onboarding](../operation/device-onboarding) — turn the concepts into a real onboarding
- [Domain Model](../architecture/domain-model) — DO/BO/VO layering and field details
- [Quick Start](../quickstart/) — bring up the stack locally
- [IoT Technology Overview](../foundations/) — place these concepts in the four-layer IoT architecture
