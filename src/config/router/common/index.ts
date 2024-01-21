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

import { RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
    {
        name: 'login',
        path: '/login',
        meta: {
            title: 'IoT DC3 Web',
        },
        component: () => import('@/views/login/Login.vue'),
    },
    {
        name: '403',
        path: '/403',
        meta: {
            title: '403',
        },
        component: () => import('@/components/error/403.vue'),
    },
    {
        name: '404',
        path: '/404',
        meta: {
            title: '404',
        },
        component: () => import('@/components/error/404.vue'),
    },
    {
        name: '500',
        path: '/500',
        meta: {
            title: '500',
        },
        component: () => import('@/components/error/500.vue'),
    },
    {
        path: '/:catchAll(.*)',
        redirect: '/404',
    },
]

export default routes
