---
title: Virtual Driver
---

# Virtual Driver

> **`dc3-driver-virtual` is the virtual (simulation) driver of IoT DC3**——it connects to no real device, instead generating random [point](../introduction/concepts/point) [values](../introduction/concepts/point-value) on the collection schedule, and can simulate command dispatch and event reporting.

Virtual is not a fieldbus protocol but a **simulation driver**: it exercises the whole IoT DC3 onboarding flow (create a [device](../introduction/concepts/device), configure a [profile](../introduction/concepts/profile), run collection, watch [point values](../introduction/concepts/point-value)) while sending no network frames at all—every value is fabricated locally at random. Use it for:

- **Trying out / demoing the platform**——with no real PLC/sensor at hand, validate the end-to-end path first.
- **Learning the driver model**——it is the simplest [driver](../introduction/concepts/driver); its source (`VirtualDriverCustomServiceImpl`) is the reference template for writing a custom driver.
- **Load testing & integration**——bulk-create devices and points to observe the platform under a data stream.

On read it fabricates a value by the point's data type: `STRING` returns the fixed `abcd1234`, `BOOLEAN` returns a random true/false, and any other type returns a random float between `0` and `100`.

- **Driver name / code**: `Virtual Driver` / `VirtualDriver`
- **Type**: `DRIVER_CLIENT` (the driver is the active side, producing data on a schedule)

## Driver configuration (device-level `driver-attribute`)

When onboarding a virtual device, fill these [attributes](../introduction/concepts/attribute-config) on the [device](../introduction/concepts/device). Note: the virtual driver never actually connects to `host:port`; these two are placeholders that keep the same config shape as a real driver so the drill is easy to follow.

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | Ip |
| Port | `port` | INT | `18600` | Port |

## Point configuration (`point-attribute`)

Fill these on each collected [point](../introduction/concepts/point):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Tag | `tag` | STRING | `TAG` | Point tag name |

::: tip The fabricated value depends on point type, not on tag
The virtual driver ignores the actual content of `tag`; fabrication looks only at the point's own data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`): `STRING` yields `abcd1234`, `BOOLEAN` yields a random boolean, numeric types yield a random float in `0~100`. `tag` is merely a placeholder identifying the point on the device.
:::

## Write command configuration (`command-attribute`)

The virtual driver supports simulated command dispatch (it writes nothing to a real device). On dispatch it renders `payloadTemplate` with the command params to obtain the request payload, then renders and parses `responseTemplate` into a mock response:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Payload Template | `payloadTemplate` | STRING | `${value}` | Template rendered with command params |
| Response Template | `responseTemplate` | STRING | `{}` | Mock response template |

::: tip Templates use `${...}` placeholders rendered from the command context
Placeholders such as `${value}`, `${deviceCode}`, `${commandCode}` in the templates are substituted from the command params and the device/command context. When `responseTemplate` is JSON, its fields are parsed verbatim into the command result; otherwise the whole string is returned as `response`. None of this touches a real device—it is pure simulation.
:::

## Event configuration (`event-attribute`)

The virtual driver also periodically simulates [event](../introduction/concepts/event) reporting for the device (one round every 30 seconds). The event attributes use JSON Path to tell the driver which path in the simulated message holds the event code, and which holds the event payload:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | JSON path used to resolve event code |
| Payload Path | `payloadPath` | STRING | `$.payload` | JSON path used to resolve event payload |

## Collection & health

- **Collection schedule**: default cron `0/30 * * * * ?` (fabricate one round every 30 seconds). The driver also runs an internal `0/5 * * * * ?` timer (`schedule.custom`), within which event reporting is triggered at a 30-second interval.
- **Health / online**: device health check defaults to cron `0/15 * * * * ?`, lease timeout `45 seconds`——see [device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

No real hardware needed—onboard one virtual device and watch the data flow:

1. Create a [device](../introduction/concepts/device) with `Virtual Driver`, set driver attributes `host=localhost`, `port=18600` (defaults are fine).
2. Add a temperature [point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`) to the [profile](../introduction/concepts/profile) bound to the device, and set the point attribute `tag=temperature`.
3. Start the driver; within 30 seconds you'll see a continuously changing random value between `0` and `100` in the [point values](../introduction/concepts/point-value).

## Pitfalls

::: warning host/port are placeholders; reachability never affects output
The virtual driver never opens a connection to `host:port`, so it produces random values even with a non-existent address. In other words, **it cannot validate real network connectivity**——it only exercises the platform flow; to test a real link, switch to the driver for that protocol.
:::

::: tip Point type shapes the value; get `pointTypeFlag` right first
Want boolean flips? Configure the point as `BOOLEAN`. Want a continuous curve? Configure a numeric type (e.g. `FLOAT`). A mismatched point type only yields an unexpected value shape, not an error—just like with a real device: type is a point-side convention.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the generic driver model and registration mechanism
- [Attribute & Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `tag`
- [Device onboarding](../operation/device-onboarding) — one complete onboarding flow
- [Listening Virtual Driver](./listening-virtual) — the passive-listening simulation/onboarding driver
