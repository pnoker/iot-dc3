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

/// <reference types="vite/client" />

/* eslint-disable */
declare module '*.vue' {
    import type { DefineComponent } from 'vue'
    const component: DefineComponent<{}, {}, any>
    export default component
}

/* eslint-disable */
import { Axios, AxiosResponseHeaders, InternalAxiosRequestConfig } from 'axios'

declare module 'axios' {
    interface AxiosResponse<T = any, D = any> {
        data: T
        status: number
        statusText: string
        headers: RawAxiosResponseHeaders | AxiosResponseHeaders
        config: InternalAxiosRequestConfig<D>
        request?: any
    }

    type MyAxiosPromise<T = any> = Promise<AxiosResponse<T>>

    interface AxiosInstance extends Axios {
        <T>(config: AxiosRequestConfig): MyAxiosPromise<T>

        <T>(url: string, config?: AxiosRequestConfig): MyAxiosPromise<T>
    }
}
