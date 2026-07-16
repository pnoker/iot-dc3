---
title: PLC S7 Driver
---

<script setup>
import PlcS7Diagram from '../../.vitepress/theme/components/PlcS7Diagram.vue'
</script>


# PLC S7 Driver

`dc3-driver-plcs7` connects Siemens S7 series PLCs to IoT DC3: acting as an S7 client, it connects over TCP to one or
more PLCs, periodically reads values according to the data block number and offset configured on
each [Point](../introduction/concepts/point), and supports writing values back. After this page you can fill in the
right `host` / `plcType` / `dbNum` attributes on a device, turn a DB variable into a readable and writable Point, and
diagnose the most common cannot-connect, wrong-value, and write-failure problems.

## Protocol background

S7 (also called S7comm / ISO-on-TCP) is the proprietary Ethernet protocol used by Siemens PLCs (
S7-200/300/400/1200/1500, S7-200 Smart, and SINUMERIK CNC systems). It runs on top of `ISO 8073 COTP`, encapsulated in
TCP on the standard port `102`, and is used to read and write the PLC's internal memory areas directly — especially the
**Data Blocks (DB)** that engineers define in STEP 7 / TIA Portal.

In terms of communication model, S7 is a classic **master/slave, request-response** protocol: this driver acts as the
master (client), actively opening connections and sending read/write requests to each PLC in turn, while the PLC
responds passively and never pushes on its own. Addressing uses the numeric form "DB number + byte offset (+ bit
offset)", aligned by engineering convention — which means that before onboarding you must look up, from the PLC program,
which DB and which byte each variable lives at.

In the four-layer IoT architecture, S7 sits on the wired-industrial side of the **network layer**: it solves the last
mile of "how data inside a PLC gets read by an external system over Ethernet". For the communication models, addressing
schemes, and byte-order trade-offs of industrial bus protocols,
see [Industrial Bus and Protocols](../foundations/fieldbus).

<PlcS7Diagram lang="en" />

A single driver process can connect to multiple PLCs; connections are reused per `deviceId`, and each PLC is
distinguished by the `host` and `plcType` on its own [Device](../introduction/concepts/device).

## Attribute configuration

S7 has no separate "write-command attribute" (`application.yml` has no `command-attribute`). Addressing information
splits into two layers: **driver attributes** locate "which PLC to connect to", and **point attributes** locate "which
variable inside the PLC to read". The fields, types, and defaults in both tables below come from the driver's
`application.yml`.

### Driver attributes (device-level `driver-attribute`)

When onboarding an S7 PLC device, fill in these [attributes](../introduction/concepts/attribute-config) on
the [Device](../introduction/concepts/device). `host` and `port` together determine the connection target, while
`plcType` selects which S7 addressing scheme the driver uses to resolve DB addresses and byte order.

| Attribute | code      | Type   | Default        | Description                 |
|-----------|-----------|--------|----------------|-----------------------------|
| Host      | `host`    | STRING | `192.168.0.20` | PLC IP address              |
| Port      | `port`    | INT    | `102`          | S7 TCP port, standard `102` |
| PLC Type  | `plcType` | STRING | `S1200`        | PLC model, values below     |

::: tip plcType decides how addresses are resolved
Different S7 PLC models differ in DB addressing detail and data layout; `plcType` selects the matching S7 addressing
scheme. Valid values come from the underlying `EPlcType` enum: `S200` / `S200_SMART` / `S300` / `S400` / `S1200` /
`S1500` / `SINUMERIK_828D`. Typical mapping: use `S1200` for S7-1200, `S1500` for S7-1500, and `S200_SMART` for S7-200
Smart. If the value is wrong or outside the enum, the driver logs a warning and falls back to `S1200`.
:::

### Point attributes (`point-attribute`)

Each [Point](../introduction/concepts/point) locates a single variable inside a PLC data block. The driver assembles
these three items into an S7 address string and hands it to the underlying library: a non-boolean Point becomes
`DB{dbNum}.{byteOffset}`, and only when the Point is boolean and `bitOffset > 0` does it become
`DB{dbNum}.{byteOffset}.{bitOffset}`.

| Attribute   | code         | Type | Default | Description                                                   |
|-------------|--------------|------|---------|---------------------------------------------------------------|
| DB Number   | `dbNum`      | INT  | `0`     | Data block number, counted from 0                             |
| Byte Offset | `byteOffset` | INT  | `0`     | Byte offset within the data block                             |
| Bit Offset  | `bitOffset`  | INT  | `0`     | Bit offset within the byte (only for boolean Points when > 0) |

::: tip The Point type decides how many bytes are read
Read/write width is derived from the Point's data type (the `pointTypeFlag` of
the [Point](../introduction/concepts/point)) starting at `byteOffset`, with no extra configuration: `BOOLEAN` takes one
bit, `BYTE` 1 byte, `SHORT` 2 bytes, `INT`/`FLOAT` 4 bytes, `LONG`/`DOUBLE` 8 bytes, `STRING` reads a string. So the
same DB offset configured as different Point types reads values of different widths.
:::

