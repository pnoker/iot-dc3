---
title: OPC DA 驱动
---

# OPC DA 驱动

> **`dc3-driver-opc-da` 把 OPC DA 服务器接入 IoT DC3**——以 OPC DA 服务器为目标，通过 DCOM 周期性读取标签（item）实时值。

OPC DA（OPC Data Access）是 Windows 平台上最经典的工业数据访问规范，SCADA、组态软件、PLC 网关大多内置 OPC DA Server，把现场点位以"标签"（item）形式暴露出来。本驱动作为 OPC DA 客户端，基于 DCOM/J-Interop 连接一台或多台 OPC DA Server，按[位号](../introduction/concepts/point)上配置的分组名（group）与标签名（tag）读取实时值。

适用场景：现场已有 OPC DA Server（OPC DA 2.0 / 3.0），需要把其中的标签接入 DC3 统一采集与存储。

- **驱动名 / code**：`OPC DA Driver` / `OpcDaDriver`
- **类型**：`DRIVER_CLIENT`（主动连接 OPC DA Server）

::: warning DCOM 是前置条件
OPC DA 基于 Windows COM/DCOM。Server 必须跑在可经 DCOM 远程访问的 Windows 主机上，并已配置好 DCOM 权限，允许驱动所在主机远程访问。这一步在操作系统层面完成，不属于本驱动配置。
:::

## 驱动配置（设备级 `driver-attribute`）

接入一台 OPC DA 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | OPC DA host |
| CLSID | `clsId` | STRING | `F8582CF2-88FB-11D0-B850-00C0F0104305` | OPC DA server CLSID |
| Username | `username` | STRING | `dc3` | OPC DA username |
| Password | `password` | STRING | `dc3dc3` | OPC DA password |

::: tip CLSID 是 OPC DA Server 的 COM 标识
OPC DA 通过 COM 类标识符（CLSID）定位 Server，而不是 TCP 端口。CLSID 由目标 OPC DA Server 的厂商决定，可在服务器主机的注册表或 OPC 服务器浏览工具中查到。默认值仅为占位，接入时务必替换为真实 Server 的 CLSID。
:::

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Group | `group` | STRING | `GROUP` | OPC DA item group name |
| Tag | `tag` | STRING | `TAG` | OPC DA item tag name |

OPC DA 把标签组织在分组（group）下。驱动按位号的 `group` 找到（或新建）分组，再用 `tag` 在组内定位标签并读取其值；标签返回的 COM 变体类型（如 VT_I4、VT_R8、VT_BOOL、VT_BSTR 等）由驱动转换为位号值。

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把一台运行在 `192.168.1.10` 的 OPC DA Server 中的某个标签接进来：

1. 选 `OPC DA Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`clsId=`（填真实 Server 的 CLSID）、`username`/`password`（填可远程访问该 Server 的 Windows 账号）。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个[位号](../introduction/concepts/point)，point 属性填 `group=Group1`、`tag=Channel1.Device1.Tag1`（按目标 Server 的实际命名）。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning Server 跑在 Windows，且 DCOM 必须放行
连不上的最常见原因不是驱动，而是 DCOM：防火墙未放行、远程账号未授权、Server 主机未启用远程激活权限。先在驱动主机用 OPC 客户端工具验证能连上该 CLSID，再接入 DC3。
:::

::: tip group/tag 必须与 Server 的命名一致
`group` 是 OPC DA 的分组名，`tag` 是标签的完整 item 名（不同厂商命名风格不同，常见如 `Channel1.Device1.TagA`）。填错名字时驱动会在该分组下找不到标签而读取失败——这些名字以目标 Server 浏览出来的为准，不要凭习惯臆测。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `group` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus TCP 驱动](./modbus-tcp) — 另一种工业现场协议驱动
