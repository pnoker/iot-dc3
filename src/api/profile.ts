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
 * Add a new profile
 *
 * @param profile Profile object
 * @returns MyAxiosPromise
 */
export const addProfile = (profile: any) =>
  request<R>({
    url: `api/v3/manager/profile/add`,
    method: 'post',
    data: profile,
  });

/**
 * Delete a profile
 *
 * @param id Profile ID
 * @returns MyAxiosPromise
 */
export const deleteProfile = (id: string) =>
  request<R>({
    url: `api/v3/manager/profile/delete/${id}`,
    method: 'post',
  });

/**
 * Update a profile
 *
 * @param profile Profile object
 * @returns MyAxiosPromise
 */
export const updateProfile = (profile: any) =>
  request<R>({
    url: `api/v3/manager/profile/update`,
    method: 'post',
    data: profile,
  });

/**
 * Get profile by ID
 *
 * @param id Profile ID
 * @returns MyAxiosPromise
 */
export const getProfileById = (id: string) =>
  request<R>({
    url: `api/v3/manager/profile/id/${id}`,
    method: 'get',
  });

/**
 * Get profiles by IDs
 *
 * @param profileIds Profile ID array
 * @returns MyAxiosPromise
 */
export const getProfileByIds = (profileIds: any) =>
  request<R>({
    url: `api/v3/manager/profile/ids`,
    method: 'post',
    data: profileIds,
  });

/**
 * Get profile by device ID
 *
 * @param deviceId Device ID
 * @returns MyAxiosPromise
 */
export const getProfileByDeviceId = (deviceId: string) =>
  request<R>({
    url: `api/v3/manager/profile/device_id/${deviceId}`,
    method: 'get',
  });

/**
 * Get profile list with pagination
 *
 * @param profile Profile query parameters
 * @returns MyAxiosPromise
 */
export const getProfileList = (profile: any) =>
  request<R>({
    url: `api/v3/manager/profile/list`,
    method: 'post',
    data: profile,
  });
