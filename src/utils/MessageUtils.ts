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

import { ElMessage } from 'element-plus'

export const info = (message: string, grouping = true, showClose = true) => {
    ElMessage({
        type: 'info',
        grouping: grouping,
        showClose: showClose,
        message: message,
    })
}

export const success = (message: string, grouping = true, showClose = true) => {
    ElMessage({
        type: 'success',
        grouping: grouping,
        showClose: showClose,
        message: message,
    })
}

export const error = (message: string, grouping = true, showClose = true) => {
    ElMessage({
        type: 'error',
        grouping: grouping,
        showClose: showClose,
        message: message,
    })
}

export const warning = (message: string, grouping = true, showClose = true) => {
    ElMessage({
        type: 'warning',
        grouping: grouping,
        showClose: showClose,
        message: message,
    })
}
