---
title: HTTP 驱动
---

# HTTP 驱动

> **`dc3-driver-http` 把 HTTP/REST 接口当作数据源接入 IoT DC3**——周期性调用 REST 端点，从 JSON 响应里取值，并支持用请求体模板向接口写值的命令。

很多设备、网关或上游系统不直接说工业协议，而是暴露一个 HTTP/REST 接口（返回一段 JSON）。本驱动作为 HTTP 客户端（[驱动](../introduction/concepts/driver) 类型 `DRIVER_CLIENT`），用 Spring WebFlux `WebClient` 按[位号](../introduction/concepts/point)上配置的路径与方法去调接口，从 JSON 响应中按路径抽取出一个值作为[位号值](../introduction/concepts/point-value)。适用于：第三方平台开放 API、设备自带的 RESTful 接口、HTTP 形态的数据网关。

下面先解释两个本驱动特有的概念，后面配置表会反复用到：

- **JSON 路径（Response Path）**：从响应 JSON 里定位某个字段的简单点号路径，如 `$.data.temperature` 表示取 `data` 对象下的 `temperature`。留空则把整段响应原文作为值。
- **请求体模板（Body Template）**：写值时用的请求体模板，里面的 `${value}` 占位符会被命令参数替换成实际写入值。

## 驱动名 / code / 类型

- **驱动名 / code**：`HTTP REST Client Driver` / `HttpDriver`
- **类型**：`DRIVER_CLIENT`（驱动主动发起 HTTP 请求）

## 驱动配置（设备级 `driver-attribute`）

接入一个 HTTP 数据源时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。它们决定连到哪个服务、用什么默认方法、带什么头、超时多久：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Base URL | `baseUrl` | STRING | （空）| Base URL for API requests (e.g. https://api.example.com) |
| Method | `method` | STRING | `GET` | Default HTTP method (GET, POST, PUT, DELETE) |
| Headers | `headers` | STRING | （空）| Custom headers as JSON (e.g. {"Authorization":"Bearer xxx"}) |
| Timeout | `timeout` | INT | `5000` | Request timeout in milliseconds |

`baseUrl` 是必填项——它是所有位号路径的前缀，缺了驱动无法建立连接。

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：调哪个路径、用什么方法、（写时）发什么请求体、从响应里取哪个字段：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Path | `path` | STRING | （空）| API path (e.g. /api/v1/sensor/{id}) |
| Method | `method` | STRING | `GET` | HTTP method override for this point |
| Body Template | `bodyTemplate` | STRING | （空）| Request body template with ${value} placeholder |
| Response Path | `responsePath` | STRING | （空）| JSON path to extract value (e.g. $.data.temperature) |

::: tip path 拼在 baseUrl 之后
实际请求的 URL 是 `baseUrl + path`。例如 `baseUrl=https://api.example.com`、`path=/api/v1/sensor/1`，驱动就会请求 `https://api.example.com/api/v1/sensor/1`。位号的 `method` 会覆盖驱动级默认方法。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Path | `path` | STRING | （空）| API path for command |
| Method | `method` | STRING | `POST` | HTTP method for command |

写值时，驱动把位号的 `bodyTemplate` 里的 `${value}` 替换成命令参数，再用此处的方法向 `path` 发请求。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。
- 本驱动以"设备是否已建立过 `WebClient` 连接"判断在线：首次成功读/写后即视为在线。

## 最小接入示例

把一个返回 `{"data":{"temperature":25.6}}` 的天气 API 接进来：

1. 选 `HTTP REST Client Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `baseUrl=https://api.example.com`、`method=GET`、`timeout=5000`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `path=/api/v1/sensor/1`、`method=GET`、`responsePath=$.data.temperature`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到抽取出的 `25.6`。

## 易错点

::: warning Response Path 用的是简单点号路径，不是完整 JSONPath
驱动只支持 `$.a.b.c` 这样的逐层取字段写法，按 `.` 拆段从根对象往下钻。**不支持**数组下标（`[0]`）、过滤器、通配符等完整 JSONPath 语法。取数组里的元素、或路径取不到对应字段时，会退回返回整段响应原文——这往往就是位号值"看起来不对"的原因。
:::

::: tip Response Path 留空 = 整段响应作为值
当接口本身就只返回一个裸值（如纯数字或纯字符串），`responsePath` 留空即可，驱动会把整段响应原文当作位号值。只有响应是 JSON 且要取其中某个字段时，才需要填路径。
:::

::: warning 写值要靠 Body Template 渲染
写命令不会自动把值塞进请求体。必须在位号的 `bodyTemplate` 里写好带 `${value}` 的模板（如 `{"value":${value}}`），驱动才会把命令参数替换进去发出。模板为空时发的是空请求体。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `baseUrl` / `responsePath` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 工业现场最常见的 Modbus 协议接入
