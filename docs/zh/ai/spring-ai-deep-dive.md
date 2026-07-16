---
title: "为什么选择 Spring AI：DC3 如何让大模型操控你的工厂"
---

<script setup>
import SpringAiSequenceDiagram from '../../.vitepress/theme/components/SpringAiSequenceDiagram.vue'
</script>


# 为什么选择 Spring AI：DC3 如何让大模型操控你的工厂

2025 年，大语言模型（LLM）不再只是聊天机器人，它们正在成为运维操作员。GPT-4o、Claude 4、DeepSeek、Qwen —
这些模型已经能够读取传感器数据、推理设备状态，并判断是否需要开启某个阀门。唯一的缺失环节，是"模型理解"与"模型执行"
之间的桥梁。这座桥梁就是 Spring AI，也是 IoT DC3 选择它作为 Agentic Center 基座的根本原因。

## 问题所在：AI 想行动，平台说不行

传统 IoT 平台将数据和指令视为两个独立的世界。数据从设备北上流向仪表盘，指令从操作员南下流向设备。
这两个方向很少在单次 API 调用中交汇，更不用说在单句自然语言中完成。

来看一个真实场景：操作员发现 3 号锅炉温度异常。在传统平台中，工作流如下：

1. 导航到仪表盘，找到 3 号锅炉，记下温度。
2. 切换到历史页面，查询过去 2 小时的温度数据。
3. 大脑中评估：这是趋势还是脉冲？
4. 导航到指令页面，选择"风机转速"，输入新值。
5. 提交，等待确认，验证温度开始下降。

五个上下文切换，跨越三个页面。每次切换耗时 15–30 秒。乘以 200 台设备，运维摩擦就变成了瓶颈。

而在 DC3 的 Agentic Center 中，操作员只需输入一句话：

> "3 号锅炉温度在上升。检查过去 2 小时的数据，如果持续上升超过 30 分钟，将排风机降到 60%。"

模型将这句话拆解为工具调用序列，执行读取和有条件写入，并在单轮对话中回报结果——
这不是 Demo，而是运行在 DC3 的 Spring AI 工具调用基础设施上的生产级能力。

## 为什么是 Spring AI，而非 LangChain 或自研

团队在选定 Spring AI 之前，评估了三种方案：

| 方案                     | 优势                                          | 劣势                           | 结论             |
|------------------------|---------------------------------------------|------------------------------|----------------|
| **LangChain (Python)** | 生态庞大，原型开发快                                  | Python/JVM 互操作开销、独立部署、安全边界模糊 | 对 JVM 原生平台过于沉重 |
| **自研 @Tool 框架**        | 完全掌控，零依赖                                    | 数月工程投入，长期维护负担，无社区支撑          | 重复造轮子          |
| **Spring AI**          | 原生 JVM、Spring Boot 深度集成、OpenAI 兼容、类型安全的工具定义 | 生态较新（2024+）                  | ✅ 最佳选择         |

Spring AI 在三个决定性维度上胜出：

### 1. 原生 JVM，无需 Python 桥接

DC3 是纯 Java 21 / Spring Boot 4 平台。每个服务都在 JVM 上运行。为引入 AI 而加入 Python 运行时，
意味着额外的容器、额外的部署、以及两个语言运行时之间脆弱的网络桥接。

Spring AI 运行在进程内——`ChatClient` 是一个 Spring Bean，工具调用是普通的 Java 方法调用，
认证上下文通过与其他任何请求相同的 Spring Security 过滤器链流转。

```java
// DC3 中的一个 @Tool 方法——纯 Java、类型安全、租户感知
@Tool(description = "获取指定测点的最新值")
public PointValueBO getLatestPointValue(
        @ToolParam(description = "测点ID") Long pointId,
        ToolContext toolContext) {
    var tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
    return pointValueService.getLatestByPointId(pointId, tenantId);
}
```

无需跨语言的序列化，无需独立的认证流程，无需在 AI 和数据之间插入 gRPC 或 HTTP ——
这是直接 Java 调用到服务层，租户隔离在方法级别执行。

### 2. 天生 OpenAI 兼容

Spring AI 的 `ChatClient` 使用 OpenAI Chat Completions 协议。这意味着 DC3 可以与任何暴露 OpenAI
兼容端点的模型提供商配合：OpenAI (GPT-4o, GPT-5)、Anthropic (通过兼容代理的 Claude)、DeepSeek、
Qwen、Groq、Together AI、Ollama (本地模型)、vLLM —— 列表还在不断增长。

对运维人员而言，这意味着选择自由：先用云端模型方便上手，再切换到自托管模型保证数据主权，
或者采用混合方案，敏感查询留在本地。Agentic Center 的 `dc3_model_provider` 表支持配置多个提供商，
并可按会话切换。

### 3. 类型安全的工具定义，编译期校验

在基于 Python 的工具调用框架中，工具定义通常是 JSON Schema 或带字符串描述的装饰器。
参数名的拼写错误就是运行时错误。而在 Spring AI 中，`@Tool` 和 `@ToolParam` 注解由 Java 编译器验证。
如果你修改了方法签名中的参数名但忘了更新注解，IDE 会在编译之前就标记出来。

当你拥有 10 个工具类、30+ 个工具方法以及一个贡献者团队时，这一点至关重要。
编译器成为了 Python 工具调用框架无法提供的安全网。

## 架构全景：一条聊天消息如何变成设备指令

以下是 DC3 中一次典型 AI 辅助操作的完整路径，端到端：

<SpringAiSequenceDiagram lang="zh" />

四个要素使这个架构达到生产级别：

