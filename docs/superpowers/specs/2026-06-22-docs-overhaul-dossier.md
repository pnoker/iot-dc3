# IoT DC3 Docs Overhaul — Source-of-Truth Dossier

> 由只读研究编排（13 个 agent）于 2026-06-22 产出，作为全站文档重构的事实底座。每条事实带 `file:line` 引用；落地每页文档前应据此核对源码（见末尾
> Gaps）。配套设计见 `2026-06-22-docs-overhaul-design.md`。

## A. Product truth (PM lens)

**What it is (honest one-paragraph):** IoT DC3 is a distributed, open-source (AGPL 3.0) industrial IoT platform that
connects heterogeneous devices through 28 multi-protocol drivers, normalizes raw device registers into
semantically-labeled `PointValue` data, and closes the loop by letting LLMs read that data and dispatch commands back to
devices via Spring AI Tool Calling. It runs on Java 21 / Spring Boot 4 / Spring Cloud 2025 / Spring AI 2.0, with
PostgreSQL (+TimescaleDB/AGE/pgvector) for persistence, RabbitMQ for async messaging, and gRPC for inter-service
coordination (`README.md:232-241`, `README.md:73-84`).

**Problem solved:** Two stated gaps — "data stuck in devices that AI cannot consume" and "AI can only observe, not act."
Traditional IoT platforms offer device connectivity *or* AI analysis but not closed-loop autonomous operation (
`docs/superpowers/strategy/ai-positioning.md:9-17`).

**Personas:** Industrial IoT platform builders; smart-factory / equipment-monitoring teams; energy / agriculture /
city-infrastructure operators; teams needing drivers for heterogeneous devices; Spring-ecosystem developers; teams
exploring AI-assisted operations (`README.ai.md:19-28`).

**Top scenarios:** Smart Factory (line monitoring, predictive maintenance, OEE); Energy Monitoring (remote metering,
anomaly alarms); Smart Agriculture (greenhouse monitoring, irrigation control, yield forecasting); Smart City (
streetlight, environmental, municipal ops) (`README.md:260-283`).

**Capability pillars:**

1. Multi-protocol device access — 28 drivers across industrial / IoT / DB-bridge / basic-comms / simulation (
   `README.md:73-84`).
2. AI-ready data layer — point templates map raw registers to labeled `PointValue` with unit normalization, timestamps,
   tenant isolation (`docs/superpowers/strategy/ai-positioning.md:38-44`).
3. Closed-loop agentic execution — AI analysis → decision → device command → result feedback via Spring AI `@Tool` (
   `docs/superpowers/strategy/ai-positioning.md:58-79`).
4. Multi-tenant by design — `TenantOwned` + `requireTenant()` at every layer (`AGENTS.md:123`).

**Real differentiators (per `ai-positioning.md:82-93`):** (1) AI integrated natively via Spring AI, not as an external
service; (2) breadth of 28 protocol drivers incl. DB bridging; (3) structured AI-ready output (`PointValue` with
semantic labels); (4) closed-loop command execution from LLM decisions back to devices; (5) fully open-source stack, no
proprietary core; (6) multi-tenant by design. **Caveat:** multi-protocol support alone is NOT the differentiator (many
platforms have it); the loop + breadth combination is. Known gaps vs commercial/`iot-communication`: no RTSP/H264 video
streaming.

---

## B. Architecture & flows truth (architect lens)

### B.1 Topology / services

- **Six core deployable services:** Gateway (HTTP router), Auth Center, Manager Center, Data Center, Agentic Center,
  protocol Drivers (`iot-dc3/AGENTS.md:14-21`, `docker-compose.yml:49-368`).
- **Ports:** Gateway HTTP 8000 (sole HTTP entrypoint, `DC3_GATEWAY_PORT`); Auth 8300/gRPC 9300; Manager 8400/9400; Data
  8500/9500; Agentic 8600; Single all-in-one 8100/9100. All non-gateway HTTP ports are internal-only (
  `dc3-gateway/.../application.yml:20`, `.env.example`, `docker-compose.yml:110-217`,
  `dc3-center-single/.../application.yml:19-40`).
- **Startup order (enforced by `depends_on` health):** PostgreSQL + RabbitMQ → Auth (no deps) → Manager (needs Auth) →
  Data (needs Auth+Manager) → Agentic (needs Auth+Manager+Data) → Gateway (needs Auth/Manager/Data/Agentic) → Drivers (
  need Manager). Treat the `depends_on` health edges as authoritative: centers become healthy, then gateway.
- **Facade mode** (`dc3.facade.mode`): `grpc` (default, distributed) vs `local` (in-process, single-service). This is a
  **deployment-topology** choice, NOT a wire-protocol choice. Contracts in `dc3-common-facade-api`; gRPC impls in
  `dc3-common-facade-grpc`; in-process in `dc3-common-facade-local-*` (`GrpcFacadeAutoConfiguration.java:34-38`,
  `LocalFacadeManagerAutoConfiguration.java:42-43`, `AGENTS.md:50-55`).
- **Dependencies:** PostgreSQL (5432 internal / 35432 host), RabbitMQ (5672 / 35672, MQTT 1883). Optional stack: EMQX,
  ELK, Prometheus, Grafana (`docker-compose-db.yml:25-88`, `docker-compose-optional.yml`).
