# IoT DC3 Docs Overhaul — 共享事实包（Facts Pack）

> 由调研编排于 2026-06-22 产出并经源码核验：驱动目录、黄金路径 API 合约、环境变量目录、告警与事件模型、Agentic 与 MCP。每页落地以此为准，写前再就近核对源码。配套：2026-06-22-docs-overhaul-dossier.md（架构事实底座）。

## Still unresolved (decide at write time)

No items were flagged UNRESOLVED in the research outputs. One internal inconsistency to reconcile at write time:

- **Agentic built-in tool count**: prose says "8 tools" / "8 built-in tools" but the enumerated list contains **10** classes (`CommandTool`, `DeviceTool`, `DriverTool`, `EventTool`, `PointTool`, `PointValueTool`, `ProfileTool`, `SystemTool`, `TenantTool`, `UserTool`). The verified directory listing confirms **10**. Use **10**; treat the "8" as stale.

---

## Verified facts

### Postgres init scripts (execution order)

`/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/dependencies/postgres/initdb/`

| Order | File |
|-------|------|
| 00 | `00-iot-dc3-extensions.sql` |
| 01 | `01-iot-dc3-common.sql` |
| 02 | `02-iot-dc3-auth.sql` (menus, resources, users, roles, OAuth + MCP tables) |
| 03 | `03-iot-dc3-data.sql` (runtime data: alarm/notify/rule tables) |
| 04 | `04-iot-dc3-manager.sql` (entity management: device, driver, point, profile) |
| 05 | `05-iot-dc3-history.sql` (hypertables) |
| 06 | `06-iot-dc3-agentic.sql` (session, message, attachment) |

### Enums

- **PointCommandTypeEnum** — `READ(0)`, `READ_BATCH(1)`, `WRITE(2)`, `WRITE_BATCH(3)`, `CONFIG(4)`. Yes, `READ_BATCH=1` sits between `READ=0` and `WRITE=2`.
  `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/PointCommandTypeEnum.java:36-62`

### Agentic tool classes (10)

`/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/tools/`
`CommandTool`, `DeviceTool`, `DriverTool`, `EventTool`, `PointTool`, `PointValueTool`, `ProfileTool`, `SystemTool`, `TenantTool`, `UserTool`.

### Auth Center facade mode

Distributed default is **GRPC**. Base `application.yml` declares `dc3.facade.mode: local` (line 40, local override); Manager `application.yml:35` declares `dc3.facade.mode: ${DC3_FACADE_MODE:grpc}`; `dc3/env/dev.env` sets `DC3_FACADE_MODE=grpc`.
- `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-center/dc3-center-auth/src/main/resources/application.yml:38-40`
- `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-center/dc3-center-manager/src/main/resources/application.yml:35`

### CRUD verb convention

Result cardinality drives the verb across both repos. `select*` = raw MyBatis Mapper only; `remove*` = inherited MyBatis-Plus Manager only; business deletion uses `delete*`.

| Action | Java method | HTTP path | gRPC RPC | Frontend |
|--------|-------------|-----------|----------|----------|
| Single record | `getXxx(...)` | `/get_xxx` | `GetXxx` | `getXxx(...)` |
| Collection | `listXxx(...)` | `/list_xxx` | `ListXxx` | `listXxx(...)` |
| Create | `add(BO)` | `/add` | n/a | `addXxx(...)` |
| Update | `update(BO)` | `/update` | n/a | `updateXxx(...)` |
| Delete | `delete(Long)` | `/delete` | n/a | `deleteXxx(...)` |

- `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/AGENTS.md:317-321`
- `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/controller/DeviceController.java:106-254` (endpoints: `/add`, `/delete`, `/update`, `/get_by_id`, `/list_by_ids`, `/list_by_profile_id`, `/list`)

### Critical spot-checks

- **`docs/guide/usage.md` include target**: `<!--@include: ../../dc3/doc/USAGE.md-->` → `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/doc/USAGE.md`
- **`docs/development/changelog.md` include target**: `<!--@include: ../../dc3/doc/CHANGE.md-->` → `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/doc/CHANGE.md`
- **HMAC fail-fast condition**: `isProtectedEnvironment()` checks activeProfiles and the `spring.env` property for `ENV_PRE="pre"` OR `ENV_PRO="pro"`; if matched and the secret is blank or equals the default `io.github.pnoker.dc3`, throws `IllegalStateException`.
  `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-public/src/main/java/io/github/pnoker/common/config/HmacAuthConfig.java:59-85`
