---
title: 设备 Device
---

<script setup>
import DeviceRelationDiagram from '../../../.vitepress/theme/components/DeviceRelationDiagram.vue'
import DeviceStateDiagram from '../../../.vitepress/theme/components/DeviceStateDiagram.vue'
</script>

# 设备 Device

> **设备是现场一台具体设备在平台里的镜像**——一台 PLC、一个温控器、一块电表，在 DC3 里就对应一个 `Device`
> 。它绑定一个[模板 Profile](./profile) 决定"有哪些能力"，绑定一个[驱动 Driver](./driver) 决定"怎么通信"
> ，运行时它的在线/离线由心跳租约维护。

设备回答的是"现场到底有哪一台机器"。它不是数据本身，也不是设备的类型定义：某型号温控器"应该有温度、湿度两个位号"
是[模板](./profile)说的事；车间里编号 `TC-001` 的那一台、此刻在线、刚上报温度 25.3℃，才是一个 `Device`。

可以这样类比：[模板](./profile)像类（class），设备像它的实例（instance）。一个模板可被许多设备复用——100 台同型号温控器共用一个
Profile；但一个设备**只能归属一个** Profile（多对多绑定已在模板改造中收敛为单归属）。设备能采哪些[位号](./point)
、能下哪些[指令](./command)、会报哪些[事件](./event)，全部由它绑定的那个 Profile 决定。

设备的另一条绑定是[驱动](./driver)：Profile 说"这台设备有温度位号"，但"用 Modbus 去哪个寄存器读这个温度"是驱动的事。
`profileId` 决定能力模型，`driverId` 决定通信通道，两者缺一不可。

## 关键字段

设备业务对象 `DeviceBO`（表 `dc3_device`），字段名与类型取自源码：

| 字段           | 类型             | 含义                                 |
|--------------|----------------|------------------------------------|
| `deviceName` | String         | 设备名称（展示用，如"1号车间温控器"）               |
| `deviceCode` | String         | 设备标识符                              |
| `profileId`  | Long           | 归属的[模板 Profile](./profile)，决定能力模型 |
| `driverId`   | Long           | 归属的[驱动 Driver](./driver)，决定通信方式    |
| `deviceExt`  | DeviceExt      | JSON 扩展，存放协议无关的自定义配置               |
| `enableFlag` | EnableFlagEnum | 启停标记，见下                            |
| `tenantId`   | Long           | 归属[租户](./tenant)，多租户隔离             |

继承自 `BaseBO` 的通用字段：`id`、`remark`（描述）、`creatorId`/`creatorName`、`operatorId`/`operatorName`、`createTime`/
`operateTime`。

::: tip profileId 是单值，不是集合
早期一个设备可绑定多个 Profile（`Set<Long> profileIds`）。模板改造后收敛为 `Long profileId` 单值：一个 Profile
可被多设备复用，但一个设备只归属一个 Profile。
:::

## 启停标记

| `enableFlag` | `0` enable 启用 | `1` disable 停用 |
|--------------|---------------|----------------|

`enableFlag` 是配置态开关（这台设备是否纳入采集），与下文的运行态在线/离线是两回事：停用的设备不参与采集，启用的设备才会被驱动轮询并维护心跳租约。

## 与其它概念的关系

<DeviceRelationDiagram lang="zh" />

- 设备通过 `profileId` 取得自己的[位号](./point)、[指令](./command)、[事件](./event)定义。
- 设备运行时产生[位号值](./point-value)（`device_id + point_id`）和事件实例。
- 设备通过 `driverId` 找到[驱动](./driver)完成实际读写。

## 在线状态与心跳租约

设备的"在线/离线"不是 `dc3_device` 上的字段，而是一份独立的**运行态状态租约**，由设备/驱动的超时管理机制维护，状态事实源是
`dc3_entity_state` 表（`entity_type_flag = 6` 表示设备）。机制是"心跳续租 + 超时维护"：

<DeviceStateDiagram lang="zh" />

- **续租**：驱动按配置周期对设备做健康检查，上报 `DeviceStateDTO`，数据中心把 `expire_time` 顺延（`now + timeout`），
  `lease_version` 递增。
- **超时**：扫描器每隔固定 tick 唤醒，把 `expire_time <= now()` 仍处在线族的设备批量判为离线。不同设备的超时长短体现在各自的
  `expire_time` 上，而非扫描周期上。

状态共四种（沿用设计稿 `EntityStateStatus` 契约）：`0` online、`1` offline、`2` maintain、`3` fault。

::: warning 在线状态查的是 dc3_entity_state，不是 dc3_device
`dc3_device` 是设备的**配置元数据**（名称、归属、扩展），不存高频心跳；当前在线/离线请查 `dc3_entity_state`。把心跳写进
`dc3_device` 会污染元数据表，这正是超时方案要避免的。
:::

## 示例

车间里一台 Modbus 温控器接入 DC3：先选一个描述"温控器"这类设备的[模板](./profile)（含 `temperature`、`humidity`
两个[位号](./point)），再选一个 Modbus [驱动](./driver)，创建一个设备
`DeviceBO{ deviceName: "1号车间温控器", deviceCode: "TC-001", profileId: 1024, driverId: 2048, enableFlag: enable }`
。启用后，Modbus 驱动按 Profile 的位号配置去读寄存器，产出[位号值](./point-value)；同时每 15 秒上报一次设备健康，数据中心据此把
`dc3_entity_state` 里这台设备的 `expire_time` 续租 45 秒（租约时长）；某次驱动连不上、心跳断了，扫描器在 `expire_time`
过期后把它判为 `offline`。

## 延伸阅读

- [模板 Profile](./profile) — 决定设备有哪些位号 / 指令 / 事件
- [驱动 Driver](./driver) — 决定设备怎么通信
- [位号 Point](./point) — Profile 下的数据点定义
- [设备接入操作](../../operation/device-onboarding) — 一步步把现场设备接入平台
- [概念概览](../concepts) — 回到概念地图
