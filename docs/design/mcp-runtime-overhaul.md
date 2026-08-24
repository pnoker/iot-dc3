# Design: MCP Runtime Overhaul — Cohesive Authorization Contract

|                |                                                                                                                            |
|----------------|----------------------------------------------------------------------------------------------------------------------------|
| **Status**     | Proposed — not yet implemented                                                                                              |
| **Date**       | 2026-08-18                                                                                                                  |
| **Scope**      | MCP runtime plane: `mcp_runtime.proto`, `McpRuntimeFacade`, `McpGatewayController`, auth-side `OAuthMcpRuntimeServiceImpl` + `McpRuntimeServer` |
| **Target**     | One cohesive gateway→auth round-trip per MCP method; reactive, non-blocking; real input schemas; async audit               |
| **Related**    | [`mq-abstraction.md`](./mq-abstraction.md) — broker port used for the async audit channel, if adopted                      |
| **Discussion** | open for review before implementation starts                                                                                |

## 1. Summary

The MCP runtime that sits behind `McpGatewayController` (the gateway's MCP JSON-RPC resource
server) currently reaches the auth center through a **synchronous facade with five fine-grained
RPCs**, which forces the gateway to behave like a remote database client: it first calls
`Introspect` to recover the token context, then spreads `tenantId` / `principalId` /
`connectionId` across `ResolveTool` and `AuthorizeToolCall`, and finally calls `Audit`.
One `tools/call` therefore costs **4 gRPC round-trips, ~8–10 DB queries, one blocking-thread hop
per call**, and — as a correctness gap — returns tools whose `inputSchema` is a static empty
envelope instead of the real JSON schema.

This document proposes a **breaking, non-compatible overhaul** of that runtime contract:

- **3 RPCs instead of 5**: `ListTools`, `CallTool`, `Audit`.
- **The bearer token is the only input** for `ListTools` / `CallTool`; the auth center is the
  single place that understands a token, so it does verification + visibility + authorization in
  one decision inside the same call.
- `CallTool` returns the **decision, the resolved tool, and the principal context** together, so
  the gateway no longer needs a separate introspection round-trip before forwarding to a backend.
- The facade becomes **reactive** (`Mono`/future stubs); the `blocking() + boundedElastic` hop
  disappears.
- `inputSchema` is carried end-to-end so external agents see real tool parameters.
- `Audit` is decoupled from the call path (fire-and-forget or broker event).

## 2. Background — how the MCP runtime works today

Verified against the tree on 2026-08-18. File references are exact.

### 2.1 The contract today

`dc3-api/dc3-api-auth/src/main/protobuf/api/common/auth/mcp_runtime.proto` declares five RPCs:

| RPC | Purpose | Called per tools/call? |
|-----|---------|------------------------|
| `Introspect` | Validate the OAuth bearer token, return tenant/principal/connection context | yes (every request) |
| `ListTools` | List tools visible to the connection | no (tools/list only) |
| `ResolveTool` | Resolve one tool to its backend invocation metadata | yes |
| `AuthorizeToolCall` | Enforce high-risk confirmation + idempotency, return a decision | yes |
| `Audit` | Store one audit record | yes |

`McpRuntimeFacade` (`dc3-common-facade-api`) mirrors this as **synchronous** methods:
`introspect(String)`, `listTools(...)`, `resolveTool(...)`, `authorizeToolCall(...)`,
`audit(...)`. Its gRPC implementation (`McpRuntimeGrpcFacade`) uses an injected blocking stub;
`GrpcFacadeSupport.call` only adds a deadline. Connection reuse is fine — the stub is a shared
bean — the problem is *what* is called and *how often*.

### 2.2 One `tools/call`, step by step

The gateway path is `McpGatewayController.mcp(...)` → `dispatch(...)` →
`McpGatewayClient.callTool(...)`. Each hop and its verified cost:

