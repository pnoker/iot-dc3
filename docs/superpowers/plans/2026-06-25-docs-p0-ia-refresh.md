# P0 · 文档站信息架构精炼 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `iot-dc3/docs` 的扁平 10 栏目导航收敛为 **5 个专业支柱 + 社区下拉**（知识支柱「基础」留待 P1），现有 76×2 页 URL
全部不变、零死链，并新增术语表与驱动能力矩阵两个参考附录。

**Architecture:** 只改 `config.mts`（把按栏目的 `SECTIONS` 模型重构为按支柱的 `PILLARS` 模型，一个支柱用 `paths`
拥有多个旧目录前缀、共享同一侧栏）+ 首页 `index.md` 特性卡 + 2 个新参考页。利用 VitePress「侧栏 key 按路径段数降序、最长前缀优先」的特性，让
`operation/device-onboarding` 在 URL 不变的前提下归入「接入」支柱侧栏，无需移动文件或改 40+ 处内链。

**Tech Stack:** VitePress 1.6（默认主题 + `vitepress-plugin-mermaid`）；pnpm；TypeScript 配置 `config.mts`；中英双语 locale（
`/zh`、`/en`）。

## Global Constraints

- **中英同步**：每个新增/改写的页面、每处 nav/sidebar 文案、首页卡片，`zh` 与 `en` 必须同时落地、结构一致。
- **零文件移动 / 零 URL 变更**（P0）：不移动任何 `.md`；现有 76×2 页 URL 保持不变。
- **零死链**：`pnpm docs:build` 对死链会失败；新页只可链接到已存在的目标。
- **写作契约**：任何含散文的页面遵守 `superpowers/specs/2026-06-22-docs-overhaul-authoring-contract.md`（导语先行、表格前有散文、术语统一
  §6、诚实标注、Don'ts）。
- **事实核验**：涉及 DC3 事实（驱动数 28、`dc3.driver.code`、端口、读写能力等）一律据
  `superpowers/specs/2026-06-22-docs-overhaul-factspack.md` + 源码 `path:line` 核对，不臆造。
- **包管理器**：仅 pnpm。验证命令统一 `pnpm --dir iot-dc3/docs docs:build` / `docs:preview`（在仓库根 `github/` 下执行，免
  cd）。
- **不引入第三方主题或自定义组件**；沿用现有 `theme/`。
- **提交受用户确认**：每个 commit 步骤在执行时先征得用户同意（当前分支 `develop`，如需提交先新建分支）。
- **「基础」支柱（②）留 P1**：本计划交付 5 支柱（总览/架构/接入/运维/开发）+ 社区；P1 再插入「基础」于第②位。

## 文件结构（P0 触及）

| 文件                                         | 职责                                             | 操作                             |
|--------------------------------------------|------------------------------------------------|--------------------------------|
| `iot-dc3/docs/.vitepress/config.mts`       | 站点 IA：`PILLARS` 模型 + `buildNav`/`buildSidebar` | 重写 §SECTIONS 相关段（L21–234），其余不动 |
| `iot-dc3/docs/zh/introduction/glossary.md` | 中文术语表（IoT + DC3）                               | 新建                             |
| `iot-dc3/docs/en/introduction/glossary.md` | 英文术语表                                          | 新建                             |
| `iot-dc3/docs/zh/drivers/matrix.md`        | 中文驱动能力矩阵                                       | 新建                             |
| `iot-dc3/docs/en/drivers/matrix.md`        | 英文驱动能力矩阵                                       | 新建                             |
| `iot-dc3/docs/zh/index.md`                 | 首页特性卡 9→5 支柱                                   | 改 `features` 段                 |
| `iot-dc3/docs/en/index.md`                 | 同上（英文）                                         | 改 `features` 段                 |

---

## Task 1: 术语表 Glossary（zh + en）

**Files:**

- Create: `iot-dc3/docs/zh/introduction/glossary.md`
- Create: `iot-dc3/docs/en/introduction/glossary.md`

**Interfaces:**

- Produces: 站内页面 `/zh/introduction/glossary`、`/en/introduction/glossary`（Task 3 的 ① 支柱侧栏「附录」组引用）。
- 仅链接到已存在页面（`introduction/concepts/*`、`architecture/*`、`drivers/*`），不得链接尚不存在的 `foundations/*`。

