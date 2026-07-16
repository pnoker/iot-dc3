---
title: Command
---

<script setup>
import CommandRelationDiagram from '../../../.vitepress/theme/components/CommandRelationDiagram.vue'
import CommandFlowDiagram from '../../../.vitepress/theme/components/CommandFlowDiagram.vue'
</script>

# Command

> **A Command is one action request issued to a device**—restart, calibrate, switch mode, set temperature… Its
> definition belongs to a [Profile](./profile), its invocation belongs to a [Device](./device), it carries a set of
> input/output parameters, and it is executed by a [Driver](./driver) which returns a result receipt.

A Command answers "make the device do something". It is the dual of an [Event](./event): events flow up (the device
says "this happened"), commands flow down (the platform says "go do this").

## What it is and why it exists

Beyond "reporting values" and "having a single quantity read/written", industrial devices also need to be triggered to
perform **action-type capabilities**: restart, firmware upgrade, mode switch, push a block of configuration from a
template. Such actions often take parameters, need a receipt, and require timeout handling and auditing—modeling them as
a structured sub-resource under a `Profile` is the **Command**. The definition lives in the thing model (what actions
this kind of device can perform, what parameters each action takes); the invocation lands on a concrete device
instance (a given device executed it once at a given moment).

### The key distinction: do not confuse the two downlinks

DC3 has two independent downlink paths, and this is what beginners most often confuse:

| Dimension         | Write Point (PointCommand)                                | Custom Command (Command / CommandCall)    |
|-------------------|-----------------------------------------------------------|-------------------------------------------|
| What changes      | A **single quantity** (the value of one [Point](./point)) | Triggers **one parameterized action**     |
| Definition source | The [Point](./point)'s `rwFlag` includes WRITE            | The standalone `dc3_command` table        |
| Driver interface  | `DriverCustomService.write()`                             | `DriverCommand.execute()`                 |
| Parameters        | Single value (one target value)                           | Structured input/output parameter Map     |
| DTO               | `PointCommandDTO`                                         | `CommandCallDTO` / `CommandCallResultDTO` |
| Queue prefix      | `dc3.e.point_command`                                     | `dc3.e.command`                           |

The boundary in one line (see design doc point-command.md §1.2): **writing a point is runtime access on the attribute
dimension; a custom command is an action capability at the thing-model layer.**

::: tip One example to nail the boundary
Telling an air conditioner to "set the target temperature to 26 °C"—this is **writing a point**: `targetTemp` is a
writable Point, you just write `26`; in essence you change a quantity.
Telling an air conditioner to "run a self-clean cycle"—this is the **custom command** `selfClean`: it does not map to
any quantity but to an action, possibly with parameters (e.g. `duration=30`), and returns a `resultCode` when done.
Rule of thumb: if it boils down to "change the value of some Point", it's writing a point; if it "triggers an action
flow", it's a command.
:::

This page covers the **custom command**. For writing a point, see [Point](./point).

## Key fields

Command definition `CommandBO` (belongs to a [Profile](./profile), table `dc3_command`):

| Field             | Type            | Meaning                                                                                        |
|-------------------|-----------------|------------------------------------------------------------------------------------------------|
| `commandName`     | String          | Command name (for display)                                                                     |
| `commandCode`     | String          | Command identifier, unique within a `profileId`, matched on invocation (e.g. `setTemperature`) |
| `commandTypeFlag` | CommandTypeEnum | Command type, see below                                                                        |
| `callTypeFlag`    | CallTypeEnum    | Call mode: `sync` / `async`                                                                    |
| `timeout`         | Integer         | Call timeout (seconds)                                                                         |
| `commandExt`      | CommandExt      | Extension config (protocol mapping, driver command template, idempotency, etc.)                |
| `profileId`       | Long            | Owning [Profile](./profile)                                                                    |
| `enableFlag`      | EnableFlagEnum  | Enabled / disabled state                                                                       |
| `tenantId`        | Long            | Owning [Tenant](./tenant)                                                                      |

