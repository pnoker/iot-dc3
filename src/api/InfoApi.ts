/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import request from '@/config/axios'

export const driverInfoAddApi = (driverInfo: any) =>
    request<R>({
        url: `api/v3/manager/driver_info/add`,
        method: 'post',
        data: driverInfo,
    })

export const driverInfoUpdateApi = (driverInfo: any) =>
    request<R>({
        url: `api/v3/manager/driver_info/update`,
        method: 'post',
        data: driverInfo,
    })

export const driverInfoByAttributeIdAndDeviceIdApi = (attributeId: string, deviceId: string) =>
    request<R>({
        url: `api/v3/manager/driver_info/attributeId/${attributeId}/device_id/${deviceId}`,
        method: 'get',
    })

export const driverInfoByDeviceIdApi = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/driver_info/device_id/${deviceId}`,
        method: 'get',
    })

export const pointInfoAddApi = (pointInfo: any) =>
    request<R>({
        url: `api/v3/manager/point_info/add`,
        method: 'post',
        data: pointInfo,
    })

export const pointInfoUpdateApi = (pointInfo: any) =>
    request<R>({
        url: `api/v3/manager/point_info/update`,
        method: 'post',
        data: pointInfo,
    })

export const pointInfoByDeviceIdAndPointIdApi = (deviceId: string, pointId: string) =>
    request<R>({
        url: `api/v3/manager/point_info/device_id/${deviceId}/pointId/${pointId}`,
        method: 'get',
    })

export const pointInfoByDeviceIdApi = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/point_info/device_id/${deviceId}`,
        method: 'get',
    })
