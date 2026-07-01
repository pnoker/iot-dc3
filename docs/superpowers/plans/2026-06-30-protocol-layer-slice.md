# 协议层垂直切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用「协议层」主题端到端跑通"docs 学术溯源 + 代码缺陷分析"双目标，验证方法论可否铺开到其余 91 页。

**Architecture:** 4 步工作流——①读 PDF 建知识条目 → ②注入 docs（带 verify）→ ③复用既有 analysis + 增量缺陷分析 → ④复盘。verify 对齐项目既有的「带 `文件:行` 证据 + 源码/原文核对」范式（见 `specs/2026-06-23-docs-review-findings.md`），注入与 verify 分属不同子 agent 上下文。

**Tech Stack:** VitePress markdown、Java 21 源码、PDF（Read 工具 `pages` 参数）、git。

## Global Constraints

- **路径根**：`PROJ=/Users/pnoker/Code/pnoker/IoTDC3/github`。后端 git 仓库 = `$PROJ/iot-dc3`（`develop` 分支）；书库 = `$PROJ/iot-books`；docs = `$PROJ/iot-dc3/docs`。Read 工具需把 `$PROJ` 展开为绝对路径。
- **产物位置**（对齐 `docs/superpowers/documentation/index.md` 文档宪法）：知识条目/缺陷/复盘 → `$PROJ/iot-dc3/docs/superpowers/analysis/`；docs 改动 → `$PROJ/iot-dc3/docs/zh/`。
- **引用格式**：inline 括号引用 + 页末 `## 参考文献` 编号列表。VitePress 无 markdown-it-footnote 插件，**禁用 `[^1]` 脚注语法**。条目格式：`《书名》，作者，出版社·年份，第 X 章「章节名」，第 Y 页。`
- **措辞**（文档宪法）：禁用"生产级闭环""完全领先""一键无忧"等难验证措辞。
- **commit**：docs/analysis 改动 commit 到 `iot-dc3` 仓库，Conventional Commits（`docs(...)` scope），**不含 `Co-Authored-By`**。
- **verify 铁律**：每条注入引用必须由独立子 agent 回读 PDF 对应页核对原句+页码；伪造或核对不上 → 删除该引用，宁缺毋假。全量核对（切片规模小，不抽样）。
- **docs 验证命令**：`pnpm --dir $PROJ/iot-dc3/docs build`。

---

## File Structure

**创建（analysis 工作产物）：**
- `$PROJ/iot-dc3/docs/superpowers/analysis/protocol-knowledge-entries.md` — 知识条目表（概念→书→页码→原句），可复用
- `$PROJ/iot-dc3/docs/superpowers/analysis/protocol-defects.md` — 协议层缺陷清单（三分类）
- `$PROJ/iot-dc3/docs/superpowers/analysis/protocol-slice-retrospective.md` — 方法论复盘

**修改（docs 发布页，commit）：**
- `$PROJ/iot-dc3/docs/zh/foundations/fieldbus.md` — 注入工业协议溯源引用 + 参考文献
- `$PROJ/iot-dc3/docs/zh/foundations/iot-protocols.md` — 注入 IoT 协议溯源引用 + 参考文献
- `$PROJ/iot-dc3/docs/zh/drivers/index.md` — 注入"协议适配层"架构论证 + 少量引用

**只读（复用/核查）：**
- `$PROJ/iot-dc3/docs/superpowers/analysis/iot-dc3-vs-iot-communication.md`（目标②底座）
- `$PROJ/iot-dc3/docs/superpowers/specs/2026-06-23-docs-review-findings.md`（已知错误 + verify 范式）
- `$PROJ/iot-dc3/dc3-driver/dc3-driver-{mqtt,lwm2m,iec104,dlms,ethernet-ip,can,modbus-tcp,opc-ua,plcs7}/...`
- `$PROJ/iot-books/communication-networking/*.pdf`

---

### Task 1: 建 knowledge-entries 表 + 读 fieldbus 书提取条目

**Files:**
- Create: `$PROJ/iot-dc3/docs/superpowers/analysis/protocol-knowledge-entries.md`
- Read: `$PROJ/iot-books/communication-networking/iot-soul-protocols-and-operating-systems-vol1.pdf`