- **Health:** all services expose `/actuator/health/readiness` (`docker-compose.yml:99,120,144,170,208`).
- **Stack versions:** Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1, protobuf 4.35.0, Spring gRPC 1.0.3,
  MyBatis-Plus 3.5.16 (Snowflake IDs) (`pom.xml:70-93`).

### B.2 Auth / tenant / RBAC

- **Gateway auth pipeline:** `AuthenticGatewayFilter` resolves tenant/user from `X-Auth-Tenant`/`X-Auth-Login`/
  `X-Auth-Token`, computes `X-Auth-Principal` JSON + HMAC-SHA256 signature `X-Auth-Sign`, forwards to backends (
  `AuthenticGatewayFilter.java:61-95`, `RequestConstant.java:55-79`, `HmacAuthSigner.java:49-93`).
- **HMAC secret** (`dc3.auth.hmac.secret` / `AUTH_HMAC_SECRET`): warns if blank in dev/test, **fails fast in pre/pro** —
  hard production requirement (`HmacAuthProperties.java:36-44`, `HmacAuthSigner.java:57-71`).
- **Identity model:** `dc3_principal` is the root identity (USER / SERVICE_ACCOUNT / SYSTEM); credentials map to
  `principal_id`, not `user_id`. Tenant membership explicit via `dc3_tenant_membership` — one USER can belong to
  multiple tenants; SERVICE_ACCOUNT is single-tenant (`identity-principal-service-account.md:113-149,186-279`).
- **Token flow:** validates tenant → credential → membership → password → expiry → password-change flag; JWT bound to
  `principal_id`+`tenant_id`. Logout denylist keyed `(tenantCode:loginName)` in Caffeine (
  `TokenServiceImpl.java:79-111`, `TokenDenylistCache.java:64-92`).
- **RBAC:** `dc3_role_principal_bind` (tenant-scoped) → `dc3_role_resource_bind` (global) → `dc3_resource`; permissions
  cached per `(tenantId:principalId)` for 5 min; **fails closed** (empty authorities, → 403) on lookup failure (
  `AuthPermissionProvider.java:50-69`, `GatewayJwtConverter.java:61-123`, `PermissionServer.java:48-70`).
- **Tenant enforcement:** `BaseController.requireTenant()` returns 404 on cross-tenant ID lookups; `filterTenant()`
  strips non-owned in bulk (`BaseController.java:79-97`). MyBatis-Plus tenant-line handler adds `WHERE tenant_id=?` for
  `TenantOwned` entities, DB-wide across all four centers (`TenantContextHolder.java:38-92`).
- **External identity:** tables present (`dc3_identity_provider`, `dc3_external_identity`) but **not implemented** —
  login endpoints closed (`identity-principal-service-account.md:8-13,354-443`).

### B.3 Metadata / domain model

- **Thing model:** `Profile` is the root capability template containing `Point` (data/control attributes), `Command` (
  custom actions), `Event` (info/alert/fault/lifecycle) (`thing-model.md:97-110`).
- **Device binding:** `Device.profileId` is a **singular** `Long` FK — each Device → exactly one Profile (Phase-1 moved
  away from many-to-many `ProfileBind`) (`DeviceDO.java:78-81`, `thing-model.md:14-24`).
- **Point:** `pointName`, `pointCode` (unique/profile), `pointTypeFlag`, `rwFlag`, `unit`, precision, conversion (
  baseValue/multiple). **Point read/write is `rwFlag` on Point, NOT the Command table** (`PointDO.java:62-117`,
  `thing-model.md:139-145`).
- **Param vs Attribute vs Config (3 distinct layers):**
    - **Param** — business-layer (`CommandParam`/`EventParam`): I/O parameter definitions in the Profile model.
    - **Attribute** — driver-protocol-layer (`DriverAttribute`/`PointAttribute`/`CommandAttribute`/`EventAttribute`):
      registered by drivers at startup from `application.yml`; defines *what config items exist per driver*.
    - **Config** — device-instance-layer (`PointAttributeConfigDO` etc.): stores *this device's actual values* for those
      attributes (`command-event-attribute-config.md:30-43,200-258`, `PointAttributeConfigDO.java`).
- **DO/BO/VO:** DO = DB shape (Byte flags, `@TableId`/`@TableLogic`); BO = business (domain enums `EnableFlagEnum`
  etc.); VO = API shape. MapStruct `*Builder` with `@AfterMapping` handles enum (`@EnumValue` index ↔ `ofIndex`/
  `ofCode`) and JSON-ext (`BaseExt`, `JacksonTypeHandler`) conversion (`DeviceBuilder.java:50-138`,
  `CommandBuilder.java:71-108`). Enum suffixes: `*FlagEnum` (0/1), `*StatusEnum` (state-machine), `*TypeEnum` (
  classification) (`AGENTS.md:367-376`).
- **Note:** `dc3_event` (model definition) is separate from `dc3_entity_alarm` (runtime instances).

### B.4 Data plane (point-value ingest)

- **Path:** Device → Driver SDK → RabbitMQ value exchange → Data Center → TimescaleDB; Caffeine cache for hot reads (
  `PointValueLocalCache.java:48-70`).
- **RabbitMQ topology:** topic exchange `dc3.e.value`, routing key `dc3.r.value.point.{driver-service-name}`; durable
  queue `dc3.q.value.point` with 7-day TTL (604800000 ms); DLX `dc3.e.point_value_dead` (`RabbitConstant.java:162-165`,
  `DataTopicConfig.java:126-142`).
