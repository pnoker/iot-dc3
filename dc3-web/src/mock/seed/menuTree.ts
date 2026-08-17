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
  menuName?: string;
  titles?: { zh: string; en: string };
  url?: string;
  icon?: string;
  menuIndex?: number;
  children?: MenuNode[];
}

/**
 * Build a menu node. Titles mirror the dc3_menu seed rows in
 * dependencies/postgres/initdb/02-iot-dc3-auth.sql (authoritative {zh,en} map) so the
 * static demo matches what the backend serves; resolveMenuTitle prefers content.titles.
 */
const mk = (menuCode: string, opts: NodeOpts = {}): MenuNode => ({
  id: String(++seq),
  parentMenuId: '0',
  menuName: opts.menuName ?? menuCode,
  menuCode,
  menuIndex: opts.menuIndex,
  menuExt: {content: {titles: opts.titles, url: opts.url, icon: opts.icon}},
  enableFlag: 'ENABLED',
  children: opts.children,
});

/**
 * Full menu tree for the static demo, aligned to the dc3_menu seed. Covers every route
 * the router guard checks; detail/edit routes resolve via ROUTE_MENU_ALIASES to their
 * list sibling. Group structure mirrors src/config/settingsNav.ts.
 */
export const menuTree: MenuNode[] = [
  mk('home', {menuName: 'Home', titles: {zh: '首页', en: 'Home'}, icon: 'HomeFilled', url: '/home', menuIndex: 1}),
  mk('driver', {
    menuName: 'Driver',
    titles: {zh: '驱动管理', en: 'Driver'},
    icon: 'Promotion',
    url: '/driver',
    menuIndex: 2
  }),
  mk('profile', {
    menuName: 'Profile',
    titles: {zh: '模板管理', en: 'Profile'},
    icon: 'List',
    url: '/profile',
    menuIndex: 3
  }),
  mk('device', {
    menuName: 'Device',
    titles: {zh: '设备管理', en: 'Device'},
    icon: 'Management',
    url: '/device',
    menuIndex: 4
  }),
  mk('pointValue', {
    menuName: 'PointValue',
    titles: {zh: '位号数据', en: 'PointValue'},
    icon: 'TrendCharts',
    url: '/point_value',
    menuIndex: 5
  }),
  mk('settings', {
    menuName: 'Settings', titles: {zh: '设置', en: 'Settings'}, icon: 'Setting', menuIndex: 6, children: [
      mk('settingsIdentity', {
        menuName: 'Accounts & Identity', titles: {zh: '账户与身份', en: 'Accounts & Identity'}, icon: 'User', menuIndex: 1, children: [
          mk('settingsUser', {
            menuName: 'User',
            titles: {zh: '用户管理', en: 'User'},
            icon: 'User',
            url: '/settings/user',
            menuIndex: 1
          }),
          mk('settingsPrincipal', {
            menuName: 'Identity Principals',
            titles: {zh: '身份主体', en: 'Identity Principals'},
            icon: 'Avatar',
            url: '/settings/principal',
            menuIndex: 2
          }),
          mk('settingsTenantMembership', {
            menuName: 'Tenant Membership',
            titles: {zh: '租户成员', en: 'Tenant Membership'},
            icon: 'OfficeBuilding',
            url: '/settings/tenant_membership',
            menuIndex: 3
          }),
          mk('settingsLocalCredential', {
            menuName: 'Local Credentials',
            titles: {zh: '本地凭证', en: 'Local Credentials'},
            icon: 'Lock',
            url: '/settings/local_credential',
            menuIndex: 4
          }),
          mk('settingsServiceAccount', {
            menuName: 'Service Accounts',
            titles: {zh: '服务账号', en: 'Service Accounts'},
            icon: 'Key',
            url: '/settings/service_account',
            menuIndex: 5
          })
        ]
      }),
      mk('settingsAccess', {
        menuName: 'Roles & Permissions', titles: {zh: '角色与权限', en: 'Roles & Permissions'}, icon: 'Stamp', menuIndex: 2, children: [
          mk('settingsRole', {
            menuName: 'Role',
            titles: {zh: '角色管理', en: 'Role'},
            icon: 'Lock',
            url: '/settings/role',
            menuIndex: 1
          }),
          mk('settingsRolePrincipalBind', {
            menuName: 'Role Assignments',
            titles: {zh: '角色分配', en: 'Role Assignments'},
            icon: 'Link',
            url: '/settings/role_principal_bind',
            menuIndex: 2
          }),
          mk('settingsResource', {
            menuName: 'Resource',
            titles: {zh: '资源管理', en: 'Resource'},
            icon: 'Tickets',
            url: '/settings/resource',
            menuIndex: 3
          }),
          mk('settingsApi', {
            menuName: 'API',
            titles: {zh: 'API 接口', en: 'API'},
            icon: 'Connection',
            url: '/settings/api',
            menuIndex: 4
          }),
          mk('settingsMenu', {
            menuName: 'Menu',
            titles: {zh: '菜单管理', en: 'Menu'},
            icon: 'Discount',
            url: '/settings/menu',
            menuIndex: 5
          })
        ]
      }),
      mk('settingsSystem', {
        menuName: 'Metadata', titles: {zh: '元数据', en: 'Metadata'}, icon: 'Collection', menuIndex: 3, children: [
          mk('settingsGroup', {
            menuName: 'Group',
            titles: {zh: '分组管理', en: 'Groups'},
            icon: 'Grid',
            url: '/settings/group',
            menuIndex: 1
          }),
          mk('settingsLabel', {
            menuName: 'Label',
            titles: {zh: '标签管理', en: 'Labels'},
            icon: 'CollectionTag',
            url: '/settings/label',
            menuIndex: 2
          })
        ]
      }),
      mk('settingsModel', {
        menuName: 'AI Models', titles: {zh: 'AI 模型', en: 'AI Models'}, icon: 'Cpu', menuIndex: 4, children: [
          mk('settingsModelProvider', {
            menuName: 'Model Providers',
            titles: {zh: '模型供应商', en: 'Model Providers'},
            icon: 'ChatLineSquare',
            url: '/settings/model/provider',
            menuIndex: 1
          }),
          mk('settingsModelConfig', {
            menuName: 'Model Config',
            titles: {zh: '模型配置', en: 'Model Config'},
            icon: 'ChatDotRound',
            url: '/settings/model/config',
            menuIndex: 2
          })
        ]
      }),
      mk('settingsIntegration', {
        menuName: 'MCP Integration', titles: {zh: 'MCP 集成', en: 'MCP Integration'}, icon: 'Share', menuIndex: 5, children: [
          mk('settingsMcpServer', {
            menuName: 'MCP Service',
            titles: {zh: 'MCP 服务', en: 'MCP Service'},
            icon: 'Connection',
            url: '/settings/mcp',
            menuIndex: 1
          }),
          mk('settingsMcpClient', {
            menuName: 'MCP Client',
            titles: {zh: 'MCP 客户端', en: 'MCP Client'},
            icon: 'Ticket',
            url: '/settings/mcp/client',
            menuIndex: 2
          }),
          mk('settingsMcpConnection', {
            menuName: 'MCP Connection',
            titles: {zh: 'MCP 连接', en: 'MCP Connection'},
            icon: 'Link',
            url: '/settings/mcp/connection',
            menuIndex: 3
          }),
          mk('settingsMcpTool', {
            menuName: 'MCP Tool',
            titles: {zh: 'MCP 工具', en: 'MCP Tool'},
            icon: 'Tools',
            url: '/settings/mcp/tool',
            menuIndex: 4
          })
        ]
      }),
      mk('settingsAlarm', {
        menuName: 'Alarm Configuration', titles: {zh: '告警配置', en: 'Alarm Configuration'}, icon: 'AlarmClock', menuIndex: 6, children: [
          mk('settingsAlarmRule', {
            menuName: 'Alarm Rules',
            titles: {zh: '告警规则', en: 'Alarm Rules'},
            icon: 'SetUp',
            url: '/settings/alarm/rule',
            menuIndex: 1
          }),
          mk('settingsAlarmNotify', {
            menuName: 'Notification Policies',
            titles: {zh: '告警通知策略', en: 'Notification Policies'},
            icon: 'Bell',
            url: '/settings/alarm/notify',
            menuIndex: 2
          }),
          mk('settingsAlarmMessage', {
            menuName: 'Message Templates',
            titles: {zh: '告警消息模板', en: 'Message Templates'},
            icon: 'Message',
            url: '/settings/alarm/message',
            menuIndex: 3
          }),
          mk('settingsAlarmChannel', {
            menuName: 'Alarm Channels',
            titles: {zh: '告警通知渠道', en: 'Alarm Channels'},
            icon: 'Connection',
            url: '/settings/alarm/channel',
            menuIndex: 4
          }),
          mk('settingsAlarmBind', {
            menuName: 'Alarm Bindings',
            titles: {zh: '告警渠道绑定', en: 'Alarm Bindings'},
            icon: 'Link',
            url: '/settings/alarm/bind',
            menuIndex: 5
          })
        ]
      }),
      mk('settingsEventCommand', {
        menuName: 'Operations & History',
        titles: {zh: '运行与历史', en: 'Operations & History'},
        icon: 'Operation',
        menuIndex: 7,
        children: [
          mk('settingsAlarmOverview', {
            menuName: 'Overview',
            titles: {zh: '概览', en: 'Overview'},
            icon: 'DataAnalysis',
            url: '/settings/alarm/overview',
            menuIndex: 1
          }),
          mk('settingsAlarmState', {
            menuName: 'Alarm States',
            titles: {zh: '告警运行状态', en: 'Alarm States'},
            icon: 'Monitor',
            url: '/settings/alarm/state',
            menuIndex: 2
          }),
          mk('settingsAlarmHistory', {
            menuName: 'Alarm History',
            titles: {zh: '告警历史', en: 'Alarm History'},
            icon: 'DocumentChecked',
            url: '/settings/alarm/history',
            menuIndex: 3
          }),
          mk('settingsDriverAlarm', {
            menuName: 'Driver Alarm',
            titles: {zh: '驱动告警', en: 'Driver Alarm'},
            icon: 'Promotion',
            url: '/settings/alarm/driver',
            menuIndex: 4
          }),
          mk('settingsDeviceAlarm', {
            menuName: 'Device Alarm',
            titles: {zh: '设备告警', en: 'Device Alarm'},
            icon: 'Management',
            url: '/settings/alarm/device',
            menuIndex: 5
          }),
          mk('settingsPointAlarm', {
            menuName: 'Point Alarm',
            titles: {zh: '点位告警', en: 'Point Alarm'},
            icon: 'TrendCharts',
            url: '/settings/alarm/point',
            menuIndex: 6
          }),
          mk('settingsEventHistory', {
            menuName: 'Event History',
            titles: {zh: '事件历史', en: 'Event History'},
            icon: 'Document',
            url: '/settings/event/history',
            menuIndex: 7
          }),
          mk('settingsCommandHistory', {
            menuName: 'Command History',
            titles: {zh: '指令历史', en: 'Command History'},
            icon: 'Document',
            url: '/settings/command/history',
            menuIndex: 8
          })
        ]
      }),
      mk('settingsAudit', {
        menuName: 'Audit', titles: {zh: '审计', en: 'Audit'}, icon: 'Files', menuIndex: 8, children: [
          mk('settingsIdentityAudit', {
            menuName: 'Identity Audit',
            titles: {zh: '身份审计', en: 'Identity Audit'},
            icon: 'DocumentChecked',
            url: '/settings/identity_audit',
            menuIndex: 1
          }),
          mk('settingsMcpAudit', {
            menuName: 'MCP Audit',
            titles: {zh: 'MCP 审计', en: 'MCP Audit'},
            icon: 'Document',
            url: '/settings/mcp_audit',
            menuIndex: 2
          })
        ]
      }),
      mk('settingsAbout', {
        menuName: 'About',
        titles: {zh: '关于', en: 'About'},
        icon: 'InfoFilled',
        url: '/settings/about',
        menuIndex: 9
      })
    ]
  })
];
