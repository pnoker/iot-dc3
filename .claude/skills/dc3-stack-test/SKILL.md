---
name: dc3-stack-test
description: "Spin up the IoT DC3 stack (postgres, rabbitmq, gateway, auth, manager, data, agentic) on a clean Docker volume, log in as the default tenant/user, hit a smoke set of HTTP endpoints to verify routing and rename changes, optionally run the iot-dc3-web frontend test suites, then tear everything down. Use when validating changes that touch HTTP paths, gRPC RPCs, Service contracts, BO/VO fields, gateway auth, or any cross-service wiring — and you want a fresh database, not whatever the developer was poking at yesterday."
metadata:
  requires:
    bins: ["docker", "make", "curl", "python3", "md5sum", "pnpm"]
---

# DC3 Stack Test

End-to-end recipe for the **iot-dc3** backend repo (and the sibling **iot-dc3-web** frontend) when you need a fresh,
scripted run from a clean Docker state. Read [`../../../AGENTS.md`](../../../AGENTS.md) first for project conventions;
this skill only documents the test workflow on top.

## When to use

- After any rename across HTTP paths, gRPC RPCs, Service contracts, or VO/BO fields. The frontend
  `tests/api/api-contracts.test.ts` snapshot will reveal mismatches in seconds.
- After a backend change that the developer wants smoke-tested against a real PostgreSQL/RabbitMQ — not against an
  in-memory or stubbed DB.
- When the existing `dc3-postgres` / `dc3-rabbitmq` container has accumulated state from previous sessions and you want
  to start over.
- When the user explicitly asks you to run the stack, run "interface tests", or "test against a real backend".

Do not run this on a developer's hot-loop loop unless asked. Build + start takes ~5 minutes the first time; the cleanup
wipes volumes (database state) by design.

## Prerequisites

| Need                | Detail                                                                                                                                                  |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Compose driver      | The Makefile defaults to `podman compose`. If only Docker is installed, every `make` invocation in this skill **must** pass `COMPOSE='docker compose'`. |
| `.env` at repo root | `cp .env.example .env` once. Compose interpolation reads it.                                                                                            |
| Local Maven         | Not required. The unified Dockerfile builds JARs inside the builder stage.                                                                              |
| Default credentials | tenant=`default`, login=`dc3`, password=`dc3dc3dc3` (md5 → `10e339be1130a90dc1b9ff0332abced6` is what the seeded `dc3_user_password` row stores).       |
| Frontend tests      | `pnpm install` already done in `iot-dc3-web/` (managed by `packageManager` field).                                                                      |

## Phase 1 — Reset Docker

Wipe both containers and the named volumes. The DB volume holds the auth/manager/data schemas; without wiping you will
reuse stale rows.

```bash
docker stop dc3-rabbitmq dc3-postgres dc3-center-auth dc3-center-manager \
            dc3-center-data dc3-center-agentic dc3-gateway 2>/dev/null
docker rm   dc3-rabbitmq dc3-postgres dc3-center-auth dc3-center-manager \
            dc3-center-data dc3-center-agentic dc3-gateway 2>/dev/null
docker volume rm dc3_postgres dc3_rabbitmq dc3_logs 2>/dev/null
# (Older anonymous volumes from previous compose runs may also exist;
# leave the ones you do not recognise.)
```

## Phase 2 — Build the 5 core service images

The base `pnoker/dc3-postgres:2026.5` and `pnoker/dc3-rabbitmq:2026.5` images are already published; only the
application images need (re)building when source changed.

```bash
cd iot-dc3
make build STACK=dev GROUP=core COMPOSE='docker compose'
```

`GROUP=core` resolves to `gateway auth manager data agentic` (see Makefile `GROUP_SERVICES_core`). Maven runs inside the
builder stage with `.mvn/settings-container.xml` (Maven Central, not the Aliyun mirror) — first run pulls dependencies
and takes ~5–8 min; rebuilds with the dependency cache warm take ~1 min.

## Phase 3 — Start DBs, then services

