---
title: Tenant
---

<script setup>
import TenantRelationDiagram from '../../../.vitepress/theme/components/TenantRelationDiagram.vue'
import TenantAuthDiagram from '../../../.vitepress/theme/components/TenantAuthDiagram.vue'
</script>

# Tenant

> **A tenant is the isolation boundary for business data on the platform**—within a single deployment, company
> A's [devices](./device), points, data and company B's are invisible to each other. Every business record carries a
`tenantId`, and the platform uses it to slice data into mutually isolated partitions.

A tenant answers "who owns this record, and who can see it." It is not a feature or a role, but a **data wall**: the
token you receive after login is bound to one `tenantId`, and the devices you create afterward, the point values you
collect, the commands you dispatch all get stamped with that label automatically; accessing another tenant's records by
ID or in bulk gets them reported as nonexistent, or dropped.

What's easy to confuse is tenant versus [principal](../../architecture/auth-rbac) and role. In one line: **a tenant
governs "which data you can touch," a role governs "which kinds of operations you can perform," and the principal is "
who is operating."** The three are orthogonal—you may have `device:get` permission (granted by a role), yet getting
another tenant's device still fails (blocked by the tenant). Think of an office building: the access card decides which
floor you can enter (tenant), your rank decides which meeting rooms you can open on your own floor (role), and the badge
shows who you personally are (principal).

## Key Fields

Tenant `TenantBO` (table `dc3_tenant`, inheriting `id` / `remark` / audit fields from `BaseBO`):

| Field        | Type            | Meaning                                                                                                                       |
|--------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------|
| `tenantName` | String          | Tenant name (for display)                                                                                                     |
| `tenantCode` | String          | Unique tenant code, used to locate the tenant at login; the tenant whose code is `default` is the system-administrator tenant |
| `tenantExt`  | TenantExt(JSON) | Extension config, reserved field                                                                                              |
| `enableFlag` | EnableFlagEnum  | Enable flag, see below                                                                                                        |

A tenant is not isolated: which tenant an identity "belongs to" is declared row by row by the tenant membership
`TenantMembershipBO` (table `dc3_tenant_membership`), with a unique index on `(tenant_id, principal_id)`:

| Field              | Type                 | Meaning                                               |
|--------------------|----------------------|-------------------------------------------------------|
| `tenantId`         | Long                 | The owning tenant                                     |
| `principalId`      | Long                 | The owning [principal](../../architecture/auth-rbac)  |
| `principalType`    | PrincipalTypeEnum    | Principal type: `USER` / `SERVICE_ACCOUNT` / `SYSTEM` |
| `membershipStatus` | MembershipStatusEnum | Membership status: `ACTIVE` / `SUSPENDED` / `INVITED` |
| `joinedTime`       | LocalDateTime        | Join time                                             |

::: tip One person can belong to multiple tenants
Because the unique index is on `(tenant_id, principal_id)`, the same `USER` principal can have one membership row under
each of several tenants (multi-tenant membership). At login, `name + tenant` together locate which membership applies.
By design a `SERVICE_ACCOUNT` belongs to only one tenant.
:::

## Enable Flag `enableFlag`

| Value `EnableFlagEnum` | Database | Meaning  |
|------------------------|----------|----------|
| `ENABLE`               | `0`      | Enabled  |
| `DISABLE`              | `1`      | Disabled |

## Relationship to Other Concepts

<TenantRelationDiagram lang="en" />

- Every business entity implementing `TenantOwned` (which provides `getTenantId()`) is owned by some tenant and is the
  subject on which isolation is applied.
- A principal joins a tenant via `dc3_tenant_membership`; once inside, RBAC (`dc3_role_principal_bind`) decides what
  operations it may perform. See [Auth · Tenant · RBAC](../../architecture/auth-rbac).

## How Isolation Is Enforced

Tenant isolation lands at the controller layer: after fetching, it compares the entity's `tenantId` against the caller's
tenant, and cross-tenant access is reported as nonexistent or dropped.

<TenantAuthDiagram lang="en" />

- **Controller layer (single by ID)**: after fetching an entity, `BaseController.requireTenant()` compares the entity's
  `tenantId` against the caller's tenant; on mismatch (or a missing entity) it throws `NotFoundException`, returning *
  *404** to the outside.
- **Controller layer (bulk)**: `BaseController.filterTenant()` keeps only entries belonging to the caller's tenant,
  dropping records of other tenants.
- **Database-level auto-append of `WHERE tenant_id = ?`**: not currently enabled (`MybatisPlusConfig` only registers
  `PaginationInnerInterceptor`); a uniform backstop of this kind is still planned.

::: warning Cross-tenant access returns 404, not 403
This deliberately reports "does not exist" rather than "no permission"—to avoid leaking "whether a cross-tenant resource
exists." So when you can't find a device, it may genuinely not exist, or it may belong to another tenant: to you the two
are indistinguishable. Batch queries go through `filterTenant()`, which simply drops entries not belonging to your
tenant rather than erroring out.
:::

## Example

A development environment usually has just one default tenant whose `tenantCode = default`—which is also the *
*system-administrator tenant**: only users in the `default` tenant can create/delete/update other tenants (
`TenantController` explicitly checks `"default".equals(tenantCode)`).

Imagine a SaaS deployment adds a customer tenant `tenantCode = acme`. After `alice`, an operator of `acme`, logs in (
token bound to `acme`'s `tenantId`) and creates device `pumphouse-01`, the device is persisted with `tenant_id`
automatically set to `acme`. At this point an administrator of the `default` tenant, even holding `device:get`
permission, who queries `pumphouse-01` by its ID gets a 404 because `requireTenant()` fails the comparison—unless they
first switch into the `acme` tenant context. Conversely `alice` cannot see any data of the `default` tenant.

## Management API

Tenant management endpoints live in the auth center under the prefix `/tenant` (via the gateway, `/api/v3/auth/tenant`).
Non-administrators can operate only on the tenant they belong to:

| Method | Path                  | Description                                            |
|--------|-----------------------|--------------------------------------------------------|
| POST   | `/tenant/add`         | Add a tenant (only the `default` tenant administrator) |
| POST   | `/tenant/delete`      | Delete a tenant                                        |
| POST   | `/tenant/update`      | Update a tenant                                        |
| GET    | `/tenant/get_by_id`   | Query by ID                                            |
| GET    | `/tenant/get_by_code` | Query by code                                          |
| POST   | `/tenant/list`        | Paged query                                            |

## Further Reading

- [Device](./device) — the most typical business entity that gets tenant-isolated
- [Core Concepts and Mental Model](../concepts) — where the tenant boundary sits in the overall object model
- [Auth · Tenant · RBAC](../../architecture/auth-rbac) — the full chain of principal, membership, RBAC and
  controller-layer tenant isolation
- [Quick Start](../../quickstart/) — bring up the stack locally with the default `default` tenant
