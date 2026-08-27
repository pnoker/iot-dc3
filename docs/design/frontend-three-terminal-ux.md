# 前端三终端 UX 架构（ADR）

状态：已接受 · 范围：dc3-web（以及未来的 dc3 app 客户端）· 驱动因素：第一性原理 UX 工程

## 背景

dc3-web 是面向 IoT 运维人员的 Vue 3 管理控制台。其体验面向三类设备（桌面、平板、移动），并且后续必须容纳一个原生 app，
作为移动体验的第四个宿主。遗留代码库是桌面优先的：`body` 与布局壳上硬性的 `min-width: 1280px` 下限、固定的表头列、
固定的 220px 设置侧栏（aside），而且没有 JavaScript 断点系统。

临时性的响应式修补（少量 `@media` 块、十个页面上的 `el-col` 响应式属性）已经证明补丁不可扩展：每个新页面都会重新引入
桌面假设。本 ADR 用一套从第一性原理推导出的架构取代打补丁。

## 第一性原理

体验质量可以归结为四个物理量，在任何设备上都成立：

| 要素       | 问题                                                   | 预算              |
|------------|--------------------------------------------------------|-------------------|
| 感知       | 用户能否即刻注意到关键状态？                            | < 100ms           |
| 理解       | 信息层级是否一眼可读？                                  | 首屏 < 3s         |
| 操作       | 用户能否以最小的动作成本完成任务？                      | 步骤最少          |
| 反馈       | 每个操作是否可见且可预期地得到回应？                    | < 300ms           |

设备之间的差异恰好落在四个变量上：输入模态（鼠标 vs 拇指）、视口几何、使用情境（长时间会话 vs 告警响应），以及算力/带宽。
这个产品是一个运维管理控制台：长时间会话中的状态监控，加上高后果操作。

由此，该架构推导出七条公理。每条公理都是一条带有可证伪违反特征的规则，从而可以在评审与 CI 中加以强制。

## 公理

**A1. 内容语义与设备无关。**实体模型、操作模型与 schema 只编写一次，由每个呈现宿主消费。*违反特征：*为另一台设备复制出
第二份字段定义。

**A2. 呈现按设备类重建，而不是缩放。**布局经流式原语（`minmax`、`auto-fit`、可换行 flex）跟随容器几何；粗糙的设备类切换
才是断点的唯一职责。*违反特征：*一次性 `@media` 补丁越堆越多。

**A3. 输入能力决定交互模式。**悬停、右键与行内编辑是鼠标语言；44x44 目标、底部抽屉与滑动是拇指语言。指针能力
（fine/coarse、悬停）是运行期属性，不是媒体查询的猜测——带键盘的平板仍然配得上鼠标交互。*违反特征：*只有悬停才能触发的
操作，或低于 44px 的触控目标。

**A4. 反馈时延就是被感知的产品。**100ms 同步反馈、<1s 骨架屏，再往上是乐观更新。*违反特征：*在网络应答之前毫无反应的
按钮。

**A5. 一致性承载用户的记忆。**Token -> 组件 -> 页面模板，各自拥有单一事实来源；没有硬编码颜色、圆角或魔法宽度。*违反
特征：*SCSS 中非 token 的颜色/间距字面量。

**A6. 状态是体验的一部分。**偏好（主题、密度、语言）、导航位置与草稿表单要跨越设备与会话边界存活。*违反特征：*切换设备或
刷新后用户上下文被重置。

**A7. 无障碍是质量基线。**对比度、键盘焦点、减弱动效与缩放没有商量余地——它们同时也是户外/现场巡检场景的规格。

## 分层模型

```text
L1 Semantic   entity model + operation model + schemas (summary/detail)   device-independent, single truth
L2 Tokens     design tokens + breakpoints + theme (light/dark/auto) + density  shared values
L3 Components ~40 shared components, normalized behavior (loading/empty/error, a11y)
L4 Patterns   4 page templates: monitor / list / detail / edit — three physical implementations each
L5 Quality    measurement -> gates -> regression -> revision (closed loop)
```

每一层只依赖紧邻的下一层。变更只向下传播，绝不横向扩散。

## 边界纪律（复用契约）

Web UI 壳（L3/L4）对原生 app 不可复用；渲染边界之下的层则可以复用。为了让这种复用真实成立，以下内容在 L1/L2 以及未来
共享的 `dc3-sdk` 包中一律禁止：

- 导入 `@/config/*` 的 Vue/Element Plus 基础设施（axios 实例、`ElMessage`、vue-i18n `ComposerTranslation`）；
- 实体 schema 定义中出现 Element Plus 或 Vue 类型（label 是 i18n 键，不是翻译函数）；
- token 源中出现设备特定值（token 是与宿主无关的值；SCSS/CSS 只是它们的一种渲染）。

强制手段：ADR 评审 + lint 规则（SCSS 中不得出现非 token 字面量，L2 之下不得导入框架）。

## 断点契约

单一契约，与 Element Plus `el-col` 语义对齐（A5）：

