## What & Why / 改动与原因

（这次改动做什么、为什么。1–3 句。 / What this change does and why. 1–3 sentences.）

## Changes / 主要改动

- （逐条列关键改动，可追溯到需求 / issue。 / List key changes, traceable to a requirement / issue.）

## Verification / 验证

### 后端 / Backend (Java / Maven)

- [ ] `mvn -B -DskipTests compile` 通过 / passes
- [ ] 新增 / 修改的行为有测试覆盖 / new or changed behavior is covered by tests
- [ ] 文档已更新（如有 API / 行为变更） / docs updated (if API / behavior changed)

### 前端 / Frontend (dc3-web)

- [ ] `pnpm lint-check && pnpm check && pnpm build` 通过 / passes
- [ ] 大范围改动通过 `pnpm run test:ci` 覆盖门槛 / large changes meet the `pnpm run test:ci` coverage gate
- [ ] E2E 数据动态创建并清理，无固定业务 ID / E2E data is created and cleaned up dynamically, no hard-coded business IDs
- [ ] 文档已更新（如有 UI / 行为变更） / docs updated (if UI / behavior changed)

## Impact / 影响

- （影响的模块、是否有 breaking change、是否需要 changelog / affected modules, breaking changes, changelog needed）