| Step | Where | Cost |
|------|-------|------|
| ① introspect | `OAuthMcpRuntimeServiceImpl.introspect` | 1× local JWT verify (`verifyWith(publicKey())`) + **4 DB queries**: `selectAuthorizationByAccessTokenJti`, `selectConnectionById`, `principalManager.getById`, `tenantMembershipService.isTenantMember` |
| ② resolveTool | `resolveVisibleTool` | `selectVisibleToolByName` (1) + `updateConnectionLastUsed` (1) |
| ③ authorizeToolCall | `authorizeToolCall` → re-runs `resolveVisibleTool` | **repeats** `selectVisibleToolByName` + `updateConnectionLastUsed` (2 more); only HIGH risk then queries the confirmation ticket |
| ④ invokeBackend | gateway `WebClient` | 1 HTTP forward (the actual business call) |
| ⑤ audit | `audit` → `insert` | 1 DB write (post-hoc, failure swallowed) |

Plus **4 gateway→auth gRPC round-trips** (`Introspect`, `ResolveTool`, `AuthorizeToolCall`,
`Audit`), each wrapped in `blocking(...)` which subscribes on `boundedElastic` — one
blocking thread per in-flight call.

### 2.3 Three redundancies and one correctness gap

1. **Introspection is fully re-run on every request, with no cache.** The four DB queries cannot be
   dropped (OAuth introspection must check revocation, principal enablement, and tenant membership),
   but the same token pays them again on every tool call in an agent loop.
2. **Resolve and authorize duplicate the visibility query.** `authorizeToolCall` explicitly
   re-runs `resolveVisibleTool` ("Re-run the full visibility/whitelist/scope check"). The result
   is identical; `selectVisibleToolByName` and `updateConnectionLastUsed` each execute twice.
3. **`updateConnectionLastUsed` is non-critical telemetry executed synchronously — twice.**
4. **Correctness gap: `inputSchema` is lost.** `GrpcMcpToolDefinitionDTO` has **no**
   `input_schema` field (the proto comment says "excluding the static JSON schema envelope"), and
   `McpRuntimeGrpcFacade.toDTO` hard-codes `DEFAULT_INPUT_SCHEMA`. An external agent therefore
   receives an empty schema envelope for every tool instead of the real parameters that
   `McpOpenApiAggregator` already computed and `OAuthMcpRuntimeServiceImpl.inputSchemaOf` already
   reads from `tool_ext`.

## 3. Goals / Non-Goals

**Goals**

- Reduce one `tools/call` to **2 gateway→auth round-trips** (`CallTool` + async `Audit`).
- Make the token the single input to `ListTools` / `CallTool`; auth is the only token authority.
- Eliminate the duplicate visibility query (`selectVisibleToolByName` once, not twice).
- Make the runtime **reactive end-to-end**; remove `blocking()` + `boundedElastic`.
- Carry the **real `inputSchema`** through `tools/list` so external agents see true parameters.
- Decouple audit from the call path.
- Drop `Introspect`, `ResolveTool`, `AuthorizeToolCall` as gateway-facing RPCs — **no
  backward compatibility**, no dual-mode shim.

**Non-Goals**

- No change to the MCP wire protocol the client sees (still JSON-RPC 2.0 + OAuth bearer).
- No change to the management plane (`McpManagementController`) or the frontend MCP settings pages.
- No change to `McpOpenApiAggregator` / `dc3_api` tool-catalog generation.
- No change to how backend services authenticate downstream principal headers (HMAC + JSON header
  stay as-is).

## 4. Design principles

1. **Decision, not lookup.** The gateway asks "may this token call this tool, and where does it go?"
   in one request; it does not assemble the answer from three lookups.
2. **Auth owns the token.** `ListTools` and `CallTool` take the raw token; auth parses, verifies,
   and resolves context internally. The gateway never reconstructs tenant/principal context from
   claims.
