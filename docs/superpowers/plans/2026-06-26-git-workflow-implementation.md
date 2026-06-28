# Git 工作流与协作规范改造 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 iot-dc3 与 iot-dc3-web 统一改造为简化 Git Flow（main 生产+部署+tag、develop 集成、feature 单线、release 归档），tag
改 semver，CI 去重，补齐 PR/Issue 模板与分支保护。

**Architecture:** 每个仓库从 develop 切 `feature/git-workflow` 分支，在该分支上改 workflow/tag 脚本/模板/CONTRIBUTING，PR
回 develop；两仓库 develop 就绪后各自 promote 到 main，最后做不可逆的 GitHub 设置（默认分支 release→main、分支保护、release
归档）。

**Tech Stack:** GitHub Actions workflow（YAML）、Bash（tag.sh）、Make、gh CLI、GitHub REST API（分支保护/默认分支）。

**Spec:** `docs/superpowers/specs/2026-06-26-git-workflow-and-contributing-design.md`

## Global Constraints

- **两仓库统一**：iot-dc3（后端，`git@github.com:pnoker/iot-dc3.git`）与 iot-dc3-web（前端，
  `git@github.com:pnoker/iot-dc3-web.git`）同构改造。
- **分支模型 B1**：feature→develop（CI 全量）→main（发版）；release 归档只读。
- **tag**：semver `vYYYY.M.P`，只在 main 打；develop 不打正式 tag。
- **不改业务代码**：只动 workflow / 脚本 / 模板 / 文档 / GitHub 设置。
- **commit**：Conventional Commits，scope 用 `chore` 或 `ci`/`docs`（如 `chore(git): rewrite tag.sh to semver on main`）。
- **每仓库从 develop 切分支执行**，不直接在 develop/main 上提交。
- **bash 脚本**：`set -euo pipefail`；tag.sh 必须支持 `--dry-run`。
- **验证手段**：workflow 用 `actionlint`（若有）或 YAML 语法 + on 块核对；tag.sh 用 dry-run；前端
  `pnpm lint-check && pnpm check && pnpm build`；后端 `mvn -q -DskipTests compile` 不适用（无 Java 改动，跳过）。
- **不可逆操作放最后**：改默认分支、归档 release 在所有代码改动合并并 promote 到 main 之后。
- **GitHub 设置需 owner 权限**：用 `gh api` 或 `gh repo edit`；执行前确认 `gh auth status` 为 pnoker。

---

## 阶段一：后端 iot-dc3（feature/git-workflow 分支）

### Task 1: 后端 workflow 触发调整

**Files:**

- Modify: `iot-dc3/.github/workflows/ci.yml`（on.branches）
- Modify: `iot-dc3/.github/workflows/test.yml`（on.branches）
- Modify: `iot-dc3/.github/workflows/e2e.yml`（on 改 pull_request→develop）
- Modify: `iot-dc3/.github/workflows/security.yml`（on.branches）
- Modify: `iot-dc3/.github/workflows/codeql.yml`（on.branches）
- Modify: `iot-dc3/.github/workflows/docs.yml`（on.branches release→main）
- Modify: `iot-dc3/.github/workflows/docker-ci.yml`（on.tags `dc3.release.*`→`v*`）

**Interfaces:** 无（配置改动）。

**改动规则（逐文件 Read 后应用）：**

- `ci/test/security/codeql.yml`：`branches: [ develop, release, main ]` → `branches: [ develop, main ]`（出现几处替换几处）。
- `e2e.yml`：触发从 `push branches:[develop,release,main]` 改为 `pull_request branches:[develop]`（e2e 只在 PR 到 develop
  时跑）。
- `docs.yml`：`branches: [ release ]` → `branches: [ main ]`。
- `docker-ci.yml`：`tags: [ 'dc3.release.*' ]` → `tags: [ 'v*' ]`。

- [ ] **Step 1: 切分支**

```bash
cd iot-dc3 && git checkout develop && git pull && git checkout -b feature/git-workflow
```

