# Environment Variables

This project uses two environment-variable scopes. Keep them separate.

## Files

| File | Reader | Scope |
|------|--------|-------|
| `.env.example` | Template for repository-root `.env`; read by Docker Compose | Compose interpolation for `dc3/docker-compose*.yml`, plus documented local source-run defaults |
| `dc3/env/dev.env` | IDE EnvFile plugins, `docker --env-file`, or `set -a && source` | Local source-run Java processes |
| `dc3/env/dev.env.sh` | Shell via `source dc3/env/dev.env.sh` | Local source-run Java processes |

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

## Alignment Rules

- Compose-only variables stay in `.env.example`.
  Examples: `DC3_IMAGE_REGISTRY`, `DC3_IMAGE_TAG`, `DC3_BIND_HOST`,
  `DC3_*_PORT`, `DC3_LOG_MAX_SIZE`, `DC3_LOG_MAX_FILE`.
- Local source-run variables must match between `.env.example`,
  `dc3/env/dev.env`, and `dc3/env/dev.env.sh`.
  Examples: `POSTGRES_HOST`, `RABBITMQ_HOST`, `CENTER_*_HOST`,
  `OPENAI_*`, `AGENTIC_*`, `POINT_*`, `MQTT_*`.
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
