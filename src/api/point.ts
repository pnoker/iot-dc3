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
import { R } from '@/config/types'

// point
export const pointAddApi = (point: any) =>
    request<R>({
        url: `api/v3/manager/point/add`,
        method: 'post',
        data: point,
    })

export const pointDeleteApi = (id: string) =>
    request<R>({
        url: `api/v3/manager/point/delete/${id}`,
        method: 'post',
    })

export const pointUpdateApi = (point: any) =>
    request<R>({
        url: `api/v3/manager/point/update`,
        method: 'post',
        data: point,
    })

export const pointByIdApi = (id: string) =>
    request<R>({
        url: `api/v3/manager/point/id/${id}`,
        method: 'get',
    })

export const pointByIdsApi = (pointIds: any) =>
    request<R>({
        url: `api/v3/manager/point/ids`,
        method: 'post',
        data: pointIds,
    })

export const pointListApi = (point: any) =>
    request<R>({
        url: `api/v3/manager/point/list`,
        method: 'post',
        data: point,
    })

export const pointUnitApi = (pointIds: any) =>
    request<R>({
        url: `api/v3/manager/point/unit`,
        method: 'post',
        data: pointIds,
    })

export const pointByProfileIdApi = (profileId: string) =>
    request<R>({
        url: `api/v3/manager/point/profile_id/${profileId}`,
        method: 'get',
    })

export const pointByDeviceIdApi = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/point/device_id/${deviceId}`,
        method: 'get',
    })

// point value
export const pointValueLatestApi = (pointValue: any) =>
    request<R>({
        url: `api/v3/data/point_value/latest`,
        method: 'post',
        data: pointValue,
    })

export const pointValueListApi = (pointValue: any) =>
    request<R>({
        url: `api/v3/data/point_value/list`,
        method: 'post',
        data: pointValue,
    })
