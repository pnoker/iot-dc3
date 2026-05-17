# Environment Variables

This project uses two environment-variable scopes. Keep them separate.

## Files

| File                 | Reader                                                          | Scope                                                                                          |
|----------------------|-----------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `.env.example`       | Template for repository-root `.env`; read by Docker Compose     | Compose interpolation for `dc3/docker-compose*.yml`, plus documented local source-run defaults |
| `dc3/env/dev.env`    | IDE EnvFile plugins, `docker --env-file`, or `set -a && source` | Local source-run Java processes                                                                |
| `dc3/env/dev.env.sh` | Shell via `source dc3/env/dev.env.sh`                           | Local source-run Java processes                                                                |

## Compose Usage

Create a local `.env` from the template:

```bash
cp .env.example .env
```

Run Compose commands from the repository root so Compose can discover `.env`:

```bash
make dev-all
make app-all REGISTRY=aliyun
podman compose -f dc3/docker-compose-dev.yml config
```

The root `.env` is primarily used for Compose interpolation, for example:

```yaml
image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-gateway:${DC3_IMAGE_TAG:-2026.5}
ports:
  - "${DC3_BIND_HOST:-127.0.0.1}:${DC3_GATEWAY_PORT:-8000}:8000"
```

Compose does not inject every `.env` variable into every container. A variable is
passed into a container only when a compose file references it under
`environment`, `env_file`, or another container setting.

## Local Source-Run Usage

For local Java processes started from an IDE or the command line, use:

```bash
source dc3/env/dev.env.sh
```

These values point application code at dependencies published on localhost:

```env
POSTGRES_HOST=localhost
POSTGRES_PORT=35432
RABBITMQ_HOST=localhost
RABBITMQ_PORT=35672
CENTER_AUTH_HOST=localhost
NODE_ENV=dev
DC3_FACADE_MODE=grpc
```

`dc3/env/dev.env` contains the same source-run variables without `export`, which
is convenient for IDE run configurations.

## JetBrains IDEA Usage

`dc3/env/dev.env` is kept for IDE usage and is not a duplicate of root `.env`.
It intentionally contains only variables needed by local Java processes, while
root `.env` also contains Compose-only image, port, logging, and observability
interpolation variables.

Recommended IDEA setup:

1. Install the JetBrains EnvFile plugin.
2. Open the run configuration for the service you want to start.
3. Enable EnvFile for that run configuration.
4. Add `dc3/env/dev.env`.
5. Add only service-specific overrides in the same run configuration when
   needed, for example `SERVER_PORT`, `GRPC_SERVER_PORT`, `TCP_PORT`,
   `UDP_PORT`, or `POSTGRES_SCHEMA`.

If you do not use the EnvFile plugin, open the run configuration, edit
`Environment variables`, and paste the key-value pairs from `dc3/env/dev.env`.

Do not point IDEA at `.env.example`; it is a template and is not loaded at
runtime. Do not use root `.env` as the default IDEA file unless you explicitly
want Compose-only variables to appear in the local Java process environment.

## Variable Reference

### Shared Compose Defaults

| Variable             | Scope           | Meaning                                                                                                 |
|----------------------|-----------------|---------------------------------------------------------------------------------------------------------|
| `DC3_IMAGE_REGISTRY` | Compose         | Image registry namespace used by all DC3 images. Use `registry.cn-beijing.aliyuncs.com/dc3` for Aliyun. |
| `DC3_IMAGE_TAG`      | Compose         | Image tag shared by application, database, EMQX, and observability images.                              |
| `DC3_LOG_MAX_SIZE`   | Compose         | Maximum size of a single container log file before rotation.                                            |
| `DC3_LOG_MAX_FILE`   | Compose         | Number of rotated container log files to keep.                                                          |
| `DC3_BIND_HOST`      | Compose         | Host address for published ports. Keep `127.0.0.1` for local-only access; use `0.0.0.0` for LAN access. |
| `APM_AGENT_ENABLE`   | Compose/runtime | Enables or disables the Java APM agent in application containers.                                       |

### Database and RabbitMQ