3. **One authoritative visibility check.** Visibility + risk + confirmation + idempotency are
   decided in one place, once, inside `CallTool`.
4. **Reactive throughout.** Facade returns `Mono`; gRPC uses future/async stubs; gateway stays on
   the WebFlux event loop.
5. **Telemetry is off-path.** Audit is fire-and-forget (or a broker event); it can never delay or
   fail a call.

## 5. Target contract

### 5.1 Protobuf

`mcp_runtime.proto` shrinks to three RPCs. Message names keep the existing `Grpc` convention.

```proto
service McpRuntimeApi {
  // tools/list: auth verifies the token and returns the visible tool list.
  rpc ListTools (GrpcMcpListToolsRequest) returns (GrpcRMcpToolListDTO);

  // tools/call: auth verifies the token, enforces visibility + risk + idempotency,
  // and returns the decision, the resolved tool, and the principal context.
  rpc CallTool (GrpcMcpCallToolRequest) returns (GrpcRMcpCallToolDTO);

  // Audit: decoupled from the call path (fire-and-forget or broker event).
  rpc Audit (GrpcMcpAuditCommand) returns (GrpcRMcpBoolean);
}
```

Removed: `Introspect`, `ResolveTool`, `AuthorizeToolCall` and their request/response messages
(`GrpcMcpIntrospectRequest`, `GrpcRMcpIntrospectDTO`, `GrpcMcpIntrospectDTO`,
`GrpcMcpToolListRequest`, `GrpcMcpToolResolveRequest`, `GrpcRMcpToolResolveDTO`,
`GrpcMcpToolResolveDTO`, `GrpcMcpToolAuthorizeRequest`, `GrpcRMcpToolAuthorizeDTO`). The
shared enums (`GrpcMcpRiskLevel`, `GrpcMcpDecision`, `GrpcMcpPrincipalType`,
`GrpcMcpAuditStatus`) stay.

New / changed messages:

```proto
message GrpcMcpListToolsRequest {
  string token = 1;              // bearer token; auth resolves context internally
}

message GrpcMcpCallToolRequest {
  string token = 1;
  string tool_name = 2;
  string argument_digest = 3;    // sha256-base64url of arguments (idempotency + audit)
  string confirm_id = 4;         // empty on first attempt; present on confirm
  string idempotency_key = 5;
  string client_name = 6;        // for audit; read from MCP client headers
  string client_version = 7;
  string remote_ip = 8;
}

// tools/list item: now carries the real input schema.
message GrpcMcpToolDefinitionDTO {
  string name = 1;
  string title = 2;
  string description = 3;
  string input_schema = 4;       // NEW: JSON Schema, serialized from tool_ext
  GrpcMcpToolAnnotationsDTO annotations = 5;
  GrpcMcpToolMetadataDTO meta = 6;
}

// Principal context the gateway needs to forward downstream headers.
// Replaces the gateway's dependence on GrpcMcpIntrospectDTO.
message GrpcMcpPrincipalContext {
  int64 tenant_id = 1;
  int64 principal_id = 2;
  GrpcMcpPrincipalType principal_type = 3;
  string principal_name = 4;
  string display_name = 5;
  string client_id = 6;
  int64 connection_id = 7;
}

// tools/call result: decision + tool + principal context in one payload.
message GrpcMcpCallToolDTO {
  GrpcMcpDecision decision = 1;
  string confirm_id = 2;
  string message = 3;
  GrpcMcpRiskLevel risk_level = 4;
  GrpcMcpToolResolveDTO tool = 5;        // keeps service_name/api_path/http_method
  GrpcMcpPrincipalContext principal = 6;
}
```

`GrpcMcpToolResolveDTO` is retained (it is still the `tool` sub-message inside
`GrpcMcpCallToolDTO`) and gains `input_schema` so the gateway can forward a tool's schema to a
backend when a future backend needs it.

### 5.2 Facade

