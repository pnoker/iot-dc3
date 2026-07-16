---
title: Zigbee 驱动
---

# Zigbee 驱动

> **`dc3-driver-zigbee` 把 Zigbee 设备接入 IoT DC3**——通过串口协调器（coordinator）连入 Zigbee 网络，按 ZCL
> 属性周期性读取节点数据，并支持向 ZCL 属性写值的命令。

读完这页，你能理解 Zigbee 在物联网网络层中的位置、知道接一台 Zigbee 节点要在驱动侧和位号侧填哪些属性，并清楚当前实现到了哪一步、哪些能力还不能依赖。

## 协议背景

Zigbee 是一种**低功耗、低速率的短距无线 Mesh 协议**，建立在 IEEE 802.15.4 物理/链路层之上，工作在 2.4 GHz
免授权频段。它的典型用途是智能家居与楼宇自控里大量电池供电的传感节点——温湿度、门磁、人体感应、开关、灯具、智能插座等。这类设备数据量小、要求长续航，不适合直接跑
IP 协议栈，于是组成自己的 Zigbee 网络：节点之间可互相中继（mesh），由一个**协调器（coordinator）**统一组网与进出。

在[物联网四层架构](../foundations/iot-protocols)里，Zigbee 属于**网络层**的无线接入技术——它解决"
低速设备的信号怎么省电地传出去"，而不直接讲 IP。Zigbee 网络不直接连公网，而是通过协调器/网关汇聚后再上联，这一点和 BLE 类似、和
MQTT/CoAP 这类应用层消息协议不同。在 IoT DC3 中，协调器以一枚 **USB 串口 dongle** 的形式插在运行驱动的主机上，驱动作为
Zigbee 应用层 client，把每个[位号](../introduction/concepts/point)映射到 Zigbee 网络中的一个 ZCL 属性来采数、写值。

Zigbee 的寻址分多级。每个 Zigbee 设备用 **IEEE 地址**（64 位长地址，出厂固定）唯一标识；设备内部再按 **endpoint（端点）→
cluster（簇）→ attribute（ZCL 属性）** 三级定位某个具体的数据点。理解这套寻址，是配置位号属性的前提。

::: tip 三级寻址：endpoint / cluster / attribute
一个 Zigbee 节点可有多个端点（多功能设备），每个端点下挂若干 ZCL 簇（如温度测量簇 `1026`、相对湿度簇 `1029`），每个簇里再有若干属性。
`cluster` + `attribute` 决定读到的是哪一类物理量——位号的数据类型（[Point](../introduction/concepts/point) 的
`pointTypeFlag`）要和该 ZCL 属性的实际类型对得上。
:::

## 属性配置

接入一台 Zigbee 设备分两层填写：**驱动属性（`driver-attribute`）** 配的是协调器侧的接入参数，一个协调器服务整个 Zigbee 网络；
**位号属性（`point-attribute`）** 把每个采集位号定位到网络中具体的一个 ZCL 属性。下面两张表的字段都来自驱动的
`application.yml`（`dc3.driver.driver-attribute` / `point-attribute`），默认值即 yml 中的 `default-value`。

### 驱动属性（设备级 `driver-attribute`）

这些属性配在[设备](../introduction/concepts/device)上，描述本机上那枚协调器 dongle 怎么连、组到哪张 Zigbee 网络里。
`serialPort` 与 `baudRate` 决定串口连接，`dongleType` 决定用哪种协调器适配器，`panId` 与 `channel` 决定加入哪张网络的哪个信道（填
`0` 表示自动）。

