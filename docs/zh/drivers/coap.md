---
title: CoAP 驱动
---

<script setup>
import CoapDiagram from '../../.vitepress/theme/components/CoapDiagram.vue'
</script>


# CoAP 驱动

`dc3-driver-coap` 把 CoAP 设备接入 IoT DC3。它基于 Eclipse Californium，既能作为 **CoAP 客户端**主动连设备（读发 GET、写发
PUT），也能作为 **CoAP 服务端**监听设备主动 POST 上报的遥测。读完你能在[设备](../introduction/concepts/device)上配好
`deviceHost`/`devicePort`、在[位号](../introduction/concepts/point)上配好读写资源路径，并定位常见的"采不到值 / UDP 连不通"
问题。

> 你在这里：网络层"轻协议侧"的一个落地驱动。CoAP 在协议层的请求/响应模型、UDP/DTLS 端口、Observe
> 概念见[IoT 协议与无线网络](../foundations/iot-protocols)。

## 协议背景

CoAP（Constrained Application Protocol，受限应用协议）是 IETF 为低功耗、低带宽的物联网终端设计的轻量协议（RFC 7252）。它保留了
HTTP 熟悉的**请求/响应 + 方法（GET/PUT/POST/DELETE）+ 资源路径**模型，但把报文压到几十字节，跑在 **UDP** 上、默认端口 `5683`
（加密用 DTLS 的 CoAPS 走 `5684`）。无连接的 UDP 省去了 TCP 握手与保活开销，对电池供电、偶尔醒来上报一次的终端极友好；代价是可靠性要靠
CoAP 自己的 CON/NON 确认机制补回来。常见于电池供电的传感器、嵌入式网关、NB-IoT/6LoWPAN 终端等"省电省流量"的场景。

在[物联网四层架构](../foundations/iot-protocols)里，CoAP 属于**网络层**的应用层消息协议：它定义"
一条消息长什么样、怎么投递、可靠到什么程度"，与底层用什么无线无关——同一个 CoAP 报文，可以跑在 Wi-Fi 上，也可以跑在 NB-IoT
蜂窝链路上。CoAP 的通信模型既支持客户端**主动请求**资源，也支持服务端在客户端 POST 时**被动接收**，本驱动两侧都实现了：

<CoapDiagram lang="zh" />

客户端模式是默认形态，由 IoT DC3 的[采集调度](../introduction/concepts/driver)按 cron 周期对每个位号的 `readPath` 发
GET，下发写命令时对 `writePath` 发 PUT。服务端模式则反过来：驱动监听一个 CoAP 端口，设备主动把遥测 POST 到 `/data`
资源，驱动解析后转发上报。两种模式由 `dc3.driver.coap.mode` 决定（见下文属性配置）。

## 属性配置

接入一台 CoAP 设备，主要在两个层面填[属性](../introduction/concepts/attribute-config)：设备级的连接参数（`driver-attribute`
）和每个位号的资源路径（`point-attribute`）。此外驱动还暴露一组进程级的 `dc3.driver.coap.*` Spring
配置项（控制客户端/服务端模式、超时、DTLS），它们不在设备配置里，而是运维通过环境/配置文件调。下面各属性、类型、默认值均取自驱动的
`application.yml` 与 `CoapProperties`（`dc3-driver-coap` 模块）。

### 驱动属性（设备级 `driver-attribute`）

驱动属性回答"连到哪台设备"。在[设备](../introduction/concepts/device)上为每台 CoAP 设备填一组：

| 属性          | code         | 类型     | 默认值         | 说明                   |
|-------------|--------------|--------|-------------|----------------------|
| Device Host | `deviceHost` | STRING | `localhost` | CoAP 设备主机地址（IP 或主机名） |
| Device Port | `devicePort` | INT    | `5683`      | CoAP 设备端口（标准 `5683`） |

驱动用这两个属性拼出设备根地址 `coap://<deviceHost>:<devicePort>`，再接上位号的资源路径访问具体资源。驱动按设备根地址（URI）缓存
`CoapClient`（一个 URI 一个客户端），设备被删除或更新时释放对应客户端。配置校验（`validate`）会要求 `deviceHost`、`devicePort`
两项都非空，缺一即报错、设备无法启动。

### 位号属性（`point-attribute`）

位号属性回答"读写这台设备的哪个资源路径"。每个[位号](../introduction/concepts/point)填一组：

| 属性             | code            | 类型     | 默认值          | 说明                                               |
|----------------|-----------------|--------|--------------|--------------------------------------------------|
| Read Path      | `readPath`      | STRING | `/sensors`   | 采集时 GET 的 CoAP 资源路径                              |
| Write Path     | `writePath`     | STRING | `/actuators` | 下发写值时 PUT 的 CoAP 资源路径                            |
| Content Format | `contentFormat` | STRING | `json`       | 内容格式声明：`json` / `text` / `cbor` / `octet-stream` |

