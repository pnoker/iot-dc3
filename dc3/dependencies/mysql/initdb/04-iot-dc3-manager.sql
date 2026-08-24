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
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dc3_manager;

-- ----------------------------
-- Function for update operate time
-- ----------------------------

-- ----------------------------
-- Table structure for dc3_driver
-- ----------------------------
CREATE TABLE dc3_driver
(
    id               BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    driver_name      TEXT     DEFAULT ('')          NOT NULL, -- Driver name
    driver_code      TEXT     DEFAULT ('')          NOT NULL, -- Driver code
    service_name     TEXT     DEFAULT ('')          NOT NULL, -- Service name
    service_host     TEXT     DEFAULT ('')          NOT NULL, -- Service host
    driver_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Driver type flag
    driver_ext       JSON     DEFAULT ('{}')        NOT NULL, -- Driver extension information
    enable_flag      SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark           TEXT     DEFAULT ('')          NOT NULL, -- Description
    signature        TEXT     DEFAULT ('')          NOT NULL, -- Signature
    version          INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name     TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,   -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name    TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,  -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_driver_type_flag CHECK (driver_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_driver_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_deleted CHECK (deleted IN (0, 1))
);






-- ----------------------------
-- Driver runtime instances and device ownership
-- ----------------------------
-- dc3_driver is the logical protocol definition. Runtime replicas are recorded
-- separately so registering a second pod never overwrites the first pod's identity.
CREATE TABLE dc3_driver_instance
(
    tenant_id      BIGINT      NOT NULL,                            -- Tenant ID
    driver_id      BIGINT      NOT NULL,                            -- Logical driver ID
    node_id        VARCHAR(191)        NOT NULL,                            -- Stable runtime node ID
    client_id      VARCHAR(191)        NOT NULL,                            -- Unique messaging client ID
    service_host   TEXT        NOT NULL,                            -- Runtime service host
    started_at     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Runtime start time
    last_heartbeat DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Last successful heartbeat time
    lease_until    DATETIME(6) NOT NULL,                            -- Runtime lease expiration time
    PRIMARY KEY (tenant_id, driver_id, node_id),
    UNIQUE (tenant_id, client_id)
);

CREATE INDEX idx_driver_instance_active
    ON dc3_driver_instance (tenant_id, driver_id, lease_until DESC);

CREATE TABLE dc3_device_lease
(
    tenant_id    BIGINT      NOT NULL,                                             -- Tenant ID
    driver_id    BIGINT      NOT NULL,                                             -- Logical driver ID
    device_id    BIGINT      NOT NULL,                                             -- Device ID
    owner_node   VARCHAR(191)        NOT NULL,                                             -- Owning runtime node ID
    fencing_token BIGINT     DEFAULT 1 NOT NULL, -- Monotonic ownership fencing token (row-local increment)
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (tenant_id, device_id)
);

CREATE INDEX idx_device_lease_owner
    ON dc3_device_lease (tenant_id, driver_id, owner_node, device_id);

CREATE TABLE dc3_driver_device_revision
(
    tenant_id BIGINT NOT NULL, -- Tenant ID
    driver_id BIGINT NOT NULL, -- Logical driver ID
    revision  BIGINT NOT NULL, -- Monotonic device-set revision
    PRIMARY KEY (tenant_id, driver_id)
);

CREATE TABLE dc3_driver_lease_state
(
    tenant_id          BIGINT      NOT NULL,                                                   -- Tenant ID
    driver_id          BIGINT      NOT NULL,                                                   -- Logical driver ID
    membership_hash    VARCHAR(64) NOT NULL,                                                   -- SHA-256 hash of active runtime membership
    device_revision    BIGINT      NOT NULL,                                                   -- Last assigned device-set revision
    assignment_version BIGINT      DEFAULT 1 NOT NULL, -- Monotonic assignment generation (row-local increment)
    operate_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,                         -- Operation time
    PRIMARY KEY (tenant_id, driver_id)
);






-- ----------------------------
-- Table structure for dc3_driver_attribute
-- ----------------------------
CREATE TABLE dc3_driver_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ('')          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ('')          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ('')          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT ('{}')        NOT NULL, -- Driver attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ('')          NOT NULL, -- Description
    signature           TEXT     DEFAULT ('')          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_driver_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_attribute_deleted CHECK (deleted IN (0, 1))
);






