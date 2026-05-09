<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a>
</p>

<p align="center">
	<img src="dc3/images/logo-blue.png" width="400" alt="IoT DC3 Logo">
<br>
<a href='https://gitee.com/pnoker/iot-dc3/stargazers'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/star.svg?theme=gvp' alt='star'/>
</a>
<a href='https://gitee.com/pnoker/iot-dc3/members'>
    <img src='https://gitee.com/pnoker/iot-dc3/badge/fork.svg?theme=gvp' alt='fork'/>
</a>
<br>
<strong>
IoT DC3 is a fully open-source, distributed Internet of Things (IoT) platform built on Spring Cloud.
It accelerates IoT solution delivery and simplifies full-lifecycle device management with a comprehensive architecture for robust, production-ready IoT systems.
It is AI-ready, enabling seamless integration of intelligent connectivity, automation, and data-driven operations.
All components and code are open-source, ensuring transparency, flexibility, and community-driven innovation.
</strong>
</p>

---

![iot-dc3-architecture](dc3/images/architecture-en.png)

# 1 Architecture

The architecture is designed for end-to-end IoT capabilities across device connectivity, data services, operational
management, and extensible application integration.

- **Driver Layer**: Provides SDKs for rapid driver development and seamless connectivity to physical devices through
  standard or proprietary protocols. This layer handles southbound data acquisition and command execution;
- **Data Layer**: Supports reliable collection, storage, and retrieval of device data, exposing robust interfaces for
  real-time and historical data services;
- **Management Layer**: Serves as the core hub for distributed microservice collaboration, including service
  registration, device/driver management, command orchestration, and centralized configuration governance;
- **Application Layer**: Enables data openness, scheduling, alarms, messaging, logging, third-party integrations, and
  AI-enhanced automation scenarios.

# 2 Objectives

- **Scalability**: Supports horizontal scaling with Spring Cloud for distributed, high-throughput IoT workloads;
- **Resilience**: Minimizes single-point-of-failure risk with interchangeable service nodes and fault-tolerant design;
- **Performance**: Handles large-scale device access and telemetry workloads for demanding IoT scenarios;
- **Extensibility**: Accelerates integration of new protocols and custom drivers through SDK and service registration;
- **Deployment Flexibility**: Runs across private cloud, public cloud, and edge environments with Java compatibility;
- **Operational Efficiency**: Streamlines onboarding, registration, and permission validation for devices and services;
- **Security and Multi-Tenancy**: Enforces encrypted communication, namespace isolation, and tenant-level separation;
- **Cloud-Native Delivery**: Optimized for Kubernetes and containerized with Docker for consistent deployments;
- **AI-Ready Evolution**: Enables integration of intelligent automation and data-driven operational workflows.

# 3 Development

## 3.1 Startup Dependencies

> Choose one
>
> This base stack starts PostgreSQL and RabbitMQ. If you need a database SQL script, connect directly to the started
> database in the container for export.

```bash
# Global access with standard container registry service
podman compose -f dc3/docker-compose-db.yml up -d

# Optimized registry service for users in mainland China
podman compose -f dc3/docker-compose-db-aliyun.yml up -d
```

Optional helper targets:

```bash
make dev-db
make dev-optional
make dev
make dev-all
```

Use `REGISTRY=domestic` when you want the mainland China image registry variants. Backward-compatible aliases
`REGISTRY=aliyun` and `REGISTRY=cn` still work:

```bash
make dev-db REGISTRY=domestic
make dev-all REGISTRY=domestic
make app-all REGISTRY=aliyun
make compose-up STACK=grafana REGISTRY=cn
make compose-logs STACK=dev REGISTRY=global
```

### Docker Compose environment overrides

Copy the example file before changing any published ports, image tags, or observability settings:

```bash
cp .env.example .env
```

Common overrides used by the Compose stacks in `dc3/`:

| Variable                                                                | Default                 | Purpose                                                                     |
|-------------------------------------------------------------------------|-------------------------|-----------------------------------------------------------------------------|
| `DC3_IMAGE_TAG`                                                         | `2026.5`                | Shared image tag for application, database, EMQX, and observability images  |
| `DC3_LOG_MAX_SIZE`                                                      | `10M`                   | Maximum size per container log file                                         |
| `DC3_LOG_MAX_FILE`                                                      | `20`                    | Number of rotated log files to keep                                         |
| `DC3_BIND_HOST`                                                         | `127.0.0.1`             | Host address used for published ports; set `0.0.0.0` if you need LAN access |
| `APM_AGENT_ENABLE`                                                      | `false`                 | Enables the Java APM agent in application containers                        |
| `POSTGRES_USERNAME`                                                     | `dc3`                   | Username used by the Postgres health check                                  |
| `POSTGRES_DB`                                                           | `dc3`                   | Database name used by the Postgres health check                             |
| `DC3_POSTGRES_PORT`                                                     | `35432`                 | Published PostgreSQL port                                                   |
| `DC3_RABBITMQ_TLS_PORT` / `DC3_RABBITMQ_PORT`                           | `35671` / `35672`       | RabbitMQ TLS / AMQP ports                                                   |
| `DC3_RABBITMQ_MANAGEMENT_PORT`                                          | `15672`                 | RabbitMQ management UI port                                                 |
| `DC3_GATEWAY_PORT`                                                      | `8000`                  | Gateway HTTP port                                                           |
| `DC3_AUTH_PORT` / `DC3_AUTH_GRPC_PORT`                                  | `8300` / `9300`         | Auth center HTTP and gRPC ports                                             |
| `DC3_MANAGER_PORT` / `DC3_MANAGER_GRPC_PORT`                            | `8400` / `9400`         | Manager center HTTP and gRPC ports                                          |
| `DC3_DATA_PORT` / `DC3_DATA_GRPC_PORT`                                  | `8500` / `9500`         | Data center HTTP and gRPC ports                                             |
| `DC3_AGENTIC_PORT`                                                      | `8600`                  | Agentic center HTTP port                                                    |
| `DC3_LISTENING_VIRTUAL_DRIVER_PORT` / `DC3_LISTENING_VIRTUAL_GRPC_PORT` | `6270` / `6271`         | Listening virtual driver ports                                              |
| `OPENAI_BASE_URL` / `OPENAI_MODEL`                                      | `https://api.openai.com` / `gpt-4o` | OpenAI-compatible endpoint and chat model for Agentic center       |
| `OPENAI_API_KEY`                                                        | empty                   | OpenAI-compatible API key for Agentic center                                |
| `DC3_EMQX_WS_PORT` / `DC3_EMQX_WSS_PORT`                                | `38083` / `38084`       | EMQX WebSocket endpoints                                                    |
| `DC3_EMQX_MQTT_PORT` / `DC3_EMQX_MQTTS_PORT`                            | `31883` / `38883`       | EMQX MQTT / MQTTS ports                                                     |
| `DC3_EMQX_DASHBOARD_PORT`                                               | `18083`                 | EMQX dashboard port                                                         |
| `GF_SERVER_ROOT_URL`                                                    | `http://localhost:3000` | Grafana external root URL                                                   |
| `DC3_GRAFANA_PORT`                                                      | `3000`                  | Grafana HTTP port                                                           |
| `DC3_KIBANA_PORT`                                                       | `5601`                  | Kibana HTTP port                                                            |
| `DC3_ES_JAVA_OPTS`                                                      | `-Xms512m -Xmx512m`     | Elasticsearch JVM heap settings                                             |
| `DC3_LS_JAVA_OPTS`                                                      | `-Xms256m -Xmx256m`     | Logstash JVM heap settings                                                  |

## 3.2 Preparation

```bash
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package
```

> **Module Overview**: See [`dc3/doc/MODULES.md`](dc3/doc/MODULES.md) for the full module dependency map and runtime flow
> diagram.

> **Local Dev Guide**: See [`dc3/doc/QUICKSTART.md`](dc3/doc/QUICKSTART.md) for a one-stop local setup workflow.

> **Troubleshooting**: See [`dc3/doc/TROUBLESHOOTING.md`](dc3/doc/TROUBLESHOOTING.md) for common build/runtime issues and
> resolutions.

## 3.3 Start Services

> Start in order

```bash
# Gateway
java -jar dc3-gateway/target/dc3-gateway.jar

# Auth Center
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar

# Data Center
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar

# Manager Center
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar

# Agentic Center
java -jar dc3-center/dc3-center-agentic/target/dc3-center-agentic.jar

# Virtual Driver
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar

# Other driver: Listening Virtual Driver, Modbus TCP Driver, MQTT Driver, OPC DA Driver, OPC UA Driver, Siemens S7 Driver
```

# 4 Technology Stack

- [Java 21](https://www.java.com)
- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.0.0](https://spring.io/projects/spring-cloud)

# 5 Contribution

- **Branch Creation**: Start by creating a new branch from the `main` branch. Ensure that the `main` branch is
  up-to-date before branching out;
- **Branch Naming**: Follow the naming convention for the new branch: `feature/your_name/feature_description`. For
  example: `feature/pnoker/mqtt_driver`;
- **Code and Documentation**: Make your changes to the code or documentation on the new branch. Once done, commit your
  changes;
- **Pull Request**: Submit a `Pull Request` (PR) to merge your changes into the `develop` branch. Your PR will be
  reviewed and merged by the maintainers.

# 6 License

The `IoT DC3` open-source platform is licensed under the [AGPL 3.0 License](./LICENSE-AGPL.txt). 
