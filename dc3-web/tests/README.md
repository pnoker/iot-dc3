# Test Strategy

The frontend test suite is intentionally layered so failures point at the right
level of the application.

## Commands

- `pnpm test`: run all Vitest suites.
- `pnpm run test:unit`: run utility, store, and infrastructure unit tests.
- `pnpm run test:api`: run API wrapper contract tests.
- `pnpm run test:component`: run Vue component contract tests.
- `pnpm run test:views`: run route-level view tests (mounted views with mocked APIs).
- `pnpm run test:guard`: run meta tests that keep the test suite itself safe.
- `pnpm run test:impact`: print the recommended checks for the current change.
- `pnpm run test:ci`: run Vitest with coverage thresholds.
- `pnpm run test:coverage`: run Vitest with coverage.
- `pnpm run test:e2e`: run Playwright Test specs.
- `pnpm run test:e2e:headed`: run Playwright Test specs in a visible browser.
- `pnpm run test:e2e:sweep`: run the deep browser sweep.
- `pnpm run test:e2e:sweep:headed`: run the deep browser sweep in a visible browser.

## Layers

- `tests/unit`: deterministic unit tests for utils, stores, and shared
  infrastructure such as Axios interceptors.
- `tests/api`: frontend API wrapper contracts. These tests mock the transport
  layer and snapshot the method, URL, body, and config used by every exported
  wrapper.
- `tests/component`: Vue component contracts using `@vue/test-utils`. Reuse the
  shared Element Plus stubs in `tests/setup/stubs/element-plus.ts` instead of
  redefining them per file.
- `tests/views`: route-level view contracts. Same toolchain as `tests/component`
  but the subject is a `src/views/**` page mounted with mocked APIs. Use this
  layer when the unit-of-behaviour is a routed page rather than a reusable
  building block.
- `tests/e2e`: Playwright browser tests. The specs cover authentication,
  route availability, page health, and safe UI interactions.
- `tests/e2e/browser-sweep.mjs`: entrypoint for the deep browser sweep. The
  actual runner lives under `tests/e2e/browser-sweep`; keep new browser
  scenarios in Playwright specs unless they specifically need the sweep runner.
- `tests/guardrails`: meta tests for AI-assisted development. These tests block
  focused or disabled tests, fixed E2E business IDs, missing guard scripts, and
  missing testing policy documentation.

## Adding a new test

1. Pick the right layer (`tests/{unit,component,views,api,e2e}`).
2. Copy the matching template from `tests/_templates/`:

- `store.test.template.ts` for Pinia stores
- `composable.test.template.ts` for `src/composables/*`
- `component.test.template.ts` for `src/components/*` (and views)
- `api.test.template.ts` for new API wrappers (which actually means
  extending `tests/api/api-contracts.test.ts`)

3. Reuse fixtures from `tests/fixtures/` when the data shape already exists
   there — don't inline a duplicate `sampleMenuTree` / credentials block.
4. Reuse stubs from `tests/setup/stubs/element-plus.ts` for any Element
   Plus components.
5. Run `pnpm test` locally before committing. If it fails with a Vue warn,
   see [test-debugging.md](../docs/test-debugging.md).

## Conventions

These rules are mechanically enforced by `tests/guardrails/ai-guardrails.test.ts`.
Style points without a guardrail are listed for reviewer reference.

### File naming (enforced)

- `tests/{unit,component,views}/<kebab-case>.test.ts`. The base name should
  mirror the source module under test (`auth.ts` → `auth-store.test.ts`,
  `usePagedList.ts` → `use-paged-list.test.ts`,
  `validationUtil.ts` → `validation-util.test.ts`).
- Avoid suffixes that describe the test itself (`-coverage`, `-bonus`); name
  for the _subject_, not the goal.

### Mocking (enforced for multi-mock files)

- More than one `vi.mock` in a file → bundle the spies in one `vi.hoisted(() => ({…}))`
  block at the top, conventionally named `xxxMocks` (`tokenMocks`, `apiMocks`).
  Keeps top-level state minimal and avoids "Cannot access X before initialization"
  hoisting errors.
- A single `vi.mock` may inline its factory.

### Element Plus stubs (enforced)

- Reuse `tests/setup/stubs/element-plus.ts`. Do not redefine `ElButton`,
  `ElForm`, `ElPagination`, `ElInput`, `ElSelect`, etc. inline.
- Add new layout/decorator stubs to `layoutStubs` in that file when a new
  Element Plus component is needed.

### Type assertions (enforced)

- No `as unknown as T` double assertions. Use a type-correct fixture builder
  or `// @ts-expect-error` for the one line that intentionally violates the
  contract.
- No `as never`. Build a properly-typed fixture instead.

### `wrapper.vm` is forbidden in component/view tests (enforced)

- Drive the component through props, slots, emits, and DOM events — the
  public contract. Calling `wrapper.vm.someInternalMethod()` couples tests
  to the component's internal API, which churns whenever `<script setup>`
  details change.

### Describe nesting (style)

- One top-level `describe` per file, matching the subject (`describe('auth store', …)`).
- Multi-action subjects use a second-level `describe` per action
  (`describe('login', …)`, `describe('logout', …)`).
- Single-purpose subjects (e.g. `interval store` with one method) stay flat.

### Naming inside files (style)

- Stores: lowercase prose — `describe('auth store')`, `describe('menu store')`.
- Components/views: PascalCase symbol — `describe('ToolCard')`,
  `describe('AlarmNotify view')`.
- Utility describe blocks mirror the `src/utils/<file>` base — `describe('storageUtil')`.

### Vue warnings are errors (enforced via `tests/setup/vitest.setup.ts`)

- `[Vue warn]` and `[Vue error]` messages are promoted to thrown errors
  during tests. If you genuinely need to silence one, add a regex to
  `VUE_WARN_ALLOWLIST` with a comment explaining the source — don't broaden
  the check.
- Set `VITEST_ALLOW_VUE_WARN=1` to bypass while debugging locally.

## AI Guardrails

The mandatory policy lives in `docs/frontend-testing-guardrails.md`. In short:

- API wrapper changes require `tests/api` coverage.
- Shared utility, store, composable, and Axios changes require `tests/unit`.
- Reusable component changes require `tests/component`.
- Route, page, menu, and permission changes require Playwright coverage.
- New test data must be created dynamically and cleaned up; do not hard-code
  business IDs.

## E2E Environment

Playwright defaults to `http://localhost:8080`, runs with one worker, and starts
`pnpm run serve:e2e` unless `E2E_START_SERVER=0` is set. The E2E server builds
the app, serves `dist/`, and proxies `/api` to `http://localhost:8000` by
default. Use `E2E_BASE_URL` to point at an already running environment,
`E2E_API_TARGET` to point the local E2E proxy at a different gateway, or
`E2E_WORKERS=N` to opt into parallelism against an isolated backend dataset.

Use the headed scripts when you want to watch the browser operate:

- `pnpm run test:e2e:headed`
- `pnpm run test:e2e:sweep:headed`

The `tests/e2e/browser-sweep.mjs` script is kept for deeper
manual/full-environment sweeps. It performs destructive delete checks against
seeded data, so run it only against a disposable test dataset.

Both the Playwright specs and the browser sweep discover required route IDs at
runtime. If a required entity does not exist, the test creates
`e2e_*` fixture data through the backend API and deletes those records at the
end of the run.
