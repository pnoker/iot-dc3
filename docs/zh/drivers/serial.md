---
title: 串口 驱动
---

# 串口 驱动

> **`dc3-driver-serial` 把 RS232/RS485/RS422 串口设备接入 IoT DC3**——按[位号](../introduction/concepts/point)上配的 HEX 指令周期性发送、读回原始字节，再按帧头/帧尾、校验、数据偏移与格式解析成值，并支持向设备写值的命令。

串口（Serial）是工业现场最朴素也最通用的连接方式，RS232 点对点、RS485/RS422 总线广泛用于仪表、变送器、扫码枪、PLC 串口模块等。很多设备并不跑标准协议，而是"发一串字节、回一串字节"的私有报文。本驱动作为串口主站，通过 jSerialComm 打开一个串口、按设备缓存一条连接，逐个位号发送你配置的 HEX 指令，再把回包按帧结构拆出数据区、按指定格式（HEX/ASCII/BINARY/FLOAT）解码。

- **驱动名 / code**：`Serial Port Driver` / `SerialDriver`
- **类型**：`DRIVER_CLIENT`（主动打开串口、轮询设备）

::: tip 先认识两个词
**HEX 指令**：用十六进制书写的字节串，如 `01 03 00 00 00 0A C5 CD`，空格只为可读，对应实际发送的字节。**帧（Frame）**：设备回复的一整段字节，通常由帧头、数据区、可选校验、帧尾拼成；本驱动靠帧头/帧尾与偏移/长度从回包里"切"出真正的数据区。
:::

## 驱动配置（设备级 `driver-attribute`）

接入一台串口设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)，它们决定串口怎么打开（线路参数）：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Serial Port | `port` | STRING | `/dev/ttyUSB0` | 串口设备路径 |
| Baud Rate | `baudRate` | INT | `9600` | 波特率（1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200）|
| Data Bits | `dataBits` | INT | `8` | 数据位（5, 6, 7, 8）|
| Stop Bits | `stopBits` | INT | `1` | 停止位（1, 2）|
| Parity | `parity` | INT | `0` | 校验位（0=无, 1=奇, 2=偶）|
| Timeout | `timeout` | INT | `1000` | 读超时（毫秒）|

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：发什么、回包怎么拆、按什么格式解码。

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | （空）| 要发送的 HEX 指令（如 `01 03 00 00 00 0A C5 CD`）|
| Receive Length | `receiveLength` | INT | `0` | 预期回包字节数（0=自动判断）|
| Frame Header | `frameHeader` | STRING | （空）| HEX 帧头（如 `01 03`）|
| Frame Footer | `frameFooter` | STRING | （空）| HEX 帧尾（如 `0D 0A`）|
| Data Offset | `dataOffset` | INT | `0` | 数据区相对帧起始的偏移 |
| Data Length | `dataLength` | INT | `0` | 数据区字节长度（0=取到帧尾）|
| Checksum Type | `checksumType` | STRING | `NONE` | 校验类型：NONE, CRC16, XOR |
| Data Format | `dataFormat` | STRING | `HEX` | 数据格式：HEX, ASCII, BINARY, FLOAT |
| Byte Order | `byteOrder` | STRING | `BIG` | 字节序：BIG, LITTLE |

::: tip 解析顺序：先切帧、再解码
驱动收到回包后，先用 `frameHeader`/`frameFooter` 定位帧，按 `dataOffset`/`dataLength` 切出数据区并按 `checksumType` 校验，再按 `dataFormat` + `byteOrder` 解码成位号值。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）应与 `dataFormat` 对得上：2/4/8 字节的数会按 `byteOrder` 拼为整数或浮点，`FLOAT` 用 4 字节（单精度）或 8 字节（双精度）。
:::

## 写命令配置（`command-attribute`）

可写位号在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | `${value}` | 带 `${value}` 占位符的 HEX 指令模板 |
| Byte Order | `byteOrder` | STRING | `BIG` | 编码写入值的字节序：BIG, LITTLE |

写入时，驱动把命令参数渲染进 `sendCommand` 模板的 `${value}` 位置，转成字节后整帧发出。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒轮询一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`；驱动以"串口是否已打开"判定设备在线——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

接入一台挂在 `/dev/ttyUSB0`、9600-8-N-1 的 Modbus RTU 风格温度变送器（从站 1，读保持寄存器，回包 `01 03 02 <数据 2 字节> <CRC 2 字节>`）：

1. 选 `Serial Port Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `port=/dev/ttyUSB0`、`baudRate=9600`、`dataBits=8`、`stopBits=1`、`parity=0`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`），point 属性填 `sendCommand=01 03 00 00 00 01 84 0A`、`frameHeader=01 03`、`dataOffset=3`、`dataLength=2`、`dataFormat=BINARY`、`byteOrder=BIG`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning sendCommand 的 CRC 要自己算对
本驱动不会替你补 Modbus CRC——`sendCommand` 是原样发送的整条字节串，校验字节必须由你算好写进去（上例 `84 0A` 即对应 CRC）。CRC 算错，设备要么不回包，要么回异常帧。`checksumType` 只用于校验**回包**，不会改写你发出的指令。
:::

::: warning dataOffset 从帧起始算，别把帧头也当成数据
`dataOffset`/`dataLength` 是相对整帧起始的字节偏移，不是相对数据区。Modbus RTU 回包 `01 03 02 ...` 的有效数据从第 4 字节开始，故 `dataOffset=3`、`dataLength=2`。漏算帧头会把地址码/功能码当成数据解出来。
:::

::: tip 一个驱动实例可接多台串口设备
同一驱动进程按设备缓存独立的串口连接，可同时服务挂在不同 `port` 的多台设备。但同一物理串口（RS485 总线）上的多台设备需各自建一台[设备](../introduction/concepts/device)、用各自的 `sendCommand`（含不同站号）区分，驱动按位号顺序轮询发指令。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `port` / `sendCommand` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus RTU 驱动](./modbus-rtu) — 跑标准 Modbus 协议的串口驱动
