# 消息 broker 选型指南

IoT DC3 的内部异步平面（中心服务与驱动之间的点位值、命令、状态、告警、事件、通知任务）运行在 broker 中立的消息
Port 后面：应用代码在每个 broker 上完全相同，部署侧只需一个配置项加一个依赖即可选定 broker。

> 设计背景：[`docs/design/mq-abstraction.md`](./design/mq-abstraction.md)

## 如何选型 broker

1. 设置 `DC3_MQ_TYPE`（映射到 `dc3.mq.type`）——见下表。默认值为
   `rabbitmq`，因此既有部署无需任何改动。
2. 提供该 broker 的连接配置（见下表）。
3. 配置层面仅此而已——技术栈中没有其他任何部分与特定 broker 相关。注意标准 `dc3` 镜像只随附
   `rabbitmq` 适配器；运行其他 broker 还需在部署中额外加入对应的
   `dc3-mq-*` 适配器依赖（选型第 1 步仍然只是类型 + 连接配置）。

内部平面与南向设备面相互独立：更换内部 broker 绝不会影响 MQTT 驱动或基于 EMQX 的设备接入。

## 南向设备面（MQTT 南向接入）

南向设备面的 MQTT 技术栈完全不需要适配器层——MQTT 是开放的线上协议，驱动使用标准 Paho 客户端，broker 只由一个
URL（`dc3.driver.mqtt.url`）选定：EMQX、Mosquitto、HiveMQ、NanoMQ、VerneMQ …… 都可以零代码改动直接替换。
Compose 默认值（`dc3-emqx`）是一种部署选择，而非依赖；厂商特有功能（管理 API、规则引擎）不得泄漏进驱动代码，
`MqttVendorNeutralityIT` 以机械化方式持续验证这一性质——同一份客户端代码对两家不同的 broker 厂商做往返验证，
唯一差异只是 URL。

一条方言说明：南向设备面客户端使用 MQTT 3.1.1——所有主流 broker 都支持该版本，且足以覆盖设备接入
（发布/订阅、QoS、保留消息、TLS、X.509）。共享订阅等 MQTT 5 独有特性仅由上文的内部平面适配器使用。

## 已认证的 broker

下表中的适配器除 RocketMQ 外，均已在真实 broker 上通过同一套 broker 中立契约套件（`dc3-mq-tck`）。
RocketMQ 适配器为**实验性 / 尚未认证**：其契约套件需显式开启（`TCK_ROCKETMQ_NAMESRV`），并且自述为尚未认证——
在依赖它之前，请先针对你自己的 broker 运行一遍。

| Broker                                     | `DC3_MQ_TYPE` | 连接配置                                                                              | 说明                                                                                                                            |
|--------------------------------------------|---------------|---------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| RabbitMQ（默认）                           | `rabbitmq`    | `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD`（+ SSL） | 与 Port 引入之前的所有部署线上兼容；发布者确认 + returns                                                                        |
| Kafka                                      | `kafka`       | `spring.kafka.bootstrap-servers`                                                      | `acks=all`；新消费者组从 latest 起始；按 key 有序；仅明文连接——适配器不配置任何 SASL/TLS                                       |
| RocketMQ（实验性）                         | `rocketmq`    | `dc3.mq.rocketmq.name-server-address`                                                 | CLUSTERING/BROADCASTING 模式；同步发送确认；未认证——见上文                                                                     |
| Pulsar                                     | `pulsar`      | `dc3.mq.pulsar.service-url`                                                           | 共享/独占订阅；原生批量接收                                                                                                    |
| ActiveMQ（Artemis / Classic）              | `activemq`    | `dc3.mq.activemq.url`（+ user/password）                                              | JMS 2.0；共享持久订阅；定时延迟；无发布者确认——驱动发件箱(outbox)保障持久性                                                    |
| MQTT 5（EMQX / HiveMQ / NanoMQ / VerneMQ） | `mqtt`        | `dc3.mq.mqtt.host` / `dc3.mq.mqtt.port`                                               | 共享订阅（MQTT 5）；QoS 1；可让一个 broker 同时服务设备面与内部平面                                                            |

## 能力矩阵

