# 识别与定位层切片 — 方法论复盘（Retrospective）

> **生成日期**：2026-07-01
>
> **范围**：Task 1–3 的总结。回答 design 第 8 节复盘标准，与协议层、传感层切片横向对比，给出是否继续铺开的结论。本文是切片的收尾。
>
> **底座**：`analysis/identification-knowledge-entries.md`（实读 12 行条目；Task 1 报告摘要与 brief 称"10 条"系计数偏低）、`analysis/identification-defects.md`（1/3/3 三分类）、`docs/zh/foundations/identification.md` 注入（11 处 inline + 3 条参考文献）、`2026-07-01-identification-layer-slice-design.md` 第 8 节。
>
> **对比**：`protocol-slice-retrospective.md`（协议层，3 页 / 16 条目 / 16 缺陷 4-5-7）、`sensing-slice-retrospective.md`（传感层，1 页 / 7+1 条目 / 0-2-3 缺陷）。

---

## 1. 产出统计

### 1.1 注入与条目

| 维度 | 数量 | 明细 |
|------|------|------|
| docs 注入（inline 括号引用） | **11 处**（散在 10 行） | identification.md 1 个页面；RFID 频段/LF/HF/UHF/无源有源/耦合/防碰撞/EPC/EPCglobal/GNSS 原理/北斗精度/室内失效，对应行 31/32/33/34/36/37/43/85/89/90/96 |
| 参考文献小节 | **1 个**（3 本书） | identification.md `## 参考文献`（GB/T 7714：Core 黄玉兰 2016 / Multi-domain 拉纳辛哈等 2013 中译本 / Beidou 王博等 2020） |
| 知识条目 | **12 条**（Task 1 报告摘要与 brief 均称"10 条"，实读条目表为 12 行——摘要计数偏低，以文件实读为准） | RFID 频段/LF/HF/UHF/无源有源/读写器耦合/防碰撞（7 条 Core）+ EPC 编码体系（Core）+ EPCglobal 网络（Multi-domain，差异化）+ GNSS 原理与精度/北斗精度/室内失效（3 条 Beidou） |
| 诚实标 N/A | **0 条 N/A，但有 3 个锚点诚实保留原文不加引用** | UWB、蓝牙信标、二维码 3 个 docs 锚点书库 3 本 PDF 均无理论章节，Task 2 保留 docs 原述不硬凑引用（与协议层 3 条 N/A 同源机制：宁缺毋假） |
| 缺陷（三分类） | **1 / 3 / 3** | 真缺陷 **1**（D-1 TenantContextHolder fail-closed 契约无 MyBatis 拦截器执行）／ 有意简化 **3**（S-1 deviceName 无 DB 唯一索引 / S-2 DeviceLockManager 进程内锁非分布式 / S-3 DriverMetadataListener/MetadataEventDTO 不带 tenantId）／ 技术债 **3**（T-1 DO 不实现 TenantOwned / T-2 listByIds 无租户参数 / T-3 filterTenant 先查全量再内存过滤） |

### 1.2 verify 与幻觉拦截

| 维度 | 结果 |
|------|------|
| verify 拦截幻觉数（流入产出） | **0** |
| verify 拦截并修正的项 | **1 处**（Task 1 review 发现 EPC 条目原标 PDF p49，主 agent 用 `pdftotext -f 49 -l 49`/`-f 50 -l 50` 双重核对后修正为 p50——§3.1.3 EPC 在 PDF p50 而非 p49；该修正发生在产出固化前，未流入最终条目表） |
| implementer 诚实保留无书源锚点 | 3 处（UWB/蓝牙信标/二维码，3 本 PDF 均无 → 保留原文不加引用） |
| 主 agent 独立核对 | Task 1 对全部 12 条的 12 个 cited 页执行 `pdftotext -f <页> -l <页>` 抽页核对，原句片段逐字命中（含 Core 书系统性 OCR 异体字规范化后比对，文件头已列斱→方、冴→况 等 10 个映射） |

> **verify 拦截数 = 1**（EPC p49→p50）。这是三个切片里首次出现"单条 cited 页码需返工修正"——协议层拦截的是 nb-iot 表格列错位（数据问题）与 T2 行号漂移（行号问题），本切片拦截的是**单条页码错位**，属同一机制（命令行 pdftotext 独立核对）的不同命中面，证明机制对"页码错位"这类细粒度幻觉同样有效。

