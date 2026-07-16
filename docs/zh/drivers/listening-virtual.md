---
title: Listening Virtual 驱动
---

<script setup>
import ListeningVirtualDiagram from '../../.vitepress/theme/components/ListeningVirtualDiagram.vue'
</script>


# Listening Virtual 驱动

> `dc3-driver-listening-virtual` 把"自己往平台推数据"的 TCP/UDP 设备接入 IoT
> DC3。驱动开一个监听端口等设备来连，从设备推上来的字节流里按[位号](../introduction/concepts/point)
> 配置截取并解析出值。读完本页，你能配出第一个监听位号、让 GPS/北斗一类终端把二进制帧推进平台。

工业现场有一类设备不等人来问，而是自己周期性地把数据"推"出去：GPS 定位器、环境监测盒子、各种走私有二进制报文的传感终端。常见做法就是连到一个固定的
IP:端口，把一段字节流发过来。本驱动正是为这类场景准备的——它是一个**监听型（被动）驱动**，自启 TCP 与 UDP
两个监听端口，设备主动连上来推数据，驱动把字节流解析成[位号值](../introduction/concepts/point-value)。它不会主动去"读"设备。

## 协议背景

这是一个**虚拟/测试驱动**，没有真实的标准协议层——报文格式是本驱动自定的、最小够用的二进制约定，专门用来演示和打通"
设备主动上报"这条链路。在物联网四层架构里，它落在**网络层**：靠 TCP/UDP 承载设备到平台的字节流，由平台被动接收。真实项目里你可以拿它当模板，把私有上报协议的解析逻辑替换进去。

适用场景：

- 自带上报逻辑的 GPS/北斗终端、推送二进制帧的传感网关；
- 任何"客户端连服务端、服务端被动收"的私有 TCP/UDP 协议；
- 想在没有真实硬件时，验证"推送 → 解析 → 落库"全链路。

在动手前，先记住两个本驱动特有的概念，后面配置表会反复用到：

- **报文关键字（Keyword）**：设备推上来的报文里，紧跟设备名之后的 1 个字节，用十六进制表示（如 `62`
  ）。同一台设备可以用不同关键字区分不同类型的报文，驱动据此决定这一帧该解析给哪个位号。
- **字节区间（Start / End）**：在报文里截取数据的字节偏移。`start` 是起始偏移（含），`end` 是结束偏移（不含）；定长数值位号只看
  `start` 起的固定字节数，`coordinate` 字符串位号才用 `start..end` 这段区间。

报文结构固定为：设备名 22 字节 + 关键字 1 字节 + 数据载荷变长。

<ListeningVirtualDiagram lang="zh" />

- 前 22 字节是设备名，驱动用 `Long.parseLong` 把它解析成[设备](../introduction/concepts/device) ID（必须与平台上的设备一一对应）。
- 第 23 字节（偏移 22）是关键字，与位号上配的 `key` 逐字比对。
- 之后是数据载荷，每个位号按自己的 `start`/`end` 从报文里取一段，**解析类型由位号名（pointName）决定**，而不是 `type` 属性。

## 属性配置

本驱动**不声明任何设备级 `driver-attribute`**
——监听端口是驱动进程级配置，所有接入细节都落在[位号](../introduction/concepts/point)上。

**驱动名 / code / 类型**（来自 `application.yml`）：

- 驱动名 / code：`Listening Virtual TCP/UDP Driver` / `ListeningVirtualDriver`
- 类型：`DRIVER_SERVER`（驱动作监听端，被动收设备推上来的数据）

**监听端口**为进程级配置，不在位号上填：TCP 默认 `6270`、可用环境变量 `TCP_PORT` 覆盖；UDP 默认 `6271`、可用 `UDP_PORT`
覆盖。两个端口由 `initial()` 在驱动启动时各起一个线程监听，收到的报文走同一套解析逻辑。

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填这四个[属性](../introduction/concepts/attribute-config)
，告诉驱动认哪个关键字、从报文哪一段取（解析类型则由位号名决定，见下方说明）：

| 属性         | code    | 类型     | 默认值      | 说明                     |
|------------|---------|--------|----------|------------------------|
| Keyword    | `key`   | STRING | `62`     | 报文识别关键字，十六进制           |
| Start Byte | `start` | INT    | `0`      | 起始字节偏移（含）              |
| End Byte   | `end`   | INT    | `8`      | 结束字节偏移（不含）             |
| Type       | `type`  | STRING | `string` | 必填属性；仅做存在性校验，不用于选择解析类型 |

::: tip key 用十六进制，与报文关键字逐字比对
`key` 填的是报文第 23 字节的十六进制值（如默认 `62`）。一帧报文进来，驱动只把关键字相同的位号拿来解析；关键字对不上的位号这一帧不出值。同一台设备的不同位号可以用同一个
`key`（一帧里同时取多个字段），也可以用不同 `key`（不同帧各管各的）。
:::

::: warning 解析类型由位号名决定，不是 `type` 属性
驱动按 **位号名（pointName）** 选解析方式（见 `NettyServerHandler.readConfiguredValue`），受支持的位号名只有这 6 个：
`altitude`→float（4 字节）、`speed`→double（8 字节）、`level`→long（8 字节）、`direction`→int（4 字节）、`locked`→boolean（1 字节）、
`coordinate`→string（按 `start..end`）。位号名必须是上述之一，否则该位号这一帧解析为空字符串、采不到数据。`type` 属性只在
`validatePoint` 做存在性检查（必填四项之一），解析时从不读取它。
:::

## 故障排查

