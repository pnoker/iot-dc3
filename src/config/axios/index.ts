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

import axios, { AxiosInstance } from 'axios'

import router from '@/config/router'
import store from '@/config/store'

import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

import CommonConstant from '@/config/constant/common'
import { warning } from '@/utils/MessageUtils'
import { getStorage } from '@/utils/StorageUtils'
import { isNull } from '@/utils/utils'

import { encode } from 'js-base64'

NProgress.configure({
    easing: 'ease',
    showSpinner: false,
})

const noAuthMessage = '检测到您未登录或登陆凭证已失效，请重新登录'

const request: AxiosInstance = axios.create({
    timeout: 15000,
    withCredentials: true,
    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
    },
    validateStatus(status): boolean {
        return status >= 200 && status <= 500
    },
})

request.interceptors.request.use(
    (config) => {
        NProgress.start()

        const tenant = getStorage(CommonConstant.TENANT_HEADER)
        const user = getStorage(CommonConstant.USER_HEADER)
        const token = getStorage(CommonConstant.TOKEN_HEADER)
        if (!isNull(tenant) && !isNull(user) && !isNull(token)) {
            const headers = config.headers
            if (headers) {
                headers[CommonConstant.TENANT_HEADER] = encode(tenant)
                headers[CommonConstant.USER_HEADER] = encode(user)
                headers[CommonConstant.TOKEN_HEADER] = encode(JSON.stringify(token))
            }
        }

        return config
    },
    (error: any) => {
        NProgress.done()
        return Promise.reject(error)
    }
)

request.interceptors.response.use(
    (response) => {
        NProgress.done()

        const ok = response.data.ok || false
        const status = response.status || 401

        if (!ok && status === 401) {
            warning(noAuthMessage)
            store.dispatch('auth/logout').then(() => router.push({ path: '/login' }))
            return Promise.reject(noAuthMessage)
        }

        return response
    },
    (error: any) => {
        NProgress.done()

        if (!axios.isCancel(error)) {
            console.log('Response error:', error)
        }

        return Promise.reject(error)
    }
)

export default request
