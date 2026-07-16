---
title: CAN 总线驱动
---

# CAN 总线驱动

`dc3-driver-can` 把 CAN 总线设备接入 IoT DC3：作为总线上的一个节点，监听 SocketCAN 接口上匹配指定 CAN ID
的帧并按[位号](../introduction/concepts/point)配置解析为采集值，必要时先发"请求帧"再读"应答帧"，并通过命令帧写值。读完本页，你能在一台
CAN 设备上配好驱动属性与位号属性，并判断哪些行为已就位、哪些仍是骨架。

## 协议背景

CAN（Controller Area
Network，控制器局域网）是汽车与工业自动化里使用极广的现场总线，在[物联网四层架构](../foundations/fieldbus)里属于**网络层**
——它定义了设备之间怎么在一根总线上可靠地交换帧。

CAN 与 Modbus 这类主从协议最大的不同在于它是**广播 + 按 ID 过滤**的发布-订阅模型：

- 节点不靠地址点对点通信，而是把带 **CAN ID**（标识符）的帧广播到总线上，所有节点都能收到，由接收方按自己关心的 CAN ID 过滤。
- 一帧最多携带 **8 字节**载荷；要表达的物理量按字节偏移、长度、字节序从这 8 字节里切分出来。
- 标准帧用 **11 位** CAN ID，扩展帧用 **29 位** CAN ID，二者由帧格式标志区分。
- 没有中心轮询器，天然适合多接收方、事件驱动的场景：值变就发，谁关心谁收。

典型用途包括车载 ECU、电池管理系统（BMS）、伺服驱动器、各类传感器节点，以及越来越多的工业嵌入式控制器。在 Linux 上，CAN 设备通常以
**SocketCAN** 网络接口（如 `can0`）的形式呈现，应用通过它收发帧——本驱动正是接到这层接口上工作。

::: info CAN 在物联网网络层的位置
CAN 解决的是"同一根总线上的节点如何交换帧"，属于现场总线（网络层）范畴。它和 Modbus、Profibus、BACnet
等并列；和这些协议如何在四层架构里取舍，见[工业总线与协议](../foundations/fieldbus)。
:::

## 属性配置

CAN 设备的接入配置分三层填写：设备级 `driver-attribute`（接口、波特率、帧格式），位号级 `point-attribute`（CAN
ID、字节切分、可选请求帧），以及可写位号的 `command-attribute`（写目标与帧数据模板）。下表均来自驱动 `application.yml`
，先读散文理解每项作用，再照表填值。

### 驱动属性（设备级 `driver-attribute`）

接入一台 CAN 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。
`interfaceName` 指向驱动进程所在 Linux 主机上的 SocketCAN 接口名，是必填项（其它属性都建立在它之上）；`bitrate` 与
`frameFormat` 描述总线本身的物理与帧格式特征，必须与设备实际一致。

| 属性           | code            | 类型     | 默认值        | 说明                                        |
|--------------|-----------------|--------|------------|-------------------------------------------|
| Interface    | `interfaceName` | STRING | `can0`     | SocketCAN 接口名，驱动据此收发帧                     |
| Bitrate      | `bitrate`       | INT    | `500000`   | CAN 总线波特率（bps），需与总线一致                     |
| Frame Format | `frameFormat`   | STRING | `STANDARD` | 帧格式：`STANDARD`(11bit) 或 `EXTENDED`(29bit) |

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填以下属性。前五项决定"
匹配哪一帧、从载荷哪几个字节、按什么格式和字节序解析"；后两项 `requestCanId`/`requestData` 是可选的"先请求后应答"
机制，留空则纯被动监听。

| 属性             | code           | 类型     | 默认值      | 说明                         |
|----------------|----------------|--------|----------|----------------------------|
| CAN ID         | `canId`        | STRING | （空）      | 要匹配的 CAN 标识符（十六进制，不带 `0x`） |
| Data Offset    | `dataOffset`   | INT    | `0`      | 帧载荷内的起始字节偏移                |
| Data Length    | `dataLength`   | INT    | `1`      | 从偏移处读取的字节数                 |
| Data Format    | `dataFormat`   | STRING | `INT`    | 解析格式：`INT`/`UINT`/`HEX`    |
| Byte Order     | `byteOrder`    | STRING | `LITTLE` | 多字节的字节序（如 `LITTLE`）        |
| Request CAN ID | `requestCanId` | STRING | （空）      | 可选请求帧的 CAN ID              |
| Request Data   | `requestData`  | STRING | （空）      | 可选请求帧的载荷（十六进制）             |

