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
import type {ResourceForm, ResourceRecord} from '@/config/types/auth';

export const addResource = (resource: ResourceForm) =>
  httpPost<R<ResourceRecord>>(`${API_AUTH_BASE}/resource/add`, resource);

export const deleteResource = (id: string) => httpPost(`${API_AUTH_BASE}/resource/delete`, undefined, {params: {id}});

export const updateResource = (resource: ResourceForm) =>
  httpPost<R<ResourceRecord>>(`${API_AUTH_BASE}/resource/update`, resource);

export const getResourceById = (id: string) =>
  httpGet<R<ResourceRecord>>(`${API_AUTH_BASE}/resource/get_by_id`, {params: {id}});

export const listResource = <T = R<PageResult<ResourceRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_AUTH_BASE}/resource/list`, query);

export const listResourceTree = (query: PageQuery = {}) =>
  httpPost<R<ResourceRecord[]>>(`${API_AUTH_BASE}/resource/list_tree`, query);
