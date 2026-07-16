---
title: TCP/UDP 驱动
---

<script setup>
import TcpUdpDiagram from '../../.vitepress/theme/components/TcpUdpDiagram.vue'
</script>


# TCP/UDP 驱动

`dc3-driver-tcp-udp` 把任意"在一个 TCP 或 UDP 端口上裸收发字节流"的设备接入 IoT
DC3：按[位号](../introduction/concepts/point)发一条 HEX
指令、收回原始字节，再按帧规则切片并转成值。读完你能在没有标准协议栈的私有设备上完成采集与写值，并知道字节序、帧偏移与连接退避在哪里出问题。

## 协议背景

TCP 与 UDP 是 [TCP/IP 协议族](../foundations/iot-protocols)里的两个传输层协议：TCP 面向连接、提供可靠有序的字节流；UDP
无连接、以数据报尽力投递。在物联网四层架构（感知层 → 网络层 → 平台层 → 应用层）中，它们属于**网络层**
——是上层各类应用协议（MQTT、CoAP、Modbus TCP 等）共同的承载底座。

很多现场设备并没有跑标准协议栈：串口转网口模块、自研单片机、私有协议网关，往往只是在某个端口上"你发一段字节、它回一段字节"
。这类设备无法用某个特定协议驱动接入。`dc3-driver-tcp-udp` 就是它们的通用底座——它不引入任何第三方协议库，直接用 JDK 的
`Socket` / `DatagramSocket` 收发，把"发什么指令、回包怎么解析"
全部交给[属性配置](../introduction/concepts/attribute-config)。

TCP 与 UDP 在本驱动里的行为差异很重要：

- **TCP**：按设备缓存一条长连接（`tcpConnectMap`），避免每轮采集都重做三次握手；连接断开或通信异常时失效并重连。
- **UDP**：无连接，每次采集临时新建一个 `DatagramSocket` 发包、等回包、随即关闭。

::: info HEX 指令与帧（frame）
你和设备之间收发的是二进制。本驱动统一用十六进制字符串书写指令（如 `01 03 00 00 00 02`，空白会被忽略）。设备回来的一整段字节叫一帧；
`dataOffset` / `dataLength` 用来从帧里定位出真正要的数据，`dataFormat` 决定这段字节如何变成位号值。
:::

- **驱动名 / code**：`TCP/UDP Raw Driver` / `TcpUdpDriver`
- **类型**：`DRIVER_CLIENT`（驱动主动连设备、发指令）

## 属性配置

属性来自驱动的 `application.yml`，分三层：**驱动属性**填在[设备](../introduction/concepts/device)上（一台设备一份连接参数），
**位号属性**填在每个[位号](../introduction/concepts/point)上（描述这一路采什么、怎么解析），**写命令属性**
填在可写位号的写命令上。每张表前的散文先讲清每个属性干什么。

### 驱动属性（设备级 `driver-attribute`）

`protocol` 二选一决定走 TCP 还是 UDP；`host` / `port` 指向设备的网络地址（端口默认值 `502` 仅是占位，按实际设备改）；
`connectTimeout` 是 TCP 建连超时、`readTimeout` 是等待设备回包的读超时，单位都是毫秒。`delimiter` 为按分隔符切包预留，当前实现以
`dataOffset`/`dataLength` 切帧为主。

| 属性              | code             | 类型     | 默认值         | 说明            |
|-----------------|------------------|--------|-------------|---------------|
| Protocol        | `protocol`       | STRING | `TCP`       | TCP or UDP    |
| Host            | `host`           | STRING | `localhost` | 设备 IP / 主机名   |
| Port            | `port`           | INT    | `502`       | 设备端口          |
| Connect Timeout | `connectTimeout` | INT    | `5000`      | TCP 连接超时，毫秒   |
| Read Timeout    | `readTimeout`    | INT    | `3000`      | 读响应超时，毫秒      |
| Delimiter       | `delimiter`      | STRING | （空）         | Hex delimiter |

### 位号属性（`point-attribute`）

`sendCommand` 是这一路采集时发出的 HEX 指令；驱动收到回包后，按 `dataOffset` + `dataLength` 切出一段字节，再按 `dataFormat`
转换，多字节量受 `byteOrder` 控制。`frameHeader` / `frameFooter` / `receiveLength` 为帧头帧尾、定长收包预留。

| 属性             | code            | 类型     | 默认值   | 说明                                 |
|----------------|-----------------|--------|-------|------------------------------------|
| Send Command   | `sendCommand`   | STRING | （空）   | 采集时发送的 HEX 指令                      |
| Receive Length | `receiveLength` | INT    | `0`   | 0 means use delimiter              |
| Frame Header   | `frameHeader`   | STRING | （空）   | 帧头 HEX                             |
| Frame Footer   | `frameFooter`   | STRING | （空）   | 帧尾 HEX                             |
| Data Offset    | `dataOffset`    | INT    | `0`   | 数据在帧中的字节偏移                         |
| Data Length    | `dataLength`    | INT    | `0`   | 数据字节长度                             |
| Data Format    | `dataFormat`    | STRING | `HEX` | HEX/ASCII/INT16/UINT16/INT32/FLOAT |
| Byte Order     | `byteOrder`     | STRING | `BIG` | 字节序：BIG / LITTLE                   |

