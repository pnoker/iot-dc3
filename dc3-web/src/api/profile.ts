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
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {ProfileForm, ProfileRecord} from '@/config/types/manager';

/**
 * Create profile.
 * @param profile - profile record the operation targets
 * @returns the operation result
 */
export const addProfile = (profile: ProfileForm) =>
  httpPost<ProfileRecord>(`${API_MANAGER_BASE}/profile/add`, profile);

/**
 * Delete profile.
 * @param id - record id
 * @param version - record version for optimistic locking
 * @returns the operation result
 */
export const deleteProfile = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/profile`, id, version);

/**
 * Update profile.
 * @param profile - profile record the operation targets
 * @returns the operation result
 */
export const updateProfile = (profile: ProfileForm) =>
  httpPost<ProfileRecord>(`${API_MANAGER_BASE}/profile/update`, profile);

/**
 * Fetch profile by id.
 * @param id - record id
 * @returns the fetched profile by id
 */
export const getProfileById = (id: string) =>
  httpGet<ProfileRecord>(`${API_MANAGER_BASE}/profile/get_by_id`, {params: {id}});

/**
 * List profile by ids.
 * @param profileIds - profile ids to scope the request
 * @returns the listed profile by ids
 */
export const listProfileByIds = (profileIds: string[]) =>
  httpPost<Record<string, ProfileRecord>>(`${API_MANAGER_BASE}/profile/list_by_ids`, profileIds);

/**
 * List profile by device id.
 * @param deviceId - device id to scope the request
 * @returns the listed profile by device id
 */
export const listProfileByDeviceId = (deviceId: string) =>
  httpGet(`${API_MANAGER_BASE}/profile/list_by_device_id`, {params: {device_id: deviceId}});

/**
 * List profile.
 * @param query - page query with filters and paging
 * @returns the listed profile
 */
export const listProfile = <T = PageResult<ProfileRecord>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/profile/list`, query);