- [ ] **Step 2: 逐文件 Read 确认 on 段，按规则 Edit**
- [ ] **Step 3: YAML 语法校验**

```bash
for f in ci test e2e security codeql docs docker-ci; do python3 -c "import yaml,sys; yaml.safe_load(open('iot-dc3/.github/workflows/$f.yml'))" && echo "$f.yml OK"; done
```

Expected: 全部 OK。

- [ ] **Step 4: grep 确认无残留 release 触发**

```bash
grep -rn "release" iot-dc3/.github/workflows/ | grep -v "name:" | grep -v "#"
```

Expected: 无 `branches: [... release ...]` 或 `dc3.release.*` 残留（注释/文案里的 release 可接受）。

- [ ] **Step 5: commit**

```bash
git -C iot-dc3 add .github/workflows && git -C iot-dc3 commit -m "ci: drop release branch, rewire triggers to develop/main and v* tags"
```

### Task 2: 后端 tag.sh 重写（semver on main）+ Makefile

**Files:**

- Modify: `iot-dc3/dc3/bin/tag.sh`（重写）
- Modify: `iot-dc3/Makefile`（tag target 加参数）

**Interfaces:** 产出 `make tag [patch|minor|major] [--dry-run]`。

**tag.sh 完整新内容：**

```bash
#!/usr/bin/env bash
set -euo pipefail

# semver tag on main: vYYYY.M.P
# usage: make tag [patch|minor|major] [--dry-run]

bump="${1:-patch}"
dryrun=0
for arg in "$@"; do [ "$arg" = "--dry-run" ] && dryrun=1; done
case "$bump" in patch|minor|major) ;; *) echo "unknown bump: $bump (patch|minor|major)"; exit 1;; esac

branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$branch" != "main" ]; then
  echo "tagging only allowed on main (now: $branch). switch to main first." >&2
  exit 1
fi

git pull --tags --quiet
last=$(git tag -l "v*" --sort=-v:refname | head -1)
if [ -z "$last" ]; then echo "no v* tag found; set an initial tag manually, e.g. v2025.9.0"; exit 1; fi
# parse vYYYY.M.P
re='^v([0-9]+)\.([0-9]+)\.([0-9]+)$'
[[ $last =~ $re ]] || { echo "unparseable last tag: $last"; exit 1; }
major=${BASH_REMATCH[1]}; minor=${BASH_REMATCH[2]}; patch=${BASH_REMATCH[3]}
case "$bump" in
  patch) patch=$((patch+1));;
  minor) minor=$((minor+1)); patch=0;;
  major) major=$((major+1)); minor=0; patch=0;;
esac
newtag="v${major}.${minor}.${patch}"
echo "last=$last -> new=$newtag"
[ "$dryrun" = "1" ] && { echo "(dry-run, not tagging)"; exit 0; }

git tag "$newtag"
git push origin "$newtag"
gh release create "$newtag" --generate-notes --title "$newtag"
```

**Makefile tag target（替换现有 `tag:` 段）：**

```make
tag:
	@dc3/bin/tag.sh $(filter-out $@,$(MAKECMDGOALS))
%: ; @:
```

（保留 `make tag` 默认 patch；支持 `make tag minor`。）

- [ ] **Step 1: Write tag.sh**（上述内容，`chmod +x`）
- [ ] **Step 2: Edit Makefile tag target**
- [ ] **Step 3: dry-run 验证（在 develop 上测脚本逻辑，应拒绝并提示）**

```bash
cd iot-dc3 && bash dc3/bin/tag.sh patch --dry-run
```

Expected: `tagging only allowed on main (now: feature/git-workflow)...` 退出码 1（验证分支守卫生效）。

- [ ] **Step 4: 临时切 main 测 dry-run（本地，不 push）**

```bash
git stash -u 2>/dev/null; git checkout main 2>/dev/null && bash dc3/bin/tag.sh patch --dry-run; git checkout feature/git-workflow; git stash pop 2>/dev/null
```

Expected: 打印 `last=v... -> new=v...` + `(dry-run, not tagging)`，不实际打 tag。

- [ ] **Step 5: commit**