-- ----------------------------
-- Table structure for dc3_point_attribute
-- ----------------------------
CREATE TABLE dc3_point_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ('')          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ('')          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ('')          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT ('{}')        NOT NULL, -- Point attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ('')          NOT NULL, -- Description
    signature           TEXT     DEFAULT ('')          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_point_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_point_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_point_attribute_deleted CHECK (deleted IN (0, 1))
);






-- ----------------------------
-- Table structure for dc3_command_attribute
-- ----------------------------
CREATE TABLE dc3_command_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ('')          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ('')          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ('')          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT ('{}')        NOT NULL, -- Command attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ('')          NOT NULL, -- Description
    signature           TEXT     DEFAULT ('')          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_command_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_attribute_deleted CHECK (deleted IN (0, 1))
);






-- ----------------------------
-- Table structure for dc3_event_attribute
-- ----------------------------
CREATE TABLE dc3_event_attribute
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    attribute_name      TEXT     DEFAULT ('')          NOT NULL, -- Attribute name
    attribute_code      TEXT     DEFAULT ('')          NOT NULL, -- Attribute code
    attribute_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Attribute type flag
    default_value       TEXT     DEFAULT ('')          NOT NULL, -- Default value
    driver_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Driver ID
    attribute_ext       JSON     DEFAULT ('{}')        NOT NULL, -- Event attribute extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark              TEXT     DEFAULT ('')          NOT NULL, -- Description
    signature           TEXT     DEFAULT ('')          NOT NULL, -- Signature
    version             INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_attribute_attribute_type_flag CHECK (attribute_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_event_attribute_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_attribute_deleted CHECK (deleted IN (0, 1))
);






-- ----------------------------
-- Table structure for dc3_profile
-- ----------------------------
CREATE TABLE dc3_profile
(
    id                 BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    profile_name       TEXT     DEFAULT ('')          NOT NULL, -- Profile name
    profile_code       TEXT     DEFAULT ('')          NOT NULL, -- Profile code
    profile_share_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Profile sharing type flag
    profile_type_flag  SMALLINT DEFAULT 2 NOT NULL,                 -- Profile type flag
    profile_ext        JSON     DEFAULT ('{}')        NOT NULL, -- Profile extension information
    enable_flag        SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark             TEXT     DEFAULT ('')          NOT NULL, -- Description
    signature          TEXT     DEFAULT ('')          NOT NULL, -- Signature
    version            INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name       TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,     -- Creation time
    operator_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name      TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,    -- Operation time
    deleted            SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_profile_profile_share_flag CHECK (profile_share_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_profile_profile_type_flag CHECK (profile_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_profile_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_profile_deleted CHECK (deleted IN (0, 1))
);





-- ----------------------------
-- Table structure for dc3_point
-- ----------------------------
CREATE TABLE dc3_point
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    point_name      TEXT     DEFAULT ('')          NOT NULL, -- Point name
    point_code      TEXT     DEFAULT ('')          NOT NULL, -- Point code
    point_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Point type flag
    rw_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Read/write flag
    base_value      REAL     DEFAULT 0 NOT NULL,                 -- Base value
    multiple        REAL     DEFAULT 1 NOT NULL,                 -- Multiplier
    value_decimal   SMALLINT DEFAULT 6 NOT NULL,                 -- Value precision
    unit            TEXT     DEFAULT ('')          NOT NULL, -- Unit
    profile_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Profile ID
    point_ext       JSON     DEFAULT ('{}')        NOT NULL, -- Point extension information
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    remark          TEXT     DEFAULT ('')          NOT NULL, -- Description
    signature       TEXT     DEFAULT ('')          NOT NULL, -- Signature
    version         INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ('')          NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ('')          NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_point_point_type_flag CHECK (point_type_flag BETWEEN 0 AND 7),
    CONSTRAINT chk_point_rw_flag CHECK (rw_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_point_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_point_deleted CHECK (deleted IN (0, 1))
);



-- Supports listByProfileId queries with tenant scoping.




-- ----------------------------
-- Table structure for dc3_device
-- ----------------------------
CREATE TABLE dc3_device
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    device_name   TEXT     DEFAULT ('')          NOT NULL,   -- Device name
    device_code   TEXT     DEFAULT ('')          NOT NULL,   -- Device code
    driver_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Driver ID
    profile_id    BIGINT,                                        -- Profile ID (device belongs to single profile)
    device_ext    JSON     DEFAULT ('{}')        NOT NULL,   -- Device extension information
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ('')          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ('')          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ('')          NOT NULL,   -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ('')          NOT NULL,   -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_device_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_device_deleted CHECK (deleted IN (0, 1))
);




-- Supports listByDriverId queries with tenant scoping.









-- ----------------------------
-- Table structure for dc3_driver_attribute_config
-- ----------------------------
CREATE TABLE dc3_driver_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Driver attribute ID
    config_value  TEXT     DEFAULT ('')          NOT NULL,   -- Driver configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT ('{}')        NOT NULL,   -- Driver configuration information
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ('')          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ('')          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ('')          NOT NULL,   -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ('')          NOT NULL,   -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_attribute_config_deleted CHECK (deleted IN (0, 1))
);



