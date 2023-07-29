/*
 * Copyright 2016-present the original author or authors.
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

const Layout = () => import('@/components/layout/Layout.vue')
const routes: Array<RouteRecordRaw> = [
    {
        path: '/home',
        component: Layout,
        children: [
            {
                name: 'dashboard',
                path: '/home/dashboard',
                meta: {
                    icon: 'el-icon-s-home',
                    title: '看板',
                },
                component: () => import('@/views/dashboard/Dashboard.vue'),
            },
            {
                name: 'application',
                path: '/home/application',
                meta: {
                    icon: 'el-icon-s-home',
                    title: '应用',
                },
                component: () => import('@/views/application/Application.vue'),
            },
        ],
    },
    {
        path: '/driver',
        component: Layout,
        children: [
            {
                name: 'driverDetail',
                path: '/driver/detail',
                meta: {
                    icon: 'el-icon-s-promotion',
                    title: '驱动信息',
                },
                component: () => import('@/views/driver/detail/DriverDetail.vue'),
            },
        ],
    },
    {
        path: '/device',
        component: Layout,
        children: [
            {
                name: 'deviceDetail',
                path: '/device/detail',
                meta: {
                    icon: 'el-icon-s-finance',
                    title: '设备信息',
                },
                component: () => import('@/views/device/detail/DeviceDetail.vue'),
            },
            {
                name: 'deviceEdit',
                path: '/device/edit',
                meta: {
                    icon: 'el-icon-s-finance',
                    title: '设备编辑',
                },
                component: () => import('@/views/device/edit/DeviceEdit.vue'),
            },
        ],
    },
    {
        path: '/profile',
        component: Layout,
        children: [
            {
                name: 'profileDetail',
                path: '/profile/detail',
                meta: {
                    icon: 'el-icon-s-order',
                    title: '模板信息',
                },
                component: () => import('@/views/profile/detail/ProfileDetail.vue'),
            },
            {
                name: 'profileEdit',
                path: '/profile/edit',
                meta: {
                    icon: 'el-icon-s-order',
                    title: '模板编辑',
                },
                component: () => import('@/views/profile/edit/ProfileEdit.vue'),
            },
            {
                name: 'pointDetail',
                path: '/profile/point/detail',
                meta: {
                    icon: 'el-icon-s-order',
                    title: '位号信息',
                },
                component: () => import('@/views/point/detail/PointDetail.vue'),
            },
            {
                name: 'pointEdit',
                path: '/profile/point/edit',
                meta: {
                    icon: 'el-icon-s-order',
                    title: '位号编辑',
                },
                component: () => import('@/views/point/edit/PointEdit.vue'),
            },
        ],
    },
]

export default routes
