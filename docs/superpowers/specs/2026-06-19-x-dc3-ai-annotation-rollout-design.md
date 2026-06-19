# SP2：x-dc3-ai 注解全量铺开（设计）

- 日期：2026-06-19
- 范围：本 spec 覆盖 **SP2** —— 把 SP1 定义的 x-dc3-ai 标准真正落到全部 ~334 个端点，并收掉 SP1 的三个遗留尾巴。SP1（标准 +
  解析管道 + 校验器）已完成。
- 状态：待评审
- 前序：[2026-06-18-x-dc3-ai-standard-and-pipeline-design.md](2026-06-18-x-dc3-ai-standard-and-pipeline-design.md)（SP1）

## 1. 背景与目标

SP1 让「**注解做好 → 导出 JSON 质量高 → auth 解析出的工具质量高**」这条链路成立，把 AI 元数据真源切到导出的 OpenAPI JSON（路径
B），拆掉历史路径 A，并新增了构建期校验器 `ApiAnnotationValidator`。但 SP1 只补了 **1 个金丝雀**（`DriverController.list`
）做示范。

**关键事实（SP2 起点，来自代码溯源）**：

- 全仓 **334 个端点已 100% 带 `@Operation`**，summary/description 多数已写好且质量不低（2–4 句）。所以 SP2 的真实缺口*
  *不是从零写描述**，而是：①给每个端点补 `@Extension(x-dc3-ai)` 分类标志；②给请求体字段补 `@Schema(description)`。
- 请求体类型实测分布：**VO 80、Query 6、DTO 3**。本项目 Web 层用 **VO** 承载请求/响应（DO/BO/VO 三层建模），`*DTO`
  仅用于跨切面/传输载荷（MCP/OAuth）。SP1 设计文档 §5 笼统写的「请求 DTO」在本仓实指 **VO/Query（+少量 DTO）**。
- 各服务体量：`manager 148 端点 / 23 控制器`、`auth 91 / 17`、`data 75 / 15`、`agentic 20 / 7`。

**目标**：用一套可复用 playbook，按服务分批把全部端点注解到 SP1 §4/§5 的「档位 3」标准，用扩展后的校验器做**棘轮式服务级
fail-gate** 防回退。

**非目标（SP2 不做）**：

- 不改 rubric/标准本身（SP1 §4/§5 冻结）。
- 不改权限系统、角色/资源模型、`dc3_mcp_*` 连接/白名单机制。
- 不改已完成的解析管道（`ToolQuality`、`toolQualityByApiCode`、`applyQuality`、路径 A 删除均不动）。
- descriptions 已覆盖，只在明显薄弱处润色，不批量重写。

## 2. 真源与标准（沿用 SP1，冻结）

- AI 元数据真源 = 导出的 `openapi-*.json`（路径 B）。
- `x-dc3-ai` 字段集**不扩展**，固定 6 键：`riskLevel`、`destructive`、`idempotent`、`openWorld`、`hidden`、`description`。
- `readOnly` 由 HTTP 方法派生（GET=true），**不进** `x-dc3-ai`。
- 合法值：`riskLevel ∈ {HIGH, MEDIUM, LOW}`；`destructive/idempotent/openWorld/hidden ∈ {"true","false"}`（字符串字面量）。

## 3. rubric 速查（SP1 §4，供 worker 一致分类）

| 字段            | 取值              | 判定                                                                                                                                                                                    |
|---------------|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `riskLevel`   | HIGH/MEDIUM/LOW | **HIGH**=不可逆/触物理世界/批量销毁：下发命令、驱动重启、delete/remove、reset/purge/clear、撤销授权、令牌·密码写。**MEDIUM**=可恢复状态变更：add/update 设备·驱动·点位·模板、enable/disable、bind/unbind。**LOW**=只读：get/list/query/统计/历史/导出 |
| `destructive` | true/false      | true=delete/remove/reset/purge/clear/revoke + 改变物理状态的下发命令；其余 false                                                                                                                    |
| `idempotent`  | true/false      | true=GET/list/query、按 id 的 update(PUT)、按 id 的 delete；false=add（创建会重复）、下发命令                                                                                                            |
| `openWorld`   | true/false      | true=触达 DC3 之外：下发命令、驱动重启、发通知、对接外部；false=纯 DB CRUD                                                                                                                                     |
| `hidden`      | true/false      | true=不暴露给 AI 的内部/危险端点（令牌盐、内部同步、调试）；默认 false                                                                                                                                           |
| `description` | 文本（可空）          | 多数留空、直接走写好的 `@Operation.description`；仅需专门给 AI 措辞时填覆盖                                                                                                                                  |

