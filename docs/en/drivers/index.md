---
title: Drivers
---

# Drivers

> IoT DC3 ships **28 protocol drivers** covering industrial buses, PLC/SCADA, IoT, databases, and virtual testing. Each driver is a standalone service (`dc3-driver-*`) that registers itself and the [config attributes](../introduction/concepts/attribute-config) it accepts with the manager on startup, then reads [points](../introduction/concepts/point) and writes via [commands](../introduction/concepts/command).

For the general onboarding flow see [Device Onboarding](../operation/device-onboarding); for the driver model see the [Driver](../introduction/concepts/driver) concept. Pick your protocol by category below:

## Industrial Bus / PLC / SCADA

| Driver | Protocol | Notes |
|---|---|---|
| [Modbus TCP](./modbus-tcp) | Modbus TCP | Ethernet Modbus master |
| [Modbus RTU](./modbus-rtu) | Modbus RTU | Serial Modbus master |
| [OPC UA](./opc-ua) | OPC UA | OPC Unified Architecture client |
| [OPC DA](./opc-da) | OPC DA | Classic OPC Data Access |
| [S7](./plcs7) | Siemens S7 | Siemens PLC |
| [MELSEC](./melsec) | Mitsubishi MELSEC | Mitsubishi PLC |
| [FINS](./fins) | Omron FINS | Omron PLC |
| [EtherNet/IP](./ethernet-ip) | EtherNet/IP (CIP) | Rockwell / CIP |
| [BACnet/IP](./bacnet-ip) | BACnet/IP | Building automation |
| [IEC 104](./iec104) | IEC 60870-5-104 | Power SCADA |
| [DLMS](./dlms) | DLMS / COSEM | Smart meters |
| [SL651](./sl651) | SL651 | Hydrology monitoring |
| [SNMP](./snmp) | SNMP | Network device monitoring |

## IoT / Wireless

| Driver | Protocol | Notes |
|---|---|---|
| [MQTT](./mqtt) | MQTT | IoT message bus |
| [CoAP](./coap) | CoAP | RESTful for constrained devices |
| [LwM2M](./lwm2m) | LwM2M | Lightweight device management |
| [HTTP](./http) | HTTP | Generic HTTP polling |
| [BLE](./ble) | Bluetooth LE | Low-energy Bluetooth |
| [Zigbee](./zigbee) | Zigbee | Short-range wireless |
| [CAN](./can) | CAN | Controller Area Network |

## Serial / Generic Network

| Driver | Protocol | Notes |
|---|---|---|
| [Serial](./serial) | Serial | Generic serial port |
| [TCP/UDP](./tcp-udp) | TCP / UDP | Generic socket ingestion |

## Database

| Driver | Source | Notes |
|---|---|---|
| [MySQL](./mysql) | MySQL | Read points from tables |
| [PostgreSQL](./postgresql) | PostgreSQL | Read points from tables |
| [Oracle](./oracle) | Oracle | Read points from tables |
| [SQL Server](./sqlserver) | SQL Server | Read points from tables |

## Virtual / Testing

| Driver | Notes |
|---|---|
| [Virtual](./virtual) | Generate simulated data with no real device — for demos and load testing |
| [Listening Virtual](./listening-virtual) | Listen on a port for device pushes — for integration testing |

## Further Reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration
- [Attribute & Config](../introduction/concepts/attribute-config) — the three layers of driver / point / command attributes
- [Device Onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [Module Map](../architecture/modules) — where drivers sit in the overall architecture
- [Industrial Buses & Protocols](../foundations/fieldbus) · [IoT Protocols & Wireless](../foundations/iot-protocols) — the systematic knowledge behind the protocols
