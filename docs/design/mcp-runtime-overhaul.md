# 设计：MCP 运行时全面重构 —— 内聚的授权契约

|                |                                                                                                                                                 |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **状态**       | 提案 —— 尚未实施                                                                                                                                |
| **日期**       | 2026-08-18                                                                                                                                      |
| **范围**       | MCP 运行时平面：`mcp_runtime.proto`、`McpRuntimeFacade`、`McpGatewayController`，auth 侧的 `OAuthMcpRuntimeServiceImpl` + `McpRuntimeServer`    |
| **目标**       | 每个 MCP 方法一次内聚的 gateway→auth 往返；响应式、非阻塞；真实 input schema；异步审计                                                          |
| **相关**       | [`mq-abstraction.md`](./mq-abstraction.md) —— 异步审计通道（若采用）所用的 broker port                                                          |
| **讨论**       | 实施启动前开放评审                                                                                                                              |

## 1. 摘要

位于 `McpGatewayController`（网关的 MCP JSON-RPC 资源服务器）背后的 MCP 运行时，目前通过一个**带有五个细粒度 RPC 的
同步 Facade** 访问 auth 中心，这迫使网关表现得像一个远程数据库客户端：它先调用
`Introspect` 取回令牌上下文，再把 `tenantId` / `principalId` /
`connectionId` 摊到 `ResolveTool` 和 `AuthorizeToolCall` 两个调用里，最后调用 `Audit`。因此一次 `tools/call` 的代价是
**4 次 gRPC 往返、约 8–10 次 DB 查询、每次调用一次阻塞线程跳转**，并且——作为一个正确性缺口——返回的工具
`inputSchema` 是静态的空壳信封，而不是真实的 JSON schema。

本文档提出对这一运行时契约进行**破坏性、不兼容的全面重构**：

- **3 个 RPC 取代 5 个**：`ListTools`、`CallTool`、`Audit`。
- `ListTools` / `CallTool` **以 bearer token 作为唯一输入**；auth 中心是唯一理解令牌的地方，
  因此它在同一次调用内部以一个决策完成校验 + 可见性 + 授权。
- `CallTool` 一次性返回**决策、解析出的工具与主体上下文**，网关在转发给后端之前不再需要单独的自省往返。
- Facade 变为**响应式**（`Mono`/future stub）；`blocking() + boundedElastic` 跳转消失。
- `inputSchema` 端到端透传，外部 agent 看到的是真实的工具参数。
- `Audit` 与调用路径解耦（fire-and-forget 或 broker 事件）。

## 2. 背景 —— MCP 运行时今天如何工作

已对照 2026-08-18 的代码树核实。文件引用均为确切路径。

### 2.1 现行契约

`dc3-api/dc3-api-auth/src/main/protobuf/api/common/auth/mcp_runtime.proto` 声明了五个 RPC：

| RPC                 | 用途                                                                         | 每次 tools/call 都调用？ |
|---------------------|-----------------------------------------------------------------------------|------------------------|
| `Introspect`        | 校验 OAuth bearer token，返回租户/主体/连接上下文                            | 是（每个请求）          |
| `ListTools`         | 列出该连接可见的工具                                                         | 否（仅 tools/list）     |
| `ResolveTool`       | 把一个工具解析为其后端调用元数据                                             | 是                      |
| `AuthorizeToolCall` | 强制高风险确认 + 幂等，返回决策                                              | 是                      |
| `Audit`             | 存储一条审计记录                                                             | 是                      |

`McpRuntimeFacade`（`dc3-common-facade-api`）以**同步**方法镜像这一契约：
`introspect(String)`、`listTools(...)`、`resolveTool(...)`、`authorizeToolCall(...)`、
`audit(...)`。它的 gRPC 实现（`McpRuntimeGrpcFacade`）使用注入的阻塞 stub；
`GrpcFacadeSupport.call` 只增加了一个 deadline。连接复用没有问题——stub 是共享 bean——问题在于
*调用的是什么*以及*调用频率*。

### 2.2 一次 `tools/call` 的逐步分解

网关路径是 `McpGatewayController.mcp(...)` → `dispatch(...)` →
`McpGatewayClient.callTool(...)`。每一跳及其核实过的成本：

