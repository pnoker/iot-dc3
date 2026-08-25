--
-- MySQL 8 seed for IoT DC3 — auto-derived from the PostgreSQL seed; regenerate
-- together with it (docs/design/storage-abstraction.md §3). Requires MySQL 8.0+
-- (expression DEFAULTs, CTEs, SKIP LOCKED, JSON). Timestamps are DATETIME(6)
-- stored in UTC. Table/column comments live in the PostgreSQL seed (source of
-- truth). TimescaleDB artifacts are absent on purpose: a MySQL core requires an
-- external time-series store (docs/tsdb-stores.md); dc3_history keeps only the
-- dc3_point_latest projection.
--
SET sql_mode = CONCAT(@@sql_mode, ',NO_BACKSLASH_ESCAPES');

/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

CREATE DATABASE IF NOT EXISTS dc3_data
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dc3_data;

-- ----------------------------
-- Function for update operate time
-- ----------------------------

-- ----------------------------
-- Table structure for dc3_notify
-- ----------------------------
CREATE TABLE dc3_notify
(
    id                BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    notify_name       TEXT        DEFAULT ('')                                                NOT NULL, -- Notification name
    notify_code       TEXT        DEFAULT ('')                                                NOT NULL, -- Notification code
    auto_confirm_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Auto-confirm flag
    notify_interval   BIGINT      DEFAULT 0                                                   NOT NULL, -- Notification interval, milliseconds
    notify_ext        JSON        DEFAULT ('{}')                                              NOT NULL, -- Notification configuration
    enable_flag       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark            TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted           SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_notify_auto_confirm_flag CHECK (auto_confirm_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_notify_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_notify_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_notify_channel
-- ----------------------------
CREATE TABLE dc3_notify_channel
(
    id                BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    channel_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Notification channel name
    channel_code      TEXT        DEFAULT ('')                                                NOT NULL, -- Notification channel code
    channel_type_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Notification channel type flag
    credential_ref    TEXT        DEFAULT ('')                                                NOT NULL, -- Credential reference
    channel_ext       JSON        DEFAULT ('{}')                                              NOT NULL, -- Notification channel configuration
    enable_flag       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark            TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted           SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_notify_channel_channel_type_flag CHECK (channel_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_notify_channel_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_notify_channel_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_notify_channel_bind
-- ----------------------------
CREATE TABLE dc3_notify_channel_bind
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    notify_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Notification ID
    channel_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Notification channel ID
    bind_ext      JSON        DEFAULT ('{}')                                              NOT NULL, -- Notification channel binding configuration
    enable_flag   SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark        TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_notify_channel_bind_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_notify_channel_bind_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_message
-- ----------------------------
CREATE TABLE dc3_message
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    message_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Message name
    message_code  TEXT        DEFAULT ('')                                                NOT NULL, -- Message code
    message_level SMALLINT    DEFAULT 2                                                   NOT NULL, -- Message level
    message_ext   JSON        DEFAULT ('{}')                                              NOT NULL, -- Message configuration
    enable_flag   SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark        TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_message_message_level CHECK (message_level BETWEEN 0 AND 3),
    CONSTRAINT chk_message_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_message_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_rule
-- ----------------------------
CREATE TABLE dc3_rule
(
    id                     BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    alarm_target_type_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Alarm target type flag
    rule_name              TEXT        DEFAULT ('')                                                NOT NULL, -- Rule name
    rule_code              TEXT        DEFAULT ('')                                                NOT NULL, -- Rule code
    entity_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Entity ID
    notify_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Notification ID
    message_id             BIGINT      DEFAULT 0                                                   NOT NULL, -- Message ID
    rule_ext               JSON        DEFAULT ('{}')                                              NOT NULL, -- Rule configuration
    enable_flag            SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark                 TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id             BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name           TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time            DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id            BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name          TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted                SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_rule_alarm_target_type_flag CHECK (alarm_target_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_rule_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_rule_deleted CHECK (deleted IN (0, 1))
);



-- Hot-path rule lookup used by RuleCandidateLookup on every fact (tenant + target type + enable + entity).


-- ----------------------------
-- Table structure for dc3_rule_state
-- ----------------------------
CREATE TABLE dc3_rule_state
(
    id                     BIGINT PRIMARY KEY                                                       NOT NULL, -- Primary key ID
    rule_id                BIGINT       DEFAULT 0                                                   NOT NULL, -- Rule ID
    alarm_target_type_flag SMALLINT     DEFAULT 0                                                   NOT NULL, -- Alarm target type flag
    entity_id              BIGINT       DEFAULT 0                                                   NOT NULL, -- Entity ID
    fingerprint            VARCHAR(191) DEFAULT ''                                                  NOT NULL, -- Rule state fingerprint
    entity_state_flag      SMALLINT     DEFAULT 0                                                   NOT NULL, -- Rule state flag
    first_trigger_time     DATETIME(6),                                                                       -- First trigger time
    last_trigger_time      DATETIME(6),                                                                       -- Last trigger time
    last_recover_time      DATETIME(6),                                                                       -- Last recovery time
    last_notify_time       DATETIME(6),                                                                       -- Last notification time
    trigger_count          BIGINT       DEFAULT 0                                                   NOT NULL, -- Trigger count
    alarm_id               BIGINT       DEFAULT 0                                                   NOT NULL, -- Latest alarm ID (dc3_entity_alarm.id)
    entity_state_ext       JSON         DEFAULT ('{}')                                              NOT NULL, -- Rule state extension
    tenant_id              BIGINT       DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark                 TEXT         DEFAULT ('')                                                NOT NULL, -- Description
    creator_id             BIGINT       DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name           TEXT         DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time            DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL,-- Creation time
    operator_id            BIGINT       DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name          TEXT         DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time           DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,-- Operation time
    CONSTRAINT chk_rule_state_alarm_target_type_flag CHECK (alarm_target_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_rule_state_entity_state_flag CHECK (entity_state_flag BETWEEN 0 AND 3)
);

CREATE UNIQUE INDEX idx_rule_state_unique ON dc3_rule_state (tenant_id, rule_id, alarm_target_type_flag, entity_id, fingerprint);
CREATE INDEX idx_rule_state_rule ON dc3_rule_state (tenant_id, rule_id, entity_state_flag);
CREATE INDEX idx_rule_state_entity ON dc3_rule_state (tenant_id, alarm_target_type_flag, entity_id, entity_state_flag);



-- ----------------------------
-- Table structure for dc3_notify_history
-- ----------------------------
CREATE TABLE dc3_notify_history
(
    id                BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    rule_id           BIGINT      DEFAULT 0                                                   NOT NULL, -- Rule ID
    notify_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Notification ID
    message_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Message ID
    channel_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Notification channel ID
    alarm_id          BIGINT      DEFAULT 0                                                   NOT NULL, -- Alarm ID (dc3_entity_alarm.id)
    channel_type_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Notification channel type flag
    target            TEXT        DEFAULT ('')                                                NOT NULL, -- Notification target
    status_flag       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Notification history status flag
    request_ext       JSON        DEFAULT ('{}')                                              NOT NULL, -- Notification request
    response_ext      JSON        DEFAULT ('{}')                                              NOT NULL, -- Notification response
    error_message     TEXT        DEFAULT ('')                                                NOT NULL, -- Error message
    retry_count       INTEGER     DEFAULT 0                                                   NOT NULL, -- Retry count
    tenant_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark            TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL,-- Creation time
    operator_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,-- Operation time
    CONSTRAINT chk_notify_history_channel_type_flag CHECK (channel_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_notify_history_status_flag CHECK (status_flag BETWEEN 0 AND 4)
);

CREATE INDEX idx_notify_history_status ON dc3_notify_history (tenant_id, status_flag, create_time DESC);
CREATE INDEX idx_notify_history_rule ON dc3_notify_history (tenant_id, rule_id, create_time DESC);
CREATE INDEX idx_notify_history_alarm ON dc3_notify_history (tenant_id, alarm_id, create_time DESC);
CREATE INDEX idx_notify_history_channel ON dc3_notify_history (tenant_id, channel_id, status_flag, create_time DESC);
-- Pending-task scan index used by the NotifyWorker replay/reaper paths.
CREATE INDEX idx_notify_history_pending ON dc3_notify_history (tenant_id, status_flag, create_time);



-- ----------------------------
-- Table structure for dc3_entity_alarm
-- ----------------------------
CREATE TABLE dc3_entity_alarm
(
    id                     BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    alarm_target_type_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Alarm target type flag, 0: point, 1: device, 2: driver, 3: event
    entity_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Alarm target entity ID
    driver_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Driver ID
    device_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Device ID
    point_id               BIGINT      DEFAULT 0                                                   NOT NULL, -- Point ID
    rule_id                BIGINT      DEFAULT 0                                                   NOT NULL, -- Rule ID
    rule_state_id          BIGINT      DEFAULT 0                                                   NOT NULL, -- Rule state ID
    alarm_type_flag        SMALLINT    DEFAULT 0                                                   NOT NULL, -- Alarm type flag, 0: rule, 1: offline, 2: fault, 3: state flip, 4: report
    alarm_source_flag      SMALLINT    DEFAULT 0                                                   NOT NULL, -- Alarm source flag, 0: rule, 1: state timeout, 2: device report, 3: driver report, 4: system, 5: event report
    alarm_level_flag       SMALLINT    DEFAULT 2                                                   NOT NULL, -- Alarm level flag, 0: P0, 1: P1, 2: P2, 3: P3
    alarm_ext              JSON        DEFAULT ('{}')                                              NOT NULL, -- Alarm extension information
    expired_time           BIGINT      DEFAULT 0                                                   NOT NULL, -- Expiration duration, seconds
    confirm_flag           SMALLINT    DEFAULT 0                                                   NOT NULL, -- Confirmation flag, 0: unconfirmed, 1: confirmed
    tenant_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    create_time            DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operate_time           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    CONSTRAINT chk_entity_alarm_alarm_target_type_flag CHECK (alarm_target_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_entity_alarm_alarm_type_flag CHECK (alarm_type_flag BETWEEN 0 AND 4),
    CONSTRAINT chk_entity_alarm_alarm_source_flag CHECK (alarm_source_flag BETWEEN 0 AND 5),
    CONSTRAINT chk_alarm_level_flag CHECK (alarm_level_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_alarm_confirm_flag CHECK (confirm_flag IN (0, 1))
);

CREATE INDEX idx_entity_alarm_alert ON dc3_entity_alarm (tenant_id, alarm_level_flag, confirm_flag, create_time DESC);
-- Supports filtering alarms by source (driver/device/point) within a tenant.
CREATE INDEX idx_entity_alarm_source_time ON dc3_entity_alarm (tenant_id, alarm_source_flag, create_time DESC);
CREATE INDEX idx_entity_alarm_target ON dc3_entity_alarm (tenant_id, alarm_target_type_flag, entity_id, create_time DESC);
CREATE INDEX idx_entity_alarm_driver ON dc3_entity_alarm (tenant_id, driver_id, create_time DESC);
CREATE INDEX idx_entity_alarm_device ON dc3_entity_alarm (tenant_id, device_id, create_time DESC);
CREATE INDEX idx_entity_alarm_point ON dc3_entity_alarm (tenant_id, point_id, create_time DESC);
CREATE INDEX idx_entity_alarm_rule ON dc3_entity_alarm (tenant_id, rule_id, create_time DESC);



-- ----------------------------
-- Table structure for dc3_entity_state
-- ----------------------------
CREATE TABLE dc3_entity_state
(
    id                  BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    entity_type_flag    SMALLINT    DEFAULT 0                                                   NOT NULL, -- Entity type flag (EntityTypeEnum: 3=driver, 6=device)
    entity_id           BIGINT      DEFAULT 0                                                   NOT NULL, -- Entity ID (driver ID or device ID)
    parent_entity_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Parent entity ID (for devices: owning driver; for drivers: 0)
    entity_state_flag   SMALLINT    DEFAULT 1                                                   NOT NULL, -- Current status index (EntityStateStatus)
    last_state_flag     SMALLINT    DEFAULT 1                                                   NOT NULL, -- Previous status index
    lease_version       BIGINT      DEFAULT 0                                                   NOT NULL, -- Monotonic version incremented on each heartbeat
    expire_time         DATETIME(6)                                                             NOT NULL, -- Absolute time when this lease expires
    timeout_seconds     INT         DEFAULT 0                                                   NOT NULL, -- Timeout in seconds used for this entry
    last_heartbeat_time DATETIME(6)                                                             NOT NULL, -- Latest heartbeat time
    last_alarm_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Latest related alarm ID
    timeout_source_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Timeout source flag, 0: system, 1: driver, 2: device, 3: profile
    entity_state_ext    JSON        DEFAULT ('{}')                                              NOT NULL, -- State extension information
    tenant_id           BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    create_time         DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operate_time        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    CONSTRAINT chk_entity_state_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_entity_state_entity_state_flag CHECK (entity_state_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_entity_state_last_state_flag CHECK (last_state_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_entity_state_timeout_source_flag CHECK (timeout_source_flag BETWEEN 0 AND 3)
);

-- One state row per entity
CREATE UNIQUE INDEX idx_entity_state_unique ON dc3_entity_state (tenant_id, entity_type_flag, entity_id);
-- For the expiry scanner to find expired rows
CREATE INDEX idx_entity_state_expire ON dc3_entity_state (entity_type_flag, entity_state_flag, expire_time);
-- For status queries filtered by tenant and entity type
CREATE INDEX idx_entity_state_tenant_status ON dc3_entity_state (tenant_id, entity_type_flag, entity_state_flag);
-- For queries grouped by parent driver
CREATE INDEX idx_entity_state_parent ON dc3_entity_state (tenant_id, entity_type_flag, parent_entity_id, entity_state_flag);



-- ----------------------------
-- Table structure for dc3_point_command_history
-- ----------------------------
CREATE TABLE dc3_point_command_history
(
    id             BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    command_id     CHAR(36)                                                                NOT NULL, -- Command UUID
    tenant_id      BIGINT                                                                  NOT NULL, -- Tenant ID
    type           SMALLINT                                                                NOT NULL, -- Command type flag, 0: read, 1: read-batch, 2: write, 3: write-batch, 4: config
    device_id      BIGINT                                                                  NOT NULL, -- Device ID
    point_id       BIGINT                                                                  NOT NULL, -- Point ID
    request_value  VARCHAR(256),                                                                     -- Request value
    response_value VARCHAR(256),                                                                     -- Response value
    status         SMALLINT                                                                NOT NULL, -- Command status flag, 0: pending, 1: sent, 2: success, 3: failed, 4: timeout, 5: expired, 6: dead, 7: duplicate
    error_code     VARCHAR(64),                                                                      -- Error code
    error_message  VARCHAR(1024),                                                                    -- Error message
    source         SMALLINT                                                                NOT NULL, -- Command source flag, 0: http, 1: grpc, 2: agentic, 3: scheduled
    source_user_id BIGINT,                                                                           -- Source user ID
    occur_time     DATETIME(6)                                                             NOT NULL, -- Occurrence time
    send_time      DATETIME(6),                                                                      -- Sent time
    finish_time    DATETIME(6),                                                                      -- Finished time
    expire_time    DATETIME(6)                                                             NOT NULL, -- Expiration time
    schema_version SMALLINT                                                                NOT NULL, -- Schema version
    create_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operate_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    CONSTRAINT chk_point_command_history_type CHECK (type BETWEEN 0 AND 4),
    CONSTRAINT chk_point_command_history_status CHECK (status BETWEEN 0 AND 7),
    CONSTRAINT chk_point_command_history_source CHECK (source BETWEEN 0 AND 3)
);

-- command_id is the external correlation key used by HTTP and RabbitMQ callbacks.
CREATE UNIQUE INDEX idx_point_command_history_unique
    ON dc3_point_command_history (command_id);

-- Tenant-scoped command history pages sort by occurrence time.
CREATE INDEX idx_point_command_history_tenant_time
    ON dc3_point_command_history (tenant_id, occur_time DESC);

CREATE INDEX idx_point_command_history_lookup
    ON dc3_point_command_history (tenant_id, device_id, point_id, occur_time DESC);



-- ----------------------------
-- Table structure for dc3_command_history
-- ----------------------------
CREATE TABLE dc3_command_history
(
    id              BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    record_id       CHAR(36)                                                                NOT NULL, -- Record UUID
    tenant_id       BIGINT                                                                  NOT NULL, -- Tenant ID
    device_id       BIGINT                                                                  NOT NULL, -- Device ID
    command_id      BIGINT                                                                  NOT NULL, -- Command ID
    command_code    VARCHAR(128)                                                            NOT NULL, -- Command code
    param_values    JSON,                                                                             -- Parameter values (JSON)
    result_values   JSON,                                                                             -- Result values (JSON)
    config_snapshot JSON,                                                                             -- Command config snapshot (JSON)
    status          SMALLINT                                                                NOT NULL, -- Record status flag, 0: pending, 1: sent, 2: success, 3: failed, 4: timeout, 5: expired, 6: dead, 7: duplicate
    error_code      VARCHAR(64),                                                                      -- Error code
    error_message   VARCHAR(1024),                                                                    -- Error message
    source          SMALLINT                                                                NOT NULL, -- Command source flag, 0: http, 1: grpc, 2: agentic
    source_user_id  BIGINT,                                                                           -- Source user ID
    occur_time      DATETIME(6)                                                             NOT NULL, -- Occurrence time
    send_time       DATETIME(6),                                                                      -- Sent time
    finish_time     DATETIME(6),                                                                      -- Finished time
    expire_time     DATETIME(6)                                                             NOT NULL, -- Expiration time
    schema_version  SMALLINT                                                                NOT NULL, -- Schema version
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operate_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    CONSTRAINT chk_command_history_status CHECK (status BETWEEN 0 AND 7),
    CONSTRAINT chk_command_history_source CHECK (source BETWEEN 0 AND 2)
);

-- record_id is the external correlation key used by HTTP and RabbitMQ callbacks.
CREATE UNIQUE INDEX idx_command_history_unique
    ON dc3_command_history (record_id);

-- Tenant-scoped command record pages sort by occurrence time.
CREATE INDEX idx_command_history_tenant_time
    ON dc3_command_history (tenant_id, occur_time DESC);

CREATE INDEX idx_command_history_lookup
    ON dc3_command_history (tenant_id, device_id, command_id, occur_time DESC);



-- ----------------------------
-- Table structure for dc3_event_history
-- ----------------------------
CREATE TABLE dc3_event_history
(
    id                  BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    record_id           CHAR(36)                                                                NOT NULL, -- Record UUID
    tenant_id           BIGINT                                                                  NOT NULL, -- Tenant ID
    device_id           BIGINT                                                                  NOT NULL, -- Device ID
    event_id            BIGINT                                                                  NOT NULL, -- Event ID
    event_code          VARCHAR(128)                                                            NOT NULL, -- Event code
    event_type_flag     SMALLINT                                                                NOT NULL, -- Event type flag
    event_level_flag    SMALLINT                                                                NOT NULL, -- Event level flag
    param_values        JSON,                                                                             -- Parameter values (JSON)
    config_snapshot     JSON,                                                                             -- Event config snapshot (JSON)
    message             VARCHAR(1024),                                                                    -- Event message
    occur_time          DATETIME(6)                                                             NOT NULL, -- Occurrence time
    receive_time        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Receive time
    acknowledge_flag    SMALLINT    DEFAULT 0                                                   NOT NULL, -- Acknowledge flag, 0: unacknowledged, 1: acknowledged
    acknowledge_time    DATETIME(6),                                                                      -- Acknowledge time
    acknowledge_user_id BIGINT,                                                                           -- Acknowledge user ID
    schema_version      SMALLINT                                                                NOT NULL, -- Schema version
    create_time         DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operate_time        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    CONSTRAINT chk_event_history_event_type_flag CHECK (event_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_event_history_event_level_flag CHECK (event_level_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_event_history_acknowledge_flag CHECK (acknowledge_flag IN (0, 1))
);

-- record_id is the external correlation key used by HTTP and gRPC callbacks.
CREATE UNIQUE INDEX idx_event_history_unique
    ON dc3_event_history (record_id);

-- Tenant-scoped event record pages sort by occurrence time.
CREATE INDEX idx_event_history_tenant_time
    ON dc3_event_history (tenant_id, occur_time DESC);

CREATE INDEX idx_event_history_lookup
    ON dc3_event_history (tenant_id, device_id, event_id, occur_time DESC);

CREATE INDEX idx_event_history_type
    ON dc3_event_history (tenant_id, event_type_flag, occur_time DESC);

CREATE INDEX idx_event_history_ack
    ON dc3_event_history (tenant_id, acknowledge_flag, occur_time DESC);


