---
title: LwM2M Driver
---

<script setup>
import Lwm2mDiagram from '../../.vitepress/theme/components/Lwm2mDiagram.vue'
</script>


# LwM2M Driver

`dc3-driver-lwm2m` embeds an Eclipse Leshan LwM2M server: devices act as clients and register with their own `endpoint`
name, and the driver then reads/writes resources on that endpoint by the three-part
`Object / Object Instance / Resource` path configured on each point. This page explains which protocol it speaks, which
attributes to fill in, how to troubleshoot a failed connection, and its real implementation status in IoT DC3.

## Protocol Background

LwM2M (Lightweight M2M) is an OMA-defined protocol for IoT **device management plus data collection**. It does not start
from scratch—it is **built on top of CoAP**, running over UDP (default `5683`, `5684` for DTLS/CoAPS encryption) and
supplying the "device management" layer that CoAP lacks. In
the [four-layer IoT reference architecture](../foundations/iot-protocols), it sits—like CoAP and MQTT—among the *
*network layer's application messaging protocols**: defining "what a message looks like, how it is delivered, and how
reliable it is," independent of whether Wi-Fi or NB-IoT carries it underneath.

The core of LwM2M is abstracting device capabilities into an **object tree**:

- **Object** (e.g. `3303` = Temperature)—a class of capability;
- **Object Instance**—multiple instances of the same capability (e.g. several temperature sensors on one device);
- **Resource** (e.g. `5700` = Sensor Value)—a specific readable/writable item within an instance.

Accessing a specific value means giving the path `/<objectId>/<objectInstanceId>/<resourceId>`. Firmware upgrade, remote
configuration, and subscription reporting are all standardized into this object model, which is why LwM2M is common on *
*carrier-grade endpoints that need remote operation**: NB-IoT modules, smart meters, remote environmental
sensors—anywhere both remote management and low power consumption are needed.

Unlike drivers such as Modbus or CoAP that **actively connect to devices**, this driver works the other way around—it
embeds a **LwM2M server**:

<Lwm2mDiagram lang="en" />

The device first registers to this server with its own endpoint name; only after a successful registration can the
driver issue reads/writes to it by the point's path. Whether a device is online depends on whether its endpoint is still
in the registry.

## Attribute Configuration

LwM2M attributes come in two layers: **driver attributes** are set on the [device](../introduction/concepts/device) and
describe "where the server listens, which endpoint to match, whether to encrypt"; **point attributes** are set on
each [point](../introduction/concepts/point) and describe "which resource path in the object tree this point maps to."
Both originate from the driver's `application.yml` `driver-attribute` / `point-attribute` declarations; at onboarding
you [fill a concrete value](../introduction/concepts/attribute-config) for each attribute on the device instance.

### Driver attributes (device-level `driver-attribute`)

`endpoint` is the key that maps this DC3 device to a registered LwM2M client—it must match the endpoint name the device
reports at registration **exactly**, otherwise they will not match and the device stays offline. `serverHost` /
`serverPort` / `securePort` declare the server's bind address and ports; `securityMode` decides plaintext versus PSK
encryption, with `pskIdentity` and `pskKey` added when PSK is enabled.

| Attribute     | code           | Type   | Default   | Remark                                        |
|---------------|----------------|--------|-----------|-----------------------------------------------|
| Endpoint      | `endpoint`     | STRING | (empty)   | LwM2M device endpoint name                    |
| Server Host   | `serverHost`   | STRING | `0.0.0.0` | Server bind address                           |
| Server Port   | `serverPort`   | INT    | `5683`    | CoAP port                                     |
| Secure Port   | `securePort`   | INT    | `5684`    | CoAPS/DTLS port                               |
| Security Mode | `securityMode` | STRING | `NOSEC`   | Security mode: NOSEC, PSK                     |
| PSK Identity  | `pskIdentity`  | STRING | (empty)   | PSK identity (when `securityMode=PSK`)        |
| PSK Key       | `pskKey`       | STRING | (empty)   | HEX-encoded PSK key (when `securityMode=PSK`) |

