---
title: 串口 驱动
---

<script setup>
import SerialDiagram from '../../.vitepress/theme/components/SerialDiagram.vue'
</script>


# 串口 驱动

`dc3-driver-serial` 把跑私有报文的 RS232/RS485/RS422 串口设备接入 IoT
DC3：作为串口主站，按[位号](../introduction/concepts/point)上配的 HEX
指令周期性发送、读回原始字节，再按帧头/帧尾、校验、数据偏移与格式解析成值，并支持向设备写值的命令。读完本页你能为一台"
发一串字节、回一串字节"的串口设备配好线路参数与帧解析规则，并知道接不上时从哪儿查。

## 协议背景

串口（Serial）是工业现场最朴素也最通用的连接方式。**RS232** 点对点、**RS485/RS422** 总线广泛用于仪表、变送器、电表、扫码枪、PLC
串口模块等。它只规定了物理层与链路层"用什么电平、几根线、按什么波特率收发字节"，**不规定字节里装什么**——很多设备并不跑
Modbus 这类标准协议，而是厂商自定义的私有报文：发一串固定字节，回一串固定结构的字节。

在物联网四层架构里，串口属于**网络层**：它解决的是现场设备与采集主站之间"用哪种信号、按什么线路参数交换字节"
的问题，而不关心这些字节的业务语义。本驱动正是把这条原始字节通道补成一个可配置的协议适配器——你用 HEX 指令描述"发什么"
，用帧头/帧尾/偏移/长度描述"回包怎么切",用校验类型与数据格式描述"切出来的字节怎么校验、怎么解码"
。关于寻址、字节序、轮询模型这些网络层通用背景，见[物联网网络层：工业总线与协议](../foundations/fieldbus)。

- **驱动名 / code**：`Serial Port Driver` / `SerialDriver`
- **类型**：`DRIVER_CLIENT`（主动打开串口、轮询设备）
- **底层库**：jSerialComm（每台[设备](../introduction/concepts/device)一条独立串口连接，按设备 ID 缓存）

::: tip 先认识两个词
**HEX 指令**：用十六进制书写的字节串，如 `01 03 00 00 00 0A C5 CD`；空格只为可读，对应实际发送的字节（驱动会去掉空格与 `-`
再解析）。**帧（Frame）**：设备回复的一整段字节，通常由帧头、数据区、可选校验、帧尾拼成；本驱动靠帧头/帧尾与偏移/长度从回包里"切"
出真正的数据区，再按格式解码。
:::

## 属性配置

串口的配置分三层：**驱动属性**（`driver-attribute`，设备级）描述这条串口怎么打开（线路参数）；**位号属性**（`point-attribute`
）描述每个采集点发什么、回包怎么拆、按什么格式解码；**命令属性**（`command-attribute`）描述可写位号往哪儿写。下面三张表的默认值与说明均来自驱动
`application.yml`，属性的三层来历见[属性与配置](../introduction/concepts/attribute-config)。

### 驱动属性（设备级 `driver-attribute`）

接入一台串口设备时，在[设备](../introduction/concepts/device)上填这些线路参数。它们会原样传给 jSerialComm 打开串口，因此*
*必须与设备的串口设置逐一对上**——串口没有握手协商，任一项不匹配只会读到乱码或超时：

| 属性          | code       | 类型     | 默认值            | 说明                                                       |
|-------------|------------|--------|----------------|----------------------------------------------------------|
| Serial Port | `port`     | STRING | `/dev/ttyUSB0` | 串口设备路径（如 /dev/ttyUSB0、COM3）                              |
| Baud Rate   | `baudRate` | INT    | `9600`         | 波特率（1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200） |
| Data Bits   | `dataBits` | INT    | `8`            | 数据位（5, 6, 7, 8）                                          |
| Stop Bits   | `stopBits` | INT    | `1`            | 停止位（1, 2）                                                |
| Parity      | `parity`   | INT    | `0`            | 校验位（0=无, 1=奇, 2=偶）                                       |
| Timeout     | `timeout`  | INT    | `1000`         | 读超时（毫秒），到点仍未读够即返回已收字节                                    |

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填三组：发什么（`sendCommand`/`receiveLength`）、回包怎么拆（`frameHeader`/
`frameFooter`/`dataOffset`/`dataLength`/`checksumType`）、按什么格式解码（`dataFormat`/`byteOrder`）。其中只有 `sendCommand`
是必填，其余留空则取默认值：

