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
import type { PageQuery } from '@/config/types';
import type { RoleUserBindForm } from '@/config/types/auth';

export const addRoleUserBind = (body: RoleUserBindForm) => httpPost(`${API_AUTH_BASE}/role_user/add`, body);

export const deleteRoleUserBind = (id: string) =>
  httpPost(`${API_AUTH_BASE}/role_user/delete`, undefined, { params: { id } });

export const listRoleUserBind = (query: PageQuery) => httpPost(`${API_AUTH_BASE}/role_user/list`, query);

export const listRoleByUserId = (userId: string, tenantId?: string) => {
  const params: Record<string, string> = { user_id: userId };
  if (tenantId) {
    params.tenant_id = tenantId;
  }
  return httpGet(`${API_AUTH_BASE}/role_user/list_role_by_user`, { params });
};

export const listUserByRoleId = (roleId: string) =>
  httpGet(`${API_AUTH_BASE}/role_user/list_user_by_role`, { params: { role_id: roleId } });
