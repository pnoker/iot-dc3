/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 统一返回
 */
export interface R<T = any> {
    ok: boolean
    code: number
    message: string
    data: T
}

/**
 * 登录信息
 */
export interface LoginType {
    tenant: string
    name: string
    salt: string
    password: string
}

/**
 * 字典
 */
export interface Dictionary {
    type: string
    value: string
    disabled: boolean
    expand: boolean
    label: string
    children: Array<Dictionary>
}

/**
 * 排序
 */
export interface Order {
    column: string
    asc: boolean
}