| Variable                       | Scope           | Meaning                                                                                                          |
|--------------------------------|-----------------|------------------------------------------------------------------------------------------------------------------|
| `POSTGRES_HOST`                | Runtime         | PostgreSQL host used by local Java processes and containers that pass it through.                                |
| `POSTGRES_PORT`                | Runtime         | PostgreSQL port seen by the Java process. For local source runs this is usually the published host port `35432`. |
| `POSTGRES_USERNAME`            | Runtime/Compose | PostgreSQL username used by applications and compose health checks.                                              |
| `POSTGRES_PASSWORD`            | Runtime         | PostgreSQL password used by applications.                                                                        |
| `POSTGRES_DB`                  | Runtime/Compose | PostgreSQL database name used by applications and compose health checks.                                         |
| `POSTGRES_SCHEMA`              | Per-process     | Optional schema override for one service process, for example `dc3_manager` or `dc3_data`.                       |
| `DC3_POSTGRES_PORT`            | Compose         | Published host port for the PostgreSQL container.                                                                |
| `RABBITMQ_VIRTUAL_HOST`        | Runtime         | RabbitMQ virtual host used by Spring AMQP.                                                                       |
| `RABBITMQ_HOST`                | Runtime         | RabbitMQ host used by local Java processes and containers that pass it through.                                  |
| `RABBITMQ_PORT`                | Runtime         | RabbitMQ AMQP port seen by the Java process. For local source runs this is usually `35672`.                      |
| `RABBITMQ_USERNAME`            | Runtime         | RabbitMQ username used by applications.                                                                          |
| `RABBITMQ_PASSWORD`            | Runtime         | RabbitMQ password used by applications.                                                                          |
| `DC3_RABBITMQ_TLS_PORT`        | Compose         | Published host port for RabbitMQ TLS.                                                                            |
| `DC3_RABBITMQ_PORT`            | Compose         | Published host port for RabbitMQ AMQP.                                                                           |
| `DC3_RABBITMQ_MANAGEMENT_PORT` | Compose         | Published host port for the RabbitMQ management UI.                                                              |

### Application Ports and Runtime Mode

