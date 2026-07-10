---
title: 技术栈
---

# 技术栈

这页汇总 IoT DC3 当前推荐和运行中的主要技术选型。精确版本以仓库内的 `pom.xml`、`dc3-web/package.json` 和
`docs/package.json` 为准；README 只保留面向新读者的摘要。

## 后端与中心服务

| 范围 | 技术 | 用途 |
|------|------|------|
| 语言与框架 | Java 21 · Spring Boot 4 · Spring Cloud 2025 | 网关、四个中心服务与驱动进程的运行基础 |
| AI 集成 | Spring AI 2.0 | Agentic Center 接入 OpenAI-compatible provider、Tool Calling 与 MCP 工作流 |
| Web 与 API | Spring WebFlux · Spring Security · springdoc-openapi | HTTP API、认证鉴权、聚合 API 文档 |
| 服务协作 | gRPC · Protobuf · Facade 接口 | 中心服务之间的强类型调用契约 |
| 构建 | Maven 3.9+ | 多模块构建、测试、打包和依赖版本管理 |

## 数据、消息与调度

| 范围 | 技术 | 用途 |
|------|------|------|
| 主存储 | PostgreSQL | 业务数据、租户、资源、设备模型与运行数据 |
| 时序与扩展 | TimescaleDB · AGE · pgvector | 位号历史、图能力和向量能力扩展 |
| ORM / 数据访问 | MyBatis-Plus | DO 层持久化访问与分页查询 |
| 消息总线 | RabbitMQ | 驱动与数据中心之间的异步值上报、命令下发与削峰 |
| 缓存与调度 | Caffeine · Quartz | 进程内缓存、定时任务与调度 |

## 前端、文档与自动化

| 范围 | 技术 | 用途 |
|------|------|------|
| Web 前端 | Vue 3 · TypeScript 6 · Vite 8 · Element Plus | `dc3-web/` 下的管理控制台 |
| 可视化 | AntV G2/G6 | 仪表盘图表与关系可视化 |
| 文档站 | VitePress · Mermaid | 当前 `docs/` 文档站、架构图和流程图 |
| CLI 自动化 | TypeScript · pnpm · Vitest | sibling `dc3-cli/` 项目，面向 Gateway 的命令行客户端 |
| 容器部署 | Podman · Docker Compose | 本地依赖、开发栈、应用栈和可选观测栈 |

## 继续阅读

- [从源码本地开发](../quickstart/) — 启动依赖、加载环境变量、构建与验证
- [前端开发](../frontend/) — `dc3-web/` 的运行、目录和测试命令
- [系统架构总览](../architecture/) — 网关、中心服务、驱动、消息总线和存储如何协作
- [模块地图](../architecture/modules) — Maven 模块、部署单元和依赖关系
