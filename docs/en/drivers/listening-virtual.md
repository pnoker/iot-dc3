---
title: Listening Virtual Driver
---

<script setup>
import ListeningVirtualDiagram from '../../.vitepress/theme/components/ListeningVirtualDiagram.vue'
</script>


# Listening Virtual Driver

> `dc3-driver-listening-virtual` onboards TCP/UDP devices that push data into IoT DC3 on their own. The driver opens a
> listening port, waits for devices to connect, then slices and parses values out of the pushed byte stream according to
> each [point](../introduction/concepts/point)'s configuration. After this page you can configure your first listening
> point and have a GPS/BeiDou-class terminal push binary frames into the platform.

Some field devices don't wait to be polled; they periodically push data out by themselves: GPS trackers, environmental
monitoring boxes, and assorted sensors speaking proprietary binary frames. The common pattern is to connect to a fixed
IP:port and send a byte stream. This driver is built for exactly that case—it is a **listening (passive) driver** that
starts both a TCP and a UDP listening port; the device connects in and pushes data, and the driver parses the byte
stream into [point values](../introduction/concepts/point-value). It never actively "reads" a device.

## Protocol Background

This is a **virtual / testing driver** with no real standard protocol layer—the packet format is a minimal binary
convention defined by the driver itself, meant to demonstrate and exercise the "device-initiated push" path end to end.
In the four-layer IoT architecture it sits at the **network layer**: TCP/UDP carries the device-to-platform byte stream,
which the platform receives passively. In a real project you can use it as a template and drop in your own parsing logic
for a proprietary reporting protocol.

It fits:

- GPS/BeiDou terminals with their own reporting logic, and sensor gateways that push binary frames;
- any proprietary TCP/UDP protocol where the client connects to a passive server;
- validating the full "push → parse → store" path when no real hardware is available.

Before you start, two driver-specific concepts come first, since the config tables use them repeatedly:

- **Keyword**: the single byte right after the device name in the pushed packet, written in hexadecimal (e.g. `62`). One
  device can use different keywords to distinguish packet types; the driver uses it to decide which point a given frame
  should be parsed into.
- **Byte range (Start / End)**: the byte offsets at which data is sliced out of the packet. `start` is the start
  offset (inclusive), `end` is the end offset (exclusive); fixed-length numeric points only read a fixed number of bytes
  from `start`, while the `coordinate` string point uses the whole `start..end` range.

Every packet has a fixed structure: a 22-byte device name + a 1-byte keyword + a variable-length payload.

<ListeningVirtualDiagram lang="en" />

- The first 22 bytes are the device name; the driver parses it into a [device](../introduction/concepts/device) ID via
  `Long.parseLong` (it must map one-to-one to a device on the platform).
- The 23rd byte (offset 22) is the keyword, compared byte-for-byte against the point's `key`.
- The rest is the payload; each point slices a segment from the packet per its own `start`/`end`, and the **parse type
  is decided by the point name (pointName)**, not by the `type` attribute.

## Attribute Configuration

This driver **declares no device-level `driver-attribute`**—the listening ports are process-level configuration, and all
onboarding detail lives on the [point](../introduction/concepts/point).

**Driver name / code / type** (from `application.yml`):

- Driver name / code: `Listening Virtual TCP/UDP Driver` / `ListeningVirtualDriver`
- Type: `DRIVER_SERVER` (the driver acts as the listening server, passively receiving pushed data)

**Listening ports** are process-level configuration, not set on the point: TCP defaults to `6270`, overridable via the
`TCP_PORT` environment variable; UDP defaults to `6271`, overridable via `UDP_PORT`. At startup `initial()` launches one
thread per port to listen; packets received on either port go through the same parsing logic.

### Point Attributes (`point-attribute`)

Each collected [point](../introduction/concepts/point) carries these
four [attributes](../introduction/concepts/attribute-config), telling the driver which keyword to match and which
segment of the packet to slice (the parse type is decided by the point name, see the note below):

| Attribute  | code    | Type   | Default  | Remark                                                                       |
|------------|---------|--------|----------|------------------------------------------------------------------------------|
| Keyword    | `key`   | STRING | `62`     | Packet identification keyword, hexadecimal                                   |
| Start Byte | `start` | INT    | `0`      | Inclusive start byte offset                                                  |
| End Byte   | `end`   | INT    | `8`      | Exclusive end byte offset                                                    |
| Type       | `type`  | STRING | `string` | Required attribute; presence-checked only, not used to choose the parse type |

::: tip key is hexadecimal, compared byte-for-byte with the packet keyword
`key` holds the hexadecimal value of the packet's 23rd byte (e.g. the default `62`). When a frame arrives, the driver
only parses points whose keyword matches; points whose keyword differs produce no value for that frame. Different points
on the same device may share one `key` (extracting several fields from one frame) or use different keys (each frame
handled separately).
:::

::: warning The parse type is decided by the point name, not the `type` attribute
The driver chooses how to parse based on the **point name (pointName)** (see `NettyServerHandler.readConfiguredValue`),
and only these 6 names are supported: `altitude`→float (4 bytes), `speed`→double (8 bytes), `level`→long (8 bytes),
`direction`→int (4 bytes), `locked`→boolean (1 byte), `coordinate`→string (by `start..end`). The point name must be one
of these, otherwise the point parses to an empty string for that frame and collects no data. The `type` attribute is
only presence-checked in `validatePoint` (one of the four required); it is never read during parsing.
:::

