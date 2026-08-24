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

CREATE DATABASE IF NOT EXISTS dc3_manager
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE dc3_manager;

-- ----------------------------
-- Function for update operate time
-- ----------------------------

-- ----------------------------
-- Table structure for dc3_label
-- ----------------------------
CREATE TABLE dc3_label
(
    id               BIGINT PRIMARY KEY         NOT NULL,         -- Primary key ID
    label_name       TEXT     DEFAULT ('')          NOT NULL, -- Label name
    label_code       TEXT     DEFAULT ('')          NOT NULL, -- Label code
    label_color      TEXT     DEFAULT ('#F4F4F5') NOT NULL,         -- Label color
    entity_type_flag SMALLINT DEFAULT 0         NOT NULL,         -- Entity type flag
    enable_flag      SMALLINT DEFAULT 0         NOT NULL,         -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0         NOT NULL,         -- Tenant ID
    remark           TEXT     DEFAULT ('')          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0         NOT NULL,         -- Creator ID
    creator_name     TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0         NOT NULL,         -- Operator ID
    operator_name    TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0         NOT NULL,         -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_label_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_label_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_label_deleted CHECK (deleted IN (0, 1))
);





-- ----------------------------
-- Table structure for dc3_label_bind
-- ----------------------------
CREATE TABLE dc3_label_bind
(
    id               BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    entity_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Entity type flag
    label_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Label ID
    entity_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Entity ID
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark           TEXT     DEFAULT ('')          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_label_bind_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_label_bind_deleted CHECK (deleted IN (0, 1))
);



-- Delete guards and label detail paths check whether a label is still bound.




-- ----------------------------
-- Table structure for dc3_group
-- ----------------------------
CREATE TABLE dc3_group
(
    id               BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    parent_group_id  BIGINT   DEFAULT 0 NOT NULL,                 -- Parent group ID
    group_name       TEXT     DEFAULT ('')          NOT NULL, -- Group name
    group_code       TEXT     DEFAULT ('')          NOT NULL, -- Group code
    group_level      SMALLINT DEFAULT 0 NOT NULL,                 -- Group level
    group_index      SMALLINT DEFAULT 0 NOT NULL,                 -- Group order
    entity_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Entity type flag
    enable_flag      SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark           TEXT     DEFAULT ('')          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_group_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_group_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_group_deleted CHECK (deleted IN (0, 1))
);






-- ----------------------------
-- Table structure for dc3_group_bind
-- ----------------------------
CREATE TABLE dc3_group_bind
(
    id               BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    entity_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Entity type flag
    group_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Group ID
    entity_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Entity ID
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark           TEXT     DEFAULT ('')          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_group_bind_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_group_bind_deleted CHECK (deleted IN (0, 1))
);





