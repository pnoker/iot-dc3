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
 * Common routes configuration
 * Includes login page and error pages
 */
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
      title: '403 Forbidden',
    },
    component: () => import('@/components/error/403.vue'),
  },
  {
    name: '404',
    path: '/404',
    meta: {
      title: '404 Not Found',
    },
    component: () => import('@/components/error/404.vue'),
  },
  {
    name: '500',
    path: '/500',
    meta: {
      title: '500 Server Error',
    },
    component: () => import('@/components/error/500.vue'),
  },
  {
    path: '/:catchAll(.*)',
    redirect: '/404',
  },
];

export default routes;
