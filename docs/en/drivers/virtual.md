---
title: Virtual Driver
---

# Virtual Driver

`dc3-driver-virtual` is the **virtual (simulation) driver** of IoT DC3: it connects to no real device, instead
generating random [point](../introduction/concepts/point) [values](../introduction/concepts/point-value) on the
collection schedule, and simulates command execution and event reporting. After this page you can use it to exercise the
whole onboarding path end to end, and understand which of its capabilities are real implementations versus placeholders.

> You are here: you have no real PLC/sensor yet and want to validate the platform end to end first, or you want the
> simplest [driver](../introduction/concepts/driver) template to copy when writing your own protocol. Next,
> see [device onboarding](../operation/device-onboarding).

## Protocol background

Virtual is not a fieldbus or industrial protocol but a **simulation driver**—it exercises the whole IoT DC3 onboarding
flow (create a [device](../introduction/concepts/device), configure a [Profile](../introduction/concepts/profile), run
collection, watch [point values](../introduction/concepts/point-value)) while sending no network frames at all; every
value is fabricated locally at random. Because it has no real protocol layer, it does **not** belong to any
network-protocol tier of the IoT four-layer architecture; it is a "device-less onboarder" used on the platform side for
demos, learning, and load testing. Typical uses:

- **Trying out / demoing the platform**——with no real hardware at hand, validate the end-to-end path first.
- **Learning the driver model**——it is the simplest driver; its source `VirtualDriverCustomServiceImpl` is the reference
  template for writing a custom driver.
- **Load testing & integration**——bulk-create devices and points to observe the platform under a sustained data stream.

On read it fabricates a value by the point's data type: `STRING` returns the fixed `abcd1234`, `BOOLEAN` returns a
random true/false, and any other type returns a random float between `0` and `100`. What gets fabricated depends only on
the point's own data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`), not on the `tag` content.

::: info The virtual driver has no real protocol layer
Other driver pages link to their protocol spec; this one does not—Virtual implements no wire protocol. Its "link" lives
entirely inside IoT DC3. For real protocol drivers, see the [driver overview](./index).
:::

## Attribute configuration

Attributes fall into two kinds: **driver attributes** (`driver-attribute`) filled on
the [device](../introduction/concepts/device), and **point attributes** (`point-attribute`) filled on
each [point](../introduction/concepts/point). The driver also declares command attributes and event attributes, used for
template rendering in command execution and event reporting. These definitions come from the driver module's
`application.yml`; each is explained below with its purpose and origin.

### Driver attributes (device-level `driver-attribute`)

When onboarding a virtual device, fill the two items below. Note: the virtual driver **never actually connects** to
`host:port`; these are placeholders that keep the same config shape as a real driver so the drill is easy to follow.

| Attribute | code   | Type   | Default     | Remark                                        |
|-----------|--------|--------|-------------|-----------------------------------------------|
| Host      | `host` | STRING | `localhost` | Device IP (placeholder, no real connection)   |
| Port      | `port` | INT    | `18600`     | Device port (placeholder, no real connection) |

### Point attributes (`point-attribute`)

Fill one `tag` on each collected point, as a placeholder identifying the point on the device:

| Attribute | code  | Type   | Default | Remark         |
|-----------|-------|--------|---------|----------------|
| Tag       | `tag` | STRING | `TAG`   | Point tag name |

::: tip The fabricated value depends on point type, not on tag
The virtual driver ignores the actual content of `tag`; fabrication looks only at the point's data type `pointTypeFlag`:
`STRING` yields `abcd1234`, `BOOLEAN` a random boolean, numeric types a random float in `0~100`. Want boolean flips?
Configure the point as `BOOLEAN`. Want a continuous curve? Configure `FLOAT`.
:::

### Command attributes (`command-attribute`)

The virtual driver implements **command execution** (`execute()`): on dispatch it renders `payloadTemplate` with the
command params and the device/command context to obtain the request payload, then renders and parses `responseTemplate`
into a mock response. None of this touches a real device.

| Attribute         | code               | Type   | Default    | Remark                                                |
|-------------------|--------------------|--------|------------|-------------------------------------------------------|
| Payload Template  | `payloadTemplate`  | STRING | `${value}` | Request payload template rendered with command params |
| Response Template | `responseTemplate` | STRING | `{}`       | Mock response template                                |

::: tip Templates use `${...}` placeholders rendered from the command context
Placeholders such as `${value}`, `${deviceCode}`, `${commandCode}`, `${deviceId}`, `${commandName}` are substituted one
by one (string replace) from the command params and the device/command context. When `responseTemplate` is a JSON
object, its fields are parsed verbatim into the command result; otherwise the whole string is returned as the `response`
field. The result also carries the rendered `payload`.
:::

### Event attributes (`event-attribute`)

The virtual driver periodically simulates [event](../introduction/concepts/event) reporting for the device (one round
every 30 seconds). The event attributes use JSON-Path-like expressions to tell the driver which path in the simulated
message holds the event code, and which holds the event payload.