### 1.3 单页成本（粗估）

切片处理 **1 个 docs 页**（identification.md），3 个 PDF 书源（Core 360 页 / Multi-domain 372 页 / Beidou 325 页，合计 1057 页 PDF，但实际 cited 仅 12 个 PDF 页）。

| 环节 | 子 agent / 操作 | 量级 |
|------|----------------|------|
| PDF 阅读 + 目录定位 + 提取条目 | Task 1 implementer，含 pdftotext 抽页 + grep 原句 + 跨 3 书源重叠处理 | 1 子 agent |
| 注入（docs diff） | Task 2 implementer + pnpm build 自检 + 源书 CIP 核正书名 | 1 子 agent |
| 缺陷分析 | Task 3 implementer，10 个靶子类全链路源码实读 + DDL + MybatisPlusConfig + 22 controller grep 覆盖核查 | 1 子 agent |
| 主 agent pdftotext 抽查 | 12 个 cited 页全核 | 主 agent 内联 |

**单页成本**：约 **3 个子 agent 调用**（无独立 verify/review 子 agent，verify 由主 agent 内联 pdftotext 承担），与 sensing 切片持平，显著低于协议层（协议层单页约 10–12 子 agent / 30–50k token，含独立 review）。Token 量级粗估单页 **20–40k**（含 implementer 上下文）。

**单页成本三次走势**：协议层（多页多主题 4 书源 + 独立 verify 子 agent）≈ 10–12 子 agent → sensing（单页 2 书源 + 主 agent 内联 verify）≈ 3 子 agent → identification（单页 3 书源 + 主 agent 内联 verify）≈ 3 子 agent。**单页单主题切片的成本已稳定在 3 子 agent 量级**，与书源数量弱相关（书源多只增加 pdftotext 抽页次数，不增加子 agent 编排）。

---

## 2. 回答 design 第 8 节复盘标准（逐条）

### Q1：verify 是否拦住幻觉？

**答：拦住；本轮 1 次拦截事件（EPC p49→p50），机制对"页码错位"细粒度幻觉同样有效。**

Task 1 主 agent 对全部 12 条的 12 个 cited 页用 `pdftotext -f <页> -l <页>` 独立回读，发现 EPC 条目原标 PDF p49 实际命中在 p50（§3.1.3 EPC 编码章节在 p50，p49 是上一节末尾），修正后固化。其余 11 个 cited 页首次抽页即逐字命中原句片段（含 Core 书 OCR 异体字规范化比对）。

这与协议层、sensing 层的"零幻觉流入产出"一致。三轮共验证：**命令行 pdftotext 独立核对**这一机制对三类幻觉均有效——协议层拦表格列错位、行号漂移；sensing 层零拦截（cited 页少且清晰）；本切片拦单条页码错位。机制有效，且**对单条页码错位这种最细粒度的幻觉也能命中**，是比前两轮更强的验证。

implementer 的 3 处"诚实保留无书源锚点"（UWB/蓝牙信标/二维码）从源头消除了最常见的幻觉来源（把无关文本强行解读为目标概念），与协议层 3 条 N/A、sensing 层 1 条 N/A 同机制。

### Q2：三分类是否真能区分真缺陷 vs 工程取舍？

**答：能区分；本切片的关键价值是"区分架构层缺陷与 controller 层无 bug"——真缺陷密度低于协议层是诚实且正确的。**

本轮真缺陷 = **1**（D-1：`TenantContextHolder` Javadoc 声称 fail-closed，但全仓库 grep `TenantLineInnerInterceptor`/`TenantLineHandler` 零命中，无任何 MyBatis 拦截器执行该契约）。

**borderline 案例：D-1 归真缺陷而非技术债**。判定依据是 `TenantContextHolder.java:26-31` 的 Javadoc 明示了 fail-closed 契约（"tenantId 为 null 且未 ignore 时查询应被拒绝"），但该契约**实际未被任何组件执行**——契约声明与现实不一致 = 缺陷，而非"待改进"。若 Javadoc 未声明该契约（仅是"理想"），则归技术债；正因为白纸黑字声明了却无组件落地，才坐实为缺陷。判据可复用：**契约声明与执行一致性**是真缺陷/技术债的分水岭。

