---
title: TCP/UDP 驱动
---

# TCP/UDP 驱动

> **`dc3-driver-tcp-udp` 把任意走裸 TCP/UDP socket 的设备接入 IoT DC3**——按[位号](../introduction/concepts/point)发一条 HEX 指令、收回原始字节，再按帧规则切出数据并转成值。

很多串口转网口模块、自研单片机、私有协议设备没有标准协议栈，只是在一个 TCP/UDP 端口上"收发字节流"：你发一段十六进制指令，它回一段十六进制响应。本驱动就是这类设备的通用底座——它不绑定任何第三方协议库，直接用 JDK 的 socket，把"发什么指令、回包怎么解析"全交给[属性](../introduction/concepts/attribute-config)配置。

> **协议（protocol）**：TCP 是面向连接的可靠字节流，UDP 是无连接的数据报。本驱动按设备上的 `protocol` 属性二选一：TCP 会按设备缓存长连接，UDP 每次采集临时建 socket 收发。
>
> **HEX 指令 / 帧（frame）**：你和设备之间收发的是二进制，这里统一用十六进制字符串书写（如 `01 03 00 00 00 02`）。设备回来的一整段字节叫一帧，"帧头/帧尾/偏移/长度"用来从帧里定位出真正的数据。

- **驱动名 / code**：`TCP/UDP Raw Driver` / `TcpUdpDriver`
- **类型**：`DRIVER_CLIENT`（驱动主动连设备、发指令）

## 驱动配置（设备级 `driver-attribute`）

接入一台 TCP/UDP 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Protocol | `protocol` | STRING | `TCP` | TCP or UDP |
| Host | `host` | STRING | `localhost` | （设备 IP / 主机名）|
| Port | `port` | INT | `502` | （设备端口）|
| Connect Timeout | `connectTimeout` | INT | `5000` | （TCP 连接超时，毫秒）|
| Read Timeout | `readTimeout` | INT | `3000` | （读响应超时，毫秒）|
| Delimiter | `delimiter` | STRING | （空）| Hex delimiter |

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填，描述"发什么、怎么从回包里取值"：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | （空）| （采集时发送的 HEX 指令）|
| Receive Length | `receiveLength` | INT | `0` | 0 means use delimiter |
| Frame Header | `frameHeader` | STRING | （空）| （帧头 HEX）|
| Frame Footer | `frameFooter` | STRING | （空）| （帧尾 HEX）|
| Data Offset | `dataOffset` | INT | `0` | （数据在帧中的字节偏移）|
| Data Length | `dataLength` | INT | `0` | （数据字节长度）|
| Data Format | `dataFormat` | STRING | `HEX` | HEX/ASCII/INT16/UINT16/INT32/FLOAT |
| Byte Order | `byteOrder` | STRING | `BIG` | （字节序：BIG / LITTLE）|

::: tip dataFormat 决定回包怎么变成值
驱动按 `dataOffset` + `dataLength` 从回包里切出一段字节，再按 `dataFormat` 转换：`HEX` 原样输出十六进制字符串、`ASCII` 转文本、`INT16/UINT16/INT32/FLOAT` 按数值解析（多字节量受 `byteOrder` 控制，`BIG` 为大端、`LITTLE` 为小端）。若 `dataLength=0`，则不切帧、整段回包以 HEX 返回。
:::

## 写命令配置（`command-attribute`）

可写位号在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Send Command | `sendCommand` | STRING | `${value}` | （写指令模板，`${value}` 用命令值替换）|

写值时把命令里的 `${value}` 占位符替换成实际值，再作为 HEX 指令发给设备。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒采一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。TCP 设备靠一次快速建连判断在线；UDP 无连接，默认按在线上报——在线状态机制见[设备](../introduction/concepts/device)。
- **自定义调度**：`schedule.custom` 默认启用，cron `0/5 * * * * ?`（每 5 秒触发一次）。本驱动未实现自定义周期任务（`schedule()` 为空方法），故该调度实际不执行任何动作。

## 最小接入示例

把一台 `192.168.1.50:8899` 的 TCP 设备接进来，采集一个 16 位温度值（设备回 `01 03 04 00 FA 12 34 ...`，温度在第 3、4 字节）：

1. 选 `TCP/UDP Raw Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `protocol=TCP`、`host=192.168.1.50`、`port=8899`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=INT`、`READ_ONLY`），point 属性填 `sendCommand=010300000001`、`dataOffset=3`、`dataLength=2`、`dataFormat=INT16`、`byteOrder=BIG`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到解析出的数值（`00FA` → `250`）。

## 易错点

::: warning sendCommand / 帧解析都是 HEX，不要混入空格外的字符
`sendCommand`、`frameHeader`、`frameFooter` 都按十六进制解析（空白会被忽略，可写成 `01 03 00 00`）。填入非 HEX 字符（如十进制 `10` 当成一个数）会解析失败或读到错值。`dataFormat=ASCII` 是把切出的字节再转文本，指令本身仍是 HEX。
:::

::: warning offset/length 越界会回退成整段原始 HEX
`dataOffset + dataLength` 超过回包实际长度时，驱动不会报错，而是放弃切帧、把整段回包以 HEX 原样返回。看到位号值是一长串 HEX 却期望是数字，多半是 `dataOffset`/`dataLength` 没对上设备实际回包结构。
:::

::: tip 连接失败有退避，别被"暂时不连"吓到
TCP 连续 3 次连接/读写失败后，会进入 60 秒退避窗口暂停重连；其间设备按离线上报，窗口过后自动重试。设备恢复并成功通信一次即清零计数。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `protocol` / `sendCommand` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 标准化的 TCP 协议示例，可与本通用驱动对照
