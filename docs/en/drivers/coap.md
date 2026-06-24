---
title: CoAP Driver
---

# CoAP Driver

> **`dc3-driver-coap` connects CoAP devices to IoT DC3**——it targets a device resource path, periodically sends GET to read values, and supports sending PUT to a resource to write values.

CoAP (Constrained Application Protocol) is a lightweight protocol designed for low-power, low-bandwidth IoT endpoints. It runs over UDP on default port `5683` and uses a request/response model like a stripped-down HTTP (accessing "resource paths" with methods such as GET / PUT / POST). It is common on battery-powered sensors, embedded gateways, and NB-IoT/6LoWPAN endpoints—anywhere power and bandwidth must be conserved. Built on the Eclipse Californium library, this driver acts as a CoAP client and actively connects to devices: the read path sends a CoAP GET to the resource, and the write path sends a CoAP PUT.

- **Driver name / code**: `CoAP Driver` / `CoapDriver`
- **Type**: `DRIVER_CLIENT` (actively connects to the device)

## Driver Configuration (device-level `driver-attribute`)

When onboarding a CoAP device, fill in these [attributes](../introduction/concepts/attribute-config) on the [device](../introduction/concepts/device):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Device Host | `deviceHost` | STRING | `localhost` | CoAP device host address |
| Device Port | `devicePort` | INT | `5683` | CoAP device port |

The driver combines these two attributes into the device root address `coap://<deviceHost>:<devicePort>`, then appends the point's resource path to reach a specific resource.

## Point Configuration (`point-attribute`)

Fill in on each [point](../introduction/concepts/point):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Read Path | `readPath` | STRING | `/sensors` | CoAP resource path for reading point data |
| Write Path | `writePath` | STRING | `/actuators` | CoAP resource path for writing point data |
| Content Format | `contentFormat` | STRING | `json` | Content format: json, text, cbor, octet-stream |

::: tip Reads and writes go to separate resource paths
When collecting, the driver sends GET to `coap://<host>:<port><readPath>`; the response payload is the [point value](../introduction/concepts/point-value) for that [point](../introduction/concepts/point). When issuing a write command, it sends PUT to `<writePath>` with the value as the request body. `readPath` and `writePath` are independent—a read-only point only needs `readPath`, and leaving `writePath` empty is fine since it is never used.
:::

CoAP has no separate `command-attribute` table—the write target of a writable point is determined by the point's own `writePath`. When a write command is issued, the driver sends PUT directly to that path, with no extra command attributes needed.

## Collection and Health

- **Collection cycle**: default cron `0/30 * * * * ?` (collects every 30 seconds, sending one GET to each point's `readPath`).
- **Custom job**: the driver ships a built-in custom schedule, default cron `0/5 * * * * ?` (every 5 seconds), for the driver's own periodic logic, independent of point collection.
- **Health / liveness**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`—see [device](../introduction/concepts/device) for the liveness mechanism.

## Minimal Onboarding Example

Onboard a CoAP sensor at `192.168.1.20:5683` whose temperature resource is at `/temp`:

1. Create a [device](../introduction/concepts/device) with `CoAP Driver`, and set the driver attributes `deviceHost=192.168.1.20`, `devicePort=5683`.
2. Add a temperature [point](../introduction/concepts/point) (`READ_ONLY`) to the [profile](../introduction/concepts/profile) bound to the device, with point attributes `readPath=/temp`, `contentFormat=json` (leave `writePath` empty).
3. Start the driver. Within 30 seconds you will see the value fetched by GET on `coap://192.168.1.20:5683/temp` in the [point value](../introduction/concepts/point-value).

## Common Pitfalls

::: warning CoAP runs over UDP—if it won't connect, check the firewall and port first
CoAP defaults to **UDP 5683** (not TCP). When the device does not respond, `read` treats it as a timeout and skips the round—seeing `statusCode=timeout` in the logs usually means the UDP packet did not get through. First confirm the device is online, the firewall allows UDP 5683, and `deviceHost`/`devicePort` are correct, rather than assuming a wrong path.
:::

::: tip contentFormat is a protocol declaration
`contentFormat` declares the resource's content format (`json` / `text` / `cbor` / `octet-stream`). The driver currently returns the [point value](../introduction/concepts/point-value) as the raw payload and does not parse it by this format. When unsure what format the device actually returns, GET the resource once manually with a CoAP client (such as `coap-client`) to check the format before filling this in.
:::

## Further Reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes and Config](../introduction/concepts/attribute-config) — the three layers behind attributes like `deviceHost` / `readPath`
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — the Modbus master driver for industrial field devices
