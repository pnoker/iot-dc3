# DC3 Web

`dc3-web` is the Vue and TypeScript management frontend for IoT DC3. It communicates with the backend exclusively
through `dc3-gateway`.

## Prerequisites

- Node.js 22 or newer, as declared by `engines` in `package.json`.
- Corepack enabled.
- The pnpm version declared by `packageManager` in `package.json`.

```bash
corepack enable
pnpm install
```

Use pnpm only. Do not generate npm or Yarn lockfiles. When changing the package-manager version, keep `package.json` and
the Dockerfile toolchain pin aligned.

## Develop

```bash
pnpm dev
```

The development server uses the port and API proxy configured by `vite.config.ts` and `src/config/env/`. Its defaults
are `http://localhost:8080` for the UI and `http://localhost:8000` for the gateway. The UI can start without the backend,
but login and data requests require a running gateway and center services.

## Build and verify

```bash
pnpm check
pnpm lint:check
pnpm test:guard
pnpm test:ci
pnpm build
```

Run focused suites with `pnpm test:unit`, `pnpm test:api`, `pnpm test:component`, or `pnpm test:views`. Browser workflows
use `pnpm test:e2e`; see [tests/README.md](./tests/README.md) for the test layers and fixture policy.

## Project conventions

- Vite environment files live under `src/config/env/` and use the `APP_` prefix.
- Java 64-bit IDs are represented as strings and decoded with the existing JSONBigInt support.
- Type-only imports must use `import type` because `verbatimModuleSyntax` is enabled.
- API wrapper names mirror backend cardinality: `getXxx` for one result and `listXxx` for collections/pages/maps.
- Every router-guard branch must settle navigation.

Repository-wide architecture, commit, and verification rules are in [../AGENTS.md](../AGENTS.md).
