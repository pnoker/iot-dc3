# 统一主体与服务账号设计方案

> 状态: 已实现并落地（PostgreSQL）。本文定义 DC3 面向全球 IoT 平台的 Principal、User、Service
> Account、Tenant Membership 和角色绑定模型。
>
> 实现注记：(1) 本方案"后端模块建议"中的认证上下文类 `PrincipalContext` 实际由 `RequestHeader.PrincipalHeader`（承载
> `principalId`/`principalType`/`tenantId` 等）承担，职责一致、命名不同。(2) `IdentityProvider` / `ExternalIdentity`
> 按方案保持"模型先到位、登录入口先关闭"——已有表/DO/Mapper，暂无 Service/Controller 与外部登录逻辑。(3)
`dc3_local_credential`
> 的 `require_password_change` / `password_expire_time` 已在登录链路生效：命中时拒发 token 并引导
`/token/change_password`
> 自助改密（响应码 `R20303`/`R20304`，有效期配置 `dc3.auth.password.expire-days`）。(4) MySQL seed 未迁移至本模型，
> 当前部署目标为 PostgreSQL。

## 背景

当前 DC3 已有 `dc3_tenant`、`dc3_user`、`dc3_tenant_bind`、`dc3_role`、`dc3_role_user_bind`、`dc3_role_resource_bind`、
`dc3_resource` 等基础 RBAC 能力。该模型适合传统平台用户权限，但不足以承载 OAuth、MCP、自动化服务账号和企业身份联邦。

核心问题是 `user_id` 只能表达"人类用户"，不能继续扩展成"所有调用主体"。MCP/OAuth 建设前必须先把"谁在操作"抽象成统一
Principal，否则会把人类用户、服务账号、OAuth client 和 MCP connection 混在一起，导致审计、授权和租户隔离后续返工。

本方案不支持旧模型，不做运行时双读/双写，不提供旧表到新表的产品化转换链路。最终表结构按本文重建，初始化数据按新模型重新
seed。

## 推荐结论

一次性建设统一主体模型:

- 新增 `dc3_principal`，作为所有可认证、可授权、可审计主体的根表。
- `dc3_user` 继续保存人类用户资料，但通过 `principal_id` 关联 Principal。
- 新增 `dc3_local_credential`，本地用户名密码登录只绑定 Principal，不再把登录凭证混入用户资料。
- 新增 `dc3_service_account`，服务账号是独立主体，不复用 `dc3_user.id`。
- `dc3_tenant_bind` 被 `dc3_tenant_membership` 替代。
- `dc3_role_user_bind` 被 `dc3_role_principal_bind` 替代。
- 新增 `dc3_identity_provider`、`dc3_external_identity`，当前不启用 Google/GitHub/OIDC 登录，但表结构一次到位。
- 新权限入口统一为 `PermissionProvider.listPermissionCodes(tenantId, principalId)`。
- OAuth access token 的 `sub` 固定为 `principal_id`。
- Client Credentials 只能绑定 `SERVICE_ACCOUNT` principal。

重建策略: 以新 IAM schema 为唯一事实源。旧表、旧 token、旧绑定关系不作为新系统输入；如需要保留历史，只做离线归档，不进入新系统运行路径。

## 实施红线

- 不实现旧身份模型兼容层，不做运行时双读、双写、回填或灰度切换。
- 不再以 `dc3_user.id` 作为权限、审计、OAuth subject 或服务账号主体。
- 不创建“服务账号即用户”的假用户数据，服务账号必须是 `SERVICE_ACCOUNT` principal。
- 不复用旧 `dc3_tenant_bind`、`dc3_role_user_bind`、`X-Auth-User`、旧 token 或旧 appKey 作为运行时输入。
- 不做前端 MD5、固定 salt、可逆密码存储或自定义登录 token。
- 不允许一个 access token 同时代表多个租户。

## 概念边界

