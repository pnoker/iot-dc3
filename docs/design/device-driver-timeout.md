---
title: 设备与驱动超时管理方案
---

# 设备与驱动超时管理方案

本文定义 DC3 驱动状态、设备状态的超时管理方案。核心目标是把当前进程内 TTL 缓存，调整为可持久化、可恢复、可横向扩展的状态租约机制。

## 核心结论

- 驱动和设备状态统一按"租约"理解：心跳续租，租约到期后判定离线。
- PostgreSQL `dc3_entity_state` 是唯一状态事实源。
- RabbitMQ 只做"到期检查闹钟"，不保存最终状态。
- 初版只新增一张状态表：`dc3_entity_state`。
- `dc3_entity_state` 放在 `dc3_data` schema，属于运行态数据。
- 不复用 `dc3_driver` / `dc3_device`，避免高频心跳污染 Manager 元数据表。
- 不再使用 `LocalCacheService` 承载驱动/设备在线状态。
- 不使用 RabbitMQ Delayed Message Plugin，只使用 RabbitMQ TTL + DLX。

## 背景问题

当前驱动、设备状态由 `LocalCacheService` 维护：

- 驱动心跳固定上报，Data Center 设置本地 45 秒 TTL。
- 设备心跳由驱动上报，payload 中携带超时时间。
- TTL 过期后由 `OfflineExpiryListener` 派生离线事件。
- 状态查询先读本地缓存，缓存不存在时按离线处理。

这个方案的问题是：

- Data Center 重启后，本地 TTL 丢失。
- 多实例部署时，不同实例的本地状态可能不一致。
- 状态事实源在内存中，不利于恢复、排查和统一查询。

## 设计边界

本方案只解决驱动/设备运行态在线、离线、故障等状态管理。

不做以下事情：

- 不把驱动/设备状态建模为 `Profile` 下的模型事件。
- 不把每次心跳写入 `dc3_entity_alarm`。
- 不要求离线判断精确到毫秒，允许扫描间隔带来的少量延迟。
- 不为每个设备心跳都生成一条延迟消息，避免 RabbitMQ 堆积。

## 状态表设计

统一使用一张表保存驱动和设备当前状态。字段风格参考现有 PostgreSQL 脚本：

- 数据库对象统一使用 `state`，如 `dc3_entity_state`、`state_flag`。
- Java DTO 和已有枚举可继续使用 `DeviceStatus` / `DriverStatus`，写入状态表时映射为 `state_flag`。
- 枚举类字段使用 `*_flag SMALLINT`，不直接存字符串。
- 扩展字段使用 `JSON DEFAULT '{}'::JSON`。
- 索引命名使用 `idx_*_active_unique` / `idx_*`。
- 使用 `operate_time` 触发器维护更新时间。

```sql
CREATE TABLE dc3_entity_state
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    entity_type_flag    SMALLINT DEFAULT 0 NOT NULL,                 -- Entity type flag, 3: driver, 6: device
    entity_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Entity ID
    parent_entity_id    BIGINT   DEFAULT 0 NOT NULL,                 -- Parent entity ID
    state_flag          SMALLINT DEFAULT 1 NOT NULL,                 -- State flag, 0: online, 1: offline, 2: maintain, 3: fault
    last_state_flag     SMALLINT DEFAULT 1 NOT NULL,                 -- Last state flag
    last_heartbeat_time TIMESTAMPTZ NOT NULL,                        -- Latest heartbeat time
    expire_time TIMESTAMPTZ NOT NULL,                                -- Lease expiration time
    lease_version       BIGINT   DEFAULT 0 NOT NULL,                 -- Lease version
    last_alarm_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Latest alarm ID
    timeout_seconds     INTEGER  DEFAULT 0 NOT NULL,                 -- Timeout duration, seconds
    timeout_source_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Timeout source flag, 0: system, 1: driver, 2: device, 3: profile
    state_ext           JSON     DEFAULT '{}'::JSON        NOT NULL, -- State extension information
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,      -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL                  -- Logical delete flag, 0: not deleted, 1: deleted
);

CREATE UNIQUE INDEX idx_entity_state_active_unique
    ON dc3_entity_state (tenant_id, entity_type_flag, entity_id) WHERE deleted = 0;

CREATE INDEX idx_entity_state_expire
    ON dc3_entity_state (tenant_id, entity_type_flag, state_flag, expire_time) WHERE deleted = 0;

CREATE INDEX idx_entity_state_parent
    ON dc3_entity_state (tenant_id, entity_type_flag, parent_entity_id, state_flag) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_entity_state
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();
```

