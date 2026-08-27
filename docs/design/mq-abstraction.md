# 设计：可插拔消息中间件抽象（MQ Port）

|            |                                                                                                                                                             |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **状态**   | 已交付：Port + 五个适配器（rabbitmq / kafka / activemq / mqtt / pulsar）已通过 TCK 认证；rocketmq 适配器为实验性（TCK 可选启用，尚未认证）                  |
| **日期**   | 2026-08-17，修订于 2026-08-19（提交 `956de3dd3` 后复核；MQTT 解耦）                                                                                          |
| **范围**   | `dc3-common-*` 消息层（中心↔驱动 异步平面）                                                                                                                  |
| **目标**   | RabbitMQ（默认）、Kafka、RocketMQ、Pulsar、ActiveMQ（Artemis / Classic）、MQTT 5（EMQX / HiveMQ / NanoMQ / …）                                               |
| **相关**   | [`storage-abstraction.md`](./storage-abstraction.md) —— 关系 + 时序可插拔，同一套 profile/TCK 机制                                                            |
| **讨论**   | Phase 1 启动前开放评审                                                                                                                                       |

## 1. 摘要

IoT DC3 目前在三个业务模块中通过 Spring AMQP 原生 API（`RabbitTemplate`、`@RabbitListener`、`Channel`、
`CorrelationData`）与 RabbitMQ 交互。作为一个全球分布的开源项目，我们希望部署者——以及社区——能够把平台运行在
其环境已经标准化的任意中间件上。

本方案：引入一个薄 **Port**（`dc3-mq-core`），抽象平台实际使用的*语义*——逻辑主题、负载均衡订阅与广播订阅、
延迟投递、死信、带事务性确认的批量消费、有界重投、发布者确认——并为每个中间件提供一个适配器模块。RabbitMQ
率先落地，作为现有代码逐字节线级兼容的迁移；一套中间件中立（broker-neutral）的契约测试套件（TCK）以明确的
验收门槛让社区为其他中间件贡献适配器成为可能。其中一个适配器是 **MQTT 5**，因此内部平面也可以运行在部署环境
已在运营的任意 MQTT 中间件之上。

本设计刻意 **不** 去建模中间件拓扑（exchange、binding、分区）。它只建模每个主流中间件都能表达的两种订阅模式，
其余一切通过声明的能力进行协商，并配有文档化的回退(fallback)。

MQTT 在平台中扮演两个不同的角色，二者都必须把中间件视为自由选择：**南向设备面**（协议层面本就厂商中立）以及
——通过新适配器——**内部异步平面**本身。§7.1 划定了这一边界。

两项近期的平台变更（2026-08-18，提交 `956de3dd3`——租约 fencing 的持久化遥测）是加强而非否定本设计：驱动现在
通过中间件前置的强制性 SQLite 发件箱(outbox) 掌控持久性，且点位值摄入是事务性且幂等的（带 schema 版本的载荷，
配 `messageId` 去重与 fencing token）。两者都降低了 Port 对中间件的要求。

## 2. 现状 —— RabbitMQ 目前的使用方式

信息截至 2026-08-19 准确，在提交 `956de3dd3`（2026-08-18）之后复核。所有数字均已对照代码树核实。

**拓扑**（名称集中定义于
`dc3-common/dc3-common-constant/.../driver/RabbitConstant.java`）：

- **15 个已声明的 topic exchange**（`dc3.e.<domain>`）：state、alarm、metadata、point_command、value、mqtt、
  state_timeout_delay、state_timeout_check、command、command_result、command_dead、event（声明于
  `dc3-common-rabbitmq/.../ExchangeConfig.java`），外加 point_value_dead、point_command_dead、point_command_result
  （声明于
  `dc3-common-data/.../DataTopicConfig.java`）。其中两个是无用的累赘：`dc3.e.mqtt` 已声明但没有任何 binding、
  生产者或消费者，`RabbitConstant` 中的 `register` exchange/queue 常量声明了却无处引用——两者都是 Phase 1 的
  清理候选（§12；经 MQTT 中间件路由属于 §7.1 适配器的职责，而非定制 exchange）。
- **19 个已声明的队列定义** —— `DataTopicConfig` 中 16 个中心侧（负载均衡与死信）队列，
  `dc3-common-driver/.../DriverTopicConfig.java` 中 3 个驱动侧模式，后者按每个驱动客户端实例化。驱动侧命令队列
  携带与租约耦合的 `x-expires`（§8.8）；元数据队列为 auto-delete，TTL 30 秒。
- 路由键 `dc3.r.<domain>.<service>`；环境前缀通过 `dc3.rabbit.tag` 系统属性指定。

**生产者**（主代码中 14 处裸 `rabbitTemplate.convertAndSend` 调用点；驱动的 8 个业务发送方法汇入其中 6 处裸调用点）：

| 侧             | 是否有接口封装？                                         | 调用点                                                                                                                           |
|----------------|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| 驱动           | 是 —— `DriverSenderService` → `DriverSenderServiceImpl` | 8 个发送（6 处裸调用点）+ `BufferServiceImpl` 发件箱(outbox) 重发布                                                              |
| 数据中心       | 否 —— 直接使用 `RabbitTemplate`                         | `CommandHistoryServiceImpl`、`PointCommandServiceImpl`、`DriverStateServiceImpl`、`NotifyTaskSender`、`EntityStateExpiryScanner` |
| 管理中心       | 否 —— 直接使用 `RabbitTemplate`                         | `MetadataEventListener`                                                                                                          |

**消费者** —— 16 个 `@RabbitListener` 方法，全部通过 `Channel` + `RabbitAckUtil` 手动确认，分布于三个容器工厂
（低延迟调优的默认工厂、高吞吐工厂，以及——自 `956de3dd3` 起——点位值路径启用批量的
`pointValueRabbitListenerContainerFactory`）：13 个位于 `dc3-common-data`
（`data/rabbit/*` 接收器、`NotifyWorker`、`EntityStateExpiryScanner`），3 个位于
`dc3-common-driver`（`receiver/rabbit/*`）。点位值 **死信** 队列刻意没有监听器：自
`956de3dd3` 起它是无消费者的隔离区（§8.2）。