| Variable                         | Scope       | Meaning                                                                                                                                                               |
|----------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DC3_WEB_HTTP_PORT`              | Compose     | Published host HTTP port for `dc3-web` (the nginx frontend).                                                                                                          |
| `DC3_WEB_HTTPS_PORT`             | Compose     | Published host HTTPS port for `dc3-web`.                                                                                                                              |
| `DC3_WEB_VERSION`                | Compose     | Image tag for the `dc3-web` frontend (independent of `DC3_IMAGE_TAG` since the UI ships on its own cadence). Defaults to `latest`.                                    |
| `APP_API_HOST`                   | Runtime     | Backend gateway hostname seen by the `dc3-web` nginx container. Defaults to the `dc3-gateway` compose alias.                                                          |
| `APP_API_PORT`                   | Runtime     | Backend gateway port seen by the `dc3-web` nginx container. Defaults to `8000`.                                                                                       |
| `DC3_GATEWAY_PORT`               | Compose     | Published host HTTP port for `dc3-gateway`. Only effective in `docker-compose-dev.yml`; in production `docker-compose.yml` the gateway is reached via `dc3-web` only. |
| `DC3_AUTH_PORT`                  | Compose     | Published host HTTP port for `dc3-center-auth`.                                                                                                                       |
| `DC3_AUTH_GRPC_PORT`             | Compose     | Published host gRPC port for `dc3-center-auth`.                                                                                                                       |
| `DC3_MANAGER_PORT`               | Compose     | Published host HTTP port for `dc3-center-manager`.                                                                                                                    |
| `DC3_MANAGER_GRPC_PORT`          | Compose     | Published host gRPC port for `dc3-center-manager`.                                                                                                                    |
| `DC3_DATA_PORT`                  | Compose     | Published host HTTP port for `dc3-center-data`.                                                                                                                       |
| `DC3_DATA_GRPC_PORT`             | Compose     | Published host gRPC port for `dc3-center-data`.                                                                                                                       |
| `DC3_AGENTIC_PORT`               | Compose     | Published host HTTP port for `dc3-center-agentic`.                                                                                                                    |
| `DC3_LISTENING_VIRTUAL_TCP_PORT` | Compose     | Published host TCP port for the listening virtual driver.                                                                                                             |
| `DC3_LISTENING_VIRTUAL_UDP_PORT` | Compose     | Published host UDP port for the listening virtual driver.                                                                                                             |
| `SERVER_PORT`                    | Per-process | Spring Boot HTTP port override for one local service process.                                                                                                         |
| `GRPC_SERVER_PORT`               | Per-process | Spring gRPC server port override for one local center service process.                                                                                                |
| `TCP_PORT`                       | Per-process | Internal TCP listening port for one listening virtual driver process.                                                                                                 |
| `UDP_PORT`                       | Per-process | Internal UDP listening port for one listening virtual driver process.                                                                                                 |
| `NODE_ENV`                       | Runtime     | Active runtime profile group, usually `dev` for local source runs and `test` in compose app stacks.                                                                   |
| `DC3_FACADE_MODE`                | Runtime     | Cross-service facade transport mode. `grpc` uses remote gRPC calls; local facade modules can override this for single-process modes.                                  |

### Center Service Discovery and Gateway Routes

| Variable                       | Scope       | Meaning                                                                       |
|--------------------------------|-------------|-------------------------------------------------------------------------------|
| `CENTER_AUTH_HOST`             | Runtime     | Hostname used by local processes to reach Auth Center gRPC/HTTP endpoints.    |
| `CENTER_MANAGER_HOST`          | Runtime     | Hostname used by local processes to reach Manager Center gRPC/HTTP endpoints. |
| `CENTER_DATA_HOST`             | Runtime     | Hostname used by local processes to reach Data Center gRPC/HTTP endpoints.    |
| `CENTER_AGENTIC_HOST`          | Runtime     | Hostname used by local processes to reach Agentic Center HTTP endpoints.      |
| `GATEWAY_ROUTE_AUTH_TOKEN_URI` | Per-process | Optional gateway route override for Auth token endpoints.                     |
| `GATEWAY_ROUTE_AUTH_URI`       | Per-process | Optional gateway route override for Auth service endpoints.                   |
| `GATEWAY_ROUTE_MANAGER_URI`    | Per-process | Optional gateway route override for Manager service endpoints.                |
| `GATEWAY_ROUTE_DATA_URI`       | Per-process | Optional gateway route override for Data service endpoints.                   |
| `GATEWAY_ROUTE_AGENTIC_URI`    | Per-process | Optional gateway route override for Agentic service endpoints.                |

### Agentic and OpenAI-Compatible API

| Variable                              | Scope   | Meaning                                                                                                                     |
|---------------------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL`    | Runtime | Fallback OpenAI-compatible API base URL used only when no database model/provider matches.                                  |
| `AGENTIC_FALLBACK_OPENAI_API_KEY`     | Runtime | Fallback API key. Normal Agentic provider credentials are stored in `dc3_model_provider`.                                   |
| `AGENTIC_FALLBACK_OPENAI_MODEL`       | Runtime | Fallback chat model name exposed only when no enabled `dc3_model_config` exists.                                            |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | Runtime | Fallback sampling temperature for chat completions.                                                                         |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS`  | Runtime | Fallback maximum output token budget for one model response.                                                                |
| `AGENTIC_MEMORY_SCHEMA_INIT`          | Runtime | Spring AI JDBC memory schema initialization mode. Keep `never`; DC3 pre-creates `dc3_chat_memory`.                          |
| `AGENTIC_MEMORY_ENABLED`              | Runtime | Whether Agentic Center should include persisted conversation memory when preparing chat requests.                           |
| `AGENTIC_TOOL_CALLING_ENABLED`        | Runtime | Whether Agentic Center exposes provider-native tool calling. Default is enabled; disable only for provider troubleshooting. |
| `AGENTIC_MEMORY_MAX_MESSAGES`         | Runtime | Maximum chat messages retained per conversation window.                                                                     |
| `AGENTIC_ATTACHMENT_STORAGE_PATH`     | Runtime | Directory used by Agentic Center to store uploaded attachment files.                                                        |

### MQTT and Point Processing

| Variable               | Scope   | Meaning                                                                                                                                 |
|------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `MQTT_BROKER_HOST`     | Runtime | MQTT broker host used by MQTT-enabled services and drivers.                                                                             |
| `MQTT_BROKER_PORT`     | Runtime | MQTT broker port seen by the Java process. For local source runs this is usually the EMQX published port `31883`.                       |
| `MQTT_USERNAME`        | Runtime | MQTT username.                                                                                                                          |
| `MQTT_PASSWORD`        | Runtime | MQTT password.                                                                                                                          |
| `MQTT_BATCH_SPEED`     | Runtime | MQTT message speed threshold. Below the threshold, messages are handled individually; above it, they are buffered for batch processing. |
| `MQTT_BATCH_INTERVAL`  | Runtime | MQTT batch scheduler interval in seconds.                                                                                               |
| `POINT_BATCH_SPEED`    | Runtime | Point-value processing speed threshold for switching between direct and batch handling.                                                 |
| `POINT_BATCH_INTERVAL` | Runtime | Point-value batch scheduler interval in seconds.                                                                                        |

### gRPC Facade

| Variable                      | Scope   | Meaning                                                                                        |
|-------------------------------|---------|------------------------------------------------------------------------------------------------|
| `DC3_FACADE_GRPC_DEADLINE_MS` | Runtime | Per-request gRPC facade deadline in milliseconds. Set `0` to disable the client-side deadline. |

### Auth and HMAC Signing

| Variable           | Scope   | Meaning                                                                                                                                                                                                                                    |
|--------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AUTH_HMAC_SECRET` | Runtime | Shared HMAC-SHA256 secret for `X-Auth-User` header signing between the gateway and backend services. When empty, signing is disabled and backend services trust the header without verification. Set a strong random string in production. |

