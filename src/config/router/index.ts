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

import type { RouteLocationNormalized, RouteMeta } from 'vue-router';
import { createRouter, createWebHashHistory } from 'vue-router';

import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import commonRouters from './common';
import operateRouters from './operate';
import settingsRouters from './settings';
import viewsRouters from './views';
import { checkTokenValid } from '@/api/token';
import type { Login } from '@/config/types';
import { getStorage, removeStorage } from '@/utils/storageUtil';
import { isNull } from '@/utils/validationUtil';
import { AUTH_HEADERS } from '@/config/constant/common';

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
 * Navigation guard to handle authentication and page title.
 * Vue Router 4 deprecated the next() callback — we return the target
 * (or true/false) directly instead.
 */
router.beforeEach(async (to: RouteLocationNormalized) => {
  NProgress.start();

  // Skip authentication check for login page
  if (to.name === 'login') {
    return true;
  }

  // Check if user is authenticated locally
  const tenant = getStorage(AUTH_HEADERS.TENANT) as string | undefined;
  const user = getStorage(AUTH_HEADERS.LOGIN) as string | undefined;
  const tokenData = getStorage(AUTH_HEADERS.TOKEN) as { salt?: string; token?: string } | undefined;

  if (isNull(tenant) || isNull(user) || isNull(tokenData)) {
    return { name: 'login' };
  }

  // Validate token with server
  try {
    const login: Login = {
      tenant,
      name: user,
      salt: tokenData!.salt,
      token: tokenData!.token,
    };
    const validRes = await checkTokenValid(login);
    if (!validRes?.data) {
      removeStorage(AUTH_HEADERS.TENANT);
      removeStorage(AUTH_HEADERS.LOGIN);
      removeStorage(AUTH_HEADERS.TOKEN);
      return { name: 'login' };
    }
  } catch {
    removeStorage(AUTH_HEADERS.TENANT);
    removeStorage(AUTH_HEADERS.LOGIN);
    removeStorage(AUTH_HEADERS.TOKEN);
    return { name: 'login' };
  }

  // Update page title
  const meta: RouteMeta = to.meta || {};
  if (meta.title) {
    document.title = meta.title as string;
  }

  return true;
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
