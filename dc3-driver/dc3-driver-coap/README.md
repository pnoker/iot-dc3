# DC3 Driver CoAP

## Overview

`dc3-driver-coap` is the CoAP protocol driver of the IoT DC3 platform. It uses the Eclipse Californium CoAP library to
talk to CoAP devices with active request-response communication: the read path performs CoAP GET requests against a
resource path and the write path performs CoAP PUT requests.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-coap
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

The module `application.yml` is authoritative for attribute codes, types, default values, scheduling, health, and local
buffering. Keep this README aligned when those user-facing settings change.

## Prerequisites

A reachable CoAP device (or simulator) exposing the configured resource paths. CoAP commonly uses UDP port 5683.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev GROUP=core
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-coap -am package
java -jar dc3-driver/dc3-driver-coap/target/dc3-driver-coap.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-coap -am test
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration
