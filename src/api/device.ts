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
 * Add a new device
 *
 * @param device Device object
 * @returns MyAxiosPromise
 */
export const addDevice = (device: any) =>
  request<R>({
    url: `api/v3/manager/device/add`,
    method: 'post',
    data: device,
  });

/**
 * Delete a device
 *
 * @param id Device ID
 * @returns MyAxiosPromise
 */
export const deleteDevice = (id: string) =>
  request<R>({
    url: `api/v3/manager/device/delete/${id}`,
    method: 'post',
  });

/**
 * Update a device
 *
 * @param device Device object
 * @returns MyAxiosPromise
 */
export const updateDevice = (device: any) =>
  request<R>({
    url: `api/v3/manager/device/update`,
    method: 'post',
    data: device,
  });

/**
 * Get device by ID
 *
 * @param id Device ID
 * @returns MyAxiosPromise
 */
export const getDeviceById = (id: string) =>
  request<R>({
    url: `api/v3/manager/device/id/${id}`,
    method: 'get',
  });

/**
 * Get devices by IDs
 *
 * @param deviceIds Device ID array
 * @returns MyAxiosPromise
 */
export const getDeviceByIds = (deviceIds: any) =>
  request<R>({
    url: `api/v3/manager/device/ids`,
    method: 'post',
    data: deviceIds,
  });

/**
 * Get devices by driver ID
 *
 * @param driverId Driver ID
 * @returns MyAxiosPromise
 */
export const getDeviceByDriverId = (driverId: string) =>
  request<R>({
    url: `api/v3/manager/device/driver_id/${driverId}`,
    method: 'get',
  });

/**
 * Get devices by profile ID
 *
 * @param profileId Profile ID
 * @returns MyAxiosPromise
 */
export const getDeviceByProfileId = (profileId: string) =>
  request<R>({
    url: `api/v3/manager/device/profile_id/${profileId}`,
    method: 'get',
  });

/**
 * Get device list with pagination
 *
 * @param device Device query parameters
 * @returns MyAxiosPromise
 */
export const getDeviceList = (device: any) =>
  request<R>({
    url: `api/v3/manager/device/list`,
    method: 'post',
    data: device,
  });

/**
 * Get device status with pagination
 *
 * @param device Device query parameters
 * @returns MyAxiosPromise
 */
export const getDeviceStatus = (device: any) =>
  request<R>({
    url: `api/v3/data/device/status/device`,
    method: 'post',
    data: device,
  });

/**
 * Get device status by driver ID
 *
 * @param driverId Driver ID
 * @returns MyAxiosPromise
 */
export const getDeviceStatusByDriverId = (driverId: string) =>
  request<R>({
    url: `api/v3/data/device/status/device/driver_id/${driverId}`,
    method: 'get',
  });

/**
 * Get device status by profile ID
 *
 * @param profileId Profile ID
 * @returns MyAxiosPromise
 */
export const getDeviceStatusByProfileId = (profileId: string) =>
  request<R>({
    url: `api/v3/data/device/status/device/profile_id/${profileId}`,
    method: 'get',
  });

/**
 * Get device import template
 *
 * @param device Device query parameters
 * @returns MyAxiosPromise
 */
export const importDeviceTemplate = (device: any) =>
  request<R>({
    url: `api/v3/manager/device/export/import_template`,
    responseType: 'blob',
    method: 'post',
    data: device,
  });

/**
 * Import devices from file
 *
 * @param device Device file data
 * @returns MyAxiosPromise
 */
export const importDevice = (device: any) =>
  request<R>({
    url: `api/v3/manager/device/import`,
    method: 'post',
    timeout: 0,
    headers: { 'Content-Type': 'multipart/form-data' },
    data: device,
  });
