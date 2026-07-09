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
