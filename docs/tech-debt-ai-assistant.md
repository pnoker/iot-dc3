# AI 助手链路技术债修复指南（v2.1 · 已实施）

> v2 修订于 2026-10-03：对 v1 逐项做了源码级核查与全量测试运行。本版原则：
> ①每处"位置/现象/根因"均标注核查依据（`文件:行号`），无法从代码验证的运行时观察一律标注【未复现】；
> ②修复方案先验证假设、后动手，优先消除根因而非症状；
> ③编号沿用 v1 以保持外部引用稳定，**严重度为 v2 重估值**（P0-3 由高降为中）。
>
> **v2.1（2026-10-03 同日）：全部 10 项已按本指南修复完毕**，实施记录与偏差见各项"状态"行及附录 D；
> 基线既有的 scenario-corpus weak-assertion 失败一并根治。终态基线：前端 97 文件 7,153 项全绿
> （7,149 通过 + 4 跳过），后端 dc3-common-agentic 130 项全绿，compose 校验 9 文件 89 变量通过。

## 修复状态总表（v2.1）

| 项 | 状态 | 落点 |
|---|---|---|
| P0-1 | ✅ 已修复 | 观测测试 3 项 + 三处 builder `maxRetries` + per-provider 并发节流 |
| P0-2 | ✅ 已修复 | 负向验证闭环（2 红灯实锤 → 混合配对 → 19/19 绿） |
| P0-3 | ✅ 已修复（统一 true） | 部署侧 10 处；tool-calling 此前已统一，无需改动 |
| P1-4 | ✅ 已修复 | 删除冗余 `!important`，留注释说明 |
| P1-5 | ✅ 第 1 步已实施 | 只持久化活跃会话；IndexedDB 留作长期项 |
| P1-6 | ✅ 已修复 | `break-word` + code/pre 保留 `anywhere` |
| P2-7 | ✅ 已修复 | `priority` 显式声明 + 5 项新单测 |
| P2-8 | ✅ 已修复 | 收敛为 8 变体，保留三值语义（见状态注记） |
| P2-9 | ✅ 已修复 | CSS 变量 + 组件状态类；body class 保留（见偏差说明） |
| P2-10 | ✅ 已修复 | `resetMockChatEngine` 导出 + 隔离测试 |

## v1 → v2 关键更正

| 项 | v1 说法 | v2 更正 | 依据 |
|---|---|---|---|
| P0-1 | "默认行为不重试 429" | **错误**。openai-java 默认 `maxRetries=2`，自动对 429/408/409/5xx/连接错误做指数退避重试；缺口在重试深度、流式路径覆盖与并发治理 | openai-java 官方文档；`pom.xml:63` 版本 4.39.1 |
| P0-2 | 修复方案"按消息 ID 配对" | **方案不可行**。本地乐观 id 从未上报服务器，与回读 id 必不相同，纯 ID 配对会使合并完全失效。改为"id 配对 + 尾对齐"混合方案 | `agentic.ts:507`（前端生成 id）、`agentic.ts:523-532`（请求不含 id）、`agentic.ts:622`（服务器 id） |
| P0-3 | "三处默认值不一致""疑似调试后忘记恢复" | 实为 **12 处**（代码侧 2 处 true，部署侧 10 处 false）；git 证实部署默认 false 是**有意引入**的分层策略，非遗忘 | `git show d65fc715f`；全仓库 grep |
| P1-4 | "JS 写 340px 内联宽度被 CSS !important 覆盖（优先级战争）" | **现象不成立**。xs 断点下 `panelStyle` 本身返回 `{width:'100%'}`，与 CSS 值恒等，`!important` 是纯冗余，删除即可 | `AgenticAssistant.vue:423-425`；`useBreakpoint.ts:84`；`tokens.scss:40` |
| P1-5 | "每次流式 delta 都触发 persistMessages 全量写" | **不成立**。delta 路径直接赋值响应式数组，流式期间零 localStorage 写；实际每轮约 2-3 次。容量风险（全量序列化 + 静默溢出）保留 | `agentic.ts:909-917`；`storageUtil.ts:60-73` |
| P2-9 | 备选方案"用 @container/@supports 查询自定义属性" | **技术上不可能**，已删除。:has() 方案因面板与 .body-main 是兄弟关系（非祖先）也不适用 | `AgenticAssistant.vue:830` |
| 硬数据 | "现有 6,364 项测试全部通过"；验收引用 `tests/unit/agentic-store.test.ts` 与"现有 mock-adapter 测试" | 实测 **7,142 项**（7137 通过 / 1 项既有失败 / 4 跳过）；两个被引用的测试文件**均不存在** | 2026-10-03 全量 `pnpm test` 实跑 |

---

## 目录

