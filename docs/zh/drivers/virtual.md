---
title: Virtual 驱动
---

# Virtual 驱动

`dc3-driver-virtual` 是 IoT DC3 的**虚拟（仿真）驱动**：它不连接任何真实设备，而是按采集周期为[位号](../introduction/concepts/point)生成随机[位号值](../introduction/concepts/point-value)，并模拟命令执行与事件上报。读完本页你能用它把整套接入链路跑通，并理解它"哪些能力是真实实现、哪些只是占位"。

> 你在这里：还没有真实 PLC/传感器，想先把平台端到端验证一遍，或想找一份最简单的[驱动](../introduction/concepts/driver)样板来照着写自己的协议。下一步可看[设备接入](../operation/device-onboarding)。

## 协议背景

Virtual 不是某种现场总线或工业协议，而是一个**仿真驱动**——它把 IoT DC3 的整套接入流程（建[设备](../introduction/concepts/device)、配[模板 Profile](../introduction/concepts/profile)、跑采集、看[位号值](../introduction/concepts/point-value)）走通，但底层不发任何网络报文，所有数据都是本地随机造的。因为没有真实协议层，所以它**不属于**物联网四层架构里的某一层网络协议，而是平台侧用来演练、教学、压测的"无设备接入器"。典型用途：

- **跑通平台、做演示**——没有真实硬件时，先用它把端到端链路验证一遍。
- **学习驱动模型**——它是最简单的驱动，源码 `VirtualDriverCustomServiceImpl` 就是写自定义驱动的参考样板。
- **压测与联调**——批量造设备和位号，观察平台在持续数据流下的表现。

