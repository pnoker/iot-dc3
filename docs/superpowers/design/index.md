---
title: 设计方案
---

# 设计方案

这里沉淀项目级设计方案、领域模型调整、接口约定和落地演进思路。方案文档优先说明背景、边界、核心模型、兼容策略和分阶段落地计划。

这些材料属于工程资料，不作为公开功能说明。用户视角的当前能力以 [快速开始](../../quickstart/)、[操作手册](../../operation/) 和 [模块清单](../../modules/) 为准。

## 目录

- [物模型设计方案](thing-model.md) - 基于现有 `Profile` 体系补齐属性、服务、事件三类物模型能力
- [指令与事件属性配置方案](command-event-attribute-config.md) - 设计 `CommandAttribute`、`EventAttribute` 及设备侧
  `CommandConfig`、`EventConfig` 的配置来源、表结构和运行链路
- [位号命令链路重构方案](point-command.md) - 把 `PointValueCommand / DeviceCommand` 统一为 `PointCommand`，清理
  `DriverCommand*` 死代码，并修复唯一性、准确性、结果反馈、报文序列化四类问题
- [设备与驱动状态超时管理说明](device-driver-timeout.md) - 梳理当前基于 Data Center 本地 TTL 缓存的状态续租、过期告警、查询语义和后续持久化租约演进
- [自定义指令调用方案](command-call.md) - 梳理自定义指令的调用入口、参数模型和执行边界
- [事件上报方案](event-report.md) - 梳理驱动、设备和业务事件的上报链路
- [实体告警统一表设计](entity-alarm.md) - 统一 point/device/driver 告警记录和规则落库链路的待补充设计
- [规则告警链路优化](rule-alarm-optimization.md) - 规则缓存、批量判断、状态转换和通知异步化的待补充设计

## 状态说明

部分设计仍是草案或待补充结构页。进入开发前，需要补齐当前实现分析、迁移方案、测试计划和验收标准。
