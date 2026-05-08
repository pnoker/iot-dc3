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

import type { RouteRecordRaw } from 'vue-router';

/**
 * Lazy-loaded layout component
 */
const Layout = () => import('@/components/layout/Layout.vue');

/**
 * Operate routes configuration
 * Includes detail and edit pages for various entities
 */
const routes: Array<RouteRecordRaw> = [
  {
    path: '/home',
    component: Layout,
    children: [
      {
        name: 'application',
        path: '/home/application',
        meta: {
          icon: 'el-icon-s-home',
          title: 'Application',
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
          title: 'Driver Detail',
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
          title: 'Device Detail',
        },
        component: () => import('@/views/device/detail/DeviceDetail.vue'),
      },
      {
        name: 'deviceEdit',
        path: '/device/edit',
        meta: {
          icon: 'el-icon-s-finance',
          title: 'Device Edit',
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
          title: 'Profile Detail',
        },
        component: () => import('@/views/profile/detail/ProfileDetail.vue'),
      },
      {
        name: 'profileEdit',
        path: '/profile/edit',
        meta: {
          icon: 'el-icon-s-order',
          title: 'Profile Edit',
        },
        component: () => import('@/views/profile/edit/ProfileEdit.vue'),
      },
    ],
  },
  {
    path: '/point',
    component: Layout,
    children: [
      {
        name: 'pointDetail',
        path: '/point/detail',
        meta: {
          icon: 'el-icon-s-order',
          title: 'Point Detail',
        },
        component: () => import('@/views/point/detail/PointDetail.vue'),
      },
      {
        name: 'pointEdit',
        path: '/point/edit',
        meta: {
          icon: 'el-icon-s-order',
          title: 'Point Edit',
        },
        component: () => import('@/views/point/edit/PointEdit.vue'),
      },
    ],
  },
  {
    path: '/settings/api',
    component: Layout,
    children: [
      {
        name: 'settingsApiDetail',
        path: '/settings/api/detail',
        meta: {
          icon: 'el-icon-link',
          title: 'API Detail',
        },
        component: () => import('@/views/settings/api/detail/ApiDetail.vue'),
      },
    ],
  },
  {
    path: '/settings/resource',
    component: Layout,
    children: [
      {
        name: 'settingsResourceDetail',
        path: '/settings/resource/detail',
        meta: {
          icon: 'el-icon-key',
          title: 'Resource Detail',
        },
        component: () => import('@/views/settings/resource/detail/ResourceDetail.vue'),
      },
    ],
  },
  {
    path: '/settings/menu',
    component: Layout,
    children: [
      {
        name: 'settingsMenuDetail',
        path: '/settings/menu/detail',
        meta: {
          icon: 'el-icon-menu',
          title: 'Menu Detail',
        },
        component: () => import('@/views/settings/menu/detail/MenuDetail.vue'),
      },
    ],
  },
  {
    path: '/settings/user',
    component: Layout,
    children: [
      {
        name: 'settingsUserDetail',
        path: '/settings/user/detail',
        meta: {
          icon: 'el-icon-user',
          title: 'User Detail',
        },
        component: () => import('@/views/settings/user/detail/UserDetail.vue'),
      },
    ],
  },
  {
    path: '/settings/role',
    component: Layout,
    children: [
      {
        name: 'settingsRoleDetail',
        path: '/settings/role/detail',
        meta: {
          icon: 'el-icon-user-filled',
          title: 'Role Detail',
        },
        component: () => import('@/views/settings/role/detail/RoleDetail.vue'),
      },
    ],
  },
];

export default routes;
