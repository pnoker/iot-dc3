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

import type {MenuNode} from '@/store/modules/menu';

let seq = 0;

interface NodeOpts {
  url?: string;
  icon?: string;
  menuIndex?: number;
  children?: MenuNode[];
}

const mk = (menuCode: string, opts: NodeOpts = {}): MenuNode => ({
  id: ++seq,
  parentMenuId: 0,
  menuName: menuCode,
  menuCode,
  menuIndex: opts.menuIndex,
  // resolveMenuTitle treats content.title as an i18n key, so `nav.<code>`
  // localizes automatically for every route covered by Layout's nameMap /
  // SETTINGS_TITLE_KEYS.
  menuExt: {content: {title: `nav.${menuCode}`, url: opts.url, icon: opts.icon}},
  enableFlag: 'ENABLED',
  children: opts.children,
});

const leaf = (menuCode: string, url: string): MenuNode => mk(menuCode, {url});

/**
 * Full menu tree for the static demo. Covers every route name the router
 * guard checks (top-level views + all settings leaves). Detail/edit routes
 * resolve via ROUTE_MENU_ALIASES to their list sibling, so they need no node
 * of their own. Group structure mirrors src/config/settingsNav.ts.
 */
export const menuTree: MenuNode[] = [
  mk('home', {url: '/home', menuIndex: 1}),
  mk('driver', {url: '/driver', icon: 'Promotion', menuIndex: 2}),
  mk('profile', {url: '/profile', icon: 'List', menuIndex: 3}),
  mk('device', {url: '/device', icon: 'Management', menuIndex: 4}),
  mk('pointValue', {url: '/point_value', icon: 'Histogram', menuIndex: 5}),
  mk('settings', {
    icon: 'Setting',
    menuIndex: 6,
    children: [
      mk('settingsIdentity', {
        icon: 'User',
        children: [
          leaf('settingsUser', '/settings/user'),
          leaf('settingsPrincipal', '/settings/principal'),
          leaf('settingsTenantMembership', '/settings/tenant_membership'),
          leaf('settingsLocalCredential', '/settings/local_credential'),
          leaf('settingsServiceAccount', '/settings/service_account'),
        ],
      }),
      mk('settingsAccess', {
        icon: 'Stamp',
        children: [
          leaf('settingsRole', '/settings/role'),
          leaf('settingsRolePrincipalBind', '/settings/role_principal_bind'),
          leaf('settingsResource', '/settings/resource'),
          leaf('settingsApi', '/settings/api'),
          leaf('settingsMenu', '/settings/menu'),
        ],
      }),
      mk('settingsModel', {
        icon: 'Cpu',
        children: [
          leaf('settingsModelConfig', '/settings/model/config'),
          leaf('settingsModelProvider', '/settings/model/provider'),
        ],
      }),
      mk('settingsAlarm', {
        icon: 'AlarmClock',
        children: [
          leaf('settingsAlarmRule', '/settings/alarm/rule'),
          leaf('settingsAlarmNotify', '/settings/alarm/notify'),
          leaf('settingsAlarmMessage', '/settings/alarm/message'),
          leaf('settingsAlarmChannel', '/settings/alarm/channel'),
          leaf('settingsAlarmBind', '/settings/alarm/bind'),
          leaf('settingsAlarmOverview', '/settings/alarm/overview'),
          leaf('settingsAlarmState', '/settings/alarm/state'),
          leaf('settingsAlarmHistory', '/settings/alarm/history'),
          leaf('settingsDriverAlarm', '/settings/alarm/driver'),
          leaf('settingsDeviceAlarm', '/settings/alarm/device'),
          leaf('settingsPointAlarm', '/settings/alarm/point'),
        ],
      }),
      mk('settingsEventCommand', {
        icon: 'Operation',
        children: [
          leaf('settingsEventHistory', '/settings/event/history'),
          leaf('settingsCommandHistory', '/settings/command/history'),
        ],
      }),
      mk('settingsAudit', {
        icon: 'Files',
        children: [
          leaf('settingsIdentityAudit', '/settings/identity_audit'),
          leaf('settingsMcpAudit', '/settings/mcp_audit'),
        ],
      }),
      mk('settingsIntegration', {
        icon: 'Share',
        children: [
          leaf('settingsMcpServer', '/settings/mcp'),
          leaf('settingsMcpConnection', '/settings/mcp/connection'),
          leaf('settingsMcpClient', '/settings/mcp/client'),
          leaf('settingsMcpTool', '/settings/mcp/tool'),
        ],
      }),
      mk('settingsSystem', {
        icon: 'Tools',
        children: [
          leaf('settingsGroup', '/settings/group'),
          leaf('settingsLabel', '/settings/label'),
          leaf('settingsAbout', '/settings/about'),
        ],
      }),
    ],
  }),
];
