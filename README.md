<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a>
</p>

> **AI assistants:** Read [README.ai.md](./README.ai.md) first for a concise, AI-friendly overview of IoT DC3.

<p align="center">
  <img src="docs/public/images/logo.svg" width="240" alt="IoT DC3">
</p>

<p align="center">
  <a href="https://github.com/pnoker/iot-dc3/stargazers">
    <img src="https://img.shields.io/github/stars/pnoker/iot-dc3?style=flat&logo=github&color=green" alt="GitHub Stars">
  </a>
  <a href="https://gitee.com/pnoker/iot-dc3/stargazers">
    <img src="https://gitee.com/pnoker/iot-dc3/badge/star.svg?theme=gvp" alt="Gitee Star">
  </a>
  <a href="https://gitee.com/pnoker/iot-dc3/members">
    <img src="https://gitee.com/pnoker/iot-dc3/badge/fork.svg?theme=gvp" alt="Gitee Fork">
  </a>
  <a href="https://github.com/pnoker/iot-dc3/graphs/contributors">
    <img src="https://img.shields.io/github/contributors/pnoker/iot-dc3?label=contributors&color=orange" alt="Contributors">
  </a>
  <img src="https://img.shields.io/badge/License-AGPL%203.0-blue" alt="License">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot" alt="Spring Boot 4">
</p>

<p align="center">
  <strong>
    IoT DC3 — the open-source industrial IoT platform that lets large language models operate your devices directly.<br>
    Native Spring AI integration · Natural-language operations · 28 ready-to-use drivers
  </strong>
</p>

<p align="center">
  🔌 <strong>Multi-protocol connectivity</strong> &nbsp;·&nbsp;
  🤖 <strong>AI Agentic Center</strong> &nbsp;·&nbsp;
  ☁️ <strong>Cloud-native microservices</strong>
</p>

---

## 📸 Product Preview

<table>
  <tr>
    <th width="33%">📸 Platform Overview</th>
    <th width="33%">📸 Device Management</th>
    <th width="33%">📸 AI Chat</th>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/public/images/screenshot-overview.png" alt="Platform dashboard" width="100%">
      <br>
      <strong>Home / Dashboard</strong><br>
      <em>System overview · Online device metrics · Data trend charts</em>
    </td>
    <td align="center">
      <img src="docs/public/images/screenshot-device.png" alt="Device management page" width="100%">
      <br>
      <strong>Device Management</strong><br>
      <em>Device list · Online status · Search and filtering</em>
    </td>
    <td align="center">
      <img src="docs/public/images/screenshot-ai.png" alt="AI chat page" width="100%">
      <br>
      <strong>AI Chat</strong><br>
      <em>Natural-language device operations · Data queries · Intelligent analysis</em>
    </td>
  </tr>
</table>

## ✨ Core Features

### 🔌 Multi-Protocol Device Connectivity

IoT DC3 includes **28 access driver modules** for industrial automation, IoT communication, data bridging, basic
communication, and simulation/debugging scenarios, reducing the cost of connecting common devices and data sources:

| Category                           | Driver Modules                                                                                                                                     |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| 🏭 **Industrial protocols**        | Modbus TCP · Modbus RTU · OPC UA · OPC DA · Siemens S7 · BACnet/IP · EtherNet/IP · Omron FINS · Mitsubishi MELSEC · IEC 60870-5-104 · SL651 · DLMS |
| 📡 **IoT protocols**               | MQTT · CoAP · LwM2M · HTTP · BLE · Zigbee                                                                                                          |
| 🗄️ **Data bridging**              | MySQL · PostgreSQL · Oracle · SQL Server                                                                                                           |
| 🔧 **Basic communication and NMS** | TCP/UDP · Serial · SNMP · CAN                                                                                                                      |
| 🧪 **Simulation and debugging**    | Virtual · Listening Virtual                                                                                                                        |

The **Driver SDK** supports fast development of custom protocol drivers and registration into the runtime platform.

### 🤖 AI Capability Integration

The agentic center is built on **Spring AI** and connects large language models into IoT operations workflows:

- **Natural-language device operations** - LLMs can query devices, read/write points, and execute commands through Tool
  Calling
- **Intelligent alarm analysis** - AI assists with root-cause analysis and response suggestions
- **Data insights** - Query device data in natural language and generate visual charts
- **Multi-model support** - Compatible with OpenAI API-style providers and mainstream models such as GPT, Claude,
  DeepSeek, and Qwen
- **Conversation memory** - Multi-turn conversations and context memory persisted to the database

### 🏗️ Cloud-Native Microservices

Distributed microservice architecture based on **Spring Boot 4 + Spring Cloud 2025**:

- **Service governance** - Spring Cloud Gateway as the unified entrypoint, with static routes and flexible environment
  variables
