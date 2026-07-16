---
title: DLMS/COSEM 驱动
---

<script setup>
import DlmsDiagram from '../../.vitepress/theme/components/DlmsDiagram.vue'
</script>


# DLMS/COSEM 驱动

`dc3-driver-dlms` 把 DLMS/COSEM 计量设备（电表、水表、气表、热表）接入 IoT DC3：它以 **OBIS 编码**为目标，作为 DLMS
客户端连接表计，周期性读取 COSEM 对象的属性值。读完本页你能看懂 DLMS/COSEM 的对象寻址方式、给设备和位号填对协议属性，并清楚这个驱动当前的实现边界。

> 你在这里：现场设备的"计量表"接入侧。要理解计量协议为什么自成一套、OBIS
> 编码在网络层处于什么位置，先读 [工业总线与协议](../foundations/fieldbus)。

## 协议背景

DLMS/COSEM（Device Language Message Specification / Companion Specification for Energy
Metering）是电力、水、气、热等公用事业计量领域的国际标准协议，对应 IEC 62056 / EN 13757-1 系列标准，由 DLMS User Association
维护。它是抄表系统、能源管理平台与智能电表/水表之间事实上的通用语言：欧洲与中国的大量智能电表、集中器、采集终端都说这套协议。

DLMS/COSEM 与 Modbus、CIP 这类协议最大的不同在**对象化的寻址模型**：

- **Modbus 按寄存器地址寻址**——你要知道某个量挂在第几号保持寄存器（如 `40001`），地址是裸数字。
- **DLMS/COSEM 按对象 + OBIS 编码寻址**——表里每个可读量（有功电能、电压、时钟……）被建模为一个 **COSEM 对象**，用一段 6 字段的
  **OBIS 编码**（形如 `1.0.1.8.0.255`）唯一标识；每个对象又有若干带编号的**属性**（attribute），其中属性 `2` 通常是"当前值"
  。读一个量，就是"按 OBIS 编码定位对象、按属性编号取值"。

这套"对象 + 编码"的模型让计量语义高度标准化——`1.0.1.8.0.255` 在任何符合规范的电表里都表示"总有功电能"
，跨厂商可移植；代价是接入前必须查表确认每个量的 OBIS 编码与对象类型。

在物联网四层架构里，DLMS/COSEM 属于**网络层**的计量协议侧：它解决"一只表计如何把计量数据按标准语义送出去"
的问题，位于感知层（计量芯片/传感）之上、平台层（IoT DC3 数据汇聚）之下。下面这张图给出 OBIS 按编码寻址在一次采集里的位置：

<DlmsDiagram lang="zh" />

驱动用 **Gurux DLMS 库**（`GXDLMSClient`）构建并解析 DLMS 帧，作为客户端（client）通过 TCP 或串口连到表计，按位号配置的 OBIS
编码取对应属性值，统一为 [位号值](../introduction/concepts/point-value) 上送平台。

## 属性配置

DLMS/COSEM 的接入参数分两层：**驱动属性（driver-attribute）** 描述"连哪只表、用什么传输方式与认证"
，填在 [设备](../introduction/concepts/device) 上；**位号属性（point-attribute）** 描述"读哪个对象、取哪个属性"
，填在每个 [位号](../introduction/concepts/point) 上。这些属性的默认值都来自驱动的 `application.yml`
，三层来历见 [属性与配置](../introduction/concepts/attribute-config)。DLMS/COSEM 是只读抄表语义，本驱动不提供写命令，因此没有命令属性（
`command-attribute` 为空）。

### 驱动属性（设备级 `driver-attribute`）

接入一只表计时，先在设备上指明它的传输方式与网络/串口位置，再填 DLMS 会话的客户端/服务端地址与认证。`transportType` 决定走
TCP 还是串口，对应两组互斥的连接参数；`clientAddress` / `serverAddress` 标识 DLMS 会话的两端；`authentication` / `password`
决定以什么权限建立关联。

| 属性             | code             | 类型     | 默认值            | 说明                          |
|----------------|------------------|--------|----------------|-----------------------------|
| Transport Type | `transportType`  | STRING | `TCP`          | 传输方式（TCP, SERIAL）           |
| Host           | `host`           | STRING | `localhost`    | 远端设备地址（TCP 模式）              |
| Port           | `port`           | INT    | `4059`         | 远端设备端口（TCP 模式，DLMS 标准 4059） |
| Serial Port    | `serialPort`     | STRING | `/dev/ttyUSB0` | 串口路径（SERIAL 模式）             |
| Baud Rate      | `baudRate`       | INT    | `9600`         | 波特率（SERIAL 模式）              |
| Client Address | `clientAddress`  | INT    | `16`           | DLMS 客户端地址（公共客户端=16）        |
| Server Address | `serverAddress`  | INT    | `1`            | DLMS 服务端地址                  |
| Authentication | `authentication` | STRING | `NONE`         | 认证方式（NONE, LOW, HIGH）       |
| Password       | `password`       | STRING | （空）            | 认证密码（LOW/HIGH 认证时使用）        |

