---
title: Modbus RTU 驱动
---

# Modbus RTU 驱动

`dc3-driver-modbus-rtu` 把 Modbus RTU 从站设备接入 IoT DC3：作为主站（master）经一个串口连到挂在 RS-485/RS-232
总线上的从站，周期性读取线圈/寄存器值，并支持向线圈和保持寄存器写值的命令。读完本页你能为一台串口 Modbus
设备配好串口参数、功能码与地址，并知道连不上时从哪儿查。

## 协议背景

Modbus 是 1979 年为 PLC 串口通信而生的工业协议，至今仍是现场最常见的协议之一，PLC、电表、变频器、温控仪、传感器大量在用。*
*Modbus RTU** 是它的串行链路变体：报文以紧凑的二进制帧跑在 RS-485/RS-232 总线上，靠 CRC
校验保证完整性。它与 [Modbus TCP 驱动](./modbus-tcp)的功能码、地址模型完全一致——读用 `01/02/03/04`、写用 `05/06/15/16`，地址都是
0 基偏移——区别只在物理层：RTU 走串口（波特率、数据位、校验位、停止位），而不是 IP/端口。

在物联网四层架构里，Modbus RTU 属于**网络层**：它解决的是现场设备与采集主站之间"用哪种信号、按什么规则交换字节"的问题。它是典型的
**主从（master/slave）请求-响应**协议——主站轮流向从站发请求、等应答，从站不会主动说话；一条 RS-485 总线上可挂多台从站，由从站单元号（
`slaveId`）区分。协议本身往往只搬运字节，**怎么解释这串字节由配置决定**（16 位整数还是 32
位浮点、低字节在前还是高字节在前），这也是现场最常见的坑。关于寻址、字节序、轮询模型的通用背景，见[物联网网络层：工业总线与协议](../foundations/fieldbus)。

- **驱动名 / code**：`Modbus RTU Driver` / `ModbusRtuDriver`
- **类型**：`DRIVER_CLIENT`（主动连从站）
- **底层库**：modbus4j + jSerialComm（每台设备一条独立串口连接）

## 属性配置

Modbus RTU 的配置分三层：**驱动属性**（`driver-attribute`，设备级）描述这条串口怎么开；**位号属性**（`point-attribute`
）描述每个采集点读哪台从站、哪个寄存器；**命令属性**（`command-attribute`）描述可写位号往哪儿写。下面三张表的取值默认值与说明，均来自驱动
`application.yml`，属性的三层来历见[属性与配置](../introduction/concepts/attribute-config)。

### 驱动属性（设备级 `driver-attribute`）

接入一台 Modbus RTU 设备时，在[设备](../introduction/concepts/device)上填这五项串口参数。它们会被原样传给 jSerialComm 的
`setComPortParameters` 打开串口，因此**必须与从站的串口设置逐一对上**——RTU 不像 TCP 有握手协商，参数不匹配只会读到乱码或超时：

| 属性        | code       | 类型     | 默认值            | 说明                                  |
|-----------|------------|--------|----------------|-------------------------------------|
| Port      | `port`     | STRING | `/dev/ttyUSB0` | 串口名（如 /dev/ttyUSB0、COM3）            |
| Baud Rate | `baudRate` | INT    | `9600`         | 串口波特率（如 9600、19200、115200）          |
| Data Bits | `dataBits` | INT    | `8`            | 数据位（7 或 8）                          |
| Stop Bits | `stopBits` | INT    | `1`            | 停止位（1 或 2）                          |
| Parity    | `parity`   | INT    | `0`            | 校验位（0=无, 1=奇, 2=偶, 3=Mark, 4=Space） |

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填三项，确定"读哪台从站的哪个地址、用哪个读功能码"：

| 属性            | code           | 类型  | 默认值 | 说明                  |
|---------------|----------------|-----|-----|---------------------|
| Slave ID      | `slaveId`      | INT | `1` | Modbus 从站单元号        |
| Function Code | `functionCode` | INT | `1` | 读功能码 `[1, 2, 3, 4]` |
| Offset        | `offset`       | INT | `0` | 寄存器/线圈地址偏移（0 基）     |

::: tip 功能码决定读什么，位号类型决定怎么拼字节
读取支持四个功能码：`01`（线圈）/ `02`（离散输入）/ `03`（保持寄存器）/ `04`（输入寄存器）。对寄存器类（`03`/`04`
），驱动按位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）决定取几个寄存器、怎么解释：`LONG`→4
字节有符号整型、`FLOAT`→4 字节浮点、`DOUBLE`→8 字节浮点，其余按 2 字节有符号整型。位号类型配错，浮点会读成一个无意义的大数。
:::

### 命令属性（`command-attribute`）

可写位号还需在写命令上填四项。`valueTemplate` 是写值模板，用命令参数渲染后再下发：

| 属性             | code            | 类型     | 默认值        | 说明                               |
|----------------|-----------------|--------|------------|----------------------------------|
| Slave ID       | `slaveId`       | INT    | `1`        | Modbus 从站单元号                     |
| Function Code  | `functionCode`  | INT    | `6`        | 写功能码（驱动实际只处理 `1` 写线圈、`3` 写保持寄存器） |
| Offset         | `offset`        | INT    | `0`        | 寄存器/线圈地址偏移（0 基）                  |
| Value Template | `valueTemplate` | STRING | `${value}` | 写值模板，用命令参数渲染                     |

