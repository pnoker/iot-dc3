---
title: Modbus TCP 驱动
---

<script setup>
import ModbusTcpDiagram from '../../.vitepress/theme/components/ModbusTcpDiagram.vue'
</script>


# Modbus TCP 驱动

`dc3-driver-modbus-tcp` 把 Modbus TCP 从站设备接入 IoT DC3。它作为 Modbus
主站（client），通过以太网周期性读取线圈/寄存器，并支持向线圈和保持寄存器写值。读完你能在[设备](../introduction/concepts/device)
上配好 `host`/`port`、在[位号](../introduction/concepts/point)上配好功能码与地址，并定位常见的"读不到值/写不下去"问题。

> 你在这里：网络层"工业有线侧"
> 的一个落地驱动。协议层面的寻址模型、字节序、功能码概念见[工业总线与协议](../foundations/fieldbus)。

## 协议背景

Modbus 诞生于 1979 年，最初是 Modicon 为 PLC 串口通信设计的主从协议，至今仍是工业现场最常见的协议之一——PLC、电表、变频器、传感器网关大量在用。它简单、开放、文档公开，因此被各家厂商广泛实现。

**Modbus TCP** 是 Modbus 的以太网封装：把原本跑在 RS-485 串口上的 Modbus 应用层报文（PDU）裹进 TCP/IP，默认监听 **502**
端口。相比串口版 [Modbus RTU](./modbus-rtu)，TCP 版去掉了 CRC 校验（交给 TCP 保证），并在报文前加了 7 字节的 MBAP
头来做事务标识。一条以太网上可以挂多个从站，也可以由一个 Modbus TCP 网关桥接背后的多台串口从站。

在[物联网四层架构](../foundations/fieldbus)里，Modbus TCP 属于**网络层**
的工业有线侧：它定义现场设备如何在网络上被寻址与读写，把感知层采集到的物理量搬运到平台。它的通信模型是典型的**主从 / 请求-响应
**——主站不主动问，从站就不会说话，所以 IoT DC3 的驱动作为主站按 cron 周期轮询。

Modbus 的数据被组织成四类寄存器空间，读取由**功能码**区分：

<ModbusTcpDiagram lang="zh" />

线圈与离散输入是单比特（开关量），保持寄存器与输入寄存器是 16 位字（模拟量）。一个 32 位 `FLOAT` 或 `LONG` 要占连续两个寄存器，64
位 `DOUBLE` 占四个——驱动按位号的数据类型自动跨寄存器拼装。

## 属性配置

接入一台 Modbus TCP 设备，需要在三个层面填[属性](../introduction/concepts/attribute-config)：设备级的连接参数（
`driver-attribute`）、每个采集位号的寻址参数（`point-attribute`）、每个可写位号的写命令参数（`command-attribute`
）。下面各属性、类型、默认值均取自驱动的 `application.yml`（`dc3-driver-modbus-tcp` 模块）。

### 驱动属性（设备级 `driver-attribute`）

驱动属性回答"连到哪台从站"。在[设备](../introduction/concepts/device)上为每台 Modbus TCP 设备填一组：

| 属性   | code   | 类型     | 默认值         | 说明                    |
|------|--------|--------|-------------|-----------------------|
| Host | `host` | STRING | `localhost` | Modbus 从站 IP 或主机名     |
| Port | `port` | INT    | `502`       | Modbus TCP 端口（标准 502） |

`host` + `port` 唯一确定一条 TCP 连接。驱动按设备 ID 缓存连接（一台设备一条 `ModbusMaster`），连接的 socket 超时固定为 **5 秒
**。`port` 在配置校验时会检查必须落在 `1–65535` 之间。

### 位号属性（`point-attribute`）

位号属性回答"读这台从站的哪一个数据点"。每个采集[位号](../introduction/concepts/point)填一组：

| 属性            | code           | 类型  | 默认值 | 说明                      |
|---------------|----------------|-----|-----|-------------------------|
| Slave ID      | `slaveId`      | INT | `1` | Modbus 从站单元号（unit ID）   |
| Function Code | `functionCode` | INT | `1` | 读功能码，仅支持 `[1, 2, 3, 4]` |
| Offset        | `offset`       | INT | `0` | 寄存器/线圈地址偏移（0 基）         |

`slaveId` 用于区分挂在同一 IP（网关）背后的多台从站。`functionCode` 决定读哪类寄存器空间，配置校验会强制它落在 `1–4`：

::: tip 功能码决定读什么寄存器空间
读取用 `01`（线圈）/ `02`（离散输入）/ `03`（保持寄存器）/ `04`
（输入寄存器）。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要和功能码取回的数据宽度对得上——驱动按类型映射为
Modbus 数据宽度：`LONG`→4 字节整型、`FLOAT`→4 字节浮点、`DOUBLE`→8 字节浮点、其余（如 `INT`）→2 字节整型。多寄存器量由位号类型自动跨寄存器拼装。
:::

### 写命令属性（`command-attribute`）

可写位号还要在写命令上填一组，回答"往哪个地址写、写成什么值"：

| 属性             | code            | 类型     | 默认值        | 说明                                     |
|----------------|-----------------|--------|------------|----------------------------------------|
| Slave ID       | `slaveId`       | INT    | `1`        | 从站单元号                                  |
| Function Code  | `functionCode`  | INT    | `6`        | 写功能码（yml 标注 `[5, 6, 15, 16]`，但见下方实现状态） |
| Offset         | `offset`        | INT    | `0`        | 地址偏移（0 基）                              |
| Value Template | `valueTemplate` | STRING | `${value}` | 写值模板，用命令参数渲染                           |

