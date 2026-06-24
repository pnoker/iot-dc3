---
title: Virtual 驱动
---

# Virtual 驱动

> **`dc3-driver-virtual` 是 IoT DC3 的虚拟（仿真）驱动**——它不连接任何真实设备，而是按采集周期为[位号](../introduction/concepts/point)生成随机[位号值](../introduction/concepts/point-value)，并能模拟命令下发与事件上报。

Virtual 不是某种现场总线协议，而是一个**仿真驱动**：它把 IoT DC3 的整套接入流程（建[设备](../introduction/concepts/device)、配[物模型](../introduction/concepts/profile)、跑采集、看[位号值](../introduction/concepts/point-value)）走通，但底层不发任何网络报文，所有数据都是本地随机造的。适用场景：

- **跑通平台、做演示**——没有真实 PLC/传感器时，先用它把端到端链路验证一遍。
- **学习驱动模型**——它是最简单的[驱动](../introduction/concepts/driver)，源码（`VirtualDriverCustomServiceImpl`）就是写自定义驱动的参考样板。
- **压测与联调**——批量造设备和位号，观察平台在数据流下的表现。

读取时按位号的数据类型造值：`STRING` 类型固定返回 `abcd1234`，`BOOLEAN` 类型返回随机真/假，其余类型返回 `0~100` 之间的随机浮点数。

- **驱动名 / code**：`Virtual Driver` / `VirtualDriver`
- **类型**：`DRIVER_CLIENT`（驱动作主动方，按周期主动产出数据）

## 驱动配置（设备级 `driver-attribute`）

接入一台虚拟设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)。注意：虚拟驱动并不真正连接 `host:port`，这两项只是占位，保持与真实驱动一致的配置形态，方便照搬演练。

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | Ip |
| Port | `port` | INT | `18600` | Port |

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Tag | `tag` | STRING | `TAG` | Point tag name |

::: tip 造什么值由位号类型决定，与 tag 无关
虚拟驱动忽略 `tag` 的具体内容，造值只看位号本身的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）：`STRING` 出 `abcd1234`、`BOOLEAN` 出随机布尔、数值类出 `0~100` 随机浮点。`tag` 仅作位号在该设备上的标识占位。
:::

## 写命令配置（`command-attribute`）

虚拟驱动支持模拟命令下发（不真正写设备）。下发时用命令参数渲染 `payloadTemplate` 得到请求载荷，再用 `responseTemplate` 渲染并解析出一份模拟响应：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Payload Template | `payloadTemplate` | STRING | `${value}` | Template rendered with command params |
| Response Template | `responseTemplate` | STRING | `{}` | Mock response template |

::: tip 模板用 `${...}` 占位，由命令上下文渲染
模板里的 `${value}`、`${deviceCode}`、`${commandCode}` 等占位会被命令参数与设备/命令上下文替换。`responseTemplate` 是 JSON 时，其字段会被原样解析进命令返回结果；非 JSON 则整体作为 `response` 返回。这套流程不接触任何真实设备，纯属仿真。
:::

## 事件配置（`event-attribute`）

虚拟驱动还会周期性地为设备模拟[事件](../introduction/concepts/event)上报（每 30 秒一轮）。事件属性用 JSON Path 告诉驱动：从模拟报文里哪个路径取事件 code、哪个路径取事件载荷：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | JSON path used to resolve event code |
| Payload Path | `payloadPath` | STRING | `$.payload` | JSON path used to resolve event payload |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒造一轮值）。驱动另有一个 `0/5 * * * * ?` 的内部定时任务（`schedule.custom`），事件上报在其中按 30 秒间隔触发。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

不需要任何真实硬件，把一台虚拟设备接进来看到数据流动：

1. 选 `Virtual Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=localhost`、`port=18600`（保持默认即可）。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`），point 属性填 `tag=temperature`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到 `0~100` 之间不断变化的随机值。

## 易错点

::: warning host/port 是占位，连不连得上都不影响出值
虚拟驱动从不真正建立到 `host:port` 的连接，所以即使填一个不存在的地址也照样产出随机值。换言之，**用它验证不了真实的网络连通性**——它只用来跑通平台流程；要测真实链路请换对应协议的驱动。
:::

::: tip 位号类型决定值的形态，先把 `pointTypeFlag` 配对
想看布尔翻转就把位号配成 `BOOLEAN`，想看连续曲线就配成数值类型（如 `FLOAT`）。位号类型配错只会得到不符合预期的造值形态，而不会报错——这点和接真实设备一致：类型是位号侧的约定。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `tag` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Listening Virtual 驱动](./listening-virtual) — 被动监听版的仿真/接入驱动
