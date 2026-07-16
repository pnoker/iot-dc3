---
title: 自动化
---

<script setup>
import AutomationIndexDiagram from '../../.vitepress/theme/components/AutomationIndexDiagram.vue'
</script>


# 自动化

确定性的、可重复的程序化操作，用 `dc3` CLI 完成——不涉及大模型，结果可预测、可脚本化。AI 栏目（Agentic 中心、MCP）解决"
让模型决策"，自动化栏目解决"让人或脚本执行"。

## dc3 CLI

`dc3` CLI 是一个独立的 TypeScript 命令行客户端（Node ≥ 20），通过 HTTP 网关与运行中的后端通信，自身不耦合 Java
构建。它封装了三段式登录、Token 自动续期与凭据存储，让你直接面对按结果基数命名的子命令：`dc3 device list`、
`dc3 point history`、`dc3 driver add`。

适合三类场景：

- **本地调试**——终端里快速读一个位号值、看某台设备状态、下发一次读命令。
- **脚本与 CI**——批量建设备、定时拉历史、把平台操作编进部署流水线。
- **AI 编码工具**——让 Claude Code、Codex、Gemini CLI 等经 shell 调用平台；每条命令都支持 `--format json`，输出可供程序可靠解析。

<AutomationIndexDiagram lang="zh" />

CLI 的鉴权用登录令牌：先取盐、再换 12 小时有效的 access token，每个请求带 `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token`
三个鉴权头。和 AI 栏目一样，CLI 拿不到比登录账号更多的权限，跨租户数据看不到（返回 404 而非数据）。

> 想让大模型而非脚本来驱动操作？见 [AI 栏目](../ai/)（Agentic 中心对话式运营 + MCP 接外部 Agent）。

## 延伸阅读

- [CLI 使用指南](./cli) — 完整命令面、三段式登录、凭据存储后端（keychain / 加密文件 / 环境变量）
- [AI](../ai/) — 对话式 Agentic 与 MCP 接外部 Agent
- [第一个设备：端到端](../quickstart/first-device) — 用 CLI 跑通第一个设备的示例
