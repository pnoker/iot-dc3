# Token Unification & MCP-First CLI (Design)

|                |                                                                                                                                                     |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**     | Phase 0 decided (Q1–Q4 resolved 2026-08-27) — §8 Q5 threshold pending; no implementation started                                     |
| **Date**       | 2026-08-27                                                                                                                                          |
| **Scope**      | Gateway authentication plane (`AuthenticGatewayFilter`, `FilterService`), auth center (`TokenServiceImpl`, `OAuthMcpRuntimeServiceImpl`), dc3-cli    |
| **Target**     | One permission truth source (RBAC), one verdict point (gateway), two interchangeable bearer tokens; CLI rebuilt around the MCP/OAuth credential model |
| **Related**    | [`mcp-runtime-overhaul.md`](./mcp-runtime-overhaul.md) · [`dc3-client-sdk.md`](./dc3-client-sdk.md) (its Phase 4 is superseded here — see §7)       |
| **Discussion** | Open questions in §8 require a decision before any code is written                                                                                  |

## 1. Problem

The platform currently runs **two parallel authentication systems** that cannot interoperate:

|                    | Login tokens (web / dc3-cli today)                     | MCP OAuth tokens                                            |
|--------------------|--------------------------------------------------------|-------------------------------------------------------------|
| Issued by          | `/api/v3/auth/token/generate` (salt + password)        | Dynamic client registration + `/oauth2/token`               |
| Algorithm          | HS256 shared key (`KeyUtil`)                           | RS256 key pair (`OAuthProperties.jwt`)                      |
| Claims             | subject+issuer bound to principalId & tenantId         | iss / aud / **scope claims**                                |
| Verdict point      | `AuthenticGatewayFilter` on every `/api/v3/*` route    | `OAuthMcpRuntimeService` only behind `POST /mcp`            |
| Lifetime           | fixed 12 h (`TimeoutConstant.TOKEN_CACHE_TIMEOUT`)     | access 15 min + refresh 30 d with rotation & theft teardown |
| Permission model   | none in the token — full RBAC resolved server-side     | 4 preset scopes (`McpConstant.Scope`)                       |

Consequences: an MCP token cannot call `/api/v3/*`; a login token cannot call `/mcp`;
the same AI agent must hold two identities; the CLI carries a hand-rolled duplicate
of the login flow even though its stated audience ("AI-ready") matches MCP's model,
not the browser-session model.

## 2. First principles

A bearer token makes exactly three signed statements. Everything else is detail:

1. **Who** — a principal inside exactly one tenant (`tenantId` isolation is non-negotiable).
2. **What** — a bounded set of capabilities, expressed once, not twice.
3. **Until** — expiry plus a revocation story proportional to the capability's blast radius.

Design rule derived from this: do **not** unify by inventing new token types or new
permission stores. Unify each dimension onto the existing best owner, then converge
verification into a single verdict point:

| Dimension | Single source of truth | Why there |
|-----------|------------------------|-----------|
| Who       | `dc3_auth` principal / tenant model (already shared — service accounts already live in `dc3_local_credential`) | Tenant-safety logic exists once |
| What      | Existing RBAC (`dc3_resource.permissionCode` + role bindings); MCP scopes become a *projection* of it | Two parallel stores would drift within one release cycle |
| Until     | Expiry policy layered by risk class; refresh rotation (MCP side already correct) moves to the shared path | Time control should scale with blast radius, not with which door was used |

## 3. Core design — the gateway becomes the sole verifier

The load-bearing fact making this cheap: after verification, `AuthenticGatewayFilter`
compresses identity into an `X_AUTH_PRINCIPAL` header and **every downstream service
already trusts that header alone**. Convergence therefore happens entirely at the
gateway; downstream services change nothing.