**内容规格（执行时按写作契约撰写散文 + 表格，非占位）：**

- H1：`# 术语表` / `# Glossary`。
- 导语 1–2 句：本页统一全站术语，给"中文 / 英文·标识 / 说明 / 所属"四列；技术标识符保留原文。
- 至少分 3 组（每组前一句引导散文，禁止纯表格开篇）：
    1. **DC3 平台术语** —— 直接采用 authoring-contract §6 全表：驱动/模板 Profile/设备 Device/位号 Point/位号值
       PointValue/网关 Gateway `dc3-gateway`/鉴权中心 `dc3-center-auth`/管理中心 `dc3-center-manager`/数据中心
       `dc3-center-data`/智能中心 `dc3-center-agentic`/租户 `tenantId`/属性 Attribute/配置
       Config。每条「说明」末尾给一个站内链接到对应概念页（如 位号 → `../introduction/concepts/point`）。
    2. **物联网通用术语** —— 感知层/网络层/平台层/应用层、传感器、执行器、RFID、NB-IoT、MQTT、CoAP、边缘计算、时序数据、数字孪生、AIoT
       等（原创解释，每条 1 句，校准专业；不引用书籍）。
    3. **协议与接口标识** —— `dc3.driver.code`、`dc3.e.value`/`dc3.e.point_command` 交换机、
       `X-Auth-Tenant/X-Auth-Login/X-Auth-Token` 头、`POST /token/salt`+`/token/generate` 等（据 factspack「Golden-path API
       contract」+ §6 核对）。
- 结尾「延伸阅读」：链接 `../introduction/concepts`（核心概念）、`../architecture/domain-model`（领域模型）。
- en 版逐条对应，术语标识保持一致原文。

- [ ] **Step 1：撰写两份 glossary.md**，按上面规格，zh/en 同时落地。
- [ ] **Step 2：构建校验（含死链检测）**

Run: `pnpm --dir iot-dc3/docs docs:build`
Expected: 构建成功，无 `dead link` 报错（此时页面尚未进侧栏，孤页不影响构建）。

- [ ] **Step 3：提交（先征得用户同意）**

```bash
git add iot-dc3/docs/zh/introduction/glossary.md iot-dc3/docs/en/introduction/glossary.md
git commit -m "docs(site): add bilingual glossary appendix"
```

---

## Task 2: 驱动能力矩阵 Driver Capability Matrix（zh + en）

**Files:**

- Create: `iot-dc3/docs/zh/drivers/matrix.md`
- Create: `iot-dc3/docs/en/drivers/matrix.md`

**Interfaces:**

- Produces: `/zh/drivers/matrix`、`/en/drivers/matrix`（Task 3 的 ④ 支柱侧栏「附录」组引用）。
- 每行链接到对应已存在驱动页 `./modbus-tcp` 等。

**内容规格：**

- H1：`# 驱动能力矩阵` / `# Driver Capability Matrix`。
- 导语 2 句：本页一览全部 **28** 个驱动的协议类别与读/写/订阅能力，便于选型；逐驱动细节见各自页面。
- 一张总表，列：`驱动 (dc3.driver.code) | 类别 | 读 | 写 | 订阅/上报 | 备注`。共 28 行，分类与 Task 3 ④
  支柱分组一致（工业总线/PLC、SCADA/电力/计量、IoT/无线、串口/通用网络、数据库、虚拟/测试）。
    - **数据来源**：`factspack` 的「Driver catalog」小节；逐行核对 `dc3.driver.code`、读写/订阅能力。拿不准的能力项据驱动
      `application.yml` / 源码核验；无法确证的用「—」并在表后 `::: info 以代码为准` 标注，不臆造。
    - 驱动名单（28）：modbus-tcp, modbus-rtu, opc-ua, opc-da, plcs7, melsec, fins, ethernet-ip, bacnet-ip, iec104, dlms,
      sl651, snmp, mqtt, coap, lwm2m, http, ble, zigbee, can, serial, tcp-udp, mysql, postgresql, oracle, sqlserver,
      virtual, listening-virtual。
- 表前必有上述导语散文（禁纯表格开篇）。
- 「延伸阅读」：`./index`（驱动总览）、`../development/driver-authoring`（自定义驱动）。

