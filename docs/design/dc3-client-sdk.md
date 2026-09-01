# dc3-sdk：框架无关的客户端 SDK（设计）

状态：提案（phase 0 已落地） · 范围：web / cli / 未来 app · 驱动因素：A1（设备无关语义）、
docs/design/frontend-three-terminal-ux.md 中的边界纪律

## 问题

HTTP 网关契约目前被实现了两次：

- dc3-web/src/api/（30 个模块）——薄 axios 封装，与
  `@/config/axios`（Element Plus 通知、路由守卫 401 流程）及 Vite `@/` 别名耦合。
- dc3-cli/src/core/client.ts——为认证（token/salt、token/generate、token/cancel）手写的 fetch 调用。

原生 app（React Native）则需要第三套实现。每个新端点、请求头与错误码解释都必须逐客户端重复实现——
这是对 A1 的持续性违反。

## 目标

一个 TypeScript SDK，将网关契约精确实现一次，供 web、CLI 与未来的 app 客户端共同消费。该 SDK 框架无关：
无 Vue、无 Element Plus、无 vue-i18n、不硬依赖 axios。

## 包结构

```text
dc3-sdk/
├── src/
│   ├── core/            http adapter contract, typed payloads, pagination,
│   │                    token/session lifecycle, error taxonomy
│   ├── api/             one module per domain (driver/device/point/...),
│   │                    mirroring backend CRUD verbs (getXxx/listXxx/add/update/delete)
│   ├── types/           PageQuery/PageResult, Record types, domain enums
│   ├── i18n/            (optional) locale dictionaries as plain JSON
│   └── index.ts
├── tests/               vitest with an in-memory mock adapter
└── package.json
```

### 核心契约

```ts
// HTTP transport abstraction — web injects axios, RN/node injects fetch.
interface HttpClient {
  request<T>(config: {
    method: 'get' | 'post' | 'put' | 'delete';
    url: string;
    params?: Record<string, unknown>;
    data?: unknown;
    headers?: Record<string, string>;
  }): Promise<T>;
}

// Interceptors owned by the HOST, not the SDK: auth headers and 401
// handling stay in the consuming app (web keeps its axios interceptors).
interface SdkOptions {
  http: HttpClient;
  baseUrl: string;
  apiPrefix?: string; // default '/api/v3'
  onUnauthorized?: () => void; // host-provided redirect/logout policy
}
```

认证/会话：SDK 暴露 salt/generate/cancel/change-password 以及
`TokenStore` 抽象（web：httpOnly cookie——空实现；CLI：内存/文件；app：经宿主适配器访问 Keychain）。存储策略
永远不由 SDK 决定。

### 语义层（L1）可移植性

实体 schema 在本次迭代中已经去框架化：

- `EntityListConfig` 及相关类型以翻译后的字符串承载标签，字符串由
  `Translator = (key, params?) => string` 生成——Vue 的 ComposerTranslation 在结构上满足该签名；16 个配置模块
  不再 import vue-i18n。
- 下一步：把 `EntityListConfig` 类型移入 SDK 的 types 层，使 web 与 app 渲染同一份 schema
  （配置仍留在宿主应用中，共享的是形状）。

未来选项：通过 OpenAPI 导出（`make openapi`）由后端下发 JSON schema，在不改动客户端的前提下取代手写配置。

## 边界规则（可用 lint 强制）

在 dc3-sdk 内部，禁止 import 以下内容：

- vue / vue-router / pinia / element-plus / vue-i18n
- `@/config/*` 宿主基础设施
- 任何仅浏览器才有的全局对象，除非位于 `typeof window` 守卫之后

在 SDK 包配置中用 eslint `no-restricted-imports` / `import/no-extraneous-dependencies`
规则强制执行。

## 迁移计划

1. **Phase 0（已落地）**：L1 配置去框架化——引入 `Translator` 类型，16 个实体配置模块零 vue-i18n import。
2. **Phase 1**：搭建 `dc3-sdk` 包（pnpm workspace），移植
   `PageQuery/PageResult`、RFC 9457 错误和错误分类体系；使用 mock 适配器的 vitest。
3. **Phase 2**：移植 30 个 API 封装模块；针对后端 OpenAPI 规范（或录制的 fixture）做契约测试。
4. **Phase 3**：dc3-web 接入 SDK——删除 `src/api`，保留 axios 适配器 + 拦截器作为宿主粘合层；删除任何内容之前
   先跑完整 e2e（608 个单元测试 + Playwright）。
5. **Phase 4（已被 [`token-unification-mcp-first-cli.md`](./token-unification-mcp-first-cli.md) 取代）**：dc3-cli
   认证迁移到 OAuth/MCP 凭证机制（RS256 + scope + 刷新轮换），而非 SDK 包装的 salt/generate/cancel；SDK 仍是
   web/app 与 REST 类型的契约层。
6. **Phase 5**：app 客户端通过 fetch/Keychain 适配器消费 SDK。

## 验证

- SDK 单元测试在 node 中运行，零 DOM/Vue import（CI 断言：
  SDK 源码中不存在 `import ... from 'vue'`）。
- Web 回归门禁：pnpm check + lint + 完整 vitest + Playwright e2e。
- CLI 门禁：dc3-cli 的 vitest 套件针对 mock 适配器保持通过。

## 修订记录

2026-08：v1——提案，Phase 0 已落地（Translator 去框架化，16 个模块）。
