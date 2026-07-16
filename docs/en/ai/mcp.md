---
title: AI Agent / MCP Integration
---

<script setup>
import McpFilterFlowDiagram from '../../.vitepress/theme/components/McpFilterFlowDiagram.vue'
import McpOAuthSequenceDiagram from '../../.vitepress/theme/components/McpOAuthSequenceDiagram.vue'
import McpConfirmSequenceDiagram from '../../.vitepress/theme/components/McpConfirmSequenceDiagram.vue'
</script>


# AI Agent / MCP Integration

IoT DC3 turns the platform's entire HTTP surface into a single MCP (Model Context Protocol) tool catalog. An external AI
Agent authenticates through OAuth 2.1, then discovers and calls tools over the gateway's `/mcp` endpoint: read devices,
query point values, issue commands. This page covers how to get a token, how to call `/mcp`, why some tools don't show
up, and why HIGH-risk operations need a second confirmation.

> You're wiring IoT DC3 to an AI Agent. If you only want a chat box where people ask about data, see
> the [Agentic Center](./agentic). If you're driving the platform from scripts, see the [CLI Guide](../automation/cli).

## Why MCP, instead of calling HTTP directly

The naive approach is to hand-write every REST endpoint as a tool and feed it to the LLM. That breaks down fast: 300+
endpoints spread across four centers, permission and tenant checks scattered everywhere, and destructive operations
mixed in with read-only queries with no risk grading. MCP standardizes the layer. The platform **automatically** exports
its endpoints as a risk-annotated tool catalog. The Agent discovers and calls them through one JSON-RPC protocol, and
authentication, tenancy, permissions, and risk confirmation all flow through the gateway and auth center.

Three roles make up the pipeline. The **Auth Center (`dc3-center-auth`)** is the OAuth 2.1 authorization server — it
issues tokens, performs introspection, and aggregates the tool catalog. The **Gateway (`dc3-gateway`)** is the MCP
Resource Server: it hosts `POST /mcp`, re-checks permissions on every call, and forwards signed requests to the backend.
The backend's **Manager / Data / Agentic centers** are where business logic actually runs. The Agent only ever talks to
the first two.

## Where the tool catalog comes from: automatic aggregation, stable tool_id

The tool catalog is generated, not hand-written. The auth center's `McpOpenApiAggregator` pulls the OpenAPI specs of the
four centers — auth / manager / data / agentic — at runtime, joins them with `dc3_api` (`api_code` / `api_name`) and
`dc3_resource` (`resource_code` / `permission_code`), creates one tool record per endpoint, and stores them in
`dc3_mcp_tool_catalog`. The scale is roughly **330+ tools**, generated from **330+ OpenAPI operations** across the four
centers.

Each tool has a **stable `tool_id`** (equal to `dc3_api.api_code`), formatted as
`{service_name}:{HTTP_METHOD}:{api_path}`, where `service_name` is the full service name `dc3-center-<x>` (from
`spring.application.name`):

```text
dc3-center-manager:POST:/device/add
dc3-center-data:POST:/point_command/write
dc3-center-data:POST:/point_value/latest
```

A tool's **risk level** is set by hand, one endpoint at a time — never inferred from the verb. Every endpoint declares
`riskLevel` (`LOW` / `MEDIUM` / `HIGH`) in its `@Extension(name = "x-dc3-ai")` annotation. The resource registrar
enforces during scanning that the annotation exists and `riskLevel` is valid, and reports a defect if it's missing. The
aggregator reads `riskLevel` from the annotation verbatim and only falls back to a conservative `HIGH` when the
annotation is absent. So `POST /point_command/write` is marked `HIGH` (`destructive=true`) by hand, and never downgraded
to `MEDIUM` just because it's a write.

`read_only_hint` is derived from the HTTP method (`GET` → 1, `POST` → 0). The aggregator writes these annotations —
`destructive_hint`, `idempotent_hint`, `open_world_hint` — into `dc3_mcp_tool_catalog`, sourced from the `x-dc3-ai`
OpenAPI extension on each endpoint (details at the end of this page).

::: info How the catalog refreshes: a manual endpoint only
The only way to refresh the tool catalog is an HTTP endpoint an administrator calls by hand (
`McpManagementController.refreshToolCatalog` → `OAuthMcpRuntimeServiceImpl.refreshToolCatalog`), which re-aggregates and
persists. **There is no scheduled refresh and no event-driven refresh** — no `@Scheduled` task and no post-commit
trigger such as `McpToolCatalogChangedEvent`. After adding a new endpoint, an administrator has to trigger a refresh
once before the catalog updates.
:::

