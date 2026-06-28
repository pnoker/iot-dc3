# TODO.md 19 项修复规划

> 分支: `feature/docs-polish` | 日期: 2026-06-28 | 状态: 规划中

## 概览

TODO.md 所列 19 项全部验证属实（详见底部验证矩阵）。按影响面和紧急度分为三个批次：

| 批次     | 项数 | 预估工作量 | 描述                             |
|--------|----|-------|--------------------------------|
| P0 救火  | 3  | 0.5h  | 语法错误、链接缺失、命名不统一 —— 用户一眼可见的 bug |
| P1 核心  | 10 | 3-5d  | 文档质量、UX、内容补充 —— 决定文档站专业度       |
| P2 工程化 | 6  | 2-3d  | 开发体验、部署完整性、仓库治理                |

---

## P0 — 紧急修复 (3 项)

### #15 修复 `**语言与框架 **` 语法错误

**文件**: `iot-dc3/docs/zh/index.md:72-73`

**问题**: `**语言与框架` 换行后 `**：` 导致两个 `**` 跨行错位，星号字面显示而非加粗。

**修法**: 将跨行 `**` 改为 `<br>` 或写成单行：

```markdown
<!-- 当前（错误） -->
- **语言与框架
  **：[Java 21]...

<!-- 修后 ═╗ 方案A：用 <br> 保持换行 -->
- **语言与框架**<br>：[Java 21]...

<!-- 方案B：写成单行 -->
- **语言与框架**：[Java 21]...
```

**验证**: `pnpm --filter docs docs:dev` 预览首页，确认 "语言与框架" 加粗正常。

---

### #1 添加 Gitee + Twitter/X 社交链接

**文件**: `iot-dc3/docs/.vitepress/config.mts:499-501`

**问题**: `socialLinks` 仅含 GitHub，缺少 Gitee 和 Twitter。

**修法**:

```typescript
socialLinks: [
    { icon: 'github', link: 'https://github.com/pnoker/iot-dc3' },
    { icon: { svg: '<svg>...</svg>' }, link: 'https://gitee.com/pnoker/iot-dc3' },  // Gitee 图标
    { icon: 'x', link: 'https://x.com/pnoker' }  // VitePress 内置 x 图标
]
```

**注意**: VitePress 内置了 `x` (Twitter) 图标，但 Gitee 图标需要自定义 SVG。同时检查 footer 是否需要加链接。

**验证**: `pnpm --filter docs docs:dev` 确认导航栏三个社交图标可点击、正确跳转。

---

### #10 "接入" → "驱动" 命名统一

**文件**:

- `iot-dc3/docs/.vitepress/config.mts:153` — `navZh: '接入'` → `navZh: '驱动'`
- `iot-dc3/docs/zh/index.md:36` — `title: 接入` → `title: 驱动`，`linkText: 接入设备` → `linkText: 接入设备`（保留，语义不同）

**修法**: PILLARS[3] 的 `navZh` 从 `'接入'` 改为 `'驱动'`。index.md 的 feature card title 同步修改。但 `linkText: '接入设备'`
保留（动词"接入"与名词"驱动"不冲突）。

**验证**: 导航栏显示"驱动"，首页 feature 卡片显示"驱动"，sidebar 标题为"接入指南"（保留）。

---

## P1 — 核心改进 (10 项)

### #5 Mermaid → 架构图组件替换

**范围**: `docs/zh/` 下 16+ 个 .md 文件中的 mermaid 代码块

**策略**: 沿用项目已有的 `.dc3-diagram` + SVG 组件范式（DataPlane / CommandPlane / AuthFlow / FourLayers
等），将关键图表逐个替换。不是全删 mermaid —— mermaid 作为 Markdown 原生语法的轻量图表在某些场景仍有用（如简单流程图、状态图），但
**架构级、数据流级、部署拓扑级**图表必须用组件。

**替换优先级**:

| priority | 文件                            | 当前 mermaid | 替换方案                                  |
|----------|-------------------------------|------------|---------------------------------------|
| 1        | architecture/index.md         | 分层架构图      | 已有 `<Architecture />` 但需增强交互（点击跳转子页面） |
| 2        | architecture/data-plane.md    | 数据流时序图     | `<DataPlane />` 组件（已有）                |
| 3        | architecture/command-plane.md | 命令下发时序图    | `<CommandPlane />` 组件（已有）             |
| 4        | architecture/auth-rbac.md     | 鉴权流程图      | `<AuthFlow />` 组件（已有）                 |
| 5        | architecture/domain-model.md  | ER 图       | 新建 `<DomainModel />` 组件               |
| 6        | architecture/services.md      | 服务拓扑图      | 新建 `<ServiceTopology />` 组件           |
| 7        | foundations/ 下各页              | 四层架构、协议栈   | `<FourLayers />` 组件（已有），其余用新组件        |
| 8        | ai/agentic.md                 | Agent 交互时序 | 新建 `<AgenticFlow />` 组件               |
| 9        | quickstart/index.md           | 启动流程图      | 轻量简单，保留 mermaid（项目内 Bootstrap 类流程图例外） |
| 10       | operation/data-commands.md    | 数据流        | 保留 mermaid 或新建精简组件                    |

