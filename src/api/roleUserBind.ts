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

import { httpGet, httpPost } from '@/api/common';
import { API_AUTH_BASE } from '@/config/constant/api';

export const bindRoleUser = (body: any) => httpPost(`${API_AUTH_BASE}/role-user/add`, body);

export const unbindRoleUser = (id: string) => httpPost(`${API_AUTH_BASE}/role-user/delete/${id}`);

export const getRoleUserList = (query: any) => httpPost(`${API_AUTH_BASE}/role-user/list`, query);

export const listRoleByUserId = (userId: string, tenantId?: string | number) => {
  // Only append numeric tenantId; string tenant names would cause Long type-mismatch on the backend.
  const query = typeof tenantId === 'number' ? `?tenantId=${tenantId}` : '';
  return httpGet(`${API_AUTH_BASE}/role-user/list-role-by-user/${userId}${query}`);
};
