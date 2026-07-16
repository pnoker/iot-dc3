---
title: 指令 Command
---

<script setup>
import CommandRelationDiagram from '../../../.vitepress/theme/components/CommandRelationDiagram.vue'
import CommandFlowDiagram from '../../../.vitepress/theme/components/CommandFlowDiagram.vue'
</script>

# 指令 Command

> **指令是下发给设备的一次动作请求**——重启、校准、切换模式、设定温度……定义归属[模板 Profile](./profile)
> ，调用归属[设备](./device)，带一组输入/输出参数，由[驱动](./driver)执行后回执结果。

指令回答的是"让设备做一件事"，它是[事件](./event)的对偶：事件上行（设备说"发生了什么"），指令下行（平台说"去做什么"）。

## 它是什么、为什么需要

工业设备除了"上报数值"和"被读写某个量"，还需要被触发**动作型能力**：重启、固件升级、模式切换、按模板下发一段配置。这些动作往往带参数、需要回执、要做超时与审计——把它们建模成
`Profile` 下的结构化子资源，就是**指令 Command**。定义沉淀在模板里（这类设备能做哪些动作、每个动作收哪些参数），调用落到具体设备实例（某台设备某时刻执行了一次）。

### 最关键的区分：两类下行不要混淆

DC3 有两条独立的下行链路，初学者最容易混淆：

| 维度   | 写位号 PointCommand                    | 自定义指令 Command / CommandCall               |
|------|-------------------------------------|-------------------------------------------|
| 改什么  | 改**某一个量**（一个 [Point](./point) 的值）   | 触发**一个带参数的动作**                            |
| 定义来源 | [Point](./point) 的 `rwFlag` 含 WRITE | `dc3_command` 独立定义表                       |
| 驱动接口 | `DriverCustomService.write()`       | `DriverCommand.execute()`                 |
| 参数   | 单值（一个目标值）                           | 结构化输入/输出参数 Map                            |
| DTO  | `PointCommandDTO`                   | `CommandCallDTO` / `CommandCallResultDTO` |
| 队列前缀 | `dc3.e.point_command`               | `dc3.e.command`                           |

一句话边界（见设计稿 point-command.md §1.2）：**写位号是属性维度的运行态访问，自定义指令是模板层的动作能力。**

::: tip 一个例子讲清边界
给空调"把目标温度设为 26℃"——这是**写位号**：`targetTemp` 是一个可写 Point，写入 `26` 即可，本质是改一个量。
给空调"执行一次自清洁"——这是**自定义指令** `selfClean`：它不对应某个量，而是一个动作，可能还带参数（如 `duration=30`），执行完返回
`resultCode`。
判断口诀：能落到"改某个 Point 的值"就是写位号；是"触发一段动作流程"就是指令。
:::

本页讲的"指令 Command"指**自定义指令**。写位号请看 [位号 Point](./point)。

## 关键字段

指令定义 `CommandBO`（归属 [Profile](./profile)，表 `dc3_command`）：

| 字段                | 类型              | 含义                                                   |
|-------------------|-----------------|------------------------------------------------------|
| `commandName`     | String          | 指令名称（展示用）                                            |
| `commandCode`     | String          | 指令标识符，同一 `profileId` 下唯一，调用时按它匹配（如 `setTemperature`） |
| `commandTypeFlag` | CommandTypeEnum | 指令类型，见下                                              |
| `callTypeFlag`    | CallTypeEnum    | 调用方式：`sync` / `async`                                |
| `timeout`         | Integer         | 调用超时时间（秒）                                            |
| `commandExt`      | CommandExt      | 扩展配置（协议映射、驱动指令模板、幂等等）                                |
| `profileId`       | Long            | 归属的[模板](./profile)                                  |
| `enableFlag`      | EnableFlagEnum  | 启停状态                                                 |
| `tenantId`        | Long            | 归属[租户](./tenant)                                     |

指令参数 `CommandParamBO`（声明指令的输入/输出参数，归属指令定义）：

| 字段                        | 类型                     | 含义                           |
|---------------------------|------------------------|------------------------------|
| `paramName` / `paramCode` | String                 | 参数名 / 标识符                    |
| `paramDirectionFlag`      | ParamDirectionTypeEnum | 方向：`input`（入参）/ `output`（出参） |
| `paramTypeFlag`           | PointTypeEnum          | 参数数据类型                       |
| `requiredFlag`            | Boolean                | 是否必填                         |
| `defaultValue`            | String                 | 默认值                          |
| `commandId`               | Long                   | 归属的指令定义                      |

