---
title: Modbus TCP Driver
---

# Modbus TCP Driver

> **`dc3-driver-modbus-tcp` connects Modbus TCP slave devices to IoT DC3** — it targets a slave, periodically reads coil/register values, and supports commands that write values to registers.

Modbus TCP is one of the most common protocols on the industrial floor (heavily used by PLCs, power meters, and sensor gateways). This driver acts as the Modbus master (client), connecting over TCP to one or more slaves, then reading data and writing values according to the function code and address configured on each [Point](../introduction/concepts/point).

- **Driver name / code**: `Modbus TCP Driver` / `ModbusTcpDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the slave)

## Driver configuration (device-level `driver-attribute`)

When onboarding a Modbus TCP device, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | Modbus slave IP |
| Port | `port` | INT | `502` | Modbus TCP port (standard 502) |

## Point configuration (`point-attribute`)

Fill in these on each collected [Point](../introduction/concepts/point):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus slave unit ID |
| Function Code | `functionCode` | INT | `1` | Read function code `[1, 2, 3, 4]` |
| Offset | `offset` | INT | `0` | Register/coil address offset |

::: tip The function code decides what gets read
Reading uses `01` (coils) / `02` (discrete inputs) / `03` (holding registers) / `04` (input registers). The Point's data type (the `pointTypeFlag` of the [Point](../introduction/concepts/point)) must match the data width returned by the function code — multi-register quantities (such as a 32-bit `FLOAT`) are assembled according to the Point type.
:::

## Write command configuration (`command-attribute`)

A writable Point also needs these on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Slave unit ID |
| Function Code | `functionCode` | INT | `6` | Write function code `[5, 6, 15, 16]` |
| Offset | `offset` | INT | `0` | Address offset |
| Value Template | `valueTemplate` | STRING | `${value}` | Value template, rendered with command params |

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds` — see [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

Onboard a Modbus slave at IP `192.168.1.10:502`:

1. Create a [Device](../introduction/concepts/device) using `Modbus TCP Driver`, and set the driver attributes `host=192.168.1.10` and `port=502`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `slaveId=1`, `functionCode=3`, `offset=0`.
3. Start the driver, and within 30 seconds you will see the collected value in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning offset is a 0-based protocol address, not 40001
`offset` is the protocol-layer 0-based offset. To read "holding register 40001" in the conventional Modbus notation, set `functionCode=3` and `offset=0` (the 2nd holding register is `offset=1`, and so on). Putting `40001` directly into `offset` reads the wrong address.
:::

::: tip One driver instance can serve multiple slaves
A single Modbus TCP driver process can serve multiple devices. When several slaves hang off the same gateway under different unit IDs, `host` is the same and they are distinguished by the Point's `slaveId`.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute and Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `slaveId`
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Modbus RTU Driver](./modbus-rtu) — the serial version of Modbus