**注解写法约定（档位 3，SP1 §5）**：summary 动词开头、工具名级短语；description 2–4 句（做什么 / 何时用·何时别用 / 关键副作用）；请求体字段
`@Schema(description)` 说明含义、单位、取值范围或枚举、是否必填。

## 4. 前置 SP2.0：收 SP1 尾巴（一次性，分批前先做）

棘轮门禁与注解传播都依赖这层，故独立成第一份 plan，分批 plan 之前完成。

- **P1 扩展校验器**：`ApiAnnotationValidator` 新增「请求参数描述覆盖」检查，对齐聚合器 `buildOperationSchema` 实际并入
  inputSchema 的两类来源：
  - **请求体字段**：反射解析 `@RequestBody` 参数类型 → 遍历其非静态、非 transient 声明字段 → 要求每个字段带 `@Schema` 且
    `description` 非空；缺失报 `apiCode: body field <name> missing @Schema(description)`
    。嵌套类型有界递归（深度上限，防循环），跳过原生/包装/JDK/框架类型。
  - **query/path 参数**：对 `@RequestParam`/`@PathVariable` 方法参数，要求带 `@Parameter` 且 `description` 非空；缺失报
    `apiCode: param <name> missing @Parameter(description)`。
  - 无任何请求参数的端点（如无参 GET）此项跳过。保留 SP1 的 operation 级检查。
- **P2 接入 make/CI 做棘轮 fail-gate**：把校验器做成可在 `make`/CI
  调用的检查，带「已完成服务白名单」（配置项，初始为空）。白名单内服务有任一缺陷 → **fail build**；名单外服务 → 仅输出缺失清单不
  fail。每完成一个服务批次，把该服务加入白名单。
- **P3 修 refresh 变更检测（Mi1）**：`refreshToolCatalog` 的变更检测当前只比对
  `schemaHash/permissionCode/apiPath/riskLevel/enableFlag/toolExt`，纯质量编辑（`destructive/idempotent/openWorld` hint 或
  description，且 riskLevel/toolExt 未变）不会触发重入库。改为对 catalog 的质量字段做内容哈希（或补齐缺失的比较字段），保证
  SP2 注解一定能被 refresh 重新入库。
- **P4 删死代码（M1）**：删除 `McpOpenApiAggregator.inputSchemasByApiCode()`（SP1 后仅其自身单测 `McpOpenApiAggregatorTest`
  在用）及对应测试；`toolQualityByApiCode()` 已覆盖其职责。

## 5. 每服务 playbook（按序对每个服务重复）

### 5.1 单服务「完成判定」（Definition of Done）

- a. 该服务每个端点的 `@Operation`：summary 非空、description ≥20 字、`x-dc3-ai` 合法 riskLevel + 四个布尔标志（按 §3
  rubric）。
- b. 该服务每个端点的请求参数都有描述：请求体类（VO/Query/DTO）字段有 `@Schema(description)`，`@RequestParam`/
  `@PathVariable` 参数有 `@Parameter(description)`（含义/单位/取值/必填，有意义处给）——二者皆进 inputSchema。
- c. 扩展后的校验器对该服务 **0 缺陷**（operation 级 + 字段级）。
- d. 把该服务加入校验器白名单（**棘轮**，自此 CI 强制、防回退）。
- e.（栈可用时）`make openapi` 重生成该服务快照 `openapi-<svc>.json` → `POST /mcp/tool/catalog/refresh` → 抽查
  `tools/list`：annotations 正确、参数描述出现在 inputSchema、差异化可见性不受影响。

### 5.2 执行方式（多 agent 并行）

- 扇出粒度 = **一个控制器一个 worker**（manager 23 个 worker）。每个 worker：注解本控制器全部端点的 `x-dc3-ai`，补本控制器引用的请求类字段
  `@Schema`，跑本切片校验器至 0 缺陷，提交。
- 协调者：跑服务级校验器门禁、做白名单棘轮、（栈可用时）触发导出 + refresh 抽查。
- worker 之间文件不重叠（各自控制器 + 其专属请求类）；共享请求类（若被多控制器引用）由协调者指派单一 owner，避免并行写冲突。

### 5.3 分批顺序

`manager(148/23) → data(75/15) → auth(91/17) → agentic(20/7)`。

理由：manager 最大且最典型 CRUD、金丝雀 `DriverController.list` 已是现成模板，先行可固化套路；agentic 最小，收尾。每个服务 =
一次 SP2 运行 = 一份独立实现 plan。

