---
title: Modbus RTU Driver
---

# Modbus RTU Driver

`dc3-driver-modbus-rtu` connects Modbus RTU slave devices to IoT DC3: acting as the master, it reaches slaves on an RS-485/RS-232 bus over a single serial port, periodically reads coil/register values, and supports commands that write to coils and holding registers. By the end of this page you can configure the serial parameters, function codes, and addresses for a serial Modbus device, and know where to look when it won't connect.

## Protocol background

Modbus is an industrial protocol born in 1979 for serial PLC communication, and it remains one of the most common protocols on the plant floor — heavily used by PLCs, energy meters, VFDs, temperature controllers, and sensors. **Modbus RTU** is its serial-link variant: messages travel as compact binary frames over an RS-485/RS-232 bus, with a CRC to guarantee integrity. It shares the exact same function codes and address model as the [Modbus TCP Driver](./modbus-tcp) — reads use `01/02/03/04`, writes use `05/06/15/16`, and addresses are 0-based offsets — the only difference being the physical layer: RTU runs over a serial port (baud rate, data bits, parity, stop bits) rather than IP/port.

In the four-layer IoT architecture, Modbus RTU sits at the **network layer**: it answers "what signaling and what rules to exchange bytes over" between field devices and the polling master. It is a textbook **master/slave request-response** protocol — the master polls slaves and waits for replies, slaves never speak unprompted; one RS-485 bus can carry multiple slaves, distinguished by unit ID (`slaveId`). The protocol itself often just moves bytes, and **how those bytes are interpreted is decided by configuration** (16-bit int vs 32-bit float, low byte first vs high byte first) — the most common pitfall on the floor. For the general background on addressing, byte order, and the polling model, see [IoT network layer: Industrial Buses & Protocols](../foundations/fieldbus).

- **Driver name / code**: `Modbus RTU Driver` / `ModbusRtuDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to slaves)
- **Underlying libraries**: modbus4j + jSerialComm (one dedicated serial connection per device)

## Attribute configuration

Modbus RTU configuration spans three layers: **driver attributes** (`driver-attribute`, device-level) describe how this serial port is opened; **point attributes** (`point-attribute`) describe which slave and register each collected point reads; **command attributes** (`command-attribute`) describe where a writable point writes. The defaults and descriptions in the three tables below all come from the driver's `application.yml`; for where attributes originate across the three layers, see [Attributes & config](../introduction/concepts/attribute-config).

### Driver attributes (device-level `driver-attribute`)

When onboarding a Modbus RTU device, set these five serial parameters on the [Device](../introduction/concepts/device). They are passed verbatim to jSerialComm's `setComPortParameters` to open the port, so they **must match the slave's serial settings one by one** — unlike TCP, RTU has no handshake negotiation, and a mismatch just yields garbage or timeouts:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Port | `port` | STRING | `/dev/ttyUSB0` | Serial port name (e.g. /dev/ttyUSB0, COM3) |
| Baud Rate | `baudRate` | INT | `9600` | Serial baud rate (e.g. 9600, 19200, 115200) |
| Data Bits | `dataBits` | INT | `8` | Data bits (7 or 8) |
| Stop Bits | `stopBits` | INT | `1` | Stop bits (1 or 2) |
| Parity | `parity` | INT | `0` | Parity (0=None, 1=Odd, 2=Even, 3=Mark, 4=Space) |

### Point attributes (`point-attribute`)

Set three attributes on each collected [Point](../introduction/concepts/point), pinning down "which slave's which address, read with which function code":

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus slave unit ID |
| Function Code | `functionCode` | INT | `1` | Read function code `[1, 2, 3, 4]` |
| Offset | `offset` | INT | `0` | Register/coil address offset (0-based) |

::: tip The function code decides what is read; the Point type decides how bytes are assembled
Reading supports four function codes: `01` (coil) / `02` (discrete input) / `03` (holding register) / `04` (input register). For register reads (`03`/`04`), the driver uses the Point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) to decide how many registers to take and how to interpret them: `LONG`→4-byte signed int, `FLOAT`→4-byte float, `DOUBLE`→8-byte float, everything else 2-byte signed int. Get the Point type wrong and a float reads back as a meaningless large number.
:::

### Command attributes (`command-attribute`)

Writable Points also need four attributes on the write command. `valueTemplate` is the value template, rendered with command params before the write is issued:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus slave unit ID |
| Function Code | `functionCode` | INT | `6` | Write function code (the driver actually handles only `1` write coil and `3` write holding register) |
| Offset | `offset` | INT | `0` | Register/coil address offset (0-based) |
| Value Template | `valueTemplate` | STRING | `${value}` | Value template rendered with command params |

### Collection & health

- **Collection cycle**: default cron `0/30 * * * * ?` (reads all points once every 30 seconds).
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds` — the driver decides online/offline from whether the serial connection is initialized; see [Device](../introduction/concepts/device) for the online-state mechanism.