- **Consumer:** `PointValueReceiver` `@RabbitListener` on high-throughput factory (prefetch=100, concurrent=4, max=32).
  Immediate persist or `PointValueJob` batch based on inbound velocity (`PointValueReceiver.java:54-73`).
- **Sender:** `DriverSenderService.pointValueSender()` injects driverId/tenantId from `DriverMetadata`, sends with
  `PointValueCorrelation` tracking (`DriverSenderServiceImpl.java:148-177`).
- **Model transform:** `ReadPointValue` → `CalculatedPointValue` (scaling/projection) → `PointValue` (driver bean) →
  `PointValueBO` (message) → `PointValueDO` (persist) → `PointValueVO` (API). `PointValue` ≠ `PointValueBO` — different
  layers/serialization (`PointValue.java:101-109`, `PointValueServiceImpl.java:74-88`).
- **Storage:** `dc3_point_value` is a TimescaleDB hypertable partitioned by `create_time` (1 day) + `device_id` (16
  buckets); index `(tenant_id, device_id, point_id, create_time DESC)`. `num_value` is **nullable** for non-numeric —
  aggregates must filter `num_value IS NOT NULL` (`05-iot-dc3-history.sql:52-98`, `PointValueDO.java:40-102`).
- **Timestamps:** `create_time` (device collection) vs `operate_time` (Data Center persist) — distinct, needed for
  latency analysis.
- **Messaging guarantees:** persistent delivery, manual ack, publisher confirms; failed → reject/nack-requeue (
  `RabbitConfig.java:100-155`). JSON via `JacksonJsonMessageConverter`.
- **Alarm engine:** `AlarmRuleTriggerService` evaluates each `PointValueBO` immediately after persistence (
  `PointValueServiceImpl.java:86-87`).
- **Read APIs:** `/latest` and `/page` → `PointValueVO`, tenant-scoped via `PointValueQuery` (
  `PointValueController.java:73-80`).

### B.5 Command plane (read/write dispatch + ack)

- **Entry:** `POST /point_command/read` and `/point_command/write` return `commandId` immediately for polling (
  `PointCommandController.java:75-107`).
- **Validation:** tenant scope, device/point enableFlags, write `rwFlag`, driver online status before dispatch (
  `PointCommandServiceImpl.java:96-243`).
- **Persist+publish:** write `dc3_point_command_history` with PENDING → publish with publisher-confirm
  `CorrelationData=commandId`; confirm ACK transitions PENDING→SENT (`PointCommandServiceImpl.java:114-135,271-275`).
- **DTO:** `PointCommandDTO` sealed interface + polymorphic `ReadPayload`/`WritePayload`, Instant UTC, `commandId`+
  `tenantId`+`expireAt`+`schemaVersion`. Default `expireAt = occurredAt + 10s` (`PointCommandDTO.java:51-80`).
- **RabbitMQ:** topic `dc3.e.point_command` → `dc3.q.point_command.{serviceName}` (30s TTL + DLX
  `dc3.e.point_command_dead`). Results → `dc3.e.point_command_result` (60s TTL) → `PointCommandResultReceiver` (
  `DriverTopicConfig.java:91-96`, `DataTopicConfig.java:272-289`).
- **Driver processing:** `PointCommandReceiver` → expireAt pre-check → dedup (Caffeine 5-min, 50k) → per-device
  `ReentrantLock` (`DeviceLockManager`, ref-counted) → read/write dispatch (`PointCommandReceiver.java:65-148`).
- **Write semantics:** success only if `driverCustomService.write()` returns `Boolean.TRUE`; **failed writes do NOT echo
  a value** (`responseValue=null` on FAILED) — intentional, prevents false success (
  `DriverWriteServiceImpl.java:98-108`, `PointCommandReceiver.java:126-131`).
- **Lifecycle:** `PENDING → SENT → [SUCCESS | FAILED | TIMEOUT | EXPIRED | DUPLICATE | DEAD]`. EXPIRED set by driver
  when `now > expireAt` at consume time; DUPLICATE by dedup cache miss (`PointCommandStatusEnum.java:36-77`).
- **Error path:** non-redelivered exception → nack(requeue) + release dedup; redelivered → FAILED result + ack (avoid
  loop) (`PointCommandReceiver.java:136-148`).
- **Polling:** `GET /point_command/{commandId}` → `PointCommandHistoryVO` (`PointCommandServiceImpl.java:183-203`).
- **History table:** `dc3_point_command_history` — `command_id` (UUID 36-char), `type` (0=READ/2=WRITE), `status` (0-7),
  occur/send/finish/expire times, `source` (0=HTTP/1=gRPC/2=agentic).
- **Device timeout:** `dc3_entity_state` lease version + expire_time + 45s RabbitMQ heartbeat TTL (
  `device-driver-timeout.md §5-7`).
- **Distinct from Custom Command:** `dc3.e.command` / `CommandCallDTO` is a separate namespace+DTO from
  `dc3.e.point_command` / `PointCommandDTO` (`command-call.md §1.3`, `point-command.md §1.2`).

### B.6 Drivers & SDK