`valueTemplate` 默认 `${value}` 表示直接写入命令传入的值；需要换算（如乘系数、加偏移）时可改模板。写值的类型由位号的
`pointTypeFlag` 决定如何编码进寄存器。

::: warning 写功能码：yml 列了 4 个，实现只认 2 个
`application.yml` 把写功能码标注为 `[5, 6, 15, 16]`、默认 `6`，但当前 `ModbusTcpDriverCustomServiceImpl.writeValue()` 只处理
**`functionCode=1`（写单个线圈）** 和 **`functionCode=3`（写单个保持寄存器）** 两种，其余功能码（含默认值 `6`）会落到 `default`
分支、直接返回 `false`（写失败）。因此写寄存器请把命令的 `functionCode` 显式改成 `3`、写线圈改成 `1`，不要沿用默认 `6`
。FC05/06/15/16 的语义尚未在代码里实现——以代码为准。
:::

## 故障排查

Modbus TCP 接入失败大多集中在连接、寻址、字节序三类。按下面顺序排查：

1. **端口/连接不通（设备一直 offline）**。先确认从站 IP 与 502 端口可达：`telnet <host> 502` 或 `nc -vz <host> 502`。驱动的
   socket 超时是 5 秒，连不上会抛 `ConnectorException`。注意：连续 **3 次**连接失败后驱动进入 **60 秒退避**，期间健康检查直接报
   offline、不再尝试连接——修好网络后最多等一个退避周期就会自动恢复。

2. **能连上但读不到值 / 报错**。检查位号的 `slaveId` 是否对（网关背后多从站时尤其常见错配）、`functionCode`
   与该地址实际的寄存器类型是否一致（拿读保持寄存器的 `03` 去读只读的离散输入会报异常）。读失败会抛 `ReadPointException`
   并使该设备连接失效、下个周期重连。

3. **`offset` 填成了 40001 这类地址**。这是最高频的错误，详见下方易错点容器——`offset` 是 0 基协议偏移，不是 PLC 习惯的
   4xxxx 编号。

4. **数值不对 / 大小颠倒（字节序问题）**。32/64 位数值跨多个寄存器，不同设备的寄存器字序（word order）/字节序（byte
   order）约定不同。驱动用底层 modbus4j 的默认字序读取——若读出的浮点数明显错乱（如把 `25.0`
   读成天文数字），通常是设备端字序与默认不符。当前驱动属性未暴露字序开关，遇到此类设备需在设备侧调整寄存器映射，或改用整型读取后自行换算。

5. **写命令返回失败**。先按上面的「写功能码」警告确认 `functionCode` 是 `1` 或 `3`；若仍失败，检查目标是否为可写空间（离散输入
   `02`、输入寄存器 `04` 物理只读，无法写）。写失败抛 `WritePointException` 并使连接失效。

6. **设备在线状态抖动**。健康检查默认每 15 秒一次、租约超时 45 秒。若设备频繁在 online/offline 间跳变，多半是网络丢包或从站响应慢于
   5 秒超时——在线状态机制见[设备](../introduction/concepts/device)。

::: warning offset 是 0 基协议地址，不是 40001
`offset` 是协议层的 0 基偏移。按 Modbus 习惯写法读"保持寄存器 40001"，应填 `functionCode=3`、`offset=0`（第 2 个保持寄存器是
`offset=1`，以此类推）。把 `40001` 直接填进 `offset` 会读到错误地址或越界报错。
:::

## 在 IoT DC3 中如何落地

- **`dc3.driver.code`**：`ModbusTcpDriver`（类型 `DRIVER_CLIENT`，主动连从站）。这是稳定的路由标识，不要随意改。
- **读能力**：✓ 已实现。支持功能码 `1/2/3/4`（线圈/离散输入/保持寄存器/输入寄存器），按位号类型自动拼装多寄存器量。
- **写能力**：✓ 已实现，但**仅** `functionCode=1`（写线圈）与 `functionCode=3`（写保持寄存器）；其余功能码返回失败（见上方写功能码警告）。
- **订阅/上报**：— 不支持。Modbus 是主从轮询模型，驱动只主动读写、不被动接收推送。这与[驱动能力矩阵](./matrix)中 Modbus TCP
  的「✓ / ✓ / —」一致。
- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮），在驱动 `application.yml` 的 `schedule.read` 配置；`custom`
  自定义调度默认关闭。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`。

::: info 实现状态：可用
本驱动是**完整实现**（非骨架），底层基于 modbus4j。读路径覆盖全部四类寄存器，写路径覆盖线圈与保持寄存器，并带连接缓存与失败退避。唯一需注意的差异是写功能码的
yml 标注（`[5,6,15,16]`）宽于代码实现（仅 `1/3`）——按上方警告显式配置即可正常落地。
:::

### 最小接入示例

把 IP `192.168.1.10:502` 的一台 Modbus 从站接进来：

1. 选 `Modbus TCP Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`port=502`。
2. 给设备绑定的[模板](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（
   `pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `slaveId=1`、`functionCode=3`、`offset=0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。
4. 若该位号需可写，给它配写[命令](../introduction/concepts/command)，把 `functionCode` 显式设为 `3`（写保持寄存器）。

::: tip 一个驱动实例可接多台从站
同一个 Modbus TCP 驱动进程可服务多台设备。多台从站挂在同一网关不同单元号时，`host` 相同、由位号的 `slaveId` 区分；不同 IP
的设备则各占一条缓存连接。
:::

## 延伸阅读

- [驱动总览](./index) — 全部驱动入口与分类
- [驱动能力矩阵](./matrix) — 读/写/订阅能力一览，含 Modbus TCP 行
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [工业总线与协议](../foundations/fieldbus) — Modbus 等协议的寻址模型与字节序原理
- [Modbus RTU 驱动](./modbus-rtu) — 串口版 Modbus
