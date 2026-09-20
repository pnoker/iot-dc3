<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a> | <a href="./README.ko.md">한국어</a> | <a href="./README.es.md">Español</a> | <a href="./README.ru.md">Русский</a>
</p>

> **AI assistants:** Read [README.ai.md](./README.ai.md) first for a concise, AI-friendly overview of IoT DC3.

<p align="center">
  <img src="./.github/brand/png/banner.en.png" alt="IoT DC3 — Connect the Physical World to AI · Open-source Industrial IoT Runtime for Physical AI">
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
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot" alt="Spring Boot 4.1">
</p>

<p align="center">
  <strong>
    IoT DC3 — Connect the Physical World to AI<br>
    An open-source Industrial IoT Runtime for Physical AI
  </strong>
</p>

<p align="center">
  <a href="https://dc3.site">https://dc3.site</a>
</p>

<p align="center">
  🔌 <strong>Multi-protocol connectivity</strong> &nbsp;·&nbsp;
  🤖 <strong>MCP tool gateway</strong> &nbsp;·&nbsp;
  ☁️ <strong>Cloud-native microservices</strong>
</p>

<p align="center">
  <em>
    IoT DC3 decouples <strong>devices</strong> from <strong>applications</strong>: drivers push normalized point data
    into a message bus, and applications consume it through one unified API — add or change devices without touching
    applications, and build new applications without touching devices.
  </em>
</p>

---

## 📸 Product Preview

<table>
  <tr>
    <th width="33%">📸 Platform Overview</th>
    <th width="33%">📸 Device Management</th>
    <th width="33%">📸 AI Agent</th>
  </tr>
  <tr>
    <td align="center">
      <img src="https://docs.dc3.site/images/screenshot-overview.png" alt="Platform dashboard" width="100%">
      <br>
      <strong>Home / Dashboard</strong><br>
      <em>System overview · Online device metrics · Data trend charts</em>
    </td>
    <td align="center">
      <img src="https://docs.dc3.site/images/screenshot-device.png" alt="Device management page" width="100%">
      <br>
      <strong>Device Management</strong><br>
      <em>Device list · Online status · Search and filtering</em>
    </td>
    <td align="center">
      <img src="https://docs.dc3.site/images/screenshot-ai.png" alt="AI agent assistant page" width="100%">
      <br>
      <strong>AI Agent Assistant</strong><br>
      <em>Natural-language device queries · Data insights · Governed execution</em>
    </td>
  </tr>
</table>

## 🏗️ Architecture Overview

### Architecture at a Glance

