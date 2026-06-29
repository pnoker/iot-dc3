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
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {GroupForm, GroupRecord} from '@/config/types/manager';

export const addGroup = (group: GroupForm) => httpPost(`${API_MANAGER_BASE}/group/add`, group);

export const deleteGroup = (id: string) => httpPost(`${API_MANAGER_BASE}/group/delete`, undefined, {params: {id}});

export const updateGroup = (group: GroupForm) => httpPost(`${API_MANAGER_BASE}/group/update`, group);

export const getGroupById = (id: string) =>
  httpGet<R<GroupRecord>>(`${API_MANAGER_BASE}/group/get_by_id`, {params: {id}});

export const listGroup = <T = R<PageResult<GroupRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/group/list`, query);
