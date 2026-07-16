---
title: Point
---

<script setup>
import PointRelationDiagram from '../../../.vitepress/theme/components/PointRelationDiagram.vue'
</script>

# Point

> **A Point is a single data item**—one concrete quantity to be collected from, or written to, a class of devices. Its
> definition belongs to a [Profile](./profile); its runtime value is a [PointValue](./point-value).

A Point answers "which quantities can be read or written on this kind of device." On an air conditioner, "indoor
temperature", "set temperature", and "power state" are each a Point; their instantaneous numeric snapshots
are [PointValues](./point-value). An analogy: a [Profile](./profile) is the header definition of a table, each Point is
one **column** of it, and a [PointValue](./point-value) is the **cell** a given device fills into that column at a given
moment.

Two easy confusions:

- **Point ≠ PointValue.** A Point is the definition of a "column" (its name, type, writability, unit), and is stable; a
  PointValue is the value in a "cell", changing as collection proceeds. See [PointValue](./point-value).
- **Point ≠ Command.** A Point is a "quantity"; a [Command](./command) is an "action" (reboot, calibrate, switch mode).
  Whether a Point can be written is decided by its own `rwFlag`—it is **not, and never** registered in the `dc3_command`
  command table. Reading/writing a Point goes through the `PointCommand` path; custom commands go through the `Command`
  path.

## Key Fields

Point `PointBO` (table `dc3_point`):

| Field           | Type           | Meaning                                                                    |
|-----------------|----------------|----------------------------------------------------------------------------|
| `pointName`     | String         | Point name (for display, e.g. "indoor temperature")                        |
| `pointCode`     | String         | Point identifier, unique within a [Profile](./profile)                     |
| `pointTypeFlag` | PointTypeEnum  | Data type, see below                                                       |
| `rwFlag`        | RwTypeEnum     | Read/write capability, see below                                           |
| `unit`          | String         | Engineering unit, e.g. `℃`, `kPa`                                          |
| `baseValue`     | BigDecimal     | Offset for linear conversion (default `0`)                                 |
| `multiple`      | BigDecimal     | Multiplier for linear conversion (default `1`)                             |
| `valueDecimal`  | Byte           | Decimal precision, digits kept for floating-point values (default `6`)     |
| `profileId`     | Long           | Owning [Profile](./profile)                                                |
| `pointExt`      | PointExt       | Extended config (protocol mapping, constraints, collection strategy, etc.) |
| `enableFlag`    | EnableFlagEnum | Enable / disable state                                                     |
| `tenantId`      | Long           | Owning [Tenant](./tenant)                                                  |

## Data Type `pointTypeFlag`

| Enum                              | code                              | Meaning          |
|-----------------------------------|-----------------------------------|------------------|
| `STRING`                          | `string`                          | String (default) |
| `BYTE` / `SHORT` / `INT` / `LONG` | `byte` / `short` / `int` / `long` | Integer          |
| `FLOAT` / `DOUBLE`                | `float` / `double`                | Floating-point   |
| `BOOLEAN`                         | `boolean`                         | Boolean          |

## Read/Write Capability `rwFlag`

| Enum         | code | Meaning                                                  |
|--------------|------|----------------------------------------------------------|
| `READ_ONLY`  | `r`  | Read only; can be collected, cannot be written (default) |
| `WRITE_ONLY` | `w`  | Write only; can be written                               |
| `READ_WRITE` | `rw` | Readable and writable                                    |

::: warning Writability is decided by rwFlag, not the command table
Whether a Point can be written is determined **solely** by whether its `rwFlag` includes write capability (`WRITE_ONLY`
or `READ_WRITE`). This has nothing to do with [Command](./command) (`dc3_command`)—Point read/write is not modeled in
the command table. The center validates `rwFlag` before a write command: a write request against a read-only Point is
rejected outright.
:::

## Raw Value and Engineering Value Conversion

What a [Driver](./driver) reads from a device is often a **raw value** (a register integer, an ADC count, etc.). A Point
converts it to a human-readable **engineering value** via a linear formula:

```text
engineering value = raw value × multiple + baseValue   (then rounded by valueDecimal)
```

Example: a temperature transmitter register reads `2531`, configured with `multiple = 0.01`, `baseValue = 0`,
`unit = ℃`, `valueDecimal = 2`; the converted engineering value stored as a [PointValue](./point-value) is `25.31 ℃`.
The defaults `multiple = 1`, `baseValue = 0` mean the raw value equals the engineering value, with no conversion.

## Relationship to Other Concepts

<PointRelationDiagram lang="en" />

- A Point **definition** hangs under a [Profile](./profile), alongside [Command](./command) and [Event](./event),
  together describing "what capabilities this kind of device has."
- A [Device](./device) belongs to one Profile and thus automatically owns all Points under it; runtime data lands
  as [PointValues](./point-value) keyed by `device_id + point_id`.

## Example

Configure three Points for an air-conditioner Profile:

| pointCode     | pointName          | pointTypeFlag | rwFlag       | unit | Note                                        |
|---------------|--------------------|---------------|--------------|------|---------------------------------------------|
| `indoor_temp` | Indoor temperature | `FLOAT`       | `READ_ONLY`  | `℃`  | Collect only, `multiple=0.1` for conversion |
| `set_temp`    | Set temperature    | `FLOAT`       | `READ_WRITE` | `℃`  | Readable, and a new setpoint can be written |
| `power`       | Power state        | `BOOLEAN`     | `READ_WRITE` | —    | Read current state, write to start/stop     |

One air conditioner (`device_id=1001`) collects an engineering value of `26.5℃` for `indoor_temp` → it lands as
one [PointValue](./point-value). To set it to 22℃, send one write `PointCommand` against `set_temp`; because its
`rwFlag=READ_WRITE`, the write request passes validation and is dispatched to the driver.

::: tip A Point carries type, read/write, unit, and conversion together
A newly created Point, if not configured explicitly, defaults to type `STRING`, read/write `READ_ONLY`, `baseValue=0`,
`multiple=1`, `valueDecimal=6`, and an empty `unit`. When collecting a numeric quantity, remember to change it to the
matching numeric type and configure conversion, otherwise it is stored as a raw string.
:::

## Further Reading

- [Profile](./profile) — Point definitions hang under a Profile
- [PointValue](./point-value) — the runtime value snapshot of a Point
- [Command](./command) — action-type capability; Point read/write is not in this table
- [Event](./event) — another capability type alongside Point
- [Device Onboarding](../../operation/device-onboarding) — how a device picks a Profile and inherits its Points