Command parameter `CommandParamBO` (declares a command's input/output parameters, belongs to a command definition):

| Field                     | Type                   | Meaning                       |
|---------------------------|------------------------|-------------------------------|
| `paramName` / `paramCode` | String                 | Parameter name / identifier   |
| `paramDirectionFlag`      | ParamDirectionTypeEnum | Direction: `input` / `output` |
| `paramTypeFlag`           | PointTypeEnum          | Parameter data type           |
| `requiredFlag`            | Boolean                | Whether required              |
| `defaultValue`            | String                 | Default value                 |
| `commandId`               | Long                   | Owning command definition     |

::: tip CommandParam reuses the Point type system
`paramTypeFlag` uses the same `PointTypeEnum` as points (`STRING` / `INT` / `FLOAT` / `DOUBLE` / `BOOLEAN`…). Mind the
distinction: **input** parameters are supplied by the caller at invocation (e.g. `temperature`), while **output**
parameters are written back by the device after execution (e.g. `resultCode`).
:::

Invocation body `CommandCallBO` (the submit payload for one call): `deviceId`, `commandId`, `commandCode`,
`paramValues` (`Map<String,String>`, keyed by each parameter's `paramCode`).

## Command types

| Type `commandTypeFlag` | Description    |
|------------------------|----------------|
| `custom`               | Custom command |
| `config`               | Config command |
| `action`               | Action command |

## Relationship to other concepts

<CommandRelationDiagram lang="en" />

- A command **definition** hangs under a Profile, alongside [Point](./point) and [Event](./event), together describing "
  what this kind of device can do".
- A command **invocation** is initiated by a [Device](./device); the protocol mapping the driver needs to execute is
  supplied by [Command/Event Attribute Config](./attribute-config) (`CommandConfig`), which sits at a different layer
  from the business `CommandParam`.

## Invocation lifecycle and receipt

One invocation (`CommandCallDTO`) carries: `recordId`, `tenantId`, `deviceId`, `commandId`, `commandCode`,
`paramValues`, `source`, `occurredAt`, `expireAt`. The data center persists it as a `dc3_command_history` record (
PENDING), publishes it to RabbitMQ, and after the driver executes it the driver returns `CommandCallResultDTO` (
`status`, `resultValues`, `errorCode`, `errorMessage`, `finishedAt`).

<CommandFlowDiagram lang="en" />

Invocation state machine (`PointCommandStatusEnum`, shared with write-point):

```
PENDING → SENT → SUCCESS / FAILED / TIMEOUT / EXPIRED / DUPLICATE / DEAD
```

| Status                | Meaning                                                                   |
|-----------------------|---------------------------------------------------------------------------|
| `PENDING`             | Record created, waiting to publish                                        |
| `SENT`                | Published to RabbitMQ, waiting for the driver to execute                  |
| `SUCCESS` / `FAILED`  | Driver executed successfully / failed, result written back                |
| `TIMEOUT` / `EXPIRED` | Application-level timeout / `expireAt` passed before execution            |
| `DUPLICATE` / `DEAD`  | Duplicate command rejected by dedup / rejected into the dead-letter queue |

::: warning A sync command is not "done on call"
`callTypeFlag = sync` only means the caller is willing to wait for the receipt; it **does not mean the HTTP call returns
the execution result immediately**. Today `/call` returns a `recordId`, with which the caller polls `get_by_record_id`
for the terminal status. Whether it is truly "done" is determined by the `status` in the receipt—do not assume the
device executed just because HTTP returned 200.
:::

## Example

In an air conditioner's thing model, define a command: `commandCode = setTemperature`, `commandTypeFlag = action`,
`callTypeFlag = sync`, `timeout = 10`, with one input parameter `temperature` (`paramDirectionFlag = input`,
`paramTypeFlag = DOUBLE`, `requiredFlag = true`) and one output parameter `resultCode` (`output`, `STRING`).

To invoke, submit `CommandCallBO{ deviceId: 1001, commandCode: "setTemperature", paramValues: { temperature: "26" } }`.
The data center checks whether the device's `profileId` contains this command, persists a `dc3_command_history` record (
PENDING→SENT), and publishes it to the driver; the driver's `execute()` renders the protocol payload and sends it to the
air conditioner, then returns `CommandCallResultDTO{ status: SUCCESS, resultValues: { resultCode: "OK" } }`, and the
record advances to SUCCESS.

## API

The data center service is mounted under `/data`:

| Method | Path                                     | Description                                                 |
|--------|------------------------------------------|-------------------------------------------------------------|
| POST   | `/data/command_history/call`             | Issue a custom command, returns `recordId`                  |
| GET    | `/data/command_history/get_by_record_id` | Fetch one call record and its terminal status by `recordId` |
| POST   | `/data/command_history/list`             | Page through call records                                   |

## Further reading

- [Profile](./profile) — command definitions hang under the thing model
- [Point](./point) — the other side of the write-point vs custom-command boundary
- [Event](./event) — the dual of the downlink: commands down, events up
- [Command/Event Attribute Config](./attribute-config) — how `CommandConfig` maps parameters into a protocol payload
- [Command Plane](../../architecture/command-plane) — exchanges / queues / receipts / reliability of the downlink
- [Data and Command Operations](../../operation/data-commands) — how to issue commands from the console
