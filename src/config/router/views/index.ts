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

import { RouteRecordRaw } from 'vue-router'

import Layout from '@/components/layout/Layout.vue'

const routes: RouteRecordRaw = {
    path: '/',
    redirect: '/home',
    component: Layout,
    children: [
        {
            name: 'home',
            path: '/home',
            meta: {
                title: '首页',
            },
            component: () => import('@/views/home/Home.vue'),
        },
        {
            name: 'driver',
            path: '/driver',
            meta: {
                icon: 'Promotion',
                title: '驱动',
            },
            component: () => import('@/views/driver/Driver.vue'),
        },
        {
            name: 'profile',
            path: '/profile',
            meta: {
                icon: 'List',
                title: '模板',
            },
            component: () => import('@/views/profile/ProfileList.vue'),
        },
        {
            name: 'device',
            path: '/device',
            meta: {
                icon: 'Management',
                title: '设备',
            },
            component: () => import('@/views/device/Device.vue'),
        },
        {
            name: 'pointValue',
            path: '/point_value',
            meta: {
                icon: 'Histogram',
                title: '数据',
            },
            component: () => import('@/views/point/value/PointValue.vue'),
        },
    ],
}

export default routes
