# 自动识别与定位切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`。Steps use checkbox (`- [ ]`)。**本计划复用协议层 + 传感层切片（`2026-06-30-protocol-layer-slice.md` + `2026-07-01-sensing-layer-slice.md`）的全部流程与决策**，执行前建议先读 `analysis/sensing-slice-retrospective.md` 了解已验证的方法论。

**Goal:** 用识别/定位主题跑第三个垂直切片，复用协议层 + 传感层方法论。

**Architecture:** 4 步工作流——①读识别/定位书建条目 → ②注入 identification.md（verify）→ ③身份与隔离缺陷分析 → ④复盘。

**Tech Stack:** VitePress markdown、Java 21 源码、PDF（Read `pages` + pdftotext）、git。

## Global Constraints（复用协议层 + 传感层，逐条）

- **路径根**：`PROJ=/Users/pnoker/Code/pnoker/IoTDC3/github`。后端 git 仓库 `$PROJ/iot-dc3`；书库 `$PROJ/iot-books`；docs `$PROJ/iot-dc3/docs`。
- **分支**：`feature/docs-identification-layer-slice`（已从 `main` 切出，因 develop 缺前两切片成果会断链）。
- **产物位置**：`$PROJ/iot-dc3/docs/superpowers/analysis/`；docs 改动 `$PROJ/iot-dc3/docs/zh/`。
- **引用格式**：括号 inline + 页末 `## 参考文献` GB/T 7714 编号列表，**禁 `[^1]` 脚注**。
- **措辞**：禁"生产级闭环""完全领先"等（文档宪法 `documentation/index.md`）。
- **commit**：`docs(...)` scope，**无 Co-Authored-By**。
- **verify 铁律**：每条注入引用由独立子 agent + 主 agent pdftotext 抽 cited 页核对；伪造即删。
- **docs 验证**：`pnpm --dir $PROJ/iot-dc3/docs build`。

## File Structure

**创建**：`analysis/identification-knowledge-entries.md`、`analysis/identification-defects.md`、`analysis/identification-slice-retrospective.md`
**修改**：`docs/zh/foundations/identification.md`
**只读**：
- 设备身份：`DeviceDO`、`DeviceServiceImpl`、`DeviceLockManager`、`DriverMetadata`
- 租户隔离：`TenantOwned`、`TenantContextHolder`、`EntityTenantServiceImpl`、`BaseController`、`AuthenticGatewayFilter`、`DriverMetadataListener`
- 3 本识别/定位 PDF（`iot-books/sensing-identification/`）

---

### Task 1: 识别/定位书条目提取 + verify

**Files:** Create `analysis/identification-knowledge-entries.md`；Read 3 本识别/定位 PDF
**Interfaces:** Consumes `identification.md`（确定主题锚点：RFID 频段/无源有源、NFC、GNSS/北斗、EPC、距离—成本取舍）；Produces 知识条目表

- [ ] 1.1 Read 3 本 PDF `pages="1-15"` 定位目录：`$PROJ/iot-books/sensing-identification/iot-rfid-core-technology-tutorial.pdf` 找 RFID 频段(LF/HF/UHF)/无源有源/读写器-标签/防碰撞章节；`iot-rfid-multi-domain-solutions.pdf` 找差异化应用案例；`iot-beidou-applications.pdf` 找北斗定位/精度章节。记「来源定位」表。**注意两本 RFID 书可能重叠**，以 core-technology-tutorial 为主，multi-domain 只补差异化案例。
- [ ] 1.2 Read 目标章节正文（≤20 页/次），提取条目（每主题 1-2 条）。页码必须实读。北斗书若偏应用层、理论溯源薄，诚实标 N/A。
- [ ] 1.3 写 `identification-knowledge-entries.md`（5 列：概念|原句|出处|docs锚点|verify状态"待核"）。
- [ ] 1.4 verify：独立子 agent 回读 cited 页核对原句/页码；主 agent `pdftotext -f <页> -l <页> <pdf> -` 抽查 2-3 页独立确认。
- [ ] 1.5 不符的修正或删除（宁缺毋假）。
- [ ] 1.6 commit: `docs(superpowers): add identification knowledge entries with verified citations`

### Task 2: 注入 identification.md + verify + 构建 + commit

**Files:** Modify `docs/zh/foundations/identification.md`
**Interfaces:** Consumes `identification-knowledge-entries.md`（已核条目）

- [ ] 2.1 Read `identification.md` 全文，定位 RFID LF/HF/UHF、无源/有源、NFC、GNSS/北斗、UWB、EPC、工程要点（频段合规）段落。
- [ ] 2.2 注入 inline 括号引用（仅已核条目；某主题无书源则保留原文不加，诚实）。
- [ ] 2.3 `## 延伸阅读` 前插入 `## 参考文献`（GB/T 7714）。
- [ ] 2.4 verify（独立子 agent）+ grep 自检：`## 参考文献` 存在、无 `[^`/TBD、mermaid/`:::` 容器完整。
- [ ] 2.5 `pnpm --dir $PROJ/iot-dc3/docs build` 确认无 error。
- [ ] 2.6 commit: `docs(foundations): add verified academic citations to identification page`