```bash
git add dc3/bin/tag.sh Makefile && git commit -m "chore(git): rewrite tag.sh to semver on main with dry-run"
```

### Task 3: 后端 PR + Issue 模板

**Files:**

- Create: `iot-dc3/.github/PULL_REQUEST_TEMPLATE.md`
- Create: `iot-dc3/.github/ISSUE_TEMPLATE/bug-report.md`
- Create: `iot-dc3/.github/ISSUE_TEMPLATE/feature-request.md`
- Create: `iot-dc3/.github/ISSUE_TEMPLATE/config.yml`

**PR 模板内容**（spec §6 统一版）：

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

**bug-report.md**：

```markdown
---
name: Bug Report
about: 报告一个缺陷
labels: ["bug"]
---
## 环境
- 后端版本（git tag / Docker 镜像 tag）：
- 前端版本：
- 部署方式（Docker / 源码 / 其他）：

## 复现步骤
1.

## 预期 / 实际
- 预期：
- 实际：

## 日志 / 截图
```

**feature-request.md**：

```markdown
---
name: Feature Request
about: 提一个功能建议
labels: ["enhancement"]
---
## 场景
（你想解决什么问题。）
## 期望
（你希望平台怎么做。）
## 替代方案
（你考虑过的其他做法。）
```

**config.yml**：

```yaml
blank_issues_enabled: false
contact_links:
  - name: 讨论 / 提问
    url: https://github.com/pnoker/iot-dc3/discussions
    about: 使用问题与想法请到 Discussions
```

- [ ] **Step 1: 创建 4 个文件**（上述内容）
- [ ] **Step 2: 校验 config.yml 语法**

```bash
python3 -c "import yaml; yaml.safe_load(open('iot-dc3/.github/ISSUE_TEMPLATE/config.yml'))" && echo OK
```

- [ ] **Step 3: commit**

```bash
git add .github/PULL_REQUEST_TEMPLATE.md .github/ISSUE_TEMPLATE && git commit -m "docs: add PR and issue templates"
```

### Task 4: 后端 CONTRIBUTING 更新

**Files:**

- Modify: `iot-dc3/CONTRIBUTING.md`（增补「分支与发版」节）

**增补内容（插入到合适位置，如提交规范之后）：**

```markdown
## 分支与发版

IoT DC3 采用简化 Git Flow：

- `develop`：开发集成分支。新功能从 `develop` 切 `feature/<scope>` 分支，PR 回 `develop`（CI 全量门禁）。
- `main`：生产主干。`develop` 验证通过后以 PR promote 到 `main`；每次合并到 `main` 即为一次发版。
- `hotfix/<scope>`：从 `main` 切，修复生产问题，PR 回 `main` 并打 tag，再回合并 `develop`。
- `release`：已归档（只读），不再接收合并或部署。新工作请走 `develop`/`main`。

**发版（打 tag）**：在 `main` 分支执行 `make tag [patch|minor|major]`（默认 patch），生成 semver tag `vYYYY.M.P` 并创建 GitHub Release。tag 推送后自动触发 Docker 镜像发布。

**外部贡献者**：请从 `develop` 切 feature 分支，PR 提交到 `develop`（不要提交到 `main` 或 `release`）。
```

- [ ] **Step 1: Read CONTRIBUTING.md 定位插入点**
- [ ] **Step 2: Edit 插入「分支与发版」节**
- [ ] **Step 3: commit**

```bash
git add CONTRIBUTING.md && git commit -m "docs: document branch model and release flow in CONTRIBUTING"
```

### Task 5: 后端 spec 文件纳入 + 推送 + PR

- [ ] **Step 1: 把本 spec 与 plan 文件加入该分支**（它们目前在 develop 工作区，需带进 feature 分支）

```bash
git -C iot-dc3 add docs/superpowers/specs/2026-06-26-git-workflow-and-contributing-design.md docs/superpowers/plans/2026-06-26-git-workflow-implementation.md
git -C iot-dc3 commit -m "docs: add git workflow design spec and implementation plan"
```