- **`dc3_point_value` hypertable**: single hypertable. `device_id BIGINT NOT NULL DEFAULT 0`, `point_id BIGINT NOT NULL DEFAULT 0`, `raw_value TEXT NOT NULL DEFAULT ''`, `cal_value TEXT NOT NULL DEFAULT ''`, `num_value DOUBLE PRECISION` (**NULLABLE** — allows NULL for non-numeric/JSON payloads), `driver_id BIGINT NOT NULL DEFAULT 0`, `tenant_id BIGINT NOT NULL DEFAULT 0`, `create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, `operate_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`. Partitioned by `by_range('create_time', INTERVAL '1 day')` and `by_hash('device_id', 16)`.
  `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/dependencies/postgres/initdb/05-iot-dc3-history.sql:52-63, 96-98`
- **`PointCommandDTO` expireAt default**: `Instant.now().plusSeconds(10)` (10-second TTL), set in factory methods `ofRead()` and `ofWrite()`.
  `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-model/src/main/java/io/github/pnoker/common/entity/dto/PointCommandDTO.java:60, 77`

---

## Driver catalog

**Total: 28 modules.** Source: `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-driver/pom.xml:180-209` plus each `dc3-driver-*/` module pom.

### Industrial Fieldbus & PLC Protocols

| Module | Protocol | Purpose |
|--------|----------|---------|
| dc3-driver-modbus-tcp | Modbus TCP | Modbus over TCP/IP for industrial devices |
| dc3-driver-modbus-rtu | Modbus RTU | Modbus over serial (RS232/RS485/RS422) |
| dc3-driver-plcs7 | Siemens S7 | Siemens PLC S7 communication via TCP |
| dc3-driver-ethernet-ip | EtherNet/IP | Rockwell AB PLC communication |
| dc3-driver-opc-ua | OPC UA | OPC Unified Architecture |
| dc3-driver-opc-da | OPC DA | OPC Data Access (legacy Windows) |
| dc3-driver-fins | Omron FINS | Omron FINS for Omron PLCs |
| dc3-driver-melsec | Mitsubishi MC | Mitsubishi Melsec MC protocol |
| dc3-driver-bacnet-ip | BACnet/IP | Building Automation and Control Networks |
| dc3-driver-iec104 | IEC 60870-5-104 | IEC 104 for power systems / SCADA |
| dc3-driver-snmp | SNMP | Simple Network Management Protocol |

### IoT & Wireless Protocols

| Module | Protocol | Purpose |
|--------|----------|---------|
| dc3-driver-mqtt | MQTT | Lightweight pub/sub messaging |
| dc3-driver-coap | CoAP | Constrained Application Protocol |
| dc3-driver-lwm2m | LwM2M | Lightweight M2M device management |
| dc3-driver-ble | Bluetooth LE | Bluetooth Low Energy |
| dc3-driver-zigbee | Zigbee | Zigbee mesh networking |
| dc3-driver-sl651 | SL651-2014 | Hydrological telemetry protocol |

### Basic Communications

| Module | Protocol | Purpose |
|--------|----------|---------|
| dc3-driver-serial | RS232/RS485/RS422 | Generic serial port communication |
| dc3-driver-tcp-udp | TCP/UDP | Raw TCP and UDP socket communication |
| dc3-driver-http | HTTP REST | HTTP REST client for web API integration |
| dc3-driver-can | CAN bus | Controller Area Network |

### Database Bridges

| Module | Database | Purpose |
|--------|----------|---------|
| dc3-driver-mysql | MySQL | MySQL database integration |
| dc3-driver-postgresql | PostgreSQL | PostgreSQL database integration |
| dc3-driver-oracle | Oracle | Oracle database integration |
| dc3-driver-sqlserver | SQL Server | Microsoft SQL Server integration |

### Meter / Energy Protocols

| Module | Protocol | Purpose |
|--------|----------|---------|
| dc3-driver-dlms | DLMS/COSEM | Device Language Message Specification for smart meters |

### Utility / Simulation & Testing

| Module | Type | Purpose |
|--------|------|---------|
| dc3-driver-virtual | Virtual (authoring template) | Testing driver; **the driver authoring template** for new drivers |
| dc3-driver-listening-virtual | Virtual (listening ingress) | Reverse-mode driver listening for incoming data via TCP/UDP, enabling external systems to push data into DC3 |

---

## Golden-path API contract

All endpoints exposed through Spring Cloud Gateway (`dc3-gateway`) at the root path; the gateway aggregates paths from auth, manager, and data services, applies request/response filtering, extracts auth headers, and injects principal context for downstream tenant-isolation and permission checks.

Auth headers on protected endpoints: `X-Auth-Tenant`, `X-Auth-Login`, `X-Auth-Token`.

### (a) Get salt + Login → Token

`/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/controller/TokenController.java`

| Step | HTTP | Request body | Response | Auth |
|------|------|--------------|----------|------|
| Get salt | `POST /token/salt` | `tenant`, `name` | `String` salt (5-min TTL) | public |
| Generate token | `POST /token/generate` | `tenant`, `name`, `salt`, `password` (hashed with salt) | `String` access token (12-hr validity) | public |

### (b)–(f) Manager center

Controllers under `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/controller/`. All return `String` new-entity ID via `SuccessCode.ADD`.

| Step | HTTP | Key request fields (VO) | Permission |
|------|------|-------------------------|------------|
| (b) Add Driver | `POST /driver/add` | `driverName`, `serviceName`, `serviceHost` (IPv4), `driverTypeFlag` (DriverTypeEnum), `enableFlag` | `driver:add` |
| (c) Add Profile | `POST /profile/add` | `profileName`, `profileShareFlag` (ProfileShareTypeEnum: TENANT/DRIVER/USER), `enableFlag` | `profile:add` |
| (d) Add Point | `POST /point/add` | `pointName`, `pointTypeFlag` (PointTypeEnum: STRING/INT/FLOAT/DOUBLE), `rwFlag` (RwTypeEnum: READ_ONLY/WRITE_ONLY/READ_WRITE), `profileId`, `valueDecimal` (Byte), `baseValue` (BigDecimal, opt), `multiple` (BigDecimal, opt), `unit` (opt), `enableFlag` | `point:add` |
| (e) Add Device | `POST /device/add` | `deviceName`, `driverId`, `profileId`, `enableFlag` | `device:add` |
| (f) Configure Point Attribute | `POST /point_attribute_config/add` | `attributeId`, `deviceId`, `pointId`, `configValue`, `enableFlag` | `point_attribute_config:add` |

Files: `DriverController.java`, `ProfileController.java`, `PointController.java`, `DeviceController.java`, `PointAttributeConfigController.java`.

### (g)–(k) Data center

Controllers under `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/controller/`.

| Step | HTTP | Key request fields | Response | Permission |
|------|------|--------------------|----------|------------|
| (g) Command Read | `POST /point_command/read` | `deviceId`, `pointId`, `commandId` (opt, idempotent) | `String` command ID | `point_command:list` |
| (h) Command Write | `POST /point_command/write` | `deviceId`, `pointId`, `value`, `commandId` (opt) | `String` command ID | `point_command:list` |
| (i) Get Command | `GET /command_history/get_by_record_id?recordId={id}` | `recordId` | `CommandHistoryVO` (recordId, deviceId, commandId, commandCode, paramValues, executeStatus, executeResult, executeTime, expireTime, createTime) | `command_history:get` |
| (j) Point Value Latest | `POST /point_value/latest` | `deviceId` (opt), `pointId` (opt), `current`, `size` | `Page<PointValueVO>` (deviceId, pointId, value, valueTime) | `point_value:list` |
| (k) Point Value Page | `POST /point_value/list` | `deviceId` (opt), `pointId` (opt), `current`, `size`, `startTime`, `endTime` | `Page<PointValueVO>` | `point_value:list` |

Files: `PointCommandController.java`, `CommandHistoryController.java`, `PointValueController.java`.

---

## Env catalog

### File structure and usage split

| File | Audience | Purpose | Interpolation |
|------|----------|---------|---------------|
| `.env.example` | Docker Compose | Template for root `.env`; image registry, tags, published ports | Compose variable interpolation |
| `dc3/env/dev.env` | IDE (EnvFile plugin) | Local Java runtime, no `export` | Read by IDE EnvFile plugin |
| `dc3/env/dev.env.sh` | Shell scripts | Local Java runtime with `export`; `source dc3/env/dev.env.sh` | Shell environment setup |

**Critical distinction**: Root `.env` is Docker Compose only — it does NOT auto-inject into local Java processes. Local Java IDE/CLI runs MUST use `dc3/env/dev.env(.sh)` to point at Compose-published services on localhost.

Sources: `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/.env.example`; `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/docs/quickstart/environment.md`; `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/env/dev.env(.sh)`.

### Scope legend

| Scope | Loaded by | Use case |
|-------|-----------|----------|
| Compose only | Docker Compose from root `.env` | Registry, image tags, published ports (`DC3_*`), bind host, observability ports |
| Runtime | Java process (local or container) | DB/MQ/broker hosts, security keys, agentic config, batching |
| Per-process override | Single service `SERVER_PORT`/`GRPC_SERVER_PORT`/route overrides | Override one service's port when running multiple locally |

### Security & Authentication (Runtime)

| Variable | Default | Required | Purpose |
|----------|---------|----------|---------|
| `DC3_SECURITY_KEY` | `dc3.security.key.2026.io.github.pnoker` | Yes (prod: strong random) | Auth Center token generation/validation, login Token signing key |
| `AUTH_HMAC_SECRET` | `io.github.pnoker.dc3` | Yes (prod: strong random) | HMAC-SHA256 key for Gateway→backend signing of `X-Auth-Principal` |

Production must use strong random values; never log/hardcode.

### Database — PostgreSQL

| Variable | Default | Scope | Purpose |
|----------|---------|-------|---------|
| `POSTGRES_HOST` | `localhost` | Runtime | `localhost` for IDE/local, `dc3-postgres` in Compose |
| `POSTGRES_PORT` | `35432` | Runtime | `35432` host-published; `5432` internal |
| `POSTGRES_USERNAME` | `dc3` | Runtime | User |
| `POSTGRES_PASSWORD` | `dc3dc3dc3` | Runtime | Password |
| `POSTGRES_DB` | `dc3` | Runtime | Database name |
| `POSTGRES_SCHEMA` | (unset) | Per-process | Single service schema (e.g. `dc3_manager`, `dc3_data`) |
| `DC3_POSTGRES_PORT` | `35432` | Compose only | Container published port |

Host vs internal: host `localhost:35432` ↔ Compose-internal `dc3-postgres:5432`.

### Message Queue — RabbitMQ

| Variable | Default | Scope | Purpose |
|----------|---------|-------|---------|
| `RABBITMQ_HOST` | `localhost` | Runtime | `localhost` local, `dc3-rabbitmq` in Compose |
| `RABBITMQ_PORT` | `35672` | Runtime | AMQP `35672` host; `5672` internal |
| `RABBITMQ_USERNAME` | `dc3` | Runtime | User |
| `RABBITMQ_PASSWORD` | `dc3dc3dc3` | Runtime | Password |
| `RABBITMQ_VIRTUAL_HOST` | `dc3` | Runtime | Virtual host |
| `RABBITMQ_MQTT_EXCHANGE` | `dc3.e.mqtt` | Runtime | MQTT-to-AMQP bridge exchange |
| `RABBITMQ_SSL_ENABLED` | `false` | Runtime | Enable TLS (port 5671 if true) |
| `RABBITMQ_SSL_ALGORITHM` | `TLS` | Runtime | TLS algorithm |
| `RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE` | `false` | Runtime | Validate server cert |
| `RABBITMQ_SSL_VERIFY_HOSTNAME` | `false` | Runtime | Verify hostname |
| `RABBITMQ_CONTAINER_PORT` | `5672` | Runtime | Internal app→RabbitMQ port (5671 with TLS) |
| `DC3_RABBITMQ_PORT` | `35672` | Compose only | AMQP published port |
| `DC3_RABBITMQ_TLS_PORT` | `35671` | Compose only | TLS published port |
| `DC3_RABBITMQ_MANAGEMENT_PORT` | `15672` | Compose only | Management UI published port |

### MQTT Broker — EMQX

| Variable | Default | Scope | Purpose |
|----------|---------|-------|---------|
| `MQTT_BROKER_HOST` | `localhost` | Runtime | Broker hostname |
| `MQTT_BROKER_PORT` | `31883` | Runtime | Broker port (EMQX published; ~1883 internal) |
| `MQTT_USERNAME` | `dc3` | Runtime | User |
| `MQTT_PASSWORD` | `dc3dc3dc3` | Runtime | Password |
| `MQTT_BATCH_SPEED` | `100` | Runtime | Batch size threshold (messages/batch) |
| `MQTT_BATCH_INTERVAL` | `5` | Runtime | Batch scheduling interval (ms) |
| `DC3_EMQX_WS_PORT` | `38083` | Compose only | WebSocket published |
| `DC3_EMQX_WSS_PORT` | `38084` | Compose only | Secure WebSocket published |
| `DC3_EMQX_MQTT_PORT` | `31883` | Compose only | MQTT published |
| `DC3_EMQX_MQTTS_PORT` | `38883` | Compose only | MQTT-over-TLS published |
| `DC3_EMQX_DASHBOARD_PORT` | `18083` | Compose only | Dashboard published |

### gRPC Inter-service

| Variable | Default | Scope | Purpose |
|----------|---------|-------|---------|
| `CENTER_AUTH_HOST` | `localhost` | Runtime | Auth Center host |
| `CENTER_MANAGER_HOST` | `localhost` | Runtime | Manager Center host |
| `CENTER_DATA_HOST` | `localhost` | Runtime | Data Center host |
| `CENTER_AGENTIC_HOST` | `localhost` | Runtime | Agentic Center host |
| `DC3_FACADE_MODE` | `grpc` | Runtime | Facade protocol mode (`grpc` or REST) |
| `DC3_FACADE_GRPC_DEADLINE_MS` | `3000` | Runtime | gRPC deadline (0 = no client deadline) |
| `DC3_AUTH_GRPC_PORT` | `9300` | Compose only | Auth gRPC published |
| `DC3_MANAGER_GRPC_PORT` | `9400` | Compose only | Manager gRPC published |
| `DC3_DATA_GRPC_PORT` | `9500` | Compose only | Data gRPC published |

### HTTP Gateway & Service Ports

| Variable | Default | Scope | Purpose |
|----------|---------|-------|---------|
| `DC3_GATEWAY_PORT` | `8000` | Compose only | Gateway HTTP published (entry point) |
| `DC3_AUTH_PORT` | `8300` | Compose only | Auth Center HTTP published |
| `DC3_MANAGER_PORT` | `8400` | Compose only | Manager Center HTTP published |
| `DC3_DATA_PORT` | `8500` | Compose only | Data Center HTTP published |
| `DC3_AGENTIC_PORT` | `8600` | Compose only | Agentic Center HTTP published |
| `SERVER_PORT` | (unset) | Per-process | Single service HTTP port override |
| `GRPC_SERVER_PORT` | (unset) | Per-process | Single center gRPC port override |
| `DC3_LISTENING_VIRTUAL_TCP_PORT` | `6270` | Compose only | Listening Virtual driver TCP published |
| `DC3_LISTENING_VIRTUAL_UDP_PORT` | `6271` | Compose only | Listening Virtual driver UDP published |
| `TCP_PORT` | (unset) | Per-process | Listening Virtual internal TCP port |
| `UDP_PORT` | (unset) | Per-process | Listening Virtual internal UDP port |
| `GATEWAY_ROUTE_AUTH_TOKEN_URI` | (unset) | Per-process | Auth token route override |
| `GATEWAY_ROUTE_AUTH_URI` | (unset) | Per-process | Auth service route override |
| `GATEWAY_ROUTE_MANAGER_URI` | (unset) | Per-process | Manager service route override |
| `GATEWAY_ROUTE_DATA_URI` | (unset) | Per-process | Data service route override |
| `GATEWAY_ROUTE_AGENTIC_URI` | (unset) | Per-process | Agentic service route override |

Uncomment per-service overrides only when running a single service locally.

### Agentic / AI Model Integration (Runtime)

| Variable | Default | Required | Purpose |
|----------|---------|----------|---------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL` | `https://api.openai.com` | No | Fallback API endpoint (OpenAI-compatible) |
| `AGENTIC_FALLBACK_OPENAI_API_KEY` | (empty) | Conditional | Fallback API key (if endpoint requires auth) |
| `AGENTIC_FALLBACK_OPENAI_MODEL` | `gpt-4o` | No | Fallback model name |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | `0.7` | No | Sampling temperature (0.0–2.0) |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS` | `2048` | No | Max output tokens |
| `AGENTIC_MEMORY_SCHEMA_INIT` | `never` | No | Spring AI JDBC memory schema init (`always`/`never`/`create_if_not_exists`) |
| `AGENTIC_MEMORY_ENABLED` | `false` | No | Enable persistent session memory (JDBC) |
| `AGENTIC_MEMORY_MAX_MESSAGES` | `50` | No | Max messages per conversation window |
| `AGENTIC_TOOL_CALLING_ENABLED` | `true` | No | Enable function/tool calling |
| `AGENTIC_ATTACHMENT_STORAGE_PATH` | `dc3/data/agentic/attachments` | No | Attachment storage directory |

"Fallback" = defaults if no provider is configured in `dc3_model_provider`. Never commit/log real keys.

> Note: env-var defaults above mirror `.env.example`; the in-code `AgenticProperties` defaults (see Agentic & MCP section) differ in places (e.g. `memoryEnabled` default true, `attachmentStoragePath` = `dc3/data/upload/agentic/attachment`). Reconcile against the intended deployment surface when documenting.

### Data Processing — Batching (Runtime)

| Variable | Default | Purpose |
|----------|---------|---------|
| `MQTT_BATCH_SPEED` | `100` | MQTT batch size threshold |
| `MQTT_BATCH_INTERVAL` | `5` | MQTT batch interval (ms) |
| `POINT_BATCH_SPEED` | `100` | Point value batch size threshold |
| `POINT_BATCH_INTERVAL` | `5` | Point value batch interval (ms) |

Whichever condition (count or interval) hits first flushes the batch.

### Container Image & Logging (Compose only)

| Variable | Default | Purpose |
|----------|---------|---------|
| `REGISTRY` | `auto` | Makefile registry selector (`auto`, `cn`, or hostname) |
| `DC3_IMAGE_REGISTRY` | `pnoker` | Docker image namespace |
| `DC3_IMAGE_TAG` | `2026.6` | Image tag for all services/dependencies |
| `DC3_LOG_MAX_SIZE` | `10M` | Single container log file rotation size |
| `DC3_LOG_MAX_FILE` | `20` | Rotated log files retained |
| `DC3_BIND_HOST` | `127.0.0.1` | Host bind address (`0.0.0.0` for network access) |

Makefile uses `REGISTRY`; Compose uses `DC3_IMAGE_REGISTRY`.

### Observability Stack

| Variable | Default | Scope | Purpose |
|----------|---------|-------|---------|
| `GF_SERVER_ROOT_URL` | `http://localhost:3000` | Runtime | Grafana external root URL |
| `DC3_GRAFANA_PORT` | `3000` | Compose only | Grafana published |
| `DC3_KIBANA_PORT` | `5601` | Compose only | Kibana published |
| `DC3_ES_JAVA_OPTS` | `-Xms512m -Xmx512m` | Runtime | Elasticsearch JVM heap |
| `DC3_LS_JAVA_OPTS` | `-Xms256m -Xmx256m` | Runtime | Logstash JVM heap |
| `APM_AGENT_ENABLE` | `false` | Runtime | Enable Java APM agent |

