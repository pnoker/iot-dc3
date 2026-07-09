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
      path: '/home',
      meta: {
        title: 'Home',
      },
      component: () => import('@/views/home/Home.vue'),
    },
    {
      name: 'driver',
      path: '/driver',
      meta: {
        icon: 'Promotion',
        title: 'Driver',
      },
      component: () => import('@/views/driver/Driver.vue'),
    },
    {
      name: 'profile',
      path: '/profile',
      meta: {
        icon: 'List',
        title: 'Profile',
      },
      component: () => import('@/views/profile/Profile.vue'),
    },
    {
      name: 'device',
      path: '/device',
      meta: {
        icon: 'Management',
        title: 'Device',
      },
      component: () => import('@/views/device/Device.vue'),
    },
    {
      name: 'pointValue',
      path: '/point_value',
      meta: {
        icon: 'Histogram',
        title: 'Data',
      },
      component: () => import('@/views/point/value/PointValue.vue'),
    },
  ],
};

export default routes;