-- Driver metadata assembly lists all driver attribute configs for a device.





-- ----------------------------
-- Table structure for dc3_point_attribute_config
-- ----------------------------
CREATE TABLE dc3_point_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Point attribute ID
    config_value  TEXT     DEFAULT ('')          NOT NULL,   -- Point configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT ('{}')        NOT NULL,   -- Point configuration information
    point_id      BIGINT   DEFAULT 0 NOT NULL,                   -- Point ID
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ('')          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ('')          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ('')          NOT NULL,   -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ('')          NOT NULL,   -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_point_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_point_attribute_config_deleted CHECK (deleted IN (0, 1))
);








-- ----------------------------
-- Table structure for dc3_command_attribute_config
-- ----------------------------
CREATE TABLE dc3_command_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Command attribute ID
    config_value  TEXT     DEFAULT ('')          NOT NULL,   -- Command configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT ('{}')        NOT NULL,   -- Command configuration information
    command_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Command ID
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ('')          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ('')          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ('')          NOT NULL,   -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ('')          NOT NULL,   -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_attribute_config_deleted CHECK (deleted IN (0, 1))
);







-- ----------------------------
-- Table structure for dc3_event_attribute_config
-- ----------------------------
CREATE TABLE dc3_event_attribute_config
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    attribute_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Event attribute ID
    config_value  TEXT     DEFAULT ('')          NOT NULL,   -- Event configuration value
    device_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Device ID
    config_ext    JSON     DEFAULT ('{}')        NOT NULL,   -- Event configuration information
    event_id      BIGINT   DEFAULT 0 NOT NULL,                   -- Event ID
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    tenant_id     BIGINT   DEFAULT 0 NOT NULL,                   -- Tenant ID
    remark        TEXT     DEFAULT ('')          NOT NULL,   -- Description
    signature     TEXT     DEFAULT ('')          NOT NULL,   -- Signature
    version       INTEGER  DEFAULT 0 NOT NULL,                   -- Version
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ('')          NOT NULL,   -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ('')          NOT NULL,   -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_attribute_config_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_attribute_config_deleted CHECK (deleted IN (0, 1))
);







