---
title: "Domain Model: DO / BO / VO and Object Relationships"
---

<script setup>
import DomainModelErDiagram from '../../.vitepress/theme/components/DomainModelErDiagram.vue'
import DomainModelLayerDiagram from '../../.vitepress/theme/components/DomainModelLayerDiagram.vue'
import DomainModelClassDiagram from '../../.vitepress/theme/components/DomainModelClassDiagram.vue'
import DomainModelSequenceDiagram from '../../.vitepress/theme/components/DomainModelSequenceDiagram.vue'
</script>


# Domain Model: DO / BO / VO and Object Relationships

This page is for anyone writing code on the platform. It maps out how Profile, Point, Command, Event, Device, and Driver
fit together; untangles the "three-tier configuration" (Param / Attribute / Config) that trips people up most; and shows
how a value moves between the DO, BO, and VO tiers through MapStruct `*Builder`. Read it once and you'll know how to add
fields and enums correctly, and what shape a value takes anywhere in a `*Controller → *Service → *Manager` call chain.

> You are here: you've already seen the object relationships in [Core Concepts](../introduction/concepts), and now
> you're going one level deeper into fields and tiers. From here you can move on to the [Data Plane](./data-plane) (how
> point values are persisted) or [Driver Authoring](../development/driver-authoring) (turning these objects into a real
> driver).

## Everything starts with Profile

The IoT DC3 domain model has one root: the **template Profile**. A Profile is not a device — it's a capability manifest
for a class of devices. It declares which **Points** that class can read and write, which custom **Commands** it
supports, and which **Events** it reports. Put the capabilities on the template, and a device inherits the whole set by
binding to it. You don't redefine them device by device.

A **Device** is the platform-side mirror of one physical device. It makes two bindings: it binds to a Profile (which
decides its capabilities) and to a **Driver** (which decides the protocol it speaks). There's a hard constraint that
landed after Phase 1: `DeviceDO.profileId` is a **single foreign key** (one `Long`). The early many-to-many
`ProfileBind` is gone — a device binds to exactly **one template**.

::: danger A device and a template are one-to-one; stop designing for many-to-many
`dc3_device.profile_id` is a single-valued foreign key (`DeviceDO.java`). When you write queries that ask "where do this
device's capabilities come from?", frame them as "device → one Profile". Don't assume a device can carry multiple
templates.
:::

A Point is the smallest unit of data. Two flags decide what it can do:

- `pointTypeFlag` (`PointTypeEnum`) — the value's data type.
- `rwFlag` (`RwTypeEnum`) — the read/write direction. **Whether a point is writable is decided by its own `rwFlag`, not
  by the command table.** Writing to a `READ_ONLY` point is rejected during command validation.

A point also carries engineering-quantity metadata: `unit`, `valueDecimal` (decimal precision, default `6`), and the
linear conversion `baseValue` / `multiple`. The driver applies these to turn a raw collected value into an engineering
value (the semantics are `engineering value = raw value × multiple + baseValue`).

::: info There are actually 8 point-type enums, not 4
For readability, `introduction/concepts` and the Add Point API table list only `STRING / INT / FLOAT / DOUBLE`. In the
source, `PointTypeEnum` actually has 8 values:
`STRING(0) / BYTE(1) / SHORT(2) / INT(3) / LONG(4) / FLOAT(5) / DOUBLE(6) / BOOLEAN(7)` (`PointTypeEnum.java`). `rwFlag`
maps to `RwTypeEnum`: `READ_ONLY(0) / WRITE_ONLY(1) / READ_WRITE(2)`. The code is the source of truth.
:::

### Domain entity relationships

The diagram pulls the root Profile, its three sub-capabilities, the device and driver bindings, and the attribute/config
relationships from the "three-tier configuration" into one picture. It's more complete than the one
in [Core Concepts](../introduction/concepts) — it also shows the protocol-tier `*Attribute` and the instance-tier
`*AttributeConfig`.

<DomainModelErDiagram lang="en" />

`profileShareFlag` (`ProfileShareTypeEnum`: `TENANT / DRIVER / USER`) controls a template's sharing scope. An `Event`'s
`event_type_flag` (`0=info / 1=alert / 2=fault / 3=lifecycle`) classifies the event **definition** and lives in the
`dc3_event` table (the management domain, created by `04-iot-dc3-manager.sql`). People often confuse this with alarms —
covered separately below.

## Three-tier configuration: Param, Attribute, and Config each own their slice

This is the most-misunderstood part of the domain model. The platform splits "configuration" into **three tiers with
different scopes**. Each tier answers a different question, is produced by a different person or process, and maps to a
different DO class:

<DomainModelLayerDiagram lang="en" />

- **Param (business tier)** — `CommandParamDO` / `EventParamDO`. Describes the input/output params of a command or event
  in the template. It's **business semantics**, independent of any specific protocol.
- **Attribute (protocol tier)** — `DriverAttributeDO` / `PointAttributeDO` / `CommandAttributeDO` / `EventAttributeDO`.
  **Registered by the driver at startup.** The driver reads its own `application.yml` and tells the management center, "
  here are the config items my protocol needs." A Modbus driver, for example, declares "a point needs a register
  address" — that's an Attribute. It defines **which items exist**, with no values.
- **Config (instance tier)** — `PointAttributeConfigDO` (plus `DriverAttributeConfigDO` / `CommandAttributeConfigDO` /
  `EventAttributeConfigDO`). Stores the **concrete values** that **this device** fills in for those attributes. The core
  fields of `PointAttributeConfigDO` are exactly `attributeId` (which attribute) + `deviceId` + `pointId` +
  `configValue` (the value). So "device #3's temperature point has register address 40001" — `40001` lives here.

In one line: **Attribute says "there's a slot"; Config says "what goes in the slot."** Once that clicks, the "configure
point attributes" step in [Device Onboarding](../operation/device-onboarding) makes sense, and so does what
`POST /api/v3/manager/point_attribute_config/add` actually writes (its request fields are exactly `attributeId` /
`deviceId` / `pointId` / `configValue`).

## Three shapes of the same data: DO / BO / VO

A domain object takes three forms in the system, one per tier and per concern. Using a point as the example: `PointDO` /
`PointBO` / `PointVO`.

- **DO (`*DO`, e.g. `PointDO`) — the database shape.** It mirrors the `dc3_point` table. Flags are raw `Byte` (
  `pointTypeFlag`, `rwFlag`, `enableFlag` are all `Byte`), with MyBatis-Plus annotations `@TableName` /
  `@TableId(type = ASSIGN_ID)` (Snowflake ID) / `@TableLogic` (logical delete on `deleted`) / `JacksonTypeHandler` for
  JSON extensions. DOs live only in the persistence tier. **Raw `Byte` flags must not leak into the business tier or
  external responses.**
- **BO (`*BO`, e.g. `PointBO`) — the business shape.** The same flags become **domain enums**: `pointTypeFlag` is
  `PointTypeEnum`, `rwFlag` is `RwTypeEnum`, `enableFlag` is `EnableFlagEnum`. A BO extends `BaseBO` and implements
  `TenantOwned` (carrying `tenantId`, the starting point of tenant isolation). Business code and inter-Service calls
  pass BOs, not VOs. Conversion fields use `BigDecimal` in the BO (`baseValue` / `multiple`) and become `Double` when
  persisted to the DO — the `*Builder` handles that precision boundary too.
- **VO (`*VO`, e.g. `PointVO`) — the API shape.** Controller requests and responses use VOs. Like the BO, it uses domain
  enums, unless a raw numeric value has to be kept for backward compatibility with old clients.

The diagram shows where the three tiers sit in the call chain and the conversion directions handled by the MapStruct
`*Builder`.

<DomainModelClassDiagram lang="en" />

When `PointController` receives a `PointVO`, it calls `PointBuilder.buildBOByVO()` to get a `PointBO` and hands that to
`PointService`. The Service calls `buildDOByBO()` to get a `PointDO` and hands that to `PointManager` for persistence.
Reads go the other way: `buildBOByDO()` → `buildVOByBO()`. Raw Mapper methods like `select*` appear only in
`*ManagerImpl`. Service and Controller always use `get*` / `list*` / `add` / `update` / `delete` (see the CRUD verb
convention in the [API Documentation](../development/api-documentation)).

## Enums and JSON extensions: `@AfterMapping` is the key

MapStruct maps same-name, same-type fields automatically. It does not handle the `Byte ↔ domain enum` or
`JSON string ↔ extension object` conversions — you write those by hand in the `*Builder`'s `@AfterMapping` hooks. That's
exactly what keeps the DO/BO/VO layering from leaking.

The contract on both ends of an enum is fixed: the `Byte` stored in the DO is the `index` annotated with `@EnumValue` on
the enum. DO→BO uses `XxxEnum.ofIndex(byte)` to turn the number into an enum; BO→DO uses `enum.getIndex()` to get the
number back. The sequence below is what happens in `PointBuilder` when it reads a row of point data:

