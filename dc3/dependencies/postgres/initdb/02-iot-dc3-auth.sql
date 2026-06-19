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

CREATE SCHEMA IF NOT EXISTS dc3_auth;
SET search_path TO dc3_auth;

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
-- Table structure for dc3_tenant
-- ----------------------------
CREATE TABLE dc3_tenant
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    tenant_name   TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Tenant name
    tenant_code   TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Tenant code
    tenant_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Tenant extension information
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_tenant_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_tenant_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_tenant_code_active_unique ON dc3_tenant (tenant_code) WHERE deleted = 0 AND tenant_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_tenant
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_tenant IS 'Tenant table';
COMMENT
ON COLUMN dc3_tenant.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_tenant.tenant_name IS 'Tenant name';
COMMENT
ON COLUMN dc3_tenant.tenant_code IS 'Tenant code';
COMMENT
ON COLUMN dc3_tenant.tenant_ext IS 'Tenant extension information';
COMMENT
ON COLUMN dc3_tenant.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_tenant.remark IS 'Description';
COMMENT
ON COLUMN dc3_tenant.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_tenant.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_tenant.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_tenant.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_tenant.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_tenant.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_tenant.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_principal
-- ----------------------------
CREATE TABLE dc3_principal
(
    id             BIGINT PRIMARY KEY NOT NULL,                  -- Primary key ID
    principal_type TEXT     DEFAULT 'USER'::TEXT   NOT NULL,     -- Principal type, USER: user, SERVICE_ACCOUNT: service account, SYSTEM: system principal
    principal_name TEXT     DEFAULT ''::TEXT       NOT NULL,     -- Principal name
    display_name   TEXT     DEFAULT ''::TEXT       NOT NULL,     -- Principal display name
    source_type    TEXT     DEFAULT 'LOCAL'::TEXT  NOT NULL,     -- Principal source type, LOCAL: local, EXTERNAL: external, SYSTEM: system
    enable_flag    SMALLINT DEFAULT 0 NOT NULL,                  -- Enable flag, 0: enabled, 1: disabled
    locked_flag    SMALLINT DEFAULT 0 NOT NULL,                  -- Locked flag, 0: unlocked, 1: locked
    last_login_time TIMESTAMPTZ,                                 -- Last login time
    principal_ext  JSON     DEFAULT '{}'::JSON     NOT NULL,     -- Principal extension information
    remark         TEXT     DEFAULT ''::TEXT       NOT NULL,     -- Description
    creator_id     BIGINT   DEFAULT 0 NOT NULL,                  -- Creator ID
    creator_name   TEXT     DEFAULT ''::TEXT       NOT NULL,     -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id    BIGINT   DEFAULT 0 NOT NULL,                  -- Operator ID
    operator_name  TEXT     DEFAULT ''::TEXT       NOT NULL,     -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted        SMALLINT DEFAULT 0 NOT NULL,                  -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_principal_source_type CHECK (source_type IN ('LOCAL', 'EXTERNAL', 'SYSTEM')),
    CONSTRAINT chk_principal_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_principal_locked_flag CHECK (locked_flag IN (0, 1)),
    CONSTRAINT chk_principal_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_principal_name_type_active_unique
    ON dc3_principal (principal_type, principal_name) WHERE deleted = 0 AND principal_name <> ''::TEXT;
CREATE INDEX idx_principal_type ON dc3_principal (principal_type) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_principal
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_principal IS 'Unified principal table for users, service accounts and system identities';
COMMENT
ON COLUMN dc3_principal.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_principal.principal_type IS 'Principal type, USER: user, SERVICE_ACCOUNT: service account, SYSTEM: system principal';
COMMENT
ON COLUMN dc3_principal.principal_name IS 'Principal name';
COMMENT
ON COLUMN dc3_principal.display_name IS 'Principal display name';
COMMENT
ON COLUMN dc3_principal.source_type IS 'Principal source type, LOCAL: local, EXTERNAL: external, SYSTEM: system';
COMMENT
ON COLUMN dc3_principal.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_principal.locked_flag IS 'Locked flag, 0: unlocked, 1: locked';
COMMENT
ON COLUMN dc3_principal.last_login_time IS 'Last login time';
COMMENT
ON COLUMN dc3_principal.principal_ext IS 'Principal extension information';
COMMENT
ON COLUMN dc3_principal.remark IS 'Description';
COMMENT
ON COLUMN dc3_principal.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_principal.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_principal.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_principal.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_principal.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_principal.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_principal.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_user
-- ----------------------------
CREATE TABLE dc3_user
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    principal_id  BIGINT   DEFAULT 0 NOT NULL,                   -- Principal ID
    user_name     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Username
    nick_name     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- User nickname
    phone         TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Phone number
    email         TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Email
    social_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Social extension information
    identity_ext  JSON     DEFAULT '{}'::JSON        NOT NULL,   -- Identity extension information
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_user_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_user_deleted CHECK (deleted IN (0, 1))
);