接入这个驱动时，绝大多数"采不到值"都能归到下面几条。报文被丢弃时驱动只在日志里 `warn`，不会向设备回错，所以排查时优先看驱动日志。

::: warning 报文前 22 字节必须是平台上的数字设备 ID
驱动用 `Long.parseLong` 把报文前 22 字节解析成设备 ID 去匹配平台上的[设备](../introduction/concepts/device)。设备推数据时
**必须把对应的数字设备 ID 填进这 22 字节**——解析不出数字（`deviceIdInvalid`）、或匹配不到设备（`deviceMissing`
），这一帧会被直接丢弃且不向设备报错。先在平台建好设备拿到 ID，再配进设备端固件。
:::

::: warning start/end 的偏移是相对整帧报文，不是相对载荷
位号的 `start`/`end` 是在**整帧报文**上的字节偏移。前 23 字节被设备名（22）和关键字（1）占用，所以载荷第一个字节的偏移是 `23`
，而不是 `0`。要取载荷开头的数据，`start` 应从 `23` 起算；按默认 `start=0` 会落在设备名里取到错误的值。
:::

- **关键字对不上**：位号的 `key` 与报文第 23 字节的十六进制必须完全相等（如 `62`）。`key` 不匹配的位号这一帧静默跳过、不出值；先确认设备实际发的是哪个字节。
- **字节序问题**：定长数值用 Netty `ByteBuf` 的 `getFloat/getDouble/getLong/getInt` 读取，均为**大端（big-endian）**
  。设备端若按小端打包，解析出的数值会错乱，需在设备侧改成大端或自行调整解析逻辑。
- **报文太短 / 偏移越界**：整帧不足 23 字节（`payloadTooShort`），或某位号 `start+长度` 超出报文实际长度（
  `payloadOutOfBounds`），该位号这一帧不出值。注意 UDP 单包不可分片、TCP 可能粘包/拆包——本驱动按收到的 `ByteBuf` 原样解析，不做组帧。
- **设备一直在线、与是否推数据无关**：本驱动**未实现协议级健康判定**（没有覆写 `health()`），SDK 用默认实现每
  `0/15 * * * * ?` 无条件把设备上报为在线，并续上 `45` 秒租约 TTL。也就是说设备**不会**因为"超时未推数据"
  而离线——在线状态与数据推送完全无关。若你需要"无推送即离线"，得在驱动里覆写 `health()`、按最近一次推送时间返回
  OFFLINE。在线状态机制见[设备](../introduction/concepts/device)。

## 在 IoT DC3 中如何落地

- **dc3.driver.code**：`ListeningVirtualDriver`（路由标识，稳定不可随意改）。
- **读 / 写 / 订阅能力**（与[驱动能力矩阵](./matrix)对齐）：
    - **读**：`—`，不做主动轮询。`schedule.read.enable=false`，`read()` 直接返回 `null`，数据完全由设备推送触发。配置上启用了
      `0/5 * * * * ?` 的 `schedule.custom` 定时回调，但驱动的 `schedule()` 是空实现、不执行任何动作（无周期性自维护逻辑）。
    - **写**：`✓`，`write()` 已实现。任何（TCP 或 UDP）成功解析的报文都会把该设备最近一次的 `Channel` 登记进
      `DEVICE_CHANNEL_MAP`（登记发生在 TCP/UDP 共用的 `NettyServerHandler.read()` 里）；下发写命令时按 `deviceId`
      取出活跃通道、把值字节写回设备（5 秒 flush 超时）。通道不存在或不活跃则写失败返回 `false`。注意：若该设备最近一次登记的是无连接的
      UDP 通道，回写通常会失败——回写依赖的是面向连接的 TCP 通道。
    - **订阅 / 上报**：`✓`，这是本驱动的主能力——设备主动连入、被动收推送。

::: info 这是虚拟/测试驱动，报文格式自定
本驱动整体可用（监听、解析、回写均已实现），但它没有标准协议层：报文格式（22+1+载荷）、6 个固定位号名、大端数值解析都是本驱动自定的演示约定。真实项目接入私有协议时，请把
`NettyServerHandler` 的解析逻辑替换为你的协议规则，把它当作"被动监听型驱动"的实现模板。
:::

### 最小接入示例

把一台通过 TCP 推送数据的 GPS 终端接进来：

1. 用 `Listening Virtual TCP/UDP Driver` 创建[设备](../introduction/concepts/device)（本驱动无 driver
   属性，设备本身不需填连接参数），记下平台分配的数字设备 ID。
2. 给设备绑定的[模板 Profile](../introduction/concepts/profile) 加一个字符串[位号](../introduction/concepts/point)，*
   *位号名必须取支持的名字之一**（这里用 `coordinate`，解析为字符串），point 属性填 `key=62`、`start=23`、`end=31`、
   `type=string`——即认关键字 `62` 的报文，从载荷起始处取 8 个字节当字符串。位号名若不在
   `altitude/speed/level/direction/locked/coordinate` 之列，驱动采不到值。
3. 启动驱动；让设备把"22 字节数字设备 ID + 1 字节 `0x62` + 载荷"推到驱动的 TCP `6270`
   端口，几秒内就能在[位号值](../introduction/concepts/point-value)里看到解析结果。

## 延伸阅读

- [驱动总览](./index) — 驱动是什么、注册与生命周期、配置三层来历
- [驱动能力矩阵](./matrix) — 28 个驱动的读/写/订阅一览，确认本驱动定位
- [设备接入](../operation/device-onboarding) — 一次完整的设备接入流程
