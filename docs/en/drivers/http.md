---
title: HTTP Driver
---

# HTTP Driver

`dc3-driver-http` onboards any HTTP/REST endpoint into IoT DC3 as a data source—it periodically calls REST endpoints, extracts one field from the JSON response as the [PointValue](../introduction/concepts/point-value), and supports write commands that push values via a request-body template. After reading this you can decide which devices/platforms fit it, what each attribute should hold, and where to look when a connection fails.

## Protocol background

HTTP (HyperText Transfer Protocol) and the REST-style endpoints built on it are the internet's most universal request/response protocol: a client issues a **method** (`GET`/`POST`/`PUT`/`DELETE`) against a **resource path**, and the server returns a status code and a payload (usually JSON in IoT scenarios). It has simple connectionless semantics, native support in virtually every language and tool, and easy debugging—which makes it the "greatest common divisor" of system integration.

In the four-layer IoT reference architecture, HTTP belongs to the **network layer**, in the **application-layer messaging protocol** family (alongside MQTT, CoAP, LwM2M)—it defines "what a message looks like and how it is delivered," not which radio the bytes travel over. But to be honest: HTTP has bulky headers, costly keep-alives, and was not designed for constrained devices, so it is **not** a good fit for high-frequency reporting from battery-powered endpoints. Its real place in IoT is **integrating with existing endpoints**—open REST APIs from third-party platforms, RESTful interfaces built into devices, and data gateways that aggregate field data into an HTTP endpoint. These "the upstream already speaks REST, I just need to poll it" cases are exactly what this driver is for. For HTTP versus MQTT/CoAP/LwM2M trade-offs, see the [IoT network layer chapter](../foundations/iot-protocols).

This driver acts as an HTTP client ([Driver](../introduction/concepts/driver) type `DRIVER_CLIENT`), using Spring WebFlux `WebClient` to call endpoints by the path and method configured on each [Point](../introduction/concepts/point), then extracting one value from the JSON response. Two driver-specific concepts recur below:

- **Response Path**: a simple dot-notation path locating a field in the response JSON, e.g. `$.data.temperature` picks `temperature` under the `data` object. Leave it empty to use the whole raw response as the value.
- **Body Template**: the request body template used when writing; the `${value}` placeholder is replaced with the actual value from the command parameter.

## Attribute configuration

Attributes are filled in two layers: **driver attributes** (`driver-attribute`, device-level, deciding which service to connect to) and **point attributes** (`point-attribute`, deciding which path each point calls, which method to use, and which field to extract—used by both read and write). `application.yml` also declares **command attributes** (`command-attribute`), but the current `write()` does not consume them (see the Command attributes section below). For where these layers come from and how they override, see [Attributes & Config](../introduction/concepts/attribute-config). All defaults are taken from the driver's `application.yml`.

### Driver attributes (device-level `driver-attribute`)

When onboarding an HTTP data source, fill in these attributes on the [Device](../introduction/concepts/device). They decide which service to connect to, the headers, and the timeout—every point under the same device shares this connection:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Base URL | `baseUrl` | STRING | (empty) | Base URL for API requests (e.g. `https://api.example.com`) |
| Method | `method` | STRING | `GET` | Declared default HTTP method, but the current implementation does not read this attribute (see the warning below); the actual method is decided by the point attribute only |
| Headers | `headers` | STRING | (empty) | Custom headers as JSON (e.g. `{"Authorization":"Bearer xxx"}`) |
| Timeout | `timeout` | INT | `5000` | Request timeout in milliseconds, applied as `responseTimeout` |

`baseUrl` is required—the driver uses it as the `WebClient` base URL and prefixes every point path with it; without it the driver's `validate()` fails. `timeout` defaults to `5000` ms and lands on the Reactor Netty `HttpClient` `responseTimeout`.