**Interfaces:**
- Consumes: `docs/zh/foundations/fieldbus.md`（确定要补溯源的四个主题：三种通信模型 / 寻址（寄存器·标签·对象）/ 字节序 ABCD·CDAB·BADC·DCBA / 轮询机制）
- Produces: `protocol-knowledge-entries.md` 的「fieldbus 条目」区块，每条字段 = `概念 | 可引用原句 | 书/作者/社年/章/页 | 对应 docs 页锚点 | verify 状态`

- [ ] **Step 1.1：读 PDF 目录定位章节**

Read `$PROJ/iot-books/communication-networking/iot-soul-protocols-and-operating-systems-vol1.pdf` `pages="1-15"`，找到工业总线 / 协议（Modbus / OPC / 寻址 / 字节序）相关章节，把「章节名 + 起止页码」记入 `protocol-knowledge-entries.md` 顶部的「来源定位」表。

- [ ] **Step 1.2：读目标章节正文**

对 Step 1.1 定位到的每个章节，Read `pages="<该章节页码范围>"`（单次 ≤20 页，超长分批）。

- [ ] **Step 1.3：提取知识条目**

针对 fieldbus 四主题各提取 1-2 条**可引用原句**，按表格式追加到 `protocol-knowledge-entries.md` 的 fieldbus 区块。每条必须含真实页码（来自 Step 1.2 实读，不得推测）。

- [ ] **Step 1.4：verify（独立子 agent）**

派一个**新子 agent**（`superpowers:subagent-driven-development` 的 verify 角色，独立上下文），输入 = fieldbus 条目列表 + PDF 路径，指令："对每一条，回读 PDF 的 cited 页码 ±2 页，核对：①原句是否真实存在；②页码是否准确；③概念归属是否断章取义。输出逐条 `通过 / 不符(原因)`。" 不得让注入 agent 自己 verify。

- [ ] **Step 1.5：修正不符条目**

对 verify 标「不符」的：能修正页码/原句的则修正；核不上的**删除**。在表里把 verify 状态改为 `已核` 或 `已删`。

- [ ] **Step 1.6：commit**

```bash
git -C $PROJ/iot-dc3 add docs/superpowers/analysis/protocol-knowledge-entries.md
git -C $PROJ/iot-dc3 commit -m "docs(superpowers): add fieldbus knowledge entries with verified citations"
```

---

### Task 2: 读 iot-protocols 相关书提取条目

**Files:**
- Modify: `$PROJ/iot-dc3/docs/superpowers/analysis/protocol-knowledge-entries.md`
- Read: `iot-soul-protocols-and-operating-systems-vol2.pdf`、`nb-iot-technology-analysis-and-cases.pdf`、`5g-iot-and-nb-iot-technology-guide.pdf`

**Interfaces:**
- Consumes: `docs/zh/foundations/iot-protocols.md`（补溯源主题：MQTT QoS 0/1/2 / CoAP 请求-响应+Observe / LwM2M 对象树 / LPWAN 权衡）
- Produces: `protocol-knowledge-entries.md` 的「iot-protocols 条目」区块

- [ ] **Step 2.1：分别读三本 PDF 目录定位章节**

对 vol2、nb-iot、5g 三本各 Read `pages="1-15"`，定位 MQTT/CoAP/LwM2M/LPWAN/NB-IoT 章节，记页码到「来源定位」表。

- [ ] **Step 2.2：读目标章节正文**

Read 各章节页码范围（≤20 页/次）。

- [ ] **Step 2.3：提取知识条目**

针对 iot-protocols 四主题各提取 1-2 条可引用原句，追加到 `protocol-knowledge-entries.md` 的 iot-protocols 区块。

- [ ] **Step 2.4：verify（独立子 agent）**

同 Step 1.4，对 iot-protocols 条目全量回读核对。

- [ ] **Step 2.5：修正不符条目**

同 Step 1.5。

- [ ] **Step 2.6：commit**

