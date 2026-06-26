---
title: BLE Driver
---

# BLE Driver

`dc3-driver-ble` brings Bluetooth Low Energy (BLE) devices into IoT DC3. It acts as the BLE host (central): through a Bluetooth adapter on the host it connects to a peripheral, periodically reads the bytes of its GATT characteristics, parses them into a [PointValue](../introduction/concepts/point-value) according to the configured format, and supports writing command values back to characteristics. By the end you can set `adapterName`/`deviceAddress` on the [Device](../introduction/concepts/device), set the service/characteristic UUIDs and parse format on each [Point](../introduction/concepts/point), and diagnose the common "device stays offline / no value read" problems.

## Protocol background

BLE (Bluetooth Low Energy) is the most common short-range wireless protocol on wearables, environmental sensors, beacons, and portable meters—typically ten-meter range, low data rate, and extremely low power, running for months to years on a coin cell. In the four-layer IoT reference architecture, BLE sits on the **wireless access** side of the [network layer](../foundations/iot-protocols): it only governs "how the signal travels over the air" and does not itself define how upper-layer messages are organized, so a BLE device usually needs a host or gateway to relay its data onto the internet—and this driver plays exactly that central-host role.

A BLE device organizes its data as a **GATT** (Generic Attribute Profile) tree: a peripheral contains several **Services**, each Service holds several **Characteristics**, every Characteristic is identified by a **UUID**, and a characteristic value is just a stretch of raw bytes. To read a data point the host issues a read against the characteristic located by "service UUID + characteristic UUID"; to write, it writes bytes into the target characteristic. This driver maps each [Point](../introduction/concepts/point) to one characteristic on the peripheral, reading and writing bytes against the configured UUIDs, then parsing those bytes into a PointValue per the configured format.

::: info GATT decides "how you address a data point"
Modbus addresses with "function code + register address"; BLE addresses with "service UUID + characteristic UUID"—both are "first connect to the device, then locate a data point inside it". Understanding this mapping keeps you from confusing the Service with the Characteristic during configuration.
:::

For the underlying transport, this driver uses the Sputnikdev Bluetooth Manager framework with the TinyB transport, which handles scanning, connection, and GATT read/write on top of the host's physical Bluetooth adapter (default `hci0`).

## Attribute configuration

Onboarding a BLE device means filling [attributes](../introduction/concepts/attribute-config) at three levels: device-level connection parameters (`driver-attribute`), the addressing and parsing parameters of each polled point (`point-attribute`), and the write-command parameters of each writable point (`command-attribute`). The attributes, types, and defaults below all come from the driver's `application.yml` (the `dc3-driver-ble` module).

### Driver attributes (`driver-attribute`)

Driver attributes answer "which adapter connects to which peripheral". `adapterName` names the Bluetooth adapter on the host (usually `hci0`, `hci1` on Linux); `deviceAddress` is the peripheral's MAC address, serving as its unique identifier. `connectionTimeout` is declared in `application.yml` but is not read by the current driver code (see the note below). Fill one set per BLE device on the [Device](../introduction/concepts/device):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Adapter Name | `adapterName` | STRING | `hci0` | Host Bluetooth adapter name |
| Device Address | `deviceAddress` | STRING | (empty) | BLE device MAC address (e.g. `AA:BB:CC:DD:EE:FF`) |
| Connection Timeout | `connectionTimeout` | INT | `10000` | Connection timeout in milliseconds; **not read by the current implementation**, see the note below |

::: info `connectionTimeout` currently has no effect
`connectionTimeout` is declared in `application.yml` and can be set on the device, but the `dc3-driver-ble` code never reads it—link setup is done by `bluetoothManager.getCharacteristicGovernor(charUrl, true)` waiting for the governor to become ready, without passing any timeout. Changing this value does not affect connection behavior; it is a reserved attribute.
:::

::: tip One peripheral = one device
`deviceAddress` uniquely identifies a peripheral, so one BLE peripheral maps to one [Device](../introduction/concepts/device) on the platform. A single adapter (`hci0`) can connect to several peripherals at once, distinguished by each device's `deviceAddress`; the driver caches a connection controller (governor) per `deviceId`, establishing the link on the first read or write.
:::

