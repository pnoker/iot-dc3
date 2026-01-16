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
 * Add a new point
 *
 * @param point Point object
 * @returns MyAxiosPromise
 */
export const pointAddApi = (point: any) =>
  request<R>({
    url: `api/v3/manager/point/add`,
    method: 'post',
    data: point,
  });

/**
 * Delete a point
 *
 * @param id Point ID
 * @returns MyAxiosPromise
 */
export const pointDeleteApi = (id: string) =>
  request<R>({
    url: `api/v3/manager/point/delete/${id}`,
    method: 'post',
  });

/**
 * Update a point
 *
 * @param point Point object
 * @returns MyAxiosPromise
 */
export const getPointUpdate = (point: any) =>
  request<R>({
    url: `api/v3/manager/point/update`,
    method: 'post',
    data: point,
  });

/**
 * Get point by ID
 *
 * @param id Point ID
 * @returns MyAxiosPromise
 */
export const getPointById = (id: string) =>
  request<R>({
    url: `api/v3/manager/point/id/${id}`,
    method: 'get',
  });

/**
 * Get points by IDs
 *
 * @param pointIds Point ID array
 * @returns MyAxiosPromise
 */
export const getPointByIds = (pointIds: any) =>
  request<R>({
    url: `api/v3/manager/point/ids`,
    method: 'post',
    data: pointIds,
  });

/**
 * Get point list with pagination and fuzzy search
 *
 * @param point Point query parameters
 * @returns MyAxiosPromise
 */
export const getPointList = (point: any) =>
  request<R>({
    url: `api/v3/manager/point/list`,
    method: 'post',
    data: point,
  });

/**
 * Get point units by point IDs
 *
 * @param pointIds Point ID array
 * @returns MyAxiosPromise
 */
export const getPointUnit = (pointIds: any) =>
  request<R>({
    url: `api/v3/manager/point/unit`,
    method: 'post',
    data: pointIds,
  });

/**
 * Get points by profile ID
 *
 * @param profileId Profile ID
 * @returns MyAxiosPromise
 */
export const getPointByProfileId = (profileId: string) =>
  request<R>({
    url: `api/v3/manager/point/profile_id/${profileId}`,
    method: 'get',
  });

/**
 * Get points by device ID
 *
 * @param deviceId Device ID
 * @returns MyAxiosPromise
 */
export const getPointByDeviceId = (deviceId: string) =>
  request<R>({
    url: `api/v3/manager/point/device_id/${deviceId}`,
    method: 'get',
  });

/**
 * Get latest point values with pagination
 *
 * @param pointValue Point value query parameters
 * @returns MyAxiosPromise
 */
export const getPointValueLatest = (pointValue: any) =>
  request<R>({
    url: `api/v3/data/point_value/latest`,
    method: 'post',
    data: pointValue,
  });

/**
 * Get point values with pagination
 *
 * @param pointValue Point value query parameters
 * @returns MyAxiosPromise
 */
export const getPointValueList = (pointValue: any) =>
  request<R>({
    url: `api/v3/data/point_value/list`,
    method: 'post',
    data: pointValue,
  });

/**
 * Get point value history
 *
 * @param deviceId Device ID
 * @param pointId Point ID
 * @param count Number of records to retrieve
 * @returns MyAxiosPromise
 */
export const getPointValueHistory = (deviceId: number, pointId: number, count: number = 100) =>
  request<R>({
    url: `api/v3/data/point_value/history/device_id/${deviceId}/point_id/${pointId}`,
    method: 'get',
    params: {
      count,
    },
  });

/**
 * Read point value
 *
 * @param pointValueReadVO Point value read object
 * @returns MyAxiosPromise
 */
export const readPointValue = (pointValueReadVO: any) =>
  request<R>({
    url: `api/v3/data/point_value_command/read`,
    method: 'post',
    data: pointValueReadVO,
  });

/**
 * Write point value
 *
 * @param pointValueWriteVO Point value write object
 * @returns MyAxiosPromise
 */
export const writePointValue = (pointValueWriteVO: any) =>
  request<R>({
    url: `api/v3/data/point_value_command/write`,
    method: 'post',
    data: pointValueWriteVO,
  });