The minimal onboarding path: create a [Device](../introduction/concepts/device) with `Modbus RTU Driver` and set driver attributes `port=/dev/ttyUSB0`, `baudRate=9600`, `dataBits=8`, `stopBits=1`, `parity=0`; on the bound [Profile](../introduction/concepts/profile), add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) with point attributes `slaveId=1`, `functionCode=3`, `offset=0`; start the driver, and within 30 seconds the collected value shows up in [PointValue](../introduction/concepts/point-value).

## Troubleshooting

::: warning One wrong serial parameter and nothing connects
All five of `port`, `baudRate`, `dataBits`, `stopBits`, `parity` must match the slave's serial settings one by one. RTU has no handshake negotiation, so any mismatch yields garbage or timeouts rather than a clear error. Confirm cabling and baud rate with a multimeter / serial terminal first, then check parity (many meters default to even parity `2`, not the default no-parity `0`).
:::

::: warning Three consecutive failures trigger a 60-second backoff; the device is offline throughout
The driver tracks consecutive failures per device: **after 3 consecutive connection failures it enters a 60-second backoff** (`FAILURE_BACKOFF_THRESHOLD=3`, `FAILURE_BACKOFF_MS=60000`), during which the health check reports offline outright and stops attempting connections. So even after you fix the serial parameters or cabling, you must wait out the backoff before it reconnects — don't keep tweaking config inside the backoff window and conclude it "still won't connect."
:::

::: warning offset is a 0-based protocol address, not 40001
`offset` is the protocol-level 0-based offset. To read "holding register 40001" in the conventional Modbus notation, set `functionCode=3`, `offset=0` (the 2nd holding register is `offset=1`, and so on). Putting `40001` straight into `offset` reads the wrong address or goes out of range.
:::

::: warning Byte order: a 32-bit float read as a huge number usually means swapped register order
A 32-bit `FLOAT`/`LONG` spans two 16-bit registers, and field devices use one of four register orderings (ABCD/CDAB/BADC/DCBA). This driver interprets registers in modbus4j's default order; if the float reads back as a meaningless large number, the slave is most likely using the opposite register order — adjust the byte-order setting on the slave side, or switch to an integer type and convert from the raw register value in an upper layer.
:::

::: tip Multiple slaves share one serial port, addressed by slaveId
An RS-485 bus can carry multiple slaves; they share the same `port` and are distinguished by the Point's `slaveId`. The driver keeps one connection per [Device](../introduction/concepts/device) ID (`connectMap`), so slaves on the same physical bus should be created as devices pointing at the **same `port`** and addressed via `slaveId` — do not assign a different serial port per slave. Note that a serial port is an exclusive resource: make sure that `port` is not held by another process (and on Linux the running user needs read/write access to `/dev/ttyUSB*`).
:::

## How it lands in IoT DC3

Whatever the underlying protocol, everything converges on the platform into a single [Point](../introduction/concepts/point) and its [PointValue](../introduction/concepts/point-value). The Modbus RTU driver registers with `dc3.driver.code = ModbusRtuDriver`, the stable routing identifier the platform uses to dispatch read/write commands to this driver.

Per the [driver capability matrix](./matrix), this driver's capabilities are:

| Capability | Supported | Implementation notes |
|---|---|---|
| Read | ✓ | Function codes `01/02/03/04`, covering coils, discrete inputs, holding registers, input registers |
| Write | ✓ | **Only** `01` (write coil) and `03` (write holding register) |
| Subscribe | — | Master/slave polling protocol; no device-initiated reports, the collection cycle reads on a timer |

::: info Implementation status: available
`ModbusRtuDriverCustomServiceImpl`'s `read()`/`write()`/`health()` and connection management are fully implemented (on modbus4j + jSerialComm) — not a skeleton. The read path supports all four read function codes; connection handling, backoff, health detection, and metadata events (destroying the stale connection when a device is added/updated/deleted) are all in place.
:::

::: warning Write commands only support coils and holding registers
The command attribute `functionCode` defaults to `6`, but `write()` actually handles only `01` (write coil) and `03` (write holding register) — codes like `15`/`16` fall into the `default` branch and return `false`, so the write fails silently. For writable Points, set `functionCode=1` for coil writes and `functionCode=3` for holding-register writes.
:::

## Further reading

- [Drivers overview](./index) — all protocol drivers and the entry point for selection
- [Driver capability matrix](./matrix) — quick reference for read/write/subscribe per driver
- [Device onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [Industrial Buses & Protocols](../foundations/fieldbus) — network layer: addressing, byte order, and the polling model
- [Modbus TCP Driver](./modbus-tcp) — the Ethernet flavor of Modbus, with the same function codes and address model
