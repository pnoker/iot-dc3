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
import type {PointValueDashboard} from '@/config/types/dashboard';
import type {PointForm, PointRecord} from '@/config/types/manager';

/**
 * Point command acceptance envelope.
 */
export interface PointCommandAccepted {
  commandId: string;
  statusUri: string;
}

const crud = createCrudApi<PointForm, PointRecord>({base: API_MANAGER_BASE, entity: 'point'});

export const addPoint = crud.add;

/**
 * Delete point.
 * @param id - record id
 * @param version - record version for optimistic locking
 * @returns the operation result
 */
export const deletePoint = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/point`, id, version);

export const updatePoint = crud.update;

export const getPointById = crud.getById;

/**
 * List point.
 * @param query - page query with filters and paging
 * @returns the listed point
 */
export const listPoint = <T = PointRecord>(query: PageQuery) =>
  httpPost<PageResult<T>, PageQuery>(`${API_MANAGER_BASE}/point/list`, query);

/**
 * List point by ids.
 * @param pointIds - point ids to scope the request
 * @returns the listed point by ids
 */
export const listPointByIds = (pointIds: string[]) =>
  httpPost<Record<string, PointRecord>>(`${API_MANAGER_BASE}/point/list_by_ids`, pointIds);

/**
 * List point unit.
 * @param pointIds - point ids to scope the request
 * @returns the listed point unit
 */
export const listPointUnit = (pointIds: string[]) =>
  httpPost<Record<string, string>>(`${API_MANAGER_BASE}/point/list_units`, pointIds);

/**
 * List point by profile id.
 * @param profileId - profile id to scope the request
 * @returns the listed point by profile id
 */
export const listPointByProfileId = (profileId: string) =>
  httpGet<PointRecord[]>(`${API_MANAGER_BASE}/point/list_by_profile_id`, {params: {profile_id: profileId}});

/**
 * List point by device id.
 * @param deviceId - device id to scope the request
 * @returns the listed point by device id
 */
export const listPointByDeviceId = (deviceId: string) =>
  httpGet<PointRecord[]>(`${API_MANAGER_BASE}/point/list_by_device_id`, {params: {device_id: deviceId}});

/**
 * Fetch point value latest.
 * @param pointValue - point reading to format
 * @returns the fetched point value latest
 */
export const getPointValueLatest = (pointValue: Record<string, unknown>) =>
  httpPost<PageResult<Record<string, unknown>>>(`${API_DATA_BASE}/point_value/latest`, pointValue);

/**
 * List point value.
 * @param pointValue - point reading to format
 * @returns the listed point value
 */
export const listPointValue = (pointValue: Record<string, unknown>) =>
  httpPost<CursorPageResult<Record<string, unknown>>>(`${API_DATA_BASE}/point_value/list`, pointValue);

/**
 * List point value history.
 * @param deviceId - device id to scope the request
 * @param pointId - point id to scope the request
 * @param cursor - cursor marking the next page
 * @param limit - maximum number of entries to return or generate
 * @returns the listed point value history
 */
export const listPointValueHistory = (deviceId: string, pointId: string, cursor?: string, limit = 100) =>
  httpGet<CursorPageResult<Record<string, unknown>>>(`${API_DATA_BASE}/point_value/history`, {
    params: {device_id: deviceId, point_id: pointId, cursor, limit}
  });

/**
 * Fetch the data-dashboard payload of a single point: trend band, hourly
 * volume, value histogram, sampling-interval histogram, gaps, typical-day
 * curve and window stats — everything in one round trip.
 * @param deviceId - device id whose point is being queried
 * @param pointId - point id whose dashboard is being queried
 * @param rangeHours - lookback window in hours from 1 through 168; defaults to 24
 * @returns the point dashboard payload
 */
export const getPointValueDashboard = (deviceId: string, pointId: string, rangeHours = 24) =>
  httpGet<PointValueDashboard>(`${API_DATA_BASE}/point_value/dashboard`, {
    params: {device_id: deviceId, point_id: pointId, range_hours: rangeHours}
  });

/**
 * Read the current value of a point.
 * @param pointValueReadVO - point value read payload
 * @returns the parsed point value
 */
export const readPointValue = (pointValueReadVO: Record<string, unknown>) =>
  httpPost<PointCommandAccepted>(`${API_DATA_BASE}/point_command/read`, pointValueReadVO);

/**
 * Write a value to a point.
 * @param pointValueWriteVO - point value write payload
 * @returns the point command accepted response
 */
export const writePointValue = (pointValueWriteVO: Record<string, unknown>) =>
  httpPost<PointCommandAccepted>(`${API_DATA_BASE}/point_command/write`, pointValueWriteVO);
