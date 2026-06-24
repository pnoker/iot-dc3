---
title: HTTP Driver
---

# HTTP Driver

> **`dc3-driver-http` onboards an HTTP/REST endpoint into IoT DC3 as a data source**—it periodically calls REST endpoints, extracts a value from the JSON response, and supports write commands that push values via a request-body template.

Many devices, gateways, or upstream systems don't speak an industrial protocol directly—they expose an HTTP/REST endpoint that returns some JSON. This driver acts as an HTTP client ([Driver](../introduction/concepts/driver) type `DRIVER_CLIENT`), using Spring WebFlux `WebClient` to call endpoints by the path and method configured on each [Point](../introduction/concepts/point), then extracting one value from the JSON response as the [PointValue](../introduction/concepts/point-value). It fits: third-party platform open APIs, RESTful endpoints built into devices, and HTTP-style data gateways.

Two driver-specific concepts that the configuration tables below rely on:

- **JSON path (Response Path)**: a simple dot-notation path to locate a field in the response JSON, e.g. `$.data.temperature` picks `temperature` under the `data` object. Leave it empty to use the whole raw response as the value.
- **Request body template (Body Template)**: the body template used when writing; the `${value}` placeholder is replaced with the actual value from the command parameter.

## Driver name / code / type

- **Driver name / code**: `HTTP REST Client Driver` / `HttpDriver`
- **Type**: `DRIVER_CLIENT` (the driver actively issues HTTP requests)

## Driver configuration (device-level `driver-attribute`)

When onboarding an HTTP data source, fill in these [attributes](../introduction/concepts/attribute-config) on the [Device](../introduction/concepts/device). They decide which service to connect to, the default method, the headers to send, and the timeout:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Base URL | `baseUrl` | STRING | (empty) | Base URL for API requests (e.g. https://api.example.com) |
| Method | `method` | STRING | `GET` | Default HTTP method (GET, POST, PUT, DELETE) |
| Headers | `headers` | STRING | (empty) | Custom headers as JSON (e.g. {"Authorization":"Bearer xxx"}) |
| Timeout | `timeout` | INT | `5000` | Request timeout in milliseconds |

`baseUrl` is required—it is the prefix for every point path, and without it the driver cannot establish a connection.

## Point configuration (`point-attribute`)

On each polled [Point](../introduction/concepts/point), fill in: which path to call, which method to use, (for writes) what request body to send, and which field to extract from the response:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Path | `path` | STRING | (empty) | API path (e.g. /api/v1/sensor/{id}) |
| Method | `method` | STRING | `GET` | HTTP method override for this point |
| Body Template | `bodyTemplate` | STRING | (empty) | Request body template with ${value} placeholder |
| Response Path | `responsePath` | STRING | (empty) | JSON path to extract value (e.g. $.data.temperature) |

::: tip path is appended after baseUrl
The actual request URL is `baseUrl + path`. For example, with `baseUrl=https://api.example.com` and `path=/api/v1/sensor/1`, the driver requests `https://api.example.com/api/v1/sensor/1`. The point's `method` overrides the driver-level default method.
:::

## Write command configuration (`command-attribute`)

A writable point also needs these on its write command:

| Attribute | code | Type | Default | Remark |
|---|---|---|---|---|
| Path | `path` | STRING | (empty) | API path for command |
| Method | `method` | STRING | `POST` | HTTP method for command |

When writing, the driver replaces `${value}` in the point's `bodyTemplate` with the command parameter, then sends the request to `path` using the method configured here.

## Polling & health

- **Polling interval**: default cron `0/30 * * * * ?` (read once every 30 seconds).
- **Health/online**: device health check defaults to cron `0/15 * * * * ?` with a lease timeout of `45 seconds`—see [Device](../introduction/concepts/device) for the online-state mechanism.
- This driver decides online status by "whether a `WebClient` connection has been established for the device": it is considered online after the first successful read/write.

## Minimal onboarding example

Onboard a weather API that returns `{"data":{"temperature":25.6}}`:

1. Create a [Device](../introduction/concepts/device) with `HTTP REST Client Driver`, and set the driver attributes `baseUrl=https://api.example.com`, `method=GET`, `timeout=5000`.
2. Add a temperature [Point](../introduction/concepts/point) (`pointTypeFlag=FLOAT`, `READ_ONLY`) to the [Profile](../introduction/concepts/profile) bound to the device, and set the point attributes `path=/api/v1/sensor/1`, `method=GET`, `responsePath=$.data.temperature`.
3. Start the driver, and within 30 seconds the extracted `25.6` shows up in the [PointValue](../introduction/concepts/point-value).

## Pitfalls

::: warning Response Path is simple dot-notation, not full JSONPath
The driver only supports field-by-field paths like `$.a.b.c`, splitting on `.` and drilling down from the root object. It does **not** support array indices (`[0]`), filters, wildcards, or other full JSONPath syntax. When you try to reach an array element, or the path doesn't resolve to a field, the driver falls back to returning the whole raw response—which is usually why a PointValue "looks wrong".
:::

::: tip Empty Response Path = whole response as the value
When the endpoint itself returns a bare value (a plain number or string), just leave `responsePath` empty and the driver uses the entire raw response as the PointValue. You only need a path when the response is JSON and you want one field out of it.
:::

::: warning Writing relies on the Body Template
A write command does not automatically place the value into the request body. You must author a template with `${value}` in the point's `bodyTemplate` (e.g. `{"value":${value}}`) for the driver to substitute the command parameter and send it. An empty template sends an empty request body.
:::

## Further reading

- [Driver](../introduction/concepts/driver) — the general driver model and registration mechanism
- [Attributes & Config](../introduction/concepts/attribute-config) — where attributes like `baseUrl` / `responsePath` come from across the three layers
- [Device Onboarding](../operation/device-onboarding) — a complete onboarding walkthrough
- [Modbus TCP Driver](./modbus-tcp) — onboarding the most common industrial Modbus protocol
