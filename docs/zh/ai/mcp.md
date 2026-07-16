---
title: AI Agent / MCP 集成
---

<script setup>
import McpFilterFlowDiagram from '../../.vitepress/theme/components/McpFilterFlowDiagram.vue'
import McpOAuthSequenceDiagram from '../../.vitepress/theme/components/McpOAuthSequenceDiagram.vue'
import McpConfirmSequenceDiagram from '../../.vitepress/theme/components/McpConfirmSequenceDiagram.vue'
</script>


# AI Agent / MCP 集成

IoT DC3 把整个平台的 HTTP 能力自动聚合成一份 MCP（Model Context Protocol）工具目录，让外部 AI Agent 经 OAuth 2.1 鉴权后，通过网关的
`/mcp` 入口安全地列举并调用工具——读设备、查位号值、下发命令。读完你能搞清楚 token 怎么取、`/mcp` 怎么调、为什么有的工具看不见、HIGH
风险操作为什么要确认两次。

> 你在这里：想把 IoT DC3 接给一个 AI Agent。若只是想让人在对话框里问数据，看 [Agentic 中心](./agentic)
> ；若是命令行脚本接入，看 [CLI 使用指南](../automation/cli)。

## 为什么是 MCP，而不是直接调 HTTP

一个 AI Agent 要操作平台，最朴素的办法是把每个 REST
接口手写成一个工具喂给大模型。问题是：接口有三百多个、横跨四个中心，权限和租户隔离散落各处，删除类操作和只读查询混在一起没有风险分级。MCP
把这层标准化——平台把自己的接口**自动**导出成带风险标注的工具目录，Agent 用统一的 JSON-RPC
协议发现和调用，鉴权、租户、权限、风险确认全部在网关与鉴权中心收口。

这条链路有三个角色：**鉴权中心（Auth Center / `dc3-center-auth`）** 兼做 OAuth 2.1 授权服务器，负责发 token、做内省、聚合工具目录；
**网关（Gateway / `dc3-gateway`）** 是 MCP Resource Server，承载 `POST /mcp`，每次调用都重新校验权限并签名转发到后端；后端的*
*管理中心 / 数据中心 / 智能中心**才真正执行业务。Agent 只跟前两者打交道。

## 工具目录怎么来：自动聚合，稳定 tool_id

工具目录不是手写的。鉴权中心的 `McpOpenApiAggregator` 在运行期拉取 auth / manager / data / agentic 四个中心的 OpenAPI，与
`dc3_api`（`api_code` / `api_name`）和 `dc3_resource`（`resource_code` / `permission_code`）合并，为每个接口生成一条工具记录，落库到
`dc3_mcp_tool_catalog`。规模约 **330+ 个工具**，由四个中心 **330+ 个 OpenAPI 操作**自动生成。

每个工具有一个**稳定的 `tool_id`**（等于 `dc3_api.api_code`），格式是 `{service_name}:{HTTP_METHOD}:{api_path}`，其中
`service_name` 是完整服务名 `dc3-center-<x>`（来自 `spring.application.name`），例如：

```text
dc3-center-manager:POST:/device/add
dc3-center-data:POST:/point_command/write
dc3-center-data:POST:/point_value/latest
```

工具的**风险等级**逐条手工标注，不靠动词猜测：每个接口在 `@Extension(name = "x-dc3-ai")` 注解里显式写出 `riskLevel`（
`LOW` / `MEDIUM` / `HIGH`），resource-registrar 在扫描期强制校验该注解存在且 `riskLevel` 合法，缺失即报缺陷。聚合器原样读取注解里的
`riskLevel`，仅在注解整体缺失时才回退为保守的 `HIGH`。例如 `POST /point_command/write` 被手工标为 `HIGH`（
`destructive=true`），并不会因为它是写操作就降成 `MEDIUM`。

`read_only_hint` 由 HTTP 方法推导（`GET` → 1，`POST` → 0）。这些标注连同 `destructive_hint` / `idempotent_hint` /
`open_world_hint` 一起写进 `dc3_mcp_tool_catalog`，来源是接口上的 `x-dc3-ai` OpenAPI 扩展（见文末）。

