# 协议层垂直切片 — 方法论复盘（Retrospective）

> **生成日期**：2026-06-30
>
> **范围**：Task 1–7 的总结。回答 design 第 9 节四个判定问题，给出"是否铺开到其余 91 页/其余主题"的结论。本文是切片的收尾，也是后续决策的依据。
>
> **底座**：`progress.md`（进度账本）、`protocol-knowledge-entries.md`（16 条）、`protocol-defects.md`（16 条三分类）、`2026-06-30-protocol-layer-slice-design.md` 第 9 节。

---

## 1. 产出统计

### 1.1 注入与条目

| 维度 | 数量 | 明细 |
|------|------|------|
| docs 注入（inline 引用） | **10 处** | fieldbus 4 + iot-protocols 4 + drivers/index 2 |
| 参考文献小节 | **3 个** | fieldbus.md / iot-protocols.md / drivers/index.md 各 1 |
| 知识条目 | **16 条** | fieldbus 7（含 2 条 N/A）+ iot-protocols 7（含 1 条 N/A）+ drivers 2 |
| 其中诚实标 N/A | **3 条** | 字节序 ABCD/CDAB、fieldbus 轮询、LwM2M 对象树 |
| 缺陷 | **16 条** | 真缺陷 4 / 有意简化 5 / 技术债 7 |
| 复用既有 analysis | 6 条结论 | 3 成立 / 1 部分成立 / 2 已变化 |

### 1.2 verify 与幻觉拦截

| 维度 | 结果 |
|------|------|
| verify 拦截幻觉数 | **0 条幻觉进入最终产出** |
| verify 拦截的"待修正"项 | **2 处**（均被拦截并修正，未流入产出）：① defects T2 原 TODO 行号 209 → 实读 208（行号修正）；② knowledge entries nb-iot 表 1.2 数值列错位 → implementer 自检删数值、仅留维度描述 |
| implementer 诚实标 N/A | 3 条（宁缺毋假，未硬凑字节序/轮询/LwM2M 原句） |
| 主 agent 独立核对 | fieldbus 7 条全核；iot-protocols 5 条独立 pdftotext 抽查 + 2 条信任 implementer 自检；defects 19 处 `文件:行` 由 verify 子 agent 逐行回读 |

### 1.3 单页成本（粗估）

切片处理 3 个 docs 页（fieldbus / iot-protocols / drivers/index），共 4 个 PDF 书源（iot-soul vol1/vol2、5G 书、nb-iot 书、四网融合书）。

| 环节 | 子 agent 调用 / 操作 | 量级 |
|------|---------------------|------|
| PDF 阅读 + 目录定位 + 提取条目 | Task 1（fieldbus）+ Task 2（iot-protocols）各 1 个 implementer 子 agent，含 pdftotext 抽页 + grep 原句 | ~2 子 agent，每页 cited 抽取约 1–2 轮 pdftotext+grep |
| verify（独立核对） | fieldbus 7 条全核；iot-protocols 主 agent pdftotext 抽查 5 页 | ~1 verify 子 agent + 主 agent 抽查 |
| 注入（docs diff） | Task 3/4/5 各 1 个 inject 子 agent + 1 个 inject review | 3 注入 + 3 review |
| 缺陷分析 | Task 6 复用既有 analysis + 增量核查 9 驱动主类，1 个 defect 子 agent + 1 个 defect review | ~2 子 agent |

**单页平均成本（3 页）**：约 10–12 个子 agent 调用（含 review），主 agent 独立 pdftotext/grep 核对约 12 页次。Token 量级：每页注入约 1 个子 agent 上下文（PDF 抽页 + docs 现状 + 条目表），粗估单页 30–50k token（含 review）。

**铺到 92 页的外推**：92 页 ×（10 子 agent / 3 页）≈ **300+ 子 agent 调用**，token 约 3–5M。这是上限估计——批量同主题页可共享书源定位与条目表，实际会更低（见第 4 节）。

---

## 2. 回答 design 第 9 节四个判定问题

### Q1：verify 是否拦住幻觉？漏过几条？

**答：零幻觉流入产出；机制有效。**

