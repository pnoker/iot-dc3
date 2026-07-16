---
title: Deployment Modes and Image Registries
---

<script setup>
import UsageStackDiagram from '../../.vitepress/theme/components/UsageStackDiagram.vue'
</script>


# Deployment Modes and Image Registries

IoT DC3 runs as four Compose stacks: `db` brings up dependencies, `dev` builds from source, `app` pulls prebuilt images,
and `optional` adds observability. This page covers what each stack does, the `make` lifecycle, which registry images
come from, and which ports are exposed. By the end you'll know which stack to pick, which registry to use, and which
ports to lock down in production.

> If you're ready to run the platform, you're in the right place. To set up environment variables first,
> see [Environment Variables](../quickstart/environment). To develop locally from source,
> see [Quick Start](../quickstart/).

## Four stacks, each with its own role

There's no single Compose file — the platform is split by responsibility into four stacks, one per Compose file under
`dc3/`. They layer on top of each other: dependencies come up first, then the application, then observability as needed.

- **`db`** (`docker-compose-db.yml`): the infrastructure layer, just two containers — `dc3-postgres` (PostgreSQL with
  AGE/TimescaleDB/pgvector, holding metadata, time-series values, alarms, and Agentic sessions) and `dc3-rabbitmq` (the
  message bus for data and command streams). Every other stack waits for this one.
- **`dev`** (`docker-compose-dev.yml`): the source-build stack. It builds `dc3-gateway` and the four centers (
  auth/manager/data/agentic) from local `Dockerfile`s. Use it when you're changing backend code and need local
  debugging. The frontend isn't part of this stack — start it separately with `pnpm dev`.
- **`app`** (`docker-compose.yml`): the prebuilt-image stack. It pulls remote images and runs them as-is, including
  `dc3-web`, the gateway, the four centers, and a set of driver containers. Use it for evaluation, demos, and
  production — no compilation, fast startup.
- **`optional`** (`docker-compose-optional.yml`): observability and optional dependencies — EMQX,
  Elasticsearch/Logstash/Kibana, Prometheus, Grafana, APM, and several exporters. Add it when you need it; it doesn't
  sit on the critical path. See [Observability](./observability).

::: info Choosing between dev and app
Use `dev` when you need to change Java code (built on the spot, rebuild after each change). Use `app` when you just want
to run the platform for evaluation or production (pull images, fastest path). Both share the same `db`/`optional`
dependency stacks.
:::

## Deployment topology: what faces outward, what stays internal

The diagram shows how the four stacks layer and where the ingress boundary sits. **In the production form (the `app`
stack), only `dc3-web` and `dc3-driver-listening-virtual` are published to the host.** The gateway and the four centers
live on the internal `dc3net` network, reached through the frontend reverse proxy or internal calls — never exposed
directly.

<UsageStackDiagram lang="en" />

::: danger Only web and listening-virtual are exposed
In the `app` (production) stack, only `dc3-web` (8080/8443) and `dc3-driver-listening-virtual` (TCP 6270 / UDP 6271) are
published to host ports. The gateway's 8000, the four centers' HTTP/gRPC ports, the database, and the message queue *
*all stay on the internal network** — do not expose them to the public internet.

For debugging convenience, the `dev` stack additionally publishes the gateway's 8000, each center's HTTP port (
8300/8400/8500/8600), and the gRPC ports for auth/manager/data (9300/9400/9500; agentic has no gRPC server). That's a
development convenience — **don't carry it into production**. All published ports bind to `DC3_BIND_HOST=127.0.0.1` (
local only) by default; switch to `0.0.0.0` only when you need cross-host access, and narrow the port list first.
:::

## The make lifecycle: one set of commands for every stack

All stacks share the same `make` targets. Variables pick the stack, the services, and the image registry. Run every
command from the `iot-dc3/` directory.

Core lifecycle targets:

