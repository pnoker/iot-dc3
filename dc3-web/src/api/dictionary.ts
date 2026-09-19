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

/**
 * List tenant dictionary.
 * @returns the listed tenant dictionary
 */
export const listTenantDictionary = () => httpGet<Dictionary[]>(`${API_AUTH_BASE}/dictionary/list_tenant`);

/**
 * List driver dictionary.
 * @param query - page query with filters and paging
 * @returns the listed driver dictionary
 */
export const listDriverDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_driver`, query);

/**
 * List device dictionary.
 * @param query - page query with filters and paging
 * @returns the listed device dictionary
 */
export const listDeviceDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_device`, query);

/**
 * List profile dictionary.
 * @param query - page query with filters and paging
 * @returns the listed profile dictionary
 */
export const listProfileDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_profile`, query);

/**
 * List point dictionary.
 * @param query - page query with filters and paging
 * @returns the listed point dictionary
 */
export const listPointDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_device_point`, query);

/**
 * List profile point dictionary.
 * @param query - page query with filters and paging
 * @returns the listed profile point dictionary
 */
export const listProfilePointDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_profile_point`, query);

/**
 * List driver device dictionary.
 * @param query - page query with filters and paging
 * @returns the listed driver device dictionary
 */
export const listDriverDeviceDictionary = <T = PageResult<Dictionary>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/list_driver_device`, query);
