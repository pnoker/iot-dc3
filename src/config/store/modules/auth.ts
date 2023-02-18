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

import router from '@/config/router'
import { ElLoading } from 'element-plus'

import { cancelTokenApi, generateSaltApi, generateTokenApi } from '@/api/token'

import { Login } from '@/config/type/types'
import CommonConstant from '@/config/constant/CommonConstant'
import { getStorage, removeStorage, setStorage } from '@/utils/StorageUtils'
import { isNull } from '@/utils/utils'
import { Md5 } from 'ts-md5'

const auth = {
    namespaced: true,
    state: {
        tenant: 'default',
        name: 'pnoker',
    },
    mutations: {
        setToken: (state: any, login: any) => {
            setStorage(CommonConstant.TENANT_HEADER, login.tenant)
            setStorage(CommonConstant.USER_HEADER, login.name)
            setStorage(CommonConstant.TOKEN_HEADER, { salt: login.salt, token: login.token })

            state.tenant = login.tenant
            state.name = login.name
        },
        removeToken: () => {
            removeStorage(CommonConstant.TENANT_HEADER)
            removeStorage(CommonConstant.USER_HEADER)
            removeStorage(CommonConstant.TOKEN_HEADER)
        },
    },
    actions: {
        login({ commit }: any, form: any) {
            const loading = ElLoading.service({
                lock: true,
                text: '登录中,请稍后...',
            })
            const login = {
                tenant: form.tenant,
                name: form.name,
            } as Login
            generateSaltApi(login)
                .then((res) => {
                    const salt = res.data.data
                    const login = {
                        tenant: form.tenant,
                        name: form.name,
                        salt: salt,
                        password: Md5.hashStr(Md5.hashStr(form.password) + salt),
                    } as Login

                    generateTokenApi(login)
                        .then((res) => {
                            commit('setToken', {
                                tenant: login.tenant,
                                name: login.name,
                                salt: login.salt,
                                token: res.data.data,
                            })
                            router.push({ path: '/' }).then(() => loading.close())
                        })
                        .catch(() => loading.close())
                })
                .catch(() => loading.close())
        },
        logout({ commit }: any) {
            const tenant = getStorage(CommonConstant.TENANT_HEADER)
            const user = getStorage(CommonConstant.USER_HEADER)
            if (!isNull(tenant) && !isNull(user)) {
                const login = {
                    tenant: tenant,
                    name: user,
                } as Login
                cancelTokenApi(login)
            }
            commit('removeToken')
        },
    },
}

export default auth
