---
title: CoAP Driver
---

<script setup>
import CoapDiagram from '../../.vitepress/theme/components/CoapDiagram.vue'
</script>


# CoAP Driver

`dc3-driver-coap` connects CoAP devices to IoT DC3. Built on Eclipse Californium, it can act as a **CoAP client** that
actively reaches devices (GET to read, PUT to write) and as a **CoAP server** that listens for telemetry the device
pushes via POST. By the end you will be able to set `deviceHost`/`devicePort` on
a [device](../introduction/concepts/device), set read/write resource paths on a [point](../introduction/concepts/point),
and diagnose the common "no value / UDP won't connect" problems.

> You are here: a concrete driver on the "light-protocol" side of the network layer. For CoAP's request/response model,
> UDP/DTLS ports, and the Observe concept at the protocol level,
> see [IoT Protocols & Wireless Networks](../foundations/iot-protocols).

## Protocol Background

CoAP (Constrained Application Protocol) is a lightweight protocol the IETF designed for low-power, low-bandwidth IoT
endpoints (RFC 7252). It keeps the familiar HTTP **request/response + methods (GET/PUT/POST/DELETE) + resource path**
model, but squeezes the message down to a few dozen bytes over **UDP** on default port `5683` (`5684` for CoAPS over
DTLS). Connectionless UDP avoids TCP handshakes and keep-alive overhead, which is extremely friendly to battery-powered
endpoints that wake up only occasionally to report; the cost is that reliability must be added back through CoAP's own
CON/NON confirmation mechanism. It is common on battery-powered sensors, embedded gateways, and NB-IoT/6LoWPAN
endpoints—anywhere power and bandwidth must be conserved.

In the [four-layer IoT architecture](../foundations/iot-protocols), CoAP is an application-layer messaging protocol of
the **network layer**: it defines "what a message looks like, how it is delivered, and how reliable it is," independent
of which wireless carries it underneath—the same CoAP message can run over Wi-Fi or over an NB-IoT cellular link. CoAP's
communication model supports both a client **actively requesting** a resource and a server **passively receiving** a
client's POST. This driver implements both sides:

<CoapDiagram lang="en" />

Client mode is the default shape: IoT DC3's [collection schedule](../introduction/concepts/driver) sends a GET on each
point's `readPath` per cron cycle, and sends a PUT on `writePath` when a write command is issued. Server mode is the
reverse: the driver listens on a CoAP port, devices POST telemetry to the `/data` resource, and the driver parses and
forwards it. The two modes are selected by `dc3.driver.coap.mode` (see the configuration below).

## Attribute Configuration

Onboarding a CoAP device mainly involves two layers of [attributes](../introduction/concepts/attribute-config):
device-level connection parameters (`driver-attribute`) and per-point resource paths (`point-attribute`). In addition,
the driver exposes a set of process-level `dc3.driver.coap.*` Spring settings (controlling client/server mode, timeouts,
DTLS) that are not part of the device config but are tuned by operations via environment/config file. All attributes,
types, and defaults below come from the driver's `application.yml` and `CoapProperties` (the `dc3-driver-coap` module).

### Driver Attributes (device-level `driver-attribute`)

Driver attributes answer "which device to connect to." Fill in one set per CoAP device on
the [device](../introduction/concepts/device):

| Attribute   | code         | Type   | Default     | Remark                                    |
|-------------|--------------|--------|-------------|-------------------------------------------|
| Device Host | `deviceHost` | STRING | `localhost` | CoAP device host address (IP or hostname) |
| Device Port | `devicePort` | INT    | `5683`      | CoAP device port (standard `5683`)        |

The driver combines these two attributes into the device root address `coap://<deviceHost>:<devicePort>`, then appends
the point's resource path to reach a specific resource. The driver caches one `CoapClient` per device root address (URI)
—one URI, one client—and releases the client when the device is deleted or updated. Config validation (`validate`)
requires both `deviceHost` and `devicePort` to be non-empty; missing either is an error and the device cannot start.

### Point Attributes (`point-attribute`)

Point attributes answer "which resource path on this device to read/write." Fill in one set
per [point](../introduction/concepts/point):

| Attribute      | code            | Type   | Default      | Remark                                                                |
|----------------|-----------------|--------|--------------|-----------------------------------------------------------------------|
| Read Path      | `readPath`      | STRING | `/sensors`   | CoAP resource path to GET when collecting                             |
| Write Path     | `writePath`     | STRING | `/actuators` | CoAP resource path to PUT when writing                                |
| Content Format | `contentFormat` | STRING | `json`       | Content format declaration: `json` / `text` / `cbor` / `octet-stream` |

