---
title: CAN Bus Driver
---

# CAN Bus Driver

> **`dc3-driver-can` connects CAN bus devices to IoT DC3**——it listens for CAN frames on a SocketCAN interface, parses frame payloads into readings according to each [Point](../introduction/concepts/point) configuration, and writes values by sending command frames onto the bus.

CAN (Controller Area Network) is a fieldbus widely used in automotive and industrial automation: nodes do not communicate point-to-point by address——they broadcast frames tagged with a **CAN ID** (identifier) onto the bus, and receivers filter the frames they care about by CAN ID. A single frame carries up to 8 bytes of payload. It is common in vehicle ECUs, battery management systems (BMS), servo drives, and sensor nodes.

This driver joins the bus as a node: it captures frames matching a CAN ID on a Linux SocketCAN interface (e.g. `can0`) to read, optionally sends a "request" frame first and then reads the "response", and writes values via command frames.

- **Driver name / code**: `CAN Bus Driver` / `CanDriver`
- **Type**: `DRIVER_CLIENT` (actively sends/receives frames on the bus)

::: warning Skeleton implementation
This driver is a starting template: protocol-level I/O is not yet fully implemented. `read()`/`write()` shell out to the Linux `can-utils` tools (`candump`/`cansend`), and `health()` checks the interface via `ip link show`. Native SocketCAN integration must be completed before production use.
:::

## Driver configuration (device-level `driver-attribute`)

When onboarding a CAN device, fill these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Interface | `interfaceName` | STRING | `can0` | SocketCAN interface name |
| Bitrate | `bitrate` | INT | `500000` | CAN bus bitrate (bps) |
| Frame Format | `frameFormat` | STRING | `STANDARD` | STANDARD(11bit) or EXTENDED(29bit) |

## Point configuration (`point-attribute`)

Fill these on each [Point](../introduction/concepts/point) to be read:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| CAN ID | `canId` | STRING | (empty) | CAN identifier to match (hex) |
| Data Offset | `dataOffset` | INT | `0` | Byte offset within the frame payload |
| Data Length | `dataLength` | INT | `1` | Number of bytes to read |
| Data Format | `dataFormat` | STRING | `INT` | INT/UINT/HEX |
| Byte Order | `byteOrder` | STRING | `LITTLE` | Byte order (e.g. LITTLE) |
| Request CAN ID | `requestCanId` | STRING | (empty) | CAN ID of an optional request frame |
| Request Data | `requestData` | STRING | (empty) | Payload of the optional request frame |

::: tip Request-driven reads
Many CAN devices only answer with data after receiving a "request" frame. If both `requestCanId` and `requestData` are set, the driver sends a request frame before reading, then listens for the response frame matching `canId`; leave both empty to passively listen for frames the bus broadcasts periodically.
:::

## Write command configuration (`command-attribute`)

Writable points also need these on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| CAN ID | `canId` | STRING | (empty) | CAN identifier to write to |
| Data | `data` | STRING | `${value}` | Frame data, rendered from the command argument (supports `${value}`) |

## Collection and health

- **Collection cycle**: default cron `0/30 * * * * ?` (capture one round of frames every 30 seconds).
- **Health / online**: device health check defaults to cron `0/15 * * * * ?` with a `45 second` lease timeout——the driver uses `ip link show <interface>` to determine whether the interface exists; see [Device](../introduction/concepts/device) for the online-state mechanism.

## Minimal onboarding example

Onboard a node on `can0` that periodically broadcasts temperature with CAN ID `123`:

1. Create a [Device](../introduction/concepts/device) with `CAN Bus Driver`, set driver attributes `interfaceName=can0`, `bitrate=500000`, `frameFormat=STANDARD`.
2. Add a temperature [Point](../introduction/concepts/point) (`READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, with point attributes `canId=123`, `dataOffset=0`, `dataLength=2`, `dataFormat=INT`, `byteOrder=LITTLE`, leaving `requestCanId`/`requestData` empty (passive listening).
3. Start the driver; the reading appears in [PointValue](../introduction/concepts/point-value) within 30 seconds.

## Pitfalls

::: warning The driver must run on Linux with can-utils installed
Low-level read/write relies on `candump`/`cansend`, and the health check relies on `ip link show`, plus an available SocketCAN interface is required. The driver process must run on a Linux host with access to that interface; on macOS/Windows or without `can-utils`, reads fail and the device stays offline.
:::

::: tip Use hex CAN IDs without a prefix
Fill `canId`/`requestCanId` the way `can-utils` expects——e.g. `123` for a standard frame, and the 29-bit ID in its hex form for extended frames; do not add a `0x` prefix. `frameFormat` (STANDARD/EXTENDED) must match the device's actual frame format.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes and Config](../introduction/concepts/attribute-config) — where attributes like `interfaceName` / `canId` come from across the three layers
- [Device onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — another industrial fieldbus integration
