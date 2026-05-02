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

import { httpGet, httpPost } from '@/api/common';
import { API_DATA_BASE, API_MANAGER_BASE } from '@/config/constant/api';

export const addDevice = (device: any) => httpPost(`${API_MANAGER_BASE}/device/add`, device);

export const deleteDevice = (id: string) => httpPost(`${API_MANAGER_BASE}/device/delete/${id}`);

export const updateDevice = (device: any) => httpPost(`${API_MANAGER_BASE}/device/update`, device);

export const getDeviceById = (id: string) => httpGet(`${API_MANAGER_BASE}/device/id/${id}`);

export const getDeviceByIds = (deviceIds: any) => httpPost(`${API_MANAGER_BASE}/device/ids`, deviceIds);

export const getDeviceByDriverId = (driverId: string) => httpGet(`${API_MANAGER_BASE}/device/driver_id/${driverId}`);

export const getDeviceByProfileId = (profileId: string) =>
  httpGet(`${API_MANAGER_BASE}/device/profile_id/${profileId}`);

export const getDeviceList = (device: any) => httpPost(`${API_MANAGER_BASE}/device/list`, device);

export const getDeviceStatus = (device: any) => httpPost(`${API_DATA_BASE}/device/status/device`, device);

export const getDeviceStatusByDriverId = (driverId: string) =>
  httpGet(`${API_DATA_BASE}/device/status/device/driver_id/${driverId}`);

export const getDeviceStatusByProfileId = (profileId: string) =>
  httpGet(`${API_DATA_BASE}/device/status/device/profile_id/${profileId}`);

export const importDeviceTemplate = (device: any) =>
  httpPost(`${API_MANAGER_BASE}/device/export/import_template`, device, { responseType: 'blob' });

export const importDevice = (device: any) =>
  httpPost(`${API_MANAGER_BASE}/device/import`, device, {
    timeout: 0,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
