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
import {API_AUTH_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {Dictionary, PageQuery, PageResult} from '@/config/types';

export const listTenantDictionary = () => httpGet<Dictionary[]>(`${API_AUTH_BASE}/dictionary/list_tenant`);

export const listDriverDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_driver`, query);

export const listDeviceDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_device`, query);

export const listProfileDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_profile`, query);

export const listPointDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_device_point`, query);

export const listProfilePointDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_profile_point`, query);

export const listDriverDeviceDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_driver_device`, query);
