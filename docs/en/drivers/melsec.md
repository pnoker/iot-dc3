---
title: Melsec Driver
---

# Melsec Driver

`dc3-driver-melsec` onboards Mitsubishi PLCs into IoT DC3 over the MC protocol: it acts as an MC client, actively
connects to the PLC, periodically reads by the device address configured on
each [Point](../introduction/concepts/point), and supports commands that write values back to devices. After reading
this page you can fill in the driver / point attributes, onboard a Mitsubishi PLC, and know where to look when it won't
connect.

## Protocol background

The MC protocol (MELSEC Communication) is the native communication protocol of Mitsubishi Electric PLCs, widely used
across the A, QnA, Q/L, and iQ-R series. On the plant floor a Mitsubishi PLC exposes an MC server port through its
Ethernet module or built-in CPU port; an upstream system connects as an MC client and reads/writes data units in PLC
memory by their **device address** (e.g. `D100`, `M0`) — data registers hold process parameters, internal relays hold
logic state, input/output relays map to field IO.

In the four-layer IoT architecture the MC protocol sits on the wired-industrial side of the **network layer**: it is
the "last mile" language contract between a PLC and upstream systems, governing how bytes are laid out, how devices are
addressed, and the request-response timing of each exchange. Like Siemens S7 and Omron FINS, it is a vendor-proprietary
master/slave protocol — the PLC never reports unprompted; the master must ask. To see where it sits in the protocol
landscape and why PLC vendors' protocols are mutually incompatible,
read [IoT network layer: industrial buses and protocols](../foundations/fieldbus).

The driver is built on the `McPLC` of the `iot-communication` protocol library, automatically selecting the matching
word width and codec by the point's data type when reading or writing.

- **Driver name / code**: `Mitsubishi Melsec Driver` / `MelsecDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the PLC)

::: tip A few MC concepts first
**Device (also called memory address)**: a data unit in a Mitsubishi PLC partitioned by purpose — e.g. `D` (data
register, the most common), `M` (internal relay), `X` (input relay), `W` (link register). **Device address**: a complete
address string made of an area prefix plus a number, e.g. `D100`, `M0`, `X10`, `W200` — the area and number are written
together, not split into two fields. **PLC Series**: the MC frame format differs slightly between series, so choose
`A` / `QnA` / `Q_L` / `IQ_R` to match the actual PLC.
:::

## Attribute configuration

Melsec driver configuration has two layers: **driver attributes** describe "which PLC to connect to" (device-level, one
set per device), and **point attributes** describe "which device to read" (point-level, one set per point). Both layers
are declared in the driver's `application.yml`; you fill the values in the console at onboarding time.

### Driver attributes (device-level `driver-attribute`)

`host` / `port` decide which PLC's MC server port the TCP connection targets; `series` decides which framing the MC
frame uses and must match the real PLC series. When onboarding a Melsec PLC, fill
these [Attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute  | code     | Type   | Default        | Description                        |
|------------|----------|--------|----------------|------------------------------------|
| Host       | `host`   | STRING | `192.168.0.20` | PLC host address (Ip)              |
| Port       | `port`   | INT    | `6000`         | MC service port                    |
| PLC Series | `series` | STRING | `QnA`          | PLC series, `A`/`QnA`/`Q_L`/`IQ_R` |

### Point attributes (`point-attribute`)

`address` specifies the device to read/write (whole-string notation, area + number written together); `length` is only
used for the string type — the byte length of the string to read. Fill these on
each [Point](../introduction/concepts/point):

| Attribute      | code      | Type   | Default | Description                                        |
|----------------|-----------|--------|---------|----------------------------------------------------|
| Device Address | `address` | STRING | `D100`  | Device address (`D100`, `M0`, `X10`, `W200`, etc.) |
| String Length  | `length`  | INT    | `0`     | String read length (`0` for non-string types)      |

::: tip The data type decides how many words to read and how to decode them
The driver picks the read/write width and codec automatically from the point's data
type ([Point](../introduction/concepts/point)'s `pointTypeFlag`): `BOOLEAN` reads/writes a bit, `BYTE` reads/writes 8
bits, `SHORT` reads/writes 16 bits (int16), `INT`/`FLOAT` read/write 32 bits, `LONG`/`DOUBLE` read/write 64 bits,
`STRING` reads/writes a string. Only the `STRING` type uses `length` (the number of bytes to read; **the driver falls
back to 64 when it is 0 or empty**); for non-string points keep `length=0` and the driver ignores it.
:::

### Write command: reuses the point `address`, no separate attribute

This driver supports writing values to a point (numeric, boolean, and string alike), but there is **no
separate `command-attribute`** — a write command reuses the `address` already configured on the point, the target device
is the point's `address`, and the word width of the written value is determined by the data type of the value being
issued. So a writable point needs no extra configuration; just mark the point as writable in
the [Profile](../introduction/concepts/profile).

### Acquisition and health scheduling

These crons come from the `schedule` / `health` sections of `application.yml` and govern the acquisition cadence and
online detection:

- **Acquisition cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds).
- **Custom task**: default cron `0/5 * * * * ?` (the Melsec driver currently has no custom task; `schedule()` is an
  empty implementation and the schedule slot is reserved).
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds` — the driver judges
  online status by whether the TCP connection is alive; on a read/write exception it proactively disconnects and evicts
  that connection from the cache, and the next acquisition round reconnects automatically.
  See [Device](../introduction/concepts/device) for the online-status mechanism.

