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

CREATE SCHEMA IF NOT EXISTS dc3_data;
SET search_path TO dc3_data;

-- ----------------------------
-- Function for update operate time
-- ----------------------------
CREATE OR REPLACE FUNCTION update_operate_time()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.operate_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- ----------------------------
-- Table structure for dc3_notify
-- ----------------------------
CREATE TABLE dc3_notify
(
    id                BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    notify_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Notification name
    notify_code       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Notification code
    auto_confirm_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Auto-confirm flag
    notify_interval   BIGINT   DEFAULT 0 NOT NULL,                 -- Notification interval, milliseconds
    notify_ext        JSON     DEFAULT '{}'::JSON        NOT NULL, -- Notification configuration
    enable_flag       SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,    -- Creation time
    operator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Operation time
    deleted           SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_notify_auto_confirm_flag CHECK (auto_confirm_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_notify_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_notify_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_notify_tenant_code_active_unique ON dc3_notify (tenant_id, notify_code) WHERE deleted = 0 AND notify_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_notify
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_notify IS 'Notification table';
COMMENT
ON COLUMN dc3_notify.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_notify.notify_name IS 'Notification name';
COMMENT
ON COLUMN dc3_notify.notify_code IS 'Notification code';
COMMENT
ON COLUMN dc3_notify.auto_confirm_flag IS 'Auto-confirm flag';
COMMENT
ON COLUMN dc3_notify.notify_interval IS 'Notification interval, milliseconds';
COMMENT
ON COLUMN dc3_notify.notify_ext IS 'Notification configuration';
COMMENT
ON COLUMN dc3_notify.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_notify.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_notify.remark IS 'Description';
COMMENT
ON COLUMN dc3_notify.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_notify.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_notify.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_notify.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_notify.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_notify.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_notify.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_notify_channel
-- ----------------------------
CREATE TABLE dc3_notify_channel
(
    id                BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    channel_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Notification channel name
    channel_code      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Notification channel code
    channel_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Notification channel type flag
    credential_ref    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Credential reference
    channel_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Notification channel configuration
    enable_flag       SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,    -- Creation time
    operator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Operation time
    deleted           SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_notify_channel_channel_type_flag CHECK (channel_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_notify_channel_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_notify_channel_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_notify_channel_tenant_code_active_unique ON dc3_notify_channel (tenant_id, channel_code) WHERE deleted = 0 AND channel_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_notify_channel
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_notify_channel IS 'Notification channel table';
COMMENT
ON COLUMN dc3_notify_channel.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_notify_channel.channel_name IS 'Notification channel name';
COMMENT
ON COLUMN dc3_notify_channel.channel_code IS 'Notification channel code';
COMMENT
ON COLUMN dc3_notify_channel.channel_type_flag IS 'Notification channel type flag';
COMMENT
ON COLUMN dc3_notify_channel.credential_ref IS 'Credential reference';
COMMENT
ON COLUMN dc3_notify_channel.channel_ext IS 'Notification channel configuration';
COMMENT
ON COLUMN dc3_notify_channel.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_notify_channel.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_notify_channel.remark IS 'Description';
COMMENT
ON COLUMN dc3_notify_channel.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_notify_channel.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_notify_channel.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_notify_channel.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_notify_channel.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_notify_channel.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_notify_channel.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_notify_channel_bind
-- ----------------------------
CREATE TABLE dc3_notify_channel_bind
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    notify_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Notification ID
    channel_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Notification channel ID
    bind_ext      JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Notification channel binding configuration
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_notify_channel_bind_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_notify_channel_bind_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_notify_channel_bind_active_unique ON dc3_notify_channel_bind (tenant_id, notify_id, channel_id) WHERE deleted = 0;
CREATE INDEX idx_notify_channel_bind_notify_id ON dc3_notify_channel_bind (notify_id) WHERE deleted = 0;
CREATE INDEX idx_notify_channel_bind_channel_id ON dc3_notify_channel_bind (channel_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_notify_channel_bind
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_notify_channel_bind IS 'Notification channel binding table';
COMMENT
ON COLUMN dc3_notify_channel_bind.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_notify_channel_bind.notify_id IS 'Notification ID';
COMMENT
ON COLUMN dc3_notify_channel_bind.channel_id IS 'Notification channel ID';
COMMENT
ON COLUMN dc3_notify_channel_bind.bind_ext IS 'Notification channel binding configuration';
COMMENT
ON COLUMN dc3_notify_channel_bind.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_notify_channel_bind.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_notify_channel_bind.remark IS 'Description';
COMMENT
ON COLUMN dc3_notify_channel_bind.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_notify_channel_bind.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_notify_channel_bind.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_notify_channel_bind.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_notify_channel_bind.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_notify_channel_bind.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_notify_channel_bind.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_message
-- ----------------------------
CREATE TABLE dc3_message
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    message_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Message name
    message_code  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Message code
    message_level SMALLINT DEFAULT 2 NOT NULL,                   -- Message level
    message_ext   JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Message configuration
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_message_message_level CHECK (message_level BETWEEN 0 AND 3),
    CONSTRAINT chk_message_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_message_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_message_tenant_code_active_unique ON dc3_message (tenant_id, message_code) WHERE deleted = 0 AND message_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_message
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_message IS 'Message table';
COMMENT
ON COLUMN dc3_message.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_message.message_name IS 'Message name';
COMMENT
ON COLUMN dc3_message.message_code IS 'Message code';
COMMENT
ON COLUMN dc3_message.message_level IS 'Message level';
COMMENT
ON COLUMN dc3_message.message_ext IS 'Message configuration';
COMMENT
ON COLUMN dc3_message.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_message.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_message.remark IS 'Description';
COMMENT
ON COLUMN dc3_message.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_message.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_message.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_message.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_message.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_message.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_message.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_rule
-- ----------------------------
CREATE TABLE dc3_rule
(
    id                     BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    alarm_target_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm target type flag
    rule_name              TEXT     DEFAULT ''::TEXT          NOT NULL, -- Rule name
    rule_code              TEXT     DEFAULT ''::TEXT          NOT NULL, -- Rule code
    entity_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Entity ID
    notify_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Notification ID
    message_id             BIGINT   DEFAULT 0 NOT NULL,                 -- Message ID
    rule_ext               JSON     DEFAULT '{}'::JSON        NOT NULL, -- Rule configuration
    enable_flag            SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark                 TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id             BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,         -- Creation time
    operator_id            BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,        -- Operation time
    deleted                SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_rule_alarm_target_type_flag CHECK (alarm_target_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_rule_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_rule_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_rule_entity_id ON dc3_rule (entity_id) WHERE deleted = 0;
CREATE INDEX idx_rule_notify_id ON dc3_rule (notify_id) WHERE deleted = 0;
CREATE INDEX idx_rule_message_id ON dc3_rule (message_id) WHERE deleted = 0;
CREATE UNIQUE INDEX idx_rule_tenant_code_active_unique ON dc3_rule (tenant_id, rule_code) WHERE deleted = 0 AND rule_code <> ''::TEXT;
-- Hot-path rule lookup used by RuleCandidateLookup on every fact (tenant + target type + enable + entity).
CREATE INDEX idx_rule_eval ON dc3_rule (tenant_id, alarm_target_type_flag, enable_flag, entity_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_rule
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_rule IS 'Rule table';
COMMENT
ON COLUMN dc3_rule.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_rule.alarm_target_type_flag IS 'Alarm target type flag';
COMMENT
ON COLUMN dc3_rule.rule_name IS 'Rule name';
COMMENT
ON COLUMN dc3_rule.rule_code IS 'Rule code';
COMMENT
ON COLUMN dc3_rule.entity_id IS 'Entity ID';
COMMENT
ON COLUMN dc3_rule.notify_id IS 'Notification ID';
COMMENT
ON COLUMN dc3_rule.message_id IS 'Message ID';
COMMENT
ON COLUMN dc3_rule.rule_ext IS 'Rule configuration';
COMMENT
ON COLUMN dc3_rule.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_rule.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_rule.remark IS 'Description';
COMMENT
ON COLUMN dc3_rule.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_rule.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_rule.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_rule.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_rule.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_rule.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_rule.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_rule_state
-- ----------------------------
CREATE TABLE dc3_rule_state
(
    id                     BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    rule_id                BIGINT   DEFAULT 0 NOT NULL,                 -- Rule ID
    alarm_target_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm target type flag
    entity_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Entity ID
    fingerprint            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Rule state fingerprint
    entity_state_flag      SMALLINT DEFAULT 0 NOT NULL,                 -- Rule state flag
    first_trigger_time TIMESTAMPTZ,                                     -- First trigger time
    last_trigger_time TIMESTAMPTZ,                                      -- Last trigger time
    last_recover_time TIMESTAMPTZ,                                      -- Last recovery time
    last_notify_time TIMESTAMPTZ,                                       -- Last notification time
    trigger_count          BIGINT   DEFAULT 0 NOT NULL,                 -- Trigger count
    alarm_id               BIGINT   DEFAULT 0 NOT NULL,                 -- Latest alarm ID (dc3_entity_alarm.id)
    entity_state_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Rule state extension
    tenant_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark                 TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id             BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,-- Creation time
    operator_id            BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,-- Operation time
    CONSTRAINT chk_rule_state_alarm_target_type_flag CHECK (alarm_target_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_rule_state_entity_state_flag CHECK (entity_state_flag BETWEEN 0 AND 3)
);

CREATE UNIQUE INDEX idx_rule_state_unique ON dc3_rule_state (tenant_id, rule_id, alarm_target_type_flag, entity_id, fingerprint);
CREATE INDEX idx_rule_state_rule ON dc3_rule_state (tenant_id, rule_id, entity_state_flag);
CREATE INDEX idx_rule_state_entity ON dc3_rule_state (tenant_id, alarm_target_type_flag, entity_id, entity_state_flag);

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_rule_state
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_rule_state IS 'Rule runtime state table';
COMMENT
ON COLUMN dc3_rule_state.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_rule_state.rule_id IS 'Rule ID';
COMMENT
ON COLUMN dc3_rule_state.alarm_target_type_flag IS 'Alarm target type flag';
COMMENT
ON COLUMN dc3_rule_state.entity_id IS 'Entity ID';
COMMENT
ON COLUMN dc3_rule_state.fingerprint IS 'Rule state fingerprint';
COMMENT
ON COLUMN dc3_rule_state.entity_state_flag IS 'Rule state flag';
COMMENT
ON COLUMN dc3_rule_state.first_trigger_time IS 'First trigger time';
COMMENT
ON COLUMN dc3_rule_state.last_trigger_time IS 'Last trigger time';
COMMENT
ON COLUMN dc3_rule_state.last_recover_time IS 'Last recovery time';
COMMENT
ON COLUMN dc3_rule_state.last_notify_time IS 'Last notification time';
COMMENT
ON COLUMN dc3_rule_state.trigger_count IS 'Trigger count';
COMMENT
ON COLUMN dc3_rule_state.alarm_id IS 'Latest alarm ID (dc3_entity_alarm.id)';
COMMENT
ON COLUMN dc3_rule_state.entity_state_ext IS 'Rule state extension';
COMMENT
ON COLUMN dc3_rule_state.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_rule_state.remark IS 'Description';
COMMENT
ON COLUMN dc3_rule_state.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_rule_state.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_rule_state.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_rule_state.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_rule_state.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_rule_state.operate_time IS 'Operation time';

-- ----------------------------
-- Table structure for dc3_notify_history
-- ----------------------------
CREATE TABLE dc3_notify_history
(
    id                BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    rule_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Rule ID
    notify_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Notification ID
    message_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Message ID
    channel_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Notification channel ID
    alarm_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Alarm ID (dc3_entity_alarm.id)
    channel_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Notification channel type flag
    target            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Notification target
    status_flag       SMALLINT DEFAULT 0 NOT NULL,                 -- Notification history status flag
    request_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Notification request
    response_ext      JSON     DEFAULT '{}'::JSON        NOT NULL, -- Notification response
    error_message     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Error message
    retry_count       INTEGER  DEFAULT 0 NOT NULL,                 -- Retry count
    tenant_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,-- Creation time
    operator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,-- Operation time
    CONSTRAINT chk_notify_history_channel_type_flag CHECK (channel_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_notify_history_status_flag CHECK (status_flag BETWEEN 0 AND 4)
);

CREATE INDEX idx_notify_history_status ON dc3_notify_history (tenant_id, status_flag, create_time DESC);
CREATE INDEX idx_notify_history_rule ON dc3_notify_history (tenant_id, rule_id, create_time DESC);
CREATE INDEX idx_notify_history_alarm ON dc3_notify_history (tenant_id, alarm_id, create_time DESC);
CREATE INDEX idx_notify_history_channel ON dc3_notify_history (tenant_id, channel_id, status_flag, create_time DESC);
-- Pending-task scan index used by the NotifyWorker replay/reaper paths.
CREATE INDEX idx_notify_history_pending ON dc3_notify_history (tenant_id, status_flag, create_time);

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_notify_history
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_notify_history IS 'Notification delivery history table';
COMMENT
ON COLUMN dc3_notify_history.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_notify_history.rule_id IS 'Rule ID';
COMMENT
ON COLUMN dc3_notify_history.notify_id IS 'Notification ID';
COMMENT
ON COLUMN dc3_notify_history.message_id IS 'Message ID';
COMMENT
ON COLUMN dc3_notify_history.channel_id IS 'Notification channel ID';
COMMENT
ON COLUMN dc3_notify_history.alarm_id IS 'Alarm ID (dc3_entity_alarm.id)';
COMMENT
ON COLUMN dc3_notify_history.channel_type_flag IS 'Notification channel type flag';
COMMENT
ON COLUMN dc3_notify_history.target IS 'Notification target';
COMMENT
ON COLUMN dc3_notify_history.status_flag IS 'Notification history status flag';
COMMENT
ON COLUMN dc3_notify_history.request_ext IS 'Notification request';
COMMENT
ON COLUMN dc3_notify_history.response_ext IS 'Notification response';
COMMENT
ON COLUMN dc3_notify_history.error_message IS 'Error message';
COMMENT
ON COLUMN dc3_notify_history.retry_count IS 'Retry count';
COMMENT
ON COLUMN dc3_notify_history.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_notify_history.remark IS 'Description';
COMMENT
ON COLUMN dc3_notify_history.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_notify_history.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_notify_history.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_notify_history.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_notify_history.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_notify_history.operate_time IS 'Operation time';

-- ----------------------------
-- Table structure for dc3_entity_alarm
-- ----------------------------
CREATE TABLE dc3_entity_alarm
(
    id                     BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    alarm_target_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm target type flag, 0: point, 1: device, 2: driver, 3: event
    entity_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Alarm target entity ID
    driver_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    device_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Device ID
    point_id               BIGINT   DEFAULT 0 NOT NULL,                 -- Point ID
    rule_id                BIGINT   DEFAULT 0 NOT NULL,                 -- Rule ID
    rule_state_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Rule state ID
    alarm_type_flag        SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm type flag, 0: rule, 1: offline, 2: fault, 3: state flip, 4: report
    alarm_source_flag      SMALLINT DEFAULT 0 NOT NULL,                 -- Alarm source flag, 0: rule, 1: state timeout, 2: device report, 3: driver report, 4: system, 5: event report
    alarm_level_flag       SMALLINT DEFAULT 2 NOT NULL,                 -- Alarm level flag, 0: P0, 1: P1, 2: P2, 3: P3
    alarm_ext              JSON     DEFAULT '{}'::JSON        NOT NULL, -- Alarm extension information
    expired_time           BIGINT   DEFAULT 0 NOT NULL,                 -- Expiration duration, seconds
    confirm_flag           SMALLINT DEFAULT 0 NOT NULL,                 -- Confirmation flag, 0: unconfirmed, 1: confirmed
    tenant_id              BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,         -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,        -- Operation time
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

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_entity_alarm
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_entity_alarm IS 'Entity alarm table';
COMMENT
ON COLUMN dc3_entity_alarm.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_entity_alarm.alarm_target_type_flag IS 'Alarm target type flag, 0: point, 1: device, 2: driver, 3: event';
COMMENT
ON COLUMN dc3_entity_alarm.entity_id IS 'Alarm target entity ID';
COMMENT
ON COLUMN dc3_entity_alarm.driver_id IS 'Driver ID';
COMMENT
ON COLUMN dc3_entity_alarm.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_entity_alarm.point_id IS 'Point ID';
COMMENT
ON COLUMN dc3_entity_alarm.rule_id IS 'Rule ID';
COMMENT
ON COLUMN dc3_entity_alarm.rule_state_id IS 'Rule state ID';
COMMENT
ON COLUMN dc3_entity_alarm.alarm_type_flag IS 'Alarm type flag, 0: rule, 1: offline, 2: fault, 3: state flip, 4: report';
COMMENT
ON COLUMN dc3_entity_alarm.alarm_source_flag IS 'Alarm source flag, 0: rule, 1: state timeout, 2: device report, 3: driver report, 4: system, 5: event report';
COMMENT
ON COLUMN dc3_entity_alarm.alarm_level_flag IS 'Alarm level flag, 0: P0, 1: P1, 2: P2, 3: P3';
COMMENT
ON COLUMN dc3_entity_alarm.alarm_ext IS 'Alarm extension information';
COMMENT
ON COLUMN dc3_entity_alarm.expired_time IS 'Expiration duration, seconds';
COMMENT
ON COLUMN dc3_entity_alarm.confirm_flag IS 'Confirmation flag, 0: unconfirmed, 1: confirmed';
COMMENT
ON COLUMN dc3_entity_alarm.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_entity_alarm.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_entity_alarm.operate_time IS 'Operation time';

-- ----------------------------
-- Table structure for dc3_entity_state
-- ----------------------------
CREATE TABLE dc3_entity_state
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    entity_type_flag    SMALLINT DEFAULT 0 NOT NULL,                 -- Entity type flag (EntityTypeEnum: 3=driver, 6=device)
    entity_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Entity ID (driver ID or device ID)
    parent_entity_id    BIGINT   DEFAULT 0 NOT NULL,                 -- Parent entity ID (for devices: owning driver; for drivers: 0)
    entity_state_flag   SMALLINT DEFAULT 1 NOT NULL,                 -- Current status index (EntityStateStatus)
    last_state_flag     SMALLINT DEFAULT 1 NOT NULL,                 -- Previous status index
    lease_version       BIGINT   DEFAULT 0 NOT NULL,                 -- Monotonic version incremented on each heartbeat
    expire_time TIMESTAMPTZ NOT NULL,                                -- Absolute time when this lease expires
    timeout_seconds     INT      DEFAULT 0 NOT NULL,                 -- Timeout in seconds used for this entry
    last_heartbeat_time TIMESTAMPTZ NOT NULL,                        -- Latest heartbeat time
    last_alarm_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Latest related alarm ID
    timeout_source_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Timeout source flag, 0: system, 1: driver, 2: device, 3: profile
    entity_state_ext    JSON     DEFAULT '{}'::JSON        NOT NULL, -- State extension information
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,      -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Operation time
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

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_entity_state
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_entity_state IS 'Entity state lease table';
COMMENT
ON COLUMN dc3_entity_state.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_entity_state.entity_type_flag IS 'Entity type flag (EntityTypeEnum: 3=driver, 6=device)';
COMMENT
ON COLUMN dc3_entity_state.entity_id IS 'Entity ID (driver ID or device ID)';
COMMENT
ON COLUMN dc3_entity_state.parent_entity_id IS 'Parent entity ID (for devices: owning driver; for drivers: 0)';
COMMENT
ON COLUMN dc3_entity_state.entity_state_flag IS 'Current status index (EntityStateStatus)';
COMMENT
ON COLUMN dc3_entity_state.last_state_flag IS 'Previous status index';
COMMENT
ON COLUMN dc3_entity_state.lease_version IS 'Monotonic version incremented on each heartbeat';
COMMENT
ON COLUMN dc3_entity_state.expire_time IS 'Absolute time when this lease expires';
COMMENT
ON COLUMN dc3_entity_state.timeout_seconds IS 'Timeout in seconds used for this entry';
COMMENT
ON COLUMN dc3_entity_state.last_heartbeat_time IS 'Latest heartbeat time';
COMMENT
ON COLUMN dc3_entity_state.last_alarm_id IS 'Latest related alarm ID';
COMMENT
ON COLUMN dc3_entity_state.timeout_source_flag IS 'Timeout source flag, 0: system, 1: driver, 2: device, 3: profile';
COMMENT
ON COLUMN dc3_entity_state.entity_state_ext IS 'State extension information';
COMMENT
ON COLUMN dc3_entity_state.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_entity_state.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_entity_state.operate_time IS 'Operation time';

-- ----------------------------
-- Table structure for dc3_point_command_history
-- ----------------------------
CREATE TABLE dc3_point_command_history
(
    id             BIGINT PRIMARY KEY NOT NULL,                  -- Primary key ID
    command_id     CHAR(36)           NOT NULL,                  -- Command UUID
    tenant_id      BIGINT             NOT NULL,                  -- Tenant ID
    type           SMALLINT           NOT NULL,                  -- Command type flag, 0: read, 1: read-batch, 2: write, 3: write-batch, 4: config
    device_id      BIGINT             NOT NULL,                  -- Device ID
    point_id       BIGINT             NOT NULL,                  -- Point ID
    request_value  VARCHAR(256),                                 -- Request value
    response_value VARCHAR(256),                                 -- Response value
    status         SMALLINT           NOT NULL,                  -- Command status flag, 0: pending, 1: sent, 2: success, 3: failed, 4: timeout, 5: expired, 6: dead, 7: duplicate
    error_code     VARCHAR(64),                                  -- Error code
    error_message  VARCHAR(1024),                                -- Error message
    source         SMALLINT           NOT NULL,                  -- Command source flag, 0: http, 1: grpc, 2: agentic, 3: scheduled
    source_user_id BIGINT,                                       -- Source user ID
    occur_time TIMESTAMPTZ NOT NULL,                             -- Occurrence time
    send_time TIMESTAMPTZ,                                       -- Sent time
    finish_time TIMESTAMPTZ,                                     -- Finished time
    expire_time TIMESTAMPTZ NOT NULL,                            -- Expiration time
    schema_version SMALLINT           NOT NULL,                  -- Schema version
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
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

CREATE INDEX idx_point_command_history_pending
    ON dc3_point_command_history (status, expire_time) WHERE status IN (0, 1);

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_point_command_history
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_point_command_history IS 'Point command execution history table';
COMMENT
ON COLUMN dc3_point_command_history.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_point_command_history.command_id IS 'Command UUID';
COMMENT
ON COLUMN dc3_point_command_history.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_point_command_history.type IS 'Command type flag, 0: read, 1: read-batch, 2: write, 3: write-batch, 4: config';
COMMENT
ON COLUMN dc3_point_command_history.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_point_command_history.point_id IS 'Point ID';
COMMENT
ON COLUMN dc3_point_command_history.request_value IS 'Request value';
COMMENT
ON COLUMN dc3_point_command_history.response_value IS 'Response value';
COMMENT
ON COLUMN dc3_point_command_history.status IS 'Command status flag, 0: pending, 1: sent, 2: success, 3: failed, 4: timeout, 5: expired, 6: dead, 7: duplicate';
COMMENT
ON COLUMN dc3_point_command_history.error_code IS 'Error code';
COMMENT
ON COLUMN dc3_point_command_history.error_message IS 'Error message';
COMMENT
ON COLUMN dc3_point_command_history.source IS 'Command source flag, 0: http, 1: grpc, 2: agentic, 3: scheduled';
COMMENT
ON COLUMN dc3_point_command_history.source_user_id IS 'Source user ID';
COMMENT
ON COLUMN dc3_point_command_history.occur_time IS 'Occurrence time';
COMMENT
ON COLUMN dc3_point_command_history.send_time IS 'Sent time';
COMMENT
ON COLUMN dc3_point_command_history.finish_time IS 'Finished time';
COMMENT
ON COLUMN dc3_point_command_history.expire_time IS 'Expiration time';
COMMENT
ON COLUMN dc3_point_command_history.schema_version IS 'Schema version';
COMMENT
ON COLUMN dc3_point_command_history.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_point_command_history.operate_time IS 'Operation time';

-- ----------------------------
-- Table structure for dc3_command_history
-- ----------------------------
CREATE TABLE dc3_command_history
(
    id             BIGINT PRIMARY KEY NOT NULL,                  -- Primary key ID
    record_id      CHAR(36)           NOT NULL,                  -- Record UUID
    tenant_id      BIGINT             NOT NULL,                  -- Tenant ID
    device_id      BIGINT             NOT NULL,                  -- Device ID
    command_id     BIGINT             NOT NULL,                  -- Command ID
    command_code   VARCHAR(128)       NOT NULL,                  -- Command code
    param_values JSONB,                                          -- Parameter values (JSONB)
    result_values JSONB,                                         -- Result values (JSONB)
    config_snapshot JSONB,                                       -- Command config snapshot (JSONB)
    status         SMALLINT           NOT NULL,                  -- Record status flag, 0: pending, 1: sent, 2: success, 3: failed, 4: timeout, 5: expired, 6: dead, 7: duplicate
    error_code     VARCHAR(64),                                  -- Error code
    error_message  VARCHAR(1024),                                -- Error message
    source         SMALLINT           NOT NULL,                  -- Command source flag, 0: http, 1: grpc, 2: agentic
    source_user_id BIGINT,                                       -- Source user ID
    occur_time TIMESTAMPTZ NOT NULL,                             -- Occurrence time
    send_time TIMESTAMPTZ,                                       -- Sent time
    finish_time TIMESTAMPTZ,                                     -- Finished time
    expire_time TIMESTAMPTZ NOT NULL,                            -- Expiration time
    schema_version SMALLINT           NOT NULL,                  -- Schema version
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
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

CREATE INDEX idx_command_history_pending
    ON dc3_command_history (status, expire_time) WHERE status IN (0, 1);

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_command_history
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_command_history IS 'Command execution history table';
COMMENT
ON COLUMN dc3_command_history.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_command_history.record_id IS 'Record UUID';
COMMENT
ON COLUMN dc3_command_history.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_command_history.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_command_history.command_id IS 'Command ID';
COMMENT
ON COLUMN dc3_command_history.command_code IS 'Command code';
COMMENT
ON COLUMN dc3_command_history.param_values IS 'Parameter values (JSONB)';
COMMENT
ON COLUMN dc3_command_history.result_values IS 'Result values (JSONB)';
COMMENT
ON COLUMN dc3_command_history.config_snapshot IS 'Command config snapshot (JSONB)';
COMMENT
ON COLUMN dc3_command_history.status IS 'Record status flag, 0: pending, 1: sent, 2: success, 3: failed, 4: timeout, 5: expired, 6: dead, 7: duplicate';
COMMENT
ON COLUMN dc3_command_history.error_code IS 'Error code';
COMMENT
ON COLUMN dc3_command_history.error_message IS 'Error message';
COMMENT
ON COLUMN dc3_command_history.source IS 'Command source flag, 0: http, 1: grpc, 2: agentic';
COMMENT
ON COLUMN dc3_command_history.source_user_id IS 'Source user ID';
COMMENT
ON COLUMN dc3_command_history.occur_time IS 'Occurrence time';
COMMENT
ON COLUMN dc3_command_history.send_time IS 'Sent time';
COMMENT
ON COLUMN dc3_command_history.finish_time IS 'Finished time';
COMMENT
ON COLUMN dc3_command_history.expire_time IS 'Expiration time';
COMMENT
ON COLUMN dc3_command_history.schema_version IS 'Schema version';
COMMENT
ON COLUMN dc3_command_history.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_command_history.operate_time IS 'Operation time';

-- ----------------------------
-- Table structure for dc3_event_history
-- ----------------------------
CREATE TABLE dc3_event_history
(
    id                  BIGINT PRIMARY KEY NOT NULL,             -- Primary key ID
    record_id           CHAR(36)           NOT NULL,             -- Record UUID
    tenant_id           BIGINT             NOT NULL,             -- Tenant ID
    device_id           BIGINT             NOT NULL,             -- Device ID
    event_id            BIGINT             NOT NULL,             -- Event ID
    event_code          VARCHAR(128)       NOT NULL,             -- Event code
    event_type_flag     SMALLINT           NOT NULL,             -- Event type flag
    event_level_flag    SMALLINT           NOT NULL,             -- Event level flag
    param_values JSONB,                                          -- Parameter values (JSONB)
    config_snapshot JSONB,                                       -- Event config snapshot (JSONB)
    message             VARCHAR(1024),                           -- Event message
    occur_time TIMESTAMPTZ NOT NULL,                             -- Occurrence time
    receive_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Receive time
    acknowledge_flag    SMALLINT DEFAULT 0 NOT NULL,             -- Acknowledge flag, 0: unacknowledged, 1: acknowledged
    acknowledge_time TIMESTAMPTZ,                                -- Acknowledge time
    acknowledge_user_id BIGINT,                                  -- Acknowledge user ID
    schema_version      SMALLINT           NOT NULL,             -- Schema version
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
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

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_event_history
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_event_history IS 'Event report history table';
COMMENT
ON COLUMN dc3_event_history.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_event_history.record_id IS 'Record UUID';
COMMENT
ON COLUMN dc3_event_history.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_event_history.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_event_history.event_id IS 'Event ID';
COMMENT
ON COLUMN dc3_event_history.event_code IS 'Event code';
COMMENT
ON COLUMN dc3_event_history.event_type_flag IS 'Event type flag';
COMMENT
ON COLUMN dc3_event_history.event_level_flag IS 'Event level flag';
COMMENT
ON COLUMN dc3_event_history.param_values IS 'Parameter values (JSONB)';
COMMENT
ON COLUMN dc3_event_history.config_snapshot IS 'Event config snapshot (JSONB)';
COMMENT
ON COLUMN dc3_event_history.message IS 'Event message';
COMMENT
ON COLUMN dc3_event_history.occur_time IS 'Occurrence time';
COMMENT
ON COLUMN dc3_event_history.receive_time IS 'Receive time';
COMMENT
ON COLUMN dc3_event_history.acknowledge_flag IS 'Acknowledge flag, 0: unacknowledged, 1: acknowledged';
COMMENT
ON COLUMN dc3_event_history.acknowledge_time IS 'Acknowledge time';
COMMENT
ON COLUMN dc3_event_history.acknowledge_user_id IS 'Acknowledge user ID';
COMMENT
ON COLUMN dc3_event_history.schema_version IS 'Schema version';
COMMENT
ON COLUMN dc3_event_history.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_event_history.operate_time IS 'Operation time';
