# 模块清单

本页按仓库目录列出 IoT DC3 当前模块。每个模块链接指向 GitHub `release` 分支中的原始 `README.md` 或源码目录，便于进一步查看实现。

::: tip 事实来源
驱动数量和模块名称以仓库当前目录为准：`dc3-driver/` 下共有 28 个接入驱动模块。
:::

## 网关

| 模块 | 说明 | 文档 |
|------|------|------|
| `dc3-gateway` | Spring Cloud Gateway，对外 HTTP 统一入口 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-gateway/README.md) |

## 中心服务

| 模块 | 说明 | 文档 |
|------|------|------|
| `dc3-center-auth` | 认证中心，管理租户、用户、角色、资源和 Token | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-auth/README.md) |
| `dc3-center-manager` | 管理中心，管理驱动、模板、设备、位号和元数据 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-manager/README.md) |
| `dc3-center-data` | 数据中心，处理点位值、查询和命令分发 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-data/README.md) |
| `dc3-center-agentic` | Agentic Center，承载 AI 会话、模型提供方和工具调用 | [README](https://github.com/pnoker/iot-dc3/tree/release/dc3-center/dc3-center-agentic) |
| `dc3-center-single` | 单进程聚合启动，适合本地调试 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-single/README.md) |

## 协议驱动

| 分类 | 模块 | 协议 / 用途 |
|------|------|-------------|
| 工业协议 | `dc3-driver-modbus-tcp` | Modbus TCP |
| 工业协议 | `dc3-driver-modbus-rtu` | Modbus RTU |
| 工业协议 | `dc3-driver-opc-ua` | OPC UA |
| 工业协议 | `dc3-driver-opc-da` | OPC DA |
| 工业协议 | `dc3-driver-plcs7` | Siemens S7 |
| 工业协议 | `dc3-driver-bacnet-ip` | BACnet/IP |
| 工业协议 | `dc3-driver-ethernet-ip` | EtherNet/IP |
| 工业协议 | `dc3-driver-fins` | Omron FINS |
| 工业协议 | `dc3-driver-melsec` | Mitsubishi MELSEC |
| 工业协议 | `dc3-driver-iec104` | IEC 60870-5-104 |
| 工业协议 | `dc3-driver-sl651` | SL651 水文监测协议 |
| 工业协议 | `dc3-driver-dlms` | DLMS / COSEM |
| 物联网协议 | `dc3-driver-mqtt` | MQTT |
| 物联网协议 | `dc3-driver-coap` | CoAP |
| 物联网协议 | `dc3-driver-lwm2m` | LwM2M |
| 物联网协议 | `dc3-driver-http` | HTTP |
| 物联网协议 | `dc3-driver-ble` | Bluetooth Low Energy |
| 物联网协议 | `dc3-driver-zigbee` | Zigbee |
| 数据桥接 | `dc3-driver-mysql` | MySQL 数据源 |
| 数据桥接 | `dc3-driver-postgresql` | PostgreSQL 数据源 |
| 数据桥接 | `dc3-driver-oracle` | Oracle 数据源 |
| 数据桥接 | `dc3-driver-sqlserver` | SQL Server 数据源 |
| 基础通信与管理 | `dc3-driver-tcp-udp` | TCP / UDP |
| 基础通信与管理 | `dc3-driver-serial` | Serial |
| 基础通信与管理 | `dc3-driver-snmp` | SNMP |
| 基础通信与管理 | `dc3-driver-can` | CAN |
| 仿真与调试 | `dc3-driver-virtual` | 虚拟驱动 |
| 仿真与调试 | `dc3-driver-listening-virtual` | 监听式虚拟驱动 |

驱动开发方式见 [驱动开发](../development/driver-authoring.md)。

## API 合约

| 模块 | 用途 | 文档 |
|------|------|------|
| `dc3-api-auth` | Auth Center gRPC / Protobuf 合约 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-auth/README.md) |
| `dc3-api-manager` | Manager Center gRPC / Protobuf 合约 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-manager/README.md) |
| `dc3-api-data` | Data Center gRPC / Protobuf 合约 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-data/README.md) |
| `dc3-api-driver` | Driver gRPC / Protobuf 合约 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-driver/README.md) |

## 公共组件

| 分类 | 模块 | 用途 |
|------|------|------|
| 基础模型 | `dc3-common-model` | BO / VO / DTO / Builder / Ext 等共享模型 |
| 基础能力 | `dc3-common-public` | `R<T>` 响应封装、`BaseService`、租户标记等公共能力 |
| Web | `dc3-common-web` | WebFlux、BaseController、OpenAPI、安全基础配置 |
| 常量与异常 | `dc3-common-constant` | 常量、枚举和值对象 |
| 常量与异常 | `dc3-common-exception` | 异常体系 |
| 数据访问 | `dc3-common-dal` | 共享 DAL 基础能力 |
| 数据访问 | `dc3-common-postgres` | PostgreSQL / MyBatis-Plus 配置 |
| 数据访问 | `dc3-common-sql` | SQL 工具 |
| 数据访问 | `dc3-common-repository` | 点位值存储抽象 |
| 通信 | `dc3-common-rabbitmq` | RabbitMQ 配置和常量 |
| 通信 | `dc3-common-mqtt` | MQTT 客户端配置 |
| 通信 | `dc3-common-facade-api` | 跨服务 facade 接口 |
| 通信 | `dc3-common-facade-grpc` | gRPC facade 实现 |
| 通信 | `dc3-common-facade-local-auth` | Auth 本地 facade |
| 通信 | `dc3-common-facade-local-manager` | Manager 本地 facade |
| 通信 | `dc3-common-facade-local-data` | Data 本地 facade |
| 领域能力 | `dc3-common-auth` | 认证、授权、租户和 Token 领域能力 |
| 领域能力 | `dc3-common-manager` | 驱动、模板、设备、位号和元数据领域能力 |
| 领域能力 | `dc3-common-data` | 点位值、命令和数据查询领域能力 |
| 领域能力 | `dc3-common-driver` | Driver SDK、注册、调度、采集和命令运行时 |
| 领域能力 | `dc3-common-agentic` | AI 会话、模型提供方、工具调用和记忆能力 |
| 网关 | `dc3-common-gateway` | Gateway 过滤器和路由辅助能力 |
| 平台支撑 | `dc3-common-log` | 日志配置 |
| 平台支撑 | `dc3-common-thread` | 线程池配置 |
| 平台支撑 | `dc3-common-quartz` | 调度基础设施 |
| 平台支撑 | `dc3-common-api` | API 工具 |
| 平台支撑 | `dc3-common-resource-registrar` | 资源注册 |
| 测试 | `dc3-common-test` | Testcontainers、gRPC、RabbitMQ 和契约测试基础设施 |

## 相关文档

- [架构总览](../architecture/)
- [模块与依赖](../architecture/modules.md)
- [驱动开发](../development/driver-authoring.md)
- [API 文档](../development/api-documentation.md)