- **28 production drivers** as `dc3-driver-<protocol>` Spring Boot modules (`dc3-driver/pom.xml:180-209`). Categories:
  industrial (Modbus TCP/RTU, OPC UA/DA, S7, BACnet, EtherNet/IP, FINS, MELSEC, IEC104, SL651, DLMS), IoT (MQTT, CoAP,
  LwM2M, HTTP, BLE, Zigbee), DB-bridge (MySQL/PostgreSQL/Oracle/SqlServer), basic (TCP/UDP, Serial, SNMP, CAN),
  simulation (Virtual, Listening-Virtual).
- **Lifecycle:** `DriverInitRunner` orchestrates register → `initial()` → `schedule()`; registration retry exponential
  backoff (2-30s, max 30 attempts) (`DriverInitRunner.java:54-98`).
- **SPI:** `DriverCustomService` aggregates 7 interfaces — `DriverLifecycle`, `DriverMetadataListener`, `DriverHealth`,
  `DeviceHealth`, `DriverProtocol`, `DriverCommand`, `DriverValidator` (`DriverCustomService.java:37`).
- **Registration:** reads `dc3.driver` props, submits `RegisterBO` via gRPC to Manager with code/name/service/tenant +
  all attribute defs (`DriverRegisterServiceImpl.java:50-92`).
- **Read scheduling:** Quartz `DriverReadScheduleJob` (cron `dc3.driver.schedule.read`) iterates devices from
  `DriverMetadata` cache → ThreadPoolExecutor per-device tasks (`DriverReadScheduleJob.java:68-79`).
- **Protocol contract:** `read(driverConfig, pointConfig, device, point) → ReadPointValue`; `write(...) → Boolean` (
  `DriverProtocol.java:54-70`).
- **Senders:** `pointValueSender`, `deviceStatusSender` (with TTL), `driverEventSender`, `deviceEventSender`, alarm
  senders (`driver-authoring.md:219-235`).
- **Metadata events:** `DriverMetadataListener.event(MetadataEventDTO)` receives ADD/DELETE/UPDATE for cache refresh (
  `DriverMetadataListener.java:33-42`).
- **Health:** `DriverHealth.health() → ONLINE/OFFLINE/FAULT/MAINTAIN`; `DeviceHealth.health(...)` per device. **Status
  TTL must exceed read-cycle period** (e.g. 30s cron → TTL ≥ 25s) or devices flap offline (
  `driver-authoring.md:233-235`).
- **Authoring:** copy `dc3-driver-virtual` template, rename, inherit `dc3-driver` parent, implement
  `DriverCustomService` with `@Service`. **`dc3.driver.code` is a stable routing identifier — never change without
  migration** (`driver-authoring.md:36-49,237-247`).
- **Validator SPI:** `validate`/`validateDevice`/`validatePoint` + `simulate(point)` for synthetic values (
  `DriverValidator.java:60-119`).

### B.7 Agentic / AI — **implemented vs aspirational**

**Implemented (production code):**

- Agentic Center is a real microservice (`dc3-center-agentic:8600`) (`AgenticApplication.java`).
- Spring AI `ChatClient` with memory advisor + tool-call advisor; history replayed per request from `dc3_message` (
  history is **persisted, not in-memory session**) (`ChatClientConfig.java:61-117`).
- Native `@Tool` calling across tool domains (Tenant/User/Device/Driver/Profile/Point/PointValue/Command/System),
  tenant-isolated via `AgenticToolContextUtil.requireTenantId(toolContext)` (`ChatClientConfig.java:106-117`, `tools/`).
- OpenAI-compatible `POST /v1/chat/completions` (SSE + non-streaming); `x-dc3-ai` OpenAPI extensions (
  riskLevel/destructive/idempotent/openWorld) (`ChatController.java:67-88`).
- MCP gateway at `dc3-gateway:/mcp` — RFC 9728 metadata discovery, OAuth 2.1 introspection, `tools/list`/`tools/call`
  JSON-RPC, re-verified every call (`McpGatewayController.java:86-167`).
- OAuth AS+RS in `dc3-center-auth` — hand-written RS256 JWT, introspection, PKCE, refresh rotation (
  `OAuthProperties.java`, `mcp-server.md:9-52`).
- Pending-action confirmation: HIGH-risk tools → `CONFIRM_REQUIRED`+`confirmId`; two-phase commit with SHA256 param
  digest + idempotency key (`ActionController.java:68-100`).
- Persistence: `dc3_session` / `dc3_message` / `dc3_attachment` / `dc3_action` (`06-iot-dc3-agentic.sql:18-300`).
- Multi-provider via `ChatClientFactory` (OpenAI/Anthropic + Spring AI fallback); credentials from `dc3_model_provider`
  DB table, env only as fallback when table empty (`ChatClientFactory.java:68-98`).
- Feature flags: `agentic.tool-calling-enabled`, `memory-enabled`, `fallback-{tool-calling,vision,reasoning}-enabled` (
  `AgenticProperties.java:39-96`).

**Aspirational / gated / not-default:**

- Tool calling defaults true but is **flag-gated**, not hard-wired.
- Tool catalog `x-dc3-ai` metadata is **manually authored**, not auto-generated from `@Operation`.
- `AGENTIC_MEMORY_SCHEMA_INIT` defaults `never` — must be `always` on first run (`.env.example:66-76`).
- Token model is **OAuth 2.1 only** (no PAT/`dc3mcp_*` tokens).
- Tool visibility = RBAC ∩ OAuth scope ∩ MCP-connection whitelist ∩ risk policy — not "in catalog = exposed."

