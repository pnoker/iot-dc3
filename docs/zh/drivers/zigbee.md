---
title: Zigbee 驱动
---

# Zigbee 驱动

> **`dc3-driver-zigbee` 把 Zigbee 设备接入 IoT DC3**——通过串口协调器（coordinator）连入 Zigbee 网络，按 ZCL 属性周期性读取节点数据，并支持向 ZCL 属性写值的命令。

Zigbee 是低功耗无线 Mesh 协议，广泛用于智能家居与楼宇自控的传感节点（温湿度、门磁、开关、灯具等）。设备不直接连 IP 网络，而是组成自己的 Zigbee 网络，由一个**协调器 dongle**（通过 USB 串口插在运行驱动的主机上）统一进出。每个 Zigbee 设备用 **IEEE 地址**（64 位长地址）唯一标识；设备内部按 **endpoint（端点）→ cluster（簇）→ attribute（ZCL 属性）** 三级寻址某个具体的数据点。本驱动作为 Zigbee 应用层 client，把每个 [位号](../introduction/concepts/point) 映射到一个 ZCL 属性来采数、写值。

::: warning 当前为骨架实现（Work in Progress）
该驱动目前是**骨架版本**，协议层 I/O 尚未完整实现，请按"接入模板"而非生产可用驱动看待。具体见本页末尾[易错点](#易错点)，关键限制：`initial()` 暂时**硬编码**串口 `/dev/ttyUSB0` 与波特率 `115200`（未读取下方驱动属性）、设备级健康检查恒返回在线、写命令只记日志而**不真正下发**。
:::

- **驱动名 / code**：`Zigbee Driver` / `ZigbeeDriver`
- **类型**：`DRIVER_CLIENT`（主动连协调器、轮询节点）

## 驱动配置（设备级 `driver-attribute`）

接入一台 Zigbee 设备时，在 [设备](../introduction/concepts/device) 上填这些 [属性](../introduction/concepts/attribute-config)。这里配的是**协调器侧**的接入参数（同一协调器服务整个 Zigbee 网络）：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Serial Port | `serialPort` | STRING | `/dev/ttyUSB0` | Zigbee 协调器串口 |
| Baud Rate | `baudRate` | INT | `115200` | 串口波特率 |
| Dongle Type | `dongleType` | STRING | `TELEGESIS` | 协调器 dongle 类型（TELEGESIS, EMBER, CONBEE）|
| PAN ID | `panId` | INT | `0` | PAN ID（0=自动）|
| Channel | `channel` | INT | `0` | 信道（0=自动，11-26）|

## 位号配置（`point-attribute`）

每个采集 [位号](../introduction/concepts/point) 用 IEEE 地址 + 端点 + 簇 + 属性定位到 Zigbee 网络中的一个 ZCL 属性：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Node IEEE Address | `nodeIeeeAddress` | STRING | （空）| Zigbee 节点 IEEE 地址（如 `00158D0001234567`）|
| Endpoint ID | `endpointId` | INT | `1` | 端点 ID |
| Cluster ID | `clusterId` | INT | `0` | 簇 ID（如 `1026`=温度测量）|
| Attribute ID | `attributeId` | INT | `0` | 属性 ID（如 `0`=测量值）|

::: tip 三级寻址：endpoint / cluster / attribute
一个 Zigbee 节点可有多个端点（多功能设备），每个端点下挂若干 ZCL 簇（如温度测量簇 `1026`、湿度测量簇 `1029`），每个簇里再有若干属性。`cluster` + `attribute` 决定读到的是哪一类物理量——位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要和该 ZCL 属性的实际类型对得上。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填（与位号同样的四级定位，但指向要写入的目标属性）：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Node IEEE Address | `nodeIeeeAddress` | STRING | （空）| Zigbee 节点 IEEE 地址 |
| Endpoint ID | `endpointId` | INT | `1` | 端点 ID |
| Cluster ID | `clusterId` | INT | `0` | 写入目标的簇 ID |
| Attribute ID | `attributeId` | INT | `0` | 写入目标的属性 ID |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮 ZCL 属性）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见 [设备](../introduction/concepts/device)。注意当前骨架实现中设备级健康检查恒返回在线（见[易错点](#易错点)）。

## 最小接入示例

把一个 IEEE 地址为 `00158D0001234567` 的温度传感节点接进来：

1. 选 `Zigbee Driver` 创建 [设备](../introduction/concepts/device)，driver 属性填 `serialPort=/dev/ttyUSB0`、`baudRate=115200`、`dongleType=TELEGESIS`、`panId=0`、`channel=0`。
2. 给设备绑定的 [物模型](../introduction/concepts/profile) 加一个温度 [位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `nodeIeeeAddress=00158D0001234567`、`endpointId=1`、`clusterId=1026`（温度测量簇）、`attributeId=0`（测量值）。
3. 确保节点已加入协调器的 Zigbee 网络，启动驱动，30 秒内就能在 [位号值](../introduction/concepts/point-value) 里看到采集值。

## 易错点

::: warning 写命令当前不真正下发
当前骨架实现的写路径（`writeAttribute`）会校验节点/端点/簇/属性是否存在，但只记一条日志，**不会真正把值写到 ZCL 属性**。在写能力补全前，配置了写命令的位号会"看似成功"但设备状态不变——不要依赖它做实际控制。
:::

::: warning 串口与波特率当前被硬编码
当前 `initial()` 写死了 `/dev/ttyUSB0` 与 `115200`，**不读取** `serialPort` / `baudRate` 驱动属性。如果协调器不在 `/dev/ttyUSB0`，需先补齐配置读取逻辑，否则填了属性也不生效。同时 dongle 适配器目前只打包了 Telegesis，`dongleType` 填其他值暂不会切换适配器。
:::

::: tip IEEE 地址是 16 位十六进制，不要带分隔符
`nodeIeeeAddress` 是 64 位 IEEE 长地址，写成连续 16 位十六进制字符（如 `00158D0001234567`），不要加冒号或 `0x` 前缀。它由设备出厂决定，可在协调器/网关的设备列表里查到——填错地址会报"node not found"。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `serialPort` / `clusterId` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [BLE 驱动](./ble) — 另一种低功耗短距无线设备接入
