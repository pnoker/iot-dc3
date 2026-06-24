---
title: Zigbee Driver
---

# Zigbee Driver

> **`dc3-driver-zigbee` connects Zigbee devices to IoT DC3** — it joins a Zigbee network through a serial coordinator, periodically reads node data via ZCL attributes, and supports commands that write values to ZCL attributes.

Zigbee is a low-power wireless mesh protocol, widely used for sensor nodes in smart home and building automation (temperature/humidity, door contacts, switches, lights, etc.). Devices do not connect to an IP network directly; instead they form their own Zigbee network, with a single **coordinator dongle** (plugged into the host running the driver over a USB serial port) handling all ingress and egress. Each Zigbee device is uniquely identified by its **IEEE address** (64-bit extended address); a specific data point inside a device is addressed by three levels: **endpoint → cluster → attribute (ZCL attribute)**. This driver acts as a Zigbee application-layer client, mapping each [Point](../introduction/concepts/point) to one ZCL attribute for reads and writes.

::: warning Work in progress (skeleton)
This driver is currently a **skeleton** — protocol-level I/O is not yet fully implemented. Treat it as an onboarding template, not a production-ready driver. See the [Pitfalls](#pitfalls) at the end of this page; the key limitations: `initial()` currently **hardcodes** the serial port `/dev/ttyUSB0` and baud rate `115200` (it does not read the driver attributes below), the device-level health check always returns online, and the write command only logs and **does not actually dispatch** the value.
:::

- **Driver name / code**: `Zigbee Driver` / `ZigbeeDriver`
- **Type**: `DRIVER_CLIENT` (connects to the coordinator and polls nodes)

## Driver configuration (device-level `driver-attribute`)

When onboarding a Zigbee device, fill these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). These configure the **coordinator side** (one coordinator serves the whole Zigbee network):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Serial Port | `serialPort` | STRING | `/dev/ttyUSB0` | Zigbee coordinator serial port |
| Baud Rate | `baudRate` | INT | `115200` | Serial port baud rate |
| Dongle Type | `dongleType` | STRING | `TELEGESIS` | Coordinator dongle type (TELEGESIS, EMBER, CONBEE) |
| PAN ID | `panId` | INT | `0` | PAN ID (0=auto) |
| Channel | `channel` | INT | `0` | Channel (0=auto, 11-26) |

## Point configuration (`point-attribute`)

Each [Point](../introduction/concepts/point) uses IEEE address + endpoint + cluster + attribute to locate one ZCL attribute in the Zigbee network:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Node IEEE Address | `nodeIeeeAddress` | STRING | (empty) | Zigbee node IEEE address (e.g. `00158D0001234567`) |
| Endpoint ID | `endpointId` | INT | `1` | Endpoint ID |
| Cluster ID | `clusterId` | INT | `0` | Cluster ID (e.g. `1026`=Temperature Measurement) |
| Attribute ID | `attributeId` | INT | `0` | Attribute ID (e.g. `0`=Measured Value) |

::: tip Three-level addressing: endpoint / cluster / attribute
A Zigbee node may have several endpoints (multi-function devices); each endpoint hosts several ZCL clusters (e.g. Temperature Measurement `1026`, Relative Humidity `1029`), and each cluster holds several attributes. `cluster` + `attribute` decide which physical quantity is read — the Point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) must match the actual type of that ZCL attribute.
:::

## Write command configuration (`command-attribute`)

Writable points also fill these on the write command (same four-level addressing as the point, but pointing at the target attribute to write):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Node IEEE Address | `nodeIeeeAddress` | STRING | (empty) | Zigbee node IEEE address |
| Endpoint ID | `endpointId` | INT | `1` | Endpoint ID |
| Cluster ID | `clusterId` | INT | `0` | Cluster ID for writing |
| Attribute ID | `attributeId` | INT | `0` | Attribute ID for writing |

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (reads ZCL attributes every 30 seconds).
- **Health / online**: the device health check defaults to cron `0/15 * * * * ?` with a `45-second` lease timeout — see [Device](../introduction/concepts/device) for the online-state mechanism. Note that in the current skeleton the device-level health check always returns online (see [Pitfalls](#pitfalls)).

## Minimal onboarding example

Onboard a temperature sensor node with IEEE address `00158D0001234567`:

1. Create a [Device](../introduction/concepts/device) with `Zigbee Driver`, and fill the driver attributes `serialPort=/dev/ttyUSB0`, `baudRate=115200`, `dongleType=TELEGESIS`, `panId=0`, `channel=0`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and fill the point attributes `nodeIeeeAddress=00158D0001234567`, `endpointId=1`, `clusterId=1026` (Temperature Measurement cluster), `attributeId=0` (Measured Value).
3. Make sure the node has joined the coordinator's Zigbee network, start the driver, and within 30 seconds you will see the value in [Point Value](../introduction/concepts/point-value).

## Pitfalls

::: warning The write command does not actually dispatch yet
The current skeleton write path (`writeAttribute`) validates that the node/endpoint/cluster/attribute exist, but only logs a line — it **does not actually write the value to the ZCL attribute**. Until the write capability is completed, a point with a write command will appear to succeed while the device state stays unchanged — do not rely on it for real control.
:::

::: warning Serial port and baud rate are currently hardcoded
The current `initial()` hardcodes `/dev/ttyUSB0` and `115200` and **does not read** the `serialPort` / `baudRate` driver attributes. If the coordinator is not on `/dev/ttyUSB0`, you must first wire up the configuration-reading logic, otherwise the attributes have no effect. Also, only the Telegesis dongle adapter is currently bundled, so a different `dongleType` value will not switch the adapter yet.
:::

::: tip IEEE address is 16 hex digits, no separators
`nodeIeeeAddress` is the 64-bit IEEE extended address, written as 16 continuous hex characters (e.g. `00158D0001234567`) — no colons and no `0x` prefix. It is fixed at the factory and can be found in the coordinator/gateway device list; a wrong address yields a "node not found" error.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute & Config](../introduction/concepts/attribute-config) — where attributes like `serialPort` / `clusterId` come from across the three layers
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding flow
- [BLE Driver](./ble) — onboarding another kind of low-power short-range wireless device
