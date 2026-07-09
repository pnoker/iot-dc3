# Sensing 切片 Task 3：标定换算链路代码缺陷分析

> 目标②。审查 IoT DC3 平台**位号标定换算**链路（`工程值 = 原始值 × multiple + baseValue`，按 `valueDecimal`
> 保留小数），按协议层同款三分类挑缺陷。结论诚实——标定换算是简单线性变换，缺陷密度显著低于协议层。

## 元信息

| 字段     | 内容                                                                                                                                                                     |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 切片     | sensing（位号标定换算落点）                                                                                                                                                      |
| 审查范围   | `dc3-common-driver` 的标定核心（`TypedValueConverter` / `PointValue` / `ReadPointValue` / `CalculatedPointValue`）、`PointBO` 标定参数、`ArithmeticUtil` 精度工具、gRPC/facade 边界的标定参数搬运 |
| 换算实际位置 | `TypedValueConverter#calculatePointValue`（`dc3-common/dc3-common-driver/.../support/TypedValueConverter.java:94`）→ `linearValue`（同文件 `:160`）                           |
| 三分类计数  | 真缺陷 **0** ／ 有意简化 **2** ／ 技术债 **3**                                                                                                                                     |
| 总体结论   | **标定换算实现简洁稳健，未发现真缺陷**。线性变换全程走 `BigDecimal` 精确算术；null 标定参数有 `Optional.orElse` 默认值兜底；浮点 round 经 JDK shortest-round-trip `toString` 缓解了经典二进制误差。仅发现边界处一处静默截断（技术债）与若干工程取舍   |
| 学术对照   | 条目 4.1（标定定义／量值传递）、4.2（精度／分辨力／漂移指标），见 `sensing-knowledge-entries.md`（verify 状态 `待核`）                                                                                    |

## 换算链路实读摘要（核对用）

- 入口：`ReadPointValue#calculate()`（`ReadPointValue.java:73`）→ `TypedValueConverter.calculatePointValue(value, point)`。
- 参数解析：`PointBO.baseValue/multiple/valueDecimal` 三字段（`PointBO.java:71/76/81`），均为包装类型可空；
  `calculatePointValue` 用 `Optional.orElse(DEFAULT_*)` 兜底（`TypedValueConverter.java:101-103`），默认
  `base=0 / multiple=1 / decimal=6`。
- 线性算术：`linearValue`（`TypedValueConverter.java:160-173`）——`BigDecimal` 全程精确，按"恒等 / 仅乘 / 仅加 / 乘加"
  四分支短路（避免无谓运算），最终 `multiple.multiply(value).add(base)`。
- 类型落盘：BYTE/SHORT/INT/LONG 走 `exactXxx`（范围检查 + 整数性检查 + `xxxValueExact`）；FLOAT/DOUBLE 走
  `roundedFloat/roundedDouble` → `ArithmeticUtil.round`；STRING/BOOLEAN 不标定。
- 精度工具：`ArithmeticUtil.round`（`ArithmeticUtil.java:203/216/230`）用 `divide(BigDecimal.ONE, scale, HALF_UP)` 实现
  round（HALF_UP），float/double 重载有 `0~7 / 0~16` 的 scale 上界校验。

## 三分类缺陷表

### 真缺陷（0 条）

无。诚实评估：标定换算全程 `BigDecimal`、null 参数已兜底、HALF_UP rounding 无系统性 DOWN 截断偏差、类型转换显式抛
`TypeException/OutRangeException` 不静默吞异常。未发现违反最佳实践且有改进空间的真缺陷。

### 有意简化（2 条）

| 编号  | 文件:行                               | 主题         | 对照(书/标准)                             | 描述                                                                                                        | 建议                                     |
|-----|------------------------------------|------------|--------------------------------------|-----------------------------------------------------------------------------------------------------------|----------------------------------------|
| S-1 | `TypedValueConverter.java:160-173` | 仅支持线性标定    | 条目 4.1：标定是"明确输出-输入关系"——关系形式不限，线性只是其一 | 平台只实现 `y = multiple·x + base` 单一线性模型，不支持非线性多项式、查表插值、分段标定（如热电偶/RTD 的分度表）。属合理工程取舍：大部分工业位号线性足够，非线性可由驱动侧或下游处理 | 记录不判错；若未来覆盖温度/流量类传感器，再考虑引入可插拔标定函数      |
| S-2 | `TypedValueConverter.java:50,103`  | 默认小数位硬编码 6 | 条目 4.2：分辨力是"检测出的最小变化量"               | `DEFAULT_DECIMAL = 6` 对所有未配置位号生效。这是合理默认（覆盖多数浮点位号），但 6 位并非来自任何精度规范，对超高精度或超低位号都不一定贴切                        | 记录不判错；文档中提示用户对精度敏感位号显式配 `valueDecimal` |

### 技术债（3 条）

