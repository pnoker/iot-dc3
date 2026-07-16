---
title: Environment Variables Explained
---

<script setup>
import EnvironmentDiagram from '../../.vitepress/theme/components/EnvironmentDiagram.vue'
</script>


# Environment Variables Explained

IoT DC3 has two separate sets of environment files, and they target different consumers. The root `.env` feeds Docker
Compose interpolation; `dc3/env/dev.env(.sh)` feeds local Java processes. By the end of this page you'll know which
variable belongs in which file, why `localhost` ports differ from the ports a container sees, and which two secrets you
must change before going to production.

> You are here: you've already [developed locally from source](./) or brought the stack up with Compose. Next,
> see [Deployment Modes and Image Sources](../guide/usage) to learn how the whole stack starts.

## Why Two Sets of Files

The same variable name (say `POSTGRES_HOST`) means different things inside a container versus in a Java process on your
laptop. Containers reach each other by service name on the Compose network (`dc3-postgres`). A Java process running in
your IDE lives on the host, so it can only connect through the **ports Compose publishes to the host** (such as
`localhost:35432`). The two file sets exist for these two non-overlapping paths. Mix them up and a local process tries
to resolve a container name it can't reach, or a container tries to talk to a `localhost` it can't see.

| File                 | Consumer                | Purpose                                                                            | Injection Method                    |
|----------------------|-------------------------|------------------------------------------------------------------------------------|-------------------------------------|
| `.env.example`       | Docker Compose template | Copied to root `.env`; defines image registry, image tag, published ports          | Compose variable interpolation      |
| `.env`               | Docker Compose          | Untracked local config, interpolated by `dc3/docker-compose*.yml`                  | Compose variable interpolation      |
| `dc3/env/dev.env`    | IDE (EnvFile plugin)    | Environment variables for local Java processes, **without** `export`               | Read by the IDE EnvFile plugin      |
| `dc3/env/dev.env.sh` | Shell                   | Environment variables for local Java processes, with `export`, loaded via `source` | Injected into the shell environment |

::: warning The root `.env` is not injected into local Java processes
The root `.env` serves Docker Compose only. It is **not** automatically passed to Java processes started by your IDE or
command line. Running from source locally requires `dc3/env/dev.env(.sh)`, which points the process at the dependency
ports Compose publishes on `localhost`. Setting `POSTGRES_HOST=localhost` in the root `.env` changes nothing about any
container's runtime environment.
:::

## Two Isolated Paths

The diagram shows how each file set takes effect along its own path. The key point: these two paths **never cross**.
Compose does not read `dev.env.sh`, and local Java processes do not read the root `.env`.

<EnvironmentDiagram lang="en" />

The mapping between host ports and internal ports is the core of this diagram:

| Dependency    | host (for local processes) | internal (for container-to-container) |
|---------------|----------------------------|---------------------------------------|
| PostgreSQL    | `localhost:35432`          | `dc3-postgres:5432`                   |
| RabbitMQ AMQP | `localhost:35672`          | `dc3-rabbitmq:5672`                   |
| EMQX MQTT     | `localhost:31883`          | `dc3-emqx:1883` (conventional value)  |

## How to Use

### Bring the Stack Up with Compose

Create a local `.env` from the template, then bring it up with `make` (backed by `podman compose`):

::: code-group

```bash [make]
cp .env.example .env
make up-db && make up-optional && make up-dev
```

```bash [podman compose]
cp .env.example .env
podman compose -f dc3/docker-compose-dev.yml config --quiet
```

:::

Variables in the root `.env` interpolate the compose files, for example images and published ports:

```yaml
image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-gateway:${DC3_IMAGE_TAG:-2026.6}
ports:
  - "${DC3_BIND_HOST:-127.0.0.1}:${DC3_GATEWAY_PORT:-8000}:8000"
```

Compose does not inject every variable in `.env` into every container. Only the variables the compose files explicitly
reference via `environment` or `env_file` reach a container.

### Run Locally from Source

`source` the file before starting a Java process from the command line:

```bash
source dc3/env/dev.env.sh
```

Without it, the local process falls back to container service names (`dc3-postgres`, `dc3-rabbitmq`,
`dc3-center-manager`) or default ports, so it can't reach the dependencies on your machine. JetBrains IDEA users should
use `dc3/env/dev.env` instead (same content, no `export`): install the EnvFile plugin, enable EnvFile in the Run
Configuration, and add `dc3/env/dev.env`. Don't use `.env.example` directly as an IDEA environment file — it's a Compose
template, not Java runtime configuration.

