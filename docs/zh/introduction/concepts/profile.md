---
title: 模板 Profile
---

<script setup>
import ProfileRelationDiagram from '../../../.vitepress/theme/components/ProfileRelationDiagram.vue'
import ProfileLifecycleDiagram from '../../../.vitepress/theme/components/ProfileLifecycleDiagram.vue'
</script>

# 模板 Profile (Thing Model) <Badge type="tip" text="物模型+" />

> **模板是"一类设备的能力模板"** <Badge type="tip" text="物模型+" />——它把同型号设备共有的[位号](./point)、[指令](./command)、[事件](./event)
> 聚合在一起，描述"这类设备能采什么、能控什么、会报什么"。一个[设备](./device)恰好归属一个模板，多个设备可以复用同一个模板。

## 它是什么 / 为什么需要

想象你接入 100 台同型号的温湿度传感器。如果每台都单独配置"温度位号、湿度位号、校准指令、故障事件"，那就是 100 份重复劳动，改一处要改
100 次。模板解决的正是这件事：**把能力定义抽出来沉淀成一份模板**，设备实例只引用它。

类比产品和实物：模板像"产品说明书 / 出厂规格"，设备像"按这份规格出厂的一台台实物"。说明书写一遍，实物可以造很多台。

::: tip 模板与"物模型"的关系
"物模型（Thing Model）"是行业里常见的设备能力建模设计，DC3 的**模板 `Profile`** 与它属于**同级抽象**——都在回答"一类设备有哪些能力"。DC3
没有沿用 `Product` / `ThingModel` 这类叫法，而是选了**模板**，并且能力比典型物模型**更强**：模板支持[共享范围](#枚举)（租户
/ 驱动 / 用户三档复用）、版本演进、弱结构化扩展 `profileExt` 等，比"一产品一物模型"的固定结构更灵活。可以理解为：**模板 ⊇ 物模型**——物模型能表达的，模板都能表达，反之未必（见[设计哲学](../../architecture/domain-model)）。
:::

**模板 vs 物模型（一眼看懂"强在哪"）：**

| 维度       | 物模型 Thing Model（行业通用） | 模板 Profile（DC3） <Badge type="tip" text="物模型+" /> |
|----------|---------------------|------------------------------------------------|
| 定位       | 设备能力建模的抽象           | 同级抽象，能力更强（超集）                                  |
| 能力聚合     | 属性 / 服务 / 事件        | [位号](./point) / [指令](./command) / [事件](./event)    |
| 复用范围     | 通常按产品固定             | 共享范围三档：租户 / 驱动 / 用户（`profileShareFlag`）         |
| 版本演进     | 一般无显式版本             | `version` 显式版本，可查询、可演进                          |
| 扩展字段     | 结构相对固定              | `profileExt` 弱结构化扩展（可承载 category / tags 等）      |
| 创建来源     | —                   | `profileTypeFlag`：系统 / 驱动 / 用户                  |
| 设备绑定     | 视实现而定               | 恰好绑定一个（`Device.profileId` 单一外键）                 |

> 一句话：**模板是物模型的加强版**——保留"一类设备的能力模板"这一同级抽象，再叠加共享、版本、扩展等平台化能力。

**容易混淆的三组概念：**

- **模板 vs 设备**：模板是"类"（定义一遍），设备是"实例"（接入多台）。位号"温度"定义在模板上，而"3 号传感器此刻的温度 =
  25.3℃"这一[位号值](./point-value)是设备的运行态数据。
- **模板 vs 驱动**：模板描述"设备有哪些能力"（业务语义），[驱动](./driver)描述"用什么协议怎么连"
  （连接方式）。同一个模板可以配不同驱动，二者正交。
- **聚合 vs 拥有**：模板不"存"位号 / 指令 / 事件的数据，它只是它们的**归属根**——`Point`、`Command`、`Event` 都通过
  `profileId` 挂回模板。

## 关键字段

模板 `ProfileBO`（表 `dc3_profile`）：

| 字段                 | 类型                   | 含义                                     |
|--------------------|----------------------|----------------------------------------|
| `profileName`      | String               | 模板名称（展示用）                             |
| `profileCode`      | String               | 模板编码，同租户下唯一，作为模型标识                    |
| `profileShareFlag` | ProfileShareTypeEnum | 共享范围，见下                                |
| `profileTypeFlag`  | ProfileTypeEnum      | 创建来源，见下                                |
| `version`          | Integer              | 模型版本，可查询、由人工设置                         |
| `profileExt`       | ProfileExt (JSON)    | 弱结构化扩展字段（设计上可承载 `category`、`tags` 等内容） |
| `enableFlag`       | EnableFlagEnum       | 启停状态                                   |
| `tenantId`         | Long                 | 归属的[租户](./tenant)                      |

::: tip 模板不直接持有子能力的字段
`ProfileBO` 上看不到位号 / 指令 / 事件列表——它们是独立实体，靠各自的 `profileId` 外键挂回来。查"这个模板有哪些能力"要分别查
`Point` / `Command` / `Event`，而不是读 `ProfileBO` 的某个字段。
:::

## 枚举

**共享范围 `profileShareFlag`（`ProfileShareTypeEnum`）**——控制这份模板能被谁复用：

| 枚举       | code   | 含义                |
|----------|--------|-------------------|
| `TENANT` | tenant | 租户内共享，租户下所有设备可引用  |
| `DRIVER` | driver | 驱动内共享，归属某驱动的设备可引用 |
| `USER`   | user   | 用户私有，仅创建者可见       |

**创建来源 `profileTypeFlag`（`ProfileTypeEnum`）**：

| 枚举       | code   | 含义   |
|----------|--------|------|
| `SYSTEM` | system | 系统内置 |
| `DRIVER` | driver | 驱动创建 |
| `USER`   | user   | 用户创建 |

## 与其它概念的关系

<ProfileRelationDiagram lang="zh" />

模板是[位号](./point)、[指令](./command)、[事件](./event)三类能力的归属根，三者并列地回答"
这类设备有什么能力"。[设备](./device)通过 `profileId` **恰好绑定一个**
模板——这是单一外键，不是多对多。设备如何连接由[驱动](./driver)决定，与模板正交。

## 生命周期

<ProfileLifecycleDiagram lang="zh" />

先建模板并补齐位号 / 指令 / 事件，再让多台同型号设备绑定它；运行期设备按模板采集位号值、接收指令、上报事件；能力变更时递增
`version`。

::: warning 一个设备只能绑一个模板
早期版本支持设备绑定多个模板（`dc3_profile_bind` 多对多），现已收敛为 `Device.profileId` 单一外键：**一个设备恰好归属一个模板
**，一个模板可被多个设备复用。设备的位号集合只来自它 `profileId` 指向的那一个模板，不会跨模板混取。
:::

## 示例

为"温湿度传感器 ZS-100"建一个模板：`profileCode = ZS-100`、`profileShareFlag = TENANT`（租户内共享）、`version = 1`
。在它下面定义两个位号（`temperature`、`humidity`）、一条指令（`CALIBRATE` 校准）、一个事件（`SENSOR_FAULT` 传感器故障）。随后接入的
100 台该型号传感器，每台 `Device` 都把 `profileId` 指向这一个模板即可复用全部能力；下次给温度位号加个 `max`
约束，只改模板一处，100 台设备同时生效，`version` 升到 2。

## API

模板管理接口前缀 `/profile`（Manager 服务）：

| 方法   | 路径                           | 说明         |
|------|------------------------------|------------|
| POST | `/profile/add`               | 新增模板      |
| POST | `/profile/update`            | 更新模板元数据   |
| POST | `/profile/delete`            | 删除模板      |
| GET  | `/profile/get_by_id`         | 按 ID 查询模板 |
| POST | `/profile/list`              | 分页查询模板    |
| GET  | `/profile/list_by_device_id` | 查某设备绑定的模板 |

## 延伸阅读

- [位号 Point](./point) — 模板聚合的数据点 / 控制点
- [指令 Command](./command) — 模板聚合的动作型能力
- [事件 Event](./event) — 模板聚合的上报能力
- [设备 Device](./device) — 模板的实例，通过 `profileId` 绑定
- [概念概览](../concepts) — 全部核心概念一览
- [领域模型](../../architecture/domain-model) — Profile 在 DC3 领域语言中的定位
