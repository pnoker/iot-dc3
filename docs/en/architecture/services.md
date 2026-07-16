---
title: Services and Topology
---

<script setup>
import ServicesFlowDiagram from '../../.vitepress/theme/components/ServicesFlowDiagram.vue'
import ServicesSequenceDiagram from '../../.vitepress/theme/components/ServicesSequenceDiagram.vue'
</script>


# Services and Topology

IoT DC3 isn't one big process. It's a set of independently deployable services that talk to each other over gRPC and
RabbitMQ. This page covers what those units are, how they fit together, and why they start in a fixed order. Read it
once and every `depends_on` in `docker-compose.yml` will make sense — and you'll be able to debug "why won't the gateway
come up" on your own.

> You are here: you've read the [System Architecture Overview](./) and want to map "five centers + drivers" onto actual
> processes, ports, and startup order. Next, read [Facade Modes](./facade-modes) to see how services call each other, or
> jump to the [Quick Start](../quickstart/) to get the stack running.

## Why Split Into So Many Units

Splitting the platform into a gateway, four centers, and a set of drivers isn't microservices for their own sake. These
categories have genuinely different scaling and failure boundaries. Southbound protocol drivers are many and scale per
site — a different concern from northbound metadata management. Authentication is the mandatory checkpoint on every
request, so it needs to be independent and ready first. Time-series ingestion is high-throughput and needs a dedicated
data center to absorb the RabbitMQ flood. Split apart, each unit can be scaled, restarted, and debugged on its own.

The platform has **six categories of deployable unit**, plus a `single` monolith that packs every center into one
process:

- **Gateway (`dc3-gateway`)** — the only external API entry point. It parses auth headers, injects HMAC signatures,
  routes requests, and hosts the MCP resource server at `/mcp` (reached through the web frontend's reverse proxy under
  the app stack — see the ports section below).
- **Auth Center (`dc3-center-auth`)** — authentication, tenancy, RBAC, and the OAuth 2.1 authorization server. No
  business dependencies, so it's ready first.
- **Manager Center (`dc3-center-manager`)** — metadata for drivers, profiles, devices, points, and so on.
- **Data Center (`dc3-center-data`)** — point-value persistence, command dispatch and acknowledgment, and the alarm
  engine.
- **Agentic Center (`dc3-center-agentic`)** — Spring AI conversations, tool calls, and chat persistence.
- **Protocol Drivers (`dc3-driver-*`)** — the driver catalog has 28 protocol adapters; `docker-compose.yml` ships 22 by
  default (the 6 not included — `ble`/`iec104`/`lwm2m`/`sl651`/`zigbee`/`can` — can be started on demand). Southbound they
  connect to devices; northbound they're decoupled from the data center through RabbitMQ.
- **Single Monolith (`dc3-center-single`)** — folds all four centers into one process, wired in-process via
  `dc3.facade.mode: local`. Good for local development and lightweight deployments (see [Facade Modes](./facade-modes)).

::: info First mention of a center gives "English name + identifier"
The rest of this page follows that glossary: "Data Center" means `dc3-center-data`, "Gateway" means `dc3-gateway`, and
the full names aren't repeated.
:::

## Who Listens on Which Port

Each unit may expose an HTTP port (an external or internal REST entry) and a gRPC port (for facade calls between
centers). **Key constraint: the gateway is the only external API entry point, but its HTTP `8000` isn't published to the
host under the app stack** — `docker-compose.yml` maps only the web frontend's `8080/8443` and the listening-virtual
driver's device inbound ports to the host. External requests go through the web frontend's nginx reverse proxy to
`dc3-gateway:8000` inside the container network. The gateway's `8000` — like the HTTP ports of the other centers — is
reachable only inside that network. Only the dev stack (`docker-compose-dev.yml`) publishes the gateway's `8000` (and
the other center ports) to the host. In production, don't map backend ports to the host.

The diagram below maps the topology by "who depends on whom being ready first." Solid arrows are `depends_on` health
dependencies, labeled with each service's HTTP / gRPC ports.

<ServicesFlowDiagram lang="en" />

The port allocation follows a pattern: HTTP ports `83/84/85/86xx` map one-to-one with gRPC ports `93/94/95xx` for
auth/manager/data. The Agentic Center currently exposes only HTTP `8600`. The single monolith claims HTTP `8100` / gRPC
`9100` (`DC3_SINGLE_PORT` / `DC3_SINGLE_GRPC_PORT`) — they don't clash with the distributed stack's ports and can
coexist on the same machine.

