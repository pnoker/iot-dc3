# IoT DC3 文档站 — 信息架构精炼 + 物联网知识体系 设计方案

- 日期：2026-06-25
- 状态：待评审
- 关系：本方案**承接并修订** 2026-06-22 docs-overhaul 系列。除本文新增/覆盖的部分外，全部沿用：
  - `2026-06-22-docs-overhaul-authoring-contract.md` —— 写作契约（页面骨架、Mermaid 规范、诚实标注、术语表、Don'ts）。**所有新增/改写页面（含子 agent 产出）必须遵守。**
  - `2026-06-22-docs-overhaul-dossier.md` —— 架构事实底座（§B 链路、§E 图表、§F 风格）。
  - `2026-06-22-docs-overhaul-factspack.md` —— 驱动/API/env/告警/Agentic 已核验事实。

## 1. 背景与问题

2026-06-22 的全站重构已把内容质量、Mermaid 图、事实准确性做到位，并落地了中英双语（`zh/` 与 `en/` 各 76 页、逐页平行）。但**顶层信息架构仍是 10 个并列栏目**，存在两个新问题：

1. **顶级分类不专业**：介绍 / 快速开始 / 架构 / 驱动 / AI / 自动化 / 操作指南 / 开发 / 部署运维 / 社区 —— 10 个扁平栏目，粒度不齐（"自动化"实为 1 篇 CLI、"模块"仅 2 页），缺少一线文档站的 3–6 个清晰大类的层级感。
2. **缺少物联网知识体系**：站点是纯产品文档，没有把 DC3 放进物联网专业语境，读起来不像"一本书"。

本方案解决这两点，且**不推翻** 2026-06-22 的页面与质量成果。

## 2. 目标与非目标

**目标**

1. **IA 精炼**：将 10 个扁平栏目收敛为 **6 个支柱 + 社区**（顶级短标，见 §3），阅读顺序天然成书。
2. **新增物联网知识体系支柱**：按物联网四层参考架构（感知 / 网络 / 平台 / 应用 + 贯穿安全）组织 **9 章原创内容**，每章接回 DC3 实现（见 §4）。
3. **加深现有文档**：在既有基础上施加统一"书感"处理与交叉链接，重点补薄页（见 §5）。
4. **中英同步**：`zh/` 与 `en/` 逐页平行，任何改动两侧一起落（见 §6）。
5. **低 URL 扰动**：IA 改造尽量只动 nav/sidebar，不大规模搬文件、不制造死链（见 §3.3）。

**非目标**

- 不改后端 / 前端代码；仅文档与 `config.mts`。
- 不重写 2026-06-22 已达标的页面正文（除 §5 的定向加深与交叉链）。
- 不引入第三方文档主题或自定义组件；沿用 VitePress 默认主题 + 现有 `theme/`。
- 知识体系**不出现书籍引用、不逆向照搬**；书仅作主题脚手架与术语/深度校准（见 §4.3）。

## 3. 顶层信息架构：6 支柱

### 3.1 顶级导航（短标，中/英）

| # | 顶级标题 | English | 落地页 | 覆盖内容 |
|---|---|---|---|---|
| ① | 总览 | Overview | `introduction/` | 定位 · 核心概念 · 角色路径 · 快速开始 · 术语表★ |
| ② | 基础 | Foundations | `foundations/`★ | 物联网四层知识体系（9 章），每章接 DC3 |
| ③ | 架构 | Architecture | `architecture/` | 领域模型 · 服务拓扑 · 数据/命令平面 · 鉴权租户 · 模块 |
| ④ | 接入 | Connectivity | `drivers/` | 接入流程 · 28 协议驱动 · 驱动能力矩阵★ |
| ⑤ | 运维 | Operations | `operation/` | 采集与命令 · 告警 · 部署 · 可观测性 · 日志 · 排障 |
| ⑥ | 开发 | Develop | `development/` | 驱动开发 · API · 测试 · CLI · AI/MCP · 变更日志 |
| — | 社区 | Community | 下拉 | 贡献 · 行为准则 · 安全 |

`★` = 本次新增页面。

