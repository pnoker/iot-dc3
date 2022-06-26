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

import request from "@/config/axios";
import { R } from "@/config/type/types";

export const deviceAdd = (device: any) => request<R>({
    url: `api/v3/manager/device/add`,
    method: "post",
    data: device
}).then(res => res)

export const deviceDelete = (id: string) => request<R>({
    url: `api/v3/manager/device/delete/${id}`,
    method: "post"
}).then(res => res)

export const deviceUpdate = (device: any) => request<R>({
    url: `api/v3/manager/device/update`,
    method: "post",
    data: device
}).then(res => res)

export const deviceById = (id: string) => request<R>({
    url: `api/v3/manager/device/id/${id}`,
    method: "get"
}).then(res => res)

export const deviceByDriverId = (driverId: string) => request<R>({
    url: `api/v3/manager/device/driver_id/${driverId}`,
    method: "get"
}).then(res => res)

export const deviceByProfileId = (profileId: string) => request<R>({
    url: `api/v3/manager/device/profile_id/${profileId}`,
    method: "get"
}).then(res => res)

export const deviceList = (device: any) => request<R>({
    url: `api/v3/manager/device/list`,
    method: "post",
    data: device
}).then(res => res)

// device status
export const deviceStatus = (device) => request<R>({
    url: `api/v3/manager/status/device`,
    method: "post",
    data: device
}).then(res => res)

export const deviceStatusByDriverId = (driverId: string) => request<R>({
    url: `api/v3/manager/status/device/driver_id/${driverId}`,
    method: "get"
}).then(res => res)

export const deviceStatusByProfileId = (profileId: string) => request<R>({
    url: `api/v3/manager/status/device/profile_id/${profileId}`,
    method: "get"
}).then(res => res)
