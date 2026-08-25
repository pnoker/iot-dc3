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

CREATE DATABASE IF NOT EXISTS dc3_auth
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dc3_auth;

-- ----------------------------
-- Function for update operate time
-- ----------------------------

-- ----------------------------
-- Table structure for dc3_tenant
-- ----------------------------
CREATE TABLE dc3_tenant
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    tenant_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Tenant name
    tenant_code   TEXT        DEFAULT ('')                                                NOT NULL, -- Tenant code
    tenant_ext    JSON        DEFAULT ('{}')                                              NOT NULL, -- Tenant extension information
    enable_flag   SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_tenant_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_tenant_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_principal
-- ----------------------------
CREATE TABLE dc3_principal
(
    id              BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    principal_type  TEXT        DEFAULT ('USER')                                            NOT NULL, -- Principal type, USER: user, SERVICE_ACCOUNT: service account, SYSTEM: system principal
    principal_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Principal name
    display_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Principal display name
    source_type     TEXT        DEFAULT ('LOCAL')                                           NOT NULL, -- Principal source type, LOCAL: local, EXTERNAL: external, SYSTEM: system
    enable_flag     SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    locked_flag     SMALLINT    DEFAULT 0                                                   NOT NULL, -- Locked flag, 0: unlocked, 1: locked
    last_login_time DATETIME(6),                                                                      -- Last login time
    principal_ext   JSON        DEFAULT ('{}')                                              NOT NULL, -- Principal extension information
    remark          TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_principal_source_type CHECK (source_type IN ('LOCAL', 'EXTERNAL', 'SYSTEM')),
    CONSTRAINT chk_principal_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_principal_locked_flag CHECK (locked_flag IN (0, 1)),
    CONSTRAINT chk_principal_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_user
-- ----------------------------
CREATE TABLE dc3_user
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    principal_id  BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    user_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Username
    nick_name     TEXT        DEFAULT ('')                                                NOT NULL, -- User nickname
    phone         TEXT        DEFAULT ('')                                                NOT NULL, -- Phone number
    email         TEXT        DEFAULT ('')                                                NOT NULL, -- Email
    social_ext    JSON        DEFAULT ('{}')                                              NOT NULL, -- Social extension information
    identity_ext  JSON        DEFAULT ('{}')                                              NOT NULL, -- Identity extension information
    enable_flag   SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_user_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_user_deleted CHECK (deleted IN (0, 1))
);

-- Unique within non-deleted users; empty values are excluded so optional phone/email may be blank for many users.