### B.8 Deploy / ops

- **Four compose stacks:** db (postgres+rabbitmq), dev (source-build), app (pre-built images), optional (
  observability) (`Makefile:37,39,87`).
- **Postgres init:** ordered SQL scripts via `/docker-entrypoint-initdb.d/` on first start — 00 extensions, 01 common,
  02 auth, 03 data, 04 manager, 05 history, 06 agentic (VERIFY actual file count/order in
  `dc3/dependencies/postgres/initdb/`).
- **RabbitMQ provisioning:** `definitions.json` template with `@RABBITMQ_*@` substitution (`definitions.json:1-46`).
- **Env file split:** `.env.example`→`.env` for Compose interpolation only; `dc3/env/dev.env(.sh)` for local Java/IDE
  runs (localhost:35432 pg, 35672 rabbit, 31883 MQTT/EMQX) (`environment.md:5-15`, `dev.env.sh:23-44`).
- **REGISTRY:** `{auto|global|cn}` → `DC3_IMAGE_REGISTRY` (global=pnoker/Docker Hub, cn=Aliyun) (`Makefile:75-85`).
- **Secrets:** `DC3_SECURITY_KEY` + `AUTH_HMAC_SECRET` ship with hardcoded defaults — must be env-specific random in
  prod (`.env.example:17-18`).
- **Web:** independent `DC3_WEB_VERSION` (decoupled from `DC3_IMAGE_TAG`); nginx + `APP_API_HOST/PORT` routing (
  `docker-compose.yml:49-64`).
- **Ingress:** only web (user) + listening-virtual TCP 6270/UDP 6271 (device) are mapped; all other backend ports
  internal (`docker-compose.yml:224-230`).

### B.9 Frontend surface

- **Primary nav:** Home, Driver, Profile, Device, Data (`views.ts:28-80`). Three API bases: Manager, Data, Agentic (
  `device.ts:17-18`, `agentic.ts:17-18`).
- **Settings (15+):** Users/Principals/Roles/Resources/APIs/Menus/Groups/Labels + nested Alarm/Event/Command/Model/MCP (
  `settings.ts:21-307`).
- **RBAC-driven menu:** dynamic from `menuStore.tree`, route guards via `ROUTE_MENU_ALIASES` (`router/index.ts:50-142`).
  Session via `AUTH_HEADERS` (TENANT/LOGIN/TOKEN) in localStorage; 401 interceptor clears.
- **SSE chat:** chunked delta + reasoning + trace + visualization specs (`agentic.ts:126-291`).

### B.10 CLI

- `dc3-cli` (Node ≥20, TypeScript): 14 command modules over `/api/v3/*` gateway (`index.ts:1-16`). Three-stage token:
  GET salt → POST MD5(password)+salt → JWT to `~/.dc3/tokens.json` (`client.ts:173-228`). Proactive renewal (<1h
  threshold) + reactive 401-retry. Credential backends: keychain/encrypted(AES-256-GCM)/env/prompt. Exit codes 0/1/2/3 (
  success/business/network/auth).

---

## C. Current docs audit

### Cross-cutting global issues

1. **Two empty/broken pages** (`/guide/usage.md`, `/development/changelog.md`) contain only `<!--@include-->`
   directives — VitePress renders blank; included files exist (`dc3/doc/USAGE.md`, `dc3/doc/CHANGE.md`) but no fallback
   content. **CRITICAL.**
2. **No diagrams anywhere** on public pages — only one static PNG reference; all flows are text/ASCII-only.
3. **Inconsistent depth** — `concepts.md` is tables-only; `device-onboarding.md` is detailed; `troubleshooting.md`
   exhaustive vs `logging.md` narrow.
4. **No "you are here" learning path** — index pages don't sequence the journey.
5. **Architecture terminology collision** — `modules.md` is a Maven reference while `architecture/index.md` claims
   system architecture but barely explains gRPC facade / RabbitMQ / tenant isolation working together.
6. **Chinese-only site** — no lang selector; EN README/CONTRIBUTING exist but docs are monolingual zh-CN.
7. **No persona entry points** — "onboard a Modbus device" or "deploy to production" require stitching 3+ scattered
   pages.
8. **Superpowers tree bleeds into nav** — WIP maintainer notes appear in public navigation.

### Per-page table

