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
          name: 'settingsRole',
          path: 'role',
          meta: { title: 'Roles' },
          component: () => import('@/views/settings/role/Role.vue'),
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
          redirect: '/settings/agentic',
          meta: { title: 'Model' },
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
          meta: { title: 'Alarm Histories' },
          component: () => import('@/views/settings/alarm/AlarmNotify.vue'),
          props: { entity: 'history' },
        },
        {
          name: 'settingsAgentic',
          path: 'agentic',
          meta: { title: 'Model Config' },
          component: () => import('@/views/settings/agentic/AgenticSettings.vue'),
        },
        {
          name: 'settingsAgenticProvider',
          path: 'agentic/provider',
          meta: { title: 'Model Providers' },
          component: () => import('@/views/settings/agentic/ProviderSettings.vue'),
        },
        {
          name: 'settingsEvent',
          path: 'event',
          meta: { title: 'Events' },
          component: () => import('@/views/settings/event/Overview.vue'),
        },
        {
          name: 'settingsDeviceEvent',
          path: 'event/device',
          meta: { title: 'Device Events' },
          component: () => import('@/views/settings/event/DeviceEvent.vue'),
        },
        {
          name: 'settingsDriverEvent',
          path: 'event/driver',
          meta: { title: 'Driver Events' },
          component: () => import('@/views/settings/event/DriverEvent.vue'),
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