| 编号  | 文件:行                                                               | 主题                                                     | 对照(书/标准)                      | 描述                                                                                                                                                                                                                                                                                                                                                    | 建议                                                                                                                                    |
|-----|--------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| D-1 | `FacadeGrpcPointBuilder.java:102`                                  | valueDecimal 静默 byte 截断                                | 条目 4.2（精度/分辨力）：参数失真直接污染标定结果   | gRPC DTO 的 `valueDecimal` 为 `int`，此处 `(byte) dto.getValueDecimal()` 直接窄化转型，超出 `[-128,127]` 会**静默环绕**（如 200 → -56），不抛异常、不告警。`baseValue/multiple` 同文件走 `BigDecimal.valueOf(double)`（`:100-101`）也有浮点→精确的隐式精度损失，但相对可控                                                                                                                                     | 加 `if (valueDecimal < -128                                                                                                            || valueDecimal > 127) 抛/告警`；或 DTO 改 `int32` 后在边界显式校验 |
| D-2 | `TypedValueConverter.java:232-238` → `ArithmeticUtil.java:216/230` | 浮点 round 经 `BigDecimal→primitive→String→BigDecimal` 绕行 | 条目 4.2（精度）：理想是全程 `BigDecimal` | `roundedFloat/roundedDouble` 先 `bd.floatValue()/doubleValue()`（精确→近似 widening），再丢给 `ArithmeticUtil.round(float/double, scale)`，后者 `Float/Double.toString` 再回 `BigDecimal`。**实测**：因 JDK `toString` 的 shortest-round-trip 特性，常见值（含经典 `2.675`、17 位有效数字）经此绕行后结果与直接 `bd.setScale(scale, HALF_UP)` **一致**，故实际偏差极小、非真缺陷；但代码意图（精确标定）与实现路径（绕道近似类型）不一致，属可读性/隐患债 | 标定核心的 round 直接调 `bd.setScale(scale, HALF_UP)`（或 `ArithmeticUtil` 增 `round(BigDecimal, int)` 重载），跳过 primitive 绕行；动机是"代码表达精确意图"而非"当前算错" |
| D-3 | `ArithmeticUtil.java:203-207`                                      | `divide(BigDecimal.ONE, scale, HALF_UP)` 实现 round 的写法  | —                             | 用 `x.divide(ONE, scale, HALF_UP)` 达到 round 效果，语义正确但比 `x.setScale(scale, HALF_UP)` 更绕（`setScale` 才是 JDK 为 round 提供的原语）。纯写法债，不影响正确性                                                                                                                                                                                                                     | 统一改为 `setScale`；顺手把 `new BigDecimal("1")` 这类魔数提为常量                                                                                    |

## 学术对照（关键条目回链）

- **条目 4.1（标定定义／量值传递）** — 出处：CPS／李同滨等／机械工业出版社·2018／第6章 6.4.4／PDF p308。原文："
  传感器的标定是指在明确传感器的输出和输入关系的前提下，利用标准器具对传感器进行标定。" 用于 **S-1**（线性只是输出-输入关系的一种形式）。
- **条目 4.2（精度／分辨力／漂移）** — 出处：VOL1／范茂军主编／机械工业出版社·2012／第5章 5.1.2／印刷 p118–p119（PDF
  p125–p126）。原文："分辨力是指传感器在规定测量范围内可能检测出被测量的最小变化量"；"
  漂移是指在一定的时间间隔内，传感器输出中与被测量无关的、不希望的变化量。" 用于 **S-2 / D-1 / D-2**（精度参数失真与表达）。
- 两条均 verify 状态 `待核`（见 `sensing-knowledge-entries.md`），引用前应跟随其 verify 结论。

## 自检

- 表中每条 `文件:行` 均经 Read 实读：`TypedValueConverter.java`（全 347 行）、`PointBO.java`（全 118 行）、`PointValue.java`（全
  164 行）、`ReadPointValue.java`（全 97 行）、`CalculatedPointValue.java`（全 54 行）、`ArithmeticUtil.java`（全 237 行）、
  `FacadeGrpcPointBuilder.java:85-119`、`ArithmeticUtilTest.java`、`ReadPointValueTest.java`。
- "真缺陷 0" 经实证而非臆断：写 `/tmp/RoundTest*.java` 实跑 `BigDecimal→double→String→BigDecimal` 与直接 `setScale` 对比，覆盖
  17 位有效数字、`2.675` tie、float 7 位精度边界等典型坑，均未观察到 round 结果发散——故 D-2 降级为技术债而非真缺陷。
- 未硬凑真缺陷：标定的简单线性设计如实归入【有意简化】；绕行 round、`divide(ONE)` 写法如实归入【技术债】。
- 学术条目仅引用 `sensing-knowledge-entries.md` 中已存在的 4.1/4.2，未杜撰新出处；标注其 `待核` 状态。