::: tip 主动请求型读取
不少 CAN 设备需要先收到一帧"请求"才会应答数据。源码中，只有 `requestCanId` 与 `requestData` **同时非空**时，驱动才会在采集前用
`cansend` 发一帧请求（形如 `cansend can0 <requestCanId>#<requestData>`），再监听 `canId` 匹配的应答帧；两者留空则纯被动监听总线上周期广播的帧。
:::

::: warning dataOffset / dataLength / byteOrder 当前未参与解析
按源码，`read()` 实际只用到 `interfaceName`、`canId`、`requestCanId`、`requestData`，把 `candump` 抓到的那一帧载荷字段原样返回为采集值；
`dataOffset`、`dataLength`、`dataFormat`、`byteOrder` 这几项尚未在读取路径里用于切分/转换字节（属骨架待补部分）。配置项已就位，行为以后续实现为准。
:::

### 写命令属性（`command-attribute`）

`application.yml` 在 `command-attribute` 下声明了 `canId` 与 `data`（`data` 默认 `${value}`），原意是让写命令携带帧数据模板。

::: warning `data` 模板当前不会生效（骨架待补）
按源码，`write()` 只从**位号属性**（`pointConfig`）读取 `canId` 与 `data`：
`canId = getConfigValue(pointConfig, "canId", "")`、`data = getConfigValue(pointConfig, "data", "")`。但 `point-attribute`
里**没有** `data`（`data` 只声明在 `command-attribute` 下），而 `command-attribute` 仅经
`DriverCommand.execute(commandConfig, …)` 这条路传入——CAN 驱动并未覆写 `execute()`（用默认空实现），所以写路径根本读不到
`command-attribute`。结果是 `data` 恒取默认空串、`frameData` 恒为空，`${value}` 模板不会被渲染，`cansend` 发出的载荷为空（形如
`cansend can0 <canId>#`）。这与 `dataOffset`/`dataLength` 同属"骨架待补"部分，下发的写值当前不会真正落到帧载荷里。
:::

| 属性     | code    | 类型     | 默认值        | 说明                                       |
|--------|---------|--------|------------|------------------------------------------|
| CAN ID | `canId` | STRING | （空）        | 写入目标的 CAN 标识符（十六进制）                      |
| Data   | `data`  | STRING | `${value}` | 帧数据模板（设计上用命令参数渲染 `${value}`，当前未接通，见上方告警） |

### 采集与健康

- **采集周期**：`read` 调度默认 cron `0/30 * * * * ?`（每 30 秒抓一轮帧）；驱动另有 `custom` 自定义调度默认 cron
  `0/5 * * * * ?`，但 CAN 驱动的 `schedule()` 为空实现，未挂自定义任务。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——驱动用 `ip link show <interfaceName>`
  判断接口是否存在（退出码 0 即在线），在线状态机制见[设备](../introduction/concepts/device)。

::: details 最小接入示例
把 `can0` 上一个周期广播温度、CAN ID 为 `123` 的节点接进来：

1. 选 `CAN Bus Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `interfaceName=can0`、`bitrate=500000`、
   `frameFormat=STANDARD`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`
   ），point 属性填 `canId=123`、`dataOffset=0`、`dataLength=2`、`dataFormat=INT`、`byteOrder=LITTLE`，`requestCanId`/
   `requestData` 留空（被动监听）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。
   :::

## 故障排查

- **驱动必须跑在装了 `can-utils` 的 Linux 上**。底层读写依赖 `candump`/`cansend`，健康检查依赖 `ip link show`，且要求一个可用的
  SocketCAN 接口。命令通过 `sh -c` 执行：在 macOS/Windows 或缺少 `can-utils` 时，`candump` 无输出，`read()` 会因
  `output.isEmpty()` 抛 `No CAN frame received` 读异常（`ReadPointException`）；写侧 `cansend` 缺失则因 `executeCommand`
  的退出码/超时进入 `WritePointException`，设备一直离线。
