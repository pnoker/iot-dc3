# DC3 Common Agentic

## Overview

`dc3-common-agentic` is the agentic / AI SDK of the IoT DC3 platform and the engine behind `dc3-center-agentic`. Built
on Spring AI, it provides the chat client, conversation memory, model management, and the suite of platform tools the
model uses to read and operate platform resources.

## Module Information

- **Group ID**: io.github.pnoker
- **Artifact ID**: dc3-common-agentic
- **Version**: 2026.5.22

## Key Components

| Area            | Components                                                                                          |
|-----------------|-----------------------------------------------------------------------------------------------------|
| Chat client     | `ChatClientConfig`, `ChatClientFactory` — build Spring AI `ChatClient`s per model configuration     |
| Memory          | `MessageChatMemoryRepository` — JDBC-backed conversation memory                                     |
| Configuration   | `AgenticProperties` (prefix `dc3.agentic`), `AgenticApiGroupConfig`, `AgenticAutoConfiguration`     |
| Tools           | `DriverTool`, `DeviceTool`, `PointTool`, `PointValueTool`, `ProfileTool`, `CommandTool`, `EventTool`, `UserTool`, `TenantTool`, `SystemTool` |
| Model mgmt      | `ModelController` + MapStruct mappers (`Session`, `Message`, `Attachment`, `ModelProvider`, `ModelConfig`, `Action`) |
| Tool support    | `AgenticToolMetadata` annotation, `AgenticToolUtil`, `AgenticToolContextUtil`, `AgenticConversationIdUtil`, `AgenticVisualizationUtil`, `AgenticTokenEstimatorUtil` |

## Configuration

- Bound from the `dc3.agentic` prefix (`AgenticProperties`).
- An OpenAI-compatible fallback model is configured via the `AGENTIC_FALLBACK_OPENAI_*` environment variables (see `.env.example`).
- Conversation memory uses the Spring AI JDBC chat-memory repository.

## Dependencies

- `spring-ai-starter-model-chat-memory-repository-jdbc` — Spring AI chat client + JDBC memory

## Build Instructions

```bash
mvn -s ../../.mvn/settings.xml clean package
```

## Related Modules

- `dc3-center-agentic` — the service shell that exposes this SDK over HTTP (`/api/v3/agentic/**`)
- `dc3-common-facade-*` — facades the platform tools call to reach other services

## License

Copyright 2016-present the IoT DC3 original author or authors.

Licensed under the GNU Affero General Public License v3.0 (AGPL 3.0)