## Key Variables Grouped by Scenario

The groups below cover what you actually need for a given scenario. The full long table is collapsed under `::: details`
at the end of the page; here we list only the variables you'll really change or look up when troubleshooting. The three
`Scope` values mean: `Runtime` (read by both local and in-container Java processes), `Compose only` (only the root
`.env` for Compose), and `Per-process` (a single-service override).

### Security Keys (Runtime)

Two secrets are the platform's root of identity. Their defaults exist only for first-run convenience and must never go
to production.

| Variable           | Default                                  | Purpose                                                                          |
|--------------------|------------------------------------------|----------------------------------------------------------------------------------|
| `DC3_SECURITY_KEY` | `dc3.security.key.2026.io.github.pnoker` | Signing key the auth center uses to generate and verify login tokens             |
| `AUTH_HMAC_SECRET` | `io.github.pnoker.dc3`                   | HMAC-SHA256 key the gateway uses to sign `X-Auth-Principal` for backend services |

::: danger Change to strong random values in production, and never commit or print them
`DC3_SECURITY_KEY` and `AUTH_HMAC_SECRET` ship with defaults. In production, change them to strong random values and
never commit them to the repository, write them to logs, or print them as-is. When the Spring profile is `pre` or `pro`
and `AUTH_HMAC_SECRET` is empty or still equals the default `io.github.pnoker.dc3`, the service throws
`IllegalStateException` and refuses to start. This is an intentional security gate — don't bypass it.
:::

### PostgreSQL (Runtime)

Local processes connect to `localhost:35432`; in-container processes connect to `dc3-postgres:5432`. `POSTGRES_SCHEMA`
overrides the schema only inside a single-service process (such as `dc3_manager` or `dc3_data`) — don't set it globally.

| Variable            | Default     | Scope        | Purpose                                          |
|---------------------|-------------|--------------|--------------------------------------------------|
| `POSTGRES_HOST`     | `localhost` | Runtime      | `localhost` locally, `dc3-postgres` in-container |
| `POSTGRES_PORT`     | `35432`     | Runtime      | host-published port; internal is `5432`          |
| `POSTGRES_USERNAME` | `dc3`       | Runtime      | Username                                         |
| `POSTGRES_PASSWORD` | `dc3dc3dc3` | Runtime      | Password                                         |
| `POSTGRES_DB`       | `dc3`       | Runtime      | Database name                                    |
| `POSTGRES_SCHEMA`   | (unset)     | Per-process  | Single-service schema override                   |
| `DC3_POSTGRES_PORT` | `35432`     | Compose only | Port the container publishes to the host         |

### RabbitMQ (Runtime)

AMQP host port `35672`, internal `5672`. With TLS on, the internal port switches to `5671`.

| Variable                       | Default     | Scope        | Purpose                                          |
|--------------------------------|-------------|--------------|--------------------------------------------------|
| `RABBITMQ_HOST`                | `localhost` | Runtime      | `localhost` locally, `dc3-rabbitmq` in-container |
| `RABBITMQ_PORT`                | `35672`     | Runtime      | AMQP host port; internal `5672`                  |
| `RABBITMQ_USERNAME`            | `dc3`       | Runtime      | Username                                         |
| `RABBITMQ_PASSWORD`            | `dc3dc3dc3` | Runtime      | Password                                         |
| `RABBITMQ_VIRTUAL_HOST`        | `dc3`       | Runtime      | virtual host                                     |
| `RABBITMQ_SSL_ENABLED`         | `false`     | Runtime      | Enable TLS (uses 5671 when true)                 |
| `DC3_RABBITMQ_PORT`            | `35672`     | Compose only | AMQP published port                              |
| `DC3_RABBITMQ_MANAGEMENT_PORT` | `15672`     | Compose only | Management UI published port                     |

### EMQX / MQTT (Runtime)

MQTT broker host port `31883`. EMQX also publishes several other ports (WebSocket, Dashboard) — see the collapsed long
table for the full list.

| Variable                  | Default     | Scope        | Purpose                                                 |
|---------------------------|-------------|--------------|---------------------------------------------------------|
| `MQTT_BROKER_HOST`        | `localhost` | Runtime      | broker host                                             |
| `MQTT_BROKER_PORT`        | `31883`     | Runtime      | broker port (published by EMQX; internal around `1883`) |
| `MQTT_USERNAME`           | `dc3`       | Runtime      | Username                                                |
| `MQTT_PASSWORD`           | `dc3dc3dc3` | Runtime      | Password                                                |
| `DC3_EMQX_MQTT_PORT`      | `31883`     | Compose only | MQTT published port                                     |
| `DC3_EMQX_DASHBOARD_PORT` | `18083`     | Compose only | Dashboard published port                                |