**S-3（DriverMetadataListener 不带 tenantId）是最有诱惑力被判真缺陷的，经论证降级**：接口与 `MetadataEventDTO` 确实无 tenant 痕迹（对应已知上下文"metadata-listener tenant convention"），但事件源头是 manager（controller 已校验租户后 publish），驱动消费的是 trusted 内部事件路径——把 tenantId 加进 DTO 会冗余。归 S-3 有意简化（trusted-path 设计），非缺陷。判据与协议层 S1/S2（驱动 `throw NotImplemented` 快速失败）、sensing 层 S-1（仅线性标定）同构：**如实记录设计边界，不判错**。

**3 条技术债（T-1/T-2/T-3）同源**：均源于"无框架级隔离兜底"——DO 不实现 TenantOwned、listByIds 不过滤租户、filterTenant 先查全量再内存过滤，都是 D-1 在不同层面的表现（service API 表面"像普通查询"实则依赖调用方过滤）。**真缺陷 D-1 是根因，3 条技术债是其衍生症状**，这一归因结构是本切片的分析亮点。

**结论**：三分类有效。本切片证明的关键点是——**真缺陷密度取决于代码本身的架构特征（是否有框架级不变量兜底），而非切片方法论**。身份/隔离代码在 controller 层覆盖完整（22/22 controller 全调用 `requireTenant`/`filterTenant`），故真缺陷只在"框架级兜底缺失"这一架构层暴露 1 条，远低于协议层（连接管理 4 真缺陷）。

### Q3：知识条目表格式是否可复用？

**答：完全可复用，字段零改动；多书源重叠处理模式第三次验证有效。**

`概念 | 可引用原句 | 书名/作者/出版社·年份/章节/页码 | 对应 docs 锚点 | verify状态` 五字段在协议层定义、sensing 层复用后，本轮**再次直接复用未改一字**。

**多书源场景的处理模式**（本切片 3 本书，其中 Core 与 Multi-domain 都覆盖 RFID/EPC）：
- 文件头声明"书源简称 + 页码约定 + OCR 异体字说明"三段式（Core 书存在系统性 OCR 异体字，文件头列明斱→方等 10 个映射），让 reviewer 用 pdftotext 回读时不会被异体字误导。
- "来源定位"子表声明每本书锚定的章节与 PDF 页，重叠主题以一本为主、另一本补差异化（RFID 全部以 Core 为主，Multi-domain 仅补 EPCglobal 网络层 1 条差异化，避免重复建条目）。

这与协议层 iot-protocols 区块的"书源×章节×页码×相关性"矩阵、sensing 层的"单表（书源≤2）"模式一致。**结论：书源≥3 用"来源定位子表 + 重叠处理说明"，≤2 用单表，跨书源场景字段稳定**。

### Q4：单页成本估算铺开是否可接受？

**答：可接受；单页单主题切片成本已稳定在 3 子 agent 量级。**

单页约 3 子 agent / 20–40k token，与 sensing 层持平，约为协议层单页成本的 1/3。三轮走势证明：**单页单主题 + 主 agent 内联 verify（cited 页≤15 时）= 3 子 agent 是稳态成本**。书源数量（2 本 vs 3 本）对子 agent 数无影响，只影响主 agent pdftotext 抽页次数（本切片 12 页次 vs sensing 3 页次 vs 协议层 12 页次）。

**外推**：若其余 foundations 页单页成本与本轮相当，逐页铺开的子 agent 调用量约为协议层外推值的 1/3。但本切片 PDF 总页数 1057 页（3 本厚书）却只 cited 12 页，说明**书源厚度 ≠ 可用知识密度**——铺开时应优先选"对口章节密集"的书，而非"最厚"的书。仍建议按"书源 → 主题群"批量推进（见第 5 节）。

---

## 3. 与协议层、传感层切片对比

### 3.1 书源驱动策略的有效性（第三次验证，且首次遇 3 书重叠）

协议层复盘提出"书源驱动主题群"策略，sensing 层第二次验证（VOL1/VOL2 重复弃用一本），本切片第三次验证——**策略有效，且首次处理"3 本书中 2 本主题重叠"的场景**。

