# IoT DC3 文档站全站重构 — 设计方案

- 日期：2026-06-22
- 状态：待评审
- 事实底座：`2026-06-22-docs-overhaul-dossier.md`（每页落地前据此核对源码）

## 1. 背景与问题

当前 `iot-dc3/docs/` 是一个 VitePress（zh-CN，`vitepress ^1.6.4`）站点，内容覆盖较全但质量不达标，逐页审计（见 dossier
§C）确认了三类硬伤：

- **零真实图表**：所有"数据流/命令流"是 ` ```text ` 里的 `->` ASCII 箭头，公开页面没有一张流程图/时序图。
- **AI 化严重**：`concepts.md`、`data-commands.md` 等是纯平行表格 + 零叙事；架构页只罗列 4 个设计点不解释；模块页用 Maven
  清单冒充架构。
- **不专业 / 不友好**：两个空白页（`guide/usage.md`、`development/changelog.md` 只含 `<!--@include-->`
  ，渲染为全空白）；无角色化学习路径；维护者内部草稿 `superpowers/` 混入公开导航。

## 2. 目标与非目标

**目标**

1. 把全站重写/润色为**产品经理 + 架构师双视角**：既讲清"是什么、解决什么问题、给谁用"，也讲透"
   如何构成、关键链路如何流转、为何这样设计"。
2. 用 **Mermaid** 为每条关键链路补齐流程图/时序图/状态图/ER 图（共 42 张，见 dossier §E）。
3. 消除 AI 味：遵循 dossier §F 的 12 条写作风格指南（叙事先行、先讲为什么、术语就地定义、具体示例替代占位符、诚实标注实现状态）。
4. 落地完整扩展版信息架构（dossier §D），新增 ~12 个高价值页面。
5. 所有事实可追溯到源码（dossier 已带 `file:line`），不写通用 IoT 套话。

**非目标（本次不做）**

- **不做双语**：站点维持 zh-CN 单语；英文化是独立工作，本次仅保证中文质量。
- **不改后端代码 / 不改前端代码**：纯文档与文档站配置（含 mermaid 依赖与 `config.mts`）。
- **不重写 `superpowers/` 内部草稿内容**：仅将其移出公开站构建（见 §5.2）。
- **不引入第三方文档主题**：沿用 VitePress 默认主题 + 现有 `theme/`。

## 3. 设计原则（反 AI 化写作规范）

落地每页必须遵守 dossier §F 全部 12 条，核心七条复述：

1. 表格是参考资料、不是叙事主体；任何概念表前必须有建立心智模型、回答"为什么存在"的散文。
2. 先"为什么"后"怎么做"，不堆设计点清单。
3. 术语首次出现就地定义，并给出取值来源（如 attribute 来自驱动 `application.yml`）。
4. 每条关键链路配一张 Mermaid 图，不用 ASCII art。
5. 用可运行示例（virtual 驱动黄金路径 + 真实 curl + JSON 响应）替代 `temperature/host/port` 占位符。
6. 诚实标注硬约束与失败（HMAC 在 pre/pro fail-fast；`num_value` 可空；写失败不回显值；`dc3.driver.code` 不可变）。
7. 区分**已实现 / 受开关控制 / 未实现**（如外部身份表已建但登录端点关闭；MCP 仅 OAuth 2.1、无 PAT）。

## 4. 图表与文案约定

- **图表技术**：Mermaid，嵌入 markdown 的 ` ```mermaid ` 代码块。
- **标签语言**：图内节点/说明用**中文**，技术标识符保留原文（`Gateway`、`RabbitMQ`、`dc3.e.value`、`PointValueBO`、类名/表名/路由键）。
- **类型选择**：链路时序 → `sequenceDiagram`；状态机 → `stateDiagram-v2`；拓扑/分支/数据流 → `flowchart`；实体关系 →
  `erDiagram`；分层/类结构 → `classDiagram`。
- **主题**：暗色模式由插件强制深色；浅色模式用 `mermaid` 配置默认主题。不在单图里手写 `themeVariables` 颜色，保持全站一致、随站点主题切换。
- **可维护性**：图与正文同源同页；架构变更时图随文改。每张图就近放在它解释的段落下方，并在正文引用其要点（图示结构、文述细节，二者不重复堆砌）。

## 5. 技术基建

### 5.1 Mermaid 集成（VitePress 1.6）

已用 context7 核实当前做法（`vitepress-plugin-mermaid` + `mermaid`，`withMermaid()` 包裹配置）：

1. 在 `iot-dc3/package.json` 增加 devDependencies：`vitepress-plugin-mermaid`、`mermaid`。
2. 改 `docs/.vitepress/config.mts`：
   ```ts
   import { withMermaid } from 'vitepress-plugin-mermaid'
   export default withMermaid(defineConfig({ /* 现有配置 */ }))
   ```
   保留现有 `defineConfig` 内容不动，仅外层包裹并按需加 `mermaid: {}` 占位。
