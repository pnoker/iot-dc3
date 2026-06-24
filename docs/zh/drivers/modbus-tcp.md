---
title: Modbus TCP 驱动
---

# Modbus TCP 驱动

> **`dc3-driver-modbus-tcp` 把 Modbus TCP 从站设备接入 IoT DC3**——以从站为目标，周期性读取线圈/寄存器值，并支持向寄存器写值的命令。

Modbus TCP 是工业现场最常见的协议之一（PLC、电表、传感器网关大量在用）。本驱动作为 Modbus 主站（client），通过 TCP 连到一个或多个从站，按[位号](../introduction/concepts/point)上配置的功能码与地址采数、写值。

- **驱动名 / code**：`Modbus TCP Driver` / `ModbusTcpDriver`
- **类型**：`DRIVER_CLIENT`（主动连从站）

## 驱动配置（设备级 `driver-attribute`）

接入一台 Modbus TCP 设备时，在[设备](../introduction/concepts/device)上填这些[属性](../introduction/concepts/attribute-config)：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Host | `host` | STRING | `localhost` | Modbus 从站 IP |
| Port | `port` | INT | `502` | Modbus TCP 端口（标准 502）|

## 位号配置（`point-attribute`）

每个采集[位号](../introduction/concepts/point)上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | Modbus 从站单元号 |
| Function Code | `functionCode` | INT | `1` | 读功能码 `[1, 2, 3, 4]` |
| Offset | `offset` | INT | `0` | 寄存器/线圈地址偏移 |

::: tip 功能码决定读什么
读取用 `01`（线圈）/ `02`（离散输入）/ `03`（保持寄存器）/ `04`（输入寄存器）。位号的数据类型（[Point](../introduction/concepts/point) 的 `pointTypeFlag`）要和功能码取回的数据宽度对得上——多寄存器量（如 32 位 `FLOAT`）由位号类型决定怎么拼。
:::

## 写命令配置（`command-attribute`）

可写位号还需在写命令上填：

| 属性 | code | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| Slave ID | `slaveId` | INT | `1` | 从站单元号 |
| Function Code | `functionCode` | INT | `6` | 写功能码 `[5, 6, 15, 16]` |
| Offset | `offset` | INT | `0` | 地址偏移 |
| Value Template | `valueTemplate` | STRING | `${value}` | 写值模板，用命令参数渲染 |

## 采集与健康

- **采集周期**：默认 cron `0/30 * * * * ?`（每 30 秒读一轮）。
- **健康/在线**：设备健康检查默认 cron `0/15 * * * * ?`，租约超时 `45 秒`——在线状态机制见[设备](../introduction/concepts/device)。

## 最小接入示例

把 IP `192.168.1.10:502` 的一台 Modbus 从站接进来：

1. 选 `Modbus TCP Driver` 创建[设备](../introduction/concepts/device)，driver 属性填 `host=192.168.1.10`、`port=502`。
2. 给设备绑定的[物模型](../introduction/concepts/profile)加一个温度[位号](../introduction/concepts/point)（`pointTypeFlag=FLOAT`、`READ_ONLY`），point 属性填 `slaveId=1`、`functionCode=3`、`offset=0`。
3. 启动驱动，30 秒内就能在[位号值](../introduction/concepts/point-value)里看到采集值。

## 易错点

::: warning offset 是 0 基协议地址，不是 40001
`offset` 是协议层的 0 基偏移。按 Modbus 习惯写法读"保持寄存器 40001"，应填 `functionCode=3`、`offset=0`（第 2 个保持寄存器是 `offset=1`，以此类推）。把 `40001` 直接填进 `offset` 会读到错误地址。
:::

::: tip 一个驱动实例可接多台从站
同一个 Modbus TCP 驱动进程可服务多台设备。多台从站挂在同一网关不同单元号时，`host` 相同、由位号的 `slaveId` 区分。
:::

## 延伸阅读

- [驱动 Driver](../introduction/concepts/driver) — 驱动的通用模型与注册机制
- [属性与配置](../introduction/concepts/attribute-config) — `host` / `slaveId` 这些属性的三层来历
- [设备接入](../operation/device-onboarding) — 一次完整的接入流程
- [Modbus RTU 驱动](./modbus-rtu) — 串口版 Modbus
