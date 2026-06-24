---
title: BLE Driver
---

# BLE Driver

> **`dc3-driver-ble` brings Bluetooth Low Energy (BLE) devices into IoT DC3**—it connects to a BLE peripheral, periodically reads the values of its GATT characteristics, and supports commands that write values back to characteristics.

BLE (Bluetooth Low Energy) is the most common short-range wireless protocol on wearables, environmental sensors, beacons, and portable meters. A BLE device organizes its data as a **GATT** (Generic Attribute Profile) tree: a peripheral contains several **Services**, each Service holds several **Characteristics**, every Characteristic is identified by a **UUID**, and a characteristic value is just a stretch of bytes. This driver acts as the BLE host (central): through a Bluetooth adapter on the host it connects to the peripheral, reads and writes bytes against the service/characteristic UUIDs configured on each [Point](../introduction/concepts/point), then parses those bytes into a [PointValue](../introduction/concepts/point-value) according to the configured format.

- **Driver name / code**: `Bluetooth LE Driver` / `BleDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively connects to the peripheral)

## Driver configuration (device-level `driver-attribute`)

When onboarding a BLE device, fill these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Adapter Name | `adapterName` | STRING | `hci0` | Host Bluetooth adapter name |
| Device Address | `deviceAddress` | STRING | (empty) | BLE device MAC address (e.g. `AA:BB:CC:DD:EE:FF`) |
| Connection Timeout | `connectionTimeout` | INT | `10000` | Connection timeout in milliseconds |

::: tip One peripheral = one device
`deviceAddress` uniquely identifies a peripheral, so one BLE peripheral maps to one [Device](../introduction/concepts/device) on the platform. A single adapter (`hci0`) can connect to several peripherals at once, distinguished by each device's `deviceAddress`.
:::

## Point configuration (`point-attribute`)

Each polled [Point](../introduction/concepts/point) maps to one GATT characteristic on the peripheral:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Service UUID | `serviceUuid` | STRING | (empty) | GATT Service UUID |
| Characteristic UUID | `characteristicUuid` | STRING | (empty) | GATT Characteristic UUID |
| Read Format | `readFormat` | STRING | `UTF8` | Data format (UTF8, HEX, INT16, UINT16, FLOAT) |
| Byte Order | `byteOrder` | STRING | `BIG` | Byte order (BIG, LITTLE) |

::: tip Read Format decides how bytes become a value
A characteristic read returns raw bytes, which the driver parses per `readFormat`: `UTF8` as a string, `HEX` as a hex string, `INT16`/`UINT16`/`FLOAT` as numbers. `INT16`/`UINT16`/`FLOAT` are also affected by `byteOrder` (`BIG` big-endian, `LITTLE` little-endian); `UTF8` and `HEX` are independent of byte order.
:::

## Write command configuration (`command-attribute`)

Writable points additionally need these on the write command:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Service UUID | `serviceUuid` | STRING | (empty) | GATT Service UUID |
| Characteristic UUID | `characteristicUuid` | STRING | (empty) | UUID of the characteristic to write |

::: warning Writes are sent as UTF-8 bytes
The driver encodes the command value to UTF-8 bytes and writes them straight into the characteristic—no format conversion like `readFormat`. To write a number or hex value, express it as a string the target characteristic accepts at the layer above.
:::

## Polling and health

- **Poll interval**: default cron `0/30 * * * * ?` (one read every 30 seconds).
- **Health / online**: the device health check defaults to cron `0/15 * * * * ?` with a `45 second` lease timeout. A device is online when it is connected and reachable (BLE link `isOnline() && isConnected()`); see [Device](../introduction/concepts/device) for the online state mechanism.

## Minimal onboarding example

Bring in a BLE thermometer at MAC `AA:BB:CC:DD:EE:FF`:

1. Create a [Device](../introduction/concepts/device) with `Bluetooth LE Driver`, setting driver attributes `adapterName=hci0`, `deviceAddress=AA:BB:CC:DD:EE:FF`, and leaving `connectionTimeout` at the default `10000`.
2. Add a temperature [Point](../introduction/concepts/point) (`READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device. Set the point attributes `serviceUuid` and `characteristicUuid` to that temperature characteristic's UUIDs, with `readFormat=FLOAT` and `byteOrder=LITTLE` (per the peripheral's datasheet).
3. Start the driver; within 30 seconds the polled reading appears in [PointValue](../introduction/concepts/point-value).

## Common pitfalls

::: warning The host needs a working Bluetooth adapter and the TinyB native library
This driver uses the Sputnikdev Bluetooth Manager with the TinyB transport, relying on a physical Bluetooth adapter on the host and the TinyB native library. With no adapter, or a wrong `adapterName` (the default is `hci0`), the device stays offline forever. For containerized deployments you must pass the host's Bluetooth capabilities through into the container.
:::

::: warning UUIDs must match the peripheral's datasheet exactly
`serviceUuid` and `characteristicUuid` must match the GATT the peripheral actually exposes, character for character (including short/long form). A wrong or mis-cased UUID means the characteristic is not found and both reads and writes fail. Confirm the peripheral's service/characteristic UUIDs with a BLE scanning tool before filling them in.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attribute & Config](../introduction/concepts/attribute-config) — the three-tier origin of attributes like `adapterName` / `serviceUuid`
- [Device onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — an Ethernet industrial-bus example