| Page                                | Action      | Top issues                                                                                                      | AI-ish patterns                                                     | Priority |
|-------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|----------|
| `/docs/index.md`                    | polish      | Hero image unexplained; terse cards, no "why this one"; unsourced tech table; superpowers footer link confuses  | Emoji feature bullets; "Superpowers… evolving" placeholder phrasing | high     |
| `/quickstart/index.md`              | polish      | Forward-refs env distinction w/o inline summary; no startup-dependency rationale; sparse pitfalls; no IDE setup | Bash command list reads like chatbot transcript                     | high     |
| `/quickstart/environment.md`        | polish      | Defensive tone; abstract conceptual model; 155+ vars organized by scope not persona; key-placement unclear      | Var dump + FAQ-style "misconceptions"                               | medium   |
| `/operation/index.md`               | polish      | 26 lines; Web UI links to separate repo (broken entry); no "what does success look like"                        | Generic table, defers all content                                   | high     |
| `/operation/concepts.md`            | **rewrite** | Tables-only, zero prose; ASCII flows; vague tenant section; missing Point data types + writable flag            | Classic all-tables data dump                                        | high     |
| `/operation/device-onboarding.md`   | polish      | "Attribute" never sourced; log-reading unexplained; placeholder examples; no decision tree                      | Numeric steps, terse bullets, generic placeholders                  | medium   |
| `/operation/data-commands.md`       | polish      | Data+commands not connected; no offline/read-only handling; no curl examples; reactive troubleshooting          | Parallel tables, symptom/solution w/o root cause                    | high     |
| `/operation/agentic.md`             | polish      | Vague "tool calling"; config-source debugging unclear; boilerplate security; prereqs assume knowledge           | AI jargon ungrounded; generic LLM safety checklist                  | medium   |
| `/guide/index.md`                   | polish      | 18 lines; no connective prose; quickstart pointer buried; local-dev-only                                        | Bare table                                                          | low      |
| `/guide/usage.md`                   | **rewrite** | **CRITICAL** include-only blank page; content invisible in nav                                                  | Single-line include, no fallback                                    | high     |
| `/guide/logging.md`                 | polish      | Prescriptive w/o rationale; assumes MDC knowledge; weak PII-masking guidance                                    | Code + "avoid X" reactive style                                     | low      |
| `/guide/troubleshooting.md`         | polish      | Presupposes prior failure; make/podman-only; no per-OS PID lookup; pre/pro+Nacos unexplained                    | Symptom-solution w/o root-cause                                     | medium   |
| `/architecture/index.md`            | polish      | Image unexplained; 4 design patterns listed not explained; no consistency model / scalability                   | Terse "Key Design" checklist                                        | high     |
| `/architecture/modules.md`          | polish      | Flat 28-driver list w/o maturity; naming conventions implicit; no dependency graph; facade-mode unexplained     | Maven-structure dump not architectural concern                      | medium   |
| `/development/index.md`             | polish      | 27 lines; speed-read w/o rationale; no first-path guidance; AGENTS.md link missing                              | Speed-read bullets                                                  | low      |
| `/development/driver-authoring.md`  | polish      | Assumes Maven depth; attribute-flag values unvalidated; lifecycle/error-handling thin; no API-only smoke test   | Numbered tutorial, no connective narrative                          | medium   |
| `/development/api-documentation.md` | polish      | ASCII diagram hard to read; default creds unstated; conventions w/o rationale; prod-disable rationale missing   | Architecture + bullet conventions                                   | low      |
| `/development/testing.md`           | polish      | "when to add tests" lacks nuance; Testcontainers reuse unexplained; no local E2E guide                          | Exhaustive reference, no narrative                                  | low      |
| `/development/changelog.md`         | **rewrite** | **CRITICAL** include-only blank page; no generation-process explanation                                         | Single-line include                                                 | high     |
| `/modules/index.md`                 | polish      | Flat catalog; no mandatory/optional/maturity; GitHub-nav assumed; no versioning policy                          | Auto-generated-inventory feel                                       | low      |
| `/community/contributing.md`        | keep        | EN file mismatches zh context; broken `dc3/doc/ENVIRONMENT.md` ref                                              | —                                                                   | low      |
| `/community/code-of-conduct.md`     | keep        | Boilerplate; no IoT-specific examples                                                                           | —                                                                   | low      |
| `/community/security.md`            | polish      | No TLS/mutual-auth config link; version scheme unexplained                                                      | —                                                                   | low      |

---

## D. Proposed information architecture (sketch)

```
Home (index.md)                         [polish: add C4 diagram, "why this one" narrative, audience+deploy paths]

Introduction (NEW section)
├── What is IoT DC3 / positioning      [NEW — from A; the closed-loop story]
├── Core concepts & mental model        [merge concepts.md rewrite — prose + ER diagram]
└── Choose your path (persona router)   [NEW — "onboard a device" / "deploy to prod" / "use AI" / "write a driver"]

Quickstart
├── Local development from source        [polish index.md]
├── Environment files explained          [polish environment.md — add decision-tree diagram]
└── First device end-to-end (NEW)        [NEW — golden-path: virtual driver → profile → device → point → see data]

Architecture
├── System overview                      [polish — C4 + key-design prose]
├── Services & topology (NEW split)      [NEW — from B.1: ports, startup order, health]
├── Facade modes (grpc vs local) (NEW)   [NEW — from B.1: deployment-topology choice]
├── Data plane                           [NEW — from B.4: ingest sequence + RabbitMQ topology]
├── Command plane                        [NEW — from B.5: lifecycle state machine + topology]
├── Auth, tenant & RBAC (NEW)            [NEW — from B.2: header/HMAC/permission flows]
├── Domain model (Profile/Point/...)     [NEW — from B.3: ER + Param/Attribute/Config layering]
└── Module map                           [polish modules.md — add maturity + dependency graph]

Operation
├── Overview / running entry points      [polish — fix Web-UI broken link]
├── Device onboarding                    [polish — add decision-tree + data-flow diagrams]
├── Data & commands                      [polish/split — connect the two; curl examples]
├── Alarms & notifications (NEW)         [NEW — from frontend findings: rule/notify/channel ER]
└── Agentic center                       [polish — ground jargon, config-precedence diagram]

Development
├── Overview / conventions               [polish — link AGENTS.md, first-path guidance]
├── Driver authoring                     [polish — class + lifecycle-sequence diagrams]
├── API documentation (OpenAPI)          [polish — default creds, auth flow]
├── Testing                              [polish — pyramid diagram, local E2E]
└── Changelog                            [rewrite — fallback content + generation note]

Automation (NEW section — from CLI findings)
├── CLI user guide                       [NEW]
└── AI agent / MCP integration           [NEW]

Deployment & Ops Guide
├── Overview                             [polish guide/index.md]
├── Deployment modes & registries        [rewrite usage.md — embed real content]
├── Observability (optional stack) (NEW) [NEW — from B.8]
├── Logging                              [polish — add rationale, PII masking]
└── Troubleshooting                      [polish — flowchart, per-OS notes]

Community
├── Contributing / Code of Conduct / Security   [keep/polish — fix broken refs, version scheme]

(superpowers/ — maintainer-internal, OUT OF PUBLIC NAV SCOPE; exclude from site build)
```