## Access control: four gates decide whether a tool is visible and callable

Being in the catalog doesn't mean the Agent can use it. Whether a tool is **visible** to a given connection, and whether
it's **callable**, depends on OAuth verification first (to get the principal and scope), then on the intersection of
RBAC, the connection allowlist, and the risk policy.

<McpFilterFlowDiagram lang="en" />

`tools/list` returns the intersection of three sets: **the principal's permission codes ∩ this MCP connection's
allowlist ∩ the risk policy**. `tools/call` adds the OAuth scope and a per-call risk confirmation. So even if
`dc3-center-data:POST:/point_value/latest` is in the catalog, the Agent can't reach it when the connection's allowlist (
`dc3_mcp_connection_tool`) hasn't permitted it, the token lacks the `mcp:tools:call` scope, or the principal is missing
the matching query permission.

::: warning HIGH risk is invisible by default
HIGH-risk tools (the various `delete` operations) are hidden by default in `tools/list` and only appear when explicitly
enabled. Calling them requires the `mcp:tools:call:high` scope on top of that, plus the two-phase confirmation below.
This conservative default keeps the Agent from deleting things by accident.
:::

## OAuth 2.1 authorization server: how to obtain a token

The auth center runs a hand-written OAuth 2.1 authorization server (RS256 JWT). These are its HTTP endpoints — all
hosted by the auth center and exposed through the gateway:

| Endpoint                                  | Method        | Purpose                                                                     |
|-------------------------------------------|---------------|-----------------------------------------------------------------------------|
| `/.well-known/oauth-authorization-server` | `GET`         | Authorization server metadata discovery                                     |
| `/.well-known/oauth-protected-resource`   | `GET`         | Protected resource metadata (RFC 9728, gateway side)                        |
| `/oauth2/authorize`                       | `GET`         | Authorization code + PKCE (user login + consent + MCP connection selection) |
| `/oauth2/token`                           | `POST` (form) | Exchange for access_token / refresh_token                                   |
| `/oauth2/jwks`                            | `GET`         | Public key set (RS256, for signature verification)                          |
| `/oauth2/revoke`                          | `POST` (form) | Revoke a token (with replay detection)                                      |
| `/oauth2/register`                        | `POST` (JSON) | Dynamic client registration (admin-restricted)                              |

Token introspection (`introspect`) is **not exposed as an HTTP endpoint**. It's an internal gRPC interface the gateway
uses, as the Resource Server, to validate the Bearer token.

**Security baseline** (all implemented): public clients are **forced to use PKCE S256**; `redirect_uri` is matched
exactly, no wildcards; refresh tokens rotate (RFC 9700 §6.3, with replay detection via `previous_refresh_token_hash`);
client secrets are stored as hashes only, never in plaintext.

**Token types and lifetimes**: the access_token is a short-lived JWT (default 15 minutes) carrying
`iss/aud/exp/nbf/sub=principal_id/principal_type/scope/tenant_id/mcp_connection_id`; the refresh_token rotates, default
30 days; the authorization_code is single-use within 5 minutes and PKCE-bound; client_credentials runs as a
SERVICE_ACCOUNT with no refresh.

::: danger OAuth 2.1 only, no long-lived tokens
The platform's MCP access **supports OAuth 2.1 only**. There is no PAT (Personal Access Token) and no long-lived static
token such as `dc3mcp_*`. Every call uses a short-lived access_token plus a rotating refresh_token. Don't try to
hard-code a "permanent MCP key" in a script — it doesn't exist.
:::

## A complete call: from obtaining a token to getting a result

The sequence below shows the whole path: the Agent gets a token, calls `/mcp`, and the gateway introspects and forwards
the signed request.

<McpOAuthSequenceDiagram lang="en" />