| 能力                     | RabbitMQ           | Kafka                 | RocketMQ                           | Pulsar                      | ActiveMQ                                 | MQTT 5                |
|--------------------------|--------------------|-----------------------|------------------------------------|-----------------------------|------------------------------------------|-----------------------|
| 延迟消息                 | 回退(fallback)*    | ❌ → 本地回退(fallback)   | 回退(fallback)（按级别量化）         | 回退(fallback)（原生能力可用） | ✅ JMS 定时投递                         | ❌ → 本地回退(fallback)   |
| 原生死信队列             | DLX + 隔离区       | 适配器 `.dlq` 主题  | 适配器 `-dlq` 主题               | 适配器 `.dlq` 主题        | 适配器 `.dlq` 队列                     | 适配器 `/dlq` 主题  |
| 广播                     | ✅                 | ✅                    | ✅                                 | ✅                          | ✅                                       | ✅                    |
| 逐消息 ack               | ✅                 | offset（近似）       | ✅                                 | ✅                          | ✅                                       | ✅（QoS 1）            |
| 发布者确认               | ✅                 | ✅ (acks=all)         | ✅ 同步发送                       | ✅（message id）             | ❌ 尽力而为（驱动发件箱(outbox)兜底） | ✅ (PUBACK)           |
| 批量投递                 | ✅ 原生          | ✅ 原生             | ✅ 原生                          | ✅ 原生                   | ⚠️ 合成                           | ⚠️ 合成        |
| 按 key 有序         | ❌                 | ✅                    | ❌（key 仅用于路由） | ❌（共享订阅）             | ❌                                       | ❌                    |
| key 模式路由      | ✅ broker 绑定 | ✅ 客户端路由器 | ✅ 客户端路由器              | ✅ 客户端路由器       | ✅ 客户端路由器                    | ✅ 客户端路由器 |
| 订阅过期      | ✅ x-expires       | ❌ 文档注明不支持         | ❌ 文档注明不支持                      | ❌ 文档注明不支持               | ❌ 文档注明不支持                            | ⚠️ 会话过期     |
| 消费组离线持久性 | ✅ 持久化队列   | ✅ 日志保留      | ✅ offsets                         | ✅ 持久订阅             | ✅ 持久订阅                  | ⚠️ 取决于 broker   |
| 保留                | queue TTL          | 保留配置      | 保留                          | 保留                   | 订阅保留                   | 取决于 broker      |

\* RabbitMQ 固有的 TTL+DLX 延迟（驱动租约超时、设备扫描 tick）仍在服务端按原样工作；任意逐消息延迟则统一使用
Port 的本地调度器回退(fallback)，以保证所有 broker 行为一致。

完整论证（各 broker 的语义、回退(fallback)策略、作为社区适配器验收门槛的 TCK）见上方链接的设计文档。

## 说明

- **key 模式路由**：只有 RabbitMQ 在 broker 侧求值 `SubscriptionSpec.keyPattern`（topic 交换机绑定）。
  其余所有适配器都在每个 (topic, group) 的单个消费者前面运行一个客户端主题路由器：消息的分区 key 按 Rabbit
  topic 绑定通配符语义（`*` = 一个词，`#` = 零个或多个词，空白模式 = 全部；见 `dc3-mq-core` 中的
  `KeyMatcher`）与各监听器的模式进行匹配；同一 JVM 内多个匹配的监听器以轮询方式分发；key 在该 JVM 中
  匹配不到任何监听器时会被确认并跳过——即 Rabbit 的不可路由即丢弃语义。跨 JVM 负载均衡仍由 broker
  通过共享消费者组承担。
- **Kafka 安全**：适配器构建生产者/消费者时带 `acks=all`，但不带任何 SASL/TLS 设置——仅明文连接。
  在适配器具备安全属性桥接之前，启用了安全机制的 Kafka 集群需要用户自行提供 `KafkaTemplate`/消费者配置。
- **驱动发件箱(outbox)**：点位值持久性在每个 broker 上都由驱动的 SQLite 发件箱(outbox)负责，因此不支持
  发布者确认的 broker 依然完全安全。
- **至少一次（at-least-once）**：投递契约在每个 broker 上都是至少一次、消费方幂等；消费方不得假设
  有序性（矩阵中注明按 key 有序的场景除外）。
- **社区适配器**：针对你的 broker 通过 `dc3-mq-tck` 中的契约套件，即可认证新的适配器
  （Redis Streams、SQS 等）。
