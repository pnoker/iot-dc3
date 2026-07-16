---
title: HTTP 驱动
---

# HTTP 驱动

`dc3-driver-http` 把任意 HTTP/REST 接口当作数据源接入 IoT DC3——周期性调用 REST 端点、从 JSON
响应里抽一个字段作为[位号值](../introduction/concepts/point-value)，并支持用请求体模板向接口写值。读完你能判断哪些设备/平台适合用它接入、每个属性该填什么、接不通时从哪里查。

## 协议背景

HTTP（HyperText Transfer Protocol）与建立在它之上的 REST 风格接口，是互联网最通用的请求/响应协议：客户端用一个**方法**（
`GET`/`POST`/`PUT`/`DELETE`）对一个**资源路径**发起请求，服务端返回状态码与一段报文（在物联网场景里通常是
JSON）。它无连接语义简单、几乎所有语言和工具都原生支持、调试方便，因此成为系统间集成的"最大公约数"。

在物联网四层参考架构里，HTTP 属于**网络层**中的**应用层消息协议**一类（与 MQTT、CoAP、LwM2M 并列）——它定义"
消息长什么样、怎么投递"，而不关心底层走的是 Wi-Fi 还是蜂窝。但要诚实地说：HTTP 报文头臃肿、保活成本高、不为受限设备设计，**并不适合
**电池供电终端的高频上报。它在 IoT 里的真正位置是**对接现成接口**——第三方平台开放的 REST API、设备自带的 RESTful
接口、把现场数据聚合成 HTTP 端点的数据网关。这类"上游已经讲 REST、只需周期取数"的场景，正是本驱动的用武之地。关于 HTTP 与
MQTT/CoAP/LwM2M 的取舍，见[物联网网络层章节](../foundations/iot-protocols)。

本驱动作为 HTTP 客户端（[驱动](../introduction/concepts/driver)类型 `DRIVER_CLIENT`），用 Spring WebFlux 的 `WebClient`
按[位号](../introduction/concepts/point)上配置的路径与方法去调接口，从 JSON 响应中按路径抽出一个值。两个本驱动特有的概念后面会反复出现：

- **响应路径（Response Path）**：从响应 JSON 里定位某字段的简单点号路径，如 `$.data.temperature` 表示取 `data` 对象下的
  `temperature`。留空则把整段响应原文作为值。
- **请求体模板（Body Template）**：写值时用的请求体模板，里面的 `${value}` 占位符会被命令参数替换成实际写入值。

## 属性配置

属性分两层填写：**驱动属性**（`driver-attribute`，设备级，决定连到哪个服务）、**位号属性**（`point-attribute`
，决定每个位号调哪个路径、用什么方法、取哪个字段——读和写都用这一份）。`application.yml` 里还声明了**命令属性**（
`command-attribute`），但当前 `write()`
不消费它（见下文命令属性小节）。三类属性的来历与覆盖关系见[属性与配置](../introduction/concepts/attribute-config)
。所有默认值均取自驱动的 `application.yml`。

### 驱动属性（设备级 `driver-attribute`）

接入一个 HTTP 数据源时，在[设备](../introduction/concepts/device)上填这些属性。它们决定连到哪个服务、带什么头、超时多久——同一台设备下所有位号共享这套连接参数：

| 属性       | code      | 类型     | 默认值    | 说明                                                 |
|----------|-----------|--------|--------|----------------------------------------------------|
| Base URL | `baseUrl` | STRING | （空）    | API 请求的基础地址（如 `https://api.example.com`）           |
| Method   | `method`  | STRING | `GET`  | 声明的默认 HTTP 方法，但当前实现未读取该属性（见下方告警）；实际方法只由位号属性决定      |
| Headers  | `headers` | STRING | （空）    | 自定义请求头，JSON 形式（如 `{"Authorization":"Bearer xxx"}`） |
| Timeout  | `timeout` | INT    | `5000` | 请求超时（毫秒），作用于响应超时 `responseTimeout`                 |

`baseUrl` 是必填项——驱动用它构造 `WebClient` 的 base URL，所有位号路径都拼在它之后；缺了它驱动的 `validate()` 会直接判定校验不通过。
`timeout` 默认 `5000` 毫秒，落到 Reactor Netty `HttpClient` 的 `responseTimeout` 上。

