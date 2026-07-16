---
title: 属性与配置 Attribute & Config
---

<script setup>
import AttributeConfigRelationDiagram from '../../../.vitepress/theme/components/AttributeConfigRelationDiagram.vue'
import AttributeConfigFlowDiagram from '../../../.vitepress/theme/components/AttributeConfigFlowDiagram.vue'
</script>

# 属性与配置 Attribute & Config

> **属性（Attribute）是[驱动](./driver)声明"接入一台设备需要填哪些配置项"的定义，配置（Config）是某台[设备](./device)
给这些配置项填的具体值。** 一个回答"有哪些格子要填"，一个回答"这台设备的格子里填了什么"。

设备能不能被采集，往往不取决于"温度位号叫什么"，而取决于一些很具体的协议细节：Modbus 要读哪个寄存器地址、HTTP 要请求哪个
URL、MQTT 要订阅哪个 Topic。这些细节因协议而异、因设备而异，写死在代码里行不通。IoT DC3 把它拆成两层来管：*
*驱动声明需要哪些配置项（Attribute）**，**设备实例填这些配置项的值（Config）**。

理解它的关键，是先分清这里其实有**三摊**互不相同的东西：

| 层                  | 归属                       | 回答的问题         | 谁来填         |
|--------------------|--------------------------|---------------|-------------|
| **Param 业务参数**     | [模板 Profile](./profile) | 指令/事件携带哪些业务字段 | 建模者，在模板里定义  |
| **Attribute 属性定义** | [驱动 Driver](./driver)    | 接这种协议需要哪些配置项  | 驱动开发者，启动时注册 |
| **Config 配置值**     | [设备 Device](./device)    | 这台设备这些配置项填什么  | 集成者，在设备编辑页填 |

`Param` 是"业务语义"（温度、模式、故障码），属于[模板](./profile)，和具体协议无关；`Attribute` / `Config` 是"协议映射"
（寄存器地址、Topic、报文模板），属于驱动和设备。本页只讲后两层，Param 见[指令](./command)与[事件](./event)。

## 经典例子：一句话讲清 Attribute vs Config

> Modbus 驱动声明"读一个位号，需要一个寄存器地址"——这是 **Attribute**（驱动注册的"有这么个配置项"）。
> 给 3 号设备的温度位号填上"地址 = 40001"——这是 **Config**（这台设备这个配置项的具体值）。

同一个 `PointAttribute(registerAddress)`，1 号设备可能填 40001，3 号设备填 40003；换 MQTT 驱动，声明的就不再是寄存器地址，而是
`topic`。属性是"模具"，配置是"浇出来的件"。

## Attribute 定义从哪来

::: tip 属性不是在数据库里手建的，是驱动启动时注册的
驱动在自己的 `application.yml` 里声明支持哪些属性，**启动时上报给 Manager**，Manager 以
`tenant_id + driver_id + attribute_code` 为唯一键落库。不同协议需要的属性不同，所以属性的"权威来源"是驱动，而不是页面手填。
:::

```yaml
dc3:
  driver:
    driver-attribute:        # 连接级配置项：连这台设备网关需要什么
      - attribute-name: Host
        attribute-code: host
        attribute-type-flag: STRING
        default-value: localhost
    point-attribute:         # 位号级配置项：采每个位号需要什么
      - attribute-name: Register Address
        attribute-code: registerAddress
        attribute-type-flag: INT
        default-value: ''
```

`DriverAttribute` 和 `PointAttribute` 的区别只在**作用范围**：前者一台设备填一份（连接信息），后者每个[位号](./point)
各填一份（采集映射）。

## 关键字段

属性定义 `DriverAttributeBO` / `PointAttributeBO`（两者字段完全一致，结构同源）：

| 字段                  | 类型                                     | 含义                                      |
|---------------------|----------------------------------------|-----------------------------------------|
| `attributeName`     | String                                 | 属性名称（页面展示用）                             |
| `attributeCode`     | String                                 | 属性编码，配置按它匹配（如 `host`、`registerAddress`） |
| `attributeTypeFlag` | AttributeTypeEnum                      | 值类型，见下                                  |
| `defaultValue`      | String                                 | 默认值，设备未填时兜底                             |
| `driverId`          | Long                                   | 归属的[驱动](./driver)                       |
| `attributeExt`      | DriverAttributeExt / PointAttributeExt | 扩展信息（如 UI 控件、校验规则）                      |
| `enableFlag`        | EnableFlagEnum                         | 启停状态                                    |
| `tenantId`          | Long                                   | 归属[租户](./tenant)                        |

