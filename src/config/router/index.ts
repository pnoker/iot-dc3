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

import { createRouter, createWebHashHistory } from 'vue-router';
import type { NavigationGuardNext, RouteLocationNormalized, RouteMeta } from 'vue-router';

import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import commonRouters from './common';
import operateRouters from './operate';
import settingsRouters from './settings';
import viewsRouters from './views';
import { getStorage } from '@/utils/StorageUtil';
import { isNull } from '@/utils/ValidationUtil';
import { AUTH_HEADERS } from '@/config/constant/common';
import { checkTokenValid } from '@/api/token';
import type { Login } from '@/config/entity';
import { logout } from '@/utils/CommonUtil';

/**
 * NProgress configuration
 */
const NPROGRESS_CONFIG = {
  easing: 'ease',
  showSpinner: false,
} as const;

/**
 * Configure NProgress
 */
NProgress.configure(NPROGRESS_CONFIG);

/**
 * Vue Router instance with authentication guards
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [...commonRouters, viewsRouters, settingsRouters, ...operateRouters],
});

/**
 * Navigation guard to handle authentication and page title
 */
router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
  NProgress.start();

  // Skip authentication check for login page
  if (from.name === 'login' || to.name === 'login') {
    next();
    return;
  }

  // Check if user is authenticated
  const tenant = getStorage(AUTH_HEADERS.TENANT);
  const user = getStorage(AUTH_HEADERS.LOGIN);
  const token = getStorage(AUTH_HEADERS.TOKEN);
  if (isNull(tenant) || isNull(user) || isNull(token)) {
    next({ path: '/login' });
    return;
  }

  // Validate token
  const login: Login = {
    tenant: tenant,
    name: user,
    ...token,
  };

  checkTokenValid(login)
    .then((res) => {
      if (!res.data) {
        next({ path: '/login' });
        logout();
        return;
      }

      // Update page title
      const meta: RouteMeta = to.meta || {};
      if (meta.title) {
        document.title = to.meta.title as string;
      }

      next();
    })
    .catch(() => {
      next({ path: '/login' });
      logout();
    });
});

/**
 * Navigation guard to handle progress bar completion
 */
router.afterEach(() => {
  NProgress.done();
});

/**
 * Error handler to remove progress bar on navigation errors
 */
router.onError(() => {
  NProgress.remove();
});

export default router;
