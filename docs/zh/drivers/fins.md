---
title: FINS 驱动
---

# FINS 驱动

> **`dc3-driver-fins` 把欧姆龙（Omron）PLC 通过 FINS 协议接入 IoT DC3**——按字地址周期性读取 PLC 内存区数值，并支持向内存区写值的命令。

FINS（Factory Interface Network Service）是欧姆龙 PLC 的原生通信协议，CP/CJ/CS 等系列 PLC 广泛在用。本驱动作为 FINS 客户端，通过 TCP 主动连接 PLC，按[位号](../introduction/concepts/point)上配置的内存区与字地址采数、写值。驱动内部手工拼装 FINS 帧（不依赖第三方协议库），支持 D、W、H、C 四种内存区。

- **驱动名 / code**：`Omron FINS Driver` / `FinsDriver`
- **类型**：`DRIVER_CLIENT`（主动连 PLC）

::: tip 先认识几个 FINS 概念
**内存区（Memory Area）**：PLC 里按用途划分的数据区——`D`（数据存储区，最常用）、`W`（工作区）、`H`（保持区）、`C`（计数器区）。**字地址（Word Address）**：内存区内以"字"（16 位）为单位的偏移，如 `D100` 就是 D 区第 100 个字。**节点/单元号（Node/Unit）**：FINS 网络里寻址 PLC 用的源/目的地址，单台直连场景一般保持默认。
:::

## 驱动配置（设备级 `driver-attribute`）

接入一台 FINS PLC 时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `127.0.0.1` | PLC 主机地址 |
| Port | `port` | INT | `9600` | FINS 端口（标准 9600）|
| Protocol | `protocol` | STRING | `TCP` | 传输协议 |
| Source Node | `sourceNode` | INT | `1` | FINS 源节点号 |
| Dest Node | `destNode` | INT | `2` | FINS 目的节点号 |
| Source Unit | `sourceUnit` | INT | `0` | FINS 源单元号 |
| Dest Unit | `destUnit` | INT | `0` | FINS 目的单元号 |
| Timeout | `timeout` | INT | `5000` | 请求超时（毫秒）|

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Memory Area | `memoryArea` | STRING | `D` | 内存区，`D`/`W`/`H`/`C` |
| Address | `address` | INT | `0` | 内存区内的字地址 |
| Data Type | `dataType` | STRING | `UINT16` | `INT16`/`UINT16`/`INT32`/`UINT32`/`FLOAT`/`STRING`/`BCD` |
| Bit Position | `bitPosition` | INT | `0` | 字内位偏移（当前读路径未使用，固定按 `0` 处理）|

::: warning 当前读路径只支持 16 位类型，`dataType` 不决定读几个字
读取时固定只读 **1 个字（2 字节）**，与 `dataType` 无关。因此**只有 `INT16`/`UINT16` 能正确读出**；`INT32`/`UINT32`/`FLOAT`/`STRING`/`BCD` 是协议语义、实现待补——它们需要 4 字节或更多，按 2 字节大端序解码会触发 BufferUnderflow，当前实际读不出值。`STRING`/`BCD` 同理只会拿到 2 字节。驱动按大端序（Big-Endian）解码读回的字节；位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）应与这里的 `dataType` 对得上。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Memory Area | `memoryArea` | STRING | `D` | 内存区，`D`/`W`/`H`/`C` |
| Address | `address` | INT | `0` | 内存区内的字地址 |
| Data Type | `dataType` | STRING | `UINT16` | 写值数据类型 |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义任务**：默认 cron `0/5 * * * * ?`（FINS 驱动当前未使用自定义任务，保留该调度位）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——驱动以 TCP 连接是否存活判定在线，在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 IP `192.168.1.20:9600` 的一台欧姆龙 PLC 接进来，采集 `D100` 的一个 16 位整数：

1. 选 `Omron FINS Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.20`、`port=9600`，其余（`protocol`、节点/单元号、`timeout`）保持默认。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个[位号](../introduction/concepts/point)（`pointTypeFlag=INT`、`READ_ONLY`），point 属性填 `memoryArea=D`、`address=100`、`dataType=INT16`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到 `D100` 的采集值。

## 易错点

::: warning address 是字地址，不是带区前缀的写法
`address` 只填内存区内的数字偏移。要读欧姆龙习惯写法的 `D100`，应填 `memoryArea=D`、`address=100`，**不要**把 `D100` 整体填进 `address`。区由 `memoryArea` 单独指定。
:::

::: warning 32 位写值当前按整数编码
写命令对 `INT32`/`UINT32`/`FLOAT` 都按 `Integer.parseInt` 解析后写 4 字节大端整数。也就是说写 `FLOAT` 时传入的是整数形式的位模式，而非 `12.5` 这样的小数文本——写浮点前请确认下发值的格式与 PLC 侧期望一致。
:::

::: tip 一个驱动实例可接多台 PLC
同一个 FINS 驱动进程可服务多台设备，每台设备各自维护一条 TCP 连接（按设备 ID 缓存）。多台 PLC 用各自的 `host`、`destNode` 区分。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `memoryArea` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 另一种常见的 TCP 工业协议