- **Core（360 页，核心技术教程）vs Multi-domain（372 页，多领域应用）**：两本都覆盖 RFID/EPC，但 Core 偏理论与标准（频段/耦合/防碰撞/EPC 编码结构），Multi-domain 偏应用与数据管理（EPCglobal 网络架构/发现服务）。**重叠处理：以 Core 为主建 7 条，Multi-domain 仅补 1 条差异化（EPCglobal 网络层），避免重复**——这是协议层"同 ISBN vol1/vol2 弃用一本"经验的对偶：协议层是"重复弃用"，本切片是"互补分工"。
- **Beidou（325 页，北斗应用）**：design 第 7 节预警"北斗书偏应用层，理论溯源可能薄"。实证：第 1 章 1.1.2 对 GNSS 原理（测时-测距）、精度（PRN 5–10m、差分厘米级）、室内失效（信号遮挡）有清晰理论陈述，足以支撑 docs 的 3 个 GNSS 锚点。**预警部分成立但未完全应验**——Beidou 不像 sensing 层 VOL1/CPS 那样全书对口，但其第 1 章理论段足够。

**三次验证后可固化**：书源驱动策略有效；遇多书源重叠时按"主书 + 差异化补充"分工（而非简单弃用），分工依据是各书的章节定位（理论 vs 应用 vs 标准）。

### 3.2 目标②靶子性质差异（4 真缺陷 vs 0 vs 1，三次印证）

| 切片 | 靶子性质 | 真缺陷数 | 典型真缺陷 |
|------|---------|---------|-----------|
| 协议层 | 连接管理 / 协议栈 | **4** | MQTT health 桩伪造 online / 资源未关闭 / 错误不退避 |
| 传感层 | 数值标定 / 线性换算 | **0** | （D-2 浮点 round 经实跑验证降级为技术债） |
| 识别层 | 身份唯一性 / 租户隔离边界 | **1** | D-1 TenantContextHolder fail-closed 契约无拦截器执行 |

**三切片真缺陷密度 4 / 0 / 1 的反差，是诚实且正确的**，三次印证"靶子性质决定缺陷密度"：

- **协议层（连接管理，4 真缺陷）**：分布式资源生命周期——连接池、重连退避、健康检查、线程安全，缺陷天然多。
- **传感层（数值标定，0 真缺陷）**：`工程值 = 原始值 × multiple + baseValue` 是简单线性变换，实现质量好（BigDecimal 全程精确、HALF_UP rounding、显式抛异常），缺陷天然少。
- **识别层（身份/隔离，1 真缺陷）**：身份唯一性（deviceId 雪花 + deviceCode UUID + DB 唯一索引）实现稳健；租户隔离在 controller 层覆盖完整（22/22），但**缺一道框架级兜底**——D-1 是"架构层"缺陷，不是 controller 层 bug。controller 层无串台漏洞（rg 扫全 22 个无一遗漏），故真缺陷只暴露 1 条。

**关键判据收敛**：代码缺陷密度取决于**代码本身的复杂度与资源/不变量管理面**：
- 协议层管"分布式资源生命周期"，管理面最大，缺陷最多；
- 识别层管"身份不变量 + 隔离边界"，controller 层手过滤覆盖完整，但缺框架兜底，缺陷居中（1 条架构层）；
- 传感层管"纯计算"，无资源/边界管理面，缺陷最少（0）。

**方法论的作用是如实暴露，而非保证每个切片都有料**——这一结论在 sensing 层（0 真缺陷合法）首次提出，本切片再次印证：1 真缺陷也是合法结论，不硬凑也不漏看。

---

## 4. 关键发现：真缺陷的"架构层 vs 实现层"归因

本切片最重要的发现是 D-1 的归因结构——**真缺陷在架构层（无框架兜底），3 条技术债是其衍生症状（实现层）**。

D-1（`MybatisPlusConfig` 无 `TenantLineInnerInterceptor`）是根因：隔离纯靠 controller+service 手过滤，无框架兜底。由此衍生：
- T-1（DO 不实现 TenantOwned）：类型表达力弱，隔离契约只在 BO 层；
- T-2（listByIds 无租户参数）：service API 表面"像普通查询"实则依赖调用方过滤；
- T-3（filterTenant 先查全量再内存过滤）：批量端点"先拉跨租户数据再丢弃"的侧信道。

这一"1 根因 + 3 衍生"结构说明：**租户隔离的薄弱点是单一架构决策（不做 SQL 改写），而非散落的多处 bug**。修复路径也清晰——补 `TenantLineInnerInterceptor` 即可同时消除 D-1 和缓解 T-2/T-3（T-1 是类型表达力，独立）。