### gRPC / facade (Runtime)

Center services talk to each other through facades. Distributed deployments default to `DC3_FACADE_MODE=grpc`, and local
processes point `CENTER_*_HOST` at `localhost`.

| Variable                      | Default     | Scope   | Purpose                                                     |
|-------------------------------|-------------|---------|-------------------------------------------------------------|
| `CENTER_AUTH_HOST`            | `localhost` | Runtime | Auth center host                                            |
| `CENTER_MANAGER_HOST`         | `localhost` | Runtime | Manager center host                                         |
| `CENTER_DATA_HOST`            | `localhost` | Runtime | Data center host                                            |
| `CENTER_AGENTIC_HOST`         | `localhost` | Runtime | Agentic center host                                         |
| `DC3_FACADE_MODE`             | `grpc`      | Runtime | facade protocol mode                                        |
| `DC3_FACADE_GRPC_DEADLINE_MS` | `3000`      | Runtime | Per-request gRPC deadline; `0` disables the client deadline |

### Gateway and Service Ports (Compose only)

The gateway is the only external HTTP entry point (`8000`). The HTTP and gRPC published ports of each center are listed
below. `SERVER_PORT` and `GRPC_SERVER_PORT` are single-process overrides, useful only when you run multiple services
locally and need to avoid port collisions.

| Variable                         | Default | Scope        | Purpose                                   |
|----------------------------------|---------|--------------|-------------------------------------------|
| `DC3_GATEWAY_PORT`               | `8000`  | Compose only | Gateway HTTP published port (entry point) |
| `DC3_AUTH_PORT`                  | `8300`  | Compose only | Auth center HTTP                          |
| `DC3_MANAGER_PORT`               | `8400`  | Compose only | Manager center HTTP                       |
| `DC3_DATA_PORT`                  | `8500`  | Compose only | Data center HTTP                          |
| `DC3_AGENTIC_PORT`               | `8600`  | Compose only | Agentic center HTTP                       |
| `DC3_AUTH_GRPC_PORT`             | `9300`  | Compose only | Auth center gRPC                          |
| `DC3_MANAGER_GRPC_PORT`          | `9400`  | Compose only | Manager center gRPC                       |
| `DC3_DATA_GRPC_PORT`             | `9500`  | Compose only | Data center gRPC                          |
| `DC3_LISTENING_VIRTUAL_TCP_PORT` | `6270`  | Compose only | Listening Virtual driver TCP publish      |
| `DC3_LISTENING_VIRTUAL_UDP_PORT` | `6271`  | Compose only | Listening Virtual driver UDP publish      |
| `SERVER_PORT`                    | (unset) | Per-process  | Single-service HTTP port override         |
| `GRPC_SERVER_PORT`               | (unset) | Per-process  | Single-center gRPC port override          |

::: warning `DC3_LISTENING_VIRTUAL_*_PORT` are host-published ports
`DC3_LISTENING_VIRTUAL_TCP_PORT` and `DC3_LISTENING_VIRTUAL_UDP_PORT` are the ports Compose publishes to the host. The
process's internal ports use `TCP_PORT` and `UDP_PORT` (Per-process). Don't confuse the two.
:::

### Agentic / AI (Runtime)

The `AGENTIC_FALLBACK_OPENAI_*` group only kicks in as a fallback when `dc3_model_provider` has no usable provider
configured. Conversation memory is off by default.

