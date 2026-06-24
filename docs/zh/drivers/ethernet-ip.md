---
title: EtherNet/IP 驱动
---

# EtherNet/IP 驱动

> **`dc3-driver-ethernet-ip` 把 EtherNet/IP（CIP）PLC 接入 IoT DC3**——以标签（Tag）为目标，周期性读取 PLC 标签值，并支持向标签写值的命令。

EtherNet/IP 是工业以太网协议，承载 CIP（Common Industrial Protocol，通用工业协议），主要用于罗克韦尔 Allen-Bradley 系列 PLC（如 ControlLogix / CompactLogix）。与 Modbus 这类按寄存器地址寻址的协议不同，CIP 是**按标签名（Tag Name）寻址**的：PLC 里的变量有名字（如 `Motor_Speed`），驱动通过 CIP 的 Data Table Read/Write 服务按名字读写，无需关心物理地址。

本驱动作为 EtherNet/IP 客户端，通过裸 TCP 套接字（默认端口 44818）连到 PLC，按[位号](../introduction/concepts/point)上配置的标签名采数、写值。

::: warning 当前为骨架实现（Work in progress）
本驱动是协议骨架：CIP 会话建立（RegisterSession / ForwardOpen）与封装帧仍是占位实现，源码方法体带有 TODO 标记，`health()` 仅检查缓存的套接字连接状态而非真实协议探测。请将它作为接入起点模板，而非生产可用驱动。
:::

- **驱动名 / code**：`EtherNet/IP Driver` / `EthernetIpDriver`
- **类型**：`DRIVER_CLIENT`（主动连 PLC）

## 驱动配置（设备级 `driver-attribute`）

接入一台 EtherNet/IP PLC 时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | PLC 主机地址 |
| Port | `port` | INT | `44818` | EtherNet/IP TCP 端口（标准 44818）|
| Slot | `slot` | INT | `0` | PLC 背板槽位号 |
| Timeout | `timeout` | INT | `5000` | 请求超时（毫秒）|

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Tag Name | `tagName` | STRING | （空）| CIP 标签名，如 `Motor_Speed` |
| Tag Type | `tagType` | STRING | `DINT` | 标签数据类型：`BOOL` / `SINT` / `INT` / `DINT` / `REAL` / `STRING` |
| Element Count | `elementCount` | INT | `1` | 读取的元素个数（数组标签用）；**当前实现未消费**，读请求中元素个数被硬编码为 `1` |

::: tip Tag Type 决定如何解码字节
驱动按 `tagType` 把 PLC 返回的原始字节解析成对应类型：`BOOL` 1 字节、`SINT` 1 字节、`INT` 2 字节、`DINT` 4 字节整数、`REAL` 4 字节浮点、`STRING` ASCII 文本。`tagType` 要和 PLC 里该标签的实际类型一致，否则解析出错。位号自身的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）应与之匹配。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | `${value}` | 写值模板，用命令参数渲染；写入时按位号的 `tagType` 编码。**当前实现未消费**，写入直接取命令传入的值，未走该模板 |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **自定义调度**：`schedule.custom` 已启用（cron `0/5 * * * * ?`），但当前 `schedule()` 方法体为空，不执行任何自定义逻辑。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 IP `192.168.1.20:44818`、槽位 0 的一台 Allen-Bradley PLC 接进来：

1. 选 `EtherNet/IP Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.20`、`port=44818`、`slot=0`、`timeout=5000`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个转速[位号](../introduction/concepts/point)（`pointTypeFlag=INT`、`READ_ONLY`），point 属性填 `tagName=Motor_Speed`、`tagType=DINT`、`elementCount=1`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning tagName 是 PLC 里的标签名，不是地址
CIP 按名字寻址。`tagName` 必须与 PLC 程序中定义的变量名**逐字一致**（区分大小写）。填错名字会读取失败而不是读到错误地址——这点和 Modbus 的 `offset` 不同：Modbus 填错偏移会静默读到别的寄存器，而 CIP 标签名不存在会直接报错。
:::

::: tip tagType 必须与 PLC 标签真实类型一致
驱动不会自动探测 PLC 标签类型，完全按你填的 `tagType` 解码字节。把一个 `REAL`（浮点）标签配成 `DINT`，会把浮点的字节当整数解析，得到一个无意义的大数。接入前先在 PLC 工程里确认每个标签的实际类型。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `tagName` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 按寄存器地址寻址的 Modbus
