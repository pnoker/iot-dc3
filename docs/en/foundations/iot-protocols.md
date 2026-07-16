---
title: IoT Protocols & Wireless Networks
---

<script setup>
import IotProtocolsMqttDiagram from '../../.vitepress/theme/components/IotProtocolsMqttDiagram.vue'
import IotProtocolsWirelessDiagram from '../../.vitepress/theme/components/IotProtocolsWirelessDiagram.vue'
</script>


# IoT Protocols & Wireless Networks

Fieldbuses connect the machines on a shop floor, but the wider Internet of Things—battery-powered sensors, water meters
out in the countryside, smart hardware on the public internet—relies on a different family of "light, frugal,
far-reaching" protocols and wireless technologies. This chapter covers the IoT side of the network layer on two fronts:
the upper-layer **application messaging protocols** (MQTT, CoAP, LwM2M, HTTP, AMQP) and the lower-layer **wireless and
wide-area access** (BLE, Zigbee, LoRa/LoRaWAN, NB-IoT, 5G), plus the "power–bandwidth–range–cost" trade-offs between
them. By the end you will be able to pick the right protocol stack for a class of devices, and know which IoT DC3 driver
each choice maps to.

> You are here: the previous chapter, [Fieldbuses & Industrial Protocols](./fieldbus), dealt with deterministic
> communication close to the field. This chapter steps one level outward, into the world of IoT protocols built for
> massive, low-power, long-range endpoints.

## What This Layer Is / Why It Exists

Back to the [four-layer reference architecture](./): the network layer is responsible for "carrying the signals produced
by the perception layer reliably." Fieldbuses solve connectivity inside the factory walls—cabled, strongly real-time.
But the other half of IoT looks completely different: devices number in the thousands, are scattered across wide areas,
run on batteries, have bandwidth measured in KB, and often sit behind unreliable wireless links and the public internet.
Under such constraints, the classic "master polls every device" model is both power-hungry and unscalable.

So IoT protocols evolved along two main lines. One is the **application messaging protocols**: they define "what a
message looks like, how it is delivered, and how reliable it is," running on top of TCP/UDP, independent of which
wireless carries them underneath. MQTT decouples devices and platforms through publish/subscribe, CoAP compresses HTTP's
request/response model down to a few dozen bytes over UDP, LwM2M layers a device-management object model on top of CoAP,
and HTTP/REST is still widely used because of its ubiquity. The other line is **wireless and wide-area access**: it
decides "how the signal travels through the air," from BLE at a few meters to LoRa at several kilometers, up to the
carrier networks of NB-IoT and 5G.

The two lines are orthogonal: the same MQTT payload can run over Wi-Fi or over an NB-IoT cellular link. The key to
understanding the network layer is to look at "how a message is organized" and "how a signal is transmitted" separately,
then compose them per scenario.

## Key Technologies & Trade-offs

### Application protocols: how messages are organized and delivered

**MQTT** is the de facto messaging bus of IoT. At its core is the **publish/subscribe (pub/sub)** model: a device does
not talk to the platform directly—it **publishes** messages to a **topic**, and the platform receives them by *
*subscribing** to those topics. The two sides are decoupled through an intermediate **broker** (a message relay server
such as EMQX, Mosquitto, or the RabbitMQ MQTT plugin); neither needs the other's address, nor for both to be online at
once. This "pushed by subscription rather than polled" semantics is precisely what makes massive-device scenarios
power-efficient and horizontally scalable.

MQTT describes delivery guarantees with three **QoS (Quality of Service)** levels, declared independently by the
publishing and subscribing sides, with the weaker side prevailing:

- **QoS 0 (at most once)**: fire and forget—no acknowledgement, no retransmission. The cheapest; if it is lost, it is
  lost. Good for high-frequency telemetry that can tolerate gaps.
- **QoS 1 (at least once)**: the receiver must reply `PUBACK`, and unacknowledged messages are resent—no loss, but *
  *possible duplicates**, so the downstream must be idempotent.
- **QoS 2 (exactly once)**: a four-way handshake (`PUBREC`/`PUBREL`/`PUBCOMP`) guarantees no loss and no duplication—the
  most reliable and the heaviest, suited to non-repeatable critical commands.

