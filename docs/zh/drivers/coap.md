---
title: CoAP 驱动
---

# CoAP 驱动

> **`dc3-driver-coap` 把 CoAP 设备接入 IoT DC3**——以设备资源路径为目标，周期性发 GET 采数，并支持向资源发 PUT 写值。

CoAP（Constrained Application Protocol，受限应用协议）是为低功耗、低带宽的物联网终端设计的轻量协议，跑在 UDP 上、默认端口 `5683`，请求/响应模型类似精简版 HTTP（用 GET / PUT / POST 等方法访问"资源路径"）。常见于电池供电的传感器、嵌入式网关、NB-IoT/6LoWPAN 终端等"省电省流量"的场景。本驱动基于 Eclipse Californium 库，作为 CoAP 客户端（client）主动连接设备：读路径对资源发 CoAP GET，写路径对资源发 CoAP PUT。

- **驱动名 / code**：`CoAP Driver` / `CoapDriver`
- **类型**：`DRIVER_CLIENT`（主动连设备）

## 驱动配置（设备级 `driver-attribute`）

接入一台 CoAP 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Device Host | `deviceHost` | STRING | `localhost` | CoAP device host address |
| Device Port | `devicePort` | INT | `5683` | CoAP device port |

驱动用这两个属性拼出设备根地址 `coap://<deviceHost>:<devicePort>`，再接上位号的资源路径访问具体资源。

## 位号配置（`point-attribute`）

每个[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Read Path | `readPath` | STRING | `/sensors` | CoAP resource path for reading point data |
| Write Path | `writePath` | STRING | `/actuators` | CoAP resource path for writing point data |
| Content Format | `contentFormat` | STRING | `json` | Content format: json, text, cbor, octet-stream |

::: tip 读写各走各的资源路径
采集时驱动对 `coap://<host>:<port><readPath>` 发 GET，返回的响应体（payload）就是这个[位号](../introduction/concepts/point)的[位号值](../introduction/concepts/point-value)；下发写命令时对 `<writePath>` 发 PUT，请求体是要写的值。`readPath` 和 `writePath` 互不影响，只读位号只配 `readPath` 即可，`writePath` 空着不会被用到。
:::

CoAP 没有独立的 `command-attribute` 配置表——可写位号的写入目标由位号自身的 `writePath` 决定，下发写命令时驱动直接对该路径发 PUT，无需额外的命令属性。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒采一轮，对每个位号的 `readPath` 发一次 GET）。
- **自定义任务**：驱动内置一个 custom 调度，默认 cron `0/5 * * * * ?`（每 5 秒一次），用于驱动自有的周期逻辑，与位号采集相互独立。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把地址 `192.168.1.20:5683`、温度资源在 `/temp` 的一台 CoAP 传感器接进来：

1. 选 `CoAP Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `deviceHost=192.168.1.20`、`devicePort=5683`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`），point 属性填 `readPath=/temp`、`contentFormat=json`（`writePath` 留空）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到对 `coap://192.168.1.20:5683/temp` GET 回来的值。

## 易错点

::: warning CoAP 跑在 UDP 上，连不通先查防火墙和端口
CoAP 默认走 **UDP 5683**（不是 TCP）。设备无响应时，`read` 会按超时处理并跳过本轮——日志里看到 `statusCode=timeout` 多半是 UDP 包没通：先确认设备在线、防火墙放行 UDP 5683、`deviceHost`/`devicePort` 填对，而不是路径配错。
:::

::: tip contentFormat 是协议声明
`contentFormat` 声明资源的内容格式（`json` / `text` / `cbor` / `octet-stream`）。当前驱动按原始 payload 返回[位号值](../introduction/concepts/point-value)、未据它做格式解析。拿不准设备实际返回格式时，可先用 CoAP 客户端（如 `coap-client`）手动 GET 一次看返回格式再填。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `deviceHost` / `readPath` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 工业现场的 Modbus 主站驱动
