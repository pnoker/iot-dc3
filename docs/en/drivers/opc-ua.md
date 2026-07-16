---
title: OPC UA Driver
---

<script setup>
import OpcUaDiagram from '../../.vitepress/theme/components/OpcUaDiagram.vue'
</script>


# OPC UA Driver

`dc3-driver-opc-ua` connects OPC UA servers to IoT DC3: acting as an OPC UA client, it connects to one or more servers,
periodically reads node values according to the namespace and identifier configured on
each [Point](../introduction/concepts/point), and supports writing values to nodes. After reading this page you can
onboard an OPC UA device, configure its points, and troubleshoot the common reasons a connection fails.

## Protocol background

OPC UA (OPC Unified Architecture) is the cross-platform data-interoperability standard for industrial automation. PLCs,
SCADA, MES, and edge gateways commonly embed an OPC UA server that exposes field data as a "node tree." It supersedes
classic OPC (the Windows DCOM-based [OPC DA](./opc-da)), replacing it with the platform-independent `opc.tcp://` binary
protocol (HTTPS is also supported) and building security (certificates, signing, encryption) and information modeling
into the spec.

In the [four-layer IoT architecture](../foundations/fieldbus), OPC UA belongs to the **network layer (fieldbus)**: it is
the protocol boundary between shop-floor devices and upper systems, facing PLCs/controllers downward and handing data to
the data platform upward. Unlike Modbus, which addresses by register, or Ethernet/IP, which uses CIP tags, OPC UA
addresses with an **object model** — every data point is a node, uniquely identified by a **NodeId**. A NodeId has two
parts:

- **namespace index**: an integer that separates identifier spaces from different sources to avoid collisions.
- **identifier**: the node's name within that namespace — a string, numeric, or GUID. This driver uses **string
  identifiers**.

For example, `namespace=2` with identifier `Demo.Static.Float` uniquely locates the node named `Demo.Static.Float` under
namespace 2. The driver is built on Eclipse Milo and acts as the OPC UA client, actively connecting to the server — a
classic "master polling" model: it does not listen for device-pushed updates but reads nodes one by one on each
collection cycle.

- **Driver name / code**: `OPC UA Driver` / `OpcUaDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the server)

<OpcUaDiagram lang="en" />

## Attribute configuration

OPC UA onboarding parameters come in two layers: **driver attributes** on the [Device](../introduction/concepts/device)
say "which server to connect to," and **point attributes** on each [Point](../introduction/concepts/point) say "which
node to read/write." Both layers originate from the driver's `application.yml`; fill them in when creating a
device/point, or leave them blank to use the defaults below.

### Driver attributes (device-level `driver-attribute`)

These three attributes combine into the endpoint address `opc.tcp://<host>:<port><path>`, telling the driver which
endpoint of which OPC UA server to connect to.

| Attribute | code   | Type   | Default     | Description                     |
|-----------|--------|--------|-------------|---------------------------------|
| Host      | `host` | STRING | `localhost` | Server hostname or IP           |
| Port      | `port` | INT    | `18600`     | Server `opc.tcp` listening port |
| Path      | `path` | STRING | `/`         | Endpoint path                   |

For example, `host=192.168.1.20`, `port=4840`, `path=/milo` combine into `opc.tcp://192.168.1.20:4840/milo`. `host` and
`port` are required (the driver's `validate()` checks both are non-empty); `path` may keep the default `/`. During
endpoint discovery the driver connects to the **first** endpoint the server returns.

### Point attributes (`point-attribute`)

Fill in these two on each collected point; together they form the target node's NodeId.

| Attribute | code        | Type   | Default | Description                   |
|-----------|-------------|--------|---------|-------------------------------|
| Namespace | `namespace` | INT    | `5`     | Namespace index               |
| Tag       | `tag`       | STRING | `TAG`   | String identifier (node name) |

::: tip NodeId = namespace + tag
The driver combines `namespace` (the namespace index) and `tag` (the string identifier) into `NodeId(namespace, tag)`
for reads and writes. For example, `namespace=2` and `tag=Demo.Static.Float` resolve to the node named
`Demo.Static.Float` under namespace 2. The Point's data type (the `pointTypeFlag` of
the [Point](../introduction/concepts/point)) must match the node's actual value type — read values are stringified
before reporting, while writes pick the OPC UA data type from the Point type.
:::

::: info Write commands have no separate attribute
The OPC UA driver has **no `command-attribute`**. To write a value to a node it reuses the Point's own `namespace` and
`tag` to locate the target node, and the value type is decided by the Point type — the driver supports writing `INT` /
`LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` / `STRING` (see `writeNode()` in the source). So once a writable Point has its
`namespace` and `tag` set, you can issue write commands without filling in any command attribute.
:::

### Collection and health check

These come from `dc3.driver.schedule` and `dc3.driver.health` in `application.yml`; they are driver-level defaults, not
configured per device.

| Item             | Config key              | Default          | Description                           |
|------------------|-------------------------|------------------|---------------------------------------|
| Collection cycle | `schedule.read.cron`    | `0/30 * * * * ?` | Read all points once every 30 seconds |
| Health check     | `health.device.cron`    | `0/15 * * * * ?` | Probe once every 15 seconds           |
| Lease timeout    | `health.device.timeout` | `45` (seconds)   | Mark offline if not renewed in time   |

