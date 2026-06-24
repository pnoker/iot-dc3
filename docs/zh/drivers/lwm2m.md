---
title: LwM2M 驱动
---

# LwM2M 驱动

> **`dc3-driver-lwm2m` 把 LwM2M 设备接入 IoT DC3**——内嵌一个 LwM2M 服务端，接受设备注册，按 Object / Object Instance / Resource 三段路径读写设备上的资源。

LwM2M（Lightweight M2M，轻量级 M2M）是 OMA 制定的物联网设备管理与数据采集协议，跑在 CoAP 之上（默认 UDP `5683`，加密用 DTLS/CoAPS `5684`）。它把设备能力抽象成一棵"对象树"：每个 **Object**（如 `3303`=温度）下有若干 **Object Instance**（同类资源的多个实例），每个实例下有若干 **Resource**（如 `5700`=传感器读数）。访问一个具体的值，就是给出 `/<objectId>/<objectInstanceId>/<resourceId>` 这条路径。它常见于电池供电、远程部署的终端（NB-IoT 模组、智能表计、环境传感器等"需要远程管理 + 省电"的场景）。

与 Modbus、CoAP 这类驱动主动去连设备不同，本驱动基于 Eclipse Leshan 内嵌一个 **LwM2M 服务端**：设备作为 LwM2M 客户端，用自己的 **endpoint 名**注册到这个服务端；注册成功后，驱动按位号配置的路径对该 endpoint 发起读/写。

- **驱动名 / code**：`LwM2M Driver` / `Lwm2mDriver`
- **类型**：`DRIVER_CLIENT`

::: warning 当前为骨架实现（WIP）
该驱动目前是骨架版本，协议级 I/O 尚未完整实现（`Lwm2mDriverCustomServiceImpl` 的类注释明确标注 "work-in-progress skeleton"）。请把它当作接入模板/起点，而非可直接上生产的成品。
:::

## 驱动配置（设备级 `driver-attribute`）

接入一台 LwM2M 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Endpoint | `endpoint` | STRING | （空） | LwM2M device endpoint name |
| Server Host | `serverHost` | STRING | `0.0.0.0` | LwM2M server bind address |
| Server Port | `serverPort` | INT | `5683` | CoAP port |
| Secure Port | `securePort` | INT | `5684` | CoAPS/DTLS port |
| Security Mode | `securityMode` | STRING | `NOSEC` | Security mode: NOSEC, PSK |
| PSK Identity | `pskIdentity` | STRING | （空） | PSK identity (when securityMode=PSK) |
| PSK Key | `pskKey` | STRING | （空） | PSK key in HEX (when securityMode=PSK) |

其中 `endpoint` 是把这台设备和已注册的 LwM2M 客户端对应起来的关键——它必须与设备注册时上报的 endpoint 名一字不差。`serverHost` / `serverPort` / `securePort` 决定内嵌服务端监听在哪个地址和端口。

## 位号配置（`point-attribute`）

每个[位号](../introduction/concepts/point)上填一条 LwM2M 资源路径：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Object ID | `objectId` | INT | `0` | LwM2M Object ID (e.g. 3303=Temperature) |
| Object Instance ID | `objectInstanceId` | INT | `0` | LwM2M Object Instance ID |
| Resource ID | `resourceId` | INT | `0` | LwM2M Resource ID (e.g. 5700=Sensor Value) |
| Observe | `observe` | STRING | `false` | Enable LwM2M Observe: true, false |

::: tip 三段路径决定读哪个资源
驱动用 `objectId` / `objectInstanceId` / `resourceId` 拼成路径 `/<objectId>/<objectInstanceId>/<resourceId>`，对设备 endpoint 发起读取，返回值即为该[位号](../introduction/concepts/point)的[位号值](../introduction/concepts/point-value)。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要和该 Resource 实际的数据类型对得上。`observe=true` 表示对该资源开启 LwM2M Observe（订阅式上报），由设备在值变化时主动推送，而非每轮主动拉取。
:::

LwM2M 没有独立的 `command-attribute` 配置表（yml 中 `command-attribute: [ ]` 为空）——可写位号的写入目标就是位号自身的那条 `/<objectId>/<objectInstanceId>/<resourceId>` 路径，下发写命令时驱动直接对该路径写值，无需额外的命令属性。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮，对每个位号的资源路径发起一次读取）。
- **自定义任务**：驱动内置一个 custom 调度，默认 cron `0/5 * * * * ?`（每 5 秒一次），用于驱动自有的周期逻辑，与位号采集相互独立。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。设备在线与否取决于其 endpoint 是否仍注册在内嵌服务端上——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把一台 endpoint 名为 `urn:imei:860000000000001`、温度资源在 `/3303/0/5700` 的 LwM2M 传感器接进来：

1. 选 `LwM2M Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `endpoint=urn:imei:860000000000001`、`serverHost=0.0.0.0`、`serverPort=5683`、`securityMode=NOSEC`（明文模式无需填 PSK）。
2. 让该 LwM2M 客户端用同样的 endpoint 名注册到这台服务的 `5683` 端口。
3. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`），point 属性填 `objectId=3303`、`objectInstanceId=0`、`resourceId=5700`。
4. 启动驱动，设备注册成功后，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到读回的温度值。

## 易错点

::: warning endpoint 名必须和设备注册时一字不差
设备在线判定靠 `endpoint` 名在内嵌服务端上匹配。设备实际注册用的 endpoint（常见形如 `urn:imei:<IMEI>` 或厂商自定义串）和[设备](../introduction/concepts/device)上填的 `endpoint` 只要差一个字符，就匹配不上：设备会一直显示离线、读取也拿不到值。接入前先确认设备固件里注册用的 endpoint 名到底是什么。
:::

::: tip 启用 PSK 时 Identity 和 Key 要成对配，端口走 5684
`securityMode=PSK` 时，加密握手（DTLS）走 `securePort`（默认 `5684`），且 `pskIdentity` 与 `pskKey` 必须与设备侧预置的一致，`pskKey` 是 **HEX 字符串**。三者任一不匹配，DTLS 握手就会失败、设备注册不上。只是先打通链路时，用 `securityMode=NOSEC` 走明文 `5683` 端口最省事。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `endpoint` / `objectId` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [CoAP 驱动](./coap) — LwM2M 底层依赖的 CoAP 协议驱动
