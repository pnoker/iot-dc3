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

import request from '@/config/axios';

/**
 * Add driver configuration
 *
 * @param driverInfo Driver configuration object
 * @returns MyAxiosPromise
 */
export const addDriverInfo = (driverInfo: any) =>
  request<R>({
    url: `api/v3/manager/driver_attribute_config/add`,
    method: 'post',
    data: driverInfo,
  });

/**
 * Update driver configuration
 *
 * @param driverInfo Driver configuration object
 * @returns MyAxiosPromise
 */
export const updateDriverInfo = (driverInfo: any) =>
  request<R>({
    url: `api/v3/manager/driver_attribute_config/update`,
    method: 'post',
    data: driverInfo,
  });

/**
 * Get driver configuration by device ID and attribute ID
 *
 * @param deviceId Device ID
 * @param attributeId Attribute ID
 * @returns MyAxiosPromise
 */
export const getDriverInfoByDeviceIdAndAttributeId = (deviceId: string, attributeId: string) =>
  request<R>({
    url: `api/v3/manager/driver_attribute_config/device_id/${deviceId}/attribute_id/${attributeId}`,
    method: 'get',
  });

/**
 * Get driver configuration by device ID
 *
 * @param deviceId Device ID
 * @returns MyAxiosPromise
 */
export const getDriverInfoByDeviceId = (deviceId: string) =>
  request<R>({
    url: `api/v3/manager/driver_attribute_config/device_id/${deviceId}`,
    method: 'get',
  });

/**
 * Add point configuration
 *
 * @param pointInfo Point configuration object
 * @returns MyAxiosPromise
 */
export const addPointInfo = (pointInfo: any) =>
  request<R>({
    url: `api/v3/manager/point_attribute_config/add`,
    method: 'post',
    data: pointInfo,
  });

/**
 * Update point configuration
 *
 * @param pointInfo Point configuration object
 * @returns MyAxiosPromise
 */
export const updatePointInfo = (pointInfo: any) =>
  request<R>({
    url: `api/v3/manager/point_attribute_config/update`,
    method: 'post',
    data: pointInfo,
  });

/**
 * Get point configuration by device ID and point ID
 *
 * @param deviceId Device ID
 * @param pointId Point ID
 * @returns MyAxiosPromise
 */
export const getPointInfoByDeviceIdAndPointId = (deviceId: string, pointId: string) =>
  request<R>({
    url: `api/v3/manager/point_attribute_config/device_id/${deviceId}/point_id/${pointId}`,
    method: 'get',
  });

/**
 * Get point configuration by device ID
 *
 * @param deviceId Device ID
 * @returns MyAxiosPromise
 */
export const getPointInfoByDeviceId = (deviceId: string) =>
  request<R>({
    url: `api/v3/manager/point_attribute_config/device_id/${deviceId}`,
    method: 'get',
  });
