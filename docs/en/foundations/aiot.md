---
title: Data Intelligence & AIoT
---

<script setup>
import AiotDiagram from '../../.vitepress/theme/components/AiotDiagram.vue'
</script>


# Data Intelligence & AIoT

Once data is collected and stored in the time-series database, the real value is only beginning: turning a flood of
point values into "what's happening now, what's coming next, and what to do about it." This chapter covers
application-layer intelligence — real-time monitoring, historical analysis, predictive maintenance, anomaly detection —
and how large language models step into IoT operations. By the end you will have a framework for deciding what belongs
to rules versus models, where AI should run, and where IoT DC3 lands this intelligence.

> You are here: you already understand how [time-series data and stream processing](./data-pipeline) aggregate values.
> This chapter makes "decisions" on top of that data — it is the intelligent part of the application layer in the
> four-layer architecture.

## What This Layer Is / Why It Exists

The lower layers solve "bring the physical world into the digital one": perception collects, the network transports, the
platform stores and normalizes. By now you hold a stream of semantically labeled, continuous, queryable point values.
But data alone produces no value — **nobody pays for "the temperature was 73.2°C at 8 a.m. yesterday"; they pay for "the
boiler may overheat in two hours, so lower the load now."** The reason application-layer intelligence exists is to turn
data into actionable judgment.

What this layer does converges into four categories, escalating from "see now → see the past → see the future → no human
watching":

- **Real-time monitoring**: threshold, state, and trend checks on the current value to catch excursions immediately. The
  need is **low latency** — the verdict must be computed the moment a value lands (or on the same stream before it
  lands).
- **Historical analysis**: aggregation, comparison, and correlation over time, answering "why did this device's energy
  use rise this month" or "under which conditions are failures most frequent." The need is **wide scans** and slicing by
  dimension.
- **Predictive maintenance**: learning "what normal looks like" from historical patterns to foresee degradation and
  failure ahead of time, turning "fix after it breaks" into "fix before it does." The need is a **model**, not a fixed
  threshold.
- **Anomaly detection and smart alarming**: spotting behavior that deviates from a normal baseline, and compressing "a
  pile of raw excursions" into "a few alarms with context that someone can act on," so an alarm storm doesn't drown
  operations.

These four are not a flat feature list but an inherent line of tension: the closer to the "see now" end, the more you
need **low latency** and lightweight compute; the closer to the "see the future" end, the more you need **large data
volumes** and **model capability**. In one system, real-time monitoring may run in a millisecond loop at the edge while
prediction and historical analysis run as offline jobs in the cloud — understanding this tension is what tells you where
each kind of intelligence belongs and which means to use.

AIoT (AI of Things) is the umbrella term for this layer: **letting AI take part in the IoT sense–decide–act loop**
rather than only producing after-the-fact reports. Its boundary is not "how fancy a model you used" but "whether the
model's judgment can write back to the physical world" — only when it can issue commands and trigger actions does the
loop truly close.

## Key Technologies and Trade-offs

Application-layer intelligence is not a single algorithm but a pipeline: **collect → analyze/model → decide → act**,
then feed the outcome back into collection to form a loop. The diagram below is the skeleton of this universal pattern —
across industry, energy, buildings, and cities the concrete shape varies, but the loop structure is the same.

<AiotDiagram lang="en" />

Take the loop apart and every hop carries a trade-off:

**Rules or models?** This is the first choice to make, and it is not "models are fancier, so use them everywhere." For *
*deterministic judgments** — fixed thresholds, state machines, simple trends — rules are the bargain: explainable,
auditable, zero training cost, millisecond-fast. Only when "normal" is hard to describe with a threshold (multi-variable
coupling, drift with operating conditions, periodic swings) is a model worth it. Most production systems are **rules as
the floor + models for reinforcement**: rules cover the known hard constraints, models find the anomalies rules can't
express.

**Where does AI run?** The split among device, edge, and cloud is fundamentally a balance of **latency, bandwidth,
compute, and data breadth**:

- **On-device AI**: runs on the device/sensor, making the lightest local judgments (is this vibration abnormal?). Lowest
  latency, no network dependence — but limited compute and model size, and no global view.
- **Edge AI**: runs on the field gateway or edge box, aggregating a cluster of devices for real-time detection and
  preprocessing, compressing a "raw stream" into an "event stream" before sending it up. It balances latency and
  breadth, and can stay autonomous when the link is down.
- **Cloud AI**: the richest in compute and data — suited to training models, cross-device/cross-site global analysis,
  and LLM-driven operations. The cost is latency and bandwidth, so it is unfit for millisecond loops.

A sound architecture is often **train in the cloud, infer at the edge, react on the device**: train on full history in
the cloud, push the model down to the edge for low-latency inference, and let the device handle only the final fast
reaction. This split is not either/or — it is one model carrying different latency responsibilities at different
locations.

