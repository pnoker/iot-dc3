---
title: Serial Driver
---

<script setup>
import SerialDiagram from '../../.vitepress/theme/components/SerialDiagram.vue'
</script>


# Serial Driver

`dc3-driver-serial` connects RS232/RS485/RS422 serial devices that speak proprietary frames to IoT DC3: acting as the
serial master, it periodically sends the HEX command configured on each [Point](../introduction/concepts/point), reads
back the raw bytes, parses them into a value by frame header/footer, checksum, data offset and format, and supports
commands that write values to the device. After this page you can configure the line parameters and frame-parsing rules
for a "send some bytes, get some bytes back" device, and know where to look when it won't connect.

## Protocol background

Serial is the plainest yet most universal way to wire up the industrial floor. **RS232** point-to-point and *
*RS485/RS422** buses are widely used by meters, transmitters, energy meters, barcode scanners, PLC serial modules, and
more. It only defines the physical and link layers — what voltage levels, how many wires, what baud rate to send and
receive bytes at — and **says nothing about what the bytes mean**. Many devices do not speak a standard protocol like
Modbus but a vendor-proprietary frame: send a fixed byte string, get a fixed-structure byte string back.

In the four-layer IoT architecture, serial belongs to the **network layer**: it solves how a field device and the
polling master exchange bytes — which signal, which line parameters — not what those bytes mean to the business. This
driver turns that raw byte channel into a configurable protocol adapter: you describe "what to send" with a HEX
command, "how to slice the response" with frame header/footer plus offset/length, and "how to verify and decode the
sliced bytes" with a checksum type and data format. For the general network-layer background on addressing, byte order
and polling, see [IoT network layer: industrial buses and protocols](../foundations/fieldbus).

- **Driver name / code**: `Serial Port Driver` / `SerialDriver`
- **Type**: `DRIVER_CLIENT` (actively opens the port and polls the device)
- **Underlying library**: jSerialComm (one serial connection per [Device](../introduction/concepts/device), cached by
  device id)

::: tip Two terms first
**HEX command**: a byte string written in hexadecimal, e.g. `01 03 00 00 00 0A C5 CD`; the spaces are only for
readability and map to the actual bytes sent (the driver strips spaces and `-` before parsing). **Frame**: the full
chunk of bytes the device replies with, usually a header, a data region, an optional checksum, and a footer; this driver
uses the header/footer plus offset/length to "cut" the real data region out of the response, then decodes it.
:::

## Attribute configuration

Serial configuration comes in three layers: **driver attributes** (`driver-attribute`, device-level) describe how the
serial port is opened (line parameters); **point attributes** (`point-attribute`) describe what each collected point
sends, how to slice the response, and what format to decode in; **command attributes** (`command-attribute`) describe
where a writable point writes. The defaults and descriptions in the three tables below come from the driver
`application.yml`; for the three-layer origin of attributes
see [Attribute and Config](../introduction/concepts/attribute-config).

### Driver attributes (device-level `driver-attribute`)

When onboarding a serial device, fill in these line parameters on the [Device](../introduction/concepts/device). They
are passed verbatim to jSerialComm to open the port, so they **must match the device's serial settings one by one** —
serial has no handshake negotiation, and any mismatch just yields garbage or a timeout:

| Attribute   | code       | Type   | Default        | Description                                                                                |
|-------------|------------|--------|----------------|--------------------------------------------------------------------------------------------|
| Serial Port | `port`     | STRING | `/dev/ttyUSB0` | Serial port device path (e.g. /dev/ttyUSB0, COM3)                                          |
| Baud Rate   | `baudRate` | INT    | `9600`         | Baud rate (1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200)                            |
| Data Bits   | `dataBits` | INT    | `8`            | Data bits (5, 6, 7, 8)                                                                     |
| Stop Bits   | `stopBits` | INT    | `1`            | Stop bits (1, 2)                                                                           |
| Parity      | `parity`   | INT    | `0`            | Parity (0=None, 1=Odd, 2=Even)                                                             |
| Timeout     | `timeout`  | INT    | `1000`         | Read timeout in milliseconds; returns the bytes received so far if still short at deadline |

### Point attributes (`point-attribute`)

On each collected [Point](../introduction/concepts/point), set three groups: what to send (`sendCommand`/
`receiveLength`), how to slice the response (`frameHeader`/`frameFooter`/`dataOffset`/`dataLength`/`checksumType`), and
how to decode (`dataFormat`/`byteOrder`). Only `sendCommand` is required; the rest fall back to defaults when left
blank:

| Attribute      | code            | Type   | Default | Description                                                                                               |
|----------------|-----------------|--------|---------|-----------------------------------------------------------------------------------------------------------|
| Send Command   | `sendCommand`   | STRING | (empty) | HEX command to send (e.g. `01 03 00 00 00 0A C5 CD`), required                                            |
| Receive Length | `receiveLength` | INT    | `0`     | Expected response length; 0=read until timeout/inter-frame gap, >0=read exactly this many bytes           |
| Frame Header   | `frameHeader`   | STRING | (empty) | Frame header in HEX (e.g. `01 03`), used to locate the frame start in the response                        |
| Frame Footer   | `frameFooter`   | STRING | (empty) | Frame footer in HEX (e.g. `0D 0A`), used to locate the frame end                                          |
| Data Offset    | `dataOffset`    | INT    | `0`     | Data region start offset (**relative to after the frame header**; relative to frame start when no header) |
| Data Length    | `dataLength`    | INT    | `0`     | Data region length in bytes (0=until footer/checksum region)                                              |
| Checksum Type  | `checksumType`  | STRING | `NONE`  | Response checksum type: NONE, CRC16, XOR                                                                  |
| Data Format    | `dataFormat`    | STRING | `HEX`   | Data format: HEX, ASCII, BINARY, FLOAT                                                                    |
| Byte Order     | `byteOrder`     | STRING | `BIG`   | Byte order: BIG, LITTLE                                                                                   |

::: tip Parsing order: locate the frame, verify, then decode
After receiving a response, the driver runs `parseResponse` → `SerialFrameParser.parse` in order: ① locate the frame
start via `frameHeader` (`indexOf`, error if not found) and the frame end via `frameFooter` searching from after the
header (`lastIndexOf`); ② take the data region start as "after the header plus `dataOffset`", and reserve a checksum
region before the footer per `checksumType` (CRC16 takes 2 bytes, XOR takes 1); ③ if `checksumType≠NONE`, recompute the
checksum over the bytes from after the header to the end of the data region and compare it with the checksum bytes in
the response, erroring on mismatch; ④ slice the data region by `dataLength` (0=up to the checksum region) and decode by
`dataFormat`+`byteOrder`. The Point's data type (the `pointTypeFlag` of the [Point](../introduction/concepts/point))
should match `dataFormat`: under `BINARY`/`FLOAT`, 1/2/4/8-byte regions are assembled into integers or floats per
`byteOrder` (`FLOAT` uses 4 bytes single precision, 8 bytes double precision); a length not in 1/2/4/8 falls back to a
HEX string.
:::

### Command attributes (`command-attribute`)

A writable Point sets these on the write command:

| Attribute    | code          | Type   | Default    | Description                                            |
|--------------|---------------|--------|------------|--------------------------------------------------------|
| Send Command | `sendCommand` | STRING | `${value}` | HEX command template with the `${value}` placeholder   |
| Byte Order   | `byteOrder`   | STRING | `BIG`      | Byte order for encoding the written value: BIG, LITTLE |

On write, the driver renders the command param into the `${value}` slot of the `sendCommand` template, parses the whole
result as HEX into bytes, and sends the frame without reading a response.

::: warning The write byteOrder is currently not applied during encoding
`command-attribute` lists `byteOrder`, but `write()` only does a string replace `sendCommand.replace("${value}", value)`
and then parses the whole result as HEX — `${value}` must already be valid HEX text, and `byteOrder` does no endianness
conversion on it. To write a multi-byte numeric value, encode it into a correctly byte-ordered HEX string upstream and
pass that as the command param.
:::

### Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one polling round over all points every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds`; the
  driver treats a device as online based on whether its serial port is open (`SerialPort.isOpen()`) —
  see [Device](../introduction/concepts/device) for the online-status mechanism.
- **Custom task**: the yml has a `custom` schedule (`0/5 * * * * ?`), but the driver's `schedule()` is an empty
  implementation — serial drivers need no custom periodic task.

## Troubleshooting

::: warning You must compute the checksum bytes in sendCommand yourself
This driver **does not append a CRC/checksum for you** — `sendCommand` is sent verbatim as a single byte string, so if
the device requires a Modbus CRC or another checksum, you must compute it and write it into the command. Get it wrong
and the device either does not reply or returns an exception frame, which shows up as an empty response or a parse
error. Note that `checksumType` only verifies the **response** and never rewrites the command you send.
:::

::: warning dataOffset counts from "after the header", not byte 0 of the whole frame
In the source the data region start is `start + dataOffset`, where `start` is the position **after the header** (the
header length is skipped when `frameHeader` is set). So: with a header `01 03`, `dataOffset` is counted from the byte
after `01 03`; without a header, `start=0` and only then does `dataOffset` count from the frame start. Counting the
header into the offset skips extra bytes and decodes wrong data. The safest approach is either-or: use `frameHeader` to
locate and fill `dataOffset` as the offset after the header, or omit `frameHeader` and count `dataOffset` straight from
byte 0.
:::