---

## E. Diagram inventory (42)

| ID  | Title                                   | Mermaid type         | Home page                            | Shows                                                                     |
|-----|-----------------------------------------|----------------------|--------------------------------------|---------------------------------------------------------------------------|
| D1  | System C4 context                       | flowchart (C4-style) | Home + architecture/overview         | Client → Gateway → 5 centers + drivers, tenancy boundary                  |
| D2  | Service topology & dependencies         | flowchart            | architecture/services                | Six services, HTTP/gRPC ports, depends_on edges                           |
| D3  | Startup & health-check order            | sequence             | architecture/services + quickstart   | PG/Rabbit → Auth → Manager → Data → Agentic → Gateway → Drivers           |
| D4  | Facade mode selection                   | stateDiagram         | architecture/facade-modes            | grpc (distributed) vs local (single) activation                           |
| D5  | HTTP request flow through gateway       | sequence             | architecture/auth-rbac               | Client → gateway (auth+HMAC sign) → gRPC facade → backend → back          |
| D6  | Multi-tenant context threading          | sequence             | architecture/auth-rbac               | X-Auth-Principal → ThreadLocal → gRPC propagation → MyBatis tenant filter |
| D7  | Auth & token issuance                   | sequence             | architecture/auth-rbac               | salt handshake → validation chain → JWT bound to principal+tenant         |
| D8  | RBAC resolution & caching               | flowchart            | architecture/auth-rbac               | role-principal → role-resource → resource codes, 5-min cache, fail-closed |
| D9  | Principal entity model                  | erDiagram            | architecture/auth-rbac               | principal → user/service_account/local_credential → tenant_membership     |
| D10 | Gateway principal header verification   | flowchart            | architecture/auth-rbac               | extract → HMAC verify → parse → reactive permission load                  |
| D11 | Core metadata ER                        | erDiagram            | architecture/domain-model + concepts | Profile→Point/Command/Event; Device→Profile (single FK)                   |
| D12 | DO/BO/VO layering                       | classDiagram         | architecture/domain-model            | Controller(VO)→Service(BO)→Manager(DO) + MapStruct builders               |
| D13 | Param vs Attribute vs Config scope      | flowchart            | architecture/domain-model            | business / driver / device-instance scopes                                |
| D14 | Enum conversion in builders             | sequence             | development/domain-modeling          | DO Byte@EnumValue → ofIndex → BO enum → VO                                |
| D15 | Point-value ingest end-to-end           | flowchart            | architecture/data-plane              | Device→Driver→RabbitMQ→Data Center→TimescaleDB + cache                    |
| D16 | Value exchange topology                 | stateDiagram         | architecture/data-plane              | value exchange → point queue (7-day TTL) → DLX                            |
| D17 | PointValue model transformation         | classDiagram         | architecture/data-plane              | ReadPointValue→PointValue→PointValueBO→DO→VO                              |
| D18 | Latest-value cache + repository read    | sequence             | architecture/data-plane              | Caffeine hit vs TimescaleDB fallback                                      |
| D19 | Command happy path (write success)      | sequence             | architecture/command-plane           | HTTP submit → driver execute → result → poll                              |
| D20 | Command lifecycle state machine         | stateDiagram         | architecture/command-plane           | PENDING→SENT→SUCCESS/FAILED/EXPIRED/TIMEOUT/DUPLICATE/DEAD                |
| D21 | Command RabbitMQ topology               | flowchart            | architecture/command-plane           | exchanges, queue bindings, TTL/DLX, result channel                        |
| D22 | Driver command processing pipeline      | flowchart            | development/driver-authoring         | validation→dedup→lock→execute→result + error paths                        |
| D23 | Driver lifecycle state machine          | stateDiagram         | development/driver-authoring         | startup→register(retry)→initial→schedule→runtime                          |
| D24 | New-driver authoring workflow           | flowchart            | development/driver-authoring         | template copy→pom→yml→SPI impl                                            |
| D25 | Driver SDK SPI composition              | classDiagram         | architecture/module-map              | DriverCustomService aggregating 7 SPIs                                    |
| D26 | Driver registration & attribute flow    | erDiagram            | development/driver-authoring         | application.yml → DriverProperties → RegisterBO → Manager                 |
| D27 | Read/write command flow with scheduling | sequence             | development/driver-authoring         | Quartz→cache→read()→RabbitMQ + reverse write                              |
| D28 | AI-assisted operation sequence          | sequence             | operation/agentic                    | chat → tool call → action confirm → device execute                        |
| D29 | MCP gateway access-control layering     | flowchart            | operation/agentic                    | OAuth → catalog → RBAC ∩ scope ∩ whitelist ∩ risk                         |
| D30 | Spring AI ChatClient advisor chain      | flowchart            | operation/agentic                    | memory advisor + tool advisor + trace callback                            |
| D31 | Agentic DB schema                       | erDiagram            | operation/agentic                    | session→message→action→attachment                                         |
| D32 | Tool catalog construction               | flowchart            | development/api-documentation        | OpenAPI + dc3_api + dc3_resource → stable tool_id                         |
| D33 | Deployment topology (compose stacks)    | flowchart (C4-style) | guide/deployment                     | db / dev / app / optional stacks + ingress                                |
| D34 | Local dev workflow (zero-to-running)    | flowchart            | quickstart/index                     | up-db → source env → package → start/up-dev → test                        |
| D35 | Env variable scope & resolution         | stateDiagram         | quickstart/environment               | .env→Compose vs dev.env.sh→Java process                                   |
| D36 | Optional observability integration      | flowchart (C4-style) | guide/observability                  | EMQX/ELK/Prometheus/Grafana wiring                                        |
| D37 | Frontend navigation hierarchy           | flowchart            | architecture/frontend                | primary nav + settings tree + detail routes                               |
| D38 | Alarm/event entity relationships        | erDiagram            | operation/alarms                     | rule/notify/channel/bind/state/history                                    |
| D39 | RBAC menu → route-guard → backend       | flowchart            | architecture/frontend                | menu tree → route guard → backend enforcement                             |
| D40 | CLI authentication flow                 | sequence             | automation/cli                       | three-stage token + proactive renewal                                     |
| D41 | CLI command architecture                | classDiagram         | automation/cli                       | entry → 14 modules → client/config/token/credential                       |
| D42 | Credential resolution chain             | flowchart            | automation/cli                       | keychain→encrypted→env→prompt fallback                                    |

