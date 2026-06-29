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
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {DriverRecord} from '@/config/types/manager';

export const getDriverById = (id: string) =>
  httpGet<R<DriverRecord>>(`${API_MANAGER_BASE}/driver/get_by_id`, {params: {id}});

export const listDriverByIds = (driverIds: string[]) =>
  httpPost<R<Record<string, DriverRecord>>>(`${API_MANAGER_BASE}/driver/list_by_ids`, driverIds);

export const listDriver = <T = R<PageResult<DriverRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/driver/list`, query);

export const listDriverStatus = (query: Record<string, unknown>) =>
  httpPost(`${API_DATA_BASE}/driver/status/list`, query);
