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

CREATE SCHEMA IF NOT EXISTS dc3_agentic;
SET search_path TO dc3_agentic;

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
-- Table structure for dc3_session
-- ----------------------------
CREATE TABLE dc3_session
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    conversation_id TEXT     DEFAULT ''::TEXT          NOT NULL, -- Conversation ID
    title           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Session title
    session_ext     JSON     DEFAULT '{}'::JSON        NOT NULL, -- Session extension metadata and chat preferences
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    user_id         BIGINT   DEFAULT 0 NOT NULL,                 -- User ID
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_session_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_session_conversation_id_active ON dc3_session (conversation_id) WHERE deleted = 0;
CREATE INDEX idx_session_tenant_user_time ON dc3_session (tenant_id, user_id, operate_time DESC) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_session
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_session IS 'Agentic chat session metadata';
COMMENT
ON COLUMN dc3_session.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_session.conversation_id IS 'Conversation ID';
COMMENT
ON COLUMN dc3_session.title IS 'Session title';
COMMENT
ON COLUMN dc3_session.session_ext IS 'Session extension metadata and chat preferences, e.g. model, reasoningEnabled, temperature, maxTokens, requireConfirmation';
COMMENT
ON COLUMN dc3_session.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_session.user_id IS 'User ID';
COMMENT
ON COLUMN dc3_session.remark IS 'Description';
COMMENT
ON COLUMN dc3_session.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_session.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_session.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_session.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_session.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_session.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_session.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_message
-- ----------------------------
CREATE TABLE dc3_message
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    conversation_id TEXT     DEFAULT ''::TEXT          NOT NULL, -- Conversation ID
    role            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Message role, e.g. user/assistant/system
    content         JSON     DEFAULT '{}'::JSON        NOT NULL, -- Structured message content
    model           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Model identifier used for this message
    message_index   BIGINT   DEFAULT 0 NOT NULL,                 -- Monotonic message order inside one conversation
    status          SMALLINT DEFAULT 0 NOT NULL,                 -- Message status flag
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    user_id         BIGINT   DEFAULT 0 NOT NULL,                 -- User ID
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_message_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_message_conversation_time ON dc3_message (conversation_id, create_time DESC, id DESC) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_message
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_message IS 'Agentic persisted conversation messages';
COMMENT
ON COLUMN dc3_message.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_message.conversation_id IS 'Conversation ID';
COMMENT
ON COLUMN dc3_message.role IS 'Message role, e.g. user/assistant/system';
COMMENT
ON COLUMN dc3_message.content IS 'Structured message content';
COMMENT
ON COLUMN dc3_message.model IS 'Model identifier used for this message';
COMMENT
ON COLUMN dc3_message.message_index IS 'Monotonic message order inside one conversation';
COMMENT
ON COLUMN dc3_message.status IS 'Message status flag';
COMMENT
ON COLUMN dc3_message.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_message.user_id IS 'User ID';
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
-- Table structure for dc3_attachment
-- ----------------------------
CREATE TABLE dc3_attachment
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    conversation_id TEXT     DEFAULT ''::TEXT          NOT NULL, -- Conversation ID
    file_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Original uploaded file name
    content_type    TEXT     DEFAULT ''::TEXT          NOT NULL, -- MIME content type
    size            BIGINT   DEFAULT 0 NOT NULL,                 -- File size in bytes
    file_path       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Stored file path
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    user_id         BIGINT   DEFAULT 0 NOT NULL,                 -- User ID
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_attachment_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_attachment_conversation ON dc3_attachment (conversation_id) WHERE deleted = 0;
CREATE INDEX idx_attachment_tenant_user ON dc3_attachment (tenant_id, user_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_attachment
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_attachment IS 'Agentic uploaded attachment metadata and file location';
COMMENT
ON COLUMN dc3_attachment.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_attachment.conversation_id IS 'Conversation ID';
COMMENT
ON COLUMN dc3_attachment.file_name IS 'Original uploaded file name';
COMMENT
ON COLUMN dc3_attachment.content_type IS 'MIME content type';
COMMENT
ON COLUMN dc3_attachment.size IS 'File size in bytes';
COMMENT
ON COLUMN dc3_attachment.file_path IS 'Stored file path';
COMMENT
ON COLUMN dc3_attachment.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_attachment.user_id IS 'User ID';
COMMENT
ON COLUMN dc3_attachment.remark IS 'Description';
COMMENT
ON COLUMN dc3_attachment.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_attachment.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_attachment.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_attachment.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_attachment.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_attachment.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_attachment.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_action
-- ----------------------------
CREATE TABLE dc3_action
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    action_id       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Stable action identifier exposed to clients
    conversation_id TEXT     DEFAULT ''::TEXT          NOT NULL, -- Conversation ID
    action_type     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Action type, e.g. write/control/read
    title           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Action title
    description     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Action description shown to the operator
    payload         JSON     DEFAULT '{}'::JSON        NOT NULL, -- Action payload, free-form structured data
    status          SMALLINT DEFAULT 0 NOT NULL,                 -- Action status flag
    expire_time TIMESTAMPTZ,                                     -- Optional expiration deadline
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    user_id         BIGINT   DEFAULT 0 NOT NULL,                 -- User ID
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_action_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_action_id_active ON dc3_action (action_id) WHERE deleted = 0;
CREATE INDEX idx_action_pending ON dc3_action (tenant_id, user_id, conversation_id, status, expire_time) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_action
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_action IS 'Agentic pending and executed actions';
COMMENT
ON COLUMN dc3_action.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_action.action_id IS 'Stable action identifier exposed to clients';
COMMENT
ON COLUMN dc3_action.conversation_id IS 'Conversation ID';
COMMENT
ON COLUMN dc3_action.action_type IS 'Action type, e.g. write/control/read';
COMMENT
ON COLUMN dc3_action.title IS 'Action title';
COMMENT
ON COLUMN dc3_action.description IS 'Action description shown to the operator';
COMMENT
ON COLUMN dc3_action.payload IS 'Action payload, free-form structured data';
COMMENT
ON COLUMN dc3_action.status IS 'Action status flag';
COMMENT
ON COLUMN dc3_action.expire_time IS 'Optional expiration deadline';
COMMENT
ON COLUMN dc3_action.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_action.user_id IS 'User ID';
COMMENT
ON COLUMN dc3_action.remark IS 'Description';
COMMENT
ON COLUMN dc3_action.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_action.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_action.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_action.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_action.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_action.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_action.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_model_provider
-- ----------------------------
CREATE TABLE dc3_model_provider
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    name          TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Provider name, unique
    provider_type SMALLINT DEFAULT 0 NOT NULL,                   -- Provider type, 0: openai-compatible, 1: anthropic
    base_url      TEXT     DEFAULT ''::TEXT          NOT NULL,   -- API base URL
    api_key       TEXT     DEFAULT ''::TEXT          NOT NULL,   -- API key
    default_flag  SMALLINT DEFAULT 0 NOT NULL,                   -- Default flag, 1: default, 0: not default
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
    CONSTRAINT chk_model_provider_provider_type CHECK (provider_type BETWEEN 0 AND 1),
    CONSTRAINT chk_model_provider_default_flag CHECK (default_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_model_provider_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_model_provider_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_model_provider_tenant_name_active ON dc3_model_provider (tenant_id, name) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_model_provider
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_model_provider IS 'Agentic model provider connection metadata';
COMMENT
ON COLUMN dc3_model_provider.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_model_provider.name IS 'Provider name, unique';
COMMENT
ON COLUMN dc3_model_provider.provider_type IS 'Provider type, 0: openai-compatible, 1: anthropic';
COMMENT
ON COLUMN dc3_model_provider.base_url IS 'API base URL';
COMMENT
ON COLUMN dc3_model_provider.api_key IS 'API key';
COMMENT
ON COLUMN dc3_model_provider.default_flag IS 'Default flag, 1: default, 0: not default';
COMMENT
ON COLUMN dc3_model_provider.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_model_provider.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_model_provider.remark IS 'Description';
COMMENT
ON COLUMN dc3_model_provider.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_model_provider.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_model_provider.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_model_provider.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_model_provider.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_model_provider.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_model_provider.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_model_config
-- ----------------------------
CREATE TABLE dc3_model_config
(
    id            BIGINT PRIMARY KEY             NOT NULL,             -- Primary key ID
    model         TEXT             DEFAULT ''::TEXT          NOT NULL, -- Model identifier
    label         TEXT             DEFAULT ''::TEXT          NOT NULL, -- Display label
    provider_id   BIGINT           DEFAULT 0     NOT NULL,             -- Foreign key to dc3_model_provider
    stream        BOOLEAN          DEFAULT TRUE  NOT NULL,             -- Whether streaming responses are supported
    tool_call     BOOLEAN          DEFAULT TRUE  NOT NULL,             -- Whether tool calling is supported
    vision        BOOLEAN          DEFAULT FALSE NOT NULL,             -- Whether vision input is supported
    reasoning     BOOLEAN          DEFAULT FALSE NOT NULL,             -- Whether reasoning mode is supported
    temperature   DOUBLE PRECISION DEFAULT 0.7   NOT NULL,             -- Default sampling temperature
    max_tokens    INTEGER          DEFAULT 2048  NOT NULL,             -- Default maximum tokens
    default_flag  SMALLINT         DEFAULT 0     NOT NULL,             -- Default flag, 1: default, 0: not default
    enable_flag   SMALLINT         DEFAULT 0     NOT NULL,             -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT           DEFAULT 0     NOT NULL,             -- Tenant ID
    remark        TEXT             DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id    BIGINT           DEFAULT 0     NOT NULL,             -- Creator ID
    creator_name  TEXT             DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,        -- Creation time
    operator_id   BIGINT           DEFAULT 0     NOT NULL,             -- Operator ID
    operator_name TEXT             DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,       -- Operation time
    deleted       SMALLINT         DEFAULT 0     NOT NULL,             -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_model_config_default_flag CHECK (default_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_model_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_model_config_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_model_config_tenant_model_active ON dc3_model_config (tenant_id, model) WHERE deleted = 0;
CREATE INDEX idx_model_config_provider ON dc3_model_config (provider_id) WHERE deleted = 0;
CREATE INDEX idx_model_config_enable ON dc3_model_config (enable_flag, default_flag) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_model_config
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_model_config IS 'Agentic model option metadata';
COMMENT
ON COLUMN dc3_model_config.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_model_config.model IS 'Model identifier';
COMMENT
ON COLUMN dc3_model_config.label IS 'Display label';
COMMENT
ON COLUMN dc3_model_config.provider_id IS 'Foreign key to dc3_model_provider';
COMMENT
ON COLUMN dc3_model_config.stream IS 'Whether streaming responses are supported';
COMMENT
ON COLUMN dc3_model_config.tool_call IS 'Whether tool calling is supported';
COMMENT
ON COLUMN dc3_model_config.vision IS 'Whether vision input is supported';
COMMENT
ON COLUMN dc3_model_config.reasoning IS 'Whether reasoning mode is supported';
COMMENT
ON COLUMN dc3_model_config.temperature IS 'Default sampling temperature';
COMMENT
ON COLUMN dc3_model_config.max_tokens IS 'Default maximum tokens';
COMMENT
ON COLUMN dc3_model_config.default_flag IS 'Default flag, 1: default, 0: not default';
COMMENT
ON COLUMN dc3_model_config.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_model_config.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_model_config.remark IS 'Description';
COMMENT
ON COLUMN dc3_model_config.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_model_config.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_model_config.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_model_config.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_model_config.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_model_config.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_model_config.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

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

-- ----------------------------
-- Records of dc3_model_provider
-- provider_type: 0=openai-compatible
-- default_flag:  1=default
-- enable_flag:   0=enable
-- ----------------------------
INSERT INTO dc3_model_provider (id, name, provider_type, base_url, api_key, default_flag, enable_flag, tenant_id,
                                remark, creator_id, creator_name, create_time, operator_id, operator_name, operate_time,
                                deleted)
VALUES (1, 'DeepSeek', 0, 'https://api.deepseek.com', '', 1, 0, 0, 'Default DeepSeek OpenAI-compatible provider', 1,
        'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0);

-- ----------------------------
-- Records of dc3_model_config
-- stream/tool_call/reasoning enabled, vision disabled
-- default_flag: 1=default
-- enable_flag:  0=enable
-- ----------------------------
INSERT INTO dc3_model_config (id, model, label, provider_id, stream, tool_call, vision, reasoning, temperature,
                              max_tokens, default_flag, enable_flag, tenant_id, remark, creator_id, creator_name,
                              create_time, operator_id, operator_name, operate_time, deleted)
VALUES (1, 'deepseek-v4-pro', 'DeepSeek V4 Pro', 1, TRUE, TRUE, FALSE, TRUE, 0.7, 8192, 1, 0, 0,
        'Default DeepSeek V4 Pro model config', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 0);
