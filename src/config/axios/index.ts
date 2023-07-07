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

import CommonConstant from '@/config/constant/common'
import { logout } from '@/utils/CommonUtils'
import { failMessage } from '@/utils/NotificationUtils'
import { getStorage } from '@/utils/StorageUtils'
import { isNull } from '@/utils/utils'
import { encode } from 'js-base64'

const request: AxiosInstance = axios.create({
    timeout: 15000,
    withCredentials: true,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    validateStatus: (status) => status >= 200 && status <= 500,
})

request.interceptors.request.use(
    (config) => {
        const headers = config.headers
        if (!headers) {
            return config
        }

        const tenant = getStorage(CommonConstant.TENANT_HEADER)
        if (!isNull(tenant)) {
            headers[CommonConstant.TENANT_HEADER] = encode(tenant)
        }

        const user = getStorage(CommonConstant.LOGIN_HEADER)
        if (!isNull(user)) {
            headers[CommonConstant.LOGIN_HEADER] = encode(user)
        }

        const token = getStorage(CommonConstant.TOKEN_HEADER)
        if (!isNull(token)) {
            headers[CommonConstant.TOKEN_HEADER] = encode(JSON.stringify(token))
        }

        return config
    },
    (error: any) => {
        return Promise.reject(error)
    }
)

request.interceptors.response.use(
    (response) => {
        const ok = response.data.ok || false
        const status = response.status || 401
        const responseType = response.config.responseType

        if (ok || responseType === 'blob') return response

        if (status === 401) {
            logout()
        } else {
            failMessage('接口请求异常，请联系系统管理员。', response.data.code, response.data)
        }
        return Promise.reject()
    },
    (error: any) => {
        return Promise.reject(error)
    }
)

export default request