**模块耦合** —— `dc3-common-data`、`dc3-common-driver`、`dc3-common-manager` 与
`dc3-common-facade-local-manager` 直接依赖 `dc3-common-rabbitmq`，因此 Spring AMQP API 泄漏进每个
业务模块。另一个并行模块 `dc3-common-mqtt`
（Spring Integration MQTT + Eclipse Paho v3 客户端）承载驱动侧的 MQTT 南向设备面；只有
`dc3-driver-mqtt` 依赖它，且其中不含任何中间件厂商代码——EMQX 是部署默认值，不是代码
依赖（§7.1）。

**抽象必须承载的八项 RabbitMQ 特有语义**（难点所在——普通的收发很容易）：

| # | 语义                                       | 当前实现                                                                                                                                                                                                                                                                                                                                                         |
|---|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | 延迟消息                                   | TTL + DLX：45 秒驱动超时检查、10 秒设备扫描 tick（`DriverStateServiceImpl`、`EntityStateExpiryScanner`，delay/check exchange 见 `DataTopicConfig`）                                                                                                                                                                                                              |
| 2 | 每实例广播                                 | 驱动元数据同步，通过按驱动客户端命名的 auto-delete 队列（TTL 30 秒）（`DriverTopicConfig`）                                                                                                                                                                                                                                                                     |
| 3 | 死信队列                                   | point_value（活队列 TTL 7 天 → DLX；死队列自 `956de3dd3` 起为无消费者的隔离区）、point_command、command DLX + 专用死信接收器（`PointCommandDeadReceiver`、`CommandDeadReceiver`）                                                                                                                                                                                |
| 4 | 手动确认 + 有界背压                        | 到处手动确认；点位值路径仅在 PostgreSQL 事务提交后才确认整个中间件批次（`PointValueReceiver` + `PointValueRabbitConfig`）；背压以 prefetch ≥ batchSize 为界，中间件充当持久缓冲（旧的数据侧摄入缓冲及其缓冲满时的 `nack(requeue=true)` 已在 `956de3dd3` 移除）                                     |
| 5 | 持久发件箱(outbox) + 发布者确认            | 驱动在发布前把每个点位值持久化到 **强制性 SQLite 发件箱(outbox)**（WAL、`synchronous=FULL`）；确认 future 与重试调度器负责删除或重发布（`BufferServiceImpl`、`PointValueBuffer`）                                                                                                                                                                                |
| 6 | 队列 TTL 作为保留守卫                      | state/alarm 30 秒、event/command_result 60 秒、notify_task 24 小时、point_value 7 天、驱动侧命令队列 30 秒（`DataTopicConfig`、`DriverTopicConfig`）                                                                                                                                                                                                             |
| 7 | 批量消费                                   | 点位值路径上的消费者侧批量：批量监听器 + 消费者批量，批次级有界退避重试，耗尽后将整个批次拒绝进 DLX（`PointValueRabbitConfig`）                                                                                                                                                                                                                                  |
| 8 | 与租约耦合的每实例队列生命周期             | 驱动命令队列随驱动租约过期（`x-expires` 取自 `driver.lease.queue-expires`）；元数据广播队列为 auto-delete，TTL 30 秒（`DriverTopicConfig`）                                                                                                                                                                                                                      |

两个承重事实使整个抽象可行：

- **延迟超时路径已经把数据库租约当作事实来源**
  （`DriverTimeoutCheckReceiver` 中的 `leaseVersion` 幂等检查）。MQ 延迟只是一个 *触发器*，因此不具备原生延迟投递的
  中间件可以回退(fallback) 到本地调度器而不破坏正确性。
- **至少一次 + 幂等消费者现在已实现，而非假设**：点位值载荷携带 schema-v1
  信封（`schemaVersion`、`messageId`、`sequence`、
  `fencingToken`、`driverNode`）并在摄入时校验，数据中心摄入是事务性且幂等的
  （`956de3dd3`）。

已经存在且必须在抽象中原样保留的横切关注点：
JSON 载荷约定（Jackson）——在点位值路径上由 schema-v1 信封固化，`X-Request-Id` 的 MDC 跨中间件跳传播
（`MdcRequestIdMessagePostProcessor` / `MdcRequestIdListenerAdvice`），持久化投递，租户作用域载荷。

## 3. 目标 / 非目标

**目标**

- 业务模块（`dc3-common-data`、`dc3-common-driver`、`dc3-common-manager`）面向中间件中立 API 编译，编译类路径上
  **零** 中间件类。
- 每个中间件一个适配器模块；运行时恰好激活一个适配器，由
  `dc3.mq.type` 选择（默认 `rabbitmq`）。
- MQTT 中间件在其两个角色中都是自由的部署选择：南向设备面在协议层面保持厂商中立（EMQX 是 compose 默认值，
  不是代码依赖），内部平面可选地通过
  `dc3-mq-mqtt` 适配器运行在任意 MQTT 5 中间件之上（§7.1）。
- 现有 RabbitMQ 部署在迁移期间持续可用：物理 exchange/queue/路由键名称逐字节一致，不强制一次性重新部署全部实例。
- 点位值路径的吞吐语义在抽象之后保持不变：批量投递、提交后批次确认、有界重投。
- 第三方驱动作者不受影响：`DriverSenderService` 签名不变。
- 社区贡献的适配器有一个机械化的验收门槛（TCK）。

**非目标**

- 恰好一次投递或跨中间件分布式事务。契约是 **至少一次 + 幂等
  消费者**——在点位值路径上正式实现（messageId 去重、fencing token），其余路径按约定要求。
- 多中间件拓扑（把 RabbitMQ 桥接到 Kafka 等）。
- 重写南向设备面的 MQTT 平面（`dc3-common-mqtt`、`dc3-driver-mqtt`）——它们是协议驱动，不是
  内部异步平面。§7.1 改为记录边界与厂商中立护栏。
- 隐藏中间件之间的性能/顺序差异（文档化，而非抹平）。

## 4. 设计原则

1. **抽象语义，而非中间件特性。** 统一层次是 *逻辑主题 + 订阅模式*。
   exchange/binding/分区/tag 模型属于适配器内部。两种订阅模式（`LOAD_BALANCE`、`BROADCAST`）
   是全部五个目标中间件都能表达的最低公共语义。
2. **能力协商与优雅降级，而非最低公共分母。**
   适配器声明自己支持什么；核心应用文档化的回退(fallback)（如延迟消息 → 本地调度器），
   并在启动时记录协商结果。我们绝不把 API 降到 JMS 能做到的水平。
