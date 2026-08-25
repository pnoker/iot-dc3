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

CREATE DATABASE IF NOT EXISTS dc3_agentic
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dc3_agentic;

-- ----------------------------
-- Function for update operate time
-- ----------------------------

-- ----------------------------
-- Table structure for dc3_session
-- ----------------------------
CREATE TABLE dc3_session
(
    id              BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    conversation_id TEXT        DEFAULT ('')                                                NOT NULL, -- Conversation ID
    title           TEXT        DEFAULT ('')                                                NOT NULL, -- Session title
    session_ext     JSON        DEFAULT ('{}')                                              NOT NULL, -- Session extension metadata and chat preferences
    tenant_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    user_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- User ID
    remark          TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_session_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_message
-- ----------------------------
CREATE TABLE dc3_message
(
    id              BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    conversation_id TEXT        DEFAULT ('')                                                NOT NULL, -- Conversation ID
    role            TEXT        DEFAULT ('')                                                NOT NULL, -- Message role, e.g. user/assistant/system
    content         JSON        DEFAULT ('{}')                                              NOT NULL, -- Structured message content
    model           TEXT        DEFAULT ('')                                                NOT NULL, -- Model identifier used for this message
    message_index   BIGINT      DEFAULT 0                                                   NOT NULL, -- Monotonic message order inside one conversation
    status          SMALLINT    DEFAULT 0                                                   NOT NULL, -- Message status flag
    tenant_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    user_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- User ID
    remark          TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_message_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_attachment
-- ----------------------------
CREATE TABLE dc3_attachment
(
    id              BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    conversation_id TEXT        DEFAULT ('')                                                NOT NULL, -- Conversation ID
    file_name       TEXT        DEFAULT ('')                                                NOT NULL, -- Original uploaded file name
    content_type    TEXT        DEFAULT ('')                                                NOT NULL, -- MIME content type
    size            BIGINT      DEFAULT 0                                                   NOT NULL, -- File size in bytes
    file_path       TEXT        DEFAULT ('')                                                NOT NULL, -- Stored file path
    tenant_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    user_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- User ID
    remark          TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_attachment_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_action
-- ----------------------------
CREATE TABLE dc3_action
(
    id              BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    action_id       TEXT        DEFAULT ('')                                                NOT NULL, -- Stable action identifier exposed to clients
    conversation_id TEXT        DEFAULT ('')                                                NOT NULL, -- Conversation ID
    action_type     TEXT        DEFAULT ('')                                                NOT NULL, -- Action type, e.g. write/control/read
    title           TEXT        DEFAULT ('')                                                NOT NULL, -- Action title
    description     TEXT        DEFAULT ('')                                                NOT NULL, -- Action description shown to the operator
    payload         JSON        DEFAULT ('{}')                                              NOT NULL, -- Action payload, free-form structured data
    status          SMALLINT    DEFAULT 0                                                   NOT NULL, -- Action status flag
    expire_time     DATETIME(6),                                                                      -- Optional expiration deadline
    tenant_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    user_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- User ID
    remark          TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_action_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_model_provider
-- ----------------------------
CREATE TABLE dc3_model_provider
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    name          TEXT        DEFAULT ('')                                                NOT NULL, -- Provider name, unique
    provider_type SMALLINT    DEFAULT 0                                                   NOT NULL, -- Provider type, 0: openai-compatible, 1: anthropic
    base_url      TEXT        DEFAULT ('')                                                NOT NULL, -- API base URL
    api_key       TEXT        DEFAULT ('')                                                NOT NULL, -- API key
    default_flag  SMALLINT    DEFAULT 0                                                   NOT NULL, -- Default flag, 1: default, 0: not default
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
    CONSTRAINT chk_model_provider_provider_type CHECK (provider_type BETWEEN 0 AND 1),
    CONSTRAINT chk_model_provider_default_flag CHECK (default_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_model_provider_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_model_provider_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_model_config
-- ----------------------------
CREATE TABLE dc3_model_config
(
    id            BIGINT PRIMARY KEY                                                           NOT NULL, -- Primary key ID
    model         TEXT             DEFAULT ('')                                                NOT NULL, -- Model identifier
    label         TEXT             DEFAULT ('')                                                NOT NULL, -- Display label
    provider_id   BIGINT           DEFAULT 0                                                   NOT NULL, -- Foreign key to dc3_model_provider
    stream        BOOLEAN          DEFAULT TRUE                                                NOT NULL, -- Whether streaming responses are supported
    tool_call     BOOLEAN          DEFAULT TRUE                                                NOT NULL, -- Whether tool calling is supported
    vision        BOOLEAN          DEFAULT FALSE                                               NOT NULL, -- Whether vision input is supported
    reasoning     BOOLEAN          DEFAULT FALSE                                               NOT NULL, -- Whether reasoning mode is supported
    temperature   DOUBLE PRECISION DEFAULT 0.7                                                 NOT NULL, -- Default sampling temperature
    max_tokens    INTEGER          DEFAULT 2048                                                NOT NULL, -- Default maximum tokens
    default_flag  SMALLINT         DEFAULT 0                                                   NOT NULL, -- Default flag, 1: default, 0: not default
    enable_flag   SMALLINT         DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT           DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark        TEXT             DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT           DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT             DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6)      DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT           DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT             DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6)      DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT         DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_model_config_default_flag CHECK (default_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_model_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_model_config_deleted CHECK (deleted IN (0, 1))
);



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
        'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0);

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
        'Default DeepSeek V4 Pro model config', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3',
        '2026-05-01 00:00:00', 0);
