# IoT DC3 Web

Frontend for the IoT DC3 IoT platform (Vue 3 + Vite + Element Plus + Tauri).

## Package Manager

**pnpm 10.33.2** (pinned by the `packageManager` field in `package.json` + Corepack).

- First-time setup: run `corepack enable` once.
- **Do not use `npm` or `yarn`** — they will generate a second lockfile and diverge from the team.

## Common Commands

```bash
pnpm install           # install deps
pnpm dev               # start Vite dev server (port 8080)
pnpm build             # produce dist/
pnpm build:tauri       # build Tauri desktop app
pnpm check             # vue-tsc type check
pnpm lint              # eslint --fix + prettier
```

The backend API lives at `http://localhost:8000` by default (see `src/config/env/.env.dev`). The dev server proxies
`/api` there via Vite. **The dev server runs fine without the
backend**, but login and data endpoints will fail.

## Stack Highlights

- **Vue** 3.5.x — mixes `<script setup>` and classic `<script lang="ts">` options API.
- **Vite** 8 + `@vitejs/plugin-legacy` (ships a legacy chunk with core-js polyfills for old browsers).
- **TypeScript** 6.x with **`verbatimModuleSyntax: true`** — see the conventions below.
- **element-plus** 2.13 + `unplugin-vue-components` / `unplugin-auto-import` for on-demand imports.
- **vue-router** 5.x in **hash mode** + **pinia** 3.x.
- **i18n**: Chinese only.
- **Styling**: SCSS; element-plus variables injected globally via `@use` (see `additionalData` in `vite.config.ts`).

## Project Conventions

### 1. Types must use `import type` (strict)

`tsconfig.json` enables `verbatimModuleSyntax: true`. **Any import that is used only as a type must use `import type`
** — otherwise Vite keeps the import at runtime, the browser
can't find the named export, and you get a `SyntaxError` → blank page.

```ts
// ❌ Crashes at runtime
import { FormInstance, FormRules } from 'element-plus';

// ✅
import type { FormInstance, FormRules } from 'element-plus';
```

Common type-only names to watch out for:

- `element-plus`: `FormInstance`, `FormRules`, `UploadProps`, etc.
- `@/config/entity`: `Order`, `Dictionary`, `Attribute`, `Login` — all plain interfaces.
- `vue-router`: `RouteRecordRaw`, `RouteLocationNormalized`, `NavigationGuardNext`, `RouteMeta`.
- `axios`: `AxiosInstance`, `AxiosError`, `AxiosResponse`, `InternalAxiosRequestConfig`.

Icons from `@element-plus/icons-vue` (`Box`, `Edit`, …) are **values (Vue components)**, so regular `import { ... }` is
correct.

### 2. Router guards must always call `next()`

Every branch of `beforeEach` in `src/config/router/index.ts` must eventually call `next()` (or
`next({ path: '/login' })`). A missing call leaves the navigation pending →
`<router-view>` renders nothing → blank page with NProgress stuck. In particular, watch the `.then(!res.data)` and
`.catch` branches of `checkTokenValid`.

Note: vue-router 5 has deprecated the callback style (`next(...)`) in favor of returning a value. It still works (warn
only), but new code should prefer the return style.

### 3. `envDir` is under `src/config/env`

Vite is configured with `envDir: './src/config/env'`, so dotenv files are **not** at the repo root. The env-var prefix
is `APP_`.

### 4. API verb convention: `get*` for single, `list*` for multi

Matches the backend convention documented in `iot-dc3/AGENTS.md`. The verb on
every API function and HTTP path reflects the cardinality of the response.

```ts
// ✅ single record -> get*, /get_*
export const getDeviceById = (id: string) =>
  httpGet<R<DeviceRecord>>(`${API_MANAGER_BASE}/device/get_by_id`, { params: { id } });

// ✅ collection -> list*, /list_*
export const listDevice = <T = R<PageResult<DeviceRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/device/list`, query);

export const listDeviceByDriverId = (driverId: string) =>
  httpGet(`${API_MANAGER_BASE}/device/list_by_driver_id`, { params: { driver_id: driverId } });

// ❌ Not allowed in src/api/**
//   getDeviceList, getDriverByIds, /select_by_id, /tree
```

- Function names: `getXxx` returns a single record, `listXxx` returns a
  collection (list, page or map). `addXxx`/`deleteXxx`/`updateXxx` are for
  the corresponding mutations.
- HTTP paths use snake_case and mirror the function name (`/get_by_id`,
  `/list_by_driver_id`, `/list`, `/list_tree`). `/select_*` paths are no
  longer accepted.
- Use `getXxxCountByYyy` when the endpoint returns a single count value
  rather than a list (the backend exposes `/get_count_by_*`).

## Project Layout

```
src/
├── api/                 REST API wrappers (axios instance in src/config/axios/)
├── components/          shared components
├── config/
│   ├── axios/           axios instance + interceptors (JSONBigInt for large ints)
│   ├── constant/
│   ├── entity/          interface-only module — always use `import type`
│   ├── env/             dotenv files (see envDir above)
│   ├── plugins/         element-plus / highlight.js setup
│   ├── router/          vue-router config
│   └── types/           global ambient .d.ts
├── store/               pinia stores (auth / interval)
├── utils/               shared utilities
└── views/               route-level pages
```

## Known Issues & Notes

- **`src/components/particles/particles.vue`**: the login page's 3D particle background. Under Vue 3.5 its `mounted`
  hook may fire before the canvas ref is ready, causing
  `Cannot read properties of null (reading 'appendChild')`. Non-blocking for login. Fix by wrapping the init in
  `onMounted(() => nextTick(...))` or replacing the canvas setup.
- **`auto-imports.d.ts` declarations like `const FormInstance: typeof import('element-plus').FormInstance`** are for TS
  only. You still must write `import type` in source files —
  auto-import alone would inject a runtime import and crash.
- **Ignored build scripts for `@parcel/watcher` / `core-js`**: disabled by default in pnpm 10 as a security hardening.
  Harmless — macOS/Linux use the prebuilt binary for
  `@parcel/watcher`, and core-js's postinstall is only an ad. Run `pnpm approve-builds` if you want to silence the
  warning.
- **Tauri desktop**: `src-tauri/` is kept around; `@tauri-apps/api` is currently not imported anywhere in `src/`.
  Revisit when desktop work starts.

## Dockerfile Build

Uses `pnpm@10.33.2`, matching `package.json`'s `packageManager` field. **When bumping pnpm, update
the `corepack prepare pnpm@X --activate` line in `Dockerfile` in lockstep** —
otherwise CI will resolve a different version than local.