### Runtime & Profile Selection

| Variable | Default | Scope | Purpose |
|----------|---------|-------|---------|
| `NODE_ENV` | `dev` | Runtime | Spring profile group (`dev` = dev tools, relaxed security, verbose logging) |

---

## Alarm & event model

### Core tables

| Table | Purpose | Key attributes |
|-------|---------|----------------|
| `dc3_entity_alarm` | Runtime alarm records (all sources) | `id`, `alarm_target_type_flag` (0=point, 1=device, 2=driver, 3=event), `alarm_type_flag` (0=rule, 1=offline, 2=fault, 3=state-flip, 4=report), `alarm_source_flag` (0=rule, 1=state-timeout, 2=device-report, 3=driver-report, 4=system, 5=event-report), `alarm_level_flag` (0=P0…3=P3), `entity_id`, `rule_id`, `rule_state_id`, `driver_id`, `device_id`, `point_id`, `confirm_flag`, `alarm_ext` (JSON), `tenant_id`, `create_time`, `operate_time` |
| `dc3_rule` | Alarm rule definitions | `id`, `alarm_target_type_flag`, `rule_name`, `rule_code`, `entity_id`, `notify_id`, `message_id`, `rule_ext` (JSON), `enable_flag`, `tenant_id` |
| `dc3_rule_state` | Rule runtime state & trigger tracking | `id`, `rule_id`, `alarm_target_type_flag`, `entity_id`, `fingerprint`, `entity_state_flag` (0=pending, 1=firing, 2=recovered, 3=closed), `first_trigger_time`, `last_trigger_time`, `last_recover_time`, `last_notify_time`, `trigger_count`, `alarm_id`, `entity_state_ext` (JSON), `tenant_id` |
| `dc3_notify` | Notification channel config | `id`, `notify_name`, `notify_code`, `auto_confirm_flag`, `notify_interval` (ms), `notify_ext` (JSON), `enable_flag`, `tenant_id` |
| `dc3_notify_channel` | Delivery channels (email/SMS/Slack) | `id`, `channel_name`, `channel_code`, `channel_type_flag` (0=email, 1=SMS, 2=webhook), `credential_ref`, `channel_ext` (JSON auth), `enable_flag`, `tenant_id` |
| `dc3_notify_channel_bind` | Notify ↔ Channel mapping | `id`, `notify_id`, `channel_id`, `bind_ext` (JSON), `enable_flag`, `tenant_id` |
| `dc3_notify_history` | Notification delivery audit | `id`, `rule_id`, `notify_id`, `message_id`, `channel_id`, `alarm_id`, `channel_type_flag`, `target`, `status_flag` (0=pending, 1=sent, 2=success, 3=failed, 4=retry), `request_ext` (JSON), `response_ext` (JSON), `error_message`, `retry_count`, `tenant_id` |
| `dc3_message` (alarm domain) | Message templates | `id`, `message_name`, `message_code`, `message_level`, `message_ext` (JSON), `enable_flag`, `tenant_id` |
| `dc3_entity_state` | Driver/Device lease state (offline detection) | `id`, `entity_type_flag` (3=driver, 6=device), `entity_id`, `entity_state_flag` (0=unknown, 1=online, 2=offline, 3=fault), `expire_time`, `lease_version`, `last_heartbeat_time`, `last_alarm_id`, `timeout_seconds`, `tenant_id` |
| `dc3_event_history` | Event report records (device-originated) | `id`, `record_id` (UUID), `device_id`, `event_id`, `event_code`, `event_type_flag` (0=info, 1=alert, 2=fault, 3=lifecycle), `event_level_flag` (0=normal, 1=warning, 2=severe, 3=urgent), `param_values` (JSONB), `message`, `occur_time`, `acknowledge_flag`, `tenant_id` |

