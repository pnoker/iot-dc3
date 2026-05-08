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
import type { PageQuery, PageResult } from '@/config/entity';
import type { UserForm, UserRecord } from '@/config/entity/crud';

export const addUser = (user: UserForm) => httpPost<R<UserRecord>>(`${API_AUTH_BASE}/user-profile/add`, user);

export const deleteUser = (id: string) => httpPost(`${API_AUTH_BASE}/user-profile/delete/${id}`);

export const updateUser = (user: UserForm) => httpPost<R<UserRecord>>(`${API_AUTH_BASE}/user-profile/update`, user);

export const getUserById = (id: string) => httpGet<R<UserRecord>>(`${API_AUTH_BASE}/user-profile/id/${id}`);

export const getUserByName = (name: string) => httpGet<R<UserRecord>>(`${API_AUTH_BASE}/user-profile/name/${name}`);

export const getUserList = <T = R<PageResult<UserRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_AUTH_BASE}/user-profile/list`, query);
