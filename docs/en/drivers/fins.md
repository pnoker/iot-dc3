---
title: FINS Driver
---

# FINS Driver

> **`dc3-driver-fins` onboards Omron PLCs into IoT DC3 over the FINS protocol**——it periodically reads PLC memory-area values by word address, and supports commands that write values back to memory areas.

FINS (Factory Interface Network Service) is the native communication protocol of Omron PLCs, widely used across the CP/CJ/CS series. This driver acts as a FINS client, actively connecting to the PLC over TCP, then reading and writing per the memory area and word address configured on each [Point](../introduction/concepts/point). It builds FINS frames by hand (no third-party protocol library) and supports the D, W, H, and C memory areas.

- **Driver name / code**: `Omron FINS Driver` / `FinsDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the PLC)

::: tip A few FINS concepts first
**Memory Area**: a region of PLC data partitioned by purpose——`D` (data memory, the most common), `W` (work), `H` (holding), `C` (counter). **Word Address**: an offset within a memory area in units of one "word" (16 bits), e.g. `D100` is the 100th word of the D area. **Node/Unit number**: the source/destination addresses used to reach a PLC on the FINS network; for a single direct connection these usually stay at their defaults.
:::

## Driver configuration (device-level `driver-attribute`)

When onboarding a FINS PLC, fill these [Attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `127.0.0.1` | PLC host address |
| Port | `port` | INT | `9600` | FINS port (standard 9600) |
| Protocol | `protocol` | STRING | `TCP` | Transport protocol |
| Source Node | `sourceNode` | INT | `1` | FINS source node number |
| Dest Node | `destNode` | INT | `2` | FINS destination node number |
| Source Unit | `sourceUnit` | INT | `0` | FINS source unit number |
| Dest Unit | `destUnit` | INT | `0` | FINS destination unit number |
| Timeout | `timeout` | INT | `5000` | Request timeout (milliseconds) |

## Point configuration (`point-attribute`)

Fill these on each acquisition [Point](../introduction/concepts/point):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Memory Area | `memoryArea` | STRING | `D` | Memory area, `D`/`W`/`H`/`C` |
| Address | `address` | INT | `0` | Word address within the memory area |
| Data Type | `dataType` | STRING | `UINT16` | `INT16`/`UINT16`/`INT32`/`UINT32`/`FLOAT`/`STRING`/`BCD` |
| Bit Position | `bitPosition` | INT | `0` | Bit offset within the word (unused on the current read path; treated as `0`) |

::: warning The current read path only supports 16-bit types; `dataType` does not decide how many words are read
A read always fetches exactly **1 word (2 bytes)**, regardless of `dataType`. So **only `INT16`/`UINT16` read back correctly**; `INT32`/`UINT32`/`FLOAT`/`STRING`/`BCD` are protocol semantics whose implementation is still pending——they need 4 bytes or more, and decoding a 2-byte big-endian read triggers a BufferUnderflow, so no value is actually produced today. `STRING`/`BCD` likewise only receive 2 bytes. The driver decodes the returned bytes Big-Endian; the Point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) should match the `dataType` set here.
:::

## Write command configuration (`command-attribute`)

Writable points additionally need these on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Memory Area | `memoryArea` | STRING | `D` | Memory area, `D`/`W`/`H`/`C` |
| Address | `address` | INT | `0` | Word address within the memory area |
| Data Type | `dataType` | STRING | `UINT16` | Data type of the written value |

## Acquisition and health

- **Acquisition cycle**: default cron `0/30 * * * * ?` (reads once every 30 seconds).
- **Custom task**: default cron `0/5 * * * * ?` (the FINS driver currently uses no custom task; this schedule slot is reserved).
- **Health / online**: the device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`——the driver decides online status by whether the TCP connection is alive; for the online state mechanism see [Device](../introduction/concepts/device).

## Minimal onboarding example

Onboard an Omron PLC at IP `192.168.1.20:9600` and acquire one 16-bit integer at `D100`:

1. Create a [Device](../introduction/concepts/device) with `Omron FINS Driver`, set driver attributes `host=192.168.1.20` and `port=9600`, and leave the rest (`protocol`, node/unit numbers, `timeout`) at their defaults.
2. Add a [Point](../introduction/concepts/point) (`pointTypeFlag=INT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, with point attributes `memoryArea=D`, `address=100`, `dataType=INT16`.
3. Start the driver; within 30 seconds the `D100` value appears in [PointValue](../introduction/concepts/point-value).

## Pitfalls

::: warning address is a word address, not a region-prefixed string
`address` takes only the numeric offset within the memory area. To read Omron's familiar `D100`, set `memoryArea=D` and `address=100`——do **not** put `D100` as a whole into `address`. The area is specified separately by `memoryArea`.
:::

::: warning 32-bit writes are currently encoded as integers
The write command parses `INT32`/`UINT32`/`FLOAT` all via `Integer.parseInt` before writing 4 big-endian bytes. That means a `FLOAT` write expects the integer form of the bit pattern, not decimal text like `12.5`——confirm the format of the dispatched value matches what the PLC expects before writing floats.
:::

::: tip One driver instance can serve multiple PLCs
A single FINS driver process can serve multiple devices, each holding its own TCP connection (cached by device ID). Multiple PLCs are distinguished by their own `host` and `destNode`.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute & Config](../introduction/concepts/attribute-config) — where attributes like `host` / `memoryArea` come from across the three layers
- [Device Onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — another common industrial TCP protocol
