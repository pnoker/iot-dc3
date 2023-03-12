/*
 * Copyright 2022 Pnoker All Rights Reserved
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
import { Login } from '@/config/types'

/**
 * 获取 Salt
 *
 * @param login Login
 * @returns MyAxiosPromise
 */
export const generateSalt = (login: Login) =>
    request<R>({
        url: `api/v3/auth/token/salt`,
        method: 'post',
        data: login,
    })

/**
 * 登录
 *
 * @param login Login
 * @returns MyAxiosPromise
 */
export const generateToken = (login: Login) =>
    request<R>({
        url: `api/v3/auth/token/generate`,
        method: 'post',
        data: login,
    })

/**
 * 注销
 *
 * @param login Login
 * @returns MyAxiosPromise
 */
export const cancelToken = (login: Login) =>
    request<R>({
        url: `api/v3/auth/token/cancel`,
        method: 'post',
        data: login,
    })

/**
 * 校验 Token
 *
 * @param login Login
 * @returns MyAxiosPromise
 */
export const checkTokenValid = (login: Login) =>
    request<R>({
        url: `api/v3/auth/token/check`,
        method: 'post',
        data: login,
    })
