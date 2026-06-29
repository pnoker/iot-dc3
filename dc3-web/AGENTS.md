# IoT DC3 Web

Frontend for the IoT DC3 IoT platform (Vue 3 + Vite + Element Plus + Tauri).

## Quick Reference

```bash
pnpm install                         # install deps (pnpm only — no npm/yarn)
pnpm dev                             # Vite dev server on port 8080
pnpm build                           # production build → dist/
pnpm check                           # vue-tsc type check
pnpm lint                            # eslint --fix + prettier --write
pnpm lint:check                      # eslint + prettier check only (no writes)

# Testing — Vitest
pnpm test                            # vitest run (all suites)
pnpm test:unit                       # vitest run tests/unit
pnpm test:api                        # vitest run tests/api
pnpm test:component                  # vitest run tests/component
pnpm test:views                      # vitest run tests/views
pnpm test:ci                         # vitest run --coverage (CI gate)
pnpm test:guard                      # vitest run tests/guardrails (AI coding guardrails)
pnpm test:coverage                   # vitest run --coverage

# Testing — Playwright E2E
pnpm test:e2e                        # playwright test (headless chromium)
pnpm test:e2e:headed                 # visible browser (E2E_HEADLESS=false)

# Full CI gate
make ci                              # lint-check + check + test-guard + test-ci + build
```

The backend API lives at `http://localhost:8000` (see `src/config/env/.env.dev`). The dev server proxies `/api` there
via Vite. **The dev server runs without the backend**, but login and data endpoints will fail.

## Package Manager

**pnpm** (pinned by `packageManager` field in `package.json` + Corepack).

- First-time setup: `corepack enable` once.
- **Do not use `npm` or `yarn`** — they will generate a second lockfile.
- When bumping pnpm, update the `corepack prepare pnpm@X --activate` line in `Dockerfile` in lockstep.

## Stack

| Concern      | Choice                                                                                      |
| ------------ | ------------------------------------------------------------------------------------------- |
| Framework    | Vue 3.5 (`<script setup>`, Composition API)                                                 |
| Language     | TypeScript 6 (`verbatimModuleSyntax`, strict, noUncheckedIndexedAccess, noImplicitOverride) |
| Build        | Vite 8 + `@vitejs/plugin-vue` + `@vitejs/plugin-legacy`                                     |
| UI Library   | Element Plus 2.14 + `@element-plus/icons-vue`                                               |
| State        | Pinia 3 (setup-function style stores)                                                       |
| Routing      | Vue Router 5 (hash mode)                                                                    |
| HTTP         | Axios 1.16 + JSONBigInt (handles 64-bit integer IDs from Java backend)                      |
| i18n         | vue-i18n 11 (English + Chinese)                                                             |
| Charts       | @antv/g2, @antv/g6                                                                          |
| Maps         | @amap/amap-jsapi-loader                                                                     |
| Testing      | Vitest 4 (happy-dom) + Playwright 1.60 (E2E)                                                |
| Auto-imports | `unplugin-auto-import` + `unplugin-vue-components` (declarations in `src/config/ambient/`)  |

## Domain Model: Four-Layer IoT Entity

The core domain follows a strict hierarchy: **Driver** (protocol adapter) → **Profile** (device template) → **Device** (
physical equipment) → **Point** (data signal). This hierarchy is reflected everywhere — API paths, types, dashboard
palette colors, routing, entity enums.

## Project Layout

```
src/
├── api/                 REST API wrappers (thin, use crud* helpers from common.ts)
├── components/          shared components (cards, charts, tags, segmented, agentic)
├── composables/         usePagedList<T,Q> (generic paginated list), useEntityNames (ID→name cache)
├── config/
│   ├── ambient/         auto-generated type declarations (unplugin)
│   ├── axios/           axios instance + interceptors (auth headers, JSONBigInt, 401 redirect)
│   ├── constant/        enums, API base paths, auth header names, palette, icon map
│   ├── env/             dotenv files (NOT at repo root — Vite uses envDir: './src/config/env')
│   ├── i18n/            vue-i18n config + locale files
│   ├── plugins/         Element Plus + Highlight.js setup
│   ├── router/          routes + auth guards (common.ts, views.ts, settings.ts, operate.ts)
│   └── types/           all TypeScript interfaces (entity Form/Record pairs, dashboard, agentic)
├── store/               Pinia stores (auth, agentic, menu, interval)
├── styles/              Global SCSS
├── utils/               pure utility functions
└── views/               page-level components
```

