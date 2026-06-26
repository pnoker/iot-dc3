---
title: Security Policy
---

# Security Policy

IoT DC3 is an industrial IoT platform that connects field devices, so a single security gap can expose both data and
control at once. This page covers two things: which versions we still maintain and how to privately report a
vulnerability, and the minimum security baseline to hit before you take the platform to production.

> You already know the platform and you're ready to deploy. Before going to production, also
> read [Environment Variables](../quickstart/environment) and [Deployment Modes and Image Registries](../guide/usage).

## Supported Versions

We ship security patches only for the current mainline release. Version numbers follow a `YYYY.M.x` year-month scheme —
for example, `2026.5.x` is the May 2026 line — and the patch number `x` keeps rolling forward within a mainline. The
current line is `2026.5.x` (latest `2026.5.22`, image tag `2026.6`).

The table lists the release lines that still get security updates. Older versions are no longer back-ported; upgrade to
a supported mainline before reporting.

| Version line           | Supported              |
|------------------------|------------------------|
| `2026.5.x`             | ✅ Supported            |
| `2026.4.x`             | ✅ Supported            |
| `2025.x.x` and earlier | ❌ No longer maintained |

::: tip Upgrade first
Before reporting a vulnerability, confirm it still reproduces on a supported version. Many issues are already fixed on
the newer mainline, and upgrading is often the fastest fix.
:::

## Vulnerability Disclosure Process

We take security reports seriously. Once a vulnerability is confirmed, we fix it as fast as we can and publish the fix
in the release notes.

::: danger Do not disclose publicly
**Do not** post potential vulnerabilities in GitHub / Gitee issues or discussion forums. A public PoC immediately
exposes unpatched instances to attack. Use the private channels below instead.
:::

If you find a potential security vulnerability, report it through either private channel:

1. **Email**: Send a message to the project maintainers with `Security Vulnerability` in the subject so it gets triaged
   first.
2. **Direct message**: Reach the maintainers directly via a Gitee or GitHub private message.

To help us reproduce and pin down the issue, include in your report: the affected version line, reproduction steps or a
minimal repro, the impact (data leak / privilege escalation / command injection, etc.), and any fix ideas you have. Once
we verify the vulnerability, we start the fix and publish the details in that version's release notes.

## Production Security Baseline

The default configuration is built for local development and **optimizes for developer convenience** — weak passwords,
cleartext ports, and development keys are all present. Tighten every one before going to production. The three items
below are the hardest constraints; for the rest, see [Environment Variables](../quickstart/environment).

### 1. Keys must be random, and pre/pro will enforce this

The platform has two keys that must never leak. Their defaults are for development only:

- `AUTH_HMAC_SECRET` — the HMAC-SHA256 key the Gateway uses to sign the `X-Auth-Principal` header when calling backend
  services; default `io.github.pnoker.dc3`.
- `DC3_SECURITY_KEY` — the signing key the Auth Center (`dc3-center-auth`) uses to mint and validate login tokens;
  default `dc3.security.key.2026.io.github.pnoker`.

::: danger HMAC key fails fast in pre/pro environments
When the active Spring profile (or the `spring.env` property) is `pre` or `pro`, and `AUTH_HMAC_SECRET` is empty or
still the default `io.github.pnoker.dc3`, startup throws an `IllegalStateException` and the service **will not start**.
That's intentional — failing to start is better than a production instance running on development keys. In production,
use a strong random value (for example, `openssl rand -base64 48`) and inject it through an environment variable. Never
hardcode it or write it to logs.
:::

```bash
# Generate a strong random value for each key (example output, do not copy verbatim)
openssl rand -base64 48   # → use as AUTH_HMAC_SECRET
openssl rand -base64 48   # → use as DC3_SECURITY_KEY
```

`DC3_SECURITY_KEY` has no startup fail-fast check like the HMAC key, but change it to a strong random value too — once
it leaks, an attacker can forge login tokens.

### 2. Enable TLS — don't run the message bus and broker in cleartext

The platform depends on RabbitMQ and (optionally) the EMQX MQTT broker. Both disable TLS by default, which is only safe
for local use. Turn on encryption in production or anywhere traffic crosses a network:

- RabbitMQ: set `RABBITMQ_SSL_ENABLED=true`, route connections over the TLS port (`5671`, published externally as
  `DC3_RABBITMQ_TLS_PORT`, default `35671`), and optionally enable `RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE` and
  `RABBITMQ_SSL_VERIFY_HOSTNAME` (both default to `false`).
- EMQX: use the MQTT-over-TLS port (`DC3_EMQX_MQTTS_PORT`, default `38883`) and secure WebSocket (`DC3_EMQX_WSS_PORT`,
  default `38084`) instead of the cleartext `31883` / `38083`.

The external HTTP entry point (the `dc3-gateway` gateway, default `8000`) should sit behind a reverse proxy or load
balancer that terminates HTTPS. Put HTTPS / SSL on every external interface and audit external calls.

### 3. Minimize exposed ports — never put field-protocol ports on the public internet

`DC3_BIND_HOST` defaults to `127.0.0.1`, so every published port binds only to the local host; you have to explicitly
set it to `0.0.0.0` to expose them to the network. In production, expose only the ports that **must** be public and keep
everything else behind the internal network or a security group.

::: danger Field-protocol ports must never face the public internet
Field-protocol ports — Modbus, raw TCP/UDP, and the various PLC gateways (for example the listening driver
`dc3-driver-listening-virtual`'s `DC3_LISTENING_VIRTUAL_TCP_PORT=6270` / `UDP=6271`) — generally have no built-in
authentication and **must never** face the public internet directly. Devices should connect over a VPN, a private
network, or through a gateway whitelist. The only thing that should face the public internet is the hardened gateway
HTTP port, behind a reverse proxy.
:::

The ideal exposed surface is a single point: the gateway. The Auth Center (`8300`), Management Center (`8400`), Data
Center (`8500`), Agentic Center (`8600`), and the gRPC ports (`9300/9400/9500`) are all internal and never published
externally. For the port list and defaults, see the "Gateway and Service Ports" and "gRPC / facade" sections
of [Environment Variables](../quickstart/environment).

### Other General Practices

- ✅ Run a supported version, and keep system dependencies and container images up to date.
- 🔑 Change every default password: replace the defaults for PostgreSQL (`POSTGRES_PASSWORD`, default `dc3dc3dc3`),
  RabbitMQ (`RABBITMQ_PASSWORD`), MQTT (`MQTT_PASSWORD`), and the rest with strong random values.
- 🧩 Let only trusted devices and users connect. Apply least privilege on external interfaces and audit access. For how
  tenant isolation and RBAC are implemented, see [Auth, Tenant, RBAC](../architecture/auth-rbac).

## Further Reading

- [Environment Variables](../quickstart/environment) — default values, scope, and production guidance for every
  security-related variable
- [Deployment Modes and Image Registries](../guide/usage) — containerized deployment, port publishing, and image
  registry selection
- [Auth, Tenant, RBAC](../architecture/auth-rbac) — how login, tenant isolation, and the permission model keep
  multi-tenant data safe
