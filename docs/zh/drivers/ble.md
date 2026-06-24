---
title: BLE 驱动
---

# BLE 驱动

> **`dc3-driver-ble` 把蓝牙低功耗（BLE）设备接入 IoT DC3**——连上一个 BLE 外设，周期性读取它 GATT 特征的值，并支持向特征写值的命令。

BLE（Bluetooth Low Energy，蓝牙低功耗）是可穿戴、环境传感器、信标、便携仪表上最常见的近场无线协议。BLE 设备把数据组织成 **GATT**（Generic Attribute Profile）结构：一个外设包含若干 **Service（服务）**，每个服务下挂若干 **Characteristic（特征）**，每个特征用一个 **UUID** 唯一标识，特征值就是一段字节。本驱动作为 BLE 主机（central），通过主机上的蓝牙适配器连到外设，按[位号](../introduction/concepts/point)上配置的服务/特征 UUID 读字节、写字节，再按配置的格式把字节解析成[位号值](../introduction/concepts/point-value)。

- **驱动名 / code**：`Bluetooth LE Driver` / `BleDriver`
- **类型**：`DRIVER_CLIENT`（驱动主动连外设）

## 驱动配置（设备级 `driver-attribute`）

接入一台 BLE 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Adapter Name | `adapterName` | STRING | `hci0` | 主机蓝牙适配器名 |
| Device Address | `deviceAddress` | STRING | （空）| BLE 设备 MAC 地址（如 `AA:BB:CC:DD:EE:FF`）|
| Connection Timeout | `connectionTimeout` | INT | `10000` | 连接超时（毫秒）|

::: tip 一个外设 = 一个设备
`deviceAddress` 是外设的唯一标识，一台 BLE 外设对应平台里一个[设备](../introduction/concepts/device)。同一适配器（`hci0`）可同时连多台外设，由各设备的 `deviceAddress` 区分。
:::

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)对应外设上的一个 GATT 特征，填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Service UUID | `serviceUuid` | STRING | （空）| GATT Service UUID |
| Characteristic UUID | `characteristicUuid` | STRING | （空）| GATT Characteristic UUID |
| Read Format | `readFormat` | STRING | `UTF8` | 数据格式（UTF8、HEX、INT16、UINT16、FLOAT）|
| Byte Order | `byteOrder` | STRING | `BIG` | 字节序（BIG、LITTLE）|

::: tip Read Format 决定字节怎么变成值
特征读回来的是一段原始字节，驱动按 `readFormat` 解析：`UTF8` 当字符串、`HEX` 转十六进制串、`INT16`/`UINT16`/`FLOAT` 按数值解析。`INT16`/`UINT16`/`FLOAT` 还受 `byteOrder` 影响（`BIG` 大端、`LITTLE` 小端）；`UTF8`、`HEX` 与字节序无关。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Service UUID | `serviceUuid` | STRING | （空）| GATT Service UUID |
| Characteristic UUID | `characteristicUuid` | STRING | （空）| 写入目标特征的 UUID |

::: warning 写入按 UTF-8 字节下发
驱动把命令值按 UTF-8 编码成字节直接写进特征，不做 `readFormat` 那样的格式转换。要写数值或十六进制，需在上层把它表达成目标特征接受的字符串形式。
:::

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。设备在线 = 已连接且可达（BLE 链路 `isOnline() && isConnected()`），在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 MAC `AA:BB:CC:DD:EE:FF` 的一台 BLE 温度计接进来：

1. 选 `Bluetooth LE Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `adapterName=hci0`、`deviceAddress=AA:BB:CC:DD:EE:FF`，`connectionTimeout` 用默认 `10000`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`），point 属性填 `serviceUuid`、`characteristicUuid` 为该温度特征的 UUID，`readFormat=FLOAT`、`byteOrder=LITTLE`（按外设手册定）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning 主机必须有可用的蓝牙适配器和 TinyB 原生库
本驱动用 Sputnikdev Bluetooth Manager + TinyB 传输，依赖运行主机的物理蓝牙适配器和 TinyB 本地库。没有适配器、或 `adapterName` 填错（默认是 `hci0`），设备会一直离线。容器化部署时需把宿主蓝牙能力透传进容器。
:::

::: warning UUID 必须与外设手册逐字一致
`serviceUuid`、`characteristicUuid` 要和外设实际暴露的 GATT 完全一致（含短/长格式）。UUID 写错或大小写不符，特征找不到，读写都会失败。先用 BLE 扫描工具确认外设的 service/characteristic UUID 再填。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `adapterName` / `serviceUuid` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 以太网工业总线版示例
