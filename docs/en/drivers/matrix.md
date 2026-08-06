---
title: Driver Capability Matrix
---

# Driver Capability Matrix

This page gives an at-a-glance view of all **28 drivers** in IoT DC3 — their protocol category, read / write /
subscribe capabilities, and implementation status — so you can match a protocol to your needs fast. Each row links to
that driver's own page, where attributes, polling cadence, and a minimal onboarding example are spelled out.

Read / write / subscribe reflect each driver's actual current implementation: "✓" means the capability is in place, "—"
means the driver does not implement that direction, deliberately does not offer it by design, or is still a skeleton —
code is the source of truth (see the note after the tables). Subscribe / report means the driver passively receives
device pushes (listens on a port, network callbacks, device registration, etc.), as opposed to periodic polling.

The "Status" column summarizes each driver's overall maturity:

- **Complete**: read / write / subscribe are fully implemented per the protocol design and ready for production
  onboarding.
- **Usable**: the core path works, but there are local gaps (e.g. some data types, Observe, or health hooks); see the
  Notes and the driver page.
- **Skeleton**: protocol framing or transport is not yet implemented; currently serves as a structural reference only
  and cannot collect from real devices.

## Industrial Bus / PLC

These drivers act as masters (clients) that actively connect to the device, poll values
per [Point](../introduction/concepts/point) and write via [commands](../introduction/concepts/command); they do not
listen for pushes. `ethernet-ip` is currently a protocol skeleton — CIP framing is not yet complete.

| Driver (dc3.driver.code)                          | Category           | Read | Write | Subscribe/Report | Status   | Notes                                                      |
|---------------------------------------------------|--------------------|------|-------|------------------|----------|------------------------------------------------------------|
| [Modbus TCP](./modbus-tcp) (`ModbusTcpDriver`)    | Industrial Bus/PLC | ✓    | ✓     | —                | Complete | Ethernet Modbus master                                     |
| [Modbus RTU](./modbus-rtu) (`ModbusRtuDriver`)    | Industrial Bus/PLC | ✓    | ✓     | —                | Complete | Serial Modbus master                                       |
| [OPC UA](./opc-ua) (`OpcUaDriver`)                | Industrial Bus/PLC | ✓    | ✓     | —                | Complete | OPC Unified Architecture client                            |
| [OPC DA](./opc-da) (`OpcDaDriver`)                | Industrial Bus/PLC | ✓    | ✓     | —                | Complete | Classic OPC Data Access (DCOM)                             |
| [S7](./plcs7) (`PlcS7Driver`)                     | Industrial Bus/PLC | ✓    | ✓     | —                | Complete | Siemens PLC                                                |
| [MELSEC](./melsec) (`MelsecDriver`)               | Industrial Bus/PLC | ✓    | ✓     | —                | Complete | Mitsubishi PLC (MC protocol)                               |
| [FINS](./fins) (`FinsDriver`)                     | Industrial Bus/PLC | ✓    | ✓     | —                | Usable   | Omron PLC, supports 16/32-bit ints, float, string, and BCD |
| [EtherNet/IP](./ethernet-ip) (`EthernetIpDriver`) | Industrial Bus/PLC | —    | —     | —                | Skeleton | Rockwell / CIP, framing pending                            |

## SCADA / Power / Metering

Building, power, and metering protocols. `bacnet-ip` and `snmp` read and write actively; `sl651` is hydrology
telemetry — it opens a TCP server and passively receives reports, hence subscribe only; `iec104` and `dlms` are
currently skeletons.

| Driver (dc3.driver.code)                    | Category             | Read | Write | Subscribe/Report | Status   | Notes                                  |
|---------------------------------------------|----------------------|------|-------|------------------|----------|----------------------------------------|
| [BACnet/IP](./bacnet-ip) (`BacnetIpDriver`) | SCADA/Power/Metering | ✓    | ✓     | —                | Complete | Building automation                    |
| [IEC 104](./iec104) (`Iec104Driver`)        | SCADA/Power/Metering | —    | —     | —                | Skeleton | Power SCADA, protocol layer pending    |
| [DLMS](./dlms) (`DlmsDriver`)               | SCADA/Power/Metering | —    | —     | —                | Skeleton | Smart meters, transport pending        |
| [SL651](./sl651) (`Sl651Driver`)            | SCADA/Power/Metering | —    | —     | ✓                | Complete | Hydrology telemetry, TCP server ingest |
| [SNMP](./snmp) (`SnmpDriver`)               | SCADA/Power/Metering | ✓    | ✓     | —                | Complete | Network device monitoring              |

## IoT / Wireless

IoT and wireless drivers. `mqtt` is publish/subscribe — values arrive passively via subscription (no active read),
commands can be sent; `lwm2m` (embedded server, receives device registration and notifications) has read and write in
place but its Observe/subscribe is not yet implemented; `coap`, `http`, `ble`, and `can` are request-response active
read/write (`coap` Observe not implemented); `can` and `zigbee` are currently skeletons — `zigbee` only listens for
coordinator network state (not node join or attribute reports), and `can` is backed by can-utils.