实际初始化 SQL 还应补充 `COMMENT ON TABLE` / `COMMENT ON COLUMN`，与现有脚本保持一致；文档中不展开完整注释，避免方案过长。

这张表是运行态状态表，和 `dc3_entity_alarm` 更接近，因此保留 `create_time`、`operate_time`、`deleted`，不增加 `creator_id`、
`creator_name`、`operator_id`、`operator_name`、`signature`、`version` 这类配置表字段。

关键字段：

| 字段                    | 说明                                                                                            |
|-----------------------|-----------------------------------------------------------------------------------------------|
| `entity_type_flag`    | 实体类型，沿用 `EntityTypeFlagEnum`：`3` 为 driver，`6` 为 device                                        |
| `entity_id`           | 驱动 ID 或设备 ID                                                                                  |
| `parent_entity_id`    | 设备所属驱动 ID；驱动为 `0`                                                                             |
| `state_flag`          | 当前状态，沿用 `DriverStatusEnum` / `DeviceStatusEnum`：`0` online，`1` offline，`2` maintain，`3` fault |
| `last_state_flag`     | 上一次状态                                                                                         |
| `last_heartbeat_time` | 最近一次心跳时间                                                                                      |
| `expire_time`         | 当前租约到期时间                                                                                      |
| `lease_version`       | 每次心跳递增，用于忽略旧超时消息                                                                              |
| `last_alarm_id`       | 最近一次关联告警 ID，便于追踪离线/故障告警                                                                       |
| `timeout_seconds`     | 当前租约时长                                                                                        |
| `timeout_source_flag` | 超时来源，建议新增枚举：`0` system，`1` driver，`2` device，`3` profile                                      |
| `state_ext`           | 节点、协议状态、诊断信息等扩展                                                                               |

## RabbitMQ 设计

RabbitMQ 只负责触发检查，不负责保存在线状态。

使用 TTL + DLX：

```text
timeout message
  -> TTL delay queue
  -> dead letter exchange
  -> timeout check queue
  -> Data Center 二次检查
```

资源建议：

| 名称                                         | 类型       | 作用             |
|--------------------------------------------|----------|----------------|
| `dc3.e.state_timeout_delay`                | exchange | 接收待延迟检查消息      |
| `dc3.e.state_timeout_check`                | exchange | 接收到期后的检查消息     |
| `dc3.q.state_timeout.driver.45s`           | queue    | 驱动 45 秒延迟队列    |
| `dc3.q.state_timeout.driver_check`         | queue    | 驱动超时检查队列       |
| `dc3.q.state_timeout.device_scan_tick.10s` | queue    | 设备扫描 tick 延迟队列 |
| `dc3.q.state_timeout.device_scan`          | queue    | 设备超时扫描队列       |

也就是说，这里是 2 个 exchange + 4 个 queue，不是 4 个 topic。若 exchange 使用 `topic` 类型，routing key 单独定义：

| routing key                      | 绑定关系                                                                      |
|----------------------------------|---------------------------------------------------------------------------|
| `state.timeout.driver.45s`       | `dc3.e.state_timeout_delay` -> `dc3.q.state_timeout.driver.45s`           |
| `state.timeout.driver.check`     | `dc3.e.state_timeout_check` -> `dc3.q.state_timeout.driver_check`         |
| `state.timeout.device.scan.tick` | `dc3.e.state_timeout_delay` -> `dc3.q.state_timeout.device_scan_tick.10s` |
| `state.timeout.device.scan`      | `dc3.e.state_timeout_check` -> `dc3.q.state_timeout.device_scan`          |

可靠性要求：

- exchange durable。
- queue durable。
- timeout message persistent。
- 开启 publisher confirm。
- 消费端手动 ack。