- [ ] **Step 2: 推送 + 开 PR 到 develop**

```bash
git -C iot-dc3 push -u origin feature/git-workflow
gh pr create --repo pnoker/iot-dc3 --base develop --head feature/git-workflow \
  --title "chore(git): unify branch model, semver tags, CI trim, templates" \
  --body "见 docs/superpowers/specs/2026-06-26-git-workflow-and-contributing-design.md。砍 release(归档)、main 成生产+部署+tag、feature→develop→main、tag 改 semver、CI 去重、补 PR/Issue 模板与 CONTRIBUTING。"
```

- [ ] **Step 3: 合并 PR（merge commit）**

```bash
PR=$(gh pr list --repo pnoker/iot-dc3 --head feature/git-workflow --base develop --json number --state open | python3 -c "import json,sys;print(json.load(sys.stdin)[0]['number'])")
gh pr merge "$PR" --repo pnoker/iot-dc3 --merge
```

---

## 阶段二：前端 iot-dc3-web（feature/git-workflow 分支）

### Task 6: 前端 workflow 触发调整

**Files:**

- Modify: `iot-dc3-web/.github/workflows/ci.yml`（on.branches 去 release）
- Modify: `iot-dc3-web/.github/workflows/docker-ci-web.yml`（on.tags `dc3.release.*`→`v*`）

**改动规则：** 前端 ci.yml 的 branches `[develop, release, main]` → `[develop, main]`；docker-ci-web.yml
`tags: 'dc3.release.*'` → `tags: 'v*'`。

- [ ] **Step 1: 切分支**

```bash
cd iot-dc3-web && git checkout develop && git pull && git checkout -b feature/git-workflow
```

- [ ] **Step 2: Read + Edit 两 workflow 的 on 段**
- [ ] **Step 3: YAML 校验**

```bash
for f in ci docker-ci-web; do python3 -c "import yaml; yaml.safe_load(open('iot-dc3-web/.github/workflows/$f.yml'))" && echo "$f.yml OK"; done
```

- [ ] **Step 4: commit**

```bash
git -C iot-dc3-web add .github/workflows && git commit -m "ci: drop release branch, rewire docker tag to v*"
```

### Task 7: 前端 tag 机制新增

**Files:**

- Create: `iot-dc3-web/bin/tag.sh`（与后端同构 semver 逻辑）
- Modify: `iot-dc3-web/Makefile`（新增 tag target）

**tag.sh**：内容同 Task 2 的后端 tag.sh（逐字复制；前端无需 dc3/ 前缀，放 `bin/tag.sh`）。

**Makefile 新增（加到 .PHONY 与 target 列表 + help）：**

- `.PHONY` 列表加 `tag`
- help 加 `'make tag [patch|minor|major] - create semver release tag on main'`
- target：

```make
tag:
	@bin/tag.sh $(filter-out $@,$(MAKECMDGOALS))
%: ; @:
```

- [ ] **Step 1: 创建 bin/tag.sh**（chmod +x，内容同 Task 2）
- [ ] **Step 2: Edit Makefile 加 tag target + .PHONY + help 行**
- [ ] **Step 3: dry-run 验证分支守卫**

```bash
cd iot-dc3-web && bash bin/tag.sh --dry-run
```

Expected: 拒绝（非 main）。

- [ ] **Step 4: commit**

```bash
git add bin/tag.sh Makefile && git commit -m "chore(git): add semver tag script and make tag target"
```

### Task 8: 前端 PR（替换）+ Issue 模板

**Files:**

- Modify: `iot-dc3-web/.github/PULL_REQUEST_TEMPLATE.md`（替换为统一版）
- Create: `iot-dc3-web/.github/ISSUE_TEMPLATE/bug-report.md`
- Create: `iot-dc3-web/.github/ISSUE_TEMPLATE/feature-request.md`
- Create: `iot-dc3-web/.github/ISSUE_TEMPLATE/config.yml`

**内容：** PR 模板用 Task 3 的统一版；Issue 模板同 Task 3（config.yml 的 discussions 链接指向 iot-dc3-web 仓库）。

