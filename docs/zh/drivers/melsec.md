---
title: Melsec 驱动
---

# Melsec 驱动

`dc3-driver-melsec` 把三菱（Mitsubishi）PLC 通过 MC 协议接入 IoT DC3：作为 MC 客户端主动连上
PLC，按[位号](../introduction/concepts/point)上配置的软元件地址周期性采数，并支持向软元件写值的命令。读完本页你能配出
driver / point 属性、把一台三菱 PLC 接进平台，并知道接不上时从哪查起。

## 协议背景

MC 协议（MELSEC Communication）是三菱电机 PLC 的原生通信协议，A、QnA、Q/L、iQ-R 等系列广泛在用。在工业现场，三菱 PLC 通过以太网模块或
CPU 内置网口开放一个 MC 服务端口，上位机以 MC 客户端身份连上来，按**软元件地址**（如 `D100`、`M0`）读写 PLC
内存里的数据单元——数据寄存器存工艺参数、内部继电器存逻辑状态、输入/输出继电器映射现场 IO。

在物联网四层架构里，MC 协议属于**网络层**的工业有线侧：它是 PLC 与上位系统之间"最后一公里"
的语言规约，规定字节怎么排、软元件怎么寻址、一问一答的请求-响应时序。它和西门子 S7、欧姆龙 FINS 一样，都是厂商私有的主从协议——主站不问，PLC
不会主动上报。要理解它在协议谱系里的位置、以及为什么各家 PLC
协议互不兼容，见[物联网网络层：工业总线与协议](../foundations/fieldbus)。

本驱动底层基于 `iot-communication` 协议库的 `McPLC` 实现，读写时按位号的数据类型自动选用对应的字宽与编解码方式。

- **驱动名 / code**：`Mitsubishi Melsec Driver` / `MelsecDriver`
- **类型**：`DRIVER_CLIENT`（主动连 PLC）

::: tip 先认识几个 MC 概念
**软元件（Device，亦称内存地址）**：三菱 PLC 里按用途划分的数据单元——如 `D`（数据寄存器，最常用）、`M`（内部继电器）、`X`（输入继电器）、
`W`（链接寄存器）。**软元件地址**：区前缀加编号组成的一个完整地址字符串，如 `D100`、`M0`、`X10`、`W200`——区和编号写在一起，不拆成两个字段。
**PLC 系列（Series）**：不同系列的 MC 帧封装格式略有差异，需按实际 PLC 选 `A` / `QnA` / `Q_L` / `IQ_R`。
:::

## 属性配置

Melsec 驱动的配置分两层：**driver 属性**描述"连哪台 PLC"（设备级，一台设备一份），**point 属性**描述"读哪个软元件"
（位号级，一个位号一份）。两层属性都来自驱动 `application.yml` 的声明，接入时在控制台对应填值。

### 驱动属性（设备级 `driver-attribute`）

`host` / `port` 决定 TCP 连到哪台 PLC 的哪个 MC 服务端口；`series` 决定 MC 帧用哪种封装格式，必须与真实 PLC 系列对得上。接入一台
Melsec PLC 时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性         | code     | 类型     | 默认值            | 说明                            |
|------------|----------|--------|----------------|-------------------------------|
| Host       | `host`   | STRING | `192.168.0.20` | PLC 主机地址（Ip）                  |
| Port       | `port`   | INT    | `6000`         | MC 服务端口                       |
| PLC Series | `series` | STRING | `QnA`          | PLC 系列，`A`/`QnA`/`Q_L`/`IQ_R` |

### 位号属性（`point-attribute`）

`address` 指定要读写的软元件（整体写法，区+编号同写一串）；`length`
只在字符串类型时用到——读取的字符串字节长度。每个[位号](../introduction/concepts/point)上填：

| 属性             | code      | 类型     | 默认值    | 说明                                |
|----------------|-----------|--------|--------|-----------------------------------|
| Device Address | `address` | STRING | `D100` | 软元件地址（`D100`、`M0`、`X10`、`W200` 等） |
| String Length  | `length`  | INT    | `0`    | 字符串读取长度（非字符串类型填 `0`）              |

::: tip 数据类型决定读几个字、怎么解码
驱动按位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）自动选用读写宽度与编解码方式：`BOOLEAN`
读/写一个位，`BYTE` 读/写 8 位，`SHORT` 读/写 16 位（int16），`INT`/`FLOAT` 读/写 32 位，`LONG`/`DOUBLE` 读/写 64 位，`STRING`
读/写字符串。只有 `STRING` 类型才用到 `length`（读取的字符串字节数，**填 0 或留空时驱动按 64 处理**）；非字符串位号保持
`length=0` 即可，驱动会忽略它。
:::

### 写命令：复用位号 `address`，无单独属性

本驱动支持向位号写值（数值、布尔、字符串均可），但**没有单独的 `command-attribute`**——写命令复用位号上已配置的 `address`
，目标软元件即位号的 `address`
，写值的字宽由下发值的数据类型决定。因此可写位号无需额外配置，只要在[模板](../introduction/concepts/profile)里把该位号设为可写即可。

