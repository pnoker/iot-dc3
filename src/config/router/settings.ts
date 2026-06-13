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

import type { RouteRecordRaw } from 'vue-router';

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
          meta: { title: 'Users' },
          component: () => import('@/views/settings/user/User.vue'),
        },
        {
          name: 'settingsPrincipal',
          path: 'principal',
          meta: { title: 'Principals' },
          component: () => import('@/views/settings/principal/Principal.vue'),
        },
        {
          name: 'settingsTenantMembership',
          path: 'tenant_membership',
          meta: { title: 'Tenant Membership' },
          component: () => import('@/views/settings/tenantMembership/TenantMembership.vue'),
        },
        {
          name: 'settingsLocalCredential',
          path: 'local_credential',
          meta: { title: 'Local Credentials' },
          component: () => import('@/views/settings/localCredential/LocalCredential.vue'),
        },
        {
          name: 'settingsRole',
          path: 'role',
          meta: { title: 'Roles' },
          component: () => import('@/views/settings/role/Role.vue'),
        },
        {
          name: 'settingsRolePrincipalBind',
          path: 'role_principal_bind',
          meta: { title: 'Role Principal Bind' },
          component: () => import('@/views/settings/rolePrincipalBind/RolePrincipalBind.vue'),
        },
        {
          name: 'settingsResource',
          path: 'resource',
          meta: { title: 'Resources' },
          component: () => import('@/views/settings/resource/Resource.vue'),
        },
        {
          name: 'settingsApi',
          path: 'api',
          meta: { title: 'APIs' },
          component: () => import('@/views/settings/api/Api.vue'),
        },
        {
          name: 'settingsMenu',
          path: 'menu',
          meta: { title: 'Menus' },
          component: () => import('@/views/settings/menu/Menu.vue'),
        },
        {
          name: 'settingsGroup',
          path: 'group',
          meta: { title: 'Groups' },
          component: () => import('@/views/settings/group/Group.vue'),
        },
        {
          name: 'settingsLabel',
          path: 'label',
          meta: { title: 'Labels' },
          component: () => import('@/views/settings/label/Label.vue'),
        },
        {
          name: 'settingsAlarm',
          path: 'alarm',
          redirect: '/settings/alarm/rule',
          meta: { title: 'Alarm' },
        },
        {
          name: 'settingsModel',
          path: 'model',
          redirect: '/settings/model/config',
          meta: { title: 'Model' },
        },
        {
          name: 'settingsEvent',
          path: 'event',
          redirect: '/settings/event/history',
          meta: { title: 'Event' },
        },
        {
          name: 'settingsCommand',
          path: 'command',
          redirect: '/settings/command/history',
          meta: { title: 'Command' },
        },
        {
          name: 'settingsAlarmRule',
          path: 'alarm/rule',
          meta: { title: 'Alarm Rules' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'rule' },
        },
        {
          name: 'settingsAlarmNotify',
          path: 'alarm/notify',
          meta: { title: 'Alarm Notify' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'notify' },
        },
        {
          name: 'settingsAlarmMessage',
          path: 'alarm/message',
          meta: { title: 'Alarm Message' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'message' },
        },
        {
          name: 'settingsAlarmChannel',
          path: 'alarm/channel',
          meta: { title: 'Alarm Channels' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'channel' },
        },
        {
          name: 'settingsAlarmBind',
          path: 'alarm/bind',
          meta: { title: 'Alarm Bindings' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'bind' },
        },
        {
          name: 'settingsAlarmState',
          path: 'alarm/state',
          meta: { title: 'Alarm States' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'state' },
        },
        {
          name: 'settingsAlarmHistory',
          path: 'alarm/history',
          meta: { title: 'Alarm History' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'history' },
        },
        {
          name: 'settingsAlarmOverview',
          path: 'alarm/overview',
          meta: { title: 'Event Overview' },
          component: () => import('@/views/settings/event/Overview.vue'),
        },
        {
          name: 'settingsDeviceAlarm',
          path: 'alarm/device',
          meta: { title: 'Device Alarm' },
          component: () => import('@/views/settings/event/DeviceEvent.vue'),
        },
        {
          name: 'settingsDriverAlarm',
          path: 'alarm/driver',
          meta: { title: 'Driver Alarm' },
          component: () => import('@/views/settings/event/DriverEvent.vue'),
        },
        {
          name: 'settingsPointAlarm',
          path: 'alarm/point',
          meta: { title: 'Point Events' },

          component: () => import('@/views/settings/event/PointEvent.vue'),
        },
        {
          name: 'settingsEventHistory',
          path: 'event/history',
          meta: { title: 'Event History' },
          component: () => import('@/views/settings/event/EventHistory.vue'),
        },
        {
          name: 'settingsCommandHistory',
          path: 'command/history',
          meta: { title: 'Command History' },
          component: () => import('@/views/settings/command/CommandHistory.vue'),
        },
        {
          name: 'settingsModelConfig',
          path: 'model/config',
          meta: { title: 'Model Config' },
          component: () => import('@/views/settings/agentic/AgenticSettings.vue'),
        },
        {
          name: 'settingsModelProvider',
          path: 'model/provider',
          meta: { title: 'Model Providers' },
          component: () => import('@/views/settings/agentic/ProviderSettings.vue'),
        },
        {
          name: 'settingsServiceAccount',
          path: 'service_account',
          meta: { title: 'Service Accounts' },
          component: () => import('@/views/settings/serviceAccount/ServiceAccount.vue'),
        },
        {
          name: 'settingsMcpServer',
          path: 'mcp',
          meta: { title: 'MCP Service' },
          component: () => import('@/views/settings/mcp/McpServer.vue'),
        },
        {
          name: 'settingsAbout',
          path: 'about',
          meta: { title: 'About' },
          component: () => import('@/views/settings/about/About.vue'),
        },
      ],
    },
  ],
};

export default settingsRouter;
