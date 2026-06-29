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
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {PointForm, PointRecord} from '@/config/types/manager';

export const addPoint = (point: PointForm) => httpPost<R<PointRecord>>(`${API_MANAGER_BASE}/point/add`, point);

export const deletePoint = (id: string) => httpPost(`${API_MANAGER_BASE}/point/delete`, undefined, {params: {id}});

export const updatePoint = (point: PointForm) => httpPost<R<PointRecord>>(`${API_MANAGER_BASE}/point/update`, point);

export const getPointById = (id: string) =>
  httpGet<R<PointRecord>>(`${API_MANAGER_BASE}/point/get_by_id`, {params: {id}});

export const listPointByIds = (pointIds: string[]) =>
  httpPost<R<Record<string, PointRecord>>>(`${API_MANAGER_BASE}/point/list_by_ids`, pointIds);

export const listPoint = <T = R<PageResult<PointRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/point/list`, query);

export const listPointUnit = (pointIds: string[]) => httpPost(`${API_MANAGER_BASE}/point/unit`, pointIds);

export const listPointByProfileId = (profileId: string) =>
  httpGet(`${API_MANAGER_BASE}/point/list_by_profile_id`, {params: {profile_id: profileId}});

export const listPointByDeviceId = (deviceId: string) =>
  httpGet(`${API_MANAGER_BASE}/point/list_by_device_id`, {params: {device_id: deviceId}});

export const getPointValueLatest = (pointValue: Record<string, unknown>) =>
  httpPost(`${API_DATA_BASE}/point_value/latest`, pointValue);

export const listPointValue = (pointValue: Record<string, unknown>) =>
  httpPost(`${API_DATA_BASE}/point_value/list`, pointValue);

export const listPointValueHistory = (deviceId: string, pointId: string, count = 100) =>
  httpGet(`${API_DATA_BASE}/point_value/list_history_by_device_id_and_point_id`, {
    params: {device_id: deviceId, point_id: pointId, count},
  });

export const readPointValue = (pointValueReadVO: Record<string, unknown>) =>
  httpPost(`${API_DATA_BASE}/point_command/read`, pointValueReadVO);

export const writePointValue = (pointValueWriteVO: Record<string, unknown>) =>
  httpPost(`${API_DATA_BASE}/point_command/write`, pointValueWriteVO);
