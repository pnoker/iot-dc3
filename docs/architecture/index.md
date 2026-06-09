# 架构

IoT DC3 的整体分层与运行时数据流。

![iot-dc3-architecture](/images/architecture-cn.png)

## 分层概览

平台采用经典的微服务分层架构，从下到上：

| 层                         | 职责                      | 主要模块                                                        |
|---------------------------|-------------------------|-------------------------------------------------------------|
| **驱动层** Driver Layer      | 设备接入、协议适配、南向数据采集与命令执行   | `dc3-driver-*`（28 个接入驱动模块）        |
| **数据层** Data Layer        | 实时与历史数据采集、存储、查询         | `dc3-center-data`                                           |
| **管理层** Management Layer  | 服务注册、设备/驱动管理、配置治理、命令编排  | `dc3-center-manager`、`dc3-center-auth`、`dc3-center-agentic` |
| **应用层** Application Layer | API 网关、对外开放、第三方集成、AI 增强 | `dc3-gateway`、`dc3-web`                                     |

## 关键设计

- **gRPC 内部 facade**：中心服务之间通过 gRPC 通信（`DC3_FACADE_MODE=grpc`），统一通过 `*Facade` 接口跨进程调用
- **MQ 异步解耦**：驱动 → 数据中心走 RabbitMQ 异步消息，避免点位写入阻塞
- **多租户隔离**：通过 `tenantId` + 命名空间隔离，支持租户级数据/权限隔离
- **HMAC 网关签名**：网关到后端的 `X-Auth-User` 请求头由 `AUTH_HMAC_SECRET` 签名，防伪造

## 进一步阅读

- [模块与依赖](modules.md) — Maven 模块图、运行时启动顺序
- [驱动开发](../development/driver-authoring.md) — 从模板派生新协议驱动
- [环境变量](../quickstart/environment.md) — 跨进程变量约定
