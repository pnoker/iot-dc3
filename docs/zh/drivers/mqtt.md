---
title: MQTT 驱动
---

<script setup>
import MqttDiagram from '../../.vitepress/theme/components/MqttDiagram.vue'
</script>


# MQTT 驱动

> **`dc3-driver-mqtt` 把 MQTT 设备接入 IoT DC3**——驱动作为服务端常驻订阅 MQTT 主题，被动接收设备 publish
> 上来的报文、解析成[位号值](../introduction/concepts/point-value)，并支持向命令主题 publish 报文下发写命令。这页讲清它消费什么
> broker、位号/命令/事件三类属性怎么填、收不到值时怎么排查，以及它在平台里是哪种驱动、实现到了哪一步。

读完你能：用 `MQTT Driver` 接一台"自己往主题上报、可被下发命令"的设备，并知道当链路不通时该看哪里。

## 协议背景

MQTT（Message Queuing Telemetry Transport）是物联网事实上的轻量**发布/订阅**消息总线，跑在 TCP 上，默认端口 `1883`、TLS
`8883`。它的语义和工业总线的"主站轮询每台设备"完全相反：设备不被轮询，而是主动把数据 **publish（发布）** 到某个 **topic（主题）
**；平台 **subscribe（订阅）** 这些主题就能收到上报。发布方与订阅方通过中间的 **broker**（消息中转服务器，如
EMQX、Mosquitto、RabbitMQ 的 MQTT 插件）解耦，互不需要知道对方地址、也无需同时在线——这正是海量、低功耗、广域设备场景下省电、可横向扩展的关键。

在[物联网四层参考架构](../foundations/iot-protocols)里，MQTT 属于**网络层**的"应用层消息协议"一支：它定义"
一条消息长什么样、怎么投递、可靠到什么程度"，与底层用 Wi-Fi 还是 NB-IoT 等无线接入正交。关于 MQTT 与 CoAP/LwM2M/HTTP
的选型权衡，见[网络层章节](../foundations/iot-protocols)。

先解释几个本驱动会反复用到的 MQTT 概念：

- **主题（Topic）**：消息的逻辑地址，如 `device/1001/up`。发布方往主题发，订阅方按主题收。订阅可用通配符 `+`（匹配一层）和 `#`
  （匹配末尾任意层）。
- **QoS（服务质量）**：消息投递保证级别，`0`=最多一次、`1`=至少一次、`2`=恰好一次。等级越高越可靠、开销也越大，发布与订阅两端各自声明、按较弱一方生效。
- **JSON 路径（Path）**：从上报报文里定位某字段的点号路径，如 `$.payload` 取根对象下的 `payload`、`$.eventCode` 取事件码字段。

与 Modbus、HTTP 这类主动连设备的驱动不同，本驱动是 **[驱动](../introduction/concepts/driver) 类型 `DRIVER_SERVER`**
：它不主动去"读"设备，而是常驻订阅、等设备把数据推上来。因此它**没有设备级 `driver-attribute` 配置表**——连哪个 broker
是部署级配置（见下文），不在设备上逐个填。

## 属性配置

MQTT 驱动的配置分两层：**broker 连接**是部署级的（整台驱动连同一个 broker），**位号 / 命令 / 事件属性**
是设备/位号级的（决定每个测点往哪个主题写、从哪个主题取事件）。

### Broker 连接（部署级，环境变量）

驱动通过 `dc3.driver.mqtt.*` 一组配置连接 broker，其取值来自部署时的环境变量。关键项：

| 配置        | 环境变量                                           | 默认值                                           | 说明                                                                          |
|-----------|------------------------------------------------|-----------------------------------------------|-----------------------------------------------------------------------------|
| broker 地址 | `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT`        | `dc3-rabbitmq` / `1883`（dev profile 为 `2883`） | broker 主机与端口，拼成连接 URL（默认明文 `tcp://host:port`；TLS 走 `ssl://host:8883` 为生产可选） |
| 用户名 / 密码  | `MQTT_USERNAME` / `MQTT_PASSWORD`              | `dc3` / 空（docker-compose 栈注入 `dc3dc3dc3`）     | 认证凭据（认证类型支持 `NONE` / `USERNAME` / `CLIENT_ID` / `X509`）；密码应用级回退为空，随部署注入     |
| 保活间隔      | 无 env 绑定（`dc3.driver.mqtt.keep-alive`）         | `15`（秒）                                       | 客户端心跳间隔，硬编码默认                                                               |
| 完成超时      | 无 env 绑定（`dc3.driver.mqtt.completion-timeout`） | `3000`（毫秒）                                    | 发布操作的等待超时，硬编码默认                                                             |
| 批量阈值      | `MQTT_BATCH_SPEED` / `MQTT_BATCH_INTERVAL`     | `100` / `5`                                   | 上报批量：满 100 条或满 5 秒先到先发                                                     |