## Key Conventions

### 1. `import type` is mandatory for type-only imports

`tsconfig.json` enables `verbatimModuleSyntax: true`. Any import used only as a type **must** use `import type` —
otherwise Vite keeps the import at runtime, the browser can't find the named export, and you get a `SyntaxError` → blank
page.

```ts
// ❌ Crashes at runtime
import {FormInstance, FormRules} from 'element-plus';

// ✅
import type {FormInstance, FormRules} from 'element-plus';
```

Common type-only names to watch out for:

- `element-plus`: `FormInstance`, `FormRules`, `UploadProps`, etc.
- `@/config/types`: `Order`, `Dictionary`, `Attribute`, `Login` — all plain interfaces.
- `vue-router`: `RouteRecordRaw`, `RouteLocationNormalized`, `NavigationGuardNext`, `RouteMeta`.
- `axios`: `AxiosInstance`, `AxiosError`, `AxiosResponse`, `InternalAxiosRequestConfig`.

Icons from `@element-plus/icons-vue` (`Box`, `Edit`, …) are **values (Vue components)** — regular `import { ... }` is
correct.

### 2. API verb convention: `get*` / `list*` / `add*` / `update*` / `delete*`

Mirrors the backend convention. The verb reflects the **cardinality** of the result:

```ts
// ✅ single record -> get*, /get_*
export const getDeviceById = (id: string) =>
  httpGet<R<DeviceRecord>>(`${API_MANAGER_BASE}/device/get_by_id`, {params: {id}});

// ✅ collection -> list*, /list_*
export const listDevice = <T = R<PageResult<DeviceRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/device/list`, query);

export const listDeviceByDriverId = (driverId: string) =>
  httpGet(`${API_MANAGER_BASE}/device/list_by_driver_id`, {params: {driver_id: driverId}});

// ❌ Not allowed in src/api/**
//   getDeviceList, getDriverByIds, /select_by_id, /tree
```

- Function names: `getXxx` returns a single record, `listXxx` returns a collection (list, page or map). `addXxx`/
  `deleteXxx`/`updateXxx` are for mutations.
- HTTP paths use snake*case and mirror the function name (`/get_by_id`, `/list_by_driver_id`, `/list`, `/list_tree`).
  `/select*\*` paths are no longer accepted.
- Use `getXxxCountByYyy` when the endpoint returns a single count value (the backend exposes `/get_count_by_*`).

API modules use generic helpers from `api/common.ts`: `crudAdd`, `crudUpdate`, `crudDelete`, `crudGetById`, `crudList`.

### 3. Type naming: Form vs Record

Every entity has a `<Entity>Form` type (create/update payloads, optional `id`) and a `<Entity>Record` type (read
responses, required `id` + timestamps `createTime`, `operateTime`).

### 4. Router guards must always resolve

Every branch of `beforeEach` in `src/config/router/index.ts` must eventually call `next()` or return a route/undefined.
A missing resolution leaves navigation pending → blank page with NProgress stuck.

Note: vue-router 5 has deprecated the callback style (`next(...)`) in favor of returning a value. It still works (warn
only), but new code should prefer the return style.

### 5. Auth flow

Login: `generateSalt` → MD5(password) → `generateToken` → store `{tenant, login, {salt, token}}` in localStorage. Every
request includes `X-Auth-Tenant`, `X-Auth-Login`, `X-Auth-Token` headers. The router guard only verifies that a complete
local auth payload exists; backend expiry and invalidation are handled by the Axios 401 interceptor.

### 6. SCSS and element-plus variables

Global `element-variables.scss` is injected into every component via Vite's `additionalData` (with circular-import
guard). Do not add duplicate `@use` directives for Element Plus variables in individual components.

### 7. `envDir` is under `src/config/env`

Vite is configured with `envDir: './src/config/env'`, so dotenv files are **not** at the repo root. The env-var prefix
is `APP_`.

## API Gateway Routing

Frontend calls go through `dc3-gateway` at `/api/v3/{auth|data|manager|agentic}`. The gateway strips the prefix and
routes to the appropriate center microservice. Base paths are defined in `src/config/constant/api.ts`:

- `API_AUTH_BASE = 'api/v3/auth'`
- `API_DATA_BASE = 'api/v3/data'`
- `API_MANAGER_BASE = 'api/v3/manager'`
- `API_AGENTIC_BASE = 'api/v3/agentic'`

## Testing

### Vitest (unit / api / component / views)

- Environment: `happy-dom`, 30s timeout
- Coverage: V8 provider, thresholds at 65% branches / 75% functions / 78% lines
- Test templates in `tests/_templates/` (api, component, composable, store)
- Guardrails in `tests/guardrails/` validate AI coding conventions

### Playwright (E2E)

- Chromium only, 60s timeout, `retain-on-failure` traces/screenshots
- Auto-starts `pnpm run serve:e2e` as webServer when `E2E_START_SERVER=1` (default). That command builds the app and
  serves `dist/` through `scripts/testing/e2e-server.mjs`, which proxies `/api` to `http://localhost:8000` by default.