- [ ] **Step 1：撰写两份 matrix.md**（先据 factspack 填表，逐驱动核对 code/能力；zh/en 同步）。
- [ ] **Step 2：构建校验**

Run: `pnpm --dir iot-dc3/docs docs:build`
Expected: 成功、无死链。

- [ ] **Step 3：核对驱动数 = 28**

Run: `grep -c '^| ' iot-dc3/docs/zh/drivers/matrix.md`（或人工数表体行）
Expected: 表体 28 行（不含表头/分隔行）。与 factspack 一致。

- [ ] **Step 4：提交（先征得用户同意）**

```bash
git add iot-dc3/docs/zh/drivers/matrix.md iot-dc3/docs/en/drivers/matrix.md
git commit -m "docs(site): add bilingual driver capability matrix"
```

---

## Task 3: `config.mts` 重构为 5 支柱 + 社区（核心）

**Files:**

- Modify: `iot-dc3/docs/.vitepress/config.mts`（替换第 21–234 行：`SECTIONS`/`SECTION_TITLES`/`SECTION_KEYS`/
  `buildSidebar`/`buildNav`；`localeThemeConfig` 及之后全部不动）

**Interfaces:**

- Consumes: Task 1/2 产出的 `introduction/glossary`、`drivers/matrix`。
- Produces: `buildNav(lang)`、`buildSidebar(lang)`（签名不变，仍被 `localeThemeConfig` 调用）。

**关键机制（VitePress 多侧栏最长前缀匹配）：** `getSidebar` 把侧栏 key 按路径段数降序排序后取首个 `path.startsWith(key)`
。因此把 `operation/device-onboarding` 注册为 ④ 接入侧栏的 key（4 段），它优先于 ⑤ 运维的 `operation/`（3 段），该页 URL
不变即可显示「接入」侧栏。

- [ ] **Step 1：替换 config.mts 第 21–234 行为以下内容**（其余文件内容保持不变）

