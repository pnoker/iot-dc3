---
title: MQTT 驱动
---

# MQTT 驱动

> **`dc3-driver-mqtt` 把 MQTT 设备接入 IoT DC3**——驱动作为服务端订阅 MQTT 主题，被动接收设备上报的报文解析成位号值，并支持向命令主题发布报文下发命令。

MQTT（Message Queuing Telemetry Transport）是物联网最常用的轻量发布/订阅协议，跑在 TCP 上，默认端口 `1883`（TLS `8883`）。设备不被轮询，而是主动把数据**发布**（publish）到某个**主题**（topic）；平台**订阅**（subscribe）这些主题就能收到上报。常见于传感器、智能硬件、网关经 broker（消息中转服务器，如 EMQX、RabbitMQ MQTT 插件）汇聚数据的场景。

先解释几个本驱动会反复用到的 MQTT 概念：

- **主题（Topic）**：消息的逻辑地址，如 `device/1001/up`。发布方往主题发，订阅方按主题收。
- **QoS（服务质量）**：消息投递保证级别，`0`=最多一次、`1`=至少一次、`2`=恰好一次。等级越高越可靠、开销也越大。
- **JSON 路径（Path）**：从上报报文里定位某字段的点号路径，如 `$.payload` 取根对象下的 `payload`。

与 Modbus、HTTP 这类驱动主动连设备不同，本驱动是 **[驱动](../introduction/concepts/driver) 类型 `DRIVER_SERVER`**：它不主动去"读"设备，而是常驻订阅，等设备把数据推上来。因此它**没有设备级 `driver-attribute` 配置表**——连哪个 broker 由部署时的环境变量（`MQTT_BROKER_HOST` / `MQTT_BROKER_PORT`）决定，设备上不再单独填驱动属性。

::: warning 该驱动当前为骨架实现
源码中 `read()` 是参考桩、`health()` 恒返回在线，协议级 I/O 尚未完整实现（见 `MqttDriverCustomServiceImpl` 的 TODO）。请将它作为接入模板与配置参考，而非生产就绪驱动。
:::

## 驱动名 / code / 类型

- **驱动名 / code**：`MQTT Driver` / `MqttDriver`
- **类型**：`DRIVER_SERVER`（驱动作为服务端，被动接收设备上报）

## 位号配置（`point-attribute`）

每个[位号](../introduction/concepts/point)上填——位号的**写入目标主题**与投递质量（采集靠订阅被动接收，故位号属性只关注下行写命令）：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Command Topic | `commandTopic` | STRING | `commandTopic` | MQTT topic used by the point or device to receive downstream commands |
| Command QoS | `commandQos` | INT | `2` | QoS level for the downstream command topic |

## 写命令配置（`command-attribute`）

可写位号在写命令上填——往哪个主题发、用什么 QoS、报文长什么样：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Command Topic | `commandTopic` | STRING | `commandTopic` | MQTT topic used by the command to publish downstream payload |
| Command QoS | `commandQos` | INT | `2` | QoS level for the command publish topic |
| Payload Template | `payloadTemplate` | STRING | `{}` | Payload template rendered with command params |

下发命令时，驱动用命令参数（外加 `deviceId`/`deviceCode`/`commandCode` 等上下文）渲染 `payloadTemplate` 里的 `${xxx}` 占位符，按 `commandQos` 把渲染后的报文发布到 `commandTopic`。

## 事件配置（`event-attribute`）

设备上报事件时，驱动从订阅到的报文里按路径拆出"事件码"和"事件负载"：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Source Topic | `sourceTopic` | STRING | `eventTopic` | MQTT topic used to receive event payload |
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | JSON path used to resolve event code |
| Payload Path | `payloadPath` | STRING | `$.payload` | JSON path used to resolve event payload |

## 采集与健康

- **采集方式**：定时读取默认**关闭**（`schedule.read.enable=false`）——MQTT 数据靠订阅被动接收，无周期轮询。
- **自定义任务**：驱动内置一个 custom 调度，默认 cron `0/5 * * * * ?`（每 5 秒一次），用于驱动自有的周期逻辑。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把一台往主题 `device/1001/up` 上报、并接收 `device/1001/down` 命令的设备接进来：

1. 部署时把 `MQTT_BROKER_HOST` / `MQTT_BROKER_PORT` 指向你的 broker（开发栈默认 RabbitMQ MQTT 插件 `dc3-rabbitmq:2883`），用 `MQTT Driver` 创建[设备](../introduction/concepts/device)（本驱动无 driver 属性可填）。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个可写[位号](../introduction/concepts/point)，point 属性填 `commandTopic=device/1001/down`、`commandQos=1`。
3. 设备把数据 publish 到订阅的主题后，[位号值](../introduction/concepts/point-value)即被动收下；下发写命令时，驱动按 `payloadTemplate` 渲染报文并发布到 `device/1001/down`。

## 易错点

::: warning 它是服务端，不会去"连"设备
`DRIVER_SERVER` 意味着驱动等设备把数据推上来，而不是主动轮询。如果迟迟收不到[位号值](../introduction/concepts/point-value)，先确认**设备端是否真的在往订阅主题发布**、主题字符串两端是否完全一致（含大小写与层级 `/`），而不是去查驱动的"采集周期"——本驱动定时读取默认就是关的。
:::

::: tip QoS 两端要匹配，下发失败会回退默认
命令下发时若 `commandQos` 配置可用，按该等级发布；取不到或异常时驱动会**回退用默认 QoS** 发，仍尽量把命令发出去。要可靠下发请把发布端与设备订阅端的 QoS 对齐（如双方都用 `1` 或 `2`），避免一端降级导致漏收或重复。
:::

::: tip Payload Template 决定下发报文长什么样
命令不会自动把值塞进报文。需在 `payloadTemplate` 里写好带 `${value}` 等占位符的模板（如 `{"value":${value}}`），驱动才会用命令参数替换后发布。模板留空时按 `{}` 发送空对象。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `commandTopic` / `payloadTemplate` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [HTTP 驱动](./http) — 同为应用层、但驱动主动发起请求的 REST 接入
- [CoAP 驱动](./coap) — 面向受限终端的轻量请求/响应协议
