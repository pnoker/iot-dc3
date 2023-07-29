/*
 * Copyright 2016-present the original author or authors.
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
 * 新增位号
 *
 * @param point Point
 * @returns MyAxiosPromise
 */
export const pointAddApi = (point: any) =>
    request<R>({
        url: `api/v3/manager/point/add`,
        method: 'post',
        data: point,
    })

/**
 * 新增位号
 *
 * @param point Point
 * @returns MyAxiosPromise
 */
export const pointDeleteApi = (id: string) =>
    request<R>({
        url: `api/v3/manager/point/delete/${id}`,
        method: 'post',
    })

/**
 * 更新位号
 *
 * @param point Point
 * @returns MyAxiosPromise
 */
export const getPointUpdate = (point: any) =>
    request<R>({
        url: `api/v3/manager/point/update`,
        method: 'post',
        data: point,
    })

/**
 * 通过位号ID查询位号
 *
 * @param id 位号ID
 * @returns MyAxiosPromise
 */
export const getPointById = (id: string) =>
    request<R>({
        url: `api/v3/manager/point/id/${id}`,
        method: 'get',
    })

/**
 * 通过位号ID集查询位号
 *
 * @param pointIds PointId Array
 * @returns MyAxiosPromise
 */
export const getPointByIds = (pointIds: any) =>
    request<R>({
        url: `api/v3/manager/point/ids`,
        method: 'post',
        data: pointIds,
    })

/**
 * 模糊分页查询位号
 *
 * @param point Point
 * @returns MyAxiosPromise
 */
export const getPointList = (point: any) =>
    request<R>({
        url: `api/v3/manager/point/list`,
        method: 'post',
        data: point,
    })

/**
 * 通过位号ID集查询位号单位
 *
 * @param pointIds PointId Array
 * @returns MyAxiosPromise
 */
export const getPointUnit = (pointIds: any) =>
    request<R>({
        url: `api/v3/manager/point/unit`,
        method: 'post',
        data: pointIds,
    })

/**
 * 通过模板ID查询位号
 *
 * @param profileId 模板ID
 * @returns MyAxiosPromise
 */
export const getPointByProfileId = (profileId: string) =>
    request<R>({
        url: `api/v3/manager/point/profile_id/${profileId}`,
        method: 'get',
    })

/**
 * 通过设备ID查询位号
 *
 * @param deviceId 设备ID
 * @returns MyAxiosPromise
 */
export const getPointByDeviceId = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/point/device_id/${deviceId}`,
        method: 'get',
    })

/**
 * 分页查询最新位号值
 *
 * @param pointValue PointValue
 * @returns MyAxiosPromise
 */
export const getPointValueLatest = (pointValue: any) =>
    request<R>({
        url: `api/v3/data/point_value/latest`,
        method: 'post',
        data: pointValue,
    })

/**
 * 分页查询位号值
 *
 * @param pointValue PointValue
 * @returns MyAxiosPromise
 */
export const getPointValueList = (pointValue: any) =>
    request<R>({
        url: `api/v3/data/point_value/list`,
        method: 'post',
        data: pointValue,
    })

/**
 * 读位号值
 *
 * @param pointValueReadVO PointValueReadVO
 * @returns MyAxiosPromise
 */
export const readPointValue = (pointValueReadVO: any) =>
    request<R>({
        url: `api/v3/data/point_value_command/read`,
        method: 'post',
        data: pointValueReadVO,
    })

/**
 * 写位号值
 *
 * @param pointValueWriteVO PointValueWriteVO
 * @returns MyAxiosPromise
 */
export const writePointValue = (pointValueWriteVO: any) =>
    request<R>({
        url: `api/v3/data/point_value_command/write`,
        method: 'post',
        data: pointValueWriteVO,
    })