| 档位 | 范围          | 终端                    |
|------|---------------|-------------------------|
| xs   | < 768px       | 手机                    |
| sm   | 768 - 991px   | 平板                    |
| md   | 992 - 1199px  | 平板 / 小桌面           |
| lg   | 1200 - 1919px | 桌面                    |
| xl   | >= 1920px     | 宽屏桌面                |

JavaScript（`useBreakpoint`）与 CSS 必须都读取这一契约——任何地方都不允许出现第二套手搓断点。

## 决策（推导而得，而非任意挑选）

1. 移动端导航：抽屉菜单，而非底部标签——任务分布是长尾的（首页/告警高频，40+ 个设置页面低频），底部标签对这种分布建模
   很差。
2. 移动端表格：由 L1 摘要 schema 驱动的摘要卡片列表，而非横向滚动。跨行比较——表格的本意——会被横向平移摧毁；粘性首列
   是被接受的过渡状态。
3. 深色模式：在范围内，作为 L2 token 的产物，为的是长会话的眼部负担与更锐利的告警对比，而非装饰。
4. 视觉层：token 化的 Element Plus，不做专属组件库——专属 UI 违背 A5 的经济学（维护成本随组件数量增长）。

## 验收标准

- 360px 到 2560px 之间无页面级横向滚动（表格容器豁免）；
  `document.documentElement.scrollWidth <= window.innerWidth`。
- Lighthouse 移动端：perf >= 90、CLS <= 0.1、LCP <= 2.5s。
- 100% 的触控目标 >= 44x44；768px 以下对话框全屏。
- SCSS 中零硬编码颜色/间距字面量（token lint 闸门）。
- Playwright 运行桌面、平板与移动视口项目；axe 扫描干净；关键页面执行视觉回归。

## 修订记录

2026-08：v1 —— 随 L2 token/断点/主题基座一起采纳（dc3-web src/styles/tokens.scss、theme.scss、
src/composables/useBreakpoint.ts、src/store/modules/app.ts）。

2026-08：v2 —— 布局壳三终端形态发布：共享 NavMenu（水平省略 / 垂直抽屉）、Settings 侧栏菜单抽取出来并托管为
aside（桌面）/ 折叠栏（平板）/ 抽屉（移动）、响应式登录面板、ToolCard 内的移动端紧凑分页。经 artifacts/viewport-check.mjs
验证：在 1440/834/390px 视口下对照 mock 构建零页面级溢出，且各终端 DOM 正确。

2026-08：v3 —— L1 去框架化（dc3-client-sdk Phase 0）：全部 16 个实体配置模块中 Translator 契约取代 vue-i18n
ComposerTranslation；dc3-sdk 抽取设计记录于 docs/design/dc3-client-sdk.md。

2026-08：v4 —— 度量闸门发布：Playwright 新增 chromium-desktop（1440x900）、chromium-tablet（834x1112，触控）与
chromium-mobile（393x851，触控）项目；tests/e2e/specs/responsive.spec.ts 为以下内容设闸：A2 溢出判据
（login/home/settings 上 scrollWidth <= clientWidth）、A3 布局壳适配（菜单条 vs 抽屉、aside vs 抽屉），以及 A7 可访问名称
冒烟探测。对照 mock 构建全部 12 个闸门测试通过。CI（ci-web.yml）经 pnpm test:e2e 且仅用 chromium 自动运行该闸门。已记录
修复：playwright 1.61.1/1.62.0 的版本分裂对齐到 1.62.0；ToolCard 刷新/排序图标按钮补上 aria-label。Lighthouse 预算与
axe-core 扫描仍是 CI 后续事项（本轮不新增依赖）。

2026-08：v5 —— 契约加固 + 首批验收缺口闭合。(1) 断点契约现已去字面量：最后一批手搓宽度阈值（Home 1024/1280/640、告警
Overview 1024/1280/640、AgenticAssistant 900）替换为 $breakpoint-* token，把单列折叠映射到 sm-max、3 列统计网格映射到
md-max、1 列映射到 xs-max。tests/guardrails/breakpoint-contract.test.ts 强制 A5：src 中任何 @media (min/max-width) 必须
引用 $breakpoint-* token。(2) A3 对话框判据落地：theme.scss 在 xs-max 以下将 .el-dialog 重塑为贴合视口
（calc (100vw - 16px)、限高、主体滚动），覆盖 Element Plus 的内联宽度；由一个仅移动端的 Playwright 测试设闸（对话框永不
宽于视口、页面零溢出）。(3) A2 闸门覆盖扩展到布局壳之外：九条模板横扫路由（monitor/list/detail/history 家族——告警概览、
设备、驱动、profile、label、point_value、alarm/point、事件/命令历史）现已在每个终端设闸，闸门测试合计 48 个。仍待解决：
L4 模板推广到其余约 100 个视图（移动端摘要卡片列表受阻于 L1 摘要 schema 决策）、CI 中的 Lighthouse 预算 + axe-core、
token lint。
