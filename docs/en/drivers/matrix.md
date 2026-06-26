---
title: Driver Capability Matrix
---

# Driver Capability Matrix

This page gives an at-a-glance view of all **28 drivers** in IoT DC3 — their protocol category and their read / write / subscribe capabilities — so you can match a protocol to your needs fast. Each row links to that driver's own page, where attributes, polling cadence, and a minimal onboarding example are spelled out.

Read / write / subscribe reflect each driver's actual current implementation: "✓" means the capability is in place, "—" means the driver does not implement that direction, deliberately does not offer it by design, or is still a skeleton — code is the source of truth (see the note after the tables). Subscribe / report means the driver passively receives device pushes (listens on a port, network callbacks, device registration, etc.), as opposed to periodic polling.

## Industrial Bus / PLC

These drivers act as masters (clients) that actively connect to the device, poll values per [Point](../introduction/concepts/point) and write via [commands](../introduction/concepts/command); they do not listen for pushes. `ethernet-ip` is currently a protocol skeleton — CIP framing is not yet complete.

| Driver (dc3.driver.code) | Category | Read | Write | Subscribe/Report | Notes |
|---|---|---|---|---|---|
| [Modbus TCP](./modbus-tcp) (`ModbusTcpDriver`) | Industrial Bus/PLC | ✓ | ✓ | — | Ethernet Modbus master |
| [Modbus RTU](./modbus-rtu) (`ModbusRtuDriver`) | Industrial Bus/PLC | ✓ | ✓ | — | Serial Modbus master |
| [OPC UA](./opc-ua) (`OpcUaDriver`) | Industrial Bus/PLC | ✓ | ✓ | — | OPC Unified Architecture client |
| [OPC DA](./opc-da) (`OpcDaDriver`) | Industrial Bus/PLC | ✓ | ✓ | — | Classic OPC Data Access (DCOM) |
| [S7](./plcs7) (`PlcS7Driver`) | Industrial Bus/PLC | ✓ | ✓ | — | Siemens PLC |
| [MELSEC](./melsec) (`MelsecDriver`) | Industrial Bus/PLC | ✓ | ✓ | — | Mitsubishi PLC (MC protocol) |
| [FINS](./fins) (`FinsDriver`) | Industrial Bus/PLC | ✓ | ✓ | — | Omron PLC |
| [EtherNet/IP](./ethernet-ip) (`EthernetIpDriver`) | Industrial Bus/PLC | — | — | — | Rockwell / CIP, skeleton pending |

## SCADA / Power / Metering

Building, power, and metering protocols. `bacnet-ip` and `snmp` read and write actively; `sl651` is hydrology telemetry — it opens a TCP server and passively receives reports, hence subscribe only; `iec104` and `dlms` are currently skeletons.

| Driver (dc3.driver.code) | Category | Read | Write | Subscribe/Report | Notes |
|---|---|---|---|---|---|
| [BACnet/IP](./bacnet-ip) (`BacnetIpDriver`) | SCADA/Power/Metering | ✓ | ✓ | — | Building automation |
| [IEC 104](./iec104) (`Iec104Driver`) | SCADA/Power/Metering | — | — | — | Power SCADA, skeleton pending |
| [DLMS](./dlms) (`DlmsDriver`) | SCADA/Power/Metering | — | — | — | Smart meters, transport pending |
| [SL651](./sl651) (`Sl651Driver`) | SCADA/Power/Metering | — | — | ✓ | Hydrology telemetry, TCP server ingest |
| [SNMP](./snmp) (`SnmpDriver`) | SCADA/Power/Metering | ✓ | ✓ | — | Network device monitoring |

## IoT / Wireless

IoT and wireless drivers. `mqtt` is publish/subscribe — values arrive passively via subscription (no active read), commands can be sent; `lwm2m` (embedded server, receives device registration and notifications) covers read, write, and subscribe; `coap`, `http`, `ble`, and `can` are request-response active read/write; `zigbee` is currently a skeleton that only listens for coordinator network state — it does not yet listen for node join or attribute reports, so its subscribe capability is not implemented.