| 概念                | 含义                            | 表                                                 |
|-------------------|-------------------------------|---------------------------------------------------|
| Principal         | 可被认证、授权和审计的调用主体               | `dc3_principal`                                   |
| User              | 人类用户资料                        | `dc3_user` + `dc3_principal`                      |
| Local Credential  | 本地用户名密码登录凭证                   | `dc3_local_credential` + `dc3_principal`          |
| Service Account   | 机器身份、自动化身份、团队机器人              | `dc3_service_account` + `dc3_principal`           |
| External Identity | Google/GitHub/OIDC/SAML 等外部身份 | `dc3_identity_provider` + `dc3_external_identity` |
| OAuth Client      | 客户端应用或凭证载体                    | `dc3_oauth_registered_client`                     |
| MCP Connection    | 某主体授权给某 MCP client 的工具暴露边界    | `dc3_mcp_connection`                              |
| Tenant Membership | 主体属于哪个租户                      | `dc3_tenant_membership`                           |
| Role Binding      | 主体拥有哪些角色                      | `dc3_role_principal_bind`                         |

OAuth client 不是业务主体。业务审计中的"谁"必须是 `principal_id`，不是 `client_id`。

## 目标模型

```
dc3_tenant
  └── dc3_tenant_membership ──N:1── dc3_principal
                                      ├── USER ─────────────── dc3_user
                                      │                         └── dc3_local_credential
                                      ├── SERVICE_ACCOUNT ──── dc3_service_account
                                      └── SYSTEM

dc3_role ──1:N── dc3_role_principal_bind ──N:1── dc3_principal
dc3_role ──1:N── dc3_role_resource_bind  ──N:1── dc3_resource

dc3_identity_provider ──1:N── dc3_external_identity ──N:1── dc3_principal
dc3_group ──1:N── dc3_group_member ──N:1── dc3_principal
dc3_role ──1:N── dc3_role_group_bind ──N:1── dc3_group

dc3_oauth_registered_client ──optional── dc3_principal (client_credentials 服务账号)
dc3_mcp_connection ─────────────────────N:1── dc3_principal
```

首版强依赖。以下表和领域对象必须落地；Google/GitHub/OIDC 登录入口可以先关闭，但不能缺少承载模型:

- `dc3_principal`
- `dc3_local_credential`
- `dc3_tenant_membership`
- `dc3_service_account`
- `dc3_role_principal_bind`
- `dc3_user.principal_id`
- `dc3_identity_provider`
- `dc3_external_identity`
- `PermissionProvider.listPermissionCodes(tenantId, principalId)`

后续增强但不改变核心模型:

- OIDC / SAML 企业身份源
- SCIM 用户和组同步
- Group role binding
- 更细粒度 ABAC / policy engine

## 数据库模型

身份表放在 `dc3_auth` schema，与用户、角色、资源同属权限域。

### dc3_principal

```sql
SET search_path TO dc3_auth;

CREATE TABLE dc3_principal
(
    id              BIGINT PRIMARY KEY NOT NULL,
    principal_type  TEXT     DEFAULT 'USER'::TEXT      NOT NULL,    -- USER / SERVICE_ACCOUNT / SYSTEM
    principal_name  TEXT     DEFAULT ''::TEXT          NOT NULL,    -- 主体唯一名称
    display_name    TEXT     DEFAULT ''::TEXT          NOT NULL,
    source_type     TEXT     DEFAULT 'LOCAL'::TEXT     NOT NULL,    -- LOCAL / EXTERNAL / SYSTEM
    enable_flag     SMALLINT DEFAULT 0 NOT NULL,
    locked_flag     SMALLINT DEFAULT 0 NOT NULL,
    last_login_time TIMESTAMPTZ,
    principal_ext   JSON     DEFAULT '{}'::JSON        NOT NULL,
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id      BIGINT   DEFAULT 0 NOT NULL,
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id     BIGINT   DEFAULT 0 NOT NULL,
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted         SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_principal_source_type CHECK (source_type IN ('LOCAL', 'EXTERNAL', 'SYSTEM')),
    CONSTRAINT chk_principal_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_principal_locked_flag CHECK (locked_flag IN (0, 1)),
    CONSTRAINT chk_principal_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_principal_name_type_active_unique
    ON dc3_principal (principal_type, principal_name)
    WHERE deleted = 0 AND principal_name <> ''::TEXT;

CREATE INDEX idx_principal_type
    ON dc3_principal (principal_type) WHERE deleted = 0;
```

