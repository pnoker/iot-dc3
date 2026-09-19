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

import {httpDelete, httpGet, httpPost} from '@/api/common';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {RolePrincipalBindForm, RolePrincipalBindRecord} from '@/config/types/auth';

/**
 * Create role principal bind.
 * @param body - request body payload
 * @returns the operation result
 */
export const addRolePrincipalBind = (body: RolePrincipalBindForm) =>
  httpPost(`${API_AUTH_BASE}/role_principal/add`, body);

/**
 * Delete role principal bind.
 * @param id - record id
 * @returns the operation result
 */
export const deleteRolePrincipalBind = (id: string) =>
  httpDelete<void>(`${API_AUTH_BASE}/role_principal/delete`, {params: {id}});

/**
 * List role principal bind.
 * @param query - page query with filters and paging
 * @returns the listed role principal bind
 */
export const listRolePrincipalBind = (query: PageQuery) =>
  httpPost<PageResult<RolePrincipalBindRecord>, PageQuery>(`${API_AUTH_BASE}/role_principal/list`, query);

/**
 * List role by principal id.
 * @param principalId - principal id to scope the request
 * @returns the listed role by principal id
 */
export const listRoleByPrincipalId = (principalId: string) =>
  httpGet(`${API_AUTH_BASE}/role_principal/list_role_by_principal`, {params: {principal_id: principalId}});

/**
 * List user by role id.
 * @param roleId - role id to scope the request
 * @returns the listed user by role id
 */
export const listUserByRoleId = (roleId: string) =>
  httpGet(`${API_AUTH_BASE}/role_principal/list_user_by_role`, {params: {role_id: roleId}});
