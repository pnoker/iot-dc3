---
title: 驱动 Driver
---

<script setup>
import DriverRelationDiagram from '../../../.vitepress/theme/components/DriverRelationDiagram.vue'
import DriverLifecycleDiagram from '../../../.vitepress/theme/components/DriverLifecycleDiagram.vue'
</script>

# 驱动 Driver

> **驱动是一个独立运行的协议适配服务实例（`dc3-driver-*`）**——它把某种工业协议（Modbus、OPC UA、MQTT……）翻译成 DC3
> 内部统一的[位号](./point)读写与[位号值](./point-value)上报。一类协议对应一个驱动模块，启动时驱动把自己和它能接受的配置项注册到管理中心。

驱动回答的是"DC3 怎么和这台[设备](./device)通上话"。设备只描述"接入了什么"，真正握着协议会话、按周期采集、把寄存器值翻译成位号值的，是驱动这个
**服务进程**。换句话说：设备是一行元数据，驱动是一个在跑的程序。

容易混淆的是"驱动"和"设备"：一个 Modbus TCP 驱动实例（`dc3-driver-modbus-tcp`）可以同时连接成百上千台 Modbus
设备；每台设备通过[连接配置](./attribute-config)告诉驱动"我的 IP、端口、从站地址是多少"。**驱动是一对多的协议网关，设备是挂在它下面的接入点。
**

## 它是什么 / 为什么需要

工业现场协议五花八门，DC3 核心不可能内置所有协议栈。于是 DC3 把"协议怎么说"
这件事下沉到独立的驱动服务里，核心只和驱动约定一套统一的位号读写契约。新增一种协议 = 新写一个 `dc3-driver-*` 服务，核心和
Web 不用改。

每个驱动启动时做一件关键的事：**自注册**。它带着自己的身份（`DriverBO`）和"我能接受哪些配置项"（一组 `DriverAttribute`
）向管理中心登记。管理中心据此知道：这个驱动叫什么、跑在哪、属于哪个[租户](./tenant)、给它配设备时该填哪些字段。

## 关键字段

驱动 `DriverBO`（驱动服务在管理中心登记的身份元数据）：

| 字段                      | 类型               | 含义                                       |
|-------------------------|------------------|------------------------------------------|
| `driverName`            | String           | 驱动展示名称（如 `Modbus Tcp Driver`）            |
| `driverCode`            | String           | 驱动编码，配置中定义的唯一标识                          |
| `serviceName`           | String           | 驱动服务名，用于注册与路由（如 `dc3-driver-modbus-tcp`） |
| `serviceHost`           | String           | 驱动服务主机地址                                 |
| `driverTypeFlag`        | DriverTypeEnum   | 驱动运行类型，见下                                |
| `driverExt`             | DriverExt        | 扩展元数据（JSON）                              |
| `enableFlag`            | EnableFlagEnum   | 启停标记                                     |
| `tenantId`              | Long             | 归属[租户](./tenant)                         |
| `signature` / `version` | String / Integer | 数据签名与版本                                  |

驱动配置项 `DriverAttributeBO`（声明该驱动能接受哪些[连接配置](./attribute-config)字段，注册时随驱动一并上报）：

| 字段                  | 类型                 | 含义                                            |
|---------------------|--------------------|-----------------------------------------------|
| `attributeName`     | String             | 配置项名称（如 `Host`、`Port`）                        |
| `attributeCode`     | String             | 配置项标识符，设备配值时按它匹配                              |
| `attributeTypeFlag` | AttributeTypeEnum  | 配置项数据类型（`string` / `int` / `long` / `float`…） |
| `defaultValue`      | String             | 默认值                                           |
| `driverId`          | Long               | 归属的驱动                                         |
| `attributeExt`      | DriverAttributeExt | 扩展配置（JSON）                                    |
| `enableFlag`        | EnableFlagEnum     | 启停标记                                          |
| `tenantId`          | Long               | 归属租户                                          |

::: tip DriverAttribute 是"配置项的声明"，不是"配置值"
`DriverAttribute` 描述的是"这个驱动需要你填 `Host`、`Port`"，是一份**模板**；某台设备真正填入的 `192.168.1.10`、`502`
是[连接配置](./attribute-config)（`DriverAttributeConfig`）。前者由驱动注册产生，后者由你配设备时产生。
:::

