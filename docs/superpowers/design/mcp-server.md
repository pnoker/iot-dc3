# Gateway MCP 服务设计方案

> 状态: 方案评审中，存在待确认问题。本文记录在 `dc3-gateway` 上构建 MCP (Model Context Protocol) 服务的完整设计，包括技术选型分析、已确认决策和待讨论问题。

## 背景

IoT DC3 的 auth、manager、data、agentic 四个中心服务已通过 `dc3-gateway` 聚合了完整的 OpenAPI 接口。为了让 AI Agent (Claude Desktop、Cursor、GPT 等) 能以标准化方式调用这些接口，需要在 Gateway 上构建一个 MCP 服务，将 OpenAPI 接口转换为 MCP 工具暴露出去。

MCP 服务需要满足以下要求:

1. **复用现有 OpenAPI** — 各中心服务已通过 Springdoc 生成了高质量的 OpenAPI spec，MCP 工具定义直接从 OpenAPI spec 构造，不需要各服务维护额外的工具描述。
2. **复用现有权限体系** — MCP 工具的可见性和调用权限与系统已有的租户、用户、角色、资源绑定体系完全一致，不新建独立的权限模型。
3. **多配置共存** — 用户可以创建多套 MCP 配置 (不同的 Token Key)，但共享同一个 MCP 服务地址，通过 Token Key 区分身份和权限。
4. **统一地址** — 所有 MCP 配置共用 `https://gateway:8000/mcp`，AI Agent 通过不同的 Bearer Token 连接不同的配置。

## 设计目标

- 在 Gateway 上新增 MCP Server 端点 (`/mcp`)，使用 Streamable HTTP (Stateless) 协议。
- Gateway 启动时从四个中心服务的 `/v3/api-docs` 拉取 OpenAPI spec，聚合为全局工具注册表。
- 从 `dc3_resource` 表获取每个 API 端点对应的权限码，建立工具 → 权限码映射。
- 用户在前端设置页面创建 MCP 配置并生成 Token Key，选择要暴露的工具子集。
- AI Agent 连接时通过 Bearer Token 鉴权，`tools/list` 返回该用户可见的工具列表。
- `tools/call` 时注入用户身份 Headers 转发到后端服务执行。

## 技术选型