::: tip TCP 与 SERIAL 二选一
`transportType=TCP` 时，只看 `host` / `port`；`transportType=SERIAL` 时，只看 `serialPort` / `baudRate`
。另一组属性按当前传输方式被忽略，不必删。`clientAddress` / `serverAddress` / `authentication` / `password` 两种方式都用。
:::

::: tip `clientAddress=16` 是公共客户端
`clientAddress=16` 对应 DLMS 的"公共客户端"（public client），多数表计允许其以 `NONE`
认证读取基础计量量。要读受保护的对象（如负荷曲线、参数配置），需改用更高权限的客户端地址，并把 `authentication` 调到 `LOW` /
`HIGH` 并配上 `password`。
:::

::: info `validate()` 只校验五项必填
驱动的 `validate()` 把 `transportType` / `host` / `port` / `clientAddress` / `serverAddress` 列为必填项；`serialPort` /
`baudRate` / `authentication` / `password` 不在必填校验内（按需填）。校验只检查"有没有填"，不检查传输方式与所填参数是否自洽——见下文故障排查。
:::

### 位号属性（`point-attribute`）

每个采集位号都要指明读哪个 COSEM 对象、以及取该对象的哪个属性。OBIS 编码定位"读哪个量"，属性编号定位"取这个量的哪一面"。

| 属性           | code          | 类型     | 默认值        | 说明                                 |
|--------------|---------------|--------|------------|------------------------------------|
| Object Type  | `objectType`  | STRING | `REGISTER` | DLMS 对象类型（REGISTER, CLOCK, DATA 等） |
| Logical Name | `logicalName` | STRING | （空）        | 对象逻辑名 / OBIS 编码（例 `1.0.1.8.0.255`） |
| Attribute ID | `attributeId` | INT    | `2`        | 属性编号（2=当前值）                        |

::: tip OBIS 编码定位"读哪个量"
`logicalName` 是 6 段 OBIS 编码，唯一标识表里的一个计量量——例如 `1.0.1.8.0.255` 是"总有功电能"，`1.0.32.7.0.255` 是"A
相电压"。`objectType` 告诉驱动这个对象是哪一类 COSEM 接口类（`REGISTER` 计量寄存器、`CLOCK` 时钟、`DATA`
通用数据等），不同接口类的属性含义不同。`attributeId=2` 取该对象的"当前值"
属性。位号自身的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要与对象属性的实际类型对得上。
:::

::: info `validatePoint()` 只校验 `objectType`
位号校验只把 `objectType` 列为必填；`logicalName` 默认值为空，但若不填则定位不到对象。接入时务必为每个采集位号填写实际的
OBIS 编码。
:::

### 采集与健康