### dc3_user

```sql
CREATE TABLE dc3_user
(
    id             BIGINT PRIMARY KEY NOT NULL,
    principal_id   BIGINT   DEFAULT 0 NOT NULL,              -- 对应 dc3_principal.id
    user_name      TEXT     DEFAULT ''::TEXT          NOT NULL,
    nick_name      TEXT     DEFAULT ''::TEXT          NOT NULL,
    phone          TEXT     DEFAULT ''::TEXT          NOT NULL,
    email          TEXT     DEFAULT ''::TEXT          NOT NULL,
    social_ext     JSON     DEFAULT '{}'::JSON        NOT NULL,
    identity_ext   JSON     DEFAULT '{}'::JSON        NOT NULL,
    enable_flag    SMALLINT DEFAULT 0 NOT NULL,
    remark         TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id     BIGINT   DEFAULT 0 NOT NULL,
    creator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id    BIGINT   DEFAULT 0 NOT NULL,
    operator_name  TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted        SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_user_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_user_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_user_principal_active_unique
    ON dc3_user (principal_id) WHERE deleted = 0 AND principal_id <> 0;

CREATE UNIQUE INDEX idx_user_name_active_unique
    ON dc3_user (user_name) WHERE deleted = 0 AND user_name <> ''::TEXT;
```

`dc3_user` 只保存人类用户资料，如用户名、昵称、手机号、邮箱和扩展资料。授权、审计、OAuth subject 不再直接使用 `dc3_user.id`。

### dc3_local_credential

```sql
CREATE TABLE dc3_local_credential
(
    id                       BIGINT PRIMARY KEY NOT NULL,
    principal_id             BIGINT   DEFAULT 0 NOT NULL,
    login_name               TEXT     DEFAULT ''::TEXT          NOT NULL,
    login_name_normalized    TEXT     DEFAULT ''::TEXT          NOT NULL,
    credential_type          TEXT     DEFAULT 'PASSWORD'::TEXT  NOT NULL,    -- PASSWORD
    password_hash            TEXT     DEFAULT ''::TEXT          NOT NULL,
    password_algorithm       TEXT     DEFAULT 'ARGON2ID'::TEXT  NOT NULL,    -- ARGON2ID / BCRYPT
    password_params          JSON     DEFAULT '{}'::JSON        NOT NULL,
    password_updated_time    TIMESTAMPTZ,
    password_expire_time     TIMESTAMPTZ,
    failed_attempts          INTEGER  DEFAULT 0 NOT NULL,
    locked_until             TIMESTAMPTZ,
    require_password_change  SMALLINT DEFAULT 1 NOT NULL,
    enable_flag              SMALLINT DEFAULT 0 NOT NULL,
    credential_ext           JSON     DEFAULT '{}'::JSON        NOT NULL,
    remark                   TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id               BIGINT   DEFAULT 0 NOT NULL,
    creator_name             TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time              TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id              BIGINT   DEFAULT 0 NOT NULL,
    operator_name            TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time             TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted                  SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_local_credential_type CHECK (credential_type IN ('PASSWORD')),
    CONSTRAINT chk_local_credential_password_algorithm CHECK (password_algorithm IN ('ARGON2ID', 'BCRYPT')),
    CONSTRAINT chk_local_credential_require_change CHECK (require_password_change IN (0, 1)),
    CONSTRAINT chk_local_credential_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_local_credential_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_local_credential_login_active_unique
    ON dc3_local_credential (credential_type, login_name_normalized)
    WHERE deleted = 0 AND login_name_normalized <> ''::TEXT;

CREATE UNIQUE INDEX idx_local_credential_principal_active_unique
    ON dc3_local_credential (principal_id, credential_type)
    WHERE deleted = 0 AND principal_id <> 0;
```

本地登录凭证是全局身份入口，不绑定单个租户。用户登录成功后先解析到唯一 `principal_id`，再从 `dc3_tenant_membership`
中选择当前租户。这样同一个人类用户可以加入多个租户，但任意一次 OAuth access token 只代表一个 `tenant_id`。