```ts
// ── 站点信息架构：支柱(pillar)模型（5 支柱 + 社区；「基础」支柱待 P1 插入第②位）──
// 一个支柱可拥有多个旧目录前缀(paths)并共享同一侧栏；Entry: [code, 中文, English]。
// code = 语言相对路径：单段视为目录首页(→ '/<lang>/<code>/')，多段视为具体页面(→ '/<lang>/<code>')。
type Entry = readonly [string, string, string]
type Group = {zh: string; en: string; items: ReadonlyArray<Entry>}
type Pillar = {
    navZh: string
    navEn: string
    landing: string                       // 顶栏链接落地页 code
    paths: ReadonlyArray<string>          // 该支柱拥有的侧栏路径前缀（多 path 共享同一侧栏）
    activeMatch?: string                  // 跨目录支柱的 nav 高亮正则（匹配含 locale 的路径）
    groups: ReadonlyArray<Group>          // 侧栏分组；组标题空串 → 置顶不带标题单列
}

const PILLARS: ReadonlyArray<Pillar> = [
    {navZh: '总览', navEn: 'Overview', landing: 'introduction',
     paths: ['introduction', 'quickstart'], activeMatch: '^/(zh|en)/(introduction|quickstart)/',
     groups: [
        {zh: '', en: '', items: [
            ['introduction', '总览', 'Overview'],
            ['introduction/concepts', '核心概念', 'Core Concepts'],
            ['introduction/paths', '按角色选择路径', 'Choose Your Path']
        ]},
        {zh: '对象与数据', en: 'Objects & Data', items: [
            ['introduction/concepts/profile', '物模型', 'Profile'],
            ['introduction/concepts/device', '设备', 'Device'],
            ['introduction/concepts/driver', '驱动', 'Driver'],
            ['introduction/concepts/point', '位号', 'Point'],
            ['introduction/concepts/point-value', '位号值', 'Point Value']
        ]},
        {zh: '能力与边界', en: 'Capabilities & Boundaries', items: [
            ['introduction/concepts/command', '指令', 'Command'],
            ['introduction/concepts/event', '事件', 'Event'],
            ['introduction/concepts/attribute-config', '属性与配置', 'Attribute & Config'],
            ['introduction/concepts/tenant', '租户', 'Tenant']
        ]},
        {zh: '快速开始', en: 'Quick Start', items: [
            ['quickstart', '本地开发', 'Local Development'],
            ['quickstart/environment', '环境变量', 'Environment Variables'],
            ['quickstart/first-device', '第一个设备', 'First Device']
        ]},
        {zh: '附录', en: 'Appendix', items: [
            ['introduction/glossary', '术语表', 'Glossary']
        ]}
     ]},

    {navZh: '架构', navEn: 'Architecture', landing: 'architecture',
     paths: ['architecture', 'modules'], activeMatch: '^/(zh|en)/(architecture|modules)/',
     groups: [
        {zh: '', en: '', items: [
            ['architecture', '总览', 'Overview']
        ]},
        {zh: '服务与协作', en: 'Services & Collaboration', items: [
            ['architecture/services', '服务与拓扑', 'Services & Topology'],
            ['architecture/facade-modes', 'Facade 模式', 'Facade Modes']
        ]},
        {zh: '链路与模型', en: 'Pipelines & Model', items: [
            ['architecture/data-plane', '数据平面', 'Data Plane'],
            ['architecture/command-plane', '命令平面', 'Command Plane'],
            ['architecture/auth-rbac', '鉴权 · 租户 · RBAC', 'Auth, Tenant & RBAC'],
            ['architecture/domain-model', '领域模型', 'Domain Model']
        ]},
        {zh: '模块', en: 'Modules', items: [
            ['architecture/modules', '模块地图', 'Module Map'],
            ['modules', '模块清单', 'Catalog']
        ]}
     ]},

    {navZh: '接入', navEn: 'Connectivity', landing: 'drivers',
     paths: ['drivers', 'operation/device-onboarding'],
     activeMatch: '^/(zh|en)/(drivers/|operation/device-onboarding)',
     groups: [
        {zh: '接入指南', en: 'Onboarding', items: [
            ['drivers', '驱动总览', 'Drivers'],
            ['operation/device-onboarding', '设备接入流程', 'Device Onboarding']
        ]},
        {zh: '工业总线 / PLC', en: 'Industrial Bus / PLC', items: [
            ['drivers/modbus-tcp', 'Modbus TCP', 'Modbus TCP'],
            ['drivers/modbus-rtu', 'Modbus RTU', 'Modbus RTU'],
            ['drivers/opc-ua', 'OPC UA', 'OPC UA'],
            ['drivers/opc-da', 'OPC DA', 'OPC DA'],
            ['drivers/plcs7', 'S7 (Siemens)', 'S7 (Siemens)'],
            ['drivers/melsec', 'MELSEC', 'MELSEC'],
            ['drivers/fins', 'FINS (Omron)', 'FINS (Omron)'],
            ['drivers/ethernet-ip', 'EtherNet/IP', 'EtherNet/IP']
        ]},
        {zh: 'SCADA / 电力 / 计量', en: 'SCADA / Power / Metering', items: [
            ['drivers/bacnet-ip', 'BACnet/IP', 'BACnet/IP'],
            ['drivers/iec104', 'IEC 104', 'IEC 104'],
            ['drivers/dlms', 'DLMS', 'DLMS'],
            ['drivers/sl651', 'SL651', 'SL651'],
            ['drivers/snmp', 'SNMP', 'SNMP']
        ]},
        {zh: '物联网 / 无线', en: 'IoT / Wireless', items: [
            ['drivers/mqtt', 'MQTT', 'MQTT'],
            ['drivers/coap', 'CoAP', 'CoAP'],
            ['drivers/lwm2m', 'LwM2M', 'LwM2M'],
            ['drivers/http', 'HTTP', 'HTTP'],
            ['drivers/ble', 'BLE', 'BLE'],
            ['drivers/zigbee', 'Zigbee', 'Zigbee'],
            ['drivers/can', 'CAN', 'CAN']
        ]},
        {zh: '串口 / 通用网络', en: 'Serial / Generic Network', items: [
            ['drivers/serial', '串口 Serial', 'Serial'],
            ['drivers/tcp-udp', 'TCP/UDP', 'TCP/UDP']
        ]},
        {zh: '数据库', en: 'Database', items: [
            ['drivers/mysql', 'MySQL', 'MySQL'],
            ['drivers/postgresql', 'PostgreSQL', 'PostgreSQL'],
            ['drivers/oracle', 'Oracle', 'Oracle'],
            ['drivers/sqlserver', 'SQL Server', 'SQL Server']
        ]},
        {zh: '虚拟 / 测试', en: 'Virtual / Testing', items: [
            ['drivers/virtual', '虚拟 Virtual', 'Virtual'],
            ['drivers/listening-virtual', '监听虚拟', 'Listening Virtual']
        ]},
        {zh: '附录', en: 'Appendix', items: [
            ['drivers/matrix', '驱动能力矩阵', 'Driver Capability Matrix']
        ]}
     ]},

    {navZh: '运维', navEn: 'Operations', landing: 'operation',
     paths: ['operation', 'guide'], activeMatch: '^/(zh|en)/(operation|guide)/',
     groups: [
        {zh: '运营', en: 'Operations', items: [
            ['operation', '概览', 'Overview'],
            ['operation/data-commands', '数据与命令', 'Data & Commands'],
            ['operation/alarms', '告警与通知', 'Alarms & Notifications']
        ]},
        {zh: '部署与运维', en: 'Deployment & Ops', items: [
            ['guide/usage', '部署模式与镜像源', 'Deployment & Images'],
            ['guide/observability', '可观测性', 'Observability'],
            ['guide/logging', '日志规范', 'Logging'],
            ['guide/troubleshooting', '故障排查', 'Troubleshooting']
        ]}
     ]},

    {navZh: '开发', navEn: 'Develop', landing: 'development',
     paths: ['development', 'ai', 'automation'], activeMatch: '^/(zh|en)/(development|ai|automation)/',
     groups: [
        {zh: '开发', en: 'Development', items: [
            ['development', '概览', 'Overview'],
            ['development/driver-authoring', '驱动开发', 'Driver Authoring'],
            ['development/api-documentation', 'API 文档', 'API Documentation'],
            ['development/testing', '测试', 'Testing'],
            ['development/changelog', '变更日志', 'Changelog']
        ]},
        {zh: 'AI 集成', en: 'AI Integration', items: [
            ['ai', 'AI 概览', 'AI Overview'],
            ['ai/agentic', 'Agentic 中心', 'Agentic Center'],
            ['ai/mcp', 'AI Agent / MCP', 'AI Agent / MCP']
        ]},
        {zh: '自动化', en: 'Automation', items: [
            ['automation/cli', 'CLI 使用指南', 'CLI Guide']
        ]}
     ]}
]

// 社区：仅作 nav 下拉 + 自身侧栏，不计入支柱
const COMMUNITY: ReadonlyArray<Entry> = [
    ['community/contributing', '贡献指南', 'Contributing'],
    ['community/code-of-conduct', '行为准则', 'Code of Conduct'],
    ['community/security', '安全策略', 'Security']
]

type Lang = 'zh' | 'en'
type SidebarItem = {text: string; link: string}
type SidebarGroup = {text: string; collapsed?: boolean; items: SidebarItem[]}

// 单段 code → 目录首页（带尾斜杠命中 index.md）；多段 code → 具体页面
const linkOf = (lang: Lang, code: string) => {
    const p = lang === 'en' ? '/en' : '/zh'
    return code.includes('/') ? `${p}/${code}` : `${p}/${code}/`
}

const itemsOf = (lang: Lang, entries: ReadonlyArray<Entry>): SidebarItem[] =>
    entries.map(([code, zh, en]) => ({text: lang === 'en' ? en : zh, link: linkOf(lang, code)}))

function buildSidebar(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    const sidebar: Record<string, SidebarGroup[]> = {}
    for (const pillar of PILLARS) {
        const groups: SidebarGroup[] = pillar.groups.map(g => {
            const text = lang === 'en' ? g.en : g.zh
            const items = itemsOf(lang, g.items)
            return text ? {text, collapsed: false, items} : {text: '', items}
        })
        // 多 path 共享同一侧栏；具体页面 path（多段）注册为更长 key，最长前缀优先生效
        for (const path of pillar.paths) {
            const key = path.includes('/') ? `${p}/${path}` : `${p}/${path}/`
            sidebar[key] = groups
        }
    }
    sidebar[`${p}/community/`] = [{
        text: lang === 'en' ? 'Community' : '社区',
        items: itemsOf(lang, COMMUNITY)
    }]
    return sidebar
}

function buildNav(lang: Lang) {
    const p = lang === 'en' ? '/en' : '/zh'
    const t = lang === 'en'
    const pillars = PILLARS.map(pillar => ({
        text: t ? pillar.navEn : pillar.navZh,
        link: linkOf(lang, pillar.landing),
        ...(pillar.activeMatch ? {activeMatch: pillar.activeMatch} : {})
    }))
    return [
        {text: t ? 'Home' : '首页', link: `${p}/`},
        ...pillars,
        {text: t ? 'Community' : '社区', items: itemsOf(lang, COMMUNITY)}
    ]
}
```