```bash
git -C $PROJ/iot-dc3 add docs/superpowers/analysis/protocol-knowledge-entries.md
git -C $PROJ/iot-dc3 commit -m "docs(superpowers): add iot-protocols knowledge entries with verified citations"
```

---

### Task 3: 注入 fieldbus.md + verify + 构建 + commit

**Files:**
- Modify: `$PROJ/iot-dc3/docs/zh/foundations/fieldbus.md`

**Interfaces:**
- Consumes: `protocol-knowledge-entries.md` 的 fieldbus 区块（已 verify `已核` 的条目）
- Produces: fieldbus.md 的 inline 括号引用 + 页末 `## 参考文献` 小节

- [ ] **Step 3.1：读当前 fieldbus.md 确认注入点**

Read `docs/zh/foundations/fieldbus.md`，定位四主题段落的精确行号，规划每条引用的 inline 插入位置（嵌入现有散文，不另起段）。

- [ ] **Step 3.2：注入 inline 括号引用**

在对应段落末尾加 inline 引用，形如「…寄存器顺序还可能颠倒（ABCD/CDAB/BADC/DCBA 四种排法，见《IoT 协议魂·卷一》第 N 章「字节序」）。」。**只引用 verify `已核` 的条目**。

- [ ] **Step 3.3：加页末参考文献小节**

在「延伸阅读」**之前**插入 `## 参考文献`，列出本页用到的条目（编号列表，`书名/作者/社年/章/页`）。保留原有「延伸阅读」不动。

- [ ] **Step 3.4：verify（独立子 agent）**

派新子 agent，输入 = fieldbus.md 里所有 inline 引用 + 参考文献条目 + 对应 PDF，指令："回读每个 cited 页码，确认原句、页码、章节均属实；另确认 inline 引用与页末参考文献编号一一对应。" 输出逐条核对结果。不符的回到 Step 3.2 修正或删除。

- [ ] **Step 3.5：grep 自检**

```bash
cd $PROJ/iot-dc3 && \
rg -n "## 参考文献" docs/zh/foundations/fieldbus.md && \
rg -n "TBD|TODO|XXX|\[\^" docs/zh/foundations/fieldbus.md || echo "无占位/脚注残留"
```
确认：`## 参考文献` 存在；无 TBD/TODO/`[^` 脚注残留；现有 mermaid 代码块、`::: tip` 容器未被破坏。

- [ ] **Step 3.6：VitePress 构建验证**

```bash
pnpm --dir $PROJ/iot-dc3/docs build
```
预期：构建成功无 error（fieldbus 页被正确渲染）。

- [ ] **Step 3.7：顺手核对 review-findings 已知错误**

`fieldbus.md` 在 `specs/2026-06-23-docs-review-findings.md` 未列已知错误，仅确认本页未引入新事实错误。

- [ ] **Step 3.8：commit**

```bash
git -C $PROJ/iot-dc3 add docs/zh/foundations/fieldbus.md
git -C $PROJ/iot-dc3 commit -m "docs(foundations): add verified academic citations to fieldbus page"
```

---

### Task 4: 注入 iot-protocols.md + verify + 构建 + commit

**Files:**
- Modify: `$PROJ/iot-dc3/docs/zh/foundations/iot-protocols.md`

**Interfaces:**
- Consumes: `protocol-knowledge-entries.md` 的 iot-protocols 区块（已 verify）
- Produces: iot-protocols.md 的 inline 引用 + `## 参考文献`

- [ ] **Step 4.1：读当前 iot-protocols.md 确认注入点**

Read 该页，定位 QoS / CoAP / LwM2M / LPWAN 段落行号。

- [ ] **Step 4.2：注入 inline 括号引用**

同 Step 3.2，对四主题各加 1 处 inline 引用，只引用 `已核` 条目。

- [ ] **Step 4.3：加页末参考文献小节**

同 Step 3.3，在「延伸阅读」前插入 `## 参考文献`。

- [ ] **Step 4.4：verify（独立子 agent）**

同 Step 3.4。

- [ ] **Step 4.5：grep 自检**

同 Step 3.5，目标文件换为 `iot-protocols.md`。

- [ ] **Step 4.6：VitePress 构建验证**