**One pattern, many industries.** Industry, energy, buildings, and cities look wildly different, yet the skeleton is the
same loop — **collect → analyze → decide → act**. Rather than piling up industry after industry, it is clearer to see
how this abstraction maps onto any scenario:

- **Collect** varies in its data source — PLC registers in industry, meter readings in energy, temperature/humidity and
  access control in buildings, roadside sensors in cities; what is constant is that all normalize into a stream of
  semantically labeled point values.
- **Analyze** varies in the metric of interest — line yield, load curves, comfort, traffic density; what is constant is
  the same two-tool combination of rules and models.
- **Decide** varies in trigger conditions and thresholds, and **act** varies in its target — shed load, shift peaks,
  adjust fans, time the lights; what is constant is the loop requirement that "judgment must write back to the physical
  world."

In other words, once you understand this universal loop, any industry solution can be placed at a glance: you can see
what it does within "collect–analyze–decide–act" and which hop it is missing. What IoT DC3 provides is the **general
substrate** for this loop, not a finished solution for any one industry.

**LLM + IoT** is a newly added capability layer. It does not replace the analytics stack above; it wraps operations in a
**natural-language interface** and **autonomous orchestration**:

- **Natural-language operations**: "plot the temperature trend of boiler #3 over the past week" replaces writing queries
  and clicking menus, lowering the operating bar.
- **Tool / function calling**: the model doesn't answer from memory — it calls the platform's real APIs to query
  devices, read points, and issue commands, so answers are traceable and actions actually take effect.
- **Retrieval-augmented generation (RAG)**: feed the model device manuals, SOPs, and past work orders as context so its
  advice fits this system's reality instead of being generic.

::: warning "Sounds right" is not "can be trusted"
LLMs hallucinate — confidently. In IoT this is especially dangerous: if a model invents a point value that doesn't exist
or issues the wrong command, the consequences act on the physical world. So the trustworthy approach is: **let the model
read real data only through tools** (not from training memory), and **require human confirmation for high-risk write
actions**.
:::

## Engineering Notes

Turning the trade-offs above into engineering, a few lessons recur:

- **Align latency tiers with the scenario**: millisecond loops (safety interlocks, e-stop) must never depend on a cloud
  round-trip — push them to the edge or device; minute-scale trend forecasts and reports belong in the cloud. First
  ask "how long can this decision wait, worst case," then decide where it runs.
- **Alarms should reduce noise, not add to it**: raw excursions arrive in clusters. Engineering needs **debouncing (only
  count after N sustained seconds), state machines (fire/recover/close to avoid flapping), and aggregation plus
  grading (P0–P3)** to collapse "ten thousand excursions" into "three alarms worth acting on." Otherwise, the more
  alarms, the fewer people read them.
- **Predictive maintenance needs a "normal baseline" first**: a model's value comes from knowing what normal looks like.
  Without enough labeled history, no algorithm can learn the baseline — so the quality of data collection and storage is
  a prerequisite for prediction, not a later optimization.
- **AI cannot bypass permissions and tenant boundaries**: a model acts on behalf of some user/account; what it can see
  and do must never exceed that account's own permissions. Cross-tenant data must be invisible to AI too. In a
  multi-tenant system this is a hard constraint, not an option.
- **Models drift over time and need continual calibration**: device aging, changing operating conditions, and seasonal
  shifts quietly move the "normal baseline," so a model accurate yesterday may false-alarm today. Prediction and anomaly
  detection are not "train once, use forever" — they need retraining and replay evaluation, or false alarms will
  gradually erode operators' trust in the alarms.
- **Write actions must be auditable, reversible, and confirmable**: reads are safe; a wrong write is hard to undo.
  Engineering must wrap writes with **human confirmation, idempotency keys, timeout expiry, and end-to-end auditing** —
  the more autonomous the AI, the thicker this guardrail must be.

## How It Lands in IoT DC3

IoT DC3's application-layer intelligence concentrates on two paths, both built on the point values already normalized by
the lower layers and on unified authentication. Their shared trait: **every AI action ultimately goes through the
platform's real APIs, has principal context injected by the gateway, and is then subject to RBAC permission checks and
tenant isolation at the Auth Center** — the model never gains more permission than its corresponding account.

**Path one: the [Agentic Center](../ai/agentic) (platform-native conversational AI operations).** Built on Spring AI, it
connects an OpenAI-compatible LLM to devices, points, data, and commands. Users ask in natural language; the model calls
built-in platform tools as needed to query metadata, read live values, and — under controlled authorization — trigger
device reads and writes. This is exactly the "tool calling" pattern above in practice: the model reads real data, not
training memory.