- Env vars: `E2E_BASE_URL` (default `http://localhost:8080`), `E2E_HEADLESS` (default `true`)

## Known Issues

- **`auto-imports.d.ts`** declarations like `const FormInstance: typeof import(...)` are for TS only — you still must
  write `import type` in source files; auto-import would inject a runtime import and crash.
- **`src/components/particles/particles.vue`**: login page particle background. Under Vue 3.5 `mounted` may fire before
  the canvas ref is ready. Wrap init in `onMounted(() => nextTick(...))` if fixing.
- **Ignored build scripts for `@parcel/watcher` / `core-js`**: disabled by default in pnpm 10 as security hardening.
  Harmless; run `pnpm approve-builds` to silence the warning.
- **Tauri desktop**: `src-tauri/` exists; `@tauri-apps/api` is not currently imported in `src/`.

## Commit Rules

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <lowercase imperative description>
```

**Allowed types**: `feat`, `fix`, `perf`, `refactor`, `docs`, `build`, `ci`, `test`, `chore`, `style`, `security`,
`revert`. Max 100 chars, English only. Install hooks with `make install-hooks` (from `iot-dc3/`).

## Cross-Repo References

The canonical project instructions are in `../AGENTS.md` (shared across AI tools). The backend lives in `../iot-dc3/`;
container/compose infrastructure and the parent POM also live under `../iot-dc3/` (see `iot-dc3/dc3/` and
`iot-dc3/pom.xml`). See the root `CLAUDE.md` for the full monorepo map.

### Menu system (frontend ↔ backend)

Settings sidebar is driven by **both** frontend config and backend database. When renaming/moving a menu item, update
ALL layers:

- Seed data SQL (`menu_code`, `menu_ext.url`, `parent_menu_id`, `menu_index`)
- `src/config/settingsNav.ts` (TITLE_KEYS, \*\_CHILDREN, GROUP_OPENERS, BREADCRUMB_PARENTS, FALLBACK_ICON, ACTIVE_ALIAS,
  ROUTE_ALIAS)
- Router (`settings.ts` route name + `operate.ts` detail routes)
- i18n (`src/config/i18n/locales/{en,zh}.ts` `nav.*` keys)
- `Layout.vue` (nameMap + icon fallback)
- `Settings.vue` (hardcoded menu code references)

### Settings route naming

All settings route names use the pattern `settings<Group><Item>`, e.g. `settingsAlarmOverview`. Group menu codes:
`settingsAlarm`, `settingsEvent`, `settingsCommand`, `settingsModel`.

## Dockerfile Build

The `Dockerfile` pins pnpm via `corepack prepare pnpm@11.3.0 --activate`, matching `package.json`'s `packageManager`
field. **When bumping pnpm, update both in lockstep** — otherwise CI resolves a different version than local.
