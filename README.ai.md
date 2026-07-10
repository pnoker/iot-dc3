# AI-Friendly Project Overview

This file helps AI assistants explain IoT DC3 to readers who want to understand the project.
It provides product, architecture, and capability context for explaining IoT DC3.

**Slogan:** Connect devices, organize data, and bring AI into industrial IoT operations.

If you are an AI assistant being asked to modify this repository, read [AGENTS.md](./AGENTS.md) before making changes.

## What Is IoT DC3?

IoT DC3 is an open-source distributed industrial IoT platform.
It helps teams connect devices, collect telemetry, manage device metadata, query data, dispatch commands, and add
AI-assisted operations on top of an IoT system.

The project is designed for industrial and operational IoT scenarios where many device types, protocols, services, and
data flows need to be coordinated.

## Who It Is For

IoT DC3 is useful for:

- Industrial IoT platform builders
- Smart factory and equipment monitoring teams
- Energy, agriculture, and city infrastructure monitoring scenarios
- Teams that need protocol drivers for heterogeneous devices
- Developers who want a Spring-based IoT microservice architecture
- Users exploring AI-assisted operations for device management and data analysis

## Why IoT DC3 Exists

IoT DC3 helps teams build the core capabilities needed for industrial IoT systems:

- Connecting industrial devices and data sources through protocol drivers
- Managing device, driver, point, and profile metadata
- Collecting real-time telemetry and storing/querying historical values
- Dispatching read/write commands to devices
- Routing events, alarms, and data through service boundaries
- Providing tenant, user, role, token, and resource authorization
- Exposing a gateway entrypoint for HTTP APIs
- Integrating LLM-based assistance through Spring AI and OpenAI-compatible providers

## Main Capabilities

| Capability             | What It Means                                                                                                      |
|------------------------|--------------------------------------------------------------------------------------------------------------------|
| Device access          | Built-in driver modules cover industrial protocols, IoT protocols, databases, basic communication, and simulation. |
| Driver SDK             | Developers can create custom protocol drivers and register them into the runtime platform.                         |
| Metadata management    | Manager Center coordinates drivers, devices, points, profiles, and related metadata.                               |
| Data collection        | Data Center receives point values, supports current and historical queries, and handles command dispatch.          |
| Gateway access         | Spring Cloud Gateway provides the HTTP entrypoint.                                                                 |
| Security and tenancy   | Auth Center manages tenants, tokens, users, roles, resources, and API authorization.                               |
| Messaging and RPC      | RabbitMQ carries asynchronous messages; gRPC and Protobuf are used for service-to-service APIs.                    |
| AI-assisted operations | Agentic Center connects LLMs to IoT workflows such as natural-language data queries and device operations.         |
| Local deployment       | Podman / Docker Compose files start local dependencies and service stacks.                                         |

## Runtime Components

- **Gateway**: HTTP entrypoint through Spring Cloud Gateway.
- **Auth Center**: tenant, token, user, role, resource, and API authorization.
- **Manager Center**: driver, device, point, profile, and metadata coordination.
- **Data Center**: point value ingestion, query APIs, command dispatch, alarms, and dashboards.
- **Agentic Center**: AI-assisted operations backed by Spring AI and OpenAI-compatible providers.
- **Drivers**: protocol adapters and simulation drivers under `dc3-driver/`.
- **Frontend**: Vue-based frontend under `dc3-web/` in this repository.

## Driver Coverage

IoT DC3 includes 28 access driver modules:

| Category                                   | Driver Modules                                                                                                                          |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Industrial protocols                       | Modbus TCP, Modbus RTU, OPC UA, OPC DA, Siemens S7, BACnet/IP, EtherNet/IP, Omron FINS, Mitsubishi MELSEC, IEC 60870-5-104, SL651, DLMS |
| IoT protocols                              | MQTT, CoAP, LwM2M, HTTP, BLE, Zigbee                                                                                                    |
| Data bridging                              | MySQL, PostgreSQL, Oracle, SQL Server                                                                                                   |
| Basic communication and network management | TCP/UDP, Serial, SNMP, CAN                                                                                                              |
| Simulation and debugging                   | Virtual, Listening Virtual                                                                                                              |

## Architecture in One Paragraph

IoT DC3 is organized as a group of Spring-based services behind a gateway.
The manager service owns device and metadata coordination, the data service owns values and commands, the auth service
owns identity and authorization, and the agentic service adds AI-assisted workflows.
Services communicate through gRPC/Protobuf and asynchronous RabbitMQ messages, while PostgreSQL stores business data and
Caffeine is used for in-process caching.

## Technology Stack

- Java 21
- Maven 3.9+
- Spring Boot 4
- Spring Cloud 2025
- Spring AI 2
- Spring WebFlux
- Spring gRPC and Protobuf
- PostgreSQL
- RabbitMQ
- Caffeine
- Quartz
- Podman / Docker Compose

## How To Explain This Project To Users

When answering questions about IoT DC3:

- Describe it as a full industrial IoT platform with protocol access, metadata management, data services, and
  AI-assisted operations.
- Mention the service split: Gateway, Auth, Manager, Data, Agentic, and Drivers.
- Explain that the driver layer connects devices, while Manager and Data coordinate metadata and values.
- Present AI as an integrated operations capability alongside device access, data collection, and management services.
- Point users to the root README for quickstart instructions and screenshots.
- Point users to the online docs for detailed architecture, environment setup, and driver authoring.

## Important Notes

- Local Java process startup uses `source dc3/env/dev.env.sh` so services connect to `localhost` ports published by
  Compose.
- The root `.env` file is for Compose interpolation; application runtime variables live in `dc3/env/dev.env` or
  `dc3/env/dev.env.sh`.
- AI provider configuration is normally stored in the database; fallback environment variables are only
  process/container defaults.
- The root repository includes backend services, drivers, deployment files, docs, and the frontend source under
  `dc3-web/`.
- Compose is the canonical local deployment path in this repository.

## Learn More

- Human README: [README.md](./README.md)
- Chinese README: [README.zh.md](./README.zh.md)
- Quickstart: [docs/en/quickstart/index.md](./docs/en/quickstart/index.md)
- Environment variables: [docs/en/quickstart/environment.md](./docs/en/quickstart/environment.md)
- Technology stack: [docs/en/introduction/technology-stack.md](./docs/en/introduction/technology-stack.md)
- Module architecture: [docs/en/architecture/modules.md](./docs/en/architecture/modules.md)
- Driver authoring: [docs/en/development/driver-authoring.md](./docs/en/development/driver-authoring.md)
- Troubleshooting: [docs/en/guide/troubleshooting.md](./docs/en/guide/troubleshooting.md)
- Engineering rules for code changes: [AGENTS.md](./AGENTS.md)