::: warning serverHost / serverPort / securePort are not actually applied yet
When the driver starts the embedded server it uses the **default binding** of `new LeshanServerBuilder().build()` (which
happens to be `5683`/`5684`); it does not feed the `serverHost`/`serverPort`/`securePort` or PSK values above into
Leshan. In other words these items are **declared but not wired**: filling them only lands on the default ports and a
plaintext link. To bring the link up, use the default plaintext `5683` port; changing the port or enabling PSK still
needs to be wired into the driver (see Implementation status below).
:::

### Point attributes (`point-attribute`)

Fill in one LwM2M resource path on each point. The driver assembles `objectId` / `objectInstanceId` / `resourceId` into
`/<objectId>/<objectInstanceId>/<resourceId>`, issues a read to the device endpoint, and the returned value is
the [point value](../introduction/concepts/point-value) for that point.

| Attribute          | code               | Type   | Default | Remark                                       |
|--------------------|--------------------|--------|---------|----------------------------------------------|
| Object ID          | `objectId`         | INT    | `0`     | LwM2M Object ID (e.g. `3303`=Temperature)    |
| Object Instance ID | `objectInstanceId` | INT    | `0`     | LwM2M Object Instance ID                     |
| Resource ID        | `resourceId`       | INT    | `0`     | LwM2M Resource ID (e.g. `5700`=Sensor Value) |
| Observe            | `observe`          | STRING | `false` | Enable LwM2M Observe: true, false            |

::: tip The three-part path decides which resource to read
The point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) must match the actual data type of
that Resource. LwM2M has no separate `command-attribute` table (`command-attribute: [ ]` is empty in the yml)—the write
target of a writable point is simply its own three-part path: when a write command is issued, the driver sends a
`WriteRequest` directly to `/<objectId>/<objectInstanceId>/<resourceId>`, with no extra command attributes needed.
:::

::: warning The observe attribute has no effect yet
At the protocol level `observe=true` means "enable LwM2M Observe (subscription-style reporting) on the resource, so the
device pushes proactively on value change." But this driver **does not register any Observe and does not consume the
attribute**—neither `read()` nor `write()` reads `observe`. Point values can currently only be obtained via the default
30-second active read; setting `observe=true` will not trigger subscription pushes (see Implementation status below).
:::

### Collection and health cadence

The following cycles come from `application.yml`'s `dc3.driver.schedule` / `health`:

- **Collection cycle**: default cron `0/30 * * * * ?`, issuing one read to each point's resource path every 30 seconds.
- **Custom job**: a built-in custom schedule, default cron `0/5 * * * * ?`, every 5 seconds, reserved for the driver's
  own periodic logic (the current `schedule()` is an empty implementation).
- **Health / liveness**: device health check default cron `0/15 * * * * ?`, lease timeout `45` seconds. Whether a device
  is online depends on whether its endpoint is still registered on the embedded server.

## Troubleshooting

| Symptom                                     | Likely cause                                                    | Where to look                                         |
|---------------------------------------------|-----------------------------------------------------------------|-------------------------------------------------------|
| Device stays offline, reads return nothing  | `endpoint` differs from what the device reports at registration | They must match exactly—see the first note below      |
| Device cannot reach the server              | UDP `5683` blocked by a firewall, or broken NAT mapping         | Verify the UDP link before the app config             |
| Changing `serverPort` still listens on 5683 | Port config is not fed to Leshan yet                            | Port override is not implemented—use the default port |
| Registration fails after enabling PSK       | PSK config not applied yet, or the device forces DTLS           | Bring the link up with `NOSEC` plaintext first        |
| `observe=true` receives no pushes           | Observe auto-forwarding is not implemented                      | Rely on the default 30-second active read             |

::: warning The endpoint name must match the device's registration exactly
Device liveness is judged by matching the `endpoint` name in the embedded server's registry (
`isDeviceRegistered(endpoint)`). If the endpoint the device actually registers with (commonly `urn:imei:<IMEI>` or a
vendor-defined string) and the `endpoint` filled on the device differ by even one character, they will not match: the
device stays offline and reads return nothing. Before onboarding, confirm exactly what endpoint name the device firmware
uses to register.
:::

