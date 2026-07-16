---
title: 租户 Tenant
---

<script setup>
import TenantRelationDiagram from '../../../.vitepress/theme/components/TenantRelationDiagram.vue'
import TenantAuthDiagram from '../../../.vitepress/theme/components/TenantAuthDiagram.vue'
</script>

# 租户 Tenant

> **租户是平台里业务数据的隔离边界**——同一套部署里，A 公司的[设备](./device)、位号、数据和 B 公司的彼此看不见。每一条业务记录都带一个
`tenantId`，平台据它把数据切成互不串台的几份。

租户回答的是"这条数据归谁、谁能看见它"。它不是一个功能、也不是一个角色，而是一道**数据围墙**：你登录后拿到的令牌绑定了一个
`tenantId`，之后你创建的设备、采到的位号值、下发的指令，全都自动打上这个标签；按 ID 或批量访问别家租户的记录会被判为不存在、或被剔除。

容易混淆的是租户和[主体](../../architecture/auth-rbac)（principal）、角色。一句话区分：**租户管"能碰哪条数据"，角色管"
能做哪类操作"，主体是"谁在操作"**。三者正交——你可能有 `device:get` 权限（角色给的），但去 get
别家租户的设备依然失败（租户拦的）。就像一栋写字楼：门禁卡决定你能进哪一层（租户），职级决定你在自己那层能开哪些会议室（角色），工牌上印的是你本人（主体）。

## 关键字段

租户 `TenantBO`（表 `dc3_tenant`，继承 `BaseBO` 的 `id` / `remark` / 审计字段）：

| 字段           | 类型              | 含义                                         |
|--------------|-----------------|--------------------------------------------|
| `tenantName` | String          | 租户名称（展示用）                                  |
| `tenantCode` | String          | 租户唯一编码，登录时用它定位租户；编码为 `default` 的租户是系统管理员租户 |
| `tenantExt`  | TenantExt(JSON) | 扩展配置，预留字段                                  |
| `enableFlag` | EnableFlagEnum  | 启用标志，见下                                    |

租户不是孤立的：身份"属于哪个租户"由租户成员关系 `TenantMembershipBO`（表 `dc3_tenant_membership`）一行一行声明，唯一索引建在
`(tenant_id, principal_id)`：

| 字段                 | 类型                   | 含义                                               |
|--------------------|----------------------|--------------------------------------------------|
| `tenantId`         | Long                 | 归属的租户                                            |
| `principalId`      | Long                 | 归属的[主体](../../architecture/auth-rbac)（principal） |
| `principalType`    | PrincipalTypeEnum    | 主体类型：`USER` / `SERVICE_ACCOUNT` / `SYSTEM`       |
| `membershipStatus` | MembershipStatusEnum | 成员状态：`ACTIVE` / `SUSPENDED` / `INVITED`          |
| `joinedTime`       | LocalDateTime        | 加入时间                                             |

::: tip 一个人可以属于多个租户
因为唯一索引在 `(tenant_id, principal_id)`，同一个 `USER` 主体可以在多个租户下各有一行成员关系（多租户成员）。登录时由
`name + tenant` 一起定位是哪一段成员关系。按设计 `SERVICE_ACCOUNT` 服务账号只属于一个租户。
:::

## 启用标志 `enableFlag`

| 值 `EnableFlagEnum` | 数据库 | 说明 |
|--------------------|-----|----|
| `ENABLE`           | `0` | 启用 |
| `DISABLE`          | `1` | 禁用 |

## 与其它概念的关系

<TenantRelationDiagram lang="zh" />

- 一切实现了 `TenantOwned`（提供 `getTenantId()`）的业务实体都归某个租户拥有，是隔离的施加对象。
- 主体经 `dc3_tenant_membership` 加入租户；进入租户后再由 RBAC（`dc3_role_principal_bind`
  ）决定能做哪些操作。详见 [鉴权 · 租户 · RBAC](../../architecture/auth-rbac)。

## 隔离是怎么落实的

租户隔离落在控制器层：取数后比对实体 `tenantId` 与调用方租户，跨租户访问被判为不存在或被剔除。

<TenantAuthDiagram lang="zh" />

- **控制器层（单条按 ID）**：查到实体后，`BaseController.requireTenant()` 比对实体的 `tenantId` 与调用方租户，不一致（或实体不存在）就抛
  `NotFoundException`，对外返回 **404**。
- **控制器层（批量）**：`BaseController.filterTenant()` 只保留属于本租户的条目，直接剔除别家租户的记录。
- **库级自动追加 `WHERE tenant_id = ?`**：当前未启用（`MybatisPlusConfig` 只注册了 `PaginationInnerInterceptor`
  ），作为统一兜底仍在规划中。

::: warning 跨租户访问返回 404，不是 403
故意用"不存在"而非"无权限"——避免泄露"某个跨租户资源是否存在"。所以你查不到一台设备时，可能它真不存在，也可能它属于别的租户：对你而言两者无差别。批量查询走
`filterTenant()`，直接把不属于本租户的条目剔除，而不是报错。
:::

## 示例

开发环境通常只有一个默认租户，其 `tenantCode = default`——它同时是**系统管理员租户**：只有 `default` 租户里的用户才能创建/删除/修改其它租户（
`TenantController` 显式判定 `"default".equals(tenantCode)`）。

设想 SaaS 部署里再开一个客户租户 `tenantCode = acme`。`acme` 的运维 `alice` 登录后（令牌绑定 `acme` 的 `tenantId`）创建设备
`泵房-01`，这台设备落库时 `tenant_id` 自动写成 `acme`。此时 `default` 租户的管理员即便手握 `device:get` 权限，按 `泵房-01`
的 ID 去查，也会因 `requireTenant()` 比对失败而得到 404——除非他先切换到 `acme` 租户上下文。`alice` 反过来也看不到
`default` 租户的任何数据。

## 管理 API

租户管理端点在鉴权中心，前缀 `/tenant`（经网关为 `/api/v3/auth/tenant`）。非管理员只能操作自己所属的租户：

| 方法   | 路径                    | 说明                      |
|------|-----------------------|-------------------------|
| POST | `/tenant/add`         | 新增租户（仅 `default` 租户管理员） |
| POST | `/tenant/delete`      | 删除租户                    |
| POST | `/tenant/update`      | 修改租户                    |
| GET  | `/tenant/get_by_id`   | 按 ID 查询                 |
| GET  | `/tenant/get_by_code` | 按编码查询                   |
| POST | `/tenant/list`        | 分页查询                    |

## 延伸阅读

- [设备 Device](./device) — 最典型的"被租户隔离"的业务实体
- [核心概念与心智模型](../concepts) — 租户边界在整个对象模型中的位置
- [鉴权 · 租户 · RBAC](../../architecture/auth-rbac) — 主体、成员关系、RBAC 与接口层租户隔离的完整链路
- [快速开始](../../quickstart/) — 用默认 `default` 租户在本地起栈