相比之下，协议层 4 真缺陷散落在 4 个驱动（MQTT/OPC-UA/modbus/plcs7），无单一根因；sensing 层 0 真缺陷无根因可溯。**本切片的归因结构是最干净的**——这得益于"身份/隔离"靶子的内聚性（隔离机制集中在 MybatisPlusConfig + BaseController + TenantContextHolder 三处）。

---

## 5. 是否继续铺开 + 下一批建议

### 5.1 结论：继续铺开，按"书源 → 主题群"批量推进

四个复盘标准的答案：verify 零幻觉（1 次拦截有效）、三分类可区分且真缺陷密度与靶子性质匹配、条目表可复用、单页成本稳定在 3 子 agent。**方法论第三次验证通过，可铺开**。

### 5.2 下一批主题建议（结合 foundations 实际页面状态）

当前 `docs/zh/foundations/` 共 9 个页面（不含 index.md）：

| 页面 | 学术溯源状态 | 下一批建议 |
|------|------------|-----------|
| `sensing.md` | ✅ 已做（sensing 切片） | — |
| `identification.md` | ✅ 已做（本切片） | — |
| `fieldbus.md` | ✅ 已做（协议层切片） | — |
| `iot-protocols.md` | ✅ 已做（协议层切片） | — |
| `index.md` | 导航页，无需溯源 | — |
| `aiot.md` | ❌ 未做 | **优先级中**（AIoT = AI+IoT，需另配 AI/边缘智能书源，当前 iot-books 可能无对口） |
| `edge-cloud.md` | ❌ 未做 | **优先级中**（边缘计算/云边协同，需另配边缘计算书源） |
| `data-pipeline.md` | ❌ 未做 | **优先级高**（数据管道/时序数据库/流处理，可与 DC3 的 point-value 数据链路结合做目标②，书源需另配） |
| `security.md` | ❌ 未做 | **优先级高，但目标②需谨慎**（安全/认证/加密，DC3 的鉴权被 design 第 9 节 YAGNI 排除在本切片外；若做 security 页，目标②会触及鉴权代码，需先确认范围） |

**下一批建议（按书源匹配度 + 目标②清晰度排序）**：

1. **`data-pipeline.md`**（优先级最高）：docs 目标①有明确理论锚点（时序数据/流处理/批处理/数据质量）；目标②可落到 DC3 的 point-value 数据链路（`PointValueService`/`PointValueMapper`/时序存储），靶子性质偏"数据完整性与时序性"，与本切片"身份/隔离"性质不同但同样清晰。
2. **`edge-cloud.md`**（优先级次高）：目标①理论锚点（边缘计算架构/云边协同/卸载）；目标②可落到 DC3 的 driver/center 部署边界（driver 在边缘、center 在云），靶子偏"部署拓扑与通信边界"。
3. **`aiot.md`** / **`security.md`**（需先盘点书源）：aiot 需 AI/边缘智能专著；security 目标②会触及鉴权（design YAGNI 排除），建议先确认是否扩范围。

**暂缓或需另配书源**：
- 当前 iot-books 已用的 3 本识别/定位书、2 本传感书、5 本协议书**已无剩余主题可延伸**——下一批必须另配书源（数据管道/边缘计算/AIoT/安全各需对口专著）。

### 5.3 流程固化（三轮验证后可入规范）

| 环节 | 固化项 | 验证次数 |
|------|--------|---------|
| verify | 一律走 `pdftotext -layout` 命令行，cited 页全核（单页切片可主 agent 内联，多页切片起独立 verify 子 agent） | 3 轮 |
| 三分类 | 真 0 / 真 1 都是合法结论；精度问题必须实跑验证再判级（sensing D-2 范式）；契约声明与执行一致性是真缺陷/技术债的分水岭（本切片 D-1 范式） | 3 轮 |
| 条目表 | 五字段不变；书源≤2 用单表，≥3 用"来源定位子表 + 重叠处理说明" | 3 轮 |
| 多书源重叠 | 按"主书 + 差异化补充"分工（而非简单弃用），依据是各书章节定位（理论 vs 应用 vs 标准）；同 ISBN vol1/vol2 先逐字比对重复即弃一本 | 协议层弃用 + 本切片分工，2 种模式 |
| 引用 | GB/T 7714 参考文献 + 括号 inline（沿用协议层统一规范）；书名/著者/年份据源书 CIP/版权页核正（本切片 3 本书均经 CIP 核正） | 3 轮 |
| OCR 异体字 | 若 PDF 存在系统性 OCR 异体字（如 Core 书斱→方），条目表文件头列明映射表，reviewer 回读时按映射比对 | 本切片新增 |

