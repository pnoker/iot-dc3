# 核心概念

IoT DC3 的操作对象围绕“驱动接入设备、模板描述点位、数据中心保存值、管理中心下发命令”展开。

## 对象关系

| 概念 | 说明 | 典型来源 |
|------|------|----------|
| Driver | 协议驱动实例，负责与设备或数据源通信 | `dc3-driver-*` 服务启动后注册 |
| Profile | 设备模板，抽象同类设备的点位和配置结构 | 用户在管理侧维护 |
| Device | 具体设备实例，绑定驱动和模板 | 用户创建或导入 |
| Point | 位号/测点，描述要采集或写入的一个数据项 | 模板或设备配置 |
| Driver Attribute | 设备级连接参数，例如 host、port、站号、认证信息 | 驱动 `application.yml` 注册 |
| Point Attribute | 点位级协议参数，例如寄存器地址、对象类型、功能码 | 驱动 `application.yml` 注册 |
| Point Value | 设备点位的实时或历史值 | Driver 采集后经 RabbitMQ 写入 Data Center |
| Command | 面向设备或点位的读写请求 | API、Web UI 或 Agentic Center 发起 |

## 服务分工

| 服务 | 操作职责 |
|------|----------|
| Gateway | 对外统一 API 入口，处理路由和请求身份透传 |
| Auth Center | 认证、租户、用户、角色、资源和 Token 管理 |
| Manager Center | 驱动、模板、设备、位号和元数据管理 |
| Data Center | 点位值接收、实时/历史查询、命令分发 |
| Agentic Center | 大模型配置、会话、工具调用和 AI 辅助操作 |
| Drivers | 协议适配、采集、写入、状态上报和元数据变更响应 |

## 数据流

```text
Device/Data Source
  -> Driver
  -> RabbitMQ
  -> Data Center
  -> PostgreSQL
  -> Gateway API / Web UI / Agentic Center
```

## 命令流

```text
Client / Web UI / Agentic Center
  -> Gateway
  -> Data Center
  -> RabbitMQ
  -> Driver
  -> Device
```

## 租户边界

业务数据以租户为边界隔离。调用 API、创建设备、查询数据和下发命令时，应保持租户上下文一致。开发环境默认租户通常为 `default`，生产环境应按实际组织和权限模型配置。
