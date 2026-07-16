---
title: BACnet/IP 驱动
---

<script setup>
import BacnetIpDiagram from '../../.vitepress/theme/components/BacnetIpDiagram.vue'
</script>


# BACnet/IP 驱动

`dc3-driver-bacnet-ip` 把 BACnet/IP 设备接入 IoT DC3。它作为本地 BACnet
设备加入网络，通过广播发现远端设备，周期性读取对象属性值，并支持向可写对象属性写值。读完你能在[设备](../introduction/concepts/device)
上配好本地组网参数、在[位号](../introduction/concepts/point)上用"对象类型 + 实例号 + 属性"定位远端数据点，并定位常见的"
发现不到设备 / 读到错对象 / 写不下去"问题。

> 你在这里：网络层"楼宇自控侧"的一个落地驱动。BACnet
> 的对象-属性寻址模型与它在四层架构中的位置，见[工业总线与协议](../foundations/fieldbus)。

## 协议背景

BACnet（Building Automation and Control network）是楼宇自控领域的国际标准协议（ASHRAE 135 / ISO 16484-5），自 1995
年起广泛用于空调机组、新风、照明、电梯、冷热源、温控器等机电设备。它的设计目标是让不同厂商的楼宇设备能互操作，因此把"设备能力"
抽象成一套统一的对象模型，而不绑定具体硬件。

**BACnet/IP** 是 BACnet 跑在以太网上的变体：把 BACnet 应用层报文封装进 UDP，默认监听 **47808** 端口（即 `0xBAC0`）。相比早期跑在
MS/TP 串口或以太网 ISO 8802-3 上的形态，BACnet/IP 直接复用现有 IP 网络，靠 UDP
广播在子网内发现设备，这也决定了它的一个硬约束——广播默认不跨三层路由（见下方故障排查）。

在[物联网四层架构](../foundations/fieldbus)里，BACnet/IP 属于**网络层**
的楼宇自控侧：它定义机电设备如何在网络上被寻址与读写，把感知层采集的温度、状态、计量等物理量搬运到平台。它的通信模型是*
*主从 / 请求-响应**——本驱动作为发起方（client）主动发现并访问远端设备，按 cron 周期轮询读取，远端设备不主动上报。

BACnet 的寻址是一套三层结构，理解它就理解了位号怎么配：

<BacnetIpDiagram lang="zh" />

一台物理设备由唯一的**设备实例号**标识；设备内含若干**对象**（如 `ANALOG_INPUT`、`BINARY_OUTPUT`），每个对象由"对象类型 +
对象实例号"定位；对象又有若干**属性**，最常用的是 `PRESENT_VALUE`（当前值）。读写一个测点，本质就是定位"远端设备 → 对象 →
属性"这条路径。

- **驱动名 / code**：`BACnet IP Driver` / `BacnetIpDriver`
- **类型**：`DRIVER_CLIENT`（本驱动主动发现并访问远端设备）

## 属性配置

接入一台 BACnet/IP 设备，需要在三个层面填[属性](../introduction/concepts/attribute-config)：设备级的本地组网参数（
`driver-attribute`）、每个采集位号的寻址参数（`point-attribute`）、每个可写位号的写命令参数（`command-attribute`
）。下面各属性、类型、默认值均取自驱动的 `application.yml`（`dc3-driver-bacnet-ip` 模块）。

### 驱动属性（设备级 `driver-attribute`）

驱动属性回答"本驱动用什么身份、绑哪块网卡、怎么发广播"——注意这里配的是**本地驱动**接入网络的参数，**不是**
远端设备地址；远端设备靠下面位号里的 `remoteDeviceId` 定位。在[设备](../introduction/concepts/device)上为每台设备填一组：

| 属性                | code               | 类型     | 默认值               | 说明                               |
|-------------------|--------------------|--------|-------------------|----------------------------------|
| Local Device ID   | `localDeviceId`    | INT    | `1001`            | 本地 BACnet 设备实例号                  |
| Bind Address      | `bindAddress`      | STRING | `0.0.0.0`         | 本地绑定地址                           |
| Port              | `port`             | INT    | `47808`           | BACnet UDP 端口（默认 47808 = 0xBAC0） |
| Broadcast Address | `broadcastAddress` | STRING | `255.255.255.255` | 设备发现用的广播地址                       |
| Timeout           | `timeout`          | INT    | `6000`            | 请求超时时间（毫秒）                       |

`localDeviceId` 是本驱动作为 `LocalDevice` 加入网络时占用的实例号，需与网络上现有 BACnet 设备的实例号都不冲突。
`bindAddress` 默认 `0.0.0.0` 让系统自选网卡；本机有多块网卡、或广播打不到目标网段时才需指定具体网卡 IP。`broadcastAddress`
是发现远端设备时发广播用的地址，默认 `255.255.255.255` 是受限广播，跨网段接入时要据实改成目标子网的定向广播地址。驱动按设备
ID 缓存连接（一台设备一个 `LocalDevice`），底层传输超时取 `timeout`。配置校验（`validate`）会要求 `localDeviceId`、
`bindAddress`、`port` 三项非空。

