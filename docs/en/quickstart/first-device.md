---
title: "Your First Device: End to End"
---

<script setup>
import FirstDeviceDiagram from '../../.vitepress/theme/components/FirstDeviceDiagram.vue'
</script>


# Your First Device: End to End

This page walks you through a complete data path with the built-in **virtual driver**: log in to get a token, create a
profile, point, and device, configure the point's attributes, then read live values and send a write command. Each step
has copy-paste commands and a "what you should see" block, so you can follow along directly.

> You are here: you've [started the dependency stack](./) and read the [core concepts](../introduction/concepts). By the
> end of this page you'll have **a device connected through the virtual driver, producing live point values you can
read,
with write commands flowing back to its writable points.**

## What This Path Looks Like

The golden path is a chain of HTTP calls with front-to-back dependencies, all going through the gateway `dc3-gateway` (
`:8000`). The first two steps get a token. The next four build metadata in the Manager Center. The last two read values
and send commands through the Data Center. Keep this map in mind — each step below tells you where you are in it.

<FirstDeviceDiagram lang="en" />

::: info Conventions
All `id` values, tokens, and return values below are **examples**. Your environment generates snowflake IDs (long
strings of digits), so replace them with the real values returned by the previous step. Every write endpoint returns the
unified envelope `{ "ok": true, "code": "...", "message": "...", "data": "..." }`. Note that `add` endpoints **return
only
a success status — they do NOT return the new entity's ID**. When you need an ID, query it back via the matching `list`
endpoint by name (each step below shows the lookup command).
:::

## Step 0: Start the Stack

Bring up the database, message queue, and development stack. These two commands start PostgreSQL + RabbitMQ, then the
gateway, the four centers, and the drivers (the virtual driver starts with the stack).

```bash
make up-db && make up-dev
```

**What you should see**: `podman ps` lists `dc3-postgres`, `dc3-rabbitmq`, `dc3-gateway`,
`dc3-center-auth/manager/data/agentic`, and several `dc3-driver-*` containers as running. The gateway is reachable at
`http://localhost:8000`.

::: tip Point the dc3 CLI at the Gateway First
If you use the `dc3` CLI, tell it the gateway address first (once is enough):
`dc3 config set gateway http://localhost:8000`.
:::

## Steps 1–2: Log In for a Token

Login takes two steps. First, fetch a **salt (use within 5 minutes)** with the tenant + username. Then submit the
**plaintext password** together with the salt to exchange it for an **access token (valid 12 hours)**. After that, every
protected request must carry three auth headers: `X-Auth-Tenant`, `X-Auth-Login`, `X-Auth-Token`.

::: code-group

```bash [curl]
# 1) Fetch the salt
curl -s -X POST http://localhost:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'
# Example response: {"ok":true,"code":"...","message":"...","data":"a1b2c3d4e5"}

# 2) Hash the password with the salt and exchange it for a token (see the auth docs for the hashing algorithm; PASSWORD_HASH here is an example)
curl -s -X POST http://localhost:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"a1b2c3d4e5","password":"<PASSWORD_HASH>"}'
# Example response: {"ok":true,"code":"...","message":"...","data":"<ACCESS_TOKEN>"}
```

```bash [dc3 CLI]
# The CLI wraps fetching the salt, hashing, and exchanging for a token
dc3 auth login --tenant default --username dc3
# Enter the password interactively; the token is saved after login

# Verify
dc3 auth status
dc3 auth token --header   # Print X-Auth-Tenant/X-Auth-Login/X-Auth-Token
```

:::

**What you should see**: `/api/v3/auth/token/salt` returns a non-empty salt; `/api/v3/auth/token/generate` returns a
long token (the `<ACCESS_TOKEN>` above). On the CLI path, `dc3 auth status` shows you logged in.

::: warning Every Subsequent Request Needs the Auth Headers
To keep the curl examples short, the three headers are extracted into variables below. Set them in your shell first,
using the real values from the previous step:

```bash
H_TENANT='X-Auth-Tenant: default'
H_LOGIN='X-Auth-Login: dc3'
H_TOKEN='X-Auth-Token: <ACCESS_TOKEN>'   # example
```

:::

## Step 3: Confirm the Virtual Driver Is Registered

When the virtual driver starts with `make up-dev`, it registers itself with the Manager Center. You need its `driverId`
to create a device, so look it up first.

::: code-group

```bash [curl]
curl -s -X POST http://localhost:8000/api/v3/manager/driver/list \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"page":{"current":1,"size":20}}'
```

```bash [dc3 CLI]
dc3 driver list
```

:::

**What you should see**: the list contains a driver whose `driverName` is `Virtual Driver` (with a space, from
`dc3.driver.name` in `application.yml`). Its `driverCode` is the routing identifier `VirtualDriver`, and the
module/service name is `dc3-driver-virtual` — three different fields. Grab its `id`, called `<DRIVER_ID>` below (
example: `92010100000000001`). The virtual driver is a **driver authoring template**: it exists for testing and
bootstrapping new drivers, and produces data without any real device attached.

## Step 4: Add a Profile

