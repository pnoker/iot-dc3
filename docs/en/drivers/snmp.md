---
title: SNMP Driver
---

# SNMP Driver

> **`dc3-driver-snmp` connects SNMP network devices to IoT DC3**——it targets an OID on the device, periodically sends SNMP GET to read values, and supports sending SNMP SET to an OID to write values.

SNMP (Simple Network Management Protocol) is the most common management protocol for network and data-center equipment. It runs over UDP on default port `161`. A managed device (router, switch, UPS, rack PDU, printer, server NIC, etc.) holds a MIB tree, and every readable/writable data point on that tree has a unique object identifier OID (e.g. `1.3.6.1.2.1.1.1.0`). Built on the SNMP4J library, this driver acts as an SNMP manager and actively connects to devices: a read [Point](../introduction/concepts/point) sends an SNMP GET to its OID, a write Point sends an SNMP SET to the OID, and one SNMP session is reused per device.

Use cases: data-center and network monitoring (bandwidth, port status, CPU/memory, temperature/humidity, UPS battery, etc.)—any SNMP-capable device can be collected once its OIDs are configured.

- **Driver name / code**: `SNMP Driver` / `SnmpDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the device)

## Driver configuration (device-level `driver-attribute`)

When onboarding an SNMP device, fill in these [Attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `127.0.0.1` | SNMP device IP |
| Port | `port` | INT | `161` | SNMP port (standard 161) |
| Version | `version` | STRING | `v2c` | SNMP version (v1/v2c) |
| Community | `community` | STRING | `public` | Community string (read-only / read-write passphrase) |
| USM Username | `usmUsername` | STRING | (empty) | SNMPv3 USM username (not used in v1/v2c) |
| USM Auth Protocol | `usmAuthProtocol` | STRING | `MD5` | SNMPv3 auth protocol (MD5/SHA) |
| USM Auth Password | `usmAuthPassword` | STRING | (empty) | SNMPv3 auth password |
| Timeout | `timeout` | INT | `5000` | Request timeout in milliseconds |
| Retries | `retries` | INT | `1` | Number of request retries |

::: tip The three USM fields are reserved for SNMPv3
`usmUsername` / `usmAuthProtocol` / `usmAuthPassword` are the SNMPv3 USM security fields. The current implementation supports only v1 and v2c, so these three have no effect even if filled in; set `version` to `v1` or `v2c`.
:::

## Point configuration (`point-attribute`)

Fill in these on each collected [Point](../introduction/concepts/point):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| OID | `oid` | STRING | (empty) | SNMP object identifier (e.g. `1.3.6.1.2.1.1.1.0`) |
| SNMP Type | `snmpType` | STRING | `OCTET_STRING` | SNMP data type (INTEGER/GAUGE32/COUNTER32/OCTET_STRING/TIMETICKS/IPADDRESS/OID) |

::: tip The OID selects which data point is collected
On read, the driver sends a GET to the configured `oid` and reports the returned variable value as-is as the [PointValue](../introduction/concepts/point-value). A read Point must have `oid` set, or validation fails. `snmpType` does not affect the read value; it is mainly used by write commands to build the correct variable type.
:::

## Write command configuration (`command-attribute`)

Writable Points additionally need these on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| OID | `oid` | STRING | (empty) | The OID to write |
| SNMP Type | `snmpType` | STRING | `OCTET_STRING` | SNMP data type of the value to write |

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read every 30 seconds).
- **Health / online**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`——see [Device](../introduction/concepts/device) for the online-state mechanism.

## Minimal onboarding example

Onboard a switch at IP `192.168.1.20:161` with community `public`, collecting its system description (`sysDescr`, OID `1.3.6.1.2.1.1.1.0`):

1. Choose `SNMP Driver` to create a [Device](../introduction/concepts/device), filling the driver attributes `host=192.168.1.20`, `port=161`, `version=v2c`, `community=public`.
2. Add a description [Point](../introduction/concepts/point) (`pointTypeFlag=STRING`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, with the point attribute `oid=1.3.6.1.2.1.1.1.0`.
3. Start the driver; within 30 seconds the device's system description string appears in the [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning Scalar OIDs usually end with `.0`—don't drop it
A scalar (single-value) object's OID ends with the instance identifier `.0`, e.g. `sysDescr` is `1.3.6.1.2.1.1.1.0`, not `1.3.6.1.2.1.1.1`. Table entries (such as per-port traffic) end with a row index instead (e.g. `...10.1`, `...10.2`). A wrong OID makes GET return empty and the read fail.
:::

::: warning A wrong community times out silently
SNMP uses the `community` string as its passphrase. If the community does not match or the device does not grant that community access, the device usually sends no reply—you see a request timeout rather than an explicit error. Before onboarding, confirm the `host`, `port`, `community`, and `oid` combination returns a value using `snmpget`/`snmpwalk` on the command line.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes & config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `community` / `oid`
- [Device onboarding](../operation/device-onboarding) — a full onboarding flow
- [CoAP Driver](./coap) — another lightweight IoT protocol over UDP
