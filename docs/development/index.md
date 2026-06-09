# 开发

参与 IoT DC3 二次开发与协议扩展的入口。

## 目录

| 文档 | 内容 |
|------|------|
| [驱动开发](driver-authoring.md) | 基于 `dc3-driver-virtual` 模板扩展新协议驱动 |
| [API 文档](api-documentation.md) | OpenAPI / Swagger 的暴露方式、认证头和导出流程 |
| [测试](testing.md) | 单元测试、集成测试、E2E 和覆盖率约定 |
| [变更日志](changelog.md) | 版本演进与重要变更记录 |

## 开发约定速览

- **JDK 21、Maven 3.9+**：版本由 Maven Enforcer 强制
- **Spring Java Format**：提交前由 `spotless` 校验，CI 也会拦截不合规格式
- **分支模型**：从 `main` 切 `feature/<author>/<topic>`，PR 合入 `develop`，发布走 `release`
- **AI 协作**：仓库根目录 `AGENTS.md` 定义了 AI 助手协作规约（指令优先级、提交身份、变更日志规则）

## 相关入口

- [快速开始](../quickstart/index.md) — 本地拉起开发环境
- [架构](../architecture/index.md) — 模块分层与运行时数据流
- [贡献指南](../community/contributing.md) — PR 提交流程