-- ----------------------------
-- Table structure for dc3_command
-- ----------------------------
CREATE TABLE dc3_command
(
    id                BIGINT PRIMARY KEY  NOT NULL,              -- Primary key ID
    command_name      TEXT     DEFAULT ('')       NOT NULL,  -- Command name
    command_code      TEXT     DEFAULT ('')       NOT NULL,  -- Command code
    command_type_flag SMALLINT DEFAULT 0  NOT NULL,              -- Command type flag, 0: custom, 1: config, 2: action
    call_type_flag    SMALLINT DEFAULT 0  NOT NULL,              -- Call type flag, 0: sync, 1: async
    timeout           INTEGER  DEFAULT 30 NOT NULL,              -- Timeout in seconds
    command_ext       JSON     DEFAULT ('{}')     NOT NULL,  -- Command extension information
    enable_flag       SMALLINT DEFAULT 0  NOT NULL,              -- Enable flag, 0: enabled, 1: disabled
    tenant_id         BIGINT   DEFAULT 0  NOT NULL,              -- Tenant ID
    profile_id        BIGINT   DEFAULT 0  NOT NULL,              -- Profile ID
    remark            TEXT     DEFAULT ('')       NOT NULL,  -- Description
    signature         TEXT     DEFAULT ('')       NOT NULL,  -- Signature
    version           INTEGER  DEFAULT 0  NOT NULL,              -- Version
    creator_id        BIGINT   DEFAULT 0  NOT NULL,              -- Creator ID
    creator_name      TEXT     DEFAULT ('')       NOT NULL,  -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id       BIGINT   DEFAULT 0  NOT NULL,              -- Operator ID
    operator_name     TEXT     DEFAULT ('')       NOT NULL,  -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted           SMALLINT DEFAULT 0  NOT NULL,              -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_command_type_flag CHECK (command_type_flag BETWEEN 0 AND 2),
    CONSTRAINT chk_command_call_type_flag CHECK (call_type_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_command_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_deleted CHECK (deleted IN (0, 1))
);


-- Profile and device detail pages load command metadata by profile, then tenant and enable flag.




-- ----------------------------
-- Table structure for dc3_command_param
-- ----------------------------
CREATE TABLE dc3_command_param
(
    id                   BIGINT PRIMARY KEY NOT NULL,              -- Primary key ID
    param_name           TEXT     DEFAULT ('')       NOT NULL, -- Param name
    param_code           TEXT     DEFAULT ('')       NOT NULL, -- Param code
    param_direction_flag SMALLINT DEFAULT 0 NOT NULL,              -- Param direction flag, 0: input, 1: output
    param_type_flag      SMALLINT DEFAULT 0 NOT NULL,              -- Param type flag
    required_flag        SMALLINT DEFAULT 0 NOT NULL,              -- Required flag, 0: no, 1: yes
    default_value        TEXT     DEFAULT ('')       NOT NULL, -- Default value
    param_ext            JSON     DEFAULT ('{}')     NOT NULL, -- Param extension information
    enable_flag          SMALLINT DEFAULT 0 NOT NULL,              -- Enable flag, 0: enabled, 1: disabled
    tenant_id            BIGINT   DEFAULT 0 NOT NULL,              -- Tenant ID
    command_id           BIGINT   DEFAULT 0 NOT NULL,              -- Command ID
    remark               TEXT     DEFAULT ('')       NOT NULL, -- Description
    signature            TEXT     DEFAULT ('')       NOT NULL, -- Signature
    version              INTEGER  DEFAULT 0 NOT NULL,              -- Version
    creator_id           BIGINT   DEFAULT 0 NOT NULL,              -- Creator ID
    creator_name         TEXT     DEFAULT ('')       NOT NULL, -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,    -- Creation time
    operator_id          BIGINT   DEFAULT 0 NOT NULL,              -- Operator ID
    operator_name        TEXT     DEFAULT ('')       NOT NULL, -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL,   -- Operation time
    deleted              SMALLINT DEFAULT 0 NOT NULL,              -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_command_param_param_direction_flag CHECK (param_direction_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_command_param_required_flag CHECK (required_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_command_param_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_command_param_deleted CHECK (deleted IN (0, 1))
);


-- Command detail and cascade-delete paths list params by command_id.




-- ----------------------------
-- Table structure for dc3_event
-- ----------------------------
CREATE TABLE dc3_event
(
    id               BIGINT PRIMARY KEY NOT NULL,                -- Primary key ID
    event_name       TEXT     DEFAULT ('')       NOT NULL,   -- Event name
    event_code       TEXT     DEFAULT ('')       NOT NULL,   -- Event code
    event_type_flag  SMALLINT DEFAULT 0 NOT NULL,                -- Event type flag, 0: info, 1: alert, 2: fault, 3: lifecycle
    event_level_flag SMALLINT DEFAULT 0 NOT NULL,                -- Event level flag, 0: low, 1: medium, 2: high, 3: critical
    event_ext        JSON     DEFAULT ('{}')     NOT NULL,   -- Event extension information
    enable_flag      SMALLINT DEFAULT 0 NOT NULL,                -- Enable flag, 0: enabled, 1: disabled
    tenant_id        BIGINT   DEFAULT 0 NOT NULL,                -- Tenant ID
    profile_id       BIGINT   DEFAULT 0 NOT NULL,                -- Profile ID
    remark           TEXT     DEFAULT ('')       NOT NULL,   -- Description
    signature        TEXT     DEFAULT ('')       NOT NULL,   -- Signature
    version          INTEGER  DEFAULT 0 NOT NULL,                -- Version
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                -- Creator ID
    creator_name     TEXT     DEFAULT ('')       NOT NULL,   -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                -- Operator ID
    operator_name    TEXT     DEFAULT ('')       NOT NULL,   -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_event_type_flag CHECK (event_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_event_event_level_flag CHECK (event_level_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_event_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_deleted CHECK (deleted IN (0, 1))
);


-- Profile and device detail pages load event metadata by profile, then tenant and enable flag.




-- ----------------------------
-- Table structure for dc3_event_param
-- ----------------------------
CREATE TABLE dc3_event_param
(
    id              BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    param_name      TEXT     DEFAULT ('')       NOT NULL,    -- Param name
    param_code      TEXT     DEFAULT ('')       NOT NULL,    -- Param code
    param_type_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Param type flag
    param_ext       JSON     DEFAULT ('{}')     NOT NULL,    -- Param extension information
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    event_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Event ID
    remark          TEXT     DEFAULT ('')       NOT NULL,    -- Description
    signature       TEXT     DEFAULT ('')       NOT NULL,    -- Signature
    version         INTEGER  DEFAULT 0 NOT NULL,                 -- Version
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name    TEXT     DEFAULT ('')       NOT NULL,    -- Creator name
    create_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,  -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name   TEXT     DEFAULT ('')       NOT NULL,    -- Operator name
    operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_event_param_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_event_param_deleted CHECK (deleted IN (0, 1))
);


-- Event detail and cascade-delete paths list params by event_id.

--
-- Row-level revision tracking, same semantics as the PostgreSQL row trigger:
-- a device insert/delete/meaningful-update bumps the owning (or formerly
-- owning) driver's device-set revision; row-local +1, no sequence objects.
--
DELIMITER $$
CREATE TRIGGER track_driver_device_revision_insert AFTER INSERT ON dc3_device FOR EACH ROW
BEGIN
    IF NEW.deleted = 0 AND NEW.enable_flag = 0 THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (NEW.tenant_id, NEW.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
END$$

CREATE TRIGGER track_driver_device_revision_delete AFTER DELETE ON dc3_device FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND OLD.enable_flag = 0 THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (OLD.tenant_id, OLD.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
END$$

CREATE TRIGGER track_driver_device_revision_update AFTER UPDATE ON dc3_device FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND OLD.enable_flag = 0
        AND (NEW.tenant_id != OLD.tenant_id OR NEW.driver_id != OLD.driver_id
            OR NEW.deleted != OLD.deleted OR NEW.enable_flag != OLD.enable_flag) THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (OLD.tenant_id, OLD.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
    IF NEW.deleted = 0 AND NEW.enable_flag = 0
        AND (NEW.tenant_id != OLD.tenant_id OR NEW.driver_id != OLD.driver_id
            OR NEW.deleted != OLD.deleted OR NEW.enable_flag != OLD.enable_flag) THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (NEW.tenant_id, NEW.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
END$$
DELIMITER ;
