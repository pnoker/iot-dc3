---
title: 项目设计方案
---

# 项目设计方案

这里沉淀项目级设计方案、领域模型调整、接口约定和落地演进思路。方案文档优先说明背景、边界、核心模型、兼容策略和分阶段落地计划，避免实现前只剩口头约定。

## 目录

- [物模型设计方案](thing-model.md) - 基于现有 `Profile` 体系补齐属性、服务、事件三类物模型能力
- [位号命令链路重构方案](point-command.md) - 把 `PointValueCommand / DeviceCommand` 统一为 `PointCommand`，清理
  `DriverCommand*` 死代码，并修复唯一性、准确性、结果反馈、报文序列化四类问题
- [设备与驱动状态超时管理说明](device-driver-timeout.md) - 梳理当前基于 Data Center 本地 TTL 缓存的状态续租、过期告警、查询语义和后续持久化租约演进
- [实体告警统一表设计方案](entity-alarm.md) - 将 `dc3_driver_event`、`dc3_device_event` 合并为 `dc3_entity_alarm`，统一
  point/device/driver 告警记录和规则落库链路
- [规则告警链路优化与缺陷修复方案](rule-alarm-optimization.md) - 优化规则缓存、批量判断、状态转换和通知异步化，修复
  tenantId 缺失、恢复误判、窗口规则未落地等问题
