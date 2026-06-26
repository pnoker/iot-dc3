# Git 工作流与协作规范改造 — 设计方案

- 日期：2026-06-26
- 状态：待评审
- 范围：`iot-dc3`（后端）与 `iot-dc3-web`（前端）两个仓库统一适用
- 关系：本方案落地后，`git-branch-flow-rule` 记忆以此为准

## 1. 背景与问题

两个仓库当前镜像同一套模型，但存在五类实质缺陷：

1. **双合并反模式**：feature 同时合并到 develop 和 main（用户初始描述），导致两分支历史分叉、冲突双解、易不一致。无任何主流模型这么做。
2. **main/release 语义重叠**：后端 `tag.sh` 中 main 分支打的 tag 前缀是 `dc3.release.*`（main 与 release 当一类），证明两者角色未区分；两仓库默认分支都是 `release`，release 同时是部署分支。
3. **tag 命名非 semver 且混用**：git tag 是日期式（`dc3.release.20251005.00`），GitHub Release 名是 semver（`2025.9.3`），两套不一致；不符合开源惯例。
4. **前端无 tag/发版机制**：`iot-dc3-web` 的 Makefile 无 `tag` target、无 tag 脚本——发版缺口。
5. **CI 重复**：6 个 workflow（ci/test/e2e/security/codeql）全监听 `develop+release+main` 三分支，每个 feature 触发 3 倍 CI。
6. **模板缺失**：后端无 PR/Issue 模板；前端无 Issue 模板（前端有 PR 模板 19 行）。

## 2. 目标与非目标

**目标**

1. 统一为**简化 Git Flow**：main（生产+部署+tag）+ develop（集成）+ feature/hotfix，砍掉 release 长期分支（归档保留）。
2. feature 单线流入（消除双合并）：feature→develop→main。
3. tag 统一 semver（`vYYYY.M.P`），只在 main 打，前后端共用同一套机制。
4. CI 去重：feature 只在 develop PR 跑全量，main 跑精简回归，tag 驱动镜像发版。
5. 补齐 PR/Issue 模板、分支保护、CONTRIBUTING，两仓库一致。

**非目标**

- 不改后端 Java 代码 / 前端 Vue 代码的业务逻辑；只改协作基础设施（workflow、脚本、模板、文档、GitHub 设置）。
- 不引入 commitlint/husky 等新工具链（已有 `.githooks/commit-msg` 与 Conventional Commits，沿用）。
- 不做 CODEOWNERS（个人维护项目，自动指派意义不大）。

## 3. 分支模型（B1）

```
feature/*   从 develop 切 ──PR──► develop   (CI 全量门禁)
develop     验证通过 ──PR promote──► main
main        = 生产 + 部署 + tag         (docs.yml/docker-ci 触发)
hotfix/*    从 main 切 ──PR──► main + 回合并 develop
release     归档保留（只读，不再部署/合并），外部旧 PR 手动 retarget
```

**职责**

- **develop**：开发集成分支。feature 的唯一 PR 落点。CI 跑全量（lint/check/test/e2e/security）。
- **main**：生产主干。只接收来自 develop 的 promote PR（或 hotfix）。每次合并 = 一次发版（打 tag + 部署文档 + 触发镜像发版 workflow）。禁止直推。
- **feature/\***：从 develop 切，命名 `feature/<scope>` 或 `feat/<scope>`，PR 回 develop。
- **hotfix/\***：从 main 切，修复生产问题，PR 回 main 并打 tag，再回合并 develop。
- **release**：归档。GitHub 上标记 archived/只读，不再触发任何 workflow，不接收合并。

**分支保护（GitHub 设置）**

- main：要求 PR + CI 通过 + 禁止 force push + 禁止直推。
- develop：要求 PR + CI 通过。
- release：设为只读（restrict pushes），保留历史。

## 4. tag / 发版（semver，前后端统一）

**命名**：`vYYYY.M.P`（与 GitHub Release 名一致，如 `v2025.9.3`）。递增规则：

- patch：默认，每次 promote 发版 `v2025.9.3 → v2025.9.4`
- minor：`make tag minor` → `v2025.9.4 → v2025.10.0`
- major：`make tag major` → `v2025.10.0 → v2026.0.0`

**只打在 main 上**。develop 不打正式 tag。

**后端 `dc3/bin/tag.sh` 重写**：

- 只允许在 main 分支执行（其他分支拒绝）。
- 读取最近一个 `v*` tag，按 patch/minor/major 递增生成新 tag。
- `git tag` + `git push origin <tag>` + `gh release create <tag> --generate-notes`（自动 changelog）。
- 命令：`make tag [patch|minor|major]`（默认 patch）。

**前端补 tag 机制**：

- 新增 `iot-dc3-web/bin/tag.sh`（与后端同逻辑，prefix 可配置）。
- Makefile 加 `tag` target。
- 前后端 tag 脚本共享同一 semver 递增逻辑（可抽取到公共片段或各自维护同构脚本）。

**tag 触发的发版**（见 §5）：打 `v*` tag → docker-ci / docker-ci-web 构建并推送镜像。

## 5. CI 触发策略（去重）

| workflow | 当前触发 | 调整后 |
|---|---|---|
| `ci.yml` | push develop+release+main | push develop + push main |
| `test.yml` | 三分支 | push develop + push main |
| `e2e.yml` | 三分支 | **pull_request to develop**（仅 PR，省 main 重测） |
| `security.yml` | 三分支 | push develop + push main |
| `codeql.yml` | 三分支 | push develop + push main（+ schedule 周期扫） |
| `docs.yml` | push release（→ Pages） | **push main**（文档随主干持续部署） |
| `docker-ci.yml`（后端镜像） | tag `dc3.release.*` | **tag `v*`** |
| `docker-ci-web.yml`（前端镜像） | （确认中，推测 tag） | **tag `v*`** |

