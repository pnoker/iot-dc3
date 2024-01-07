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
import { ElLoading } from 'element-plus'

import { cancelToken, generateSalt, generateToken } from '@/api/token'

import CommonConstant from '@/config/constant/common'
import { Login } from '@/config/types'
import { getStorage, removeStorage, setStorage } from '@/utils/StorageUtils'
import { isNull } from '@/utils/utils'
import { Md5 } from 'ts-md5'

const auth = {
    namespaced: true,
    state: {
        tenant: 'default',
        name: 'pnoker',
    },
    getters: {
        getTenant: () => {
            return getStorage(CommonConstant.X_AUTH_TENANT)
        },
        getName: () => {
            return getStorage(CommonConstant.X_AUTH_LOGIN)
        },
    },
    mutations: {
        setToken: (state: any, login: any) => {
            setStorage(CommonConstant.X_AUTH_TENANT, login.tenant)
            setStorage(CommonConstant.X_AUTH_LOGIN, login.name)
            setStorage(CommonConstant.X_AUTH_TOKEN, { salt: login.salt, token: login.token })

            state.tenant = login.tenant
            state.name = login.name
        },
        removeToken: () => {
            removeStorage(CommonConstant.X_AUTH_TENANT)
            removeStorage(CommonConstant.X_AUTH_LOGIN)
            removeStorage(CommonConstant.X_AUTH_TOKEN)
        },
    },
    actions: {
        login({ commit }: any, form: any) {
            const loading = ElLoading.service({
                lock: true,
                text: '登录中,请稍后...',
            })
            const login: Login = {
                tenant: form.tenant,
                name: form.name,
            }
            generateSalt(login)
                .then((res) => {
                    const salt: string = res.data
                    const login: Login = {
                        tenant: form.tenant,
                        name: form.name,
                        salt: salt,
                        password: Md5.hashStr(Md5.hashStr(form.password) + salt),
                    }

                    generateToken(login)
                        .then((res) => {
                            commit('setToken', {
                                tenant: login.tenant,
                                name: login.name,
                                salt: login.salt,
                                token: res.data,
                            })
                            router.push({ path: '/' }).then(() => loading.close())
                        })
                        .catch(() => loading.close())
                })
                .catch(() => loading.close())
        },
        logout({ commit, getters }: any) {
            const tenant = getters.getTenant
            const user = getters.getName
            if (!isNull(tenant) && !isNull(user)) {
                const login = {
                    tenant: tenant,
                    name: user,
                } as Login
                cancelToken(login)
            }
            commit('removeToken')
        },
    },
}

export default auth