<DomainModelSequenceDiagram lang="en" />

Mapped onto the real code in `PointBuilder.java`: in `buildBOByDO`, `pointTypeFlag` / `rwFlag` / `enableFlag` are marked
`@Mapping(ignore = true)` and then assigned one by one in `@AfterMapping` via
`RwTypeEnum.ofIndex(entityDO.getRwFlag())`. Going the other way, `buildDOByBO`'s `@AfterMapping` uses
`Optional.ofNullable(rwFlag).ifPresent(v -> entityDO.setRwFlag(v.getIndex()))`. Null safety is explicit — a null enum is
simply not written, and no NPE is thrown.

JSON extensions work the same way. `PointDO.pointExt` is a `JsonExt` (`content` stored as a JSON string, persisted with
`JacksonTypeHandler`), and becomes the strongly typed `PointExt` in the BO. In `@AfterMapping`, DO→BO calls
`JsonUtil.parseObject(content, PointExt.Content.class)` to deserialize, and BO→DO calls `JsonUtil.toJsonString(...)` to
serialize. Every extension object carries the `BaseExt` trio: `type` (identifies the subtype during parsing),
`version` (optimistic lock, default `1`), and `remark`.

::: tip When adding a new field with an enum or JSON extension

1. Add a `Byte` field + `@TableField` to the DO, and the matching **enum** field to the BO and VO.
2. On the `*Builder`, add `@Mapping(target = "xxx", ignore = true)` for that field in both DO↔BO directions.
3. In both `@AfterMapping` hooks, add the `ofIndex` / `getIndex` conversion. For JSON extensions, add `parseObject` /
   `toJsonString`.

Skip steps 2 and 3 and MapStruct either fails to compile on a type mismatch or silently drops the value. After editing,
always run `mvn -s .mvn/settings.xml -q -DskipTests compile` as a safety net.
:::

## Enum naming: the suffix tells you the semantics

The platform's flag enums encode their semantics in their suffix. There's one convention per kind:

| Suffix        | Semantics          | Example                                               |
|---------------|--------------------|-------------------------------------------------------|
| `*FlagEnum`   | 0/1 toggle         | `EnableFlagEnum` (`ENABLE(0)` / `DISABLE(1)`)         |
| `*StatusEnum` | state machine      | `PointCommandStatusEnum` (`PENDING → SENT → ...`)     |
| `*TypeEnum`   | classification set | `PointTypeEnum`, `RwTypeEnum`, `ProfileShareTypeEnum` |

::: warning In `EnableFlagEnum`, 0 means "enabled", not "disabled"
The index of `ENABLE` is `0` and `DISABLE` is `1` (`EnableFlagEnum.java`). It's natural to read 0 as false/off, but here
it's reversed. When you read SQL, `enable_flag = 0` means **enabled**.
:::

## dc3_event is the definition, dc3_entity_alarm is the instance

The last easy-to-confuse point — a classic "model vs. runtime instance" split in domain modeling:

- `dc3_event` (management domain, `04-iot-dc3-manager.sql`) — the event **definition**. It hangs under a Profile and
  describes "what kinds of events this class of device can report", carrying `event_type_flag` (
  `0=info / 1=alert / 2=fault / 3=lifecycle`). It's part of the template's capabilities, on the same level as Point and
  Command.
- `dc3_entity_alarm` (data domain, `03-iot-dc3-data.sql`) — the runtime **alarm instance**. It's a record produced at
  runtime by several sources — the rule engine, status timeouts, and device/driver/event reporting — distinguished by
  `alarm_source_flag`.

Put another way: `dc3_event` answers "what this device **can** report"; `dc3_entity_alarm` answers "what it **is**
reporting right now". The two tables live in different schemas and different init scripts. Don't treat them as the same
thing when adding queries. For the full model of alarms and events — rules, notification channels, status tracking —
see [Alarms and Notifications](../operation/alarms).

## Further reading

- [Core Concepts](../introduction/concepts) — if you haven't read it, start with the simpler object-relationship diagram
  and one-line mental model
- [Data Plane](./data-plane) — how `PointValue` moves and gets persisted, layer by layer, from `ReadPointValue` to
  `PointValueDO`
- [Driver Authoring](../development/driver-authoring) — how a driver registers Attributes and turns domain objects into
  a real protocol adapter
- [API Documentation](../development/api-documentation) — the `get`/`list`/`add`/`update`/`delete` verb convention and
  OpenAPI
- [Alarms and Notifications](../operation/alarms) — the sources, rules, and notification path of `dc3_entity_alarm`
