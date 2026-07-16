---
title: Platform Positioning
---

<script setup>
import IntroductionLoopDiagram from '../../.vitepress/theme/components/IntroductionLoopDiagram.vue'
import IntroductionArchitectureDiagram from '../../.vitepress/theme/components/IntroductionArchitectureDiagram.vue'
</script>


# Platform Positioning

IoT DC3 is an open-source distributed IoT platform built for AI scenarios. It spans device connectivity, data
collection, operations management, and intelligent analytics — and ties them into a single closed loop: multi-protocol
drivers collect data from heterogeneous devices and normalize it into point values that machines and people can both
read, then large language models read that data and push commands back to the devices. By the end of this page you'll
know what problems it solves, who it's for, and how it differs from typical IoT platforms.

> Want to jump straight in? Go to [Quick Start](../quickstart/). Want the object model first?
> See [Core Concepts](./concepts).

## The Two Gaps It Closes

Most industrial sites get stuck in one of two places:

1. **Data can't get out, so AI can't use it.** Device data sits across different protocols and registers, with
   inconsistent formats and no semantics. Even when AI reaches it, there's nothing to consume.
2. **AI can only watch, not act.** When analytics or LLMs are integrated, they're usually read-only — they can't push
   decisions down to devices for execution. The loop breaks at the last step.

Traditional IoT platforms tend to solve only one side. They're either strong at device connectivity or strong at
analytics, and few close the full loop of collect → normalize → analyze → execute → feed back. Closing exactly these two
gaps is the design goal of IoT DC3.

## One Closed Loop: From Device Data to AI Execution

Turn that goal into a runnable pipeline and you get the core of how IoT DC3 works: drivers collect → the data center
normalizes and stores → the LLM reads and analyzes → commands go out through tool calls → devices execute and
acknowledge.

<IntroductionLoopDiagram lang="en" />

The key to the loop is what a point value is. It's not raw data — it's a structured `PointValue` that carries semantic
tags, a unit, a timestamp, and tenant context. The LLM calls platform APIs through Spring AI's native `@Tool`, so it can
read and write, and every step is bounded by permission and confirmation checks (see [Agentic Center](../ai/agentic)).

## Who It's For

- **IoT platform builders** who need to unify and centrally manage devices across many industrial protocols.
- **Smart factory and equipment teams** doing production-line monitoring, device health, and predictive maintenance.
- **Remote monitoring and control operators** in energy, agriculture, and urban infrastructure.
- Backend developers at home in the Spring ecosystem who want to build on the platform.
- Teams exploring AI-assisted operations, or agents that operate devices.

## Typical Scenarios

| Scenario          | What You Do with IoT DC3                                                                  |
|-------------------|-------------------------------------------------------------------------------------------|
| Smart Factory     | Production-line monitoring, device health and predictive maintenance, OEE statistics      |
| Energy Monitoring | Remote metering and measurement, anomaly alerting                                         |
| Smart Agriculture | Greenhouse environment monitoring, irrigation control, yield prediction                   |
| Smart City        | Monitoring and remote operation of street lighting, environment, and municipal facilities |

## Capability Pillars

1. **Multi-protocol device connectivity** — 28 driver modules cover industrial fieldbuses, IoT wireless protocols,
   database bridging, basic communication, and simulation.
2. **AI capability integration** — the agentic center is built on Spring AI, so LLMs read and write points, run
   commands, and analyze alarms through tool calling, compatible with mainstream models including GPT, Claude, DeepSeek,
   and Qwen, with conversation memory persisted to the database.
3. **Cloud-native microservices** — Spring Boot 4 and Spring Cloud 2025, with the gateway as the single entrypoint, gRPC
   between services, stateless nodes that scale horizontally, and fault isolation.
4. **Real-time data engine** — drivers push telemetry through RabbitMQ into time-series storage, a rule engine drives
   multi-level alarms, and full command and event history keeps things traceable.
5. **Multi-tenant security and isolation** — `tenantId` runs through queries, gRPC calls, and cache keys, with isolation
   enforced at the database, cache, and API layers, plus JWT auth, RBAC, TLS, and audit logs.
6. **Developer friendly** — a Driver SDK for custom protocols, a separated Vue 3 + TypeScript frontend with REST and
   gRPC APIs, one-command startup with Podman or Docker Compose, and a path toward Kubernetes.

## How It Differs from Traditional IoT Platforms

What sets IoT DC3 apart isn't multi-protocol support on its own — plenty of platforms have that. The combined advantage
is:

- **AI-native integration**: built in through Spring AI, not bolted on as a separate analytics service.
- **Protocol breadth**: 28 drivers, including less common ones like database bridging.
- **Structured AI output**: `PointValue` carries semantic tags, so models consume it directly.
- **Closed-loop command execution**: LLM decisions go back down to devices for execution.
- **Fully open source**: no proprietary core.
- **Multi-tenant by design**: isolation is the foundation, not a patch.

::: info An Honest Boundary
Multi-protocol support on its own isn't a differentiator — it's the price of admission. IoT DC3 also does **not**
currently onboard RTSP/H.264 video streams. If video is your core requirement, evaluate it separately.
:::

## Tech Stack at a Glance

Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.1 · Spring AI 2.0.0 · PostgreSQL (+ TimescaleDB / AGE / pgvector) ·
RabbitMQ · gRPC / Protobuf · MyBatis-Plus (Snowflake ID).

## System Panorama

The platform exposes one HTTP entry point — the gateway. Four centers sit behind it, each with its own job, and drivers
onboard devices on the southbound side. The diagram shows how the roles fit together; for a layer-by-layer walkthrough,
see [System Architecture](../architecture/).

<IntroductionArchitectureDiagram lang="en" />

## Further Reading

- [Core Concepts and Mental Model](./concepts) — how drivers, templates, devices, and points relate
- [Choosing a Path by Role](./paths) — distinct entry points for evaluation, onboarding, development, and contribution
- [Quick Start](../quickstart/) — bring up the stack locally and run your first device end to end
- [System Architecture](../architecture/) — break the closed loop down into the implementation details of every hop