3. **单一信封格式。** 载荷始终是 JSON 字节；类型信息与追踪上下文搭载在标准化 header
   （`dc3-type`、`X-Request-Id`、`tenant-id`）中，且载荷级 schema 版本化已在点位值路径上生产使用
   （schema-v1 信封）。序列化位于 API 层——不依赖中间件侧的类型转换器（今天的 `__TypeId__` header 是 Spring AMQP
   内部机制）。这保证线级格式在各中间件间完全一致。

## 5. 模块布局

镜像现有的 facade 模块组织方式（契约模块 + 每种传输一个实现）：

```
dc3-mq/                          # top-level aggregator: the broker-selection family
├── dc3-mq-core/                 # Port: interfaces, message model, envelope, capability
│                                #      negotiation, @Dc3Listener processing, fallback
│                                #      scheduler. Zero broker dependencies.
├── dc3-mq-rabbitmq/             # Adapter: RabbitConfig/ExchangeConfig/DataTopicConfig/
│                                #         DriverTopicConfig/PointValueRabbitConfig moved
│                                #         here unchanged
├── dc3-mq-kafka/                # Adapter: spring-kafka
├── dc3-mq-rocketmq/             # Adapter: rocketmq classic client
├── dc3-mq-pulsar/               # Adapter: pulsar client
├── dc3-mq-activemq/             # Adapter: JMS 2.0 (covers Artemis and Classic)
├── dc3-mq-mqtt/                 # Adapter: MQTT 5 client (EMQX / HiveMQ / NanoMQ / …; §7.1)
└── dc3-mq-tck/                  # Broker-neutral contract test suite (Testcontainers)
```

（2026-08-20 重组：从 `dc3-common` 迁出，成为顶层 `dc3-mq` 聚合模块——该家族是部署
选择，不是共享基础设施；Java 包名保持
`io.github.pnoker.common.mq.*`。）

迁移后的依赖方向：

```
dc3-common-data ─┐
dc3-common-driver ─┼─► dc3-mq-core             (compile-time)
dc3-common-manager ─┘         ▲
                              │ (runtime, exactly one)
        dc3-mq-rabbitmq / -kafka / -rocketmq / -pulsar / -activemq / -mqtt
```

适配器由 `dc3.mq.type` 选择，并通过
`@ConditionalOnProperty(prefix = "dc3.mq", name = "type", havingValue = "...")` 激活——与 `dc3.facade.mode` 为
facade 传输使用的模式相同。

## 6. 核心 API

```java
package io.github.pnoker.common.mq;

/** Logical destinations. Business code references these only. */
public enum MqTopic {
    STATE, ALARM, METADATA, POINT_COMMAND, POINT_VALUE, EVENT,
    COMMAND, COMMAND_RESULT, POINT_COMMAND_RESULT, NOTIFY_TASK
    // + logical dead-letter variants declared internally (see §8.2)
}

/** Outbound message. Envelope + JSON body, broker-agnostic. */
public final class MqMessage {
    private final MqTopic topic;
    private final String partitionKey;          // was routing-key suffix: driver service / deviceId
    private final byte[] payload;               // JSON, serialized by the API layer
    private final Map<String, String> headers;  // dc3-type, X-Request-Id, tenant-id, ...
    private final Duration delay;               // Duration.ZERO = immediate
    // builder; delay respected only if capabilities.delayedMessage()
}

public interface MessageSender {
    /** Sync send; on confirming brokers returns after the broker accepted. */
    void send(MqMessage message);

    /** Async send with unified publisher-confirm callback. */
    void sendAsync(MqMessage message, SendConfirmation confirmation);
}

public interface SendConfirmation {
    /** confirmed=false carries a retryable flag; the driver outbox keys off this. */
    void onConfirm(MqMessage message, boolean confirmed, Throwable cause);
}

public interface MqListener<T> {
    void onMessage(MqReceived<T> message, Acknowledgment ack);
}

/** Batch variant — the highest-throughput path (point values). */
public interface MqBatchListener<T> {
    /**
     * The batch is the unit of delivery AND of acknowledgement: ack() commits every
     * message in the batch; reject() requeues or dead-letters the batch as a whole.
     */
    void onBatch(List<MqReceived<T>> messages, Acknowledgment ack);
}

public final class MqReceived<T> {
    private final T payload;                    // deserialized via dc3-type header
    private final Map<String, String> headers;  // X-Request-Id restored into MDC by core
    private final int redeliveryCount;          // best-effort per broker
}

public interface Acknowledgment {
    void ack();
    /** requeue=true → retry later; requeue=false → route to the topic's dead-letter. */
    void reject(boolean requeue);
}

/** Subscription declaration — replaces @RabbitListener + container-factory choice. */
public record SubscriptionSpec(
    MqTopic topic,
    SubscriptionMode mode,            // LOAD_BALANCE | BROADCAST
    ConsumptionProfile profile,       // LATENCY | THROUGHPUT (concurrency/prefetch defaults)
    DeliveryMode delivery,            // SINGLE | BATCH
    String keyPattern,                // key filter; blank = topic default
    String group,                     // consumer group; default derived from service name
    Duration instanceTtl,             // per-instance queue/subscription expiry (drivers; §8.8)
    Class<?> payloadType,
    boolean deadLetterEnabled
) {}

/** SPI implemented by each adapter module. */
public interface BrokerAdapter {
    String type();                                    // "rabbitmq" | "kafka" | ...
    MessageSender sender();
    void subscribe(SubscriptionSpec spec, MqListener<?> listener);
    void subscribe(SubscriptionSpec spec, MqBatchListener<?> listener);
    BrokerCapabilities capabilities();
}

public record BrokerCapabilities(
    boolean delayedMessage,         // native delayed/timed delivery
    boolean deadLetterQueue,        // broker-level DLQ (vs. explicit dead-letter topic)
    boolean broadcastSubscription,  // per-instance subscription
    boolean perMessageAck,
    boolean publisherConfirm,
    boolean batchDelivery,          // native batch fetch (vs. adapter-synthesized)
    boolean subscriptionExpiry,     // idle per-instance subscription auto-expiry
    OrderingGuarantee ordering      // NONE | PER_KEY
) {}
```

迁移后的业务侧形态——单消息接收器变为：

