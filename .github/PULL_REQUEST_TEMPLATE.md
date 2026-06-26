## What & Why

（这次改动做什么、为什么。1–3 句。）

## Changes

- （逐条列关键改动，可追溯到需求 / issue。）

## Verification

- [ ] `pnpm run test:impact` 已按推荐检查执行
- [ ] 大范围改动通过 `pnpm run test:ci` 覆盖门槛
- [ ] API wrapper 改动已更新 `tests/api`
- [ ] utility / store / composable / Axios 改动已更新 `tests/unit`
- [ ] 可复用组件改动已更新 `tests/component`
- [ ] 路由 / 菜单 / 权限 / 页面流程改动已更新 Playwright E2E
- [ ] E2E 数据动态创建并清理，无固定业务 ID
- [ ] `pnpm lint-check && pnpm check && pnpm build` 通过

## Impact

- （影响的模块、是否有 breaking change、是否需要 changelog）