### 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮全部位号）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`
  ——驱动据串口连接是否已初始化判定设备在线/离线，在线状态机制见[设备](../introduction/concepts/device)。

一次接入的最小路径：用 `Modbus RTU Driver` 建[设备](../introduction/concepts/device)，driver 属性填 `port=/dev/ttyUSB0`、
`baudRate=9600`、`dataBits=8`、`stopBits=1`、`parity=0`；给绑定的[模板](../introduction/concepts/profile)
加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `slaveId=1`、
`functionCode=3`、`offset=0`；启动驱动后 30 秒内即可在[位号值](../introduction/concepts/point-value)里看到采集值。

## 故障排查

::: warning 串口参数错一项就连不上
`port`、`baudRate`、`dataBits`、`stopBits`、`parity` 五项必须和从站串口设置逐一对上。RTU
没有握手协商，任一项不匹配只会读到乱码或超时，而不会给出明确报错。先用万用表/串口助手确认线缆与波特率，再核对校验位（很多电表默认偶校验
`2`，不是默认的无校验 `0`）。
:::

::: warning 连续 3 次失败进入 60 秒退避，期间一律离线
驱动对每台设备维护连续失败计数：**连续 3 次连接失败后进入 60 秒退避**（`FAILURE_BACKOFF_THRESHOLD=3`、
`FAILURE_BACKOFF_MS=60000`
），退避期内健康检查直接上报离线、不再尝试连接。也就是说，即便你已修好串口参数或线缆，也要等退避结束才会重连——别在退避窗口内反复改配置后误判"
还是连不上"。
:::

::: warning offset 是 0 基协议地址，不是 40001
`offset` 是协议层的 0 基偏移。按 Modbus 习惯写法读"保持寄存器 40001"，应填 `functionCode=3`、`offset=0`（第 2 个保持寄存器是
`offset=1`，以此类推）。把 `40001` 直接填进 `offset` 会读到错误地址或越界。
:::

::: warning 字节序：32 位浮点读成大数多半是寄存器顺序反了
一个 32 位 `FLOAT`/`LONG` 跨两个 16 位寄存器，寄存器先后顺序在现场有 ABCD/CDAB/BADC/DCBA 四种排法。本驱动按 modbus4j
默认顺序解释；若读出的浮点是个无意义的大数，多半是从站用了相反的寄存器顺序——需要在从站侧调整字节序设置，或改用整型按原始寄存器值在上层换算。
:::

::: tip 多台从站共用一个串口，靠 slaveId 寻址
RS-485 总线上可挂多台从站，它们共用同一个 `port`，由位号的 `slaveId` 区分。驱动按[设备](../introduction/concepts/device) ID
维护连接（`connectMap`），同一物理总线上的从站应建为指向**相同 `port`** 的设备、靠 `slaveId` 寻址，不要给每个从站配不同串口。注意：串口是独占资源，确保该
`port` 没有被其它进程占用（Linux 上还需运行用户对 `/dev/ttyUSB*` 有读写权限）。
:::

## 在 IoT DC3 中如何落地

无论底层是哪种协议，落到平台都收敛为同一个[位号 Point](../introduction/concepts/point)
的[位号值 PointValue](../introduction/concepts/point-value)。Modbus RTU 驱动以 `dc3.driver.code = ModbusRtuDriver`
注册，这是稳定的路由标识，平台据此把读/写命令分发到本驱动。

按[驱动能力矩阵](./matrix)，本驱动的能力为：

| 能力 | 支持 | 实现说明                                    |
|----|----|-----------------------------------------|
| 读  | ✓  | 功能码 `01/02/03/04`，覆盖线圈、离散输入、保持寄存器、输入寄存器 |
| 写  | ✓  | **仅** `01`（写线圈）与 `03`（写保持寄存器）两类目标       |
| 订阅 | —  | 主从轮询协议，无设备主动上报，靠采集周期定时读                 |

::: info 实现状态：可用
`ModbusRtuDriverCustomServiceImpl` 的 `read()`/`write()`/`health()`/连接管理均已完整实现（基于 modbus4j +
jSerialComm），非骨架。读路径支持全部四个读功能码；连接、退避、健康判定、元数据事件（设备更新或删除时销毁旧连接）齐备。
:::

::: warning 写命令只支持线圈和保持寄存器
命令属性 `functionCode` 默认 `6`，但 `write()` 实际只处理 `01`（写线圈）与 `03`（写保持寄存器）两类目标——填 `15`/`16`
等其它写功能码会落入 `default` 分支、直接返回 `false`，写入静默失败。可写位号请按写线圈填 `functionCode=1`、按写保持寄存器填
`functionCode=3`。
:::

## 延伸阅读

- [驱动总览](./index) — 全部协议驱动与选型入口
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力速查
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — 网络层：寻址、字节序、轮询模型的通用背景
- [Modbus TCP 驱动](./modbus-tcp) — 以太网版 Modbus，功能码与地址模型一致
