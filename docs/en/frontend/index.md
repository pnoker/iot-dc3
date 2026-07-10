---
title: Frontend Development
---

# Frontend Development

The IoT DC3 frontend is built on **Vue 3 + TypeScript + Vite + Element Plus**. Source code lives in the `dc3-web/`
directory.

## Environment Setup

| Tool    | Minimum Version | Notes                                                     |
|---------|-----------------|-----------------------------------------------------------|
| Node.js | 20 LTS          | Use fnm / nvm to manage versions                          |
| pnpm    | 9+              | Package manager, version locked in `packageManager` field |

```bash
# Install pnpm (if not already)
corepack enable && corepack prepare pnpm@latest --activate

# Verify versions
node -v   # >= v20
pnpm -v   # >= 9
```

## Quick Start

```bash
# 1. Enter the frontend project directory
cd dc3-web

# 2. Install dependencies
pnpm install

# 3. Start dev server (default http://localhost:8080)
pnpm dev
```

Once the dev server is running:

- Frontend pages: `http://localhost:8080`
- Proxies backend API to `http://localhost:8000` (gateway port) by default
- To change the backend address: edit the proxy config in `vite.config.ts`

::: tip Backend dependency
Frontend development requires backend services to be running. At minimum, you need the gateway `dc3-gateway` (port
8000). Start the full stack with docker-compose:

```bash
# From the repo root
make up-dev    # Starts gateway + 4 centers + common drivers
```

:::

## Project Structure

```
dc3-web/
├── src/
│   ├── api/           # REST API request wrappers
│   ├── components/    # Reusable components
│   │   ├── card/      #   InfoCard pattern (single-entity form + save/reset)
│   │   ├── chart/     #   Chart components (AntV G2/G6)
│   │   ├── entity/    #   Entity detail / list components
│   │   ├── layout/    #   Layout, menu, navbar
│   │   └── agentic/   #   AI chat components
│   ├── composables/   # Vue Composables
│   ├── config/        # Application configuration
│   │   ├── axios/     #   Axios instance and interceptors
│   │   ├── i18n/      #   Internationalization (zh / en)
│   │   ├── router/    #   Route definitions
│   │   └── types/     #   Entity type definitions
│   ├── store/         # Pinia state management
│   ├── styles/        # Global styles
│   ├── utils/         # Utility functions
│   └── views/         # Page components
│       ├── device/    #   Device management
│       ├── driver/    #   Driver management
│       ├── home/      #   Dashboard
│       ├── login/     #   Login
│       ├── point/     #   Point management
│       ├── profile/   #   Profile (device template) management
│       └── settings/  #   System settings
├── tests/             # Tests (Vitest + Playwright)
├── vite.config.ts     # Vite configuration
├── tsconfig.json      # TypeScript configuration
└── package.json       # Dependencies and scripts
```

## Menu System

The frontend menu is driven by two layers:

1. **Backend database** `dc3_menu` table — stores menu item definitions and permissions.
2. **Frontend router config** `src/config/router/` — maps menus to Vue page components.

The full chain for adding a new menu item:

```
Write to dc3_menu table -> Register frontend route -> i18n translation -> Bind permission point
```

All four layers must be updated; missing any one breaks the menu. See
the [Contributing Guide](../community/contributing).

## Common Commands

| Command           | Description                |
|-------------------|----------------------------|
| `pnpm dev`        | Start dev server           |
| `pnpm build`      | Production build           |
| `pnpm preview`    | Preview production build   |
| `pnpm test`       | Run unit tests             |
| `pnpm test:e2e`   | Run E2E tests (Playwright) |
| `pnpm lint`       | ESLint check               |
| `pnpm type-check` | TypeScript type check      |

## Testing

The project includes three layers of testing:

- **Unit tests** (Vitest): `tests/unit/` and `tests/component/`
- **API contract tests**: `tests/api/` — snapshot tests ensuring API wrapper interfaces don't break
- **E2E tests** (Playwright): `tests/e2e/` — browser end-to-end tests

CI gate: `pnpm lint && pnpm type-check && pnpm test && pnpm build`

See the [Test Debugging Guide](./test-debugging).

## Environment Variables

Frontend environment variables are organized by mode under `src/config/env/`:

```typescript
// .env.development
VITE_API_BASE_URL=http://localhost:8000
VITE_APP_TITLE=IoT DC3 (Dev)
```

Restart the dev server after changing the backend address.