::: warning A missing frame header/footer errors out
If `frameHeader` is set but the bytes are not present in the response, `parse()` throws `Frame header not found`; the
same goes for `frameFooter`. A truncated response (`timeout` too short, or `receiveLength` set larger than what arrives)
likewise causes a missing header/footer or an empty data region (`No data region in serial frame`). First set
`dataFormat=HEX` to dump the raw response, confirm the frame structure matches your config, then tighten each setting.
:::

::: warning A wrong checksum type makes good responses fail verification
With `checksumType=CRC16` (this driver uses Modbus CRC16, polynomial `0xA001`, low byte first) or `XOR`, the driver
recomputes the checksum over the bytes "from after the header to the end of the data region" and compares it with the
checksum bytes in the response, throwing `checksum mismatch` if they differ. If the device's checksum range or algorithm
differs (e.g. covers the header, or uses another polynomial), set it back to `NONE` and verify upstream, so good data is
not dropped as bad. An unrecognized checksum name (not NONE/CRC16/XOR) is treated as `NONE`.
:::

::: tip A serial port is exclusive — watch for contention and permissions
A single driver process caches an independent serial connection per device id and can serve several devices on different
`port` paths at once; but one physical port can only be opened by one process at a time. Make sure the `port` is not
held by a serial terminal tool or another driver process; on Linux the run user also needs read/write permission on
`/dev/ttyUSB*` (commonly by adding the user to the `dialout` group). On a read/write failure the driver actively closes
and removes the connection (`invalidateConnector`), reopening it on the next round.
:::

::: tip Each device on one RS485 bus gets its own Device
Multiple devices on the same physical port (an RS485 bus) each need their own [Device](../introduction/concepts/device)
pointing at the **same `port`**, distinguished by their own `sendCommand` (with different station addresses); the driver
polls them point by point. Do not assign different `port` values to different devices on the same bus.
:::

## How it lands in IoT DC3

However proprietary the underlying frame, in the platform everything converges to
the [PointValue](../introduction/concepts/point-value) of a single [Point](../introduction/concepts/point). The serial
driver registers with `dc3.driver.code = SerialDriver`, a stable routing identifier the platform uses to dispatch
read/write commands to this driver.

<SerialDiagram lang="en" />

Per the [driver capability matrix](./matrix), this driver's capabilities are:

| Capability | Supported | Notes                                                                                                    |
|------------|-----------|----------------------------------------------------------------------------------------------------------|
| Read       | ✓         | `read()` sends `sendCommand`, reads the response, slices the data region by frame structure and decodes  |
| Write      | ✓         | `write()` renders the `${value}` template and sends the frame (no response read, no byte-order encoding) |
| Subscribe  | —         | Master/slave polling model; devices do not push, polled on the collection cycle                          |

::: info Implementation status: available
`SerialDriverCustomServiceImpl`'s `initial()`/`read()`/`write()`/`health()`/`event()`, the frame parser (
`SerialFrameParser`), and connection management (`SerialPortConnection`, on jSerialComm) are all fully implemented — not
a skeleton. The read path supports HEX/ASCII/BINARY/FLOAT decoding and CRC16/XOR/NONE response checksum verification;
connections are cached per device, and a device metadata UPDATE/DELETE event destroys the old connection (
`connectMap.remove` then `close()`).
:::

## Minimal onboarding example

Onboard a temperature transmitter on `/dev/ttyUSB0` at 9600-8-N-1 that, after the query command, responds with
`01 03 02 <2 data bytes> <2 CRC bytes>`:

1. Create a [Device](../introduction/concepts/device) using `Serial Port Driver`, and set the driver attributes
   `port=/dev/ttyUSB0`, `baudRate=9600`, `dataBits=8`, `stopBits=1`, `parity=0`.
2. Add a temperature [Point](../introduction/concepts/point) (`READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes
   `sendCommand=01 03 00 00 00 01 84 0A`, `dataOffset=3`, `dataLength=2`, `dataFormat=BINARY`, `byteOrder=BIG` (no
   `frameHeader`, so `dataOffset` counts from byte 0 of the response, skipping the three bytes `01 03 02` to land
   exactly on the data region).
3. Start the driver, and within 30 seconds you will see the collected value
   in [PointValue](../introduction/concepts/point-value).

::: tip For standard Modbus RTU, prefer the dedicated driver
This driver is a "generic serial pass-through" — best for proprietary frames or when you need byte-level control of the
frame. If the device speaks standard Modbus RTU (regular function codes, CRC, addressing),
the [Modbus RTU Driver](./modbus-rtu) is far less work: it builds the frame, appends the CRC, and addresses by function
code for you, with no hand-written `sendCommand`.
:::

## Further reading

- [Driver overview](./index) — entry point to all protocol drivers and selection
- [Driver capability matrix](./matrix) — quick reference of read/write/subscribe capabilities
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Industrial buses and protocols](../foundations/fieldbus) — network layer: the general background on addressing, byte
  order, and polling
- [Modbus RTU Driver](./modbus-rtu) — the serial driver that speaks the standard Modbus protocol
