---
title: TCP/UDP Driver
---

# TCP/UDP Driver

> **`dc3-driver-tcp-udp` connects any device speaking raw TCP/UDP sockets to IoT DC3**—for each [Point](../introduction/concepts/point) it sends a HEX command, reads back the raw bytes, then carves out the data by frame rules and converts it to a value.

Many serial-to-Ethernet modules, home-grown microcontrollers, and proprietary-protocol devices have no standard protocol stack—they simply "exchange a byte stream" over a TCP/UDP port: you send a hex command, they reply with a hex response. This driver is the generic base for such devices—it pulls in no third-party protocol library, talks to JDK sockets directly, and leaves "what command to send and how to parse the reply" entirely to [Attribute/Config](../introduction/concepts/attribute-config).

> **Protocol**: TCP is a connection-oriented reliable byte stream; UDP is connectionless datagrams. The driver picks one per device via the `protocol` attribute: TCP caches a long-lived connection per device, while UDP creates a temporary socket for each poll.
>
> **HEX command / frame**: what you exchange with the device is binary, written here uniformly as a hex string (e.g. `01 03 00 00 00 02`). The whole chunk of bytes the device returns is one frame; frame header/footer/offset/length are used to locate the real data inside that frame.

- **Driver name / code**: `TCP/UDP Raw Driver` / `TcpUdpDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively connects to the device and sends commands)

## Driver Config (device-level `driver-attribute`)

When onboarding a TCP/UDP [Device](../introduction/concepts/device), fill in these [Attributes](../introduction/concepts/attribute-config) on the device:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Protocol | `protocol` | STRING | `TCP` | TCP or UDP |
| Host | `host` | STRING | `localhost` | (device IP / hostname) |
| Port | `port` | INT | `502` | (device port) |
| Connect Timeout | `connectTimeout` | INT | `5000` | (TCP connect timeout, ms) |
| Read Timeout | `readTimeout` | INT | `3000` | (read-response timeout, ms) |
| Delimiter | `delimiter` | STRING | (empty) | Hex delimiter |

## Point Config (`point-attribute`)

Fill in on each [Point](../introduction/concepts/point) to describe "what to send and how to pull the value out of the reply":

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | (empty) | (HEX command sent on poll) |
| Receive Length | `receiveLength` | INT | `0` | 0 means use delimiter |
| Frame Header | `frameHeader` | STRING | (empty) | (frame header HEX) |
| Frame Footer | `frameFooter` | STRING | (empty) | (frame footer HEX) |
| Data Offset | `dataOffset` | INT | `0` | (byte offset of data within the frame) |
| Data Length | `dataLength` | INT | `0` | (data byte length) |
| Data Format | `dataFormat` | STRING | `HEX` | HEX/ASCII/INT16/UINT16/INT32/FLOAT |
| Byte Order | `byteOrder` | STRING | `BIG` | (byte order: BIG / LITTLE) |

::: tip dataFormat decides how the reply becomes a value
The driver carves out a byte slice from the reply using `dataOffset` + `dataLength`, then converts it per `dataFormat`: `HEX` returns the hex string as-is, `ASCII` decodes to text, `INT16/UINT16/INT32/FLOAT` parse as numbers (multi-byte values are governed by `byteOrder`, `BIG` for big-endian and `LITTLE` for little-endian). If `dataLength=0`, no slicing happens and the whole reply is returned as HEX.
:::

## Write Command Config (`command-attribute`)

For writable points, fill in on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | `${value}` | (write command template; `${value}` is replaced by the command value) |

On write, the `${value}` placeholder in the command is replaced with the actual value, then sent to the device as a HEX command.

## Collection & Health

- **Collection cycle**: default cron `0/30 * * * * ?` (one poll every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a `45-second` lease timeout. TCP devices are judged online by a quick connect attempt; UDP is connectionless and reported online by default—see [Device](../introduction/concepts/device) for the online-state mechanism.
- **Custom schedule**: `schedule.custom` is enabled by default with cron `0/5 * * * * ?` (fires every 5 seconds). This driver does not implement a custom periodic task (`schedule()` is an empty method), so the schedule performs no work in practice.

## Minimal Onboarding Example

Onboard a TCP device at `192.168.1.50:8899` and collect a 16-bit temperature (device replies `01 03 04 00 FA 12 34 ...`, temperature in bytes 3 and 4):

1. Create a [Device](../introduction/concepts/device) with `TCP/UDP Raw Driver`, set driver attributes `protocol=TCP`, `host=192.168.1.50`, `port=8899`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=INT`, `READ_ONLY`) to the device's bound [Profile](../introduction/concepts/profile), set point attributes `sendCommand=010300000001`, `dataOffset=3`, `dataLength=2`, `dataFormat=INT16`, `byteOrder=BIG`.
3. Start the driver; within 30 seconds the parsed value appears in [PointValue](../introduction/concepts/point-value) (`00FA` → `250`).

## Pitfalls

::: warning sendCommand / frame parsing are all HEX—keep out non-hex characters
`sendCommand`, `frameHeader`, and `frameFooter` are all parsed as hexadecimal (whitespace is ignored, so `01 03 00 00` is fine). Feeding non-hex characters (e.g. decimal `10` meant as a number) makes parsing fail or read wrong values. `dataFormat=ASCII` decodes the carved-out bytes into text—the command itself is still HEX.
:::

::: warning offset/length out of range falls back to the whole raw HEX
When `dataOffset + dataLength` exceeds the actual reply length, the driver does not error—it skips slicing and returns the whole reply as raw HEX. If a point value is a long HEX string when you expected a number, `dataOffset`/`dataLength` most likely don't match the device's actual reply structure.
:::

::: tip Connection failures have backoff—don't be alarmed by "won't connect for a bit"
After 3 consecutive TCP connect/read/write failures, the driver enters a 60-second backoff window and pauses reconnection; during it the device is reported offline, and it retries automatically once the window passes. One successful exchange after recovery resets the counter.
:::

## Further Reading

- [Driver](../introduction/concepts/driver) — the generic driver model and registration mechanism
- [Attribute/Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `protocol` / `sendCommand`
- [Device Onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — a standardized TCP-protocol example to contrast with this generic driver
