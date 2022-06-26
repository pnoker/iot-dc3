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

import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponseHeaders } from "axios"

import router from "@/config/router"
import store from "@/config/store"

import NProgress from "nprogress"
import "nprogress/nprogress.css"
import { ElMessage } from "element-plus"
import { getStore } from "@/util/store";
import common from "@/util/common";

declare module "axios" {
    interface AxiosResponse<T = any, D = any> {
        data: T
        status: number
        statusText: string
        headers: AxiosResponseHeaders
        config: AxiosRequestConfig<D>
        request?: any
    }

    interface AxiosPromise<T = any> extends Promise<AxiosResponse<T>> {
    }

    interface AxiosInstance extends Axios {
        <T>(config: AxiosRequestConfig): AxiosPromise<T>

        <T>(url: string, config?: AxiosRequestConfig): AxiosPromise<T>
    }
}

NProgress.configure({
    easing: "ease",
    showSpinner: false
})

const request: AxiosInstance = axios.create({
    timeout: 15000,
    withCredentials: true,
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-Auth-Token": getStore(common.TOKEN_HEADER, false)
    },
    validateStatus(status): boolean {
        return status >= 200 && status <= 500
    }
})

request.interceptors.request.use((config: AxiosRequestConfig) => {
        NProgress.start()

        return config
    },
    (error: any) => {
        const {response} = error
        if (response) {
            return Promise.reject(response.data)
        } else {
            ElMessage({
                type: "warning",
                grouping: true,
                showClose: true,
                message: "网络连接异常,请稍后再试",
            })
        }
    })

request.interceptors.response.use((response) => {
        const ok = response.data.ok || false
        const status = response.status || 200
        const message = response.data.message || "网络连接异常,请稍后再试"

        if (!ok) {
            if (status === 401) {
                ElMessage({
                    type: "warning",
                    grouping: true,
                    showClose: true,
                    message: "未登录或登陆凭证已失效，请重新登录",
                })
                store.dispatch("clearToken").then(() => router.push({path: "/login"}))
            }
            return Promise.reject(new Error(message))
        }

        NProgress.done()
        return response
    },
    (error: any) => {
        ElMessage({
            type: "error",
            grouping: true,
            showClose: true,
            message: error.message,
        })

        NProgress.done()
        return Promise.reject(error)
    })

export default request