密码安全要求:

- 不做前端 MD5、固定 salt 或自定义 token 派生；浏览器只通过 HTTPS 提交密码。
- 服务端使用 Argon2id，无法落地时使用 bcrypt；参数写入 `password_params`，便于后续提升成本参数。
- 数据库只保存不可逆 `password_hash`，不保存明文、可逆密文、旧 salt 或旧 token。
- 登录失败计数、锁定、重置密码、强制改密和登录审计必须随首版落地。
- 后续接入 Google/GitHub/OIDC 时，外部身份只增加登录方式，不改变 `principal_id`、租户成员关系和 RBAC。

### dc3_tenant_membership

```sql
CREATE TABLE dc3_tenant_membership
(
    id                BIGINT PRIMARY KEY NOT NULL,
    tenant_id         BIGINT   DEFAULT 0 NOT NULL,
    principal_id      BIGINT   DEFAULT 0 NOT NULL,
    principal_type    TEXT     DEFAULT 'USER'::TEXT      NOT NULL,
    membership_status TEXT     DEFAULT 'ACTIVE'::TEXT    NOT NULL,  -- ACTIVE / SUSPENDED / INVITED
    joined_time       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    membership_ext    JSON     DEFAULT '{}'::JSON        NOT NULL,
    remark            TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id        BIGINT   DEFAULT 0 NOT NULL,
    creator_name      TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id       BIGINT   DEFAULT 0 NOT NULL,
    operator_name     TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted           SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_tenant_membership_principal_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_tenant_membership_status CHECK (membership_status IN ('ACTIVE', 'SUSPENDED', 'INVITED')),
    CONSTRAINT chk_tenant_membership_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_tenant_membership_active_unique
    ON dc3_tenant_membership (tenant_id, principal_id) WHERE deleted = 0;

CREATE INDEX idx_tenant_membership_principal
    ON dc3_tenant_membership (principal_id) WHERE deleted = 0;
```

多租户规则:

1. 同一个 `USER` principal 可以在多个租户下存在多条 `dc3_tenant_membership`。
2. `SERVICE_ACCOUNT` 默认只属于一个租户；如确需跨租户自动化，必须创建多个服务账号，避免审计归因混乱。
3. 登录态可以看到主体可访问的租户列表，但授权码和 access token 必须绑定单个 `tenant_id`。
4. 跨租户切换必须重新确认租户上下文，不能在一个 token 中携带多个租户。

### dc3_service_account

```sql
CREATE TABLE dc3_service_account
(
    id                     BIGINT PRIMARY KEY NOT NULL,
    principal_id           BIGINT   DEFAULT 0 NOT NULL,
    tenant_id              BIGINT   DEFAULT 0 NOT NULL,
    service_account_name   TEXT     DEFAULT ''::TEXT          NOT NULL,
    owner_principal_id     BIGINT   DEFAULT 0 NOT NULL,              -- 负责人, 必须是 USER principal
    purpose                TEXT     DEFAULT ''::TEXT          NOT NULL,
    expire_time            TIMESTAMPTZ,
    last_used_time         TIMESTAMPTZ,
    credential_policy_ext  JSON     DEFAULT '{}'::JSON        NOT NULL,
    enable_flag            SMALLINT DEFAULT 0 NOT NULL,
    remark                 TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id             BIGINT   DEFAULT 0 NOT NULL,
    creator_name           TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id            BIGINT   DEFAULT 0 NOT NULL,
    operator_name          TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time           TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted                SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_service_account_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_service_account_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_service_account_principal_active_unique
    ON dc3_service_account (principal_id) WHERE deleted = 0;

CREATE UNIQUE INDEX idx_service_account_tenant_name_active_unique
    ON dc3_service_account (tenant_id, service_account_name)
    WHERE deleted = 0 AND service_account_name <> ''::TEXT;
```

服务账号安全要求:

- 必须属于一个租户。
- 必须有负责人 `owner_principal_id`。
- 必须有用途 `purpose`。
- 必须支持过期、禁用、撤销和最近使用时间。
- 默认最小权限，不允许默认绑定管理员角色。
- Client Credentials client 必须绑定服务账号主体。
- 高安全场景支持 `private_key_jwt` 或 mTLS，不推荐长期 appKey。