**架构图组件规范**（参考 memory `dc3-docs-diagram-preference.md`）:

- `.dc3-diagram` 调色板：主色 `#0e9f6e`（品牌绿）、辅色 `#1296db`（品牌蓝）、背景 `#f6f9fc`
- 组件放在 `.vitepress/theme/components/` 下
- props: `lang: 'zh' | 'en'` 支持双语
- 每个组件 `<script setup>` + `<template>` (SVG) + `<style scoped>`
- 关键节点支持 `@click` 跳转到对应文档页

**工作量**: 每个新组件 ~2-4h，替换+调优每页 ~0.5-1h。保守估计 3-4 个新组件 + 10 页替换 = 2-3d。

**验证**: `pnpm --filter docs docs:build` 无错误 + `pnpm docs:preview` 逐页检查图表渲染。

---

### #6 图片放大功能

**范围**: 文档站全局

**方案**: VitePress 无内置 lightbox。两种方案：

| 方案                       | 优点                      | 缺点                              |
|--------------------------|-------------------------|---------------------------------|
| A. medium-zoom 插件        | 轻量 (~3KB)，无依赖，纯 vanilla | 需在 theme setup 中 `onMounted` 调用 |
| B. VitePress 自定义 wrapper | 无需外部依赖，可定制              | 需手写点击→全屏逻辑                      |

**推荐方案 A**: 在 `.vitepress/theme/index.ts` 的 `onMounted` 中 `import mediumZoom from 'medium-zoom'` 并
`mediumZoom('.main img')`。

```typescript
// .vitepress/theme/index.ts
import mediumZoom from 'medium-zoom'
import 'medium-zoom/dist/style.css'

export default {
  extends: DefaultTheme,
  setup() {
    onMounted(() => mediumZoom('.vp-doc img:not(.no-zoom)', { background: 'rgba(0,0,0,0.8)' }))
  }
}
```

**验证**: 文档站图片可点击放大，深色背景遮罩，esc/点击外部关闭。

---

### #7 代码片段样式

**范围**: 全局 CSS

**问题分析**: 当前 VitePress 默认的 Shiki 代码高亮 + 默认排版，代码块与整体品牌调性不一致。

**具体调整项**:

1. 代码块字体：`'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace`（需要时从 Google Fonts 加载或 fallback 系统字体）
2. 代码块边框颜色：品牌绿 `#0e9f6e` 左边框（4px）配合品牌调色板
3. 行内代码背景色：`rgba(14, 159, 110, 0.08)`
4. 代码块标题栏（如果 VitePress 支持 `title` 属性）：浅绿背景
5. 暗色模式适配

**文件**: `iot-dc3/docs/.vitepress/theme/style.css`

**验证**: dev 预览查看各页代码块效果，明暗双模式切换确认。

---

### #4 README 中架构图组件

**范围**: `iot-dc3/docs/zh/index.md` + `iot-dc3/README.md`

**当前状态**: `zh/index.md:66` 已有 `<Architecture lang="zh" />` 组件。但 `iot-dc3/README.md` 是纯 Markdown，不能直接用
Vue 组件。

**方案**:

- `zh/index.md`: 已有 Architecture 组件，确认是否满足需求；增强交互性
- `iot-dc3/README.md`: 用 `dc3/images/` 下的 SVG/PNG 架构图代替（README 是纯 MD，无法用 Vue 组件）

**验证**: VitePress 首页架构图正常渲染；GitHub README 页面架构图正常显示。

---

### #8 架构/总览/基础内容补充

**策略**: 对标 `iot-books/` 中的 IoT 书籍，系统性补充理论深度和实践案例。

**当前状态**:

- `foundations/` 8 页覆盖了四层架构框架，但多为概念简述
- `introduction/` 12 页覆盖核心概念，较完整
- `architecture/` 8 页覆盖关键设计，较完整

**补充计划**:

