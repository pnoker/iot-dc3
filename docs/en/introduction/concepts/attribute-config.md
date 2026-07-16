---
title: Attribute & Config
---

<script setup>
import AttributeConfigRelationDiagram from '../../../.vitepress/theme/components/AttributeConfigRelationDiagram.vue'
import AttributeConfigFlowDiagram from '../../../.vitepress/theme/components/AttributeConfigFlowDiagram.vue'
</script>

# Attribute & Config

> **An Attribute is a [Driver](./driver)'s declaration of "which config items must be filled in to connect a device",
while a Config is the concrete value a [Device](./device) supplies for those items.** One answers "which blanks exist to
> fill in", the other answers "what this device filled into those blanks".

Whether a device can be collected often hinges not on "what the temperature point is called", but on very concrete
protocol details: which register a Modbus read targets, which URL an HTTP request hits, which Topic an MQTT subscription
uses. These details vary by protocol and by device, so hard-coding them is a non-starter. IoT DC3 splits this into two
layers: **the Driver declares which config items are needed (Attribute)**, and **the device instance fills in the values
for those items (Config)**.

The key to understanding it is to first separate the **three distinct things** at play here:

| Layer                                | Belongs to           | Question it answers                           | Who fills it                                |
|--------------------------------------|----------------------|-----------------------------------------------|---------------------------------------------|
| **Param (business parameter)**       | [Profile](./profile) | which business fields a command/event carries | the modeler, defined in the template        |
| **Attribute (attribute definition)** | [Driver](./driver)   | which config items this protocol needs        | the driver developer, registered at startup |
| **Config (config value)**            | [Device](./device)   | what this device fills into those items       | the integrator, on the device edit page     |

`Param` is "business semantics" (temperature, mode, fault code), belongs to the [Profile](./profile), and is
protocol-agnostic; `Attribute` / `Config` are "protocol mapping" (register address, Topic, payload template), and belong
to the driver and device. This page covers only the latter two layers — for Param, see [Command](./command)
and [Event](./event).

## A classic example: Attribute vs Config in one line

> The Modbus driver declares "reading a point needs a register address" — this is an **Attribute** (the driver
> registers "such a config item exists").
> Filling in "address = 40001" for device #3's temperature point — this is a **Config** (the concrete value of that
> config item for this device).

For the same `PointAttribute(registerAddress)`, device #1 might fill in 40001 and device #3 fill in 40003; switch to an
MQTT driver and what gets declared is no longer a register address but a `topic`. An Attribute is the mold; a Config is
the cast part poured from it.

## Where Attribute definitions come from

::: tip Attributes are not hand-built in the database — they are registered by the driver at startup
A driver declares the attributes it supports in its own `application.yml` and **reports them to the Manager at startup
**. The Manager persists them keyed uniquely by `tenant_id + driver_id + attribute_code`. Different protocols need
different attributes, so the authoritative source of an attribute is the driver, not a manually edited page.
:::

```yaml
dc3:
  driver:
    driver-attribute:        # connection-level items: what connecting to this device gateway needs
      - attribute-name: Host
        attribute-code: host
        attribute-type-flag: STRING
        default-value: localhost
    point-attribute:         # point-level items: what collecting each point needs
      - attribute-name: Register Address
        attribute-code: registerAddress
        attribute-type-flag: INT
        default-value: ''
```

`DriverAttribute` and `PointAttribute` differ only in **scope**: the former is filled once per device (connection info),
the latter once per [Point](./point) (collection mapping).

## Key fields

Attribute definition `DriverAttributeBO` / `PointAttributeBO` (the two have identical fields, sharing the same
structure):

| Field               | Type                                   | Meaning                                                                     |
|---------------------|----------------------------------------|-----------------------------------------------------------------------------|
| `attributeName`     | String                                 | Attribute name (for display)                                                |
| `attributeCode`     | String                                 | Attribute code, matched against by configs (e.g. `host`, `registerAddress`) |
| `attributeTypeFlag` | AttributeTypeEnum                      | Value type, see below                                                       |
| `defaultValue`      | String                                 | Default value, used when a device leaves it blank                           |
| `driverId`          | Long                                   | The owning [Driver](./driver)                                               |
| `attributeExt`      | DriverAttributeExt / PointAttributeExt | Extension info (e.g. UI component, validation rules)                        |
| `enableFlag`        | EnableFlagEnum                         | Enable/disable status                                                       |
| `tenantId`          | Long                                   | The owning [Tenant](./tenant)                                               |