机制是双层：
1. **implementer 诚实标 N/A**（第一层）——遇到书源无覆盖的主题（字节序、轮询、LwM2M 对象树），implementer 不硬凑原句，直接标 N/A 并写明"未找到可引用原句"+ 解释原因（如"p416/p451 的轮询指 OS 任务调度，非总线轮询"）。这从源头杜绝了最常见的幻觉来源（把无关文本强行解读为目标概念）。
2. **主 agent 独立 pdftotext 双重核对**（第二层）——不依赖 Read 工具渲染 PDF（渲染成图片无 OCR 文字层），改用 `pdftotext -layout` 命令行抽页 + grep 原句片段，逐字比对。

**verify 拦截的 2 处修正**（均未流入产出）：
- T2 缺陷 TODO 行号 209 → 实读 208（verify 子 agent 回读源码发现 209 是日志参数行）。
- nb-iot 表 1.2 横向对比数值列错位（pdftotext 抽表格列错位）→ implementer 自检发现后删除无法核实的数值，仅保留维度描述。

**结论**：verify 机制（诚实 N/A + 命令行 pdftotext 独立核对）有效，零幻觉漏过。

### Q2：三分类是否真能区分真缺陷 vs 工程取舍？有无误判？

**答：能区分；有 1 条 borderline 经论证后判定，无误判。**

三分类定义清晰：
- 【真缺陷】违反最佳实践且有改进空间（静默吞异常、伪造成功、错误不退避、资源未关闭、线程安全）。
- 【有意简化】工程取舍，记录不判错（骨架驱动 throw NotImplemented 快速失败——诚实设计）。
- 【技术债】已知欠账（CIP 组帧未补、CAN 字节切分未齐、测试覆盖不足）。

**borderline 案例：D4 MQTT health 桩**。`health()` 恒返回 online + TODO。表面看像"骨架阶段的有意简化"（与 S4 iec104 未声明 health 同类）。最终判【真缺陷】，理由：health 是面向调度器的契约——MQTT broker 不可达时仍自报 online，调度器会继续下发写命令并静默失败，**health 在撒谎且误导上层调度**。这与 S5 dlms health 恒返回 offline（骨架阶段无设备能在线，offline 是真实状态）形成对照：同样是桩，offline 诚实、online 欺骗。判定标准因此收敛为：**桩的返回值是否误导上层**。

**无误判**：iec104/dlms 的 `throw NotImplemented`（S1/S2）被判有意简化而非真缺陷，因其快速失败、不返回假数据；modbus-tcp/plcs7 经核查未发现真缺陷，是各自类别的正面范例。

**结论**：三分类有效，关键判据是"是否误导上层/是否伪造成功"，可复用。

### Q3：知识条目表格式是否够通用可复用？

**答：够通用；字段稳定，跨主题/跨书源可复用。**

字段 `概念 | 可引用原句 | 书名/作者/出版社·年份/章节/页码 | 对应 docs 页锚点 | verify状态` 在 Task 1 定义后，Task 2（iot-protocols，多书源）、drivers 区块（不同书源）均直接复用，未改字段。

**两点可改进（Minor，不影响通用性）**：
1. **N/A 条目的格式**：当前用"—（本书无此主题内容）"占位概念列 + 解释列写原因。可考虑统一为 `N/A | <原因> | — | <锚点> | N/A`，让"为何 N/A"更显眼。但这属风格偏好，当前写法已足够诚实可读。
2. **多书源场景的"来源定位"子表**：iot-protocols 区块用了一个"书源 × 章节 × 页码 × 相关性"的来源映射表，这个模式对"一书源覆盖不全、需多书组合"的场景非常实用，建议作为标准组件保留。

**结论**：格式可复用，无需重构。

### Q4：单页成本估算铺到 92 页是否可接受？

**答：上限估计偏高，但需批量优化才可接受；不建议逐页铺开。**

裸算：92 页 × 30–50k token/页 ≈ 3–5M token，300+ 子 agent 调用。这在工程上可承受但低效，且忽略了两个减项和一个增项：

