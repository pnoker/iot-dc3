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

CREATE SCHEMA IF NOT EXISTS dc3_manager;
SET search_path TO dc3_manager;

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
-- Table structure for dc3_label
-- ----------------------------
CREATE TABLE dc3_label
(
    id               BIGINT PRIMARY KEY         NOT NULL,         -- Primary key ID
    label_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Label name
    label_code       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Label code
    label_color      TEXT     DEFAULT '#F4F4F5' NOT NULL,         -- Label color
    entity_type_flag SMALLINT DEFAULT 0         NOT NULL,         -- Entity type flag
    enable_flag      SMALLINT DEFAULT 0         NOT NULL,         -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0         NOT NULL,         -- Tenant ID
    remark           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0         NOT NULL,         -- Creator ID
    creator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0         NOT NULL,         -- Operator ID
    operator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0         NOT NULL,         -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_label_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_label_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_label_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_label_active_unique ON dc3_label (tenant_id, entity_type_flag, label_name) WHERE deleted = 0 AND label_name <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_label
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_label IS 'Label table';
COMMENT
ON COLUMN dc3_label.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_label.label_name IS 'Label name';
COMMENT
ON COLUMN dc3_label.label_code IS 'Label code';
COMMENT
ON COLUMN dc3_label.label_color IS 'Label color';
COMMENT
ON COLUMN dc3_label.entity_type_flag IS 'Entity type flag';
COMMENT
ON COLUMN dc3_label.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_label.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_label.remark IS 'Description';
COMMENT
ON COLUMN dc3_label.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_label.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_label.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_label.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_label.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_label.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_label.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

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
    remark           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_label_bind_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_label_bind_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_label_bind_active_unique ON dc3_label_bind (tenant_id, entity_type_flag, label_id, entity_id) WHERE deleted = 0;
CREATE INDEX idx_label_bind_entity ON dc3_label_bind (tenant_id, entity_type_flag, entity_id) WHERE deleted = 0;
-- Delete guards and label detail paths check whether a label is still bound.
CREATE INDEX idx_label_bind_label_id ON dc3_label_bind (label_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_label_bind
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_label_bind IS 'Binding table between labels and entities';
COMMENT
ON COLUMN dc3_label_bind.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_label_bind.entity_type_flag IS 'Entity type flag';
COMMENT
ON COLUMN dc3_label_bind.label_id IS 'Label ID';
COMMENT
ON COLUMN dc3_label_bind.entity_id IS 'Entity ID';
COMMENT
ON COLUMN dc3_label_bind.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_label_bind.remark IS 'Description';
COMMENT
ON COLUMN dc3_label_bind.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_label_bind.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_label_bind.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_label_bind.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_label_bind.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_label_bind.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_label_bind.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_group
-- ----------------------------
CREATE TABLE dc3_group
(
    id               BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    parent_group_id  BIGINT   DEFAULT 0 NOT NULL,                 -- Parent group ID
    group_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Group name
    group_code       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Group code
    group_level      SMALLINT DEFAULT 0 NOT NULL,                 -- Group level
    group_index      SMALLINT DEFAULT 0 NOT NULL,                 -- Group order
    entity_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Entity type flag
    enable_flag      SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_group_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_group_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_group_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_group_parent_id ON dc3_group (tenant_id, parent_group_id) WHERE deleted = 0;
CREATE UNIQUE INDEX idx_group_active_unique ON dc3_group (tenant_id, entity_type_flag, parent_group_id, group_name) WHERE deleted = 0 AND group_name <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_group
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_group IS 'Group table';
COMMENT
ON COLUMN dc3_group.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_group.parent_group_id IS 'Parent group ID';
COMMENT
ON COLUMN dc3_group.group_name IS 'Group name';
COMMENT
ON COLUMN dc3_group.group_code IS 'Group code';
COMMENT
ON COLUMN dc3_group.group_level IS 'Group level';
COMMENT
ON COLUMN dc3_group.group_index IS 'Group order';
COMMENT
ON COLUMN dc3_group.entity_type_flag IS 'Entity type flag';
COMMENT
ON COLUMN dc3_group.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_group.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_group.remark IS 'Description';
COMMENT
ON COLUMN dc3_group.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_group.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_group.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_group.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_group.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_group.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_group.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

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
    remark           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_group_bind_entity_type_flag CHECK (entity_type_flag BETWEEN 0 AND 8),
    CONSTRAINT chk_group_bind_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_group_bind_entity_unique ON dc3_group_bind (tenant_id, entity_type_flag, entity_id) WHERE deleted = 0;
CREATE INDEX idx_group_bind_group_id ON dc3_group_bind (group_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_group_bind
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_group_bind IS 'Binding table between groups and entities';
COMMENT
ON COLUMN dc3_group_bind.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_group_bind.entity_type_flag IS 'Entity type flag';
COMMENT
ON COLUMN dc3_group_bind.group_id IS 'Group ID';
COMMENT
ON COLUMN dc3_group_bind.entity_id IS 'Entity ID';
COMMENT
ON COLUMN dc3_group_bind.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_group_bind.remark IS 'Description';
COMMENT
ON COLUMN dc3_group_bind.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_group_bind.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_group_bind.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_group_bind.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_group_bind.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_group_bind.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_group_bind.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';