> Note: `dc3_message` is overloaded — an alarm-domain message-template table (above) and an agentic conversation-turn table (Agentic & MCP section). They live in different schemas/init scripts; keep them distinct when documenting.

### Alarm source → entity alarm flow

```
┌─ Rule Engine (dc3_rule match)
│  └─ Creates: dc3_entity_alarm (alarm_source_flag=0)
│  └─ Tracks: dc3_rule_state (entity_state_flag, alarm_id)
├─ Device/Driver Offline (dc3_entity_state expiry)
│  └─ Creates: dc3_entity_alarm (alarm_type_flag=1, alarm_source_flag=1)
├─ Device Report (device → driver → data-center)
│  └─ Creates: dc3_entity_alarm (alarm_type_flag=2, alarm_source_flag=2)
├─ Driver Report (driver → data-center)
│  └─ Creates: dc3_entity_alarm (alarm_type_flag=3, alarm_source_flag=3)
└─ Event Report (device event → dc3_event_history → rule eval)
   └─ Creates: dc3_entity_alarm (alarm_source_flag=5, target_type=3:EVENT)
```

### Rule → notify chain

```
dc3_rule (enable_flag=0, notify_id, message_id)
  ├─ notify_id → dc3_notify → dc3_notify_channel_bind → dc3_notify_channel
  └─ message_id → dc3_message

Trigger: dc3_rule_state.entity_state_flag change
  → AlarmRuleTriggerService.processXxx()
  → dc3_entity_alarm INSERT
  → dc3_notify_history INSERT (async via RabbitMQ)
  → channel delivery (email/SMS/webhook)
```

