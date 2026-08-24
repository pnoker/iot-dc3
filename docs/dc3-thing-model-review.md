# DC3 物模型（Profile）代码级核实与最终判定

|                |                                                                 |
|----------------|-----------------------------------------------------------------|
| **Status**     | Code-level review — factual findings, not a roadmap               |
| **Date**       | 2026-08-18                                                       |
| **Scope**      | 从代码层面核实「DC3 的 Profile 是否/如何等价于物模型」，并给出最终判定    |
| **Related**    | [comparison-dgiot.md](./comparison-dgiot.md)、[comparison-supos-free.md](./comparison-supos-free.md) |
| **Method**     | grep 全仓 + 逐文件读源码，证据均落到类/方法/表名                       |

> 本文是「DC3 是否真的有一套物模型、这套 Profile 是否比物模型强」这一问题的**代码级答案**。
> 结论依据仓库当前代码，而非文档宣称。判定基准是物模型的标准语义：定义层（属性/服务/事件）+ 运行时（设备影子、模型实例化、物模型贯通下游）。

---

## 1. 最终判定（先看结论）

DC3 的 Profile 是一套「**半套活的物模型**」：

- **定义层**：完整且严谨（属性/服务/事件 + 量程/单位/分级/参数 + DO/BO/VO + 多租户）。
- **运行时 — 属性、服务两条线**：真实落地，且语义完整（量程缩放真应用、服务调用带参数带返回还带锁带审计）。
- **运行时 — 物模型核心语义**：缺失（无设备影子、无物模型实例化/状态机、物模型未贯通规则/通道/组态）。

因此「Profile 比物模型强」**部分成立**：在「定义严谨度」和「属性/服务运行时落地」上成立；在「运行时物模型语义」上不成立。

---

## 2. 物模型定义层：完整且严谨

核心实体位于 `dc3-common/dc3-common-manager/.../entity/model/`，物模型三要素一一对应：

| 物模型要素 | DC3 实体（DO） | 表 | 关键字段 |
|---|---|---|---|
| 模板 | `ProfileDO` | `dc3_profile` | `profileName`/`profileCode`、`profileShareFlag`（共享标志）、`profileTypeFlag`、`profileExt` |
| 属性 Properties | `PointDO` + `PointAttributeDO` | `dc3_point` | `pointTypeFlag`、`rwFlag`（读写）、`baseValue`/`multiple`/`valueDecimal`/`unit`（量程缩放+精度+单位）、`profileId`、`pointExt` |
| 服务 Services | `CommandDO` + `CommandAttributeDO` + `CommandParamDO` | `dc3_command` | `commandTypeFlag`、`callTypeFlag`（调用方式）、`timeout`、`profileId` |
| 事件 Events | `EventDO` + `EventAttributeDO` + `EventParamDO` | `dc3_event` | `eventTypeFlag`、`eventLevelFlag`（事件分级）、`profileId` |
| 设备实例 | `DeviceDO` | `dc3_device` | `driverId` + `profileId`（协议 + 物模型双绑定） |

层级关系：

```
Profile(物模型模板)
 ├── Point      属性（可带量程缩放 baseValue×multiple、单位、精度）
 ├── Command    服务（含 CommandAttribute 参数、CommandParam 下发参数）
 └── Event      事件（含 EventAttribute 属性、EventParam 参数、事件分级）
Device = Driver(协议) + Profile(物模型)
```

---

## 3. 运行时落地情况（代码证据）

### 3.1 属性 — 采集 + 量程缩放 ✅ 真落地

`ReadPointValue.calculate()` → `TypedValueConverter.calculatePointValue(rawValue, point)`：

- `linearValue()` 应用线性变换 **`multiple × value + base`**；
- `roundedDouble()`/`roundedFloat()` 应用 **`valueDecimal`** 精度舍入；
- 类型枚举带范围/合法性校验（`exactInt`/`finiteDouble`/`strictBoolean`）。

> `baseValue`/`multiple`/`valueDecimal`/`unit` 不是「只存不用的字段」，而是采集读路径上的真实计算。

证据：`dc3-common-driver/.../support/TypedValueConverter.java`、`dc3-common-driver/.../entity/bean/ReadPointValue.java`

### 3.2 属性 — 读写命令 ✅ 真落地

点位读写走 `PointCommandServiceImpl.read()/write()` + `PointCommandValidator`（scope 校验）+ `PointCommandHistory`（历史）。

证据：`dc3-common-data/.../biz/impl/PointCommandServiceImpl.java`

### 3.3 服务 — 自定义命令 ✅ 真落地，语义完整

`CommandReceiver.commandReceive(CommandCallDTO)` 完整消费「服务」要素：

- `commandFacade.getById(tenantId, commandId)` 解析 `FacadeCommandBO`（服务定义）；
- `deviceMetadata.getCommandConfig(deviceId, commandId)` 取命令级属性配置（`CommandAttributeConfig`）；
- `driverCustomService.execute(driverConfig, commandConfig, device, command, paramValues)` 带**入参**执行、返回**出参**；
- 配套：`CommandDedupCache`（去重）、`DeviceLockManager`（设备级锁）、`expireAt`（过期预检）、重试/requeue、FAILED 落底、`buildConfigSnapshot`（审计快照）。

