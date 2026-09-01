# 消息中间件选型

IoT DC3 的内部异步平面只有四个受支持的 broker 适配器：RabbitMQ、Kafka、Pulsar 和 MQTT 5。业务代码只依赖
`dc3-mq-core` 的 Port；部署时通过 `dc3.mq.type` 选择一个适配器，不能同时启用多个适配器。

## 选择与配置

```yaml
dc3:
  mq:
    type: rabbitmq # rabbitmq | kafka | pulsar | mqtt
```

| 类型 | 连接配置 | TCK 状态 |
|---|---|---|
| `rabbitmq`（默认） | `spring.rabbitmq.*` | RabbitMQ Testcontainers 通过 |
| `kafka` | `dc3.mq.kafka.bootstrap-servers` | Apache Kafka 3.9 Testcontainers 通过 |
| `pulsar` | `dc3.mq.pulsar.service-url` | Apache Pulsar Testcontainers 通过 |
| `mqtt` | `dc3.mq.mqtt.host`、`dc3.mq.mqtt.port` | HiveMQ CE Testcontainers 通过 |

内部平面和南向设备接入平面相互独立。南向 MQTT 驱动仍使用 MQTT 3.1.1 客户端；内部 `mqtt` 适配器使用 MQTT 5
共享订阅，两者不得混淆。

## 统一投递契约

每条消息都经过 `Dc3ListenerProcessor` 调用业务 listener，并等待其返回的 `Publisher` 完成：

- 正常完成 → `ACK`；
- 明确的 `MqPoisonException` → `DEAD_LETTER`；
- 其他错误 → `REQUEUE`；
- Publisher 尚未完成、被取消或返回冲突 disposition → 不结算消息；
- listener 不得自行订阅或调用 broker acknowledgment。

适配器是业务 Publisher 的唯一订阅者。批量 listener 对整批返回一个 disposition；业务必须具备幂等性。
Kafka 的 requeue 通过重新发布并提交原 offset 实现，因此重启窗口内可能重复。MQTT 使用手动 PUBACK，只有
`ACK` 或死信发布成功后才发送 PUBACK。

## 能力矩阵

能力值以各适配器 `BrokerCapabilities` 的实际声明为准，不以 broker 的理论能力推断：

| 能力 | RabbitMQ | Kafka | Pulsar | MQTT 5 |
|---|---:|---:|---:|---:|
| 任意延迟消息 | 否（Port 本地调度） | 否（Port 本地调度） | 否（Port 本地调度） | 否（Port 本地调度） |
| 原生死信队列 | 是（DLX） | 否 | 否 | 否 |
| 广播订阅 | 是 | 是 | 是 | 是 |
| 逐消息确认 | 是 | 否（offset 提交） | 是 | 是（手动 PUBACK） |
| 发布者确认 | 是 | 是（`acks=all`） | 是 | 是（QoS 1 PUBACK） |
| 原生批量投递 | 是 | 是 | 是 | 否 |
| 订阅过期 | 是 | 否 | 否 | 否 |
| 顺序保证 | 无 | 按 key | 无 | 无 |

所有适配器均提供至少一次投递。跨服务消息必须携带 tenant 上下文、request id 和业务幂等键；不能依赖 broker
替业务去重。适配器的主题、队列、订阅名称是实现细节，业务只能使用 `MqTopic` 和 `SubscriptionSpec`。

## 验证

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-tck -am test
```

`dc3-mq-tck` 是发布门禁：四个适配器都必须通过共享契约，包括发布确认、Publisher completion、重投、批量和
poison 处理。没有通过 TCK 的 broker 不属于受支持范围。
