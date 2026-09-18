# DC3 Driver BLE

## Overview

`dc3-driver-ble` is the Bluetooth LE (BLE) protocol driver of the IoT DC3 platform. It uses the Sputnikdev Bluetooth
Manager with the TinyB transport to connect to BLE devices, read GATT characteristic values, and write values to
characteristics.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-ble
- **Driver Name**: Bluetooth LE Driver

## Driver Attributes (Device-level)

| Attribute          | Description                                     |
|--------------------|-------------------------------------------------|
| Adapter Name       | Bluetooth adapter name (e.g. hci0)              |
| Device Address     | BLE device MAC address (e.g. AA:BB:CC:DD:EE:FF) |
| Connection Timeout | Connection timeout in milliseconds              |

## Point Attributes

| Attribute           | Description                                   |
|---------------------|-----------------------------------------------|
| Service UUID        | GATT Service UUID                             |
| Characteristic UUID | GATT Characteristic UUID                      |
| Read Format         | Data format (UTF8, HEX, INT16, UINT16, FLOAT) |
| Byte Order          | Byte order (BIG, LITTLE)                      |

## Command Attributes (write)

| Attribute           | Description                          |
|---------------------|--------------------------------------|
| Service UUID        | GATT Service UUID                    |
| Characteristic UUID | GATT Characteristic UUID for writing |

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A host with a Bluetooth adapter and the TinyB native library available, plus a reachable BLE peripheral exposing the
configured GATT service and characteristics.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-ble -am package
java -jar dc3-driver/dc3-driver-ble/target/dc3-driver-ble.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-ble -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
