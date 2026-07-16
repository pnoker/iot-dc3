---
title: 核心概念与心智模型
---

<script setup>
import ConceptsDomainDiagram from '../../.vitepress/theme/components/ConceptsDomainDiagram.vue'
import ConceptsFlowDiagram from '../../.vitepress/theme/components/ConceptsFlowDiagram.vue'
</script>


# 核心概念与心智模型

要用好 IoT DC3，先要在脑子里建立一个简单的对象模型。这页用一句话心智模型 +
一张实体关系图把它讲清，然后解释每个对象、最容易混淆的"三层配置"，以及贯穿一切的租户边界。读完你就能看懂后面所有操作和架构文档里的术语。

> 你在这里：刚了解了 [平台定位](./)，准备动手前先理清概念。下一步可看 [按角色选择路径](./paths)
> 或直接 [快速开始](../quickstart/)。

## 一句话心智模型

> **驱动接入设备，模板描述能力，设备绑定模板，位号承载数据；数据中心存值并下发命令。**

也就是说：协议**驱动（Driver）**负责和设备通信；**模板（Profile）**抽象同类设备的能力（有哪些位号、命令、事件）；**设备（Device）**
是绑定了某个模板和某个驱动的具体实例；**位号（Point）**是要采集或写入的一个数据项；采到的值是 **位号值（PointValue）**。

## 对象与关系

这些对象的关系是固定的：一个模板下挂多个位号/命令/事件；一个设备**只绑定一个**模板（自 Phase-1 起，`Device.profileId`
是单一外键，不再是多对多）和一个驱动；一个位号会产生很多位号值。

<ConceptsDomainDiagram lang="zh" />

## 逐个对象

- **驱动 Driver（`dc3-driver-*`）**：一个协议适配服务实例，负责和设备或数据源通信。启动时它会把自己和它能接受的配置项（属性）注册到管理中心。平台内置
  28 个驱动，覆盖 Modbus、OPC UA、S7、MQTT 等，详见 [模块地图](../architecture/modules)。
- **模板 Profile**：同类设备的能力模板。它把"这类设备有哪些位号、支持哪些自定义命令、会上报哪些事件"沉淀下来，设备复用它即可。
- **设备 Device**：现场一台具体设备的平台镜像。它绑定一个模板（决定有哪些位号）和一个驱动（决定怎么通信）。
- **位号 Point**：一个数据项。关键字段是 `pointTypeFlag`（数据类型）和 `rwFlag`（读写方向）。

::: tip 位号的读写由 Point 自己决定
某个位号能不能写，取决于它的 `rwFlag`（`READ_ONLY` / `WRITE_ONLY` / `READ_WRITE`），**不是**由命令表决定。写一个 `READ_ONLY`
位号会被拒绝。位号还可带单位 `unit` 和换算（`baseValue` / `multiple`），把原始值线性变换成工程值。
:::

## 三层配置：Param、Attribute、Config

这是最容易混淆的地方。IoT DC3 把"配置"拆成三个不同层次，各自回答不同问题：

| 层             | 对象                                                                           | 回答的问题                   | 来源                          |
|---------------|------------------------------------------------------------------------------|-------------------------|-----------------------------|
| 业务层 Param     | `CommandParam` / `EventParam`                                                | 这个命令/事件有哪些输入输出参数        | 模板模型里定义                     |
| 协议层 Attribute | `DriverAttribute` / `PointAttribute` / `CommandAttribute` / `EventAttribute` | 这个驱动**有哪些**配置项          | 驱动启动时从 `application.yml` 注册 |
| 实例层 Config    | `PointAttributeConfigDO` 等                                                   | **这台设备**给这些配置项填的**具体值** | 用户为设备/位号配置                  |

举例：Modbus 驱动声明"位号需要一个寄存器地址"（这是 Attribute，驱动注册的），而"3 号设备的温度位号地址是 40001"（这是
Config，设备实例填的值）。理解这层区分，才能看懂 [设备接入](../operation/device-onboarding) 里"配置位号属性"那一步。

## 数据流与命令流

围绕这些对象，平台跑着两条相反的链路：**数据流**把设备的值采上来存好、对外可查；**命令流**把读写请求下发到设备执行。

<ConceptsFlowDiagram lang="zh" />

两条链路的完整实现（交换机、队列、生命周期、回执）分别见 [数据平面](../architecture/data-plane)
和 [命令平面](../architecture/command-plane)。

## 租户边界

业务数据以**租户（`tenantId`）**为边界隔离。调用 API、创建设备、查询数据、下发命令时都应保持租户上下文一致——平台在控制器层校验租户上下文（
`requireTenant` / `filterTenant`）——按 ID 或批量访问别的租户的记录会被判为不存在（返回 404 而非数据）。开发环境默认租户通常是
`default`，生产环境按实际组织与权限模型配置。隔离是怎么一层层落实的，见 [鉴权 · 租户 · RBAC](../architecture/auth-rbac)。

## 概念详解

每个核心概念都有独立词条，讲清定义、关键字段、与其它概念的关系、生命周期与易错点：

- [模板 Profile](./concepts/profile) — 同类设备的能力模板，聚合位号 / 指令 / 事件
- [设备 Device](./concepts/device) — 现场一台设备的平台镜像
- [驱动 Driver](./concepts/driver) — 协议适配服务，负责和设备通信
- [位号 Point](./concepts/point) — 一个数据项（要采集或写入的量）
- [位号值 PointValue](./concepts/point-value) — 某位号某时刻的取值快照
- [指令 Command](./concepts/command) — 触发设备动作（区别于写位号）
- [事件 Event](./concepts/event) — 设备主动上报的一次业务发生
- [属性与配置 Attribute & Config](./concepts/attribute-config) — Param / Attribute / Config 三层
- [租户 Tenant](./concepts/tenant) — 业务数据隔离边界

## 延伸阅读

- [按角色选择路径](./paths) — 按你的目标选择阅读顺序
- [设备接入](../operation/device-onboarding) — 把概念落成一次真实接入
- [领域模型](../architecture/domain-model) — DO/BO/VO 分层与字段细节
- [快速开始](../quickstart/) — 本地起栈
- [物联网技术总览](../foundations/) — 把这些概念放进物联网四层架构通盘理解
