# 自动识别与定位垂直切片 — Design Doc

- **日期**：2026-07-01
- **状态**：brainstorm 已确认，待 plan 执行
- **位置**：`iot-dc3/docs/superpowers/specs/`
- **来源**：复用协议层 + 传感层切片方法论（`protocol-slice-retrospective.md` + `sensing-slice-retrospective.md`
  验证通过）；第三个垂直切片

## 1. 背景与目标

第三个垂直切片（继协议层、传感层）。两大目标同前两切片：①docs 学术溯源 ②代码缺陷分析。**全部复用已验证方法论决策**
（verify/三分类/引用格式/文件位置），不重走 brainstorm。

## 2. 切片范围（S1）

### docs（目标①）

- `docs/zh/foundations/identification.md`（条码/二维码 · RFID LF/HF/UHF · NFC · GNSS/北斗 · UWB · 蓝牙信标；落地
  `deviceId` + `tenantId`）

### 代码（目标②）— 设备身份与租户隔离

- **Device 身份**：`deviceId` 生成、唯一性约束、生命周期（`dc3-center` 设备域）
- **租户隔离**：`tenantId` 拦截器、数据是否真能跨租户串台（metadata-listener 等监听链路）
- （精确靶子类在 plan 阶段定位）

### 书源（3 本识别/定位专著）

- `iot-books/sensing-identification/iot-rfid-core-technology-tutorial.pdf`
- `iot-books/sensing-identification/iot-rfid-multi-domain-solutions.pdf`
- `iot-books/sensing-identification/iot-beidou-applications.pdf`

## 3. 现状关键发现

1. **identification.md 质量已高**：识别/定位分类、距离—成本取舍、频段合规、EPC、mermaid、`deviceId`/`tenantId` 落地——*
   *和前两切片一样缺学术溯源**，目标①工作量轻。
2. **身份与隔离代码有实靶子**：`deviceId` 唯一性 + 租户隔离是已知坑区（metadata-listener tenant convention），目标②有实打实靶子。
3. ⚠️ **目标②靶子性质不同于传感层**：传感层是数值标定的线性换算（精度/rounding），本切片是**身份唯一性与隔离边界正确性**
   ——缺陷类型偏"边界/隔离"而非"计算精度"，不套用传感层缺陷模板。诚实评估。

## 4. 工作流（复用 4 步）

```
①读识别/定位书建条目 → ②注入 identification.md（verify）→ ③身份与隔离缺陷分析 → ④复盘
```

## 5. 决策（全部复用前两切片）

- **verify**：独立子 agent + 主 agent pdftotext 抽 cited 页核对，零幻觉铁律；伪造即删。
- **三分类**：真缺陷/有意简化/技术债。
- **引用**：括号 inline + GB/T 7714 参考文献，禁 `[^1]` 脚注。
- **文件位置**：`iot-dc3/docs/superpowers/`（specs/plans/analysis），config.mts srcExclude 不发布。

## 6. 产出

1. `identification.md` diff（注入溯源引用，zh/）
2. `analysis/identification-knowledge-entries.md`（**新建**）
3. `analysis/identification-defects.md`（身份与隔离缺陷三分类）
4. `analysis/identification-slice-retrospective.md`（方法论复盘）

## 7. 风险

| 风险                 | 对策                                                |
|--------------------|---------------------------------------------------|
| 识别层"代码"边界模糊（无独立模块） | 严格限定 `deviceId` + `tenantId` 落点，不扩散到鉴权            |
| RFID 两本书可能内容重叠     | 以 core-technology-tutorial 为主，multi-domain 补差异化案例 |
| 北斗书偏应用层，理论溯源可能薄    | 诚实标 N/A，不硬凑                                       |
| 租户隔离缺陷若跨多模块，分析易发散  | plan 阶段先定位靶子类，控制范围                                |

## 8. 复盘标准（复用前两切片）

verify 拦幻觉、三分类区分真缺陷 vs 工程取舍、条目表可复用、单页成本。

## 9. YAGNI

- `en/` 注入（只 zh/）
- 源码改动（只产出缺陷清单）
- 鉴权/身份认证（`identity`/`principal`/`service-account`）不在本切片

## 10. 与前两切片的关系

复用协议层 + 传感层方法论（`protocol-slice-retrospective.md` + `sensing-slice-retrospective.md` 验证通过）。本切片是"
书源驱动主题群批量推进"策略（复盘建议）的第三个主题。