### Source filtering via index design

- `idx_entity_alarm_source_time` (`tenant_id`, `alarm_source_flag`, `create_time DESC`) — filter by RULE/TIMEOUT/DEVICE/DRIVER/EVENT
- `idx_entity_alarm_target` (`tenant_id`, `alarm_target_type_flag`, `entity_id`, `create_time DESC`) — filter by POINT/DEVICE/DRIVER/EVENT
- `idx_rule_eval` (`tenant_id`, `alarm_target_type_flag`, `enable_flag`, `entity_id`) — hot-path rule candidate lookup

### Event history vs entity alarm

| Aspect | `dc3_event_history` (model) | `dc3_entity_alarm` (runtime) |
|--------|------------------------------|------------------------------|
| Definition | Event definition in Profile (`dc3_event` table) | Alarm record from any rule/state trigger |
| Originator | Device actively reports via `EventReportDTO` | Rule engine, state timeout, device/driver/event report |
| Record table | `dc3_event_history` (raw log) | `dc3_entity_alarm` (unified) |
| Flow | Device → `EventReportReceiver` → `dc3_event_history` → `AlarmRuleTriggerService` | Multiple sources → `AlarmEventRecordService.ensureEvent()` → `dc3_entity_alarm` |
| State tracking | `acknowledge_flag` (0=unack, 1=acked) | `confirm_flag` (0/1) + `dc3_rule_state` lifecycle |
| Lifecycle | Single creation; ack later | Linked to `dc3_rule_state`; tracks trigger_count, first/last times, recovery |

### Alarm type vs source flags

**Alarm Type** (what happened): 0=RULE (rule-engine match), 1=OFFLINE (heartbeat timeout), 2=FAULT (device internal fault report), 3=STATE_FLIP (entity state change), 4=REPORT (external event report).

**Alarm Source** (where from): 0=RULE, 1=STATE_TIMEOUT (lease expired), 2=DEVICE_REPORT (embedded in value update), 3=DRIVER_REPORT, 4=SYSTEM (e.g. config change), 5=EVENT_REPORT (device event → `dc3_event_history` → rule).

### Source files

- SQL: `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/dependencies/postgres/initdb/03-iot-dc3-data.sql:54-820`
- Java models: `EntityAlarmDO.java`, `RuleDO.java`, `RuleStateDO.java` under `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/model/`
- Enums: `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/enums/{AlarmTargetTypeEnum,AlarmTypeEnum,AlarmSourceTypeEnum}.java`
- Design docs under `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/docs/superpowers/design/`: `entity-alarm.md`, `rule-alarm-optimization.md`, `event-report.md`

---

## Agentic & MCP

IoT DC3 ships a production-grade agentic + MCP (Model Context Protocol) stack.

### Agentic Center (`dc3-center-agentic`)

**OpenAI-compatible chat:**
- `POST /v1/chat/completions` — `ChatController.java:76`. Streaming (SSE) + non-streaming JSON. `@PreAuthorize("@perm.can('chat', 'list')")`. x-dc3-ai metadata: `riskLevel=MEDIUM, destructive=false, idempotent=false, openWorld=true`.

**Built-in tools (10):** `TenantTool`, `UserTool`, `DeviceTool`, `DriverTool`, `ProfileTool`, `PointTool`, `PointValueTool`, `SystemTool`, `CommandTool`, `EventTool` (wired in `ChatClientConfig.java:106-117`). Spring AI `MethodToolCallbackProvider` + custom `AgenticToolTracingCallbackProvider`; `ToolCallAdvisor` with `streamToolCallResponses=false`.

Representative methods: `TenantTool.getTenant()`, `UserTool.getUser()`, `DeviceTool.getDevice()/listDevices()`, `DriverTool.getDriver()/listDrivers()`, `ProfileTool.getProfile()/listProfiles()`, `PointTool.getPoint()/listPoints()`, `PointValueTool.getLatestValue()/getHistory()`, `SystemTool.getSystemHealth()`, `CommandTool.getCommand()/listCommands()`, `EventTool.getEvent()/listEvents()`.

**Chat memory & persistence (`dc3_agentic` schema):**
- `dc3_session` — conversation metadata (`conversation_id` unique, `title`, `session_ext` JSON: model/reasoning/temperature/maxTokens/requireConfirmation, `tenant_id`, `user_id`).
- `dc3_message` (agentic) — persisted turns (`conversation_id`, `role` user/assistant/system, `content` JSON, `model`, `message_index`, `status`, `tenant_id`, `user_id`).
- `dc3_attachment` — uploaded files (`conversation_id`, `file_name`, `content_type`, `size`, `file_path`, `tenant_id`, `user_id`).
- `MessageChatMemoryRepository` adapter reads/writes per-conversation history from `dc3_message`; window via `dc3.agentic.historyWindowSize` (default 30).

**`AgenticProperties` (in-code defaults):** `dc3.agentic.enabled`=true, `memoryEnabled`=true, `memoryMaxMessages`=50, `historyWindowSize`=30, `toolCallingEnabled`=true, `fallbackToolCallingEnabled`=true, `fallbackVisionEnabled`=true, `fallbackReasoningEnabled`=false, `attachmentStoragePath`=`dc3/data/upload/agentic/attachment`.

