# SP1：x-dc3-ai 标准 + 解析管道 + 校验器（设计）

- 日期：2026-06-18
- 范围：本 spec 只覆盖 **SP1**（标准 + 管道 + 校验器）。339 个接口的批量补注解是后续子项目 SP2…N，各自立 spec/playbook。
- 状态：待评审

## 1. 背景与目标

auth 服务把各中心服务的 OpenAPI 解析成 MCP 工具，再配合用户角色/权限提供差异化的 MCP 服务。当前 `@Operation` 注解缺少 AI
工具所需的元信息，导出的 OpenAPI JSON 质量低，解析出的工具质量也低。

目标：让「**注解做好 → 导出 JSON 质量高 → auth 解析出的工具质量高**」这条链路成立，并把 AI 元数据的真源**彻底切到导出的
OpenAPI JSON（路径 B）**，拆掉历史上「扫描活注解 → 灌 `dc3_api.api_ext` → SQL 启发式推导」的路径 A。

非目标（SP1 不做）：

- 不在本期批量补 339 个接口的注解（SP2+）。
- 不改动权限系统、角色/资源模型、`dc3_mcp_*` 连接与白名单机制。

## 2. 关键事实（来自代码溯源）

- **AI 元数据真源**：目标定为 OpenAPI JSON（路径 B）。历史路径 A（`ApiEndpointScanner.aiMetadata()` →
  `dc3_api.api_ext.content.riskLevel...` → `OAuthMcpMapper` 的 `CASE WHEN` 启发式）要拆除。
- **权限骨架不可删**：MCP 工具可见性过滤靠 `dc3_mcp_tool_catalog.permission_code = dc3_resource.resource_code`，而
  `dc3_resource`/`dc3_api` 由资源扫描器（`ResourceRegistrar` 在 `ApplicationReadyEvent` 时）注册，`dc3_resource` 还被 Web
  端角色管理使用。**扫描器注册 `dc3_api`/`dc3_resource` 的部分必须保留**，只删它的 AI 元数据抽取部分。
- **seed 无预置**：`dc3_api`/`dc3_resource`/`dc3_mcp_tool_catalog` 均运行时生成，seed SQL 无 API 行（仅菜单类资源）。
- **catalog 刷新入口**：`POST /mcp/tool/catalog/refresh`（`McpManagementController.refreshToolCatalog` →
  `OAuthMcpRuntimeServiceImpl.refreshToolCatalog`）。保留此入口。

## 3. 架构总览（路径 B）

```
①注解侧
  @Operation(summary/description) + @Extension("x-dc3-ai") + DTO @Schema(description)
    → make openapi（export_openapi.sh 抓 /v3/api-docs/{service}）
    → 高质量 openapi-*.json（classpath:openapi/openapi-<service>.json）

②消费侧（auth · refreshToolCatalog 时）
  事实源 dc3_api(JOIN dc3_resource)  → api_code + permission_code（集合 + 权限）
  质量源 openapi-*.json（增强 Aggregator 纯解析） → Map<api_code, ToolQuality>
        ToolQuality = { description, riskLevel, destructive, idempotent, openWorld, hidden, aiDescription, inputSchema }
  以 dc3_api 为驱动，按 api_code 自然键 left-join 质量 → 合并写 dc3_mcp_tool_catalog
  缺质量 → 保守默认 + 对账报告

③运行侧（不变）
  listVisibleTools / selectVisibleToolByName 三层过滤（连接白名单 → 风险等级 → 权限码）
```

**接缝原则**：`dc3_api`/`dc3_resource` 是「事实」（哪些接口存在、归谁有权限）；JSON 是「质量」（接口长什么样给 AI 看）。两者用自然键
`api_code` 在 refresh 时临时 JOIN，互不硬依赖。

## 4. x-dc3-ai 字段规范（rubric）

字段集**不扩展**，沿用现有 6 个。判定规则（按 IoT DC3 语义）：

