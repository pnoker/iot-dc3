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

export const pointAddApi = (point: any) => httpPost(`${API_MANAGER_BASE}/point/add`, point);

export const pointDeleteApi = (id: string) => httpPost(`${API_MANAGER_BASE}/point/delete/${id}`);

export const getPointUpdate = (point: any) => httpPost(`${API_MANAGER_BASE}/point/update`, point);

export const getPointById = (id: string) => httpGet(`${API_MANAGER_BASE}/point/id/${id}`);

export const getPointByIds = (pointIds: any) => httpPost(`${API_MANAGER_BASE}/point/ids`, pointIds);

export const getPointList = <T = R>(point: unknown) => httpPost<T>(`${API_MANAGER_BASE}/point/list`, point);

export const getPointUnit = (pointIds: any) => httpPost(`${API_MANAGER_BASE}/point/unit`, pointIds);

export const getPointByProfileId = (profileId: string) => httpGet(`${API_MANAGER_BASE}/point/profile_id/${profileId}`);

export const getPointByDeviceId = (deviceId: string) => httpGet(`${API_MANAGER_BASE}/point/device_id/${deviceId}`);

export const getPointValueLatest = (pointValue: any) => httpPost(`${API_DATA_BASE}/point_value/latest`, pointValue);

export const getPointValueList = (pointValue: any) => httpPost(`${API_DATA_BASE}/point_value/list`, pointValue);

export const getPointValueHistory = (deviceId: number, pointId: number, count: number = 100) =>
  httpGet(`${API_DATA_BASE}/point_value/history/device_id/${deviceId}/point_id/${pointId}`, {
    params: { count },
  });

export const readPointValue = (pointValueReadVO: any) =>
  httpPost(`${API_DATA_BASE}/point_value_command/read`, pointValueReadVO);

export const writePointValue = (pointValueWriteVO: any) =>
  httpPost(`${API_DATA_BASE}/point_value_command/write`, pointValueWriteVO);