| 区域                        | 当前     | 目标                     | 对标书籍                                |
|---------------------------|--------|------------------------|-------------------------------------|
| foundations/sensing       | 基础传感概念 | 补充传感器分类、选型指南、工业传感器校准   | iot-books/sensing-identification/   |
| foundations/fieldbus      | 协议列表   | 深入各协议工作原理、适用场景对比矩阵     | iot-books/communication-networking/ |
| foundations/edge-cloud    | 浅述     | 补充边缘计算架构设计模式、云边协同策略    | iot-books/platform-cloud-bigdata/   |
| foundations/aiot          | 概览     | 补充行业案例（智能制造、能源管理、智慧城市） | iot-books/app-dev-programming/      |
| architecture/domain-model | 概念     | 补充实体生命周期、状态机细节         | -                                   |
| introduction/paths        | 角色路径   | 按角色（运维/开发/架构师）细化学习路线   | -                                   |

**工作方式**: 每项约 1-2h 研读书籍 + 0.5-1h 撰写。

**验证**: `pnpm --filter docs docs:build` 无 broken links，内容逻辑自洽。

---

### #11 菜单顺序调整

**文件**: `iot-dc3/docs/.vitepress/config.mts:35-271`

**当前顺序** (PILLARS 数组):

```
总览 → 基础 → 架构 → 接入 → 运维 → 开发
```

**期望顺序**:

```
总览 → 架构 → 驱动 → 基础 → 开发 → 运维
```

**修法**: 调整 PILLARS 数组的顺序，并确保 `activeMatch` 和 sidebar `paths` 配置相应调整。`首页` 已在 nav builder
中作为第一项，不需改动。

**风险评估**: 中等。移动 pillar 位置需要同步调整 sidebar 的 `paths` 和 `activeMatch` 正则，避免导航高亮错位。

**验证**: `pnpm --filter docs docs:dev` 逐项点导航，确认高亮正确、sidebar 对应。

---

### #12 前端启动说明文档

**问题**: `zh/frontend/` 目录下有 4 个文件但**不在 sidebar 中**，且无启动指南。

**方案**:

1. 新建 `zh/frontend/index.md` — 前端启动说明（安装 pnpm、配置环境变量、`pnpm dev`、端口说明）
2. 将 `frontend-testing-guardrails.md` 和 `test-debugging.md` 整理为正式文档
3. 删除 `bad.md`（反例开发笔记）
4. 在 `config.mts` 的 PILLARS 中新增 frontend pillar 或并入 Development pillar

**前端 pillar 结构**:

```
开发 pillar
  ├── development/           ← 已存在
  ├── frontend/              ← 新增
  │   ├── index.md           启动说明
  │   ├── project-structure.md  项目结构
  │   └── testing.md         测试指南（合并现有测试文件）
  ├── ai/                    ← 已存在
  └── automation/            ← 已存在
```

**验证**: 导航栏可看到前端文档入口，sidebar 正确展开。

---

### #13 前端截图

**问题**: 文档站正式发布的页面中无任何前端界面截图。

**方案**:

1. 启动前端 `pnpm dev` → 浏览器截图
2. 关键页面清单：
    - 登录页 (`/login`)
    - 仪表盘主页 (`/home`)
    - 设备列表 (`/device`)
    - 设备详情 (`/device/:id`)
    - 驱动列表 (`/driver`)
    - 位号管理 (`/point`)
    - 物模型管理 (`/profile`)
    - 系统设置 (`/settings`)
    - AI 对话 (`/chat`)
3. 截图放 `docs/public/images/screenshots/`
4. 在对应文档页中引用

**验证**: 各文档页图片正常加载，支持放大（#6 已实现）。

---

### #14 图文用户手册

**范围**: 全新内容，文档站最大工作量项。

**结构设计**:

```
operation/
  ├── index.md              概览（已有）
  ├── user-manual/           用户手册（新增）
  │   ├── index.md           总览
  │   ├── login.md           登录与退出
  │   ├── dashboard.md       仪表盘
  │   ├── device-management.md   设备管理
  │   ├── driver-management.md   驱动管理
  │   ├── point-management.md    位号管理
  │   ├── profile-management.md  物模型管理
  │   ├── alarm-management.md    告警管理
  │   ├── event-management.md    事件管理
  │   ├── command-management.md  命令管理
  │   ├── settings.md        系统设置
  │   └── ai-assistant.md    AI 助手
  ├── data-commands.md       数据与命令（已有）
  └── alarms.md              告警与通知（已有）
```

**每页结构**: 功能说明 → 前置条件 → 操作步骤（图文）→ 常见问题 → 相关链接

**工作量**: 12 页 × 1h = ~1.5d（含截图、撰写、交叉链接）

**验证**: 逐页走通操作流程，截图与文字一致。

---

### #16 Q&A 页面