- **减项①：同主题页共享书源**。本次 fieldbus + iot-protocols 共用了 iot-soul 书的同一章，书源定位与目录扫描只做一次。铺开时按"书源 → 主题群"打包，单页边际成本会显著下降。
- **减项②：缺陷分析复用既有 analysis**。Task 6 复用了 2026-05-26 报告的 6 条结论，增量核查只需回读源码。后续主题若有既有 analysis，缺陷部分成本更低。
- **增项：书源覆盖度风险推高成本**。本次 3 条 N/A 暴露：一本通论书覆盖不全一个页面的所有主题。若逐页铺开而不预先匹配书源，会反复出现"读了 PDF 才发现无料"的浪费（本次 fieldbus 字节序/轮询、iot-protocols LwM2M 均为此）。

**结论**：逐页铺开成本不可接受；按"书源 → 主题群"批量铺开 + 预先书源匹配后，成本可接受（详见第 4 节建议）。

---

## 3. 暴露的问题（诚实，供铺开改进）

### 3.1 书源覆盖度风险（核心问题）

一本通论书覆盖不全一个页面的所有主题。本次实证：
- **fieldbus 字节序 ABCD/CDAB/BADC/DCBA**：iot-soul 全书无相关内容（p60"字节流"属 FTP，与 endian 无关）。
- **fieldbus 轮询机制**：p416/p451"轮询"均指 OS 任务调度，非总线轮询。
- **iot-protocols LwM2M 对象树**：vol1/vol2/nb-iot/5g 四本书均无 LwM2M 对象模型，仅作为"设备管理协议"被点名。
- **OPC UA / EtherNet/IP / DLMS / IEC 104 / Profibus / BACnet**：本批次书源均无专门内容，defects E3 只能作间接类比。

**对铺开的影响**：若按"页面 → 找书源"顺序铺开，会反复撞"无料"。必须改为"**先盘点书源覆盖哪些主题，再按书源选主题/页面**"。

### 3.2 引用风格不一致（Minor，铺开前需定规范）

- **inline vs blockquote**：Task 4 的 MQTT（line 43）/ CoAP（line 69）用 `>` blockquote；Task 3/5 用括号 inline（如 fieldbus line 38/40/41/47）。blockquote 更醒目但打断行文，inline 更流畅。
- **参考文献格式**：Task 3（fieldbus）用简单"书名，作者，出版社·年份，章节，页码"；Task 4/5（iot-protocols/drivers）用 GB/T 7714（`作者. 书名[M]. 出版地: 出版社, 年份. ISBN`）。

**建议**：铺开前统一为 GB/T 7714 参考文献 + 括号 inline 引用（blockquote 仅用于完整原句摘录）。progress.md 的 Minor 清单已登记，待 whole-branch review 派 fix。

### 3.3 PDF 读取 workaround

Read 工具渲染 PDF 成图片，无 OCR 文字层，无法 grep。独立核对必须用 `pdftotext -layout <pdf> - | grep` 命令行。这不算 bug（pdftotext 是更可靠的核对工具），但需在流程文档中明确：**verify 一律走命令行 pdftotext，不信 Read 工具的 PDF 渲染**。

---

## 4. 是否铺开的结论 + 建议

### 4.1 结论：方法论验证通过，可铺开，但需按"书源驱动"重组

四个判定问题的答案：verify 零幻觉（机制有效）、三分类可区分（判据明确）、条目表格式可复用、单页成本需批量优化。**方法论本身成立**。

但本次暴露的书源覆盖度风险意味着：**不能逐页铺开，必须按"书源 → 主题群"批量推进**。

### 4.2 铺开建议

**下一批优先做书源充足的主题**（按本次已验证书源覆盖度排序）：

1. **MQTT / CoAP / LPWAN 系列**（书源最充足：iot-soul vol1/vol2 + 5G 书 + nb-iot 书）。可覆盖 `iot-protocols.md` 的 MQTT/CoAP/LPWAN 已注入锚点的深化，以及无线侧其他页面。
2. **HTTP / Web 协议系列**（iot-soul 1.6 HTTP 章已核，p49–57）。可覆盖 foundations 下 Web 协议相关页。
3. **物联网体系结构 / 网关 / 三层架构**（四网融合书第一章第三节已核）。可覆盖 drivers 与 introduction 下的架构页。

