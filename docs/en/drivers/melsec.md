---
title: Melsec Driver
---

# Melsec Driver

> **`dc3-driver-melsec` onboards Mitsubishi PLCs into IoT DC3 over the MC protocol**——it periodically reads PLC memory values by device address, and supports commands that write values back to devices.

The MC protocol (MELSEC Communication) is the native communication protocol of Mitsubishi PLCs, widely used across the A, QnA, Q/L, and iQ-R series. This driver acts as an MC client, actively connecting to the PLC over TCP, then reading and writing per the device address configured on each [Point](../introduction/concepts/point). It is built on the `iot-communication` protocol library and automatically picks the matching word width by the point's data type when reading or writing.

- **Driver name / code**: `Mitsubishi Melsec Driver` / `MelsecDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the PLC)

::: tip A few MC concepts first
**Device (also called memory address)**: a data unit in a Mitsubishi PLC partitioned by purpose——e.g. `D` (data register, the most common), `M` (internal relay), `X` (input relay), `W` (link register). **Device address**: a complete address string made of an area prefix plus a number, e.g. `D100`, `M0`, `X10`, `W200`——the area and number are written together, not split into two fields. **PLC Series**: the MC frame format differs slightly between series, so choose `A` / `QnA` / `Q_L` / `IQ_R` to match the actual PLC.
:::

## Driver configuration (device-level `driver-attribute`)

When onboarding a Melsec PLC, fill these [Attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `192.168.0.20` | Ip |
| Port | `port` | INT | `6000` | Port |
| PLC Series | `series` | STRING | `QnA` | PLC series (A/QnA/Q_L/IQ_R) |

## Point configuration (`point-attribute`)

Fill these on each [Point](../introduction/concepts/point):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Device Address | `address` | STRING | `D100` | Device memory address (D100, M0, X10, W200 etc.) |
| String Length | `length` | INT | `0` | String read length (0 for non-string types) |

::: tip The data type decides how many words to read and how to decode them
The driver picks the read/write width automatically from the point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`): `BOOLEAN` reads a bit, `SHORT` reads 16 bits, `INT`/`FLOAT` read 32 bits, `LONG`/`DOUBLE` read 64 bits. Only the `STRING` type uses `length` (the string read length); for non-string points keep `length=0` and the driver ignores it.
:::

## Write-command configuration

This driver supports writing values to a point (numeric, boolean, and string alike), but there is **no separate `command-attribute`**——a write command reuses the `address` already configured on the point, the target device is the point's `address`, and the word width of the written value is determined by the data type of the value being issued. So a writable point needs no extra configuration; just mark the point as writable in the [Profile](../introduction/concepts/profile).

## Acquisition and health

- **Acquisition cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds).
- **Custom task**: default cron `0/5 * * * * ?` (the Melsec driver currently has no custom task; the schedule slot is reserved).
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`——the driver judges online status by whether the TCP connection is alive, and on a read/write failure it proactively disconnects and reconnects. See [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

Onboard a QnA-series Mitsubishi PLC at IP `192.168.0.30:6000` and acquire a 16-bit integer from `D100`:

1. Create a [Device](../introduction/concepts/device) using `Mitsubishi Melsec Driver`, with driver attributes `host=192.168.0.30`, `port=6000`, `series=QnA`.
2. Add a [Point](../introduction/concepts/point) (`pointTypeFlag=SHORT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, with point attributes `address=D100`, `length=0`.
3. Start the driver, and within 30 seconds the acquired value of `D100` appears in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning Write `address` as the full device address——don't split area and number
Fill `address` with Mitsubishi's whole-string notation, e.g. `D100`, `M0`, `X10`, `W200`——the area prefix and the number go in the same string. This differs from protocols configured as "area + numeric offset" in two fields (such as FINS); **do not** split the area and number into two entries.
:::

::: warning `series` must match the real PLC series
`series` accepts only `A` / `QnA` / `Q_L` / `IQ_R`, which determines the MC frame format. A wrong (or unrecognized) value falls back to `QnA`, which may read incorrect data on other-series PLCs——confirm the series against the actual model before onboarding.
:::

::: tip One driver instance can serve multiple PLCs
A single Melsec driver process can serve multiple devices, each maintaining its own TCP connection (cached by device ID). Multiple PLCs are distinguished by their respective `host`.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes and configuration](../introduction/concepts/attribute-config) — where attributes like `host` / `address` come from across the three layers
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [FINS Driver](./fins) — a TCP industrial protocol for Omron PLCs
