---
title: API 文档
---

<script setup>
import ApiDocFlowDiagram from '../../.vitepress/theme/components/ApiDocFlowDiagram.vue'
import ApiDocSequenceDiagram from '../../.vitepress/theme/components/ApiDocSequenceDiagram.vue'
</script>


# API 文档

IoT DC3 的 REST 接口文档由代码注解自动生成，经网关聚合成一个统一的 Swagger
UI。读完这页，你能在开发环境打开各中心的在线文档、用默认凭据走完"取盐 → 取 token → 带鉴权头调用"的登录流、看懂 CRUD
路径约定，并理解每个接口上的 `x-dc3-ai` 风险元数据是怎么喂给 AI/MCP 工具的。

> 你在这里：准备调用或调试后端接口。要先把环境跑起来，看 [第一个设备](../quickstart/first-device)
> ；要理解鉴权头背后的租户与权限，看 [鉴权·租户·RBAC](../architecture/auth-rbac)。

## 文档从哪来：注解生成，网关聚合

平台不维护任何手写的 API 规格文件。每个接口的标题、参数、请求/响应模型，全部来自 Controller 上的 `springdoc-openapi` 注解（
`@Tag`、`@Operation`、`@Parameter`、`@Schema`），运行时由各中心服务在 WebFlux 栈上生成自己的 OpenAPI JSON。

四个业务中心——鉴权中心（Auth Center / `dc3-center-auth`）、管理中心（Manager Center / `dc3-center-manager`）、数据中心（Data
Center / `dc3-center-data`）、智能中心（Agentic Center / `dc3-center-agentic`）——各自暴露一份文档。网关（Gateway /
`dc3-gateway`）本身没有业务 Controller，它通过 `springdoc.swagger-ui.urls` 把这四份文档聚合到一个带服务下拉选择器的
Swagger UI 里，对外只暴露一个入口。

<ApiDocFlowDiagram lang="zh" />

分组靠两层配置完成：`dc3-common-web` 的 `SpringDocConfig` 提供全局元信息（标题、版本、联系人、许可证、安全方案），各业务模块在自己已扫描的包下声明
`GroupedOpenApi` Bean，只扫描本模块 Controller。各中心服务的 `spring.webflux.base-path`（如 `/auth`、`/manager`）会加到文档路径前，例如
`/auth/v3/api-docs`；网关聚合路径 `/v3/api-docs/{svc}` 抹平了这层差异，统一访问。

::: info dc3-center-single 模式
`dc3-center-single` 把多个业务模块打进一个进程，所以它的 Swagger UI 里会同时出现多个分组——这是预期行为，不是配置重复。
:::

## 访问入口

开发环境下，优先用网关聚合入口；调试单个中心时也可以直连它的 base-path 文档。

| 目标               | URL                                             |
|------------------|-------------------------------------------------|
| 网关聚合 UI（推荐）      | `http://<gateway>:8000/swagger-ui.html`         |
| 鉴权中心直连           | `http://<auth>:8300/auth/swagger-ui.html`       |
| 管理中心直连           | `http://<manager>:8400/manager/swagger-ui.html` |
| 数据中心直连           | `http://<data>:8500/data/swagger-ui.html`       |
| 智能中心直连           | `http://<agentic>:8600/agentic/swagger-ui.html` |
| 单中心 OpenAPI JSON | `http://<center>:<port>/<svc>/v3/api-docs`      |

## 登录与鉴权：取盐 → 取 token → 带 X-Auth-* 头

除 `/api/v3/auth/token/**`（取盐、生成 token、改密）这类公开端点外，网关后的所有业务接口都要求携带三个鉴权头：`X-Auth-Tenant`、
`X-Auth-Login`、`X-Auth-Token`。登录本身是一个两步握手：先用用户名+租户向服务端要一枚随机盐（建议 5 分钟内使用，服务端不强制过期），再把
**明文密码**连同这枚盐一起提交换取 token（12 小时有效）。盐不参与密码哈希——它与服务端密钥
`DC3_SECURITY_KEY` 拼接后，作为 JWT 的 HMAC-SHA256 签名密钥；密码本身以明文提交（依赖 HTTPS 保护传输），由后端
`PasswordUtil.verify` 用 Argon2id（不可用时回退 BCrypt）校验。

<ApiDocSequenceDiagram lang="zh" />

实际调用形如下面这样（示例值仅作演示，`default`/`dc3` 是种子数据自带的默认租户与用户）：

