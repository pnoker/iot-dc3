# Test Strategy

The frontend test suite is intentionally layered so failures point at the right
level of the application.

## Commands

- `pnpm test`: run all Vitest suites.
- `pnpm run test:unit`: run utility, store, and infrastructure unit tests.
- `pnpm run test:api`: run API wrapper contract tests.
- `pnpm run test:component`: run Vue component contract tests.
- `pnpm run test:coverage`: run Vitest with coverage.
- `pnpm run test:e2e`: run Playwright Test specs.
- `pnpm run test:e2e:legacy`: run the historical full UI script.

## Layers

- `tests/unit`: deterministic unit tests for utils, stores, and shared
  infrastructure such as Axios interceptors.
- `tests/api`: frontend API wrapper contracts. These tests mock the transport
  layer and snapshot the method, URL, body, and config used by every exported
  wrapper.
- `tests/component`: Vue component contracts using `@vue/test-utils`.
- `tests/e2e`: Playwright browser tests. The specs cover authentication,
  route availability, page health, and safe UI interactions.

## E2E Environment

Playwright defaults to `http://localhost:8080` and starts `pnpm dev` unless
`E2E_START_SERVER=0` is set. Use `E2E_BASE_URL` to point at an already running
environment.

The legacy `tests/e2e/full-ui.mjs` script is kept for compatibility and deeper
manual/full-environment sweeps. It performs destructive delete checks against
seeded data, so run it only against a disposable test dataset.

Both the Playwright specs and the legacy full UI script discover required route
IDs at runtime. If a required entity does not exist, the test creates
`e2e_*` fixture data through the backend API and deletes those records at the
end of the run.
