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

import { httpGet, httpPost } from '@/api/common';

export const addProfile = (profile: any) => httpPost('api/v3/manager/profile/add', profile);

export const deleteProfile = (id: string) => httpPost(`api/v3/manager/profile/delete/${id}`);

export const updateProfile = (profile: any) => httpPost('api/v3/manager/profile/update', profile);

export const getProfileById = (id: string) => httpGet(`api/v3/manager/profile/id/${id}`);

export const getProfileByIds = (profileIds: any) => httpPost('api/v3/manager/profile/ids', profileIds);

export const getProfileByDeviceId = (deviceId: string) => httpGet(`api/v3/manager/profile/device_id/${deviceId}`);

export const getProfileList = (profile: any) => httpPost('api/v3/manager/profile/list', profile);
