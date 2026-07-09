/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
