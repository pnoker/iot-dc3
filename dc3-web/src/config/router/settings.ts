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

const Layout = () => import('@/components/layout/Layout.vue');

const settingsRouter: RouteRecordRaw = {
  path: '/settings',
  component: Layout,
  redirect: '/settings/user',
  children: [
    {
      path: '',
      component: () => import('@/views/settings/Settings.vue'),
      children: [
        {
          name: 'settingsUser',
          path: 'user',
          meta: {title: 'nav.settingsUser'},
          component: () => import('@/views/settings/user/User.vue'),
        },
        {
          name: 'settingsPrincipal',
          path: 'principal',
          meta: {title: 'nav.settingsPrincipal'},
          component: () => import('@/views/settings/principal/Principal.vue'),
        },
        {
          name: 'settingsTenantMembership',
          path: 'tenant_membership',
          meta: {title: 'nav.settingsTenantMembership'},
          component: () => import('@/views/settings/tenantMembership/TenantMembership.vue'),
        },
        {
          name: 'settingsLocalCredential',
          path: 'local_credential',
          meta: {title: 'nav.settingsLocalCredential'},
          component: () => import('@/views/settings/localCredential/LocalCredential.vue'),
        },
        {
          name: 'settingsIdentityAudit',
          path: 'identity_audit',
          meta: {title: 'nav.settingsIdentityAudit'},
          component: () => import('@/views/settings/identityAudit/IdentityAudit.vue'),
        },
        {
          name: 'settingsRole',
          path: 'role',
          meta: {title: 'nav.settingsRole'},
          component: () => import('@/views/settings/role/Role.vue'),
        },
        {
          name: 'settingsRolePrincipalBind',
          path: 'role_principal_bind',
          meta: {title: 'nav.settingsRolePrincipalBind'},
          component: () => import('@/views/settings/rolePrincipalBind/RolePrincipalBind.vue'),
        },
        {
          name: 'settingsResource',
          path: 'resource',
          meta: {title: 'nav.settingsResource'},
          component: () => import('@/views/settings/resource/Resource.vue'),
        },
        {
          name: 'settingsApi',
          path: 'api',
          meta: {title: 'nav.settingsApi'},
          component: () => import('@/views/settings/api/Api.vue'),
        },
        {
          name: 'settingsMenu',
          path: 'menu',
          meta: {title: 'nav.settingsMenu'},
          component: () => import('@/views/settings/menu/Menu.vue'),
        },
        {
          name: 'settingsGroup',
          path: 'group',
          meta: {title: 'nav.settingsGroup'},
          component: () => import('@/views/settings/group/Group.vue'),
        },
        {
          name: 'settingsLabel',
          path: 'label',
          meta: {title: 'nav.settingsLabel'},
          component: () => import('@/views/settings/label/Label.vue'),
        },
        {
          name: 'settingsAlarm',
          path: 'alarm',
          redirect: '/settings/alarm/rule',
          meta: {title: 'nav.settingsAlarm'},
        },
        {
          name: 'settingsModel',
          path: 'model',
          redirect: '/settings/model/provider',
          meta: {title: 'nav.settingsModel'},
        },
        {
          name: 'settingsEvent',
          path: 'event',
          redirect: '/settings/event/history',
          meta: {title: 'nav.settingsEvent'},
        },
        {
          name: 'settingsCommand',
          path: 'command',
          redirect: '/settings/command/history',
          meta: {title: 'nav.settingsCommand'},
        },
        {
          name: 'settingsIdentity',
          path: 'identity',
          redirect: '/settings/user',
          meta: {title: 'nav.settingsIdentity'},
        },
        {
          name: 'settingsAccess',
          path: 'access',
          redirect: '/settings/role',
          meta: {title: 'nav.settingsAccess'},
        },
        {
          name: 'settingsEventCommand',
          path: 'event_command',
          redirect: '/settings/alarm/overview',
          meta: {title: 'nav.settingsEventCommand'},
        },
        {
          name: 'settingsAudit',
          path: 'audit',
          redirect: '/settings/identity_audit',
          meta: {title: 'nav.settingsAudit'},
        },
        {
          name: 'settingsIntegration',
          path: 'integration',
          redirect: '/settings/mcp',
          meta: {title: 'nav.settingsIntegration'},
        },
        {
          name: 'settingsSystem',
          path: 'system',
          redirect: '/settings/group',
          meta: {title: 'nav.settingsSystem'},
        },
        {
          name: 'settingsAlarmRule',
          path: 'alarm/rule',
          meta: {title: 'nav.settingsAlarmRule'},
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: {entity: 'rule'},
        },
        {
          name: 'settingsAlarmNotify',
          path: 'alarm/notify',
          meta: {title: 'nav.settingsAlarmNotify'},
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: {entity: 'notify'},
        },
        {
          name: 'settingsAlarmMessage',
          path: 'alarm/message',
          meta: {title: 'nav.settingsAlarmMessage'},
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: {entity: 'message'},
        },
        {
          name: 'settingsAlarmChannel',
          path: 'alarm/channel',
          meta: {title: 'nav.settingsAlarmChannel'},
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: {entity: 'channel'},
        },
        {
          name: 'settingsAlarmBind',
          path: 'alarm/bind',
          meta: {title: 'nav.settingsAlarmBind'},
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: {entity: 'bind'},
        },
        {
          name: 'settingsAlarmState',
          path: 'alarm/state',
          meta: {title: 'nav.settingsAlarmState'},
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: {entity: 'state'},
        },
        {
          name: 'settingsAlarmHistory',
          path: 'alarm/history',
          meta: {title: 'nav.settingsAlarmHistory'},
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: {entity: 'history'},
        },
        {
          name: 'settingsAlarmOverview',
          path: 'alarm/overview',
          meta: {title: 'nav.settingsAlarmOverview'},
          component: () => import('@/views/settings/alarm/Overview.vue'),
        },
        {
          name: 'settingsDeviceAlarm',
          path: 'alarm/device',
          meta: {title: 'nav.settingsDeviceAlarm'},
          component: () => import('@/views/settings/alarm/DeviceEvent.vue'),
        },
        {
          name: 'settingsDriverAlarm',
          path: 'alarm/driver',
          meta: {title: 'nav.settingsDriverAlarm'},
          component: () => import('@/views/settings/alarm/DriverEvent.vue'),
        },
        {
          name: 'settingsPointAlarm',
          path: 'alarm/point',
          meta: {title: 'nav.settingsPointAlarm'},

          component: () => import('@/views/settings/alarm/PointEvent.vue'),
        },
        {
          name: 'settingsEventHistory',
          path: 'event/history',
          meta: {title: 'nav.settingsEventHistory'},
          component: () => import('@/views/settings/event/EventHistory.vue'),
        },
        {
          name: 'settingsCommandHistory',
          path: 'command/history',
          meta: {title: 'nav.settingsCommandHistory'},
          component: () => import('@/views/settings/command/CommandHistory.vue'),
        },
        {
          name: 'settingsModelConfig',
          path: 'model/config',
          meta: {title: 'nav.settingsModelConfig'},
          component: () => import('@/views/settings/agentic/AgenticSettings.vue'),
        },
        {
          name: 'settingsModelProvider',
          path: 'model/provider',
          meta: {title: 'nav.settingsModelProvider'},
          component: () => import('@/views/settings/agentic/ProviderSettings.vue'),
        },
        {
          name: 'settingsServiceAccount',
          path: 'service_account',
          meta: {title: 'nav.settingsServiceAccount'},
          component: () => import('@/views/settings/serviceAccount/ServiceAccount.vue'),
        },
        {
          name: 'settingsMcpServer',
          path: 'mcp',
          meta: {title: 'nav.settingsMcpServer'},
          component: () => import('@/views/settings/mcp/McpServer.vue'),
        },
        {
          name: 'settingsMcpConnection',
          path: 'mcp/connection',
          meta: {title: 'nav.settingsMcpConnection'},
          component: () => import('@/views/settings/mcp/McpConnection.vue'),
        },
        {
          name: 'settingsMcpClient',
          path: 'mcp/client',
          meta: {title: 'nav.settingsMcpClient'},
          component: () => import('@/views/settings/mcp/McpClient.vue'),
        },
        {
          name: 'settingsMcpTool',
          path: 'mcp/tool',
          meta: {title: 'nav.settingsMcpTool'},
          component: () => import('@/views/settings/mcp/McpTool.vue'),
        },
        {
          name: 'settingsMcpAudit',
          path: 'mcp_audit',
          meta: {title: 'nav.settingsMcpAudit'},
          component: () => import('@/views/settings/mcpAudit/McpAudit.vue'),
        },
        {
          name: 'settingsAbout',
          path: 'about',
          meta: {title: 'nav.settingsAbout'},
          component: () => import('@/views/settings/about/About.vue'),
        },
      ],
    },
  ],
};

export default settingsRouter;