| Variable                              | Default                        | Purpose                                                                         |
|---------------------------------------|--------------------------------|---------------------------------------------------------------------------------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL`    | `https://api.openai.com`       | Fallback OpenAI-compatible API address                                          |
| `AGENTIC_FALLBACK_OPENAI_API_KEY`     | (empty)                        | Fallback API key (fill in when the endpoint requires authentication)            |
| `AGENTIC_FALLBACK_OPENAI_MODEL`       | `gpt-4o`                       | Fallback model name                                                             |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | `0.7`                          | Sampling temperature (0.0–2.0)                                                  |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS`  | `2048`                         | Maximum output tokens                                                           |
| `AGENTIC_MEMORY_SCHEMA_INIT`          | `never`                        | Spring AI JDBC memory table init mode (`always`/`never`/`create_if_not_exists`) |
| `AGENTIC_MEMORY_ENABLED`              | `false`                        | Whether persistent conversation memory is on                                    |
| `AGENTIC_TOOL_CALLING_ENABLED`        | `true`                         | Whether tool calling is on                                                      |
| `AGENTIC_MEMORY_MAX_MESSAGES`         | `50`                           | Maximum messages retained per conversation window                               |
| `AGENTIC_ATTACHMENT_STORAGE_PATH`     | `dc3/data/agentic/attachments` | Attachment storage path                                                         |

::: info The table binding for `AGENTIC_MEMORY_SCHEMA_INIT` is governed by the code
`AGENTIC_MEMORY_SCHEMA_INIT` defaults to `never` and is meant to control the memory table init mode. But after Compose
injects it into the container, no `application*.yml` in the repository binds it to Spring AI's `initialize-schema`; the
memory tables are actually pre-created by the initdb scripts. Whether setting it to `always` truly triggers automatic
table creation needs to be determined from the code — don't treat it as a wired, working switch.
:::

::: danger Real API keys must not enter docs, logs, or commit history
Sensitive values like `AGENTIC_FALLBACK_OPENAI_API_KEY` must never be written into docs, logs, or commit history.
Configure production providers in the `dc3_model_provider` table; the fallback is just a safety net.
:::

::: info Defaults in the table come from `.env.example` and differ from the in-code fallback defaults
The values above for `AGENTIC_MEMORY_ENABLED` (`false`) and `AGENTIC_ATTACHMENT_STORAGE_PATH` (
`dc3/data/agentic/attachments`) come from `.env.example`. On the `cp .env.example .env` + `source` path, these values
are explicitly injected. But the in-code fallback defaults in `application-agentic.yml` differ: when the environment
variables are unset, memory defaults to enabled and the attachment path is `dc3/data/upload/agentic/attachment`. When
you're not on the `.env.example` path, the code takes precedence.
:::

### Batch Processing (Runtime)

MQTT and point values each have a "count threshold + interval" pair. A Quartz schedule flushes the accumulated buffer
all at once every `interval` seconds, where `speed = count / interval`.

| Variable               | Default | Purpose                                   |
|------------------------|---------|-------------------------------------------|
| `MQTT_BATCH_SPEED`     | `100`   | MQTT batch size threshold (records/batch) |
| `MQTT_BATCH_INTERVAL`  | `5`     | MQTT batch interval (seconds)             |
| `POINT_BATCH_SPEED`    | `100`   | Point value batch size threshold          |
| `POINT_BATCH_INTERVAL` | `5`     | Point value batch interval (seconds)      |

### Image Sources (Compose only)

On mainland China networks, set `REGISTRY` to `cn` to use the Aliyun mirror. Note: the Makefile reads `REGISTRY` and
Compose interpolation reads `DC3_IMAGE_REGISTRY` — each governs its own segment.

| Variable             | Default     | Purpose                                                                                    |
|----------------------|-------------|--------------------------------------------------------------------------------------------|
| `REGISTRY`           | `auto`      | Makefile image-source selector (accepts only `auto`/`global`/`cn`; other values error out) |
| `DC3_IMAGE_REGISTRY` | `pnoker`    | Image namespace                                                                            |
| `DC3_IMAGE_TAG`      | `2026.6`    | Image tag for all services and dependencies                                                |
| `DC3_BIND_HOST`      | `127.0.0.1` | Published-port bind address (`0.0.0.0` for external access)                                |

### Observability (Compose only / Runtime)

The optional stack (EMQX, ELK, Prometheus, Grafana) comes up via `make up-optional`. Its ports and JVM parameters are
below.

| Variable             | Default                 | Scope        | Purpose                          |
|----------------------|-------------------------|--------------|----------------------------------|
| `GF_SERVER_ROOT_URL` | `http://localhost:3000` | Runtime      | Grafana external root URL        |
| `DC3_GRAFANA_PORT`   | `3000`                  | Compose only | Grafana published port           |
| `DC3_KIBANA_PORT`    | `5601`                  | Compose only | Kibana published port            |
| `DC3_ES_JAVA_OPTS`   | `-Xms512m -Xmx512m`     | Runtime      | Elasticsearch JVM heap           |
| `DC3_LS_JAVA_OPTS`   | `-Xms256m -Xmx256m`     | Runtime      | Logstash JVM heap                |
| `APM_AGENT_ENABLE`   | `false`                 | Runtime      | Whether the Java APM agent is on |

