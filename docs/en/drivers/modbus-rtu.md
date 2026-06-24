---
title: Modbus RTU Driver
---

# Modbus RTU Driver

> **`dc3-driver-modbus-rtu` connects Modbus RTU slave devices to IoT DC3** — it reaches slaves over a serial port, periodically reads coil/register values, and supports commands that write values to coils and holding registers.

Modbus RTU is the serial-link variant of the Modbus protocol, running over RS-485/RS-232 buses, and is one of the most common protocols on the plant floor (heavily used by PLCs, energy meters, VFDs, and sensors). It shares the exact same function codes and address model as the [Modbus TCP Driver](./modbus-tcp); the only difference is the physical layer: RTU runs over a serial port (baud rate, data bits, parity, etc.) rather than IP/port. This driver acts as the Modbus master, connecting through one serial port to one or more slaves on the same bus, then reads and writes values according to the function code and address configured on each [Point](../introduction/concepts/point).

- **Driver name / code**: `Modbus RTU Driver` / `ModbusRtuDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to slaves)

## Driver configuration (device-level `driver-attribute`)

When onboarding a Modbus RTU device, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). These are serial-port parameters and must match the slave's serial settings exactly, or the connection will fail:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Port | `port` | STRING | `/dev/ttyUSB0` | Serial port name (e.g. /dev/ttyUSB0, COM3) |
| Baud Rate | `baudRate` | INT | `9600` | Serial baud rate (e.g. 9600, 19200, 115200) |
| Data Bits | `dataBits` | INT | `8` | Data bits (7 or 8) |
| Stop Bits | `stopBits` | INT | `1` | Stop bits (1 or 2) |
| Parity | `parity` | INT | `0` | Parity (0=None, 1=Odd, 2=Even, 3=Mark, 4=Space) |

## Point configuration (`point-attribute`)

Fill in these on each [Point](../introduction/concepts/point) being collected:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus slave unit ID |
| Function Code | `functionCode` | INT | `1` | Read function code `[1, 2, 3, 4]` |
| Offset | `offset` | INT | `0` | Register/coil address offset |

::: tip The function code decides what gets read
Reading uses `01` (coil) / `02` (discrete input) / `03` (holding register) / `04` (input register). A Point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) must match the data width returned by the function code — the driver picks the register width from the Point type: `LONG`→4-byte int, `FLOAT`→4-byte float, `DOUBLE`→8-byte float, everything else 2-byte int.
:::

## Write command configuration (`command-attribute`)

Writable Points also need these on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus slave unit ID |
| Function Code | `functionCode` | INT | `6` | Write function code `[5, 6, 15, 16]` |
| Offset | `offset` | INT | `0` | Register/coil address offset |
| Value Template | `valueTemplate` | STRING | `${value}` | Value template rendered with command params |

## Collection & health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read every 30 seconds).
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds` — see [Device](../introduction/concepts/device) for the online-state mechanism.

## Minimal onboarding example

Onboard one Modbus RTU slave on `/dev/ttyUSB0`, baud rate `9600`, unit ID `1`:

1. Create a [Device](../introduction/concepts/device) with `Modbus RTU Driver`, and set driver attributes `port=/dev/ttyUSB0`, `baudRate=9600`, `dataBits=8`, `stopBits=1`, `parity=0`.
2. On the [Profile](../introduction/concepts/profile) bound to the device, add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`), with point attributes `slaveId=1`, `functionCode=3`, `offset=0`.
3. Start the driver; within 30 seconds the collected value shows up in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning One wrong serial parameter and nothing connects
All five of `port`, `baudRate`, `dataBits`, `stopBits`, `parity` must match the slave's serial settings one by one (unlike TCP, RTU has no handshake negotiation — a mismatch just yields garbage or timeouts). After 3 consecutive connection failures the driver enters a 60-second backoff and reports the device offline throughout; once you fix the parameters or cabling, you must wait out the backoff before it reconnects.
:::

::: tip Multiple slaves on one bus share a single serial port
An RS-485 bus can carry multiple slaves; they share the same `port` and are distinguished by the Point's `slaveId`. The driver maintains one dedicated serial connection per [Device](../introduction/concepts/device), keyed by device, so give every slave on the same physical bus the **same** `port` and address them via `slaveId` — do not assign a different serial port per slave.
:::

::: warning Write commands only support coils and holding registers
The write function code defaults to `6`, but the driver actually handles only two write targets: `01` (write coil) and `03` (write holding register). Codes like `15`/`16` are ignored and the write fails. As with reads, `offset` is the protocol-level 0-based offset, not the `40001`-style convention.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the generic driver model and registration mechanism
- [Attributes & config](../introduction/concepts/attribute-config) — where attributes like `port` / `slaveId` come from across the three layers
- [Device onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — the Ethernet flavor of Modbus
