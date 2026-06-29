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
import {API_PRINCIPAL_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {PrincipalRecord} from '@/config/types/auth';

export const getPrincipalById = (id: string) =>
  httpGet<R<PrincipalRecord>>(`${API_PRINCIPAL_BASE}/get_by_id`, {params: {id}});

export const listPrincipal = <T = R<PageResult<PrincipalRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_PRINCIPAL_BASE}/list`, query);

// Batch-resolve principals by id → used to render principalId references in other
// lists as display names (settings family relations loader). Mirrors listDeviceByIds.
export const listPrincipalByIds = (ids: string[]) =>
  httpPost<R<PrincipalRecord[]>>(`${API_PRINCIPAL_BASE}/list_by_ids`, ids);

export const enablePrincipal = (id: string) => httpPost(`${API_PRINCIPAL_BASE}/enable`, undefined, {params: {id}});

export const disablePrincipal = (id: string) => httpPost(`${API_PRINCIPAL_BASE}/disable`, undefined, {params: {id}});
