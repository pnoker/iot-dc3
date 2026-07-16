---
title: 领域模型：DO / BO / VO 与对象关系
---

<script setup>
import DomainModelErDiagram from '../../.vitepress/theme/components/DomainModelErDiagram.vue'
import DomainModelLayerDiagram from '../../.vitepress/theme/components/DomainModelLayerDiagram.vue'
import DomainModelClassDiagram from '../../.vitepress/theme/components/DomainModelClassDiagram.vue'
import DomainModelSequenceDiagram from '../../.vitepress/theme/components/DomainModelSequenceDiagram.vue'
</script>


# 领域模型：DO / BO / VO 与对象关系

这页写给要在平台上写代码的人：理清 Profile / Point / Command / Event / Device / Driver
这几个对象怎么挂在一起，搞懂最容易踩坑的"三层配置"（Param / Attribute / Config），并掌握同一份数据在 DO、BO、VO 三层之间如何用
MapStruct `*Builder` 来回转换。读完你就能正确地新增字段、加枚举、读懂任意一个 `*Controller → *Service → *Manager` 调用链里值的形态。

> 你在这里：已经看过 [核心概念](../introduction/concepts)
> 的对象关系，现在往下钻一层看字段与分层。下一步可看 [数据平面](./data-plane)
> （位号值的落库链路）或 [驱动开发](../development/driver-authoring)（把这些对象落成一个真实驱动）。

## 一切始于 Profile

IoT DC3 的领域模型有一个根：**模板 Profile**。它不是一台设备，而是"一类设备的能力清单"——这类设备有哪些可读写的**位号 Point
**、支持哪些自定义**命令 Command**、会上报哪些**事件 Event**。把能力沉淀在模板上，设备只要绑定模板就自动继承这套能力，无需逐台重复定义。

**设备 Device** 是现场一台具体设备在平台里的镜像。它做两件绑定：绑一个 Profile（决定"有哪些能力"），绑一个 **驱动 Driver**
（决定"用什么协议通信"）。这里有一个 Phase-1 之后定下的硬约束：`DeviceDO.profileId` 是**单一外键**（一个 `Long`），不再是早期的多对多
`ProfileBind`——每台设备**只能绑一个模板**。

::: danger 设备与模板是一对一，别再按多对多设计
`dc3_device.profile_id` 是单值外键（`DeviceDO.java`）。新增涉及"设备的能力来源"的查询时，按"设备 → 唯一 Profile"
来写，不要假设一台设备能挂多个模板。
:::

位号是数据的最小单位。它的两个关键标志位决定了它能干什么：

- `pointTypeFlag`（`PointTypeEnum`）——值的数据类型。
- `rwFlag`（`RwTypeEnum`）——读写方向。**一个位号能不能写，由它自己的 `rwFlag` 决定，而不是命令表。** 试图写一个 `READ_ONLY`
  位号会在命令校验阶段被拒。

位号还带工程量信息：`unit`（单位）、`valueDecimal`（小数精度，默认 `6`）、以及线性换算 `baseValue` / `multiple`
——把驱动采到的原始值变换成工程值（`工程值 = 原始值 × multiple + baseValue` 的语义由驱动实现）。

::: info 位号类型枚举其实有 8 个，不止 4 个
`introduction/concepts` 和 Add Point 的 API 表为了好懂，只列了 `STRING / INT / FLOAT / DOUBLE`。源码里 `PointTypeEnum` 实际有
8 个值：`STRING(0) / BYTE(1) / SHORT(2) / INT(3) / LONG(4) / FLOAT(5) / DOUBLE(6) / BOOLEAN(7)`（`PointTypeEnum.java`）。
`rwFlag` 对应 `RwTypeEnum`：`READ_ONLY(0) / WRITE_ONLY(1) / READ_WRITE(2)`。以代码为准。
:::

### 领域实体关系

下图把根对象 Profile、它的三类子能力、设备与驱动的绑定，以及"三层配置"
里的属性/配置关系画在一起。它比 [核心概念](../introduction/concepts) 里的那张更全——多出了协议层 `*Attribute` 与实例层
`*AttributeConfig`。

<DomainModelErDiagram lang="zh" />

`profileShareFlag`（`ProfileShareTypeEnum`：`TENANT / DRIVER / USER`）控制模板的共享范围；`Event` 的 `event_type_flag`（
`0=info / 1=alert / 2=fault / 3=lifecycle`）是事件**定义**上的分类，存在 `dc3_event` 表（管理域，由 `04-iot-dc3-manager.sql`
建）。这点容易和告警混淆，下文专门讲。

## 三层配置：Param、Attribute、Config 各管一摊