The table below pins down the ports from the diagram, with their host-publishing status. Treat it as the source of truth
when writing code or configuring an nginx reverse proxy. The "Published to host" column reflects the actual `ports:`
mappings in the app stack (`docker-compose.yml`):

| Unit                                | HTTP            | gRPC   | Published to host (app stack)                                                                            | Environment variable (published port)        |
|-------------------------------------|-----------------|--------|----------------------------------------------------------------------------------------------------------|----------------------------------------------|
| Web frontend `dc3-web`              | `8080` / `8443` | —      | **Yes (the app stack's only HTTP entry; nginx reverse-proxies to the gateway)**                          | `DC3_WEB_HTTP_PORT` / `DC3_WEB_HTTPS_PORT`   |
| Gateway `dc3-gateway`               | `8000`          | —      | No (reachable only inside the container network; published via `DC3_GATEWAY_PORT` only in the dev stack) | `DC3_GATEWAY_PORT`                           |
| Auth Center `dc3-center-auth`       | `8300`          | `9300` | No                                                                                                       | `DC3_AUTH_PORT` / `DC3_AUTH_GRPC_PORT`       |
| Manager Center `dc3-center-manager` | `8400`          | `9400` | No                                                                                                       | `DC3_MANAGER_PORT` / `DC3_MANAGER_GRPC_PORT` |
| Data Center `dc3-center-data`       | `8500`          | `9500` | No                                                                                                       | `DC3_DATA_PORT` / `DC3_DATA_GRPC_PORT`       |
| Agentic Center `dc3-center-agentic` | `8600`          | —      | No                                                                                                       | `DC3_AGENTIC_PORT`                           |
| Single monolith `dc3-center-single` | `8100`          | `9100` | Depends on deployment                                                                                    | `DC3_SINGLE_PORT` / `DC3_SINGLE_GRPC_PORT`   |

::: warning Backend HTTP ports are not published to the host in the app stack
In `docker-compose.yml` (the app stack), the gateway and auth/manager/data/agentic have **no** `ports:` mappings —
they're reachable only inside the `dc3net` container network. The only things mapped to the host are the web frontend's
`8080/8443` (`DC3_WEB_HTTP_PORT` / `DC3_WEB_HTTPS_PORT`) and the listening-virtual driver's device inbound ports, TCP
`6270` / UDP `6271` (`DC3_LISTENING_VIRTUAL_TCP_PORT` / `..._UDP_PORT`). So under the app stack, business API calls from
outside go through the web frontend's `8080`, which nginx reverse-proxies to `dc3-gateway:8000` inside the container;
the host can't reach `8000` directly. Only the dev stack (`docker-compose-dev.yml`) publishes the gateway's `8000` (and
the center ports) to the host for direct connection.
:::

## In What Order Do They Start

There's a hard readiness order between services. The gateway injects authentication into requests, so auth must be up
first. Data persists records and dispatches commands, which needs manager's metadata to exist first. Agentic reads data
and invokes commands, so auth/manager/data must all be up. This order isn't there for humans to memorize — Compose
enforces it with `depends_on: condition: service_healthy`: **a dependent starts only after its dependency's health check
passes.**

Health is checked the same way everywhere — each service's `/actuator/health/readiness` (the readiness probe). Note that
the centers' readiness paths carry a base-path prefix while the gateway's does not:

- Gateway: `http://127.0.0.1:8000/actuator/health/readiness`
- Auth Center: `http://127.0.0.1:8300/auth/actuator/health/readiness`
- Manager Center: `http://127.0.0.1:8400/manager/actuator/health/readiness`
- Data Center: `http://127.0.0.1:8500/data/actuator/health/readiness`
- Agentic Center: `http://127.0.0.1:8600/agentic/actuator/health/readiness`

The diagram below traces the full readiness timeline along the dependency chain, from infrastructure to drivers — each
hop begins only after the upstream readiness passes.

<ServicesSequenceDiagram lang="en" />

This diagram also explains a common observation: **drivers depend only on manager**, not on the gateway or the data
center. Once a driver is up, it registers itself with the Manager Center over gRPC (carrying its protocol attribute
definitions), then starts collecting on schedule and pushing point values to the data center over RabbitMQ. So drivers
can start in parallel with the gateway — they don't wait for it.

::: danger Infrastructure must be ready before any application
The centers in `docker-compose.yml` (the application stack) **do not include** PostgreSQL and RabbitMQ — those live in a
separate `docker-compose-db.yml` (the db stack), health-checked with `pg_isready` and `rabbitmq-diagnostics ping`. The
application stack assumes both are already healthy. So the correct startup sequence is **bring up the db stack first,
wait for it to be healthy, then bring up the application stack**. Skip this and auth will restart in a loop because it
can't reach the database. The matching commands follow.
:::

## Getting the Stack Running

In practice it's two steps: bring up the db stack (PostgreSQL + RabbitMQ), then the application stack. The Makefile
wraps the Compose details; run the commands from the `iot-dc3/` directory:

::: code-group

```bash [make (recommended)]
# 1. Bring up infrastructure first, wait for it to be healthy
make up-db

# 2. Bring up the application stack (build images + start in depends_on order)
make up STACK=app

# Follow logs to confirm each service's readiness passes in turn
make logs
```

```bash [podman compose (low-level)]
# db stack: postgres + rabbitmq
podman compose -f dc3/docker-compose-db.yml up -d

# application stack: gateway + four centers + drivers
podman compose -f dc3/docker-compose.yml up -d

# validate compose syntax
podman compose -f dc3/docker-compose.yml config --quiet
```

:::

After the stack is up, confirm the whole chain is ready. Under the app stack the gateway's `8000` isn't published to the
host, so connecting to `127.0.0.1:8000` from the host will fail — probe from inside the gateway container (same address
as the container healthcheck), or reach it from the host through the web frontend's `8080`:

```bash
# app stack: probe readiness inside the gateway container (expect {"status":"UP"})
podman exec dc3-gateway curl -fsS http://127.0.0.1:8000/actuator/health/readiness

# the host entry point is the web frontend's 8080 (nginx reverse-proxies to dc3-gateway:8000)
curl -fsS http://127.0.0.1:8080/
```

::: info Only the dev stack lets you connect to gateway 8000 directly from the host
If you start with `make up-dev` (the dev stack, `docker-compose-dev.yml`), the gateway's `8000` is published to the
host, so you can run `curl -fsS http://127.0.0.1:8000/actuator/health/readiness` directly.
:::

::: tip For local development, the single monolith avoids multi-process orchestration
If you just want to validate business logic quickly on your machine, there's no need to bring up six containers:
`dc3-center-single` wires the centers' capabilities in-process via `dc3.facade.mode: local`, listening on HTTP `8100` /
gRPC `9100`. The difference between distributed and monolith is only deployment topology — the business semantics are
unchanged. See [Facade Modes](./facade-modes) for details.
:::

## Constraints and Boundaries

- **The gateway is the only external API entry point, but the host entry differs by stack.** In the app stack (
  `docker-compose.yml`), only the web frontend's `8080/8443` and listening-virtual's device inbound ports TCP `6270`/UDP
  `6271` are mapped to the host; the gateway's `8000` isn't published, and external requests pass through the web
  frontend's nginx reverse proxy to `dc3-gateway:8000`. Only the dev stack (`docker-compose-dev.yml`) publishes the
  gateway's `8000` and the center ports to the host. In either stack, the remaining backend ports are reachable only
  inside the container network — don't map them to the host in production.
- **Startup order is enforced by health checks, not manual sleeps.** `depends_on: condition: service_healthy` makes a
  dependent wait until the dependency's readiness passes before starting — but this only covers the application stack
  internally. You still have to bring up the db stack yourself first.
- **Readiness paths carry a base path.** The centers use `webflux.base-path` (e.g. auth's `/auth`), so the probe paths
  carry that prefix; the gateway doesn't. Don't drop the prefix when writing monitoring or liveness scripts.
- **Distributed mode uses the gRPC facade by default.** For centers like manager, `dc3.facade.mode` defaults to
  `${DC3_FACADE_MODE:grpc}`, and `dc3/env/dev.env` also sets it to `grpc`; only the single monolith's base
  `application.yml` declares `local`. This is a deployment-topology choice, not a protocol choice —
  see [Facade Modes](./facade-modes) for details.

## Further Reading

- [System Architecture Overview](./) — the holistic view of the closed loop and where each role fits
- [Facade Modes](./facade-modes) — how `grpc` (distributed) and `local` (monolith) switch, and why this is topology
  rather than protocol
- [Quick Start](../quickstart/) — bring up the stack from scratch locally and get your first device working
- [Auth · Tenancy · RBAC](./auth-rbac) — how the gateway injects authentication headers and HMAC signatures
