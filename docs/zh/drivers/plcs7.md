---
title: PLC S7 驱动
---

<script setup>
import PlcS7Diagram from '../../.vitepress/theme/components/PlcS7Diagram.vue'
</script>


# PLC S7 驱动

`dc3-driver-plcs7` 把西门子 S7 系列 PLC 接入 IoT DC3：它作为 S7 客户端通过 TCP 连到一台或多台
PLC，按[位号](../introduction/concepts/point)上配置的数据块号与偏移地址周期读值，并支持反向写值。读完这页，你能在设备上正确填好
`host` / `plcType` / `dbNum` 等属性，把一个 DB 变量接成可读可写的位号，并能定位最常见的连不上、读不到、写失败问题。

## 协议背景

S7（也叫 S7comm / ISO-on-TCP）是西门子 PLC（S7-200/300/400/1200/1500、S7-200 Smart 及 SINUMERIK 数控系统等）使用的私有以太网协议。它跑在
`ISO 8073 COTP` 之上、再封装进 TCP，标准端口 `102`，用于直接读写 PLC 内部存储区——尤其是工程师在 STEP 7 / TIA Portal 里定义的
**数据块（Data Block, DB）**。

从通信模型看，S7 是典型的**主从 / 请求-响应**协议：本驱动作为主站（client）主动发起连接、轮流向各 PLC 发读写请求，PLC
被动应答，自己不会主动上报。寻址采用「DB 号 + 字节偏移（+ 位偏移）」的数字地址方式，靠工程约定对齐——这意味着接入前你必须从 PLC
程序里查清每个变量落在哪个 DB、哪个字节。

在物联网四层架构里，S7 属于**网络层**的工业有线侧：它解决的是「PLC
内部的数据怎么经以太网被外部系统读到」这最后一公里。关于工业总线协议的通信模型、寻址方式与字节序权衡，见[工业总线与协议](../foundations/fieldbus)。

<PlcS7Diagram lang="zh" />

一个驱动进程可同时连多台 PLC，连接按 `deviceId` 复用，每台 PLC 由各自[设备](../introduction/concepts/device)上的 `host` 与
`plcType` 区分。

## 属性配置

S7 没有独立的「写命令属性」（`application.yml` 里没有 `command-attribute`）。所有寻址信息分两层：**驱动属性**定位「连哪台 PLC」，
**位号属性**定位「读 PLC 里的哪个变量」。下面两张表的字段、类型与默认值均来自驱动的 `application.yml`。

### 驱动属性（设备级 `driver-attribute`）

接入一台 S7 PLC 设备时，在[设备](../introduction/concepts/device)
上为这些[属性](../introduction/concepts/attribute-config)填值。`host` / `port` 共同决定连接目标，`plcType` 决定驱动用哪套
S7 寻址方案去解析 DB 地址与字节序。

| 属性       | code      | 类型     | 默认值            | 说明                  |
|----------|-----------|--------|----------------|---------------------|
| Host     | `host`    | STRING | `192.168.0.20` | PLC 的 IP 地址         |
| Port     | `port`    | INT    | `102`          | S7 TCP 端口，标准为 `102` |
| PLC Type | `plcType` | STRING | `S1200`        | PLC 型号，取值见下         |

::: tip plcType 决定地址解析方式
不同型号的 S7 PLC 在 DB 寻址细节与数据排布上有差异，`plcType` 用来选对应的 S7 寻址方案。合法取值来自底层 `EPlcType` 枚举：
`S200` / `S200_SMART` / `S300` / `S400` / `S1200` / `S1500` / `SINUMERIK_828D`。常见对应：S7-1200 填 `S1200`、S7-1500 填
`S1500`、S7-200 Smart 填 `S200_SMART`。填错或填了枚举外的值时，驱动会打印告警并回退到 `S1200`。
:::

### 位号属性（`point-attribute`）

每个[位号](../introduction/concepts/point)定位 PLC 数据块里的一个变量。驱动把这三项拼成 S7 地址字符串后交给底层库读写——非布尔位号拼成
`DB{dbNum}.{byteOffset}`，仅当位号是布尔且 `bitOffset > 0` 时才拼成 `DB{dbNum}.{byteOffset}.{bitOffset}`。

| 属性          | code         | 类型  | 默认值 | 说明                      |
|-------------|--------------|-----|-----|-------------------------|
| DB Number   | `dbNum`      | INT | `0` | 数据块号，从 0 开始计            |
| Byte Offset | `byteOffset` | INT | `0` | 数据块内的字节偏移               |
| Bit Offset  | `bitOffset`  | INT | `0` | 字节内的位偏移（仅布尔位号且 > 0 时生效） |

::: tip 位号类型决定读多少字节
读写宽度由位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）从 `byteOffset` 起决定，不需要额外配置：
`BOOLEAN` 取一个 bit，`BYTE` 1 字节，`SHORT` 2 字节，`INT`/`FLOAT` 4 字节，`LONG`/`DOUBLE` 8 字节，`STRING` 读字符串。所以同一个
DB 偏移配成不同类型的位号会读出不同宽度的值。
:::

