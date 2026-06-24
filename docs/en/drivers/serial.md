---
title: Serial Driver
---

# Serial Driver

> **`dc3-driver-serial` connects RS232/RS485/RS422 serial devices to IoT DC3** — it periodically sends the HEX command configured on each [Point](../introduction/concepts/point), reads back the raw bytes, parses them into a value by frame header/footer, checksum, data offset and format, and supports commands that write values to the device.

Serial is the plainest yet most universal way to wire up the industrial floor: RS232 point-to-point, RS485/RS422 buses are widely used by meters, transmitters, barcode scanners, PLC serial modules, and more. Many devices do not speak a standard protocol but a proprietary "send some bytes, get some bytes back" frame. This driver acts as the serial master: it opens a serial port via jSerialComm, caches one connection per device, sends the HEX command you configured for each point, then slices the data region out of the response by its frame structure and decodes it in the format you specify (HEX/ASCII/BINARY/FLOAT).

- **Driver name / code**: `Serial Port Driver` / `SerialDriver`
- **Type**: `DRIVER_CLIENT` (actively opens the port and polls the device)

::: tip Two terms first
**HEX command**: a byte string written in hexadecimal, e.g. `01 03 00 00 00 0A C5 CD`; the spaces are only for readability and map to the actual bytes sent. **Frame**: the full chunk of bytes the device replies with, usually made up of a header, a data region, an optional checksum, and a footer; this driver uses the header/footer plus offset/length to "cut" the real data region out of the response.
:::

## Driver configuration (device-level `driver-attribute`)

When onboarding a serial device, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device); they decide how the serial port is opened (line parameters):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Serial Port | `port` | STRING | `/dev/ttyUSB0` | Serial port device path |
| Baud Rate | `baudRate` | INT | `9600` | Baud rate (1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200) |
| Data Bits | `dataBits` | INT | `8` | Data bits (5, 6, 7, 8) |
| Stop Bits | `stopBits` | INT | `1` | Stop bits (1, 2) |
| Parity | `parity` | INT | `0` | Parity (0=None, 1=Odd, 2=Even) |
| Timeout | `timeout` | INT | `1000` | Read timeout in milliseconds |

## Point configuration (`point-attribute`)

On each collected [Point](../introduction/concepts/point), set what to send, how to slice the response, and what format to decode in.

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | (empty) | HEX command to send (e.g. `01 03 00 00 00 0A C5 CD`) |
| Receive Length | `receiveLength` | INT | `0` | Expected response length in bytes (0=auto detect) |
| Frame Header | `frameHeader` | STRING | (empty) | Frame header in HEX (e.g. `01 03`) |
| Frame Footer | `frameFooter` | STRING | (empty) | Frame footer in HEX (e.g. `0D 0A`) |
| Data Offset | `dataOffset` | INT | `0` | Data region offset from frame start |
| Data Length | `dataLength` | INT | `0` | Data region length in bytes (0=until frame footer) |
| Checksum Type | `checksumType` | STRING | `NONE` | Checksum type: NONE, CRC16, XOR |
| Data Format | `dataFormat` | STRING | `HEX` | Data format: HEX, ASCII, BINARY, FLOAT |
| Byte Order | `byteOrder` | STRING | `BIG` | Byte order: BIG, LITTLE |

::: tip Parsing order: slice the frame first, then decode
After receiving a response, the driver first locates the frame using `frameHeader`/`frameFooter`, slices the data region by `dataOffset`/`dataLength` and verifies it by `checksumType`, then decodes it into a point value by `dataFormat` + `byteOrder`. The Point's data type (the `pointTypeFlag` of the [Point](../introduction/concepts/point)) should match `dataFormat`: 2/4/8-byte quantities are assembled into integers or floats according to `byteOrder`, and `FLOAT` uses 4 bytes (single precision) or 8 bytes (double precision).
:::

## Write command configuration (`command-attribute`)

A writable Point sets these on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | `${value}` | HEX command template with the `${value}` placeholder |
| Byte Order | `byteOrder` | STRING | `BIG` | Byte order for encoding the written value: BIG, LITTLE |

On write, the driver renders the command param into the `${value}` slot of the `sendCommand` template, converts it to bytes, and sends the whole frame.

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one polling round every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds`; the driver treats a device as online based on whether its serial port is open — see [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

Onboard a Modbus RTU-style temperature transmitter on `/dev/ttyUSB0` at 9600-8-N-1 (slave 1, reads a holding register, responds with `01 03 02 <2 data bytes> <2 CRC bytes>`):

1. Create a [Device](../introduction/concepts/device) using `Serial Port Driver`, and set the driver attributes `port=/dev/ttyUSB0`, `baudRate=9600`, `dataBits=8`, `stopBits=1`, `parity=0`.
2. Add a temperature [Point](../introduction/concepts/point) (`READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `sendCommand=01 03 00 00 00 01 84 0A`, `frameHeader=01 03`, `dataOffset=3`, `dataLength=2`, `dataFormat=BINARY`, `byteOrder=BIG`.
3. Start the driver, and within 30 seconds you will see the collected value in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning You must compute the CRC in sendCommand yourself
This driver does not append a Modbus CRC for you — `sendCommand` is sent verbatim as a single byte string, so the checksum bytes must be computed and written in by you (the `84 0A` above is exactly that CRC). A wrong CRC means the device either does not reply or returns an exception frame. `checksumType` only verifies the **response** and never rewrites the command you send.
:::

::: warning dataOffset counts from frame start — don't treat the header as data
`dataOffset`/`dataLength` are byte offsets relative to the start of the whole frame, not relative to the data region. In the Modbus RTU response `01 03 02 ...`, the payload starts at the 4th byte, so `dataOffset=3` and `dataLength=2`. Forgetting the header makes you decode the address/function code as data.
:::

::: tip One driver instance can serve multiple serial devices
A single driver process caches an independent serial connection per device and can serve several devices on different `port` paths at once. But multiple devices on the same physical port (an RS485 bus) each need their own [Device](../introduction/concepts/device), distinguished by their own `sendCommand` (with different station addresses); the driver polls them point by point.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute and Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `port` / `sendCommand`
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Modbus RTU Driver](./modbus-rtu) — the serial driver that speaks the standard Modbus protocol