## 6. 接缝与不变量（必须守住）

- **api_code 自然键**不变（SP1 守护）：`dc3-center-<svc>:<METHOD>:/path`，JSON 侧与写库侧一致。
- **保守默认**仍兜底未注解端点：注解一个端点后，其 riskLevel 通常从 HIGH 默认变为声明值（list→LOW），触发 refresh 重入库；补
  `@Schema` 改变 inputSchema→toolExt 变，亦触发。P3 修复覆盖「无请求体 + riskLevel 恰为 HIGH + 仅改 hint」的窄边界。
- **租户安全 / 权限可见性**：SP2 只动注解与 `@Schema`，不碰任何查询/权限码/可见性 SQL，差异化可见性行为不变（回归验证）。

## 7. 测试策略

- **单测**：校验器新增的参数级检查（夹具：字段全标注的 VO + 全标注的 `@Parameter` 端点通过；缺 `@Schema` 的 VO 字段、缺
  `@Parameter` 的 query 参数各报对应缺陷）；P3 修复的回归测试（纯质量编辑触发 catalog 重入库）。
- **服务级门禁**：扩展校验器对目标服务返回 0 缺陷（即 5.1.c）。
- **回归**：SP1 全部既有管道测试保持绿；路径 A 删除后的权限差异化行为不变。
- **E2E（`dc3-stack-test`，栈可用时）**：补完一个服务 → 导出 → refresh → 按不同角色拉 MCP 工具，验证 annotations、inputSchema
  参数描述、hidden 端点消失、差异化可见性。

## 8. 数据迁移

- 无表结构变更。`dc3_mcp_tool_catalog` 由 refresh 重建，质量字段来源仍是 JSON（SP1 已切）。
- `openapi-*.json` 快照随每服务批次 `make openapi` 重生成（栈可用时；否则由维护者导出，见 §10）。

## 9. 分解为实现 plan

| Plan            | 内容                                             | 依赖    |
|-----------------|------------------------------------------------|-------|
| **SP2.0 前置**    | P1–P4（扩校验器 + 接 CI 棘轮 + 修 Mi1 + 删 M1）           | SP1   |
| **SP2-manager** | manager 148 端点全量注解 + VO/Query `@Schema` + 入白名单 | SP2.0 |
| **SP2-data**    | data 75 端点                                     | SP2.0 |
| **SP2-auth**    | auth 91 端点                                     | SP2.0 |
| **SP2-agentic** | agentic 20 端点                                  | SP2.0 |

各分批 plan 复用本设计的 rubric（§3）、DoD（§5.1）、执行方式（§5.2）。每份 plan 用 多 agent 协作方式执行。

## 10. 已知约束：实地导出依赖运行栈

`make openapi` 需中心服务在运行（`export_openapi.sh` 从 `/v3/api-docs/{svc}` 抓取）。当前 dev 中心服务容器在崩溃循环——JVM
GC 日志目录 `dc3/logs/center/<svc>/gc` 缺失，属预存镜像/入口问题，与应用代码无关；pg+rabbitmq 在 podman 上健康。SP2
的注解与校验器门禁**不依赖运行栈**（注解/反射式校验跑在单测里），故批次工作可推进；§5.1.e 的导出 + 全栈冒烟在栈可用时由维护者执行，不阻塞注解批次。

## 11. 已定决策记录

- 分解粒度：**单一可复用 playbook，按服务分批跑**（manager→data→auth→agentic），各服务一份 plan。
- 校验器门禁：**棘轮式服务级 fail-gate**（白名单递增，防已完成服务回退）。
- 参数描述范围：**每批含请求体 VO/Query/DTO 字段 `@Schema` + `@RequestParam`/`@PathVariable` 的 `@Parameter` 描述**（二者皆进
  inputSchema），校验器扩展到参数级。
- 校验器机制：**注解/反射式**（沿用 SP1，跑单测、不需启栈）。
- SP1 尾巴：**门禁（Mi3 接 CI）+ 正确性（Mi1 改变更检测）+ M1 删死代码**全部并进 SP2.0 前置。
- 执行扇出：**一控制器一 agent**，协调者跑门禁 + 白名单棘轮 + 共享请求类单 owner。
- 标准冻结：rubric（§3）与 6 键字段集不变；`readOnly` 派生不进 x-dc3-ai。

## 12. 后续（SP2 之后）

四个服务批次完成、校验器白名单覆盖全部服务后，棘轮 fail-gate 即全局生效，新增端点缺注解会在 CI 直接 fail。届时 SP2
收官；无后续子项目，除非引入新中心服务。
