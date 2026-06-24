---
title: Listening Virtual Driver
---

# Listening Virtual Driver

> **`dc3-driver-listening-virtual` onboards TCP/UDP devices that push data into IoT DC3 on their own**——the driver opens a listening port and waits for devices to connect, then slices and parses values out of the pushed byte stream according to each point's configuration.

Some field devices don't wait to be polled; they periodically push data out by themselves: GPS trackers, environmental monitoring boxes, and assorted sensors speaking proprietary binary frames. The common pattern is to connect to a fixed IP:port and send a byte stream. This driver is built for exactly that case—it is a **listening (passive) [driver](../introduction/concepts/driver)** that starts both a TCP and a UDP listening port; the [device](../introduction/concepts/device) connects in and pushes data, and the driver parses the byte stream into [point values](../introduction/concepts/point-value). It never actively "reads" a device, so it fits: GPS/BeiDou terminals with their own reporting logic, sensor gateways that push binary frames, and any proprietary TCP/UDP protocol where the client connects to a passive server.

Two driver-specific concepts come first, since the config tables use them repeatedly:

- **Keyword**: the single byte right after the device name in the pushed packet, written in hexadecimal (e.g. `62`). One device can use different keywords to distinguish packet types; the driver uses it to decide which [point](../introduction/concepts/point) a given frame should be parsed into.
- **Byte range (Start / End)**: the byte offsets at which data is sliced out of the packet. `start` is the start offset (inclusive), `end` is the end offset (exclusive); fixed-length numeric points only read a fixed number of bytes from `start`, while the `coordinate` string point uses the whole `start..end` range.

## Packet Format

Every packet a device pushes has this fixed structure:

```
[ Device Name 22 bytes ][ Keyword 1 byte ][ Payload variable ]
```

- The first 22 bytes are the device name (the driver parses the [device](../introduction/concepts/device) ID from it; it must map one-to-one to a device on the platform).
- The 23rd byte (offset 22) is the keyword, compared against the point's `key`.
- The rest is the payload; each point slices a segment from it per its own `start`/`end`, and the **parse type is decided by the point name**.

## Driver Name / Code / Type

- **Driver name / code**: `Listening Virtual TCP/UDP Driver` / `ListeningVirtualDriver`
- **Type**: `DRIVER_SERVER` (the driver acts as the listening server, passively receiving pushed data)

## Driver Configuration (device-level `driver-attribute`)

This driver **declares no device-level `driver-attribute`**—the listening ports are process-level configuration (TCP defaults to `6270`, overridable via the `TCP_PORT` environment variable; UDP defaults to `6271`, overridable via `UDP_PORT`), and all onboarding configuration lives on the [point](../introduction/concepts/point).

## Point Configuration (`point-attribute`)

Each [point](../introduction/concepts/point) carries these [attributes](../introduction/concepts/attribute-config), telling the driver: which keyword to match and which segment of the packet to slice (the parse type is decided by the point name, see the note below):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Keyword | `key` | STRING | `62` | Packet identification keyword |
| Start Byte | `start` | INT | `0` | Inclusive start byte offset |
| End Byte | `end` | INT | `8` | Exclusive end byte offset |
| Type | `type` | STRING | `string` | Required attribute; presence-checked only, not used to choose the parse type |

::: tip key is hexadecimal, compared byte-for-byte with the packet keyword
`key` holds the hexadecimal value of the packet's 23rd byte (e.g. the default `62`). When a frame arrives, the driver only parses points whose keyword matches; points whose keyword differs produce no value for that frame. Different points on the same device may share one `key` (extracting several fields from one frame) or use different keys (each frame handled separately).
:::

::: warning The parse type is decided by the point name, not the `type` attribute
The driver chooses how to parse based on the **point name (pointName)**, and only these 6 names are supported: `altitude`→float, `speed`→double, `level`→long, `direction`→int, `locked`→boolean, `coordinate`→string. The point name must be one of these, otherwise the point parses to an empty value for that frame and collects no data. The `type` attribute is only presence-checked during validation (it is required); it is never read during parsing.
:::

This driver listens passively and has no command to write back to the device, so there is **no `command-attribute`**. The underlying layer retains the ability to write bytes back over the device's TCP connection, but it is not exposed as a configurable write command.

## Collection & Health

- **Collection cycle**: this driver does **no active polling**—`schedule.read.enable=false`, and data is entirely push-triggered by the device. The driver does have an internal `0/5 * * * * ?` job (`schedule.custom`, every 5 seconds) for the driver's own periodic upkeep; it issues no reads to devices.
- **Health / online**: the device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`—each push refreshes the lease; if nothing is pushed before the timeout, the device is marked offline. See [device](../introduction/concepts/device) for the online-state mechanism.

## Minimal Onboarding Example

Onboard a GPS terminal that pushes data over TCP:

1. Create a [device](../introduction/concepts/device) with `Listening Virtual TCP/UDP Driver` (this driver has no driver attributes, so the device needs no connection parameters).
2. Add a string [point](../introduction/concepts/point) to the [profile](../introduction/concepts/profile) bound to the device; the **point name must be one of the supported names** (here `coordinate`, which parses as a string; `pointTypeFlag=STRING`), with point attributes `key=62`, `start=23`, `end=31`, `type=string`—i.e. match packets with keyword `62` and take 8 bytes from the start of the payload as a string. If the point name is not one of `altitude/speed/level/direction/locked/coordinate`, the driver collects no value.
3. Start the driver; have the device push "22-byte device name + 1-byte `0x62` + payload" to the driver's TCP port `6270`, and within seconds the parsed result appears in [point values](../introduction/concepts/point-value).

## Pitfalls

::: warning The first 22 bytes of the packet must be the platform device ID
The driver parses the device ID from the first 22 bytes to match a [device](../introduction/concepts/device) on the platform. When the device pushes data, it **must left-align the numeric device ID into these 22 bytes**—if no number can be parsed, or no device matches, the frame is silently dropped without error. Create the device on the platform first to get its ID, then configure it into the device firmware.
:::

::: warning start/end offsets are relative to the whole frame, not the payload
A point's `start`/`end` are byte offsets into the **whole frame**. The first 23 bytes are taken by the device name and keyword, so the first payload byte is at offset `23`, not `0`. To read from the start of the payload, `start` must count from `23`; the default `start=0` lands inside the device name and reads the wrong value.
:::

::: tip Fixed-length numeric points only use start; only coordinate uses end
Fixed-length numeric points (`altitude`/`speed`/`level`/`direction`/`locked`) read their fixed byte width from `start` (e.g. `altitude` reads 4 bytes, `speed` reads 8 bytes), and `end` is ignored. Only `coordinate` slices the `start..end` range as a string. If the packet is too short or the offset is out of bounds, that point produces no value for the frame.
:::

## Further Reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes & Config](../introduction/concepts/attribute-config) — the three-tier origin of attributes like `key` / `start`
- [Device Onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [HTTP Driver](./http) — another way to onboard an external data source