```java
public interface McpRuntimeFacade {
    Mono<McpToolListResponseDTO> listTools(String token);
    Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request);
    Mono<Void> audit(McpAuditCommandDTO command);
}
```

- `McpRuntimeGrpcFacade` switches from `McpRuntimeApiBlockingStub` to the async
  `McpRuntimeApiStub` (future→`Mono`) and drops `GrpcFacadeSupport.call` in favor of reactive
  error translation.
- New DTOs `McpCallToolRequestDTO` / `McpCallToolResponseDTO` mirror the proto; the response
  carries `McpPrincipalContextDTO` in place of the old `McpIntrospectResponseDTO`.

### 5.3 Auth service

`OAuthMcpRuntimeServiceImpl` replaces `introspect` + `resolveVisibleTool` +
`authorizeToolCall` with two cohesive operations:

- `listTools(token)`: `parseAccessToken` → active-authorization check → connection/principal/
  membership checks → `listVisibleTools` → `toolToMcp` (now with real `inputSchema`).
- `callTool(request)`: same token verification, then **one** `selectVisibleToolByName` +
  visibility/scope/risk decision, then the confirmation/idempotency gate (HIGH risk only), returning
  decision + resolved tool + principal context.

`McpRuntimeServer` mirrors this: three gRPC methods, `toGrpc` builders for the new messages, and
`inputSchema` serialization out of `tool_ext` (reusing the existing `inputSchemaOf` logic).

### 5.4 Gateway

`McpGatewayController` dispatch simplifies to:

- `tools/list` → `mcpRuntimeFacade.listTools(token)` → JSON-RPC result.
- `tools/call` → `mcpRuntimeFacade.callTool(request(token, toolName, digest, confirmId,
  idempotencyKey, client meta))`; on `AUTHORIZED`, forward to the backend using
  `tool.serviceName/apiPath/httpMethod` and build `X_AUTH_PRINCIPAL` from `principal`; on
  `CONFIRM_REQUIRED`, return the confirm prompt; on `REJECTED`, return the denial.
- `audit` → fire-and-forget (no longer awaited before returning).

The `blocking(...)` helper, the `toLong(context.getTenantId())` scattering, and the standalone
introspection call all disappear.

## 6. Request flows

### 6.1 Before (today, one tools/call)

```text
client → gateway ─ Introspect ──────────────→ auth  (JWT + 4 DB)
                 ─ ResolveTool ─────────────→ auth  (visible tool + last_used)
                 ─ AuthorizeToolCall ───────→ auth  (visible tool + last_used again, ± confirm)
                 ─ HTTP ────────────────────→ backend
                 ─ Audit ───────────────────→ auth  (insert)
        gateway ←─ decision / tool / context ── (assembled from 3 responses)
```

### 6.2 After (target, one tools/call)

```text
client → gateway ─ CallTool(token, tool, digest, confirm, key, client meta) ─→ auth
                                                                   (JWT + authz + visibility + risk
                                                                    + idempotency, one pass)
                 ─ HTTP (built from returned tool + principal) ──────────→ backend
                 ─ Audit ─ (fire-and-forget / broker event) ─────────────→ auth
        gateway ←─ decision + tool + principal ── (one response)
```

## 7. Migration plan

Breaking, no compatibility shim. Each phase must leave the tree compiling and tests green.

| Phase | Change | Files |
|-------|--------|-------|
| 1 | Rewrite `mcp_runtime.proto` (3 RPCs, new messages, `input_schema`), regenerate stubs | `dc3-api/dc3-api-auth/.../mcp_runtime.proto` |
| 2 | Reactive facade + gRPC impl (`Mono`, async stub) | `McpRuntimeFacade`, `McpRuntimeGrpcFacade`, new request/response DTOs |
| 3 | Auth service + server: merge introspect/resolve/authorize into `listTools`/`callTool`; real `inputSchema` | `OAuthMcpRuntimeServiceImpl`, `McpRuntimeServer` |
| 4 | Gateway: token-direct dispatch, drop `blocking()`, async audit | `McpGatewayController` |
| 5 | Rewrite affected tests | `McpRuntimeServerTest`, `McpGatewayControllerTest`, `OAuthMcpRuntimeServiceImplTest`, facade tests |

