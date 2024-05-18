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
 * 新增驱动配置
 *
 * @param driverInfo DriverInfo
 * @returns MyAxiosPromise
 */
export const addDriverInfo = (driverInfo: any) =>
    request<R>({
        url: `api/v3/manager/driver_attribute_config/add`,
        method: 'post',
        data: driverInfo
    })

/**
 * 更新驱动配置
 *
 * @param driverInfo DriverInfo
 * @returns MyAxiosPromise
 */
export const updateDriverInfo = (driverInfo: any) =>
    request<R>({
        url: `api/v3/manager/driver_attribute_config/update`,
        method: 'post',
        data: driverInfo
    })

/**
 * 通过设备ID和属性ID查询驱动配置
 *
 * @param deviceId DeviceId
 * @param attributeId AttributeId
 * @returns MyAxiosPromise
 */
export const getDriverInfoByDeviceIdAndAttributeId = (deviceId: string, attributeId: string) =>
    request<R>({
        url: `api/v3/manager/driver_attribute_config/device_id/${deviceId}/attribute_id/${attributeId}`,
        method: 'get'
    })

/**
 * 通过设备ID查询驱动配置
 *
 * @param deviceId DeviceId
 * @returns MyAxiosPromise
 */
export const getDriverInfoByDeviceId = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/driver_attribute_config/device_id/${deviceId}`,
        method: 'get'
    })

/**
 * 新增位号配置
 *
 * @param pointInfo PointInfo
 * @returns MyAxiosPromise
 */
export const addPointInfo = (pointInfo: any) =>
    request<R>({
        url: `api/v3/manager/point_attribute_config/add`,
        method: 'post',
        data: pointInfo
    })

/**
 * 更新位号配置
 *
 * @param pointInfo PointInfo
 * @returns MyAxiosPromise
 */
export const updatePointInfo = (pointInfo: any) =>
    request<R>({
        url: `api/v3/manager/point_attribute_config/update`,
        method: 'post',
        data: pointInfo
    })

/**
 * 通过设备ID和位号ID查询位号配置
 *
 * @param deviceId DeviceId
 * @param pointId PointId
 * @returns MyAxiosPromise
 */
export const getPointInfoByDeviceIdAndPointId = (deviceId: string, pointId: string) =>
    request<R>({
        url: `api/v3/manager/point_attribute_config/device_id/${deviceId}/point_id/${pointId}`,
        method: 'get'
    })

/**
 * 通过设备ID查询位号配置
 *
 * @param deviceId DeviceId
 * @returns MyAxiosPromise
 */
export const getPointInfoByDeviceId = (deviceId: string) =>
    request<R>({
        url: `api/v3/manager/point_attribute_config/device_id/${deviceId}`,
        method: 'get'
    })