-- ----------------------------
-- Table structure for dc3_local_credential
-- ----------------------------
CREATE TABLE dc3_local_credential
(
    id                      BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    principal_id            BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    login_name              TEXT        DEFAULT ('')                                                NOT NULL, -- Login name
    login_name_normalized   TEXT        DEFAULT ('')                                                NOT NULL, -- Normalized login name
    credential_type         TEXT        DEFAULT ('PASSWORD')                                        NOT NULL, -- Credential type
    password_hash           TEXT        DEFAULT ('')                                                NOT NULL, -- Password hash
    password_algorithm      TEXT        DEFAULT ('ARGON2ID')                                        NOT NULL, -- Password hash algorithm
    password_params         JSON        DEFAULT ('{}')                                              NOT NULL, -- Password hash parameters
    password_updated_time   DATETIME(6),                                                                      -- Password update time
    password_expire_time    DATETIME(6),                                                                      -- Password expiration time
    failed_attempts         INTEGER     DEFAULT 0                                                   NOT NULL, -- Failed login attempts
    locked_until            DATETIME(6),                                                                      -- Credential locked until time
    require_password_change SMALLINT    DEFAULT 1                                                   NOT NULL, -- Require password change flag, 0: no, 1: yes
    enable_flag             SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    credential_ext          JSON        DEFAULT ('{}')                                              NOT NULL, -- Credential extension information
    remark                  TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id              BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name            TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id             BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name           TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time            DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted                 SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_local_credential_type CHECK (credential_type IN ('PASSWORD')),
    CONSTRAINT chk_local_credential_password_algorithm CHECK (password_algorithm IN ('ARGON2ID', 'BCRYPT')),
    CONSTRAINT chk_local_credential_require_change CHECK (require_password_change IN (0, 1)),
    CONSTRAINT chk_local_credential_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_local_credential_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_tenant_membership
-- ----------------------------
CREATE TABLE dc3_tenant_membership
(
    id                BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    tenant_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    principal_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    principal_type    TEXT        DEFAULT ('USER')                                            NOT NULL, -- Principal type
    membership_status TEXT        DEFAULT ('ACTIVE')                                          NOT NULL, -- Membership status
    joined_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Joined time
    membership_ext    JSON        DEFAULT ('{}')                                              NOT NULL, -- Membership extension information
    remark            TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted           SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_tenant_membership_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_tenant_membership_status CHECK (membership_status IN ('ACTIVE', 'SUSPENDED', 'INVITED')),
    CONSTRAINT chk_tenant_membership_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_service_account
-- ----------------------------
CREATE TABLE dc3_service_account
(
    id                    BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    principal_id          BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    tenant_id             BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    service_account_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Service account name
    owner_principal_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Owner principal ID
    purpose               TEXT        DEFAULT ('')                                                NOT NULL, -- Service account purpose
    expire_time           DATETIME(6),                                                                      -- Expiration time
    last_used_time        DATETIME(6),                                                                      -- Last used time
    credential_policy_ext JSON        DEFAULT ('{}')                                              NOT NULL, -- Credential policy extension information
    enable_flag           SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark                TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id            BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name          TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id           BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name         TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted               SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_service_account_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_service_account_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_identity_provider
-- ----------------------------
CREATE TABLE dc3_identity_provider
(
    id                BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    tenant_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    provider_code     TEXT        DEFAULT ('')                                                NOT NULL, -- Identity provider code
    provider_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Identity provider name
    provider_type     TEXT        DEFAULT ('OIDC')                                            NOT NULL, -- Identity provider type
    issuer            TEXT        DEFAULT ('')                                                NOT NULL, -- Issuer
    discovery_url     TEXT        DEFAULT ('')                                                NOT NULL, -- OIDC discovery URL
    authorization_uri TEXT        DEFAULT ('')                                                NOT NULL, -- Authorization URI
    token_uri         TEXT        DEFAULT ('')                                                NOT NULL, -- Token URI
    user_info_uri     TEXT        DEFAULT ('')                                                NOT NULL, -- User info URI
    jwks_uri          TEXT        DEFAULT ('')                                                NOT NULL, -- JWKS URI
    client_id         TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth client ID
    client_secret_ref TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth client secret reference
    scopes            TEXT        DEFAULT ('')                                                NOT NULL, -- Requested scopes
    redirect_uri      TEXT        DEFAULT ('')                                                NOT NULL, -- Redirect URI
    subject_claim     TEXT        DEFAULT ('sub')                                             NOT NULL, -- Subject claim name
    username_claim    TEXT        DEFAULT ('')                                                NOT NULL, -- Username claim name
    email_claim       TEXT        DEFAULT ('email')                                           NOT NULL, -- Email claim name
    attribute_mapping JSON        DEFAULT ('{}')                                              NOT NULL, -- Attribute mapping
    provisioning_mode TEXT        DEFAULT ('LINK_ONLY')                                       NOT NULL, -- Provisioning mode
    enable_flag       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    provider_ext      JSON        DEFAULT ('{}')                                              NOT NULL, -- Provider extension information
    remark            TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted           SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_identity_provider_type CHECK (provider_type IN ('GOOGLE', 'GITHUB', 'OIDC', 'SAML')),
    CONSTRAINT chk_identity_provider_provisioning CHECK (provisioning_mode IN ('LINK_ONLY', 'JIT')),
    CONSTRAINT chk_identity_provider_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_identity_provider_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_external_identity
-- ----------------------------
CREATE TABLE dc3_external_identity
(
    id                 BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    provider_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Identity provider ID
    principal_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    external_subject   TEXT        DEFAULT ('')                                                NOT NULL, -- External identity subject
    external_username  TEXT        DEFAULT ('')                                                NOT NULL, -- External username
    external_email     TEXT        DEFAULT ('')                                                NOT NULL, -- External email
    email_verified     SMALLINT    DEFAULT 0                                                   NOT NULL, -- Email verified flag, 0: no, 1: yes
    first_login_time   DATETIME(6),                                                                      -- First login time
    last_login_time    DATETIME(6),                                                                      -- Last login time
    last_claims_digest TEXT        DEFAULT ('')                                                NOT NULL, -- Last claims digest
    identity_ext       JSON        DEFAULT ('{}')                                              NOT NULL, -- External identity extension information
    enable_flag        SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark             TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name       TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time       DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted            SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_external_identity_email_verified CHECK (email_verified IN (0, 1)),
    CONSTRAINT chk_external_identity_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_external_identity_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_role
-- ----------------------------
CREATE TABLE dc3_role
(
    id             BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    parent_role_id BIGINT      DEFAULT 0                                                   NOT NULL, -- Parent role ID
    role_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Role name
    role_code      TEXT        DEFAULT ('')                                                NOT NULL, -- Role code
    role_ext       JSON        DEFAULT ('{}')                                              NOT NULL, -- Role extension information
    enable_flag    SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark         TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted        SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_role_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_role_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_resource
-- ----------------------------
CREATE TABLE dc3_resource
(
    id                  BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    parent_resource_id  BIGINT      DEFAULT 0                                                   NOT NULL, -- Parent resource ID
    resource_name       TEXT        DEFAULT ('')                                                NOT NULL, -- Resource name
    resource_code       TEXT        DEFAULT ('')                                                NOT NULL, -- Resource code
    service_name        TEXT        DEFAULT ('')                                                NOT NULL, -- Service name
    resource_type_flag  SMALLINT    DEFAULT 0                                                   NOT NULL, -- Resource type flag
    resource_scope_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Resource scope flag
    entity_id           BIGINT      DEFAULT 0                                                   NOT NULL, -- Resource entity ID
    resource_ext        JSON        DEFAULT ('{}')                                              NOT NULL, -- Resource extension information
    enable_flag         SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark              TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id          BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name        TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time         DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name       TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted             SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_resource_resource_type_flag CHECK (resource_type_flag BETWEEN 0 AND 6),
    CONSTRAINT chk_resource_resource_scope_flag CHECK (resource_scope_flag BETWEEN 0 AND 4),
    CONSTRAINT chk_resource_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_resource_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_role_principal_bind
-- ----------------------------
CREATE TABLE dc3_role_principal_bind
(
    id             BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    tenant_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    role_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Role ID
    principal_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    principal_type TEXT        DEFAULT ('USER')                                            NOT NULL, -- Principal type
    remark         TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted        SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_role_principal_bind_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_role_principal_bind_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_role_resource_bind
-- ----------------------------
CREATE TABLE dc3_role_resource_bind
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    role_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Role ID
    resource_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Resource ID
    remark        TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_role_resource_bind_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_driver_token
-- ----------------------------
CREATE TABLE dc3_driver_token
(
    id             BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    driver_code    TEXT        DEFAULT ('')                                                NOT NULL, -- Driver code
    driver_app_id  TEXT        DEFAULT ('')                                                NOT NULL, -- Driver App ID
    driver_app_key TEXT        DEFAULT ('')                                                NOT NULL, -- Driver App Key
    expire_flag    SMALLINT    DEFAULT 0                                                   NOT NULL, -- Expiration flag
    expire_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Expiration time
    enable_flag    SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tenant_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    remark         TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted        SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_token_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_token_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_api
-- ----------------------------
CREATE TABLE dc3_api
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    service_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Owning service name, populated by resource registrar
    api_type_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- API type flag
    api_name      TEXT        DEFAULT ('')                                                NOT NULL, -- API name
    api_code      TEXT        DEFAULT ('')                                                NOT NULL, -- API code
    api_group     TEXT        DEFAULT ('')                                                NOT NULL, -- API grouping (controller simple name)
    api_ext       JSON        DEFAULT ('{}')                                              NOT NULL, -- API extension information
    enable_flag   SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_api_api_type_flag CHECK (api_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_api_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_api_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_oauth_registered_client
-- ----------------------------
CREATE TABLE dc3_oauth_registered_client
(
    id                           BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    client_id                    TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth client ID
    client_name                  TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth client name
    client_type                  TEXT        DEFAULT ('PUBLIC')                                          NOT NULL, -- OAuth client type, PUBLIC: public client, CONFIDENTIAL: confidential client
    owner_principal_id           BIGINT      DEFAULT 0                                                   NOT NULL, -- Owner principal ID
    service_account_principal_id BIGINT      DEFAULT 0                                                   NOT NULL, -- Service account principal ID
    tenant_id                    BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    client_secret_hash           TEXT        DEFAULT ('')                                                NOT NULL, -- Client secret hash
    client_secret_expires_at     DATETIME(6),                                                                      -- Client secret expiration time
    client_auth_methods          TEXT        DEFAULT ('')                                                NOT NULL, -- Client authentication methods
    authorization_grant_types    TEXT        DEFAULT ('')                                                NOT NULL, -- Authorization grant types
    redirect_uris                TEXT        DEFAULT ('')                                                NOT NULL, -- Redirect URIs
    scopes                       TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth scopes
    jwks_uri                     TEXT        DEFAULT ('')                                                NOT NULL, -- Client JWKS URI
    jwk_set                      JSON        DEFAULT ('{}')                                              NOT NULL, -- Client JWK set
    require_pkce                 SMALLINT    DEFAULT 1                                                   NOT NULL, -- Require PKCE flag, 0: no, 1: yes
    require_consent              SMALLINT    DEFAULT 1                                                   NOT NULL, -- Require consent flag, 0: no, 1: yes
    enable_flag                  SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    client_settings              JSON        DEFAULT ('{}')                                              NOT NULL, -- Client settings
    token_settings               JSON        DEFAULT ('{}')                                              NOT NULL, -- Token settings
    remark                       TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id                   BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name                 TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time                  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id                  BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name                TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time                 DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted                      SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_oauth_client_type CHECK (client_type IN ('PUBLIC', 'CONFIDENTIAL')),
    CONSTRAINT chk_oauth_client_require_pkce CHECK (require_pkce IN (0, 1)),
    CONSTRAINT chk_oauth_client_require_consent CHECK (require_consent IN (0, 1)),
    CONSTRAINT chk_oauth_client_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_oauth_client_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_oauth_authorization
-- ----------------------------
CREATE TABLE dc3_oauth_authorization
(
    id                          BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    registered_client_id        BIGINT      DEFAULT 0                                                   NOT NULL, -- Registered client ID
    client_id                   TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth client ID
    principal_id                BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    principal_type              TEXT        DEFAULT ('USER')                                            NOT NULL, -- Principal type
    tenant_id                   BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    mcp_connection_id           BIGINT      DEFAULT 0                                                   NOT NULL, -- MCP connection ID
    authorization_grant_type    TEXT        DEFAULT ('')                                                NOT NULL, -- Authorization grant type
    authorized_scopes           TEXT        DEFAULT ('')                                                NOT NULL, -- Authorized scopes
    state_hash                  TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth state hash
    authorization_code_hash     TEXT        DEFAULT ('')                                                NOT NULL, -- Authorization code hash
    authorization_code_issued   DATETIME(6),                                                                      -- Authorization code issued time
    authorization_code_expires  DATETIME(6),                                                                      -- Authorization code expiration time
    access_token_jti            TEXT        DEFAULT ('')                                                NOT NULL, -- Access token JWT ID
    access_token_issued         DATETIME(6),                                                                      -- Access token issued time
    access_token_expires        DATETIME(6),                                                                      -- Access token expiration time
    refresh_token_hash          TEXT        DEFAULT ('')                                                NOT NULL, -- Refresh token hash
    previous_refresh_token_hash TEXT        DEFAULT ('')                                                NOT NULL, -- Previous (rotated) refresh token hash, used to detect replay
    refresh_token_issued        DATETIME(6),                                                                      -- Refresh token issued time
    refresh_token_expires       DATETIME(6),                                                                      -- Refresh token expiration time
    token_claims                JSON        DEFAULT ('{}')                                              NOT NULL, -- Token claims
    token_metadata              JSON        DEFAULT ('{}')                                              NOT NULL, -- Token metadata
    revoked_time                DATETIME(6),                                                                      -- Revoked time
    revoke_reason               TEXT        DEFAULT ('')                                                NOT NULL, -- Revoke reason
    create_time                 DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operate_time                DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted                     SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_oauth_authorization_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_oauth_authorization_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_oauth_authorization_consent
-- ----------------------------
CREATE TABLE dc3_oauth_authorization_consent
(
    id                   BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    registered_client_id BIGINT      DEFAULT 0                                                   NOT NULL, -- Registered client ID
    client_id            TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth client ID
    principal_id         BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    tenant_id            BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    scopes               TEXT        DEFAULT ('')                                                NOT NULL, -- Consented scopes
    consent_ext          JSON        DEFAULT ('{}')                                              NOT NULL, -- Consent extension information
    create_time          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operate_time         DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted              SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_oauth_consent_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_mcp_connection
-- ----------------------------
CREATE TABLE dc3_mcp_connection
(
    id              BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    connection_name TEXT        DEFAULT ('')                                                NOT NULL, -- MCP connection name
    client_id       TEXT        DEFAULT ('')                                                NOT NULL, -- OAuth client ID
    principal_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Principal ID
    principal_type  TEXT        DEFAULT ('USER')                                            NOT NULL, -- Principal type
    tenant_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Tenant ID
    grant_type      TEXT        DEFAULT ('authorization_code')                              NOT NULL, -- OAuth grant type
    enable_flag     SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    expire_time     DATETIME(6),                                                                      -- Expiration time
    revoke_time     DATETIME(6),                                                                      -- Revoke time
    last_used_time  DATETIME(6),                                                                      -- Last used time
    connection_ext  JSON        DEFAULT ('{}')                                              NOT NULL, -- Connection extension information
    remark          TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted         SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_connection_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_connection_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_mcp_connection_grant_type CHECK (grant_type IN ('authorization_code', 'client_credentials')),
    CONSTRAINT chk_mcp_connection_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_mcp_tool_catalog
-- ----------------------------
CREATE TABLE dc3_mcp_tool_catalog
(
    id               BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    tool_id          TEXT        DEFAULT ('')                                                NOT NULL, -- MCP tool ID
    tool_name        TEXT        DEFAULT ('')                                                NOT NULL, -- MCP tool name
    tool_title       TEXT        DEFAULT ('')                                                NOT NULL, -- MCP tool title
    tool_category    TEXT        DEFAULT ('')                                                NOT NULL, -- MCP tool category
    service_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Service name
    api_code         TEXT        DEFAULT ('')                                                NOT NULL, -- API resource code
    permission_code  TEXT        DEFAULT ('')                                                NOT NULL, -- Permission resource code
    http_method      TEXT        DEFAULT ('')                                                NOT NULL, -- HTTP method
    api_path         TEXT        DEFAULT ('')                                                NOT NULL, -- API path
    schema_hash      TEXT        DEFAULT ('')                                                NOT NULL, -- Tool schema hash
    risk_level       TEXT        DEFAULT ('LOW')                                             NOT NULL, -- Risk level, LOW: low, MEDIUM: medium, HIGH: high
    read_only_hint   SMALLINT    DEFAULT 0                                                   NOT NULL, -- Read-only hint, 0: false, 1: true
    destructive_hint SMALLINT    DEFAULT 0                                                   NOT NULL, -- Destructive hint, 0: false, 1: true
    idempotent_hint  SMALLINT    DEFAULT 0                                                   NOT NULL, -- Idempotent hint, 0: false, 1: true
    open_world_hint  SMALLINT    DEFAULT 0                                                   NOT NULL, -- Open-world hint, 0: false, 1: true
    enable_flag      SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    tool_ext         JSON        DEFAULT ('{}')                                              NOT NULL, -- Tool extension information
    remark           TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id       BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name     TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id      BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name    TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted          SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_tool_catalog_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_mcp_tool_catalog_read_only_hint CHECK (read_only_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_destructive_hint CHECK (destructive_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_idempotent_hint CHECK (idempotent_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_open_world_hint CHECK (open_world_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_mcp_connection_tool
-- ----------------------------
CREATE TABLE dc3_mcp_connection_tool
(
    id            BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    connection_id BIGINT      DEFAULT 0                                                   NOT NULL, -- MCP connection ID
    tool_id       TEXT        DEFAULT ('')                                                NOT NULL, -- MCP tool ID
    enable_flag   SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id   BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted       SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_connection_tool_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_connection_tool_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_mcp_audit_log
-- ----------------------------
CREATE TABLE dc3_mcp_audit_log
(
    id              BIGINT PRIMARY KEY                       NOT NULL, -- Primary key ID
    trace_id        TEXT        DEFAULT ('')                 NOT NULL, -- Trace ID
    tenant_id       BIGINT      DEFAULT 0                    NOT NULL, -- Tenant ID
    principal_id    BIGINT      DEFAULT 0                    NOT NULL, -- Principal ID
    principal_type  TEXT        DEFAULT ('USER')             NOT NULL, -- Principal type
    client_id       TEXT        DEFAULT ('')                 NOT NULL, -- OAuth client ID
    connection_id   BIGINT      DEFAULT 0                    NOT NULL, -- MCP connection ID
    tool_id         TEXT        DEFAULT ('')                 NOT NULL, -- MCP tool ID
    tool_name       TEXT        DEFAULT ('')                 NOT NULL, -- MCP tool name
    permission_code TEXT        DEFAULT ('')                 NOT NULL, -- Permission resource code
    risk_level      TEXT        DEFAULT ('LOW')              NOT NULL, -- Risk level
    confirm_id      TEXT        DEFAULT ('')                 NOT NULL, -- Confirmation ID
    idempotency_key TEXT        DEFAULT ('')                 NOT NULL, -- Idempotency key
    argument_digest TEXT        DEFAULT ('')                 NOT NULL, -- Argument digest
    status          TEXT        DEFAULT ('')                 NOT NULL, -- Tool call status
    error_code      TEXT        DEFAULT ('')                 NOT NULL, -- Error code
    duration_ms     BIGINT      DEFAULT 0                    NOT NULL, -- Duration in milliseconds
    client_name     TEXT        DEFAULT ('')                 NOT NULL, -- MCP client name
    client_version  TEXT        DEFAULT ('')                 NOT NULL, -- MCP client version
    remote_ip       TEXT        DEFAULT ('')                 NOT NULL, -- Remote IP address
    audit_ext       JSON        DEFAULT ('{}')               NOT NULL, -- Audit extension information
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL, -- Creation time
    deleted         SMALLINT    DEFAULT 0                    NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_audit_log_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_mcp_audit_log_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_mcp_audit_log_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_mcp_tool_confirmation
-- ----------------------------
CREATE TABLE dc3_mcp_tool_confirmation
(
    id              BIGINT PRIMARY KEY                       NOT NULL, -- Primary key ID
    confirm_id      TEXT        DEFAULT ('')                 NOT NULL, -- Confirmation ticket ID
    tenant_id       BIGINT      DEFAULT 0                    NOT NULL, -- Tenant ID
    principal_id    BIGINT      DEFAULT 0                    NOT NULL, -- Principal ID
    connection_id   BIGINT      DEFAULT 0                    NOT NULL, -- MCP connection ID
    tool_id         TEXT        DEFAULT ('')                 NOT NULL, -- MCP tool ID
    argument_digest TEXT        DEFAULT ('')                 NOT NULL, -- Argument digest bound to the ticket
    idempotency_key TEXT        DEFAULT ('')                 NOT NULL, -- Idempotency key
    risk_level      TEXT        DEFAULT ('HIGH')             NOT NULL, -- Risk level
    status          TEXT        DEFAULT ('PENDING')          NOT NULL, -- Ticket status, PENDING/CONSUMED
    expire_time     DATETIME(6),                                       -- Expiration time
    consumed_time   DATETIME(6),                                       -- Consumed time
    create_time     DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL, -- Creation time
    deleted         SMALLINT    DEFAULT 0                    NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_tool_confirmation_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_mcp_tool_confirmation_status CHECK (status IN ('PENDING', 'CONSUMED')),
    CONSTRAINT chk_mcp_tool_confirmation_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Table structure for dc3_menu
-- ----------------------------
CREATE TABLE dc3_menu
(
    id             BIGINT PRIMARY KEY                                                      NOT NULL, -- Primary key ID
    parent_menu_id BIGINT      DEFAULT 0                                                   NOT NULL, -- Parent menu ID
    menu_type_flag SMALLINT    DEFAULT 0                                                   NOT NULL, -- Menu type flag
    menu_name      TEXT        DEFAULT ('')                                                NOT NULL, -- Menu name
    menu_code      TEXT        DEFAULT ('')                                                NOT NULL, -- Menu code
    menu_level     SMALLINT    DEFAULT 0                                                   NOT NULL, -- Menu level
    menu_index     SMALLINT    DEFAULT 0                                                   NOT NULL, -- Menu order
    menu_ext       JSON        DEFAULT ('{}')                                              NOT NULL, -- Menu extension information
    enable_flag    SMALLINT    DEFAULT 0                                                   NOT NULL, -- Enable flag, 0: enabled, 1: disabled
    remark         TEXT        DEFAULT ('')                                                NOT NULL, -- Description
    creator_id     BIGINT      DEFAULT 0                                                   NOT NULL, -- Creator ID
    creator_name   TEXT        DEFAULT ('')                                                NOT NULL, -- Creator name
    create_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                                NOT NULL, -- Creation time
    operator_id    BIGINT      DEFAULT 0                                                   NOT NULL, -- Operator ID
    operator_name  TEXT        DEFAULT ('')                                                NOT NULL, -- Operator name
    operate_time   DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL, -- Operation time
    deleted        SMALLINT    DEFAULT 0                                                   NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_menu_menu_type_flag CHECK (menu_type_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_menu_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_menu_deleted CHECK (deleted IN (0, 1))
);



-- ----------------------------
-- Records of dc3_tenant
-- ----------------------------
INSERT INTO dc3_tenant
VALUES (1, 'Default Tenant', 'default', '{}', 0, 'Default tenant', 1, 'dc3', '2016-10-01 00:00:00.000000', 1,
        'dc3', '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_principal
-- ----------------------------
INSERT INTO dc3_principal
VALUES (1, 'USER', 'dc3', 'DC3', 'LOCAL', 0, 0, NULL, '{}', 'Default administrator principal', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
INSERT INTO dc3_user
VALUES (1, 1, 'dc3', 'DC3', '18300000000', 'dc3@dc3.com', '{}', '{}', 0, 'Default user', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_local_credential
-- login_name: dc3
-- raw password: dc3dc3dc3
-- stored as bcrypt(raw password), cost factor 12
-- ----------------------------
INSERT INTO dc3_local_credential
VALUES (1, 1, 'dc3', 'dc3', 'PASSWORD', '$2b$12$cSuC2gIZqrti2JLHur5JU.cy9D2kW6KJ5AXTd0nRPJ.cU7gUczhtK', 'BCRYPT', '{}',
        '2016-10-01 00:00:00.000000', NULL, 0, NULL, 0, 0, '{}',
        'Default local credential', 1, 'dc3', '2016-10-01 00:00:00.000000', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_tenant_membership
-- ----------------------------
INSERT INTO dc3_tenant_membership
VALUES (1, 1, 1, 'USER', 'ACTIVE', '2016-10-01 00:00:00.000000', '{}', 'Default tenant administrator membership',
        1, 'dc3', '2016-10-01 00:00:00.000000',
        1, 'dc3', '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_identity_provider
-- ----------------------------
INSERT INTO dc3_identity_provider
VALUES (1, 0, 'local', 'Local Password', 'OIDC', '', '', '', '', '', '', '', '', '', '', 'sub', '', 'email', '{}',
        'LINK_ONLY', 1, '{}',
        'Placeholder provider for local identity boundary; external login is disabled by default.', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_role
-- Default administrator role for the default tenant.
-- ----------------------------
INSERT INTO dc3_role (id, parent_role_id, role_name, role_code, role_ext, enable_flag, tenant_id, remark, creator_id,
                      creator_name, create_time, operator_id, operator_name, operate_time, deleted)
VALUES (1, 0, 'Administrator', 'admin', '{}', 0, 1, 'Default administrator role', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_role_principal_bind
-- ----------------------------
INSERT INTO dc3_role_principal_bind (id, tenant_id, role_id, principal_id, principal_type, remark, creator_id,
                                     creator_name, create_time, operator_id, operator_name, operate_time, deleted)
VALUES (1, 1, 1, 1, 'USER', 'Default administrator role principal binding', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 1, 'dc3',
        '2016-10-01 00:00:00.000000', 0);

-- ----------------------------
-- Records of dc3_menu (default sidebar tree driving the frontend)
-- menu_type_flag: 0=TITLE, 1=COMMON
-- menu_level:     1=C1 (top-level), 2=C2 (nested under a title)
-- menu_ext:       JsonExt wrapper with content holding {titles: {zh, en, ...}, icon, url, remark}.
--                 titles is the authoritative, locale-keyed display-name map. The UI picks
--                 titles[currentLocale] with fallback to titles.en. Legacy `title` i18n keys
--                 (e.g. "nav.home") have been removed from seed data; see migrations/ for
--                 the backfill script that upgrades existing databases.
-- ----------------------------
INSERT INTO dc3_menu (id, parent_menu_id, menu_type_flag, menu_name, menu_code, menu_level, menu_index, menu_ext,
                      enable_flag, remark, creator_id, creator_name, create_time, operator_id, operator_name,
                      operate_time, deleted)
VALUES (10001, 0, 1, 'Home', 'home', 1, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"首页\",\"en\":\"Home\"},\"icon\":\"HomeFilled\",\"url\":\"/home\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10002, 0, 1, 'Driver', 'driver', 1, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"驱动管理\",\"en\":\"Driver\"},\"icon\":\"Promotion\",\"url\":\"/driver\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10003, 0, 1, 'Profile', 'profile', 1, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"模板管理\",\"en\":\"Profile\"},\"icon\":\"List\",\"url\":\"/profile\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10004, 0, 1, 'Device', 'device', 1, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"设备管理\",\"en\":\"Device\"},\"icon\":\"Management\",\"url\":\"/device\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10005, 0, 1, 'PointValue', 'pointValue', 1, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"位号数据\",\"en\":\"PointValue\"},\"icon\":\"TrendCharts\",\"url\":\"/point_value\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10006, 0, 0, 'Settings', 'settings', 1, 6,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"设置\",\"en\":\"Settings\"},\"icon\":\"Setting\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10050, 10006, 0, 'Accounts & Identity', 'settingsIdentity', 2, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"账户与身份\",\"en\":\"Accounts & Identity\"},\"icon\":\"User\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10007, 10050, 1, 'User', 'settingsUser', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"用户管理\",\"en\":\"User\"},\"icon\":\"User\",\"url\":\"/settings/user\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10040, 10050, 1, 'Identity Principals', 'settingsPrincipal', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"身份主体\",\"en\":\"Identity Principals\"},\"icon\":\"Avatar\",\"url\":\"/settings/principal\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10041, 10050, 1, 'Tenant Membership', 'settingsTenantMembership', 3, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"租户成员\",\"en\":\"Tenant Membership\"},\"icon\":\"OfficeBuilding\",\"url\":\"/settings/tenant_membership\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10042, 10050, 1, 'Local Credentials', 'settingsLocalCredential', 3, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"本地凭证\",\"en\":\"Local Credentials\"},\"icon\":\"Lock\",\"url\":\"/settings/local_credential\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10038, 10050, 1, 'Service Accounts', 'settingsServiceAccount', 3, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"服务账号\",\"en\":\"Service Accounts\"},\"icon\":\"Key\",\"url\":\"/settings/service_account\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10051, 10006, 0, 'Roles & Permissions', 'settingsAccess', 2, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"角色与权限\",\"en\":\"Roles & Permissions\"},\"icon\":\"Stamp\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10008, 10051, 1, 'Role', 'settingsRole', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"角色管理\",\"en\":\"Role\"},\"icon\":\"Lock\",\"url\":\"/settings/role\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10039, 10051, 1, 'Role Assignments', 'settingsRolePrincipalBind', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"角色分配\",\"en\":\"Role Assignments\"},\"icon\":\"Link\",\"url\":\"/settings/role_principal_bind\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10009, 10051, 1, 'Resource', 'settingsResource', 3, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"资源管理\",\"en\":\"Resource\"},\"icon\":\"Tickets\",\"url\":\"/settings/resource\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10010, 10051, 1, 'API', 'settingsApi', 3, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"API 接口\",\"en\":\"API\"},\"icon\":\"Connection\",\"url\":\"/settings/api\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10011, 10051, 1, 'Menu', 'settingsMenu', 3, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"菜单管理\",\"en\":\"Menu\"},\"icon\":\"Discount\",\"url\":\"/settings/menu\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10013, 10006, 0, 'AI Models', 'settingsModel', 2, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"AI 模型\",\"en\":\"AI Models\"},\"icon\":\"Cpu\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10020, 10013, 1, 'Model Config', 'settingsModelConfig', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"模型配置\",\"en\":\"Model Config\"},\"icon\":\"ChatDotRound\",\"url\":\"/settings/model/config\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10021, 10013, 1, 'Model Providers', 'settingsModelProvider', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"模型供应商\",\"en\":\"Model Providers\"},\"icon\":\"ChatLineSquare\",\"url\":\"/settings/model/provider\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10022, 10006, 0, 'Alarm Configuration', 'settingsAlarm', 2, 6,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警配置\",\"en\":\"Alarm Configuration\"},\"icon\":\"AlarmClock\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10023, 10022, 1, 'Alarm Rules', 'settingsAlarmRule', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警规则\",\"en\":\"Alarm Rules\"},\"icon\":\"SetUp\",\"url\":\"/settings/alarm/rule\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10024, 10022, 1, 'Notification Policies', 'settingsAlarmNotify', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警通知策略\",\"en\":\"Notification Policies\"},\"icon\":\"Bell\",\"url\":\"/settings/alarm/notify\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10025, 10022, 1, 'Message Templates', 'settingsAlarmMessage', 3, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警消息模板\",\"en\":\"Message Templates\"},\"icon\":\"Message\",\"url\":\"/settings/alarm/message\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10026, 10022, 1, 'Alarm Channels', 'settingsAlarmChannel', 3, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警通知渠道\",\"en\":\"Alarm Channels\"},\"icon\":\"Connection\",\"url\":\"/settings/alarm/channel\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10027, 10022, 1, 'Alarm Bindings', 'settingsAlarmBind', 3, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警渠道绑定\",\"en\":\"Alarm Bindings\"},\"icon\":\"Link\",\"url\":\"/settings/alarm/bind\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10015, 10052, 1, 'Overview', 'settingsAlarmOverview', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"概览\",\"en\":\"Overview\"},\"icon\":\"DataAnalysis\",\"url\":\"/settings/alarm/overview\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10028, 10052, 1, 'Alarm States', 'settingsAlarmState', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警运行状态\",\"en\":\"Alarm States\"},\"icon\":\"Monitor\",\"url\":\"/settings/alarm/state\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10029, 10052, 1, 'Alarm History', 'settingsAlarmHistory', 3, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警历史\",\"en\":\"Alarm History\"},\"icon\":\"DocumentChecked\",\"url\":\"/settings/alarm/history\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10017, 10052, 1, 'Driver Alarm', 'settingsDriverAlarm', 3, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"驱动告警\",\"en\":\"Driver Alarm\"},\"icon\":\"Promotion\",\"url\":\"/settings/alarm/driver\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10016, 10052, 1, 'Device Alarm', 'settingsDeviceAlarm', 3, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"设备告警\",\"en\":\"Device Alarm\"},\"icon\":\"Management\",\"url\":\"/settings/alarm/device\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10036, 10052, 1, 'Point Alarm', 'settingsPointAlarm', 3, 6,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"点位告警\",\"en\":\"Point Alarm\"},\"icon\":\"TrendCharts\",\"url\":\"/settings/alarm/point\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10052, 10006, 0, 'Operations & History', 'settingsEventCommand', 2, 7,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"运行与历史\",\"en\":\"Operations & History\"},\"icon\":\"Operation\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10035, 10052, 1, 'Event History', 'settingsEventHistory', 3, 7,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"事件历史\",\"en\":\"Event History\"},\"icon\":\"Document\",\"url\":\"/settings/event/history\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10032, 10052, 1, 'Command History', 'settingsCommandHistory', 3, 8,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"指令历史\",\"en\":\"Command History\"},\"icon\":\"Document\",\"url\":\"/settings/command/history\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10053, 10006, 0, 'Audit', 'settingsAudit', 2, 8,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"审计\",\"en\":\"Audit\"},\"icon\":\"Files\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10044, 10053, 1, 'Identity Audit', 'settingsIdentityAudit', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"身份审计\",\"en\":\"Identity Audit\"},\"icon\":\"DocumentChecked\",\"url\":\"/settings/identity_audit\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10043, 10053, 1, 'MCP Audit', 'settingsMcpAudit', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 审计\",\"en\":\"MCP Audit\"},\"icon\":\"Document\",\"url\":\"/settings/mcp_audit\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10054, 10006, 0, 'MCP Integration', 'settingsIntegration', 2, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 集成\",\"en\":\"MCP Integration\"},\"icon\":\"Share\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10037, 10054, 1, 'MCP Service', 'settingsMcpServer', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 服务\",\"en\":\"MCP Service\"},\"icon\":\"Connection\",\"url\":\"/settings/mcp\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10056, 10054, 1, 'MCP Connection', 'settingsMcpConnection', 3, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 连接\",\"en\":\"MCP Connection\"},\"icon\":\"Link\",\"url\":\"/settings/mcp/connection\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10057, 10054, 1, 'MCP Client', 'settingsMcpClient', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 客户端\",\"en\":\"MCP Client\"},\"icon\":\"Ticket\",\"url\":\"/settings/mcp/client\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10058, 10054, 1, 'MCP Tool', 'settingsMcpTool', 3, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 工具\",\"en\":\"MCP Tool\"},\"icon\":\"Tools\",\"url\":\"/settings/mcp/tool\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10055, 10006, 0, 'Metadata', 'settingsSystem', 2, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"元数据\",\"en\":\"Metadata\"},\"icon\":\"Collection\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10018, 10055, 1, 'Group', 'settingsGroup', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"分组管理\",\"en\":\"Groups\"},\"icon\":\"Grid\",\"url\":\"/settings/group\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10019, 10055, 1, 'Label', 'settingsLabel', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"标签管理\",\"en\":\"Labels\"},\"icon\":\"CollectionTag\",\"url\":\"/settings/label\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (10014, 10006, 1, 'About', 'settingsAbout', 2, 9,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"关于\",\"en\":\"About\"},\"icon\":\"InfoFilled\",\"url\":\"/settings/about\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_resource (MENU-type mirrors of the menu seed)
-- resource_type_flag: 5=MENU    resource_scope_flag: 3=LIST
-- parent_resource_id mirrors the dc3_menu hierarchy through the corresponding MENU resource
-- service_name: blank for seeded menu resources; API resources are service-scoped by runtime registration
-- entity_id: points back to dc3_menu.id so MenuServiceImpl lookups resolve cleanly
-- ----------------------------
INSERT INTO dc3_resource (id, parent_resource_id, resource_name, resource_code, service_name, resource_type_flag,
                          resource_scope_flag, entity_id, resource_ext, enable_flag, remark, creator_id, creator_name,
                          create_time, operator_id, operator_name, operate_time, deleted)
VALUES (20000, 0, 'Administrator Wildcard', '*', '', 6, 3, 0, '{}', 0,
        'Grants all permissions to the default administrator role.', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3',
        '2026-05-01 00:00:00', 0),
       (20001, 0, 'Home', 'menu:home', '', 5, 3, 10001, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00', 1, 'dc3',
        '2026-05-01 00:00:00', 0),
       (20002, 0, 'Driver', 'menu:driver', '', 5, 3, 10002, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00', 1,
        'dc3', '2026-05-01 00:00:00', 0),
       (20003, 0, 'Profile', 'menu:profile', '', 5, 3, 10003, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00', 1,
        'dc3', '2026-05-01 00:00:00', 0),
       (20004, 0, 'Device', 'menu:device', '', 5, 3, 10004, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00', 1,
        'dc3', '2026-05-01 00:00:00', 0),
       (20005, 0, 'PointValue', 'menu:pointValue', '', 5, 3, 10005, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00',
        1, 'dc3', '2026-05-01 00:00:00', 0),
       (20006, 0, 'Settings', 'menu:settings', '', 5, 3, 10006, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00', 1,
        'dc3', '2026-05-01 00:00:00', 0),
       (20050, 20006, 'Accounts & Identity', 'menu:settingsIdentity', '', 5, 3, 10050, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20007, 20050, 'User', 'menu:settingsUser', '', 5, 3, 10007, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00',
        1, 'dc3', '2026-05-01 00:00:00', 0),
       (20040, 20050, 'Identity Principals', 'menu:settingsPrincipal', '', 5, 3, 10040, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20041, 20050, 'Tenant Membership', 'menu:settingsTenantMembership', '', 5, 3, 10041, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20042, 20050, 'Local Credentials', 'menu:settingsLocalCredential', '', 5, 3, 10042, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20038, 20050, 'Service Accounts', 'menu:settingsServiceAccount', '', 5, 3, 10038, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20051, 20006, 'Roles & Permissions', 'menu:settingsAccess', '', 5, 3, 10051, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20008, 20051, 'Role', 'menu:settingsRole', '', 5, 3, 10008, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00',
        1, 'dc3', '2026-05-01 00:00:00', 0),
       (20039, 20051, 'Role Assignments', 'menu:settingsRolePrincipalBind', '', 5, 3, 10039, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20009, 20051, 'Resource', 'menu:settingsResource', '', 5, 3, 10009, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20010, 20051, 'API', 'menu:settingsApi', '', 5, 3, 10010, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00',
        1, 'dc3', '2026-05-01 00:00:00', 0),
       (20011, 20051, 'Menu', 'menu:settingsMenu', '', 5, 3, 10011, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00',
        1, 'dc3', '2026-05-01 00:00:00', 0),
       (20013, 20006, 'AI Models', 'menu:settingsModel', '', 5, 3, 10013, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20020, 20013, 'Model Config', 'menu:settingsModelConfig', '', 5, 3, 10020, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20021, 20013, 'Model Providers', 'menu:settingsModelProvider', '', 5, 3, 10021, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20022, 20006, 'Alarm Configuration', 'menu:settingsAlarm', '', 5, 3, 10022, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20023, 20022, 'Alarm Rules', 'menu:settingsAlarmRule', '', 5, 3, 10023, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20024, 20022, 'Notification Policies', 'menu:settingsAlarmNotify', '', 5, 3, 10024, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20025, 20022, 'Message Templates', 'menu:settingsAlarmMessage', '', 5, 3, 10025, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20026, 20022, 'Alarm Channels', 'menu:settingsAlarmChannel', '', 5, 3, 10026, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20027, 20022, 'Alarm Bindings', 'menu:settingsAlarmBind', '', 5, 3, 10027, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20015, 20052, 'Overview', 'menu:settingsAlarmOverview', '', 5, 3, 10015, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20028, 20052, 'Alarm States', 'menu:settingsAlarmState', '', 5, 3, 10028, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20029, 20052, 'Alarm History', 'menu:settingsAlarmHistory', '', 5, 3, 10029, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20017, 20052, 'Driver Alarm', 'menu:settingsDriverAlarm', '', 5, 3, 10017, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20016, 20052, 'Device Alarm', 'menu:settingsDeviceAlarm', '', 5, 3, 10016, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20036, 20052, 'Point Alarm', 'menu:settingsPointAlarm', '', 5, 3, 10036, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20052, 20006, 'Operations & History', 'menu:settingsEventCommand', '', 5, 3, 10052, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20035, 20052, 'Event History', 'menu:settingsEventHistory', '', 5, 3, 10035, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20032, 20052, 'Command History', 'menu:settingsCommandHistory', '', 5, 3, 10032, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20053, 20006, 'Audit', 'menu:settingsAudit', '', 5, 3, 10053, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20044, 20053, 'Identity Audit', 'menu:settingsIdentityAudit', '', 5, 3, 10044, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20043, 20053, 'MCP Audit', 'menu:settingsMcpAudit', '', 5, 3, 10043, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20054, 20006, 'MCP Integration', 'menu:settingsIntegration', '', 5, 3, 10054, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20037, 20054, 'MCP Service', 'menu:settingsMcpServer', '', 5, 3, 10037, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20056, 20054, 'MCP Connection', 'menu:settingsMcpConnection', '', 5, 3, 10056, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20057, 20054, 'MCP Client', 'menu:settingsMcpClient', '', 5, 3, 10057, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20058, 20054, 'MCP Tool', 'menu:settingsMcpTool', '', 5, 3, 10058, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20055, 20006, 'Metadata', 'menu:settingsSystem', '', 5, 3, 10055, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20018, 20055, 'Group', 'menu:settingsGroup', '', 5, 3, 10018, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20019, 20055, 'Label', 'menu:settingsLabel', '', 5, 3, 10019, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0),
       (20014, 20006, 'About', 'menu:settingsAbout', '', 5, 3, 10014, '{}', 0, '', 1, 'dc3',
        '2026-05-01 00:00:00', 1, 'dc3', '2026-05-01 00:00:00', 0);

-- ----------------------------
-- Records of dc3_role_resource_bind
-- ----------------------------
INSERT INTO dc3_role_resource_bind (id, role_id, resource_id, remark, creator_id, creator_name, create_time,
                                    operator_id, operator_name, operate_time, deleted)
VALUES (1, 1, 20000, 'Default administrator wildcard permission binding', 1, 'dc3', '2026-05-01 00:00:00', 1,
        'dc3',
        '2026-05-01 00:00:00', 0);

-- ----------------------------
-- Table structure for dc3_identity_audit_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS dc3_identity_audit_log
(
    id             BIGINT PRIMARY KEY                       NOT NULL, -- Primary key ID
    tenant_id      BIGINT      DEFAULT 0                    NOT NULL, -- Tenant ID
    principal_id   BIGINT      DEFAULT 0                    NOT NULL, -- Principal ID
    principal_type TEXT        DEFAULT ('USER')             NOT NULL, -- Principal type
    action         TEXT        DEFAULT ('')                 NOT NULL, -- Audited action
    resource_type  TEXT        DEFAULT ('')                 NOT NULL, -- Audited resource type
    resource_id    BIGINT      DEFAULT 0                    NOT NULL, -- Audited resource ID
    resource_name  TEXT        DEFAULT ('')                 NOT NULL, -- Audited resource name
    status         TEXT        DEFAULT ('')                 NOT NULL, -- Result status
    error_code     TEXT        DEFAULT ('')                 NOT NULL, -- Stable error code
    detail_ext     JSON        DEFAULT ('{}')               NOT NULL, -- Structured audit details
    create_time    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL, -- Creation time
    deleted        SMALLINT    DEFAULT 0                    NOT NULL, -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_identity_audit_deleted CHECK (deleted IN (0, 1))
);






