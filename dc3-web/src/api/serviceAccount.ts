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
import {API_SERVICE_ACCOUNT_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {ServiceAccountForm, ServiceAccountRecord} from '@/config/types/auth';

export const addServiceAccount = (serviceAccount: ServiceAccountForm) =>
  httpPost<R<ServiceAccountRecord>>(`${API_SERVICE_ACCOUNT_BASE}/add`, serviceAccount);

export const deleteServiceAccount = (id: string) =>
  httpPost(`${API_SERVICE_ACCOUNT_BASE}/delete`, undefined, {params: {id}});

export const updateServiceAccount = (serviceAccount: ServiceAccountForm) =>
  httpPost<R<ServiceAccountRecord>>(`${API_SERVICE_ACCOUNT_BASE}/update`, serviceAccount);

export const enableServiceAccount = (id: string) =>
  httpPost(`${API_SERVICE_ACCOUNT_BASE}/enable`, undefined, {params: {id}});

export const disableServiceAccount = (id: string) =>
  httpPost(`${API_SERVICE_ACCOUNT_BASE}/disable`, undefined, {params: {id}});

export const getServiceAccountById = (id: string) =>
  httpGet<R<ServiceAccountRecord>>(`${API_SERVICE_ACCOUNT_BASE}/get_by_id`, {params: {id}});

export const listServiceAccount = <T = R<PageResult<ServiceAccountRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_SERVICE_ACCOUNT_BASE}/list`, query);