| 组件 | 选择 | 理由 |
|---|---|---|
| MCP Server 框架 | Spring AI MCP Server WebFlux Starter | Spring 官方，与 MCP Java SDK 联合维护；WebFlux 与 Gateway 同栈 |
| 传输协议 | Streamable HTTP (Stateless) | 无状态模式匹配多 Token Key 鉴权，每次请求独立验证 |
| 编程模型 | ASYNC (Reactor) | 与 Gateway WebFlux 一致，非阻塞 |
| 工具 Schema 来源 | 各中心服务的 OpenAPI spec | 已有完整的 `@Schema`、`@Parameter`、`@Operation` 注解 |
| 权限码来源 | `dc3_resource` + `dc3_api` JOIN | 已有完整的 API 端点到权限码映射 |
| 认证方式 | 自定义 Bearer Token 拦截器 | 复用现有 gRPC Facade 认证体系 |
| 工具调用传输 | HTTP (WebClient) | 全覆盖，见 [HTTP vs gRPC 决策分析](#http-vs-grpc-决策分析) |

### 依赖版本兼容性

| 组件 | 要求 | 当前版本 | 状态 |
|---|---|---|---|
| Java | 17+ | 21 | ✅ |
| Spring Boot | 4.0.x | 4.0.6 | ✅ |
| Spring AI | 2.0.0+ | 2.0.0-M8 | ⚠️ 可用，建议后续升到正式版 |
| WebFlux | Netty 运行时 | Netty 4.2.10 | ✅ |

## 整体架构

```
                    ┌──────────────────────────┐
                    │   AI Agent (Claude/GPT)   │
                    └────────────┬─────────────┘
                                 │ MCP Streamable HTTP
                                 │ POST/GET /mcp
                                 │ Authorization: Bearer <token-key>
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
│   │  • Bearer token → dc3_mcp_server → tenant/user             │   │
│   │  • gRPC PermissionFacade → user permission codes            │   │
│   │  • Build McpAuthContext into Reactor Context                │   │
│   │                                                             │   │
│   │  Layer 3: McpAsyncServer + DynamicToolRegistry              │   │
│   │  • tools/list: filter by user permissions + whitelist       │   │
│   │  • tools/call: inject auth headers → HTTP forward           │   │
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

MCP 配置数据放在 `dc3_auth` schema 中，与用户、角色、资源同属权限域。遵循项目已有的数据库约定。

### dc3_mcp_server — MCP 服务配置表

```sql
-- Table structure for dc3_mcp_server
SET search_path TO dc3_auth;

CREATE TABLE dc3_mcp_server
(
    id              BIGINT PRIMARY KEY NOT NULL,                    -- 主键 ID
    server_name     TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 配置名称
    token_key       TEXT     DEFAULT ''::TEXT          NOT NULL,    -- MCP 访问令牌 (dc3mcp_ 前缀 + 32 位 hex)
    user_id         BIGINT   DEFAULT 0 NOT NULL,                    -- 关联用户 ID
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                    -- 所属租户 ID
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                    -- 启用标志, 0: 启用, 1: 禁用
    expire_time     TIMESTAMPTZ,                                    -- 过期时间 (可选, NULL 表示永不过期)
    server_ext      JSON     DEFAULT '{}'::JSON        NOT NULL,    -- 扩展信息
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 描述
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                    -- 创建人 ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 创建人名称
    create_time     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                    -- 操作人 ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 操作人名称
    operate_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 操作时间
    deleted         SMALLINT DEFAULT 0 NOT NULL,                    -- 逻辑删除标志, 0: 未删除, 1: 已删除
    CONSTRAINT chk_mcp_server_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_server_deleted CHECK (deleted IN (0, 1))
);

-- Token Key 全局唯一
CREATE UNIQUE INDEX idx_mcp_server_token_key_active_unique
    ON dc3_mcp_server (token_key) WHERE deleted = 0 AND token_key <> ''::TEXT;

-- 租户内名称唯一
CREATE UNIQUE INDEX idx_mcp_server_tenant_name_active_unique
    ON dc3_mcp_server (tenant_id, server_name) WHERE deleted = 0 AND server_name <> ''::TEXT;

-- 按用户查询
CREATE INDEX idx_mcp_server_user_id
    ON dc3_mcp_server (user_id) WHERE deleted = 0;

-- 按租户查询
CREATE INDEX idx_mcp_server_tenant_id
    ON dc3_mcp_server (tenant_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE ON dc3_mcp_server
    FOR EACH ROW EXECUTE FUNCTION update_operate_time();
```

> **设计说明**: 本表不包含 `role_id` 字段。系统支持一个用户绑定多个角色（通过 `dc3_role_user_bind` 多对多关系），权限在所有角色间取并集（详见 [权限聚合机制](#权限聚合机制)）。MCP 配置关联到 `user_id`，`tools/list` 时直接调用 `PermissionProvider.listPermissionCodes(tenantId, userId)` 获取该用户全部角色的聚合权限码。

### dc3_mcp_server_tool — MCP 工具白名单表

```sql
-- Table structure for dc3_mcp_server_tool
CREATE TABLE dc3_mcp_server_tool
(
    id              BIGINT PRIMARY KEY NOT NULL,                    -- 主键 ID
    server_id       BIGINT   DEFAULT 0 NOT NULL,                    -- 关联 MCP 配置 ID
    tool_name       TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 工具标识 (如 manager_device_add)
    tool_category   TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 工具分类 (auth/manager/data/agentic)
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                    -- 启用标志, 0: 启用, 1: 禁用
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 描述
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                    -- 创建人 ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 创建人名称
    create_time     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                    -- 操作人 ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 操作人名称
    operate_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 操作时间
    deleted         SMALLINT DEFAULT 0 NOT NULL,                    -- 逻辑删除标志, 0: 未删除, 1: 已删除
    CONSTRAINT chk_mcp_server_tool_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_server_tool_deleted CHECK (deleted IN (0, 1))
);

-- 同一配置下工具名唯一
CREATE UNIQUE INDEX idx_mcp_server_tool_server_tool_active_unique
    ON dc3_mcp_server_tool (server_id, tool_name) WHERE deleted = 0 AND tool_name <> ''::TEXT;

-- 按配置 ID 查询
CREATE INDEX idx_mcp_server_tool_server_id
    ON dc3_mcp_server_tool (server_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE ON dc3_mcp_server_tool
    FOR EACH ROW EXECUTE FUNCTION update_operate_time();
```

> **设计说明**: 白名单表的作用是让用户在"有权访问的工具"中进一步挑选"愿意暴露给某个 MCP 配置的工具"。它不替代权限控制——白名单不能突破角色权限。即使勾选了某工具，如果用户角色没有对应权限码，该工具也不会出现。白名单提供两个价值: (1) 精细化控制每个 MCP 配置暴露的工具子集; (2) Token Key 泄露时限制攻击面。

### 表关系

```
dc3_user ──1:N── dc3_mcp_server ──1:N── dc3_mcp_server_tool
    │                 │                         │
    │                 ├── token_key: Bearer token 值
    │                 ├── user_id: 决定身份和权限范围
    │                 │   (权限 = 该用户所有角色的并集)
    │                 └── tenant_id: 租户隔离
    │
    └── 一个用户可以创建多套 MCP 配置 (不同 token_key, 不同工具子集)
```

## 权限控制方案

### 核心原则: 复用现有权限体系，不新建

MCP 服务的权限控制完全复用系统已有的 `dc3_resource` / `dc3_role_resource_bind` / `dc3_role_user_bind` 链路，不做新的权限模型。

### 现有权限链路回顾

```
@PreAuthorize("@perm.can('device','add')")
  → ApiEndpointScanner 扫描 → dc3_api {api_name="device:add"}
  → ResourceRegistrySyncServiceImpl → dc3_resource {resource_code="dc3-center-manager:device:add"}
  → dc3_role_resource_bind (角色绑定资源)
  → dc3_role_user_bind (用户绑定角色)
  → PermissionProvider.listPermissionCodes(tenantId, userId) → Set<resource_code>
```

### 权限聚合机制

系统支持一个用户绑定多个角色（`dc3_role_user_bind` 是多对多关系），权限在所有角色间取并集:

```
用户 A 绑定了两个角色:
  角色 1 (设备管理员): device:add, device:update, device:delete, device:list
  角色 2 (数据查看者): point_value:latest, point_value:latest, alarm:list

最终权限码 = 角色1 ∪ 角色2 = 7 个权限码
```

聚合过程由 `RoleResourceBindServiceImpl.listResourceByUserId()` 完成:
1. `dc3_role_user_bind` WHERE user_id = ? → 得到所有 role_id
2. 过滤启用状态 + 租户范围 → 有效 role_id 列表
3. `dc3_role_resource_bind` WHERE role_id IN (...) → 所有 resource_id (去重)
4. `dc3_resource` WHERE id IN (...) → 所有 resource_code → 组成 `Set<String>`

没有"拒绝"机制，是纯加法。通配符 `*` 满足所有检查。MCP 直接调用 `PermissionProvider.listPermissionCodes(tenantId, userId)` 即可获得聚合后的权限码集合。

### dc3_api 与 dc3_resource 的关系

两张表通过 `dc3_resource.entity_id = dc3_api.id` 关联:

```
dc3_api                                              dc3_resource
───────────────────────────                          ──────────────────────────────
id: 1001                                             id: 2001
service_name: dc3-center-manager                     service_name: dc3-center-manager
api_name: device:add            ──────────────→      resource_code: dc3-center-manager:device:add
api_code: dc3-center-manager:POST:/device/add        entity_id: 1001 (→ dc3_api.id)
api_group: DeviceController                          resource_type_flag: API
```

`dc3_api` 没有直接存储权限码，但 `dc3_resource.resource_code` 就是权限码，通过 `entity_id` 一一关联。

### MCP 工具注册表构建

Gateway 启动时，`McpToolAggregator` 从两个数据源合并构建全局工具注册表:

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

**数据源 2: dc3_resource + dc3_api** — 提供每个 API 端点的权限码

```sql
SELECT a.api_code, a.api_name, a.service_name, a.api_group,
       r.resource_code
FROM   dc3_api a
JOIN   dc3_resource r ON r.entity_id = a.id
WHERE  a.deleted = 0
  AND  r.deleted = 0
  AND  a.service_name IN ('dc3-center-auth','dc3-center-manager',
                          'dc3-center-data','dc3-center-agentic');
```

**合并结果 — 完整工具注册表条目**:

```json
{
  "name": "manager_device_add",
  "description": "Add a new device",
  "inputSchema": { "properties": {...}, "required": [...] },
  "httpMethod": "POST",
  "apiPath": "/device/add",
  "category": "manager",
  "targetServiceUrl": "http://dc3-center-manager:8400",
  "permissionCode": "dc3-center-manager:device:add"
}
```

### 工具元数据自动推导

当前 OpenAPI 注解不包含读写分类、风险等级等 Harness 级别的元数据。现有注解情况:

| 有什么 | 覆盖率 | 缺什么 |
|---|---|---|
| `@Operation(summary, description)` | 302 个端点 100% | 无 `deprecated`、无 `security`、无 `extensions` |
| `@Tag(name, description)` | 55 个 Tag 100% | 无自定义扩展 |
| `@Parameter(description)` | 查询参数 100% | — |
| `@Schema(description, example)` | VO 字段 100% | — |

不改造 OpenAPI 注解（成本太高），而是在 Gateway 侧聚合工具时自动推导元数据:

```java
ToolMetadata meta = ToolMetadata.builder()
    .name(toolName)
    .description(operation.getSummary())
    .inputSchema(inputSchema)
    // 自动推导维度:
    .accessType(deriveAccessType(httpMethod, apiPath))    // READ / WRITE
    .riskLevel(deriveRiskLevel(scope))                    // LOW / MEDIUM / HIGH
    .idempotent(deriveIdempotent(httpMethod, scope))      // true / false
    .build();
```

推导规则:

| 维度 | 推导来源 | 规则 |
|---|---|---|
| **READ / WRITE** | HTTP 方法 + API 路径 + `@PreAuthorize` scope | `GET` 请求 → READ; `POST` 请求中 `/list`、`/get_*`、`/count` → READ; 其余 POST → WRITE |
| **风险等级** | `@PreAuthorize` scope 语义 | `delete` → HIGH (不可逆); `add`/`update` → MEDIUM; `get`/`list` → LOW; 特殊操作如 `point_command:write` → HIGH (影响物理设备) |
| **幂等性** | HTTP 方法 + scope | `GET` → true; `POST /list` → true; `POST /add` → false; `POST /delete` → true (删除后再删不影响) |

这些元数据用于:
- AI Agent 在 description 中看到风险提示: `"[WRITE/MEDIUM] Add a new device"`
- 前端白名单页面按风险等级过滤，HIGH 工具默认不勾选或需二次确认
- 后续如果 OpenAPI 加 `x-risk` 扩展，MCP 侧直接读取即可

### tools/list 动态过滤

AI Agent 调用 `tools/list` 时，MCP Server 执行三重过滤:

```
全量工具注册表 (OpenAPI + dc3_resource)
         │
         ▼  ① 用户权限码过滤
PermissionProvider.listPermissionCodes(tenantId, userId)
→ Set<"dc3-center-manager:device:add", "dc3-center-auth:user:list", ...>
         │
         ▼  ② MCP 白名单过滤
dc3_mcp_server_tool WHERE server_id = ? AND enable_flag = 0
→ Set<"manager_device_add", "auth_user_list", ...>
         │
         ▼  ③ 交集
返回该用户可见的工具列表
```

**关键约束**: 白名单不能突破角色权限。即使用户在白名单中勾选了 `device:add`，如果用户的所有角色都没有 `dc3-center-manager:device:add` 权限码，该工具也不会出现。

### 不同用户的实际效果

```
用户 A (管理员, 多角色: Administrator + Operator):
  权限码: {"*", ...}                        ← 通配符，全部权限
  MCP 白名单: 勾选了 80 个工具
  tools/list 返回: 80 个工具

用户 B (观察者, 单角色: Viewer):
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
1. 用户在设置页面创建 MCP 配置 → 生成 token_key (dc3mcp_a3f8b2c1...)

2. AI Agent 连接:
   POST https://gateway:8000/mcp
   Authorization: Bearer dc3mcp_a3f8b2c1...
   Content-Type: application/json
   {"jsonrpc":"2.0","method":"tools/list","id":1}

3. McpAuthWebFilter:
   a. 解析 Bearer token → 查 dc3_mcp_server
   b. 检查 enable_flag、expire_time
   c. 获取 tenant_id, user_id
   d. PermissionProvider.listPermissionCodes(tenantId, userId)
   e. 构建 McpAuthContext 存入 Reactor Context

4. tools/list handler:
   a. 从 Reactor Context 取 McpAuthContext
   b. 用 permissionCodes 过滤全局工具注册表
   c. 用 dc3_mcp_server_tool 白名单进一步过滤
   d. 返回可见工具列表

5. tools/call handler:
   a. 验证工具在用户可见范围内
   b. 构造 HTTP 请求到对应后端服务 (内网直连)
   c. 注入 X-Auth-User (HMAC 签名) 认证 headers
   d. 发起 WebClient 请求，返回结果
```

### tools/call 认证转发策略

MCP 工具调用需要转发到后端服务。MCP Server 使用 HMAC 签名直接构造 `X-Auth-User` header，通过内网直接请求后端服务（不再走 Gateway 路由）:

```
McpToolCallHandler
  → 构造 UserHeader JSON {userId, userName, nickName, tenantId}
  → HmacAuthSigner.sign(userJson) → X-Auth-Sign
  → 直接请求后端服务 (绕过 Gateway 路由，避免 X-Auth-Token 校验)
  → 后端 GatewayJwtConverter 验证 HMAC 签名 → 通过
```

### 完整调用链路

```
AI Agent
  │  MCP 协议 (JSON-RPC over Streamable HTTP)
  │  POST /mcp
  │  Authorization: Bearer dc3mcp_xxx
  ▼
Gateway MCP Server
  │  解析 tools/call 请求
  │  → 工具名: "manager_device_add"
  │  → 参数: {"deviceName":"sensor-01", "driverId":100, "profileId":50}
  │
  │  查工具注册表:
  │  → targetServiceUrl: http://dc3-center-manager:8400
  │  → apiPath: /device/add
  │  → httpMethod: POST
  │
  │  构造 HTTP 请求:
  │  POST http://dc3-center-manager:8400/manager/device/add
  │  X-Auth-User: {"userId":100,"tenantId":1,...}
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

| 实体 | REST Controller (完整 CRUD) | gRPC Facade (只读查询) |
|---|---|---|
| Device | add, delete, update, get, list, import, export | ListByPage, ListByProfileId, GetById |
| Driver | add, delete, update, get, list | ListByPage, GetById |
| Point | add, delete, update, get, list, unit, statistics | ListByPage, GetById |
| User | add, delete, update, get, list | GetById |
| Role | add, delete, update, get, list, tree | ❌ 无 gRPC |
| Alarm | 全套 CRUD + 规则/通知/渠道 | ❌ 无 gRPC |
| Menu | add, delete, update, get, list, tree | ❌ 无 gRPC |

gRPC 覆盖约 30% 的操作（读查询），70% 的操作（写 + 角色管理 + 告警 + 菜单）没有 gRPC。

#### 方案对比

| 维度 | 全走 HTTP | 全走 gRPC | 混合 |
|---|---|---|---|
| 覆盖率 | ✅ 100% | ❌ ~30% | 需为 70% 补建 gRPC |
| 实现成本 | 低 | 极高（补 proto + 实现） | 高（两套路径） |
| 一致性 | ✅ 统一调用模式 | ❌ 读写路径分裂 | ❌ 两套代码 |
| Schema 对齐 | ✅ OpenAPI → HTTP 天然对齐 | ❌ proto 和 REST 两套定义 | ❌ 两套 |
| 维护成本 | 低 | 极高（每次新增接口写 REST + gRPC） | 高 |
| 性能 | 略慢（JSON 序列化） | 快（protobuf 二进制） | 混合 |
| 内网延迟影响 | 亚毫秒级，AI Agent 外部延迟 100-500ms，差距可忽略 | — | — |

#### 选择 HTTP 的理由

1. **覆盖率**: MCP 需要暴露所有业务接口，gRPC 只能覆盖 30%
2. **Schema 对齐**: OpenAPI spec 描述的就是 HTTP 端点，apiPath + httpMethod 直接构造请求，零映射成本
3. **BFF 模式一致**: Gateway 对前端是 `HTTP→HTTP` 转发，对 AI Agent 是 `MCP→HTTP` 转发，模式统一
4. **性能无感**: 内网 HTTP 调用亚毫秒级，AI Agent 到 MCP Server 的外部延迟 100-500ms，gRPC 的性能优势无法体现
5. **渐进式**: 后续如有性能瓶颈，可针对高频读操作单独走 gRPC，不影响整体架构

## 工具注册动态刷新机制

Gateway 是常驻进程，但各中心服务会更新、重启、增删接口。需要保证 MCP 工具注册表与服务端保持同步。

### 三种机制组合

**机制 1: 定时刷新（兜底）**

```yaml
dc3:
  mcp:
    server:
      refresh-interval: 300  # 每 5 分钟全量刷新一次
```

Gateway 定时重新拉取四个服务的 OpenAPI spec 和权限码，对比旧注册表计算差异（新增/修改/删除），更新全局注册表。

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

MCP 协议有 `notifications/tools/list_changed` 机制。当注册表更新后，Gateway 的 MCP Server 主动通知已连接的 AI Agent:

```json
{"jsonrpc":"2.0","method":"notifications/tools/list_changed"}
```

AI Agent 收到后会重新调用 `tools/list` 获取最新工具列表。

### 刷新效果

| 场景 | 刷新方式 | 延迟 |
|---|---|---|
| 服务重启 | RabbitMQ 事件 → 即时刷新 | 秒级 |
| 接口新增/修改 | 定时刷新兜底 | 最多 5 分钟 |
| 权限码变更 | `dc3_resource` 查询实时生效 | 即时（每次 tools/list 重新查） |
| MCP 白名单变更 | `dc3_mcp_server_tool` 查询实时生效 | 即时 |

> **注意**: 工具注册表（OpenAPI spec 聚合结果）的刷新间隔决定了"接口变更后多久对 AI Agent 可见"，但权限码和白名单的过滤是每次 `tools/list` 实时查询的，不受刷新间隔影响。

## 调用观测

### 现有可观测性状态

| 能力 | 状态 | 说明 |
|---|---|---|
| 结构化日志 (logback JSON) | ✅ 已有 | 所有服务输出 JSON 日志到文件，可被 ELK 采集 |
| `@Logs` AOP 注解 | ⚠️ 存在但未使用 | `dc3-common-log` 模块提供了注解和切面，但无方法使用 |
| 审计日志表 | ❌ 不存在 | 没有 `dc3_audit_log` 等表 |
| Prometheus 指标 | ✅ 基础设施就绪 | Micrometer + Prometheus endpoint 已暴露，无自定义指标 |
| Grafana 看板 | ✅ 基础设施就绪 | Docker Compose 可选栈中有 Prometheus + Grafana |
| 分布式追踪 | ⚠️ 未配置 | Brave 桥在 classpath 但无后端 |
| Elastic APM | ⚠️ 存在但禁用 | `APM_AGENT_ENABLE=false` |

### MCP 调用观测 (随方案实现)

**结构化日志**:

```java
// McpToolCallHandler 中记录每次调用
log.info("MCP tool call: tool={}, user={}, tenant={}, duration={}ms, status={}",
    toolName, userId, tenantId, duration, "success/error");
```

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
        │   ├── McpDynamicToolProvider.java
        │   └── McpToolCallHandler.java
        ├── entity/
        │   ├── McpServerDO.java / McpServerBO.java / McpServerVO.java
        │   └── McpServerToolDO.java / McpServerToolBO.java / McpServerToolToolVO.java
        └── service/
            ├── McpServerService.java
            └── McpServerToolService.java
```

`dc3-gateway` 的 pom.xml 同时依赖 `dc3-common-gateway` 和 `dc3-common-mcp`。

## 需要变更的现有文件

### 删除 ServiceMcpToolsController

| 文件 | 操作 | 说明 |
|---|---|---|
| `dc3-common-web/.../controller/ServiceMcpToolsController.java` | 删除 | 各服务不再需要独立的工具发现端点 |
| `dc3-common-web/.../config/WebFluxSecurityConfig.java` | 移除 | 删除 `/mcp_tools` 的 `permitAll()` 规则 |
| `dc3-common-resource-registrar/.../scan/ApiEndpointScanner.java` | 移除 | 删除 `DEFAULT_EXCLUDES` 中的 `"/mcp_tools"` |

### 生产环境启用 api-docs

5 个 `application-pro.yml` 从 `api-docs.enabled: false` 改为 `api-docs.enabled: true`。只开启 api-docs 端点供 MCP Server 读取，Swagger UI 面板仍然关闭 (`swagger-ui.enabled: false`):

| 文件 | 变更 |
|---|---|
| `dc3-gateway/src/main/resources/application-pro.yml` | `springdoc.api-docs.enabled: true` |
| `dc3-center/dc3-center-auth/src/main/resources/application-pro.yml` | 同上 |
| `dc3-center/dc3-center-manager/src/main/resources/application-pro.yml` | 同上 |
| `dc3-center/dc3-center-data/src/main/resources/application-pro.yml` | 同上 |
| `dc3-center/dc3-center-agentic/src/main/resources/application-pro.yml` | 同上 |

各中心服务的 `WebFluxSecurityConfig` 已有 `/v3/api-docs/**` 的 `permitAll()` 规则，无需修改。

## 前端设置页面

### 菜单注册

在 Settings 导航中新增 MCP 服务菜单，位于 About 之前:

| 层 | 文件 | 变更 |
|---|---|---|
| 后端 seed data | `iot-dc3-auth.sql` | `dc3_menu` 表新增 `settingsMcpServer` 记录 |
| 路由 | `src/config/router/settings.ts` | 新增 `settingsMcpServer` 路由 |
| 导航 | `src/config/settingsNav.ts` | `SETTINGS_TITLE_KEYS` / `SETTINGS_FALLBACK_SIDEBAR` / `SETTINGS_FALLBACK_ICON` 加条目 |
| 英文 i18n | `src/config/i18n/locales/en.ts` | `nav.settingsMcpServer: 'MCP Service'` |
| 中文 i18n | `src/config/i18n/locales/zh.ts` | `nav.settingsMcpServer: 'MCP 服务'` |
| API 常量 | `src/config/constant/api.ts` | 新增 `API_MCP_BASE = 'api/v3/auth/mcp_server'` |

### MCP Server 配置列表页

- 展示当前用户的所有 MCP 配置 (名称、Token Key、已启用工具数、状态)
- 操作: 新建配置、编辑、删除、管理工具、复制连接信息

### MCP 工具管理页

- 展示全局工具列表 (按 auth/manager/data/agentic 分类)
- 工具条目显示: 名称、描述、风险等级标签 (LOW/MEDIUM/HIGH)、读写标记
- 根据用户权限码过滤: 无权限的工具置灰不可勾选
- 有权限的工具可以通过勾选加入/移出白名单
- HIGH 风险工具默认不勾选或需二次确认

### 连接信息弹窗

- 展示 MCP Server URL + Token Key
- 一键复制
- 提供常见 AI Agent (Claude Desktop / Cursor / VS Code) 的配置片段

## 实施步骤

### Phase 1: 基础设施 (2-3 天)

| 步骤 | 任务 | 产出 |
|---|---|---|
| 1.1 | 创建 `dc3-common-mcp` 模块骨架 | pom.xml, 包结构 |
| 1.2 | 数据库: 新增 `dc3_mcp_server` + `dc3_mcp_server_tool` 表 | seed SQL |
| 1.3 | 后端: DO/BO/VO 实体 + MapStruct 转换器 | entity 包 |
| 1.4 | 后端: Service/Manager 层 CRUD | 基础增删改查 API |
| 1.5 | 删除 `ServiceMcpToolsController` 及相关引用 | 清理遗留代码 |
| 1.6 | 5 个 `application-pro.yml` 开启 `api-docs.enabled` | 生产环境配置 |

### Phase 2: MCP Server 核心 (2-3 天)

| 步骤 | 任务 | 产出 |
|---|---|---|
| 2.1 | `McpServerAutoConfiguration` + 配置属性 | 自动配置类 |
| 2.2 | `McpToolAggregator`: 从 OpenAPI spec + dc3_resource 聚合工具 | 启动时工具收集 |
| 2.3 | `McpAuthWebFilter`: Token Key 鉴权 | 认证过滤器 |
| 2.4 | `McpDynamicToolProvider`: tools/list 动态过滤 | 权限感知的工具列表 |
| 2.5 | `McpToolCallHandler`: tools/call → HTTP 转发 | 工具调用处理 |

### Phase 3: Gateway 集成 (1-2 天)

| 步骤 | 任务 | 产出 |
|---|---|---|
| 3.1 | `dc3-gateway/pom.xml` 引入 `dc3-common-mcp` + starter | 依赖 |
| 3.2 | `application.yml` 添加 MCP 配置 | 配置文件 |
| 3.3 | 验证 `/mcp` 端点启动正常 | 端到端连通性 |
| 3.4 | HMAC 签名认证转发调通 | 工具调用成功 |

### Phase 4: 前端 (2-3 天)

| 步骤 | 任务 | 产出 |
|---|---|---|
| 4.1 | 6 个前端文件更新 (路由/导航/i18n/API) | 菜单可见 |
| 4.2 | `McpServer.vue` 配置列表页 | CRUD 操作 |
| 4.3 | `McpServerTools.vue` 工具管理页 | 工具勾选 |
| 4.4 | 连接信息复制弹窗 | 一键复制 |

### Phase 5: 联调测试 (1-2 天)

| 步骤 | 任务 | 产出 |
|---|---|---|
| 5.1 | MCP Inspector 连接验证 | 协议兼容性 |
| 5.2 | Claude Desktop / Cursor 连接测试 | 真实 Agent 场景 |
| 5.3 | 多租户隔离验证 | 安全性 |
| 5.4 | 权限过滤边界测试 | 准确性 |

## 讨论记录

方案评审过程中讨论了以下 6 个问题，逐一记录问题和结论。

### D1: dc3_mcp_server_tool 白名单表的作用

**问题**: `dc3_mcp_server_tool` 这张表是做什么的？为什么不直接用权限码控制？

**结论**: 白名单表解决的是「在有权访问的工具中进一步挑选愿意暴露的工具」这个需求。三层过滤的关系:

1. **dc3_resource + dc3_role_resource_bind** — 角色决定的权限上限（比如管理员有 120 个权限码）
2. **dc3_mcp_server_tool** — 用户在设置页面勾选了其中 50 个愿意暴露给这个 MCP 配置
3. **最终 tools/list 返回** — ① ∩ ② = 50 个工具

白名单不能突破角色权限（即使勾选了某工具，角色无权限码则不出现）。白名单的价值: (1) 精细化控制每个 MCP 配置暴露的工具子集; (2) Token Key 泄露时限制攻击面。

### D2: 去掉 dc3_mcp_server.role_id，改为纯 user_id 关联

**问题**: 原方案中 `dc3_mcp_server` 有 `role_id` 字段，但系统支持一个用户绑定多个角色，权限在所有角色间取并集。怎么处理？

**结论**: 去掉 `role_id` 字段，只保留 `user_id` + `tenant_id`。

原因: 系统的 `dc3_role_user_bind` 是多对多关系，`PermissionProvider.listPermissionCodes(tenantId, userId)` 自动聚合用户所有角色的权限码。如果 MCP 配置只关联一个角色，就会丢失其他角色的权限。关联 user_id 后，MCP 工具可见范围 = 该用户全部角色权限 ∩ 白名单，与用户在系统中的实际权限完全一致。

### D3: OpenAPI 接口描述是否有 Harness 级别的元数据

**问题**: OpenAPI spec 能否告诉我们哪些接口能做什么不能做什么、风险等级、读写分类？

**现状**: 当前 OpenAPI 注解只有基础描述（`@Operation(summary, description)` 覆盖率 100%），没有读写分类、风险等级、废弃标记、操作后果、自定义扩展等 Harness 级别元数据。

**结论**: 不改造 OpenAPI 注解（成本太高），在 Gateway 侧聚合工具时基于已有数据自动推导:

| 维度 | 推导来源 | 规则 |
|---|---|---|
| READ / WRITE | HTTP 方法 + API 路径 + `@PreAuthorize` scope | GET → READ; POST 中 `/list`、`/get_*` → READ; 其余 POST → WRITE |
| 风险等级 | scope 语义 | delete → HIGH; add/update → MEDIUM; get/list → LOW; 设备写命令 → HIGH |
| 幂等性 | HTTP 方法 + scope | GET → true; POST /list → true; POST /add → false |

推导出的元数据用于: AI Agent description 中附加风险提示、前端白名单按风险过滤、后续 OpenAPI 加扩展时可直接对接。详见 [工具元数据自动推导](#工具元数据自动推导)。

### D4: tools/call 走 HTTP 还是 gRPC

**问题**: 如果 tool call 直接走 HTTP 请求后端服务，本质类似 RPC 了，那为什么不走 gRPC？

**结论**: 选 HTTP，不选 gRPC。

关键事实: 系统 gRPC 只覆盖约 30% 的操作（跨服务只读查询），70% 的操作（写 + 角色管理 + 告警 + 菜单等）没有 gRPC。选择理由: (1) 覆盖率 — MCP 需暴露所有接口，gRPC 只能覆盖 30%; (2) Schema 对齐 — OpenAPI 描述的就是 HTTP 端点，零映射成本; (3) 性能无感 — 内网 HTTP 亚毫秒级，AI Agent 外部延迟 100-500ms; (4) 维护成本 — 全走 HTTP 统一模式，混合路径维护成本高。详见 [HTTP vs gRPC 决策分析](#http-vs-grpc-决策分析)。

### D5: 工具注册动态刷新机制

**问题**: Gateway 是常驻的，其他服务会更新/修改/删除接口，如何实时同步？

**结论**: 三种机制组合:

| 场景 | 刷新方式 | 延迟 |
|---|---|---|
| 服务重启 | RabbitMQ 事件 → 即时刷新 | 秒级 |
| 接口新增/修改 | 定时刷新兜底 (每 5 分钟) | 最多 5 分钟 |
| 权限码变更 | `dc3_resource` 查询实时生效 | 即时 |
| MCP 白名单变更 | `dc3_mcp_server_tool` 查询实时生效 | 即时 |

注册表更新后通过 MCP 协议的 `notifications/tools/list_changed` 通知已连接的 AI Agent 重新拉取工具列表。详见 [工具注册动态刷新机制](#工具注册动态刷新机制)。

### D6: 多角色权限聚合 + 调用观测

**问题**: 用户可以配置多个角色吗？权限是否聚合？接口调用如何观测？

**结论**:

**多角色**: 支持。`dc3_role_user_bind` 是多对多，一个用户可绑定多个角色。权限在所有角色间取并集（`RoleResourceBindServiceImpl.listResourceByUserId` 4 步聚合），无"拒绝"机制，纯加法，通配符 `*` 满足所有检查。

**调用观测**: 当前系统可观测性基础设施部分就绪（logback JSON 日志、Prometheus/Grafana、ELK 可选栈），但缺少自定义指标和审计日志。MCP 调用观测分两层实现: (1) 随方案实现结构化日志 + 自定义 Prometheus 指标（调用量/成功率/耗时）; (2) 完整审计追踪（参数/结果记录）作为独立方案后续实施。详见 [调用观测](#调用观测)。

## 待确认问题

以下问题在讨论中提出，尚未得出最终结论，需要进一步确认后纳入实施计划。

### Q1: HMAC 签名依赖

MCP Server 的 `tools/call` 转发依赖 HMAC 签名（`HmacAuthSigner`）构造 `X-Auth-User` header。如果 HMAC 功能未启用（`dc3.auth.hmac.secret` 未配置），需要确认替代方案。是否需要在 MCP 场景下强制要求 HMAC 启用？

### Q2: OpenAPI spec 生产安全

生产环境开启 `api-docs.enabled: true` 后，API schema 端点对外暴露。虽然只暴露 schema（不含数据），但需要确认: (1) 是否需要限制 api-docs 端点只允许内网/Gateway 访问; (2) 是否需要对 api-docs 响应做脱敏（隐藏内部路径等）。

### Q3: 审计日志

当前系统没有审计日志表。MCP 工具调用是否需要完整的审计追踪（谁、什么时候、调了什么工具、传了什么参数、返回了什么结果）？如果需要，是否作为本方案的一部分，还是独立方案？

### Q4: gRPC 未来演进

当前选择 HTTP 调用。后续如果有性能需求，是否考虑为高频读操作渐进式添加 gRPC 路径？是否需要预留接口抽象以便未来切换？

### Q5: 多用户共享同一 MCP 配置

当前设计一个 MCP 配置只关联一个用户。是否需要支持多个用户共享同一个 Token Key（比如团队共享）？如果需要，`dc3_mcp_server` 需要改为关联用户组或角色而非单个用户。

## 风险与缓解

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| Spring AI 2.0.0-M8 的 MCP Starter 不够稳定 | 中 | 阻断 | 提前做 PoC 验证；如果 M8 有问题，降级到 MCP Java SDK 手动构建 |
| OpenAPI spec 中部分端点的 `@Schema` 注解不完整 | 低 | 描述缺失 | 逐步补全；即使缺少 description，工具名 + 路径仍有参考价值 |
| 工具数量过多 (100+) 导致 `tools/list` 响应慢 | 低 | 性能问题 | MCP 协议支持分页 cursor；权限过滤结果可缓存 |
| Token Key 泄露 | 低 | 安全风险 | `SecureRandom` 生成 + 支持 revoke + 支持过期时间 |
| HMAC 未启用时 tools/call 无法转发 | 中 | 功能不可用 | 实施前确认 HMAC 配置要求；或提供替代认证方案 |

## 相关资料

- [MCP Java SDK (GitHub)](https://github.com/modelcontextprotocol/java-sdk)
- [Spring AI MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [MCP Authorization Specification](https://modelcontextprotocol.io/specification/2025-03-26/basic/authorization)
- [MCP Transports Specification](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports)
- [MCP Tools Specification](https://modelcontextprotocol.io/specification/2025-03-26/server/tools)
- [Spring AI MCP Security](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-security.html)
- [设备与驱动状态超时管理说明](device-driver-timeout.md)
- [事件上报方案](event-report.md)