Config value `DriverAttributeConfigBO` / `PointAttributeConfigBO`:

| Field         | Type           | Meaning                                                              |
|---------------|----------------|----------------------------------------------------------------------|
| `attributeId` | Long           | Which attribute definition it points to                              |
| `configValue` | String         | The actual value filled in (e.g. `40001`)                            |
| `deviceId`    | Long           | The owning [Device](./device)                                        |
| `pointId`     | Long           | **`PointAttributeConfig` only**, which [Point](./point) it points to |
| `configExt`   | JsonExt        | Config extension info                                                |
| `enableFlag`  | EnableFlagEnum | Enable/disable status                                                |
| `tenantId`    | Long           | The owning [Tenant](./tenant)                                        |

::: warning DriverConfig is per device, PointConfig is per point
`DriverAttributeConfig` has only `deviceId`, because connection info is one set per device; `PointAttributeConfig` adds
a `pointId`, because every point needs its own collection mapping. This is the direct consequence of the two layers
having different scopes.
:::

## Value type AttributeTypeEnum

`attributeTypeFlag` shares the same type system as [Point](./point):

| Value | `STRING` | `BYTE` | `SHORT` | `INT` | `LONG` | `FLOAT` | `DOUBLE` | `BOOLEAN` |
|-------|----------|--------|---------|-------|--------|---------|----------|-----------|

## Relationship to other concepts

<AttributeConfigRelationDiagram lang="en" />

Attributes hang under a driver and are referenced by device configs; a point config is additionally bound to a
specific [Point](./point). At modeling time you define a [Profile](./profile) (with points, commands, events); at
onboarding time the driver declares attributes and the device fills configs — the two lines converge at the device.

## Registration and configuration flow

<AttributeConfigFlowDiagram lang="en" />

1. The driver reads `driver-attribute` / `point-attribute` from `application.yml` and reports them at startup.
2. The Manager inserts or updates the attribute definitions by unique key.
3. The device edit page loads the attribute columns for the current `driverId`, and the integrator fills in each value.
4. Config values land in the Config tables; at runtime the driver pulls them and assembles the actual protocol payload.

## What Attribute / Config can and cannot solve

`Attribute + Config` solves **protocol mapping**: translating "this device's point/connection" into the concrete
address, Topic, and template the driver can execute. It **cannot** on its own express "what the point itself is" or "
which business parameters a command carries" — those are the responsibility of the [Profile](./profile) and Param.

| Can solve                                                             | Cannot solve alone (needs other models)                                         |
|-----------------------------------------------------------------------|---------------------------------------------------------------------------------|
| Which Host / port to connect this device gateway                      | Which points, commands, events this device class has → [Profile](./profile)     |
| Which register / path this point reads                                | Which business inputs/outputs a command carries → Param of [Command](./command) |
| Different mapping values for the same Profile under different drivers | Which business fields an event reports → Param of [Event](./event)              |

::: tip Locate it in one line
Missing collection? Usually a **Config not filled or filled wrong** (wrong address/Topic). Missing capability? That's an
**Attribute not declared** (the driver never registered that config item) — the latter means editing the driver's
`application.yml` and restarting, the former is just a value change on the page, no restart needed.
:::

## Example

Temperature sensor #3, bound to a Modbus driver:

- Attributes registered by the driver: `DriverAttribute(host)`, `PointAttribute(registerAddress)`.
- Configs filled by the device: `DriverAttributeConfig{ deviceId: 3, configValue: "192.168.1.10" }` (connect to this
  gateway); the temperature point fills
  `PointAttributeConfig{ deviceId: 3, pointId: temperature point, configValue: "40001" }`.

At runtime the driver connects to `192.168.1.10`, reads register `40001`, and wraps the reading into that
point's [PointValue](./point-value) for reporting. For device #1, just change `configValue` to a different address — the
attribute definition is fully reused.

## Further reading

- [Driver](./driver) — the declarer and registration source of attributes
- [Point](./point) — the object that `PointAttributeConfig` binds to
- [Command](./command) — the division of labor between Param (business parameters) and Attribute (protocol mapping)
- [Event](./event) — the event side likewise separates Param from Attribute
- [Device Onboarding](../../operation/device-onboarding) — the full flow of filling these configs on the page
- [Core Concepts Overview](../concepts) — back to the concept map