::: info There are 10 built-in tools, not 8
The Agentic Center ships **10** `@Tool` tool classes: `TenantTool`, `UserTool`, `DeviceTool`, `DriverTool`,
`ProfileTool`, `PointTool`, `PointValueTool`, `SystemTool`, `CommandTool`, `EventTool`. Early copy mentioned "8"; the
count that matches the code and directory is 10.
:::

::: warning Tool calling is on by default, but can be turned off
Tool calling is controlled by the environment variable `AGENTIC_TOOL_CALLING_ENABLED`, default `true`. Set it to `false`
and the model degrades to pure chat, never touching any device/data interface — configure it this way to allow Q&A only
in a restricted environment. Persistent conversation memory is controlled by `AGENTIC_MEMORY_ENABLED`: the
`.env.example` deployment template sets it to `false` (off by default); if the variable is not provided, the framework's
built-in default is on — go by your actual deployment.
:::

::: danger High-risk writes are never executed directly
The Agentic Center's write tool **never issues a command directly**. It first creates a pending Action (status
`PENDING`, default expiry `now + 10 minutes`) and returns `pendingConfirmation=true`; the write command runs only after
the user confirms by calling `POST /action/confirm` with the `action_id`. This is a separate implementation from the MCP
gateway's risk gating below — see the [Agentic Center](../ai/agentic) for details.
:::

**Path two: [AI Agent / MCP](../ai/mcp) (exposing tools safely to external agents).** The gateway offers a JSON-RPC 2.0
MCP Resource Server at `POST /mcp`; the tool catalog is auto-aggregated from the OpenAPI of the four centers (~330+
tools), with an external agent deciding which to call. It targets the "build your own agent and let the model
orchestrate autonomously" scenario, with stricter constraints than the conversational path:

- **OAuth 2.1 only**: MCP access accepts only short-lived JWTs issued by OAuth 2.1 (default 15-minute validity), with
  mandatory PKCE (S256) for public clients and refresh-token rotation. **There is currently no Personal Access Token (
  PAT)** or other long-lived static-token access method.
- **Three-layer tool-visibility filter**: the tools returned by `tools/list` = principal RBAC permissions ∩ this MCP
  connection's tool whitelist ∩ risk policy (HIGH-risk tools hidden by default, must be explicitly enabled). What an
  agent can see and call is decided by all three layers together.
- **HIGH-risk two-phase confirmation**: a high-risk tool call first returns `CONFIRM_REQUIRED` + a `confirmId`; the
  client must re-call with `confirmId` + an idempotency key, and the server verifies it is not expired, the parameter
  digest matches, and it is consumed once — auditing the whole thing.

::: info MCP resources / prompts are not yet implemented
The MCP protocol's `resources` (resource exposure) and `prompts` (prompt templates) are **not yet implemented** in IoT
DC3 — they are planned; only `tools` is offered today. The `tools/list_changed` change notification is **not
event-pushed** either — the tool catalog is not synced to connected agents in real time; refreshing it requires a manual
call to the admin endpoint `POST /mcp/tool/catalog/refresh` (or a rebuild after API registrations change) (`PT5M` is the
`confirm-ttl` for high-risk two-step confirmation, unrelated to catalog refresh).
:::

**Alarms and notifications** pick up the "anomaly detection and smart alarming" engineering notes above.
DC3's [alarms and notifications](../operation/alarms) use a rule engine for deterministic judgments, `dc3_rule_state` as
a state machine (fire/recover/close) for debouncing, alarm grading (P0–P3) and multi-channel delivery (
email/SMS/webhook) for noise reduction and dispatch — this is the "rules as the floor" half, complementing the "models
for reinforcement" AI paths above: rules cover the hard constraints you can write down, AI helps people understand and
act.

Place all three back on that loop diagram: **collect** and **analyze** are handled by the lower layers and alarm rules,
**decide** lives in the rule engine or the LLM, and **act** is alarm notification or a confirmed command — AI is not a
separate stack but is wired into this existing sense–decide–act–feedback chain.

## Further Reading

- [Time-Series Data & Stream Processing](./data-pipeline) — the input to intelligent analysis: how point values
  aggregate, store, and become queryable
- [IoT Security](./security) — the same auth, tenant isolation, and transport security the AI paths must pass
- [IoT Technology Overview](./) — the four-layer reference architecture, to place the application layer in the whole
- [AI Overview](../ai/) — an overview and selection guide for DC3's two AI access methods
- [Agentic Center](../ai/agentic) — conversational AI operations, 10 built-in tools, high-risk action confirmation
- [AI Agent / MCP](../ai/mcp) — OAuth 2.1 + MCP, exposing tools safely to external agents
- [Alarms & Notifications](../operation/alarms) — rule engine, state-machine debouncing, grading, and multi-channel
  notification
