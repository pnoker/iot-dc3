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
import type {PageQuery, PageResult} from '@/config/types';
import type {RoleForm, RoleRecord} from '@/config/types/auth';

export const addRole = (role: RoleForm) => httpPost(`${API_AUTH_BASE}/role/add`, role);

export const deleteRole = (id: string) => httpPost(`${API_AUTH_BASE}/role/delete`, undefined, {params: {id}});

export const updateRole = (role: RoleForm) => httpPost(`${API_AUTH_BASE}/role/update`, role);

export const getRoleById = (id: string) => httpGet(`${API_AUTH_BASE}/role/get_by_id`, {params: {id}});

export const listRole = <T = R<PageResult<RoleRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_AUTH_BASE}/role/list`, query);

export const listRoleTree = (query: PageQuery = {}) => httpPost(`${API_AUTH_BASE}/role/list_tree`, query);