::: warning bitOffset 只对布尔位号且非零时才生效
驱动只在「类型为布尔 **且** `bitOffset > 0`」时才走位寻址（地址带第三段 `.bit`）；其余情况——非布尔类型、或布尔但 `bitOffset=0`
——一律按 `DB{dbNum}.{byteOffset}` 做字节级寻址。给一个浮点位号填 `bitOffset` 不会报错也不起作用；要跨字节请改 `byteOffset`
，不要用 `bitOffset`「跳字节」。
:::

写位号时复用它自己的位号属性（`dbNum` / `byteOffset` / `bitOffset`）定位地址，写入宽度由命令携带的值类型决定。一个可写位号配好这三项后即可读可写，无需再额外配置写命令。

## 故障排查

接入 S7 时，下面这些是最常踩的坑，多数是 PLC 侧设置而非驱动问题。

::: danger PLC 侧必须放开 PUT/GET 访问
S7-1200/1500 默认**禁止**外部 PUT/GET 通信，这是接入失败最常见的原因。需在 TIA Portal 的 CPU 属性里勾选「允许来自远程对象的
PUT/GET 通信访问」。否则 TCP 连接能建立、但每次读写都会被 PLC 拒绝。
:::

::: warning DB 必须关闭「优化的块访问」
被读写的数据块若开启了「优化的块访问（Optimized block access）」，变量在 DB 内不再有固定字节偏移，按 `byteOffset` 定位会读到错值或失败。在
TIA Portal 里选中该 DB → 属性 → 取消勾选「优化的块访问」，编译下载后偏移地址才稳定可用。
:::

- **端口 `102` 不通**：S7 走 TCP `102`，确认 PLC IP（`host`）可达、防火墙未拦截、`port` 未被改成非标值。可先用 `ping` 与
  `telnet <host> 102` 验证链路再排查驱动。
- **`plcType` 选错导致地址解析异常**：型号填错会回退到 `S1200`，对 S7-300/400/1500 可能读出错位的值。日志里出现
  `Unknown plcType ... fallback to S1200` 即说明取值不在枚举内，按上表改对。
- **读到的数值不对（字节序 / 偏移）**：先确认 DB 已关闭优化块访问、`dbNum` 与 `byteOffset` 与 PLC 程序里的变量地址逐一对应；再确认位号
  `pointTypeFlag` 与 PLC 端变量类型宽度一致（如 PLC 是 `Real` 就用 `FLOAT`、`DInt` 用 `INT`）。
- **设备一直离线 / 频繁重连**：读或写一旦抛异常，驱动会主动作废该连接（`invalidateConnection`）并在下次访问时重建。若反复重连，多半是
  PLC 侧拒绝、网络抖动或 PLC 负载过高；结合驱动日志里的 `Driver connection failed` / `read failed`
  定位。在线状态与租约超时机制见[设备](../introduction/concepts/device)。

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`PlcS7Driver`（驱动名 `PLC S7 Driver`，类型 `DRIVER_CLIENT`）。这是稳定的路由标识，不要随意改。
- **读**：支持。默认采集 cron `0/30 * * * * ?`（每 30
  秒读一轮），逐位号按地址读取后封装为[位号值](../introduction/concepts/point-value)。
- **写**：支持。复用位号属性定位地址，按命令值类型写回 PLC。
- **订阅 / 上报**：不提供。S7 是主从轮询模型，驱动不监听 PLC 主动推送——与[驱动能力矩阵](./matrix)中「S7：读 ✓ / 写 ✓ / 订阅
  —」一致。
- **健康检查**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45` 秒。

::: info 实现状态：可用
`PlcS7DriverCustomServiceImpl` 的 `read()` / `write()` / `initial()` / `event()` / `validate()` 均已完整实现，底层基于
`iot-communication`（`S7PLC`）库，连接开启自动重连、按 `deviceId` 复用并加 `ReentrantLock` 串行化读写。这是一个可用驱动，非骨架。
:::

最小接入示例：把 IP `192.168.0.20:102` 的一台 S7-1200 接进来，读 DB1 偏移 0 处的一个 32 位浮点：

1. 选 `PLC S7 Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.0.20`、`port=102`、
   `plcType=S1200`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=FLOAT`），位号属性填 `dbNum=1`、`byteOffset=0`、`bitOffset=0`。
3. 确认 PLC 已放开 PUT/GET 且该 DB 关闭优化块访问，启动驱动，30 秒内即可在[位号值](../introduction/concepts/point-value)
   里看到采集值。

## 延伸阅读

- [驱动总览](./index) — 全部驱动的分类与选型入口
- [驱动能力矩阵](./matrix) — 各驱动读 / 写 / 订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — S7 所属网络层的通信模型、寻址与字节序
- [Melsec 驱动](./melsec) — 三菱 PLC 以太网驱动，同属工业总线 / PLC 类
