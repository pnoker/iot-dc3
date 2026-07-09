---
title: Glossary
---

# Glossary

This page standardizes terminology across the docs: DC3 platform objects, general IoT terms, and the protocol and
interface identifiers you'll meet in the docs and code. Technical identifiers (class names, table names, routing keys,
HTTP paths, auth headers) are kept verbatim; when a name is ambiguous, this page is the authority.

> You are here: you hit a term while reading [Core Concepts](./concepts) or the operations docs, and came back to look
> it up. Every platform term links to its detail page.

## DC3 Platform Terms

These are IoT DC3's own object model. Their relationships are fixed: drivers connect devices, profiles describe
capabilities, devices bind profiles, points carry data, and the data center stores values and dispatches commands. Each
row gives the canonical name and where to read more.

| Chinese | English · Identifier                  | Description                                                                                                                                                       | Domain         |
|---------|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|
| 驱动      | Driver · `dc3-driver-*`               | A protocol-adapter service instance that talks to devices or data sources. See [Driver](./concepts/driver)                                                        | Device access  |
| 模板      | Profile                               | A device capability template aggregating points / commands / events. See [Profile](./concepts/profile)                                                            | Metadata       |
| 设备      | Device                                | A field-device instance bound to one Profile and one Driver. See [Device](./concepts/device)                                                                      | Metadata       |
| 位号      | Point                                 | A single data item; whether it can be written is decided by the Point's `rwFlag`. See [Point](./concepts/point)                                                   | Metadata       |
| 位号值     | PointValue                            | A collected real-time / historical value (always "PointValue", never "point reading / measurement"). See [Point Value](./concepts/point-value)                    | Data           |
| 网关      | Gateway · `dc3-gateway`               | The sole external HTTP entry point (`8000`); aggregates center routes and injects auth context. See [Services](../architecture/services)                          | Access         |
| 鉴权中心    | Auth Center · `dc3-center-auth`       | Authentication / tenant / RBAC / OAuth. See [Auth · Tenant · RBAC](../architecture/auth-rbac)                                                                     | Center service |
| 管理中心    | Manager Center · `dc3-center-manager` | Metadata management (driver / profile / device / point). See [Services](../architecture/services)                                                                 | Center service |
| 数据中心    | Data Center · `dc3-center-data`       | Point-value persistence and command dispatch. See [Data Plane](../architecture/data-plane)                                                                        | Center service |
| 智能中心    | Agentic Center · `dc3-center-agentic` | LLM chat and tool calling. See [Services](../architecture/services)                                                                                               | Center service |
| 租户      | Tenant · `tenantId`                   | The isolation boundary for business data. See [Tenant](./concepts/tenant)                                                                                         | Cross-cutting  |
| 属性      | Attribute                             | A driver protocol-layer configuration item, registered by the driver from its `application.yml` at startup. See [Attribute & Config](./concepts/attribute-config) | Configuration  |
| 配置      | Config                                | The concrete value a device instance supplies for an attribute. See [Attribute & Config](./concepts/attribute-config)                                             | Configuration  |

::: tip Don't mix "point reading / measurement / point"
Across the docs, use "Point" for the data-item definition and "PointValue" for its runtime value. On first mention, a
center service gets its "Chinese name + identifier"; afterwards either form is fine.
:::

## General IoT Terms

These are common IoT-domain terms, not specific to DC3. Knowing them helps place DC3's objects in the larger picture —
for example, drivers roughly sit between the perception and network layers, while the center services live at the
platform layer.