- `make build` — build images (the `dev` stack compiles on the spot; the `app` stack usually needs no build)
- `make up` — start (`-d`, detached)
- `make down` — stop and remove containers (keeps data volumes)
- `make config` — render and validate the Compose config without starting anything
- `make logs` — follow logs (`-f --tail=200`)
- `make reset` — ⚠️ down + **delete data volumes**, requires explicit confirmation (see the danger note below)

Variables for picking the stack and services:

| Variable   | Default          | Purpose                                                                                                       |
|------------|------------------|---------------------------------------------------------------------------------------------------------------|
| `STACK`    | `dev`            | Pick the stack: `db` / `dev` / `app` / `optional`                                                             |
| `SERVICES` | (empty = all)    | Operate only on the listed services, space-separated, e.g. `SERVICES="gateway agentic"`                       |
| `GROUP`    | (empty)          | Predefined service group: `center` (the four centers) / `core` (centers + gateway) / `drivers` (driver group) |
| `COMPOSE`  | `podman compose` | Container runtime (this repo standardizes on podman)                                                          |

`GROUP` is shorthand for `SERVICES` — `center` expands to `auth manager data agentic`, `core` adds `gateway`, and
`drivers` expands to the built-in driver set. You can combine `SERVICES` and `GROUP`.

::: code-group

```bash [Start the full environment]
# dependencies → observability → source-build stack
make up STACK=db
make up STACK=optional
make up STACK=dev
```

```bash [Start only some services]
make up STACK=db
make up SERVICES="gateway agentic"   # start only gateway + agentic center
make up GROUP=core                    # start the four centers + gateway
make logs SERVICES="gateway agentic"  # tail logs for just these two
```

```bash [Validate and shut down]
make config STACK=app                 # render the config only, don't start
make down STACK=dev                   # stop the dev stack, keep data
```

:::

::: danger reset deletes data volumes
`make reset` runs down and **deletes data volumes** — every metadata record, time-series value, and alarm in PostgreSQL
is lost. It has a hard gate: it runs only when `CONFIRM_RESET_VOLUMES=true` is set, otherwise it refuses.

```bash
make reset STACK=db CONFIRM_RESET_VOLUMES=true
```

Be careful in production. Once the volume is gone, the next database startup re-runs the initdb seed scripts (see the
final section).
:::

## Image registries: REGISTRY picks the repository, DC3_IMAGE_TAG picks the version

`REGISTRY` decides which repository images come from. At the `make` layer it resolves to `DC3_IMAGE_REGISTRY` — the
namespace Compose actually reads:

| `REGISTRY`       | Resolved `DC3_IMAGE_REGISTRY`                                                             | Use for                                        |
|------------------|-------------------------------------------------------------------------------------------|------------------------------------------------|
| `auto` (default) | Reads `DC3_IMAGE_REGISTRY` from the environment/`.env`, falling back to `pnoker` if unset | Custom private repository, or following `.env` |
| `global`         | `pnoker` (Docker Hub)                                                                     | Overseas / general networks                    |
| `cn`             | `registry.cn-beijing.aliyuncs.com/dc3` (Aliyun)                                           | Mainland China, faster pulls                   |

Any other value fails with `Unsupported REGISTRY`. The image version is controlled by `DC3_IMAGE_TAG` (default
`2026.6`) — all services and dependency images share the same tag. In production, pin a specific version rather than
`latest`.

::: code-group

```bash [Docker Hub (global)]
make up STACK=db REGISTRY=global
make up STACK=app REGISTRY=global
```

```bash [Aliyun (cn, faster in China)]
make up STACK=db REGISTRY=cn
make up STACK=app REGISTRY=cn
```

:::

For example, under `cn` the gateway image resolves to `registry.cn-beijing.aliyuncs.com/dc3/dc3-gateway:2026.6`; under
`global` it's `pnoker/dc3-gateway:2026.6`. The collapsed command reference in the final section lists the full image
set.