```java
@Dc3Listener(topic = MqTopic.COMMAND, mode = LOAD_BALANCE, profile = LATENCY)
public void commandReceive(MqReceived<CommandBO> message, Acknowledgment ack) { ... }
```

批量点位值接收器保持其吞吐语义：

```java
@Dc3Listener(topic = MqTopic.POINT_VALUE, mode = LOAD_BALANCE, profile = THROUGHPUT, delivery = BATCH)
public void pointValueReceive(List<MqReceived<PointValueBO>> messages, Acknowledgment ack) { ... }
```

batchSize、prefetch 与重试上界从配置绑定（今天是 `BatchConsumerProperties`）而非注解字面量，
以便运维按部署调优。

`Channel`、`Message`、投递标签与 `RabbitAckUtil` 从业务代码中消失。The
`@Dc3Listener` 注解由 `dc3-mq-core` 核心处理，它向激活的适配器注册订阅——
镜像今天 `@RabbitListener` 的处理方式，只是去掉了中间件 API。

批次内的毒消息处理是批次粒度的——与今天的行为一致：一条无效消息导致整个
批次重试然后死信（§8.7）。Port 最终是否应提供按消息提取是开放
问题 §13.7。

## 7. 订阅模式与目的地映射

两种订阅模式是核心建模决策：

- **LOAD_BALANCE** —— 竞争消费者，每条消息在全平台只被处理一次。用于点位值、状态、告警、
  命令、事件、通知任务。
- **BROADCAST** —— 每个运行实例都获得自己的副本。用于驱动元数据同步（每个驱动进程必须
  刷新本地缓存）与设备扫描 tick 扇出。

各中间件的物理映射（namespace = 今天的 `dc3.rabbit.tag` 环境前缀）：

| 语义                | RabbitMQ（适配器保持现有名称）                            | Kafka                                          | RocketMQ                                      | Pulsar                               | ActiveMQ (JMS)                                     | MQTT 5                                               |
|---------------------|-----------------------------------------------------------|------------------------------------------------|-----------------------------------------------|--------------------------------------|----------------------------------------------------|------------------------------------------------------|
| LOAD_BALANCE        | 绑定 `rk.*` 的共享持久队列                                | topic `dc3.<topic>` 上的消费者组               | CLUSTERING 消费模式                           | 共享订阅 `dc3-<topic>`               | JMS Queue                                          | 共享订阅 `$share/<group>/dc3/<topic>`                |
| BROADCAST           | 每实例 auto-delete 队列（现有设计）                       | 每实例 `groupId = group + instanceId`          | BROADCASTING 消费模式                         | 每实例独占订阅                       | JMS Topic，非共享持久订阅                          | 普通订阅，每实例持久会话                             |
| 分区键              | 路由键后缀 `.<service>`                                   | record key → 分区                              | 消息 key（仅路由，不保证顺序）                | key（仅路由，共享订阅）              | `dc3-partition-key` 属性                           | `dc3-partition-key` 用户属性                         |
| Key 模式路由        | 中间件 topic binding                                      | 客户端侧路由器（`KeyMatcher`/`KeyRoutes`）     | 客户端侧路由器                                | 客户端侧路由器                       | 客户端侧路由器                                     | 客户端侧路由器                                       |
| 批量                | 消费者批量（`setBatchListener`，prefetch ≥ batchSize）    | poll 循环（`max.poll.records`）                | 批量消费（`consumeMessageBatchSize`）         | 批量接收 API                         | 适配器在短窗口内排空（合成）                       | 适配器在短窗口内排空（合成）                         |
| 实例过期            | `x-expires` / auto-delete + TTL（§8.8）                   | ❌ offset 持久存在 —— 文档化清理策略           | 订阅组配置                                    | 订阅过期策略                         | 无直接等价物 —— 文档化清理策略                     | 会话过期间隔 ⚠️（§13.8）                             |
| 命名空间            | 名称前缀（今天）                                          | topic 前缀                                     | namespace                                     | Pulsar tenant/namespace（原生契合）  | destination 前缀                                   | topic 前缀（`dc3/<topic>`，斜杠分隔）                |

顺序说明（文档化，而非隐藏）：RabbitMQ 今天不提供任何顺序保证；Kafka 以
`partitionKey = driver service` 提供每驱动顺序——严格强于当前语义，且它对
点位值摄入顺序很重要。

### 7.1 MQTT 边界 —— 两平面一原则

MQTT 在平台中以两个不同角色出现。二者都必须把 MQTT 中间件视为自由、可替换的选择——
与本设计其余部分遵循同一原则：

1. **南向设备面（southbound）** —— `dc3-common-mqtt` + `dc3-driver-mqtt`。这是协议接入，不是
   内部异步平面，且它在构造上已厂商中立：标准 Eclipse Paho v3 客户端（MQTT
   3.1.1），完全通过 `dc3.driver.mqtt.*`（URL、认证、TLS 材料、topic 前缀）配置。EMQX 是
   compose 捆绑的默认值（`docker-compose-optional.yml`），仅此而已——Mosquitto、HiveMQ、NanoMQ 或 VerneMQ 仅凭
   配置即可替换。保持这一性质的护栏：Java 模块中不含任何中间件厂商管理 API、规则引擎或
   插件专属代码；厂商细节只存在于部署配置中。客户端库锁定的是
   *协议版本*（今天是 3.1.1），不是厂商——升级到 MQTT 5 客户端是一次独立的、可选的库
   变更。
2. **内部异步平面（可选中间件）** —— `dc3-mq-mqtt` 适配器。MQTT 5 共享订阅表达
   LOAD_BALANCE；每实例普通订阅表达 BROADCAST；QoS 1 提供逐消息确认与 PUBACK
   确认（§8.4）。这让部署得以整合：一个 MQTT 中间件同时服务两个平面，或彻底去掉
   RabbitMQ 的更小技术栈。能力缺口通过标准机制回退(fallback)（延迟 → 本地调度器、DLQ → 显式
   topic、批量 → 合成窗口），而 TCK——包括全体实例宕机时的组持久性（§11 用例
    13）——是验收门槛，因为共享订阅的保留语义因中间件而异（§13.8）。

两个平面保持独立：内部平面改跑 MQTT 对南向设备面的驱动毫无影响，替换南向设备面的中间件对内部平面也毫无影响。

## 8. 逐中间件梳理的硬语义

### 8.1 延迟消息

