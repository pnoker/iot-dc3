---
title: "Agentic Center: AI-Assisted Operations"
---

<script setup>
import AgenticSequenceDiagram from '../../.vitepress/theme/components/AgenticSequenceDiagram.vue'
import AgenticErDiagram from '../../.vitepress/theme/components/AgenticErDiagram.vue'
</script>


# Agentic Center: AI-Assisted Operations

The Agentic Center (`dc3-center-agentic`) connects an OpenAI-compatible large language model to IoT DC3's devices,
points, data, and commands. You ask questions in plain language, and the model calls the platform's built-in tools to
read metadata, query live values, and trigger device reads and writes under controlled authorization. This page covers
how that chain works, which tools exist, where conversations are stored, and which actions need a human to confirm.

> You are here: you've already [onboarded devices](../operation/device-onboarding) and
> can [query data and issue commands](../operation/data-commands). Now you want the model to help operate. Next,
> see [AI Agent / MCP Integration](./mcp) to bring external agents in.

## Why a dedicated Agentic Center

Once devices are connected and data is persisted, day-to-day operations are usually a string of small "check first, then
decide" actions: Which device went offline? How has this point trended over the last hour? Should we write a switch
back? Each step maps to an HTTP endpoint, but chaining them by hand is slow and error-prone.

The Agentic Center hands this "understand intent → pick the right tool → fetch data → answer" layer to the LLM. Built on
Spring AI's `ChatClient`, it exposes an OpenAI-compatible chat interface externally while hosting a set of *
*tenant-isolated built-in tools** internally. Within a single conversation, the model decides which tools to call and in
what order, then explains the conclusion in plain language.

AI isn't a prerequisite for device onboarding. Get the device, point, data, and command chains working end to end first,
then enable the Agentic Center — it consumes exactly the data and interfaces those chains produce.

::: warning Tool calling is on by default, but you can turn it off
Built-in tool calling is governed by `AGENTIC_TOOL_CALLING_ENABLED`, which defaults to `true`. Set it to `false` and the
model drops to pure conversation, no longer touching any device or data interface. If you only want Q&A in a restricted
environment, turn it off.
:::

## OpenAI-compatible chat entry point

The Agentic Center exposes a single core entry point shaped like OpenAI's Chat Completions, so any OpenAI client can
connect directly:

- Path: `POST /api/v3/agentic/chat/completions` through the gateway. The gateway's `StripPrefix=2` removes `/api/v3`
  and forwards to the Agentic Center's `/chat/completions`.
- Two response modes: with `stream=true` it returns token-by-token over **SSE**; otherwise it returns a single **JSON**
  response.
- Permission: `@PreAuthorize("@perm.can('chat', 'list')")` — the caller must carry the platform auth headers
  `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token`.
- This endpoint's own AI risk metadata is annotated as `riskLevel=MEDIUM`, `destructive=false`, `idempotent=false`,
  `openWorld=true`.

Here's the shape of a non-streaming exchange. The example values are illustrative only; replace the auth headers with
the real token you get after logging in.

::: code-group

```bash [curl]
curl -X POST http://localhost:8000/api/v3/agentic/chat/completions \
  -H "Content-Type: application/json" \
  -H "X-Auth-Tenant: <tenant>" \
  -H "X-Auth-Login: <login>" \
  -H "X-Auth-Token: <token>" \
  -d '{
        "model": "gpt-4o",
        "stream": false,
        "messages": [
          { "role": "user", "content": "What is the latest value of boiler #1 temperature point?" }
        ]
      }'
```

```json [Response JSON shape (example)]
{
  "id": "chatcmpl-...",
  "object": "chat.completion",
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "The current value of boiler #1 temperature point is 86.4 °C, collected 2 minutes ago."
      },
      "finish_reason": "stop"
    }
  ]
}
```

:::

To get the auth token above, first `POST /api/v3/auth/token/salt` to fetch the salt, then
`POST /api/v3/auth/token/generate` to exchange it for an access token valid for 12 hours. See the login steps
in [Your First Device: End to End](../quickstart/first-device).

## How an AI-assisted operation flows

The model doesn't answer out of thin air. During the conversation it calls built-in tools to read real data. The diagram
below shows a mixed "read + write" operation: read-type tools execute directly, while write-type (high-risk) tools pause
for human confirmation before continuing.

<AgenticSequenceDiagram lang="en" />

Key facts along the critical path:

- Tools are native Spring AI `@Tool` methods. They're registered as a `ToolCallbackProvider` by `ChatClientConfig` and
  wrapped in an `AgenticToolTracingCallbackProvider` for call tracing.
- Every tool method starts with `AgenticToolContextUtil.requireTenantId(toolContext)` to extract the current tenant ID,
  so all queries carry tenant scope — the model **cannot see or touch** another tenant's data.
- Write-type actions (command dispatch) go from the Agentic Center to the **Data Center**'s command plane via
  `PointCommandFacade` (the gRPC implementation `PointCommandGrpcFacade`, or the local `PointCommandLocalFacade`). They
  don't call the HTTP `POST /point_command/write` endpoint — that's the Data Center's separate Web/CLI-facing call
  surface, terminating at the same command plane. The command itself has a 10-second TTL (`PointCommandDTO.expireAt`
  defaults to `now+10s`) and won't execute once expired.