::: warning Headers 属性目前不生效
`headers` 在 `application.yml` 里已声明，但当前实现的 `getConnector()` 只为 `WebClient` 设置了固定的
`Content-Type: application/json`，**没有**读取并应用 `headers` 属性。需要带 `Authorization` 等自定义头鉴权的接口，暂时无法仅靠该属性接入。
:::

::: warning 驱动级 Method 属性目前不生效
`method` 在 `application.yml` 里声明为驱动级属性，但 `getConnector()` 只读取 `baseUrl` 与 `timeout`，从不读取驱动级
`method`；`read()`/`write()` 的方法只取自位号属性 `method`，缺省回退到硬编码的 `GET`。因此在设备上设 `method=POST` 不会生效——它与
`headers` 同属"已声明但未应用"的属性。HTTP 方法实际只由位号属性 `method` 决定。
:::

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：调哪个路径、用什么方法、（写时）发什么请求体、从响应里取哪个字段。读/写实际使用的
HTTP 方法只来自位号的 `method`，缺省时回退到硬编码的 `GET`，与驱动级 `method` 属性无关（驱动级 `method` 不被实现读取，见上）：

| 属性            | code           | 类型     | 默认值   | 说明                                     |
|---------------|----------------|--------|-------|----------------------------------------|
| Path          | `path`         | STRING | （空）   | API 路径（如 `/api/v1/sensor/{id}`）        |
| Method        | `method`       | STRING | `GET` | 本位号的 HTTP 方法，缺省回退到硬编码 `GET`            |
| Body Template | `bodyTemplate` | STRING | （空）   | 带 `${value}` 占位符的请求体模板                 |
| Response Path | `responsePath` | STRING | （空）   | 从 JSON 响应抽值的路径（如 `$.data.temperature`） |

::: tip path 拼在 baseUrl 之后
实际请求 URL 是 `baseUrl + path`。例如 `baseUrl=https://api.example.com`、`path=/api/v1/sensor/1`
，驱动就请求 `https://api.example.com/api/v1/sensor/1`。
:::

::: warning Response Path 是简单点号路径，不是完整 JSONPath
驱动把 `$.` 前缀去掉后按 `.` 逐段从根对象往下钻，只支持 `$.a.b.c` 这样的逐层取字段写法。**不支持**数组下标（`[0]`
）、过滤器、通配符等完整 JSONPath 语法。取数组元素、或路径取不到对应字段时，会退回返回整段响应原文——这往往就是位号值"
看起来不对"的根因。响应本身就是裸值（纯数字/字符串）时，`responsePath` 留空即可，驱动把整段响应当作位号值。
:::

::: warning 写值要靠 Body Template 渲染
写命令不会自动把值塞进请求体。必须在位号的 `bodyTemplate` 里写好带 `${value}` 的模板（如 `{"value":${value}}`
），驱动才会把命令参数替换进去发出；模板为空时发的是空请求体。
:::

### 命令属性（`command-attribute`）

::: warning 命令属性目前不被写入路径消费
`application.yml` 在 `command-attribute` 下声明了 `path` 与 `method`（`default-value: POST`），但驱动的 SPI `write()` 签名只接收
`driverConfig` 与 `pointConfig`，不传入命令属性映射；`write()` 实际从位号属性（`pointConfig`）读取 `path`/`method`/
`bodyTemplate`，且 `method` 缺省回退到硬编码的 `GET`（不是 `POST`）。因此下表列出的 `command-attribute`
当前是声明但未生效的死配置，写操作的路径与方法请填在位号属性上。
:::

下表为 `application.yml` 中的声明值（当前 `write()` 未读取）：

| 属性     | code     | 类型     | 默认值    | 说明                             |
|--------|----------|--------|--------|--------------------------------|
| Path   | `path`   | STRING | （空）    | 命令的 API 路径（当前未被 `write()` 读取）  |
| Method | `method` | STRING | `POST` | 命令的 HTTP 方法（当前未被 `write()` 读取） |

### 采集与健康检查

这些参数定在 `application.yml` 的 `dc3.driver.schedule` 与 `health` 下，无需在设备上填：

- **采集周期**：`application.yml` 基线定时读 cron `0/30 * * * * ?`（每 30 秒读一轮）；默认激活的 `dev` profile 在
  `application-dev.yml` 把 read cron 覆盖为 `0/5 * * * * ?`（每 5 秒），故开箱即用的实际采集间隔是 5 秒。