**暂缓或需另配书源的主题**：
- OPC UA / EtherNet/IP / DLMS / IEC 104 / Profibus / BACnet（本批次书源无覆盖，需 ODVA / IEC / DLMS UA 规范书或专著）。
- 字节序 ABCD/CDAB（需 Modbus 专著或现场总线工程手册）。
- LwM2M 对象树（需 OMA Spec 或 LwM2M 专著）。

### 4.3 需调整的环节

| 环节 | 调整 |
|------|------|
| 推进顺序 | 改为"书源 → 主题群"批量推进，而非"页面 → 找书源"。先盘点 iot-books/ 全部 PDF 覆盖哪些主题，再按书源打包主题群。 |
| verify 抽样比例 | 本次 defects 19 处全核、knowledge fieldbus 7 条全核、iot-protocols 5/7 独立抽查。铺开时对"逐字原句引用"保持全核（幻觉高风险），对"行号证据"可抽样 30–50%（行号漂移易发现）。 |
| PDF 阅读策略 | verify 一律走 `pdftotext -layout` 命令行，禁用 Read 工具的 PDF 渲染。implementer 提取也走 pdftotext。 |
| 引用风格 | 铺开前先统一：GB/T 7714 参考文献 + 括号 inline 引用（blockquote 仅用于完整原句）。 |
| en/ 同步 | 见第 5 节清单，每批 zh/ 注入后顺手同步 en/（本轮未做）。 |

---

## 5. en/ 同步点清单

以下为 Task 3/4/5 在 zh/ 注入引用的页面 + 位置，作为 en/ 后续同步清单（本轮不做）。

### 5.1 `docs/zh/foundations/fieldbus.md` → `docs/en/foundations/fieldbus.md`

| zh 行 | 位置 | 引用内容 | 书源页码 |
|-------|------|---------|---------|
| 38 | 三种通信模型 - 主从 | Modbus 主从协议（括号 inline） | p165 |
| 40 | 三种通信模型 - 客户端-服务器 | HTTP C/S 请求-响应（括号 inline） | p49、p51 |
| 41 | 三种通信模型 - 发布-订阅 | MQTT pub/sub 代理（括号 inline） | p155 |
| 47 | 寻址 - 数字地址 | Modbus 标准功能码（括号 inline） | p165 |
| 156–160 | 参考文献小节 | 3 条（主从 / C/S / pub-sub） | — |

### 5.2 `docs/zh/foundations/iot-protocols.md` → `docs/en/foundations/iot-protocols.md`

| zh 行 | 位置 | 引用内容 | 书源页码 |
|-------|------|---------|---------|
| 43 | MQTT QoS | MQTT 三档 QoS（blockquote） | p41 |
| 69 | CoAP | CoAP 定义+UDP+方法（blockquote） | p39–40 |
| 123 | LPWAN - LoRa | LoRa = Long Range（括号 inline） | p71 |
| 125 | LPWAN - NB-IoT | NB-IoT 属 LPWAN（括号 inline） | p119 |
| 183–187 | 参考文献小节 | 3 条（GB/T 7714：iot-soul vol2 / 5G 书 / nb-iot 书） | — |

### 5.3 `docs/zh/drivers/index.md` → `docs/en/drivers/index.md`

| zh 行 | 位置 | 引用内容 | 书源页码 |
|-------|------|---------|---------|
| 16 | 协议适配层 | 网关汇聚点 + 统一数据建模（括号 inline，2 处） | p13 |
| 71–73 | 参考文献小节 | 1 条（GB/T 7714：四网融合书） | — |

> **en/ 同步注意**：en/ 页面若锚点 slug 与 zh/ 不同（如 `#三种通信模型` vs `#three-communication-models`），条目表的"对应 docs 锚点"字段需同步更新。引用风格统一为 GB/T 7714 + 括号 inline（见第 3.2 节）。

---

## 6. 一句话总结

协议层垂直切片（3 页 / 16 条目 / 16 缺陷）方法论验证通过——verify 双层机制（诚实 N/A + 命令行 pdftotext）零幻觉、三分类判据明确、条目表可复用；**可铺开，但需从"逐页找书源"改为"书源驱动的主题群批量推进"**，优先做 MQTT/CoAP/LPWAN 等书源充足的主题，OPC UA/EtherNet-IP 等无书源主题暂缓或另配规范书。