Two more common mechanisms: **retain** lets the broker cache the "last" message for each topic, so a new subscriber
receives the current value the moment it connects rather than waiting for the next report—ideal for "state" topics; *
*LWT (Last Will and Testament)** lets the broker send a preset message on the device's behalf when it disconnects
abnormally, so the platform can detect the offline state.

On versioning, **MQTT 3.1.1** has long been the mainstream; **MQTT 5.0** adds enhancements such as reason codes (making
errors more diagnosable), user-defined properties, a request/response pattern, and shared subscriptions (multiple
consumers load-balancing the same topic). When selecting, first confirm that both the device firmware and the broker
support the target version—capability is the intersection of the two ends, and one-sided 5.0 support does not
auto-enable its features.

The diagram below shows the basic pub/sub topology—both devices and platform talk only to the broker:

<IotProtocolsMqttDiagram lang="en" />

**CoAP (Constrained Application Protocol)** takes another route: it keeps the familiar HTTP **request/response +
methods (GET/PUT/POST/DELETE) + resource path** model, but squeezes the message down to a few dozen bytes over **UDP** (
default port `5683`, `5684` for CoAPS over DTLS). Connectionless UDP avoids TCP handshakes and keep-alive overhead,
which is extremely friendly to battery-powered endpoints that wake up only occasionally to report; the cost is that
reliability must be added back through CoAP's own CON/NON confirmation mechanism. CoAP also supports the **Observe**
extension, letting a client "subscribe" to a resource so the server pushes on value changes—filling the gap of pure
request/response.

**LwM2M (Lightweight M2M)** does not start from scratch—it is **built on top of CoAP**, supplying the "device
management" layer that CoAP lacks. It abstracts device capabilities into an **object tree**: `Object` (e.g. `3303` =
Temperature) / `Object Instance` (multiple instances of the same kind) / `Resource` (e.g. `5700` = Sensor Value), and
accessing a value means giving the path `/<objectId>/<objectInstanceId>/<resourceId>`. Firmware upgrade, remote
configuration, and subscription reporting are all standardized into this model, which is why LwM2M is common in
carrier-grade endpoints that need remote operation (NB-IoT modules, smart meters).

**HTTP/REST** still has a place in IoT: it was not designed for constrained devices, its headers are bloated, and
keep-alive is costly, so it is **not suitable** for high-frequency reporting from battery endpoints; but it is
universal, easy to debug, and almost every upstream system speaks REST, so the many "pull data from a third-party
platform API or from a gateway with a RESTful interface" scenarios still use it. **AMQP** sits at the other end—it is a
reliable queueing protocol for enterprise messaging middleware (RabbitMQ being its implementation), with heavier
payloads and state machines than MQTT, generally used for reliable message flow **between platform and backend** rather
than direct connection to constrained endpoints.

In short: choose MQTT for high-frequency telemetry and decoupling at scale; CoAP for very constrained, sporadic
reporting; LwM2M when you need to manage devices remotely; HTTP to integrate ready-made REST interfaces; AMQP for
reliable backend queues.

### Wireless and wide-area access: how the signal travels

Application protocols decide "what a message looks like," but a message ultimately lands on a physical link. Choosing a
wireless technology is essentially a **multi-objective trade-off**: the farther it travels, the more power it tends to
use or the slower it gets; the more power-efficient it is, the more bandwidth it sacrifices; unlicensed bands save money
but are prone to congestion. Grouping the mainstream technologies into three coverage tiers makes their niches visible
at a glance—within a tier they further split by rate and power:

<IotProtocolsWirelessDiagram lang="en" />

Quantifying the trade-off into a reference table makes it easier to work backward from a scenario:

| Technology | Range                          | Rate     | Power    | Band               | Typical scenario                                   |
|------------|--------------------------------|----------|----------|--------------------|----------------------------------------------------|
| BLE        | tens of meters                 | low      | very low | 2.4 GHz unlicensed | wearables, beacons, near-field provisioning        |
| Zigbee     | tens~hundreds m (mesh extends) | low      | low      | 2.4 GHz unlicensed | low-rate smart-home/building devices               |
| LoRaWAN    | several km                     | very low | very low | Sub-GHz unlicensed | remote metering, agriculture/environment           |
| NB-IoT     | wide area (carrier)            | low      | low      | licensed           | metering, manhole covers, fixed low-freq reporting |
| 5G         | wide area (carrier)            | high     | high     | licensed           | video, AGVs, remote control                        |