### dc3_role_principal_bind

```sql
CREATE TABLE dc3_role_principal_bind
(
    id              BIGINT PRIMARY KEY NOT NULL,
    tenant_id       BIGINT   DEFAULT 0 NOT NULL,
    role_id         BIGINT   DEFAULT 0 NOT NULL,
    principal_id    BIGINT   DEFAULT 0 NOT NULL,
    principal_type  TEXT     DEFAULT 'USER'::TEXT      NOT NULL,
    remark          TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id      BIGINT   DEFAULT 0 NOT NULL,
    creator_name    TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id     BIGINT   DEFAULT 0 NOT NULL,
    operator_name   TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted         SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_role_principal_bind_type CHECK (principal_type IN ('USER', 'SERVICE_ACCOUNT', 'SYSTEM')),
    CONSTRAINT chk_role_principal_bind_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_role_principal_bind_active_unique
    ON dc3_role_principal_bind (tenant_id, role_id, principal_id) WHERE deleted = 0;

CREATE INDEX idx_role_principal_bind_principal
    ON dc3_role_principal_bind (tenant_id, principal_id) WHERE deleted = 0;
```

## 身份联邦与登录扩展表

以下表不要求首版开放 Google/GitHub/OIDC 登录入口，但 DDL 和领域模型必须现在建好。这样后续新增登录方式是打开能力，不是重构身份模型。

### dc3_identity_provider

```sql
CREATE TABLE dc3_identity_provider
(
    id                    BIGINT PRIMARY KEY NOT NULL,
    tenant_id             BIGINT   DEFAULT 0 NOT NULL,                    -- 0 表示平台级 provider
    provider_code         TEXT     DEFAULT ''::TEXT          NOT NULL,
    provider_name         TEXT     DEFAULT ''::TEXT          NOT NULL,
    provider_type         TEXT     DEFAULT 'OIDC'::TEXT      NOT NULL,    -- GOOGLE / GITHUB / OIDC / SAML
    issuer                TEXT     DEFAULT ''::TEXT          NOT NULL,
    discovery_url         TEXT     DEFAULT ''::TEXT          NOT NULL,
    authorization_uri     TEXT     DEFAULT ''::TEXT          NOT NULL,
    token_uri             TEXT     DEFAULT ''::TEXT          NOT NULL,
    user_info_uri         TEXT     DEFAULT ''::TEXT          NOT NULL,
    jwks_uri              TEXT     DEFAULT ''::TEXT          NOT NULL,
    client_id             TEXT     DEFAULT ''::TEXT          NOT NULL,
    client_secret_ref     TEXT     DEFAULT ''::TEXT          NOT NULL,    -- Secret Manager / KMS 加密引用
    scopes                TEXT     DEFAULT ''::TEXT          NOT NULL,
    redirect_uri          TEXT     DEFAULT ''::TEXT          NOT NULL,
    subject_claim         TEXT     DEFAULT 'sub'::TEXT       NOT NULL,
    username_claim        TEXT     DEFAULT ''::TEXT          NOT NULL,
    email_claim           TEXT     DEFAULT 'email'::TEXT     NOT NULL,
    attribute_mapping     JSON     DEFAULT '{}'::JSON        NOT NULL,
    provisioning_mode     TEXT     DEFAULT 'LINK_ONLY'::TEXT NOT NULL,    -- LINK_ONLY / JIT
    enable_flag           SMALLINT DEFAULT 0 NOT NULL,
    provider_ext          JSON     DEFAULT '{}'::JSON        NOT NULL,
    remark                TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id            BIGINT   DEFAULT 0 NOT NULL,
    creator_name          TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time           TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id           BIGINT   DEFAULT 0 NOT NULL,
    operator_name         TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted               SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_identity_provider_type CHECK (provider_type IN ('GOOGLE', 'GITHUB', 'OIDC', 'SAML')),
    CONSTRAINT chk_identity_provider_provisioning CHECK (provisioning_mode IN ('LINK_ONLY', 'JIT')),
    CONSTRAINT chk_identity_provider_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_identity_provider_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_identity_provider_code_active_unique
    ON dc3_identity_provider (tenant_id, provider_code)
    WHERE deleted = 0 AND provider_code <> ''::TEXT;
```