| 中间件   | 机制                                                                   |
|----------|------------------------------------------------------------------------|
| RabbitMQ | 现有 TTL + DLX 模式（不变）；延迟消息插件可选                          |
| RocketMQ | 延迟级别 / 任意定时（5.x）                                             |
| Pulsar   | 延迟投递 API                                                           |
| ActiveMQ | JMS 定时投递（`AMQ_SCHEDULED_DELAY`）                                  |
| Kafka    | **无** → API 层回退(fallback)：本地 `ScheduledExecutor` 重发           |
| MQTT     | **无** → 同样的本地调度器回退(fallback)                                |

回退(fallback) 安全性：延迟消息的消费者（`DriverTimeoutCheckReceiver`、设备扫描）已经针对数据库租约
（`leaseVersion`）幂等。多次或迟到触发无害。回退(fallback) 运行在发送该消息的实例中；启动日志输出
`delayedMessage=false → local scheduler fallback active`。

### 8.2 死信队列

`reject(false)` 是唯一入口。适配器把它映射为：

| 中间件   | 机制                                                                    |
|----------|-------------------------------------------------------------------------|
| RabbitMQ | 现有 DLX 接线（point_value / point_command / command）                  |
| Kafka    | 重试耗尽时由适配器写入显式 `dc3.<topic>.dlq` topic                      |
| RocketMQ | 内建 `%RETRY%group` / `%DLQ%group`，以逻辑死信形式呈现                  |
| Pulsar   | DLQ 策略 + `maxRedeliverCount`                                          |
| ActiveMQ | JMS 重投策略 + DLQ                                                      |
| MQTT     | 重试耗尽时由适配器写入显式 `dc3/<topic>.dlq` topic                      |

专用死信接收器（`CommandDeadReceiver`、`PointCommandDeadReceiver`）针对逻辑死信主题重新声明订阅。点位值死信队列自
`956de3dd3` 起 **设计上就没有消费者**——它是毒批次与 7 天未消费值的隔离区；Port 把它建模为没有订阅的死信主题。
其保留策略是开放问题（§13.6）。 **活** 点位值队列上的 7 天 TTL 变为主题
保留策略属性——见 §8.6。

### 8.3 确认、重试与背压

| 操作                                 | RabbitMQ                                                   | Kafka                             | RocketMQ                    | Pulsar                    | ActiveMQ              | MQTT 5                                        |
|--------------------------------------|------------------------------------------------------------|-----------------------------------|-----------------------------|---------------------------|-----------------------|-----------------------------------------------|
| `ack()`                              | basicAck（单条或批量 multiple）                            | offset 提交（批量）               | CONSUME_SUCCESS             | ack                       | acknowledge           | PUBACK（QoS 1）                               |
| `reject(true)`                       | basicNack 重新入队                                         | seek 回退，不提交                 | RECONSUME_LATER             | negative ack / 重投       | rollback              | 适配器级重试循环（中间件没有 nack）           |
| `reject(false)`                      | basicReject → DLX                                          | 写入 `.dlq` topic                 | 内建 retry→DLQ              | DLQ 策略                  | 重投策略 DLQ          | 发布到 `.dlq` topic                           |
| 有界重试（批量重试配置）             | 无状态重试 advice + recoverer（今天的批量工厂）            | 内存重试循环，然后 `.dlq`         | `%RETRY%group`（原生契合）  | 重投退避 + DLQ            | 重投策略              | 适配器内存循环                                |
| 背压                                 | prefetch ≥ batchSize，中间件充当持久缓冲                   | 消费者 `pause()`/`resume()`       | 挂起当前队列                | 流控 / 队列大小           | 会话恢复              | receive maximum（MQTT 5 流控）                |

有界重试这一行很关键：自 `956de3dd3` 起，点位值工厂用带指数退避的无状态重试 advice 包装投递，
耗尽后把整个批次拒绝进 DLX。共享的批量消费者重试配置
（`BatchConsumerProperties`，从 `dc3.data.point.batch.*` 绑定）在所有适配器上驱动这一上界，而不是
各适配器各自即兴发挥——Kafka 需要内存循环，RocketMQ 则从 `%RETRY%group` 原生获得。

旧的缓冲满 `nack(requeue=true)` 模式已从代码库中移除（数据侧摄入缓冲已删除；
`NotifyWorker` 刻意把失败按 FAILED 确认而不是重新入队）。背压现在是“中间件充当持久
缓冲 + 有界 prefetch”，能干净地映射到每个目标中间件。

面向消费者的契约文档：**至少一次**，可能重投，要求幂等。`MqReceived` 上的
`redeliveryCount` 是尽力而为（Kafka 经重试 topic 计数、RocketMQ reconsume 次数、Pulsar 重投计数为精确值；
其余为近似值）。

### 8.4 发布者确认与驱动发件箱(outbox)

自 `956de3dd3` 起驱动已掌控持久性：每个点位值都在 `convertAndSend` *之前* 持久化到 **强制性 SQLite 发件箱(outbox)**
（WAL、`synchronous=FULL`、启动校验、缺配置即快速失败），并且
`BufferServiceImpl` +
`PointValueBuffer` 对待处理行运行先认领后发布的重试调度器。

迁移后，发件箱(outbox) 本身仍留在驱动侧且中间件中立——只有最后的
`convertAndSend` + `CorrelationData`-future 管线换成 `sendAsync` +
`SendConfirmation`。中间件映射：RabbitMQ 发布者确认；Kafka `acks=all` future；RocketMQ 发送结果；Pulsar
发送 future；MQTT QoS 1 PUBACK（逐消息确认——非 RabbitMQ 阵营中最强的）；**JMS 没有确认**
→ 尽力而为（发后即忘，失败时从发件箱(outbox) 重发布），在能力矩阵中标注。

值得明确指出的结论：**发布者确认不再是持久性的承重机制**。因为发件箱(outbox)
负责持久化，`publisherConfirm=false` 的中间件完全可接受——确认变成延迟/重发布频率的
优化，而非正确性要求。因此 ActiveMQ 适配器是一等公民，不是降级
模式。

### 8.5 信封、链路追踪与租户上下文

- `dc3-type` header 携带载荷类名；由 API 层反序列化——取代 Spring AMQP 的 `__TypeId__`。
- 载荷级 schema 版本化已在点位值路径上生产使用（schema-v1：`schemaVersion`、
  `messageId`、`sequence`、`fencingToken`、`driverNode`，在 `PointValueReceiver` 中校验）；Port 采用同一
  约定——schema 字段放在载荷中，传输提示放在 header 中。
