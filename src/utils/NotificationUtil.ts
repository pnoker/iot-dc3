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

import { ElNotification } from 'element-plus'
import { isNull } from './utils'

/**
 * 成功操作
 *
 * @param message 消息内容
 * @param title 消息标题
 */
export const successMessage = (message?: string, title: string = '成功') => {
    if (isNull(message)) {
        message = '操作成功!'
    }

    ElNotification({
        type: 'success',
        title: title,
        dangerouslyUseHTMLString: true,
        message: message,
    })
}

/**
 * 警告操作
 *
 * @param message 消息内容
 * @param title 消息标题
 * @param error 错误信息
 */
export const warnMessage = (message?: string, title = '警告', error?: any) => {
    if (isNull(message)) {
        message = '操作警告!'
    }

    if ('dev' === import.meta.env.MODE && error) {
        console.error(error)
    }

    ElNotification({
        type: 'warning',
        title: title,
        dangerouslyUseHTMLString: true,
        message: message,
    })
}

/**
 * 失败操作
 *
 * @param message 消息内容
 * @param title 消息标题
 * @param error 错误信息
 */
export const failMessage = (message?: string, title = '错误', error?: any) => {
    if (isNull(message)) {
        message = '操作失败!'
    }

    if ('dev' === import.meta.env.MODE && error) {
        console.error(error)
    }

    ElNotification({
        type: 'error',
        title: title,
        dangerouslyUseHTMLString: true,
        message: message,
    })
}