`client_secret_ref` 不能保存明文密钥。没有独立 Secret Manager 时，至少使用平台 KMS/主密钥加密后保存引用或密文，并支持轮换。

### dc3_external_identity

```sql
CREATE TABLE dc3_external_identity
(
    id                   BIGINT PRIMARY KEY NOT NULL,
    provider_id          BIGINT   DEFAULT 0 NOT NULL,
    principal_id         BIGINT   DEFAULT 0 NOT NULL,
    external_subject     TEXT     DEFAULT ''::TEXT          NOT NULL,
    external_username    TEXT     DEFAULT ''::TEXT          NOT NULL,
    external_email       TEXT     DEFAULT ''::TEXT          NOT NULL,
    email_verified       SMALLINT DEFAULT 0 NOT NULL,                    -- 0 未验证, 1 已验证
    first_login_time     TIMESTAMPTZ,
    last_login_time      TIMESTAMPTZ,
    last_claims_digest   TEXT     DEFAULT ''::TEXT          NOT NULL,
    identity_ext         JSON     DEFAULT '{}'::JSON        NOT NULL,
    enable_flag          SMALLINT DEFAULT 0 NOT NULL,
    remark               TEXT     DEFAULT ''::TEXT          NOT NULL,
    creator_id           BIGINT   DEFAULT 0 NOT NULL,
    creator_name         TEXT     DEFAULT ''::TEXT          NOT NULL,
    create_time          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operator_id          BIGINT   DEFAULT 0 NOT NULL,
    operator_name        TEXT     DEFAULT ''::TEXT          NOT NULL,
    operate_time         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted              SMALLINT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_external_identity_email_verified CHECK (email_verified IN (0, 1)),
    CONSTRAINT chk_external_identity_enable_flag CHECK (enable_flag IN (0, 1)),
    CONSTRAINT chk_external_identity_deleted CHECK (deleted IN (0, 1))
);

CREATE UNIQUE INDEX idx_external_identity_subject_active_unique
    ON dc3_external_identity (provider_id, external_subject)
    WHERE deleted = 0 AND external_subject <> ''::TEXT;

CREATE UNIQUE INDEX idx_external_identity_principal_provider_active_unique
    ON dc3_external_identity (provider_id, principal_id)
    WHERE deleted = 0 AND principal_id <> 0;
```

后续增强表:

| 表                     | 作用                   |
|-----------------------|----------------------|
| `dc3_group`           | 租户内用户组/服务组           |
| `dc3_group_member`    | 组成员，成员仍然是 Principal  |
| `dc3_role_group_bind` | 角色绑定到组               |
| `dc3_scim_sync_job`   | SCIM 同步任务、游标、结果和错误摘要 |

### Google/GitHub/OIDC 后续接入

当前阶段不实现 Google、GitHub 或企业 OIDC 登录入口，但数据模型必须现在就支持:

```text
Google / GitHub / Enterprise IdP
  → dc3_identity_provider
  → dc3_external_identity(provider_id, external_subject)
  → dc3_principal
  → dc3_tenant_membership
  → dc3_role_principal_bind
```

关键规则:

1. 第三方账号不是 DC3 主体，`external_subject` 只作为外部身份标识。
2. 一个 `dc3_principal` 可以绑定多个外部身份，例如本地密码 + Google + GitHub。
3. 登录成功后必须解析到唯一 `principal_id`，再选择或确认 `tenant_id`。
4. OAuth access token 仍然只放当前 `tenant_id`，不允许一个 token 同时代表多个租户。
5. 外部身份登录不改变 RBAC，权限仍来自 `dc3_role_principal_bind`。

## 前端配套建设

身份底座必须有前端配套，否则服务账号、租户成员和角色绑定无法被安全运营。

### 当前必做页面

