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

import { createRouter, createWebHashHistory, NavigationGuardNext, RouteLocationNormalized, RouteMeta } from 'vue-router'

import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import commonRouters from './common'
import operateRouters from './operate'
import viewsRouters from './views'
import { getStorage } from '@/utils/StorageUtil'
import { isNull } from '@/utils/utils'
import CommonConstant from '@/config/constant/common'
import { checkTokenValid } from '@/api/token'
import { Login } from '@/config/entity'
import { logout } from '@/utils/CommonUtil'

NProgress.configure({
    easing: 'ease',
    showSpinner: false
})

const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL),
    routes: [...commonRouters, viewsRouters, ...operateRouters]
})

router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    NProgress.start()

    if (from.name === 'login' || to.name === 'login') {
        next()
        return
    }

    const tenant = getStorage(CommonConstant.X_AUTH_TENANT)
    const user = getStorage(CommonConstant.X_AUTH_LOGIN)
    const token = getStorage(CommonConstant.X_AUTH_TOKEN)
    if (isNull(tenant) || isNull(user) || isNull(token)) {
        next({ path: '/login' })
        return
    }

    const login: Login = {
        tenant: tenant,
        name: user,
        ...token
    }

    checkTokenValid(login)
        .then(res => {
            if (!res.data) {
                logout()
                return
            }

            const meta: RouteMeta = to.meta || {}
            if (meta.title) {
                document.title = to.meta.title as string
            }

            next()
        })
        .catch(() => {
            logout()
        })
})

router.afterEach(() => {
    NProgress.done()
})

router.onError(() => {
    NProgress.remove()
})

export default router
