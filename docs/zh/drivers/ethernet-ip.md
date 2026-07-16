---
title: EtherNet/IP 驱动
---

<script setup>
import EthernetIpDiagram from '../../.vitepress/theme/components/EthernetIpDiagram.vue'
</script>


# EtherNet/IP 驱动

`dc3-driver-ethernet-ip` 把基于 EtherNet/IP（CIP）的罗克韦尔 Allen-Bradley PLC 接入 IoT DC3：它以**标签名（Tag Name）**
为目标，周期性读取 PLC 标签值，并支持向标签写值的命令。读完本页你能看懂 EtherNet/IP 的寻址方式、给设备和位号填对协议属性，并清楚这个驱动当前的实现边界。

> 你在这里：现场设备的"罗克韦尔 PLC"接入侧。要理解工业协议为什么各家私有、CIP
> 在网络层处于什么位置，先读 [工业总线与协议](../foundations/fieldbus)。

## 协议背景

EtherNet/IP（Ethernet Industrial Protocol）是一种工业以太网协议，把 **CIP（Common Industrial Protocol，通用工业协议）** 承载在标准
TCP/IP 之上。它由 ODVA 维护，主要用于罗克韦尔 Allen-Bradley 系列 PLC（如 ControlLogix / CompactLogix）以及兼容这套生态的伺服、变频器、I/O
模块。在工厂自动化里，它和 Siemens 的 S7、Mitsubishi 的 MELSEC、Omron 的 FINS 一样，属于"厂商主导、彼此不通"的私有协议阵营——选了哪家
PLC，往往就被绑定到哪套协议。

EtherNet/IP 与 Modbus 这类协议最大的不同在**寻址模型**：

- **Modbus 按寄存器地址寻址**——你要知道某个量挂在第几号保持寄存器（如 `40001`），地址是裸数字。
- **CIP 按标签名寻址**——PLC 工程里的变量有名字（如 `Motor_Speed`），驱动通过 CIP 的 **Data Table Read/Write**
  服务直接按名字读写，不必关心它在控制器内存的物理地址。

这套"按名字访问"的模型让 PLC 程序改动后地址不再漂移，但也意味着标签名必须与 PLC 工程逐字一致。

在物联网四层架构里，EtherNet/IP 属于**网络层**的工业有线侧：它解决"现场设备如何在以太网上把一个数据点送出去"
的问题，位于感知层（传感器/变送器）之上、平台层（IoT DC3 数据汇聚）之下。下面这张图给出 CIP 按名寻址在一次采集里的位置：

<EthernetIpDiagram lang="zh" />

CIP 不依赖物理地址，因此位号上配的是 `tagName` 而非偏移量；驱动按位号配置的 `tagType` 把 PLC
返回的字节解码成具体数值后，统一为 [位号值](../introduction/concepts/point-value) 上送平台。

## 属性配置

EtherNet/IP 的接入参数分两层：**驱动属性（driver-attribute）** 描述"连哪台 PLC、用什么端口和超时"
，填在 [设备](../introduction/concepts/device) 上；**位号属性（point-attribute）** 描述"读哪个标签、按什么类型解码"
，填在每个 [位号](../introduction/concepts/point) 上。可写位号再加一项**命令属性（command-attribute）**。这些属性的默认值都来自驱动的
`application.yml`，三层来历见 [属性与配置](../introduction/concepts/attribute-config)。

### 驱动属性（设备级 `driver-attribute`）

接入一台 EtherNet/IP PLC 时，先在设备上指明它的网络位置。`host` / `port` 决定 TCP 连到哪里，`slot` 标识 PLC
在背板上的槽位（多模块机架需要据此定位 CPU），`timeout` 限制单次请求的等待时长。

| 属性      | code      | 类型     | 默认值         | 说明                           |
|---------|-----------|--------|-------------|------------------------------|
| Host    | `host`    | STRING | `localhost` | PLC 主机地址（IP 或主机名）            |
| Port    | `port`    | INT    | `44818`     | EtherNet/IP TCP 端口（标准 44818） |
| Slot    | `slot`    | INT    | `0`         | PLC 背板槽位号，定位机架中的 CPU 模块      |
| Timeout | `timeout` | INT    | `5000`      | 请求超时（毫秒），设到套接字 `SoTimeout`   |