| 步骤                | 位置                                               | 成本                                                                                                                                                                                                    |
|---------------------|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ① introspect        | `OAuthMcpRuntimeServiceImpl.introspect`            | 1× 本地 JWT 校验（`verifyWith(publicKey())`）+ **4 次 DB 查询**：`selectAuthorizationByAccessTokenJti`、`selectConnectionById`、`principalManager.getById`、`tenantMembershipService.isTenantMember` |
| ② resolveTool       | `resolveVisibleTool`                               | `selectVisibleToolByName`（1 次）+ `updateConnectionLastUsed`（1 次）                                                                                                                                          |
| ③ authorizeToolCall | `authorizeToolCall` → 重新执行 `resolveVisibleTool` | **重复执行** `selectVisibleToolByName` + `updateConnectionLastUsed`（再多 2 次）；仅 HIGH 风险随后查询确认票据                                                                                        |
| ④ invokeBackend     | 网关 `WebClient`                                | 1 次 HTTP 转发（真正的业务调用）                                                                                                                                                                       |
| ⑤ audit             | `audit` → `insert`                                 | 1 次 DB 写入（事后执行，失败被吞掉）                                                                                                                                                                    |

再加上 **4 次 gateway→auth gRPC 往返**（`Introspect`、`ResolveTool`、`AuthorizeToolCall`、
`Audit`），每次都包在 `blocking(...)` 里并在 `boundedElastic` 上订阅——每个在途调用占用一个阻塞线程。

### 2.3 三处冗余与一个正确性缺口

1. **自省在每个请求上完整重跑，没有任何缓存。** 这四次 DB 查询不能省略（OAuth
   自省必须检查吊销、主体启用状态与租户成员关系），但在 agent 循环里，同一个令牌在每次工具调用时都要再付一遍。
2. **resolve 与 authorize 重复执行可见性查询。** `authorizeToolCall` 显式重跑 `resolveVisibleTool`
   （"重新执行完整的可见性/白名单/scope 检查"）。结果完全相同；`selectVisibleToolByName` 与
   `updateConnectionLastUsed` 各执行两次。
3. **`updateConnectionLastUsed` 是非关键遥测，却同步执行——而且执行两次。**
4. **正确性缺口：`inputSchema` 丢失。** `GrpcMcpToolDefinitionDTO` **没有**
   `input_schema` 字段（proto 注释写明"不含静态 JSON schema 信封"），并且
   `McpRuntimeGrpcFacade.toDTO` 硬编码了 `DEFAULT_INPUT_SCHEMA`。因此外部 agent 对每个工具收到的都是空的 schema
   信封，而不是 `McpOpenApiAggregator` 已经算出、`OAuthMcpRuntimeServiceImpl.inputSchemaOf` 已经从 `tool_ext`
   读出的真实参数。

## 3. 目标 / 非目标

**目标**

- 把一次 `tools/call` 降为 **2 次 gateway→auth 往返**（`CallTool` + 异步 `Audit`）。
- 让令牌成为 `ListTools` / `CallTool` 的唯一输入；auth 是唯一的令牌权威。
- 消除重复的可见性查询（`selectVisibleToolByName` 一次，而非两次）。
- 让运行时**端到端响应式**；移除 `blocking()` + `boundedElastic`。
- 让**真实 `inputSchema`** 随 `tools/list` 透传，外部 agent 看到真实参数。
- 将审计与调用路径解耦。
- 移除面向网关的 `Introspect`、`ResolveTool`、`AuthorizeToolCall` RPC——**不保留向后兼容**，也不做
  双模式 shim。

**非目标**

- 客户端看到的 MCP 线上协议不变（仍是 JSON-RPC 2.0 + OAuth bearer）。
- 管理平面正在同步切换：工具目录和审计列表统一使用 `OffsetPage(items, offset, limit, total, hasNext)`，前端 MCP API 已按该形状发送 `offset/limit`。
- `McpOpenApiAggregator` / `dc3_api` 工具目录的生成方式不变。
- 后端服务认证下游 principal 头的方式不变（HMAC + JSON header 维持原样）。

