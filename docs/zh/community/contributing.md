---
title: 贡献指南
---

# 贡献指南

这页写给准备给 IoT DC3 提交代码、文档或反馈的贡献者：读完你会知道怎么搭好本地环境、一条改动从分支到 PR 该怎么走、提交信息为什么必须可读，以及合并前要跑哪些验证。

> 你在这里：想动手参与。写后端代码前先通读[开发概览与规范](../development/)（权威工程约定以仓库根的 `AGENTS.md` 为准）；想跑通验证看[测试](../development/testing)。

## 怎么参与

参与不止"写代码"。下面四类贡献都欢迎，价值同样真实：

- **报告可复现的缺陷**——附上日志、版本、配置和复现步骤，让维护者不用猜。
- **提议新功能**——说清目标场景、期望行为、以及对现有兼容性的影响。
- **改进文档**——补充示例、翻译、排错笔记；文档错一个字也值得提 PR。
- **提交代码**——聚焦的提交 + 测试或验证说明，每行改动都能追溯到一个需求。

::: tip 先开 Issue 再动手
较大的功能或会改变行为的改动，建议先开 Issue 对齐方案再写代码，避免做完才发现方向不符。小的修复可以直接提 PR。
:::

## 搭好本地开发环境

平台是 Java 21 / Spring Boot 4 的分布式服务，本地至少要起依赖栈（PostgreSQL + RabbitMQ）才能跑通。先准备工具链，再起依赖，最后让 Java 进程读到正确的运行时变量。

支持的工具链：

- JDK 21
- Maven 3.9+
- Podman 或 Docker
- Make（可选，但推荐）

从仓库根目录起本地依赖栈：

::: code-group

```bash [启动依赖栈]
make up-db          # PostgreSQL + RabbitMQ
make up-optional    # 可选栈：EMQX / ELK / Prometheus / Grafana
```

```bash [校验 compose]
podman compose -f dc3/docker-compose-db.yml config --quiet
```

:::

源码方式运行 Java 进程时，要把运行时变量注入到进程里——根目录的 `.env` 只服务 Docker Compose，**不会**自动注入本地 Java 进程：

::: code-group

```bash [Shell 运行 Java]
source dc3/env/dev.env.sh
```

```bash [准备 Compose 插值]
cp .env.example .env
```

:::

::: warning `.env` 和 `dev.env` 不是一回事
根目录 `.env`（由 `.env.example` 复制）只用于 Docker Compose 的变量插值；本地 IDE/CLI 跑 Java 必须用 `dc3/env/dev.env`（IDE EnvFile 插件读）或 `dc3/env/dev.env.sh`（`source` 进 shell）。四个文件的区别、以及 JetBrains IDEA 的用法见[环境变量详解](../quickstart/environment)。
:::

## 分支与 Pull Request

一次贡献从一个聚焦的分支开始，到一个聚焦的 PR 结束。把无关的重构、格式整理和行为改动分开，评审才跑得快。

- 除非维护者另有要求，从最新的 `main` 切出功能/修复分支。
- 分支名带语义，如 `feature/<name>/<topic>` 或 `fix/<name>/<topic>`。
- PR 提交到 `develop` 分支。
- 保持 PR 聚焦：不要把重构、格式整理和行为改动混进一个 PR，除非它们是同一个修复必需的。
- 在 PR 描述里引用相关 Issue。

## 提交信息：Conventional Commits

提交信息会被直接生成进发布说明（`dc3/doc/CHANGE.md` 由 git 历史生成），所以 subject 必须具体、可读。格式固定：

```text
<type>(<scope>): <english imperative summary>
```

- subject 用**英文、小写、祈使句**，足够具体以便写进发布说明。
- 允许的 type：`feat`、`fix`、`perf`、`refactor`、`docs`、`build`、`ci`、`test`、`chore`、`style`、`security`、`revert`。
- 非根级的微小改动尽量带 scope。
- 不要用 `update`、`fix`、`misc`、`wip`、`.` 这类弱 subject——它们会让发布说明无法读。

真实示例：

```text
fix(manager): validate tenant scope for device queries
docs(env): explain JetBrains IDEA environment variables
refactor(container): deduplicate compose registry overrides
```

::: warning subject 直接进发布说明
`dc3/doc/CHANGE.md` 由提交信息生成，弱 subject 会让发布说明无法读。提交前请对照上面的格式与真实示例自查 subject 是否具体、可读。
:::

## 合并前的验证

提 PR 前，按改动触及的范围跑对应检查——验证范围与改动成正比，不必每次全量。

::: code-group

```bash [Java / 共享行为]
mvn -s .mvn/settings.xml clean package
```

```bash [容器 / compose]
podman compose -f dc3/docker-compose-db.yml config
make config STACK=db    # 或 app/dev/optional，按触及的栈
```

:::

- **纯文档改动**：至少手动核对链接、命令和排版是否正确。
- **容器改动**：对每个改到的 compose 文件跑 `make config STACK=<app|dev|db|optional>` 或 `podman compose config`。
- **更多测试约定**（单元、集成、E2E、覆盖率门槛）见[测试](../development/testing)。

## 编码约定（要点）

完整规范以 `AGENTS.md` 为准，这里只列贡献时最常踩的几条。它们都不是风格偏好，而是平台正确性的硬约束。

- 沿用既有的包结构、命名、校验、异常、日志与 facade 模式，不引入新模式。
- **租户隔离是硬要求**：新增的查询、gRPC 调用、缓存键和数据变更都必须保留 `tenantId` 作用域。
- 对成组的配置，优先用带校验的类型化配置属性，而非散落的 `@Value`。
- 行为改动要带测试或聚焦的验证说明，尤其是共享 common 模块和跨服务契约。
- 不要提交密钥、本地生成文件、IDE 元数据或机器相关配置。

::: tip CRUD 动词随结果基数走
平台没有自由命名空间——方法名/HTTP 路径/gRPC RPC 的动词必须反映结果基数（查单条 `get`，查集合 `list`）。详见[开发概览与规范](../development/)。
:::

## 文档与翻译

改动根 README 内容时，保持 `README.md`、`README.zh.md`、`README.ja.md`、`README.vi.md` 结构对齐。若同一个 PR 内无法完成翻译同步，在 PR 描述里明确标注。

## 发布说明

打 tag 发布前，从 git 历史生成分类变更日志：

```bash
make changelog
```

默认它读取 `pom.xml` 里的当前版本，对比 `HEAD` 与最近可达的 `dc3.release.*` tag，更新 `dc3/doc/CHANGE.md`。需要时可覆盖范围或版本：

```bash
make changelog FROM=dc3.release.20251005.00 TO=HEAD VERSION=2026.5.22
```

::: info 变更日志专用提交
默认跳过"生成 changelog"这类发布提交，便于在提交 `CHANGE.md` 后重复运行保持稳定。只有当这些提交需要出现在发布说明里时，才设 `INCLUDE_CHANGELOG_COMMITS=true`。
:::

## 许可

IoT DC3 社区版基于 GNU Affero General Public License v3.0 or later 授权。授权声明见仓库根的 `LICENSE-AGPL.txt` 与 `LICENSE.txt`。

## 延伸阅读

- [开发概览与规范](../development/) — 工程权威约定：CRUD 动词、分层调用、facade 边界
- [测试](../development/testing) — 单元、集成、E2E 与覆盖率约定
- [环境变量详解](../quickstart/environment) — `.env` / `dev.env` / `dev.env.sh` 的区别与 IDE 用法
- [行为准则](./code-of-conduct) — 参与社区前请先读
- [安全策略](./security) — 如何负责任地报告安全漏洞