这是领域模型里最容易混的地方。平台把"配置"拆成**三个作用域完全不同**的层，每层回答不同的问题、由不同的人/流程产生、对应不同的
DO 类：

<DomainModelLayerDiagram lang="zh" />

- **Param（业务层）** —— `CommandParamDO` / `EventParamDO`。它描述模板里一个命令/事件的输入输出参数，是**业务语义**，和具体协议无关。
- **Attribute（协议层）** —— `DriverAttributeDO` / `PointAttributeDO` / `CommandAttributeDO` / `EventAttributeDO`。它由驱动在
  **启动时注册**：驱动读自己的 `application.yml`，告诉管理中心"我这个协议需要哪些配置项"。比如 Modbus 驱动会声明"
  位号需要一个寄存器地址"——这是 Attribute，定义的是**有哪些项**，不含值。
- **Config（实例层）** —— `PointAttributeConfigDO`（以及 `DriverAttributeConfigDO` / `CommandAttributeConfigDO` /
  `EventAttributeConfigDO`）。它存的是**这台设备**为那些属性填的**具体值**。`PointAttributeConfigDO` 的核心字段就是
  `attributeId`（指向哪个属性）+ `deviceId` + `pointId` + `configValue`（填的值）。比如"3 号设备的温度位号，寄存器地址是
  40001"——`40001` 就落在这里。

一句话区分：**Attribute 说"有这个坑"，Config 说"这个坑填什么"。**
理解这层映射，才能看懂 [设备接入](../operation/device-onboarding) 里"配置位号属性"那一步，以及
`POST /api/v3/manager/point_attribute_config/add`（请求字段正是 `attributeId` / `deviceId` / `pointId` / `configValue`
）到底在写什么。

## 同一份数据的三种形态：DO / BO / VO

一个领域对象在系统里有三种长相，对应三个层、三种关注点。以位号为例：`PointDO` / `PointBO` / `PointVO`。

- **DO（`*DO`，如 `PointDO`）—— 数据库形态。** 它是 `dc3_point` 表的镜像：标志位是裸 `Byte`（`pointTypeFlag`、`rwFlag`、
  `enableFlag` 都是 `Byte`），带 MyBatis-Plus 注解 `@TableName` / `@TableId(type = ASSIGN_ID)`（Snowflake ID）/
  `@TableLogic`（逻辑删除 `deleted`）/ JSON 扩展用 `JacksonTypeHandler`。DO 只在持久层出现，**裸 `Byte` 标志位不允许泄漏到业务层或对外响应
  **。
- **BO（`*BO`，如 `PointBO`）—— 业务形态。** 同样的标志位在这里是**域枚举**：`pointTypeFlag` 是 `PointTypeEnum`、`rwFlag` 是
  `RwTypeEnum`、`enableFlag` 是 `EnableFlagEnum`。BO 继承 `BaseBO` 并实现 `TenantOwned`（携带 `tenantId`
  ，租户隔离的起点）。业务代码、Service 之间传的是 BO，不是 VO。换算字段在 BO 里用 `BigDecimal`（`baseValue` / `multiple`），到
  DO 落库时是 `Double`——精度边界也由 `*Builder` 处理。
- **VO（`*VO`，如 `PointVO`）—— API 形态。** Controller 的请求/响应用 VO。它和 BO 一样用域枚举，除非为兼容旧客户端要保留原始数值。

下图是三层在调用链里的站位，以及 MapStruct `*Builder` 负责的转换方向。

<DomainModelClassDiagram lang="zh" />

`PointController` 收到 `PointVO` 后用 `PointBuilder.buildBOByVO()` 转成 `PointBO` 交给 `PointService`；Service 用
`buildDOByBO()` 转成 `PointDO` 交给 `PointManager` 落库；读取时反向走 `buildBOByDO()` → `buildVOByBO()`。`select*` 这类原始
Mapper 方法只在 `*ManagerImpl` 里出现，Service / Controller 一律用 `get*` / `list*` / `add` / `update` / `delete`
（见 [API 文档](../development/api-documentation) 的 CRUD 动词约定）。

## 枚举与 JSON 扩展：`@AfterMapping` 是关键

MapStruct 能自动映射同名同类型字段，但 `Byte ↔ 域枚举`、`JSON 字符串 ↔ 扩展对象`这两类转换它不会，得在 `*Builder` 的
`@AfterMapping` 钩子里手写。这正是 DO/BO/VO 分层不"漏"的地方。

枚举两端的契约很固定：DO 里存的 `Byte` 就是枚举上 `@EnumValue` 标注的 `index`；DO→BO 用 `XxxEnum.ofIndex(byte)`
把数值变枚举，BO→DO 用 `enum.getIndex()` 取回数值。下面这条时序就是 `PointBuilder` 里读一行位号时真实发生的事：

