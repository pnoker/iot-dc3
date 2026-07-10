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

import {httpGet, httpPost} from '@/api/common';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {PageQuery} from '@/config/types';
import type {RolePrincipalBindForm} from '@/config/types/auth';

export const addRolePrincipalBind = (body: RolePrincipalBindForm) =>
  httpPost(`${API_AUTH_BASE}/role_principal/add`, body);

export const deleteRolePrincipalBind = (id: string) =>
  httpPost(`${API_AUTH_BASE}/role_principal/delete`, undefined, {params: {id}});

export const listRolePrincipalBind = (query: PageQuery) => httpPost(`${API_AUTH_BASE}/role_principal/list`, query);

export const listRoleByPrincipalId = (principalId: string) =>
  httpGet(`${API_AUTH_BASE}/role_principal/list_role_by_principal`, {params: {principal_id: principalId}});

export const listUserByRoleId = (roleId: string) =>
  httpGet(`${API_AUTH_BASE}/role_principal/list_user_by_role`, {params: {role_id: roleId}});