## 驱动超时流程

驱动心跳由平台 SDK 控制，周期固定，适合每次心跳投递一条 45 秒检查消息。

### 心跳续租

1. 驱动 SDK 每 15 秒发送一次心跳。
2. Data Center upsert `dc3_entity_state`：
    - `entity_type_flag = 3`
    - `entity_id = driverId`
    - `parent_entity_id = 0`
    - `state_flag = 0`
    - `expire_time = now + 45s`
    - `lease_version = lease_version + 1`
    - `timeout_seconds = 45`
    - `timeout_source_flag = 0`
3. 发布 `DriverTimeoutCheckDTO` 到驱动延迟队列。

### 到期检查

1. 45 秒后消息进入驱动检查队列。
2. 消费者读取 `dc3_entity_state`。
3. 满足以下条件才判定离线：
    - 状态行存在。
    - `lease_version` 等于消息中的版本。
    - `expire_time <= now()`。
    - `state_flag` 仍是在线族。
4. 更新 `state_flag = 1`。
5. 构建 DRIVER 规则事实，命中规则后写入 `dc3_entity_alarm`，告警来源为 `driver-offline`。

## 设备超时流程

设备超时不做全局固定。不同协议、不同设备的心跳方式可能不同，因此由驱动或设备配置决定租约时长。

设备侧的 RabbitMQ `device_scan_tick.10s` 只是扫描周期，不是设备超时时长。不同设备的超时时长由心跳续租时计算出的
`expire_time` 区分。

### 心跳续租

1. 驱动根据协议行为上报设备状态。
2. `DeviceEventDTO.DeviceStatus` 携带 `deviceId`、`driverId`、`status`、`timeOut`、`timeUnit`。
3. Data Center upsert `dc3_entity_state`：
    - `entity_type_flag = 6`
    - `entity_id = deviceId`
    - `parent_entity_id = driverId`
    - `state_flag = payload.status.index`
    - `expire_time = now + payload timeout`
    - `lease_version = lease_version + 1`
    - `timeout_source_flag = 1`

### 扫描离线

设备默认不为每次心跳投递延迟消息，而是由 RabbitMQ 维护一个可恢复扫描 tick。

`device_scan_tick.10s` 的含义是每 10 秒唤醒一次扫描器。扫描器不关心设备各自配置了 30 秒、5 分钟还是 15 分钟，只查询
`expire_time <= now()` 的状态行。因此不同设备的超时时长体现在各自的 `expire_time` 上，而不是体现在 RabbitMQ 队列 TTL 上。

```text
device_scan_tick
  -> 到期后触发扫描
  -> 查询已过期在线设备
  -> 批量标记离线并写事件
  -> 发布下一条 scan tick
```

扫描 SQL：

```sql
SELECT *
FROM dc3_entity_state
WHERE deleted = 0
  AND entity_type_flag = 6
  AND state_flag IN (0, 2)
  AND expire_time <= now()
ORDER BY expire_time ASC
LIMIT #{batchSize}
FOR
UPDATE SKIP LOCKED;
```

示例：

| 设备 | 超时配置  | 最近心跳时间   | `expire_time` | 10 秒扫描到 10:00:40 时 |
|----|-------|----------|---------------|--------------------|
| A  | 30 秒  | 10:00:00 | 10:00:30      | 被扫描为过期             |
| B  | 5 分钟  | 10:00:00 | 10:05:00      | 不处理                |
| C  | 15 分钟 | 10:00:00 | 10:15:00      | 不处理                |

多实例下使用 `FOR UPDATE SKIP LOCKED` 避免重复处理同一批设备。

每条过期记录处理：

1. 再次确认 `expire_time <= now()`。
2. 更新 `state_flag = 1`。
3. 构建 DEVICE 规则事实，命中规则后写入 `dc3_entity_alarm`，告警来源为 `device-offline`。

## 二次检查

二次检查是本方案的关键。

原因：RabbitMQ 中已经投递的旧检查消息无法撤销。设备或驱动可能已经续租，但旧消息仍会到期。

所以检查消息到期后，必须重新读取 `dc3_entity_state`，并比较：