- [ ] **Step 1: 替换 PR 模板 + 创建 3 个 Issue 模板**
- [ ] **Step 2: commit**

```bash
git add .github/PULL_REQUEST_TEMPLATE.md .github/ISSUE_TEMPLATE && git commit -m "docs: unify PR template, add issue templates"
```

### Task 9: 前端 CONTRIBUTING 更新

**Files:**

- Modify: `iot-dc3-web/CONTRIBUTING.md`（增补「分支与发版」节，内容同 Task 4，镜像 iot-dc3-web 仓库）

- [ ] **Step 1: Read + Edit 插入「分支与发版」节**
- [ ] **Step 2: commit**

```bash
git add CONTRIBUTING.md && git commit -m "docs: document branch model and release flow in CONTRIBUTING"
```

### Task 10: 前端推送 + PR 到 develop

- [ ] **Step 1: 推送 + 开 PR**

```bash
git -C iot-dc3-web push -u origin feature/git-workflow
gh pr create --repo pnoker/iot-dc3-web --base develop --head feature/git-workflow \
  --title "chore(git): unify branch model, semver tags, CI trim, templates" \
  --body "与 iot-dc3 同构：砍 release(归档)、main 生产+部署+tag、semver tag、CI 去重、补 tag 机制与模板。"
```

- [ ] **Step 2: 合并（merge commit）**

```bash
PR=$(gh pr list --repo pnoker/iot-dc3-web --head feature/git-workflow --base develop --json number --state open | python3 -c "import json,sys;print(json.load(sys.stdin)[0]['number'])")
gh pr merge "$PR" --repo pnoker/iot-dc3-web --merge
```

---

## 阶段三：GitHub 设置（不可逆，两仓库 develop 合并后执行）

> 前置：阶段一 Task 5、阶段二 Task 10 均已合并到各自 develop。

### Task 11: promote develop → main（两仓库）

- [ ] **Step 1: 后端 develop→main PR + 合并**

```bash
gh pr create --repo pnoker/iot-dc3 --base main --head develop --title "Sync develop into main (git workflow overhaul)" --body "Bring main in line with develop after the git workflow refactor."
PR=$(gh pr list --repo pnoker/iot-dc3 --head develop --base main --json number --state open | python3 -c "import json,sys;print(json.load(sys.stdin)[0]['number'])")
gh pr merge "$PR" --repo pnoker/iot-dc3 --merge
```

- [ ] **Step 2: 前端 develop→main PR + 合并**（同上，--repo pnoker/iot-dc3-web）
- [ ] **Step 3: 确认两仓库 main 已含新 workflow/tag.sh/模板**

```bash
for r in iot-dc3 iot-dc3-web; do echo "$r main tag.sh: $(gh api repos/pnoker/$r/contents/$( [ $r = iot-dc3 ] && echo dc3/bin/tag.sh || echo bin/tag.sh )?ref=main --jq .name 2>&1)"; done
```

Expected: 两仓库 main 上 tag.sh 存在。

### Task 12: 分支保护（main / develop，两仓库）

- [ ] **Step 1: 后端 main 保护**

```bash
gh api -X PUT repos/pnoker/iot-dc3/branches/main/protection -f required_pull_request_reviews.required_approving_review_count=0 -F required_pull_request_reviews.dismiss_stale_reviews=false -F required_status_checks.strict=true -F required_status_checks.contexts[]='ci' -F enforce_admins=false -F restrictions= -F allow_force_pushes=false
```

（个人项目 review count=0；要求 CI 'ci' 通过；禁 force push。）

- [ ] **Step 2: 后端 develop 保护**（同上，branches/develop，可放宽 strict=false）
- [ ] **Step 3: 前端 main + develop 保护**（同 Step 1/2，--repo pnoker/iot-dc3-web，status check context 用前端实际名，先
  `gh run list` 确认）
- [ ] **Step 4: 验证保护生效**

```bash
gh api repos/pnoker/iot-dc3/branches/main/protection --jq .required_pull_request_reviews
```

Expected: 返回非 null 的 PR review 配置。