A profile describes what a class of devices can do. Here we create a minimal profile to hold the points. Set
`profileShareFlag` to `TENANT` (shared within the tenant) and `enableFlag` to `ENABLE` (enabled).

::: code-group

```bash [curl]
curl -s -X POST http://localhost:8000/api/v3/manager/profile/add \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"profileName":"Virtual Thermo Profile","profileShareFlag":"TENANT","enableFlag":"ENABLE"}'
# Example response: {"ok":true,"code":"ADD","message":"Added successfully","data":"Added successfully"}
# add returns only a success status, not the ID. When a later step needs profileId, look it up by name:
curl -s -X POST http://localhost:8000/api/v3/manager/profile/list \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" -H 'Content-Type: application/json' \
  -d '{"profileName":"Virtual Thermo Profile","page":{"current":1,"size":1}}'
# Get profileId from records[0].id
```

```bash [dc3 CLI]
dc3 profile create --name "Virtual Thermo Profile"
dc3 profile list --name "Virtual Thermo Profile"   # look up profileId
```

:::

**What you should see**: `add` returns a success status (`data` is a message string, not an ID); use `profile/list`
filtered by `profileName` to look it up, and read the profile ID from `records[0].id` — call it `<PROFILE_ID>` below
(example: `81010100000000001`).

::: tip profileShareFlag Values
`ProfileShareTypeEnum` is `TENANT` / `DRIVER` / `USER` — whether the profile is shared within the tenant, the driver, or
the user.
:::

## Step 5: Add a Point

A point is the data item you collect or write. **Whether it's writable is set by the point's own `rwFlag`.** Here we
create a `READ_WRITE` point so we can send write commands to it later. Set `pointTypeFlag` to `FLOAT`, attach it to the
profile from the previous step, and give it the unit `°C`.

::: code-group

```bash [curl]
curl -s -X POST http://localhost:8000/api/v3/manager/point/add \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "pointName":"Temperature",
        "pointTypeFlag":"FLOAT",
        "rwFlag":"READ_WRITE",
        "profileId":"81010100000000001",
        "valueDecimal":2,
        "unit":"°C",
        "enableFlag":"ENABLE"
      }'
# Example response: {"ok":true,"code":"ADD","message":"Added successfully","data":"Added successfully"}
# add does not return the ID. When a later step needs pointId, look it up by name:
curl -s -X POST http://localhost:8000/api/v3/manager/point/list \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" -H 'Content-Type: application/json' \
  -d '{"pointName":"Temperature","page":{"current":1,"size":1}}'
# Get pointId from records[0].id
```

```bash [dc3 CLI]
dc3 point create --name "Temperature" --profile-id "81010100000000001"
dc3 point list --name "Temperature"   # look up pointId
```

:::

**What you should see**: `add` returns a success status; use `point/list` filtered by `pointName` to look it up, and
read the point ID from `records[0].id` — call it `<POINT_ID>` below (example: `82010100000000001`).

::: tip rwFlag and pointTypeFlag Values
`RwTypeEnum` is `READ_ONLY` / `WRITE_ONLY` / `READ_WRITE`; a write command against a `READ_ONLY` point is rejected.
`PointTypeEnum` has 8 values: `STRING` / `BYTE` / `SHORT` / `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN`. A point can
also carry a conversion (`baseValue` / `multiple`) that linearly maps the raw value to an engineering value.
:::

## Step 6: Add a Device

A device is a concrete instance bound to one profile and one driver. Use the `<DRIVER_ID>` and `<PROFILE_ID>` from above
to create it.

::: code-group

```bash [curl]
curl -s -X POST http://localhost:8000/api/v3/manager/device/add \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "deviceName":"Virtual Thermo Device-01",
        "driverId":"92010100000000001",
        "profileId":"81010100000000001",
        "enableFlag":"ENABLE"
      }'
# Example response: {"ok":true,"code":"ADD","message":"Added successfully","data":"Added successfully"}
# add does not return the ID. When a later step needs deviceId, look it up by name:
curl -s -X POST http://localhost:8000/api/v3/manager/device/list \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" -H 'Content-Type: application/json' \
  -d '{"deviceName":"Virtual Thermo Device-01","page":{"current":1,"size":1}}'
# Get deviceId from records[0].id
```

```bash [dc3 CLI]
dc3 device create --name "Virtual Thermo Device-01" \
  --driver-id "92010100000000001" \
  --profile-id "81010100000000001"
dc3 device list --name "Virtual Thermo Device-01"   # look up deviceId
```

:::

**What you should see**: `add` returns a success status; use `device/list` filtered by `deviceName` to look it up, and
read the device ID from `records[0].id` — call it `<DEVICE_ID>` below (example: `83010100000000001`).

## Step 7: Configure Point Attributes

At startup, the driver declares **which** config items (attributes) it has. This step fills in a **concrete value** for
one of those attributes, for **this point on this device** — a config. This is what actually wires the point into the
driver's collection logic. The `attributeId` comes from an attribute the virtual driver registered (find it in the
driver details or the attribute list), and `configValue` is the value you supply for it.