### 3.2 各支柱侧栏分组（页面级，确保旧页一页不丢）

> 标注：`旧路径` = 现有页面归位（URL 不变）；`★新` = 新建页面；`←移入` = 逻辑归属变化但 URL 保持。

```
① 总览 introduction/
  总览        平台定位 introduction/index · 核心概念总图 concepts · 角色路径 paths
  对象与数据   物模型 · 设备 · 驱动 · 位号 · 位号值        (concepts/*)
  能力与边界   指令 · 事件 · 属性与配置 · 租户             (concepts/*)
  快速开始     本地开发 quickstart/index · 环境变量 environment · 第一个设备 first-device  ←移入(URL 不变)
  附录        术语表 introduction/glossary ★新

② 基础 foundations/  ★全新支柱（见 §4 完整目录）

③ 架构 architecture/
  总览        架构总览 index
  服务与协作   服务与拓扑 services · Facade 模式 facade-modes
  链路与模型   数据平面 data-plane · 命令平面 command-plane · 鉴权租户RBAC auth-rbac · 领域模型 domain-model
  模块        模块地图 architecture/modules · 模块清单 modules/index   ←modules 栏目并入(URL 不变)

④ 接入 drivers/
  接入指南     驱动总览 drivers/index · 设备接入流程 operation/device-onboarding ←移入(URL 不变)
  工业总线/PLC  modbus-tcp · modbus-rtu · opc-ua · opc-da · plcs7 · melsec · fins · ethernet-ip
  SCADA/电力/计量 bacnet-ip · iec104 · dlms · sl651 · snmp
  IoT/无线     mqtt · coap · lwm2m · http · ble · zigbee · can
  串口/通用网络  serial · tcp-udp
  数据库        mysql · postgresql · oracle · sqlserver
  虚拟/测试     virtual · listening-virtual
  附录        驱动能力矩阵 drivers/matrix ★新

⑤ 运维 operation/
  运营        运营概览 operation/index · 数据与命令 data-commands · 告警与通知 alarms
  部署与运维   部署模式与镜像源 guide/usage · 可观测性 guide/observability · 日志规范 guide/logging · 故障排查 guide/troubleshooting  ←guide 栏目并入(URL 不变)

⑥ 开发 development/
  开发        开发概览 index · 驱动开发 driver-authoring · API 文档 api-documentation · 测试 testing · 变更日志 changelog
  AI 集成     AI 概览 ai/index · Agentic 中心 ai/agentic · AI Agent/MCP ai/mcp   ←ai 栏目并入(URL 不变)
  自动化      CLI 使用指南 automation/cli   ←automation 栏目并入(URL 不变)

社区 community/ （下拉）  贡献 contributing · 行为准则 code-of-conduct · 安全 security
```

栏目收敛映射：`modules→③`、`guide→⑤`、`ai+automation→⑥`、`operation/device-onboarding→④`（其余 operation 留⑤）。原 10 栏目中单薄的"自动化""模块"不再是顶级，吸收为支柱内分组。

### 3.3 实现策略（低 URL 扰动）

默认采用 **配置聚合（Strategy I）**：保持现有文件位置与 URL 不变，仅改 `config.mts`：

1. 把 `SECTIONS` 模型改造为 **pillar 模型**：每个 pillar 声明 `navKey`、`landing` 路径、覆盖的 **path 前缀集合**、与 `groups`；`groups.items` 用**跨栏目绝对路径**引用既有页面（现有 `linkOf` 已支持 `code` 含 `/` 且首段为栏目 key 的绝对路径）。
2. `buildSidebar` 为一个 pillar 覆盖的**每个 path 前缀**注册**同一组 groups**（如 ⑤ 同时注册 `/zh/operation/` 与 `/zh/guide/`），保证跨旧目录页面共享同一侧栏。
3. `buildNav` 收敛为 6 项 + 社区下拉；对跨目录 pillar 设 `activeMatch`（如 ⑥：`/(development|ai|automation)/`、⑤：`/(operation|guide)/`），保证高亮正确。
4. `device-onboarding`：URL 保持在 `operation/` 下，但在 ④ 接入侧栏以跨栏目路径列为"接入指南"入口；运营侧栏不再列它。其 nav 高亮归属在 P0 plan 内定细节（必要时给该页单独 `activeMatch` 例外）。
5. 首页 `index.md`（zh+en）的 hero `features` 由 9 张卡改为 **6 张支柱卡**，文案对齐 §3.1。