- **BLE (Bluetooth Low Energy)**: typically tens of meters, low rate, extremely power-efficient—running for months to
  years on a coin cell. Suited to wearables, beacons, near-field sensing and provisioning; usually needs a phone or
  gateway to relay onto the internet.
- **Zigbee**: short-range **self-organizing mesh** based on IEEE 802.15.4, where nodes relay for each other to extend
  coverage—power-efficient and suited to large numbers of low-rate devices in smart homes/buildings, aggregated through
  a coordinator/gateway before uplinking.
- **LoRa / LoRaWAN**: the **LPWAN (Low-Power Wide-Area Network)** representative. LoRa is the physical-layer
  modulation (long range, interference-resistant); LoRaWAN is the network protocol above it. Operating in unlicensed
  Sub-GHz bands, it reaches several kilometers in cities and farther in the countryside, at very low rates (hundreds of
  bps to tens of kbps), with extremely frugal endpoints—a typical "wide coverage, low rate, self-built network"
  scenario, such as remote metering, agriculture and environmental monitoring.
- **NB-IoT (Narrowband IoT)**: a carrier cellular LPWAN, running in licensed bands carried by the telecom network—good
  coverage and penetration (basements, manhole covers), power-efficient endpoints, massive connections, but low rate and
  higher latency. No need to build base stations; suited to wide-area, fixed, low-frequency reporting metering
  endpoints, often paired with LwM2M/CoAP.
- **5G**: high bandwidth, low latency and massive connectivity in one, spanning from enhanced mobile broadband to
  industrial-control-grade uRLLC. The most capable, but also the highest in power, module and tariff costs—suited to
  high-value scenarios sensitive to bandwidth/latency such as video, AGVs and remote control, not coin-cell sensors.

To distill the trade-off into one sentence: **there is no "best" wireless, only the "best-matched" one**—first fix the
scenario's range, reporting frequency, battery budget and per-node cost, then work backward to the choice.

## Engineering Notes

- **Decouple protocol from wireless**: selection is two steps—first choose the application protocol by message model (
  pub/sub vs. request/response, whether device management is needed), then choose the wireless/access by physical
  constraints. The two are orthogonal; don't conflate them.
- **Higher QoS is not always better**: the four-way handshake of QoS 2 significantly amplifies overhead and latency
  under weak networks or massive devices. QoS 0/1 suffices for most telemetry; reserve "exactly once" for truly
  non-repeatable critical commands, and make the downstream **idempotent** when using QoS 1.
- **For UDP protocols, check the firewall first**: CoAP/LwM2M run over UDP, and a failure to connect is usually the UDP
  port (`5683`/`5684`) being blocked by a firewall or a broken NAT mapping, rather than a wrong application
  config—verify the link before the app.
- **Passive receipt ≠ "online" even with no data**: pub/sub and Observe are device-initiated pushes. The platform's "
  online" judgement must rest on lease/keep-alive/LWT, not on an "acquisition interval"; a long silence does not
  necessarily mean the link is down, but don't assume it is alive either.
- **Save power on the link**: an endpoint's power budget is dominated by the radio transceiver and keep-alive
  handshakes, not by MCU computation. To cut power, first reduce reporting frequency, use a lighter QoS, and enable deep
  sleep/DRX—rather than optimizing business logic.
- **Encrypt on the public internet**: MQTT and CoAP running over the public internet/cellular must enable TLS (`8883`)
  and DTLS (`5684`) respectively, with proper device identity (certificates/PSK) and topic-level authorization, to
  prevent spoofing or unauthorized subscriptions.

### The convergence trend

Early IoT was a set of "protocol islands"—a proprietary protocol and a dedicated gateway per device kind. The trend is
converging: the application layer is consolidating into the two-strong landscape of **MQTT + CoAP/LwM2M** (MQTT for
high-frequency telemetry, CoAP/LwM2M for constrained devices and management); the access side layers **LPWAN (
NB-IoT/LoRa) and 5G** complementarily, covering the full spectrum from "wide and frugal" to "fast and powerful." The
platform then uses a **unified protocol-adaptation layer** to normalize these heterogeneous accesses into one data
model—which is exactly what the IoT DC3 driver layer does.