Note the gateway-to-backend hop. The gateway uses `McpGatewayClient.invokeBackend()` to go straight through an internal
WebClient (**bypassing** the gateway's own routing), building `X-Auth-Principal` and applying an HMAC signature. The
backend's `GatewayJwtConverter` verifies the signature, restores the principal, and hands off to `@PreAuthorize` for the
permission decision. The HMAC key `AUTH_HMAC_SECRET` fails fast in `pre/pro` if it's empty or equal to the default —
see [Auth · Tenancy · RBAC](../architecture/auth-rbac).

### JSON-RPC methods of `/mcp`

`POST /mcp` is JSON-RPC 2.0 over Streamable HTTP, handled by the gateway's `McpGatewayController`. Supported methods:
`initialize`, `notifications/initialized`, `ping`, `tools/list`, `tools/call`.

A `tools/list` request:

::: code-group

```bash [curl]
curl -X POST http://localhost:8000/mcp \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

```json [Response shape (example)]
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "data_point_value_latest",
        "description": "Query the latest point value of a device",
        "inputSchema": { "type": "object", "properties": { "deviceId": {"type":"string"} } }
      }
    ]
  }
}
```

:::

A `tools/call` (a low-risk query tool, with example argument values):

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "data_point_value_latest",
    "arguments": { "deviceId": "1839...", "pointId": "1840...", "current": 1, "size": 10 }
  }
}
```

On the way back, the gateway wraps the backend's `R<T>` into an MCP `CallToolResult`. For the real endpoints and fields
behind a tool, see the [Agentic Center](./agentic) and each center's OpenAPI.

## HIGH risk: two-phase confirmation

For HIGH-risk tools (deletions and the like), the platform requires two-phase confirmation, so the Agent can't fire an
irreversible operation in a single reasoning step.

Phase one: the Agent calls `tools/call` without a valid confirmation. The server doesn't execute — it returns
`CONFIRM_REQUIRED` with a `confirmId` (UUID), default TTL `PT5M`.

Phase two: the Agent calls again with the `confirmId` + `idempotency_key`. The server checks that it isn't expired, the
`parameter_digest` matches the first call, the principal / connection / tool are unchanged, and it's consumed exactly
once. `status=PENDING` is the SQL-layer concurrency guard, so a replayed `confirmId` loses the race.

<McpConfirmSequenceDiagram lang="en" />

The confirmation ticket is stored in `dc3_mcp_tool_confirmation` (`confirm_id`, `tool_id`, `parameter_digest`,
`idempotency_key`, `status` PENDING/CONSUMED/EXPIRED, `ttl_expires`), with the TTL set by `dc3.mcp.confirm-ttl` (default
`PT5M`). Every HIGH-risk call is written to `dc3_mcp_audit_log` (`confirm_id`, `idempotency_key`, `argument_digest`,
`risk_level`, `duration_ms`, `remote_ip`, and so on).

## Constraints and boundaries (honestly labeled)

::: info MCP resources / prompts not implemented
`/mcp` implements the tool methods only (`tools/list` / `tools/call` plus `initialize` / `ping` /
`notifications/initialized`). The MCP protocol's `resources/*` and `prompts/*` capabilities are **not yet implemented
** (planned). The matching `mcp:resources:read` scope is reserved but not enabled.
:::

::: info `tools/list_changed` event push not implemented
When the tool catalog changes, the MCP `tools/list_changed` notification is **not pushed** (the design is planned; the
RabbitMQ push isn't built). Catalog refresh runs through the manual endpoint described above. Clients should *
*re-pull `tools/list` on their own schedule** and not assume the server pushes changes.
:::

Tool invocation is HTTP end to end (internal WebClient) — there is **no** gRPC tool-invocation channel, by design. The
`x-dc3-ai` metadata comes from real annotations on each endpoint:

```java
@Extension(name = "x-dc3-ai", properties = {
    @ExtensionProperty(name = "riskLevel",   value = "MEDIUM"),  // LOW/MEDIUM/HIGH
    @ExtensionProperty(name = "destructive", value = "false"),   // whether it damages data/config
    @ExtensionProperty(name = "idempotent",  value = "false"),   // whether it is safe to retry
    @ExtensionProperty(name = "openWorld",   value = "true")     // whether it reaches the external/physical world
})
```

## Further reading

- [Agentic Center](./agentic) — the platform's built-in conversation and tool invocation; see what actually executes
  behind an MCP tool
- [Auth · Tenancy · RBAC](../architecture/auth-rbac) — the principal model, HMAC signing, and permission resolution, end
  to end
- [CLI Guide](../automation/cli) — drive the platform directly with the `dc3` CLI, no AI involved