### Point attributes (`point-attribute`)

Point attributes answer "which characteristic on the peripheral to read, and how to parse the bytes that come back". Fill one set per polled [Point](../introduction/concepts/point):

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Service UUID | `serviceUuid` | STRING | (empty) | GATT Service UUID |
| Characteristic UUID | `characteristicUuid` | STRING | (empty) | GATT Characteristic UUID |
| Read Format | `readFormat` | STRING | `UTF8` | Data format (UTF8, HEX, INT16, UINT16, FLOAT) |
| Byte Order | `byteOrder` | STRING | `BIG` | Byte order (BIG, LITTLE) |

`serviceUuid` + `characteristicUuid` together locate one characteristic on the peripheral. A characteristic read returns raw bytes, which the driver parses per `readFormat`: `UTF8` as a string (default), `HEX` as a hex string, `INT16`/`UINT16`/`FLOAT` as numbers. Those three numeric formats are also affected by `byteOrder` (`BIG` big-endian, `LITTLE` little-endian); `UTF8` and `HEX` are independent of byte order.

::: tip Parse format follows the peripheral's datasheet
Which `readFormat` and `byteOrder` to use depends on what the peripheral's firmware actually puts in the characteristic—for example, if a thermometer writes temperature as a little-endian 4-byte float into a characteristic, set `readFormat=FLOAT` and `byteOrder=LITTLE`. A wrong format does not raise an error; it just parses the bytes into a meaningless value, so check the peripheral's GATT spec before filling these in.
:::

### Write command attributes (`command-attribute`)

`application.yml` declares `serviceUuid`/`characteristicUuid` under `command-attribute`, but **the write path does not read them**:

| Attribute | code | Type | Default | Description |
|---|---|---|---|---|
| Service UUID | `serviceUuid` | STRING | (empty) | GATT Service UUID (not consumed currently) |
| Characteristic UUID | `characteristicUuid` | STRING | (empty) | UUID of the characteristic to write (not consumed currently) |

::: warning Writes reuse the point's UUIDs; command attributes have no effect today
Like `read()`, BLE's `write()` reads `serviceUuid`/`characteristicUuid` from the point attributes (`point-attribute`)—the point decides which characteristic is written. The driver does not override `execute()`, and `command-attribute` is only consumed on the `execute()` path, so the write command attributes above are placeholder config that the current write path never reads. **A writable point does not need to repeat the UUIDs on the write command**—configure them on the point.
:::

::: warning Writes are sent as UTF-8 bytes, with no format conversion
The read path has `readFormat`/`byteOrder` to parse bytes into a value, but the write path has no symmetric inverse—the driver encodes the command value to UTF-8 bytes and writes them straight into the characteristic. To write a number or hex value, you must express it at the layer above as a string the target characteristic accepts (the driver will not turn `25.5` into little-endian float bytes for you).
:::

### Polling and health

- **Poll interval**: default cron `0/30 * * * * ?`—reads all points once every 30 seconds.
- **Health / online**: the device health check defaults to cron `0/15 * * * * ?` with a `45 second` lease timeout. A device is online when the BLE link is connected and reachable (the driver evaluates `governor.isOnline() && governor.isConnected()`); the driver as a whole is online when the Bluetooth manager has initialized. See [Device](../introduction/concepts/device) for the online-state mechanism.

::: info The `custom` schedule in `application.yml` is currently a no-op
Although the yml configures a `custom` cron (`0/5 * * * * ?`), the driver's `schedule()` method body is empty and runs no custom logic. Only `read` (`0/30`) and `health` (`0/15`) actually take effect.
:::

## Troubleshooting

