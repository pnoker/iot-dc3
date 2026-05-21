---
title: 实体状态租约表设计方案
---

# 实体状态租约表设计方案

本文描述 `dc3_entity_state` 持久化状态租约表的设计，用于替代 `LocalCacheService` 作为驱动和设备在线状态的权威数据源。

## 核心结论

- 新增 `dc3_entity_state` 表，存储每个驱动和设备的当前状态租约。
- 心跳写入路径改为双写：先写 `dc3_entity_state`，再写本地缓存。
- 状态查询路径改为优先读取 `dc3_entity_state`，本地缓存作为可选加速层。
- 新增定时扫描器 `EntityStateExpiryScanner`，扫描 `expire_time` 已过期的行并触发离线告警。
- `lease_version` 字段用于二次检查：扫描器发现过期行时，比对当前行的 `lease_version` 与扫描时的版本，避免旧扫描覆盖新心跳。

## 与 dc3_entity_alarm 的关系

| 表 | 职责 |
| --- | --- |
| `dc3_entity_state` | 当前状态事实：在线/离线/故障/维护，最近心跳时间，租约到期时间 |
| `dc3_entity_alarm` | 告警历史：离线告警、故障告警、规则告警、主动上报告警 |

状态表是当前事实的单一行（每个实体一行），告警表是不可变历史记录（每次告警一行）。

## 表结构设计

```sql
CREATE TABLE dc3_entity_state
(
    id                BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    entity_type_flag  SMALLINT DEFAULT 0 NOT NULL,                 -- Entity type flag, 1: device, 2: driver
    entity_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Entity ID (driver ID or device ID)
    driver_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID (for devices: owning driver; for drivers: same as entity_id)
    state_flag        SMALLINT DEFAULT 0 NOT NULL,                 -- Current status index (DriverStatusEnum/DeviceStatusEnum)
    lease_version     BIGINT   DEFAULT 0 NOT NULL,                 -- Monotonic version incremented on each heartbeat
    expire_time       TIMESTAMPTZ NOT NULL,                        -- Absolute time when this lease expires
    ttl_seconds       INT      DEFAULT 0 NOT NULL,                 -- TTL in seconds used for this entry
    tenant_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    create_time       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Creation time
    operate_time      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted           SMALLINT DEFAULT 0 NOT NULL                  -- Logical delete flag, 0: not deleted, 1: deleted
);

-- One active state row per entity
CREATE UNIQUE INDEX idx_entity_state_entity
    ON dc3_entity_state (entity_type_flag, entity_id)
    WHERE deleted = 0;

-- For the expiry scanner to find expired rows
CREATE INDEX idx_entity_state_expire
    ON dc3_entity_state (expire_time)
    WHERE deleted = 0;

-- For status queries filtered by tenant and entity type
CREATE INDEX idx_entity_state_tenant_status
    ON dc3_entity_state (tenant_id, entity_type_flag, state_flag)
    WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_entity_state
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();
```

## 字段说明

| 字段 | 说明 |
| --- | --- |
| `entity_type_flag` | 实体类型，1: device, 2: driver，沿用 `EntityTypeFlagEnum` |
| `entity_id` | 实体 ID，驱动状态为 driver_id，设备状态为 device_id |
| `driver_id` | 冗余字段，便于按驱动聚合。驱动条目等于 entity_id，设备条目为所属驱动 |
| `state_flag` | 当前状态码索引，对应 `DriverStatusEnum` 或 `DeviceStatusEnum` 的 index 值 |
| `lease_version` | 单调递增版本号，每次心跳续租时 +1。扫描器用于二次检查避免 ABA 问题 |
| `expire_time` | 租约绝对到期时间。扫描器检查此字段判断是否过期 |
| `ttl_seconds` | 本次心跳使用的 TTL 秒数，用于记录和审计 |

## 心跳写入路径

心跳到达 Data Center 后：

1. 根据 `entity_type_flag + entity_id` 查询现有行。
2. 不存在则新建，`lease_version = 1`；存在则 `lease_version += 1`。
3. 更新 `state_flag`、`expire_time`、`ttl_seconds`、`tenant_id`。
4. `saveOrUpdate` 写入数据库。
5. 写入本地缓存（加速同进程查询）。

## 过期扫描路径

定时扫描器每 15 秒执行一次：

1. 查询 `expire_time < now()` 且 `deleted = 0` 的行。
2. 对每行进行二次检查：重新读取行，确认 `lease_version` 未变且 `expire_time` 仍过期。
3. 如果最后状态已经是 `offline`，跳过。
4. 写入 `dc3_entity_alarm`（`alarm_type_flag = OFFLINE`，`alarm_source_flag = STATE_TIMEOUT`）。
5. 更新 `state_flag` 为 `offline`，`expire_time` 设为未来一个扫描周期。
6. 触发规则链路。

## 查询路径

所有状态查询改为读取 `dc3_entity_state`：

- 如果行不存在或 `expire_time < now()`，返回 `offline`。
- 否则根据 `state_flag` 映射为对应枚举的 code。

这解决了：
- **重启丢状态**：数据库持久化，重启后状态不丢失。
- **多实例不一致**：所有实例读取同一数据库。
- **过期不精确**：扫描器基于数据库 `expire_time` 精确判断。

## 代码落地

### 新增文件

| 文件 | 说明 |
| --- | --- |
| `EntityTypeFlagEnum` | 实体类型枚举 (DEVICE=1, DRIVER=2) |
| `EntityStateDO` | 持久化对象 |
| `EntityStateMapper` | MyBatis-Plus Mapper |
| `EntityStateManager` / `EntityStateManagerImpl` | DAL 层 |
| `EntityStateExpiryScanner` | 过期扫描器 |

### 修改文件

| 文件 | 变更 |
| --- | --- |
| `DriverStateServiceImpl` | 心跳双写 DB + cache |
| `DeviceStateServiceImpl` | 心跳双写 DB + cache |
| `OfflineExpiryListener` | 改为从 scanner 接收事件，而非 Caffeine expiry |
| `DeviceStatusServiceImpl` | 状态查询从 DB 读取 |
| `DriverStatusServiceImpl` | 状态查询从 DB 读取 |
| `SystemHealthServiceImpl` | 状态查询从 DB 读取 |
| `StatusHealthServer` | gRPC 状态查询从 DB 读取 |
| `StatusHealthLocalFacade` | 本地 facade 状态查询从 DB 读取 |