## 4. 设计原则

1. **要决策，不是查询。** 网关在一个请求里问"这个令牌能否调用这个工具、它该转发到哪里"；
   而不是从三次查询拼出答案。
2. **auth 拥有令牌。** `ListTools` 与 `CallTool` 接收原始令牌；auth 在内部解析、校验并还原上下文。
   网关绝不从 claims 重建租户/主体上下文。
3. **唯一一次权威可见性检查。** 可见性 + 风险 + 确认 + 幂等在同一处、只此一次、于 `CallTool`
   内部裁决。
4. **全程响应式。** Facade 返回 `Mono`；gRPC 使用 future/异步 stub；网关留在 WebFlux 事件
   循环上。
5. **遥测在路径之外。** 审计是 fire-and-forget（或 broker 事件）；它永远不能延迟或使一次调用失败。

## 5. 目标契约

### 5.1 Protobuf

`mcp_runtime.proto` 收缩为三个 RPC。消息名沿用既有的 `Grpc` 命名约定。

```proto
service McpRuntimeApi {
  // tools/list: auth verifies the token and returns the visible tool list.
  rpc ListTools (GrpcMcpListToolsRequest) returns (GrpcMcpToolListDTO);

  // tools/call: auth verifies the token, enforces visibility + risk + idempotency,
  // and returns the decision, the resolved tool, and the principal context.
  rpc CallTool (GrpcMcpCallToolRequest) returns (GrpcMcpCallToolDTO);

  // Audit: decoupled from the call path (fire-and-forget or broker event).
  rpc Audit (GrpcMcpAuditCommand) returns (GrpcMcpBoolean);
}
```

移除：`Introspect`、`ResolveTool`、`AuthorizeToolCall` 及其请求/响应消息。共享枚举
（`GrpcMcpRiskLevel`、`GrpcMcpDecision`、`GrpcMcpPrincipalType`、
`GrpcMcpAuditStatus`）保留。

新增 / 变更的消息：

```proto
message GrpcMcpListToolsRequest {
  string token = 1;              // bearer token; auth resolves context internally
}

message GrpcMcpCallToolRequest {
  string token = 1;
  string tool_name = 2;
  string argument_digest = 3;    // sha256-base64url of arguments (idempotency + audit)
  string confirm_id = 4;         // empty on first attempt; present on confirm
  string idempotency_key = 5;
  string client_name = 6;        // for audit; read from MCP client headers
  string client_version = 7;
  string remote_ip = 8;
}

// tools/list item: now carries the real input schema.
message GrpcMcpToolDefinitionDTO {
  string name = 1;
  string title = 2;
  string description = 3;
  string input_schema = 4;       // NEW: JSON Schema, serialized from tool_ext
  GrpcMcpToolAnnotationsDTO annotations = 5;
  GrpcMcpToolMetadataDTO meta = 6;
}

// Principal context the gateway needs to forward downstream headers.
// Replaces the gateway's dependence on GrpcMcpIntrospectDTO.
message GrpcMcpPrincipalContext {
  int64 tenant_id = 1;
  int64 principal_id = 2;
  GrpcMcpPrincipalType principal_type = 3;
  string principal_name = 4;
  string display_name = 5;
  string client_id = 6;
  int64 connection_id = 7;
}

// tools/call result: decision + tool + principal context in one payload.
message GrpcMcpCallToolDTO {
  GrpcMcpDecision decision = 1;
  string confirm_id = 2;
  string message = 3;
  GrpcMcpRiskLevel risk_level = 4;
  GrpcMcpToolResolveDTO tool = 5;        // keeps service_name/api_path/http_method
  GrpcMcpPrincipalContext principal = 6;
}
```

`GrpcMcpToolResolveDTO` 保留（它仍是 `GrpcMcpCallToolDTO` 内部的 `tool` 子消息），并新增
`input_schema`，以便未来的后端需要时，网关可以把工具的 schema 转发给后端。

### 5.2 Facade

```java
public interface McpRuntimeFacade {
    Mono<McpToolListResponseDTO> listTools(String token);
    Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request);
    Mono<Void> audit(McpAuditCommandDTO command);
}
```