证据：`dc3-common-driver/.../receiver/rabbit/CommandReceiver.java`、`dc3-common-driver/.../service/DriverCommand.java`

### 3.4 事件 ⚠️ 半落地

`EventHistoryServiceImpl` 用 `FacadeEventBO` 做事件历史的 scope 校验（`resolveEvent` → `eventFacade.listByPage`），但 `EventAttribute`/`EventParam` 未在运行时深度消费。

证据：`dc3-common-data/.../biz/impl/EventHistoryServiceImpl.java`

---

## 4. 真正缺失的部分（代码证据）

### 4.1 设备影子（reported/desired 属性集合）❌ 缺失

- 全仓库 grep `shadow` = **0 命中**。
- 仅有 `EntityStateDO`（表 `dc3_entity_state`）：`entityTypeFlag`/`stateFlag`/`lastStateFlag`/`leaseVersion`/`expireTime`/`lastHeartbeatTime`/`lastAlarmId`/`timeoutSeconds`。
- 它是「心跳租约 + 在线/离线状态 + 超时」管理，**不是**物模型的「设备当前每个属性的 reported（上报值）vs desired（期望值）影子」。

证据：`dc3-common-data/.../entity/model/EntityStateDO.java`

### 4.2 物模型实例化 / ontology / 状态机 ❌ 缺失

- 全仓库 grep `instance`/`thingModel` = **0 命中**。
- `DeviceDO` 只是 `driverId` + `profileId` 两个外键的管理实体，不是「物模型运行时实例」。
- 对照 DG-IoT 的 `spawn_instance`/gen_statem（每设备一个影子进程、状态机 init→auth→online→normal/alarm/offline），DC3 无此层。

### 4.3 物模型 → 规则引擎 / 数据通道 / 组态 ❌ 未贯通

- grep `ProfileService`/`ProfileManager` 命中全在 **manager 域 CRUD**，无一处出现在 data 域规则引擎/通道/组态的消费点。
- 告警规则（`RuleStateService`/`RuleExt`）在 data 域独立运行，不读取 Profile。
- 无「按物模型」的数据通道编排；无组态/大屏绑定。

### 4.4 物模型共享 ⚠️ 有底座、无入口

- `ProfileDO.profileShareFlag`（共享/私有）已存在，但缺导入/导出/分享的前端入口与版本化（即「模板市场」未落地）。

---

## 5. 最终判定表

| 维度 | 判定 | 证据 |
|---|---|---|
| 物模型定义层（属性/服务/事件 schema） | ✅ 完整严谨 | `ProfileDO`/`PointDO`/`CommandDO`/`EventDO` + DO/BO/VO + 多租户 |
| 属性运行时（采集 + 量程缩放） | ✅ 真落地 | `TypedValueConverter.calculatePointValue`、`ReadPointValue.calculate()` |
| 属性运行时（读写命令） | ✅ 真落地 | `PointCommandServiceImpl` |
| 服务运行时（命令 + 参数 + 返回） | ✅ 真落地，语义完整 | `CommandReceiver` + `DriverCommand.execute` + 去重/锁/超时/审计 |
| 事件运行时 | ⚠️ 半落地 | `EventHistoryServiceImpl` 仅做历史 scope 校验 |
| 设备影子（reported/desired） | ❌ 缺失 | grep `shadow`=0，仅 `EntityState` 心跳状态 |
| 物模型实例化 / ontology / 状态机 | ❌ 缺失 | grep `instance`=0，`DeviceDO` 是外键实体 |
| 物模型 → 规则 / 通道 / 组态 | ❌ 未贯通 | Profile 仅在 manager CRUD，不被下游消费 |
| 物模型共享 | ⚠️ 有底座 | `profileShareFlag` 存在，缺市场/导入导出/版本化 |

---

## 6. 结论

- **成立的部分**：DC3 的 Profile 在「定义严谨度」和「属性/服务两条线的运行时落地」上，确实强于多数「只有 schema、不落地」的物模型框架——量程缩放真的在采集管线里应用，服务调用真的带参数带返回、还带设备锁与审计。
- **不成立的部分**：DC3 缺「运行时物模型」的三样核心语义——**设备影子、物模型实例化/状态机、物模型贯通规则/通道/组态**。因此它不是「完整物模型」，而是「半套活的物模型」。

若要把 DC3 补成「完整的运行时物模型」，按优先级依次是：**设备影子（补 reported/desired 语义，演进 `EntityStateDO`）→ 物模型贯通规则引擎 → 物模型贯通数据通道/组态 → 物模型实例化/ontology（可选，视是否需要数字孪生级能力）**。

## 7. 附录：关键代码证据位置

| 关注点 | 文件 |
|---|---|
| 物模型定义 | `dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/entity/model/` |
| 量程缩放 | `dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/support/TypedValueConverter.java` |
| 读值封装 | `dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/entity/bean/ReadPointValue.java` |
| 属性读写命令 | `dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/biz/impl/PointCommandServiceImpl.java` |
| 服务调用入口 | `dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/receiver/rabbit/CommandReceiver.java` |
| 服务执行契约 | `dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/service/DriverCommand.java` |
| 事件历史 | `dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/biz/impl/EventHistoryServiceImpl.java` |
| 设备状态（非影子） | `dc3-common/dc3-common-data/src/main/java/io/github/pnoker/common/data/entity/model/EntityStateDO.java` |