## Troubleshooting

When onboarding this driver, almost every "no value collected" case maps to one of the items below. When a frame is
dropped the driver only `warn`s in its log—it does not return an error to the device—so check the driver log first.

::: warning The first 22 bytes of the packet must be the platform's numeric device ID
The driver parses the device ID from the first 22 bytes via `Long.parseLong` to match
a [device](../introduction/concepts/device) on the platform. When the device pushes data, it **must place the
corresponding numeric device ID into these 22 bytes**—if no number can be parsed (`deviceIdInvalid`) or no device
matches (`deviceMissing`), the frame is silently dropped without an error to the device. Create the device on the
platform first to get its ID, then configure it into the device firmware.
:::

::: warning start/end offsets are relative to the whole frame, not the payload
A point's `start`/`end` are byte offsets into the **whole frame**. The first 23 bytes are taken by the device name (22)
and keyword (1), so the first payload byte is at offset `23`, not `0`. To read from the start of the payload, `start`
must count from `23`; the default `start=0` lands inside the device name and reads the wrong value.
:::

- **Keyword mismatch**: a point's `key` must exactly equal the hexadecimal of the packet's 23rd byte (e.g. `62`). Points
  whose `key` does not match are silently skipped for that frame; first confirm which byte the device actually sends.
- **Byte order**: fixed-length numbers are read with Netty `ByteBuf`'s `getFloat/getDouble/getLong/getInt`, all *
  *big-endian**. If the device packs little-endian, the parsed numbers come out garbled—pack big-endian on the device
  side or adjust the parsing logic yourself.
- **Packet too short / offset out of bounds**: a frame shorter than 23 bytes (`payloadTooShort`), or a point whose
  `start+length` exceeds the actual packet length (`payloadOutOfBounds`), yields no value for that point on that frame.
  Note UDP datagrams are not fragmented while TCP may coalesce/split—this driver parses the received `ByteBuf` as-is and
  does no framing.
- **The device stays online regardless of whether data is pushed**: this driver **implements no protocol-level health
  decision** (it does not override `health()`), so the SDK's default reports the device as online unconditionally every
  `0/15 * * * * ?` and renews a `45`-second lease TTL. That means the device is **not** marked offline for "nothing
  pushed before the timeout"—online state is independent of data push. If you need "offline when nothing is pushed",
  override `health()` in the driver and return OFFLINE based on the last push time.
  See [device](../introduction/concepts/device) for the online-state mechanism.

## Landing It in IoT DC3

- **dc3.driver.code**: `ListeningVirtualDriver` (routing identifier, stable—do not change casually).
- **Read / write / subscribe capability** (aligned with the [driver capability matrix](./matrix)):
    - **Read**: `—`, no active polling. `schedule.read.enable=false`, and `read()` returns `null` directly; data is
      entirely push-triggered by the device. The config enables a `0/5 * * * * ?` `schedule.custom` callback, but the
      driver's `schedule()` is an empty implementation that does nothing (no periodic self-upkeep logic).
    - **Write**: `✓`, `write()` is implemented. Any successfully parsed frame (TCP or UDP) registers that device's most
      recent `Channel` into `DEVICE_CHANNEL_MAP` (registration happens in `NettyServerHandler.read()`, shared by both
      TCP and UDP); on a write command it looks up the active channel by `deviceId` and writes the value bytes back to
      the device (5-second flush timeout). If the channel is missing or inactive the write fails and returns `false`.
      Note: if the device's most recently registered channel is a connectionless UDP channel, the write-back usually
      fails—write-back relies on the connection-oriented TCP channel.
    - **Subscribe / report**: `✓`, this is the driver's primary capability—the device connects in and the driver
      receives pushes passively.

::: info This is a virtual / testing driver with a self-defined packet format
The driver as a whole works (listen, parse, write-back are all implemented), but it has no standard protocol layer: the
packet format (22+1+payload), the 6 fixed point names, and the big-endian numeric parsing are demonstration conventions
defined by the driver itself. To onboard a proprietary protocol in a real project, replace the parsing logic in
`NettyServerHandler` with your protocol's rules and treat this as an implementation template for a passive listening
driver.
:::

### Minimal Onboarding Example

Onboard a GPS terminal that pushes data over TCP:

1. Create a [device](../introduction/concepts/device) with `Listening Virtual TCP/UDP Driver` (this driver has no driver
   attributes, so the device needs no connection parameters), and note the numeric device ID the platform assigns.
2. Add a string [point](../introduction/concepts/point) to the [profile](../introduction/concepts/profile) bound to the
   device; the **point name must be one of the supported names** (here `coordinate`, which parses as a string), with
   point attributes `key=62`, `start=23`, `end=31`, `type=string`—i.e. match packets with keyword `62` and take 8 bytes
   from the start of the payload as a string. If the point name is not one of
   `altitude/speed/level/direction/locked/coordinate`, the driver collects no value.
3. Start the driver; have the device push "22-byte numeric device ID + 1-byte `0x62` + payload" to the driver's TCP port
   `6270`, and within seconds the parsed result appears in [point values](../introduction/concepts/point-value).

## Further Reading

- [Drivers Overview](./index) — what a driver is, registration and lifecycle, the three-tier origin of config
- [Driver Capability Matrix](./matrix) — read/write/subscribe across 28 drivers, to confirm this driver's positioning
- [Device Onboarding](../operation/device-onboarding) — a full device onboarding walkthrough