-- Unique within non-deleted users; empty values are excluded so optional phone/email may be blank for many users.
CREATE UNIQUE INDEX idx_user_principal_active_unique ON dc3_user (principal_id) WHERE deleted = 0 AND principal_id <> 0;
CREATE UNIQUE INDEX idx_user_name_active_unique ON dc3_user (user_name) WHERE deleted = 0 AND user_name <> ''::TEXT;
CREATE UNIQUE INDEX idx_user_phone_active_unique ON dc3_user (phone) WHERE deleted = 0 AND phone <> ''::TEXT;
CREATE UNIQUE INDEX idx_user_email_active_unique ON dc3_user (email) WHERE deleted = 0 AND email <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_user
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_user IS 'User table';
COMMENT
ON COLUMN dc3_user.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_user.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_user.user_name IS 'Username';
COMMENT
ON COLUMN dc3_user.nick_name IS 'User nickname';
COMMENT
ON COLUMN dc3_user.phone IS 'Phone number';
COMMENT
ON COLUMN dc3_user.email IS 'Email';
COMMENT
ON COLUMN dc3_user.social_ext IS 'Social extension information';
COMMENT
ON COLUMN dc3_user.identity_ext IS 'Identity extension information';
COMMENT
ON COLUMN dc3_user.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_user.remark IS 'Description';
COMMENT
ON COLUMN dc3_user.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_user.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_user.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_user.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_user.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_user.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_user.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_local_credential
-- ----------------------------
CREATE TABLE dc3_local_credential
(
    id                      BIGINT PRIMARY KEY NOT NULL,                -- Primary key ID
    principal_id            BIGINT   DEFAULT 0 NOT NULL,                -- Principal ID
    login_name              TEXT     DEFAULT ''::TEXT         NOT NULL, -- Login name
    login_name_normalized   TEXT     DEFAULT ''::TEXT         NOT NULL, -- Normalized login name
    credential_type         TEXT     DEFAULT 'PASSWORD'::TEXT NOT NULL, -- Credential type
    password_hash           TEXT     DEFAULT ''::TEXT         NOT NULL, -- Password hash
    password_algorithm      TEXT     DEFAULT 'ARGON2ID'::TEXT NOT NULL, -- Password hash algorithm
    password_params         JSON     DEFAULT '{}'::JSON       NOT NULL, -- Password hash parameters
    password_updated_time TIMESTAMPTZ,                                  -- Password update time
    password_expire_time TIMESTAMPTZ,                                   -- Password expiration time
    failed_attempts         INTEGER  DEFAULT 0 NOT NULL,                -- Failed login attempts
    locked_until TIMESTAMPTZ,                                           -- Credential locked until time
    require_password_change SMALLINT DEFAULT 1 NOT NULL,                -- Require password change flag, 0: no, 1: yes
    enable_flag             SMALLINT DEFAULT 0 NOT NULL,                -- Enable flag, 0: enabled, 1: disabled
    credential_ext          JSON     DEFAULT '{}'::JSON       NOT NULL, -- Credential extension information
    remark                  TEXT     DEFAULT ''::TEXT         NOT NULL, -- Description
    creator_id              BIGINT   DEFAULT 0 NOT NULL,                -- Creator ID
    creator_name            TEXT     DEFAULT ''::TEXT         NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,         -- Creation time
    operator_id             BIGINT   DEFAULT 0 NOT NULL,                -- Operator ID
    operator_name           TEXT     DEFAULT ''::TEXT         NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,        -- Operation time
    deleted                 SMALLINT DEFAULT 0 NOT NULL,                -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_local_credential_type CHECK (credential_type IN ('PASSWORD')),
    CONSTRAINT chk_local_credential_password_algorithm CHECK (password_algorithm IN ('ARGON2ID', 'BCRYPT')),
    CONSTRAINT chk_local_credential_require_change CHECK (require_password_change IN (0, 1)),
    CONSTRAINT chk_local_credential_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_local_credential_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_local_credential_login_active_unique
    ON dc3_local_credential (credential_type, login_name_normalized) WHERE deleted = 0 AND login_name_normalized <> ''::TEXT;
CREATE UNIQUE INDEX idx_local_credential_principal_active_unique
    ON dc3_local_credential (principal_id, credential_type) WHERE deleted = 0 AND principal_id <> 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_local_credential
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_local_credential IS 'Local password credential table';
COMMENT
ON COLUMN dc3_local_credential.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_local_credential.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_local_credential.login_name IS 'Login name';
COMMENT
ON COLUMN dc3_local_credential.login_name_normalized IS 'Normalized login name';
COMMENT
ON COLUMN dc3_local_credential.credential_type IS 'Credential type';
COMMENT
ON COLUMN dc3_local_credential.password_hash IS 'Password hash';
COMMENT
ON COLUMN dc3_local_credential.password_algorithm IS 'Password hash algorithm';
COMMENT
ON COLUMN dc3_local_credential.password_params IS 'Password hash parameters';
COMMENT
ON COLUMN dc3_local_credential.password_updated_time IS 'Password update time';
COMMENT
ON COLUMN dc3_local_credential.password_expire_time IS 'Password expiration time';
COMMENT
ON COLUMN dc3_local_credential.failed_attempts IS 'Failed login attempts';
COMMENT
ON COLUMN dc3_local_credential.locked_until IS 'Credential locked until time';
COMMENT
ON COLUMN dc3_local_credential.require_password_change IS 'Require password change flag, 0: no, 1: yes';
COMMENT
ON COLUMN dc3_local_credential.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_local_credential.credential_ext IS 'Credential extension information';
COMMENT
ON COLUMN dc3_local_credential.remark IS 'Description';
COMMENT
ON COLUMN dc3_local_credential.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_local_credential.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_local_credential.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_local_credential.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_local_credential.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_local_credential.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_local_credential.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_tenant_membership
-- ----------------------------
CREATE TABLE dc3_tenant_membership
(
    id                BIGINT PRIMARY KEY NOT NULL,               -- Primary key ID
    tenant_id         BIGINT   DEFAULT 0 NOT NULL,               -- Tenant ID
    principal_id      BIGINT   DEFAULT 0 NOT NULL,               -- Principal ID
    principal_type    TEXT     DEFAULT 'USER'::TEXT   NOT NULL,  -- Principal type
    membership_status TEXT     DEFAULT 'ACTIVE'::TEXT NOT NULL,  -- Membership status
    joined_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Joined time
    membership_ext    JSON     DEFAULT '{}'::JSON     NOT NULL,  -- Membership extension information
    remark            TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Description
    creator_id        BIGINT   DEFAULT 0 NOT NULL,               -- Creator ID
    creator_name      TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id       BIGINT   DEFAULT 0 NOT NULL,               -- Operator ID
    operator_name     TEXT     DEFAULT ''::TEXT       NOT NULL,  -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted           SMALLINT DEFAULT 0 NOT NULL,               -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_tenant_membership_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_tenant_membership_status CHECK (membership_status IN ('ACTIVE', 'SUSPENDED', 'INVITED')),
    CONSTRAINT chk_tenant_membership_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_tenant_membership_active_unique
    ON dc3_tenant_membership (tenant_id, principal_id) WHERE deleted = 0;
CREATE INDEX idx_tenant_membership_principal
    ON dc3_tenant_membership (principal_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_tenant_membership
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_tenant_membership IS 'Tenant membership table for principals';
COMMENT
ON COLUMN dc3_tenant_membership.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_tenant_membership.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_tenant_membership.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_tenant_membership.principal_type IS 'Principal type';
COMMENT
ON COLUMN dc3_tenant_membership.membership_status IS 'Membership status';
COMMENT
ON COLUMN dc3_tenant_membership.joined_time IS 'Joined time';
COMMENT
ON COLUMN dc3_tenant_membership.membership_ext IS 'Membership extension information';
COMMENT
ON COLUMN dc3_tenant_membership.remark IS 'Description';
COMMENT
ON COLUMN dc3_tenant_membership.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_tenant_membership.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_tenant_membership.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_tenant_membership.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_tenant_membership.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_tenant_membership.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_tenant_membership.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_service_account
-- ----------------------------
CREATE TABLE dc3_service_account
(
    id                    BIGINT PRIMARY KEY NOT NULL,           -- Primary key ID
    principal_id          BIGINT   DEFAULT 0 NOT NULL,           -- Principal ID
    tenant_id             BIGINT   DEFAULT 0 NOT NULL,           -- Tenant ID
    service_account_name  TEXT     DEFAULT ''::TEXT NOT NULL,    -- Service account name
    owner_principal_id    BIGINT   DEFAULT 0 NOT NULL,           -- Owner principal ID
    purpose               TEXT     DEFAULT ''::TEXT NOT NULL,    -- Service account purpose
    expire_time TIMESTAMPTZ,                                     -- Expiration time
    last_used_time TIMESTAMPTZ,                                  -- Last used time
    credential_policy_ext JSON     DEFAULT '{}'::JSON NOT NULL,  -- Credential policy extension information
    enable_flag           SMALLINT DEFAULT 0 NOT NULL,           -- Enable flag, 0: enabled, 1: disabled
    remark                TEXT     DEFAULT ''::TEXT NOT NULL,    -- Description
    creator_id            BIGINT   DEFAULT 0 NOT NULL,           -- Creator ID
    creator_name          TEXT     DEFAULT ''::TEXT NOT NULL,    -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id           BIGINT   DEFAULT 0 NOT NULL,           -- Operator ID
    operator_name         TEXT     DEFAULT ''::TEXT NOT NULL,    -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted               SMALLINT DEFAULT 0 NOT NULL,           -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_service_account_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_service_account_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_service_account_principal_active_unique
    ON dc3_service_account (principal_id) WHERE deleted = 0;
CREATE UNIQUE INDEX idx_service_account_tenant_name_active_unique
    ON dc3_service_account (tenant_id, service_account_name) WHERE deleted = 0 AND service_account_name <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_service_account
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_service_account IS 'Service account table';
COMMENT
ON COLUMN dc3_service_account.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_service_account.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_service_account.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_service_account.service_account_name IS 'Service account name';
COMMENT
ON COLUMN dc3_service_account.owner_principal_id IS 'Owner principal ID';
COMMENT
ON COLUMN dc3_service_account.purpose IS 'Service account purpose';
COMMENT
ON COLUMN dc3_service_account.expire_time IS 'Expiration time';
COMMENT
ON COLUMN dc3_service_account.last_used_time IS 'Last used time';
COMMENT
ON COLUMN dc3_service_account.credential_policy_ext IS 'Credential policy extension information';
COMMENT
ON COLUMN dc3_service_account.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_service_account.remark IS 'Description';
COMMENT
ON COLUMN dc3_service_account.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_service_account.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_service_account.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_service_account.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_service_account.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_service_account.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_service_account.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_identity_provider
-- ----------------------------
CREATE TABLE dc3_identity_provider
(
    id                BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    tenant_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Tenant ID
    provider_code     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Identity provider code
    provider_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Identity provider name
    provider_type     TEXT     DEFAULT 'OIDC'::TEXT      NOT NULL, -- Identity provider type
    issuer            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Issuer
    discovery_url     TEXT     DEFAULT ''::TEXT          NOT NULL, -- OIDC discovery URL
    authorization_uri TEXT     DEFAULT ''::TEXT          NOT NULL, -- Authorization URI
    token_uri         TEXT     DEFAULT ''::TEXT          NOT NULL, -- Token URI
    user_info_uri     TEXT     DEFAULT ''::TEXT          NOT NULL, -- User info URI
    jwks_uri          TEXT     DEFAULT ''::TEXT          NOT NULL, -- JWKS URI
    client_id         TEXT     DEFAULT ''::TEXT          NOT NULL, -- OAuth client ID
    client_secret_ref TEXT     DEFAULT ''::TEXT          NOT NULL, -- OAuth client secret reference
    scopes            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Requested scopes
    redirect_uri      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Redirect URI
    subject_claim     TEXT     DEFAULT 'sub'::TEXT       NOT NULL, -- Subject claim name
    username_claim    TEXT     DEFAULT ''::TEXT          NOT NULL, -- Username claim name
    email_claim       TEXT     DEFAULT 'email'::TEXT     NOT NULL, -- Email claim name
    attribute_mapping JSON     DEFAULT '{}'::JSON        NOT NULL, -- Attribute mapping
    provisioning_mode TEXT     DEFAULT 'LINK_ONLY'::TEXT NOT NULL, -- Provisioning mode
    enable_flag       SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    provider_ext      JSON     DEFAULT '{}'::JSON        NOT NULL, -- Provider extension information
    remark            TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id        BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name      TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,    -- Creation time
    operator_id       BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name     TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,   -- Operation time
    deleted           SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_identity_provider_type CHECK (provider_type IN ('GOOGLE', 'GITHUB', 'OIDC', 'SAML')),
    CONSTRAINT chk_identity_provider_provisioning CHECK (provisioning_mode IN ('LINK_ONLY', 'JIT')),
    CONSTRAINT chk_identity_provider_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_identity_provider_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_identity_provider_code_active_unique
    ON dc3_identity_provider (tenant_id, provider_code) WHERE deleted = 0 AND provider_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_identity_provider
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_identity_provider IS 'External identity provider table';
COMMENT
ON COLUMN dc3_identity_provider.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_identity_provider.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_identity_provider.provider_code IS 'Identity provider code';
COMMENT
ON COLUMN dc3_identity_provider.provider_name IS 'Identity provider name';
COMMENT
ON COLUMN dc3_identity_provider.provider_type IS 'Identity provider type';
COMMENT
ON COLUMN dc3_identity_provider.issuer IS 'Issuer';
COMMENT
ON COLUMN dc3_identity_provider.discovery_url IS 'OIDC discovery URL';
COMMENT
ON COLUMN dc3_identity_provider.authorization_uri IS 'Authorization URI';
COMMENT
ON COLUMN dc3_identity_provider.token_uri IS 'Token URI';
COMMENT
ON COLUMN dc3_identity_provider.user_info_uri IS 'User info URI';
COMMENT
ON COLUMN dc3_identity_provider.jwks_uri IS 'JWKS URI';
COMMENT
ON COLUMN dc3_identity_provider.client_id IS 'OAuth client ID';
COMMENT
ON COLUMN dc3_identity_provider.client_secret_ref IS 'OAuth client secret reference';
COMMENT
ON COLUMN dc3_identity_provider.scopes IS 'Requested scopes';
COMMENT
ON COLUMN dc3_identity_provider.redirect_uri IS 'Redirect URI';
COMMENT
ON COLUMN dc3_identity_provider.subject_claim IS 'Subject claim name';
COMMENT
ON COLUMN dc3_identity_provider.username_claim IS 'Username claim name';
COMMENT
ON COLUMN dc3_identity_provider.email_claim IS 'Email claim name';
COMMENT
ON COLUMN dc3_identity_provider.attribute_mapping IS 'Attribute mapping';
COMMENT
ON COLUMN dc3_identity_provider.provisioning_mode IS 'Provisioning mode';
COMMENT
ON COLUMN dc3_identity_provider.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_identity_provider.provider_ext IS 'Provider extension information';
COMMENT
ON COLUMN dc3_identity_provider.remark IS 'Description';
COMMENT
ON COLUMN dc3_identity_provider.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_identity_provider.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_identity_provider.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_identity_provider.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_identity_provider.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_identity_provider.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_identity_provider.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_external_identity
-- ----------------------------
CREATE TABLE dc3_external_identity
(
    id                 BIGINT PRIMARY KEY NOT NULL,              -- Primary key ID
    provider_id        BIGINT   DEFAULT 0 NOT NULL,              -- Identity provider ID
    principal_id       BIGINT   DEFAULT 0 NOT NULL,              -- Principal ID
    external_subject   TEXT     DEFAULT ''::TEXT NOT NULL,       -- External identity subject
    external_username  TEXT     DEFAULT ''::TEXT NOT NULL,       -- External username
    external_email     TEXT     DEFAULT ''::TEXT NOT NULL,       -- External email
    email_verified     SMALLINT DEFAULT 0 NOT NULL,              -- Email verified flag, 0: no, 1: yes
    first_login_time TIMESTAMPTZ,                                -- First login time
    last_login_time TIMESTAMPTZ,                                 -- Last login time
    last_claims_digest TEXT     DEFAULT ''::TEXT NOT NULL,       -- Last claims digest
    identity_ext       JSON     DEFAULT '{}'::JSON NOT NULL,     -- External identity extension information
    enable_flag        SMALLINT DEFAULT 0 NOT NULL,              -- Enable flag, 0: enabled, 1: disabled
    remark             TEXT     DEFAULT ''::TEXT NOT NULL,       -- Description
    creator_id         BIGINT   DEFAULT 0 NOT NULL,              -- Creator ID
    creator_name       TEXT     DEFAULT ''::TEXT NOT NULL,       -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id        BIGINT   DEFAULT 0 NOT NULL,              -- Operator ID
    operator_name      TEXT     DEFAULT ''::TEXT NOT NULL,       -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted            SMALLINT DEFAULT 0 NOT NULL,              -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_external_identity_email_verified CHECK (email_verified IN (0, 1)),
    CONSTRAINT chk_external_identity_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_external_identity_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_external_identity_subject_active_unique
    ON dc3_external_identity (provider_id, external_subject) WHERE deleted = 0 AND external_subject <> ''::TEXT;
CREATE UNIQUE INDEX idx_external_identity_principal_provider_active_unique
    ON dc3_external_identity (provider_id, principal_id) WHERE deleted = 0 AND principal_id <> 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_external_identity
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_external_identity IS 'External identity binding table';
COMMENT
ON COLUMN dc3_external_identity.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_external_identity.provider_id IS 'Identity provider ID';
COMMENT
ON COLUMN dc3_external_identity.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_external_identity.external_subject IS 'External identity subject';
COMMENT
ON COLUMN dc3_external_identity.external_username IS 'External username';
COMMENT
ON COLUMN dc3_external_identity.external_email IS 'External email';
COMMENT
ON COLUMN dc3_external_identity.email_verified IS 'Email verified flag, 0: no, 1: yes';
COMMENT
ON COLUMN dc3_external_identity.first_login_time IS 'First login time';
COMMENT
ON COLUMN dc3_external_identity.last_login_time IS 'Last login time';
COMMENT
ON COLUMN dc3_external_identity.last_claims_digest IS 'Last claims digest';
COMMENT
ON COLUMN dc3_external_identity.identity_ext IS 'External identity extension information';
COMMENT
ON COLUMN dc3_external_identity.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_external_identity.remark IS 'Description';
COMMENT
ON COLUMN dc3_external_identity.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_external_identity.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_external_identity.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_external_identity.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_external_identity.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_external_identity.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_external_identity.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_role
-- ----------------------------
CREATE TABLE dc3_role
(
    id             BIGINT PRIMARY KEY NOT NULL,                  -- Primary key ID
    parent_role_id BIGINT   DEFAULT 0 NOT NULL,                  -- Parent role ID
    role_name      TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Role name
    role_code      TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Role code
    role_ext       JSON     DEFAULT '{}'::JSON        NOT NULL,  -- Role extension information
    enable_flag    SMALLINT DEFAULT 0 NOT NULL,                  -- Enable flag, 0: enabled, 1: disabled
    tenant_id      BIGINT   DEFAULT 0 NOT NULL,                  -- Tenant ID
    remark         TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Description
    creator_id     BIGINT   DEFAULT 0 NOT NULL,                  -- Creator ID
    creator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id    BIGINT   DEFAULT 0 NOT NULL,                  -- Operator ID
    operator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted        SMALLINT DEFAULT 0 NOT NULL,                  -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_role_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_role_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_role_parent_role_id ON dc3_role (parent_role_id) WHERE deleted = 0;
CREATE UNIQUE INDEX idx_role_tenant_code_active_unique ON dc3_role (tenant_id, role_code) WHERE deleted = 0 AND role_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_role
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_role IS 'Role table';
COMMENT
ON COLUMN dc3_role.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_role.parent_role_id IS 'Parent role ID';
COMMENT
ON COLUMN dc3_role.role_name IS 'Role name';
COMMENT
ON COLUMN dc3_role.role_code IS 'Role code';
COMMENT
ON COLUMN dc3_role.role_ext IS 'Role extension information';
COMMENT
ON COLUMN dc3_role.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_role.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_role.remark IS 'Description';
COMMENT
ON COLUMN dc3_role.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_role.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_role.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_role.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_role.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_role.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_role.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_resource
-- ----------------------------
CREATE TABLE dc3_resource
(
    id                  BIGINT PRIMARY KEY NOT NULL,                 -- Primary key ID
    parent_resource_id  BIGINT   DEFAULT 0 NOT NULL,                 -- Parent resource ID
    resource_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Resource name
    resource_code       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Resource code
    service_name        TEXT     DEFAULT ''::TEXT          NOT NULL, -- Service name
    resource_type_flag  SMALLINT DEFAULT 0 NOT NULL,                 -- Resource type flag
    resource_scope_flag SMALLINT DEFAULT 0 NOT NULL,                 -- Resource scope flag
    entity_id           BIGINT   DEFAULT 0 NOT NULL,                 -- Resource entity ID
    resource_ext        JSON     DEFAULT '{}'::JSON        NOT NULL, -- Resource extension information
    enable_flag         SMALLINT DEFAULT 0 NOT NULL,                 -- Enable flag, 0: enabled, 1: disabled
    remark              TEXT     DEFAULT ''::TEXT          NOT NULL, -- Description
    creator_id          BIGINT   DEFAULT 0 NOT NULL,                 -- Creator ID
    creator_name        TEXT     DEFAULT ''::TEXT          NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,      -- Creation time
    operator_id         BIGINT   DEFAULT 0 NOT NULL,                 -- Operator ID
    operator_name       TEXT     DEFAULT ''::TEXT          NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,     -- Operation time
    deleted             SMALLINT DEFAULT 0 NOT NULL,                 -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_resource_resource_type_flag CHECK (resource_type_flag BETWEEN 0 AND 6),
    CONSTRAINT chk_resource_resource_scope_flag CHECK (resource_scope_flag BETWEEN 0 AND 4),
    CONSTRAINT chk_resource_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_resource_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_resource_parent_resource_id ON dc3_resource (parent_resource_id) WHERE deleted = 0;
CREATE INDEX idx_resource_type_entity ON dc3_resource (resource_type_flag, entity_id) WHERE deleted = 0;
CREATE UNIQUE INDEX idx_resource_code_service_unique ON dc3_resource (resource_code, service_name) WHERE deleted = 0 AND resource_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_resource
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_resource IS 'Resource table';
COMMENT
ON COLUMN dc3_resource.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_resource.parent_resource_id IS 'Parent resource ID';
COMMENT
ON COLUMN dc3_resource.resource_name IS 'Resource name';
COMMENT
ON COLUMN dc3_resource.resource_code IS 'Resource code';
COMMENT
ON COLUMN dc3_resource.resource_type_flag IS 'Resource type flag';
COMMENT
ON COLUMN dc3_resource.resource_scope_flag IS 'Resource scope flag';
COMMENT
ON COLUMN dc3_resource.entity_id IS 'Resource entity ID';
COMMENT
ON COLUMN dc3_resource.resource_ext IS 'Resource extension information';
COMMENT
ON COLUMN dc3_resource.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_resource.remark IS 'Description';
COMMENT
ON COLUMN dc3_resource.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_resource.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_resource.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_resource.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_resource.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_resource.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_resource.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_role_principal_bind
-- ----------------------------
CREATE TABLE dc3_role_principal_bind
(
    id             BIGINT PRIMARY KEY NOT NULL,                  -- Primary key ID
    tenant_id      BIGINT   DEFAULT 0 NOT NULL,                  -- Tenant ID
    role_id        BIGINT   DEFAULT 0 NOT NULL,                  -- Role ID
    principal_id   BIGINT   DEFAULT 0 NOT NULL,                  -- Principal ID
    principal_type TEXT     DEFAULT 'USER'::TEXT NOT NULL,       -- Principal type
    remark         TEXT     DEFAULT ''::TEXT     NOT NULL,       -- Description
    creator_id     BIGINT   DEFAULT 0 NOT NULL,                  -- Creator ID
    creator_name   TEXT     DEFAULT ''::TEXT     NOT NULL,       -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id    BIGINT   DEFAULT 0 NOT NULL,                  -- Operator ID
    operator_name  TEXT     DEFAULT ''::TEXT     NOT NULL,       -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted        SMALLINT DEFAULT 0 NOT NULL,                  -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_role_principal_bind_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_role_principal_bind_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_role_principal_bind_active_unique
    ON dc3_role_principal_bind (tenant_id, role_id, principal_id) WHERE deleted = 0;
CREATE INDEX idx_role_principal_bind_principal
    ON dc3_role_principal_bind (tenant_id, principal_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_role_principal_bind
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_role_principal_bind IS 'Association table between roles and principals';
COMMENT
ON COLUMN dc3_role_principal_bind.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_role_principal_bind.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_role_principal_bind.role_id IS 'Role ID';
COMMENT
ON COLUMN dc3_role_principal_bind.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_role_principal_bind.principal_type IS 'Principal type';
COMMENT
ON COLUMN dc3_role_principal_bind.remark IS 'Description';
COMMENT
ON COLUMN dc3_role_principal_bind.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_role_principal_bind.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_role_principal_bind.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_role_principal_bind.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_role_principal_bind.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_role_principal_bind.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_role_principal_bind.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_role_resource_bind
-- ----------------------------
CREATE TABLE dc3_role_resource_bind
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    role_id       BIGINT   DEFAULT 0 NOT NULL,                   -- Role ID
    resource_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Resource ID
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_role_resource_bind_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_role_resource_bind_active_unique ON dc3_role_resource_bind (role_id, resource_id) WHERE deleted = 0;
CREATE INDEX idx_role_resource_bind_resource_id ON dc3_role_resource_bind (resource_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_role_resource_bind
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_role_resource_bind IS 'Association table between roles and resources';
COMMENT
ON COLUMN dc3_role_resource_bind.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_role_resource_bind.role_id IS 'Role ID';
COMMENT
ON COLUMN dc3_role_resource_bind.resource_id IS 'Resource ID';
COMMENT
ON COLUMN dc3_role_resource_bind.remark IS 'Description';
COMMENT
ON COLUMN dc3_role_resource_bind.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_role_resource_bind.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_role_resource_bind.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_role_resource_bind.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_role_resource_bind.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_role_resource_bind.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_role_resource_bind.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_driver_token
-- ----------------------------
CREATE TABLE dc3_driver_token
(
    id             BIGINT PRIMARY KEY NOT NULL,                  -- Primary key ID
    driver_code    TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Driver code
    driver_app_id  TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Driver App ID
    driver_app_key TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Driver App Key
    expire_flag    SMALLINT DEFAULT 0 NOT NULL,                  -- Expiration flag
    expire_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Expiration time
    enable_flag    SMALLINT DEFAULT 0 NOT NULL,                  -- Enable flag, 0: enabled, 1: disabled
    tenant_id      BIGINT   DEFAULT 0 NOT NULL,                  -- Tenant ID
    remark         TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Description
    creator_id     BIGINT   DEFAULT 0 NOT NULL,                  -- Creator ID
    creator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id    BIGINT   DEFAULT 0 NOT NULL,                  -- Operator ID
    operator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted        SMALLINT DEFAULT 0 NOT NULL,                  -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_driver_token_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_driver_token_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_driver_token_tenant_code_active_unique ON dc3_driver_token (tenant_id, driver_code) WHERE deleted = 0 AND driver_code <> ''::TEXT;
CREATE UNIQUE INDEX idx_driver_token_app_id_active_unique ON dc3_driver_token (driver_app_id) WHERE deleted = 0 AND driver_app_id <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_driver_token
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_driver_token IS 'Driver token table';
COMMENT
ON COLUMN dc3_driver_token.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_driver_token.driver_code IS 'Driver code';
COMMENT
ON COLUMN dc3_driver_token.driver_app_id IS 'Driver App ID';
COMMENT
ON COLUMN dc3_driver_token.driver_app_key IS 'Driver App Key';
COMMENT
ON COLUMN dc3_driver_token.expire_flag IS 'Expiration flag';
COMMENT
ON COLUMN dc3_driver_token.expire_time IS 'Expiration time';
COMMENT
ON COLUMN dc3_driver_token.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_driver_token.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_driver_token.remark IS 'Description';
COMMENT
ON COLUMN dc3_driver_token.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_driver_token.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_driver_token.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_driver_token.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_driver_token.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_driver_token.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_driver_token.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_api
-- ----------------------------
CREATE TABLE dc3_api
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    service_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Owning service name, populated by resource registrar
    api_type_flag SMALLINT DEFAULT 0 NOT NULL,                   -- API type flag
    api_name      TEXT     DEFAULT ''::TEXT          NOT NULL,   -- API name
    api_code      TEXT     DEFAULT ''::TEXT          NOT NULL,   -- API code
    api_group     TEXT     DEFAULT ''::TEXT          NOT NULL,   -- API grouping (controller simple name)
    api_ext       JSON     DEFAULT '{}'::JSON        NOT NULL,   -- API extension information
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Description
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT          NOT NULL,   -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_api_api_type_flag CHECK (api_type_flag BETWEEN 0 AND 3),
    CONSTRAINT chk_api_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_api_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_api_service_name ON dc3_api (service_name) WHERE deleted = 0;
CREATE UNIQUE INDEX idx_api_code_active_unique ON dc3_api (api_code) WHERE deleted = 0 AND api_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_api
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_api IS 'API table';
COMMENT
ON COLUMN dc3_api.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_api.service_name IS 'Owning service name, populated by resource registrar';
COMMENT
ON COLUMN dc3_api.api_type_flag IS 'API type flag';
COMMENT
ON COLUMN dc3_api.api_name IS 'API name';
COMMENT
ON COLUMN dc3_api.api_code IS 'API code';
COMMENT
ON COLUMN dc3_api.api_group IS 'API grouping (controller simple name)';
COMMENT
ON COLUMN dc3_api.api_ext IS 'API extension information';
COMMENT
ON COLUMN dc3_api.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_api.remark IS 'Description';
COMMENT
ON COLUMN dc3_api.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_api.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_api.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_api.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_api.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_api.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_api.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_oauth_registered_client
-- ----------------------------
CREATE TABLE dc3_oauth_registered_client
(
    id                           BIGINT PRIMARY KEY NOT NULL,              -- Primary key ID
    client_id                    TEXT     DEFAULT ''::TEXT       NOT NULL, -- OAuth client ID
    client_name                  TEXT     DEFAULT ''::TEXT       NOT NULL, -- OAuth client name
    client_type                  TEXT     DEFAULT 'PUBLIC'::TEXT NOT NULL, -- OAuth client type, PUBLIC: public client, CONFIDENTIAL: confidential client
    owner_principal_id           BIGINT   DEFAULT 0 NOT NULL,              -- Owner principal ID
    service_account_principal_id BIGINT   DEFAULT 0 NOT NULL,              -- Service account principal ID
    tenant_id                    BIGINT   DEFAULT 0 NOT NULL,              -- Tenant ID
    client_secret_hash           TEXT     DEFAULT ''::TEXT       NOT NULL, -- Client secret hash
    client_secret_expires_at TIMESTAMPTZ,                                  -- Client secret expiration time
    client_auth_methods          TEXT     DEFAULT ''::TEXT       NOT NULL, -- Client authentication methods
    authorization_grant_types    TEXT     DEFAULT ''::TEXT       NOT NULL, -- Authorization grant types
    redirect_uris                TEXT     DEFAULT ''::TEXT       NOT NULL, -- Redirect URIs
    scopes                       TEXT     DEFAULT ''::TEXT       NOT NULL, -- OAuth scopes
    jwks_uri                     TEXT     DEFAULT ''::TEXT       NOT NULL, -- Client JWKS URI
    jwk_set                      JSON     DEFAULT '{}'::JSON     NOT NULL, -- Client JWK set
    require_pkce                 SMALLINT DEFAULT 1 NOT NULL,              -- Require PKCE flag, 0: no, 1: yes
    require_consent              SMALLINT DEFAULT 1 NOT NULL,              -- Require consent flag, 0: no, 1: yes
    enable_flag                  SMALLINT DEFAULT 0 NOT NULL,              -- Enable flag, 0: enabled, 1: disabled
    client_settings              JSON     DEFAULT '{}'::JSON     NOT NULL, -- Client settings
    token_settings               JSON     DEFAULT '{}'::JSON     NOT NULL, -- Token settings
    remark                       TEXT     DEFAULT ''::TEXT       NOT NULL, -- Description
    creator_id                   BIGINT   DEFAULT 0 NOT NULL,              -- Creator ID
    creator_name                 TEXT     DEFAULT ''::TEXT       NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,            -- Creation time
    operator_id                  BIGINT   DEFAULT 0 NOT NULL,              -- Operator ID
    operator_name                TEXT     DEFAULT ''::TEXT       NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,           -- Operation time
    deleted                      SMALLINT DEFAULT 0 NOT NULL,              -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_oauth_client_type CHECK (client_type IN ('PUBLIC', 'CONFIDENTIAL')),
    CONSTRAINT chk_oauth_client_require_pkce CHECK (require_pkce IN (0, 1)),
    CONSTRAINT chk_oauth_client_require_consent CHECK (require_consent IN (0, 1)),
    CONSTRAINT chk_oauth_client_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_oauth_client_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_oauth_client_id_active_unique
    ON dc3_oauth_registered_client (client_id) WHERE deleted = 0 AND client_id <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_oauth_registered_client
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_oauth_registered_client IS 'OAuth registered client table';
COMMENT
ON COLUMN dc3_oauth_registered_client.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_oauth_registered_client.client_id IS 'OAuth client ID';
COMMENT
ON COLUMN dc3_oauth_registered_client.client_name IS 'OAuth client name';
COMMENT
ON COLUMN dc3_oauth_registered_client.client_type IS 'OAuth client type, PUBLIC: public client, CONFIDENTIAL: confidential client';
COMMENT
ON COLUMN dc3_oauth_registered_client.owner_principal_id IS 'Owner principal ID';
COMMENT
ON COLUMN dc3_oauth_registered_client.service_account_principal_id IS 'Service account principal ID';
COMMENT
ON COLUMN dc3_oauth_registered_client.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_oauth_registered_client.client_secret_hash IS 'Client secret hash';
COMMENT
ON COLUMN dc3_oauth_registered_client.client_secret_expires_at IS 'Client secret expiration time';
COMMENT
ON COLUMN dc3_oauth_registered_client.client_auth_methods IS 'Client authentication methods';
COMMENT
ON COLUMN dc3_oauth_registered_client.authorization_grant_types IS 'Authorization grant types';
COMMENT
ON COLUMN dc3_oauth_registered_client.redirect_uris IS 'Redirect URIs';
COMMENT
ON COLUMN dc3_oauth_registered_client.scopes IS 'OAuth scopes';
COMMENT
ON COLUMN dc3_oauth_registered_client.jwks_uri IS 'Client JWKS URI';
COMMENT
ON COLUMN dc3_oauth_registered_client.jwk_set IS 'Client JWK set';
COMMENT
ON COLUMN dc3_oauth_registered_client.require_pkce IS 'Require PKCE flag, 0: no, 1: yes';
COMMENT
ON COLUMN dc3_oauth_registered_client.require_consent IS 'Require consent flag, 0: no, 1: yes';
COMMENT
ON COLUMN dc3_oauth_registered_client.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_oauth_registered_client.client_settings IS 'Client settings';
COMMENT
ON COLUMN dc3_oauth_registered_client.token_settings IS 'Token settings';
COMMENT
ON COLUMN dc3_oauth_registered_client.remark IS 'Description';
COMMENT
ON COLUMN dc3_oauth_registered_client.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_oauth_registered_client.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_oauth_registered_client.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_oauth_registered_client.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_oauth_registered_client.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_oauth_registered_client.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_oauth_registered_client.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_oauth_authorization
-- ----------------------------
CREATE TABLE dc3_oauth_authorization
(
    id                          BIGINT PRIMARY KEY NOT NULL,            -- Primary key ID
    registered_client_id        BIGINT   DEFAULT 0 NOT NULL,            -- Registered client ID
    client_id                   TEXT     DEFAULT ''::TEXT     NOT NULL, -- OAuth client ID
    principal_id                BIGINT   DEFAULT 0 NOT NULL,            -- Principal ID
    principal_type              TEXT     DEFAULT 'USER'::TEXT NOT NULL, -- Principal type
    tenant_id                   BIGINT   DEFAULT 0 NOT NULL,            -- Tenant ID
    mcp_connection_id           BIGINT   DEFAULT 0 NOT NULL,            -- MCP connection ID
    authorization_grant_type    TEXT     DEFAULT ''::TEXT     NOT NULL, -- Authorization grant type
    authorized_scopes           TEXT     DEFAULT ''::TEXT     NOT NULL, -- Authorized scopes
    state_hash                  TEXT     DEFAULT ''::TEXT     NOT NULL, -- OAuth state hash
    authorization_code_hash     TEXT     DEFAULT ''::TEXT     NOT NULL, -- Authorization code hash
    authorization_code_issued TIMESTAMPTZ,                              -- Authorization code issued time
    authorization_code_expires TIMESTAMPTZ,                             -- Authorization code expiration time
    access_token_jti            TEXT     DEFAULT ''::TEXT     NOT NULL, -- Access token JWT ID
    access_token_issued TIMESTAMPTZ,                                    -- Access token issued time
    access_token_expires TIMESTAMPTZ,                                   -- Access token expiration time
    refresh_token_hash          TEXT     DEFAULT ''::TEXT     NOT NULL, -- Refresh token hash
    previous_refresh_token_hash TEXT     DEFAULT ''::TEXT     NOT NULL, -- Previous (rotated) refresh token hash, used to detect replay
    refresh_token_issued TIMESTAMPTZ,                                   -- Refresh token issued time
    refresh_token_expires TIMESTAMPTZ,                                  -- Refresh token expiration time
    token_claims                JSON     DEFAULT '{}'::JSON   NOT NULL, -- Token claims
    token_metadata              JSON     DEFAULT '{}'::JSON   NOT NULL, -- Token metadata
    revoked_time TIMESTAMPTZ,                                           -- Revoked time
    revoke_reason               TEXT     DEFAULT ''::TEXT     NOT NULL, -- Revoke reason
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,         -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,        -- Operation time
    deleted                     SMALLINT DEFAULT 0 NOT NULL,            -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_oauth_authorization_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_oauth_authorization_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_oauth_authorization_client_principal
    ON dc3_oauth_authorization (client_id, principal_id, tenant_id) WHERE deleted = 0;
CREATE INDEX idx_oauth_authorization_code_hash
    ON dc3_oauth_authorization (authorization_code_hash) WHERE deleted = 0 AND authorization_code_hash <> ''::TEXT;
CREATE INDEX idx_oauth_authorization_access_token_jti
    ON dc3_oauth_authorization (access_token_jti) WHERE deleted = 0 AND access_token_jti <> ''::TEXT;
CREATE INDEX idx_oauth_authorization_refresh_token_hash
    ON dc3_oauth_authorization (refresh_token_hash) WHERE deleted = 0 AND refresh_token_hash <> ''::TEXT;
CREATE INDEX idx_oauth_authorization_previous_refresh_token_hash
    ON dc3_oauth_authorization (previous_refresh_token_hash) WHERE deleted = 0 AND previous_refresh_token_hash <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_oauth_authorization
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_oauth_authorization IS 'OAuth authorization table';
COMMENT
ON COLUMN dc3_oauth_authorization.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_oauth_authorization.registered_client_id IS 'Registered client ID';
COMMENT
ON COLUMN dc3_oauth_authorization.client_id IS 'OAuth client ID';
COMMENT
ON COLUMN dc3_oauth_authorization.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_oauth_authorization.principal_type IS 'Principal type';
COMMENT
ON COLUMN dc3_oauth_authorization.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_oauth_authorization.mcp_connection_id IS 'MCP connection ID';
COMMENT
ON COLUMN dc3_oauth_authorization.authorization_grant_type IS 'Authorization grant type';
COMMENT
ON COLUMN dc3_oauth_authorization.authorized_scopes IS 'Authorized scopes';
COMMENT
ON COLUMN dc3_oauth_authorization.state_hash IS 'OAuth state hash';
COMMENT
ON COLUMN dc3_oauth_authorization.authorization_code_hash IS 'Authorization code hash';
COMMENT
ON COLUMN dc3_oauth_authorization.authorization_code_issued IS 'Authorization code issued time';
COMMENT
ON COLUMN dc3_oauth_authorization.authorization_code_expires IS 'Authorization code expiration time';
COMMENT
ON COLUMN dc3_oauth_authorization.access_token_jti IS 'Access token JWT ID';
COMMENT
ON COLUMN dc3_oauth_authorization.access_token_issued IS 'Access token issued time';
COMMENT
ON COLUMN dc3_oauth_authorization.access_token_expires IS 'Access token expiration time';
COMMENT
ON COLUMN dc3_oauth_authorization.refresh_token_hash IS 'Refresh token hash';
COMMENT
ON COLUMN dc3_oauth_authorization.previous_refresh_token_hash IS 'Previous (rotated) refresh token hash, used to detect replay';
COMMENT
ON COLUMN dc3_oauth_authorization.refresh_token_issued IS 'Refresh token issued time';
COMMENT
ON COLUMN dc3_oauth_authorization.refresh_token_expires IS 'Refresh token expiration time';
COMMENT
ON COLUMN dc3_oauth_authorization.token_claims IS 'Token claims';
COMMENT
ON COLUMN dc3_oauth_authorization.token_metadata IS 'Token metadata';
COMMENT
ON COLUMN dc3_oauth_authorization.revoked_time IS 'Revoked time';
COMMENT
ON COLUMN dc3_oauth_authorization.revoke_reason IS 'Revoke reason';
COMMENT
ON COLUMN dc3_oauth_authorization.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_oauth_authorization.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_oauth_authorization.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_oauth_authorization_consent
-- ----------------------------
CREATE TABLE dc3_oauth_authorization_consent
(
    id                   BIGINT PRIMARY KEY NOT NULL,            -- Primary key ID
    registered_client_id BIGINT   DEFAULT 0 NOT NULL,            -- Registered client ID
    client_id            TEXT     DEFAULT ''::TEXT   NOT NULL,   -- OAuth client ID
    principal_id         BIGINT   DEFAULT 0 NOT NULL,            -- Principal ID
    tenant_id            BIGINT   DEFAULT 0 NOT NULL,            -- Tenant ID
    scopes               TEXT     DEFAULT ''::TEXT   NOT NULL,   -- Consented scopes
    consent_ext          JSON     DEFAULT '{}'::JSON NOT NULL,   -- Consent extension information
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted              SMALLINT DEFAULT 0 NOT NULL,            -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_oauth_consent_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_oauth_consent_client_principal_active_unique
    ON dc3_oauth_authorization_consent (registered_client_id, principal_id, tenant_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_oauth_authorization_consent
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_oauth_authorization_consent IS 'OAuth authorization consent table';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.registered_client_id IS 'Registered client ID';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.client_id IS 'OAuth client ID';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.scopes IS 'Consented scopes';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.consent_ext IS 'Consent extension information';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_oauth_authorization_consent.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_mcp_connection
-- ----------------------------
CREATE TABLE dc3_mcp_connection
(
    id              BIGINT PRIMARY KEY NOT NULL,                            -- Primary key ID
    connection_name TEXT     DEFAULT ''::TEXT                     NOT NULL, -- MCP connection name
    client_id       TEXT     DEFAULT ''::TEXT                     NOT NULL, -- OAuth client ID
    principal_id    BIGINT   DEFAULT 0 NOT NULL,                            -- Principal ID
    principal_type  TEXT     DEFAULT 'USER'::TEXT                 NOT NULL, -- Principal type
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                            -- Tenant ID
    grant_type      TEXT     DEFAULT 'authorization_code'::TEXT   NOT NULL, -- OAuth grant type
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,                            -- Enable flag, 0: enabled, 1: disabled
    expire_time TIMESTAMPTZ,                                                -- Expiration time
    revoke_time TIMESTAMPTZ,                                                -- Revoke time
    last_used_time TIMESTAMPTZ,                                             -- Last used time
    connection_ext  JSON     DEFAULT '{}'::JSON                   NOT NULL, -- Connection extension information
    remark          TEXT     DEFAULT ''::TEXT                     NOT NULL, -- Description
    creator_id      BIGINT   DEFAULT 0 NOT NULL,                            -- Creator ID
    creator_name    TEXT     DEFAULT ''::TEXT                     NOT NULL, -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,             -- Creation time
    operator_id     BIGINT   DEFAULT 0 NOT NULL,                            -- Operator ID
    operator_name   TEXT     DEFAULT ''::TEXT                     NOT NULL, -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,            -- Operation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                            -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_connection_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_connection_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_mcp_connection_grant_type CHECK (grant_type IN ('authorization_code', 'client_credentials')),
    CONSTRAINT chk_mcp_connection_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_mcp_connection_principal_client_name_active_unique
    ON dc3_mcp_connection (tenant_id, principal_id, client_id, connection_name) WHERE deleted = 0 AND connection_name <> ''::TEXT;
CREATE INDEX idx_mcp_connection_client_id ON dc3_mcp_connection (client_id) WHERE deleted = 0;
CREATE INDEX idx_mcp_connection_principal_id ON dc3_mcp_connection (principal_id) WHERE deleted = 0;
CREATE INDEX idx_mcp_connection_tenant_id ON dc3_mcp_connection (tenant_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_mcp_connection
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_mcp_connection IS 'MCP OAuth connection table';
COMMENT
ON COLUMN dc3_mcp_connection.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_mcp_connection.connection_name IS 'MCP connection name';
COMMENT
ON COLUMN dc3_mcp_connection.client_id IS 'OAuth client ID';
COMMENT
ON COLUMN dc3_mcp_connection.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_mcp_connection.principal_type IS 'Principal type';
COMMENT
ON COLUMN dc3_mcp_connection.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_mcp_connection.grant_type IS 'OAuth grant type';
COMMENT
ON COLUMN dc3_mcp_connection.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_mcp_connection.expire_time IS 'Expiration time';
COMMENT
ON COLUMN dc3_mcp_connection.revoke_time IS 'Revoke time';
COMMENT
ON COLUMN dc3_mcp_connection.last_used_time IS 'Last used time';
COMMENT
ON COLUMN dc3_mcp_connection.connection_ext IS 'Connection extension information';
COMMENT
ON COLUMN dc3_mcp_connection.remark IS 'Description';
COMMENT
ON COLUMN dc3_mcp_connection.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_mcp_connection.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_mcp_connection.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_mcp_connection.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_mcp_connection.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_mcp_connection.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_mcp_connection.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_mcp_tool_catalog
-- ----------------------------
CREATE TABLE dc3_mcp_tool_catalog
(
    id               BIGINT PRIMARY KEY NOT NULL,                -- Primary key ID
    tool_id          TEXT     DEFAULT ''::TEXT    NOT NULL,      -- MCP tool ID
    tool_name        TEXT     DEFAULT ''::TEXT    NOT NULL,      -- MCP tool name
    tool_title       TEXT     DEFAULT ''::TEXT    NOT NULL,      -- MCP tool title
    tool_category    TEXT     DEFAULT ''::TEXT    NOT NULL,      -- MCP tool category
    service_name     TEXT     DEFAULT ''::TEXT    NOT NULL,      -- Service name
    api_code         TEXT     DEFAULT ''::TEXT    NOT NULL,      -- API resource code
    permission_code  TEXT     DEFAULT ''::TEXT    NOT NULL,      -- Permission resource code
    http_method      TEXT     DEFAULT ''::TEXT    NOT NULL,      -- HTTP method
    api_path         TEXT     DEFAULT ''::TEXT    NOT NULL,      -- API path
    schema_hash      TEXT     DEFAULT ''::TEXT    NOT NULL,      -- Tool schema hash
    risk_level       TEXT     DEFAULT 'LOW'::TEXT NOT NULL,      -- Risk level, LOW: low, MEDIUM: medium, HIGH: high
    read_only_hint   SMALLINT DEFAULT 0 NOT NULL,                -- Read-only hint, 0: false, 1: true
    destructive_hint SMALLINT DEFAULT 0 NOT NULL,                -- Destructive hint, 0: false, 1: true
    idempotent_hint  SMALLINT DEFAULT 0 NOT NULL,                -- Idempotent hint, 0: false, 1: true
    open_world_hint  SMALLINT DEFAULT 0 NOT NULL,                -- Open-world hint, 0: false, 1: true
    enable_flag      SMALLINT DEFAULT 0 NOT NULL,                -- Enable flag, 0: enabled, 1: disabled
    tool_ext         JSON     DEFAULT '{}'::JSON  NOT NULL,      -- Tool extension information
    remark           TEXT     DEFAULT ''::TEXT    NOT NULL,      -- Description
    creator_id       BIGINT   DEFAULT 0 NOT NULL,                -- Creator ID
    creator_name     TEXT     DEFAULT ''::TEXT    NOT NULL,      -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id      BIGINT   DEFAULT 0 NOT NULL,                -- Operator ID
    operator_name    TEXT     DEFAULT ''::TEXT    NOT NULL,      -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted          SMALLINT DEFAULT 0 NOT NULL,                -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_tool_catalog_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_mcp_tool_catalog_read_only_hint CHECK (read_only_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_destructive_hint CHECK (destructive_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_idempotent_hint CHECK (idempotent_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_open_world_hint CHECK (open_world_hint IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_tool_catalog_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_mcp_tool_catalog_tool_id_active_unique
    ON dc3_mcp_tool_catalog (tool_id) WHERE deleted = 0 AND tool_id <> ''::TEXT;
CREATE INDEX idx_mcp_tool_catalog_permission_code
    ON dc3_mcp_tool_catalog (permission_code) WHERE deleted = 0;
CREATE INDEX idx_mcp_tool_catalog_category
    ON dc3_mcp_tool_catalog (tool_category) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_mcp_tool_catalog
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_mcp_tool_catalog IS 'MCP tool catalog table';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.tool_id IS 'MCP tool ID';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.tool_name IS 'MCP tool name';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.tool_title IS 'MCP tool title';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.tool_category IS 'MCP tool category';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.service_name IS 'Service name';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.api_code IS 'API resource code';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.permission_code IS 'Permission resource code';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.http_method IS 'HTTP method';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.api_path IS 'API path';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.schema_hash IS 'Tool schema hash';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.risk_level IS 'Risk level, LOW: low, MEDIUM: medium, HIGH: high';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.read_only_hint IS 'Read-only hint, 0: false, 1: true';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.destructive_hint IS 'Destructive hint, 0: false, 1: true';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.idempotent_hint IS 'Idempotent hint, 0: false, 1: true';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.open_world_hint IS 'Open-world hint, 0: false, 1: true';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.tool_ext IS 'Tool extension information';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.remark IS 'Description';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_mcp_tool_catalog.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_mcp_connection_tool
-- ----------------------------
CREATE TABLE dc3_mcp_connection_tool
(
    id            BIGINT PRIMARY KEY NOT NULL,                   -- Primary key ID
    connection_id BIGINT   DEFAULT 0 NOT NULL,                   -- MCP connection ID
    tool_id       TEXT     DEFAULT ''::TEXT NOT NULL,            -- MCP tool ID
    enable_flag   SMALLINT DEFAULT 0 NOT NULL,                   -- Enable flag, 0: enabled, 1: disabled
    remark        TEXT     DEFAULT ''::TEXT NOT NULL,            -- Description
    creator_id    BIGINT   DEFAULT 0 NOT NULL,                   -- Creator ID
    creator_name  TEXT     DEFAULT ''::TEXT NOT NULL,            -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id   BIGINT   DEFAULT 0 NOT NULL,                   -- Operator ID
    operator_name TEXT     DEFAULT ''::TEXT NOT NULL,            -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted       SMALLINT DEFAULT 0 NOT NULL,                   -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_connection_tool_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_mcp_connection_tool_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_mcp_connection_tool_conn_tool_active_unique
    ON dc3_mcp_connection_tool (connection_id, tool_id) WHERE deleted = 0 AND tool_id <> ''::TEXT;
CREATE INDEX idx_mcp_connection_tool_connection_id
    ON dc3_mcp_connection_tool (connection_id) WHERE deleted = 0;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_mcp_connection_tool
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_mcp_connection_tool IS 'MCP connection tool whitelist table';
COMMENT
ON COLUMN dc3_mcp_connection_tool.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_mcp_connection_tool.connection_id IS 'MCP connection ID';
COMMENT
ON COLUMN dc3_mcp_connection_tool.tool_id IS 'MCP tool ID';
COMMENT
ON COLUMN dc3_mcp_connection_tool.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_mcp_connection_tool.remark IS 'Description';
COMMENT
ON COLUMN dc3_mcp_connection_tool.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_mcp_connection_tool.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_mcp_connection_tool.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_mcp_connection_tool.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_mcp_connection_tool.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_mcp_connection_tool.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_mcp_connection_tool.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_mcp_audit_log
-- ----------------------------
CREATE TABLE dc3_mcp_audit_log
(
    id              BIGINT PRIMARY KEY NOT NULL,                -- Primary key ID
    trace_id        TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Trace ID
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                -- Tenant ID
    principal_id    BIGINT   DEFAULT 0 NOT NULL,                -- Principal ID
    principal_type  TEXT     DEFAULT 'USER'::TEXT NOT NULL,     -- Principal type
    client_id       TEXT     DEFAULT ''::TEXT     NOT NULL,     -- OAuth client ID
    connection_id   BIGINT   DEFAULT 0 NOT NULL,                -- MCP connection ID
    tool_id         TEXT     DEFAULT ''::TEXT     NOT NULL,     -- MCP tool ID
    tool_name       TEXT     DEFAULT ''::TEXT     NOT NULL,     -- MCP tool name
    permission_code TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Permission resource code
    risk_level      TEXT     DEFAULT 'LOW'::TEXT  NOT NULL,     -- Risk level
    confirm_id      TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Confirmation ID
    idempotency_key TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Idempotency key
    argument_digest TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Argument digest
    status          TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Tool call status
    error_code      TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Error code
    duration_ms     BIGINT   DEFAULT 0 NOT NULL,                -- Duration in milliseconds
    client_name     TEXT     DEFAULT ''::TEXT     NOT NULL,     -- MCP client name
    client_version  TEXT     DEFAULT ''::TEXT     NOT NULL,     -- MCP client version
    remote_ip       TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Remote IP address
    audit_ext       JSON     DEFAULT '{}'::JSON   NOT NULL,     -- Audit extension information
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Creation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_audit_log_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT')),
    CONSTRAINT chk_mcp_audit_log_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_mcp_audit_log_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_mcp_audit_log_trace_id
    ON dc3_mcp_audit_log (trace_id) WHERE deleted = 0 AND trace_id <> ''::TEXT;
CREATE INDEX idx_mcp_audit_log_principal
    ON dc3_mcp_audit_log (tenant_id, principal_id, create_time) WHERE deleted = 0;
CREATE INDEX idx_mcp_audit_log_connection
    ON dc3_mcp_audit_log (connection_id, create_time) WHERE deleted = 0;
CREATE INDEX idx_mcp_audit_log_tool
    ON dc3_mcp_audit_log (tool_id, create_time) WHERE deleted = 0;

COMMENT
ON TABLE dc3_mcp_audit_log IS 'MCP tool call audit log table';
COMMENT
ON COLUMN dc3_mcp_audit_log.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.trace_id IS 'Trace ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.principal_type IS 'Principal type';
COMMENT
ON COLUMN dc3_mcp_audit_log.client_id IS 'OAuth client ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.connection_id IS 'MCP connection ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.tool_id IS 'MCP tool ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.tool_name IS 'MCP tool name';
COMMENT
ON COLUMN dc3_mcp_audit_log.permission_code IS 'Permission resource code';
COMMENT
ON COLUMN dc3_mcp_audit_log.risk_level IS 'Risk level';
COMMENT
ON COLUMN dc3_mcp_audit_log.confirm_id IS 'Confirmation ID';
COMMENT
ON COLUMN dc3_mcp_audit_log.idempotency_key IS 'Idempotency key';
COMMENT
ON COLUMN dc3_mcp_audit_log.argument_digest IS 'Argument digest';
COMMENT
ON COLUMN dc3_mcp_audit_log.status IS 'Tool call status';
COMMENT
ON COLUMN dc3_mcp_audit_log.error_code IS 'Error code';
COMMENT
ON COLUMN dc3_mcp_audit_log.duration_ms IS 'Duration in milliseconds';
COMMENT
ON COLUMN dc3_mcp_audit_log.client_name IS 'MCP client name';
COMMENT
ON COLUMN dc3_mcp_audit_log.client_version IS 'MCP client version';
COMMENT
ON COLUMN dc3_mcp_audit_log.remote_ip IS 'Remote IP address';
COMMENT
ON COLUMN dc3_mcp_audit_log.audit_ext IS 'Audit extension information';
COMMENT
ON COLUMN dc3_mcp_audit_log.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_mcp_audit_log.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_mcp_tool_confirmation
-- ----------------------------
CREATE TABLE dc3_mcp_tool_confirmation
(
    id              BIGINT PRIMARY KEY NOT NULL,                -- Primary key ID
    confirm_id      TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Confirmation ticket ID
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,                -- Tenant ID
    principal_id    BIGINT   DEFAULT 0 NOT NULL,                -- Principal ID
    connection_id   BIGINT   DEFAULT 0 NOT NULL,                -- MCP connection ID
    tool_id         TEXT     DEFAULT ''::TEXT     NOT NULL,     -- MCP tool ID
    argument_digest TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Argument digest bound to the ticket
    idempotency_key TEXT     DEFAULT ''::TEXT     NOT NULL,     -- Idempotency key
    risk_level      TEXT     DEFAULT 'HIGH'::TEXT NOT NULL,     -- Risk level
    status          TEXT     DEFAULT 'PENDING'::TEXT NOT NULL,  -- Ticket status, PENDING/CONSUMED
    expire_time TIMESTAMPTZ,                                    -- Expiration time
    consumed_time TIMESTAMPTZ,                                  -- Consumed time
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Creation time
    deleted         SMALLINT DEFAULT 0 NOT NULL,                -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_mcp_tool_confirmation_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_mcp_tool_confirmation_status CHECK (status IN ('PENDING', 'CONSUMED')),
    CONSTRAINT chk_mcp_tool_confirmation_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_mcp_tool_confirmation_confirm_id
    ON dc3_mcp_tool_confirmation (confirm_id) WHERE deleted = 0 AND confirm_id <> ''::TEXT;
CREATE INDEX idx_mcp_tool_confirmation_idempotency_key
    ON dc3_mcp_tool_confirmation (connection_id, idempotency_key) WHERE deleted = 0 AND idempotency_key <> ''::TEXT;
CREATE INDEX idx_mcp_tool_confirmation_principal
    ON dc3_mcp_tool_confirmation (tenant_id, principal_id, create_time) WHERE deleted = 0;

COMMENT
ON TABLE dc3_mcp_tool_confirmation IS 'MCP high-risk tool call confirmation ticket table';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.confirm_id IS 'Confirmation ticket ID';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.principal_id IS 'Principal ID';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.connection_id IS 'MCP connection ID';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.tool_id IS 'MCP tool ID';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.argument_digest IS 'Argument digest bound to the ticket';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.idempotency_key IS 'Idempotency key';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.risk_level IS 'Risk level';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.status IS 'Ticket status, PENDING/CONSUMED';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.expire_time IS 'Expiration time';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.consumed_time IS 'Consumed time';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_mcp_tool_confirmation.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Table structure for dc3_menu
-- ----------------------------
CREATE TABLE dc3_menu
(
    id             BIGINT PRIMARY KEY NOT NULL,                  -- Primary key ID
    parent_menu_id BIGINT   DEFAULT 0 NOT NULL,                  -- Parent menu ID
    menu_type_flag SMALLINT DEFAULT 0 NOT NULL,                  -- Menu type flag
    menu_name      TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Menu name
    menu_code      TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Menu code
    menu_level     SMALLINT DEFAULT 0 NOT NULL,                  -- Menu level
    menu_index     SMALLINT DEFAULT 0 NOT NULL,                  -- Menu order
    menu_ext       JSON     DEFAULT '{}'::JSON        NOT NULL,  -- Menu extension information
    enable_flag    SMALLINT DEFAULT 0 NOT NULL,                  -- Enable flag, 0: enabled, 1: disabled
    remark         TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Description
    creator_id     BIGINT   DEFAULT 0 NOT NULL,                  -- Creator ID
    creator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Creator name
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Creation time
    operator_id    BIGINT   DEFAULT 0 NOT NULL,                  -- Operator ID
    operator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,  -- Operator name
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Operation time
    deleted        SMALLINT DEFAULT 0 NOT NULL,                  -- Logical delete flag, 0: not deleted, 1: deleted
    CONSTRAINT chk_menu_menu_type_flag CHECK (menu_type_flag BETWEEN 0 AND 1),
    CONSTRAINT chk_menu_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_menu_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_menu_parent_menu_id ON dc3_menu (parent_menu_id) WHERE deleted = 0;
CREATE UNIQUE INDEX idx_menu_code_active_unique ON dc3_menu (menu_code) WHERE deleted = 0 AND menu_code <> ''::TEXT;

CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_operate_time();

COMMENT
ON TABLE dc3_menu IS 'Menu table';
COMMENT
ON COLUMN dc3_menu.id IS 'Primary key ID';
COMMENT
ON COLUMN dc3_menu.parent_menu_id IS 'Parent menu ID';
COMMENT
ON COLUMN dc3_menu.menu_type_flag IS 'Menu type flag';
COMMENT
ON COLUMN dc3_menu.menu_name IS 'Menu name';
COMMENT
ON COLUMN dc3_menu.menu_code IS 'Menu code';
COMMENT
ON COLUMN dc3_menu.menu_level IS 'Menu level';
COMMENT
ON COLUMN dc3_menu.menu_index IS 'Menu order';
COMMENT
ON COLUMN dc3_menu.menu_ext IS 'Menu extension information';
COMMENT
ON COLUMN dc3_menu.enable_flag IS 'Enable flag, 0: enabled, 1: disabled';
COMMENT
ON COLUMN dc3_menu.remark IS 'Description';
COMMENT
ON COLUMN dc3_menu.creator_id IS 'Creator ID';
COMMENT
ON COLUMN dc3_menu.creator_name IS 'Creator name';
COMMENT
ON COLUMN dc3_menu.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_menu.operator_id IS 'Operator ID';
COMMENT
ON COLUMN dc3_menu.operator_name IS 'Operator name';
COMMENT
ON COLUMN dc3_menu.operate_time IS 'Operation time';
COMMENT
ON COLUMN dc3_menu.deleted IS 'Logical delete flag, 0: not deleted, 1: deleted';

-- ----------------------------
-- Records of dc3_tenant
-- ----------------------------
INSERT INTO dc3_tenant
VALUES (1, 'Default Tenant', 'default', '{}', 0, 'Default tenant', 1, 'dc3', '2016-10-01 00:00:00.000000 +00:00', 1,
        'dc3', '2016-10-01 00:00:00.000000 +00:00', 0);

-- ----------------------------
-- Records of dc3_principal
-- ----------------------------
INSERT INTO dc3_principal
VALUES (1, 'USER', 'dc3', 'DC3', 'LOCAL', 0, 0, NULL, '{}', 'Default administrator principal', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 0);

-- ----------------------------
-- Records of dc3_user
-- ----------------------------
INSERT INTO dc3_user
VALUES (1, 1, 'dc3', 'DC3', '18300000000', 'dc3@dc3.com', '{}', '{}', 0, 'Default user', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 0);

-- ----------------------------
-- Records of dc3_local_credential
-- login_name: dc3
-- raw password: dc3dc3dc3
-- stored as bcrypt(raw password), cost factor 12
-- ----------------------------
INSERT INTO dc3_local_credential
VALUES (1, 1, 'dc3', 'dc3', 'PASSWORD', '$2b$12$cSuC2gIZqrti2JLHur5JU.cy9D2kW6KJ5AXTd0nRPJ.cU7gUczhtK', 'BCRYPT', '{}',
        '2016-10-01 00:00:00.000000 +00:00', NULL, 0, NULL, 0, 0, '{}',
        'Default local credential', 1, 'dc3', '2016-10-01 00:00:00.000000 +00:00', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 0);

-- ----------------------------
-- Records of dc3_tenant_membership
-- ----------------------------
INSERT INTO dc3_tenant_membership
VALUES (1, 1, 1, 'USER', 'ACTIVE', '2016-10-01 00:00:00.000000 +00:00', '{}', 'Default tenant administrator membership',
        1, 'dc3', '2016-10-01 00:00:00.000000 +00:00',
        1, 'dc3', '2016-10-01 00:00:00.000000 +00:00', 0);

-- ----------------------------
-- Records of dc3_identity_provider
-- ----------------------------
INSERT INTO dc3_identity_provider
VALUES (1, 0, 'local', 'Local Password', 'OIDC', '', '', '', '', '', '', '', '', '', '', 'sub', '', 'email', '{}',
        'LINK_ONLY', 1, '{}',
        'Placeholder provider for local identity boundary; external login is disabled by default.', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 0);

-- ----------------------------
-- Records of dc3_role
-- Default administrator role for the default tenant.
-- ----------------------------
INSERT INTO dc3_role (id, parent_role_id, role_name, role_code, role_ext, enable_flag, tenant_id, remark, creator_id,
                      creator_name, create_time, operator_id, operator_name, operate_time, deleted)
VALUES (1, 0, 'Administrator', 'admin', '{}', 0, 1, 'Default administrator role', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 0);

-- ----------------------------
-- Records of dc3_role_principal_bind
-- ----------------------------
INSERT INTO dc3_role_principal_bind (id, tenant_id, role_id, principal_id, principal_type, remark, creator_id,
                                     creator_name, create_time, operator_id, operator_name, operate_time, deleted)
VALUES (1, 1, 1, 1, 'USER', 'Default administrator role principal binding', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 1, 'dc3',
        '2016-10-01 00:00:00.000000 +00:00', 0);

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
VALUES 
       (10001, 0, 1, 'Home', 'home', 1, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"首页\",\"en\":\"Home\"},\"icon\":\"HomeFilled\",\"url\":\"/home\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10002, 0, 1, 'Driver', 'driver', 1, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"驱动管理\",\"en\":\"Driver\"},\"icon\":\"Promotion\",\"url\":\"/driver\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10003, 0, 1, 'Profile', 'profile', 1, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"模板管理\",\"en\":\"Profile\"},\"icon\":\"List\",\"url\":\"/profile\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10004, 0, 1, 'Device', 'device', 1, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"设备管理\",\"en\":\"Device\"},\"icon\":\"Management\",\"url\":\"/device\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10005, 0, 1, 'PointValue', 'pointValue', 1, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"位号数据\",\"en\":\"PointValue\"},\"icon\":\"TrendCharts\",\"url\":\"/point_value\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10006, 0, 0, 'Settings', 'settings', 1, 6,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"设置\",\"en\":\"Settings\"},\"icon\":\"Setting\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10050, 10006, 0, 'Identity', 'settingsIdentity', 2, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"身份\",\"en\":\"Identity\"},\"icon\":\"User\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10007, 10050, 1, 'User', 'settingsUser', 2, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"用户管理\",\"en\":\"User\"},\"icon\":\"User\",\"url\":\"/settings/user\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10040, 10050, 1, 'Principals', 'settingsPrincipal', 2, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"主体\",\"en\":\"Principals\"},\"icon\":\"Avatar\",\"url\":\"/settings/principal\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10041, 10050, 1, 'Tenant Membership', 'settingsTenantMembership', 2, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"租户成员\",\"en\":\"Tenant Membership\"},\"icon\":\"OfficeBuilding\",\"url\":\"/settings/tenant_membership\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10042, 10050, 1, 'Local Credentials', 'settingsLocalCredential', 2, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"本地凭证\",\"en\":\"Local Credentials\"},\"icon\":\"Lock\",\"url\":\"/settings/local_credential\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10038, 10050, 1, 'Service Accounts', 'settingsServiceAccount', 2, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"服务账号\",\"en\":\"Service Accounts\"},\"icon\":\"Key\",\"url\":\"/settings/service_account\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10051, 10006, 0, 'Access Control', 'settingsAccess', 2, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"权限\",\"en\":\"Access Control\"},\"icon\":\"Stamp\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10008, 10051, 1, 'Role', 'settingsRole', 2, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"角色管理\",\"en\":\"Role\"},\"icon\":\"Lock\",\"url\":\"/settings/role\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10039, 10051, 1, 'Role Principal Bind', 'settingsRolePrincipalBind', 2, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"角色主体绑定\",\"en\":\"Role Principal Bind\"},\"icon\":\"Link\",\"url\":\"/settings/role_principal_bind\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10009, 10051, 1, 'Resource', 'settingsResource', 2, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"资源管理\",\"en\":\"Resource\"},\"icon\":\"Tickets\",\"url\":\"/settings/resource\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10010, 10051, 1, 'Api', 'settingsApi', 2, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"API 接口\",\"en\":\"Api\"},\"icon\":\"Connection\",\"url\":\"/settings/api\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10011, 10051, 1, 'Menu', 'settingsMenu', 2, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"菜单管理\",\"en\":\"Menu\"},\"icon\":\"Discount\",\"url\":\"/settings/menu\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10013, 10006, 0, 'Model', 'settingsModel', 2, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"模型管理\",\"en\":\"Model Management\"},\"icon\":\"Cpu\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10020, 10013, 1, 'Model Config', 'settingsModelConfig', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"模型配置\",\"en\":\"Model Config\"},\"icon\":\"ChatDotRound\",\"url\":\"/settings/model/config\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10021, 10013, 1, 'Model Providers', 'settingsModelProvider', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"模型供应商\",\"en\":\"Model Providers\"},\"icon\":\"ChatLineSquare\",\"url\":\"/settings/model/provider\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10022, 10006, 0, 'Alarm', 'settingsAlarm', 2, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警\",\"en\":\"Alarm\"},\"icon\":\"AlarmClock\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10023, 10022, 1, 'Alarm Rules', 'settingsAlarmRule', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警规则\",\"en\":\"Alarm Rules\"},\"icon\":\"SetUp\",\"url\":\"/settings/alarm/rule\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10024, 10022, 1, 'Alarm Notify', 'settingsAlarmNotify', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警通知策略\",\"en\":\"Alarm Notify\"},\"icon\":\"Bell\",\"url\":\"/settings/alarm/notify\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10025, 10022, 1, 'Alarm Message', 'settingsAlarmMessage', 3, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警消息模板\",\"en\":\"Alarm Message\"},\"icon\":\"Message\",\"url\":\"/settings/alarm/message\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10026, 10022, 1, 'Alarm Channels', 'settingsAlarmChannel', 3, 4,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警通知渠道\",\"en\":\"Alarm Channels\"},\"icon\":\"Connection\",\"url\":\"/settings/alarm/channel\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10027, 10022, 1, 'Alarm Bindings', 'settingsAlarmBind', 3, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警渠道绑定\",\"en\":\"Alarm Bindings\"},\"icon\":\"Link\",\"url\":\"/settings/alarm/bind\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10015, 10022, 1, 'Overview', 'settingsAlarmOverview', 3, 6,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"概览\",\"en\":\"Overview\"},\"icon\":\"DataAnalysis\",\"url\":\"/settings/alarm/overview\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10028, 10022, 1, 'Alarm States', 'settingsAlarmState', 3, 7,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警运行状态\",\"en\":\"Alarm States\"},\"icon\":\"Monitor\",\"url\":\"/settings/alarm/state\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10029, 10022, 1, 'Alarm History', 'settingsAlarmHistory', 3, 8,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"告警历史\",\"en\":\"Alarm History\"},\"icon\":\"DocumentChecked\",\"url\":\"/settings/alarm/history\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10017, 10022, 1, 'Driver Alarm', 'settingsDriverAlarm', 3, 9,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"驱动告警\",\"en\":\"Driver Alarm\"},\"icon\":\"Promotion\",\"url\":\"/settings/alarm/driver\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10016, 10022, 1, 'Device Alarm', 'settingsDeviceAlarm', 3, 10,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"设备告警\",\"en\":\"Device Alarm\"},\"icon\":\"Management\",\"url\":\"/settings/alarm/device\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10036, 10022, 1, 'Point Alarm', 'settingsPointAlarm', 3, 11,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"点位告警\",\"en\":\"Point Alarm\"},\"icon\":\"TrendCharts\",\"url\":\"/settings/alarm/point\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10052, 10006, 0, 'Event & Command', 'settingsEventCommand', 2, 5,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"事件与指令\",\"en\":\"Event & Command\"},\"icon\":\"Operation\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10035, 10052, 1, 'Event History', 'settingsEventHistory', 3, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"事件历史\",\"en\":\"Event History\"},\"icon\":\"Document\",\"url\":\"/settings/event/history\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10032, 10052, 1, 'Command History', 'settingsCommandHistory', 3, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"指令历史\",\"en\":\"Command History\"},\"icon\":\"Document\",\"url\":\"/settings/command/history\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10053, 10006, 0, 'Audit', 'settingsAudit', 2, 6,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"审计\",\"en\":\"Audit\"},\"icon\":\"Files\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10044, 10053, 1, 'Identity Audit', 'settingsIdentityAudit', 2, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"身份审计\",\"en\":\"Identity Audit\"},\"icon\":\"DocumentChecked\",\"url\":\"/settings/identity_audit\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10043, 10053, 1, 'MCP Audit', 'settingsMcpAudit', 2, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 审计\",\"en\":\"MCP Audit\"},\"icon\":\"Document\",\"url\":\"/settings/mcp_audit\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10054, 10006, 0, 'Integration', 'settingsIntegration', 2, 7,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"集成\",\"en\":\"Integration\"},\"icon\":\"Share\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10037, 10054, 1, 'MCP Service', 'settingsMcpServer', 2, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"MCP 服务\",\"en\":\"MCP Service\"},\"icon\":\"Connection\",\"url\":\"/settings/mcp\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10055, 10006, 0, 'System', 'settingsSystem', 2, 8,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"系统\",\"en\":\"System\"},\"icon\":\"Tools\",\"url\":\"\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10018, 10055, 1, 'Group', 'settingsGroup', 2, 1,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"分组管理\",\"en\":\"Groups\"},\"icon\":\"Grid\",\"url\":\"/settings/group\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10019, 10055, 1, 'Label', 'settingsLabel', 2, 2,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"标签管理\",\"en\":\"Labels\"},\"icon\":\"CollectionTag\",\"url\":\"/settings/label\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10014, 10055, 1, 'About', 'settingsAbout', 2, 3,
        '{"version":1,"content":"{\"titles\":{\"zh\":\"关于\",\"en\":\"About\"},\"icon\":\"InfoFilled\",\"url\":\"/settings/about\"}"}',
        0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0);

-- ----------------------------
-- Records of dc3_resource (MENU-type mirrors of the menu seed)
-- resource_type_flag: 5=MENU    resource_scope_flag: 3=LIST
-- parent_resource_id: 0 for top-level; 20006 (Settings resource) for settings children
-- service_name: blank for seeded menu resources; API resources are service-scoped by runtime registration
-- entity_id: points back to dc3_menu.id so MenuServiceImpl lookups resolve cleanly
-- ----------------------------
INSERT INTO dc3_resource (id, parent_resource_id, resource_name, resource_code, service_name, resource_type_flag,
                          resource_scope_flag, entity_id, resource_ext, enable_flag, remark, creator_id, creator_name,
                          create_time, operator_id, operator_name, operate_time, deleted)
VALUES 
       (20000, 0, 'Administrator Wildcard', '*', '', 6, 3, 0, '{}', 0,
        'Grants all permissions to the default administrator role.', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20001, 0, 'Home', 'menu:home', '', 5, 3, 10001, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20002, 0, 'Driver', 'menu:driver', '', 5, 3, 10002, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20003, 0, 'Profile', 'menu:profile', '', 5, 3, 10003, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20004, 0, 'Device', 'menu:device', '', 5, 3, 10004, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20005, 0, 'PointValue', 'menu:pointValue', '', 5, 3, 10005, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20006, 0, 'Settings', 'menu:settings', '', 5, 3, 10006, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20050, 20006, 'Identity', 'menu:settingsIdentity', '', 5, 3, 10050, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20007, 20050, 'User', 'menu:settingsUser', '', 5, 3, 10007, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20040, 20050, 'Principals', 'menu:settingsPrincipal', '', 5, 3, 10040, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20041, 20050, 'Tenant Membership', 'menu:settingsTenantMembership', '', 5, 3, 10041, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20042, 20050, 'Local Credentials', 'menu:settingsLocalCredential', '', 5, 3, 10042, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20038, 20050, 'Service Accounts', 'menu:settingsServiceAccount', '', 5, 3, 10038, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20051, 20006, 'Access Control', 'menu:settingsAccess', '', 5, 3, 10051, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20008, 20051, 'Role', 'menu:settingsRole', '', 5, 3, 10008, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20039, 20051, 'Role Principal Bind', 'menu:settingsRolePrincipalBind', '', 5, 3, 10039, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20009, 20051, 'Resource', 'menu:settingsResource', '', 5, 3, 10009, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20010, 20051, 'Api', 'menu:settingsApi', '', 5, 3, 10010, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20011, 20051, 'Menu', 'menu:settingsMenu', '', 5, 3, 10011, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20013, 20006, 'Model', 'menu:settingsModel', '', 5, 3, 10013, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20020, 20013, 'Model Config', 'menu:settingsModelConfig', '', 5, 3, 10020, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20021, 20013, 'Model Providers', 'menu:settingsModelProvider', '', 5, 3, 10021, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20022, 20006, 'Alarm', 'menu:settingsAlarm', '', 5, 3, 10022, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20023, 20022, 'Alarm Rules', 'menu:settingsAlarmRule', '', 5, 3, 10023, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20024, 20022, 'Alarm Notify', 'menu:settingsAlarmNotify', '', 5, 3, 10024, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20025, 20022, 'Alarm Message', 'menu:settingsAlarmMessage', '', 5, 3, 10025, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20026, 20022, 'Alarm Channels', 'menu:settingsAlarmChannel', '', 5, 3, 10026, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20027, 20022, 'Alarm Bindings', 'menu:settingsAlarmBind', '', 5, 3, 10027, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20015, 20022, 'Overview', 'menu:settingsAlarmOverview', '', 5, 3, 10015, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20028, 20022, 'Alarm States', 'menu:settingsAlarmState', '', 5, 3, 10028, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20029, 20022, 'Alarm History', 'menu:settingsAlarmHistory', '', 5, 3, 10029, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20017, 20022, 'Driver Alarm', 'menu:settingsDriverAlarm', '', 5, 3, 10017, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20016, 20022, 'Device Alarm', 'menu:settingsDeviceAlarm', '', 5, 3, 10016, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20036, 20022, 'Point Alarm', 'menu:settingsPointAlarm', '', 5, 3, 10036, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20052, 20006, 'Event & Command', 'menu:settingsEventCommand', '', 5, 3, 10052, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20035, 20052, 'Event History', 'menu:settingsEventHistory', '', 5, 3, 10035, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20032, 20052, 'Command History', 'menu:settingsCommandHistory', '', 5, 3, 10032, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20053, 20006, 'Audit', 'menu:settingsAudit', '', 5, 3, 10053, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20044, 20053, 'Identity Audit', 'menu:settingsIdentityAudit', '', 5, 3, 10044, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20043, 20053, 'MCP Audit', 'menu:settingsMcpAudit', '', 5, 3, 10043, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20054, 20006, 'Integration', 'menu:settingsIntegration', '', 5, 3, 10054, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20037, 20054, 'MCP Service', 'menu:settingsMcpServer', '', 5, 3, 10037, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20055, 20006, 'System', 'menu:settingsSystem', '', 5, 3, 10055, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20018, 20055, 'Group', 'menu:settingsGroup', '', 5, 3, 10018, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20019, 20055, 'Label', 'menu:settingsLabel', '', 5, 3, 10019, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (20014, 20055, 'About', 'menu:settingsAbout', '', 5, 3, 10014, '{}', 0, '', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0);

-- ----------------------------
-- Records of dc3_role_resource_bind
-- ----------------------------
INSERT INTO dc3_role_resource_bind (id, role_id, resource_id, remark, creator_id, creator_name, create_time,
                                    operator_id, operator_name, operate_time, deleted)
VALUES (1, 1, 20000, 'Default administrator wildcard permission binding', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 1,
        'dc3',
        '2026-05-01 00:00:00 +00:00', 0);

-- ----------------------------
-- Table structure for dc3_identity_audit_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS dc3_identity_audit_log
(
    id             BIGINT PRIMARY KEY NOT NULL,
    tenant_id      BIGINT   DEFAULT 0 NOT NULL,
    principal_id   BIGINT   DEFAULT 0 NOT NULL,
    principal_type TEXT     DEFAULT 'USER'::TEXT NOT NULL,
    action         TEXT     DEFAULT ''::TEXT     NOT NULL, -- LOGIN/LOGOUT/CREATE/UPDATE/DELETE/ENABLE/DISABLE/GRANT/REVOKE
    resource_type  TEXT     DEFAULT ''::TEXT     NOT NULL, -- user/role/service_account/principal/tenant_membership/local_credential
    resource_id    BIGINT   DEFAULT 0 NOT NULL,
    resource_name  TEXT     DEFAULT ''::TEXT     NOT NULL,
    status         TEXT     DEFAULT ''::TEXT     NOT NULL, -- SUCCESS/FAILURE
    error_code     TEXT     DEFAULT ''::TEXT     NOT NULL,
    detail_ext     JSON     DEFAULT '{}'::JSON   NOT NULL,
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted        SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_identity_audit_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_identity_audit_tenant_time
    ON dc3_identity_audit_log (tenant_id, create_time) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_identity_audit_principal
    ON dc3_identity_audit_log (principal_id, create_time) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_identity_audit_resource
    ON dc3_identity_audit_log (resource_type, resource_id, create_time) WHERE deleted = 0;

