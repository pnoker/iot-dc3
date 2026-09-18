# DC3 Driver SNMP

## Overview

`dc3-driver-snmp` is the SNMP driver of the IoT DC3 platform. It communicates with SNMP-enabled devices using the SNMP4J
library over UDP, supporting SNMP v1 and v2c. Point reads issue an SNMP GET against a configured OID, and write commands
issue an SNMP SET, with one SNMP session cached per device.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-snmp
- **Driver Name**: SNMP Driver

## Driver Attributes (Device-level)

| Attribute         | Code            | Type   | Default   | Description                              |
|-------------------|-----------------|--------|-----------|------------------------------------------|
| Host              | host            | STRING | 127.0.0.1 |                                          |
| Port              | port            | INT    | 161       |                                          |
| Version           | version         | STRING | v2c       | SNMP version (v1/v2c)                    |
| Community         | community       | STRING | public    |                                          |
| USM Username      | usmUsername     | STRING |           | SNMPv3 USM username (not used in v1/v2c) |
| USM Auth Protocol | usmAuthProtocol | STRING | MD5       | SNMPv3 auth protocol (MD5/SHA)           |
| USM Auth Password | usmAuthPassword | STRING |           |                                          |
| Timeout           | timeout         | INT    | 5000      | Request timeout in milliseconds          |
| Retries           | retries         | INT    | 1         |                                          |

## Point Attributes

| Attribute | Code     | Type   | Default      | Description                                                                     |
|-----------|----------|--------|--------------|---------------------------------------------------------------------------------|
| OID       | oid      | STRING |              | SNMP object identifier (e.g. 1.3.6.1.2.1.1.1.0)                                 |
| SNMP Type | snmpType | STRING | OCTET_STRING | SNMP data type (INTEGER/GAUGE32/COUNTER32/OCTET_STRING/TIMETICKS/IPADDRESS/OID) |

## Command Attributes (write)

| Attribute | Code     | Type   | Default      | Description |
|-----------|----------|--------|--------------|-------------|
| OID       | oid      | STRING |              |             |
| SNMP Type | snmpType | STRING | OCTET_STRING |             |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable SNMP agent. The agent host/port, SNMP version, community string, and per-point OIDs are supplied through the
driver and point attributes above. The driver attributes include SNMPv3 USM fields, but the current implementation
supports SNMP v1 and v2c.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-snmp -am package
java -jar dc3-driver/dc3-driver-snmp/target/dc3-driver-snmp.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-snmp -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
