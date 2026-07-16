---
title: 位号 Point
---

<script setup>
import PointRelationDiagram from '../../../.vitepress/theme/components/PointRelationDiagram.vue'
</script>

# 位号 Point

> **位号是一个数据项**——一类设备身上要采集或要写入的**一个具体的量**。定义归属[模板](./profile)
> ，运行态取值即[位号值](./point-value)。

位号回答的是"这类设备身上有哪些量可以读、可以写"。一台空调上的"室温""设定温度""开关状态"
，各自就是一个位号；它们的瞬时数值快照是[位号值](./point-value)。可以这样类比：[模板](./profile)是一张表格的表头定义，每个位号就是其中的一
**列**，而[位号值](./point-value)是某台设备某时刻填进这一列的那个**单元格**。

容易混淆的两点：

- **位号 ≠ 位号值**。位号是"列"的定义（叫什么、什么类型、能不能写、单位是什么），稳定不变；位号值是"格"
  的取值，随采集不断变化。详见[位号值](./point-value)。
- **位号 ≠ 指令**。位号是"量"，[指令](./command)是"动作"（重启、校准、切换模式）。一个位号能不能被写，由它自己的 `rwFlag` 决定，*
  *不需要、也不会**在 `dc3_command` 指令表里登记。读写位号走的是 `PointCommand` 链路，自定义指令才走 `Command` 链路。

## 关键字段

位号 `PointBO`（表 `dc3_point`）：

| 字段              | 类型             | 含义                          |
|-----------------|----------------|-----------------------------|
| `pointName`     | String         | 位号名称（展示用，如"室温"）             |
| `pointCode`     | String         | 位号标识符，同一[模板](./profile)下唯一 |
| `pointTypeFlag` | PointTypeEnum  | 数据类型，见下                     |
| `rwFlag`        | RwTypeEnum     | 读写能力，见下                     |
| `unit`          | String         | 工程单位，如 `℃`、`kPa`            |
| `baseValue`     | BigDecimal     | 线性换算的偏移量（默认 `0`）            |
| `multiple`      | BigDecimal     | 线性换算的倍率（默认 `1`）             |
| `valueDecimal`  | Byte           | 小数精度，浮点取值的保留位数（默认 `6`）      |
| `profileId`     | Long           | 归属的[模板](./profile)         |
| `pointExt`      | PointExt       | 扩展配置（协议映射、约束、采集策略等）         |
| `enableFlag`    | EnableFlagEnum | 启停状态                        |
| `tenantId`      | Long           | 归属[租户](./tenant)            |

## 数据类型 `pointTypeFlag`

| 枚举                                | code                              | 说明      |
|-----------------------------------|-----------------------------------|---------|
| `STRING`                          | `string`                          | 字符串（默认） |
| `BYTE` / `SHORT` / `INT` / `LONG` | `byte` / `short` / `int` / `long` | 整数      |
| `FLOAT` / `DOUBLE`                | `float` / `double`                | 浮点      |
| `BOOLEAN`                         | `boolean`                         | 布尔      |

## 读写能力 `rwFlag`

| 枚举           | code | 说明                 |
|--------------|------|--------------------|
| `READ_ONLY`  | `r`  | 只读，只能采集，不能下发写值（默认） |
| `WRITE_ONLY` | `w`  | 只写，只能下发写值          |
| `READ_WRITE` | `rw` | 可读可写               |

::: warning 能否写由 rwFlag 决定，不是指令表
某个位号能不能被写，**唯一**取决于它的 `rwFlag` 是否包含写能力（`WRITE_ONLY` 或 `READ_WRITE`）。这跟[指令](./command)（
`dc3_command`）没有关系——位号读写不在指令表里建模。中心侧在写命令前会校验 `rwFlag`：只读位号收到写请求会被直接拒绝。
:::

## 原始值与工程值换算

驱动从设备采到的常常是**原始值**（寄存器整数、ADC 计数等），位号通过线性公式换算成人能看懂的**工程值**：

```text
工程值 = 原始值 × multiple + baseValue   （再按 valueDecimal 保留小数）
```

例：一个温度变送器寄存器读数 `2531`，配置 `multiple = 0.01`、`baseValue = 0`、`unit = ℃`、`valueDecimal = 2`
，换算后存入[位号值](./point-value)的工程值就是 `25.31 ℃`。默认 `multiple = 1`、`baseValue = 0` 表示原始值即工程值，不做换算。

## 与其它概念的关系

<PointRelationDiagram lang="zh" />

- 位号**定义**挂在[模板](./profile)下，与[指令](./command)、[事件](./event)并列，共同描述"这类设备有什么能力"。
- [设备](./device)归属一个模板，因此自动拥有该模板下的全部位号；运行态数据按 `device_id + point_id`
  落成[位号值](./point-value)。

## 示例

给空调模板配三个位号：

| pointCode     | pointName | pointTypeFlag | rwFlag       | unit | 说明                       |
|---------------|-----------|---------------|--------------|------|--------------------------|
| `indoor_temp` | 室温        | `FLOAT`       | `READ_ONLY`  | `℃`  | 只采集，配 `multiple=0.1` 做换算 |
| `set_temp`    | 设定温度      | `FLOAT`       | `READ_WRITE` | `℃`  | 可读回，也可下发新设定值             |
| `power`       | 开关状态      | `BOOLEAN`     | `READ_WRITE` | —    | 读当前开关，写控制启停              |

某台空调（`device_id=1001`）采到 `indoor_temp` 的工程值 `26.5℃` → 落成一条[位号值](./point-value)；要把它调到 22℃，对
`set_temp` 发一条写 `PointCommand`，因为它 `rwFlag=READ_WRITE`，写请求通过校验后下发到驱动。

::: tip 一个位号同时具备类型、读写、单位、换算
新建位号若不显式配置，类型默认 `STRING`、读写默认 `READ_ONLY`、`baseValue=0`、`multiple=1`、`valueDecimal=6`、`unit`
为空。采集数值量时记得改成对应的数值类型并配好换算，否则会按字符串原样存。
:::

## 延伸阅读

- [模板 Profile](./profile) — 位号定义挂在模板下
- [位号值 PointValue](./point-value) — 位号的运行态取值快照
- [指令 Command](./command) — 动作型能力；位号读写不在此表
- [事件 Event](./event) — 与位号并列的另一类能力
- [设备接入](../../operation/device-onboarding) — 设备如何选定模板并继承其位号
