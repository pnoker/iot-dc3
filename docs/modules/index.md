# 模块清单

按子项目分类的模块文档索引。每个链接指向 GitHub 上的 `README.md` 原文件，可在线浏览。

!!! tip "首期范围"
    模块文档暂保留在各子模块目录下未做迁移；后续会按需逐步搬入文档站并中文化。

## 网关 / Gateway

| 模块 | 说明 | 文档 |
|---|---|---|
| `dc3-gateway` | API 网关，统一入口，HMAC 签名转发 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-gateway/README.md) |

## 中心服务 / Center

| 模块 | 说明 | 文档 |
|---|---|---|
| `dc3-center-auth` | 认证中心，账号/Token/HMAC | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-auth/README.md) |
| `dc3-center-manager` | 管理中心，设备/驱动/点位/配置治理 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-manager/README.md) |
| `dc3-center-data` | 数据中心，实时与历史数据 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-data/README.md) |
| `dc3-center-single` | 单进程聚合启动（调试用） | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-center/dc3-center-single/README.md) |

## 协议驱动 / Driver

| 模块 | 协议 | 文档 |
|---|---|---|
| `dc3-driver-virtual` | 虚拟驱动（开发模板） | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/dc3-driver-virtual/README.md) |
| `dc3-driver-listening-virtual` | 监听式虚拟驱动（TCP/UDP） | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/dc3-driver-listening-virtual/README.md) |
| `dc3-driver-mqtt` | MQTT | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/dc3-driver-mqtt/README.md) |
| `dc3-driver-modbus-tcp` | Modbus TCP | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/dc3-driver-modbus-tcp/README.md) |
| `dc3-driver-opc-da` | OPC DA | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/dc3-driver-opc-da/README.md) |
| `dc3-driver-opc-ua` | OPC UA | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/dc3-driver-opc-ua/README.md) |
| `dc3-driver-plcs7` | Siemens S7 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-driver/dc3-driver-plcs7/README.md) |

## 对外 API / API 模块

| 模块 | 用途 | 文档 |
|---|---|---|
| `dc3-api-auth` | Auth Center 对外 facade | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-auth/README.md) |
| `dc3-api-manager` | Manager Center 对外 facade | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-manager/README.md) |
| `dc3-api-data` | Data Center 对外 facade | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-data/README.md) |
| `dc3-api-driver` | Driver 对外 facade | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-api/dc3-api-driver/README.md) |

## 公共组件 / Common

| 模块 | 用途 | 文档 |
|---|---|---|
| `dc3-common-api` | 跨服务 API 协议定义 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-api/README.md) |
| `dc3-common-auth` | 认证、HMAC、租户上下文 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-auth/README.md) |
| `dc3-common-constant` | 常量与枚举 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-constant/README.md) |
| `dc3-common-dal` | 数据访问层基础 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-dal/README.md) |
| `dc3-common-data` | 数据领域模型 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-data/README.md) |
| `dc3-common-driver` | 驱动 SDK | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-driver/README.md) |
| `dc3-common-exception` | 异常类型 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-exception/README.md) |
| `dc3-common-gateway` | 网关共用 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-gateway/README.md) |
| `dc3-common-log` | 日志规范 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-log/README.md) |
| `dc3-common-manager` | 管理领域模型 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-manager/README.md) |
| `dc3-common-model` | 通用模型 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-model/README.md) |
| `dc3-common-mqtt` | MQTT 客户端封装 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-mqtt/README.md) |
| `dc3-common-postgres` | Postgres 数据源 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-postgres/README.md) |
| `dc3-common-public` | 公开工具类 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-public/README.md) |
| `dc3-common-quartz` | 定时任务 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-quartz/README.md) |
| `dc3-common-rabbitmq` | RabbitMQ 客户端 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-rabbitmq/README.md) |
| `dc3-common-repository` | Repository 抽象 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-repository/README.md) |
| `dc3-common-thread` | 线程池 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-thread/README.md) |
| `dc3-common-web` | Web 通用 | [README](https://github.com/pnoker/iot-dc3/blob/release/dc3-common/dc3-common-web/README.md) |
