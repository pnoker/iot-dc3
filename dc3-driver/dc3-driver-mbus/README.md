# DC3 Driver MBus

## Overview

`dc3-driver-mbus` is the M-Bus (Meter-Bus, IEC 870-5) driver of the IoT DC3 platform. It communicates with heat, water,
and gas meters over a serial port using the jSerialComm library, caching one serial connection per meter. Each point
sends a REQ_UD2 request and decodes the DIF/VIF data records returned in the RSP_UD response, selecting a record by
index and decoding it as BCD, integer, IEEE-754, or raw HEX.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-mbus
- **Driver Name**: M-Bus Meter-Bus Driver

## Driver Attributes (Device-level)

| Attribute       | Code           | Type   | Default      | Description                    |
|-----------------|----------------|--------|--------------|--------------------------------|
| Serial Port     | port           | STRING | /dev/ttyUSB0 | Serial port device path        |
| Baud Rate       | baudRate       | INT    | 2400         | Baud rate (300..9600)          |
| Data Bits       | dataBits       | INT    | 8            | Data bits (7, 8)               |
| Stop Bits       | stopBits       | INT    | 1            | Stop bits (1, 2)               |
| Parity          | parity         | INT    | 2            | Parity (0=None, 1=Odd, 2=Even) |
| Timeout         | timeout        | INT    | 1000         | Read timeout in milliseconds   |
| Primary Address | primaryAddress | INT    | 0            | M-Bus primary address (0-250)  |

## Point Attributes

| Attribute    | Code        | Type   | Default | Description                             |
|--------------|-------------|--------|---------|-----------------------------------------|
| Record Index | recordIndex | INT    | 0       | Zero-based index of the DIF/VIF record  |
| Data Format  | dataFormat  | STRING | AUTO    | Data format: AUTO, HEX, BCD, INT, FLOAT |

## Command Attributes (write)

| Attribute   | Code       | Type   | Default | Description                             |
|-------------|------------|--------|---------|-----------------------------------------|
| Data Format | dataFormat | STRING | ASCII   | Format used to encode the written value |

The module `application.yml` is authoritative for attribute codes, types, defaults, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Limitations

DIF/VIF parsing covers the common single-byte DIF and VIF case plus DIFE/VIFE extension bytes. Manufacturer-specific and
variable-length records are skipped conservatively. Extended VIF (unit scaling) is not applied to decoded values.

## Running Locally

```bash
make up-db
make up-dev GROUP=core
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-mbus -am package
java -jar dc3-driver/dc3-driver-mbus/target/dc3-driver-mbus.jar
```

## Testing

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-mbus -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
