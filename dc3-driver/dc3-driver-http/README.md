# DC3 Driver HTTP

## Overview

`dc3-driver-http` is the HTTP REST client driver of the IoT DC3 platform. It uses Spring WebFlux `WebClient` to call
configured REST endpoints, reads point values from JSON responses (with simple JSON path extraction), and writes values
via request body templates containing a `${value}` placeholder.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-driver-http
- **Version**: 2026.5.22
- **Driver Name**: HTTP REST Client Driver

## Driver Attributes (Device-level)

| Attribute | Description                                                  |
|-----------|--------------------------------------------------------------|
| Base URL  | Base URL for API requests (e.g. https://api.example.com)     |
| Method    | Default HTTP method (GET, POST, PUT, DELETE)                 |
| Headers   | Custom headers as JSON (e.g. {"Authorization":"Bearer xxx"}) |
| Timeout   | Request timeout in milliseconds                              |

## Point Attributes

| Attribute     | Description                                          |
|---------------|------------------------------------------------------|
| Path          | API path (e.g. /api/v1/sensor/{id})                  |
| Method        | HTTP method override for this point                  |
| Body Template | Request body template with ${value} placeholder      |
| Response Path | JSON path to extract value (e.g. $.data.temperature) |

## Command Attributes (write)

| Attribute | Description             |
|-----------|-------------------------|
| Path      | API path for command    |
| Method    | HTTP method for command |

## Prerequisites

A reachable HTTP/REST endpoint that the driver can call as configured via the device's Base URL and point paths.

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
podman compose -f dc3/docker-compose-db.yml up -d
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
```

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml clean package
java -jar dc3-driver/dc3-driver-http/target/dc3-driver-http.jar
```

## Related Modules

- `dc3-common-driver` — Driver SDK for registration, scheduling, and RabbitMQ integration

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
