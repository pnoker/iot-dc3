---
title: 驱动能力矩阵
---

# 驱动能力矩阵

本页一览 IoT DC3 全部 **28 个驱动**的协议类别、读 / 写 / 订阅能力与实现状态，帮你在选型时快速对位。每行链接到该驱动自己的页面，属性、采集周期、最小接入示例等细节在那里展开。

读 / 写 /
订阅按驱动当前实现的真实行为标注：「✓」表示该能力已落地，「—」表示该协议方向不实现、由设计决定不提供、或骨架尚未补齐——具体以代码为准（见表后说明）。订阅 /
上报指驱动被动接收设备推送（监听端口、网络回调、设备注册等），区别于周期轮询采集。

「实现状态」列总结该驱动整体成熟度：

- **完整**：读 / 写 / 订阅按协议设计全部落地，可直接用于生产接入。
- **可用**：核心链路可用，但有局部局限（如部分数据类型、Observe、健康钩子未实现），详见备注与驱动页。
- **骨架**：协议组帧或传输层尚未补全，目前仅供结构参考，不能直接采集真实设备。

## 工业总线 / PLC

这一类驱动作为主站（client）主动连设备，按[位号](../introduction/concepts/point)
轮询读值、按[命令](../introduction/concepts/command)写值，不监听上报。`ethernet-ip` 目前是协议骨架，CIP 组帧尚未补全。

| 驱动 (dc3.driver.code)                              | 类别       | 读 | 写 | 订阅/上报 | 实现状态 | 备注                 |
|---------------------------------------------------|----------|---|---|-------|------|--------------------|
| [Modbus TCP](./modbus-tcp) (`ModbusTcpDriver`)    | 工业总线/PLC | ✓ | ✓ | —     | 完整   | 以太网 Modbus 主站      |
| [Modbus RTU](./modbus-rtu) (`ModbusRtuDriver`)    | 工业总线/PLC | ✓ | ✓ | —     | 完整   | 串口 Modbus 主站       |
| [OPC UA](./opc-ua) (`OpcUaDriver`)                | 工业总线/PLC | ✓ | ✓ | —     | 完整   | OPC 统一架构客户端        |
| [OPC DA](./opc-da) (`OpcDaDriver`)                | 工业总线/PLC | ✓ | ✓ | —     | 完整   | 经典 OPC 数据访问（DCOM）  |
| [S7](./plcs7) (`PlcS7Driver`)                     | 工业总线/PLC | ✓ | ✓ | —     | 完整   | 西门子 PLC            |
| [MELSEC](./melsec) (`MelsecDriver`)               | 工业总线/PLC | ✓ | ✓ | —     | 完整   | 三菱 PLC（MC 协议）      |
| [FINS](./fins) (`FinsDriver`)                     | 工业总线/PLC | ✓ | ✓ | —     | 可用   | 欧姆龙 PLC，支持 16/32 位整数、浮点、字符串与 BCD |
| [EtherNet/IP](./ethernet-ip) (`EthernetIpDriver`) | 工业总线/PLC | — | — | —     | 骨架   | 罗克韦尔 / CIP，组帧待补    |

## SCADA / 电力 / 计量

楼宇、电力与计量类协议。`bacnet-ip` 与 `snmp` 主动读写；`sl651` 是水文遥测，开 TCP 服务端被动收报文，故仅订阅；`iec104`、`dlms`
当前为骨架。

| 驱动 (dc3.driver.code)                        | 类别          | 读 | 写 | 订阅/上报 | 实现状态 | 备注              |
|---------------------------------------------|-------------|---|---|-------|------|-----------------|
| [BACnet/IP](./bacnet-ip) (`BacnetIpDriver`) | SCADA/电力/计量 | ✓ | ✓ | —     | 完整   | 楼宇自控            |
| [IEC 104](./iec104) (`Iec104Driver`)        | SCADA/电力/计量 | — | — | —     | 骨架   | 电力 SCADA，协议层待补  |
| [DLMS](./dlms) (`DlmsDriver`)               | SCADA/电力/计量 | — | — | —     | 骨架   | 智能电表，传输层待补      |
| [SL651](./sl651) (`Sl651Driver`)            | SCADA/电力/计量 | — | — | ✓     | 完整   | 水文遥测，TCP 服务端收报文 |
| [SNMP](./snmp) (`SnmpDriver`)               | SCADA/电力/计量 | ✓ | ✓ | —     | 完整   | 网络设备监控          |

## IoT / 无线

物联网与无线类。`mqtt` 走发布/订阅，值由订阅被动到达（无主动读），命令可下发；`lwm2m` 内嵌服务端、收设备注册与通知，读写已落地但
Observe 订阅尚未实现；
`coap`、`http`、`ble` 为请求-响应式主动读写（`coap` 的 Observe 未实现）；`can`、`zigbee` 当前为骨架实现，`zigbee`
仅监听协调器网络状态、尚未监听节点入网与属性上报，`can` 底层走 can-utils。

