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
