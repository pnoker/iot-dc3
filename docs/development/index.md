# 开发

参与 IoT DC3 二次开发与协议扩展的入口。

<div class="grid cards" markdown>

- :material-tools: **[驱动开发](driver-authoring.md)**

  基于 `dc3-driver-virtual` 模板从零搭建新协议驱动

- :material-test-tube: **[测试](testing.md)**

  单元测试 / 集成测试约定、Mock 策略、覆盖率门槛

- :material-history: **[变更日志](changelog.md)**

  版本演进与重要变更记录

</div>

## 开发约定速览

- **JDK 21、Maven 3.9+**：版本由 Maven Enforcer 强制
- **Spring Java Format**：提交前由 `spotless` 校验，CI 也会拦截不合规格式
- **分支模型**：从 `main` 切 `feature/<author>/<topic>`，PR 合入 `develop`，发布走 `release`
- **AI 协作**：仓库根目录 `AGENTS.md` 定义了 AI 助手协作规约（指令优先级、提交身份、变更日志规则）

## 相关入口

- [快速开始](../quickstart/index.md) — 本地拉起开发环境
- [架构](../architecture/index.md) — 模块分层与运行时数据流
- [贡献指南](../community/contributing.md) — PR 提交流程
