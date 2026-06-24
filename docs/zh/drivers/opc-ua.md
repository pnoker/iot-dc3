---
title: OPC UA 驱动
---

# OPC UA 驱动

> **`dc3-driver-opc-ua` 把 OPC UA 服务端接入 IoT DC3**——以 OPC UA 服务端上的节点（Node）为目标，周期性读取节点值，并支持向节点写值。

OPC UA（OPC Unified Architecture，统一架构）是工业自动化领域的跨平台数据互通标准，PLC、SCADA、MES 和各类边缘网关普遍内置 OPC UA 服务端，把现场数据以"节点树"的形式对外暴露。每个节点由一个 **NodeId** 唯一标识，NodeId 由"命名空间索引（namespace index）+ 标识符（identifier）"两部分组成。本驱动作为 OPC UA 客户端（client），基于 Eclipse Milo 通过 `opc.tcp://` 二进制协议连到一个或多个服务端，按[位号](../introduction/concepts/point)上配置的命名空间与标识符读、写节点值。

- **驱动名 / code**：`OPC UA Driver` / `OpcUaDriver`
- **类型**：`DRIVER_CLIENT`（主动连服务端）

## 驱动配置（设备级 `driver-attribute`）

接入一台 OPC UA 服务端设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | OPC UA host |
| Port | `port` | INT | `18600` | OPC UA port |
| Path | `path` | STRING | `/` | OPC UA endpoint path |

三者拼成端点地址 `opc.tcp://<host>:<port><path>`，例如 `opc.tcp://192.168.1.20:4840/milo`。

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填，二者共同定位一个 OPC UA 节点（NodeId）：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Namespace | `namespace` | INT | `5` | OPC UA namespace index |
| Tag | `tag` | STRING | `TAG` | OPC UA node tag name |

::: tip NodeId = namespace + tag
驱动把 `namespace`（命名空间索引）和 `tag`（字符串标识符）拼成 NodeId 去读写节点。例如 `namespace=2`、`tag=Demo.Static.Float` 对应的就是命名空间 2 下名为 `Demo.Static.Float` 的节点。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要和节点的实际值类型对得上。
:::

## 写命令配置

OPC UA 驱动**没有单独的 `command-attribute`**。向节点写值时复用位号自己的 `namespace` 和 `tag` 来定位目标节点，写值类型由位号类型决定——驱动支持写 `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` / `STRING` 六种类型。所以一个可写位号只要配好 `namespace` 和 `tag`，无需再填任何命令属性即可下发写命令。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`（健康检查即向服务端发起一次连接探活）——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把端点 `opc.tcp://192.168.1.20:4840/milo` 上的一个浮点节点接进来：

1. 选 `OPC UA Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.20`、`port=4840`、`path=/milo`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `namespace=2`、`tag=Demo.Static.Float`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning port 默认值是 18600，不是标准 4840
yml 里 `port` 的默认值是 `18600`（本地内置 Milo 示例服务端的端口）。生产环境绝大多数 OPC UA 服务端用标准端口 `4840`，接入真实设备时务必按服务端实际监听端口填 `port`，不要直接用默认值。

`path` 也要和服务端的 endpoint 路径一致：有的服务端 endpoint 是根路径（填 `/`），有的带子路径（如 `/milo`、`/OPCUA/SimulationServer`），填错会连不上。
:::

::: tip 匿名身份，证书自动生成
驱动以匿名身份（AnonymousProvider）连接服务端。驱动启动时若工作目录 `dc3/opc-ua` 下没有客户端证书，会自动生成自签名证书（`dc3-opc-ua-client.pfx`）用于连接，无需手动放置。若服务端强制用户名/密码且不允许匿名，匿名连接会被拒绝，需先在服务端放开匿名。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `namespace` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [OPC DA 驱动](./opc-da) — 经典 OPC（DCOM）版本