::: info broker 默认是 RabbitMQ 的 MQTT 插件
默认的 MQTT broker 是 **RabbitMQ 的 MQTT 插件**（`dc3-rabbitmq`），由 `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT`
指定，docker-compose 栈注入 `dc3-rabbitmq:1883`（dev profile YAML 的端口回退为 `2883`）。**EMQX** 是
`docker-compose-optional.yml` 里的可选 broker（宿主机映射端口 `31883`），并非默认。RabbitMQ 的 MQTT 插件与平台内部用于服务间消息的
**RabbitMQ AMQP**（另有 `dc3.e.mqtt` 桥接交换机）是同一 broker 的两种协议——接 MQTT 设备时请把上述两个环境变量指向你真正的
MQTT broker。生产跨公网时应启用 TLS（`8883` / X509 证书）。
:::

### 位号配置（`point-attribute`）

每个[位号](../introduction/concepts/point)上填——位号的**写入目标主题**与投递质量。采集靠订阅被动接收，故位号属性只关注下行写命令：

| 属性            | code           | 类型     | 默认值            | 说明                   |
|---------------|----------------|--------|----------------|----------------------|
| Command Topic | `commandTopic` | STRING | `commandTopic` | 位号/设备接收下行命令的 MQTT 主题 |
| Command QoS   | `commandQos`   | INT    | `2`            | 下行命令主题的 QoS 级别       |

下发写命令时，驱动从位号属性取 `commandTopic`，按 `commandQos`（取不到或异常时回退默认 QoS）把要写的值 publish 到该主题。

### 写命令配置（`command-attribute`）

可写位号在写命令上填——往哪个主题发、用什么 QoS、报文长什么样：

| 属性               | code              | 类型     | 默认值            | 说明                       |
|------------------|-------------------|--------|----------------|--------------------------|
| Command Topic    | `commandTopic`    | STRING | `commandTopic` | 命令 publish 下行报文的 MQTT 主题 |
| Command QoS      | `commandQos`      | INT    | `2`            | 命令发布主题的 QoS 级别           |
| Payload Template | `payloadTemplate` | STRING | `{}`           | 用命令参数渲染的报文模板             |

执行命令时，驱动用命令参数（外加 `deviceId` / `deviceCode` / `deviceName` / `commandId` / `commandCode` / `commandName`
等上下文）替换 `payloadTemplate` 里的 `${xxx}` 占位符，按 `commandQos` 把渲染后的报文 publish 到 `commandTopic`，并返回
`topic` / `qos` / `payload` 作为执行回执。

### 事件配置（`event-attribute`）

设备上报事件时，驱动从订阅到的报文里按主题与路径拆出"事件码"和"事件负载"：

| 属性              | code            | 类型     | 默认值           | 说明                               |
|-----------------|-----------------|--------|---------------|----------------------------------|
| Source Topic    | `sourceTopic`   | STRING | `eventTopic`  | 接收事件负载的 MQTT 主题（支持 `+` / `#` 通配） |
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | 解析事件码的 JSON 路径                   |
| Payload Path    | `payloadPath`   | STRING | `$.payload`   | 解析事件负载的 JSON 路径                  |

当收到的主题与 `sourceTopic` 匹配（精确或通配），驱动按 `eventCodePath` 取事件码、按 `payloadPath`
取负载，对码值对得上、且事件处于启用态的设备事件，组装成事件上报送往数据中心。

## 数据如何被接收

值的"读"在 MQTT 里不是主动发起的请求，而是订阅消息到达时的回调。下图是一条上报值从设备到平台的路径——设备与驱动都只和
broker 打交道：

<MqttDiagram lang="zh" />

报文要被收成[位号值](../introduction/concepts/point-value)，须能解析出 `deviceId` 与 `pointId`（否则该条被跳过）；解析失败只记一条
warn 日志、不影响其他消息。批量消息走 `receiveValues()` 合并发送，命中批量阈值（`MQTT_BATCH_SPEED` / `MQTT_BATCH_INTERVAL`
）即刷出。