::: tip Reads and writes go to separate resource paths
When collecting, the driver sends GET to `coap://<host>:<port><readPath>`; the response payload is
the [point value](../introduction/concepts/point-value) for that [point](../introduction/concepts/point). When issuing a
write command, it sends PUT to `<writePath>` with the value as the request body. `readPath` and `writePath` are
independent—a read-only point only needs `readPath`, and leaving `writePath` empty is fine since it is never used. Point
validation (`validatePoint`) only requires `readPath` to be non-empty.
:::

CoAP has no separate `command-attribute` table—the write target of a writable point is determined by the point's own
`writePath`. When a write command is issued, the driver sends PUT directly to that path, with no extra command
attributes needed.

### Process-level Configuration (`dc3.driver.coap.*`)

This group controls the driver process as a whole (client timeouts, whether to start a server, DTLS), set via config
file or environment variables and applied to all devices. It is not listed explicitly in `application.yml`, so it
defaults entirely to the built-in defaults of `CoapProperties`:

| Setting               | Default   | Remark                                                                                                                                     |
|-----------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `mode`                | `CLIENT`  | Working mode: `CLIENT` (client only, active read/write) / `SERVER` (server only, listen for reports) / `BOTH`                              |
| `serverHost`          | `0.0.0.0` | Server bind address (effective in `SERVER`/`BOTH` mode)                                                                                    |
| `serverPort`          | `5683`    | Server listen port (effective in `SERVER`/`BOTH` mode)                                                                                     |
| `secureEnabled`       | `false`   | Whether DTLS encryption is enabled                                                                                                         |
| `clientTimeout`       | `5000`    | Client GET exchange lifetime (ms, min 100)                                                                                                 |
| `clientAckTimeout`    | `2000`    | Client CON acknowledgement timeout (ms, min 100)                                                                                           |
| `clientMaxRetransmit` | `4`       | Client max retransmissions (min 1)                                                                                                         |
| `dtls.*`              | empty     | DTLS credentials: `pskIdentity` / `pskSecret` or certificate paths `trustStorePath` / `identityCertificatePath` / `identityPrivateKeyPath` |

::: info Client mode by default; server mode must be turned on explicitly
The default `mode=CLIENT` makes the driver a client that actively GET/PUTs on a cron cycle and listens on no port. To
let devices POST telemetry, set `mode` to `SERVER` or `BOTH`—the driver then starts a CoAP server and registers a
`/data` resource on `serverPort` to receive reports. `secureEnabled`/`dtls.*` are reserved settings for DTLS; the
current `CoapClientManager` and `CoapServerManager` do not yet assemble DTLS endpoints when connecting (plaintext UDP).
Treat public-internet encryption as code-driven—verify against the source.
:::

## Troubleshooting

CoAP onboarding failures cluster around three areas: the UDP link, the resource path, and the report format. Work
through them in order:

1. **UDP won't connect (no value / `statusCode=timeout`)**. CoAP defaults to **UDP 5683** (not TCP). When the device
   does not respond, the client GET times out and returns `null`; `read` treats it as a failure, skips the round, and
   logs `CoAP read failed ... statusCode=timeout`. First verify the link with a CoAP client (e.g.
   `coap-client -m get coap://<host>:5683<readPath>`): confirm the device is online, the firewall allows **UDP** 5683,
   and `deviceHost`/`devicePort` are correct, rather than suspecting a wrong path first.

2. **Connects but path not found (4.04 Not Found)**. A wrong `readPath` makes the device return `4.04`; then
   `response.isSuccess()` is false and `read` likewise returns `null`. Check the resource path's case and leading `/`,
   and if needed GET the device's `/.well-known/core` to see which resources it actually exposes.

3. **Responses slower than the timeout get misjudged**. The client defaults are `clientTimeout=5000ms`,
   `clientAckTimeout=2000ms`, and max retransmit `4`. Devices on high-RTT links (e.g. cellular/NB-IoT) may not answer
   within the default window and be judged as timeouts—raise `dc3.driver.coap.clientTimeout`/`clientAckTimeout` rather
   than shortening the collection cycle.

4. **Write command returns failure**. A write is a PUT to `writePath` with the command's value as the body. On failure (
   PUT timeout or a non-2.xx response) `write` returns `false` and the write is not echoed back. Confirm `writePath` is
   a writable resource on the device and that the device accepts the PUT method. Note: the driver always PUTs with the
   `application/json` media type, regardless of the point's `contentFormat` declaration (see the pitfall below).