Frontend is **untouched** (the MCP settings pages use `McpManagementController`, not this
gateway runtime).

## 8. Verification

- `mvn -s .mvn/settings.xml -q -DskipTests compile` after each phase.
- `mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-auth -am` and
  `-pl dc3-common/dc3-common-gateway -am` for the rewritten tests.
- Contract assertions to preserve:
  - one `tools/call` = one `CallTool` RPC + one async `Audit` (assert via test doubles),
  - `selectVisibleToolByName` invoked exactly once per call,
  - `tools/list` returns non-default `inputSchema` for a fixture tool,
  - HIGH-risk still issues `CONFIRM_REQUIRED` + confirmId, idempotency key still deduplicates.
- `pnpm check` in `dc3-web` to confirm no frontend regression.

## 9. Alternatives considered

1. **Cache introspection instead of reshaping the contract** (keeps 5 RPCs). Rejected: it papers
   over the synchronous facade, the duplicate visibility query, and the missing schema; the
   round-trip count and the gateway's context-scattering remain.
2. **Local JWT verification in the gateway + short-TTL jti deny-list.** Removes the introspection
   round-trip entirely, but moves token authority into the gateway and adds a revocation-propagation
   channel. Rejected as the primary design because it blurs the auth boundary; it can be layered
   later as a pure optimization on top of the 3-RPC contract.
3. **Keep `ResolveTool` and only merge the rest.** Rejected: resolve is a sub-step of the call
   decision, not an independent operation; returning the tool inside `CallTool` is strictly
   simpler.

## 10. Open questions

1. **Audit transport.** Fire-and-forget `Mono` (smaller change) vs a RabbitMQ event through the
   MQ abstraction (fully off-path, but depends on broker availability). Leaning broker event, to be
   confirmed.
2. **`inputSchema` in `GrpcMcpToolResolveDTO`.** Should the forwarded backend ever need the
   schema, or is schema only a `tools/list` concern? Current proposal adds it defensively.
3. **Token verification cost.** With 5→3 RPCs, the 4 introspection DB queries still run once per
   `CallTool`. A short-TTL `jti → context` cache inside auth (invalidated on revoke/disable) is
   a follow-up, not part of this contract overhaul.

## 11. Appendix — current call-site inventory

Verified 2026-08-18.

| Artifact | Location | Notes |
|----------|----------|-------|
| Proto | `dc3-api/dc3-api-auth/src/main/protobuf/api/common/auth/mcp_runtime.proto` | 5 RPCs, 260 lines |
| Facade | `dc3-common-facade-api/.../McpRuntimeFacade.java` | 5 synchronous methods |
| gRPC impl | `dc3-common-facade-grpc/.../McpRuntimeGrpcFacade.java` | blocking stub + `GrpcFacadeSupport` |
| Gateway | `dc3-common-gateway/.../McpGatewayController.java` | JSON-RPC dispatch, `blocking()`+boundedElastic, `invokeBackend`, `audit` |
| Auth service | `dc3-common-auth/.../OAuthMcpRuntimeServiceImpl.java` | `introspect`, `resolveVisibleTool`, `authorizeToolCall` |
| gRPC server | `dc3-common-auth/.../McpRuntimeServer.java` | 5 server methods |
| Schema source | `dc3-common-auth/.../tool/McpOpenApiAggregator.java` | `inputSchema` computed but not carried over gRPC |
| Tests | `McpRuntimeServerTest`, `McpGatewayControllerTest`, `OAuthMcpRuntimeServiceImplTest` | rewrite in Phase 5 |
