---
title: Listening Virtual 驱动
---

# Listening Virtual 驱动

> **`dc3-driver-listening-virtual` 把"主动往平台推数据"的 TCP/UDP 设备接入 IoT DC3**——驱动开一个监听端口等设备来连，从设备推上来的字节流里按位号配置截取并解析出值。

工业现场有一类设备不等人来问，而是自己周期性地把数据"推"出去：GPS 定位器、环境监测盒子、各种走私有二进制报文的传感终端，常见做法就是连到一个固定的 IP:端口，把一段字节流发过来。本驱动就是为这类设备准备的——它是一个 **监听型（被动）[驱动](../introduction/concepts/driver)**，自己启动 TCP 和 UDP 两个监听端口，[设备](../introduction/concepts/device)主动连上来推数据，驱动负责把字节流解析成[位号值](../introduction/concepts/point-value)。它不会主动去"读"设备，因此适用于：自带上报逻辑的 GPS/北斗终端、推送二进制帧的传感网关、以及任何"客户端连服务端、服务端被动收"的私有 TCP/UDP 协议。

下面先解释两个本驱动特有的概念，后面配置表会反复用到：

- **报文关键字（Keyword）**：设备推上来的报文里，紧跟设备名之后的 1 个字节，用十六进制表示（如 `62`）。同一台设备可以用不同关键字区分不同类型的报文，驱动据此决定这一帧该解析给哪个[位号](../introduction/concepts/point)。
- **字节区间（Start / End）**：在报文里截取数据的字节偏移。`start` 是起始偏移（含），`end` 是结束偏移（不含）；定长数值位号只看 `start` 起的固定字节数，`coordinate` 字符串位号才用 `start..end` 这段区间。

## 报文格式

设备每次推上来的报文结构固定为：

```
[ 设备名 22 字节 ][ 关键字 1 字节 ][ 数据载荷 变长 ]
```

- 前 22 字节是设备名（驱动据此解析出[设备](../introduction/concepts/device) ID，需与平台上的设备一一对应）。
- 第 23 字节（偏移 22）是关键字，与位号上配的 `key` 比对。
- 之后是数据载荷，每个位号按自己的 `start`/`end` 从载荷里取一段，**按位号名决定解析类型**。

## 驱动名 / code / 类型

- **驱动名 / code**：`Listening Virtual TCP/UDP Driver` / `ListeningVirtualDriver`
- **类型**：`DRIVER_SERVER`（驱动作监听端，被动收设备推上来的数据）

## 驱动配置（设备级 `driver-attribute`）

本驱动**不声明任何设备级 `driver-attribute`**——监听端口是驱动进程级配置（TCP 默认 `6270`、可用环境变量 `TCP_PORT` 覆盖；UDP 默认 `6271`、可用 `UDP_PORT` 覆盖），所有接入配置都落在[位号](../introduction/concepts/point)上。

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填这些[属性](../introduction/concepts/attribute-config)，告诉驱动：认哪个关键字、从报文哪一段取（解析类型则由位号名决定，见下方说明）：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Keyword | `key` | STRING | `62` | Packet identification keyword |
| Start Byte | `start` | INT | `0` | Inclusive start byte offset |
| End Byte | `end` | INT | `8` | Exclusive end byte offset |
| Type | `type` | STRING | `string` | Required attribute; presence-checked only, not used to choose the parse type |

::: tip key 用十六进制，与报文关键字逐字比对
`key` 填的是报文第 23 字节的十六进制值（如默认 `62`）。一帧报文进来，驱动只把关键字相同的位号拿来解析；关键字对不上的位号这一帧不出值。同一台设备的不同位号可以用同一个 `key`（一帧里同时取多个字段），也可以用不同 `key`（不同帧各管各的）。
:::

::: warning 解析类型由位号名决定，不是 `type` 属性
驱动按 **位号名（pointName）** 选解析方式，受支持的位号名只有这 6 个：`altitude`→float、`speed`→double、`level`→long、`direction`→int、`locked`→boolean、`coordinate`→string。位号名必须是上述之一，否则该位号这一帧解析为空值、采不到数据。`type` 属性只在校验时做存在性检查（必填），解析时从不读取它。
:::

本驱动是被动监听、没有写回设备的命令属性，因此**没有 `command-attribute`**。底层保留了沿设备 TCP 连接回写字节的能力，但未暴露为可配置的写命令。

## 采集与健康

- **采集周期**：本驱动**不做主动轮询**——`schedule.read.enable=false`，数据完全由设备推送触发。驱动另有一个 `0/5 * * * * ?` 的内部定时任务（`schedule.custom`，每 5 秒）用于驱动自身的周期性维护，不向设备发起读取。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——设备每次推数据即刷新租约，超时未推则判离线。在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把一台通过 TCP 推送数据的 GPS 终端接进来：

1. 用 `Listening Virtual TCP/UDP Driver` 创建[设备](../introduction/concepts/device)（本驱动无 driver 属性，设备本身不需填连接参数）。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个字符串[位号](../introduction/concepts/point)，**位号名必须取支持的名字之一**（这里用 `coordinate`，它解析为字符串；`pointTypeFlag=STRING`），point 属性填 `key=62`、`start=23`、`end=31`、`type=string`——即认关键字 `62` 的报文，从载荷起始处取 8 个字节当字符串。位号名若不在 `altitude/speed/level/direction/locked/coordinate` 之列，驱动采不到值。
3. 启动驱动；让设备把"22 字节设备名 + 1 字节 `0x62` + 载荷"推到驱动的 TCP `6270` 端口，几秒内就能在[位号值](../introduction/concepts/point-value)里看到解析结果。

## 易错点

::: warning 报文前 22 字节必须是平台上的设备 ID
驱动按报文前 22 字节解析出设备 ID 去匹配平台上的[设备](../introduction/concepts/device)。设备推数据时**必须把对应的数字设备 ID 左对齐填进这 22 字节**——解析不出数字、或匹配不到设备，这一帧会被直接丢弃且不报错。先在平台建好设备拿到 ID，再配进设备端固件。
:::

::: warning start/end 的偏移是相对整帧报文，不是相对载荷
位号的 `start`/`end` 是在**整帧报文**上的字节偏移。前 23 字节被设备名和关键字占用，所以载荷第一个字节的偏移是 `23`，而不是 `0`。要取载荷开头的数据，`start` 应从 `23` 起算；按默认 `start=0` 会落在设备名里取到错误的值。
:::

::: tip 定长数值位号只看 start，coordinate 才用 end
定长数值位号（`altitude`/`speed`/`level`/`direction`/`locked`）按各自固定字节宽度从 `start` 起读（如 `altitude` 读 4 字节、`speed` 读 8 字节），此时 `end` 不参与计算。只有 `coordinate` 才按 `start..end` 这段区间截取字符串。报文长度不够、偏移越界时，该位号这一帧不出值。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `key` / `start` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [HTTP 驱动](./http) — 另一种把外部数据源接入平台的方式
