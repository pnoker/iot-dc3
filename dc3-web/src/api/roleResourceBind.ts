/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {httpGet, httpPost} from '@/api/common';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {PageQuery} from '@/config/types';
import type {RoleResourceBindForm} from '@/config/types/auth';

export const addRoleResourceBind = (body: RoleResourceBindForm) => httpPost(`${API_AUTH_BASE}/role_resource/add`, body);

export const deleteRoleResourceBind = (id: string) =>
  httpPost(`${API_AUTH_BASE}/role_resource/delete`, undefined, {params: {id}});

export const listRoleResourceBind = (query: PageQuery) => httpPost(`${API_AUTH_BASE}/role_resource/list`, query);

export const listResourceByRoleId = (roleId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_resource_by_role`, {params: {role_id: roleId}});

export const listResourceByPrincipalId = (principalId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_resource_by_principal`, {params: {principal_id: principalId}});

export const listRoleByResourceId = (resourceId: string) =>
  httpGet(`${API_AUTH_BASE}/role_resource/list_role_by_resource`, {params: {resource_id: resourceId}});