### MCP Gateway Resource Server (`dc3-gateway`)

- `POST /mcp` — `McpGatewayController.java`. JSON-RPC 2.0 over HTTP (Streamable). Methods: `initialize`, `ping`, `tools/list`, `tools/call`, `notifications/initialized`.
- `GET /.well-known/oauth-protected-resource` — RFC 9728 Protected Resource Metadata.
- **`McpGatewayProperties`:** `dc3.mcp.gateway.resource` (default `http://localhost:8000/mcp`), `dc3.mcp.gateway.authorizationServer` (default `http://localhost:8000`), `dc3.mcp.gateway.backendBaseUrls` (service_name→base_url map for auth/manager/data/agentic).

### OAuth 2.1 Authorization Server (`dc3-center-auth`)

Contracts: `OAuthMcpRuntimeService.java`; impl `OAuthMcpRuntimeServiceImpl.java:124`.

**Endpoints:** `POST /oauth2/authorize` (Auth Code + PKCE), `POST /oauth2/token`, `GET /.well-known/oauth-authorization-server`, `GET /oauth2/jwks` (RS256), `POST /oauth2/revoke` (RFC 9700 replay detection), `POST /oauth2/register` (Dynamic Client Registration, admin-gated), `POST /oauth2/introspect` (gRPC, internal — for Gateway MCP Resource Server).

**Token types:** access_token (short-lived JWT, default 15 min; claims iss/aud/exp/nbf/sub=principal_id/principal_type/scope/tenant_id/mcp_connection_id); refresh_token (rotated, default 30 days, replay detection via `previous_refresh_token_hash`); authorization_code (5-min TTL, PKCE-bound, one-time); client_credentials (SERVICE_ACCOUNT, no refresh).

**Security baseline:** PKCE S256 mandatory for public clients; redirect URI exact match (no wildcards); refresh-token rotation (RFC 9700 §6.3); client secret hashed (never plaintext). Scopes: `mcp:tools:list`, `mcp:tools:call`, `mcp:tools:call:high`, `mcp:resources:read` (reserved, not implemented).

**`OAuthProperties`:** `dc3.oauth.issuer` (default `http://localhost:8300/auth`), `audience` (`dc3-mcp`), `authorizationCodeTtl` (PT5M), `accessTokenTtl` (PT15M), `refreshTokenTtl` (P30D), `jwt.privateKey` (base64 PKCS8, ephemeral if blank), `jwt.publicKey` (base64 X509, ephemeral if blank).

### MCP tool catalog & runtime

**Aggregation** (`McpOpenApiAggregator.java`): pulls OpenAPI from 4 centers (auth/manager/data/agentic); merges with `dc3_api` (api_code, api_name) + `dc3_resource` (resource_code, permission_code); generates stable `tool_id` = `{service_name}:{HTTP_METHOD}:{api_path}`; derives x-dc3-ai metadata (riskLevel, readOnlyHint, destructiveHint, idempotentHint, openWorldHint). Refresh: `@Scheduled(fixedDelayString=${dc3.mcp.tool.refresh-interval:PT5M})` + event-driven `McpToolCatalogChangedEvent` (AFTER_COMMIT on `dc3_api`/`dc3_resource` mutation).

**`tools/list` filter chain:** ① principal RBAC (`PermissionProvider.listPermissionCodes`) ② MCP whitelist (`dc3_mcp_connection_tool WHERE enable_flag=0`) ③ risk policy (HIGH hidden unless explicitly enabled) → intersection.

**Tool metadata (`McpToolRecord`):** tool_id, tool_name (e.g. `manager_device_add`), tool_title, remark, permission_code, risk_level (LOW/MEDIUM/HIGH), read/destructive/idempotent/openWorld hints (0/1), schema_hash = md5(api_code||':'||api_name), tool_ext (JSON inputSchema/outputSchema).

**Scale:** ~150+ tools auto-generated from 302+ OpenAPI operations across 4 centers. risk_level derived from verb semantics (delete→HIGH, add/update→MEDIUM, get/list→LOW); read_only_hint from HTTP method (GET→1, POST→0).

### HIGH-risk two-phase confirmation

- First `tools/call` without valid confirm → `CONFIRM_REQUIRED` + `confirmId` (UUID), `ttl` default PT5M.
- Client re-calls with `confirmId` + `idempotency_key`. Server verifies: not expired, `parameter_digest` matches, principal/connection/tool unchanged, single-use consumption.
- Table `dc3_mcp_tool_confirmation` (`confirm_id`, `tool_id`, `parameter_digest`, `idempotency_key`, `status` PENDING/CONSUMED/EXPIRED, `ttl_expires`). Config `dc3.mcp.confirm-ttl` (default PT5M).
- Every HIGH-risk call audited in `dc3_mcp_audit_log` (`confirm_id`, `idempotency_key`, `argument_digest`).

### Persistence tables (`dc3_auth` schema; line refs into `02-iot-dc3-auth.sql`)

