---
title: IoT Security
---

<script setup>
import SecurityDiagram from '../../.vitepress/theme/components/SecurityDiagram.vue'
</script>


# IoT Security

IoT wires "things that can go online" to "a physical world you can act on," so a single gap threatens data and control
at once — reading a meter is a privacy problem, but tampering with a valve command is a safety incident. This chapter
walks through the threats and countermeasures of IoT security across four faces — device, communication, platform,
data — and shows how that thinking lands in IoT DC3 as authentication, tenant isolation, and transport encryption.

> You are here: you've finished the [four-layer reference architecture](./) and want to understand the security that
> cuts across all four. By the end you can draw a threat-to-countermeasure map and know which defenses DC3 places at
> each
> layer.

## What This Layer Is / Why It Exists

Security belongs to no single layer among perception, network, platform, and application — it is a **cross-cutting
concern** that runs through all four. The reason is blunt: attackers don't follow your layering, they probe for the
thinnest spot. Device firmware can be reflashed, links can be sniffed and replayed, platform APIs can be called without
authorization, and the database can be exfiltrated. If any one link falls, doing the others well counts for nothing. So
IoT security must be discussed across all four layers at once, with a default stance of "everything beyond the boundary
is untrusted."

IoT security is also harder than traditional IT, in three ways. First, **devices are resource-constrained**: field
sensors and gateways have little compute and memory and may run on batteries, so they can't afford heavy crypto or
frequent key rotation. Second, **devices are physically reachable**: they sit on the shop floor, in fields, on streets,
where an attacker can hold one, pry open the chip, read the Flash, hook the debug port — pure software defenses can't
stop a physical attack. Third, **scale and heterogeneity**: a platform may onboard dozens of protocols and thousands of
devices, so patching uniformly or rotating certificates everywhere is extremely costly, and any one aging device can
become the way into the whole network.

Because of these constraints, the goal of IoT security is not "absolute safety" but **defense in depth**: every layer
sets a gate, so no single breach loses the whole game. The four layers follow, then one diagram aligns threats with
countermeasures.

## Key Technologies and Trade-offs

### Device Security: Trust Starts in Hardware

The device is the physically reachable link, so defense must establish trust from the moment it boots. **Secure Boot**
has the bootloader verify firmware signatures stage by stage and refuse to run anything that fails — blocking "flash in
malicious firmware" at the root; the root of that trust chain is a root key fused into the chip, immutable. **Key
storage** decides whether the private key can be read off: leaving it in ordinary Flash is running naked, while the
right move is a Secure Element (SE) or Trusted Execution Environment (TEE) that keeps keys "usable but not readable." *
*Firmware update (OTA)** must verify the signature before writing and support rollback to a known-good version, or one
hijacked update can mass-compromise a whole fleet.

The trade-off is cost: chips with SE/TEE and Secure Boot cost more, and the OTA channel needs extra signing and staged
rollout. Severely constrained devices often manage only "signed updates plus software-layer key protection," reserving
the stronger hardware root of trust for critical nodes.

### Communication Security: Encryption, Authentication, Anti-Replay

The device-to-platform link is exposed on the network by nature, and three things must hold at once. **Encryption** uses
TLS (for TCP, e.g. MQTT over TLS, HTTPS) or DTLS (for UDP, e.g. CoAP) to protect the link against eavesdropping and
tampering. **Authentication** must be mutual: the server certificate stops a device from connecting to a fake platform (
man-in-the-middle), while the device proves who it is with a certificate or a pre-shared key (PSK), stopping device
spoofing. **Anti-replay** must defeat "record a valid message, replay it verbatim later" — using timestamps,
monotonically increasing sequence numbers, or a one-time nonce to invalidate stale messages.

The trade-off is constrained devices: a full TLS handshake's asymmetric math and certificate chain burden small devices,
so compromises appear — TLS-PSK, session resumption, lighter elliptic-curve algorithms. However light it gets, you
cannot drop "authentication plus anti-replay," or encryption merely encrypts the channel for the attacker too.

### Platform Security: Authentication, Authorization, Tenant Isolation, Audit

Device data converges on the platform, which makes the platform a high-value target. **Authentication** answers "who are
you" — a login yields a token that is verifiable and time-limited. **Authorization** answers "what may you do" — RBAC (
role-based access control) binds subject, role, and resource, holding to **least privilege** and **fail-closed** (no
permission found means deny, never default to allow). **Multi-tenant isolation** answers "which data may you touch" —
orthogonal to authorization: having "read device" permission does not mean reading another tenant's devices; weak
isolation lets one tenant see, or even operate, another tenant's field devices. **Audit** records "who did what, when,"
both for after-the-fact accountability and for real-time anomaly detection.

