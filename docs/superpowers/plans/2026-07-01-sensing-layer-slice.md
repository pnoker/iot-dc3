# 传感与测量切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`。Steps use checkbox (`- [ ]`)。*
*本计划复用协议层切片（`2026-06-30-protocol-layer-slice.md`）的全部流程与决策**，执行前建议先读协议层的 retrospective
> 了解已验证的方法论。

**Goal:** 用传感测量主题跑第二个垂直切片，复用协议层方法论。

**Architecture:** 4 步工作流——①读传感器书建条目 → ②注入 sensing.md（verify）→ ③标定换算缺陷分析 → ④复盘。

**Tech Stack:** VitePress markdown、Java 21 源码、PDF（Read `pages` + pdftotext）、git。

## Global Constraints（复用协议层，逐条）

- **路径根**：`PROJ=/Users/pnoker/Code/pnoker/IoTDC3/github`。后端 git 仓库 `$PROJ/iot-dc3`；书库 `$PROJ/iot-books`；docs
  `$PROJ/iot-dc3/docs`。
- **产物位置**：`$PROJ/iot-dc3/docs/superpowers/analysis/`；docs 改动 `$PROJ/iot-dc3/docs/zh/`。
- **引用格式**：括号 inline + 页末 `## 参考文献` GB/T 7714 编号列表，**禁 `[^1]` 脚注**。
- **措辞**：禁"生产级闭环""完全领先"等（文档宪法 `documentation/index.md`）。
- **commit**：`docs(...)` scope，**无 Co-Authored-By**。
- **verify 铁律**：每条注入引用由独立子 agent + 主 agent pdftotext 抽 cited 页核对；伪造即删。
- **docs 验证**：`pnpm --dir $PROJ/iot-dc3/docs build`。

## File Structure

**创建**：`analysis/sensing-knowledge-entries.md`、`analysis/sensing-defects.md`、`analysis/sensing-slice-retrospective.md`
**修改**：`docs/zh/foundations/sensing.md`
**只读**：`PointBO`/`PointValue`/`ArithmeticUtil`/`TypedValueConverter` 源码；3 本传感器 PDF

---

### Task 1: 传感器书条目提取 + verify

**Files:** Create `analysis/sensing-knowledge-entries.md`；Read 3 本传感器 PDF
**Interfaces:** Consumes `sensing.md`（确定主题锚点：传感器分类/信号链/A-D转换/标定）；Produces 知识条目表

- [ ] 1.1 Read 3 本传感器 PDF `pages="1-15"` 定位目录，找传感器分类/信号调理/ADC/标定章节，记「来源定位」表。**注意 vol1/vol2
  可能重叠**（如协议层 vol1/vol2），取一份。
- [ ] 1.2 Read 目标章节正文（≤20 页/次），提取条目（每主题 1-2 条）。页码必须实读。
- [ ] 1.3 写 `sensing-knowledge-entries.md`（5 列字段：概念|原句|出处|docs锚点|verify状态"待核"）。
- [ ] 1.4 verify：独立子 agent 回读 cited 页核对原句/页码；主 agent pdftotext 抽查 2-3 页独立确认。
- [ ] 1.5 不符的修正或删除（宁缺毋假）。
- [ ] 1.6 commit: `docs(superpowers): add sensing knowledge entries with verified citations`

### Task 2: 注入 sensing.md + verify + 构建 + commit

**Files:** Modify `docs/zh/foundations/sensing.md`
**Interfaces:** Consumes `sensing-knowledge-entries.md`（已核条目）

- [ ] 2.1 Read `sensing.md` 全文，定位传感器分类/信号链/A-D/标定段落。
- [ ] 2.2 注入 inline 括号引用（仅已核条目；某主题无书源则保留原文不加，诚实）。
- [ ] 2.3 `## 延伸阅读` 前插入 `## 参考文献`（GB/T 7714）。
- [ ] 2.4 verify（独立子 agent）+ grep 自检（`## 参考文献` 存在、无 `[^`/TBD、mermaid/`:::` 容器完整）。
- [ ] 2.5 `pnpm --dir docs build` 确认无 error。
- [ ] 2.6 commit: `docs(foundations): add verified academic citations to sensing page`

### Task 3: 标定换算代码缺陷分析 + verify + commit

**Files:** Create `analysis/sensing-defects.md`；Read `PointBO`/`PointValue`/`ArithmeticUtil`/`TypedValueConverter`
**Interfaces:** Consumes 源码 + `sensing-knowledge-entries.md`（学术依据）

- [ ] 3.1 Read `PointBO`（baseValue/multiple/valueDecimal）、`PointValue`/`PointValueBO`（rawValue/calValue）、
  `ArithmeticUtil`（BigDecimal 精度/rounding）、`TypedValueConverter`（类型转换）。
- [ ] 3.2 三分类缺陷：重点查 BigDecimal 精度处理、null baseValue/multiple 处理、类型转换异常、rounding 模式。*
  *诚实——若线性换算无真缺陷，如实记录，不强凑**。
- [ ] 3.3 用学术条目给关键缺陷补理论依据。
- [ ] 3.4 写 `sensing-defects.md`（三分类，每条 `文件:行`）。
- [ ] 3.5 verify：独立子 agent 回读每条 `文件:行` 核对；主 agent rg 抽查。
- [ ] 3.6 commit: `docs(superpowers): add sensing calibration defect analysis`

### Task 4: 复盘 + commit

**Files:** Create `analysis/sensing-slice-retrospective.md`

- [ ] 4.1 统计产出（条目数、缺陷三类计数、verify 拦幻觉数、单页成本）。
- [ ] 4.2 回答 design 第 8 节复盘标准 + 与协议层对比（书源驱动策略是否有效、目标②薄是否印证）。
- [ ] 4.3 给"是否继续铺开下一主题"结论 + 下一批建议。
- [ ] 4.4 登记 en/ 同步点（sensing.md 注入位置）。
- [ ] 4.5 commit: `docs(superpowers): add sensing-layer slice retrospective`

---

## Self-Review

- **Spec 覆盖**：目标①→Task 1/2；目标②→Task 3；复盘→Task 4。✅
- **复用协议层**：Global Constraints 与协议层 plan 一致；task 结构对齐。
- **sensing 特有**：目标②标定换算（Task 3）标注"可能薄、诚实评估"；传感器书 vol1/vol2 重叠预警。
- **执行交接**：本会话 context 已满，执行交新会话。新会话读本 plan + 协议层 retrospective，按 subagent-driven-development 执行
  4 task。
