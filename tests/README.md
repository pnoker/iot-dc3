# Test Strategy

The frontend test suite is intentionally layered so failures point at the right
level of the application.

## Commands

- `pnpm test`: run all Vitest suites.
- `pnpm run test:unit`: run utility, store, and infrastructure unit tests.
- `pnpm run test:api`: run API wrapper contract tests.
- `pnpm run test:component`: run Vue component contract tests.
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
- `tests/component`: Vue component contracts using `@vue/test-utils`.
- `tests/e2e`: Playwright browser tests. The specs cover authentication,
  route availability, page health, and safe UI interactions.
- `tests/e2e/browser-sweep.mjs`: entrypoint for the deep browser sweep. The
  actual runner lives under `tests/e2e/browser-sweep`; keep new browser
  scenarios in Playwright specs unless they specifically need the sweep runner.
- `tests/guardrails`: meta tests for AI-assisted development. These tests block
  focused or disabled tests, fixed E2E business IDs, missing guard scripts, and
  missing testing policy documentation.

## AI Guardrails

The mandatory policy lives in `docs/frontend-testing-guardrails.md`. In short:

- API wrapper changes require `tests/api` coverage.
- Shared utility, store, composable, and Axios changes require `tests/unit`.
- Reusable component changes require `tests/component`.
- Route, page, menu, and permission changes require Playwright coverage.
- New test data must be created dynamically and cleaned up; do not hard-code
  business IDs.

## E2E Environment

Playwright defaults to `http://localhost:8080` and starts `pnpm dev` unless
`E2E_START_SERVER=0` is set. Use `E2E_BASE_URL` to point at an already running
environment.

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