::: tip 读写各走各的资源路径
采集时驱动对 `coap://<host>:<port><readPath>` 发 GET，返回的响应体（payload）就是这个[位号](../introduction/concepts/point)
的[位号值](../introduction/concepts/point-value)；下发写命令时对 `<writePath>` 发 PUT，请求体是要写的值。`readPath` 和
`writePath` 互不影响，只读位号只配 `readPath` 即可，`writePath` 空着不会被用到。位号配置校验（`validatePoint`）只强制要求
`readPath` 非空。
:::

CoAP 没有独立的 `command-attribute` 配置表——可写位号的写入目标由位号自身的 `writePath` 决定，下发写命令时驱动直接对该路径发
PUT，无需额外的命令属性。

### 进程级配置（`dc3.driver.coap.*`）

这组配置控制驱动进程整体行为（客户端超时、是否起服务端、DTLS 加密），通过配置文件或环境变量设置，对所有设备生效。
`application.yml` 里未显式列出，因此默认全部取 `CoapProperties` 的内置默认值：

| 配置项                   | 默认值       | 说明                                                                                                                |
|-----------------------|-----------|-------------------------------------------------------------------------------------------------------------------|
| `mode`                | `CLIENT`  | 工作模式：`CLIENT`（仅客户端，主动读写）/ `SERVER`（仅服务端，监听上报）/ `BOTH`                                                             |
| `serverHost`          | `0.0.0.0` | 服务端绑定地址（`SERVER`/`BOTH` 模式生效）                                                                                     |
| `serverPort`          | `5683`    | 服务端监听端口（`SERVER`/`BOTH` 模式生效）                                                                                     |
| `secureEnabled`       | `false`   | 是否启用 DTLS 加密                                                                                                      |
| `clientTimeout`       | `5000`    | 客户端 GET 的报文交换生命周期（毫秒，最小 100）                                                                                      |
| `clientAckTimeout`    | `2000`    | 客户端 CON 确认超时（毫秒，最小 100）                                                                                           |
| `clientMaxRetransmit` | `4`       | 客户端最大重传次数（最小 1）                                                                                                   |
| `dtls.*`              | 空         | DTLS 凭据：`pskIdentity` / `pskSecret` 或证书路径 `trustStorePath` / `identityCertificatePath` / `identityPrivateKeyPath` |

::: info 默认即客户端模式，服务端模式需显式开
默认 `mode=CLIENT`，驱动作为客户端按 cron 周期主动 GET/PUT，不监听任何端口。要让设备主动 POST 上报，需把 `mode` 设为
`SERVER` 或 `BOTH`——此时驱动起一个 CoAP 服务端、在 `serverPort` 上注册 `/data` 资源接收上报。`secureEnabled`/`dtls.*` 是为
DTLS 预留的配置项，当前 `CoapClientManager` 与 `CoapServerManager` 建连时尚未据它装配 DTLS 端点（明文 UDP），公网加密能力以代码为准。
:::

## 故障排查

CoAP 接入失败大多集中在 UDP 链路、资源路径、上报格式三类。按下面顺序排查：

1. **UDP 连不通（采不到值 / `statusCode=timeout`）**。CoAP 默认走 **UDP 5683**（不是 TCP）。设备无响应时，客户端 GET 超时返回
   `null`，`read` 按失败处理、跳过本轮并打 `CoAP read failed ... statusCode=timeout`。先用 CoAP 客户端（如
   `coap-client -m get coap://<host>:5683<readPath>`）手动验链路：确认设备在线、防火墙放行 **UDP** 5683、`deviceHost`/
   `devicePort` 填对，而不是先怀疑路径配错。

2. **能连上但路径取不到（4.04 Not Found）**。`readPath` 写错会让设备返回 `4.04`，此时 `response.isSuccess()` 为 false、`read`
   同样返回 `null`。核对资源路径大小写与前导 `/`，必要时先 GET 设备的 `/.well-known/core` 看它实际暴露了哪些资源。

3. **响应慢于超时被误判**。客户端默认 `clientTimeout=5000ms`、`clientAckTimeout=2000ms`、最大重传 `4` 次。链路 RTT
   高（如蜂窝/NB-IoT）的设备可能在默认窗口内来不及应答而被判超时——调大 `dc3.driver.coap.clientTimeout`/`clientAckTimeout`
   ，而不是缩短采集周期。

