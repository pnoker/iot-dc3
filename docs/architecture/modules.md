# Module Dependency Map

This document describes the Maven module structure and dependency relationships.

## Top-Level Modules

```
iot-dc3 (root pom)
+-- dc3-api          <- Protobuf/gRPC API contracts
+-- dc3-common       <- Reusable business libraries
+-- dc3-center       <- Deployable center services
+-- dc3-driver       <- Protocol drivers
+-- dc3-gateway      <- Spring Cloud Gateway
```

## dc3-api - gRPC Contract Layer

| Module          | Description                                          |
|-----------------|------------------------------------------------------|
| dc3-api-auth    | Auth protobuf: tenant, user, token                   |
| dc3-api-data    | Data protobuf: point value read/write                |
| dc3-api-driver  | Driver protobuf: register, command                   |
| dc3-api-manager | Manager protobuf: device, profile, driver management |

## dc3-common - Shared Business Libraries

### Foundation Layer

| Module               | Purpose                                  |
|----------------------|------------------------------------------|
| dc3-common-constant  | Enums, constants, value objects          |
| dc3-common-exception | Custom exception hierarchy (24+ classes) |
| dc3-common-public    | `R<T>` wrapper, utility classes          |
| dc3-common-model     | BO/VO/DTO entity definitions             |
| dc3-common-web       | BaseController, WebFlux config           |
| dc3-common-log       | Request/response logging filter          |
| dc3-common-thread    | Thread pool configuration                |

### Data Access Layer

| Module                | Purpose                                      |
|-----------------------|----------------------------------------------|
| dc3-common-dal        | MyBatis-Plus mapper/repository               |
| dc3-common-postgres   | PostgreSQL datasource (dynamic multi-schema) |
| dc3-common-repository | File/blob storage abstraction                |

Runtime caches use in-process Caffeine wrappers such as `LocalCacheService`; Redis is no longer a standalone module or
infrastructure dependency.

### Communication Layer

| Module                  | Purpose                                   |
|-------------------------|-------------------------------------------|
| dc3-common-facade-api   | Facade interfaces for cross-service calls |
| dc3-common-facade-grpc  | gRPC client/server auto-configuration     |
| dc3-common-facade-local | Local facade for dc3-center-single        |
| dc3-common-rabbitmq     | RabbitMQ exchange/queue config            |
| dc3-common-mqtt         | MQTT client configuration                 |

### Domain Layer

| Module             | Purpose                | Key Dependencies                                                                  |
|--------------------|------------------------|-----------------------------------------------------------------------------------|
| dc3-common-auth    | Auth business logic    | dc3-api-auth, dal, postgres, web                                                  |
| dc3-common-manager | Manager business logic | dc3-api-*, dal, facade-api, postgres, rabbitmq, quartz, web                       |
| dc3-common-data    | Data business logic    | dc3-api-*, dal, facade-api, postgres, rabbitmq, repository, quartz, web           |
| dc3-common-driver  | Driver SDK             | dc3-api-driver, constant, exception, log, model, public, quartz, rabbitmq, thread |
| dc3-common-gateway | Gateway filter         | facade-api, log, model, public, web                                               |
| dc3-common-agentic | AI chat and tool logic | facade-api, postgres, web, Spring AI                                              |

## dc3-center - Deployable Services

| Service            | Depends On                       | HTTP | gRPC |
|--------------------|----------------------------------|------|------|
| dc3-center-auth    | dc3-common-auth                  | 8300 | 9300 |
| dc3-center-data    | dc3-common-data, facade-grpc     | 8500 | 9500 |
| dc3-center-manager | dc3-common-manager, facade-grpc  | 8400 | 9400 |
| dc3-center-agentic | dc3-common-agentic, facade-grpc  | 8600 | -    |
| dc3-center-single  | auth+manager+data (facade-local) | 8200 | 9200 |

## dc3-driver - Protocol Drivers

All drivers depend on dc3-common-driver. Communicate with Manager via gRPC + RabbitMQ.

| Driver                       | Protocol                             |
|------------------------------|--------------------------------------|
| dc3-driver-virtual           | Virtual device simulator (HTTP push) |
| dc3-driver-listening-virtual | Listening virtual driver (MQTT)      |
| dc3-driver-modbus-tcp        | Modbus TCP                           |
| dc3-driver-mqtt              | MQTT (generic)                       |
| dc3-driver-opc-da            | OPC DA (DCOM)                        |
| dc3-driver-opc-ua            | OPC UA                               |
| dc3-driver-plcs7             | Siemens S7 (PLC)                     |

## dc3-gateway

Depends on: dc3-common-gateway + dc3-common-facade-grpc.
Routes /api/v3/* to center services with StripPrefix=2 and Authentic filter, including `/api/v3/agentic/**`.

## Runtime Flow

```
Client --> Gateway (8000) --> Auth/Manager/Data (REST)
                                    |  |  |
                                    v  v  v
                               dc3-common-* libs
                                    |  |  |
                                    v  v  v
                          PostgreSQL / RabbitMQ
                          in-process LocalCache (Caffeine)
                                    |
                                    v
                              dc3-driver (gRPC + MQ)
```