| Table | ~Line | Key columns |
|-------|-------|-------------|
| `dc3_oauth_registered_client` | 1098 | client_id, client_name, client_type (PUBLIC/CONFIDENTIAL), owner_principal_id, service_account_principal_id, tenant_id, client_secret_hash, client_secret_expires_at, authorization_grant_types, redirect_uris, scopes, jwks_uri, jwk_set, require_pkce, require_consent, enable_flag |
| `dc3_oauth_authorization` | 1206 | registered_client_id, client_id, principal_id, principal_type (USER/SERVICE_ACCOUNT), tenant_id, mcp_connection_id, authorization_grant_type, authorized_scopes, state_hash, authorization_code_hash/_issued/_expires, access_token_jti/_issued/_expires, refresh_token_hash, previous_refresh_token_hash, refresh_token_issued/_expires, token_claims (JSON), token_metadata (JSON), revoked_time, revoke_reason |
| `dc3_oauth_authorization_consent` | 1316 | registered_client_id, client_id, principal_id, tenant_id, scopes, consent_ext (JSON) |
| `dc3_mcp_connection` | 1366 | connection_name, client_id, principal_id, principal_type, tenant_id, grant_type, enable_flag, expire_time, revoke_time, last_used_time, connection_ext (JSON) |
| `dc3_mcp_tool_catalog` | 1452 | tool_id (unique), tool_name, tool_title, tool_category, service_name, api_code, permission_code, http_method, api_path, schema_hash, risk_level, read/destructive/idempotent/open_world_hint, enable_flag, tool_ext (JSON) |
| `dc3_mcp_connection_tool` | 1560 | connection_id, tool_id, enable_flag |
| `dc3_mcp_audit_log` | 1619 | trace_id, tenant_id, principal_id, principal_type, client_id, connection_id, tool_id, tool_name, permission_code, risk_level, confirm_id, idempotency_key, argument_digest, status, error_code, duration_ms, client_name, client_version, remote_ip, audit_ext (JSON) |
| `dc3_mcp_tool_confirmation` | 1710 | confirm_id (UUID), tool_id, parameter_digest, idempotency_key, status, ttl_expires |

### Internal authorization flow

```
AI Agent ──Auth Code + PKCE──▶ auth: /oauth2/authorize
  (user login + consent + MCP connection select)
        ──▶ auth: /oauth2/token  (JWT access + refresh)
Agent holds access_token, refresh_token, mcp_connection_id
  ──POST /mcp  Bearer<access_token>──▶ gateway: McpGatewayController
        ──introspect (gRPC)──▶ auth: OAuthMcpRuntimeService.introspect()
              verify JWT claims; check dc3_mcp_connection enable/expire/revoke
              ◀── McpIntrospectResponseDTO {tenantId, principalId, connectionId, scopes, active}
  gateway: dispatch tools/list | tools/call → re-check RBAC + whitelist + risk + confirm
  gateway: McpGatewayClient.invokeBackend() → build X-Auth-Principal + HMAC sign
        ──internal WebClient POST (bypass gateway routing)──▶ manager|data|agentic
              GatewayJwtConverter: verify HMAC, extract principal → @PreAuthorize → R<T>
  gateway: wrap as MCP CallToolResult → return to agent
```

### x-dc3-ai metadata extension

```java
@Extension(name = "x-dc3-ai", properties = {
    @ExtensionProperty(name = "riskLevel",   value = "MEDIUM"),  // LOW/MEDIUM/HIGH
    @ExtensionProperty(name = "destructive", value = "false"),   // breaks data/settings
    @ExtensionProperty(name = "idempotent",  value = "false"),   // safe to retry
    @ExtensionProperty(name = "openWorld",   value = "true")     // accesses external/physical
})
```
Stored in `dc3_mcp_tool_catalog` as `risk_level` + `destructive_hint`/`idempotent_hint`/`open_world_hint`/`read_only_hint` (0/1).

### Implementation status

| Feature | Status | Notes |
|---------|--------|-------|
| OpenAI-compatible `/v1/chat/completions` | IMPLEMENTED | SSE + JSON |
| Agentic tool set (10 tools) | IMPLEMENTED | see list |
| Conversation memory (`dc3_session`, `dc3_message`) | IMPLEMENTED | `MessageChatMemoryRepository`, configurable window |
| MCP Resource Server (`/mcp`) | IMPLEMENTED | `McpGatewayController`, OAuth Bearer validation |
| OAuth 2.1 Authorization Server | IMPLEMENTED | /authorize, /token, /revoke, /register, JWKS |
| PKCE mandatory for public clients | IMPLEMENTED | `require_pkce=1` default |
| Refresh-token rotation (RFC 9700) | IMPLEMENTED | `previous_refresh_token_hash` |
| MCP tool catalog aggregation | IMPLEMENTED | `McpOpenApiAggregator`, ~150+ tools |
| Tool visibility filtering (RBAC ∩ whitelist ∩ risk) | IMPLEMENTED | multi-layer `tools/list` filter |
| HIGH-risk two-phase confirmation | IMPLEMENTED | `dc3_mcp_tool_confirmation`, confirmId + idempotency_key |
| Tool audit logging | IMPLEMENTED | `dc3_mcp_audit_log` |
| Tool refresh scheduling | IMPLEMENTED | scheduled + event-driven (AFTER_COMMIT) |
| MCP notifications (`tools/list_changed`) | DESIGNED, NOT EVENT-PUSHED | RabbitMQ push not implemented; refresh via schedule only |
| MCP resources/prompts | NOT IMPLEMENTED | planned |
| gRPC tool invocation | NOT IMPLEMENTED | HTTP-only (WebClient) by design |

### Key files

- Agentic: `ChatController.java`, `ChatClientConfig.java`, `AgenticProperties.java`, tools dir — under `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/`
- Gateway MCP: `McpGatewayController.java`, `McpGatewayProperties.java` — under `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-gateway/src/main/java/io/github/pnoker/common/gateway/mcp/`
- OAuth: `OAuthMcpRuntimeService.java`, `OAuthMcpRuntimeServiceImpl.java`, `OAuthProperties.java`, `McpOpenApiAggregator.java` — under `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3-common/dc3-common-auth/src/main/java/io/github/pnoker/common/auth/`
- Design: `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/docs/superpowers/design/mcp-server.md`
- Persistence: `/Users/pnoker/Code/pnoker/IoTDC3/github/iot-dc3/dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql` (OAuth + MCP, lines ~1098-1780); `06-iot-dc3-agentic.sql` (session/message/attachment)
