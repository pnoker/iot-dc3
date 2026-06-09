# API 文档

本页说明 IoT DC3 后端 OpenAPI / Swagger 文档的生成、访问、认证和导出方式。它补充 [测试](testing.md) 与根目录 `AGENTS.md` 中的工程规则。

平台在 WebFlux 栈上使用 `springdoc-openapi` `3.0.3`。REST 接口文档由代码注解生成，包括 `@Tag`、`@Operation`、`@Parameter` 和 `@Schema`，不维护独立的手写 API 规格文件。

## 架构

```text
                         +--------------------------------------------+
  Browser                |  dc3-gateway  (:8000)                      |
  /swagger-ui.html ----> |  Aggregated Swagger UI (service selector)  |
                         |  springdoc.swagger-ui.urls -> 4 centers    |
                         +--------+-----------+-----------+-----------+
              /v3/api-docs/auth   | /manager  | /data     | /agentic
                                  v           v           v
                         +------------+ +----------+ +----------+ +----------+
                         | auth :8300 | | mgr:8400 | | data:8500| |agentic   |
                         | /auth/...  | | /manager | | /data    | | :8600    |
                         +------------+ +----------+ +----------+ +----------+
                           group=auth    manager      data        agentic
```

## 分组方式

| 机制 | 说明 |
|------|------|
| Center 分组 | 每个业务模块提供自己的 `GroupedOpenApi` Bean，只扫描本模块 Controller 包 |
| 全局元信息 | `dc3-common-web` 中的 `SpringDocConfig` 提供 OpenAPI 标题、版本、联系人、许可证和安全方案 |
| Gateway 聚合 | Gateway 没有业务 Controller，通过 `springdoc.swagger-ui.urls` 聚合各中心服务文档 |
| Single 模式 | `dc3-center-single` 同时包含多个业务模块，因此 UI 中会出现多个分组 |

`SpringDocConfig` 和 `WebFluxSecurityConfig` 放在 `dc3-common-web` 的 auto-configuration 中，因为 Center 应用只扫描自身包路径。各业务模块的 `GroupedOpenApi` 放在本模块已扫描包下，由已有初始化逻辑加载。

## 访问入口

| 目标 | URL |
|------|-----|
| Gateway 聚合 UI | `http://<gateway>:8000/swagger-ui.html` |
| Auth Center 直连 | `http://<auth>:8300/auth/swagger-ui.html` |
| Manager Center 直连 | `http://<manager>:8400/manager/swagger-ui.html` |
| Data Center 直连 | `http://<data>:8500/data/swagger-ui.html` |
| Agentic Center 直连 | `http://<agentic>:8600/agentic/swagger-ui.html` |
| 单中心 JSON | `http://<center>:<port>/<svc>/v3/api-docs` |

各中心服务的 `spring.webflux.base-path` 会加到文档路径前，例如 `/auth/v3/api-docs`。Gateway 聚合路径 `/v3/api-docs/{svc}` 会隐藏这个差异，便于统一访问。

## Swagger UI 认证

除 `/auth/token/*` 外，多数接口需要通过 Gateway 认证头调用。开发环境中可按以下步骤操作：

1. 调用 `POST /api/v3/auth/token/salt`，请求体示例：`{"name":"dc3","tenant":"default"}`。
2. 计算 `md5(md5(password) + salt)`。
3. 调用 `POST /api/v3/auth/token/generate` 获取 token。
4. 在 Swagger UI 点击 **Authorize**，填写：

| Header | 示例 |
|--------|------|
| `X-Auth-Tenant` | `default` |
| `X-Auth-Login` | `dc3` |
| `X-Auth-Token` | `{"salt":"...","token":"..."}` |

不要把真实 token、password、api key 写入文档、日志或 issue。

## 环境暴露规则

API 文档在 `dev`、`test`、`pre` 环境可用，生产环境关闭。共享配置 `application-web.yml` 的 `pro` profile 会设置：

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

安全白名单允许文档路径本身访问；生产环境中 springdoc 端点不存在，因此不会暴露文档内容。

## 注解约定

- 注解文字使用英文，符合 `AGENTS.md` 对用户可见代码文本的要求。
- `@Operation` 摘要应遵循 CRUD 动词和结果基数约定：`add`、`delete`、`update`、`getXxx`、`listXxx`。
- 请求/响应 DTO 字段使用 `@Schema` 标明 `description`，必要时补充 `example` 和 `requiredMode = REQUIRED`。
- 不在 `@Schema` 示例中放 `apiKey`、`password`、`secret`、`token` 等敏感值。
- 接口统一返回 `R<T>` 响应封装，包含 `ok`、`code`、`message`、`data`。

## 导出 OpenAPI JSON

`make openapi` 会从运行中的开发或测试栈导出各中心服务的 OpenAPI JSON：

```bash
make openapi
```

可通过变量覆盖导出入口和输出目录：

```bash
make openapi OPENAPI_BASE=http://localhost:8000 OPENAPI_OUT=build/openapi
```

该命令适合生成离线契约快照或提供给客户端生成工具。

## 新增接口时的文档要求

1. Controller 类增加 `@Tag(name = "...", description = "...")`。
2. 方法增加 `@Operation(summary = "...", description = "...")`。
3. 路径参数、查询参数和请求体参数增加 `@Parameter`。
4. 请求/响应 DTO 字段增加 `@Schema`。
5. 新增业务模块时，补充 `GroupedOpenApi` Bean、Gateway 聚合配置和 Swagger UI 分组。