| Attribute       | code            | Type   | Default       | Remark                                      |
|-----------------|-----------------|--------|---------------|---------------------------------------------|
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | JSON path used to resolve the event code    |
| Payload Path    | `payloadPath`   | STRING | `$.payload`   | JSON path used to resolve the event payload |

::: warning Only simple dotted paths are supported, not full JSONPath
The driver's internal `resolvePath()` only walks the Map segment by segment on `.` (e.g. `$.payload.value`); it does not
support array indices, filter expressions, or other full JSONPath syntax. The simulated message looks like
`{"eventCode":"...","payload":{"value":...,"deviceCode":...,"source":"virtual"}}`, and falls back to the event's own
`eventCode` when a path resolves to nothing.
:::

## Troubleshooting

Virtual almost never fails for "cannot reach the device" (it never connects), so common issues center on scheduling,
type, and configuration:

- **No point values within 30 seconds**: confirm the driver is registered and online,
  the [Profile](../introduction/concepts/profile) bound to the device has enabled points, and the collection schedule
  `dc3.driver.schedule.read.enable=true` (on by default, cron `0/30 * * * * ?`). The first value can take up to one
  collection period.
- **Value shape is not as expected** (you wanted a boolean but got a number): check the point's `pointTypeFlag`. A
  mismatched type only yields an unexpected value shape, **not an error**—type is a point-side convention.
- **Write command always fails / nothing echoed back**: this is expected. The driver's point write `write()` is a
  placeholder that always returns `false`; see "How it lands in IoT DC3" below. For a working downstream path,
  use [command execution](#attribute-configuration) (`execute()`, via command attributes) or switch to a real driver.
- **No event reports arrive**: event reporting is driven by the internal timer `dc3.driver.schedule.custom` (cron
  `0/5 * * * * ?`) and throttled to a 30-second interval; the device must also have **enabled** event definitions,
  otherwise that round is skipped.
- **Device shows offline**: health check cron `0/15 * * * * ?`, lease timeout `45 seconds`. If the driver process stops
  or misses its periodic heartbeat, the device is marked offline—see [device](../introduction/concepts/device) for the
  online mechanism.
- **Wrong host/port yet values still appear**: see the pitfall below—the virtual driver neither validates nor connects
  to `host:port`.

::: warning host/port are placeholders; reachability never affects output
The virtual driver never opens a connection to `host:port`, so it produces random values even with a non-existent
address. In other words, **it cannot validate real network connectivity**——to test a real link, switch to the driver for
that protocol.
:::

## How it lands in IoT DC3

- **`dc3.driver.code`**: `VirtualDriver` (driver name `Virtual Driver`). This is the stable routing identifier the
  platform uses to route devices, commands, and events to this driver; do not change it casually.
- **Type**: `DRIVER_CLIENT`——the driver is the active side, producing data on a schedule.
- **Capabilities** (aligned with the [driver capability matrix](./matrix)):

| Capability                  | Status      | Notes                                                     |
|-----------------------------|-------------|-----------------------------------------------------------|
| Read `read()`               | Available   | Fabricates random values by point type; fully implemented |
| Write `write()`             | Placeholder | Point write always returns `false`, writes to no device   |
| Command execute `execute()` | Available   | Template render + mock response; fully implemented        |
| Event report `schedule()`   | Available   | Simulates one event-report round every 30 seconds         |

::: warning Point write is a placeholder
`write()` returns `false` directly in source—any request to write a value through a point always "fails" and changes no
state. This matches Virtual's "Write = —" in the [driver capability matrix](./matrix). When you need a working
downstream capability, use command execution (`execute()`, with `command-attribute`), or switch to a real driver.
:::

::: info Events/commands are real implementations, but differ from the matrix's "protocol subscribe" meaning
The matrix marks Virtual's "Subscribe/Report" as `—`, meaning it has no passive subscription on a real protocol layer.
But in code, both `execute()` and `schedule()` (event reporting) are fully implemented simulation capabilities—this page
labels them honestly per source. The validation methods `validate()`/`validatePoint()` currently perform no real checks
and always pass.
:::

### Minimal onboarding example

No real hardware needed—onboard one virtual device and watch the data flow:

1. Create a [device](../introduction/concepts/device) with `Virtual Driver`, set driver attributes `host=localhost`,
   `port=18600` (defaults are fine).
2. Add a temperature [point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attribute `tag=temperature`.
3. Start the driver; within 30 seconds you'll see a continuously changing random value between `0` and `100` in
   the [point values](../introduction/concepts/point-value).

## Further reading

- [Driver overview](./index) — pick a protocol by category and open its page
- [Driver capability matrix](./matrix) — the real read/write/subscribe implementation of every driver at a glance
- [Device onboarding](../operation/device-onboarding) — one complete onboarding flow
- [Custom driver](../development/driver-authoring) — implement your own protocol driver on the `virtual` template
- [Listening Virtual Driver](./listening-virtual) — the passive-listening simulation/onboarding driver
