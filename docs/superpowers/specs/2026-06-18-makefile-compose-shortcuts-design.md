# Makefile compose 命令快捷体系

> 日期：2026-06-18 ｜ 范围：`iot-dc3/Makefile` + 相关文档

## 背景与动机

`iot-dc3/Makefile` 的 compose 类操作受三个维度影响：

| 维度                   | 取值                                | 作用                                 |
|----------------------|-----------------------------------|------------------------------------|
| `STACK`              | `dev` / `app` / `db` / `optional` | 选哪个 compose 文件                     |
| `REGISTRY`           | `auto` / `global` / `cn`          | 镜像从哪个仓库拉（global=Docker Hub，cn=阿里云） |
| `SERVICES` / `GROUP` | 服务过滤                              | 操作哪些服务                             |

但目前只有 `dev-db`、`dev-optional` 两个快捷入口，且都写死、不带 registry 选择。要切换 cn/global 只能
`make dev-db REGISTRY=cn` 手动传环境变量，体验差。

目标：为**所有** compose 操作建立统一的、命令名里就能表达 `stack`+`registry` 的快捷体系，无需手动传环境变量。

> 注：GNU make 的 target 名里冒号是语法保留字，`dev:db:cn` 无法作为 target，故采用连字符命名 `dev-db-cn` 形式。

## 命名约定

```
<op>-<stack>[-<registry>]          连字符分隔
  op       : up down stop ps logs build pull restart refresh config reset
  stack    : dev app db optional
  registry : cn global   ← 仅 up/pull/build/refresh（拉/建镜像的 op）可加
```

- 不带 registry 后缀 → 沿用现状 `REGISTRY=auto`（读 `.env`）。
- 生成数量：`11 op × 4 stack` = 44，加 `4 拉镜像 op × 4 stack × 2 registry` = 32，**共 76 个**，全部用 `eval`/`foreach`
  自动生成，零手写。

示例：

```bash
make up-db-cn         # = up      STACK=db  REGISTRY=cn
make up-app-global    # = up      STACK=app REGISTRY=global
make logs-dev         # = logs    STACK=dev
make down-optional    # = down    STACK=optional
make pull-app-cn      # = pull    STACK=app REGISTRY=cn
make restart-db       # = restart STACK=db
make config-db        # = config  STACK=db
make up-db-cn SERVICES="postgres"   # 仍可叠加服务过滤
```

## 实现：递归复用，零逻辑重复

生成的 target 不重新实现逻辑，而是递归 `$(MAKE) <op> STACK=… REGISTRY=…` 调用已有基础规则（参数化现有 `dev-db` 的转发写法）。

```makefile
# <op>-<stack>  →  <op> STACK=<stack>，REGISTRY 沿用当前值
define gen_stack_target
.PHONY: $(1)-$(2)
$(1)-$(2):
	@$$(MAKE) $(1) STACK=$(2) SERVICES='$$(SERVICES)' GROUP='$$(GROUP)' REGISTRY='$$(REGISTRY)' COMPOSE='$$(COMPOSE)' COMPOSE_DIR='$$(COMPOSE_DIR)' $$(MAKE_COMPOSE_OVERRIDE)
endef

# <op>-<stack>-<registry>  →  <op> STACK=<stack> REGISTRY=<registry>
define gen_stack_registry_target
.PHONY: $(1)-$(2)-$(3)
$(1)-$(2)-$(3):
	@$$(MAKE) $(1) STACK=$(2) SERVICES='$$(SERVICES)' GROUP='$$(GROUP)' REGISTRY=$(3) COMPOSE='$$(COMPOSE)' COMPOSE_DIR='$$(COMPOSE_DIR)' $$(MAKE_COMPOSE_OVERRIDE)
endef

OPS          := up down stop ps logs build pull restart refresh config reset
REGISTRY_OPS := up pull build refresh
STACKS       := dev app db optional
REGISTRIES   := cn global

$(foreach op,$(OPS),$(foreach st,$(STACKS),$(eval $(call gen_stack_target,$(op),$(st)))))
$(foreach op,$(REGISTRY_OPS),$(foreach st,$(STACKS),$(foreach rg,$(REGISTRIES),$(eval $(call gen_stack_registry_target,$(op),$(st),$(rg))))))
```

直接收益：

- `COMPOSE_FILE` 解析、`SELECTED_SERVICES`、`dc3_compose` 全自动复用。
- `reset-db` 等**自动继承** `reset` 规则里的 `CONFIRM_RESET_VOLUMES` 保护。
- `SERVICES`/`GROUP` 仍可叠加，默认空=全部服务。
- 加新 stack / op 只需改一个列表变量。

## 删除旧名

- 移除 `dev-db` / `dev-optional` 两个 target 规则。
- 从 `.PHONY` 行移除 `dev-db dev-optional`（生成的快捷名各自在模板内声明 `.PHONY`）。

## help 重写

新增"Compose shortcuts"一节，说明命名规则 + 取值表 + 例子；`Examples` 区把 `make dev-db` 改为 `make up-db` 等。形如：

```
Compose shortcuts (auto-generated):
  make <op>-<stack>[-<registry>]
    op       : up down stop ps logs build pull restart refresh config reset
    stack    : dev app db optional
    registry : cn global   (only up/pull/build/refresh)
  e.g.  make up-db-cn   make logs-dev   make pull-app-global   make down-optional
```

## 文档同步（13 文件）

把已删除的 `make dev-db` / `make dev-optional` 替换为 `make up-db` / `make up-optional`：

- `github/AGENTS.md`
- `github/CLAUDE.md`
- `iot-dc3/.claude/skills/dc3-stack-test/SKILL.md`
- `iot-dc3/AGENTS.md`
- `iot-dc3/CONTRIBUTING.md`
- `iot-dc3/dc3/doc/USAGE.md`
- `iot-dc3/docs/guide/troubleshooting.md`
- `iot-dc3/docs/quickstart/environment.md`
- `iot-dc3/docs/quickstart/index.md`
- `iot-dc3/README.ja.md`
- `iot-dc3/README.md`
- `iot-dc3/README.vi.md`
- `iot-dc3/README.zh.md`

规则：

- **必须**替换被删的 `dev-db` / `dev-optional`（否则文档示例会报 "No rule to make target"）。
- `make up STACK=db` / `REGISTRY=cn` 这类写法**仍有效**（基础 op 保留），不强制替换；仅在主 README / AGENTS 的"快速开始"
  处主推新快捷命令。
- `.env.example` 的 `REGISTRY=cn` 注释保留（机制不变）。

## 非目标（YAGNI）

- 不做跨 stack 聚合命令（一条命令同时起 db+optional+dev）。
- 不引入交互式选择。
- `REGISTRY=auto` 读 `.env` 的现有机制不变。

## 验证

- `make -n up-db-cn` → 展开含 `STACK=db REGISTRY=cn`。
- `make -n logs-dev` → 展开含 `STACK=dev`，且不含 `REGISTRY=cn/global`（无 registry 后缀）。
- `make config-db` 实跑能渲染 db compose 配置。
- `make reset-db`（无 `CONFIRM_RESET_VOLUMES=true`）被拒绝。
- `grep -rn "make dev-db\|make dev-optional" <13 文件>` 无残留。