---

## 6. en/ 同步点清单（本轮不做，后续同步）

`docs/zh/foundations/identification.md` 注入的 11 处 inline 引用 + 1 个参考文献小节（3 条），对应 `docs/en/foundations/identification.md` 后续同步：

| zh 行 | 位置 | 引用内容 | 书源页码 |
|-------|------|---------|---------|
| 31 | 关键技术与权衡 · "按频段分三档" 句首 | RFID 频段划分 LF/HF/UHF/SHF | Core 第 4 章 4.1.1，PDF p67 |
| 32 | 关键技术与权衡 · LF 句尾 | LF RFID 特性/读距/动物芯片 | Core 第 4 章 4.1.4，PDF p70–71 |
| 33 | 关键技术与权衡 · HF 句尾 | HF 13.56 MHz / NFC 基础 / ISO 标准 | Core 第 4 章 4.1.4，PDF p71 |
| 34 | 关键技术与权衡 · UHF 句尾 | UHF 860–960 MHz / EPC Gen2 | Core 第 4 章 4.1.4，PDF p72 |
| 36 | 关键技术与权衡 · 有源标签句尾 | 无源/有源/半无源 | Core 第 2 章 2.2，PDF p34 |
| 37 | 关键技术与权衡 · "读写器供能并收发" 句尾 | 读写器-标签耦合/读距 | Core 第 4 章 4.1.4，PDF p70–71 |
| 43 | 关键技术与权衡 · 北斗 BDS 后括号 | 北斗系统精度 10m/地基增强厘米级 | Beidou 前言，PDF p10–11 |
| 43 | 关键技术与权衡 · GNSS 精度句中 | GNSS 原理与精度 5–10m/差分厘米级 | Beidou 第 1 章 1.1.2，PDF p22 |
| 43 | 关键技术与权衡 · "室内基本失效" 句尾 | GNSS 室内失效/信号遮挡 | Beidou 第 1 章 1.1.2，PDF p23 |
| 85 | 工程要点 · 频段即合规 · 中国括号注 | 中国 920–925 频段规划（同 UHF 条目） | Core 第 4 章 4.1.4，PDF p72 |
| 89 | 工程要点 · EPC 段（单品唯一身份） | EPC 编码体系（厂商+分类+序列号） | Core 第 3 章 3.1.3，PDF p50 |
| 90 | 工程要点 · EPC 思想是标识缩影句尾 | EPCglobal 网络/发现服务 | Multi-domain 第 9 章，PDF p159、p175 |
| 96 | 工程要点 · 读不到≠存在 容器内 | RFID 防碰撞 ALOHA/树搜索 | Core 第 10 章 10.1.2，PDF p213 |
| 129–133 | `## 参考文献` 小节 | 3 条 GB/T 7714（Core / Multi-domain / Beidou） | — |

> **en/ 同步注意**：
> - en/ 锚点 slug 与 zh/ 不同（如 `#关键技术与权衡` vs `#key-technologies-and-trade-offs`），条目表"对应 docs 锚点"字段需同步更新。
> - UWB/蓝牙信标/二维码 3 个无书源锚点在 en/ 同样保留原文不加引用（与 zh/ 一致）。
> - en/ 参考文献按 GB/T 7714 中文著录转译时，译者/丛书的英文表达需对照源书 CIP（本切片 3 本书均据 CIP/版权页核正过书名与著者，en/ 同步可直接复用 zh/ 的著录）。

---

## 7. 一句话总结

识别与定位层切片（1 页 / 12 条目 / 1-3-3 缺陷）方法论第三次验证通过——verify 拦截 1 次页码错位（机制对细粒度幻觉有效）、三分类区分架构层缺陷（D-1）与 controller 层无 bug（真缺陷密度 1 居于协议层 4 与传感层 0 之间，与身份/隔离靶子性质吻合）、单页成本稳定在 3 子 agent；**继续铺开，按"书源→主题群"批量推进**，下一批优先 `data-pipeline.md`（目标②清晰）与 `edge-cloud.md`，当前 iot-books 已无剩余主题延伸、需另配书源。
