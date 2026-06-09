# 项目文档机制

本页定义 IoT DC3 文档站的维护机制。目标是让用户、维护者和 AI 助手都能快速判断“应该读哪里、改哪里、如何验证”。

## 文档分层

| 层级 | 目录 | 目标 | 内容要求 |
|------|------|------|----------|
| 入口层 | `docs/index.md` | 让新读者理解项目定位和下一步 | 简洁、准确、少营销化 |
| 上手层 | `docs/quickstart/` | 帮用户跑起来 | 命令可复制，环境变量说清楚 |
| 使用层 | `docs/operation/`、`docs/guide/` | 帮用户完成实际任务和运维 | 按任务流组织，避免堆截图 |
| 架构层 | `docs/architecture/` | 解释系统边界和运行时关系 | 与当前代码模块保持一致 |
| 开发层 | `docs/development/` | 指导二次开发、测试、API 文档 | 与 `AGENTS.md` 工程规则一致 |
| 参考层 | `docs/modules/` | 列出模块、驱动和职责 | 以仓库目录为事实来源 |
| 工程资料层 | `docs/superpowers/` | 保存方案、分析、待办、历史资料 | 分类归档，标明适用范围 |

## 信息架构规则

1. 顶部导航只放稳定且面向读者的入口：快速开始、操作手册、使用指南、架构、开发、模块、Superpowers、社区。
2. 设计方案、历史分析、待办和旧版截图不直接暴露在顶层导航。
3. 公开文档应能独立回答用户问题，不要求用户阅读内部方案。
4. 同一事实只维护一个主来源，其他页面只摘要并链接过去。
5. 出现跨页面事实冲突时，按代码/配置文件、README、文档站的顺序校正。

## 事实来源

| 事实类型 | 主来源 | 需要同步的文档 |
|----------|--------|----------------|
| 依赖版本 | 根 `pom.xml` | 首页、开发文档、README |
| 本地运行环境变量 | `.env.example`、`dc3/env/dev.env`、`dc3/env/dev.env.sh` | 快速开始、环境变量 |
| Compose 命令 | `Makefile`、`dc3/docker-compose*.yml` | 快速开始、使用指南 |
| 驱动数量和名称 | `dc3-driver/` 目录 | 首页、模块清单、README、AI README |
| 服务和端口 | 服务配置、Compose 文件、环境变量 | 快速开始、架构、故障排查 |
| OpenAPI 暴露方式 | `dc3-common-web`、各中心 OpenAPI 配置、Gateway 配置 | API 文档 |
| 测试命令和覆盖率 | `Makefile`、`dc3-coverage`、CI workflow | 测试文档 |

## 文档生命周期

| 状态 | 放置位置 | 要求 |
|------|----------|------|
| 草案 | `docs/superpowers/design/` 或 `docs/superpowers/strategy/` | 写清背景、边界、未决项 |
| 待办线索 | `docs/superpowers/backlog/` | 用可追踪条目记录，不保留无上下文短句 |
| 已落地功能 | 公开文档对应目录 | 写成用户可执行步骤，避免只描述实现 |
| 历史资料 | `docs/superpowers/legacy-*` | 保留内容并标注历史来源和适用范围 |
| 过时资料 | 原地修正或迁移到历史资料 | 不在公开路径留下错误事实 |

## 页面写作规则

- 页面标题要说明任务或主题，例如“设备接入流程”，不要只写“说明”。
- 第一段说明读者能获得什么。
- 命令块默认从仓库根目录执行，例外必须写明目录。
- 本地源码运行必须提示加载 `dc3/env/dev.env.sh` 或在 IDE 配置 `dc3/env/dev.env`。
- 不使用 MkDocs Material 的特殊提示块、Material 图标短码和卡片网格语法；VitePress admonition 使用 `::: tip`、`::: warning`。
- 不用“完全领先”“一键无忧”“生产级闭环”等难验证措辞。
- 历史分析需要写日期和当前状态，不能把历史结论当作当前事实。

## 评审清单

提交文档变更前至少检查：

- 链接是否存在，尤其是移动目录后的相对链接。
- 版本号、驱动数量、服务端口是否与代码或配置一致。
- 快速开始命令是否符合 `Makefile` 当前目标。
- 公开文档是否把内部草案、待办和历史截图混入主路径。
- `README`、`README.ai.md` 与文档站是否在关键事实上一致。
- VitePress 构建是否通过。

## 验证命令

```bash
pnpm run build
git diff --check -- docs package.json .github/workflows/docs.yml
```

如果修改了 Compose 或 Java 配置，还要按 `AGENTS.md` 的验证矩阵补充 Maven、Compose 或 YAML 检查。
