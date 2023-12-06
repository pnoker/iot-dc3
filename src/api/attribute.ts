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
 * 通过驱动ID查询驱动属性
 *
 * @param id 驱动ID
 * @returns MyAxiosPromise
 */
export const getDriverAttributeByDriverId = (id: string) =>
    request<R>({
        url: `api/v3/manager/driver_attribute/driver_id/${id}`,
        method: 'get',
    })

/**
 * 通过驱动ID查询位号属性
 * @param id 驱动ID
 * @returns MyAxiosPromise
 */
export const getPointAttributeByDriverId = (id: string) =>
    request<R>({
        url: `api/v3/manager/point_attribute/driver_id/${id}`,
        method: 'get',
    })
