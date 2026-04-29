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

export const addDevice = (device: any) => httpPost('api/v3/manager/device/add', device);

export const deleteDevice = (id: string) => httpPost(`api/v3/manager/device/delete/${id}`);

export const updateDevice = (device: any) => httpPost('api/v3/manager/device/update', device);

export const getDeviceById = (id: string) => httpGet(`api/v3/manager/device/id/${id}`);

export const getDeviceByIds = (deviceIds: any) => httpPost('api/v3/manager/device/ids', deviceIds);

export const getDeviceByDriverId = (driverId: string) => httpGet(`api/v3/manager/device/driver_id/${driverId}`);

export const getDeviceByProfileId = (profileId: string) => httpGet(`api/v3/manager/device/profile_id/${profileId}`);

export const getDeviceList = (device: any) => httpPost('api/v3/manager/device/list', device);

export const getDeviceStatus = (device: any) => httpPost('api/v3/data/device/status/device', device);

export const getDeviceStatusByDriverId = (driverId: string) =>
  httpGet(`api/v3/data/device/status/device/driver_id/${driverId}`);

export const getDeviceStatusByProfileId = (profileId: string) =>
  httpGet(`api/v3/data/device/status/device/profile_id/${profileId}`);

export const importDeviceTemplate = (device: any) =>
  httpPost('api/v3/manager/device/export/import_template', device, { responseType: 'blob' });

export const importDevice = (device: any) =>
  httpPost('api/v3/manager/device/import', device, {
    timeout: 0,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
