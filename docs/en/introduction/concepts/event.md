---
title: Event
---

<script setup>
import EventRelationDiagram from '../../../.vitepress/theme/components/EventRelationDiagram.vue'
import EventFlowDiagram from '../../../.vitepress/theme/components/EventFlowDiagram.vue'
</script>

# Event

> **An event is a business occurrence reported proactively by a device**—a fault, an alert, a mode switch, a lifecycle
> change… Its definition belongs to the [Profile](./profile), its instances belong to the [Device](./device); once
> reported it is both persisted as a raw record and able to trigger an alarm.

An event answers "what happened to the device", not "what is the current value of some quantity". The latter is
a [PointValue](./point-value) (a periodically sampled numeric snapshot); the former is a discrete, semantically
meaningful occurrence: for an access-control device, "temperature = 25.3℃" is a PointValue, while "the door was forced
open" is an event.

An event has two layers: the **definition** (which events this kind of device reports, and which parameters each event
carries) lives in the Profile, backed by `dc3_event` / `dc3_event_param`; the **instance** (a specific device actually
reported one at a specific moment) is reported by the driver and lands in `dc3_event_history`.

## Key Fields

Event definition `EventBO` (table `dc3_event`):

| Field            | Type              | Meaning                                                                      |
|------------------|-------------------|------------------------------------------------------------------------------|
| `eventName`      | String            | Event name (for display)                                                     |
| `eventCode`      | String            | Event identifier; reporting and alarm rules match on it (e.g. `DOOR_FORCED`) |
| `eventTypeFlag`  | EventTypeFlagEnum | Event type, see below                                                        |
| `eventLevelFlag` | EventLevelEnum    | Event level, see below                                                       |
| `profileId`      | Long              | The owning [Profile](./profile)                                              |
| `eventExt`       | JSON              | Extended configuration                                                       |

Event parameter `EventParamBO` (table `dc3_event_param`, declares which output parameters an event carries):

| Field                     | Type          | Meaning                     |
|---------------------------|---------------|-----------------------------|
| `paramName` / `paramCode` | String        | Parameter name / identifier |
| `paramTypeFlag`           | PointTypeEnum | Parameter data type         |
| `eventId`                 | Long          | The owning event definition |

::: tip Event parameters reuse the Point type system
`paramTypeFlag` uses the same `PointTypeEnum` as Points (`STRING` / `INT` / `FLOAT` / `BOOLEAN`…); the value range of an
event parameter is exactly identical to a [Point](./point) data type.
:::

## Event Types and Levels

| Type `eventTypeFlag` | Description       |
|----------------------|-------------------|
| `info`               | Information event |
| `alert`              | Alert event       |
| `fault`              | Fault event       |
| `lifecycle`          | Lifecycle event   |

| Level `eventLevelFlag` | `0` LOW | `1` MEDIUM | `2` HIGH | `3` CRITICAL |
|------------------------|---------|------------|----------|--------------|

## Relationship to Other Concepts

<EventRelationDiagram lang="en" />

- The event **definition** hangs under the Profile, side by side with [Point](./point) and [Command](./command),
  together describing "what capabilities this kind of device has".
- The event **instance** is reported by a [Device](./device) through a [Driver](./driver), carrying the `eventCode`,
  level, and a set of parameter values.

## Reporting Path and Lifecycle

<EventFlowDiagram lang="en" />

A single report (`EventReportDTO`) carries: `recordId` (UUID), `deviceId`, `eventId`, `eventCode`, `eventTypeFlag`,
`eventLevelFlag`, `paramValues`, `message`, `occurTime`. The data center first persists it as a **raw record** in
`dc3_event_history`, then submits it to the alarm rule engine; only when a rule matches does it create/update a *
*runtime alarm** in `dc3_entity_alarm`.

::: warning An EventHistory is not an alarm
`dc3_event_history` is the raw record of "what the device said happened"—**logged on every report**; `dc3_entity_alarm`
is the result of "the alarm engine deciding it needs attention"—**present only when a rule matches**. To ask "which
events has a device reported" look at the former; to ask "which alarms exist now" look at the latter. Do not conflate
them.
:::

## Example

In an access-control device's Profile, define an event: `eventCode = DOOR_FORCED`, `eventTypeFlag = alert`,
`eventLevelFlag = 3`, with parameter `openMethod` (String). When the field device is pried open, the driver reports
`EventReportDTO{ eventCode: "DOOR_FORCED", paramValues: { openMethod: "pry" }, occurTime: ... }`; the data center
records it in `dc3_event_history`, and because the level is CRITICAL it matches an alarm rule and creates an alarm in
`dc3_entity_alarm`.

## Reporting API

| Method | Path                                             | Description                              |
|--------|--------------------------------------------------|------------------------------------------|
| POST   | `/data/event_history/report`                     | Report an event                          |
| GET    | `/data/event_history/get_by_record_id?recordId=` | Query event record details by `recordId` |
| POST   | `/data/event_history/list`                       | Paginated query of event records         |

## Further Reading

- [Profile](./profile) — the event definition hangs under the Profile
- [Command](./command) — the downstream dual: events go up, commands go down
- [PointValue](./point-value) — complementary: continuous values vs. discrete occurrences
- [Alarms and Notifications](../../operation/alarms) — how an event becomes an alarm
- [Data Plane](../../architecture/data-plane) — exchanges / queues / reliability details of the upstream path