## Ten built-in tools

The platform ships **10** built-in tool classes covering the full read surface — from tenants and users down to devices
and commands — with a few that perform writes. Tool methods mainly use the verbs `lookup*` (fetch one or many by ID) and
`search*` (paginated query), plus `list*ByXxxId` (enumerate by ownership). These don't follow the REST layer's `getXxx`/
`listXxx` convention. They're named for the model's benefit, independent of the external HTTP CRUD convention.

Get a feel for what each domain can do first, then read the table. Given a question, the model decomposes it into a tool
sequence — say, "first look up which points a profile has → then read the latest values of those points → finally decide
whether to issue a command" — and orchestrates the calls automatically.

| Tool class       | Domain      | Representative methods                                                                        | Typical use                                                                 |
|------------------|-------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `TenantTool`     | Tenant      | `getCurrentTenantInfo()`                                                                      | Confirm the current tenant context                                          |
| `UserTool`       | User        | `getCurrentUserProfile()`                                                                     | Look up the current user's info                                             |
| `DeviceTool`     | Device      | `lookupDeviceById()` / `searchDevices()`                                                      | Look up a device, search by criteria, check online status and latest values |
| `DriverTool`     | Driver      | `lookupDriverById()` / `searchDrivers()`                                                      | Check protocol driver connectivity, device online statistics                |
| `ProfileTool`    | Profile     | `lookupProfileById()` / `searchProfiles()`                                                    | Look up profiles and their capabilities                                     |
| `PointTool`      | Point       | `lookupPointById()` / `searchPoints()`                                                        | Look up points, read/write direction, list points by device/profile         |
| `PointValueTool` | Point value | `getLatestPointValue()` / `getPointValueHistory()` / `readPointValue()` / `writePointValue()` | Read live values, query history curves, issue read/write commands           |
| `SystemTool`     | System      | `getSystemHealth()`                                                                           | Check platform health                                                       |
| `CommandTool`    | Command     | `lookupCommandById()` / `searchCommands()`                                                    | Look up custom commands, list commands by device/profile                    |
| `EventTool`      | Event       | `lookupEventById()` / `searchEvents()`                                                        | Look up events reported by devices                                          |

::: info Risk metadata is annotated on REST endpoints, not tool methods
The `x-dc3-ai` risk metadata (`riskLevel` / `destructive` / `idempotent` / `openWorld`) is **hand-annotated** in the
Controller's `@Operation` extension (e.g. the chat endpoint of `ChatController`) for the OpenAPI / MCP catalog to
consume. The Agentic Center's 10 tool methods carry only `@AgenticToolMetadata(domain, title)` — just the two fields
`domain()` and `title()` — and no risk level. Don't treat endpoint-level risk metadata as a property of each tool
method.
:::

## Conversations live in the database, not in memory

Many AI services keep conversation context in memory and lose it the moment the process restarts. The Agentic Center
doesn't. It **persists every turn in the `dc3_agentic` schema**, and the `MessageChatMemoryRepository` adapter reads
history back from `dc3_message` by `conversation_id`. Conversations survive restarts and can be audited.

Here's how the three tables relate:

<AgenticErDiagram lang="en" />

- The retrieval window size is controlled by `dc3.agentic.historyWindowSize` (default `30`): only the most recent few
  turns are fed to the model, saving tokens while keeping context.
- `dc3_session.session_ext` is a JSON blob holding this conversation's model choice, temperature, `maxTokens`, and other
  preferences, carried into the next turn.
- Uploaded attachments land in the directory pointed to by `AGENTIC_ATTACHMENT_STORAGE_PATH`, with metadata recorded in
  `dc3_attachment`.

::: warning Memory table schema isn't auto-created by default
Persisted conversations depend on database tables. `AGENTIC_MEMORY_SCHEMA_INIT` is injected via compose / `dev.env` and
defaults to `never` — meaning it does not auto-initialize Spring AI's memory table schema. To get auto-creation, the
expected approach is to set it to `always` (or `create_if_not_exists`) once to create the tables, then set it back to
`never`. Note: the repository currently shows no `application*.yml` binding this variable to Spring AI's
`initialize-schema`, so whether it's actually wired up **should be verified against the code**. When in doubt,
initializing the memory tables by hand is safer.
:::

## Two-phase confirmation for high-risk actions

Not every tool call should complete in one shot. Read-type tools (`lookup*` / `search*` / `getLatest*`) are safe and run
directly; write-type tools are hard to roll back once they go wrong. The Agentic Center's only write tool,
`PointValueTool.writePointValue`, **never writes directly**. It goes through two-phase confirmation:

1. When the model calls `writePointValue`, the service calls `ActionService.createWritePointValueAction(...)` to
   generate a pending **Action** (with `actionId` as a UUID), sets its status to `AgenticActionStatusEnum.PENDING`, and
   sets the expiry to `now + 10 minutes`. The tool result carries `pendingConfirmation=true` and that `actionId` — no
   command is issued.