3. 用 `pnpm`（仓库统一包管理器）安装；本地 `pnpm docs:dev` 验证渲染，`pnpm docs:build` 验证构建通过。

**落地实测结果（P0 已完成）**：

- 安装版本：`mermaid@11.15.0` + `vitepress-plugin-mermaid@2.0.17`，与 `vitepress@1.6.4` 兼容，`docs:build` 通过。
- **pnpm 关键坑**：mermaid 的 CJS 传递依赖（`dayjs`、`cytoscape`、`cytoscape-cose-bilkent`、`@braintree/sanitize-url`、`debug`
  ）在 pnpm 非扁平 `node_modules` 下，Vite `optimizeDeps` 预构建解析失败，回退直接加载 `dayjs.min.js` 时报
  `does not provide an export named 'default'`，导致**整页不挂载**。修复：把这 5 个依赖显式加为 devDependencies 提升到顶层
  `node_modules`，再 `rm -rf docs/.vitepress/cache` 重建预构建缓存。已用 Playwright 实测：浅色模式品牌绿主题、SVG 正常渲染。

### 5.2 superpowers/ 移出公开导航

- 从 `config.mts` 的 `nav` 删除 "Superpowers" 项；从 `sidebar` 删除 `'/superpowers/'` 整组。
- 从首页 `index.md` features 删除 "Superpowers" 卡片。
- 用 VitePress `srcExclude` 将 `superpowers/**` 排除出站点构建（文件保留在仓库，仅不进站点）。
- 检查并修复其它页面指向 `superpowers/` 的公开链接。

## 6. 目标信息架构

落地 dossier §D 的完整扩展版。最终页面树（`新` = 新建，`重写` = 推倒重写，`润色` = 保留骨架重写文案+补图）：

```
首页 index.md ……………………………………………… 润色（补 C4 总览图 + "为什么选它"叙事 + 受众/部署路径）

介绍 introduction/（新栏目）
├─ 平台定位 what-is-dc3 …………………………………… 新（闭环故事，源自 dossier §A）
├─ 核心概念与心智模型 concepts ………………………… 重写（concepts.md 迁入，散文 + ER 图）
└─ 按角色选择路径 paths ………………………………… 新（接入设备 / 部署生产 / 用 AI / 写驱动 四入口）

快速开始 quickstart/
├─ 从源码本地开发 index ………………………………… 润色
├─ 环境变量详解 environment ……………………………… 润色（补 .env vs dev.env 决策图）
└─ 第一个设备端到端 first-device ……………………… 新（virtual 驱动黄金路径：驱动→模板→设备→位号→看到数据）

架构 architecture/
├─ 系统总览 index …………………………………………… 润色（C4 + 关键设计散文）
├─ 服务与拓扑 services …………………………………… 新（端口、启动顺序、健康检查）
├─ Facade 模式 facade-modes ……………………………… 新（grpc vs local 部署拓扑选择）
├─ 数据平面 data-plane …………………………………… 新（采集时序 + RabbitMQ 拓扑）
├─ 命令平面 command-plane ………………………………… 新（命令生命周期状态机 + 拓扑）
├─ 鉴权·租户·RBAC auth-rbac …………………………… 新（请求头/HMAC/权限链路）
├─ 领域模型 domain-model ………………………………… 新（ER + Param/Attribute/Config 分层）
└─ 模块地图 module-map …………………………………… 润色（modules.md，补成熟度 + 依赖图）

操作手册 operation/
├─ 概览与入口 index ……………………………………… 润色（修复 Web UI 失效链接）
├─ 设备接入 device-onboarding …………………………… 润色（补决策树 + 数据流图）
├─ 数据与命令 data-commands ……………………………… 润色（打通二者 + curl 示例）
├─ 告警与通知 alarms …………………………………… 新（规则/通知/渠道 ER）
└─ Agentic 中心 agentic ………………………………… 润色（祛术语黑话 + 配置优先级图）

开发 development/
├─ 概览与规范 index ……………………………………… 润色（链 AGENTS.md + 首条路径 + CRUD 动词约定）
├─ 驱动开发 driver-authoring …………………………… 润色（类图 + 生命周期时序图）
├─ API 文档 api-documentation ………………………… 润色（默认凭据 + 鉴权流）
├─ 测试 testing …………………………………………… 润色（测试金字塔图 + 本地 E2E）
└─ 变更日志 changelog …………………………………… 重写（补回正文 + 生成机制说明）

自动化 automation/（新栏目，源自 CLI 调研）
├─ CLI 使用指南 cli ……………………………………… 新
└─ AI Agent / MCP 集成 mcp ……………………………… 新

部署运维 guide/
├─ 概览 index ……………………………………………… 润色
├─ 部署模式与镜像源 usage ……………………………… 重写（嵌入真实内容，修复空白页）
├─ 可观测性（可选栈）observability …………………… 新（EMQX/ELK/Prometheus/Grafana）
├─ 日志规范 logging ……………………………………… 润色（补 rationale + PII 脱敏）
└─ 故障排查 troubleshooting …………………………… 润色（排障流程图 + 跨 OS）

社区 community/
└─ 贡献 / 行为准则 / 安全 ……………………………… 保留+润色（修失效引用、版本方案）

（superpowers/ → 维护者内部，移出公开站构建）
```

