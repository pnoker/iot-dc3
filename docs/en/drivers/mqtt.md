---
title: MQTT Driver
---

<script setup>
import MqttDiagram from '../../.vitepress/theme/components/MqttDiagram.vue'
</script>


# MQTT Driver

> **`dc3-driver-mqtt` onboards MQTT devices into IoT DC3**—the driver acts as a server, stays subscribed to MQTT topics,
> passively receives payloads that devices publish, parses them
> into [PointValues](../introduction/concepts/point-value),
> and supports downstream write commands by publishing payloads to a command topic. This page explains which broker it
> consumes, how to fill in the point / command / event attributes, where to look when values never arrive, and which
> driver type it is and how far it is implemented.

When you finish, you can onboard a device that reports to its own topic and can be commanded, and know where to look
when the link goes quiet.

## Protocol background

MQTT (Message Queuing Telemetry Transport) is the de-facto lightweight **publish/subscribe** message bus of IoT. It runs
over TCP, default port `1883` (TLS `8883`). Its semantics are the opposite of the fieldbus "master polls every device"
model: devices are not polled—they actively **publish** data to a **topic**, and the platform **subscribes** to those
topics to receive the reports. Publisher and subscriber are decoupled by a **broker** (a message relay server such as
EMQX, Mosquitto, or the RabbitMQ MQTT plugin)—neither needs the other's address, nor must they be online at the same
time. That is what makes the model power-efficient and horizontally scalable for fleets of low-power, wide-area devices.

In the [four-layer IoT reference architecture](../foundations/iot-protocols), MQTT belongs to the **network layer**,
under "application-layer messaging protocols": it defines what a message looks like, how it is delivered, and how
reliable that delivery is, orthogonal to whether the underlying wireless is Wi-Fi or NB-IoT. For the trade-offs between
MQTT and CoAP/LwM2M/HTTP, see the [network-layer chapter](../foundations/iot-protocols).

A few MQTT concepts this driver relies on repeatedly:

- **Topic**: the logical address of a message, e.g. `device/1001/up`. Publishers send to a topic; subscribers receive by
  topic. Subscriptions may use the wildcards `+` (one level) and `#` (any trailing levels).
- **QoS (Quality of Service)**: the delivery guarantee—`0` = at most once, `1` = at least once, `2` = exactly once.
  Higher levels are more reliable but costlier; both publisher and subscriber declare their own, and the weaker side
  prevails.
- **JSON path (Path)**: a dot-notation path to locate a field in a reported payload, e.g. `$.payload` picks `payload`
  under the root, `$.eventCode` picks the event-code field.

Unlike Modbus or HTTP drivers that actively connect to devices, this driver is a *
*[Driver](../introduction/concepts/driver) of type `DRIVER_SERVER`**: it does not actively "read" devices but stays
subscribed and waits for devices to push data up. It therefore has **no device-level `driver-attribute` table**—which
broker to connect to is deployment-level config (below), not filled per device.

## Attribute configuration

MQTT driver config splits into two layers: the **broker connection** is deployment-level (the whole driver connects to
one broker), while the **point / command / event attributes** are device/point-level (deciding which topic each point
writes to and which topic events come from).

### Broker connection (deployment-level, env vars)

The driver connects to the broker via a `dc3.driver.mqtt.*` block whose values come from deployment environment
variables. Key items:

| Setting             | Env var                                               | Default                                                      | Purpose                                                                                                                                                     |
|---------------------|-------------------------------------------------------|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Broker address      | `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT`               | `dc3-rabbitmq` / `1883` (dev profile `2883`)                 | Broker host and port, assembled into the connection URL (plaintext `tcp://host:port` by default; TLS uses `ssl://host:8883` as a production option)         |
| Username / password | `MQTT_USERNAME` / `MQTT_PASSWORD`                     | `dc3` / empty (the docker-compose stack injects `dc3dc3dc3`) | Auth credentials (auth types: `NONE` / `USERNAME` / `CLIENT_ID` / `X509`); the password falls back to empty in app config and is injected by the deployment |
| Keep-alive          | no env binding (`dc3.driver.mqtt.keep-alive`)         | `15` (s)                                                     | Client heartbeat interval, hard-coded default                                                                                                               |
| Completion timeout  | no env binding (`dc3.driver.mqtt.completion-timeout`) | `3000` (ms)                                                  | Wait timeout for a publish operation, hard-coded default                                                                                                    |
| Batch thresholds    | `MQTT_BATCH_SPEED` / `MQTT_BATCH_INTERVAL`            | `100` / `5`                                                  | Ingest batching: flush at 100 messages or 5 s, whichever comes first                                                                                       |

