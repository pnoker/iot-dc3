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
import type {LabelForm, LabelRecord} from '@/config/types/manager';

export const addLabel = (label: LabelForm) => httpPost(`${API_MANAGER_BASE}/label/add`, label);

export const deleteLabel = (id: string) => httpPost(`${API_MANAGER_BASE}/label/delete`, undefined, {params: {id}});

export const updateLabel = (label: LabelForm) => httpPost(`${API_MANAGER_BASE}/label/update`, label);

export const getLabelById = (id: string) =>
  httpGet<R<LabelRecord>>(`${API_MANAGER_BASE}/label/get_by_id`, {params: {id}});

export const listLabel = <T = R<PageResult<LabelRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/label/list`, query);
