# dc3-sdk: Framework-Agnostic Client SDK (Design)

Status: proposed (phase 0 landed) · Scope: web / cli / future app · Drivers: A1 (device-independent semantics), boundary
discipline in docs/design/frontend-three-terminal-ux.md

## Problem

The HTTP gateway contract is currently implemented twice:

- dc3-web/src/api/ (30 modules) — thin axios wrappers, coupled to
  `@/config/axios` (Element Plus notifications, router-guard 401 flow) and Vite `@/` aliases.
- dc3-cli/src/core/client.ts — hand-rolled fetch calls for auth (token/salt, token/generate, token/cancel).

A native app (React Native) would need a third implementation. Every new endpoint, header, and error-code interpretation
must be re-implemented per client — a standing violation of A1.

## Goal

One TypeScript SDK that implements the gateway contract exactly once and is consumed by web, CLI, and future app
clients. The SDK is framework-agnostic:
no Vue, no Element Plus, no vue-i18n, no axios hard-dependency.

## Package layout

```text
dc3-sdk/
├── src/
│   ├── core/            http adapter contract, envelope R<T>, pagination,
│   │                    token/session lifecycle, error taxonomy
│   ├── api/             one module per domain (driver/device/point/...),
│   │                    mirroring backend CRUD verbs (getXxx/listXxx/add/update/delete)
│   ├── types/           PageQuery/PageResult, Record types, domain enums
│   ├── i18n/            (optional) locale dictionaries as plain JSON
│   └── index.ts
├── tests/               vitest with an in-memory mock adapter
└── package.json
```

### Core contracts

```ts
// HTTP transport abstraction — web injects axios, RN/node injects fetch.
interface HttpClient {
  request<T>(config: {
    method: 'get' | 'post' | 'put' | 'delete';
    url: string;
    params?: Record<string, unknown>;
    data?: unknown;
    headers?: Record<string, string>;
  }): Promise<R<T>>;
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

Auth/session: SDK exposes salt/generate/cancel/change-password and a
`TokenStore` abstraction (web: httpOnly cookie — no-op; CLI: memory/file; app: Keychain via host adapter). The SDK never
decides storage policy.

### Semantic layer (L1) portability

Entity schemas already became framework-free in this iteration:

- `EntityListConfig` and friends carry labels as translated strings produced by a
  `Translator = (key, params?) => string` — Vue's ComposerTranslation satisfies it structurally; 16 config modules no
  longer import vue-i18n.
- Next step: move `EntityListConfig` type into the SDK's types layer so web and app render the same schema (configs stay
  in the host apps; the shape is shared).

Future option: backend-served JSON schemas via the OpenAPI export (`make openapi`) replace hand-authored configs without
client changes.

## Boundary rules (lint-enforceable)

Inside dc3-sdk, imports of the following are forbidden:

- vue / vue-router / pinia / element-plus / vue-i18n
- `@/config/*` host infrastructure
- any browser-only global except behind `typeof window` guards

Enforce with eslint `no-restricted-imports` / `import/no-extraneous-dependencies`
rules in the SDK package config.

## Migration plan

1. **Phase 0 (landed)**: de-framework L1 configs — `Translator` type, zero vue-i18n imports in the 16 entity config
   modules.
2. **Phase 1**: scaffold `dc3-sdk` package (pnpm workspace), port
   `PageQuery/PageResult`, `R<T>` envelope, error taxonomy; vitest with mock adapter.
3. **Phase 2**: port the 30 API wrapper modules; contract tests against the backend OpenAPI spec (or recorded fixtures).
4. **Phase 3**: dc3-web consumes the SDK — delete `src/api`, keep the axios adapter + interceptors as the host glue; run
   full e2e (608 unit tests + Playwright) before removing anything.
5. **Phase 4**: dc3-cli swaps its hand-rolled fetch auth for the SDK (removes the duplicated salt/generate/cancel flow).
6. **Phase 5**: app client consumes the SDK with a fetch/Keychain adapter.

## Verification

- SDK unit tests run in node with zero DOM/Vue imports (CI assertion:
  `import ... from 'vue'` absent in SDK source).
- Web regression gate: pnpm check + lint + full vitest + Playwright e2e.
- CLI gate: dc3-cli vitest suites keep passing against a mock adapter.

## Revision

2026-08: v1 — proposal with Phase 0 landed (Translator de-frameworking, 16 modules).