- **采集周期**：默认 read cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义调度**：`schedule.custom` 在 yml 中启用（cron `0/5 * * * * ?`），但当前 `schedule()` 方法体为空，不执行任何自定义逻辑。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`
  ——在线状态机制见 [设备](../introduction/concepts/device)。

## 故障排查

DLMS/COSEM 接入失败大多落在"传输方式没配对"和"读不到对象"两类。下面按由表及里的顺序排查。

::: warning 传输方式与连接参数对不上
`host` / `port` 只在 `transportType=TCP` 下生效，`serialPort` / `baudRate` 只在 `SERIAL` 下生效。`validate()` 不会拦下"
传输方式与参数不自洽"的组合——若把 `transportType` 设成 `SERIAL` 却只填了 `host`，驱动会走串口分支去找 `serialPort`
，连不上表计。改传输方式时，记得同步填对应那一组属性。
:::

::: warning TCP 端口或防火墙：4059 不通
DLMS over TCP 的标准端口是 `4059`，与 Modbus 的 `502`、IEC 104 的 `2404` 都不同，别沿用。先在驱动主机上确认 `host:4059` 可达（
`telnet <host> 4059` 或 `nc -vz <host> 4059`）。常见原因：表计/集中器侧未开放 DLMS 端口、网段不通、防火墙拦了 4059。
:::

::: warning 串口路径或波特率不符
串口模式下，`serialPort`（如 `/dev/ttyUSB0`）必须是驱动主机上真实存在的设备节点，且当前用户有读写权限（Linux 上常需把用户加入
`dialout` 组）。`baudRate` 要与表计设定一致（计量表常见 `300` / `9600` / `19200`），波特率不符会导致帧无法正确解析。
:::

::: warning OBIS 编码或对象类型不符，定位不到量
`logicalName` 必须与表计里实际存在的对象逐字一致，`objectType` 要与该对象的真实 COSEM 接口类匹配。把一个 `CLOCK`（时钟）对象配成
`REGISTER`，或填了表里不存在的 OBIS 编码，都会导致取数失败。接入前先用厂商的对象列表（OBIS 表）核对每个量的编码与对象类型。
:::

::: warning 认证权限不足，读受保护对象被拒
`clientAddress=16` 的公共客户端通常只能读基础计量量。读负荷曲线、事件日志、参数等受保护对象时，若仍用公共客户端或
`authentication=NONE`，表计会拒绝关联或拒绝读取。需改用更高权限的客户端地址，并把 `authentication` 调到 `LOW` / `HIGH`
配上正确的 `password`。
:::

::: info 设备在线态不可作为"采到数据"的依据
`health()` 只检查驱动内部是否缓存了该设备的 `GXDLMSClient` 对象，**不做真实连通探测**
——而当前实现下连接缓存从未被填充（传输层待补，见下文）。判断是否真正采到数据，应以 [位号值](../introduction/concepts/point-value)
是否更新为准，而非看设备在线态。
:::

## 在 IoT DC3 中如何落地

- **dc3.driver.code**：`DlmsDriver`（稳定路由标识，注册与消息路由都以它为准，不要随意改）。驱动名 `DLMS/COSEM Driver`，类型
  `DRIVER_CLIENT`（驱动主动连表计）。
- **读能力**：抄表语义，本应按 OBIS 编码周期性读 COSEM 属性——但当前 `read()` 直接抛 `ReadPointException`
  ，取数主干尚未接通（见下方骨架说明）。
- **写能力**：不提供。DLMS/COSEM 在本驱动中是只读抄表，`command-attribute` 为空，`write()` 直接抛 `WritePointException`。
- **订阅/上报**：不支持。本驱动按采集周期主动轮询，不监听表计主动上报。

与 [驱动能力矩阵](./matrix) 对齐：DLMS 在矩阵中读/写/订阅均标记为 `—`，备注"智能电表，传输层待补"。

::: warning 当前为骨架实现（Work in progress）
本驱动是协议骨架，比"上层流程已接通、仅差组帧"的驱动更早期：Gurux 客户端（`GXDLMSClient`）能生成 DLMS 帧，但**传输层收发与
HDLC 握手尚未实现**，取数/写值主干都还没接通：

- `read()` / `write()` 直接抛"未实现"异常（`ReadPointException` / `WritePointException`）快速失败，让 SDK
  记录失败并对连接退避，而非回显缓存值或伪造成功；
- `health()` 仅检查内部连接缓存 `clientMap` 是否含该设备，并非真实协议探测；而该缓存在当前实现下从未被填充，故设备实际不会显示在线；
- `schedule()` 方法体为空，yml 中启用的 `custom` 周期任务暂不执行任何逻辑。

请将它作为接入新表计的起点模板，而非生产可用驱动。下文属性表与采集周期均逐字取自真实 `application.yml`，可照填；但实际取数行为以
`DlmsDriverCustomServiceImpl` 的 `read()` / `write()` / `initial()` 源码为准。
:::

把一只 DLMS/COSEM 电表接进来的最小路径（用于验证配置流程，非生产采集）：

1. 选 `DLMS/COSEM Driver` 创建 [设备](../introduction/concepts/device)，driver 属性填 `transportType=TCP`、
   `host=192.168.1.20`、`port=4059`、`clientAddress=16`、`serverAddress=1`、`authentication=NONE`。
2. 给设备绑定的 [模板](../introduction/concepts/profile) 加一个电能 [位号](../introduction/concepts/point)（
   `pointTypeFlag=DOUBLE`、`READ_ONLY`），point 属性填 `objectType=REGISTER`、`logicalName=1.0.1.8.0.255`、`attributeId=2`。
3. 启动驱动观察日志；当前 `read()` 直接抛 `ReadPointException`，SDK
   记录读失败并退避，[位号值](../introduction/concepts/point-value) 里暂时不会有采集值——待传输层补全后才能真正采到值。

完整的接入操作流程见 [设备接入](../operation/device-onboarding)。

## 延伸阅读

- [驱动总览](./index) — 全部驱动的分类与选型入口
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力与实现状态一览
- [设备接入](../operation/device-onboarding) — 一次完整的设备接入流程
- [工业总线与协议](../foundations/fieldbus) — 网络层的计量协议侧，OBIS 对象寻址与其它协议的定位对比
