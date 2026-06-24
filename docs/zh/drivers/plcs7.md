---
title: PLC S7 驱动
---

# PLC S7 驱动

> **`dc3-driver-plcs7` 把西门子 S7 系列 PLC 接入 IoT DC3**——以 PLC 的数据块（DB）为目标，周期性读取寄存器值，并支持向 DB 写值。

S7 是西门子 PLC（S7-200/300/400/1200/1500 及 S7-200 Smart 等）使用的私有以太网协议，跑在 TCP 之上，工业现场用它直接读写 PLC 内部存储区。本驱动作为 S7 客户端（client），通过 TCP 连到一台或多台 PLC，按[位号](../introduction/concepts/point)上配置的数据块号与偏移地址采数、写值。

- **驱动名 / code**：`PLC S7 Driver` / `PlcS7Driver`
- **类型**：`DRIVER_CLIENT`（主动连 PLC）

## 驱动配置（设备级 `driver-attribute`）

接入一台 S7 PLC 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `192.168.0.20` | PLC IP 地址 |
| Port | `port` | INT | `102` | S7 TCP 端口（标准 102）|
| PLC Type | `plcType` | STRING | `S1200` | PLC 型号（`S200/S200_SMART/S300/S400/S1200/S1500/SINUMERIK_828D`）|

::: tip plcType 决定地址解析方式
不同型号的 S7 PLC 在 DB 寻址、字节序上有差异，`plcType` 用来选对应的 S7 寻址方案。填错或填了未知型号时，驱动会回退到 `S1200`。常见取值：S7-1200 填 `S1200`、S7-1500 填 `S1500`、S7-200 Smart 填 `S200_SMART`。
:::

## 位号配置（`point-attribute`）

每个[位号](../introduction/concepts/point)定位 PLC 数据块里的一个变量，填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| DB Number | `dbNum` | INT | `0` | 数据块号，从 0 开始计 |
| Byte Offset | `byteOffset` | INT | `0` | 数据块内的字节偏移 |
| Bit Offset | `bitOffset` | INT | `0` | 字节内的位偏移（仅 boolean 类型使用）|

::: tip 位号类型决定读多少字节
驱动按位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）决定从 `byteOffset` 起读取的宽度：`BOOLEAN` 取 `byteOffset.bitOffset` 处的 1 个 bit，`SHORT` 读 2 字节、`INT`/`FLOAT` 读 4 字节、`LONG`/`DOUBLE` 读 8 字节，`STRING` 读字符串。所以 `bitOffset` 只在布尔位号上有意义，其余类型忽略它。
:::

## 写命令配置

本驱动支持向 PLC 写值，但**没有独立的写命令属性**（application.yml 无 `command-attribute`）。写一个[位号](../introduction/concepts/point)时复用它自己的 `point-attribute`（`dbNum` / `byteOffset` / `bitOffset`）定位地址，写入的数据宽度由命令携带的值类型决定。也就是说：一个可写位号配好上面三项后即可读可写，无需再额外配置。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 IP `192.168.0.20:102` 的一台 S7-1200 PLC 接进来，读 DB1 里偏移 0 处的一个 32 位浮点：

1. 选 `PLC S7 Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.0.20`、`port=102`、`plcType=S1200`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`），point 属性填 `dbNum=1`、`byteOffset=0`、`bitOffset=0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning PLC 侧必须放开 PUT/GET 访问
S7-1200/1500 默认禁止外部 PUT/GET 通信。需要在 TIA Portal 里勾选"允许来自远程对象的 PUT/GET 通信访问"，否则连接能建立但读写会失败。被读写的 DB 还要关闭"优化的块访问"，驱动才能按字节偏移定位变量。
:::

::: warning bitOffset 只对布尔位号生效
非布尔类型（`SHORT`/`INT`/`FLOAT` 等）只看 `byteOffset`，`bitOffset` 被忽略。给一个浮点位号填了 `bitOffset` 不会报错，但也不起作用——别用它来"跳过"字节，跨字节请改 `byteOffset`。
:::

::: tip 一个驱动实例可接多台 PLC
同一个 PLC S7 驱动进程可服务多台设备，每台 PLC 由各自[设备](../introduction/concepts/device)上的 `host`（及 `plcType`）区分，连接按 deviceId 复用并自动重连。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `dbNum` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Melsec 驱动](./melsec) — 三菱 PLC 以太网驱动
