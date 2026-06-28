## What & Why

（这次改动做什么、为什么。1–3 句。）

## Changes

- （逐条列关键改动，可追溯到需求 / issue。）

## Verification

### 后端 (Java / Maven)
- [ ] `mvn -B -DskipTests compile` 通过
- [ ] 新增 / 修改的行为有测试覆盖
- [ ] 文档已更新（如有 API / 行为变更）

### 前端 (dc3-web)
- [ ] `pnpm lint-check && pnpm check && pnpm build` 通过
- [ ] 大范围改动通过 `pnpm run test:ci` 覆盖门槛
- [ ] E2E 数据动态创建并清理，无固定业务 ID
- [ ] 文档已更新（如有 UI / 行为变更）

## Impact

- （影响的模块、是否有 breaking change、是否需要 changelog）
