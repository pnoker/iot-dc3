---
title: SNMP Driver
---

<script setup>
import SnmpDiagram from '../../.vitepress/theme/components/SnmpDiagram.vue'
</script>


# SNMP Driver

`dc3-driver-snmp` connects SNMP-capable network and data-center devices to IoT DC3: it targets an OID on the device's
MIB tree, periodically sends SNMP GET to read values, and supports sending SNMP SET to an OID to write values. By the
end you will be able to onboard a router, switch, or UPS and collect its port status, traffic, temperature/humidity, and
other metrics as [PointValues](../introduction/concepts/point-value).

## Protocol background

SNMP (Simple Network Management Protocol) is the most common management protocol for network and data-center equipment.
It runs over UDP on default port `161`. It belongs to the [network layer](../foundations/fieldbus) of the IoT four-layer
architecture — like Modbus and OPC on the industrial floor, it solves the problem of "reading/writing a value in some
address space," except its devices are not PLCs and meters but IP network elements: routers, switches, UPSes, rack PDUs,
printers, server NICs.

Each managed device holds a MIB (Management Information Base) tree, and every readable/writable data point on that tree
has a unique object identifier OID (Object Identifier, e.g. `1.3.6.1.2.1.1.1.0`). A manager uses the OID to locate "
which value to read":

- **Scalar objects** end with the instance identifier `.0`, e.g. the system description `sysDescr` is
  `1.3.6.1.2.1.1.1.0`;
- **Table entries** (such as per-port traffic or status) end with a row index, e.g. `...10.1` and `...10.2` for ports 1
  and 2.

SNMP has three versions: v1 / v2c / v3. v1 and v2c use a cleartext `community` string as the passphrase — simple to
configure and the most common in the field; v3 introduces USM (User-based Security Model) for authentication and
encryption. Built on the SNMP4J library, this driver acts as an SNMP manager and actively connects to devices: a read
Point sends a GET to its OID, a write Point sends a SET to the OID, and one long-lived SNMP session is reused per
device.

The typical use case is data-center and network monitoring — bandwidth, port up/down, CPU/memory utilization, room
temperature/humidity, UPS battery, etc. Any SNMP-capable device can be managed once its OIDs are configured.

<SnmpDiagram lang="en" />

## Attribute configuration

SNMP connection parameters and collection targets are filled in at two levels: connecting to a device uses **driver
attributes** (device-level), and locating each data point uses **point attributes** (point-level). The attribute names,
types, and defaults all come from the driver `application.yml` `driver-attribute` / `point-attribute` /
`command-attribute` definitions.

### Driver attributes (device-level `driver-attribute`)

When onboarding an SNMP device, fill in these [Attributes](../introduction/concepts/attribute-config) on
the [Device](../introduction/concepts/device). `host` / `port` decide which device and UDP port to connect to,
`version` + `community` are the v1/v2c identity passphrase, and `timeout` / `retries` control request fault tolerance.

| Attribute         | code              | Type   | Default     | Description                                          |
|-------------------|-------------------|--------|-------------|------------------------------------------------------|
| Host              | `host`            | STRING | `127.0.0.1` | SNMP device IP                                       |
| Port              | `port`            | INT    | `161`       | SNMP port (standard 161)                             |
| Version           | `version`         | STRING | `v2c`       | SNMP version (`v1` / `v2c`)                          |
| Community         | `community`       | STRING | `public`    | Community string (read-only / read-write passphrase) |
| USM Username      | `usmUsername`     | STRING | (empty)     | SNMPv3 USM username (not used in v1/v2c)             |
| USM Auth Protocol | `usmAuthProtocol` | STRING | `MD5`       | SNMPv3 auth protocol (MD5/SHA)                       |
| USM Auth Password | `usmAuthPassword` | STRING | (empty)     | SNMPv3 auth password                                 |
| Timeout           | `timeout`         | INT    | `5000`      | Request timeout in milliseconds                      |
| Retries           | `retries`         | INT    | `1`         | Number of request retries                            |

::: warning The three USM fields are reserved for SNMPv3 and have no effect today
`usmUsername` / `usmAuthProtocol` / `usmAuthPassword` are the SNMPv3 USM security fields. They are declared in
`application.yml`, but the driver's `buildTarget()` builds only a `CommunityTarget` and sets `version1` or `version2c`
based on `version`. The current implementation supports only v1 and v2c, so these three are never read even if filled
in; set `version` to `v1` or `v2c`.
:::

