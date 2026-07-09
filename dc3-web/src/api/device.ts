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
import type {DeviceForm, DeviceRecord} from '@/config/types/manager';

export const addDevice = (device: DeviceForm) => httpPost<R<DeviceRecord>>(`${API_MANAGER_BASE}/device/add`, device);

export const deleteDevice = (id: string) => httpPost(`${API_MANAGER_BASE}/device/delete`, undefined, {params: {id}});

export const updateDevice = (device: DeviceForm) =>
  httpPost<R<DeviceRecord>>(`${API_MANAGER_BASE}/device/update`, device);

export const getDeviceById = (id: string) =>
  httpGet<R<DeviceRecord>>(`${API_MANAGER_BASE}/device/get_by_id`, {params: {id}});

export const listDeviceByIds = (deviceIds: string[]) =>
  httpPost<R<Record<string, DeviceRecord>>>(`${API_MANAGER_BASE}/device/list_by_ids`, deviceIds);

export const getDeviceCountByDriverId = (driverId: string) =>
  httpGet(`${API_MANAGER_BASE}/device/get_count_by_driver_id`, {params: {driver_id: driverId}});

export const listDeviceByProfileId = (profileId: string) =>
  httpGet(`${API_MANAGER_BASE}/device/list_by_profile_id`, {params: {profile_id: profileId}});

export const listDevice = <T = R<PageResult<DeviceRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/device/list`, query);

export const listDeviceStatus = (query: Record<string, unknown>) =>
  httpPost(`${API_DATA_BASE}/device/status/list`, query);

export const listDeviceStatusByDriverId = (driverId: string) =>
  httpGet(`${API_DATA_BASE}/device/status/list_by_driver_id`, {params: {driver_id: driverId}});

export const listDeviceStatusByProfileId = (profileId: string) =>
  httpGet(`${API_DATA_BASE}/device/status/list_by_profile_id`, {params: {profile_id: profileId}});

export const listDeviceByPointId = (pointId: string) =>
  httpGet<
    R<{
      count: number;
      devices: DeviceRecord[];
    }>
  >(`${API_MANAGER_BASE}/point/list_device_statistics_by_point_id`, {
    params: {point_id: pointId},
  });

export const importDeviceTemplate = (device: Record<string, unknown>) =>
  httpPost(`${API_MANAGER_BASE}/device/export/import_template`, device, {responseType: 'blob'});

export const importDevice = (device: Record<string, unknown>) =>
  httpPost(`${API_MANAGER_BASE}/device/import`, device, {
    timeout: 0,
    headers: {'Content-Type': 'multipart/form-data'},
  });