> 备选（不默认采用）：物理搬迁目录使其与支柱一一对应——URL 更干净但产生 ~20+ 页重定向与外链断裂，风险高，本次不做；如未来需要再单列方案。

## 4. 知识体系支柱②（foundations/，9 章原创）

### 4.1 结构：四层参考架构 + 安全

每章 = 专业 IoT 知识（原创散文 + Mermaid 图）+ 结尾「在 DC3 中」段落接回产品。文件位于 `foundations/`（zh + en 各一份）。

```
foundations/index   2.0 物联网技术总览   四层参考架构 · 标准体系 · DC3 全景映射图（统领后续，含一张四层↔DC3 对照图）
─ 感知层 Perception ─
foundations/sensing        2.1 传感与测量      传感器原理/分类 · MEMS · 精度/采样/标定  → DC3：物模型 Profile、位号 Point 如何抽象感知量
foundations/identification 2.2 自动识别与定位   RFID · 条码/二维码 · NFC · 北斗/定位      → DC3：设备标识与标签体系
─ 网络层 Network ─
foundations/fieldbus       2.3 工业总线与协议   Modbus/OPC UA/PROFINET/BACnet/IEC/CAN 体系与选型 → DC3：28 驱动统一适配
foundations/iot-protocols  2.4 IoT 协议与无线   MQTT/CoAP/LwM2M + BLE/Zigbee/LoRa/NB-IoT/5G     → DC3：协议驱动与网关
─ 平台层 Platform ─
foundations/edge-cloud     2.5 边缘与云架构    边缘/雾计算 · 边云协同 · 数字孪生        → DC3：中心服务 · 驱动下沉
foundations/data-pipeline  2.6 时序数据与流处理 时序库 · 大数据管道 · 流计算            → DC3：数据平面 · TimescaleDB
─ 应用层 Application ─
foundations/aiot           2.7 数据智能与 AIoT  数据分析 · AI+IoT · 大模型接入 · 行业范式  → DC3：Agentic · MCP · 告警分析
─ 安全 Security（贯穿）─
foundations/security       2.8 物联网安全      设备/通信/平台/数据安全 · 威胁模型       → DC3：鉴权 · 租户 · RBAC · TLS
```

### 4.2 每章页面骨架（在 authoring-contract §1 基础上的专属约定）

```
# <章标题>

<导语：这一层解决物联网里的什么问题；读完你能把它对应到 DC3 的哪一块。>

## 这一层是什么 / 为什么存在        ← 散文建立心智模型（四层架构里的位置）
## 关键技术与权衡                  ← 主流方案、原理、选型权衡；配 1 张 Mermaid（分类/对比/流程）
## 工程要点                       ← 实践中的坑、指标、边界
## 在 IoT DC3 中如何落地  ★必有     ← 接回产品：对应的概念/服务/驱动/表，附站内相对链接；DC3 事实据 factspack/dossier + 源码核验
## 延伸阅读                       ← 链接到相邻层章节 + 对应产品页（架构/接入/运维）
```

### 4.3 书的用法（脚手架 + 选读提炼，全原创）

- 用 `iot-books/` 的主题分类与各书目录**确定章节覆盖面与组织顺序**（书库主题↔四层已对齐：sensing→感知、communication→网络、platform-cloud→平台、industry→应用、security→安全）。
- 对关键章节**选读提炼要点**，仅用于**校准术语与深度**（确保专业、不外行）。
- **产出全部原创改写**：不出现书名/作者/引用、不逐段照搬、不复制图表。规避版权。
- 凡"在 DC3 中"段落涉及的事实（服务名、表名、路由键、默认值、驱动数、工具数等），一律据 factspack/dossier + 源码 `path:line` 核验，遵守 authoring-contract §4/§5（诚实标注已实现/受开关/未实现）。

