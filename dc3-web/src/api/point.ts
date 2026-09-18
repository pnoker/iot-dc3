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
import {createCrudApi} from '@/api/factory';
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {CursorPageResult, PageQuery, PageResult} from '@/config/types';
import type {PointForm, PointRecord} from '@/config/types/manager';

export interface PointCommandAccepted {
  commandId: string;
  statusUri: string;
}

const crud = createCrudApi<PointForm, PointRecord>({base: API_MANAGER_BASE, entity: 'point'});

export const addPoint = crud.add;

export const deletePoint = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/point`, id, version);

export const updatePoint = crud.update;

export const getPointById = crud.getById;

export const listPoint = <T = PointRecord>(query: PageQuery) =>
  httpPost<PageResult<T>, PageQuery>(`${API_MANAGER_BASE}/point/list`, query);

export const listPointByIds = (pointIds: string[]) =>
  httpPost<Record<string, PointRecord>>(`${API_MANAGER_BASE}/point/list_by_ids`, pointIds);

export const listPointUnit = (pointIds: string[]) =>
  httpPost<Record<string, string>>(`${API_MANAGER_BASE}/point/list_units`, pointIds);

export const listPointByProfileId = (profileId: string) =>
  httpGet<PointRecord[]>(`${API_MANAGER_BASE}/point/list_by_profile_id`, {params: {profile_id: profileId}});

export const listPointByDeviceId = (deviceId: string) =>
  httpGet<PointRecord[]>(`${API_MANAGER_BASE}/point/list_by_device_id`, {params: {device_id: deviceId}});

export const getPointValueLatest = (pointValue: Record<string, unknown>) =>
  httpPost<PageResult<Record<string, unknown>>>(`${API_DATA_BASE}/point_value/latest`, pointValue);

export const listPointValue = (pointValue: Record<string, unknown>) =>
  httpPost<CursorPageResult<Record<string, unknown>>>(`${API_DATA_BASE}/point_value/list`, pointValue);

export const listPointValueHistory = (deviceId: string, pointId: string, cursor?: string, limit = 100) =>
  httpGet<CursorPageResult<Record<string, unknown>>>(`${API_DATA_BASE}/point_value/history`, {
    params: {device_id: deviceId, point_id: pointId, cursor, limit}
  });

export const readPointValue = (pointValueReadVO: Record<string, unknown>) =>
  httpPost<PointCommandAccepted>(`${API_DATA_BASE}/point_command/read`, pointValueReadVO);

export const writePointValue = (pointValueWriteVO: Record<string, unknown>) =>
  httpPost<PointCommandAccepted>(`${API_DATA_BASE}/point_command/write`, pointValueWriteVO);
