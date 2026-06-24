---
title: CAN 总线驱动
---

# CAN 总线驱动

> **`dc3-driver-can` 把 CAN 总线设备接入 IoT DC3**——监听 SocketCAN 接口上的 CAN 帧，按[位号](../introduction/concepts/point)配置解析帧载荷为采集值，并支持向总线发送命令帧写值。

CAN（Controller Area Network）是汽车与工业自动化里广泛使用的现场总线：节点不靠地址点对点通信，而是把带 **CAN ID**（标识符）的帧广播到总线上，接收方按 CAN ID 过滤自己关心的帧。一帧最多携带 8 字节载荷。常见于车载 ECU、电池管理系统（BMS）、伺服驱动器、传感器节点等场景。

本驱动作为总线上的一个节点接入：它在 Linux 的 SocketCAN 接口（如 `can0`）上抓取匹配 CAN ID 的帧来采集，必要时也能先发一帧"请求"再读"应答"，并通过命令帧写值。

- **驱动名 / code**：`CAN Bus Driver` / `CanDriver`
- **类型**：`DRIVER_CLIENT`（主动在总线上收发帧）

::: warning 当前为骨架实现
该驱动是一个起步模板：底层协议 I/O 尚未完全实现，`read()`/`write()` 通过调用 Linux `can-utils` 工具（`candump`/`cansend`）完成，`health()` 用 `ip link show` 检查接口。生产前需补齐原生 SocketCAN 集成。
:::

## 驱动配置（设备级 `driver-attribute`）

接入一台 CAN 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Interface | `interfaceName` | STRING | `can0` | SocketCAN 接口名 |
| Bitrate | `bitrate` | INT | `500000` | CAN 总线波特率（bps）|
| Frame Format | `frameFormat` | STRING | `STANDARD` | STANDARD(11bit) or EXTENDED(29bit) |

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| CAN ID | `canId` | STRING | （空）| 要匹配的 CAN 标识符（十六进制）|
| Data Offset | `dataOffset` | INT | `0` | 帧载荷内的字节偏移 |
| Data Length | `dataLength` | INT | `1` | 读取的字节数 |
| Data Format | `dataFormat` | STRING | `INT` | INT/UINT/HEX |
| Byte Order | `byteOrder` | STRING | `LITTLE` | 字节序（如 LITTLE）|
| Request CAN ID | `requestCanId` | STRING | （空）| 可选请求帧的 CAN ID |
| Request Data | `requestData` | STRING | （空）| 可选请求帧的载荷 |

::: tip 主动请求型读取
不少 CAN 设备需要先收到一帧"请求"才会应答数据。若同时填了 `requestCanId` 与 `requestData`，驱动会在采集前先发一帧请求，再监听 `canId` 匹配的应答帧；两者留空则纯被动监听总线上周期广播的帧。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| CAN ID | `canId` | STRING | （空）| 写入目标的 CAN 标识符 |
| Data | `data` | STRING | `${value}` | 帧数据，用命令参数渲染（支持 `${value}`）|

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒抓一轮帧）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——驱动用 `ip link show <interface>` 判断接口是否存在，在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 `can0` 上一个周期广播温度、CAN ID 为 `123` 的节点接进来：

1. 选 `CAN Bus Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `interfaceName=can0`、`bitrate=500000`、`frameFormat=STANDARD`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`），point 属性填 `canId=123`、`dataOffset=0`、`dataLength=2`、`dataFormat=INT`、`byteOrder=LITTLE`，`requestCanId`/`requestData` 留空（被动监听）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning 驱动需运行在装了 can-utils 的 Linux 上
底层读写依赖 `candump`/`cansend`，健康检查依赖 `ip link show`，且要求一个可用的 SocketCAN 接口。驱动进程必须跑在 Linux 主机且能访问该接口；macOS/Windows 或缺少 `can-utils` 时采集会失败、设备一直离线。
:::

::: tip canId 用十六进制、不带前缀
`canId`/`requestCanId` 按 `can-utils` 的写法填，例如标准帧 `123`、扩展帧的 29 位 ID 也按其十六进制原文填；不要加 `0x` 前缀。`frameFormat` 选 STANDARD/EXTENDED 要与设备实际帧格式一致。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `interfaceName` / `canId` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 另一种工业现场总线接入
