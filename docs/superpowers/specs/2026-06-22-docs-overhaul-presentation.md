# IoT DC3 Docs Overhaul — 行业基准与表达模式参考

> 由调研编排于 2026-06-22 产出：分析 ThingsBoard / EMQX / Home Assistant / Node-RED / InfluxData / AWS IoT Core / EdgeX
> 等如何介绍与组织文档，提炼可落地的表达模式。配套设计见 2026-06-22-docs-overhaul-design.md。

## Industry benchmark

**ThingsBoard** — Hero leads with an outcome promise ("working IoT prototype in 10 minutes"); positions for both "Why
ThingsBoard?" newcomers and architects. Getting-started is a **6-step linear journey** (provision device → visualize →
alarms → share with customer) with interactive completion checkboxes and a "what you'll be able to do" success statement
up front. Architecture is split into two explicit deployment narratives (monolithic vs. microservices) with shared
primitives (message queue, actor system, cache) documented as drill-down pages.

**EMQX** — Strongest architecture-visual story: **six industry-specific SVG data-flow diagrams** (device → EMQX →
backend) per vertical. Hero pairs hard scale metrics ("100M connections," "sub-ms latency") with business outcomes ("25%
OEE increase"). Capabilities organized into 8 named pillars. Multiple deployment-tiered CTAs ("Get Started Free" / "Free
Trial").

**Home Assistant** — UI-first, **demo gateway before install** so users explore before committing. Onboarding is
task-sequenced (explore → configure → visualize → automate → voice) with progressive disclosure (basics → advanced →
troubleshooting per section). Beginner-signaling headers ("Basic automations") sit beside advanced reference; "learn by
doing, not reading" ethos.

**Node-RED** — **Card-grid doc hub** with 9 sections explicitly tiered by persona: beginner (Getting Started, FAQ,
Tutorials), practitioner (User Guide, Cookbook), advanced (Creating Nodes, API). Each card has a one-line "what's
inside" subtitle. Single positioning line: "Low-code programming for event-driven applications."

**InfluxData** — Use-case-grounded who-it's-for (server/app/network monitoring, trading analytics) rather than abstract
claims. Concrete performance specs as value-prop ("<10ms last-value queries"). Functional workflow IA (write → query →
process). No diagrams — text-only, which reads thinner.

**AWS IoT Core** — Single canonical **"What is AWS IoT?" page with one connectivity diagram** opening it, then
enumerates protocols and the access-interface matrix (SDK / CLI / API / console). Immediately links to hands-on
tutorials. Reference-dense, neutral tone.

**EdgeX Foundry** — **Problem-solution framing** ("vendor-neutral, plug-and-play interoperability") over diagrams. Five
large action-cards for distinct personas (Docker quick start, build device service, hybrid mode). Architecture emerges
implicitly via service-category navigation (Core / Supporting / Device / Application Services). "Jump in" agency-focused
tone.

## Patterns to adopt for IoT DC3

Prioritized, concrete:

1. **Lead the landing with an outcome promise + a one-sentence positioning line.** Pattern from ThingsBoard/Node-RED:
   replace terse feature tables with "Connect an industrial device and see live telemetry in N minutes" and a single "
   what IoT DC3 is" sentence (e.g. "Multi-protocol industrial IoT platform: driver → device → point → data, with tenant
   isolation"). This is DC3's biggest gap.

2. **Add architecture diagrams — at least three.** (a) A **layered data-flow diagram** (driver/edge → gateway →
   manager/data/center services → storage/UI), AWS-style, on the overview. (b) A **microservices topology** (the actual
   dc3-* services + gRPC facades + Postgres/RabbitMQ), EMQX/ThingsBoard-style. (c) A **domain-model diagram** (
   DO/BO/VO + driver→device→point→profile relationships). The repo's `architecture-diagram` skill can produce these as
   dark-themed SVG.

3. **Build a numbered first-success tutorial with an explicit success statement.** Copy ThingsBoard's 6-step shape:
   provision driver → register device → define points → publish telemetry → see it in the dashboard. State "When you
   finish, you will have X" at the top, and per step show the **exact copy-paste command** (`make up-db && make up-dev`,
   `dc3 auth login`, a sample publish) plus the screenshot of the expected result. DC3 has none of this today.

4. **Add persona paths on the docs hub.** Node-RED/EdgeX card grid: "Evaluating DC3" (overview + demo), "Operator /
   integrator" (connect devices, configure drivers), "Backend developer" (architecture, facade boundaries, BO/VO, Maven
   commands), "Contributor" (build, test gates, commit rules). Each card a one-line subtitle. Map existing
   `iot-dc3/AGENTS.md`, frontend and CLI guides into these lanes.

5. **Provide a runnable demo before deep reading.** Home Assistant's demo-first principle: surface the existing demo
   seed (`iot-dc3-demo.sql`) and `make up` as the very first CTA so evaluators see a populated dashboard before reading
   concepts.

6. **Replace terse reference tables with progressive disclosure.** Keep the dense CRUD-verb / Makefile-variable tables
   as *reference*, but front them with a prose "key concepts" page (driver/device/point/profile, tenant safety, facade
   boundaries) so newcomers aren't dropped into a table cold.

7. **Anchor value-prop in concrete use cases + specs**, EMQX/InfluxData-style: name the industrial scenarios DC3
   targets (Modbus/OPC-UA gathering, edge command dispatch, alarm history) and any real numbers (protocols supported,
   throughput) instead of generic "powerful platform" language.

8. **Per-step verification, matching the global rule.** Each tutorial step should end with "you should see…" so success
   is observable — mirror ThingsBoard's interactive completion tracking conceptually even if non-interactive.

## Anti-patterns to avoid

- **No diagrams / wall-of-table docs** — DC3's current shape; InfluxData shows even a strong product reads thin without
  a single architecture visual.
- **Capability dumps without a who-it's-for** — listing features before stating who the reader is and what they'll
  achieve forces every persona through the same undifferentiated wall.
- **Burying getting-started behind concepts** — don't require reading the full architecture before the first hands-on
  win; link concepts *from* the tutorial, not before it.
- **First success with no defined endpoint** — never end a quickstart without an explicit "what success looks like"
  screenshot/output; an unverifiable tutorial silently fails the reader.
- **Marketing adjectives over specifics** — avoid "powerful/flexible/enterprise-grade" with no protocol list, no number,
  no diagram to back it.
- **One mega-page for mixed audiences** — don't merge evaluator, operator, developer, and contributor content into a
  single linear doc; split into persona lanes as Node-RED/EdgeX do.
