---
title: Modbus TCP Driver
---

<script setup>
import ModbusTcpDiagram from '../../.vitepress/theme/components/ModbusTcpDiagram.vue'
</script>


# Modbus TCP Driver

`dc3-driver-modbus-tcp` connects Modbus TCP slave devices to IoT DC3. It acts as the Modbus master (client),
periodically reads coils/registers over Ethernet, and supports writing values to coils and holding registers. By the end
you can set `host`/`port` on a [Device](../introduction/concepts/device), set function codes and addresses on
a [Point](../introduction/concepts/point), and troubleshoot the common "no value read / write won't go through"
problems.

> You are here: a concrete driver on the "industrial wired" side of the network layer. For the protocol-level addressing
> model, byte order, and function-code concepts, see [Industrial Buses & Protocols](../foundations/fieldbus).

## Protocol background

Modbus dates back to 1979, originally a master/slave protocol Modicon designed for PLC serial communication, and it is
still one of the most common protocols on the industrial floor — heavily used by PLCs, power meters, VFDs, and sensor
gateways. It is simple, open, and publicly documented, which is why every vendor implements it.

**Modbus TCP** is the Ethernet encapsulation of Modbus: it wraps the Modbus application-layer message (PDU) that
originally ran over an RS-485 serial line into TCP/IP, listening on port **502** by default. Compared with the
serial [Modbus RTU](./modbus-rtu), the TCP variant drops the CRC check (TCP guarantees integrity) and prepends a 7-byte
MBAP header for transaction identification. A single Ethernet segment can host many slaves, and a Modbus TCP gateway can
bridge to multiple serial slaves behind it.

In the [four-layer IoT architecture](../foundations/fieldbus), Modbus TCP belongs to the **network layer**,
industrial-wired side: it defines how a field device is addressed and read/written over the network, moving the physical
quantities sampled at the perception layer up to the platform. Its communication model is the classic **master/slave,
request/response** — a slave stays silent until polled, so the IoT DC3 driver, acting as master, polls on a cron cycle.

Modbus data is organized into four register spaces, distinguished on read by the **function code**:

<ModbusTcpDiagram lang="en" />

Coils and discrete inputs are single bits (digital), while holding and input registers are 16-bit words (analog). A
32-bit `FLOAT` or `LONG` occupies two consecutive registers, and a 64-bit `DOUBLE` occupies four — the driver assembles
multi-register quantities automatically based on the Point's data type.

## Attribute configuration

Onboarding a Modbus TCP device means filling [attributes](../introduction/concepts/attribute-config) at three levels:
device-level connection parameters (`driver-attribute`), per-collected-point addressing parameters (`point-attribute`),
and per-writable-point write-command parameters (`command-attribute`). The attributes, types, and defaults below all
come from the driver's `application.yml` (the `dc3-driver-modbus-tcp` module).

### Driver attributes (device-level `driver-attribute`)

Driver attributes answer "which slave do I connect to". Fill one set on each Modbus
TCP [Device](../introduction/concepts/device):

| Attribute | code   | Type   | Default     | Description                    |
|-----------|--------|--------|-------------|--------------------------------|
| Host      | `host` | STRING | `localhost` | Modbus slave IP or hostname    |
| Port      | `port` | INT    | `502`       | Modbus TCP port (standard 502) |

`host` + `port` uniquely identify a TCP connection. The driver caches one connection per device ID (one `ModbusMaster`
per device), and the socket timeout is fixed at **5 seconds**. During config validation, `port` must fall within
`1–65535`.

### Point attributes (`point-attribute`)

Point attributes answer "which data point on this slave do I read". Fill one set on each
collected [Point](../introduction/concepts/point):

| Attribute     | code           | Type | Default | Description                                       |
|---------------|----------------|------|---------|---------------------------------------------------|
| Slave ID      | `slaveId`      | INT  | `1`     | Modbus slave unit ID                              |
| Function Code | `functionCode` | INT  | `1`     | Read function code, only `[1, 2, 3, 4]` supported |
| Offset        | `offset`       | INT  | `0`     | Register/coil address offset (0-based)            |

`slaveId` distinguishes multiple slaves behind the same IP (gateway). `functionCode` decides which register space to
read, and validation forces it into `1–4`:

::: tip The function code decides which register space gets read
Reading uses `01` (coils) / `02` (discrete inputs) / `03` (holding registers) / `04` (input registers). The Point's data
type (the `pointTypeFlag` of the [Point](../introduction/concepts/point)) must match the data width returned by the
function code — the driver maps types to Modbus data widths: `LONG`→4-byte int, `FLOAT`→4-byte float, `DOUBLE`→8-byte
float, others (e.g. `INT`)→2-byte int. Multi-register quantities are assembled across registers automatically per Point
type.
:::

### Write command attributes (`command-attribute`)

A writable Point also needs one set on the write command, answering "which address to write and what value":

| Attribute      | code            | Type   | Default    | Description                                                                           |
|----------------|-----------------|--------|------------|---------------------------------------------------------------------------------------|
| Slave ID       | `slaveId`       | INT    | `1`        | Slave unit ID                                                                         |
| Function Code  | `functionCode`  | INT    | `6`        | Write function code (yml lists `[5, 6, 15, 16]`, but see implementation status below) |
| Offset         | `offset`        | INT    | `0`        | Address offset (0-based)                                                              |
| Value Template | `valueTemplate` | STRING | `${value}` | Value template, rendered with command params                                          |

`valueTemplate` defaults to `${value}`, meaning the value passed in the command is written as-is; change the template
when a transform is needed (such as multiply-by-factor or add-offset). How the value is encoded into registers is
determined by the Point's `pointTypeFlag`.

