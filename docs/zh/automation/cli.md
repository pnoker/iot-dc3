---
title: CLI 使用指南
---

<script setup>
import CliSequenceDiagram from '../../.vitepress/theme/components/CliSequenceDiagram.vue'
import CliCredentialFlowDiagram from '../../.vitepress/theme/components/CliCredentialFlowDiagram.vue'
import CliClassDiagram from '../../.vitepress/theme/components/CliClassDiagram.vue'
</script>


# CLI 使用指南

`dc3-cli` 是 IoT DC3 的命令行客户端：一个独立的 TypeScript 包（Node ≥ 20），把平台的全部能力封装成 `dc3` 命令，全程经网关
`/api/v3/*` 通信。读完这页你能装好它、配好网关、登录拿到 token，并用真实命令读设备、读位号值、下发命令。

> 你在这里：已经能用前端或 curl 跑通 [第一个设备](../quickstart/first-device)，现在想用命令行或在 AI Agent 里驱动平台。需要让
> AI 工具直连平台时，转向 [AI Agent / MCP 集成](../ai/mcp)。

## 它是什么、给谁用

`dc3-cli` 不是另一套后端，它只是一个 HTTP 客户端：所有请求都打到你配置的网关地址，路径前缀统一为 `/api/v3/*`
（网关再聚合到鉴权、管理、数据、智能各中心）。它没有任何 Java 或构建上的耦合，装一个 Node 包即可独立运行。

它面向三类人：在终端里快速查设备、读值、下发命令的**运维/接入工程师**；把平台操作写进脚本与流水线的**自动化作者**；以及让 AI
编码工具（Claude Code、Codex、Gemini CLI 等）通过 shell 直接调用平台的 **Agent 集成方**——每个命令都支持 `--format json`
，输出可被程序稳定解析。

```bash
npm install -g dc3-cli
```

三步即可开始：配网关、登录、然后用。

```bash
dc3 config set gateway http://localhost:8000   # 网关地址（示例：本地默认端口 8000）
dc3 auth login                                 # 交互式登录
dc3 device list                                # 列出设备
```

## 鉴权：三段式 token 如何拿到与保鲜

`dc3 auth login` 背后是一条三段式的 token 链路，与平台 [黄金路径](../quickstart/first-device) 里 curl 登录用的是同一对端点，只是
CLI 帮你串好了。先 `POST /api/v3/auth/token/salt` 用租户名 + 用户名换一个**盐（salt）**；再把**明文密码**连同盐
`POST /api/v3/auth/token/generate`，换回一个 JWT。拿到 JWT 后，CLI 解出其中的 `iat` / `exp`，把
`{ token, salt, tenant, username, issuedAt, expiresAt }` 写入 `~/.dc3/tokens.json`（文件权限 `0600`，每个 profile 一条）。

<CliSequenceDiagram lang="zh" />

后续每次调用 API 前，CLI 会做两件事保证你几乎不会撞到 401：

- **主动续期**：若当前 token 在续期阈值内即将过期，调用前先静默重登换新 token。阈值由 profile 的 `renewal_threshold_hours`
  控制，默认 **1 小时**（即剩余有效期不足 1h 就提前续）。
- **401 兜底**：万一仍收到 401（时钟漂移、服务重启等），续期后**自动重试一次**该请求。

发往受保护端点的请求头沿用平台约定的三件套——`X-Auth-Tenant`、`X-Auth-Login`、`X-Auth-Token`，其中 `X-Auth-Token` 携带
`{ salt, token }`。

::: warning 续期依赖密码可取
主动续期和 401 重试都需要 CLI 能重新拿到密码去走一遍 salt→generate。如果你用 `--no-save` 或 `--store prompt`（不落盘），token
过期后没有可用密码，CLI 无法静默续期，需要你手动 `dc3 auth login` 重新登录。
:::

::: danger 不要打印真实密码或 token
本页所有密码、token 均为示例占位。请勿在脚本、日志、issue 中明文粘贴真实密码或 JWT；`dc3 auth token`
仅用于本机排障，输出的令牌等同于一次有效登录凭证。
:::

