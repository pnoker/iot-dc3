---
title: LwM2M Driver
---

# LwM2M Driver

> **`dc3-driver-lwm2m` connects LwM2M devices to IoT DC3**——it embeds a LwM2M server, accepts device registrations, and reads/writes resources on a device by the three-part Object / Object Instance / Resource path.

LwM2M (Lightweight M2M) is an OMA-defined protocol for IoT device management and data collection. It runs over CoAP (default UDP `5683`, DTLS/CoAPS `5684` for encryption). It abstracts a device's capabilities into an "object tree": each **Object** (e.g. `3303`=Temperature) contains one or more **Object Instances** (multiple instances of the same resource type), and each instance contains one or more **Resources** (e.g. `5700`=Sensor Value). Accessing a specific value means giving the path `/<objectId>/<objectInstanceId>/<resourceId>`. It is common on battery-powered, remotely deployed endpoints (NB-IoT modules, smart meters, environmental sensors—anywhere remote management and low power consumption are both needed).

Unlike drivers such as Modbus or CoAP that actively connect to devices, this driver embeds a **LwM2M server** based on Eclipse Leshan: the device acts as a LwM2M client and registers to this server using its own **endpoint name**; once registered, the driver issues reads/writes to that endpoint by the path configured on each point.

- **Driver name / code**: `LwM2M Driver` / `Lwm2mDriver`
- **Type**: `DRIVER_CLIENT`

::: warning Currently a skeleton implementation (WIP)
This driver is currently a skeleton; protocol-level I/O is not yet fully implemented (the class doc of `Lwm2mDriverCustomServiceImpl` explicitly marks it a "work-in-progress skeleton"). Treat it as an onboarding template / starting point, not a production-ready driver.
:::

## Driver Configuration (device-level `driver-attribute`)

When onboarding a LwM2M device, fill in these [attributes](../introduction/concepts/attribute-config) on the [device](../introduction/concepts/device):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Endpoint | `endpoint` | STRING | (empty) | LwM2M device endpoint name |
| Server Host | `serverHost` | STRING | `0.0.0.0` | LwM2M server bind address |
| Server Port | `serverPort` | INT | `5683` | CoAP port |
| Secure Port | `securePort` | INT | `5684` | CoAPS/DTLS port |
| Security Mode | `securityMode` | STRING | `NOSEC` | Security mode: NOSEC, PSK |
| PSK Identity | `pskIdentity` | STRING | (empty) | PSK identity (when securityMode=PSK) |
| PSK Key | `pskKey` | STRING | (empty) | PSK key in HEX (when securityMode=PSK) |

Here `endpoint` is the key that maps this device to a registered LwM2M client—it must match the endpoint name the device reports at registration exactly. `serverHost` / `serverPort` / `securePort` determine which address and ports the embedded server listens on.

## Point Configuration (`point-attribute`)

Fill in one LwM2M resource path on each [point](../introduction/concepts/point):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Object ID | `objectId` | INT | `0` | LwM2M Object ID (e.g. 3303=Temperature) |
| Object Instance ID | `objectInstanceId` | INT | `0` | LwM2M Object Instance ID |
| Resource ID | `resourceId` | INT | `0` | LwM2M Resource ID (e.g. 5700=Sensor Value) |
| Observe | `observe` | STRING | `false` | Enable LwM2M Observe: true, false |

::: tip The three-part path decides which resource to read
The driver assembles `objectId` / `objectInstanceId` / `resourceId` into the path `/<objectId>/<objectInstanceId>/<resourceId>`, issues a read to the device endpoint, and the returned value is the [point value](../introduction/concepts/point-value) for that [point](../introduction/concepts/point). The point's data type ([Point](../introduction/concepts/point)'s `pointTypeFlag`) must match the actual data type of that Resource. `observe=true` enables LwM2M Observe (subscription-style reporting) on the resource, so the device pushes proactively on value change rather than being polled each round.
:::

LwM2M has no separate `command-attribute` table (`command-attribute: [ ]` is empty in the yml)—the write target of a writable point is simply the point's own `/<objectId>/<objectInstanceId>/<resourceId>` path. When a write command is issued, the driver writes directly to that path, with no extra command attributes needed.

## Collection and Health

- **Collection cycle**: default cron `0/30 * * * * ?` (reads every 30 seconds, issuing one read to each point's resource path).
- **Custom job**: the driver ships a built-in custom schedule, default cron `0/5 * * * * ?` (every 5 seconds), for the driver's own periodic logic, independent of point collection.
- **Health / liveness**: device health check default cron `0/15 * * * * ?`, lease timeout `45 seconds`. Whether a device is online depends on whether its endpoint is still registered on the embedded server—see [device](../introduction/concepts/device) for the liveness mechanism.

## Minimal Onboarding Example

Onboard a LwM2M sensor whose endpoint name is `urn:imei:860000000000001` and whose temperature resource is at `/3303/0/5700`:

1. Create a [device](../introduction/concepts/device) with `LwM2M Driver`, and set the driver attributes `endpoint=urn:imei:860000000000001`, `serverHost=0.0.0.0`, `serverPort=5683`, `securityMode=NOSEC` (plaintext mode needs no PSK).
2. Have the LwM2M client register to this service's port `5683` using the same endpoint name.
3. Add a temperature [point](../introduction/concepts/point) (`READ_ONLY`) to the [profile](../introduction/concepts/profile) bound to the device, with point attributes `objectId=3303`, `objectInstanceId=0`, `resourceId=5700`.
4. Start the driver. Once the device registers successfully, within 30 seconds you will see the temperature value read back in the [point value](../introduction/concepts/point-value).

## Common Pitfalls

::: warning The endpoint name must match the device's registration exactly
Device liveness is judged by matching the `endpoint` name on the embedded server. If the endpoint the device actually registers with (commonly `urn:imei:<IMEI>` or a vendor-defined string) and the `endpoint` filled on the [device](../introduction/concepts/device) differ by even one character, they will not match: the device stays offline and reads return nothing. Before onboarding, confirm exactly what endpoint name the device firmware uses to register.
:::

::: tip With PSK enabled, configure Identity and Key as a pair, and use port 5684
When `securityMode=PSK`, the encrypted handshake (DTLS) goes over `securePort` (default `5684`), and `pskIdentity` and `pskKey` must match what is provisioned on the device side, with `pskKey` being a **HEX string**. If any of the three does not match, the DTLS handshake fails and the device cannot register. To just get the link working first, `securityMode=NOSEC` over the plaintext `5683` port is the simplest.
:::

## Further Reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes and Config](../introduction/concepts/attribute-config) — the three layers behind attributes like `endpoint` / `objectId`
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [CoAP Driver](./coap) — the CoAP protocol driver that LwM2M builds upon
