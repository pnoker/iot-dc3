# IoT DC3 消息 Port 设计

| 项目 | 决策 |
|---|---|
| 状态 | 已落地；RabbitMQ、Kafka、Pulsar、MQTT 5 四个适配器均受 TCK 门禁 |
| 范围 | center、driver、common 之间的异步消息平面 |
| 目标 | 一个业务 Port、一个明确的 completion 契约、四个可验证实现 |
| 非目标 | broker 间兼容层、同步桥接、业务代码感知具体 broker |

## 1. 第一性原理

消息消费的正确性不是“回调被调用”，而是“业务副作用完成后消息才被结算”。因此消费 API 的最小正确
抽象是 `Publisher<DeliveryDisposition>`：适配器订阅它，Publisher 完成前绝不 ACK、提交 offset 或发送
PUBACK。所有实现都遵循同一状态机：

```text
DELIVERED
  ├─ Publisher complete       -> ACK
  ├─ MqPoisonException        -> DEAD_LETTER
  ├─ other error              -> REQUEUE
  ├─ cancellation/incomplete  -> no settlement
  └─ conflicting disposition  -> adapter error, no settlement
```

业务 listener 必须返回 `Mono<Void>` 或 `Flux<?>`，不得在 listener 内部调用 `subscribe()`，也不得取得
broker 原生 acknowledgment。`Dc3ListenerProcessor` 是唯一的契约执行点，负责 MDC/request-id、异常分类和
默认 `ACK`。

### 1.1 至少一次与幂等

Port 明确提供至少一次，不承诺 exactly-once。业务写库、发通知、执行设备命令等副作用必须以 tenant +
业务 delivery key 建立幂等约束。重投发生在以下窗口：进程崩溃、Publisher 取消、网络断开、Kafka 重新发布
成功但原 offset 尚未提交。重复不是适配器 bug，而是至少一次模型的可观察结果。

### 1.2 batch 语义

批量 listener 接收 `List<WireMqDelivery>`，并对整批返回一个 disposition。默认完成才 ACK 整批；错误按
统一规则重投或死信整批。适配器不得把批量拆成多个互相矛盾的结算结果。需要单条隔离时由业务显式拆分并
设计幂等键，不能假设 broker 支持部分提交。

## 2. 模块边界

```text
dc3-mq-core
 ├─ BrokerAdapter / BrokerCapabilities
 ├─ MessageSender / ReactiveMessageSender
 ├─ Dc3Listener / MqListener / Dc3ListenerProcessor
 ├─ MqTopic / SubscriptionSpec / WireMqDelivery
 └─ DeliveryDisposition / retry policy / batch properties
       ▲
       ├─ dc3-mq-rabbitmq
       ├─ dc3-mq-kafka
       ├─ dc3-mq-pulsar
       └─ dc3-mq-mqtt
```

业务模块只能引用 `dc3-mq-core`。适配器负责物理拓扑、客户端生命周期、序列化、重投和 broker 原生确认。
启动时适配器发布 `BrokerCapabilities`，文档和测试只采用该声明，不根据理论能力推断行为。

## 3. 发送契约

`ReactiveMessageSender.sendConfirmed(WireMqMessage)` 返回 `Mono<Void>`，完成表示 broker 已确认发布；错误
表示发布未被确认。发送方必须在业务完成链中组合该 Publisher，不得 fire-and-forget。结果发布、发件箱
重发布和延迟调度均遵循同一确认语义。发送失败不能被转换为成功完成，否则消费方会永久丢失业务结果。

消息 envelope 使用 JSON body 与统一 headers：

| Header | 用途 |
|---|---|
| `dc3-type` | payload 类型 |
| `request-id` | 跨 broker trace |
| `tenant-id` | 运维过滤；业务 tenant 仍在 payload/命令中显式传递 |
| `dc3-correlation-id` | 业务关联键 |
| `dc3-partition-key` | 无原生 key 字段时的镜像 |
| `dc3-redelivery-count` | 适配器重投计数 |

## 4. 四个适配器的真实映射