::: code-group

```bash [curl]
curl -s -X POST http://localhost:8000/api/v3/manager/point_attribute_config/add \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "attributeId":"91010100000000001",
        "deviceId":"83010100000000001",
        "pointId":"82010100000000001",
        "configValue":"25.0",
        "enableFlag":"ENABLE"
      }'
# Example response: {"ok":true,"code":"ADD","message":"Added successfully","data":"Added successfully"}
# add does not return the ID. The config takes effect immediately upon write, no separate lookup needed.
```

```bash [dc3 CLI]
# The CLI has no standalone attribute-config subcommand yet; use the curl above
```

:::

**What you should see**: `add` returns a success status (no ID). Once the config takes effect, the virtual driver starts
producing values for that point.

::: info Where attributeId Comes From
`attributeId` refers to a point attribute (`PointAttribute`) the virtual driver registered in the Manager Center.
Different drivers declare different attributes — this is the "protocol-layer Attribute vs. instance-layer Config" split
from the [core concepts](../introduction/concepts). The example ID is a placeholder; use the attribute ID actually
registered by the virtual driver in your environment.
:::

## Step 8: Read Live Point Values

Once values flow, read the latest point values from the Data Center. `/point_value/latest` filters by `deviceId` /
`pointId` at the top level, takes pagination in the nested `page` object (`current` / `size`), and returns
`Page<PointValueVO>`.

::: code-group

```bash [curl]
curl -s -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":"83010100000000001","pointId":"82010100000000001","page":{"current":1,"size":10}}'
# Example response (PointValueVO shape):
# {"ok":true,"data":{"records":[
#   {"deviceId":"83010100000000001","pointId":"82010100000000001",
#    "rawValue":"25.0","calValue":"25.0","numValue":25.0,
#    "hasLatestValue":true,"createTime":"2026-06-22T08:30:00","operateTime":"2026-06-22T08:30:00"}
# ],"total":1,"current":1,"size":10}}
```

```bash [dc3 CLI]
dc3 point read 82010100000000001
```

:::

**What you should see**: at least one `PointValueVO` in `records`, with `rawValue` (the raw value), `calValue` (the
engineering value, as a string), `numValue` (the numeric projection, nullable), and the collection time `createTime`.
The value refreshes as the virtual driver keeps running.

## Step 9: Issue a Write Command

Finally, send a write command to this writable point. `/point_command/write` takes `deviceId` / `pointId` / `value` and
**returns a command ID (`commandId`) right away** — which means "the command was accepted," not "it ran successfully."
To get the result, take the command ID and **poll** the receipt endpoint `/point_command_history/get_by_command_id` (
with that `commandId`).

::: code-group

```bash [curl]
# 1) Send the write command and get a commandId immediately
curl -s -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":"83010100000000001","pointId":"82010100000000001","value":"26.5"}'
# Example response: {"ok":true,"code":"...","data":"cmd_20260622_a1b2c3d4"}

# 2) Poll the receipt with the commandId
curl -s -X GET 'http://localhost:8000/api/v3/data/point_command_history/get_by_command_id?commandId=cmd_20260622_a1b2c3d4' \
  -H "$H_TENANT" -H "$H_LOGIN" -H "$H_TOKEN"
# Example response (PointCommandHistoryVO shape):
# {"ok":true,"data":{
#   "commandId":"cmd_20260622_a1b2c3d4","deviceId":"83010100000000001","pointId":"82010100000000001",
#   "requestValue":"26.5","responseValue":"...","status":"...","finishTime":"2026-06-22T08:31:02"}}
```

```bash [dc3 CLI]
# Send the write
dc3 point write 82010100000000001 --device-id 83010100000000001 --value 26.5
# Poll the receipt
dc3 command history cmd_20260622_a1b2c3d4
```

:::

**What you should see**: the write returns a `commandId` right away; poll the receipt until `status` reaches a terminal
state. That closes the loop — you've run the full bidirectional path of "read value + write command."

::: danger Write Command Semantics: Asynchronous, Poll-Required, Failure Does Not Echo the Value

- Write commands are **asynchronous**: `/point_command/write` returns a `commandId` immediately and does **not** wait
  for the device to finish. Poll for the result with that ID at `/point_command_history/get_by_command_id`.
- Commands have a **TTL**: `PointCommandDTO.expireAt` defaults to `now + 10s`. A command not executed by then is
  expired.
- **On failure, the written value is not echoed back**: when the receipt shows a failure, it carries no "value that was
  written" — so don't read "got a commandId" as "the write succeeded."
- Only points whose `rwFlag` includes write permission (`WRITE_ONLY` / `READ_WRITE`) accept write commands.
  :::

## Further Reading

- [Device Onboarding](../operation/device-onboarding) — expand this minimal path into the full onboarding flow for a
  real driver
- [Data and Commands](../operation/data-commands) — the full mechanics of collection persistence and read/write
  commands, plus receipt semantics, across the two planes
- [CLI Guide](../automation/cli) — the complete `dc3` CLI command surface, with scripting and AI-integration usage
