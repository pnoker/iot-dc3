---
title: TCP/UDP Driver
---

<script setup>
import TcpUdpDiagram from '../../.vitepress/theme/components/TcpUdpDiagram.vue'
</script>


# TCP/UDP Driver

`dc3-driver-tcp-udp` connects any device that "exchanges a raw byte stream over a TCP or UDP port" to IoT DC3: for
each [Point](../introduction/concepts/point) it sends a HEX command, reads back the raw bytes, then carves out the data
by frame rules and converts it to a value. After reading this you can collect from and write to proprietary devices that
have no standard protocol stack, and know where byte order, frame offset, and connection backoff go wrong.

## Protocol Background

TCP and UDP are the two transport-layer protocols of the [TCP/IP suite](../foundations/iot-protocols): TCP is
connection-oriented and provides a reliable, ordered byte stream; UDP is connectionless and delivers datagrams
best-effort. In the four-layer IoT architecture (perception → network → platform → application), they sit in the *
*network layer**—the common carrier beneath higher-level application protocols (MQTT, CoAP, Modbus TCP, and so on).

Many field devices run no standard protocol stack: serial-to-Ethernet modules, home-grown microcontrollers,
proprietary-protocol gateways—they often just "you send some bytes on a port, they reply with some bytes." Such devices
cannot be onboarded with any one protocol-specific driver. `dc3-driver-tcp-udp` is their generic base—it pulls in no
third-party protocol library, talks to JDK `Socket` / `DatagramSocket` directly, and leaves "what command to send and
how to parse the reply" entirely to [Attribute/Config](../introduction/concepts/attribute-config).

The behavioral difference between TCP and UDP in this driver matters:

- **TCP**: caches one long-lived connection per device (`tcpConnectMap`) to avoid redoing the three-way handshake every
  poll; the connection is invalidated and reconnected on disconnect or communication error.
- **UDP**: connectionless—each poll creates a fresh `DatagramSocket`, sends, waits for the reply, then closes it.

::: info HEX command and frame
What you exchange with the device is binary. This driver writes commands uniformly as a hex string (e.g.
`01 03 00 00 00 02`; whitespace is ignored). The whole chunk of bytes the device returns is one frame; `dataOffset` /
`dataLength` locate the real data inside the frame, and `dataFormat` decides how those bytes become a point value.
:::