::: code-group

```bash [curl]
# 1. 取盐
curl -X POST http://localhost:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'
# → R<String>：data 即 salt（示例："f3a9c1..."），建议 5 分钟内使用

# 2. 连同 salt 一起提交明文密码（依赖 HTTPS 保护传输）换 token
curl -X POST http://localhost:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"f3a9c1...","password":"<明文密码>"}'
# → R<String>：data 即 access token（示例："eyJ..."），12 小时有效

# 3. 带鉴权头调用业务接口
curl -X POST http://localhost:8000/api/v3/manager/device/list \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: {"salt":"f3a9c1...","token":"eyJ..."}' \
  -H 'Content-Type: application/json' \
  -d '{"current":1,"size":10}'
```

```bash [dc3 CLI]
# CLI 封装了取盐→换 token→保存凭据的全过程
dc3 config set gateway http://localhost:8000
dc3 auth login --tenant default --username dc3
```

:::

在 Swagger UI 里调试受保护接口时，点右上角 **Authorize**，按下表填入鉴权头即可：

| Header          | 示例值                            |
|-----------------|--------------------------------|
| `X-Auth-Tenant` | `default`                      |
| `X-Auth-Login`  | `dc3`                          |
| `X-Auth-Token`  | `{"salt":"...","token":"..."}` |

::: danger 不要把真实凭据写进文档/日志/issue
token、password、salt、api key 一律不入库到文档、提交记录或工单。示例里的哈希值、token 都用占位符代替。
:::

## CRUD 路径约定：动词反映结果基数

所有业务接口的命名遵循同一条规则——HTTP 路径、Java 方法、gRPC RPC、前端函数上的动词，必须反映**返回结果的基数**。读单条用
`getXxx`，读集合用 `listXxx`，写入三件套是 `add`/`update`/`delete`。这让你看到路径就能判断它返回一条还是一批、是读还是写。

| 动作   | Java 方法        | HTTP 路径     | gRPC RPC  | 前端函数             |
|------|----------------|-------------|-----------|------------------|
| 单条记录 | `getXxx(...)`  | `/get_xxx`  | `GetXxx`  | `getXxx(...)`    |
| 集合   | `listXxx(...)` | `/list_xxx` | `ListXxx` | `listXxx(...)`   |
| 新增   | `add(BO)`      | `/add`      | n/a       | `addXxx(...)`    |
| 更新   | `update(BO)`   | `/update`   | n/a       | `updateXxx(...)` |
| 删除   | `delete(Long)` | `/delete`   | n/a       | `deleteXxx(...)` |

::: tip 保留动词的专用语义
`select*` 只用于 `*ManagerImpl` 里的原生 MyBatis Mapper 调用；`remove*` 只用于 MyBatis-Plus 继承来的 Manager 方法。业务删除一律用
`delete*`。`find*`/`query*`/`fetch*` 不作为主 CRUD 动词。
:::

举一个走管理中心的真实例子（黄金路径中"新增设备"一步）：接口是 `POST /api/v3/manager/device/add`，请求体是 `DeviceVO`，关键字段
`deviceName`、`driverId`、`profileId`、`enableFlag`，成功返回成功码 `SuccessCode.ADD`（"Added successfully"）——`add` 不回传新建实体
ID，后续若需要这个 id，得用 `device/list` 按名称查回。需要 `device:add` 权限。接口统一用 `R<T>` 响应封装，含 `ok`、`code`、
`message`、`data` 四字段。

## x-dc3-ai：给 AI/MCP 工具的风险标注

每个接口的 `@Operation` 上可以挂一段 `x-dc3-ai` OpenAPI 扩展，用四个布尔/枚举属性描述这次调用对 AI Agent
意味着什么风险。这段元数据不是写给人看的注释——它会被 MCP 工具目录聚合器读取，落进 `dc3_mcp_tool_catalog` 表，进而决定一个工具在
`tools/list` 里是否对某个 AI 连接可见、调用时是否需要二次确认。

```java
@Extension(name = "x-dc3-ai", properties = {
    @ExtensionProperty(name = "riskLevel",   value = "MEDIUM"),  // LOW / MEDIUM / HIGH
    @ExtensionProperty(name = "destructive", value = "false"),   // 是否破坏数据/配置
    @ExtensionProperty(name = "idempotent",  value = "false"),   // 是否可安全重试
    @ExtensionProperty(name = "openWorld",   value = "true")     // 是否触达外部/物理世界
})
```

