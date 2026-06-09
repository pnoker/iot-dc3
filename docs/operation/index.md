# 操作手册

本章从用户任务流解释如何使用 IoT DC3：理解核心概念、接入设备、查看数据、下发命令，以及使用 Agentic Center 做 AI 辅助运营。

如果你还没有启动本地环境，先完成 [快速开始](../quickstart/)。

## 推荐路径

1. 阅读 [核心概念](concepts.md)，理解驱动、模板、设备、位号和数据之间的关系。
2. 按 [设备接入流程](device-onboarding.md) 完成一次虚拟驱动或真实驱动接入。
3. 按 [数据与命令](data-commands.md) 验证点位采集、历史查询和读写命令。
4. 如需接入大模型，阅读 [AI 辅助运营](agentic.md)。

## 运行入口

| 入口 | 用途 |
|------|------|
| Gateway API | `http://localhost:8000/api/v3/...`，对外统一 API 入口 |
| Swagger UI | `http://localhost:8000/swagger-ui.html`，开发环境下查看聚合 API |
| Center direct API | 各中心服务的直接调试入口，详见 [API 文档](../development/api-documentation.md) |
| Web UI | 前端源码在独立 `iot-dc3-web` 仓库，后端接口通过 Gateway 访问 |

## 旧版截图资料

旧版 Web UI 截图式手册已经归档到 [Superpowers / 旧版操作手册](../superpowers/legacy-operation/)。这些资料保留历史参考价值，但公开操作路径以本章文字流程为准。