| 能力 | RabbitMQ | Kafka | Pulsar | MQTT 5 |
|---|---|---|---|---|
| 任意延迟 | Port 本地调度 | Port 本地调度 | Port 本地调度 | Port 本地调度 |
| 死信 | 原生 DLX | Port 无原生队列 | Port 无原生队列 | Port 无原生队列 |
| 广播 | 原生交换机/队列 | topic + consumer group | subscription 模式 | `$share` 组 |
| 消费确认 | delivery tag | offset 提交 | message id | 手动 PUBACK |
| 发布确认 | publisher confirm | producer `acks=all` | send future | QoS 1 PUBACK |
| 批量 | 原生 consumer batch | poll batch | receive batch | 不支持 |
| 订阅过期 | queue expiry | 不支持 | 不支持 | 不支持 |
| 顺序 | 无 | 同一 key 分区内 | 无 | 无 |

### 4.1 RabbitMQ

Rabbit 适配器在 channel 上启用 publisher confirms 和 returns。业务 Publisher 完成后才确认 delivery
tag；重投使用 `basicNack(requeue=true)`，poison 分支使用死信发布成功后再拒绝原消息。连接、channel 和
consumer 的关闭都取消未完成 Publisher，不产生 ACK。

### 4.2 Kafka

Kafka 使用 `acks=all` producer 和手动 acknowledgment listener。Kafka 没有 per-message nack；`REQUEUE`
通过重新发布到同一逻辑 topic 后提交原 offset 实现，原子性边界由 broker 决定，因此业务必须幂等。消费组
offset 持久存在，不提供订阅过期语义。TCK 使用真实 Apache Kafka 验证 Publisher completion、停止重投和
重启窗口。

### 4.3 Pulsar

Pulsar 使用 persistent topic、shared/exclusive subscription 和原生 message id ack。`REQUEUE` 使用
negative acknowledgement，poison 使用适配器的死信发布路径。consumer close 会使未 ack 消息重新可见；
Publisher 未完成时不调用 ack/nack。

### 4.4 MQTT 5

MQTT 适配器使用 QoS 1、共享订阅和客户端手动 acknowledgment。只有 disposition 已完成且 ACK/死信动作成功
后才发送 PUBACK；取消或异常不发送 PUBACK，依靠 broker 重投。协议没有原生批量、延迟、DLQ 或订阅过期，
这些能力不在 Port 中伪造。

## 5. 取消、失败和毒消息

- `Mono/Flux` cancellation 是未完成事务：适配器释放客户端资源，不结算消息。
- transient failure 返回 `REQUEUE`；适配器按自身 broker 语义重投，并保留 redelivery count。
- `MqPoisonException` 只用于不可恢复的 wire contract/schema 数据；死信发布失败时仍不得 ACK 原消息。
- 同一 delivery 同时产生两个 disposition 是程序错误，抛出冲突异常并保持未结算。
- adapter stop 必须等待或取消所有 in-flight completion；不能以 stop 作为隐式成功。

## 6. 配置与生命周期

`dc3.mq.type` 只激活一个适配器。所有 adapter bean 在 `start()` 建立消费者，在 `stop()` 取消订阅并释放
客户端。连接错误通过 lifecycle Publisher 暴露，不能在后台线程吞掉。业务线程不调用 `block()`、
`boundedElastic()` 或同步等待来“适配” broker 客户端。

## 7. TCK 发布门禁

`dc3-mq-tck` 对四个适配器执行同一套契约：发布确认、单条/批量消费、key 路由、广播/负载均衡、Publisher
completion、重投、poison、取消和生命周期。真实 broker 测试必须验证“消息已投递但 Publisher 未完成，停止
consumer 后使用同 group 重建仍可重投”。任何适配器未通过完整 TCK，都不属于支持矩阵。

```bash
mvn -s .mvn/settings.xml -pl dc3-mq/dc3-mq-tck -am verify
```

## 8. 决策结果

删除无法满足上述 completion 契约的实现，而不是在 Port 中保留同步兼容分支。部署、接口和业务代码都以这套
统一异步语义为准；不存在旧 API、旧 ACK 路径或 broker 专属业务分支。