The health check runs an idempotent `connect()` probe with the device's client: if it connects, the device
is [online](../introduction/concepts/device); otherwise offline.

## Troubleshooting

::: warning The default port is 18600, not the standard 4840
The `port` default in the yml is `18600` (the port of the local built-in Milo sample server). In production, the vast
majority of OPC UA servers use the standard port `4840`, so when onboarding a real device you must set `port` to the
port the server actually listens on — do not just keep the default. `path` must also match the server's endpoint path:
some servers expose the root path (set `/`), others a sub-path (such as `/milo` or `/OPCUA/SimulationServer`). Getting
it wrong means the connection fails.
:::

- **Anonymous identity rejected**: the driver connects with an anonymous identity (`AnonymousProvider`). If the server
  enforces username/password and disallows anonymous access, the connection is rejected. Enable anonymous access on the
  server, or open an anonymous policy for that endpoint first.

- **Device stays offline**: the health check runs a `connect()` probe every 15 seconds and marks the device offline
  after repeated failures. First confirm the endpoint address formed from `host`/`port`/`path` is correct and
  reachable (`telnet host port` or `nc -vz host port` to verify the port is open), then confirm the server process is
  running and the firewall does not block the `opc.tcp` port.

- **Read value is null or status code is not Good**: when reading a node, if the `StatusCode` is not Good or the value
  is empty, the driver throws `ReadPointException` and **proactively disconnects and evicts that connection** (
  reconnecting on the next cycle). Common causes: a wrong NodeId (namespace or tag does not exist), no read permission
  on the node, or the node currently has no value. Use a tool such as UaExpert to verify the node
  `ns=<namespace>;s=<tag>` actually exists and is readable.

- **Read/write timeout**: the driver's connect timeout is 5 s, read timeout 1 s, write timeout 1 s. Network jitter or a
  slow server easily causes timeouts, which likewise evict the connection and trigger a reconnect. If the server is
  genuinely slow, investigate the link latency on the network side rather than raising the per-point timeout.

- **Write command has no effect**: the write value type must be one of `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` /
  `STRING`, and must be compatible with the actual data type of the server node; on a type mismatch the server returns a
  non-Good status and the write is treated as failed. Confirm the Point's `pointTypeFlag` matches the server node type
  and that the node is writable by the client.

- **Certificate-related errors**: on startup the driver generates a self-signed certificate `dc3-opc-ua-client.pfx` (
  PKCS12, default password `password`, overridable via the `OPCUA_KEYSTORE_PASSWORD` environment variable) under the
  working directory `dc3/opc-ua`. If that directory is not writable, or certificate generation fails, the driver **falls
  back to a plain anonymous connection** (no client certificate). When the server's security policy requires a client
  certificate, a plain anonymous connection fails the handshake — make sure the certificate directory is writable and
  the generated client certificate is "trusted" on the server.

## How it lands in IoT DC3

- **`dc3.driver.code`**: `OpcUaDriver` — the driver's stable routing identifier in the system. Data and command paths
  address by it, so do not change it casually.
- **Read**: ✓ implemented. On each collection cycle it calls `readValue()` per point to read the node, stringifies the
  value, and reports it as a [PointValue](../introduction/concepts/point-value).
- **Write**: ✓ implemented. On a write command it reuses the Point's `namespace`/`tag` to locate the node and writes
  `INT`/`LONG`/`FLOAT`/`DOUBLE`/`BOOLEAN`/`STRING` based on the Point type.
- **Subscribe/report**: — not provided. This driver is a master-polling model; it does not subscribe to the OPC UA
  server's data-change notifications (Subscription/MonitoredItem), only reads on a cycle.

This matches the [driver capability matrix](./matrix) (read ✓ / write ✓ / subscribe —).

::: info Implementation status: available
`OpcUaDriverCustomServiceImpl`'s `read()` / `write()` / `health()` / `validate()` / `event()` are all complete
implementations (built on Eclipse Milo), not a skeleton. Reading nodes, writing the six types, connection caching and
reconnect-on-failure, self-signed certificate generation, and clearing connections on device update/delete are all in
place — it can be pointed at a real OPC UA server directly.
:::

### Minimal onboarding example

Onboard a float node on the endpoint `opc.tcp://192.168.1.20:4840/milo`:

1. Create a [Device](../introduction/concepts/device) using `OPC UA Driver`, and set the driver attributes
   `host=192.168.1.20`, `port=4840`, `path=/milo`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to
   the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `namespace=2`,
   `tag=Demo.Static.Float`.
3. Start the driver, and within 30 seconds you will see the collected value
   in [PointValue](../introduction/concepts/point-value).

See [Device onboarding](../operation/device-onboarding) for the full walkthrough.

## Further reading

- [Driver overview](./index) — all drivers and the common onboarding model
- [Driver capability matrix](./matrix) — read / write / subscribe capabilities at a glance
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Industrial buses & protocols](../foundations/fieldbus) — the network layer and addressing model OPC UA belongs to
- [OPC DA Driver](./opc-da) — the classic OPC (DCOM) version