- [ ] **Step 2：类型/构建校验**

Run: `pnpm --dir iot-dc3/docs docs:build`
Expected: 构建成功，无 TS 错误、无死链。若报死链，定位到具体 code 拼写并修正。

- [ ] **Step 3：启动 preview，逐支柱点检双语**

Run: `pnpm --dir iot-dc3/docs docs:preview`（另起后台；默认 http://localhost:4173）
人工/浏览器核对（zh 与 en 各一遍）：

1. 顶栏只有：首页 · 总览 · 架构 · 接入 · 运维 · 开发 · 社区（下拉）。
2. 进 `/zh/guide/usage` → 顶栏「运维」高亮、左侧显示「运营 / 部署与运维」分组（说明 `guide` 复用了运维侧栏 + activeMatch 生效）。
3. 进 `/zh/operation/device-onboarding` → 左侧显示「接入」支柱侧栏（验证最长前缀匹配生效）；该页 URL 仍为
   `/zh/operation/device-onboarding`（未变）。
4. 进 `/zh/ai/agentic`、`/zh/automation/cli`、`/zh/modules/` → 分别归入「开发 / 开发 / 架构」侧栏与高亮。
5. `/zh/introduction/glossary`、`/zh/drivers/matrix` 出现在各自支柱「附录」组且可达。

