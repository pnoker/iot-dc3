---
title: Industrial Buses & Protocols
---

<script setup>
import FieldbusDiagram from '../../.vitepress/theme/components/FieldbusDiagram.vue'
</script>


# Industrial Buses & Protocols

Devices on the industrial floor speak dozens of mutually unintelligible "dialects" — PLCs use vendor-proprietary
protocols, meters use metering standards, buildings use automation buses. This chapter makes clear what problem each
protocol solves, what model it communicates over, how it addresses a single data point, and what to weigh when choosing
one. By the end you will be able to read a field device's protocol parameters (register address, byte order, function
code) and know how IoT DC3 unifies them into point values.

> You are here: the "industrial wired" side of the network layer. For wireless and lightweight IoT protocols
> see [IoT Protocols & Wireless Networks](./iot-protocols); for where the upstream physical quantities come from
> see [Sensing & Measurement](./sensing).

## What This Layer Is / Why It Exists

To get a temperature from a sensor to the platform, the physical quantity is first turned into an electrical signal by a
transmitter, digitized by an acquisition device, and finally transported over the network via some **protocol**.
Industrial protocols are the language contract for that "last mile": they dictate how bytes are ordered, how addresses
are encoded, whether it is request-response or subscribe-push, and who is active versus passive.

Why are there so many of them, and why are they such a mess? Because they were born in different eras, industries, and
closed vendor ecosystems:

- **Historical baggage**. Modbus was created in 1979 for PLC serial communication and is still the most common protocol
  on the floor; OPC DA is bound to Windows COM/DCOM, a product of the PC era; only later came the cross-platform OPC UA.
- **Vendor walls**. Siemens' S7, Mitsubishi's MELSEC (MC protocol), Omron's FINS, Rockwell's EtherNet/IP — every PLC
  vendor has its own proprietary protocol, mutually incompatible, locking in customers.
- **Industry standards**. Power dispatch has IEC 60870-5-104, building automation has BACnet, utility metering has
  DLMS/COSEM, automotive and embedded have CAN — each industry set a standard for its own needs.

The result: the same "read one value" action addresses differently, frames differently, and represents data types
differently across protocols. The value of this layer is understanding the **common model** behind those differences —
once you see that they are all "read/write a value in some address space," the heterogeneity stops being scary.

## Key Technologies & Trade-offs

Set syntax details aside and the differences between industrial protocols concentrate in four dimensions: *
*communication model, addressing, byte order and data types, and polling cadence**. Grasp these four and any unfamiliar
protocol becomes approachable.

### Three communication models

- **Master/Slave / request-response**. A master polls slaves one by one with requests and waits for replies; the slave
  never speaks unprompted. Modbus, IEC 104 (client issues a general interrogation), S7, MELSEC, and FINS are essentially
  this model. Simple and deterministic, but the master gets nothing it does not ask for, and real-time-ness is bounded
  by the poll cycle.
- **Client/Server**. More symmetric than master/slave: an OPC UA client can browse the server's address space, read and
  write on demand, and **subscribe** — the server pushes when a value changes, sparing needless polling. Powerful, but
  with heavier handshake and session overhead.
- **Publish/Subscribe**. CAN broadcasts ID-tagged frames onto the bus and receivers filter by ID; MQTT (see the wireless
  side) subscribes by topic. No central polling, naturally suited to multi-receiver, event-driven scenarios.

### Addressing: register vs tag vs object

"Which point to read" is a completely different concept across protocols:

- **Numeric address (register / IOA / OBIS)**. Modbus locates coils/registers by function code + 0-based offset; IEC 104
  locates telemetry/telesignal by Information Object Address (IOA); DLMS locates COSEM objects by a 6-segment OBIS
  code (e.g. `1.0.1.8.0.255` = total active energy). Addresses are numbers, aligned by engineering convention.
- **Symbolic tag (Tag/Item)**. EtherNet/IP (CIP) addresses by tag name: a variable in the PLC is called `Motor_Speed`,
  and the driver reads/writes by name without caring about physical addresses; OPC addresses by NodeId/ItemId. More
  readable, but the name must match character for character.