::: warning bitOffset only applies to boolean Points and only when non-zero
The driver uses bit addressing (an address with a third `.bit` segment) only when the type is boolean **and**
`bitOffset > 0`. In every other case — non-boolean types, or boolean with `bitOffset=0` — it addresses at byte level as
`DB{dbNum}.{byteOffset}`. Setting `bitOffset` on a float Point causes no error and has no effect; to move across bytes
change `byteOffset`, do not use `bitOffset` to "skip" bytes.
:::

Writing a Point reuses that Point's own attributes (`dbNum` / `byteOffset` / `bitOffset`) to locate the address, and the
write width is determined by the value type carried by the command. Once a writable Point has these three items
configured, it can be both read and written — no extra write-command configuration is needed.

## Troubleshooting

When onboarding S7, these are the most common traps; most are PLC-side settings rather than driver problems.

::: danger The PLC side must allow PUT/GET access
S7-1200/1500 **block** external PUT/GET communication by default — this is the most common cause of onboarding failure.
In the CPU properties in TIA Portal you must check "Permit access with PUT/GET communication from remote partner".
Otherwise the TCP connection establishes but every read/write is rejected by the PLC.
:::

::: warning The DB must have "Optimized block access" turned off
If the data block being accessed has "Optimized block access" enabled, variables no longer have fixed byte offsets
inside the DB, so locating by `byteOffset` reads wrong values or fails. In TIA Portal, select the DB → Properties →
uncheck "Optimized block access"; after compiling and downloading, the offset addresses become stable and usable.
:::

- **Port `102` unreachable**: S7 runs over TCP `102`; confirm the PLC IP (`host`) is reachable, the firewall is not
  blocking it, and `port` has not been changed to a non-standard value. Verify the link with `ping` and
  `telnet <host> 102` before blaming the driver.
- **Wrong `plcType` breaks address resolution**: a wrong model falls back to `S1200`, which can read misaligned values
  on S7-300/400/1500. The log line `Unknown plcType ... fallback to S1200` means the value is outside the enum — correct
  it per the table above.
- **Values come back wrong (byte order / offset)**: first confirm the DB has optimized block access turned off and that
  `dbNum` and `byteOffset` map one-to-one to the variable address in the PLC program; then confirm the Point's
  `pointTypeFlag` matches the PLC variable's type width (e.g. PLC `Real` → `FLOAT`, `DInt` → `INT`).
- **Device stays offline / reconnects frequently**: when a read or write throws, the driver invalidates that
  connection (`invalidateConnection`) and rebuilds it on the next access. Repeated reconnects usually mean PLC-side
  rejection, network jitter, or an overloaded PLC; pinpoint with the `Driver connection failed` / `read failed` lines in
  the driver log. For the online-status and lease-timeout mechanism see [Device](../introduction/concepts/device).

## How it lands in IoT DC3

- **`dc3.driver.code`**: `PlcS7Driver` (driver name `PLC S7 Driver`, type `DRIVER_CLIENT`). This is a stable routing
  identifier — do not change it casually.
- **Read**: supported. Default collection cron `0/30 * * * * ?` (one read round every 30 seconds); each Point is read by
  address and wrapped into a [PointValue](../introduction/concepts/point-value).
- **Write**: supported. Reuses point attributes to locate the address and writes back to the PLC by the command value
  type.
- **Subscribe / report**: not provided. S7 is a master/slave polling model and the driver does not listen for
  PLC-initiated pushes — consistent with "S7: read ✓ / write ✓ / subscribe —" in
  the [driver capability matrix](./matrix).
- **Health check**: device health check defaults to cron `0/15 * * * * ?`, lease timeout `45` seconds.

::: info Implementation status: available
`read()` / `write()` / `initial()` / `event()` / `validate()` in `PlcS7DriverCustomServiceImpl` are all fully
implemented, backed by the `iot-communication` (`S7PLC`) library, with auto-reconnect enabled, connections reused per
`deviceId`, and a `ReentrantLock` serializing reads/writes. This is a working driver, not a skeleton.
:::

Minimal onboarding example: onboard an S7-1200 at IP `192.168.0.20:102` and read a 32-bit float at offset 0 in DB1:

1. Create a [Device](../introduction/concepts/device) using `PLC S7 Driver`, and set the driver attributes
   `host=192.168.0.20`, `port=102`, `plcType=S1200`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `dbNum=1`,
   `byteOffset=0`, `bitOffset=0`.
3. Confirm the PLC has PUT/GET enabled and that DB's optimized block access is off, start the driver, and within 30
   seconds you will see the collected value in [PointValue](../introduction/concepts/point-value).

## Further reading

- [Driver overview](./index) — the entry point and taxonomy of all drivers
- [Driver capability matrix](./matrix) — read / write / subscribe capability at a glance
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Industrial Bus and Protocols](../foundations/fieldbus) — the network-layer communication models, addressing, and byte
  order S7 belongs to
- [Melsec Driver](./melsec) — the Mitsubishi PLC Ethernet driver, also in the industrial bus / PLC category