- [ ] **Step 4：若 device-onboarding 侧栏未按预期归「接入」**（最长前缀匹配未生效的兜底）

将 `operation/device-onboarding` 这一 Entry 同时加入 ⑤ 运维「运营」组（保证该页在自身侧栏可高亮），并在 ④
接入保留作为入口。记录该兜底于本步骤勾选说明。

- [ ] **Step 5：提交（先征得用户同意）**

```bash
git add iot-dc3/docs/.vitepress/config.mts
git commit -m "docs(site): collapse flat nav into 5 professional pillars"
```

---

## Task 4: 首页特性卡 9 → 5 支柱（zh + en）

**Files:**

- Modify: `iot-dc3/docs/zh/index.md`（`features:` 段，frontmatter L19–64）
- Modify: `iot-dc3/docs/en/index.md`（`features:` 段，frontmatter L19–64）

**Interfaces:**

- Consumes: Task 3 的支柱落地页（`introduction/`、`architecture/`、`drivers/`、`operation/`、`development/`）。
- 卡片 link 必须指向存在的落地页，避免死链。

- [ ] **Step 1：将 `zh/index.md` 的 `features:` 整段替换为 5 张支柱卡**

```yaml
features:
  - icon: 🧭
    title: 总览
    details: 平台定位、核心概念与按角色的学习路径——先懂它是什么、给谁用、怎么入门。
    link: ./introduction/
    linkText: 了解定位
  - icon: 🏗️
    title: 架构
    details: 服务拓扑、数据平面与命令平面、鉴权租户、领域模型与模块地图——配时序图与状态机。
    link: ./architecture/
    linkText: 读懂架构
  - icon: 🔌
    title: 接入
    details: 28 个多协议驱动接入异构设备，含设备接入流程与驱动能力矩阵。
    link: ./drivers/
    linkText: 接入设备
  - icon: 🧰
    title: 运维
    details: 采集与读写命令、告警通知、部署模式与镜像源、可观测性、日志与排障。
    link: ./operation/
    linkText: 运营运维
  - icon: 🛠️
    title: 开发
    details: 基于 Driver SDK 派生新驱动，API 文档与测试，dc3 CLI 与 AI Agent / MCP 集成。
    link: ./development/
    linkText: 开始开发
```

- [ ] **Step 2：将 `en/index.md` 的 `features:` 整段替换为对应英文 5 卡**