**净效果**：feature 的 CI 从 3× 降到 1×（只在 develop PR 跑全量）；main 只跑 ci/test/security 回归；镜像发版由 semver tag 驱动；文档随 main 持续部署。

**两仓库一致**：iot-dc3 与 iot-dc3-web 的 workflow 触发规则对齐（前端只有 ci/docker-ci-web，同步去 release）。

## 6. PR / Issue 模板

**PR 模板**（两仓库同一份，`.github/PULL_REQUEST_TEMPLATE.md`）：

```markdown
## What & Why
（这次改动做什么、为什么。1–3 句。）

## Changes
- （逐条列关键改动，可追溯到需求/issue。）

## Verification
- [ ] 本地验证通过（lint / type-check / test / build，按子项目）
- [ ] 新增/修改的行为有测试覆盖
- [ ] 文档已更新（如有 API/行为变更）

## Impact
- （影响的模块、是否有 breaking change、是否需 changelog）
```

前端已有 19 行模板，替换为这份统一版；后端新增。

**Issue 模板**（两仓库，`.github/ISSUE_TEMPLATE/`）：

- `bug-report.md`：环境（前后端版本/部署方式）、复现步骤、预期/实际、日志。
- `feature-request.md`：场景、期望、替代方案。
- `config.yml`：空白 issue 跳转链接（引导到 Discussions）。

## 7. CONTRIBUTING 更新

两仓库 CONTRIBUTING（后端 124 行 / 前端 62 行）增补/修订「分支与发版」一节：

- 分支模型（§3 图示）与 feature/develop/main/hotfix 职责。
- PR 流程：feature→develop（CI 全量）→ promote develop→main（发版）。
- tag/发版：`make tag [patch|minor|major]`，semver `vYYYY.M.P`，只在 main。
- commit 规范：沿用 Conventional Commits（已有，保留引用）。
- 外部贡献者指引：从 develop 切 feature，PR 到 develop（不是 main/release）。

## 8. 迁移步骤（砍 release，有序执行）

每个仓库独立执行，顺序：

1. **同步主干**：确保 develop 与 main 内容一致（develop→main PR；当前 docs 改动已在三分支，其余历史内容核对）。
2. **改 workflow 触发**：所有 `branches: [develop, release, main]` → `[develop, main]`；docs.yml `release→main`；docker-ci `dc3.release.* → v*`；e2e 改 PR 触发。
3. **重写 tag.sh（后端）+ 新增（前端）**：semver on main，`make tag`。
4. **加模板**：PR 模板（统一）+ Issue 模板（bug/feature/config）。
5. **更新 CONTRIBUTING**：两仓库写入分支/发版规范。
6. **设分支保护**：main/develop 规则（gh api 或 GitHub 设置）。
7. **改默认分支**：`release → main`（`gh repo edit --default-branch main`，需两仓库）。
8. **归档 release**：release 设只读，README/CONTRIBUTING 注明已归档、新工作走 develop/main。
9. **公告**：GitHub Release / Discussions 说明分支模型变更，外部 PR retarget 到 develop。

**执行节奏**：先在 `develop` 上做完所有改动（workflow/脚本/模板/文档），通过一个 PR 把 develop promote 到 main 触发首个新流程发版；改默认分支与归档 release 放最后（不可逆性最高，最后做）。

## 9. 两仓库改动清单

### iot-dc3（后端）

- `.github/workflows/`：ci/test/e2e/security/codeql 去 release；docs.yml release→main；docker-ci.yml tag→`v*`。
- `dc3/bin/tag.sh`：重写为 semver on main。
- `Makefile`：`tag` target 加 patch/minor/major 参数。
- `.github/PULL_REQUEST_TEMPLATE.md`：新增（统一版）。
- `.github/ISSUE_TEMPLATE/`：新增 bug-report/feature-request/config。
- `CONTRIBUTING.md`：更新分支与发版节。
- GitHub：默认分支 release→main；分支保护；release 归档。

### iot-dc3-web（前端）

- `.github/workflows/`：ci/docker-ci-web 去 release；docker-ci-web.yml tag→`v*`（确认当前触发）。
- `bin/tag.sh`：新增（与后端同构 semver 逻辑）。
- `Makefile`：新增 `tag` target。
- `.github/PULL_REQUEST_TEMPLATE.md`：替换为统一版。
- `.github/ISSUE_TEMPLATE/`：新增 bug-report/feature-request/config。
- `CONTRIBUTING.md`：更新分支与发版节。
- GitHub：默认分支 release→main；分支保护；release 归档。

## 10. 风险与回退

- **公网部署中断**：docs.yml 从 release 改 main 时，若 main 尚未就绪会断部署。缓解：先确保 main 内容完整、预览验证，再切触发；切之前 release 仍可兜底（归档在最后）。
- **外部 PR retarget**：改默认分支后，外部贡献者的 PR 可能指向 release。缓解：归档（非删除）+ CONTRIBUTING + Issue 模板引导 + 公告。
- **tag.sh 重写风险**：semver 递增逻辑出错可能打错 tag。缓解：脚本加 dry-run（`make tag --dry-run`）+ 首次手动验证。
- **镜像发版断裂**：docker-ci 触发从 `dc3.release.*` 改 `v*`，过渡期若仍打旧 tag 则不发版。缓解：tag.sh 与 docker-ci 同一 PR 改、同批上线。
- **回退**：归档（非删除）release 分支 + workflow 改动可 revert，迁移可回退到三分支状态（代价：历史多一笔 revert）。