## 驱动类型

| 类型 `driverTypeFlag` | code            | 说明                               |
|---------------------|-----------------|----------------------------------|
| `DRIVER_CLIENT`     | `driver-client` | 客户端模式协议驱动，主动连设备（如 Modbus TCP 轮询） |
| `DRIVER_SERVER`     | `driver-server` | 服务端模式协议驱动，等设备来连（如 MQTT、监听类）      |
| `GATEWAY`           | `gateway`       | 网关驱动                             |
| `CONNECT`           | `connect`       | 连接驱动                             |

## 与其它概念的关系

<DriverRelationDiagram lang="zh" />

- 一个驱动**注册一次身份**，可承载**多台**[设备](./device)的采集。
- 驱动注册的 `DriverAttribute` 是模板；每台设备用[连接配置](./attribute-config)按这份模板填值。
- 驱动按[模板](./profile)定义的[位号](./point)采集，把结果翻译成[位号值](./point-value)上报。

## 启动注册与在线状态

<DriverLifecycleDiagram lang="zh" />

驱动启动时由 `DriverInitRunner`（`ApplicationRunner`）触发注册：构造 `RegisterBO`（含 `tenant`、`driver`=`DriverBO`、
`driverAttributes` 等）调用 `DriverRegisterService.initial()` 上报管理中心，注册失败按指数退避重试直至成功。注册后驱动并非"
一注册就永远在线"——它的**在线状态是一份租约**：SDK 周期触发 `DriverHealth.health()` 上报心跳，在 `dc3_entity_state`（
`entity_type_flag = 3` 表示驱动）续租 45 秒；租约到期未续即判定 `offline`。状态取值为 `online` / `offline` / `maintain` /
`fault`。

::: warning 在线状态不在元数据表里
`dc3_driver` 存的是驱动**配置元数据**（名字、服务名、租户），改它不代表驱动在跑。驱动当前是否在线看运行态状态表
`dc3_entity_state`，它由心跳续租维护，进程崩溃 / 网络断开后租约会自然过期翻为离线。查"有哪些驱动"看前者，查"驱动现在通不通"
看后者。
:::

## 示例

你要接入车间一批 Modbus TCP 仪表：

1. 部署 `dc3-driver-modbus-tcp` 服务实例并启动，它注册
   `DriverBO{ serviceName: "dc3-driver-modbus-tcp", driverTypeFlag: DRIVER_CLIENT }`，并声明配置项
   `DriverAttribute{ attributeCode: "host", type: string }`、`{ attributeCode: "port", type: int }`。
2. 在 Web 上新建[设备](./device)挂到该驱动，按声明填[连接配置](./attribute-config)：`host=192.168.1.10`、`port=502`。
3. 驱动据此建立 Modbus 会话，按[模板](./profile)里的[位号](./point)
   周期读寄存器，把读到的原始值翻译成[位号值](./point-value)上报数据中心。
4. 驱动每 15 秒上报一次心跳续租；某天该服务进程被 kill，45 秒后租约到期，平台把这个驱动标记为 `offline`，它名下设备随之转入离线扫描。

## 内置驱动

DC3 自带 **28 个**开箱即用的协议驱动，覆盖工业现场协议（Modbus RTU/TCP、OPC UA/DA、PLC
S7、Melsec、BACnet/IP、IEC104、DLMS、SNMP、CAN…）、物联协议（MQTT、CoAP、LwM2M、HTTP、ZigBee、BLE…）、串口/网络透传（Serial、TCP/UDP）以及数据库接入（MySQL、PostgreSQL、Oracle、SQLServer）。完整清单与各驱动职责见[模块地图](../../architecture/modules)。

## 延伸阅读

- [设备 Device](./device) — 挂在驱动下的接入点，一驱动多设备
- [连接配置 DriverAttributeConfig](./attribute-config) — 设备按驱动声明的 DriverAttribute 填的连接值
- [位号 Point](./point) — 驱动采集的目标数据点
- [核心概念概览](../concepts) — 对象模型与三层配置的全景
- [模块地图](../../architecture/modules) — 28 个内置驱动清单与服务拓扑
- [驱动开发指南](../../development/driver-authoring) — 如何自己写一个 `dc3-driver-*`