---

## F. Voice & anti-AI-ish style guide

1. **No parallel buzzword tables as the primary explanation.** Tables are reference, not narrative. Every concept table
   must be preceded by prose that builds the mental model and answers "why does this exist."
2. **Lead with "why," then "how."** Don't list `gRPC facade / RabbitMQ / HMAC / multi-tenancy` as a checklist — explain
   what problem each solves and how they interact.
3. **Ground every term on first use.** "Attribute," "tool calling," "facade mode," "point" must be defined where
   introduced, with a one-line source of the value (e.g. attributes come from the driver's `application.yml`).
4. **One diagram per key flow.** Every data/command/auth/lifecycle flow gets a Mermaid diagram (see §E), not ASCII art.
   Text describes; the diagram shows timing and structure.
5. **Concrete over placeholder.** Replace generic `temperature/celsius/host/port` with a runnable example (the virtual
   driver golden path), including real curl commands and JSON responses.
6. **No emoji-bullet feature cards or "superpowers… evolving" filler.** State the specific capability and its limitation
   in the same breath.
7. **Name the failure honestly.** Document hard requirements as hard (HMAC fails-fast in prod; `num_value` is nullable;
   write-failures don't echo; `dc3.driver.code` is immutable).
8. **Troubleshooting explains root cause, not just symptom→fix.** Each entry says *why* the symptom appears and how to
   read the relevant log line.
9. **Write for a persona path, not a flat menu.** Each section opens with "you are here / next step." Add the persona
   router.
10. **Distinguish implemented from aspirational.** Mark production code vs flag-gated vs not-implemented (external
    identity tables exist but login is closed; OAuth-only, no PAT).
11. **Reference, don't dump.** Exhaustive var/module lists get a short "what you actually need for X" intro and persona
    grouping before the full table.
12. **Keep maintainer-internal content out of public voice.** `superpowers/` notes must not appear in public nav or
    borrow their informal draft tone.

---

## Gaps & open questions (verify against source before/while writing each page)

1. **Postgres init script count.** Findings say "six" but enumerate seven (00 extensions, 01 common, 02 auth, 03 data,
   04 manager, 05 history, 06 agentic). CLAUDE.md references 02/03/04 by different names. Verify actual file count/order
   in `dc3/dependencies/postgres/initdb/`.
2. **Command `type` enum values.** READ=0, WRITE=2 (value 1 unexplained). Confirm whether a third type exists in
   `PointCommandTypeEnum`.
3. **Tool-domain count.** Agentic findings say "8 tool domains" then list 9-10. Confirm exact catalog from
   `dc3-center-agentic/.../tools/`.
4. **Auth Center facade mode.** One finding marks auth `application.yml` as `local` facade even in distributed default;
   reconcile with "all distributed services default to grpc."
5. **CRUD verb convention.** Reconcile the platform-wide `get`/`list`/`add`/`update`/`delete` verb convention (
   CLAUDE.md) into the API docs page.
6. **No independent verification performed.** This dossier consolidates explorer findings only. Spot-check CRITICAL
   items (empty include pages, HMAC fail-fast, hypertable schema, `expireAt` 10s default, `num_value` nullable) against
   source before publishing.