::: tip dataFormat 决定回包怎么变成值
驱动按 `dataOffset` + `dataLength` 从回包里切出一段字节，再按 `dataFormat` 转换：`HEX` 原样输出十六进制字符串、`ASCII`
转文本（末尾空白会被 trim）、`INT16/UINT16/INT32/FLOAT` 按数值解析（多字节量受 `byteOrder` 控制，`BIG` 为大端、`LITTLE` 为小端）。
`INT16/INT32/FLOAT` 要求切出的字节数分别 ≥2/≥4，长度不够时回退成 HEX。若 `dataLength=0`，则不切帧、整段回包以 HEX 返回。
:::

下面这张状态/数据流图把"一次采集"从发指令到落值的关键跳串起来：

<TcpUdpDiagram lang="zh" />

### 写命令属性（`command-attribute`）

可写位号的写命令上填 `sendCommand` 模板，里面用 `${value}` 占位。写值时驱动把 `${value}` 替换成实际命令值，再作为 HEX
指令发给设备（TCP 复用长连接、UDP 临时建 socket）。

| 属性           | code          | 类型     | 默认值        | 说明                      |
|--------------|---------------|--------|------------|-------------------------|
| Send Command | `sendCommand` | STRING | `${value}` | 写指令模板，`${value}` 用命令值替换 |

::: warning 写命令的 `sendCommand` 读自位号属性
源码中 `write()` 的 `sendCommand` 取自**位号属性**（`pointConfig`）而非写命令属性。若可写位号没有在位号属性里配
`sendCommand`，写操作会因指令为空而直接返回失败。`command-attribute` 的 `${value}` 模板在 `execute()` 渲染流程中生效。
:::

## 故障排查

- **位号值是一长串 HEX、却期望是数字**：多半是 `dataOffset` + `dataLength` 超过了回包实际长度，或 `dataLength=0`。越界时驱动
  **不报错**，而是放弃切帧、把整段回包以 HEX 原样返回。先抓一帧真实回包，数清目标数据落在第几字节、占几字节，再对上
  `dataOffset` / `dataLength`。
- **数值符号/大小明显不对**：多字节量的 `byteOrder` 没对上设备。设备是大端就填 `BIG`、小端填 `LITTLE`；`UINT16` 与 `INT16`
  在高位为 1 时差一个符号，按设备语义选对格式。
- **`sendCommand` 解析失败或读到错值**：`sendCommand` / `frameHeader` / `frameFooter` 都按十六进制解析（空白被忽略，可写成
  `01 03 00 00`）。填入非 HEX 字符（如把十进制 `10` 当一个数）会解析失败。`dataFormat=ASCII` 只影响回包字节如何转文本，指令本身仍必须是
  HEX。
- **设备一直显示离线 / 暂时连不上**：TCP 连续 3 次连接或读写失败后进入 **60 秒退避窗口**
  暂停重连，其间设备按离线上报，窗口过后自动重试；成功通信一次即清零计数。看到短时离线先确认是否在退避窗口内，再查 `host`/
  `port`/防火墙。
- **读超时**：`readTimeout` 默认 3000ms。设备回包慢或 UDP 丢包会触发读超时（UDP 收不到回包即抛 `SocketTimeoutException`
  ）。适当调大 `readTimeout`，UDP 还要确认对端确实有回包。
- **UDP 设备健康状态恒为在线**：UDP 无连接，`health()` 对 UDP 默认按在线上报（不做探测）。"在线"不代表数据通，仍要看位号是否真有新值落库。

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`TcpUdpDriver`——稳定路由标识，平台据此把命令分发到本驱动，不应随意改动。
- **读 / 写 / 订阅**：本驱动 `read()` 主动发指令采值、`write()` 渲染指令写值，二者均已实现；**不提供订阅**——`schedule()`
  为空方法，无自定义周期任务。这与[驱动能力矩阵](./matrix)中本驱动"读 ✓ / 写 ✓ / 订阅 —"一致。
- **采集与健康**：默认采集 cron `0/30 * * * * ?`（每 30 秒一轮）；设备健康检查 cron `0/15 * * * * ?`、租约超时 `45 秒`。TCP
  设备靠缓存连接状态或一次快速建连判断在线，UDP 默认按在线上报。

::: info 自定义调度已启用但无动作
`schedule.custom` 默认启用、cron `0/5 * * * * ?`，但 `schedule()` 是空方法，本驱动未实现任何自定义周期任务，该调度实际不执行动作。这是有意为之的占位，不影响正常采集。
:::

### 最小接入示例

把一台 `192.168.1.50:8899` 的 TCP 设备接进来，采集一个 16 位温度值（设备回 `01 03 04 00 FA 12 34 ...`，温度在第 3、4 字节）：

1. 选 `TCP/UDP Raw Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `protocol=TCP`、`host=192.168.1.50`、
   `port=8899`。
2. 给设备绑定的[模板 Profile](../introduction/concepts/profile) 加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=INT`、`READ_ONLY`），位号属性填 `sendCommand=010300000001`、`dataOffset=3`、`dataLength=2`、
   `dataFormat=INT16`、`byteOrder=BIG`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到解析出的数值（`00FA` → `250`）。

## 延伸阅读

- [驱动总览](./index) — 28 个协议驱动的全景与分组
- [驱动能力矩阵](./matrix) — 各驱动的读/写/订阅能力对照
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [IoT 协议与无线网络](../foundations/iot-protocols) — TCP/UDP 所在网络层与上层应用协议的关系
- [Modbus TCP 驱动](./modbus-tcp) — 标准化的 TCP 协议示例，可与本通用驱动对照