::: warning The Headers attribute currently has no effect
`headers` is declared in `application.yml`, but the current `getConnector()` only sets a fixed `Content-Type: application/json` on the `WebClient`—it does **not** read or apply the `headers` attribute. Endpoints that need `Authorization` or other custom headers cannot be onboarded by this attribute alone for now.
:::

::: warning The driver-level Method attribute currently has no effect
`method` is declared as a driver-level attribute in `application.yml`, but `getConnector()` reads only `baseUrl` and `timeout`—it never reads the driver-level `method`. The method in `read()`/`write()` comes solely from the point attribute `method`, falling back to the hard-coded `GET`. So setting `method=POST` on the device has no effect—like `headers`, it is a "declared but not applied" attribute. The HTTP method is decided only by the point attribute `method`.
:::

### Point attributes (`point-attribute`)

On each polled [Point](../introduction/concepts/point), fill in: which path to call, which method to use, (for writes) what body to send, and which field to extract. The HTTP method actually used by read/write comes solely from the point's `method`, falling back to the hard-coded `GET` when absent; it is independent of the driver-level `method` attribute (which the implementation does not read, see above):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Path | `path` | STRING | (empty) | API path (e.g. `/api/v1/sensor/{id}`) |
| Method | `method` | STRING | `GET` | HTTP method for this point, falling back to the hard-coded `GET` |
| Body Template | `bodyTemplate` | STRING | (empty) | Request body template with `${value}` placeholder |
| Response Path | `responsePath` | STRING | (empty) | Path to extract a value from the JSON response (e.g. `$.data.temperature`) |

::: tip path is appended after baseUrl
The actual request URL is `baseUrl + path`. For example, with `baseUrl=https://api.example.com` and `path=/api/v1/sensor/1`, the driver requests `https://api.example.com/api/v1/sensor/1`.
:::

::: warning Response Path is simple dot-notation, not full JSONPath
The driver strips the `$.` prefix and drills down field-by-field on `.` from the root object; it only supports paths like `$.a.b.c`. It does **not** support array indices (`[0]`), filters, wildcards, or other full JSONPath syntax. Reaching an array element, or a path that doesn't resolve to a field, makes the driver fall back to returning the whole raw response—usually the root cause of a PointValue that "looks wrong". When the response is itself a bare value (a plain number or string), leave `responsePath` empty and the driver uses the entire response as the value.
:::

::: warning Writing relies on the Body Template
A write command does not automatically place the value into the request body. You must author a template with `${value}` in the point's `bodyTemplate` (e.g. `{"value":${value}}`) for the driver to substitute the command parameter and send it; an empty template sends an empty request body.
:::

### Command attributes (`command-attribute`)

::: warning Command attributes are not consumed by the write path
`application.yml` declares `path` and `method` (`default-value: POST`) under `command-attribute`, but the SPI `write()` signature only receives `driverConfig` and `pointConfig`—it is never passed a command-attribute map. `write()` actually reads `path`/`method`/`bodyTemplate` from the point attribute (`pointConfig`), and `method` falls back to the hard-coded `GET` (not `POST`). So the `command-attribute` entries below are declared-but-inert dead config today; put the write path and method on the point attribute instead.
:::

The table below shows the values declared in `application.yml` (not read by the current `write()`):

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Path | `path` | STRING | (empty) | API path for the command (not read by `write()` today) |
| Method | `method` | STRING | `POST` | HTTP method for the command (not read by `write()` today) |

### Polling & health

These are defined under `dc3.driver.schedule` and `health` in `application.yml`; you don't fill them on the device:

- **Polling interval**: the `application.yml` baseline read cron is `0/30 * * * * ?` (read once every 30 seconds); the default-active `dev` profile overrides it in `application-dev.yml` to `0/5 * * * * ?` (every 5 seconds), so the out-of-the-box polling interval is 5 seconds.
- **Health check**: device health-check cron `0/15 * * * * ?` with a lease timeout of `45 seconds`—see [Device](../introduction/concepts/device) for the online-state mechanism.
- **Online decision**: `health()` decides online by "whether a `WebClient` exists for the device in `clientMap`"—a connection is built on the first successful read/write, after which the device is online; on a read/write exception the driver does `clientMap.remove(deviceId)`, dropping the connection so it is rebuilt next round.

