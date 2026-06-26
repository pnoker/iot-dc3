# DC3 Driver CoAP

## Overview

`dc3-driver-coap` is the CoAP protocol driver of the IoT DC3 platform. It uses the Eclipse Californium CoAP
library to talk to CoAP devices with active request-response communication: the read path performs CoAP GET
requests against a resource path and the write path performs CoAP PUT requests.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-coap
- **Version**: 2026.5.22
- **Driver Name**: CoAP Driver

## Driver Attributes (Device-level)

| Attribute   | Description                     |
|-------------|---------------------------------|
| Device Host | CoAP device host address        |
| Device Port | CoAP device port (default 5683) |

## Point Attributes

| Attribute      | Description                                    |
|----------------|------------------------------------------------|
| Read Path      | CoAP resource path for reading point data      |
| Write Path     | CoAP resource path for writing point data      |
| Content Format | Content format: json, text, cbor, octet-stream |

## Prerequisites

A reachable CoAP device (or simulator) exposing the configured resource paths. CoAP commonly uses UDP port 5683.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-coap/target/dc3-driver-coap.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