::: tip For UDP protocols, check the link first
LwM2M runs over CoAP over UDP; a failure to connect is usually the UDP port (`5683`/`5684`) being blocked by a firewall
or a broken NAT mapping, rather than a wrong application config—verify the link first, then check the endpoint and point
paths. When the device is on the public internet/cellular, remember to allow the corresponding UDP ports.
:::

::: warning Changing ports and enabling PSK are not yet available
The driver currently does not consume the `serverPort`/`securePort`/`securityMode`/PSK config; the embedded server is
fixed to Leshan defaults (plaintext `5683` / DTLS `5684`). So changing the listening port or using a PSK handshake is
not possible today—use the default `NOSEC` plaintext `5683` port to get the link working first. To support encryption,
these settings must first be wired into `LeshanServerBuilder` inside `Lwm2mServerManager`.
:::

## How It Lands in IoT DC3

- **`dc3.driver.code`**: `Lwm2mDriver` (a stable routing identifier consistent with
  the [driver capability matrix](./matrix); do not change it casually).
- **Driver name / type**: `LwM2M Driver` / `DRIVER_CLIENT`.
- **Read (implemented)**: `read()` calls `Lwm2mServerManager.read()`, sending
  `ReadRequest(objectId, objectInstanceId, resourceId)` to the registered device and using the returned content as the
  point value—this is real protocol I/O, not a stub.
- **Write (implemented)**: `write()` calls `Lwm2mServerManager.write()`, sending a `WriteRequest` to the same three-part
  path; success returns `true`, failure/timeout returns `false`.
- **Subscribe (not implemented)**: the [driver capability matrix](./matrix) marks LwM2M as
  read/write/subscribe-complete, but **Observe-based reporting is not landed yet**—
  `Lwm2mObservationHandler.onObservation()` only logs and carries a `TODO`; the driver neither registers Observe nor
  maintains the endpoint→deviceId / resource-path→pointId mapping needed to forward observed values.

::: warning Implementation status: read/write usable, subscribe and server config incomplete
The class doc of `Lwm2mDriverCustomServiceImpl` still marks it a "work-in-progress skeleton," but **read and write are
wired to real Leshan I/O and work against a registered device**. Three things remain unfinished: ① auto-forwarding of
Observe-subscribed values; ② feeding `serverHost`/`serverPort`/`securePort` config into Leshan; ③ the PSK-encrypted
link. Treat it as a "read/write runnable, subscribe/encryption pending" onboarding starting point, and validate
read/write over a plaintext link with the example below first.
:::

::: details Minimal onboarding example: read back a temperature point
Onboard a LwM2M sensor whose endpoint name is `urn:imei:860000000000001` and whose temperature resource is at
`/3303/0/5700`:

1. Create a [device](../introduction/concepts/device) with `LwM2M Driver`, and set the driver attributes
   `endpoint=urn:imei:860000000000001`, `securityMode=NOSEC` (ports are currently fixed to the default `5683`, neither
   needed nor changeable).
2. Have the LwM2M client register to this service's port `5683` (plaintext) using the **same** endpoint name.
3. Add a temperature [point](../introduction/concepts/point) (`READ_ONLY`) to
   the [profile](../introduction/concepts/profile) bound to the device, with point attributes `objectId=3303`,
   `objectInstanceId=0`, `resourceId=5700`.
4. Start the driver. Once the device registers successfully, within 30 seconds you will see the temperature value read
   back in the [point value](../introduction/concepts/point-value).
   :::

## Further Reading

- [Drivers Overview](./index) — the full landscape and grouping of the 28 drivers
- [Driver Capability Matrix](./matrix) — a quick reference for each driver's read/write/subscribe support
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [IoT Protocols & Wireless Networks](../foundations/iot-protocols) — LwM2M's place in the network layer and its
  trade-offs vs CoAP/MQTT