读取时按位号的数据类型造值：`STRING` 固定返回 `abcd1234`，`BOOLEAN` 返回随机真/假，其余类型返回 `0~100` 之间的随机浮点数。造什么值只看位号本身的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`），与 `tag` 内容无关。

::: info 虚拟驱动没有真实协议层
其他驱动页会链接到对应协议规范，本页没有——Virtual 不实现任何线缆协议。它的"链路"完全在 IoT DC3 内部，更多协议驱动请见[驱动总览](./index)。
:::

## 属性配置

属性分两类：填在[设备](../introduction/concepts/device)上的**驱动属性**（`driver-attribute`）和填在每个[位号](../introduction/concepts/point)上的**位号属性**（`point-attribute`）。此外驱动还声明了命令属性与事件属性，用于命令执行和事件上报的模板渲染。这些属性的定义来自驱动模块的 `application.yml`，下面逐项说明它们的作用与取值来源。

### 驱动属性（设备级 `driver-attribute`）

接入一台虚拟设备时，在设备上填以下两项。注意：虚拟驱动**并不真正连接** `host:port`，这两项只是占位，保持与真实驱动一致的配置形态，方便照搬演练。

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | 设备 IP（占位，不实际建连） |
| Port | `port` | INT | `18600` | 设备端口（占位，不实际建连） |

### 位号属性（`point-attribute`）

每个采集位号上填一项 `tag`，作为该位号在设备上的标识占位：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Tag | `tag` | STRING | `TAG` | 位号标签名 |

::: tip 造值由位号类型决定，与 tag 无关
虚拟驱动忽略 `tag` 的具体内容，造值只看位号的数据类型 `pointTypeFlag`：`STRING` 出 `abcd1234`、`BOOLEAN` 出随机布尔、数值类出 `0~100` 随机浮点。想看布尔翻转就把位号配成 `BOOLEAN`，想看连续曲线就配成 `FLOAT`。
:::

### 命令属性（`command-attribute`）

虚拟驱动实现了**命令执行**（`execute()`）：下发命令时，用命令参数与设备/命令上下文渲染 `payloadTemplate` 得到请求载荷，再渲染并解析 `responseTemplate` 得到一份模拟响应。整个过程不接触任何真实设备。

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Payload Template | `payloadTemplate` | STRING | `${value}` | 用命令参数渲染出的请求载荷模板 |
| Response Template | `responseTemplate` | STRING | `{}` | 模拟响应模板 |

::: tip 模板用 `${...}` 占位，由命令上下文渲染
模板里的 `${value}`、`${deviceCode}`、`${commandCode}`、`${deviceId}`、`${commandName}` 等占位，会被命令参数与设备/命令上下文逐一字符串替换。`responseTemplate` 是 JSON 对象时，其字段会被原样解析进命令返回结果；非 JSON 则整体作为 `response` 字段返回。返回里还会带上渲染后的 `payload`。
:::

### 事件属性（`event-attribute`）

虚拟驱动会周期性地为设备模拟[事件](../introduction/concepts/event)上报（每 30 秒一轮）。事件属性用类 JSON Path 告诉驱动：从模拟报文里哪个路径取事件 code、哪个路径取事件载荷。

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Event Code Path | `eventCodePath` | STRING | `$.eventCode` | 解析事件 code 的 JSON 路径 |
| Payload Path | `payloadPath` | STRING | `$.payload` | 解析事件载荷的 JSON 路径 |

::: warning 仅支持简单点号路径，不是完整 JSONPath
驱动内部 `resolvePath()` 只按 `.` 逐段在 Map 里取值（如 `$.payload.value`），不支持数组下标、过滤表达式等完整 JSONPath 语法。模拟报文形如 `{"eventCode":"...","payload":{"value":...,"deviceCode":...,"source":"virtual"}}`，取不到时回退到事件自身的 `eventCode`。
:::

## 故障排查

Virtual 几乎不会因"连不上设备"而失败（它根本不建连），所以常见问题集中在调度、类型与配置上：

- **30 秒内看不到位号值**：确认驱动已注册上线、设备绑定的 [Profile](../introduction/concepts/profile) 下挂了启用的位号，且采集调度 `dc3.driver.schedule.read.enable=true`（默认开，cron `0/30 * * * * ?`）。首轮值最长要等一个采集周期。
- **位号值形态不符合预期**（想要布尔却出数字）：检查位号的 `pointTypeFlag`。类型配错只会造出不符合预期形态的值，**不会报错**——类型是位号侧的约定。
- **写命令总是失败 / 不回显**：这是预期行为。虚拟驱动的点位写 `write()` 是占位实现，永远返回 `false`，详见下文"在 IoT DC3 中如何落地"。要看可工作的下行链路，请用[命令执行](#属性配置)（`execute()`，对应命令属性）或换接真实驱动。
- **收不到事件上报**：事件上报由内部定时任务 `dc3.driver.schedule.custom`（cron `0/5 * * * * ?`）驱动、并按 30 秒间隔节流；同时设备下必须有**启用状态**的事件定义，否则该轮跳过。
- **设备显示离线**：健康检查 cron `0/15 * * * * ?`、租约超时 `45 秒`。若驱动进程停了或未按周期心跳，设备会被判离线——在线机制见[设备](../introduction/concepts/device)。
- **host/port 填错却照样出值**：见下方易错提示——虚拟驱动从不校验也不连接 `host:port`。

::: warning host/port 是占位，连不连得上都不影响出值
虚拟驱动从不真正建立到 `host:port` 的连接，即使填一个不存在的地址也照样产出随机值。换言之，**用它验证不了真实的网络连通性**——要测真实链路请换对应协议的驱动。
:::

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`VirtualDriver`（驱动名 `Virtual Driver`）。这是稳定的路由标识，平台据此把设备、命令、事件路由到本驱动，不要随意改。
- **类型**：`DRIVER_CLIENT`——驱动作主动方，按周期主动产出数据。
- **能力**（与[驱动能力矩阵](./matrix)对齐）：

| 能力 | 状态 | 说明 |
|---|---|---|
| 读 `read()` | 可用 | 按位号类型造随机值，已完整实现 |
| 写 `write()` | 占位 | 点位写永远返回 `false`，不落任何设备 |
| 命令执行 `execute()` | 可用 | 模板渲染 + 模拟响应，已完整实现 |
| 事件上报 `schedule()` | 可用 | 每 30 秒模拟一轮事件上报 |

::: warning 点位写是占位实现
`write()` 在源码中直接返回 `false`——通过位号写值的请求永远"失败"，不会改变任何状态。这与[驱动能力矩阵](./matrix)里 Virtual 的「写 = —」一致。需要可工作的下行能力时，用命令执行（`execute()`，配 `command-attribute`），或换接真实驱动。
:::

::: info 事件/命令是真实实现，但与矩阵的"协议订阅"含义不同
矩阵把 Virtual 的「订阅/上报」标为 `—`，指的是它没有真实协议层的被动订阅。但代码里 `execute()` 与 `schedule()`（事件上报）都是完整实现的仿真能力——本页据源码如实标注。校验类方法 `validate()`/`validatePoint()` 当前不做实质校验，恒为通过。
:::

### 最小接入示例

不需要任何真实硬件，把一台虚拟设备接进来看到数据流动：

1. 选 `Virtual Driver` 创建[设备](../introduction/concepts/device)，驱动属性填 `host=localhost`、`port=18600`（保持默认即可）。
2. 给设备绑定的 [Profile](../introduction/concepts/profile) 加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`），位号属性填 `tag=temperature`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到 `0~100` 之间不断变化的随机值。

## 延伸阅读

- [驱动总览](./index) — 按类别挑选协议，进入各驱动页
- [驱动能力矩阵](./matrix) — 各驱动读/写/订阅能力的真实实现一览
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [自定义驱动](../development/driver-authoring) — 基于 `virtual` 模板实现自己的协议驱动
- [Listening Virtual 驱动](./listening-virtual) — 被动监听版的仿真/接入驱动
