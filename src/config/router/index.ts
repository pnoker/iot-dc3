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

import { createRouter, createWebHashHistory, NavigationGuardNext, RouteLocationNormalized } from 'vue-router'

import commonRouters from './common'
import operateRouters from './operate'
import viewsRouters from './views'

const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL),
    routes: [...commonRouters, viewsRouters, ...operateRouters],
})

router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    const meta = to.meta || {}
    if (meta.title) {
        document.title = to.meta.title as string
    }

    next()
})

export default router