### Data Security: Privacy, Masking, Compliance

IoT data often ties back to people and the field — a smart meter's load curve reveals whether anyone is home, a location
trace is someone's whereabouts. **Privacy** demands minimal collection and purpose limitation: don't collect what you
shouldn't. **Masking** demands that sensitive fields be redacted or anonymized before display, export, or handing to a
third party (including feeding a large model). **Compliance** raises these to hard requirements: GDPR,
personal-information laws and the like constrain collection, storage, and cross-border transfer, and the cost of
violation far exceeds any one technical fault. Data security also covers encryption at rest (column/disk encryption) and
minimal retention (delete on expiry), so that "even if exfiltrated, what's taken is ciphertext or incomplete."

### Threat Model: Spread the Attack Surface Out

Security design needs targets, so align the four layers' countermeasures to concrete threats. The diagram below labels
five typical threats and their defenses along the data flow:

<SecurityDiagram lang="en" />

- **Device spoofing**: impersonating a legitimate device to report fake data or solicit commands — defeated by strong
  device-side authentication (one-device-one-secret, certificates).
- **Firmware tampering**: flashing in backdoored firmware — held off by secure boot plus signed OTA plus rollback.
- **Replay attack**: recording a valid message and replaying it verbatim — defeated by timestamps, sequence numbers, and
  nonces that invalidate stale messages.
- **Man-in-the-middle (MITM)**: sniffing or rewriting packets in the link — defeated by mutual authentication plus
  encryption, so a forged peer fails certificate validation.
- **DDoS**: flooding the entry with requests — mitigated by converging the entry to a single gateway, with rate limiting
  and a firewall.
- **Privilege escalation / cross-tenant**: a legitimate identity reaching beyond its scope — gated by RBAC's fail-closed
  and tenant isolation together.

## Engineering Notes

- **Untrusted by default, verify beyond the boundary**: don't assume "the internal network is safe." Backend services,
  even when not directly exposed, must verify the caller's identity in case someone bypasses the gateway and connects
  directly.
- **Fail closed, not open**: when an auth component hits a transient fault, prefer treating the request as "no
  permission" and rejecting it over "error means allow" — the latter turns a hiccup into a backdoor.
- **Tier keys, tighten by environment**: development can use weak defaults for convenience, but production must force
  strong random keys; better still, make the program **fail to start** when it finds a weak key in production, catching
  the problem before release.
- **Fewer entries is better**: converge the external surface to a single gateway; field-protocol ports (Modbus, raw
  TCP/UDP) must never face the public internet — most have no authentication, and exposing them is opening the door.
- **Encrypt transport by default**: the message bus, the broker, and the HTTP entry all run over TLS when traffic
  crosses a network; cleartext is acceptable only for local testing.
- **Auditable and traceable**: key operations (login, authorization changes, command dispatch) leave a trail, and logs
  must never contain cleartext keys or passwords.

## How It Lands in IoT DC3

DC3's security backbone centers on [Authentication · Tenancy · RBAC](../architecture/auth-rbac), backed by the
deployment baseline in [Security Policy](../community/security). It concretizes the four-layer thinking above into the
points below, and strictly separates "implemented" from "designed but not yet implemented."

### Platform Authentication: Two-Step Login and Tokens

DC3 faces the outside world through one entry only, the gateway `dc3-gateway`. Login is a **two-step handshake**,
mirroring the anti-replay idea from communication security:

1. `POST /api/v3/auth/token/salt`: send `tenant` and `name`, confirm the tenant exists, and get a random salt. The salt
   is **stateless** — the server neither stores it nor enforces an expiry; it is only checked together with the next
   login request. The "5 minutes" is merely a usage hint in the response text, honored client-side, not a
   server-enforced timeout today.
2. `POST /api/v3/auth/token/generate`: send `tenant`, `name`, `salt`, and the `password` hashed with the salt; on
   success, get an access token, **valid for 12 hours**.

The salt prevents a cleartext password or a fixed hash from being replayed on the wire. The minted JWT is **bound
to `principal_id` + `tenant_id`** (not the username); on logout the identity goes onto a Caffeine denylist, so an old
token, even with a valid signature, is rejected because it was issued before the logout point.

### Platform Trust Propagation: Gateway Signing + HMAC Pass-through

Center services each have their own HTTP port, unmapped externally by default. The risk: anyone who can reach a backend
port directly only needs to forge a "I am tenant A's admin" header, and a backend that trusts it unconditionally is
impersonated. DC3's answer separates **authentication** from **trust** — authentication happens once at the gateway,
trust travels as an HMAC-SHA256 signature:

