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

import {httpGet, httpPost, versionedDelete} from '@/api/common';
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {DriverRecord} from '@/config/types/manager';

/**
 * Fetch driver by id.
 * @param id - record id
 * @returns the fetched driver by id
 */
export const getDriverById = (id: string) =>
  httpGet<DriverRecord>(`${API_MANAGER_BASE}/driver/get_by_id`, {params: {id}});

/**
 * Delete driver.
 * @param id - record id
 * @param version - record version for optimistic locking
 * @returns the operation result
 */
export const deleteDriver = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/driver`, id, version);

/**
 * List driver by ids.
 * @param driverIds - driver ids to scope the request
 * @returns the listed driver by ids
 */
export const listDriverByIds = (driverIds: string[]) =>
  httpPost<Record<string, DriverRecord>>(`${API_MANAGER_BASE}/driver/list_by_ids`, driverIds);

/**
 * List driver.
 * @param query - page query with filters and paging
 * @returns the listed driver
 */
export const listDriver = <T = PageResult<DriverRecord>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/driver/list`, query);

/**
 * List driver status.
 * @param query - page query with filters and paging
 * @returns the listed driver status
 */
export const listDriverStatus = (query: Record<string, unknown>) =>
  httpPost<Record<string, string>>(`${API_DATA_BASE}/driver/status/list`, query);
