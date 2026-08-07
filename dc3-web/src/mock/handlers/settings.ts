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

import {on} from '../dispatch';
import {ok, responseOf} from '../response';
import {registerCrud} from '../crud';
import {db} from '../db';

/** Find the first row whose `id` equals `id`, compared as strings. */
const findById = (collection: Record<string, unknown>[], id: unknown) =>
  collection.find((c) => String(c.id) === String(id));

/** Drop undefined lookups so join results stay `Record`-typed. */
const defined = <T>(row: T | undefined): row is T => Boolean(row);

/**
 * Nest flat rows into a parent→children tree. Roots are rows whose
 * `parentIdField` is `0` (numeric or string); children are matched by
 * `parentIdField === node[idField]`, recursing depth-first.
 */
const buildTree = (
  rows: Record<string, unknown>[],
  idField: string,
  parentIdField: string,
): Record<string, unknown>[] => {
  const nest = (node: Record<string, unknown>): Record<string, unknown> => ({
    ...node,
    children: rows
      .filter((r) => String(r[parentIdField]) === String(node[idField]))
      .map(nest),
  });
  return rows.filter((r) => String(r[parentIdField]) === '0').map(nest);
};

export function registerSettingsHandlers(): void {
  // ── user ──
  registerCrud({
    baseUrl: 'api/v3/auth/user_profile',
    collection: 'users',
    search: ['userName', 'nickName', 'phone', 'email'],
  });
  on('get', 'api/v3/auth/user_profile/get_by_name', (ctx) => {
    const row = db.users.find((u) => String(u.userName) === String(ctx.params.name));
    return responseOf(ctx.config, ok(row ?? {}));
  });

  // ── role ──
  registerCrud({
    baseUrl: 'api/v3/auth/role',
    collection: 'roles',
    search: ['roleName', 'roleCode'],
  });
  on('post', 'api/v3/auth/role/list_tree', (ctx) =>
    responseOf(ctx.config, ok(buildTree(db.roles, 'id', 'parentRoleId'))),
  );

  // ── role ↔ principal bind ──
  registerCrud({
    baseUrl: 'api/v3/auth/role_principal',
    collection: 'rolePrincipalBinds',
    exact: ['roleId', 'principalId', 'principalType'],
  });
  // list_role_by_principal: roles granted to a principal
  on('get', 'api/v3/auth/role_principal/list_role_by_principal', (ctx) => {
    const roles = db.rolePrincipalBinds
      .filter((b) => String(b.principalId) === String(ctx.params.principal_id))
      .map((b) => findById(db.roles, b.roleId))
      .filter(defined);
    return responseOf(ctx.config, ok(roles));
  });
  // list_user_by_role: USER principals bound to a role → joined to user rows
  // (userName/nickName live on db.users, keyed by principalId)
  on('get', 'api/v3/auth/role_principal/list_user_by_role', (ctx) => {
    const users = db.rolePrincipalBinds
      .filter(
        (b) => String(b.roleId) === String(ctx.params.role_id) && String(b.principalType) === 'USER',
      )
      .map((b) => db.users.find((u) => String(u.principalId) === String(b.principalId)))
      .filter(defined);
    return responseOf(ctx.config, ok(users));
  });

  // ── role ↔ resource bind ──
  registerCrud({
    baseUrl: 'api/v3/auth/role_resource',
    collection: 'roleResourceBinds',
    exact: ['roleId', 'resourceId'],
  });
  on('get', 'api/v3/auth/role_resource/list_resource_by_role', (ctx) => {
    const resources = db.roleResourceBinds
      .filter((b) => String(b.roleId) === String(ctx.params.role_id))
      .map((b) => findById(db.resources, b.resourceId))
      .filter(defined);
    return responseOf(ctx.config, ok(resources));
  });
  // list_resource_by_principal: principal → rolePrincipalBinds → roles → resources (deduped)
  on('get', 'api/v3/auth/role_resource/list_resource_by_principal', (ctx) => {
    const roleIds = db.rolePrincipalBinds
      .filter((b) => String(b.principalId) === String(ctx.params.principal_id))
      .map((b) => String(b.roleId));
    const seen = new Set<string>();
    const resources: Record<string, unknown>[] = [];
    for (const b of db.roleResourceBinds) {
      if (!roleIds.includes(String(b.roleId))) continue;
      const key = String(b.resourceId);
      if (seen.has(key)) continue;
      seen.add(key);
      const resource = findById(db.resources, b.resourceId);
      if (resource) resources.push(resource);
    }
    return responseOf(ctx.config, ok(resources));
  });
  on('get', 'api/v3/auth/role_resource/list_role_by_resource', (ctx) => {
    const roles = db.roleResourceBinds
      .filter((b) => String(b.resourceId) === String(ctx.params.resource_id))
      .map((b) => findById(db.roles, b.roleId))
      .filter(defined);
    return responseOf(ctx.config, ok(roles));
  });

  // ── principal ──
  registerCrud({
    baseUrl: 'api/v3/auth/principal',
    collection: 'principals',
    search: ['principalName'],
    exact: ['principalType', 'enableFlag'],
    enable: true,
  });
  on('post', 'api/v3/auth/principal/list_by_ids', (ctx) => {
    const ids = Array.isArray(ctx.body) ? ctx.body : [];
    const idSet = new Set(ids.map((id) => String(id)));
    return responseOf(ctx.config, ok(db.principals.filter((p) => idSet.has(String(p.id)))));
  });

  // ── service account ──
  registerCrud({
    baseUrl: 'api/v3/auth/service_account',
    collection: 'serviceAccounts',
    search: ['serviceAccountName'],
    enable: true,
  });

  // ── tenant membership ──
  registerCrud({
    baseUrl: 'api/v3/auth/tenant_membership',
    collection: 'tenantMemberships',
    exact: ['principalId', 'tenantId'],
  });

  // ── identity audit ──
  registerCrud({
    baseUrl: 'api/v3/auth/identity_audit',
    collection: 'identityAudits',
    search: ['action', 'status'],
  });

  // ── local credential ──
  registerCrud({
    baseUrl: 'api/v3/auth/local_credential',
    collection: 'localCredentials',
    search: ['loginName'],
  });
  on('post', 'api/v3/auth/local_credential/reset_password', (ctx) =>
    responseOf(ctx.config, ok(true)),
  );
  on('get', 'api/v3/auth/local_credential/check', (ctx) => responseOf(ctx.config, ok(true)));

  // ── group ──
  registerCrud({
    baseUrl: 'api/v3/manager/group',
    collection: 'groups',
    search: ['groupName', 'groupCode'],
    exact: ['groupTypeFlag', 'enableFlag'],
  });

  // ── label ──
  registerCrud({
    baseUrl: 'api/v3/manager/label',
    collection: 'labels',
    search: ['labelName', 'labelCode'],
    exact: ['entityTypeFlag', 'enableFlag'],
  });

  // ── api ──
  registerCrud({
    baseUrl: 'api/v3/auth/api',
    collection: 'apis',
    search: ['apiName', 'apiCode'],
    exact: ['apiGroup', 'apiTypeFlag', 'enableFlag'],
  });

  // ── resource ──
  registerCrud({
    baseUrl: 'api/v3/auth/resource',
    collection: 'resources',
    search: ['resourceName', 'resourceCode'],
    exact: ['resourceTypeFlag', 'resourceScopeFlag', 'enableFlag'],
  });
  on('post', 'api/v3/auth/resource/list_tree', (ctx) =>
    responseOf(ctx.config, ok(buildTree(db.resources, 'id', 'parentResourceId'))),
  );
}