| 页面           | 作用                                                     |
|--------------|--------------------------------------------------------|
| Principal 列表 | 查看 USER / SERVICE_ACCOUNT / SYSTEM 主体、状态、最近使用时间        |
| 用户管理         | 创建本地用户、绑定 Principal、启用/禁用、重置本地凭证                       |
| 本地凭证管理       | 设置登录名、重置密码、强制改密、锁定/解锁、查看失败登录状态                         |
| 租户成员管理       | 将 Principal 加入或移出租户，管理 ACTIVE / SUSPENDED / INVITED 状态 |
| 服务账号管理       | 创建服务账号、设置负责人、用途、过期时间、启停状态、查看最近使用时间                     |
| 角色主体绑定       | 给 USER 或 SERVICE_ACCOUNT 绑定租户内角色                       |
| 审计视图         | 按 principal_id、principal_type、tenant_id 查询身份和授权变更      |

服务账号页面是 MCP Client Credentials 的前置页面。MCP 页面只能选择已有服务账号或跳转创建，不能在 MCP 表单里临时生成一个弱治理的机器身份。

### 后续预留页面

| 页面        | 作用                                     |
|-----------|----------------------------------------|
| 身份源管理     | 配置 Google、GitHub、企业 OIDC/SAML provider |
| 外部身份绑定    | 查看和解除 external subject 与 Principal 的绑定 |
| 登录方式管理    | 用户绑定/解绑本地密码、Google、GitHub、企业 IdP       |
| SCIM 同步任务 | 查看同步状态、错误摘要和最近同步时间                     |
| 组管理       | 管理 group、group member、role group bind  |

前端路由和菜单可以先预留身份源与外部身份入口，但首版默认隐藏或仅管理员可见。

## 权限链路

标准权限链路:

```
@PreAuthorize("@perm.can('device','add')")
  → ApiEndpointScanner 扫描 → dc3_api {api_name="device:add"}
  → ResourceRegistrySyncServiceImpl → dc3_resource {resource_code="dc3-center-manager:device:add"}
  → dc3_role_resource_bind (角色绑定资源)
  → dc3_role_principal_bind (主体绑定角色)
  → PermissionProvider.listPermissionCodes(tenantId, principalId)
  → Set<resource_code>
```

聚合规则:

1. `dc3_role_principal_bind WHERE tenant_id = ? AND principal_id = ?` 得到所有 `role_id`。
2. 过滤启用状态和租户范围。
3. `dc3_role_resource_bind WHERE role_id IN (...)` 得到所有 `resource_id`。
4. `dc3_resource WHERE id IN (...)` 得到所有 `resource_code`。
5. 多角色权限取并集。
6. 没有 deny 机制，`*` 满足所有权限检查。

## OAuth 和 MCP 对接

OAuth access token 中的身份 claims 固定为:

```json
{
  "iss": "https://auth.dc3.example.com",
  "sub": "100000000000000001",
  "principal_type": "USER",
  "tenant_id": "200000000000000001",
  "client_id": "claude-code",
  "mcp_connection_id": "300000000000000001",
  "scope": "mcp:tools:list mcp:tools:call",
  "aud": "dc3:mcp"
}
```

服务账号场景:

```json
{
  "iss": "https://auth.dc3.example.com",
  "sub": "100000000000000099",
  "principal_type": "SERVICE_ACCOUNT",
  "tenant_id": "200000000000000001",
  "client_id": "dc3-automation",
  "scope": "mcp:tools:call",
  "aud": "dc3:mcp"
}
```

内部转发身份头统一为:

```http
X-Auth-Principal: {"principalId":100,"principalType":"USER","tenantId":1,"clientId":"claude-code","connectionId":300}
X-Auth-Sign: <HMAC-SHA256>
```

## 重建策略

本次身份体系按新模型全量重建，不支持旧逻辑:

1. 建立全新的 `dc3_principal`、`dc3_user`、`dc3_tenant_membership`、`dc3_service_account`、`dc3_role_principal_bind` 等表。
2. 建立 `dc3_local_credential`、`dc3_identity_provider`、`dc3_external_identity`，本地密码和外部身份都映射到
   `principal_id`。
