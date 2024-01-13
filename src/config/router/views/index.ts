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

const Layout = () => import('@/components/layout/Layout.vue')
const routes: RouteRecordRaw = {
    path: '/',
    redirect: '/home',
    component: Layout,
    children: [
        {
            path: '/home',
            meta: {
                title: '首页',
            },
            component: () => import('@/views/home/Home.vue'),
        },
        {
            path: '/driver',
            meta: {
                icon: 'Promotion',
                title: '驱动',
            },
            component: () => import('@/views/driver/Driver.vue'),
        },
        {
            path: '/profile',
            meta: {
                icon: 'List',
                title: '模板',
            },
            component: () => import('@/views/profile/Profile.vue'),
        },
        {
            path: '/device',
            meta: {
                icon: 'Management',
                title: '设备',
            },
            component: () => import('@/views/device/Device.vue'),
        },
        {
            path: '/point_value',
            meta: {
                icon: 'Histogram',
                title: '数据',
            },
            component: () => import('@/views/point/value/PointValue.vue'),
        } /*
        {
            path: '/rule_engine',
            meta: {
                icon: 'Connection',
                title: '规则编排',
            },
            component: () => import('@/views/ruleengine/RuleEngine.vue'),
        }, */,
    ],
}

export default routes
