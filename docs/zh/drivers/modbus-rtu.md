---
title: Modbus RTU 驱动
---

# Modbus RTU 驱动

> **`dc3-driver-modbus-rtu` 把 Modbus RTU 从站设备接入 IoT DC3**——通过串口连到从站，周期性读取线圈/寄存器值，并支持向线圈和保持寄存器写值的命令。

Modbus RTU 是 Modbus 协议的串行链路变体，跑在 RS-485/RS-232 总线上，是工业现场最常见的协议之一（PLC、电表、变频器、传感器大量在用）。它与 [Modbus TCP 驱动](./modbus-tcp)的功能码、地址模型完全一致，区别只在于物理层：RTU 走串口（波特率、数据位、校验位等），而不是 IP/端口。本驱动作为 Modbus 主站（master），通过一个串口连到挂在同一条总线上的一台或多台从站，按[位号](../introduction/concepts/point)上配置的功能码与地址采数、写值。

- **驱动名 / code**：`Modbus RTU Driver` / `ModbusRtuDriver`
- **类型**：`DRIVER_CLIENT`（主动连从站）

## 驱动配置（设备级 `driver-attribute`）

接入一台 Modbus RTU 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。这些是串口参数，必须与从站的串口设置完全一致，否则连不上：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Port | `port` | STRING | `/dev/ttyUSB0` | 串口名（如 /dev/ttyUSB0、COM3）|
| Baud Rate | `baudRate` | INT | `9600` | 串口波特率（如 9600、19200、115200）|
| Data Bits | `dataBits` | INT | `8` | 数据位（7 或 8）|
| Stop Bits | `stopBits` | INT | `1` | 停止位（1 或 2）|
| Parity | `parity` | INT | `0` | 校验位（0=无, 1=奇, 2=偶, 3=Mark, 4=Space）|

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus 从站单元号 |
| Function Code | `functionCode` | INT | `1` | 读功能码 `[1, 2, 3, 4]` |
| Offset | `offset` | INT | `0` | 寄存器/线圈地址偏移 |

::: tip 功能码决定读什么
读取用 `01`（线圈）/ `02`（离散输入）/ `03`（保持寄存器）/ `04`（输入寄存器）。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要和功能码取回的数据宽度对得上——驱动按位号类型决定寄存器宽度：`LONG`→4 字节整型、`FLOAT`→4 字节浮点、`DOUBLE`→8 字节浮点，其余按 2 字节整型。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus 从站单元号 |
| Function Code | `functionCode` | INT | `6` | 写功能码 `[5, 6, 15, 16]` |
| Offset | `offset` | INT | `0` | 寄存器/线圈地址偏移 |
| Value Template | `valueTemplate` | STRING | `${value}` | 写值模板，用命令参数渲染 |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把接在 `/dev/ttyUSB0`、波特率 `9600`、单元号 `1` 的一台 Modbus RTU 从站接进来：

1. 选 `Modbus RTU Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `port=/dev/ttyUSB0`、`baudRate=9600`、`dataBits=8`、`stopBits=1`、`parity=0`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `slaveId=1`、`functionCode=3`、`offset=0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning 串口参数错一项就连不上
`port`、`baudRate`、`dataBits`、`stopBits`、`parity` 五项必须和从站串口设置逐一对上（RTU 不像 TCP 有握手协商，参数不匹配只会读到乱码或超时）。连续 3 次连接失败后驱动会进入 60 秒退避，期间设备一律按离线上报——修好串口参数或线缆后需等退避结束才会重连。
:::

::: tip 一条总线上的多台从站共用一个串口
RS-485 总线上可挂多台从站，它们共用同一个 `port`，由位号的 `slaveId` 区分。驱动对每台[设备](../introduction/concepts/device)维护一条独立串口连接、按设备区分，所以请把同一条物理总线上的从站都用相同的 `port`，靠 `slaveId` 寻址，不要给每个从站配不同串口。
:::

::: warning 写命令只支持线圈和保持寄存器
写功能码默认 `6`，但驱动实际只处理 `01`（写线圈）与 `03`（写保持寄存器）两类目标；填 `15`/`16` 等其它写功能码会被忽略、写入失败。`offset` 与读取一样是协议层 0 基偏移，不是 `40001` 这种习惯写法。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `port` / `slaveId` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 以太网版 Modbus