::: info `slot` 当前仅参与校验，未参与组帧
`validate()` 会把 `slot` 列为必填项校验，但当前连接与读写代码尚未把 `slot` 编入 CIP 路由路径（`ForwardOpen` 的连接路径仍是占位）。单
CPU、CPU 在 0 号槽的场景不受影响；多槽机架的精确寻址需待协议组帧补全。
:::

### 位号属性（`point-attribute`）

每个采集位号都要指明读哪个标签、以及该标签在 PLC 里是什么数据类型——驱动不会向 PLC 探测类型，完全按你填的 `tagType` 解码字节。

| 属性            | code           | 类型     | 默认值    | 说明                                                          |
|---------------|----------------|--------|--------|-------------------------------------------------------------|
| Tag Name      | `tagName`      | STRING | （空）    | CIP 标签名，如 `Motor_Speed`，必须与 PLC 工程逐字一致                      |
| Tag Type      | `tagType`      | STRING | `DINT` | 标签数据类型：`BOOL` / `SINT` / `INT` / `DINT` / `REAL` / `STRING` |
| Element Count | `elementCount` | INT    | `1`    | 读取的元素个数（数组标签用）                                              |

::: tip `tagType` 决定字节怎么解码
驱动按 `tagType` 把 PLC 返回的原始字节（小端序）解析成对应类型：`BOOL` 1 字节、`SINT` 1 字节、`INT` 2 字节整数、`DINT` 4 字节整数、
`REAL` 4 字节浮点、`STRING` 取 ASCII 文本。`tagType` 要与 PLC
里该标签的实际类型一致，否则解析出无意义的值。位号自身的数据类型（[Point](../introduction/concepts/point) 的
`pointTypeFlag`）应与之匹配。
:::

::: warning `elementCount` 当前未被消费
`buildReadTagRequest()` 把读请求里的元素个数硬编码为 `1`，配置的 `elementCount` 暂不生效。数组标签的整段读取需待实现补全；当前只能按单元素读取。
:::

### 命令属性（`command-attribute`）

可写位号在写命令上额外配置写值模板。

| 属性           | code          | 类型     | 默认值        | 说明                            |
|--------------|---------------|--------|------------|-------------------------------|
| Send Command | `sendCommand` | STRING | `${value}` | 写值模板，约定用命令参数渲染后按 `tagType` 编码 |

::: warning `sendCommand` 模板当前未被消费
`write()` 直接取命令传入的值并按 `tagType` 编码（`encodeTagValue()`），未经过 `sendCommand` 模板渲染。该属性目前是预留约定，模板替换逻辑待补全。
:::

### 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义调度**：`schedule.custom` 在 yml 中启用（cron `0/5 * * * * ?`），但当前 `schedule()` 方法体为空，不执行任何自定义逻辑。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`
  ——在线状态机制见 [设备](../introduction/concepts/device)。

## 故障排查

EtherNet/IP 接入失败大多落在"连不上"和"读到的值不对"两类。下面按由表及里的顺序排查。

::: warning 端口或防火墙：44818 不通
EtherNet/IP 显式报文走 TCP `44818`（隐式 I/O 另用 UDP `2222`，本驱动不涉及）。先在驱动主机上确认 `host:44818` 可达（
`telnet <host> 44818` 或 `nc -vz <host> 44818`）。常见原因：PLC 侧未启用 EtherNet/IP 服务、网段不通、防火墙拦了 44818。
`getConnector()` 建连失败会抛 `ConnectorException`，日志含 `EtherNet/IP connection failed`。
:::

::: warning tagName 不存在或大小写不符
CIP 按名字寻址，`tagName` 必须与 PLC 程序里的变量名**逐字一致且区分大小写**。这点和 Modbus 的 `offset` 不同：Modbus
填错偏移会静默读到别的寄存器，而 CIP 标签名不存在会直接读取失败、抛 `ReadPointException`。排查时优先回到 PLC
工程核对标签拼写与作用域（控制器级标签 vs 程序级标签）。
:::

