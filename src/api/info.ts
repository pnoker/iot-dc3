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

export const driverInfoAdd = (driverInfo: any) => request<R>({
    url: `api/v3/manager/driver_info/add`,
    method: "post",
    data: driverInfo
}).then(res => res)

export const driverInfoUpdate = (driverInfo: any) => request<R>({
    url: `api/v3/manager/driver_info/update`,
    method: "post",
    data: driverInfo
}).then(res => res)

export const driverInfoByAttributeIdAndDeviceId = (attributeId: string, deviceId: string) => request<R>({
    url: `api/v3/manager/driver_info/attributeId/${attributeId}/device_id/${deviceId}`,
    method: "get"
}).then(res => res)

export const driverInfoByDeviceId = (deviceId: string) => request<R>({
    url: `api/v3/manager/driver_info/device_id/${deviceId}`,
    method: "get"
}).then(res => res)

export const pointInfoAdd = (pointInfo: any) => request<R>({
    url: `api/v3/manager/point_info/add`,
    method: "post",
    data: pointInfo
}).then(res => res)

export const pointInfoUpdate = (pointInfo: any) => request<R>({
    url: `api/v3/manager/point_info/update`,
    method: "post",
    data: pointInfo
}).then(res => res)

export const pointInfoByDeviceIdAndPointId = (deviceId: string, pointId: string) => request<R>({
    url: `api/v3/manager/point_info/device_id/${deviceId}/pointId/${pointId}`,
    method: "get"
}).then(res => res)

export const pointInfoByDeviceId = (deviceId: string) => request<R>({
    url: `api/v3/manager/point_info/device_id/${deviceId}`,
    method: "get"
}).then(res => res)