## 凭据存哪儿：四级解析链

密码本身不进 `tokens.json`，而是交给**凭据存储后端**保管。CLI 在续期时按固定优先级解析密码：先问 OS
钥匙串，再问加密文件，再读环境变量，最后回落到交互式 prompt 兜底。每一级"可用且命中"就采用，否则继续往下。用
`dc3 config set auth.store <type>` 选择当前 profile 使用哪种后端。

<CliCredentialFlowDiagram lang="zh" />

四种后端各自的定位：

| 存储          | 落点                                                                         | 适用场景         |
|-------------|----------------------------------------------------------------------------|--------------|
| `keychain`  | OS 钥匙串（macOS Keychain / Linux Secret Service / Windows Credential Manager） | 日常使用（默认）     |
| `encrypted` | `~/.dc3/credentials.enc`，AES-256-GCM 加密                                    | 钥匙串不可用时的回落   |
| `env`       | 读取 `DC3_PASSWORD` 环境变量                                                     | CI/CD、脚本     |
| `prompt`    | 不落盘，每次用到都交互输入                                                              | 安全性最高，无法自动续期 |

加密文件后端用 `aes-256-gcm`，密钥由机器标识经 `scrypt` 派生，密码以 `identifier → password`（`username@tenant`）形式存储，不写明文。

```bash
# 登录时一并选择凭据后端
dc3 auth login --store keychain        # 存入 OS 钥匙串（适合日常）
dc3 auth login --store env             # 从 DC3_PASSWORD 读取（适合 CI）
dc3 auth login --no-save               # 不保存密码，过期需手动重登

# 非交互登录（示例值，请勿用真实密码明文）
dc3 auth login --tenant default --username dc3 --password '<示例密码>'

dc3 auth status                        # 查看登录态与剩余有效期
dc3 auth token --header                # 以 JSON 打印完整鉴权头（排障用）
```

## 命令模块概览

CLI 共 14 个命令模块，按对象与场景划分。配置与鉴权是入口，元数据类（device/driver/point/profile/group/label）对应管理中心的增删改查，事件/命令/告警/仪表盘对应数据与运行态，
`chat` 则把请求转发到智能中心。

| 模块  | 命令前缀            | 用途                      |
|-----|-----------------|-------------------------|
| 配置  | `dc3 config`    | 网关地址、租户、凭据后端、profile 切换 |
| 鉴权  | `dc3 auth`      | 登录/登出、查看登录态与 token      |
| 设备  | `dc3 device`    | 设备增删改查、计数、在线状态          |
| 驱动  | `dc3 driver`    | 驱动列表、详情、运行状态            |
| 位号  | `dc3 point`     | 位号增删改查、读最新值、历史、写值       |
| 模板  | `dc3 profile`   | 模板增删改查                  |
| 分组  | `dc3 group`     | 设备分组管理                  |
| 标签  | `dc3 label`     | 标签管理                    |
| 事件  | `dc3 event`     | 事件定义增删查、事件历史            |
| 命令  | `dc3 command`   | 命令列表、调用、命令历史            |
| 告警  | `dc3 alert`     | 告警概览、列表、确认、趋势、Top 来源    |
| 仪表盘 | `dc3 dashboard` | 统计、时序、拓扑、健康、实时流         |
| 主题  | `dc3 topic`     | 主题列表                    |
| 智能  | `dc3 chat`      | 与智能中心对话（可选流式、指定模型）      |

结构上，`dc3` 入口把命令行解析到 14 个命令模块，所有模块再共用同一组核心组件：HTTP 客户端、配置管理、token
管理与凭据存储。命令模块只描述"做什么"，真正的网关请求、profile 解析、续期与密码读取都收敛在核心层。

<CliClassDiagram lang="zh" />

全局选项对所有模块通用：`--profile <name>` 切换配置档；`--format json|table|yaml` 选输出格式（TTY 默认 table，管道默认
json）；`--verbose` 打印请求/响应细节；`--ci` 进入 CI 模式（无颜色、json 输出、严格退出码）。