- `lease_version` 是否仍一致。
- `expire_time` 是否确实已过期。
- `state_flag` 是否仍是在线族。

只有全部满足，才能判定离线。

## 状态查询

状态查询直接读取 `dc3_entity_state`。

查询规则：

1. 按 `tenant_id + entity_type_flag + entity_id` 查询状态行。
2. 如果状态行不存在，返回 `offline` 或 `unknown`，具体由产品口径决定。
3. 如果 `expire_time <= now()` 且 `state_flag` 仍是在线族，查询结果返回 `offline`。
4. 查询接口不直接写离线事件，避免读接口产生副作用。

初版不再增加本地读缓存。状态表已有唯一索引，先直连数据库把语义跑通；如果后续查询压力过大，再评估只读副本、索引优化或接口级短缓存。

## 和实体告警的关系

`dc3_entity_state` 只保存"当前状态和租约"。

`dc3_entity_alarm` 保存需要关注、确认、通知、追踪的告警记录：

| 表                  | 职责                    |
|--------------------|-----------------------|
| `dc3_entity_state` | 当前状态事实                |
| `dc3_entity_alarm` | 离线告警、故障告警、状态翻转告警、规则告警 |

心跳本身不写告警表，避免 `dc3_entity_alarm` 被刷爆。只有状态翻转、离线、故障、规则命中等有业务意义的变化才进入告警链路。

`dc3_entity_alarm` 的详细命名和表结构见 [实体告警统一表设计方案](entity-alarm.md)。

## 和 LocalCacheService 的关系

改造完成后，`LocalCacheService` 不再参与驱动/设备状态链路：

- 心跳续租只写 `dc3_entity_state`。
- 超时处理只更新 `dc3_entity_state` 并触发规则告警链路。
- 状态查询直接读 `dc3_entity_state`。
- `OfflineExpiryListener` 移除。

`LocalCacheService` 仍可用于 token denylist、最新点位值缓存等其他场景，但不再承载驱动/设备在线状态。

## 设备超时策略

设备超时建议按以下优先级取值：

1. `Device.device_ext.stateTimeout`。
2. `Profile.profile_ext.defaultStateTimeout`。
3. 驱动上报的 `DeviceStatus.timeOut/timeUnit`。
4. 系统默认值，例如 15 分钟。

协议建议：

| 场景              | 在线依据                      | 建议超时                  |
|-----------------|---------------------------|-----------------------|
| Modbus / PLC 轮询 | 最近一次采集成功                  | 采集周期的 3-5 倍           |
| MQTT 主动上报       | 最近一次设备报文或心跳               | 设备声明周期的 2-3 倍         |
| OPC UA          | Session / Subscription 状态 | 协议连接状态 + 数据更新时间       |
| 低频设备            | 最近一次业务上报                  | 单独配置较长超时或返回 `unknown` |

## 落地阶段

### 第一阶段：状态表

- 新增 `dc3_entity_state`。
- 驱动/设备心跳 upsert 状态表。
- 状态查询改为读取状态表。
- 移除状态链路对 `LocalCacheService` 的读写。

### 第二阶段：驱动超时

- 新增驱动 45 秒延迟队列和检查队列。
- 驱动心跳发布 `DriverTimeoutCheckDTO`。
- 检查消费者按 `lease_version + expire_time` 二次检查。

### 第三阶段：设备扫描

- 新增设备扫描 tick。
- 扫描状态表中过期的在线设备。
- 批量写 `device-offline`。
- 多实例使用 `FOR UPDATE SKIP LOCKED`。

### 第四阶段：清理旧链路

- 移除 `OfflineExpiryListener`。
- Dashboard、状态 API、健康检查统一以状态表为准。

### 第五阶段：策略产品化

- 在 `Device` 或 `Profile` 扩展中补充默认超时配置。
- Web UI 支持配置设备状态超时。
- 驱动 SDK 文档补充不同协议的设备 TTL 建议。

## 待确认

- 是否引入 `unknown` 状态。
- 设备扫描 tick 初值使用 10 秒还是 30 秒。
- 驱动是否也需要兜底扫描，防止个别检查消息丢失。
