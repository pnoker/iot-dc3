---
title: OPC DA Driver
---

<script setup>
import OpcDaDiagram from '../../.vitepress/theme/components/OpcDaDiagram.vue'
</script>


# OPC DA Driver

`dc3-driver-opc-da` acts as an OPC DA client: it connects to a field OPC DA Server over Windows DCOM and periodically
reads real-time values by the group and tag configured on each Point, and can also write values back to a tag. This page
explains what OPC DA is, which attributes the driver exposes, how to troubleshoot connection failures, and its
implementation status inside IoT DC3.

> You are here: about to onboard a device that already has an OPC DA Server into DC3. Learn the protocol first, then
> fill in [Attribute configuration](#attribute-configuration), and consult [Troubleshooting](#troubleshooting) if you
> get
> stuck.

## Protocol background

OPC DA (OPC Data Access) is the classic industrial data-access specification on the Windows platform. Born in the PC
era, it applies Microsoft's COM/DCOM component technology to the industrial floor: SCADA systems, HMI/configuration
software, and PLC gateways mostly embed an OPC DA Server that exposes the points of underlying PLCs and instruments as "
items". Upper systems read and write through a uniform OPC DA client without caring about each PLC's proprietary
protocol. Common versions are OPC DA 2.0 / 3.0.

In the [four-layer IoT architecture](../foundations/fieldbus), OPC DA sits at the **network layer** — it is the "
universal translation layer" between field devices and upper systems. But its network transport is not an ordinary TCP
port; it is **DCOM (Distributed COM)**: the client locates a Server by a COM class identifier (CLSID), and calls travel
as DCOM remote procedure calls. This is why deploying and debugging OPC DA carries a heavy Windows footprint — and why
the cross-platform [OPC UA](./opc-ua) was created later.

<OpcDaDiagram lang="en" />

OPC DA organizes tags as "items under a group": the client first creates or finds a group on the Server, then adds the
items to read/write into that group, and reads or writes their values on demand. Each item value carries a COM variant
type (VARIANT), such as `VT_I4` (integer), `VT_R8` (double), `VT_BOOL`, `VT_BSTR` (string); the driver converts these
into point values accordingly.

::: warning DCOM is a prerequisite, and only on Windows
OPC DA is built on Windows COM/DCOM. The Server must run on a Windows host reachable over DCOM, with DCOM permissions
configured at the OS level to allow remote access from the driver host. This is not part of the driver configuration,
yet it is the decisive factor in whether a connection succeeds.
:::

## Attribute configuration

OPC DA onboarding parameters come in two layers: **driver attributes** describe "which Server to connect to" (
device-level, one set per device), and **point attributes** describe "which tag to read" (point-level, one set per
point). The two tables below come from the driver's `application.yml` (`driver-attribute` / `point-attribute`); the
defaults are the driver's built-in defaults.

### Driver attributes (device-level `driver-attribute`)

When onboarding an OPC DA device, fill in these [attributes](../introduction/concepts/attribute-config) on
the [Device](../introduction/concepts/device). `host` points at the Server host, `clsId` locates the specific OPC DA
Server, and `username` / `password` are the Windows credentials used for DCOM remote access:

| Attribute | code       | Type   | Default                                | Description                                        |
|-----------|------------|--------|----------------------------------------|----------------------------------------------------|
| Host      | `host`     | STRING | `localhost`                            | Host (IP or hostname) where the OPC DA Server runs |
| CLSID     | `clsId`    | STRING | `F8582CF2-88FB-11D0-B850-00C0F0104305` | COM class identifier of the target OPC DA Server   |
| Username  | `username` | STRING | `dc3`                                  | Windows username for DCOM remote access            |
| Password  | `password` | STRING | `dc3dc3`                               | Corresponding password                             |

::: tip CLSID is the OPC DA Server's COM identifier, not a port
OPC DA locates a Server through a COM class identifier (CLSID), not a TCP port. The CLSID is determined by the vendor of
the target OPC DA Server and can be found in the registry on the Server host or via an OPC server browsing tool. The
default in `application.yml` is only a placeholder — always replace it with the real Server's CLSID when onboarding.
:::

### Point attributes (`point-attribute`)

Fill in `group` and `tag` on each collected [Point](../introduction/concepts/point). The driver first finds (or creates)
the group on the Server by `group`, then locates the tag item within that group by `tag` and reads/writes its value:

| Attribute | code    | Type   | Default | Description                      |
|-----------|---------|--------|---------|----------------------------------|
| Group     | `group` | STRING | `GROUP` | OPC DA group name                |
| Tag       | `tag`   | STRING | `TAG`   | Full item name of the OPC DA tag |

::: info group / tag must match what the Server browses
Naming styles differ by vendor; `tag` is commonly something like `Channel1.Device1.TagA`. These names must match the
target Server's actual naming and should be taken from the Server's browse output, not guessed — a mismatched name makes
the driver fail to find the item in that group and the read fails.
:::

### Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (one read round every 30 seconds), controlled by
  `dc3.driver.schedule.read`.
- **Custom task**: `dc3.driver.schedule.custom` defaults to cron `0/5 * * * * ?`, but this driver's `schedule()` is an
  empty implementation (device liveness is owned by the SDK health job).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?`, with a lease timeout of `45 seconds` —
  see [Device](../introduction/concepts/device) for the online-status mechanism.

## Troubleshooting

When OPC DA fails to connect, the vast majority of problems lie in DCOM and naming, not in the driver code itself. Work
through them in order:

1. **Cannot connect / `ConnectorException`**: the most common root cause is DCOM. Check whether the Server host firewall
   allows DCOM ports, whether the remote account is granted "remote activation / remote access" permissions, and whether
   `username` / `password` is a valid account on that Windows host with rights to the Server. Best practice: first
   verify with a third-party OPC client tool on the driver host that you can connect to that CLSID, then onboard into
   DC3.
2. **Wrong CLSID**: `clsId` must be the target Server's real CLSID (the default is only a placeholder). Confirm it in
   the Server host registry or an OPC browsing tool; a wrong CLSID fails at the connection stage.
3. **Read fails / `ReadPointException`**: usually `group` or `tag` does not match the Server's naming, so the driver's
   `addItem` cannot find the tag under that group. Check the Point's `group` / `tag` character by character. On a read
   failure the driver **disposes and removes that device's connection**, and the next collection round reconnects
   automatically — if the tag name on the Server side stays wrong, it keeps failing.
4. **Write fails / `WritePointException` or `UnSupportException`**: writing only handles the
   `SHORT / INT / LONG / FLOAT / DOUBLE / BOOLEAN / STRING` point types. Only a completely unrecognized type code throws
   `UnSupportException`; a known type that is not in the handled set (such as `BYTE`) does not throw — the write is
   reported as a **failure returning `false`** (no value written). Also confirm the target item is writable on the
   Server and the current account has write permission.
5. **Device shows offline**: online state is maintained by the SDK health job (default 15-second check, 45-second
   lease). If the connection is repeatedly disposed and rebuilt due to DCOM jitter, the device toggles online/offline —
   stabilize the DCOM link first.
6. **Cross-platform limits**: the driver itself, based on J-Interop (a pure-Java DCOM implementation), can run on Linux,
   but the **peer Server must be a Windows DCOM endpoint**. Do not expect to connect to a non-Windows OPC DA endpoint.

## How it lands in IoT DC3

- **dc3.driver.code**: `OpcDaDriver` (a stable routing identifier tied to messaging routing and registration — do not
  change it casually).
- **Driver name / type**: `OPC DA Driver` / `DRIVER_CLIENT` (actively connects to the OPC DA Server).
- **Capabilities**: read ✓, write ✓, subscribe — , consistent with the [driver capability matrix](./matrix). Reads run
  on the periodic collection cycle (cron `0/30`); writes run on dispatched commands. The driver does not use OPC DA's
  change-subscription push; it polls actively on a cycle.

::: warning Implementation status: code is complete, but running depends on Windows / DCOM infrastructure
`read()` / `write()` in `OpcDaDriverCustomServiceImpl` are **fully implemented** on top of the bundled OpenSCADA OPC DA
client library (J-Interop): connections are established via `Server.connect()` and cached per device, reads call
`item.read()` and convert by COM variant type, and writes build a `JIVariant` and call `item.write()`. A stale "
work-in-progress skeleton / see TODO markers" warning lingers in the class javadoc, but there are no TODO markers left
in the method bodies and it no longer matches the implementation — trust the method bodies. The real barrier is not the
code but the requirement of a DCOM-configured Windows OPC DA Server to actually run end to end; without that environment
it cannot be verified.
:::

::: tip Minimal onboarding example
Onboard one tag from an OPC DA Server running at `192.168.1.10`:

1. Create a [Device](../introduction/concepts/device) using `OPC DA Driver`, and set the driver attributes
   `host=192.168.1.10`, `clsId=` (the real Server's CLSID), and `username` / `password` (a Windows account allowed to
   access the Server remotely).
2. Add a [Point](../introduction/concepts/point) to the [Profile](../introduction/concepts/profile) bound to the device,
   and set the point attributes `group=Group1` and `tag=Channel1.Device1.Tag1` (per the target Server's actual naming).
3. Start the driver, and within 30 seconds you will see the collected value
   in [PointValue](../introduction/concepts/point-value).
   :::

## Further reading

- [Driver overview](./index) — the unified model and registration mechanism for all drivers
- [Driver capability matrix](./matrix) — a quick read / write / subscribe lookup across drivers
- [Device onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Industrial Buses & Protocols](../foundations/fieldbus) — where OPC DA sits at the network layer and how it compares
  with peers
- [OPC UA Driver](./opc-ua) — the cross-platform, subscription-capable next-generation OPC