::: info The broker defaults to the RabbitMQ MQTT plugin
The default MQTT broker is the **RabbitMQ MQTT plugin** (`dc3-rabbitmq`), addressed via `MQTT_BROKER_HOST` /
`MQTT_BROKER_PORT`; the docker-compose stack injects `dc3-rabbitmq:1883` (the dev profile YAML falls back to port
`2883`). **EMQX** is the optional broker in `docker-compose-optional.yml` (host-mapped port `31883`), not the default.
The RabbitMQ MQTT plugin and the platform's internal **RabbitMQ AMQP** (with a separate `dc3.e.mqtt` bridge exchange)
used for inter-service messaging are two protocols on the same broker—when onboarding MQTT devices, point those two env
vars at your actual MQTT broker. Across the public internet, enable TLS (`8883` / X509 certificates).
:::

### Point configuration (`point-attribute`)

On each [Point](../introduction/concepts/point), fill in the point's **command target topic** and delivery quality.
Acquisition is passive via subscription, so point attributes only concern downstream write commands:

| Attribute     | code           | Type   | Default        | Remark                                                                |
|---------------|----------------|--------|----------------|-----------------------------------------------------------------------|
| Command Topic | `commandTopic` | STRING | `commandTopic` | MQTT topic used by the point or device to receive downstream commands |
| Command QoS   | `commandQos`   | INT    | `2`            | QoS level for the downstream command topic                            |

When dispatching a write command, the driver takes `commandTopic` from the point attributes and publishes the value to
it at `commandQos` (falling back to the default QoS if it is missing or an error occurs).

### Write command configuration (`command-attribute`)

A writable point fills in, on its write command, which topic to publish to, which QoS to use, and what the payload looks
like:

| Attribute        | code              | Type   | Default        | Remark                                                       |
|------------------|-------------------|--------|----------------|--------------------------------------------------------------|
| Command Topic    | `commandTopic`    | STRING | `commandTopic` | MQTT topic used by the command to publish downstream payload |
| Command QoS      | `commandQos`      | INT    | `2`            | QoS level for the command publish topic                      |
| Payload Template | `payloadTemplate` | STRING | `{}`           | Payload template rendered with command params                |

When executing a command, the driver substitutes the `${xxx}` placeholders in `payloadTemplate` with the command
parameters (plus context such as `deviceId` / `deviceCode` / `deviceName` / `commandId` / `commandCode` /
`commandName`), publishes the rendered payload to `commandTopic` at `commandQos`, and returns `topic` / `qos` /
`payload` as the execution result.

### Event configuration (`event-attribute`)

When a device reports an event, the driver extracts the "event code" and "event payload" from the subscribed message by
topic and path:

| Attribute       | code            | Type   | Default       | Remark                                                                  |
|-----------------|-----------------|--------|---------------|-------------------------------------------------------------------------|
| Source Topic    | `sourceTopic`   | STRING | `eventTopic`  | MQTT topic used to receive event payload (supports `+` / `#` wildcards) |
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | JSON path used to resolve event code                                    |
| Payload Path    | `payloadPath`   | STRING | `$.payload`   | JSON path used to resolve event payload                                 |

When an arriving topic matches `sourceTopic` (exact or wildcard), the driver resolves the event code via `eventCodePath`
and the payload via `payloadPath`, and for device events whose code matches and that are enabled, assembles an event
report and sends it to the Data Center.

## How data is received

In MQTT, a "read" is not an outbound request but a callback fired when a subscribed message arrives. The diagram below
traces a reported value from device to platform—both device and driver only talk to the broker:

<MqttDiagram lang="en" />

For a payload to become a [PointValue](../introduction/concepts/point-value), it must resolve a `deviceId` and a
`pointId` (otherwise that message is skipped); a parse failure only logs a warn and does not affect other messages.
Batch messages go through `receiveValues()` and are sent together, flushed once a batch threshold (`MQTT_BATCH_SPEED` /
`MQTT_BATCH_INTERVAL`) is hit.

## Troubleshooting

