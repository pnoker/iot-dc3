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

import { ElNotification } from 'element-plus'
import { isNull } from './utils'

/**
 * 成功操作
 *
 * @param message
 */
export const successMessage = (message?: string) => {
    if (isNull(message)) {
        message = '操作成功!'
    }

    ElNotification.success({
        title: '成功',
        message: message,
    })
}

/**
 * 失败操作
 *
 * @param message
 * @param error
 */
export const failMessage = (message?: string, error?: any) => {
    if (isNull(message)) {
        message = '操作失败!'
    }

    if (error) {
        console.error(error)
    }

    ElNotification.error({
        title: '错误',
        dangerouslyUseHTMLString: true,
        message: `${message}`,
    })
}
