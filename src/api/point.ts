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

// point
export const pointAdd = (point: any) => request<R>({
    url: `api/v3/manager/point/add`,
    method: "post",
    data: point
}).then(res => res)

export const pointDelete = (id: string) => request<R>({
    url: `api/v3/manager/point/delete/${id}`,
    method: "post"
}).then(res => res)

export const pointUpdate = (point: any) => request<R>({
    url: `api/v3/manager/point/update`,
    method: "post",
    data: point
}).then(res => res)

export const pointById = (id: string) => request<R>({
    url: `api/v3/manager/point/id/${id}`,
    method: "get"
}).then(res => res)

export const pointList = (point: any) => request<R>({
    url: `api/v3/manager/point/list`,
    method: "post",
    data: point
}).then(res => res)

export const pointUnit = (pointIds: any) => request<R>({
    url: `api/v3/manager/point/unit`,
    method: "post",
    data: pointIds
}).then(res => res)

export const pointByProfileId = (profileId: string) => request<R>({
    url: `api/v3/manager/point/profile_id/${profileId}`,
    method: "get"
}).then(res => res)

export const pointByDeviceId = (deviceId: string) => request<R>({
    url: `api/v3/manager/point/device_id/${deviceId}`,
    method: "get"
}).then(res => res)

// point value
export const pointValueByDeviceId = (deviceId: string, history: any) => request<R>({
    url: `api/v3/data/point_value/latest/device_id/${deviceId}`,
    method: "get",
    params: {history}
}).then(res => res)

export const pointValueList = (pointValue: any) => request<R>({
    url: `api/v3/data/point_value/list`,
    method: "post",
    data: pointValue
}).then(res => res)
