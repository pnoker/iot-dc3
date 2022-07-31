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

import md5 from 'js-md5'

import common from '@/util/common'
import { getStore, removeCookies, removeStore, setCookies, setStore } from '@/util/store'

import router from '@/config/router'
import { cancelTokenApi, generateSaltApi, generateTokenApi } from '@/api/token'

import { ElLoading } from 'element-plus'

const auth = {
    namespaced: true,
    state: {
        name: 'pnoker',
        tenant: 'default',
    },
    mutations: {
        setToken: (state, token) => {
            setCookies(common.TOKEN_HEADER, token)
            setStore(common.TOKEN_HEADER, token, false)

            state.name = token.name
            state.tenant = token.tenant
        },
        removeToken: () => {
            removeCookies(common.TOKEN_HEADER)
            removeStore(common.TOKEN_HEADER, false)
        },
    },
    actions: {
        login({ commit }, form) {
            const loading = ElLoading.service({
                lock: true,
                text: '登录中,请稍后...',
            })
            generateSaltApi(form.name)
                .then((res) => {
                    const salt = res.data.data
                    const login = {
                        tenant: form.tenant,
                        name: form.name,
                        salt: salt,
                        password: md5(md5(form.password) + salt),
                    }

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
        logout({ commit }) {
            const token = getStore(common.TOKEN_HEADER, false)
            if (token && token.name) {
                cancelTokenApi(token.name)
            }
            commit('removeToken')
        },
    },
}

export default auth
