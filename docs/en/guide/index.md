---
title: "Deployment & Operations"
---

<script setup>
import GuideIndexDiagram from '../../.vitepress/theme/components/GuideIndexDiagram.vue'
</script>


# Deployment & Operations

This section takes IoT DC3 from a `java -jar` on your laptop to a fleet of orchestrated containers — and then makes it
observable and debuggable in production. It covers deployment topologies, image registries, the observability stack,
logging conventions, and troubleshooting. By the end you'll know where each topic lives and where the line falls between
local development and containerized deployment.

> You are here: you've already [brought the stack up locally and run your first device](../quickstart/), and now you
> want to deploy, observe, and operate it.

## Two routes — draw the boundary first

Every topic here lands on one of two routes. They pull their environment variables from different places, and mixing
them up is the single most common pitfall:

- **Local development**: dependencies (PostgreSQL, RabbitMQ, optionally EMQX/ELK/Prometheus) run in containers, but the
  Java processes (the gateway, the four centers, and the drivers) run directly in your IDE or via `java -jar`. This
  route is owned by [Quick Start](../quickstart/) — it's fast to debug and picks up code changes immediately.
- **Containerized deployment**: the gateway, the four centers, the drivers, and their dependencies are all orchestrated
  as containers. This route is owned by [Deployment Modes & Image Registries](./usage).

::: warning Environment variables don't cross over automatically
The root-level `.env` **is for Docker Compose only** — it is not injected into Java processes running on your host. When
you run Java locally, use `dc3/env/dev.env` (read by the IDE EnvFile plugin) or `source dc3/env/dev.env.sh` (shell
export) to point the services at the ports Compose publishes on `localhost` (PostgreSQL `35432`, RabbitMQ `35672`).
Pointing a local Java process at the in-container hostnames (`dc3-postgres`, `dc3-rabbitmq`) will always fail the
connection.
:::

## How to read this section

The four sub-pages each cover one stage of the operations lifecycle: bring the services up (deployment and image
registries), make them visible (observability, logging), and make them fixable when something breaks (troubleshooting).

<GuideIndexDiagram lang="en" />

- **[Deployment Modes & Image Registries](./usage)** — container image selection, registry switching, and Compose
  orchestration. `make` picks the registry through `REGISTRY` (`auto`/`global`/`cn`): `global` uses the default
  registry, `cn` uses a mainland-China mirror. The fastest way to confirm the stack comes up is `make up-db`; on a China
  network use `make up-db-cn` instead.
- **[Observability](./observability)** — how the app and its dependencies plug into Grafana, Prometheus, and ELK (the
  optional `optional` stack). Run `make up-optional` to bring up EMQX/ELK/Prometheus/Grafana; ports are listed under
  the "Observability Stack" section of the environment-variable reference (Grafana `3000`, Kibana `5601`).
- **[Logging Conventions](./logging)** — `dc3-common-log` emits colored console logs for human debugging and rolling
  JSON file logs for machine parsing (timestamp/logger/thread/level/MDC/message/stack). Messages use stable English
  event names with SLF4J parameterized placeholders, so they're easy to search and correlate across modules.
- **[Troubleshooting](./troubleshooting)** — finding and fixing the common issues: slow builds, JDK version mismatches,
  port conflicts, DB/MQ connection failures, Gateway 401/403, and drivers that fail to register.

## A few of the most common commands

The container stack's lifecycle runs through `make`, following the pattern `make <op>-<stack>[-<registry>]`. Here are
the commands you'll type most often (run them from the `iot-dc3/` directory):

::: code-group

```bash [Start the dependency stack]
# Start PostgreSQL + RabbitMQ (minimal dependencies)
make up-db

# On a China network, use the mainland-China image registry
make up-db-cn

# Add the optional observability stack: EMQX / ELK / Prometheus / Grafana
make up-optional
```

```bash [View logs]
# Follow a stack's logs (last 200 lines)
make logs STACK=db

# Show only the specified services
make logs SERVICES="gateway agentic"
```

```bash [Prerequisite for running from local source]
# Point local Java processes at the ports Compose publishes to localhost
source dc3/env/dev.env.sh
```

:::

::: tip Startup order
For a distributed bring-up, start in the order Auth → Manager → Data → Agentic → Gateway → Driver: Auth has no
dependencies and starts first; the Gateway starts only after the four centers are healthy (`gateway`'s `depends_on`
requires auth/manager/data/agentic to all be `service_healthy`); and drivers can register only after Manager Center and
RabbitMQ are ready. See [Troubleshooting · Drivers fail to register](./troubleshooting) for details.
:::

## Verify the wiring: walk the golden path once

After deployment, the fastest way to confirm the gateway and the authentication chain are connected is to log in: fetch
the salt, then exchange the salted credential for a token. Every external request goes through one HTTP entry point, the
gateway (default `8000`).

```bash
# 1) Fetch the salt (public endpoint; use within 5 minutes; the tenant/username below are example values)
curl -X POST http://localhost:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'

# 2) Hash the credential with the salt, then exchange it for a token (valid for 12 hours)
curl -X POST http://localhost:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"<salt returned by the previous step>","password":"<salted hash>"}'
```

Once you have the token, protected endpoints require three headers: `X-Auth-Tenant`, `X-Auth-Login`, and `X-Auth-Token`.
A 401/403 here almost always means a missing or expired token —
see [Troubleshooting · Gateway returns 401 or 403](./troubleshooting).

## Further reading

- [Deployment Modes & Image Registries](./usage) — container images, registry switching, and Compose orchestration
- [Observability](./observability) — integrating Grafana, Prometheus, and ELK
- [Logging Conventions](./logging) — log message style, levels, and output format
- [Troubleshooting](./troubleshooting) — locating and fixing startup and connection issues
- [Quick Start](../quickstart/) — bring the stack up locally and run your first device (the starting point of the
  local-development route)