| 字段            | 取值              | 判定规则                                                                                                                                                                                       |
|---------------|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `riskLevel`   | HIGH/MEDIUM/LOW | **HIGH**=不可逆 / 触达物理世界 / 批量销毁：下发命令、驱动重启、delete/remove、reset/purge/clear、撤销授权、令牌·密码写。**MEDIUM**=可恢复状态变更：add/update 设备·驱动·点位·模板、enable/disable、bind/unbind。**LOW**=只读：get/list/query/统计/历史/导出 |
| `destructive` | true/false      | true=delete/remove/reset/purge/clear/revoke + 改变物理状态的下发命令；其余 false                                                                                                                         |
| `idempotent`  | true/false      | true=GET/list/query、按 id 的 update(PUT)、按 id 的 delete；false=add（创建会重复）、下发命令                                                                                                                 |
| `openWorld`   | true/false      | true=触达 DC3 之外：下发命令、驱动重启、发通知、对接外部；false=纯 DB CRUD                                                                                                                                          |
| `hidden`      | true/false      | true=不暴露给 AI 的内部/危险端点（令牌盐、内部同步、调试）；默认 false                                                                                                                                                |
| `description` | 文本（可空）          | 多数留空、直接走写好的 `@Operation.description`；仅当需要专门给 AI 的措辞时才填覆盖                                                                                                                                   |

派生（不进 x-dc3-ai）：

- `readOnlyHint`：由 HTTP 方法派生，GET=true，其余=false。

合法值约束：`riskLevel ∈ {HIGH, MEDIUM, LOW}`；`destructive/idempotent/openWorld/hidden ∈ {true, false}`（字符串字面量）。

## 5. 注解写法约定（档位 3）

- **summary**：动词开头、工具名级短语。
- **description**：2–4 句，依次「做什么 / 何时用·何时别用 / 关键副作用或前置条件」。写给模型，不写营销词。
- **请求 DTO 字段 `@Schema(description=...)`**：每个字段说明含义、单位、取值范围或枚举、是否必填，必要时给示例。

（SP1 仅落「约定」本身 + 用 1 个样板接口示范；全量执行属 SP2+。）

## 6. 接缝设计：api_code 自然键（方案 A）

不建绑定表、不加绑定字段。两源共享内容派生的稳定键 `api_code = service:METHOD:/path`。

### 6.1 自然键现状（已对齐，只需守护）

`api_code` 形如 `dc3-center-<svc>:<METHOD_NAME>:<path>`（**用方法名 POST/GET/PUT/DELETE，不是数字**），写库侧与 JSON 侧*
*当前已经一致**：

- **写库侧**：`ResourceRegistrySyncServiceImpl.apiCodeOf()` =
  `serviceName + ":" + methodToTypeFlag(method).name() + ":" + path`。
- **JSON 侧**：`McpOpenApiAggregator` 用 `serviceNameOf()`（`openapi-<svc>.json` → `dc3-center-<svc>`）+ HTTP 方法名大写 +
  path 拼 key，与上式同形。

因此**不需要新建共享归一组件**。风险仅在于「未来某次改动让两侧悄悄漂移」，故只需补一个**跨侧一致性回归测试**（同一
service/method/path 输入，两侧产出的 api_code 必须相等）守住即可。

### 6.2 合并与对账（refresh 时）

- 以 `dc3_api`（JOIN `dc3_resource`）为驱动遍历，取 `api_code + permission_code`。
- 按 `api_code` 去 JSON 质量 Map 取 `ToolQuality`：
  - 命中 → 用 JSON 质量。
  - 未命中 → **保守默认**（见 §9）+ 记入对账报告「缺质量」。
- JSON 里有、`dc3_api` 里无的 → 不生成工具，记入对账报告「孤儿条目」。
- 对账报告作为 refresh 返回的一部分输出（也供校验器使用），是「JSON 是否过期、该不该重导」的信号灯。

## 7. auth 消费侧改造

### 7.1 增强 `McpOpenApiAggregator`

- 现状：`inputSchemasByApiCode()` 只解析 `paths` 的 `requestBody`/`parameters` 得 `inputSchema`，按 `api_code` 索引。
- 改为：解析每个 operation 的 `summary/description` + `x-dc3-ai` 扩展 + `inputSchema`，产出 `Map<api_code, ToolQuality>`。
- 保持**纯解析**：只吃 `openapi-*.json`，不查任何表（解耦）。

