---
title: Troubleshooting
---

<script setup>
import TroubleshootingDiagram from '../../.vitepress/theme/components/TroubleshootingDiagram.vue'
</script>


# Troubleshooting

This page walks you through the most common startup and connection failures, and helps you tell them apart fast. Each
section follows the same shape: **Symptom → Root cause → Diagnosis** — what you'll see, why it happens, and which log
line or port to check. By the end you'll know whether you're stuck on a dependency, an environment variable, a port, or
authentication.

> If you landed here, you're probably following [Local development from source](../quickstart/) or bringing up the
> container stack and hit a startup or connection error. Use the decision flow below to classify the problem, then jump
> to
> the matching section. Run all commands from the `iot-dc3/` directory unless noted otherwise.

## First, classify the problem: troubleshooting decision flow

Most "won't start / won't connect" cases fall into five buckets: dependencies not ready, environment variables not
loaded, port in use, services started out of order, and authentication chain issues. Working through the diagram below
top to bottom beats guessing one by one. The platform exposes a single HTTP entry point through the gateway (`8000`);
the center services talk to each other over gRPC facades; drivers and the data center are decoupled through RabbitMQ. So
when the dependencies underneath them (PostgreSQL / RabbitMQ) fail to come up, everything above cascades into failure.

<TroubleshootingDiagram lang="en" />

The order of this chain is deliberate. Unloaded variables point every connection at the wrong host. A port in use kills
the process during the bind phase. And the startup order of dependency services decides whether the gRPC facades and
driver registration can succeed at all. Clear the first four gates, and almost everything left traces back to a root
cause in the log keywords.

## Dependencies not ready: can't connect to PostgreSQL / RabbitMQ

**Symptom**: The startup log keeps printing `Connection refused` or `Connection to localhost:35432 refused`, or RabbitMQ
reports `Channel shutdown` / `vhost not found`. The center services start, then exit.

**Root cause**: The PostgreSQL or RabbitMQ container hasn't started yet, its health check hasn't passed, or — when
running from local source — the connection parameters point at the wrong host/port. The key gotcha is that **the host
address differs from the in-container address**: services inside containers reach each other via `dc3-postgres:5432` and
`dc3-rabbitmq:5672`, while a local Java process on the host has to go through the published ports `localhost:35432` and
`localhost:35672`.

**Diagnosis and resolution**: First confirm the dependency stack is running and the published ports match the
application variables.

::: code-group

```bash [make]
make ps STACK=db        # check whether the postgres / rabbitmq containers are healthy
make config STACK=db    # print the effective compose config; verify published ports
make logs STACK=db      # follow dependency container logs (last 200 lines)
```

```bash [podman]
podman ps               # list running containers and their port mappings
podman exec dc3-postgres psql -U dc3 -d dc3 -c "select 1"   # connect directly to verify the database is available
```

:::

Once the containers report `healthy`, running from local source **requires loading the environment variables first**, so
connections point at the dependencies Compose publishes to localhost:

```bash
source dc3/env/dev.env.sh
```

For RabbitMQ specifically, make sure these variables match the actual container (defaults are
in [Environment variables explained](../quickstart/environment)): `RABBITMQ_HOST`, `RABBITMQ_PORT` (`35672` locally,
`5672` in-container), `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VIRTUAL_HOST` (default `dc3`). A vhost
mismatch shows up right away as `Channel shutdown`, immediately after the connection is established.

::: tip Why wait for the health check first
The center services open the database connection pool and RabbitMQ channels very early in startup. If the dependencies
are still initializing (on first start, PostgreSQL runs 7 initdb scripts to create tables and seed data), a service that
starts too early exits on connection failure. Wait until `make ps STACK=db` reports healthy before starting the upper
layers — it saves you a needless round of restarts.
:::

## Environment variables not loaded: connections point at the wrong host

**Symptom**: The dependency containers are clearly running, but local source still can't connect — or connects to an
unexpected host/port. You edited the root `.env`, and it seems to have no effect on the local Java process.

**Root cause**: The root `.env` **serves Docker Compose only**. It is not injected into the local Java process. When you
run a jar locally from an IDE or the command line, you have to load `dc3/env/dev.env(.sh)` explicitly — it points
connections at the dependency ports Compose publishes on localhost. The three files have different roles:

| File                 | Used by              | Purpose                                                                 |
|----------------------|----------------------|-------------------------------------------------------------------------|
| Root `.env`          | Docker Compose       | Image registry, tags, published ports; **not injected into local Java** |
| `dc3/env/dev.env`    | IDE (EnvFile plugin) | Local Java runs, no `export`                                            |
| `dc3/env/dev.env.sh` | Shell                | Local Java runs, with `export`, loaded via `source`                     |

