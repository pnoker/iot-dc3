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
import type {RoleResourceBindForm, RoleResourceBindRecord} from '@/config/types/auth';

export const addRoleResourceBind = (body: RoleResourceBindForm) => httpPost(`${API_AUTH_BASE}/role_resource/add`, body);

export const deleteRoleResourceBind = (id: string) =>
  httpDelete<void>(`${API_AUTH_BASE}/role_resource/delete`, {params: {id}});

export const listRoleResourceBind = (query: PageQuery) =>
  httpPost<PageResult<RoleResourceBindRecord>, PageQuery>(`${API_AUTH_BASE}/role_resource/list`, query);

export const listResourceByRoleId = (roleId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_resource_by_role`, {params: {role_id: roleId}});

export const listResourceByPrincipalId = (principalId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_resource_by_principal`, {params: {principal_id: principalId}});

export const listRoleByResourceId = (resourceId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_role_by_resource`, {params: {resource_id: resourceId}});