各属性的含义：

- `riskLevel`：`LOW`/`MEDIUM`/`HIGH`，**按动词语义约定人工标注**（不是由代码从 HTTP 方法自动推导）——惯例上 `delete` 标
  `HIGH`、`add`/`update` 标 `MEDIUM`、`get`/`list` 标 `LOW`，但最终取值来自每个 `@Operation` 上手写的注解，聚合器只在缺失或非法时兜底为
  `HIGH`。`HIGH` 风险工具默认对 AI 隐藏，需显式启用，且调用走两阶段确认。
- `destructive`：调用是否会破坏既有数据或设置（如改密、取消 token）。
- `idempotent`：同样参数重复调用是否安全（决定失败后能否自动重试）。
- `openWorld`：是否会触达平台之外的外部系统或物理设备（如下发写命令）。

以鉴权中心 `TokenController` 为例，取盐接口标注 `riskLevel=LOW, destructive=false, idempotent=false, openWorld=false`，而生成
token 接口标注 `riskLevel=HIGH`——两者都属公开端点、对 AI 工具目录隐藏（`hidden=true`），但风险等级如实区分。智能中心的
`POST /api/v3/agentic/chat/completions` 则标注
`riskLevel=MEDIUM, destructive=false, idempotent=false, openWorld=true`。

聚合器还会从 HTTP 方法补出 `read_only_hint`（`GET` → 1，`POST` → 0），把全部提示位（`destructive_hint`/`idempotent_hint`/
`open_world_hint`/`read_only_hint`，取值 0/1）连同 `risk_level` 一起持久化。AI Agent 通过 MCP 看到的工具风险，就是这套标注的最终呈现。完整的
MCP 工具暴露、过滤与确认机制见 [AI Agent / MCP 集成](../ai/mcp)。

## 导出 OpenAPI JSON

需要离线契约快照、或喂给客户端代码生成器时，从运行中的开发/测试栈一键导出各中心的 OpenAPI JSON：

```bash
make openapi
```

可通过变量覆盖导出入口与输出目录：

```bash
make openapi OPENAPI_BASE=http://localhost:8000 OPENAPI_OUT=build/openapi
```

## 新增接口时的文档要求

给后端加接口时，文档不是事后补的——注解就是文档源：

1. Controller 类加 `@Tag(name = "...", description = "...")`。
2. 方法加 `@Operation(summary = "...", description = "...")`，摘要遵循 CRUD 动词约定（`add`/`delete`/`update`/`getXxx`/
   `listXxx`）。
3. 路径、查询、请求体参数加 `@Parameter`。
4. 请求/响应 DTO 字段加 `@Schema(description = ...)`，必要时补 `example` 与 `requiredMode = REQUIRED`。
5. 涉及 AI/MCP 可调用的接口，按真实风险补 `x-dc3-ai` 扩展。
6. 新增业务模块时，补 `GroupedOpenApi` Bean、网关聚合配置和 Swagger UI 分组。

::: warning 注解文字一律用英文
注解里的 `summary`/`description` 属于用户可见代码文本，按工程规则用英文书写；同时不要在 `@Schema` 的 `example` 里放
`apiKey`、`password`、`secret`、`token` 等敏感值。
:::

::: danger 生产环境关闭 Swagger / OpenAPI 暴露
API 文档仅在 `dev`、`test`、`pre` 环境可用，生产环境（`pro` profile）由每个中心服务各自的 `application-pro.yml`
（auth/manager/data/agentic/single 各一份）关闭：

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

共享的 `application-web.yml` 只设 springdoc 的基线路径，不负责禁用——它的注释也写明禁用动作落在各服务的
`application-pro.yml`。生产环境中 springdoc 端点根本不存在，因此不会暴露任何文档内容。
:::

## 延伸阅读

- [鉴权·租户·RBAC](../architecture/auth-rbac) — 鉴权头背后的取盐/token/HMAC 与租户隔离、权限模型
- [第一个设备](../quickstart/first-device) — 用 `dc3` CLI 走完黄金路径，把这些接口实际跑一遍
- [AI Agent / MCP 集成](../ai/mcp) — `x-dc3-ai` 元数据如何变成 MCP 工具风险策略与两阶段确认
- [测试](./testing) — 接口契约与集成测试如何验证这些路径