::: info 目录如何刷新：当前只有手动端点
工具目录的唯一刷新入口是管理员手动调用的 HTTP 端点（`McpManagementController.refreshToolCatalog` →
`OAuthMcpRuntimeServiceImpl.refreshToolCatalog`）重新聚合落库。**没有定时刷新、也没有事件驱动自动刷新**：代码中不存在
`@Scheduled` 刷新任务，也不存在 `McpToolCatalogChangedEvent` 之类的提交后触发。所以新增接口后，需要管理员手动触发一次刷新，工具目录才会更新。
:::

## 访问控制：四道闸门决定一个工具可见可调

不是"在目录里 = Agent 就能用"。一个工具最终是否对某次连接**可见**、是否**可调**，要先过 OAuth 验证拿到 principal 与
scope，再经过 RBAC、连接白名单、风险策略三层求交集。

<McpFilterFlowDiagram lang="zh" />

`tools/list` 返回的，是 **principal 的权限码集合 ∩ 该 MCP 连接的工具白名单 ∩ 风险策略**三者的交集；`tools/call` 还要再叠加
OAuth scope 与逐次的风险确认。也就是说：即便目录里有 `dc3-center-data:POST:/point_value/latest`，若该连接的白名单（
`dc3_mcp_connection_tool`）没放行、或 token 没有 `mcp:tools:call` scope、或 principal 没有对应的查询权限，Agent 都调不到它。

::: warning HIGH 风险默认不可见
风险等级为 HIGH 的工具（如各类 `delete`）在 `tools/list` 中默认隐藏，需要显式开启才出现，调用时还要带 `mcp:tools:call:high`
scope，并走下文的两阶段确认。这是有意的保守默认，避免 Agent 误删。
:::

## OAuth 2.1 授权服务器：怎么拿 token

鉴权中心内置了一个手写的 OAuth 2.1 授权服务器（RS256 JWT），HTTP 端点如下（均挂在鉴权中心，经网关对外）：

| 端点                                        | 方法           | 用途                                |
|-------------------------------------------|--------------|-----------------------------------|
| `/.well-known/oauth-authorization-server` | `GET`        | 授权服务器元数据发现                        |
| `/.well-known/oauth-protected-resource`   | `GET`        | 受保护资源元数据（RFC 9728，网关侧）            |
| `/oauth2/authorize`                       | `GET`        | 授权码 + PKCE（用户登录 + 同意 + 选择 MCP 连接） |
| `/oauth2/token`                           | `POST`（form） | 换取 access_token / refresh_token   |
| `/oauth2/jwks`                            | `GET`        | 公钥集（RS256，供验签）                    |
| `/oauth2/revoke`                          | `POST`（form） | 撤销 token（含重放检测）                   |
| `/oauth2/register`                        | `POST`（JSON） | 动态客户端注册（管理员受限）                    |

token 内省 `introspect` **不暴露为 HTTP 端点**——它是 gRPC 内部接口，只给网关这个 Resource Server 校验 Bearer 用。

**安全基线**（均已实现）：公共客户端**强制 PKCE S256**；`redirect_uri` 精确匹配，不接受通配符；refresh-token 轮换（RFC 9700
§6.3，通过 `previous_refresh_token_hash` 做重放检测）；client secret 仅存哈希，不留明文。

**token 类型与有效期**：access_token 为短时 JWT（默认 15 分钟，claims 含
`iss/aud/exp/nbf/sub=principal_id/principal_type/scope/tenant_id/mcp_connection_id`）；refresh_token 轮换式，默认 30
天；authorization_code 5 分钟一次性、PKCE 绑定；client_credentials 走 SERVICE_ACCOUNT、无 refresh。

::: danger 仅 OAuth 2.1，没有长期令牌
平台的 MCP 接入**只支持 OAuth 2.1**，没有 PAT（Personal Access Token）、没有 `dc3mcp_*` 之类的长期静态令牌。所有调用都依赖短时
access_token + 轮换 refresh_token。不要试图在脚本里硬编码一个"永久 MCP key"——它不存在。
:::

## 一次完整调用：从取 token 到拿结果

下面这条时序覆盖 Agent 取 token、调用 `/mcp`、网关内省与签名转发的全过程。

<McpOAuthSequenceDiagram lang="zh" />

注意网关到后端这一跳：网关用 `McpGatewayClient.invokeBackend()` 直接走内部 WebClient（**绕过**网关自身的路由），构造
`X-Auth-Principal` 并做 HMAC 签名；后端的 `GatewayJwtConverter` 验签、还原 principal，再交给 `@PreAuthorize` 做权限判定。HMAC
密钥 `AUTH_HMAC_SECRET` 在 `pre/pro` 环境下若为空或等于默认值会
fail-fast，详见 [鉴权 · 租户 · RBAC](../architecture/auth-rbac)。

