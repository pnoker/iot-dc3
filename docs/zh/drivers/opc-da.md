---
title: OPC DA 驱动
---

<script setup>
import OpcDaDiagram from '../../.vitepress/theme/components/OpcDaDiagram.vue'
</script>


# OPC DA 驱动

`dc3-driver-opc-da` 作为 OPC DA 客户端，通过 Windows DCOM 连接现场的 OPC DA
Server，按位号上配置的分组（group）与标签（tag）周期性读取实时值，也支持把值写回标签。本页讲清 OPC DA
是什么协议、驱动暴露哪些属性、接不上时怎么排查，以及它在 IoT DC3 里的落地状态。

> 你在这里：要把一台已有 OPC DA Server 的设备接入 DC3。先了解协议，再照[属性配置](#属性配置)
> 填表，遇阻看[故障排查](#故障排查)。

## 协议背景

OPC DA（OPC Data Access）是 Windows 平台上最经典的工业数据访问规范。它诞生于 PC 时代，把 Microsoft 的 COM/DCOM
组件技术用于工业现场：SCADA、组态软件、PLC 网关大多内置一个 OPC DA Server，把底层 PLC、仪表的点位以"标签"
（item）的形式暴露出来，上位机用统一的 OPC DA 客户端去读写，而不必关心每个 PLC 的私有协议。常见版本是 OPC DA 2.0 / 3.0。

在[物联网四层架构](../foundations/fieldbus)里，OPC DA 属于**网络层**——它是现场设备与上位系统之间的"通用翻译层"
。但它的网络承载不是普通 TCP 端口，而是 **DCOM（Distributed COM）**：客户端通过 COM 类标识符（CLSID）定位 Server，调用走 DCOM
远程过程调用。这一点决定了 OPC DA 的部署与排错都带着浓重的 Windows 烙印——这也是后来跨平台的 [OPC UA](./opc-ua) 出现的原因。

<OpcDaDiagram lang="zh" />

OPC DA 把标签组织成"分组（group）下的若干 item"：客户端先在 Server 上创建或找到一个 group，再往组里添加要订阅/读写的
item，按需读取或写入其值。每个 item 的值带 COM 变体类型（VARIANT），如 `VT_I4`（整型）、`VT_R8`（双精度）、`VT_BOOL`、`VT_BSTR`
（字符串），驱动据此转换成位号值。

::: warning DCOM 是前置条件，且只在 Windows 上
OPC DA 基于 Windows COM/DCOM。Server 必须跑在可经 DCOM 远程访问的 Windows 主机上，并在操作系统层面配好 DCOM
权限，允许驱动所在主机远程访问。这一步不属于本驱动配置，却是能否接通的决定性因素。
:::

## 属性配置

OPC DA 的接入参数分两层：**driver 属性**描述"连哪台 Server"（设备级，一台设备一份），**point 属性**描述"读哪个标签"
（位号级，一个位号一份）。下面两张表来自驱动 `application.yml` 的 `driver-attribute` / `point-attribute`，默认值即驱动内置默认值。

### 驱动属性（设备级 `driver-attribute`）

接入一台 OPC DA 设备时，在[设备](../introduction/concepts/device)
上填这些[属性](../introduction/concepts/attribute-config)。`host` 指向 Server 主机，`clsId` 定位具体的 OPC DA Server，
`username` / `password` 是用于 DCOM 远程访问的 Windows 凭据：

| 属性       | code       | 类型     | 默认值                                    | 说明                          |
|----------|------------|--------|----------------------------------------|-----------------------------|
| Host     | `host`     | STRING | `localhost`                            | OPC DA Server 所在主机（IP 或主机名） |
| CLSID    | `clsId`    | STRING | `F8582CF2-88FB-11D0-B850-00C0F0104305` | 目标 OPC DA Server 的 COM 类标识符 |
| Username | `username` | STRING | `dc3`                                  | DCOM 远程访问用的 Windows 用户名     |
| Password | `password` | STRING | `dc3dc3`                               | 对应密码                        |

::: tip CLSID 是 OPC DA Server 的 COM 标识，不是端口
OPC DA 通过 COM 类标识符（CLSID）定位 Server，而不是 TCP 端口。CLSID 由目标 OPC DA Server 的厂商决定，可在 Server 主机的注册表，或用
OPC 服务器浏览工具查到。`application.yml` 里的默认值仅为占位，接入时务必替换为真实 Server 的 CLSID。
:::

### 位号属性（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填 `group` 与 `tag`。驱动先按 `group` 在 Server 上找到（或新建）分组，再用
`tag` 在组内定位标签 item，读取/写入其值：

| 属性    | code    | 类型     | 默认值     | 说明                     |
|-------|---------|--------|---------|------------------------|
| Group | `group` | STRING | `GROUP` | OPC DA 分组名（group name） |
| Tag   | `tag`   | STRING | `TAG`   | OPC DA 标签的完整 item 名    |

::: info group / tag 以 Server 浏览出来的为准
不同厂商命名风格不同，`tag` 常见形如 `Channel1.Device1.TagA`。这些名字必须与目标 Server 的实际命名一致，应从 Server
浏览结果中取，不要凭习惯臆测——名字对不上时驱动会在该组里找不到 item 而读取失败。
:::

### 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮），由 `dc3.driver.schedule.read` 控制。
- **自定义任务**：`dc3.driver.schedule.custom` 默认 cron `0/5 * * * * ?`，但本驱动的 `schedule()` 为空实现（设备保活由 SDK
  健康任务负责）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`
  ——在线状态机制见[设备](../introduction/concepts/device)。

## 故障排查

OPC DA 接不上，绝大多数问题出在 DCOM 与命名，而不在驱动代码本身。按下面顺序排查：

1. **连不上 / `ConnectorException`**：最常见根因是 DCOM。检查 Server 主机防火墙是否放行 DCOM 端口、远程账号是否被授予"
   远程激活/远程访问"权限、`username` / `password` 是否为该 Windows 主机上有效且有权访问该 Server 的账号。建议先在驱动所在主机用第三方
   OPC 客户端工具验证能连上该 CLSID，再接入 DC3。
2. **CLSID 错误**：`clsId` 必须是目标 Server 的真实 CLSID（默认值只是占位）。在 Server 主机注册表或 OPC 浏览工具中确认；CLSID
   写错会在连接阶段失败。
3. **读取失败 / `ReadPointException`**：通常是 `group` 或 `tag` 与 Server 命名不符，驱动在该组下 `addItem` 找不到标签。逐字符核对位号上的
   `group` / `tag`。读失败时驱动会**销毁并移除该设备的连接**，下一轮采集自动重连——若 Server 侧标签名一直不对，会持续失败。
4. **写入失败 / `WritePointException` 或 `UnSupportException`**：写入只处理
   `SHORT / INT / LONG / FLOAT / DOUBLE / BOOLEAN / STRING` 这几种位号类型。只有完全无法识别的类型码才会抛
   `UnSupportException`；而已知但不在上述处理范围内的类型（如 `BYTE`）不会抛异常，写入会被报告为**失败返回 `false`**
   （不写值）。另需确认目标 item 在 Server 上可写、当前账号有写权限。
5. **设备显示离线**：在线态由 SDK 健康任务维护（默认 15 秒一检、45 秒租约）。若连接反复因 DCOM 抖动被销毁重建，设备会在线/离线间跳变——先稳住
   DCOM 链路。
6. **跨平台限制**：驱动本身基于 J-Interop（纯 Java DCOM 实现）可在 Linux 上运行，但**对端 Server 必须是 Windows DCOM**
   。不要期望连接非 Windows 的 OPC DA 端。

## 在 IoT DC3 中如何落地

- **dc3.driver.code**：`OpcDaDriver`（稳定路由标识，对应消息路由与注册，不可随意改）。
- **驱动名 / 类型**：`OPC DA Driver` / `DRIVER_CLIENT`（主动连接 OPC DA Server）。
- **能力**：读 ✓、写 ✓、订阅 —，与[驱动能力矩阵](./matrix)一致。读走周期采集（cron `0/30`），写走下发命令；驱动不使用 OPC DA
  的变化订阅推送，而是周期主动读。

::: warning 实现状态：代码完整，但运行依赖 Windows / DCOM 基础设施
`OpcDaDriverCustomServiceImpl` 的 `read()` / `write()` 已基于内置的 OpenSCADA OPC DA 客户端库（J-Interop）**完整实现**
：连接通过 `Server.connect()` 建立并按设备缓存，读取调用 `item.read()` 并按 COM 变体类型转换，写入构造 `JIVariant` 调
`item.write()`。源码类注释里残留一句"work-in-progress skeleton / 见 TODO 标记"的旧警告，但方法体内已无
TODO，与当前实现不符——应以方法实现为准。真正的门槛不在代码，而在于必须有一台配好 DCOM 的 Windows OPC DA Server
才能实际跑通；缺少该环境时无法验证。
:::

::: tip 最小接入示例
把一台运行在 `192.168.1.10` 的 OPC DA Server 中的某个标签接进来：

1. 选 `OPC DA Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`clsId=`（真实 Server
   的 CLSID）、`username` / `password`（可远程访问该 Server 的 Windows 账号）。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个[位号](../introduction/concepts/point)，point 属性填
   `group=Group1`、`tag=Channel1.Device1.Tag1`（按目标 Server 实际命名）。
3. 启动驱动，30 秒内即可在[位号值](../introduction/concepts/point-value)里看到采集值。
   :::

## 延伸阅读

- [驱动总览](./index) — 全部驱动的统一模型与注册机制
- [驱动能力矩阵](./matrix) — 各驱动读 / 写 / 订阅能力速查
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — OPC DA 在网络层的位置与同类协议对比
- [OPC UA 驱动](./opc-ua) — 跨平台、可订阅的下一代 OPC
