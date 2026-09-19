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

/**
 * Create role resource bind.
 * @param body - request body payload
 * @returns the operation result
 */
export const addRoleResourceBind = (body: RoleResourceBindForm) => httpPost(`${API_AUTH_BASE}/role_resource/add`, body);

/**
 * Delete role resource bind.
 * @param id - record id
 * @returns the operation result
 */
export const deleteRoleResourceBind = (id: string) =>
  httpDelete<void>(`${API_AUTH_BASE}/role_resource/delete`, {params: {id}});

/**
 * List role resource bind.
 * @param query - page query with filters and paging
 * @returns the listed role resource bind
 */
export const listRoleResourceBind = (query: PageQuery) =>
  httpPost<PageResult<RoleResourceBindRecord>, PageQuery>(`${API_AUTH_BASE}/role_resource/list`, query);

/**
 * List resource by role id.
 * @param roleId - role id to scope the request
 * @returns the listed resource by role id
 */
export const listResourceByRoleId = (roleId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_resource_by_role`, {params: {role_id: roleId}});

/**
 * List resource by principal id.
 * @param principalId - principal id to scope the request
 * @returns the listed resource by principal id
 */
export const listResourceByPrincipalId = (principalId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_resource_by_principal`, {params: {principal_id: principalId}});

/**
 * List role by resource id.
 * @param resourceId - resource id to scope the request
 * @returns the listed role by resource id
 */
export const listRoleByResourceId = (resourceId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_role_by_resource`, {params: {resource_id: resourceId}});