### `/mcp` 的 JSON-RPC 方法

`POST /mcp` 是 JSON-RPC 2.0（Streamable HTTP），由网关的 `McpGatewayController` 处理，支持的方法：`initialize`、
`notifications/initialized`、`ping`、`tools/list`、`tools/call`。

一次 `tools/list` 请求：

::: code-group

```bash [curl]
curl -X POST http://localhost:8000/mcp \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

```json [响应形态（示例）]
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "data_point_value_latest",
        "description": "查询设备最新位号值",
        "inputSchema": { "type": "object", "properties": { "deviceId": {"type":"string"} } }
      }
    ]
  }
}
```

:::

一次 `tools/call`（调用低风险的查询工具，参数值为示例）：

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "data_point_value_latest",
    "arguments": { "deviceId": "1839...", "pointId": "1840...", "current": 1, "size": 10 }
  }
}
```

返回时网关把后端的 `R<T>` 包成 MCP `CallToolResult`。工具背后对应的真实接口与字段，见 [Agentic 中心](./agentic) 与各中心
OpenAPI。

## HIGH 风险：两阶段确认

对 HIGH 风险工具（如删除），平台强制两阶段确认，防止 Agent 在一次推理里直接执行不可逆操作。

第一阶段：Agent 调 `tools/call` 但没带有效确认，服务端不执行，返回 `CONFIRM_REQUIRED` 和一个 `confirmId`（UUID），默认 TTL
`PT5M`。

第二阶段：Agent 带着 `confirmId` + `idempotency_key` 重新调用。服务端校验：未过期、`parameter_digest` 与首次一致、principal /
连接 / 工具均未变、且为一次性消费（`status=PENDING` 在 SQL 层做并发护栏，重放的 `confirmId` 会输掉竞争）。

<McpConfirmSequenceDiagram lang="zh" />

确认票据落在 `dc3_mcp_tool_confirmation`（`confirm_id`、`tool_id`、`parameter_digest`、`idempotency_key`、`status`
PENDING/CONSUMED/EXPIRED、`ttl_expires`），TTL 由 `dc3.mcp.confirm-ttl`（默认 `PT5M`）控制。每一次 HIGH 风险调用都审计进
`dc3_mcp_audit_log`（含 `confirm_id`、`idempotency_key`、`argument_digest`、`risk_level`、`duration_ms`、`remote_ip` 等）。

## 约束与边界（诚实标注）

::: info MCP resources / prompts 未实现
当前 `/mcp` 只实现了工具相关方法（`tools/list` / `tools/call` 及 `initialize` / `ping` / `notifications/initialized`）。MCP
协议中的 `resources/*` 与 `prompts/*` 能力**尚未实现**（已规划）；对应的 `mcp:resources:read` scope 保留但未启用。
:::

::: info `tools/list_changed` 事件推送未实现
工具目录变化时，MCP 的 `tools/list_changed` 通知**未做事件推送**（设计已规划，RabbitMQ 推送未实现）。目录刷新走的是上文中提到的手动端点，客户端应
**按计划自行重新拉取** `tools/list`，不要假设服务端会主动推送变更。
:::

工具调用全程为 HTTP（内部 WebClient），**没有** gRPC 工具调用通道——这是有意的设计。`x-dc3-ai` 元数据来自接口上的真实注解：

```java
@Extension(name = "x-dc3-ai", properties = {
    @ExtensionProperty(name = "riskLevel",   value = "MEDIUM"),  // LOW/MEDIUM/HIGH
    @ExtensionProperty(name = "destructive", value = "false"),   // 是否破坏数据/配置
    @ExtensionProperty(name = "idempotent",  value = "false"),   // 是否可安全重试
    @ExtensionProperty(name = "openWorld",   value = "true")     // 是否触达外部/物理世界
})
```

## 延伸阅读

- [Agentic 中心](./agentic) — 平台内置的对话与工具调用，理解 MCP 工具背后执行的是什么
- [鉴权 · 租户 · RBAC](../architecture/auth-rbac) — principal 模型、HMAC 签名与权限求解的完整链路
- [CLI 使用指南](../automation/cli) — 不接 AI，用 `dc3` CLI 直接驱动平台
