---
layout: home

hero:
  name: IoT DC3
  text: An open-source distributed IoT platform built for AI scenarios
  tagline: Device connectivity, data collection, operations management, and intelligent analytics for industrial IoT. 28 multi-protocol drivers bring data up from heterogeneous devices; Spring AI lets large language models read that data and drive commands back to devices, closing the sense–decide–act–feedback loop. Distributed, multi-tenant, fully open source.
  image:
    src: /images/hero-logo.svg
    alt: IoT DC3
  actions:
    - theme: brand
      text: Quick Start
      link: /en/quickstart/
    - theme: alt
      text: Run your first device
      link: /en/quickstart/first-device
    - theme: alt
      text: View on GitHub
      link: https://github.com/pnoker/iot-dc3

features:
  - icon: 🧭
    title: Introduction
    details: What it is, who it's for, the problem it solves, and how it differs from comparable platforms.
    link: /en/introduction/
    linkText: Read the pitch
  - icon: 🚀
    title: Quick Start
    details: Bring up the dependencies, load the environment, and go from the virtual driver to your first live data point — every command ready to copy.
    link: /en/quickstart/
    linkText: Get started
  - icon: 🏗️
    title: Architecture
    details: Service topology, the data and command planes, auth and tenancy, the domain model — with sequence and state diagrams.
    link: /en/architecture/
    linkText: Understand the design
  - icon: 🤖
    title: AI
    details: Drive operations in natural language with the Agentic Center, and expose tools safely to external AI agents over MCP.
    link: /en/ai/
    linkText: Wire up AI
  - icon: ⚡
    title: Automation
    details: Drive the platform from the `dc3` CLI — local debugging, CI pipelines, ops scripts.
    link: /en/automation/
    linkText: Script it
  - icon: 🧰
    title: Operation Guide
    details: Onboard devices, collect data and run read/write commands, and configure alarms and notifications — the everyday operations flow.
    link: /en/operation/
    linkText: See the flows
  - icon: 🛠️
    title: Development
    details: Derive a new protocol driver from the Driver SDK template, plus API docs, testing, and coding conventions.
    link: /en/development/
    linkText: Start building
  - icon: 📦
    title: Deploy & Ops
    details: Deployment modes and image registries, the optional observability stack (EMQX/ELK/Prometheus/Grafana), logging, and troubleshooting.
    link: /en/guide/
    linkText: Ship it
  - icon: 🤝
    title: Community
    details: Contributing guide, code of conduct, and security disclosure policy — drivers, fixes, and docs all welcome.
    link: /en/community/contributing
    linkText: Get involved
---

## What is IoT DC3

IoT DC3 is an open-source, distributed IoT platform built for AI scenarios (AGPL-3.0). It covers **device connectivity, data collection, operations management, and intelligent analytics** for industrial IoT. **28 driver modules** pull data up from heterogeneous devices and normalize it into semantically labeled point values; **Spring AI** then plugs large language models into operations — a model can query devices, read and write points, run commands, analyze alarms, and surface insights, closing the sense–decide–act–feedback loop.

It's a good fit for teams that need to connect many industrial protocols, manage devices and points, query real-time and historical data, and build on the platform within the Spring ecosystem — including bringing AI into operations. To understand the problem it solves and how it compares to alternatives, start with [the pitch](/en/introduction/).

## Architecture at a glance

The platform is one gateway, four center services, and a set of protocol drivers. Only the gateway's HTTP port faces outward; the centers talk to each other over gRPC, and drivers and the data center are decoupled through RabbitMQ.

![IoT DC3 Architecture](/images/iot-dc3-architecture-en.svg){.dc3-arch-diagram}

For how each hop works and why it's designed that way, see [Architecture](/en/architecture/).

## Tech stack

- **Language & frameworks**: [Java 21](https://www.java.com) · [Spring Boot 4](https://spring.io/projects/spring-boot) · [Spring Cloud 2025](https://spring.io/projects/spring-cloud) · [Spring AI 2.0.0](https://spring.io/projects/spring-ai)
- **Data, cache & scheduling**: PostgreSQL (+ TimescaleDB / AGE / pgvector) · Caffeine · MyBatis-Plus · Quartz
- **Messaging**: RabbitMQ · gRPC · MQTT (Paho + EMQX) · Protobuf
- **Security**: Spring Security · JWT · BouncyCastle
- **Frontend**: Vue 3 · TypeScript 6 · Vite 8 · Element Plus · AntV G2/G6 (in the separate [`iot-dc3-web`](https://github.com/pnoker/iot-dc3-web) repo)

## License

IoT DC3 is open source under the [AGPL-3.0 License](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt). For licensing details and commercial licensing, see [LICENSE.txt](https://github.com/pnoker/iot-dc3/blob/release/LICENSE.txt).
