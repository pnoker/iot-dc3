# AI 辅助运营

Agentic Center 把 OpenAI-compatible 大模型接入 IoT DC3 的设备、数据和命令流程，用于自然语言查询、辅助分析和受控操作。

## 能力边界

| 能力 | 说明 |
|------|------|
| 会话 | 保存多轮对话上下文，可按配置启用持久化记忆 |
| 模型提供方 | 支持 OpenAI-compatible API，具体模型和密钥由配置决定 |
| 工具调用 | 在权限和工具边界内查询设备、点位、数据或触发操作 |
| 附件 | 支持上传附件并由 Agentic Center 管理存储路径 |

AI 能力不是设备接入的前置条件。建议先跑通设备、点位、数据和命令链路，再启用 Agentic Center。

## 配置来源

Agentic 的模型提供方通常存储在数据库配置中。环境变量中的 fallback 配置只作为没有可用数据库模型配置时的兜底：

| 变量 | 用途 |
|------|------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL` | OpenAI-compatible API 地址 |
| `AGENTIC_FALLBACK_OPENAI_API_KEY` | fallback API key |
| `AGENTIC_FALLBACK_OPENAI_MODEL` | fallback 模型名 |
| `AGENTIC_TOOL_CALLING_ENABLED` | 是否启用工具调用 |
| `AGENTIC_MEMORY_ENABLED` | 是否启用会话记忆 |
| `AGENTIC_ATTACHMENT_STORAGE_PATH` | 附件存储目录 |

完整变量说明见 [环境变量](../quickstart/environment.md)。

## 使用前检查

1. Agentic Center 已启动并可通过 Gateway 访问。
2. Auth、Manager、Data 的基础能力正常。
3. 至少有一个设备和点位产生数据。
4. 模型提供方可访问，API key 未写入文档或日志。
5. 工具调用只开放给可信用户和明确的业务场景。

## 安全建议

- 不在文档、截图、日志或 issue 中暴露 API key、token、password。
- 对会触发设备写入或控制动作的工具保持显式授权。
- 在生产环境中限制模型可见的数据范围，保持租户隔离。
- 对 AI 生成的操作建议保留审计记录和人工确认机制。

## 相关文档

- [核心概念](concepts.md)
- [数据与命令](data-commands.md)
- [API 文档](../development/api-documentation.md)
- [环境变量](../quickstart/environment.md)