- **健康检查**：设备健康检查 cron `0/15 * * * * ?`，租约超时 `45 秒`
  ——在线状态机制见[设备](../introduction/concepts/device)。
- **在线判定**：`health()` 以"`clientMap` 里是否为该设备建过 `WebClient`"判断在线——首次成功读/写后建立连接即视为在线；读/写抛异常时驱动会
  `clientMap.remove(deviceId)` 摘除连接，下一轮重建。

## 故障排查

::: warning 连不上 / 一直 offline
先确认 `baseUrl` 可达：用 `curl <baseUrl><path>` 在驱动所在主机直接验证网络与端口是否通。驱动以"是否建过 `WebClient`"
判在线，首次读/写失败会立即摘除连接，因此 `baseUrl` 错、DNS 解析不了、或目标端口被防火墙挡，都会表现为设备始终 offline。
:::

::: warning 请求超时
默认 `timeout=5000` 毫秒落在响应超时上。目标接口本身慢、或网络抖动时会触发超时并把本轮读/写判为失败、摘除连接。先用
`curl -w '%{time_total}'` 量一下真实耗时，确认是接口慢还是链路慢，再据此调大 `timeout`。
:::

::: warning 位号值看起来不对 / 总是整段 JSON
多半是 `responsePath` 没匹配上。该驱动只认 `$.a.b.c` 的逐层点号路径，路径写错、字段名大小写不符、或想取数组元素（
`$.list[0].v`），都会因取不到字段而**退回返回整段响应原文**。对照接口真实响应逐层核对字段名；要取数组里的元素，当前实现做不到。
:::

::: warning 鉴权接口 401/403
当前实现未应用 `headers` 属性（见上文驱动属性表的告警）。需要 `Authorization`、`X-Api-Key` 等请求头的接口，暂时无法仅凭设备属性接入，会被服务端拒为
401/403。
:::

::: warning 写命令报错或没生效
写值经由位号的 `bodyTemplate` 渲染：模板为空时发的是**空请求体**，接口若要求 JSON body 会拒绝。确认 `bodyTemplate` 里含
`${value}` 占位符、且渲染后是接口接受的合法 JSON；写失败时驱动抛 `WritePointException` 并摘除连接。
:::

## 在 IoT DC3 中如何落地

- **dc3.driver.code**：`HttpDriver`（驱动名 `HTTP REST Client Driver`，类型 `DRIVER_CLIENT`）。该 code 是稳定路由标识，不可随意改。
- **读**：✓ 已实现。定时按位号 `path`/`method` 调接口，`responsePath` 从 JSON 抽值。
- **写**：✓ 已实现。把位号 `bodyTemplate` 中 `${value}` 替换为命令参数后发请求。
- **订阅/上报**：— 不支持。HTTP 是请求/响应模型，驱动主动发起，无被动推送通道。

以上读/写能力与[驱动能力矩阵](./matrix)的 `HTTP (HttpDriver)` 行一致。

::: info 实现状态：可用
`HttpDriverCustomServiceImpl` 的 `initial()`/`read()`/`write()`/`health()`/`validate()` 均已完整实现，是可投入采集的成熟驱动。两处实现边界须知：①
`responsePath` 仅支持简单点号路径，不支持数组/过滤器（见上）；② `headers` 属性已声明但尚未在连接中应用，自定义请求头当前无法生效。
:::

### 最小接入示例

把一个返回 `{"data":{"temperature":25.6}}` 的接口接进来：

1. 选 `HTTP REST Client Driver` 创建[设备](../introduction/concepts/device)，驱动属性填
   `baseUrl=https://api.example.com`、`method=GET`、`timeout=5000`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=FLOAT`、`READ_ONLY`），位号属性填 `path=/api/v1/sensor/1`、`method=GET`、`responsePath=$.data.temperature`。
3. 启动驱动，数秒内就能在[位号值](../introduction/concepts/point-value)里看到抽取出的 `25.6`（默认 `dev` profile 每 5
   秒采一轮）。

## 延伸阅读

- [驱动总览](./index) — 全部驱动分组与选型入口
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [物联网网络层章节](../foundations/iot-protocols) — HTTP 与 MQTT/CoAP/LwM2M 在网络层的定位与取舍