## Troubleshooting

::: warning Cannot connect / always offline
First confirm `baseUrl` is reachable: run `curl <baseUrl><path>` on the driver host to verify connectivity and the port. The driver decides online by "whether a `WebClient` was built"; a first failed read/write immediately drops the connection, so a wrong `baseUrl`, an unresolvable DNS, or a firewalled target port all show up as the device staying offline.
:::

::: warning Request timeouts
The default `timeout=5000` ms applies to the response timeout. A slow endpoint or network jitter triggers the timeout, fails this read/write round, and drops the connection. Measure the real round-trip with `curl -w '%{time_total}'` to tell whether the endpoint or the link is slow, then raise `timeout` accordingly.
:::

::: warning PointValue looks wrong / always the whole JSON
Most likely `responsePath` didn't match. The driver only understands `$.a.b.c` dot paths; a wrong path, mismatched field-name case, or trying to reach an array element (`$.list[0].v`) all fail to resolve and make the driver **fall back to the whole raw response**. Check field names level by level against the real response; reaching an element inside an array is not possible in the current implementation.
:::

::: warning 401/403 from authenticated endpoints
The current implementation does not apply the `headers` attribute (see the warning in the driver-attributes table). Endpoints requiring `Authorization`, `X-Api-Key`, etc. cannot be onboarded by device attributes alone and will be rejected with 401/403.
:::

::: warning Write command errors or has no effect
Writes are rendered through the point's `bodyTemplate`: an empty template sends an **empty body**, which an endpoint expecting a JSON body will reject. Make sure `bodyTemplate` contains the `${value}` placeholder and renders to valid JSON the endpoint accepts; on failure the driver throws `WritePointException` and drops the connection.
:::

## Landing in IoT DC3

- **dc3.driver.code**: `HttpDriver` (driver name `HTTP REST Client Driver`, type `DRIVER_CLIENT`). This code is a stable routing identifier and must not be changed casually.
- **Read**: ✓ implemented. Periodically calls the endpoint by the point's `path`/`method`, extracting a value via `responsePath`.
- **Write**: ✓ implemented. Substitutes `${value}` in the point's `bodyTemplate` with the command parameter, then sends the request.
- **Subscribe/report**: — not supported. HTTP is request/response, the driver always initiates, there is no passive push channel.

These read/write capabilities match the `HTTP (HttpDriver)` row in the [driver capability matrix](./matrix).

::: info Implementation status: available
`HttpDriverCustomServiceImpl` fully implements `initial()`/`read()`/`write()`/`health()`/`validate()`; it is a mature driver ready for collection. Two implementation boundaries to know: (1) `responsePath` supports only simple dot paths, not arrays/filters (above); (2) the `headers` attribute is declared but not yet applied to the connection, so custom request headers don't currently take effect.
:::

### Minimal onboarding example

Onboard an endpoint that returns `{"data":{"temperature":25.6}}`:

1. Create a [Device](../introduction/concepts/device) with `HTTP REST Client Driver`, and set the driver attributes `baseUrl=https://api.example.com`, `method=GET`, `timeout=5000`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `path=/api/v1/sensor/1`, `method=GET`, `responsePath=$.data.temperature`.
3. Start the driver, and within a few seconds the extracted `25.6` shows up in the [PointValue](../introduction/concepts/point-value) (the default `dev` profile polls every 5 seconds).

## Further reading

- [Driver overview](./index) — all driver groups and the selection entry point
- [Driver capability matrix](./matrix) — read/write/subscribe capabilities at a glance
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [IoT network layer chapter](../foundations/iot-protocols) — where HTTP sits among MQTT/CoAP/LwM2M and the trade-offs
