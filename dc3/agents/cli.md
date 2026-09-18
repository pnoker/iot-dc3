# CLI conventions (`dc3-cli/`)

Read this when changing `dc3-cli/` or adding gateway subcommands. Entry point and cross-cutting rules:
[`AGENTS.md`](../../AGENTS.md).

Standalone TypeScript package (pnpm + tsup + vitest, binary name `dc3`). It has no Java/build coupling to the Maven
build; it talks to the running backend through the HTTP gateway only. It owns an independent package and lockfile;
the repository does not use a root pnpm workspace.

```bash
(cd dc3-cli && corepack pnpm install) # install the CLI's independent dependencies
(cd dc3-cli && corepack pnpm build)   # tsup → dist/ (bin: dc3)
(cd dc3-cli && corepack pnpm test)    # vitest run
(cd dc3-cli && corepack pnpm lint)    # eslint src --fix
```

- The CLI mirrors backend CRUD verb conventions: command surfaces use `get/list/add/update/delete`; list endpoints that
  call manager APIs use POST with a paging body.
- Output contract: `--format json|table|yaml`, TTY defaults to table, pipes default to json; exit codes 0 ok / 1
  business error / 2 network error / 3 auth error.
- When adding or changing a gateway HTTP API, check whether `dc3-cli/src/commands/*` needs the matching subcommand.
