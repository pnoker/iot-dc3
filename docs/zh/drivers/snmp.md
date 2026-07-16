---
title: SNMP 驱动
---

<script setup>
import SnmpDiagram from '../../.vitepress/theme/components/SnmpDiagram.vue'
</script>


# SNMP 驱动

`dc3-driver-snmp` 把支持 SNMP 的网络与机房设备接入 IoT DC3：以设备 MIB 树上的 OID 为目标，周期性发 SNMP GET 采数，并支持向
OID 发 SNMP SET 写值。读完你能配好一台路由器、交换机或
UPS，把它的端口状态、流量、温湿度等指标采成[位号值](../introduction/concepts/point-value)。

## 协议背景

SNMP（Simple Network Management Protocol，简单网络管理协议）是网络与数据中心设备最通用的管理协议，跑在 UDP 上、默认端口 `161`
。它属于物联网四层架构里的[网络层](../foundations/fieldbus)——和工业现场的 Modbus、OPC 一样解决"
在某个寻址空间里读/写一个值"的问题，只是它的设备不是 PLC 和电表，而是路由器、交换机、UPS、机房 PDU、打印机、服务器网卡这类 IP
网络元件。

每台被管设备内置一棵 MIB（Management Information Base）树，树上每个可读写的数据点都有唯一的对象标识符 OID（Object
Identifier，形如 `1.3.6.1.2.1.1.1.0`）。管理端（manager）通过 OID 来定位"采哪个值"：

- **标量对象**末尾带实例标识 `.0`，例如系统描述 `sysDescr` 是 `1.3.6.1.2.1.1.1.0`；
- **表项对象**（如各端口的流量、状态）末尾带行索引，例如 `...10.1`、`...10.2` 分别对应 1 号、2 号端口。

SNMP 有 v1 / v2c / v3 三个版本。v1 与 v2c 用明文的 `community`（团体名）做口令，配置简单、现场最常见；v3 引入 USM（User-based
Security Model）支持认证与加密。本驱动基于 SNMP4J 库，作为 SNMP 管理端主动连接设备：读位号对其 OID 发 GET，写位号对 OID 发
SET，每台设备复用一个常驻的 SNMP 会话。

典型用途是机房与网络监控——带宽、端口 up/down、CPU/内存利用率、机房温湿度、UPS 电量等，凡是支持 SNMP 的设备，配好 OID 即可纳管。

<SnmpDiagram lang="zh" />

## 属性配置

SNMP 的连接参数和采集目标在两个层面填写：连接一台设备靠**驱动属性**（device 级），定位每个数据点靠**位号属性**（point
级）。属性的名称、类型与默认值都来自驱动 `application.yml` 的 `driver-attribute` / `point-attribute` / `command-attribute`
定义。

### 驱动属性（设备级 `driver-attribute`）

接入一台 SNMP 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。
`host` / `port` 决定连哪台设备的哪个 UDP 端口，`version` + `community` 是 v1/v2c 的身份口令，`timeout` / `retries`
控制请求的容错。

| 属性                | code              | 类型     | 默认值         | 说明                          |
|-------------------|-------------------|--------|-------------|-----------------------------|
| Host              | `host`            | STRING | `127.0.0.1` | SNMP 设备 IP                  |
| Port              | `port`            | INT    | `161`       | SNMP 端口（标准 161）             |
| Version           | `version`         | STRING | `v2c`       | SNMP 版本（`v1` / `v2c`）       |
| Community         | `community`       | STRING | `public`    | 团体名（相当于只读/读写口令）             |
| USM Username      | `usmUsername`     | STRING | （空）         | SNMPv3 USM 用户名（v1/v2c 下不使用） |
| USM Auth Protocol | `usmAuthProtocol` | STRING | `MD5`       | SNMPv3 认证算法（MD5/SHA）        |
| USM Auth Password | `usmAuthPassword` | STRING | （空）         | SNMPv3 认证密码                 |
| Timeout           | `timeout`         | INT    | `5000`      | 请求超时（毫秒）                    |
| Retries           | `retries`         | INT    | `1`         | 请求重试次数                      |

