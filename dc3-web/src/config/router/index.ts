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

import type {RouteLocationNormalized, RouteMeta} from 'vue-router';
import {createRouter, createWebHashHistory} from 'vue-router';

import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import commonRouters from './common';
import operateRouters from './operate';
import settingsRouters from './settings';
import viewsRouters from './views';
import {getStorage, removeStorage} from '@/utils/storageUtil';
import {isNull} from '@/utils/validationUtil';
import {AUTH_HEADERS} from '@/config/constant/common';
import i18n from '@/config/i18n';
import {ElMessage} from 'element-plus';
import {type MenuNode, useMenuStore} from '@/store/modules/menu';

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

const ROUTE_MENU_ALIASES: Record<string, string> = {
  driverDetail: 'driver',
  deviceDetail: 'device',
  deviceEdit: 'device',
  profileDetail: 'profile',
  profileEdit: 'profile',
  pointDetail: 'pointValue',
  settingsUserDetail: 'settingsUser',
  settingsRoleDetail: 'settingsRole',
  settingsResourceDetail: 'settingsResource',
  settingsApiDetail: 'settingsApi',
  settingsMenuDetail: 'settingsMenu',
  settingsGroupDetail: 'settingsGroup',
  settingsLabelDetail: 'settingsLabel',
  settingsAlarmRuleDetail: 'settingsAlarmRule',
  settingsAlarmNotifyDetail: 'settingsAlarmNotify',
  settingsAlarmMessageDetail: 'settingsAlarmMessage',
  settingsAlarmChannelDetail: 'settingsAlarmChannel',
  settingsAlarmBindDetail: 'settingsAlarmBind',
  settingsAlarmStateDetail: 'settingsAlarmState',
  settingsAlarmHistoryDetail: 'settingsAlarmHistory',
  settingsModelConfigDetail: 'settingsModelConfig',
  settingsModelProviderDetail: 'settingsModelProvider',
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
    return {name: 'login'};
  }

  // Do not preflight every route with /token/check. A successful login has
  // already obtained a signed token; protected API calls are still enforced by
  // the gateway, and the Axios 401 interceptor clears stale credentials.
  if (isNull(tokenData?.salt) || isNull(tokenData?.token)) {
    removeStorage(AUTH_HEADERS.TENANT);
    removeStorage(AUTH_HEADERS.LOGIN);
    removeStorage(AUTH_HEADERS.TOKEN);
    return {name: 'login'};
  }

  // Permission check: public routes (login, errors, home) are always allowed.
  // For all other routes, the route name must appear as a menu_code in the
  // user's menu tree (which the server already scopes to the user's roles).
  const publicRoutes = ['login', '403', '404', '500', 'home'];
  const routeName = to.name as string;
  if (!publicRoutes.includes(routeName)) {
    const menuStore = useMenuStore();
    // Wait for the menu tree to load before checking permissions.
    // Without this, the guard would silently allow access to any route
    // while the tree is still being fetched.
    if (!menuStore.loaded) {
      await menuStore.fetchTree();
    }
    const menuRouteName = ROUTE_MENU_ALIASES[routeName] || routeName;
    if (!isRouteInMenuTree(menuRouteName, menuStore.tree)) {
      ElMessage.warning(i18n.global.t('error.forbidden'));
      return {name: 'home'};
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
