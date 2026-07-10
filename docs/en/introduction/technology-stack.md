---
title: Technology Stack
---

# Technology Stack

This page summarizes the main technologies IoT DC3 currently runs on and recommends for local development. Treat
`pom.xml`, `dc3-web/package.json`, and `docs/package.json` as the source of truth for exact versions; the README keeps
only a short reader-facing summary.

## Backend and Center Services

| Area | Technologies | Purpose |
|------|--------------|---------|
| Language and frameworks | Java 21 · Spring Boot 4 · Spring Cloud 2025 | Runtime foundation for the gateway, center services, and driver processes |
| AI integration | Spring AI 2.0 | Agentic Center integration with OpenAI-compatible providers, Tool Calling, and MCP workflows |
| Web and API | Spring WebFlux · Spring Security · springdoc-openapi | HTTP APIs, authentication and authorization, and aggregated API docs |
| Service collaboration | gRPC · Protobuf · Facade interfaces | Strongly typed contracts between center services |
| Build | Maven 3.9+ | Multi-module build, tests, packaging, and dependency version management |

## Data, Messaging, and Scheduling

| Area | Technologies | Purpose |
|------|--------------|---------|
| Primary storage | PostgreSQL | Business data, tenants, resources, device models, and runtime data |
| Time-series and extensions | TimescaleDB · AGE · pgvector | Point history, graph capabilities, and vector capabilities |
| ORM / data access | MyBatis-Plus | Persistence access and paginated queries at the DO layer |
| Message bus | RabbitMQ | Asynchronous point value reports, command dispatch, and buffering between drivers and the data center |
| Cache and scheduling | Caffeine · Quartz | In-process caching, scheduled jobs, and task orchestration |

## Frontend, Docs, and Automation

| Area | Technologies | Purpose |
|------|--------------|---------|
| Web frontend | Vue 3 · TypeScript 6 · Vite 8 · Element Plus | Management console under `dc3-web/` |
| Visualization | AntV G2/G6 | Dashboard charts and relationship visualizations |
| Docs site | VitePress · Mermaid | The current `docs/` site, architecture diagrams, and flow diagrams |
| CLI automation | TypeScript · pnpm · Vitest | Sibling `dc3-cli/` project, a Gateway-oriented command-line client |
| Container deployment | Podman · Docker Compose | Local dependencies, dev stack, app stack, and optional observability stack |

## Continue Reading

- [Local Development from Source](../quickstart/) — start dependencies, load environment variables, build, and verify
- [Frontend Development](../frontend/) — running, structure, and test commands for `dc3-web/`
- [System Architecture Overview](../architecture/) — how the gateway, centers, drivers, message bus, and storage work together
- [Module Map](../architecture/modules) — Maven modules, deployable units, and dependencies
