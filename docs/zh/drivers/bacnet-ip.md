---
title: BACnet/IP 驱动
---

# BACnet/IP 驱动

> **`dc3-driver-bacnet-ip` 把 BACnet/IP 设备接入 IoT DC3**——以远端设备的对象属性为目标，周期性读取属性值，并支持向可写对象属性写值的命令。

BACnet（Building Automation and Control network）是楼宇自控领域的国际标准协议，广泛用于空调机组、新风、照明、电梯、冷热源、温控器等机电设备。BACnet/IP 是它跑在 UDP（标准端口 `47808 = 0xBAC0`）上的变体。本驱动基于 BACnet4J 库，作为本地 BACnet 设备（LocalDevice）加入网络，通过广播发现远端设备，再按[位号](../introduction/concepts/point)上配置的对象类型、对象实例号与属性标识去读、写远端对象的属性。

在 BACnet 里，一台物理设备由唯一的**设备实例号**标识，设备内含若干**对象**（如 `ANALOG_INPUT`、`BINARY_OUTPUT`），每个对象由"对象类型 + 对象实例号"定位，对象又有若干**属性**（最常用的是 `PRESENT_VALUE`，即当前值）。读写一个测点，本质就是定位到"远端设备 → 对象 → 属性"。

- **驱动名 / code**：`BACnet IP Driver` / `BacnetIpDriver`
- **类型**：`DRIVER_CLIENT`（本驱动主动发现并访问远端设备）

## 驱动配置（设备级 `driver-attribute`）

接入一台 BACnet/IP 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。注意这里配的是**本地驱动**接入网络的参数，不是远端设备地址——远端设备靠下面位号里的设备实例号定位：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Local Device ID | `localDeviceId` | INT | `1001` | 本地 BACnet 设备实例号 |
| Bind Address | `bindAddress` | STRING | `0.0.0.0` | 本地绑定地址 |
| Port | `port` | INT | `47808` | BACnet UDP 端口（默认 47808 = 0xBAC0）|
| Broadcast Address | `broadcastAddress` | STRING | `255.255.255.255` | 设备发现用的广播地址 |
| Timeout | `timeout` | INT | `6000` | 请求超时时间（毫秒）|

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填，用来定位"哪台远端设备的哪个对象的哪个属性"：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Remote Device ID | `remoteDeviceId` | INT | `0` | 远端 BACnet 设备实例号 |
| Object Type | `objectType` | STRING | `ANALOG_INPUT` | BACnet 对象类型（ANALOG_INPUT、ANALOG_OUTPUT、ANALOG_VALUE、BINARY_INPUT、BINARY_OUTPUT、BINARY_VALUE、MULTI_STATE_INPUT、MULTI_STATE_OUTPUT、MULTI_STATE_VALUE、DEVICE）|
| Object Instance | `objectInstance` | INT | `0` | 对象实例号 |
| Property ID | `propertyId` | STRING | `PRESENT_VALUE` | 属性标识（PRESENT_VALUE、DESCRIPTION、STATUS_FLAGS 等）|

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填（结构与位号配置一致，但默认指向可写的输出对象）：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Remote Device ID | `remoteDeviceId` | INT | `0` | 远端 BACnet 设备实例号 |
| Object Type | `objectType` | STRING | `ANALOG_OUTPUT` | 写入目标的 BACnet 对象类型 |
| Object Instance | `objectInstance` | INT | `0` | 对象实例号 |
| Property ID | `propertyId` | STRING | `PRESENT_VALUE` | 属性标识 |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——驱动按本地与远端设备的连接是否就绪判定在线，机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把网络上一台设备实例号为 `9001` 的 BACnet/IP 设备的温度对象接进来：

1. 选 `BACnet IP Driver` 创建[设备](../introduction/concepts/device)，driver 属性可全用默认（`localDeviceId=1001`、`bindAddress=0.0.0.0`、`port=47808`、`broadcastAddress=255.255.255.255`、`timeout=6000`）；只有当本机有多块网卡或广播打不到目标网段时才需调整 `bindAddress` / `broadcastAddress`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`），point 属性填 `remoteDeviceId=9001`、`objectType=ANALOG_INPUT`、`objectInstance=1`、`propertyId=PRESENT_VALUE`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到该对象的 `PRESENT_VALUE`。

## 易错点

::: warning Object Type / Property ID 必须用准确的大写枚举名
`objectType` 和 `propertyId` 是按精确的大写名匹配的（如 `ANALOG_INPUT`、`PRESENT_VALUE`）。填错或拼错不会报错——驱动会**静默回退**到 `ANALOG_INPUT` / `PRESENT_VALUE`，于是你读到的可能是另一个对象的值却毫无察觉。`objectType` 仅支持上表列出的 10 种，`propertyId` 支持 `PRESENT_VALUE`、`DESCRIPTION`、`STATUS_FLAGS`、`EVENT_STATE`、`RELIABILITY`、`UNITS`、`OUT_OF_SERVICE`。
:::

::: tip 写值的编码由对象类型决定
驱动按对象类型前缀决定怎么编码写入值：`ANALOG_*` 当浮点数写；`BINARY_*` 把 `true` / `1` / `active`（不分大小写）当"激活"、其余当"未激活"；`MULTI_STATE_*` 当整数写。所以给 `BINARY_OUTPUT` 写开关，传 `1` 或 `true` 而不是 `ON`。
:::

::: warning 远端设备要能被广播发现
驱动靠广播在网络上发现远端设备后才能读写（`remoteDeviceId` 找不到就会阻塞直至超时）。BACnet/IP 走 UDP 广播，通常无法跨三层路由——请确保驱动与目标设备在同一广播域，跨网段时需在网络上部署 BBMD，并据实调整 `broadcastAddress`。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `localDeviceId` / `objectType` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 另一种常见的工业现场协议