### 7.2 改 `refreshToolCatalog`

- 从 `listRegistryToolCandidates()` 取「身份 + 权限」列（见 §7.3），按 `api_code` 关联 Aggregator 的 `ToolQuality` 合并，写
  `dc3_mcp_tool_catalog`（
  `risk_level / read_only_hint / destructive_hint / idempotent_hint / open_world_hint / tool_ext(inputSchema) / description / hidden`
  等列由 JSON 质量 + 派生 readOnly 填充）。
- 缺质量字段用保守默认；产出对账报告。
- `toolToMcp()` 维持把 catalog 字段映射到 MCP 工具定义；如字段来源变化需相应对齐（如 description / aiDescription /
  hidden）。

### 7.3 删除路径 A 的 AI 通路

- `OAuthMcpMapper.xml` · `listRegistryToolCandidates`：删除读
  `api_ext.content.riskLevel/destructiveHint/idempotentHint/openWorldHint` 的 `COALESCE` 与 `CASE WHEN method=…`
  启发式；只保留身份+权限列（`api_code/tool_name/service_name/http_method/api_path/permission_code/enable_flag` 等）。
- `ApiEndpointScanner`：删除 `aiMetadata()` 抽取及其对 `FacadeScannedApiBO` AI 字段的赋值。
- `FacadeScannedApiBO`：删除 AI 字段（`riskLevel/destructiveHint/openWorldHint/idempotentHint/aiDescription/hidden`）。
- `ResourceRegistrySyncServiceImpl`：构造 `api_ext` 时删除 AI 字段写入，仅保留 `title/url/remark` 等基础信息。
- `ApiExt.Content`（entity/ext）：删除 AI 字段。

### 7.4 保留（不动）

- `ResourceRegistrar` 扫描 + 注册 `dc3_api`/`dc3_resource`（权限骨架）。
- `listVisibleTools`/`selectVisibleToolByName` 三层可见性过滤与相关 SQL。
- `dc3_mcp_connection`/`dc3_mcp_connection_tool`/`dc3_mcp_tool_catalog` 表与白名单机制。
- `POST /mcp/tool/catalog/refresh` 入口。

## 8. 第 0 步：springdoc 输出实测（前置）

- 给 1 个接口加 `@Extension(name="x-dc3-ai", ...)`，`make openapi` 导出后 grep `openapi-<svc>.json` 确认 `x-dc3-ai`
  字段确实出现在 operation 节点下。
- 若未输出，先在 `SpringDocConfig`（dc3-common-web）补 vendor extension 序列化配置/自定义器，再继续。
- 此步通过前不启动全量与 §7 改造的合并逻辑落地。

## 9. 错误处理与边界

- **保守默认**（某 api_code 在 JSON 无质量时）：`riskLevel=HIGH`、`destructive=true`、`idempotent=false`、`openWorld=true`、
  `hidden=false`、`readOnly=按方法派生`，并记对账报告。理由：宁可让未标注接口被当成高风险（受 `allowHighRisk`
  过滤更易被挡），也不误标为安全。
- **非法值**：`x-dc3-ai` 出现越界值（如 `riskLevel=foo`）→ 退回该字段保守默认 + 校验器报错项。
- **漂移**：缺质量 / 孤儿条目分别报告，不静默。

## 10. 构建期校验器

- 扫描所有 `@Operation`（或导出的 `openapi-*.json`），报告：
  1. 缺 `x-dc3-ai` 的接口；
  2. 缺失或过短 `description` 的接口；
  3. 缺 `@Schema(description)` 的请求 DTO 字段；
  4. `x-dc3-ai` 非法值。
- 默认**只报告不 fail build**（保守默认兜底），提供开关可切换为 fail（CI 收口用）。
- 形态：优先做成可在 `make`/CI 调用的检查（单测或独立校验任务），输出缺失清单。

## 11. 测试策略