- `McpRuntimeGrpcFacade` 从 `McpRuntimeApiBlockingStub` 切换到异步的
  `McpRuntimeApiStub`（future→`Mono`），并去掉 `GrpcFacadeSupport.call`，改用响应式错误转换。
- 新 DTO `McpCallToolRequestDTO` / `McpCallToolResponseDTO` 与 proto 一一对应；响应以
  `McpPrincipalContextDTO` 取代旧的 `McpIntrospectResponseDTO`。

### 5.3 Auth 服务

`OAuthMcpRuntimeServiceImpl` 用两个内聚操作取代 `introspect` + `resolveVisibleTool` +
`authorizeToolCall`：

- `listTools(token)`：`parseAccessToken` → 有效授权检查 → 连接/主体/成员关系检查 →
  `listVisibleTools` → `toolToMcp`（现在带真实 `inputSchema`）。
- `callTool(request)`：同样的令牌校验，然后**一次** `selectVisibleToolByName` + 可见性/scope/风险决策，
  随后是确认/幂等闸门（仅 HIGH 风险），返回决策 + 解析出的工具 + 主体上下文。

`McpRuntimeServer` 与之对应：三个 gRPC 方法、为新消息准备的
`toGrpc` 构建器，以及从 `tool_ext` 序列化 `inputSchema`（复用既有的 `inputSchemaOf` 逻辑）。

### 5.4 网关

`McpGatewayController` 的分发简化为：

- `tools/list` → `mcpRuntimeFacade.listTools(token)` → JSON-RPC 结果。
- `tools/call` → `mcpRuntimeFacade.callTool(request(token, toolName, digest, confirmId, idempotencyKey, client meta))`；
  `AUTHORIZED` 时使用
  `tool.serviceName/apiPath/httpMethod` 转发到后端，并由 `principal` 构建 `X_AUTH_PRINCIPAL`；
  `CONFIRM_REQUIRED` 时返回确认提示；`REJECTED` 时返回拒绝。
- `audit` → fire-and-forget（返回前不再等待）。

`blocking(...)` 辅助方法、`toLong(context.getTenantId())` 的四处散布，以及独立的自省调用全部消失。

## 6. 请求流程

### 6.1 之前（今天，一次 tools/call）

```text
client → gateway ─ Introspect ──────────────→ auth  (JWT + 4 DB)
                 ─ ResolveTool ─────────────→ auth  (visible tool + last_used)
                 ─ AuthorizeToolCall ───────→ auth  (visible tool + last_used again, ± confirm)
                 ─ HTTP ────────────────────→ backend
                 ─ Audit ───────────────────→ auth  (insert)
        gateway ←─ decision / tool / context ── (assembled from 3 responses)
```

### 6.2 之后（目标，一次 tools/call）

```text
client → gateway ─ CallTool(token, tool, digest, confirm, key, client meta) ─→ auth
                                                                   (JWT + authz + visibility + risk
                                                                    + idempotency, one pass)
                 ─ HTTP (built from returned tool + principal) ──────────→ backend
                 ─ Audit ─ (fire-and-forget / broker event) ─────────────→ auth
        gateway ←─ decision + tool + principal ── (one response)
```

## 7. 迁移计划

破坏性变更，无兼容 shim。每个阶段结束时代码树必须可编译、测试保持绿色。

| 阶段  | 变更                                                                                                       | 文件                                                                                                |
|-------|-----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| 1     | 重写 `mcp_runtime.proto`（3 个 RPC、新消息、`input_schema`），重新生成 stub                                | `dc3-api/dc3-api-auth/.../mcp_runtime.proto`                                                        |
| 2     | 响应式 Facade + gRPC 实现（`Mono`、异步 stub）                                                            | `McpRuntimeFacade`、`McpRuntimeGrpcFacade`、新的请求/响应 DTO                                        |
| 3     | Auth 服务 + 服务端：把 introspect/resolve/authorize 合并进 `listTools`/`callTool`；真实 `inputSchema`     | `OAuthMcpRuntimeServiceImpl`、`McpRuntimeServer`                                                    |
| 4     | 网关：凭令牌直接分发，去掉 `blocking()`，异步审计                                                          | `McpGatewayController`                                                                              |
| 5     | 重写受影响的测试                                                                                          | `McpRuntimeServerTest`、`McpGatewayControllerTest`、`OAuthMcpRuntimeServiceImplTest`、Facade 测试   |