- `X-Request-Id` 传播（当前为 `MdcRequestIdMessagePostProcessor` /
  `MdcRequestIdListenerAdvice`）移入 `dc3-mq-core` 核心；所有中间件都支持字符串 header。
- 租户 id 留在载荷内（并镜像为一个 header 便于运维过滤）；租户作用域规则不受中间件选择影响。

### 8.6 队列级消息 TTL 作为保留策略

Port 必须建模的一种 RabbitMQ 特有用法：队列携带每队列消息 TTL 作为保留/过期守卫——
state/alarm 30 秒（过期的生命周期事件毫无价值）、event/command_result 60 秒、notify_task 24 小时（出站通道卡死时
的失控积压守卫）、point_value 7 天（随后死信，§8.2）、驱动侧命令队列 30 秒。

这变成主题注册表上的一个 `retention` 属性，各中间件语义文档化而非完全一致：
RabbitMQ TTL 从队头逐消息丢弃；Kafka/RocketMQ/Pulsar 的保留是基于时间的日志保留
（丢弃整个旧段——实际上等同于“不永久保存”的保证）；JMS TTL 与 MQTT 5 消息过期是
逐消息的。区别只在精确过期时点上才有影响，代码库中没有任何消费者依赖它（过期的
状态事件直接丢弃，数据库仍是事实来源）。

### 8.7 批量消费与事务性确认

点位值路径——平台吞吐量最大的流——自 `956de3dd3` 起运行在消费者侧批量之上：

- `PointValueRabbitConfig` 声明第三个容器工厂：批量监听器 + 消费者批量、`prefetch ≥ batchSize`、
  带退避的有界重试、耗尽 → 整批进入点位值 DLX。
- `PointValueReceiver` 接收 `List<Message>`，逐消息校验 schema-v1 信封，在 **单个 PostgreSQL 事务** 中持久化历史 +
  最新值投影，然后才 `basicAck(lastTag, multiple=true)`——从消费者视角看，确认与提交是
  原子的。

| 中间件   | 批量机制                                                                                                                           |
|----------|------------------------------------------------------------------------------------------------------------------------------------|
| RabbitMQ | 消费者批量（`setBatchListener` + `setConsumerBatchEnabled`），现有设计                                                             |
| Kafka    | 带 `max.poll.records` 的 poll 循环；处理器返回后提交 offset                                                                        |
| RocketMQ | 批量消费（`consumeMessageBatchSize`）                                                                                              |
| Pulsar   | 批量接收 API                                                                                                                       |
| ActiveMQ | 无原生消费者批量 → 适配器在短窗口内排空可用消息（合成，能力 `batchDelivery=false`）                                                |
| MQTT     | 无原生消费者批量 → 同样的适配器侧窗口（合成，`batchDelivery=false`）                                                               |

Port 模型：`DeliveryMode.BATCH` + `MqBatchListener` + 共享批量重试配置。TCK 验证批量处理后的 `ack()`
会提交批内每条消息，且重试耗尽走死信而非丢弃
（§11）。

已知取舍（文档化，而非隐藏）：今天一条毒消息会导致 **整个批次** 重试并死信。
批次级粒度是 Spring 默认行为，也让跨中间件的叙事保持简单；按消息提取是开放
问题 §13.7。

### 8.8 每实例队列生命周期（租约耦合的过期）

驱动侧订阅是每实例的，且不得比其驱动存活更久：

- command/point-command 队列携带 `x-expires = driver.lease.queue-expires`——失效驱动的队列随其租约消失，
  而不是默默堆积命令、在重启后投递给一个过期实例；
- 元数据广播队列是 auto-delete，消息 TTL 30 秒，限制失效实例造成的陈旧。

Port 模型：`SubscriptionSpec` 上的 `instanceTtl` + 能力 `subscriptionExpiry`。RabbitMQ 原生映射到 `x-expires`
/auto-delete；RocketMQ/Pulsar 有订阅级等价物；Kafka offset 直接持久存在（过期组只是观感问题——通过管理工具
文档化清理策略）；JMS 没有直接等价物（文档化清理策略）；
MQTT 5 映射到持久会话的会话过期间隔。两种回退(fallback) 都不影响正确性，因为
命令发送方在分发前已经校验驱动租约/所有权。

## 9. 能力矩阵（按适配器发布）

实现状态（2026-08-25 修订）：五个适配器——rabbitmq、kafka、activemq（Artemis）、mqtt 5 与 pulsar——
已实现并通过 TCK 对照真实中间件完成认证。**rocketmq 适配器为实验性且尚未认证**：其契约套件为可选启用
（`TCK_ROCKETMQ_NAMESRV`），且自述为尚未认证，因此它面向评估发布，不是生产选择。“已认证”列反映的是已实现的行为
（例如 rabbit 经 Port 回退(fallback) 延迟任意消息，rocketmq 延迟级别会量化；rocketmq classic 客户端对全新消费者组
会无视 consumeFromWhere 重放 topic 积压，因此该适配器把新组播种到最新 offset，并在订阅时预热尚未创建的
topic——预热探针携带业务监听器永远看不到的标记属性；pulsar 订阅原生从最新位置开始，无需播种即符合新队列语义）。