## Troubleshooting

::: warning Write `address` as the full device address — don't split area and number
Fill `address` with Mitsubishi's whole-string notation, e.g. `D100`, `M0`, `X10`, `W200` — the area prefix and the
number go in the same string. This differs from protocols configured as "area + numeric offset" in two fields (such
as [FINS](./fins)); **do not** split the area and number into two entries, or `McPLC` parses an invalid address and the
read/write fails.
:::

::: warning `series` must match the real PLC series
`series` accepts only `A` / `QnA` / `Q_L` / `IQ_R`, which determines the MC frame format. A wrong or unrecognized value
makes the driver log `Unknown series ... fallback to QnA` and fall back to `QnA` — which may read incorrect data or
error outright on other-series PLCs. Confirm the series against the actual model before onboarding.
:::

::: warning When it won't connect / read-write fails, check the PLC-side MC service first
The driver opens a TCP connection directly to `host:port`; a failed connect throws `Driver connection failed`. Common
causes: the PLC Ethernet module has no MC service enabled, the port differs from `port` (many models do not default to
`6000`), PLC-side IP filtering or an exhausted connection limit, or a firewall block. Confirm the port is reachable with
`telnet host port`, then verify the MC service port and protocol (TCP) in the PLC project.
:::

::: warning If strings or byte order read back wrong, check the type and length
A garbled or truncated string usually means `length` does not match the PLC-side string byte count (with 0 left in, the
driver reads 64). An off numeric value usually means the point's `pointTypeFlag` does not match the word width actually
stored in the device — configuring a 32-bit float device as `SHORT` only reads the low 16 bits.
:::

::: tip One driver instance can serve multiple PLCs, with connections cached per device
A single Melsec driver process can serve multiple devices, each maintaining its own `McPLC` connection (cached by device
ID in `connectMap`) and serializing read/write with its own `ReentrantLock`. Multiple PLCs are distinguished by their
respective `host`. When a device is updated or deleted, the driver receives a metadata event and closes and evicts the
corresponding connection.
:::

## How it lands in IoT DC3

::: info Implementation status: available
Both the read and write paths of the Melsec driver are fully implemented — `read()` / `write()` call real MC
reads/writes through `iot-communication`'s `McPLC` (`readInt16` / `writeInt16` / `readString`, etc.), selecting the
correct word width by data type. This is an available driver, and its behavior matches the `application.yml`
declaration.
:::

- **dc3.driver.code**: `MelsecDriver` — the driver's stable routing identifier inside the platform; device-to-driver
  binding and message dispatch address it by this code, so don't change it casually.
- **Read capability**: ✓ supported. Reads periodically on the acquisition cycle, covering all of `BOOLEAN`/`BYTE`/
  `SHORT`/`INT`/`LONG`/`FLOAT`/`DOUBLE`/`STRING`, consistent with MELSEC's "read ✓" in
  the [driver capability matrix](./matrix).
- **Write capability**: ✓ supported. On a write command it writes the matching word width by the value's data type,
  consistent with the matrix's "write ✓".
- **Subscribe capability**: — not supported. MC is a master/slave polling model; this driver polls on the acquisition
  cycle and offers no PLC-pushed subscription, consistent with the matrix's "subscribe —".

**Minimal onboarding example**: onboard a QnA-series Mitsubishi PLC at IP `192.168.0.30:6000` and acquire a 16-bit
integer from `D100`:

1. Create a [Device](../introduction/concepts/device) using `Mitsubishi Melsec Driver`, with driver attributes
   `host=192.168.0.30`, `port=6000`, `series=QnA`.
2. Add a [Point](../introduction/concepts/point) (`pointTypeFlag=SHORT`, `READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, with point attributes `address=D100`,
   `length=0`.
3. Start the driver, and within 30 seconds the acquired value of `D100` appears
   in [PointValue](../introduction/concepts/point-value).

For the full onboarding flow (modeling, binding, dispatch) see [Device onboarding](../operation/device-onboarding).

## Further reading

- [Driver overview](./index) — entry point to all drivers and their categories
- [Driver capability matrix](./matrix) — a quick read/write/subscribe lookup per driver
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Industrial buses and protocols](../foundations/fieldbus) — where the MC protocol sits in the network-layer protocol
  landscape
- [FINS Driver](./fins) — a TCP industrial protocol for Omron PLCs, addressed by "area + offset" in two fields
