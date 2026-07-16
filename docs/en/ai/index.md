---
title: AI
---

<script setup>
import AiIndexDiagram from '../../.vitepress/theme/components/AiIndexDiagram.vue'
</script>


# AI

IoT DC3 plugs large language models into operations, so a model can do more than read data — it can act on devices. This
section covers two ways an LLM drives those actions; the difference is who initiates and how it's constrained.

- **Agentic Center** — a built-in, conversational AI operations assistant. Built on Spring AI with 10 `@Tool`s, it lets
  the LLM query devices, read and write points, and run commands through Tool Calling. It speaks the OpenAI API, so you
  can point GPT, Claude, DeepSeek, Qwen, and friends at it. Good when you want a UI-driven, multi-turn assistant.
- **MCP** — exposes platform tools safely to external AI agents. The gateway serves a JSON-RPC 2.0 MCP resource server
  at `POST /mcp`; the tool catalog is aggregated from the four centers' OpenAPI (330+ tools) and gated by OAuth 2.1, a
  per-connection tool whitelist, and risk tiers. Good when you run your own agent and want the model to pick which tool
  to call.

> You are here: you've already [onboarded a device](../operation/device-onboarding) and want the model to query,
> analyze, even issue commands. Next, pick [Agentic Center](./agentic) or [AI Agent / MCP](./mcp). Prefer scripts over
> AI?
> See [Automation (dc3 CLI)](../automation/cli).

## Two paths, one door

The two differ less in what they can do than in who initiates and how it's bounded. Either way, the platform has a
single HTTP entry — the gateway `dc3-gateway` (`8000`). The Agentic chat and the MCP tool calls both go through it,
where the gateway injects principal context and hands off to `dc3-center-auth` for **RBAC** and **tenant isolation**.

In other words: the AI gets no more privilege than the account behind it, and cross-tenant data stays invisible (you get
a 404, not the data). The README's "tenant-level isolation across database, cache, and API paths" and "JWT + Spring
Security + RBAC" apply equally to both.

<AiIndexDiagram lang="en" />

Auth differs, but the destination is the same — before any business service runs, the call clears the `@PreAuthorize`
permission point and the tenant boundary:

- **Agentic Center** acts under the logged-in user's session; Tool Calling still hits the platform's business APIs, so
  permissions follow the current user.
- **MCP** uses a short-lived OAuth 2.1 JWT (15 minutes by default). The gateway re-introspects on every call, checks the
  MCP connection, then runs the `tools/list` three-layer filter (RBAC ∩ connection whitelist ∩ risk policy) to decide
  which tools the agent can see and invoke.

## Further reading

- [Agentic Center](./agentic) — conversational AI operations, 10 built-in tools, session persistence, high-risk
  confirmation
- [AI Agent / MCP](./mcp) — OAuth 2.1 + MCP, exposing tools safely to external agents
- [Why Spring AI](./spring-ai-deep-dive) — architecture rationale, tool-calling mechanics, and roadmap
- [Automation (dc3 CLI)](../automation/cli) — drive the platform from the command line, no AI
- [Data Intelligence & AIoT](../foundations/aiot) — the big picture of IoT analytics meeting large models