| 能力                     | RabbitMQ ✅           | Kafka ✅              | ActiveMQ ✅                             | MQTT 5 ✅                   | RocketMQ ⚠️ 实验性                 | Pulsar ✅             |
|--------------------------|-----------------------|-----------------------|-----------------------------------------|-----------------------------|------------------------------------|-----------------------|
| 延迟消息                 | 回退(fallback)*       | ❌ → 本地回退(fallback) | ✅ JMS 定时                             | ❌ → 本地回退(fallback)     | 回退(fallback)（级别量化）         | ✅ 原生               |
| 原生 DLQ                 | DLX + 隔离区          | 适配器 `.dlq` topic   | 适配器 `.dlq` 队列                      | 适配器 `/dlq` topic         | 适配器 `-dlq` topic                | 适配器 `.dlq` topic   |
| 广播                     | ✅ 每实例队列         | ✅（实例组）          | ✅ topic 消费者                         | ✅ 普通过滤                 | ✅ BROADCASTING                    | ✅                    |
| 逐消息确认               | ✅                    | offset（近似）        | ✅ 客户端确认                           | ✅ QoS 1                    | ✅                                 | ✅                    |
| 发布者确认               | ✅ confirms           | ✅（acks=all）        | ❌ 尽力而为（发件箱(outbox) 兜底，§8.4） | ✅（PUBACK）                | ✅ 同步发送                        | ✅                    |
| 批量投递                 | ✅ 原生               | ✅ 原生               | ⚠️ 合成                                 | ⚠️ 合成                     | ✅ 消费者批量                      | ✅ 批量接收           |
| 按 key 顺序              | ❌                    | ✅                    | ❌                                      | ❌                          | ❌（key 仅用于路由）               | ❌（共享订阅）        |
| Key 模式路由             | ✅ 中间件 binding     | ✅ 客户端侧路由器     | ✅ 客户端侧路由器                       | ✅ 客户端侧路由器           | ✅ 客户端侧路由器                  | ✅ 客户端侧路由器     |
| 订阅过期                 | ✅ x-expires          | ❌ 文档化             | ❌ 文档化                               | ⚠️ 会话过期                 | ❌ 文档化                          | ❌ 文档化             |
| 组离线持久性             | ✅ 持久队列           | ✅ 日志保留           | ✅ 持久订阅                             | ⚠️ 取决于中间件（§13.8）    | ✅ offset                          | ✅                    |
| 保留                     | 队列 TTL              | 保留配置              | 订阅保留                                | 取决于中间件                | 保留                               | 保留/TTL              |

\* rabbit 固有的 TTL+DLX 延迟（STATE_TIMEOUT / DEVICE_SCAN）照旧在服务端生效；任意逐消息
延迟使用 Port 回退(fallback)（能力为 false）。

这张表是面向用户的文档（“我该选哪个中间件？”），启动协商日志按部署对它做摘要。

## 10. 已考虑的备选方案

**Spring Cloud Stream** —— 免费提供官方 RabbitMQ/Kafka binder，但是：RocketMQ binder 由
Alibaba 维护，Pulsar binder 社区覆盖薄弱，ActiveMQ 没有现代 binder；且其函数式
编程模型与我们按驱动的动态订阅、TTL+DLX 延迟、手动确认背压、批量确认与
确认-发件箱(outbox) 模式相冲突。作为 Port 层被否决——不过单个适配器以后可以在内部基于 SCStream
构建；Port 是我们掌控的契约。

**以 JMS 2.0 作为核心 API** —— 否决。最低公共分母：没有延迟投递标准、DLQ 语义弱、没有
发布者确认、广播/组语义映射不佳、没有批量消费。JMS 仅保留为 ActiveMQ 适配器的实现
技术。

**以 Apache Camel 作为 Port** —— 否决。把集成框架当作契约层颠倒了依赖（平台的核心消息建立在路由 DSL
之上），给每个服务拖入庞大的依赖图，而且 DC3 特有语义（批量提交后确认、租约耦合的订阅过期）仍需要在某处建模。
适配器保持纯客户端库。

**什么都不做 / 仅支持 RabbitMQ** —— 对一个全球分布的项目而言被否决：Kafka 与 RocketMQ 是许多企业环境的默认选择
（RocketMQ 尤其在国内生态），“自带中间件”是社区在本地化集成中反复提出的诉求。

## 11. TCK —— 社区扩展机制

`dc3-mq-tck` 包含一套中间件中立的契约套件，通过 Testcontainers 对每个适配器执行（rabbitmq、
kafka、pulsar、artemis、一个 MQTT 5 中间件——EMQX 或 NanoMQ；rocketmq 套件通过
`TCK_ROCKETMQ_NAMESRV` 可选启用，尚未认证）：

1. send → receive（往返、信封保真、header、`dc3-type` 反序列化）
2. LOAD_BALANCE：跨 2 个实例时每条消息恰好被一个消费者接收
3. BROADCAST：两个实例都接收每条消息
4. delay：截止时间之前不投递，之后投递
5. `reject(false)` → 死信主题收到消息
6. `reject(true)` → 观察到重投（至少一次）
7. `sendAsync` 确认以正确结果触发
8. requestId header 在跳转后仍存在（MDC 恢复）
9. 背压：被拒绝/经完整路径的消息不丢失
10. BATCH 投递：批量回调收到 ≥ 1 条消息；`ack()` 提交整个批次（重启后无重投）
11. 有界重试：观察到的尝试次数 ≤ 配置上界，然后死信——绝不静默丢弃
12. instanceTtl：在 `subscriptionExpiry=true` 的地方，空闲的每实例订阅在 TTL 之后被移除
    （对时序宽容的断言）
13. LOAD_BALANCE 且无存活实例：全组宕机期间发布的消息被保留，并在实例启动时投递
    （队列级持久性；MQTT 共享订阅中间件在此表现不一——§13.8）

**适配器通过 TCK 即视为合规。** 这是社区适配器（EMQX 桥接
传输、Redis Streams、SQS…）的验收门槛。现有
`RabbitDeliveryIT` / `RabbitTestHarness` 断言迁入本套件；E2E 套件在迁移门槛期间继续原样运行于
rabbitmq 适配器之上。

## 12. 迁移计划

线级兼容是不变量：任何现有 RabbitMQ 部署都不应察觉这次重构。

- **Phase 1 —— 抽取 Port，RabbitMQ 适配器原样搬迁。**
  创建 `dc3-mq-core` + `dc3-mq-rabbitmq`。把 `RabbitConfig`、
  `ExchangeConfig`、`DataTopicConfig`、`DriverTopicConfig`、`PointValueRabbitConfig`、MDC 传播、确认辅助类以
  **完全相同的物理名称** 移入适配器。把 14 处裸生产者调用点（8 个驱动发送方法、发件箱(outbox)
  重发布、6 处中心侧发送）与 16 个监听器转换到新 API（`@Dc3Listener`），包括批量点位值
  接收器。业务模块 pom 把 `dc3-common-rabbitmq` 换成 `dc3-mq-core` +
  `dc3-mq-rabbitmq`。*门槛：现有 E2E（`RabbitDeliveryIT` 等）不改一行、全绿通过。*
- **Phase 2 —— TCK + Kafka 适配器。** 全球需求最高。按驱动的分区键顺序是文档化的、超越
  RabbitMQ 基线的升级。