配置值 `DriverAttributeConfigBO` / `PointAttributeConfigBO`：

| 字段            | 类型             | 含义                                               |
|---------------|----------------|--------------------------------------------------|
| `attributeId` | Long           | 指向哪条属性定义                                         |
| `configValue` | String         | 实际填的值（如 `40001`）                                 |
| `deviceId`    | Long           | 归属哪台[设备](./device)                               |
| `pointId`     | Long           | **仅 `PointAttributeConfig` 有**，指向哪个[位号](./point) |
| `configExt`   | JsonExt        | 配置扩展信息                                           |
| `enableFlag`  | EnableFlagEnum | 启停状态                                             |
| `tenantId`    | Long           | 归属[租户](./tenant)                                 |

::: warning DriverConfig 按设备、PointConfig 按位号
`DriverAttributeConfig` 只有 `deviceId`，因为连接信息一台设备一份；`PointAttributeConfig` 多一个 `pointId`
，因为每个位号都要单独填采集映射。这正是两层作用范围不同的直接体现。
:::

## 值类型 AttributeTypeEnum

`attributeTypeFlag` 与[位号](./point)共用同一套类型系统：

| 值 | `STRING` | `BYTE` | `SHORT` | `INT` | `LONG` | `FLOAT` | `DOUBLE` | `BOOLEAN` |
|---|----------|--------|---------|-------|--------|---------|----------|-----------|

## 与其它概念的关系

<AttributeConfigRelationDiagram lang="zh" />

属性挂在驱动下、被设备配置引用；位号配置还额外绑定到具体[位号](./point)。建模时定义[模板](./profile)
（含位号、指令、事件），接入时由驱动声明属性、设备填配置，两条线在设备处汇合。

## 注册与配置流程

<AttributeConfigFlowDiagram lang="zh" />

1. 驱动从 `application.yml` 读取 `driver-attribute` / `point-attribute`，启动时上报。
2. Manager 按唯一键写入或更新属性定义。
3. 设备编辑页按当前 `driverId` 加载属性列，集成者逐项填值。
4. 配置值落到 Config 表，运行时驱动拉取并组装成实际协议报文。

## Attribute / Config 能解决什么、不能解决什么

`Attribute + Config` 解决的是**协议映射**：把"这台设备的这个位号/连接"翻译成驱动能执行的具体地址、Topic、模板。它**不能**
单独表达"位号本身是什么""指令要传哪些业务参数"——那是[模板](./profile)与 Param 的职责。

| 能解决                  | 不能单独解决（需要其它模型）                                 |
|----------------------|------------------------------------------------|
| 连这台设备网关用什么 Host / 端口 | 这类设备有哪些位号、指令、事件 → [模板 Profile](./profile)     |
| 采这个位号读哪个寄存器 / 路径     | 指令携带哪些业务入参出参 → [指令 Command](./command) 的 Param |
| 同一模板在不同驱动下填不同映射值    | 事件上报哪些业务字段 → [事件 Event](./event) 的 Param       |

::: tip 一句话定位
缺采集？多半是 **Config 没填或填错**（地址/Topic 写错）。缺能力？那是 **Attribute 没声明**（驱动根本没注册这个配置项）——后者要改驱动
`application.yml` 重启，前者在页面改值即可、无需重启。
:::

## 示例

3 号温度传感器，绑定 Modbus 驱动：

- 驱动注册的属性：`DriverAttribute(host)`、`PointAttribute(registerAddress)`。
- 设备填的配置：`DriverAttributeConfig{ deviceId: 3, configValue: "192.168.1.10" }`（连接到这台网关）；温度位号填
  `PointAttributeConfig{ deviceId: 3, pointId: 温度位号, configValue: "40001" }`。

运行时驱动据此连接 `192.168.1.10`、读寄存器 `40001`，把读数封装成该位号的[位号值](./point-value)上报。换成 1 号设备，
`configValue` 改成另一个地址即可，属性定义完全复用。

## 延伸阅读

- [驱动 Driver](./driver) — 属性的声明者与注册来源
- [位号 Point](./point) — `PointAttributeConfig` 绑定的对象
- [指令 Command](./command) — Param（业务参数）与 Attribute（协议映射）的分工
- [事件 Event](./event) — 事件侧同样区分 Param 与 Attribute
- [设备接入 Device Onboarding](../../operation/device-onboarding) — 在页面上填这些配置的完整流程
- [核心概念总览](../concepts) — 回到概念地图