### Task 13: 改默认分支 release → main（两仓库，不可逆点）

- [ ] **Step 1: 后端**

```bash
gh repo edit pnoker/iot-dc3 --default-branch main
```

- [ ] **Step 2: 前端**

```bash
gh repo edit pnoker/iot-dc3-web --default-branch main
```

- [ ] **Step 3: 确认**

```bash
for r in iot-dc3 iot-dc3-web; do echo "$r default: $(gh repo view pnoker/$r --json defaultBranchRef --jq .defaultBranchRef.name)"; done
```

Expected: 均为 main。

### Task 14: 归档 release 分支（两仓库，只读）

- [ ] **Step 1: release 设为只读（restrict pushes，仅 owner）**

```bash
for r in iot-dc3 iot-dc3-web; do
  gh api -X PUT repos/pnoker/$r/branches/release/protection -F required_status_checks= -F enforce_admins=true -f restrictions.users[]=pnoker -f restrictions.teams[]= 2>&1 | head -1
done
```

（限制只有 pnoker 能 push；或用 `restrictions` 限定。enforce_admins=true 锁死。）

- [ ] **Step 2: 在两仓库 README/CONTRIBUTING 顶部加归档提示**（已由 Task 4/9 的 CONTRIBUTING「release 已归档」覆盖；可选额外加
  README badge）
- [ ] **Step 3: 确认 release 不再触发 workflow**（去 release 监听已在 Task 1/6 完成；此处核对 grep 无残留）

### Task 15: 首个 semver tag 验证（dry-run，两仓库）

- [ ] **Step 1: 后端**

```bash
cd iot-dc3 && git checkout main && git pull && bash dc3/bin/tag.sh patch --dry-run && git checkout develop
```

Expected: `last=<上一个 v* 或提示无 v* tag>` → `new=vYYYY.M.P` + dry-run 不打 tag。

- [ ] **Step 2: 前端**

```bash
cd iot-dc3-web && git checkout main && git pull && bash bin/tag.sh patch --dry-run && git checkout develop
```

- [ ] **Step 3: 若历史无 v* tag（全是 dc3.release.* 日期式），手动定一个初始 semver 基线 tag**

```bash
# 仅当上一步提示 "no v* tag found"：
# 后端: git tag v2025.9.3（对齐最近 GitHub Release 名） && git push origin v2025.9.3
# 前端: 同上
```

（此步需与用户确认基线版本号。）

### Task 16: 更新记忆 + 公告（可选）

- [ ] **Step 1: 更新 `git-branch-flow-rule` 记忆**为 B1 模型（main 主干+部署+tag、develop 集成、feature→develop→main、release
  归档、semver）。
- [ ] **Step 2: 在 iot-dc3 Discussions 发公告**说明分支模型变更（外部 PR retarget 到 develop）。

---

## Self-Review

**Spec 覆盖：**

- §3 分支模型 B1 → T11(promote)、T12(保护)、T13(默认)、T14(归档) ✅
- §4 tag semver → T2(后端 tag.sh)、T7(前端)、T15(验证) ✅
- §5 CI 去重 → T1(后端 workflow)、T6(前端) ✅
- §6 模板 → T3(后端)、T8(前端) ✅
- §7 CONTRIBUTING → T4(后端)、T9(前端) ✅
- §8 迁移步骤 → 阶段三 T11-T15 ✅
- §9 两仓库清单 → 阶段一/二全覆盖 ✅

**占位符扫描：** T12 Step 3 的前端 status check context 标注"先 gh run list 确认"——执行动作（非 TBD），可接受。T15 Step 3 初始
tag"需与用户确认"——明确的执行决策点，非占位。无其他 TBD/TODO。

**一致性：** tag.sh 在 T2(后端) 与 T7(前端) 内容一致（同构）；模板在 T3 与 T8 一致；CONTRIBUTING 增补段在 T4 与 T9 一致。分支名
`feature/git-workflow` 两仓库统一。

**风险已识别：** 阶段三不可逆，放最后；T15 历史无 v* tag 的基线确认点已标注。
