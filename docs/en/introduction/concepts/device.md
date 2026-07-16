---
title: Device
---

<script setup>
import DeviceRelationDiagram from '../../../.vitepress/theme/components/DeviceRelationDiagram.vue'
import DeviceStateDiagram from '../../../.vitepress/theme/components/DeviceStateDiagram.vue'
</script>

# Device

> **A Device is the platform-side mirror of one concrete field device**—a PLC, a thermostat, an electricity meter maps
> to one `Device` in DC3. It binds one [Profile](./profile) that decides "what capabilities it has", binds
> one [Driver](./driver) that decides "how it communicates", and at runtime its online/offline state is maintained by a
> heartbeat lease.

A Device answers "which physical machine is actually out there". It is neither the data itself nor the device's type
definition: that a certain thermostat model "should have temperature and humidity points" is what
the [Profile](./profile) says; the specific unit numbered `TC-001` on the shop floor, online right now, having just
reported temperature 25.3℃, is a `Device`.

Think of it this way: a [Profile](./profile) is like a class, a Device is like its instance. One Profile can be reused
by many devices—100 thermostats of the same model share one Profile; but a Device **belongs to exactly one** Profile (
the old many-to-many binding was collapsed into single ownership during the thing-model refactor).
Which [Points](./point) a device can collect, which [Commands](./command) it can receive, and which [Events](./event) it
can report are all decided by the Profile it binds.

A device's other binding is the [Driver](./driver): the Profile says "this device has a temperature point", but "read
that temperature from which Modbus register" is the driver's job. `profileId` decides the capability model, `driverId`
decides the communication channel; neither can be omitted.

## Key Fields

Device business object `DeviceBO` (table `dc3_device`). Field names and types are taken from the source:

| Field        | Type           | Meaning                                                         |
|--------------|----------------|-----------------------------------------------------------------|
| `deviceName` | String         | Device name (for display, e.g. "Workshop 1 Thermostat")         |
| `deviceCode` | String         | Device identifier                                               |
| `profileId`  | Long           | The owning [Profile](./profile), decides the capability model   |
| `driverId`   | Long           | The owning [Driver](./driver), decides the communication method |
| `deviceExt`  | DeviceExt      | JSON extension holding protocol-agnostic custom configuration   |
| `enableFlag` | EnableFlagEnum | Enable/disable flag, see below                                  |
| `tenantId`   | Long           | The owning [Tenant](./tenant), for multi-tenant isolation       |

Common fields inherited from `BaseBO`: `id`, `remark` (description), `creatorId`/`creatorName`, `operatorId`/
`operatorName`, `createTime`/`operateTime`.

::: tip profileId is a single value, not a set
Early on, a device could bind multiple Profiles (`Set<Long> profileIds`). After the thing-model refactor it was
collapsed to a single `Long profileId`: one Profile can be reused by many devices, but a device belongs to exactly one
Profile.
:::

## Enable Flag

| `enableFlag` | `0` enable | `1` disable |
|--------------|------------|-------------|

`enableFlag` is a configuration-time switch (whether this device is included in collection), which is a different matter
from the runtime online/offline state below: a disabled device does not participate in collection; only an enabled
device is polled by the driver and has its heartbeat lease maintained.

## Relationships With Other Concepts

<DeviceRelationDiagram lang="en" />

- A device obtains its [Point](./point), [Command](./command), and [Event](./event) definitions via `profileId`.
- A device produces [PointValues](./point-value) (`device_id + point_id`) and event instances at runtime.
- A device reaches its [Driver](./driver) via `driverId` to perform actual reads and writes.

## Online State and Heartbeat Lease

A device's "online/offline" is not a field on `dc3_device`, but a separate **runtime state lease** maintained by the
device/driver timeout-management mechanism, whose source of truth is the `dc3_entity_state` table (
`entity_type_flag = 6` denotes a device). The mechanism is "heartbeat renewal + timeout maintenance":

<DeviceStateDiagram lang="en" />

- **Renewal**: the driver health-checks the device on a configured cycle and reports `DeviceStateDTO`; the Data Center
  pushes `expire_time` forward (`now + timeout`) and increments `lease_version`.
- **Timeout**: a scanner wakes on a fixed tick and batch-marks devices whose `expire_time <= now()` and that are still
  in the online family as offline. Different devices' timeout lengths live in their own `expire_time`, not in the scan
  period.

There are four states (following the design's `EntityStateStatus` contract): `0` online, `1` offline, `2` maintain, `3`
fault.

::: warning Online state is queried from dc3_entity_state, not dc3_device
`dc3_device` holds the device's **configuration metadata** (name, ownership, extension), not high-frequency heartbeats;
for current online/offline state query `dc3_entity_state`. Writing heartbeats into `dc3_device` would pollute the
metadata table, which is exactly what the timeout design avoids.
:::

## Example

A Modbus thermostat on the shop floor is onboarded to DC3: first pick a [Profile](./profile) that describes the "
thermostat" class of device (containing the `temperature` and `humidity` [Points](./point)), then pick a
Modbus [Driver](./driver), and create a device
`DeviceBO{ deviceName: "Workshop 1 Thermostat", deviceCode: "TC-001", profileId: 1024, driverId: 2048, enableFlag: enable }`.
Once enabled, the Modbus driver reads registers according to the Profile's point configuration,
producing [PointValues](./point-value); meanwhile it reports device health every 15 seconds, on which the Data Center
renews this device's `expire_time` in `dc3_entity_state` by the 45-second lease TTL. When the driver loses connection on
some cycle and the heartbeat stops, the scanner marks it `offline` after `expire_time` has passed.

## Further Reading

- [Profile](./profile) — decides which points / commands / events a device has
- [Driver](./driver) — decides how a device communicates
- [Point](./point) — data point definitions under a Profile
- [Device Onboarding](../../operation/device-onboarding) — step-by-step onboarding of a field device
- [Concepts Overview](../concepts) — back to the concept map