- **Phase 3 —— RocketMQ（国内生态需求）、Pulsar、ActiveMQ/Artemis、MQTT 5。** MQTT 适配器让部署可以在一个中间件上
  运行两个平面（南向设备面 + 内部异步）——或在已运营 MQTT 中间件的地方彻底去掉
  RabbitMQ。发布能力矩阵、按中间件的 `dc3.mq.type` compose profile。
- **贯穿始终** —— `DriverSenderService` 接口不动；针对它编译的第三方驱动 JAR 持续可用。

随 Phase 1 一并进行的建议清理：

- `RabbitConstant` 移出 `dc3-common-constant` 的 `constant/driver` 包：逻辑名称变成 Port 中的 `MqTopic`；
  物理名称变成适配器私有（`RabbitNames`）。
- 删除死拓扑：`register` exchange/queue/路由常量（声明了却无处引用）与未使用的
  `dc3.e.mqtt` exchange + `QUEUE_MQTT`（无 binding、无生产者、无消费者）。只在真实消费者
  存在时再引入。
- 包 `data/rabbit`、`receiver/rabbit` → `data/mq/listener`、`driver/mq/listener`。
- `dc3.rabbit.tag` → `dc3.mq.namespace`（rabbitmq 适配器为兼容把它映射到旧属性）。

## 13. 开放问题

1. **Pulsar 租户模型** —— 把 `dc3.mq.namespace` 原生映射到 Pulsar tenant/namespace，还是压平为 topic 前缀？（原生
   更干净，但需要租户开通文档。）
2. **信封演进** —— 为了第三方互操作，把 `dc3-type`/header 对齐到 CloudEvents 1.0 属性名？现在改便宜、以后改是
   破坏性的；对 `type` /
   `traceparent` 倾向采纳。schema-v1 载荷信封强化了这一理由：传输 header 与载荷 schema 应
   在同一路线图上演进。
3. **Kafka 背压语义** —— `pause()` 需要一个当前
   `Acknowledgment` API 无法表达的恢复信号；可能需要 `Acknowledgment.defer()` 或监听器容器级钩子。在
   Phase 2 的 TCK 工作中决定。
4. **BROADCAST 队列 TTL** —— 元数据队列 30 秒 auto-delete TTL 限制了失效实例造成的陈旧；
   非 RabbitMQ 中间件通过订阅过期或心跳表达——在 TCK 中确认各适配器策略。
5. **单进程模式（`dc3-center-single`）** —— 确认是否应为最小部署提供一个进程内/无中间件适配器
   （`dc3-mq-local`），复用 facade `local` 的先例。
6. **点位值死信队列保留** —— 隔离队列既无消费者也无 TTL；卡住的部署会让它
   无界增长。需要决定：队列 TTL、按容量上限告警，或一个最小审计消费者。
7. **批量毒消息粒度** —— 今天一条毒消息会把整个批次死信。按消息提取
   （只死信问题消息、确认其余）更友好，但跨中间件更麻烦（Kafka 需要重处理循环）。
   决定 TCK 强制什么；批次粒度是安全默认值。
8. **MQTT 共享订阅组语义** —— MQTT 5 把 `$share` 的投递标准化为投给一个 *在线* 成员，但对没有成员在线时的保留
   保持沉默（因中间件而异）。TCK 用例 13 逐中间件判定合规；
   能力矩阵必须向权衡 MQTT 适配器的部署者明确指出该行为。

## 14. 附录 —— 当前调用点清单（Phase 1 检查清单）

生产者（14 处裸调用点，主代码）：

| 模块    | 类                                                  | 发往                                                                       |
|---------|----------------------------------------------------|---------------------------------------------------------------------------|
| driver  | `DriverSenderServiceImpl`（8 个方法，6 处裸调用点） | STATE、ALARM ×2、POINT_VALUE、POINT_COMMAND_RESULT、COMMAND_RESULT、EVENT  |
| driver  | `BufferServiceImpl`                                | POINT_VALUE（发件箱(outbox) 重发布 + 重试调度器）                          |
| data    | `DriverStateServiceImpl`                           | STATE_TIMEOUT_DELAY（45 秒）                                               |
| data    | `EntityStateExpiryScanner`                         | STATE_TIMEOUT_DELAY（扫描 tick）、DEVICE_SCAN                              |
| data    | `CommandHistoryServiceImpl`                        | COMMAND                                                                    |
| data    | `PointCommandServiceImpl`                          | POINT_COMMAND                                                              |
| data    | `NotifyTaskSender`                                 | NOTIFY_TASK（经 alarm exchange）                                           |
| manager | `MetadataEventListener`                            | METADATA                                                                   |

消费者（16 个）：

| 模块   | 监听器                                                | 主题 / 角色             | 模式                                                   |
|--------|-------------------------------------------------------|-------------------------|--------------------------------------------------------|
| data   | `DriverStateReceiver`、`DeviceStateReceiver`          | STATE                   | LOAD_BALANCE                                           |
| data   | `DriverAlarmReceiver`、`DeviceAlarmReceiver`          | ALARM                   | LOAD_BALANCE                                           |
| data   | `PointValueReceiver`（BATCH，提交后确认）             | POINT_VALUE             | LOAD_BALANCE                                           |
| data   | `PointCommandResultReceiver`、`CommandResultReceiver` | 结果主题                | LOAD_BALANCE                                           |
| data   | `PointCommandDeadReceiver`、`CommandDeadReceiver`     | 死信                    | LOAD_BALANCE                                           |
| data   | `EventReportReceiver`                                 | EVENT                   | LOAD_BALANCE                                           |
| data   | `DriverTimeoutCheckReceiver`                          | 延迟检查                | LOAD_BALANCE                                           |
| data   | `NotifyWorker`                                        | NOTIFY_TASK             | LOAD_BALANCE                                           |
| data   | `EntityStateExpiryScanner`（监听器）                  | 设备扫描                | LOAD_BALANCE                                           |
| driver | `MetadataReceiver`                                    | METADATA                | **BROADCAST**                                          |
| driver | `CommandReceiver`、`PointCommandReceiver`             | COMMAND / POINT_COMMAND | LOAD_BALANCE（每服务队列，租约耦合过期）               |

点位值死信队列刻意没有监听器（隔离区，§8.2）。