`validate()` marks `host` / `port` / `version` / `community` as required — missing any one fails device validation.

### Point attributes (`point-attribute`)

On each collected [Point](../introduction/concepts/point), fill in `oid` to specify which data point to read; `snmpType`
labels that value's SNMP data type.

| Attribute | code       | Type   | Default        | Description                                                                     |
|-----------|------------|--------|----------------|---------------------------------------------------------------------------------|
| OID       | `oid`      | STRING | (empty)        | SNMP object identifier (e.g. `1.3.6.1.2.1.1.1.0`)                               |
| SNMP Type | `snmpType` | STRING | `OCTET_STRING` | SNMP data type (INTEGER/GAUGE32/COUNTER32/OCTET_STRING/TIMETICKS/IPADDRESS/OID) |

::: tip The OID selects which data point is collected; snmpType is mainly for writes
On read, the driver sends a GET to the configured `oid` and reports the returned `VariableBinding` value as-is via
`variable.toString()` as the [PointValue](../introduction/concepts/point-value) — `snmpType` is not used in reads. Its
real role is on write: `createVariable()` uses it to convert the string into the correct SNMP variable type.
`validatePoint()` marks `oid` as required, so a read Point missing `oid` fails validation.
:::

### Writes reuse the point attributes — no separate write command needed

Writes and reads share the same `oid` / `snmpType` on the Point: `write()` (SET) reads `oid` and `snmpType` from
`pointConfig` (point-attribute), sends a SET to that OID, and builds the value per `snmpType`. A writable Point only
needs `oid` and `snmpType` configured on the point — no need to repeat them on a write command.

`snmpType` values supported by `createVariable()`: `INTEGER`/`INTEGER32`, `GAUGE32`/`COUNTER32`/`UNSIGNED_INTEGER32`,
`COUNTER64`, `TIMETICKS`, `OID`, `IPADDRESS`, `NULL`; anything else is treated as `OCTET_STRING`.

::: info `command-attribute` is not read by the write path today
`application.yml` declares a `command-attribute` (`oid` / `snmpType`), but `write()`'s signature only takes
`driverConfig` and `pointConfig` — command attributes are not passed in, and this driver does not override `execute()`.
So that `command-attribute` is a placeholder declaration today and is never read on write. Configure a writable Point's
`oid` / `snmpType` on the Point itself, not on a write command — otherwise the write falls back to the point defaults (
`oid` empty, `snmpType=OCTET_STRING`).
:::

### Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read every 30 seconds, from `schedule.read.cron`).
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`. `health()` decides
  online by "whether the device has an established SNMP session" — if `clientMap` holds the device it is treated as
  online; otherwise it tries to build a session, and online once built. See [Device](../introduction/concepts/device)
  for the online-state mechanism.

::: info An established SNMP session does not mean the device is reachable
`getConnector()` builds a local UDP transport (`DefaultUdpTransportMapping`); once `listen()` succeeds it is cached as "
online" without probing the device. So after a device goes offline, the health check may briefly still report online — a
real failure only surfaces on the next `read()` timeout, at which point the driver does `clientMap.remove(deviceId)` to
destroy the session, and the next health check flips to offline.
:::

## Troubleshooting

::: warning Scalar OIDs usually end with `.0` — don't drop it
A scalar (single-value) object's OID ends with the instance identifier `.0`, e.g. `sysDescr` is `1.3.6.1.2.1.1.1.0`, not
`1.3.6.1.2.1.1.1`. Table entries (such as per-port traffic) end with a row index instead (e.g. `...10.1`, `...10.2`).
When the OID is wrong, the device returns `noSuchObject`/`noSuchInstance`, and `variable.toString()` reports it as a
plain string PointValue — it looks like "collected" but is invalid data, which is easy to be misled by during diagnosis.
:::

::: warning A wrong community times out silently
SNMP uses the `community` string as its passphrase. If the community does not match, or the device does not grant that
community access, the device usually sends no reply; the `response.getResponse()` from `snmp.send()` is `null` and the
driver throws `ReadPointException("SNMP response is null...")`. This shows up as a request timeout rather than an
explicit "auth failed." Before onboarding, confirm the `host`, `port`, `community`, and `oid` combination returns a
value on the command line with `snmpget -v2c -c public <host> 1.3.6.1.2.1.1.1.0`.
:::

::: warning Firewall blocks UDP 161 / device has SNMP disabled
SNMP runs over UDP, not TCP; many firewalls pass TCP by default but block UDP, and switches/servers often have the SNMP
agent disabled by default. The symptom is again a timeout. First confirm the target device has the SNMP service enabled
and that inbound UDP `161` traffic from the manager to the device is allowed.
:::

Before onboarding, verify the link on the command line with net-snmp tools — the driver uses the same SNMP4J semantics,
so if the command line returns nothing, don't create the device in DC3 yet:

```bash
# Minimal connectivity check
snmpget -v2c -c public 192.168.1.20:161 1.3.6.1.2.1.1.1.0
# If no value comes back, rule out each: host pingable? UDP 161 open? community correct? SNMP enabled?
snmpwalk -v2c -c public 192.168.1.20:161 1.3.6.1.2.1.1   # walk the system subtree to see if the device answers
```

::: warning version only accepts v1 / v2c — v3 is treated as v2c
The driver's `buildTarget()` only recognizes `v1` (case-insensitive); any other value — including `v3` — falls through
to the `version2c` branch. If the device only allows SNMPv3, this driver cannot connect, and there is no explicit "
version unsupported" error — it just shows up as a community-validation timeout. Make sure the device allows v1/v2c
access.
:::

::: warning A write returning true does not mean the device accepted it
`write()` returns `true` as soon as it gets a non-null `response`; it does not check the response PDU's `errorStatus`.
Some devices return a response with an error code (rather than no reply) for a read-only OID or an unauthorized write,
and the driver still treats it as success. After writing a critical parameter, read the OID back to confirm it took
effect.
:::

## How it lands in IoT DC3

- **`dc3.driver.code`**: `SnmpDriver` (driver name `SNMP Driver`, type `DRIVER_CLIENT`, actively connects to devices).
  This is a stable routing identifier and must not be changed casually.
- **Read / write / subscribe capability**: read ✓, write ✓, subscribe —, consistent with
  the [driver capability matrix](./matrix). The driver polls actively as an SNMP manager and does not listen for device
  pushes, so there is no subscribe direction.

::: info Implementation status: available
In `SnmpDriverCustomServiceImpl`, `read()` (GET), `write()` (SET), `getConnector()` (session management), `health()`,
and `event()` (destroying a session on device update/delete) are all implemented; the SNMP4J v1/v2c send/receive path is
complete and usable. Known boundaries: SNMPv3/USM is not wired up (see the three USM fields above), `write()` does not
check the response `errorStatus`, and the health check is a local session-liveness check rather than an end-to-end
probe. These are deliberate trade-offs in the current implementation and do not affect normal v1/v2c collection and
writes.
:::

Minimal onboarding example — onboard a switch at IP `192.168.1.20:161` with community `public`, collecting its system
description (`sysDescr`, OID `1.3.6.1.2.1.1.1.0`):

1. Choose `SNMP Driver` to create a [Device](../introduction/concepts/device), filling the driver attributes
   `host=192.168.1.20`, `port=161`, `version=v2c`, `community=public`.
2. Add a description [Point](../introduction/concepts/point) (`pointTypeFlag=STRING`, `READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, with the point attribute
   `oid=1.3.6.1.2.1.1.1.0`.
3. Start the driver; within 30 seconds the device's system description string appears in
   the [PointValue](../introduction/concepts/point-value).

See [Device onboarding](../operation/device-onboarding) for the full flow.

## Further reading

- [Drivers overview](./index) — the general driver model, registration, and lifecycle
- [Driver capability matrix](./matrix) — read/write/subscribe capability of all 28 drivers
- [Device onboarding](../operation/device-onboarding) — a full onboarding flow
- [Industrial Buses & Protocols](../foundations/fieldbus) — the network layer SNMP belongs to, and the "protocol
  parameters are driver attributes" model
- [IoT Protocols & Wireless Networks](../foundations/iot-protocols) — the wireless and lightweight IoT half of the
  network layer
- [CoAP Driver](./coap) — another lightweight IoT protocol over UDP