::: details Full Variable Reference (collapsed)

#### Security & Authentication (Runtime)

| Variable           | Default                                  | Purpose                            |
|--------------------|------------------------------------------|------------------------------------|
| `DC3_SECURITY_KEY` | `dc3.security.key.2026.io.github.pnoker` | Login token signing key            |
| `AUTH_HMAC_SECRET` | `io.github.pnoker.dc3`                   | `X-Auth-Principal` HMAC-SHA256 key |

#### PostgreSQL

| Variable            | Default     | Scope        |
|---------------------|-------------|--------------|
| `POSTGRES_HOST`     | `localhost` | Runtime      |
| `POSTGRES_PORT`     | `35432`     | Runtime      |
| `POSTGRES_USERNAME` | `dc3`       | Runtime      |
| `POSTGRES_PASSWORD` | `dc3dc3dc3` | Runtime      |
| `POSTGRES_DB`       | `dc3`       | Runtime      |
| `POSTGRES_SCHEMA`   | (unset)     | Per-process  |
| `DC3_POSTGRES_PORT` | `35432`     | Compose only |

#### RabbitMQ

| Variable                                   | Default      | Scope        |
|--------------------------------------------|--------------|--------------|
| `RABBITMQ_HOST`                            | `localhost`  | Runtime      |
| `RABBITMQ_PORT`                            | `35672`      | Runtime      |
| `RABBITMQ_USERNAME`                        | `dc3`        | Runtime      |
| `RABBITMQ_PASSWORD`                        | `dc3dc3dc3`  | Runtime      |
| `RABBITMQ_VIRTUAL_HOST`                    | `dc3`        | Runtime      |
| `RABBITMQ_MQTT_EXCHANGE`                   | `dc3.e.mqtt` | Runtime      |
| `RABBITMQ_SSL_ENABLED`                     | `false`      | Runtime      |
| `RABBITMQ_SSL_ALGORITHM`                   | `TLS`        | Runtime      |
| `RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE` | `false`      | Runtime      |
| `RABBITMQ_SSL_VERIFY_HOSTNAME`             | `false`      | Runtime      |
| `RABBITMQ_CONTAINER_PORT`                  | `5672`       | Runtime      |
| `DC3_RABBITMQ_PORT`                        | `35672`      | Compose only |
| `DC3_RABBITMQ_TLS_PORT`                    | `35671`      | Compose only |
| `DC3_RABBITMQ_MANAGEMENT_PORT`             | `15672`      | Compose only |

#### EMQX / MQTT

| Variable                  | Default     | Scope        |
|---------------------------|-------------|--------------|
| `MQTT_BROKER_HOST`        | `localhost` | Runtime      |
| `MQTT_BROKER_PORT`        | `31883`     | Runtime      |
| `MQTT_USERNAME`           | `dc3`       | Runtime      |
| `MQTT_PASSWORD`           | `dc3dc3dc3` | Runtime      |
| `MQTT_BATCH_SPEED`        | `100`       | Runtime      |
| `MQTT_BATCH_INTERVAL`     | `5`         | Runtime      |
| `DC3_EMQX_WS_PORT`        | `38083`     | Compose only |
| `DC3_EMQX_WSS_PORT`       | `38084`     | Compose only |
| `DC3_EMQX_MQTT_PORT`      | `31883`     | Compose only |
| `DC3_EMQX_MQTTS_PORT`     | `38883`     | Compose only |
| `DC3_EMQX_DASHBOARD_PORT` | `18083`     | Compose only |

#### gRPC / facade

| Variable                      | Default     | Scope        |
|-------------------------------|-------------|--------------|
| `CENTER_AUTH_HOST`            | `localhost` | Runtime      |
| `CENTER_MANAGER_HOST`         | `localhost` | Runtime      |
| `CENTER_DATA_HOST`            | `localhost` | Runtime      |
| `CENTER_AGENTIC_HOST`         | `localhost` | Runtime      |
| `DC3_FACADE_MODE`             | `grpc`      | Runtime      |
| `DC3_FACADE_GRPC_DEADLINE_MS` | `3000`      | Runtime      |
| `DC3_AUTH_GRPC_PORT`          | `9300`      | Compose only |
| `DC3_MANAGER_GRPC_PORT`       | `9400`      | Compose only |
| `DC3_DATA_GRPC_PORT`          | `9500`      | Compose only |

#### HTTP Gateway & Service Ports