**文件**: 新建 `zh/community/faq.md`

**内容大纲**:

1. **许可证与授权**
    - AGPL-3.0 对我有什么影响？
    - 商业授权如何获取？
    - 可以闭源二次开发吗？
2. **收费问题**
    - IoT DC3 本身收费吗？
    - 有哪些付费服务？
3. **技术选型**
    - 为什么用 Java 而不是 Go/Node.js？
    - 为什么用 PostgreSQL 而不是 MySQL？
    - 支持哪些设备协议？怎么选择？
4. **部署运维**
    - 最低硬件要求？
    - 如何从开发环境迁移到生产？
    - 数据备份怎么做？
5. **社区参与**
    - 如何参与贡献？
    - 遇到问题怎么求助？

**验证**: `pnpm --filter docs docs:dev` 确认 Q&A 页面渲染正确，sidebar 可见。

---

## P2 — 工程化改进 (6 项)

### #2 README Logo 更新

**问题**: `iot-dc3/README.md` 引用 `dc3/images/logo-blue.png`，需确认是否为最新品牌 logo。

**操作**:

1. 确认最新的 logo 文件位置（`docs/public/images/logo.svg`？`dc3/images/logo-blue.png`？）
2. 如果是 SVG 版本，`iot-dc3/README.md` 中 `<img>` 标签引用 SVG
3. 确保 GitHub README 渲染正常（GitHub 支持 SVG）

**验证**: GitHub 上查看 README 效果。

---

### #3 仓库合并与 Web 归档

**问题**: monorepo 已有 `iot-dc3-web/`（在 `iot-dc3/dc3-web/` 下）和 `dc3-cli/`，但：

- `zh/index.md:77-78` 仍引用 `https://github.com/pnoker/iot-dc3-web` 外部仓库
- 未在 README 中说明 web 项目后续由后端 monorepo 统一管理

**方案**:

1. 更新 `zh/index.md` 技术栈中的外部链接，改为 `./dc3-web/` 相对路径或直接描述
2. 在 `iot-dc3/README.md` 中添加说明：前端已并入 monorepo，原 `iot-dc3-web` 独立仓库已归档
3. 检查 `iot-dc3-web/` 独立仓库是否需要添加归档说明

**验证**: 文档链接正确，前端源码路径可访问。

---

### #9 快速开始补充 IDEA 细节

**问题**: `quickstart/` 虽有 3 个文件，但对 JetBrains IDEA 用户不够友好。

**补充内容**:

1. **IDEA 导入步骤**（截图+文字）：
    - Open → 选择 `iot-dc3/pom.xml` → Open as Project
    - 等待 Maven 索引完成
2. **EnvFile 插件安装与配置**：
    - 插件市场搜索 "EnvFile"
    - Run Configuration → EnvFile tab → 添加 `dc3/env/dev.env`
3. **IDEA 运行配置**：
    - 各服务的 Spring Boot Run Configuration
    - 各服务的端口、VM options、program arguments
4. **常见 IDEA 问题**：
    - Lombok 注解处理器未启用
    - Maven 索引卡住
    - 模块未被识别

**验证**: 按文档步骤在 IDEA 中操作可成功启动。

---

### #17 开发指南/贡献指南增强

**问题**: contributing.md (179行) 和 development/index.md (136行) 内容偏概念化，"看不懂"。

**诊断**: 当前文档假设读者已熟悉 Spring Boot 生态和 Maven 多模块项目，对不熟悉这些的开发者门槛过高。

**补充内容**:

1. **开发指南**:
    - "第一行代码" 教程：从克隆到提交一个最简单的 Controller
    - 多模块项目导航：哪个模块干什么、依赖关系图
    - 常用 Maven 命令速查
    - 调试技巧（IDEA 远程调试、日志级别调整）
2. **贡献指南**:
    - 从 Issue 到 PR 的完整流程视频/截图
    - PR 模板解读：每项填什么
    - CI 失败怎么排查
    - 代码评审常见反馈及修法

**验证**: 让初级开发者按文档操作，收集反馈。

---

### #18 Maven Wrapper

**文件**: 新增 `iot-dc3/.mvn/wrapper/maven-wrapper.properties` + `mvnw` + `mvnw.cmd`

**操作**:

```bash
cd iot-dc3
mvn wrapper:wrapper -Dtype=only-script  # 只生成 wrapper 脚本，保持已有 .mvn/settings.xml
```

**注意事项**:

- 保留已有的 `.mvn/settings.xml`、`maven.config`、`jvm.config`、`settings-container.xml`
- `maven-wrapper.properties` 中指定 Maven 版本（与项目当前 Maven 3.9+ 一致）
- 更新 `AGENTS.md` 中的命令从 `mvn` 改为 `./mvnw`
- `mvnw` 需 `chmod +x` 并提交到 git