3. 删除产品代码中对 `dc3_tenant_bind`、`dc3_role_user_bind`、旧 token 认证链路的依赖。
4. 使用新 seed 数据创建默认租户、默认管理员 Principal、默认管理员用户、默认本地凭证、默认角色和资源绑定。
5. 使用新模型重新创建服务账号和 OAuth client，不从旧 appKey、driver token 或自定义 token 派生。
6. 将 `PermissionProvider`、认证上下文、审计上下文统一改为 `principal_id`。
7. 所有新接口只读写新表；旧表如果保留，只能作为离线归档，不允许进入运行时查询。

重建校验至少包括:

- 默认租户、默认管理员和默认角色能完成登录、授权和资源访问。
- 每个启用用户都有 `dc3_user.principal_id`，且对应 `dc3_principal.principal_type=USER`。
- 每个可本地登录的用户都有唯一启用的 `dc3_local_credential`，登录成功后解析到唯一 `principal_id`。
- 每个服务账号都有 `dc3_service_account.principal_id`，且对应 `dc3_principal.principal_type=SERVICE_ACCOUNT`。
- 每个可登录或可调用的 Principal 都有有效 `dc3_tenant_membership`。
- `listPermissionCodes(tenantId, principalId)` 只读取 `dc3_role_principal_bind`。
- 代码仓库中不再存在对 `dc3_role_user_bind`、`dc3_tenant_bind`、`X-Auth-User` 的运行时依赖。

## 后端模块建议

```text
dc3-common-auth/
└── principal/
    ├── entity/
    │   ├── PrincipalDO.java
    │   ├── LocalCredentialDO.java
    │   ├── TenantMembershipDO.java
    │   ├── ServiceAccountDO.java
    │   ├── RolePrincipalBindDO.java
    │   ├── IdentityProviderDO.java
    │   └── ExternalIdentityDO.java
    ├── service/
    │   ├── PrincipalService.java
    │   ├── LocalCredentialService.java
    │   ├── TenantMembershipService.java
    │   ├── ServiceAccountService.java
    │   ├── RolePrincipalBindService.java
    │   ├── IdentityProviderService.java
    │   └── ExternalIdentityService.java
    └── security/
        ├── PrincipalContext.java
        └── PrincipalPermissionProvider.java
```

## 实施步骤

| 步骤 | 任务                                                                                                                        | 产出                                                         |
|----|---------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| 1  | 重建 Identity 标准表 DDL                                                                                                       | rebuild SQL                                                |
| 2  | 新增 Principal / LocalCredential / Membership / ServiceAccount / RolePrincipalBind / IdentityProvider / ExternalIdentity 实体 | DO/BO/VO/Mapper                                            |
| 3  | 编写新模型初始化 seed                                                                                                             | 默认租户、管理员、管理员本地凭证、角色、资源绑定                                   |
| 4  | 改造 `PermissionProvider`                                                                                                   | `listPermissionCodes(tenantId, principalId)`               |
| 5  | 改造认证上下文                                                                                                                   | `PrincipalContext` / `X-Auth-Principal`                    |
| 6  | 新增服务账号管理 API                                                                                                              | 创建、禁用、过期、轮换、审计                                             |
| 7  | 新增身份前端页面                                                                                                                  | Principal、用户、本地凭证、租户成员、服务账号、角色主体绑定                         |
| 8  | 删除旧身份链路运行时依赖                                                                                                              | 无 `dc3_role_user_bind` / `dc3_tenant_bind` / `X-Auth-User` |
| 9  | 接入 OAuth / MCP                                                                                                            | access token `sub=principal_id`                            |

## 相关资料

- [OAuth 2.0 Security Best Current Practice (RFC 9700)](https://datatracker.ietf.org/doc/html/rfc9700)
- [OAuth 2.0 Resource Indicators (RFC 8707)](https://datatracker.ietf.org/doc/html/rfc8707)
- [SCIM Core Schema (RFC 7643)](https://datatracker.ietf.org/doc/html/rfc7643)
- [SCIM Protocol (RFC 7644)](https://datatracker.ietf.org/doc/html/rfc7644)
- [NIST Digital Identity Guidelines SP 800-63-4](https://csrc.nist.gov/pubs/sp/800/63/4/final)