::: warning Write function code: yml lists 4, the implementation honors only 2
`application.yml` annotates the write function codes as `[5, 6, 15, 16]` with default `6`, but the current
`ModbusTcpDriverCustomServiceImpl.writeValue()` handles only **`functionCode=1` (write single coil)** and *
*`functionCode=3` (write single holding register)**. Any other code (including the default `6`) falls to the `default`
branch and returns `false` (write failed). So set the command's `functionCode` explicitly to `3` to write a register or
`1` to write a coil — do not keep the default `6`. The semantics of FC05/06/15/16 are not yet implemented in code — the
code is the source of truth.
:::

## Troubleshooting

Most Modbus TCP onboarding failures cluster into three classes: connection, addressing, and byte order. Work through
them in this order:

1. **Port/connection unreachable (device stays offline)**. First confirm the slave IP and port 502 are reachable:
   `telnet <host> 502` or `nc -vz <host> 502`. The driver's socket timeout is 5 seconds; a failed connect throws
   `ConnectorException`. Note: after **3** consecutive connection failures the driver enters a **60-second backoff**,
   during which the health check reports offline immediately without retrying — once the network is fixed it recovers
   automatically within at most one backoff window.

2. **Connects but reads nothing / errors**. Check that the Point's `slaveId` is correct (mismatches are common with
   multiple slaves behind a gateway) and that `functionCode` matches the actual register type at that address (using
   `03` for holding registers against a read-only discrete input throws an exception). A failed read throws
   `ReadPointException`, invalidates that device's connection, and reconnects on the next cycle.

3. **`offset` filled in as an address like 40001**. This is the most frequent mistake — see the pitfall container below.
   `offset` is a 0-based protocol offset, not the PLC-conventional 4xxxx numbering.

4. **Wrong value / bytes swapped (byte order)**. 32/64-bit values span multiple registers, and devices differ in their
   word-order/byte-order conventions. The driver reads with the default word order of the underlying modbus4j — if a
   float comes out clearly garbled (e.g. `25.0` read as an astronomical number), the device-side word order usually
   differs from the default. The driver attributes do not currently expose a word-order switch, so for such devices
   adjust the register mapping on the device side, or read as integers and convert yourself.

5. **Write command returns failure**. First confirm `functionCode` is `1` or `3` per the "write function code" warning
   above; if it still fails, check the target is a writable space (discrete inputs `02` and input registers `04` are
   physically read-only and cannot be written). A failed write throws `WritePointException` and invalidates the
   connection.

6. **Flapping online status**. The health check runs every 15 seconds by default, with a 45-second lease timeout. If a
   device flaps between online/offline, it is usually packet loss or a slave responding slower than the 5-second
   timeout — see [Device](../introduction/concepts/device) for the online-status mechanism.

::: warning offset is a 0-based protocol address, not 40001
`offset` is the protocol-layer 0-based offset. To read "holding register 40001" in the conventional Modbus notation, set
`functionCode=3` and `offset=0` (the 2nd holding register is `offset=1`, and so on). Putting `40001` directly into
`offset` reads the wrong address or errors out of range.
:::

## How it lands in IoT DC3

- **`dc3.driver.code`**: `ModbusTcpDriver` (type `DRIVER_CLIENT`, actively connects to the slave). This is a stable
  routing identifier — do not change it casually.
- **Read**: ✓ implemented. Supports function codes `1/2/3/4` (coils/discrete inputs/holding registers/input registers),
  assembling multi-register quantities by Point type.
- **Write**: ✓ implemented, but **only** `functionCode=1` (write coil) and `functionCode=3` (write holding register);
  other codes return failure (see the write function-code warning above).
- **Subscribe/report**: — not supported. Modbus is a master/slave polling model; the driver only actively reads/writes
  and never passively receives pushes. This matches the "✓ / ✓ / —" for Modbus TCP in
  the [driver capability matrix](./matrix).
- **Collection cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds), configured under
  `schedule.read` in the driver's `application.yml`; the `custom` schedule is disabled by default.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds`.

::: info Implementation status: available
This driver is a **complete implementation** (not a skeleton), built on modbus4j. The read path covers all four register
spaces, the write path covers coils and holding registers, and it includes connection caching and failure backoff. The
only caveat is that the yml annotation for write function codes (`[5,6,15,16]`) is broader than the code (only `1/3`) —
configure explicitly per the warning above and it works.
:::

### Minimal onboarding example

Onboard a Modbus slave at IP `192.168.1.10:502`:

1. Create a [Device](../introduction/concepts/device) using `Modbus TCP Driver`, and set the driver attributes
   `host=192.168.1.10` and `port=502`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `slaveId=1`,
   `functionCode=3`, `offset=0`.
3. Start the driver, and within 30 seconds you will see the collected value
   in [PointValue](../introduction/concepts/point-value).
4. If the Point must be writable, configure a write [Command](../introduction/concepts/command) for it and set
   `functionCode` explicitly to `3` (write holding register).

::: tip One driver instance can serve multiple slaves
A single Modbus TCP driver process can serve multiple devices. When several slaves hang off the same gateway under
different unit IDs, `host` is the same and they are distinguished by the Point's `slaveId`; devices at different IPs
each hold their own cached connection.
:::

## Further reading

- [Driver overview](./index) — entry point and taxonomy for all drivers
- [Driver capability matrix](./matrix) — read/write/subscribe at a glance, including the Modbus TCP row
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Industrial Buses & Protocols](../foundations/fieldbus) — the addressing model and byte-order principles behind Modbus
  and friends
- [Modbus RTU Driver](./modbus-rtu) — the serial version of Modbus