| Variable                         | Default | Scope        |
|----------------------------------|---------|--------------|
| `DC3_GATEWAY_PORT`               | `8000`  | Compose only |
| `DC3_AUTH_PORT`                  | `8300`  | Compose only |
| `DC3_MANAGER_PORT`               | `8400`  | Compose only |
| `DC3_DATA_PORT`                  | `8500`  | Compose only |
| `DC3_AGENTIC_PORT`               | `8600`  | Compose only |
| `SERVER_PORT`                    | (unset) | Per-process  |
| `GRPC_SERVER_PORT`               | (unset) | Per-process  |
| `DC3_LISTENING_VIRTUAL_TCP_PORT` | `6270`  | Compose only |
| `DC3_LISTENING_VIRTUAL_UDP_PORT` | `6271`  | Compose only |
| `TCP_PORT`                       | (unset) | Per-process  |
| `UDP_PORT`                       | (unset) | Per-process  |
| `GATEWAY_ROUTE_AUTH_TOKEN_URI`   | (unset) | Per-process  |
| `GATEWAY_ROUTE_AUTH_URI`         | (unset) | Per-process  |
| `GATEWAY_ROUTE_MANAGER_URI`      | (unset) | Per-process  |
| `GATEWAY_ROUTE_DATA_URI`         | (unset) | Per-process  |
| `GATEWAY_ROUTE_AGENTIC_URI`      | (unset) | Per-process  |

#### Agentic / AI (Runtime)

| Variable                              | Default                        |
|---------------------------------------|--------------------------------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL`    | `https://api.openai.com`       |
| `AGENTIC_FALLBACK_OPENAI_API_KEY`     | (empty)                        |
| `AGENTIC_FALLBACK_OPENAI_MODEL`       | `gpt-4o`                       |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | `0.7`                          |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS`  | `2048`                         |
| `AGENTIC_MEMORY_SCHEMA_INIT`          | `never`                        |
| `AGENTIC_MEMORY_ENABLED`              | `false`                        |
| `AGENTIC_MEMORY_MAX_MESSAGES`         | `50`                           |
| `AGENTIC_TOOL_CALLING_ENABLED`        | `true`                         |
| `AGENTIC_ATTACHMENT_STORAGE_PATH`     | `dc3/data/agentic/attachments` |

#### Batch Processing / Images / Observability

| Variable               | Default                 | Scope        |
|------------------------|-------------------------|--------------|
| `POINT_BATCH_SPEED`    | `100`                   | Runtime      |
| `POINT_BATCH_INTERVAL` | `5`                     | Runtime      |
| `REGISTRY`             | `auto`                  | Compose only |
| `DC3_IMAGE_REGISTRY`   | `pnoker`                | Compose only |
| `DC3_IMAGE_TAG`        | `2026.6`                | Compose only |
| `DC3_LOG_MAX_SIZE`     | `10M`                   | Compose only |
| `DC3_LOG_MAX_FILE`     | `20`                    | Compose only |
| `DC3_BIND_HOST`        | `127.0.0.1`             | Compose only |
| `GF_SERVER_ROOT_URL`   | `http://localhost:3000` | Runtime      |
| `DC3_GRAFANA_PORT`     | `3000`                  | Compose only |
| `DC3_KIBANA_PORT`      | `5601`                  | Compose only |
| `DC3_ES_JAVA_OPTS`     | `-Xms512m -Xmx512m`     | Runtime      |
| `DC3_LS_JAVA_OPTS`     | `-Xms256m -Xmx256m`     | Runtime      |
| `APM_AGENT_ENABLE`     | `false`                 | Runtime      |
| `NODE_ENV`             | `dev`                   | Runtime      |

:::

## Constraints and Common Pitfalls

- Editing `.env.example` does nothing at runtime — you must `cp .env.example .env` first.
- `dc3/env/dev.env` and the root `.env` serve different purposes. They are not the same file; don't copy one into the
  other.
- Use `DC3_*_PORT` consistently for service published ports. The process internals still use Spring Boot's native names
  such as `SERVER_PORT` and `GRPC_SERVER_PORT`.
- Per-process variables (`POSTGRES_SCHEMA`, `SERVER_PORT`, `TCP_PORT`, and so on) are single-service overrides only —
  don't set them globally.
- The Compose application stack can use a different `NODE_ENV` than local source runs.

## Further Reading

- [Develop Locally from Source](./) — the full path of bringing up the stack, logging in, and running your first device
  end to end
- [Deployment Modes and Image Sources](../guide/usage) — how the whole stack starts and how to use `REGISTRY=cn`