### 采集与健康调度

这些 cron 来自 `application.yml` 的 `schedule` / `health` 段，决定采集节奏与在线判定：

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义任务**：默认 cron `0/5 * * * * ?`（Melsec 驱动当前未使用自定义任务，`schedule()` 为空实现，保留该调度位）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——驱动以 TCP
  连接是否存活判定在线，读写抛异常会主动断开并从连接缓存移除该连接，下一轮采集自动重连。在线状态机制见[设备](../introduction/concepts/device)。

## 故障排查

::: warning address 写成完整软元件地址，区和编号不拆开
`address` 直接填三菱习惯的整体写法，如 `D100`、`M0`、`X10`、`W200`——区前缀和编号写在同一个字符串里。这与按"区 + 数字偏移"
两字段配置的协议（如 [FINS](./fins)）不同，**不要**把区和编号拆成两项填，否则 `McPLC` 会按非法地址解析而读写失败。
:::

::: warning series 必须匹配真实 PLC 系列
`series` 取值仅限 `A` / `QnA` / `Q_L` / `IQ_R`，决定 MC 帧的封装格式。填错或填了识别不了的值时，驱动会打印
`Unknown series ... fallback to QnA` 并回退到 `QnA`——对其它系列 PLC 可能读不到正确数据或直接报错。接入前请按实机型号确认系列。
:::

::: warning 连不上 / 读写失败时优先查 PLC 侧 MC 服务
驱动用 `host:port` 直接发起 TCP 连接，建连失败会抛 `Driver connection failed`。常见原因：PLC 以太网模块未启用 MC 服务、端口与
`port` 不一致（不少机型默认并非 `6000`）、PLC 侧 IP 过滤或连接数已满、防火墙拦截。先用 `telnet host port` 确认端口可达，再核对
PLC 工程里 MC 服务的端口与协议（TCP）设置。
:::

::: warning 字符串/字节序读出来不对，检查类型与 length
读出的字符串乱码或截断，多半是 `length` 与 PLC 侧实际字符串字节数不匹配（留 0 时驱动按 64 读）。数值类型读出来异常，先确认位号的
`pointTypeFlag` 与软元件里实际存的字宽一致——把一个 32 位浮点软元件配成 `SHORT` 只会读到低 16 位。
:::

::: tip 一个驱动实例可接多台 PLC，连接按设备缓存
同一个 Melsec 驱动进程可服务多台设备，每台设备各自维护一条 `McPLC` 连接（按设备 ID 缓存在 `connectMap` 里），并用各自的
`ReentrantLock` 串行化读写。多台 PLC 用各自的 `host` 区分。设备被更新或删除时，驱动会收到元数据事件并关闭、移除对应连接。
:::

## 在 IoT DC3 中如何落地

::: info 实现状态：可用
Melsec 驱动的读、写路径均已完整实现——`read()` / `write()` 通过 `iot-communication` 的 `McPLC` 调用真实的 MC 读写（
`readInt16` / `writeInt16` / `readString` 等），按数据类型选用正确字宽。这是一个可用驱动，行为与 `application.yml` 声明一致。
:::

- **dc3.driver.code**：`MelsecDriver`——驱动在平台内的稳定路由标识，设备绑定驱动、消息分发都按它寻址，不要随意更改。
- **读能力**：✓ 支持。按采集周期周期性读取，覆盖 `BOOLEAN`/`BYTE`/`SHORT`/`INT`/`LONG`/`FLOAT`/`DOUBLE`/`STRING`
  全部类型，与[驱动能力矩阵](./matrix)中 MELSEC 的"读 ✓"一致。
- **写能力**：✓ 支持。下发写命令时按值的数据类型写对应字宽，与能力矩阵"写 ✓"一致。
- **订阅能力**：— 不支持。MC 协议是主从轮询模型，本驱动靠采集周期轮询取值，不提供 PLC 主动上报式订阅，与能力矩阵"订阅 —"一致。

**最小接入示例**：把 IP `192.168.0.30:6000` 的一台 QnA 系列三菱 PLC 接进来，采集 `D100` 的一个 16 位整数：

1. 选 `Mitsubishi Melsec Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.0.30`、
   `port=6000`、`series=QnA`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个[位号](../introduction/concepts/point)（
   `pointTypeFlag=SHORT`、`READ_ONLY`），point 属性填 `address=D100`、`length=0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到 `D100` 的采集值。

完整的接入流程（建模、绑定、下发）见[设备接入](../operation/device-onboarding)。

## 延伸阅读

- [驱动总览](./index) — 全部驱动与分类入口
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力速查
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — MC 协议在网络层协议谱系里的位置
- [FINS 驱动](./fins) — 欧姆龙 PLC 的 TCP 工业协议，按"区 + 偏移"两字段寻址