| Driver (dc3.driver.code)            | Category     | Read | Write | Subscribe/Report | Status   | Notes                                                                    |
|-------------------------------------|--------------|------|-------|------------------|----------|--------------------------------------------------------------------------|
| [MQTT](./mqtt) (`MqttDriver`)       | IoT/Wireless | —    | ✓     | ✓                | Usable   | Publish/subscribe, values via subscription; `initial()` hook is skeleton |
| [CoAP](./coap) (`CoapDriver`)       | IoT/Wireless | ✓    | ✓     | —                | Usable   | RESTful for constrained devices, Observe not implemented                 |
| [LwM2M](./lwm2m) (`Lwm2mDriver`)    | IoT/Wireless | ✓    | ✓     | —                | Usable   | Embedded server, read/write ready, Observe not implemented               |
| [HTTP](./http) (`HttpDriver`)       | IoT/Wireless | ✓    | ✓     | —                | Complete | Generic HTTP polling                                                     |
| [BLE](./ble) (`BleDriver`)          | IoT/Wireless | ✓    | ✓     | —                | Complete | Bluetooth Low Energy GATT                                                |
| [Zigbee](./zigbee) (`ZigbeeDriver`) | IoT/Wireless | ✓    | ✓     | —                | Skeleton | Skeleton; subscribe (join/reports) not implemented                       |
| [CAN](./can) (`CanDriver`)          | IoT/Wireless | ✓    | —     | —                | Skeleton | Controller Area Network, backed by can-utils                             |

## Serial / Generic Network

Generic pass-through drivers that frame requests from a command template, sending and receiving actively without
listening.

| Driver (dc3.driver.code)                    | Category               | Read | Write | Subscribe/Report | Status   | Notes                       |
|---------------------------------------------|------------------------|------|-------|------------------|----------|-----------------------------|
| [Serial](./serial) (`SerialDriver`)         | Serial/Generic Network | ✓    | ✓     | —                | Complete | Generic serial pass-through |
| [TCP/UDP](./tcp-udp) (`TcpUdpDriver`) (raw) | Serial/Generic Network | ✓    | ✓     | —                | Complete | Generic socket pass-through |

## Database

Treat a table as a data source: reads go through `executeQuery`, writes through `executeUpdate`, driven by the SQL
template on each [Point](../introduction/concepts/point); no change subscription.

| Driver (dc3.driver.code)                        | Category | Read | Write | Subscribe/Report | Status   | Notes                   |
|-------------------------------------------------|----------|------|-------|------------------|----------|-------------------------|
| [MySQL](./mysql) (`MysqlDriver`)                | Database | ✓    | ✓     | —                | Complete | Read points from tables |
| [PostgreSQL](./postgresql) (`PostgresqlDriver`) | Database | ✓    | ✓     | —                | Complete | Read points from tables |
| [Oracle](./oracle) (`OracleDriver`)             | Database | ✓    | ✓     | —                | Complete | Read points from tables |
| [SQL Server](./sqlserver) (`SqlserverDriver`)   | Database | ✓    | ✓     | —                | Complete | Read points from tables |

## Virtual / Testing

Two drivers with no real device: `virtual` generates simulated read values by point type (write is a placeholder,
nothing hits a device); `listening-virtual` runs in reverse, opening TCP/UDP servers to receive external pushes and able
to write back to a device over the connection channel.

| Driver (dc3.driver.code)                                            | Category        | Read | Write | Subscribe/Report | Status   | Notes                                            |
|---------------------------------------------------------------------|-----------------|------|-------|------------------|----------|--------------------------------------------------|
| [Virtual](./virtual) (`VirtualDriver`)                              | Virtual/Testing | ✓    | —     | —                | Usable   | Generates simulated data, write is a placeholder |
| [Listening Virtual](./listening-virtual) (`ListeningVirtualDriver`) | Virtual/Testing | —    | ✓     | ✓                | Complete | TCP/UDP server ingest, can write back            |

::: info Capability marks are code-driven
The "✓ / —" in these tables reflect what each driver's `*DriverCustomServiceImpl` actually implements today: a "—" may
mean the protocol direction simply is not offered (e.g. `virtual` write, `mqtt` active read), or that the skeleton is
not yet filled in (e.g. `ethernet-ip`, `iec104`, `dlms`, `can`). The "Status" column is an overall maturity summary;
finer data-type or sub-capability gaps are flagged on each driver page via `::: warning` / `::: info`. For the final
behavior, consult that module's `read()` / `write()` / `initial()` source; this table is kept in sync as drivers evolve.
:::

## Further Reading

- [Drivers Overview](./index) — pick a protocol by category and open its page
- [Custom Driver](../development/driver-authoring) — implement your own protocol driver from the `virtual` template
