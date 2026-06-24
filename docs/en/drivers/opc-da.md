---
title: OPC DA Driver
---

# OPC DA Driver

> **`dc3-driver-opc-da` connects OPC DA servers to IoT DC3** — it targets an OPC DA server and periodically reads real-time tag (item) values over DCOM.

OPC DA (OPC Data Access) is the classic industrial data-access specification on the Windows platform. SCADA systems, HMI/configuration software, and PLC gateways mostly ship with an OPC DA Server that exposes field points as "items". This driver acts as an OPC DA client, using DCOM/J-Interop to connect to one or more OPC DA Servers and read real-time values according to the group name (group) and tag name (tag) configured on each [Point](../introduction/concepts/point).

Use case: an OPC DA Server (OPC DA 2.0 / 3.0) already exists on site, and its tags need to be brought into DC3 for unified collection and storage.

- **Driver name / code**: `OPC DA Driver` / `OpcDaDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the OPC DA Server)

::: warning DCOM is a prerequisite
OPC DA is built on Windows COM/DCOM. The Server must run on a Windows host reachable over DCOM, with DCOM permissions configured to allow remote access from the driver host. This is done at the OS level and is not part of the driver configuration.
:::

## Driver configuration (device-level `driver-attribute`)

When onboarding an OPC DA device, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | OPC DA host |
| CLSID | `clsId` | STRING | `F8582CF2-88FB-11D0-B850-00C0F0104305` | OPC DA server CLSID |
| Username | `username` | STRING | `dc3` | OPC DA username |
| Password | `password` | STRING | `dc3dc3` | OPC DA password |

::: tip CLSID is the OPC DA Server's COM identifier
OPC DA locates a Server through a COM class identifier (CLSID), not a TCP port. The CLSID is determined by the vendor of the target OPC DA Server and can be found in the registry on the server host or via an OPC server browsing tool. The default value is only a placeholder — always replace it with the real Server's CLSID when onboarding.
:::

## Point configuration (`point-attribute`)

Fill in these on each collected [Point](../introduction/concepts/point):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Group | `group` | STRING | `GROUP` | OPC DA item group name |
| Tag | `tag` | STRING | `TAG` | OPC DA item tag name |

OPC DA organizes tags under groups. The driver finds (or creates) the group by the Point's `group`, then locates the tag within that group by `tag` and reads its value; the COM variant type returned by the tag (such as VT_I4, VT_R8, VT_BOOL, VT_BSTR, etc.) is converted by the driver into the point value.

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds` — see [Device](../introduction/concepts/device) for the online-status mechanism.

## Minimal onboarding example

Onboard one tag from an OPC DA Server running at `192.168.1.10`:

1. Create a [Device](../introduction/concepts/device) using `OPC DA Driver`, and set the driver attributes `host=192.168.1.10`, `clsId=` (the real Server's CLSID), and `username`/`password` (a Windows account allowed to access the Server remotely).
2. Add a [Point](../introduction/concepts/point) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `group=Group1` and `tag=Channel1.Device1.Tag1` (per the target Server's actual naming).
3. Start the driver, and within 30 seconds you will see the collected value in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning The Server runs on Windows, and DCOM must allow access
The most common reason for a failed connection is not the driver but DCOM: the firewall is not opened, the remote account is not authorized, or the Server host has not enabled remote activation permissions. First verify with an OPC client tool on the driver host that you can connect to that CLSID, then onboard into DC3.
:::

::: tip group/tag must match the Server's naming
`group` is the OPC DA group name, and `tag` is the tag's full item name (naming styles differ by vendor, commonly something like `Channel1.Device1.TagA`). With a wrong name, the driver cannot find the tag under that group and the read fails — these names must come from browsing the target Server, not from guesswork.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute and Config](../introduction/concepts/attribute-config) — the three-layer origin of attributes like `host` / `group`
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — another industrial field-protocol driver