::: warning Makefile uses REGISTRY, Compose uses DC3_IMAGE_REGISTRY
Don't confuse the two. `REGISTRY=auto|global|cn` is the `make` selector; it injects the matching `DC3_IMAGE_REGISTRY`
namespace into Compose. When you run `podman compose` directly, set `DC3_IMAGE_REGISTRY` yourself.
:::

## After the stack is up: seed data and external verification

Once the `app`/`dev` stack is running, the platform is ready. The simplest way to check the external ingress is through
the frontend `dc3-web` (default `http://127.0.0.1:8080`). To hit the API from the command line you go through the
gateway — but the gateway isn't exposed in the `app` stack, so this is usually done under the `dev` stack (which
publishes 8000). Login is two steps: first `POST /api/v3/auth/token/salt` to get the salt, then
`POST /api/v3/auth/token/generate` to trade it for a token valid for 12 hours. After that, requests carry the three auth
headers `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token`.

```bash
# Only available under the dev stack where the gateway is exposed; all values are examples, replace with your own tenant/account
# 1) Get the salt (public)
curl -s -X POST http://127.0.0.1:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'
# → returns a salt string (example; use within 5 minutes)

# 2) Hash the password with the salt to exchange for a token (public), returning an access token valid for 12 hours
curl -s -X POST http://127.0.0.1:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"<from previous step>","password":"<hashed password>"}'
```

When the database starts for the first time on an **empty data volume**, the `dc3-postgres` entrypoint runs the **7 seed
scripts** under `initdb` in filename order, building the schema and base data in one pass:

| Order | Script                      | Contents                                              |
|-------|-----------------------------|-------------------------------------------------------|
| 00    | `00-iot-dc3-extensions.sql` | Enable extensions                                     |
| 01    | `01-iot-dc3-common.sql`     | Common tables                                         |
| 02    | `02-iot-dc3-auth.sql`       | Menus, resources, users, roles, OAuth/MCP             |
| 03    | `03-iot-dc3-data.sql`       | Runtime data: alarms, notifications, rules            |
| 04    | `04-iot-dc3-manager.sql`    | Entity management: devices, drivers, points, profiles |
| 05    | `05-iot-dc3-history.sql`    | Time-series hypertables                               |
| 06    | `06-iot-dc3-agentic.sql`    | Sessions, messages, attachments                       |

::: warning Seed scripts run once, on an empty database
These scripts run exactly once — when the data volume is empty. They won't re-run once the volume has data, and editing
the SQL won't take effect on its own. To re-seed, first clear the volume with
`make reset ... CONFIRM_RESET_VOLUMES=true` (which loses data).
:::

::: danger Production secrets must be random
The `DC3_SECURITY_KEY` and `AUTH_HMAC_SECRET` in `.env.example`, along with `POSTGRES_PASSWORD` / `RABBITMQ_PASSWORD` (
default `dc3dc3dc3`), are **publicly known weak defaults** — local use only. Replace them with strong random values
before any production deployment.

`AUTH_HMAC_SECRET` has fail-fast protection: when the Spring profile is `pre` or `pro` and the secret is empty or still
equals the default `io.github.pnoker.dc3`, the service throws `IllegalStateException` at startup and refuses to come up.
See [Environment Variables](../quickstart/environment) for what each secret does.
:::

## Full command and image reference

The collapsible section below is the full original text of `dc3/doc/USAGE.md` — every `make` shortcut and the image
coordinates for each service across the Docker Hub and Aliyun repositories. Keep it handy while operating.

::: details Expand the full command and image list
<!--@include: ../../../dc3/doc/USAGE.md-->
:::

## Further reading

- [Environment Variables](../quickstart/environment) — the default value, scope, and production value of every `DC3_*` /
  runtime variable
- [Observability](./observability) — wiring up EMQX/ELK/Prometheus/Grafana in the `optional` stack, and what to watch
- [Local Development from Source](../quickstart/) — the local workflow using the `dev` stack + IDE for the backend and
  `pnpm dev` for the frontend
