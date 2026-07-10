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
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {ProfileForm, ProfileRecord} from '@/config/types/manager';

export const addProfile = (profile: ProfileForm) =>
  httpPost<R<ProfileRecord>>(`${API_MANAGER_BASE}/profile/add`, profile);

export const deleteProfile = (id: string) => httpPost(`${API_MANAGER_BASE}/profile/delete`, undefined, {params: {id}});

export const updateProfile = (profile: ProfileForm) =>
  httpPost<R<ProfileRecord>>(`${API_MANAGER_BASE}/profile/update`, profile);

export const getProfileById = (id: string) =>
  httpGet<R<ProfileRecord>>(`${API_MANAGER_BASE}/profile/get_by_id`, {params: {id}});

export const listProfileByIds = (profileIds: string[]) =>
  httpPost<R<Record<string, ProfileRecord>>>(`${API_MANAGER_BASE}/profile/list_by_ids`, profileIds);

export const listProfileByDeviceId = (deviceId: string) =>
  httpGet(`${API_MANAGER_BASE}/profile/list_by_device_id`, {params: {device_id: deviceId}});

export const listProfile = <T = R<PageResult<ProfileRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/profile/list`, query);