::: warning USM 三项仅为 SNMPv3 预留，当前不生效
`usmUsername` / `usmAuthProtocol` / `usmAuthPassword` 是 SNMPv3 的 USM 安全字段，`application.yml` 里已预留但驱动
`buildTarget()` 只构造 `CommunityTarget`，按 `version` 设置 `version1` 或 `version2c`。当前实现只支持 v1 与
v2c，这三项填了也不会被读取，`version` 请填 `v1` 或 `v2c`。
:::

`validate()` 把 `host` / `port` / `version` / `community` 列为必填——缺任一项设备校验不通过。

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填 `oid` 指定采哪个数据点；`snmpType` 标注该值的 SNMP 数据类型。

| 属性        | code       | 类型     | 默认值            | 说明                                                                        |
|-----------|------------|--------|----------------|---------------------------------------------------------------------------|
| OID       | `oid`      | STRING | （空）            | SNMP 对象标识符（如 `1.3.6.1.2.1.1.1.0`）                                         |
| SNMP Type | `snmpType` | STRING | `OCTET_STRING` | SNMP 数据类型（INTEGER/GAUGE32/COUNTER32/OCTET_STRING/TIMETICKS/IPADDRESS/OID） |

::: tip OID 决定采哪个数据点，snmpType 主要供写入用
读取时驱动对配置的 `oid` 发 GET，把返回 `VariableBinding` 的变量值用 `variable.toString()`
原样作为[位号值](../introduction/concepts/point-value)上报——此时 `snmpType` 不参与取值。`snmpType` 的真正作用在写入：
`createVariable()` 据它把字符串转成正确的 SNMP 变量类型。`validatePoint()` 把 `oid` 列为必填，读位号缺 `oid` 校验不通过。
:::

### 写入复用位号属性，无需另配写命令

写入和读取共用位号上的同一份 `oid` / `snmpType`：`write()`（SET）从 `pointConfig`（point-attribute）读取 `oid` 与 `snmpType`
，对该 OID 发 SET、按 `snmpType` 构造写入值。可写位号只要在 point 上配好 `oid` 与 `snmpType` 即可，不需要在写命令上重复填。

`createVariable()` 支持的 `snmpType` 取值：`INTEGER`/`INTEGER32`、`GAUGE32`/`COUNTER32`/`UNSIGNED_INTEGER32`、`COUNTER64`、
`TIMETICKS`、`OID`、`IPADDRESS`、`NULL`，其余一律按 `OCTET_STRING` 处理。

::: info `command-attribute` 当前不被写入路径读取
`application.yml` 里声明了 `command-attribute`（`oid` / `snmpType`），但 `write()` 的签名只接收 `driverConfig` 与
`pointConfig`，并不传入命令属性，本驱动也未覆写 `execute()`，因此这组 `command-attribute` 目前是占位声明、写入时不会被读取。请把可写位号的
`oid` / `snmpType` 配在位号(point)上，而非写命令上——否则写入会落到位号缺省值（`oid` 为空、`snmpType=OCTET_STRING`）。
:::

### 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒采一轮，对应 `schedule.read.cron`）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。`health()` 以"设备是否已建立 SNMP 会话"判定在线——
  `clientMap` 里有该设备则视为 online，没有则尝试建会话，建成即
  online。在线状态机制见[设备](../introduction/concepts/device)。

::: info SNMP 会话的建立不等于设备可达
`getConnector()` 建的是本地 UDP transport（`DefaultUdpTransportMapping`），`listen()` 成功就缓存为"online"
，并不向设备发探测包。也就是说设备掉线后，健康检查仍可能短暂报 online，真正的失败要等下一次 `read()` 超时——此时驱动会
`clientMap.remove(deviceId)` 销毁会话，下一轮健康检查才转 offline。
:::

## 故障排查

::: warning OID 末尾常带 `.0`，别漏
标量（单值）对象的 OID 末尾要带实例标识 `.0`，例如 `sysDescr` 是 `1.3.6.1.2.1.1.1.0` 而不是 `1.3.6.1.2.1.1.1`
。表项（如各端口流量）则用行索引结尾（如 `...10.1`、`...10.2`）。OID 写错时设备返回 `noSuchObject`/`noSuchInstance`，
`variable.toString()` 会把它当普通字符串上报成位号值，看起来"采到了"却是无效数据，排查时容易被误导。
:::