1. **Device stays offline (most common)**. The host has no usable Bluetooth adapter, or `adapterName` is wrong (default `hci0`), so `governor.isOnline()` is never true and the device shows offline forever. Confirm the adapter exists and is up on the host with `hciconfig`/`bluetoothctl`, then re-check `adapterName`.
2. **Can't reach Bluetooth from a container**. This driver relies on the host's **physical** Bluetooth adapter and the TinyB native library. In containerized deployments, if you don't pass the host's Bluetooth capabilities through (e.g. no D-Bus/adapter mounted, no privileges granted), driver init won't error (`withIgnoreTransportInitErrors(true)` swallows transport-init failures) but the device never connects.
3. **Characteristic not found, read/write fails**. `serviceUuid`/`characteristicUuid` must match the GATT the peripheral actually exposes, character for character (including short/long form and case). A wrong UUID means the characteristic can't be located: reads throw `ReadPointException` (caught by the driver as a read failure), writes throw `WritePointException`. Confirm the peripheral's service/characteristic UUIDs with a BLE scanning tool (`bluetoothctl`, nRF Connect, etc.) before filling them in.
4. **Read value looks like garbage or absurd numbers**. Usually `readFormat`/`byteOrder` don't match the peripheral's actual encoding—e.g. the peripheral uses a little-endian float but you set `UTF8`, or the byte order is reversed. Adjust these two against the peripheral's GATT spec; when a read returns empty bytes (`data.length == 0`) the driver returns `null` and nothing is persisted.
5. **Connection timeout / can't connect**. The current driver does not use `connectionTimeout` (see the note above), so raising it has no effect; connection failures stem only from a weak signal, too much distance, or the peripheral's connection being held by another host. BLE typically allows only one central connected to a peripheral at a time—make sure no phone app or other gateway is holding the connection.
6. **Write command "has no effect"**. The command value isn't a string the target characteristic accepts (see the write semantics above), or the characteristic isn't writable. Confirm the characteristic's GATT properties include Write, then confirm the string sent from the layer above matches what the device expects.

## How it lands in IoT DC3

- **`dc3.driver.code`**: `BleDriver` (driver name `Bluetooth LE Driver`, type `DRIVER_CLIENT`—the driver actively connects to the peripheral). This is a stable routing identifier and should not be changed casually.
- **Capabilities**: read ✓, write ✓, subscribe —. Consistent with the [driver capability matrix](./matrix)—BLE is request/response active read-write: the driver polls characteristics with read and pushes commands with write, and does not subscribe to GATT notify/indication for passive reporting.
- **Implementation status**: available. `read()`/`write()`/`initial()`/`health()` are all fully implemented, using the Sputnikdev Bluetooth Manager + TinyB for connection and GATT read/write.

::: warning Prerequisite: a working Bluetooth adapter + TinyB native library on the host
The code is ready, but whether it runs depends on the deployment environment: the host must have a physical Bluetooth adapter (default `hci0`) and the TinyB native library. This is a hardware/environment prerequisite beyond pure software—neither can be missing. See troubleshooting items 1 and 2 above.
:::

### Minimal onboarding example

Bring in a BLE thermometer at MAC `AA:BB:CC:DD:EE:FF`:

1. Create a [Device](../introduction/concepts/device) with `Bluetooth LE Driver`, setting driver attributes `adapterName=hci0` and `deviceAddress=AA:BB:CC:DD:EE:FF` (`connectionTimeout` is not read by the current implementation, so leave it at the default).
2. Add a temperature [Point](../introduction/concepts/point) (`READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device. Set the point attributes `serviceUuid` and `characteristicUuid` to that temperature characteristic's UUIDs, with `readFormat=FLOAT` and `byteOrder=LITTLE` (per the peripheral's datasheet).
3. Start the driver; within 30 seconds the polled reading appears in [PointValue](../introduction/concepts/point-value).
4. If the point must be writable, configure a write [command](../introduction/concepts/command) for it—the write reuses the `serviceUuid`/`characteristicUuid` already set on the point, with no need to repeat them on the command; just express the command value at the layer above as a string the characteristic accepts.

## Further reading

- [Driver overview](./index) — grouping and selection across all 28 drivers
- [Driver capability matrix](./matrix) — a read/write/subscribe overview of every driver
- [Device onboarding](../operation/device-onboarding) — a full onboarding walkthrough
- [IoT protocols and wireless networks](../foundations/iot-protocols) — where BLE sits on the wireless-access side of the network layer, and its trade-offs