| Driver (dc3.driver.code) | Category | Read | Write | Subscribe/Report | Notes |
|---|---|---|---|---|---|
| [MQTT](./mqtt) (`MqttDriver`) | IoT/Wireless | — | ✓ | ✓ | Publish/subscribe, values via subscription |
| [CoAP](./coap) (`CoapDriver`) | IoT/Wireless | ✓ | ✓ | — | RESTful for constrained devices |
| [LwM2M](./lwm2m) (`Lwm2mDriver`) | IoT/Wireless | ✓ | ✓ | ✓ | Embedded server, device registration & notifications |
| [HTTP](./http) (`HttpDriver`) | IoT/Wireless | ✓ | ✓ | — | Generic HTTP polling |
| [BLE](./ble) (`BleDriver`) | IoT/Wireless | ✓ | ✓ | — | Bluetooth Low Energy GATT |
| [Zigbee](./zigbee) (`ZigbeeDriver`) | IoT/Wireless | ✓ | ✓ | — | Skeleton; subscribe (join/reports) not implemented |
| [CAN](./can) (`CanDriver`) | IoT/Wireless | ✓ | — | — | Controller Area Network |

## Serial / Generic Network

Generic pass-through drivers that frame requests from a command template, sending and receiving actively without listening.

| Driver (dc3.driver.code) | Category | Read | Write | Subscribe/Report | Notes |
|---|---|---|---|---|---|
| [Serial](./serial) (`SerialDriver`) | Serial/Generic Network | ✓ | ✓ | — | Generic serial pass-through |
| [TCP/UDP](./tcp-udp) (`TcpUdpDriver`) (raw) | Serial/Generic Network | ✓ | ✓ | — | Generic socket pass-through |

## Database

Treat a table as a data source: reads go through `executeQuery`, writes through `executeUpdate`, driven by the SQL template on each [Point](../introduction/concepts/point); no change subscription.

| Driver (dc3.driver.code) | Category | Read | Write | Subscribe/Report | Notes |
|---|---|---|---|---|---|
| [MySQL](./mysql) (`MysqlDriver`) | Database | ✓ | ✓ | — | Read points from tables |
| [PostgreSQL](./postgresql) (`PostgresqlDriver`) | Database | ✓ | ✓ | — | Read points from tables |
| [Oracle](./oracle) (`OracleDriver`) | Database | ✓ | ✓ | — | Read points from tables |
| [SQL Server](./sqlserver) (`SqlserverDriver`) | Database | ✓ | ✓ | — | Read points from tables |

## Virtual / Testing

Two drivers with no real device: `virtual` generates simulated read values by point type (write is a placeholder, nothing hits a device); `listening-virtual` runs in reverse, opening TCP/UDP servers to receive external pushes and able to write back to a device over the connection channel.

| Driver (dc3.driver.code) | Category | Read | Write | Subscribe/Report | Notes |
|---|---|---|---|---|---|
| [Virtual](./virtual) (`VirtualDriver`) | Virtual/Testing | ✓ | — | — | Generates simulated data, write is a placeholder |
| [Listening Virtual](./listening-virtual) (`ListeningVirtualDriver`) | Virtual/Testing | — | ✓ | ✓ | TCP/UDP server ingest, can write back |

::: info Capability marks are code-driven
The "✓ / —" in these tables reflect what each driver's `*DriverCustomServiceImpl` actually implements today: a "—" may mean the protocol direction simply is not offered (e.g. `virtual` write, `mqtt` active read), or that the skeleton is not yet filled in (e.g. `ethernet-ip`, `iec104`, `dlms`). For the final behavior, consult that module's `read()` / `write()` / `initial()` source; this table is kept in sync as drivers evolve.
:::

## Further Reading

- [Drivers Overview](./index) — pick a protocol by category and open its page
- [Custom Driver](../development/driver-authoring) — implement your own protocol driver from the `virtual` template
