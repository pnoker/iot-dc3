/*
 * Copyright 2016-present the original author or authors.
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
import JSONBigInt from 'json-bigint'

const JSONBigIntStr = JSONBigInt({ storeAsString: true })
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

        const tenant = getStorage(CommonConstant.X_AUTH_TENANT)
        if (!isNull(tenant)) {
            headers[CommonConstant.X_AUTH_TENANT] = encode(tenant)
        }

        const login = getStorage(CommonConstant.X_AUTH_LOGIN)
        if (!isNull(login)) {
            headers[CommonConstant.X_AUTH_LOGIN] = encode(login)
        }

        const token = getStorage(CommonConstant.X_AUTH_TOKEN)
        if (!isNull(token)) {
            headers[CommonConstant.X_AUTH_TOKEN] = encode(JSON.stringify(token))
        }

        return config
    },
    (error: any) => {
        return Promise.reject(error)
    },
)

request.interceptors.response.use(
    (response) => {
        const ok = response.data.ok || false
        const status = response.status || 401
        const responseType = response.config.responseType

        if (ok || responseType === 'blob') {
            return JSONBigIntStr.parse(response)
        }

        if (status === 401) {
            logout()
        } else {
            failMessage('接口请求异常，请联系系统管理员。', response.data.code, response.data)
        }
        return Promise.reject()
    },
    (error: any) => {
        return Promise.reject(error)
    },
)

export default request