- 单测：Aggregator 正确解析 `x-dc3-ai` + description + inputSchema；缺失走保守默认；非法值处理。
- 单测：`api_code` 归一函数双侧一致（scanner 侧与 JSON 侧同输入同输出）。
- 单测：校验器能抓出 4 类缺失。
- 集成：`refreshToolCatalog` 后 `dc3_mcp_tool_catalog` 的 `risk_level` 等来自 JSON fixture 而非 SQL 推导；对账报告正确列出缺质量/孤儿。
- 端到端（`dc3-stack-test`）：补 1 接口 → 导出 → refresh → 按不同角色拉 MCP 工具，验证 annotations 正确且差异化可见性不受影响。
- 回归：删除路径 A 后，权限差异化过滤行为不变。

## 12. 数据迁移

- 运行库 `dc3_api.api_ext` 残留 AI 字段：随 §7.3 后服务重启，scanner 重写 `api_ext`（不含 AI 字段）即自然清理；无需独立迁移脚本。
- `dc3_mcp_tool_catalog`：由 refresh 重建，字段来源切换为 JSON 质量。

## 13. 删除 / 保留清单（精确到组件）

**删除（路径 A 的 AI 通路，全链零残留 = 选项 1）**

6 个 AI 字段：`riskLevel/destructiveHint/openWorldHint/idempotentHint/aiDescription/hidden`。

- `OAuthMcpMapper.xml`：`listRegistryToolCandidates` 中 `api_ext` 读取 + `CASE WHEN` 启发式（读侧）
- `ApiEndpointScanner`：`aiMetadata()` + builder 6 行 AI 赋值 + `Extension`/`ExtensionProperty` import（填值侧）
- `FacadeScannedApiBO`（facade-api BO）：6 个 AI 字段
- `ResourceRegistryLocalFacade`：本进程映射的 6 个 AI 字段（约 line 64-68）
- `resource_registry.proto` · `GrpcScannedApiDTO`：字段 7–12，删后 `reserved 7,8,9,10,11,12;` + reserved 名占位；重新生成
  stub
- `ResourceRegistryServer`（gRPC 收端）：dto→BO 的 6 个 AI 字段映射（约 line 64-68）
- `ResourceRegistryScannedApi`（auth BO）：6 个 AI 字段
- `ResourceRegistrySyncServiceImpl`：`buildContent` 的 AI setter（line 654-659）+ `equalsContent` 的 AI 比较（line 676-681）
- `ApiExt.Content`：6 个 AI 字段（保留 title/url/remark）

> 注：`mcp_runtime.proto` 与 `McpRuntimeGrpcFacade`/`McpRuntimeServer` 里的 `risk_level` 等是**运行时工具定义**
> 通路（catalog→MCP），保留，改由 JSON 供数。

**保留**

- `ResourceRegistrar` 注册 `dc3_api`/`dc3_resource`
- 可见性三层过滤及其 SQL
- `dc3_mcp_*` 连接/白名单/目录表
- `POST /mcp/tool/catalog/refresh`

**新增 / 增强**

- `McpOpenApiAggregator`：解析 description + `x-dc3-ai` → `ToolQuality`
- `refreshToolCatalog`：按 `api_code` 合并 JSON 质量 + DB 权限 + 保守默认 + 对账
- `api_code` 共享归一组件
- 构建期校验器
- （视 §8 结果）`SpringDocConfig` extension 序列化配置

## 14. 后续子项目（SP2…N，非本期）

按服务（manager / data / auth / agentic）逐批补全 summary/description + `x-dc3-ai` + DTO `@Schema`，每批用 §10 校验器把关，可多
agent 并行。各自立 spec/playbook。

## 15. 已定决策记录

- 真源：路径 B（JSON）；拆路径 A 的 AI 通路。
- 接缝：方案 A（`api_code` 自然键，不建绑定表）。
- 注解彻底程度：档位 3（标志 + AI 描述 + 参数描述）。
- 缺标兜底：保守默认 + 构建期校验报告。
- `readOnly`：按 HTTP 方法派生，不进 `x-dc3-ai`。
- `x-dc3-ai` 字段集：不扩展，沿用现有 6 个。