- **Driver name / code**: `TCP/UDP Raw Driver` / `TcpUdpDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively connects to the device and sends commands)

## Attribute Config

Attributes come from the driver's `application.yml` in three layers: **driver attributes** go on
the [Device](../introduction/concepts/device) (one set of connection parameters per device), **point attributes** go on
each [Point](../introduction/concepts/point) (describe what this channel reads and how to parse it), and **command
attributes** go on a writable point's write command. The prose before each table explains what each attribute does.

### Driver Attributes (device-level `driver-attribute`)

`protocol` chooses TCP or UDP; `host` / `port` point at the device's network address (the port default `502` is just a
placeholder—change it to your device). `connectTimeout` is the TCP connect timeout and `readTimeout` is the read timeout
while waiting for the reply, both in milliseconds. `delimiter` is reserved for delimiter-based framing; the current
implementation frames mainly by `dataOffset`/`dataLength`.

| Attribute       | code             | Type   | Default     | Description               |
|-----------------|------------------|--------|-------------|---------------------------|
| Protocol        | `protocol`       | STRING | `TCP`       | TCP or UDP                |
| Host            | `host`           | STRING | `localhost` | device IP / hostname      |
| Port            | `port`           | INT    | `502`       | device port               |
| Connect Timeout | `connectTimeout` | INT    | `5000`      | TCP connect timeout, ms   |
| Read Timeout    | `readTimeout`    | INT    | `3000`      | read-response timeout, ms |
| Delimiter       | `delimiter`      | STRING | (empty)     | Hex delimiter             |

### Point Attributes (`point-attribute`)

`sendCommand` is the HEX command sent when this channel polls; on receiving the reply the driver carves out a byte slice
with `dataOffset` + `dataLength`, then converts it per `dataFormat`, with multi-byte values governed by `byteOrder`.
`frameHeader` / `frameFooter` / `receiveLength` are reserved for frame header/footer and fixed-length reads.

| Attribute      | code            | Type   | Default | Description                          |
|----------------|-----------------|--------|---------|--------------------------------------|
| Send Command   | `sendCommand`   | STRING | (empty) | HEX command sent on poll             |
| Receive Length | `receiveLength` | INT    | `0`     | 0 means use delimiter                |
| Frame Header   | `frameHeader`   | STRING | (empty) | frame header HEX                     |
| Frame Footer   | `frameFooter`   | STRING | (empty) | frame footer HEX                     |
| Data Offset    | `dataOffset`    | INT    | `0`     | byte offset of data within the frame |
| Data Length    | `dataLength`    | INT    | `0`     | data byte length                     |
| Data Format    | `dataFormat`    | STRING | `HEX`   | HEX/ASCII/INT16/UINT16/INT32/FLOAT   |
| Byte Order     | `byteOrder`     | STRING | `BIG`   | byte order: BIG / LITTLE             |

::: tip dataFormat decides how the reply becomes a value
The driver carves out a byte slice from the reply using `dataOffset` + `dataLength`, then converts it per `dataFormat`:
`HEX` returns the hex string as-is, `ASCII` decodes to text (trailing whitespace is trimmed), `INT16/UINT16/INT32/FLOAT`
parse as numbers (multi-byte values are governed by `byteOrder`, `BIG` for big-endian and `LITTLE` for little-endian).
`INT16/INT32/FLOAT` require the carved slice to be ≥2/≥4 bytes respectively; when too short it falls back to HEX. If
`dataLength=0`, no slicing happens and the whole reply is returned as HEX.
:::

The flow below strings together the key hops of "one poll" from sending the command to landing a value:

<TcpUdpDiagram lang="en" />

### Write Command Attributes (`command-attribute`)

A writable point's write command takes a `sendCommand` template with a `${value}` placeholder. On write the driver
replaces `${value}` with the actual command value, then sends it to the device as a HEX command (TCP reuses the
long-lived connection, UDP creates a temporary socket).

| Attribute    | code          | Type   | Default    | Description                                                         |
|--------------|---------------|--------|------------|---------------------------------------------------------------------|
| Send Command | `sendCommand` | STRING | `${value}` | write command template; `${value}` is replaced by the command value |

::: warning The write path reads `sendCommand` from the point attribute
In the source, `write()` takes `sendCommand` from the **point attribute** (`pointConfig`), not the command attribute. If
a writable point has no `sendCommand` set in its point attributes, the write returns failure because the command is
empty. The `command-attribute` `${value}` template applies in the `execute()` rendering flow.
:::

## Troubleshooting

- **The point value is a long HEX string when you expected a number**: usually `dataOffset` + `dataLength` exceeds the
  actual reply length, or `dataLength=0`. On out-of-range, the driver does **not** error—it skips slicing and returns
  the whole reply as raw HEX. Capture a real reply frame first, count which byte the target data starts at and how many
  bytes it spans, then match `dataOffset` / `dataLength`.
- **Sign/magnitude clearly wrong**: `byteOrder` does not match the device for a multi-byte value. Use `BIG` for
  big-endian devices, `LITTLE` for little-endian; `UINT16` and `INT16` differ by a sign when the high bit is 1—pick the
  format that matches the device's semantics.
- **`sendCommand` fails to parse or reads wrong values**: `sendCommand` / `frameHeader` / `frameFooter` are all parsed
  as hexadecimal (whitespace is ignored, so `01 03 00 00` is fine). Feeding non-hex characters (e.g. decimal `10` meant
  as a number) makes parsing fail. `dataFormat=ASCII` only affects how reply bytes decode to text—the command itself
  must still be HEX.
- **Device stays offline / temporarily won't connect**: after 3 consecutive TCP connect or read/write failures the
  driver enters a **60-second backoff window** and pauses reconnection; during it the device is reported offline, and it
  retries automatically once the window passes—one successful exchange resets the counter. On a brief offline, first
  check whether you're inside the backoff window, then check `host`/`port`/firewall.
- **Read timeout**: `readTimeout` defaults to 3000ms. A slow reply or UDP packet loss triggers a read timeout (UDP
  throws `SocketTimeoutException` when no reply arrives). Raise `readTimeout` as needed, and for UDP confirm the peer
  actually sends a reply.
- **A UDP device is always reported online**: UDP is connectionless, so `health()` reports UDP online by default (no
  probe). "Online" does not mean data flows—still check whether the point has fresh values landing.

## How It Works in IoT DC3

- **`dc3.driver.code`**: `TcpUdpDriver`—a stable routing identifier the platform uses to dispatch commands to this
  driver; do not change it casually.
- **Read / write / subscribe**: this driver's `read()` actively sends a command to collect, and `write()` renders a
  command to write—both are implemented; it offers **no subscribe**—`schedule()` is an empty method with no custom
  periodic task. This matches "read ✓ / write ✓ / subscribe —" for this driver in
  the [driver capability matrix](./matrix).
- **Collection & health**: default collection cron `0/30 * * * * ?` (one poll every 30 seconds); device health-check
  cron `0/15 * * * * ?` with a `45-second` lease timeout. TCP devices are judged online by cached connection state or a
  quick connect attempt; UDP is reported online by default.

::: info Custom schedule is enabled but does nothing
`schedule.custom` is enabled by default with cron `0/5 * * * * ?`, but `schedule()` is an empty method—this driver
implements no custom periodic task, so the schedule performs no work. It is an intentional placeholder and does not
affect normal collection.
:::

### Minimal Onboarding Example

Onboard a TCP device at `192.168.1.50:8899` and collect a 16-bit temperature (device replies `01 03 04 00 FA 12 34 ...`,
temperature in bytes 3 and 4):

1. Create a [Device](../introduction/concepts/device) with `TCP/UDP Raw Driver`, set driver attributes `protocol=TCP`,
   `host=192.168.1.50`, `port=8899`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=INT`, `READ_ONLY`) to the device's
   bound [Profile](../introduction/concepts/profile), set point attributes `sendCommand=010300000001`, `dataOffset=3`,
   `dataLength=2`, `dataFormat=INT16`, `byteOrder=BIG`.
3. Start the driver; within 30 seconds the parsed value appears in [PointValue](../introduction/concepts/point-value) (
   `00FA` → `250`).

## Further Reading

- [Drivers Overview](./index) — the panorama and grouping of all 28 protocol drivers
- [Driver Capability Matrix](./matrix) — read/write/subscribe capability across drivers
- [Device Onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [IoT Protocols & Wireless Networks](../foundations/iot-protocols) — the network layer TCP/UDP sits in and its relation
  to higher application protocols
- [Modbus TCP Driver](./modbus-tcp) — a standardized TCP-protocol example to contrast with this generic driver
