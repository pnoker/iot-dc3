# 模块与依赖

本页说明 IoT DC3 的 Maven 模块结构、运行时服务边界和驱动模块分布。

## 顶层模块

```text
iot-dc3 (root pom)
+-- dc3-api          <- Protobuf / gRPC API 合约
+-- dc3-common       <- 可复用业务和基础库
+-- dc3-center       <- 可部署中心服务
+-- dc3-driver       <- 协议驱动
+-- dc3-gateway      <- Spring Cloud Gateway
```

## dc3-api：gRPC 合约层

| 模块 | 说明 |
|------|------|
| `dc3-api-auth` | Auth protobuf：租户、用户、Token |
| `dc3-api-data` | Data protobuf：点位值读写 |
| `dc3-api-driver` | Driver protobuf：驱动注册、命令 |
| `dc3-api-manager` | Manager protobuf：设备、模板、驱动管理 |

## dc3-common：共享库

### 基础层

| 模块 | 用途 |
|------|------|
| `dc3-common-constant` | 枚举、常量和值对象 |
| `dc3-common-exception` | 自定义异常体系 |
| `dc3-common-public` | `R<T>` 响应封装、公共工具和基础接口 |
| `dc3-common-model` | BO / VO / DTO 等实体定义 |
| `dc3-common-web` | BaseController、WebFlux、OpenAPI 和安全基础配置 |
| `dc3-common-log` | 请求/响应和日志配置 |
| `dc3-common-thread` | 线程池配置 |

### 数据访问层

| 模块 | 用途 |
|------|------|
| `dc3-common-dal` | MyBatis-Plus mapper / repository 基础能力 |
| `dc3-common-postgres` | PostgreSQL 数据源和多 schema 配置 |
| `dc3-common-repository` | 文件、点位值或 blob 存储抽象 |
| `dc3-common-sql` | SQL 工具 |

运行时缓存使用 Caffeine 等进程内能力，例如 `LocalCacheService`。Redis 不再是当前文档描述的独立基础设施依赖。

### 通信层

| 模块 | 用途 |
|------|------|
| `dc3-common-facade-api` | 跨服务 facade 接口 |
| `dc3-common-facade-grpc` | gRPC facade 实现 |
| `dc3-common-facade-local-auth` | Auth 本地 facade |
| `dc3-common-facade-local-manager` | Manager 本地 facade |
| `dc3-common-facade-local-data` | Data 本地 facade |
| `dc3-common-rabbitmq` | RabbitMQ exchange / queue 配置 |
| `dc3-common-mqtt` | MQTT 客户端配置 |

### 领域层

| 模块 | 用途 | 关键依赖 |
|------|------|----------|
| `dc3-common-auth` | 认证、租户、用户、角色、资源和 Token 业务逻辑 | `dc3-api-auth`、dal、postgres、web |
| `dc3-common-manager` | 驱动、模板、设备、位号和元数据业务逻辑 | `dc3-api-*`、dal、facade-api、postgres、rabbitmq、quartz、web |
| `dc3-common-data` | 点位值、命令、数据查询和仪表盘业务逻辑 | `dc3-api-*`、dal、facade-api、postgres、rabbitmq、repository、quartz、web |
| `dc3-common-driver` | Driver SDK、注册、调度、采集和命令运行时 | driver API、constant、model、rabbitmq、quartz、thread |
| `dc3-common-gateway` | Gateway 过滤器、认证透传和路由辅助能力 | facade-api、log、model、public、web |
| `dc3-common-agentic` | AI 会话、模型提供方、工具调用和记忆能力 | facade-api、postgres、web、Spring AI |

## dc3-center：可部署服务

| 服务 | 依赖 | HTTP | gRPC |
|------|------|------|------|
| `dc3-center-auth` | `dc3-common-auth` | 8300 | 9300 |
| `dc3-center-manager` | `dc3-common-manager`、facade-grpc | 8400 | 9400 |
| `dc3-center-data` | `dc3-common-data`、facade-grpc | 8500 | 9500 |
| `dc3-center-agentic` | `dc3-common-agentic`、facade-grpc | 8600 | - |
| `dc3-center-single` | auth + manager + data，本地 facade | 8200 | 9200 |

## dc3-driver：协议驱动

所有驱动都依赖 `dc3-common-driver`。驱动通过 gRPC 获取管理元数据，通过 RabbitMQ 发送点位值和接收命令。

| 分类 | 驱动 | 协议 / 用途 |
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
| 工业协议 | `dc3-driver-sl651` | SL651 |
| 工业协议 | `dc3-driver-dlms` | DLMS / COSEM |
| 物联网协议 | `dc3-driver-mqtt` | MQTT |
| 物联网协议 | `dc3-driver-coap` | CoAP |
| 物联网协议 | `dc3-driver-lwm2m` | LwM2M |
| 物联网协议 | `dc3-driver-http` | HTTP |
| 物联网协议 | `dc3-driver-ble` | Bluetooth Low Energy |
| 物联网协议 | `dc3-driver-zigbee` | Zigbee |
| 数据桥接 | `dc3-driver-mysql` | MySQL |
| 数据桥接 | `dc3-driver-postgresql` | PostgreSQL |
| 数据桥接 | `dc3-driver-oracle` | Oracle |
| 数据桥接 | `dc3-driver-sqlserver` | SQL Server |
| 基础通信与管理 | `dc3-driver-tcp-udp` | TCP / UDP |
| 基础通信与管理 | `dc3-driver-serial` | Serial |
| 基础通信与管理 | `dc3-driver-snmp` | SNMP |
| 基础通信与管理 | `dc3-driver-can` | CAN |
| 仿真与调试 | `dc3-driver-virtual` | 虚拟驱动 |
| 仿真与调试 | `dc3-driver-listening-virtual` | 监听式虚拟驱动 |

## dc3-gateway

`dc3-gateway` 依赖 `dc3-common-gateway` 和 `dc3-common-facade-grpc`。它把 `/api/v3/*` 路由到各中心服务，包含 `/api/v3/agentic/**`，并通过 Gateway 过滤器处理认证透传。

## 运行时流程

```text
Client
  -> Gateway (8000)
  -> Auth / Manager / Data / Agentic (REST)
  -> dc3-common-* libs
  -> PostgreSQL / RabbitMQ / Caffeine
  -> dc3-driver (gRPC + MQ)
  -> Device / Data Source
```

## 相关文档

- [模块清单](../modules/)
- [快速开始](../quickstart/)
- [驱动开发](../development/driver-authoring.md)
