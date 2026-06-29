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