2. Once the user has reviewed the action's contents, they confirm it with `POST /action/confirm` (or reject it with
   `POST /action/reject`), passing the `action_id`. Only after confirmation does the service execute it via
   `PointCommandFacade.submitWrite(...)`.

This keeps the split clean: the AI proposes, a human decides. Irreversible physical-world actions stay within human
authorization.

::: info This is Agentic's own Action mechanism, not the MCP gateway's risk gate
The confirmation flow uses `actionId` + `POST /action/confirm|reject` — **not** `CONFIRM_REQUIRED` / `confirmId`, and
not a generic risk policy gated on `riskLevel=HIGH`. The latter belongs to the [MCP gateway](./mcp)'s
`dc3_mcp_tool_confirmation` subsystem, a separate implementation from this chat chain. Don't conflate the two.
:::

::: danger A failed write command never echoes a fabricated value
A write command issued through a tool ultimately goes to the Data Center's command plane. The command carries a
10-second TTL (`PointCommandDTO.expireAt` defaults to `now+10s`), and **when a write command fails, `responseValue`
is `null` and no value is echoed** — never treat "no error" as "write succeeded."
See [Command Plane](../architecture/command-plane).
:::

## Where the model comes from: database first, env fallback

The Agentic Center supports multiple model providers. `ChatClientFactory` first reads enabled provider configuration
from the database table **`dc3_model_provider`** (`provider_type`: `0` openai-compatible / `1` anthropic, `base_url`,
`api_key`, `default_flag`, etc., tenant-isolated). Only when the table has no usable provider does it fall back to a set
of environment-variable defaults.

In other words: manage providers centrally in the database in production. The `AGENTIC_FALLBACK_*` env vars are just the
last line of defense when there's no DB configuration.

| Variable                              | Default                        | Purpose                                                                      |
|---------------------------------------|--------------------------------|------------------------------------------------------------------------------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL`    | `https://api.openai.com`       | Fallback OpenAI-compatible API address                                       |
| `AGENTIC_FALLBACK_OPENAI_API_KEY`     | *(empty)*                      | Fallback API key (when the endpoint requires auth)                           |
| `AGENTIC_FALLBACK_OPENAI_MODEL`       | `gpt-4o`                       | Fallback model name                                                          |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | `0.7`                          | Sampling temperature (0.0–2.0)                                               |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS`  | `2048`                         | Maximum output tokens                                                        |
| `AGENTIC_TOOL_CALLING_ENABLED`        | `true`                         | Whether to enable tool calling                                               |
| `AGENTIC_MEMORY_ENABLED`              | `false`                        | Whether to enable persisted conversation memory                              |
| `AGENTIC_MEMORY_MAX_MESSAGES`         | `50`                           | Max messages in a single conversation window                                 |
| `AGENTIC_MEMORY_SCHEMA_INIT`          | `never`                        | Memory table schema initialization (`always`/`never`/`create_if_not_exists`) |
| `AGENTIC_ATTACHMENT_STORAGE_PATH`     | `dc3/data/agentic/attachments` | Attachment storage directory                                                 |

::: tip Defaults reflect compose / `dev.env`, not the Spring bare defaults
The defaults above are the values injected by compose / `dev.env`, which **differ** from the bare Spring defaults in
`application-agentic.yml`: e.g. `AGENTIC_MEMORY_ENABLED` defaults to `true` in Spring (compose injects `false`), and
`AGENTIC_ATTACHMENT_STORAGE_PATH` defaults to `dc3/data/upload/agentic/attachment` in Spring (compose injects
`dc3/data/agentic/attachments`). When you start via compose / `make up-*`, this table applies; if you run Spring
directly in an IDE without compose, the yml bare defaults apply instead.
:::

See [Environment Variables](../quickstart/environment) for the full reference.

::: danger Never leak an API key
Never let a real `api_key` / `token` / `password` appear in docs, screenshots, logs, issues, or commits.
`dc3_model_provider.api_key` lives in the database, access-constrained by tenant isolation. The env fallback key should
likewise be injected only through local files like `dc3/env/dev.env` and never committed to the repository.
:::

## Pre-use checklist

Before enabling the Agentic Center, verify these prerequisites — otherwise the model may answer fluently while the data
underneath is wrong:

1. The Agentic Center is started and reachable through the gateway on port `8000`.
2. The Auth Center, Manager Center, and Data Center base capabilities are healthy — the tools ultimately call their
   interfaces.
3. At least one device and point are producing data; otherwise value-query tools return empty.
4. The model provider (DB or env fallback) is reachable, and the API key isn't written into docs or logs.
5. Tool calling is opened only to trusted users and well-defined business scenarios. Turn it off with
   `AGENTIC_TOOL_CALLING_ENABLED=false` when you don't need it.

## Further reading

- [AI Agent / MCP Integration](./mcp) — connect external AI agents to platform tools securely via OAuth 2.1 + MCP
- [Core Concepts](../introduction/concepts) — the object model of driver / profile / device / point / point value, so
  you understand what the tools are querying
- [Command Plane](../architecture/command-plane) — how the read/write commands issued by tools flow, and why a failed
  write echoes nothing