| 属性             | code            | 类型     | 默认值    | 说明                                          |
|----------------|-----------------|--------|--------|---------------------------------------------|
| Send Command   | `sendCommand`   | STRING | （空）    | 要发送的 HEX 指令（如 `01 03 00 00 00 0A C5 CD`），必填 |
| Receive Length | `receiveLength` | INT    | `0`    | 预期回包字节数；0=读到超时/帧间空闲为止，>0=精确读这么多字节           |
| Frame Header   | `frameHeader`   | STRING | （空）    | HEX 帧头（如 `01 03`），用于在回包中定位帧起点               |
| Frame Footer   | `frameFooter`   | STRING | （空）    | HEX 帧尾（如 `0D 0A`），用于定位帧终点                   |
| Data Offset    | `dataOffset`    | INT    | `0`    | 数据区起点偏移（**相对帧头之后**；无帧头时相对帧起始）               |
| Data Length    | `dataLength`    | INT    | `0`    | 数据区字节长度（0=取到帧尾/校验区之前）                       |
| Checksum Type  | `checksumType`  | STRING | `NONE` | 回包校验类型：NONE, CRC16, XOR                     |
| Data Format    | `dataFormat`    | STRING | `HEX`  | 数据格式：HEX, ASCII, BINARY, FLOAT              |
| Byte Order     | `byteOrder`     | STRING | `BIG`  | 字节序：BIG, LITTLE                             |

::: tip 解析顺序：先定位帧、再校验、最后解码
驱动收到回包后，按 `parseResponse` → `SerialFrameParser.parse` 依次执行：① 用 `frameHeader` 找帧起点（`indexOf`，找不到报错）、用
`frameFooter` 从帧头之后找帧终点（`lastIndexOf`）；② 从帧头之后再加 `dataOffset` 得到数据区起点，按 `checksumType`
在帧尾前留出校验区（CRC16 占 2 字节、XOR 占 1 字节）；③ 若 `checksumType≠NONE`，对帧头之后到数据区结束的字节算校验并与回包中的校验字节比对，不符则报错；④
按 `dataLength`（0=取到校验区之前）切出数据区，按 `dataFormat`+`byteOrder`
解码。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）应与 `dataFormat` 对得上：`BINARY`/`FLOAT`
下 1/2/4/8 字节会按 `byteOrder` 拼为整数或浮点（`FLOAT` 用 4 字节单精度、8 字节双精度），长度不在 1/2/4/8 之列时退化为 HEX
字符串。
:::

### 命令属性（`command-attribute`）

可写位号在写命令上填：

| 属性           | code          | 类型     | 默认值        | 说明                         |
|--------------|---------------|--------|------------|----------------------------|
| Send Command | `sendCommand` | STRING | `${value}` | 带 `${value}` 占位符的 HEX 指令模板 |
| Byte Order   | `byteOrder`   | STRING | `BIG`      | 编码写入值的字节序：BIG, LITTLE      |

写入时，驱动把命令参数渲染进 `sendCommand` 模板的 `${value}` 位置，按 HEX 转成字节后整帧发出，不读回包。

::: warning 写命令的 byteOrder 当前未参与编码
`command-attribute` 列了 `byteOrder`，但 `write()` 的实现只做了 `sendCommand.replace("${value}", value)` 的字符串替换，再把整条结果当作
HEX 解析发出——`${value}` 必须是一段合法 HEX 文本，`byteOrder` 不会对它做大小端转换。要写一个多字节数值，请在上层把它编码成正确字节序的
HEX 字符串再作为命令参数传入。
:::

### 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒轮询一轮全部位号）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`；驱动以"该设备的串口是否已打开（
  `SerialPort.isOpen()`）"判定设备在线/离线——在线状态机制见[设备](../introduction/concepts/device)。
- **自定义任务**：yml 里有 `custom` 调度（`0/5 * * * * ?`），但驱动的 `schedule()` 为空实现，串口驱动不需要自定义周期任务。

## 故障排查

::: warning sendCommand 的校验字节要自己算对
本驱动**不替你补 CRC/校验**——`sendCommand` 是原样发送的整条字节串，若设备要求 Modbus CRC
或其它校验，校验字节必须由你算好写进指令里。算错的话，设备要么不回包，要么回异常帧，表现为读到空响应或解析报错。注意
`checksumType` 只用于校验**回包**，绝不会改写你发出的指令。
:::

::: warning dataOffset 从"帧头之后"算，不是从整帧第 0 字节
源码里数据区起点是 `start + dataOffset`，而 `start` 是**帧头之后**的位置（配了 `frameHeader` 时已跳过帧头长度）。所以：配了帧头
`01 03` 时，`dataOffset` 是相对 `01 03` 之后那一字节计的；没配帧头时，`start=0`，`dataOffset` 才相对整帧起始。把"
帧头也算进偏移"会多跳几个字节、解出错误数据。最稳妥的做法是二选一：要么只用 `frameHeader` 定位、`dataOffset` 填帧头之后的偏移；要么不配
`frameHeader`、`dataOffset` 直接从第 0 字节数。
:::