1. **每层租户隔离。** 任何工具执行之前，`requireTenantId(toolContext)` 提取调用方的租户 ID。
   每次数据库查询、每个缓存键、每次 Facade 调用都携带该租户 ID。即使模型错误地猜测了另一租户
   的设备 ID，查询也是返回空结果——而非返回错误数据。

2. **写入指令需要人工确认。** 模型可以*提议*写入，但无法直接执行写入。
   `PointValueTool.writePointValue()` 生成一个待确认的 `Action`，10 分钟有效期。
   只有人工 `POST /action/confirm`（或同等的 UI 按钮点击）才能释放它。这不是建议——
   而是在服务层强制执行，不仅限于 UI 层。

3. **会话可跨重启持久化。** 每一轮——用户消息、工具调用、工具结果、助手回复——
   都落入 `dc3_message` 表。即使 Agentic Center 重启，`MessageChatMemoryRepository`
   会回放最近 30 轮（可配置），对话无缝恢复。这对跨小时的诊断会话至关重要。

4. **模型永远看不到其他租户的数据。** 租户 ID 由网关注入（来自 JWT），而非由模型指定。
   即使提示词说"显示所有设备"，工具的 SQL 查询中带有 `WHERE tenant_id = ?`，
   绑定到网关注入的值。模型无法绕过——不存在跨租户查询的 API。

## 10 个内建工具：完整的运维操作面

Agentic Center 出厂自带 10 个工具类，覆盖平台每个领域对象。每个工具方法包装已有的服务层方法——
不存在重复的业务逻辑。

| 工具类              | 领域  | 关键方法                                                                                       | 风险等级       |
|------------------|-----|--------------------------------------------------------------------------------------------|------------|
| `TenantTool`     | 租户  | `getCurrentTenantInfo()`                                                                   | 低          |
| `UserTool`       | 用户  | `getCurrentUserProfile()`                                                                  | 低          |
| `DeviceTool`     | 设备  | `lookupDeviceById()`, `searchDevices()`                                                    | 低          |
| `DriverTool`     | 驱动  | `lookupDriverById()`, `searchDrivers()`                                                    | 低          |
| `ProfileTool`    | 模板  | `lookupProfileById()`, `searchProfiles()`                                                  | 低          |
| `PointTool`      | 位号  | `lookupPointById()`, `searchPoints()`                                                      | 低          |
| `PointValueTool` | 位号值 | `getLatestPointValue()`, `getPointValueHistory()`, `readPointValue()`, `writePointValue()` | **高 (写入)** |
| `SystemTool`     | 系统  | `getSystemHealth()`                                                                        | 低          |
| `CommandTool`    | 指令  | `lookupCommandById()`, `searchCommands()`                                                  | 低          |
| `EventTool`      | 事件  | `lookupEventById()`, `searchEvents()`                                                      | 低          |

工具方法刻意使用与 REST/gRPC 层不同的命名规范。REST 端点遵循项目的 `getXxx`/`listXxx` 约定；
工具方法使用 `lookupXxx`/`searchXxx`。这种分离让模型能够区分"按 ID 获取一个"
(`lookupDeviceById`) 和"分页搜索"(`searchDevices`)，这对模型选择正确的工具至关重要。

## 为什么这对工业 IoT 意义深远

工业场景是 AI 辅助运维的完美用武之地：

- **高认知负荷。** 一名工厂操作员需要同时监控数十个屏幕、数百个数据点，必须在数秒内响应异常。
  模型可以同时观察所有这一切。

- **结构化、边界明确的动作。** 与开放式的创意任务不同，工业运维有清晰的边界：
  读取传感器、检查阈值、调整执行器。这些完美映射到工具调用模式。

- **可审计性不可妥协。** DC3 中的每一次 AI 辅助操作都被记录到 `dc3_message` 和
  `dc3_action` 表中。谁问了什么、模型决定了什么、调用了哪些工具、结果是什么、
  谁确认了写操作——全部可审计。

- **多供应商现实。** 工厂中可能存在 Siemens、Rockwell、Mitsubishi 等十几种品牌的设备。
  DC3 的 28 个协议驱动将这种异构性抽象为统一的设备模型，AI 工具查询的就是这个统一模型——
  模型无需知道 Modbus 寄存器映射或 OPC UA 节点 ID。

## 路线图：下一步走向何方

今天的 Agentic Center 处理的是描述性和诊断性操作："温度是多少""为什么在上升""给我看趋势"。
下一个前沿是*建议性*操作：

- **异常到动作管道。** 模型在数据流中检测到异常，通过工具调用诊断根因，
  并在操作员打开仪表盘之前提出纠正措施。

- **定时健康报告。** 每天早上 7 点，Agentic Center 生成一份自然语言交接报告：
  昨晚哪些设备离线、哪些位号趋势异常、建议哪些维护动作。

- **多模型路由。** 将简单查询路由到快速廉价的模型 (GPT-4o mini)；
  将复杂诊断路由到推理模型 (GPT-5 或 Claude)；
  将敏感的本地查询路由到本地 Ollama 实例。`dc3_model_provider` 表和按会话的模型选择
  已支持这一点——路由逻辑是下一层。

- **MCP 外部 Agent 接入。** Agentic Center 面向人类操作员，而 MCP 端点 (OAuth 2.1 +
  JSON-RPC 2.0) 让外部 AI Agent 访问同一工具面——即将推出的 MCP ↔ Agentic 桥接
  将使对话可以从自动化 Agent 无缝升级到人类操作员。

---

> **下一步：** 查看 [Agentic Center](./agentic) 获取完整工具参考和配置指南。
> 查看 [AI Agent / MCP](./mcp) 接入外部 Agent。
> 查看 [快速开始](../quickstart/first-device) 让你的第一台设备上线。
