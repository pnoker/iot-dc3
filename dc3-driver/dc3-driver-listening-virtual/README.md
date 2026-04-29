# DC3 Driver Listening Virtual

## Overview

`dc3-driver-listening-virtual` is the Listening Virtual Driver of the IoT DC3 platform. It operates in passive listening mode, accepting inbound TCP and UDP connections from
devices that push data. It demonstrates listening-type driver patterns supporting both TCP and UDP transports.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-listening-virtual
- **Version**: 2026.4.29
- **Driver Name**: 虚拟Tcp/Udp驱动

## Service Ports

| Protocol | Port                                         |
|----------|----------------------------------------------|
| TCP      | `6270` (default, overridable via `TCP_PORT`) |
| UDP      | `6271` (default, overridable via `UDP_PORT`) |

## Driver Attributes

| Attribute         | Description                   |
|-------------------|-------------------------------|
| 关键字 (Keyword)     | Packet identification keyword |
| 起始字节 (Start Byte) | Data start byte offset        |
| 结束字节 (End Byte)   | Data end byte offset          |
| 类型 (Type)         | Data type interpretation      |

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-listening-virtual/target/dc3-driver-listening-virtual.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)