- **Object + attribute**. BACnet models each quantity as an object (e.g. Analog Input #1) + attribute (Present_Value);
  DLMS's COSEM objects also have numbered attributes (attribute 2 = current value).

### Byte order and data types

Industrial devices are mostly Big-Endian, but when a 32-bit `FLOAT` spans two 16-bit registers, the **register order**
can still be swapped (four layouts: ABCD / CDAB / BADC / DCBA) — the single most common pitfall on the Modbus floor. The
protocol itself often only moves bytes; **how to interpret that byte string is decided by configuration**: whether it is
a 16-bit integer or a 32-bit float, low byte first or high byte first, whether to apply a multiplier and offset. Get the
byte order wrong and a float reads as a meaningless large number.

### Polling mechanism

Master/slave protocols fetch data by timed polling: too short a cycle overwhelms the device and bus, too long hurts
real-time-ness. Subscription protocols (OPC UA, CAN, MQTT) improve on this — they push only on change. In practice
points are often grouped by importance: critical quantities polled often, auxiliary ones rarely.

The diagram below groups this chapter's protocols by **application domain**, each domain mapping to a typical
communication model and addressing style:

<FieldbusDiagram lang="en" />

::: tip There is no "best" protocol, only the "most suitable"
Start from what the device itself supports — most field device protocols are fixed by the vendor, and you can only
adapt. When you do have a choice, weigh it: pick OPC UA for cross-vendor interoperability; pick a subscription protocol
to save bandwidth and go event-driven; pick DLMS for pure meter reading; pick Modbus or CAN for lightweight, low-cost
embedded.
:::

## Engineering Notes

- **Ports differ per protocol — do not mix them up**. Modbus TCP is `502`, EtherNet/IP is `44818`, IEC 104 is `2404`,
  DLMS over TCP is commonly `4059`. Use the wrong port and you will not connect.
- **Addresses are engineering conventions — verify before onboarding**. Modbus `offset` is a 0-based protocol address ("
  40001" should be `offset=0`, not `40001`); IEC 104's COT/CA/IOA byte lengths must match the peer exactly, or the whole
  frame parses misaligned; CIP tag names are case-sensitive and must match character for character.
- **Align data type and byte order with the device**. The Point's data type decides how bytes are assembled: a
  multi-register 32-bit float needs the right register order; a CAN frame payload is sliced by `dataOffset`/
  `dataLength`/`byteOrder`. Configure a `REAL` as a `DINT` and the float bytes will be parsed as an integer into a
  meaningless large number.
- **Distinguish read and write services/codes**. Modbus reads with `01/02/03/04` and writes with `05/06/15/16`; many
  protocols use different services for read and write, so a writable Point needs a separate write command configured.
- **Fail loudly — never fake success**. When a device is unreachable or parsing fails, the right behavior is to record
  the failure and back off, not to echo a cached value or pretend the write succeeded — the latter leads upper layers to
  decide on bad data.

## How It Lands in IoT DC3

Facing so many heterogeneous protocols, IoT DC3's strategy is: **one [protocol driver](../drivers/) per protocol,
confining protocol-layer differences inside the driver, and unifying upward into semantically labeled point values**.
Whether the underlying layer is a Modbus register, a CIP tag, or an OBIS code, it lands on the platform as the
same [PointValue](../introduction/concepts/point-value) of a [Point](../introduction/concepts/point), and the upper
layers — storage, query, alarming, AI — need not care about protocol details at all.

DC3 ships **28 drivers** in total, and most industrial protocols in this chapter have a corresponding one:

- [Modbus TCP](../drivers/modbus-tcp) / [Modbus RTU](../drivers/modbus-rtu) — Ethernet / serial Modbus master
- [OPC UA](../drivers/opc-ua) / [OPC DA](../drivers/opc-da) — OPC Unified Architecture client / classic Data Access
- [S7](../drivers/plcs7) — Siemens PLC
- [MELSEC](../drivers/melsec) — Mitsubishi PLC (MC protocol)
- [FINS](../drivers/fins) — Omron PLC
- [BACnet/IP](../drivers/bacnet-ip) — building automation
- [SNMP](../drivers/snmp) — network device monitoring
- [EtherNet/IP](../drivers/ethernet-ip), [IEC 104](../drivers/iec104), [DLMS](../drivers/dlms), [CAN](../drivers/can) —
  Rockwell CIP / power SCADA / smart meters / Controller Area Network

### Protocol parameters are driver attributes; the device instance fills the values

Every protocol parameter in this chapter lands in DC3 as an **attribute** declared by the driver, for which the device
instance fills a **config value** — this mechanism is covered
in [Attribute and Config](../introduction/concepts/attribute-config). Take Modbus TCP: driver-level attributes `host`/
`port` identify the slave, point-level attributes `slaveId`/`functionCode`/`offset` locate the register, and
write-command attributes specify the write function code and value template. Switch to EtherNet/IP and the point
attributes become `tagName`/`tagType`; switch to DLMS and they become `logicalName` (OBIS) / `attributeId`. The byte
order and register order discussed earlier likewise surface as corresponding point attributes.

In other words, the protocol knowledge in this chapter is not abstract theory — it maps directly to every field you fill
in when onboarding a device in DC3. Understand the protocol and you understand the driver's attribute table.

::: warning Some drivers are currently protocol skeletons (WIP)
Not every driver has finished its protocol-layer
I/O. [EtherNet/IP](../drivers/ethernet-ip), [IEC 104](../drivers/iec104), [DLMS](../drivers/dlms),
and [CAN](../drivers/can) are currently **skeleton implementations**: the attribute tables, collection cycles, and
addressing semantics are in place and can be filled in, but how far the actual protocol send/receive has progressed
varies —

- [IEC 104](../drivers/iec104), [DLMS](../drivers/dlms): `read()`/`write()` explicitly throw a "not implemented"
  exception to fail fast (the SDK records the failure and backs off, rather than faking success); the protocol I/O is
  not yet written.
- [EtherNet/IP](../drivers/ethernet-ip): the upper-layer flow (fetch by tag, encode/decode, socket connect) is in place,
  but the CIP protocol framing (`RegisterSession`/`ForwardOpen`/encapsulation frame) is not yet complete.
- [CAN](../drivers/can): `read()`/`write()` shell out to the Linux `can-utils` tools (`candump`/`cansend`) via
  `ProcessBuilder` and can actually send/receive, but byte slicing/type conversion (`dataOffset`/`dataLength`, etc.) and
  native SocketCAN I/O are not yet in place, and the `data` template on the write path is not actually wired up yet.

Treat them as **starting-point templates** for the corresponding protocol, not production-ready products. The
implementation status on each driver page is authoritative.
:::

::: info Modbus / OPC UA / S7 / BACnet and others are working drivers
[Modbus TCP](../drivers/modbus-tcp), [Modbus RTU](../drivers/modbus-rtu), [OPC UA](../drivers/opc-ua), [OPC DA](../drivers/opc-da), [S7](../drivers/plcs7), [MELSEC](../drivers/melsec), [BACnet/IP](../drivers/bacnet-ip),
and [SNMP](../drivers/snmp) have implemented protocol-layer read/write; [FINS](../drivers/fins) works but its current
read path only supports 16-bit types (see its driver page). Before onboarding, defer to the attribute table and notes on
each driver page.
:::

## Further Reading

- [IoT Protocols & Wireless Networks](./iot-protocols) — the wireless side of the network layer: MQTT, CoAP, LwM2M,
  NB-IoT
- [Sensing & Measurement](./sensing) — where the values the protocols carry come from: sensors, transduction, range and
  accuracy
- [IoT Technology Overview](./) — the four-layer reference architecture and a reading map for this part
- [Connectivity & Drivers](../drivers/) — how 28 drivers unify heterogeneous devices into one ingress
- [Attribute and Config](../introduction/concepts/attribute-config) — how protocol parameters become driver attributes
  filled by the device instance