::: warning It is a server—it will not "connect" to devices
`DRIVER_SERVER` means the driver waits for devices to push data, rather than actively polling.
If [PointValues](../introduction/concepts/point-value) never arrive, **first confirm the device is actually publishing
to the subscribed topic** and that the topic strings match exactly on both sides (including case and the level
separators `/`)—rather than checking the driver's "acquisition interval", since scheduled reads are off by default
here (`schedule.read.enable=false`).
:::

- **Broker unreachable**: check that `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` point at a real MQTT broker (default
  `dc3-rabbitmq:1883`) and that `MQTT_USERNAME` / `MQTT_PASSWORD` match the broker account; for public/TLS setups
  confirm the port is `8883` and the certificate matches.
- **Messages arrive but no point values**: a payload must resolve a `deviceId` and a `pointId`, or it is silently
  skipped. Look for the `MQTT point value parse failed` warn in the driver log—usually the reported JSON lacks those two
  fields or is not valid JSON.
- **QoS mismatch causes missed/duplicated messages**: align QoS on the publishing side and the device's subscribing
  side. When `commandQos` is missing or errors, the driver **falls back to the default QoS** so the command is still
  sent, but mismatched levels can still downgrade—use `1` or `2` on both sides for reliable dispatch.
- **Empty payload or unsubstituted placeholders**: a command does not auto-insert the value; author a template with
  placeholders such as `${value}` in `payloadTemplate` (e.g. `{"value":${value}}`). An empty template sends the empty
  object `{}`. Placeholder names must match the command parameter keys to be substituted.
- **Events not received**: confirm the arriving topic matches `sourceTopic` (the `+` wildcard matches exactly one level;
  `#` only at the end), that the code resolved by `eventCodePath` matches the device event's `eventCode`, and that the
  event is enabled.
- **Device shows "online" but no data**: MQTT is passive push, so "nothing for a while" does not mean the link is down.
  Online detection uses lease/keep-alive, not an acquisition interval—health check defaults to cron `0/15 * * * * ?`
  with a `45 second` lease timeout; see [Device](../introduction/concepts/device) for the mechanism.

## How it lands in IoT DC3

- **Driver name / code**: `MQTT Driver` / `MqttDriver`
- **Type**: `DRIVER_SERVER` (the driver acts as a server and passively receives device reports)
- **Capabilities** (consistent with the [driver capability matrix](./matrix)): read `—`, write `✓`, subscribe/report `✓`
  —values arrive passively via subscription, with no active read; commands can be dispatched and events reported.

::: info Implementation status: ingestion, command dispatch, and health check are implemented; `initial()` is a skeleton
Per the `MqttDriverCustomServiceImpl` and `MqttReceiveServiceImpl` source: **data reception** (parse to point value and
forward, event report with topic matching), **write commands** (`write()` / `execute()` publishing the payload, QoS
fallback, template rendering), and the **`health()` check** (listening to `MqttSubscribedEvent` /
`MqttConnectionFailedEvent` to reflect the broker connection state in real time) are implemented; `read()` returns
`null` by pub/sub semantics (data arrives passively via subscription—not a defect). The only reference stub left is
`initial()`, an empty initialization template.
:::

Minimal onboarding example—onboard a device that reports to `device/1001/up` and receives commands on
`device/1001/down`:

1. At deployment, point `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` at your broker (default `dc3-rabbitmq:1883`, the
   RabbitMQ MQTT plugin), and create a [Device](../introduction/concepts/device) with `MQTT Driver` (this driver has no
   driver attributes to fill).
2. Add a writable [Point](../introduction/concepts/point) to the [Profile](../introduction/concepts/profile) bound to
   the device, and set the point attributes `commandTopic=device/1001/down`, `commandQos=1`.
3. Once the device publishes data to the subscribed topic, the [PointValue](../introduction/concepts/point-value) is
   received passively; when a write command is dispatched, the driver renders the payload per `payloadTemplate` and
   publishes it to `device/1001/down`.

For the full flow, see [Device Onboarding](../operation/device-onboarding).

## Further reading

- [Drivers overview](./index) — the full landscape and categories of the 28 drivers
- [Driver capability matrix](./matrix) — read/write/subscribe capabilities at a glance
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Network layer: IoT protocols](../foundations/iot-protocols) — trade-offs between MQTT and CoAP/LwM2M/HTTP
- [CoAP Driver](./coap) — a lightweight request/response protocol for constrained endpoints
