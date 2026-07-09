# 传感与测量垂直切片 — Design Doc

- **日期**：2026-07-01
- **状态**：brainstorm 已确认，待 plan 执行
- **位置**：`iot-dc3/docs/superpowers/specs/`
- **来源**：复用协议层切片方法论（`2026-06-30-protocol-layer-slice-design.md` 验证通过）+ TODO.md #8

## 1. 背景与目标

第二个垂直切片（继协议层）。两大目标同协议层：①docs 学术溯源 ②代码缺陷分析。**全部复用协议层方法论决策**
（verify/三分类/引用格式/文件位置），不重走 brainstorm。

## 2. 切片范围（S1）

### docs（目标①）

- `docs/zh/foundations/sensing.md`

### 代码（目标②）— 标定换算链路

- `PointBO`（`baseValue`/`multiple`/`valueDecimal` 标定参数）
- `PointValue`/`PointValueBO`（`rawValue`/`calValue`）
- `ArithmeticUtil`（BigDecimal 精度/rounding）
- `TypedValueConverter`（类型转换）

### 书源（3 本传感器专著，比协议层通论书对口）

- `iot-books/sensing-identification/iot-sensor-technology-vol1.pdf`
- `iot-books/sensing-identification/iot-sensor-technology-vol2.pdf`
- `iot-books/sensing-identification/iot-source-cyber-physical-sensing-basics.pdf`

## 3. 现状关键发现

1. **sensing.md 质量已高**：传感器分类（电阻/电容/压电/热电/半导体）、信号链 mermaid、A/D 双维度（采样率/量化+奈奎斯特/混叠）、6
   指标表、精度≠分辨率、标定、MEMS——**和协议层一样缺学术溯源**，目标①工作量轻。
2. **标定换算代码集中**（PointBO/PointValue/ArithmeticUtil/TypedValueConverter），目标②有实打实靶子。
3. ⚠️ **目标②可能比协议层薄**——标定换算（`工程值=原始值×multiple+baseValue`）是简单线性变换，缺陷密度可能不如协议层（OPC-UA
   连接风暴那种）。诚实评估，不硬凑。

## 4. 工作流（复用协议层 4 步）

```
①读传感器书建条目 → ②注入 sensing.md（verify）→ ③标定换算缺陷分析 → ④复盘
```

## 5. 决策（全部复用协议层）

- **verify**：独立子 agent + 主 agent pdftotext 抽 cited 页核对，零幻觉铁律；伪造即删。
- **三分类**：真缺陷/有意简化/技术债。
- **引用**：括号 inline + GB/T 7714 参考文献，禁 `[^1]` 脚注。
- **文件位置**：`iot-dc3/docs/superpowers/`（specs/plans/analysis），config.mts srcExclude 不发布。

## 6. 产出

1. `sensing.md` diff（注入溯源引用，zh/）
2. `analysis/sensing-knowledge-entries.md`（**新建**，主题独立）
3. `analysis/sensing-defects.md`（标定换算缺陷三分类）
4. `analysis/sensing-slice-retrospective.md`（方法论复盘）

## 7. 风险

| 风险                                | 对策                  |
|-----------------------------------|---------------------|
| 目标②薄（线性换算简单）                      | 诚实评估，不硬凑；若无真缺陷，如实记录 |
| 传感器书 vol1/vol2 重叠（如协议层 vol1/vol2） | 取一份，避免重复            |
| 书源覆盖度（某主题无内容）                     | 诚实标 N/A             |

## 8. 复盘标准（复用协议层）

verify 拦幻觉、三分类区分真缺陷 vs 工程取舍、条目表可复用、单页成本。

## 9. YAGNI

- `identification.md`（RFID/北斗）不在本切片
- `en/` 注入（只 zh/）
- 源码改动（只产出缺陷清单）

## 10. 与协议层切片的关系

复用协议层方法论（`protocol-slice-retrospective.md` 验证通过）。本切片是"书源驱动主题群批量推进"策略（复盘建议）的第二个主题。
