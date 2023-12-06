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
 * 查询驱动字典
 *
 * @param dictionary Dictionary
 * @returns MyAxiosPromise
 */
export const getDriverDictionary = (dictionary: any) =>
    request<R>({
        url: `api/v3/manager/dictionary/driver`,
        method: 'post',
        data: dictionary,
    })

/**
 * 查询设备字典
 *
 * @param dictionary Dictionary
 * @returns MyAxiosPromise
 */
export const getDeviceDictionary = (dictionary: any) =>
    request<R>({
        url: `api/v3/manager/dictionary/device`,
        method: 'post',
        data: dictionary,
    })

/**
 * 查询模板字典
 *
 * @param dictionary Dictionary
 * @returns MyAxiosPromise
 */
export const getProfileDictionary = (dictionary: any) =>
    request<R>({
        url: `api/v3/manager/dictionary/profile`,
        method: 'post',
        data: dictionary,
    })

/**
 * 查询位号字典
 *
 * @param dictionary Dictionary
 * @returns MyAxiosPromise
 */
export const getPointDictionary = (dictionary: any) =>
    request<R>({
        url: `api/v3/manager/dictionary/point`,
        method: 'post',
        data: dictionary,
    })