| Chinese | English · Identifier | Description                                                                                                             | Domain               |
|---------|----------------------|-------------------------------------------------------------------------------------------------------------------------|----------------------|
| 感知层     | Perception Layer     | The bottom IoT layer, where sensors and actuators interact directly with the physical world to collect and act          | Layering             |
| 网络层     | Network Layer        | The layer that transports perception-layer data to the platform over wired or wireless networks                         | Layering             |
| 平台层     | Platform Layer       | The layer that aggregates, stores, and manages devices and data, exposing capabilities to applications                  | Layering             |
| 应用层     | Application Layer    | The layer that consumes platform capabilities for concrete business scenarios (monitoring, scheduling, analytics)       | Layering             |
| 传感器     | Sensor               | A device that converts a physical quantity such as temperature or pressure into a readable electrical or digital signal | Perception           |
| 执行器     | Actuator             | A device that receives control instructions and acts on the physical world (e.g. open a valve, start a motor)           | Perception           |
| RFID    | RFID                 | Radio-frequency identification: contactless reading and writing of electronic tags over radio to identify objects       | Identification       |
| NB-IoT  | NB-IoT               | Narrowband IoT: a cellular standard for low-power, wide-coverage, massive-connection scenarios                          | Network              |
| MQTT    | MQTT                 | A lightweight publish / subscribe messaging protocol, common for device reporting over constrained or unstable networks | Application protocol |
| CoAP    | CoAP                 | The Constrained Application Protocol: a REST-like protocol over UDP designed for low-power constrained devices          | Application protocol |
| LwM2M   | LwM2M                | Lightweight M2M device-management protocol built on CoAP, used for device registration and remote management            | Device management    |
| 边缘计算    | Edge Computing       | Processing data on or near the data source — on devices or gateways — to cut latency and backhaul bandwidth             | Computing paradigm   |
| 雾计算     | Fog Computing        | Distributed processing on network nodes between edge and cloud; an intermediate layer between the two                   | Computing paradigm   |
| 时序数据    | Time-series          | Measurement data keyed by timestamp and produced in time order; point values are the canonical example                  | Data model           |
| 数字孪生    | Digital Twin         | A real-time digital mirror of a physical entity, used for simulation, monitoring, and prediction                        | Modeling             |
| AIoT    | AIoT                 | The convergence of AI and IoT, adding intelligent analysis and decision-making on top of collection and connectivity    | Convergence paradigm |

## Protocol and Interface Identifiers

These identifiers appear directly in the doc examples and the source: the message-bus exchanges, the driver routing key,
the login endpoints, and the auth headers the gateway injects. Treat the source as the authority for exact usage; here
we give each one's role in the pipeline.

| Name · Identifier      | Type                      | Description                                                                                                                      | Domain       |
|------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------|--------------|
| `dc3.driver.code`      | Driver routing identifier | A driver's stable routing identifier used for message-bus addressing; it is a stable identifier and must not be changed casually | Driver       |
| `dc3.e.value`          | RabbitMQ exchange         | The exchange for point-value reporting; a driver wraps a reading as a `PointValue` and sends it here                             | Data flow    |
| `dc3.e.point_command`  | RabbitMQ exchange         | The exchange for read/write command dispatch; the data center routes commands to the target driver through it                    | Command flow |
| `X-Auth-Tenant`        | HTTP auth header          | The tenant identifier carried on protected endpoints, feeding downstream tenant isolation                                        | Auth         |
| `X-Auth-Login`         | HTTP auth header          | The login-identity identifier carried on protected endpoints                                                                     | Auth         |
| `X-Auth-Token`         | HTTP auth header          | The access token carried on protected endpoints                                                                                  | Auth         |
| `POST /token/salt`     | HTTP endpoint (public)    | Login step one: send `tenant` and `name` to get the salt; use it within 5 minutes (server does not enforce the timeout)          | Login        |
| `POST /token/generate` | HTTP endpoint (public)    | Login step two: send `tenant`, `name`, `salt`, and the salt-hashed `password` to get an access token valid for 12 hours          | Login        |

::: info Login is a two-step token exchange
First `POST /token/salt` to get the salt, then hash the password with the salt and `POST /token/generate` to exchange it
for an access token. With the token in hand, protected requests carry `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token`
through the gateway. Treat the source as the authority for exact fields.
:::

## Further Reading

- [Core Concepts](./concepts) — a one-sentence mental model plus an entity-relationship diagram that ties the platform
  terms into an object model
- [Domain Model](../architecture/domain-model) — DO/BO/VO layering and field details