```bash
pnpm --dir $PROJ/iot-dc3/docs build
```

- [ ] **Step 4.7：commit**

```bash
git -C $PROJ/iot-dc3 add docs/zh/foundations/iot-protocols.md
git -C $PROJ/iot-dc3 commit -m "docs(foundations): add verified academic citations to iot-protocols page"
```

---

### Task 5: 注入 drivers/index.md + verify + 构建 + commit

**Files:**
- Modify: `$PROJ/iot-dc3/docs/zh/drivers/index.md`

**Interfaces:**
- Consumes: `iot-books/communication-networking/iot-four-network-convergence-research.pdf`（"协议适配层/四网融合"主题）；`protocol-knowledge-entries.md`
- Produces: drivers/index.md 顶部补一段"协议适配层"架构论证（带 1-2 处引用）+ `## 参考文献`

- [ ] **Step 5.1：读 four-network-convergence 书定位"协议适配层/融合"章节**

Read PDF `pages="1-15"` 定位章节 → 读章节正文，提取 1-2 条关于"异构协议归一到统一数据模型/协议适配层"的可引用原句，追加到 `protocol-knowledge-entries.md` 的 drivers 区块。

- [ ] **Step 5.2：verify 该批次条目（独立子 agent）**

同 Step 1.4，只核对 drivers 区块新条目。

- [ ] **Step 5.3：读当前 drivers/index.md**

Read 该页（内容薄，仅分类表 + 延伸阅读）。

- [ ] **Step 5.4：注入"协议适配层"论证段 + 引用**

在标题下、分类表前插入一段散文（2-4 句）：论证"28 个驱动 = 协议适配层，把异构协议归一为位号值"，内嵌 1-2 处 inline 引用（仅 `已核` 条目）。语气与现有页面一致，不堆砌。

- [ ] **Step 5.5：加页末参考文献小节**

同 Step 3.3。

- [ ] **Step 5.6：verify（独立子 agent）**

同 Step 3.4，核对 drivers/index.md 所有引用。

- [ ] **Step 5.7：grep 自检 + 构建验证**

同 Step 3.5 + 3.6，目标 `drivers/index.md`。

- [ ] **Step 5.8：commit**

```bash
git -C $PROJ/iot-dc3 add docs/zh/drivers/index.md docs/superpowers/analysis/protocol-knowledge-entries.md
git -C $PROJ/iot-dc3 commit -m "docs(drivers): add protocol-adaptation-layer rationale with verified citations"
```

---

### Task 6: 协议层缺陷分析（复用既有 analysis + 增量 + 学术依据）

**Files:**
- Create: `$PROJ/iot-dc3/docs/superpowers/analysis/protocol-defects.md`
- Read: `analysis/iot-dc3-vs-iot-communication.md`（底座）；骨架/抽查驱动源码；`protocol-knowledge-entries.md`（学术依据）

**Interfaces:**
- Consumes: `iot-dc3-vs-iot-communication.md` 的 P0/P1/P2 清单；`dc3-driver/dc3-driver-{mqtt,lwm2m,iec104,dlms,ethernet-ip,can}/src/main/java/...`（骨架完成度）；抽查 modbus-tcp/opc-ua/plcs7
- Produces: `protocol-defects.md`，每条字段 = `编号 | 文件:行 | 协议/驱动 | 对照(书/标准/竞品) | 三分类 | 描述 | 建议`

- [ ] **Step 6.1：复用既有 analysis，逐条核验当前有效性**

Read `analysis/iot-dc3-vs-iot-communication.md`，对其中每条协议层结论（S7 nodave 薄封装、Modbus 模式边界、协议引擎强绑定、字节缓冲缺失、序列化仅限 S7、测试薄弱），**回读当前源码核验是否仍成立**（历史报告可能过时）。仍成立的 → 纳入缺陷清单；已过时的 → 标注"已变化"。

- [ ] **Step 6.2：增量核查骨架驱动完成度**

