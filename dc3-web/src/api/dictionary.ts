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

import {httpPost} from '@/api/common';
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {Dictionary, PageQuery, PageResult} from '@/config/types';

export const listDriverDictionary = <T = R<PageResult<Dictionary>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/driver`, query);

export const listDeviceDictionary = <T = R<PageResult<Dictionary>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/device`, query);

export const listProfileDictionary = <T = R<PageResult<Dictionary>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/profile`, query);

export const listPointDictionary = <T = R<PageResult<Dictionary>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/dictionary/device_point`, query);