## How It Lands in IoT DC3

DC3 implements each of these "light protocols" as a standalone protocol [driver](../drivers/) (`dc3-driver-*`); at
startup it registers itself and the [attributes](../introduction/concepts/attribute-config) it accepts with the manager
center, then acquires data per [Point](../introduction/concepts/point) and writes
per [Command](../introduction/concepts/command). The application protocols in this chapter map to four drivers:

- **[MQTT driver](../drivers/mqtt)** (`dc3-driver-mqtt`): type `DRIVER_SERVER`—it **acts as a server, subscribes to MQTT
  topics and passively receives** device reports rather than actively polling. Downstream has two paths: the point
  `write()` publishes the raw value directly per `commandTopic`/`commandQos`; only the command `execute()` renders the
  command-attribute `payloadTemplate` before publishing. Which broker to connect to is decided by the deployment
  environment variables `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT`, so this driver has **no device-level driver attributes
  **; on the MQTT side it can work with a broker such as **EMQX**, while the docker-compose stack defaults to injecting
  the RabbitMQ MQTT plugin (`dc3-rabbitmq:1883`; the dev profile's YAML port fallback is `2883`).
- **[CoAP driver](../drivers/coap)** (`dc3-driver-coap`): type `DRIVER_CLIENT`, based on Eclipse Californium, actively
  connecting to devices—reads issue a GET to the point's `readPath`, writes a PUT to `writePath`, over UDP `5683`; the
  acquisition interval defaults to 30 seconds in the base config, overridden to 5 seconds by the dev profile (active by
  default).
- **[LwM2M driver](../drivers/lwm2m)** (`dc3-driver-lwm2m`): embeds an Eclipse Leshan LwM2M server; devices register
  with their `endpoint` name, and resources are read/written by the point's three-part
  `objectId/objectInstanceId/resourceId` path.
- **[HTTP driver](../drivers/http)** (`dc3-driver-http`): type `DRIVER_CLIENT`, using `WebClient` to call a REST
  endpoint periodically and extract the value from the JSON response per `responsePath`.

::: warning The MQTT driver has "passive arrival via subscription" semantics
`dc3-driver-mqtt`'s `read()` does not actively return an acquired value—scheduled reads are off by default (
`schedule.read.enable=false`), and a point value is **received passively after the device publishes** it. If values
never arrive, first confirm the device is actually publishing to the subscribed topic and that the topic strings match
exactly on both sides—rather than checking the "acquisition interval."
:::

::: warning The MQTT / LwM2M drivers are currently skeleton implementations
In the source, `dc3-driver-mqtt`'s `read()` is a reference stub and `health()` always reports online, and
`dc3-driver-lwm2m`'s class comment is marked "work-in-progress skeleton"; protocol-level I/O is not yet fully
implemented. Treat them as onboarding templates and configuration references, not production-ready drivers; defer to
each [driver page](../drivers/) and the source for specifics.
:::

As for the **wireless/access technologies** in the second half of this chapter (BLE, Zigbee, LoRa, NB-IoT, 5G), they
belong to the physical-link layer: DC3 does not speak the air interface directly—it sits on top of them. BLE and Zigbee
each have a corresponding driver (see the "IoT / Wireless" group in the [Drivers overview](../drivers/)); LoRa/NB-IoT/5G
endpoints are usually first aggregated into a broker or REST gateway, then onboarded uniformly through DC3's
MQTT/CoAP/HTTP drivers—the concrete product form of the "unified protocol-adaptation layer" described above.

## Further Reading

- [Fieldbuses & Industrial Protocols](./fieldbus) — the other half of the network layer: close-range, strongly
  deterministic
- [Edge & Cloud Architecture](./edge-cloud) — above protocol access, how data is split between edge and cloud
- [IoT Technology Overview](./) — the four-layer reference architecture and the DC3 panorama
- [MQTT Driver](../drivers/mqtt) — how pub/sub, QoS and brokers land in DC3
- [Connectivity & Drivers](../drivers/) — how 28 protocol drivers bring heterogeneous devices in