新增页面（13）：what-is-dc3、paths、first-device、services、facade-modes、data-plane、command-plane、auth-rbac、domain-model、alarms、cli、mcp、observability。
合并/去重：`concepts.md` 并入 introduction（重写）；架构叙事归 `architecture/module-map`，`/modules/index`
收敛为纯模块清单参考（仅表格 + 一句指引到 module-map），二者按"叙事 vs 清单"分工去重，保留 `/modules/` 路径以防外链失效。

> 落地时同步更新 `config.mts` 的 `nav` 与 `sidebar` 以匹配新结构，并校验所有内链有效。

## 7. 图表清单

完整 42 张见 dossier §E（D1–D42，已映射到上述页面）。落地时每页按其归属图逐张实现；图必须反映源码事实，落地前据 dossier 对应
§B 小节核对。

## 8. 交付计划（按章节全量推进，逐节设检查点）

分 8 批，每批一节，按"基建优先 + 价值优先"排序。每批完成即交付 review。

| 批次           | 范围                                           | 关键产出                               | 图（dossier ID）               |
|--------------|----------------------------------------------|------------------------------------|-----------------------------|
| P0 基建        | mermaid 集成 + superpowers 移除 + nav/sidebar 骨架 | 站点可渲染 mermaid、空白页修复、导航重构           | —                           |
| P1 介绍 + 首页   | introduction 全栏目 + 首页 index.md 润色            | 定位/核心概念重写/角色路径 + 首页门面              | D1, D11                     |
| P2 架构        | architecture 全栏目（8 页）                        | 服务拓扑/facade/数据面/命令面/鉴权租户/领域模型/模块地图 | D2–D13, D15–D21, D25, D39   |
| P3 快速开始      | quickstart 全栏目                               | 本地开发/环境变量/第一个设备 E2E                | D3, D34, D35                |
| P4 操作手册      | operation 全栏目                                | 设备接入/数据与命令/告警/Agentic              | D28–D31, D37, D38           |
| P5 开发        | development 全栏目                              | 驱动开发/API/测试/Changelog/规范           | D14, D22–D24, D26, D27, D32 |
| P6 自动化       | automation 新栏目                               | CLI / MCP 集成                       | D40, D41, D42, D29          |
| P7 部署运维 + 社区 | guide 全栏目 + 社区                               | 部署/可观测性/日志/排障 + 社区修订               | D33, D36                    |

**每批的验证方式（完成标准）**

1. `pnpm docs:build` 通过（无死链、无 mermaid 语法错误）。
2. 本批所有 Mermaid 图在 `pnpm docs:dev` 浅/深两色模式下均正确渲染。
3. 本批内链、跨页链接、`config.mts` nav/sidebar 全部有效（无 404）。
4. 本批每页事实已据 dossier §B 对应小节 + 源码抽查核对（重点核 §9 待核实项）。
5. 自查无 §F 违规（无纯表格开篇、无占位符示例、无未标注的"未实现"表述）。

## 9. 待核实事实清单（落地前逐项核对源码）

来自 dossier Gaps，写到相关页前必须确认，不得照搬：

1. `dc3/dependencies/postgres/initdb/` 实际脚本数量与顺序（6 还是 7）。
2. `PointCommandTypeEnum`：READ=0/WRITE=2，值 1 是否存在第三类。
3. Agentic 工具域实际数量与清单（`dc3-center-agentic/.../tools/`）。
4. Auth Center 的 facade 模式（distributed 默认下是否仍 local）。
5. 平台 CRUD 动词约定（get/list/add/update/delete）并入 API 文档页。
6. 抽查 CRITICAL 项：空白 include 页、HMAC fail-fast、hypertable schema、`expireAt` 默认 10s、`num_value` 可空。

## 10. 风险与权衡

- **图与代码漂移**：42 张图增加维护面；通过"图就近放、随文改 + 事实带源码引用"降低漂移，不做与代码强耦合的自动生成。
- **mermaid 版本兼容**：见 §5.1 风险点，安装时锁版本验证。
- **工作量大**：分 8 批交付，每批独立可评审、可回退，避免一次性大改方向跑偏。
- **`/modules/` 与架构模块地图去重**：可能影响既有外链；落地时保留重定向或在 `/modules/` 留指引。

```