```text
request → AuthenticGatewayFilter → TokenResolver chain (first success wins)
             ├─ PlatformTokenResolver   HS256 login ticket (unchanged behavior)
             └─ OAuthTokenResolver      RS256 ticket against JWKS from auth center
          ↓ both produce the SAME extended PrincipalHeader:
          { principalId, tenantId, authClass: LOGIN|SERVICE_ACCOUNT,
            riskCeiling: NONE|CALL|CALL_HIGH, scopes: [...] }
          ↓ mutate headers, sign (HmacAuthSigner), route as today
```

- Resolvers are ordered, pure, and fail-closed; unknown algorithms reject immediately.
- The JWKS is fetched from the auth center (`/oauth2/jwks`, already exposed) with a
  short cache and key-id pinning; no shared secret ever leaves the auth center.
- Reuse, do not fork, the claim-verification logic in `OAuthMcpRuntimeServiceImpl`
  (extract into a shared verifier consumed by both the gateway filter and the MCP path).

### 3.1 Scopes as a projection, not a second store

Today's four scopes stay as the external vocabulary, but they stop being an
independent configuration axis. On authorization, the server computes them from the
client principal's RBAC bindings:

```text
effectiveScopes(principal) =
    resources_read : has any read-permissionCode binding
    tools_list     : has any API the principal may see
    tools_call     : all bound APIs are LOW risk
    tools_call_high: ≥1 HIGH-risk API bound
```

The RS256 ticket signs the computed result; per-request enforcement keeps using the
existing riskLevel check in the tool path, later extended to `X_AUTH_PRINCIPAL`-based
API checks in Phase 2 (§6). Result: granting a CLI token and granting an MCP
connection draw from the same well and can never disagree.

### 3.2 Risk-layered lifetimes (answers the TTL question structurally)

| Capability class           | Access ticket | Renewal                                    |
|----------------------------|---------------|--------------------------------------------|
| read-only (`resources_read`, dashboards) | 1 h           | silent refresh                             |
| write (`tools_call`, manager mutations)  | 15 min        | refresh rotation (reuse current mechanism) |
| high-risk (`tools_call_high`)            | step-up only: minutes-lived ticket minted on confirmation of the specific call | no standing grant |

The platform-side 12 h constant moves to configuration in the same change
(some consumer — batch jobs — will eventually need longer); refresh-rotation theft
teardown semantics extend unchanged because both resolvers share the auth-center
session store. Per-client TTL overrides are explicitly out of scope for Phase 1.

## 4. The CLI, rebuilt MCP-first

Supersedes the CLI's current credential model (`credential-keychain/encrypted/env/prompt`
stores feeding salt/generate). Two options were evaluated:

### Option A — CLI speaks MCP JSON-RPC only (transport-level MCP-first)

Commands become thin projections over `tools/list` + `tools/call` against `POST /mcp`.
Discovery-driven (`dc3 device list` resolves to whichever tool exposes it).

- ✅ Works without §3 (gateway MCP route + RS256 already live).
- ❌ Tool catalog does not yet cover manager CRUD breadth (~40 covered endpoints vs
  ~346); pagination/bulk ergonomics of JSON-RPC are poor for shell scripting;
  streaming/table formatting fights the envelope.

### Option B (chosen direction) — OAuth credential mechanism, dual transport

CLI adopts the **MCP authorization mechanism wholesale**: dynamic client registration
(`POST /oauth2/register`), authorization, 15-min access tickets with refresh rotation,
scope grants, revocation. Transport stays pluggable:

```text
dc3 auth login --oauth        # DCR + device-code flow; prints granted scopes
dc3 device list               # Bearer <RS256> on /api/v3 (needs §3 Phase 1)
dc3 ask "…"                   # same token on POST /mcp (tools/call)
```

- ✅ One identity per agent everywhere; storage reduces to "access + refresh +
  client registration" in the OS keychain (the four legacy stores collapse to one);
  exit codes map cleanly (2 network / 3 auth → OAuth error taxonomy).
- ✅ Command layer stops guessing backend endpoints — gap analysis done 2026-08-27
  showed ~300 uncovered endpoints; under Option B coverage grows by adding backend
  tools/resources, not by rewriting CLI modules per domain.