- **设备一直离线**。健康检查实质是 `ip link show <interfaceName>` 的退出码：接口名写错、接口未 `up`、或进程无权限访问该接口，都会让退出码非
  0 而判离线。先在主机上手动跑 `ip link show can0` 确认接口存在且 UP。
- **采到 `No CAN frame received`**。`candump` 用 `timeout 3` 抓单帧，3 秒内没等到匹配 `canId` 的帧就抛读异常。排查方向：
  `canId` 写错（大小写/进制）、`frameFormat` 与设备实际帧格式（11/29 位）不一致、设备本就需要先收到请求帧——后者要配上
  `requestCanId`/`requestData`。
- **canId 写法要对**。`canId`/`requestCanId` 按 `can-utils` 的写法填十六进制、**不带 `0x` 前缀**（标准帧如 `123`，扩展帧按其
  29 位十六进制原文填）。带前缀或写成十进制会匹配不到帧。
- **波特率/帧格式不匹配**。`bitrate` 与 `frameFormat` 必须与总线和设备一致；总线波特率配错会导致整条总线收不到任何帧，表现为持续
  `No CAN frame received`。
- **写值没生效**。当前写路径未真正接通：`write()` 从位号属性读取 `data`，而 `data` 仅声明在 `command-attribute`、且
  `execute()` 未实现，因此 `data` 恒为空、`${value}` 不被渲染，`cansend` 发出的是空载荷帧（`cansend can0 <canId>#`
  ），设备收不到预期数据——这属于"骨架待补"，不是配置问题。即便如此仍可先确认目标 `canId` 是否正确、该位号是否为可写（
  `rwFlag`）。

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`CanDriver`（驱动名 `CAN Bus Driver`，类型 `DRIVER_CLIENT`，主动在总线上收发帧）。这是稳定的路由标识，不可随意更改。
- **读能力**：✓ 支持。`read()` 通过 `candump` 抓取匹配 `canId` 的单帧并返回载荷字段，可选先用 `cansend` 发请求帧。
- **写能力**：桩/部分实现。`write()` 已能调 `cansend` 把帧发上总线，但 `data` 帧数据模板当前未接通（`data` 取自
  `pointConfig` 却只声明于 `command-attribute`、`execute()` 未实现），故 `${value}` 不被渲染、当前发出的是空载荷帧（详见上方"
  写命令属性"告警）。
- **订阅能力**：— 不支持。CAN 在本驱动里是请求-响应式的定时主动读，并非把订阅推送接进 DC3。以上与[驱动能力矩阵](./matrix)中
  CAN 行（✓ 读 / — 写 / — 订阅）一致。

::: warning 实现状态：骨架（WIP），底层走 can-utils
该驱动是一个起步模板。`read()`/`write()` 通过 `ProcessBuilder` 调用 Linux `can-utils`（`candump`/`cansend`）完成收发，
`health()` 用 `ip link show` 检查接口——这些调用路径在装了 can-utils 的 Linux + 真实 SocketCAN 接口上能真正执行，而非抛"
未实现"。但源码自身标注为 WIP 骨架，仍有未接通处：

- **读路径**：位号属性里的 `dataOffset`/`dataLength`/`dataFormat`/`byteOrder` 尚未参与字节切分与类型转换，`read()`
  把抓到的载荷字段原样返回。
- **写路径**：`write()` 的 `data` 帧数据模板渲染当前未真正接通——`data` 取自 `pointConfig` 但仅声明于 `command-attribute`
  ，且 `execute()` 未实现，故 `${value}` 不被渲染、当前发出的是空载荷帧。与 `dataOffset` 等同属待补骨架，**不要把 write
  当作已可写值**。
- `TODO` 标注计划用原生 SocketCAN JNI 替换每次起进程的 `ProcessBuilder` 方案以降延迟。生产前需补齐字节解析、写值模板渲染与原生
  I/O 集成。
  :::

## 延伸阅读

- [驱动总览](./index) — 全部驱动的导航与分类
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — CAN 所在的网络层与现场总线选型