<DomainModelSequenceDiagram lang="zh" />

对应到 `PointBuilder.java` 里的真实代码：`buildBOByDO` 上把 `pointTypeFlag` / `rwFlag` / `enableFlag` 标了
`@Mapping(ignore = true)`，再在 `@AfterMapping` 里逐个 `RwTypeEnum.ofIndex(entityDO.getRwFlag())` 赋回；反方向
`buildDOByBO` 的 `@AfterMapping` 用 `Optional.ofNullable(rwFlag).ifPresent(v -> entityDO.setRwFlag(v.getIndex()))`。
`null` 安全是显式处理的——枚举为空就不写，不会抛 NPE。

JSON 扩展同理。`PointDO.pointExt` 是 `JsonExt`（`content` 存成 JSON 字符串，配 `JacksonTypeHandler` 落库），到 BO 是强类型的
`PointExt`。`@AfterMapping` 里 DO→BO 用 `JsonUtil.parseObject(content, PointExt.Content.class)` 反序列化，BO→DO 用
`JsonUtil.toJsonString(...)` 序列化。所有扩展对象都带 `BaseExt` 的三件套字段：`type`（解析时识别子类型）、`version`（乐观锁，默认
`1`）、`remark`。

::: tip 加一个带枚举或 JSON 扩展的新字段时

1. DO 加 `Byte` 字段 + `@TableField`；BO/VO 加对应的**枚举**字段。
2. 在 `*Builder` 上对该字段加 `@Mapping(target = "xxx", ignore = true)`（DO↔BO 两个方向都要）。
3. 在两个 `@AfterMapping` 里补 `ofIndex` / `getIndex` 转换；JSON 扩展则补 `parseObject` / `toJsonString`。
   漏掉第 2、3 步时 MapStruct 会因类型不匹配编译失败或静默丢值——改完务必
   `mvn -s .mvn/settings.xml -q -DskipTests compile` 兜底。
   :::

## 枚举命名：看后缀就知道语义

平台的标志位枚举用后缀编码语义，三类各有约定：

| 后缀            | 语义     | 例子                                                  |
|---------------|--------|-----------------------------------------------------|
| `*FlagEnum`   | 0/1 开关 | `EnableFlagEnum`（`ENABLE(0)` / `DISABLE(1)`）        |
| `*StatusEnum` | 状态机    | `PointCommandStatusEnum`（`PENDING → SENT → ...`）    |
| `*TypeEnum`   | 分类集合   | `PointTypeEnum`、`RwTypeEnum`、`ProfileShareTypeEnum` |

::: warning `EnableFlagEnum` 的 0 是"启用"，不是"禁用"
`ENABLE` 的 index 是 `0`、`DISABLE` 是 `1`（`EnableFlagEnum.java`）。很多人直觉里 0 是 false/关，这里反了。读 SQL 时
`enable_flag = 0` 表示**启用**。
:::

## dc3_event 是定义，dc3_entity_alarm 是实例

最后一个易混点，也是领域建模里"模型 vs 运行实例"的典型分界：

- `dc3_event`（管理域，`04-iot-dc3-manager.sql`）—— 事件**定义**。它挂在 Profile 下，描述"这类设备会上报哪种事件"，带
  `event_type_flag`（`0=info / 1=alert / 2=fault / 3=lifecycle`）。这是模板能力的一部分，和 Point、Command 平级。
- `dc3_entity_alarm`（数据域，`03-iot-dc3-data.sql`）—— 运行期**告警实例**。它是规则引擎、状态超时、设备/驱动/事件上报等多个来源在运行时产生的记录，按
  `alarm_source_flag` 区分来源。

换句话说：`dc3_event` 回答"这设备**能**报什么"，`dc3_entity_alarm` 回答"现在**报了**什么"。两张表在不同 schema、不同 init
脚本里，新增查询时别把它们当一回事。告警与事件的完整模型（规则、通知通道、状态跟踪）见 [告警与通知](../operation/alarms)。

## 延伸阅读

- [核心概念](../introduction/concepts) — 没读过先补这张更简的对象关系图与一句话心智模型
- [数据平面](./data-plane) — `PointValue` 从 `ReadPointValue` 到 `PointValueDO` 的逐层变换与落库
- [驱动开发](../development/driver-authoring) — 驱动如何注册 Attribute、把领域对象落成一个真实协议适配
- [API 文档](../development/api-documentation) — `get`/`list`/`add`/`update`/`delete` 动词约定与 OpenAPI
- [告警与通知](../operation/alarms) — `dc3_entity_alarm` 的来源、规则与通知链路
