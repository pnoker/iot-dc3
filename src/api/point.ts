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

export const pointAddApi = (point: any) => httpPost('api/v3/manager/point/add', point);

export const pointDeleteApi = (id: string) => httpPost(`api/v3/manager/point/delete/${id}`);

export const getPointUpdate = (point: any) => httpPost('api/v3/manager/point/update', point);

export const getPointById = (id: string) => httpGet(`api/v3/manager/point/id/${id}`);

export const getPointByIds = (pointIds: any) => httpPost('api/v3/manager/point/ids', pointIds);

export const getPointList = (point: any) => httpPost('api/v3/manager/point/list', point);

export const getPointUnit = (pointIds: any) => httpPost('api/v3/manager/point/unit', pointIds);

export const getPointByProfileId = (profileId: string) => httpGet(`api/v3/manager/point/profile_id/${profileId}`);

export const getPointByDeviceId = (deviceId: string) => httpGet(`api/v3/manager/point/device_id/${deviceId}`);

export const getPointValueLatest = (pointValue: any) => httpPost('api/v3/data/point_value/latest', pointValue);

export const getPointValueList = (pointValue: any) => httpPost('api/v3/data/point_value/list', pointValue);

export const getPointValueHistory = (deviceId: number, pointId: number, count: number = 100) =>
  httpGet(`api/v3/data/point_value/history/device_id/${deviceId}/point_id/${pointId}`, {
    params: { count },
  });

export const readPointValue = (pointValueReadVO: any) =>
  httpPost('api/v3/data/point_value_command/read', pointValueReadVO);

export const writePointValue = (pointValueWriteVO: any) =>
  httpPost('api/v3/data/point_value_command/write', pointValueWriteVO);