### Task 3: 身份与隔离代码缺陷分析 + verify + commit

**Files:** Create `analysis/identification-defects.md`；Read 设备身份 + 租户隔离源码
**Interfaces:** Consumes 源码 + `identification-knowledge-entries.md`（学术依据：RFID/EPC 全局唯一 ID、身份归属边界）

**只读靶子（精确路径）：**
- 设备身份：`dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/entity/model/DeviceDO.java`（`@TableName device`，deviceId 字段）、`dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/service/impl/DeviceServiceImpl.java`（deviceId 生成/唯一性）、`dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/command/DeviceLockManager.java`（按 deviceId 加锁）、`dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/metadata/DriverMetadata.java`（`Set<Long> deviceIds`）
- 租户隔离：`dc3-common/dc3-common-public/src/main/java/io/github/pnoker/common/entity/common/TenantOwned.java`（基类）、`dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/tenant/TenantContextHolder.java`（上下文）、`dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/service/impl/EntityTenantServiceImpl.java`（`requireTenant`）、`dc3-common/dc3-common-web/src/main/java/io/github/pnoker/common/base/BaseController.java`（`requireTenant`/`filterTenant` 统一入口）、`dc3-common/dc3-common-gateway/src/main/java/io/github/pnoker/common/gateway/filter/AuthenticGatewayFilter.java`、`dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/service/DriverMetadataListener.java`

- [ ] 3.1 Read 设备身份 4 个类：`DeviceDO`（deviceId 字段类型/生成注解）、`DeviceServiceImpl`（deviceId 唯一性校验、生成策略）、`DeviceLockManager`（锁粒度/释放）、`DriverMetadata`（deviceIds 集合一致性）。
- [ ] 3.2 Read 租户隔离 6 个类，重点核查：① `DeviceDO` 是否 `extends TenantOwned`（grep 未直接命中继承，需读类声明确认）→ 设备实体是否真受租户隔离；② `BaseController.requireTenant/filterTenant` 是否所有 controller 入口都调用（遗漏 = 跨租户串台风险）；③ `DriverMetadataListener` grep 无 tenant 痕迹 → 驱动元数据链路是否缺租户校验（对应已知 metadata-listener tenant convention）。
- [ ] 3.3 三分类缺陷（真缺陷/有意简化/技术债），每条标 `文件:行`。**诚实——身份/隔离若无真缺陷，如实记录，不强凑**。
- [ ] 3.4 用学术条目给关键缺陷补理论依据（EPC 全局唯一、身份 + 归属二元边界）。
- [ ] 3.5 写 `identification-defects.md`（三分类，每条 `文件:行`）。
- [ ] 3.6 verify：独立子 agent 回读每条 `文件:行` 核对；主 agent `rg -n '<关键字>' <文件>` 抽查。
- [ ] 3.7 commit: `docs(superpowers): add identification identity & tenant isolation defect analysis`

### Task 4: 复盘 + commit

**Files:** Create `analysis/identification-slice-retrospective.md`

- [ ] 4.1 统计产出（条目数、缺陷三类计数、verify 拦幻觉数、单页成本）。
- [ ] 4.2 回答 design 第 8 节复盘标准 + 与协议层/传感层对比：书源驱动策略第三次是否仍有效；目标②靶子性质差异（身份/隔离 vs 数值标定 vs 协议连接）是否印证。
- [ ] 4.3 给"是否继续铺开下一主题"结论 + 下一批建议（foundations 剩余页：fieldbus 等）。
- [ ] 4.4 登记 en/ 同步点（identification.md 注入位置）。
- [ ] 4.5 commit: `docs(superpowers): add identification-layer slice retrospective`

---

## Self-Review

- **Spec 覆盖**：目标①→Task 1/2；目标②→Task 3；复盘→Task 4。✅
- **复用协议层 + 传感层**：Global Constraints 与前两切片 plan 一致；task 结构对齐 sensing plan。
- **identification 特有**：目标②靶子是**身份唯一性 + 租户隔离边界**（Task 3），与传感层（数值标定）性质不同，已标注；RFID 两书重叠预警（以 core-technology-tutorial 为主）；北斗书偏应用层预警；`DriverMetadataListener` 无 tenant 痕迹列为重点核查项（对应已知 metadata-listener tenant convention）。
- **诚实评估**：Task 1 北斗书理论薄则标 N/A；Task 3 隔离无真缺陷则如实记录。
- **Type/路径一致性**：所有源码路径均为 grep 实证存在；`deviceId` 为 `Long` 类型（与 `DeviceDO`/`PointValueDO` 等一致）。
