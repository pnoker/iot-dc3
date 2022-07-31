/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import request from '@/config/axios'
import { R } from '@/config/type/types'

export const deviceAddApi = (device: any) =>
    request<R>({
        url: `api/v3/manager/device/add`,
        method: 'post',
        data: device,
    })

export const deviceDeleteApi = (id: string) =>
    request<R>({
        url: `api/v3/manager/device/delete/${id}`,
        method: 'post',
    })

export const deviceUpdateApi = (device: any) =>
    request<R>({
        url: `api/v3/manager/device/update`,
        method: 'post',
        data: device,
    })

export const deviceByIdApi = (id: string) =>
    request<R>({
        url: `api/v3/manager/device/id/${id}`,
        method: 'get',
    })

export const deviceByIdsApi = (deviceIds: any) =>
    request<R>({
        url: `api/v3/manager/device/ids`,
        method: 'post',
        data: deviceIds,
    })

export const deviceByDriverIdApi = (driverId: string) =>
    request<R>({
        url: `api/v3/manager/device/driver_id/${driverId}`,
        method: 'get',
    })

export const deviceByProfileIdApi = (profileId: string) =>
    request<R>({
        url: `api/v3/manager/device/profile_id/${profileId}`,
        method: 'get',
    })

export const deviceListApi = (device: any) =>
    request<R>({
        url: `api/v3/manager/device/list`,
        method: 'post',
        data: device,
    })

// device status
export const deviceStatusApi = (device) =>
    request<R>({
        url: `api/v3/manager/status/device`,
        method: 'post',
        data: device,
    })

export const deviceStatusByDriverIdApi = (driverId: string) =>
    request<R>({
        url: `api/v3/manager/status/device/driver_id/${driverId}`,
        method: 'get',
    })

export const deviceStatusByProfileIdApi = (profileId: string) =>
    request<R>({
        url: `api/v3/manager/status/device/profile_id/${profileId}`,
        method: 'get',
    })