::: details 多 profile 并存（开发 / 生产切换）
每个 profile 各自保存网关、租户、凭据后端与 token，互不干扰：

```bash
dc3 config profile use prod
dc3 config set gateway https://iot.example.com   # 示例生产地址
dc3 auth login

dc3 config profile use default                    # 切回本地
dc3 device list
```

:::

## 实操：读值、读历史、下发命令

下面用真实命令演示常见操作，所有 ID、值均为示例占位。读最新值对应数据中心 `POST /api/v3/data/point_value/latest`，写位号对应
`POST /api/v3/data/point_command/write`，命令回执对应 `GET /api/v3/data/point_command_history/get_by_command_id`。

::: code-group

```bash [dc3 CLI]
# 读位号最新值
dc3 point read 456789 --format json

# 读位号历史
dc3 point history 456789 --device-id 123456 --count 100 --format json

# 给可写位号下发写命令（位号须为 WRITE_ONLY 或 READ_WRITE）
dc3 point write 456789 --device-id 123456 --value 25.5

# 调用设备命令
dc3 command call --device-id 123456 --command-id 789 --params '{"speed":1500}'

# 查命令执行回执（用 call 返回的 recordId，示例值）
dc3 command history 9a1f2c3d-0000-0000-0000-000000000000

# 设备与系统健康
dc3 device status 123456 --format json
dc3 dashboard health --format json
```

```bash [对应 curl]
# 等价的写命令直连网关（示例值）
curl -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H 'Content-Type: application/json' \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: {"salt":"<示例盐>","token":"<示例JWT>"}' \
  -d '{"deviceId":123456,"pointId":456789,"value":"25.5"}'
```

:::

`dc3 point write` 与 `dc3 command call` 的最终落点是命令链路。写命令是异步下发：网关/数据中心受理后立即返回一个命令
ID，真正的执行结果要用该 ID 去查命令历史。

::: danger 写失败不回显、命令有 TTL
位号能否写取决于它的 `rwFlag`，对 `READ_ONLY` 位号写会被拒绝。写命令若执行失败，回执的 `responseValue` 为 `null`
，不会把失败值回显成成功；命令本身有有效期，`PointCommandDTO.expireAt` 默认 `now + 10s`，超时未被驱动消费即作废。这些语义在
CLI 与直连 curl 下完全一致。
:::

## 退出码：脚本里如何判定结果

`dc3` 用退出码区分成功与失败：成功退出 `0`，任何错误（参数非法、网关不可达、鉴权被拒、API 报错等）都统一退出 `1`。CLI 顶层捕获所有异常后
`process.exit(1)`，并不按错误类别细分退出码——要分辨具体原因，读 stderr 的错误信息或加 `--verbose`。

| 退出码 | 含义                            |
|-----|-------------------------------|
| `0` | 成功                            |
| `1` | 任何错误（参数非法、网络不可达、鉴权被拒、API 报错等） |

```bash
# CI 中按退出码判定：非 0 即失败，从 stderr 区分原因
if ! dc3 device list --ci 2>err.log; then
  if grep -qE 'Authentication failed|Forbidden' err.log; then
    echo "需要登录"
  else
    echo "其他错误"; cat err.log
  fi
  exit 1
fi
```

::: tip 给 AI Agent 用时优先 `--format json`
让 AI 编码工具通过 shell 调用平台时，统一加 `--format json`（或 `--ci`），输出字段稳定、可解析；退出码 `0`/`1` 让 Agent
先判断成功与否，再读 stderr 的错误信息决定是否重新登录或重试。若希望 AI 工具直接发现并调用平台全部
API，参见 [AI Agent / MCP 集成](../ai/mcp) 的网关 MCP 端点接法。
:::

## 延伸阅读

- [自动化](./) — CLI、脚本与 MCP 在整体自动化中的位置
- [AI Agent / MCP 集成](../ai/mcp) — 让 AI 工具经网关 `/mcp` 自动发现并调用平台工具
- [第一个设备](../quickstart/first-device) — 黄金路径：从建驱动到读值的端到端流程
