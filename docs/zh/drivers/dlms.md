---
title: DLMS/COSEM 驱动
---

# DLMS/COSEM 驱动

> **`dc3-driver-dlms` 把 DLMS/COSEM 计量设备接入 IoT DC3**——以电表等计量表为目标，按 OBIS 编码周期性读取 COSEM 对象属性。

DLMS/COSEM（Device Language Message Specification / Companion Specification for Energy Metering）是电力、水、气、热等公用事业计量领域的国际标准协议（IEC 62056）。设备里的每个可读量（如有功电能、电压、时钟）被建模为一个 **COSEM 对象**，用 **OBIS 编码**（形如 `1.0.1.8.0.255`）唯一标识，每个对象又有若干带编号的**属性**（attribute），其中属性 `2` 通常是"当前值"。本驱动用 Gurux DLMS 库构建/解析 DLMS 帧，作为客户端（client）通过 TCP 或串口连到表计，按[位号](../introduction/concepts/point)上配置的 OBIS 编码取属性值。

适用场景：抄表系统对接电表/水表/气表、能源管理平台采集计量数据。

- **驱动名 / code**：`DLMS/COSEM Driver` / `DlmsDriver`
- **类型**：`DRIVER_CLIENT`（主动连表计）

::: warning 当前为骨架实现
该驱动目前是模板骨架：Gurux 客户端能生成 DLMS 帧，但传输层收发尚未实现——当前实现 `read()` / `write()` 直接抛异常（读失败抛 `ReadPointException`、写失败抛 `WritePointException`），SDK 据此记录失败并对连接退避，而非返回伪造的采集值或写成功；`health()` 仅检查客户端对象是否已缓存（无真实连通探测）。请把它当作接入新表计的起点，而非生产可用的成品。下文的属性表与采集周期均取自真实配置，可照填，但实际取数行为仍待补全。
:::

## 驱动配置（设备级 `driver-attribute`）

接入一台 DLMS/COSEM 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Transport Type | `transportType` | STRING | `TCP` | 传输方式（TCP, SERIAL）|
| Host | `host` | STRING | `localhost` | 远端设备地址（TCP 模式）|
| Port | `port` | INT | `4059` | 远端设备端口（TCP 模式）|
| Serial Port | `serialPort` | STRING | `/dev/ttyUSB0` | 串口路径（SERIAL 模式）|
| Baud Rate | `baudRate` | INT | `9600` | 波特率（SERIAL 模式）|
| Client Address | `clientAddress` | INT | `16` | DLMS 客户端地址（公共客户端=16）|
| Server Address | `serverAddress` | INT | `1` | DLMS 服务端地址 |
| Authentication | `authentication` | STRING | `NONE` | 认证方式（NONE, LOW, HIGH）|
| Password | `password` | STRING | `（空）` | 认证密码 |

::: tip TCP 与 SERIAL 二选一
`transportType=TCP` 时，只看 `host` / `port`；`transportType=SERIAL` 时，只看 `serialPort` / `baudRate`。另一组属性按当前传输方式被忽略，不必删。`clientAddress` / `serverAddress` / `authentication` / `password` 两种方式都用。
:::

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填，用 OBIS 编码定位要读的 COSEM 对象：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Object Type | `objectType` | STRING | `REGISTER` | DLMS 对象类型（REGISTER, CLOCK, DATA 等）|
| Logical Name | `logicalName` | STRING | `（空）` | 对象逻辑名 / OBIS 编码（例 `1.0.1.8.0.255`）|
| Attribute ID | `attributeId` | INT | `2` | 属性编号（2=当前值）|

::: tip OBIS 编码定位"读哪个量"
`logicalName` 是 6 段 OBIS 编码，唯一标识表里的一个计量量，例如 `1.0.1.8.0.255` 是"总有功电能"。`attributeId=2` 取该对象的"当前值"属性。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要和对象属性的实际类型对得上。
:::

DLMS/COSEM 是只读抄表语义，本驱动不提供写命令——`application.yml` 中 `command-attribute` 为空，因此没有写命令配置表。

## 采集与健康

- **采集周期**：默认 read cron `0/30 * * * * ?`（每 30 秒读一轮）；另有 custom 周期任务 cron `0/5 * * * * ?`（每 5 秒）——当前实现的 `schedule()` 为空操作，该任务暂不执行任何逻辑。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 IP `192.168.1.20:4059` 的一只电表接进来，读总有功电能：

1. 选 `DLMS/COSEM Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `transportType=TCP`、`host=192.168.1.20`、`port=4059`、`clientAddress=16`、`serverAddress=1`、`authentication=NONE`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个电能[位号](../introduction/concepts/point)（`pointTypeFlag=DOUBLE`、`READ_ONLY`），point 属性填 `objectType=REGISTER`、`logicalName=1.0.1.8.0.255`、`attributeId=2`。
3. 启动驱动，30 秒内会触发一轮读——但当前 `read()` 直接抛 `ReadPointException`，SDK 记录读失败并退避，[位号值](../introduction/concepts/point-value)里暂时不会有采集值；待传输层补全后才能真正采到值。

## 易错点

::: warning Host/Port 与传输方式必须匹配
`host` / `port` 只在 `transportType=TCP` 下生效。若把 `transportType` 设成 `SERIAL` 却只填了 `host`，驱动会走串口分支去找 `serialPort`，连不上表计。改传输方式时，记得同步填对应那一组属性。
:::

::: tip clientAddress 默认是公共客户端
`clientAddress=16` 对应 DLMS 的"公共客户端"（public client），多数表计允许其无认证读取基础计量量。要读受保护的对象，需改用更高权限的客户端地址，并把 `authentication` 调到 `LOW` / `HIGH` 配上 `password`。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `logicalName` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 另一种 TCP 工业协议驱动
