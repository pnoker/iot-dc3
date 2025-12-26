# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IoT DC3 is a distributed IoT platform built on Spring Cloud for industrial device connectivity, data collection, and management. It uses a microservices architecture with gRPC for
inter-service communication and supports multiple industrial protocols (Modbus, OPC DA/UA, MQTT, Siemens S7).

**Architecture Layers:**

1. **Driver Layer** - Device connectivity SDKs for various industrial protocols
2. **Data Layer** - Device data collection, storage, and retrieval
3. **Management Layer** - Core hub for device management and microservice coordination
4. **Application Layer** - Advanced features (alarms, scheduling, logging)

**Technology Stack:**

- Java 21, Spring Boot 3.5.5, Spring Cloud 2025.0.0
- PostgreSQL (primary DB), Redis (cache), RabbitMQ (messaging), MQTT (IoT protocol)
- gRPC/Protobuf for service APIs
- Docker & Kubernetes for deployment

## Development Workflow

### Environment Setup

Start infrastructure dependencies first:

```bash
# Standard Docker registry (global)
docker-compose -f dc3/docker-compose-db.yml up -d

# For mainland China (Aliyun registry)
docker-compose -f dc3/docker-compose-db-aliyun.yml up -d
```

This starts:

- PostgreSQL on port 35432 (credentials: dc3/dc3dc3dc3)
- Redis on port 36379 (credentials: dc3dc3dc3)
- RabbitMQ on port 35672 (credentials: dc3/dc3dc3dc3)
- MQTT broker on port 31883 (credentials: dc3/dc3dc3dc3)

### Building

```bash
# Set environment variables
source dc3/env/dev.env.sh

# Build entire project
mvn clean package

# Build specific module
mvn clean package -pl dc3-center/dc3-center-auth
```

### Running Services

**IMPORTANT:** Services must be started in this order:

1. **Gateway** (port 8000)
   ```bash
   java -jar dc3-gateway/target/dc3-gateway.jar
   ```

2. **Auth Center** (ports 8300/9300)
   ```bash
   java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar
   ```

3. **Data Center** (ports 8500/9500)
   ```bash
   java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar
   ```

4. **Manager Center** (ports 8400/9400)
   ```bash
   java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
   ```

5. **Drivers** (after centers are running)
   ```bash
   java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar
   java -jar dc3-driver/dc3-driver-modbus-tcp/target/dc3-driver-modbus-tcp.jar
   # ... other drivers as needed
   ```

## Module Structure

```
iot-dc3/
├── dc3-api/                    # gRPC API definitions (Protobuf)
│   ├── dc3-api-auth           # Authentication APIs
│   ├── dc3-api-data           # Data service APIs
│   ├── dc3-api-driver         # Driver service APIs
│   └── dc3-api-manager        # Manager service APIs
│
├── dc3-common/                 # Shared components and utilities
│   ├── dc3-common-api         # Common gRPC definitions
│   ├── dc3-common-auth        # Authentication components
│   ├── dc3-common-constant    # Constants and enums
│   ├── dc3-common-dal         # Data Access Layer
│   ├── dc3-common-data        # Data models and DTOs
│   ├── dc3-common-driver      # Driver framework SDK
│   ├── dc3-common-model       # Domain models
│   └── ... (other common modules)
│
├── dc3-center/                 # Core microservices
│   ├── dc3-center-auth        # Authentication service
│   ├── dc3-center-data        # Data service
│   ├── dc3-center-manager     # Manager service
│   └── dc3-center-single      # Single deployment option
│
├── dc3-driver/                 # Device drivers
│   ├── dc3-driver-virtual     # Virtual driver (testing)
│   ├── dc3-driver-modbus-tcp  # Modbus TCP driver
│   ├── dc3-driver-mqtt        # MQTT driver
│   ├── dc3-driver-opc-da      # OPC DA driver
│   ├── dc3-driver-opc-ua      # OPC UA driver
│   └── dc3-driver-plcs7       # Siemens S7 driver
│
└── dc3-gateway/                # API Gateway
```

## gRPC Service API Definitions

Service APIs are defined in `.proto` files under `dc3-api/` modules:

- **dc3-api-auth**: tenant.proto, token.proto, user.proto, user_login.proto
- **dc3-api-data**: point_value.proto
- **dc3-api-driver**: driver_device.proto, driver_driver.proto, driver_point.proto, etc.
- **dc3-api-manager**: manager_device.proto, manager_driver.proto, manager_point.proto, etc.

When modifying service APIs:

1. Edit the `.proto` file in the appropriate `dc3-api-*` module
2. Build the project to generate Java classes: `mvn clean package`
3. Implement the service interface in the corresponding `dc3-center-*` or driver module
4. All inter-service communication uses gRPC, not REST

## Developing New Drivers

New device drivers should be created in the `dc3-driver/` directory:

1. **Create new driver module** following the pattern of existing drivers (e.g., `dc3-driver-virtual`)
2. **Implement driver interface** using `dc3-common-driver` SDK
3. **Register driver** with the platform via gRPC `DriverApi.DriverRegister()`
4. **Configure application.yml** with appropriate settings

Key driver responsibilities:

- Connect to physical devices using the device's protocol
- Acquire data from devices (southbound)
- Execute commands on devices
- Register with Manager Center on startup
- Report device status and events

## Code Architecture Patterns

**Dependency Management:**

- Parent POM (`dc3-parent`) manages all dependency versions
- All modules use version `2025.11.1` (coordinated across project)
- Maven profiles: dev (default), test, pre, pro

**Configuration:**

- Environment variables loaded via `source dc3/env/dev.env.sh`
- Application YAMLs use `${NODE_ENV:dev}` for profile selection
- gRPC service hosts configured as environment variables (e.g., `CENTER_AUTH_HOST`)

**Multi-tenancy:**

- Platform supports namespaces for multi-tenant isolation
- Tenant context is managed through Auth Center

**Message Flow:**

1. Drivers register with Manager Center
2. Manager Center coordinates device/driver/point metadata
3. Data Center handles time-series data storage
4. RabbitMQ for asynchronous messaging between services
5. Redis for caching and session management

## Branching and Contribution

- **Main branch:** `main` (production-ready code)
- **Development branch:** `develop` (integration branch for PRs)
- **Branch naming:** `feature/your_name/feature_description` (e.g., `feature/pnoker/mqtt_driver`)
- **PR target:** Submit pull requests to `develop` branch
- **License:** AGPL 3.0 - all code changes must maintain this license

## Common Patterns

**Driver Services:**

- Main class: `@SpringBootApplication` with `SpringApplication.run()`
- Extend `dc3-common-driver` base classes
- Implement gRPC service interfaces from `dc3-api-driver`

**Center Services:**

- Implement gRPC service interfaces from respective `dc3-api-*` modules
- Use `dc3-common-dal` for data access
- Use `dc3-common-redis` for caching
- Use `dc3-common-rabbitmq` for messaging

**Constants and Enums:**

- Centralized in `dc3-common-constant`
- Referenced across all modules for consistency

**Error Handling:**

- `dc3-common-exception` provides standard exception types
- `dc3-common-model` contains response wrappers (`GrpcR` in protobuf)