5. **Server mode receives no reports**. `mode` must be `SERVER` or `BOTH`, and devices must POST to
   `coap://<driver-host>:<serverPort>/data`. The report body must be JSON that deserializes into a `PointValue` (at
   least `deviceId` and `pointId`); otherwise the driver logs `missingIdentity` / `parse failed` and drops it. An empty
   body is answered with `4.00 Bad Request`.

6. **Online status flapping**. The health check defaults to every 15 seconds with a 45-second lease timeout. Frequent
   online/offline flapping usually means UDP packet loss or device responses slower than the
   timeout—see [device](../introduction/concepts/device) for the liveness mechanism.

::: warning contentFormat is only a declaration—currently not used for parsing
`contentFormat` declares the resource's content format (`json` / `text` / `cbor` / `octet-stream`), but the driver
currently returns the [point value](../introduction/concepts/point-value) as the raw payload (
`response.getResponseText()`) and **does not parse by this format**; the write direction also always PUTs with the
`application/json` media type and does not read this attribute. When unsure what format the device actually returns, GET
the resource once manually with a CoAP client to check the content before filling this in.
:::

## How It Works in IoT DC3

- **`dc3.driver.code`**: `CoapDriver` (type `DRIVER_CLIENT`). This is a stable routing identifier—do not change it
  casually.
- **Read**: ✓ Implemented. In client mode it sends a GET on each point's `readPath` per cron cycle and reports the
  response body as the [point value](../introduction/concepts/point-value).
- **Write**: ✓ Implemented. It sends a PUT (`application/json`) to the point's `writePath`, triggered by a write
  command.
- **Subscribe**: — Not counted in the "subscribe" column of the [driver capability matrix](./matrix). CoAP is shown as "
  ✓ / ✓ / —," meaning **active read / active write / no subscribe** under the SDK point model. CoAP's **Observe** (RFC
  7641, push on resource change) is **not wired up** in this driver: only the `CoapObserveHandler` interface is defined,
  with no implementation or caller.
- **Server-push reporting (extra capability)**: in `SERVER`/`BOTH` mode the driver starts a CoAP server that receives
  `PointValue` JSON the device POSTs to `/data` and forwards it. This is a separate path outside the matrix and requires
  explicitly setting `mode`.
- **Collection cycle**: default cron `0/30 * * * * ?` (collects every 30 seconds), configured in the driver's
  `application.yml` under `schedule.read`. `schedule.custom` (default cron `0/5 * * * * ?`) is enabled, but `schedule()`
  is a no-op—it runs no periodic logic beyond point collection.
- **Health / liveness**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`.

::: info Implementation status: available (client read/write + server reporting); Observe not implemented
The driver's **client read/write** and **server-push reception** are both complete implementations (not skeletons),
built on Eclipse Californium. The only piece not wired up is CoAP **Observe** subscription push: `CoapObserveHandler` is
an interface only, with no implementation, so the driver cannot currently "subscribe" to a resource and have the device
push on change—for event-driven reporting, use the server POST mode instead. DTLS encryption (`secureEnabled`/`dtls.*`)
is reserved in config but not yet assembled at connection time; verify against the source.
:::

### Minimal Onboarding Example

Onboard a CoAP sensor at `192.168.1.20:5683` whose temperature resource is at `/temp`:

1. Create a [device](../introduction/concepts/device) with `CoAP Driver`, and set the driver attributes
   `deviceHost=192.168.1.20`, `devicePort=5683`.
2. Add a temperature [point](../introduction/concepts/point) (`READ_ONLY`) to
   the [profile](../introduction/concepts/profile) bound to the device, with point attributes `readPath=/temp`,
   `contentFormat=json` (leave `writePath` empty).
3. Start the driver. Within 30 seconds you will see the value fetched by GET on `coap://192.168.1.20:5683/temp` in
   the [point value](../introduction/concepts/point-value).
4. If the point needs to be writable, configure a write [command](../introduction/concepts/command) and set `writePath`;
   on issue the driver PUTs to that path.

::: tip Choose server mode for device-initiated reporting
If a device only wakes up occasionally to report and does not accept being polled, set `dc3.driver.coap.mode` to
`SERVER` and have the device POST to the driver's `/data` resource (report body is `PointValue` JSON with `deviceId`/
`pointId`). This avoids pointless periodic GETs against a sleeping device.
:::

## Further Reading

- [Drivers Overview](./index) — entry point and classification of all drivers
- [Driver Capability Matrix](./matrix) — read/write/subscribe at a glance, including the CoAP row
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [IoT Protocols & Wireless Networks](../foundations/iot-protocols) — request/response model, UDP/DTLS, and Observe for
  light protocols like CoAP/LwM2M
- [LwM2M Driver](./lwm2m) — a driver built on top of CoAP with a device-management object model
