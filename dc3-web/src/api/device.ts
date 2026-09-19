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
import type {AxiosResponse} from 'axios';
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {DeviceForm, DeviceRecord} from '@/config/types/manager';
import type {OperationAccepted, OperationView} from '@/config/types/operation';

/**
 * Create device.
 * @param device - device record the operation targets
 * @returns the operation result
 */
export const addDevice = (device: DeviceForm) => httpPost<DeviceRecord>(`${API_MANAGER_BASE}/device/add`, device);

/**
 * Delete device.
 * @param id - record id
 * @param version - record version for optimistic locking
 * @returns the operation result
 */
export const deleteDevice = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/device`, id, version);

/**
 * Update device.
 * @param device - device record the operation targets
 * @returns the operation result
 */
export const updateDevice = (device: DeviceForm) =>
  httpPost<DeviceRecord>(`${API_MANAGER_BASE}/device/update`, device);

/**
 * Fetch device by id.
 * @param id - record id
 * @returns the fetched device by id
 */
export const getDeviceById = (id: string) =>
  httpGet<DeviceRecord>(`${API_MANAGER_BASE}/device/get_by_id`, {params: {id}});

/**
 * List device by ids.
 * @param deviceIds - device ids to scope the request
 * @returns the listed device by ids
 */
export const listDeviceByIds = (deviceIds: string[]) =>
  httpPost<Record<string, DeviceRecord>>(`${API_MANAGER_BASE}/device/list_by_ids`, deviceIds);

/**
 * Fetch device count by driver id.
 * @param driverId - driver id to scope the request
 * @returns the fetched device count by driver id
 */
export const getDeviceCountByDriverId = (driverId: string) =>
  httpGet(`${API_MANAGER_BASE}/device/get_count_by_driver_id`, {params: {driver_id: driverId}});

/**
 * List device by profile id.
 * @param profileId - profile id to scope the request
 * @returns the listed device by profile id
 */
export const listDeviceByProfileId = (profileId: string) =>
  httpGet(`${API_MANAGER_BASE}/device/list_by_profile_id`, {params: {profile_id: profileId}});

/**
 * List device.
 * @param query - page query with filters and paging
 * @returns the listed device
 */
export const listDevice = <T = PageResult<DeviceRecord>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/device/list`, query);

/**
 * List device status.
 * @param query - page query with filters and paging
 * @returns the listed device status
 */
export const listDeviceStatus = (query: Record<string, unknown>) =>
  httpPost(`${API_DATA_BASE}/device/status/list`, query);

/**
 * List device status by driver id.
 * @param driverId - driver id to scope the request
 * @returns the listed device status by driver id
 */
export const listDeviceStatusByDriverId = (driverId: string) =>
  httpGet(`${API_DATA_BASE}/device/status/list_by_driver_id`, {params: {driver_id: driverId}});

/**
 * List device status by profile id.
 * @param profileId - profile id to scope the request
 * @returns the listed device status by profile id
 */
export const listDeviceStatusByProfileId = (profileId: string) =>
  httpGet(`${API_DATA_BASE}/device/status/list_by_profile_id`, {params: {profile_id: profileId}});

/**
 * Fetch device statistics by point id.
 * @param pointId - point id to scope the request
 * @returns the fetched device statistics by point id
 */
export const getDeviceStatisticsByPointId = (pointId: string) =>
  httpGet<
    {
      count: number;
      devices: DeviceRecord[];
    }
  >(`${API_MANAGER_BASE}/point/get_device_statistics_by_point_id`, {
    params: {point_id: pointId},
  });

/**
 * Import device template.
 * @param device - device record the operation targets
 * @returns the operation result
 */
export const importDeviceTemplate = (device: Record<string, unknown>) =>
  httpPost<AxiosResponse<Blob>>(`${API_MANAGER_BASE}/device/export/import_template`, device, {responseType: 'blob'});

/**
 * Import device.
 * @param form - form payload to submit
 * @param file - uploaded file payload
 * @param idempotencyKey - idempotency key guarding duplicate submissions
 * @returns the operation result
 */
export const importDevice = (form: Record<string, unknown>, file: File, idempotencyKey: string) => {
  const data = new FormData();
  data.append(
    'request',
    new Blob([JSON.stringify({driverId: form.driverId, profileId: form.profileId})], {type: 'application/json'})
  );
  data.append('file', file);
  // No manual Content-Type: axios clears it for FormData in browsers so the
  // client sets the multipart boundary automatically.
  return httpPost<OperationAccepted>(`${API_MANAGER_BASE}/device/import`, data, {
    headers: {'Idempotency-Key': idempotencyKey},
    timeout: 0,
  });
};

/**
 * Fetch device import operation.
 * @param statusUri - status endpoint address
 * @param signal - abort signal
 * @returns the fetched device import operation
 */
export const getDeviceImportOperation = (statusUri: string, signal?: AbortSignal) => {
  const [path = '', query = ''] = statusUri.split('?', 2);
  return httpGet<OperationView>(path.replace(/^\/+/, ''), {
    params: Object.fromEntries(new URLSearchParams(query)),
    signal,
  });
};