4. **写命令返回失败**。写走 PUT 到 `writePath`，请求体为命令传入的值。失败（PUT 超时或设备返回非 2.xx）时 `write` 返回
   `false`、写命令不回显。先确认 `writePath` 是设备上可写的资源、且设备接受 PUT 方法。注意：驱动 PUT 固定以
   `application/json` 媒体类型发送，与位号的 `contentFormat` 声明无关（见下方易错点）。

5. **服务端模式收不到上报**。`mode` 必须为 `SERVER` 或 `BOTH`，设备要 POST 到 `coap://<驱动host>:<serverPort>/data`
   。上报体必须是能反序列化成 `PointValue` 的 JSON（至少含 `deviceId`、`pointId`），否则驱动打 `missingIdentity` /
   `parse failed` 并丢弃。空 body 会被回 `4.00 Bad Request`。

6. **设备在线状态抖动**。健康检查默认每 15 秒一次、租约超时 45 秒。频繁 online/offline 跳变多半是 UDP
   丢包或设备响应慢于超时——在线状态机制见[设备](../introduction/concepts/device)。

::: warning contentFormat 只是声明，当前不参与解析
`contentFormat` 声明资源的内容格式（`json` / `text` / `cbor` / `octet-stream`），但当前驱动按原始 payload（
`response.getResponseText()`）直接返回[位号值](../introduction/concepts/point-value)、**未据它做格式解析**；写方向也固定用
`application/json` 媒体类型 PUT，不读这个属性。拿不准设备实际返回格式时，先用 CoAP 客户端手动 GET 一次看返回内容再填。
:::

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`CoapDriver`（类型 `DRIVER_CLIENT`）。这是稳定的路由标识，不要随意改。
- **读能力**：✓ 已实现。客户端模式按 cron 对每个位号的 `readPath` 发
  GET，响应体作为[位号值](../introduction/concepts/point-value)上报。
- **写能力**：✓ 已实现。对位号的 `writePath` 发 PUT（`application/json`），下发命令时触发。
- **订阅/上报**：— 不计入[驱动能力矩阵](./matrix)的"订阅"列。矩阵中 CoAP 为「✓ / ✓ / —」，指的是 SDK 位号模型下的**主动读 /
  主动写 / 无订阅**。CoAP 的 **Observe**（RFC 7641，资源变化推送）在本驱动里**尚未接通**：仅有 `CoapObserveHandler`
  接口定义，无任何实现或调用方。
- **服务端上报（额外能力）**：`SERVER`/`BOTH` 模式下驱动起 CoAP 服务端、在 `/data` 接收设备 POST 的 `PointValue` JSON
  并转发上报。这是矩阵之外的一条独立链路，需显式开 `mode`。
- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒采一轮），在驱动 `application.yml` 的 `schedule.read` 配置。
  `schedule.custom`（默认 cron `0/5 * * * * ?`）虽启用，但 `schedule()` 实现为空操作，不做位号采集之外的周期逻辑。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。

::: info 实现状态：可用（客户端读写 + 服务端上报），Observe 未实现
本驱动的**客户端读写**与**服务端上报接收**均为完整实现（非骨架），底层基于 Eclipse Californium。唯一未接通的是 CoAP *
*Observe** 订阅推送：`CoapObserveHandler` 仅是接口、无实现，因此驱动当前不能"订阅"资源由设备变更推送——需要事件驱动上报的场景请改用服务端
POST 模式。DTLS 加密（`secureEnabled`/`dtls.*`）为配置预留、尚未在建连时装配，以代码为准。
:::

### 最小接入示例

把地址 `192.168.1.20:5683`、温度资源在 `/temp` 的一台 CoAP 传感器接进来：

1. 选 `CoAP Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `deviceHost=192.168.1.20`、
   `devicePort=5683`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`
   ），point 属性填 `readPath=/temp`、`contentFormat=json`（`writePath` 留空）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到对 `coap://192.168.1.20:5683/temp` GET
   回来的值。
4. 若该位号需可写，给它配写[命令](../introduction/concepts/command)、并填 `writePath`，下发时驱动对该路径 PUT。

::: tip 设备主动上报选服务端模式
若设备只会偶尔醒来主动上报、不接受被轮询，把 `dc3.driver.coap.mode` 设为 `SERVER`，让设备 POST 到驱动的 `/data` 资源（上报体为含
`deviceId`/`pointId` 的 `PointValue` JSON）。这避免了对休眠设备做无谓的周期 GET。
:::

## 延伸阅读

- [驱动总览](./index) — 全部驱动入口与分类
- [驱动能力矩阵](./matrix) — 读/写/订阅能力一览，含 CoAP 行
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [IoT 协议与无线网络](../foundations/iot-protocols) — CoAP/LwM2M 等轻协议的请求/响应模型、UDP/DTLS 与 Observe
- [LwM2M 驱动](./lwm2m) — 架在 CoAP 之上、带设备管理对象模型的驱动
