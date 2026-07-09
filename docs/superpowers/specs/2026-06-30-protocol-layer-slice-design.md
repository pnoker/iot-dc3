# T2 协议层垂直切片 — Design Doc

- **日期**：2026-06-30
- **状态**：brainstorm 已确认；已对齐既有 superpowers 产物；plan 见 `../plans/2026-06-30-protocol-layer-slice.md`
- **位置**：`iot-dc3/docs/superpowers/specs/`（对齐 `documentation/index.md` 文档宪法）
- **来源**：`github/文档优化.md`（文档学术化 prompt）+ `github/TODO.md` 第 8 条

---

## 1. 背景与两大目标

|      | 目标① docs 学术化富化          | 目标② 书籍对照代码挑缺陷       |
|------|-------------------------|---------------------|
| 工作对象 | `iot-dc3/docs/zh/` 92 页 | `iot-dc3/` 源码       |
| 对照   | 页面叙事 ↔ 书籍概念             | 代码实现 ↔ 书籍最佳实践       |
| 产出   | 注入溯源引用 + 参考文献           | 缺陷清单 + 改进路线图        |
| 头号风险 | **引用幻觉**（编造页码/原句）       | **书生气误判**（把工程取舍当缺陷） |

采用**垂直切片**：选最窄主题端到端走完整条链路，先用小样本把方法论磨出来，再横向铺开。

## 2. 为什么选 T2 协议层

docs（iot-protocols+fieldbus+drivers）+ 代码（28 驱动）+ 书（communication-networking 7 本）三者最丰富，目标②有 docs 自标注的骨架靶子，是
DC3 招牌。

## 3. 切片范围（已按"复用既有产物"调整）

### docs（目标①）

- `docs/zh/foundations/iot-protocols.md`
- `docs/zh/foundations/fieldbus.md`
- `docs/zh/drivers/index.md`

### 代码（目标②）—— 复用 + 增量，不重做

- **底座（复用）**：`superpowers/analysis/iot-dc3-vs-iot-communication.md` 已覆盖 S7 深度、Modbus
  模式边界、协议引擎与框架强绑定、字节缓冲工具缺失、注解序列化仅限 S7、驱动测试薄弱。
- **增量①**：骨架驱动当前完成度核查（mqtt/lwm2m/iec104/dlms/ethernet-ip/can）——既有报告未细做。
- **增量②**：用学术书籍给既有缺陷补**理论依据**（既有报告是和竞品对比，此处补学术对照）。

### 参考书（按页面对号）

| docs 页             | 核心参考书                                                                                                                                                            |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fieldbus.md`      | `communication-networking/iot-soul-protocols-and-operating-systems-vol1.pdf`                                                                                     |
| `iot-protocols.md` | `communication-networking/iot-soul-protocols-and-operating-systems-vol2.pdf`、`nb-iot-technology-analysis-and-cases.pdf`、`5g-iot-and-nb-iot-technology-guide.pdf` |
| `drivers/index.md` | `communication-networking/iot-four-network-convergence-research.pdf`                                                                                             |

## 4. 现状关键发现

1. **docs 质量已高，缺学术溯源**——iot-protocols/fieldbus 已含 QoS、CoAP、LwM2M、字节序等专业内容，但全文无外部书目引用。目标①在此切片工作量较轻。
2. **docs 已过 2026-06-23 事实核查**（`specs/2026-06-23-docs-review-findings.md`，35 页全审）——但那是核查"对错"，不是"
   学术溯源"，目标①仍是空白。该核查已发现本切片邻近页错误（modules.md 漏 http 驱动、driver-authoring.md 用已存在的 bacnet-ip
   做示例），注入时顺手核对是否已修。
3. **目标②大半已有现成产物**——`analysis/iot-dc3-vs-iot-communication.md`（2026-05-26）已做协议层深度对比 + 缺陷清单。切片复用、只做增量。
4. **代码骨架问题 docs 已诚实自标注**——MQTT/LwM2M 骨架、IEC104/DLMS 显式 fail-fast、EtherNet/IP CIP 组帧未补、CAN 走
   can-utils。
5. **文档宪法约束**——`documentation/index.md` 规定文件分层、事实来源、写作规则（禁"生产级闭环"等措辞）、验证命令（
   `pnpm run build`）。

## 5. 工作流（4 步，verify 对齐既有范式）

```
①读 PDF 建知识条目 → ②注入 docs（带 verify）→ ③复用+增量缺陷分析 → ④产出 + 复盘
```

- **verify 对齐** `review-findings` 的「带 `文件:行` 证据 + 源码/原文核对」范式，不另造。
- verify 子 agent 与注入子 agent **分属不同上下文**，防自我背书；全量核对；伪造即删。

## 6. 决策（三项已确认 + 位置修正）

- **6.1 verify**：独立子 agent 回读 PDF 对应页核对原句/页码；inline 括号引用 + 页末 `## 参考文献` 编号列表（VitePress
  无脚注插件，**不用 `[^1]`**）；全量 verify；伪造即删。
- **6.2 en/**：切片只做 zh/；登记 en/ 同步点列入后续，不本轮做；书名/作者保留原文。
- **6.3 目标②三分类**：【真缺陷】/【有意简化】/【技术债】；内部路线图，措辞直白。
- **6.4 文件位置**：design→`specs/`、plan→`plans/`、知识条目/缺陷/复盘→`analysis/`（全部在 `iot-dc3/docs/superpowers/`
  ，对齐文档宪法）。

## 7. 产出物

1. **3 个 docs 页 diff**（注入溯源引用，zh/，commit 到 iot-dc3）
2. **知识条目表** → `analysis/protocol-knowledge-entries.md`
3. **缺陷清单**（以既有 analysis 为底座 + 增量，三分类）→ `analysis/protocol-defects.md`
4. **方法论复盘** → `analysis/protocol-slice-retrospective.md`

## 8. 风险与对策

| 风险            | 对策                     |
|---------------|------------------------|
| 引用幻觉          | 全量 verify 子 agent 回读原页 |
| 书生气误判         | 三分类 + 每条对照代码实际行号       |
| 盲信历史 analysis | 既有结论逐条核验当前代码有效性，不照搬    |
| 切片膨胀          | 锁死 3 页 + 协议层 + 只做 zh/  |

## 9. 方法论复盘标准

- verify 是否拦住幻觉？漏过几条？
- 三分类是否真能区分真缺陷 vs 工程取舍？
- 知识条目表格式是否够通用、可复用？
- 单页成本估算铺到 92 页是否可接受？

## 10. 不在本切片做（YAGNI）

- `en/` 注入、其余 89 页、其他主题、新增页面、任何源码改动。

## 11. 与既有 superpowers 产物的关系

- **复用**：`analysis/iot-dc3-vs-iot-communication.md`（目标②底座）、`specs/2026-06-23-docs-review-findings.md`（verify
  范式 + 已知错误清单）。
- **遵守**：`documentation/index.md`（文档宪法：分层、事实来源、措辞、验证）。
