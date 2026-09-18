# Frontend conventions

Read this when changing `dc3-web/`. Entry point and cross-cutting rules: [`AGENTS.md`](../../AGENTS.md).

Executable configuration is the source of truth:

- dependencies, package-manager version, and scripts: `dc3-web/package.json` and `pnpm-lock.yaml`;
- TypeScript behaviour: `dc3-web/tsconfig.json`;
- Vite, proxy, environment, and SCSS behaviour: `dc3-web/vite.config.ts`;
- test configuration: `dc3-web/vitest.config.ts` and `playwright.config.ts`;
- container toolchain pins: `dc3-web/Dockerfile`.

Use pnpm only; do not create npm or Yarn lockfiles. Keep package-manager pins aligned between `package.json` and the
Dockerfile.

Key rules:

- `verbatimModuleSyntax` is enabled. Use `import type` for every type-only import; Vue components, functions, and icons
  remain normal value imports.
- Use `<Entity>Form` for create/update payloads and `<Entity>Record` for read responses.
- Represent Java 64-bit IDs as strings. The backend emits identifiers as JSON strings on the HTTP contract, so standard
  JSON parsing (no JSONBigInt) is sufficient.
- API wrappers mirror backend cardinality: `getXxx` for one value, `listXxx` for collections/maps/pages, and
  `addXxx`/`updateXxx`/`deleteXxx` for mutations.
- Reuse CRUD helpers from `src/api/common.ts` and API bases from `src/config/constant/api.ts`; keep API wrappers thin.
- Prefer `<script setup>`, Composition API, setup-style Pinia stores, and existing composables.
- Every router-guard branch must settle navigation. Prefer return-style guards for new code and cover guard changes with
  tests.
- Axios interceptors own authentication headers and 401 handling; do not duplicate that logic in feature APIs.
- Vite dotenv files live under `src/config/env/` and use the `APP_` prefix.
- Global Element Plus variables are injected by Vite; do not duplicate their `@use` directives in components.
- Menu changes may require synchronized backend seed data, `settingsNav.ts`, router definitions, i18n locales,
  `Layout.vue`, and `Settings.vue` changes.

Checks, run from `dc3-web/`:

```bash
pnpm check
pnpm lint:check
pnpm test:guard
pnpm test:ci
pnpm build
```

Use affected Vitest suites for focused changes and Playwright for browser-level workflows. Coverage thresholds belong in
`vitest.config.ts`; do not duplicate their numbers here.