- **Efficient communication** - gRPC service calls with Protobuf serialization
- **Horizontal scaling** - Stateless design for scaling individual services by workload
- **Resilience** - Replaceable service nodes and fault isolation

### 📊 Real-Time Data Engine

- **Data collection** - Drivers collect device telemetry and send it asynchronously through RabbitMQ
- **Time-series storage** - Efficient queries for real-time and historical data
- **Rule engine** - Flexible alarm rules with multi-level alarms and notifications
- **Event traceability** - Full command and event history

### 🔐 Enterprise Security and Multi-Tenancy

- **Tenant isolation** - Tenant-level isolation across database, cache, and API paths
- **Authentication and authorization** - JWT + Spring Security with RBAC
- **Transport encryption** - TLS/SSL communication support
- **Audit tracking** - User operation and system event logs

### 🧩 Developer Friendly

- **Driver SDK** - A complete driver development toolkit. See
  the [Driver Authoring Guide](https://pnoker.github.io/iot-dc3/development/driver-authoring)
- **Separated frontend and backend** - Vue 3 + TypeScript frontend, RESTful and gRPC APIs
- **Containerized deployment** - One-command startup with Podman / Docker Compose, with a path toward Kubernetes and
  other container platforms
- **Complete documentation** - Online docs, quickstart guide, and troubleshooting guide

## ⚡ Quick Start

### Prerequisites

| Dependency       | Version       |
|------------------|---------------|
| Java (JDK)       | 21+           |
| Maven            | 3.9+          |
| Podman or Docker | Latest stable |

### Start in Three Steps

**① Clone the repository**

```bash
git clone https://github.com/pnoker/iot-dc3.git
cd iot-dc3
```

**② Start base dependencies** (PostgreSQL + RabbitMQ)

```bash
# Global registry
make up-db

# Mainland China users (Alibaba Cloud registry)
make up-db-cn
```

**③ Load local environment variables, build, and start**

```bash
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package
```

`dc3/env/dev.env.sh` points local Java processes to the PostgreSQL, RabbitMQ, and gRPC ports published on `localhost`.
Run the following `java -jar` commands in the same terminal session.

Start services in order:

```bash
java -jar dc3-gateway/target/dc3-gateway.jar                          # API Gateway
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar        # Auth Center
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar  # Manager Center
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar        # Data Center
java -jar dc3-center/dc3-center-agentic/target/dc3-center-agentic.jar  # Agentic Center
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar  # Virtual Driver for demos
```

> 📖 For full local setup, see the [Quickstart](https://pnoker.github.io/iot-dc3/quickstart/) and
> [Environment Variables](https://pnoker.github.io/iot-dc3/quickstart/environment) docs.

<details>
<summary>🔧 More startup options (optional dependencies, single-service startup, environment variables)</summary>

**Start optional infrastructure** (EMQX, ELK/APM, Prometheus, Grafana, etc.):

```bash
make up-optional-cn              # Start optional dependencies
make up-db-cn && make up-optional-cn && make up-dev-cn  # Start all dependencies
```

**Start selected services** (useful for frontend/API testing):

```bash
make up SERVICES=agentic REGISTRY=cn               # Single service
make up SERVICES="gateway agentic" REGISTRY=cn      # Multiple services
make up GROUP=core REGISTRY=cn                      # Core service group
make up GROUP=drivers REGISTRY=cn                   # Driver service group
make logs SERVICES="gateway agentic"                # Follow logs
```

**Compose environment overrides**:

```bash
cp .env.example .env    # Copy the template
```

The root `.env` file is used for Compose variable interpolation, such as image registry, image tag, and published ports.
Application runtime variables are configured in `dc3/env/dev.env`. See
the [environment documentation](https://pnoker.github.io/iot-dc3/quickstart/environment).

</details>

## 🏗️ Architecture Overview

### Architecture at a Glance

![IoT DC3 Architecture Panorama](docs/public/images/architecture-panorama-en.svg)

Six-layer microservice architecture at a glance: clients → gateway → four center services → message bus → 28 protocol
drivers → field devices. PostgreSQL (TimescaleDB + pgvector + AGE) persistence and optional observability stack
(ELK + Prometheus + Grafana) laid out in one view.

### Four-Layer Reference Architecture Mapping

![IoT DC3 Four-Layer Reference Architecture](docs/public/images/architecture-en.svg)

Industry-standard IoT four-layer reference — Application, Platform, Network, Perception — plus security as a
cross-cutting concern.

| Layer           | IoT Reference Responsibilities            | DC3 Implementation                         |
|-----------------|-------------------------------------------|--------------------------------------------|
| **Application** | Operations · Alarms · Analytics · AIoT    | Operations · Agentic Center · MCP          |
| **Platform**    | Device mgmt · Storage · Rules & compute   | Center services · Data plane · TimescaleDB |
| **Network**     | Fieldbus · IoT protocols · Wireless / WAN | 28 protocol drivers · Gateway · RabbitMQ   |
| **Perception**  | Sensing · Auto-ID · Actuators             | Profile · Device · Point                   |

🧱 **Design principles** — cross-service calls always go through Facade interfaces; the DO/BO/VO three-tier model keeps
persistence, business, and API shapes strictly separated; and tenant isolation runs end to end across database, cache,
and API paths. Clear boundaries that scale across services and teams.

> 📖 For the full architecture documentation,
> see [System Architecture Overview](https://pnoker.github.io/iot-dc3/architecture/).

## 🛠️ Technology Stack

| Category                        | Technologies                                                |
|---------------------------------|-------------------------------------------------------------|
| **Language and frameworks**     | Java 21 · Spring Boot 4 · Spring Cloud 2025 · Spring AI 2.0 |
| **Data, cache, and scheduling** | PostgreSQL · Caffeine · MyBatis-Plus · Quartz               |
| **Messaging and communication** | RabbitMQ · gRPC · MQTT (Paho + EMQX) · Protobuf             |
| **Security and authentication** | Spring Security · JWT · BouncyCastle                        |
| **Observability**               | Micrometer · Prometheus · Grafana · ELK                     |
| **Frontend**                    | Vue 3 · TypeScript 6 · Vite 8 · Element Plus · AntV G2/G6   |
| **Desktop**                     | Tauri 2                                                     |
| **Deployment**                  | Podman · Docker Compose                                     |

> 💡 Frontend source code is in the `dc3-web/` directory of this repository (the standalone `iot-dc3-web` repo is
> archived).

## 📖 Documentation and Community

| Resource              | Link                                                                                    |
|-----------------------|-----------------------------------------------------------------------------------------|
| 📚 Online docs        | [pnoker.github.io/iot-dc3](https://pnoker.github.io/iot-dc3/)                           |
| 🚀 Quickstart         | [Quickstart Guide](https://pnoker.github.io/iot-dc3/quickstart/)                        |
| 🏗️ Architecture      | [Modules and Dependencies](https://pnoker.github.io/iot-dc3/architecture/modules)       |
| 🔧 Driver development | [Driver Authoring Guide](https://pnoker.github.io/iot-dc3/development/driver-authoring) |
| 🐛 Troubleshooting    | [Troubleshooting](https://pnoker.github.io/iot-dc3/guide/troubleshooting)               |
| 📋 Changelog          | [Release Changelog](https://pnoker.github.io/iot-dc3/development/changelog)             |
| 🐛 Issue feedback     | [GitHub Issues](https://github.com/pnoker/iot-dc3/issues)                               |
| 🇨🇳 Gitee mirror     | [Gitee GVP Project](https://gitee.com/pnoker/iot-dc3)                                   |

## 🌍 Use Cases

<table>
  <tr>
    <td align="center" width="60">🏭</td>
    <td><strong>Smart Factory</strong></td>
    <td>Production-line device monitoring, process parameter collection, predictive maintenance, and OEE analysis</td>
  </tr>
  <tr>
    <td align="center">⚡</td>
    <td><strong>Energy Monitoring</strong></td>
    <td>Remote metering for power, water, and gas; energy trend analysis; anomaly alarms</td>
  </tr>
  <tr>
    <td align="center">🌾</td>
    <td><strong>Smart Agriculture</strong></td>
    <td>Greenhouse monitoring, automatic irrigation control, pest and disease warnings, yield forecasting</td>
  </tr>
  <tr>
    <td align="center">🏙️</td>
    <td><strong>Smart City</strong></td>
    <td>Streetlight management, environmental monitoring, municipal facility operations, safety monitoring</td>
  </tr>
</table>

## 🤝 Contributing

Contributions of all kinds are welcome. Please follow this workflow:

1. **Fork and branch** - Create a branch from `main`, using the format `feature/your_name/feature_description`
   (for example: `feature/pnoker/mqtt_driver`)
2. **Develop and commit** - Complete your changes on the new branch and follow
   the [Conventional Commits](https://www.conventionalcommits.org/) specification
3. **Open a PR** - Submit a Pull Request to the `develop` branch for maintainer review and merge

## 📄 License

IoT DC3 is open source under the [AGPL 3.0](./LICENSE-AGPL.txt) license.

- ✅ **Personal learning, research, and internal use** - Free
- ✅ **Modify the code and open source your changes** - Welcome
- ⚠️ **Offering it as a commercial service to third parties without open-sourcing modifications** - Requires a
  commercial license

For commercial licensing details, see [LICENSE.txt](./LICENSE.txt).

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=pnoker/iot-dc3&type=Date)](https://star-history.com/#pnoker/iot-dc3&Date)