```yaml
features:
  - icon: 🧭
    title: Overview
    details: Positioning, core concepts, and role-based learning paths — what it is, who it's for, and how to start.
    link: /en/introduction/
    linkText: Read the pitch
  - icon: 🏗️
    title: Architecture
    details: Service topology, data and command planes, auth and tenancy, the domain model and module map — with sequence and state diagrams.
    link: /en/architecture/
    linkText: Understand the design
  - icon: 🔌
    title: Connectivity
    details: 28 multi-protocol drivers for heterogeneous devices, with the onboarding flow and a driver capability matrix.
    link: /en/drivers/
    linkText: Connect devices
  - icon: 🧰
    title: Operations
    details: Data collection and read/write commands, alarms and notifications, deployment modes and registries, observability, logging and troubleshooting.
    link: /en/operation/
    linkText: Operate & maintain
  - icon: 🛠️
    title: Develop
    details: Derive new drivers from the Driver SDK, API docs and testing, the dc3 CLI and AI Agent / MCP integration.
    link: /en/development/
    linkText: Start building
---
```

（注：保持 frontmatter 结束的 `---` 与正文不变；社区入口保留在顶栏下拉与页脚，不再单列卡片。）

- [ ] **Step 3：构建校验**

Run: `pnpm --dir iot-dc3/docs docs:build`
Expected: 成功，无死链；首页渲染 5 张支柱卡。

- [ ] **Step 4：提交（先征得用户同意）**

```bash
git add iot-dc3/docs/zh/index.md iot-dc3/docs/en/index.md
git commit -m "docs(site): align homepage feature cards to 5 pillars"
```

---

## Task 5: 全量验证（P0 完成标准）

**Files:** 无（验证任务）

- [ ] **Step 1：干净构建**

Run: `pnpm --dir iot-dc3/docs docs:build`
Expected: 成功，无死链、无 mermaid 语法错误。

- [ ] **Step 2：双语逐支柱点检（preview）**

Run: `pnpm --dir iot-dc3/docs docs:preview`
逐项确认（zh + en）：

- 顶栏 = 首页 + 5 支柱 + 社区下拉，无残留旧栏目（AI / 自动化 / 操作指南 / 部署运维 等不再是顶级）。
- 5 个落地页 `/introduction/ /architecture/ /drivers/ /operation/ /development/` 均可达、侧栏分组正确。
- 跨目录归并正确：`modules→架构`、`guide→运维`、`ai+automation→开发`、`operation/device-onboarding→接入`。
- 抽查任意 5 个驱动页、3 个架构页、concepts 子页可达且无 404。
- `introduction/glossary`、`drivers/matrix` 在侧栏「附录」可达。
- 首页 5 卡链接正确。

- [ ] **Step 3：确认零文件移动 / 零 URL 变更**

Run: `git status --porcelain iot-dc3/docs/zh iot-dc3/docs/en`
Expected: 仅 `index.md`×2 修改 + `glossary.md`×2、`matrix.md`×2 新增；无任何 `R`（重命名/移动）、无既有页删除。

- [ ] **Step 4：P0 收尾**（如各 Task 未即时提交，此处统一征得用户同意后提交）

---

## Self-Review（已据 spec 核对）

- **Spec §3.1/§3.2 覆盖**：5 支柱 + 社区在 Task 3 全部落地；「基础」按计划留 P1（Global Constraints 已声明）。✅
- **§3.3 低扰动**：Task 3 用 paths 聚合 + 最长前缀匹配，零文件移动；Task 5 Step 3 校验。✅
- **§3.1 附录**：glossary（Task 1）、matrix（Task 2）。✅
- **首页 6→对齐**：Task 4 落 5 支柱卡（基础卡 P1 补）。✅
- **中英同步**：每个 Task 均 zh/en 并列。✅
- **占位符扫描**：config 为完整可粘贴代码；两份新页给出结构化内容规格 + 事实来源（执行时按写作契约撰写），无 TODO。✅
- **类型一致**：`Entry`/`Group`/`Pillar`/`linkOf`/`itemsOf`/`buildSidebar`/`buildNav` 命名贯穿一致；`buildNav`/
  `buildSidebar` 签名与 `localeThemeConfig` 调用方匹配。✅
- **风险**：device-onboarding 侧栏归属依赖 VitePress 最长前缀匹配——Task 3 Step 3 验证、Step 4 给出兜底。✅