- [P0-1: 模型 API 429 限流下重试深度与并发治理不足](#p0-1-模型-api-429-限流下重试深度与并发治理不足)
- [P0-2: 消息合并按索引配对可能错位](#p0-2-消息合并按索引配对可能错位)
- [P0-3: 记忆开关代码默认与部署默认分层未对齐](#p0-3-记忆开关代码默认与部署默认分层未对齐)
- [P1-4: xs 断点冗余 !important 死代码](#p1-4-xs-断点冗余-important-死代码)
- [P1-5: 全量消息缓存 localStorage 的容量风险](#p1-5-全量消息缓存-localstorage-的容量风险)
- [P1-6: overflow-wrap anywhere 作为默认策略](#p1-6-overflow-wrap-anywhere-作为默认策略)
- [P2-7: mock 路由优先级隐式于数组顺序](#p2-7-mock-路由优先级隐式于数组顺序)
- [P2-8: 布尔值字符串手写解析](#p2-8-布尔值字符串手写解析)
- [P2-9: 全局 body class 操作](#p2-9-全局-body-class-操作)
- [P2-10: 测试 mock DB 跨测试累积](#p2-10-测试-mock-db-跨测试累积)

---

## P0-1: 模型 API 429 限流下重试深度与并发治理不足

**状态**: ✅ 已修复（2026-10-03）。观测先行：新增 `TransportRetryBehaviorTest`（MockWebServer 真实 HTTP）3 项，实证 SDK 默认重试 429 深度为 2、`maxRetries(N)` 配置生效。修复：`AgenticProperties` 新增 `transportMaxRetries=4`、`transportMaxConcurrentRequests=4`；**三处** builder 显式 `maxRetries`（factory 的 OpenAI/Anthropic 分支 + `OpenAiCompatibleAgenticRuntime.createClient`——实施时发现的第三处：该处自建临时 client 不走 factory 缓存，v2 核查时遗漏）；并发节流以 `ChatClientFactory.acquireInFlightSlot`（per-provider Semaphore，boundedElastic 排队，UNBOUNDED 租约覆盖无 provider 的 fallback 路径）实现，在 `SpringAiAgenticRuntime` 的 stream/call 入口以 `usingWhen` 包裹——工具循环与 Spring AI 两条路径都被闸住。模块测试 130/130。

**严重度**: 高 —— 影响生产可用性
**类型**: 传输层韧性配置不足 + 客户端并发无治理

### 位置

- `dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/service/runtime/OpenAiCompatibleAgenticRuntime.java`
- `dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/config/ChatClientFactory.java`（两个 builder：`buildOpenAiClient` L245、`buildAnthropicClient` L261 —— v1 只提了 OpenAI 分支，**Anthropic 分支同样未配置且必须一并修改**）

### 现象

模型 API 返回 `429 Too Many Requests` 时请求失败，SSE 流以 `finishReason=error` 终止。"并发 >4 大量失败""15 分钟 0 成功 / 397 失败"为 v1 作者的自述【运行时观察·未复现，本修订未验证具体数字】。失败经由 `sink::error` 走错误路径属实的代码依据：`OpenAiCompatibleAgenticRuntime.java:144`。

### 根因分析（v2 更正）

v1 称"未配置重试策略，默认行为不重试 429"——**与事实相反**。实证：

1. 项目使用 `com.openai:openai-java-client-okhttp:4.39.1`（`dc3-common-agentic/pom.xml:63`）。openai-java 官方文档明确：**默认 `maxRetries=2`**，自动对连接错误、408、409、**429**、5xx 做指数退避重试。`ChatClientFactory` 的 builder 未显式配置，即保持默认 2 次。v1 所称"日志栈帧可见 RetryingHttpClient"恰恰说明 SDK 内建重试在工作。
2. 真实缺口有三个，且都不是"没有重试"：
   - **深度不足**：持续限流窗口内（如按分钟配额的 provider），默认 2 次短退避大概率仍在窗口内撞墙；
   - **流式路径覆盖**：SDK 重试覆盖请求建立阶段；SSE 流建立后的中途异常不会重试（流不可能安全重放）【此点需按下方"先观测"步骤实证后再定性】;
   - **并发无治理**：调用侧对同一 provider 无并发上限，N 路并发直接打满配额，再多重试也只是排队撞墙。

第一性原则：**与其失败后重试，不如从源头少发**。重试是缓冲，并发节流才是治理。

### 修复方案

**第 0 步（先观测，禁止跳过）**：用 MockWebServer（okhttp 随 SDK 传递可用）写集成测试：持续返回 429（带/不带 `Retry-After`），断言实际重试次数与间隔，实证当前行为基线。没有这一步，后续任何配置都是盲调。

**第 1 步（治本）：provider 级并发节流**。在 agentic 调用链（runtime 或 service 层）为每个 provider 加信号量/队列限并发（建议初值 2-4，走 `AgenticProperties` 可配），超出者排队而非直接打向 provider。

**第 2 步（缓冲）：显式配置重试深度**，两个分支同步修改：

```java
// buildOpenAiClient / buildAnthropicClient 中（openai-java 与 anthropic-java 的
// builder 均提供 .maxRetries(int)，默认 2）：
OpenAIOkHttpClient.builder()
    .baseUrl(provider.getBaseUrl())
    .apiKey(provider.getApiKey())
    .maxRetries(4)
    .build();
```

不建议采用 v1 的"方案 B 自造 RetryInterceptor"：`RetryInterceptor` 类并不存在，等于在 SDK 已有重试之上再叠一层未定义行为的手写重试。

### 验收标准

1. 第 0 步的 MockWebServer 测试可复现当前重试行为（次数、间隔、是否尊重 Retry-After），并留作回归
2. 配置 `maxRetries(4)` 后，同测试显示 4 次退避重试；4 次仍失败才以 `finishReason=error` 终止且错误链完整
3. 并发 8 路压测时，实际打到 provider 的在途请求不超过节流上限；用户侧排队不报错
4. OpenAI 与 Anthropic 两个 builder 行为一致

---

## P0-2: 消息合并按索引配对可能错位

**状态**: ✅ 已修复（负向验证闭环）。在既有 `tests/unit/agentic-store.test.ts` 追加 5 场景 describe：单轮、连发多轮、多端插入、删除中间、id 命中历史——前两场景旧实现即红，**多端插入与删除中间两场景在旧索引实现下红灯实锤错位**；替换为混合配对后 19/19 全绿。函数已导出以获得直接覆盖。

**严重度**: 高 —— 可能导致数据错乱
**类型**: 数据配对策略脆弱 + **零测试覆盖**

### 位置

`dc3-web/src/store/modules/agentic.ts` L1138-L1165（`mergeEphemeralAssistantState`，模块私有函数，当前未导出、无任何测试引用）

### 现象

该函数用**数组索引**将本地乐观消息（流式期间的 reasoning/charts/finishReason）与服务器回读消息配对。若服务器新增或删除了任何一条 assistant 消息（如另一客户端同时操作），索引错位导致 reasoning/charts 张冠李戴。触发时机实证：`sendMessage` 的 `finally`（L586-L595）**不做** post-stream reload，乐观消息滞留本地与 localStorage，直到下次 `selectSession`/刷新后的 `loadMessages`（L631）才合并——因此"乐观 id vs 服务器 id"是**主要**场景，不是边缘场景。

### 根因分析

```typescript
// 当前实现（agentic.ts:1138-1165）
let assistantIndex = 0;
return loaded.map((message) => {
  ...
  const state = previousAssistantState[assistantIndex++]; // ← 按出现顺序配对
});
```

按索引配对隐含"本地第 N 条 assistant = 服务器第 N 条 assistant"假设，多端并发时不成立。

**为什么不能照 v1 方案改成纯 ID 配对**：本地乐观消息 id 由前端生成（L507 `createMessageId('assistant')`），且从未包含在请求体中（L523-L532 只有 model/messages/conversationId/temperature/maxTokens/attachments/reasoning）；服务器回读 id 来自后端（L622）。两者必不相同，`previousById.get(message.id)` 在主场景永远 miss，合并会整体失效——比现状更糟。而该函数**零测试覆盖**（全 tests/ 目录无引用），改坏了不会有测试报警。

### 修复方案

**第 1 步：先补测试再改实现**。导出该函数并新建 `tests/unit/agentic-store-merge.test.ts`，至少覆盖：单轮合并、连发多轮后一次 reload、多端插入一条、删除中间一条。

**第 2 步：混合配对（id 配对 + 尾对齐）**。历史消息 id 已知相同（上次合并后写回 localStorage 的就是服务器 id），用 id 精确配对；仅 previous 尾部那些 id 不在 loaded 集合中的乐观占位，与 loaded 尾部未配对的 assistant **从尾向头**一一对应（本轮消息总在尾部，多端插入多发生在中间）：

```typescript
export const mergeEphemeralAssistantState = (previous: AgenticMessage[], loaded: AgenticMessage[]) => {
  const stateOf = (message: AgenticMessage) => ({
    reasoning: message.reasoning,
    finishReason: message.finishReason,
    charts: message.contentExt?.charts,
  });
  const apply = (message: AgenticMessage, state?: ReturnType<typeof stateOf>) => {
    if (!state) return message;
    return {
      ...message,
      reasoning: message.reasoning || state.reasoning,
      finishReason: message.finishReason || state.finishReason,
      contentExt:
        message.contentExt?.charts?.length || !state.charts?.length
          ? message.contentExt
          : {...(message.contentExt || {}), charts: state.charts},
    };
  };

  const loadedIds = new Set(loaded.map((message) => message.id));
  const previousById = new Map(previous.map((message) => [message.id, stateOf(message)]));

  // 尾对齐：previous 尾部 id 不存在于 loaded 的 assistant 是前端乐观占位，
  // 与 loaded 尾部尚未消耗的 assistant 从尾向头配对（跨过中间的 user 消息，
  // 支持连发多轮后一次 reload 的场景）。
  const tailPairs = new Map<string, ReturnType<typeof stateOf>>();
  let l = loaded.length - 1;
  for (let p = previous.length - 1; p >= 0; p--) {
    const prev = previous[p]!;
    if (prev.role === 'user') continue;
    if (prev.role !== 'assistant' || loadedIds.has(prev.id)) break; // 进入历史区，停止
    while (l >= 0 && loaded[l]!.role !== 'assistant') l--;
    if (l < 0) break;
    tailPairs.set(loaded[l]!.id, stateOf(prev));
    l--;
  }

  return loaded.map((message) =>
    message.role === 'assistant'
      ? apply(message, tailPairs.get(message.id) ?? previousById.get(message.id))
      : message
  );
};
```

残余风险（如实声明）：若另一端在本轮**之后**又产生了更新的 assistant 消息，尾对齐仍可能错一位。彻底根治需协议层配合（SSE finish 帧回传服务器 messageIndex，或后端持久化 reasoning/charts 后整体删除此合并函数，回到单一事实源），列为长期项。

### 验收标准

1. 新测试全部通过，覆盖上述四个场景
2. 手动流式对话一轮后切换会话再切回，reasoning/charts/finishReason 正确落在对应消息上
3. `pnpm --dir dc3-web test` 无新增失败

---

## P0-3: 记忆开关代码默认与部署默认分层未对齐

**状态**: ✅ 已修复（2026-10-03 产品决策：统一为 **true**）。部署侧 10 处全部改 true；`AGENTIC_TOOL_CALLING_ENABLED` 实施时核查发现**此前已被统一为 true**（全部部署文件 + .env.example 均为 true），无需改动——v2 所述"完全相同的分层"中 tool-calling 一侧已先行对齐。`check_compose_vars.py` 通过（9 文件 89 变量）。

**严重度**: 中（v1 为高）—— 这是一个**决策项**，不是机械 bug
**类型**: 多事实源冲突

### 位置（v2 更正：不是三处，是 12 处）

代码侧默认 `true`：

| 位置 | 默认值 |
|---|---|
| `AgenticProperties.java` L56 | `true` |
| `application-agentic.yml` L37 | `${AGENTIC_MEMORY_ENABLED:true}` |

部署侧默认 `false`（**10 处**，v1 只列了 1 处）：

| 位置 |
|---|
| `dc3/docker-compose.yml:227` |
| `dc3/docker-compose-swarm.yml:394` |
| `dc3/docker-compose-scale.yml:291` |
| `dc3/docker-compose-native.yml:243` |
| `dc3/docker-compose-dev.yml:227` |
| `dc3/deploy/helm/dc3/values.yaml:87` |
| `dc3/deploy/k8s/configmap.yaml:67` |
| `.env.example:103` |
| `dc3/env/dev.env:70` |
| `dc3/env/dev.env.sh:87` |

### 现象

Docker/Helm/K8s 部署默认关闭记忆，多轮对话失忆；裸跑（不设环境变量）则默认开启。同一功能两种部署形态行为不同。

### 根因分析（v2 更正）

v1 猜测"早期为调试改为 false 忘记恢复"——**与 git 证据相反**：`d65fc715f`（2026-05-11）首次引入该变量时即为 `false`，commit message 明写 "with defaults in dev.env"；同一 commit 里 `AGENTIC_TOOL_CALLING_ENABLED` 也是 `false`，且与 `AgenticProperties.java:69`（`toolCallingEnabled = true`）构成**完全相同的分层**。结论：这是"代码默认开启、部署默认关闭"的**系统性保守 rollout 分层**，是有意设计。

真正的债是：这个分层从未被显式决策确认——"记忆默认开还是关"的产品语义悬而未决，两层各自为真。违反 AGENTS.md L30 "Executable configuration is the only source of truth" 的精神：不是缺事实源，而是**事实源之间没有裁决记录**。

### 修复方案

**先决策，后统一，禁止只改一处**：

1. 产品决策：记忆（及同类分层开关 tool-calling）默认开还是关？
2. 若定 **true**：上述部署侧 10 处全部 `${AGENTIC_MEMORY_ENABLED:-true}` / `"true"`（helm/k8s 是字面量，直接改）；`.env.example`、`dev.env`、`dev.env.sh` 同步改
3. 若定 **false**：反向统一——`AgenticProperties.java:56` 改 `false`、`application-agentic.yml:37` 改 `${AGENTIC_MEMORY_ENABLED:false}`，部署侧不动
4. 决策结论写进 commit message，作为分层意图的裁决记录

### 验收标准

1. `grep -rn "AGENTIC_MEMORY_ENABLED" .`（仓库根执行）只剩一种默认值语义；helm/k8s 字面量同步
2. `make validate-compose-vars` 通过
3. 按所选方向部署后验证：true 方向第 2 轮对话引用第 1 轮内容；false 方向每轮无历史回放

---

## P1-4: xs 断点冗余 !important 死代码

**状态**: ✅ 已修复。删除 `width: 100% !important;` 一行，留注释说明三路宽度恒等、无需覆盖。

**严重度**: 低（v1 为中；现象降级为死代码，但治理成本也降为删一行）
**类型**: 冗余样式

### 位置

`dc3-web/src/components/agentic/AgenticAssistant.vue` L1524

```scss
@media (max-width: $breakpoint-xs-max) {   // tokens.scss:40 → max-width: 767.98px
  .agentic-panel {
    ...
    width: 100% !important;                // ← 本项目标
```

### 现象（v2 更正：v1 描述的"JS 340px vs CSS 100% 优先级战争"不成立）

实证三路宽度来源在 xs 断点下**恒等**：

1. `panelStyle` 在 `isMobile` 时返回 `{width: '100%'}`（`AgenticAssistant.vue:423-425`）；而 `isMobile` = xs 层（`useBreakpoint.ts:84`，xs <768px）与 `$breakpoint-xs-max: 767.98px` **完全同域**——非 expanded 状态下内联宽度就是 100%
2. expanded 状态无内联样式（L27 `:style="expanded ? undefined : panelStyle"`），由 `.agentic-panel--expanded { width: 100% }`（L930-L933）接管，同为 100%
3. `!important` 覆盖的与被覆盖的值恒等 → **纯冗余死代码**，不存在控制权竞争

旁证：sm 层做过同类治理——L409-L412 注释明言 tablet 上限 "lives here instead of a CSS !important rule"。xs 的这条是治理遗漏的尾巴。

### 修复方案（v2 简化：v1 的 CSS 变量重构属过度设计）

删除 L1524 的 `width: 100% !important;` 一行即可（`position/inset/z-index` 等其余声明保留）。项目为纯 CSR，JS 执行前组件不渲染，无竞争窗口；删除后三个来源仍恒等 100%。

### 验收标准

1. 375px 视口下：面板全宽、resizer 隐藏（L1530 `display: none`）、expanded 工作台模式全宽，均正常
2. `grep -c '!important' AgenticAssistant.vue` 减 1（注：L1564 `animation: none !important` 属 prefers-reduced-motion，语义正确，不在本项范围）
3. `pnpm --dir dc3-web test tests/component` 通过

---

## P1-5: 全量消息缓存 localStorage 的容量风险

**状态**: ✅ 第 1 步已实施：`persistMessages` 只写活跃会话（含注释说明容量与静默溢出依据）。第 2 步（IndexedDB / 去缓存）留作长期项，未实施。

**严重度**: 中 —— 容量瓶颈（v1 的性能论证不成立，已删除）
**类型**: 缓存策略不当

### 位置

`dc3-web/src/store/modules/agentic.ts` L978-L980（`persistMessages`）

### 现象（v2 更正）

v1 称"每次流式 delta 都触发 persistMessages 全量写"——**不成立**。实证：`appendAssistantDelta` / `appendAssistantReasoning` / `appendAssistantVisualization`（L909-L940）直接赋值响应式数组，**不经过** `setConversationMessages`/`persistMessages`，流式期间 localStorage **零写入**。真实触发点每轮约 2-3 次：发送占位（L514→L906）、流结束 `markAssistantComplete`（L952→L906）、下次进入会话的 `loadMessages`（L632），另有 `selectSession` 空初始化（L324）与 `deleteSession`（L394）。

仍然成立的问题：

- **容量**：每次写入都是**所有会话的全部消息**全量 JSON 序列化（L979），localStorage 上限 5-10MB，长会话必溢出；且 `setStorage`（`storageUtil.ts:60-73`）的 try-catch 吞掉 `QuotaExceededError`——**静默失败**，缓存从此停在旧版本
- **必要性存疑**：刷新后 `loadMessages` 本来就会从服务器拉取，全量缓存的首屏价值有限

另更正 v1 的一处关联叙述：`mergeEphemeralAssistantState`（P0-2）协调的是"本地乐观状态 vs 服务器快照"，与 localStorage 缓存无直接关系，不是"为协调双写而存在"。

### 修复方案（v2 重排优先级；v1 的 debounce 止血步骤取消——热点不存在）

**第 1 步：只持久化活跃会话**：

```typescript
const persistMessages = () => {
  const activeId = activeConversationId.value;
  if (!activeId) return;
  setStorage(MESSAGE_STORAGE_KEY, {[activeId]: messagesByConversation.value[activeId] || []});
};
```

非活跃会话切换进入时由 `loadMessages` 拉取（selectSession 本就如此），行为不变、容量上界降为单会话。

**第 2 步（长期可选）**：迁移 IndexedDB，或评估直接去掉本地缓存（服务器为单一事实源）。

**独立小项**：`setStorage` 静默吞配额异常值得单独治理（至少 console.warn），但属工具层，不阻塞本项。

### 验收标准

1. 多会话长对话后 localStorage 中只剩活跃会话数据，总量可控
2. 刷新后活跃会话消息即时恢复；切换其他会话正常加载
3. 构造超大数据集时不再触发静默配额失败（或至少有告警日志）

---

## P1-6: overflow-wrap anywhere 作为默认策略

**状态**: ✅ 已修复。容器与 blockquote 改 `break-word`；`code` / `pre` 显式保留 `anywhere`。

**严重度**: 中 —— 排版质量下降
**类型**: 策略过激

### 位置

`dc3-web/src/components/agentic/AgenticAssistant.vue` L1391（`.agentic-markdown` 容器）、L1413（`blockquote`）

```scss
:deep(.agentic-markdown) {
  overflow-wrap: anywhere;   // ← 允许在任意字符处断行
```

### 现象

`overflow-wrap: anywhere` 使英文文本可在单词中间任意断行，排版不美观。`break-word` 只在单词整体超出容器时才断行，质量更好。

**代码库旁证**（本项与现状自洽，改动风险低）：表格单元格已用 `overflow-wrap: break-word`（L1490）；`pre` 已有 `overflow: auto` 横向滚动（L1457-L1461）——容器级 anywhere 的"防溢出"职责在这些场景已有更精确的承担者。

### 修复方案

```scss
:deep(.agentic-markdown) {
  overflow-wrap: break-word;

  // 长串无空格内容（代码、长 URL/哈希）才允许任意断行
  code, pre {
    overflow-wrap: anywhere;
  }
}
```

blockquote（L1413）同步改 `break-word`。

### 验收标准

1. 含长 URL / 长哈希的消息不溢出、不撑破卡片
2. 英文段落不在单词中间随意断行；中文排版不受影响
3. 375px 视口与桌面视口各抽查一条含代码块的消息

---

## P2-7: mock 路由优先级隐式于数组顺序

**状态**: ✅ 已修复。`Scenario` 增加 `priority`（位号 10 / 温度 20 / 能耗 30 / 驱动 40，default 999），`pickScenario` 按 priority 排序匹配；`scenarios` / `pickScenario` / `defaultScenario` 导出，新增 `tests/unit/mock-scenario.test.ts` 5 项直接单测（含"温度位号"路由到位号场景）。v2 曾断言"mock-adapter 测试不存在"系工具误报（见附录 D 勘误），该文件实际存在且作为回归保障通过。

**严重度**: 低 —— 仅影响 demo 可维护性
**类型**: 声明性不足

### 位置

`dc3-web/src/mock/fetch.ts` L148-L215（`scenarios` 数组 + `pickScenario`）

### 现象

`pickScenario` 用 `scenarios.find(...)`（L214-L215）按数组顺序首个命中。"温度位号现在是多少"同时匹配 `/温度|temp|发热|过热/i`（L150，排第 1）与 `/位号|point|点位|实时/i`（L189，排第 4），实际命中温度场景——顺序即行为，但代码无任何机制声明或守护这一点，重排数组即静默改变 demo 行为。

### 修复方案

给场景加显式 `priority`，匹配前排序（更具体的查询意图给更小数字）：

```typescript
interface Scenario {
  priority: number;  // 越小越先匹配
  match: RegExp;
  ...
}

const scenarios: Scenario[] = [
  { priority: 10, match: /位号|point|点位|实时/i, ... },
  { priority: 20, match: /温度|temp|发热|过热/i, ... },
  { priority: 30, match: /能耗|用电|电量|energy|功率/i, ... },
  { priority: 40, match: /驱动|driver|掉线|负载/i, ... },
];

const pickScenario = (prompt: string): Scenario =>
  [...scenarios]
    .sort((a, b) => a.priority - b.priority)
    .find((s) => s.match.test(prompt)) ?? defaultScenario;
```

### 验收标准

1. `温度位号现在是多少` 路由到位号场景
2. 新建 `tests/unit/mock-scenario.test.ts` 直接覆盖 `pickScenario`（注意：**当前不存在**所谓"mock-adapter 测试"，pickScenario 仅经 `tests/agentic-dialogue` 语料链路间接覆盖——v1 引用的测试文件不存在）
3. `tests/agentic-dialogue` 语料测试不回归

---

## P2-8: 布尔值字符串手写解析

**状态**: ✅ 已修复，且**比 v2 方案 B 更保守**：未匹配字符串仍返回 `undefined` 而非 `false`（保留三值语义），因为调用方 `sessionExt.archived` 依赖"关闭"与"缺失"的区分（既有测试 `agentic-store.test.ts` 断言 `toBeUndefined()`）。收敛后为 8 个规范变体 + TODO 注释；两个调用方（reasoningEnabled / archived）经 grep 排查无被删除变体的依赖。

**严重度**: 低 —— 类型契约不严格
**类型**: 前后端协议模糊

### 位置

`dc3-web/src/store/modules/agentic.ts` L1176-L1194（`normalizeBoolean`）

### 现象

手写 14 种字符串变体（`'true','1','yes','y','on','enable','enabled'` + 对应 7 个否定式）。后端 session ext 经 JSON 往返可能把 boolean 变字符串，此函数是兼容层；但 14 个变体是穷举式防御，后端任何新表示形式都会漏。

### 修复方案

**方案 A（治本）**：后端 ext 序列化输出原生 JSON boolean，前端保留最小兼容层一个版本期后删除。

**方案 B（务实收敛）**：

```typescript
/**
 * Parse legacy string-encoded booleans from server session ext payloads.
 * TODO: remove once the backend emits native JSON booleans.
 */
const normalizeBoolean = (value: unknown): boolean | undefined => {
  if (value === undefined || value === null || value === '') return undefined;
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  const v = String(value).trim().toLowerCase();
  return v === 'true' || v === '1' || v === 'on' || v === 'yes';
};
```

**语义变化警告（v1 未声明）**：方案 B 相对现实现有两处变化——`'y'/'enable'/'enabled'` 不再识别为 true；未匹配字符串（含 `'disable'/'disabled'`）从返回 `false` 变为返回 `undefined`。合入前 grep `normalizeBoolean` 的全部调用方，确认无人依赖这些变体或区分 `false`/`undefined`。

### 验收标准

1. session 恢复（URL 参数、localStorage 缓存）功能正常
2. 调用方语义差异已排查（grep 确认）
3. `pnpm --dir dc3-web test` 无新增失败

---

## P2-9: 全局 body class 操作

**状态**: ✅ 已修复，含一处**实施偏差**：拖拽过渡暂停迁至 `documentElement` 的 `--dc3-agentic-body-transition`（`Layout.vue` 的 `.body-main` margin-right 过渡接变量，与 `--dc3-agentic-dock-width` 同通道），面板自身过渡改组件内 `is-dragging` 状态类；但 `body.agentic-resizing` class **保留**，仅承载 `cursor/user-select` 全局拖拽反馈——那是合法的全局交互状态而非越界（原生 resize 同理）。债的边界是"借全局 class 控制他人样式"，该部分已清零：`body.agentic-resizing .body-main` 与 `body.agentic-resizing .agentic-panel` 两条组合选择器均不存在了。验收标准 1 相应修正为 `grep -c 'body\.agentic-resizing \.'` 为 0（class 声明块本身保留）。

**严重度**: 低 —— 组件封装泄漏
**类型**: 样式作用域越界

### 位置

- JS：`AgenticAssistant.vue` L556 / L757 / L784（`document.body.classList.add/remove('agentic-resizing')`）
- 全局样式：L1578-L1588（非 scoped 块，`body.agentic-resizing .body-main { transition: none !important }`）
- scoped 内另有一条同类：L941-L944（`body.agentic-resizing .agentic-panel`）

### 现象

面板组件拖拽期间给 `document.body` 加全局 class，再在全局样式中用它禁用主布局 `.body-main` 的过渡。功能正确，但组件直接操控不属于自己的全局 DOM 与主布局样式。

### 修复方案（v2 更正：删除 v1 的两个错误备选）

v1 的 `:has()` 方案不适用：实证 `.body-main`（`Layout.vue:147`）不是面板的祖先——`AgenticAssistant.vue:830` 从 `panelRef.parentElement` 向下 `querySelector('.body-main')` 能命中，说明二者是兄弟关系，`.body-main:has(.agentic-panel)` 永不匹配。v1 备选"@container/@supports 查询自定义属性"在 CSS 规范上不可能，直接删除。

**主方案：documentElement 自定义属性**（仓库已有同模式先例：L444 的 `--dc3-agentic-dock-width`）：

```typescript
// 拖拽开始
document.documentElement.style.setProperty('--dc3-agentic-body-transition', '0s');
// 拖拽结束 / onBeforeUnmount
document.documentElement.style.removeProperty('--dc3-agentic-body-transition');
```

```scss
// Layout 全局样式（只读变量，不操作 body class）
.body-main {
  transition: padding-right var(--dc3-agentic-body-transition, 0.2s) ease;
}
```

scoped 内那条（L941-L944）改为组件自身状态类，不再借道 body：

```typescript
const isDragging = ref(false); // handleResizeStart/End 维护
```

```scss
.agentic-panel.is-dragging {
  transition: none;
}
```

### 验收标准

1. `grep -c 'body\.agentic' AgenticAssistant.vue` 为 0（两处都清掉）
2. 拖拽期间宽度 1:1 跟手、主布局无过渡延迟；松开后过渡恢复
3. `pnpm --dir dc3-web test tests/component` 通过

---

## P2-10: 测试 mock DB 跨测试累积

**状态**: ✅ 已修复。`chatHarness` 导出 `resetMockChatEngine`（清空 agentic 集合，注释说明隔离边界）；新增 `tests/agentic-dialogue/mock-engine-reset.test.ts`——固定 conversationId 连跑两轮 + 中途 reset，断言第二轮不读到第一轮数据（相对计数断言，不依赖 persistMockTurn 的具体条数）。

**严重度**: 低 —— 仅影响测试隔离性（v2 注：影响被既有设计大幅缓解）
**类型**: 隐式共享状态

### 位置

`dc3-web/tests/agentic-dialogue/helpers/chatHarness.ts`（L23 `import {db} from '@/mock/db'`、L45-L57 `ensureMockChatEngine` 模块级只装一次）；`dc3-web/src/mock/db.ts` L122（`export const db` 单例）；`dc3-web/src/mock/fetch.ts` L312（`persistMockTurn` 持续向 `db.agenticMessages` 追加）

### 现象（v2 补充既有缓解设计）

mock DB 是模块级单例，`persistMockTurn` 的数据确实跨测试累积（内存增长属实）。**但** v1 未提及：harness 作者已用唯一 conversationId 隔离了数据串扰——`chatHarness.ts:70-73` 每个用例轮次使用 `dialogue-${item.id}-${++conversationSeq}`，注释明示 "so mock persistence cannot bleed across cases"。当前残余风险仅两项：①进程内内存单调增长（无实质危害）；②**未来**新增测试若复用同一 conversationId，会读到前人累积的数据而踩雷。

### 修复方案

在 harness 导出重置函数，供未来需要固定 conversationId 的测试在 `beforeEach` 调用（当前不必急改，属预防性）：

```typescript
import {db} from '@/mock/db';

/** Reset agentic collections so tests reusing a fixed conversationId start clean. */
export const resetMockChatEngine = (): void => {
  db.agenticMessages.length = 0;
  db.agenticSessions.length = 0;
};
```

（若选择在 `@/mock/db` 增加 `initMockDb()` 重置全部集合并由 seed 重建，注意它会波及其他测试文件共享的同一 db 实例，需评估影响面后再做。）

### 验收标准

1. 新增测试"同一文件内连续 2 个测试使用相同 conversationId 互不影响"通过
2. 现有测试无回归（基线见附录 B：**7,142 项，7137 通过 / 1 项与本清单无关的既有失败 / 4 跳过**——v1 的"6,364 项全部通过"数字有误）

---

## 附录 A：修复优先级建议（v2.1 已全部执行完毕）

原 v2 排序（第 1 批 CSS → 第 2 批 P0-3 决策 → 第 3 批 P0-2 → 第 4 批 P0-1 → 第 5 批杂项）已按序执行完成，其中 P0-3 经产品决策定为统一 `true`。

## 附录 B：验证命令与基线

```bash
# 前端全量
pnpm --dir dc3-web check
pnpm --dir dc3-web lint:check
pnpm --dir dc3-web test

# 后端编译 + 模块测试
mvn -s .mvn/settings.xml -q -DskipTests compile -pl dc3-common/dc3-common-agentic
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-agentic

# Compose / 文档校验
make validate-compose-vars
python3 dc3/bin/check_compose_vars.py
python3 dc3/bin/check_documentation.py
```

**终态基线（2026-10-03 v2.1 实施后，全部实跑）**：

| 套件 | 结果 |
|---|---|
| 前端 `pnpm test` | **97 文件 / 7,153 项：7,149 通过 + 4 跳过，0 失败**（v2 基线中的既有 guardrails 失败已随 scenario-corpus 断言收紧一并根治） |
| 前端 `pnpm check` / `lint:check` | 通过（导出函数补齐 JSDoc） |
| 后端 `mvn test -pl dc3-common-agentic` | **130 项全绿**（含新增 `TransportRetryBehaviorTest` 3 项；两个 runtime 测试适配新构造器签名） |
| `check_compose_vars.py` | 通过（9 文件 89 变量；`AGENTIC_MEMORY_ENABLED` 全栈默认 true） |
| `check_documentation.py` | 通过 |

## 附录 C：本修订的核查方法与证据边界

- **已实证**（源码级）：全部"位置/行号"；P0-1 的 SDK 版本与默认重试（openai-java 官方文档）；P0-2 的 id 生成与请求体字段；P0-3 的 12 处默认值与 `git show d65fc715f`；P1-4 的三路宽度恒等；P1-5 的触发点与 `setStorage` 吞异常；P2-9 的 DOM 兄弟关系；P2-10 的单例与唯一 conversationId 设计；测试总数与既有失败（全量实跑）
- **未复现**（运行时观察，保留待验）：P0-1 的"并发 >4 大量失败""15 分钟 0 成功 / 397 失败"等具体数字；P0-3 的"助手声称没有上下文"用户体验描述
- **无法静态验证**：SSE 流建立后中途异常不重试的推断（已转为 P0-1 第 0 步的待观测项——v2.1 的 MockWebServer 测试覆盖了请求建立阶段的重试契约，流中途失败不在其范围）

## 附录 D：v2.1 实施记录与勘误

**实施中发现的新事实（v2 核查未覆盖）**：

1. `OpenAiCompatibleAgenticRuntime.createClient`（原 L585）自建临时 client、不经 `ChatClientFactory` 缓存——429 修复必须覆盖的第三处 builder，v2 的"两处 factory 分支"不完整
2. `AGENTIC_TOOL_CALLING_ENABLED` 在全部部署文件与 `.env.example` 中已是 `true`——tool-calling 分层此前已被对齐，v2"完全相同的分层"描述已过时一半
3. `.body-main` 的过渡属性是 `margin-right`（`Layout.vue:932`），v2 修复示例中的 `padding-right` 有误

**v2 自我勘误（工具误报，如实记录）**：

v2 更正表中"`tests/unit/agentic-store.test.ts` 不存在""现有 mock-adapter 测试不存在"两条是**误报**——两文件实际存在且内容充实（15 项 store 测试 / 端到端 mock 引擎测试），误报源于检索工具的花括号通配模式失效。仍然成立的是：`mergeEphemeralAssistantState` 纯函数此前**无任何直接覆盖**（两文件均未引用它），P0-2 的债与修复不受误报影响。

**实施偏差（与 v2 方案的差异，均已就地注明理由）**：

- P2-8：未匹配字符串返回 `undefined` 而非 v2 示例的 `false`（`archived` 调用方依赖三值语义）
- P2-9：`body.agentic-resizing` class 保留用于 cursor/user-select 反馈（合法全局交互状态），仅清除其对布局样式的越界控制；验收标准相应从"grep 为 0"修正为"不再有 `body.agentic-resizing .` 组合选择器"
- P0-2：新测试落在既有 `agentic-store.test.ts` 的独立 describe 中（而非 v2 建议的新文件），贴合仓库组织

## 附录 C：本修订的核查方法与证据边界

- **已实证**（源码级）：全部"位置/行号"；P0-1 的 SDK 版本与默认重试（openai-java 官方文档）；P0-2 的 id 生成与请求体字段；P0-3 的 12 处默认值与 `git show d65fc715f`；P1-4 的三路宽度恒等；P1-5 的触发点与 `setStorage` 吞异常；P2-9 的 DOM 兄弟关系；P2-10 的单例与唯一 conversationId 设计；测试总数与既有失败（全量实跑）
- **未复现**（运行时观察，保留待验）：P0-1 的"并发 >4 大量失败""15 分钟 0 成功 / 397 失败"等具体数字；P0-3 的"助手声称没有上下文"用户体验描述
- **无法静态验证**：SSE 流建立后中途异常不重试的推断（已转为 P0-1 第 0 步的待观测项）