## 5. 加深现有文档（书感规范，定向）

不盲目重写已达标页面。统一施加、可验证：

1. **交叉链接到知识层**：现有产品页（架构/接入/运维相关）在导语或"延伸阅读"补一条指向 foundations 对应章的链接，反向亦然，形成"理论↔实现"闭环。
2. **补薄页**：`community/code-of-conduct`（7 行）补正文；驱动页（多为 70–90 行模板）按"协议背景→属性配置→排错→DC3 映射"四段补齐并接 §2.3/§2.4。
3. **术语一致**：统一接 authoring-contract §6 术语表与新增 `introduction/glossary`。
4. 不改动事实正确、结构完好的页面正文，避免无谓扰动（遵守全局规则：改动可追溯到需求）。

## 6. 中英同步

- `en/` 与 `zh/` 已逐页平行，本次维持该不变量：**每个新增/改写页面、每处 nav/sidebar 文案、首页卡片，zh 与 en 同时落地**。
- 知识层 9 章 = 9（zh）+ 9（en）= 18 页；术语表、驱动矩阵各 +2。
- `config.mts` 的 `buildNav/buildSidebar(lang)` 已双语参数化，结构改造对两语言一次生效；仅各分组/标题的 en 文案需补全。

## 7. 分阶段交付（每阶段独立可验证、可上线）

| 阶段 | 范围 | 关键产出 | 完成标准（验证） |
|---|---|---|---|
| **P0 · IA 精炼** | `config.mts` pillar 改造（6 nav + 聚合 sidebar + activeMatch）；首页 6 卡改版；术语表/驱动矩阵占位或首版；全内链校验 | 专业 6 支柱导航上线，旧页全部可达、零死链 | `pnpm docs:build` 通过；`pnpm docs:preview` 中/英逐支柱点检无 404；nav 高亮正确 |
| **P1 · 知识支柱** | foundations 2.0–2.8 共 9 章 ×（zh+en）= 18 页，每章「在 DC3 中」+ Mermaid + 交叉链 | 物联网四层知识体系成书，融入 DC3 | build 通过；每章 ≥1 图双色渲染正常；「在 DC3 中」链接有效；DC3 事实经源码核验 |
| **P2 · 加深现有** | §5 定向加深：补薄页、双向交叉链、术语统一 | 产品页与知识层闭环、薄页补齐 | build 通过；抽查页面符合 authoring-contract；无纯表格开篇/占位符/未标注未实现 |

推进顺序 P0 → P1 → P2，先交付 P0（最低风险、立刻可见）。每阶段各自 review。

## 8. 风险与权衡

- **配置聚合复杂度**：pillar 跨目录共享 sidebar + activeMatch 有边界情形（device-onboarding 归属）。缓解：P0 内先打通 `config.mts` 并用 `pnpm docs:preview` 双语全量点检；必要时给个别页 `activeMatch` 例外。
- **知识层与代码漂移**：「在 DC3 中」段落随产品演进可能过时。缓解：只引稳定事实（服务/平面/概念），数值类据 factspack/源码核验并就近标注，不做强耦合自动生成。
- **双语工作量翻倍**：P1 共 18 页。缓解：zh 定稿后 en 平行产出；结构改造一次双语生效。
- **版权**：见 §4.3，全原创、无引用、不照搬。
- **范围蔓延**：P2"加深"易无边界。缓解：限定为 §5 四项定向动作，不做全站重写。

## 9. 落地前须据源码核验的事实（沿用 factspack「Still unresolved」+ 本次新增）

知识层「在 DC3 中」涉及以下需就近核验（authoring-contract §5）：

- 驱动数 **28**、Agentic 内置工具 **10**、`PointCommandTypeEnum` 取值、Auth 分布式默认 `facade=grpc`、登录双步 `salt/generate`、鉴权头三元组、`num_value` 可空、HMAC pre/pro fail-fast、`dc3.driver.code` 不可变 —— 均以 factspack/dossier 为准并打开源码确认。
- 四层↔DC3 映射图中每个 DC3 节点（中心服务名、数据平面交换机/队列、TimescaleDB 表）据 dossier §B 核对。
