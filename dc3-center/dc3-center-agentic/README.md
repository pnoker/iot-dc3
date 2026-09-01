# DC3 Center Agentic

## Overview

`dc3-center-agentic` is the AI / agentic center of the IoT DC3 platform. It exposes an LLM-backed conversational and
tool-calling service (built on Spring AI) that lets users query and operate platform resources — drivers, devices,
points, point values, profiles, commands, and events — through natural language. The service is a thin Spring Boot
shell; the agentic logic lives in `dc3-common-agentic`.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-center-agentic
- **Package**: `io.github.pnoker.center.agentic`

## Service Ports

| Protocol | Port   | Override           |
|----------|--------|--------------------|
| HTTP     | `8600` | `DC3_AGENTIC_PORT` |

The WebFlux base path is `/agentic`; through the gateway the service is reached at `/api/v3/agentic/**`.

## Key Responsibilities

- **Conversational AI**: chat sessions backed by a Spring AI `ChatClient`, with configurable model providers and
  per-conversation memory.
- **Tool calling**: a suite of platform tools (`DriverTool`, `DeviceTool`, `PointTool`, `PointValueTool`, `ProfileTool`,
  `CommandTool`, `EventTool`, `UserTool`, `TenantTool`, `SystemTool`) lets the model read and act on platform data.
- **Model management**: model providers and model configs are managed via REST controllers (e.g. `ModelController`) and
  persisted through MyBatis mappers.
- **Facade access**: cross-service reads/writes go through the gRPC facade (`dc3-common-facade-grpc`).

## Configuration

- `DC3_AGENTIC_PORT` — HTTP port (default `8600`)
- `DC3_FACADE_GRPC_DEADLINE_MS` — cross-service gRPC deadline in milliseconds (default `3000`)
- Model/provider settings are bound via `AgenticProperties`, with an OpenAI-compatible fallback configured through the
  `AGENTIC_FALLBACK_OPENAI_*` environment variables (see `dc3/.env.example` / repo-root `.env.example`).

## Running Locally

### 1. Start Infrastructure and Center Services

```bash
make up-db
make up-dev SERVICES="auth manager data"
```

The agentic service has gRPC facade channels for Auth, Manager, and Data centers. Start all three before exercising
platform tools; the gateway is optional when calling the agentic service directly.

### 2. Build and Run

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-agentic -am package
java -jar dc3-center/dc3-center-agentic/target/dc3-center-agentic.jar
```

## Testing

Run the module tests from the repository root:

```bash
mvn -s .mvn/settings.xml -pl dc3-center/dc3-center-agentic -am test
```

## Related Modules

- `dc3-common-agentic` — agentic SDK: Spring AI chat client, platform tools, conversation memory, model management
- `dc3-common-facade-grpc` — gRPC facade for cross-service access
- `dc3-common-resource-registrar` — registers this service's API / menu resources
