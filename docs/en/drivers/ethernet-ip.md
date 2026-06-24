---
title: EtherNet/IP Driver
---

# EtherNet/IP Driver

> **`dc3-driver-ethernet-ip` connects EtherNet/IP (CIP) PLCs to IoT DC3**——targeting tags, it periodically reads PLC tag values and supports commands that write values to tags.

EtherNet/IP is an industrial Ethernet protocol that carries CIP (Common Industrial Protocol), used mainly with Rockwell Allen-Bradley PLCs (such as ControlLogix / CompactLogix). Unlike register-addressed protocols like Modbus, CIP is **addressed by tag name**: variables in the PLC have names (e.g. `Motor_Speed`), and the driver reads/writes them by name through the CIP Data Table Read/Write services, with no need to deal with physical addresses.

This driver acts as an EtherNet/IP client, connecting to the PLC over a raw TCP socket (default port 44818) and reading/writing data using the tag name configured on each [Point](../introduction/concepts/point).

::: warning Work in progress (skeleton)
This driver is a protocol skeleton: the CIP session setup (RegisterSession / ForwardOpen) and the encapsulation frame are still placeholders, the source method bodies carry TODO markers, and `health()` only inspects the cached socket connection state rather than performing a real protocol probe. Treat it as a starting template, not a production-ready driver.
:::

- **Driver name / code**: `EtherNet/IP Driver` / `EthernetIpDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the PLC)

## Driver Configuration (device-level `driver-attribute`)

When onboarding an EtherNet/IP PLC, fill in these [attributes](../introduction/concepts/attribute-config) on the [device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | PLC host address |
| Port | `port` | INT | `44818` | EtherNet/IP TCP port (standard 44818) |
| Slot | `slot` | INT | `0` | PLC backplane slot |
| Timeout | `timeout` | INT | `5000` | Request timeout (milliseconds) |

## Point Configuration (`point-attribute`)

On each collected [Point](../introduction/concepts/point), fill in:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Tag Name | `tagName` | STRING | (empty) | CIP tag name, e.g. `Motor_Speed` |
| Tag Type | `tagType` | STRING | `DINT` | Tag data type: `BOOL` / `SINT` / `INT` / `DINT` / `REAL` / `STRING` |
| Element Count | `elementCount` | INT | `1` | Number of elements to read (for array tags); **not consumed by the current implementation** — the element count in the read request is hardcoded to `1` |

::: tip Tag Type decides how bytes are decoded
The driver parses the raw bytes returned by the PLC into the matching type according to `tagType`: `BOOL` 1 byte, `SINT` 1 byte, `INT` 2 bytes, `DINT` 4-byte integer, `REAL` 4-byte float, `STRING` ASCII text. `tagType` must match the actual type of that tag in the PLC, otherwise parsing fails. The Point's own data type ([Point](../introduction/concepts/point) `pointTypeFlag`) should match it.
:::

## Write Command Configuration (`command-attribute`)

Writable points also need this on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | `${value}` | Write-value template rendered from the command argument; encoded on write according to the Point's `tagType`. **Not consumed by the current implementation** — writes take the value passed in the command directly and do not go through this template |

## Collection and Health

- **Collection cycle**: default cron `0/30 * * * * ?` (reads once every 30 seconds).
- **Custom schedule**: `schedule.custom` is enabled (cron `0/5 * * * * ?`), but the current `schedule()` method body is empty and performs no custom logic.
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`——see [Device](../introduction/concepts/device) for the online-state mechanism.

## Minimal Onboarding Example

To onboard an Allen-Bradley PLC at IP `192.168.1.20:44818`, slot 0:

1. Create a [device](../introduction/concepts/device) with `EtherNet/IP Driver`, and set the driver attributes `host=192.168.1.20`, `port=44818`, `slot=0`, `timeout=5000`.
2. Add a speed [Point](../introduction/concepts/point) (`pointTypeFlag=INT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `tagName=Motor_Speed`, `tagType=DINT`, `elementCount=1`.
3. Start the driver, and within 30 seconds the collected value appears in [PointValue](../introduction/concepts/point-value).

## Common Pitfalls

::: warning tagName is the PLC tag name, not an address
CIP is addressed by name. `tagName` must match the variable name defined in the PLC program **verbatim** (case-sensitive). A wrong name fails the read rather than reading a wrong address——this differs from Modbus `offset`: a wrong Modbus offset silently reads a different register, whereas a non-existent CIP tag name errors out directly.
:::

::: tip tagType must match the PLC tag's real type
The driver does not auto-detect the PLC tag type; it decodes bytes strictly according to the `tagType` you set. Configuring a `REAL` (float) tag as `DINT` parses the float's bytes as an integer, yielding a meaningless large number. Confirm the actual type of each tag in the PLC project before onboarding.
:::

## Further Reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes and Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `tagName`
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding flow
- [Modbus TCP Driver](./modbus-tcp) — register-addressed Modbus
