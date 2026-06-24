---
title: PLC S7 Driver
---

# PLC S7 Driver

> **`dc3-driver-plcs7` connects Siemens S7 series PLCs to IoT DC3** — it targets the PLC's data blocks (DB), periodically reads register values, and supports writing values back to a DB.

S7 is the proprietary Ethernet protocol used by Siemens PLCs (S7-200/300/400/1200/1500, S7-200 Smart, and so on), running over TCP and used on the industrial floor to read and write the PLC's internal memory areas directly. This driver acts as an S7 client, connecting over TCP to one or more PLCs, then reading data and writing values according to the data block number and offset configured on each [Point](../introduction/concepts/point).

- **Driver name / code**: `PLC S7 Driver` / `PlcS7Driver`
- **Type**: `DRIVER_CLIENT` (actively connects to the PLC)

## Driver configuration (device-level `driver-attribute`)

When onboarding an S7 PLC device, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `192.168.0.20` | PLC IP address |
| Port | `port` | INT | `102` | S7 TCP port (standard 102) |
| PLC Type | `plcType` | STRING | `S1200` | PLC model (`S200/S200_SMART/S300/S400/S1200/S1500/SINUMERIK_828D`) |

::: tip plcType decides how addresses are resolved
Different S7 PLC models differ in DB addressing and byte order; `plcType` selects the matching S7 addressing scheme. If it is wrong or an unknown model, the driver falls back to `S1200`. Common values: use `S1200` for S7-1200, `S1500` for S7-1500, and `S200_SMART` for S7-200 Smart.
:::

## Point configuration (`point-attribute`)

Each [Point](../introduction/concepts/point) locates a single variable inside a PLC data block. Fill in:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| DB Number | `dbNum` | INT | `0` | Data block number, counted from 0 |
| Byte Offset | `byteOffset` | INT | `0` | Byte offset within the data block |
| Bit Offset | `bitOffset` | INT | `0` | Bit offset within the byte (only used for boolean type) |

::: tip The Point type decides how many bytes are read
The driver derives the read width from the Point's data type (the `pointTypeFlag` of the [Point](../introduction/concepts/point)) starting at `byteOffset`: `BOOLEAN` takes the single bit at `byteOffset.bitOffset`, `SHORT` reads 2 bytes, `INT`/`FLOAT` read 4 bytes, `LONG`/`DOUBLE` read 8 bytes, and `STRING` reads a string. So `bitOffset` only matters on boolean Points and is ignored for the other types.
:::

## Write command configuration

This driver supports writing values to the PLC, but has **no separate write-command attributes** (application.yml has no `command-attribute`). Writing a [Point](../introduction/concepts/point) reuses that Point's own `point-attribute` (`dbNum` / `byteOffset` / `bitOffset`) to locate the address, and the write width is determined by the value type carried by the command. In other words: once a writable Point has the three items above configured, it can be both read and written — no extra configuration is needed.

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds` — see [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

Onboard an S7-1200 PLC at IP `192.168.0.20:102` and read a 32-bit float at offset 0 in DB1:

1. Create a [Device](../introduction/concepts/device) using `PLC S7 Driver`, and set the driver attributes `host=192.168.0.20`, `port=102`, `plcType=S1200`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `dbNum=1`, `byteOffset=0`, `bitOffset=0`.
3. Start the driver, and within 30 seconds you will see the collected value in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning The PLC side must allow PUT/GET access
S7-1200/1500 block external PUT/GET communication by default. In TIA Portal you must check "Permit access with PUT/GET communication from remote partner", otherwise the connection can be established but reads/writes will fail. The DB being accessed must also have "Optimized block access" turned off, so the driver can locate variables by byte offset.
:::

::: warning bitOffset only applies to boolean Points
Non-boolean types (`SHORT`/`INT`/`FLOAT`, etc.) look only at `byteOffset`; `bitOffset` is ignored. Setting `bitOffset` on a float Point causes no error but has no effect — do not use it to "skip" bytes; to move across bytes, change `byteOffset`.
:::

::: tip One driver instance can serve multiple PLCs
A single PLC S7 driver process can serve multiple devices; each PLC is distinguished by the `host` (and `plcType`) on its own [Device](../introduction/concepts/device), and connections are reused per deviceId with automatic reconnect.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute and Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `dbNum`
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Melsec Driver](./melsec) — the Mitsubishi PLC Ethernet driver
