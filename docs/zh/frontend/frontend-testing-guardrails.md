# Frontend Testing Guardrails

This project uses tests as guardrails for AI-assisted development. Every change
should make the smallest safe code edit and update the test layer that protects
the behavior being changed.

## Required Test Mapping

| Change area                         | Required test layer               | Command                         |
| ----------------------------------- | --------------------------------- | ------------------------------- |
| `src/api/**`                        | API contract tests                | `pnpm run test:api`             |
| `src/utils/**`                      | Unit tests                        | `pnpm run test:unit`            |
| `src/config/axios/**`               | Unit tests                        | `pnpm run test:unit`            |
| `src/store/**`                      | Unit tests                        | `pnpm run test:unit`            |
| `src/composables/**`                | Unit tests                        | `pnpm run test:unit`            |
| `src/components/**`                 | Component contract tests          | `pnpm run test:component`       |
| `src/views/**`                      | Component or Playwright E2E tests | `pnpm run test:e2e` when routed |
| `src/config/router/**`              | Playwright route/auth smoke tests | `pnpm run test:e2e`             |
| build, lint, test, CI configuration | Guardrail tests and full quality  | `pnpm run test:ci`              |

Run `pnpm run test:impact` before finishing a feature to print the checks that
match the current changed files.

## AI Change Rules

- Do not change production code without updating or confirming the relevant
  test layer from the mapping above.
- Do not commit focused or disabled tests such as `test.only`, `describe.only`,
  `test.skip`, or `test.todo`.
- Do not add a new API wrapper file unless it is included in
  `tests/api/api-contracts.test.ts`.
- Do not use URL query strings or path interpolation inside API wrapper URLs.
  Pass dynamic values through Axios `params` or request bodies.
- Do not add fixed production IDs to tests. Test data must be discovered or
  created at runtime.
- Do not add new scenarios to `tests/e2e/browser-sweep.mjs`; it is a thin
  browser sweep entrypoint. Prefer Playwright specs for new browser scenarios.
- Do not hide broken coverage by loosening thresholds without a clear reason.

## E2E Data Rules

- E2E tests must create missing data instead of skipping scenarios.
- Runtime fixture data must use the `e2e_` prefix.
- Data created by a test must be registered in the cleanup stack.
- Delete checks must target disposable fixture data only.
- Playwright tests must report console errors, page errors, and failing
  business API responses.

## CI Gates

Pull requests and pushes run the non-environment-dependent quality gate:

1. `pnpm run lint:check`
2. `pnpm run type-check`
3. `pnpm run test:guard`
4. `pnpm run test:ci`
5. `pnpm build`

Playwright E2E runs through the manual workflow when a disposable backend URL is
provided with `e2e_base_url`.

## Adding New Features

1. Add the production code.
2. Add or update the matching unit, component, API contract, or E2E test.
3. Run `pnpm run test:impact` and the recommended commands.
4. Run the full local gate for broad changes:

```bash
pnpm run lint:check
pnpm run type-check
pnpm run test:guard
pnpm run test:ci
pnpm run build
```

For route or page changes, also run Playwright against a disposable test
environment:

```bash
E2E_BASE_URL=http://localhost:8080 E2E_START_SERVER=0 pnpm run test:e2e
```
