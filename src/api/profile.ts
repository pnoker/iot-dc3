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

export const profileAddApi = (profile: any) =>
    request<R>({
        url: `api/v3/manager/profile/add`,
        method: 'post',
        data: profile,
    })

export const profileDeleteApi = (id: string) =>
    request<R>({
        url: `api/v3/manager/profile/delete/${id}`,
        method: 'post',
    })

export const profileUpdateApi = (profile: any) =>
    request<R>({
        url: `api/v3/manager/profile/update`,
        method: 'post',
        data: profile,
    })

export const profileByIdApi = (id: string) =>
    request<R>({
        url: `api/v3/manager/profile/id/${id}`,
        method: 'get',
    })

export const profileByIdsApi = (profileIds: any) =>
    request<R>({
        url: `api/v3/manager/profile/ids`,
        method: 'post',
        data: profileIds,
    })

export const profileByDeviceIdApi = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/profile/device_id/${deviceId}`,
        method: 'get',
    })

export const profileListApi = (profile: any) =>
    request<R>({
        url: `api/v3/manager/profile/list`,
        method: 'post',
        data: profile,
    })