| 属性          | code         | 类型     | 默认值            | 说明                                      |
|-------------|--------------|--------|----------------|-----------------------------------------|
| Serial Port | `serialPort` | STRING | `/dev/ttyUSB0` | Zigbee 协调器串口                            |
| Baud Rate   | `baudRate`   | INT    | `115200`       | 串口波特率                                   |
| Dongle Type | `dongleType` | STRING | `TELEGESIS`    | 协调器 dongle 类型（TELEGESIS, EMBER, CONBEE） |
| PAN ID      | `panId`      | INT    | `0`            | PAN ID（0=自动）                            |
| Channel     | `channel`    | INT    | `0`            | 信道（0=自动，11-26）                          |

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)用 IEEE 地址 + 端点 + 簇 + 属性，唯一定位到 Zigbee 网络中的一个 ZCL 属性。
`nodeIeeeAddress` 选哪台节点，`endpointId` / `clusterId` / `attributeId` 按上文三级寻址逐级缩小到那个具体属性。

| 属性                | code              | 类型     | 默认值 | 说明                                      |
|-------------------|-------------------|--------|-----|-----------------------------------------|
| Node IEEE Address | `nodeIeeeAddress` | STRING | （空） | Zigbee 节点 IEEE 地址（如 `00158D0001234567`） |
| Endpoint ID       | `endpointId`      | INT    | `1` | 端点 ID                                   |
| Cluster ID        | `clusterId`       | INT    | `0` | 簇 ID（如 `1026`=温度测量）                     |
| Attribute ID      | `attributeId`     | INT    | `0` | 属性 ID（如 `0`=测量值）                        |

### 写命令属性（`command-attribute`）

可写位号在写命令上填同样的四级定位，但指向要写入的目标属性。字段与位号属性同名同义，只是用于 `write` 路径。

| 属性                | code              | 类型     | 默认值 | 说明                |
|-------------------|-------------------|--------|-----|-------------------|
| Node IEEE Address | `nodeIeeeAddress` | STRING | （空） | Zigbee 节点 IEEE 地址 |
| Endpoint ID       | `endpointId`      | INT    | `1` | 端点 ID             |
| Cluster ID        | `clusterId`       | INT    | `0` | 写入目标的簇 ID         |
| Attribute ID      | `attributeId`     | INT    | `0` | 写入目标的属性 ID        |

### 一个最小接入示例

把一个 IEEE 地址为 `00158D0001234567` 的温度传感节点接进来：

1. 选 `Zigbee Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `serialPort=/dev/ttyUSB0`、
   `baudRate=115200`、`dongleType=TELEGESIS`、`panId=0`、`channel=0`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `nodeIeeeAddress=00158D0001234567`、`endpointId=1`、`clusterId=1026`
   （温度测量簇）、`attributeId=0`（测量值）。
3. 确保节点已加入协调器的 Zigbee 网络，启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 故障排查

接入 Zigbee 设备时，问题大多出在串口、网络入网、地址格式或当前实现的边界上。下面按"先连上、再找到、再读对"的顺序排查。

::: warning 协调器找不到 / 串口被占
默认串口是 `/dev/ttyUSB0`、波特率 `115200`。先确认 dongle 已插好、宿主能看到串口设备（如 `ls /dev/ttyUSB*`
），且该串口没有被其他进程占用。容器化部署时，需把宿主串口设备透传进容器（如 `--device=/dev/ttyUSB0`），否则驱动启动后会一直连不上协调器。
**注意**：当前实现的串口与波特率是硬编码的，见下文实现状态。
:::

::: warning 节点报 "node not found"
读写时报找不到节点，通常是 `nodeIeeeAddress` 写错或节点未入网。IEEE 地址是连续 16 位十六进制字符（如 `00158D0001234567`），*
*不要加冒号或 `0x` 前缀**。地址出厂固定，可在协调器/网关的设备列表里查到。另外，节点必须先加入本协调器的 Zigbee
网络（permit-join 入网）后才能被寻址。
:::

::: warning 端点 / 簇 / 属性找不到
报 "endpoint/cluster/attribute not found" 说明三级寻址某一级填错。多功能设备的端点不一定是 `1`；簇 ID 要对应实际物理量（温度测量
`1026`、相对湿度 `1029`）；属性 ID 要对应该簇下的具体属性（测量值常为 `0`）。先用协调器工具查清目标节点暴露的端点与簇，再回填位号属性。

读路径取的是该 ZCL 属性的**最近缓存值**（`attribute.getLastValue()`）。若节点尚未上报过该属性、或属性绑定/上报未配置，可能读到
`0`（默认占位）。
:::

::: warning 数据类型对不上
位号的 `pointTypeFlag` 要与 ZCL 属性的实际类型一致。温度测量值是有符号整数（单位 0.01℃），按 `FLOAT`
/数值解析；若把字符串类簇当数值读，或反之，会得到无意义的值。配置位号前先核对该 ZCL 属性的数据类型。
:::

::: warning 驱动 / 设备显示离线
驱动级健康检查依赖 `networkManager`
是否已初始化：协调器没连上时驱动级为离线。设备级在线态见[设备](../introduction/concepts/device)的租约机制（健康检查 cron
`0/15 * * * * ?`、租约超时 `45 秒`）。注意当前实现中设备记录有效时设备级健康检查固定返回在线（未按 IEEE
地址做可达性校验），不能据此判断单个节点是否真实可达，见下文实现状态。
:::

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`ZigbeeDriver`（驱动名 `Zigbee Driver`，类型 `DRIVER_CLIENT`——主动连协调器、轮询节点）。这是稳定的路由标识，不要随意改。
- **采集周期**：默认 cron `0/30 * * * * ?`，每 30 秒读一轮 ZCL 属性。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`
  ，在线状态机制见[设备](../introduction/concepts/device)。
