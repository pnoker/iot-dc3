---
title: OPC UA 驱动
---

<script setup>
import OpcUaDiagram from '../../.vitepress/theme/components/OpcUaDiagram.vue'
</script>


# OPC UA 驱动

`dc3-driver-opc-ua` 把 OPC UA 服务端接入 IoT DC3：作为 OPC UA
客户端连到一个或多个服务端，按[位号](../introduction/concepts/point)上配置的命名空间与标识符周期性读取节点值，并支持向节点写值。读完本页，你能完成一台
OPC UA 设备的接入、配置位号、排查连不上的常见原因。

## 协议背景

OPC UA（OPC Unified Architecture，统一架构）是工业自动化领域的跨平台数据互通标准，PLC、SCADA、MES 和各类边缘网关普遍内置 OPC
UA 服务端，把现场数据以「节点树」的形式对外暴露。它取代了依赖 Windows DCOM 的经典 OPC（即 [OPC DA](./opc-da)），改用平台无关的
`opc.tcp://` 二进制协议（也支持 HTTPS），并把安全（证书、签名、加密）与信息建模内建进规范。

在[物联网四层架构](../foundations/fieldbus)里，OPC UA 属于**网络层（现场总线）**：它是车间设备与上层系统之间的协议边界，向下对接
PLC/控制器，向上把数据递给数据平台。和 Modbus 用寄存器地址、Ethernet/IP 用 CIP 标签不同，OPC UA 用**对象模型**
寻址——每个数据点是一个节点（Node），由一个 **NodeId** 唯一标识。NodeId 由两部分组成：

- **命名空间索引（namespace index）**：一个整数，把不同来源的标识符空间隔开，避免重名。
- **标识符（identifier）**：节点在该命名空间下的名字，可以是字符串、数字或 GUID。本驱动用**字符串标识符**。

例如 `namespace=2`、标识符 `Demo.Static.Float`，就唯一定位到命名空间 2 下名为 `Demo.Static.Float` 的节点。本驱动基于
Eclipse Milo 实现，以 OPC UA 客户端（client）身份主动连接服务端，是典型的「主站轮询」模型——它不监听设备上报，而是按采集周期挨个读节点。

- **驱动名 / code**：`OPC UA Driver` / `OpcUaDriver`
- **类型**：`DRIVER_CLIENT`（主动连服务端）

<OpcUaDiagram lang="zh" />

## 属性配置

OPC UA 的接入参数分两层：**driver 属性**填在[设备](../introduction/concepts/device)上，定位「连哪台服务端」；**point 属性**
填在每个[位号](../introduction/concepts/point)上，定位「读写哪个节点」。两层属性都来自驱动的 `application.yml`
，在创建设备/位号时按需填值，留空则用下表的默认值。

### 驱动属性（设备级 `driver-attribute`）

这三个属性拼成端点地址 `opc.tcp://<host>:<port><path>`，告诉驱动去连哪个 OPC UA 服务端的哪个端点。

| 属性   | code   | 类型     | 默认值         | 说明                  |
|------|--------|--------|-------------|---------------------|
| Host | `host` | STRING | `localhost` | 服务端主机名或 IP          |
| Port | `port` | INT    | `18600`     | 服务端 `opc.tcp` 监听端口  |
| Path | `path` | STRING | `/`         | 端点路径（endpoint path） |

例如 `host=192.168.1.20`、`port=4840`、`path=/milo`，拼成 `opc.tcp://192.168.1.20:4840/milo`。`host` 和 `port` 是必填项（驱动
`validate()` 会校验二者非空），`path` 可留默认 `/`。驱动在发现端点时取服务端返回的**第一个**端点来连接。

### 位号属性（`point-attribute`）

每个采集位号填这两项，二者拼成目标节点的 NodeId。

| 属性        | code        | 类型     | 默认值   | 说明          |
|-----------|-------------|--------|-------|-------------|
| Namespace | `namespace` | INT    | `5`   | 命名空间索引      |
| Tag       | `tag`       | STRING | `TAG` | 字符串标识符（节点名） |

