## 1. Prepare

- `git`
- `Visual Studio Code`
- `nodejs` >= 22 (enforced by `engines` in `package.json`)
- `pnpm` 11.3.0 (pinned via `packageManager`), install using
  `corepack enable && corepack prepare pnpm@11.3.0 --activate`

## 2. Source code

```bash
git clone https://github.com/pnoker/iot-dc3.git
```

## 3. Develop

```bash
cd iot-dc3/dc3-web

# install
pnpm install

# run
pnpm dev
```

The dev server runs on `http://localhost:8080` and proxies API calls to the backend
gateway (`http://localhost:8000`), so start the backend stack first.

## 4. More

For the full command surface (build, type-check, lint, unit/component/E2E tests), the
tech stack, environment configuration (`src/config/env/`), and project conventions, see
[`AGENTS.md`](./AGENTS.md).