| 驱动 (dc3.driver.code)                | 类别     | 读 | 写 | 订阅/上报 | 实现状态 | 备注                        |
|-------------------------------------|--------|---|---|-------|------|---------------------------|
| [MQTT](./mqtt) (`MqttDriver`)       | IoT/无线 | — | ✓ | ✓     | 可用   | 发布/订阅，值经订阅到达；`initial()` 钩子为骨架    |
| [CoAP](./coap) (`CoapDriver`)       | IoT/无线 | ✓ | ✓ | —     | 可用   | 受限设备 RESTful，Observe 未实现  |
| [LwM2M](./lwm2m) (`Lwm2mDriver`)    | IoT/无线 | ✓ | ✓ | —     | 可用   | 内嵌服务端，读写已落地，Observe 订阅未实现 |
| [HTTP](./http) (`HttpDriver`)       | IoT/无线 | ✓ | ✓ | —     | 完整   | 通用 HTTP 采集                |
| [BLE](./ble) (`BleDriver`)          | IoT/无线 | ✓ | ✓ | —     | 完整   | 低功耗蓝牙 GATT                |
| [Zigbee](./zigbee) (`ZigbeeDriver`) | IoT/无线 | ✓ | ✓ | —     | 骨架   | 骨架实现，订阅（入网/上报）未实现         |
| [CAN](./can) (`CanDriver`)          | IoT/无线 | ✓ | — | —     | 骨架   | 控制器局域网，底层走 can-utils      |

## 串口 / 通用网络

按命令模板组帧的通用透传驱动，主动收发，不监听。

| 驱动 (dc3.driver.code)                        | 类别      | 读 | 写 | 订阅/上报 | 实现状态 | 备注           |
|---------------------------------------------|---------|---|---|-------|------|--------------|
| [串口 Serial](./serial) (`SerialDriver`)      | 串口/通用网络 | ✓ | ✓ | —     | 完整   | 通用串口透传       |
| [TCP/UDP](./tcp-udp) (`TcpUdpDriver`) (raw) | 串口/通用网络 | ✓ | ✓ | —     | 完整   | 通用 socket 透传 |

## 数据库

把库表当数据源：读走 `executeQuery`、写走 `executeUpdate`，由[位号](../introduction/concepts/point)上的 SQL 模板驱动，不监听变更。

| 驱动 (dc3.driver.code)                            | 类别  | 读 | 写 | 订阅/上报 | 实现状态 | 备注      |
|-------------------------------------------------|-----|---|---|-------|------|---------|
| [MySQL](./mysql) (`MysqlDriver`)                | 数据库 | ✓ | ✓ | —     | 完整   | 从库表采集位号 |
| [PostgreSQL](./postgresql) (`PostgresqlDriver`) | 数据库 | ✓ | ✓ | —     | 完整   | 从库表采集位号 |
| [Oracle](./oracle) (`OracleDriver`)             | 数据库 | ✓ | ✓ | —     | 完整   | 从库表采集位号 |
| [SQL Server](./sqlserver) (`SqlserverDriver`)   | 数据库 | ✓ | ✓ | —     | 完整   | 从库表采集位号 |

## 虚拟 / 测试

无真实设备的两个驱动：`virtual` 按位号类型生成模拟读值（写为占位、不落设备）；`listening-virtual` 反向开 TCP/UDP
服务端收外部推送，并可经连接通道向设备回写。

| 驱动 (dc3.driver.code)                                                     | 类别    | 读 | 写 | 订阅/上报 | 实现状态 | 备注                 |
|--------------------------------------------------------------------------|-------|---|---|-------|------|--------------------|
| [虚拟 Virtual](./virtual) (`VirtualDriver`)                                | 虚拟/测试 | ✓ | — | —     | 可用   | 生成模拟数据，写为占位        |
| [监听虚拟 Listening Virtual](./listening-virtual) (`ListeningVirtualDriver`) | 虚拟/测试 | — | ✓ | ✓     | 完整   | TCP/UDP 服务端收推送，可回写 |

::: info 能力标记以代码为准
表中「✓ / —」反映各驱动 `*DriverCustomServiceImpl` 当前的真实实现：「—」可能是协议方向本就不提供（如 `virtual` 的写、`mqtt`
的主动读），也可能是骨架未补齐（如 `ethernet-ip`、`iec104`、`dlms`、`can`）。「实现状态」列是对成熟度的整体概括，局部的数据类型或子能力缺口以各驱动页的
`::: warning` / `::: info` 标注为准。最终行为请以对应模块的 `read()` / `write()` / `initial()` 源码为准；驱动迭代后此表会同步更新。
:::

## 延伸阅读

- [驱动总览](./index) — 按类别挑选协议，进入各驱动页
- [自定义驱动](../development/driver-authoring) — 基于 `virtual` 模板实现自己的协议驱动