::: tip NodeId = namespace + tag
驱动把 `namespace`（命名空间索引）和 `tag`（字符串标识符）拼成 `NodeId(namespace, tag)` 去读写节点。例如 `namespace=2`、
`tag=Demo.Static.Float` 对应命名空间 2 下名为 `Demo.Static.Float`
的节点。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`
）要和节点的实际值类型对得上——读到的值会被转成字符串上报，写值时则按位号类型选择 OPC UA 数据类型。
:::

::: info 写命令没有单独的属性
OPC UA 驱动**没有 `command-attribute`**。向节点写值时复用位号自己的 `namespace` 和 `tag` 定位目标节点，写值类型由位号类型决定——驱动支持写
`INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` / `STRING` 六种类型（见源码 `writeNode()`）。所以一个可写位号只要配好
`namespace` 和 `tag`，无需再填任何命令属性即可下发写命令。
:::

### 采集与健康检查

这些参数来自 `application.yml` 的 `dc3.driver.schedule` 与 `dc3.driver.health`，是驱动级默认值，不在设备上逐个配置。

| 项    | 配置键                     | 默认值              | 说明            |
|------|-------------------------|------------------|---------------|
| 采集周期 | `schedule.read.cron`    | `0/30 * * * * ?` | 每 30 秒读一轮全部位号 |
| 健康检查 | `health.device.cron`    | `0/15 * * * * ?` | 每 15 秒探活一次    |
| 租约超时 | `health.device.timeout` | `45`（秒）          | 超时未续约则判离线     |

健康检查会拿设备的客户端做一次幂等 `connect()` 探活：连得上判[在线](../introduction/concepts/device)，否则离线。

## 故障排查

::: warning port 默认值是 18600，不是标准 4840
yml 里 `port` 默认 `18600`（本地内置 Milo 示例服务端的端口）。生产环境绝大多数 OPC UA 服务端用标准端口 `4840`
，接入真实设备时务必按服务端实际监听端口填 `port`，不要直接用默认值。`path` 也要和服务端的 endpoint 路径一致：有的服务端是根路径（填
`/`），有的带子路径（如 `/milo`、`/OPCUA/SimulationServer`），填错会连不上。
:::

- **匿名身份被拒**：驱动以匿名身份（`AnonymousProvider`）连接服务端。若服务端强制用户名/密码、禁用匿名访问，连接会被拒绝。需先在服务端放开匿名访问，或为该端点开放匿名策略。

- **设备一直离线**：健康检查每 15 秒做一次 `connect()` 探活，连续失败即判离线。先确认 `host`/`port`/`path` 拼出的端点地址正确、网络可达（
  `telnet host port` 或 `nc -vz host port` 验端口通），再确认服务端进程在跑、防火墙未拦 `opc.tcp` 端口。

- **读到的值为 null 或状态码不为 Good**：驱动读节点时若 `StatusCode` 不是 Good、或值为空，会抛 `ReadPointException` 并*
  *主动断开并剔除该连接**（下一轮重连）。常见原因：NodeId 写错（namespace 或 tag 不存在）、节点无读权限、节点当前无值。用
  UaExpert 等工具核对节点 `ns=<namespace>;s=<tag>` 是否真实存在且可读。

- **读/写超时**：驱动的连接超时 5 秒、读超时 1 秒、写超时 1 秒。网络抖动或服务端响应慢时易超时，超时同样会剔除连接触发重连。若服务端确实慢，需在网络侧排查链路时延，而非调大单点超时。

- **写命令不生效**：写值类型必须落在 `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` / `STRING`
  之内，且要和服务端节点的实际数据类型兼容；类型不匹配时服务端返回非 Good 状态，写命令判失败。确认位号 `pointTypeFlag`
  与服务端节点类型一致，且该节点对客户端有写权限。

- **证书相关报错**：驱动启动时在工作目录 `dc3/opc-ua` 下生成自签名证书 `dc3-opc-ua-client.pfx`（PKCS12，默认口令 `password`
  ，可用环境变量 `OPCUA_KEYSTORE_PASSWORD` 覆盖）。若该目录无写权限、或证书生成失败，驱动会**降级为纯匿名连接**
  （不带客户端证书）。当服务端的安全策略要求客户端证书时，纯匿名连接会握手失败——此时需确保证书目录可写、并把生成的客户端证书在服务端「信任」。

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`OpcUaDriver`——这是驱动在系统里的稳定路由标识，数据/命令链路按它寻址，不可随意改。
- **读能力**：✓ 已实现。按采集周期对每个位号调用 `readValue()`
  读节点，值转字符串后封装为 [PointValue](../introduction/concepts/point-value) 上报。
- **写能力**：✓ 已实现。下发写命令时复用位号 `namespace`/`tag` 定位节点，按位号类型写 `INT`/`LONG`/`FLOAT`/`DOUBLE`/
  `BOOLEAN`/`STRING`。
- **订阅/上报**：— 不提供。本驱动是主站轮询模型，不订阅 OPC UA 服务端的数据变更通知（Subscription/MonitoredItem），只按周期主动读。

以上与[驱动能力矩阵](./matrix)的标注一致（读 ✓ / 写 ✓ / 订阅 —）。

::: info 实现状态：可用
`OpcUaDriverCustomServiceImpl` 的 `read()` / `write()` / `health()` / `validate()` / `event()` 均为完整实现（基于 Eclipse
Milo），非骨架。读节点、写六种类型、连接缓存与失效重连、自签名证书生成、设备更新或删除时清理连接等行为都已落地，可直接接入真实
OPC UA 服务端。
:::

### 最小接入示例

把端点 `opc.tcp://192.168.1.20:4840/milo` 上的一个浮点节点接进来：

1. 选 `OPC UA Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.20`、`port=4840`、
   `path=/milo`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `namespace=2`、`tag=Demo.Static.Float`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

完整接入流程见[设备接入](../operation/device-onboarding)。

## 延伸阅读

- [驱动总览](./index) — 全部驱动与通用接入模型
- [驱动能力矩阵](./matrix) — 各驱动读 / 写 / 订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — OPC UA 所在的网络层与寻址模型
- [OPC DA 驱动](./opc-da) — 经典 OPC（DCOM）版本
