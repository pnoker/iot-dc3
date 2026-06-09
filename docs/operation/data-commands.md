# 数据与命令

数据与命令是设备接入后的核心验证点：数据要能稳定进入 Data Center，命令要能路由回正确驱动和设备。

## 数据采集

典型采集链路：

```text
Driver read/subscription
  -> PointValue
  -> RabbitMQ
  -> Data Center
  -> PostgreSQL
  -> API query
```

驱动可以通过两种方式产生点位值：

| 方式 | 场景 |
|------|------|
| 定时读取 | Modbus、OPC、PLC 等轮询类协议 |
| 主动推送 | MQTT、HTTP、监听式 TCP/UDP、订阅类协议 |

## 查询数据

开发环境推荐从 Gateway 访问 API：

```bash
curl http://localhost:8000/api/v3/data/...
```

具体路径以 Swagger UI 或 OpenAPI JSON 为准：

- Gateway 聚合入口：`http://localhost:8000/swagger-ui.html`
- Data Center 直连入口：`http://localhost:8500/data/swagger-ui.html`

## 命令下发

命令链路从 Data Center 发起，再经 RabbitMQ 路由到驱动：

```text
Client
  -> Gateway
  -> Data Center
  -> RabbitMQ command queue
  -> Driver write/read
  -> Device
```

命令下发前确认：

| 检查项 | 说明 |
|--------|------|
| 驱动在线 | 目标驱动实例已注册并监听命令队列 |
| 设备在线 | 设备状态由驱动健康检查或协议会话上报 |
| 点位可写 | 点位配置和协议能力允许写入 |
| 租户一致 | 调用方、设备、驱动和点位在同一租户上下文 |
| 参数合法 | 写入值类型、范围、单位和编码符合驱动实现 |

## 常见问题

| 现象 | 排查方向 |
|------|----------|
| 有设备但无数据 | 检查采集周期、点位属性、驱动协议日志和 RabbitMQ |
| 数据有延迟 | 检查批处理阈值、RabbitMQ 积压、Data Center 日志 |
| 命令没有到设备 | 检查命令队列、驱动服务名、设备绑定关系 |
| 写入失败 | 检查点位是否支持写、协议返回码、数据类型转换 |
| 查询返回空 | 检查租户、时间范围、设备 ID 和点位 ID |

## API 文档

OpenAPI / Swagger 的暴露方式、认证头和导出方式见 [API 文档](../development/api-documentation.md)。
