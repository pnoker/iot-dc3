---
title: 驱动总览
---

# 驱动 Drivers

> IoT DC3 内置 **28 个协议驱动**，覆盖工业总线、PLC/SCADA、物联网、数据库与虚拟测试。每个驱动是一个独立服务（`dc3-driver-*`
> ），启动时把自己和可接受的[配置属性](../introduction/concepts/attribute-config)
> 注册到管理中心，按[位号](../introduction/concepts/point)采数、按[指令](../introduction/concepts/command)写值。

接入一台设备的通用流程见[设备接入](../operation/device-onboarding)
；驱动的通用模型见[驱动](../introduction/concepts/driver)概念。下面按类别选你的协议：

## 协议适配层

设备世界的协议异构且割裂：Modbus 主从、OPC UA 地址空间、MQTT 发布订阅、PLC 专有帧、SQL 结果集——彼此既无统一寻址也无统一报文。这 28 个驱动在网络层各自扮演**感知数据汇聚点**的角色（见《基于物联网的四网融合技术研究及其应用》温喆、范亚斌著，吉林人民出版社·2016，第一章 第三节 物联网体系结构，p13），把上述异构协议的读写收敛为统一的[位号](../introduction/concepts/point)值；这正对应 IoT 三层架构中应用层的"**统一数据建模**、通信通道管理"（同上，p13）——上层应用只消费归一化后的位号，不再感知底层协议差异。

## 工业总线 / PLC / SCADA

| 驱动                           | 协议                | 说明            |
|------------------------------|-------------------|---------------|
| [Modbus TCP](./modbus-tcp)   | Modbus TCP        | 以太网 Modbus 主站 |
| [Modbus RTU](./modbus-rtu)   | Modbus RTU        | 串口 Modbus 主站  |
| [OPC UA](./opc-ua)           | OPC UA            | OPC 统一架构客户端   |
| [OPC DA](./opc-da)           | OPC DA            | 经典 OPC 数据访问   |
| [S7](./plcs7)                | Siemens S7        | 西门子 PLC       |
| [MELSEC](./melsec)           | Mitsubishi MELSEC | 三菱 PLC        |
| [FINS](./fins)               | Omron FINS        | 欧姆龙 PLC       |
| [EtherNet/IP](./ethernet-ip) | EtherNet/IP (CIP) | 罗克韦尔 / CIP    |
| [BACnet/IP](./bacnet-ip)     | BACnet/IP         | 楼宇自控          |
| [IEC 104](./iec104)          | IEC 60870-5-104   | 电力 SCADA      |
| [DLMS](./dlms)               | DLMS / COSEM      | 智能电表          |
| [SL651](./sl651)             | SL651             | 水文监测          |
| [SNMP](./snmp)               | SNMP              | 网络设备监控        |

## 物联网 / 无线

| 驱动                 | 协议           | 说明           |
|--------------------|--------------|--------------|
| [MQTT](./mqtt)     | MQTT         | 物联网消息总线      |
| [CoAP](./coap)     | CoAP         | 受限设备 RESTful |
| [LwM2M](./lwm2m)   | LwM2M        | 轻量级设备管理      |
| [HTTP](./http)     | HTTP         | 通用 HTTP 采集   |
| [BLE](./ble)       | Bluetooth LE | 低功耗蓝牙        |
| [Zigbee](./zigbee) | Zigbee       | 短距无线         |
| [CAN](./can)       | CAN          | 控制器局域网       |

## 串口 / 通用网络

| 驱动                    | 协议        | 说明           |
|-----------------------|-----------|--------------|
| [串口 Serial](./serial) | Serial    | 通用串口         |
| [TCP/UDP](./tcp-udp)  | TCP / UDP | 通用 socket 接入 |

## 数据库

| 驱动                         | 数据源        | 说明      |
|----------------------------|------------|---------|
| [MySQL](./mysql)           | MySQL      | 从库表采集点位 |
| [PostgreSQL](./postgresql) | PostgreSQL | 从库表采集点位 |
| [Oracle](./oracle)         | Oracle     | 从库表采集点位 |
| [SQL Server](./sqlserver)  | SQL Server | 从库表采集点位 |

## 虚拟 / 测试

| 驱动                                            | 说明                    |
|-----------------------------------------------|-----------------------|
| [虚拟 Virtual](./virtual)                       | 生成模拟数据，无需真实设备，用于体验与压测 |
| [监听虚拟 Listening Virtual](./listening-virtual) | 监听端口接收设备推送，用于联调       |

## 参考文献

温喆, 范亚斌. 基于物联网的四网融合技术研究及其应用[M]. 长春: 吉林人民出版社, 2016. ISBN 978-7-206-12410-5. （第一章 第三节 物联网体系结构, p13）

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — driver / point / command 属性的三层
- [设备接入](../operation/device-onboarding) — 一次完整接入流程
- [模块地图](../architecture/modules) — 驱动在整体架构里的位置
- [工业总线与协议](../foundations/fieldbus) · [IoT 协议与无线网络](../foundations/iot-protocols) — 协议背后的体系化知识