- ⚠️ Requires §3 (an RS256 ticket must pass `/api/v3`). This dependency is why
  §3 ships first.

Option A remains available for machine agents that prefer native MCP; the CLI never
becomes a required wrapper around it.

## 5. Security considerations

- **Downstream trust is unchanged but gains risk data**: `X_AUTH_PRINCIPAL` gets
  extra fields; services must ignore what they don't understand (HMAC signature
  covers the whole payload, so spoofing surface is unchanged).
- **Key custody**: RS256 private key never leaves auth center (today's
  `ephemeralRsaKey` fallback must refuse to ship in that mode — fail-fast at boot).
- **Replay/step-up**: high-risk calls keep the agentic action confirm/reject loop as
  the second factor; step-up tickets bind to a specific confirmation record id.
- **Denial of fallback**: no `tenant IS NULL` shortcuts anywhere in the projection
  computation; service-account principals stay tenant-scoped as today.

## 6. Phasing

1. **Phase 0 (doc-only)** — this document; decisions on §8 questions.
2. **Phase 1** — gateway TokenResolver chain + shared RS256 verifier extraction +
  PrincipalHeader extension; platform TTL constant configurable. CLI Option-B auth
  lands behind a flag alongside the legacy flow.
3. **Phase 2** — scope-as-projection (RBAC computation at authorization); API-level
  enforcement reads scopes off `X_AUTH_PRINCIPAL`; risk-ladder TTLs; step-up.
4. **Phase 3** — CLI command surface regenerates from tool/resource catalogs;
  legacy salt/generate flow deprecated (not removed until web also offers OAuth).

Each phase is independently shippable; nothing before Phase 3 requires touching the
per-domain CLI command files again.

## 7. Relationship to prior docs

- [`mcp-runtime-overhaul.md`](./mcp-runtime-overhaul.md): complementary — that doc
  overhauls the gateway→auth RPC contract inside the MCP path; this doc reuses its
  resulting single-verifier shape at the HTTP gateway edge. No conflicts.
- [`dc3-client-sdk.md`](./dc3-client-sdk.md): **Phase 4 superseded**. The SDK stays
  valuable for web/app and for REST typing, but CLI auth migrates to the OAuth
  mechanism here instead of SDK-wrapped salt/generate/cancel. SDK `TokenStore`
  contract should target "RS256 access/refresh pairs" when CLI (§4B) lands.

## 8. Open questions

**Decisions taken 2026-08-27 (Q1–Q4):**

1. **Scope vocabulary retained.** The four scopes stay as the external contract; internally they are computed as a
  projection of RBAC bindings (§3.1 unchanged). Rationale: pre-flight capability discovery for AI clients and MCP
   ecosystem compatibility outweigh the extra projection logic.
2. **Step-up channels: all three, layered.** Default channel is the web approval surface (agentic action
   confirm/reject, one audit chain for humans and agents); CLI TTY second-prompt binding a step-up ticket is supported
   for single-operator flows; `--approve-window <duration>` exists as an explicit, audit-hardened escape hatch for
   batch scripts.
3. **Cached-JWKS local verification at the gateway.** No per-request introspection hop; revocation lag stays bounded
   by the ≤15 min access-ticket lifetime. The auth center's availability no longer gates every `/api/v3` request.
4. **Web keeps cookie sessions.** Browser auth remains httpOnly + CSRF; only the shared RS256 verifier and RBAC truth
   source are reused, not the flow. Re-evaluation is allowed if dual-track maintenance ever becomes a real cost.

5. **Phase-3 regeneration threshold — proposed, still open:** manager-domain (device/driver/profile/point) tool
   coverage ≥80%. Percentage-based gating may be swapped for the P0 capability list (analytics nine tools, alert deep
   analysis, agentic session plane) once that backlog lands; final call pending.

> Status after this revision: **Phase 0 complete. Phase 1 (gateway TokenResolver chain + configurable platform TTL +
> OAuth-flagged CLI login) is cleared for implementation planning.**