```bash
# Postgres + RabbitMQ
make dev-db COMPOSE='docker compose'

# Wait until both are healthy (each takes ~10 s)
for i in {1..30}; do
  pg=$(docker inspect dc3-postgres --format '{{.State.Health.Status}}' 2>/dev/null)
  rb=$(docker inspect dc3-rabbitmq --format '{{.State.Health.Status}}' 2>/dev/null)
  [[ "$pg $rb" == "healthy healthy" ]] && break
  sleep 2
done

# Gateway + Auth + Manager + Data + Agentic
make up STACK=dev GROUP=core COMPOSE='docker compose'
```

`make up` blocks on each service's healthcheck. When it returns, all 5 containers report healthy. The gateway listens on
`127.0.0.1:8000` by default (`DC3_BIND_HOST` / `DC3_GATEWAY_PORT` in `.env`).

## Phase 4 — Authenticate

The gateway expects three headers, all required:

| Header          | Value                                                                                                             |
|-----------------|-------------------------------------------------------------------------------------------------------------------|
| `X-Auth-Tenant` | tenant code, e.g. `default`                                                                                       |
| `X-Auth-Login`  | login name, e.g. `dc3` (**not** `X-Auth-User` — that one is set by the gateway and contains a JSON user envelope) |
| `X-Auth-Token`  | JSON object `{"salt":"<salt>","token":"<jwt>"}` — **not** the bare JWT                                            |

The token-generation flow is salt → md5(md5(password) + salt):

```bash
salt=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"name":"dc3","tenant":"default"}' \
  http://127.0.0.1:8000/api/v3/auth/token/salt \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['data'])")

md5p=$(echo -n "dc3dc3dc3" | md5sum | cut -d' ' -f1)
hash=$(echo -n "${md5p}${salt}" | md5sum | cut -d' ' -f1)

token=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"name\":\"dc3\",\"tenant\":\"default\",\"salt\":\"$salt\",\"password\":\"$hash\"}" \
  http://127.0.0.1:8000/api/v3/auth/token/generate \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['data'])")

token_hdr="{\"salt\":\"$salt\",\"token\":\"$token\"}"
```

Reusable helper for downstream curl calls:

```bash
api() {
  local label="$1"; shift
  local resp=$(curl -sw "###%{http_code}" \
    -H "Content-Type: application/json" \
    -H "X-Auth-Tenant: default" \
    -H "X-Auth-Login: dc3" \
    -H "X-Auth-Token: $token_hdr" "$@" 2>&1)
  printf "[%s] %s  %s\n" "${resp##*###}" "$label" "${resp%###*}" | head -c 400
  echo
}
```

## Phase 5 — Smoke the renamed endpoints

Drop-in tests that exercise the verb-rename surface (HTTP path + Service body + gRPC RPC). Adjust the list to match what
you just touched.

```bash
api "manager/device/list"          -X POST -d '{"page":{"current":1,"size":10}}' \
    http://127.0.0.1:8000/api/v3/manager/device/list
api "manager/device/get_by_id"     http://127.0.0.1:8000/api/v3/manager/device/get_by_id?id=1
api "manager/driver/list"          -X POST -d '{"page":{"current":1,"size":10}}' \
    http://127.0.0.1:8000/api/v3/manager/driver/list
api "auth/menu/list_tree"          -X POST -d '{}' \
    http://127.0.0.1:8000/api/v3/auth/menu/list_tree
api "auth/role/list_tree"          -X POST -d '{}' \
    http://127.0.0.1:8000/api/v3/auth/role/list_tree
api "auth/resource/list_tree"      -X POST -d '{}' \
    http://127.0.0.1:8000/api/v3/auth/resource/list_tree
api "manager/dashboard/growth"     http://127.0.0.1:8000/api/v3/manager/dashboard/growth?days=7
api "manager/dashboard/topology"   "http://127.0.0.1:8000/api/v3/manager/dashboard/topology?mode=cardinality&range_key=last_24h"
api "data/device/status/list"      -X POST -d '{}' \
    http://127.0.0.1:8000/api/v3/data/device/status/list
api "manager/device/get_count_by_driver_id" \
    http://127.0.0.1:8000/api/v3/manager/device/get_count_by_driver_id?driver_id=1
```

What to expect on a fresh DB:

- **`200`** with empty page records — endpoint routes and serialises correctly.
- **`200`** on `/dashboard/growth` should include the keys `driverDailyCounts`, `deviceDailyCounts`, `pointDailyCounts`,
  `profileDailyCounts` (post-rename names).
- **`404 / Device does not exist`** on `/get_by_id?id=1` — the route works, the row simply is not seeded. **Not a
  regression.**
- **`500 / Application error processing RPC`** on `/data/driver/status/get_device_*_by_driver_id` against a non-existent
  driver id is also a known gRPC-level wrapping of `NotFoundException`; not caused by the rename.

To diagnose unexpected 500s, tail the relevant container:

```bash
docker logs --since 2m dc3-center-manager 2>&1 | grep -B 1 -A 5 ERROR | head -30
docker logs --since 2m dc3-gateway 2>&1 | grep -B 1 -A 3 unauthorized | head
```

## Phase 6 — Frontend test suites

The frontend `tests/api/api-contracts.test.ts` is a snapshot test that records every
`(function, transport, URL, payload)` tuple. After any HTTP-path rename it must be regenerated, otherwise the contract
is unenforced.

```bash
cd ../iot-dc3-web

pnpm exec vue-tsc --noEmit             # type check
pnpm run test                          # unit + api + component (vitest)
# If api-contracts snapshots intentionally moved:
pnpm exec vitest run tests/api -u      # regenerate, then commit the .snap

# Optional, only for HTTP-level smoke against the running backend:
pnpm run test:e2e                      # playwright; needs gateway:8000 healthy
```

Common failure modes after a rename:

- `Snapshot ... mismatched` in `tests/api` → run `vitest -u` and commit the regenerated `.snap`.
- `vi.mock(...)` returns `undefined` for a function → search component tests for the old export name (`getXxxList`,
  `getXxxByYyy`) and rename to the new export name (`listXxx`, `listXxxByYyy`).
- Vue tests blowing up with `import as type` errors → CLAUDE.md item #1 in iot-dc3-web; remember `verbatimModuleSyntax`
  requires `import type` for type-only imports.

## Phase 7 — Tear down

```bash
cd ../iot-dc3
make down STACK=dev COMPOSE='docker compose'
make down STACK=db  COMPOSE='docker compose'
docker volume rm dc3_postgres dc3_rabbitmq dc3_logs 2>/dev/null
docker ps -a | grep dc3-              # should be empty
docker volume ls | grep -E "dc3|postgres|rabbitmq"   # only foreign volumes left
```

Built application images (`pnoker/dc3-{gateway,center-*}:2026.5`) stay on disk so the next `make up` skips the build.
Delete them too with `docker image rm` if disk pressure matters.

## Coverage audit (optional)

Compare backend `@*Mapping` paths against frontend HTTP calls to flag stale callers and uncalled endpoints. The audit
lives in [`./scripts/coverage_audit.py`](./scripts/coverage_audit.py); it walks both repos and prints
`Both / Frontend-only / Backend-only` buckets. Useful sanity check after big renames; not required on every test run.

## Failure recovery

| Symptom                                                  | Likely cause                                                                      | Fix                                                                                     |
|----------------------------------------------------------|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `make ... COMPOSE` complains `podman: command not found` | Default driver is podman                                                          | Add `COMPOSE='docker compose'` to every `make` call                                     |
| `Error: relation "dc3_xxx" does not exist` in agentic    | The pre-built `pnoker/dc3-postgres:2026.5` image does not seed the agentic schema | Skip agentic-only endpoints, or load the agentic SQL by hand into the running container |
| `Invalid request auth header`                            | Sent bare JWT in `X-Auth-Token` instead of `{"salt":"...","token":"..."}`         | See Phase 4 — the gateway parses it as `RequestHeader.TokenHeader` JSON                 |
| `Tenant, user information does not match`                | Wrong `tenant` field on the salt request                                          | Use `tenant=default`, not `dc3`                                                         |
| `pg_isready` never reports healthy                       | Old volume's `pgdata` lock left from a hard kill                                  | `docker volume rm dc3_postgres` and re-run Phase 3                                      |
