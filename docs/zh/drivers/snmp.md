---
title: SNMP 驱动
---

# SNMP 驱动

> **`dc3-driver-snmp` 把 SNMP 网络设备接入 IoT DC3**——以设备上的 OID 为目标，周期性发 SNMP GET 采数，并支持向 OID 发 SNMP SET 写值。

SNMP（Simple Network Management Protocol，简单网络管理协议）是网络与机房设备最常用的管理协议，跑在 UDP 上、默认端口 `161`。被管设备（路由器、交换机、UPS、机房 PDU、打印机、服务器网卡等）内置一棵 MIB 树，树上每个可读写的数据点都有唯一的对象标识符 OID（形如 `1.3.6.1.2.1.1.1.0`）。本驱动基于 SNMP4J 库，作为 SNMP 管理端（manager）主动连接设备：读[位号](../introduction/concepts/point)对其 OID 发 SNMP GET，写位号对 OID 发 SNMP SET，每台设备复用一个 SNMP 会话。

适用场景：机房/网络监控（带宽、端口状态、CPU/内存、温湿度、UPS 电量等）——凡是支持 SNMP 的设备，配好 OID 即可采集。

- **驱动名 / code**：`SNMP Driver` / `SnmpDriver`
- **类型**：`DRIVER_CLIENT`（主动连设备）

## 驱动配置（设备级 `driver-attribute`）

接入一台 SNMP 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `127.0.0.1` | SNMP 设备 IP |
| Port | `port` | INT | `161` | SNMP 端口（标准 161）|
| Version | `version` | STRING | `v2c` | SNMP 版本（v1/v2c）|
| Community | `community` | STRING | `public` | 团体名（相当于只读/读写口令）|
| USM Username | `usmUsername` | STRING | （空） | SNMPv3 USM 用户名（v1/v2c 下不使用）|
| USM Auth Protocol | `usmAuthProtocol` | STRING | `MD5` | SNMPv3 认证算法（MD5/SHA）|
| USM Auth Password | `usmAuthPassword` | STRING | （空） | SNMPv3 认证密码 |
| Timeout | `timeout` | INT | `5000` | 请求超时（毫秒）|
| Retries | `retries` | INT | `1` | 请求重试次数 |

::: tip USM 三项仅为 SNMPv3 预留
`usmUsername` / `usmAuthProtocol` / `usmAuthPassword` 是 SNMPv3 的 USM 安全字段。当前实现只支持 v1 与 v2c，这三项填了也不生效，`version` 请填 `v1` 或 `v2c`。
:::

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| OID | `oid` | STRING | （空） | SNMP 对象标识符（如 `1.3.6.1.2.1.1.1.0`）|
| SNMP Type | `snmpType` | STRING | `OCTET_STRING` | SNMP 数据类型（INTEGER/GAUGE32/COUNTER32/OCTET_STRING/TIMETICKS/IPADDRESS/OID）|

::: tip OID 决定采哪个数据点
读取时驱动对配置的 `oid` 发 GET，把返回的变量值原样作为[位号值](../introduction/concepts/point-value)上报。读位号必须填 `oid`，否则校验不通过。`snmpType` 在读取时不影响取值，主要供写命令构造正确的变量类型。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| OID | `oid` | STRING | （空） | 要写入的 OID |
| SNMP Type | `snmpType` | STRING | `OCTET_STRING` | 写入值的 SNMP 数据类型 |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒采一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 IP `192.168.1.20:161`、团体名 `public` 的一台交换机接进来，采集它的系统描述（`sysDescr`，OID `1.3.6.1.2.1.1.1.0`）：

1. 选 `SNMP Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.20`、`port=161`、`version=v2c`、`community=public`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个描述[位号](../introduction/concepts/point)（`pointTypeFlag=STRING`、`READ_ONLY`），point 属性填 `oid=1.3.6.1.2.1.1.1.0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到设备的系统描述字符串。

## 易错点

::: warning OID 末尾常带 `.0`，别漏
标量（单值）对象的 OID 末尾要带实例标识 `.0`，例如 `sysDescr` 是 `1.3.6.1.2.1.1.1.0` 而不是 `1.3.6.1.2.1.1.1`。表项（如各端口流量）则用行索引结尾（如 `...10.1`、`...10.2`）。OID 写错会导致 GET 返回为空、采集报错。
:::

::: warning community 写错会静默超时
SNMP 用 `community` 团体名做口令。团体名不匹配或设备未开放该团体的访问时，设备通常不回包，表现为请求超时而非明确报错。接入前先用 `snmpget`/`snmpwalk` 在命令行确认 `host`、`port`、`community`、`oid` 这一组能取到值。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `community` / `oid` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [CoAP 驱动](./coap) — 同样跑在 UDP 上的轻量物联网协议