**验证**: `./mvnw compile` 在无全局 Maven 安装的环境中正常工作。

---

### #19 Docker Compose 驱动补充

**问题**: `dc3/docker-compose.yml` 仅有 8 个驱动，缺少 20 个已开发但未容器化的驱动模块。

**策略**: 不是全部 28 个驱动都必须进 compose（有些是嵌入式/被动模式，不需要独立容器），按需补充。

**补充清单**（按优先级）:

| 批次 | 驱动                                   | 理由         |
|----|--------------------------------------|------------|
| 1  | bacnet-ip, fins, melsec, ethernet-ip | 工业总线类高频使用  |
| 2  | iec104, sl651, snmp, dlms            | 电力/SCADA 类 |
| 3  | coap, lwm2m, http, ble, zigbee, can  | IoT/无线类    |
| 4  | serial, tcp-udp                      | 通用接入       |
| 5  | mysql, postgresql, oracle, sqlserver | 数据库类       |

**每个驱动 compose 服务模板**:

```yaml
  dc3-driver-<name>:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-driver-<name>:${DC3_IMAGE_TAG:-2026.6}
    container_name: dc3-driver-<name>
    hostname: dc3-driver-<name>
    restart: unless-stopped
    ports:
      - "${DC3_DRIVER_<NAME>_PORT:-<default>}:<default>"
    volumes:
      - logs:/dc3-driver/dc3-driver-<name>/dc3/logs
    environment:
      <<: *dc3-env
    networks:
      - dc3-driver-<name>
```

**注意**: 部分驱动（如 lwm2m、ble、zigbee、can）依赖特殊硬件或系统权限，compose 中需标注 `profiles` 或条件启动。

**验证**: `podman compose -f dc3/docker-compose.yml config --quiet` 通过。

---

## 建议执行顺序

```
Week 1: P0 (0.5d) + P2 #18 #19 (1d) + P1 #15 #1 #10 #2 #11 (0.5d)
Week 2: P1 #6 #7 #12 #4 (1.5d) + P1 #16 (0.5d) + P2 #9 #17 (1d) + P2 #3 (0.5d)
Week 3-4: P1 #5 #8 #13 #14 (3-5d) ← 最大工作量
```

---

## 附录: 验证矩阵

| #  | TODO            | 验证结果    | 证据位置                                      |
|----|-----------------|---------|-------------------------------------------|
| 1  | 缺 Gitee/Twitter | ✅ 真实    | `config.mts:500` 仅 GitHub                 |
| 2  | logo 未更新        | ✅ 真实    | `iot-dc3/README.md:14` 引用 `logo-blue.png` |
| 3  | 仓库合并/归档         | ⚠️ 部分完成 | monorepo 已有 web+cli，但 index.md 仍引外部仓库     |
| 4  | 架构图组件           | ✅ 真实    | 仅 1 个 `<Architecture>` 组件，README 无图       |
| 5  | mermaid 体验差     | ✅ 真实    | 16+ 文件用 mermaid                           |
| 6  | 无图片放大           | ✅ 真实    | docs 未找到 zoom/lightbox                    |
| 7  | 代码片段样式          | ✅ 真实    | style.css 无代码块定制                          |
| 8  | 内容不足            | ✅ 真实    | foundations 各页偏概念简述                       |
| 9  | 快速开始不细          | ✅ 真实    | 3 文件，无 IDEA 截图步骤                          |
| 10 | 接入→驱动           | ⚠️ 部分完成 | 目录已是 drivers，nav 仍写"接入"                   |
| 11 | 菜单顺序            | ✅ 真实    | 当前: 总览→基础→架构→接入→运维→开发                     |
| 12 | 缺前端启动           | ✅ 真实    | frontend 目录不在 sidebar 中                   |
| 13 | 缺前端截图           | ✅ 真实    | docs 中图片引用为 0                             |
| 14 | 缺用户手册           | ✅ 真实    | 无图文逐页操作手册                                 |
| 15 | `**` 语法错误       | ✅ 真实    | `zh/index.md:72-73`                       |
| 16 | 缺 Q&A           | ✅ 真实    | community 无 faq.md                        |
| 17 | 指南太简单           | ✅ 真实    | 179+136行偏概念化                              |
| 18 | Maven Wrapper   | ✅ 真实    | 有 `.mvn/` 但无 `mvnw`                       |
| 19 | compose 驱动不全    | ✅ 真实    | 8/28 驱动在 compose 中                        |