- **读 / 写能力**：读路径取协调器侧 ZCL 属性的最近缓存值（`attribute.getLastValue()`），而非每次都同步轮询设备空口；这与请求-响应式驱动（如
  `ble`、`coap`）不同。
- **订阅能力（尚未实现）**：`initial()` 目前**只注册了协调器网络状态监听器**（`addNetworkStateListener`，仅记录网络 UP/DOWN
  日志），**并未监听节点入网（node-join/announce）或 ZCL 属性上报**
  ，也未配置属性绑定/上报（binding/reporting）。因此读路径取到的缓存值依赖节点自行上报或外部工具配置上报，驱动本身不会捕获入网与上报事件——[驱动能力矩阵](./matrix)
  中 Zigbee 的"订阅"一栏据此标注为未实现。

::: warning 当前为骨架实现（Work in Progress）
该驱动目前是**骨架版本**，协议层 I/O 尚未完整实现，请按"接入模板"而非生产可用驱动看待。源码方法体中有多处 `TODO` 标记，关键限制如下：

- **串口与波特率被硬编码**：`initial()` 写死了 `/dev/ttyUSB0` 与 `115200`，**不读取** `serialPort` / `baudRate`
  驱动属性。若协调器不在 `/dev/ttyUSB0`，需先补齐配置读取逻辑，否则填了属性也不生效。
- **只打包了 Telegesis 适配器**：代码只引入并使用 `ZigBeeDongleTelegesis`，`dongleType` 填 `EMBER` / `CONBEE` 暂不会切换适配器。
- **设备级健康检查恒在线**：`health(driverConfig, device)` 在设备记录有效时固定返回在线（仅当 device 或其 id 为空才返回离线），未真正按
  IEEE 地址校验节点可达性。
  :::

::: warning 写命令当前不真正下发
写路径（`writeAttribute`）会校验节点 / 端点 / 簇 / 属性是否存在，但只记一条日志，**不会真正把值写到 ZCL 属性**
。在写能力补全前，配置了写命令的位号会"看似成功"但设备状态不变——不要依赖它做实际控制。
:::

## 延伸阅读

- [驱动总览](./index) — 全部驱动分组与选型入口
- [驱动能力矩阵](./matrix) — Zigbee 的读 / 写 / 订阅能力与同类驱动对比
- [设备接入](../operation/device-onboarding) — 一次完整的设备接入流程
- [物联网网络层](../foundations/iot-protocols) — Zigbee 在无线接入与网络融合中的定位
- [BLE 驱动](./ble) — 另一种低功耗短距无线设备接入