::: tip CommandParam 复用位号的类型系统
`paramTypeFlag` 用的是和位号一样的 `PointTypeEnum`（`STRING` / `INT` / `FLOAT` / `DOUBLE` / `BOOLEAN`…）。注意区分：**入参**
是调用时由调用方传入（如 `temperature`），**出参**是执行后由设备回写（如 `resultCode`）。
:::

调用入参 `CommandCallBO`（一次调用的提交体）：`deviceId`、`commandId`、`commandCode`、`paramValues`（`Map<String,String>`，按参数
`paramCode` 键控）。

## 指令类型

| 类型 `commandTypeFlag` | 说明    |
|----------------------|-------|
| `custom`             | 自定义指令 |
| `config`             | 配置型指令 |
| `action`             | 动作型指令 |

## 与其它概念的关系

<CommandRelationDiagram lang="zh" />

- 指令**定义**挂在模板下，与[位号](./point)、[事件](./event)并列，共同描述"这类设备有什么能力"。
- 指令**调用**由[设备](./device)发起；驱动执行所需的协议映射由 [指令属性配置](./attribute-config)（`CommandConfig`
  ）提供，与业务参数 `CommandParam` 分属两层。

## 调用生命周期与回执

一次调用（`CommandCallDTO`）携带：`recordId`、`tenantId`、`deviceId`、`commandId`、`commandCode`、`paramValues`、`source`、
`occurredAt`、`expireAt`。数据中心持久化为一条 `dc3_command_history` 记录（PENDING），投递到 RabbitMQ，驱动执行后回执
`CommandCallResultDTO`（`status`、`resultValues`、`errorCode`、`errorMessage`、`finishedAt`）。

<CommandFlowDiagram lang="zh" />

调用状态机（`PointCommandStatusEnum`，与写位号共用）：

```
PENDING → SENT → SUCCESS / FAILED / TIMEOUT / EXPIRED / DUPLICATE / DEAD
```

| 状态                    | 含义                        |
|-----------------------|---------------------------|
| `PENDING`             | 已创建记录，等待投递                |
| `SENT`                | 已投递到 RabbitMQ，等待驱动执行      |
| `SUCCESS` / `FAILED`  | 驱动执行成功 / 失败，结果已回写         |
| `TIMEOUT` / `EXPIRED` | 应用层超时 / `expireAt` 已过期未执行 |
| `DUPLICATE` / `DEAD`  | 重复命令被去重拦截 / 拒入死信队列        |

::: warning 同步指令不等于"调用即完成"
`callTypeFlag = sync` 只表示调用方愿意等回执，**不代表 HTTP 立即返回执行结果**。当前 `/call` 返回 `recordId`，调用方据此轮询
`get_by_record_id` 拿终态。是否真正"做完"以回执里的 `status` 为准，别凭 HTTP 200 就认为设备已执行。
:::

## 示例

空调的模板里定义一个指令：`commandCode = setTemperature`、`commandTypeFlag = action`、`callTypeFlag = sync`、
`timeout = 10`，带一个输入参数 `temperature`（`paramDirectionFlag = input`、`paramTypeFlag = DOUBLE`、`requiredFlag = true`
）和一个输出参数 `resultCode`（`output`、`STRING`）。

调用时提交 `CommandCallBO{ deviceId: 1001, commandCode: "setTemperature", paramValues: { temperature: "26" } }`。数据中心校验设备
`profileId` 是否包含该指令，落一条 `dc3_command_history`（PENDING→SENT），投递给驱动；驱动 `execute()` 渲染协议报文下发到空调，回执
`CommandCallResultDTO{ status: SUCCESS, resultValues: { resultCode: "OK" } }`，记录推进到 SUCCESS。

## API

数据中心服务挂载于 `/data`：

| 方法   | 路径                                       | 说明                       |
|------|------------------------------------------|--------------------------|
| POST | `/data/command_history/call`             | 下发自定义指令，返回 `recordId`    |
| GET  | `/data/command_history/get_by_record_id` | 按 `recordId` 查询调用记录详情与终态 |
| POST | `/data/command_history/list`             | 分页查询调用记录                 |

## 延伸阅读

- [模板 Profile](./profile) — 指令定义挂在模板下
- [位号 Point](./point) — 写位号 vs 自定义指令的边界另一侧
- [事件 Event](./event) — 下行的对偶：指令下行、事件上行
- [指令/事件属性配置](./attribute-config) — `CommandConfig` 如何把参数映射成协议报文
- [命令平面](../../architecture/command-plane) — 下行链路的交换机 / 队列 / 回执 / 可靠性
- [数据与命令操作](../../operation/data-commands) — 如何在控制台下发指令
