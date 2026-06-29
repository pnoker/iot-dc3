---
layout: home

hero:
  name: IoT DC3
  text: An open-source distributed IoT platform built for AI scenarios
  tagline: Device connectivity, data collection, operations management, and intelligent analytics for industrial IoT. 28 multi-protocol drivers bring data up from heterogeneous devices; Spring AI lets large language models read that data and drive commands back to devices, closing the sense–decide–act–feedback loop. Distributed, multi-tenant, fully open source.
  image:
    src: /images/logo.svg
    alt: IoT DC3
  actions:
    - theme: brand
      text: Run your first device
      link: /en/quickstart/first-device
    - theme: alt
      text: Run from source
      link: /en/quickstart/

features:
  - icon: 🧭
    title: Overview
    details: Positioning, core concepts, and role-based learning paths — what it is, who it's for, and how to start.
    link: /en/introduction/
    linkText: Read the pitch
  - icon: 🏗️
    title: Architecture
    details: Service topology, data and command planes, auth and tenancy, the domain model and module map — with sequence and state diagrams.
    link: /en/architecture/
    linkText: Understand the design
  - icon: 🔌
    title: Drivers
    details: 28 multi-protocol drivers for heterogeneous devices, with the onboarding flow and a driver capability matrix.
    link: /en/drivers/
    linkText: Browse drivers
  - icon: 📚
    title: Foundations
    details: The four-layer IoT technology stack — perception, network, platform, application and security — each tied back to how DC3 implements it.
    link: /en/foundations/
    linkText: Learn the stack
  - icon: 🛠️
    title: Develop
    details: Derive new drivers from the Driver SDK, API docs and testing, the dc3 CLI and AI Agent / MCP integration.
    link: /en/development/
    linkText: Start building
  - icon: 🧰
    title: Operations
    details: Data collection and read/write commands, alarms and notifications, deployment modes and registries, observability, logging and troubleshooting.
    link: /en/operation/
    linkText: Operate & maintain
---

## What is IoT DC3

IoT DC3 is an open-source, distributed IoT platform built for AI scenarios (AGPL-3.0). It covers **device connectivity,
data collection, operations management, and intelligent analytics** for industrial IoT. **28 driver modules** pull data
up from heterogeneous devices and normalize it into semantically labeled point values; **Spring AI** then plugs large
language models into operations — a model can query devices, read and write points, run commands, analyze alarms, and
surface insights, closing the sense–decide–act–feedback loop.

It's a good fit for teams that need to connect many industrial protocols, manage devices and points, query real-time and
historical data, and build on the platform within the Spring ecosystem — including bringing AI into operations. To
understand the problem it solves and how it compares to alternatives, start with [the pitch](/en/introduction/).

## Architecture at a glance

The platform is one gateway, four center services, and a set of protocol drivers. Only the gateway's HTTP port faces
outward; the centers talk to each other over gRPC, and drivers and the data center are decoupled through RabbitMQ.

<TopologyDiagram lang="en" />

For how each hop works and why it's designed that way, see [Architecture](/en/architecture/).

## Tech stack

- **Language & frameworks
  **: [Java 21](https://www.java.com) · [Spring Boot 4](https://spring.io/projects/spring-boot) · [Spring Cloud 2025](https://spring.io/projects/spring-cloud) · [Spring AI 2.0.0](https://spring.io/projects/spring-ai)
- **Data, cache & scheduling**: PostgreSQL (+ TimescaleDB / AGE / pgvector) · Caffeine · MyBatis-Plus · Quartz
- **Messaging**: RabbitMQ · gRPC · MQTT (Paho + EMQX) · Protobuf
- **Security**: Spring Security · JWT · BouncyCastle
- **Frontend**: Vue 3 · TypeScript 6 · Vite 8 · Element Plus · AntV G2/G6 (source in `dc3-web/` directory of this repo;
  the standalone `iot-dc3-web` repo is archived)

## License

IoT DC3 is open source under the [AGPL-3.0 License](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt).
For licensing details and commercial licensing,
see [LICENSE.txt](https://github.com/pnoker/iot-dc3/blob/release/LICENSE.txt).