对 mqtt/lwm2m/iec104/dlms/ethernet-ip/can 各 Read 其 `*DriverCustomServiceImpl.java`（或等价主类），记录 `read()`/`write()`/`health()` 的真实实现状态（骨架/显式 fail-fast/部分实现），与 docs 自标注对照。产出【技术债】/【有意简化】分类条目，每条带 `文件:行`。

- [ ] **Step 6.3：抽查可用驱动找真缺陷**

Read modbus-tcp/opc-ua/plcs7 的核心实现，对照书籍最佳实践（如 Modbus 字节序处理、错误退避"显性失败不伪造成功"——见 fieldbus.md 工程要点），找【真缺陷】候选（如静默吞异常、缓存伪装成功）。

- [ ] **Step 6.4：用学术书籍给关键缺陷补理论依据**

从 `protocol-knowledge-entries.md` 取相关条目，给 2-3 条关键缺陷补"书籍怎么说 / 标准怎么要求"的理论对照（这是既有竞品分析没有的维度）。

- [ ] **Step 6.5：三分类汇总到 protocol-defects.md**

按【真缺陷】/【有意简化】/【技术债】三类归档，每条填全字段。顶部加元信息：日期、底座来源、核验范围。

- [ ] **Step 6.6：verify（独立子 agent 核行号）**

派新子 agent，输入 = 缺陷清单，指令："对每条 `文件:行`，回读该源码位置，确认：①行号定位的代码确实存在；②描述与代码一致；③三分类判断有据。" 输出逐条核对。行号漂移或描述不符 → 修正。

- [ ] **Step 6.7：commit**

```bash
git -C $PROJ/iot-dc3 add docs/superpowers/analysis/protocol-defects.md
git -C $PROJ/iot-dc3 commit -m "docs(superpowers): add protocol-layer defect analysis (reuse + incremental)"
```

---

### Task 7: 汇总 + 方法论复盘

**Files:**
- Create: `$PROJ/iot-dc3/docs/superpowers/analysis/protocol-slice-retrospective.md`

**Interfaces:**
- Consumes: Task 1-6 全部产出
- Produces: 复盘报告 + 是否铺开的结论

- [ ] **Step 7.1：统计产出**

汇总：注入引用总数、verify 拦下的幻觉数、缺陷条目数（按三类）、单页平均成本（PDF 阅读+注入+verify 的 token/轮次）。

- [ ] **Step 7.2：回答 design 第 9 节四个判定问题**

逐条回答：①verify 是否拦住幻觉、漏过几条；②三分类是否区分得了真缺陷 vs 工程取舍、有无误判；③知识条目表格式是否够通用可复用；④单页成本估算铺到 92 页是否可接受。

- [ ] **Step 7.3：给"是否铺开"的结论与建议**

明确：方法论是否验证通过；若铺开，下一批优先做哪些主题/页面；哪些环节需调整（如 verify 抽样比例、PDF 阅读策略）。

- [ ] **Step 7.4：登记 en/ 同步点**

列出 Task 3-5 在 zh/ 注入引用的页面 + 位置，作为 en/ 后续同步的清单（本轮不做）。

- [ ] **Step 7.5：commit**

```bash
git -C $PROJ/iot-dc3 add docs/superpowers/analysis/protocol-slice-retrospective.md
git -C $PROJ/iot-dc3 commit -m "docs(superpowers): add protocol-layer slice retrospective"
```

---

## Self-Review（写计划后自检）

- **Spec 覆盖**：design 第 3 节三页 → Task 3/4/5；目标②复用+增量 → Task 6；verify 对齐既有范式 → 每个 Task 的 verify step；产出物 4 份 → Task 1/2/5（条目表）、3/4/5（docs diff）、6（缺陷）、7（复盘）。✅ 无遗漏。
- **占位扫描**：PDF 页码用"先读目录定位"流程，未编造具体页码（诚实）。无 TBD/TODO。
- **类型一致**：`protocol-knowledge-entries.md` 字段在 Task 1 定义、Task 2/5 追加、Task 6 取用，格式统一。`protocol-defects.md` 三分类在 Task 6 定义、Task 7 统计，一致。
- **commit 一致**：全部 `git -C $PROJ/iot-dc3`、`docs(...)` scope、无 Co-Authored-By。
