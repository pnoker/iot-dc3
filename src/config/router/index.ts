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
import { ElMessage } from 'element-plus';
import { useMenuStore, type MenuNode } from '@/store/modules/menu';

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
 * Recursively search the menu tree for a node whose menu_code matches
 * the given route name, indicating the user has permission to access it.
 */
const isRouteInMenuTree = (routeName: string, nodes: MenuNode[]): boolean => {
  for (const node of nodes) {
    if (node.menuCode === routeName) return true;
    if (node.children && node.children.length > 0) {
      if (isRouteInMenuTree(routeName, node.children)) return true;
    }
  }
  return false;
};

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

  // Permission check: if the menu tree is loaded, verify the route is accessible.
  // Public routes (login, errors, home) are always allowed.
  // For all other routes, the route name must appear as a menu_code in the
  // user's menu tree (which the server already scopes to the user's roles).
  const publicRoutes = ['login', '403', '404', '500', 'home'];
  const routeName = to.name as string;
  if (!publicRoutes.includes(routeName)) {
    const menuStore = useMenuStore();
    if (menuStore.tree.length > 0 && !isRouteInMenuTree(routeName, menuStore.tree)) {
      ElMessage.warning('You do not have permission to access this page');
      return { name: 'home' };
    }
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