### 位号属性（`point-attribute`）

位号属性回答"读哪台远端设备的哪个对象的哪个属性"。每个采集[位号](../introduction/concepts/point)填一组：

| 属性               | code             | 类型     | 默认值             | 说明                 |
|------------------|------------------|--------|-----------------|--------------------|
| Remote Device ID | `remoteDeviceId` | INT    | `0`             | 远端 BACnet 设备实例号    |
| Object Type      | `objectType`     | STRING | `ANALOG_INPUT`  | BACnet 对象类型（见下方枚举） |
| Object Instance  | `objectInstance` | INT    | `0`             | 对象实例号              |
| Property ID      | `propertyId`     | STRING | `PRESENT_VALUE` | 属性标识               |

`remoteDeviceId` 是要读的那台远端设备的实例号，驱动先按它广播发现设备，再按 `objectType` + `objectInstance` 定位对象、按
`propertyId` 取属性。`objectType` 与 `propertyId` 都是按**精确的大写枚举名**
匹配的，取值来源于代码里固定的映射表（详见下方易错点与"在 IoT DC3 中如何落地"）。配置校验（`validatePoint`）会要求这四项全部非空。

### 写命令属性（`command-attribute`）

可写位号还要在写命令上填一组，结构与位号属性一致，但默认指向可写的输出对象：

| 属性               | code             | 类型     | 默认值             | 说明                |
|------------------|------------------|--------|-----------------|-------------------|
| Remote Device ID | `remoteDeviceId` | INT    | `0`             | 远端 BACnet 设备实例号   |
| Object Type      | `objectType`     | STRING | `ANALOG_OUTPUT` | 写入目标的 BACnet 对象类型 |
| Object Instance  | `objectInstance` | INT    | `0`             | 对象实例号             |
| Property ID      | `propertyId`     | STRING | `PRESENT_VALUE` | 属性标识              |

写值会按目标对象类型自动编码（见下方"写值编码"提示）。

::: warning Object Type / Property ID 必须用精确的大写枚举名，否则静默回退
`objectType` 和 `propertyId` 按精确的大写名匹配。填错或拼错**不会报错**——驱动的 `resolveObjectType()` /
`resolvePropertyIdentifier()` 会**静默回退**到 `ANALOG_INPUT` / `PRESENT_VALUE`，于是你可能读到的是另一个对象的值却毫无察觉。

- `objectType` 代码支持 **10** 种：`ANALOG_INPUT`、`ANALOG_OUTPUT`、`ANALOG_VALUE`、`BINARY_INPUT`、`BINARY_OUTPUT`、
  `BINARY_VALUE`、`MULTI_STATE_INPUT`、`MULTI_STATE_OUTPUT`、`MULTI_STATE_VALUE`、`DEVICE`（yml 的 remark 只列了前 9 种，
  `DEVICE` 也可用——以代码为准）。
- `propertyId` 支持 7 种：`PRESENT_VALUE`、`DESCRIPTION`、`STATUS_FLAGS`、`EVENT_STATE`、`RELIABILITY`、`UNITS`、
  `OUT_OF_SERVICE`。
  :::

## 故障排查

BACnet/IP 接入失败大多集中在广播发现、对象寻址、写值编码三类。按下面顺序排查：

1. **设备一直 offline / 创建连接失败**。驱动以 `localDeviceId` 加入网络时若该实例号与网络上已有设备冲突、或绑定端口/网卡失败，
   `LocalDevice.initialize()` 会抛 `ConnectorException`，该设备始终 offline。先确认 `localDeviceId` 在网络内唯一、`47808`
   端口未被同机其他 BACnet 程序占用、`bindAddress` 指向真实可用网卡。

2. **连上了但读取一直超时（卡到 timeout）**。驱动靠 `getRemoteDeviceBlocking(remoteDeviceId)` 阻塞等待广播发现远端设备——
   `remoteDeviceId` 在网络上找不到时会**一直阻塞到超时**再抛 `ReadPointException`。先核对 `remoteDeviceId`
   是否就是目标设备的实例号；再确认驱动与目标设备处在同一广播域（见下方易错点）。

3. **读到的值不对 / 像是别的点**。多半是 `objectType` 或 `propertyId` 拼错触发了静默回退（回退到 `ANALOG_INPUT` /
   `PRESENT_VALUE`）。逐字符核对大写枚举名，确认在上方支持列表内。

4. **写命令失败**。先确认目标对象类型可写（`*_INPUT` 类对象通常物理只读，写不进去），再确认传入值与编码规则匹配（见下方"
   写值编码"）。写失败会抛 `WritePointException`。