::: warning tagType 与 PLC 真实类型不符，读到乱值
驱动完全按你填的 `tagType` 解码字节、不向 PLC 探测真实类型。把一个 `REAL`（浮点）标签配成 `DINT`，会把浮点的 4
字节当整数解析，得到一个无意义的大数。接入前在 PLC 工程里逐个确认标签的实际类型，并让位号的 `pointTypeFlag` 与之匹配。
:::

::: warning 超时偏短导致间歇性读失败
`timeout` 设到套接字 `SoTimeout`，默认 `5000` 毫秒。网络抖动或 PLC 负载高时，过短的超时会让 `readFully()` 抛
`SocketTimeoutException`，进而触发 `invalidateConnector()` 断开重连。表现为周期性读失败、设备在线态抖动。可适当调大
`timeout`，并优先排查网络质量。
:::

::: info 设备显示在线但读不到值
`health()` 只检查缓存的套接字是否 `isConnected() && !isClosed()`，**不做真实协议探测**。也就是说，TCP 连着但 CIP
会话未真正建立时，设备仍可能显示"在线"。判断是否真正采到数据，应以 [位号值](../introduction/concepts/point-value)
是否更新为准，而非仅看设备在线态。
:::

::: info 多槽机架定位不到 CPU
当前 `slot` 未编入 CIP 路由路径。若 PLC 在非 0 号槽、或机架含多个 CPU，精确寻址需待协议组帧补全；在此之前建议用单 CPU、CPU 置于
0 号槽的场景做接入验证。
:::

## 在 IoT DC3 中如何落地

- **dc3.driver.code**：`EthernetIpDriver`（稳定路由标识，注册与消息路由都以它为准，不要随意改）。驱动名 `EtherNet/IP Driver`
  ，类型 `DRIVER_CLIENT`（驱动主动连 PLC）。
- **读能力**：`read()` 已接通"取标签名 → 组 Read Tag 请求 → 解码字节"的主干流程。
- **写能力**：`write()` 已接通"取标签名/类型 → 编码值 → 组 Write Tag 请求"的主干流程。
- **订阅/上报**：不支持。EtherNet/IP 显式报文是请求-应答模型，本驱动按采集周期主动轮询，不监听设备主动上报。

与 [驱动能力矩阵](./matrix) 对齐：EtherNet/IP 在矩阵中读/写/订阅均标记为 `—`，备注"罗克韦尔 / CIP，骨架待补"。

::: warning 当前为骨架实现（Work in progress）
本驱动是协议骨架。读写的上层流程（按 `tagName` 取数、按 `tagType` 编解码、套接字连接与失效重建）已就位，但 **CIP 协议组帧尚未补全
**：

- 会话建立 `RegisterSession`、连接打开 `ForwardOpen` 仍是 `TODO` 占位；
- `buildEncapsulationHeader()` 只写了一个 24 字节长度头，并非完整的 EtherNet/IP 封装帧；
- `health()` 仅检查套接字状态，非真实协议探测；
- `elementCount`、`sendCommand` 两个属性当前未被消费。

请将它作为接入起点模板，而非生产可用驱动。最终行为以 `EthernetIpDriverCustomServiceImpl` 的 `read()` / `write()` /
`initial()` 源码为准。
:::

把一台 Allen-Bradley PLC 接进来的最小路径（用于验证流程，非生产采集）：

1. 选 `EtherNet/IP Driver` 创建 [设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.20`、`port=44818`、
   `slot=0`、`timeout=5000`。
2. 给设备绑定的 [模板](../introduction/concepts/profile) 加一个转速 [位号](../introduction/concepts/point)（
   `pointTypeFlag=INT`、`READ_ONLY`），point 属性填 `tagName=Motor_Speed`、`tagType=DINT`、`elementCount=1`。
3. 启动驱动观察连接与采集日志；CIP 组帧补全后，30 秒内即可在 [位号值](../introduction/concepts/point-value) 看到采集值。

完整的接入操作流程见 [设备接入](../operation/device-onboarding)。

## 延伸阅读

- [驱动总览](./index) — 全部驱动的分类与选型入口
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力与实现状态一览
- [设备接入](../operation/device-onboarding) — 一次完整的设备接入流程
- [工业总线与协议](../foundations/fieldbus) — 网络层工业有线侧，CIP 与其它私有协议的定位与寻址模型