- The gateway's `AuthenticGatewayFilter` verifies the three headers `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token`
  against the Auth Center, resolves the real principal, serializes it to `X-Auth-Principal`, signs it with the shared
  secret into `X-Auth-Sign`, and passes both downstream.
- The backend's `GatewayJwtConverter` recomputes the HMAC with the same secret, compares it to `X-Auth-Sign` in *
  *constant time**, and rejects on mismatch; it also rejects when `tenantId` or `principalId` is missing.
- When HMAC is disabled, the gateway **actively strips any inbound `X-Auth-Sign`**, so a downstream service can't be
  tricked by a fake signature the client brought along.

::: danger Production HMAC/key fail-fast (hard constraint)
In `pre` / `pro` environments, if `AUTH_HMAC_SECRET` is empty or still equals the default `io.github.pnoker.dc3`, the
service **fails to start** (throws `IllegalStateException`; the check is `HmacAuthConfig.isProtectedEnvironment()`).
This is intentional: better not to start than to run a production instance on development keys. In production, inject a
strong random value (e.g. `openssl rand -base64 48`) via an environment variable, never hardcode it or write it to logs.
`DC3_SECURITY_KEY` (the Auth Center's token signing key, default `dc3.security.key.2026.io.github.pnoker`) is subject to
a **"must exist" startup check only** — missing it fails startup, but there is no "must not equal the default weak
value" rejection; so it still must be actively replaced with a strong random value, since once it leaks an attacker can
forge login tokens.
:::

### Platform Authorization: RBAC, Fail-Closed

Once the signature is verified and the principal is in hand, RBAC decides "what may you do":
`principal → roles (per-tenant) → resource codes (global)`. Permission resolution carries a 5-minute short cache (keyed
by `(tenantId:principalId)`). The failure semantics matter most:

::: danger No permission found = deny
When permission loading hits a transient fault, `GatewayJwtConverter` still creates an "authenticated but
empty-authority" token, so any `@PreAuthorize` guard returns **403**. This is deliberate fail-closed — never dress up a
backend hiccup as an allow.
:::

### Multi-Tenant Isolation: Controller-Layer Checks

RBAC decides "may you do this kind of operation," tenant isolation decides "may you touch this row of data" — the two
are orthogonal. Isolation lives at the **controller layer**:

- After fetching an entity by ID, `BaseController.requireTenant()` compares the entity's `tenantId` with the caller's
  tenant; on mismatch (or non-existence) it throws `NotFoundException`, **returning 404 rather than 403** — deliberately
  using "does not exist" to avoid leaking "whether a cross-tenant resource exists." Batch queries go through
  `filterTenant()`, which drops entries not belonging to the tenant.

::: warning No database-layer tenant safety net
The current implementation has **no** MyBatis-Plus tenant row interceptor; isolation rests entirely on the
controller-layer `requireTenant` / `filterTenant`. When adding queries, you must apply the tenant check yourself — the
SQL layer will not scope by tenant automatically.
:::

### Communication and Data Security: Deployment Baseline

Transport encryption and data protection live in the production baseline of [Security Policy](../community/security):
RabbitMQ and EMQX disable TLS by default and must enable it when traffic crosses a network (e.g.
`RABBITMQ_SSL_ENABLED=true`, over the TLS port); the gateway HTTP entry should sit behind a reverse proxy that
terminates HTTPS; field-protocol ports must never face the public internet (`DC3_BIND_HOST` defaults to `127.0.0.1`, so
you must explicitly set it to `0.0.0.0` to expose anything).

::: info External identity (IdP) not yet implemented
The `dc3_identity_provider` (external IdP config for OIDC/SAML, etc.) and `dc3_external_identity` (external-identity
binding) tables are already created in `02-iot-dc3-auth.sql`, and `principal.source_type` reserves the `EXTERNAL` value,
but the corresponding **login endpoint is not implemented and stays closed**. The only working login path today is the
local-credential two-step handshake above.
:::

::: tip OAuth 2.1 for AI callers
For access by large models / MCP clients, DC3 ships a separate OAuth 2.1 authorization server (mandatory PKCE,
refresh-token rotation, tools filtered by scope and risk level) — see [Agentic & MCP](../ai/)
and [Authentication · Tenancy · RBAC](../architecture/auth-rbac).
:::

## Further Reading

- [Data Intelligence & AIoT](./aiot) — how data, once inside the security perimeter, becomes insight and automated
  decisions
- [IoT Technology Overview](./) — the four-layer reference architecture and where security cuts across
- [Authentication · Tenancy · RBAC](../architecture/auth-rbac) — the full path of login, HMAC pass-through, RBAC, and
  tenant isolation
- [Security Policy](../community/security) — supported versions, vulnerability reporting, and the minimum pre-production
  baseline