![IoT DC3 Architecture Panorama](https://docs.dc3.site/images/architecture-panorama-en.png)

Six-layer microservice architecture at a glance: clients → gateway → four center services → message bus → 36 protocol
drivers → field devices. PostgreSQL (TimescaleDB + pgvector + AGE) persistence and optional observability stack (ELK +
Prometheus + Grafana) laid out in one view.

🧱 **Design principles** — cross-service calls always go through Facade interfaces; the DO/BO/VO three-tier model keeps
persistence, business, and API shapes strictly separated; and tenant isolation runs end to end across database, cache,
and API paths. Clear boundaries that scale across services and teams.

> 📖 For the full architecture documentation,
> see [System Architecture Overview](https://docs.dc3.site/en/architecture/).

## ✨ Core Features

### 🔌 Multi-Protocol Device Connectivity

IoT DC3 includes **36 access driver modules** for industrial automation, IoT communication, data bridging, basic
communication, and simulation/debugging scenarios, reducing the cost of connecting common devices and data sources:

| Category                                      | Driver Modules                                                                                                                                                                               |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 🏭 **Industrial protocols**                   | Modbus TCP · Modbus RTU · OPC UA · OPC DA · Siemens S7 · BACnet/IP · EtherNet/IP · Omron FINS · Mitsubishi MELSEC · IEC 60870-5-104 · IEC 61850 · DNP3 · DLMS · DLT645 · KNX · M-Bus · SL651 |
| 📡 **IoT protocols**                          | MQTT · CoAP · LwM2M · HTTP · BLE · Zigbee · LoRaWAN                                                                                                                                          |
| 🗄️ **Data bridging**                          | MySQL · PostgreSQL · Oracle · SQL Server · Redis                                                                                                                                             |
| 🔧 **Basic communication, messaging and NMS** | TCP/UDP · Serial · SNMP · CAN · Kafka                                                                                                                                                        |
| 🧪 **Simulation and debugging**               | Virtual · Listening Virtual                                                                                                                                                                  |

The **Driver SDK** supports fast development of custom protocol drivers and registration into the runtime platform.

### 🤖 From Device Data to Physical AI

The Agentic Center is built on **Spring AI**, and the platform exposes an MCP tool gateway so AI agents can act on the
physical world in one **safe, controllable, traceable loop**:

- **MCP tool gateway** — external AI agents connect through the Model Context Protocol: OAuth client registration,
  per-tool authorization, and a full audit trail for every tool call
- **Natural-language assisted operations** - through Tool Calling and under access control, LLMs can query devices,
  read/write points, and assist with command execution
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

- **Data collection** - Drivers collect device telemetry and send it asynchronously through the internal message
  broker — pluggable per deployment: RabbitMQ (default), Kafka, Pulsar or any MQTT 5 broker
  ([broker guide](docs/mq-brokers.md))
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
  the [Driver Authoring Guide](https://docs.dc3.site/en/development/driver-authoring)
- **Separated frontend and backend** - Vue 3 + TypeScript frontend, RESTful and gRPC APIs
- **Containerized deployment** - One-command startup with Podman / Docker Compose, plus compose scaling, Docker Swarm,
  Kubernetes and Helm deployment configs. See the [Deployment Guide](dc3/doc/DEPLOYMENT.md).
- **Complete documentation** - Online docs, quickstart guide, and troubleshooting guide

## ⚡ Quick Start

For source-based local development, start PostgreSQL and RabbitMQ, load local environment variables, then build:

```bash
make up-db
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package
```

Use `make up-db-cn` if you prefer the Alibaba Cloud registry in Mainland China.

> 📖 For service startup order, IDE setup, verification commands, and common pitfalls,
> see the [full Quickstart](https://docs.dc3.site/en/quickstart/).

## 🛠️ Technology Stack

IoT DC3 is built on Java 21, Spring Boot 4, Spring Cloud 2025, Spring AI 2, PostgreSQL, a pluggable message broker
(RabbitMQ, Kafka, Pulsar or MQTT 5 — [selection guide](docs/mq-brokers.md)), gRPC, Vue 3,
TypeScript, and Vite.

See [Technology Stack](https://docs.dc3.site/en/development/technology-stack) for component details and where each
technology is used.

## 📖 Documentation and Community

| Resource              | Link                                                                            |
|-----------------------|---------------------------------------------------------------------------------|
| 📚 Online docs        | [docs.dc3.site](https://docs.dc3.site/)                                         |
| 🎬 Live demo          | [demo.dc3.site](https://demo.dc3.site/)                                         |
| 🏭 Industry demos     | [dc3.site/en/demo](https://dc3.site/en/demo/)                                   |
| 🚀 Quickstart         | [Quickstart Guide](https://docs.dc3.site/en/quickstart/)                        |
| 🛠️ Technology stack   | [Technology Stack](https://docs.dc3.site/en/development/technology-stack)       |
| 🏗️ Architecture       | [Modules and Dependencies](https://docs.dc3.site/en/architecture/modules)       |
| 🔧 Driver development | [Driver Authoring Guide](https://docs.dc3.site/en/development/driver-authoring) |
| 🐛 Troubleshooting    | [Troubleshooting](https://docs.dc3.site/en/guide/troubleshooting)               |
| 📋 Changelog          | [Release Changelog](https://docs.dc3.site/en/development/changelog)             |
| 💰 Pricing & licensing | [Plans and commercial license](https://dc3.site/en/pricing/)                  |
| 🐛 Issue feedback     | [GitHub Issues](https://github.com/pnoker/iot-dc3/issues)                       |
| 🇨🇳 Gitee mirror       | [Gitee GVP Project](https://gitee.com/pnoker/iot-dc3)                           |

## 🌍 Use Cases

Twelve illustrative industry dashboards — built on IoT DC3 with mock data — show how the platform lands in each
scenario. [Browse all demos](https://dc3.site/en/demo/).

|                    |                                                              |                                                        |
|--------------------|--------------------------------------------------------------|--------------------------------------------------------|
| 🏭 [Smart Factory](https://dc3.site/en/demo/smart-factory/) — OEE monitoring | 💧 [Water Network](https://dc3.site/en/demo/water-network/) — digital twin | ⚡ [Microgrid](https://dc3.site/en/demo/microgrid/) — solar-storage balance |
| 🌾 [Precision Agriculture](https://dc3.site/en/demo/precision-agri/) — greenhouse climate | 🏢 [Smart Building](https://dc3.site/en/demo/smart-building/) — HVAC and occupancy | 🚦 [Smart Traffic](https://dc3.site/en/demo/smart-traffic/) — congestion and signals |
| 🛢️ [Oil & Gas Pipeline](https://dc3.site/en/demo/oil-gas/) — line pressure | ⛏️ [Smart Mine](https://dc3.site/en/demo/smart-mine/) — gas and ventilation | ❄️ [Cold Chain](https://dc3.site/en/demo/cold-chain/) — temperature traceability |
| 🌿 [Environmental Monitoring](https://dc3.site/en/demo/eco-monitor/) — air and water quality | ⚓ [Smart Port](https://dc3.site/en/demo/smart-port/) — berth and yard scheduling | 🔌 [EV Charging](https://dc3.site/en/demo/ev-charging/) — load and storage synergy |

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
