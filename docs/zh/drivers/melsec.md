---
title: Melsec 驱动
---

# Melsec 驱动

> **`dc3-driver-melsec` 把三菱（Mitsubishi）PLC 通过 MC 协议接入 IoT DC3**——按软元件地址周期性读取 PLC 内存值，并支持向软元件写值的命令。

MC 协议（MELSEC Communication）是三菱 PLC 的原生通信协议，A、QnA、Q/L、iQ-R 等系列 PLC 广泛在用。本驱动作为 MC 客户端，通过 TCP 主动连接 PLC，按[位号](../introduction/concepts/point)上配置的软元件地址采数、写值。底层基于 `iot-communication` 协议库实现，读写时按位号的数据类型自动选用对应的字宽。

- **驱动名 / code**：`Mitsubishi Melsec Driver` / `MelsecDriver`
- **类型**：`DRIVER_CLIENT`（主动连 PLC）

::: tip 先认识几个 MC 概念
**软元件（Device，亦称内存地址）**：三菱 PLC 里按用途划分的数据单元——如 `D`（数据寄存器，最常用）、`M`（内部继电器）、`X`（输入继电器）、`W`（链接寄存器）。**软元件地址**：区前缀加编号组成的一个完整地址字符串，如 `D100`、`M0`、`X10`、`W200`——区和编号写在一起，不拆成两个字段。**PLC 系列（Series）**：不同系列的 MC 帧格式略有差异，需按实际 PLC 选 `A` / `QnA` / `Q_L` / `IQ_R`。
:::

## 驱动配置（设备级 `driver-attribute`）

接入一台 Melsec PLC 时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `192.168.0.20` | Ip |
| Port | `port` | INT | `6000` | Port |
| PLC Series | `series` | STRING | `QnA` | PLC series (A/QnA/Q_L/IQ_R) |

## 位号配置（`point-attribute`）

每个[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Device Address | `address` | STRING | `D100` | Device memory address (D100, M0, X10, W200 etc.) |
| String Length | `length` | INT | `0` | String read length (0 for non-string types) |

::: tip 数据类型决定读几个字、怎么解码
驱动按位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）自动选用读写宽度：`BOOLEAN` 读位，`SHORT` 读 16 位、`INT`/`FLOAT` 读 32 位、`LONG`/`DOUBLE` 读 64 位。只有 `STRING` 类型才用到 `length`（读取的字符串长度）；非字符串位号保持 `length=0` 即可，驱动会忽略它。
:::

## 写命令配置

本驱动支持向位号写值（数值、布尔、字符串均可），但**没有单独的 `command-attribute`**——写命令复用位号上已配置的 `address`，目标软元件即位号的 `address`，写值的字宽由下发值的数据类型决定。因此可写位号无需额外配置，只要在[物模型](../introduction/concepts/profile)里把该位号设为可写即可。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义任务**：默认 cron `0/5 * * * * ?`（Melsec 驱动当前未使用自定义任务，保留该调度位）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——驱动以 TCP 连接是否存活判定在线，读写失败会主动断开并重连，在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 IP `192.168.0.30:6000` 的一台 QnA 系列三菱 PLC 接进来，采集 `D100` 的一个 16 位整数：

1. 选 `Mitsubishi Melsec Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.0.30`、`port=6000`、`series=QnA`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个[位号](../introduction/concepts/point)（`pointTypeFlag=SHORT`、`READ_ONLY`），point 属性填 `address=D100`、`length=0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到 `D100` 的采集值。

## 易错点

::: warning address 写成完整软元件地址，区和编号不拆开
`address` 直接填三菱习惯的整体写法，如 `D100`、`M0`、`X10`、`W200`——区前缀和编号写在同一个字符串里。这与按"区 + 数字偏移"两字段配置的协议（如 FINS）不同，**不要**把区和编号拆成两项填。
:::

::: warning series 必须匹配真实 PLC 系列
`series` 取值仅限 `A` / `QnA` / `Q_L` / `IQ_R`，决定 MC 帧的封装格式。填错（或填了识别不了的值）时驱动会回退到 `QnA`，对其它系列 PLC 可能读不到正确数据——接入前请按实机型号确认系列。
:::

::: tip 一个驱动实例可接多台 PLC
同一个 Melsec 驱动进程可服务多台设备，每台设备各自维护一条 TCP 连接（按设备 ID 缓存）。多台 PLC 用各自的 `host` 区分。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `address` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [FINS 驱动](./fins) — 欧姆龙 PLC 的 TCP 工业协议
