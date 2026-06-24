---
title: MQTT Driver
---

# MQTT Driver

> **`dc3-driver-mqtt` onboards MQTT devices into IoT DC3**—the driver acts as a server, subscribes to MQTT topics, passively receives device-reported payloads and parses them into point values, and supports downstream commands by publishing payloads to a command topic.

MQTT (Message Queuing Telemetry Transport) is the most common lightweight publish/subscribe protocol in IoT. It runs over TCP, default port `1883` (TLS `8883`). Devices are not polled—they actively **publish** data to a **topic**, and the platform **subscribes** to those topics to receive the reports. It is common where sensors, smart hardware, or gateways aggregate data through a broker (a message relay server such as EMQX or the RabbitMQ MQTT plugin).

A few MQTT concepts this driver relies on repeatedly:

- **Topic**: the logical address of a message, e.g. `device/1001/up`. Publishers send to a topic; subscribers receive by topic.
- **QoS (Quality of Service)**: the delivery guarantee level—`0` = at most once, `1` = at least once, `2` = exactly once. Higher levels are more reliable but also more costly.
- **JSON path (Path)**: a dot-notation path to locate a field in a reported payload, e.g. `$.payload` picks `payload` under the root object.

Unlike Modbus or HTTP drivers that actively connect to devices, this driver is a **[Driver](../introduction/concepts/driver) of type `DRIVER_SERVER`**: it does not actively "read" devices but stays subscribed and waits for devices to push data up. Therefore it has **no device-level `driver-attribute` table**—which broker to connect to is decided by deployment environment variables (`MQTT_BROKER_HOST` / `MQTT_BROKER_PORT`), and no driver attribute is filled on the device.

::: warning This driver is currently a skeleton
In the source, `read()` is a reference stub and `health()` always reports online; protocol-level I/O is not yet fully implemented (see the TODO markers in `MqttDriverCustomServiceImpl`). Treat it as an onboarding template and configuration reference, not a production-ready driver.
:::

## Driver name / code / type

- **Driver name / code**: `MQTT Driver` / `MqttDriver`
- **Type**: `DRIVER_SERVER` (the driver acts as a server and passively receives device reports)

## Point configuration (`point-attribute`)

On each [Point](../introduction/concepts/point), fill in the point's **command target topic** and delivery quality (acquisition is passive via subscription, so point attributes only concern downstream write commands):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Command Topic | `commandTopic` | STRING | `commandTopic` | MQTT topic used by the point or device to receive downstream commands |
| Command QoS | `commandQos` | INT | `2` | QoS level for the downstream command topic |

## Write command configuration (`command-attribute`)

A writable point fills in, on its write command, which topic to publish to, which QoS to use, and what the payload looks like:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Command Topic | `commandTopic` | STRING | `commandTopic` | MQTT topic used by the command to publish downstream payload |
| Command QoS | `commandQos` | INT | `2` | QoS level for the command publish topic |
| Payload Template | `payloadTemplate` | STRING | `{}` | Payload template rendered with command params |

When dispatching a command, the driver renders the `${xxx}` placeholders in `payloadTemplate` with the command parameters (plus context such as `deviceId`/`deviceCode`/`commandCode`), and publishes the rendered payload to `commandTopic` at `commandQos`.

## Event configuration (`event-attribute`)

When a device reports an event, the driver extracts the "event code" and "event payload" by path from the subscribed message:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Source Topic | `sourceTopic` | STRING | `eventTopic` | MQTT topic used to receive event payload |
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | JSON path used to resolve event code |
| Payload Path | `payloadPath` | STRING | `$.payload` | JSON path used to resolve event payload |

## Acquisition & health

- **Acquisition**: scheduled reads are **disabled** by default (`schedule.read.enable=false`)—MQTT data is received passively via subscription, with no periodic polling.
- **Custom task**: the driver ships a built-in custom schedule, default cron `0/5 * * * * ?` (every 5 seconds), for the driver's own periodic logic.
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`—see [Device](../introduction/concepts/device) for the online-state mechanism.

## Minimal onboarding example

Onboard a device that reports to topic `device/1001/up` and receives commands on `device/1001/down`:

1. At deployment, point `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` at your broker (the dev stack defaults to the RabbitMQ MQTT plugin `dc3-rabbitmq:2883`), and create a [Device](../introduction/concepts/device) with `MQTT Driver` (this driver has no driver attributes to fill).
2. Add a writable [Point](../introduction/concepts/point) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `commandTopic=device/1001/down`, `commandQos=1`.
3. Once the device publishes data to the subscribed topic, the [PointValue](../introduction/concepts/point-value) is received passively; when a write command is dispatched, the driver renders the payload per `payloadTemplate` and publishes it to `device/1001/down`.

## Pitfalls

::: warning It is a server—it will not "connect" to devices
`DRIVER_SERVER` means the driver waits for devices to push data, rather than actively polling. If [PointValues](../introduction/concepts/point-value) never arrive, first confirm that the **device is actually publishing to the subscribed topic** and that the topic strings match exactly on both sides (including case and the level separators `/`)—rather than checking the driver's "acquisition interval", since scheduled reads are off by default here.
:::

::: tip QoS must match on both sides; dispatch falls back to the default
When dispatching a command, if `commandQos` is available the driver publishes at that level; if it is missing or an error occurs, the driver **falls back to the default QoS** so the command is still sent. For reliable dispatch, align the QoS of the publishing side and the device's subscribing side (e.g. both use `1` or `2`) to avoid one side downgrading into missed or duplicated messages.
:::

::: tip The Payload Template defines what the dispatched message looks like
A command does not automatically place the value into the payload. You must author a template with placeholders such as `${value}` in `payloadTemplate` (e.g. `{"value":${value}}`) for the driver to substitute the command parameters before publishing. An empty template sends the empty object `{}`.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes & Config](../introduction/concepts/attribute-config) — where attributes like `commandTopic` / `payloadTemplate` come from across the three layers
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [HTTP Driver](./http) — an application-layer REST onboarding where the driver actively issues requests
- [CoAP Driver](./coap) — a lightweight request/response protocol for constrained endpoints
