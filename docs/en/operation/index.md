---
title: Operations Manual
---

<script setup>
import OperationIndexDiagram from '../../.vitepress/theme/components/OperationIndexDiagram.vue'
</script>


# Operations Manual

This page is the entry to the operations manual. It follows one main thread — onboard a device, see data, issue
commands, receive alarms, then let AI drive operations — and points you to the right page for each step. Read it once
and you get a task you can run end to end, not a list of features.

> You are here: you already know the [platform positioning](../introduction/) and
> the [core concepts](../introduction/concepts), and you're ready to get hands-on. If your local environment isn't up
> yet,
> finish the [Quick Start](../quickstart/) first.

## One main thread, five actions

Day-to-day use of the platform is a linear flow. First a driver brings field devices online and you confirm that point
values come back. Then you issue read and write commands and check the receipts. Finally the rule engine turns anomalies
into alarms, and you can optionally hand things off to the Agentic Center for natural-language operations. Each step
feeds the next — no online device means no point values, and without point values commands and alarms have nothing to
act on.

<OperationIndexDiagram lang="en" />

Solid lines are the task order; dashed lines point to the page that covers each action. The first four steps are the
platform's core. The fifth, AI Operations, is optional.

## Recommended path

Work through the pages in this order. Each step builds on the last and gives you something concrete to check.

1. Start with [Core Concepts](../introduction/concepts) to lock in the relationships among drivers, profiles, devices,
   points, and point values. The terminology there is used everywhere else.
2. Follow [Device Onboarding](./device-onboarding) to onboard one device. Run the full chain with `dc3-driver-virtual`
   first, then swap in a real protocol driver.
3. Follow [Data and Commands](./data-commands) to confirm point value collection and history queries, and to issue
   read/write commands and read their receipts.
4. Follow [Alarms and Notifications](./alarms) to set up rules that fire alarms when a device goes offline, a point goes
   out of bounds, or an event is reported.
5. If you want to drive operations in natural language with a large language model,
   read [Agentic Center](../ai/agentic).

### What success looks like

Every step has a signal you can check yourself. Don't move on without it.

::: tip Three success signals

- **Device online**: after onboarding, the device status flips to online (the heartbeat lease is still alive) instead of
  sitting at unknown or offline.
- **Point has a value**: `POST /api/v3/data/point_value/latest` returns the latest value of the device's point, with
  `calValue`/`numValue` and `createTime` populated.
- **Command has a receipt**: after you issue a read/write command, take the returned command ID and query
  `GET /api/v3/data/point_command_history/get_by_command_id`. `status` reaches a terminal state (SUCCESS, FAILED, and so
  on) and `responseValue` holds a result, rather than hanging on pending.
  :::

::: warning A failed write command does not echo back
When a write command fails, `responseValue` in the receipt is `null` and the device-side value is not echoed. While
troubleshooting, trust `status` — don't read "no echo" as "not yet executed".
:::

## Runtime entry points

The platform exposes a single HTTP endpoint to the outside world: the Gateway (default port `8000`, set by
`DC3_GATEWAY_PORT`). It fronts the four centers — Auth, Manager, Data, and Agentic — and handles auth-header extraction
and tenant-context injection in one place. During development you can bypass the gateway and hit a center directly to
debug. In production, traffic always goes through the gateway.

The table below is a reference index. For how to use each entry point, see its own page.

| Entry point                         | Address / Description                                         | Purpose                                                                                                                              |
|-------------------------------------|---------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Gateway API                         | `http://localhost:8000/api/v3/...`                            | The only external HTTP entry point; the curl examples below all hit this                                                             |
| Swagger UI                          | `http://localhost:8000/swagger-ui.html`                       | Browse the gateway-aggregated API in development (usually disabled in production)                                                    |
| Direct-connect debugging per center | Auth `8300` / Manager `8400` / Data `8500` / Agentic `8600`   | When debugging a single center, connect straight to its HTTP port, bypassing the gateway                                             |
| MCP / OAuth entry                   | `POST /mcp`, `GET /.well-known/oauth-protected-resource`      | For AI Agents to reach MCP tools over OAuth 2.1 (both at the gateway root, not under `/api/v3`); see [Agentic Center](../ai/agentic) |
| Web UI                              | The frontend source lives under `dc3-web/` in this repository | The graphical interface; its backend calls go through the Gateway too                                                                |

::: info The Web UI uses the same API entry point
The graphical interface lives under `dc3-web/` in this repository and calls the same set of APIs through the Gateway.
This manual describes operations in terms of API calls and curl. The matching UI entry points map one-to-one.
:::

## From login to a single command: the minimal runnable example

The two snippets below walk the golden path through its minimal closed loop: get a token, then issue a read command.
Login is two steps — fetch the salt, then exchange the salted password for a token (valid for 12 hours). After that,
every protected call must carry the three auth headers. The example values (tenant, username, IDs) are placeholders.
Replace them with your own.

::: code-group

```bash [1. Get salt + exchange for token]
# Get the login salt (public endpoint; use within 5 minutes)
curl -s -X POST http://localhost:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'

# Exchange the salted password for an access token (valid for 12 hours)
curl -s -X POST http://localhost:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"<salt returned in the previous step>","password":"<salted password>"}'
```

```bash [2. Issue a read command]
# Carry the three auth headers and issue a read command for a device's point
curl -s -X POST http://localhost:8000/api/v3/data/point_command/read \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: <tenantId>' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token returned in the previous step>' \
  -d '{"deviceId":"<deviceId>","pointId":"<pointId>"}'
# The return value is the command's ID (String); use it to query the receipt in point_command_history
```

:::

::: warning Commands are asynchronous, with a 10-second default validity
A read or write command returns the command ID right away; the execution result is written back asynchronously. The
command's `expireAt` defaults to `now+10s`. If a driver doesn't consume it before the timeout, it's discarded. So a
returned command ID means "accepted", not "done". To see the actual result, query `point_command_history` by ID.
See [Data and Commands](./data-commands) for the details.
:::

## Further reading

- [Core Concepts](../introduction/concepts) — the relationships among driver, profile, device, and point, plus the
  three-tier configuration. Read it before you operate.
- [Device Onboarding](./device-onboarding) — Step 1: run a complete onboarding once with the virtual driver.
- [Data and Commands](./data-commands) — Steps 2 and 3: point value collection, history queries, and read/write command
  receipts.
- [Alarms and Notifications](./alarms) — Step 4: rule-triggered alarms, notification channels, and the acknowledgment
  flow.
- [Agentic Center](../ai/agentic) — optional: natural-language operations, built-in tools, and MCP integration.