::: warning 找不到帧头/帧尾会直接报错
配了 `frameHeader` 但回包里没有这段字节，`parse()` 抛 `Frame header not found`；`frameFooter` 同理。回包被截断（`timeout`
太短、`receiveLength` 配大了读不够）也会导致帧头/帧尾缺失或数据区为空（`No data region in serial frame`）。先把
`dataFormat=HEX` 打出原始回包，确认帧结构与配置一致，再逐项收紧。
:::

::: warning 校验类型配错会判回包"校验失败"
`checksumType=CRC16`（本驱动用 Modbus CRC16，多项式 `0xA001`、低字节在前）或 `XOR` 时，驱动会对"帧头之后到数据区结束"
的字节重算校验并与回包中的校验字节比对，不符则抛 `checksum mismatch`。若设备的校验范围或算法与此不同（如校验含帧头、或用别的多项式），请改回
`NONE` 由上层自行校验，避免好数据被误判丢弃。无法识别的校验名（非 NONE/CRC16/XOR）会被当作 `NONE` 处理。
:::

::: tip 串口是独占资源，注意占用与权限
一个驱动进程按设备 ID 缓存独立串口连接，可同时服务挂在不同 `port` 的多台设备；但同一物理串口同一时刻只能被一个进程打开。确认该
`port` 没有被串口助手、其它驱动进程占用；Linux 上还需运行用户对 `/dev/ttyUSB*` 有读写权限（常见做法是把用户加入 `dialout`
组）。读/写失败时驱动会主动关闭并移除该连接（`invalidateConnector`），下一轮采集重新打开。
:::

::: tip 一根 RS485 总线上的多台设备各建一台 Device
同一物理串口（RS485 总线）上的多台设备需各自建一台[设备](../introduction/concepts/device)、指向**相同的 `port`**，用各自的
`sendCommand`（含不同站号）区分，驱动按位号顺序轮询发指令。不要给同总线的不同设备配不同 `port`。
:::

## 在 IoT DC3 中如何落地

无论底层报文多私有，落到平台都收敛为同一个[位号 Point](../introduction/concepts/point)
的[位号值 PointValue](../introduction/concepts/point-value)。串口驱动以 `dc3.driver.code = SerialDriver`
注册，这是稳定的路由标识，平台据此把读/写命令分发到本驱动。

<SerialDiagram lang="zh" />

按[驱动能力矩阵](./matrix)，本驱动的能力为：

| 能力 | 支持 | 实现说明                                             |
|----|----|--------------------------------------------------|
| 读  | ✓  | `read()` 发 `sendCommand`、读回包、按帧结构切出数据区并解码        |
| 写  | ✓  | `write()` 渲染 `${value}` 模板、转字节整帧发出（不读回包、不做字节序编码） |
| 订阅 | —  | 主从轮询模型，设备不主动上报，靠采集周期定时读                          |

::: info 实现状态：可用
`SerialDriverCustomServiceImpl` 的 `initial()`/`read()`/`write()`/`health()`/`event()` 与帧解析（`SerialFrameParser`
）、连接管理（`SerialPortConnection`，基于 jSerialComm）均已完整实现，非骨架。读路径支持 HEX/ASCII/BINARY/FLOAT 解码与
CRC16/XOR/NONE 回包校验；连接按设备缓存，设备元数据 UPDATE/DELETE 事件会销毁旧连接（`connectMap.remove` 后 `close()`）。
:::

## 最小接入示例

接入一台挂在 `/dev/ttyUSB0`、9600-8-N-1 的温度变送器，发查询指令后回包为 `01 03 02 <数据 2 字节> <CRC 2 字节>`：

1. 选 `Serial Port Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `port=/dev/ttyUSB0`、
   `baudRate=9600`、`dataBits=8`、`stopBits=1`、`parity=0`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`READ_ONLY`
   ），point 属性填 `sendCommand=01 03 00 00 00 01 84 0A`、`dataOffset=3`、`dataLength=2`、`dataFormat=BINARY`、
   `byteOrder=BIG`（不配 `frameHeader`，`dataOffset` 从回包第 0 字节数起，跳过 `01 03 02` 三个字节，正好落在数据区）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

::: tip 跑标准 Modbus RTU 优先用专用驱动
本驱动是"通用串口透传"，适合私有报文或需要逐字节控制帧的场景。如果设备跑的是标准 Modbus
RTU（功能码、CRC、寻址都规范），用 [Modbus RTU 驱动](./modbus-rtu)更省事——它替你拼帧、补 CRC、按功能码寻址，无需手写
`sendCommand`。
:::

## 延伸阅读

- [驱动总览](./index) — 全部协议驱动与选型入口
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力速查
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — 网络层：寻址、字节序、轮询模型的通用背景
- [Modbus RTU 驱动](./modbus-rtu) — 跑标准 Modbus 协议的串口驱动
