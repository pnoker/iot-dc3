# Gateway MCP 服务设计方案

> 状态: 已实现并落地。本文记录在 `dc3-gateway` 上构建 MCP (Model Context Protocol)
> 服务的完整设计，包括技术选型、工具治理、权限控制和生产安全策略。本文后半部分的设计推导（技术选型、推荐结论、实施步骤）保留作为决策记录；
> 实际代码实现以下方 [实现与本方案的差异（权威）](#实现与本方案的差异权威) 章节为准——当正文与该章节冲突时，以该章节为准。

## 实现与本方案的差异（权威）

落地实现达成了本方案的全部能力目标（统一 `/mcp` 端点、OAuth 2.1、RBAC∩白名单∩风险三重过滤、审计、生产 api-docs
收口），但实现路径与正文的部分命名/选型不同。以下为权威对照，按此查阅代码：

**模块与技术选型**

- 不存在独立的 `dc3-common-mcp` 模块。MCP 运行时逻辑分布在：`dc3-common-gateway`（`/mcp` 协议端点）、`dc3-common-auth`（OAuth
  授权服务器 + MCP 运行时服务 + 工具目录）、`dc3-common-model`（DTO）、`dc3-common-facade-*`（gateway↔auth 的 gRPC/local
  facade）。
- **未使用** Spring AI MCP Server Starter，`/mcp` JSON-RPC（initialize/ping/tools/list/tools/call）为手写实现。
- **未使用** Spring Authorization Server，OAuth 五端点 + JWKS + introspection + JWT(RS256) 签发/校验均为手写实现（WebFlux
  栈，Spring Authorization Server 当前为 servlet-only 故不适用）。

**类名 / 实体对照**（左为正文命名，右为实际实现）

| 正文                                                                                | 实际实现                                                                                                                 |
|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `dc3-common-mcp` 模块                                                               | `dc3-common-gateway` + `dc3-common-auth` + `dc3-common-model` + `dc3-common-facade-*`                                |
| `McpAuthWebFilter` / `McpAuthContext`                                             | 内联于 `McpGatewayController`（bearer 解析经 `mcpRuntimeFacade.introspect`，上下文为 `McpIntrospectResponseDTO`）                 |
| `McpToolAggregator`                                                               | `OAuthMcpRuntimeServiceImpl`（目录构建/刷新）+ `McpOpenApiAggregator`（可选 OpenAPI schema 富化）                                  |
| `McpToolCallHandler`                                                              | 内联于 `McpGatewayController.McpGatewayClient`（`callTool` / `invokeBackend`）                                            |
| `McpConnectionDO` / `McpToolCatalogDO` / `McpConnectionToolDO`（`*DO`+`*BO`+`*VO`） | `McpConnectionRecord` / `McpToolRecord` / `McpToolConfirmationRecord`（`entity/oauth`）+ `Mcp*DTO`（`dc3-common-model`） |
| `McpServerTools.vue`（前端）                                                          | `src/views/settings/mcp/McpServer.vue` + `src/views/settings/mcpAudit/McpAudit.vue`                                  |
| Identity 侧 `PrincipalContext`                                                     | `RequestHeader.PrincipalHeader`（承载 `principalId` 等转发身份）                                                              |

**本方案首版之外、已额外落地的安全增强**

- **高风险工具平台二次确认 + 幂等**：新增 `dc3_mcp_tool_confirmation` 表与 `AuthorizeToolCall` 流程。HIGH 风险工具
  `tools/call` 无有效 confirm 时返回 `CONFIRM_REQUIRED`(含 confirmId)，二次调用校验 confirm_id
  未过期、参数摘要一致、principal/connection/tool 一致并单次消费（消费即幂等）；`idempotency_key` 在 confirm 层去重。取代了早期仅校验
  confirm_id/idempotency_key 非空的占位逻辑。TTL 配置 `dc3.mcp.confirm-ttl`(默认 PT5M)。
- **OAuth refresh token 轮换 + 重放检测**（RFC 9700）：`dc3_oauth_authorization` 增 `previous_refresh_token_hash` 列。每次
  refresh 轮换签发新 refresh token，旧 token 被轮换后再次出现判定为泄露重放 → 吊销整条 authorization。
- **登录强制改密 / 密码过期**：本地登录校验密码通过后，若 `require_password_change=1` 或 `password_expire_time`
  已过，返回专用响应码（`R20303`/`R20304`）拒发 token，前端引导走公开端点 `/token/change_password` 自助改密。密码有效期配置
  `dc3.auth.password.expire-days`(默认 0=不过期)。

**工具目录刷新机制（实际落地：定时 + 进程内事件，未用 RabbitMQ / list_changed）**

- **定时兜底**：`OAuthMcpRuntimeServiceImpl.scheduledRefreshToolCatalog()` 经
  `@Scheduled(fixedDelayString=${dc3.mcp.tool.refresh-interval:PT5M})` 全量刷新。
- **事件驱动**：所有中心服务的资源注册经 facade 汇聚到 auth 进程内的 `ResourceRegistrySyncServiceImpl.sync()`；当
  `dc3_api`/`dc3_resource` 实际变更时发布进程内 `McpToolCatalogChangedEvent`，`OAuthMcpRuntimeServiceImpl` 经
  `@TransactionalEventListener(AFTER_COMMIT)` 刷新目录。因工具目录与其上游同在 auth 进程，**无需 RabbitMQ 跨进程**；正文"机制
  2/机制 3（RabbitMQ / notifications/tools/list_changed）"未实现。

## 背景

IoT DC3 的 auth、manager、data、agentic 四个中心服务已通过 `dc3-gateway` 聚合了完整的 OpenAPI 接口。为了让 AI Agent (Claude
Desktop、Cursor、GPT 等) 能以标准化方式调用这些接口，需要在 Gateway 上构建一个 MCP 服务，将 OpenAPI 接口转换为可治理、可授权、可审计的
MCP 工具。

MCP 服务需要满足以下要求:

1. **复用现有 OpenAPI** — 各中心服务已通过 Springdoc 生成了高质量的 OpenAPI spec，MCP 工具目录从 OpenAPI spec
   自动生成，再经过权限、风险和 AI 语义治理后对外暴露。
2. **复用现有权限体系** — MCP 工具的可见性和调用权限与系统统一后的租户、Principal、角色、资源绑定体系完全一致，不新建独立的权限模型。
3. **多配置共存** — 人类用户或服务账号可以创建多套 MCP OAuth 连接配置，所有配置共享同一个 MCP 服务地址，通过 OAuth
   client、授权主体和连接 ID 区分身份与工具白名单。
4. **统一地址** — 外部只暴露 `https://gateway:8000/mcp` 一个 MCP 入口，各中心服务不各自对外提供 MCP endpoint。
5. **AI 原生治理** — 不把所有 REST CRUD 原样暴露给模型；首版允许 OpenAPI
   自动生成原子工具，但默认只启用经过白名单、风险标注和审计策略覆盖的工具。后续优先沉淀业务型工具、MCP resources 和 prompts。

## 推荐结论

本方案采用 **Gateway 统一 MCP Runtime + 各中心服务提供 OpenAPI/能力元数据** 的建设方式:

- `dc3-gateway` 是唯一对外 MCP Server，负责 MCP 协议、OAuth Resource Server、租户、RBAC、工具白名单、风险策略、审计、限流和工具目录刷新。
- auth、manager、data、agentic 不对外提供独立 MCP 服务，只继续提供 REST/OpenAPI；后续可在 OpenAPI 中补充 `x-dc3-ai`
  扩展，声明风险等级、确认策略、业务标签等 AI 元数据。
- OpenAPI 生成的是底层原子工具目录，不等于全部对模型开放。最终对外工具 = 主体 RBAC 权限 ∩ MCP 白名单 ∩ 风险策略 ∩ 数据域约束。
- 首版重点暴露低风险读工具和少量明确需要的写工具；影响物理设备、删除、批量导入、权限变更等高风险工具必须默认关闭，并要求二次确认、幂等键和完整审计。

## 实施红线

- 不实现 `dc3mcp_*`、PAT、Token Key 或任何平台自定义长期 Bearer token。
- 不保留旧 `dc3_mcp_server` / `dc3_mcp_server_tool` 语义；统一使用 OAuth client、`dc3_mcp_connection` 和
  `dc3_mcp_connection_tool`。
- 不让各中心服务各自对外暴露 MCP endpoint；外部 MCP 入口只能是 Gateway `/mcp`。
- 不把 `/v3/api-docs/**` 在生产环境 `permitAll()`；生产必须内部可达、外部不可见，并有内部签名或 mTLS。
- 不信任 `tools/list` 的隐藏结果；`tools/call` 必须重新校验 OAuth、scope、RBAC、白名单、风险策略和数据域。
- 不允许白名单突破角色权限；白名单只缩小暴露面，不提高权限上限。

## 设计目标

- 在 Gateway 上新增 MCP Server 端点 (`/mcp`)，使用 Streamable HTTP (Stateless) 协议。
- Gateway 启动时从四个中心服务的内部 `/v3/api-docs` 拉取 OpenAPI spec，聚合为全局 MCP 工具目录。
- 从 `dc3_api` / `dc3_resource` 构建 `api_code`、`permission_code`、`tool_id` 的稳定映射，避免直接依赖 `entity_id` 一一关联。
- 授权主体或管理员在前端设置页面创建 MCP OAuth 连接，授权时选择租户、工具白名单和风险策略；系统不再生成长期静态 Token Key。
- 授权主体选择要暴露的工具子集，白名单绑定稳定 `tool_id`，不直接绑定易变的工具名称。
- AI Agent 通过 OAuth 2.1 Authorization Code + PKCE 完成授权，使用短期 Bearer access token 连接 `/mcp`。
- `tools/call` 时重新校验 OAuth token、scope、RBAC、白名单、风险策略和数据域，再注入标准主体身份 Headers 转发到后端服务执行。

## 技术选型

| 组件            | 选择                                                   | 理由                                                    |
|---------------|------------------------------------------------------|-------------------------------------------------------|
| MCP Server 框架 | Spring AI MCP Server WebFlux Starter                 | Spring 官方，与 MCP Java SDK 联合维护；WebFlux 与 Gateway 同栈    |
| 传输协议          | Streamable HTTP (Stateless)                          | 无状态模式匹配 OAuth Bearer Token 鉴权，每次请求独立验证                |
| 编程模型          | ASYNC (Reactor)                                      | 与 Gateway WebFlux 一致，非阻塞                              |
| 工具 Schema 来源  | 各中心服务的 OpenAPI spec                                  | 已有完整的 `@Schema`、`@Parameter`、`@Operation` 注解          |
| 权限码来源         | `dc3_api.api_name` + `dc3_resource.resource_code` 校验 | 多个 API 可能共享同一权限码，不能假设 `entity_id` 一一映射                |
| 认证方式          | OAuth 2.1 Authorization Server + Resource Server     | 不提供 PAT/Token Key 认证方式，MCP 对外只接受标准 OAuth access token |
| 工具调用传输        | HTTP (WebClient)                                     | 全覆盖，见 [HTTP vs gRPC 决策分析](#http-vs-grpc-决策分析)         |

### 依赖版本适配性

| 组件          | 要求        | 当前版本         | 状态              |
|-------------|-----------|--------------|-----------------|
| Java        | 17+       | 21           | ✅               |
| Spring Boot | 4.0.x     | 4.0.6        | ✅               |
| Spring AI   | 2.0.0+    | 2.0.0-M8     | ⚠️ 可用，建议后续升到正式版 |
| WebFlux     | Netty 运行时 | Netty 4.2.10 | ✅               |

## 前置依赖: 统一主体与服务账号

MCP/OAuth
建设前必须先按重建策略完成统一主体模型，详见 [统一主体与服务账号设计方案](identity-principal-service-account.md)
。该前置方案不保留旧身份、旧 token 或旧角色绑定运行路径。

本方案只消费该身份底座，不重复定义 IAM 表结构。MCP 侧依赖以下结论:

- 所有授权主体统一使用 `dc3_principal.id`。
- `USER`、`SERVICE_ACCOUNT`、`SYSTEM` 都是 Principal 类型。
- OAuth access token 的 `sub` 固定为 `principal_id`。
- Client Credentials 只能绑定 `SERVICE_ACCOUNT` principal。
- RBAC 入口为 `PermissionProvider.listPermissionCodes(tenantId, principalId)`。
- 内部转发身份头为 `X-Auth-Principal`，不再使用 `X-Auth-User`。

## OAuth 建设策略

### 结论: 一步到位建设完整 OAuth 2.1，只接受标准 OAuth token

MCP 远程服务只接受标准 OAuth access token，不再生成或识别 `dc3mcp_*` Token Key。`dc3-center-auth` 同步建设 OAuth
Authorization Server，`dc3-gateway` 建设 MCP Resource Server。所有 AI Agent 通过 Authorization Code + PKCE
完成交互式授权；服务账号和自动化场景走 OAuth Client Credentials，不走平台自定义 PAT。

一次性交付范围:

| 能力                          | 落地位置                      | 内容                                                                                        |
|-----------------------------|---------------------------|-------------------------------------------------------------------------------------------|
| Authorization Server        | `dc3-center-auth`         | Authorization Code + PKCE、Client Credentials、refresh token、consent、scope、JWK、token revoke |
| Resource Server             | `dc3-gateway`             | `/mcp` Bearer token 校验、Protected Resource Metadata、401 challenge、scope 校验                 |
| Client 管理                   | `dc3-center-auth` + 前端设置页 | OAuth client 注册、redirect URI、client secret hash、public/confidential client、服务账号绑定         |
| MCP 连接治理                    | `dc3_auth` schema         | OAuth 授权连接、工具白名单、风险策略、审计标签                                                                |
| Dynamic Client Registration | `dc3-center-auth`         | 支持 MCP 客户端自动注册；可由管理员开关控制                                                                  |

### OAuth 在 DC3 中的角色分工

```
AI Agent / MCP Client
  │ Authorization Code + PKCE
  ▼
dc3-gateway
  │ MCP Resource Server
  │ - /.well-known/oauth-protected-resource
  │ - /mcp Bearer token validation
  │ - token claims -> tenant/principal/connection
  │ - RBAC + whitelist + risk policy
  ▼
dc3-center-auth
  │ Authorization Server
  │ - login / consent
  │ - client registration
  │ - access token / refresh token
  │ - scopes
  ▼
dc3_principal / dc3_role_principal_bind / dc3_role_resource_bind / dc3_resource
```

### Token 类型

只支持 OAuth token:

| Token 类型                        | 用途                                  | 要求                                                                                                        |
|---------------------------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------|
| access token                    | 调用 `/mcp`、`tools/list`、`tools/call` | 短有效期，JWT 签名或 introspection，包含 `sub=principal_id`、`principal_type`、`tenant_id`、`mcp_connection_id`、`scope` |
| refresh token                   | 续期 access token                     | 仅 Authorization Code + PKCE 场景发放，支持撤销和轮换                                                                  |
| authorization code              | 授权码交换 token                         | 必须绑定 PKCE code verifier，短有效期，一次性使用                                                                        |
| client credentials access token | 服务账号/自动化调用                          | 只能绑定服务账号和最小角色，不代表人类用户                                                                                     |

OAuth token 进入统一的 `McpAuthContext`:

```java
record McpAuthContext(
    Long tenantId,
    Long principalId,
    String principalType,  // USER / SERVICE_ACCOUNT
    String clientId,
    Long connectionId,
    String grantType,      // authorization_code / client_credentials
    Set<String> scopes,
    Set<String> permissionCodes
) {}
```

### OAuth 安全基线

必须按 OAuth 2.1 的安全模型一次性落地，不保留弱授权方式:

| 约束                          | 要求                                                                                                                                                               |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 授权模式                        | 只开放 Authorization Code + PKCE 和 Client Credentials；禁止 password grant、implicit grant、device code 的未评估启用和平台自定义 PAT                                                 |
| PKCE                        | public client 必须使用 PKCE S256；不接受 plain code challenge                                                                                                            |
| Redirect URI                | 注册时保存完整 redirect URI，授权时精确匹配，不允许通配符和动态拼接                                                                                                                         |
| Token 绑定                    | access token 必须校验 `iss`、`aud`、`exp`、`nbf`、`sub`、`principal_type`、`scope`、`tenant_id`、`mcp_connection_id`；推荐支持 OAuth resource indicator，使 token 只能用于 MCP resource |
| Token 生命周期                  | access token 短有效期；refresh token 仅对 authorization_code 发放，必须轮换、可撤销、可感知重放                                                                                          |
| Client secret               | confidential client 的 secret 只展示一次，数据库只保存 hash；支持轮换、过期和撤销                                                                                                        |
| Dynamic Client Registration | 生产默认需要管理员开关、redirect URI 白名单、client 类型限制和审计；不能让任意外部客户端无限注册                                                                                                       |
| CORS / Origin               | `/mcp`、`/oauth2/token`、`/oauth2/register` 按客户端类型配置明确来源；不使用全局 `*`                                                                                                 |
| 日志与审计                       | token、authorization code、refresh token、client secret 不进入日志；审计只记录 client_id、connection_id、scope、结果和摘要                                                             |

### Scope 与 RBAC 的关系

OAuth scope 不替代 DC3 RBAC，只做粗粒度入口约束:

| Scope                 | 含义                          |
|-----------------------|-----------------------------|
| `mcp:tools:list`      | 允许读取工具列表                    |
| `mcp:tools:call`      | 允许调用工具                      |
| `mcp:tools:call:high` | 允许调用高风险工具，但仍需白名单、RBAC 和二次确认 |
| `mcp:resources:read`  | 允许读取 MCP resources          |

最终授权仍然是:

```
OAuth scope
  ∩ DC3 RBAC permission_code
  ∩ MCP 工具白名单
  ∩ 风险策略
  ∩ 租户/数据域约束
```

### OAuth 端点建设

必须同时提供 Resource Server 发现、授权服务器发现和完整授权流程:

| Endpoint / Header                                          | 责任                                          |
|------------------------------------------------------------|---------------------------------------------|
| `/.well-known/oauth-protected-resource`                    | 声明 MCP resource 标识和 authorization server 地址 |
| `WWW-Authenticate: Bearer resource_metadata="..."`         | 未授权访问 `/mcp` 时引导客户端发现授权服务器                  |
| `/mcp`                                                     | 校验 Bearer Token，处理 MCP JSON-RPC             |
| `/.well-known/oauth-authorization-server` 或 OIDC discovery | 授权服务器元数据                                    |
| `/oauth2/authorize`                                        | Authorization Code + PKCE 授权入口              |
| `/oauth2/token`                                            | access token / refresh token                |
| `/oauth2/jwks`                                             | JWT 公钥                                      |
| `/oauth2/revoke`                                           | token 撤销                                    |
| `/oauth2/register`                                         | Dynamic Client Registration                 |

### 实施建议

1. `dc3-center-auth` 增加 OAuth Authorization Server 模块，优先复用 Spring Authorization Server 的 registered
   client、authorization、consent 抽象。
2. `dc3-gateway` 的 `/mcp` 作为 Resource Server 验证 access token，再进入 MCP transport handler。
3. 授权页在用户登录后要求选择租户、MCP 连接配置、工具白名单和高风险工具策略，确认后生成 `dc3_mcp_connection`。
4. access token 中写入 `tenant_id`、`sub=principal_id`、`principal_type`、`mcp_connection_id`、`scope`；`tools/list` 和
   `tools/call` 以这些 claims 为入口构建 `McpAuthContext`。
5. 前端连接信息弹窗只提供 OAuth 配置，不提供 Token Key。

### OAuth 后的使用流程

MCP OAuth 后不再存在平台自定义的长期 MCP Token 配置。AI Agent 使用的是标准 OAuth 授权结果:

| 场景                        | 适用主体                   | Agent 侧保存什么                                                          | DC3 侧保存什么                                                                 |
|---------------------------|------------------------|----------------------------------------------------------------------|---------------------------------------------------------------------------|
| Authorization Code + PKCE | 人类用户 `USER`            | client_id、issuer/resource metadata、短期 access token、可轮换 refresh token | OAuth client、authorization、consent、`dc3_mcp_connection`、工具白名单             |
| Client Credentials        | 服务账号 `SERVICE_ACCOUNT` | client_id + client secret，或 `private_key_jwt` 私钥引用                   | OAuth client secret hash / JWKS、服务账号 Principal、`dc3_mcp_connection`、工具白名单 |

交互式用户流程:

1. 管理员注册 OAuth client，或在开启审计和白名单后允许 Dynamic Client Registration。
2. Agent 访问 `/mcp` 未带 token 时，Gateway 返回 Bearer challenge 和 Protected Resource Metadata。
3. Agent 跳转到 `dc3-center-auth` 的 `/oauth2/authorize`，使用 Authorization Code + PKCE。
4. 用户完成登录，选择当前租户，选择或创建 MCP 连接，确认工具白名单和风险策略。
5. `dc3-center-auth` 发放短期 access token，并按需发放可轮换 refresh token。
6. Agent 后续调用 `/mcp` 时只携带 `Authorization: Bearer <access-token>`。

服务账号流程:

1. 管理员先在身份前端创建服务账号，设置负责人、用途、过期时间和租户成员关系。
2. 管理员给服务账号绑定最小角色，并创建 confidential OAuth client。
3. OAuth client 必须绑定 `SERVICE_ACCOUNT` principal 和单个 `tenant_id`。
4. 系统只展示一次 client secret，或注册 `private_key_jwt` 公钥；数据库不保存明文 secret。
5. 自动化 Agent 调用 `/oauth2/token` 使用 Client Credentials 获取短期 access token，再访问 `/mcp`。

这里仍可能存在 `client_secret` 或私钥配置，但它是 OAuth client 凭证，不是 DC3 自定义 MCP Token。它必须支持轮换、撤销、过期、最小
scope 和审计。

## 整体架构

```
                    ┌──────────────────────────┐
                    │   AI Agent (Claude/GPT)   │
                    └────────────┬─────────────┘
                                 │ MCP Streamable HTTP
                                 │ POST/GET /mcp
                                 │ Authorization: Bearer <oauth-access-token>
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      dc3-gateway (port 8000)                        │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              MCP Server (/mcp endpoint)                     │   │
│   │                                                             │   │
│   │  Layer 1: WebFluxStreamableServerTransportProvider          │   │
│   │  • JSON-RPC over HTTP POST/GET                              │   │
│   │  • SSE streaming for multi-message responses                │   │
│   │                                                             │   │
│   │  Layer 2: McpAuthWebFilter                                  │   │
│   │  • Bearer token → OAuth claims → tenant/principal/connection│   │
│   │  • PermissionProvider → principal permission codes           │   │
│   │  • Build McpAuthContext into Reactor Context                │   │
│   │                                                             │   │
│   │  Layer 3: McpAsyncServer + ToolCatalog + RiskPolicy         │   │
│   │  • tools/list: permissions + whitelist + risk filter        │   │
│   │  • tools/call: re-check + audit + auth headers → HTTP       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  原有 REST API Routes (不变)                                │   │
│   │  /api/v3/auth/** → dc3-center-auth:8300                     │   │
│   │  /api/v3/manager/** → dc3-center-manager:8400               │   │
│   │  /api/v3/data/** → dc3-center-data:8500                     │   │
│   │  /api/v3/agentic/** → dc3-center-agentic:8600              │   │
│   └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
         │                │                │                │
         ▼                ▼                ▼                ▼
   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
   │   Auth   │    │ Manager  │    │   Data   │    │ Agentic  │
   │  :8300   │    │  :8400   │    │  :8500   │    │  :8600   │
   │  OpenAPI │    │  OpenAPI │    │  OpenAPI │    │  OpenAPI │
   └──────────┘    └──────────┘    └──────────┘    └──────────┘
```

## 数据库模型

OAuth 与 MCP 治理数据放在 `dc3_auth` schema 中，与用户、角色、资源同属权限域。遵循项目已有的数据库约定。

### 前置 Identity 表

MCP 本文档不重复定义 Identity 表结构。以下表由 [统一主体与服务账号设计方案](identity-principal-service-account.md)
先行建设:

| 表                         | MCP 依赖点                                         |
|---------------------------|-------------------------------------------------|
| `dc3_principal`           | OAuth token `sub=principal_id`，MCP 审计和权限主体      |
| `dc3_local_credential`    | Authorization Code 登录时解析本地用户名密码到 `principal_id` |
| `dc3_tenant_membership`   | 校验主体是否属于当前租户                                    |
| `dc3_service_account`     | Client Credentials 绑定的机器身份                      |
| `dc3_role_principal_bind` | 主体到角色的 RBAC 绑定                                  |

MCP 侧只读取这些标准身份结果，不把 `user_id` 作为统一主体使用。

### OAuth 标准表

OAuth Authorization Server 建议优先复用 Spring Authorization Server 的数据模型，并按 DC3 命名和审计字段落库:

| 表                                 | 作用                                                                                                                                                      |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dc3_oauth_registered_client`     | OAuth client 注册信息，包含 client_id、client_secret_hash、redirect_uri、grant_type、scope、client 类型；Client Credentials client 必须绑定 `service_account_principal_id` |
| `dc3_oauth_authorization`         | authorization code、access token、refresh token、token claims、过期和撤销状态                                                                                      |
| `dc3_oauth_authorization_consent` | 用户对 client 和 scope 的授权同意                                                                                                                                |

> **安全要求**: client secret 只展示一次，数据库只保存 hash；public client 必须使用 Authorization Code + PKCE；confidential
> client 必须校验 client secret 或 mTLS/private_key_jwt；Client Credentials 必须绑定 `SERVICE_ACCOUNT`
> principal，并具备明确租户、负责人、用途、过期时间和最小角色。

最小字段契约如下。实现时可以使用 Spring Authorization Server 的实体和 JDBC service 适配这些表，但不能省略以下安全字段。

#### dc3_oauth_registered_client

```sql
CREATE TABLE dc3_oauth_registered_client
(
    id                           BIGINT PRIMARY KEY NOT NULL,
    client_id                    TEXT     DEFAULT ''::TEXT          NOT NULL,
    client_name                  TEXT     DEFAULT ''::TEXT          NOT NULL,
    client_type                  TEXT     DEFAULT 'PUBLIC'::TEXT    NOT NULL, -- PUBLIC / CONFIDENTIAL
    owner_principal_id           BIGINT   DEFAULT 0 NOT NULL,
    service_account_principal_id BIGINT   DEFAULT 0 NOT NULL,                -- client_credentials 必填
    tenant_id                    BIGINT   DEFAULT 0 NOT NULL,                -- client_credentials 必填
    client_secret_hash           TEXT     DEFAULT ''::TEXT          NOT NULL,
    client_secret_expires_at     TIMESTAMPTZ,
    client_auth_methods          TEXT     DEFAULT ''::TEXT          NOT NULL, -- none/client_secret_basic/private_key_jwt/mtls
    authorization_grant_types    TEXT     DEFAULT ''::TEXT          NOT NULL,
    redirect_uris                TEXT     DEFAULT ''::TEXT          NOT NULL,
    scopes                       TEXT     DEFAULT ''::TEXT          NOT NULL,
    jwks_uri                     TEXT     DEFAULT ''::TEXT          NOT NULL,
    jwk_set                      JSON     DEFAULT '{}'::JSON        NOT NULL,
    require_pkce                 SMALLINT DEFAULT 1 NOT NULL,
    require_consent              SMALLINT DEFAULT 1 NOT NULL,
    enable_flag                  SMALLINT DEFAULT 0 NOT NULL,
    client_settings              JSON     DEFAULT '{}'::JSON        NOT NULL,
    token_settings               JSON     DEFAULT '{}'::JSON        NOT NULL,
    remark                       TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id                   BIGINT   DEFAULT 0 NOT NULL,
    creator_name                 TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time                  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id                  BIGINT   DEFAULT 0 NOT NULL,
    operator_name                TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted                      SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_oauth_client_type CHECK (client_type IN ('PUBLIC', 'CONFIDENTIAL')),
    CONSTRAINT chk_oauth_client_require_pkce CHECK (require_pkce IN (0, 1)),
    CONSTRAINT chk_oauth_client_require_consent CHECK (require_consent IN (0, 1)),
    CONSTRAINT chk_oauth_client_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_oauth_client_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_oauth_client_id_active_unique
    ON dc3_oauth_registered_client (client_id)
    WHERE deleted = 0 AND client_id <> ''::TEXT;
```

约束:

- `PUBLIC` client 只能走 Authorization Code + PKCE，`client_auth_methods` 使用 `none`。
- `CONFIDENTIAL` client 必须有 secret hash、`private_key_jwt`、mTLS 之一。
- `client_credentials` grant 必须满足 `service_account_principal_id <> 0` 且 `tenant_id <> 0`。
- `redirect_uris` 必须精确匹配，不能使用通配符、前缀匹配或运行时拼接。

#### dc3_oauth_authorization

```sql
CREATE TABLE dc3_oauth_authorization
(
    id                         BIGINT PRIMARY KEY NOT NULL,
    registered_client_id       BIGINT   DEFAULT 0 NOT NULL,
    client_id                  TEXT     DEFAULT ''::TEXT          NOT NULL,
    principal_id               BIGINT   DEFAULT 0 NOT NULL,
    principal_type             TEXT     DEFAULT 'USER'::TEXT      NOT NULL,
    tenant_id                  BIGINT   DEFAULT 0 NOT NULL,
    mcp_connection_id          BIGINT   DEFAULT 0 NOT NULL,
    authorization_grant_type   TEXT     DEFAULT ''::TEXT          NOT NULL,
    authorized_scopes          TEXT     DEFAULT ''::TEXT          NOT NULL,
    state_hash                 TEXT     DEFAULT ''::TEXT          NOT NULL,
    authorization_code_hash    TEXT     DEFAULT ''::TEXT          NOT NULL,
    authorization_code_issued  TIMESTAMPTZ,
    authorization_code_expires TIMESTAMPTZ,
    access_token_jti           TEXT     DEFAULT ''::TEXT          NOT NULL,
    access_token_issued        TIMESTAMPTZ,
    access_token_expires       TIMESTAMPTZ,
    refresh_token_hash         TEXT     DEFAULT ''::TEXT          NOT NULL,
    refresh_token_issued       TIMESTAMPTZ,
    refresh_token_expires      TIMESTAMPTZ,
    token_claims               JSON     DEFAULT '{}'::JSON        NOT NULL,
    token_metadata             JSON     DEFAULT '{}'::JSON        NOT NULL,
    revoked_time               TIMESTAMPTZ,
    revoke_reason              TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time                TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operate_time               TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted                    SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_oauth_authorization_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_oauth_authorization_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_oauth_authorization_client_principal
    ON dc3_oauth_authorization (client_id, principal_id, tenant_id) WHERE deleted = 0;

CREATE INDEX idx_oauth_authorization_code_hash
    ON dc3_oauth_authorization (authorization_code_hash)
    WHERE deleted = 0 AND authorization_code_hash <> ''::TEXT;

CREATE INDEX idx_oauth_authorization_access_token_jti
    ON dc3_oauth_authorization (access_token_jti)
    WHERE deleted = 0 AND access_token_jti <> ''::TEXT;

CREATE INDEX idx_oauth_authorization_refresh_token_hash
    ON dc3_oauth_authorization (refresh_token_hash)
    WHERE deleted = 0 AND refresh_token_hash <> ''::TEXT;
```

约束:

- authorization code 和 refresh token 只保存 hash；access token 如果是 JWT，只保存 `jti`、claims 摘要和撤销状态。
- refresh token 只能在 Authorization Code + PKCE 场景发放，Client Credentials 不发 refresh token。
- token claims 必须包含 `iss`、`aud`、`exp`、`sub=principal_id`、`principal_type`、`tenant_id`、`client_id`、
  `mcp_connection_id`、`scope`。
- 撤销 client、撤销 MCP connection、禁用 Principal 或移出租户时，相关 authorization 必须立即失效。

#### dc3_oauth_authorization_consent

```sql
CREATE TABLE dc3_oauth_authorization_consent
(
    id                   BIGINT PRIMARY KEY NOT NULL,
    registered_client_id BIGINT   DEFAULT 0 NOT NULL,
    client_id            TEXT     DEFAULT ''::TEXT          NOT NULL,
    principal_id         BIGINT   DEFAULT 0 NOT NULL,
    tenant_id            BIGINT   DEFAULT 0 NOT NULL,
    scopes               TEXT     DEFAULT ''::TEXT          NOT NULL,
    consent_ext          JSON     DEFAULT '{}'::JSON        NOT NULL,
    create_time          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operate_time         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted              SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_oauth_consent_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_oauth_consent_client_principal_active_unique
    ON dc3_oauth_authorization_consent (registered_client_id, principal_id, tenant_id)
    WHERE deleted = 0;
```

consent 只表达主体允许 OAuth client 使用哪些 scope，不表达业务权限。业务权限仍由 `dc3_role_principal_bind` 和
`dc3_role_resource_bind` 决定。

### dc3_mcp_connection — MCP OAuth 连接配置表

```sql
-- Table structure for dc3_mcp_connection
SET search_path TO dc3_auth;

CREATE TABLE dc3_mcp_connection
(
    id              BIGINT PRIMARY KEY NOT NULL,                    -- 主键 ID
    connection_name TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 连接名称
    client_id       TEXT     DEFAULT ''::TEXT          NOT NULL,    -- OAuth client_id
    principal_id    BIGINT   DEFAULT 0 NOT NULL,                    -- 授权主体 ID, dc3_principal.id
    principal_type  TEXT     DEFAULT 'USER'::TEXT      NOT NULL,    -- 主体类型: USER / SERVICE_ACCOUNT
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                    -- 所属租户 ID
    grant_type      TEXT     DEFAULT 'authorization_code'::TEXT NOT NULL, -- 授权类型
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                    -- 启用标志, 0: 启用, 1: 禁用
    expire_time     TIMESTAMPTZ,                                    -- 过期时间 (可选, NULL 表示永不过期)
    revoke_time     TIMESTAMPTZ,                                    -- 撤销时间 (可选, NULL 表示未撤销)
    last_used_time  TIMESTAMPTZ,                                    -- 最近一次使用时间
    connection_ext  JSON     DEFAULT '{}'::JSON        NOT NULL,    -- 扩展信息
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 描述
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                    -- 创建人 ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 创建人名称
    create_time     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                    -- 操作人 ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 操作人名称
    operate_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 操作时间
    deleted         SMALLINT DEFAULT 0 NOT NULL,                    -- 逻辑删除标志, 0: 未删除, 1: 已删除
    CONSTRAINT chk_mcp_connection_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_connection_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_mcp_connection_grant_type CHECK (grant_type IN ('authorization_code', 'client_credentials')),
    CONSTRAINT chk_mcp_connection_deleted CHECK (deleted IN (0, 1))
);

-- 同一租户、主体、client 下连接名称唯一
CREATE UNIQUE INDEX idx_mcp_connection_principal_client_name_active_unique
    ON dc3_mcp_connection (tenant_id, principal_id, client_id, connection_name)
    WHERE deleted = 0 AND connection_name <> ''::TEXT;

-- 按 client 查询
CREATE INDEX idx_mcp_connection_client_id
    ON dc3_mcp_connection (client_id) WHERE deleted = 0;

-- 按主体查询
CREATE INDEX idx_mcp_connection_principal_id
    ON dc3_mcp_connection (principal_id) WHERE deleted = 0;

-- 按租户查询
CREATE INDEX idx_mcp_connection_tenant_id
    ON dc3_mcp_connection (tenant_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE ON dc3_mcp_connection
    FOR EACH ROW EXECUTE FUNCTION update_operate_time();
```

> **设计说明**: 本表不保存任何 access token 或 refresh token，只保存 OAuth client、授权主体、租户和 MCP 工具治理配置之间的绑定关系。
`principal_type=USER` 表示人类用户授权；`principal_type=SERVICE_ACCOUNT` 表示 Client Credentials 绑定的服务账号。服务账号不复用
`dc3_user.id` 语义，而是通过 `dc3_principal.id` 参与租户成员、角色绑定和审计。`mcp_connection_id` 写入 access token claims，
`tools/list` 和 `tools/call` 通过它加载白名单、风险策略和审计标签。

### dc3_mcp_tool_catalog — MCP 工具目录表

```sql
-- Table structure for dc3_mcp_tool_catalog
CREATE TABLE dc3_mcp_tool_catalog
(
    id                  BIGINT PRIMARY KEY NOT NULL,                    -- 主键 ID
    tool_id             TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 稳定工具 ID, 如 manager:POST:/device/add
    tool_name           TEXT     DEFAULT ''::TEXT          NOT NULL,    -- MCP 工具名称, 如 manager_device_add
    tool_title          TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 工具展示名称
    tool_category       TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 工具分类 auth/manager/data/agentic
    service_name        TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 后端服务名
    api_code            TEXT     DEFAULT ''::TEXT          NOT NULL,    -- dc3_api.api_code
    permission_code     TEXT     DEFAULT ''::TEXT          NOT NULL,    -- dc3_resource.resource_code
    http_method         TEXT     DEFAULT ''::TEXT          NOT NULL,    -- GET/POST/PUT/DELETE
    api_path            TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 后端 API path
    schema_hash         TEXT     DEFAULT ''::TEXT          NOT NULL,    -- input/output schema 摘要
    risk_level          TEXT     DEFAULT 'LOW'::TEXT       NOT NULL,    -- LOW/MEDIUM/HIGH
    read_only_hint      SMALLINT DEFAULT 0 NOT NULL,                    -- MCP annotation: readOnlyHint
    destructive_hint    SMALLINT DEFAULT 0 NOT NULL,                    -- MCP annotation: destructiveHint
    idempotent_hint     SMALLINT DEFAULT 0 NOT NULL,                    -- MCP annotation: idempotentHint
    open_world_hint     SMALLINT DEFAULT 0 NOT NULL,                    -- MCP annotation: openWorldHint
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                    -- 启用标志, 0: 启用, 1: 禁用
    tool_ext            JSON     DEFAULT '{}'::JSON        NOT NULL,    -- OpenAPI/MCP 扩展元数据
    remark              TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 描述
    creator_id          BIGINT   DEFAULT 0 NOT NULL,
    creator_name        TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id         BIGINT   DEFAULT 0 NOT NULL,
    operator_name       TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time        TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted             SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_mcp_tool_catalog_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_mcp_tool_catalog_tool_id_active_unique
    ON dc3_mcp_tool_catalog (tool_id) WHERE deleted = 0 AND tool_id <> ''::TEXT;

CREATE INDEX idx_mcp_tool_catalog_permission_code
    ON dc3_mcp_tool_catalog (permission_code) WHERE deleted = 0;

CREATE INDEX idx_mcp_tool_catalog_category
    ON dc3_mcp_tool_catalog (tool_category) WHERE deleted = 0;
```

> **设计说明**: 工具目录表是 OpenAPI、权限码、MCP 工具名和风险元数据之间的稳定中间层。白名单绑定 `tool_id`，而不是直接绑定
`tool_name` 或路径，避免接口描述变化、工具重命名、多个 API 共享权限码时出现错配。`tool_name` 面向 MCP 客户端，必须稳定、短小、语义清楚；
`tool_id` 面向平台治理，必须可追踪到原始 API。

### dc3_mcp_connection_tool — MCP 工具白名单表

```sql
-- Table structure for dc3_mcp_connection_tool
CREATE TABLE dc3_mcp_connection_tool
(
    id              BIGINT PRIMARY KEY NOT NULL,                    -- 主键 ID
    connection_id   BIGINT   DEFAULT 0 NOT NULL,                    -- 关联 MCP OAuth 连接 ID
    tool_id         TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 关联 dc3_mcp_tool_catalog.tool_id
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                    -- 启用标志, 0: 启用, 1: 禁用
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 描述
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                    -- 创建人 ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 创建人名称
    create_time     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                    -- 操作人 ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 操作人名称
    operate_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 操作时间
    deleted         SMALLINT DEFAULT 0 NOT NULL,                    -- 逻辑删除标志, 0: 未删除, 1: 已删除
    CONSTRAINT chk_mcp_connection_tool_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_connection_tool_deleted CHECK (deleted IN (0, 1))
);

-- 同一配置下工具名唯一
CREATE UNIQUE INDEX idx_mcp_connection_tool_conn_tool_active_unique
    ON dc3_mcp_connection_tool (connection_id, tool_id) WHERE deleted = 0 AND tool_id <> ''::TEXT;

-- 按配置 ID 查询
CREATE INDEX idx_mcp_connection_tool_connection_id
    ON dc3_mcp_connection_tool (connection_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE ON dc3_mcp_connection_tool
    FOR EACH ROW EXECUTE FUNCTION update_operate_time();
```

> **设计说明**: 白名单表的作用是让授权主体在"有权访问的工具"中进一步挑选"愿意暴露给某个 OAuth 连接的工具"
>
。它不替代权限控制——白名单不能突破角色权限。即使勾选了某工具，如果主体角色没有对应权限码，该工具也不会出现。白名单提供两个价值: (

1) 精细化控制每个 OAuth 连接暴露的工具子集; (2) OAuth client 被滥用时限制攻击面。

### 表关系

```
dc3_oauth_registered_client ──1:N── dc3_mcp_connection ──1:N── dc3_mcp_connection_tool ──N:1── dc3_mcp_tool_catalog
                                      │
dc3_principal ────────────────────────┘

dc3_mcp_connection.principal_id 决定身份和权限范围
dc3_mcp_connection.tenant_id 决定租户隔离
dc3_mcp_connection.client_id 决定 OAuth client

一个 Principal 可以为不同 OAuth client 创建多套 MCP 连接配置 (不同工具子集、不同风险策略)
```

## 权限控制方案

### 核心原则: 复用角色资源体系，主体绑定升级为 Principal

MCP 服务的权限控制复用系统已有的 `dc3_resource` / `dc3_role_resource_bind` 授权资源体系，但角色和调用主体的绑定统一升级为
`dc3_role_principal_bind`。这不是给 MCP 新建一套权限模型，而是把现有"用户绑定角色"标准化为"主体绑定角色"
，让人类用户、服务账号和系统主体使用同一条 RBAC 链路。

MCP 在现有 RBAC 之上只增加"暴露面控制"和"AI 风险控制"，不增加新的授权上限:

```
最终可调用工具
  = Principal RBAC 权限
  ∩ OAuth scope
  ∩ MCP 连接工具白名单
  ∩ 风险策略
  ∩ 数据域/租户约束
```

`tools/list` 只决定模型能看到什么；`tools/call` 必须重新执行同样的校验，不能信任客户端没有调用隐藏工具。

### 标准权限链路

```
@PreAuthorize("@perm.can('device','add')")
  → ApiEndpointScanner 扫描 → dc3_api {api_name="device:add"}
  → ResourceRegistrySyncServiceImpl → dc3_resource {resource_code="dc3-center-manager:device:add"}
  → dc3_role_resource_bind (角色绑定资源)
  → dc3_role_principal_bind (主体绑定角色)
  → PermissionProvider.listPermissionCodes(tenantId, principalId) → Set<resource_code>
```

### 权限聚合机制

系统支持一个主体绑定多个角色（`dc3_role_principal_bind` 是多对多关系），权限在所有角色间取并集:

```
Principal A 绑定了两个角色:
  角色 1 (设备管理员): device:add, device:update, device:delete, device:list
  角色 2 (数据查看者): point_value:latest, point_value:latest, alarm:list

最终权限码 = 角色1 ∪ 角色2 = 7 个权限码
```

聚合过程由 `RoleResourceBindServiceImpl.listResourceByPrincipalId()` 完成:

1. `dc3_role_principal_bind` WHERE tenant_id = ? AND principal_id = ? → 得到所有 role_id
2. 过滤启用状态 + 租户范围 → 有效 role_id 列表
3. `dc3_role_resource_bind` WHERE role_id IN (...) → 所有 resource_id (去重)
4. `dc3_resource` WHERE id IN (...) → 所有 resource_code → 组成 `Set<String>`

没有"拒绝"机制，是纯加法。通配符 `*` 满足所有检查。MCP 直接调用
`PermissionProvider.listPermissionCodes(tenantId, principalId)` 即可获得聚合后的权限码集合。

### dc3_api 与 dc3_resource 的关系

`dc3_api` 记录具体 HTTP 端点，`dc3_resource` 记录可授权的权限资源。两者有 `entity_id` 关联，但 **不能假设每个 API 都有一个独立
resource row**。现有资源同步会按 `resource_code` 去重，多个 API 可能共享同一个权限码:

```
dc3_api                                              dc3_resource
───────────────────────────                          ──────────────────────────────
id: 1001                                             id: 2001
service_name: dc3-center-manager                     service_name: dc3-center-manager
api_name: device:list           ──────────────→      resource_code: dc3-center-manager:device:list
api_code: dc3-center-manager:POST:/device/list       entity_id: 1001
api_group: DeviceController                          resource_type_flag: API

id: 1002                                             同一个 resource_code 可能覆盖多个查询 API
service_name: dc3-center-manager
api_name: device:list
api_code: dc3-center-manager:POST:/device/list_by_ids
api_group: DeviceController
```

因此 MCP 工具目录构建时使用以下稳定规则:

1. 以 OpenAPI 的 `service + method + path` 匹配 `dc3_api.api_code`。
2. 取 `dc3_api.api_name` 生成权限码: `{service_name}:{api_name}`。
3. 用 `dc3_resource.resource_code` 校验该权限码存在、启用且 `resource_type_flag = API`。
4. 多个 API 共享同一权限码时，为每个 API 生成独立 `tool_id`，但它们的 `permission_code` 相同。

这样既保留细粒度工具调用，又与现有 RBAC 权限粒度一致。

### MCP 工具目录构建

Gateway 启动时，`McpToolAggregator` 从两个数据源合并构建全局工具目录:

**数据源 1: OpenAPI spec** — 提供工具的描述信息和参数 Schema

```
GET http://dc3-center-manager:8400/manager/v3/api-docs
→ 解析 OpenAPI Paths + Components
→ 每个 Operation 生成:
  {
    name: "manager_device_add",
    description: "Add a new device",               // 来自 @Operation(summary)
    inputSchema: {                                  // 来自 @RequestBody @Schema 展开
      "properties": {
        "deviceName": {"type":"string","description":"设备名称"},
        "driverId":   {"type":"integer","description":"关联驱动 ID"},
        "profileId":  {"type":"integer","description":"关联模板 ID"}
      },
      "required": ["deviceName","driverId","profileId"]
    },
    httpMethod: "POST",
    apiPath: "/device/add",
    category: "manager"
  }
```

**数据源 2: dc3_api + dc3_resource** — 提供每个 API 端点的权限码校验

```sql
SELECT a.id, a.api_code, a.api_name, a.service_name, a.api_group,
       r.resource_code
FROM   dc3_api a
JOIN   dc3_resource r
       ON r.service_name = a.service_name
      AND r.resource_code = CONCAT(a.service_name, ':', a.api_name)
WHERE  a.deleted = 0
  AND  r.deleted = 0
  AND  a.enable_flag = 0
  AND  r.enable_flag = 0
  AND  r.resource_type_flag = 6  -- ResourceTypeEnum.API
  AND  a.service_name IN ('dc3-center-auth','dc3-center-manager',
                          'dc3-center-data','dc3-center-agentic');
```

**合并结果 — 完整工具目录条目**:

```json
{
  "toolId": "dc3-center-manager:POST:/device/add",
  "name": "manager_device_add",
  "title": "Add Device",
  "description": "Add a new device",
  "inputSchema": { "properties": {...}, "required": [...] },
  "outputSchema": { "properties": {...} },
  "httpMethod": "POST",
  "apiPath": "/device/add",
  "category": "manager",
  "targetServiceUrl": "http://dc3-center-manager:8400",
  "apiCode": "dc3-center-manager:POST:/device/add",
  "permissionCode": "dc3-center-manager:device:add",
  "annotations": {
    "readOnlyHint": false,
    "destructiveHint": false,
    "idempotentHint": false,
    "openWorldHint": false
  },
  "riskLevel": "MEDIUM"
}
```

### 工具元数据与 AI 语义治理

当前 OpenAPI 注解不包含读写分类、风险等级、确认策略、幂等语义等 AI 工具治理元数据。现有注解情况:

| 有什么                                | 覆盖率           | 缺什么                                        |
|------------------------------------|---------------|--------------------------------------------|
| `@Operation(summary, description)` | 302 个端点 100%  | 无 `deprecated`、无 `security`、无 `extensions` |
| `@Tag(name, description)`          | 55 个 Tag 100% | 无自定义扩展                                     |
| `@Parameter(description)`          | 查询参数 100%     | —                                          |
| `@Schema(description, example)`    | VO 字段 100%    | —                                          |

首版不要求一次性改造所有 OpenAPI 注解，而是在 Gateway 侧聚合工具时自动推导元数据，并落入 `dc3_mcp_tool_catalog`
。推导结果是兜底值，后续可通过 `tool_ext` 或 OpenAPI `x-dc3-ai` 扩展覆盖:

```java
ToolMetadata meta = ToolMetadata.builder()
    .name(toolName)
    .title(operation.getSummary())
    .description(operation.getSummary())
    .inputSchema(inputSchema)
    .outputSchema(outputSchema)
    .annotations(ToolAnnotations.builder()
        .readOnlyHint(deriveReadOnlyHint(httpMethod, apiPath, scope))
        .destructiveHint(deriveDestructiveHint(scope))
        .idempotentHint(deriveIdempotentHint(httpMethod, scope))
        .openWorldHint(false)
        .build())
    .riskLevel(deriveRiskLevel(scope))
    .confirmationPolicy(deriveConfirmationPolicy(scope))
    .build();
```

推导规则:

| 维度                  | 推导来源                                     | 规则                                                                                                              |
|---------------------|------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| **readOnlyHint**    | HTTP 方法 + API 路径 + `@PreAuthorize` scope | `GET` 请求 → true; `POST` 请求中 `/list`、`/get_*`、`/count` → true; 其余 POST → false                                   |
| **destructiveHint** | `@PreAuthorize` scope + 业务域              | `delete`、`reset`、批量导入、设备指令下发 → true; 普通 add/update 默认 false                                                     |
| **idempotentHint**  | HTTP 方法 + scope                          | `GET` → true; `POST /list` → true; `POST /add` → false; 删除是否幂等由具体接口显式覆盖                                         |
| **openWorldHint**   | 是否访问外部系统或物理设备                            | 普通 DC3 数据库读写 → false; 设备指令、外部模型调用、通知发送 → true                                                                   |
| **风险等级**            | `@PreAuthorize` scope 语义                 | `delete` → HIGH (不可逆); `add`/`update` → MEDIUM; `get`/`list` → LOW; 特殊操作如 `point_command:write` → HIGH (影响物理设备) |
| **确认策略**            | 风险等级 + 业务域                               | LOW 无需确认; MEDIUM 可配置确认; HIGH 默认必须二次确认并写审计                                                                       |

这些元数据用于:

- MCP Tool annotations 帮助客户端理解工具是否只读、是否破坏性、是否幂等
- AI Agent 在 description 中看到风险提示: `"[WRITE/MEDIUM] Add a new device"`
- 前端白名单页面按风险等级过滤，HIGH 工具默认不勾选或需二次确认
- 后续如果 OpenAPI 加 `x-dc3-ai` 扩展，MCP 侧直接读取即可

### AI 原生工具分层

OpenAPI 自动生成的工具只是原子能力，不应长期作为唯一工具形态。推荐分三层治理:

| 层级                    | 示例                                                                                                 | 作用                             |
|-----------------------|----------------------------------------------------------------------------------------------------|--------------------------------|
| 原子 API 工具             | `manager_device_add`, `data_command_history_call`                                                  | 从 OpenAPI 自动生成，覆盖面广，但语义偏技术接口   |
| 业务组合工具                | `device_create_with_profile`, `alarm_acknowledge`, `telemetry_latest`, `command_send_with_confirm` | 面向 AI Agent 的自然任务，封装多步调用、校验和确认 |
| MCP resources/prompts | `dc3://device/{id}`, `dc3://profile/{id}`, `巡检诊断 prompt`                                           | 给模型提供稳定上下文和标准操作流程，减少盲目调用工具     |

首版可以先落地原子 API 工具，但默认只启用低风险读工具；写工具和影响物理世界的工具优先沉淀为业务组合工具。

### tools/list 动态过滤

AI Agent 调用 `tools/list` 时，MCP Server 执行多重过滤:

```
全量工具目录 (OpenAPI + dc3_api + dc3_resource)
         │
         ▼  ① 主体权限码过滤
PermissionProvider.listPermissionCodes(tenantId, principalId)
→ Set<"dc3-center-manager:device:add", "dc3-center-auth:user:list", ...>
         │
         ▼  ② MCP 白名单过滤
dc3_mcp_connection_tool WHERE connection_id = ? AND enable_flag = 0
→ Set<"dc3-center-manager:POST:/device/add", "dc3-center-auth:POST:/user/list", ...>
         │
         ▼  ③ 风险策略过滤
HIGH 风险工具默认不返回，除非配置显式启用并满足确认策略
         │
         ▼  ④ 交集
返回该主体可见的工具列表
```

**关键约束**: 白名单不能突破角色权限。即使主体在白名单中勾选了 `device:add`，如果主体的所有角色都没有
`dc3-center-manager:device:add` 权限码，该工具也不会出现。

### 不同主体的实际效果

```
主体 A (管理员用户, 多角色: Administrator + Operator):
  权限码: {"*", ...}                        ← 通配符，全部权限
  MCP 白名单: 勾选了 80 个工具
  tools/list 返回: 80 个工具

主体 B (观察者用户, 单角色: Viewer):
  权限码: {"dc3-center-manager:device:list",
           "dc3-center-manager:device:get_by_id",
           "dc3-center-data:point_value:latest", ...}  ← 只有读权限
  MCP 白名单: 勾选了 80 个工具
  tools/list 返回: ~30 个工具 (只有读权限覆盖到的)
  即使白名单勾选了 device:add，角色无该权限码，工具不出现
```

## tools/call 调用链路

### 认证流程

```
1. 管理员或授权主体在设置页面注册 OAuth client，并创建 MCP OAuth 连接

2. AI Agent 连接:
   POST https://gateway:8000/mcp
   Authorization: Bearer <oauth-access-token>
   Content-Type: application/json
   {"jsonrpc":"2.0","method":"tools/list","id":1}

3. McpAuthWebFilter:
   a. 验证 OAuth access token 签名、issuer、audience、过期时间
   b. 校验 scope 包含 mcp:tools:list 或 mcp:tools:call
   c. 从 claims 获取 tenant_id, sub(principal_id), principal_type, client_id, mcp_connection_id
   d. 查询 dc3_mcp_connection，检查 enable_flag、expire_time、revoke_time
   e. PermissionProvider.listPermissionCodes(tenantId, principalId)
   f. 构建 McpAuthContext 存入 Reactor Context

4. tools/list handler:
   a. 从 Reactor Context 取 McpAuthContext
   b. 用 permissionCodes 过滤全局工具目录
   c. 用 dc3_mcp_connection_tool 白名单进一步过滤
   d. 应用风险策略和分页 cursor
   e. 返回可见工具列表

5. tools/call handler:
   a. 重新验证 OAuth token 有效性、scope、工具在主体可见范围内
   b. 重新校验 RBAC、白名单、风险策略、数据域和确认策略
   c. 构造 HTTP 请求到对应后端服务 (内网直连)
   d. 注入 X-Auth-Principal (HMAC 签名) 认证 headers
   e. 发起 WebClient 请求，记录审计并返回结果
```

### tools/call 认证转发策略

MCP 工具调用需要转发到后端服务。MCP Server 使用 HMAC 签名直接构造 `X-Auth-Principal` header，通过内网直接请求后端服务（不再走
Gateway 路由）:

```
McpToolCallHandler
  → 构造 PrincipalHeader JSON {principalId, principalType, tenantId, clientId, connectionId}
  → HmacAuthSigner.sign(principalJson) → X-Auth-Sign
  → 直接请求后端服务 (绕过 Gateway 路由，避免 X-Auth-Token 校验)
  → 后端 GatewayJwtConverter 验证 HMAC 签名 → 通过
```

### 高风险工具确认策略

`tools/list` 可以隐藏未启用的高风险工具，但一旦授权主体或管理员显式启用 HIGH 工具，`tools/call` 仍必须做运行时确认:

| 风险等级   | 默认可见 | 调用要求                                |
|--------|------|-------------------------------------|
| LOW    | 是    | RBAC + 白名单 + 租户校验                   |
| MEDIUM | 可配置  | RBAC + 白名单 + 租户校验，建议支持确认            |
| HIGH   | 否    | RBAC + 白名单 + 租户校验 + 二次确认 + 幂等键 + 审计 |

二次确认可以通过两种方式实现:

1. **Agent 客户端确认**: 在工具 description 和 annotations 中声明风险，由支持 MCP elicitation/confirmation 的客户端发起用户确认。
2. **平台确认工具**: 对关键操作拆成两步，先调用 `prepare_*` 返回 `confirm_id` 和影响摘要，再调用 `commit_*` 执行。MCP
   Server 校验 `confirm_id` 未过期、参数摘要一致、调用人一致。

设备指令下发、删除、批量导入、权限变更、模型配置变更等工具必须走第二种平台确认，不能只依赖客户端 UI。

### 完整调用链路

```
AI Agent
  │  MCP 协议 (JSON-RPC over Streamable HTTP)
  │  POST /mcp
  │  Authorization: Bearer <oauth-access-token>
  ▼
Gateway MCP Server
  │  解析 tools/call 请求
  │  → 工具名: "manager_device_add"
  │  → 参数: {"deviceName":"sensor-01", "driverId":100, "profileId":50}
  │
  │  查工具目录:
  │  → targetServiceUrl: http://dc3-center-manager:8400
  │  → apiPath: /device/add
  │  → httpMethod: POST
  │
  │  构造 HTTP 请求:
  │  POST http://dc3-center-manager:8400/manager/device/add
  │  X-Auth-Principal: {"principalId":100,"principalType":"USER","tenantId":1,...}
  │  X-Auth-Sign: <HMAC-SHA256>
  │  Content-Type: application/json
  │  Body: {"deviceName":"sensor-01","driverId":100,"profileId":50}
  │
  │  (内网 WebClient 调用，不走 Gateway 路由)
  ▼
dc3-center-manager:8400
  │  GatewayJwtConverter 验证 HMAC
  │  → 加载权限: dc3-center-manager:device:add ✓
  │  → @PreAuthorize("@perm.can('device','add')") ✓
  │  → 执行 Service → Manager → DAL → PostgreSQL
  │  → 返回 R<String>
  ▼
Gateway MCP Server
  │  封装为 CallToolResult:
  │  {"content":[{"type":"text","text":"{...}"}],"isError":false}
  ▼
AI Agent 收到结果
```

### HTTP vs gRPC 决策分析

> **结论: 选 HTTP，不选 gRPC。**

#### 事实: gRPC 仅覆盖只读查询子集

系统中有 15 个 gRPC Facade 接口、30 个 proto 文件，但 gRPC 只用于**跨服务只读查询**，不覆盖写操作:

| 实体     | REST Controller (完整 CRUD)                        | gRPC Facade (只读查询)                   |
|--------|--------------------------------------------------|--------------------------------------|
| Device | add, delete, update, get, list, import, export   | ListByPage, ListByProfileId, GetById |
| Driver | add, delete, update, get, list                   | ListByPage, GetById                  |
| Point  | add, delete, update, get, list, unit, statistics | ListByPage, GetById                  |
| User   | add, delete, update, get, list                   | GetById                              |
| Role   | add, delete, update, get, list, tree             | ❌ 无 gRPC                             |
| Alarm  | 全套 CRUD + 规则/通知/渠道                               | ❌ 无 gRPC                             |
| Menu   | add, delete, update, get, list, tree             | ❌ 无 gRPC                             |

gRPC 覆盖约 30% 的操作（读查询），70% 的操作（写 + 角色管理 + 告警 + 菜单）没有 gRPC。

#### 方案对比

| 维度        | 全走 HTTP                            | 全走 gRPC                 | 混合             |
|-----------|------------------------------------|-------------------------|----------------|
| 覆盖率       | ✅ 100%                             | ❌ ~30%                  | 需为 70% 补建 gRPC |
| 实现成本      | 低                                  | 极高（补 proto + 实现）        | 高（两套路径）        |
| 一致性       | ✅ 统一调用模式                           | ❌ 读写路径分裂                | ❌ 两套代码         |
| Schema 对齐 | ✅ OpenAPI → HTTP 天然对齐              | ❌ proto 和 REST 两套定义     | ❌ 两套           |
| 维护成本      | 低                                  | 极高（每次新增接口写 REST + gRPC） | 高              |
| 性能        | 略慢（JSON 序列化）                       | 快（protobuf 二进制）         | 混合             |
| 内网延迟影响    | 亚毫秒级，AI Agent 外部延迟 100-500ms，差距可忽略 | —                       | —              |

#### 选择 HTTP 的理由

1. **覆盖率**: MCP 需要暴露所有业务接口，gRPC 只能覆盖 30%
2. **Schema 对齐**: OpenAPI spec 描述的就是 HTTP 端点，apiPath + httpMethod 直接构造请求，零映射成本
3. **BFF 模式一致**: Gateway 对前端是 `HTTP→HTTP` 转发，对 AI Agent 是 `MCP→HTTP` 转发，模式统一
4. **性能无感**: 内网 HTTP 调用亚毫秒级，AI Agent 到 MCP Server 的外部延迟 100-500ms，gRPC 的性能优势无法体现
5. **渐进式**: 后续如有性能瓶颈，可针对高频读操作单独走 gRPC，不影响整体架构

## 工具目录动态刷新机制

Gateway 是常驻进程，但各中心服务会更新、重启、增删接口。需要保证 MCP 工具目录与服务端保持同步。

### 三种机制组合

**机制 1: 定时刷新（兜底）**

```yaml
dc3:
  mcp:
    server:
      refresh-interval: 300  # 每 5 分钟全量刷新一次
```

Gateway 定时重新拉取四个服务的 OpenAPI spec 和权限码，对比上一版工具目录计算差异（新增/修改/删除/schema_hash 变化），更新
`dc3_mcp_tool_catalog`。

**机制 2: 事件驱动（实时）**

利用已有 RabbitMQ 基础设施。各中心服务在 `ApplicationReadyEvent` 时发布消息:

```
服务启动/重启 → ApplicationReadyEvent → 发布 "mcp.tools.refresh" 消息到 RabbitMQ
接口变更（热部署） → 发布 "mcp.tools.refresh" 消息
```

Gateway 监听队列，收到消息后立即刷新对应服务的工具列表:

```java
@Component
@RabbitListener(queues = "dc3.mcp.tools.refresh")
public class McpToolRefreshListener {
    @RabbitHandler
    public void onRefresh(String serviceName) {
        toolAggregator.refreshService(serviceName);
    }
}
```

**机制 3: MCP 协议内置通知**

MCP 协议有 `notifications/tools/list_changed` 机制。当工具目录更新后，Gateway 的 MCP Server 主动通知已连接的 AI Agent:

```json
{"jsonrpc":"2.0","method":"notifications/tools/list_changed"}
```

AI Agent 收到后会重新调用 `tools/list` 获取最新工具列表。

### 刷新效果

| 场景        | 刷新方式                             | 延迟                    |
|-----------|----------------------------------|-----------------------|
| 服务重启      | RabbitMQ 事件 → 即时刷新               | 秒级                    |
| 接口新增/修改   | 定时刷新兜底                           | 最多 5 分钟               |
| 权限码变更     | `dc3_resource` 查询实时生效            | 即时（每次 tools/list 重新查） |
| MCP 白名单变更 | `dc3_mcp_connection_tool` 查询实时生效 | 即时                    |

> **注意**: 工具目录（OpenAPI spec 聚合结果）的刷新间隔决定了"接口变更后多久对 AI Agent 可见"，但权限码和白名单的过滤是每次
`tools/list` 实时查询的，不受刷新间隔影响。`schema_hash` 变化时不直接删除连接白名单，而是将工具标记为 `schema_changed`
> ，由前端提示授权主体重新确认。

## 调用观测

### 现有可观测性状态

| 能力                   | 状态        | 说明                                          |
|----------------------|-----------|---------------------------------------------|
| 结构化日志 (logback JSON) | ✅ 已有      | 所有服务输出 JSON 日志到文件，可被 ELK 采集                 |
| `@Logs` AOP 注解       | ⚠️ 存在但未使用 | `dc3-common-log` 模块提供了注解和切面，但无方法使用          |
| 审计日志表                | ❌ 不存在     | 没有 `dc3_audit_log` 等表                       |
| Prometheus 指标        | ✅ 基础设施就绪  | Micrometer + Prometheus endpoint 已暴露，无自定义指标 |
| Grafana 看板           | ✅ 基础设施就绪  | Docker Compose 可选栈中有 Prometheus + Grafana   |
| 分布式追踪                | ⚠️ 未配置    | Brave 桥在 classpath 但无后端                     |
| Elastic APM          | ⚠️ 存在但禁用  | `APM_AGENT_ENABLE=false`                    |

### MCP 调用观测 (随方案实现)

**结构化日志**:

```java
// McpToolCallHandler 中记录每次调用
log.info("MCP tool call: traceId={}, connectionId={}, clientId={}, toolId={}, principal={}, tenant={}, risk={}, duration={}ms, status={}",
    traceId, connectionId, clientId, toolId, principalId, tenantId, riskLevel, duration, "success/error");
```

**最小审计日志**:

首版新增 `dc3_mcp_audit_log` 或复用后续统一审计表，至少记录:

| 字段                                                                          | 说明                         |
|-----------------------------------------------------------------------------|----------------------------|
| `trace_id`                                                                  | 一次 MCP 调用链路 ID             |
| `tenant_id`, `principal_id`, `principal_type`, `client_id`, `connection_id` | 调用主体、OAuth client 和 MCP 连接 |
| `tool_id`, `tool_name`, `permission_code`                                   | 被调用工具和权限码                  |
| `risk_level`, `confirm_id`, `idempotency_key`                               | 风险、确认和幂等信息                 |
| `argument_digest`                                                           | 参数摘要或脱敏 JSON，不默认记录完整敏感参数   |
| `status`, `error_code`, `duration_ms`                                       | 执行结果                       |
| `client_name`, `client_version`, `remote_ip`                                | MCP 客户端信息                  |

高风险工具必须有审计记录；低风险读工具也建议记录摘要，便于排查 OAuth client 滥用和异常自动化调用。

**自定义 Prometheus 指标**:

```java
// MCP 调用计数器
Counter.builder("mcp.tool.calls")
    .tag("tool", toolName).tag("category", category).tag("status", "success/error")
    .register(registry).increment();

// MCP 调用耗时
Timer.builder("mcp.tool.duration")
    .tag("tool", toolName).register(registry)
    .record(duration, TimeUnit.MILLISECONDS);
```

这些指标可在 Grafana 中展示每个工具的调用量、成功率、响应时间。

## 后端模块结构

```
dc3-common/
└── dc3-common-mcp/                    # 新增 MCP Server 模块
    └── src/main/java/io/github/pnoker/common/mcp/
        ├── config/
        │   ├── McpServerAutoConfiguration.java
        │   └── McpServerProperties.java
        ├── auth/
        │   ├── McpAuthWebFilter.java
        │   └── McpAuthContext.java
        ├── tool/
        │   ├── McpToolAggregator.java
        │   ├── McpToolRegistry.java
        │   ├── McpToolCatalogService.java
        │   ├── McpDynamicToolProvider.java
        │   └── McpToolCallHandler.java
        ├── audit/
        │   └── McpAuditLogService.java
        ├── entity/
        │   ├── McpConnectionDO.java / McpConnectionBO.java / McpConnectionVO.java
        │   ├── McpToolCatalogDO.java / McpToolCatalogBO.java / McpToolCatalogVO.java
        │   └── McpConnectionToolDO.java / McpConnectionToolBO.java / McpConnectionToolVO.java
        └── service/
            ├── McpConnectionService.java
            ├── McpConnectionToolService.java
            └── McpInternalDocsClient.java

dc3-center-auth/
└── oauth/                              # 新增 OAuth Authorization Server 能力
    ├── client/
    ├── authorization/
    ├── consent/
    ├── jwk/
    └── discovery/
```

`dc3-gateway` 的 pom.xml 同时依赖 `dc3-common-gateway` 和 `dc3-common-mcp`。

## 需要变更的现有文件

### 删除 ServiceMcpToolsController

| 文件                                                               | 操作 | 说明                                      |
|------------------------------------------------------------------|----|-----------------------------------------|
| `dc3-common-web/.../controller/ServiceMcpToolsController.java`   | 删除 | 各服务不再需要独立的工具发现端点                        |
| `dc3-common-web/.../config/WebFluxSecurityConfig.java`           | 移除 | 删除 `/mcp_tools` 的 `permitAll()` 规则      |
| `dc3-common-resource-registrar/.../scan/ApiEndpointScanner.java` | 移除 | 删除 `DEFAULT_EXCLUDES` 中的 `"/mcp_tools"` |

### 生产环境 api-docs 安全

不能在生产环境简单把 `/v3/api-docs/**` `permitAll()` 放开。OpenAPI schema 不包含业务数据，但会暴露接口路径、参数结构、权限语义和内部模块边界，对攻击者有明显辅助价值。

推荐采用 **内部可读、外部不可见** 的策略:

| 位置           | 生产策略                                                           |
|--------------|----------------------------------------------------------------|
| 各中心服务        | `springdoc.api-docs.enabled: true`，仅允许 Gateway/MCP 聚合器从内网读取    |
| Gateway 对外路由 | 不暴露 `/v3/api-docs/{svc}` 聚合路由，或仅管理员鉴权后可访问                      |
| Swagger UI   | 生产默认关闭，仅 dev/test 开启                                           |
| MCP 聚合器      | 使用服务发现或内网地址直连中心服务 `/auth/v3/api-docs`、`/manager/v3/api-docs` 等 |

权限实现分三层，建议全部落地:

1. **网络层**: 中心服务端口不对公网开放；Kubernetes 用 NetworkPolicy / Ingress 规则限制只有 Gateway
   namespace/serviceAccount 可访问中心服务 api-docs；Docker/裸机部署用安全组或反向代理只暴露 Gateway。
2. **路由层**: `application-pro.yml` 中 Gateway 不注册 `/v3/api-docs/{svc}` 外部聚合路由；如确需给管理员查看，走正常登录态和
   `@perm.can('api_docs','read')` 管理权限，不再 `permitAll()`。
3. **应用层**: 各中心服务把 `/v3/api-docs/**` 从无条件 `permitAll()` 改成环境敏感访问控制:

```java
.pathMatchers("/v3/api-docs/**", "/v3/api-docs.yaml")
    .access(internalDocsAccessManager)
```

`internalDocsAccessManager` 的规则:

- `dev/test` 且 `dc3.docs.public-enabled=true` 时允许匿名访问，方便本地 Swagger 调试。
- `pre/pro` 默认拒绝匿名访问。
- Gateway MCP 聚合器访问时携带内部调用签名头，例如 `X-Internal-Caller: dc3-gateway`、`X-Internal-Timestamp`、
  `X-Internal-Nonce`、`X-Internal-Sign`。
- 中心服务用与 HMAC 同源但用途隔离的内部 secret 验签，并校验时间窗口和 nonce 防重放。
- 可选增强: 使用 mTLS 或服务网格身份代替内部签名。

管理员查看 API 文档的权限路径单独处理:

1. 浏览器不能直接访问中心服务 `/v3/api-docs/**`。
2. 如确需生产查看，前端只访问 Gateway 管理路由，例如 `/api/v3/auth/api-docs/{service}`。
3. Gateway 管理路由必须要求登录态和权限码，例如 `dc3-center-auth:api_docs:read`。
4. Gateway 通过内部签名或 mTLS 向中心服务拉取原始 OpenAPI，再返回给管理员。
5. 每次查看记录审计: `principal_id`、`tenant_id`、`service_name`、`remote_ip`、`trace_id`。

这条路径和 MCP 聚合器路径都不能使用 `permitAll()`。区别只是调用主体不同: MCP 聚合器是受信任服务主体，管理员查看是人类
`USER` principal。

配置建议:

```yaml
# center services: application-pro.yml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: false

dc3:
  docs:
    public-enabled: false
    internal-signature-enabled: true
    internal-signature-secret-ref: ${DOCS_INTERNAL_SECRET_REF}

# gateway: application-pro.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

> 结论: MCP Server 可以在生产读取 api-docs，但 api-docs 不能对外公开放行。`permitAll()`
> 只适合本地和测试环境；生产必须至少做到"外部路由不可达 + 内部签名校验 + 管理员查看走 RBAC"，更高等级环境用
> mTLS/服务网格身份。pre/pro 环境如果开启 `springdoc.api-docs.enabled=true` 但没有内部签名或 mTLS 配置，中心服务应启动失败。

## 前端设置页面

### 菜单注册

在 Settings 导航中新增 MCP 服务菜单，位于 About 之前:

| 层            | 文件                              | 变更                                                                                 |
|--------------|---------------------------------|------------------------------------------------------------------------------------|
| 后端 seed data | `iot-dc3-auth.sql`              | `dc3_menu` 表新增 `settingsMcpServer` 记录                                              |
| 路由           | `src/config/router/settings.ts` | 新增 `settingsMcpServer` 路由                                                          |
| 导航           | `src/config/settingsNav.ts`     | `SETTINGS_TITLE_KEYS` / `SETTINGS_FALLBACK_SIDEBAR` / `SETTINGS_FALLBACK_ICON` 加条目 |
| 英文 i18n      | `src/config/i18n/locales/en.ts` | `nav.settingsMcpServer: 'MCP Service'`                                             |
| 中文 i18n      | `src/config/i18n/locales/zh.ts` | `nav.settingsMcpServer: 'MCP 服务'`                                                  |
| API 常量       | `src/config/constant/api.ts`    | 新增 `API_MCP_BASE = 'api/v3/auth/mcp'`                                              |

### OAuth Client 与 MCP 连接列表页

- 展示 OAuth client 列表 (client_id、client 类型、redirect URI、授权类型、状态)
- 展示当前 Principal 的 MCP 连接 (连接名称、主体类型、client_id、已启用工具数、状态、最近使用时间)
- 操作: 注册 client、选择服务账号、跳转服务账号管理、编辑 redirect URI、轮换 client secret、撤销授权、管理工具、复制 OAuth 配置信息

> 服务账号创建、负责人、用途、过期和角色绑定由 [统一主体与服务账号设计方案](identity-principal-service-account.md)
> 的身份前端负责。MCP 页面只消费已治理的 `SERVICE_ACCOUNT` principal。

### MCP 工具管理页

- 展示全局工具列表 (按 auth/manager/data/agentic 分类)
- 工具条目显示: 名称、描述、权限码、风险等级标签 (LOW/MEDIUM/HIGH)、MCP annotations、schema 变化状态
- 根据主体权限码过滤: 无权限的工具置灰不可勾选
- 有权限的工具可以通过勾选加入/移出白名单
- HIGH 风险工具默认不勾选或需二次确认

### 连接信息弹窗

- 展示 MCP Server URL、authorization server metadata、client_id、redirect URI、scopes
- 一键复制
- 提供常见 AI Agent (Claude Desktop / Cursor / VS Code) 的配置片段

## 实施步骤

### Phase 0: 前置身份底座

先完成 [统一主体与服务账号设计方案](identity-principal-service-account.md) 的全量重建。MCP 实施默认 `dc3_principal`、
`dc3_tenant_membership`、`dc3_service_account`、`dc3_role_principal_bind` 和
`PermissionProvider.listPermissionCodes(tenantId, principalId)` 已经可用，且旧身份链路不再进入运行时。

### Phase 1: MCP/OAuth 基础设施 (2-3 天)

| 步骤  | 任务                                                                                              | 产出                                                                             |
|-----|-------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| 1.1 | 创建 `dc3-common-mcp` 模块骨架                                                                        | pom.xml, 包结构                                                                   |
| 1.2 | 数据库: 新增 OAuth 标准表 + `dc3_mcp_connection` + `dc3_mcp_tool_catalog` + `dc3_mcp_connection_tool` 表 | seed SQL                                                                       |
| 1.3 | 后端: MCP DO/BO/VO 实体 + MapStruct 转换器                                                             | entity 包                                                                       |
| 1.4 | 后端: MCP Service/Manager 层 CRUD                                                                  | 基础增删改查 API                                                                     |
| 1.5 | 删除 `ServiceMcpToolsController` 及相关引用                                                            | 清理遗留代码                                                                         |
| 1.6 | 生产 api-docs 安全改造                                                                                | 中心服务内部可读，Gateway 外部不公开聚合 docs                                                  |
| 1.7 | OAuth Authorization Server 基础设施                                                                 | registered client、authorization、consent、JWK、authorize/token/revoke/register 端点 |
| 1.8 | OAuth Resource Server 基础设施                                                                      | Protected Resource Metadata、Bearer challenge、JWT/introspection 校验              |

### Phase 2: MCP Server 核心 (2-3 天)

| 步骤  | 任务                                                                | 产出                |
|-----|-------------------------------------------------------------------|-------------------|
| 2.1 | `McpServerAutoConfiguration` + 配置属性                               | 自动配置类             |
| 2.2 | `McpToolAggregator`: 从 OpenAPI spec + dc3_api/dc3_resource 生成工具目录 | 启动时工具收集           |
| 2.3 | `McpAuthWebFilter`: OAuth access token 鉴权                         | 统一 McpAuthContext |
| 2.4 | `McpDynamicToolProvider`: tools/list 动态过滤                         | 权限感知的工具列表         |
| 2.5 | `McpToolCallHandler`: tools/call → HTTP 转发                        | 工具调用处理            |

### Phase 3: Gateway 集成 (1-2 天)

| 步骤  | 任务                                                  | 产出     |
|-----|-----------------------------------------------------|--------|
| 3.1 | `dc3-gateway/pom.xml` 引入 `dc3-common-mcp` + starter | 依赖     |
| 3.2 | `application.yml` 添加 MCP/OAuth 配置                   | 配置文件   |
| 3.3 | 验证 `/mcp` 端点启动正常                                    | 端到端连通性 |
| 3.4 | HMAC 签名认证转发调通                                       | 工具调用成功 |

### Phase 4: 前端 (3-5 天)

| 步骤  | 任务                         | 产出                                                         |
|-----|----------------------------|------------------------------------------------------------|
| 4.1 | 路由/导航/i18n/API 常量          | MCP 菜单可见                                                   |
| 4.2 | OAuth client 管理            | 注册 client、redirect URI、scope、secret 轮换、撤销                  |
| 4.3 | MCP connection 管理          | 选择 Principal/服务账号、租户、授权类型、启停和过期                            |
| 4.4 | `McpServerTools.vue` 工具管理页 | RBAC 过滤、白名单勾选、风险确认、schema 变化提示                             |
| 4.5 | 连接信息弹窗                     | OAuth metadata、client_id、scopes、Agent 配置片段；不展示平台 Token Key |

### Phase 5: 联调测试 (1-2 天)

| 步骤  | 任务                           | 产出          |
|-----|------------------------------|-------------|
| 5.1 | MCP Inspector 连接验证           | 协议符合度       |
| 5.2 | Claude Desktop / Cursor 连接测试 | 真实 Agent 场景 |
| 5.3 | 多租户隔离验证                      | 安全性         |
| 5.4 | 权限过滤边界测试                     | 准确性         |

## 讨论记录

方案评审过程中讨论了以下问题，逐一记录问题和结论。

### D1: dc3_mcp_connection_tool 白名单表的作用

**问题**: `dc3_mcp_connection_tool` 这张表是做什么的？为什么不直接用权限码控制？

**结论**: 白名单表解决的是「在有权访问的工具中进一步挑选愿意暴露的工具」这个需求。三层过滤的关系:

1. **dc3_resource + dc3_role_resource_bind** — 角色决定的权限上限（比如管理员有 120 个权限码）
2. **dc3_mcp_tool_catalog** — Gateway 聚合出的稳定工具目录，保存 tool_id、api_code、permission_code、schema_hash、风险等级和
   MCP annotations
3. **dc3_mcp_connection_tool** — 授权主体在设置页面勾选了其中 50 个愿意暴露给这个 OAuth 连接
4. **最终 tools/list 返回** — ① ∩ ③ ∩ 风险策略 = 可见工具

白名单不能突破角色权限（即使勾选了某工具，角色无权限码则不出现）。白名单绑定 `tool_id`，不绑定工具名称。它的价值: (1) 精细化控制每个
OAuth 连接暴露的工具子集; (2) OAuth client 被滥用时限制攻击面; (3) schema 变化后可提示用户重新确认。

### D2: MCP 连接关联 principal_id，而不是 role_id

**问题**: MCP 连接是否应该绑定某个角色？系统支持一个主体绑定多个角色，权限在所有角色间取并集。怎么处理？

**结论**: 不绑定单个 `role_id`。MCP 连接保存 `client_id` + `principal_id` + `principal_type` + `tenant_id` +
`connection_id`，权限仍按该主体在该租户下的全部角色聚合。

原因: 标准模型中的 `dc3_role_principal_bind` 是多对多关系，
`PermissionProvider.listPermissionCodes(tenantId, principalId)` 自动聚合主体所有角色的权限码。如果 MCP
连接只关联一个角色，就会丢失其他角色的权限。`principal_type=USER` 时主体资料来自 `dc3_user`；
`principal_type=SERVICE_ACCOUNT` 时主体资料来自 `dc3_service_account`。MCP 工具可见范围 = 主体全部角色权限 ∩
白名单，与系统中的实际权限完全一致。

### D3: OpenAPI 接口描述是否有 Harness 级别的元数据

**问题**: OpenAPI spec 能否告诉我们哪些接口能做什么不能做什么、风险等级、读写分类？

**现状**: 当前 OpenAPI 注解只有基础描述（`@Operation(summary, description)` 覆盖率 100%），没有读写分类、风险等级、废弃标记、操作后果、自定义扩展等
Harness 级别元数据。

**结论**: 首版不强制改造全部 OpenAPI 注解，在 Gateway 侧聚合工具时基于已有数据自动推导，并落入 `dc3_mcp_tool_catalog`
。后续可通过 OpenAPI `x-dc3-ai` 扩展或后台工具目录覆盖:

| 维度              | 推导来源                                     | 规则                                                               |
|-----------------|------------------------------------------|------------------------------------------------------------------|
| readOnlyHint    | HTTP 方法 + API 路径 + `@PreAuthorize` scope | GET → true; POST 中 `/list`、`/get_*` → true; 其余 POST → false      |
| destructiveHint | scope + 业务域                              | delete/reset/批量导入/设备指令 → true                                    |
| idempotentHint  | HTTP 方法 + scope                          | GET → true; POST /list → true; POST /add → false                 |
| 风险等级            | scope 语义                                 | delete → HIGH; add/update → MEDIUM; get/list → LOW; 设备写命令 → HIGH |
| 确认策略            | 风险等级 + 业务域                               | HIGH 默认要求二次确认和审计                                                 |

推导出的元数据用于: MCP annotations、AI Agent description 风险提示、前端白名单按风险过滤、后续 OpenAPI
扩展对接。详见 [工具元数据与 AI 语义治理](#工具元数据与-ai-语义治理)。

### D4: tools/call 走 HTTP 还是 gRPC

**问题**: 如果 tool call 直接走 HTTP 请求后端服务，本质类似 RPC 了，那为什么不走 gRPC？

**结论**: 选 HTTP，不选 gRPC。

关键事实: 系统 gRPC 只覆盖约 30% 的操作（跨服务只读查询），70% 的操作（写 + 角色管理 + 告警 + 菜单等）没有 gRPC。选择理由: (1)
覆盖率 — MCP 需暴露所有接口，gRPC 只能覆盖 30%; (2) Schema 对齐 — OpenAPI 描述的就是 HTTP 端点，零映射成本; (3) 性能无感 —
内网 HTTP 亚毫秒级，AI Agent 外部延迟 100-500ms; (4) 维护成本 — 全走 HTTP
统一模式，混合路径维护成本高。详见 [HTTP vs gRPC 决策分析](#http-vs-grpc-决策分析)。

### D5: 工具目录动态刷新机制

**问题**: Gateway 是常驻的，其他服务会更新/修改/删除接口，如何实时同步？

**结论**: 三种机制组合:

| 场景        | 刷新方式                             | 延迟      |
|-----------|----------------------------------|---------|
| 服务重启      | RabbitMQ 事件 → 即时刷新               | 秒级      |
| 接口新增/修改   | 定时刷新兜底 (每 5 分钟)                  | 最多 5 分钟 |
| 权限码变更     | `dc3_resource` 查询实时生效            | 即时      |
| MCP 白名单变更 | `dc3_mcp_connection_tool` 查询实时生效 | 即时      |

工具目录更新后通过 MCP 协议的 `notifications/tools/list_changed` 通知已连接的 AI Agent
重新拉取工具列表。详见 [工具目录动态刷新机制](#工具目录动态刷新机制)。

### D6: 多角色权限聚合 + 调用观测

**问题**: 主体可以配置多个角色吗？权限是否聚合？接口调用如何观测？

**结论**:

**多角色**: 支持。`dc3_role_principal_bind` 是多对多，一个 Principal 可绑定多个角色。权限在所有角色间取并集（
`RoleResourceBindServiceImpl.listResourceByPrincipalId` 4 步聚合），无"拒绝"机制，纯加法，通配符 `*` 满足所有检查。

**调用观测**: 当前系统可观测性基础设施部分就绪（logback JSON 日志、Prometheus/Grafana、ELK 可选栈），但缺少 MCP 级审计日志。MCP
首版必须实现最小审计:
tenant_id、principal_id、principal_type、client_id、connection_id、tool_id、permission_code、风险等级、参数摘要、调用结果、耗时、trace_id。详见 [调用观测](#调用观测)。

## 已决策问题

以下问题已按评审结论纳入实施计划。

### Q1: HMAC 签名依赖

**结论**: 生产环境必须启用 HMAC。MCP Server 的 `tools/call` 转发依赖 HMAC 签名（`HmacAuthSigner`）构造 `X-Auth-Principal`
header，后端服务必须验证签名后才信任主体身份。pre/pro 环境如果缺少 `dc3.auth.hmac.secret` 或 `AUTH_HMAC_SECRET`，应用应
fail-fast。

### Q2: OpenAPI spec 生产安全

**结论**: api-docs 允许生产内部开启，但不能对外 `permitAll()`。中心服务仅允许 Gateway/MCP 聚合器通过内网和内部签名访问；Gateway
生产不暴露 `/v3/api-docs/{svc}` 聚合路由。详见 [生产环境 api-docs 安全](#生产环境-api-docs-安全)。

### Q3: 审计日志

**结论**: 审计必须随首版落地。最小审计记录谁、在哪个租户、通过哪个 OAuth client 和 MCP 连接、调用了哪个工具、参数摘要、是否成功、耗时和
trace_id；高风险工具记录确认信息和幂等键。完整结果体默认不落库，只记录摘要或错误码，避免泄露敏感数据。

### Q4: gRPC 未来演进

**结论**: 首版只走 HTTP，但在代码中保留 `ToolInvoker` 抽象。后续如高频只读工具存在明确瓶颈，可针对单个业务组合工具增加 gRPC
invoker，不引入全局混合路径。

### Q5: 多用户共享同一 MCP 授权

**结论**: 不支持多人共享同一个人类用户授权。团队场景使用 OAuth Client Credentials +
服务账号，并绑定最小角色权限、负责人、过期时间和审计标签。这样不会破坏审计归因，也能满足团队级自动化。

### Q6: 是否现在建设 OAuth

**结论**: 一步到位建设完整 OAuth 2.1。`dc3-center-auth` 提供 Authorization Server，`dc3-gateway` 提供 MCP Resource Server；
`/mcp` 只接受标准 OAuth access token，不保留 PAT 或 Token Key 路径。详见 [OAuth 建设策略](#oauth-建设策略)。

## 风险与缓解

| 风险                                    | 概率 | 影响    | 缓解                                                                                                  |
|---------------------------------------|----|-------|-----------------------------------------------------------------------------------------------------|
| Spring AI 2.0.0-M8 的 MCP Starter 不够稳定 | 中  | 阻断    | 提前做 PoC 验证；如果 M8 有问题，降级到 MCP Java SDK 手动构建                                                          |
| OpenAPI spec 中部分端点的 `@Schema` 注解不完整   | 低  | 描述缺失  | 逐步补全；即使缺少 description，工具名 + 路径仍有参考价值                                                                |
| 工具数量过多 (100+) 导致 `tools/list` 响应慢     | 低  | 性能问题  | MCP 协议支持分页 cursor；权限过滤结果可缓存                                                                         |
| OAuth 标准实现复杂、客户端差异大                   | 中  | 联调延期  | 先以 MCP Inspector、Claude Desktop、Cursor 做最小互操作矩阵；严格按 metadata、PKCE、401 challenge 和 Bearer error 响应实现 |
| OAuth client secret 泄露                | 中  | 安全风险  | client secret 只展示一次，数据库保存 hash；支持轮换、撤销、过期时间、最小 scope 和审计                                            |
| HMAC 未启用时 tools/call 无法转发             | 中  | 功能不可用 | pre/pro 强制 HMAC 配置，缺失时 fail-fast                                                                    |
| `/v3/api-docs/**` 对外暴露                | 中  | 安全风险  | Gateway 生产不暴露聚合 docs；中心服务只允许内网 + 内部签名访问                                                             |
| 高风险工具被模型误调用                           | 中  | 业务风险  | HIGH 默认不启用；启用后要求二次确认、幂等键、审计和限流                                                                      |

## 相关资料

- [统一主体与服务账号设计方案](identity-principal-service-account.md)
- [MCP Java SDK (GitHub)](https://github.com/modelcontextprotocol/java-sdk)
- [Spring AI MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [MCP Authorization Specification](https://modelcontextprotocol.io/specification/latest/basic/authorization)
- [MCP Transports Specification](https://modelcontextprotocol.io/specification/latest/basic/transports)
- [MCP Tools Specification](https://modelcontextprotocol.io/specification/latest/server/tools)
- [Spring AI MCP Security](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-security.html)
- [OAuth 2.0 Security Best Current Practice (RFC 9700)](https://datatracker.ietf.org/doc/html/rfc9700)
- [OAuth 2.0 Resource Indicators (RFC 8707)](https://datatracker.ietf.org/doc/html/rfc8707)
- [设备与驱动状态超时管理说明](device-driver-timeout.md)
- [事件上报方案](event-report.md)