::: warning community 写错会静默超时
SNMP 用 `community` 团体名做口令。团体名不匹配、或设备未对该团体开放访问时，设备通常不回包，`snmp.send()` 返回的
`response.getResponse()` 为 `null`，驱动抛 `ReadPointException("SNMP response is null...")`。这表现为请求超时而非明确的"
鉴权失败"。接入前先在命令行用 `snmpget -v2c -c public <host> 1.3.6.1.2.1.1.1.0` 确认 `host`、`port`、`community`、`oid`
这一组能取到值。
:::

::: warning 防火墙拦 UDP 161 / 设备未启用 SNMP
SNMP 走 UDP 而非 TCP，许多防火墙默认放行 TCP 却拦 UDP；交换机/服务器也常默认关闭 SNMP agent。现象同样是超时。先确认目标设备已启用
SNMP 服务、并放行了管理端到设备 UDP `161` 的入站流量。
:::

接入前，建议先在命令行用 net-snmp 工具验证这条链路是否通——驱动用的是同一套 SNMP4J 语义，命令行取不到值就先别在 DC3 里建设备：

```bash
# 最小连通性验证
snmpget -v2c -c public 192.168.1.20:161 1.3.6.1.2.1.1.1.0
# 取不到值时逐项排除：host 可 ping 通？UDP 161 放行？community 对？SNMP 已启用？
snmpwalk -v2c -c public 192.168.1.20:161 1.3.6.1.2.1.1   # 遍历 system 子树看设备是否应答
```

::: warning version 只能填 v1 / v2c，填 v3 会被当 v2c
驱动 `buildTarget()` 只识别 `v1`（不区分大小写），其余任何值——包括 `v3`——都走 `version2c` 分支。若设备只允许
SNMPv3，用本驱动连不上，且不会有"版本不支持"的明确报错，只会表现为 community 校验失败导致的超时。请确认设备允许 v1/v2c 访问。
:::

::: warning 写入返回 true 不代表设备已接受
`write()` 只要拿到非空 `response` 就返回 `true`，并未校验响应 PDU 的 `errorStatus`。某些设备对只读 OID
或越权写会返回带错误码的响应而非不回包，此时驱动仍判成功。写关键参数后建议回读该 OID 确认实际生效。
:::

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`SnmpDriver`（驱动名 `SNMP Driver`，类型 `DRIVER_CLIENT`，主动连设备）。这是稳定的路由标识，不可随意改。
- **读 / 写 / 订阅能力**：读 ✓、写 ✓、订阅 —，与[驱动能力矩阵](./matrix)一致。驱动作为 SNMP 管理端主动轮询，不监听设备上报，因此没有订阅方向。

::: info 实现状态：可用
`SnmpDriverCustomServiceImpl` 的 `read()`（GET）、`write()`（SET）、`getConnector()`（会话管理）、`health()`、`event()`
（设备更新/删除时销毁会话）均已实现，基于 SNMP4J 的 v1/v2c 收发链路完整可用。已知边界：SNMPv3/USM 未接（见上文 USM
三项说明）、写入未校验响应 `errorStatus`、健康检查为本地会话存活判定而非端到端探测。这些是当前实现的取舍，不影响 v1/v2c
的常规采集与写值。
:::

最小接入示例——把 IP `192.168.1.20:161`、团体名 `public` 的一台交换机接进来，采集它的系统描述（`sysDescr`，OID
`1.3.6.1.2.1.1.1.0`）：

1. 选 `SNMP Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.20`、`port=161`、
   `version=v2c`、`community=public`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个描述[位号](../introduction/concepts/point)（
   `pointTypeFlag=STRING`、`READ_ONLY`），point 属性填 `oid=1.3.6.1.2.1.1.1.0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到设备的系统描述字符串。

完整接入流程见[设备接入](../operation/device-onboarding)。

## 延伸阅读

- [驱动总览](./index) — 驱动的通用模型、注册与生命周期
- [驱动能力矩阵](./matrix) — 28 个驱动的读/写/订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — SNMP 所属网络层，及"协议参数即驱动属性"的统一模型
- [IoT 协议与无线网络](../foundations/iot-protocols) — 网络层的无线与轻量物联网另一半
- [CoAP 驱动](./coap) — 同样跑在 UDP 上的轻量物联网协议