### Optional and Observability Stacks

| Variable                  | Scope   | Meaning                                        |
|---------------------------|---------|------------------------------------------------|
| `DC3_EMQX_WS_PORT`        | Compose | Published host port for EMQX WebSocket.        |
| `DC3_EMQX_WSS_PORT`       | Compose | Published host port for EMQX secure WebSocket. |
| `DC3_EMQX_MQTT_PORT`      | Compose | Published host port for EMQX MQTT.             |
| `DC3_EMQX_MQTTS_PORT`     | Compose | Published host port for EMQX MQTTS.            |
| `DC3_EMQX_DASHBOARD_PORT` | Compose | Published host port for the EMQX dashboard.    |
| `GF_SERVER_ROOT_URL`      | Compose | External root URL used by Grafana.             |
| `DC3_GRAFANA_PORT`        | Compose | Published host port for Grafana.               |
| `DC3_KIBANA_PORT`         | Compose | Published host port for Kibana.                |
| `DC3_ES_JAVA_OPTS`        | Compose | JVM heap options for Elasticsearch.            |
| `DC3_LS_JAVA_OPTS`        | Compose | JVM heap options for Logstash.                 |

## Alignment Rules

- Compose-only variables stay in `.env.example`.
  Examples: `DC3_IMAGE_REGISTRY`, `DC3_IMAGE_TAG`, `DC3_BIND_HOST`,
  `DC3_*_PORT`, `DC3_LOG_MAX_SIZE`, `DC3_LOG_MAX_FILE`.
- Local source-run variables must match between `.env.example`,
  `dc3/env/dev.env`, and `dc3/env/dev.env.sh`.
  Examples: `POSTGRES_HOST`, `RABBITMQ_HOST`, `CENTER_*_HOST`,
  `AGENTIC_FALLBACK_OPENAI_*`, `AGENTIC_*`, `DC3_FACADE_GRPC_*`, `AUTH_HMAC_SECRET`,
  `POINT_*`, `MQTT_*`.
- Per-process variables are documented as commented examples only.
  Examples: `SERVER_PORT`, `GRPC_SERVER_PORT`, `TCP_PORT`, `UDP_PORT`,
  `POSTGRES_SCHEMA`.
- Service-specific host port variables use the `DC3_*_PORT` prefix and describe
  published host ports. Internal Spring Boot variables keep their native names,
  such as `SERVER_PORT` and `GRPC_SERVER_PORT`.

## Common Pitfalls

- Editing `.env.example` has no runtime effect. Copy it to `.env` first.
- `dc3/env/dev.env` looks similar to part of root `.env.example`, but it serves
  a different reader: IDE/source-run processes instead of Compose
  interpolation.
- Setting `POSTGRES_HOST=localhost` in root `.env` does not automatically change
  every app container; the compose service must pass the variable into the
  container.
- The app compose stacks intentionally set `NODE_ENV=test` for containers.
  `NODE_ENV=dev` in `dc3/env/dev.env(.sh)` is for local source runs.
- `DC3_LISTENING_VIRTUAL_TCP_PORT` and `DC3_LISTENING_VIRTUAL_UDP_PORT` are
  host-published ports. `TCP_PORT` and `UDP_PORT` are per-process internal
  application ports for the listening virtual driver.
