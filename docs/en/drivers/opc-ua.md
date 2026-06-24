---
title: OPC UA Driver
---

# OPC UA Driver

> **`dc3-driver-opc-ua` connects OPC UA servers to IoT DC3** — it targets nodes on an OPC UA server, periodically reads node values, and supports writing values to nodes.

OPC UA (OPC Unified Architecture) is the cross-platform data-interoperability standard for industrial automation. PLCs, SCADA, MES, and edge gateways commonly embed an OPC UA server that exposes field data as a "node tree." Each node is uniquely identified by a **NodeId**, which combines a "namespace index" and an "identifier." This driver acts as the OPC UA client, using Eclipse Milo to connect over the `opc.tcp://` binary protocol to one or more servers, then reading and writing node values according to the namespace and identifier configured on each [Point](../introduction/concepts/point).

- **Driver name / code**: `OPC UA Driver` / `OpcUaDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the server)

## Driver configuration (device-level `driver-attribute`)

When onboarding an OPC UA server device, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | OPC UA host |
| Port | `port` | INT | `18600` | OPC UA port |
| Path | `path` | STRING | `/` | OPC UA endpoint path |

The three combine into the endpoint address `opc.tcp://<host>:<port><path>`, e.g. `opc.tcp://192.168.1.20:4840/milo`.

## Point configuration (`point-attribute`)

Fill in these on each collected [Point](../introduction/concepts/point); together they locate one OPC UA node (NodeId):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Namespace | `namespace` | INT | `5` | OPC UA namespace index |
| Tag | `tag` | STRING | `TAG` | OPC UA node tag name |

::: tip NodeId = namespace + tag
The driver combines `namespace` (the namespace index) and `tag` (the string identifier) into a NodeId for reads and writes. For example, `namespace=2` and `tag=Demo.Static.Float` resolve to the node named `Demo.Static.Float` under namespace 2. The Point's data type (the `pointTypeFlag` of the [Point](../introduction/concepts/point)) must match the node's actual value type.
:::

## Write command configuration

The OPC UA driver has **no separate `command-attribute`**. To write a value to a node, it reuses the Point's own `namespace` and `tag` to locate the target node, and the value type is decided by the Point type — the driver supports writing `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` / `STRING`. So once a writable Point has its `namespace` and `tag` set, you can issue write commands without filling in any command attribute.

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds` (the health check makes a connect probe to the server) — see [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

Onboard a float node on the endpoint `opc.tcp://192.168.1.20:4840/milo`:

1. Create a [Device](../introduction/concepts/device) using `OPC UA Driver`, and set the driver attributes `host=192.168.1.20`, `port=4840`, `path=/milo`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `namespace=2`, `tag=Demo.Static.Float`.
3. Start the driver, and within 30 seconds you will see the collected value in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning The default port is 18600, not the standard 4840
The `port` default in the yml is `18600` (the port of the local built-in Milo sample server). In production, the vast majority of OPC UA servers use the standard port `4840`, so when onboarding a real device you must set `port` to the port the server actually listens on — do not just keep the default.

`path` must also match the server's endpoint path: some servers expose the root path (set `/`), others a sub-path (such as `/milo` or `/OPCUA/SimulationServer`). Getting it wrong means the connection fails.
:::

::: tip Anonymous identity, certificate auto-generated
The driver connects with an anonymous identity (AnonymousProvider). On startup, if no client certificate exists under the working directory `dc3/opc-ua`, the driver auto-generates a self-signed certificate (`dc3-opc-ua-client.pfx`) to use for the connection — no manual placement required. If the server enforces username/password auth and disallows anonymous, the anonymous connection is rejected — enable anonymous on the server first.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute and Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `namespace`
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [OPC DA Driver](./opc-da) — the classic OPC (DCOM) version
