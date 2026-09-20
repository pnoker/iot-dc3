/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import type {RouteRecordRaw} from 'vue-router';

/**
 * Lazy-loaded layout component
 * @returns the lazy-loaded layout component
 */
const Layout = () => import('@/components/layout/Layout.vue');

/**
 * Main view routes configuration
 * Includes navigation menu items
 */
const routes: RouteRecordRaw = {
  path: '/',
  redirect: '/home',
  component: Layout,
  children: [
    {
      name: 'home',
      path: 'home',
      meta: {
        title: 'nav.home',
      },
      component: () => import('@/views/home/Home.vue'),
    },
    {
      name: 'driver',
      path: 'driver',
      meta: {
        title: 'nav.driver',
      },
      component: () => import('@/views/driver/Driver.vue'),
    },
    {
      name: 'driverDetail',
      path: 'driver/detail',
      meta: {
        title: 'nav.driverDetail',
      },
      component: () => import('@/views/driver/detail/DriverDetail.vue'),
    },
    {
      name: 'profile',
      path: 'profile',
      meta: {
        title: 'nav.profile',
      },
      component: () => import('@/views/profile/Profile.vue'),
    },
    {
      name: 'profileDetail',
      path: 'profile/detail',
      meta: {
        title: 'nav.profileDetail',
      },
      component: () => import('@/views/profile/detail/ProfileDetail.vue'),
    },
    {
      name: 'profileEdit',
      path: 'profile/edit',
      meta: {
        title: 'nav.profileEdit',
      },
      component: () => import('@/views/profile/edit/ProfileEdit.vue'),
    },
    {
      name: 'device',
      path: 'device',
      meta: {
        title: 'nav.device',
      },
      component: () => import('@/views/device/Device.vue'),
    },
    {
      name: 'deviceDetail',
      path: 'device/detail',
      meta: {
        title: 'nav.deviceDetail',
      },
      component: () => import('@/views/device/detail/DeviceDetail.vue'),
    },
    {
      name: 'deviceEdit',
      path: 'device/edit',
      meta: {
        title: 'nav.deviceEdit',
      },
      component: () => import('@/views/device/edit/DeviceEdit.vue'),
    },
    {
      name: 'pointValue',
      path: 'point_value',
      meta: {
        title: 'page.pointValue',
      },
      component: () => import('@/views/point/value/PointValue.vue'),
    },
    {
      name: 'pointDetail',
      path: 'point/detail',
      meta: {
        title: 'nav.pointDetail',
      },
      component: () => import('@/views/point/detail/PointDetail.vue'),
    },
  ],
};

export default routes;