5. **跨网段 / 多网卡环境读不到设备**。BACnet/IP 走 UDP 广播，默认不跨三层路由。详见下方易错点容器。

6. **设备在线状态抖动**。健康检查默认每 15 秒一次（cron `0/15 * * * * ?`）、租约超时 45 秒；驱动按
   `LocalDevice.isInitialized()` 判定在线。若频繁在 online/offline
   间跳变，多半是网络丢包或本地设备初始化不稳定——在线状态机制见[设备](../introduction/concepts/device)。

::: tip 写值编码由对象类型决定
驱动 `createEncodable()` 按对象类型前缀决定怎么编码写入值：`ANALOG_*` 当浮点数（`Real`）写；`BINARY_*` 把 `true` / `1` /
`active`（不分大小写）当"激活"、其余当"未激活"；`MULTI_STATE_*` 与 `DEVICE` 当整数（`UnsignedInteger`）写；非数值且落到模拟量分支时退化为字符串（
`CharacterString`）。所以给 `BINARY_OUTPUT` 写开关，传 `1` 或 `true` 而不是 `ON`。
:::

::: warning 远端设备必须能被广播发现
驱动靠广播在网络上发现远端设备后才能读写。BACnet/IP 走 UDP 广播，通常无法跨三层路由——请确保驱动与目标设备在同一广播域；跨网段时需在网络上部署
**BBMD**（BACnet/IP Broadcast Management Device），并把 `broadcastAddress` 据实改成目标子网的定向广播地址。找不到
`remoteDeviceId` 会阻塞直至超时（见上方排查第 2 条）。
:::

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`BacnetIpDriver`（类型 `DRIVER_CLIENT`，主动发现并访问远端设备）。这是稳定的路由标识，不要随意改。
- **读能力**：✓ 已实现。按 `remoteDeviceId` 广播发现设备，按 `objectType` + `objectInstance` + `propertyId`
  读对象属性，结果以字符串形态回传。
- **写能力**：✓ 已实现。按对象类型自动编码写值（见上方"写值编码"提示）。
- **订阅/上报**：— 不支持。BACnet 是主从轮询模型，本驱动只主动读写、不被动接收推送（未实现 COV
  订阅）。这与[驱动能力矩阵](./matrix)中 BACnet/IP 的「✓ / ✓ / —」一致。
- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮），在驱动 `application.yml` 的 `schedule.read` 配置。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`，按 `LocalDevice.isInitialized()` 判定。

::: info 实现状态：可用
本驱动是**完整实现**（非骨架），底层基于 BACnet4J。`read()` / `write()` 走真实的 BACnet 读写请求，`health()` 按本地设备初始化状态判在线，
`validate()` / `validatePoint()` 做必填校验，并按设备 ID 缓存 `LocalDevice` 连接。需注意两处与直觉不同的行为：①
`objectType` / `propertyId` 拼错会**静默回退**而非报错；② 找不到 `remoteDeviceId` 会**阻塞至超时**——均见上文。
:::

::: info schedule.custom 已开启但为空实现
驱动 `application.yml` 标注了 `schedule.custom`（cron `0/5 * * * * ?`、`enable: true`），但 `schedule()`
方法是空实现（无自定义周期逻辑）。也就是说该自定义调度当前不产生任何采集行为，实际采集只由 `schedule.read` 驱动——以代码为准。
:::

### 最小接入示例

把网络上一台设备实例号为 `9001` 的 BACnet/IP 设备的温度对象接进来：

1. 选 `BACnet IP Driver` 创建[设备](../introduction/concepts/device)，driver 属性可全用默认（`localDeviceId=1001`、
   `bindAddress=0.0.0.0`、`port=47808`、`broadcastAddress=255.255.255.255`、`timeout=6000`）；只有当本机有多块网卡、或广播打不到目标网段时才需调整
   `bindAddress` / `broadcastAddress`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`
   ），point 属性填 `remoteDeviceId=9001`、`objectType=ANALOG_INPUT`、`objectInstance=1`、`propertyId=PRESENT_VALUE`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到该对象的 `PRESENT_VALUE`。
4. 若该位号需可写，给它配写[命令](../introduction/concepts/command)，把 `objectType` 显式设为可写的输出对象（如
   `ANALOG_OUTPUT`），写值按对象类型规则传（模拟量传数字、开关量传 `1`/`true`）。

::: tip 一个驱动实例可接多台设备
同一个 BACnet/IP 驱动进程可服务多台设备，每台按设备 ID 各持一个缓存的 `LocalDevice`，由位号里的 `remoteDeviceId` 区分目标远端设备。
:::

## 延伸阅读

- [驱动总览](./index) — 全部驱动入口与分类
- [驱动能力矩阵](./matrix) — 读/写/订阅能力一览，含 BACnet/IP 行
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — BACnet 的对象-属性寻址模型与楼宇自控侧的定位
- [SNMP 驱动](./snmp) — 另一种主动读写的网络管理类协议
