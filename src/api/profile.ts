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

import request from '@/config/axios'

/**
 * 新增模板
 *
 * @param profile Profile
 * @returns MyAxiosPromise
 */
export const addProfile = (profile: any) =>
    request<R>({
        url: `api/v3/manager/profile/add`,
        method: 'post',
        data: profile,
    })

/**
 * 删除模板
 *
 * @param id 模板ID
 * @returns MyAxiosPromise
 */
export const deleteProfile = (id: string) =>
    request<R>({
        url: `api/v3/manager/profile/delete/${id}`,
        method: 'post',
    })

/**
 * 修改模板
 *
 * @param profile Profile
 * @returns MyAxiosPromise
 */
export const updateProfile = (profile: any) =>
    request<R>({
        url: `api/v3/manager/profile/update`,
        method: 'post',
        data: profile,
    })

/**
 * 通过模板ID查询模板
 *
 * @param id 模板ID
 * @returns MyAxiosPromise
 */
export const getProfileById = (id: string) =>
    request<R>({
        url: `api/v3/manager/profile/id/${id}`,
        method: 'get',
    })

/**
 * 通过模板ID集查询模板
 *
 * @param profileIds ProfileId Array
 * @returns MyAxiosPromise
 */
export const getProfileByIds = (profileIds: any) =>
    request<R>({
        url: `api/v3/manager/profile/ids`,
        method: 'post',
        data: profileIds,
    })

/**
 * 通过设备ID查询模板
 *
 * @param deviceId DeviceId
 * @returns MyAxiosPromise
 */
export const getProfileByDeviceId = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/profile/device_id/${deviceId}`,
        method: 'get',
    })

/**
 * 分页查询
 *
 * @param profile Profile
 * @returns MyAxiosPromise
 */
export const getProfileList = (profile: any) =>
    request<R>({
        url: `api/v3/manager/profile/list`,
        method: 'post',
        data: profile,
    })
