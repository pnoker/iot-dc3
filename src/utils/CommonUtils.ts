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

import router from '@/config/router'
import store from '@/store'

import { warnMessage } from '@/utils/NotificationUtils'
import { dateFormat, setCopyContent } from '@/utils/utils'

export const copy = (content: string, message: string) => {
    setCopyContent(content, true, message)
}

export const timestamp = (timestamp: string): string => {
    return dateFormat(new Date(timestamp))
}

export const logout = (withWarn: boolean = false) => {
    if (withWarn) {
        warnMessage('检测到您未登录或登陆凭证已失效，请重新登录!', '登录凭证失效')
    }
    store.dispatch('auth/logout').then(() => router.push({ path: '/login' }))
}
