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
-- Table structure for dc3_driver
-- ----------------------------
CREATE TABLE dc3_driver
(
    id               BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    driver_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Driver name
    driver_code      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Driver code
    service_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Service name
    service_host     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Service host
    driver_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Driver type flag
    driver_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Driver extension information
    enable_flag      SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    signature        TEXT     DEFAULT ''::TEXT          NOT NULL, -- Signature
    version          INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_driver_type_flag CHECK (driver_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_driver_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_driver_tenant_code_active_unique ON dc3_driver (tenant_id, driver_code) WHERE deleted = 0 AND driver_code <> ''::TEXT;
CREATE INDEX idx_driver_tenant_service_name ON dc3_driver (tenant_id, service_name) WHERE deleted = 0 AND service_name <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_driver
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_driver IS 'Driver table';
COMMENT
ON COLUMN dc3_driver.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_driver.driver_name IS 'Driver name';
COMMENT
ON COLUMN dc3_driver.driver_code IS 'Driver code';
COMMENT
ON COLUMN dc3_driver.service_name IS 'Service name';
COMMENT
ON COLUMN dc3_driver.service_host IS 'Service host';
COMMENT
ON COLUMN dc3_driver.driver_type_flag IS 'Driver type flag';
COMMENT
ON COLUMN dc3_driver.driver_ext IS 'Driver extension information';
COMMENT
ON COLUMN dc3_driver.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_driver.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_driver.remark IS 'Description';
COMMENT
ON COLUMN dc3_driver.signature IS 'Signature';
COMMENT
ON COLUMN dc3_driver.version IS 'Version';
COMMENT
ON COLUMN dc3_driver.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_driver.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_driver.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_driver.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_driver.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_driver.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_driver.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_driver_attribute
-- ----------------------------
CREATE TABLE dc3_driver_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Driver attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    signature           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_driver_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_attribute_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_driver_attribute_active_unique ON dc3_driver_attribute (tenant_id, driver_id, attribute_code) WHERE deleted = 0 AND attribute_code <> ''::TEXT;
CREATE INDEX idx_driver_attribute_driver_id ON dc3_driver_attribute (driver_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_driver_attribute
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_driver_attribute IS 'Driver attribute table';
COMMENT
ON COLUMN dc3_driver_attribute.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_driver_attribute.attribute_name IS 'Attribute name';
COMMENT
ON COLUMN dc3_driver_attribute.attribute_code IS 'Attribute code';
COMMENT
ON COLUMN dc3_driver_attribute.attribute_type_flag IS 'Attribute type flag';
COMMENT
ON COLUMN dc3_driver_attribute.default_value IS 'Default value';
COMMENT
ON COLUMN dc3_driver_attribute.driver_id IS 'Driver ID';
COMMENT
ON COLUMN dc3_driver_attribute.attribute_ext IS 'Driver attribute extension information';
COMMENT
ON COLUMN dc3_driver_attribute.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_driver_attribute.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_driver_attribute.remark IS 'Description';
COMMENT
ON COLUMN dc3_driver_attribute.signature IS 'Signature';
COMMENT
ON COLUMN dc3_driver_attribute.version IS 'Version';
COMMENT
ON COLUMN dc3_driver_attribute.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_driver_attribute.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_driver_attribute.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_driver_attribute.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_driver_attribute.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_driver_attribute.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_driver_attribute.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_point_attribute
-- ----------------------------
CREATE TABLE dc3_point_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Point attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    signature           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_point_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_point_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_point_attribute_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_point_attribute_active_unique ON dc3_point_attribute (tenant_id, driver_id, attribute_code) WHERE deleted = 0 AND attribute_code <> ''::TEXT;
CREATE INDEX idx_point_attribute_driver_id ON dc3_point_attribute (driver_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_point_attribute
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_point_attribute IS 'Point attribute table';
COMMENT
ON COLUMN dc3_point_attribute.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_point_attribute.attribute_name IS 'Attribute name';
COMMENT
ON COLUMN dc3_point_attribute.attribute_code IS 'Attribute code';
COMMENT
ON COLUMN dc3_point_attribute.attribute_type_flag IS 'Attribute type flag';
COMMENT
ON COLUMN dc3_point_attribute.default_value IS 'Default value';
COMMENT
ON COLUMN dc3_point_attribute.driver_id IS 'Driver ID';
COMMENT
ON COLUMN dc3_point_attribute.attribute_ext IS 'Point attribute extension information';
COMMENT
ON COLUMN dc3_point_attribute.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_point_attribute.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_point_attribute.remark IS 'Description';
COMMENT
ON COLUMN dc3_point_attribute.signature IS 'Signature';
COMMENT
ON COLUMN dc3_point_attribute.version IS 'Version';
COMMENT
ON COLUMN dc3_point_attribute.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_point_attribute.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_point_attribute.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_point_attribute.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_point_attribute.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_point_attribute.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_point_attribute.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_command_attribute
-- ----------------------------
CREATE TABLE dc3_command_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Command attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    signature           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_command_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_attribute_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_command_attribute_active_unique ON dc3_command_attribute (tenant_id, driver_id, attribute_code) WHERE deleted = 0 AND attribute_code <> ''::TEXT;
CREATE INDEX idx_command_attribute_driver_id ON dc3_command_attribute (driver_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_command_attribute
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_command_attribute IS 'Command attribute table';
COMMENT
ON COLUMN dc3_command_attribute.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_command_attribute.attribute_name IS 'Attribute name';
COMMENT
ON COLUMN dc3_command_attribute.attribute_code IS 'Attribute code';
COMMENT
ON COLUMN dc3_command_attribute.attribute_type_flag IS 'Attribute type flag';
COMMENT
ON COLUMN dc3_command_attribute.default_value IS 'Default value';
COMMENT
ON COLUMN dc3_command_attribute.driver_id IS 'Driver ID';
COMMENT
ON COLUMN dc3_command_attribute.attribute_ext IS 'Command attribute extension information';
COMMENT
ON COLUMN dc3_command_attribute.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_command_attribute.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_command_attribute.remark IS 'Description';
COMMENT
ON COLUMN dc3_command_attribute.signature IS 'Signature';
COMMENT
ON COLUMN dc3_command_attribute.version IS 'Version';
COMMENT
ON COLUMN dc3_command_attribute.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_command_attribute.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_command_attribute.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_command_attribute.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_command_attribute.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_command_attribute.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_command_attribute.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_event_attribute
-- ----------------------------
CREATE TABLE dc3_event_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Event attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    signature           TEXT     DEFAULT ''::TEXT          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_event_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_attribute_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_event_attribute_active_unique ON dc3_event_attribute (tenant_id, driver_id, attribute_code) WHERE deleted = 0 AND attribute_code <> ''::TEXT;
CREATE INDEX idx_event_attribute_driver_id ON dc3_event_attribute (driver_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_event_attribute
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_event_attribute IS 'Event attribute table';
COMMENT
ON COLUMN dc3_event_attribute.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_event_attribute.attribute_name IS 'Attribute name';
COMMENT
ON COLUMN dc3_event_attribute.attribute_code IS 'Attribute code';
COMMENT
ON COLUMN dc3_event_attribute.attribute_type_flag IS 'Attribute type flag';
COMMENT
ON COLUMN dc3_event_attribute.default_value IS 'Default value';
COMMENT
ON COLUMN dc3_event_attribute.driver_id IS 'Driver ID';
COMMENT
ON COLUMN dc3_event_attribute.attribute_ext IS 'Event attribute extension information';
COMMENT
ON COLUMN dc3_event_attribute.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_event_attribute.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_event_attribute.remark IS 'Description';
COMMENT
ON COLUMN dc3_event_attribute.signature IS 'Signature';
COMMENT
ON COLUMN dc3_event_attribute.version IS 'Version';
COMMENT
ON COLUMN dc3_event_attribute.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_event_attribute.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_event_attribute.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_event_attribute.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_event_attribute.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_event_attribute.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_event_attribute.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_profile
-- ----------------------------
CREATE TABLE dc3_profile
(
    id                 BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    profile_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Profile name
    profile_code       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Profile code
    profile_share_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Profile sharing type flag
    profile_type_flag  SMALLINT DEFAULT 2 NOT NULL,                 -- Profile type flag
    profile_ext        JSON     DEFAULT '{}'::JSON        NOT NULL, -- Profile extension information
    enable_flag        SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark             TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    signature          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Signature
    version            INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Creation time
    operator_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,    -- Operation time
    deleted            SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_profile_profile_share_flag CHECK (profile_share_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_profile_profile_type_flag CHECK (profile_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_profile_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_profile_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_profile_tenant_code_active_unique ON dc3_profile (tenant_id, profile_code) WHERE deleted = 0 AND profile_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_profile
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_profile IS 'Profile table';
COMMENT
ON COLUMN dc3_profile.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_profile.profile_name IS 'Profile name';
COMMENT
ON COLUMN dc3_profile.profile_code IS 'Profile code';
COMMENT
ON COLUMN dc3_profile.profile_share_flag IS 'Profile sharing type flag';
COMMENT
ON COLUMN dc3_profile.profile_type_flag IS 'Profile type flag';
COMMENT
ON COLUMN dc3_profile.profile_ext IS 'Profile extension information';
COMMENT
ON COLUMN dc3_profile.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_profile.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_profile.remark IS 'Description';
COMMENT
ON COLUMN dc3_profile.signature IS 'Signature';
COMMENT
ON COLUMN dc3_profile.version IS 'Version';
COMMENT
ON COLUMN dc3_profile.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_profile.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_profile.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_profile.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_profile.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_profile.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_profile.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_point
-- ----------------------------
CREATE TABLE dc3_point
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    point_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Point name
    point_code      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Point code
    point_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Point type flag
    rw_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Read/write flag
    base_value      REAL     DEFAULT 0 NOT NULL,                 -- Base value
    multiple        REAL     DEFAULT 1 NOT NULL,                 -- Multiplier
    value_decimal   SMALLINT DEFAULT 6 NOT NULL,                 -- Value precision
    unit            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Unit
    profile_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Profile ID
    point_ext       JSON     DEFAULT '{}'::JSON        NOT NULL, -- Point extension information
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    signature       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Signature
    version         INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_point_point_type_flag CHECK (point_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_point_rw_flag CHECK (rw_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_point_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_point_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_point_profile_code_active_unique ON dc3_point (tenant_id, profile_id, point_code) WHERE deleted = 0 AND point_code <> ''::TEXT;
CREATE INDEX idx_point_profile_id ON dc3_point (profile_id) WHERE deleted = 0;
-- Supports listByProfileId queries with tenant scoping.
CREATE INDEX idx_point_tenant_profile ON dc3_point (tenant_id, profile_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_point
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_point IS 'Point table';
COMMENT
ON COLUMN dc3_point.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_point.point_name IS 'Point name';
COMMENT
ON COLUMN dc3_point.point_code IS 'Point code';
COMMENT
ON COLUMN dc3_point.point_type_flag IS 'Point type flag';
COMMENT
ON COLUMN dc3_point.rw_flag IS 'Read/write flag';
COMMENT
ON COLUMN dc3_point.base_value IS 'Base value';
COMMENT
ON COLUMN dc3_point.multiple IS 'Multiplier';
COMMENT
ON COLUMN dc3_point.value_decimal IS 'Value precision';
COMMENT
ON COLUMN dc3_point.unit IS 'Unit';
COMMENT
ON COLUMN dc3_point.profile_id IS 'Profile ID';
COMMENT
ON COLUMN dc3_point.point_ext IS 'Point extension information';
COMMENT
ON COLUMN dc3_point.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_point.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_point.remark IS 'Description';
COMMENT
ON COLUMN dc3_point.signature IS 'Signature';
COMMENT
ON COLUMN dc3_point.version IS 'Version';
COMMENT
ON COLUMN dc3_point.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_point.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_point.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_point.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_point.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_point.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_point.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_device
-- ----------------------------
CREATE TABLE dc3_device
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    device_name   TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Device name
    device_code   TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Device code
    driver_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Driver ID
    profile_id    BIGINT,                                        -- Profile ID (device belongs to single profile)
    device_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Device extension information
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_device_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_device_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_device_tenant_code_active_unique ON dc3_device (tenant_id, device_code) WHERE deleted = 0 AND device_code <> ''::TEXT;
CREATE INDEX idx_device_driver_id ON dc3_device (driver_id) WHERE deleted = 0;
CREATE INDEX idx_device_profile ON dc3_device (tenant_id, profile_id) WHERE deleted = 0;
-- Supports listByDriverId queries with tenant scoping.
CREATE INDEX idx_device_tenant_driver ON dc3_device (tenant_id, driver_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_device
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_device IS 'Device table';
COMMENT
ON COLUMN dc3_device.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_device.device_name IS 'Device name';
COMMENT
ON COLUMN dc3_device.device_code IS 'Device code';
COMMENT
ON COLUMN dc3_device.driver_id IS 'Driver ID';
COMMENT
ON COLUMN dc3_device.profile_id IS 'Profile ID';
COMMENT
ON COLUMN dc3_device.device_ext IS 'Device extension information';
COMMENT
ON COLUMN dc3_device.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_device.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_device.remark IS 'Description';
COMMENT
ON COLUMN dc3_device.signature IS 'Signature';
COMMENT
ON COLUMN dc3_device.version IS 'Version';
COMMENT
ON COLUMN dc3_device.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_device.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_device.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_device.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_device.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_device.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_device.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_driver_attribute_config
-- ----------------------------
CREATE TABLE dc3_driver_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Driver attribute ID
    config_value  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Driver configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Driver configuration information
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_attribute_config_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_driver_attribute_config_active_unique ON dc3_driver_attribute_config (tenant_id, device_id, attribute_id) WHERE deleted = 0;
CREATE INDEX idx_driver_attribute_config_attribute_id ON dc3_driver_attribute_config (attribute_id) WHERE deleted = 0;
-- Driver metadata assembly lists all driver attribute configs for a device.
CREATE INDEX idx_driver_attribute_config_device_id ON dc3_driver_attribute_config (device_id) WHERE deleted = 0;
CREATE INDEX idx_driver_attr_config_device ON dc3_driver_attribute_config (tenant_id, device_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_driver_attribute_config
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_driver_attribute_config IS 'Driver attribute configuration table';
COMMENT
ON COLUMN dc3_driver_attribute_config.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_driver_attribute_config.attribute_id IS 'Driver attribute ID';
COMMENT
ON COLUMN dc3_driver_attribute_config.config_value IS 'Driver configuration value';
COMMENT
ON COLUMN dc3_driver_attribute_config.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_driver_attribute_config.config_ext IS 'Driver configuration information';
COMMENT
ON COLUMN dc3_driver_attribute_config.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_driver_attribute_config.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_driver_attribute_config.remark IS 'Description';
COMMENT
ON COLUMN dc3_driver_attribute_config.signature IS 'Signature';
COMMENT
ON COLUMN dc3_driver_attribute_config.version IS 'Version';
COMMENT
ON COLUMN dc3_driver_attribute_config.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_driver_attribute_config.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_driver_attribute_config.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_driver_attribute_config.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_driver_attribute_config.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_driver_attribute_config.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_driver_attribute_config.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_point_attribute_config
-- ----------------------------
CREATE TABLE dc3_point_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Point attribute ID
    config_value  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Point configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Point configuration information
    point_id      BIGINT   DEFAULT 0 NOT NULL,                   -- Point ID
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_point_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_point_attribute_config_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_point_attribute_config_active_unique ON dc3_point_attribute_config (tenant_id, device_id, point_id, attribute_id) WHERE deleted = 0;
CREATE INDEX idx_point_attribute_config_attribute_id ON dc3_point_attribute_config (attribute_id) WHERE deleted = 0;
CREATE INDEX idx_point_attribute_config_device_point ON dc3_point_attribute_config (device_id, point_id) WHERE deleted = 0;
CREATE INDEX idx_point_attr_config_device ON dc3_point_attribute_config (tenant_id, device_id, point_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_point_attribute_config
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_point_attribute_config IS 'Point attribute configuration table';
COMMENT
ON COLUMN dc3_point_attribute_config.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_point_attribute_config.attribute_id IS 'Point attribute ID';
COMMENT
ON COLUMN dc3_point_attribute_config.config_value IS 'Point configuration value';
COMMENT
ON COLUMN dc3_point_attribute_config.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_point_attribute_config.config_ext IS 'Point configuration information';
COMMENT
ON COLUMN dc3_point_attribute_config.point_id IS 'Point ID';
COMMENT
ON COLUMN dc3_point_attribute_config.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_point_attribute_config.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_point_attribute_config.remark IS 'Description';
COMMENT
ON COLUMN dc3_point_attribute_config.signature IS 'Signature';
COMMENT
ON COLUMN dc3_point_attribute_config.version IS 'Version';
COMMENT
ON COLUMN dc3_point_attribute_config.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_point_attribute_config.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_point_attribute_config.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_point_attribute_config.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_point_attribute_config.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_point_attribute_config.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_point_attribute_config.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_command_attribute_config
-- ----------------------------
CREATE TABLE dc3_command_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Command attribute ID
    config_value  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Command configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Command configuration information
    command_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Command ID
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_attribute_config_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_command_attribute_config_active_unique ON dc3_command_attribute_config (tenant_id, device_id, command_id, attribute_id) WHERE deleted = 0;
CREATE INDEX idx_command_attribute_config_attribute_id ON dc3_command_attribute_config (attribute_id) WHERE deleted = 0;
CREATE INDEX idx_command_attribute_config_device_command ON dc3_command_attribute_config (device_id, command_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_command_attribute_config
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_command_attribute_config IS 'Command attribute configuration table';
COMMENT
ON COLUMN dc3_command_attribute_config.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_command_attribute_config.attribute_id IS 'Command attribute ID';
COMMENT
ON COLUMN dc3_command_attribute_config.config_value IS 'Command configuration value';
COMMENT
ON COLUMN dc3_command_attribute_config.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_command_attribute_config.config_ext IS 'Command configuration information';
COMMENT
ON COLUMN dc3_command_attribute_config.command_id IS 'Command ID';
COMMENT
ON COLUMN dc3_command_attribute_config.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_command_attribute_config.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_command_attribute_config.remark IS 'Description';
COMMENT
ON COLUMN dc3_command_attribute_config.signature IS 'Signature';
COMMENT
ON COLUMN dc3_command_attribute_config.version IS 'Version';
COMMENT
ON COLUMN dc3_command_attribute_config.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_command_attribute_config.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_command_attribute_config.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_command_attribute_config.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_command_attribute_config.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_command_attribute_config.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_command_attribute_config.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_event_attribute_config
-- ----------------------------
CREATE TABLE dc3_event_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Event attribute ID
    config_value  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Event configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Event configuration information
    event_id      BIGINT   DEFAULT 0 NOT NULL,                   -- Event ID
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_attribute_config_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_event_attribute_config_active_unique ON dc3_event_attribute_config (tenant_id, device_id, event_id, attribute_id) WHERE deleted = 0;
CREATE INDEX idx_event_attribute_config_attribute_id ON dc3_event_attribute_config (attribute_id) WHERE deleted = 0;
CREATE INDEX idx_event_attribute_config_device_event ON dc3_event_attribute_config (device_id, event_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_event_attribute_config
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_event_attribute_config IS 'Event attribute configuration table';
COMMENT
ON COLUMN dc3_event_attribute_config.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_event_attribute_config.attribute_id IS 'Event attribute ID';
COMMENT
ON COLUMN dc3_event_attribute_config.config_value IS 'Event configuration value';
COMMENT
ON COLUMN dc3_event_attribute_config.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_event_attribute_config.config_ext IS 'Event configuration information';
COMMENT
ON COLUMN dc3_event_attribute_config.event_id IS 'Event ID';
COMMENT
ON COLUMN dc3_event_attribute_config.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_event_attribute_config.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_event_attribute_config.remark IS 'Description';
COMMENT
ON COLUMN dc3_event_attribute_config.signature IS 'Signature';
COMMENT
ON COLUMN dc3_event_attribute_config.version IS 'Version';
COMMENT
ON COLUMN dc3_event_attribute_config.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_event_attribute_config.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_event_attribute_config.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_event_attribute_config.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_event_attribute_config.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_event_attribute_config.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_event_attribute_config.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_command
-- ----------------------------
CREATE TABLE dc3_command
(
    id                BIGINT PRIMARY KEY  NOT NULL,              -- Primary key ID
    command_name      TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Command name
    command_code      TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Command code
    command_type_flag SMALLINT DEFAULT 0  NOT NULL,              -- Command type flag, 0: custom, 1: config, 2: action
    call_type_flag    SMALLINT DEFAULT 0  NOT NULL,              -- Call type flag, 0: sync, 1: async
    timeout           INTEGER  DEFAULT 30 NOT NULL,              -- Timeout in seconds
    command_ext       JSON     DEFAULT '{}'::JSON     NOT NULL,  -- Command extension information
    enable_flag       SMALLINT DEFAULT 0  NOT NULL,              -- Enable flag, 0: enabled, 1: disabled
    tenant_id         BIGINT   DEFAULT 0  NOT NULL,              -- Tenant ID
    profile_id        BIGINT   DEFAULT 0  NOT NULL,              -- Profile ID
    remark            TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Description
    signature         TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Signature
    version           INTEGER  DEFAULT 0  NOT NULL,              -- Version
    creator_id        BIGINT   DEFAULT 0  NOT NULL,              -- Creator ID
    creator_name      TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id       BIGINT   DEFAULT 0  NOT NULL,              -- Operator ID
    operator_name     TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted           SMALLINT DEFAULT 0  NOT NULL,              -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_command_type_flag CHECK (command_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_command_call_type_flag CHECK (call_type_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_command_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_command_tenant_profile_code_active_unique ON dc3_command (tenant_id, profile_id, command_code) WHERE deleted = 0 AND command_code <> ''::TEXT;
-- Profile and device detail pages load command metadata by profile, then tenant and enable flag.
CREATE INDEX idx_command_profile_id ON dc3_command (profile_id, tenant_id, enable_flag) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_command
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_command IS 'Command table';
COMMENT
ON COLUMN dc3_command.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_command.command_name IS 'Command name';
COMMENT
ON COLUMN dc3_command.command_code IS 'Command code';
COMMENT
ON COLUMN dc3_command.command_type_flag IS 'Command type flag, 0: custom, 1: config, 2: action';
COMMENT
ON COLUMN dc3_command.call_type_flag IS 'Call type flag, 0: sync, 1: async';
COMMENT
ON COLUMN dc3_command.timeout IS 'Timeout in seconds';
COMMENT
ON COLUMN dc3_command.command_ext IS 'Command extension information';
COMMENT
ON COLUMN dc3_command.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_command.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_command.profile_id IS 'Profile ID';
COMMENT
ON COLUMN dc3_command.remark IS 'Description';
COMMENT
ON COLUMN dc3_command.signature IS 'Signature';
COMMENT
ON COLUMN dc3_command.version IS 'Version';
COMMENT
ON COLUMN dc3_command.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_command.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_command.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_command.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_command.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_command.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_command.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_command_param
-- ----------------------------
CREATE TABLE dc3_command_param
(
    id                   BIGINT PRIMARY KEY NOT NULL,              -- Primary key ID
    param_name           TEXT     DEFAULT ''::TEXT       NOT NULL, -- Param name
    param_code           TEXT     DEFAULT ''::TEXT       NOT NULL, -- Param code
    param_direction_flag SMALLINT DEFAULT 0 NOT NULL,              -- Param direction flag, 0: input, 1: output
    param_type_flag      SMALLINT DEFAULT 0 NOT NULL,              -- Param type flag
    required_flag        SMALLINT DEFAULT 0 NOT NULL,              -- Required flag, 0: no, 1: yes
    default_value        TEXT     DEFAULT ''::TEXT       NOT NULL, -- Default value
    param_ext            JSON     DEFAULT '{}'::JSON     NOT NULL, -- Param extension information
    enable_flag          SMALLINT DEFAULT 0 NOT NULL,              -- Enable flag, 0: enabled, 1: disabled
    tenant_id            BIGINT   DEFAULT 0 NOT NULL,              -- Tenant ID
    command_id           BIGINT   DEFAULT 0 NOT NULL,              -- Command ID
    remark               TEXT     DEFAULT ''::TEXT       NOT NULL, -- Description
    signature            TEXT     DEFAULT ''::TEXT       NOT NULL, -- Signature
    version              INTEGER  DEFAULT 0 NOT NULL,              -- Version
    creator_id           BIGINT   DEFAULT 0 NOT NULL,              -- Creator ID
    creator_name         TEXT     DEFAULT ''::TEXT       NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,    -- Creation time
    operator_id          BIGINT   DEFAULT 0 NOT NULL,              -- Operator ID
    operator_name        TEXT     DEFAULT ''::TEXT       NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Operation time
    deleted              SMALLINT DEFAULT 0 NOT NULL,              -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_param_param_direction_flag CHECK (param_direction_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_command_param_required_flag CHECK (required_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_command_param_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_param_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_command_param_active_unique ON dc3_command_param (tenant_id, command_id, param_code) WHERE deleted = 0 AND param_code <> ''::TEXT;
-- Command detail and cascade-delete paths list params by command_id.
CREATE INDEX idx_command_param_command_id ON dc3_command_param (command_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_command_param
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_command_param IS 'Command param table';
COMMENT
ON COLUMN dc3_command_param.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_command_param.param_name IS 'Param name';
COMMENT
ON COLUMN dc3_command_param.param_code IS 'Param code';
COMMENT
ON COLUMN dc3_command_param.param_direction_flag IS 'Param direction flag, 0: input, 1: output';
COMMENT
ON COLUMN dc3_command_param.param_type_flag IS 'Param type flag';
COMMENT
ON COLUMN dc3_command_param.required_flag IS 'Required flag, 0: no, 1: yes';
COMMENT
ON COLUMN dc3_command_param.default_value IS 'Default value';
COMMENT
ON COLUMN dc3_command_param.param_ext IS 'Param extension information';
COMMENT
ON COLUMN dc3_command_param.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_command_param.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_command_param.command_id IS 'Command ID';
COMMENT
ON COLUMN dc3_command_param.remark IS 'Description';
COMMENT
ON COLUMN dc3_command_param.signature IS 'Signature';
COMMENT
ON COLUMN dc3_command_param.version IS 'Version';
COMMENT
ON COLUMN dc3_command_param.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_command_param.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_command_param.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_command_param.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_command_param.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_command_param.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_command_param.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_event
-- ----------------------------
CREATE TABLE dc3_event
(
    id               BIGINT PRIMARY KEY NOT NULL,                -- Primary key ID
    event_name       TEXT     DEFAULT ''::TEXT       NOT NULL,   -- Event name
    event_code       TEXT     DEFAULT ''::TEXT       NOT NULL,   -- Event code
    event_type_flag  SMALLINT DEFAULT 0 NOT NULL,                -- Event type flag, 0: info, 1: alert, 2: fault, 3: lifecycle
    event_level_flag SMALLINT DEFAULT 0 NOT NULL,                -- Event level flag, 0: low, 1: medium, 2: high, 3: critical
    event_ext        JSON     DEFAULT '{}'::JSON     NOT NULL,   -- Event extension information
    enable_flag      SMALLINT DEFAULT 0 NOT NULL,                -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                -- Tenant ID
    profile_id       BIGINT   DEFAULT 0 NOT NULL,                -- Profile ID
    remark           TEXT     DEFAULT ''::TEXT       NOT NULL,   -- Description
    signature        TEXT     DEFAULT ''::TEXT       NOT NULL,   -- Signature
    version          INTEGER  DEFAULT 0 NOT NULL,                -- Version
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                -- Creator ID
    creator_name     TEXT     DEFAULT ''::TEXT       NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                -- Operator ID
    operator_name    TEXT     DEFAULT ''::TEXT       NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_event_type_flag CHECK (event_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_event_event_level_flag CHECK (event_level_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_event_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_event_tenant_profile_code_active_unique ON dc3_event (tenant_id, profile_id, event_code) WHERE deleted = 0 AND event_code <> ''::TEXT;
-- Profile and device detail pages load event metadata by profile, then tenant and enable flag.
CREATE INDEX idx_event_profile_id ON dc3_event (profile_id, tenant_id, enable_flag) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_event
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_event IS 'Event table';
COMMENT
ON COLUMN dc3_event.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_event.event_name IS 'Event name';
COMMENT
ON COLUMN dc3_event.event_code IS 'Event code';
COMMENT
ON COLUMN dc3_event.event_type_flag IS 'Event type flag, 0: info, 1: alert, 2: fault, 3: lifecycle';
COMMENT
ON COLUMN dc3_event.event_level_flag IS 'Event level flag, 0: low, 1: medium, 2: high, 3: critical';
COMMENT
ON COLUMN dc3_event.event_ext IS 'Event extension information';
COMMENT
ON COLUMN dc3_event.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_event.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_event.profile_id IS 'Profile ID';
COMMENT
ON COLUMN dc3_event.remark IS 'Description';
COMMENT
ON COLUMN dc3_event.signature IS 'Signature';
COMMENT
ON COLUMN dc3_event.version IS 'Version';
COMMENT
ON COLUMN dc3_event.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_event.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_event.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_event.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_event.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_event.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_event.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_event_param
-- ----------------------------
CREATE TABLE dc3_event_param
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    param_name      TEXT     DEFAULT ''::TEXT       NOT NULL,    -- Param name
    param_code      TEXT     DEFAULT ''::TEXT       NOT NULL,    -- Param code
    param_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Param type flag
    param_ext       JSON     DEFAULT '{}'::JSON     NOT NULL,    -- Param extension information
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    event_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Event ID
    remark          TEXT     DEFAULT ''::TEXT       NOT NULL,    -- Description
    signature       TEXT     DEFAULT ''::TEXT       NOT NULL,    -- Signature
    version         INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ''::TEXT       NOT NULL,    -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ''::TEXT       NOT NULL,    -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_param_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_param_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_event_param_active_unique ON dc3_event_param (tenant_id, event_id, param_code) WHERE deleted = 0 AND param_code <> ''::TEXT;
-- Event detail and cascade-delete paths list params by event_id.
CREATE INDEX idx_event_param_event_id ON dc3_event_param (event_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_event_param
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_event_param IS 'Event param table';
COMMENT
ON COLUMN dc3_event_param.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_event_param.param_name IS 'Param name';
COMMENT
ON COLUMN dc3_event_param.param_code IS 'Param code';
COMMENT
ON COLUMN dc3_event_param.param_type_flag IS 'Param type flag';
COMMENT
ON COLUMN dc3_event_param.param_ext IS 'Param extension information';
COMMENT
ON COLUMN dc3_event_param.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_event_param.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_event_param.event_id IS 'Event ID';
COMMENT
ON COLUMN dc3_event_param.remark IS 'Description';
COMMENT
ON COLUMN dc3_event_param.signature IS 'Signature';
COMMENT
ON COLUMN dc3_event_param.version IS 'Version';
COMMENT
ON COLUMN dc3_event_param.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_event_param.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_event_param.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_event_param.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_event_param.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_event_param.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_event_param.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';