**Resolution**: Before starting from the command line or scripts, run `source dc3/env/dev.env.sh`. In the IDE, attach
`dc3/env/dev.env` through the EnvFile plugin. For the full variable catalog and the host-to-in-container address
mapping, see [Environment variables explained](../quickstart/environment).

## Port in use: the process exits during the bind phase

**Symptom**: Startup fails, and the log contains `Address already in use` or
`Web server failed to start. Port 8400 was already in use`. Ports such as `8000`, `8300`, `8400`, `8500`, `8600`,
`9300`, `9400`, `9500` are already taken.

**Root cause**: The same port is held by a leftover process from a previous run that didn't exit cleanly, or by another
program. These ports map to the gateway HTTP (`8000`), the four center HTTP ports (Auth `8300` / Manager `8400` / Data
`8500` / Agentic `8600`), and the three gRPC ports (Auth `9300` / Manager `9400` / Data `9500`).

**Diagnosis (cross-platform)**: First find which process holds the port and get its PID, then decide whether to kill it
or change the port.

::: code-group

```bash [macOS / Linux (lsof)]
lsof -i :8400 -sTCP:LISTEN    # list the process and PID listening on 8400
kill <PID>                    # confirm it's a leftover process before killing it
```

```bash [Linux (ss)]
ss -ltnp 'sport = :8400'      # show the process (with PID) listening on 8400
```

```bash [Linux (netstat)]
netstat -ltnp | grep ':8400'  # on older systems, netstat finds the PID just as well
```

```powershell [Windows (PowerShell)]
Get-NetTCPConnection -LocalPort 8400 -State Listen | Select-Object OwningProcess
Stop-Process -Id <PID>        # verify before killing the holding process
```

:::

**Resolution**: If the port is held by a program you'd rather not kill, override the port through an environment
variable or the root `.env` so the DC3 service avoids the conflict. Common override variables:

- `DC3_GATEWAY_PORT`, `DC3_AUTH_PORT`, `DC3_MANAGER_PORT`, `DC3_DATA_PORT`, `DC3_AGENTIC_PORT` (Compose published ports)
- `SERVER_PORT`, `GRPC_SERVER_PORT` (override that process's HTTP / gRPC port when running a single service locally)

::: warning When running multiple services locally at once
`SERVER_PORT` / `GRPC_SERVER_PORT` are **per-process** overrides. Set them only when running a single service locally
and you need to avoid the default ports. When running several services in parallel, give each a different value, or
they'll fight over the same port.
:::

## Started out of order: dependency services not yet ready

**Symptom**: Driver registration fails, gRPC calls report `UNAVAILABLE`, or a center service starts and then errors out
because it can't reach a downstream dependency.

**Root cause**: The center services collaborate over gRPC facades, and a driver registers with the management center on
startup and depends on RabbitMQ. Start a downstream service before its upstream is ready, and the connection fails. The
correct startup order is **Gateway → Auth → Manager → Data → Agentic → Driver**.

**Diagnosis and resolution**:

1. Start in the order Gateway → Auth → Manager → Data → Agentic → Driver, and wait for each to become ready before
   starting the next.
2. When running from local source, confirm you've run `source dc3/env/dev.env.sh`.
3. Check the management center and driver logs to confirm the gRPC target addresses (`CENTER_MANAGER_HOST` etc., default
   `localhost`) are reachable.
4. Confirm `dc3.driver.code` is unique and stable — a duplicate code gets registration rejected.

::: danger Don't change the driver code casually
`dc3.driver.code` is the driver's stable routing identifier; the data center uses it to route commands back to the right
driver instance. Once it's live, leave it alone — changing it stops commands from reaching devices already bound to that
driver.
:::

## Authentication failures: 401 / 403 and HMAC

**Symptom**: Calling a protected endpoint through the gateway returns `401` (unauthenticated) or `403` (unauthorized).

**Root cause**: The request doesn't carry a valid token, or the tenant/login/token trio is incomplete. Platform login is
a two-step flow: fetch the salt first, then hash the password with the salt to exchange it for a token. Every subsequent
protected request must carry the three headers `X-Auth-Tenant`, `X-Auth-Login`, and `X-Auth-Token`.

**Diagnosis and resolution**: Log in to get a token first, then call the endpoint with the headers. The happy path uses
the real endpoints below (swap the example values for your environment's actual values):

```bash
# 1) Fetch the salt (public endpoint; use within 5 minutes)
curl -X POST http://localhost:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'

# 2) Hash the password with the salt, then exchange for a token (valid for 12 hours)
curl -X POST http://localhost:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"<salt returned in the previous step>","password":"<password hashed with the salt>"}'

# 3) Call the protected endpoint with the trio of headers
curl -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token returned in the previous step>' \
  -H 'Content-Type: application/json' \
  -d '{"current":1,"size":10}'
```

If the 401s cluster on the gateway-to-backend hop rather than being a user token problem, they're almost certainly about
the HMAC signature. The gateway signs the injected `X-Auth-Principal` with HMAC-SHA256 using `AUTH_HMAC_SECRET`, and the
backend trusts the principal only after the signature verifies. For how to authenticate inside Swagger UI, see
the [API documentation](../development/api-documentation).

::: danger HMAC fails fast in pre/production environments
When the Spring profile (or `spring.env`) is `pre` or `pro`, an empty `AUTH_HMAC_SECRET` — or one still set to the
default weak key `io.github.pnoker.dc3` — makes the service throw `IllegalStateException` at startup and refuse to boot.
This is a deliberate security gate. Before going to `pre`/`pro`, you **must** replace `AUTH_HMAC_SECRET` and
`DC3_SECURITY_KEY` with strong random values, and neither may be logged or hardcoded.
:::

## pre/pro profile won't start locally

**Symptom**: Starting locally with the `pre` / `pro` profile produces connection errors (`UnknownHostException` /
`Connection refused`), or the service fails fast and exits outright.

**Root cause**: `pre` / `pro` target container-stack deployment, so their connection parameters default to container
hostnames rather than localhost. The data source `POSTGRES_HOST` defaults to `dc3-postgres`, `RABBITMQ_HOST` defaults to
`dc3-rabbitmq`, and the gRPC channels use in-container addresses like
`static://${CENTER_AUTH_HOST:dc3-center-auth}:9300`. Locally those hostnames don't resolve, so connections fail. On top
of that comes the HMAC security gate: under `pre` / `pro`, an empty `AUTH_HMAC_SECRET` or the default weak key makes the
service fail fast and refuse to start (see the previous section).

**Resolution**: For local source debugging, always use the `dev` profile — it points connections at the dependency ports
Compose publishes to localhost. Reach for `pre` / `pro` only when you're actually validating the containerized
deployment shape, and make sure the container hostnames resolve and the HMAC / security keys are configured to
production requirements.

## Build and image issues

These aren't runtime connection problems — they belong to the build and packaging phase, grouped here separately.

**Maven build is very slow** — usually parallelism or heap memory isn't taking effect. The repo already ships sensible
defaults: `.mvn/maven.config` contains `-T 1C`, and `.mvn/jvm.config` contains `-Xms512m -Xmx1024m`. If it's still slow,
raise the heap or free up CPU on your machine.

**Wrong Java version** — you see `unsupported class file major version` or Maven Enforcer errors. The project requires
JDK 21. Run the two commands below to confirm **the Java Maven actually uses** is also 21 — the two can differ:

```bash
java -version
mvn -version
```

**Docker image build fails** — usually the Maven packaging inside the image failed, or dependencies weren't built ahead
of time. Confirm Maven passes on the host first, then build the image:

```bash
make package
make build STACK=db
```

**Image source isn't what you expected** — that's the image registry selection. Switch with `REGISTRY`: `global` uses
the default registry (Docker Hub `pnoker`), `cn` uses the mainland China mirror (Aliyun).

```bash
make up STACK=db REGISTRY=global   # default registry
make up STACK=db REGISTRY=cn       # mainland China mirror
```

## Want to debug faster

Run Auth, Manager, and Data in a single JVM by starting `dc3-center-single`, and skip the startup coordination between
multiple services:

```bash
source dc3/env/dev.env.sh
java -jar dc3-center/dc3-center-single/target/dc3-center-single.jar
```

::: info Single process is for local debugging only
Single-JVM mode is handy for quick local validation, but **it is not the production deployment shape** — production is
still the distributed topology of gateway + four centers + drivers.
:::

## Further reading

- [Local development from source](../quickstart/) — the full steps to bring up dependencies locally, load environment
  variables, and get your first driver working end to end
- [Environment variables explained](../quickstart/environment) — the host-to-in-container address mapping and the full
  catalog of port and connection variables
- [Services and topology](../architecture/services) — how the gateway, four centers, and drivers fit together, and the
  dependency relationships behind the startup order
- [API documentation](../development/api-documentation) — how to use Swagger UI and the authentication headers