前端**完全不动**（MCP 设置页使用的是 `McpManagementController`，不是这个网关运行时）。

## 8. 验证

- 每个阶段之后运行 `mvn -s .mvn/settings.xml -q -DskipTests compile`。
- 用 `mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-auth -am` 与
  `-pl dc3-common/dc3-common-gateway -am` 运行重写后的测试。
- 需要保留的契约断言：
    - 一次 `tools/call` = 一次 `CallTool` RPC + 一次异步 `Audit`（通过测试替身断言），
    - `selectVisibleToolByName` 每次调用恰好执行一次，
    - `tools/list` 为夹具工具返回非默认 `inputSchema`，
    - HIGH 风险仍产生 `CONFIRM_REQUIRED` + confirmId，幂等键仍去重。
- 在 `dc3-web` 中运行 `pnpm check`，确认前端无回归。

## 9. 已考虑的备选方案

1. **缓存自省结果而不重塑契约**（保留 5 个 RPC）。已否决：它只是掩盖同步
   Facade、重复的可见性查询和缺失的 schema；往返次数与网关的上下文散布依然存在。
2. **网关本地 JWT 校验 + 短 TTL jti 拒绝列表。** 彻底消除自省往返，
   但把令牌权威移进了网关，并引入一条吊销传播通道。因其模糊 auth 边界而否决作为主设计；
   日后可以作为纯优化叠加在 3-RPC 契约之上。
3. **保留 `ResolveTool`，只合并其余部分。** 已否决：resolve 是调用决策的一个子步骤，不是独立操作；
   把工具放进 `CallTool` 的返回里严格更简单。

## 10. 开放问题

1. **审计传输。** fire-and-forget 的 `Mono`（改动更小）还是通过 MQ 抽象发 RabbitMQ 事件（完全在路径之外，
   但依赖 broker 可用性）。倾向 broker 事件，待确认。
2. **`GrpcMcpToolResolveDTO` 中的 `inputSchema`。** 被转发的后端是否终究需要 schema，
   还是 schema 只是 `tools/list` 的关注点？当前提案防御性地加上了它。
3. **令牌校验成本。** RPC 从 5 收敛到 3 之后，4 次自省 DB 查询在每次
   `CallTool` 时仍会执行。auth 内部的短 TTL `jti → context` 缓存（在吊销/禁用时失效）是后续工作，
   不属于本次契约重构。

## 11. 附录 —— 现有调用点盘点

已于 2026-08-18 核实。

| 构件          | 位置                                                                                 | 说明                                                                     |
|---------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| Proto         | `dc3-api/dc3-api-auth/src/main/protobuf/api/common/auth/mcp_runtime.proto`           | 5 个 RPC，260 行                                                         |
| Facade        | `dc3-common-facade-api/.../McpRuntimeFacade.java`                                    | 5 个同步方法                                                             |
| gRPC 实现     | `dc3-common-facade-grpc/.../McpRuntimeGrpcFacade.java`                               | 阻塞 stub + `GrpcFacadeSupport`                                          |
| 网关          | `dc3-common-gateway/.../McpGatewayController.java`                                   | JSON-RPC 分发、`blocking()`+boundedElastic、`invokeBackend`、`audit`     |
| Auth 服务     | `dc3-common-auth/.../OAuthMcpRuntimeServiceImpl.java`                                | `introspect`、`resolveVisibleTool`、`authorizeToolCall`                  |
| gRPC 服务端   | `dc3-common-auth/.../McpRuntimeServer.java`                                          | 5 个服务端方法                                                           |
| Schema 来源   | `dc3-common-auth/.../tool/McpOpenApiAggregator.java`                                 | 已计算 `inputSchema` 但未随 gRPC 传递                                    |
| 测试          | `McpRuntimeServerTest`、`McpGatewayControllerTest`、`OAuthMcpRuntimeServiceImplTest` | 在第 5 阶段重写                                                          |