## 故障排查

::: warning 它是服务端，不会去"连"设备
`DRIVER_SERVER`
意味着驱动等设备把数据推上来，而不是主动轮询。如果迟迟收不到[位号值](../introduction/concepts/point-value)，*
*先确认设备端是否真的在往订阅主题发布**、主题字符串两端是否完全一致（含大小写与层级 `/`），而不是去查驱动的"采集周期"
——本驱动定时读取默认就是关的（`schedule.read.enable=false`）。
:::

- **broker 连不通**：检查 `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` 是否指向真正的 MQTT broker（默认 `dc3-rabbitmq:1883`），以及
  `MQTT_USERNAME` / `MQTT_PASSWORD` 与 broker 上的账号是否一致；公网/TLS 场景确认端口走的是 `8883` 且证书匹配。
- **收到消息但没有位号值**：报文必须能解析出 `deviceId` 与 `pointId`，否则会被静默跳过。看驱动日志里的
  `MQTT point value parse failed` warn——多半是上报 JSON 结构不含这两个字段或非合法 JSON。
- **QoS 不匹配导致漏收/重复**：发布端与设备订阅端 QoS 要对齐。`commandQos` 取不到或异常时驱动会**回退默认 QoS**
  发，仍尽量把命令发出去，但若两端档位不一致仍可能降级——可靠下发请让双方都用 `1` 或 `2`。
- **下发报文为空或占位符没被替换**：命令不会自动把值塞进报文，须在 `payloadTemplate` 里写好带 `${value}` 等占位符的模板（如
  `{"value":${value}}`）；模板留空时按 `{}` 发空对象。占位符名要和命令参数键一致才会被替换。
- **事件收不到**：确认收到的主题与 `sourceTopic` 匹配（通配符 `+` 只匹配一层、`#` 只能在末尾），`eventCodePath` 取出的码值与设备事件的
  `eventCode` 对得上，且该事件处于启用态。
- **设备"在线"但没数据**：MQTT 是被动推送，"长时间没收到"不代表链路一定断。在线判断走租约/保活，而非采集周期——健康检查默认
  cron `0/15 * * * * ?`、租约超时 `45 秒`，机制见[设备](../introduction/concepts/device)。

## 在 IoT DC3 中如何落地

- **驱动名 / code**：`MQTT Driver` / `MqttDriver`
- **类型**：`DRIVER_SERVER`（驱动作为服务端，被动接收设备上报）
- **能力**（与[驱动能力矩阵](./matrix)一致）：读 `—`、写 `✓`、订阅/上报 `✓`——值经订阅被动到达，无主动读；命令可下发、事件可上报。

::: info 实现状态：数据接收、命令下发与健康检查已实现，`initial()` 为骨架
据 `MqttDriverCustomServiceImpl` 与 `MqttReceiveServiceImpl` 源码：**数据接收**（解析为位号值并转发、事件上报与主题匹配）、*
*写命令**（`write()` / `execute()` 发布报文、QoS 回退、模板渲染）、**`health()` 健康检查**（监听 `MqttSubscribedEvent` /
`MqttConnectionFailedEvent`，实时反映 broker 连接态）均已实现；`read()` 按 pub/sub 语义恒返回 `null`
（数据靠订阅被动到达，非缺陷）。仍为参考桩的是 `initial()` 空的初始化模板。
:::

最小接入示例——接一台往 `device/1001/up` 上报、并接收 `device/1001/down` 命令的设备：

1. 部署时把 `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` 指向你的 broker（默认 `dc3-rabbitmq:1883`，即 RabbitMQ 的 MQTT 插件），用
   `MQTT Driver` 创建[设备](../introduction/concepts/device)（本驱动无 driver 属性可填）。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个可写[位号](../introduction/concepts/point)，point 属性填
   `commandTopic=device/1001/down`、`commandQos=1`。
3. 设备把数据 publish 到订阅主题后，[位号值](../introduction/concepts/point-value)即被动收下；下发写命令时，驱动按
   `payloadTemplate` 渲染报文并 publish 到 `device/1001/down`。

完整接入流程见[设备接入](../operation/device-onboarding)。

## 延伸阅读

- [驱动总览](./index) — 28 个驱动的全景与分类
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [网络层：物联网协议](../foundations/iot-protocols) — MQTT 与 CoAP/LwM2M/HTTP 的选型权衡
- [CoAP 驱动](./coap) — 面向受限终端的轻量请求/响应协议
